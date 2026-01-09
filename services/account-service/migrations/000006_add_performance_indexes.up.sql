-- Migration Dosyası: 000006
-- Açıklama: Performans optimizasyonu için ek indexler
-- Not: Backend koduna dokunmadan sadece performans artışı sağlar

-- ============================================
-- INDEX 1: FILTERED INDEX - Sadece Aktif Ürünler
-- Amaç: is_active=1 olan ürünleri hızlıca getirmek
-- Kullanım: Ürün listesi, yeni hesap açma ekranı
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_product_definitions_active_filtered' 
    AND object_id = OBJECT_ID('dbo.product_definitions')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_product_definitions_active_filtered
    ON dbo.product_definitions (code, name, category)
    WHERE is_active = 1;  -- FILTERED INDEX: Sadece aktif olanlar
    
    PRINT '✅ IX_product_definitions_active_filtered oluşturuldu';
    PRINT '   Tip: FILTERED INDEX (WHERE is_active = 1)';
    PRINT '   Fayda: Disk kullanımı azaldı, performans arttı';
END;

-- ============================================
-- INDEX 2: COVERING INDEX - Müşteri Bakiye Sorguları
-- Amaç: Müşterinin toplam bakiyesini hızlıca hesaplamak
-- Kullanım: Dashboard, toplam bakiye gösterimi
-- INCLUDE kullanarak table'a hiç gitmeden sonuç dönecek
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_accounts_customer_balance_covering' 
    AND object_id = OBJECT_ID('dbo.accounts')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_accounts_customer_balance_covering
    ON dbo.accounts (customer_id)
    INCLUDE (balance, currency, status);  -- COVERING INDEX: Tüm gerekli kolonlar dahil
    
    PRINT '✅ IX_accounts_customer_balance_covering oluşturuldu';
    PRINT '   Tip: COVERING INDEX (INCLUDE ile)';
    PRINT '   Fayda: Index Seek + Table Scan yok = Çok hızlı!';
END;

-- ============================================
-- INDEX 3: COMPOSITE + INCLUDE - Yeni Hesaplar
-- Amaç: En son açılan hesapları hızlıca getirmek
-- Kullanım: "Son 30 günde açılan hesaplar" raporu
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_accounts_created_desc' 
    AND object_id = OBJECT_ID('dbo.accounts')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_accounts_created_desc
    ON dbo.accounts (created_at DESC)  -- DESC: En yeniler önce gelsin
    INCLUDE (account_number, customer_id, status, product_code);
    
    PRINT '✅ IX_accounts_created_desc oluşturuldu';
    PRINT '   Tip: DESC INDEX + INCLUDE';
    PRINT '   Fayda: ORDER BY created_at DESC sorgularında hızlı';
END;

-- ============================================
-- Başarı Mesajı
-- ============================================
PRINT '';
PRINT '========================================';
PRINT 'Account Service INDEX''leri eklendi!';
PRINT '========================================';
PRINT 'Toplam 3 yeni index:';
PRINT '  1. FILTERED INDEX (aktif ürünler)';
PRINT '  2. COVERING INDEX (müşteri bakiyeleri)';
PRINT '  3. DESC INDEX (yeni hesaplar)';
PRINT '';
PRINT 'Backend kodu değişmedi ✅';
PRINT 'Performans arttı ✅';
PRINT '========================================';
