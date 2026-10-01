package io.idpprovider.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.idpprovider.BaseIntegrationTest;
import io.idpprovider.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class JwksControllerIT extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void getJwks_returnsAllAccessKeysWithCorrectStructure() {
        given()
                .when()
                .get("/.well-known/jwks.json")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .header("Cache-Control", containsString("max-age=3600"))
                .header("Cache-Control", containsString("public"))
                .body("keys", hasSize(totalAccessKeyCount()))
                .body("keys[0].kty", is("RSA"))
                .body("keys[0].use", is("sig"))
                .body("keys[0].alg", is("RS256"))
                .body("keys[0].kid", is(jwtProperties.getFrontend().getAccess().getFirst().getKeyId()))
                .body("keys[0].n", notNullValue())
                .body("keys[0].e", notNullValue());
    }

    @Test
    void getJwks_keyModulusAndExponentMatchConfiguredPublicKey() {
        final JwtProperties.TokenConfig config = jwtProperties.getFrontend().getAccess().getFirst();
        final RSAPublicKey pub = (RSAPublicKey) config.getPublicKey();
        final Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        final String expectedN = enc.encodeToString(pub.getModulus().toByteArray());
        final String expectedE = enc.encodeToString(pub.getPublicExponent().toByteArray());

        given()
                .when()
                .get("/.well-known/jwks.json")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("keys[0].n", is(expectedN))
                .body("keys[0].e", is(expectedE));
    }

    private int totalAccessKeyCount() {
        return jwtProperties.getFrontend().getAccess().size()
                + jwtProperties.getServices().values().stream()
                        .mapToInt(s -> s.getAccess().size()).sum();
    }

    @Test
    void getJwks_isAccessibleWithoutAuthentication() {
        given()
                .when()
                .get("/.well-known/jwks.json")
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}