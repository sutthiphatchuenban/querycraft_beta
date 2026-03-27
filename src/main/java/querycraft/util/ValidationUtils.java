package querycraft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility class for SQL validation and normalization.
 * Consolidates SQL validation logic that was previously duplicated across the codebase.
 */
public final class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    // Dangerous SQL patterns for security validation
    private static final String[] DANGEROUS_PATTERNS = {
        "\\bDROP\\s+(TABLE|DATABASE|INDEX|VIEW|PROCEDURE|FUNCTION|TRIGGER|SCHEMA)",
        "\\bTRUNCATE\\s+TABLE",
        "\\bALTER\\s+(DATABASE|SYSTEM|SCHEMA)",
        "\\bGRANT\\s+ALL",
        "\\bREVOKE\\s+ALL",
        "\\bSHUTDOWN",
        "\\bKILL\\s+\\d",
        ";\\s*DROP",
        ";\\s*DELETE\\s+FROM",
        ";\\s*TRUNCATE"
    };

    // Read query detection patterns
    private static final String[] READ_QUERY_PREFIXES = {
        "SELECT", "WITH", "SHOW", "DESCRIBE", "EXPLAIN", "DESC"
    };

    // Write query detection patterns
    private static final String[] WRITE_QUERY_PREFIXES = {
        "INSERT", "UPDATE", "DELETE"
    };

    private ValidationUtils() {
        // Prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Normalizes SQL by removing comments and extra whitespace for analysis.
     * This is a safe operation that doesn't modify the actual SQL execution.
     *
     * @param sql the SQL to normalize
     * @return normalized SQL in uppercase
     */
    public static String normalizeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        String normalized = sql;

        // Remove block comments /* ... */
        normalized = normalized.replaceAll("/\\*[\\s\\S]*?\\*/", "");

        // Remove line comments -- ...
        normalized = normalized.replaceAll("--.*", "");

        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized.toUpperCase();
    }

    /**
     * Checks if SQL is a read query (SELECT, SHOW, DESCRIBE, etc.).
     *
     * @param sql the SQL to check
     * @return true if it's a read query
     */
    public static boolean isReadQuery(String sql) {
        String normalized = normalizeSql(sql);
        if (normalized.isEmpty()) {
            return false;
        }

        for (String prefix : READ_QUERY_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if SQL is a write query (INSERT, UPDATE, DELETE).
     *
     * @param sql the SQL to check
     * @return true if it's a write query
     */
    public static boolean isWriteQuery(String sql) {
        String normalized = normalizeSql(sql);
        if (normalized.isEmpty()) {
            return false;
        }

        for (String prefix : WRITE_QUERY_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if SQL is specifically a DELETE query.
     *
     * @param sql the SQL to check
     * @return true if it's a DELETE query
     */
    public static boolean isDeleteQuery(String sql) {
        if (sql == null) {
            return false;
        }
        String normalized = normalizeSql(sql);
        return normalized.startsWith("DELETE");
    }

    /**
     * Validates SQL for dangerous operations.
     * Returns a validation result with status and message.
     *
     * @param sql the SQL to validate
     * @return ValidationResult containing validation status and message
     */
    public static ValidationResult validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ValidationResult(false, "Query cannot be empty");
        }

        String normalized = normalizeSql(sql);

        // Check for empty query after removing comments
        if (normalized.isEmpty()) {
            return new ValidationResult(false, "Query contains only comments");
        }

        // Check for dangerous patterns
        for (String pattern : DANGEROUS_PATTERNS) {
            if (Pattern.compile(".*" + pattern + ".*", Pattern.CASE_INSENSITIVE).matcher(normalized).matches()) {
                logger.warn("Dangerous SQL pattern detected: {}", pattern);
                return new ValidationResult(false,
                    "Potentially dangerous SQL pattern detected. Operation not allowed for safety.");
            }
        }

        return new ValidationResult(true, null);
    }

    /**
     * Checks if SQL contains only comments (no executable SQL).
     *
     * @param sql the SQL to check
     * @return true if it contains only comments
     */
    public static boolean isCommentOnly(String sql) {
        if (sql == null) {
            return true;
        }
        return normalizeSql(sql).isEmpty();
    }

    /**
     * Result of SQL validation.
     */
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

        @Override
        public String toString() {
            return "ValidationResult{valid=" + valid + ", message='" + message + "'}";
        }
    }
}
