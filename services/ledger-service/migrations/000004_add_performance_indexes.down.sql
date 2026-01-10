-- Rollback: Drop performance indexes

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ledger_entries_type_account')
    DROP INDEX IX_ledger_entries_type_account ON dbo.ledger_entries;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ledger_transactions_date_range')
    DROP INDEX IX_ledger_transactions_date_range ON dbo.ledger_transactions;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_account_balances_updated_active')
    DROP INDEX IX_account_balances_updated_active ON dbo.account_balances;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_account_balances_currency')
    DROP INDEX IX_account_balances_currency ON dbo.account_balances;

PRINT 'Ledger Service performance indexleri kaldırıldı';
