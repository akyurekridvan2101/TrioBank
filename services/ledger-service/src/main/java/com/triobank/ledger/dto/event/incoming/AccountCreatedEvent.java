package com.triobank.ledger.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AccountCreatedEvent - Account Service'ten gelen event
 * 
 * Topic: triobank.prod.account.AccountCreated.v1
 * Action: Initial balance (0.00) oluştur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {

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
        private String accountId;
        private String accountNumber;
        private String customerId;
        private String accountType;
        private String currency;
        private String status;
        private String createdBy;
        private Instant createdAt;
    }
}
