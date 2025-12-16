package com.triobank.ledger.exception;

/**
 * InvalidAmountException - Geçersiz tutar (negatif veya sıfır)
 * 
 * HTTP Status: 400 Bad Request
 */
public class InvalidAmountException extends LedgerException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public static InvalidAmountException negativeAmount() {
        return new InvalidAmountException("Amount must be positive");
    }

    public static InvalidAmountException zeroAmount() {
        return new InvalidAmountException("Amount cannot be zero");
    }
}
