package com.triobank.account.dto.response;

import com.triobank.account.domain.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Hesap Detay Cevabı (Response)
 * 
 * API'den dış dünyaya (Mobil Uygulama, İnternet Şube) dönülen hesap objesidir.
 * Veritabanı varlığımız (Entity) ile dış dünya arasındaki yalıtımı sağlar.
 * Hassas veriler (varsa) burada filtrelenebilir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    /** Hesabın sistemdeki tekil ID'si (UUID) */
    private String id;

    /** Hesabın sahibi olan müşterinin ID'si */
    private String customerId;

    /**
     * Müşterinin göreceği Hesap Numarası (IBAN)
     * Örn: TR12 34...
     */
    private String accountNumber;

    /** Ürün Kodu (Vadesiz, Vadeli, Altın vb.) */
    private String productCode;

    /** Hesabın güncel durumu (ACTIVE, FROZEN vb.) */
    private AccountStatus status;

    /**
     * Ürüne özgü dinamik ayarlar
     * Örn: { "ekHesapLimiti": 5000, "faizOrani": 0.18 }
     */
    private Map<String, Object> configurations;

    /** Oluşturulma Zamanı */
    private Instant createdAt;

    /** Son Güncelleme Zamanı */
    private Instant updatedAt;
}
