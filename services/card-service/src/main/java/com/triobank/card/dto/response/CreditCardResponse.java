package com.triobank.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Credit Card Response DTO
 * 
 * Extends CardResponse with credit card specific fields.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreditCardResponse extends CardResponse {
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal interestRate;
    private Integer statementDay;
    private Integer paymentDueDay;
}
