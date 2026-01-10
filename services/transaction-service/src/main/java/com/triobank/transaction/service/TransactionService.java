package com.triobank.transaction.service;

import com.triobank.transaction.client.AccountServiceClient;
import com.triobank.transaction.client.CardServiceClient;
import com.triobank.transaction.client.LedgerServiceClient;
import com.triobank.transaction.domain.model.Transaction;
import com.triobank.transaction.domain.model.TransactionStatus;
import com.triobank.transaction.domain.model.TransactionType;
import com.triobank.transaction.dto.external.AccountValidationResponse;
import com.triobank.transaction.dto.external.CardAuthorizationResponse;
import com.triobank.transaction.dto.external.LedgerBalanceResponse;
import com.triobank.transaction.dto.request.CreatePurchaseRequest;
import com.triobank.transaction.dto.request.CreateTransferRequest;
import com.triobank.transaction.dto.request.CreateWithdrawalRequest;
import com.triobank.transaction.repository.OutboxEventRepository;
import com.triobank.transaction.repository.TransactionRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction Service - İş Mantığı (Business Logic)
 *
 * İşlem yaratılışını SAGA pattern ile yönetir:
 * 1. İsteği doğrula (idempotency, hesaplar, kartlar)
 * 2. İşlem kaydını oluştur (PENDING statüsünde)
 * 3. TransactionStartedEvent'i Kafka'ya at (Outbox üzerinden)
 * 4. Ledger (Defder) bu eventi işler
 * 5. Ledger TransactionPosted veya TransactionReversed döner
 * 6. LedgerEventListener işlem sonucuna göre statüyü günceller
 *
 * Pattern: AccountService ve CardService ile benzer
 *
 * NOT: SERIALIZABLE izolasyon seviyesi idempotency için race condition'ı önler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class TransactionService {

        private final TransactionRepository transactionRepository;
        private final OutboxService outboxService;
        private final AccountServiceClient accountServiceClient;
        private final CardServiceClient cardServiceClient;
        private final LedgerServiceClient ledgerServiceClient;

        // ============================================
        // Transaction Creation - TRANSFER
        // ============================================

        public Transaction createTransfer(CreateTransferRequest request) {
                log.info("Creating TRANSFER transaction: from={}, to={}, amount={}, idempotencyKey={}",
                                request.getFromAccountId(), request.getToAccountId(),
                                request.getAmount(), request.getIdempotencyKey());

                // 1. Idempotency (Mükerrerlik) kontrolü
                Optional<Transaction> existing = transactionRepository
                                .findByIdempotencyKeyWithLock(request.getIdempotencyKey());
                if (existing.isPresent()) {
                        log.warn("Mükerrer istek tespit edildi: idempotencyKey={}", request.getIdempotencyKey());
                        return existing.get();
                }

                // 2. GÖNDEREN hesabı doğrula
                AccountValidationResponse fromAccount = accountServiceClient
                                .validateAccount(request.getFromAccountId());
                if (!fromAccount.isValid()) {
                        throw new ValidationException("Invalid source account: " + request.getFromAccountId());
                }

                // 3. ALICI hesabı doğrula
                AccountValidationResponse toAccount = accountServiceClient.validateAccount(request.getToAccountId());
                if (!toAccount.isValid()) {
                        throw new ValidationException("Invalid destination account: " + request.getToAccountId());
                }

                // 3.1. Kendine transfer engeli
                if (request.getFromAccountId().equals(request.getToAccountId())) {
                        throw new ValidationException("Cannot transfer to the same account");
                }

                // 3.2. Para birimi kontrolü
                if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
                        throw new ValidationException(
                                        String.format("Currency mismatch: source account is %s but destination is %s",
                                                        fromAccount.getCurrency(), toAccount.getCurrency()));
                }

                // 4. Bakiye kontrolü (Optimistic - Son söz Ledger'ın)
                LedgerBalanceResponse balance = ledgerServiceClient.getAccountBalance(request.getFromAccountId());
                if (balance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                        throw new InsufficientBalanceException("Insufficient balance");
                }

                // 5. İşlem kaydını oluştur
                String transactionId = generateTransactionId();
                String referenceNumber = generateReferenceNumber();

                Transaction transaction = Transaction.builder()
                                .id(transactionId)
                                .idempotencyKey(request.getIdempotencyKey())
                                .transactionType(TransactionType.TRANSFER)
                                .status(TransactionStatus.PENDING)
                                .totalAmount(request.getAmount())
                                .currency(request.getCurrency() != null ? request.getCurrency() : "TRY")
                                .fromAccountId(request.getFromAccountId())
                                .toAccountId(request.getToAccountId())
                                .description(request.getDescription())
                                .initiatorId(request.getInitiatorId())
                                .referenceNumber(referenceNumber)
                                .build();

                transaction.setLedgerDates(LocalDate.now(), LocalDate.now());
                transactionRepository.save(transaction);

                // 6. TransactionStartedEvent yayınla (SAGA başlat)
                publishTransactionStartedEvent(transaction);

                log.info("TRANSFER transaction created: id={}, status=PENDING", transactionId);
                return transaction;
        }

        // ============================================
        // Transaction Creation - WITHDRAWAL
        // ============================================

        public Transaction createWithdrawal(CreateWithdrawalRequest request) {
                log.info("Creating WITHDRAWAL transaction: card={}, amount={}, atmId={}, idempotencyKey={}",
                                request.getCardId(), request.getAmount(), request.getAtmId(),
                                request.getIdempotencyKey());

                // 1. Idempotency (Mükerrerlik) kontrolü
                Optional<Transaction> existing = transactionRepository
                                .findByIdempotencyKeyWithLock(request.getIdempotencyKey());
                if (existing.isPresent()) {
                        log.warn("Duplicate request detected: idempotencyKey={}", request.getIdempotencyKey());
                        return existing.get();
                }

                // 2. PIN doğrulama
                boolean pinValid = cardServiceClient.validatePin(request.getCardId(), request.getPin());
                if (!pinValid) {
                        throw new ValidationException("Invalid PIN");
                }

                // 3. Kartın çekim yetkisini kontrol et (Limit vs)
                CardAuthorizationResponse cardAuth = cardServiceClient.authorizeCard(
                                request.getCardId(),
                                request.getAmount(),
                                "WITHDRAWAL",
                                "ATM");

                if (!cardAuth.isAuthorized()) {
                        throw new AuthorizationException("Card authorization declined: " + cardAuth.getDeclineReason());
                }

                String accountId = cardAuth.getAccountId();

                // 4. Bakiye kontrolü
                LedgerBalanceResponse balance = ledgerServiceClient.getAccountBalance(accountId);
                if (balance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                        throw new InsufficientBalanceException("Insufficient balance");
                }

                // 5. İşlem kaydını oluştur
                String transactionId = generateTransactionId();
                String referenceNumber = generateReferenceNumber();

                Transaction transaction = Transaction.builder()
                                .id(transactionId)
                                .idempotencyKey(request.getIdempotencyKey())
                                .transactionType(TransactionType.WITHDRAWAL)
                                .status(TransactionStatus.PENDING)
                                .totalAmount(request.getAmount())
                                .currency("TRY")
                                .fromAccountId(accountId)
                                .cardId(request.getCardId())
                                .atmId(request.getAtmId())
                                .location(request.getLocation())
                                .description("ATM Withdrawal - " + request.getAtmId())
                                .initiatorId(request.getInitiatorId())
                                .referenceNumber(referenceNumber)
                                .build();

                transaction.setLedgerDates(LocalDate.now(), LocalDate.now());
                transactionRepository.save(transaction);

                // 6. Event yayınla
                publishTransactionStartedEvent(transaction);

                log.info("WITHDRAWAL transaction created: id={}, status=PENDING", transactionId);
                return transaction;
        }

        // ============================================
        // Transaction Creation - PURCHASE
        // ============================================

        public Transaction createPurchase(CreatePurchaseRequest request) {
                log.info("Creating PURCHASE transaction: card={}, amount={}, merchant={}, idempotencyKey={}",
                                request.getCardId(), request.getAmount(), request.getMerchantName(),
                                request.getIdempotencyKey());

                // 1. Idempotency (Mükerrerlik) kontrolü
                Optional<Transaction> existing = transactionRepository
                                .findByIdempotencyKeyWithLock(request.getIdempotencyKey());
                if (existing.isPresent()) {
                        log.warn("Duplicate request detected: idempotencyKey={}", request.getIdempotencyKey());
                        return existing.get();
                }

                // 2. Kartın harcama yetkisini kontrol et
                CardAuthorizationResponse cardAuth = cardServiceClient.authorizeCard(
                                request.getCardId(),
                                request.getAmount(),
                                "PURCHASE",
                                request.isOnline() ? "ONLINE" : "POS");

                if (!cardAuth.isAuthorized()) {
                        throw new AuthorizationException("Card authorization declined: " + cardAuth.getDeclineReason());
                }

                String accountId = cardAuth.getAccountId();

                // 3. Bakiye kontrolü
                LedgerBalanceResponse balance = ledgerServiceClient.getAccountBalance(accountId);
                if (balance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                        throw new InsufficientBalanceException("Insufficient balance");
                }

                // 4. İşlem kaydını oluştur
                String transactionId = generateTransactionId();
                String referenceNumber = generateReferenceNumber();

                Transaction transaction = Transaction.builder()
                                .id(transactionId)
                                .idempotencyKey(request.getIdempotencyKey())
                                .transactionType(TransactionType.PURCHASE)
                                .status(TransactionStatus.PENDING)
                                .totalAmount(request.getAmount())
                                .currency("TRY")
                                .fromAccountId(accountId)
                                .cardId(request.getCardId())
                                .merchantName(request.getMerchantName())
                                .merchantCategory(request.getMerchantCategory())
                                .isOnline(request.isOnline())
                                .description("Purchase - " + request.getMerchantName())
                                .initiatorId(request.getInitiatorId())
                                .referenceNumber(referenceNumber)
                                .build();

                transaction.setLedgerDates(LocalDate.now(), LocalDate.now());
                transactionRepository.save(transaction);

                // 5. Event yayınla
                publishTransactionStartedEvent(transaction);

                log.info("PURCHASE transaction created: id={}, status=PENDING", transactionId);
                return transaction;
        }

        // ============================================
        // Event Publishing (SAGA)
        // ============================================

        private void publishTransactionStartedEvent(Transaction transaction) {
                log.debug("Publishing TransactionStartedEvent: id={}", transaction.getId());

                // Ledger kayıtlarını hazırla
                List<Map<String, Object>> entries = buildLedgerEntries(transaction);

                outboxService.publishTransactionStarted(
                                transaction.getId(),
                                transaction.getTransactionType().name(),
                                transaction.getFromAccountId(),
                                transaction.getToAccountId(),
                                transaction.getTotalAmount(),
                                transaction.getCurrency(),
                                transaction.getDescription(),
                                transaction.getPostingDate(),
                                transaction.getValueDate(),
                                transaction.getInitiatorId(),
                                transaction.getReferenceNumber(),
                                entries);
        }

        private List<Map<String, Object>> buildLedgerEntries(Transaction transaction) {
                List<Map<String, Object>> entries = new ArrayList<>();

                switch (transaction.getTransactionType()) {
                        case TRANSFER:
                                // Validate required fields for TRANSFER
                                if (transaction.getFromAccountId() == null) {
                                        throw new IllegalStateException(
                                                        "fromAccountId is required for TRANSFER transaction: "
                                                                        + transaction.getId());
                                }
                                if (transaction.getToAccountId() == null) {
                                        throw new IllegalStateException(
                                                        "toAccountId is required for TRANSFER transaction: "
                                                                        + transaction.getId());
                                }

                                // Debit from source account
                                entries.add(Map.of(
                                                "sequence", 1,
                                                "accountId", transaction.getFromAccountId(),
                                                "entryType", "DEBIT",
                                                "amount", transaction.getTotalAmount(),
                                                "currency", transaction.getCurrency(),
                                                "description", "Transfer to " + transaction.getToAccountId(),
                                                "referenceNumber", transaction.getReferenceNumber()));
                                // Credit to destination account
                                entries.add(Map.of(
                                                "sequence", 2,
                                                "accountId", transaction.getToAccountId(),
                                                "entryType", "CREDIT",
                                                "amount", transaction.getTotalAmount(),
                                                "currency", transaction.getCurrency(),
                                                "description", "Transfer from " + transaction.getFromAccountId(),
                                                "referenceNumber", transaction.getReferenceNumber()));
                                break;

                        case WITHDRAWAL:
                        case PURCHASE:
                                // Validate required fields for WITHDRAWAL/PURCHASE
                                if (transaction.getFromAccountId() == null) {
                                        throw new IllegalStateException("fromAccountId is required for " +
                                                        transaction.getTransactionType() + " transaction: "
                                                        + transaction.getId());
                                }

                                // Debit from account (single entry for withdrawals/purchases)
                                entries.add(Map.of(
                                                "sequence", 1,
                                                "accountId", transaction.getFromAccountId(),
                                                "entryType", "DEBIT",
                                                "amount", transaction.getTotalAmount(),
                                                "currency", transaction.getCurrency(),
                                                "description",
                                                transaction.getDescription() != null ? transaction.getDescription()
                                                                : "",
                                                "referenceNumber", transaction.getReferenceNumber()));
                                break;
                }

                return entries;
        }

        // ============================================
        // SAGA Callbacks (called by LedgerEventListener)
        // ============================================

        public void markAsCompleted(String transactionId, String ledgerTransactionId) {
                log.info("Marking transaction as COMPLETED: transactionId={}, ledgerTransactionId={}",
                                transactionId, ledgerTransactionId);

                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

                transaction.markAsCompleted(ledgerTransactionId);
                transactionRepository.save(transaction);

                log.info("Transaction marked as COMPLETED: {}", transactionId);
        }

        public void markAsFailed(String transactionId, String reason, String details, String step) {
                log.warn("Marking transaction as FAILED: transactionId={}, reason={}, step={}",
                                transactionId, reason, step);

                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

                transaction.markAsFailed(reason, details, step);
                transactionRepository.save(transaction);

                log.info("Transaction marked as FAILED: {}", transactionId);
        }

        // ============================================
        // Query Methods (for Controller)
        // ============================================

        @Transactional(readOnly = true)
        public Transaction getTransaction(String id) {
                return transactionRepository.findById(id)
                                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));
        }

        @Transactional(readOnly = true)
        public Transaction getByIdempotencyKey(String idempotencyKey) {
                return transactionRepository.findByIdempotencyKey(idempotencyKey)
                                .orElseThrow(() -> new TransactionNotFoundException(
                                                "Transaction not found: " + idempotencyKey));
        }

        @Transactional(readOnly = true)
        public Transaction getByReferenceNumber(String referenceNumber) {
                return transactionRepository.findByReferenceNumber(referenceNumber)
                                .orElseThrow(() -> new TransactionNotFoundException(
                                                "Transaction not found: " + referenceNumber));
        }

        @Transactional(readOnly = true)
        public Page<Transaction> getAccountTransactions(String accountId, Pageable pageable) {
                return transactionRepository.findByAccountId(accountId, pageable);
        }

        @Transactional(readOnly = true)
        public Page<Transaction> getAccountTransactions(String accountId, TransactionStatus status, Pageable pageable) {
                return transactionRepository.findByAccountIdAndStatus(accountId, status, pageable);
        }

        @Transactional(readOnly = true)
        public Page<Transaction> getCardTransactions(String cardId, Pageable pageable) {
                return transactionRepository.findByCardId(cardId, pageable);
        }

        @Transactional(readOnly = true)
        public Page<Transaction> getCardTransactions(String cardId, TransactionStatus status, Pageable pageable) {
                return transactionRepository.findByCardIdAndStatus(cardId, status, pageable);
        }

        @Transactional(readOnly = true)
        public Page<Transaction> getUserTransactions(String initiatorId, Pageable pageable) {
                return transactionRepository.findByInitiatorId(initiatorId, pageable);
        }

        @Transactional(readOnly = true)
        public List<Transaction> getPendingTransactions(String initiatorId) {
                return transactionRepository.findPendingByInitiatorId(initiatorId);
        }

        @Transactional(readOnly = true)
        public Page<Transaction> getFailedTransactions(String reason, Pageable pageable) {
                if (reason != null) {
                        return transactionRepository.findFailedByReason(reason, pageable);
                }
                return transactionRepository.findAllFailed(pageable);
        }

        @Transactional(readOnly = true)
        public Map<String, Object> getStatistics() {
                Map<String, Object> stats = new HashMap<>();

                stats.put("total", transactionRepository.count());
                stats.put("pending", transactionRepository.countByStatus(TransactionStatus.PENDING));
                stats.put("completed", transactionRepository.countByStatus(TransactionStatus.COMPLETED));
                stats.put("failed", transactionRepository.countByStatus(TransactionStatus.FAILED));

                stats.put("transfers", transactionRepository.countByTransactionType(TransactionType.TRANSFER));
                stats.put("withdrawals", transactionRepository.countByTransactionType(TransactionType.WITHDRAWAL));
                stats.put("purchases", transactionRepository.countByTransactionType(TransactionType.PURCHASE));

                return stats;
        }

        // ============================================
        // Utility Methods
        // ============================================

        private String generateTransactionId() {
                return "TXN-" + UUID.randomUUID().toString();
        }

        private String generateReferenceNumber() {
                return "REF-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
        }

        // ============================================
        // Custom Exceptions
        // ============================================

        public static class TransactionNotFoundException extends RuntimeException {
                public TransactionNotFoundException(String message) {
                        super(message);
                }
        }

        public static class ValidationException extends RuntimeException {
                public ValidationException(String message) {
                        super(message);
                }
        }

        public static class AuthorizationException extends RuntimeException {
                public AuthorizationException(String message) {
                        super(message);
                }
        }

        public static class InsufficientBalanceException extends RuntimeException {
                public InsufficientBalanceException(String message) {
                        super(message);
                }
        }
}
