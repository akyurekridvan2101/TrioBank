package com.triobank.ledger.exception;

import com.triobank.ledger.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Hata Yakalayıcı (Exception Handler)
 * 
 * Uygulamanın neresinde bir hata patlarsa patlasın, burası yakalar.
 * Böylece kullanıcıya saçma sapan stack trace dönmek yerine,
 * "kardeş senin işlem olmadı çünkü şu yüzden" diye düzgün JSON dönüyoruz.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        /**
         * Transaction zaten var (409 Conflict)
         */
        @ExceptionHandler(TransactionAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleTransactionAlreadyExists(
                        TransactionAlreadyExistsException ex,
                        HttpServletRequest request) {

                log.warn("Transaction already exists: {}", ex.getTransactionId());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .code("LE-409")
                                .message(ex.getMessage())
                                .details(Map.of("transactionId", ex.getTransactionId()))
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        /**
         * Transaction bulunamadı (404 Not Found)
         */
        @ExceptionHandler(TransactionNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleTransactionNotFound(
                        TransactionNotFoundException ex,
                        HttpServletRequest request) {

                log.warn("Transaction not found: {}", ex.getTransactionId());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .code("LE-404")
                                .message(ex.getMessage())
                                .details(Map.of("transactionId", ex.getTransactionId()))
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Account bulunamadı (404 Not Found)
         */
        @ExceptionHandler(AccountNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleAccountNotFound(
                        AccountNotFoundException ex,
                        HttpServletRequest request) {

                log.warn("Account not found: {}", ex.getAccountId());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .code("LE-404-ACCOUNT")
                                .message(ex.getMessage())
                                .details(Map.of("accountId", ex.getAccountId()))
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Double-entry mismatch (400 Bad Request)
         */
        @ExceptionHandler(DoubleEntryMismatchException.class)
        public ResponseEntity<ErrorResponse> handleDoubleEntryMismatch(
                        DoubleEntryMismatchException ex,
                        HttpServletRequest request) {

                log.error("Double-entry mismatch: DEBIT={}, CREDIT={}",
                                ex.getTotalDebit(), ex.getTotalCredit());

                Map<String, Object> details = new HashMap<>();
                details.put("totalDebit", ex.getTotalDebit());
                details.put("totalCredit", ex.getTotalCredit());
                details.put("difference", ex.getDifference());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code("LE-400-DOUBLE_ENTRY")
                                .message(ex.getMessage())
                                .details(details)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Invalid amount (400 Bad Request)
         */
        @ExceptionHandler(InvalidAmountException.class)
        public ResponseEntity<ErrorResponse> handleInvalidAmount(
                        InvalidAmountException ex,
                        HttpServletRequest request) {

                log.warn("Invalid amount: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code("LE-400-AMOUNT")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Optimistic Locking Hatası (409 Conflict)
         * 
         * AYNI ANDA (Concurrent) işlem yapmaya çalışan iki kişi çakışırsa bu hata
         * fırlar.
         * 
         * Senaryo:
         * 1. Ali ve Veli aynı anda hesabı okudu (Version: 5).
         * 2. Ali parayı çekti ve kaydetti (Version: 6 oldu).
         * 3. Veli de kaydetmeye çalıştı ama elindeki Version 5 artık eskidi!
         * 4. Sistem: "Hop dedik, veri değişti, tekrar dene" der.
         * 
         * Çözüm:
         * Client'a 409 dönüyoruz. Client işlemi baştan (retry) yapmalı.
         */
        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
                        OptimisticLockingFailureException ex,
                        HttpServletRequest request) {

                // Log WARNING seviyesinde - kritik değil ama takip edilmeli
                log.warn("Optimistic locking failure detected - concurrent modification: {}",
                                ex.getMessage());

                // Detaylı log (DEBUG) - troubleshooting için
                if (log.isDebugEnabled()) {
                        log.debug("Optimistic lock exception details", ex);
                }

                // Client'a anlamlı hata mesajı
                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.CONFLICT.value())
                                .error("Conflict")
                                .code("LE-409-CONCURRENCY")
                                .message("The resource was modified by another transaction. Please retry your request.")
                                .details(Map.of(
                                                "reason", "Concurrent modification detected",
                                                "recommendation", "Retry the operation with fresh data",
                                                "technicalDetails", ex.getMessage()))
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        /**
         * Bean validation hatası (@Valid)
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, Object> details = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));

                log.warn("Validation errors: {}", details);

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code("LE-400-VALIDATION")
                                .message("Validation failed")
                                .details(details)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Genel Ledger exception (500 Internal Server Error)
         */
        @ExceptionHandler(LedgerException.class)
        public ResponseEntity<ErrorResponse> handleLedgerException(
                        LedgerException ex,
                        HttpServletRequest request) {

                log.error("Ledger exception", ex);

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .code("LE-500")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        /**
         * Static resource bulunamadı (404) - Spring Boot 3 favicon.ico vb.
         */
        @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
                        org.springframework.web.servlet.resource.NoResourceFoundException ex,
                        HttpServletRequest request) {

                // Info level yeterli, error değil
                log.info("Resource not found: {}", ex.getResourcePath());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .code("LE-404-RESOURCE")
                                .message("Resource not found: " + ex.getResourcePath())
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Beklenmedik hata (500 Internal Server Error)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

                log.error("Unexpected error", ex);

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .code("LE-500-UNEXPECTED")
                                .message("An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
