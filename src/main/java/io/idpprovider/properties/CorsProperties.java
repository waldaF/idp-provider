package io.idpprovider.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RequiredArgsConstructor
@ConfigurationProperties(prefix = "cors.configuration")
@Validated
@Getter
public class CorsProperties {

    @NotNull
    @Size(min = 1, max = 5, message = "List size must be between 1 and 5")
    private final List<String> urls;
}