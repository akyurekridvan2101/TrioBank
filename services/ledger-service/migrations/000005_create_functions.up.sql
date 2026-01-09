-- Migration Dosyası: 000005
-- Açıklama: Database Functions & Procedures
-- Not: Bu objeler backend'de raporlama ve hesaplama için kullanılıyor

-- ============================================
-- FUNCTION 1: fn_CalculateAccountBalance
-- Amaç: Ledger entries'den gerçek bakiyeyi hesapla
-- Kullanım: Bakiye doğrulama, cache validation
-- ============================================

-- Eğer varsa önce sil
IF OBJECT_ID('dbo.fn_CalculateAccountBalance', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_CalculateAccountBalance;
GO

CREATE FUNCTION dbo.fn_CalculateAccountBalance (
    @AccountId NVARCHAR(50)
)
RETURNS DECIMAL(19, 4)
AS
BEGIN
    DECLARE @Balance DECIMAL(19, 4);
    
    -- Ledger entries'den gerçek bakiyeyi hesapla
    -- CREDIT (alacak) = pozitif, DEBIT (borç) = negatif
    SELECT @Balance = COALESCE(SUM(
        CASE 
            WHEN le.entry_type = 'CREDIT' THEN le.amount     -- Para girişi
            WHEN le.entry_type = 'DEBIT' THEN -le.amount     -- Para çıkışı
            ELSE 0
        END
    ), 0)
    FROM ledger_entries le
    INNER JOIN ledger_transactions lt ON le.transaction_id = lt.transaction_id
    WHERE le.account_id = @AccountId 
      AND lt.status = 'POSTED';  -- Sadece kesinleşmiş işlemler
    
    RETURN @Balance;
END;
GO

PRINT '✅ fn_CalculateAccountBalance oluşturuldu';

-- ============================================
-- STORED PROCEDURE: sp_GetMonthlyTransactionSummary
-- Amaç: Aylık işlem istatistiklerini getir
-- Kullanım: Yönetim raporları, dashboard
-- ============================================

-- Eğer varsa önce sil
IF OBJECT_ID('dbo.sp_GetMonthlyTransactionSummary', 'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_GetMonthlyTransactionSummary;
GO

CREATE PROCEDURE dbo.sp_GetMonthlyTransactionSummary
    @Year INT,   -- Hangi yıl: 2025
    @Month INT   -- Hangi ay: 1-12
AS
BEGIN
    SET NOCOUNT ON;  -- Performans için
    
    -- İşlem tipine göre istatistikler
    SELECT 
        transaction_type,                          -- TRANSFER, WITHDRAWAL, DEPOSIT vs.
        COUNT(*) AS transaction_count,             -- Kaç işlem var
        SUM(total_amount) AS total_volume,         -- Toplam hacim
        AVG(total_amount) AS average_amount,       -- Ortalama tutar
        MIN(total_amount) AS min_amount,           -- En küçük işlem
        MAX(total_amount) AS max_amount,           -- En büyük işlem
        currency                                   -- Para birimi
    FROM ledger_transactions
    WHERE YEAR(posting_date) = @Year
      AND MONTH(posting_date) = @Month
      AND status = 'POSTED'  -- Sadece kesinleşmiş işlemler
    GROUP BY transaction_type, currency
    ORDER BY total_volume DESC;  -- En yüksek hacim önce
END;
GO

PRINT '✅ sp_GetMonthlyTransactionSummary oluşturuldu';

-- ============================================
-- Test & Kullanım Örnekleri
-- ============================================

PRINT '';
PRINT '========================================';
PRINT 'Ledger Service Functions & Procedures';
PRINT '========================================';
PRINT '';
PRINT 'FUNCTION: fn_CalculateAccountBalance';
PRINT '  Kullanım:';
PRINT '    SELECT dbo.fn_CalculateAccountBalance(''ACC-123'');';
PRINT '  Backend:';
PRINT '    - Bakiye sorgularında kullanılıyor';
PRINT '    - Cache validation için';
PRINT '';
PRINT 'PROCEDURE: sp_GetMonthlyTransactionSummary';
PRINT '  Kullanım:';
PRINT '    EXEC dbo.sp_GetMonthlyTransactionSummary @Year=2025, @Month=12;';
PRINT '  Backend:';
PRINT '    - Aylık raporlar için';
PRINT '    - Dashboard istatistikleri';
PRINT '';
PRINT '========================================';
