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

    /** Para birimi */
    private String currency;

    /** Son güncellenme zamanı */
    private Instant lastUpdatedAt;

    /** Son işlenen entry ID */
    private java.util.UUID lastEntryId;

    /** Optimistic lock versiyonu */
    private Integer version;
}
