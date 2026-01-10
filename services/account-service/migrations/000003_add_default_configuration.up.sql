-- Migration: Add default_configuration column to product_definitions
-- Version: 000003
-- Date: 2025-12-19
-- Description: Adds default user configuration template for products

ALTER TABLE product_definitions 
ADD default_configuration NVARCHAR(MAX);
