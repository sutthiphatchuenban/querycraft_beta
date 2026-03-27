package querycraft;

import org.junit.Test;
import querycraft.util.ValidationUtils;

import static org.junit.Assert.*;

public class ValidationUtilsTest {

    @Test
    public void testNormalizeSqlRemovesCommentsAndWhitespace() {
        String sql = "  SELECT  *  FROM users -- comment\n WHERE id = 1  ";
        String normalized = ValidationUtils.normalizeSql(sql);
        assertEquals("SELECT * FROM USERS WHERE ID = 1", normalized);
    }

    @Test
    public void testIsReadQuery() {
        assertTrue(ValidationUtils.isReadQuery("SELECT * FROM users"));
        assertTrue(ValidationUtils.isReadQuery("show tables"));
        assertFalse(ValidationUtils.isReadQuery("UPDATE users SET name='x'"));
    }

    @Test
    public void testIsWriteQuery() {
        assertTrue(ValidationUtils.isWriteQuery("INSERT INTO users VALUES (1)"));
        assertTrue(ValidationUtils.isWriteQuery("DELETE FROM users"));
        assertFalse(ValidationUtils.isWriteQuery("SELECT * FROM users"));
    }

    @Test
    public void testIsDeleteQuery() {
        assertTrue(ValidationUtils.isDeleteQuery("DELETE FROM users"));
        assertFalse(ValidationUtils.isDeleteQuery("UPDATE users SET x=1"));
    }

    @Test
    public void testValidateSqlDangerous() {
        ValidationUtils.ValidationResult result = ValidationUtils.validateSql("DROP TABLE users");
        assertFalse(result.isValid());
        assertNotNull(result.getMessage());
    }

    @Test
    public void testValidateSqlSafe() {
        ValidationUtils.ValidationResult result = ValidationUtils.validateSql("SELECT * FROM users");
        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @Test
    public void testIsCommentOnly() {
        assertTrue(ValidationUtils.isCommentOnly("-- comment only"));
        assertTrue(ValidationUtils.isCommentOnly(null));
        assertFalse(ValidationUtils.isCommentOnly("SELECT 1"));
    }
}
