package com.triobank.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ReleaseAmountRequest - Bloke edilmiş tutarı serbest bırakma talebi
 * 
 * Transaction tamamlandı veya iptal edildi, bloke kaldırılmalı.
 * Endpoint: POST /api/v1/ledger/balances/{accountId}/release
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseAmountRequest {

    /** Serbest bırakılacak tutar (pozitif olmalı) */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    /** Transaction ID (hangi transaction'ın blokesi kaldırılacak) */
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
}
