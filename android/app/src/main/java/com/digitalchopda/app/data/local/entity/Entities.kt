package com.digitalchopda.app.data.local.entity

import androidx.room.Entity
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

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val itemsSummary: String, // e.g. "Cotton Fabric (x2), Silk Thread (x1)"
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val roundOff: Double,
    val grandTotal: Double,
    val status: String, // PAID, UNPAID, PARTIAL
    val paymentMode: String, // CASH, UPI, BANK
    val syncStatus: String, // PENDING_SYNC, SYNCED
    val idempotencyKey: String,
    val serverOrderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val customerId: Long? = null,
    val customerPhone: String = "",
    val customerName: String = "",
    val amount: Double,
    val isJama: Boolean, // true = You Got (Credit), false = You Gave (Debit)
    val paymentMode: String = "cash", // CASH, UPI, BANK
    val receiptNumber: String = "",
    val notes: String = "",
    val syncStatus: String = "PENDING_SYNC",
    val serverPaymentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
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
    val version: Int = 1, // Monotonic integer for delta rebasing
    val lastUpdated: Long = System.currentTimeMillis()
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

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val name: String,
    val phone: String,
    val gstin: String = "",
    val balance: Double = 0.0,
    val address: String = ""
)
