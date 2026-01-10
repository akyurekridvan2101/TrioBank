package com.triobank.account.domain.model;

import com.triobank.account.domain.converter.JsonMapConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Ürün Tanımı (Katalog)
 * 
 * Bankanın müşterilerine sunduğu ürünlerin "şablonudur". Hesaplar bu
 * şablonlardan üretilir.
 * Örneğin: "Genç Kart", "Altın Hesabı", "Süper Kredi" birer ürün tanımıdır.
 * 
 * Bu yapı sayesinde, yeni bir hesap türü çıkarmak için kod yazmaya gerek
 * kalmaz,
 * sadece veritabanına yeni bir ürün tanımı eklemek yeterlidir (Product Catalog
 * Pattern).
 */
@Entity
@Table(name = "product_definitions")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class ProductDefinition {

    @Id
    @Column(name = "code", length = 50, nullable = false)
    @NotBlank
    @Size(max = 50)
    private String code;

    @Column(name = "name", length = 100, nullable = false, columnDefinition = "NVARCHAR(100)")
    @NotBlank
    @Size(max = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    @NotNull
    private ProductCategory category;

    /**
     * Ürün Özellikleri (JSON)
     * 
     * Ürünün tüm kuralları ve varsayılan ayarları burada tutulur.
     * Faiz oranı, eksi bakiye izni, limitler, bildirim tercihleri vb.
     * Veritabanında JSON durur, Java'da otomatik olarak Map'e dönüşür.
     * 
     * Bu features Account açılırken configuration olarak kopyalanır.
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "features", columnDefinition = "NVARCHAR(MAX)")
    @Builder.Default
    private Map<String, Object> features = new HashMap<>();

    /**
     * Varsayılan Kullanıcı Konfigürasyonu (JSON)
     * 
     * Bu üründen açılan hesapların başlangıç ayarları.
     * Bildirimler, limitler, tercihler gibi kullanıcının değiştirebileceği ayarlar.
     * 
     * Örnek:
     * {
     * "emailNotifications": true,
     * "smsNotifications": true,
     * "dailyTransactionLimit": 5000.00,
     * "preferredLanguage": "tr-TR"
     * }
     * 
     * Account oluşturulurken:
     * 1. defaultConfiguration kopyalanır
     * 2. CreateAccountRequest.configuration (varsa) merge edilir
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "default_configuration", columnDefinition = "NVARCHAR(MAX)")
    @Builder.Default
    private Map<String, Object> defaultConfiguration = new HashMap<>();

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
