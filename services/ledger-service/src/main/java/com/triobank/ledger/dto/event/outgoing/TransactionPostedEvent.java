package com.triobank.ledger.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * TransactionPostedEvent - Transaction ledger'a kaydedildi (SAGA success)
 * 
 * Topic: ledger.Transaction.v1
 * Outbox: aggregate_type=Transaction, type=TransactionPosted
 * Consumers: Transaction Service (SAGA)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPostedEvent {

    /** Transaction ID */
    private String transactionId;

    /** Transaction tipi */
    private String transactionType;

    /** Toplam tutar */
    private BigDecimal totalAmount;

    /** Para birimi */
    private String currency;

    /** Muhasebe tarihi */
    private LocalDate postingDate;

    /** Kaç entry oluşturuldu */
    private Integer entriesCount;

    /** Kaydedilme zamanı */
    private Instant postedAt;
}
