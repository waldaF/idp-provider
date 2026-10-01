package io.idpprovider.controller;

import io.idpprovider.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtProperties jwtProperties;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {
        final List<Map<String, Object>> keys = new ArrayList<>();

        if (Objects.nonNull(jwtProperties.getFrontend())) {
            jwtProperties.getFrontend().getAccess()
                    .forEach(k -> keys.add(createJwk((RSAPublicKey) k.getPublicKey(), k.getKeyId())));
        }

        jwtProperties.getServices().values()
                .forEach(svc -> svc.getAccess()
                        .forEach(k -> keys.add(createJwk((RSAPublicKey) k.getPublicKey(), k.getKeyId()))));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(Map.of("keys", keys));
    }

    private Map<String, Object> createJwk(final RSAPublicKey publicKey, final String kid) {
        return Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", kid,
                "n", base64UrlEncode(publicKey.getModulus().toByteArray()),
                "e", base64UrlEncode(publicKey.getPublicExponent().toByteArray())
        );
    }

    private String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}