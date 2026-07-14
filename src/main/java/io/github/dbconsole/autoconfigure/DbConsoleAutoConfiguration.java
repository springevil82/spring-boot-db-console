package io.github.dbconsole.autoconfigure;

import io.github.dbconsole.controller.DbConsoleController;
import io.github.dbconsole.service.DataSourceRegistry;
import io.github.dbconsole.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot auto-configuration for the DB Console starter.
 *
 * <p>Activates automatically when:
 * <ul>
 *   <li>A servlet-based web application is detected.</li>
 *   <li>At least one {@link DataSource} bean is on the classpath.</li>
 *   <li>The property {@code db-console.enabled=true} is explicitly provided.</li>
 * </ul>
 *
 * <p>The console is accessible at {@code /db-console} by default (configurable
 * via {@code db-console.path}).
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "db-console", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DbConsoleProperties.class)
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration"
})
public class DbConsoleAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbConsoleAutoConfiguration.class);

    /**
     * Collects all {@link DataSource} beans, filtering out any explicitly excluded by name.
     *
     * <p>Spring auto-populates the {@code Map<String, DataSource>} parameter with all
     * DataSource beans keyed by their bean name.
     */
    @Bean
    @ConditionalOnMissingBean(DataSourceRegistry.class)
    public DataSourceRegistry dbConsoleDataSourceRegistry(
            Map<String, DataSource> allDataSources,
            ObjectProvider<DataSource> primaryDataSource,
            DbConsoleProperties properties) {

        Map<String, DataSource> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, DataSource> entry : allDataSources.entrySet()) {
            if (!properties.getExcludeDatasources().contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        if (filtered.isEmpty()) {
            log.warn("DB Console: no DataSource beans found in the application context. "
                    + "The console will be available but without any datasource.");
        } else {
            log.info("DB Console: registered {} datasource(s): {}", filtered.size(), filtered.keySet());
        }

        return new DataSourceRegistry(filtered, primaryDataSource.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(DatabaseService.class)
    public DatabaseService dbConsoleDatabaseService(DataSourceRegistry registry) {
        return new DatabaseService(registry);
    }

    @Bean
    @ConditionalOnMissingBean(DbConsoleController.class)
    public DbConsoleController dbConsoleController(DatabaseService databaseService,
                                                    DbConsoleProperties properties) {
        log.info("DB Console UI available at: {}", properties.getPath());
        return new DbConsoleController(databaseService, properties);
    }
}
