package com.triobank.ledger.repository;

import com.triobank.ledger.domain.model.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AccountBalanceRepository - Bakiye yönetiminin kalbi.
 * 
 * Burada en kritik konu concurrency. Aynı anda birden fazla işlem gelirse
 * bakiyenin tutarlı kalması için Pessimistic Locking kullanıyoruz.
 * Performans için cache mantığıyla çalışır ama gerektiğinde lock ile veriyi
 * korur.
 */
@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {

    // ========================================
    // Basic Queries
    // ========================================

    /**
     * Hesap ID ile bakiye bul
     * 
     * @param accountId Hesap ID
     * @return AccountBalance (varsa)
     */
    Optional<AccountBalance> findByAccountId(String accountId);

    /**
     * Hesap bakiyesi var mı?
     * 
     * @param accountId Hesap ID
     * @return true ise var
     */
    boolean existsByAccountId(String accountId);

    // ========================================
    // Locking Queries (Concurrent Update için)
    // ========================================

    /**
     * Pessimistic lock ile bakiye bul
     * Concurrent update yapılacaksa kullan
     * 
     * @param accountId Hesap ID
     * @return AccountBalance (locked)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountId = :accountId")
    Optional<AccountBalance> findByAccountIdWithLock(@Param("accountId") String accountId);

    // ========================================
    // Currency Queries
    // ========================================

    /**
     * Belirli para birimindeki hesapları bul
     * 
     * @param currency Para birimi (TRY, USD, EUR)
     * @return AccountBalance listesi
     */
    List<AccountBalance> findByCurrency(String currency);

    // ========================================
    // Balance Range Queries
    // ========================================

    /**
     * Bakiyesi belirli değerin üstünde olan hesaplar
     * 
     * @param minBalance Minimum bakiye
     * @return AccountBalance listesi
     */
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.balance > :minBalance")
    List<AccountBalance> findByBalanceGreaterThan(@Param("minBalance") BigDecimal minBalance);

    /**
     * Bakiyesi belirli değerin altında olan hesaplar (düşük bakiye uyarısı için)
     * 
     * @param maxBalance Maximum bakiye
     * @return AccountBalance listesi
     */
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.balance < :maxBalance")
    List<AccountBalance> findByBalanceLessThan(@Param("maxBalance") BigDecimal maxBalance);

    /**
     * Negatif bakiyeli hesaplar (borçlu hesaplar)
     * 
     * @return AccountBalance listesi
     */
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.balance < 0")
    List<AccountBalance> findNegativeBalances();

    /**
     * Sıfır bakiyeli hesaplar
     * 
     * @return AccountBalance listesi
     */
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.balance = 0")
    List<AccountBalance> findZeroBalances();

    // ========================================
    // Statistics
    // ========================================

    /**
     * Toplam bakiye (tüm hesaplar)
     * 
     * @param currency Para birimi
     * @return Toplam bakiye
     */
    @Query("SELECT COALESCE(SUM(ab.balance), 0) FROM AccountBalance ab WHERE ab.currency = :currency")
    BigDecimal sumTotalBalance(@Param("currency") String currency);

    /**
     * Ortalama bakiye
     * 
     * @param currency Para birimi
     * @return Ortalama bakiye
     */
    @Query("SELECT COALESCE(AVG(ab.balance), 0) FROM AccountBalance ab WHERE ab.currency = :currency")
    BigDecimal averageBalance(@Param("currency") String currency);

    /**
     * Para birimindeki hesap sayısı
     * 
     * @param currency Para birimi
     * @return Hesap sayısı
     */
    long countByCurrency(String currency);
}
