package com.triobank.ledger.service;

import com.triobank.ledger.domain.model.AccountBalance;
import com.triobank.ledger.domain.model.EntryType;
import com.triobank.ledger.domain.model.LedgerEntry;
import com.triobank.ledger.domain.model.LedgerTransaction;
import com.triobank.ledger.domain.model.TransactionStatus;
import com.triobank.ledger.domain.service.DoubleEntryValidator;
import com.triobank.ledger.dto.response.BalanceUpdateDto;
import com.triobank.ledger.exception.TransactionAlreadyExistsException;
import com.triobank.ledger.exception.TransactionNotFoundException;
import com.triobank.ledger.repository.AccountBalanceRepository;
import com.triobank.ledger.repository.LedgerEntryRepository;
import com.triobank.ledger.repository.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * LedgerService - Muhasebe kayıtlarının (Defter-i Kebir) yönetildiği ana
 * servis.
 * 
 * Burası işin kalbi. Transaction Service'ten gelen işlem event'lerini alıp
 * burada muhasebeleştiriyoruz (LedgerEntry). Ayrıca bakiye güncellemeleri
 * ve SAGA pattern gereği gitmesi gereken cevaplar buradan yönetiliyor.
 * 
 * Concurrency kontrolü çok kritik olduğu için AccountBalance güncellemelerinde
 * Pessimistic Locking kullanıyoruz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

        private final LedgerTransactionRepository transactionRepository;
        private final LedgerEntryRepository entryRepository;
        private final AccountBalanceRepository balanceRepository;
        private final DoubleEntryValidator doubleEntryValidator;
        private final OutboxService outboxService;

        /**
         * Transaction Service'ten gelen "TransactionStarted" event'ini işler.
         * 
         * Burada yapılanlar:
         * 1. Transaction daha önce işlendi mi kontrol et (Idempotency)
         * 2. Çift taraflı kayıt kuralını doğrula (Debit = Credit)
         * 3. LedgerTransaction ve LedgerEntry'leri oluşturup kaydet
         * 4. Bakiyeleri güvenli şekilde güncelle (Lock ile)
         * 5. Başarılı olduysa "TransactionPosted" event'ini fırlat (SAGA devam etsin)
         */
        @Transactional
        public void recordTransaction(com.triobank.ledger.dto.event.incoming.TransactionStartedEvent.Payload request) {
                log.info("Recording transaction: {}", request.getTransactionId());

                checkTransactionNotExists(request.getTransactionId());

                // Map to ValidationEntry for validator
                List<DoubleEntryValidator.ValidationEntry> validationEntries = request.getEntries().stream()
                                .map(dto -> new DoubleEntryValidator.ValidationEntry(
                                                dto.getSequence(),
                                                EntryType.valueOf(dto.getEntryType()),
                                                dto.getAmount(),
                                                dto.getCurrency()))
                                .toList();

                doubleEntryValidator.validate(validationEntries, request.getCurrency());

                LedgerTransaction transaction = createTransaction(request);
                List<LedgerEntry> entries = createEntries(transaction, request);
                entries.forEach(transaction::addEntry);

                transactionRepository.save(transaction);
                List<BalanceUpdateDto> balanceUpdates = updateBalances(entries);

                balanceUpdates.forEach(outboxService::publishBalanceUpdated);

                // Publish TransactionPostedEvent to Outbox (SAGA success)
                outboxService.publishTransactionPosted(
                                transaction.getTransactionId(),
                                transaction.getTransactionType(), // It is already a String
                                transaction.getTotalAmount(),
                                transaction.getCurrency(),
                                transaction.getPostingDate(),
                                entries.size());

                log.info("Transaction recorded successfully: {}", transaction.getTransactionId());
        }

        /**
         * İşlem iptali (Reversal) - SAGA Compensation
         * 
         * Eğer bir transaction iptal edildiyse (yetersiz bakiye, hata vs),
         * burada "Ters Kayıt" (Reversal Transaction) oluşturarak durumu düzeltiyoruz.
         * 
         * Asla silme (DELETE) yapmayız, her zaman ters kayıt atarız (Audit trail için).
         */
        @Transactional
        public void reverseTransaction(
                        com.triobank.ledger.dto.event.incoming.CompensationRequiredEvent.Payload request) {
                String transactionId = request.getTransactionId();
                String reversalId = request.getCompensationId(); // Using compensationId as reversal Transaction ID
                log.info("Reversing transaction: {}, reversalId: {}", transactionId, reversalId);

                LedgerTransaction originalTransaction = transactionRepository
                                .findByTransactionId(transactionId)
                                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

                if (originalTransaction.isReversed()) {
                        log.warn("Transaction already reversed: {}", transactionId);
                        return;
                }

                List<LedgerEntry> originalEntries = entryRepository.findByTransactionIdOrderBySequence(transactionId);
                LedgerTransaction reversalTransaction = createReversalTransaction(originalTransaction,
                                request.getReason(), reversalId);
                List<LedgerEntry> reversalEntries = createReversalEntries(reversalTransaction, originalEntries);
                reversalEntries.forEach(reversalTransaction::addEntry);

                transactionRepository.save(reversalTransaction);
                List<BalanceUpdateDto> balanceUpdates = updateBalances(reversalEntries);

                try {
                        originalTransaction.markAsReversed(reversalTransaction.getTransactionId());
                        transactionRepository.save(originalTransaction);
                        log.info("Original transaction {} marked as REVERSED", transactionId);
                } catch (IllegalStateException ex) {
                        log.warn("Transaction {} already reversed, skipping status update", transactionId);
                }

                balanceUpdates.forEach(outboxService::publishBalanceUpdated);

                outboxService.publishTransactionReversed(
                                transactionId,
                                reversalTransaction.getTransactionId(),
                                request.getReason());

                log.info("Transaction reversed successfully: {} -> {}",
                                transactionId, reversalTransaction.getTransactionId());
        }

        private void checkTransactionNotExists(String transactionId) {
                if (transactionRepository.existsByTransactionId(transactionId)) {
                        throw new TransactionAlreadyExistsException(transactionId);
                }
        }

        private LedgerTransaction createTransaction(
                        com.triobank.ledger.dto.event.incoming.TransactionStartedEvent.Payload request) {
                return LedgerTransaction.builder()
                                .transactionId(request.getTransactionId())
                                .transactionType(request.getTransactionType())
                                .postingDate(request.getPostingDate())
                                .valueDate(request.getValueDate())
                                .totalAmount(request.getTotalAmount())
                                .currency(request.getCurrency())
                                .status(TransactionStatus.POSTED)
                                .description(request.getDescription())
                                .initiatorAccountId(request.getInitiatorId())
                                .isReversal(false)
                                .build();
        }

        private List<LedgerEntry> createEntries(LedgerTransaction transaction,
                        com.triobank.ledger.dto.event.incoming.TransactionStartedEvent.Payload request) {
                List<LedgerEntry> entries = new ArrayList<>();

                for (com.triobank.ledger.dto.event.incoming.TransactionStartedEvent.EntryDto dto : request
                                .getEntries()) {
                        LedgerEntry entry = LedgerEntry.builder()
                                        .transaction(transaction)
                                        .sequence(dto.getSequence())
                                        .accountId(dto.getAccountId())
                                        .entryType(EntryType.valueOf(dto.getEntryType()))
                                        .amount(dto.getAmount())
                                        .currency(dto.getCurrency())
                                        .description(dto.getDescription())
                                        .referenceNumber(dto.getReferenceNumber())
                                        .transactionType(transaction.getTransactionType())
                                        .postingDate(request.getPostingDate())
                                        .valueDate(request.getValueDate())
                                        .build();

                        entries.add(entry);
                }

                return entries;
        }

        private LedgerTransaction createReversalTransaction(LedgerTransaction original,
                        String reason, String reversalId) {
                return LedgerTransaction.builder()
                                .transactionId(reversalId)
                                .transactionType(original.getTransactionType())
                                .transactionDate(Instant.now())
                                .postingDate(LocalDate.now())
                                .valueDate(LocalDate.now())
                                .totalAmount(original.getTotalAmount())
                                .currency(original.getCurrency())
                                .status(TransactionStatus.POSTED)
                                .description("Reversal: " + reason)
                                .initiatorAccountId(original.getInitiatorAccountId())
                                .isReversal(true)
                                .originalTransactionId(original.getTransactionId())
                                .build();
        }

        private List<LedgerEntry> createReversalEntries(LedgerTransaction reversalTransaction,
                        List<LedgerEntry> originalEntries) {
                List<LedgerEntry> reversalEntries = new ArrayList<>();

                for (LedgerEntry original : originalEntries) {
                        LedgerEntry reversal = LedgerEntry.builder()
                                        .transaction(reversalTransaction)
                                        .sequence(original.getSequence())
                                        .accountId(original.getAccountId())
                                        .entryType(original.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT
                                                        : EntryType.DEBIT)
                                        .amount(original.getAmount())
                                        .currency(original.getCurrency())
                                        .description("Reversal: " + original.getDescription())
                                        .referenceNumber(original.getReferenceNumber())
                                        .transactionType(original.getTransactionType())
                                        .postingDate(reversalTransaction.getPostingDate())
                                        .valueDate(reversalTransaction.getValueDate())
                                        .build();

                        reversalEntries.add(reversal);
                }

                return reversalEntries;
        }

        /**
         * Hesap bakiyelerini günceller.
         * 
         * EN KRİTİK METOD BURASI.
         * AccountBalanceRepository.findByAccountIdWithLock() kullanarak database
         * seviyesinde
         * row-lock alıyoruz. Böylece aynı anda iki işlem gelirse biri bekler,
         * race condition oluşmaz ve bakiye tutarlı kalır.
         */
        private List<BalanceUpdateDto> updateBalances(List<LedgerEntry> entries) {
                Map<String, BigDecimal> deltaMap = new HashMap<>();

                for (LedgerEntry entry : entries) {
                        BigDecimal delta = entry.getSignedAmount();
                        deltaMap.merge(entry.getAccountId(), delta, BigDecimal::add);
                }

                List<BalanceUpdateDto> updates = new ArrayList<>();

                for (Map.Entry<String, BigDecimal> deltaEntry : deltaMap.entrySet()) {
                        String accountId = deltaEntry.getKey();
                        BigDecimal delta = deltaEntry.getValue();

                        AccountBalance balance = balanceRepository
                                        .findByAccountIdWithLock(accountId)
                                        .orElseGet(() -> {
                                                log.info("Creating new balance record for account: {}", accountId);
                                                return createNewBalance(accountId, entries.get(0).getCurrency());
                                        });

                        BigDecimal previousBalance = balance.getBalance();
                        balance.updateBalance(delta, entries.get(0).getId());
                        balanceRepository.save(balance);

                        updates.add(BalanceUpdateDto.builder()
                                        .accountId(accountId)
                                        .previousBalance(previousBalance)
                                        .newBalance(balance.getBalance())
                                        .delta(delta)
                                        .build());

                        log.debug("Balance updated: accountId={}, previous={}, new={}, delta={}",
                                        accountId, previousBalance, balance.getBalance(), delta);
                }

                return updates;
        }

        private AccountBalance createNewBalance(String accountId, String currency) {
                return AccountBalance.builder()
                                .accountId(accountId)
                                .balance(BigDecimal.ZERO)
                                .currency(currency)
                                .build();
        }

        /**
         * Transaction detaylarını getir (tüm entry'leri ile)
         * 
         * @param transactionId Transaction ID
         * @return Transaction detayı
         */
        @Transactional(readOnly = true)
        public com.triobank.ledger.dto.response.TransactionDetailResponse getTransactionDetail(String transactionId) {
                log.debug("Getting transaction detail for: {}", transactionId);

                // Transaction'ı bul
                LedgerTransaction transaction = transactionRepository
                                .findByTransactionId(transactionId)
                                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

                // Entry'leri map et
                List<com.triobank.ledger.dto.response.TransactionDetailResponse.TransactionEntryDto> entryDtos = transaction
                                .getEntries().stream()
                                .map(entry -> com.triobank.ledger.dto.response.TransactionDetailResponse.TransactionEntryDto
                                                .builder()
                                                .sequence(entry.getSequence())
                                                .accountId(entry.getAccountId())
                                                .entryType(entry.getEntryType().name())
                                                .amount(entry.getAmount())
                                                .currency(entry.getCurrency())
                                                .description(entry.getDescription())
                                                .referenceNumber(entry.getReferenceNumber())
                                                .build())
                                .toList();

                // Response oluştur
                return com.triobank.ledger.dto.response.TransactionDetailResponse.builder()
                                .transactionId(transaction.getTransactionId())
                                .transactionType(transaction.getTransactionType())
                                .totalAmount(transaction.getTotalAmount())
                                .currency(transaction.getCurrency())
                                .status(transaction.getStatus())
                                .postingDate(transaction.getPostingDate())
                                .valueDate(transaction.getValueDate())
                                .description(transaction.getDescription())
                                .initiatorAccountId(transaction.getInitiatorAccountId())
                                .reversedByTransactionId(transaction.getReversedByTransactionId())
                                .entries(entryDtos)
                                .createdAt(transaction.getCreatedAt())
                                .build();
        }

        /**
         * Create initial balance for new account (0.00)
         * Called when AccountCreatedEvent is received
         */
        @Transactional
        public void createInitialBalance(String accountId, String currency) {
                log.info("Creating initial balance for account: {}, currency: {}", accountId, currency);

                // Check if balance already exists (idempotency)
                if (balanceRepository.findByAccountId(accountId).isPresent()) {
                        log.warn("Balance already exists for account: {}, skipping creation", accountId);
                        return;
                }

                // Create balance with 0.00
                AccountBalance balance = AccountBalance.builder()
                                .accountId(accountId)
                                .currency(currency)
                                .balance(BigDecimal.ZERO)
                                .lastUpdatedAt(Instant.now())
                                .build();

                balanceRepository.save(balance);

                log.info("Initial balance created for account: {}", accountId);
        }

        /**
         * Freeze balance for deleted account
         * Called when AccountDeletedEvent is received
         */
        @Transactional
        public void freezeBalance(String accountId) {
                log.info("Freezing balance for deleted account: {}", accountId);

                // Use pessimistic lock to prevent race conditions
                AccountBalance balance = balanceRepository.findByAccountIdWithLock(accountId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Cannot freeze balance - account not found: " + accountId));

                // Balance should be zero before deletion
                if (balance.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                        log.error("CRITICAL: Attempting to freeze account with non-zero balance! accountId={}, balance={}",
                                        accountId, balance.getBalance());
                        throw new IllegalStateException("Cannot freeze account with non-zero balance: " + accountId);
                }

                // Mark as frozen (prevent future updates)
                // In production, you might add a "frozen" flag to AccountBalance entity
                log.info("Balance frozen for deleted account: {} (balance was {})", accountId,
                                balance.getBalance());

                // In production: balance.setFrozen(true); balanceRepository.save(balance);
        }
}
