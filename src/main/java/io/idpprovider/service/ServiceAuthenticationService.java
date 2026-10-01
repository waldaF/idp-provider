package io.idpprovider.service;

import io.idpprovider.dto.response.ServiceAuthResponse;
import io.idpprovider.error.JwtApiException;
import io.idpprovider.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAuthenticationService {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public ServiceAuthResponse authenticate(final String serviceName, final String clientSecret) {
        final JwtProperties.ServiceConfig config = Optional.ofNullable(jwtProperties.getServices().get(serviceName))
                .orElseThrow(() -> new JwtApiException("Invalid credentials"));

        if (!passwordEncoder.matches(clientSecret, config.getClientSecretHash())) {
            throw new JwtApiException("Invalid credentials");
        }

        final String accessToken = jwtService.generateServiceAccessToken(serviceName);
        log.info("Service account authenticated: {}", serviceName);

        return new ServiceAuthResponse(
                serviceName,
                config.getRoles(),
                accessToken
        );
    }
}