-- Migration 020: Add place_of_supply, tax_mode, and round_off to orders table

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS place_of_supply TEXT,
  ADD COLUMN IF NOT EXISTS tax_mode TEXT DEFAULT 'split',
  ADD COLUMN IF NOT EXISTS round_off NUMERIC DEFAULT 0;

-- Backfill existing orders based on contact state vs business profile state ('Gujarat')
UPDATE orders o
SET place_of_supply = COALESCE(c.state, 'Gujarat'),
    tax_mode = CASE
      WHEN c.state IS NOT NULL AND LOWER(TRIM(c.state)) != 'gujarat' THEN 'igst'
      ELSE 'split'
    END
FROM contacts c
WHERE o.contact_id = c.id AND o.place_of_supply IS NULL;
