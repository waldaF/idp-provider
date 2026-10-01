package io.idpprovider.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.idpprovider.BaseIntegrationTest;
import io.idpprovider.domain.entity.Account;
import io.idpprovider.domain.entity.RefreshToken;
import io.idpprovider.domain.repository.AccountRepository;
import io.idpprovider.domain.repository.RefreshTokenRepository;
import io.idpprovider.error.ErrorCode;
import io.idpprovider.error.GoogleVerifierException;
import io.idpprovider.service.GoogleTokenVerificationService;
import io.idpprovider.service.JwtService;
import io.idpprovider.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthControllerIT extends BaseIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@example.com";

    @LocalServerPort
    private int port;

    @MockitoBean
    private GoogleTokenVerificationService googleTokenVerificationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        refreshTokenRepository.deleteAll();
    }

    @Test
    void authenticate_withValidGoogleToken_returns200WithCookiesAndBody() {
        when(googleTokenVerificationService.verifyTokenAndRelatedToDomain(anyString()))
                .thenReturn(ADMIN_EMAIL);

        final Account admin = accountRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        given()
                .contentType(ContentType.JSON)
                .body("{\"token\": \"any-google-token\"}")
                .when()
                .post("/api/v1/auth/authenticate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .cookie("access_token", notNullValue())
                .cookie("refresh_token", notNullValue())
                .body("email", is(ADMIN_EMAIL))
                .body("roles", hasItem("ADMIN"))
                .body("guid", is(admin.getGuid().toString()));
    }

    @Test
    void authenticate_whenAccountNotFound_returns401() {
        when(googleTokenVerificationService.verifyTokenAndRelatedToDomain(anyString()))
                .thenReturn("unknown@example.com");

        given()
                .contentType(ContentType.JSON)
                .body("{\"token\": \"any-google-token\"}")
                .when()
                .post("/api/v1/auth/authenticate")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void authenticate_whenGoogleVerificationFails_returns401() {
        when(googleTokenVerificationService.verifyTokenAndRelatedToDomain(anyString()))
                .thenThrow(new GoogleVerifierException(ErrorCode.UNAUTHORIZED, "Invalid token"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"token\": \"bad-token\"}")
                .when()
                .post("/api/v1/auth/authenticate")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void verify_withValidAccessToken_returns200() {
        final Account admin = accountRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        final String accessToken = jwtService.generateFrontendAccessToken(admin);

        given()
                .cookie("access_token", accessToken)
                .when()
                .post("/api/v1/auth/verify")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void verify_withNoToken_returns401() {
        given()
                .when()
                .post("/api/v1/auth/verify")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void verify_withInvalidToken_returns401() {
        given()
                .cookie("access_token", "not.a.valid.jwt")
                .when()
                .post("/api/v1/auth/verify")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void refresh_withValidRefreshToken_returns200WithNewCookies() {
        final Account admin = accountRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        final String refreshToken = jwtService.generateFrontendRefreshToken(admin);
        refreshTokenService.saveRefreshToken(admin, refreshToken, "test-agent");

        given()
                .cookie("refresh_token", refreshToken)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(HttpStatus.OK.value())
                .cookie("access_token", notNullValue())
                .cookie("refresh_token", notNullValue());
    }

    @Test
    void refresh_withRevokedToken_returns401WithClearedCookies() {
        final Account admin = accountRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        final String refreshToken = jwtService.generateFrontendRefreshToken(admin);
        refreshTokenService.saveRefreshToken(admin, refreshToken, "test-agent");

        refreshTokenRepository.findByTokenValue(refreshToken).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });

        given()
                .cookie("refresh_token", refreshToken)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .cookie("access_token", emptyString())
                .cookie("refresh_token", emptyString());
    }

    @Test
    void refresh_withMissingCookie_returns401() {
        given()
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }


    @Test
    void logout_withRefreshToken_returns204AndRevokesTokenAndClearsCookies() {
        final Account admin = accountRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        final String refreshToken = jwtService.generateFrontendRefreshToken(admin);
        refreshTokenService.saveRefreshToken(admin, refreshToken, "test-agent");

        given()
                .cookie("refresh_token", refreshToken)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .cookie("access_token", emptyString())
                .cookie("refresh_token", emptyString());

        final boolean revoked = refreshTokenRepository.findByTokenValue(refreshToken)
                .map(RefreshToken::isRevoked)
                .orElseThrow();
        assertTrue(revoked);
    }

    @Test
    void logout_withNoCookie_returns204WithClearedCookies() {
        given()
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .cookie("access_token", emptyString())
                .cookie("refresh_token", emptyString());
    }
}