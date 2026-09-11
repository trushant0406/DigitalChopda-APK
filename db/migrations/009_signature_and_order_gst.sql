-- Authorised-signature image for the quotation PDF, stored as a data URL —
-- small, single image, no need for separate file storage.
ALTER TABLE business_profile ADD COLUMN IF NOT EXISTS signature_image TEXT;

-- Orders can now show a GST-inclusive total too, using the same per-line rate
-- concept quotations already use. Nullable: only set when the line item came
-- from a catalog product that carries a gst_rate.
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS gst_rate NUMERIC;
