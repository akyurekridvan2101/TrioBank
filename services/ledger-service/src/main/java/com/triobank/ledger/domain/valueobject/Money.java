package com.triobank.ledger.domain.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Money - Para value object (DDD Value Object pattern)
 * 
 * Para birimi ve tutar bilgisini birlikte tutan immutable sınıf.
 * 
 * ⚠️ Value Object Nedir?
 * ============================================================================
 * Value Object, değeri (value) ile tanımlanan nesnelerdir. Kimliği (ID) yoktur.
 * İki Money nesnesi, aynı amount ve currency'ye sahipse EŞİTTİR.
 * 
 * Örnek:
 * Money m1 = Money.of(100, "TRY");
 * Money m2 = Money.of(100, "TRY");
 * m1.equals(m2) → true ✅ (değerleri aynı)
 * 
 * Entity ile fark:
 * - Entity: Kimliği (ID) ile tanımlanır (AccountBalance, LedgerEntry)
 * - Value Object: Değeri ile tanımlanır (Money, Email, Address)
 * ============================================================================
 * 
 * Neden Money Value Object Kullanıyoruz?
 * - Tutar ve para birimi her zaman birlikte gitmeli (cohesion)
 * - Yanlışlıkla farklı para birimlerini toplamayı engeller
 * - Domain logic tek yerde (add, subtract)
 * - Type safety (BigDecimal + String yerine Money)
 * 
 * Özellikler:
 * - ✅ Immutable: Bir kez oluşturulduktan sonra değiştirilemez
 * - ✅ Validation: amount > 0, currency 3 karakter
 * - ✅ Domain operations: add(), subtract()
 * - ✅ JPA @Embeddable: Entity içine gömülebilir
 * 
 * NOT: Şu anda Entity'lerde kullanmıyoruz (migration ayrı kolonlarda).
 * İleride refactor edilebilir.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA için
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Factory method için
public class Money implements Serializable {

    /**
     * Tutar - Pozitif sayı olmalı
     * 
     * precision=19, scale=4 → Max: 999,999,999,999,999.9999
     */
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    /**
     * Para birimi - ISO 4217 standart kod (3 karakter)
     * 
     * Örnekler: "TRY", "USD", "EUR"
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    // ========================================
    // Factory Method - Nesne oluşturma
    // ========================================

    /**
     * Money nesnesi oluştur - Factory method (önerilen yol)
     * 
     * Constructor yerine bu metodu kullan. Validation garantili.
     * 
     * Kullanım:
     * Money money = Money.of(new BigDecimal("100.00"), "TRY");
     * 
     * @param amount   Tutar (pozitif olmalı, null olamaz)
     * @param currency Para birimi (3 karakter ISO 4217, null olamaz)
     * @return Geçerli Money nesnesi
     * @throws IllegalArgumentException Geçersiz değerler için
     */
    public static Money of(BigDecimal amount, String currency) {
        validate(amount, currency);
        return new Money(amount, currency);
    }

    // ========================================
    // Validation - Geçerlilik kontrolü
    // ========================================

    /**
     * Amount ve currency'nin geçerli olduğunu kontrol eder
     * 
     * Kurallar:
     * 1. amount null olamaz ve pozitif olmalı (> 0)
     * 2. currency null olamaz ve tam 3 karakter olmalı
     * 
     * @throws IllegalArgumentException Kural ihlali varsa
     */
    private static void validate(BigDecimal amount, String currency) {
        // Amount null olamaz ve pozitif olmalı
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Currency null olamaz ve 3 karakter olmalı
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be 3 characters (ISO 4217)");
        }
    }

    // ========================================
    // Domain Operations - İş mantığı
    // ========================================

    /**
     * İki Money'yi toplar - Aynı para birimi olmalı
     * 
     * Immutable: Yeni Money nesnesi döner, orijinaller değişmez.
     * 
     * Örnek:
     * Money m1 = Money.of(100, "TRY");
     * Money m2 = Money.of(50, "TRY");
     * Money total = m1.add(m2); → 150 TRY
     * // m1 hala 100 TRY (değişmedi)
     * 
     * @param other Eklenecek Money
     * @return Toplam Money (yeni nesne)
     * @throws IllegalArgumentException Farklı currency ise
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Bir Money'den diğerini çıkarır - Aynı para birimi olmalı
     * 
     * Immutable: Yeni Money nesnesi döner, orijinaller değişmez.
     * 
     * Örnek:
     * Money m1 = Money.of(100, "TRY");
     * Money m2 = Money.of(30, "TRY");
     * Money remaining = m1.subtract(m2); → 70 TRY
     * 
     * @param other Çıkarılacak Money
     * @return Fark Money (yeni nesne, negatif olabilir)
     * @throws IllegalArgumentException Farklı currency ise
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Aynı currency kontrolü
     * 
     * Farklı para birimlerini toplamak/çıkarmak mantıksız!
     * TRY + USD = ??? (döviz kuru gerekir)
     * 
     * @throws IllegalArgumentException Farklı currency ise
     */
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("Cannot operate on different currencies: %s vs %s",
                            this.currency, other.currency));
        }
    }

    // ========================================
    // Object Methods - equals, hashCode, toString
    // ========================================

    /**
     * Value Object equality check
     * 
     * İki Money nesne, amount VE currency aynı ise eşittir.
     * Kimlik (ID) yok, sadece değer önemli.
     * 
     * Örnek:
     * Money m1 = Money.of(100, "TRY");
     * Money m2 = Money.of(100, "TRY");
     * m1.equals(m2) → true (farklı nesneler ama değerleri aynı)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Money money))
            return false;
        return Objects.equals(amount, money.amount) &&
                Objects.equals(currency, money.currency);
    }

    /**
     * Hash code - equals ile uyumlu olmalı
     * 
     * Eğer m1.equals(m2) true ise, m1.hashCode() == m2.hashCode() olmalı.
     */
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    /**
     * String representation - Debugging ve loglama için
     * 
     * Örnek: "100.00 TRY", "50.50 USD"
     */
    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
