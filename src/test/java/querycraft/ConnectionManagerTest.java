package querycraft;

import org.junit.After;
import org.junit.Test;
import querycraft.exception.QueryCraftException;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.DatabaseType;
import querycraft.connection.ConnectionManager;
import querycraft.connection.CsvConnectionManager;
import querycraft.connection.DatabaseConnectionService;
import querycraft.connection.PooledConnectionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;

import static org.junit.Assert.*;

public class ConnectionManagerTest {

    private File tempCsvFolder;

    @After
    public void tearDown() {
        DatabaseConnectionService.getInstance().disconnect();
        if (tempCsvFolder != null && tempCsvFolder.exists()) {
            File[] files = tempCsvFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempCsvFolder.delete();
        }
    }

    @Test
    public void testCsvConnectionManagerSupportsCsvInfo() {
        ConnectionManager manager = new CsvConnectionManager();
        assertTrue(manager.supports(new CsvConnectionInfo("test-folder")));
        assertFalse(manager.supports(new ConnectionInfo(DatabaseType.MYSQL, "localhost", 3306, "db", "u", "p")));
    }

    @Test
    public void testPooledConnectionManagerDoesNotSupportCsvInfo() {
        ConnectionManager manager = new PooledConnectionManager();
        assertFalse(manager.supports(new CsvConnectionInfo("test-folder")));
        assertTrue(manager.supports(new ConnectionInfo(DatabaseType.MYSQL, "localhost", 3306, "db", "u", "p")));
    }

    @Test
    public void testCsvConnectionManagerTestConnection() throws Exception {
        tempCsvFolder = createCsvFolder();
        CsvConnectionManager manager = new CsvConnectionManager();
        CsvConnectionInfo info = new CsvConnectionInfo(tempCsvFolder.getAbsolutePath());

        assertTrue(manager.testConnection(info));
    }

    @Test
    public void testCsvConnectionManagerConnectAndDisconnect() throws Exception {
        tempCsvFolder = createCsvFolder();
        CsvConnectionManager manager = new CsvConnectionManager();
        CsvConnectionInfo info = new CsvConnectionInfo(tempCsvFolder.getAbsolutePath());

        Connection connection = manager.connect(info);
        assertNotNull(connection);
        assertTrue(manager.isConnected());
        assertNotNull(manager.getCurrentCsvInfo());

        manager.disconnect();
        assertFalse(manager.isConnected());
        assertNull(manager.getCurrentCsvInfo());
    }

    @Test
    public void testDatabaseConnectionServiceCsvLifecycle() throws Exception {
        tempCsvFolder = createCsvFolder();
        DatabaseConnectionService service = DatabaseConnectionService.getInstance();
        CsvConnectionInfo info = new CsvConnectionInfo(tempCsvFolder.getAbsolutePath());

        Connection connection = service.connect(info);
        assertNotNull(connection);
        assertTrue(service.isConnected());
        assertEquals(info, service.getCurrentConnectionInfo());

        service.disconnect();
        assertFalse(service.isConnected());
        assertNull(service.getCurrentConnectionInfo());
    }

    @Test
    public void testDatabaseConnectionServiceValidateConnectionThrowsWhenDisconnected() {
        DatabaseConnectionService service = DatabaseConnectionService.getInstance();
        service.disconnect();

        try {
            service.validateConnection();
            fail("Expected QueryCraftException");
        } catch (QueryCraftException e) {
            assertEquals(QueryCraftException.ErrorCode.CONNECTION_FAILED, e.getErrorCode());
        }
    }

    private File createCsvFolder() throws Exception {
        File folder = new File(System.getProperty("java.io.tmpdir"), "querycraft_connection_manager_test_" + System.nanoTime());
        assertTrue(folder.mkdirs());

        File csv = new File(folder, "users.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(csv))) {
            pw.println("id,name");
            pw.println("1,Alice");
            pw.println("2,Bob");
        }
        return folder;
    }
}
