package io.idpprovider.error;

public class JwtApiException extends ApiException {
    public JwtApiException(final String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public JwtApiException(final String message, final Throwable cause) {
        super(message, cause);
    }

}