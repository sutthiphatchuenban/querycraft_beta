package querycraft;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import querycraft.model.QueryResult;
import querycraft.service.QueryExecutor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryExecutorTest {

    private Connection conn;
    private QueryExecutor executor;

    @Before
    public void setup() throws SQLException {
        // Use H2 in-memory for testing
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        executor = new QueryExecutor();

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
    public void testSelectQuery() throws SQLException {
        QueryResult result = executor.execute("SELECT * FROM test_data", conn, 1000);

        assertEquals(200, result.getRowCount());
        assertFalse(result.isTruncated());
        assertEquals("Item 1", result.getRows().get(0)[1]);
        assertTrue(result.isSelectQuery());
    }

    @Test
    public void testSelectQueryWithTruncation() throws SQLException {
        QueryResult result = executor.execute("SELECT * FROM test_data", conn, 50);

        assertEquals(50, result.getRowCount());
        assertTrue(result.isTruncated());
        assertEquals("Item 50", result.getRows().get(49)[1]);
    }

    @Test
    public void testInsertQuery() throws SQLException {
        QueryResult result = executor.execute("INSERT INTO test_data VALUES (999, 'New Item')", conn, 1000);

        assertFalse(result.isSelectQuery());
        assertEquals(1, result.getAffectedRows());
    }

    @Test
    public void testUpdateQuery() throws SQLException {
        QueryResult result = executor.execute("UPDATE test_data SET name = 'Updated' WHERE id <= 10", conn, 1000);

        assertFalse(result.isSelectQuery());
        assertEquals(10, result.getAffectedRows());
    }

    @Test
    public void testDeleteQuery() throws SQLException {
        QueryResult result = executor.execute("DELETE FROM test_data WHERE id <= 5", conn, 1000);

        assertFalse(result.isSelectQuery());
        assertEquals(5, result.getAffectedRows());
    }

    @Test
    public void testIsReadQuery() {
        assertTrue(executor.isReadQuery("SELECT * FROM test"));
        assertTrue(executor.isReadQuery("WITH cte AS (SELECT 1) SELECT * FROM cte"));
        assertTrue(executor.isReadQuery("SHOW TABLES"));
        assertTrue(executor.isReadQuery("DESCRIBE test_table"));
        assertTrue(executor.isReadQuery("EXPLAIN SELECT * FROM test"));

        assertFalse(executor.isReadQuery("INSERT INTO test VALUES (1)"));
        assertFalse(executor.isReadQuery("UPDATE test SET x = 1"));
        assertFalse(executor.isReadQuery("DELETE FROM test"));
    }

    @Test
    public void testIsWriteQuery() {
        assertTrue(executor.isWriteQuery("INSERT INTO test VALUES (1)"));
        assertTrue(executor.isWriteQuery("UPDATE test SET x = 1"));
        assertTrue(executor.isWriteQuery("DELETE FROM test"));

        assertFalse(executor.isWriteQuery("SELECT * FROM test"));
    }

    @Test
    public void testIsDeleteQuery() {
        assertTrue(executor.isDeleteQuery("DELETE FROM test WHERE id = 1"));
        assertFalse(executor.isDeleteQuery("SELECT * FROM test"));
        assertFalse(executor.isDeleteQuery("INSERT INTO test VALUES (1)"));
    }
}
