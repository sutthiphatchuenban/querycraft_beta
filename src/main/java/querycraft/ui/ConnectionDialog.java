package querycraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import querycraft.model.ConnectionInfo;
import querycraft.model.DatabaseType;
import querycraft.service.DatabaseConnectionService;

import java.sql.SQLException;

/**
 * Dialog for database connection configuration.
 */
public class ConnectionDialog extends Dialog<ConnectionInfo> {

    private final DatabaseConnectionService connectionService;

    public ConnectionDialog() {
        this.connectionService = DatabaseConnectionService.getInstance();

        setTitle("Database Connection");
        setHeaderText("Connect to Database");
        initModality(Modality.APPLICATION_MODAL);

        // Create form fields
        ComboBox<DatabaseType> dbTypeCombo = new ComboBox<>();
        dbTypeCombo.getItems().addAll(DatabaseType.values());
        dbTypeCombo.setValue(DatabaseType.MYSQL);

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField(String.valueOf(DatabaseType.MYSQL.getDefaultPort()));
        TextField databaseField = new TextField();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();

        // Update default port when database type changes
        dbTypeCombo.setOnAction(e -> {
            DatabaseType selected = dbTypeCombo.getValue();
            if (selected != null) {
                portField.setText(String.valueOf(selected.getDefaultPort()));
            }
        });

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Database Type:"), 0, 0);
        grid.add(dbTypeCombo, 1, 0);
        grid.add(new Label("Host:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("Port:"), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label("Database:"), 0, 3);
        grid.add(databaseField, 1, 3);
        grid.add(new Label("Username:"), 0, 4);
        grid.add(usernameField, 1, 4);
        grid.add(new Label("Password:"), 0, 5);
        grid.add(passwordField, 1, 5);

        // Test connection button
        Button testButton = new Button("Test Connection");
        testButton.setOnAction(e -> {
            ConnectionInfo info = buildConnectionInfo(
                    dbTypeCombo.getValue(),
                    hostField.getText(),
                    portField.getText(),
                    databaseField.getText(),
                    usernameField.getText(),
                    passwordField.getText()
            );

            if (info != null) {
                testConnection(info);
            }
        });

        HBox buttonBox = new HBox(10, testButton);
        grid.add(buttonBox, 1, 6);

        getDialogPane().setContent(grid);

        // Buttons
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(connectButtonType, cancelButtonType);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return buildConnectionInfo(
                        dbTypeCombo.getValue(),
                        hostField.getText(),
                        portField.getText(),
                        databaseField.getText(),
                        usernameField.getText(),
                        passwordField.getText()
                );
            }
            return null;
        });
    }

    private ConnectionInfo buildConnectionInfo(DatabaseType type, String host, String port,
                                                String database, String username, String password) {
        if (type == null || host.trim().isEmpty() || database.trim().isEmpty() ||
                username.trim().isEmpty()) {
            return null;
        }

        int portNum;
        try {
            portNum = Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            portNum = type.getDefaultPort();
        }

        ConnectionInfo info = new ConnectionInfo();
        info.setDatabaseType(type);
        info.setHost(host.trim());
        info.setPort(portNum);
        info.setDatabase(database.trim());
        info.setUsername(username.trim());
        info.setPassword(password);

        return info;
    }

    private void testConnection(ConnectionInfo info) {
        try {
            boolean success = connectionService.testConnection(info);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Connection Successful",
                        "Successfully connected to " + info.getDatabaseType().getDisplayName());
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Connection Failed",
                    "Failed to connect: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
