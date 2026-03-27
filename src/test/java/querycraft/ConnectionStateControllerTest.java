package querycraft;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.BeforeClass;
import org.junit.Test;
import querycraft.model.CsvConnectionInfo;
import querycraft.connection.DatabaseConnectionService;
import querycraft.query.QueryExecutorService;
import querycraft.ui.controller.ConnectionStateController;
import querycraft.ui.controller.DialogManager;
import querycraft.ui.component.QueryEditorSection;
import querycraft.ui.component.ResultTableSection;
import querycraft.ui.component.SidebarSection;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionStateControllerTest {

    @BeforeClass
    public static void setupToolkit() {
        JavaFxTestBootstrap.initToolkit();
    }

    @Test
    public void testUpdateConnectionStatusWhenDisconnected() {
        Label dbInfoLabel = new Label();
        Button connectButton = new Button();
        Button disconnectButton = new Button();
        AtomicReference<String> status = new AtomicReference<>();

        JavaFxTestBootstrap.runOnFxThreadAndWait(() -> {
            DatabaseConnectionService.getInstance().disconnect();
            ConnectionStateController controller = new ConnectionStateController(
                    DatabaseConnectionService.getInstance(),
                    new QueryExecutorService(),
                    new SidebarSection(),
                    new QueryEditorSection(),
                    new ResultTableSection(),
                    new DialogManager(null),
                    dbInfoLabel,
                    connectButton,
                    disconnectButton,
                    status::set
            );

            controller.updateConnectionStatus();
        });

        assertEquals("Not connected", dbInfoLabel.getText());
        assertFalse(connectButton.isDisabled());
        assertTrue(disconnectButton.isDisabled());
    }

    @Test
    public void testFetchTablesForCsvUpdatesEditorAndSidebar() throws Exception {
        File folder = createCsvFolder();
        CsvConnectionInfo info = new CsvConnectionInfo(folder.getAbsolutePath());
        DatabaseConnectionService.getInstance().connect(info);

        CountDownLatch latch = new CountDownLatch(1);
        StubSidebarSection sidebar = new StubSidebarSection(latch);
        StubQueryEditorSection editorSection = new StubQueryEditorSection();
        AtomicReference<String> status = new AtomicReference<>();

        JavaFxTestBootstrap.runOnFxThreadAndWait(() -> {
            ConnectionStateController controller = new ConnectionStateController(
                    DatabaseConnectionService.getInstance(),
                    new QueryExecutorService(),
                    sidebar,
                    editorSection,
                    new ResultTableSection(),
                    new DialogManager(null),
                    new Label(),
                    new Button(),
                    new Button(),
                    status::set
            );
            controller.fetchTablesForCsv();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, sidebar.tableCount);
        assertEquals(1, editorSection.tableNames.size());
        assertTrue(status.get().startsWith("CSV folder loaded:"));

        DatabaseConnectionService.getInstance().disconnect();
        cleanup(folder);
    }

    private File createCsvFolder() throws Exception {
        File folder = new File(System.getProperty("java.io.tmpdir"), "querycraft_fx_csv_" + System.nanoTime());
        assertTrue(folder.mkdirs());
        File csv = new File(folder, "users.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(csv))) {
            pw.println("id,name");
            pw.println("1,Alice");
        }
        return folder;
    }

    private void cleanup(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        folder.delete();
    }

    private static class StubSidebarSection extends SidebarSection {
        private final CountDownLatch latch;
        int tableCount;

        StubSidebarSection(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void setTables(javafx.collections.ObservableList<querycraft.model.DbTable> tables) {
            this.tableCount = tables.size();
            latch.countDown();
        }
    }

    private static class StubQueryEditorSection extends QueryEditorSection {
        List<String> tableNames;

        @Override
        public querycraft.ui.component.SqlEditor getEditor() {
            return new querycraft.ui.component.SqlEditor() {
                @Override
                public void setTableNames(List<String> tableNames) {
                    StubQueryEditorSection.this.tableNames = tableNames;
                }
            };
        }
    }
}
