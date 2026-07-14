package io.github.dbconsole.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DB Console starter.
 *
 * <p>All properties are prefixed with {@code db-console}.
 *
 * <pre>
 * # application.properties / application.yml
 * db-console.enabled=true          # default: true
 * db-console.path=/db-console      # default: /db-console
 * db-console.max-rows=500          # default: 500  (hard cap for SELECT results)
 * db-console.exclude-datasources=  # comma-separated bean names to skip
 * </pre>
 */
@ConfigurationProperties(prefix = "db-console")
public class DbConsoleProperties {

    /** Set to {@code false} to completely disable the console endpoint. */
    private boolean enabled = true;

    /** URL path prefix for all DB Console endpoints. Must start with '/'. */
    private String path = "/db-console";

    /** Default maximum number of rows returned by SELECT queries. */
    private int maxRows = 500;

    /**
     * Comma-separated list of DataSource bean names that should be excluded
     * from the console (e.g. internal datasources used by Spring Batch).
     */
    private java.util.List<String> excludeDatasources = new java.util.ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPath() {
        // Ensure the path always starts with a slash and has no trailing slash
        String p = path == null ? "/db-console" : path.trim();
        if (!p.startsWith("/")) p = "/" + p;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }
    public void setPath(String path) { this.path = path; }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows > 0 ? maxRows : 500; }

    public java.util.List<String> getExcludeDatasources() { return excludeDatasources; }
    public void setExcludeDatasources(java.util.List<String> excludeDatasources) {
        this.excludeDatasources = excludeDatasources;
    }
}

