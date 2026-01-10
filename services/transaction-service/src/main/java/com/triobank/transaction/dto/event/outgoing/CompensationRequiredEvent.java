package com.triobank.transaction.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CompensationRequiredEvent - Transaction Service'ten gelen SAGA compensation
 * event
 * 
 * Topic: transaction.CompensationRequired.v1
 * Action: Transaction'ı reversal yap (CRITICAL - SAGA)
 * 
 * NOT: Debezium EventRouter metadata'yı soyuyor, sadece payload geliyor!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationRequiredEvent {

    // Payload fields (EventRouter extracts these from outbox)
    private String transactionId;
    private String reason;
    private String failedStep;
    private String failureDetails;
    private String compensationId;
}
