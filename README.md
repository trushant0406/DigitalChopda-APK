# DigitalChopda.in — Smart Invoicing, GST Billing & Khata Platform

DigitalChopda.in is an enterprise-grade GST billing, quotation, order, sales return, call journal, and credit (khata) management platform built with Node/Express, PostgreSQL, and React (Vite).

---

## 🔐 User Authentication & User Management

The application enforces authentication on all routes.

### Default Admin Credentials
- **Username**: `admin`
- **Password**: `admin123`

### Creating New Users (Direct Database Injection)
To maintain security, user creation is performed directly in the PostgreSQL database instead of an open signup form.

#### Method 1: Command-Line One-Liner (Recommended)
Run this command inside `backend/` to create a new user instantly:

```bash
node -e "
const crypto = require('crypto');
const pg = require('pg');
const pool = new pg.Pool({ connectionString: 'postgres://callintel:callintel@localhost:5432/callintel' });

const username = 'john';        // Set username
const password = 'password123'; // Set password
const name = 'John Doe';        // Set display name

const hash = crypto.pbkdf2Sync(password, 'callintel_salt_2026', 1000, 64, 'sha512').toString('hex');
pool.query('INSERT INTO users (username, password_hash, name, role) VALUES (\$1, \$2, \$3, \$4) RETURNING id, username, name', [username.toLowerCase(), hash, name, 'admin'])
  .then(res => { console.log('✅ User created:', res.rows[0]); pool.end(); })
  .catch(err => { console.error('❌ Error:', err.message); pool.end(); });
"
```

#### Method 2: Manual SQL Query
1. Generate the SHA-512 password hash for your password:
   ```bash
   node -e "console.log(require('crypto').pbkdf2Sync('YOUR_PASSWORD', 'callintel_salt_2026', 1000, 64, 'sha512').toString('hex'))"
   ```
2. Insert the user into PostgreSQL:
   ```sql
   INSERT INTO users (username, password_hash, name, role)
   VALUES ('username_here', 'HASH_FROM_STEP_1', 'Display Name', 'admin');
   ```

---

## 🚀 Setting Up on a New Machine

Follow these step-by-step instructions to run the application on a clean computer.

### Step 1: Install Prerequisites
1. **Node.js (v18 or v20 LTS)**: [https://nodejs.org/](https://nodejs.org/)
2. **PostgreSQL (v15 or v16)**: [https://www.postgresql.org/download/windows/](https://www.postgresql.org/download/windows/)

### Step 2: Database Initialization (`callintel`)
Open **Command Prompt** / **psql** as Administrator:
```bash
psql -U postgres
```

Execute the following commands:
```sql
CREATE USER callintel WITH PASSWORD 'callintel';
CREATE DATABASE callintel OWNER callintel;
GRANT ALL PRIVILEGES ON DATABASE callintel TO callintel;
```

### Step 3: Backend Configuration & Migrations
Navigate to `backend/`:
```bash
cd backend
```

Create a `.env` file (copy from `env.sample`):
```env
DATABASE_URL=postgres://callintel:callintel@localhost:5432/callintel
PORT=4000

VAPID_PUBLIC_KEY=BAzn7LGy6dGVKArSZuhkP_4OTCePSwEMCZ1v1yflIRiywwIp56tfMq45W7OiNrhB4EP_T4TPpm-sA-jZ2kwwhDg
VAPID_PRIVATE_KEY=XkwbRSCeiQtrF9ulQBKgb7verKDMD68N0AeUIlIAceQ
VAPID_SUBJECT=mailto:admin@example.com

REMINDER_POLL_SECONDS=60
```

Install dependencies & execute migrations:
```bash
npm install
npm run migrate
```
*(Optionally restore full database dump: `psql -U callintel -d callintel -f ../callintel.sql`).*

### Step 4: Frontend Setup & Dev Server
Navigate to `frontend/`:
```bash
cd frontend
npm install
npm run dev
```

Open your browser at `http://localhost:5173`.

---

## 🌐 Custom Hostname Binding (`callintel.local`)

Instead of accessing `localhost:5173`, you can bind a custom domain name:

1. Open Notepad as Administrator and edit:
   `C:\Windows\System32\drivers\etc\hosts`
2. Add this line at the bottom:
   ```text
   127.0.0.1   callintel.local
   ```
3. Open your browser and go to:
   `http://callintel.local:5173/`

---

## 📦 System Architecture & Features

- **Dashboard**: Live analytics for due follow-ups, calls, quotations, and pending order collections.
- **Calls**: Call logging, recording attachments, follow-up scheduling, and CSV export.
- **Contacts**: Searchable CRM database with credit limits, GST details, and interactive customer timeline.
- **Orders & Quotations**: Order management, GST calculations, downloadable PDF quotations, and payment actions.
- **Credit (Khata)**: Customer ledger tracking, partial collections, over-limit badges, and auto-settlement upon full collection.
- **WhatsApp Web Integration**: Direct tokenless WhatsApp PDF attachments via QR code linking (`SettingsPage`).
- **Native Android App (`android/`)**: Offline-first Room SQLite mobile client with Biometric security, ESC/POS Bluetooth thermal printing, and GST invoicing.

---

## 📱 Native Android App (Build & Run)

```bash
cd android
./gradlew assembleDebug
```
Built APK output: `android/app/build/outputs/apk/debug/app-debug.apk` (or convenient shortcut in `apk/DigitalChopda-debug.apk`).
