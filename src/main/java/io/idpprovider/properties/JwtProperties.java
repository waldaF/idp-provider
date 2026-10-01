package io.idpprovider.properties;

import io.idpprovider.error.ErrorCode;
import io.idpprovider.error.JwtDecodeException;
import io.idpprovider.error.JwtApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    @NotBlank
    private final String issuer;
    @Valid
    private final FrontendConfig frontend;
    private final Map<String, ServiceConfig> services;

    @ConstructorBinding
    public JwtProperties(@NotBlank final String issuer,
                         @Valid final FrontendConfig frontend,
                         final Map<String, ServiceConfig> services) {
        this.issuer = issuer;
        this.frontend = frontend;
        this.services = Objects.nonNull(services) ? Map.copyOf(services) : Map.of();
    }

    @Getter
    public static class FrontendConfig {
        @NotEmpty
        private final List<String> audience;
        @NotEmpty
        private final List<TokenConfig> access;
        @NotEmpty
        private final List<TokenConfig> refresh;

        @ConstructorBinding
        public FrontendConfig(@NotEmpty final List<String> audience,
                              @NotEmpty final List<TokenConfig> access,
                              @NotEmpty final List<TokenConfig> refresh) {
            this.audience = audience;
            this.access = access;
            this.refresh = refresh;
        }

        public TokenConfig getLatestAccess() {
            return access.getLast();
        }

        public TokenConfig getLatestRefresh() {
            return refresh.getLast();
        }
    }

    @Getter
    public static class ServiceConfig {
        @NotEmpty
        private final List<String> audience;
        @NotEmpty
        private final List<String> roles;
        @NotBlank
        private final String clientSecretHash;
        @NotEmpty
        private final List<TokenConfig> access;

        @ConstructorBinding
        public ServiceConfig(@NotEmpty final List<String> audience,
                             @NotEmpty final List<String> roles,
                             @NotBlank final String clientSecretHash,
                             @NotEmpty final List<TokenConfig> access) {
            this.audience = audience;
            this.roles = roles;
            this.clientSecretHash = clientSecretHash;
            this.access = access;
        }

        public TokenConfig getLatestAccess() {
            return access.getLast();
        }
    }

    @Getter
    public static class TokenConfig {
        // null for verification-only (legacy) entries
        private final PrivateKey privateKey;
        @NotNull
        private final PublicKey publicKey;
        @NotBlank
        private final String keyId;
        private final long expiration;
        private final ChronoUnit unit;
        private final CookieConfig cookie;

        @ConstructorBinding
        public TokenConfig(final String privateKeyPem,
                           @NotBlank final String publicKeyPem,
                           @NotBlank final String keyId,
                           final long expiration,
                           final ChronoUnit unit,
                           final CookieConfig cookie) {
            try {
                final KeyFactory kf = KeyFactory.getInstance("RSA");
                this.privateKey = StringUtils.hasText(privateKeyPem)
                        ? kf.generatePrivate(new PKCS8EncodedKeySpec(decode(privateKeyPem)))
                        : null;
                this.publicKey = kf.generatePublic(new X509EncodedKeySpec(decode(publicKeyPem)));
                this.keyId = keyId;
                this.expiration = expiration;
                this.unit = unit;
                this.cookie = cookie;
            } catch (Exception e) {
                throw new JwtApiException("Failed to initialize RSA keys. Check if keys are valid Base64.", e);
            }
        }

        private byte[] decode(final String base64) {
            if (!StringUtils.hasText(base64)) {
                throw new JwtDecodeException(ErrorCode.INTERNAL_SERVER_ERROR, "JWT Key content is missing!");
            }
            return DECODER.decode(base64.trim());
        }

        public record CookieConfig(String name,
                                   boolean httpOnly,
                                   boolean secure,
                                   String path,
                                   String sameSite,
                                   String domain) {

            @ConstructorBinding
            public CookieConfig(@NotBlank final String name,
                                final boolean httpOnly,
                                final boolean secure,
                                @NotBlank final String path,
                                @NotBlank final String sameSite,
                                final String domain
            ) {
                this.name = name;
                this.httpOnly = httpOnly;
                this.secure = secure;
                this.path = path;
                this.sameSite = sameSite;
                this.domain = domain;
            }
        }
    }
}