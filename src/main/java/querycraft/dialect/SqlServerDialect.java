package querycraft.dialect;

/**
 * Microsoft SQL Server specific SQL dialect.
 */
public class SqlServerDialect implements DatabaseDialect {
    
    @Override
    public String getShowTablesQuery() {
        return "SELECT name, CASE WHEN type = 'U' THEN 'BASE TABLE' WHEN type = 'V' THEN 'VIEW' ELSE 'OTHER' END as table_type FROM sys.objects WHERE type IN ('U', 'V') ORDER BY name";
    }

    @Override
    public String getDescribeTableQuery(String tableName) {
        return String.format("SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s' ORDER BY ORDINAL_POSITION", tableName);
    }

    @Override
    public String getBeginTransaction() {
        return "BEGIN TRANSACTION;";
    }

    @Override
    public String getCommitTransaction() {
        return "COMMIT TRANSACTION;";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return "[]";
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public String formatBoolean(boolean value) {
        return value ? "1" : "0";
    }

    @Override
    public String buildUrl(String host, int port, String database, boolean useSSL, String baseUrlFormat) {
        String url = String.format(baseUrlFormat, host, port, database);
        if (useSSL) {
            url += ";encrypt=true;trustServerCertificate=true";
        }
        return url;
    }
}
