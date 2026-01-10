package com.triobank.account.dto.event.incoming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [INCOMING EVENT] Ledger Kaydı Oluşamadı (Compensation)
 * 
 * Kaynak: Ledger Service
 * Amaç: SAGA Telafi Mekanizması (Compensation)
 * 
 * Senaryo:
 * Biz Account Service olarak hesabı başarılı bir şekilde açtık ve event
 * fırlattık.
 * Ancak Ledger Service, "Ben bu hesap için defter (bakiye) kaydı oluşturamadım"
 * dedi.
 * Bu durumda sistemin tutarlı kalması için bizim de açtığımız o hesabı iptal
 * etmemiz gerekir.
 * 
 * NOT: Debezium EventRouter metadata'yı soyuyor, sadece payload geliyor!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccountCreationFailedEvent {

    // Payload fields (EventRouter extracts these from outbox)
    /** Hata alan hesap ID */
    private String accountId;

    /** Ledger'ın işlemi neden yapamadığının sebebi */
    private String reason;
}
