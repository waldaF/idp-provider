package io.idpprovider.dto.response;

import java.util.List;

public record AuthResponse(String email, List<String> roles, String guid) {}