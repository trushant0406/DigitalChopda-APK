CREATE TABLE IF NOT EXISTS products (
  id             SERIAL PRIMARY KEY,
  name           TEXT NOT NULL UNIQUE,
  default_price  NUMERIC NOT NULL CHECK (default_price >= 0),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
