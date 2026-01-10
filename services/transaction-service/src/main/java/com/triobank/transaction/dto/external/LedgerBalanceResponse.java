package com.triobank.transaction.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Account Balance Response (from Ledger Service)
 * 
 * Response from: GET /api/v1/ledger/balances/{accountId}
 * Used for: Balance check before transaction processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerBalanceResponse {

    /** Hesap ID */
    private String accountId;

    /** Güncel bakiye */
    private BigDecimal balance;

    /** Bloke edilmiş tutar (pending transactions için ayrılmış) */
    private BigDecimal blockedAmount;

    /** Kullanılabilir bakiye (balance - blockedAmount) */
    private BigDecimal availableBalance;

    /** Bekleyen borç işlemleri toplamı */
    private BigDecimal pendingDebits;

    /** Bekleyen alacak işlemleri toplamı */
    private BigDecimal pendingCredits;

    /** Para birimi */
    private String currency;

    /** Son güncellenme zamanı */
    private Instant lastUpdatedAt;

    /** Son işlenen entry ID */
    private java.util.UUID lastEntryId;

    /** Optimistic lock versiyonu */
    private Long version;
}
