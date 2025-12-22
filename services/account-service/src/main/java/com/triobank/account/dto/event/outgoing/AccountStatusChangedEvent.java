package com.triobank.account.dto.event.outgoing;

import com.triobank.account.domain.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * [EVENT] Hesap Durumu Değişti (AccountStatusChanged)
 * 
 * Bir hesabın statüsü (Active -> Frozen, Frozen -> Active, Active -> Closed)
 * değiştiğinde fırlatılır.
 * 
 * Dinleyenler (Consumers):
 * 1. Transaction Service: Eğer hesap "FROZEN" veya "CLOSED" olduysa, o hesaptan
 * para çıkışını engellemek (Block) için kendi veritabanını günceller.
 * 2. Card Service: Hesaba bağlı banka kartlarını geçici olarak kapatır.
 * 
 * Topic: triobank.account.AccountStatusChanged.v1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusChangedEvent {

    private String eventId;
    private String eventType; // "AccountStatusChanged"
    private Instant timestamp;

    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String accountId;
        private String customerId;

        /** Eski Durum */
        private AccountStatus oldStatus;

        /** Yeni (Güncel) Durum */
        private AccountStatus newStatus;

        /** Değişiklik Nedeni (Audit Log) */
        private String reason;
    }
}
