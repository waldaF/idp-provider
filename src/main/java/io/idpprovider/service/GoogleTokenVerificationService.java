package io.idpprovider.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import io.idpprovider.error.ErrorCode;
import io.idpprovider.error.GoogleVerifierException;
import io.idpprovider.properties.GoogleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
public class GoogleTokenVerificationService {

    private final GoogleProperties googleProperties;
    private final GoogleIdTokenVerifier verifier;

    public String verifyTokenAndRelatedToDomain(final String token) {
        try {
            final GoogleIdToken idToken = verifier.verify(token);
            if (Objects.isNull(idToken)) {
                throw new GoogleVerifierException(ErrorCode.UNAUTHORIZED, "Invalid token");
            }

            final GoogleIdToken.Payload payload = idToken.getPayload();
            final String email = payload.getEmail();
            final String hostedDomain = payload.getHostedDomain();

            if (!googleProperties.getAllowedDomains().contains(hostedDomain)) {
                throw new GoogleVerifierException(ErrorCode.UNAUTHORIZED, "User domain not allowed");
            }

            if (!payload.getEmailVerified()) {
                throw new GoogleVerifierException(ErrorCode.UNAUTHORIZED, "Email not verified");
            }
            log.info("Token verified successfully for email: {}", email);
            return email;

        } catch (GoogleVerifierException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Google token verification", e);
            throw new GoogleVerifierException("Token verification failed", e);
        }
    }
}