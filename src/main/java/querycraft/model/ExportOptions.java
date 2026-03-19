package querycraft.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Model class holding CSV export options.
 */
public class ExportOptions {

    public enum CsvFormat {
        STANDARD("Standard CSV"),
        EXCEL("Excel Compatible");

        private final String displayName;

        CsvFormat(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum Encoding {
        UTF_8_BOM("UTF-8 with BOM", StandardCharsets.UTF_8, true),
        UTF_8("UTF-8", StandardCharsets.UTF_8),
        TIS_620("TIS-620 (Thai Windows)", Charset.forName("TIS-620")),
        WINDOWS_874("Windows-874", Charset.forName("Windows-874")),
        UTF_16LE_BOM("UTF-16LE with BOM (Unicode)", StandardCharsets.UTF_16LE, true),
        ASCII("US-ASCII", StandardCharsets.US_ASCII),
        ISO_8859_1("ISO-8859-1 (Western)", StandardCharsets.ISO_8859_1);

        private final String displayName;
        private final Charset charset;
        private final boolean withBom;

        Encoding(String displayName, Charset charset) {
            this(displayName, charset, false);
        }

        Encoding(String displayName, Charset charset, boolean withBom) {
            this.displayName = displayName;
            this.charset = charset;
            this.withBom = withBom;
        }

        public Charset getCharset() {
            return charset;
        }

        public boolean isWithBom() {
            return withBom;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum Delimiter {
        COMMA(",", "Comma (,)", ","),
        SEMICOLON(";", "Semicolon (;)", ";"),
        TAB("\t", "Tab", "\\t"),
        PIPE("|", "Pipe (|)", "|"),
        OTHER("", "Other (Custom)", "");

        private final String value;
        private final String displayName;
        private final String displayValue;

        Delimiter(String value, String displayName, String displayValue) {
            this.value = value;
            this.displayName = displayName;
            this.displayValue = displayValue;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private CsvFormat format = CsvFormat.STANDARD;
    private Encoding encoding = Encoding.UTF_8_BOM;
    private Delimiter delimiter = Delimiter.COMMA;
    private String customDelimiter = "";
    private boolean includeHeader = true;
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private String nullValue = "";
    private boolean quoteAllValues = false;

    public CsvFormat getFormat() {
        return format;
    }

    public void setFormat(CsvFormat format) {
        this.format = format;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    public Charset getEffectiveCharset() {
        return encoding.getCharset();
    }

    public Delimiter getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(Delimiter delimiter) {
        this.delimiter = delimiter;
    }

    public String getCustomDelimiter() {
        return customDelimiter;
    }

    public void setCustomDelimiter(String customDelimiter) {
        this.customDelimiter = customDelimiter;
    }

    public String getEffectiveDelimiter() {
        if (delimiter == Delimiter.OTHER) {
            return customDelimiter != null && !customDelimiter.isEmpty() ? customDelimiter : ",";
        }
        return delimiter.getValue();
    }

    public boolean isIncludeHeader() {
        return includeHeader;
    }

    public void setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getNullValue() {
        return nullValue;
    }

    public void setNullValue(String nullValue) {
        this.nullValue = nullValue;
    }

    public boolean isQuoteAllValues() {
        return quoteAllValues;
    }

    public void setQuoteAllValues(boolean quoteAllValues) {
        this.quoteAllValues = quoteAllValues;
    }

    @Override
    public String toString() {
        return String.format("ExportOptions{format=%s, encoding=%s, delimiter=%s, includeHeader=%s}",
                format, encoding, delimiter, includeHeader);
    }
}
