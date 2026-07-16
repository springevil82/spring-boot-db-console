package io.github.dbconsole.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Auto-configuration that bypasses Spring Security for all DB Console endpoints
 * when {@code db-console.websecurity.enabled=false}.
 *
 * <p>This configuration is activated only when:
 * <ul>
 *   <li>{@code spring-security-config} is on the classpath.</li>
 *   <li>{@code db-console.websecurity.enabled} is explicitly set to {@code false}.</li>
 * </ul>
 *
 * <p>When active, a {@link WebSecurityCustomizer} bean is registered that instructs
 * Spring Security to ignore all requests matching {@code <path>/**}, where
 * {@code <path>} is the configured {@code db-console.path} (default: {@code /db-console}).
 *
 * <p>Example usage in {@code application.properties}:
 * <pre>
 * db-console.websecurity.enabled=false
 * </pre>
 * or in {@code application.yml}:
 * <pre>
 * db-console:
 *   websecurity:
 *     enabled: false
 * </pre>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebSecurityCustomizer.class)
@ConditionalOnProperty(prefix = "db-console.websecurity", name = "enabled", havingValue = "false")
@EnableConfigurationProperties(DbConsoleProperties.class)
@AutoConfigureAfter(DbConsoleAutoConfiguration.class)
public class DbConsoleSecurityAutoConfiguration {

    /**
     * Registers a {@link WebSecurityCustomizer} that tells Spring Security to ignore
     * (completely bypass) all requests to the DB Console path.
     *
     * <p>Works correctly even when the app is deployed behind a context prefix
     * (e.g., {@code /salesforce-broker/db-console/**}), because the pattern uses
     * wildcards to match any prefix before the console path.
     *
     * <p>This is equivalent to adding the following to your own {@code SecurityConfig}:
     * <pre>
     * web.ignoring().requestMatchers(new AntPathRequestMatcher("/**" + properties.getPath() + "/**"));
     * </pre>
     */
    @Bean
    public WebSecurityCustomizer dbConsoleWebSecurityCustomizer(DbConsoleProperties properties) {
        String pattern = "/**" + properties.getPath() + "/**";
        return web -> web.ignoring().requestMatchers(new AntPathRequestMatcher(pattern));
    }
}

