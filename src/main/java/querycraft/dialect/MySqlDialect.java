package querycraft.dialect;

/**
 * MySQL specific SQL dialect.
 */
public class MySqlDialect implements DatabaseDialect {
    
    @Override
    public String getShowTablesQuery() {
        return "SHOW FULL TABLES";
    }

    @Override
    public String getDescribeTableQuery(String tableName) {
        return String.format("DESCRIBE %s", tableName);
    }

    @Override
    public String getBeginTransaction() {
        return "START TRANSACTION;";
    }

    @Override
    public String getCommitTransaction() {
        return "COMMIT;";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return "";
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String formatBoolean(boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    @Override
    public String buildUrl(String host, int port, String database, boolean useSSL, String baseUrlFormat) {
        String url = String.format(baseUrlFormat, host, port, database);
        if (useSSL) {
            url = url.replace("useSSL=false", "useSSL=true");
        }
        return url;
    }
}
