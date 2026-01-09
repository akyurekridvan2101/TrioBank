-- Migration Rollback Version: 000001
-- Description: Rollback initial schema and CDC setup
-- WARNING: This will delete all data in outbox_events table!

-- ============================================
-- 1. Disable CDC for outbox_events Table
-- ============================================
IF EXISTS (
    SELECT * 
    FROM sys.tables t
    JOIN sys.schemas s ON t.schema_id = s.schema_id
    WHERE s.name = 'dbo' 
      AND t.name = 'outbox_events' 
      AND t.is_tracked_by_cdc = 1
)
BEGIN
    EXEC sys.sp_cdc_disable_table 
        @source_schema = N'dbo',
        @source_name = N'outbox_events',
        @capture_instance = N'dbo_outbox_events';
END;

-- ============================================
-- 2. Disable CDC at Database Level
-- ============================================
DECLARE @cdc_db_enabled BIT;
SELECT @cdc_db_enabled = is_cdc_enabled FROM sys.databases WHERE name = DB_NAME();

IF @cdc_db_enabled = 1
BEGIN
    EXEC sys.sp_cdc_disable_db;
END;

-- ============================================
-- 3. Drop outbox_events Table
-- ============================================
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'outbox_events' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    DROP TABLE dbo.outbox_events;
END;
