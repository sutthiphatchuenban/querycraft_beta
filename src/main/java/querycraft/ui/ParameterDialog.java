package querycraft.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import querycraft.service.PreparedStatementService;

import java.util.*;

/**
 * Dialog for entering parameter values for prepared statements.
 */
public class ParameterDialog extends Dialog<Map<String, Object>> {

    private final PreparedStatementService preparedStatementService;
    private final String sql;
    private final List<TextField> paramFields = new ArrayList<>();
    private final List<ComboBox<String>> paramTypes = new ArrayList<>();
    private final List<String> paramNames;

    public ParameterDialog(String sql) {
        this.sql = sql;
        this.preparedStatementService = new PreparedStatementService();
        this.paramNames = preparedStatementService.extractNamedParameters(sql);

        setTitle("Query Parameters");
        setHeaderText("Enter values for query parameters");
        initModality(Modality.APPLICATION_MODAL);

        if (paramNames.isEmpty()) {
            // No parameters found
            setHeaderText("No parameters found in query");
        }

        createParameterInputs();

        // Buttons
        ButtonType executeButtonType = new ButtonType("Execute", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(executeButtonType, cancelButtonType);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == executeButtonType) {
                return collectParameterValues();
            }
            return null;
        });

        // Validate before allowing execute
        Button executeButton = (Button) getDialogPane().lookupButton(executeButtonType);
        executeButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateInputs()) {
                event.consume();
            }
        });
    }

    private void createParameterInputs() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Show SQL preview
        TextArea sqlPreview = new TextArea(sql);
        sqlPreview.setEditable(false);
        sqlPreview.setWrapText(true);
        sqlPreview.setPrefRowCount(3);
        sqlPreview.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");

        Label sqlLabel = new Label("SQL Query:");
        sqlLabel.setStyle("-fx-font-weight: bold;");

        content.getChildren().addAll(sqlLabel, sqlPreview);

        if (!paramNames.isEmpty()) {
            Label paramsLabel = new Label("Parameter Values:");
            paramsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
            content.getChildren().add(paramsLabel);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10, 0, 0, 0));

            ObservableList<String> typeOptions = FXCollections.observableArrayList(
                "String", "Integer", "Long", "Double", "Boolean", "Date (yyyy-MM-dd)", "DateTime (yyyy-MM-dd HH:mm:ss)"
            );

            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                
                Label nameLabel = new Label(":" + paramName);
                nameLabel.setStyle("-fx-font-family: 'Consolas', monospace;");
                
                TextField valueField = new TextField();
                valueField.setPromptText("Enter value...");
                valueField.setPrefWidth(200);
                
                ComboBox<String> typeCombo = new ComboBox<>(typeOptions);
                typeCombo.setValue("String");
                typeCombo.setPrefWidth(150);
                
                grid.add(nameLabel, 0, i);
                grid.add(valueField, 1, i);
                grid.add(typeCombo, 2, i);
                
                paramFields.add(valueField);
                paramTypes.add(typeCombo);
            }

            content.getChildren().add(grid);
        }

        // Add hint
        Label hintLabel = new Label("Tip: Use :paramName syntax in your SQL for named parameters");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-padding: 10 0 0 0;");
        content.getChildren().add(hintLabel);

        getDialogPane().setContent(content);
    }

    private Map<String, Object> collectParameterValues() {
        Map<String, Object> values = new HashMap<>();
        
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String valueStr = paramFields.get(i).getText();
            String type = paramTypes.get(i).getValue();
            
            Object value = convertValue(valueStr, type);
            values.put(paramName, value);
        }
        
        return values;
    }

    private Object convertValue(String valueStr, String type) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            switch (type) {
                case "Integer":
                    return Integer.parseInt(valueStr);
                case "Long":
                    return Long.parseLong(valueStr);
                case "Double":
                    return Double.parseDouble(valueStr);
                case "Boolean":
                    return Boolean.parseBoolean(valueStr);
                case "Date (yyyy-MM-dd)":
                    return java.sql.Date.valueOf(valueStr);
                case "DateTime (yyyy-MM-dd HH:mm:ss)":
                    return java.sql.Timestamp.valueOf(valueStr);
                case "String":
                default:
                    return valueStr;
            }
        } catch (Exception e) {
            // Return as string if conversion fails
            return valueStr;
        }
    }

    private boolean validateInputs() {
        for (int i = 0; i < paramNames.size(); i++) {
            String value = paramFields.get(i).getText();
            String type = paramTypes.get(i).getValue();
            
            if (value == null || value.trim().isEmpty()) {
                showAlert("Missing Value", "Please enter a value for parameter :" + paramNames.get(i));
                return false;
            }
            
            // Validate type conversion
            try {
                convertValue(value, type);
            } catch (Exception e) {
                showAlert("Invalid Value", "Cannot convert '" + value + "' to type " + type + 
                         " for parameter :" + paramNames.get(i));
                return false;
            }
        }
        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Check if the SQL contains any named parameters.
     */
    public boolean hasParameters() {
        return !paramNames.isEmpty();
    }
}
