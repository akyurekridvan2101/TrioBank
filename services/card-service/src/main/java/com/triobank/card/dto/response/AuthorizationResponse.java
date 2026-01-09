package com.triobank.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Authorization Response DTO
 * 
 * Response for transaction authorization check.
 * Contains authorization result and relevant account/card information.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {

    /**
     * Whether the transaction is authorized
     */
    private boolean authorized;

    /**
     * Account ID associated with the card (needed by Transaction Service)
     */
    private String accountId;

    /**
     * Card type (DEBIT, CREDIT, VIRTUAL)
     */
    private String cardType;

    /**
     * Card status
     */
    private String cardStatus;

    /**
     * Remaining daily limit (for DEBIT cards)
     */
    private BigDecimal remainingDailyLimit;

    /**
     * Decline reason if not authorized
     */
    private String declineReason;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * Constructor for successful authorization
     */
    public static AuthorizationResponse authorized(String accountId, String cardType, BigDecimal remainingLimit) {
        return AuthorizationResponse.builder()
                .authorized(true)
                .accountId(accountId)
                .cardType(cardType)
                .remainingDailyLimit(remainingLimit)
                .build();
    }

    /**
     * Constructor for declined authorization
     */
    public static AuthorizationResponse declined(String reason, String message) {
        return AuthorizationResponse.builder()
                .authorized(false)
                .declineReason(reason)
                .message(message)
                .build();
    }
}
