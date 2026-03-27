package querycraft.dialect;

/**
 * PostgreSQL specific SQL dialect.
 */
public class PostgreSqlDialect implements DatabaseDialect {
    
    @Override
    public String getShowTablesQuery() {
        return "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'public' AND table_type IN ('BASE TABLE', 'VIEW') ORDER BY table_name";
    }

    @Override
    public String getDescribeTableQuery(String tableName) {
        return String.format("SELECT column_name, data_type, character_maximum_length, is_nullable, column_default FROM information_schema.columns WHERE table_name = '%s' ORDER BY ordinal_position", 
            tableName.replace("'", "''"));
    }

    @Override
    public String getSelectAllWithLimitQuery(String tableName, int limit) {
        return String.format("SELECT * FROM %s LIMIT %d", escapeIdentifier(tableName), limit);
    }

    @Override
    public String getBeginTransaction() {
        return "BEGIN;";
    }

    @Override
    public String getCommitTransaction() {
        return "COMMIT;";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return "";
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String formatBoolean(boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    @Override
    public String buildUrl(String host, int port, String database, boolean useSSL, String baseUrlFormat) {
        String url = String.format(baseUrlFormat, host, port, database);
        if (useSSL) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }
        return url;
    }
}
