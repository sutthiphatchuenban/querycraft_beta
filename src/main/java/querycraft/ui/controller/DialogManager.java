package querycraft.ui.controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import querycraft.model.ConnectionInfo;
import querycraft.query.QueryExecutorService;
import querycraft.ui.dialog.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Handles all popup dialogs and alerts for the application to offload UI logic from the MainController.
 */
public class DialogManager {

    private Window ownerWindow;
    private static File lastExportDirectory;

    public DialogManager(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    private File getInitialExportDirectory() {
        if (lastExportDirectory != null && lastExportDirectory.exists() && lastExportDirectory.isDirectory()) {
            return lastExportDirectory;
        }
        
        // Try Desktop
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists() && desktop.isDirectory() && desktop.canWrite()) {
            return desktop;
        }
        
        // Try Documents as fallback
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (docs.exists() && docs.isDirectory() && docs.canWrite()) {
            return docs;
        }

        return new File(System.getProperty("user.home"));
    }

    public void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (ownerWindow != null) {
                alert.initOwner(ownerWindow);
            }
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showInfo(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (ownerWindow != null) {
                alert.initOwner(ownerWindow);
            }
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public boolean confirmAction(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }
        alert.setTitle(title);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public boolean confirmBatchProcess() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        if (ownerWindow != null) {
            confirm.initOwner(ownerWindow);
        }
        confirm.setTitle("Confirm Batch Process");
        confirm.setHeaderText("Starting Archive/Cleanup Operation");
        confirm.setContentText("This will:\n1. Fetch data using SELECT\n2. Export to CSV & SQL INSERTs\n3. Permanently DELETE matched records\n\nProceed?");
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public Optional<ConnectionInfo> showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog();
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }
        return dialog.showAndWait();
    }

    public void showHelpDialog() {
        HelpDialog dialog = new HelpDialog();
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }
        dialog.showAndWait();
    }

    public Optional<Boolean> showSettingsDialog(QueryExecutorService executor) {
        SettingsDialog dialog = new SettingsDialog(executor);
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }
        return dialog.showAndWait().map(saved -> {
            if (saved) {
                dialog.saveSettings();
                return true;
            }
            return false;
        });
    }

    public Optional<ExportConfig> promptExportConfig() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File initialDir = getInitialExportDirectory();
        
        ExportDialog dialog = new ExportDialog("QueryCraft_Archive_" + timestamp + ".csv", initialDir);
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }
        dialog.setHeaderText("Batch Process config: This data will be exported, then DELETED.");
        
        Optional<ExportConfig> result = dialog.showAndWait();
        result.ifPresent(config -> {
            if (config.getFile() != null) {
                lastExportDirectory = config.getFile().getParentFile();
            }
        });
        return result;
    }

    public Optional<String> promptTargetTableName(String initialName) {
        TextInputDialog tableDialog = new TextInputDialog(initialName);
        if (ownerWindow != null) {
            tableDialog.initOwner(ownerWindow);
        }
        tableDialog.setTitle("Target Table Name");
        tableDialog.setHeaderText("Batch Process config: Specify target table for SQL INSERTs");
        tableDialog.setContentText("Table name:");
        return tableDialog.showAndWait();
    }

    public Optional<File> promptSqlFileSave(String defaultFileName, File initialDirectory) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SQL File");
        fileChooser.setInitialFileName(defaultFileName);
        
        File dir = (initialDirectory != null && initialDirectory.exists()) ? initialDirectory : getInitialExportDirectory();
        fileChooser.setInitialDirectory(dir);
        
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            lastExportDirectory = file.getParentFile();
        }
        return Optional.ofNullable(file);
    }
}
