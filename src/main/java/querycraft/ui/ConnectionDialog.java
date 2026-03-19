package querycraft.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import querycraft.model.ConnectionInfo;
import querycraft.model.DatabaseType;
import querycraft.service.DatabaseConnectionService;

import java.sql.SQLException;
import java.util.prefs.Preferences;

/**
 * Dialog for database connection configuration.
 */
public class ConnectionDialog extends Dialog<ConnectionInfo> {

    private final DatabaseConnectionService connectionService;
    private final Preferences prefs = Preferences.userNodeForPackage(ConnectionDialog.class);
    private static final int MAX_RECENT = 3;

    private static class RecentConnection {
        String type, host, port, database, username;
        boolean ssl;

        RecentConnection(String type, String host, String port, String database, String username, boolean ssl) {
            this.type = type;
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.ssl = ssl;
        }

        @Override
        public String toString() {
            return String.format("%s: %s@%s:%s", database, username, host, port);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RecentConnection)) return false;
            RecentConnection that = (RecentConnection) o;
            return type.equals(that.type) && host.equals(that.host) && port.equals(that.port) && 
                   database.equals(that.database) && username.equals(that.username);
        }
    }

    public ConnectionDialog() {
        this.connectionService = DatabaseConnectionService.getInstance();

        setTitle("Connect to Database");
        setHeaderText("Enter database connection details or select a recent one");
        initModality(Modality.APPLICATION_MODAL);

        // Load recent connections
        java.util.List<RecentConnection> recentList = loadRecentConnections();

        // Create form fields
        ComboBox<RecentConnection> recentCombo = new ComboBox<>();
        recentCombo.setPromptText("Select a recent connection...");
        recentCombo.getItems().addAll(recentList);
        recentCombo.setPrefWidth(300);

        ComboBox<DatabaseType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(DatabaseType.values());
        typeCombo.setValue(DatabaseType.MYSQL);

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField(String.valueOf(DatabaseType.MYSQL.getDefaultPort()));
        TextField databaseField = new TextField();
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        CheckBox sslCheck = new CheckBox("Use SSL (Required for Neon/Cloud)");
        CheckBox rememberCheck = new CheckBox("Remember Connection");
        rememberCheck.setSelected(true); // Default to checked as user wants history

        // Handle recent selection
        recentCombo.setOnAction(e -> {
            RecentConnection selected = recentCombo.getValue();
            if (selected != null) {
                try {
                    typeCombo.setValue(DatabaseType.valueOf(selected.type));
                    hostField.setText(selected.host);
                    portField.setText(selected.port);
                    databaseField.setText(selected.database);
                    usernameField.setText(selected.username);
                    sslCheck.setSelected(selected.ssl);
                } catch (Exception ex) {
                    // Ignore invalid saved data
                }
            }
        });

        // Detect neon.tech and auto-enable SSL
        hostField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.toLowerCase().contains("neon.tech")) {
                sslCheck.setSelected(true);
            }
        });

        // Update default port when database type changes
        typeCombo.setOnAction(e -> {
            DatabaseType selected = typeCombo.getValue();
            if (selected != null) {
                portField.setText(String.valueOf(selected.getDefaultPort()));
            }
        });

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Recent:"), 0, 0);
        grid.add(recentCombo, 1, 0);
        grid.add(new Label("Database Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Host:"), 0, 2);
        grid.add(hostField, 1, 2);
        grid.add(new Label("Port:"), 0, 3);
        grid.add(portField, 1, 3);
        grid.add(new Label("Database:"), 0, 4);
        grid.add(databaseField, 1, 4);
        grid.add(new Label("Username:"), 0, 5);
        grid.add(usernameField, 1, 5);
        grid.add(new Label("Password:"), 0, 6);
        grid.add(passwordField, 1, 6);
        grid.add(sslCheck, 1, 7);
        grid.add(rememberCheck, 1, 8);

        getDialogPane().setContent(grid);

        // Buttons
        ButtonType testButtonType = new ButtonType("Test Connection", ButtonBar.ButtonData.APPLY);
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(testButtonType, connectButtonType, cancelButtonType);

        // Test connection button
        Button testButton = (Button) getDialogPane().lookupButton(testButtonType);
        testButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                ConnectionInfo info = new ConnectionInfo(
                        typeCombo.getValue(),
                        hostField.getText(),
                        Integer.parseInt(portField.getText()),
                        databaseField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        sslCheck.isSelected()
                );

            if (info != null) {
                testConnection(info);
            }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "Port must be a valid number.");
            }
            event.consume(); // Prevent the dialog from closing
        });

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                try {
                    ConnectionInfo info = new ConnectionInfo(
                            typeCombo.getValue(),
                            hostField.getText(),
                            Integer.parseInt(portField.getText()),
                            databaseField.getText(),
                            usernameField.getText(),
                            passwordField.getText(),
                            sslCheck.isSelected()
                    );

                    // Save to recent list if 'Remember' is checked
                    if (rememberCheck.isSelected()) {
                        RecentConnection current = new RecentConnection(
                                info.getDatabaseType().name(),
                                info.getHost(),
                                String.valueOf(info.getPort()),
                                info.getDatabase(),
                                info.getUsername(),
                                info.isUseSSL()
                        );
                        saveRecentConnection(current, recentList);
                    }

                    return info;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Input Error", "Port must be a valid number.");
                    return null;
                }
            }
            return null;
        });
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

    private java.util.List<RecentConnection> loadRecentConnections() {
        java.util.List<RecentConnection> list = new java.util.ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String type = prefs.get("recent_" + i + "_type", null);
            if (type != null) {
                list.add(new RecentConnection(
                        type,
                        prefs.get("recent_" + i + "_host", ""),
                        prefs.get("recent_" + i + "_port", ""),
                        prefs.get("recent_" + i + "_db", ""),
                        prefs.get("recent_" + i + "_user", ""),
                        prefs.getBoolean("recent_" + i + "_ssl", false)
                ));
            }
        }
        return list;
    }

    private void saveRecentConnection(RecentConnection newConn, java.util.List<RecentConnection> list) {
        // Remove existing if duplicate to move it to top
        list.remove(newConn);
        // Add to top
        list.add(0, newConn);
        // Keep only top MAX_RECENT
        while (list.size() > MAX_RECENT) {
            list.remove(list.size() - 1);
        }

        // Save to preferences
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < list.size()) {
                RecentConnection c = list.get(i);
                prefs.put("recent_" + i + "_type", c.type);
                prefs.put("recent_" + i + "_host", c.host);
                prefs.put("recent_" + i + "_port", c.port);
                prefs.put("recent_" + i + "_db", c.database);
                prefs.put("recent_" + i + "_user", c.username);
                prefs.putBoolean("recent_" + i + "_ssl", c.ssl);
            } else {
                prefs.remove("recent_" + i + "_type");
                prefs.remove("recent_" + i + "_host");
                prefs.remove("recent_" + i + "_port");
                prefs.remove("recent_" + i + "_db");
                prefs.remove("recent_" + i + "_user");
                prefs.remove("recent_" + i + "_ssl");
            }
        }
    }
}
