package com.triobank.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Outbox Service - Transactional Outbox Deseni (Ledger Pattern)
 * 
 * Dağıtık sistemlerin olmazsa olmazı.
 * Kafka'ya doğrudan mesaj atarsak ve o an ağ koparsa, veri tutarsızlığı olur.
 * O yüzden önce veritabanındaki 'outbox_events' tablosuna yazıyoruz (Commit
 * garantili).
 * Sonra Debezium (CDC) aracı bu tablodan okuyup Kafka'ya basıyor.
 * 
 * Özetle: "Database'e yazdım ama Kafka'ya gidemedim" derdine son.
 * 
 * NOT: Ledger servisinden kopyalanmıştır. Flat payload yapısı kullanır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes AccountCreated event
     */
    public void publishAccountCreated(
            String accountId,
            String accountNumber,
            String customerId,
            String accountType,
            String currency,
            String status,
            String createdBy,
            java.time.Instant createdAt) {

        try {
            Map<String, Object> payload = Map.of(
                    "accountId", accountId,
                    "accountNumber", accountNumber,
                    "customerId", customerId,
                    "accountType", accountType,
                    "currency", currency,
                    "status", status,
                    "createdBy", createdBy,
                    "createdAt", createdAt.toString());

            insertOutboxEvent("Account", accountId, "AccountCreated", payload);
            log.debug("Published AccountCreated event: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to publish AccountCreated event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publishes AccountStatusChanged event
     */
    public void publishAccountStatusChanged(
            String accountId,
            String previousStatus,
            String newStatus,
            String reason,
            String changedBy) {

        try {
            Map<String, Object> payload = Map.of(
                    "accountId", accountId,
                    "previousStatus", previousStatus,
                    "newStatus", newStatus,
                    "reason", reason,
                    "changedBy", changedBy,
                    "changedAt", java.time.Instant.now().toString());

            insertOutboxEvent("Account", accountId, "AccountStatusChanged", payload);
            log.debug("Published AccountStatusChanged event: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to publish AccountStatusChanged event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publishes AccountConfigurationChanged event
     */
    public void publishAccountConfigurationChanged(
            String accountId,
            String customerId,
            Map<String, Object> previousConfiguration,
            Map<String, Object> newConfiguration) {

        try {
            Map<String, Object> payload = Map.of(
                    "accountId", accountId,
                    "customerId", customerId,
                    "previousConfiguration", previousConfiguration,
                    "newConfiguration", newConfiguration,
                    "changedAt", java.time.Instant.now().toString());

            insertOutboxEvent("Account", accountId, "AccountConfigurationChanged", payload);
            log.debug("Published AccountConfigurationChanged event: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to publish AccountConfigurationChanged event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publishes AccountClosed event (optional - for future use)
     */
    public void publishAccountClosed(
            String accountId,
            String reason,
            String closedBy) {

        try {
            Map<String, Object> payload = Map.of(
                    "accountId", accountId,
                    "reason", reason,
                    "closedBy", closedBy,
                    "closedAt", java.time.Instant.now().toString());

            insertOutboxEvent("Account", accountId, "AccountClosed", payload);
            log.debug("Published AccountClosed event: {}", accountId);
        } catch (Exception e) {
            log.error("Failed to publish AccountClosed event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    private void insertOutboxEvent(
            String aggregateType,
            String aggregateId,
            String type,
            Map<String, Object> payload) {

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            String sql = "INSERT INTO outbox_events " +
                    "(id, aggregate_type, aggregate_id, type, payload, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    UUID.randomUUID(),
                    aggregateType,
                    aggregateId,
                    type,
                    payloadJson,
                    java.sql.Timestamp.from(java.time.Instant.now()));

        } catch (Exception e) {
            log.error("Failed to insert outbox event", e);
            throw new RuntimeException("Failed to insert outbox event", e);
        }
    }
}
