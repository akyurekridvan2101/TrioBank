-- Rollback: Drop performance indexes

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_accounts_created_desc')
    DROP INDEX IX_accounts_created_desc ON dbo.accounts;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_accounts_customer_balance_covering')
    DROP INDEX IX_accounts_customer_balance_covering ON dbo.accounts;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_product_definitions_active_filtered')
    DROP INDEX IX_product_definitions_active_filtered ON dbo.product_definitions;

PRINT 'Account Service performance indexleri kaldırıldı';
