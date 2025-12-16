package com.triobank.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


/**
 * LedgerEntry - Muhasebe kaydı
 *
 * Her finansal işlem için oluşturulan kayıt. Çift taraflı muhasebe sistemiyle
 * çalışır yani her işlemde en az 2 entry olur (DEBIT + CREDIT).
 *
 * Immutable: Bir kez oluşturuldu mu asla değiştirilemez.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA için
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder için
@Builder
public class LedgerEntry {

    /**
     * Primary key - UUID
     * 
     * UUID kullanıyoruz çünkü entry bu serviste oluşturuluyor, başka servisten
     * gelmiyor.
     * Zaten kullanıcıya da gösterilmez bu yüzden Long yerine UUID tercih ettim.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Transaction ilişkisi - Bu entry hangi transaction'a ait
     * 
     * Bir transaction içinde birden fazla entry olabilir (ManyToOne ilişkisi).
     * Örneğin bir transfer işleminde en az 2 entry olur: gönderen hesap için DEBIT,
     * alan hesap için CREDIT.
     * 
     * LAZY fetch: Transaction bilgisi sadece ihtiyaç olduğunda yüklenir
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransaction transaction;

    /**
     * Entry sırası - Transaction içindeki sıra numarası
     * 
     * Bir transaction içinde birden fazla entry olabilir (örneğin 4 entry: 2 tanesi
     * kullanıcı hesapları, 2 tanesi komisyon). Bu sıra numarası ile entry'lerin
     * hangi sırada uygulanacağını belirleriz.
     * 
     * Kullanım alanları:
     * - Database'den çekerken sıralama (ORDER BY sequence)
     * - Ledger işlemlerini uygularken sıra kontrolü
     */
    @Column(name = "entry_sequence", nullable = false)
    private Integer sequence;

    /**
     * Hesap ID - Bu entry hangi hesaba ait
     * 
     * Her ledger entry mutlaka bir hesaba ait olur. Account servisindeki hesap
     * ID'sini
     * burada tutuyoruz. String kullanıyoruz çünkü bu ID Account servisinden
     * geliyor,
     * burada otomatik oluşturulmuyor. UUID kullanmıyoruz çünkü o zaman burada yeni
     * UUID generate etmemiz gerekirdi ama bu ID zaten dışarıdan gelir.
     * 
     * NOT: Foreign key değil, sadece referans tutuyoruz (mikroservis mimarisi)
     */
    @Column(name = "account_id", length = 50, nullable = false)
    private String accountId;

    /**
     * Entry tipi - DEBIT veya CREDIT
     * 
     * Bir entry'nin sadece 2 tipi olabilir: DEBIT (borç/gider) veya CREDIT
     * (alacak/gelir).
     * Başka seçenek yok. @Enumerated(EnumType.STRING) ile database'deki string
     * değeri
     * enum'a çeviriyoruz ve tam tersi.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 10, nullable = false)
    private EntryType entryType;

    /**
     * Tutar - İşlem miktarı
     * 
     * EntryType işlemin yönünü gösterir (DEBIT/CREDIT), amount ise tutarın kendisi.
     * Her zaman pozitif değer tutarız, negatif olmaz.
     * 
     * precision=19, scale=4 → Maksimum 15 basamak tam sayı, 4 basamak ondalık
     */
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    /**
     * Para birimi - ISO 4217 standart kod
     * 
     * İşlem yapılan para birimini 3 karakterle tutuyoruz (ISO standardı).
     * Örnekler: TRY, USD, EUR, GBP
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Açıklama - İşlem açıklaması
     * 
     * Transfer işlemi sırasında sistem veya kullanıcı tarafından eklenen açıklama.
     * Opsiyonel, maksimum 500 karakter.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Referans numarası - Dış sistemin işlem numarası
     * 
     * Dış sistemlerin (banka, kart sistemi, Papara vs) verdiği işlem numarasını
     * buraya
     * yazıyoruz. Bu sayede daha sonra o dış sistemdeki işlemle bizim ledger
     * kaydımızı
     * eşleştirebiliyoruz (reconciliation için önemli).
     * 
     * Kullanım senaryoları:
     * - Banka EFT'si → "EFT-123456" numarasını buraya yazarız
     * - Kart işlemi → "TXN-789012" gibi kart sisteminin verdiği numarayı yazarız
     * - Papara transfer → "PP-987654" gibi Papara'nın referans numarasını yazarız
     * 
     * Bu field sayesinde müşteri "paramı gönderdim ama göremiyorum" dediğinde,
     * verdiği referans numarasıyla ledger'da arama yapıp bulabiliriz.
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    /**
     * İşlem tipi - Transaction türü
     * 
     * Bu işlemin hangi türde olduğunu tutarız.
     * Örnekler: TRANSFER, DEPOSIT, WITHDRAW, FEE (komisyon), REFUND (iade)
     * 
     * Bu bilgi raporlama ve filtreleme için kullanılır. Mesela "son 10 TRANSFER
     * işlemini göster" gibi sorgulamalar yapabiliriz.
     */
    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    /**
     * Entry durumu - Her zaman POSTED
     * 
     * Ledger service'de tüm entry'ler kesinleşmiş olarak gelir, bu yüzden status
     * her zaman "POSTED" olur. Database migration'da status kolonu var ama aktif
     * olarak kullanmıyoruz şu an için.
     * 
     * NOT: İleride "PENDING" veya "REVERSED" gibi durumlar eklenebilir ama şimdilik
     * sadece POSTED kullanıyoruz.
     */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "POSTED";

    /**
     * Muhasebe kayıt tarihi - İşlemin deftere yazıldığı tarih
     * 
     * Bu tarih işlemin muhasebe defterine ne zaman kaydedildiğini gösterir.
     * Transaction'daki postingDate'i buraya kopyalıyoruz (denormalization).
     * 
     * Neden denormalization yapıyoruz?
     * - Performans: Her seferinde Transaction'a join yapmadan entry'leri tarihe
     * göre
     * sorgulayabiliyoruz
     * - Raporlama: Belirli tarih aralığındaki entry'leri hızlıca bulabiliyoruz
     */
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    /**
     * Değer tarihi - Paranın geçerlilik tarihi
     * 
     * İşlemin hangi tarih itibariyle geçerli olduğunu gösterir. Posting date ile
     * aynı
     * olabilir ama bazı durumlarda farklı olabilir.
     * 
     * Örnek 1: Bugün (16 Aralık) bir havale gönderdin ama banka "18 Aralık'ta
     * işleme
     * alacağız" dedi. Bu durumda:
     * - posting_date = 2024-12-16 (bugün deftere kaydedildi)
     * - value_date = 2024-12-18 (para o gün geçerli olacak)
     * 
     * Örnek 2: Vadeli işlemler için value_date ileri bir tarih olabilir.
     * 
     * Transaction'daki valueDate'i buraya kopyalıyoruz (denormalization -
     * performans).
     */
    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    /**
     * Oluşturulma zamanı - Entry ne zaman oluşturuldu
     * 
     * Bu entry'nin database'e ne zaman yazıldığını gösterir. @CreationTimestamp
     * sayesinde Hibernate otomatik olarak şu anki zamanı (UTC) buraya yazar,
     * biz manuel set etmeyiz.
     * 
     * UTC timezone'da saklanır (Instant tipi). Böylece farklı timezone'lardaki
     * serverlar aynı zamanı görür.
     * 
     * updatable=false: Bir kez yazıldı mı asla değiştirilemez
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Bu entry bir DEBIT mi?
     */
    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    /**
     * Bu entry bir CREDIT mi?
     */
    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }

    /**
     * Bakiye hesaplamalarında kullanılır
     * CREDIT: pozitif (+)
     * DEBIT: negatif (-)
     */
    public BigDecimal getSignedAmount() {
        return isCredit() ? amount : amount.negate();
    }
}
