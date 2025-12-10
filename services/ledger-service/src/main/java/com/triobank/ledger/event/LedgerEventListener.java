package com.triobank.ledger.event;

import com.triobank.ledger.dto.event.incoming.AccountCreatedEvent;
import com.triobank.ledger.dto.event.incoming.AccountDeletedEvent;
import com.triobank.ledger.dto.event.incoming.CompensationRequiredEvent;
import com.triobank.ledger.dto.event.incoming.TransactionStartedEvent;
import com.triobank.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka event listener for Ledger Service
 * 
 * Listens to incoming events and delegates to LedgerService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventListener {

    private final LedgerService ledgerService;

    /**
     * Transaction started - Record to ledger (SAGA)
     * 
     * Topic: triobank.{env}.transaction.TransactionStarted.v1
     */
    @KafkaListener(topics = "${ledger.kafka.consumer.topics.transaction-started}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleTransactionStarted(
            @Payload TransactionStartedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("Received TransactionStarted event: transactionId={}, topic={}, offset={}",
                event.getPayload().getTransactionId(), topic, offset);

        try {
            // Direkt event'i service'e gönder
            ledgerService.recordTransaction(event.getPayload());

            // ACK
            acknowledgment.acknowledge();

            log.info("Transaction recorded successfully: {}", event.getPayload().getTransactionId());

        } catch (Exception e) {
            log.error("Failed to process TransactionStarted event: {}",
                    event.getPayload().getTransactionId(), e);

            // TODO: Dead Letter Queue (production)
            acknowledgment.acknowledge(); // Şimdilik ACK
        }
    }

    /**
     * Compensation required - Reversal (SAGA failed)
     * 
     * Topic: triobank.{env}.transaction.CompensationRequired.v1
     */
    @KafkaListener(topics = "${ledger.kafka.consumer.topics.compensation-required}", groupId = "${spring.kafka.consumer.group-id}")
    public void onCompensationRequired(CompensationRequiredEvent event) {
        log.info("Received CompensationRequiredEvent: {}", event.getPayload().getTransactionId());
        try {
            ledgerService.reverseTransaction(event.getPayload());
        } catch (Exception e) {
            log.error("Error processing compensation: {}", event.getPayload().getTransactionId(), e);
        }
    }

    /**
     * Account created - Create initial balance
     * 
     * Topic: triobank.{env}.account.AccountCreated.v1
     */
    @KafkaListener(topics = "${ledger.kafka.consumer.topics.account-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleAccountCreated(
            @Payload AccountCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("Received AccountCreated event: accountId={}, accountNumber={}, topic={}, offset={}",
                event.getPayload().getAccountId(), event.getPayload().getAccountNumber(), topic, offset);

        try {
            // Create initial balance (0.00)
            ledgerService.createInitialBalance(
                    event.getPayload().getAccountId(),
                    event.getPayload().getCurrency());

            // ACK
            acknowledgment.acknowledge();

            log.info("Initial balance created for account: {}", event.getPayload().getAccountId());

        } catch (Exception e) {
            log.error("Failed to create balance for account: {}", event.getPayload().getAccountId(), e);
            acknowledgment.acknowledge(); // Idempotent - ACK
        }
    }

    /**
     * Account deleted - Freeze balance
     * 
     * Topic: triobank.{env}.account.AccountDeleted.v1
     */
    @KafkaListener(topics = "${ledger.kafka.consumer.topics.account-deleted}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleAccountDeleted(
            @Payload AccountDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("Received AccountDeleted event: accountId={}, deletedBy={}, reason={}, topic={}, offset={}",
                event.getPayload().getAccountId(), event.getPayload().getDeletedBy(),
                event.getPayload().getDeletionReason(), topic, offset);

        try {
            // Freeze balance (no more transactions)
            ledgerService.freezeBalance(event.getPayload().getAccountId());

            // ACK
            acknowledgment.acknowledge();

            log.info("Balance frozen for deleted account: {}", event.getPayload().getAccountId());

        } catch (Exception e) {
            log.error("Failed to freeze balance for deleted account: {}", event.getPayload().getAccountId(), e);
            acknowledgment.acknowledge(); // ACK
        }
    }
}
