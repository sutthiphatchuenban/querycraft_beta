package querycraft.dialect;

/**
 * CSV dialect using H2 Database for querying CSV files.
 */
public class CsvDialect implements DatabaseDialect {

    @Override
    public String getShowTablesQuery() {
        return "SELECT TABLE_NAME, 'BASE TABLE' as table_type FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'";
    }

    @Override
    public String getDescribeTableQuery(String tableName) {
        return String.format(
            "SELECT COLUMN_NAME, TYPE_NAME, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT " +
            "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s' ORDER BY ORDINAL_POSITION",
            tableName.toUpperCase()
        );
    }

    @Override
    public String getBeginTransaction() {
        return "START TRANSACTION;";
    }

    @Override
    public String getCommitTransaction() {
        return "COMMIT;";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return "";
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String formatBoolean(boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    @Override
    public String buildUrl(String host, int port, String database, boolean useSSL, String baseUrlFormat) {
        // CSV uses CsvConnectionInfo.getJdbcUrl() instead - this method is for interface compliance
        // Returns a basic H2 in-memory URL
        return String.format("jdbc:h2:mem:csvdb_%d;DB_CLOSE_DELAY=-1;IGNORECASE=TRUE", System.currentTimeMillis());
    }


    @Override
    public String toString() {
        return "CSV (via H2)";
    }
}
