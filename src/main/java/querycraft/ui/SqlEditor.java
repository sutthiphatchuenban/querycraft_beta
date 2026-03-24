package querycraft.ui;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced SQL editor with syntax highlighting using RichTextFX.
 */
public class SqlEditor extends CodeArea {

    private static final String[] KEYWORDS = new String[] {
            "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON",
            "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "INSERT", "INTO", "VALUES",
            "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "DROP", "ALTER", "TRUNCATE",
            "INDEX", "VIEW", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT",
            "UNION", "ALL", "EXCEPT", "INTERSECT", "IN", "EXISTS", "BETWEEN", "LIKE",
            "IS", "NULL", "AND", "OR", "NOT", "AS", "DISTINCT", "DESC", "ASC", "WITH",
            "CASE", "WHEN", "THEN", "ELSE", "END"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String STRING_PATTERN = "'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "--.*|/\\*(.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";
    private static final String OPERATOR_PATTERN = "[<>!=+\\-*/%]|\\b(LIKE|IN|BETWEEN|IS|AND|OR|NOT)\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")",
            Pattern.CASE_INSENSITIVE
    );

    private final javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
    private final javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
    private java.util.List<String> tableNames = new java.util.ArrayList<>();

    public SqlEditor() {
        super();
        this.setParagraphGraphicFactory(LineNumberFactory.get(this));
        
        // Re-compute highlighting whenever text changes
        this.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> this.setStyleSpans(0, computeHighlighting(this.getText())));
        
        setupAutocomplete();
        this.getStyleClass().add("sql-editor");
    }

    private void setupAutocomplete() {
        suggestionList.setPrefWidth(200);
        suggestionList.setPrefHeight(150);
        suggestionPopup.getContent().add(suggestionList);

        this.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (newPos > 0) {
                showSuggestions();
            } else {
                suggestionPopup.hide();
            }
        });

        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                completeSelection();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                suggestionPopup.hide();
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                completeSelection();
            }
        });

        // Handle keys in editor to navigate suggestions
        this.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (suggestionPopup.isShowing()) {
                if (e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                    suggestionList.getSelectionModel().selectNext();
                    e.consume();
                } else if (e.getCode() == javafx.scene.input.KeyCode.UP) {
                    suggestionList.getSelectionModel().selectPrevious();
                    e.consume();
                } else if (e.getCode() == javafx.scene.input.KeyCode.ENTER || e.getCode() == javafx.scene.input.KeyCode.TAB) {
                    completeSelection();
                    e.consume();
                } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    suggestionPopup.hide();
                    e.consume();
                }
            }
        });

        // Add robust shortcut handling for Thai layout on Windows
        this.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown()) {
                // Check code name or text for fallback (handles Thai layout issues)
                String key = e.getCode().getName().toUpperCase();
                String text = e.getText().toUpperCase();
                
                if (key.equals("C") || text.equals("C")) {
                    this.copy();
                    e.consume();
                } else if (key.equals("V") || text.equals("V")) {
                    this.paste();
                    e.consume();
                } else if (key.equals("X") || text.equals("X")) {
                    this.cut();
                    e.consume();
                } else if (key.equals("A") || text.equals("A")) {
                    this.selectAll();
                    e.consume();
                } else if (key.equals("Z") || text.equals("Z")) {
                    this.undo();
                    e.consume();
                } else if (key.equals("Y") || text.equals("Y")) {
                    this.redo();
                    e.consume();
                }
            }
        });
    }

    private void showSuggestions() {
        String text = getText();
        int caretPos = getCaretPosition();
        if (caretPos <= 0) return;

        // Find current word
        int start = caretPos - 1;
        while (start >= 0 && Character.isLetterOrDigit(text.charAt(start))) {
            start--;
        }
        start++;
        String prefix = text.substring(start, caretPos).toUpperCase();

        if (prefix.length() < 1) {
            suggestionPopup.hide();
            return;
        }

        javafx.collections.ObservableList<String> matches = javafx.collections.FXCollections.observableArrayList();
        for (String kw : KEYWORDS) {
            if (kw.startsWith(prefix)) matches.add(kw);
        }
        for (String table : tableNames) {
            if (table.toUpperCase().startsWith(prefix)) matches.add(table);
        }

        if (matches.isEmpty()) {
            suggestionPopup.hide();
        } else {
            suggestionList.setItems(matches);
            suggestionList.getSelectionModel().select(0);
            
            // Position popup
            javafx.geometry.Bounds bounds = this.caretBoundsProperty().getValue().orElse(null);
            if (bounds != null) {
                suggestionPopup.show(this, bounds.getMinX(), bounds.getMaxY());
            }
        }
    }

    private void completeSelection() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String text = getText();
        int caretPos = getCaretPosition();
        int start = caretPos - 1;
        while (start >= 0 && Character.isLetterOrDigit(text.charAt(start))) {
            start--;
        }
        start++;

        this.replaceText(start, caretPos, selected);
        suggestionPopup.hide();
        this.requestFocus();
    }

    public void setTableNames(java.util.List<String> tables) {
        this.tableNames = tables;
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    matcher.group("NUMBER") != null ? "number" :
                    matcher.group("OPERATOR") != null ? "operator" :
                    null;
            assert styleClass != null;
            
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    public void setPromptText(String prompt) {
        setPlaceholder(new javafx.scene.control.Label(prompt));
    }
}
