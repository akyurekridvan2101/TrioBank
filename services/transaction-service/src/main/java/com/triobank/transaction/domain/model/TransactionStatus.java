package com.triobank.transaction.domain.model;

/**
 * Transaction Status Enum
 * 
 * Database: status NVARCHAR(20)
 * Constraint: CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
 * 
 * State Machine:
 * - PENDING → COMPLETED (normal flow via TransactionPostedEvent)
 * - PENDING → FAILED (validation error or ledger rejection)
 * 
 * NOTE: No CANCELLED status (use FAILED with failure_reason='USER_CANCELLED')
 */
public enum TransactionStatus {
    /**
     * Transaction created, validation in progress
     * - Initial state
     */
    PENDING,

    /**
     * Ledger confirmed success
     * - Set when TransactionPostedEvent received from Ledger Service
     * - completed_at timestamp populated
     */
    COMPLETED,

    /**
     * Validation failed or Ledger rejected
     * - Set when validation fails or TransactionReversedEvent received
     * - failed_at timestamp populated
     * - failure_reason, failure_details, failed_step populated
     */
    FAILED
}
