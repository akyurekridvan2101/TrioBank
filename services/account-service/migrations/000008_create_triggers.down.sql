-- Rollback: Drop Account Creation Trigger

IF OBJECT_ID('dbo.trg_AccountCreated_InsertOutboxEvent', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_AccountCreated_InsertOutboxEvent;

PRINT 'Trigger trg_AccountCreated_InsertOutboxEvent kaldırıldı';
