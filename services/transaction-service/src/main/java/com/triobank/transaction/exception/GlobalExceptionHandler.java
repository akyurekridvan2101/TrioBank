package com.triobank.transaction.exception;

import com.triobank.transaction.client.AccountServiceClient;
import com.triobank.transaction.client.CardServiceClient;
import com.triobank.transaction.client.LedgerServiceClient;
import com.triobank.transaction.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * Centralized exception handling for all REST controllers.
 * Converts exceptions to standardized error responses.
 * 
 * Pattern: Copied from AccountService and CardService
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (@Valid)
     * 
     * Returns 400 Bad Request with field-level error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Invalid request parameters")
                .path(request.getDescription(false).replace("uri=", ""))
                .details(fieldErrors)
                .build();

        log.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle TransactionNotFoundException
     * 
     * Returns 404 Not Found
     */
    @ExceptionHandler(TransactionService.TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionService.TransactionNotFoundException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Transaction Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Transaction not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle ValidationException
     * 
     * Returns 400 Bad Request
     */
    @ExceptionHandler(TransactionService.ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            TransactionService.ValidationException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle AuthorizationException
     * 
     * Returns 403 Forbidden
     */
    @ExceptionHandler(TransactionService.AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationException(
            TransactionService.AuthorizationException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Authorization Failed")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Authorization failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle InsufficientBalanceException
     * 
     * Returns 422 Unprocessable Entity
     */
    @ExceptionHandler(TransactionService.InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            TransactionService.InsufficientBalanceException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Insufficient Balance")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.warn("Insufficient balance: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handle AccountServiceException
     * 
     * Returns 503 Service Unavailable
     */
    @ExceptionHandler(AccountServiceClient.AccountServiceException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceException(
            AccountServiceClient.AccountServiceException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Account Service Unavailable")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.error("Account Service error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle CardServiceException
     * 
     * Returns 503 Service Unavailable
     */
    @ExceptionHandler(CardServiceClient.CardServiceException.class)
    public ResponseEntity<ErrorResponse> handleCardServiceException(
            CardServiceClient.CardServiceException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Card Service Unavailable")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.error("Card Service error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle LedgerServiceException
     * 
     * Returns 503 Service Unavailable
     */
    @ExceptionHandler(LedgerServiceClient.LedgerServiceException.class)
    public ResponseEntity<ErrorResponse> handleLedgerServiceException(
            LedgerServiceClient.LedgerServiceException ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Ledger Service Unavailable")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.error("Ledger Service error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle all other exceptions
     * 
     * Returns 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Standardized Error Response DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> details; // For validation errors
    }
}
