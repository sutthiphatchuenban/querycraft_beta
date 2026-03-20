package querycraft.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import querycraft.model.*;
import querycraft.service.DatabaseConnectionService;
import querycraft.service.QueryExecutorService;
import querycraft.util.CsvExporter;
import querycraft.util.SqlInsertGenerator;

import java.io.File;

/**
 * Main controller for the QueryCraft application.
 */
public class MainController extends BorderPane {

    private final DatabaseConnectionService connectionService;
    private final QueryExecutorService queryExecutor;

    // UI Components
    private Label statusLabel;
    private Label dbInfoLabel;
    private SqlEditor queryEditor;
    private TableView<Object[]> resultTable;
    private TextField filterField;
    private Label resultInfoLabel;
    private Button connectButton;
    private Button executeSelectButton;
    private Button executeDeleteButton;
    private Button exportCsvButton;
    private Button generateSqlButton;
    private Button disconnectButton;
    private ListView<String> tableListView;
    private ListView<String> historyListView;
    private ObservableList<String> historyData = FXCollections.observableArrayList();

    // Current query result
    private QueryResult currentResult;
    private ObservableList<Object[]> masterData = FXCollections.observableArrayList();
    private int currentPage = 0;
    private static final int PAGE_SIZE = 100;

    // Persist last directory for the session
    private File lastExportDirectory;

    public MainController() {
        this.connectionService = DatabaseConnectionService.getInstance();
        this.queryExecutor = new QueryExecutorService();

        initializeUI();
        updateConnectionStatus();
    }

    private void initializeUI() {
        // Top section - Connection bar
        HBox topBar = createTopBar();
        setTop(topBar);

        // SplitPane for Sidebar and Main Content
        SplitPane mainSplitPane = new SplitPane();

        // Sidebar - Tables List
        VBox sidebar = createTableSidebar();
        mainSplitPane.getItems().add(sidebar);

        // Center section - Query and results
        SplitPane centerPane = new SplitPane();
        centerPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

        // Query area
        VBox queryBox = createQuerySection();
        centerPane.getItems().add(queryBox);

        // Results area
        VBox resultBox = createResultSection();
        centerPane.getItems().add(resultBox);
        centerPane.setDividerPositions(0.3);

        mainSplitPane.getItems().add(centerPane);
        mainSplitPane.setDividerPositions(0.2);

        setCenter(mainSplitPane);

        // Bottom section - Status bar
        HBox statusBar = createStatusBar();
        setBottom(statusBar);

        // Set padding
        setPadding(new Insets(5));
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
        disconnectButton.setDisable(true);

        dbInfoLabel = new Label("Not connected");
        dbInfoLabel.getStyleClass().add("db-info-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(connectButton, disconnectButton, spacer, dbInfoLabel);
        return bar;
    }

    private VBox createQuerySection() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));

        Label label = new Label("SQL Query:");

        queryEditor = new SqlEditor();
        queryEditor.setPlaceholder(new Label("Enter your SQL query here...\nExample: SELECT * FROM users LIMIT 100"));
        queryEditor.setPrefHeight(200);
        VBox.setVgrow(queryEditor, Priority.ALWAYS);

        // Button bar for query execution
        HBox buttonBar = new HBox(10);

        executeSelectButton = new Button("Execute SELECT");
        executeSelectButton.getStyleClass().add("button-success");
        executeSelectButton.setOnAction(e -> executeQuery(false));
        executeSelectButton.setDisable(true);

        executeDeleteButton = new Button("Execute DELETE");
        executeDeleteButton.getStyleClass().add("button-danger");
        executeDeleteButton.setOnAction(e -> executeQuery(true));
        executeDeleteButton.setDisable(true);

        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("button-neutral");
        clearButton.setOnAction(e -> queryEditor.clear());

        Button formatButton = new Button("Format SQL");
        formatButton.getStyleClass().add("button-neutral");
        formatButton.setOnAction(e -> formatSql());

        buttonBar.getChildren().addAll(executeSelectButton, executeDeleteButton, clearButton, formatButton);

        box.getChildren().addAll(label, queryEditor, buttonBar);
        return box;
    }

    private VBox createResultSection() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));

        // Search/Filter Bar
        HBox filterBar = new HBox(10);
        filterBar.setPadding(new Insets(0, 0, 5, 0));
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label filterLabel = new Label("Filter Results:");
        filterField = new TextField();
        filterField.setPromptText("Type to filter data...");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
        
        filterBar.getChildren().addAll(filterLabel, filterField);

        // Result info label
        resultInfoLabel = new Label("No results");

        // Result table
        resultTable = new TableView<>();
        resultTable.setPlaceholder(new Label("Execute a query to see results"));
        resultTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        resultTable.getSelectionModel().setCellSelectionEnabled(true);
        VBox.setVgrow(resultTable, Priority.ALWAYS);

        // Add copy context menu
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY));
        copyItem.setOnAction(e -> copySelectionToClipboard());

        MenuItem clearSelectionItem = new MenuItem("Clear Selection");
        clearSelectionItem.setOnAction(e -> resultTable.getSelectionModel().clearSelection());

        resultTable.setContextMenu(new ContextMenu(copyItem, clearSelectionItem));

        // Keyboard shortcuts for copy and clear selection
        resultTable.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                copySelectionToClipboard();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                resultTable.getSelectionModel().clearSelection();
                event.consume();
            }
        });

        // Toggle selection on click (allows deselecting by clicking again)
        resultTable.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void handle(javafx.scene.input.MouseEvent event) {
                if (event.getClickCount() == 1 && !event.isControlDown() && !event.isShiftDown()) {
                    TablePosition pos = resultTable.getFocusModel().getFocusedCell();
                    if (pos != null && pos.getRow() != -1) {
                        if (resultTable.getSelectionModel().isSelected(pos.getRow(), pos.getTableColumn())) {
                            resultTable.getSelectionModel().clearSelection(pos.getRow(), pos.getTableColumn());
                        }
                    }
                }
            }
        });

        // Export button bar
        HBox exportBar = new HBox(10);

        exportCsvButton = new Button("Export to CSV...");
        exportCsvButton.getStyleClass().add("button-primary");
        exportCsvButton.setOnAction(e -> exportToCsv());
        exportCsvButton.setDisable(true);

        generateSqlButton = new Button("Generate SQL INSERTs...");
        generateSqlButton.getStyleClass().add("button-primary");
        generateSqlButton.setOnAction(e -> generateSqlInserts());
        generateSqlButton.setDisable(true);

        // Pagination Bar
        HBox paginationBar = new HBox(10);
        paginationBar.setAlignment(javafx.geometry.Pos.CENTER);
        paginationBar.setPadding(new Insets(5, 0, 0, 0));
        
        Button prevButton = new Button("Previous");
        prevButton.setOnAction(e -> changePage(-1));
        
        Button nextButton = new Button("Next");
        nextButton.setOnAction(e -> changePage(1));
        
        paginationBar.getChildren().addAll(prevButton, nextButton);

        exportBar.getChildren().addAll(exportCsvButton, generateSqlButton);

        box.getChildren().addAll(filterBar, resultInfoLabel, resultTable, paginationBar, exportBar);
        return box;
    }

    private VBox createTableSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(10));
        sidebar.setMinWidth(200);
        sidebar.setPrefWidth(220);

        Label label = new Label("Tables");
        label.getStyleClass().add("sidebar-header");
        
        tableListView = new ListView<>();
        tableListView.getStyleClass().add("sidebar-list");
        VBox.setVgrow(tableListView, Priority.ALWAYS);
        
        tableListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedTable = tableListView.getSelectionModel().getSelectedItem();
                if (selectedTable != null) {
                    queryEditor.replaceText("SELECT * FROM " + selectedTable + " LIMIT 100");
                }
            }
        });

        // Context menu for table list
        ContextMenu contextMenu = new ContextMenu();
        MenuItem describeItem = new MenuItem("Describe Structure");
        describeItem.setOnAction(e -> {
            String selectedTable = tableListView.getSelectionModel().getSelectedItem();
            if (selectedTable != null) {
                describeTable(selectedTable);
            }
        });
        
        MenuItem selectItem = new MenuItem("SELECT * (Top 100)");
        selectItem.setOnAction(e -> {
            String selectedTable = tableListView.getSelectionModel().getSelectedItem();
            if (selectedTable != null) {
                queryEditor.replaceText("SELECT * FROM " + selectedTable + " LIMIT 100");
                executeQuery(false);
            }
        });

        contextMenu.getItems().addAll(describeItem, selectItem);
        tableListView.setContextMenu(contextMenu);

        Button refreshBtn = new Button("Refresh Tables");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> fetchTables());

        sidebar.getChildren().addAll(label, tableListView, refreshBtn);

        // History Section
        Label historyLabel = new Label("Recent Queries");
        historyLabel.getStyleClass().add("sidebar-header");
        historyLabel.setPadding(new Insets(20, 0, 10, 0));
        
        historyListView = new ListView<>(historyData);
        historyListView.getStyleClass().add("sidebar-list");
        historyListView.setPrefHeight(300);
        VBox.setVgrow(historyListView, Priority.ALWAYS);
        
        historyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedQuery = historyListView.getSelectionModel().getSelectedItem();
                if (selectedQuery != null) {
                    queryEditor.replaceText(selectedQuery);
                }
            }
        });

        sidebar.getChildren().addAll(historyLabel, historyListView);
        return sidebar;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("status-bar");

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label appLabel = new Label("QueryCraft v1.0");
        appLabel.getStyleClass().add("status-label");

        bar.getChildren().addAll(statusLabel, spacer, appLabel);
        return bar;
    }

    private void showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog();
        dialog.initOwner(getScene().getWindow());

        ConnectionInfo result = dialog.showAndWait().orElse(null);
        if (result != null) {
            connect(result);
        }
    }

    private void connect(ConnectionInfo info) {
        try {
            setStatus("Connecting to " + info.getDatabaseType().getDisplayName() + "...");
            connectionService.connect(info);
            updateConnectionStatus();
            fetchTables();
            setStatus("Connected to " + info);
        } catch (Throwable e) {
            e.printStackTrace();
            showError("Connection Failed", "An unexpected error occurred during connection:\n" + e.toString());
            setStatus("Connection failed");
        }
    }

    private void disconnect() {
        try {
            connectionService.disconnect();
            updateConnectionStatus();
            clearResults();
            setStatus("Disconnected");
        } catch (Throwable e) {
            e.printStackTrace();
            showError("Disconnect Error", "Failed to disconnect: " + e.getMessage());
        }
    }

    private void updateConnectionStatus() {
        boolean connected = connectionService.isConnected();

        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        executeSelectButton.setDisable(!connected);
        executeDeleteButton.setDisable(!connected);

        if (connected) {
            ConnectionInfo info = connectionService.getCurrentConnectionInfo();
            dbInfoLabel.setText(info.getDatabaseType().getDisplayName() + " | " + info.getHost() + "/" + info.getDatabase());
        } else {
            dbInfoLabel.setText("Not connected");
        }
    }

    private void executeQuery(boolean isDelete) {
        String sql = queryEditor.getText().trim();
        if (sql.isEmpty()) {
            showError("Empty Query", "Please enter a SQL query.");
            return;
        }

        // Add to history
        addToHistory(sql);

        // Validate query
        QueryExecutorService.ValidationResult validation = queryExecutor.validateQuery(sql);
        if (!validation.isValid()) {
            showError("Invalid Query", validation.getMessage());
            return;
        }

        // Check if query type matches button
        if (!isDelete && queryExecutor.isDeleteQuery(sql)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm DELETE");
            alert.setHeaderText("This is a DELETE query");
            alert.setContentText("The query appears to be a DELETE statement. Are you sure you want to execute it as a SELECT?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        if (isDelete && !queryExecutor.isDeleteQuery(sql)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Execution");
            alert.setHeaderText("Execute as DELETE?");
            alert.setContentText("The query doesn't appear to be a DELETE statement. Execute anyway?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        // Confirm DELETE operations
        if (isDelete || queryExecutor.isDeleteQuery(sql)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm DELETE");
            alert.setHeaderText("Warning: DELETE Operation");
            alert.setContentText("This will delete data from the database. Are you sure you want to proceed?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        setStatus("Executing query...");

        try {
            QueryResult result;
            if (isDelete) {
                result = queryExecutor.executeDelete(sql);
            } else {
                result = queryExecutor.executeSelect(sql);
            }

            displayResult(result);

            if (result.hasError()) {
                setStatus("Query failed: " + result.getErrorMessage());
            } else {
                setStatus(String.format("Query completed in %d ms", result.getExecutionTimeMs()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            showError("Query Execution Error", "An unexpected error occurred during execution:\n" + e.toString());
            setStatus("Query failed");
        }
    }

    private void displayResult(QueryResult result) {
        clearResults();
        this.currentResult = result;

        if (result.hasError()) {
            resultInfoLabel.setText("Error: " + result.getErrorMessage());
            resultInfoLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        if (result.isSelectQuery()) {
            // Create table columns dynamically
            for (ColumnInfo col : result.getColumns()) {
                TableColumn<Object[], Object> tableCol = new TableColumn<>(col.getName());
                final int colIndex = result.getColumns().indexOf(col);

                tableCol.setCellValueFactory(data -> {
                    Object[] row = data.getValue();
                    Object value = colIndex < row.length ? row[colIndex] : null;
                    return new javafx.beans.property.SimpleObjectProperty<>(value);
                });

                tableCol.setPrefWidth(150);
                resultTable.getColumns().add(tableCol);
            }

            // Add data
            ObservableList<Object[]> data = FXCollections.observableArrayList(result.getRows());
            resultTable.setItems(data);

            resultInfoLabel.setText(String.format("Showing %d rows (fetched in %d ms)",
                    result.getRowCount(), result.getExecutionTimeMs()));
            resultInfoLabel.setStyle("-fx-text-fill: #333333;");

            // Enable export buttons
            exportCsvButton.setDisable(false);
            generateSqlButton.setDisable(false);

            // Setup Pagination
            masterData.setAll(result.getRows());
            currentPage = 0;
            updateTableData();
        } else {
            // Non-SELECT query result
            resultInfoLabel.setText(String.format("Affected %d rows (completed in %d ms)",
                    result.getAffectedRows(), result.getExecutionTimeMs()));
            resultInfoLabel.setStyle("-fx-text-fill: #4CAF50;");

            exportCsvButton.setDisable(true);
            generateSqlButton.setDisable(true);
        }
    }

    private void clearResults() {
        resultTable.getColumns().clear();
        resultTable.getItems().clear();
        resultInfoLabel.setText("No results");
        resultInfoLabel.setStyle("-fx-text-fill: #333333;");
        currentResult = null;
        exportCsvButton.setDisable(true);
        generateSqlButton.setDisable(true);
    }

    private void exportToCsv() {
        try {
            if (currentResult == null) {
                showError("No Data", "There is no data to export.");
                return;
            }
            if (currentResult.getRows() == null || currentResult.getRows().isEmpty()) {
                showError("No Data", "There is no data to export.");
                return;
            }

            String defaultFilename = CsvExporter.generateFilename("export", "csv");
            ExportDialog dialog = new ExportDialog(defaultFilename, lastExportDirectory);
            dialog.initOwner(getScene().getWindow());

            ExportConfig config = dialog.showAndWait().orElse(null);
            if (config != null) {
                lastExportDirectory = config.getFile().getParentFile();
                CsvExporter.export(currentResult, config.getFile(), config.getOptions());
                setStatus("Exported to " + config.getFile().getName());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Complete");
                alert.setHeaderText(null);
                alert.setContentText("Data exported successfully to:\n" + config.getFile().getAbsolutePath());
                alert.showAndWait();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            showError("Export Failed", e);
        }
    }

    private void generateSqlInserts() {
        try {
            if (currentResult == null) {
                showError("No Data", "There is no data to generate SQL from.");
                return;
            }
            if (currentResult.getRows() == null || currentResult.getRows().isEmpty()) {
                showError("No Data", "There is no data to generate SQL from.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog("exported_data");
            dialog.setTitle("Generate SQL INSERTs");
            dialog.setHeaderText("Enter target table name:");
            dialog.setContentText("Table name:");
            dialog.initOwner(getScene().getWindow());

            String tableName = dialog.showAndWait().orElse(null);
            if (tableName == null || tableName.trim().isEmpty()) {
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save SQL File");
            fileChooser.setInitialFileName(SqlInsertGenerator.generateTableName(currentResult) + ".sql");
            if (lastExportDirectory != null && lastExportDirectory.exists()) {
                fileChooser.setInitialDirectory(lastExportDirectory);
            }
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SQL Files", "*.sql")
            );

            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                lastExportDirectory = file.getParentFile();
                SqlInsertGenerator.generate(currentResult, file, tableName.trim());
                setStatus("SQL saved to " + file.getName());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("SQL Generated");
                alert.setHeaderText(null);
                alert.setContentText("SQL INSERT statements saved to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            showError("Generation Failed", e);
        }
    }

    @SuppressWarnings("rawtypes")
    private void copySelectionToClipboard() {
        ObservableList<TablePosition> selectedCells = resultTable.getSelectionModel().getSelectedCells();
        if (selectedCells.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        int lastRow = -1;

        for (TablePosition pos : selectedCells) {
            int row = pos.getRow();
            int col = pos.getColumn();
            Object value = resultTable.getColumns().get(col).getCellData(row);

            if (lastRow != -1) {
                if (row != lastRow) {
                    sb.append("\n");
                } else {
                    sb.append("\t");
                }
            }
            sb.append(value == null ? "" : value.toString());
            lastRow = row;
        }

        final ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void applyFilter(String filter) {
        if (currentResult == null || currentResult.getRows() == null) return;
        
        if (filter == null || filter.isEmpty()) {
            masterData.setAll(currentResult.getRows());
        } else {
            String lowerFilter = filter.toLowerCase();
            java.util.List<Object[]> filtered = currentResult.getRows().stream()
                    .filter(row -> {
                        for (Object cell : row) {
                            if (cell != null && cell.toString().toLowerCase().contains(lowerFilter)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());
            masterData.setAll(filtered);
        }
        
        currentPage = 0;
        updateTableData();
    }

    private void changePage(int delta) {
        int totalPages = (int) Math.ceil((double) masterData.size() / PAGE_SIZE);
        if (totalPages == 0) return;
        
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            updateTableData();
        }
    }

    private void updateTableData() {
        if (masterData.isEmpty()) {
            resultTable.getItems().clear();
            resultInfoLabel.setText("No matching results");
            return;
        }

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, masterData.size());
        
        resultTable.getItems().setAll(masterData.subList(start, end));
        
        int totalPages = (int) Math.ceil((double) masterData.size() / PAGE_SIZE);
        resultInfoLabel.setText(String.format("Showing %d-%d of %d rows (Page %d of %d) | Total %d rows in result", 
                start + 1, end, masterData.size(), currentPage + 1, totalPages, currentResult.getRowCount()));
    }

    private void fetchTables() {
        if (!connectionService.isConnected()) return;
        
        DatabaseType type = connectionService.getCurrentConnectionInfo().getDatabaseType();
        String query = type.getShowTablesQuery();
        
        if (query.isEmpty()) return;
        
        setStatus("Fetching tables...");
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    ObservableList<String> tables = FXCollections.observableArrayList();
                    for (Object[] row : result.getRows()) {
                        if (row.length > 0 && row[0] != null) {
                            tables.add(row[0].toString());
                        }
                    }
                    tableListView.setItems(tables);
                    queryEditor.setTableNames(new java.util.ArrayList<>(tables));
                    setStatus("Tables refreshed");
                });
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    setStatus("Error fetching tables: " + e.getMessage());
                });
            }
        });
    }

    private void describeTable(String tableName) {
        if (!connectionService.isConnected()) return;
        
        DatabaseType type = connectionService.getCurrentConnectionInfo().getDatabaseType();
        String query = type.getDescribeTableQuery(tableName);
        
        if (query.isEmpty()) return;
        
        setStatus("Describing table " + tableName + "...");
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    displayResult(result);
                    setStatus("Described table: " + tableName);
                });
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    showError("Describe Failed", "Could not describe table " + tableName + ":\n" + e.getMessage());
                });
            }
        });
    }

    private void addToHistory(String sql) {
        Platform.runLater(() -> {
            // Remove if exists to move to top
            historyData.remove(sql);
            historyData.add(0, sql);
            
            // Keep last 50
            if (historyData.size() > 50) {
                historyData.remove(50);
            }
        });
    }

    private void formatSql() {
        String text = queryEditor.getText().trim();
        if (text.isEmpty()) return;

        // Basic Formatting Logic
        String formatted = text
                .replaceAll("(?i)\\bSELECT\\b", "SELECT")
                .replaceAll("(?i)\\bFROM\\b", "\nFROM")
                .replaceAll("(?i)\\bWHERE\\b", "\nWHERE")
                .replaceAll("(?i)\\bAND\\b", "\n  AND")
                .replaceAll("(?i)\\bOR\\b", "\n  OR")
                .replaceAll("(?i)\\bGROUP BY\\b", "\nGROUP BY")
                .replaceAll("(?i)\\bORDER BY\\b", "\nORDER BY")
                .replaceAll("(?i)\\bLEFT JOIN\\b", "\nLEFT JOIN")
                .replaceAll("(?i)\\bINNER JOIN\\b", "\nINNER JOIN")
                .replaceAll("(?i)\\bJOIN\\b", "\nJOIN")
                .replaceAll("(?i)\\bLIMIT\\b", "\nLIMIT");

        queryEditor.replaceText(formatted);
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
            alert.initOwner(getScene().getWindow());
            alert.showAndWait();
        });
    }

    private void showError(String title, Throwable e) {
        StringBuilder fullMessage = new StringBuilder(e.toString());
        if (e.getCause() != null) {
            fullMessage.append("\n\nCause: ").append(e.getCause().toString());
        }
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(fullMessage.toString());
            alert.initOwner(getScene().getWindow());
            alert.showAndWait();
        });
    }
}
