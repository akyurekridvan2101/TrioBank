-- ============================================
-- Migration: 000002 (ROLLBACK)
-- Description: Drop account service domain tables
-- Author: TrioBank Team
-- WARNING: This will delete ALL account data!
-- ============================================

PRINT 'Starting rollback of migration 000002...';
PRINT 'WARNING: This will delete all account data!';
PRINT '';
-- GO command removed (not supported by migration tool)

-- ============================================
-- Drop tables in reverse order (respecting FK constraints)
-- ============================================

-- Drop accounts first (has FK to product_definitions)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'accounts' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.accounts;
    PRINT 'Table accounts dropped';
END
ELSE
BEGIN
    PRINT 'Table accounts does not exist - skipping';
END;
-- GO command removed (not supported by migration tool)

-- Drop product_definitions last (referenced by accounts FK)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'product_definitions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.product_definitions;
    PRINT 'Table product_definitions dropped';
END
ELSE
BEGIN
    PRINT 'Table product_definitions does not exist - skipping';
END;
-- GO command removed (not supported by migration tool)

-- ============================================
-- Verification
-- ============================================
PRINT '';
PRINT '========================================';
PRINT 'Migration 000002 rolled back successfully';
PRINT '========================================';
PRINT 'Tables dropped:';
PRINT '  1. accounts';
PRINT '  2. product_definitions';
PRINT '';
PRINT 'Note: outbox_events table NOT dropped (managed by migration 000001)';
PRINT '========================================';
-- GO command removed (not supported by migration tool)
