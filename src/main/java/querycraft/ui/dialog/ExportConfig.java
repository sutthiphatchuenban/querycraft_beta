package querycraft.ui.dialog;

import querycraft.model.ExportOptions;

import java.io.File;

/**
 * Configuration class for export operations.
 */
public class ExportConfig {
    private final File file;
    private final ExportOptions options;

    public ExportConfig(File file, ExportOptions options) {
        this.file = file;
        this.options = options;
    }

    public File getFile() {
        return file;
    }

    public ExportOptions getOptions() {
        return options;
    }
}
