package querycraft.service.handler;

import querycraft.model.QueryResult;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy interface for handling different types of SQL queries.
 */
public interface QueryHandler {
    
    /**
     * Determine if this handler can process the given SQL.
     */
    boolean canHandle(String sql);
    
    /**
     * Execute the query and return a result.
     */
    QueryResult handle(String sql, Connection conn) throws SQLException;
    
    /**
     * Get a display category name for this handler.
     */
    String getCategory();
}
