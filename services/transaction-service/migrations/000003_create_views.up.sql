-- Migration Dosyası: Create Views for Transaction Service
-- Versiyon: 000003
-- Açıklama: Raporlama ve analiz VIEW'leri

-- ============================================
-- VIEW 1: vw_transaction_flow
-- Amaç: İşlem durumu dağılımı ve başarı oranları
-- Kullanım: Monitoring, performans analizi
-- PENDING/COMPLETED/FAILED oranları ne
-- ============================================
IF OBJECT_ID('dbo.vw_transaction_flow', 'V') IS NOT NULL
    DROP VIEW dbo.vw_transaction_flow;
GO

CREATE VIEW dbo.vw_transaction_flow AS
SELECT 
    transaction_type,                                       -- TRANSFER, WITHDRAWAL, PURCHASE
    status,                                                 -- PENDING, COMPLETED, FAILED
    COUNT(*) AS transaction_count,                          -- Kaç işlem
    AVG(total_amount) AS average_amount,                    -- Ortalama tutar
    SUM(total_amount) AS total_volume,                      -- Toplam hacim
    currency,
    CAST(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY transaction_type) AS DECIMAL(5,2)) AS percentage  -- Yüzde hesaplama
FROM dbo.transactions
GROUP BY transaction_type, status, currency;
GO

-- ============================================
-- VIEW 2: vw_failed_transactions
-- Amaç: Başarısız işlemlerin detaylı analizi
-- Kullanım: Problem analizi, hata takibi
-- Hangi işlemler neden başarısız oldu, hangi adımda hata oldu vs.
-- ============================================
IF OBJECT_ID('dbo.vw_failed_transactions', 'V') IS NOT NULL
    DROP VIEW dbo.vw_failed_transactions;
GO

CREATE VIEW dbo.vw_failed_transactions AS
SELECT 
    id AS transaction_id,
    transaction_type,
    from_account_id,
    to_account_id,
    total_amount,
    currency,
    failure_reason,                                         -- INSUFFICIENT_FUNDS, CARD_BLOCKED vs.
    failure_details,                                        -- Hata detay açıklaması
    failed_step,                                            -- VALIDATION, BALANCE_CHECK vs.
    created_at,
    failed_at,
    DATEDIFF(SECOND, created_at, failed_at) AS seconds_to_failure  -- Kaç saniyede fail oldu
FROM dbo.transactions
WHERE status = 'FAILED';  -- Sadece başarısız işlemler
GO

PRINT 'Transaction Service VIEW''leri başarıyla oluşturuldu';
PRINT '  - vw_transaction_flow';
PRINT '  - vw_failed_transactions';
