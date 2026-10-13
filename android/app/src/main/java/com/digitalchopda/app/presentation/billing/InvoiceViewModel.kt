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

class InvoiceViewModel(
    private val billingRepository: BillingRepository,
    private val companyRepository: CompanyRepository,
    private val inventoryRepository: InventoryRepository,
    private val customerRepository: CustomerRepository
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

    private val _customerName = MutableStateFlow("Pooja Varma")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("+91 98251 12345")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _customerGstin = MutableStateFlow("")
    val customerGstin: StateFlow<String> = _customerGstin.asStateFlow()

    private val _selectedStatus = MutableStateFlow(InvoiceStatus.PAID)
    val selectedStatus: StateFlow<InvoiceStatus> = _selectedStatus.asStateFlow()

    private val _selectedPaymentMode = MutableStateFlow(PaymentMode.UPI)
    val selectedPaymentMode: StateFlow<PaymentMode> = _selectedPaymentMode.asStateFlow()

    private val _lineItems = MutableStateFlow<List<LineItem>>(
        listOf(
            LineItem(
                itemId = 1L,
                sku = "TEX-COT-01",
                title = "Pure Cotton Shahi Saree 6.3m",
                unitPrice = 1850.0,
                quantity = 2,
                gstRatePercent = 5.0
            ),
            LineItem(
                itemId = 2L,
                sku = "TEX-SILK-02",
                title = "Kanjivaram Jacquard Silk",
                unitPrice = 4200.0,
                quantity = 1,
                gstRatePercent = 5.0
            )
        )
    )
    val lineItems: StateFlow<List<LineItem>> = _lineItems.asStateFlow()

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
        return createInvoiceUseCase.buildInvoice(
            company = _activeCompany.value,
            invoiceNumber = _invoiceNumber.value,
            customerName = _customerName.value,
            customerPhone = _customerPhone.value,
            items = _lineItems.value,
            status = _selectedStatus.value,
            paymentMode = _selectedPaymentMode.value
        )
    }

    fun saveInvoice(
        pdfGenerator: InvoicePdfGenerator,
        onSuccess: (Invoice, File, android.net.Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val company = _activeCompany.value
                val invoice = createInvoiceUseCase.buildInvoice(
                    company = company,
                    invoiceNumber = _invoiceNumber.value,
                    customerName = _customerName.value,
                    customerPhone = _customerPhone.value,
                    items = _lineItems.value,
                    status = _selectedStatus.value,
                    paymentMode = _selectedPaymentMode.value
                )
                billingRepository.saveAndDispatchInvoice(company, invoice)
                val pdfFile = pdfGenerator.generateInvoicePdf(company, invoice)
                val uri = pdfGenerator.getFileProviderUri(pdfFile)

                // Refresh next invoice number for subsequent bills
                _invoiceNumber.value = billingRepository.getNextInvoiceNumber(company.id)

                _isSaving.value = false
                onSuccess(invoice, pdfFile, uri)
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
        private val customerRepository: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceViewModel(
                billingRepository,
                companyRepository,
                inventoryRepository,
                customerRepository
            ) as T
        }
    }
}
