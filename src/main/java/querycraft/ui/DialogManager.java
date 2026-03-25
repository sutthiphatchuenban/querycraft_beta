package querycraft.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import querycraft.model.ConnectionInfo;
import querycraft.service.QueryExecutorService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Handles all popup dialogs and alerts for the application to offload UI logic from the MainController.
 */
public class DialogManager {

    private final Window ownerWindow;

    public DialogManager(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    public void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(ownerWindow);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showInfo(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(ownerWindow);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public boolean confirmAction(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
        alert.initOwner(ownerWindow);
        alert.setTitle(title);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public boolean confirmBatchProcess() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(ownerWindow);
        confirm.setTitle("Confirm Batch Process");
        confirm.setHeaderText("Starting Archive/Cleanup Operation");
        confirm.setContentText("This will:\n1. Fetch data using SELECT\n2. Export to CSV & SQL INSERTs\n3. Permanently DELETE matched records\n\nProceed?");
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public Optional<ConnectionInfo> showConnectionDialog() {
        ConnectionDialog dialog = new ConnectionDialog();
        dialog.initOwner(ownerWindow);
        return dialog.showAndWait();
    }

    public void showHelpDialog() {
        HelpDialog dialog = new HelpDialog();
        dialog.initOwner(ownerWindow);
        dialog.showAndWait();
    }

    public Optional<Boolean> showSettingsDialog(QueryExecutorService executor) {
        SettingsDialog dialog = new SettingsDialog(executor);
        dialog.initOwner(ownerWindow);
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
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        
        ExportDialog dialog = new ExportDialog("QueryCraft_Archive_" + timestamp + ".csv", desktop);
        dialog.initOwner(ownerWindow);
        dialog.setHeaderText("Batch Process config: This data will be exported, then DELETED.");
        return dialog.showAndWait();
    }

    public Optional<String> promptTargetTableName(String initialName) {
        TextInputDialog tableDialog = new TextInputDialog(initialName);
        tableDialog.initOwner(ownerWindow);
        tableDialog.setTitle("Target Table Name");
        tableDialog.setHeaderText("Batch Process config: Specify target table for SQL INSERTs");
        tableDialog.setContentText("Table name:");
        return tableDialog.showAndWait();
    }

    public Optional<File> promptSqlFileSave(String defaultFileName, File initialDirectory) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SQL File");
        fileChooser.setInitialFileName(defaultFileName);
        if (initialDirectory != null) {
            fileChooser.setInitialDirectory(initialDirectory);
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
        return Optional.ofNullable(fileChooser.showSaveDialog(ownerWindow));
    }
}
