package querycraft.service.handler;

import querycraft.model.QueryResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fallback handler using stmt.execute() to detect results.
 */
public class GenericHandler extends BaseHandler {
    
    @Override
    public boolean canHandle(String sql) {
        return true; // Always handle as generic
    }

    @Override
    public QueryResult handle(String sql, Connection conn) throws SQLException {
        QueryResult result = new QueryResult();
        long startTime = System.currentTimeMillis();

        try (Statement stmt = conn.createStatement()) {
            boolean hasRs = stmt.execute(sql);

            if (hasRs) {
                try (ResultSet rs = stmt.getResultSet()) {
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
                    result.setSelectQuery(true);
                }
            } else {
                result.setAffectedRows(stmt.getUpdateCount());
                result.setSelectQuery(false);
            }
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public String getCategory() { return "GENERIC"; }
}
