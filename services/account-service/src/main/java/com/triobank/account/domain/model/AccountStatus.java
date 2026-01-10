package com.triobank.account.domain.model;

/**
 * Hesap Statüsü
 * 
 * Basitleştirilmiş model: Hesap ya açıktır ya kapalı.
 * Ara durumlar (Frozen, Suspended vb.) işletme kararıyla kaldırıldı.
 */
public enum AccountStatus {
    ACTIVE,
    CLOSED
}
