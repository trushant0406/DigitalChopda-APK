package com.digitalchopda.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalchopda.app.core.network.ApiDashboardAnalyticsResponse
import com.digitalchopda.app.core.network.ApiDashboardSummaryResponse
import com.digitalchopda.app.core.network.ApiOrdersSummaryResponse
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.PaymentEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.ProductRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RevenueChartPoint(
    val date: String,
    val label: String,
    val billed: Double,
    val invoicesCount: Int
)

data class RevenueChartData(
    val period: String,
    val totalBilled: Double,
    val totalInvoices: Int,
    val averageTicket: Double,
    val points: List<RevenueChartPoint>
)

data class TopCustomerInfo(
    val id: Long,
    val name: String,
    val city: String,
    val totalPaid: Double
)

data class TopDebtorInfo(
    val id: Long,
    val name: String,
    val phone: String,
    val city: String,
    val balanceDue: Double,
    val daysPending: Int
)

data class TopProductInfo(
    val id: Long,
    val name: String,
    val sku: String,
    val unitsSold: Int,
    val unit: String
)

class DashboardViewModel(
    private val companyRepository: CompanyRepository,
    private val billingRepository: BillingRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository? = null
) : ViewModel() {

    private val _activeCompany = MutableStateFlow(
        Company(
            id = 1L,
            firmName = "ABC E-COMMERCE",
            ownerName = "Admin User",
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

    private val _selectedPeriod = MutableStateFlow("30d")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _recentInvoices = MutableStateFlow<List<InvoiceEntity>>(emptyList())
    val recentInvoices: StateFlow<List<InvoiceEntity>> = _recentInvoices.asStateFlow()

    private val _totalSales = MutableStateFlow(0.0)
    val totalSales: StateFlow<Double> = _totalSales.asStateFlow()

    private val _invoiceCount = MutableStateFlow(0)
    val invoiceCount: StateFlow<Int> = _invoiceCount.asStateFlow()

    private val _dashboardSummary = MutableStateFlow<ApiDashboardSummaryResponse?>(null)
    val dashboardSummary: StateFlow<ApiDashboardSummaryResponse?> = _dashboardSummary.asStateFlow()

    private val _dashboardAnalytics = MutableStateFlow<ApiDashboardAnalyticsResponse?>(null)
    val dashboardAnalytics: StateFlow<ApiDashboardAnalyticsResponse?> = _dashboardAnalytics.asStateFlow()

    private val _ordersSummary = MutableStateFlow<ApiOrdersSummaryResponse?>(null)
    val ordersSummary: StateFlow<ApiOrdersSummaryResponse?> = _ordersSummary.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val payments: StateFlow<List<PaymentEntity>> = _activeCompany
        .flatMapLatest { company ->
            billingRepository.getPaymentsForCompany(company.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCashInflow: StateFlow<Double> = payments.combine(_activeCompany) { list, _ ->
        list.filter { it.isJama }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCashOutflow: StateFlow<Double> = payments.combine(_activeCompany) { list, _ ->
        list.filter { !it.isJama }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawCustomers: StateFlow<List<CustomerEntity>> = _activeCompany
        .flatMapLatest { company ->
            customerRepository.getCustomersForCompany(company.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawInvoices: StateFlow<List<InvoiceEntity>> = _activeCompany
        .flatMapLatest { company ->
            billingRepository.getInvoicesForCompany(company.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawProducts: StateFlow<List<Product>> = _activeCompany
        .flatMapLatest { company ->
            productRepository?.getProducts(company.id) ?: flowOf(emptyList())
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

    val totalReceivable: StateFlow<Double> = rawCustomers.combine(_activeCompany) { list, _ ->
        list.filter { it.balance > 0 }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayable: StateFlow<Double> = rawCustomers.combine(_activeCompany) { list, _ ->
        list.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val receivableCount: StateFlow<Int> = rawCustomers.combine(_activeCompany) { list, _ ->
        list.count { it.balance > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val payableCount: StateFlow<Int> = rawCustomers.combine(_activeCompany) { list, _ ->
        list.count { it.balance < 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalJama: StateFlow<Double> = totalReceivable
    val totalUdhar: StateFlow<Double> = totalPayable

    // ── REVENUE DYNAMICS & CHART STATE (Offline-first + Cloud Enrichment) ──
    val revenueChartData: StateFlow<RevenueChartData> = combine(
        _selectedPeriod,
        rawInvoices,
        _dashboardAnalytics
    ) { period, invoices, serverAnalytics ->
        // 1. If server analytics matches current period, use official server numbers
        if (serverAnalytics != null && serverAnalytics.period == period && !serverAnalytics.series.isNullOrEmpty()) {
            val sPoints = serverAnalytics.series.map {
                RevenueChartPoint(
                    date = it.date ?: "",
                    label = it.label ?: "",
                    billed = it.billed,
                    invoicesCount = it.invoicesCount
                )
            }
            val tot = serverAnalytics.totals
            val b = tot?.totalBilled ?: sPoints.sumOf { it.billed }
            val c = tot?.totalInvoices ?: sPoints.sumOf { it.invoicesCount }
            val avg = if (c > 0) b / c else (tot?.averageOrderValue ?: 0.0)
            return@combine RevenueChartData(
                period = period,
                totalBilled = b,
                totalInvoices = c,
                averageTicket = avg,
                points = sPoints
            )
        }

        // 2. Offline-First Dynamic Local Calculation from SQLite
        computeOfflineRevenueChart(period, invoices)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RevenueChartData("30d", 0.0, 0, 0.0, emptyList())
    )

    // ── SPOTLIGHT INTELLIGENCE ──
    val topCustomers: StateFlow<List<TopCustomerInfo>> = combine(
        rawCustomers,
        rawInvoices,
        _dashboardAnalytics
    ) { customers, invoices, analytics ->
        val serverList = analytics?.topCustomers
        if (!serverList.isNullOrEmpty()) {
            return@combine serverList.map {
                TopCustomerInfo(
                    id = it.contactId ?: 0L,
                    name = it.name ?: "Customer",
                    city = it.city ?: "Local",
                    totalPaid = it.totalPaid
                )
            }
        }

        // Offline calculation: top parties by completed billing/payments
        val grouped = invoices.groupBy { it.customerName }
            .map { (name, invList) ->
                val paid = invList.sumOf { it.paidAmount }
                val match = customers.find { it.name.equals(name, ignoreCase = true) }
                TopCustomerInfo(
                    id = match?.id ?: 0L,
                    name = name,
                    city = match?.city?.ifBlank { "Surat" } ?: "Surat",
                    totalPaid = if (paid > 0) paid else invList.sumOf { it.grandTotal }
                )
            }
            .sortedByDescending { it.totalPaid }
            .take(4)

        if (grouped.isNotEmpty()) grouped
        else customers.take(4).map {
            TopCustomerInfo(
                id = it.id,
                name = it.name,
                city = it.city.ifBlank { "Ahmedabad" },
                totalPaid = kotlin.math.abs(it.balance)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topDebtors: StateFlow<List<TopDebtorInfo>> = combine(
        rawCustomers,
        rawInvoices,
        _dashboardAnalytics
    ) { customers, invoices, analytics ->
        val serverList = analytics?.topDebtors
        if (!serverList.isNullOrEmpty()) {
            return@combine serverList.map {
                TopDebtorInfo(
                    id = it.contactId ?: 0L,
                    name = it.name ?: "Party",
                    phone = it.phone ?: "",
                    city = it.city ?: "Local",
                    balanceDue = it.balanceDue,
                    daysPending = it.daysPending
                )
            }
        }

        // Offline calculation: parties with largest positive balance (Udhar)
        customers
            .filter { it.balance > 0 }
            .sortedByDescending { it.balance }
            .take(4)
            .map { c ->
                val dueInvoices = invoices.filter { it.customerName.equals(c.name, ignoreCase = true) && it.balanceDue > 0 }
                val oldestDays = dueInvoices.minOfOrNull {
                    val days = (System.currentTimeMillis() - it.createdAt) / (1000 * 60 * 60 * 24)
                    days.toInt().coerceAtLeast(1)
                } ?: 7
                TopDebtorInfo(
                    id = c.id,
                    name = c.name,
                    phone = c.phone,
                    city = c.city.ifBlank { "Surat" },
                    balanceDue = c.balance,
                    daysPending = oldestDays
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topProducts: StateFlow<List<TopProductInfo>> = combine(
        rawProducts,
        rawInvoices,
        _dashboardAnalytics
    ) { products, _, analytics ->
        val serverList = analytics?.topProducts
        if (!serverList.isNullOrEmpty()) {
            return@combine serverList.map {
                TopProductInfo(
                    id = 0L,
                    name = it.productName ?: "Product",
                    sku = "SKU-${it.productName?.hashCode()?.rem(100) ?: 10}",
                    unitsSold = it.unitsSold,
                    unit = "pcs"
                )
            }
        }

        products.take(4).mapIndexed { idx, p ->
            TopProductInfo(
                id = p.id,
                name = p.name,
                sku = p.sku ?: "SKU-${idx + 1}",
                unitsSold = (p.stockQuantity.toInt() / 2).coerceAtLeast(1),
                unit = p.unit
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── OPERATIONAL ACTION QUEUE ──
    val lowStockCount: StateFlow<Int> = combine(
        rawProducts,
        _dashboardAnalytics
    ) { products, analytics ->
        val serverCount = analytics?.lowStockItems?.size
        if (serverCount != null && serverCount > 0) return@combine serverCount
        products.count { it.stockQuantity <= it.minStockAlert }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overdueCount: StateFlow<Int> = combine(
        rawInvoices,
        _dashboardSummary,
        _dashboardAnalytics
    ) { invoices, summary, analytics ->
        if (analytics?.recentUrgentFollowups != null && analytics.recentUrgentFollowups.isNotEmpty()) {
            return@combine analytics.recentUrgentFollowups.size
        }
        if (summary?.overdue != null && summary.overdue > 0) {
            return@combine summary.overdue
        }
        invoices.count { !it.status.equals("PAID", ignoreCase = true) && it.balanceDue > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val collectionHealth: StateFlow<Double> = combine(
        _totalSales,
        totalJama,
        _dashboardAnalytics,
        _dashboardSummary
    ) { sales, jama, analytics, _ ->
        val eff = analytics?.totals?.collectionEfficiency
        if (eff != null && eff > 0.0) return@combine eff
        if (sales > 0) {
            val collected = (sales - jama).coerceAtLeast(0.0)
            ((collected / sales) * 100.0).coerceIn(10.0, 100.0)
        } else {
            86.4
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 86.4)

    init {
        viewModelScope.launch {
            companyRepository.getActiveCompanyFlow().collect { company ->
                if (company != null) {
                    _activeCompany.value = company
                    loadInvoiceStats(company.id)
                    loadSummaries()
                    loadAnalytics(_selectedPeriod.value)
                }
            }
        }
        refresh()
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        viewModelScope.launch {
            loadAnalytics(period)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val company = companyRepository.getActiveCompany()
                _activeCompany.value = company
                loadInvoiceStats(company.id)
                loadSummaries()
                loadAnalytics(_selectedPeriod.value)
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error in refresh()", e)
            }
        }
    }

    private suspend fun loadSummaries() {
        try {
            val sum = billingRepository.fetchDashboardSummary()
            if (sum != null) _dashboardSummary.value = sum
            val ord = billingRepository.fetchOrdersSummary()
            if (ord != null) _ordersSummary.value = ord
        } catch (e: Exception) {
            android.util.Log.e("DashboardViewModel", "Error in loadSummaries()", e)
        }
    }

    private suspend fun loadAnalytics(period: String) {
        try {
            val res = billingRepository.fetchDashboardAnalytics(period)
            if (res != null) {
                _dashboardAnalytics.value = res
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardViewModel", "Error in loadAnalytics()", e)
        }
    }

    private suspend fun loadInvoiceStats(companyId: Long) {
        try {
            val invoices = billingRepository.getRecentInvoices(companyId, limit = 10)
            _recentInvoices.value = invoices
            _totalSales.value = billingRepository.getTotalSales(companyId)
            _invoiceCount.value = billingRepository.getInvoiceCount(companyId)
        } catch (e: Exception) {
            android.util.Log.e("DashboardViewModel", "Error in loadInvoiceStats()", e)
        }
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
            try {
                val companyId = _activeCompany.value.id
                val currentList = rawCustomers.value
                val existing = currentList.firstOrNull { it.name.equals(partyName.trim(), ignoreCase = true) }
                val targetCustomerId = if (existing != null) {
                    existing.id
                } else {
                    customerRepository.addCustomer(
                        CustomerEntity(
                            companyId = companyId,
                            name = partyName.trim(),
                            phone = "",
                            balance = 0.0,
                            address = note
                        )
                    )
                }

                customerRepository.recordPayment(
                    companyId = companyId,
                    customerId = targetCustomerId,
                    amount = amount,
                    isJama = isJama,
                    paymentMode = "cash",
                    notes = note.ifBlank { if (isJama) "Quick Entry (You Got)" else "Quick Entry (You Gave)" }
                )
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error in addQuickVoucher()", e)
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                billingRepository.syncPendingInvoices()
                billingRepository.syncPendingPayments()
                billingRepository.syncInvoicesWithServer(_activeCompany.value.id)
                customerRepository.syncContactsWithServer(_activeCompany.value.id)
                refresh()
            } catch (_: Exception) {
            } finally {
                _isSyncing.value = false
            }
        }
    }

    companion object {
        fun computeOfflineRevenueChart(period: String, invoices: List<InvoiceEntity>): RevenueChartData {
            val sdfDay = SimpleDateFormat("dd MMM", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()

            val bucketCount = when (period) {
                "5d" -> 5
                "30d" -> 6
                "3m" -> 6
                "1y" -> 12
                else -> 6
            }

            val points = mutableListOf<RevenueChartPoint>()
            var runningBilled = 0.0
            var runningInvoices = 0

            when (period) {
                "5d" -> {
                    // Last 5 days
                    for (i in 4 downTo 0) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val dayStart = cal.apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val dayEnd = dayStart + 86400000L - 1

                        val bucketInvoices = invoices.filter { it.createdAt in dayStart..dayEnd }
                        val billed = bucketInvoices.sumOf { it.grandTotal }
                        val count = bucketInvoices.size
                        runningBilled += billed
                        runningInvoices += count

                        points.add(
                            RevenueChartPoint(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dayStart)),
                                label = sdfDay.format(Date(dayStart)),
                                billed = billed,
                                invoicesCount = count
                            )
                        )
                    }
                }
                "30d" -> {
                    // 6 intervals of 5 days
                    for (i in 5 downTo 0) {
                        val startDaysAgo = (i + 1) * 5
                        val endDaysAgo = i * 5
                        val endMillis = calendar.timeInMillis - (endDaysAgo * 86400000L)
                        val startMillis = calendar.timeInMillis - (startDaysAgo * 86400000L)

                        val bucketInvoices = invoices.filter { it.createdAt in startMillis..endMillis }
                        val billed = bucketInvoices.sumOf { it.grandTotal }
                        val count = bucketInvoices.size
                        runningBilled += billed
                        runningInvoices += count

                        points.add(
                            RevenueChartPoint(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endMillis)),
                                label = sdfDay.format(Date(endMillis)),
                                billed = billed,
                                invoicesCount = count
                            )
                        )
                    }
                }
                "3m" -> {
                    // 6 intervals of 15 days
                    for (i in 5 downTo 0) {
                        val endMillis = calendar.timeInMillis - (i * 15L * 86400000L)
                        val startMillis = endMillis - (15L * 86400000L)

                        val bucketInvoices = invoices.filter { it.createdAt in startMillis..endMillis }
                        val billed = bucketInvoices.sumOf { it.grandTotal }
                        val count = bucketInvoices.size
                        runningBilled += billed
                        runningInvoices += count

                        points.add(
                            RevenueChartPoint(
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endMillis)),
                                label = sdfDay.format(Date(endMillis)),
                                billed = billed,
                                invoicesCount = count
                            )
                        )
                    }
                }
                "1y" -> {
                    // 12 months
                    for (i in 11 downTo 0) {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.MONTH, -i)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val monthStart = cal.timeInMillis
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, maxDay)
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        val monthEnd = cal.timeInMillis

                        val bucketInvoices = invoices.filter { it.createdAt in monthStart..monthEnd }
                        val billed = bucketInvoices.sumOf { it.grandTotal }
                        val count = bucketInvoices.size
                        runningBilled += billed
                        runningInvoices += count

                        points.add(
                            RevenueChartPoint(
                                date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(monthStart)),
                                label = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(monthStart)),
                                billed = billed,
                                invoicesCount = count
                            )
                        )
                    }
                }
                else -> {
                    points.addAll(listOf(
                        RevenueChartPoint("2026-11-01", "01 Nov", 0.0, 0),
                        RevenueChartPoint("2026-11-07", "07 Nov", 0.0, 0)
                    ))
                }
            }

            // Total fallback if company has overall invoices
            val totalB = if (runningBilled > 0) runningBilled else invoices.sumOf { it.grandTotal }
            val totalC = if (runningInvoices > 0) runningInvoices else invoices.size
            val avg = if (totalC > 0) totalB / totalC else 0.0

            return RevenueChartData(
                period = period,
                totalBilled = totalB,
                totalInvoices = totalC,
                averageTicket = avg,
                points = points
            )
        }
    }

    class Factory(
        private val companyRepository: CompanyRepository,
        private val billingRepository: BillingRepository,
        private val customerRepository: CustomerRepository,
        private val productRepository: ProductRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                companyRepository,
                billingRepository,
                customerRepository,
                productRepository
            ) as T
        }
    }
}
