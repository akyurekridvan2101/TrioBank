package com.triobank.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * AccountBalance - Hesap bakiyesi (performans cache'i)
 * 
 * Ledger entry'lerden hesaplanan güncel bakiyeyi saklar. Her hesap için tek
 * kayıt var.
 * Optimistic locking ile concurrent update'lerden korunur.
 * 
 * Database: account_balances tablosu
 * 
 * Neden cache olarak kullanıyoruz:
 * - Performans: Her sorguda tüm entry'leri toplamak yerine direkt bakiyeyi
 * okuruz
 * - Hızlı sorgulama: "Bakiyem ne kadar?" sorusu çok hızlı cevaplanır
 * 
 * Önemli:
 * - Mutable: Ledger entry geldiğinde UPDATE edilir (diğer tablolar immutable)
 * - Version kontrollu: Optimistic locking ile eş zamanlı güncellemeler
 * yönetilir
 */
@Entity
@Table(name = "account_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA için
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder için
@Builder
public class AccountBalance {

    /**
     * Account ID - Primary key
     * 
     * Account Service'teki hesap ID'sini tutuyoruz. Foreign key değil çünkü
     * mikroservis mimarisindeyiz, Account Service başka bir database kullanıyor.
     * 
     * Örnekler: "ACC-123", "ACC-456"
     */
    @Id
    @Column(name = "account_id", length = 50, nullable = false)
    private String accountId;

    /**
     * Güncel bakiye - Hesaptaki toplam para
     * 
     * Her entry geldiğinde güncellenir:
     * - CREDIT entry geldi mi bakiye artar (+)
     * - DEBIT entry geldi mi bakiye azalır (-)
     * 
     * Pozitif: Hesapta para var
     * Negatif: Borç var (kredı hesaplarında olabilir)
     * Sıfır: Boş hesap
     */
    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Para birimi - ISO 4217 standart kod
     * 
     * Örnekler: "TRY", "USD", "EUR"
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Son işlenen entry ID - Audit için
     * 
     * Bu hesabın bakiyesini etkileyen son ledger entry'nin ID'si.
     * Sorun çıktığında "hangi entry'ye kadar işlendi?" diye kontrol için.
     */
    @Column(name = "last_entry_id")
    private UUID lastEntryId;

    /**
     * Son güncellenme zamanı - Bakiye en son ne zaman değişti
     * 
     * Her balance update'inde otomatik güncellenir.
     */
    @Column(name = "last_updated_at", nullable = false)
    @Builder.Default
    private Instant lastUpdatedAt = Instant.now();

    /**
     * Version - Optimistic Locking (eş zamanlı güncelleme kontrolü)
     * 
     * Çalışma mantığı:
     * -----------------------------------------------------------------------
     * Senaryo: Ali'nin hesabına aynı anda 2 transfer geliyor
     * 
     * Başlangıç: balance=1000 TL, version=5
     * 
     * [Transaction 1] [Transaction 2]
     * - Oku: balance=1000, version=5 - Oku: balance=1000, version=5
     * - Hesapla: 1000 + 500 = 1500 - Hesapla: 1000 + 300 = 1300
     * - UPDATE account_balances - UPDATE account_balances
     * SET balance=1500, version=6 SET balance=1300, version=6
     * WHERE id='ACC-ALI' WHERE id='ACC-ALI'
     * AND version=5 ✅ Başarılı! AND version=5 ❌ HATA!
     * (version artık 6, 5 değil)
     * -----------------------------------------------------------------------
     * 
     * Transaction 2 OptimisticLockException alır ve retry yapar:
     * - Tekrar oku: balance=1500, version=6
     * - Hesapla: 1500 + 300 = 1800
     * - UPDATE ... WHERE version=6 ✅ Başarılı!
     * 
     * Sonuç: balance=1800 TL, version=7 (doğru!)
     * 
     * @Version annotationı JPA tarafından otomatik yönetilir.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 1L;

    /**
     * Oluşturulma zamanı - İlk entry geldiğinde otomatik oluşturulur
     * 
     * Hibernate otomatik olarak şu anki zamanı (UTC) buraya yazar.
     * updatable=false: Bir kez yazıldı mı asla değiştirilemez.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Bakiyeyi güncelle - Her yeni entry geldiğinde çağrılır
     * 
     * @param delta   Değişim miktarı (pozitif: artış, negatif: azalış)
     * @param entryId Son işlenen entry ID'si
     * 
     *                NOT: version field'ı @Version annotation sayesinde JPA
     *                tarafından
     *                otomatik arttırılır, manuel update gerekmez.
     */
    public void updateBalance(BigDecimal delta, UUID entryId) {
        this.balance = this.balance.add(delta);
        this.lastEntryId = entryId;
        this.lastUpdatedAt = Instant.now();
        // version otomatik artacak (@Version)
    }

    /**
     * Bakiye pozitif mi? (hesapta para var)
     */
    public boolean hasPositiveBalance() {
        return balance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Bakiye negatif mi? (borç var)
     */
    public boolean hasNegativeBalance() {
        return balance.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Bakiye sıfır mı?
     */
    public boolean isZeroBalance() {
        return balance.compareTo(BigDecimal.ZERO) == 0;
    }
}
