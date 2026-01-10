package com.triobank.account.domain.model;

import com.triobank.account.domain.converter.JsonMapConverter;
import com.triobank.account.domain.exception.DomainException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Müşteri Hesabı (Account)
 * 
 * Bu sınıf, bankadaki gerçek müşteri hesaplarını temsil eder. Domain Odaklı
 * Tasarım (DDD)
 * prensiplerine göre "Rich Domain Model" olarak kurgulanmıştır. Yani sadece
 * veri taşımaz,
 * kendi durumunu yöneten iş kurallarını (aktif etme, dondurma vb.) da içerir.
 * 
 * Her hesap bir "Ürün" (ProductDefinition) şablonundan türetilir.
 */
@Entity
@Table(name = "accounts")
@Getter // Setter yok! Encapsulation korumak icin.
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "customer_id", length = 50, nullable = false)
    @NotBlank
    @Size(max = 50)
    private String customerId;

    @Column(name = "account_number", length = 50, nullable = false, unique = true)
    @NotBlank
    @Size(max = 50)
    private String accountNumber;

    @Column(name = "product_code", length = 50, nullable = false)
    @NotBlank
    @Size(max = 50)
    private String productCode;

    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    /**
     * Hesap Bakiyesi (Ledger Projection)
     * 
     * Bu bakiye, Ledger Service'den gelen BalanceUpdatedEvent ile güncellenir.
     * Transaction başlatırken bu değer kontrol edilir (Optimistic Check).
     * Gerçek kayıt (Source of Truth) Ledger'dadır.
     */
    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @NotNull
    private AccountStatus status;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "configurations", columnDefinition = "NVARCHAR(MAX)")
    @Builder.Default
    private Map<String, Object> configurations = new HashMap<>();

    @Version
    @Column(name = "version")
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = AccountStatus.ACTIVE;
        }
    }

    /**
     * Hesabı kullanıma açar (Aktifleştirme).
     * 
     * Sadece kapalı hesaplar tekrar açılamaz kuralı geçerlidir.
     * Kapalı bir hesabı tekrar açamayız, yeni hesap gerekir.
     */
    public void activate() {
        if (this.status == AccountStatus.CLOSED) {
            throw new DomainException("Closed account cannot be activated.");
        }
        this.status = AccountStatus.ACTIVE;
    }

    /**
     * Hesabı kalıcı olarak kapatır.
     * 
     * Bu işlem geri alınamaz. Kapanan hesap bir daha işlem yapamaz.
     */
    public void close() {
        if (this.status == AccountStatus.CLOSED) {
            return; // Already closed
        }
        this.status = AccountStatus.CLOSED;
    }

    /**
     * Hesaba özel bir ayar ekler veya var olanı günceller.
     * 
     * Örnek: Ürün limiti 5.000 TL olsa da, bu müşteriye özel 10.000 TL tanımlamak
     * için.
     * Veriler JSON olarak saklanır.
     * 
     * @param key   Ayar anahtarı (örn: "monthlyLimit")
     * @param value Ayar değeri (örn: 10000)
     */
    public void addConfiguration(String key, Object value) {
        if (this.configurations == null) {
            this.configurations = new HashMap<>();
        }
        this.configurations.put(key, value);
    }

    /**
     * Bakiyeyi Günceller (Ledger Sync)
     * 
     * Bu metod sadece Ledger Service'den gelen BalanceUpdatedEvent ile
     * çağrılmalıdır.
     * Manuel kullanımı tavsiye edilmez.
     * 
     * @param newBalance Ledger'dan gelen güncel bakiye
     */
    public void updateBalance(BigDecimal newBalance) {
        if (newBalance == null) {
            throw new DomainException("Balance cannot be null");
        }
        this.balance = newBalance;
    }

    /**
     * Hesap Yapılandırmasını Günceller
     * 
     * @param newConfigurations Yeni yapılandırma değerleri
     */
    public void updateConfiguration(Map<String, Object> newConfigurations) {
        if (newConfigurations == null) {
            throw new DomainException("Configuration cannot be null");
        }
        this.configurations = newConfigurations;
    }
}
