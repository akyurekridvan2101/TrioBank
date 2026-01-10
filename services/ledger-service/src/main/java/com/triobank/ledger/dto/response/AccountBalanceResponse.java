package com.triobank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * AccountBalanceResponse - Hesap bakiyesi response
 * 
 * Endpoint: GET /api/v1/ledger/balances/{accountId}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {

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
