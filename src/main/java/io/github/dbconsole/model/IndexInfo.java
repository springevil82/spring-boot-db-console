package io.github.dbconsole.model;

/**
 * Represents an index on a database table.
 */
public class IndexInfo {

    private String name;
    private String columnName;
    private boolean unique;
    private int ordinalPosition;
    private String ascOrDesc; // A = ascending, D = descending, null = not supported

    public IndexInfo() {}

    public IndexInfo(String name, String columnName, boolean unique,
                     int ordinalPosition, String ascOrDesc) {
        this.name = name;
        this.columnName = columnName;
        this.unique = unique;
        this.ordinalPosition = ordinalPosition;
        this.ascOrDesc = ascOrDesc;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public boolean isUnique() { return unique; }
    public void setUnique(boolean unique) { this.unique = unique; }

    public int getOrdinalPosition() { return ordinalPosition; }
    public void setOrdinalPosition(int ordinalPosition) { this.ordinalPosition = ordinalPosition; }

    public String getAscOrDesc() { return ascOrDesc; }
    public void setAscOrDesc(String ascOrDesc) { this.ascOrDesc = ascOrDesc; }
}

