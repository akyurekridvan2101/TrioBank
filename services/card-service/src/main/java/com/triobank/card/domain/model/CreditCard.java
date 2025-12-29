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
 * Credit Card (Kredi Kartı)
 * 
 * Kredi limiti ile çalışan kart
 */
@Entity
@DiscriminatorValue("CREDIT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreditCard extends Card {

    /**
     * Toplam kredi limiti
     */
    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    /**
     * Kullanılabilir kredi (projection from Ledger)
     */
    @Column(name = "available_credit")
    private BigDecimal availableCredit;

    /**
     * Aylık faiz oranı (%)
     */
    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    /**
     * Ekstre günü (ayın hangi günü)
     */
    @Column(name = "statement_day")
    private Integer statementDay;

    /**
     * Son ödeme günü (ekstre gününden kaç gün sonra)
     */
    @Column(name = "payment_due_day")
    private Integer paymentDueDay;

    @Override
    public CardType getCardType() {
        return CardType.CREDIT;
    }

    /**
     * Credit card için özel validasyon
     */
    public void validateTransaction(BigDecimal amount) {
        if (!isUsable()) {
            throw new IllegalStateException("Card is not usable");
        }

        if (availableCredit == null || availableCredit.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient credit limit");
        }
    }

    /**
     * Available credit güncelle (event handler)
     */
    public void updateAvailableCredit(BigDecimal newAvailableCredit) {
        this.availableCredit = newAvailableCredit;
    }

    /**
     * Kullanılan kredi hesapla
     */
    public BigDecimal getUsedCredit() {
        if (creditLimit == null || availableCredit == null) {
            return BigDecimal.ZERO;
        }
        return creditLimit.subtract(availableCredit);
    }
}
