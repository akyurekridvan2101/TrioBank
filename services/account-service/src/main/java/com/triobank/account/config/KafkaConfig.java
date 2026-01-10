package com.triobank.account.config;

import com.triobank.account.dto.event.incoming.BalanceUpdatedEvent;
import com.triobank.account.dto.event.incoming.LedgerAccountCreationFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Konfigürasyonu
 *
 * Ledger Service ile benzer yapıda.
 * Tip güvenli deserialization ve global hata yönetimi içerir.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.triobank.*");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return config;
    }

    private <T> ConsumerFactory<String, T> createConsumerFactory(Class<T> targetType) {
        Map<String, Object> config = baseConsumerConfig();
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Global Hata Yönetimi
     * 3 deneme (retry) sonrası log basar.
     */
    @Bean
    public org.springframework.kafka.listener.DefaultErrorHandler errorHandler() {
        org.springframework.util.backoff.FixedBackOff backOff = new org.springframework.util.backoff.FixedBackOff(1000L,
                3);

        return new org.springframework.kafka.listener.DefaultErrorHandler(
                (record, exception) -> {
                    System.err.println("3 deneme başarısız, mesaj işlenemedi: " + record.value());
                }, backOff);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> createContainerFactory(Class<T> targetType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(targetType));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    // LISTENER FACTORIES

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LedgerAccountCreationFailedEvent> ledgerAccountCreationFailedListenerFactory() {
        return createContainerFactory(LedgerAccountCreationFailedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BalanceUpdatedEvent> balanceUpdatedListenerFactory() {
        return createContainerFactory(BalanceUpdatedEvent.class);
    }
}
