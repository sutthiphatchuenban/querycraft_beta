package querycraft.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing query execution result.
 */
public class QueryResult {
    private List<ColumnInfo> columns;
    private List<Object[]> rows;
    private int rowCount;
    private boolean isSelectQuery;
    private int affectedRows;
    private String errorMessage;
    private long executionTimeMs;

    public QueryResult() {
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.isSelectQuery = true;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public List<Object[]> getRows() {
        return rows;
    }

    public void setRows(List<Object[]> rows) {
        this.rows = rows;
        this.rowCount = rows != null ? rows.size() : 0;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public boolean isSelectQuery() {
        return isSelectQuery;
    }

    public void setSelectQuery(boolean selectQuery) {
        isSelectQuery = selectQuery;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public void addRow(Object[] row) {
        rows.add(row);
        rowCount = rows.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        Object[] row = rows.get(rowIndex);
        if (columnIndex < 0 || columnIndex >= row.length) {
            return null;
        }
        return row[columnIndex];
    }

    public String getTableName() {
        // Try to extract table name from first column if available
        if (!columns.isEmpty()) {
            // This is a simplified approach - in real scenario, we might want to parse the query
            return "exported_data";
        }
        return "table";
    }
}
