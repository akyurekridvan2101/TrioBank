-- Migration Dosyası: 000007
-- Açıklama: Database Function - IBAN Validation
-- Not: Bu fonksiyon backend'de hesap açarken kullanılıyor

-- ============================================
-- FUNCTION: fn_ValidateIBAN
-- Amaç: Türk IBAN formatını doğrula
-- Kullanım: Backend'de hesap numarası oluştururken validation
-- ============================================

-- Eğer varsa önce sil
IF OBJECT_ID('dbo.fn_ValidateIBAN', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_ValidateIBAN;
GO

CREATE FUNCTION dbo.fn_ValidateIBAN (
    @IBAN NVARCHAR(50)
)
RETURNS BIT  -- 1 = Geçerli, 0 = Geçersiz
AS
BEGIN
    -- TR IBAN Formatı: TR + 2 check digit + 5 banka + 1 reserved + 16 hesap = 26 karakter
    -- Örnek: TR330006100519786457841326
    
    -- Uzunluk kontrolü (26 karakter olmalı)
    IF LEN(@IBAN) != 26 
        RETURN 0;
    
    -- Ülke kodu kontrolü (TR ile başlamalı)
    IF LEFT(@IBAN, 2) != 'TR' 
        RETURN 0;
    
    -- Geri kalan 24 karakter rakam olmalı
    IF ISNUMERIC(SUBSTRING(@IBAN, 3, 24)) = 0 
        RETURN 0;
    
    -- Tüm kontroller geçti, geçerli IBAN
    RETURN 1;
END;
GO

-- ============================================
-- Test Sorguları (Fonksiyonu test et)
-- ============================================

PRINT '';
PRINT '========================================';
PRINT 'fn_ValidateIBAN oluşturuldu!';
PRINT '========================================';
PRINT '';
PRINT 'Test:';
PRINT '  Geçerli IBAN: ' + CAST(dbo.fn_ValidateIBAN('TR330006100519786457841326') AS NVARCHAR(1));
PRINT '  Geçersiz IBAN: ' + CAST(dbo.fn_ValidateIBAN('INVALID123') AS NVARCHAR(1));
PRINT '';
PRINT 'Kullanım: SELECT dbo.fn_ValidateIBAN(account_number) FROM accounts;';
PRINT '';
PRINT 'Backend Entegrasyon:';
PRINT '  - Hesap açarken IBAN validation';
PRINT '  - JdbcTemplate ile çağrılıyor';
PRINT '  - Geçersiz IBAN DB''ye kaydedilmiyor';
PRINT '========================================';
