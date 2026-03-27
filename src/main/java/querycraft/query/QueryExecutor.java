package querycraft.query;

import querycraft.model.ColumnInfo;
import querycraft.model.QueryResult;
import querycraft.util.ResultSetUtils;
import querycraft.util.ValidationUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified query executor that handles all SQL query types.
 * Replaces the separate handler pattern with a single execution strategy.
 *
 * <p>This class delegates SQL validation and normalization to {@link ValidationUtils}
 * and column extraction to {@link ResultSetUtils} for consistency across the codebase.</p>
 */
public class QueryExecutor {

    /**
     * Execute any SQL query and return appropriate result type.
     * Automatically detects SELECT vs UPDATE/INSERT/DELETE queries.
     *
     * @param sql the SQL query to execute
     * @param conn the database connection
     * @param maxRows maximum number of rows to return (0 means unlimited)
     * @return QueryResult containing the query results
     * @throws SQLException if query execution fails
     */
    public QueryResult execute(String sql, Connection conn, int maxRows) throws SQLException {
        long startTime = System.currentTimeMillis();

        // Determine query type using centralized validation
        if (ValidationUtils.isReadQuery(sql)) {
            return executeReadQuery(sql, conn, maxRows, startTime);
        } else {
            return executeWriteQuery(sql, conn, startTime);
        }
    }

    /**
     * Execute SELECT-like queries that return result sets.
     */
    private QueryResult executeReadQuery(String sql, Connection conn, int maxRows, long startTime) throws SQLException {
        QueryResult result = new QueryResult();
        result.setSelectQuery(true);

        try (Statement stmt = conn.createStatement()) {
            stmt.setMaxRows(maxRows);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Use centralized column extraction
                List<ColumnInfo> columns = ResultSetUtils.extractColumns(metaData);
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
                if (rows.size() >= maxRows && maxRows > 0) {
                    result.setTruncated(true);
                }
            }
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Execute INSERT/UPDATE/DELETE queries that return affected row counts.
     */
    private QueryResult executeWriteQuery(String sql, Connection conn, long startTime) throws SQLException {
        QueryResult result = new QueryResult();
        result.setSelectQuery(false);

        try (Statement stmt = conn.createStatement()) {
            int affectedRows = stmt.executeUpdate(sql);
            result.setAffectedRows(affectedRows);
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Check if this is a SELECT-like query (returns result set).
     * Delegates to {@link ValidationUtils#isReadQuery(String)}.
     *
     * @param sql the SQL to check
     * @return true if it's a read query
     */
    public boolean isReadQuery(String sql) {
        return ValidationUtils.isReadQuery(sql);
    }

    /**
     * Check if this is a DELETE statement.
     * Delegates to {@link ValidationUtils#isDeleteQuery(String)}.
     *
     * @param sql the SQL to check
     * @return true if it's a DELETE query
     */
    public boolean isDeleteQuery(String sql) {
        return ValidationUtils.isDeleteQuery(sql);
    }

    /**
     * Check if this is a write query (INSERT/UPDATE/DELETE).
     * Delegates to {@link ValidationUtils#isWriteQuery(String)}.
     *
     * @param sql the SQL to check
     * @return true if it's a write query
     */
    public boolean isWriteQuery(String sql) {
        return ValidationUtils.isWriteQuery(sql);
    }
}
