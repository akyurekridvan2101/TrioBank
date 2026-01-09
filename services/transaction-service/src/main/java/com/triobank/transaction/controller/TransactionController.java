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
 * Transaction Management API Controller
 * 
 * REST endpoints for transaction operations (TRANSFER, WITHDRAWAL, PURCHASE).
 * Follows Account/Card controller patterns for consistency.
 * 
 * Architecture:
 * - Controller: HTTP layer, validation, response formatting
 * - Service: Business logic, orchestration, SAGA coordination
 * - Repository: Data access
 * 
 * Pattern: Copied from AccountController and CardController
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
     * [POST] Create Transfer Transaction
     * 
     * Account-to-account money transfer.
     * Validates both accounts, checks balance, initiates SAGA.
     * 
     * @param request Transfer details
     * @return Created transaction with PENDING status
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
     * [POST] Create Withdrawal Transaction
     * 
     * ATM cash withdrawal using debit card.
     * Validates card, PIN, daily limit, account balance.
     * 
     * @param request Withdrawal details
     * @return Created transaction with PENDING status
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
     * [POST] Create Purchase Transaction
     * 
     * Merchant payment via card (POS or online).
     * Validates card, authorization, account balance.
     * 
     * @param request Purchase details
     * @return Created transaction with PENDING status
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
     * [GET] Get Transaction by ID
     * 
     * @param id Transaction ID
     * @return Transaction details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details", description = "Retrieves details of a specific transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        Transaction transaction = transactionService.getTransaction(id);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    /**
     * [GET] Get Transaction by Idempotency Key
     * 
     * For duplicate detection and idempotent request handling.
     * 
     * @param idempotencyKey Unique request identifier
     * @return Transaction if exists
     */
    @GetMapping("/by-idempotency-key/{idempotencyKey}")
    @Operation(summary = "Get transaction by idempotency key", description = "Retrieves transaction by idempotency key for duplicate detection")
    public ResponseEntity<TransactionResponse> getByIdempotencyKey(
            @PathVariable String idempotencyKey) {

        Transaction transaction = transactionService.getByIdempotencyKey(idempotencyKey);
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    /**
     * [GET] Get Transaction by Reference Number
     * 
     * For customer support and transaction lookup.
     * 
     * @param referenceNumber Transaction reference number
     * @return Transaction if exists
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
     * [GET] Get Account Transactions
     * 
     * Lists all transactions for an account (sender or receiver).
     * Supports pagination and optional status filter.
     * 
     * Examples:
     * - /v1/transactions?accountId=acc-123
     * - /v1/transactions?accountId=acc-123&status=COMPLETED
     * - /v1/transactions?accountId=acc-123&page=0&size=20
     * 
     * @param accountId Account ID (required)
     * @param status    Optional status filter
     * @param pageable  Pagination parameters
     * @return Paginated transaction list
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
     * [GET] Get Card Transactions
     * 
     * Lists all transactions for a specific card.
     * 
     * @param cardId   Card ID
     * @param status   Optional status filter
     * @param pageable Pagination parameters
     * @return Paginated transaction list
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
     * [GET] Get User-Initiated Transactions
     * 
     * Lists transactions initiated by a specific user.
     * 
     * @param initiatorId User/initiator ID
     * @param pageable    Pagination parameters
     * @return Paginated transaction list
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
     * [GET] Get Pending Transactions for User
     * 
     * Lists PENDING transactions for monitoring/timeout detection.
     * 
     * @param initiatorId User ID
     * @return List of pending transactions
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
     * [GET] Get Failed Transactions
     * 
     * For operations team to monitor failures.
     * 
     * @param reason   Optional failure reason filter
     * @param pageable Pagination parameters
     * @return Paginated failed transaction list
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
     * [GET] Get Transaction Statistics
     * 
     * Returns transaction counts by status/type for dashboard.
     * 
     * @return Statistics map
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get transaction statistics", description = "Returns transaction counts by status and type for monitoring dashboard")
    public ResponseEntity<java.util.Map<String, Object>> getStatistics() {
        java.util.Map<String, Object> statistics = transactionService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
