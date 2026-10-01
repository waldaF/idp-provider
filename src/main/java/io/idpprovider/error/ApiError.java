package io.idpprovider.error;

import java.util.List;
import java.util.Objects;

public record ApiError(
        ErrorCode errorCode,
        String message,
        List<String> details
) {
    public static ApiError of(final ErrorCode code, final String message, final List<String> details) {
        return new ApiError(code, message, Objects.isNull(details) ? List.of() : List.copyOf(details));
    }
    public static ApiError of(final ErrorCode code, final String message) {
        return new ApiError(code, message, List.of());
    }
}