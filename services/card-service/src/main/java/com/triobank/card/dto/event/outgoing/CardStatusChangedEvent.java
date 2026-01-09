package com.triobank.card.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * [EVENT] Card Status Changed (CardStatusChanged)
 * 
 * Published when a card's status changes (e.g., ACTIVE → BLOCKED).
 * 
 * Potential Consumers:
 * 1. Notification Service: Alert customer about card status change
 * 2. Fraud Detection Service: Monitor unusual status changes
 * 
 * Topic: card.CardStatusChanged.v1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusChangedEvent {

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
        private String previousStatus;
        private String newStatus;
        private String reason;
        private String changedBy;
        private Instant changedAt;
    }
}
