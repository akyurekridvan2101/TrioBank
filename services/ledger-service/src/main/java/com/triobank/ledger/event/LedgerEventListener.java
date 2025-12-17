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
 * Kafka Event Dinleyicisi (Listener)
 * 
 * Transaction ve Account servislerinden gelen Kafka mesajlarını (event'leri)
 * burada karşılıyoruz ve LedgerService'e (iş mantığına) yönlendiriyoruz.
 * Hata yönetimi (Retry) KafkaConfig tarafında yapılıyor, burası sadece
 * yönlendirici.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventListener {

        private final LedgerService ledgerService;

        /**
         * Transaction Başladı (SAGA Adım 1)
         * 
         * Kullanıcı bir işlem başlattı, hemen deftere kaydediyoruz.
         */
        @KafkaListener(topics = "${ledger.kafka.consumer.topics.transaction-started}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "transactionStartedListenerFactory")
        public void handleTransactionStarted(
                        @Payload TransactionStartedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) Long offset,
                        Acknowledgment acknowledgment) {

                log.info("TransactionStarted eventi geldi: id={}, topic={}, offset={}",
                                event.getPayload().getTransactionId(), topic, offset);

                // Hata olursa exception fırlar, ErrorHandler yakalayıp retry eder.
                ledgerService.recordTransaction(event.getPayload());

                // Sorun yoksa Kafka'ya "tamamdır" diyoruz
                acknowledgment.acknowledge();

                log.info("Transaction başarıyla kaydedildi: {}", event.getPayload().getTransactionId());
        }

        /**
         * İşlem İptali / İade (Compensation)
         * 
         * Transaction bir yerde patladıysa (örn. bakiye yetmedi),
         * SAGA gereği işlemi geri alıyoruz (Ters kayıt atıyoruz).
         */
        @KafkaListener(topics = "${ledger.kafka.consumer.topics.compensation-required}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "compensationRequiredListenerFactory")
        public void onCompensationRequired(
                        @Payload CompensationRequiredEvent event,
                        Acknowledgment acknowledgment) {

                log.info("Compensation (İade) talebi geldi: transactionId={}", event.getPayload().getTransactionId());

                ledgerService.reverseTransaction(event.getPayload());

                acknowledgment.acknowledge();

                log.info("İade işlemi tamamlandı: {}", event.getPayload().getTransactionId());
        }

        /**
         * Yeni Hesap Açıldı
         * 
         * Account Service hesap açtı, biz de hemen 0.00 bakiyeli bir kayıt
         * oluşturuyoruz.
         */
        @KafkaListener(topics = "${ledger.kafka.consumer.topics.account-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "accountCreatedListenerFactory")
        public void handleAccountCreated(
                        @Payload AccountCreatedEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) Long offset,
                        Acknowledgment acknowledgment) {

                log.info("AccountCreated eventi geldi: accountId={}", event.getPayload().getAccountId());

                ledgerService.createInitialBalance(
                                event.getPayload().getAccountId(),
                                event.getPayload().getCurrency());

                acknowledgment.acknowledge();

                log.info("Hesap için başlangıç bakiyesi oluşturuldu: {}", event.getPayload().getAccountId());
        }

}
