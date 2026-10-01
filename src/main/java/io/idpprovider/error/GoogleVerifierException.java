package io.idpprovider.error;

public class GoogleVerifierException extends ApiException {

    public GoogleVerifierException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
    public GoogleVerifierException(final String message, final Throwable cause) {
        super(message, cause);
    }
}