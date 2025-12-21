-- Migration Version: 000004
-- Description: Add TrioBank-specific product definitions
-- Purpose: Replace seed data with real TrioBank products

-- ============================================
-- Delete Old Seed Products
-- ============================================
DELETE FROM dbo.product_definitions WHERE code IN ('RETAIL_TRY', 'SAVINGS_USD', 'GOLD_ACC', 'CREDIT_STD');
PRINT 'Old seed products removed';

-- ============================================
-- Insert TrioBank Products (Only CHECKING_TRY)
-- ============================================

INSERT INTO dbo.product_definitions (code, name, category, features, default_configuration, is_active)
VALUES 
    (
        'CHECKING_TRY',
        'Vadesiz TL Hesabı',
        'CHECKING',
        '{
            "currency": "TRY",
            "allowNegative": false,
            "minimumBalance": 0.0,
            "monthlyFee": 0.0
        }',
        '{
            "dailyTransactionLimit": 50000.0,
            "dailyWithdrawalLimit": 10000.0,
            "emailNotifications": true,
            "smsNotifications": false
        }',
        1
    );

PRINT '======================================';
PRINT 'Migration 000004 completed successfully';
PRINT '======================================';
PRINT 'TrioBank product created:';
PRINT '  - CHECKING_TRY: Vadesiz TL Hesabı';
PRINT '';
PRINT 'Total: 1 product added';
PRINT '======================================';

-- Verification
SELECT code, name, category, is_active FROM dbo.product_definitions ORDER BY category, code;
