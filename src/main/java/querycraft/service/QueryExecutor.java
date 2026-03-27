package querycraft.service;

import querycraft.model.ColumnInfo;
import querycraft.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified query executor that handles all SQL query types.
 * Replaces the separate handler pattern with a single execution strategy.
 */
public class QueryExecutor {

    /**
     * Execute any SQL query and return appropriate result type.
     * Automatically detects SELECT vs UPDATE/INSERT/DELETE queries.
     */
    public QueryResult execute(String sql, Connection conn, int maxRows) throws SQLException {
        String normalized = normalizeSql(sql);
        long startTime = System.currentTimeMillis();

        // Determine query type
        if (isReadQuery(normalized)) {
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
                result.setColumns(extractColumnInfo(metaData, columnCount));

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
     */
    public boolean isReadQuery(String sql) {
        String normalized = sql.contains(" ") ? sql : normalizeSql(sql);
        return normalized.startsWith("SELECT") ||
               normalized.startsWith("WITH") ||
               normalized.startsWith("SHOW") ||
               normalized.startsWith("DESCRIBE") ||
               normalized.startsWith("EXPLAIN") ||
               normalized.startsWith("DESC");
    }

    /**
     * Check if this is a DELETE statement.
     */
    public boolean isDeleteQuery(String sql) {
        if (sql == null) return false;
        String normalized = normalizeSql(sql);
        return normalized.startsWith("DELETE");
    }

    /**
     * Check if this is a write query (INSERT/UPDATE/DELETE).
     */
    public boolean isWriteQuery(String sql) {
        String normalized = normalizeSql(sql);
        return normalized.startsWith("INSERT") ||
               normalized.startsWith("UPDATE") ||
               normalized.startsWith("DELETE");
    }

    /**
     * Normalize SQL for type detection.
     */
    private String normalizeSql(String sql) {
        if (sql == null) return "";
        // Remove comments and normalize whitespace
        String normalized = sql.replaceAll("--.*", "")
                               .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                               .trim()
                               .toUpperCase();
        return normalized;
    }

    /**
     * Extract column metadata from result set.
     */
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
}
