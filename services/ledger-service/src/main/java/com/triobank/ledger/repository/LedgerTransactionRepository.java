package com.triobank.ledger.repository;

import com.triobank.ledger.domain.model.LedgerTransaction;
import com.triobank.ledger.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * LedgerTransactionRepository - Transaction verilerine erişim noktası.
 * 
 * Standart CRUD işlemleri Spring Data JPA tarafından hallediliyor.
 * Burada daha çok raporlama, geçmiş sorgulama ve analiz için gereken
 * özelleştirilmiş sorguları yazdım.
 */
@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, String> {

    // ========================================
    // Basic Queries (Spring Data otomatik)
    // ========================================

    /**
     * Transaction ID ile bul
     * 
     * @param transactionId Transaction ID
     * @return Transaction (varsa)
     */
    Optional<LedgerTransaction> findByTransactionId(String transactionId);

    /**
     * Transaction var mı kontrol et (idempotency için)
     * 
     * @param transactionId Transaction ID
     * @return true ise var
     */
    boolean existsByTransactionId(String transactionId);

    // ========================================
    // Status Queries
    // ========================================

    /**
     * Belirli status'taki transaction'ları bul
     * 
     * @param status Transaction status
     * @return Transaction listesi
     */
    List<LedgerTransaction> findByStatus(TransactionStatus status);

    /**
     * İptal edilmiş transaction'ları bul
     * 
     * @return REVERSED status'taki transaction'lar
     */
    default List<LedgerTransaction> findReversedTransactions() {
        return findByStatus(TransactionStatus.REVERSED);
    }

    // ========================================
    // Date Range Queries
    // ========================================

    /**
     * Tarih aralığında transaction'ları bul
     * 
     * @param startDate Başlangıç tarihi (dahil)
     * @param endDate   Bitiş tarihi (dahil)
     * @return Transaction listesi
     */
    List<LedgerTransaction> findByPostingDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Belirli bir tarihteki transaction'ları bul
     * 
     * @param date Tarih
     * @return Transaction listesi
     */
    List<LedgerTransaction> findByPostingDate(LocalDate date);

    // ========================================
    // Type Queries
    // ========================================

    /**
     * Transaction tipine göre bul
     * 
     * @param transactionType Transaction tipi (TRANSFER, CARD_PAYMENT, vb.)
     * @return Transaction listesi
     */
    List<LedgerTransaction> findByTransactionType(String transactionType);

    // ========================================
    // Account Queries
    // ========================================

    /**
     * Belirli hesabın başlattığı transaction'ları bul
     * 
     * @param accountId Hesap ID
     * @return Transaction listesi
     */
    List<LedgerTransaction> findByInitiatorAccountId(String accountId);

    // ========================================
    // Reversal Queries
    // ========================================

    /**
     * Orijinal transaction'ın reversal'ını bul
     * 
     * @param originalTransactionId Orijinal transaction ID
     * @return Reversal transaction (varsa)
     */
    Optional<LedgerTransaction> findByOriginalTransactionId(String originalTransactionId);

    /**
     * Reversal transaction'ları bul
     * 
     * @param isReversal true ise sadece reversal'lar
     * @return Transaction listesi
     */
    List<LedgerTransaction> findByIsReversal(Boolean isReversal);

    // ========================================
    // Complex Queries (JPQL)
    // ========================================

    /**
     * Belirli bir hesabı etkileyen transaction'ları getirir.
     * 
     * N+1 problemini önlemek için JOIN FETCH kullanıyorum.
     * Böylece transaction ile birlikte entry'leri de tek sorguda çekiyoruz.
     * 
     * @param accountId Hesap ID
     * @return Transaction listesi (Entry'leri dolu halde)
     */
    @Query("SELECT DISTINCT t FROM LedgerTransaction t " +
            "JOIN FETCH t.entries e " +
            "WHERE e.accountId = :accountId " +
            "ORDER BY t.postingDate DESC")
    List<LedgerTransaction> findTransactionsAffectingAccount(@Param("accountId") String accountId);

    /**
     * Belirli tarih aralığında, belirli tipteki işlem sayısını döner.
     * Raporlama ekranlarında istatistik göstermek için kullanıyoruz.
     * 
     * @param transactionType Transaction tipi
     * @param startDate       Başlangıç tarihi
     * @param endDate         Bitiş tarihi
     * @return Toplam işlem adedi
     */
    @Query("SELECT COUNT(t) FROM LedgerTransaction t " +
            "WHERE t.transactionType = :type " +
            "AND t.postingDate BETWEEN :startDate AND :endDate")
    long countByTypeAndDateRange(
            @Param("type") String transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
