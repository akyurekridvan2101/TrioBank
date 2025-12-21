-- Migration Dosyası: Create Views for Account Service
-- Versiyon: 000005
-- Açıklama: Raporlama ve analiz için VIEW'ler

-- ============================================
-- VIEW 1: vw_account_summary
-- Amaç: Hesap bilgilerini ürün bilgileriyle birlikte göster
-- Kullanım: Dashboard, raporlar, müşteri bilgileri
-- ============================================
IF OBJECT_ID('dbo.vw_account_summary', 'V') IS NOT NULL
    DROP VIEW dbo.vw_account_summary;  -- Varsa önce sil
GO

CREATE VIEW dbo.vw_account_summary AS
SELECT 
    a.id AS account_id,
    a.customer_id,
    a.account_number,
    a.balance,
    a.currency,
    a.status,
    p.name AS product_name,              -- Ürün adı (Vadesiz TL Hesabı vs.)
    p.category AS product_category,      -- CHECKING, SAVINGS, CREDIT
    a.created_at AS account_open_date,
    DATEDIFF(DAY, a.created_at, GETDATE()) AS days_since_opening  -- Kaç gündür açık
FROM dbo.accounts a
INNER JOIN dbo.product_definitions p ON a.product_code = p.code;
GO

-- ============================================
-- VIEW 2: vw_active_accounts_by_product
-- Amaç: Ürün bazında hesap istatistikleri
-- Kullanım: Yönetim raporları, pazarlama analizleri
-- Her üründen kaç hesap var, toplam/ortalama bakiyeler ne
-- ============================================
IF OBJECT_ID('dbo.vw_active_accounts_by_product', 'V') IS NOT NULL
    DROP VIEW dbo.vw_active_accounts_by_product;
GO

CREATE VIEW dbo.vw_active_accounts_by_product AS
SELECT 
    p.code AS product_code,
    p.name AS product_name,
    p.category,
    COUNT(a.id) AS total_accounts,           -- Kaç hesap var
    SUM(a.balance) AS total_balance,         -- Toplam bakiye
    AVG(a.balance) AS average_balance,       -- Ortalama bakiye
    MIN(a.balance) AS min_balance,           -- En düşük bakiye
    MAX(a.balance) AS max_balance            -- En yüksek bakiye
FROM dbo.product_definitions p
LEFT JOIN dbo.accounts a ON p.code = a.product_code AND a.status = 'ACTIVE'
GROUP BY p.code, p.name, p.category;
GO

PRINT 'Account Service VIEW''leri başarıyla oluşturuldu';
PRINT '  - vw_account_summary';
PRINT '  - vw_active_accounts_by_product';
