-- Rollback: Drop Transaction Service views

IF OBJECT_ID('dbo.vw_failed_transactions', 'V') IS NOT NULL
    DROP VIEW dbo.vw_failed_transactions;

IF OBJECT_ID('dbo.vw_transaction_flow', 'V') IS NOT NULL
    DROP VIEW dbo.vw_transaction_flow;

PRINT 'Transaction Service views dropped';
