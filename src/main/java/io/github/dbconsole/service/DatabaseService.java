package io.github.dbconsole.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dbconsole.model.ColumnInfo;
import io.github.dbconsole.model.DataSourceInfo;
import io.github.dbconsole.model.IndexInfo;
import io.github.dbconsole.model.QueryRequest;
import io.github.dbconsole.model.QueryResult;
import io.github.dbconsole.model.TableInfo;

/**
 * Core service that talks to the database via JDBC metadata and statement execution.
 * All operations open a fresh connection and close it immediately to avoid
 * interfering with the application's connection pool or transaction management.
 */
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final DataSourceRegistry registry;

    public DatabaseService(DataSourceRegistry registry) {
        this.registry = registry;
    }

    // ===================================================================
    //  DataSource info
    // ===================================================================

    public List<DataSourceInfo> getDataSourceInfos() {
        List<DataSourceInfo> result = new ArrayList<>();
        for (String name : registry.getNames()) {
            DataSource ds = registry.get(name);
            DataSourceInfo info = buildDataSourceInfo(name, ds);
            result.add(info);
        }
        return result;
    }

    private DataSourceInfo buildDataSourceInfo(String name, DataSource ds) {
        DataSourceInfo info = new DataSourceInfo();
        info.setName(name);
        info.setPrimary(registry.isPrimary(name));

        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            info.setUrl(safeGet(meta::getURL));
            info.setDriverClass(safeGet(meta::getDriverName));
            info.setProductName(safeGet(meta::getDatabaseProductName));
            info.setProductVersion(safeGet(meta::getDatabaseProductVersion));
        } catch (Exception e) {
            log.warn("Could not read metadata for datasource '{}': {}", name, e.getMessage());
            info.setUrl("(unavailable)");
        }
        return info;
    }

    // ===================================================================
    //  Schema / catalog discovery
    // ===================================================================

    /**
     * Returns a two-level structure: catalog names → list of schema names.
     * For databases that don't support catalogs (PostgreSQL, etc.) the catalog
     * key will be {@code null}.
     */
    public Map<String, List<String>> getSchemasGroupedByCatalog(String dsName) {
        DataSource ds = requireDataSource(dsName);
        Map<String, List<String>> result = new LinkedHashMap<>();

        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // Try schemas first
            try (ResultSet rs = meta.getSchemas()) {
                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    String catalog;
                    try {
                        catalog = rs.getString("TABLE_CATALOG");
                    } catch (SQLException ex) {
                        catalog = null;
                    }

                    if (catalog == null) {
                        catalog = "default";
                    }

                    result.computeIfAbsent(catalog, k -> new ArrayList<>()).add(schema);
                }
            }

            // If no schemas, fall back to catalogs
            if (result.isEmpty()) {
                try (ResultSet rs = meta.getCatalogs()) {
                    while (rs.next()) {
                        String catalog = rs.getString("TABLE_CAT");

                        if (catalog == null) {
                            catalog = "default";
                        }

                        result.put(catalog, Collections.<String>emptyList());
                    }
                }
            }

            // Last resort: single unnamed entry
            if (result.isEmpty()) {
                result.put(null, Collections.<String>emptyList());
            }

        } catch (SQLException e) {
            log.error("Failed to read schemas for datasource '{}': {}", dsName, e.getMessage());
            throw new DbConsoleException("Failed to read schemas: " + e.getMessage(), e);
        }
        return result;
    }

    // ===================================================================
    //  Table listing
    // ===================================================================

    public List<TableInfo> getTables(String dsName, String catalog, String schema) {
        DataSource ds = requireDataSource(dsName);
        List<TableInfo> tables = new ArrayList<>();

        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String[] types = {"TABLE", "VIEW", "MATERIALIZED VIEW", "FOREIGN TABLE"};

            try (ResultSet rs = meta.getTables(emptyToNull(catalog), emptyToNull(schema), "%", types)) {
                while (rs.next()) {
                    String tableCatalog = rs.getString("TABLE_CAT");
                    String tableSchema  = rs.getString("TABLE_SCHEM");
                    String tableName    = rs.getString("TABLE_NAME");
                    String tableType    = rs.getString("TABLE_TYPE");
                    String remarks;
                    try { remarks = rs.getString("REMARKS"); } catch (SQLException ex) { remarks = null; }

                    tables.add(new TableInfo(tableCatalog, tableSchema, tableName, tableType, remarks));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to list tables for datasource '{}': {}", dsName, e.getMessage());
            throw new DbConsoleException("Failed to list tables: " + e.getMessage(), e);
        }

        tables.sort(Comparator.comparing(TableInfo::getName, String.CASE_INSENSITIVE_ORDER));
        return tables;
    }

    // ===================================================================
    //  Column & index info
    // ===================================================================

    public List<ColumnInfo> getColumns(String dsName, String catalog, String schema, String table) {
        DataSource ds = requireDataSource(dsName);
        List<ColumnInfo> columns = new ArrayList<>();

        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // Primary key columns
            Set<String> pkCols = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(emptyToNull(catalog), emptyToNull(schema), table)) {
                while (pkRs.next()) {
                    pkCols.add(pkRs.getString("COLUMN_NAME"));
                }
            } catch (SQLException ex) {
                log.debug("Could not retrieve PK info for {}: {}", table, ex.getMessage());
            }

            try (ResultSet rs = meta.getColumns(emptyToNull(catalog), emptyToNull(schema), table, "%")) {
                while (rs.next()) {
                    String colName     = rs.getString("COLUMN_NAME");
                    String typeName    = rs.getString("TYPE_NAME");
                    int    colSize     = rs.getInt("COLUMN_SIZE");
                    int    decDigits   = getIntSafe(rs, "DECIMAL_DIGITS");
                    boolean nullable   = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                    String defVal      = rs.getString("COLUMN_DEF");
                    String remarks;
                    try { remarks = rs.getString("REMARKS"); } catch (SQLException ex) { remarks = null; }
                    int ordinal        = rs.getInt("ORDINAL_POSITION");

                    columns.add(new ColumnInfo(colName, typeName, colSize, decDigits,
                            nullable, defVal, remarks, pkCols.contains(colName), ordinal));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to read columns for table '{}': {}", table, e.getMessage());
            throw new DbConsoleException("Failed to read columns: " + e.getMessage(), e);
        }

        columns.sort(Comparator.comparingInt(ColumnInfo::getOrdinalPosition));
        return columns;
    }

    public List<IndexInfo> getIndexes(String dsName, String catalog, String schema, String table) {
        DataSource ds = requireDataSource(dsName);
        List<IndexInfo> indexes = new ArrayList<>();

        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getIndexInfo(emptyToNull(catalog), emptyToNull(schema), table, false, true)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName == null) continue; // statistics pseudo-row
                    String  colName    = rs.getString("COLUMN_NAME");
                    boolean unique     = !rs.getBoolean("NON_UNIQUE");
                    int     ordinal    = rs.getInt("ORDINAL_POSITION");
                    String  asc        = rs.getString("ASC_OR_DESC");
                    indexes.add(new IndexInfo(indexName, colName, unique, ordinal, asc));
                }
            }
        } catch (SQLException e) {
            log.warn("Could not read indexes for table '{}': {}", table, e.getMessage());
            // non-fatal – return empty list
        }
        return indexes;
    }

    // ===================================================================
    //  Query execution
    // ===================================================================

    public QueryResult executeQuery(String dsName, QueryRequest request) {
        DataSource ds = requireDataSource(dsName);
        String sql = request.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            return QueryResult.error("SQL statement is empty", 0);
        }

        long start = System.currentTimeMillis();

        try (Connection conn = ds.getConnection()) {
            // Optionally set schema/catalog context
            if (request.getCatalog() != null && !request.getCatalog().isEmpty()) {
                try { conn.setCatalog(request.getCatalog()); } catch (Exception ignored) {}
            }
            if (request.getSchema() != null && !request.getSchema().isEmpty()) {
                try { conn.setSchema(request.getSchema()); } catch (Exception ignored) {}
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.setMaxRows(Math.min(request.getMaxRows(), 10_000));
                boolean hasResultSet = stmt.execute(sql.trim());

                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        return buildSelectResult(rs, System.currentTimeMillis() - start);
                    }
                } else {
                    int updateCount = stmt.getUpdateCount();
                    return QueryResult.update(updateCount, System.currentTimeMillis() - start);
                }
            }
        } catch (SQLException e) {
            log.debug("Query execution error on datasource '{}': {}", dsName, e.getMessage());
            return QueryResult.error(e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    public QueryResult previewTable(String dsName, String catalog, String schema,
                                    String table, int limit) {
        String quotedTable = quoteIdentifier(table);
        String sql = "SELECT * FROM " + quotedTable;

        QueryRequest req = new QueryRequest();
        req.setSql(sql);
        req.setMaxRows(limit > 0 ? limit : 100);
        req.setCatalog(catalog);
        req.setSchema(schema);
        return executeQuery(dsName, req);
    }

    // ===================================================================
    //  Private helpers
    // ===================================================================

    private QueryResult buildSelectResult(ResultSet rs, long elapsedMs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int colCount = rsmd.getColumnCount();

        List<String> columns = new ArrayList<>(colCount);
        for (int i = 1; i <= colCount; i++) {
            String label = rsmd.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = rsmd.getColumnName(i);
            }
            columns.add(label);
        }

        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                // Convert types that Jackson might not know how to serialize
                if (val instanceof byte[]) {
                    val = Base64.getEncoder().encodeToString((byte[]) val);
                } else if (val != null && !(val instanceof String)
                        && !(val instanceof Number)
                        && !(val instanceof Boolean)
                        && !(val instanceof java.util.Date)
                        && !(val instanceof java.sql.Date)
                        && !(val instanceof java.sql.Timestamp)
                        && !(val instanceof java.sql.Time)) {
                    val = val.toString();
                }
                row.add(val);
            }
            rows.add(row);
        }
        return QueryResult.select(columns, rows, elapsedMs);
    }

    private DataSource requireDataSource(String name) {
        DataSource ds = registry.get(name);
        if (ds == null) {
            throw new DbConsoleException("DataSource not found: " + name);
        }
        return ds;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s;
    }

    private static int getIntSafe(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (SQLException e) { return 0; }
    }

    private static String quoteIdentifier(String identifier) {
        // Standard SQL double-quote escaping
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private static <T> T safeGet(SqlSupplier<T> supplier) {
        try { return supplier.get(); } catch (Exception e) { return null; }
    }

    // ===================================================================
    //  Inner exception type
    // ===================================================================

    public static class DbConsoleException extends RuntimeException {
        public DbConsoleException(String message) { super(message); }
        public DbConsoleException(String message, Throwable cause) { super(message, cause); }
    }
}

