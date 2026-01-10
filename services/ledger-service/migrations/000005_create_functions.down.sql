-- Rollback: Drop Ledger functions and procedures

IF OBJECT_ID('dbo.sp_GetMonthlyTransactionSummary', 'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_GetMonthlyTransactionSummary;

IF OBJECT_ID('dbo.fn_CalculateAccountBalance', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_CalculateAccountBalance;

PRINT 'Ledger Service functions ve procedures kaldırıldı';
