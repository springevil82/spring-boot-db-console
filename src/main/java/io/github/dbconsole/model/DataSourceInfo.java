package io.github.dbconsole.model;

/**
 * Metadata about a registered DataSource.
 */
public class DataSourceInfo {

    private String name;
    private String url;
    private String driverClass;
    private String productName;
    private String productVersion;
    private boolean primary;

    public DataSourceInfo() {}

    public DataSourceInfo(String name, String url, String driverClass,
                          String productName, String productVersion, boolean primary) {
        this.name = name;
        this.url = url;
        this.driverClass = driverClass;
        this.productName = productName;
        this.productVersion = productVersion;
        this.primary = primary;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDriverClass() { return driverClass; }
    public void setDriverClass(String driverClass) { this.driverClass = driverClass; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}

