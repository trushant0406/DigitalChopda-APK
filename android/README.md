# 📒 DigitalChopda (ડિજિટલ ચોપડા)

> Sovereign Khata, Vyapar Ledger & Cashbook Android Application

---

## 🏛️ What is DigitalChopda?

In the Indian market, a traditional merchant relies on three physical tools:
1. **The Chopda (ચોપડો)**: The sacred red-cloth bound ledger book for tracking credit and debit (Udhar-Jama).
2. **The Rokad Box (રોકડ)**: The physical cash drawer for daily in-hand cash flow.
3. **The Bill Book**: For invoices and receipts.

**DigitalChopda** is the modern, sovereign digital transformation of this ancient practice into a native Android app:
- **Zero Internet Requirement**: Pure offline-first Room SQLite database that works in underground basements, rural mandis, and crowded markets.
- **Atomic Balance Updates**: Every transaction automatically updates the customer's balance.
- **WhatsApp Payment Reminders**: 1-click friendly payment reminders sent directly to customers with their exact outstanding balance.
- **Daily Rokad Register**: Track morning cash, sales, operational expenses, and closing balance.

---

## 🏗️ Architecture

```
com.gatistvam.digitalchopda
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── CustomerEntity.kt     (Parties: Customer & Supplier details)
│   │   │   ├── TransactionEntity.kt  (Udhar / Jama entries with notes)
│   │   │   └── CashbookEntity.kt     (Daily in/out cash register)
│   │   ├── dao/
│   │   │   ├── CustomerDao.kt
│   │   │   ├── TransactionDao.kt
│   │   │   └── CashbookDao.kt
│   │   └── DigitalChopdaDatabase.kt  (Room DB Singleton)
│   └── repository/
│       └── ChopdaRepository.kt       (Atomic balance calculation & reactive flows)
├── ui/
│   ├── theme/                        (ChopdaRed, ChopdaGold, CreditGreen, DebitRed)
│   ├── navigation/                   (Type-safe Compose NavHost)
│   ├── components/                   (MetricSummaryCard, PartyItemRow, TransactionItemRow)
│   └── screens/
│       ├── dashboard/                (Balances, search, customer list)
│       ├── customer/                 (Customer detail, WhatsApp reminder, Add customer)
│       ├── transaction/              (You Gave / You Got entry recording)
│       └── cashbook/                 (Daily in/out cash register)
└── util/
    ├── CurrencyFormatter.kt          (Indian Rupee ₹ 1,23,456.00 formatting)
    └── DateTimeUtils.kt              (Localized date/time strings)
```

---

## 🚀 Build & Run

```bash
cd apps/digitalchopda-android
./gradlew assembleDebug
```
