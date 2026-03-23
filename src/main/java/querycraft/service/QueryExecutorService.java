package querycraft.service;

import querycraft.model.QueryResult;

import java.sql.*;

/**
 * Service for executing SQL queries.
 */
public class QueryExecutorService {

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("QueryExecutor-Thread");
        return t;
    });

    private final DatabaseConnectionService connectionService;
    private final java.util.List<querycraft.service.handler.QueryHandler> handlers = new java.util.ArrayList<>();

    public QueryExecutorService() {
        this.connectionService = DatabaseConnectionService.getInstance();
        
        // Initialize handlers (Strategy/Command pattern)
        handlers.add(new querycraft.service.handler.SelectHandler());
        handlers.add(new querycraft.service.handler.UpdateHandler());
        handlers.add(new querycraft.service.handler.GenericHandler()); // Fallback
    }

    /**
     * Execute any query using the appropriate handler.
     */
    public QueryResult execute(String sql) throws SQLException {
        connectionService.validateConnection();
        Connection conn = connectionService.getCurrentConnection();

        for (querycraft.service.handler.QueryHandler handler : handlers) {
            if (handler.canHandle(sql)) {
                try {
                    return handler.handle(sql, conn);
                } catch (SQLException e) {
                    // Specific handler failed
                    throw e;
                }
            }
        }
        
        throw new SQLException("No suitable handler found for this query");
    }

    private String stripComments(String sql) {
        if (sql == null) return "";
        // Remove line comments (-- ...) and block comments (/* ... */)
        return sql.replaceAll("--.*", "").replaceAll("/\\*(.|\\R)*?\\*/", "");
    }


    /**
     * Validate if the query is safe to execute (basic check).
     * Now more robust against comment-based bypasses.
     */
    public ValidationResult validateQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ValidationResult(false, "Query cannot be empty");
        }

        // Strip comments and normalize whitespace for inspection
        String normalized = stripComments(sql).replaceAll("\\s+", " ").toUpperCase().trim();

        // Check for potentially dangerous operations
        // Using word boundaries to avoid false positives (e.g., 'SELECT * FROM DROPTABLE')
        if (normalized.matches(".*\\bDROP\\b.*") && !isSelectQuery(sql)) {
            return new ValidationResult(false, "DROP operations are not allowed for safety");
        }

        if (normalized.matches(".*\\bTRUNCATE\\b.*")) {
            return new ValidationResult(false, "TRUNCATE operations are not allowed for safety");
        }

        if (normalized.matches(".*\\bALTER\\b.*\\b(DATABASE|SYSTEM)\\b.*")) {
            return new ValidationResult(false, "System-level ALTER operations are not allowed");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Check if query is a DELETE statement.
     */
    public boolean isDeleteQuery(String sql) {
        return sql != null && stripComments(sql).trim().toUpperCase().startsWith("DELETE");
    }

    /**
     * Check if query is a SELECT statement.
     */
    public boolean isSelectQuery(String sql) {
        if (sql == null) return false;
        String clean = stripComments(sql).trim().toUpperCase();
        return clean.startsWith("SELECT") || clean.startsWith("WITH") || clean.startsWith("SHOW") || clean.startsWith("DESCRIBE") || clean.startsWith("EXPLAIN");
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
        EXECUTOR.shutdownNow();
    }
}
