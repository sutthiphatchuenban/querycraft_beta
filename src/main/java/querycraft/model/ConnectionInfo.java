package querycraft.model;

import java.util.Objects;

/**
 * Model class holding database connection parameters.
 */
public class ConnectionInfo {
    private DatabaseType databaseType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    public ConnectionInfo() {
    }

    public ConnectionInfo(DatabaseType databaseType, String host, int port, String database, String username, String password) {
        this(databaseType, host, port, database, username, password, false);
    }

    public ConnectionInfo(DatabaseType databaseType, String host, int port, String database, String username, String password, boolean useSSL) {
        this.databaseType = databaseType;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getJdbcUrl() {
        if (databaseType == null) {
            throw new IllegalStateException("Database type must be set");
        }
        return databaseType.buildUrl(host, port, database, useSSL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionInfo that = (ConnectionInfo) o;
        return port == that.port &&
                databaseType == that.databaseType &&
                Objects.equals(host, that.host) &&
                Objects.equals(database, that.database) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseType, host, port, database, username);
    }

    @Override
    public String toString() {
        return String.format("%s@%s:%d/%s", databaseType != null ? databaseType.getDisplayName() : "Unknown", host, port, database);
    }
}
