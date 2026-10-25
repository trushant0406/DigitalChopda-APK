package com.digitalchopda.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.digitalchopda.app.data.local.dao.CompanyDao
import com.digitalchopda.app.data.local.dao.CreditNoteDao
import com.digitalchopda.app.data.local.dao.CustomerDao
import com.digitalchopda.app.data.local.dao.InvoiceDao
import com.digitalchopda.app.data.local.dao.ItemDao
import com.digitalchopda.app.data.local.dao.LeadDao
import com.digitalchopda.app.data.local.dao.LeaseDao
import com.digitalchopda.app.data.local.dao.OrderRevisionDao
import com.digitalchopda.app.data.local.dao.PaymentDao
import com.digitalchopda.app.data.local.dao.ProductDao
import com.digitalchopda.app.data.local.dao.PurchaseDao
import com.digitalchopda.app.data.local.dao.QuotationDao
import com.digitalchopda.app.data.local.dao.StockAdjustmentDao
import com.digitalchopda.app.data.local.dao.SupplierDao
import com.digitalchopda.app.data.local.dao.OutboxDao
import com.digitalchopda.app.data.local.entity.CompanyEntity
import com.digitalchopda.app.data.local.entity.CreditNoteEntity
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.ItemEntity
import com.digitalchopda.app.data.local.entity.LeadEntity
import com.digitalchopda.app.data.local.entity.LeaseEntity
import com.digitalchopda.app.data.local.entity.OrderRevisionEntity
import com.digitalchopda.app.data.local.entity.PaymentEntity
import com.digitalchopda.app.data.local.entity.ProductEntity
import com.digitalchopda.app.data.local.entity.PurchaseEntity
import com.digitalchopda.app.data.local.entity.PurchaseItemEntity
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.data.local.entity.StockAdjustmentEntity
import com.digitalchopda.app.data.local.entity.SupplierEntity
import com.digitalchopda.app.data.local.entity.OutboxEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CompanyEntity::class,
        LeaseEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        ItemEntity::class,
        LeadEntity::class,
        CustomerEntity::class,
        QuotationEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        StockAdjustmentEntity::class,
        OrderRevisionEntity::class,
        ProductEntity::class,
        CreditNoteEntity::class,
        OutboxEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class ChopdaDatabase : RoomDatabase() {

    abstract fun companyDao(): CompanyDao
    abstract fun leaseDao(): LeaseDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun itemDao(): ItemDao
    abstract fun leadDao(): LeadDao
    abstract fun customerDao(): CustomerDao
    abstract fun quotationDao(): QuotationDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun productDao(): ProductDao
    abstract fun creditNoteDao(): CreditNoteDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun orderRevisionDao(): OrderRevisionDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var INSTANCE: ChopdaDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotations ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE credit_notes ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE invoices SET paidAmount = grandTotal WHERE status = 'PAID'")
                db.execSQL("ALTER TABLE payments ADD COLUMN invoiceId INTEGER")
                db.execSQL("ALTER TABLE payments ADD COLUMN orderId INTEGER")
                db.execSQL("ALTER TABLE payments ADD COLUMN invoiceNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_invoiceId ON payments (invoiceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_orderId ON payments (orderId)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Invoices
                db.execSQL("ALTER TABLE invoices ADD COLUMN clientReferenceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN syncAttempts INTEGER NOT NULL DEFAULT 0")

                // Payments
                db.execSQL("ALTER TABLE payments ADD COLUMN clientReferenceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN serverContactId INTEGER")

                // Customers
                db.execSQL("ALTER TABLE customers ADD COLUMN serverContactId INTEGER")
                db.execSQL("ALTER TABLE customers ADD COLUMN clientReferenceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN serverOutstanding REAL NOT NULL DEFAULT 0.0")

                // Credit Notes
                db.execSQL("ALTER TABLE credit_notes ADD COLUMN clientReferenceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE credit_notes ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC'")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN fulfillmentStatus TEXT NOT NULL DEFAULT 'not_ready'")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotations ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC'")
                db.execSQL("ALTER TABLE quotations ADD COLUMN serverQuotationId INTEGER")
                db.execSQL("ALTER TABLE quotations ADD COLUMN clientReferenceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_quotations_syncStatus ON quotations (syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_quotations_serverQuotationId ON quotations (serverQuotationId)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Companies - Bank details
                db.execSQL("ALTER TABLE companies ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE companies ADD COLUMN bankAccountNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE companies ADD COLUMN bankIfsc TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE companies ADD COLUMN bankBranch TEXT NOT NULL DEFAULT ''")

                // 2. Invoices - WhatsApp audit fields
                db.execSQL("ALTER TABLE invoices ADD COLUMN whatsappSentCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN lastWhatsappSentAt INTEGER")

                // 3. Products - Stock & Catalog fields
                db.execSQL("ALTER TABLE products ADD COLUMN stockQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE products ADD COLUMN minStockAlert REAL NOT NULL DEFAULT 5.0")
                db.execSQL("ALTER TABLE products ADD COLUMN purchasePrice REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE products ADD COLUMN sku TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

                // 4. Payments - Settlement & Void audit
                db.execSQL("ALTER TABLE payments ADD COLUMN isSettlement INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN isVoid INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN voidReason TEXT")
                db.execSQL("ALTER TABLE payments ADD COLUMN voidedAt INTEGER")

                // 5. Purchases - Enhanced columns & indices
                db.execSQL("ALTER TABLE purchases ADD COLUMN supplierInvoiceNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchases ADD COLUMN subtotal REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE purchases ADD COLUMN taxAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE purchases ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'cash'")
                db.execSQL("ALTER TABLE purchases ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchases ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC'")
                db.execSQL("ALTER TABLE purchases ADD COLUMN serverPurchaseId INTEGER")
                db.execSQL("ALTER TABLE purchases ADD COLUMN clientReferenceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchases ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_companyId ON purchases (companyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_supplierId ON purchases (supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_purchaseNumber ON purchases (purchaseNumber)")

                // 6. Purchase Items table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS purchase_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        purchaseId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitPrice REAL NOT NULL,
                        gstRate REAL NOT NULL DEFAULT 0.0,
                        hsnCode TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_purchaseId ON purchase_items (purchaseId)")

                // 7. Stock Adjustments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stock_adjustments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        adjustmentType TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        previousStock REAL NOT NULL,
                        newStock REAL NOT NULL,
                        reason TEXT NOT NULL DEFAULT 'Manual Adjustment',
                        notes TEXT,
                        clientReferenceId TEXT NOT NULL DEFAULT '',
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_companyId_productId ON stock_adjustments (companyId, productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_companyId_createdAt ON stock_adjustments (companyId, createdAt)")

                // 8. Order Revisions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_revisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        orderId INTEGER NOT NULL,
                        revisionNumber INTEGER NOT NULL DEFAULT 1,
                        revisionDate INTEGER NOT NULL DEFAULT 0,
                        previousTotal REAL NOT NULL,
                        newTotal REAL NOT NULL,
                        deltaAmount REAL NOT NULL DEFAULT 0.0,
                        reason TEXT,
                        itemsSnapshotJson TEXT NOT NULL DEFAULT '[]',
                        newItemsSnapshotJson TEXT NOT NULL DEFAULT '[]',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_order_revisions_companyId_orderId ON order_revisions (companyId, orderId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_order_revisions_orderId_revisionNumber ON order_revisions (orderId, revisionNumber)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS outbox_sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        idempotencyKey TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        actionType TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        syncedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_sync_queue_companyId_status ON outbox_sync_queue (companyId, status)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_outbox_sync_queue_idempotencyKey ON outbox_sync_queue (idempotencyKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_sync_queue_createdAt ON outbox_sync_queue (createdAt)")
            }
        }

        fun getInstance(context: Context): ChopdaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChopdaDatabase::class.java,
                    "digital_chopda_v1.db"
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Enable SQLite fast WAL optimizations for concurrent sub-millisecond reads and writes
                            try {
                                db.execSQL("PRAGMA synchronous = NORMAL")
                                db.execSQL("PRAGMA temp_store = MEMORY")
                            } catch (e: Exception) {
                                android.util.Log.w("ChopdaDatabase", "Could not apply SQLite pragmas", e)
                            }
                        }

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate realistic seed data for immediate out-of-the-box experience
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    populateSeedData(getInstance(context))
                                } catch (e: Exception) {
                                    android.util.Log.e("ChopdaDatabase", "Failed to populate initial seed data", e)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun ensureSeedData(db: ChopdaDatabase) {
            try {
                val companyCount = db.companyDao().getCompanyCount()
                if (companyCount == 0) {
                    android.util.Log.d("ChopdaDatabase", "Seeding initial company into Room database...")
                    populateSeedData(db)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChopdaDatabase", "Failed to ensure seed data", e)
            }
        }

        private suspend fun populateSeedData(db: ChopdaDatabase) {
            // Seed Initial Default Company aligned with Backend Business #1
            val c1 = db.companyDao().insertCompany(
                CompanyEntity(
                    id = 1L,
                    firmName = "My Business",
                    ownerName = "Admin User",
                    gstin = "",
                    phone = "9876543210",
                    address = "Surat, Gujarat",
                    stateCode = "24",
                    financialYear = "26-27",
                    invoicePrefix = "INV-",
                    isActive = true,
                    upiVpa = ""
                )
            )

            // Seed Sequence Leases (Rule 46)
            db.leaseDao().insertOrUpdateLease(
                LeaseEntity(
                    companyId = c1,
                    prefix = "INV-",
                    financialYear = "26-27",
                    rangeStart = 1001L,
                    rangeEnd = 1200L,
                    currentCursor = 1001L
                )
            )

            // Seed Inventory Items
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-COT-01",
                    itemName = "Pure Cotton Shahi Saree 6.3m",
                    packSize = "Box of 1",
                    quantity = 42,
                    sellingPrice = 1850.0,
                    costPrice = 1200.0,
                    gstRatePercent = 5.0,
                    minStockThreshold = 10,
                    version = 1
                )
            )
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-SILK-02",
                    itemName = "Kanjivaram Jacquard Silk",
                    packSize = "Box of 1",
                    quantity = 28,
                    sellingPrice = 4200.0,
                    costPrice = 2900.0,
                    gstRatePercent = 5.0,
                    minStockThreshold = 8,
                    version = 1
                )
            )
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-KUR-03",
                    itemName = "Embroidered Kurti Set M/L/XL",
                    packSize = "Pack of 3",
                    quantity = 3, // Low stock alert!
                    sellingPrice = 1450.0,
                    costPrice = 900.0,
                    gstRatePercent = 12.0,
                    minStockThreshold = 5,
                    version = 1
                )
            )
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-DUP-04",
                    itemName = "Bandhani Georgette Dupatta",
                    packSize = "Single",
                    quantity = 2, // Low stock alert!
                    sellingPrice = 650.0,
                    costPrice = 380.0,
                    gstRatePercent = 5.0,
                    minStockThreshold = 6,
                    version = 1
                )
            )

            // Seed Products Catalog (Aligning with Web Products)
            db.productDao().insertProduct(
                ProductEntity(
                    companyId = c1,
                    name = "Pure Cotton Shahi Saree 6.3m",
                    defaultPrice = 1850.0,
                    hsnCode = "5208",
                    gstRate = 5.0,
                    unit = "PCS"
                )
            )
            db.productDao().insertProduct(
                ProductEntity(
                    companyId = c1,
                    name = "Kanjivaram Jacquard Silk",
                    defaultPrice = 4200.0,
                    hsnCode = "5007",
                    gstRate = 5.0,
                    unit = "PCS"
                )
            )
            db.productDao().insertProduct(
                ProductEntity(
                    companyId = c1,
                    name = "Embroidered Kurti Set M/L/XL",
                    defaultPrice = 1450.0,
                    hsnCode = "6204",
                    gstRate = 12.0,
                    unit = "SET"
                )
            )
            db.productDao().insertProduct(
                ProductEntity(
                    companyId = c1,
                    name = "Bandhani Georgette Dupatta",
                    defaultPrice = 650.0,
                    hsnCode = "5407",
                    gstRate = 5.0,
                    unit = "PCS"
                )
            )
            db.productDao().insertProduct(
                ProductEntity(
                    companyId = c1,
                    name = "Pure Chanderi Fabric 10m",
                    defaultPrice = 1200.0,
                    hsnCode = "5208",
                    gstRate = 5.0,
                    unit = "MTR"
                )
            )
            db.productDao().insertProduct(
                ProductEntity(
                    companyId = c1,
                    name = "Steel Bolt (M8)",
                    defaultPrice = 45.0,
                    hsnCode = "7318",
                    gstRate = 18.0,
                    unit = "PCS"
                )
            )

            // Seed Customers (Khatavahi Accounts & Integrated Leads)
            val custRameshId = db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Ramesh Patel (Textiles)",
                    phone = "+91 98251 22340",
                    gstin = "24AABCR1234F1Z8",
                    pan = "AABCR1234F",
                    companyName = "Patel Textiles Pvt Ltd",
                    city = "Surat",
                    state = "Gujarat",
                    balance = 12771.0,
                    address = "Shop 45, Ring Road Cloth Market, Surat",
                    creditLimit = 100000.0,
                    openingBalance = 12771.0,
                    openingBalanceType = "to_receive",
                    leadPriority = "HOT",
                    expectedDealValue = 45000.0,
                    leadNotes = "Regular wholesale buyer, interested in festival silk lot",
                    nextFollowUpDate = System.currentTimeMillis() + 86400000L
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Shreenathji Silk Mills",
                    phone = "+91 97240 11980",
                    gstin = "24BBDCS5678G1Z2",
                    pan = "BBDCS5678G",
                    companyName = "Shreenathji Fabrics",
                    city = "Surat",
                    state = "Gujarat",
                    balance = -3013.0,
                    address = "Plot 12, Pandesara GIDC, Surat",
                    creditLimit = 50000.0,
                    openingBalance = -3013.0,
                    openingBalanceType = "to_pay",
                    leadPriority = "WARM",
                    expectedDealValue = 65000.0
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Ananya Fashion Hub",
                    phone = "+91 98980 55432",
                    gstin = "24CCDCA9012H1Z5",
                    companyName = "Ananya Boutique",
                    city = "Surat",
                    state = "Gujarat",
                    balance = 4120.0,
                    address = "Ghod Dod Road, Surat",
                    creditLimit = 75000.0,
                    openingBalance = 4120.0,
                    openingBalanceType = "to_receive",
                    leadPriority = "HOT",
                    expectedDealValue = 30000.0
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Sneha Boutique",
                    phone = "+91 98980 11223",
                    gstin = "24AABCS9988D1Z3",
                    companyName = "Sneha Designer Studio",
                    city = "Ahmedabad",
                    state = "Gujarat",
                    balance = 0.0,
                    address = "CG Road, Navrangpura, Ahmedabad",
                    creditLimit = 80000.0,
                    openingBalance = 0.0,
                    leadPriority = "HOT",
                    expectedDealValue = 45000.0,
                    leadNotes = "Urgent requirement for 50 Designer Kurtis for exhibition"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Pooja Varma",
                    phone = "+91 98251 12345",
                    gstin = "24AABCV9988E1Z1",
                    companyName = "Varma Collections",
                    city = "Surat",
                    state = "Gujarat",
                    balance = 0.0,
                    address = "Adajan, Surat",
                    creditLimit = 30000.0,
                    openingBalance = 0.0,
                    leadPriority = "CONVERTED"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Rajesh Patel",
                    phone = "+91 97240 54321",
                    companyName = "Rajesh Trading Co",
                    city = "Surat",
                    state = "Gujarat",
                    balance = 0.0,
                    address = "Varachha, Surat",
                    leadPriority = "COLD"
                )
            )

            // Seed Leads for pipeline
            db.leadDao().insertLead(
                LeadEntity(
                    companyId = c1,
                    customerName = "Sneha Boutique",
                    businessName = "Sneha Shah",
                    phone = "+91 98980 11223",
                    priority = "HOT",
                    requirementDetails = "Urgent requirement for 50 Designer Kurtis for Diwali wedding exhibition",
                    expectedDealValue = 45000.0,
                    followUpTimestamp = System.currentTimeMillis() + 86400000L
                )
            )
            db.leadDao().insertLead(
                LeadEntity(
                    companyId = c1,
                    customerName = "Manish Textiles Wholesale",
                    businessName = "Manish Agrawal",
                    phone = "+91 94260 77889",
                    priority = "WARM",
                    requirementDetails = "Bulk sample order for 200 Banarasi Duppatas",
                    expectedDealValue = 85000.0,
                    followUpTimestamp = System.currentTimeMillis() + (86400000L * 2)
                )
            )

            // Seed Invoices for Ramesh Patel (Textiles)
            val invRamesh1 = db.invoiceDao().insertInvoice(
                InvoiceEntity(
                    companyId = c1,
                    invoiceNumber = "DC/26-27/00998",
                    customerName = "Ramesh Patel (Textiles)",
                    customerPhone = "+91 98251 22340",
                    itemsSummary = "Pure Cotton Shahi Saree (x5), Surat Chanderi Silk (x3)",
                    subtotal = 14285.71,
                    cgst = 357.14,
                    sgst = 357.14,
                    igst = 0.0,
                    roundOff = 0.01,
                    grandTotal = 15000.0,
                    paidAmount = 15000.0,
                    status = "PAID",
                    paymentMode = "BANK",
                    syncStatus = "SYNCED",
                    idempotencyKey = java.util.UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                )
            )
            val invRamesh2 = db.invoiceDao().insertInvoice(
                InvoiceEntity(
                    companyId = c1,
                    invoiceNumber = "DC/26-27/01002",
                    customerName = "Ramesh Patel (Textiles)",
                    customerPhone = "+91 98251 22340",
                    itemsSummary = "Kanjivaram Jacquard Silk (x4), Surat Chanderi Silk (x2)",
                    subtotal = 17619.05,
                    cgst = 440.48,
                    sgst = 440.48,
                    igst = 0.0,
                    roundOff = -0.01,
                    grandTotal = 18500.0,
                    paidAmount = 5000.0,
                    status = "PARTIAL",
                    paymentMode = "CASH",
                    syncStatus = "SYNCED",
                    idempotencyKey = java.util.UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000
                )
            )

            // Seed Recent Invoices for Pooja Varma
            db.invoiceDao().insertInvoice(
                InvoiceEntity(
                    companyId = c1,
                    invoiceNumber = "DC/26-27/01001",
                    customerName = "Pooja Varma",
                    customerPhone = "+91 98251 12345",
                    itemsSummary = "Pure Cotton Shahi Saree (x2), Kanjivaram Jacquard Silk (x1)",
                    subtotal = 7900.0,
                    cgst = 197.5,
                    sgst = 197.5,
                    igst = 0.0,
                    roundOff = 0.0,
                    grandTotal = 8295.0,
                    paidAmount = 8295.0,
                    status = "PAID",
                    paymentMode = "UPI",
                    syncStatus = "SYNCED",
                    idempotencyKey = java.util.UUID.randomUUID().toString()
                )
            )

            // Seed Payments for Ramesh Patel (Textiles)
            db.paymentDao().insertPayment(
                PaymentEntity(
                    companyId = c1,
                    customerId = custRameshId,
                    customerName = "Ramesh Patel (Textiles)",
                    customerPhone = "+91 98251 22340",
                    invoiceId = invRamesh1,
                    invoiceNumber = "DC/26-27/00998",
                    amount = 15000.0,
                    isJama = true,
                    paymentMode = "bank",
                    receiptNumber = "REC-00101",
                    notes = "Bank NEFT against Bill DC/26-27/00998",
                    syncStatus = "SYNCED",
                    createdAt = System.currentTimeMillis() - 6L * 24 * 60 * 60 * 1000
                )
            )
            db.paymentDao().insertPayment(
                PaymentEntity(
                    companyId = c1,
                    customerId = custRameshId,
                    customerName = "Ramesh Patel (Textiles)",
                    customerPhone = "+91 98251 22340",
                    invoiceId = invRamesh2,
                    invoiceNumber = "DC/26-27/01002",
                    amount = 5000.0,
                    isJama = true,
                    paymentMode = "upi",
                    receiptNumber = "REC-00105",
                    notes = "Advance UPI against Bill DC/26-27/01002",
                    syncStatus = "SYNCED",
                    createdAt = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
                )
            )

            // Seed Quotations for Ramesh Patel (Textiles) and Sneha Boutique
            db.quotationDao().insertQuotation(
                QuotationEntity(
                    companyId = c1,
                    quotationNumber = "QT/26-27/00042",
                    customerName = "Ramesh Patel (Textiles)",
                    customerPhone = "+91 98251 22340",
                    itemsSummary = "Banarasi Pure Katan Silk (x10), Surat Chanderi Silk (x5)",
                    subtotal = 38000.0,
                    taxAmount = 1900.0,
                    grandTotal = 39900.0,
                    status = "PENDING",
                    validUntil = System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000,
                    createdAt = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
                )
            )
            db.quotationDao().insertQuotation(
                QuotationEntity(
                    companyId = c1,
                    quotationNumber = "QT/26-27/00043",
                    customerName = "Sneha Boutique",
                    customerPhone = "+91 98980 11223",
                    itemsSummary = "Designer Cotton Kurtis Exhibition Lot (x50)",
                    subtotal = 42857.14,
                    taxAmount = 2142.86,
                    grandTotal = 45000.0,
                    status = "PENDING",
                    validUntil = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000,
                    createdAt = System.currentTimeMillis()
                )
            )

            // Seed Initial Supplier & Purchase
            val s1 = db.supplierDao().insertSupplier(
                SupplierEntity(
                    companyId = c1,
                    name = "Surat Mega Weaving Mills",
                    phone = "+91 98250 88990",
                    gstin = "24AABCS9876E1Z5",
                    openingBalance = 0.0,
                    balanceToPay = 42500.0
                )
            )
            db.purchaseDao().insertPurchase(
                PurchaseEntity(
                    companyId = c1,
                    supplierId = s1,
                    purchaseNumber = "PUR/26-27/0001",
                    vendorBillNumber = "SMWM-9942",
                    grandTotal = 42500.0,
                    paymentStatus = "partial"
                )
            )

            // Seed Initial Credit Notes (Sales Returns)
            db.creditNoteDao().insertCreditNote(
                CreditNoteEntity(
                    companyId = c1,
                    creditNoteNumber = "CN-0001",
                    customerName = "Ramesh Patel (Textiles)",
                    customerPhone = "+91 98251 22340",
                    originalInvoiceNumber = "DC/26-27/00998",
                    reason = "Damaged / Defective Goods",
                    itemsSummary = "Pure Cotton Shahi Saree 6.3m (x1)",
                    subtotal = 1850.0,
                    taxAmount = 92.5,
                    totalAmount = 1942.5,
                    notes = "Weaving defect found on pallu border, credited back to party ledger.",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
                )
            )
            db.creditNoteDao().insertCreditNote(
                CreditNoteEntity(
                    companyId = c1,
                    creditNoteNumber = "CN-0002",
                    customerName = "Sneha Boutique",
                    customerPhone = "+91 98980 11223",
                    originalInvoiceNumber = "DC/26-27/01001",
                    reason = "Sales Return",
                    itemsSummary = "Bandhani Georgette Dupatta (x2)",
                    subtotal = 1300.0,
                    taxAmount = 65.0,
                    totalAmount = 1365.0,
                    notes = "Color mismatch returned by client, ledger adjusted.",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
                )
            )
        }
    }
}
