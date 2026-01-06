package com.ngoctran.interactionservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);
        log.warn("Invalid argument - CorrelationId: {} - Message: {}", correlationId, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .correlationId(correlationId)
                .code("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .type("CLIENT_ERROR")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);
        log.warn("Invalid state - CorrelationId: {} - Message: {}", correlationId, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .correlationId(correlationId)
                .code("INVALID_STATE")
                .message(ex.getMessage())
                .type("BUSINESS_ERROR")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);
        log.warn("Security violation - CorrelationId: {} - Message: {}", correlationId, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .correlationId(correlationId)
                .code("ACCESS_DENIED")
                .message("Access denied")
                .type("SECURITY_ERROR")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<ErrorResponse> handleWorkflowException(
            WorkflowException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);
        log.error("Workflow error - CorrelationId: {} - Code: {} - Message: {}",
                correlationId, ex.getErrorCode(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .correlationId(correlationId)
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .type(ex.getErrorType())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .build();

        HttpStatus status = determineHttpStatus(ex.getErrorType());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        String correlationId = getCorrelationId(request);
        log.error("Unexpected error - CorrelationId: {} - Message: {}", correlationId, ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .correlationId(correlationId)
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .type("SYSTEM_ERROR")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getCorrelationId(WebRequest request) {
        // Try to get from request header
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.trim().isEmpty()) {
            // Generate new one
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private HttpStatus determineHttpStatus(String errorType) {
        switch (errorType) {
            case "CLIENT_ERROR":
                return HttpStatus.BAD_REQUEST;
            case "BUSINESS_ERROR":
                return HttpStatus.UNPROCESSABLE_ENTITY;
            case "SECURITY_ERROR":
                return HttpStatus.FORBIDDEN;
            case "SYSTEM_ERROR":
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
