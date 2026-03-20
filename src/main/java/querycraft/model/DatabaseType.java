package querycraft.model;

/**
 * Enum representing supported database types.
 */
public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 3306),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s", 5432),
    MSSQL("Microsoft SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false", 1433);

    private final String displayName;
    private final String driverClass;
    private final String urlFormat;
    private final int defaultPort;

    DatabaseType(String displayName, String driverClass, String urlFormat, int defaultPort) {
        this.displayName = displayName;
        this.driverClass = driverClass;
        this.urlFormat = urlFormat;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrlFormat() {
        return urlFormat;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String buildUrl(String host, int port, String database, boolean useSSL) {
        String url = String.format(urlFormat, host, port, database);
        if (useSSL) {
            if (this == POSTGRESQL) {
                url += (url.contains("?") ? "&" : "?") + "sslmode=require";
            } else if (this == MYSQL) {
                url = url.replace("useSSL=false", "useSSL=true");
            } else if (this == MSSQL) {
                url += ";encrypt=true;trustServerCertificate=true";
            }
        }
        return url;
    }

    public String buildUrl(String host, int port, String database) {
        return buildUrl(host, port, database, false);
    }

    /**
     * Get the SQL query to list all tables in the current database.
     */
    public String getShowTablesQuery() {
        switch (this) {
            case MYSQL:
                // Returns Tables_in_db, Table_type
                return "SHOW FULL TABLES";
            case POSTGRESQL:
                return "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'public' AND table_type IN ('BASE TABLE', 'VIEW') ORDER BY table_name";
            case MSSQL:
                return "SELECT name, CASE WHEN type = 'U' THEN 'BASE TABLE' WHEN type = 'V' THEN 'VIEW' ELSE 'OTHER' END as table_type FROM sys.objects WHERE type IN ('U', 'V') ORDER BY name";
            default:
                return "";
        }
    }

    /**
     * Get the SQL query to describe a table's structure.
     */
    public String getDescribeTableQuery(String tableName) {
        switch (this) {
            case MYSQL:
                return "DESCRIBE " + tableName;
            case POSTGRESQL:
                return "SELECT column_name, data_type, character_maximum_length, is_nullable, column_default " +
                       "FROM information_schema.columns " +
                       "WHERE table_name = '" + tableName + "' " +
                       "ORDER BY ordinal_position";
            case MSSQL:
                return "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT " +
                       "FROM INFORMATION_SCHEMA.COLUMNS " +
                       "WHERE TABLE_NAME = '" + tableName + "' " +
                       "ORDER BY ORDINAL_POSITION";
            default:
                return "";
        }
    }

    /**
     * Escape an identifier (table or column name) based on database syntax.
     */
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return "";
        
        switch (this) {
            case MYSQL:
                return "`" + identifier.replace("`", "``") + "`";
            case POSTGRESQL:
                return "\"" + identifier.replace("\"", "\"\"") + "\"";
            case MSSQL:
                return "[" + identifier.replace("]", "]]") + "]";
            default:
                return "\"" + identifier + "\"";
        }
    }

    /**
     * Get the transaction start command.
     */
    public String getBeginTransaction() {
        switch (this) {
            case MYSQL:
                return "START TRANSACTION;";
            case POSTGRESQL:
                return "BEGIN;";
            case MSSQL:
                return "BEGIN TRANSACTION;";
            default:
                return "BEGIN TRANSACTION;";
        }
    }

    /**
     * Get the transaction commit command.
     */
    public String getCommitTransaction() {
        switch (this) {
            case MYSQL:
            case POSTGRESQL:
                return "COMMIT;";
            case MSSQL:
                return "COMMIT TRANSACTION;";
            default:
                return "COMMIT;";
        }
    }

    /**
     * Format a boolean value for SQL.
     */
    public String formatBoolean(boolean value) {
        switch (this) {
            case MSSQL:
                return value ? "1" : "0"; // BIT type
            default:
                return value ? "TRUE" : "FALSE";
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
