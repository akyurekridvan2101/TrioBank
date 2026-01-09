package com.triobank.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BlockAmountRequest - Tutar bloke etme talebi
 * 
 * Transaction Service bir işlem başlatmadan önce bakiye bloke eder.
 * Endpoint: POST /api/v1/ledger/balances/{accountId}/block
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlockAmountRequest {

    /** Bloke edilecek tutar (pozitif olmalı) */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    /** Transaction ID (takip için) */
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    /** Bloke sebabi */
    @NotBlank(message = "Reason is required")
    private String reason; // "PENDING_TRANSACTION", "AUTHORIZATION_HOLD", etc.
}
