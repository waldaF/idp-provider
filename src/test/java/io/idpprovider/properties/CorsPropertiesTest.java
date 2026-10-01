package io.idpprovider.properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class TestConfig {}

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void whenValidConfiguration_shouldBindSuccessfully(final String description, final String[] properties) {
        contextRunner
                .withPropertyValues(properties)
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    static Stream<Arguments> whenValidConfiguration_shouldBindSuccessfully() {
        return Stream.of(
                Arguments.of("single URL (min boundary)", urls("http://localhost:5173")),
                Arguments.of("three URLs", urls("http://a.com", "http://b.com", "http://c.com")),
                Arguments.of("five URLs (max boundary)", urls("http://a.com", "http://b.com", "http://c.com", "http://d.com", "http://e.com"))
        );
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
                Arguments.of("urls missing", new String[]{}),
                Arguments.of("six URLs (exceeds max of 5)", urls("http://a.com", "http://b.com", "http://c.com", "http://d.com", "http://e.com", "http://f.com"))
        );
    }

    private static String[] urls(String... values) {
        final String[] props = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            props[i] = "cors.configuration.urls[" + i + "]=" + values[i];
        }
        return props;
    }
}