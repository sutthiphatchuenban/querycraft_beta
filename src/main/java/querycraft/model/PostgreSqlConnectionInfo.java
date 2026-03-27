package querycraft.model;

/**
 * Connection info for PostgreSQL database.
 * Provides PostgreSQL-specific connection configuration.
 */
public class PostgreSqlConnectionInfo extends ConnectionInfo {

    private boolean useSsl;
    private String sslMode;

    public PostgreSqlConnectionInfo() {
        super();
        setDatabaseType(DatabaseType.POSTGRESQL);
        this.sslMode = "prefer";
    }

    public PostgreSqlConnectionInfo(String host, int port, String database, String username, String password) {
        super(DatabaseType.POSTGRESQL, host, port, database, username, password);
        this.sslMode = "prefer";
    }

    public PostgreSqlConnectionInfo(String host, int port, String database, String username, String password, boolean useSsl) {
        this(host, port, database, username, password);
        this.useSsl = useSsl;
        setUseSSL(useSsl);
        this.sslMode = useSsl ? "require" : "prefer";
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        setUseSSL(useSsl);
        this.sslMode = useSsl ? "require" : "prefer";
    }

    public String getSslMode() {
        return sslMode;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    @Override
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:postgresql://")
           .append(getHost()).append(":").append(getPort())
           .append("/").append(getDatabase());
        
        if (useSsl) {
            url.append("?sslmode=require");
        } else {
            url.append("?sslmode=").append(sslMode);
        }
        
        return url.toString();
    }

    @Override
    public String toString() {
        return String.format("PostgreSQL@%s:%d/%s", getHost(), getPort(), getDatabase());
    }
}
