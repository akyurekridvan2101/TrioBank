package com.triobank.card.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for issuing a new Virtual Card
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IssueVirtualCardRequest {

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;

    private Boolean onlineOnly = true;

    private Boolean singleUse = false;

    private Integer singleUseValidityHours; // If single-use, expiry in hours

    private String usageRestriction; // ONLINE_ONLY, INTERNATIONAL, etc.
}
