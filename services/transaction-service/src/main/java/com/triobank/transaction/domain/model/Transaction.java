package com.triobank.transaction.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Transaction Entity - Immutable transaction record
 *
 * Represents all types of financial transactions:
 * - TRANSFER: Money transfer between accounts
 * - WITHDRAWAL: Cash withdrawal from ATM
 * - PURCHASE: Card purchase (POS/online)
 *
 * SAGA Pattern:
 * 1. Created with status PENDING
 * 2. Publishes TransactionStartedEvent to Kafka (via Outbox)
 * 3. Ledger Service processes and publishes result
 * 4. Updated to COMPLETED or FAILED based on Ledger response
 */
@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_transactions_idempotency_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "UQ_transactions_reference_number", columnNames = "reference_number")
}, indexes = {
        @Index(name = "IX_transactions_from_account", columnList = "from_account_id"),
        @Index(name = "IX_transactions_to_account", columnList = "to_account_id"),
        @Index(name = "IX_transactions_card", columnList = "card_id"),
        @Index(name = "IX_transactions_status", columnList = "status"),
        @Index(name = "IX_transactions_type", columnList = "transaction_type"),
        @Index(name = "IX_transactions_initiator", columnList = "initiator_id"),
        @Index(name = "IX_transactions_created_desc", columnList = "created_at DESC"),
        @Index(name = "IX_transactions_posting_date", columnList = "posting_date"),
        @Index(name = "IX_transactions_from_account_status", columnList = "from_account_id, status"),
        @Index(name = "IX_transactions_from_account_created", columnList = "from_account_id, created_at DESC"),
        @Index(name = "IX_transactions_initiator_status", columnList = "initiator_id, status"),
        @Index(name = "IX_transactions_status_created", columnList = "status, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requirement
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder requirement
@Builder
public class Transaction {

    // ============================================
    // Primary Key
    // ============================================
    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    // ============================================
    // Idempotency (CRITICAL for Duplicate Prevention)
    // ============================================
    @Column(name = "idempotency_key", length = 100, nullable = false, unique = true)
    private String idempotencyKey;

    // ============================================
    // Transaction Classification
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20, nullable = false)
    private TransactionType transactionType;

    // ============================================
    // Transaction Status (State Machine)
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatus status;

    // ============================================
    // Financial Information
    // ============================================
    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "TRY";

    // ============================================
    // Account References (Microservice Boundaries)
    // ============================================
    @Column(name = "from_account_id", length = 50)
    private String fromAccountId;

    @Column(name = "to_account_id", length = 50)
    private String toAccountId;

    // ============================================
    // Card Reference (Optional for Card-Based Transactions)
    // ============================================
    @Column(name = "card_id", length = 50)
    private String cardId;

    // ============================================
    // Merchant Information (for PURCHASE transactions only)
    // ============================================
    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "merchant_category", length = 50)
    private String merchantCategory;

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    // ============================================
    // ATM Information (for WITHDRAWAL transactions only)
    // ============================================
    @Column(name = "atm_id", length = 50)
    private String atmId;

    @Column(name = "location", length = 200)
    private String location;

    // ============================================
    // Business Metadata
    // ============================================
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "initiator_id", length = 50)
    private String initiatorId;

    @Column(name = "reference_number", length = 100, nullable = false, unique = true)
    private String referenceNumber;

    // ============================================
    // Ledger Integration (Event-Driven)
    // ============================================
    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "ledger_transaction_id", length = 100)
    private String ledgerTransactionId;

    // ============================================
    // Timestamps (Full Audit Trail)
    // ============================================
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    // ============================================
    // Failure Information (populated only if status=FAILED)
    // ============================================
    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "failure_details", length = 1000)
    private String failureDetails;

    @Column(name = "failed_step", length = 50)
    private String failedStep;

    // ============================================
    // Optimistic Locking (Concurrency Control)
    // ============================================
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    // ============================================
    // Domain Methods
    // ============================================

    /**
     * Mark transaction as COMPLETED
     * Called when TransactionPostedEvent received from Ledger Service
     */
    public void markAsCompleted(String ledgerTransactionId) {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot complete transaction %s with status %s", this.id, this.status));
        }
        this.status = TransactionStatus.COMPLETED;
        this.ledgerTransactionId = ledgerTransactionId;
        this.completedAt = Instant.now();
    }

    /**
     * Mark transaction as FAILED
     * Called when validation fails or TransactionReversedEvent received
     */
    public void markAsFailed(String reason, String details, String step) {
        if (this.status == TransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                    String.format("Cannot fail already completed transaction %s", this.id));
        }
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.failureDetails = details;
        this.failedStep = step;
        this.failedAt = Instant.now();
    }

    /**
     * Set ledger integration dates
     * Called when TransactionStartedEvent is published
     */
    public void setLedgerDates(LocalDate postingDate, LocalDate valueDate) {
        this.postingDate = postingDate;
        this.valueDate = valueDate;
    }
}
