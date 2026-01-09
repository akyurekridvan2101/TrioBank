-- Rollback: Drop Ledger Service views

IF OBJECT_ID('dbo.vw_account_balance_with_ledger', 'V') IS NOT NULL
    DROP VIEW dbo.vw_account_balance_with_ledger;

IF OBJECT_ID('dbo.vw_daily_transaction_summary', 'V') IS NOT NULL
    DROP VIEW dbo.vw_daily_transaction_summary;

IF OBJECT_ID('dbo.vw_account_activities', 'V') IS NOT NULL
    DROP VIEW dbo.vw_account_activities;

PRINT 'Ledger Service views dropped';
