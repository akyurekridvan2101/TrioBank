-- Migration Dosyası: 000004
-- Açıklama: TRIGGER - Transaction Started Event (DISABLED)
-- UYARI: Bu trigger başlangıçta DISABLED! Backend OutboxService kullandığı için duplicate event yaratmasın.

-- ============================================
-- TRIGGER: trg_TransactionStarted_InsertOutboxEvent
-- Amaç: transactions tablosuna INSERT yapılınca otomatik outbox_events'e event yaz
-- Durum: DISABLED (Backend zaten OutboxService ile yazıyor)
-- ============================================

-- Eğer varsa önce sil
IF OBJECT_ID('dbo.trg_TransactionStarted_InsertOutboxEvent', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_TransactionStarted_InsertOutboxEvent;
GO

CREATE TRIGGER dbo.trg_TransactionStarted_InsertOutboxEvent
ON dbo.transactions
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Her yeni transaction için outbox event oluştur
    INSERT INTO dbo.outbox_events (
        id,
        aggregate_type,
        aggregate_id,
        type,
        payload,
        created_at
    )
    SELECT 
        NEWID() AS id,
        'Transaction' AS aggregate_type,
        i.id AS aggregate_id,
        'TransactionStarted' AS type,
        -- JSON payload oluştur
        CONCAT(
            '{',
            '"transactionId":"', i.id, '",',
            '"transactionType":"', i.transaction_type, '",',
            '"fromAccountId":"', ISNULL(i.from_account_id, ''), '",',
            '"toAccountId":"', ISNULL(i.to_account_id, ''), '",',
            '"totalAmount":', i.total_amount, ',',
            '"currency":"', i.currency, '",',
            '"status":"', i.status, '",',
            '"referenceNumber":"', i.reference_number, '",',
            '"description":"', REPLACE(ISNULL(i.description, ''), '"', '\"'), '",',
            '"postingDate":"', FORMAT(ISNULL(i.posting_date, CAST(GETDATE() AS DATE)), 'yyyy-MM-dd'), '",',
            '"valueDate":"', FORMAT(ISNULL(i.value_date, CAST(GETDATE() AS DATE)), 'yyyy-MM-dd'), '",',
            '"createdAt":"', FORMAT(i.created_at, 'yyyy-MM-ddTHH:mm:ss.fffffffZ'), '",',
            '"createdBy":"TRIGGER"',
            '}'
        ) AS payload,
        SYSDATETIME() AS created_at
    FROM inserted i;  -- INSERTED tablosu: Yeni eklenen satırlar
    
    PRINT 'Trigger: TransactionStarted event eklendi';
END;
GO

-- ============================================
-- TRIGGER'ı DISABLE ET (Önemli!)
-- Backend OutboxService kullandığı için duplicate olmasın
-- ============================================
DISABLE TRIGGER dbo.trg_TransactionStarted_InsertOutboxEvent ON dbo.transactions;
GO

PRINT '';
PRINT '========================================';
PRINT 'TRIGGER: trg_TransactionStarted_InsertOutboxEvent';
PRINT '========================================';
PRINT '';
PRINT 'Durum: DISABLED ⚠️';
PRINT '';
PRINT 'Neden DISABLED?';
PRINT '  - Backend OutboxService zaten event yazıyor';
PRINT '  - Trigger aktif olsa DUPLICATE event olur';
PRINT '  - Production''da kullanmıyoruz';
PRINT '';
PRINT 'Test İçin Nasıl ENABLE Edilir?';
PRINT '  ENABLE TRIGGER dbo.trg_TransactionStarted_InsertOutboxEvent ON dbo.transactions;';
PRINT '';
PRINT 'Test Sonrası Tekrar DISABLE Et:';
PRINT '  DISABLE TRIGGER dbo.trg_TransactionStarted_InsertOutboxEvent ON dbo.transactions;';
PRINT '';
PRINT 'Trigger Durumunu Kontrol Et:';
PRINT '  SELECT name, is_disabled FROM sys.triggers';
PRINT '  WHERE name = ''trg_TransactionStarted_InsertOutboxEvent'';';
PRINT '';
PRINT '========================================';
PRINT 'Sunumda Nasıl Anlatılır?';
PRINT '========================================';
PRINT '';
PRINT '1. "Transaction TRIGGER da var, her INSERT''te otomatik event yazıyor"';
PRINT '2. "Ama production''da DISABLED çünkü backend SAGA pattern kullanıyor"';
PRINT '3. "TransactionService zaten OrchestrationService ile event yönetiyor"';
PRINT '4. "Duplicate event olmasın diye kapalı tuttuk"';
PRINT '';
PRINT '========================================';
