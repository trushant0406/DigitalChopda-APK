# Call Intelligence

Manual-first call journal. Answers: who called, who you answered, who's waiting
on a follow-up. See `call-intelligence-handoff.md` (Desktop) for full project context
and build phases. Phase 4 (virtual number / telephony) is intentionally not built yet —
still to be brainstormed separately.

## Stack

- Backend: Node/Express + Postgres (`backend/`)
- Frontend: React + Vite (`frontend/`)
- DB: Postgres via `docker-compose.yml`, schema in `db/migrations/`

## Run it

```bash
# 1. Database
docker-compose up -d db          # or: docker exec -i <container> psql -U callintel -d callintel < db/migrations/00N_*.sql
                                  #   to apply a new migration against an already-initialized volume

# 2. Backend
cd backend
cp env.sample .env               # only needed once; includes generated VAPID keys for push
npm install
npm run dev                       # http://localhost:4000

# 3. Frontend (separate terminal)
cd frontend
npm install
npm run dev                       # http://localhost:5173
```

## What's here

### UI (enterprise shell — sidebar navigation)

- **Dashboard** — stat cards (due today, overdue, never followed up, repeat-no-connect,
  calls today, total contacts) + the four intelligence tables.
- **Calls** — log-call form (with optional recording upload and follow-up scheduling)
  and a full call log with CSV export.
- **Contacts** — searchable list; click a row to open a detail drawer with the full
  call + follow-up timeline and an "add follow-up" form.
- **Follow-ups** — every follow-up, filterable by status and channel, with
  done/cancel actions.
- **Settings** — enable owner push notifications on this device; WhatsApp
  configuration status (dry-run until Cloud API credentials are set).

### Phase 2 — follow-up engine (built)

- A scheduler (`backend/src/scheduler.js`) polls every `REMINDER_POLL_SECONDS`
  (default 60s) for follow-ups past due and not yet reminded.
- **Owner reminders** (`channel: 'push'`) → Web Push via VAPID (free, no signup).
  Keys are pre-generated in `env.sample`; register your device from Settings.
- **Customer messages** (`channel: 'whatsapp'`) → WhatsApp Cloud API template send.
  With no `WHATSAPP_TOKEN`/`WHATSAPP_PHONE_NUMBER_ID` set, it logs a dry-run line
  to the backend console instead of failing — fully demoable with zero signup.
- Rescheduling a follow-up's due date clears its `sent_at` so the scheduler fires
  again for the new date.

### Phase 3 — reduce the typing

Skipped for now per decision: the Android companion app (reads `CallLog.Calls`,
background sync) is a separate native codebase and out of scope for this pass.
Nothing here blocks adding it later — `calls.source` already distinguishes
`'manual' | 'device' | 'telephony'`.

## Next

- Phase 4 (virtual number / telephony) — needs a separate design discussion
  (KooKoo vs Exotel, ingestion flow, audio fallback) before building.
