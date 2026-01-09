package com.triobank.transaction.repository;

import com.triobank.transaction.domain.model.Transaction;
import com.triobank.transaction.domain.model.TransactionStatus;
import com.triobank.transaction.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository
 * 
 * Spring Data JPA repository for Transaction entity.
 * Includes custom queries optimized for common use cases.
 * 
 * N+1 Prevention: All queries are optimized to avoid N+1 select problems.
 * 
 * Pattern: Copied from AccountRepository and LedgerTransactionRepository
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // ============================================
    // Idempotency & Uniqueness Checks
    // ============================================

    /**
     * Check if transaction exists by idempotency key (for duplicate prevention)
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find transaction by idempotency key (for idempotent requests)
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find transaction by idempotency key WITH PESSIMISTIC WRITE LOCK
     * 
     * ISSUE #6 FIX: Prevents race conditions during idempotency checks.
     * 
     * Usage: Transaction creation - locks row during check-then-insert.
     * SQL: SELECT ... FOR UPDATE
     * 
     * This ensures that concurrent requests with same idempotencyKey
     * will be serialized - only one can proceed at a time.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.idempotencyKey = :key")
    Optional<Transaction> findByIdempotencyKeyWithLock(@Param("key") String idempotencyKey);

    /**
     * Check if transaction exists by reference number
     */
    boolean existsByReferenceNumber(String referenceNumber);

    /**
     * Find transaction by reference number
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    // ============================================
    // Account-Based Queries (Customer Transactions)
    // ============================================

    /**
     * Find all transactions for a specific account (as sender or receiver)
     * Paginated for performance
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") String accountId, Pageable pageable);

    /**
     * Find transactions FROM an account (outgoing)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.fromAccountId = :accountId " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByFromAccountId(@Param("accountId") String accountId, Pageable pageable);

    /**
     * Find transactions TO an account (incoming)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.toAccountId = :accountId " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByToAccountId(@Param("accountId") String accountId, Pageable pageable);

    /**
     * Find transactions by account and status
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
            "AND t.status = :status " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndStatus(
            @Param("accountId") String accountId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    /**
     * Find transactions by account within date range
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    // ============================================
    // Card-Based Queries
    // ============================================

    /**
     * Find all transactions for a specific card
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.cardId = :cardId " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByCardId(@Param("cardId") String cardId, Pageable pageable);

    /**
     * Find card transactions by status
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.cardId = :cardId AND t.status = :status " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByCardIdAndStatus(
            @Param("cardId") String cardId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    // ============================================
    // Initiator-Based Queries (User Activity Tracking)
    // ============================================

    /**
     * Find transactions initiated by a specific user
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.initiatorId = :initiatorId " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByInitiatorId(@Param("initiatorId") String initiatorId, Pageable pageable);

    /**
     * Find pending transactions by initiator (for monitoring)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.initiatorId = :initiatorId AND t.status = 'PENDING' " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findPendingByInitiatorId(@Param("initiatorId") String initiatorId);

    // ============================================
    // Status-Based Queries (Operations & Monitoring)
    // ============================================

    /**
     * Find all transactions by status (paginated)
     */
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    /**
     * Find all PENDING transactions (for timeout monitoring)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.status = 'PENDING' " +
            "ORDER BY t.createdAt ASC")
    List<Transaction> findAllPending();

    /**
     * Find PENDING transactions older than specified time (for timeout detection)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.status = 'PENDING' AND t.createdAt < :threshold " +
            "ORDER BY t.createdAt ASC")
    List<Transaction> findPendingOlderThan(@Param("threshold") Instant threshold);

    /**
     * Find FAILED transactions (for analysis)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.status = 'FAILED' " +
            "ORDER BY t.failedAt DESC")
    Page<Transaction> findAllFailed(Pageable pageable);

    /**
     * Find failed transactions by failure reason
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.status = 'FAILED' AND t.failureReason = :reason " +
            "ORDER BY t.failedAt DESC")
    Page<Transaction> findFailedByReason(@Param("reason") String reason, Pageable pageable);

    // ============================================
    // Type-Based Queries
    // ============================================

    /**
     * Find transactions by type
     */
    Page<Transaction> findByTransactionType(TransactionType type, Pageable pageable);

    /**
     * Find transactions by type and status
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.transactionType = :type AND t.status = :status " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByTypeAndStatus(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    // ============================================
    // Ledger Integration Queries
    // ============================================

    /**
     * Find transaction by ledger transaction ID (for reconciliation)
     */
    Optional<Transaction> findByLedgerTransactionId(String ledgerTransactionId);

    /**
     * Find transactions without ledger ID (not yet posted)
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.ledgerTransactionId IS NULL AND t.status = 'PENDING' " +
            "ORDER BY t.createdAt ASC")
    List<Transaction> findWithoutLedgerTransaction();

    // ============================================
    // Statistics & Reporting Queries
    // ============================================

    /**
     * Count transactions by status
     */
    long countByStatus(TransactionStatus status);

    /**
     * Count transactions by account
     */
    @Query("SELECT COUNT(t) FROM Transaction t " +
            "WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId")
    long countByAccountId(@Param("accountId") String accountId);

    /**
     * Count transactions by type
     */
    long countByTransactionType(TransactionType type);
}
