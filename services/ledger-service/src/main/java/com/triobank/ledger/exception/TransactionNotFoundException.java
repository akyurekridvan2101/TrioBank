package com.triobank.ledger.exception;

/**
 * TransactionNotFoundException - Transaction bulunamadı
 * 
 * HTTP Status: 404 Not Found
 */
public class TransactionNotFoundException extends LedgerException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super(String.format("Transaction not found: %s", transactionId));
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
