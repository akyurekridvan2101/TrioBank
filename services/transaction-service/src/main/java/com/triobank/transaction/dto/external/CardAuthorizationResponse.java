package com.triobank.transaction.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Authorization Response (from Card Service)
 * 
 * Response from: POST /v1/cards/{id}/authorize
 * Used for: Card authorization before purchase/withdrawal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardAuthorizationResponse {

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
    public static CardAuthorizationResponse authorized(String accountId, String cardType, BigDecimal remainingLimit) {
        return CardAuthorizationResponse.builder()
                .authorized(true)
                .accountId(accountId)
                .cardType(cardType)
                .remainingDailyLimit(remainingLimit)
                .build();
    }

    /**
     * Constructor for declined authorization
     */
    public static CardAuthorizationResponse declined(String reason, String message) {
        return CardAuthorizationResponse.builder()
                .authorized(false)
                .declineReason(reason)
                .message(message)
                .build();
    }
}
