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

/**
 * Debit Card (Banka Kartı)
 * 
 * Vadesiz hesaba bağlı, bakiye ile sınırlı kart
 */
@Entity
@DiscriminatorValue("DEBIT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DebitCard extends Card {

    /**
     * Günlük ATM çekim limiti
     */
    @Column(name = "daily_withdrawal_limit")
    private BigDecimal dailyWithdrawalLimit;

    /**
     * ATM kullanım izni
     */
    @Column(name = "atm_enabled")
    private Boolean atmEnabled;

    @Override
    public CardType getCardType() {
        return CardType.DEBIT;
    }

    /**
     * Debit card için özel validasyon
     */
    public void validateTransaction(BigDecimal amount) {
        if (!isUsable()) {
            throw new IllegalStateException("Card is not usable");
        }

        if (dailyWithdrawalLimit != null && amount.compareTo(dailyWithdrawalLimit) > 0) {
            throw new IllegalStateException("Amount exceeds daily withdrawal limit");
        }
    }
}
