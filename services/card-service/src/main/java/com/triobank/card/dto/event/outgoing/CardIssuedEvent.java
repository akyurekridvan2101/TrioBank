package com.triobank.card.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * [EVENT] Card Issued (CardIssued)
 * 
 * Published when a new card is successfully issued.
 * 
 * Potential Consumers:
 * 1. Notification Service: Send card issuance confirmation to customer
 * 2. Analytics Service: Track card issuance metrics
 * 
 * Topic: card.CardIssued.v1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardIssuedEvent {

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
        private String cardId;
        private String cardType;
        private String accountId;
        private String maskedNumber;
        private String cardBrand;
        private String status;
        private String issuedBy;
        private Instant issuedAt;
    }
}
