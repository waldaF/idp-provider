package io.idpprovider.dto.response;

import java.util.List;

public record ServiceAuthResponse(String serviceName, List<String> roles, String accessToken) {}