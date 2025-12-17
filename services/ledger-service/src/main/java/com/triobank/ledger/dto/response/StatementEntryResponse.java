package com.triobank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * StatementEntryResponse - Ekstre entry bilgisi
 * 
 * AccountStatementResponse içinde kullanılır
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementEntryResponse {

    /** Entry ID */
    private java.util.UUID entryId;

    /** Transaction ID */
    private String transactionId;

    /** Entry tarihi */
    private LocalDate date;

    /** İşlem saati (UI gösterimi için) */
    private java.time.Instant transactionTime;

    /** Entry tipi (DEBIT, CREDIT) */
    private String entryType;

    /** Tutar */
    private BigDecimal amount;

    /** Running balance (opsiyonel) */
    private BigDecimal runningBalance;

    /** Açıklama */
    private String description;

    /** Referans numarası */
    private String referenceNumber;
}
