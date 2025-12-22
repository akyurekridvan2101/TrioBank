package com.triobank.account.dto.event.outgoing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * AccountConfigurationChangedEvent
 * 
 * Hesap yapılandırması değiştiğinde yayınlanan event.
 * Topic: triobank.{env}.account.AccountConfigurationChanged.v1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountConfigurationChangedEvent {

    private String accountId;
    private String customerId;
    private Map<String, Object> previousConfiguration;
    private Map<String, Object> newConfiguration;

    @Builder.Default
    private Instant changedAt = Instant.now();
}
