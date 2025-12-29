package com.triobank.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Debit Card Response DTO
 * 
 * Extends CardResponse with debit card specific fields.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DebitCardResponse extends CardResponse {
    private BigDecimal dailyWithdrawalLimit;
    private Boolean atmEnabled;
}
