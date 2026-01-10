-- Migration Dosyası: 000001
-- Açıklama: Outbox tablosu ve CDC kurulumu
-- Not: Bağlantı stringinde mutlaka database=card_db olmalı!

-- ============================================
-- 1. Outbox Events Tablosu Oluştur
-- Event'leri Kafka'ya göndermeden önce buraya yazıyoruz
-- Debezium CDC ile bu tabloyu okuyup Kafka'ya aktaracak
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'outbox_events' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.outbox_events (
        id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
        aggregate_type NVARCHAR(255) NOT NULL,  -- Hangi entity (Card vs.)
        aggregate_id NVARCHAR(255) NOT NULL,    -- Entity'nin ID'si
        type NVARCHAR(255) NOT NULL,            -- Event tipi (CardCreated, CardBlocked vs.)
        payload NVARCHAR(MAX) NOT NULL,         -- Event verisi (JSON formatında)
        created_at DATETIME2 DEFAULT SYSDATETIME(),
        
        INDEX IX_outbox_events_created_at (created_at),
        INDEX IX_outbox_events_aggregate_type (aggregate_type)
    );
END;

-- ============================================
-- 2. Veritabanı Seviyesinde CDC'yi Aktif Et
-- CDC = Change Data Capture (Değişiklik yakalama)
-- Transaction log'u okuyarak değişiklikleri tespit eder
-- ============================================
DECLARE @cdc_enabled BIT;
SELECT @cdc_enabled = is_cdc_enabled FROM sys.databases WHERE name = 'card_db';

IF @cdc_enabled = 0
BEGIN
    EXEC sys.sp_cdc_enable_db;  -- Database için CDC'yi aç
END;

-- ============================================
-- 3. outbox_events Tablosu İçin CDC Aktif Et
-- Sadece bu tablodaki değişiklikleri takip edeceğiz
-- Debezium bu tabloyu izleyip Kafka'ya event gönderiyor
-- ============================================
IF NOT EXISTS (
    SELECT * 
    FROM sys.tables t
    JOIN sys.schemas s ON t.schema_id = s.schema_id
    WHERE s.name = 'dbo' 
      AND t.name = 'outbox_events' 
      AND t.is_tracked_by_cdc = 1  -- CDC aktif mi kontrol et
)
BEGIN
    EXEC sys.sp_cdc_enable_table
        @source_schema = N'dbo',
        @source_name   = N'outbox_events',
        @role_name     = NULL,  -- Herkes okuyabilir
        @supports_net_changes = 0;  -- Tüm değişiklikleri kaydet
END;
