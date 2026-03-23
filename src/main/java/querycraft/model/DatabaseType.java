package querycraft.model;

import querycraft.dialect.*;

/**
 * Enum representing supported database types, delegating behavior to dialects.
 */
public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 3306, new MySqlDialect()),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://%s:%d/%s", 5432, new PostgreSqlDialect()),
    MSSQL("Microsoft SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false", 1433, new SqlServerDialect()),
    CSV("CSV File (H2)", "org.h2.Driver", "jdbc:h2:mem:csvdb_%d;DB_CLOSE_DELAY=-1;IGNORECASE=TRUE", 0, new CsvDialect());

    private final String displayName;
    private final String driverClass;
    private final String urlFormat;
    private final int defaultPort;
    private final DatabaseDialect dialect;

    DatabaseType(String displayName, String driverClass, String urlFormat, int defaultPort, DatabaseDialect dialect) {
        this.displayName = displayName;
        this.driverClass = driverClass;
        this.urlFormat = urlFormat;
        this.defaultPort = defaultPort;
        this.dialect = dialect;
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
        return dialect.buildUrl(host, port, database, useSSL, urlFormat);
    }

    public String buildUrl(String host, int port, String database) {
        return buildUrl(host, port, database, false);
    }

    public String getShowTablesQuery() {
        return dialect.getShowTablesQuery();
    }

    public String getDescribeTableQuery(String tableName) {
        return dialect.getDescribeTableQuery(tableName);
    }

    public String escapeIdentifier(String identifier) {
        return dialect.escapeIdentifier(identifier);
    }

    public String getBeginTransaction() {
        return dialect.getBeginTransaction();
    }

    public String getCommitTransaction() {
        return dialect.getCommitTransaction();
    }

    public String formatBoolean(boolean value) {
        return dialect.formatBoolean(value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}

