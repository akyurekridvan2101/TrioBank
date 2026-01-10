USE account_db;

-- 2. Seed Products
IF NOT EXISTS (SELECT * FROM product_definitions WHERE code = 'SAVINGS_USD')
BEGIN
    INSERT INTO product_definitions (code, name, is_active, category, features, created_at)
    VALUES ('SAVINGS_USD', 'Vadeli Dolar Hesabı', 1, 'SAVINGS', '{"interestRate": 3.5, "minBalance": 100}', SYSDATETIMEOFFSET());
END

IF NOT EXISTS (SELECT * FROM product_definitions WHERE code = 'GOLD_ACC')
BEGIN
    INSERT INTO product_definitions (code, name, is_active, category, features, created_at)
    VALUES ('GOLD_ACC', 'Altın Hesabı', 1, 'SAVINGS', '{"purity": 0.995}', SYSDATETIMEOFFSET());
END

IF NOT EXISTS (SELECT * FROM product_definitions WHERE code = 'LOAN_PERS')
BEGIN
    INSERT INTO product_definitions (code, name, is_active, category, features, created_at)
    VALUES ('LOAN_PERS', 'İhtiyaç Kredisi', 1, 'LOAN', '{"maxAmount": 100000}', SYSDATETIMEOFFSET());
END

IF NOT EXISTS (SELECT * FROM product_definitions WHERE code = 'INACTIVE_PROD')
BEGIN
    INSERT INTO product_definitions (code, name, is_active, category, features, created_at)
    VALUES ('INACTIVE_PROD', 'Eski Ürün', 0, 'CHECKING', '{}', SYSDATETIMEOFFSET());
END

-- 3. Seed Accounts
IF NOT EXISTS (SELECT * FROM accounts WHERE customer_id = 'user_001' AND product_code = 'RETAIL_TRY')
BEGIN
    INSERT INTO accounts (id, customer_id, account_number, product_code, status, currency, configurations, created_at, updated_at)
    VALUES (NEWID(), 'user_001', 'TR112101000000000000000001', 'RETAIL_TRY', 'ACTIVE', 'TRY', '{}', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET());
END

IF NOT EXISTS (SELECT * FROM accounts WHERE customer_id = 'user_001' AND product_code = 'SAVINGS_USD')
BEGIN
    INSERT INTO accounts (id, customer_id, account_number, product_code, status, currency, configurations, created_at, updated_at)
    VALUES (NEWID(), 'user_001', 'TR222101000000000000000002', 'SAVINGS_USD', 'CLOSED', 'USD', '{}', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET());
END

IF NOT EXISTS (SELECT * FROM accounts WHERE customer_id = 'user_002')
BEGIN
    INSERT INTO accounts (id, customer_id, account_number, product_code, status, currency, configurations, created_at, updated_at)
    VALUES (NEWID(), 'user_002', 'TR332101000000000000000003', 'GOLD_ACC', 'ACTIVE', 'XAU', '{}', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET());
END
