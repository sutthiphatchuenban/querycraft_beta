package querycraft;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import querycraft.model.QueryResult;
import querycraft.service.handler.SelectHandler;
import querycraft.service.handler.DeleteHandler;
import querycraft.service.handler.GenericHandler;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryHandlerIntegrationTest {

    private Connection conn;

    @Before
    public void setup() throws SQLException {
        // Use H2 in-memory for testing
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_data (id INT PRIMARY KEY, name VARCHAR(255))");
            for (int i = 1; i <= 200; i++) {
                stmt.execute(String.format("INSERT INTO test_data VALUES (%d, 'Item %d')", i, i));
            }
        }
    }

    @After
    public void tearDown() throws SQLException {
        if (conn != null) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE test_data");
            }
            conn.close();
        }
    }

    @Test
    public void testSelectHandlerFullResults() throws SQLException {
        SelectHandler handler = new SelectHandler();
        // Request more rows than available (200 total)
        QueryResult result = handler.handle("SELECT * FROM test_data", conn, 1000);
        
        assertEquals(200, result.getRowCount());
        assertFalse(result.isTruncated());
        assertEquals("Item 1", result.getRows().get(0)[1]);
    }

    @Test
    public void testSelectHandlerTruncation() throws SQLException {
        SelectHandler handler = new SelectHandler();
        // Set maxRows to 50 (available is 200)
        QueryResult result = handler.handle("SELECT * FROM test_data", conn, 50);
        
        assertEquals(50, result.getRowCount());
        assertTrue(result.isTruncated());
        assertEquals("Item 50", result.getRows().get(49)[1]);
    }

    @Test
    public void testDeleteHandler() throws SQLException {
        DeleteHandler handler = new DeleteHandler();
        QueryResult result = handler.handle("UPDATE test_data SET name = 'Updated' WHERE id <= 10", conn, 1000);
        
        assertFalse(result.isSelectQuery());
        assertEquals(10, result.getAffectedRows());
    }

    @Test
    public void testGenericHandlerSelect() throws SQLException {
        GenericHandler handler = new GenericHandler();
        QueryResult result = handler.handle("SELECT name FROM test_data WHERE id = 1", conn, 1000);
        
        assertTrue(result.isSelectQuery());
        assertEquals(1, result.getRowCount());
        assertEquals("Item 1", result.getRows().get(0)[0]);
    }

    @Test
    public void testGenericHandlerOther() throws SQLException {
        GenericHandler handler = new GenericHandler();
        QueryResult result = handler.handle("CREATE TABLE temp_table (id INT)", conn, 1000);
        
        assertFalse(result.isSelectQuery());
        assertFalse(result.hasError());
    }
}
