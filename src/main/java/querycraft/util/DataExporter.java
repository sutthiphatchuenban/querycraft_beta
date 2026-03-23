package querycraft.util;

import querycraft.model.QueryResult;
import java.io.File;
import java.io.IOException;

/**
 * Strategy interface for exporting data.
 */
public interface DataExporter {
    /**
     * Export query result to a file.
     * @param result The data to export
     * @param file The target file
     * @throws IOException If export fails
     */
    void export(QueryResult result, File file) throws IOException;
    
    /**
     * Get the default file extension for this exporter.
     */
    String getFileExtension();
    
    /**
     * Get a display name for this exporter.
     */
    String getDisplayName();
}
