package querycraft.util;

import querycraft.model.ColumnInfo;
import querycraft.model.DatabaseType;

import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Shared utility for formatting SQL values.
 * Used by both SqlInsertGenerator (batch) and SqlStreamingExporter (streaming).
 */
public final class SqlValueFormatter {

    private SqlValueFormatter() {} // Prevent instantiation

    /**
     * Build the INSERT INTO ... (...) VALUES prefix string.
     */
    public static String buildInsertPrefix(String tableName, List<ColumnInfo> columns, DatabaseType dbType) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(dbType.escapeIdentifier(tableName)).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(dbType.escapeIdentifier(columns.get(i).getName()));
        }

        sb.append(") VALUES ");
        return sb.toString();
    }

    /**
     * Build the (val1, val2, ...) VALUES clause for a single row.
     */
    public static String buildValuesClause(Object[] row, List<ColumnInfo> columns,
                                            SimpleDateFormat sdf, DatabaseType dbType) {
        StringBuilder sb = new StringBuilder("(");

        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatSqlValue(row[i], columns.get(i), sdf, dbType));
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Format a single Java object into its SQL literal representation.
     */
    public static String formatSqlValue(Object value, ColumnInfo columnInfo,
                                         SimpleDateFormat sdf, DatabaseType dbType) {
        if (value == null) {
            return "NULL";
        }

        int sqlType = columnInfo.getSqlType();

        // Numeric types - no quotes
        if (isNumericType(sqlType)) {
            return value.toString();
        }

        // Date/Time types
        if (value instanceof java.sql.Timestamp) {
            return "'" + value.toString() + "'";
        }

        if (value instanceof java.sql.Date) {
            return "'" + value.toString() + "'";
        }

        if (value instanceof java.sql.Time) {
            return "'" + value.toString() + "'";
        }

        if (value instanceof Date) {
            return "'" + sdf.format((Date) value) + "'";
        }

        // Boolean
        if (value instanceof Boolean) {
            return dbType.formatBoolean((Boolean) value);
        }

        // String and other types - escape and quote
        String strValue = value.toString();
        strValue = strValue.replace("'", "''"); // Escape single quotes
        return "'" + strValue + "'";
    }

    /**
     * Check if a JDBC SQL type is numeric.
     */
    public static boolean isNumericType(int sqlType) {
        return sqlType == Types.INTEGER ||
                sqlType == Types.BIGINT ||
                sqlType == Types.SMALLINT ||
                sqlType == Types.TINYINT ||
                sqlType == Types.NUMERIC ||
                sqlType == Types.DECIMAL ||
                sqlType == Types.FLOAT ||
                sqlType == Types.DOUBLE ||
                sqlType == Types.REAL;
    }
}
