package com.triobank.card.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Virtual Card (Sanal Kart)
 * 
 * Online işlemler için sanal kart
 */
@Entity
@DiscriminatorValue("VIRTUAL")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VirtualCard extends Card {

    /**
     * Sadece online kullanım
     */
    @Column(name = "online_only")
    private Boolean onlineOnly;

    /**
     * Tek kullanımlık kart ise, son kullanma zamanı
     */
    @Column(name = "single_use_expires_at")
    private Instant singleUseExpiresAt;

    /**
     * Kullanım kısıtlaması (ONLINE_ONLY, INTERNATIONAL, etc.)
     */
    @Column(name = "usage_restriction", length = 50)
    private String usageRestriction;

    @Override
    public CardType getCardType() {
        return CardType.VIRTUAL;
    }

    /**
     * Virtual card için özel validasyon
     */
    public void validateTransaction(BigDecimal amount, boolean isOnline) {
        if (!isUsable()) {
            throw new IllegalStateException("Card is not usable");
        }

        // Online-only check
        if (Boolean.TRUE.equals(onlineOnly) && !isOnline) {
            throw new IllegalStateException("Card is valid for online transactions only");
        }

        // Single-use expiry check
        if (singleUseExpiresAt != null && Instant.now().isAfter(singleUseExpiresAt)) {
            throw new IllegalStateException("Virtual card has expired");
        }
    }

    /**
     * Tek kullanımlık kart mı?
     */
    public boolean isSingleUse() {
        return singleUseExpiresAt != null;
    }

    /**
     * Tek kullanımlık kart süresini ayarla
     */
    public void setSingleUseExpiry(Instant expiryTime) {
        this.singleUseExpiresAt = expiryTime;
    }
}
