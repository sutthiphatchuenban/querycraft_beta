package querycraft.export;

import querycraft.model.ColumnInfo;
import querycraft.model.ExportOptions;
import querycraft.query.StreamingQueryService.StreamingExporter;
import querycraft.util.CsvValueFormatter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

public class CsvStreamingExporter implements StreamingExporter {

    private final ExportOptions options;
    private final File file;
    private OutputStream os;
    private Writer writer;

    private final String delimiter;
    private final SimpleDateFormat dateFormat;

    public CsvStreamingExporter(File file, ExportOptions options) {
        this.file = file;
        this.options = options != null ? options : new ExportOptions();
        this.delimiter = this.options.getEffectiveDelimiter();
        this.dateFormat = new SimpleDateFormat(this.options.getDateFormat());
    }

    @Override
    public void start(List<ColumnInfo> columns) throws Exception {
        Charset charset = options.getEffectiveCharset();
        boolean withBom = options.getEncoding().isWithBom();

        this.os = new FileOutputStream(file);
        
        // Write BOM if needed
        if (withBom) {
            if (charset.equals(StandardCharsets.UTF_8)) {
                os.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
            } else if (charset.equals(StandardCharsets.UTF_16LE)) {
                os.write(new byte[]{(byte)0xFF, (byte)0xFE});
            } else if (charset.equals(StandardCharsets.UTF_16BE)) {
                os.write(new byte[]{(byte)0xFE, (byte)0xFF});
            }
        }
        
        this.writer = new OutputStreamWriter(os, charset);

        // Write header
        if (options.isIncludeHeader()) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) writer.write(delimiter);
                writer.write(escapeValue(columns.get(i).getName()));
            }
            writer.write("\r\n");
        }
    }

    @Override
    public void writeRow(Object[] row) throws Exception {
        for (int i = 0; i < row.length; i++) {
            if (i > 0) writer.write(delimiter);

            Object value = row[i];
            String formattedValue = formatValue(value);
            writer.write(escapeValue(formattedValue));
        }
        writer.write("\r\n");
    }

    @Override
    public void finish() throws Exception {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
        if (os != null) {
            os.close();
        }
    }

    @Override
    public void abort() {
        try {
            if (writer != null) writer.close();
            if (os != null) os.close();
        } catch (Exception ignored) {}
    }

    private String formatValue(Object value) {
        return CsvValueFormatter.formatValue(value, dateFormat, options);
    }

    private String escapeValue(String value) {
        return CsvValueFormatter.escapeValue(value, delimiter, options);
    }
}
