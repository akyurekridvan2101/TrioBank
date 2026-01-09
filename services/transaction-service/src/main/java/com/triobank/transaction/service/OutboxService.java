package com.triobank.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox Service - Transactional Outbox Pattern
 * 
 * Dağıtık sistemlerin olmazsa olmazı.
 * Kafka'ya doğrudan mesaj atarsak ve o an ağ koparsa, veri tutarsızlığı olur.
 * O yüzden önce veritabanındaki 'outbox_events' tablosuna yazıyoruz (Commit
 * garantili).
 * Sonra Debezium (CDC) aracı bu tablodan okuyup Kafka'ya basıyor.
 * 
 * Özetle: "Database'e yazdım ama Kafka'ya gidemedim" derdine son.
 * 
 * NOTE: Pattern copied from Account/Ledger services. Flat payload structure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes TransactionStarted event (to Ledger Service)
     * 
     * Topic: transaction.TransactionStarted.v1
     * Consumer: Ledger Service
     */
    public void publishTransactionStarted(
            String transactionId,
            String transactionType,
            String fromAccountId,
            String toAccountId,
            BigDecimal totalAmount,
            String currency,
            String description,
            LocalDate postingDate,
            LocalDate valueDate,
            String initiatorId,
            String referenceNumber,
            List<Map<String, Object>> entries) {

        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("transactionId", transactionId);
            payload.put("transactionType", transactionType);
            payload.put("fromAccountId", fromAccountId != null ? fromAccountId : "");
            payload.put("toAccountId", toAccountId != null ? toAccountId : "");
            payload.put("totalAmount", totalAmount);
            payload.put("currency", currency);
            payload.put("description", description != null ? description : "");
            payload.put("postingDate", postingDate.toString());
            payload.put("valueDate", valueDate.toString());
            payload.put("initiatorId", initiatorId != null ? initiatorId : "");
            payload.put("referenceNumber", referenceNumber);
            payload.put("entries", entries);

            insertOutboxEvent("Transaction", transactionId, "TransactionStarted", payload);
            log.debug("Published TransactionStarted event: {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to publish TransactionStarted event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publishes CompensationRequired event (to Ledger Service)
     * 
     * Topic: transaction.CompensationRequired.v1
     * Consumer: Ledger Service (for SAGA compensation/reversal)
     */
    public void publishCompensationRequired(
            String transactionId,
            String reason,
            String failedStep,
            String failureDetails,
            String compensationId) {

        try {
            Map<String, Object> payload = Map.of(
                    "transactionId", transactionId,
                    "reason", reason,
                    "failedStep", failedStep,
                    "failureDetails", failureDetails != null ? failureDetails : "",
                    "compensationId", compensationId);

            insertOutboxEvent("Transaction", transactionId, "CompensationRequired", payload);
            log.debug("Published CompensationRequired event: {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to publish CompensationRequired event", e);
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
                    java.sql.Timestamp.from(Instant.now()));

        } catch (Exception e) {
            log.error("Failed to insert outbox event", e);
            throw new RuntimeException("Failed to insert outbox event", e);
        }
    }
}
