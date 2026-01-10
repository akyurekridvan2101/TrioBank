package com.triobank.transaction.controller;

import com.triobank.transaction.domain.model.Transaction;
import com.triobank.transaction.domain.model.TransactionStatus;
import com.triobank.transaction.domain.model.TransactionType;
import com.triobank.transaction.dto.mapper.TransactionMapper;
import com.triobank.transaction.dto.request.CreatePurchaseRequest;
import com.triobank.transaction.dto.request.CreateTransferRequest;
import com.triobank.transaction.dto.request.CreateWithdrawalRequest;
import com.triobank.transaction.dto.response.TransactionResponse;
import com.triobank.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * İşlem (Transaction) Yönetim API Controller
 *
 * Para transferi, para çekme ve harcama işlemlerini yönetir.
 * Account ve Card controller ile benzer yapıda kurgulandı.
 *
 * Mimari:
 * - Controller: HTTP katmanı, validasyon, response formatlama
 * - Service: İş mantığı, orkestrasyon, SAGA koordinasyonu
 * - Repository: Veri erişimi
 */
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Transaction operations and management APIs")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    // ============================================
    // Transaction Creation Endpoints
    // ============================================

    /**
     * [POST] Transfer İşlemi Oluştur
     *
     * Hesaptan hesaba para transferi (EFT/Havale gibi).
     * Her iki hesabı doğrular, bakiyeyi kontrol eder ve SAGA sürecini başlatır.
     *
     * @param request Transfer detayları
     * @return Oluşturulan işlem (PENDING statüsünde)
     */
    @PostMapping("/transfer")
    @Operation(summary = "Create transfer transaction", description = "Transfers money from one account to another. Validates accounts and balance before initiating SAGA.")
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody CreateTransferRequest request) {

        Transaction transaction = transactionService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionMapper.toResponse(transaction));
    }

    /**
     * [POST] Para Çekme İşlemi (ATM)
     *
     * Banka kartı ile ATM'den para çekme senaryosu.
     * Kartı, PIN'i, günlük limiti ve bakiyeyi doğrular.
     *
     * @param request Çekim detayları
     * @return Oluşturulan işlem (PENDING statüsünde)
     */
    @PostMapping("/withdrawal")
    @Operation(summary = "Create withdrawal transaction", description = "Processes ATM withdrawal. Validates card, PIN, daily limits, and account balance.")
    public ResponseEntity<TransactionResponse> createWithdrawal(
            @Valid @RequestBody CreateWithdrawalRequest request) {

        Transaction transaction = transactionService.createWithdrawal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionMapper.toResponse(transaction));
    }

    /**
     * [POST] Alışveriş İşlemi (Purchase)
     *
     * Kart ile yapılan harcama (POS veya Online).
     * Kart yetkisini (limit vs) ve hesap bakiyesini kontrol eder.
     *
     * @param request Harcama detayları
     * @return Oluşturulan işlem (PENDING statüsünde)
     */
    @PostMapping("/purchase")
    @Operation(summary = "Create purchase transaction", description = "Processes card purchase at merchant. Validates card authorization and account balance.")
    public ResponseEntity<TransactionResponse> createPurchase(
            @Valid @RequestBody CreatePurchaseRequest request) {

        Transaction transaction = transactionService.createPurchase(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionMapper.toResponse(transaction));
    }

    // ============================================
    // Transaction Retrieval Endpoints
    // ============================================

    /**
     * [GET] İşlem Detayı Getir
     *
     * @param id İşlem ID
     * @return İşlem detayları
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details", description = "Retrieves details of a specific transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        Transaction transaction = transactionService.getTransaction(id);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    /**
     * [GET] Idempotency Key ile Getir
     *
     * Mükerrer işlem kontrolü için kullanılır.
     *
     * @param idempotencyKey İsteğe özel tekil anahtar
     * @return Varsa işlemi döner
     */
    @GetMapping("/by-idempotency-key/{idempotencyKey}")
    @Operation(summary = "Get transaction by idempotency key", description = "Retrieves transaction by idempotency key for duplicate detection")
    public ResponseEntity<TransactionResponse> getByIdempotencyKey(
            @PathVariable String idempotencyKey) {

        Transaction transaction = transactionService.getByIdempotencyKey(idempotencyKey);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    /**
     * [GET] Referans Numarası ile Getir
     *
     * Müşteri hizmetleri ve işlem sorgulama ekranları için.
     *
     * @param referenceNumber İşlem referans no
     * @return Varsa işlemi döner
     */
    @GetMapping("/by-reference/{referenceNumber}")
    @Operation(summary = "Get transaction by reference number", description = "Retrieves transaction by reference number for customer lookup")
    public ResponseEntity<TransactionResponse> getByReferenceNumber(
            @PathVariable String referenceNumber) {

        Transaction transaction = transactionService.getByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    // ============================================
    // Transaction Listing Endpoints (Paginated)
    // ============================================

    /**
     * [GET] Hesaba Ait İşlemler
     *
     * Bir hesabın tüm işlemlerini listeler (gönderen veya alan olarak).
     * Sayfalama ve opsiyonel statü filtresi destekler.
     *
     * Örnekler:
     * - /v1/transactions?accountId=acc-123
     * - /v1/transactions?accountId=acc-123&status=COMPLETED
     *
     * @param accountId Hesap ID (Zorunlu)
     * @param status    Statü filtresi (Opsiyonel)
     * @param pageable  Sayfalama parametreleri
     * @return Sayfalı işlem listesi
     */
    @GetMapping
    @Operation(summary = "Get account transactions", description = "Lists transactions for an account with optional status filter and pagination")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @RequestParam String accountId,
            @RequestParam(required = false) TransactionStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<Transaction> transactions;

        if (status != null) {
            transactions = transactionService.getAccountTransactions(accountId, status, pageable);
        } else {
            transactions = transactionService.getAccountTransactions(accountId, pageable);
        }

        return ResponseEntity.ok(transactions.map(transactionMapper::toResponse));
    }

    /**
     * [GET] Karta Ait İşlemler
     *
     * Belirli bir kartla yapılan işlemleri listeler.
     *
     * @param cardId   Kart ID
     * @param status   Statü filtresi
     * @param pageable Sayfalama
     * @return Sayfalı işlem listesi
     */
    @GetMapping("/by-card")
    @Operation(summary = "Get card transactions", description = "Lists transactions for a specific card with optional status filter")
    public ResponseEntity<Page<TransactionResponse>> getCardTransactions(
            @RequestParam String cardId,
            @RequestParam(required = false) TransactionStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<Transaction> transactions;

        if (status != null) {
            transactions = transactionService.getCardTransactions(cardId, status, pageable);
        } else {
            transactions = transactionService.getCardTransactions(cardId, pageable);
        }

        return ResponseEntity.ok(transactions.map(transactionMapper::toResponse));
    }

    /**
     * [GET] Kullanıcının Başlattığı İşlemler
     *
     * Belirli bir kullanıcının başlattığı işlemleri listeler.
     *
     * @param initiatorId Kullanıcı ID
     * @param pageable    Sayfalama
     * @return Sayfalı işlem listesi
     */
    @GetMapping("/by-initiator")
    @Operation(summary = "Get user-initiated transactions", description = "Lists transactions initiated by a specific user")
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @RequestParam String initiatorId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<Transaction> transactions = transactionService.getUserTransactions(initiatorId, pageable);
        return ResponseEntity.ok(transactions.map(transactionMapper::toResponse));
    }

    /**
     * [GET] Bekleyen (Pending) İşlemler
     *
     * Timeout tespiti ve izleme için PENDING statüsündeki işlemleri döner.
     *
     * @param initiatorId Kullanıcı ID
     * @return Bekleyen işlemler listesi
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending transactions", description = "Lists pending transactions for monitoring and timeout detection")
    public ResponseEntity<List<TransactionResponse>> getPendingTransactions(
            @RequestParam String initiatorId) {

        List<Transaction> transactions = transactionService.getPendingTransactions(initiatorId);
        List<TransactionResponse> responses = transactions.stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // ============================================
    // Operations & Monitoring Endpoints
    // ============================================

    /**
     * [GET] Hatalı İşlemler
     *
     * Operasyon ekibinin hataları izlemesi için.
     *
     * @param reason   Hata sebebi filtresi (Opsiyonel)
     * @param pageable Sayfalama
     * @return Hatalı işlem listesi
     */
    @GetMapping("/failed")
    @Operation(summary = "Get failed transactions", description = "Lists failed transactions with optional reason filter for operations monitoring")
    public ResponseEntity<Page<TransactionResponse>> getFailedTransactions(
            @RequestParam(required = false) String reason,
            @PageableDefault(size = 20, sort = "failedAt") Pageable pageable) {

        Page<Transaction> transactions = transactionService.getFailedTransactions(reason, pageable);
        return ResponseEntity.ok(transactions.map(transactionMapper::toResponse));
    }

    /**
     * [GET] İşlem İstatistikleri
     *
     * Dashboard için statü/tip bazlı işlem sayılarını döner.
     *
     * @return İstatistik map'i
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get transaction statistics", description = "Returns transaction counts by status and type for monitoring dashboard")
    public ResponseEntity<java.util.Map<String, Object>> getStatistics() {
        java.util.Map<String, Object> statistics = transactionService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
