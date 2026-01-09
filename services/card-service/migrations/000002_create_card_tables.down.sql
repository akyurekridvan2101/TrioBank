-- Migration Rollback Version: 000002
-- Description: Drop Card Service domain tables
-- WARNING: This will delete all card data!

-- ============================================
-- 1. Drop Cards Table
-- ============================================
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'cards' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.cards;
END;
