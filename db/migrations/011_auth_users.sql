-- Users table for admin authentication
CREATE TABLE IF NOT EXISTS users (
  id            SERIAL PRIMARY KEY,
  username      TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name          TEXT NOT NULL,
  role          TEXT NOT NULL DEFAULT 'admin',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Default admin user (username: admin, password: admin123)
INSERT INTO users (username, password_hash, name, role)
VALUES (
  'admin',
  '05e9fd2690322eca9166e3550322f385dc162cd99f8a79d6626a56eaa3e58c1ea0a2cd5ceaba31882a3a8522fa2ee298c054746f1432a23a00e9653c333da91d',
  'Admin User',
  'admin'
)
ON CONFLICT (username) DO NOTHING;
