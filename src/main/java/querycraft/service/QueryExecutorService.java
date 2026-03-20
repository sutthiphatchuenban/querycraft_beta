package querycraft.service;

import querycraft.model.ColumnInfo;
import querycraft.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for executing SQL queries.
 */
public class QueryExecutorService {

    private static final int MAX_ROWS = 10000; // Limit rows for safety
    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("QueryExecutor-Thread");
        return t;
    });

    private final DatabaseConnectionService connectionService;

    public QueryExecutorService() {
        this.connectionService = DatabaseConnectionService.getInstance();
    }

    /**
     * Execute a SELECT query and return results.
     */
    public QueryResult executeSelect(String sql) throws SQLException {
        connectionService.validateConnection();

        QueryResult result = new QueryResult();
        result.setSelectQuery(true);

        long startTime = System.currentTimeMillis();

        try (Statement stmt = connectionService.getCurrentConnection().createStatement()) {
            stmt.setMaxRows(MAX_ROWS);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Extract column information
                List<ColumnInfo> columns = extractColumnInfo(metaData, columnCount);
                result.setColumns(columns);

                // Extract rows
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    rows.add(row);
                }
                result.setRows(rows);
            }
        } catch (SQLException e) {
            result.setErrorMessage("Query execution failed: " + e.getMessage());
            throw e;
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Execute a DELETE query and return affected row count.
     */
    public QueryResult executeDelete(String sql) throws SQLException {
        connectionService.validateConnection();

        QueryResult result = new QueryResult();
        result.setSelectQuery(false);

        long startTime = System.currentTimeMillis();

        try (Statement stmt = connectionService.getCurrentConnection().createStatement()) {
            int affectedRows = stmt.executeUpdate(sql);
            result.setAffectedRows(affectedRows);
        } catch (SQLException e) {
            result.setErrorMessage("Delete execution failed: " + e.getMessage());
            throw e;
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Execute any query and auto-detect if it's SELECT or other.
     */
    public QueryResult execute(String sql) throws SQLException {
        String cleanSql = stripComments(sql).trim().toUpperCase();

        if (cleanSql.startsWith("SELECT") || cleanSql.startsWith("WITH") || cleanSql.startsWith("SHOW") || cleanSql.startsWith("DESCRIBE") || cleanSql.startsWith("EXPLAIN")) {
            return executeSelect(sql);
        } else if (cleanSql.startsWith("DELETE")) {
            return executeDelete(sql);
        } else if (cleanSql.startsWith("INSERT") || cleanSql.startsWith("UPDATE")) {
            return executeUpdate(sql);
        } else {
            return executeGeneric(sql);
        }
    }

    private String stripComments(String sql) {
        if (sql == null) return "";
        // Remove line comments (-- ...) and block comments (/* ... */)
        return sql.replaceAll("--.*", "").replaceAll("/\\*(.|\\R)*?\\*/", "");
    }

    /**
     * Execute INSERT or UPDATE query.
     */
    public QueryResult executeUpdate(String sql) throws SQLException {
        connectionService.validateConnection();

        QueryResult result = new QueryResult();
        result.setSelectQuery(false);

        long startTime = System.currentTimeMillis();

        try (Statement stmt = connectionService.getCurrentConnection().createStatement()) {
            int affectedRows = stmt.executeUpdate(sql);
            result.setAffectedRows(affectedRows);
        } catch (SQLException e) {
            result.setErrorMessage("Update execution failed: " + e.getMessage());
            throw e;
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Execute a generic SQL statement.
     */
    public QueryResult executeGeneric(String sql) throws SQLException {
        connectionService.validateConnection();

        QueryResult result = new QueryResult();
        result.setSelectQuery(false);

        long startTime = System.currentTimeMillis();

        try (Statement stmt = connectionService.getCurrentConnection().createStatement()) {
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet) {
                // It was actually a query
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    List<ColumnInfo> columns = extractColumnInfo(metaData, columnCount);
                    result.setColumns(columns);

                    List<Object[]> rows = new ArrayList<>();
                    while (rs.next()) {
                        Object[] row = new Object[columnCount];
                        for (int i = 0; i < columnCount; i++) {
                            row[i] = rs.getObject(i + 1);
                        }
                        rows.add(row);
                    }
                    result.setRows(rows);
                    result.setSelectQuery(true);
                }
            } else {
                // It was an update
                result.setAffectedRows(stmt.getUpdateCount());
            }
        } catch (SQLException e) {
            result.setErrorMessage("Execution failed: " + e.getMessage());
            throw e;
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
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

    private List<ColumnInfo> extractColumnInfo(ResultSetMetaData metaData, int columnCount) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            ColumnInfo col = new ColumnInfo();
            col.setName(metaData.getColumnLabel(i));
            col.setTypeName(metaData.getColumnTypeName(i));
            col.setSqlType(metaData.getColumnType(i));
            col.setDisplaySize(metaData.getColumnDisplaySize(i));
            col.setNullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columns.add(col);
        }
        return columns;
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
