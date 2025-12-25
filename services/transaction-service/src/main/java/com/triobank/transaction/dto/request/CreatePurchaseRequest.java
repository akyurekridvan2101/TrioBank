package com.triobank.transaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for card-based purchase (POS or online)
 * 
 * Validation Rules:
 * - Idempotency key is required
 * - Account ID is required
 * - Card ID is required (debit or virtual)
 * - Amount must be positive
 * - Merchant name is required (max 200 characters)
 * - Merchant category and online flag are optional
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Card ID is required")
    private String cardId;

    /**
     * Purchase amount (must be positive)
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Merchant name (max 200 characters)
     */
    @NotBlank(message = "Merchant name is required")
    @Size(max = 200, message = "Merchant name cannot exceed 200 characters")
    private String merchantName;

    private String merchantCategory;

    /**
     * Whether this is an online purchase (defaults to false for POS)
     */
    private boolean isOnline = false;

    /**
     * Initiator user ID (optional, for audit trail)
     */
    private String initiatorId;
}
