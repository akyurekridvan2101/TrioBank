package com.triobank.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for Transaction
 * 
 * Returned to frontend after transaction creation or query.
 * Contains full transaction details including status, timestamps, and failure
 * info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    /** Transaction ID (UUID) */
    private String id;

    /** Idempotency key (for duplicate detection) */
    private String idempotencyKey;

    /** Transaction type: TRANSFER, WITHDRAWAL, PURCHASE */
    private String transactionType;

    /** Transaction status: PENDING, COMPLETED, FAILED */
    private String status;

    /** Transaction amount */
    private BigDecimal totalAmount;

    /** Currency (TRY) */
    private String currency;

    /** Source account ID */
    private String fromAccountId;

    /** Destination account ID (null for WITHDRAWAL/PURCHASE) */
    private String toAccountId;

    /** Card ID (null for TRANSFER) */
    private String cardId;

    /** Merchant name (PURCHASE only) */
    private String merchantName;

    /** Merchant category (PURCHASE only) */
    private String merchantCategory;

    /** Online purchase flag (PURCHASE only) */
    private Boolean isOnline;

    /** ATM ID (WITHDRAWAL only) */
    private String atmId;

    /** Location (WITHDRAWAL only) */
    private String location;

    /** Transaction description */
    private String description;

    /** Reference number (for reconciliation) */
    private String referenceNumber;

    /** Posting date (accounting date) */
    private LocalDate postingDate;

    /** Value date (for interest) */
    private LocalDate valueDate;

    /** Ledger transaction ID (after posting) */
    private String ledgerTransactionId;

    /** Transaction created timestamp */
    private Instant createdAt;

    /** Transaction completed timestamp */
    private Instant completedAt;

    /** Transaction failed timestamp */
    private Instant failedAt;

    /** Failure reason code */
    private String failureReason;

    /** Failure details message */
    private String failureDetails;

    /** Failed step (for debugging) */
    private String failedStep;
}
