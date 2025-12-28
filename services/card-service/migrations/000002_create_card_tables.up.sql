-- Migration Version: 000002
-- Description: Create Card Service domain tables
-- Tables: cards (main entity with Single Table Inheritance)

-- ============================================
-- 1. Create Cards Table
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'cards' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.cards (
        id NVARCHAR(50) PRIMARY KEY,
        card_type NVARCHAR(20) NOT NULL,
        
        -- Common card fields
        card_number NVARCHAR(19) NOT NULL,
        masked_number NVARCHAR(19) NOT NULL,
        cvv NVARCHAR(4) NOT NULL,
        cardholder_name NVARCHAR(100) NOT NULL,
        expiry_month INT NOT NULL,
        expiry_year INT NOT NULL,
        card_brand NVARCHAR(20) NOT NULL,
        status NVARCHAR(20) NOT NULL,
        account_id NVARCHAR(50) NOT NULL,
        pin_hash NVARCHAR(255),
        
        -- Debit Card specific fields (nullable for other types)
        daily_withdrawal_limit DECIMAL(15,2),
        atm_enabled BIT,
        
        -- Credit Card specific fields (nullable for other types)
        credit_limit DECIMAL(15,2),
        available_credit DECIMAL(15,2),
        interest_rate DECIMAL(5,2),
        statement_day INT,
        payment_due_day INT,
        
        -- Virtual Card specific fields (nullable for other types)
        online_only BIT,
        single_use_expires_at DATETIMEOFFSET(6),
        usage_restriction NVARCHAR(50),
        
        -- Audit fields
        created_at DATETIMEOFFSET(6) DEFAULT SYSDATETIME() NOT NULL,
        blocked_at DATETIMEOFFSET(6),
        block_reason NVARCHAR(255),
        
        -- Indexes inline
        INDEX IX_cards_card_number (card_number),
        INDEX IX_cards_account_id (account_id),
        INDEX IX_cards_card_type (card_type),
        INDEX IX_cards_status (status),
        INDEX IX_cards_expiry (expiry_year, expiry_month)
    );
    
    -- Unique constraint for card number
    ALTER TABLE dbo.cards ADD CONSTRAINT UQ_cards_card_number UNIQUE (card_number);
END;
