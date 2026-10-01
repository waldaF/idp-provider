package io.idpprovider.properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    private static final String[] VALID_ISSUER = {"jwt.issuer=test-issuer"};
    private static final String[] VALID_AUDIENCE = {"jwt.frontend.audience[0]=example-service"};
    private static final String[] VALID_ACCESS;
    private static final String[] VALID_REFRESH;

    static {
        try {
            // 1024-bit keys — insecure, but fast enough for unit tests
            final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            final Base64.Encoder enc = Base64.getEncoder();

            final KeyPair accessPair = gen.generateKeyPair();
            final KeyPair refreshPair = gen.generateKeyPair();

            VALID_ACCESS = frontendTokenEntry("access", 0,
                    enc.encodeToString(accessPair.getPrivate().getEncoded()),
                    enc.encodeToString(accessPair.getPublic().getEncoded()));
            VALID_REFRESH = frontendTokenEntry("refresh", 0,
                    enc.encodeToString(refreshPair.getPrivate().getEncoded()),
                    enc.encodeToString(refreshPair.getPublic().getEncoded()));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
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
                Arguments.of("single access and refresh key", validBase()),
                Arguments.of("two access keys (rotation)", combine(VALID_ISSUER, VALID_AUDIENCE, VALID_ACCESS, rotationEntry(), VALID_REFRESH)),
                Arguments.of("with services map", combine(validBase(), serviceEntry())),
                Arguments.of("no frontend (service-only mode)", combine(VALID_ISSUER, serviceEntry()))
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
                Arguments.of("issuer blank", combine(VALID_AUDIENCE, VALID_ACCESS, VALID_REFRESH)),
                Arguments.of("frontend audience empty", combine(VALID_ISSUER, VALID_ACCESS, VALID_REFRESH)),
                Arguments.of("frontend access list empty", combine(VALID_ISSUER, VALID_AUDIENCE, VALID_REFRESH)),
                Arguments.of("frontend refresh list empty", combine(VALID_ISSUER, VALID_AUDIENCE, VALID_ACCESS))
        );
    }

    private static String[] validBase() {
        return combine(VALID_ISSUER, VALID_AUDIENCE, VALID_ACCESS, VALID_REFRESH);
    }

    private static String[] rotationEntry() {
        final String publicKeyPem = Arrays.stream(VALID_ACCESS)
                .filter(p -> p.contains(".public-key-pem="))
                .map(p -> p.split("=", 2)[1])
                .findFirst().orElseThrow();
        final String privateKeyPem = Arrays.stream(VALID_ACCESS)
                .filter(p -> p.contains(".private-key-pem="))
                .map(p -> p.split("=", 2)[1])
                .findFirst().orElseThrow();
        return frontendTokenEntry("access", 1, privateKeyPem, publicKeyPem);
    }

    private static String[] serviceEntry() {
        try {
            final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            final Base64.Encoder enc = Base64.getEncoder();
            final KeyPair pair = gen.generateKeyPair();
            final String prefix = "jwt.services.test-service.access[0]";
            return new String[]{
                    "jwt.services.test-service.audience[0]=some-target",
                    "jwt.services.test-service.roles[0]=READ",
                    "jwt.services.test-service.client-secret-hash=some-hash",
                    prefix + ".key-id=test-service-access-v1",
                    prefix + ".private-key-pem=" + enc.encodeToString(pair.getPrivate().getEncoded()),
                    prefix + ".public-key-pem=" + enc.encodeToString(pair.getPublic().getEncoded()),
                    prefix + ".expiration=1",
                    prefix + ".unit=HOURS"
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] frontendTokenEntry(final String type, final int idx,
                                               final String privateKeyPem, final String publicKeyPem) {
        final String prefix = "jwt.frontend." + type + "[" + idx + "]";
        return new String[]{
                prefix + ".key-id=test-" + type + "-v" + (idx + 1),
                prefix + ".private-key-pem=" + privateKeyPem,
                prefix + ".public-key-pem=" + publicKeyPem,
                prefix + ".expiration=30",
                prefix + ".unit=MINUTES",
                prefix + ".cookie.name=" + type + "_token",
                prefix + ".cookie.http-only=true",
                prefix + ".cookie.secure=false",
                prefix + ".cookie.path=/",
                prefix + ".cookie.same-site=Strict"
        };
    }

    private static String[] combine(final String[]... groups) {
        return Arrays.stream(groups)
                .flatMap(Arrays::stream)
                .toArray(String[]::new);
    }
}