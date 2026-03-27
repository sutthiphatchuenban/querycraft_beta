package querycraft;

import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;
import querycraft.model.ColumnInfo;
import querycraft.model.QueryResult;
import querycraft.query.PreparedStatementService;
import querycraft.query.QueryExecutorService;
import querycraft.query.StreamingQueryService;
import querycraft.ui.controller.DialogManager;
import querycraft.ui.controller.QueryExecutionController;
import querycraft.ui.component.QueryEditorSection;
import querycraft.ui.component.ResultTableSection;
import querycraft.ui.component.SidebarSection;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryExecutionControllerTest {

    @BeforeClass
    public static void setupToolkit() {
        JavaFxTestBootstrap.initToolkit();
    }

    @Test
    public void testExecuteUnifiedWithBlankSqlDoesNothing() {
        AtomicReference<String> status = new AtomicReference<>();

        JavaFxTestBootstrap.runOnFxThreadAndWait(() -> {
            TestQueryEditorSection querySection = new TestQueryEditorSection("   ");
            QueryExecutionController controller = new QueryExecutionController(
                    new StubQueryExecutorService(),
                    new StubPreparedStatementService(),
                    new StubStreamingQueryService(),
                    new SidebarSection(),
                    querySection,
                    new ResultTableSection(),
                    new StubDialogManager(),
                    new AtomicLong(),
                    status::set
            );

            controller.executeUnified("   ");
        });

        assertEquals(null, status.get());
    }

    @Test
    public void testExecuteQueryAddsHistoryAndDisplaysResult() throws Exception {
        AtomicReference<String> status = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        StubSidebarSection sidebar = new StubSidebarSection();
        StubResultTableSection resultSection = new StubResultTableSection(latch);

        JavaFxTestBootstrap.runOnFxThreadAndWait(() -> {
            QueryExecutionController controller = new QueryExecutionController(
                    new StubQueryExecutorService(),
                    new StubPreparedStatementService(),
                    new StubStreamingQueryService(),
                    sidebar,
                    new TestQueryEditorSection("SELECT 1"),
                    resultSection,
                    new StubDialogManager(),
                    new AtomicLong(),
                    status::set
            );
            controller.executeQuery();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("SELECT 1", sidebar.lastHistory);
        assertEquals(1, resultSection.lastResult.getRows().size());
        assertTrue(status.get().startsWith("Done in "));
    }

    @Test
    public void testExecuteStreamingQueryFinishesStreaming() throws Exception {
        AtomicReference<String> status = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        StubResultTableSection resultSection = new StubResultTableSection(latch);

        JavaFxTestBootstrap.runOnFxThreadAndWait(() -> {
            QueryExecutionController controller = new QueryExecutionController(
                    new StubQueryExecutorService(),
                    new StubPreparedStatementService(),
                    new StubStreamingQueryService(),
                    new StubSidebarSection(),
                    new TestQueryEditorSection("SELECT * FROM users"),
                    resultSection,
                    new StubDialogManager(),
                    new AtomicLong(),
                    status::set
            );
            controller.executeStreamingQuery("SELECT * FROM users");
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, resultSection.streamingRows.size());
        assertEquals("Streaming done: 2 rows in 15ms", status.get());
    }

    private static class StubDialogManager extends DialogManager {
        StubDialogManager() {
            super(null);
        }

        @Override
        public boolean confirmAction(String title, String content) {
            return true;
        }
    }

    private static class TestQueryEditorSection extends QueryEditorSection {
        private final String sql;

        TestQueryEditorSection(String sql) {
            this.sql = sql;
        }

        @Override
        public String getSqlText() {
            return sql;
        }
    }

    private static class StubSidebarSection extends SidebarSection {
        String lastHistory;

        @Override
        public void addToHistory(String query) {
            this.lastHistory = query;
        }
    }

    private static class StubResultTableSection extends ResultTableSection {
        private final CountDownLatch latch;
        QueryResult lastResult;
        final List<Object[]> streamingRows = new ArrayList<>();

        StubResultTableSection(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void displayResult(QueryResult result) {
            this.lastResult = result;
            latch.countDown();
        }

        @Override
        public void initializeStreaming(List<ColumnInfo> columns) {
        }

        @Override
        public void addStreamingBatch(List<Object[]> batch) {
            streamingRows.addAll(batch);
        }

        @Override
        public void finishStreaming(long durationMs, long totalRows) {
            latch.countDown();
        }
    }

    private static class StubQueryExecutorService extends QueryExecutorService {
        @Override
        public boolean isSelectQuery(String sql) {
            return sql != null && sql.trim().toUpperCase().startsWith("SELECT");
        }

        @Override
        public boolean isDeleteQuery(String sql) {
            return false;
        }

        @Override
        public void executeQueryAsync(String sql, QueryCallback callback) {
            QueryResult result = new QueryResult();
            result.setColumns(List.of(new ColumnInfo("value", "INTEGER", Types.INTEGER, 10, false)));
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{1});
            result.setRows(rows);
            result.setExecutionTimeMs(12);
            Platform.runLater(() -> callback.onSuccess(result));
        }
    }

    private static class StubPreparedStatementService extends PreparedStatementService {
        @Override
        public QueryResult executeQueryWithNamedParams(String sql, Map<String, Object> params) {
            QueryResult result = new QueryResult();
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{1});
            result.setRows(rows);
            result.setExecutionTimeMs(1);
            return result;
        }
    }

    private static class StubStreamingQueryService extends StreamingQueryService {
        @Override
        public void streamQuery(String sql,
                                java.util.function.Consumer<List<ColumnInfo>> columnConsumer,
                                java.util.function.Consumer<Object[]> rowConsumer,
                                StreamCallback callback) {
            Platform.runLater(() -> {
                columnConsumer.accept(List.of(new ColumnInfo("id", "INTEGER", Types.INTEGER, 10, false)));
                rowConsumer.accept(new Object[]{1});
                rowConsumer.accept(new Object[]{2});
                callback.onComplete(2, 15);
            });
        }
    }
}
