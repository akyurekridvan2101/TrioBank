package com.triobank.transaction.event;

import com.triobank.transaction.dto.event.incoming.TransactionPostedEvent;
import com.triobank.transaction.dto.event.incoming.TransactionReversedEvent;
import com.triobank.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Event Listener - Ledger Service Events
 * 
 * Ledger Service'ten gelen Kafka mesajlarını (event'leri)
 * burada karşılıyoruz ve TransactionService'e (iş mantığına) yönlendiriyoruz.
 * Hata yönetimi (Retry) KafkaConfig tarafında yapılıyor, burası sadece
 * yönlendirici.
 * 
 * Pattern: Copied from Account/Ledger services
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionService transactionService;

    /**
     * Transaction Posted (SAGA Success)
     * 
     * Senaryo: Ledger Service başarıyla transaction'ı kayda aldı.
     * Aksiyon: Transaction status'unu COMPLETED'e güncelle.
     */
    @KafkaListener(topics = "${transaction.kafka.consumer.topics.transaction-posted}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "transactionPostedListenerFactory")
    public void handleTransactionPosted(
            @Payload TransactionPostedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("TransactionPosted event received: transactionId={}, topic={}, offset={}",
                event.getTransactionId(), topic, offset);

        try {
            // Business logic with potential optimistic lock retry
            transactionService.markAsCompleted(event.getTransactionId(), "");

            // Success - acknowledge
            acknowledgment.acknowledge();
            log.info("Transaction marked as COMPLETED: {}", event.getTransactionId());

        } catch (jakarta.persistence.OptimisticLockException e) {
            // BUG FIX #5: Handle optimistic locking gracefully
            log.warn("Optimistic lock conflict for transaction {}, will retry", event.getTransactionId(), e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Optimistic lock conflict, will retry", e);

        } catch (Exception e) {
            log.error("Error processing TransactionPosted event: transactionId={}",
                    event.getTransactionId(), e);
            // Let ErrorHandler manage retries
            throw e;
        }
    }

    /**
     * Transaction Reversed (SAGA Compensation)
     * 
     * Senaryo: Ledger Service işlemi iptal etti (reversal yaptı).
     * Aksiyon: Transaction status'unu FAILED'e güncelle.
     */
    @KafkaListener(topics = "${transaction.kafka.consumer.topics.transaction-reversed}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "transactionReversedListenerFactory")
    public void handleTransactionReversed(
            @Payload TransactionReversedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.warn("TransactionReversed event received: transactionId={}, reason={}, topic={}, offset={}",
                event.getOriginalTransactionId(), event.getReason(), topic, offset);

        try {
            // Business logic
            transactionService.markAsFailed(
                    event.getOriginalTransactionId(),
                    event.getReason(),
                    "",
                    "");

            // Success - acknowledge
            acknowledgment.acknowledge();
            log.info("Transaction marked as FAILED: {}", event.getOriginalTransactionId());

        } catch (jakarta.persistence.OptimisticLockException e) {
            // BUG FIX #5: Handle optimistic locking gracefully
            log.warn("Optimistic lock conflict for transaction {}, will retry", event.getOriginalTransactionId(), e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Optimistic lock conflict, will retry", e);

        } catch (Exception e) {
            log.error("Error processing TransactionReversed event: transactionId={}",
                    event.getOriginalTransactionId(), e);
            // Let ErrorHandler manage retries
            throw e;
        }
    }
}
