-- Migration Rollback Version: 000002
-- Description: Drop transactions table

IF EXISTS (SELECT * FROM sys.tables WHERE name = 'transactions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.transactions;
    PRINT 'Table transactions dropped successfully';
END
ELSE
BEGIN
    PRINT 'Table transactions does not exist - nothing to drop';
END;

PRINT '';
PRINT '========================================';
PRINT 'Migration 000002 rollback completed';
PRINT '========================================';
