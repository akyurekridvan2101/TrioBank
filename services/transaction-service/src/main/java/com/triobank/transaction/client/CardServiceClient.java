package com.triobank.transaction.client;

import com.triobank.transaction.dto.external.CardAuthorizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Card Service HTTP Client
 * 
 * Makes HTTP calls to Card Service for:
 * - Card authorization for transactions
 * - PIN validation for ATM withdrawals
 * 
 * Endpoints Used:
 * - POST /v1/cards/{id}/authorize → Authorize transaction
 * - POST /v1/cards/{id}/validate-pin → Validate PIN
 * 
 * Pattern: Similar to inter-service communication in microservices
 */
@Component
@Slf4j
public class CardServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CardServiceClient(RestTemplate restTemplate,
            @Qualifier("cardServiceBaseUrl") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Authorize card for transaction
     * 
     * Calls Card Service authorization endpoint to check:
     * - Card exists and is ACTIVE
     * - Card type allows transaction (debit/credit/virtual)
     * - Daily limits not exceeded
     * - Associated account ID
     * 
     * @param cardId          Card ID
     * @param amount          Transaction amount
     * @param transactionType Transaction type (WITHDRAWAL, PURCHASE)
     * @param channel         Transaction channel (ATM, POS, ONLINE)
     * @return Authorization response with account ID if authorized
     * @throws CardServiceException if service call fails
     */
    public CardAuthorizationResponse authorizeCard(
            String cardId,
            BigDecimal amount,
            String transactionType,
            String channel) {

        String url = baseUrl + "/v1/cards/{id}/authorize";

        log.debug("Calling Card Service authorization: cardId={}, amount={}, type={}, channel={}",
                cardId, amount, transactionType, channel);

        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amount);
            requestBody.put("transactionType", transactionType);
            requestBody.put("channel", channel);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            CardAuthorizationResponse response = restTemplate.postForObject(
                    url,
                    request,
                    CardAuthorizationResponse.class,
                    cardId);

            log.debug("Card authorization response: cardId={}, authorized={}, accountId={}",
                    cardId, response.isAuthorized(), response.getAccountId());

            return response;

        } catch (HttpClientErrorException.NotFound e) {
            log.error("Card not found: cardId={}", cardId);
            throw new CardServiceException("Card not found: " + cardId, e);

        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("Card authorization declined: cardId={}", cardId);
            // Return declined response instead of throwing exception
            return CardAuthorizationResponse.declined("DECLINED", "Authorization declined");

        } catch (HttpClientErrorException e) {
            log.error("Card Service HTTP error: cardId={}, status={}, message={}",
                    cardId, e.getStatusCode(), e.getMessage());
            throw new CardServiceException("Card Service error: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Card Service communication error: cardId={}, error={}",
                    cardId, e.getMessage(), e);
            throw new CardServiceException("Failed to communicate with Card Service", e);
        }
    }

    /**
     * Validate card PIN
     * 
     * Calls Card Service PIN validation endpoint.
     * Used for ATM withdrawals.
     * 
     * @param cardId Card ID
     * @param pin    PIN to validate
     * @return true if PIN is valid
     * @throws CardServiceException if service call fails
     */
    public boolean validatePin(String cardId, String pin) {
        String url = baseUrl + "/v1/cards/{id}/validate-pin?pin={pin}";

        log.debug("Calling Card Service PIN validation: cardId={}", cardId);

        try {
            Boolean isValid = restTemplate.postForObject(
                    url,
                    null,
                    Boolean.class,
                    cardId,
                    pin);

            log.debug("PIN validation response: cardId={}, isValid={}", cardId, isValid);

            return isValid != null && isValid;

        } catch (HttpClientErrorException.NotFound e) {
            log.error("Card not found for PIN validation: cardId={}", cardId);
            throw new CardServiceException("Card not found: " + cardId, e);

        } catch (HttpClientErrorException e) {
            log.error("Card Service PIN validation error: cardId={}, status={}",
                    cardId, e.getStatusCode());
            throw new CardServiceException("PIN validation error: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Card Service communication error during PIN validation: cardId={}", cardId, e);
            throw new CardServiceException("Failed to communicate with Card Service", e);
        }
    }

    /**
     * Custom exception for Card Service errors
     */
    public static class CardServiceException extends RuntimeException {
        public CardServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
