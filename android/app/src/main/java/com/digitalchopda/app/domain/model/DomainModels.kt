package com.digitalchopda.app.domain.model

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
    val createdAt: Long = System.currentTimeMillis()
)

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
    val needsRenewal: Boolean get() = remainingPercentage <= 10f

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

data class InventoryItem(
    val id: Long = 0,
    val companyId: Long,
    val sku: String,
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
    val status: InvoiceStatus = InvoiceStatus.PAID,
    val paymentMode: PaymentMode = PaymentMode.UPI,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class LeadPriority {
    HOT,
    WARM,
    COLD,
    CONVERTED
}

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

data class Customer(
    val id: Long = 0,
    val companyId: Long,
    val name: String,
    val phone: String,
    val gstin: String = "",
    val balance: Double = 0.0,
    val address: String = ""
)

data class UserSession(
    val userId: String,
    val mobileNumber: String,
    val ownerName: String,
    val activeCompanyId: Long,
    val accessToken: String,
    val refreshToken: String,
    val isLoggedIn: Boolean = true
)
