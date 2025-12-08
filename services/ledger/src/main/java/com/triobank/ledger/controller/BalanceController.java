package com.triobank.ledger.controller;

import com.triobank.ledger.domain.model.EntryType;
import com.triobank.ledger.dto.response.AccountBalanceResponse;
import com.triobank.ledger.dto.response.AccountStatementResponse;
import com.triobank.ledger.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * BalanceController - Bakiye ve hesap hareketleri sorgulama API'si.
 * 
 * Burası "Read-Only" bir ön yüzdür. Sadece veri okur, asla veri değiştirmez.
 * 
 * Base URL: /api/v1/ledger
 */
@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Balance", description = "Balance Query API")
public class BalanceController {

    private final BalanceService balanceService;

    /**
     * Hesap bakiyesini anlık olarak sorgular.
     * 
     * AccountBalance tablosundaki (Cache) en güncel bakiyeyi döner.
     * Bu sorgu çok hızlıdır ve database'i yormaz.
     * 
     * GET /api/v1/ledger/balances/{accountId}
     */
    @GetMapping("/balances/{accountId}")
    @Operation(summary = "Get account balance", description = "Returns current balance for an account")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
            @PathVariable String accountId) {

        log.debug("Getting balance for account: {}", accountId);

        AccountBalanceResponse response = balanceService.getAccountBalance(accountId);

        return ResponseEntity.ok(response);
    }

    /**
     * Hesap ekstresi (Statement) ve işlem geçmişini döner.
     * 
     * Müşterinin hesap hareketlerini detaylı olarak listeler.
     * "Running Balance" özelliği ile her işlemden sonraki bakiyeyi de
     * görebilirsiniz.
     * 
     * GET /api/v1/ledger/accounts/{accountId}/statement
     * 
     * @param startDate Başlangıç tarihi (opsiyonel) -> Örn: 2024-01-01
     * @param endDate   Bitiş tarihi (opsiyonel) -> Örn: 2024-01-31
     * @param type      Filtre: DEBIT (Borç), CREDIT (Alacak) veya boş (Hepsi)
     * @param page      Sayfa no (default 0)
     * @param size      Sayfa boyutu (default 20)
     */
    @GetMapping("/accounts/{accountId}/statement")
    @Operation(summary = "Get account statement", description = "Returns account transaction history with optional running balance, filtering, and pagination")
    public ResponseEntity<AccountStatementResponse> getAccountStatement(
            @PathVariable String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) EntryType type,
            @RequestParam(defaultValue = "true") boolean includeRunningBalance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info(
                "Getting statement for account: {}, startDate: {}, endDate: {}, type: {}, includeBalance: {}, page: {}, size: {}",
                accountId, startDate, endDate, type, includeRunningBalance, page, size);

        // En yeni işlemler en üstte olsun (DESC)
        Pageable pageable = PageRequest.of(page, size, Sort.by("postingDate").descending());

        AccountStatementResponse response = balanceService.getAccountStatement(
                accountId,
                startDate,
                endDate,
                type,
                includeRunningBalance,
                pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * [ADMIN & DEBUG] Bakiye hesaplama (Re-calculation).
     * 
     * DİKKAT: Bu metod cache'i atlayıp, tüm transactionları baştan sona toplayarak
     * hesaplama yapar. Database'i yorabilir, sadece tutarsızlık şüphesi varsa
     * kullanılmalı.
     * 
     * GET /api/v1/ledger/balances/{accountId}/calculate
     */
    @GetMapping("/balances/{accountId}/calculate")
    @Operation(summary = "Calculate balance", description = "Calculates balance from ledger entries (for reconciliation)")
    public ResponseEntity<Map<String, Object>> calculateBalance(
            @PathVariable String accountId,
            @RequestParam(required = false) LocalDate upToDate) {

        log.debug("Calculating balance for account: {}", accountId);

        BigDecimal calculatedBalance;

        if (upToDate != null) {
            calculatedBalance = balanceService.calculateBalanceUpToDate(accountId, upToDate);
        } else {
            calculatedBalance = balanceService.calculateBalance(accountId);
        }

        // Karşılaştırma için cache'deki bakiyeyi de alalım
        AccountBalanceResponse cachedBalance = balanceService.getAccountBalance(accountId);

        Map<String, Object> response = Map.of(
                "accountId", accountId,
                "calculatedBalance", calculatedBalance,
                "cachedBalance", cachedBalance.getBalance(),
                "difference", calculatedBalance.subtract(cachedBalance.getBalance()).abs(),
                "upToDate", upToDate != null ? upToDate : "all",
                "matches", calculatedBalance.compareTo(cachedBalance.getBalance()) == 0);

        return ResponseEntity.ok(response);
    }

    /**
     * [ADMIN & DEBUG] Bakiye Mutabakatı (Reconciliation).
     * 
     * Cache'deki bakiye ile, entry'lerden hesaplanan bakiye tutuyor mu?
     * Periyodik kontrollerde çağrılabilir.
     * 
     * GET /api/v1/ledger/balances/{accountId}/reconcile
     */
    @GetMapping("/balances/{accountId}/reconcile")
    @Operation(summary = "Reconcile balance", description = "Checks if cached balance matches calculated balance")
    public ResponseEntity<Map<String, Object>> reconcileBalance(
            @PathVariable String accountId) {

        log.info("Reconciling balance for account: {}", accountId);

        boolean matches = balanceService.reconcileBalance(accountId);

        AccountBalanceResponse cachedBalance = balanceService.getAccountBalance(accountId);
        BigDecimal calculatedBalance = balanceService.calculateBalance(accountId);

        Map<String, Object> response = Map.of(
                "accountId", accountId,
                "matches", matches,
                "cachedBalance", cachedBalance.getBalance(),
                "calculatedBalance", calculatedBalance,
                "difference", calculatedBalance.subtract(cachedBalance.getBalance()).abs());

        return ResponseEntity.ok(response);
    }
}
