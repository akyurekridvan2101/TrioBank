package com.triobank.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a new Credit Card
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IssueCreditCardRequest {

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;

    @NotNull(message = "Credit limit is required")
    private BigDecimal creditLimit;

    private BigDecimal interestRate = new BigDecimal("2.99"); // Default 2.99%

    private Integer statementDay = 15; // Default 15th

    private Integer paymentDueDay = 5; // Default 5 days after statement
}
