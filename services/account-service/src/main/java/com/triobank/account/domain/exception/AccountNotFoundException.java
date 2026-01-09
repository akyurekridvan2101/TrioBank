package com.triobank.account.domain.exception;

/**
 * Account Not Found Exception
 * 
 * Thrown when a requested account does not exist in the system.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }

    public AccountNotFoundException(String accountId, Throwable cause) {
        super("Account not found: " + accountId, cause);
    }
}
