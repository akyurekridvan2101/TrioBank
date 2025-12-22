package com.triobank.account.repository;

import com.triobank.account.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Müşteri Hesapları Veri Erişim Katmanı (Repository)
 * 
 * Hesaplarla ilgili tüm veritabanı işlemlerini yönetir.
 * 
 * Performans Notu (N+1 Önlemi):
 * Account entity'miz "ProductDefinition" ile DB seviyesinde ilişkili olsa da
 * (FK),
 * Java seviyesinde doğrudan `@ManyToOne` bağı kurmadık. Sadece `productCode`
 * tutuyoruz.
 * 
 * Bunun avantajı: Bir müşterinin hesaplarını listelerken (`findByCustomerId`),
 * her hesap için gidip ayrıca Ürün tablosunu çekmeye (N+1 Select) çalışmaz.
 * Çok hızlı ve hafif bir sorgu çalışır. Ürün detayı lazımsa ayrıca sorulur.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Hesap numarasına (IBAN) göre hesabı bulur.
     * 
     * Bu alan veritabanında UNIQUE indexlidir, çok hızlı sonuç döner.
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Müşteriye ait tüm hesapları listeler.
     * 
     * Mobil uygulama veya internet şubesi "Hesaplarım" ekranı için kritiktir.
     * Index (`customer_id`) üzerinden çalıştığı için performanslıdır.
     */
    /**
     * Müşteriye ait tüm hesapları listeler.
     * 
     * Mobil uygulama veya internet şubesi "Hesaplarım" ekranı için kritiktir.
     * Index (`customer_id`) üzerinden çalıştığı için performanslıdır.
     */
    List<Account> findByCustomerId(String customerId);

    /**
     * Müşterinin hesaplarını belirli statülere göre filtreleyerek getirir.
     * 
     * Örn: Sadece ACTIVE ve FROZEN hesapları görmek istiyorum, CLOSED olanları
     * getirme.
     * SQL: SELECT * FROM accounts WHERE customer_id=? AND status IN (?, ?)
     */
    List<Account> findByCustomerIdAndStatusIn(String customerId,
            List<com.triobank.account.domain.model.AccountStatus> statuses);

    /**
     * Bu hesap numarası daha önce kullanılmış mı?
     * (Yeni hesap açarken çakışmayı önlemek için)
     */
    boolean existsByAccountNumber(String accountNumber);
}
