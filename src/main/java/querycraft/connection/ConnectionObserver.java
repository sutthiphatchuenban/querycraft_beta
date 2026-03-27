package querycraft.connection;

import querycraft.model.ConnectionInfo;

/**
 * Observer interface for database connection status changes.
 */
public interface ConnectionObserver {
    /**
     * Called when a connection is successful.
     */
    void onConnected(ConnectionInfo info);
    
    /**
     * Called when disconnected from database.
     */
    void onDisconnected();
    
    /**
     * Called when a connection attempt fails.
     */
    void onConnectionFailed(Exception e);
}
