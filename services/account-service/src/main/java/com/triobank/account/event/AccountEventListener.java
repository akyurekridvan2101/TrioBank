package com.triobank.account.event;

import com.triobank.account.domain.model.AccountStatus;
import com.triobank.account.dto.event.incoming.BalanceUpdatedEvent;
import com.triobank.account.dto.event.incoming.LedgerAccountCreationFailedEvent;
import com.triobank.account.service.AccountService;
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
 * Kafka Event Dinleyicisi (Listener)
 * 
 * Ledger ve diğer servislerden gelen Kafka mesajlarını (event'leri)
 * burada karşılıyoruz ve AccountService'e (iş mantığına) yönlendiriyoruz.
 * Hata yönetimi (Retry) KafkaConfig tarafında yapılıyor, burası sadece
 * yönlendirici.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class AccountEventListener {

    private final AccountService accountService;

    /**
     * Ledger Hesabı Oluşturulamadı (SAGA Compensation)
     * 
     * Senaryo: Biz hesabı açtık (ACTIVE), ama Ledger servisi çöktü ve bakiye kaydı
     * oluşturamadı.
     * Çözüm: Hesabı kapatıyoruz veya hata statüsüne çekiyoruz (Consistency).
     */
    @KafkaListener(topics = "${account.kafka.consumer.topics.ledger-account-creation-failed}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "ledgerAccountCreationFailedListenerFactory")
    public void handleLedgerCreationFailure(
            @Payload LedgerAccountCreationFailedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.error("SAGA Compensation Eventi geldi: Ledger bakiye oluşturamadı. id={}, topic={}, offset={}",
                event.getAccountId(), topic, offset);

        log.error("Hata Sebebi: {}", event.getReason());

        // Hesabı kapatarak tutarlılığı sağla
        try {
            accountService.changeStatus(event.getAccountId(), AccountStatus.CLOSED,
                    "System Error: Ledger creation failed");

            log.info("Compensation tamamlandı: Hesap kapatıldı. AccountId={}", event.getAccountId());
        } catch (Exception e) {
            log.warn("Account already handled or not found during compensation: {}", e.getMessage());
        }

        acknowledgment.acknowledge();
    }

    /**
     * Bakiye Güncelleme Event'i (Ledger → Account Sync)
     * 
     * Senaryo: Ledger'da bir transaction işlendi ve bakiye değişti.
     * Aksiyon: Account Service'deki bakiye projeksiyonunu (cache) güncelle.
     * 
     * Bu event sayesinde kullanıcı anlık bakiyesini Account Service'den görebilir.
     */
    @KafkaListener(topics = "${account.kafka.consumer.topics.balance-updated}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "balanceUpdatedListenerFactory")
    public void handleBalanceUpdate(
            @Payload BalanceUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("Bakiye Güncelleme Eventi geldi: accountId={}, newBalance={}, currency={}, topic={}, offset={}",
                event.getAccountId(), event.getNewBalance(), event.getCurrency(), topic, offset);

        try {
            accountService.updateBalance(event.getAccountId(), event.getNewBalance());
            log.info("Bakiye başarıyla güncellendi: accountId={}, newBalance={}",
                    event.getAccountId(), event.getNewBalance());
        } catch (Exception e) {
            log.error("Bakiye güncellenirken hata oluştu: accountId={}, error={}",
                    event.getAccountId(), e.getMessage(), e);
            // Retry mekanizması KafkaConfig'de tanımlı, burada yeniden fırlatmıyoruz
        }

        acknowledgment.acknowledge();
    }
}
