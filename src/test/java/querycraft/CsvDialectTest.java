package querycraft;

import static org.junit.Assert.*;
import org.junit.Test;
import querycraft.dialect.CsvDialect;

public class CsvDialectTest {

    @Test
    public void testUrlGeneration() {
        CsvDialect dialect = new CsvDialect();
        String urlFormat = "jdbc:h2:mem:csvdb_%d;MODE=MySQL";
        
        // Single backslash should be handled
        String url = dialect.buildUrl("C:\\data", 0, "", false, urlFormat);
        assertTrue(url.contains("jdbc:h2:mem:csvdb_"));
        
        // Check formatting behavior
        String url2 = String.format(urlFormat, 123);
        assertEquals("jdbc:h2:mem:csvdb_123;MODE=MySQL", url2);
    }

    @Test
    public void testIdentifierEscaping() {
        CsvDialect dialect = new CsvDialect();
        // CsvDialect uses standard double quotes
        assertEquals("\"students\"", dialect.escapeIdentifier("students"));
        assertEquals("\"student\"\"code\"", dialect.escapeIdentifier("student\"code"));
    }

    @Test
    public void testBooleanFormatting() {
        CsvDialect dialect = new CsvDialect();
        assertEquals("TRUE", dialect.formatBoolean(true));
        assertEquals("FALSE", dialect.formatBoolean(false));
    }

    @Test
    public void testQueries() {
        CsvDialect dialect = new CsvDialect();
        // H2/CSV uses standard information_schema
        assertNotNull(dialect.getShowTablesQuery());
        assertTrue(dialect.getShowTablesQuery().contains("TABLES"));
        
        assertNotNull(dialect.getDescribeTableQuery("students"));
        assertTrue(dialect.getDescribeTableQuery("students").contains("COLUMNS"));
    }
}
