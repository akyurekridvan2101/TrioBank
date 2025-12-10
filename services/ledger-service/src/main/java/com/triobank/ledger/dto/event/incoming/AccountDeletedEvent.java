package com.triobank.ledger.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AccountDeletedEvent - Account Service'ten gelen event
 * 
 * Topic: triobank.prod.account.AccountDeleted.v1
 * Action: Balance'ı freeze et
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletedEvent {

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
        private String deletedBy;
        private String deletionReason;
        private Instant deletedAt;
    }
}
