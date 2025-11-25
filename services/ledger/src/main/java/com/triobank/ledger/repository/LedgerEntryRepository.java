package com.triobank.ledger.repository;

import com.triobank.ledger.domain.model.EntryType;
import com.triobank.ledger.domain.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * LedgerEntryRepository - Entry sorgulama ve bakiye hesaplama
 * 
 * En kritik repository - bakiye hesaplamaları burada
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

        // ========================================
        // Transaction Queries
        // ========================================

        /**
         * Bir transaction'ın tüm entry'lerini bul (sequence sırasına göre)
         * 
         * @param transactionId Transaction ID
         * @return Entry listesi (sıralı)
         */
        @Query("SELECT e FROM LedgerEntry e " +
                        "WHERE e.transaction.transactionId = :transactionId " +
                        "ORDER BY e.sequence ASC")
        List<LedgerEntry> findByTransactionIdOrderBySequence(@Param("transactionId") String transactionId);

        // ========================================
        // Account Queries (Pagination)
        // ========================================

        /**
         * Bir hesabın tüm entry'lerini bul (sayfalama ile)
         * 
         * @param accountId Hesap ID
         * @param pageable  Sayfalama bilgisi
         * @return Entry sayfası
         */
        Page<LedgerEntry> findByAccountId(String accountId, Pageable pageable);

        /**
         * Bir hesabın tüm entry'lerini bul (tarihe göre sıralı - running balance için)
         * 
         * @param accountId Hesap ID
         * @return Entry listesi (posting date ASC)
         */
        List<LedgerEntry> findByAccountIdOrderByPostingDateAsc(String accountId);

        /**
         * Bir hesabın entry'lerini tarih aralığında bul
         * 
         * @param accountId Hesap ID
         * @param startDate Başlangıç tarihi
         * @param endDate   Bitiş tarihi
         * @param pageable  Sayfalama
         * @return Entry sayfası
         */
        Page<LedgerEntry> findByAccountIdAndPostingDateBetween(
                        String accountId,
                        LocalDate startDate,
                        LocalDate endDate,
                        Pageable pageable);

        /**
         * Bir hesabın belirli tipteki entry'lerini bul
         * 
         * @param accountId Hesap ID
         * @param entryType DEBIT veya CREDIT
         * @param pageable  Sayfalama
         * @return Entry sayfası
         */
        Page<LedgerEntry> findByAccountIdAndEntryType(
                        String accountId,
                        EntryType entryType,
                        Pageable pageable);

        // ========================================
        // Balance Calculation (ÇOK ÖNEMLİ!)
        // ========================================

        /**
         * Bir hesabın bakiyesini hesapla (CREDIT - DEBIT)
         * 
         * CREDIT: Pozitif (hesaba giriş)
         * DEBIT: Negatif (hesaptan çıkış)
         * 
         * @param accountId Hesap ID
         * @return Toplam bakiye
         */
        @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = com.triobank.ledger.domain.model.EntryType.CREDIT THEN e.amount "
                        +
                        "                            WHEN e.entryType = com.triobank.ledger.domain.model.EntryType.DEBIT THEN -e.amount "
                        +
                        "                       END), 0) " +
                        "FROM LedgerEntry e " +
                        "WHERE e.accountId = :accountId " +
                        "AND e.status = 'POSTED'")
        BigDecimal calculateBalance(@Param("accountId") String accountId);

        /**
         * Belirli tarihe kadar bakiye hesapla
         * 
         * @param accountId Hesap ID
         * @param upToDate  Hangi tarihe kadar (dahil)
         * @return Bakiye
         */
        @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = com.triobank.ledger.domain.model.EntryType.CREDIT THEN e.amount "
                        +
                        "                            WHEN e.entryType = com.triobank.ledger.domain.model.EntryType.DEBIT THEN -e.amount "
                        +
                        "                       END), 0) " +
                        "FROM LedgerEntry e " +
                        "WHERE e.accountId = :accountId " +
                        "AND e.postingDate <= :upToDate " +
                        "AND e.status = 'POSTED'")
        BigDecimal calculateBalanceUpToDate(
                        @Param("accountId") String accountId,
                        @Param("upToDate") LocalDate upToDate);

        // ========================================
        // Statistics Queries
        // ========================================

        /**
         * Bir hesapta toplam kaç adet hareket olduğunu döner.
         * Sayfalama yaparken toplam sayfa sayısını bulmak için lazım oluyor.
         */
        long countByAccountId(String accountId);

        /**
         * Belirli tarih aralığındaki hareket sayısını verir.
         * Filtreli listelemelerde pagination için kullanıyoruz.
         */
        long countByAccountIdAndPostingDateBetween(
                        String accountId,
                        LocalDate startDate,
                        LocalDate endDate);

        /**
         * Belirli bir tarih aralığında hesaptan çıkan toplam para miktarını (DEBIT)
         * hesaplar.
         * Müşteriye "bu ay ne kadar harcadım" bilgisini göstermek için kullanışlı.
         */
        @Query("SELECT COALESCE(SUM(e.amount), 0) " +
                        "FROM LedgerEntry e " +
                        "WHERE e.accountId = :accountId " +
                        "AND e.entryType = com.triobank.ledger.domain.model.EntryType.DEBIT " +
                        "AND e.postingDate BETWEEN :startDate AND :endDate")
        BigDecimal sumDebitsByAccountAndDateRange(
                        @Param("accountId") String accountId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Belirli bir tarih aralığında hesaba giren toplam para miktarını (CREDIT)
         * hesaplar.
         * Gelir/Gider raporlarında gelir tarafını oluşturur.
         */
        @Query("SELECT COALESCE(SUM(e.amount), 0) " +
                        "FROM LedgerEntry e " +
                        "WHERE e.accountId = :accountId " +
                        "AND e.entryType = com.triobank.ledger.domain.model.EntryType.CREDIT " +
                        "AND e.postingDate BETWEEN :startDate AND :endDate")
        BigDecimal sumCreditsByAccountAndDateRange(
                        @Param("accountId") String accountId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
