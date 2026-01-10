package com.triobank.account.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BalanceUpdatedEvent - Ledger Service tarafından yayınlanan bakiye güncelleme
 * event'i
 * 
 * Topic: triobank.ledger.BalanceUpdated.v1
 * Producer: Ledger Service
 * Consumer: Account Service (bu sınıf)
 * 
 * Bu event, Ledger'daki gerçek bakiye değiştiğinde (işlem sonrası) yayınlanır.
 * Account Service bu event'i dinleyerek kendi üzerindeki bakiye projeksiyonunu
 * günceller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdatedEvent {

    /** Hesap ID (Account Service'deki Account.id ile eşleşir) */
    private String accountId;

    /** Önceki bakiye */
    private BigDecimal previousBalance;

    /** Yeni bakiye (güncel) */
    private BigDecimal newBalance;

    /** Değişim miktarı (+/-) */
    private BigDecimal delta;

    /** Para birimi */
    private String currency;

    /** Güncellenme zamanı */
    private Instant updatedAt;
}
