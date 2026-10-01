package io.idpprovider.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.idpprovider.properties.GoogleProperties;
import io.idpprovider.properties.JwtProperties;
import io.idpprovider.util.CookieProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class JwtAuthConfiguration {

    @Bean
    public CookieProvider cookieProvider(final JwtProperties jwtProperties) {
        return new CookieProvider(jwtProperties);
    }

    @Configuration
    @ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
    @EnableConfigurationProperties(GoogleProperties.class)
    static class FrontendAuthConfiguration {

        @Bean
        public GoogleIdTokenVerifier googleIdTokenVerifier(final GoogleProperties googleProperties) {
            return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(googleProperties.getClientId()))
                    .build();
        }
    }
}