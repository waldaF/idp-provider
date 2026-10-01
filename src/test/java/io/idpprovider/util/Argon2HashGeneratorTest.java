package io.idpprovider.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Slf4j
class Argon2HashGeneratorTest {

    private static final Argon2PasswordEncoder ENCODER = new Argon2PasswordEncoder(16, 32, 1, 24576, 1);

    @Test
    void generateHash() {
        String plaintext = "test";

        String hash = ENCODER.encode(plaintext);

        System.out.println("Service:  " + "example-service");
        System.out.println("Hash:     " + hash);
        System.out.println();
        System.out.println("Place this hash into application.yaml under:");
        System.out.println("  spring.flyway.placeholders.e2e_service_secret_hash");
        System.out.println("Or inject via env var:");
        System.out.println("  SPRING_FLYWAY_PLACEHOLDERS_E2E_SERVICE_SECRET_HASH=" + hash);
    }
}