package querycraft.model;

import querycraft.dialect.SqlServerDialect;

/**
 * Connection info for Microsoft SQL Server with Windows Authentication and Named Pipes support.
 */
public class MssqlConnectionInfo extends ConnectionInfo {

    private boolean useWindowsAuth;
    private boolean useNamedPipes;
    private String instanceName;

    public MssqlConnectionInfo() {
        super();
        setDatabaseType(DatabaseType.MSSQL);
    }

    public MssqlConnectionInfo(String host, int port, String database, String username, String password) {
        super(DatabaseType.MSSQL, host, port, database, username, password);
    }

    public MssqlConnectionInfo(String host, int port, String database, boolean useWindowsAuth) {
        super();
        setDatabaseType(DatabaseType.MSSQL);
        setHost(host);
        setPort(port);
        setDatabase(database);
        this.useWindowsAuth = useWindowsAuth;
    }

    /**
     * Constructor for Named Pipes connection.
     */
    public MssqlConnectionInfo(String host, String database, String instanceName,
                                boolean useWindowsAuth, boolean useNamedPipes) {
        super();
        setDatabaseType(DatabaseType.MSSQL);
        setHost(host);
        setPort(0); // Port not used for Named Pipes
        setDatabase(database);
        this.instanceName = instanceName;
        this.useWindowsAuth = useWindowsAuth;
        this.useNamedPipes = useNamedPipes;
    }

    public boolean isUseWindowsAuth() {
        return useWindowsAuth;
    }

    public void setUseWindowsAuth(boolean useWindowsAuth) {
        this.useWindowsAuth = useWindowsAuth;
    }

    public boolean isUseNamedPipes() {
        return useNamedPipes;
    }

    public void setUseNamedPipes(boolean useNamedPipes) {
        this.useNamedPipes = useNamedPipes;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public String getJdbcUrl() {
        SqlServerDialect dialect = new SqlServerDialect();

        // When using Windows Auth, stick to standard TCP/IP as it's more stable in local IDE runs.
        // Also ensure encryption is handled correctly to prevent hangs on handshake.
        if (useWindowsAuth) {
            return String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;integratedSecurity=true;encrypt=false;trustServerCertificate=true",
                getHost(), getPort(), getDatabase()
            );
        }

        if (useNamedPipes) {
            return dialect.buildNamedPipesUrl(
                getHost(), getDatabase(), instanceName, isUseSSL(), useWindowsAuth
            );
        }

        String baseUrlFormat = "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false";
        return dialect.buildUrl(getHost(), getPort(), getDatabase(), isUseSSL(), useWindowsAuth, baseUrlFormat);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MSSQL@");
        sb.append(getHost());
        if (!useNamedPipes) {
            sb.append(":").append(getPort());
        }
        sb.append("/").append(getDatabase());

        if (useNamedPipes) {
            sb.append(" (Named Pipes");
            if (instanceName != null && !instanceName.isEmpty()) {
                sb.append(": ").append(instanceName);
            }
            sb.append(")");
        }
        if (useWindowsAuth) {
            sb.append(" (Windows Auth)");
        }
        return sb.toString();
    }
}
