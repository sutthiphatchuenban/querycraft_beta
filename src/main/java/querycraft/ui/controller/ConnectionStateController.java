package querycraft.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.DbTable;
import querycraft.model.QueryResult;
import querycraft.connection.ConnectionObserver;
import querycraft.connection.DatabaseConnectionService;
import querycraft.query.QueryExecutorService;
import querycraft.ui.component.QueryEditorSection;
import querycraft.ui.component.ResultTableSection;
import querycraft.ui.component.SidebarSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles connection-related UI state and table discovery logic extracted from MainController.
 */
public class ConnectionStateController implements ConnectionObserver {

    private final DatabaseConnectionService connectionService;
    private final QueryExecutorService queryExecutor;
    private final SidebarSection sidebarSection;
    private final QueryEditorSection querySection;
    private final ResultTableSection resultSection;
    private final DialogManager dialogManager;
    private final Label dbInfoLabel;
    private final Button connectButton;
    private final Button disconnectButton;
    private final StatusReporter statusReporter;

    public ConnectionStateController(
            DatabaseConnectionService connectionService,
            QueryExecutorService queryExecutor,
            SidebarSection sidebarSection,
            QueryEditorSection querySection,
            ResultTableSection resultTable,
            DialogManager dialogManager,
            Label dbInfoLabel,
            Button connectButton,
            Button disconnectButton,
            StatusReporter statusReporter) {
        this.connectionService = connectionService;
        this.queryExecutor = queryExecutor;
        this.sidebarSection = sidebarSection;
        this.querySection = querySection;
        this.resultSection = resultTable;
        this.dialogManager = dialogManager;
        this.dbInfoLabel = dbInfoLabel;
        this.connectButton = connectButton;
        this.disconnectButton = disconnectButton;
        this.statusReporter = statusReporter;
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
            statusReporter.setStatus("Connected to " + info.getDatabase());
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            updateConnectionStatus();
            resultSection.displayResult(null);
            statusReporter.setStatus("Disconnected");
        });
    }

    @Override
    public void onConnectionFailed(Exception e) {
        Platform.runLater(() -> {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            dialogManager.showError("Connection Failed", message);
            statusReporter.setStatus("Connection Error: " + message);
            updateConnectionStatus();
        });
    }

    public void updateConnectionStatus() {
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

    public void fetchTablesForCsv() {
        if (!connectionService.isConnected()) {
            return;
        }

        ConnectionInfo info = connectionService.getCurrentConnectionInfo();
        if (info instanceof CsvConnectionInfo csvInfo) {
            Platform.runLater(() -> {
                ObservableList<DbTable> tables = FXCollections.observableArrayList();
                List<String> rawNames = new ArrayList<>();

                for (CsvConnectionInfo.CsvFileInfo csvFile : csvInfo.getCsvFiles()) {
                    tables.add(new DbTable(csvFile.getTableName(), "CSV"));
                    rawNames.add(csvFile.getTableName());
                }

                sidebarSection.setTables(tables);
                querySection.getEditor().setTableNames(rawNames);
                statusReporter.setStatus("CSV folder loaded: " + csvInfo.getCsvFileCount() + " file(s)");
            });
        }
    }

    public void fetchTables() {
        if (!connectionService.isConnected()) {
            return;
        }

        String query = connectionService.getCurrentConnectionInfo().getDatabaseType().getShowTablesQuery();
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> {
                    ObservableList<DbTable> tables = FXCollections.observableArrayList();
                    List<String> rawNames = new ArrayList<>();

                    for (Object[] row : result.getRows()) {
                        if (row.length > 0) {
                            String name = row[0].toString();
                            String type = row.length > 1 ? row[1].toString() : "TABLE";
                            tables.add(new DbTable(name, type));
                            rawNames.add(name);
                        }
                    }

                    sidebarSection.setTables(tables);
                    querySection.getEditor().setTableNames(rawNames);
                });
            }

            @Override
            public void onError(Exception e) {
                statusReporter.setStatus("Fetch tables failed");
            }
        });
    }

    public void describeTable(String tableName) {
        String query = connectionService.getCurrentConnectionInfo().getDatabaseType().getDescribeTableQuery(tableName);
        queryExecutor.executeQueryAsync(query, new QueryExecutorService.QueryCallback() {
            @Override
            public void onSuccess(QueryResult result) {
                Platform.runLater(() -> resultSection.displayResult(result));
            }

            @Override
            public void onError(Exception e) {
                dialogManager.showError("Describe Failed", e.getMessage());
            }
        });
    }

    @FunctionalInterface
    public interface StatusReporter {
        void setStatus(String message);
    }
}
