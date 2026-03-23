package querycraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import querycraft.service.DatabaseConnectionService;
import querycraft.service.QueryExecutorService;

import java.util.prefs.Preferences;

/**
 * Settings dialog for configuring QueryCraft options.
 */
public class SettingsDialog extends Dialog<Boolean> {

    private final Preferences prefs = Preferences.userNodeForPackage(SettingsDialog.class);
    private final QueryExecutorService queryExecutor;

    public SettingsDialog(QueryExecutorService queryExecutor) {
        this.queryExecutor = queryExecutor;

        setTitle("Settings");
        setHeaderText("Configure QueryCraft Options");
        initModality(Modality.APPLICATION_MODAL);

        // Create settings tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Query Settings Tab
        Tab queryTab = new Tab("Query Settings", createQuerySettingsPane());
        queryTab.setClosable(false);

        // Connection Settings Tab
        Tab connectionTab = new Tab("Connection", createConnectionSettingsPane());
        connectionTab.setClosable(false);

        tabPane.getTabs().addAll(queryTab, connectionTab);

        getDialogPane().setContent(tabPane);
        getDialogPane().setPrefSize(450, 300);

        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Result converter
        setResultConverter(dialogButton -> dialogButton == saveButtonType);
    }

    private GridPane createQuerySettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 20, 10, 20));

        // Query Timeout
        Label timeoutLabel = new Label("Query Timeout (seconds):");
        Spinner<Integer> timeoutSpinner = new Spinner<>(5, 300, queryExecutor.getQueryTimeout(), 5);
        timeoutSpinner.setPrefWidth(100);
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setId("timeoutSpinner");

        Label timeoutHint = new Label("Range: 5-300 seconds");
        timeoutHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        // Max Rows
        Label maxRowsLabel = new Label("Max Rows Limit:");
        Spinner<Integer> maxRowsSpinner = new Spinner<>(100, 50000, getMaxRows(), 100);
        maxRowsSpinner.setPrefWidth(100);
        maxRowsSpinner.setEditable(true);
        maxRowsSpinner.setId("maxRowsSpinner");

        Label maxRowsHint = new Label("Maximum rows to fetch per query");
        maxRowsHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        // Auto-format SQL
        CheckBox autoFormatCheck = new CheckBox("Auto-format SQL on paste");
        autoFormatCheck.setSelected(isAutoFormatEnabled());
        autoFormatCheck.setId("autoFormatCheck");

        // Confirm DELETE
        CheckBox confirmDeleteCheck = new CheckBox("Confirm DELETE operations");
        confirmDeleteCheck.setSelected(isConfirmDeleteEnabled());
        confirmDeleteCheck.setId("confirmDeleteCheck");

        grid.add(timeoutLabel, 0, 0);
        grid.add(timeoutSpinner, 1, 0);
        grid.add(timeoutHint, 1, 1);

        grid.add(maxRowsLabel, 0, 2);
        grid.add(maxRowsSpinner, 1, 2);
        grid.add(maxRowsHint, 1, 3);

        grid.add(autoFormatCheck, 0, 4, 2, 1);
        grid.add(confirmDeleteCheck, 0, 5, 2, 1);

        return grid;
    }

    private GridPane createConnectionSettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 20, 10, 20));

        // Connection Pool Size
        Label poolSizeLabel = new Label("Max Pool Size:");
        Spinner<Integer> poolSizeSpinner = new Spinner<>(2, 50, getPoolSize(), 1);
        poolSizeSpinner.setPrefWidth(100);
        poolSizeSpinner.setEditable(true);
        poolSizeSpinner.setId("poolSizeSpinner");

        Label poolSizeHint = new Label("Maximum connections in pool");
        poolSizeHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        // Connection Timeout
        Label connTimeoutLabel = new Label("Connection Timeout (seconds):");
        Spinner<Integer> connTimeoutSpinner = new Spinner<>(5, 120, getConnectionTimeout(), 5);
        connTimeoutSpinner.setPrefWidth(100);
        connTimeoutSpinner.setEditable(true);
        connTimeoutSpinner.setId("connTimeoutSpinner");

        Label connTimeoutHint = new Label("Timeout for establishing connection");
        connTimeoutHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        // SSL by default
        CheckBox sslDefaultCheck = new CheckBox("Enable SSL by default for new connections");
        sslDefaultCheck.setSelected(isSslDefault());
        sslDefaultCheck.setId("sslDefaultCheck");

        grid.add(poolSizeLabel, 0, 0);
        grid.add(poolSizeSpinner, 1, 0);
        grid.add(poolSizeHint, 1, 1);

        grid.add(connTimeoutLabel, 0, 2);
        grid.add(connTimeoutSpinner, 1, 2);
        grid.add(connTimeoutHint, 1, 3);

        grid.add(sslDefaultCheck, 0, 4, 2, 1);

        return grid;
    }

    /**
     * Save all settings to preferences.
     */
    public void saveSettings() {
        // Get controls from Query Settings tab
        TabPane tabPane = (TabPane) getDialogPane().getContent();
        GridPane queryPane = (GridPane) tabPane.getTabs().get(0).getContent();
        GridPane connPane = (GridPane) tabPane.getTabs().get(1).getContent();

        // Query settings
        Spinner<Integer> timeoutSpinner = lookupSpinner(queryPane, "#timeoutSpinner");
        Spinner<Integer> maxRowsSpinner = lookupSpinner(queryPane, "#maxRowsSpinner");
        CheckBox autoFormatCheck = (CheckBox) queryPane.lookup("#autoFormatCheck");
        CheckBox confirmDeleteCheck = (CheckBox) queryPane.lookup("#confirmDeleteCheck");

        // Connection settings
        Spinner<Integer> poolSizeSpinner = lookupSpinner(connPane, "#poolSizeSpinner");
        Spinner<Integer> connTimeoutSpinner = lookupSpinner(connPane, "#connTimeoutSpinner");
        CheckBox sslDefaultCheck = (CheckBox) connPane.lookup("#sslDefaultCheck");

        // Save to preferences
        prefs.putInt("queryTimeout", timeoutSpinner.getValue());
        prefs.putInt("maxRows", maxRowsSpinner.getValue());
        prefs.putBoolean("autoFormat", autoFormatCheck.isSelected());
        prefs.putBoolean("confirmDelete", confirmDeleteCheck.isSelected());
        prefs.putInt("poolSize", poolSizeSpinner.getValue());
        prefs.putInt("connectionTimeout", connTimeoutSpinner.getValue());
        prefs.putBoolean("sslDefault", sslDefaultCheck.isSelected());

        // Apply to services
        queryExecutor.setQueryTimeout(timeoutSpinner.getValue());
        DatabaseConnectionService.getInstance().applyRuntimeSettings(
            poolSizeSpinner.getValue(),
            connTimeoutSpinner.getValue()
        );
    }

    @SuppressWarnings("unchecked")
    private Spinner<Integer> lookupSpinner(GridPane pane, String selector) {
        return (Spinner<Integer>) pane.lookup(selector);
    }

    // Preference getters
    public int getQueryTimeout() {
        return prefs.getInt("queryTimeout", 30);
    }

    public int getMaxRows() {
        return prefs.getInt("maxRows", 10000);
    }

    public boolean isAutoFormatEnabled() {
        return prefs.getBoolean("autoFormat", false);
    }

    public boolean isConfirmDeleteEnabled() {
        return prefs.getBoolean("confirmDelete", true);
    }

    public int getPoolSize() {
        return prefs.getInt("poolSize", 10);
    }

    public int getConnectionTimeout() {
        return prefs.getInt("connectionTimeout", 30);
    }

    public boolean isSslDefault() {
        return prefs.getBoolean("sslDefault", false);
    }
}
