package com.triobank.account.service;

import com.triobank.account.domain.exception.AccountNotFoundException;
import com.triobank.account.domain.exception.DomainException;
import com.triobank.account.domain.model.Account;
import com.triobank.account.domain.model.AccountStatus;
import com.triobank.account.domain.model.ProductDefinition;
import com.triobank.account.dto.request.CreateAccountRequest;
import com.triobank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hesap Yönetim Servisi (Core Business Logic)
 * 
 * Hesap yaşam döngüsünü (Açılış, Kapanış, Dondurma) yönetir.
 * Tüm işlemler Transactional'dır ve tutarlılık esastır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

        private final AccountRepository accountRepository;
        private final ProductService productService; // Doğrudan Repo yerine Service kullanıyoruz (Best Practice)
        private final OutboxService outboxService;

        /**
         * Yeni bir hesap açar.
         * 
         * Süreç:
         * 1. Validasyon (Ürün var mı?)
         * 2. Entity Üretimi (Unique IBAN)
         * 3. Kayıt (DB Insert)
         * 4. Event Fırlatma (Outbox Insert)
         * Hepsi tek bir veritabanı transaction'ı içindedir (Atomicity).
         */
        @Transactional
        public Account createAccount(CreateAccountRequest request) {
                log.info("Creating account for customer: {}", request.getCustomerId());

                String productCode = request.getProductCode();

                ProductDefinition product = productService.getProductByCode(productCode)
                                .orElseThrow(() -> new DomainException("Product not found: " + productCode));

                // IBAN Üretimi: MOD-97 algoritması ile geçerli TR IBAN'ı oluşturuluyor.
                String accountNumber = com.triobank.account.util.IbanUtil.generateIban();

                Account account = Account.builder()
                                .customerId(request.getCustomerId())
                                .productCode(product.getCode())
                                .accountNumber(accountNumber)
                                .currency(request.getCurrency())
                                .status(AccountStatus.ACTIVE)
                                .build();

                // MERGE LOGIC: Default Configuration + Request Overrides
                Map<String, Object> finalConfiguration = new HashMap<>();

                // Step 1: Copy product defaults (template)
                if (product.getDefaultConfiguration() != null && !product.getDefaultConfiguration().isEmpty()) {
                        finalConfiguration.putAll(product.getDefaultConfiguration());
                        log.info("Loaded {} default settings from product {}",
                                        finalConfiguration.size(), productCode);
                }

                // Step 2: Apply request overrides (if provided)
                if (request.getConfigurations() != null && !request.getConfigurations().isEmpty()) {
                        finalConfiguration.putAll(request.getConfigurations());
                        log.info("Applied {} custom overrides from request",
                                        request.getConfigurations().size());
                }

                // Step 3: Set merged configuration to account
                if (!finalConfiguration.isEmpty()) {
                        account.updateConfiguration(finalConfiguration);
                        log.info("Account configuration initialized with {} total settings",
                                        finalConfiguration.size());
                } else {
                        log.warn("Product {} has no default configuration defined", productCode);
                }

                Account savedAccount = accountRepository.save(account);

                // Publish AccountCreated event with flat payload (Ledger pattern)
                outboxService.publishAccountCreated(
                                savedAccount.getId(),
                                savedAccount.getAccountNumber(),
                                savedAccount.getCustomerId(),
                                product.getCategory().name(),
                                request.getCurrency(),
                                savedAccount.getStatus().name(),
                                "SYSTEM",
                                savedAccount.getCreatedAt());

                // Hesabı hemen aktif et ve güncelle
                // Hesap zaten ACTIVE olarak oluşturuldu
                // savedAccount.activate(); -> Gerek kalmadı
                accountRepository.save(savedAccount); // Dirty checking yapar ama explicit save güvenlidir.

                log.info("Account created successfully: {}", savedAccount.getId());
                return savedAccount;
        }

        /**
         * Hesap durumunu değiştirir (Freeze/Unfreeze/Close).
         * 
         * Hesap kapatılırken bakiye kontrolü yapılır.
         * Sadece bakiyesi 0 olan hesaplar kapatılabilir.
         */
        @Transactional
        public void changeStatus(String accountId, AccountStatus newStatus, String reason) {
                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new DomainException("Account not found"));

                AccountStatus oldStatus = account.getStatus();

                // Hesap kapatılırken bakiye kontrolü
                if (newStatus == AccountStatus.CLOSED) {
                        if (account.getBalance() != null && account.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
                                throw new DomainException("Hesap kapatılamaz. Hesapta para bulunmaktadır. Hesabı kapatmak için bakiyenin 0 olması gerekir.");
                        }
                }

                switch (newStatus) {
                        case ACTIVE -> account.activate();
                        case CLOSED -> account.close();
                        default -> throw new DomainException("Invalid status transition to " + newStatus);
                }

                accountRepository.save(account);

                // Publish AccountStatusChanged event with flat payload (Ledger pattern)
                outboxService.publishAccountStatusChanged(
                                account.getId(),
                                oldStatus.name(),
                                account.getStatus().name(),
                                reason,
                                "SYSTEM");

                log.info("Account status changed: id={}, old={}, new={}", accountId, oldStatus, newStatus);
        }

        /**
         * Hesap detayını getirir.
         * Transactional(readOnly = true): Hibernate'in Dirty Checking mekanizmasını
         * kapatır, performans artar.
         */
        @Transactional(readOnly = true)
        public Account getAccount(String accountId) {
                return accountRepository.findById(accountId)
                                .orElseThrow(() -> new DomainException("Account not found"));
        }

        @Transactional(readOnly = true)
        public List<Account> getCustomerAccounts(String customerId, List<AccountStatus> status) {
                if (status == null || status.isEmpty()) {
                        return accountRepository.findByCustomerId(customerId);
                }
                return accountRepository.findByCustomerIdAndStatusIn(customerId, status);
        }

        /**
         * Bakiye Güncelleme (Ledger Event Handler)
         * 
         * Ledger Service'den gelen BalanceUpdatedEvent ile çağrılır.
         * Account Service'deki bakiye projeksiyonunu günceller.
         * 
         * @param accountId  UUID of the account
         * @param newBalance New balance from Ledger
         */
        @Transactional
        public void updateBalance(String accountId, java.math.BigDecimal newBalance) {
                log.debug("Updating balance for accountId={}, newBalance={}", accountId, newBalance);

                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new DomainException("Account not found: " + accountId));

                account.updateBalance(newBalance);
                accountRepository.save(account);

                log.info("Balance updated successfully for accountId={}", accountId);
        }

        /**
         * Hesap Yapılandırmasını Günceller (PATCH - Deep Merge)
         * 
         * PATCH semantiği: Sadece verilen alanlar güncellenir, diğerleri korunur.
         * Nested/İç içe yapılar da recursive merge edilir.
         */
        @Transactional
        public void updateConfiguration(String accountId, java.util.Map<String, Object> configurations) {
                log.debug("Updating configuration for accountId={}", accountId);

                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new DomainException("Account not found: " + accountId));

                // Mevcut configuration'ı al
                java.util.Map<String, Object> existingConfig = account.getConfigurations() != null
                                ? account.getConfigurations()
                                : new java.util.HashMap<>();

                // DEEP MERGE: Mevcut + Yeni (sadece değişenler)
                java.util.Map<String, Object> mergedConfig = com.triobank.account.util.ConfigurationMergeUtil
                                .deepMerge(existingConfig, configurations);

                log.info("Configuration merge: {} existing + {} new = {} total",
                                existingConfig.size(),
                                configurations.size(),
                                mergedConfig.size());

                // Merged config'i kaydet
                account.updateConfiguration(mergedConfig);
                accountRepository.save(account);

                // Publish event
                outboxService.publishAccountConfigurationChanged(
                                account.getId(),
                                account.getCustomerId(),
                                existingConfig, // Previously: previousConfig
                                mergedConfig); // Previously: configurations

                log.info("Configuration updated successfully for accountId={}", accountId);
        }

        /**
         * Transaction Service için hesap validasyonu
         * 
         * Hesabın işlem yapabilir durumda olup olmadığını, ürün kurallarını
         * ve kullanıcı yapılandırmasını döner.
         * 
         * Hem UUID hem de IBAN (accountNumber) kabul eder.
         * IBAN formatındaysa (TR ile başlıyorsa), önce IBAN ile hesabı bulur.
         * 
         * @param accountIdOrIban Hesap ID (UUID) veya IBAN (accountNumber)
         * @return Validation bilgileri (durum, limitler, özellikler)
         */
        @Transactional(readOnly = true)
        public com.triobank.account.dto.response.AccountValidationResponse validateForTransaction(String accountIdOrIban) {
                log.info("Validating account for transaction: {}", accountIdOrIban);

                // Hesabı getir - IBAN formatındaysa (TR ile başlıyorsa) IBAN ile bul
                Account account;
                if (accountIdOrIban != null && accountIdOrIban.startsWith("TR")) {
                        // IBAN formatında - accountNumber ile bul
                        account = accountRepository.findByAccountNumber(accountIdOrIban)
                                        .orElseThrow(() -> new AccountNotFoundException("Account not found with IBAN: " + accountIdOrIban));
                } else {
                        // UUID formatında - id ile bul
                        account = accountRepository.findById(accountIdOrIban)
                                        .orElseThrow(() -> new AccountNotFoundException(accountIdOrIban));
                }

                // Ürün tanımını getir
                ProductDefinition product = productService.getProductByCode(account.getProductCode())
                                .orElseThrow(() -> new DomainException(
                                                "Product not found: " + account.getProductCode()));

                // Validation durumunu belirle
                boolean isValid = account.getStatus() == AccountStatus.ACTIVE;

                log.debug("Account validation result - ID: {}, Status: {}, Valid: {}",
                                accountIdOrIban, account.getStatus(), isValid);

                return com.triobank.account.dto.response.AccountValidationResponse.builder()
                                .isValid(isValid)
                                .accountStatus(account.getStatus().name())
                                .productCode(account.getProductCode())
                                .productCategory(product.getCategory() != null
                                                ? product.getCategory().name()
                                                : "UNKNOWN")
                                .currency(account.getCurrency())
                                .currentBalance(account.getBalance())
                                .productFeatures(product.getFeatures() != null
                                                ? product.getFeatures()
                                                : java.util.Collections.emptyMap())
                                .userConfiguration(account.getConfigurations() != null
                                                ? account.getConfigurations()
                                                : java.util.Collections.emptyMap())
                                .build();
        }
}
