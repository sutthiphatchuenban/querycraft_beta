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
    private final QueryExecutor queryExecutor;
    private int queryTimeoutSeconds = DEFAULT_QUERY_TIMEOUT_SECONDS;
    private int maxRows = 10000;

    public QueryExecutorService() {
        this.connectionService = DatabaseConnectionService.getInstance();
        this.queryExecutor = new QueryExecutor();
        logger.debug("QueryExecutorService initialized");
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
     * Execute any query with timeout support.
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
        try (Connection conn = connectionService.getCurrentConnection()) {
            return queryExecutor.execute(sql, conn, maxRows);
        }
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

        return new ValidationResult(true, null);
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
     * Check if query is a DELETE statement.
     */
    public boolean isDeleteQuery(String sql) {
        return queryExecutor.isDeleteQuery(sql);
    }

    /**
     * Check if query is a SELECT statement.
     */
    public boolean isSelectQuery(String sql) {
        return queryExecutor.isReadQuery(sql);
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
