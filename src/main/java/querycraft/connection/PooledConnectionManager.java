package querycraft.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.exception.QueryCraftException;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.DatabaseType;
import querycraft.model.MssqlConnectionInfo;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Connection manager that uses HikariCP for connection pooling.
 * Handles standard database connections (MySQL, PostgreSQL, SQL Server).
 */
public class PooledConnectionManager implements ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(PooledConnectionManager.class);

    // Default configuration values
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 30000;
    private static final int DEFAULT_IDLE_TIMEOUT_MS = 300000;
    private static final int DEFAULT_MAX_LIFETIME_MS = 1800000;
    private static final int DEFAULT_VALIDATION_TIMEOUT_MS = 5000;

    private final Map<DatabaseType, Boolean> driverLoadedMap = new HashMap<>();
    private HikariDataSource dataSource;
    @SuppressWarnings("unused")
    private ConnectionInfo currentConnectionInfo;

    // Configurable settings
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int minIdle = DEFAULT_MIN_IDLE;
    private int connectionTimeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;

    @Override
    public Connection connect(ConnectionInfo connectionInfo) throws QueryCraftException {
        if (connectionInfo instanceof CsvConnectionInfo) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "PooledConnectionManager does not support CSV connections. Use CsvConnectionManager instead."
            );
        }

        // Disconnect any existing connection first
        disconnect();

        // Load driver
        loadDriver(connectionInfo.getDatabaseType());

        // Configure Windows Auth for MSSQL if needed
        if (connectionInfo instanceof MssqlConnectionInfo mssqlInfo && mssqlInfo.isUseWindowsAuth()) {
            configureWindowsAuthDll();
        }

        // Create connection pool
        createConnectionPool(connectionInfo);
        this.currentConnectionInfo = connectionInfo;

        // Test the connection
        try (Connection testConn = dataSource.getConnection()) {
            if (testConn == null || testConn.isClosed()) {
                throw new QueryCraftException(
                    QueryCraftException.ErrorCode.CONNECTION_FAILED,
                    "Failed to establish connection - connection is closed"
                );
            }
            logger.info("Successfully connected to {}@{} using connection pool",
                connectionInfo.getDatabaseType(), connectionInfo.getHost());
            return dataSource.getConnection();
        } catch (SQLException e) {
            disconnect();
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Failed to establish connection: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing connection pool");
            dataSource.close();
            dataSource = null;
        }
        currentConnectionInfo = null;
    }

    @Override
    public Connection getConnection() throws QueryCraftException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "No active database connection. Please connect first."
            );
        }

        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Failed to get connection from pool: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean testConnection(ConnectionInfo connectionInfo) throws QueryCraftException {
        loadDriver(connectionInfo.getDatabaseType());

        try (Connection testConn = DriverManager.getConnection(
                connectionInfo.getJdbcUrl(),
                connectionInfo.getUsername(),
                connectionInfo.getPassword())) {
            boolean valid = testConn != null && !testConn.isClosed();
            logger.debug("Connection test result for {}: {}",
                connectionInfo.getDatabaseType(), valid);
            return valid;
        } catch (SQLException e) {
            logger.error("Connection test failed", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Connection test failed: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public boolean supports(ConnectionInfo connectionInfo) {
        return !(connectionInfo instanceof CsvConnectionInfo);
    }

    /**
     * Configures pool settings.
     */
    public void configurePool(int maxPoolSize, int minIdle, int connectionTimeoutSeconds) {
        this.maxPoolSize = Math.max(1, maxPoolSize);
        this.minIdle = Math.min(Math.max(1, minIdle), this.maxPoolSize);
        this.connectionTimeoutMs = Math.max(1000, connectionTimeoutSeconds * 1000);

        // Apply to existing datasource if active
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.setMaximumPoolSize(this.maxPoolSize);
            dataSource.setMinimumIdle(this.minIdle);
            dataSource.setConnectionTimeout(this.connectionTimeoutMs);
            logger.info("Applied runtime pool settings: max={}, minIdle={}, timeout={}ms",
                this.maxPoolSize, this.minIdle, this.connectionTimeoutMs);
        }
    }

    /**
     * Returns pool statistics for monitoring.
     */
    public String getPoolStats() {
        if (dataSource == null) {
            return "No active pool";
        }
        return String.format("Pool: %s, Active: %d, Idle: %d, Total: %d",
            dataSource.getPoolName(),
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections());
    }

    private void loadDriver(DatabaseType databaseType) throws QueryCraftException {
        if (driverLoadedMap.getOrDefault(databaseType, false)) {
            return;
        }

        try {
            logger.debug("Loading driver: {}", databaseType.getDriverClass());
            Class.forName(databaseType.getDriverClass());
            driverLoadedMap.put(databaseType, true);
            logger.info("Driver loaded successfully: {}", databaseType.getDriverClass());
        } catch (ClassNotFoundException e) {
            logger.error("JDBC Driver not found: {}", databaseType.getDriverClass(), e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.DRIVER_NOT_FOUND,
                "JDBC Driver not found: " + databaseType.getDriverClass() +
                ". Please ensure the driver library is included.",
                e
            );
        }
    }

    private void createConnectionPool(ConnectionInfo connectionInfo) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionInfo.getJdbcUrl());
        config.setUsername(connectionInfo.getUsername());
        config.setPassword(connectionInfo.getPassword());

        // Pool settings
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(Math.min(minIdle, maxPoolSize));

        // Timeouts
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(Math.min(5000, connectionTimeoutMs));
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME_MS);
        config.setValidationTimeout(DEFAULT_VALIDATION_TIMEOUT_MS);

        // Validation
        config.setConnectionTestQuery("SELECT 1");

        // Pool naming
        config.setPoolName("QueryCraft-" + connectionInfo.getDatabaseType().name());

        // Database-specific optimizations
        applyDatabaseSpecificSettings(config, connectionInfo.getDatabaseType());

        dataSource = new HikariDataSource(config);
        logger.info("Connection pool created for: {}@{}",
            connectionInfo.getDatabaseType(), connectionInfo.getHost());
    }

    private void applyDatabaseSpecificSettings(HikariConfig config, DatabaseType databaseType) {
        switch (databaseType) {
            case MYSQL -> {
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("allowMultiQueries", "true");
            }
            case POSTGRESQL -> {
                config.addDataSourceProperty("socketTimeout", "30");
            }
            case MSSQL -> {
                config.addDataSourceProperty("loginTimeout", "5");
                config.addDataSourceProperty("trustServerCertificate", "true");
            }
            default -> {
                // No additional settings for other database types
            }
        }
    }

    private void configureWindowsAuthDll() {
        String dllName = "mssql-jdbc_auth-12.6.1.x64.dll";
        String userDir = System.getProperty("user.dir");

        String[] searchPaths = {
            userDir + File.separator + "lib",
            userDir,
            "lib",
            ".",
            "target"
        };

        File foundDll = null;
        for (String path : searchPaths) {
            File dllFile = new File(path, dllName);
            if (dllFile.exists()) {
                foundDll = dllFile;
                break;
            }
        }

        if (foundDll != null) {
            String absolutePath = foundDll.getAbsolutePath();
            logger.info("Configuring MSSQL auth DLL from: {}", absolutePath);

            System.setProperty("mssql.auth.dll.name", absolutePath);

            try {
                System.load(absolutePath);
                logger.info("Successfully pre-loaded MSSQL auth DLL");
            } catch (UnsatisfiedLinkError e) {
                if (e.getMessage() != null && e.getMessage().contains("already loaded")) {
                    logger.debug("MSSQL auth DLL already loaded");
                } else {
                    logger.warn("Native load warning: {}", e.getMessage());
                }
            }
        } else {
            logger.warn("SQL Server auth DLL not found. Windows Authentication may fail.");
        }
    }
}
