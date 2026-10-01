package io.idpprovider.service;

import io.jsonwebtoken.Claims;
import io.idpprovider.domain.entity.Account;
import io.idpprovider.domain.entity.RefreshToken;
import io.idpprovider.domain.repository.RefreshTokenRepository;
import io.idpprovider.dto.response.AuthResponse;
import io.idpprovider.dto.response.AuthenticationData;
import io.idpprovider.error.JwtApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final GoogleTokenVerificationService verificationService;
    private final AccountService accountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public void verifyAccessToken(final String accessToken) {
        jwtService.verifyAccessToken(accessToken);
    }

    @Transactional
    public AuthenticationData authenticateWithGoogle(final String googleToken, final String userAgent) {
        final String email = verificationService.verifyTokenAndRelatedToDomain(googleToken);

        final Account account = accountService.findByEmail(email)
                .orElseThrow(() -> new JwtApiException("Account not found"));

        final String accessToken = jwtService.generateFrontendAccessToken(account);
        final String refreshToken = jwtService.generateFrontendRefreshToken(account);

        refreshTokenService.saveRefreshToken(account, refreshToken, userAgent);

        return new AuthenticationData(
                accessToken,
                refreshToken,
                toAuthResponse(account)
        );
    }

    @Transactional
    public void logout(final String refreshTokenValue) {
        refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setUpdatedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(token);
                    log.info("Token revoked for user: {}", token.getAccount().getEmail());
                });
    }

    @Transactional
    public AuthenticationData refresh(final String refreshTokenValue, final String userAgent) {
        final Claims claims = jwtService.verifyRefreshToken(refreshTokenValue);
        final String email = claims.getSubject();

        final Account account = accountService.findByEmail(email)
                .orElseThrow(() -> new JwtApiException("Account not found"));

        final RefreshToken stored = refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> new JwtApiException("Refresh token not found"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new JwtApiException("Refresh token invalid or expired");
        }
        stored.setRevoked(true);
        stored.setUpdatedAt(OffsetDateTime.now());
        refreshTokenRepository.save(stored);

        final String newAccessToken = jwtService.generateFrontendAccessToken(account);
        final String newRefreshTokenValue = jwtService.generateFrontendRefreshToken(account);
        refreshTokenService.saveRefreshToken(account, newRefreshTokenValue, userAgent);
        return new AuthenticationData(newAccessToken, newRefreshTokenValue, toAuthResponse(account));
    }

    private AuthResponse toAuthResponse(final Account account) {
        return new AuthResponse(
                account.getEmail(),
                account.getRoles().stream().map(Enum::name).toList(),
                account.getGuid().toString()
        );
    }
}