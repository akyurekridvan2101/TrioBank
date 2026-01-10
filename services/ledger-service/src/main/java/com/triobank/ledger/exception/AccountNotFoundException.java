package com.triobank.ledger.exception;

/**
 * AccountNotFoundException - Hesap ledger'da bulunamadı
 * 
 * HTTP Status: 404 Not Found
 */
public class AccountNotFoundException extends LedgerException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super(String.format("Account not found in ledger: %s", accountId));
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
