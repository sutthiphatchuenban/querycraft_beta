package querycraft.ui.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import querycraft.model.*;
import querycraft.ui.ExportConfig;
import querycraft.ui.ExportDialog;
import querycraft.util.DataExporter;
import querycraft.util.ExporterFactory;
import querycraft.service.DatabaseConnectionService;

import java.io.File;

/**
 * Component for the results table with filtering, pagination, and data export.
 */
public class ResultTableSection extends VBox {

    private final TableView<Object[]> resultTable;
    private final TextField filterField;
    private final Label resultInfoLabel;
    private final Button prevButton;
    private final Button nextButton;
    private final Button exportCsvButton;
    private final Button generateSqlButton;
    private final Button streamModeButton;
    private final StackPane tableContainer;
    private final ProgressIndicator loadingIndicator;
    private final StackPane loadingOverlay;
    
    private QueryResult currentResult;
    private File lastExportDirectory;
    private boolean streamingModeEnabled;
    private final ObservableList<Object[]> masterData = FXCollections.observableArrayList();
    private final FilteredList<Object[]> filteredData = new FilteredList<>(masterData);
    private int currentPage = 0;
    private static final int PAGE_SIZE = 100;
    private final javafx.animation.PauseTransition filterDebounce;

    public ResultTableSection() {
        super(5);
        this.setPadding(new Insets(10));
        VBox.setVgrow(this, Priority.ALWAYS);

        // Filter bar
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setPadding(new Insets(0, 0, 5, 0));

        Label filterLabel = new Label("Filter Results:");
        filterField = new TextField();
        filterField.setPromptText("Type to filter data...");
        filterField.setPrefWidth(300);
        
        // Debounce filter input
        filterDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        filterDebounce.setOnFinished(e -> applyFilter());
        
        filterField.textProperty().addListener((obs, oldV, newV) -> filterDebounce.playFromStart());

        filterBar.getChildren().addAll(filterLabel, filterField);

        resultInfoLabel = new Label("No results to display");
        resultInfoLabel.getStyleClass().add("result-info-label");

        resultTable = new TableView<>();
        VBox.setVgrow(resultTable, Priority.ALWAYS);
        resultTable.getSelectionModel().setCellSelectionEnabled(true);
        resultTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        resultTable.setPlaceholder(new Label("Execute a query to see results"));

        // Loading Overlay
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(50, 50);
        
        loadingOverlay = new StackPane(loadingIndicator);
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false); // Don't take space when hidden
        
        tableContainer = new StackPane(resultTable, loadingOverlay);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        setupTableContextMenu();

        // Pagination bar
        HBox paginationBar = new HBox(10);
        paginationBar.getStyleClass().add("pagination-bar");
        paginationBar.setAlignment(Pos.CENTER);
        paginationBar.setPadding(new Insets(10, 0, 0, 0));

        prevButton = new Button("Previous");
        prevButton.getStyleClass().add("button-neutral");
        prevButton.setOnAction(e -> changePage(-1));
        prevButton.setDisable(true);

        nextButton = new Button("Next");
        nextButton.getStyleClass().add("button-neutral");
        nextButton.setOnAction(e -> changePage(1));
        nextButton.setDisable(true);

        paginationBar.getChildren().addAll(prevButton, nextButton);

        // Export bar
        HBox exportBar = new HBox(10);
        exportBar.setPadding(new Insets(10, 0, 0, 0));

        exportCsvButton = new Button("Export to CSV...");
        exportCsvButton.getStyleClass().add("button-primary");
        exportCsvButton.setOnAction(e -> exportToCsv());
        exportCsvButton.setDisable(true);

        generateSqlButton = new Button("Generate SQL INSERTs...");
        generateSqlButton.getStyleClass().add("button-primary");
        generateSqlButton.setOnAction(e -> generateSqlInserts());
        generateSqlButton.setDisable(true);

        streamModeButton = new Button("Streaming: OFF");
        streamModeButton.getStyleClass().add("button-neutral");
        streamModeButton.setOnAction(e -> toggleStreamingMode());

        exportBar.getChildren().addAll(exportCsvButton, generateSqlButton, streamModeButton);

        this.getChildren().addAll(filterBar, resultInfoLabel, tableContainer, paginationBar, exportBar);
    }

    private void setupTableContextMenu() {
        ContextMenu ctx = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copy Selection");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY));
        copyItem.setOnAction(e -> copySelectionToClipboard());
        
        ctx.getItems().add(copyItem);
        resultTable.setContextMenu(ctx);
        
        resultTable.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                copySelectionToClipboard();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                resultTable.getSelectionModel().clearSelection();
                e.consume();
            }
        });
    }

    public void displayResult(QueryResult result) {
        this.currentResult = result;
        this.currentPage = 0;
        this.resultTable.getColumns().clear();
        this.masterData.clear();
        this.filterField.clear();

        if (result == null) {
            setLoading(false);
            resultInfoLabel.setText("No results (Not connected or query not executed)");
            resultInfoLabel.setStyle("-fx-text-fill: #333;");
            updatePaginationButtons();
            setExportButtonsEnabled(false);
            return;
        }

        if (result.hasError()) {
            setLoading(false);
            resultInfoLabel.setText("Error: " + result.getErrorMessage());
            resultInfoLabel.setStyle("-fx-text-fill: #f44336;"); // Red
            setExportButtonsEnabled(false);
            return;
        }

        if (result.isSelectQuery()) {
            setLoading(false);
            // Setup columns
            for (int i = 0; i < result.getColumns().size(); i++) {
                final int colIndex = i;
                TableColumn<Object[], Object> col = new TableColumn<>(result.getColumns().get(i).getName());
                col.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()[colIndex]));
                resultTable.getColumns().add(col);
            }

            masterData.setAll(result.getRows());
            updateTableData();
            resultInfoLabel.setStyle("-fx-text-fill: #333;");
            setExportButtonsEnabled(!result.getRows().isEmpty());
        } else {
            setLoading(false);
            // Non-SELECT (INSERT, UPDATE, DELETE)
            resultInfoLabel.setText(String.format("Successfully executed. Affected rows: %d (in %d ms)", 
                    result.getAffectedRows(), result.getExecutionTimeMs()));
            resultInfoLabel.setStyle("-fx-text-fill: #4CAF50;"); // Green
            updatePaginationButtons();
            setExportButtonsEnabled(false);
        }
    }

    private void applyFilter() {
        String filter = filterField.getText();
        if (filter == null || filter.trim().isEmpty()) {
            filteredData.setPredicate(null);
        } else {
            String lowerFilter = filter.toLowerCase();
            filteredData.setPredicate(row -> {
                for (Object cell : row) {
                    if (cell != null && cell.toString().toLowerCase().contains(lowerFilter)) {
                        return true;
                    }
                }
                return false;
            });
        }
        currentPage = 0;
        updateTableData();
    }

    private void changePage(int delta) {
        int totalPages = getTotalPages();
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            updateTableData();
        }
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) filteredData.size() / PAGE_SIZE);
    }

    private void updateTableData() {
        if (filteredData.isEmpty()) {
            resultTable.getItems().clear();
            if (currentResult != null && currentResult.isSelectQuery()) {
                resultInfoLabel.setText("No matches found for filter");
            }
            updatePaginationButtons();
            return;
        }

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredData.size());
        
        resultTable.getItems().setAll(filteredData.subList(start, end));
        updatePaginationButtons();

        int totalDocs = filteredData.size();
        int totalPages = getTotalPages();
        resultInfoLabel.setText(String.format("Showing %d-%d of %d matches (Page %d of %d) | Total %d rows in result", 
                start + 1, end, totalDocs, currentPage + 1, totalPages, currentResult.getRowCount()));
    }

    private void updatePaginationButtons() {
        int totalPages = getTotalPages();
        prevButton.setDisable(currentPage == 0 || totalPages <= 1);
        nextButton.setDisable(filteredData.isEmpty() || currentPage >= totalPages - 1);
    }

    private void setExportButtonsEnabled(boolean enabled) {
        exportCsvButton.setDisable(!enabled);
        generateSqlButton.setDisable(!enabled);
    }

    private void toggleStreamingMode() {
        streamingModeEnabled = !streamingModeEnabled;
        streamModeButton.setText(streamingModeEnabled ? "Streaming: ON" : "Streaming: OFF");
    }

    public boolean isStreamingModeEnabled() {
        return streamingModeEnabled;
    }

    private void exportToCsv() {
        if (currentResult == null || currentResult.getRows().isEmpty()) return;
        
        try {
            String defaultFilename = ExporterFactory.generateDefaultFilename("export", "csv");
            ExportDialog dialog = new ExportDialog(defaultFilename, lastExportDirectory);
            dialog.initOwner(this.getScene().getWindow());

            ExportConfig config = dialog.showAndWait().orElse(null);
            if (config != null) {
                DataExporter exporter = ExporterFactory.createCsvExporter(config.getOptions());
                performExport(exporter, config.getFile());
            }
        } catch (Exception e) {
            showError("Export Failed", e.getMessage());
        }
    }

    private void generateSqlInserts() {
        if (currentResult == null || currentResult.getRows().isEmpty()) return;
        
        try {
            TextInputDialog tableDialog = new TextInputDialog("exported_data");
            tableDialog.setTitle("Target Table Name");
            tableDialog.setHeaderText("Specify the target table for SQL INSERTs");
            tableDialog.setContentText("Table name:");
            tableDialog.initOwner(this.getScene().getWindow());

            String tableName = tableDialog.showAndWait().orElse(null);
            if (tableName == null || tableName.trim().isEmpty()) return;

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save SQL File");
            fileChooser.setInitialFileName(tableName.trim() + ".sql");
            if (lastExportDirectory != null) fileChooser.setInitialDirectory(lastExportDirectory);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
            
            File file = fileChooser.showSaveDialog(this.getScene().getWindow());
            if (file != null) {
                DatabaseType dbType = DatabaseConnectionService.getInstance().getCurrentConnectionInfo().getDatabaseType();
                DataExporter exporter = ExporterFactory.createSqlExporter(tableName.trim(), dbType);
                performExport(exporter, file);
            }
        } catch (Exception e) {
            showError("Generation Failed", e.getMessage());
        }
    }

    private void performExport(DataExporter exporter, File file) {
        try {
            lastExportDirectory = file.getParentFile();
            exporter.export(currentResult, file);
            showInfo(exporter.getDisplayName() + " Complete", 
                    "Data exported successfully to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Export Failed", e.getMessage());
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
                if (row != lastRow) sb.append("\n");
                else sb.append("\t");
            }
            sb.append(value == null ? "" : value.toString());
            lastRow = row;
        }

        final ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public TableView<Object[]> getTable() {
        return resultTable;
    }

    public void setLoading(boolean loading) {
        javafx.application.Platform.runLater(() -> {
            loadingOverlay.setVisible(loading);
            loadingOverlay.setManaged(loading);
            resultTable.setOpacity(loading ? 0.5 : 1.0);
            resultTable.setDisable(loading);
        });
    }
}
