package io.github.dbconsole.model;

import java.util.List;

/**
 * Unified result of a SQL query or DML statement execution.
 */
public class QueryResult {

    public enum Type { SELECT, UPDATE, ERROR }

    private Type type;
    private List<String> columns;
    private List<List<Object>> rows;
    private int updateCount;
    private long executionTimeMs;
    private String error;
    private String warningMessage;

    // ----------------------------------------------------------------
    // Factory methods
    // ----------------------------------------------------------------

    public static QueryResult select(List<String> columns, List<List<Object>> rows, long executionTimeMs) {
        QueryResult r = new QueryResult();
        r.type = Type.SELECT;
        r.columns = columns;
        r.rows = rows;
        r.executionTimeMs = executionTimeMs;
        return r;
    }

    public static QueryResult update(int updateCount, long executionTimeMs) {
        QueryResult r = new QueryResult();
        r.type = Type.UPDATE;
        r.updateCount = updateCount;
        r.executionTimeMs = executionTimeMs;
        return r;
    }

    public static QueryResult error(String errorMessage, long executionTimeMs) {
        QueryResult r = new QueryResult();
        r.type = Type.ERROR;
        r.error = errorMessage;
        r.executionTimeMs = executionTimeMs;
        return r;
    }

    // ----------------------------------------------------------------
    // Getters / setters
    // ----------------------------------------------------------------

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<List<Object>> getRows() { return rows; }
    public void setRows(List<List<Object>> rows) { this.rows = rows; }

    public int getUpdateCount() { return updateCount; }
    public void setUpdateCount(int updateCount) { this.updateCount = updateCount; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}

