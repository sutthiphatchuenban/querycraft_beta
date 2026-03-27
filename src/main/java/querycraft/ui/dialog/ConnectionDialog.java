package querycraft.ui.dialog;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import querycraft.model.ConnectionInfo;
import querycraft.model.CsvConnectionInfo;
import querycraft.model.DatabaseType;
import querycraft.model.MssqlConnectionInfo;
import querycraft.connection.DatabaseConnectionService;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Dialog for database connection configuration.
 */
public class ConnectionDialog extends Dialog<ConnectionInfo> {

    private final DatabaseConnectionService connectionService;
    private final Preferences prefs = Preferences.userNodeForPackage(ConnectionDialog.class);
    private static final int MAX_RECENT = 20;

    // CSV folder specific fields
    private File selectedCsvFolder;
    private TextField csvFolderField;

    private static class RecentConnection {
        String type, host, port, database, username, password, csvPath, instanceName;
        boolean ssl, useWindowsAuth, useNamedPipes;

        RecentConnection(String type, String host, String port, String database, String username, String password, boolean ssl, 
                         boolean useWindowsAuth, boolean useNamedPipes, String instanceName, String csvPath) {
            this.type = type;
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
            this.ssl = ssl;
            this.useWindowsAuth = useWindowsAuth;
            this.useNamedPipes = useNamedPipes;
            this.instanceName = instanceName;
            this.csvPath = csvPath;
        }

        @Override
        public String toString() {
            if ("CSV".equals(type)) {
                File f = new File(csvPath != null ? csvPath : "");
                return "CSV: " + f.getName() + " (" + csvPath + ")";
            }
            String info = database + " @ " + host;
            if ("MSSQL".equals(type)) {
                if (useNamedPipes) info += " (NP: " + instanceName + ")";
                if (useWindowsAuth) info += " [WinAuth]";
            }
            return type + ": " + info;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RecentConnection)) return false;
            RecentConnection that = (RecentConnection) o;
            return type.equals(that.type) && 
                   java.util.Objects.equals(host, that.host) && 
                   java.util.Objects.equals(port, that.port) && 
                   java.util.Objects.equals(database, that.database) && 
                   java.util.Objects.equals(username, that.username) &&
                   java.util.Objects.equals(csvPath, that.csvPath);
        }

        // Simple obfuscation for password saving (Base64)
        static String obfuscate(String s) {
            if (s == null || s.isEmpty()) return "";
            return java.util.Base64.getEncoder().encodeToString(s.getBytes());
        }
        
        static String deobfuscate(String s) {
            if (s == null || s.isEmpty()) return "";
            try {
                return new String(java.util.Base64.getDecoder().decode(s));
            } catch (Exception e) { return ""; }
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
        recentCombo.setPrefWidth(225);
        recentCombo.setPrefHeight(30);

        Button deleteRecentButton = new Button("Delete");
        deleteRecentButton.getStyleClass().add("button-danger");
        deleteRecentButton.setPrefWidth(70);
        deleteRecentButton.setPrefHeight(30);
        deleteRecentButton.setOnAction(e -> {
            RecentConnection selected = recentCombo.getValue();
            if (selected != null) {
                recentList.remove(selected);
                recentCombo.getItems().remove(selected);
                recentCombo.setValue(null);
                saveAllRecentConnections(recentList);
            }
        });

        HBox recentBox = new HBox(5, recentCombo, deleteRecentButton);

        ComboBox<DatabaseType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(DatabaseType.values());
        typeCombo.setValue(DatabaseType.MYSQL);
        typeCombo.setPrefWidth(300);
        typeCombo.setPrefHeight(30);

        // Database connection fields
        TextField hostField = new TextField("localhost");
        hostField.setPrefWidth(300);
        hostField.setPrefHeight(30);
        TextField portField = new TextField(String.valueOf(DatabaseType.MYSQL.getDefaultPort()));
        portField.setPrefWidth(300);
        portField.setPrefHeight(30);
        TextField databaseField = new TextField();
        databaseField.setPrefWidth(300);
        databaseField.setPrefHeight(30);
        TextField usernameField = new TextField();
        usernameField.setPrefWidth(300);
        usernameField.setPrefHeight(30);
        PasswordField passwordField = new PasswordField();
        passwordField.setPrefWidth(255);
        passwordField.setPrefHeight(30);

        TextField passwordTextField = new TextField();
        passwordTextField.setPrefWidth(255);
        passwordTextField.setPrefHeight(30);
        passwordTextField.setManaged(false);
        passwordTextField.setVisible(false);
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        ToggleButton showPasswordBtn = new ToggleButton("👁");
        showPasswordBtn.setPrefWidth(40);
        showPasswordBtn.setPrefHeight(30);
        showPasswordBtn.setOnAction(e -> {
            if (showPasswordBtn.isSelected()) {
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
            } else {
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
            }
        });

        HBox passBox = new HBox(5, new StackPane(passwordField, passwordTextField), showPasswordBtn);
        passBox.setPrefWidth(300);
        CheckBox sslCheck = new CheckBox("Use SSL (Required for Neon/Cloud)");
        CheckBox rememberCheck = new CheckBox("Remember Connection");
        rememberCheck.setSelected(true);

        // Windows Authentication checkbox (for SQL Server only)
        CheckBox windowsAuthCheck = new CheckBox("Use Windows Authentication");
        windowsAuthCheck.setVisible(false);
        windowsAuthCheck.setManaged(false);

        // Named Pipes checkbox (for SQL Server only)
        CheckBox namedPipesCheck = new CheckBox("Use Named Pipes (no TCP/IP required)");
        namedPipesCheck.setVisible(false);
        namedPipesCheck.setManaged(false);

        // Instance Name field (for Named Pipes)
        Label instanceLabel = new Label("Instance:");
        instanceLabel.setVisible(false);
        instanceLabel.setManaged(false);
        TextField instanceField = new TextField("MSSQLSERVER");
        instanceField.setPrefWidth(300);
        instanceField.setPrefHeight(30);
        instanceField.setPromptText("e.g. MSSQLSERVER, SQLEXPRESS");
        instanceField.setVisible(false);
        instanceField.setManaged(false);

        // Toggle username/password visibility based on Windows Auth
        windowsAuthCheck.setOnAction(e -> {
            boolean useWinAuth = windowsAuthCheck.isSelected();
            usernameField.setDisable(useWinAuth);
            passwordField.setDisable(useWinAuth);
            passwordTextField.setDisable(useWinAuth);
            if (useWinAuth) {
                usernameField.clear();
                passwordField.clear();
            }
        });

        // Toggle Named Pipes mode
        namedPipesCheck.setOnAction(e -> {
            boolean usePipes = namedPipesCheck.isSelected();
            portField.setDisable(usePipes);
            instanceLabel.setVisible(usePipes);
            instanceLabel.setManaged(usePipes);
            instanceField.setVisible(usePipes);
            instanceField.setManaged(usePipes);
            if (usePipes) {
                portField.setText("0");
            } else {
                portField.setText(String.valueOf(DatabaseType.MSSQL.getDefaultPort()));
            }
        });

        // CSV folder specific fields
        csvFolderField = new TextField();
        csvFolderField.setPrefWidth(250);
        csvFolderField.setEditable(false);
        csvFolderField.setPromptText("Select a folder with CSV files...");
        
        Button browseCsvButton = new Button("Browse...");
        browseCsvButton.setPrefWidth(80);
        browseCsvButton.setOnAction(e -> {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("Select Folder with CSV Files");
            File folder = dirChooser.showDialog(getOwner());
            if (folder != null) {
                selectedCsvFolder = folder;
                csvFolderField.setText(folder.getAbsolutePath());
            }
        });
        
        HBox csvFolderBox = new HBox(5, csvFolderField, browseCsvButton);
        
        Label csvHintLabel = new Label("All .csv files in folder will be loaded as tables (auto-detect charset & delimiter)");
        csvHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        // Container for database fields
        GridPane dbGrid = new GridPane();
        dbGrid.setHgap(10);
        dbGrid.setVgap(10);
        dbGrid.setPadding(new Insets(0));
        
        dbGrid.add(new Label("Host:"), 0, 0);
        dbGrid.add(hostField, 1, 0);
        dbGrid.add(new Label("Port:"), 0, 1);
        dbGrid.add(portField, 1, 1);
        dbGrid.add(new Label("Database:"), 0, 2);
        dbGrid.add(databaseField, 1, 2);
        dbGrid.add(new Label("Username:"), 0, 3);
        dbGrid.add(usernameField, 1, 3);
        dbGrid.add(new Label("Password:"), 0, 4);
        dbGrid.add(passBox, 1, 4);
        dbGrid.add(windowsAuthCheck, 1, 5);
        dbGrid.add(namedPipesCheck, 1, 6);
        dbGrid.add(instanceLabel, 0, 7);
        dbGrid.add(instanceField, 1, 7);
        dbGrid.add(sslCheck, 1, 8);

        // Container for CSV fields
        GridPane csvGrid = new GridPane();
        csvGrid.setHgap(10);
        csvGrid.setVgap(10);
        csvGrid.setPadding(new Insets(0));
        
        csvGrid.add(new Label("CSV Folder:"), 0, 0);
        csvGrid.add(csvFolderBox, 1, 0);
        VBox csvHintBox = new VBox(csvHintLabel);
        csvHintBox.setPadding(new Insets(5, 0, 0, 0));
        csvGrid.add(csvHintBox, 0, 1, 2, 1);

        // StackPane to switch between modes
        StackPane contentStack = new StackPane();
        contentStack.getChildren().addAll(dbGrid, csvGrid);
        csvGrid.setVisible(false);
        csvGrid.setManaged(false);

        // Handle recent selection
        recentCombo.setOnAction(e -> {
            RecentConnection selected = recentCombo.getValue();
            if (selected != null) {
                try {
                    DatabaseType type = DatabaseType.valueOf(selected.type);
                    typeCombo.setValue(type);
                    
                    // Force UI update to show correct Grid (CSV vs DB)
                    typeCombo.getOnAction().handle(new ActionEvent());

                    if (type == DatabaseType.CSV) {
                        if (selected.csvPath != null) {
                            selectedCsvFolder = new File(selected.csvPath);
                            csvFolderField.setText(selected.csvPath);
                        }
                    } else {
                        hostField.setText(selected.host);
                        portField.setText(selected.port);
                        databaseField.setText(selected.database);
                        usernameField.setText(selected.username);
                        passwordField.setText(RecentConnection.deobfuscate(selected.password));
                        sslCheck.setSelected(selected.ssl);
                        
                        if (type == DatabaseType.MSSQL) {
                            windowsAuthCheck.setSelected(selected.useWindowsAuth);
                            namedPipesCheck.setSelected(selected.useNamedPipes);
                            instanceField.setText(selected.instanceName);
                            
                            // Trigger action handlers to update UI state
                            windowsAuthCheck.getOnAction().handle(new ActionEvent());
                            namedPipesCheck.getOnAction().handle(new ActionEvent());
                        }
                    }
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

        // Update UI when database type changes
        typeCombo.setOnAction(e -> {
            DatabaseType selected = typeCombo.getValue();
            if (selected != null) {
                boolean isCsv = selected == DatabaseType.CSV;
                boolean isMssql = selected == DatabaseType.MSSQL;
                
                // Switch between CSV and Database modes
                dbGrid.setVisible(!isCsv);
                dbGrid.setManaged(!isCsv);
                csvGrid.setVisible(isCsv);
                csvGrid.setManaged(isCsv);
                
                // Show Windows Auth and Named Pipes checkboxes only for MSSQL
                windowsAuthCheck.setVisible(isMssql);
                windowsAuthCheck.setManaged(isMssql);
                namedPipesCheck.setVisible(isMssql);
                namedPipesCheck.setManaged(isMssql);

                // Reset Named Pipes UI when switching away from MSSQL
                if (!isMssql) {
                    namedPipesCheck.setSelected(false);
                    instanceLabel.setVisible(false);
                    instanceLabel.setManaged(false);
                    instanceField.setVisible(false);
                    instanceField.setManaged(false);
                    portField.setDisable(false);
                }
                
                if (!isCsv) {
                    portField.setText(String.valueOf(selected.getDefaultPort()));
                }
            }
        });

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Recent:"), 0, 0);
        grid.add(recentBox, 1, 0);
        grid.add(new Label("Database Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        
        // Add the stack pane for mode switching
        GridPane.setColumnSpan(contentStack, 2);
        grid.add(contentStack, 0, 2);
        
        grid.add(rememberCheck, 1, 3);

        getDialogPane().setContent(grid);

        // Buttons
        ButtonType testButtonType = new ButtonType("Test Connection", ButtonBar.ButtonData.APPLY);
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(testButtonType, connectButtonType, cancelButtonType);

        // Test connection button
        Button testButton = (Button) getDialogPane().lookupButton(testButtonType);
        testButton.addEventFilter(ActionEvent.ACTION, event -> {
            DatabaseType selectedType = typeCombo.getValue();
            if (selectedType == DatabaseType.CSV) {
                testCsvConnection();
            } else if (selectedType == DatabaseType.MSSQL && (windowsAuthCheck.isSelected() || namedPipesCheck.isSelected())) {
                // Test MSSQL with Windows Auth or Named Pipes
                try {
                    MssqlConnectionInfo info;
                    if (namedPipesCheck.isSelected()) {
                        info = new MssqlConnectionInfo(
                                hostField.getText(),
                                databaseField.getText(),
                                instanceField.getText(),
                                windowsAuthCheck.isSelected(),
                                true // useNamedPipes
                        );
                    } else {
                        info = new MssqlConnectionInfo(
                                hostField.getText(),
                                Integer.parseInt(portField.getText()),
                                databaseField.getText(),
                                true // useWindowsAuth
                        );
                    }
                    info.setUseSSL(sslCheck.isSelected());
                    if (!windowsAuthCheck.isSelected()) {
                        info.setUsername(usernameField.getText());
                        info.setPassword(passwordField.getText());
                    }
                    testConnection(info);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Input Error", "Port must be a valid number.");
                }
            } else {
                // Test MySQL or PostgreSQL with specific connection info
                try {
                    DatabaseType type = typeCombo.getValue();
                    ConnectionInfo info;
                    
                    switch (type) {
                        case MYSQL:
                            info = new querycraft.model.MySqlConnectionInfo(
                                    hostField.getText(),
                                    Integer.parseInt(portField.getText()),
                                    databaseField.getText(),
                                    usernameField.getText(),
                                    passwordField.getText(),
                                    sslCheck.isSelected()
                            );
                            break;
                        case POSTGRESQL:
                            info = new querycraft.model.PostgreSqlConnectionInfo(
                                    hostField.getText(),
                                    Integer.parseInt(portField.getText()),
                                    databaseField.getText(),
                                    usernameField.getText(),
                                    passwordField.getText(),
                                    sslCheck.isSelected()
                            );
                            break;
                        default:
                            info = new ConnectionInfo(
                                    type,
                                    hostField.getText(),
                                    Integer.parseInt(portField.getText()),
                                    databaseField.getText(),
                                    usernameField.getText(),
                                    passwordField.getText(),
                                    sslCheck.isSelected()
                            );
                    }
                    testConnection(info);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Input Error", "Port must be a valid number.");
                }
            }
            event.consume();
        });

        // Connect connection button
        Button connectButton = (Button) getDialogPane().lookupButton(connectButtonType);
        connectButton.addEventFilter(ActionEvent.ACTION, event -> {
            DatabaseType selectedType = typeCombo.getValue();
            if (selectedType == DatabaseType.CSV) {
                if (selectedCsvFolder == null) {
                    showAlert(Alert.AlertType.ERROR, "Input Error", "Please select a folder containing CSV files.");
                    event.consume();
                    return;
                }
                CsvConnectionInfo temp = new CsvConnectionInfo(selectedCsvFolder.getAbsolutePath());
                if (temp.getCsvFileCount() == 0) {
                    showAlert(Alert.AlertType.ERROR, "No CSV Files", "No CSV files found in the selected folder.\nPlease choose a folder that contains .csv files.");
                    event.consume();
                }
            } else {
                try {
                    Integer.parseInt(portField.getText());
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Input Error", "Port must be a valid number.");
                    event.consume();
                }
            }
        });

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                DatabaseType selectedType = typeCombo.getValue();
                ConnectionInfo result;

                if (selectedType == DatabaseType.CSV) {
                    result = createCsvConnectionInfo();
                } else if (selectedType == DatabaseType.MSSQL && (windowsAuthCheck.isSelected() || namedPipesCheck.isSelected())) {
                    result = createMssqlConnectionInfo(hostField, portField, databaseField, usernameField, passwordField, sslCheck, windowsAuthCheck, namedPipesCheck, instanceField);
                } else {
                    result = createDatabaseConnectionInfo(typeCombo, hostField, portField,
                        databaseField, usernameField, passwordField, sslCheck, rememberCheck, recentList);
                }

                // Global logic to save to recent for ALL types
                if (rememberCheck.isSelected() && result != null) {
                    RecentConnection recent = new RecentConnection(
                        selectedType.name(),
                        hostField.getText(),
                        portField.getText(),
                        databaseField.getText(),
                        usernameField.getText(),
                        RecentConnection.obfuscate(passwordField.getText()),
                        sslCheck.isSelected(),
                        windowsAuthCheck.isSelected(),
                        namedPipesCheck.isSelected(),
                        instanceField.getText(),
                        selectedCsvFolder != null ? selectedCsvFolder.getAbsolutePath() : null
                    );
                    
                    recentList.remove(recent);
                    recentList.add(0, recent);
                    while (recentList.size() > MAX_RECENT) {
                        recentList.remove(MAX_RECENT);
                    }
                    saveAllRecentConnections(recentList);
                }
                
                return result;
            }
            return null;
        });

        getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        getDialogPane().getStyleClass().add("dialog-pane");
    }

    private ConnectionInfo createCsvConnectionInfo() {
        return new CsvConnectionInfo(selectedCsvFolder.getAbsolutePath());
    }

    private MssqlConnectionInfo createMssqlConnectionInfo(TextField hostField, TextField portField, TextField databaseField, TextField usernameField, PasswordField passwordField, CheckBox sslCheck, CheckBox windowsAuthCheck, CheckBox namedPipesCheck, TextField instanceField) {
        MssqlConnectionInfo info;
        if (namedPipesCheck.isSelected()) {
            info = new MssqlConnectionInfo(
                hostField.getText(),
                databaseField.getText(),
                instanceField.getText(),
                windowsAuthCheck.isSelected(),
                true // useNamedPipes
            );
        } else {
            info = new MssqlConnectionInfo(
                hostField.getText(),
                Integer.parseInt(portField.getText()),
                databaseField.getText(),
                windowsAuthCheck.isSelected()
            );
        }
        
        if (!windowsAuthCheck.isSelected()) {
            info.setUsername(usernameField.getText());
            info.setPassword(passwordField.getText());
        }
        info.setUseSSL(sslCheck.isSelected());
        return info;
    }

    private ConnectionInfo createDatabaseConnectionInfo(ComboBox<DatabaseType> typeCombo, TextField hostField, TextField portField, 
            TextField databaseField, TextField usernameField, PasswordField passwordField, 
            CheckBox sslCheck, CheckBox rememberCheck, java.util.List<RecentConnection> recentList) {
        
        DatabaseType type = typeCombo.getValue();
        ConnectionInfo info;
        
        switch (type) {
            case MYSQL:
                info = new querycraft.model.MySqlConnectionInfo(
                        hostField.getText(),
                        Integer.parseInt(portField.getText()),
                        databaseField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        sslCheck.isSelected()
                );
                break;
            case POSTGRESQL:
                info = new querycraft.model.PostgreSqlConnectionInfo(
                        hostField.getText(),
                        Integer.parseInt(portField.getText()),
                        databaseField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        sslCheck.isSelected()
                );
                break;
            default:
                info = new ConnectionInfo(
                        type,
                        hostField.getText(),
                        Integer.parseInt(portField.getText()),
                        databaseField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        sslCheck.isSelected()
                );
        }
        return info;
    }

    private void testCsvConnection() {
        if (selectedCsvFolder == null) {
            showAlert(Alert.AlertType.ERROR, "Selection Required", "Please select a folder first.");
            return;
        }

        setStatusMessage("Testing CSV connection...");
        new Thread(() -> {
            try {
                CsvConnectionInfo info = new CsvConnectionInfo(selectedCsvFolder.getAbsolutePath());
                if (connectionService.testConnection(info)) {
                    javafx.application.Platform.runLater(() -> 
                        showAlert(Alert.AlertType.INFORMATION, "Connection Test", 
                            "Success! Found " + info.getCsvFileCount() + " CSV file(s) in the folder."));
                } else {
                    javafx.application.Platform.runLater(() -> 
                        showAlert(Alert.AlertType.ERROR, "Connection Test", "Failed to validate CSV folder."));
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Connection Test", "Error: " + e.getMessage()));
            } finally {
                javafx.application.Platform.runLater(() -> setStatusMessage("Ready"));
            }
        }).start();
    }

    private void testConnection(ConnectionInfo info) {
        setStatusMessage("Testing connection...");
        new Thread(() -> {
            try {
                if (connectionService.testConnection(info)) {
                    javafx.application.Platform.runLater(() -> 
                        showAlert(Alert.AlertType.INFORMATION, "Connection Test", "Success! Connection established."));
                } else {
                    javafx.application.Platform.runLater(() -> 
                        showAlert(Alert.AlertType.ERROR, "Connection Test", "Failed to establish connection."));
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Connection Test", "Error: " + e.getMessage()));
            } finally {
                javafx.application.Platform.runLater(() -> setStatusMessage("Ready"));
            }
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(getOwner());
        alert.showAndWait();
    }

    private void setStatusMessage(String message) {
        setHeaderText(message);
    }

    private java.util.List<RecentConnection> loadRecentConnections() {
        java.util.List<RecentConnection> list = new java.util.ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String type = prefs.get("recent_type_" + i, null);
            if (type != null) {
                list.add(new RecentConnection(
                        type,
                        prefs.get("recent_host_" + i, ""),
                        prefs.get("recent_port_" + i, ""),
                        prefs.get("recent_db_" + i, ""),
                        prefs.get("recent_user_" + i, ""),
                        prefs.get("recent_pass_" + i, ""),
                        prefs.getBoolean("recent_ssl_" + i, false),
                        prefs.getBoolean("recent_winauth_" + i, false),
                        prefs.getBoolean("recent_pipes_" + i, false),
                        prefs.get("recent_instance_" + i, "MSSQLSERVER"),
                        prefs.get("recent_csvpath_" + i, null)
                ));
            }
        }
        return list;
    }

    private void saveAllRecentConnections(java.util.List<RecentConnection> list) {
        // Clear old ones first
        for (int i = 0; i < MAX_RECENT; i++) {
            prefs.remove("recent_type_" + i);
        }

        // Save new ones
        for (int i = 0; i < list.size(); i++) {
            RecentConnection conn = list.get(i);
            prefs.put("recent_type_" + i, conn.type);
            prefs.put("recent_host_" + i, conn.host != null ? conn.host : "");
            prefs.put("recent_port_" + i, conn.port != null ? conn.port : "");
            prefs.put("recent_db_" + i, conn.database != null ? conn.database : "");
            prefs.put("recent_user_" + i, conn.username != null ? conn.username : "");
            prefs.put("recent_pass_" + i, conn.password != null ? conn.password : "");
            prefs.putBoolean("recent_ssl_" + i, conn.ssl);
            prefs.putBoolean("recent_winauth_" + i, conn.useWindowsAuth);
            prefs.putBoolean("recent_pipes_" + i, conn.useNamedPipes);
            if (conn.instanceName != null) prefs.put("recent_instance_" + i, conn.instanceName);
            if (conn.csvPath != null) prefs.put("recent_csvpath_" + i, conn.csvPath);
        }
    }
}
