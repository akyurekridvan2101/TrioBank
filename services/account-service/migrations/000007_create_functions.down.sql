-- Rollback: Drop IBAN validation function

IF OBJECT_ID('dbo.fn_ValidateIBAN', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_ValidateIBAN;

PRINT 'fn_ValidateIBAN fonksiyonu kaldırıldı';
