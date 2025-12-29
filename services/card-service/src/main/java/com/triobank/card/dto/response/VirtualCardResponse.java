package com.triobank.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Virtual Card Response DTO
 * 
 * Extends CardResponse with virtual card specific fields.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VirtualCardResponse extends CardResponse {
    private String cvv; // CVV for virtual cards (visible to user)
    private Boolean onlineOnly;
    private Instant singleUseExpiresAt;
    private String usageRestriction;
    // Note: number field inherited from CardResponse contains full PAN for virtual
    // cards
}
