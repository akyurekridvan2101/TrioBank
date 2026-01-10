package com.triobank.transaction.domain.model;

/**
 * Transaction Type Enum
 * 
 * Database: transaction_type NVARCHAR(20)
 * Constraint: CHECK (transaction_type IN ('TRANSFER', 'WITHDRAWAL',
 * 'PURCHASE'))
 */
public enum TransactionType {
    /**
     * Account-to-account money transfer
     * - from_account_id: NOT NULL
     * - to_account_id: NOT NULL
     * - card_id: NULL
     */
    TRANSFER,

    /**
     * ATM cash withdrawal
     * - from_account_id: NOT NULL
     * - to_account_id: NULL
     * - card_id: NOT NULL (debit card + PIN required)
     */
    WITHDRAWAL,

    /**
     * Merchant payment via card (POS or online)
     * - from_account_id: NOT NULL
     * - to_account_id: NULL
     * - card_id: NOT NULL (debit or virtual card)
     */
    PURCHASE
}
