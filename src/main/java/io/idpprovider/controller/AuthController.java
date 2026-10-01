package io.idpprovider.controller;

import io.idpprovider.dto.request.GoogleAuthRequest;
import io.idpprovider.dto.response.AuthResponse;
import io.idpprovider.dto.response.AuthenticationData;
import io.idpprovider.service.AuthenticationService;
import io.idpprovider.util.CookieProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "api/v1/auth")
@ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;
    private final CookieProvider cookieProvider;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                                     @RequestBody final GoogleAuthRequest request) {
        final AuthenticationData authData = authenticationService.authenticateWithGoogle(request.token(), userAgent);

        final String accessCookie = cookieProvider.createAccessTokenCookie(authData.accessToken()).toString();
        final String refreshCookie = cookieProvider.createRefreshTokenCookie(authData.refreshToken()).toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie)
                .header(HttpHeaders.SET_COOKIE, refreshCookie)
                .body(authData.responseBody());
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@CookieValue(name = "access_token", required = false) final String token) {
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authenticationService.verifyAccessToken(token);
        return ResponseEntity.ok().build();

    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                        @CookieValue(name = "refresh_token") final String refreshToken) {
        try {

            final AuthenticationData authData = authenticationService.refresh(refreshToken, userAgent);
            final String accessCookie = cookieProvider.createAccessTokenCookie(authData.accessToken()).toString();
            final String refreshCookie = cookieProvider.createRefreshTokenCookie(authData.refreshToken()).toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie)
                    .header(HttpHeaders.SET_COOKIE, refreshCookie)
                    .build();
        } catch (Exception e) {
            final String clearAccess = cookieProvider.deleteAccessTokenCookie().toString();
            final String clearRefresh = cookieProvider.deleteRefreshTokenCookie().toString();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearAccess)
                    .header(HttpHeaders.SET_COOKIE, clearRefresh)
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) final String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            authenticationService.logout(refreshToken);
        }
        final String clearAccess = cookieProvider.deleteAccessTokenCookie().toString();
        final String clearRefresh = cookieProvider.deleteRefreshTokenCookie().toString();

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, clearAccess)
                .header(HttpHeaders.SET_COOKIE, clearRefresh)
                .build();
    }
}