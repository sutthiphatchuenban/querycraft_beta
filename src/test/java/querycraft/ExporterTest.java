package querycraft;

import static org.junit.Assert.*;
import org.junit.Test;
import querycraft.export.ExporterFactory;
import querycraft.export.CsvExporter;
import querycraft.model.ExportOptions;
import querycraft.model.DatabaseType;

public class ExporterTest {

    @Test
    public void testFilenameGeneration() {
        String filename = ExporterFactory.generateDefaultFilename("export", "csv");
        assertTrue(filename.startsWith("export_"));
        assertTrue(filename.endsWith(".csv"));
        assertTrue(filename.length() > 20); // Length includes timestamp
    }

    @Test
    public void testCsvExporterOptions() {
        ExportOptions options = new ExportOptions();
        options.setEncoding(ExportOptions.Encoding.UTF_8);
        options.setDelimiter(ExportOptions.Delimiter.COMMA);
        options.setIncludeHeader(true);
        
        CsvExporter exporter = (CsvExporter) ExporterFactory.createCsvExporter(options);
        assertNotNull(exporter);
        assertEquals(ExportOptions.Encoding.UTF_8, exporter.getOptions().getEncoding());
        assertEquals(ExportOptions.Delimiter.COMMA, exporter.getOptions().getDelimiter());
        assertTrue(exporter.getOptions().isIncludeHeader());
    }

    @Test
    public void testSqlExporterCreation() {
        // Just verify it creates the right type
        assertNotNull(ExporterFactory.createSqlExporter("test_table", DatabaseType.MYSQL));
        assertNotNull(ExporterFactory.createSqlExporter("test_table", DatabaseType.POSTGRESQL));
    }
}
