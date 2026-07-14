package io.github.dbconsole.model;

/**
 * Represents a column in a database table.
 */
public class ColumnInfo {

    private String name;
    private String typeName;
    private int columnSize;
    private int decimalDigits;
    private boolean nullable;
    private String defaultValue;
    private String remarks;
    private boolean primaryKey;
    private int ordinalPosition;

    public ColumnInfo() {}

    public ColumnInfo(String name, String typeName, int columnSize, int decimalDigits,
                      boolean nullable, String defaultValue, String remarks,
                      boolean primaryKey, int ordinalPosition) {
        this.name = name;
        this.typeName = typeName;
        this.columnSize = columnSize;
        this.decimalDigits = decimalDigits;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.remarks = remarks;
        this.primaryKey = primaryKey;
        this.ordinalPosition = ordinalPosition;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public int getColumnSize() { return columnSize; }
    public void setColumnSize(int columnSize) { this.columnSize = columnSize; }

    public int getDecimalDigits() { return decimalDigits; }
    public void setDecimalDigits(int decimalDigits) { this.decimalDigits = decimalDigits; }

    public boolean isNullable() { return nullable; }
    public void setNullable(boolean nullable) { this.nullable = nullable; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public int getOrdinalPosition() { return ordinalPosition; }
    public void setOrdinalPosition(int ordinalPosition) { this.ordinalPosition = ordinalPosition; }
}

