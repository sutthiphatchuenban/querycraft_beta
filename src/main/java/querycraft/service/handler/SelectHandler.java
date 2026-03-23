package querycraft.service.handler;

import querycraft.model.QueryResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle SELECT and other queries that return a result set.
 */
public class SelectHandler extends BaseHandler {
    
    // Rows limit for safety
    private static final int MAX_ROWS = 10000;

    @Override
    public boolean canHandle(String sql) {
        String normalized = normalizeSql(sql);
        return normalized.startsWith("SELECT") || 
               normalized.startsWith("WITH") || 
               normalized.startsWith("SHOW") || 
               normalized.startsWith("DESCRIBE") || 
               normalized.startsWith("EXPLAIN");
    }

    @Override
    public QueryResult handle(String sql, Connection conn) throws SQLException {
        QueryResult result = new QueryResult();
        result.setSelectQuery(true);
        long startTime = System.currentTimeMillis();

        try (Statement stmt = conn.createStatement()) {
            stmt.setMaxRows(MAX_ROWS);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int count = metaData.getColumnCount();
                result.setColumns(extractColumnInfo(metaData, count));

                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[count];
                    for (int i = 0; i < count; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    rows.add(row);
                }
                result.setRows(rows);
            }
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public String getCategory() { return "READ"; }
}
