package io.idpprovider.util;

import io.idpprovider.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;

@RequiredArgsConstructor
public class CookieProvider {

    private final JwtProperties jwtProperties;

    public ResponseCookie createAccessTokenCookie(final String token) {
        return createCookie(token, jwtProperties.getFrontend().getLatestAccess());
    }

    public ResponseCookie createRefreshTokenCookie(final String token) {
        return createCookie(token, jwtProperties.getFrontend().getLatestRefresh());
    }

    public ResponseCookie deleteAccessTokenCookie() {
        var cookieConfig = jwtProperties.getFrontend().getLatestAccess().getCookie();
        return deleteCookie(cookieConfig.name(), cookieConfig.path());
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        var cookieConfig = jwtProperties.getFrontend().getLatestRefresh().getCookie();
        return deleteCookie(cookieConfig.name(), cookieConfig.path());
    }

    private ResponseCookie deleteCookie(final String name, final String path) {
        return ResponseCookie.from(name, "")
                .maxAge(0)
                .path(Objects.nonNull(path) ? path : "/")
                .build();
    }

    private ResponseCookie createCookie(final String token, final JwtProperties.TokenConfig config) {
        var duration = Duration.of(config.getExpiration(), config.getUnit());
        var cookie = config.getCookie();

        return ResponseCookie.from(cookie.name(), token)
                .httpOnly(cookie.httpOnly())
                .secure(cookie.secure())
                .path(cookie.path())
                .maxAge(duration)
                .sameSite(cookie.sameSite())
                .domain(StringUtils.hasText(cookie.domain()) ? cookie.domain() : null)
                .build();
    }
}