-- Migration Dosyası: 000004
-- Açıklama: Performans optimizasyonu için ek indexler
-- Not: Ledger Service için kritik indexler (account_balances'ta hiç index yoktu!)

-- ============================================
-- INDEX 1: account_balances - Currency Index
-- ÖNEMLİ: Bu tabloda hiç index yoktu! (Sadece PK)
-- Amaç: Para birimi bazlı bakiye raporları
-- Kullanım: "TRY bakiyesi olan tüm hesaplar"
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_account_balances_currency' 
    AND object_id = OBJECT_ID('dbo.account_balances')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_account_balances_currency
    ON dbo.account_balances (currency)
    INCLUDE (balance, blocked_amount, account_id);  -- COVERING INDEX
    
    PRINT '✅ IX_account_balances_currency oluşturuldu';
    PRINT '   Tip: COVERING INDEX (para birimi)';
    PRINT '   NOT: Bu tablo için ILK INDEX! (PK hariç)';
END;

-- ============================================
-- INDEX 2: account_balances - Son Güncellenenler (FILTERED)
-- Amaç: Aktif hesapları (balance != 0) hızlıca getirmek
-- Kullanım: Günlük bakiye değişimleri raporu
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_account_balances_updated_active' 
    AND object_id = OBJECT_ID('dbo.account_balances')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_account_balances_updated_active
    ON dbo.account_balances (last_updated_at DESC)
    INCLUDE (account_id, balance, currency)
    WHERE balance != 0;  -- FILTERED: Sadece aktif hesaplar (balance var)
    
    PRINT '✅ IX_account_balances_updated_active oluşturuldu';
    PRINT '   Tip: FILTERED INDEX + DESC (balance != 0)';
    PRINT '   Fayda: Boş hesaplar indexte yok, performans arttı';
END;

-- ============================================
-- INDEX 3: ledger_transactions - Tarih Aralığı (COMPOSITE + INCLUDE)
-- Amaç: Belirli tarihler arası işlem raporları
-- Kullanım: "2025-12'deki tüm işlemler" gibi sorgular
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_ledger_transactions_date_range' 
    AND object_id = OBJECT_ID('dbo.ledger_transactions')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_ledger_transactions_date_range
    ON dbo.ledger_transactions (posting_date, status)  -- COMPOSITE: İki kolon
    INCLUDE (transaction_type, total_amount, currency, transaction_id);  -- COVERING
    
    PRINT '✅ IX_ledger_transactions_date_range oluşturuldu';
    PRINT '   Tip: COMPOSITE + COVERING INDEX';
    PRINT '   Fayda: Tarih aralığı raporları çok hızlı';
END;

-- ============================================
-- INDEX 4: ledger_entries - Entry Type + Account (COMPOSITE)
-- Amaç: DEBIT/CREDIT bazlı hesap hareketleri
-- Kullanım: "Son 30 günde alınan tüm CREDIT'ler"
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_ledger_entries_type_account' 
    AND object_id = OBJECT_ID('dbo.ledger_entries')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_ledger_entries_type_account
    ON dbo.ledger_entries (entry_type, account_id, posting_date DESC)  -- 3 kolonlu COMPOSITE
    INCLUDE (amount, currency, description);  -- COVERING
    
    PRINT '✅ IX_ledger_entries_type_account oluşturuldu';
    PRINT '   Tip: 3-kolonlu COMPOSITE + COVERING';
    PRINT '   Fayda: DEBIT/CREDIT sorguları optimize';
END;

-- ============================================
-- Başarı Mesajı
-- ============================================
PRINT '';
PRINT '========================================';
PRINT 'Ledger Service INDEX''leri eklendi!';
PRINT '========================================';
PRINT 'Toplam 4 yeni index:';
PRINT '  1. COVERING INDEX (currency - account_balances)';
PRINT '  2. FILTERED INDEX (aktif hesaplar - balance!=0)';
PRINT '  3. COMPOSITE INDEX (tarih aralığı raporları)';
PRINT '  4. COMPOSITE INDEX (DEBIT/CREDIT sorguları)';
PRINT '';
PRINT '⚠️  ÖNEMLİ: account_balances için ilk indexler!';
PRINT 'Backend kodu değişmedi ✅';
PRINT 'Performans ÇOK arttı ✅';
PRINT '========================================';
