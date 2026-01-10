package com.triobank.ledger.exception;

import java.math.BigDecimal;

/**
 * DoubleEntryMismatchException - Toplam DEBIT != Toplam CREDIT
 * 
 * HTTP Status: 400 Bad Request
 * 
 * Double-entry bookkeeping kuralı ihlali
 */
public class DoubleEntryMismatchException extends LedgerException {

    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;

    public DoubleEntryMismatchException(BigDecimal totalDebit, BigDecimal totalCredit) {
        super(String.format("Double-entry mismatch: DEBIT=%s, CREDIT=%s", totalDebit, totalCredit));
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getDifference() {
        return totalDebit.subtract(totalCredit).abs();
    }
}
