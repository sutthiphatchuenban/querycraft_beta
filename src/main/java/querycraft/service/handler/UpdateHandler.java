package querycraft.service.handler;

import querycraft.model.QueryResult;
import java.sql.*;

/**
 * Handle INSERT/UPDATE/DELETE queries that return affected row counts.
 */
public class UpdateHandler extends BaseHandler {
    
    @Override
    public boolean canHandle(String sql) {
        String normalized = normalizeSql(sql);
        return normalized.startsWith("INSERT") || 
               normalized.startsWith("UPDATE") || 
               normalized.startsWith("DELETE");
    }

    @Override
    public QueryResult handle(String sql, Connection conn, int maxRows) throws SQLException {
        QueryResult result = new QueryResult();
        result.setSelectQuery(false);
        long startTime = System.currentTimeMillis();

        try (Statement stmt = conn.createStatement()) {
            int affectedRows = stmt.executeUpdate(sql);
            result.setAffectedRows(affectedRows);
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public String getCategory() { return "WRITE"; }
}
