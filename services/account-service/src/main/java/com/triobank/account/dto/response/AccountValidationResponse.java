package com.triobank.account.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Account Validation Response
 * 
 * Transaction Service için hesap doğrulama bilgilerini döner.
 * Hem hesap durumu hem de ürün kurallarını içerir.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResponse {

    /**
     * Hesap işlem yapabilir durumda mı?
     * (Status ACTIVE ve balance yeterli ise true)
     */
    private boolean isValid;

    /**
     * Hesap durumu
     */
    private String accountStatus;

    /**
     * Ürün kodu
     */
    private String productCode;

    /**
     * Ürün kategorisi
     */
    private String productCategory;

    /**
     * Para birimi
     */
    private String currency;

    /**
     * Mevcut bakiye (Account Service projection)
     */
    private BigDecimal currentBalance;

    /**
     * Ürün özellikleri (Product Features)
     * 
     * Örnekler:
     * - minimumBalance: 100.00
     * - monthlyLimit: 50000.00
     * - allowOverdraft: false
     * - interestRate: 12.5
     */
    private Map<String, Object> productFeatures;

    /**
     * Kullanıcıya özel yapılandırma (User Configuration)
     * 
     * Örnekler:
     * - dailyTransactionLimit: 5000.00
     * - emailNotifications: true
     * - smsNotifications: false
     */
    private Map<String, Object> userConfiguration;
}
