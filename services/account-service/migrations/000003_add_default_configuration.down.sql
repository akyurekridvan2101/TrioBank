-- Rollback: Remove default_configuration column
ALTER TABLE product_definitions 
DROP COLUMN default_configuration;
