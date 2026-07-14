package io.github.dbconsole;

import io.github.dbconsole.autoconfigure.DbConsoleAutoConfiguration;
import io.github.dbconsole.controller.DbConsoleController;
import io.github.dbconsole.service.DataSourceRegistry;
import io.github.dbconsole.service.DatabaseService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DbConsoleAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DbConsoleAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver");

    @Test
    void allBeansRegistered() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(DataSourceRegistry.class);
            assertThat(ctx).hasSingleBean(DatabaseService.class);
            assertThat(ctx).hasSingleBean(DbConsoleController.class);
        });
    }

    @Test
    void disabledWhenPropertyFalse() {
        contextRunner
                .withPropertyValues("db-console.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(DbConsoleController.class);
                });
    }

    @Test
    void customPathIsRespected() {
        contextRunner
                .withPropertyValues("db-console.path=/my-console")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(DbConsoleController.class);
                    // DbConsoleProperties should have the custom path
                    assertThat(ctx.getBean(
                            io.github.dbconsole.autoconfigure.DbConsoleProperties.class).getPath())
                            .isEqualTo("/my-console");
                });
    }
}

