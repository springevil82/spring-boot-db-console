package io.github.dbconsole.model;

/**
 * Request body for SQL query execution.
 */
public class QueryRequest {

    /** The SQL statement(s) to execute. */
    private String sql;

    /** Maximum number of rows to return for SELECT queries. Defaults to 500. */
    private int maxRows = 500;

    /** Catalog context for the query (optional). */
    private String catalog;

    /** Schema context for the query (optional). */
    private String schema;

    public QueryRequest() {}

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows > 0 ? maxRows : 500; }

    public String getCatalog() { return catalog; }
    public void setCatalog(String catalog) { this.catalog = catalog; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}

