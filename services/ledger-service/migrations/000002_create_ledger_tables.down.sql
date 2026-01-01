-- ============================================
-- Migration: 000002 (ROLLBACK)
-- Description: Drop ledger service domain tables
-- Author: TrioBank Team
-- WARNING: This will delete ALL ledger data!
-- ============================================

PRINT 'Starting rollback of migration 000002...';
PRINT 'WARNING: This will delete all ledger data!';
PRINT '';
-- GO command removed (not supported by migration tool)

-- ============================================
-- Drop tables in reverse order (respecting FK constraints)
-- ============================================

-- Drop account_balances first (no dependencies)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'account_balances' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.account_balances;
    PRINT 'Table account_balances dropped';
END
ELSE
BEGIN
    PRINT 'Table account_balances does not exist - skipping';
END;
-- GO command removed (not supported by migration tool)

-- Drop ledger_entries second (has FK to ledger_transactions)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'ledger_entries' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.ledger_entries;
    PRINT 'Table ledger_entries dropped';
END
ELSE
BEGIN
    PRINT 'Table ledger_entries does not exist - skipping';
END;
-- GO command removed (not supported by migration tool)

-- Drop ledger_transactions last (referenced by ledger_entries FK)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'ledger_transactions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.ledger_transactions;
    PRINT 'Table ledger_transactions dropped';
END
ELSE
BEGIN
    PRINT 'Table ledger_transactions does not exist - skipping';
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
PRINT '  1. account_balances';
PRINT '  2. ledger_entries';
PRINT '  3. ledger_transactions';
PRINT '';
PRINT 'Note: outbox_events table NOT dropped (managed by migration 000001)';
PRINT '========================================';
-- GO command removed (not supported by migration tool)
