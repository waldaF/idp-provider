package io.idpprovider.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RequiredArgsConstructor
@ConfigurationProperties(prefix = "google.configuration")
@Validated
@Getter
public class GoogleProperties {

    @NotBlank
    @Size(min = 50, max = 200, message = "Size must be between 50 - 200")
    private final String clientId;
    @NotNull
    @Size(min = 1, message = "At least one domain must be provided")
    private final List<String> allowedDomains;
}