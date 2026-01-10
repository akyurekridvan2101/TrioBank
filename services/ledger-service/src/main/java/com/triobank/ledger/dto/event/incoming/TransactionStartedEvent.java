package com.triobank.ledger.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * TransactionStartedEvent - Transaction Service'ten gelen SAGA event
 * 
 * Topic: transaction.TransactionStarted.v1
 * Action: Transaction'ı ledger'a kaydet (CRITICAL - SAGA)
 * 
 * NOT: Debezium EventRouter metadata'yı soyuyor, sadece payload geliyor!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStartedEvent {

    // Payload fields (EventRouter extracts these from outbox)
    private String transactionId;
    private String transactionType;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal totalAmount;
    private String currency;
    private String description;
    private LocalDate postingDate;
    private LocalDate valueDate;
    private String initiatorId;
    private String referenceNumber;
    private List<EntryDto> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryDto {
        private Integer sequence;
        private String accountId;
        private String entryType;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String referenceNumber;
    }
}
