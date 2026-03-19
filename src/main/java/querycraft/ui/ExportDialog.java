package querycraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import querycraft.model.ExportOptions;

import java.io.File;

/**
 * Dialog for configuring CSV export options.
 */
public class ExportDialog extends Dialog<ExportConfig> {

    private File selectedFile;

    public ExportDialog(String defaultFilename) {
        setTitle("Export Options");
        setHeaderText("Configure Export Settings");
        initModality(Modality.APPLICATION_MODAL);

        // Create form fields
        ComboBox<ExportOptions.CsvFormat> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(ExportOptions.CsvFormat.values());
        formatCombo.setValue(ExportOptions.CsvFormat.STANDARD);

        ComboBox<ExportOptions.Encoding> encodingCombo = new ComboBox<>();
        encodingCombo.getItems().addAll(ExportOptions.Encoding.values());
        encodingCombo.setValue(ExportOptions.Encoding.UTF_8);

        ComboBox<ExportOptions.Delimiter> delimiterCombo = new ComboBox<>();
        delimiterCombo.getItems().addAll(ExportOptions.Delimiter.values());
        delimiterCombo.setValue(ExportOptions.Delimiter.COMMA);

        CheckBox headerCheck = new CheckBox("Include header row");
        headerCheck.setSelected(true);

        CheckBox quoteAllCheck = new CheckBox("Quote all values");
        quoteAllCheck.setSelected(false);

        TextField dateFormatField = new TextField("yyyy-MM-dd HH:mm:ss");

        // File selection
        TextField fileField = new TextField();
        fileField.setEditable(false);
        fileField.setPrefWidth(300);

        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV File");
            fileChooser.setInitialFileName(defaultFilename);
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );

            File file = fileChooser.showSaveDialog(getOwner());
            if (file != null) {
                selectedFile = file;
                fileField.setText(file.getAbsolutePath());
            }
        });

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Format:"), 0, 0);
        grid.add(formatCombo, 1, 0);
        grid.add(new Label("Encoding:"), 0, 1);
        grid.add(encodingCombo, 1, 1);
        grid.add(new Label("Delimiter:"), 0, 2);
        grid.add(delimiterCombo, 1, 2);
        grid.add(new Label("Date Format:"), 0, 3);
        grid.add(dateFormatField, 1, 3);
        grid.add(headerCheck, 1, 4);
        grid.add(quoteAllCheck, 1, 5);
        grid.add(new Label("File:"), 0, 6);
        grid.add(fileField, 1, 6);
        grid.add(browseButton, 2, 6);

        getDialogPane().setContent(grid);

        // Buttons
        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(exportButtonType, cancelButtonType);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType && selectedFile != null) {
                ExportOptions options = new ExportOptions();
                options.setFormat(formatCombo.getValue());
                options.setEncoding(encodingCombo.getValue());
                options.setDelimiter(delimiterCombo.getValue());
                options.setIncludeHeader(headerCheck.isSelected());
                options.setQuoteAllValues(quoteAllCheck.isSelected());
                options.setDateFormat(dateFormatField.getText());

                return new querycraft.ui.ExportConfig(selectedFile, options);
            }
            return null;
        });
    }
}
