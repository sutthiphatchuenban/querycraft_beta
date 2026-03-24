package querycraft.util;

import querycraft.model.ColumnInfo;
import querycraft.model.ExportOptions;
import querycraft.model.QueryResult;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Strategy implementation for exporting query results to CSV format.
 */
public class CsvExporter implements DataExporter {

    private final ExportOptions options;

    public CsvExporter(ExportOptions options) {
        this.options = options != null ? options : new ExportOptions();
    }

    public CsvExporter() {
        this(new ExportOptions());
    }

    public ExportOptions getOptions() {
        return options;
    }

    @Override
    public void export(QueryResult result, File file) throws IOException {
        export(result, file, this.options);
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public String getDisplayName() {
        return "Comma Separated Values (CSV)";
    }

    /**
     * Legacy static method for backward compatibility if needed.
     */
    public static void export(QueryResult result, File file, ExportOptions options) throws IOException {
        java.nio.charset.Charset charset = options.getEffectiveCharset();
        boolean withBom = options.getEncoding().isWithBom();
        String delimiter = options.getEffectiveDelimiter();

        try (java.io.OutputStream os = new java.io.FileOutputStream(file);
             java.io.Writer writer = new java.io.OutputStreamWriter(os, charset)) {

            // Write BOM if needed
            if (withBom) {
                if (charset.equals(java.nio.charset.StandardCharsets.UTF_8)) {
                    os.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                } else if (charset.equals(java.nio.charset.StandardCharsets.UTF_16LE)) {
                    os.write(new byte[]{(byte)0xFF, (byte)0xFE});
                } else if (charset.equals(java.nio.charset.StandardCharsets.UTF_16BE)) {
                    os.write(new byte[]{(byte)0xFE, (byte)0xFF});
                }
            }

            // Write header
            if (options.isIncludeHeader()) {
                writeHeader(writer, result.getColumns(), delimiter, options);
            }

            // Write data rows
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(options.getDateFormat());
            for (Object[] row : result.getRows()) {
                writeRow(writer, row, result.getColumns(), delimiter, dateFormat, options);
            }
        }
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

        if (value instanceof java.sql.Date) {
            return dateFormat.format(new Date(((java.sql.Date) value).getTime()));
        }

        if (value instanceof java.sql.Timestamp) {
            return dateFormat.format(new Date(((java.sql.Timestamp) value).getTime()));
        }

        if (value instanceof java.sql.Time) {
            return dateFormat.format(new Date(((java.sql.Time) value).getTime()));
        }

        if (value instanceof Date) {
            return dateFormat.format((Date) value);
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
