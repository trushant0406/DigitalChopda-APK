# DigitalChopda Android Workspace Instructions (`GEMINI.md`)

This file is automatically loaded upon starting a session in the `DigitalChopdaAndroid` workspace. It establishes the foundational architectural context, Room SQLite schema rules, Maestro E2E test flows, and mandatory implementation & living documentation rules.

---

## 🏛️ Core Workspace Context & Documentation Hub

- **Master Living Specification**: Read [APP_CONTEXT.md](./APP_CONTEXT.md) for complete component coordinates, UI flows, and entity architectures.
- **Project Overview & Setup**: Read [README.md](./README.md) for build commands and device connections.
- **Tech Stack Baseline**:
  - Native Android (Kotlin 2.0.21, Jetpack Compose, Material 3)
  - Target SDK: 35 (Android 15), Min SDK: 26
  - Local Database: Room SQLite v14 (`ChopdaDatabase.kt`)
  - Testing & Verification: Maestro E2E CLI (`.maestro/`), JUnit 4/5, Roborazzi / Robolectric
  - Hardware Integrations: Bluetooth ESC/POS Thermal Printing (58mm/80mm), CameraX Barcode Scanner, BiometricPrompt, ZXing UPI QR

---

## 📜 MANDATORY IMPLEMENTATION & LIVING DOCUMENTATION RULES

### 1. The Twin-Pillar Definition of Done (Reality Test + Living Documentation)
Whenever planning, coding, or modifying any module or feature in DigitalChopda Android, work is **NEVER COMPLETE** without satisfying both pillars:

1. **Pillar 1: Empirical Reality Verification**:
   - Write code directly in live strata.
   - Run live build verification: `./gradlew assembleDebug` or `./gradlew testDebugUnitTest`.
   - Verify UI flows via Maestro E2E tests (`./run_e2e_tests.sh` or `maestro test .maestro/*.yaml`) on the connected device.
   - Verify Room migrations: Ensure `fallbackToDestructiveMigration()` is **NEVER** used in production; write explicit `Migration(from, to)`.
   - **Zero Simulations**: Never run theoretical loops, mocks, or sandbox stubs. Pure live execution only.

2. **Pillar 2: Living Documentation Update (Token-Preservation Rule)**:
   - Update [APP_CONTEXT.md](./APP_CONTEXT.md) immediately whenever any entity column, UI screen, composable, use case, or API sync contract changes.
   - **Why?**: `APP_CONTEXT.md` is the high-density long-term memory of this repository. Keeping it 100% synchronized prevents future AI agents from burning thousands of tokens recursively scanning 100+ Kotlin files across `presentation/`, `domain/`, and `data/` layers.

### 2. Deep Pre-Analysis Before Modification
- Before touching Kotlin code or Compose layouts, perform targeted pre-analysis of existing composables, ViewModels, Room DAOs, and navigation routes.
- Once known, make all technical and architectural decisions autonomously without hesitations or workflow interruptions.

### 3. Multi-Firm Boundary Isolation
- **Firm Scope Mandate**: All database queries touching business-owned entities (`companies`, `customers`, `invoices`, `payments`, `products`, `purchases`, `quotations`) **MUST** filter by `companyId` (active business).
- Never allow cross-firm data leakage.

### 4. Cloud Sync & Backend Alignment
- Must remain 100% compatible with the DigitalChopda Web/Backend REST API (`/api/v1/sync` delta engine).
- Every offline mutation must generate and persist a UUID `idempotencyKey` to guarantee zero duplicate records upon reconnection.
