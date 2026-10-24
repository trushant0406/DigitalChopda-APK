# 📱 DIGITALCHOPDA ANDROID — SOVEREIGN NATIVE BILLING & KHATA PLATFORM

> **Enterprise GST Invoicing, Cashbook (Rokad), Party Ledger, Products & Offline-First POS**  
> *Target SDK: 35 (Android 15) | Kotlin: 2.0.21 | Jetpack Compose | Room SQLite v14*

---

## 1. ⚙️ App Architecture & Stack

DigitalChopda Android is a 100% sovereign native Android application built with modern Jetpack Compose and Room SQLite, architected for high-velocity merchants, multi-firm khata, and instant offline POS billing.

* **Package**: `com.digitalchopda.app`
* **Entry Activity**: `com.digitalchopda.app.MainActivity` (Single-Activity Compose)
* **Application Class**: `com.digitalchopda.app.DigitalChopdaApplication`
* **Local Database**: Room SQLite v14 (`ChopdaDatabase.kt`)
* **Design System**: Midnight Surface & Crisp Slate Canvas (`#F8FAFC`)
* **Security**: Keystore AES-256 Vault (`SecurityVault.kt`), BiometricPrompt hardware auth

---

## 2. 🧭 UI Navigation & 5-Tab Floating Capsule Dock

Persistent bottom navigation via `FloatingCapsuleDock`:

* **Tab 0: Chopda / Dashboard (`Screen.Dashboard`)**: Dual-perspective Rokad cash drawer, Web-aligned 6-metric orders overview, live recent invoices feed, daily statement PDF download.
* **Tab 1: Party / Khata Ledger (`Screen.Party`)**: Customer directory, Udhar/Jama balances, 360° party passbook, credit limits.
* **Tab 2: Billing Hub (`Screen.Billing`)**: Quick POS billing, GST tax invoices, quotation estimates, sales return credit notes.
* **Tab 3: Products Catalog (`Screen.Products`)**: Barcode inventory, HSN codes, GST tax slabs (0%, 5%, 12%, 18%, 28%), stock adjustments.
* **Tab 4: Settings & GST Hub (`Screen.Settings` / `Screen.GstHub`)**: Business switcher, UPI VPA configuration, language switching (English, Hindi, Gujarati), biometric lock.

---

## 3. 🛠️ Build & Verification Commands

### Run Unit Tests
```bash
cd android
./gradlew testDebugUnitTest
```

### Build Debug APK
```bash
cd android
./gradlew assembleDebug
```

### Build Minified Release APK
```bash
cd android
./gradlew assembleRelease
```

### Run Maestro E2E Flows
```bash
cd android
./run_e2e_tests.sh
# Or run individual flow
maestro test .maestro/01_login_flow.yaml
```

---

## 4. 🔒 Release Signing & Environment

Release signing is configured in `app/build.gradle.kts` to pull from system environment variables with a clean fallback to the debug keystore for development:

* `KEYSTORE_PATH`: Path to production `.keystore` / `.jks` file
* `KEYSTORE_PASSWORD`: Keystore password
* `KEY_ALIAS`: Key alias name
* `KEY_PASSWORD`: Key password
