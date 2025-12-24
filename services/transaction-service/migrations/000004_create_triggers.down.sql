-- Rollback: Drop Transaction Started Trigger

IF OBJECT_ID('dbo.trg_TransactionStarted_InsertOutboxEvent', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_TransactionStarted_InsertOutboxEvent;

PRINT 'Trigger trg_TransactionStarted_InsertOutboxEvent kaldırıldı';
