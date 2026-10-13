package com.digitalchopda.app.data.repository

import com.digitalchopda.app.core.network.ApiCreateOrderRequest
import com.digitalchopda.app.core.network.ApiOrderItemRequest
import com.digitalchopda.app.core.network.AuthResponse
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.core.network.MockDigitalChopdaApiService
import com.digitalchopda.app.core.network.StockSyncDelta
import com.digitalchopda.app.core.security.TokenStorage
import com.digitalchopda.app.data.local.database.ChopdaDatabase
import com.digitalchopda.app.data.local.entity.CompanyEntity
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.ItemEntity
import com.digitalchopda.app.data.local.entity.LeadEntity
import com.digitalchopda.app.data.local.entity.LeaseEntity
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.InventoryItem
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.Lead
import com.digitalchopda.app.domain.model.LeadPriority
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.SequenceLease
import com.digitalchopda.app.domain.model.SyncStatus
import com.digitalchopda.app.domain.usecase.AllocateLeaseUseCase
import com.digitalchopda.app.domain.usecase.CreateInvoiceUseCase
import com.digitalchopda.app.domain.usecase.RebaseStockUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CompanyRepository(
    private val db: ChopdaDatabase,
    private val tokenStorage: TokenStorage,
    private val apiService: DigitalChopdaApiService? = null
) {
    fun getAllCompanies(): Flow<List<Company>> = db.companyDao().getAllCompanies().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getActiveCompany(): Company {
        val activeEntity = db.companyDao().getActiveCompany()
        return activeEntity?.toDomain() ?: Company(
            id = 1L,
            firmName = "Sharma Traders",
            ownerName = "Amit Sharma",
            gstin = "24AAACS1429B1Z8",
            phone = "+91 98765 43210",
            address = "Surat, Gujarat",
            isActive = true
        )
    }

    suspend fun switchActiveCompany(companyId: Long) {
        db.companyDao().deactivateAll()
        db.companyDao().setActiveCompany(companyId)
        tokenStorage.setActiveCompanyId(companyId)
    }

    suspend fun addCompany(company: Company): Long {
        val entity = CompanyEntity(
            firmName = company.firmName,
            ownerName = company.ownerName,
            gstin = company.gstin,
            phone = company.phone,
            address = company.address,
            stateCode = company.stateCode,
            financialYear = company.financialYear,
            invoicePrefix = company.invoicePrefix,
            currencySymbol = company.currencySymbol,
            isActive = false,
            upiVpa = company.upiVpa
        )
        return db.companyDao().insertCompany(entity)
    }

    suspend fun syncCompaniesWithServer(): Result<List<Company>> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            val response = api.getBusinesses()
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!
                val activeId = tokenStorage.getActiveCompanyId()
                for (biz in remoteList) {
                    val entity = CompanyEntity(
                        id = biz.id,
                        firmName = biz.name,
                        ownerName = biz.tradeName ?: biz.name,
                        invoicePrefix = biz.invoicePrefix ?: "INV-",
                        isActive = (biz.id == activeId)
                    )
                    db.companyDao().insertCompany(entity)
                }
                Result.success(remoteList.map { biz ->
                    Company(
                        id = biz.id,
                        firmName = biz.name,
                        ownerName = biz.tradeName ?: biz.name,
                        invoicePrefix = biz.invoicePrefix ?: "INV-",
                        isActive = (biz.id == activeId)
                    )
                })
            } else {
                Result.failure(Exception("Failed to fetch businesses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class BillingRepository(
    private val db: ChopdaDatabase,
    private val apiService: DigitalChopdaApiService? = null,
    private val mockApi: MockDigitalChopdaApiService? = null
) {
    private val allocateLeaseUseCase = AllocateLeaseUseCase()

    fun getInvoicesForCompany(companyId: Long): Flow<List<InvoiceEntity>> {
        return db.invoiceDao().getInvoicesForCompany(companyId)
    }

    suspend fun getTotalSales(companyId: Long): Double {
        return db.invoiceDao().getTotalSales(companyId) ?: 24580.0
    }

    suspend fun getInvoiceCount(companyId: Long): Int {
        val count = db.invoiceDao().getInvoiceCount(companyId)
        return if (count > 0) count else 18
    }

    suspend fun getNextInvoiceNumber(companyId: Long): String {
        // Try live backend next number check when online
        if (apiService != null) {
            try {
                val nextRes = apiService.getNextInvoiceNumber()
                if (nextRes.isSuccessful && nextRes.body() != null) {
                    return nextRes.body()!!.suggestedNumber
                }
            } catch (_: Exception) {}
        }

        // Offline sequence lease fallback
        var lease = db.leaseDao().getActiveLease(companyId)
        if (lease == null) {
            lease = LeaseEntity(
                companyId = companyId,
                prefix = "DC",
                financialYear = "26-27",
                rangeStart = 1001L,
                rangeEnd = 1200L,
                currentCursor = 1001L
            )
            db.leaseDao().insertOrUpdateLease(lease)
        }
        val domainLease = lease.toDomain()
        return domainLease.currentFormattedNumber()
    }

    suspend fun saveAndDispatchInvoice(
        company: Company,
        invoice: Invoice
    ): Result<InvoiceEntity> {
        // 1. Advance Sequence Lease locally (Rule 46)
        val leaseEntity = db.leaseDao().getActiveLease(company.id)
        if (leaseEntity != null) {
            val res = allocateLeaseUseCase.allocateNextInvoiceNumber(leaseEntity.toDomain())
            db.leaseDao().updateLease(
                leaseEntity.copy(
                    currentCursor = res.updatedLease.currentCursor
                )
            )
        }

        // 2. Persist locally first with PENDING_SYNC (Zero-latency offline first)
        val summary = invoice.items.joinToString(", ") { "${it.title} (x${it.quantity})" }
        val entity = InvoiceEntity(
            companyId = company.id,
            invoiceNumber = invoice.invoiceNumber,
            customerName = invoice.customerName,
            customerPhone = invoice.customerPhone,
            itemsSummary = summary,
            subtotal = invoice.subtotal,
            cgst = invoice.cgst,
            sgst = invoice.sgst,
            igst = invoice.igst,
            roundOff = invoice.roundOff,
            grandTotal = invoice.grandTotal,
            status = invoice.status.name,
            paymentMode = invoice.paymentMode.name,
            syncStatus = SyncStatus.PENDING_SYNC.name,
            idempotencyKey = invoice.idempotencyKey
        )
        val insertedId = db.invoiceDao().insertInvoice(entity)

        // 3. Central Depot Dispatch: Push to live backend
        try {
            if (apiService != null) {
                val orderRequest = ApiCreateOrderRequest(
                    phone = invoice.customerPhone.ifBlank { "9999999999" },
                    name = invoice.customerName.ifBlank { "Walk-in Customer" },
                    items = invoice.items.map {
                        ApiOrderItemRequest(
                            productName = it.title,
                            quantity = it.quantity.toDouble(),
                            unitPrice = it.unitPrice,
                            gstRate = it.gstRatePercent
                        )
                    },
                    paymentStatus = if (invoice.status == InvoiceStatus.PAID) "paid" else "pending",
                    paymentMethod = invoice.paymentMode.name.lowercase(),
                    createInvoice = true,
                    invoiceNumber = invoice.invoiceNumber,
                    notes = "Created via DigitalChopda Mobile App"
                )
                val resp = apiService.createOrder(orderRequest)
                if (resp.isSuccessful) {
                    db.invoiceDao().updateSyncStatus(insertedId, SyncStatus.SYNCED.name)
                }
            } else if (mockApi != null) {
                val syncResult = mockApi.syncInvoiceOutbox(invoice, invoice.idempotencyKey)
                if (syncResult.isSuccess) {
                    db.invoiceDao().updateSyncStatus(insertedId, SyncStatus.SYNCED.name)
                }
            }
        } catch (_: Exception) {
            // Preserves PENDING_SYNC for automatic outbox flush on reconnection
        }

        return Result.success(entity.copy(id = insertedId))
    }

    suspend fun syncPendingInvoices(): Int {
        val api = apiService ?: return 0
        val pending = db.invoiceDao().getPendingSyncInvoices()
        var syncedCount = 0
        for (inv in pending) {
            try {
                val orderRequest = ApiCreateOrderRequest(
                    phone = inv.customerPhone.ifBlank { "9999999999" },
                    name = inv.customerName.ifBlank { "Walk-in Customer" },
                    items = listOf(
                        ApiOrderItemRequest(
                            productName = inv.itemsSummary.ifBlank { "General Items" },
                            quantity = 1.0,
                            unitPrice = inv.subtotal,
                            gstRate = 18.0
                        )
                    ),
                    paymentStatus = if (inv.status == "PAID") "paid" else "pending",
                    paymentMethod = inv.paymentMode.lowercase(),
                    createInvoice = true,
                    invoiceNumber = inv.invoiceNumber,
                    notes = "Synced from Mobile Outbox"
                )
                val res = api.createOrder(orderRequest)
                if (res.isSuccessful) {
                    db.invoiceDao().updateSyncStatus(inv.id, SyncStatus.SYNCED.name)
                    syncedCount++
                }
            } catch (_: Exception) {}
        }
        return syncedCount
    }

    suspend fun getRecentInvoices(companyId: Long, limit: Int = 10): List<InvoiceEntity> {
        return db.invoiceDao().getRecentInvoices(companyId, limit)
    }
}

class InventoryRepository(
    private val db: ChopdaDatabase,
    private val apiService: DigitalChopdaApiService? = null,
    private val mockApi: MockDigitalChopdaApiService? = null
) {
    fun getItemsForCompany(companyId: Long): Flow<List<InventoryItem>> {
        return db.itemDao().getItemsForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getLowStockItems(companyId: Long): Flow<List<InventoryItem>> {
        return db.itemDao().getLowStockItems(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun adjustQuantity(itemId: Long, delta: Int) {
        val item = db.itemDao().getItemById(itemId) ?: return
        val newQty = (item.quantity + delta).coerceAtLeast(0)
        val newVer = item.version + 1
        val now = System.currentTimeMillis()

        db.itemDao().updateStockQuantityAndVersion(itemId, newQty, newVer, now)
    }

    suspend fun addItem(item: InventoryItem): Long {
        val entity = ItemEntity(
            companyId = item.companyId,
            sku = item.sku,
            itemName = item.itemName,
            packSize = item.packSize,
            quantity = item.quantity,
            sellingPrice = item.sellingPrice,
            costPrice = item.costPrice,
            gstRatePercent = item.gstRatePercent,
            minStockThreshold = item.minStockThreshold,
            version = 1
        )
        return db.itemDao().insertItem(entity)
    }

    suspend fun syncProductsWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            val response = api.getProducts()
            if (response.isSuccessful && response.body() != null) {
                val remoteItems = response.body()!!
                for (p in remoteItems) {
                    val itemEntity = ItemEntity(
                        companyId = companyId,
                        sku = "PROD-${p.id}",
                        itemName = p.name,
                        packSize = "1 Unit",
                        quantity = 50,
                        sellingPrice = p.defaultPrice,
                        costPrice = p.defaultPrice * 0.7,
                        gstRatePercent = p.gstRate ?: 18.0,
                        minStockThreshold = 5,
                        version = 1
                    )
                    db.itemDao().insertItem(itemEntity)
                }
                Result.success(remoteItems.size)
            } else {
                Result.failure(Exception("Failed to fetch products: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class LeadRepository(private val db: ChopdaDatabase) {
    fun getLeadsForCompany(companyId: Long): Flow<List<Lead>> {
        return db.leadDao().getLeadsForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun addLead(lead: Lead): Long {
        val entity = LeadEntity(
            companyId = lead.companyId,
            customerName = lead.customerName,
            businessName = lead.businessName,
            phone = lead.phone,
            priority = lead.priority.name,
            requirementDetails = lead.requirementDetails,
            expectedDealValue = lead.expectedDealValue,
            followUpTimestamp = lead.followUpTimestamp
        )
        return db.leadDao().insertLead(entity)
    }
}

class CustomerRepository(
    private val db: ChopdaDatabase,
    private val apiService: DigitalChopdaApiService? = null
) {
    fun getCustomersForCompany(companyId: Long): Flow<List<CustomerEntity>> {
        return db.customerDao().getCustomersForCompany(companyId)
    }

    suspend fun getCustomerById(id: Long): CustomerEntity? {
        return db.customerDao().getCustomerById(id)
    }

    suspend fun addCustomer(customer: CustomerEntity): Long {
        return db.customerDao().insertCustomer(customer)
    }

    suspend fun updateBalance(customerId: Long, delta: Double) {
        db.customerDao().updateBalance(customerId, delta)
    }

    fun searchCustomers(companyId: Long, query: String): Flow<List<CustomerEntity>> {
        return db.customerDao().searchCustomers(companyId, query)
    }

    suspend fun deleteCustomer(id: Long) {
        db.customerDao().deleteCustomer(id)
    }

    suspend fun syncContactsWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            val res = api.getContacts()
            if (res.isSuccessful && res.body() != null) {
                val contacts = res.body()!!
                for (c in contacts) {
                    val entity = CustomerEntity(
                        companyId = companyId,
                        name = c.name ?: "Contact ${c.phone}",
                        phone = c.phone,
                        gstin = c.gstin ?: "",
                        balance = c.balance ?: 0.0,
                        address = c.billingAddress ?: c.city ?: ""
                    )
                    db.customerDao().insertCustomer(entity)
                }
                Result.success(contacts.size)
            } else {
                Result.failure(Exception("Failed to fetch contacts: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AuthRepository(
    private val tokenStorage: TokenStorage,
    private val apiService: DigitalChopdaApiService? = null,
    private val db: ChopdaDatabase? = null,
    private val mockApi: MockDigitalChopdaApiService? = null
) {
    suspend fun loginWithPassword(usernameOrMobile: String, pass: String?): Result<AuthResponse> {
        val api = apiService
        if (api != null) {
            try {
                val req = com.digitalchopda.app.core.network.ApiLoginRequest(
                    username = usernameOrMobile.trim(),
                    password = pass
                )
                val res = api.login(req)
                if (res.isSuccessful && res.body()?.token != null) {
                    val body = res.body()!!
                    val token = body.token!!
                    val userId = body.user?.id?.toString() ?: "1"
                    val activeBizId = body.activeBusiness?.id ?: body.businesses?.firstOrNull()?.id ?: 1L

                    tokenStorage.saveTokens(token, "")
                    tokenStorage.saveUserSession(userId, usernameOrMobile, activeBizId)

                    val companiesList = body.businesses?.map { b ->
                        Company(
                            id = b.id,
                            firmName = b.name,
                            ownerName = b.tradeName ?: body.user?.name ?: b.name,
                            invoicePrefix = b.invoicePrefix ?: "INV-",
                            isActive = (b.id == activeBizId)
                        )
                    } ?: listOf(
                        Company(
                            id = activeBizId,
                            firmName = body.activeBusiness?.name ?: "DigitalChopda Store",
                            ownerName = body.user?.name ?: "Owner",
                            isActive = true
                        )
                    )

                    // Seed server businesses into Room
                    if (db != null) {
                        for (c in companiesList) {
                            db.companyDao().insertCompany(
                                CompanyEntity(
                                    id = c.id,
                                    firmName = c.firmName,
                                    ownerName = c.ownerName,
                                    invoicePrefix = c.invoicePrefix,
                                    isActive = c.isActive
                                )
                            )
                        }
                    }

                    return Result.success(
                        AuthResponse(
                            success = true,
                            accessToken = token,
                            refreshToken = "",
                            userId = userId,
                            ownerName = body.user?.name ?: body.user?.username ?: "Merchant",
                            companies = companiesList
                        )
                    )
                } else {
                    val parsedError = try {
                        val errorString = res.errorBody()?.string()
                        if (!errorString.isNullOrBlank()) {
                            org.json.JSONObject(errorString).optString("error", "")
                        } else ""
                    } catch (_: Exception) { "" }.ifBlank {
                        res.body()?.error ?: "Invalid credentials (Code ${res.code()})"
                    }
                    return Result.failure(IllegalArgumentException(parsedError))
                }
            } catch (e: Exception) {
                // If live backend fails, fall back to mock service for offline resilience
                if (mockApi != null) {
                    return mockApi.login(usernameOrMobile, pass).also {
                        it.onSuccess { auth ->
                            tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
                            tokenStorage.saveUserSession(auth.userId, usernameOrMobile, auth.companies.firstOrNull()?.id ?: 1L)
                        }
                    }
                }
                return Result.failure(e)
            }
        }

        val fallback = mockApi ?: MockDigitalChopdaApiService()
        val res = fallback.login(usernameOrMobile, pass)
        res.onSuccess { auth ->
            tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
            tokenStorage.saveUserSession(auth.userId, usernameOrMobile, auth.companies.firstOrNull()?.id ?: 1L)
        }
        return res
    }

    suspend fun verifyOtp(mobile: String, otp: String): Result<AuthResponse> {
        val fallback = mockApi ?: MockDigitalChopdaApiService()
        val res = fallback.verifyOtp(mobile, otp)
        res.onSuccess { auth ->
            tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
            tokenStorage.saveUserSession(auth.userId, mobile, auth.companies.firstOrNull()?.id ?: 1L)
        }
        return res
    }

    suspend fun logout() {
        val token = tokenStorage.getAccessToken()
        try {
            mockApi?.logout(token)
        } catch (_: Exception) {}
        tokenStorage.clear()
    }

    fun isUserLoggedIn(): Boolean = tokenStorage.isLoggedIn()
}

// Entity Mappers
fun CompanyEntity.toDomain() = Company(
    id = id,
    firmName = firmName,
    ownerName = ownerName,
    gstin = gstin,
    phone = phone,
    address = address,
    stateCode = stateCode,
    financialYear = financialYear,
    invoicePrefix = invoicePrefix,
    currencySymbol = currencySymbol,
    isActive = isActive,
    upiVpa = upiVpa
)

fun LeaseEntity.toDomain() = SequenceLease(
    id = id,
    companyId = companyId,
    prefix = prefix,
    financialYear = financialYear,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    currentCursor = currentCursor,
    deviceId = deviceId,
    lastRenewedAt = lastRenewedAt
)

fun ItemEntity.toDomain() = InventoryItem(
    id = id,
    companyId = companyId,
    sku = sku,
    itemName = itemName,
    packSize = packSize,
    quantity = quantity,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    gstRatePercent = gstRatePercent,
    minStockThreshold = minStockThreshold,
    version = version,
    lastUpdated = lastUpdated
)

fun LeadEntity.toDomain() = Lead(
    id = id,
    companyId = companyId,
    customerName = customerName,
    businessName = businessName,
    phone = phone,
    priority = try { LeadPriority.valueOf(priority) } catch (_: Exception) { LeadPriority.HOT },
    requirementDetails = requirementDetails,
    expectedDealValue = expectedDealValue,
    followUpTimestamp = followUpTimestamp,
    isClosed = isClosed
)
