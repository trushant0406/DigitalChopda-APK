package com.digitalchopda.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.domain.model.Company
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val companyRepository: CompanyRepository,
    private val billingRepository: BillingRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _activeCompany = MutableStateFlow(
        Company(
            id = 1L,
            firmName = "Sharma Traders",
            ownerName = "Amit Sharma",
            gstin = "24AAACS1429B1Z8",
            phone = "+91 98765 43210",
            address = "Surat, Gujarat",
            isActive = true
        )
    )
    val activeCompany: StateFlow<Company> = _activeCompany.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _partyFilter = MutableStateFlow("ALL") // "ALL", "JAMA", "UDHAR"
    val partyFilter: StateFlow<String> = _partyFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _recentInvoices = MutableStateFlow<List<InvoiceEntity>>(emptyList())
    val recentInvoices: StateFlow<List<InvoiceEntity>> = _recentInvoices.asStateFlow()

    private val _totalSales = MutableStateFlow(0.0)
    val totalSales: StateFlow<Double> = _totalSales.asStateFlow()

    private val _invoiceCount = MutableStateFlow(0)
    val invoiceCount: StateFlow<Int> = _invoiceCount.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawCustomers: StateFlow<List<CustomerEntity>> = _activeCompany
        .flatMapLatest { company ->
            customerRepository.getCustomersForCompany(company.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(
        rawCustomers,
        _searchQuery,
        _partyFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesSearch = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.phone.contains(query)
            val matchesFilter = when (filter) {
                "JAMA" -> item.balance > 0
                "UDHAR" -> item.balance < 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalJama: StateFlow<Double> = rawCustomers.combine(_activeCompany) { list, _ ->
        list.filter { it.balance > 0 }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalUdhar: StateFlow<Double> = rawCustomers.combine(_activeCompany) { list, _ ->
        list.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val company = companyRepository.getActiveCompany()
            _activeCompany.value = company
            loadInvoiceStats(company.id)
        }
    }

    private suspend fun loadInvoiceStats(companyId: Long) {
        val invoices = billingRepository.getRecentInvoices(companyId, limit = 10)
        _recentInvoices.value = invoices
        _totalSales.value = billingRepository.getTotalSales(companyId)
        _invoiceCount.value = billingRepository.getInvoiceCount(companyId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPartyFilter(filter: String) {
        _partyFilter.value = filter
    }

    fun addQuickVoucher(
        partyName: String,
        amount: Double,
        isJama: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val companyId = _activeCompany.value.id
            val currentList = rawCustomers.value
            val existing = currentList.firstOrNull { it.name.equals(partyName.trim(), ignoreCase = true) }
            val delta = if (isJama) amount else -amount

            if (existing != null) {
                customerRepository.updateBalance(existing.id, delta)
            } else {
                customerRepository.addCustomer(
                    CustomerEntity(
                        companyId = companyId,
                        name = partyName.trim(),
                        phone = "",
                        balance = delta,
                        address = note
                    )
                )
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                billingRepository.syncPendingInvoices()
                customerRepository.syncContactsWithServer(_activeCompany.value.id)
                refresh()
            } catch (_: Exception) {
            } finally {
                _isSyncing.value = false
            }
        }
    }

    class Factory(
        private val companyRepository: CompanyRepository,
        private val billingRepository: BillingRepository,
        private val customerRepository: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                companyRepository,
                billingRepository,
                customerRepository
            ) as T
        }
    }
}
