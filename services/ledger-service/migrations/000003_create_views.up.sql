-- Migration Dosyası: Create Views for Ledger Service
-- Versiyon: 000003
-- Açıklama: Raporlama ve analiz VIEW'leri

-- ============================================
-- VIEW 1: vw_account_activities
-- Amaç: Hesap hareketleri özeti (hesap ekstresi gibi)
-- Kullanım: Müşteriye ekstren göstermek, müşteri hizmetleri
-- ============================================
IF OBJECT_ID('dbo.vw_account_activities', 'V') IS NOT NULL
    DROP VIEW dbo.vw_account_activities;
GO

CREATE VIEW dbo.vw_account_activities AS
SELECT 
    le.account_id,
    le.posting_date,                        -- İşlem tarihi
    le.value_date,                          -- Değer tarihi (faiz için)
    lt.transaction_id,
    lt.transaction_type,                    -- TRANSFER, WITHDRAWAL vs.
    le.entry_type,                          -- DEBIT (borç) veya CREDIT (alacak)
    le.amount,
    le.currency,
    le.description,                         -- İşlem açıklaması
    lt.status AS transaction_status,
    le.created_at
FROM dbo.ledger_entries le
INNER JOIN dbo.ledger_transactions lt ON le.transaction_id = lt.transaction_id
WHERE lt.status = 'POSTED';  -- Sadece kesinleşmiş işlemler
GO

-- ============================================
-- VIEW 2: vw_daily_transaction_summary
-- Amaç: Günlük işlem istatistikleri
-- Kullanım: Operasyonel raporlar, günlük analiz
-- Her gün kaç işlem oldu, toplam hacim ne, ortalama tutar ne vs.
-- ============================================
IF OBJECT_ID('dbo.vw_daily_transaction_summary', 'V') IS NOT NULL
    DROP VIEW dbo.vw_daily_transaction_summary;
GO

CREATE VIEW dbo.vw_daily_transaction_summary AS
SELECT 
    posting_date,
    transaction_type,
    COUNT(*) AS transaction_count,          -- Kaç işlem var
    SUM(total_amount) AS total_volume,      -- Toplam hacim
    AVG(total_amount) AS average_amount,    -- Ortalama tutar
    MIN(total_amount) AS min_amount,        -- En küçük işlem
    MAX(total_amount) AS max_amount,        -- En büyük işlem
    currency
FROM dbo.ledger_transactions
WHERE status = 'POSTED'  -- Sadece kesinleşmiş işlemler
GROUP BY posting_date, transaction_type, currency;
GO

-- ============================================
-- VIEW 3: vw_account_balance_with_ledger
-- Amaç: Bakiye doğrulama (cached vs hesaplanan)
-- Kullanım: Veri bütünlüğü kontrolü, audit
-- account_balances'taki bakiye ile ledger_entries'den hesaplanan bakiye karşılaştırması
-- İkisi aynı olmalı, farklıysa bir yerlerde hata var demektir
-- ============================================
IF OBJECT_ID('dbo.vw_account_balance_with_ledger', 'V') IS NOT NULL
    DROP VIEW dbo.vw_account_balance_with_ledger;
GO

CREATE VIEW dbo.vw_account_balance_with_ledger AS
SELECT 
    ab.account_id,
    ab.balance AS cached_balance,  -- account_balances tablosundaki bakiye (cache)
    COALESCE(
        (SELECT SUM(
            CASE 
                WHEN le.entry_type = 'CREDIT' THEN le.amount      -- CREDIT ise + ekle
                WHEN le.entry_type = 'DEBIT' THEN -le.amount      -- DEBIT ise - çıkar
                ELSE 0
            END
        )
        FROM dbo.ledger_entries le
        INNER JOIN dbo.ledger_transactions lt ON le.transaction_id = lt.transaction_id
        WHERE le.account_id = ab.account_id AND lt.status = 'POSTED'
        ), 0
    ) AS calculated_balance,  -- Ledger'dan gerçek hesaplanan bakiye
    ab.blocked_amount,       -- Bloke edilmiş tutar
    ab.pending_debits,       -- Bekleyen borçlar
    ab.pending_credits,      -- Bekleyen alacaklar
    ab.currency,
    ab.last_updated_at
FROM dbo.account_balances ab;
GO

PRINT 'Ledger Service VIEW''leri başarıyla oluşturuldu';
PRINT '  - vw_account_activities';
PRINT '  - vw_daily_transaction_summary';
PRINT '  - vw_account_balance_with_ledger';
