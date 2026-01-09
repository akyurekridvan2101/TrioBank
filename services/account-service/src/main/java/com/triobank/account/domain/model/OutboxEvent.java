package com.triobank.account.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Event (Güvenli Event Kutusu)
 * 
 * Mikroservis dünyasında "Transaction" ve "Event Atma" işini atomik yapmak için
 * kullanılır.
 * 
 * Mantık şudur: Hesap açıldığında, hem hesap tablosuna kayıt atarız hem de bu
 * tabloya
 * "Hesap Açıldı" diye bir event yazarız. İkisi aynı transaction içinde olur (ya
 * hepsi, ya hiçbiri).
 * Daha sonra Debezium (CDC) bu tabloyu okuyup Kafka'ya taşır. Böylece event
 * kaybı asla yaşanmaz.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent {

    /**
     * Event ID (UUID)
     * 
     * Tekil id. Debezium ve Kafka tarafında deduplication (çift kaydı önleme) için
     * kritiktir.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Aggregate Type (Hangi Nesne?)
     * 
     * Event'in hangi kök nesneyle ilgili olduğunu belirtir.
     * Örn: "Account", "Transaction".
     */
    @Column(name = "aggregate_type", length = 255, nullable = false)
    @NotBlank
    @Size(max = 255)
    private String aggregateType;

    /**
     * Aggregate ID (Hangi Kayıt?)
     * 
     * İlgili nesnenin ID'sidir. Kafka'da "Partition Key" olarak kullanılır.
     * Böylece aynı hesabın olayları hep aynı sırayla işlenir.
     */
    @Column(name = "aggregate_id", length = 255, nullable = false)
    @NotBlank
    @Size(max = 255)
    private String aggregateId;

    /**
     * Event Type (Ne Oldu?)
     * 
     * Olayın türünü belirtir.
     * Örn: "AccountCreated", "BalanceUpdated".
     */
    @Column(name = "type", length = 255, nullable = false)
    @NotBlank
    @Size(max = 255)
    private String type;

    /**
     * Payload (Veri)
     * 
     * Olayın tüm detaylarını içeren JSON verisidir.
     * Örn: Hesap no, müşteri no, limit bilgileri vb.
     */
    @Column(name = "payload", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    @NotBlank
    private String payload;

    /**
     * Oluşturulma Zamanı
     * 
     * Event'in sisteme düştüğü an. Sıralama için önemlidir.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
