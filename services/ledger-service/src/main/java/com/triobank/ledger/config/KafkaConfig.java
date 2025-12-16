package com.triobank.ledger.config;

import com.triobank.ledger.dto.event.incoming.*;
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
 * KafkaConfig - Kafka consumer configuration with separate factories per event
 * type
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
     * Global Error Handler
     * 
     * Strategy:
     * - Retry 3 times with 1 second interval
     * - If still fails, log error (and potentially move to DLQ)
     * 
     * This protects against transient failures (e.g. DB connection lost)
     */
    @Bean
    public org.springframework.kafka.listener.DefaultErrorHandler errorHandler() {
        // BackOff: 1000ms (1s), 3 attempts
        org.springframework.util.backoff.FixedBackOff backOff = new org.springframework.util.backoff.FixedBackOff(1000L,
                3);

        org.springframework.kafka.listener.DefaultErrorHandler errorHandler = new org.springframework.kafka.listener.DefaultErrorHandler(
                (record, exception) -> {
                    // Recovery logic (after retries exhausted)
                    // In production, sending to a Dead Letter Queue (DLQ) is best practice
                    System.err.println("Failed to process record after 3 retries: " + record.value());
                    // Here you could add logic to send to DLQ manually if needed
                }, backOff);

        // Exceptions that should NOT be retried (e.g. Deserialization errors, Invalid
        // Data)
        // errorHandler.addNotRetryableExceptions(IllegalArgumentException.class); //
        // Example

        return errorHandler;
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> createContainerFactory(Class<T> targetType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(targetType));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler()); // Enable Error Handler
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AccountCreatedEvent> accountCreatedListenerFactory() {
        return createContainerFactory(AccountCreatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionStartedEvent> transactionStartedListenerFactory() {
        return createContainerFactory(TransactionStartedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CompensationRequiredEvent> compensationRequiredListenerFactory() {
        return createContainerFactory(CompensationRequiredEvent.class);
    }
}
