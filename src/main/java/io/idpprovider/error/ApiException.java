package io.idpprovider.error;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<String> details;

    public ApiException(final ErrorCode errorCode, final String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN;
        this.details = List.of();
    }

    public ApiException(final ErrorCode errorCode, final String message, final List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = Objects.isNull(details) ? List.of() : List.copyOf(details);
    }

}