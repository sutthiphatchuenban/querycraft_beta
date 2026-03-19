package querycraft.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
    private TextArea queryTextArea;
    private TableView<Object[]> resultTable;
    private Label resultInfoLabel;
    private Button connectButton;
    private Button executeSelectButton;
    private Button executeDeleteButton;
    private Button exportCsvButton;
    private Button generateSqlButton;
    private Button disconnectButton;

    // Current query result
    private QueryResult currentResult;

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

        setCenter(centerPane);

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

        queryTextArea = new TextArea();
        queryTextArea.setPromptText("Enter your SQL query here...\nExample: SELECT * FROM users LIMIT 100");
        queryTextArea.setPrefRowCount(6);
        VBox.setVgrow(queryTextArea, Priority.ALWAYS);

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
        clearButton.setOnAction(e -> queryTextArea.clear());

        buttonBar.getChildren().addAll(executeSelectButton, executeDeleteButton, clearButton);

        box.getChildren().addAll(label, queryTextArea, buttonBar);
        return box;
    }

    private VBox createResultSection() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));

        // Result info label
        resultInfoLabel = new Label("No results");

        // Result table
        resultTable = new TableView<>();
        resultTable.setPlaceholder(new Label("Execute a query to see results"));
        VBox.setVgrow(resultTable, Priority.ALWAYS);

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

        exportBar.getChildren().addAll(exportCsvButton, generateSqlButton);

        box.getChildren().addAll(resultInfoLabel, resultTable, exportBar);
        return box;
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
        String sql = queryTextArea.getText().trim();
        if (sql.isEmpty()) {
            showError("Empty Query", "Please enter a SQL query.");
            return;
        }

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
            ExportDialog dialog = new ExportDialog(defaultFilename);
            dialog.initOwner(getScene().getWindow());

            ExportConfig config = dialog.showAndWait().orElse(null);
            if (config != null) {
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
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SQL Files", "*.sql")
            );

            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
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
