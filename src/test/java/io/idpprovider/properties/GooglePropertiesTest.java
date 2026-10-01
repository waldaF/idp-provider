package io.idpprovider.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GooglePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    private final ApplicationContextRunner conditionalContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalTestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GoogleProperties.class)
    static class TestConfig {}

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
    @EnableConfigurationProperties(GoogleProperties.class)
    static class ConditionalTestConfig {}

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void whenValidConfiguration_shouldBindSuccessfully(final String description, final String[] properties) {
        contextRunner
                .withPropertyValues(properties)
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    static Stream<Arguments> whenValidConfiguration_shouldBindSuccessfully() {
        return Stream.of(
                Arguments.of("min clientId length (50) + single domain", combine(clientId(50), domains("example.com"))),
                Arguments.of("mid clientId length (100) + multiple domains", combine(clientId(100), domains("example.com", "example.org"))),
                Arguments.of("max clientId length (200)", combine(clientId(200), domains("example.com")))
        );
    }

    @Test
    void whenFrontendDisabled_googlePropertiesShouldNotBeRequired() {
        conditionalContextRunner
                .withPropertyValues("auth.frontend.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(GoogleProperties.class);
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void whenInvalidConfiguration_shouldFailValidation(final String description, final String[] properties) {
        contextRunner
                .withPropertyValues(properties)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    static Stream<Arguments> whenInvalidConfiguration_shouldFailValidation() {
        return Stream.of(
                Arguments.of("clientId missing", domains("example.com")),
                Arguments.of("clientId too short (49)", combine(clientId(49), domains("example.com"))),
                Arguments.of("clientId too long (201)", combine(clientId(201), domains("example.com"))),
                Arguments.of("domains missing", clientId(50))
        );
    }

    private static String[] clientId(final int length) {
        return new String[]{"google.configuration.client-id=" + "a".repeat(length)};
    }

    private static String[] domains(final String... values) {
        final String[] props = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            props[i] = "google.configuration.allowed-domains[" + i + "]=" + values[i];
        }
        return props;
    }

    private static String[] combine(final String[]... groups) {
        return Arrays.stream(groups)
                .flatMap(Arrays::stream)
                .toArray(String[]::new);
    }
}