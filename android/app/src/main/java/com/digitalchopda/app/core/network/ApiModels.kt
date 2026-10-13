package com.digitalchopda.app.core.network

import com.google.gson.annotations.SerializedName

// --- Authentication DTOs ---
data class ApiLoginRequest(
    val username: String,
    val password: String?
)

data class ApiUserDto(
    val id: Long,
    val username: String,
    val name: String?,
    val role: String?
)

data class ApiBusinessDto(
    val id: Long,
    val name: String,
    @SerializedName("trade_name") val tradeName: String?,
    @SerializedName("invoice_prefix") val invoicePrefix: String?,
    val status: String?,
    @SerializedName("user_role") val userRole: String?
)

data class ApiLoginResponse(
    val token: String?,
    val user: ApiUserDto?,
    val businesses: List<ApiBusinessDto>?,
    val activeBusiness: ApiBusinessDto?,
    val error: String?
)

// --- Product / Catalog DTOs ---
data class ApiProductDto(
    val id: Long,
    val name: String,
    @SerializedName("default_price") val defaultPrice: Double,
    @SerializedName("hsn_code") val hsnCode: String?,
    @SerializedName("gst_rate") val gstRate: Double?,
    @SerializedName("business_id") val businessId: Long?
)

data class ApiCreateProductRequest(
    val name: String,
    @SerializedName("default_price") val defaultPrice: Double,
    @SerializedName("hsn_code") val hsnCode: String? = null,
    @SerializedName("gst_rate") val gstRate: Double = 18.0
)

// --- Party / Customer / Contact DTOs ---
data class ApiContactDto(
    val id: Long,
    val name: String?,
    val phone: String,
    val city: String?,
    val gstin: String?,
    val state: String?,
    @SerializedName("billing_address") val billingAddress: String?,
    val balance: Double?
)

// --- Order / Invoicing DTOs ---
data class ApiOrderItemRequest(
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("gst_rate") val gstRate: Double
)

data class ApiCreateOrderRequest(
    val phone: String,
    val name: String,
    val city: String? = null,
    val items: List<ApiOrderItemRequest>,
    @SerializedName("payment_status") val paymentStatus: String, // "paid" or "pending"
    @SerializedName("payment_method") val paymentMethod: String? = null, // "cash", "upi", "bank"
    @SerializedName("create_invoice") val createInvoice: Boolean = true,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    val notes: String? = null,
    val gstin: String? = null,
    val state: String? = null,
    @SerializedName("place_of_supply") val placeOfSupply: String? = null,
    @SerializedName("tax_mode") val taxMode: String? = null, // "split" or "igst"
    @SerializedName("round_off") val roundOff: Double? = null,
    @SerializedName("billing_address") val billingAddress: String? = null
)

data class ApiOrderDto(
    val id: Long,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("invoice_number") val invoiceNumber: String?,
    @SerializedName("grand_total") val grandTotal: Double?,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("items_summary") val itemsSummary: String? = null,
    @SerializedName("created_at") val createdAt: String?
)

data class ApiNextNumberResponse(
    val prefix: String?,
    val nextNumber: Long,
    val padding: Int,
    val suggestedNumber: String
)

data class ApiCreditSummaryResponse(
    @SerializedName("total_outstanding") val totalOutstanding: Double?,
    @SerializedName("total_customers") val totalCustomers: Int?
)

// --- Payment-In / Credit Ledger DTOs ---
data class ApiPaymentInRequest(
    @SerializedName("contact_id") val contactId: Long? = null,
    val phone: String? = null,
    val name: String? = null,
    val amount: Double,
    @SerializedName("payment_mode") val paymentMode: String = "cash", // "cash", "upi", "bank"
    @SerializedName("order_id") val orderId: Long? = null,
    val notes: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null
)

data class ApiPaymentInResponse(
    val id: Long?,
    val amount: Double?,
    @SerializedName("receipt_number") val receiptNumber: String?,
    @SerializedName("contact_id") val contactId: Long?,
    @SerializedName("order_id") val orderId: Long?,
    @SerializedName("payment_mode") val paymentMode: String?,
    @SerializedName("paid_at") val paidAt: String?,
    val notes: String?
)

data class ApiLedgerEntryDto(
    val id: Long?,
    val type: String?, // "order", "payment", "credit_note", "opening_balance"
    val date: String?,
    val reference: String?,
    val description: String?,
    val debit: Double?, // You Gave
    val credit: Double?, // You Got
    val balance: Double?
)

data class ApiCustomerLedgerResponse(
    val customer: ApiContactDto?,
    val entries: List<ApiLedgerEntryDto>?,
    @SerializedName("total_debit") val totalDebit: Double?,
    @SerializedName("total_credit") val totalCredit: Double?,
    @SerializedName("net_balance") val netBalance: Double?
)

// --- WhatsApp Dispatch Response ---
data class ApiSendWhatsAppResponse(
    val ok: Boolean?,
    val message: String?,
    val status: String?,
    val error: String?
)
