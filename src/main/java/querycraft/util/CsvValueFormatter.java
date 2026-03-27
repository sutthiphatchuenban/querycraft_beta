package querycraft.util;

import querycraft.model.ExportOptions;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Shared utility for formatting CSV values.
 * Used by both CsvExporter (batch) and CsvStreamingExporter (streaming).
 */
public final class CsvValueFormatter {

    private CsvValueFormatter() {} // Prevent instantiation

    /**
     * Format a Java object into its CSV string representation.
     */
    public static String formatValue(Object value, SimpleDateFormat dateFormat, ExportOptions options) {
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

    /**
     * Escape a CSV value (add quotes if needed, double internal quotes).
     */
    public static String escapeValue(String value, String delimiter, ExportOptions options) {
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
}
