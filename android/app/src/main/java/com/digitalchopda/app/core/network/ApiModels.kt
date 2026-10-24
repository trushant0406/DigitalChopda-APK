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

data class ApiCreateBusinessRequest(
    val name: String,
    @SerializedName("trade_name") val tradeName: String? = null,
    val gstin: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerializedName("invoice_prefix") val invoicePrefix: String? = "INV-"
)

data class ApiLoginResponse(
    val token: String?,
    val user: ApiUserDto?,
    val businesses: List<ApiBusinessDto>?,
    val activeBusiness: ApiBusinessDto?,
    val error: String?
)

data class ApiSendOtpRequest(
    val phone: String
)

data class ApiSendOtpResponse(
    val success: Boolean?,
    val message: String?,
    @SerializedName("dev_otp") val devOtp: String? = null
)

data class ApiVerifyOtpRequest(
    val phone: String,
    val otp: String
)

// --- Product / Catalog DTOs ---
data class ApiProductDto(
    val id: Long,
    val name: String,
    @SerializedName("default_price") val defaultPrice: Double,
    @SerializedName("hsn_code") val hsnCode: String?,
    @SerializedName("gst_rate") val gstRate: Double?,
    @SerializedName("business_id") val businessId: Long?,
    val barcode: String? = null,
    val sku: String? = null,
    val version: Int = 1,
    @SerializedName("is_tax_inclusive") val isTaxInclusive: Boolean = false,
    @SerializedName(value = "stock_quantity", alternate = ["stockQuantity"]) val stockQuantity: Double = 0.0,
    @SerializedName(value = "min_stock_alert", alternate = ["minStockAlert"]) val minStockAlert: Double? = 5.0,
    @SerializedName(value = "purchase_price", alternate = ["purchasePrice"]) val purchasePrice: Double? = 0.0
)

data class ApiCreateProductRequest(
    val name: String,
    @SerializedName("default_price") val defaultPrice: Double,
    @SerializedName("hsn_code") val hsnCode: String? = null,
    @SerializedName("gst_rate") val gstRate: Double = 18.0,
    val barcode: String? = null,
    val sku: String? = null,
    @SerializedName("is_tax_inclusive") val isTaxInclusive: Boolean = false,
    @SerializedName("stock_quantity") val stockQuantity: Double = 0.0,
    @SerializedName("min_stock_alert") val minStockAlert: Double = 5.0,
    @SerializedName("purchase_price") val purchasePrice: Double = 0.0
)

// --- Sequence Lease DTOs (Offline Billing Engine) ---
data class ApiLeaseRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("requested_block_size") val requestedBlockSize: Int = 200
)

data class ApiLeaseResponse(
    val ok: Boolean? = true,
    val success: Boolean? = true,
    val prefix: String? = "DC",
    @SerializedName("financial_year") val financialYear: String? = "26-27",
    @SerializedName("range_start") val rangeStart: Long? = 1001L,
    @SerializedName("range_end") val rangeEnd: Long? = 1200L,
    @SerializedName("lease_id") val leaseId: Long? = null
)

// --- Inventory Rebase & Delta Sync DTOs ---
data class ApiStockSyncDeltaRequest(
    @SerializedName("item_id") val itemId: Long,
    @SerializedName("delta_quantity") val deltaQuantity: Int,
    @SerializedName("base_version") val baseVersion: Int
)

data class ApiStockRebaseResponse(
    val ok: Boolean? = true,
    val success: Boolean? = true,
    @SerializedName("item_id") val itemId: Long? = null,
    @SerializedName("updated_quantity") val updatedQuantity: Int? = null,
    @SerializedName("updated_version") val updatedVersion: Int? = null,
    @SerializedName("rebased_cleanly") val rebasedCleanly: Boolean? = true
)

// --- Party / Customer / Contact DTOs ---
data class ApiContactDto(
    val id: Long,
    val name: String?,
    val phone: String,
    val city: String? = null,
    val gstin: String? = null,
    val pan: String? = null,
    val state: String? = null,
    @SerializedName("billing_address") val billingAddress: String? = null,
    @SerializedName("shipping_address") val shippingAddress: String? = null,
    val balance: Double? = null,
    val outstanding: Double? = null,
    @SerializedName("party_type") val partyType: String? = "customer",
    @SerializedName("party_tag") val partyTag: String? = "Active Party",
    @SerializedName("credit_limit") val creditLimit: Double? = 0.0,
    @SerializedName("opening_balance") val openingBalance: Double? = 0.0,
    @SerializedName("opening_balance_type") val openingBalanceType: String? = "to_receive",
    @SerializedName("opening_balance_date") val openingBalanceDate: String? = null,
    val email: String? = null,
    @SerializedName("total_sales") val totalSales: Double? = null,
    @SerializedName("total_collected") val totalCollected: Double? = null
)

data class ApiCreateContactRequest(
    val name: String,
    val phone: String,
    @SerializedName("party_type") val partyType: String = "customer",
    @SerializedName("party_tag") val partyTag: String = "Active Party",
    @SerializedName("opening_balance") val openingBalance: Double = 0.0,
    @SerializedName("opening_balance_type") val openingBalanceType: String = "to_receive",
    @SerializedName("opening_balance_date") val openingBalanceDate: String? = null,
    @SerializedName("credit_limit") val creditLimit: Double = 0.0,
    val gstin: String? = null,
    val pan: String? = null,
    @SerializedName("billing_address") val billingAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val email: String? = null,
    @SerializedName("client_reference_id") val clientReferenceId: String? = null
)

// --- Order / Invoicing DTOs ---
data class ApiOrderItemDto(
    val id: Long? = null,
    @SerializedName("order_id") val orderId: Long? = null,
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("gst_rate") val gstRate: Double? = 0.0,
    @SerializedName("hsn_code") val hsnCode: String? = null
)

data class ApiOrderItemRequest(
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("gst_rate") val gstRate: Double,
    val rate: Double = unitPrice,
    val barcode: String? = null,
    val sku: String? = null,
    @SerializedName("is_tax_inclusive") val isTaxInclusive: Boolean = false,
    @SerializedName("discount_percent") val discountPercent: Double = 0.0,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0
)

// --- Quotation DTOs ---
data class ApiQuotationItemRequest(
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    val rate: Double,
    @SerializedName("gst_rate") val gstRate: Double = 18.0,
    val unit: String = "PCS",
    @SerializedName("hsn_code") val hsnCode: String? = null
)

data class ApiQuotationRequest(
    @SerializedName("contact_id") val contactId: Long? = null,
    val phone: String,
    val name: String,
    val city: String? = null,
    val gstin: String? = null,
    val pan: String? = null,
    @SerializedName("billing_address") val billingAddress: String? = null,
    @SerializedName("shipping_address") val shippingAddress: String? = null,
    val state: String? = null,
    val items: List<ApiQuotationItemRequest>,
    @SerializedName("terms_and_conditions") val termsAndConditions: String? = null
)

data class ApiQuotationDto(
    val id: Long,
    @SerializedName("business_id") val businessId: Long? = null,
    @SerializedName("quotation_number") val quotationNumber: String? = null,
    @SerializedName("bill_to_name") val billToName: String? = null,
    @SerializedName("bill_to_phone") val billToPhone: String? = null,
    @SerializedName("bill_to_mobile") val billToMobile: String? = null,
    @SerializedName("subtotal") val subtotal: Double? = null,
    @SerializedName("tax_amount") val taxAmount: Double? = null,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("items") val items: List<ApiQuotationItemRequest>? = emptyList()
)

data class ApiCreateOrderRequest(
    val phone: String,
    val name: String,
    val city: String? = null,
    val items: List<ApiOrderItemRequest>,
    @SerializedName("payment_status") val paymentStatus: String, // "paid" or "pending"
    @SerializedName("payment_method") val paymentMethod: String? = null, // "cash", "upi", "bank"
    @SerializedName("paid_amount") val paidAmount: Double? = null,
    @SerializedName("create_invoice") val createInvoice: Boolean = true,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    val notes: String? = null,
    val gstin: String? = null,
    val state: String? = null,
    @SerializedName("place_of_supply") val placeOfSupply: String? = null,
    @SerializedName("tax_mode") val taxMode: String? = null, // "split" or "igst"
    @SerializedName("round_off") val roundOff: Double? = null,
    @SerializedName("billing_address") val billingAddress: String? = null,
    @SerializedName("client_uuid") val clientUuid: String? = null,
    @SerializedName("idempotency_key") val idempotencyKey: String? = null,
    @SerializedName("discount_percent") val discountPercent: Double? = 0.0,
    @SerializedName("discount_amount") val discountAmount: Double? = 0.0,
    @SerializedName("client_reference_id") val clientReferenceId: String? = null
)

data class ApiOrderDto(
    val id: Long,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("invoice_number") val invoiceNumber: String?,
    @SerializedName("grand_total") val grandTotal: Double?,
    @SerializedName("total_amount") val totalAmount: Double? = null,
    @SerializedName("tax_amount") val taxAmount: Double? = null,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("items_summary") val itemsSummary: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("invoice_date") val invoiceDate: String? = null,
    @SerializedName("business_id") val businessId: Long? = null,
    @SerializedName("received_amount") val receivedAmount: Double? = null,
    @SerializedName("balance_due") val balanceDue: Double? = null,
    @SerializedName("fulfillment_status") val fulfillmentStatus: String? = "not_ready",
    @SerializedName("items") val items: List<ApiOrderItemDto>? = emptyList()
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
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("client_reference_id") val clientReferenceId: String? = null
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
    val id: String?,
    val type: String?, // "order", "payment", "credit_note", "opening_balance"
    val date: String?,
    val reference: String?,
    @SerializedName("sr_no") val srNo: String? = null,
    val description: String?,
    @SerializedName("particulars") val particulars: String? = null,
    @SerializedName("voucher") val voucher: String? = null,
    @SerializedName("payment_mode") val paymentMode: String? = null,
    val debit: Double?, // You Gave
    val credit: Double?, // You Got
    val balance: Double?,
    @SerializedName("running_balance") val runningBalance: Double? = null,
    val status: String? = null
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

// --- Credit Note / Sales Return DTOs ---
data class ApiCreditNoteItemDto(
    val id: Long? = null,
    @SerializedName("product_name") val productName: String,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("gst_rate") val gstRate: Double? = 0.0,
    @SerializedName("hsn_code") val hsnCode: String? = null
)

data class ApiReturnableItemsResponse(
    @SerializedName("is_fully_returned") val isFullyReturned: Boolean = false,
    @SerializedName("total_billed_qty") val totalBilledQty: Double = 0.0,
    @SerializedName("total_returned_qty") val totalReturnedQty: Double = 0.0,
    val items: List<ApiCreditNoteItemDto> = emptyList()
)

data class ApiCreditNoteDto(
    val id: Long,
    @SerializedName("business_id") val businessId: Long? = null,
    @SerializedName("credit_note_number") val creditNoteNumber: String,
    @SerializedName("order_id") val orderId: Long? = null,
    @SerializedName("contact_id") val contactId: Long? = null,
    @SerializedName("contact_name") val contactName: String? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerializedName("credit_note_date") val creditNoteDate: String? = null,
    @SerializedName("original_invoice_no") val originalInvoiceNo: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val subtotal: Double? = 0.0,
    @SerializedName("tax_amount") val taxAmount: Double? = 0.0,
    @SerializedName("total_amount") val totalAmount: Double? = 0.0,
    @SerializedName("items_summary") val itemsSummary: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ApiCreateCreditNoteRequest(
    @SerializedName("contact_id") val contactId: Long,
    @SerializedName("order_id") val orderId: Long? = null,
    @SerializedName("credit_note_number") val creditNoteNumber: String? = null,
    @SerializedName("credit_note_date") val creditNoteDate: String? = null,
    val reason: String = "Sales Return",
    val notes: String? = null,
    val items: List<ApiCreditNoteItemDto>,
    @SerializedName("client_reference_id") val clientReferenceId: String? = null
)

// --- Direct Khata Transaction DTOs ---
data class ApiCreateKhataTransactionRequest(
    @SerializedName("contact_id") val contactId: Long,
    @SerializedName("entry_type") val entryType: String, // "debit" or "credit"
    val amount: Double,
    @SerializedName("transaction_date") val transactionDate: String? = null,
    val particulars: String? = null,
    @SerializedName("payment_mode") val paymentMode: String? = "cash",
    @SerializedName("reference_no") val referenceNo: String? = null,
    val notes: String? = null,
    @SerializedName("client_reference_id") val clientReferenceId: String? = null
)

data class ApiKhataTransactionDto(
    val id: Long,
    @SerializedName("business_id") val businessId: Long?,
    @SerializedName("contact_id") val contactId: Long,
    @SerializedName("entry_type") val entryType: String,
    val amount: Double,
    @SerializedName("transaction_date") val transactionDate: String?,
    val particulars: String?,
    @SerializedName("payment_mode") val paymentMode: String?,
    @SerializedName("reference_no") val referenceNo: String?,
    val notes: String?,
    @SerializedName("client_reference_id") val clientReferenceId: String?
)

// --- Dashboard & Product Diagnostic DTOs ---
data class ApiDashboardSummaryResponse(
    @SerializedName("totalParties") val totalParties: Int = 0,
    @SerializedName("customersCount") val customersCount: Int = 0,
    @SerializedName("suppliersCount") val suppliersCount: Int = 0,
    @SerializedName("totalReceivable") val totalReceivable: Double = 0.0,
    @SerializedName("totalPayable") val totalPayable: Double = 0.0,
    @SerializedName("netOutstanding") val netOutstanding: Double = 0.0,
    @SerializedName("dueToday") val dueToday: Int = 0,
    @SerializedName("overdue") val overdue: Int = 0
)

data class ApiOrdersSummaryResponse(
    @SerializedName("paymentPending") val paymentPending: Int = 0,
    @SerializedName("readyToDispatch") val readyToDispatch: Int = 0,
    @SerializedName("notDispatched") val notDispatched: Int = 0,
    @SerializedName("dispatched") val dispatched: Int = 0,
    @SerializedName("delivered") val delivered: Int = 0,
    @SerializedName("totalOrders") val totalOrders: Int = 0,
    @SerializedName("pendingAmount") val pendingAmount: Double = 0.0
)

data class ApiSettingsStatusResponse(
    @SerializedName("pushConfigured") val pushConfigured: Boolean = false,
    @SerializedName("whatsappConfigured") val whatsappConfigured: Boolean = false,
    @SerializedName("whatsappWebStatus") val whatsappWebStatus: String? = null,
    @SerializedName("whatsappWebQr") val whatsappWebQr: String? = null,
    @SerializedName("businessId") val businessId: Long? = null
)

// --- GST & Statutory Reporting DTOs ---
data class ApiGstPeriodDto(
    @SerializedName("fp") val fp: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("fromDate") val fromDate: String? = null,
    @SerializedName("toDate") val toDate: String? = null
)

data class ApiGstOutwardClassificationDto(
    @SerializedName("b2bCount") val b2bCount: Int = 0,
    @SerializedName("b2bTaxable") val b2bTaxable: Double = 0.0,
    @SerializedName("b2clCount") val b2clCount: Int = 0,
    @SerializedName("b2csCount") val b2csCount: Int = 0,
    @SerializedName("cdnrCount") val cdnrCount: Int = 0,
    @SerializedName("cdnurCount") val cdnurCount: Int = 0
)

data class ApiGstOutwardSuppliesDto(
    @SerializedName("invoicesCount") val invoicesCount: Int = 0,
    @SerializedName("taxableTurnover") val taxableTurnover: Double = 0.0,
    @SerializedName("totalInvoiceValue") val totalInvoiceValue: Double = 0.0,
    @SerializedName("totalOutwardTax") val totalOutwardTax: Double = 0.0,
    @SerializedName("classification") val classification: ApiGstOutwardClassificationDto? = null
)

data class ApiGstEligibleItcDto(
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("igst") val igst: Double = 0.0,
    @SerializedName("cgst") val cgst: Double = 0.0,
    @SerializedName("sgst") val sgst: Double = 0.0
)

data class ApiGstInwardSuppliesDto(
    @SerializedName("purchasesCount") val purchasesCount: Int = 0,
    @SerializedName("taxableValue") val taxableValue: Double = 0.0,
    @SerializedName("eligibleItc") val eligibleItc: ApiGstEligibleItcDto? = null
)

data class ApiGstCashPayableDto(
    @SerializedName("total") val total: Double = 0.0,
    @SerializedName("igst") val igst: Double = 0.0,
    @SerializedName("cgst") val cgst: Double = 0.0,
    @SerializedName("sgst") val sgst: Double = 0.0
)

data class ApiGstRule88aSetoffDto(
    @SerializedName("cashPayable") val cashPayable: ApiGstCashPayableDto? = null
)

data class ApiGstSummaryResponse(
    @SerializedName("period") val period: ApiGstPeriodDto? = null,
    @SerializedName("outwardSupplies") val outwardSupplies: ApiGstOutwardSuppliesDto? = null,
    @SerializedName("inwardSupplies") val inwardSupplies: ApiGstInwardSuppliesDto? = null,
    @SerializedName("rule88aSetoff") val rule88aSetoff: ApiGstRule88aSetoffDto? = null,
    @SerializedName(value = "totalInwardTaxLiability", alternate = ["total_inward_tax_liability", "totalInwardTax"]) val totalInwardTaxLiability: Double = 0.0,
    @SerializedName(value = "totalOutwardTaxLiability", alternate = ["total_outward_tax_liability", "totalOutwardTax"]) val totalOutwardTaxLiability: Double = 0.0,
    @SerializedName(value = "inputTaxCredit", alternate = ["input_tax_credit", "itc"]) val inputTaxCredit: Double = 0.0,
    @SerializedName(value = "netGstPayable", alternate = ["net_gst_payable", "netPayable"]) val netGstPayable: Double = 0.0,
    @SerializedName(value = "gstr1Status", alternate = ["gstr1_status"]) val gstr1Status: String? = null,
    @SerializedName(value = "gstr3bStatus", alternate = ["gstr3b_status"]) val gstr3bStatus: String? = null,
    @SerializedName(value = "gstr2bStatus", alternate = ["gstr2b_status"]) val gstr2bStatus: String? = null,
    @SerializedName(value = "b2bCount", alternate = ["b2b_count"]) val b2bCount: Int = 0,
    @SerializedName(value = "b2cCount", alternate = ["b2c_count"]) val b2cCount: Int = 0,
    @SerializedName(value = "totalInvoices", alternate = ["total_invoices", "invoiceCount"]) val totalInvoices: Int = 0
) {
    val displayPeriod: String
        get() = period?.label ?: period?.fp ?: "Current Period"

    val effectiveOutwardTax: Double
        get() = if ((outwardSupplies?.totalOutwardTax ?: 0.0) > 0.0) outwardSupplies?.totalOutwardTax ?: 0.0 else totalOutwardTaxLiability

    val effectiveItc: Double
        get() = if ((inwardSupplies?.eligibleItc?.total ?: 0.0) > 0.0) inwardSupplies?.eligibleItc?.total ?: 0.0 else inputTaxCredit

    val effectiveNetPayable: Double
        get() = if ((rule88aSetoff?.cashPayable?.total ?: 0.0) > 0.0) rule88aSetoff?.cashPayable?.total ?: 0.0 else netGstPayable
}

// --- Purchases (Inward Supplier Bills) DTOs ---
data class ApiPurchaseItemRequest(
    @SerializedName("product_name") val productName: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("gst_rate") val gstRate: Double = 0.0,
    @SerializedName("hsn_code") val hsnCode: String? = null
)

data class ApiCreatePurchaseRequest(
    @SerializedName("contact_id") val contactId: Long,
    @SerializedName("purchase_number") val purchaseNumber: String? = null,
    @SerializedName("supplier_invoice_no") val supplierInvoiceNo: String? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("payment_status") val paymentStatus: String = "unpaid",
    @SerializedName("payment_method") val paymentMethod: String = "cash",
    @SerializedName("paid_amount") val paidAmount: Double? = 0.0,
    @SerializedName("items") val items: List<ApiPurchaseItemRequest>,
    @SerializedName("notes") val notes: String = "",
    @SerializedName(value = "client_reference_id", alternate = ["idempotency_key"]) val clientReferenceId: String? = null
)

data class ApiPurchaseItemDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("purchase_id") val purchaseId: Long = 0L,
    @SerializedName("product_name") val productName: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("gst_rate") val gstRate: Double = 0.0,
    @SerializedName("hsn_code") val hsnCode: String? = null
)

data class ApiPurchaseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("business_id") val businessId: Long = 1L,
    @SerializedName("contact_id") val contactId: Long,
    @SerializedName("purchase_number") val purchaseNumber: String,
    @SerializedName("supplier_invoice_no") val supplierInvoiceNo: String? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("payment_status") val paymentStatus: String = "unpaid",
    @SerializedName("payment_method") val paymentMethod: String? = "cash",
    @SerializedName("paid_amount") val paidAmount: Double? = 0.0,
    @SerializedName("subtotal") val subtotal: Double = 0.0,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("items") val items: List<ApiPurchaseItemDto> = emptyList()
)

// --- Inventory & Stock Adjustments DTOs ---
data class ApiStockAdjustmentRequest(
    @SerializedName("product_id") val productId: Long,
    @SerializedName("adjustment_type") val adjustmentType: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("reason") val reason: String = "Manual Adjustment",
    @SerializedName("notes") val notes: String? = null
)

data class ApiStockAdjustmentDto(
    @SerializedName("id") val id: Long,
    @SerializedName("business_id") val businessId: Long = 1L,
    @SerializedName("product_id") val productId: Long,
    @SerializedName("adjustment_type") val adjustmentType: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("previous_stock") val previousStock: Double = 0.0,
    @SerializedName("new_stock") val newStock: Double = 0.0,
    @SerializedName("reason") val reason: String = "Manual Adjustment",
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ApiStockAdjustmentResponse(
    @SerializedName("ok") val ok: Boolean = true,
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("product") val product: ApiProductDto? = null,
    @SerializedName("adjustment") val adjustment: ApiStockAdjustmentDto? = null
)

// --- Pre-Filing Tax Anomalies DTOs ---
data class ApiPreFilingAnomalyDto(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String,
    @SerializedName("severity") val severity: String, // CRITICAL, WARNING, INFO
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("entityType") val entityType: String? = null,
    @SerializedName("entityId") val entityId: Long? = null,
    @SerializedName("refNumber") val refNumber: String? = null,
    @SerializedName("suggestion") val suggestion: String? = null
)

