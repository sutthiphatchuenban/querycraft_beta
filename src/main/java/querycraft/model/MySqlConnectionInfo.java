package querycraft.model;

/**
 * Connection info for MySQL database.
 * Provides MySQL-specific connection configuration.
 */
public class MySqlConnectionInfo extends ConnectionInfo {

    private boolean useSsl;
    private String serverTimezone;

    public MySqlConnectionInfo() {
        super();
        setDatabaseType(DatabaseType.MYSQL);
        this.serverTimezone = "UTC";
    }

    public MySqlConnectionInfo(String host, int port, String database, String username, String password) {
        super(DatabaseType.MYSQL, host, port, database, username, password);
        this.serverTimezone = "UTC";
    }

    public MySqlConnectionInfo(String host, int port, String database, String username, String password, boolean useSsl) {
        this(host, port, database, username, password);
        this.useSsl = useSsl;
        setUseSSL(useSsl);
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        setUseSSL(useSsl);
    }

    public String getServerTimezone() {
        return serverTimezone;
    }

    public void setServerTimezone(String serverTimezone) {
        this.serverTimezone = serverTimezone;
    }

    @Override
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://")
           .append(getHost()).append(":").append(getPort())
           .append("/").append(getDatabase())
           .append("?useSSL=").append(useSsl)
           .append("&serverTimezone=").append(serverTimezone)
           .append("&allowPublicKeyRetrieval=true");
        
        return url.toString();
    }

    @Override
    public String toString() {
        return String.format("MySQL@%s:%d/%s", getHost(), getPort(), getDatabase());
    }
}
