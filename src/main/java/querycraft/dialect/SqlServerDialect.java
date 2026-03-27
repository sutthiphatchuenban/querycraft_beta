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
        return String.format("SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s' ORDER BY ORDINAL_POSITION", 
            tableName.replace("'", "''"));
    }

    @Override
    public String getSelectAllWithLimitQuery(String tableName, int limit) {
        return String.format("SELECT TOP %d * FROM %s", limit, escapeIdentifier(tableName));
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
    public String getPreInsertSql(String tableName) {
        return String.format("SET IDENTITY_INSERT %s ON;", escapeIdentifier(tableName));
    }
    
    @Override
    public String getPostInsertSql(String tableName) {
        return String.format("SET IDENTITY_INSERT %s OFF;", escapeIdentifier(tableName));
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
        } else {
            // For SQL Server 2022+, encrypt is true by default, so we need to explicitly disable it
            url += ";encrypt=false";
        }
        // Always trust server certificate for development environments
        url += ";trustServerCertificate=true";
        return url;
    }

    /**
     * Build URL using Named Pipes protocol (no TCP/IP required).
     * Uses the MSSQL JDBC driver's namedPipe=true property.
     * For named instances, the instance name is appended to the server name.
     */
    public String buildNamedPipesUrl(String host, String database, String instanceName, boolean useSSL, boolean useWindowsAuth) {
        StringBuilder url = new StringBuilder("jdbc:sqlserver://");
        url.append(host);

        // For named instances (e.g., SQLEXPRESS), append to server name
        if (instanceName != null && !instanceName.isEmpty() && !instanceName.equalsIgnoreCase("MSSQLSERVER")) {
            url.append("\\").append(instanceName);
        }

        url.append(";databaseName=").append(database);
        url.append(";namedPipe=true");

        if (useWindowsAuth) {
            url.append(";integratedSecurity=true");
        }
        if (useSSL) {
            url.append(";encrypt=true;trustServerCertificate=true");
        } else {
            url.append(";encrypt=false");
        }
        url.append(";trustServerCertificate=true");
        return url.toString();
    }

    /**
     * Build URL with Windows Authentication support.
     */
    public String buildUrl(String host, int port, String database, boolean useSSL, boolean useWindowsAuth, String baseUrlFormat) {
        String url = String.format(baseUrlFormat, host, port, database);
        if (useWindowsAuth) {
            // Use Windows Authentication with current user credentials
            url += ";integratedSecurity=true";
        }
        if (useSSL) {
            url += ";encrypt=true;trustServerCertificate=true";
        } else {
            // For SQL Server 2022+, encrypt is true by default, so we need to explicitly disable it
            url += ";encrypt=false";
        }
        // Always trust server certificate for development environments
        url += ";trustServerCertificate=true";
        return url;
    }
}
