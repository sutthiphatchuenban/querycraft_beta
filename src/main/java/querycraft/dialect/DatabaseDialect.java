package querycraft.dialect;

/**
 * Interface for database-specific SQL dialect and behavior.
 */

public interface DatabaseDialect {
    
    String getShowTablesQuery();
    
    String getDescribeTableQuery(String tableName);
    
    String getBeginTransaction();
    
    String getCommitTransaction();
    
    String escapeIdentifier(String identifier);
    
    String formatBoolean(boolean value);
    
    String buildUrl(String host, int port, String database, boolean useSSL, String baseUrlFormat);
    
    String getSelectAllWithLimitQuery(String tableName, int limit);
    
    default String getPreInsertSql(String tableName) { return ""; }
    
    default String getPostInsertSql(String tableName) { return ""; }
}
