-- Rollback: Drop performance indexes

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cards_type_status')
    DROP INDEX IX_cards_type_status ON dbo.cards;

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_cards_expiring_soon')
    DROP INDEX IX_cards_expiring_soon ON dbo.cards;

PRINT 'Card Service performance indexleri kaldırıldı';
