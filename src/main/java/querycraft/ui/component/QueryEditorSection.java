package querycraft.ui.component;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import querycraft.ui.SqlEditor;

/**
 * Component for the SQL editor and execution buttons.
 */
public class QueryEditorSection extends VBox {

    private final SqlEditor queryEditor;
    private final Button executeSelectButton;
    private final Button executeDeleteButton;
    private final Button clearButton;
    private final Button formatButton;
    private QueryActionListener listener;

    public interface QueryActionListener {
        void onExecuteRequested(boolean isDelete);
        void onFormatRequested();
    }

    public QueryEditorSection() {
        super(5);
        this.setPadding(new Insets(10));

        Label label = new Label("SQL Query:");
        
        queryEditor = new SqlEditor();
        queryEditor.setPromptText("Enter your SQL query here...\nExample: SELECT * FROM users LIMIT 100");
        queryEditor.setPrefHeight(200);
        VBox.setVgrow(queryEditor, Priority.ALWAYS);

        // Button bar
        HBox buttonBar = new HBox(10);

        executeSelectButton = new Button("Execute SELECT");
        executeSelectButton.getStyleClass().add("button-success");
        executeSelectButton.setOnAction(e -> {
            if (listener != null) listener.onExecuteRequested(false);
        });
        executeSelectButton.setDisable(true);

        executeDeleteButton = new Button("Execute DELETE");
        executeDeleteButton.getStyleClass().add("button-danger");
        executeDeleteButton.setOnAction(e -> {
            if (listener != null) listener.onExecuteRequested(true);
        });
        executeDeleteButton.setDisable(true);

        clearButton = new Button("Clear");
        clearButton.getStyleClass().add("button-neutral");
        clearButton.setOnAction(e -> queryEditor.clear());

        formatButton = new Button("Format SQL");
        formatButton.getStyleClass().add("button-neutral");
        formatButton.setOnAction(e -> {
            if (listener != null) listener.onFormatRequested();
        });

        buttonBar.getChildren().addAll(executeSelectButton, executeDeleteButton, clearButton, formatButton);
        this.getChildren().addAll(label, queryEditor, buttonBar);
    }

    public void setListener(QueryActionListener listener) {
        this.listener = listener;
    }

    public String getSqlText() {
        return queryEditor.getText().trim();
    }

    public void setSqlText(String sql) {
        queryEditor.replaceText(sql);
    }

    public void setButtonsEnabled(boolean enabled) {
        executeSelectButton.setDisable(!enabled);
        executeDeleteButton.setDisable(!enabled);
    }

    public SqlEditor getEditor() {
        return queryEditor;
    }
}
