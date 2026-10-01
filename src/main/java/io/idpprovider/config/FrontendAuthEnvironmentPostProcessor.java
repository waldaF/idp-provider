package io.idpprovider.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class FrontendAuthEnvironmentPostProcessor implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final String[] DB_AUTO_CONFIG_EXCLUDES = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    };

    @Override
    public void onApplicationEvent(final ApplicationEnvironmentPreparedEvent event) {
        final ConfigurableEnvironment environment = event.getEnvironment();
        final boolean frontendEnabled = Boolean.parseBoolean(
                environment.getProperty("auth.frontend.enabled", "false")
        );
        if (!frontendEnabled) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("disable-db-autoconfig",
                            Map.of("spring.autoconfigure.exclude", DB_AUTO_CONFIG_EXCLUDES))
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}