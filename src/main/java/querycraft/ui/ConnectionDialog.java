package querycraft.ui;

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
import querycraft.service.DatabaseConnectionService;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Dialog for database connection configuration.
 */
public class ConnectionDialog extends Dialog<ConnectionInfo> {

    private final DatabaseConnectionService connectionService;
    private final Preferences prefs = Preferences.userNodeForPackage(ConnectionDialog.class);
    private static final int MAX_RECENT = 3;

    // CSV folder specific fields
    private File selectedCsvFolder;
    private TextField csvFolderField;

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

        ToggleButton showPasswordBtn = new ToggleButton("\uD83D\uDC41");
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
                
                if (selectedType == DatabaseType.CSV) {
                    return createCsvConnectionInfo();
                } else if (selectedType == DatabaseType.MSSQL && (windowsAuthCheck.isSelected() || namedPipesCheck.isSelected())) {
                    return createMssqlConnectionInfo(hostField, portField, databaseField, usernameField, passwordField, sslCheck, windowsAuthCheck, namedPipesCheck, instanceField);
                } else {
                    return createDatabaseConnectionInfo(typeCombo, hostField, portField,
                        databaseField, usernameField, passwordField, sslCheck, rememberCheck, recentList);
                }
            }
            return null;
        });
    }

    private ConnectionInfo createDatabaseConnectionInfo(ComboBox<DatabaseType> typeCombo,
            TextField hostField, TextField portField, TextField databaseField,
            TextField usernameField, PasswordField passwordField, CheckBox sslCheck,
            CheckBox rememberCheck, java.util.List<RecentConnection> recentList) {
        try {
            DatabaseType type = typeCombo.getValue();
            ConnectionInfo info;
            
            // Use specific connection info class based on database type
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

    private CsvConnectionInfo createCsvConnectionInfo() {
        if (selectedCsvFolder == null) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please select a folder containing CSV files.");
            return null;
        }

        CsvConnectionInfo info = new CsvConnectionInfo(selectedCsvFolder.getAbsolutePath());
        
        // Check if any CSV files found
        if (info.getCsvFileCount() == 0) {
            showAlert(Alert.AlertType.ERROR, "No CSV Files", 
                "No CSV files found in the selected folder.\nPlease choose a folder that contains .csv files.");
            return null;
        }
        
        return info;
    }

    private MssqlConnectionInfo createMssqlConnectionInfo(TextField hostField,
            TextField portField, TextField databaseField,
            TextField usernameField, PasswordField passwordField,
            CheckBox sslCheck, CheckBox windowsAuthCheck,
            CheckBox namedPipesCheck, TextField instanceField) {
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
                        windowsAuthCheck.isSelected()
                );
            }
            info.setUseSSL(sslCheck.isSelected());
            if (!windowsAuthCheck.isSelected()) {
                info.setUsername(usernameField.getText());
                info.setPassword(passwordField.getText());
            }
            return info;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Port must be a valid number.");
            return null;
        }
    }

    private void testCsvConnection() {
        if (selectedCsvFolder == null) {
            showAlert(Alert.AlertType.ERROR, "Test Failed", "Please select a folder first.");
            return;
        }
        
        if (!selectedCsvFolder.exists() || !selectedCsvFolder.isDirectory()) {
            showAlert(Alert.AlertType.ERROR, "Test Failed", "Invalid folder: " + selectedCsvFolder.getAbsolutePath());
            return;
        }
        
        // Count CSV files
        File[] csvFiles = selectedCsvFolder.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".csv")
        );
        int count = csvFiles != null ? csvFiles.length : 0;
        
        if (count == 0) {
            showAlert(Alert.AlertType.WARNING, "No CSV Files", 
                "No .csv files found in the selected folder.");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Folder OK", 
                "Found " + count + " CSV file(s) in:\n" + selectedCsvFolder.getName() + 
                "\n\nFiles will be loaded as tables on connect.");
        }
    }

    private void testConnection(ConnectionInfo info) {
        try {
            boolean success = connectionService.testConnection(info);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Connection Successful",
                        "Successfully connected to " + info.getDatabaseType().getDisplayName());
            }
        } catch (querycraft.exception.QueryCraftException e) {
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

        saveAllRecentConnections(list);
    }

    private void saveAllRecentConnections(java.util.List<RecentConnection> list) {
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
