package com.triobank.transaction.client;

import com.triobank.transaction.dto.external.LedgerBalanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Ledger Service HTTP Client
 * 
 * Makes HTTP calls to Ledger Service for:
 * - Account balance inquiry (for validation before transaction)
 * 
 * Endpoints Used:
 * - GET /api/v1/ledger/balances/{accountId} → Get account balance
 * 
 * NOTE: We do NOT call Ledger for posting transactions directly.
 * Instead, we publish TransactionStartedEvent to Kafka, and Ledger consumes it.
 * This is the SAGA pattern (event-driven choreography).
 * 
 * Pattern: Similar to inter-service communication in microservices
 */
@Component
@Slf4j
public class LedgerServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public LedgerServiceClient(RestTemplate restTemplate,
            @Qualifier("ledgerServiceBaseUrl") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Get account balance from Ledger
     * 
     * Calls Ledger Service balance endpoint to get:
     * - Current balance (source of truth)
     * - Available balance (balance - blocked amount)
     * - Pending debits/credits
     * - Last updated timestamp
     * 
     * This is used for VALIDATION purposes only, before initiating transaction.
     * The actual balance deduction happens when Ledger processes
     * TransactionStartedEvent.
     * 
     * @param accountId Account ID
     * @return Balance information
     * @throws LedgerServiceException if service call fails
     */
    public LedgerBalanceResponse getAccountBalance(String accountId) {
        String url = baseUrl + "/api/v1/ledger/balances/{accountId}";

        log.debug("Calling Ledger Service balance inquiry: accountId={}, url={}", accountId, url);

        try {
            LedgerBalanceResponse response = restTemplate.getForObject(
                    url,
                    LedgerBalanceResponse.class,
                    accountId);

            log.debug("Ledger balance response: accountId={}, balance={}, available={}",
                    accountId, response.getBalance(), response.getAvailableBalance());

            return response;

        } catch (HttpClientErrorException.NotFound e) {
            log.error("Account balance not found in Ledger: accountId={}", accountId);
            throw new LedgerServiceException("Account balance not found: " + accountId, e);

        } catch (HttpClientErrorException e) {
            log.error("Ledger Service HTTP error: accountId={}, status={}, message={}",
                    accountId, e.getStatusCode(), e.getMessage());
            throw new LedgerServiceException("Ledger Service error: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Ledger Service communication error: accountId={}, error={}",
                    accountId, e.getMessage(), e);
            throw new LedgerServiceException("Failed to communicate with Ledger Service", e);
        }
    }

    /**
     * Custom exception for Ledger Service errors
     */
    public static class LedgerServiceException extends RuntimeException {
        public LedgerServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
