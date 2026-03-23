package querycraft.service.handler;

import querycraft.model.ColumnInfo;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for query handlers with shared utilities.
 */
public abstract class BaseHandler implements QueryHandler {
    
    protected String normalizeSql(String sql) {
        if (sql == null) return "";
        // Simple normalization for type detection
        return sql.replaceAll("--.*", "").replaceAll("/\\*(.|\\R)*?\\*/", "").trim().toUpperCase();
    }

    protected List<ColumnInfo> extractColumnInfo(ResultSetMetaData metaData, int columnCount) throws SQLException {
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
