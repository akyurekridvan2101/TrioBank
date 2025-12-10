package com.triobank.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * OutboxEvent - Event saklama tablosu (Outbox Pattern)
 * 
 * Mikroservislerde event yayınlarken atomicity sorunu yaşanmasın diye
 * kullanılır.
 * Database transaction'ı içinde hem iş mantığı hem de event bu tabloya yazılır.
 * Debezium CDC bu tabloyu izleyip Kafka'ya event'leri yayınlar.
 * 
 * Database: outbox_events tablosu
 * 
 * Neden Outbox Pattern:
 * - Transaction güvenliği: Event ve iş mantığı aynı transaction'da
 * - Event kaybı olmaz: Database'e yazıldı mı mutlaka yayınlanır
 * - Exactly-once delivery: Debezium CDC garantisi
 */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent {

    /**
     * Event ID - UUID
     * 
     * Her event'in unique ID'si. Debezium CDC bu ID ile event'leri takip eder.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Aggregate type - Hangi entity tipine ait event olduğunu gösterir
     * 
     * Event routing ve topic belirleme için kullanılır.
     * Örnekler: "AccountBalance", "Transaction", "LedgerEntry"
     */
    @Column(name = "aggregate_type", length = 255, nullable = false)
    private String aggregateType;

    /**
     * Aggregate ID - Entity'nin ID'si
     * 
     * Kafka message key olarak kullanılır, böylece aynı entity'nin event'leri
     * aynı partition'a gider ve sıralı işlenir.
     * 
     * Örnekler: "ACC-123" (account ID), "TXN-456" (transaction ID)
     */
    @Column(name = "aggregate_id", length = 255, nullable = false)
    private String aggregateId;

    /**
     * Event type - Event'in ne olduğunu gösterir
     * 
     * Consumer'lar bu field'a bakarak hangi event'i handle edeceklerini bilir.
     * Örnekler: "BalanceUpdated", "TransactionReversed", "AccountCreated"
     */
    @Column(name = "type", length = 255, nullable = false)
    private String type;

    /**
     * Payload - Event'in detay bilgileri (JSON formatında)
     * 
     * Event'e ait tüm data burada JSON olarak saklanır.
     * Örnek: {"accountId": "ACC-123", "newBalance": 1000.00, "delta": 500.00}
     */
    @Column(name = "payload", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String payload;

    /**
     * Oluşturulma zamanı - Event ne zaman oluşturuldu
     * 
     * Hibernate otomatik olarak şu anki zamanı (UTC) buraya yazar.
     * updatable=false: Bir kez yazıldı mı asla değiştirilemez
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
