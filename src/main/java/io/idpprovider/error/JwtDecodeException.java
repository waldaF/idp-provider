package io.idpprovider.error;

public class JwtDecodeException extends ApiException {
    public JwtDecodeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}