package querycraft.service;

import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.DatabaseType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing database connections.
 */
public class DatabaseConnectionService {

    private static DatabaseConnectionService instance;
    private Connection currentConnection;
    private ConnectionInfo currentConnectionInfo;
    private final Map<DatabaseType, Boolean> driverLoadedMap;
    private final java.util.List<ConnectionObserver> observers = new java.util.ArrayList<>();

    private DatabaseConnectionService() {
        this.driverLoadedMap = new HashMap<>();
    }

    public void addObserver(ConnectionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(ConnectionObserver observer) {
        observers.remove(observer);
    }

    private void notifyConnected(ConnectionInfo info) {
        observers.forEach(o -> o.onConnected(info));
    }

    private void notifyDisconnected() {
        observers.forEach(ConnectionObserver::onDisconnected);
    }

    private void notifyConnectionFailed(Exception e) {
        observers.forEach(o -> o.onConnectionFailed(e));
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
    public void loadDriver(DatabaseType databaseType) throws SQLException {
        if (driverLoadedMap.getOrDefault(databaseType, false)) {
            return;
        }

        try {
            Class.forName(databaseType.getDriverClass());
            driverLoadedMap.put(databaseType, true);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver not found: " + databaseType.getDriverClass() +
                    ". Please ensure the driver library is included.", e);
        }
    }

    /**
     * Connect to a database using the provided connection info.
     */
    public Connection connect(ConnectionInfo connectionInfo) throws SQLException {
        try {
            // Close existing connection if any
            disconnect();

            // Load driver
            loadDriver(connectionInfo.getDatabaseType());

            // Handle CSV connection specially
            if (connectionInfo instanceof CsvConnectionInfo) {
                return connectToCsv((CsvConnectionInfo) connectionInfo);
            }

            // Create connection
            String jdbcUrl = connectionInfo.getJdbcUrl();
            currentConnection = java.sql.DriverManager.getConnection(
                    jdbcUrl,
                    connectionInfo.getUsername(),
                    connectionInfo.getPassword()
            );
            currentConnectionInfo = connectionInfo;

            notifyConnected(connectionInfo);
            return currentConnection;
        } catch (SQLException e) {
            notifyConnectionFailed(e);
            throw e;
        }
    }

    /**
     * Connect to CSV files in a folder using H2 Database.
     */
    private Connection connectToCsv(CsvConnectionInfo csvInfo) throws SQLException {
        String jdbcUrl = csvInfo.getJdbcUrl();
        
        // Create H2 connection
        currentConnection = DriverManager.getConnection(jdbcUrl, "sa", "");
        
        // Load all CSV files as tables
        try (Statement stmt = currentConnection.createStatement()) {
            for (CsvConnectionInfo.CsvFileInfo csvFile : csvInfo.getCsvFiles()) {
                String createTableSql = csvInfo.getCreateTableSql(csvFile);
                stmt.execute(createTableSql);
            }
        } catch (SQLException e) {
            // Close connection on error
            try {
                currentConnection.close();
            } catch (SQLException ignored) {}
            currentConnection = null;
            throw new SQLException("Failed to load CSV files: " + e.getMessage(), e);
        }
        
        currentConnectionInfo = csvInfo;
        notifyConnected(csvInfo);
        return currentConnection;
    }

    /**
     * Test a connection without storing it as the current connection.
     */
    public boolean testConnection(ConnectionInfo connectionInfo) throws SQLException {
        loadDriver(connectionInfo.getDatabaseType());

        // For CSV, just check if folder exists and has CSV files
        if (connectionInfo instanceof CsvConnectionInfo) {
            CsvConnectionInfo csvInfo = (CsvConnectionInfo) connectionInfo;
            return csvInfo.getCsvFileCount() > 0;
        }

        try (Connection testConn = DriverManager.getConnection(
                connectionInfo.getJdbcUrl(),
                connectionInfo.getUsername(),
                connectionInfo.getPassword())) {
            return testConn != null && !testConn.isClosed();
        }
    }

    /**
     * Disconnect from the current database.
     */
    public void disconnect() {
        if (currentConnection != null) {
            try {
                if (!currentConnection.isClosed()) {
                    currentConnection.close();
                }
            } catch (SQLException e) {
                // Log error but don't throw - we're closing anyway
                System.err.println("Error closing connection: " + e.getMessage());
            } finally {
                currentConnection = null;
                currentConnectionInfo = null;
                notifyDisconnected();
            }
        }
    }

    /**
     * Get the current active connection.
     */
    public Connection getCurrentConnection() throws SQLException {
        if (currentConnection == null || currentConnection.isClosed()) {
            throw new SQLException("No active database connection. Please connect first.");
        }
        return currentConnection;
    }

    /**
     * Check if there is an active connection.
     */
    public boolean isConnected() {
        try {
            return currentConnection != null && !currentConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
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
    public void validateConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("No active database connection. Please connect to a database first.");
        }
    }
}
