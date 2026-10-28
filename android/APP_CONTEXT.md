# 📖 DIGITALCHOPDA ANDROID — LIVING APP CONTEXT & SPECIFICATION
> **Single Source of Truth for Autonomous AI Agents & Developers**
> *Last Updated: October 2026 | Version: 1.0.1 (code 2) | Target SDK: 35 | Kotlin: 2.0.21*

---

## 1. ⚙️ App Coordinates & Hardware Environment
* **Package Name**: `com.digitalchopda.app`
* **Application Class**: `com.digitalchopda.app.DigitalChopdaApplication`
* **Entry Activity**: `com.digitalchopda.app.MainActivity` (Single-Activity Jetpack Compose)
* **Primary ADB Device**: `100.116.214.73:35241` (Google Pixel 8 Pro - `husky`)
* **Termux Device SSH Node**: `pixel` (`100.116.214.73:8022`, User: `u0_a392`)
* **Compute Worker SSH Node**: `mac` (`100.88.50.47`, macOS 8-Core 16GB RAM)
* **Test Credentials**:
  * Username: `admin` | Password: `admin123`
  * Active Company ID: `1` (`ABC E-COMMERCE`) / ID: `2` (`Sharma Traders`)

---

## 2. 📱 UI Navigation & Floating Capsule Dock

The application uses a 5-tab persistent bottom dock (`FloatingCapsuleDock` in `MainContainerScreen.kt`):

```
┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│ 0: Chopra    │ 1: Party     │ 2: Billing   │ 3: Products  │ 4: Settings  │
│ [14%, 96%]   │ [31%, 96%]   │ [48%, 96%]   │ [67%, 96%]   │ [86%, 96%]   │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

| Tab Index | Route (`Screen.*`) | Screen Title / Labels | Screen Bounds & Coordinates |
| :--- | :--- | :--- | :--- |
| **0** | `Screen.Dashboard` | `चोपड़ा` (Chopda / Dashboard) | `point: "14%,96%"` (Center: `185, 2864`) |
| **1** | `Screen.Party` | `पार्टी` (Parties / Khata Ledger) | `point: "31%,96%"` (Center: `416, 2864`) |
| **2** | `Screen.Billing` | `बिलिंग` (Invoices, Quotes, Returns)| `point: "48%,96%"` (Center: `650, 2864`) |
| **3** | `Screen.Products` | `प्रोडक्ट्स` (Stock Catalog) | `point: "67%,96%"` (Center: `903, 2864`) |
| **4** | `Screen.GstHub` / Settings | `सेटिंग्स` (Settings & GST Hub) | `point: "86%,96%"` (Center: `1156, 2864`)|

### Full-Screen Sub-Destinations:
* `Screen.PartyDetail`: `party_detail/{partyId}?initialTab={initialTab}` (Party 360° Profile, Passbook, Invoices)
* `Screen.CreateInvoice`: `create_invoice?mode={mode}&editId={editId}` (POS Billing, Quotations, Barcode Scanner)
* `Screen.DocumentDetail`: `document_detail/{docType}/{docId}` (Invoice, Quote, Credit Note details)
* `Screen.PdfViewer`: `pdf_viewer?filePath={filePath}&...` (In-app PDF viewer with WhatsApp / Print sharing)
* `Screen.Leads`: `leads` (CRM Pipeline & Follow-up Scheduler)
* `Screen.BiometricLock`: `biometric_lock` (Hardware Fingerprint / Face Unlock)

---

## 3. 🗄️ Database Architecture (Room SQLite v9)

Database Class: `ChopdaDatabase.kt` (Room SQLite)

| Entity Class | Table Name | Purpose / Core Fields |
| :--- | :--- | :--- |
| `CompanyEntity` | `companies` | Multi-tenancy businesses (`firmName`, `gstin`, `phone`, `upiId`, `logoPath`) |
| `CustomerEntity` | `customers` | Khata accounts (`name`, `phone`, `balance`, `partyType`, `creditLimit`, `gstin`) |
| `PaymentEntity` | `payments` | Audit-trailed cash/UPI vouchers (`amount`, `isJama`, `paymentMode`, `receiptNumber`) |
| `InvoiceEntity` | `invoices` | Sales tax invoices (`invoiceNumber`, `subtotal`, `taxTotal`, `grandTotal`, `balanceDue`) |
| `ItemEntity` | `invoice_items` | Invoice line items (`description`, `hsnCode`, `quantity`, `rate`, `gstRate`) |
| `ProductEntity` | `products` | Inventory catalog (`barcode`, `sku`, `stockQty`, `purchaseRate`, `sellingRate`) |
| `QuotationEntity` | `quotations` | Estimates (`quotationNumber`, `expiryDate`, `convertedToInvoice`) |
| `CreditNoteEntity`| `credit_notes` | Sales returns (`creditNoteNumber`, `originalInvoiceId`, `amount`, `reason`) |
| `LeadEntity` | `leads` | CRM leads (`leadStage`, `leadScore`, `source`, `nextFollowupDate`) |
| `LeaseEntity` | `sequence_leases`| Rule 46 offline numbering leases (`prefix`, `rangeStart`, `currentCursor`) |
| `SupplierEntity` | `suppliers` | Supplier accounts (`name`, `phone`, `balance`) |
| `PurchaseEntity` | `purchases` | Purchase invoices (`purchaseNumber`, `totalAmount`) |

---

## 4. 🚀 POS Hardware & Financial Modules

1. **Rule 46 Numbering**: `AllocateLeaseUseCase.kt` issues numbers like `DC/26-27/01001` with local leases to ensure zero duplicate numbers even during offline sales.
2. **Dynamic UPI QR Code**: `UpiQrDialog.kt` generates instant ZXing QR codes encoded with standard NPCI UPI payload: `upi://pay?pa={vpa}&pn={name}&am={amount}&tn={invoiceNo}`.
3. **Bluetooth Thermal Receipt Printing**: `ThermalPrinterManager.kt` connects to 58mm/80mm ESC/POS printers over Bluetooth SPP.
4. **Barcode Scanning**: `BarcodeScannerDialog.kt` powered by CameraX and ML Kit Barcode API.
5. **PDF Generation**: `InvoicePdfGenerator.kt` compiles standard A4 and 3-inch thermal PDF receipts with FileProvider sharing.

---

## 5. 🧪 E2E Test Suite (Maestro CLI)

Located in `android/.maestro/` — verified 100% passing in Reality Mode:
* `01_login_flow.yaml`: Handles 1-tap biometric or admin credentials, asserts Dashboard landing.
* `02_add_customer_flow.yaml`: Opens Party tab, taps `+ Add Party`, fills customer form, taps `SAVE PARTY`, and verifies via search.
* `03_record_transaction_flow.yaml`: Opens party 360°, taps `+ Receive`, inputs amount (₹1,500), selects UPI, confirms, and verifies passbook ledger row.
* `04_billing_and_itemized_detail_flow.yaml`: Opens Billing tab, selects "Walk-in Customer" via dropdown, adds item to cart, generates invoice, views PDF viewer, and asserts itemized products table.
* `05_party_new_bill_preselect_flow.yaml`: Searches party in 89-party master list, taps party 360°, taps `+ New Bill`, and verifies customer pre-selection in invoice creator.
* `06_product_creation_and_search_flow.yaml`: Opens Products tab, taps `+ Add Product`, creates "Raymond Wool Blend Blazer" with base price ₹2,500, and verifies catalog list.
* `07_pos_fast_cart_and_stepper_flow.yaml`: Tests fast POS cart, catalog dropdown selection, quantity stepper increment, 1-tap print and save sticky buttons.
* `08_sync_transparency_and_soundbox_flow.yaml`: Asserts Cloud Synced pill, verifies header actions, company switcher dismiss, and Billing Hub Outbox sync transparency.
* `09_statement_download_flow.yaml`: Verifies Dashboard statement download flow and FileProvider URI authority.

### Master Runner:
```bash
cd android
./run_e2e_tests.sh
# Or targeting explicit device:
DEVICE=emulator-5554 ./run_e2e_tests.sh
```

---

## 6. ⚡ Build & Performance Settings (`gradle.properties`)
* `org.gradle.parallel=true`
* `org.gradle.caching=true`
* `org.gradle.vfs.watch=true`
* `kotlin.incremental=true`
* `org.gradle.jvmargs=-Xmx4096m -XX:+UseG1GC -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8`
* `composeCompiler.featureFlags = [OptimizeNonSkippingGroups]`

---

## 7. 🧩 Complete Component Inventory & Architecture Reference

### 7.1 Application Entry Points
* **`DigitalChopdaApplication.kt`**: Main Android Application class initializing Hilt/dependency injection, coroutine scopes, and global application state.
* **`MainActivity.kt`**: Single-activity Jetpack Compose container handling edge-to-edge window insets, status bar appearance, and top-level NavHost rendering.

### 7.2 Core Modules (`core/`)
* **Design System (`core/designsystem/`)**:
  * `Color.kt`: Defines brand color palettes (`ChopdaRed`, `ChopdaGold`, `CreditGreen`, `DebitRed`, `SurfaceDark`, `CanvasSlate`).
  * `Components.kt`: Reusable composables including `MetricSummaryCard`, `FloatingCapsuleDock`, custom buttons, badges, and ledger rows.
  * `Theme.kt`: Material 3 `DigitalChopdaTheme` enforcing consistent typography, shapes, and color schemes.
  * `Type.kt`: Compose typography configuration optimized for financial numerals and Indian currency formats.
* **Document Management (`core/document/`)**:
  * `DocumentItemParser.kt`: Utility parsing line items, HSN codes, and tax rates from document data structures.
  * `InvoicePdfGenerator.kt`: Generates professional A4 sales tax invoices and 3-inch thermal receipt PDFs using Android `PdfDocument` and Canvas, with `FileProvider` URI sharing.
* **Localization (`core/localization/`)**:
  * `AppLanguage.kt`: Enum representing supported languages (`English`, `Hindi`, `Gujarati`).
  * `AppStrings.kt`: Comprehensive dictionary of localized static UI strings.
  * `LanguageManager.kt`: Manages runtime language switching and preferences persistence.
* **Network & API (`core/network/`)**:
  * `ApiModels.kt`: Data transfer objects (DTOs) for backend synchronization.
  * `AuthHeaderInterceptor.kt`: OkHttp interceptor injecting Bearer auth tokens and firm identification headers.
  * `DigitalChopdaApiService.kt`: Retrofit service interface defining REST endpoints for cloud backup and sync.
  * `MockDigitalChopdaApiService.kt`: Mock service implementation for offline testing and fallback scenarios.
  * `NetworkClient.kt`: OkHttpClient and Retrofit builder singleton.
* **Payment & Security (`core/payment/`, `core/security/`, `core/printer/`, `core/sound/`)**:
  * `UpiPaymentUtils.kt`: Constructs NPCI-compliant UPI deep link URIs (`upi://pay?pa=...`) for instant QR collections.
  * `ThermalPrinterManager.kt`: Bluetooth SPP manager connecting to 58mm/80mm ESC/POS thermal receipt printers.
  * `BiometricAuthManager.kt`: Wrapper around AndroidX `BiometricPrompt` for hardware fingerprint / face unlock.
  * `SecurityVault.kt`: `EncryptedSharedPreferences` vault storing auth tokens and sensitive credentials securely.
  * `SoundEffectsManager.kt`: Audio feedback manager for cash register drawer sounds and barcode scanner beeps.
  * `InvoiceSyncWorker.kt`: WorkManager background worker ensuring queued invoices sync to the backend.

### 7.3 Data Architecture (`data/`)
* **Local Persistence (`data/local/`)**:
  * `entity/Entities.kt`: Room database entity models (`CompanyEntity`, `CustomerEntity`, `PaymentEntity`, `InvoiceEntity`, `ItemEntity`, `ProductEntity`, `QuotationEntity`, `CreditNoteEntity`, `LeadEntity`, `LeaseEntity`, `SupplierEntity`, `PurchaseEntity`).
  * `dao/AppDaos.kt`: Room DAOs providing SQL queries and Flow observables for all entities.
  * `database/ChopdaDatabase.kt`: Room database singleton managing migrations (v1 to v14) and schema validation.
* **Repositories (`data/repository/`)**:
  * `AppRepositories.kt`: Repositories (`CompanyRepository`, `CustomerRepository`, `InvoiceRepository`, `ProductRepository`, `QuotationRepository`, `LeadRepository`, `SettingsRepository`) abstracting data sources for ViewModels.
  * `PurchaseRepository.kt`: Repository handling supplier purchases and stock inward flows.

### 7.4 Domain Layer (`domain/`)
* **Models (`domain/model/`)**:
  * `DomainModels.kt`: Pure Kotlin domain data classes representing business concepts decoupled from database schemas.
* **Use Cases (`domain/usecase/`)**:
  * `DomainUseCases.kt`: Business rules implementation including `AllocateLeaseUseCase` (Rule 46 invoice numbering), `CalculateGstUseCase`, and transaction processing logic.

### 7.5 Presentation & UI Layer (`presentation/`)
* **Authentication (`presentation/auth/`)**:
  * `BiometricLockScreen.kt`: Lock screen requiring biometric verification before accessing sensitive ledgers.
  * `LoginRegisterScreen.kt`: Authentication screen for merchant login and initial registration.
* **Billing & POS (`presentation/billing/`)**:
  * `BillingHubScreen.kt`: Hub screen for managing sales invoices, estimates, and returns.
  * `CreateInvoiceScreen.kt`: POS billing screen with cart management, live tax calculation, and quick payment selection.
  * `DocumentDetailScreen.kt`: Detailed view for viewing/sharing invoices, quotations, and credit notes.
  * `CreateCreditNoteDialog.kt`: Dialog for issuing sales return credit notes linked to original invoices.
  * `InvoiceRevisionHistoryDialog.kt`: Dialog displaying audit history of invoice modifications.
  * `UpiQrDialog.kt`: Dynamic UPI QR code generator for instant customer payment collection.
  * `WhatsAppDispatchDialog.kt`: Dialog for dispatching professional invoice PDFs and payment reminders via WhatsApp.
  * `BarcodeScannerDialog.kt`: CameraX barcode scanner dialog for rapid product lookup in POS billing.
  * `InvoiceViewModel.kt`: ViewModel managing invoice cart state, totals calculation, and database persistence.
* **Dashboard (`presentation/dashboard/`)**:
  * `DashboardScreen.kt`: Rokad cash drawer, business metrics, recent invoice feeds, and daily statement download.
  * `DashboardViewModel.kt`: ViewModel driving dashboard state, cashflow calculations, and metric aggregates.
* **Core & Sound Systems (`core/sound/`)**:
  * `SoundEffectsManager.kt`: Native Android `TextToSpeech` voice soundbox engine (Indian English / Hindi voice announcements: "Payment of ₹X received from [Customer] on DigitalChopda") combined with dual high-frequency sine wave `ToneGenerator` chimes.
* **GST & Compliance (`presentation/gst/`)**:
  * `GstHubScreen.kt`: Live Reality Mode GST filing dashboard aggregating actual Room SQLite records (`InvoiceDao.getAllInvoicesSync` and `PurchaseDao.getAllPurchasesSync`) for dynamic tax liability (CGST, SGST, IGST), ITC credit, net tax payable, and B2B/B2C statutory counts.
* **Leads & CRM (`presentation/leads/`)**:
  * `LeadsScreen.kt`: CRM sales pipeline and follow-up scheduler for customer acquisition.
* **Main Container & Navigation (`presentation/main/`, `presentation/navigation/`)**:
  * `MainContainerScreen.kt`: Root presentation container hosting the 5-tab persistent `FloatingCapsuleDock` and NavHost.
  * `NavGraph.kt`: Type-safe Jetpack Compose navigation graph defining routes and argument passing.
  * `Screen.kt`: Sealed route definitions for all app destinations.
* **Party & Khata Ledger (`presentation/party/`)**:
  * `PartyScreen.kt`: Customer directory and Udhar/Jama Khata ledger list with 150ms search debounce, 1-tap WhatsApp payment reminder dispatch, and 1-tap contextual "Settle" (Jama) / "Pay" (Udhar) quick actions.
  * `PartyDetailScreen.kt`: Party 360° profile screen with ledger passbook, balance summaries, and transaction history.
  * `AddEditPartyDialog.kt`: Dialog for creating or editing customer and supplier accounts.
  * `RecordPaymentDialog.kt`: Dialog for recording payment received (Jama) or given, wired with tactile haptics and Voice Soundbox TTS announcement.
  * `RecordPaymentOutDialog.kt`: Dialog for recording money paid out to suppliers.
  * `RecordPurchaseDialog.kt`: Dialog for recording supplier purchase bills.
  * `PaymentReminderDialog.kt`: Dialog for sending friendly WhatsApp payment reminders with outstanding balances.
* **PDF Viewer (`presentation/pdf/`)**:
  * `PdfViewerScreen.kt`: In-app PDF viewer screen supporting pinch-to-zoom, WhatsApp sharing, and printing.
* **Products & Inventory (`presentation/products/`)**:
  * `ProductsScreen.kt`: Comprehensive inventory catalog screen with 150ms debounced search, stock counts, stock ledger audit logs, and barcode filters.
  * `AddEditProductDialog.kt`: Dialog for adding or updating products, HSN codes, and pricing.
  * `AdjustStockDialog.kt`: Dialog for recording stock additions/reductions with audit reasons.
* **Settings (`presentation/settings/`)**:
  * `SettingsScreen.kt`: Application settings, language selection, backup, and security configurations.
  * `CompanySwitchSheet.kt`: Bottom sheet allowing merchants to instantly switch between active business firms.
* **Splash (`presentation/splash/`)**:
  * `SplashScreen.kt`: Adaptive Velocity startup (<380ms transition into active merchant session, ~724ms cold launch).

### 7.6 Utilities (`util/`)
* **`FormatUtils.kt`**: Helper functions for Indian currency formatting (`₹ 1,23,456.00`), phone number sanitation, and date-time formatting.

---

## 8. 📜 MANDATORY IMPLEMENTATION & LIVING DOCUMENTATION RULES

### 8.1 The Twin-Pillar Definition of Done (Reality Verification + Living Memory Sync)
Whenever planning, coding, or modifying any feature in DigitalChopda Android:

1. **Pillar 1: Empirical Reality Verification**:
   - Write real production code directly in the live strata.
   - Run live build verification: `./gradlew assembleDebug` or `./gradlew testDebugUnitTest`.
   - Run Maestro E2E test flows (`./run_e2e_tests.sh` or `maestro test .maestro/*.yaml`) on the connected device.
   - Ensure explicit Room migrations: never use `fallbackToDestructiveMigration()`.
   - **Zero Simulations**: No theoretical loops, fake tests, or sandboxed stubs. Pure execution only.

2. **Pillar 2: Living Documentation Update (Token Preservation Rule)**:
   - Update `APP_CONTEXT.md` immediately with any new composable, entity column, DAO query, navigation destination, or DTO.
   - **Why?**: This document is the externalized long-term memory of the Android project. Keeping it 100% synchronized allows future AI sessions to read this single file (~1,500 tokens) instead of recursively scanning 100+ Kotlin source files (~40,000 tokens).

### 8.2 Room Schema Migration Protocol
- Production migrations must define explicit `Migration(fromVersion, toVersion)`.
- Update Section 3 (Database Architecture) whenever Room entities or DAOs are altered.

### 8.3 Backend & Cloud Sync Parity
- Keep client DTOs in `core/network/ApiModels.kt` 100% aligned with the DigitalChopda Web/Backend (44 PostgreSQL migrations, `/api/v1/sync` batch delta synchronizer).
- Enforce UUID idempotency keys on every offline transaction.
