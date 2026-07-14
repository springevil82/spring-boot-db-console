package io.github.dbconsole.model;

/**
 * Represents a database table or view.
 */
public class TableInfo {

    private String catalog;
    private String schema;
    private String name;
    private String type;    // TABLE, VIEW, SYSTEM TABLE, etc.
    private String remarks;

    public TableInfo() {}

    public TableInfo(String catalog, String schema, String name, String type, String remarks) {
        this.catalog = catalog;
        this.schema = schema;
        this.name = name;
        this.type = type;
        this.remarks = remarks;
    }

    public String getCatalog() { return catalog; }
    public void setCatalog(String catalog) { this.catalog = catalog; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}

