package com.digitalchopda.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firmName: String,
    val ownerName: String,
    val gstin: String = "",
    val phone: String = "",
    val address: String = "",
    val stateCode: String = "24",
    val financialYear: String = "26-27",
    val invoicePrefix: String = "DC",
    val currencySymbol: String = "₹",
    val isActive: Boolean = false,
    val upiVpa: String = "",
    val bankName: String = "",
    val bankAccountNo: String = "",
    val bankIfsc: String = "",
    val bankBranch: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sequence_leases")
data class LeaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val prefix: String = "DC",
    val financialYear: String = "26-27",
    val rangeStart: Long = 1001L,
    val rangeEnd: Long = 1200L,
    val currentCursor: Long = 1001L,
    val deviceId: String = "DEVICE-01",
    val lastRenewedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["customerPhone"]),
        Index(value = ["invoiceNumber"]),
        Index(value = ["syncStatus"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val itemsSummary: String, // e.g. "Cotton Fabric (x2), Silk Thread (x1)"
    val itemsJson: String = "",
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val roundOff: Double,
    val grandTotal: Double,
    val paidAmount: Double = 0.0,
    val status: String, // PAID, UNPAID, PARTIAL
    val paymentMode: String, // CASH, UPI, BANK
    val syncStatus: String, // PENDING_SYNC, SYNCED
    val idempotencyKey: String,
    val serverOrderId: Long? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val syncAttempts: Int = 0,
    val fulfillmentStatus: String = "not_ready", // not_ready, ready, dispatched, delivered
    val whatsappSentCount: Int = 0,
    val lastWhatsappSentAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val balanceDue: Double
        get() = if (status.equals("PAID", ignoreCase = true)) 0.0 else maxOf(0.0, grandTotal - paidAmount)
}

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["customerPhone"]),
        Index(value = ["syncStatus"]),
        Index(value = ["invoiceId"]),
        Index(value = ["orderId"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val customerId: Long? = null,
    val customerPhone: String = "",
    val customerName: String = "",
    val invoiceId: Long? = null,
    val orderId: Long? = null,
    val invoiceNumber: String = "",
    val amount: Double,
    val isJama: Boolean, // true = You Got (Credit), false = You Gave (Debit)
    val paymentMode: String = "cash", // CASH, UPI, BANK
    val receiptNumber: String = "",
    val notes: String = "",
    val syncStatus: String = "PENDING_SYNC",
    val serverPaymentId: Long? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val serverContactId: Long? = null,
    val isSettlement: Boolean = false,
    val isVoid: Boolean = false,
    val voidReason: String? = null,
    val voidedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["sku"]),
        Index(value = ["barcode"]),
        Index(value = ["companyId", "itemName"])
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val sku: String,
    val barcode: String = "",
    val itemName: String,
    val packSize: String = "1 Unit",
    val quantity: Int = 0,
    val sellingPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val gstRatePercent: Double = 18.0,
    val minStockThreshold: Int = 5,
    val version: Int = 1, // Monotonic integer for delta rebasing
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["companyId", "name"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val name: String,
    val defaultPrice: Double,
    val hsnCode: String? = null,
    val gstRate: Double = 18.0,
    val unit: String = "PCS",
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val purchasePrice: Double = 0.0,
    val sku: String? = null,
    val barcode: String? = null,
    val version: Int = 1,
    val syncStatus: String = "SYNCED",
    val serverProductId: Long? = null,
    val primaryImageUrl: String? = null,
    val wholesalePrice: Double? = null,
    val wholesaleQuantity: Double = 1.0,
    val batchNumber: String? = null,
    val expiryDate: String? = null,
    val showInOnlineStore: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val customerName: String,
    val businessName: String,
    val phone: String,
    val priority: String, // HOT, WARM, COLD, CONVERTED
    val requirementDetails: String,
    val expectedDealValue: Double,
    val followUpTimestamp: Long,
    val isClosed: Boolean = false
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val name: String,
    val phone: String,
    val gstin: String = "",
    val balance: Double = 0.0,
    val address: String = "",
    val companyName: String = "",
    val city: String = "",
    val state: String = "",
    val pan: String = "",
    val creditLimit: Double = 0.0,
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "to_receive", // to_receive (Debit), to_pay (Credit)
    val openingBalanceDate: Long? = null,
    val leadPriority: String = "WARM", // HOT, WARM, COLD, CONVERTED
    val expectedDealValue: Double = 0.0,
    val leadNotes: String = "",
    val nextFollowUpDate: Long? = null,
    val partyType: String = "customer", // customer / supplier
    val partyTag: String = "Active Party",
    val serverContactId: Long? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val serverOutstanding: Double = 0.0
)

@Entity(
    tableName = "quotations",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["customerPhone"]),
        Index(value = ["syncStatus"]),
        Index(value = ["serverQuotationId"])
    ]
)
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val quotationNumber: String,
    val customerName: String,
    val customerPhone: String,
    val itemsSummary: String,
    val itemsJson: String = "",
    val subtotal: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val validUntil: Long = System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000,
    val status: String = "PENDING", // PENDING, CONVERTED, EXPIRED
    val syncStatus: String = "PENDING_SYNC", // PENDING_SYNC, SYNCED
    val serverQuotationId: Long? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val name: String,
    val phone: String,
    val gstin: String = "",
    val openingBalance: Double = 0.0,
    val balanceToPay: Double = 0.0
)

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["supplierId"]),
        Index(value = ["purchaseNumber"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val supplierId: Long,
    val purchaseNumber: String,
    val vendorBillNumber: String = "",
    val supplierInvoiceNo: String = "",
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentStatus: String = "unpaid", // unpaid, paid, partially_paid
    val paymentMethod: String = "cash",
    val notes: String = "",
    val syncStatus: String = "PENDING_SYNC",
    val serverPurchaseId: Long? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val purchaseDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    indices = [
        Index(value = ["purchaseId"])
    ]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val gstRate: Double = 0.0,
    val hsnCode: String? = null
)

@Entity(
    tableName = "stock_adjustments",
    indices = [
        Index(value = ["companyId", "productId"]),
        Index(value = ["companyId", "createdAt"])
    ]
)
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val productId: Long,
    val adjustmentType: String, // 'add', 'reduce', 'set'
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val reason: String = "Manual Adjustment",
    val notes: String? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val syncStatus: String = "PENDING_SYNC",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_revisions",
    indices = [
        Index(value = ["companyId", "orderId"]),
        Index(value = ["orderId", "revisionNumber"], unique = true)
    ]
)
data class OrderRevisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val orderId: Long,
    val revisionNumber: Int = 1,
    val revisionDate: Long = System.currentTimeMillis(),
    val previousTotal: Double,
    val newTotal: Double,
    val deltaAmount: Double = 0.0,
    val reason: String? = null,
    val itemsSnapshotJson: String = "[]",
    val newItemsSnapshotJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "credit_notes",
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["customerPhone"]),
        Index(value = ["creditNoteNumber"])
    ]
)
data class CreditNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val creditNoteNumber: String,
    val customerName: String,
    val customerPhone: String,
    val orderId: Long? = null,
    val originalInvoiceNumber: String = "",
    val reason: String = "Sales Return", // Sales Return, Damaged / Defective Goods, etc.
    val itemsSummary: String = "",
    val itemsJson: String = "",
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val status: String = "ACTIVE", // ACTIVE, VOIDED
    val serverCreditNoteId: Long? = null,
    val clientReferenceId: String = java.util.UUID.randomUUID().toString(),
    val syncStatus: String = "PENDING_SYNC",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "outbox_sync_queue",
    indices = [
        Index(value = ["companyId", "status"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    val entityType: String, // 'contact', 'invoice', 'payment', 'stock_adjustment'
    val actionType: String, // 'INSERT', 'UPDATE', 'DELETE'
    val payloadJson: String,
    val status: String = "PENDING", // 'PENDING', 'SYNCING', 'SYNCED', 'FAILED'
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)


