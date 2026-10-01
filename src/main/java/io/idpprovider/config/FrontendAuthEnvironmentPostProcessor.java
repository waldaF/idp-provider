package io.idpprovider.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class FrontendAuthEnvironmentPostProcessor implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final String[] DB_AUTO_CONFIG_EXCLUDES = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    };

    @Override
    public void onApplicationEvent(final ApplicationEnvironmentPreparedEvent event) {
        final ConfigurableEnvironment environment = event.getEnvironment();
        final boolean frontendEnabled = Boolean.parseBoolean(
                environment.getProperty("auth.frontend.enabled", "false")
        );
        if (frontendEnabled) {
            return;
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource("disable-db-autoconfig",
                        Map.of("spring.autoconfigure.exclude", DB_AUTO_CONFIG_EXCLUDES))
        );
    }

    @Override
    public int getOrder() {
        // Must run after ConfigDataEnvironmentPostProcessor (which loads application.yaml) so that
        // "auth.frontend.enabled" is already visible here.
        return Ordered.LOWEST_PRECEDENCE;
    }
}