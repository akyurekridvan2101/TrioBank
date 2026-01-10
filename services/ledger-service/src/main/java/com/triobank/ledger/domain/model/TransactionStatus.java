package com.triobank.ledger.domain.model;

/**
 * TransactionStatus - Ledger transaction durumu
 * 
 * Ledger Service'te transaction'lar sadece 2 durumda olabilir:
 * POSTED veya REVERSED. Başka durum YOK!
 * 
 * ⚠️ ÖNEMLİ - Transaction Service ile Fark:
 * -----------------------------------------------------------------------
 * Transaction Service'te:
 * - PENDING: İşlem başlatıldı, henüz tamamlanmadı
 * - IN_PROGRESS: İşlem devam ediyor
 * - COMPLETED: İşlem başarıyla tamamlandı
 * - FAILED: İşlem başarısız oldu
 * 
 * Ledger Service'te (bu enum):
 * - POSTED: Muhasebe kaydı yapıldı (kesinleşti)
 * - REVERSED: Muhasebe kaydı iptal edildi (ters kayıt yapıldı)
 * 
 * Neden sadece 2 durum var?
 * Çünkü Ledger Service SADECE kesinleşmiş işlemleri tutar!
 * PENDING veya IN_PROGRESS işlemler Ledger'a GELMEMELİ.
 * -----------------------------------------------------------------------
 */
public enum TransactionStatus {

    /**
     * POSTED - Kesinleşmiş işlem
     * 
     * Muhasebe kaydı yazıldı, entry'ler oluşturuldu, bakiyeler güncellendi.
     * Bu normal, başarılı bir işlemdir.
     * 
     * Örnek: Ali Ayşe'ye 1000 TL gönderdi, muhasebe kaydı yapıldı.
     */
    POSTED,

    /**
     * REVERSED - İptal edilmiş işlem
     * 
     * ⚠️ ÖNEMLİ: REVERSED = İPTAL (reversal), İADE (refund) DEĞİL!
     * 
     * NASIL ÇALIŞIR:
     * ========================================================================
     * Reversal yapıldığında 2 AYRI TRANSACTION oluşur:
     * 
     * Eski transaction'ın STATUS'Ü güncellenir: POSTED → REVERSED
     * Yeni bir reversal transaction oluşturulur: status = POSTED
     * 
     * ========================================================================
     * 
     * ÖRNEK - Adım Adım:
     * ------------------------------------------------------------------------
     * BAŞLANGIÇ: Ali Ayşe'ye 1000 TL gönderdi
     * 
     * Transaction-1 (TXN-123):
     * - status: POSTED ✅
     * - entries:
     * Ali : -1000 TL (DEBIT)
     * Ayşe : +1000 TL (CREDIT)
     * 
     * NET: Ali -1000, Ayşe +1000
     * ------------------------------------------------------------------------
     * İPTAL İSTEĞİ GELDİ!
     * ------------------------------------------------------------------------
     * 
     * Transaction-1 (TXN-123) GÜNCELLEME:
     * - status: POSTED → REVERSED ❌ (değişti!)
     * - reversedByTransactionId: "TXN-123-REV" (kim iptal etti)
     * - entries: AYNI (değişmedi!)
     * Ali : -1000 TL (DEBIT)
     * Ayşe : +1000 TL (CREDIT)
     * 
     * Transaction-2 (TXN-123-REV) YENİ OLUŞTURULDU:
     * - status: POSTED ✅ (reversal transaction da POSTED!)
     * - isReversal: true
     * - originalTransactionId: "TXN-123" (neyi iptal ediyor)
     * - entries: TERS!
     * Ali : +1000 TL (CREDIT) ← Ters kayıt
     * Ayşe : -1000 TL (DEBIT) ← Ters kayıt
     * 
     * NET SONUÇ:
     * Ali : -1000 (orijinal) + 1000 (reversal) = 0 ✅
     * Ayşe : +1000 (orijinal) - 1000 (reversal) = 0 ✅
     * ------------------------------------------------------------------------
     * 
     * NEDEN 2 TRANSACTION VAR?
     * - Muhasebede hiçbir kayıt silinmez veya değiştirilmez (immutable)
     * - Audit trail için her şey kayıt altında kalmalı
     * - Orijinal işlem ne oldu, iptal ne zaman yapıldı - hepsi görünür
     * 
     * ÖZETLE:
     * - Orijinal transaction: REVERSED durumuna geçer (işaretlenir)
     * - Yeni reversal transaction: POSTED olarak oluşturulur
     * - İkisi birlikte net etkiyi sıfırlar
     */
    REVERSED
}
