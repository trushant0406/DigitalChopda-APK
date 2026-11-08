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
import com.digitalchopda.app.data.local.dao.BatchDao
import com.digitalchopda.app.data.local.dao.DeliveryChallanDao
import com.digitalchopda.app.data.local.dao.PurchaseOrderDao
import com.digitalchopda.app.data.local.dao.DebitNoteDao
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
import com.digitalchopda.app.data.local.entity.BatchEntity
import com.digitalchopda.app.data.local.entity.DeliveryChallanEntity
import com.digitalchopda.app.data.local.entity.PurchaseOrderEntity
import com.digitalchopda.app.data.local.entity.PurchaseOrderItemEntity
import com.digitalchopda.app.data.local.entity.DebitNoteEntity
import com.digitalchopda.app.data.local.entity.DebitNoteItemEntity
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
        OutboxEntity::class,
        BatchEntity::class,
        DeliveryChallanEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        DebitNoteEntity::class,
        DebitNoteItemEntity::class
    ],
    version = 20,
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
    abstract fun batchDao(): BatchDao
    abstract fun deliveryChallanDao(): DeliveryChallanDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun debitNoteDao(): DebitNoteDao

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

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN primaryImageUrl TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN wholesalePrice REAL")
                db.execSQL("ALTER TABLE products ADD COLUMN wholesaleQuantity REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE products ADD COLUMN batchNumber TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN expiryDate TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN showInOnlineStore INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Invoices E-Invoice & E-Way Bill fields
                db.execSQL("ALTER TABLE invoices ADD COLUMN irn TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN ackNo TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN signedQrCode TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN ewayBillNo TEXT")

                // 2. Products Batches & Serials flags
                db.execSQL("ALTER TABLE products ADD COLUMN hasBatches INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN hasSerials INTEGER NOT NULL DEFAULT 0")

                // 3. Batches table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        batchNumber TEXT NOT NULL,
                        expiryDate INTEGER,
                        manufacturingDate INTEGER,
                        mrp REAL NOT NULL DEFAULT 0.0,
                        purchasePrice REAL NOT NULL DEFAULT 0.0,
                        openingQuantity REAL NOT NULL DEFAULT 0.0,
                        currentQuantity REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_batches_companyId_productId ON batches (companyId, productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_batches_batchNumber ON batches (batchNumber)")

                // 4. Delivery Challans table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS delivery_challans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        challanNumber TEXT NOT NULL,
                        recipientName TEXT NOT NULL,
                        recipientPhone TEXT,
                        shippingAddress TEXT,
                        dispatchDate INTEGER NOT NULL DEFAULT 0,
                        transportMode TEXT NOT NULL DEFAULT 'road',
                        vehicleNumber TEXT,
                        ewayBillNo TEXT,
                        totalQuantity REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'out_for_delivery',
                        orderId INTEGER,
                        itemsJson TEXT NOT NULL DEFAULT '[]',
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_delivery_challans_companyId ON delivery_challans (companyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_delivery_challans_challanNumber ON delivery_challans (challanNumber)")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Payments: Linkage to Inward Purchase bills
                db.execSQL("ALTER TABLE payments ADD COLUMN purchaseId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_purchaseId ON payments (purchaseId)")

                // 2. Companies: WhatsApp Bot & Subscription Tier
                db.execSQL("ALTER TABLE companies ADD COLUMN whatsappBotEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE companies ADD COLUMN plan TEXT NOT NULL DEFAULT 'STARTER'")
                db.execSQL("ALTER TABLE companies ADD COLUMN isExpired INTEGER NOT NULL DEFAULT 0")

                // 3. Purchase Items: Total Amount field
                db.execSQL("ALTER TABLE purchase_items ADD COLUMN totalAmount REAL NOT NULL DEFAULT 0.0")

                // 4. Purchase Orders
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS purchase_orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        supplierId INTEGER NOT NULL,
                        supplierName TEXT NOT NULL DEFAULT '',
                        orderNumber TEXT NOT NULL,
                        orderDate INTEGER NOT NULL DEFAULT 0,
                        expectedDate INTEGER,
                        subtotal REAL NOT NULL DEFAULT 0.0,
                        taxAmount REAL NOT NULL DEFAULT 0.0,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'draft',
                        notes TEXT,
                        clientReferenceId TEXT NOT NULL DEFAULT '',
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC',
                        serverOrderId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_orders_companyId ON purchase_orders (companyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_orders_supplierId ON purchase_orders (supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_orders_orderNumber ON purchase_orders (orderNumber)")

                // 5. Purchase Order Items
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS purchase_order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        purchaseOrderId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitPrice REAL NOT NULL,
                        gstRate REAL NOT NULL DEFAULT 0.0,
                        taxAmount REAL NOT NULL DEFAULT 0.0,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        hsnCode TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_order_items_purchaseOrderId ON purchase_order_items (purchaseOrderId)")

                // 6. Debit Notes (Purchase Returns)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debit_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        companyId INTEGER NOT NULL,
                        supplierId INTEGER NOT NULL,
                        supplierName TEXT NOT NULL DEFAULT '',
                        purchaseId INTEGER,
                        originalPurchaseNumber TEXT NOT NULL DEFAULT '',
                        debitNoteNumber TEXT NOT NULL,
                        debitNoteDate INTEGER NOT NULL DEFAULT 0,
                        subtotal REAL NOT NULL DEFAULT 0.0,
                        taxAmount REAL NOT NULL DEFAULT 0.0,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        balanceRemaining REAL NOT NULL DEFAULT 0.0,
                        reason TEXT NOT NULL DEFAULT 'Purchase Return',
                        status TEXT NOT NULL DEFAULT 'issued',
                        itemsSummary TEXT NOT NULL DEFAULT '',
                        notes TEXT,
                        clientReferenceId TEXT NOT NULL DEFAULT '',
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC',
                        serverDebitNoteId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_notes_companyId ON debit_notes (companyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_notes_supplierId ON debit_notes (supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_notes_purchaseId ON debit_notes (purchaseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_notes_debitNoteNumber ON debit_notes (debitNoteNumber)")

                // 7. Debit Note Items
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debit_note_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        debitNoteId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitPrice REAL NOT NULL,
                        gstRate REAL NOT NULL DEFAULT 0.0,
                        taxAmount REAL NOT NULL DEFAULT 0.0,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        hsnCode TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_note_items_debitNoteId ON debit_note_items (debitNoteId)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_orders_supplierId ON purchase_orders (supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_notes_supplierId ON debit_notes (supplierId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debit_notes_purchaseId ON debit_notes (purchaseId)")
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
                    .addMigrations(
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20
                    )
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
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
