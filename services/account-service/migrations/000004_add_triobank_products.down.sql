-- Migration Version: 000004 (ROLLBACK)
-- Description: Remove TrioBank-specific products and restore seed data

-- ============================================
-- Delete TrioBank Product
-- ============================================
DELETE FROM dbo.product_definitions WHERE code = 'CHECKING_TRY';

PRINT 'TrioBank product removed (CHECKING_TRY)';

-- ============================================
-- Restore Original Seed Data
-- ============================================
INSERT INTO dbo.product_definitions (code, name, category, features, is_active)
VALUES 
    (
        'RETAIL_TRY', 
        'Vadesiz TL Hesabı', 
        'CHECKING', 
        '{"currency":"TRY","allow_negative":true,"interest_rate":0.0,"monthly_fee":0.0}',
        1
    ),
    (
        'SAVINGS_USD', 
        'USD Tasarruf Hesabı', 
        'SAVINGS', 
        '{"currency":"USD","allow_negative":false,"interest_rate":2.5,"monthly_fee":0.0,"minimum_balance":100.0}',
        1
    ),
    (
        'GOLD_ACC', 
        'Altın Hesabı', 
        'SAVINGS', 
        '{"currency":"XAU","allow_negative":false,"interest_rate":0.0,"monthly_fee":5.0,"purity":995}',
        1
    ),
    (
        'CREDIT_STD', 
        'Standart Kredi Kartı', 
        'CREDIT', 
        '{"currency":"TRY","billing_cycle":30,"limit_default":5000.0,"interest_rate":3.99,"grace_period":45}',
        1
    );

PRINT 'Original seed data restored';
PRINT 'Migration 000004 rolled back successfully';

-- Verification
SELECT code, name, category, is_active FROM dbo.product_definitions ORDER BY code;
