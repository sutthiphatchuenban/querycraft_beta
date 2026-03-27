package querycraft;

import static org.junit.Assert.*;
import org.junit.Test;
import querycraft.query.QueryExecutorService;
import querycraft.export.CsvExporter;
import querycraft.model.DatabaseType;

public class AppTest {
    
    private final QueryExecutorService executor = new QueryExecutorService();

    @SuppressWarnings("deprecation")
    @Test
    public void testQueryValidationSafe() {
        assertTrue(executor.validateQuery("SELECT * FROM users").isValid());
        assertTrue(executor.validateQuery("SELECT (SELECT 1) as test").isValid());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testQueryValidationDangerous() {
        // Direct drop
        assertFalse(executor.validateQuery("DROP TABLE users").isValid());
        
        // Comment bypass attempts
        assertFalse(executor.validateQuery("D/**/ROP TABLE users").isValid());
        assertFalse(executor.validateQuery("-- nothing\n DROP TABLE users").isValid());
        
        // Truncate
        assertFalse(executor.validateQuery("TRUNCATE TABLE logs").isValid());
    }

    @Test
    public void testSqlTypeDetection() {
        assertTrue(executor.isSelectQuery("SELECT * FROM test"));
        assertTrue(executor.isSelectQuery("SHOW TABLES"));
        assertTrue(executor.isSelectQuery("WITH cte AS (SELECT 1) SELECT * FROM cte"));
        
        assertTrue(executor.isDeleteQuery("DELETE FROM users WHERE id = 1"));
        assertTrue(executor.isDeleteQuery("-- delete request\n DELETE FROM users"));
    }

    @Test
    public void testFilenameGeneration() {
        String filename = CsvExporter.generateFilename("test", "csv");
        assertTrue(filename.startsWith("test_"));
        assertTrue(filename.endsWith(".csv"));
        assertTrue(filename.length() > 15);
    }
    
    @Test
    public void testIdentifierEscaping() {
        assertEquals("`table`", DatabaseType.MYSQL.escapeIdentifier("table"));
        assertEquals("\"table\"", DatabaseType.POSTGRESQL.escapeIdentifier("table"));
        assertEquals("[table]", DatabaseType.MSSQL.escapeIdentifier("table"));
        
        // Test escaping quotes inside identifiers
        assertEquals("`a``b`", DatabaseType.MYSQL.escapeIdentifier("a`b"));
        assertEquals("\"a\"\"b\"", DatabaseType.POSTGRESQL.escapeIdentifier("a\"b"));
        assertEquals("[a]]b]", DatabaseType.MSSQL.escapeIdentifier("a]b"));
    }
}
