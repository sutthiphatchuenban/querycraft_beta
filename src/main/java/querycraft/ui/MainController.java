package querycraft.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCombination;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.QueryResult;
import querycraft.service.DatabaseConnectionService;
import querycraft.service.QueryExecutorService;
import querycraft.ui.component.*;

/**
 * Main controller for the QueryCraft application, refactored for scalability and OOP patterns.
 */
public class MainController extends BorderPane implements querycraft.service.ConnectionObserver {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private final DatabaseConnectionService connectionService;
    private final QueryExecutorService queryExecutor;
    private final querycraft.service.PreparedStatementService preparedStatementService;
    private final querycraft.service.StreamingQueryService streamingQueryService;

    // UI Components (Refactored into sections)
    private final SidebarSection sidebarSection;
    private final QueryEditorSection querySection;
    private final ResultTableSection resultSection;
    private long currentQuerySession = 0;
    
    // Top & Bottom bars
    private Label statusLabel;
    private Label dbInfoLabel;
    private Button connectButton;
    private Button disconnectButton;

    public MainController() {
        this.connectionService = DatabaseConnectionService.getInstance();
        this.queryExecutor = new QueryExecutorService();
        this.preparedStatementService = new querycraft.service.PreparedStatementService();
        this.streamingQueryService = new querycraft.service.StreamingQueryService();
        
        // Load saved settings
        loadSettings();
        
        // Register as observer
        this.connectionService.addObserver(this);

        // Initialize components
        this.sidebarSection = new SidebarSection();
        this.querySection = new QueryEditorSection();
        this.resultSection = new ResultTableSection();

        initializeUI();
        setupListeners();
        setupShortcuts();
        updateConnectionStatus();
    }

    private void loadSettings() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(SettingsDialog.class);
        queryExecutor.setQueryTimeout(prefs.getInt("queryTimeout", 30));
        queryExecutor.setMaxRows(prefs.getInt("maxRows", 10000));
    }

    @Override
    public void onConnected(ConnectionInfo info) {
        Platform.runLater(() -> {
            updateConnectionStatus();
            if (info instanceof CsvConnectionInfo) {
                fetchTablesForCsv();
            } else {
                fetchTables();
            }
            setStatus("Connected to " + info.getDatabase());
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            updateConnectionStatus();
            resultSection.displayResult(null);
            setStatus("Disconnected");
        });
    }

    @Override
    public void onConnectionFailed(Exception e) {
        Platform.runLater(() -> {
            // Extract the most meaningful message (often buried in the cause for Hikari/JDBC)
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            
            showError("Connection Failed", message);
            setStatus("Connection Error: " + message);
            
            // Re-enable connect button if it was disabled during attempt
            updateConnectionStatus();
        });
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
                // For CSV, don't add LIMIT
                ConnectionInfo info = connectionService.getCurrentConnectionInfo();
                if (info instanceof CsvConnectionInfo) {
                    querySection.setSqlText("SELECT * FROM \"" + tableName + "\"");
                } else {
                    querySection.setSqlText("SELECT * FROM " + tableName + " LIMIT 100");
                }
                executeQuery(false);
            }

            @Override
            public void onHistoryItemDoubleClicked(String query) {
                querySection.setSqlText(query);
            }

            @Override
            public void onDescribeTableRequested(String tableName) {
                describeTable(tableName);
            }

            @Override
            public void onRefreshTablesRequested() {
                fetchTables();
            }
        });

        // Query Section Listeners
        querySection.setListener(new QueryEditorSection.QueryActionListener() {
            @Override
            public void onExecuteRequested(boolean isDelete) {
                executeQuery(isDelete);
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
        Label appLabel = new Label("QueryCraft v1.0.0");
        appLabel.getStyleClass().add("status-label");
        bar.getChildren().addAll(statusLabel, spacer, appLabel);
        return bar;
    }

    private void setupShortcuts() {
        final KeyCombination executeCombo = KeyCombination.valueOf("Shortcut+Enter");
        final KeyCombination formatCombo = KeyCombination.valueOf("Shortcut+F");

        this.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (executeCombo.match(e)) {
                executeQuery(false);
                e.consume();
            } else if (formatCombo.match(e)) {
                formatSql();
                e.consume();
            } else if (e.isControlDown()) {
                // Specialized fallback for ENTER
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    executeQuery(false);
                    e.consume();
                }
            }
        });
    }

    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog();
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait().ifPresent(this::connect);
    }

    private void showHelpDialog() {
        HelpDialog dialog = new HelpDialog();
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait();
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(queryExecutor);
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait().ifPresent(saved -> {
            if (saved) {
                dialog.saveSettings();
                setStatus("Settings saved (Timeout: " + dialog.getQueryTimeout() + "s)");
            }
        });
    }

    private void connect(ConnectionInfo info) {
        try {
            setStatus("Connecting...");
            if (info instanceof CsvConnectionInfo) {
                setStatus("Loading CSV file...");
            }
            connectionService.connect(info);
            // UI updates now handled by onConnected observer method
        } catch (Exception e) {
            // We catch general Exception here to prevent crashes from Hikari RuntimeExceptions.
            // Note: DatabaseConnectionService.connect now notifies observers on failure, 
            // so onConnectionFailed(e) will be called automatically and show the alert.
            logger.error("Connection attempt failed unexpectedly", e);
            setStatus("Connection failed");
        }
    }

    private void disconnect() {
        connectionService.disconnect();
        // UI updates now handled by onDisconnected observer method
    }

    private void fetchTablesForCsv() {
        if (!connectionService.isConnected()) return;
        
        ConnectionInfo info = connectionService.getCurrentConnectionInfo();
        if (info instanceof CsvConnectionInfo) {
            CsvConnectionInfo csvInfo = (CsvConnectionInfo) info;
            Platform.runLater(() -> {
                ObservableList<querycraft.model.DbTable> tables = FXCollections.observableArrayList();
                java.util.List<String> rawNames = new java.util.ArrayList<>();
                
                // For CSV folder, add all CSV files as tables
                for (CsvConnectionInfo.CsvFileInfo csvFile : csvInfo.getCsvFiles()) {
                    tables.add(new querycraft.model.DbTable(csvFile.getTableName(), "CSV"));
                    rawNames.add(csvFile.getTableName());
                }
                
                sidebarSection.setTables(tables);
                querySection.getEditor().setTableNames(rawNames);
                setStatus("CSV folder loaded: " + csvInfo.getCsvFileCount() + " file(s)");
            });
        }
    }

    private void updateConnectionStatus() {
        boolean connected = connectionService.isConnected();
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        querySection.setButtonsEnabled(connected);

        if (connected) {
            ConnectionInfo info = connectionService.getCurrentConnectionInfo();
            dbInfoLabel.setText(info.getDatabaseType().getDisplayName() + " | " + info.getHost());
        } else {
            dbInfoLabel.setText("Not connected");
        }
    }

    private void executeQuery(boolean isDelete) {
        String sql = querySection.getSqlText();
        if (sql == null || sql.trim().isEmpty()) return;

        if (resultSection.isStreamingModeEnabled() && !isDelete && queryExecutor.isSelectQuery(sql)) {
            executeStreamingQuery(sql);
            return;
        }

        // Check for named parameters (:paramName)
        if (sql.contains(":") && !isDelete) {
            ParameterDialog paramDialog = new ParameterDialog(sql);
            if (paramDialog.hasParameters()) {
                paramDialog.initOwner(getScene().getWindow());
                java.util.Map<String, Object> params = paramDialog.showAndWait().orElse(null);
                if (params == null) return; // User cancelled
                
                // Execute with parameters using PreparedStatementService
                executeWithParameters(sql, params);
                return;
            }
        }

        sidebarSection.addToHistory(sql);
        
        // Safety check for query type vs action
        boolean queryIsDelete = queryExecutor.isDeleteQuery(sql);
        if (!isDelete && queryIsDelete) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "This query appears to be a DELETE statement. Are you sure you want to execute it as a SELECT?",
                ButtonType.YES, ButtonType.NO);
            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        if (isDelete && !queryIsDelete) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "The query doesn't appear to be a DELETE statement. Execute anyway?",
                ButtonType.YES, ButtonType.NO);
            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        // Warning for all DELETE operations
        if (isDelete || queryIsDelete) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Warning: This operation will modify data in the database. Proceed?",
                ButtonType.YES, ButtonType.NO);
            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        setStatus("Executing...");
        resultSection.setLoading(true);
        final long sessionId = ++currentQuerySession;
        queryExecutor.executeQueryAsync(sql, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    if (sessionId == currentQuerySession) {
                        resultSection.displayResult(result);
                        setStatus("Done in " + result.getExecutionTimeMs() + "ms");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    if (sessionId == currentQuerySession) {
                        String errorTitle = "Query Error";
                        if (e instanceof querycraft.exception.QueryCraftException) {
                            querycraft.exception.QueryCraftException qce = (querycraft.exception.QueryCraftException) e;
                            switch (qce.getErrorCode()) {
                                case QUERY_TIMEOUT -> errorTitle = "Query Timeout";
                                case QUERY_VALIDATION_FAILED -> errorTitle = "Invalid Query";
                                case CONNECTION_FAILED -> errorTitle = "Connection Error";
                                default -> errorTitle = "Query Error";
                            }
                        }
                        showError(errorTitle, e.getMessage());
                        setStatus("Error: " + errorTitle);
                        resultSection.setLoading(false);
                    }
                });
            }
        });
    }

    private void executeWithParameters(String sql, java.util.Map<String, Object> params) {
        sidebarSection.addToHistory(sql);
        setStatus("Executing with parameters...");
        resultSection.setLoading(true);
        
        final long sessionId = ++currentQuerySession;
        new Thread(() -> {
            try {
                QueryResult result = preparedStatementService.executeQueryWithNamedParams(sql, params);
                javafx.application.Platform.runLater(() -> {
                    if (sessionId == currentQuerySession) {
                        resultSection.displayResult(result);
                        setStatus("Done (Prepared Statement)");
                    }
                });
            } catch (querycraft.exception.QueryCraftException e) {
                javafx.application.Platform.runLater(() -> {
                    if (sessionId == currentQuerySession) {
                        showError("Query Error", e.getMessage());
                        setStatus("Error: " + e.getErrorCode());
                        resultSection.setLoading(false);
                    }
                });
            }
        }).start();
    }

    private void executeStreamingQuery(String sql) {
        sidebarSection.addToHistory(sql);
        setStatus("Executing in streaming mode...");
        
        final long sessionId = ++currentQuerySession;
        resultSection.setLoading(true);

        java.util.List<Object[]> batch = new java.util.ArrayList<>();
        final int BATCH_SIZE = 500;

        streamingQueryService.streamQuery(sql, 
            cols -> {
                javafx.application.Platform.runLater(() -> {
                    if (sessionId == currentQuerySession) {
                        resultSection.initializeStreaming(cols);
                    }
                });
            },
            row -> {
                synchronized (batch) {
                    batch.add(row);
                    if (batch.size() >= BATCH_SIZE) {
                        java.util.List<Object[]> batchToProcess = new java.util.ArrayList<>(batch);
                        batch.clear();
                        javafx.application.Platform.runLater(() -> {
                            if (sessionId == currentQuerySession) {
                                resultSection.addStreamingBatch(batchToProcess);
                            }
                        });
                    }
                }
            }, 
            new querycraft.service.StreamingQueryService.StreamCallback() {
                @Override
                public void onComplete(long totalRows, long durationMs) {
                    javafx.application.Platform.runLater(() -> {
                        if (sessionId == currentQuerySession) {
                            synchronized (batch) {
                                if (!batch.isEmpty()) {
                                    resultSection.addStreamingBatch(new java.util.ArrayList<>(batch));
                                    batch.clear();
                                }
                            }
                            resultSection.finishStreaming(durationMs, totalRows);
                            setStatus("Streaming done: " + totalRows + " rows in " + durationMs + "ms");
                        }
                    });
                }

                @Override
                public void onError(querycraft.exception.QueryCraftException e) {
                    javafx.application.Platform.runLater(() -> {
                        if (sessionId == currentQuerySession) {
                            showError("Streaming Query Error", e.getMessage());
                            setStatus("Streaming error");
                            resultSection.failStreaming(e.getMessage());
                        }
                    });
                }
            }
        );
    }

    private void fetchTables() {
        if (!connectionService.isConnected()) return;
        String query = connectionService.getCurrentConnectionInfo().getDatabaseType().getShowTablesQuery();
        
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    ObservableList<querycraft.model.DbTable> tables = FXCollections.observableArrayList();
                    java.util.List<String> rawNames = new java.util.ArrayList<>();
                    
                    for (Object[] row : result.getRows()) {
                        if (row.length > 0) {
                            String name = row[0].toString();
                            String type = row.length > 1 ? row[1].toString() : "TABLE";
                            tables.add(new querycraft.model.DbTable(name, type));
                            rawNames.add(name);
                        }
                    }
                    sidebarSection.setTables(tables);
                    querySection.getEditor().setTableNames(rawNames);
                });
            }
            @Override public void onError(Exception e) { setStatus("Fetch tables failed"); }
        });
    }

    private void describeTable(String tableName) {
        String query = connectionService.getCurrentConnectionInfo().getDatabaseType().getDescribeTableQuery(tableName);
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> resultSection.displayResult(result));
            }
            @Override public void onError(Exception e) { showError("Describe Failed", e.getMessage()); }
        });
    }

    private void formatSql() {
        String text = querySection.getSqlText();
        if (text == null || text.trim().isEmpty()) return;
        
        // Remove existing weird whitespace and comment normalization
        String normalized = text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)\\s*,\\s*", ", ");

        // Basic keywords to put on new lines
        String[] keywords = {"SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", 
                            "LEFT JOIN", "INNER JOIN", "RIGHT JOIN", "JOIN", "UNION", 
                            "INSERT INTO", "VALUES", "SET", "UPDATE", "DELETE FROM"};
        
        String formatted = normalized;
        for (String kw : keywords) {
            formatted = formatted.replaceAll("(?i)\\b" + kw + "\\b", "\n" + kw);
        }

        // Sub-indentation for AND/OR
        formatted = formatted.replaceAll("(?i)\\b(AND|OR)\\b", "\n  $1");

        // Clean up leading/trailing whitespace
        formatted = formatted.trim()
                    .replaceAll("\\n+", "\n");
        
        querySection.setSqlText(formatted);
    }

    private void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
