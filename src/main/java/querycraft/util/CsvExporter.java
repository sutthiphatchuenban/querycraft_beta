package querycraft.util;

import querycraft.model.ColumnInfo;
import querycraft.model.ExportOptions;
import querycraft.model.QueryResult;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utility class for exporting query results to CSV format.
 */
public class CsvExporter {

    /**
     * Export query result to CSV file with specified options.
     */
    public static void export(QueryResult result, File file, ExportOptions options) throws IOException {
        Charset charset = options.getEffectiveCharset();
        boolean withBom = options.getEncoding().isWithBom();
        String delimiter = options.getEffectiveDelimiter();

        try (OutputStream os = new FileOutputStream(file);
             Writer writer = new OutputStreamWriter(os, charset)) {

            // Write BOM if needed
            if (withBom) {
                os.write(0xEF);
                os.write(0xBB);
                os.write(0xBF);
            }

            // Write header
            if (options.isIncludeHeader()) {
                writeHeader(writer, result.getColumns(), delimiter, options);
            }

            // Write data rows
            SimpleDateFormat dateFormat = new SimpleDateFormat(options.getDateFormat());
            for (Object[] row : result.getRows()) {
                writeRow(writer, row, result.getColumns(), delimiter, dateFormat, options);
            }
        }
    }

    /**
     * Export query result to CSV with default options.
     */
    public static void export(QueryResult result, File file) throws IOException {
        export(result, file, new ExportOptions());
    }

    private static void writeHeader(Writer writer, List<ColumnInfo> columns, String delimiter,
                                    ExportOptions options) throws IOException {
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                writer.write(delimiter);
            }
            String header = columns.get(i).getName();
            writer.write(escapeValue(header, delimiter, options));
        }
        writer.write("\r\n"); // Windows line ending for Excel compatibility
    }

    private static void writeRow(Writer writer, Object[] row, List<ColumnInfo> columns,
                                 String delimiter, SimpleDateFormat dateFormat,
                                 ExportOptions options) throws IOException {
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                writer.write(delimiter);
            }

            Object value = row[i];
            String formattedValue = formatValue(value, columns.get(i), dateFormat, options);
            writer.write(escapeValue(formattedValue, delimiter, options));
        }
        writer.write("\r\n");
    }

    private static String formatValue(Object value, ColumnInfo columnInfo,
                                      SimpleDateFormat dateFormat, ExportOptions options) {
        if (value == null) {
            return options.getNullValue();
        }

        if (value instanceof Date) {
            return dateFormat.format((Date) value);
        }

        if (value instanceof java.sql.Date) {
            return dateFormat.format(new Date(((java.sql.Date) value).getTime()));
        }

        if (value instanceof java.sql.Timestamp) {
            return dateFormat.format(new Date(((java.sql.Timestamp) value).getTime()));
        }

        if (value instanceof java.sql.Time) {
            return dateFormat.format(new Date(((java.sql.Time) value).getTime()));
        }

        return value.toString();
    }

    private static String escapeValue(String value, String delimiter, ExportOptions options) {
        if (value == null) {
            return "";
        }

        boolean needsQuotes = options.isQuoteAllValues() ||
                value.contains(delimiter) ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r");

        if (!needsQuotes) {
            return value;
        }

        // Escape quotes by doubling them
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Generate a default filename for export.
     */
    public static String generateFilename(String tableName, String extension) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        return String.format("%s_%s.%s", tableName, timestamp, extension);
    }
}
