package com.triobank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Hesap Açılış İsteği (Request)
 * 
 * Yeni bir müşteri hesabı açmak için API'ye gönderilen verileri taşır.
 * 
 * Validasyon Kuralları:
 * - Müşteri ID boş olamaz.
 * - Hesap tipi (Ürün) seçilmek zorundadır.
 * - Para birimi belirtilmelidir.
 * - Configuration opsiyoneldir:
 * • Boş gönderilirse → ProductDefinition.defaultConfiguration kullanılır
 * • Kısmi gönderilirse → Default + Override merge edilir
 * • Tam gönderilirse → Default üzerine yazılır
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    /**
     * Müşteri Numarası (TCKN veya Müşteri ID)
     * Örn: "12345678"
     */
    @NotBlank(message = "Müşteri ID'si zorunludur.")
    private String customerId;

    /**
     * Açılacak Hesap Türü / Ürün Kodu
     * Örn: "RETAIL_TRY", "GOLD_ACC", "SAVINGS_USD"
     * 
     * Bu ürünün defaultConfiguration'u otomatik uygulanır.
     */
    @NotBlank(message = "Ürün kodu zorunludur.")
    private String productCode;

    /**
     * Para Birimi
     * Örn: "TRY", "USD", "EUR"
     */
    @NotBlank(message = "Para birimi zorunludur.")
    private String currency;

    /**
     * Özel Konfigürasyon (Opsiyonel)
     * 
     * Örnek 1 - Boş (default'lar kullanılır):
     * null veya {}
     * 
     * Örnek 2 - Kısmi override:
     * {
     * "dailyTransactionLimit": 10000 // Sadece bu override edilir
     * }
     * 
     * Örnek 3 - Tam override:
     * {
     * "emailNotifications": false,
     * "dailyTransactionLimit": 20000,
     * "preferredLanguage": "en-US"
     * }
     */
    private Map<String, Object> configurations;
}
