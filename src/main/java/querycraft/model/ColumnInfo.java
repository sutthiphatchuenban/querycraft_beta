package querycraft.model;

import java.sql.Types;

/**
 * Model class representing column metadata.
 */
public class ColumnInfo {
    private String name;
    private String typeName;
    private int sqlType;
    private int displaySize;
    private boolean nullable;

    public ColumnInfo() {
    }

    public ColumnInfo(String name, String typeName, int sqlType, int displaySize, boolean nullable) {
        this.name = name;
        this.typeName = typeName;
        this.sqlType = sqlType;
        this.displaySize = displaySize;
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getSqlType() {
        return sqlType;
    }

    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    public int getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(int displaySize) {
        this.displaySize = displaySize;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isNumeric() {
        return sqlType == Types.INTEGER ||
                sqlType == Types.BIGINT ||
                sqlType == Types.SMALLINT ||
                sqlType == Types.TINYINT ||
                sqlType == Types.NUMERIC ||
                sqlType == Types.DECIMAL ||
                sqlType == Types.FLOAT ||
                sqlType == Types.DOUBLE ||
                sqlType == Types.REAL;
    }

    public boolean isDateTime() {
        return sqlType == Types.DATE ||
                sqlType == Types.TIME ||
                sqlType == Types.TIMESTAMP ||
                sqlType == Types.TIME_WITH_TIMEZONE ||
                sqlType == Types.TIMESTAMP_WITH_TIMEZONE;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, typeName);
    }
}
