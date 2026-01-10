-- Migration Dosyası: 000003
-- Açıklama: Performans optimizasyonu için ek indexler
-- Not: Card Service için kritik indexler

-- ============================================
-- INDEX 1: Süresi Dolacak Kartlar (FILTERED + COMPOSITE)
-- Amaç: Süresi dolmak üzere olan kartları hızlıca bulmak
-- Kullanım: "30 gün içinde süresi dolacak kartlar" bildirimi
-- FILTERED INDEX: Sadece aktif kartlar
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_cards_expiring_soon' 
    AND object_id = OBJECT_ID('dbo.cards')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_cards_expiring_soon
    ON dbo.cards (expiry_year, expiry_month, account_id)
    INCLUDE (card_number, cardholder_name, card_type)
    WHERE status = 'ACTIVE';  -- FILTERED: Sadece aktif kartlar
    
    PRINT '✅ IX_cards_expiring_soon oluşturuldu';
    PRINT '   Tip: FILTERED INDEX (status = ACTIVE)';
    PRINT '   Fayda: Kart yenileme bildirimleri optimize';
END;

-- ============================================
-- INDEX 2: Kart Tipi + Durum (COMPOSITE + INCLUDE)
-- Amaç: Kart tipi ve durum bazlı istatistikler
-- Kullanım: "Kaç tane aktif DEBIT kart var?" gibi sorgular
-- ============================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_cards_type_status' 
    AND object_id = OBJECT_ID('dbo.cards')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_cards_type_status
    ON dbo.cards (card_type, status)  -- COMPOSITE: İki kolon
    INCLUDE (account_id, created_at, card_brand);  -- COVERING
    
    PRINT '✅ IX_cards_type_status oluşturuldu';
    PRINT '   Tip: COMPOSITE + COVERING INDEX';
    PRINT '   Fayda: Kart tipi raporları hızlı';
END;

-- ============================================
-- Başarı Mesajı
-- ============================================
PRINT '';
PRINT '========================================';
PRINT 'Card Service INDEX''leri eklendi!';
PRINT '========================================';
PRINT 'Toplam 2 yeni index:';
PRINT '  1. FILTERED INDEX (süresi dolacak kartlar)';
PRINT '  2. COMPOSITE INDEX (kart tipi istatistikleri)';
PRINT '';
PRINT 'Backend kodu değişmedi ✅';
PRINT 'Performans arttı ✅';
PRINT '========================================';
