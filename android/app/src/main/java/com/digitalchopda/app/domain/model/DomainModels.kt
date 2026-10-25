package com.digitalchopda.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Company(
    val id: Long = 0,
    val firmName: String,
    val ownerName: String,
    val gstin: String = "",
    val phone: String = "",
    val address: String = "",
    val stateCode: String = "24", // Gujarat default
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

@Immutable
data class SequenceLease(
    val id: Long = 0,
    val companyId: Long,
    val prefix: String = "DC",
    val financialYear: String = "26-27",
    val rangeStart: Long = 1001L,
    val rangeEnd: Long = 1200L,
    val currentCursor: Long = 1001L,
    val deviceId: String = "DEVICE-01",
    val lastRenewedAt: Long = System.currentTimeMillis()
) {
    val totalCapacity: Long get() = rangeEnd - rangeStart + 1
    val remainingCapacity: Long get() = (rangeEnd - currentCursor + 1).coerceAtLeast(0)
    val remainingPercentage: Float get() = if (totalCapacity > 0) (remainingCapacity.toFloat() / totalCapacity.toFloat()) * 100f else 0f
    val needsRenewal: Boolean get() = remainingPercentage <= 20f

    /**
     * Strictly formats invoice numbers as Rule 46: PREFIX/FY/00001 (e.g., DC/26-27/01001)
     */
    fun formatInvoiceNumber(sequence: Long): String {
        return "$prefix/$financialYear/${sequence.toString().padStart(5, '0')}"
    }

    fun currentFormattedNumber(): String = formatInvoiceNumber(currentCursor)
}

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}

@Immutable
data class InventoryItem(
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
    val version: Int = 1, // Monotonic version for delta rebasing
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val status: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= minStockThreshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}

@Immutable
data class Product(
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
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class LineItem(
    val itemId: Long = 0,
    val sku: String = "",
    val title: String,
    val unitPrice: Double,
    val quantity: Int = 1,
    val gstRatePercent: Double = 18.0
) {
    val subtotal: Double get() = unitPrice * quantity
    val gstAmount: Double get() = subtotal * (gstRatePercent / 100.0)
    val total: Double get() = subtotal + gstAmount
}

enum class InvoiceStatus {
    UNPAID,
    PAID,
    PARTIAL
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK
}

enum class SyncStatus {
    PENDING_SYNC,
    SYNCED,
    FAILED
}

@Immutable
data class Invoice(
    val id: Long = 0,
    val companyId: Long,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<LineItem> = emptyList(),
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val roundOff: Double,
    val grandTotal: Double,
    val paidAmount: Double = 0.0,
    val balanceDue: Double = 0.0,
    val status: InvoiceStatus = InvoiceStatus.PAID,
    val paymentMode: PaymentMode = PaymentMode.UPI,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val serverOrderId: Long? = null,
    val idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class LeadPriority {
    HOT,
    WARM,
    COLD,
    CONVERTED
}

@Immutable
data class Lead(
    val id: Long = 0,
    val companyId: Long,
    val customerName: String,
    val businessName: String,
    val phone: String,
    val priority: LeadPriority = LeadPriority.HOT,
    val requirementDetails: String,
    val expectedDealValue: Double,
    val followUpTimestamp: Long = System.currentTimeMillis() + 86400000L, // +1 day
    val isClosed: Boolean = false
)

@Immutable
data class Customer(
    val id: Long = 0,
    val companyId: Long,
    val name: String,
    val phone: String,
    val gstin: String = "",
    val balance: Double = 0.0,
    val address: String = ""
)

@Immutable
data class UserSession(
    val userId: String,
    val mobileNumber: String,
    val ownerName: String,
    val activeCompanyId: Long,
    val accessToken: String,
    val refreshToken: String,
    val isLoggedIn: Boolean = true
)

@Immutable
data class PartyLedgerEntry(
    val id: String,
    val date: Long,
    val voucherType: String, // Opening Balance, Sales Invoice, Payment In, Payment Out, Credit Note
    val refNumber: String,
    val particulars: String,
    val debit: Double, // You Gave / Udhar (ઉધાર)
    val credit: Double, // You Got / Jama (જમા)
    val runningBalance: Double,
    val paymentMode: String = "—"
)

@Immutable
data class Party360Detail(
    val customer: com.digitalchopda.app.data.local.entity.CustomerEntity,
    val invoices: List<com.digitalchopda.app.data.local.entity.InvoiceEntity> = emptyList(),
    val quotations: List<com.digitalchopda.app.data.local.entity.QuotationEntity> = emptyList(),
    val payments: List<com.digitalchopda.app.data.local.entity.PaymentEntity> = emptyList(),
    val ledgerEntries: List<PartyLedgerEntry> = emptyList(),
    val totalBilled: Double = 0.0,
    val totalCollected: Double = 0.0,
    val netOutstanding: Double = 0.0
)

@Immutable
data class PurchaseItem(
    val id: Long = 0,
    val purchaseId: Long = 0,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val gstRate: Double = 0.0,
    val hsnCode: String? = null
) {
    val subtotal: Double get() = quantity * unitPrice
    val taxAmount: Double get() = subtotal * (gstRate / 100.0)
    val totalAmount: Double get() = subtotal + taxAmount
}

@Immutable
data class Purchase(
    val id: Long = 0,
    val companyId: Long,
    val supplierId: Long,
    val purchaseNumber: String,
    val vendorBillNumber: String = "",
    val supplierInvoiceNo: String = "",
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentStatus: String = "unpaid",
    val paymentMethod: String = "cash",
    val notes: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val items: List<PurchaseItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class StockAdjustment(
    val id: Long = 0,
    val companyId: Long,
    val productId: Long,
    val adjustmentType: String, // 'add', 'reduce', 'set'
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val reason: String = "Manual Adjustment",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
data class OrderRevision(
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


