package com.triobank.ledger.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * AccountCreatedEvent - Account Service'ten gelen event
 * 
 * Topic: account.AccountCreated.v1
 * Action: Initial balance (0.00) oluştur
 * 
 * NOT: Debezium EventRouter metadata'yı soyuyor, sadece payload geliyor!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {

    // Payload fields (EventRouter extracts these from outbox)
    private String accountId;
    private String accountNumber;
    private String customerId;
    private String accountType;
    private String currency;
    private String status;
    private String createdBy;
    private Instant createdAt;
}
