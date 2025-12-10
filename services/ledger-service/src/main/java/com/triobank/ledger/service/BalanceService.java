package com.triobank.ledger.service;

import com.triobank.ledger.domain.model.AccountBalance;
import com.triobank.ledger.domain.model.EntryType;
import com.triobank.ledger.domain.model.LedgerEntry;
import com.triobank.ledger.dto.response.AccountBalanceResponse;
import com.triobank.ledger.dto.response.AccountStatementResponse;
import com.triobank.ledger.dto.response.PaginationMetadata;
import com.triobank.ledger.dto.response.StatementEntryResponse;
import com.triobank.ledger.exception.AccountNotFoundException;
import com.triobank.ledger.repository.AccountBalanceRepository;
import com.triobank.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BalanceService - Bakiye ve hesap ekstresi sorgulama servisi.
 * 
 * Burası tamamen Read-Only çalışır. Asla kayıt değiştirmez.
 * CQRS mantığında "Query" tarafı gibi düşünebiliriz.
 * 
 * Veriyi hızlıca `AccountBalance` tablosundan okur ama gerekirse
 * `LedgerEntry` tablosundan baştan sona hesaplama yaparak (reconciliation)
 * doğrulama da yapabilir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BalanceService {

        private final AccountBalanceRepository balanceRepository;
        private final LedgerEntryRepository entryRepository;

        /**
         * Hesap bakiyesini getir (cache'den)
         * 
         * @param accountId Hesap ID
         * @return Bakiye bilgisi
         */
        public AccountBalanceResponse getAccountBalance(String accountId) {
                log.debug("Getting balance for account: {}", accountId);

                AccountBalance balance = balanceRepository
                                .findByAccountId(accountId)
                                .orElseThrow(() -> new AccountNotFoundException(accountId));

                return AccountBalanceResponse.builder()
                                .accountId(balance.getAccountId())
                                .balance(balance.getBalance())
                                .currency(balance.getCurrency())
                                .lastEntryId(balance.getLastEntryId() != null
                                                ? ((java.util.UUID) balance.getLastEntryId()).getMostSignificantBits()
                                                : null)
                                .lastUpdatedAt(balance.getLastUpdatedAt())
                                .version(balance.getVersion() != null ? balance.getVersion().intValue() : null)
                                .build();
        }

        /**
         * Bakiye hesapla (entry'lerden - reconciliation için)
         * 
         * @param accountId Hesap ID
         * @return Hesaplanmış bakiye
         */
        public BigDecimal calculateBalance(String accountId) {
                log.debug("Calculating balance for account: {}", accountId);
                return entryRepository.calculateBalance(accountId);
        }

        /**
         * Belirli tarihe kadar bakiye hesapla
         * 
         * @param accountId Hesap ID
         * @param upToDate  Hangi tarihe kadar
         * @return Hesaplanmış bakiye
         */
        public BigDecimal calculateBalanceUpToDate(String accountId, LocalDate upToDate) {
                log.debug("Calculating balance for account {} up to {}", accountId, upToDate);
                return entryRepository.calculateBalanceUpToDate(accountId, upToDate);
        }

        /**
         * Hesap ekstresi (Account Statement) oluşturur.
         * 
         * Müşterinin hesap hareketlerini listeler. En önemli özellik:
         * "Running Balance" (Yürüyen Bakiye) hesaplaması.
         * Yani her işlemden sonra bakiyenin kaç olduğunu gösterir.
         * 
         * @param accountId             Hesap ID
         * @param startDate             Başlangıç tarihi (opsiyonel)
         * @param endDate               Bitiş tarihi (opsiyonel)
         * @param entryType             Sadece borçlar veya alacaklar (opsiyonel)
         * @param includeRunningBalance Yürüyen bakiye olsun mu? (Performans için
         *                              kapatılabilir)
         * @param pageable              Sayfalama
         * @return Detaylı ekstre
         */
        public AccountStatementResponse getAccountStatement(
                        String accountId,
                        LocalDate startDate,
                        LocalDate endDate,
                        EntryType entryType,
                        boolean includeRunningBalance,
                        Pageable pageable) {

                log.debug("Getting statement for account: {}, startDate: {}, endDate: {}, type: {}, includeBalance: {}",
                                accountId, startDate, endDate, entryType, includeRunningBalance);

                // Entry'leri çek (filtrelenmiş ve sayfalanmış)
                Page<LedgerEntry> entriesPage = getFilteredEntries(
                                accountId, startDate, endDate, entryType, pageable);

                // Currency bilgisi AccountBalance'tan al (doğru kaynak)
                String currency = "TRY"; // Default fallback
                try {
                        AccountBalance balance = balanceRepository
                                        .findByAccountId(accountId)
                                        .orElse(null);

                        if (balance != null) {
                                currency = balance.getCurrency();
                        } else if (entriesPage.hasContent()) {
                                // AccountBalance yoksa ilk entry'den al
                                currency = entriesPage.getContent().get(0).getCurrency();
                        }
                } catch (Exception e) {
                        log.warn("Could not get currency for account {}, using default TRY", accountId);
                }

                // Running balance hesapla (includeRunningBalance true ise)
                List<StatementEntryResponse> statementEntries = includeRunningBalance
                                ? buildEntriesWithRunningBalance(accountId, entriesPage.getContent())
                                : buildEntriesWithoutRunningBalance(entriesPage.getContent());

                // Pagination metadata
                PaginationMetadata pagination = PaginationMetadata.builder()
                                .page(entriesPage.getNumber())
                                .size(entriesPage.getSize())
                                .totalElements(entriesPage.getTotalElements())
                                .totalPages(entriesPage.getTotalPages())
                                .build();

                return AccountStatementResponse.builder()
                                .accountId(accountId)
                                .currency(currency)
                                .entries(statementEntries)
                                .pagination(pagination)
                                .build();
        }

        /**
         * Filtrelenmiş entry'leri getir
         */
        private Page<LedgerEntry> getFilteredEntries(
                        String accountId,
                        LocalDate startDate,
                        LocalDate endDate,
                        EntryType entryType,
                        Pageable pageable) {

                // Tarih ve tip filtresi yoksa
                if (startDate == null && endDate == null && entryType == null) {
                        return entryRepository.findByAccountId(accountId, pageable);
                }

                // Tarih filtresi varsa (tek tarih de destekle!)
                if (startDate != null || endDate != null) {
                        // Eksik tarihleri tamamla
                        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
                        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

                        return entryRepository.findByAccountIdAndPostingDateBetween(
                                        accountId, effectiveStart, effectiveEnd, pageable);
                }

                // Sadece tip filtresi varsa
                if (entryType != null) {
                        return entryRepository.findByAccountIdAndEntryType(accountId, entryType, pageable);
                }

                // Default: tüm entry'ler
                return entryRepository.findByAccountId(accountId, pageable);
        }

        /**
         * Running balance ile entry'leri oluştur (OPTIMIZED)
         * 
         * PERFORMANS İYİLEŞTİRMESİ:
         * - Eski: Tüm entry'leri çeker (100K entry için 100K kayıt)
         * - Yeni: Sadece başlangıç bakiyesini hesaplar (1 aggregate query)
         * - Sonuç: ~100x daha hızlı
         */
        private List<StatementEntryResponse> buildEntriesWithRunningBalance(
                        String accountId,
                        List<LedgerEntry> entries) {

                if (entries.isEmpty()) {
                        return new ArrayList<>();
                }

                // 1. Bu sayfadaki ilk entry'nin tarihini al
                // NOT: entries DESC sırada gelir (en yeni başta)
                LedgerEntry firstEntry = entries.get(entries.size() - 1); // En eski (ASC'de ilk)
                LocalDate firstDate = firstEntry.getPostingDate();

                // 2. Bu tarihten ÖNCE kalan bakiyeyi hesapla (tek aggregate query)
                BigDecimal startingBalance = entryRepository
                                .calculateBalanceUpToDate(accountId, firstDate.minusDays(1));

                // 3. Sadece bu sayfadaki entry'ler için running balance hesapla
                // NOT: ASC sırada işle (eski→yeni), sonra ters çevir
                List<StatementEntryResponse> result = new ArrayList<>();
                BigDecimal runningBalance = startingBalance;

                // DESC → ASC çevir (en eski başta olsun)
                List<LedgerEntry> sortedEntries = new ArrayList<>(entries);
                sortedEntries.sort((a, b) -> a.getPostingDate().compareTo(b.getPostingDate()));

                for (LedgerEntry entry : sortedEntries) {
                        runningBalance = runningBalance.add(entry.getSignedAmount());
                        result.add(buildStatementEntry(entry, runningBalance));
                }

                // 4. Sonucu ters çevir (DESC - en yeni başta)
                java.util.Collections.reverse(result);

                return result;
        }

        /**
         * Running balance olmadan entry'leri oluştur
         */
        private List<StatementEntryResponse> buildEntriesWithoutRunningBalance(
                        List<LedgerEntry> entries) {

                return entries.stream()
                                .map(entry -> buildStatementEntry(entry, null))
                                .toList();
        }

        /**
         * Statement entry response oluştur
         */
        private StatementEntryResponse buildStatementEntry(
                        LedgerEntry entry,
                        BigDecimal runningBalance) {

                return StatementEntryResponse.builder()
                                .entryId(((java.util.UUID) entry.getId()).getMostSignificantBits())
                                .transactionId(entry.getTransaction().getTransactionId())
                                .date(entry.getPostingDate())
                                .entryType(entry.getEntryType().name())
                                .amount(entry.getAmount())
                                .runningBalance(runningBalance)
                                .description(entry.getDescription())
                                .referenceNumber(entry.getReferenceNumber())
                                .build();
        }

        /**
         * Bakiye mutabakatı (cache vs gerçek)
         * 
         * @param accountId Hesap ID
         * @return Fark var mı?
         */
        public boolean reconcileBalance(String accountId) {
                AccountBalance cachedBalance = balanceRepository
                                .findByAccountId(accountId)
                                .orElseThrow(() -> new AccountNotFoundException(accountId));

                BigDecimal calculatedBalance = entryRepository.calculateBalance(accountId);

                boolean matches = cachedBalance.getBalance().compareTo(calculatedBalance) == 0;

                if (!matches) {
                        log.warn("Balance mismatch for account {}: cached={}, calculated={}",
                                        accountId, cachedBalance.getBalance(), calculatedBalance);
                }

                return matches;
        }
}
