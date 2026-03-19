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

    public String buildUrl(String host, int port, String database) {
        return String.format(urlFormat, host, port, database);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
