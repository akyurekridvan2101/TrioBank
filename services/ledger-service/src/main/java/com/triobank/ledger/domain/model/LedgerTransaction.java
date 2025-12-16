package com.triobank.ledger.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * LedgerTransaction - Muhasebe işlem başlığı
 * 
 * Her finansal işlem için oluşturulan kayıt. LedgerEntry'leri gruplar.
 * 
 * Database: ledger_transactions tablosu
 * 
 * ⚠️ ÇOK ÖNEMLİ - Transaction Service ile İlişki:
 * SORU: Bu LedgerTransaction ile Transaction Service'teki Transaction aynı mı?
 * CEVAP: HAYIR! İkisi farklı şeyler ama ilişkililer.
 * 
 * FARK:
 * - Transaction Service'teki Transaction: İşlem talebi (request/command)
 * → "Ali, Ayşe'ye 1000 TL gönder" komutu
 * → İşlem durumunu yönetir (PENDING, COMPLETED, FAILED)
 * → İş sürecini (workflow) kontrol eder
 * 
 * - LedgerTransaction (bu class): Muhasebeye yazılan kayıt
 * → "Ali'nin hesabından -1000 TL, Ayşe'nin hesabına +1000 TL"
 * → Sadece kesinleşmiş işlemleri tutar (POSTED veya REVERSED)
 * → Finans/muhasebe kayıtları için
 * 
 * BAĞLANTI:
 * Transaction Service bir işlem başlattığında Kafka event yayınlar.
 * Ledger Service bu event'i alır ve LedgerTransaction + LedgerEntry'ler
 * oluşturur.
 * 
 * transactionId field'ı ikisini birbirine bağlar:
 * - Transaction Service: transaction.getId() → "TXN-123"
 * - Ledger Service: ledgerTransaction.getTransactionId() → "TXN-123" (aynı!)
 * 
 * Önemli:
 * - Immutable: Bir kez oluşturuldu mu değiştirilemez (sadece status
 * güncellenebilir)
 * - Her transaction en az 2 entry içerir (çift taraflı muhasebe)
 */
@Entity
@Table(name = "ledger_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA için
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder için
@Builder
public class LedgerTransaction {

    /**
     * Transaction ID - Business key (Transaction Service ile bağlantı)
     * 
     * Transaction Service'teki transaction ID'si ile AYNI değer.
     * Bu sayede iki mikroservis arasında ilişki kurulur.
     * 
     * Akış:
     * 1. Transaction Service: Yeni işlem oluştur → ID: "TXN-123"
     * 2. Transaction Service: Kafka'ya event yayınla → transactionId: "TXN-123"
     * 3. Ledger Service: Event'i al, LedgerTransaction oluştur → transactionId:
     * "TXN-123"
     * 
     * Böylece eğer Transaction Service'te "TXN-123" işlemini sorgularsak,
     * Ledger Service'te de aynı ID ile muhasebe kayıtlarını buluruz.
     * 
     * Örnekler: "TXN-001", "TXN-20241216-123456"
     */
    @Id
    @Column(name = "transaction_id", length = 100, nullable = false)
    private String transactionId;

    /**
     * Transaction tipi - İşlem kategorisi
     * 
     * Transaction Service'ten gelen işlem tipi bilgisi.
     * Raporlama ve kategorilendirme için kullanılır.
     * 
     * Örnekler: "TRANSFER", "CARD_PAYMENT", "EFT", "FAST", "LOAN_DISBURSEMENT"
     */
    @Column(name = "transaction_type", length = 50, nullable = false)
    private String transactionType;

    /**
     * Transaction tarihi - Teknik timestamp (ne zaman kaydedildi)
     * 
     * Hibernate otomatik olarak bu transaction'\u0131n database'e yazıldı\u011fı
     * an\u0131 buraya yazar.
     * updatable=false: Bir kez yazıldı mı asla de\u011fi\u015ftirilemez.
     * 
     * posting_date ile fark\u0131: Bu teknik timestamp, posting_date iş
     * g\u00fcn\u00fc.
     */
    @CreationTimestamp
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private Instant transactionDate;

    /**
     * Posting date - Muhasebe kayit tarihi (iş günü)
     * 
     * İşlemin muhasebe defterinde hangi güne kaydedileceğini belirler.
     * Genelde bugünün tarihi kullanılır ama gerekirse farklı tarih verilebilir.
     * 
     * Örnek: Cumartesi yapılan bir işlem Pazartesi gününe kaydedilebilir.
     */
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    /**
     * Value date - Değer tarihi (faiz başlangıç tarihi)
     * 
     * Paranın "değer" kazanmaya başladığı tarih. Faiz hesaplamalarında kullanılır.
     * Genelde posting_date ile aynı ama bazı özel durumlarda farklı olabilir.
     * 
     * Örnek: Cum.a yatırılan para Pazartesi'den itibaren faiz kazanabilir.
     */
    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    /**
     * Toplam tutar - Tüm entry'lerin toplam tutarı
     * 
     * Bu transaction'daki tüm ledger entry'lerin DEBIT veya CREDIT toplamı.
     * Çift taraflı muhasebede DEBIT toplam = CREDIT toplam olmalı.
     * 
     * Örnek: Ali Ayşe'ye 1000 TL gönderdi → totalAmount = 1000
     */
    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    /**
     * Para birimi - ISO 4217 standart kod
     * 
     * Tüm entry'ler aynı currency'de olmalı.
     * Örnekler: "TRY", "USD", "EUR"
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Transaction durumu - POSTED veya REVERSED
     * 
     * POSTED: Normal kesineşmiş işlem
     * REVERSED: İptal edilmiş işlem (ters kayıt yapıldı)
     * 
     * Detaylı açıklama için TransactionStatus enum'a bak.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatus status;

    /**
     * Açıklama - İşlem detayı (kullanıcıya gösterilecek)
     * 
     * İnsan tarafından okunabilir açıklama.
     * Örnek: "Ali'den Ayşe'ye transfer", "ATM'den para çekimi"
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * İşlemi başlatan hesap ID - Kaynak hesap
     * 
     * Transaction'ı başlatan (giren, talep eden) hesabın ID'si.
     * Transfer'de gönderen, çekimde çeken hesap.
     * 
     * Örnek: "ACC-ALI" (Ali'nin hesabı)
     */
    @Column(name = "initiator_account_id", length = 50)
    private String initiatorAccountId;

    /**
     * Bu işlem bir reversal (iptal) mi?
     * 
     * true: Bu bir iptal işlemi (orijinal işlemi tersleyecek)
     * false: Normal işlem
     * 
     * Örnek:
     * - Transaction-1: isReversal=false (normal transfer)
     * - Transaction-2: isReversal=true (Transaction-1'ı iptal ediyor)
     */
    @Column(name = "is_reversal", nullable = false)
    @Builder.Default
    private Boolean isReversal = false;

    /**
     * Orijinal transaction ID - Eğer bu bir reversal ise
     * 
     * Bu alan SADECE isReversal=true olan transaction'larda dolu.
     * Hangi transaction'ı iptal ediyoruz?
     * 
     * Örnek:
     * - Transaction-1 (TXN-123): Normal transfer
     * - Transaction-2 (TXN-123-REV): isReversal=true,
     * originalTransactionId="TXN-123"
     * 
     * null: Normal transaction (reversal değil)
     */
    @Column(name = "original_transaction_id", length = 100)
    private String originalTransactionId;

    /**
     * Bu transaction hangi transaction tarafından iptal edildi?
     * 
     * Bu alan SADECE status=REVERSED olan transaction'larda dolu.
     * Kim beni iptal etti?
     * 
     * Örnek:
     * - Transaction-1 (TXN-123): status=REVERSED,
     * reversedByTransactionId="TXN-123-REV"
     * - Transaction-2 (TXN-123-REV): status=POSTED, reversedByTransactionId=null
     * 
     * null: Henüz iptal edilmemiş (status=POSTED)
     */
    @Column(name = "reversed_by_transaction_id", length = 100)
    private String reversedByTransactionId;

    /**
     * Oluşturulma zamanı - Transaction ne zaman oluşturuldu
     * 
     * Hibernate otomatik olarak şu anki zamanı (UTC) buraya yazar.
     * updatable=false: Bir kez yazıldı mı asla değiştirilemez.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Bu transaction'a ait ledger entry'ler (One-to-Many)
     * 
     * Her transaction en az 2 entry içerir (çift taraflı muhasebe).
     * 
     * JPA relationship detayları:
     * - mappedBy="transaction": LedgerEntry.transaction field'ı owner
     * - cascade=ALL: Transaction silinirse entry'ler de silinir
     * - orphanRemoval=true: Collection'dan çıkarılan entry silinir
     * - fetch=LAZY: Performans için (entry'leri sadece gerektiğinde yükle)
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LedgerEntry> entries = new ArrayList<>();

    /**
     * Entry ekle - Bidirectional ilişki için helper metot
     * 
     * Transaction'a yeni bir entry ekler. JPA bidirectional ilişkisini
     * yönetmek için kullanılır.
     * 
     * NOT: entry.setTransaction(this) çağrısı LedgerEntry tarafında yapılacak.
     */
    public void addEntry(LedgerEntry entry) {
        entries.add(entry);
        // Not: entry.setTransaction(this) LedgerEntry'de yapılacak
    }

    /**
     * Bu transaction iptal edilmiş mi?
     * 
     * @return true: REVERSED durumunda, false: POSTED durumunda
     */
    public boolean isReversed() {
        return status == TransactionStatus.REVERSED;
    }

    /**
     * Transaction'ı iptal edilmiş olarak işaretle
     * 
     * IMPORTANT:
     * ----------
     * Bu metod MUTABLE bir operasyondur. Transaction entity'sinin sadece
     * status ve reversedByTransactionId field'larını günceller.
     * 
     * NEDEN MUTABLE:
     * --------------
     * Transaction zaten database'e kaydedildi (immutable kısmı).
     * Sonradan sadece status güncellemesi gerekiyor (lifecycle değişimi).
     * Builder ile yeniden oluşturursak:
     * - createdAt üzerine yazılabilir
     * - entries ilişkisi kaybolabilir
     * - Database ID çakışması olabilir
     * 
     * USAGE:
     * ------
     * reverseTransaction() metodu bu metodu çağırır:
     * 
     * LedgerTransaction original = repository.findById(...);
     * original.markAsReversed(reversalTransactionId);
     * repository.save(original); // ✅ Sadece status güncellenir
     * 
     * @param reversedByTransactionId İptal eden transaction'ın ID'si
     * @throws IllegalStateException Eğer transaction zaten reversed ise
     */
    public void markAsReversed(String reversedByTransactionId) {
        // Guard clause: Zaten reversed ise hata fırlat
        if (this.status == TransactionStatus.REVERSED) {
            throw new IllegalStateException(
                    String.format("Transaction %s is already reversed", this.transactionId));
        }

        // Status'ü güncelle
        this.status = TransactionStatus.REVERSED;

        // Reversal transaction ID'sini set et
        this.reversedByTransactionId = reversedByTransactionId;

        // Not: save() repository'de yapılacak
        // Hibernate dirty checking otomatik UPDATE yapacak
    }

    /**
     * Entry sayısı - Bu transaction kaç entry içeriyor
     * 
     * @return Entry sayısı (minimum 2 olmalı - çift taraflı muhasebe)
     */
    public int getEntryCount() {
        return entries.size();
    }
}
