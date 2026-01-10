-- Migration Dosyası: 000008
-- Açıklama: TRIGGER - Account Creation Event (DISABLED)
-- UYARI: Bu trigger başlangıçta DISABLED! Backend OutboxService kullandığı için duplicate event yaratmasın.

-- ============================================
-- TRIGGER: trg_AccountCreated_InsertOutboxEvent
-- Amaç: accounts tablosuna INSERT yapılınca otomatik outbox_events'e event yaz
-- Durum: DISABLED (Backend zaten OutboxService ile yazıyor)
-- ============================================

-- Eğer varsa önce sil
IF OBJECT_ID('dbo.trg_AccountCreated_InsertOutboxEvent', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_AccountCreated_InsertOutboxEvent;
GO

CREATE TRIGGER dbo.trg_AccountCreated_InsertOutboxEvent
ON dbo.accounts
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Her yeni account için outbox event oluştur
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
        'Account' AS aggregate_type,
        i.id AS aggregate_id,
        'AccountCreated' AS type,
        -- JSON payload oluştur
        CONCAT(
            '{',
            '"accountId":"', i.id, '",',
            '"accountNumber":"', i.account_number, '",',
            '"customerId":"', i.customer_id, '",',
            '"currency":"', i.currency, '",',
            '"status":"', i.status, '",',
            '"productCode":"', i.product_code, '",',
            '"balance":', i.balance, ',',
            '"createdAt":"', FORMAT(i.created_at, 'yyyy-MM-ddTHH:mm:ss.fffffffZ'), '",',
            '"createdBy":"TRIGGER"',
            '}'
        ) AS payload,
        SYSDATETIME() AS created_at
    FROM inserted i;  -- INSERTED tablosu: Yeni eklenen satırlar
    
    PRINT 'Trigger: AccountCreated event eklendi';
END;
GO

-- ============================================
-- TRIGGER'ı DISABLE ET (Önemli!)
-- Backend OutboxService kullandığı için duplicate olmasın
-- ============================================
DISABLE TRIGGER dbo.trg_AccountCreated_InsertOutboxEvent ON dbo.accounts;
GO

PRINT '';
PRINT '========================================';
PRINT 'TRIGGER: trg_AccountCreated_InsertOutboxEvent';
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
PRINT '  ENABLE TRIGGER dbo.trg_AccountCreated_InsertOutboxEvent ON dbo.accounts;';
PRINT '';
PRINT 'Test Sonrası Tekrar DISABLE Et:';
PRINT '  DISABLE TRIGGER dbo.trg_AccountCreated_InsertOutboxEvent ON dbo.accounts;';
PRINT '';
PRINT 'Trigger Durumunu Kontrol Et:';
PRINT '  SELECT name, is_disabled FROM sys.triggers';
PRINT '  WHERE name = ''trg_AccountCreated_InsertOutboxEvent'';';
PRINT '';
PRINT '========================================';
PRINT 'Sunumda Nasıl Anlatılır?';
PRINT '========================================';
PRINT '';
PRINT '1. "Trigger oluşturduk, her account INSERT''te otomatik event yazıyor"';
PRINT '2. "Ama production''da DISABLED çünkü backend OutboxService kullanıyor"';
PRINT '3. "Duplicate event olmasın diye kapalı tuttuk"';
PRINT '4. "Test için ENABLE edip gösterebiliriz"';
PRINT '';
PRINT '========================================';
