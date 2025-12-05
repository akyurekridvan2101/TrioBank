package com.triobank.ledger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triobank.ledger.dto.response.BalanceUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * OutboxService - "Transactional Outbox Pattern" implementasyonu.
 * 
 * Burası dağıtık sistemlerde veri tutarlılığı için hayati önem taşır.
 * Kafka'ya direkt event atmak yerine, önce veritabanındaki `outbox_events`
 * tablosuna
 * yazıyoruz. Bu yazma işlemi, business transaction (örn: para transferi) ile
 * AYNI transaction içinde gerçekleşiyor (Atomicity).
 * 
 * Daha sonra Debezium (CDC) bu tablodaki kayıtları okuyup Kafka'ya basacak.
 * Böylece "Database'e yazdım ama Kafka tpatladı" durumu asla yaşanmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void publishBalanceUpdated(BalanceUpdateDto balanceUpdate) {
        try {
            Map<String, Object> payload = Map.of(
                    "accountId", balanceUpdate.getAccountId(),
                    "previousBalance", balanceUpdate.getPreviousBalance(),
                    "newBalance", balanceUpdate.getNewBalance(),
                    "delta", balanceUpdate.getDelta(),
                    "currency", balanceUpdate.getCurrency());

            insertOutboxEvent("AccountBalance", balanceUpdate.getAccountId(), "BalanceUpdated", payload);
            log.debug("Published BalanceUpdated event: {}", balanceUpdate.getAccountId());
        } catch (Exception e) {
            log.error("Failed to publish BalanceUpdated event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    public void publishTransactionPosted(
            String transactionId,
            String transactionType,
            java.math.BigDecimal totalAmount,
            String currency,
            java.time.LocalDate postingDate,
            Integer entriesCount) {

        try {
            Map<String, Object> payload = Map.of(
                    "transactionId", transactionId,
                    "transactionType", transactionType,
                    "totalAmount", totalAmount,
                    "currency", currency,
                    "postingDate", postingDate.toString(),
                    "entriesCount", entriesCount,
                    "postedAt", java.time.Instant.now().toString());

            insertOutboxEvent("Transaction", transactionId, "TransactionPosted", payload);
            log.debug("Published TransactionPosted event: {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to publish TransactionPosted event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    public void publishTransactionReversed(
            String originalTransactionId,
            String reversalTransactionId,
            String reason) {

        try {
            Map<String, Object> payload = Map.of(
                    "originalTransactionId", originalTransactionId,
                    "reversalTransactionId", reversalTransactionId,
                    "reason", reason,
                    "reversedAt", java.time.Instant.now().toString());

            insertOutboxEvent("Transaction", originalTransactionId, "TransactionReversed", payload);
            log.debug("Published TransactionReversed event: {}", originalTransactionId);
        } catch (Exception e) {
            log.error("Failed to publish TransactionReversed event", e);
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
