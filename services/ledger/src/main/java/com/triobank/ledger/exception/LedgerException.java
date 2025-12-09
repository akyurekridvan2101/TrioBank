package com.triobank.ledger.exception;

/**
 * LedgerException - Tüm Ledger exception'larının base class'ı
 * 
 * RuntimeException'dan türer (unchecked exception)
 */
public class LedgerException extends RuntimeException {

    /**
     * Sadece mesaj ile exception
     */
    public LedgerException(String message) {
        super(message);
    }

    /**
     * Mesaj ve cause ile exception
     */
    public LedgerException(String message, Throwable cause) {
        super(message, cause);
    }
}
