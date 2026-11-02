package in.ashish.combined.exception;

import in.ashish.combined.dto.ErrorResponse;
import in.ashish.combined.filter.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized Global Exception Handler for Banking Transaction Management System.
 *
 * Interview Concepts:
 * - @RestControllerAdvice: Catches exceptions across all controllers and serializes uniform JSON error envelopes.
 * - HTTP 4xx vs 5xx Semantics:
 *     * 4xx indicates Client Errors (bad input, missing accounts, insufficient funds). Logged at WARN level.
 *     * 5xx indicates Internal Server Errors (unhandled exceptions, system failures). Logged at ERROR level with full stack trace.
 * - Root-Cause Analysis via Request Correlation: Every error envelope includes a `requestId` tracked in SLF4J MDC,
 *   allowing engineering teams to locate the exact log line and stack trace within seconds.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.warn("Duplicate resource violation: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(
            InsufficientBalanceException ex,
            HttpServletRequest request) {

        log.warn("Insufficient balance: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactiveException(
            AccountInactiveException ex,
            HttpServletRequest request) {

        log.warn("Account inactive violation: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransferException(
            InvalidTransferException ex,
            HttpServletRequest request) {

        log.warn("Invalid transfer request: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed for request: {} | Errors count: {}",
                request.getRequestURI(), ex.getBindingResult().getErrorCount());

        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Input validation failed. Please check field-level errors.",
                request.getRequestURI(),
                validationErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed JSON request received: {} | {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request body. Please verify JSON syntax and data types.",
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' should be of type '%s'", paramName, expectedType);

        log.warn("Type mismatch on request: {} | {}", request.getRequestURI(), message);
        ErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Unsupported media type on request: {} | {}", request.getRequestURI(), ex.getMessage());
        String message = String.format("Content-Type '%s' is not supported. Please send request body as 'application/json'.", ex.getContentType());
        ErrorResponse response = buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("HTTP method not supported: {} | {}", request.getRequestURI(), ex.getMessage());
        String message = String.format("HTTP Method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        ErrorResponse response = buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message, request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Database constraint violation on request: {} | {}", request.getRequestURI(), ex.getMessage());
        String message = "Database constraint violation. A record with identical unique details (such as account number or email) already exists.";
        ErrorResponse response = buildErrorResponse(HttpStatus.CONFLICT, message, request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied: {} | Request Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse response = buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: You do not have permission to access this resource", request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception caught on path: {}", request.getRequestURI(), ex);
        String message = "An unexpected error occurred. Please try again later or contact support.";
        ErrorResponse response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request.getRequestURI(), null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors) {

        String requestId = MDC.get(RequestCorrelationFilter.MDC_KEY);

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .requestId(requestId)
                .validationErrors(validationErrors)
                .build();
    }
}
