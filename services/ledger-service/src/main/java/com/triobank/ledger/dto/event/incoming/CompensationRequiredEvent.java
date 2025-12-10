package com.triobank.ledger.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.Instant;

/**
 * CompensationRequiredEvent - Transaction Service'ten gelen SAGA compensation
 * event
 * 
 * Topic: triobank.prod.transaction.CompensationRequired.v1
 * Action: Transaction'ı reversal yap (CRITICAL - SAGA)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationRequiredEvent {

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
        private String reason;
        private String failedStep;
        private String failureDetails;
        private String compensationId;
    }
}
