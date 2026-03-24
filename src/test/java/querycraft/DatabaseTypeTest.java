package querycraft;

import static org.junit.Assert.*;
import org.junit.Test;
import querycraft.model.DatabaseType;

public class DatabaseTypeTest {

    @Test
    public void testEscaping() {
        // MySQL
        assertEquals("`users`", DatabaseType.MYSQL.escapeIdentifier("users"));
        assertEquals("`user``name`", DatabaseType.MYSQL.escapeIdentifier("user`name"));
        
        // PostgreSQL
        assertEquals("\"users\"", DatabaseType.POSTGRESQL.escapeIdentifier("users"));
        assertEquals("\"user\"\"name\"", DatabaseType.POSTGRESQL.escapeIdentifier("user\"name"));
        
        // SQL Server
        assertEquals("[users]", DatabaseType.MSSQL.escapeIdentifier("users"));
        assertEquals("[user]]name]", DatabaseType.MSSQL.escapeIdentifier("user]name"));
    }

    @Test
    public void testUrlGeneration() {
        // MySQL
        String mysqlUrl = DatabaseType.MYSQL.buildUrl("localhost", 3306, "testdb", false);
        assertTrue(mysqlUrl.contains("jdbc:mysql://localhost:3306/testdb"));
        
        // PostgreSQL
        String pgUrl = DatabaseType.POSTGRESQL.buildUrl("remote-db", 5432, "prod", true);
        assertTrue(pgUrl.contains("jdbc:postgresql://remote-db:5432/prod"));
        
        // SQL Server
        String sqlServerUrl = DatabaseType.MSSQL.buildUrl("sqlserver", 1433, "master", false);
        assertTrue(sqlServerUrl.contains("jdbc:sqlserver://sqlserver:1433;databaseName=master"));
        
        // CSV
        String csvUrl = DatabaseType.CSV.buildUrl("C:/data/folder", 0, "", false);
        assertTrue(csvUrl.contains("jdbc:h2:mem:csvdb"));
    }

    @Test
    public void testFindByName() {
        assertEquals(DatabaseType.MYSQL, DatabaseType.valueOf("MYSQL"));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.valueOf("POSTGRESQL"));
        assertEquals(DatabaseType.CSV, DatabaseType.valueOf("CSV"));
    }
}
