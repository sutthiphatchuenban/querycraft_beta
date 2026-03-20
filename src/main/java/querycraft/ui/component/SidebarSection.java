package querycraft.ui.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Component for the sidebar that displays tables and query history.
 */
public class SidebarSection extends VBox {

    private final ListView<String> tableListView;
    private final ListView<String> historyListView;
    private final ObservableList<String> historyData = FXCollections.observableArrayList();
    private SidebarListener listener;

    public interface SidebarListener {
        void onTableDoubleClicked(String tableName);
        void onHistoryItemDoubleClicked(String query);
        void onDescribeTableRequested(String tableName);
        void onRefreshTablesRequested();
    }

    public SidebarSection() {
        super(10);
        this.getStyleClass().add("sidebar");
        this.setPadding(new Insets(10));
        this.setMinWidth(200);
        this.setPrefWidth(220);

        // Tables Section
        Label tablesLabel = new Label("Tables");
        tablesLabel.getStyleClass().add("sidebar-header");
        
        tableListView = new ListView<>();
        tableListView.getStyleClass().add("sidebar-list");
        VBox.setVgrow(tableListView, Priority.ALWAYS);
        
        tableListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listener != null) {
                String selected = tableListView.getSelectionModel().getSelectedItem();
                if (selected != null) listener.onTableDoubleClicked(selected);
            }
        });

        setupTableContextMenu();

        Button refreshBtn = new Button("Refresh Tables");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> {
            if (listener != null) listener.onRefreshTablesRequested();
        });

        // History Section
        Label historyLabel = new Label("Recent Queries");
        historyLabel.getStyleClass().add("sidebar-header");
        historyLabel.setPadding(new Insets(20, 0, 10, 0));
        
        historyListView = new ListView<>(historyData);
        historyListView.getStyleClass().add("sidebar-list");
        historyListView.setPrefHeight(300);
        VBox.setVgrow(historyListView, Priority.ALWAYS);
        
        historyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listener != null) {
                String selected = historyListView.getSelectionModel().getSelectedItem();
                if (selected != null) listener.onHistoryItemDoubleClicked(selected);
            }
        });

        this.getChildren().addAll(tablesLabel, tableListView, refreshBtn, historyLabel, historyListView);
    }

    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem describeItem = new MenuItem("Describe Structure");
        describeItem.setOnAction(e -> {
            String selected = tableListView.getSelectionModel().getSelectedItem();
            if (selected != null && listener != null) listener.onDescribeTableRequested(selected);
        });
        
        MenuItem selectItem = new MenuItem("SELECT * (Top 100)");
        selectItem.setOnAction(e -> {
            String selected = tableListView.getSelectionModel().getSelectedItem();
            if (selected != null && listener != null) listener.onTableDoubleClicked(selected);
        });

        contextMenu.getItems().addAll(describeItem, selectItem);
        tableListView.setContextMenu(contextMenu);
    }

    public void setListener(SidebarListener listener) {
        this.listener = listener;
    }

    public void setTables(ObservableList<String> tables) {
        tableListView.setItems(tables);
    }

    public void addToHistory(String sql) {
        historyData.remove(sql);
        historyData.add(0, sql);
        if (historyData.size() > 50) historyData.remove(50);
    }

    public ObservableList<String> getHistory() {
        return historyData;
    }
}
