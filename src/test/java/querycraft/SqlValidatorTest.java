package querycraft;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import querycraft.service.QueryExecutorService;

public class SqlValidatorTest {

    private QueryExecutorService executor;

    @Before
    public void setup() {
        executor = new QueryExecutorService();
    }

    @Test
    public void testValidQueries() {
        assertTrue(executor.validateQuery("SELECT * FROM students").isValid());
        assertTrue(executor.validateQuery("SELECT id, name FROM students WHERE id = 1").isValid());
        assertTrue(executor.validateQuery("UPDATE students SET name = 'Test' WHERE id = 1").isValid());
        assertTrue(executor.validateQuery("DELETE FROM students WHERE id = 99").isValid());
        assertTrue(executor.validateQuery("SHOW TABLES").isValid());
        assertTrue(executor.validateQuery("DESCRIBE students").isValid());
    }

    @Test
    public void testDangerousDrop() {
        QueryExecutorService.ValidationResult result = executor.validateQuery("DROP TABLE students");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Potentially dangerous SQL pattern detected"));
    }

    @Test
    public void testDangerousTruncate() {
        QueryExecutorService.ValidationResult result = executor.validateQuery("TRUNCATE TABLE logs");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Potentially dangerous SQL pattern detected"));
    }

    @Test
    public void testCommentBypassAttempts() {
        // Obfuscated DROP
        assertFalse(executor.validateQuery("D/**/ROP TABLE x").isValid());
        assertFalse(executor.validateQuery("DROP /* inner comment */ TABLE x").isValid());
        assertFalse(executor.validateQuery("-- leading comment\nDROP TABLE x").isValid());
    }

    @Test
    public void testCaseInsensitivity() {
        assertFalse(executor.validateQuery("drop table students").isValid());
        assertFalse(executor.validateQuery("TrUnCaTe TaBlE logs").isValid());
    }

    @Test
    public void testEmptyAndNull() {
        assertFalse(executor.validateQuery("").isValid());
        assertFalse(executor.validateQuery("   ").isValid());
        assertFalse(executor.validateQuery(null).isValid());
    }
}
