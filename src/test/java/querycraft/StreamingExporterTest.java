package querycraft;

import org.junit.Test;
import querycraft.model.ColumnInfo;
import querycraft.model.DatabaseType;
import querycraft.model.ExportOptions;
import querycraft.util.CompositeStreamingExporter;
import querycraft.util.CsvStreamingExporter;
import querycraft.util.SqlStreamingExporter;

import java.io.File;
import java.nio.file.Files;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class StreamingExporterTest {

    @Test
    public void testCsvStreamingExporter() throws Exception {
        File csvFile = File.createTempFile("test_export", ".csv");
        csvFile.deleteOnExit();

        ExportOptions options = new ExportOptions();
        options.setIncludeHeader(true);
        options.setQuoteAllValues(false);

        CsvStreamingExporter exporter = new CsvStreamingExporter(csvFile, options);
        
        List<ColumnInfo> columns = Arrays.asList(
                new ColumnInfo("id", "INTEGER", Types.INTEGER, 11, true),
                new ColumnInfo("name", "VARCHAR", Types.VARCHAR, 255, true)
        );

        exporter.start(columns);
        exporter.writeRow(new Object[]{1, "Alice"});
        exporter.writeRow(new Object[]{2, "Bob, Smith"}); // Should be quoted automatically
        exporter.finish();

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).endsWith("id,name")); // Might have BOM depending on platform default false/true
        assertEquals("1,Alice", lines.get(1));
        assertEquals("2,\"Bob, Smith\"", lines.get(2));
    }

    @Test
    public void testSqlStreamingExporter() throws Exception {
        File sqlFile = File.createTempFile("test_export", ".sql");
        sqlFile.deleteOnExit();

        SqlStreamingExporter exporter = new SqlStreamingExporter(sqlFile, "users", DatabaseType.POSTGRESQL);

        List<ColumnInfo> columns = Arrays.asList(
                new ColumnInfo("id", "INTEGER", Types.INTEGER, 11, true),
                new ColumnInfo("is_active", "BOOLEAN", Types.BOOLEAN, 1, true)
        );

        exporter.start(columns);
        exporter.writeRow(new Object[]{1, true});
        exporter.writeRow(new Object[]{2, false});
        exporter.finish();

        String sqlContent = Files.readString(sqlFile.toPath());

        assertTrue(sqlContent.contains("BEGIN;"));
        assertTrue(sqlContent.contains("INSERT INTO \"users\" (\"id\", \"is_active\") VALUES"));
        assertTrue(sqlContent.contains("(1, TRUE)"));
        assertTrue(sqlContent.contains("(2, FALSE);"));
        assertTrue(sqlContent.contains("COMMIT;"));
    }

    @Test
    public void testCompositeStreamingExporter() throws Exception {
        File csvFile = File.createTempFile("test_composite", ".csv");
        File sqlFile = File.createTempFile("test_composite", ".sql");
        csvFile.deleteOnExit();
        sqlFile.deleteOnExit();

        CompositeStreamingExporter composite = new CompositeStreamingExporter();
        composite.addExporter(new CsvStreamingExporter(csvFile, new ExportOptions()));
        composite.addExporter(new SqlStreamingExporter(sqlFile, "composite_table", DatabaseType.MYSQL));

        List<ColumnInfo> columns = Arrays.asList(
                new ColumnInfo("id", "INTEGER", Types.INTEGER, 11, true)
        );

        composite.start(columns);
        composite.writeRow(new Object[]{100});
        composite.finish();

        List<String> csvLines = Files.readAllLines(csvFile.toPath());
        String sqlContent = Files.readString(sqlFile.toPath());

        assertEquals(2, csvLines.size()); // Header + 1 row
        assertEquals("100", csvLines.get(1));

        assertTrue(sqlContent.contains("INSERT INTO `composite_table` (`id`) VALUES"));
        assertTrue(sqlContent.contains("(100);"));
    }
}
