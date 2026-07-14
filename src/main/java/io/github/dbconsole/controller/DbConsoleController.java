package io.github.dbconsole.controller;

import io.github.dbconsole.autoconfigure.DbConsoleProperties;
import io.github.dbconsole.model.*;
import io.github.dbconsole.service.DatabaseService;
import io.github.dbconsole.service.DatabaseService.DbConsoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Exposes the DB Console UI and REST API.
 *
 * <p>All endpoints are mounted under the configured {@code db-console.path}
 * (defaults to {@code /db-console}).
 *
 * <p><strong>NOTE:</strong> This class intentionally avoids any direct
 * {@code javax.servlet} / {@code jakarta.servlet} imports so that the same
 * compiled JAR works on both Spring Boot 2.x (javax) and Spring Boot 3.x
 * (jakarta) runtimes.
 */
@RestController
@RequestMapping("${db-console.path:/db-console}")
public class DbConsoleController {

    private static final Logger log = LoggerFactory.getLogger(DbConsoleController.class);

    private final DatabaseService databaseService;
    private final DbConsoleProperties properties;

    public DbConsoleController(DatabaseService databaseService, DbConsoleProperties properties) {
        this.databaseService = databaseService;
        this.properties = properties;
    }

    // ===================================================================
    //  UI – serve the single-page application
    // ===================================================================

    @GetMapping(value = {"", "/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> ui() throws IOException {
        String html = readClasspathResource("db-console/index.html");
        html = html.replace("__BASE_PATH__", properties.getPath());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    // ===================================================================
    //  API – DataSources
    // ===================================================================

    @GetMapping("/api/datasources")
    public ResponseEntity<List<DataSourceInfo>> datasources() {
        return ResponseEntity.ok(databaseService.getDataSourceInfos());
    }

    // ===================================================================
    //  API – Schema tree
    // ===================================================================

    @GetMapping("/api/datasources/{name}/schemas")
    public ResponseEntity<Map<String, List<String>>> schemas(
            @PathVariable("name") String datasourceName) {
        return ResponseEntity.ok(databaseService.getSchemasGroupedByCatalog(datasourceName));
    }

    @GetMapping("/api/datasources/{name}/tables")
    public ResponseEntity<List<TableInfo>> tables(
            @PathVariable("name") String datasourceName,
            @RequestParam(value = "catalog", required = false) String catalog,
            @RequestParam(value = "schema",  required = false) String schema) {
        return ResponseEntity.ok(databaseService.getTables(datasourceName, catalog, schema));
    }

    @GetMapping("/api/datasources/{name}/columns")
    public ResponseEntity<List<ColumnInfo>> columns(
            @PathVariable("name") String datasourceName,
            @RequestParam(value = "catalog", required = false) String catalog,
            @RequestParam(value = "schema",  required = false) String schema,
            @RequestParam("table") String table) {
        return ResponseEntity.ok(databaseService.getColumns(datasourceName, catalog, schema, table));
    }

    @GetMapping("/api/datasources/{name}/indexes")
    public ResponseEntity<List<IndexInfo>> indexes(
            @PathVariable("name") String datasourceName,
            @RequestParam(value = "catalog", required = false) String catalog,
            @RequestParam(value = "schema",  required = false) String schema,
            @RequestParam("table") String table) {
        return ResponseEntity.ok(databaseService.getIndexes(datasourceName, catalog, schema, table));
    }

    // ===================================================================
    //  API – Query execution
    // ===================================================================

    @PostMapping("/api/datasources/{name}/execute")
    public ResponseEntity<QueryResult> execute(
            @PathVariable("name") String datasourceName,
            @RequestBody QueryRequest request) {
        return ResponseEntity.ok(databaseService.executeQuery(datasourceName, request));
    }

    @GetMapping("/api/datasources/{name}/preview")
    public ResponseEntity<QueryResult> preview(
            @PathVariable("name") String datasourceName,
            @RequestParam(value = "catalog", required = false) String catalog,
            @RequestParam(value = "schema",  required = false) String schema,
            @RequestParam("table") String table,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return ResponseEntity.ok(databaseService.previewTable(datasourceName, catalog, schema, table, limit));
    }

    // ===================================================================
    //  API – CSV export
    // ===================================================================

    @PostMapping("/api/datasources/{name}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable("name") String datasourceName,
            @RequestBody QueryRequest request) throws IOException {

        request.setMaxRows(100_000);
        QueryResult result = databaseService.executeQuery(datasourceName, request);

        if (result.getType() == QueryResult.Type.ERROR) {
            return ResponseEntity.badRequest()
                    .body(("Error: " + result.getError()).getBytes(StandardCharsets.UTF_8));
        }

        byte[] csv = buildCsv(result);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"query-result.csv\"");
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    // ===================================================================
    //  Error handling
    // ===================================================================

    @ExceptionHandler(DbConsoleException.class)
    public ResponseEntity<Map<String, String>> handleDbConsoleException(DbConsoleException ex) {
        log.debug("DB Console error: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("Unexpected error in DB Console", ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", "Internal error: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ===================================================================
    //  Private helpers
    // ===================================================================

    private String readClasspathResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return inputStreamToString(is);
        }
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] block = new byte[4096];
        int read;
        while ((read = is.read(block)) != -1) {
            buf.write(block, 0, read);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    private static byte[] buildCsv(QueryResult result) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        writer.write('\uFEFF'); // BOM for Excel

        List<String> cols = result.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) writer.write(',');
            writer.write(csvEscape(cols.get(i)));
        }
        writer.write("\r\n");

        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) writer.write(',');
                    Object val = row.get(i);
                    writer.write(val == null ? "" : csvEscape(val.toString()));
                }
                writer.write("\r\n");
            }
        }
        writer.flush();
        return baos.toByteArray();
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
