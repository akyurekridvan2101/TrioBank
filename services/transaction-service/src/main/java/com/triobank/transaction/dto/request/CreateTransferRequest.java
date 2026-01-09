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
 * Request DTO for creating a money transfer between accounts
 * 
 * Validation Rules:
 * - Idempotency key is required (prevents duplicate transactions)
 * - Both account IDs are required
 * - Amount must be positive
 * - Description is optional (max 500 characters)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Source account ID is required")
    private String fromAccountId;

    @NotBlank(message = "Destination account ID is required")
    private String toAccountId;

    /**
     * Amount to transfer (must be positive)
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Transaction description (optional, max 500 characters)
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * Currency code (defaults to TRY if not provided)
     */
    private String currency;

    /**
     * Initiator user ID (optional, for audit trail)
     */
    private String initiatorId;
}
