package io.idpprovider.service;

import io.idpprovider.domain.entity.Account;
import io.idpprovider.domain.entity.RefreshToken;
import io.idpprovider.properties.JwtProperties;
import io.idpprovider.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public void saveRefreshToken(final Account account,
                                 final String tokenValue,
                                 final String userAgent) {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setAccount(account);
        refreshToken.setTokenValue(tokenValue);
        refreshToken.setExpiresAt(calculateExpiration());
        refreshToken.setUserAgent(userAgent);
        refreshTokenRepository.save(refreshToken);
    }

    private OffsetDateTime calculateExpiration() {
        final var config = jwtProperties.getFrontend().getLatestRefresh();
        return OffsetDateTime.now().plus(config.getExpiration(), config.getUnit());
    }
}