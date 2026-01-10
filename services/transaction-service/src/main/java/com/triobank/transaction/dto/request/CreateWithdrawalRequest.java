package com.triobank.transaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for ATM cash withdrawal
 * 
 * Validation Rules:
 * - Idempotency key is required
 * - Account ID is required
 * - Card ID is required (debit card)
 * - Amount must be positive
 * - PIN is required for authentication
 * - ATM ID and location are optional (for audit trail)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWithdrawalRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    /**
     * Withdrawal amount (must be positive)
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "PIN is required")
    private String pin;

    private String atmId;

    private String location;

    /**
     * Initiator user ID (optional, for audit trail)
     */
    private String initiatorId;
}
