package com.triobank.card.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Card Base Entity (Abstract)
 * 
 * Single Table Inheritance ile tüm kart türlerinin base class'ı.
 * Subclasses: DebitCard, CreditCard, VirtualCard
 */
@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "card_type", discriminatorType = DiscriminatorType.STRING)
@EntityListeners(AuditingEntityListener.class)
@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Card {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "card_number", length = 19, unique = true, nullable = false)
    private String cardNumber;

    @Column(name = "masked_number", length = 19, nullable = false)
    private String maskedNumber;

    @Column(name = "cvv", length = 3, nullable = false)
    private String cvv;

    @Column(name = "cardholder_name", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String cardholderName;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", length = 20, nullable = false)
    private CardBrand cardBrand;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CardStatus status;

    @Column(name = "account_id", length = 50, nullable = false)
    private String accountId; // FK to Account

    @Column(name = "pin_hash", length = 60)
    private String pinHash; // BCrypt hashed PIN

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "block_reason", columnDefinition = "NVARCHAR(255)")
    private String blockReason;

    /**
     * Template method - subclasses return their type
     */
    public abstract CardType getCardType();

    /**
     * Business method - block card
     */
    public void block(String reason) {
        this.status = CardStatus.BLOCKED;
        this.blockedAt = Instant.now();
        this.blockReason = reason;
    }

    /**
     * Business method - activate card
     */
    public void activate() {
        if (isExpired()) {
            throw new IllegalStateException("Cannot activate expired card");
        }
        this.status = CardStatus.ACTIVE;
        this.blockedAt = null;
        this.blockReason = null;
    }

    /**
     * Check if card is expired
     */
    public boolean isExpired() {
        LocalDate expiry = LocalDate.of(expiryYear, expiryMonth, 1)
                .plusMonths(1)
                .minusDays(1); // Last day of expiry month
        return LocalDate.now().isAfter(expiry);
    }

    /**
     * Check if card is usable
     */
    public boolean isUsable() {
        return status == CardStatus.ACTIVE && !isExpired();
    }
}
