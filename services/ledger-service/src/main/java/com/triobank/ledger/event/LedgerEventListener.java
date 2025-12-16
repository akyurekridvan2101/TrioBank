package com.triobank.ledger.event;

import com.triobank.ledger.dto.event.incoming.AccountCreatedEvent;

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
        @KafkaListener(topics = "${ledger.kafka.consumer.topics.transaction-started}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "transactionStartedListenerFactory")
        public void handleTransactionStarted(
                        @Payload TransactionStartedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) Long offset,
                        Acknowledgment acknowledgment) {

                log.info("Received TransactionStarted event: transactionId={}, topic={}, offset={}",
                                event.getPayload().getTransactionId(), topic, offset);

                // Direkt event'i service'e gönder - Exception fırlarsa ErrorHandler yakalar ve
                // retry eder
                ledgerService.recordTransaction(event.getPayload());

                // Başarılı olursa ACK
                acknowledgment.acknowledge();

                log.info("Transaction recorded successfully: {}", event.getPayload().getTransactionId());
        }

        /**
         * Compensation required - Reversal (SAGA failed)
         * 
         * Topic: triobank.{env}.transaction.CompensationRequired.v1
         */
        @KafkaListener(topics = "${ledger.kafka.consumer.topics.compensation-required}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "compensationRequiredListenerFactory")
        public void onCompensationRequired(
                        @Payload CompensationRequiredEvent event,
                        Acknowledgment acknowledgment) {

                log.info("Received CompensationRequiredEvent: {}", event.getPayload().getTransactionId());

                ledgerService.reverseTransaction(event.getPayload());

                acknowledgment.acknowledge();

                log.info("Compensation processed successfully: {}", event.getPayload().getTransactionId());
        }

        /**
         * Account created - Create initial balance
         * 
         * Topic: triobank.{env}.account.AccountCreated.v1
         */
        @KafkaListener(topics = "${ledger.kafka.consumer.topics.account-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "accountCreatedListenerFactory")
        public void handleAccountCreated(
                        @Payload AccountCreatedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) Long offset,
                        Acknowledgment acknowledgment) {

                log.info("Received AccountCreated event: accountId={}, accountNumber={}, topic={}, offset={}",
                                event.getPayload().getAccountId(), event.getPayload().getAccountNumber(), topic,
                                offset);

                // Create initial balance (0.00)
                ledgerService.createInitialBalance(
                                event.getPayload().getAccountId(),
                                event.getPayload().getCurrency());

                // ACK
                acknowledgment.acknowledge();

                log.info("Initial balance created for account: {}", event.getPayload().getAccountId());
        }

}
