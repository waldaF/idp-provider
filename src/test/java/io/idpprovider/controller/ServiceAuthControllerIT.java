package io.idpprovider.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.idpprovider.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class ServiceAuthControllerIT extends BaseIntegrationTest {

    private static final String SERVICE_NAME = "backend-service";
    private static final String CLIENT_SECRET = "test";
    private static final String ENDPOINT = "/api/v1/auth/service/authenticate";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void authenticate_withValidCredentials_returns200WithTokenAndRoles() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"serviceName\": \"%s\", \"clientSecret\": \"%s\"}".formatted(SERVICE_NAME, CLIENT_SECRET))
                .when()
                .post(ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("serviceName", is(SERVICE_NAME))
                .body("roles", hasItem("READ"))
                .body("accessToken", notNullValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void authenticate_withInvalidCredentials_returns401(final String description, final String body) {
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    static Stream<Arguments> authenticate_withInvalidCredentials_returns401() {
        return Stream.of(
                Arguments.of("unknown service name",
                        "{\"serviceName\": \"unknown-service\", \"clientSecret\": \"%s\"}".formatted(CLIENT_SECRET)),
                Arguments.of("wrong client secret",
                        "{\"serviceName\": \"%s\", \"clientSecret\": \"wrong-secret\"}".formatted(SERVICE_NAME))
        );
    }
}