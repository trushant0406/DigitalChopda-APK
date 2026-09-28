-- Migration 023: Scope products table uniqueness by tenant (business_id, name)

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_name_key;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_business_name_key;
ALTER TABLE products ADD CONSTRAINT products_business_name_key UNIQUE (business_id, name);
