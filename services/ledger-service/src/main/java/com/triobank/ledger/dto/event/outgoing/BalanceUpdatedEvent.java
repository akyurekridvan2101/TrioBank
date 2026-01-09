package com.triobank.ledger.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BalanceUpdatedEvent - Bakiye değişim event'i
 * 
 * Topic: ledger.BalanceUpdated.v1
 * Outbox: aggregate_type=AccountBalance, type=BalanceUpdated
 * Consumers: Account Service, Notification Service, Reporting
 * 
 * NOT: OutboxService Map kullanarak serialize eder, DTO sadece type-safe
 * wrapper
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdatedEvent {

    /** Hesap ID */
    private String accountId;

    /** Önceki bakiye */
    private BigDecimal previousBalance;

    /** Yeni bakiye */
    private BigDecimal newBalance;

    /** Değişim miktarı (+/-) */
    private BigDecimal delta;

    /** Para birimi */
    private String currency;

    /** Güncellenme zamanı */
    private Instant updatedAt;
}
