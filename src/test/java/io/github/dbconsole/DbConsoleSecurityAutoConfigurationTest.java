package io.github.dbconsole;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import io.github.dbconsole.autoconfigure.DbConsoleAutoConfiguration;
import io.github.dbconsole.autoconfigure.DbConsoleSecurityAutoConfiguration;

class DbConsoleSecurityAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DbConsoleAutoConfiguration.class,
                    DbConsoleSecurityAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver");

    @Test
    void noSecurityBeanByDefault() {
        contextRunner.run(ctx ->
                assertThat(ctx).doesNotHaveBean(WebSecurityCustomizer.class));
    }

    @Test
    void noSecurityBeanWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("db-console.websecurity.enabled=true")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(WebSecurityCustomizer.class));
    }

    @Test
    void securityBeanRegisteredWhenDisabled() {
        contextRunner
                .withPropertyValues("db-console.websecurity.enabled=false")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(WebSecurityCustomizer.class));
    }

    @Test
    void securityBeanUsesCustomPath() {
        contextRunner
                .withPropertyValues(
                        "db-console.path=/my-console",
                        "db-console.websecurity.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(WebSecurityCustomizer.class);
                    // Verify the path property is correct
                    assertThat(ctx.getBean(
                            io.github.dbconsole.autoconfigure.DbConsoleProperties.class).getPath())
                            .isEqualTo("/my-console");
                });
    }
}

