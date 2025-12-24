package com.triobank.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * REST Client Configuration
 * 
 * Configures RestTemplate for external service communication.
 * Includes connection timeout, read timeout, and error handling.
 * 
 * Services:
 * - Account Service: Account validation
 * - Card Service: Card authorization, PIN validation
 * - Ledger Service: Balance inquiry
 */
@Configuration
public class RestClientConfig {

    @Value("${transaction.services.account-service.url}")
    private String accountServiceUrl;

    @Value("${transaction.services.card-service.url}")
    private String cardServiceUrl;

    @Value("${transaction.services.ledger-service.url}")
    private String ledgerServiceUrl;

    /**
     * General-purpose RestTemplate with sensible defaults
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Account Service base URL
     */
    @Bean("accountServiceBaseUrl")
    public String accountServiceBaseUrl() {
        return accountServiceUrl;
    }

    /**
     * Card Service base URL
     */
    @Bean("cardServiceBaseUrl")
    public String cardServiceBaseUrl() {
        return cardServiceUrl;
    }

    /**
     * Ledger Service base URL
     */
    @Bean("ledgerServiceBaseUrl")
    public String ledgerServiceBaseUrl() {
        return ledgerServiceUrl;
    }
}
