package querycraft.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import querycraft.model.ConnectionInfo;
import querycraft.model.QueryResult;
import querycraft.service.DatabaseConnectionService;
import querycraft.service.QueryExecutorService;
import querycraft.ui.component.*;

/**
 * Main controller for the QueryCraft application, refactored for scalability.
 */
public class MainController extends BorderPane {

    private final DatabaseConnectionService connectionService;
    private final QueryExecutorService queryExecutor;

    // UI Components (Refactored into sections)
    private final SidebarSection sidebarSection;
    private final QueryEditorSection querySection;
    private final ResultTableSection resultSection;
    
    // Top & Bottom bars
    private Label statusLabel;
    private Label dbInfoLabel;
    private Button connectButton;
    private Button disconnectButton;

    public MainController() {
        this.connectionService = DatabaseConnectionService.getInstance();
        this.queryExecutor = new QueryExecutorService();

        // Initialize components
        this.sidebarSection = new SidebarSection();
        this.querySection = new QueryEditorSection();
        this.resultSection = new ResultTableSection();

        initializeUI();
        setupListeners();
        updateConnectionStatus();
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
                querySection.setSqlText("SELECT * FROM " + tableName + " LIMIT 100");
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
        disconnectButton.getStyleClass().add("button-neutral");
        disconnectButton.setOnAction(e -> disconnect());

        dbInfoLabel = new Label("Not connected");
        dbInfoLabel.getStyleClass().add("db-info-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(connectButton, disconnectButton, spacer, dbInfoLabel);
        return bar;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("status-bar");
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label appLabel = new Label("QueryCraft v1.1 Refactored");
        appLabel.getStyleClass().add("status-label");
        bar.getChildren().addAll(statusLabel, spacer, appLabel);
        return bar;
    }

    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog();
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait().ifPresent(this::connect);
    }

    private void connect(ConnectionInfo info) {
        try {
            setStatus("Connecting...");
            connectionService.connect(info);
            updateConnectionStatus();
            fetchTables();
            setStatus("Connected to " + info.getDatabase());
        } catch (Exception e) {
            showError("Connection Failed", e.getMessage());
        }
    }

    private void disconnect() {
        connectionService.disconnect();
        updateConnectionStatus();
        resultSection.displayResult(null);
        setStatus("Disconnected");
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
        queryExecutor.executeQueryAsync(sql, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    resultSection.displayResult(result);
                    setStatus("Done in " + result.getExecutionTimeMs() + "ms");
                });
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    showError("Query Error", e.getMessage());
                    setStatus("Error occurred");
                    resultSection.setLoading(false);
                });
            }
        });
    }

    private void fetchTables() {
        if (!connectionService.isConnected()) return;
        String query = connectionService.getCurrentConnectionInfo().getDatabaseType().getShowTablesQuery();
        
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    ObservableList<String> tables = FXCollections.observableArrayList();
                    for (Object[] row : result.getRows()) {
                        if (row.length > 0) tables.add(row[0].toString());
                    }
                    sidebarSection.setTables(tables);
                    querySection.getEditor().setTableNames(new java.util.ArrayList<>(tables));
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
        
        // Simple but more efficient formatter using a cleaner regex approach
        // Note: For complex SQL, a real parser like JSqlParser would be better
        String formatted = text.trim()
                .replaceAll("(?i)\\s*\\b(SELECT|FROM|WHERE|GROUP BY|ORDER BY|HAVING|LIMIT|LEFT JOIN|INNER JOIN|RIGHT JOIN|JOIN|UNION|VALUES|SET|UPDATE|DELETE)\\b\\s*", "\n$1 ")
                .replaceAll("(?i)\\s*\\b(AND|OR)\\b\\s*", "\n  $1 ")
                .replaceAll("\\n+", "\n")
                .trim();
        
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
