package querycraft.util;

import querycraft.model.ColumnInfo;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for extracting metadata from ResultSet objects.
 * Provides standardized column extraction to eliminate duplicate code across the codebase.
 */
public final class ResultSetUtils {

    private ResultSetUtils() {
        // Prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Extracts column metadata from a ResultSetMetaData object.
     *
     * @param metaData the result set metadata
     * @return a list of ColumnInfo objects representing the columns
     * @throws SQLException if metadata access fails
     */
    public static List<ColumnInfo> extractColumns(ResultSetMetaData metaData) throws SQLException {
        if (metaData == null) {
            return Collections.emptyList();
        }

        int columnCount = metaData.getColumnCount();
        List<ColumnInfo> columns = new ArrayList<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            ColumnInfo column = new ColumnInfo();
            column.setName(metaData.getColumnLabel(i));
            column.setTypeName(metaData.getColumnTypeName(i));
            column.setSqlType(metaData.getColumnType(i));
            column.setDisplaySize(metaData.getColumnDisplaySize(i));
            column.setNullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columns.add(column);
        }

        return columns;
    }

    /**
     * Extracts column metadata with a limit on the number of columns.
     * Useful for wide tables where only a subset of columns is needed.
     *
     * @param metaData the result set metadata
     * @param maxColumns maximum number of columns to extract (0 or negative means all)
     * @return a list of ColumnInfo objects
     * @throws SQLException if metadata access fails
     */
    public static List<ColumnInfo> extractColumns(ResultSetMetaData metaData, int maxColumns) throws SQLException {
        if (metaData == null) {
            return Collections.emptyList();
        }

        int columnCount = metaData.getColumnCount();
        int limit = (maxColumns > 0) ? Math.min(columnCount, maxColumns) : columnCount;

        List<ColumnInfo> columns = new ArrayList<>(limit);

        for (int i = 1; i <= limit; i++) {
            ColumnInfo column = new ColumnInfo();
            column.setName(metaData.getColumnLabel(i));
            column.setTypeName(metaData.getColumnTypeName(i));
            column.setSqlType(metaData.getColumnType(i));
            column.setDisplaySize(metaData.getColumnDisplaySize(i));
            column.setNullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columns.add(column);
        }

        return columns;
    }

    /**
     * Creates an array of column names from a list of ColumnInfo.
     *
     * @param columns the list of column info
     * @return array of column names
     */
    public static String[] extractColumnNames(List<ColumnInfo> columns) {
        if (columns == null || columns.isEmpty()) {
            return new String[0];
        }

        return columns.stream()
                .map(ColumnInfo::getName)
                .toArray(String[]::new);
    }
}
