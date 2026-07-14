package io.github.dbconsole.service;

import javax.sql.DataSource;
import java.util.*;

/**
 * Holds all DataSource beans discovered in the application context.
 * Spring auto-injects all {@link DataSource} beans as a map keyed by bean name.
 */
public class DataSourceRegistry {

    /** Bean-name → DataSource mapping collected by Spring. */
    private final Map<String, DataSource> dataSources;

    /** The @Primary DataSource (may be null). */
    private final DataSource primaryDataSource;

    public DataSourceRegistry(Map<String, DataSource> dataSources, DataSource primaryDataSource) {
        this.dataSources = Collections.unmodifiableMap(new LinkedHashMap<>(dataSources));
        this.primaryDataSource = primaryDataSource;
    }

    /** Returns all registered datasource names (bean names), in insertion order. */
    public Set<String> getNames() {
        return dataSources.keySet();
    }

    /**
     * Returns the DataSource for the given bean name, or {@code null} if not found.
     */
    public DataSource get(String name) {
        return dataSources.get(name);
    }

    /**
     * Returns all datasources as an unmodifiable map (bean name → DataSource).
     */
    public Map<String, DataSource> getAll() {
        return dataSources;
    }

    /**
     * Returns {@code true} if the given datasource bean is the @Primary one.
     */
    public boolean isPrimary(String name) {
        DataSource ds = dataSources.get(name);
        return ds != null && ds == primaryDataSource;
    }

    public boolean isEmpty() {
        return dataSources.isEmpty();
    }
}

