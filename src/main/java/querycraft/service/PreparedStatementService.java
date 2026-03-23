package querycraft.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querycraft.exception.QueryCraftException;
import querycraft.model.ColumnInfo;
import querycraft.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for executing parameterized queries with PreparedStatement.
 */
public class PreparedStatementService {

    private static final Logger logger = LoggerFactory.getLogger(PreparedStatementService.class);
    private final DatabaseConnectionService connectionService;
    
    // Pattern to match :paramName or ? placeholders
    private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
    private static final Pattern POSITIONAL_PARAM_PATTERN = Pattern.compile("\\?");

    public PreparedStatementService() {
        this.connectionService = DatabaseConnectionService.getInstance();
    }

    /**
     * Extract parameter names from SQL with named parameters (:paramName).
     * @param sql the SQL query with named parameters
     * @return list of parameter names in order of appearance
     */
    public List<String> extractNamedParameters(String sql) {
        List<String> params = new ArrayList<>();
        String sanitizedSql = sanitizeSqlForParameterParsing(sql);
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sanitizedSql);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (!params.contains(paramName)) {
                params.add(paramName);
            }
        }

        return params;
    }

    /**
     * Count positional parameters (?) in SQL.
     * @param sql the SQL query
     * @return number of ? placeholders
     */
    public int countPositionalParameters(String sql) {
        String sanitizedSql = sanitizeSqlForParameterParsing(sql);
        Matcher matcher = POSITIONAL_PARAM_PATTERN.matcher(sanitizedSql);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Convert named parameters (:paramName) to positional parameters (?).
     * @param sql SQL with named parameters
     * @param paramNames list to populate with parameter names in order
     * @return SQL with ? placeholders
     */
    public String convertNamedToPositional(String sql, List<String> paramNames) {
        paramNames.clear();
        String sanitizedSql = sanitizeSqlForParameterParsing(sql);
        StringBuffer result = new StringBuffer();
        Matcher matcher = NAMED_PARAM_PATTERN.matcher(sanitizedSql);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            paramNames.add(paramName);
            matcher.appendReplacement(result, "?");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String sanitizeSqlForParameterParsing(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(sql);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sanitized.length(); i++) {
            char current = sanitized.charAt(i);
            char next = i + 1 < sanitized.length() ? sanitized.charAt(i + 1) : '\0';

            if (!inSingleQuote && !inDoubleQuote && !inBlockComment && current == '-' && next == '-') {
                inLineComment = true;
                sanitized.setCharAt(i, ' ');
                sanitized.setCharAt(i + 1, ' ');
                i++;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inLineComment && current == '/' && next == '*') {
                inBlockComment = true;
                sanitized.setCharAt(i, ' ');
                sanitized.setCharAt(i + 1, ' ');
                i++;
                continue;
            }

            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                } else {
                    sanitized.setCharAt(i, ' ');
                }
                continue;
            }

            if (inBlockComment) {
                sanitized.setCharAt(i, ' ');
                if (current == '*' && next == '/') {
                    sanitized.setCharAt(i + 1, ' ');
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inDoubleQuote && current == '\'') {
                inSingleQuote = !inSingleQuote;
                sanitized.setCharAt(i, ' ');
                continue;
            }

            if (!inSingleQuote && current == '"') {
                inDoubleQuote = !inDoubleQuote;
                sanitized.setCharAt(i, ' ');
                continue;
            }

            if (inSingleQuote || inDoubleQuote) {
                sanitized.setCharAt(i, ' ');
            }
        }

        return sanitized.toString();
    }

    /**
     * Execute a prepared statement query with positional parameters.
     * @param sql SQL with ? placeholders
     * @param parameters values for parameters
     * @return QueryResult
     */
    public QueryResult executeQuery(String sql, Object... parameters) throws QueryCraftException {
        connectionService.validateConnection();
        
        try (Connection conn = connectionService.getCurrentConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                setParameter(pstmt, i + 1, parameters[i]);
            }
            
            logger.debug("Executing prepared query with {} parameters", parameters.length);
            
            // Determine if it's a SELECT or UPDATE
            boolean isResultSet = pstmt.execute();
            
            if (isResultSet) {
                return processResultSet(pstmt.getResultSet());
            } else {
                return processUpdateCount(pstmt.getUpdateCount());
            }
            
        } catch (SQLException e) {
            logger.error("Prepared statement execution failed", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                "Query execution failed: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Execute a prepared statement with named parameters.
     * @param sql SQL with :paramName placeholders
     * @param paramValues map of parameter names to values
     * @return QueryResult
     */
    public QueryResult executeQueryWithNamedParams(String sql, java.util.Map<String, Object> paramValues) 
            throws QueryCraftException {
        
        List<String> paramNames = new ArrayList<>();
        String positionalSql = convertNamedToPositional(sql, paramNames);
        
        Object[] values = new Object[paramNames.size()];
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            if (!paramValues.containsKey(paramName)) {
                throw new QueryCraftException(
                    QueryCraftException.ErrorCode.QUERY_VALIDATION_FAILED,
                    "Missing parameter value for: " + paramName
                );
            }
            values[i] = paramValues.get(paramName);
        }
        
        return executeQuery(positionalSql, values);
    }

    /**
     * Batch execute prepared statements for bulk operations.
     * @param sql SQL with ? placeholders
     * @param batch list of parameter arrays for each batch
     * @return array of update counts
     */
    public int[] executeBatch(String sql, List<Object[]> batch) throws QueryCraftException {
        connectionService.validateConnection();
        
        try (Connection conn = connectionService.getCurrentConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (Object[] params : batch) {
                for (int i = 0; i < params.length; i++) {
                    setParameter(pstmt, i + 1, params[i]);
                }
                pstmt.addBatch();
            }
            
            logger.debug("Executing batch with {} statements", batch.size());
            return pstmt.executeBatch();
            
        } catch (SQLException e) {
            logger.error("Batch execution failed", e);
            throw new QueryCraftException(
                QueryCraftException.ErrorCode.QUERY_EXECUTION_FAILED,
                "Batch execution failed: " + e.getMessage(),
                e
            );
        }
    }

    private void setParameter(PreparedStatement pstmt, int index, Object value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            pstmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            pstmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            pstmt.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            pstmt.setDouble(index, (Double) value);
        } else if (value instanceof java.sql.Date) {
            pstmt.setDate(index, (java.sql.Date) value);
        } else if (value instanceof java.sql.Timestamp) {
            pstmt.setTimestamp(index, (java.sql.Timestamp) value);
        } else if (value instanceof java.sql.Time) {
            pstmt.setTime(index, (java.sql.Time) value);
        } else if (value instanceof Boolean) {
            pstmt.setBoolean(index, (Boolean) value);
        } else {
            pstmt.setObject(index, value);
        }
    }

    private QueryResult processResultSet(ResultSet rs) throws SQLException {
        QueryResult result = new QueryResult();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Extract columns
        List<ColumnInfo> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            ColumnInfo col = new ColumnInfo();
            col.setName(metaData.getColumnLabel(i));
            col.setTypeName(metaData.getColumnTypeName(i));
            col.setSqlType(metaData.getColumnType(i));
            col.setDisplaySize(metaData.getColumnDisplaySize(i));
            col.setNullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columns.add(col);
        }
        result.setColumns(columns);
        
        // Extract rows (with limit)
        List<Object[]> rows = new ArrayList<>();
        int maxRows = 10000; // Configurable
        int rowCount = 0;
        
        while (rs.next() && rowCount < maxRows) {
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            rows.add(row);
            rowCount++;
        }
        
        result.setRows(rows);
        result.setSelectQuery(true);
        
        return result;
    }

    private QueryResult processUpdateCount(int updateCount) {
        QueryResult result = new QueryResult();
        result.setAffectedRows(updateCount);
        result.setSelectQuery(false);
        return result;
    }

    /**
     * Validate parameter values against expected types.
     * @param sql SQL query
     * @param tableName table name for metadata lookup
     * @param paramValues parameter values
     * @return validation result
     */
    public ValidationResult validateParameters(String sql, String tableName, 
                                                java.util.Map<String, Object> paramValues) {
        // Basic validation - check all named parameters have values
        List<String> requiredParams = extractNamedParameters(sql);
        
        for (String param : requiredParams) {
            if (!paramValues.containsKey(param)) {
                return new ValidationResult(false, "Missing value for parameter: " + param);
            }
        }
        
        return new ValidationResult(true, null);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
