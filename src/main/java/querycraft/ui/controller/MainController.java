package querycraft.ui.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import querycraft.export.CompositeStreamingExporter;
import querycraft.export.CsvStreamingExporter;
import querycraft.export.SqlStreamingExporter;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.DatabaseType;
import querycraft.model.QueryResult;
import querycraft.connection.DatabaseConnectionService;
import querycraft.query.PreparedStatementService;
import querycraft.query.QueryExecutorService;
import querycraft.query.StreamingQueryService;
import querycraft.ui.dialog.ExportConfig;
import querycraft.ui.dialog.SettingsDialog;
import querycraft.ui.component.QueryEditorSection;
import querycraft.ui.component.ResultTableSection;
import querycraft.ui.component.SidebarSection;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

/**
 * Main controller for the QueryCraft application, refactored for scalability and OOP patterns.
 */
public class MainController extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private final DatabaseConnectionService connectionService;
    private final QueryExecutorService queryExecutor;
    private final PreparedStatementService preparedStatementService;
    private final StreamingQueryService streamingQueryService;
    private final QueryExecutionController queryExecutionController;
    private final ConnectionStateController connectionStateController;
    private final DialogManager dialogManager;

    // UI Components (Refactored into sections)
    private final SidebarSection sidebarSection;
    private final QueryEditorSection querySection;
    private final ResultTableSection resultSection;
    private final AtomicLong currentQuerySession = new AtomicLong();
    
    // Top & Bottom bars
    private Label statusLabel;
    private Label dbInfoLabel;
    private Button connectButton;
    private Button disconnectButton;

    public MainController() {
        this.connectionService = DatabaseConnectionService.getInstance();
        this.queryExecutor = new QueryExecutorService();
        this.preparedStatementService = new PreparedStatementService();
        this.streamingQueryService = new StreamingQueryService();
        this.dialogManager = new DialogManager(null);
        
        // Load saved settings
        loadSettings();
        
        // Initialize components
        this.sidebarSection = new SidebarSection();
        this.querySection = new QueryEditorSection();
        this.resultSection = new ResultTableSection();
        this.queryExecutionController = new QueryExecutionController(
                queryExecutor,
                preparedStatementService,
                streamingQueryService,
                sidebarSection,
                querySection,
                resultSection,
                dialogManager,
                currentQuerySession,
                this::setStatus
        );

        initializeUI();
        this.connectionStateController = new ConnectionStateController(
                connectionService,
                queryExecutor,
                sidebarSection,
                querySection,
                resultSection,
                dialogManager,
                dbInfoLabel,
                connectButton,
                disconnectButton,
                this::setStatus
        );
        this.connectionService.addObserver(connectionStateController);
        setupListeners();
        setupShortcuts();
        connectionStateController.updateConnectionStatus();
    }

    private DialogManager getDialogManager() {
        return dialogManager;
    }

    private void loadSettings() {
        Preferences prefs = Preferences.userNodeForPackage(SettingsDialog.class);
        queryExecutor.setQueryTimeout(prefs.getInt("queryTimeout", 30));
        queryExecutor.setMaxRows(prefs.getInt("maxRows", 10000));
    }

    private void initializeUI() {
        setTop(createTopBar());
        setBottom(createStatusBar());

        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.getItems().add(sidebarSection);

        SplitPane centerPane = new SplitPane();
        centerPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        centerPane.getItems().addAll(querySection, resultSection);
        centerPane.setDividerPositions(0.3);

        mainSplitPane.getItems().add(centerPane);
        mainSplitPane.setDividerPositions(0.2);

        setCenter(mainSplitPane);
        setPadding(new Insets(5));
    }

    private void setupListeners() {
        // Sidebar Listeners
        sidebarSection.setListener(new SidebarSection.SidebarListener() {
            @Override
            public void onTableDoubleClicked(String tableName) {
                String sql = connectionService.getCurrentConnectionInfo().getDatabaseType().getSelectAllWithLimitQuery(tableName, 100);
                querySection.setSqlText(sql);
                queryExecutionController.executeQuery();
            }

            @Override
            public void onHistoryItemDoubleClicked(String query) {
                querySection.setSqlText(query);
            }

            @Override
            public void onDescribeTableRequested(String tableName) {
                connectionStateController.describeTable(tableName);
            }

            @Override
            public void onRefreshTablesRequested() {
                connectionStateController.fetchTables();
            }
        });

        // Query Section Listeners
        querySection.setListener(new QueryEditorSection.QueryActionListener() {
            @Override
            public void onExecuteUnifiedRequested(String sql) {
                queryExecutionController.executeUnified(sql);
            }

            @Override
            public void onBatchProcessRequested(String selectSql, String deleteSql) {
                executeBatchProcess(selectSql, deleteSql);
            }

            @Override
            public void onFormatRequested() {
                formatSql();
            }
        });
    }

    private HBox createTopBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("top-bar");

        connectButton = new Button("Connect...");
        connectButton.getStyleClass().add("button-primary");
        connectButton.setOnAction(e -> showConnectionDialog());

        disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().add("button-danger");
        disconnectButton.setOnAction(e -> disconnect());

        Button helpButton = new Button("Help");
        helpButton.getStyleClass().add("button-neutral");
        helpButton.setOnAction(e -> showHelpDialog());

        Button settingsButton = new Button("Settings");
        settingsButton.getStyleClass().add("button-neutral");
        settingsButton.setOnAction(e -> showSettingsDialog());

        dbInfoLabel = new Label("Not connected");
        dbInfoLabel.getStyleClass().add("db-info-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(connectButton, disconnectButton, helpButton, settingsButton, spacer, dbInfoLabel);
        return bar;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("status-bar");
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label appLabel = new Label("QueryCraft v1.1.0-SNAPSHOT");
        appLabel.getStyleClass().add("status-label");
        bar.getChildren().addAll(statusLabel, spacer, appLabel);
        return bar;
    }

    private void setupShortcuts() {
        final KeyCombination executeCombo = KeyCombination.valueOf("Shortcut+Enter");
        final KeyCombination formatCombo = KeyCombination.valueOf("Shortcut+F");

        this.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (executeCombo.match(e)) {
                if (querySection.isBatchMode()) {
                    executeBatchProcess(querySection.getSqlText(), querySection.getDeleteSqlText());
                } else {
                    queryExecutionController.executeUnified(querySection.getSqlText());
                }
                e.consume();
            } else if (formatCombo.match(e)) {
                formatSql();
                e.consume();
            } else if (e.isControlDown()) {
                // Specialized fallback for ENTER
                if (e.getCode() == KeyCode.ENTER) {
                    if (querySection.isBatchMode()) {
                        executeBatchProcess(querySection.getSqlText(), querySection.getDeleteSqlText());
                    } else {
                        queryExecutionController.executeUnified(querySection.getSqlText());
                    }
                    e.consume();
                }
            }
        });
    }

    private void showConnectionDialog() {
        getDialogManager().showConnectionDialog().ifPresent(this::connect);
    }

    private void connect(ConnectionInfo info) {
        setStatus("Connecting...");
        if (info instanceof CsvConnectionInfo) {
            setStatus("Loading CSV file...");
        }

        new Thread(() -> {
            try {
                connectionService.connect(info);
            } catch (Exception e) {
                logger.error("Connection attempt failed unexpectedly", e);
                setStatus("Connection failed");
            }
        }, "Database-Connect").start();
    }

    private void showHelpDialog() {
        getDialogManager().showHelpDialog();
    }

    private void showSettingsDialog() {
        getDialogManager().showSettingsDialog(queryExecutor).ifPresent(saved -> {
            if (saved) {
                setStatus("Settings saved (Timeout: " + queryExecutor.getQueryTimeout() + "s)");
            }
        });
    }

    private void disconnect() {
        connectionService.disconnect();
        // UI updates now handled by onDisconnected observer method
    }

    private void executeBatchProcess(String selectSql, String deleteSql) {
        if (selectSql.isEmpty() || deleteSql.isEmpty()) {
            getDialogManager().showError("Input Required", "Both SELECT and DELETE queries are required for batch processing.");
            return;
        }

        if (!getDialogManager().confirmBatchProcess()) return;

        setStatus("Phase 1: Estimating row count...");
        resultSection.setLoading(true);

        new Thread(() -> {
            try {
                long estimatedRows = streamingQueryService.estimateRowCount(selectSql);
                if (estimatedRows == 0) {
                    Platform.runLater(() -> {
                        getDialogManager().showError("Batch Aborted", "No records found to archive.");
                        resultSection.setLoading(false);
                        setStatus("Batch aborted: No data found");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    setStatus("Phase 2: Awaiting Export Configuration (" + estimatedRows + " rows estimated)...");
                    
                    getDialogManager().promptExportConfig().ifPresentOrElse(config -> {
                        DatabaseType currentDbType = connectionService.getCurrentConnectionInfo().getDatabaseType();
                        
                        // If CSV database, skip SQL generation (quirk fix)
                        if (currentDbType == DatabaseType.CSV) {
                            proceedWithBatchExportAndCleanup(config, null, null, currentDbType, selectSql, deleteSql, estimatedRows);
                            return;
                        }

                        getDialogManager().promptTargetTableName("archived_records").ifPresentOrElse(tableName -> {
                            if (tableName.trim().isEmpty()) {
                                resultSection.setLoading(false);
                                setStatus("Batch aborted logic: Empty table name.");
                                return;
                            }
                            
                            File initialDir = config.getFile().getParentFile();
                            getDialogManager().promptSqlFileSave(tableName.trim() + ".sql", initialDir).ifPresentOrElse(sqlFile -> {
                                proceedWithBatchExportAndCleanup(config, sqlFile, tableName.trim(), currentDbType, selectSql, deleteSql, estimatedRows);
                            }, () -> {
                                resultSection.setLoading(false);
                                setStatus("Batch aborted by user at SQL file selection.");
                            });
                        }, () -> {
                            resultSection.setLoading(false);
                            setStatus("Batch aborted by user at SQL table name input.");
                        });
                    }, () -> {
                        resultSection.setLoading(false);
                        setStatus("Batch aborted by user at export phase.");
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    resultSection.setLoading(false);
                    getDialogManager().showError("Batch Estimation Error", "Failed to estimate rows: " + e.getMessage());
                    setStatus("Batch failed at pre-flight check.");
                });
            }
        }).start();
    }

    private void proceedWithBatchExportAndCleanup(
            ExportConfig config,
            File sqlFile,
            String tableName, 
            DatabaseType currentDbType,
            String selectSql, 
            String deleteSql,
            long estimatedRows) {

        setStatus("Phase 2: Exporting streaming records...");
        resultSection.setLoading(true);

        CompositeStreamingExporter composite = new CompositeStreamingExporter();
        composite.addExporter(new CsvStreamingExporter(config.getFile(), config.getOptions()));
        
        if (sqlFile != null && tableName != null) {
            composite.addExporter(new SqlStreamingExporter(sqlFile, tableName, currentDbType));
        }

        streamingQueryService.streamExportToFile(selectSql, composite, new StreamingQueryService.StreamCallback() {
            @Override
            public void onComplete(long totalRows, long durationMs) {
                Platform.runLater(() -> setStatus("Phase 3: Cleanup (Executing DELETE)..."));
                
                // Phase 3: DELETE
                queryExecutor.executeQueryAsync(deleteSql, new QueryExecutorService.QueryCallback() {
                    @Override
                    public void onSuccess(QueryResult deleteResult) {
                        Platform.runLater(() -> {
                            resultSection.setLoading(false);
                            setStatus("Batch Success: " + totalRows + " archived, " + deleteResult.getAffectedRows() + " deleted.");
                            
                            String fileInfo = "- " + config.getFile().getAbsolutePath();
                            if (sqlFile != null) {
                                fileInfo += "\n- " + sqlFile.getAbsolutePath();
                            }
                            
                            String content = "Data archived to:\n" + fileInfo + 
                                             "\n\nRecords deleted: " + deleteResult.getAffectedRows() + "\nExport time: " + durationMs + "ms";
                            getDialogManager().showInfo("Batch Process Complete", "Success!", content);
                            
                            // Refresh tables/sidebar if needed
                            connectionStateController.fetchTables();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Platform.runLater(() -> {
                            resultSection.setLoading(false);
                            getDialogManager().showError("Batch Cleanup Error", "Data was exported, but DELETE failed: " + e.getMessage());
                            setStatus("Batch partial success: Exported but not deleted.");
                        });
                    }
                });
            }

            @Override
            public void onError(querycraft.exception.QueryCraftException e) {
                Platform.runLater(() -> {
                    resultSection.setLoading(false);
                    getDialogManager().showError("Stream Export Error", "Failed to export data: " + e.getMessage());
                    setStatus("Batch failed at export phase.");
                });
            }
        });
    }

    private void formatSql() {
        String text = querySection.getSqlText();
        if (text == null || text.trim().isEmpty()) return;
        
        // Only normalize whitespace and commas outside of string literals
        // Regex lookahead ensures we only match whitespace if followed by an even number of single quotes
        String normalized = text.trim()
                .replaceAll("\\s+(?=(?:[^']*'[^']*')*[^']*$)", " ")
                .replaceAll("(?i)\\s*,\\s*(?=(?:[^']*'[^']*')*[^']*$)", ", ");

        // Basic keywords to put on new lines
        String[] keywords = {"SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", 
                            "LEFT JOIN", "INNER JOIN", "RIGHT JOIN", "JOIN", "UNION", 
                            "INSERT INTO", "VALUES", "SET", "UPDATE", "DELETE FROM"};
        
        String formatted = normalized;
        for (String kw : keywords) {
            String regex = "(?i)\\b" + kw + "\\b(?=(?:[^']*'[^']*')*[^']*$)";
            formatted = formatted.replaceAll(regex, "\n" + kw);
        }

        // Sub-indentation for AND/OR (lookahead for strings)
        formatted = formatted.replaceAll("(?i)\\b(AND|OR)\\b(?=(?:[^']*'[^']*')*[^']*$)", "\n  $1");

        // Clean up leading/trailing whitespace
        formatted = formatted.trim()
                    .replaceAll("\\n+", "\n");
        
        querySection.setSqlText(formatted);
    }

    private void setStatus(String message) {
        javafx.application.Platform.runLater(() -> statusLabel.setText(message));
    }
}
