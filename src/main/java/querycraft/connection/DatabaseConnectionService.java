package querycraft.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.exception.QueryCraftException;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing database connections.
 * This is a facade that delegates to appropriate ConnectionManager implementations.
 * Supports connection pooling for standard databases and file-based connections for CSV.
 *
 * <p>This class maintains backward compatibility while internally using the
 * ConnectionManager abstraction for better separation of concerns.</p>
 */
public class DatabaseConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionService.class);

    private static volatile DatabaseConnectionService instance;
    private static final Object INSTANCE_LOCK = new Object();

    private final List<ConnectionObserver> observers = new ArrayList<>();
    private final PooledConnectionManager pooledManager;
    private final CsvConnectionManager csvManager;

    private ConnectionInfo currentConnectionInfo;
    private ConnectionManager activeManager;

    private DatabaseConnectionService() {
        this.pooledManager = new PooledConnectionManager();
        this.csvManager = new CsvConnectionManager();
    }

    /**
     * Returns the singleton instance of DatabaseConnectionService.
     *
     * @return the singleton instance
     */
    public static DatabaseConnectionService getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new DatabaseConnectionService();
                }
            }
        }
        return instance;
    }

    /**
     * Adds an observer to receive connection state change notifications.
     *
     * @param observer the observer to add
     */
    public void addObserver(ConnectionObserver observer) {
        synchronized (observers) {
            if (!observers.contains(observer)) {
                observers.add(observer);
                logger.debug("Added connection observer: {}", observer.getClass().getSimpleName());
            }
        }
    }

    /**
     * Removes an observer from receiving notifications.
     *
     * @param observer the observer to remove
     */
    public void removeObserver(ConnectionObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
            logger.debug("Removed connection observer: {}", observer.getClass().getSimpleName());
        }
    }

    /**
     * Establishes a connection using the provided connection info.
     * Automatically selects the appropriate connection manager based on connection type.
     *
     * @param connectionInfo the connection parameters
     * @return an active database connection
     * @throws QueryCraftException if connection fails
     */
    public Connection connect(ConnectionInfo connectionInfo) throws QueryCraftException {
        // Disconnect any existing connection first
        disconnect();

        try {
            // Select appropriate manager
            ConnectionManager manager = selectManager(connectionInfo);

            // Establish connection
            Connection conn = manager.connect(connectionInfo);

            // Update state
            this.activeManager = manager;
            this.currentConnectionInfo = connectionInfo;

            // Notify observers
            notifyConnected(connectionInfo);

            return conn;

        } catch (QueryCraftException e) {
            notifyConnectionFailed(e);
            throw e;
        } catch (Exception e) {
            QueryCraftException wrapped = new QueryCraftException(
                QueryCraftException.ErrorCode.CONNECTION_FAILED,
                "Unexpected error during connection: " + e.getMessage(),
                e
            );
            notifyConnectionFailed(wrapped);
            throw wrapped;
        }
    }

    /**
     * Disconnects from the current database.
     */
    public void disconnect() {
        if (activeManager != null) {
            activeManager.disconnect();
            activeManager = null;
        }
        currentConnectionInfo = null;
        notifyDisconnected();
    }

    /**
     * Returns the current active connection.
     *
     * @return the current connection
     * @throws QueryCraftException if no active connection exists
     */
    public Connection getCurrentConnection() throws QueryCraftException {
        validateConnection();
        return activeManager.getConnection();
    }

    /**
     * Tests a connection without storing it as the current connection.
     *
     * @param connectionInfo the connection parameters to test
     * @return true if connection succeeds
     * @throws QueryCraftException if connection fails
     */
    public boolean testConnection(ConnectionInfo connectionInfo) throws QueryCraftException {
        ConnectionManager manager = selectManager(connectionInfo);
        return manager.testConnection(connectionInfo);
    }

    /**
     * Checks if there is an active connection.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return activeManager != null && activeManager.isConnected();
    }

    /**
     * Returns the current connection info.
     *
     * @return the current connection info, or null if not connected
     */
    public ConnectionInfo getCurrentConnectionInfo() {
        return currentConnectionInfo;
    }

    /**
     * Validates that there is an active connection.
     *
     * @throws QueryCraftException if not connected
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
     * Returns connection pool statistics.
     *
     * @return pool statistics string, or "No active pool" if not using pooling
     */
    public String getPoolStats() {
        if (activeManager instanceof PooledConnectionManager) {
            return ((PooledConnectionManager) activeManager).getPoolStats();
        }
        return "No active pool (CSV connection or not connected)";
    }

    /**
     * Applies runtime pool settings. Only affects pooled connections.
     *
     * @param maxPoolSize maximum pool size
     * @param connectionTimeoutSeconds connection timeout in seconds
     */
    public void applyRuntimeSettings(int maxPoolSize, int connectionTimeoutSeconds) {
        pooledManager.configurePool(maxPoolSize, -1, connectionTimeoutSeconds);

        // Apply to active pooled connection if exists
        if (activeManager instanceof PooledConnectionManager) {
            ((PooledConnectionManager) activeManager).configurePool(maxPoolSize, -1, connectionTimeoutSeconds);
        }
    }

    // Private helper methods

    private ConnectionManager selectManager(ConnectionInfo connectionInfo) throws QueryCraftException {
        if (connectionInfo instanceof CsvConnectionInfo) {
            return csvManager;
        }
        return pooledManager;
    }

    private void notifyConnected(ConnectionInfo info) {
        logger.info("Connection established to: {}@{}", info.getDatabaseType(), info.getHost());
        synchronized (observers) {
            for (ConnectionObserver o : observers) {
                try {
                    o.onConnected(info);
                } catch (Exception e) {
                    logger.error("Error notifying observer of connection", e);
                }
            }
        }
    }

    private void notifyDisconnected() {
        logger.info("Disconnected from database");
        synchronized (observers) {
            for (ConnectionObserver o : observers) {
                try {
                    o.onDisconnected();
                } catch (Exception e) {
                    logger.error("Error notifying observer of disconnection", e);
                }
            }
        }
    }

    private void notifyConnectionFailed(Exception e) {
        logger.error("Connection failed", e);
        synchronized (observers) {
            for (ConnectionObserver o : observers) {
                try {
                    o.onConnectionFailed(e);
                } catch (Exception ex) {
                    logger.error("Error notifying observer of connection failure", ex);
                }
            }
        }
    }
}
