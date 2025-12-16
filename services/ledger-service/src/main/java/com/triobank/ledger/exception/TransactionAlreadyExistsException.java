package com.triobank.ledger.exception;

/**
 * TransactionAlreadyExistsException - Transaction ID zaten mevcut (idempotency)
 * 
 * HTTP Status: 409 Conflict
 */
public class TransactionAlreadyExistsException extends LedgerException {

    private final String transactionId;

    public TransactionAlreadyExistsException(String transactionId) {
        super(String.format("Transaction already exists: %s", transactionId));
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
