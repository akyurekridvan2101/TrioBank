package com.triobank.account.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * [EVENT] Hesap Oluşturuldu (AccountCreated)
 * 
 * YENİ bir hesap başarıyla açıldığında fırlatılır.
 * 
 * Dinleyenler (Consumers):
 * 1. Ledger Service: Bu hesabın "Defter" kaydını ve 0.00'lık başlangıç
 * bakiyesini oluşturur.
 * 
 * Topic: triobank.account.AccountCreated.v1
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