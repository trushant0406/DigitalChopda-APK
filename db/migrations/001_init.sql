CREATE TABLE IF NOT EXISTS contacts (
  id             SERIAL PRIMARY KEY,
  phone          TEXT NOT NULL UNIQUE,
  name           TEXT,
  city           TEXT,
  first_seen     TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_contacted TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS calls (
  id            SERIAL PRIMARY KEY,
  contact_id    INTEGER NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  direction     TEXT NOT NULL CHECK (direction IN ('incoming', 'outgoing')),
  outcome       TEXT NOT NULL CHECK (outcome IN ('answered', 'missed', 'busy')),
  duration_sec  INTEGER,
  started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  notes         TEXT,
  recording_url TEXT,
  source        TEXT NOT NULL DEFAULT 'manual' CHECK (source IN ('manual', 'device', 'telephony')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS followups (
  id         SERIAL PRIMARY KEY,
  contact_id INTEGER NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  call_id    INTEGER REFERENCES calls(id) ON DELETE SET NULL,
  due_at     TIMESTAMPTZ NOT NULL,
  reason     TEXT,
  status     TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'done', 'cancelled')),
  channel    TEXT NOT NULL DEFAULT 'push' CHECK (channel IN ('push', 'whatsapp')),
  sent_at    TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_calls_contact_id ON calls(contact_id);
CREATE INDEX IF NOT EXISTS idx_followups_contact_id ON followups(contact_id);
CREATE INDEX IF NOT EXISTS idx_followups_due_at ON followups(due_at) WHERE status = 'pending';
