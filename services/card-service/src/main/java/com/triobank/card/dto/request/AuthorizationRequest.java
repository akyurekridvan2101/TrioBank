package com.triobank.card.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Authorization Request DTO
 * 
 * Request body for transaction authorization check.
 * Used by Transaction Service to validate if a card can be used for a
 * transaction.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private String currency;

    private String merchantId;

    private String merchantName;

    private String merchantCategory;

    @NotNull(message = "Transaction type is required")
    private String transactionType; // PURCHASE, WITHDRAWAL, REFUND

    private String channel; // POS, ATM, ONLINE, MOBILE
}
