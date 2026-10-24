package com.digitalchopda.app.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.InventoryRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.InventoryItem
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.SyncStatus
import com.digitalchopda.app.domain.usecase.CreateInvoiceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

import com.digitalchopda.app.core.document.DocumentItemParser
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.data.repository.ProductRepository
import com.digitalchopda.app.data.repository.QuotationRepository
import com.digitalchopda.app.domain.model.Product
import kotlinx.coroutines.flow.flowOf

class InvoiceViewModel(
    private val billingRepository: BillingRepository,
    private val companyRepository: CompanyRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository? = null,
    private val quotationRepository: QuotationRepository? = null
) : ViewModel() {

    private val createInvoiceUseCase = CreateInvoiceUseCase()

    private val _activeCompany = MutableStateFlow(
        Company(
            id = 1L,
            firmName = "Sharma Traders",
            ownerName = "Amit Sharma",
            gstin = "24AAACS1429B1Z8",
            phone = "+91 98765 43210",
            address = "Surat, Gujarat",
            stateCode = "24",
            isActive = true
        )
    )
    val activeCompany: StateFlow<Company> = _activeCompany.asStateFlow()

    private val _invoiceNumber = MutableStateFlow("DC/26-27/01001")
    val invoiceNumber: StateFlow<String> = _invoiceNumber.asStateFlow()

    private val _customerName = MutableStateFlow("Walk-in Customer")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _customerGstin = MutableStateFlow("")
    val customerGstin: StateFlow<String> = _customerGstin.asStateFlow()

    private val _selectedStatus = MutableStateFlow(InvoiceStatus.PAID)
    val selectedStatus: StateFlow<InvoiceStatus> = _selectedStatus.asStateFlow()

    private val _paidAmount = MutableStateFlow("")
    val paidAmount: StateFlow<String> = _paidAmount.asStateFlow()

    private val _selectedPaymentMode = MutableStateFlow(PaymentMode.UPI)
    val selectedPaymentMode: StateFlow<PaymentMode> = _selectedPaymentMode.asStateFlow()

    private val _lineItems = MutableStateFlow<List<LineItem>>(emptyList())
    val lineItems: StateFlow<List<LineItem>> = _lineItems.asStateFlow()

    fun isInterstateSupply(
        sellerGstin: String,
        sellerStateCode: String,
        buyerGstin: String,
        buyerState: String?
    ): Boolean {
        if (buyerGstin.length >= 2) {
            val buyerStateCode = buyerGstin.substring(0, 2)
            return buyerStateCode != sellerStateCode
        }
        if (!buyerState.isNullOrBlank()) {
            return !buyerState.equals(sellerStateCode, ignoreCase = true) &&
                   !buyerState.equals("Gujarat", ignoreCase = true)
        }
        return false
    }

    // Auto-detect interstate commerce (IGST vs CGST+SGST) based on customer GSTIN
    val isInterstate: StateFlow<Boolean> = combine(_activeCompany, _customerGstin) { company, gstin ->
        if (gstin.length >= 2) {
            val customerStateCode = gstin.substring(0, 2)
            customerStateCode != company.stateCode
        } else {
            false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Reactive Tax Breakdown
    val taxCalculation: StateFlow<CreateInvoiceUseCase.CalculationResult> = combine(
        _lineItems,
        isInterstate
    ) { items, interstate ->
        createInvoiceUseCase.calculateTaxes(items, isInterState = interstate)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        createInvoiceUseCase.calculateTaxes(_lineItems.value)
    )

    // Stock items from local Room for instant search-as-you-type autocomplete
    @OptIn(ExperimentalCoroutinesApi::class)
    val stockItems: StateFlow<List<InventoryItem>> = _activeCompany
        .flatMapLatest { company ->
            inventoryRepository.getItemsForCompany(company.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products from catalog for instant search-as-you-type autocomplete
    @OptIn(ExperimentalCoroutinesApi::class)
    val catalogProducts: StateFlow<List<Product>> = _activeCompany
        .flatMapLatest { company ->
            productRepository?.getProducts(company.id) ?: flowOf<List<Product>>(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer search for customer autofill
    @OptIn(ExperimentalCoroutinesApi::class)
    val matchingCustomers: StateFlow<List<CustomerEntity>> = _activeCompany
        .flatMapLatest { company ->
            customerRepository.getCustomersForCompany(company.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val company = companyRepository.getActiveCompany()
            _activeCompany.value = company
            val nextNumber = billingRepository.getNextInvoiceNumber(company.id)
            _invoiceNumber.value = nextNumber
        }
    }

    fun setCustomer(name: String, phone: String, gstin: String = "") {
        _customerName.value = name
        _customerPhone.value = phone
        _customerGstin.value = gstin
    }

    fun preselectParty(partyName: String, partyPhone: String = "", partyId: Long = 0L) {
        viewModelScope.launch {
            if (partyId > 0L) {
                val customer = customerRepository.getCustomerById(partyId)
                if (customer != null) {
                    _customerName.value = customer.name
                    _customerPhone.value = customer.phone
                    _customerGstin.value = customer.gstin
                    return@launch
                }
            }
            if (partyPhone.isNotBlank()) {
                val company = companyRepository.getActiveCompany()
                val customer = customerRepository.getCustomerByPhone(company.id, partyPhone)
                if (customer != null) {
                    _customerName.value = customer.name
                    _customerPhone.value = customer.phone
                    _customerGstin.value = customer.gstin
                    return@launch
                }
            }
            if (partyName.isNotBlank()) {
                _customerName.value = partyName
                _customerPhone.value = partyPhone
            }
        }
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setCustomerPhone(phone: String) {
        _customerPhone.value = phone
    }

    fun setCustomerGstin(gstin: String) {
        _customerGstin.value = gstin
    }

    fun setStatus(status: InvoiceStatus) {
        _selectedStatus.value = status
    }

    fun setPaidAmount(amount: String) {
        _paidAmount.value = amount
    }

    fun setPaymentMode(mode: PaymentMode) {
        _selectedPaymentMode.value = mode
    }

    fun addLineItem(item: LineItem) {
        _lineItems.value = _lineItems.value + item
    }

    fun updateItemQuantity(index: Int, qty: Int) {
        val current = _lineItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(quantity = qty)
            _lineItems.value = current
        }
    }

    fun removeLineItem(index: Int) {
        val current = _lineItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _lineItems.value = current
        }
    }

    // Optimization 3: 1-Tap Stock Item Autocomplete
    fun selectStockItem(item: InventoryItem) {
        val lineItem = LineItem(
            itemId = item.id,
            sku = item.sku,
            title = item.itemName,
            unitPrice = item.sellingPrice,
            quantity = 1,
            gstRatePercent = item.gstRatePercent
        )
        addLineItem(lineItem)
    }

    // Optimization 4: Barcode / QR Scan Hook
    fun addFromBarcode(scannedCode: String): Boolean {
        val trimmed = scannedCode.trim()
        val matchedStock = stockItems.value.firstOrNull {
            it.barcode.equals(trimmed, ignoreCase = true) ||
            it.sku.equals(trimmed, ignoreCase = true) ||
            it.itemName.contains(trimmed, ignoreCase = true)
        }
        if (matchedStock != null) {
            selectStockItem(matchedStock)
            return true
        }
        // Fallback: Add as new scanned item
        addLineItem(
            LineItem(
                itemId = System.currentTimeMillis(),
                sku = trimmed,
                title = "Scanned Item ($trimmed)",
                unitPrice = 100.0,
                quantity = 1,
                gstRatePercent = 5.0
            )
        )
        return true
    }

    fun buildCurrentInvoice(): Invoice {
        val parsedPaid = _paidAmount.value.toDoubleOrNull() ?: 0.0
        return createInvoiceUseCase.buildInvoice(
            company = _activeCompany.value,
            invoiceNumber = _invoiceNumber.value,
            customerName = _customerName.value,
            customerPhone = _customerPhone.value,
            items = _lineItems.value,
            status = _selectedStatus.value,
            paymentMode = _selectedPaymentMode.value,
            paidAmount = parsedPaid
        )
    }

    enum class DocumentMode {
        INVOICE,
        QUOTATION
    }

    private val _documentMode = MutableStateFlow(DocumentMode.INVOICE)
    val documentMode: StateFlow<DocumentMode> = _documentMode.asStateFlow()

    fun setDocumentMode(mode: DocumentMode) {
        _documentMode.value = mode
    }

    fun loadDocumentForEdit(docType: String, docId: Long) {
        viewModelScope.launch {
            if (docType.equals("quotation", ignoreCase = true)) {
                _documentMode.value = DocumentMode.QUOTATION
                val quote = quotationRepository?.getQuotationById(docId)
                if (quote != null) {
                    _invoiceNumber.value = quote.quotationNumber
                    _customerName.value = quote.customerName
                    _customerPhone.value = quote.customerPhone
                    val items = DocumentItemParser.parseLineItems(
                        itemsJson = quote.itemsJson,
                        itemsSummary = quote.itemsSummary,
                        defaultSubtotal = quote.subtotal,
                        catalogProducts = catalogProducts.value
                    )
                    if (items.isNotEmpty()) {
                        _lineItems.value = items
                    }
                    val cust = customerRepository.getCustomerByPhone(_activeCompany.value.id, quote.customerPhone)
                    if (cust != null) {
                        _customerGstin.value = cust.gstin
                    }
                }
            } else {
                _documentMode.value = DocumentMode.INVOICE
                val inv = billingRepository.getInvoiceById(docId)
                if (inv != null) {
                    _invoiceNumber.value = inv.invoiceNumber
                    _customerName.value = inv.customerName
                    _customerPhone.value = inv.customerPhone
                    try {
                        _selectedStatus.value = InvoiceStatus.valueOf(inv.status)
                    } catch (_: Exception) {}
                    if (inv.paidAmount > 0.0) {
                        _paidAmount.value = inv.paidAmount.toString()
                    }
                    try {
                        _selectedPaymentMode.value = PaymentMode.valueOf(inv.paymentMode)
                    } catch (_: Exception) {}
                    val items = DocumentItemParser.parseLineItems(
                        itemsJson = inv.itemsJson,
                        itemsSummary = inv.itemsSummary,
                        defaultSubtotal = inv.subtotal,
                        catalogProducts = catalogProducts.value
                    )
                    if (items.isNotEmpty()) {
                        _lineItems.value = items
                    }
                    val cust = customerRepository.getCustomerByPhone(_activeCompany.value.id, inv.customerPhone)
                    if (cust != null) {
                        _customerGstin.value = cust.gstin
                    }
                }
            }
        }
    }

    fun saveInvoice(
        pdfGenerator: InvoicePdfGenerator,
        isQuotation: Boolean = (_documentMode.value == DocumentMode.QUOTATION),
        onSuccess: (Invoice, File, android.net.Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (_lineItems.value.isEmpty()) {
            onError(IllegalStateException("Please add at least one product to the invoice"))
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val company = _activeCompany.value
                val parsedPaid = _paidAmount.value.toDoubleOrNull() ?: 0.0
                val invoice = createInvoiceUseCase.buildInvoice(
                    company = company,
                    invoiceNumber = _invoiceNumber.value,
                    customerName = _customerName.value,
                    customerPhone = _customerPhone.value,
                    items = _lineItems.value,
                    isInterState = isInterstate.value,
                    status = _selectedStatus.value,
                    paymentMode = _selectedPaymentMode.value,
                    paidAmount = parsedPaid
                )
                val cleanNumber = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")

                val pdfFile: File
                val syncedInvoice: Invoice
                if (isQuotation) {
                    val summary = invoice.items.joinToString(", ") { "${it.title} (x${it.quantity})" }
                    val itemsJson = DocumentItemParser.toJson(invoice.items)
                    val quotationRes = billingRepository.saveAndDispatchQuotation(company, invoice)
                    val qId = quotationRes.getOrNull()
                    val localQuotationId = quotationRepository?.createQuotation(
                        QuotationEntity(
                            companyId = company.id,
                            quotationNumber = invoice.invoiceNumber,
                            customerName = invoice.customerName,
                            customerPhone = invoice.customerPhone,
                            itemsSummary = summary,
                            itemsJson = itemsJson,
                            subtotal = invoice.subtotal,
                            taxAmount = invoice.cgst + invoice.sgst + invoice.igst,
                            grandTotal = invoice.grandTotal,
                            status = "PENDING",
                            syncStatus = if (qId != null && qId > 0) "SYNCED" else "PENDING_SYNC",
                            serverQuotationId = qId
                        )
                    ) ?: 0L

                    val downloadedQuotation = if (qId != null) {
                        billingRepository.downloadBackendQuotationPdf(
                            context = pdfGenerator.context,
                            quotationId = qId,
                            filename = "Quotation_${qId}_$cleanNumber.pdf"
                        )
                    } else null

                    pdfFile = downloadedQuotation ?: pdfGenerator.generateInvoicePdf(company, invoice)
                    syncedInvoice = invoice.copy(
                        id = localQuotationId,
                        serverOrderId = qId,
                        syncStatus = if (qId != null && qId > 0) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC
                    )
                } else {
                    val saveResult = billingRepository.saveAndDispatchInvoice(company, invoice)
                    val savedEntity = saveResult.getOrNull()

                    val backendPdfFile = if (savedEntity?.serverOrderId != null) {
                        billingRepository.downloadBackendInvoicePdf(
                            context = pdfGenerator.context,
                            backendOrderId = savedEntity.serverOrderId,
                            filename = "Invoice_$cleanNumber.pdf"
                        )
                    } else null

                    pdfFile = backendPdfFile ?: pdfGenerator.generateInvoicePdf(company, invoice)

                    // Refresh next invoice number for subsequent bills
                    _invoiceNumber.value = billingRepository.getNextInvoiceNumber(company.id)

                    syncedInvoice = invoice.copy(
                        id = savedEntity?.id ?: 0L,
                        serverOrderId = savedEntity?.serverOrderId,
                        syncStatus = if (savedEntity?.serverOrderId != null) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC
                    )
                }

                if (syncedInvoice.serverOrderId == null) {
                    try {
                        com.digitalchopda.app.data.work.InvoiceSyncWorker.enqueueImmediateSync(pdfGenerator.context)
                    } catch (_: Exception) {}
                }

                val uri = pdfGenerator.getFileProviderUri(pdfFile)

                _isSaving.value = false
                onSuccess(syncedInvoice, pdfFile, uri)
            } catch (e: Throwable) {
                _isSaving.value = false
                onError(e)
            }
        }
    }

    class Factory(
        private val billingRepository: BillingRepository,
        private val companyRepository: CompanyRepository,
        private val inventoryRepository: InventoryRepository,
        private val customerRepository: CustomerRepository,
        private val productRepository: ProductRepository? = null,
        private val quotationRepository: QuotationRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceViewModel(
                billingRepository,
                companyRepository,
                inventoryRepository,
                customerRepository,
                productRepository,
                quotationRepository
            ) as T
        }
    }
}
