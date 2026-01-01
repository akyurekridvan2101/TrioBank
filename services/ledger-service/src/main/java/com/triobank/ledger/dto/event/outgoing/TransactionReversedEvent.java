package com.triobank.ledger.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * TransactionReversedEvent - Transaction reversal tamamlandı
 * 
 * Topic: ledger.Transaction.v1
 * Outbox: aggregate_type=Transaction, type=TransactionReversed
 * Consumers: Transaction Service (SAGA compensation), Reporting
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReversedEvent {

    /** Orijinal transaction ID (iptal edilen) */
    private String originalTransactionId;

    /** Reversal transaction ID (yeni oluşturulan) */
    private String reversalTransactionId;

    /** İptal nedeni */
    private String reason;

    /** İptal zamanı */
    private Instant reversedAt;
}
