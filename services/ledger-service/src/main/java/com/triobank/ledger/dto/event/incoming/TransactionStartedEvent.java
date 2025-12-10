package com.triobank.ledger.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * TransactionStartedEvent - Transaction Service'ten gelen SAGA event
 * 
 * Topic: triobank.prod.transaction.TransactionStarted.v1
 * Action: Transaction'ı ledger'a kaydet (CRITICAL - SAGA)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStartedEvent {

    /** Event metadata */
    private String eventId;
    private String eventType;
    private String eventVersion;
    private Instant timestamp;
    private String aggregateType;
    private String aggregateId;

    /** Event payload */
    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
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
    }

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
