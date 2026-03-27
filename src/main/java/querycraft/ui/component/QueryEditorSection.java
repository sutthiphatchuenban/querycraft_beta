package querycraft.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Component for the SQL editor and execution buttons.
 * Now supports Batch Mode with dual editors.
 */
public class QueryEditorSection extends VBox {

    private final SqlEditor queryEditor;
    private final SqlEditor deleteQueryEditor;
    private final Label deleteLabel;
    private final Button executeButton;
    private final Button processBatchButton;
    private final Button clearButton;
    private final Button formatButton;
    private final ToggleButton batchModeToggle;
    
    private QueryActionListener listener;

    public interface QueryActionListener {
        void onExecuteUnifiedRequested(String sql);
        void onBatchProcessRequested(String selectSql, String deleteSql);
        void onFormatRequested();
    }

    public QueryEditorSection() {
        super(10);
        this.setPadding(new Insets(10));
        this.getStyleClass().add("section-panel");

        // Mode Toggling
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("SQL Query Editor:");
        label.getStyleClass().add("section-label");
        
        batchModeToggle = new ToggleButton("Batch Action Mode");
        batchModeToggle.getStyleClass().add("button-neutral");
        batchModeToggle.setSelected(false);
        batchModeToggle.setOnAction(e -> toggleBatchMode(batchModeToggle.isSelected()));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(label, spacer, batchModeToggle);

        // Main Query Editor
        queryEditor = new SqlEditor();
        queryEditor.setPromptText("Enter your SQL query here...\nExample: SELECT * FROM users LIMIT 100");
        queryEditor.setPrefHeight(200);
        VBox.setVgrow(queryEditor, Priority.ALWAYS);

        // Delete Query Editor (Hidden by default)
        deleteLabel = new Label("Cleanup / Archive Query (DELETE):");
        deleteLabel.getStyleClass().add("section-label");
        deleteLabel.setVisible(false);
        deleteLabel.setManaged(false);

        deleteQueryEditor = new SqlEditor();
        deleteQueryEditor.setPromptText("Enter DELETE query for batch cleanup...\nExample: DELETE FROM users WHERE created_at < '2023-01-01'");
        deleteQueryEditor.setPrefHeight(150);
        deleteQueryEditor.setVisible(false);
        deleteQueryEditor.setManaged(false);

        // Button bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        executeButton = new Button("Execute Query");
        executeButton.getStyleClass().add("button-primary");
        executeButton.setOnAction(e -> {
            if (listener != null) listener.onExecuteUnifiedRequested(getSqlText());
        });
        executeButton.setDisable(true);

        processBatchButton = new Button("Process Batch (SELECT + EXPORT + DELETE)");
        processBatchButton.getStyleClass().add("button-success");
        processBatchButton.setVisible(false);
        processBatchButton.setManaged(false);
        processBatchButton.setOnAction(e -> {
            if (listener != null) listener.onBatchProcessRequested(getSqlText(), getDeleteSqlText());
        });
        processBatchButton.setDisable(true);

        clearButton = new Button("Clear All");
        clearButton.getStyleClass().add("button-neutral");
        clearButton.setOnAction(e -> {
            queryEditor.clear();
            deleteQueryEditor.clear();
        });

        formatButton = new Button("Format SQL");
        formatButton.getStyleClass().add("button-neutral");
        formatButton.setOnAction(e -> {
            if (listener != null) listener.onFormatRequested();
        });

        buttonBar.getChildren().addAll(executeButton, processBatchButton, clearButton, formatButton);
        
        this.getChildren().addAll(header, queryEditor, deleteLabel, deleteQueryEditor, buttonBar);
    }

    private void toggleBatchMode(boolean isBatch) {
        deleteLabel.setVisible(isBatch);
        deleteLabel.setManaged(isBatch);
        deleteQueryEditor.setVisible(isBatch);
        deleteQueryEditor.setManaged(isBatch);
        
        processBatchButton.setVisible(isBatch);
        processBatchButton.setManaged(isBatch);
        
        executeButton.setVisible(!isBatch);
        executeButton.setManaged(!isBatch);
        
        if (isBatch) {
            queryEditor.setPromptText("Enter SELECT query to fetch data for export/archive...");
            queryEditor.setPrefHeight(150);
        } else {
            queryEditor.setPromptText("Enter your SQL query here...\nExample: SELECT * FROM users LIMIT 100");
            queryEditor.setPrefHeight(200);
        }
    }

    public void setListener(QueryActionListener listener) {
        this.listener = listener;
    }

    public String getSqlText() {
        return queryEditor.getText().trim();
    }
    
    public String getDeleteSqlText() {
        return deleteQueryEditor.getText().trim();
    }

    public void setSqlText(String sql) {
        queryEditor.replaceText(sql);
    }

    public void setButtonsEnabled(boolean enabled) {
        executeButton.setDisable(!enabled);
        processBatchButton.setDisable(!enabled);
    }

    public SqlEditor getEditor() {
        return queryEditor;
    }
    
    public SqlEditor getDeleteEditor() {
        return deleteQueryEditor;
    }
    
    public boolean isBatchMode() {
        return batchModeToggle.isSelected();
    }
}
