package com.triobank.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a new Debit Card
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IssueDebitCardRequest {

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;

    private BigDecimal dailyWithdrawalLimit;

    private Boolean atmEnabled = true;

    private String pin; // 4 digits, will be hashed
}
