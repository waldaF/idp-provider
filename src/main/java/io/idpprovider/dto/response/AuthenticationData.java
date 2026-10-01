package io.idpprovider.dto.response;

public record AuthenticationData(String accessToken, String refreshToken, AuthResponse responseBody) {}