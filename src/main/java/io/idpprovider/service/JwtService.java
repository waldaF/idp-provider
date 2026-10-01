package io.idpprovider.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.idpprovider.domain.entity.Account;
import io.idpprovider.error.JwtApiException;
import io.idpprovider.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Map<String, PublicKey> frontendAccessKeyMap;
    private final Map<String, PublicKey> frontendRefreshKeyMap;
    // Unified key map for verification — covers frontend access and all service access keys.
    // Refresh keys are intentionally excluded to prevent refresh tokens from being accepted as access tokens.
    private final Map<String, PublicKey> allAccessPublicKeys;

    public JwtService(final JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        final JwtProperties.FrontendConfig frontend = jwtProperties.getFrontend();
        this.frontendAccessKeyMap = Objects.nonNull(frontend) ? buildKeyMap(frontend.getAccess()) : Map.of();
        this.frontendRefreshKeyMap = Objects.nonNull(frontend) ? buildKeyMap(frontend.getRefresh()) : Map.of();

        final Map<String, PublicKey> all = new HashMap<>(frontendAccessKeyMap);
        jwtProperties.getServices().values()
                .forEach(svc -> all.putAll(buildKeyMap(svc.getAccess())));
        this.allAccessPublicKeys = Map.copyOf(all);
    }

    public Claims verifyAccessToken(final String token) {
        return verifyToken(token, allAccessPublicKeys);
    }

    public Claims verifyRefreshToken(final String token) {
        return verifyToken(token, frontendRefreshKeyMap);
    }

    public String generateFrontendAccessToken(final Account account) {
        final JwtProperties.FrontendConfig frontend = jwtProperties.getFrontend();
        return createToken(account.getEmail(), frontend.getLatestAccess(), frontend.getAudience(), builder -> builder
                .claim("guid", account.getGuid().toString())
                .claim("roles", account.getRoles().stream().map(Enum::name).toList()));
    }

    public String generateFrontendRefreshToken(final Account account) {
        final JwtProperties.FrontendConfig frontend = jwtProperties.getFrontend();
        return createToken(account.getEmail(), frontend.getLatestRefresh(), frontend.getAudience(), builder -> builder
                .id(UUID.randomUUID().toString()));
    }

    public String generateServiceAccessToken(final String serviceName) {
        final JwtProperties.ServiceConfig config = Optional.ofNullable(jwtProperties.getServices().get(serviceName))
                .orElseThrow(() -> new JwtApiException("Invalid credentials"));
        return createToken(serviceName, config.getLatestAccess(), config.getAudience(), builder -> builder
                .claim("roles", config.getRoles()));
    }

    private String createToken(final String subject,
                               final JwtProperties.TokenConfig config,
                               final List<String> audience,
                               final Consumer<JwtBuilder> customizer) {
        final Instant now = Instant.now();
        final Instant expiry = now.plus(config.getExpiration(), config.getUnit());

        final JwtBuilder builder = Jwts.builder()
                .header()
                .add("kid", config.getKeyId())
                .and()
                .issuer(jwtProperties.getIssuer())
                .audience().add(audience).and()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(config.getPrivateKey(), Jwts.SIG.RS256);

        customizer.accept(builder);
        return builder.compact();
    }

    private Claims verifyToken(final String token, final Map<String, PublicKey> keyMap) {
        try {
            return Jwts.parser()
                    .keyLocator(header -> keyMap.get((String) header.get("kid")))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            final Claims expiredClaims = e.getClaims();
            final String userEmail = expiredClaims.getSubject();
            log.warn("User {} tried to use an expired token", userEmail);
            throw new JwtApiException("Token has expired", e);
        } catch (JwtException e) {
            throw new JwtApiException("JWT verification failed", e);
        }
    }

    private static Map<String, PublicKey> buildKeyMap(final List<JwtProperties.TokenConfig> configs) {
        return configs.stream()
                .collect(Collectors.toUnmodifiableMap(
                        JwtProperties.TokenConfig::getKeyId,
                        JwtProperties.TokenConfig::getPublicKey
                ));
    }
}