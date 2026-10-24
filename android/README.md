# 📒 DigitalChopda (ડિજિટલ ચોપડા)

> Sovereign Khata, Vyapar Ledger & Cashbook Android Application

---

## 🏛️ What is DigitalChopda?

In the Indian market, a traditional merchant relies on three physical tools:
1. **The Chopda (ચોપડો)**: The sacred red-cloth bound ledger book for tracking credit and debit (Udhar-Jama).
2. **The Rokad Box (રોકડ)**: The physical cash drawer for daily in-hand cash flow.
3. **The Bill Book**: For invoices and receipts.

**DigitalChopda** is the modern, sovereign digital transformation of this ancient practice into a native Android app:
- **Zero Latency Architecture**: Real-time cloud-synchronized Room SQLite database engineered for high reliability across all retail markets.
- **Atomic Balance Updates**: Every transaction automatically updates the customer's balance.
- **WhatsApp Payment Reminders**: 1-click friendly payment reminders sent directly to customers with their exact outstanding balance.
- **Daily Rokad Register**: Track morning cash, sales, operational expenses, and closing balance.

---

## 🏗️ Architecture

```
com.digitalchopda.app
├── core/
│   ├── designsystem/                 (Midnight Surface, Theme, Typography, Components)
│   ├── document/                     (InvoicePdfGenerator, DocumentItemParser, ReportExport)
│   ├── localization/                 (Gujarati, Hindi, English LanguageManager)
│   ├── network/                      (Retrofit, OkHttp, DigitalChopdaApiService, MockApi)
│   ├── payment/                      (Dynamic UPI QR bitmatrix generator)
│   ├── printer/                      (ESC/POS 58mm/80mm Bluetooth SPP thermal driver)
│   ├── security/                     (BiometricPrompt, AES-256 SecurityVault)
│   └── sound/                        (Retail soundbox and scanner chime manager)
├── data/
│   ├── local/
│   │   ├── entity/                   (15 Entities: Customer, Invoice, Payment, Product, etc.)
│   │   ├── dao/                      (15 DAOs with reactive Kotlin Flows)
│   │   └── database/                 (ChopdaDatabase Room SQLite v15 Singleton)
│   ├── repository/                   (AppRepositories, PurchaseRepository)
│   └── work/                         (InvoiceSyncWorker background sync)
├── domain/
│   ├── model/                        (Pure business models: Company, Invoice, Product, etc.)
│   └── usecase/                      (AllocateLeaseUseCase, CreateInvoiceUseCase, RebaseStockUseCase)
├── presentation/
│   ├── auth/                         (LoginRegisterScreen, BiometricLockScreen)
│   ├── billing/                      (BillingHubScreen, CreateInvoiceScreen, POS Dialogs)
│   ├── dashboard/                    (DashboardScreen, DashboardViewModel)
│   ├── gst/                          (GstHubScreen, PreFilingAnomalyDialog)
│   ├── leads/                        (LeadsScreen CRM pipeline)
│   ├── main/                         (MainContainerScreen with FloatingCapsuleDock)
│   ├── navigation/                   (NavGraph, Screen routes)
│   ├── party/                        (PartyScreen, PartyDetailScreen, Khata Dialogs)
│   ├── pdf/                          (PdfViewerScreen with in-app sharing)
│   ├── products/                     (ProductsScreen, AddEditProductDialog, AdjustStockDialog)
│   ├── settings/                     (SettingsScreen, CompanySwitchSheet)
│   └── splash/                       (SplashScreen)
└── util/
    └── FormatUtils.kt                (Indian Rupee formatting & date utilities)
```

---

## 🚀 Build & Verification Commands

```bash
# Run Unit Tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Build Release Minified APK
./gradlew assembleRelease

# Run Maestro E2E Flows
./run_e2e_tests.sh
```
