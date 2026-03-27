package querycraft.service;

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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing database connections with connection pooling.
 */
public class DatabaseConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionService.class);
    private static final int CONNECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 2;
    
    private static DatabaseConnectionService instance;
    private HikariDataSource dataSource;
    private ConnectionInfo currentConnectionInfo;
    private int configuredConnectionTimeoutMs = CONNECTION_TIMEOUT_MS;
    private int configuredMaxPoolSize = MAX_POOL_SIZE;
    private int configuredMinIdle = MIN_IDLE;
    private final Map<DatabaseType, Boolean> driverLoadedMap;
    private final java.util.List<ConnectionObserver> observers = new java.util.ArrayList<>();

    private DatabaseConnectionService() {
        this.driverLoadedMap = new HashMap<>();
    }

    public void addObserver(ConnectionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            logger.debug("Added connection observer: {}", observer.getClass().getSimpleName());
        }
    }

    public void removeObserver(ConnectionObserver observer) {
        observers.remove(observer);
        logger.debug("Removed connection observer: {}", observer.getClass().getSimpleName());
    }

    private void notifyConnected(ConnectionInfo info) {
        logger.info("Connection established to: {}@{}", info.getDatabaseType(), info.getHost());
        observers.forEach(o -> {
            try {
                o.onConnected(info);
            } catch (Exception e) {
                logger.error("Error notifying observer of connection", e);
            }
        });
    }

    private void notifyDisconnected() {
        logger.info("Disconnected from database");
        observers.forEach(o -> {
            try {
                o.onDisconnected();
            } catch (Exception e) {
                logger.error("Error notifying observer of disconnection", e);
            }
        });
    }

    private void notifyConnectionFailed(Exception e) {
        logger.error("Connection failed", e);
        observers.forEach(o -> {
            try {
                o.onConnectionFailed(e);
            } catch (Exception ex) {
                logger.error("Error notifying observer of connection failure", ex);
            }
        });
    }


    public static synchronized DatabaseConnectionService getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionService();
        }
        return instance;
    }

    /**
     * Load the JDBC driver for the specified database type.
     */
    public void loadDriver(DatabaseType databaseType) throws QueryCraftException {
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

    /**
     * Connect to a database using the provided connection info.
     */
    public Connection connect(ConnectionInfo connectionInfo) throws QueryCraftException {
        try {
            // Close existing connection pool if any
            disconnect();

            // Load driver
            loadDriver(connectionInfo.getDatabaseType());

            // Handle CSV connection specially (no pooling for H2 CSV)
            if (connectionInfo instanceof CsvConnectionInfo) {
                return connectToCsv((CsvConnectionInfo) connectionInfo);
            }

            // For MSSQL Windows Auth, ensure auth DLL is available
            if (connectionInfo instanceof MssqlConnectionInfo mssqlInfo) {
                if (mssqlInfo.isUseWindowsAuth()) {
                    configureWindowsAuthDll();
                }
            }

            // Create connection pool
            createConnectionPool(connectionInfo);
            currentConnectionInfo = connectionInfo;

            // Test connection
            try (Connection testConn = dataSource.getConnection()) {
                if (testConn == null || testConn.isClosed()) {
                    throw new QueryCraftException(
                        QueryCraftException.ErrorCode.CONNECTION_FAILED,
                        "Failed to establish connection"
                    );
                }
            }

            notifyConnected(connectionInfo);
            return dataSource.getConnection();
            
        } catch (Exception e) {
            logger.error("Database connection failed", e);
            notifyConnectionFailed(e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Failed to connect to database: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                e
            );
        }
    }

    /**
     * Create HikariCP connection pool.
     */
    private void createConnectionPool(ConnectionInfo connectionInfo) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionInfo.getJdbcUrl());
        config.setUsername(connectionInfo.getUsername());
        config.setPassword(connectionInfo.getPassword());
        
        // Pool settings
        config.setMaximumPoolSize(configuredMaxPoolSize);
        config.setMinimumIdle(Math.min(configuredMinIdle, configuredMaxPoolSize));
        
        // Fail fast if the first connection fails (prevents hangs on wrong database name)
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(5000); // 5 seconds for initial connection attempt
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // Pool name for monitoring
        config.setPoolName("QueryCraft-" + connectionInfo.getDatabaseType().name());
        
        // Additional properties for specific databases
        switch (connectionInfo.getDatabaseType()) {
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
            case CSV -> {
                // CSV connections are handled separately and do not use HikariCP.
            }
        }
        
        dataSource = new HikariDataSource(config);
        logger.info("Connection pool created for: {}@{}", 
            connectionInfo.getDatabaseType(), connectionInfo.getHost());
    }

    /**
     * Connect to CSV files in a folder using H2 Database.
     */
    private Connection connectToCsv(CsvConnectionInfo csvInfo) throws QueryCraftException {
        String jdbcUrl = csvInfo.getJdbcUrl();
        
        try {
            // Create H2 connection (no pooling for CSV)
            Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
            
            // Load all CSV files as tables
            try (Statement stmt = conn.createStatement()) {
                for (CsvConnectionInfo.CsvFileInfo csvFile : csvInfo.getCsvFiles()) {
                    String createTableSql = csvInfo.getCreateTableSql(csvFile);
                    logger.debug("Creating table from CSV: {}", csvFile.getTableName());
                    stmt.execute(createTableSql);
                }
            } catch (SQLException e) {
                // Close connection on error
                try {
                    conn.close();
                } catch (SQLException ignored) {}
                logger.error("Failed to load CSV files", e);
                throw new QueryCraftException(
                    QueryCraftException.ErrorCode.CONNECTION_FAILED,
                    "Failed to load CSV files: " + e.getMessage(),
                    e
                );
            }
            
            currentConnectionInfo = csvInfo;
            logger.info("CSV connection established with {} files", csvInfo.getCsvFileCount());
            notifyConnected(csvInfo);
            return conn;
            
        } catch (SQLException e) {
            logger.error("CSV connection failed", e);
            notifyConnectionFailed(e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Failed to connect to CSV: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Test a connection without storing it as the current connection.
     */
    public boolean testConnection(ConnectionInfo connectionInfo) throws QueryCraftException {
        try {
            loadDriver(connectionInfo.getDatabaseType());

            // For CSV, just check if folder exists and has CSV files
            if (connectionInfo instanceof CsvConnectionInfo) {
                CsvConnectionInfo csvInfo = (CsvConnectionInfo) connectionInfo;
                boolean valid = csvInfo.getCsvFileCount() > 0;
                logger.debug("CSV connection test: {} files found", csvInfo.getCsvFileCount());
                return valid;
            }

            try (Connection testConn = DriverManager.getConnection(
                    connectionInfo.getJdbcUrl(),
                    connectionInfo.getUsername(),
                    connectionInfo.getPassword())) {
                boolean valid = testConn != null && !testConn.isClosed();
                logger.debug("Connection test result: {}", valid);
                return valid;
            }
        } catch (SQLException e) {
            logger.error("Connection test failed", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Connection test failed: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Disconnect from the current database.
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing connection pool");
            dataSource.close();
            dataSource = null;
        }
        currentConnectionInfo = null;
        notifyDisconnected();
    }

    /**
     * Get the current active connection.
     */
    public Connection getCurrentConnection() throws QueryCraftException {
        if (currentConnectionInfo instanceof CsvConnectionInfo) {
            // For CSV, return a new connection each time
            try {
                return DriverManager.getConnection(currentConnectionInfo.getJdbcUrl(), "sa", "");
            } catch (SQLException e) {
                throw new QueryCraftException(
                    QueryCraftException.ErrorCode.CONNECTION_FAILED,
                    "Failed to get CSV connection: " + e.getMessage(),
                    e
                );
            }
        }
        
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

    /**
     * Check if there is an active connection.
     */
    public boolean isConnected() {
        if (currentConnectionInfo instanceof CsvConnectionInfo) {
            return currentConnectionInfo != null;
        }
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Get current connection info.
     */
    public ConnectionInfo getCurrentConnectionInfo() {
        return currentConnectionInfo;
    }

    /**
     * Validate that we have an active connection, throwing exception if not.
     */
    public void validateConnection() throws QueryCraftException {
        if (!isConnected()) {
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "No active database connection. Please connect to a database first."
            );
        }
    }

    /**
     * Get connection pool statistics (for monitoring).
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

    public void applyRuntimeSettings(int maxPoolSize, int connectionTimeoutSeconds) {
        configuredMaxPoolSize = Math.max(1, maxPoolSize);
        configuredMinIdle = Math.min(configuredMinIdle, configuredMaxPoolSize);
        configuredConnectionTimeoutMs = Math.max(1, connectionTimeoutSeconds) * 1000;

        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Applying runtime pool settings to active datasource");
            dataSource.setMaximumPoolSize(configuredMaxPoolSize);
            dataSource.setMinimumIdle(Math.min(configuredMinIdle, configuredMaxPoolSize));
            dataSource.setConnectionTimeout(configuredConnectionTimeoutMs);
        }
    }

    /**
     * Pre-load the mssql-jdbc_auth DLL for Windows Authentication.
     * Tells the Microsoft JDBC driver exactly where the DLL is using absolute path.
     * This allows Windows Auth to work from both IDE and run.bat.
     */
    private void configureWindowsAuthDll() {
        String dllName = "mssql-jdbc_auth-12.6.1.x64.dll";
        String userDir = System.getProperty("user.dir");
        
        // Search locations for the auth DLL (preferring those relative to project root)
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
            
            // 1. Tell driver exactly where the DLL is using absolute path property
            System.setProperty("mssql.auth.dll.name", absolutePath);
            
            // 2. Also pre-load it as a backup strategy
            try {
                System.load(absolutePath);
                logger.info("Successfully pre-loaded and configured MSSQL auth DLL");
            } catch (UnsatisfiedLinkError e) {
                if (e.getMessage() != null && e.getMessage().contains("already loaded")) {
                    logger.debug("MSSQL auth DLL already loaded (OK)");
                } else {
                    logger.warn("Native load warning (driver may still work via property): {}", e.getMessage());
                }
            }
        } else {
            logger.warn("SQL Server auth DLL ({}) not found. Windows Authentication may fail. "
                + "Please ensure it exists in the 'lib' folder.", dllName);
        }
    }
}
