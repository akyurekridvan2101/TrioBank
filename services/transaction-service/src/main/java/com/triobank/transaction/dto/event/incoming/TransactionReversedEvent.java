package com.triobank.transaction.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * TransactionReversedEvent - Transaction reversal tamamlandı
 * 
 * Topic: ledger.Transaction.v1
 * Outbox: aggregate_type=Transaction, type=TransactionReversed
 * Consumers: Transaction Service (SAGA compensation), Reporting
 */
@Data
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
