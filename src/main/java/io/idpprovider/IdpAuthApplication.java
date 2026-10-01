package io.idpprovider;

import io.idpprovider.properties.CorsProperties;
import io.idpprovider.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@EnableConfigurationProperties({
        CorsProperties.class,
        JwtProperties.class}
)@PropertySource(value = {
        "file:/app/config/application.yaml",
        "file:/app/config/secret.properties",
        "classpath:application.yaml",
}, ignoreResourceNotFound = true)
public class IdpAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdpAuthApplication.class, args);
    }

}