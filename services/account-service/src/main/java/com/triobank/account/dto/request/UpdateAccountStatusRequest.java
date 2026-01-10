package com.triobank.account.dto.request;

import com.triobank.account.domain.model.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hesap Durum Güncelleme İsteği
 * 
 * Mevcut bir hesabın durumunu değiştirmek (Dondurma, Kapatma, Aktifleştirme)
 * için kullanılır.
 * 
 * Not:
 * Durum değişikliği hassas bir işlem olduğu için genellikle bir "sebep"
 * (reason)
 * belirtilmesi istenir (Denetim izleri - Audit Log için).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountStatusRequest {

    /**
     * Hesabın geçeceği yeni durum
     * Örn: FROZEN (Dondurulmuş), CLOSED (Kapanmış)
     */
    @NotNull(message = "Yeni durum (status) bilgisi zorunludur.")
    private AccountStatus status;

    /**
     * Değişiklik Nedeni
     * Örn: "Müşteri talimatı ile kapatıldı", "Şüpheli işlem şüphesiyle donduruldu"
     */
    private String reason;
}
