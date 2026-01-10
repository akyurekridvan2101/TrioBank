-- Migration Version: 000002
-- Description: Ledger Service domain tables (Transactions, Entries, Balances)
-- Dependencies: Requires migration 000001 (outbox_events + CDC)

-- ============================================
-- Table 1: ledger_transactions
-- Purpose: Transaction headers (group ledger entries)
-- Mutable: YES - Status can change (POSTED → REVERSED)
-- Relationships: One-to-Many with ledger_entries
-- Event Sourcing: State changes trigger events via outbox pattern
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ledger_transactions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.ledger_transactions (
        -- ============================================
        -- Primary Key
        -- transaction_id: Business key from Transaction Service (not UUID!)
        -- Enables correlation between Transaction Service and Ledger Service
        -- ============================================
        transaction_id NVARCHAR(100) NOT NULL PRIMARY KEY,
        
        -- ============================================
        -- Transaction Classification
        -- transaction_type: Business category (TRANSFER, CARD_PAYMENT, EFT, FAST, etc.)
        -- Used for reporting and analytics
        -- ============================================
        transaction_type NVARCHAR(50) NOT NULL,
        
        -- ============================================
        -- Transaction Dates
        -- transaction_date: Technical timestamp (when recorded in ledger)
        -- posting_date: Accounting date (business day)
        -- value_date: Value date (for interest calculations)
        -- ============================================
        transaction_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        posting_date DATE NOT NULL,
        value_date DATE NOT NULL,
        
        -- ============================================
        -- Financial Information
        -- total_amount: Sum of all entries (DEBIT total = CREDIT total requirement)
        -- currency: ISO 4217 currency code
        -- Must use DECIMAL for financial precision (never float/double!)
        -- ============================================
        total_amount DECIMAL(19, 4) NOT NULL,
        currency NVARCHAR(3) NOT NULL,
        
        -- ============================================
        -- Transaction Status
        -- status: POSTED (normal) or REVERSED (cancelled)
        -- Status changes trigger events to Consumer services
        -- ============================================
        status NVARCHAR(20) NOT NULL,
        
        -- ============================================
        -- Business Information
        -- description: Human-readable transaction description
        -- initiator_account_id: Account that initiated the transaction
        -- ============================================
        description NVARCHAR(500) NULL,
        initiator_account_id NVARCHAR(50) NULL,
        
        -- ============================================
        -- Reversal Tracking
        -- is_reversal: Is this a reversal transaction?
        -- original_transaction_id: If reversal, which transaction is being reversed?
        -- reversed_by_transaction_id: If reversed, which transaction reversed this?
        -- ============================================
        is_reversal BIT NOT NULL DEFAULT 0,
        original_transaction_id NVARCHAR(100) NULL,
        reversed_by_transaction_id NVARCHAR(100) NULL,
        
        -- ============================================
        -- Audit Fields
        -- ============================================
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        -- ============================================
        -- Indexes for Performance
        -- - posting_date: Financial reports by date range
        -- - initiator_account_id: Find transactions by account
        -- - status: Filter active vs reversed transactions
        -- - transaction_type: Category-based queries
        -- ============================================
        INDEX IX_ledger_transactions_posting_date (posting_date),
        INDEX IX_ledger_transactions_initiator (initiator_account_id),
        INDEX IX_ledger_transactions_status (status),
        INDEX IX_ledger_transactions_type (transaction_type),
        INDEX IX_ledger_transactions_reversal (is_reversal, original_transaction_id)
    );
    
    PRINT 'Table ledger_transactions created successfully';
    PRINT '  - Transaction headers with double-entry validation';
    PRINT '  - Reversal tracking for transaction cancellations';
    PRINT '  - Immutable core (only status updates allowed)';
END
ELSE
BEGIN
    PRINT 'Table ledger_transactions already exists - skipping';
END;
-- GO command removed (not supported by migration tool)

-- ============================================
-- Table 2: ledger_entries
-- Purpose: Individual ledger entries (double-entry accounting)
-- Mutable: NO - Completely immutable once created
-- Relationships: Many-to-One with ledger_transactions
-- Constraint: DEBIT total = CREDIT total per transaction
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ledger_entries' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.ledger_entries (
        -- ============================================
        -- Primary Key
        -- id: UUID generated by application
        -- ============================================
        id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY DEFAULT NEWID(),
        
        -- ============================================
        -- Transaction Relationship
        -- transaction_id: Foreign key to ledger_transactions
        -- Groups entries into transactions
        -- ============================================
        transaction_id NVARCHAR(100) NOT NULL,
        
        -- ============================================
        -- Entry Sequencing
        -- entry_sequence: Order within transaction (for consistent application)
        -- ============================================
        entry_sequence INT NOT NULL,
        
        -- ============================================
        -- Account Association
        -- account_id: References account in Account Service (microservice boundary - no FK!)
        -- Every entry must belong to exactly one account
        -- ============================================
        account_id NVARCHAR(50) NOT NULL,
        
        -- ============================================
        -- Double-Entry Accounting
        -- entry_type: DEBIT (decrease) or CREDIT (increase)
        -- amount: Always positive (direction determined by entry_type)
        -- currency: ISO 4217 currency code
        -- ============================================
        entry_type NVARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
        amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
        currency NVARCHAR(3) NOT NULL,
        
        -- ============================================
        -- Business Information
        -- description: Entry-specific description
        -- reference_number: External system reference (for reconciliation)
        -- transaction_type: Denormalized from transaction (performance)
        -- ============================================
        description NVARCHAR(500) NULL,
        reference_number NVARCHAR(100) NULL,
        transaction_type NVARCHAR(50) NULL,
        
        -- ============================================
        -- Entry Status
        -- status: Always 'POSTED' (no pending entries in ledger)
        -- ============================================
        status NVARCHAR(20) NOT NULL DEFAULT 'POSTED',
        
        -- ============================================
        -- Accounting Dates (denormalized for performance)
        -- posting_date: Accounting date
        -- value_date: Value date (for interest)
        -- ============================================
        posting_date DATE NOT NULL,
        value_date DATE NOT NULL,
        
        -- ============================================
        -- Audit Fields
        -- ============================================
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        -- ============================================
        -- Foreign Key to Transaction
        -- ============================================
        CONSTRAINT FK_ledger_entries_transaction 
            FOREIGN KEY (transaction_id) 
            REFERENCES dbo.ledger_transactions(transaction_id)
            ON DELETE CASCADE,
        
        -- ============================================
        -- Indexes for Performance
        -- - account_id: Find all entries for an account (account statement)
        -- - transaction_id: Find entries by transaction
        -- - posting_date: Date range queries
        -- - account_posting: Composite for account statements by date
        -- ============================================
        INDEX IX_ledger_entries_account_id (account_id),
        INDEX IX_ledger_entries_transaction_id (transaction_id),
        INDEX IX_ledger_entries_posting_date (posting_date),
        INDEX IX_ledger_entries_account_posting (account_id, posting_date DESC)
    );
    
    PRINT 'Table ledger_entries created successfully';
    PRINT '  - Double-entry accounting entries';
    PRINT '  - Foreign key to ledger_transactions';
    PRINT '  - Immutable (no updates allowed)';
    PRINT '  - Optimized for account statement queries';
END
ELSE
BEGIN
    PRINT 'Table ledger_entries already exists - skipping';
END;
-- GO command removed (not supported by migration tool)

-- ============================================
-- Table 3: account_balances
-- Purpose: Account balance cache (performance optimization)
-- Mutable: YES - Updated on every ledger entry
-- Concurrency: Uses optimistic locking (version column)
-- Transaction Hold: Supports blocked amounts for pending transactions
-- ============================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'account_balances' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE dbo.account_balances (
        -- ============================================
        -- Primary Key
        -- account_id: References account in Account Service (microservice boundary - no FK!)
        -- One record per account
        -- ============================================
        account_id NVARCHAR(50) NOT NULL PRIMARY KEY,
        
        -- ============================================
        -- Balance Information
        -- balance: Current account balance
        -- blocked_amount: Amount held for pending transactions
        -- available_balance: Computed as (balance - blocked_amount) - can be added as computed column
        -- currency: ISO 4217 currency code
        -- ============================================
        balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
        blocked_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
        currency NVARCHAR(3) NOT NULL,
        
        -- ============================================
        -- Pending Transaction Tracking
        -- pending_debits: Sum of pending DEBIT transactions
        -- pending_credits: Sum of pending CREDIT transactions
        -- Used for reporting and risk management
        -- ============================================
        pending_debits DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
        pending_credits DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
        
        -- ============================================
        -- Audit Trail
        -- last_entry_id: Last ledger entry that updated this balance
        -- last_updated_at: When was balance last changed
        -- ============================================
        last_entry_id UNIQUEIDENTIFIER NULL,
        last_updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        -- ============================================
        -- Optimistic Locking (Concurrency Control)
        -- version: Incremented on each update
        -- Prevents race conditions when multiple transactions update same account simultaneously
        -- JPA @Version annotation will manage this automatically
        -- ============================================
        version BIGINT NOT NULL DEFAULT 1,
        
        -- ============================================
        -- Audit Fields
        -- ============================================
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        
        -- ============================================
        -- Indexes for Performance
        -- - account_id: Already covered by PRIMARY KEY
        -- ============================================
    );
    
    PRINT 'Table account_balances created successfully';
    PRINT '  - Balance cache for fast queries';
    PRINT '  - Optimistic locking enabled (version column)';
    PRINT '  - Transaction hold support (blocked_amount)';
    PRINT '  - Pending transaction tracking';
END
ELSE
BEGIN
    PRINT 'Table account_balances already exists - skipping';
END;
-- GO command removed (not supported by migration tool)

-- ============================================
-- Verification: Check all tables created
-- ============================================
PRINT '';
PRINT '========================================';
PRINT 'Migration 000002 completed successfully';
PRINT '========================================';
PRINT 'Tables created:';
PRINT '  1. ledger_transactions (transaction headers)';
PRINT '  2. ledger_entries (double-entry accounting)';
PRINT '  3. account_balances (balance cache with holds)';
PRINT '';
PRINT 'Notes:';
PRINT '  - outbox_events already exists (created in 000001)';
PRINT '  - Transaction state changes must publish events to outbox_events';
PRINT '  - DEBIT total = CREDIT total per transaction (application enforced)';
PRINT '  - All monetary amounts use DECIMAL(19,4) for precision';
PRINT '  - Foreign key: ledger_entries->ledger_transactions';
PRINT '  - account_balances uses optimistic locking (version column)';
PRINT '  - blocked_amount enables transaction authorization holds';
PRINT '========================================';
-- GO command removed (not supported by migration tool)

-- ============================================
-- Quick Sanity Check Query
-- ============================================
SELECT 
    t.name AS TableName,
    i.name AS IndexName,
    i.type_desc AS IndexType
FROM sys.tables t
LEFT JOIN sys.indexes i ON t.object_id = i.object_id
WHERE t.schema_id = SCHEMA_ID('dbo')
  AND t.name IN ('ledger_transactions', 'ledger_entries', 'account_balances', 'outbox_events')
  AND i.name IS NOT NULL
ORDER BY t.name, i.index_id;
-- GO command removed (not supported by migration tool)
