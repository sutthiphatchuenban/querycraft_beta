package querycraft.ui.controller;

import javafx.application.Platform;
import querycraft.exception.QueryCraftException;
import querycraft.model.QueryResult;
import querycraft.query.PreparedStatementService;
import querycraft.query.QueryExecutorService;
import querycraft.query.StreamingQueryService;
import querycraft.ui.dialog.ParameterDialog;
import querycraft.ui.component.QueryEditorSection;
import querycraft.ui.component.ResultTableSection;
import querycraft.ui.component.SidebarSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles query execution workflows extracted from MainController.
 */
public class QueryExecutionController {

    private final QueryExecutorService queryExecutor;
    private final PreparedStatementService preparedStatementService;
    private final StreamingQueryService streamingQueryService;
    private final SidebarSection sidebarSection;
    private final QueryEditorSection querySection;
    private final ResultTableSection resultSection;
    private final DialogManager dialogManager;
    private final AtomicLong currentQuerySession;
    private final StatusReporter statusReporter;

    public QueryExecutionController(
            QueryExecutorService queryExecutor,
            PreparedStatementService preparedStatementService,
            StreamingQueryService streamingQueryService,
            SidebarSection sidebarSection,
            QueryEditorSection querySection,
            ResultTableSection resultSection,
            DialogManager dialogManager,
            AtomicLong currentQuerySession,
            StatusReporter statusReporter) {
        this.queryExecutor = queryExecutor;
        this.preparedStatementService = preparedStatementService;
        this.streamingQueryService = streamingQueryService;
        this.sidebarSection = sidebarSection;
        this.querySection = querySection;
        this.resultSection = resultSection;
        this.dialogManager = dialogManager;
        this.currentQuerySession = currentQuerySession;
        this.statusReporter = statusReporter;
    }

    public void executeUnified(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        executeQuery();
    }

    public void executeQuery() {
        String sql = querySection.getSqlText();
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }

        boolean isSelect = queryExecutor.isSelectQuery(sql);

        if (resultSection.isStreamingModeEnabled() && isSelect) {
            executeStreamingQuery(sql);
            return;
        }

        if (sql.length() < 10000 && sql.contains(":") && !queryExecutor.isDeleteQuery(sql)) {
            ParameterDialog paramDialog = new ParameterDialog(sql);
            if (paramDialog.hasParameters()) {
                Map<String, Object> params = paramDialog.showAndWait().orElse(null);
                if (params == null) {
                    return;
                }
                executeWithParameters(sql, params);
                return;
            }
        }

        sidebarSection.addToHistory(sql);

        boolean queryIsDelete = queryExecutor.isDeleteQuery(sql);
        if (queryIsDelete) {
            String msg = "Warning: This operation is a DELETE statement and will permanently modify data. Proceed?";
            if (!dialogManager.confirmAction("Confirm Deletion", msg)) {
                return;
            }
        } else if (!isSelect) {
            String msg = "Warning: This operation will modify data in the database. Proceed?";
            if (!dialogManager.confirmAction("Confirm Modification", msg)) {
                return;
            }
        }

        statusReporter.setStatus("Executing...");
        resultSection.setLoading(true);
        final long sessionId = currentQuerySession.incrementAndGet();

        queryExecutor.executeQueryAsync(sql, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    if (sessionId == currentQuerySession.get()) {
                        resultSection.displayResult(result);
                        statusReporter.setStatus("Done in " + result.getExecutionTimeMs() + "ms");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    if (sessionId == currentQuerySession.get()) {
                        String errorTitle = resolveErrorTitle(e);
                        dialogManager.showError(errorTitle, e.getMessage());
                        statusReporter.setStatus("Error: " + errorTitle);
                        resultSection.setLoading(false);
                    }
                });
            }
        });
    }

    public void executeWithParameters(String sql, Map<String, Object> params) {
        sidebarSection.addToHistory(sql);
        statusReporter.setStatus("Executing with parameters...");
        resultSection.setLoading(true);

        final long sessionId = currentQuerySession.incrementAndGet();
        new Thread(() -> {
            try {
                preparedStatementService.setMaxRows(queryExecutor.getMaxRows());
                QueryResult result = preparedStatementService.executeQueryWithNamedParams(sql, params);
                Platform.runLater(() -> {
                    if (sessionId == currentQuerySession.get()) {
                        resultSection.displayResult(result);
                        statusReporter.setStatus("Done (Prepared Statement)");
                    }
                });
            } catch (QueryCraftException e) {
                Platform.runLater(() -> {
                    if (sessionId == currentQuerySession.get()) {
                        dialogManager.showError("Query Error", e.getMessage());
                        statusReporter.setStatus("Error: " + e.getErrorCode());
                        resultSection.setLoading(false);
                    }
                });
            }
        }, "PreparedStatement-Execution").start();
    }

    public void executeStreamingQuery(String sql) {
        sidebarSection.addToHistory(sql);
        statusReporter.setStatus("Executing in streaming mode...");

        final long sessionId = currentQuerySession.incrementAndGet();
        resultSection.setLoading(true);

        List<Object[]> batch = new ArrayList<>();
        final int batchSize = 500;

        streamingQueryService.streamQuery(
                sql,
                cols -> Platform.runLater(() -> {
                    if (sessionId == currentQuerySession.get()) {
                        resultSection.initializeStreaming(cols);
                    }
                }),
                row -> {
                    synchronized (batch) {
                        batch.add(row);
                        if (batch.size() >= batchSize) {
                            List<Object[]> batchToProcess = new ArrayList<>(batch);
                            batch.clear();
                            Platform.runLater(() -> {
                                if (sessionId == currentQuerySession.get()) {
                                    resultSection.addStreamingBatch(batchToProcess);
                                }
                            });
                        }
                    }
                },
                new StreamingQueryService.StreamCallback() {
                    @Override
                    public void onComplete(long totalRows, long durationMs) {
                        Platform.runLater(() -> {
                            if (sessionId == currentQuerySession.get()) {
                                synchronized (batch) {
                                    if (!batch.isEmpty()) {
                                        resultSection.addStreamingBatch(new ArrayList<>(batch));
                                        batch.clear();
                                    }
                                }
                                resultSection.finishStreaming(durationMs, totalRows);
                                statusReporter.setStatus("Streaming done: " + totalRows + " rows in " + durationMs + "ms");
                            }
                        });
                    }

                    @Override
                    public void onError(QueryCraftException e) {
                        Platform.runLater(() -> {
                            if (sessionId == currentQuerySession.get()) {
                                dialogManager.showError("Streaming Query Error", e.getMessage());
                                statusReporter.setStatus("Streaming error");
                                resultSection.failStreaming(e.getMessage());
                            }
                        });
                    }
                }
        );
    }

    private String resolveErrorTitle(Exception e) {
        String errorTitle = "Query Error";
        if (e instanceof QueryCraftException qce) {
            switch (qce.getErrorCode()) {
                case QUERY_TIMEOUT -> errorTitle = "Query Timeout";
                case QUERY_VALIDATION_FAILED -> errorTitle = "Invalid Query";
                case CONNECTION_FAILED -> errorTitle = "Connection Error";
                default -> errorTitle = "Query Error";
            }
        }
        return errorTitle;
    }

    @FunctionalInterface
    public interface StatusReporter {
        void setStatus(String message);
    }
}
