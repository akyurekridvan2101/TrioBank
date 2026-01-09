-- Rollback: Drop Account Service views

IF OBJECT_ID('dbo.vw_active_accounts_by_product', 'V') IS NOT NULL
    DROP VIEW dbo.vw_active_accounts_by_product;

IF OBJECT_ID('dbo.vw_account_summary', 'V') IS NOT NULL
    DROP VIEW dbo.vw_account_summary;

PRINT 'Account Service views dropped';
