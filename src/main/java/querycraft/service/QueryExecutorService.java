package querycraft.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.exception.QueryCraftException;
import querycraft.model.QueryResult;

import java.sql.*;
import java.util.concurrent.*;

/**
 * Service for executing SQL queries with timeout support and improved security.
 */
public class QueryExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutorService.class);
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 30;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("QueryExecutor-Thread");
        return t;
    });

    private final DatabaseConnectionService connectionService;
    private final java.util.List<querycraft.service.handler.QueryHandler> handlers = new java.util.ArrayList<>();
    private int queryTimeoutSeconds = DEFAULT_QUERY_TIMEOUT_SECONDS;
    private int maxRows = 10000;

    public QueryExecutorService() {
        this.connectionService = DatabaseConnectionService.getInstance();
        
        // Initialize handlers (Strategy/Command pattern)
        handlers.add(new querycraft.service.handler.SelectHandler());
        handlers.add(new querycraft.service.handler.UpdateHandler());
        handlers.add(new querycraft.service.handler.GenericHandler()); // Fallback
        
        logger.debug("QueryExecutorService initialized with {} handlers", handlers.size());
    }

    /**
     * Set query timeout in seconds.
     */
    public void setQueryTimeout(int seconds) {
        if (seconds < 1 || seconds > 300) {
            throw new IllegalArgumentException("Query timeout must be between 1 and 300 seconds");
        }
        this.queryTimeoutSeconds = seconds;
        logger.debug("Query timeout set to {} seconds", seconds);
    }

    /**
     * Get current query timeout in seconds.
     */
    public int getQueryTimeout() {
        return queryTimeoutSeconds;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
        logger.debug("Max rows limit set to {}", maxRows);
    }

    public int getMaxRows() {
        return maxRows;
    }

    /**
     * Execute any query using the appropriate handler with timeout support.
     */
    public QueryResult execute(String sql) throws QueryCraftException {
        connectionService.validateConnection();
        
        // Validate query before execution
        ValidationResult validation = validateQuery(sql);
        if (!validation.isValid()) {
            logger.warn("Query validation failed: {}", validation.getMessage());
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_VALIDATION_FAILED,
                validation.getMessage()
            );
        }

        // Execute with timeout
        Future<QueryResult> future = EXECUTOR.submit(() -> executeInternal(sql));
        
        try {
            return future.get(queryTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.error("Query execution timeout after {} seconds", queryTimeoutSeconds);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_TIMEOUT,
                String.format("Query execution timed out after %d seconds. Consider adding LIMIT clause or optimizing the query.", queryTimeoutSeconds)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Query execution interrupted", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                "Query execution was interrupted",
                e
            );
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof QueryCraftException) {
                throw (QueryCraftException) cause;
            }
            if (cause instanceof SQLException) {
                logger.error("SQL execution failed", cause);
                throw new QueryCraftException(
                    QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                    "Query execution failed: " + cause.getMessage(),
                    cause
                );
            }
            logger.error("Unexpected error during query execution", cause);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                "Unexpected error: " + cause.getMessage(),
                cause
            );
        }
    }

    /**
     * Internal execution without timeout handling.
     */
    private QueryResult executeInternal(String sql) throws Exception {
        Connection conn = null;
        try {
            conn = connectionService.getCurrentConnection();
            
            for (querycraft.service.handler.QueryHandler handler : handlers) {
                if (handler.canHandle(sql)) {
                    logger.debug("Using handler {} for query", handler.getCategory());
                    try {
                        return handler.handle(sql, conn, maxRows);
                    } catch (SQLException e) {
                        logger.error("Handler {} failed to execute query", handler.getCategory(), e);
                        throw new QueryCraftException(
                            QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                            "Query execution failed: " + e.getMessage(),
                            e
                        );
                    }
                }
            }
            
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                "No suitable handler found for this query"
            );
            
        } finally {
            // Close connection if not using pool (CSV connections)
            if (conn != null && !connectionService.getCurrentConnectionInfo().getDatabaseType().equals(
                    querycraft.model.DatabaseType.CSV)) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warn("Failed to close connection", e);
                }
            }
        }
    }

    /**
     * Normalize SQL by removing comments and extra whitespace.
     */
    private String normalizeSql(String sql) {
        if (sql == null) return "";
        
        String normalized = sql;
        
        // Remove block comments /* ... */
        normalized = normalized.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        // Remove line comments -- ...
        normalized = normalized.replaceAll("--.*", "");
        
        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized.toUpperCase();
    }

    /**
     * Enhanced query validation with improved security checks.
     */
    public ValidationResult validateQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ValidationResult(false, "Query cannot be empty");
        }

        String normalized = normalizeSql(sql);

        // Check for empty query after removing comments
        if (normalized.isEmpty()) {
            return new ValidationResult(false, "Query contains only comments");
        }

        // Block dangerous operations
        // Using more comprehensive patterns
        String[] dangerousPatterns = {
            "\\bDROP\\s+(TABLE|DATABASE|INDEX|VIEW|PROCEDURE|FUNCTION|TRIGGER)",
            "\\bTRUNCATE\\s+TABLE",
            "\\bALTER\\s+(DATABASE|SYSTEM)",
            "\\bGRANT\\s+ALL",
            "\\bREVOKE\\s+ALL",
            "\\bSHUTDOWN",
            "\\bKILL\\s+\\d",
            "--",
            "/\\*\\!",
            ";\\s*DROP",
            ";\\s*DELETE\\s+FROM"
        };

        for (String pattern : dangerousPatterns) {
            if (normalized.matches(".*" + pattern + ".*")) {
                logger.warn("Dangerous SQL pattern detected: {}", pattern);
                return new ValidationResult(false, 
                    "Potentially dangerous SQL pattern detected. Operation not allowed for safety.");
            }
        }

        // Check for multiple statements (SQL injection via semicolon)
        int semicolonCount = countUnquotedSemicolons(sql);
        if (semicolonCount > 1) {
            return new ValidationResult(false, 
                "Multiple SQL statements are not allowed. Please execute one statement at a time.");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Count semicolons that are not inside quotes or comments.
     */
    private int countUnquotedSemicolons(String sql) {
        int count = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i < sql.length() - 1) ? sql.charAt(i + 1) : '\0';
            
            // Handle line comment start
            if (!inBlockComment && !inSingleQuote && !inDoubleQuote && c == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }
            
            // Handle block comment start
            if (!inLineComment && !inSingleQuote && !inDoubleQuote && c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            
            // Handle block comment end
            if (inBlockComment && c == '*' && next == '/') {
                inBlockComment = false;
                i++;
                continue;
            }
            
            // Handle line comment end
            if (inLineComment && c == '\n') {
                inLineComment = false;
                continue;
            }
            
            // Skip if in comment
            if (inLineComment || inBlockComment) continue;
            
            // Handle quotes
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            
            // Count semicolons outside quotes
            if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Check if query is a DELETE statement.
     */
    public boolean isDeleteQuery(String sql) {
        if (sql == null) return false;
        String normalized = normalizeSql(sql);
        return normalized.startsWith("DELETE");
    }

    /**
     * Check if query is a SELECT statement.
     */
    public boolean isSelectQuery(String sql) {
        if (sql == null) return false;
        String normalized = normalizeSql(sql);
        return normalized.startsWith("SELECT") || 
               normalized.startsWith("WITH") || 
               normalized.startsWith("SHOW") || 
               normalized.startsWith("DESCRIBE") || 
               normalized.startsWith("EXPLAIN") ||
               normalized.startsWith("DESC");
    }

    /**
     * Inner class for validation results.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Interface for query execution callbacks.
     */
    public interface QueryCallback {
        void onSuccess(QueryResult result);
        void onError(Exception e);
    }

    /**
     * Execute a query asynchronously.
     */
    public void executeQueryAsync(String sql, QueryCallback callback) {
        EXECUTOR.submit(() -> {
            try {
                QueryResult result = execute(sql);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Shutdown the executor service.
     */
    public static void shutdown() {
        logger.info("Shutting down QueryExecutorService");
        EXECUTOR.shutdownNow();
    }
}
