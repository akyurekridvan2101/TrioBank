package com.triobank.transaction.client;

import com.triobank.transaction.dto.external.AccountValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Account Service HTTP Client
 * 
 * Makes HTTP calls to Account Service for:
 * - Account validation before transaction
 * 
 * Endpoints Used:
 * - GET /v1/accounts/{id}/validation → Validate account for transaction
 * 
 * Pattern: Similar to inter-service communication in microservices
 */
@Component
@Slf4j
public class AccountServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AccountServiceClient(RestTemplate restTemplate,
            @Qualifier("accountServiceBaseUrl") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Validate account for transaction
     * 
     * Calls Account Service validation endpoint to check:
     * - Account exists and is ACTIVE
     * - Account status allows transactions
     * - Current balance (projection)
     * - Product features and limits
     * 
     * @param accountId Account ID to validate
     * @return Validation response with account details
     * @throws AccountServiceException if service call fails
     */
    public AccountValidationResponse validateAccount(String accountId) {
        String url = baseUrl + "/v1/accounts/{id}/validation";

        log.debug("Calling Account Service validation: accountId={}, url={}", accountId, url);

        try {
            AccountValidationResponse response = restTemplate.getForObject(
                    url,
                    AccountValidationResponse.class,
                    accountId);

            log.debug("Account validation response: accountId={}, isValid={}, status={}",
                    accountId, response.isValid(), response.getAccountStatus());

            return response;

        } catch (HttpClientErrorException.NotFound e) {
            log.error("Account not found: accountId={}", accountId);
            throw new AccountServiceException("Account not found: " + accountId, e);

        } catch (HttpClientErrorException e) {
            log.error("Account Service HTTP error: accountId={}, status={}, message={}",
                    accountId, e.getStatusCode(), e.getMessage());
            throw new AccountServiceException("Account Service error: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Account Service communication error: accountId={}, error={}",
                    accountId, e.getMessage(), e);
            throw new AccountServiceException("Failed to communicate with Account Service", e);
        }
    }

    /**
     * Custom exception for Account Service errors
     */
    public static class AccountServiceException extends RuntimeException {
        public AccountServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
