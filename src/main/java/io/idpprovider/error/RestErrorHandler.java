package io.idpprovider.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RestErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(final MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        var code = ErrorCode.BAD_REQUEST;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(final ConstraintViolationException ex) {
        var details = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    int lastDot = path.lastIndexOf('.');
                    return (lastDot >= 0 ? path.substring(lastDot + 1) : path) + ": " + v.getMessage();
                })
                .toList();
        var code = ErrorCode.BAD_REQUEST;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "Validation failed", details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        var code = ErrorCode.BAD_REQUEST;
        var details = List.of(ex.getName() + ": invalid value");
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "Invalid request parameter", details));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(final HttpRequestMethodNotSupportedException ex) {
        var code = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "Method not allowed"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(final NoHandlerFoundException ex) {
        var code = ErrorCode.NOT_FOUND;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "Endpoint not found"));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiError> handleMissingCookie(final MissingRequestCookieException ex) {
        var code = ErrorCode.UNAUTHORIZED;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "Authentication required"));
    }

    @ExceptionHandler(JwtApiException.class)
    public ResponseEntity<ApiError> handleJwtApi(final JwtApiException ex) {
        log.warn("JWT authentication failure: {}", ex.getMessage());
        var code = ErrorCode.UNAUTHORIZED;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(final ApiException ex) {
        var code = ex.getErrorCode();
        if (code == ErrorCode.UNKNOWN || code == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("Unexpected application error [{}]: {}", code, ex.getMessage(), ex);
            return ResponseEntity
                    .status(code.getStatus())
                    .body(ApiError.of(code, "An unexpected error occurred. Please contact support."));
        }
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(final Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getClass().getSimpleName(), ex);
        var code = ErrorCode.UNKNOWN;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiError.of(code, "An unexpected error occurred. Please contact support."));
    }
}