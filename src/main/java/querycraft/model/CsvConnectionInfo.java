package querycraft.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection info for CSV folder connections using H2.
 * Supports multiple CSV files in a folder, each becoming a table.
 */
public class CsvConnectionInfo extends ConnectionInfo {

    private String csvFolderPath;
    private List<CsvFileInfo> csvFiles = new ArrayList<>();

    public static class CsvFileInfo {
        private final String fileName;
        private final String tableName;
        private final String fullPath;

        public CsvFileInfo(String fileName, String tableName, String fullPath) {
            this.fileName = fileName;
            this.tableName = tableName;
            this.fullPath = fullPath;
        }

        public String getFileName() { return fileName; }
        public String getTableName() { return tableName; }
        public String getFullPath() { return fullPath; }
    }

    public CsvConnectionInfo() {
        super();
        setDatabaseType(DatabaseType.CSV);
        setHost("localhost");
        setPort(0);
    }

    public CsvConnectionInfo(String csvFolderPath) {
        this();
        this.csvFolderPath = csvFolderPath;
        scanCsvFiles();
    }

    /**
     * Scan folder for CSV files and create table mappings.
     */
    private void scanCsvFiles() {
        csvFiles.clear();
        if (csvFolderPath == null) return;

        File folder = new File(csvFolderPath);
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".csv")
        );

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                // Create table name from filename (remove .csv extension)
                String tableName = fileName;
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    tableName = fileName.substring(0, lastDotIndex);
                }
                // Clean table name for SQL (remove special chars, spaces)
                tableName = sanitizeTableName(tableName);
                csvFiles.add(new CsvFileInfo(fileName, tableName, file.getAbsolutePath()));
            }
        }
    }

    /**
     * Sanitize table name for SQL - remove/replace special characters.
     */
    private String sanitizeTableName(String name) {
        // Replace spaces and special chars with underscore
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");
        // Remove multiple underscores
        sanitized = sanitized.replaceAll("_+", "_");
        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_|_$", "");
        // Ensure it doesn't start with a number
        if (sanitized.matches("^\\d.*")) {
            sanitized = "csv_" + sanitized;
        }
        // If empty after sanitization, use generic name
        if (sanitized.isEmpty()) {
            sanitized = "csv_table";
        }
        return sanitized;
    }

    public String getCsvFolderPath() {
        return csvFolderPath;
    }

    public void setCsvFolderPath(String csvFolderPath) {
        this.csvFolderPath = csvFolderPath;
        scanCsvFiles();
    }

    public List<CsvFileInfo> getCsvFiles() {
        return csvFiles;
    }

    /**
     * Get number of CSV files found.
     */
    public int getCsvFileCount() {
        return csvFiles.size();
    }

    @Override
    public String getJdbcUrl() {
        // For CSV, we use a unique in-memory H2 database
        long uniqueId = System.currentTimeMillis();
        return String.format("jdbc:h2:mem:csvdb_%d;DB_CLOSE_DELAY=-1;IGNORECASE=TRUE", uniqueId);
    }

    /**
     * Get the SQL to create table from CSV file.
     * H2 CSVREAD auto-detects charset and delimiter.
     */
    public String getCreateTableSql(CsvFileInfo csvFile) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(escapeIdentifier(csvFile.getTableName())).append(" AS ");
        sql.append("SELECT * FROM CSVREAD('").append(escapeSqlString(csvFile.getFullPath())).append("')");
        return sql.toString();
    }

    /**
     * Get all CREATE TABLE SQL statements.
     */
    public List<String> getAllCreateTableSqls() {
        List<String> sqls = new ArrayList<>();
        for (CsvFileInfo csvFile : csvFiles) {
            sqls.add(getCreateTableSql(csvFile));
        }
        return sqls;
    }

    /**
     * Escape SQL string literals.
     */
    private String escapeSqlString(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }

    /**
     * Escape identifier for H2.
     */
    private String escapeIdentifier(String identifier) {
        if (identifier == null) return "";
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String getDatabase() {
        if (csvFolderPath != null) {
            File folder = new File(csvFolderPath);
            return folder.getName() + " (" + csvFiles.size() + " files)";
        }
        return "CSV Folder";
    }

    @Override
    public String toString() {
        return "CSV: " + (csvFolderPath != null ? new File(csvFolderPath).getName() : "Unknown") 
               + " (" + csvFiles.size() + " files)";
    }
}
