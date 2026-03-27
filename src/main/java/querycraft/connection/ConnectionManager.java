package querycraft.connection;

import querycraft.exception.QueryCraftException;
import querycraft.model.ConnectionInfo;

import java.sql.Connection;

/**
 * Interface for managing database connections.
 * Implementations handle specific connection strategies (pooled, file-based, etc.).
 */
public interface ConnectionManager {

    /**
     * Establishes a connection using the provided connection info.
     *
     * @param connectionInfo the connection parameters
     * @return an active database connection
     * @throws QueryCraftException if connection fails
     */
    Connection connect(ConnectionInfo connectionInfo) throws QueryCraftException;

    /**
     * Disconnects and releases all resources.
     */
    void disconnect();

    /**
     * Returns the current active connection.
     *
     * @return the current connection, or throws if not connected
     * @throws QueryCraftException if no active connection exists
     */
    Connection getConnection() throws QueryCraftException;

    /**
     * Tests if a connection can be established without storing it.
     *
     * @param connectionInfo the connection parameters to test
     * @return true if connection succeeds
     * @throws QueryCraftException if connection fails
     */
    boolean testConnection(ConnectionInfo connectionInfo) throws QueryCraftException;

    /**
     * Checks if currently connected.
     *
     * @return true if there is an active connection
     */
    boolean isConnected();

    /**
     * Returns the type of connections this manager handles.
     *
     * @return true if this manager can handle the given connection info
     */
    boolean supports(ConnectionInfo connectionInfo);
}
