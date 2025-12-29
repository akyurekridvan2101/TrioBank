package com.triobank.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Base Card Response DTO
 * 
 * Contains common fields for all card types.
 * Subclasses: DebitCardResponse, CreditCardResponse, VirtualCardResponse
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private String id;
    private String cardType;
    private String number; // Masked for Debit/Credit, Full PAN for Virtual
    private String cardholderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardBrand;
    private String status;
    private String accountId;
    private Instant createdAt;
    private Instant blockedAt;
    private String blockReason;
}
