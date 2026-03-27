package querycraft.export;

import querycraft.model.DatabaseType;
import querycraft.model.ExportOptions;

/**
 * Factory for creating data exporters.
 */
public class ExporterFactory {
    
    public enum ExportFormat {
        CSV, SQL
    }
    
    /**
     * Create a CSV exporter with specified options.
     */
    public static DataExporter createCsvExporter(ExportOptions options) {
        return new CsvExporter(options);
    }
    
    /**
     * Create a SQL exporter for a specific table.
     */
    public static DataExporter createSqlExporter(String tableName, DatabaseType dbType) {
        return new SqlInsertGenerator(tableName, dbType);
    }
    
    /**
     * Generate a default filename based on prefix and extension.
     */
    public static String generateDefaultFilename(String prefix, String ext) {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return prefix + "_" + timestamp + "." + ext;
    }
}
