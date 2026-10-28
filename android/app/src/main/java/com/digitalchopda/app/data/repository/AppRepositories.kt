package com.digitalchopda.app.data.repository

import android.content.Context
import java.io.File
import com.digitalchopda.app.core.network.ApiCreateOrderRequest
import com.digitalchopda.app.core.network.ApiOrderItemRequest
import com.digitalchopda.app.core.network.ApiPaymentInRequest
import com.digitalchopda.app.core.network.ApiQuotationRequest
import com.digitalchopda.app.core.network.ApiQuotationItemRequest
import com.digitalchopda.app.core.network.AuthResponse
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.core.security.TokenStorage
import com.digitalchopda.app.data.local.database.ChopdaDatabase
import androidx.room.withTransaction
import com.digitalchopda.app.data.local.entity.CompanyEntity
import com.digitalchopda.app.data.local.entity.CreditNoteEntity
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.ItemEntity
import com.digitalchopda.app.data.local.entity.LeadEntity
import com.digitalchopda.app.data.local.entity.LeaseEntity
import com.digitalchopda.app.data.local.entity.OrderRevisionEntity
import com.digitalchopda.app.data.local.entity.PaymentEntity
import com.digitalchopda.app.data.local.entity.ProductEntity
import com.digitalchopda.app.data.local.entity.PurchaseEntity
import com.digitalchopda.app.data.local.entity.PurchaseItemEntity
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.data.local.entity.StockAdjustmentEntity
import com.digitalchopda.app.data.local.entity.SupplierEntity
import com.digitalchopda.app.domain.model.Party360Detail
import com.digitalchopda.app.domain.model.PartyLedgerEntry
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.InventoryItem
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.Lead
import com.digitalchopda.app.domain.model.LeadPriority
import com.digitalchopda.app.domain.model.OrderRevision
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.Product
import com.digitalchopda.app.domain.model.Purchase
import com.digitalchopda.app.domain.model.PurchaseItem
import com.digitalchopda.app.domain.model.SequenceLease
import com.digitalchopda.app.domain.model.StockAdjustment
import com.digitalchopda.app.domain.model.SyncStatus
import com.digitalchopda.app.domain.usecase.AllocateLeaseUseCase
import com.digitalchopda.app.domain.usecase.CreateInvoiceUseCase
import com.digitalchopda.app.domain.usecase.RebaseStockUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompanyRepository(
    private val db: ChopdaDatabase,
    private val tokenStorage: TokenStorage,
    val apiService: DigitalChopdaApiService? = null
) {
    fun getAllCompanies(): Flow<List<Company>> = db.companyDao().getAllCompanies().map { list ->
        list.map { it.toDomain() }
    }

    fun getActiveCompanyFlow(): Flow<Company?> = db.companyDao().getActiveCompanyFlow().map { it?.toDomain() }

    suspend fun getActiveCompany(): Company {
        val activeEntity = db.companyDao().getActiveCompany()
        return activeEntity?.toDomain() ?: Company(
            id = 1L,
            firmName = "My Business",
            ownerName = "Admin User",
            gstin = "",
            phone = "9876543210",
            address = "Surat, Gujarat",
            isActive = true,
            invoicePrefix = "INV-"
        )
    }

    suspend fun switchActiveCompany(companyId: Long) {
        db.companyDao().deactivateAll()
        db.companyDao().setActiveCompany(companyId)
        tokenStorage.setActiveCompanyId(companyId)
        try {
            com.digitalchopda.app.core.network.RealtimeEventSubscriber.restartForCompany(
                com.digitalchopda.app.DigitalChopdaApplication.instance,
                companyId
            )
        } catch (_: Exception) {}
    }

    suspend fun addCompany(company: Company): Long {
        var serverId: Long? = null
        if (apiService != null) {
            try {
                val req = com.digitalchopda.app.core.network.ApiCreateBusinessRequest(
                    name = company.firmName,
                    tradeName = company.ownerName,
                    gstin = company.gstin.ifBlank { null },
                    phone = company.phone.ifBlank { null },
                    address = company.address.ifBlank { null },
                    invoicePrefix = company.invoicePrefix.ifBlank { "INV-" }
                )
                val res = apiService.createBusiness(req)
                if (res.isSuccessful && res.body() != null) {
                    serverId = res.body()!!.id
                }
            } catch (e: Exception) {
                android.util.Log.e("CompanyRepository", "Failed to create company on server", e)
            }
        }
        val entity = CompanyEntity(
            id = serverId ?: 0L,
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

    suspend fun updateCompany(company: Company) {
        val entity = CompanyEntity(
            id = company.id,
            firmName = company.firmName,
            ownerName = company.ownerName,
            gstin = company.gstin,
            phone = company.phone,
            address = company.address,
            stateCode = company.stateCode,
            financialYear = company.financialYear,
            invoicePrefix = company.invoicePrefix,
            currencySymbol = company.currencySymbol,
            isActive = company.isActive,
            upiVpa = company.upiVpa,
            bankName = company.bankName,
            bankAccountNo = company.bankAccountNo,
            bankIfsc = company.bankIfsc,
            bankBranch = company.bankBranch
        )
        db.companyDao().updateCompany(entity)

        // Keep active lease prefix aligned with updated company prefix
        val currentLease = db.leaseDao().getActiveLease(company.id)
        if (currentLease != null && company.invoicePrefix.isNotBlank() && currentLease.prefix != company.invoicePrefix) {
            db.leaseDao().updateLease(currentLease.copy(prefix = company.invoicePrefix))
        }
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

                    // Keep active lease prefix aligned with server business settings
                    val currentLease = db.leaseDao().getActiveLease(biz.id)
                    if (currentLease != null && biz.invoicePrefix != null && currentLease.prefix != biz.invoicePrefix) {
                        db.leaseDao().updateLease(currentLease.copy(prefix = biz.invoicePrefix))
                    }
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

    suspend fun getSettingsStatus(): Result<com.digitalchopda.app.core.network.ApiSettingsStatusResponse> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            val res = api.getSettingsStatus()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reconnectWhatsApp(): Result<com.digitalchopda.app.core.network.ApiSettingsStatusResponse> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            val res = api.reconnectWhatsAppWeb()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class BillingRepository(
    private val db: ChopdaDatabase,
    val apiService: DigitalChopdaApiService? = null
) {
    private val allocateLeaseUseCase = AllocateLeaseUseCase()

    fun getInvoicesForCompany(companyId: Long): Flow<List<InvoiceEntity>> {
        return db.invoiceDao().getInvoicesForCompany(companyId)
    }

    fun observePendingSyncCount(): Flow<Int> {
        return combine(
            db.invoiceDao().observePendingSyncInvoiceCount(),
            db.paymentDao().observePendingSyncPaymentCount(),
            db.customerDao().observePendingSyncCustomerCount(),
            db.stockAdjustmentDao().observePendingSyncAdjustmentCount()
        ) { invoiceCount, paymentCount, customerCount, adjustmentCount ->
            invoiceCount + paymentCount + customerCount + adjustmentCount
        }
    }

    fun observePendingInvoiceCount(): Flow<Int> = db.invoiceDao().observePendingSyncInvoiceCount()
    fun observePendingPaymentCount(): Flow<Int> = db.paymentDao().observePendingSyncPaymentCount()
    fun observePendingCustomerCount(): Flow<Int> = db.customerDao().observePendingSyncCustomerCount()
    fun observePendingAdjustmentCount(): Flow<Int> = db.stockAdjustmentDao().observePendingSyncAdjustmentCount()

    suspend fun getActiveCompany(): Company? {
        return db.companyDao().getActiveCompany()?.toDomain()
    }

    suspend fun getCreditNoteById(id: Long): CreditNoteEntity? {
        return db.creditNoteDao().getCreditNoteById(id)
    }

    suspend fun getInvoiceById(id: Long): InvoiceEntity? {
        return db.invoiceDao().getInvoiceById(id)
    }

    suspend fun getInvoiceByNumber(companyId: Long, invoiceNumber: String): InvoiceEntity? {
        return db.invoiceDao().getInvoiceByNumber(companyId, invoiceNumber)
    }

    suspend fun getTotalSales(companyId: Long): Double {
        return db.invoiceDao().getTotalSales(companyId) ?: 0.0
    }

    suspend fun getInvoiceCount(companyId: Long): Int {
        return db.invoiceDao().getInvoiceCount(companyId)
    }

    suspend fun syncInvoicesWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.success(0)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Flush local pending invoices first
                try {
                    syncPendingInvoices()
                } catch (e: Exception) {
                    android.util.Log.w("BillingRepo", "Outbox flush before order fetch had errors", e)
                }

                val res = api.getOrders()
                if (!res.isSuccessful || res.body() == null) {
                    return@withContext Result.failure(Exception("Failed to fetch orders: ${res.code()}"))
                }
                val remoteOrders = res.body()!!
                var syncedCount = 0
                for (o in remoteOrders) {
                    val oBizId = o.businessId ?: 1L
                    if (oBizId != companyId && companyId != 1L) {
                        continue
                    }
                    val invoiceNum = o.invoiceNumber ?: o.orderNumber ?: "INV-${o.id}"
                    val custName = o.customerName ?: o.name ?: "Customer"
                    val custPhone = o.customerPhone ?: o.phone ?: ""
                    val grandTot = o.grandTotal ?: 0.0
                    val subTot = o.totalAmount ?: if (grandTot > 0) (grandTot / 1.18) else 0.0
                    val taxTot = o.taxAmount ?: (grandTot - subTot)
                    val isPaid = o.paymentStatus?.equals("paid", ignoreCase = true) == true
                    val isPartial = o.paymentStatus?.equals("partially_paid", ignoreCase = true) == true
                    val statusStr = when {
                        isPaid -> "PAID"
                        isPartial -> "PARTIAL"
                        else -> "UNPAID"
                    }
                    val recAmount = o.receivedAmount ?: (if (isPaid) grandTot else 0.0)
                    val createdAtMillis = com.digitalchopda.app.util.DateTimeUtils.parseIsoDateToMillis(o.createdAt ?: o.invoiceDate)

                    val itemsJsonStr = if (!o.items.isNullOrEmpty()) {
                        val lineItems = o.items.map { item ->
                            com.digitalchopda.app.domain.model.LineItem(
                                title = item.productName,
                                unitPrice = item.unitPrice,
                                quantity = item.quantity.toInt().coerceAtLeast(1),
                                gstRatePercent = item.gstRate ?: 18.0
                            )
                        }
                        com.digitalchopda.app.core.document.DocumentItemParser.toJson(lineItems)
                    } else ""

                    val existing = db.invoiceDao().getInvoiceByServerOrderId(o.id)
                        ?: db.invoiceDao().getInvoiceByNumber(companyId, invoiceNum)
                    val fulfillment = o.fulfillmentStatus ?: existing?.fulfillmentStatus ?: "not_ready"

                    if (existing != null) {
                        db.invoiceDao().updateInvoice(
                            existing.copy(
                                companyId = companyId,
                                invoiceNumber = invoiceNum,
                                customerName = custName,
                                customerPhone = custPhone,
                                itemsSummary = o.itemsSummary ?: existing.itemsSummary.ifBlank { "Goods / Services" },
                                itemsJson = if (itemsJsonStr.isNotBlank()) itemsJsonStr else existing.itemsJson,
                                subtotal = subTot,
                                grandTotal = grandTot,
                                paidAmount = recAmount,
                                status = statusStr,
                                fulfillmentStatus = fulfillment,
                                syncStatus = "SYNCED",
                                serverOrderId = o.id
                            )
                        )
                    } else {
                        db.invoiceDao().insertInvoice(
                            InvoiceEntity(
                                companyId = companyId,
                                invoiceNumber = invoiceNum,
                                customerName = custName,
                                customerPhone = custPhone,
                                itemsSummary = o.itemsSummary ?: "Goods / Services",
                                itemsJson = itemsJsonStr,
                                subtotal = subTot,
                                cgst = taxTot / 2.0,
                                sgst = taxTot / 2.0,
                                igst = 0.0,
                                roundOff = 0.0,
                                grandTotal = grandTot,
                                paidAmount = recAmount,
                                status = statusStr,
                                paymentMode = "CASH",
                                fulfillmentStatus = fulfillment,
                                syncStatus = "SYNCED",
                                idempotencyKey = "server-${o.id}-${System.currentTimeMillis()}",
                                serverOrderId = o.id,
                                createdAt = createdAtMillis
                            )
                        )
                    }
                    syncedCount++
                }

                // 🧹 Authoritative Reconciliation: Prune stale or reseeded invoices
                val currentInvoices = db.invoiceDao().getAllInvoicesSync(companyId)
                val remoteOrderIds = remoteOrders.map { it.id }.toSet()
                val staleInvoices = currentInvoices.filter {
                    it.companyId == companyId && (
                        (it.serverOrderId != null && it.serverOrderId !in remoteOrderIds && it.syncStatus == "SYNCED") ||
                        (it.serverOrderId == null && it.syncStatus == "SYNCED")
                    )
                }
                for (si in staleInvoices) {
                    db.invoiceDao().deleteInvoice(si.id)
                }

                val remainingInvoices = db.invoiceDao().getAllInvoicesSync(companyId)
                val groupedInvoices = remainingInvoices.filter { it.serverOrderId != null }.groupBy { it.serverOrderId!! }
                for ((_, duplicates) in groupedInvoices) {
                    if (duplicates.size > 1) {
                        val sorted = duplicates.sortedByDescending { it.id }
                        for (dup in sorted.drop(1)) {
                            db.invoiceDao().deleteInvoice(dup.id)
                        }
                    }
                }

                Result.success(syncedCount)
            } catch (e: Exception) {
                android.util.Log.e("BillingRepository", "Error syncing orders/invoices from server", e)
                Result.failure(e)
            }
        }
    }

    suspend fun updateFulfillmentStatus(invoiceId: Long, serverOrderId: Long?, nextStatus: String): Result<Unit> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                db.invoiceDao().updateFulfillmentStatus(invoiceId, nextStatus)
                if (serverOrderId != null && serverOrderId > 0 && apiService != null) {
                    try {
                        apiService.updateOrderStatus(serverOrderId, mapOf("fulfillment_status" to nextStatus))
                    } catch (e: Exception) {
                        android.util.Log.w("BillingRepository", "Failed to sync fulfillment status to server: ${e.message}")
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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

        val company = db.companyDao().getCompanyById(companyId)
        val companyPrefix = company?.invoicePrefix?.trim()?.ifBlank { "DC" } ?: "DC"
        val companyFy = company?.financialYear?.ifBlank { "26-27" } ?: "26-27"

        // Cloud sequence lease fallback or dynamic acquisition
        var lease = db.leaseDao().getActiveLease(companyId)
        if (lease != null && lease.prefix != companyPrefix) {
            lease = lease.copy(prefix = companyPrefix)
            db.leaseDao().updateLease(lease)
        }

        if (lease == null || lease.toDomain().needsRenewal) {
            if (apiService != null) {
                try {
                    val leaseRes = apiService.acquireLease(
                        com.digitalchopda.app.core.network.ApiLeaseRequest(
                            deviceId = "DEVICE-01",
                            requestedBlockSize = 200
                        )
                    )
                    if (leaseRes.isSuccessful && leaseRes.body() != null) {
                        val body = leaseRes.body()!!
                        val newLease = LeaseEntity(
                            companyId = companyId,
                            prefix = body.prefix ?: companyPrefix,
                            financialYear = body.financialYear ?: companyFy,
                            rangeStart = body.rangeStart ?: 1001L,
                            rangeEnd = body.rangeEnd ?: 1200L,
                            currentCursor = body.rangeStart ?: 1001L,
                            deviceId = "DEVICE-01",
                            lastRenewedAt = System.currentTimeMillis()
                        )
                        db.leaseDao().insertOrUpdateLease(newLease)
                        lease = newLease
                    }
                } catch (_: Exception) {}
            }
        }

        if (lease == null) {
            lease = LeaseEntity(
                companyId = companyId,
                prefix = companyPrefix,
                financialYear = companyFy,
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
        val summary = invoice.items.joinToString(", ") { "${it.title} (x${it.quantity})" }
        val itemsJson = com.digitalchopda.app.core.document.DocumentItemParser.toJson(invoice.items)
        val isEdit = invoice.id > 0L
        val existingInvoice = if (isEdit) db.invoiceDao().getInvoiceById(invoice.id) else null

        val finalInvoiceNumber: String
        val targetId: Long

        if (isEdit && existingInvoice != null) {
            // Edit Mode: Retain invoice number, record revision snapshot, and update record
            finalInvoiceNumber = existingInvoice.invoiceNumber
            val revCount = db.orderRevisionDao().getRevisionCount(existingInvoice.id)
            db.orderRevisionDao().insertRevision(
                OrderRevisionEntity(
                    companyId = company.id,
                    orderId = existingInvoice.id,
                    revisionNumber = revCount + 1,
                    revisionDate = System.currentTimeMillis(),
                    previousTotal = existingInvoice.grandTotal,
                    newTotal = invoice.grandTotal,
                    deltaAmount = invoice.grandTotal - existingInvoice.grandTotal,
                    reason = "Mobile Edit - Rev ${revCount + 1}",
                    itemsSnapshotJson = existingInvoice.itemsJson.ifBlank { "[]" },
                    newItemsSnapshotJson = itemsJson
                )
            )

            val updatedEntity = existingInvoice.copy(
                invoiceNumber = finalInvoiceNumber,
                customerName = invoice.customerName,
                customerPhone = invoice.customerPhone,
                itemsSummary = summary,
                itemsJson = itemsJson,
                subtotal = invoice.subtotal,
                cgst = invoice.cgst,
                sgst = invoice.sgst,
                igst = invoice.igst,
                roundOff = invoice.roundOff,
                grandTotal = invoice.grandTotal,
                paidAmount = if (invoice.status == InvoiceStatus.PAID) invoice.grandTotal else if (invoice.status == InvoiceStatus.PARTIAL) invoice.paidAmount else existingInvoice.paidAmount,
                status = invoice.status.name,
                paymentMode = invoice.paymentMode.name,
                syncStatus = SyncStatus.PENDING_SYNC.name
            )
            db.invoiceDao().updateInvoice(updatedEntity)
            targetId = existingInvoice.id
        } else {
            // New Invoice Creation: Advance Sequence Lease locally (Rule 46)
            val leaseEntity = db.leaseDao().getActiveLease(company.id)
            if (leaseEntity != null) {
                val res = allocateLeaseUseCase.allocateNextInvoiceNumber(leaseEntity.toDomain())
                db.leaseDao().updateLease(
                    leaseEntity.copy(
                        currentCursor = res.updatedLease.currentCursor
                    )
                )
            }
            finalInvoiceNumber = invoice.invoiceNumber

            val entity = InvoiceEntity(
                companyId = company.id,
                invoiceNumber = finalInvoiceNumber,
                customerName = invoice.customerName,
                customerPhone = invoice.customerPhone,
                itemsSummary = summary,
                itemsJson = itemsJson,
                subtotal = invoice.subtotal,
                cgst = invoice.cgst,
                sgst = invoice.sgst,
                igst = invoice.igst,
                roundOff = invoice.roundOff,
                grandTotal = invoice.grandTotal,
                paidAmount = if (invoice.status == InvoiceStatus.PAID) invoice.grandTotal else if (invoice.status == InvoiceStatus.PARTIAL) invoice.paidAmount else 0.0,
                status = invoice.status.name,
                paymentMode = invoice.paymentMode.name,
                syncStatus = SyncStatus.PENDING_SYNC.name,
                idempotencyKey = invoice.idempotencyKey
            )
            targetId = db.invoiceDao().insertInvoice(entity)
        }

        // Central Depot Dispatch: Push to live backend with Idempotency Key
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
                            gstRate = it.gstRatePercent,
                            sku = it.sku
                        )
                    },
                    paymentStatus = when (invoice.status) {
                        InvoiceStatus.PAID -> "paid"
                        InvoiceStatus.PARTIAL -> "partially_paid"
                        InvoiceStatus.UNPAID -> "pending"
                    },
                    paymentMethod = invoice.paymentMode.name.lowercase(),
                    paidAmount = if (invoice.status == InvoiceStatus.PARTIAL) invoice.paidAmount else if (invoice.status == InvoiceStatus.PAID) invoice.grandTotal else null,
                    createInvoice = true,
                    invoiceNumber = finalInvoiceNumber,
                    notes = if (isEdit) "Updated via DigitalChopda Mobile App" else "Created via DigitalChopda Mobile App",
                    clientUuid = invoice.idempotencyKey,
                    idempotencyKey = invoice.idempotencyKey
                )

                if (isEdit && existingInvoice?.serverOrderId != null) {
                    val updateResp = apiService.updateOrder(existingInvoice.serverOrderId, orderRequest)
                    if (updateResp.isSuccessful) {
                        db.invoiceDao().updateSyncStatus(targetId, SyncStatus.SYNCED.name)
                    }
                } else {
                    val resp = apiService.createOrder(orderRequest)
                    if (resp.isSuccessful) {
                        val serverOrderId = resp.body()?.id
                        if (serverOrderId != null) {
                            db.invoiceDao().updateServerOrderIdAndSyncStatus(targetId, serverOrderId, SyncStatus.SYNCED.name)
                        } else {
                            db.invoiceDao().updateSyncStatus(targetId, SyncStatus.SYNCED.name)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Preserves PENDING_SYNC for automatic outbox flush on reconnection
        }

        val finalEntity = db.invoiceDao().getInvoiceById(targetId)
        return Result.success(finalEntity ?: InvoiceEntity(
            id = targetId,
            companyId = company.id,
            invoiceNumber = finalInvoiceNumber,
            customerName = invoice.customerName,
            customerPhone = invoice.customerPhone,
            itemsSummary = summary,
            itemsJson = itemsJson,
            subtotal = invoice.subtotal,
            cgst = invoice.cgst,
            sgst = invoice.sgst,
            igst = invoice.igst,
            roundOff = invoice.roundOff,
            grandTotal = invoice.grandTotal,
            paidAmount = if (invoice.status.name.equals("PAID", ignoreCase = true)) invoice.grandTotal else 0.0,
            status = invoice.status.name,
            paymentMode = invoice.paymentMode.name,
            syncStatus = SyncStatus.PENDING_SYNC.name,
            idempotencyKey = invoice.idempotencyKey
        ))
    }

    suspend fun downloadBackendInvoicePdf(context: Context, backendOrderId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getOrderPdf(backendOrderId)
            if (response.isSuccessful && response.body() != null) {
                val invoicesDir = File(context.filesDir, "invoices").apply { mkdirs() }
                val targetFile = File(invoicesDir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadBackendQuotationPdf(context: Context, quotationId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getQuotationPdf(quotationId)
            if (response.isSuccessful && response.body() != null) {
                val dir = File(context.filesDir, "quotations").apply { mkdirs() }
                val targetFile = File(dir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadBackendShippingLabelPdf(context: Context, backendOrderId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getOrderShippingLabelPdf(backendOrderId)
            if (response.isSuccessful && response.body() != null) {
                val dir = File(context.filesDir, "shipping_labels").apply { mkdirs() }
                val targetFile = File(dir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadQuotationShippingLabelPdf(context: Context, quotationId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getQuotationShippingLabelPdf(quotationId)
            if (response.isSuccessful && response.body() != null) {
                val dir = File(context.filesDir, "shipping_labels").apply { mkdirs() }
                val targetFile = File(dir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteInvoice(invoiceId: Long): Result<Boolean> {
        return try {
            val inv = db.invoiceDao().getInvoiceById(invoiceId)
            if (inv?.serverOrderId != null && apiService != null) {
                try {
                    apiService.deleteOrder(inv.serverOrderId)
                } catch (e: Exception) {
                    android.util.Log.w("BillingRepo", "Remote deleteOrder failed, proceeding locally", e)
                }
            }
            db.invoiceDao().deleteInvoice(invoiceId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInvoiceRevisions(orderId: Long): Flow<List<OrderRevision>> {
        return db.orderRevisionDao().getRevisionsForOrder(orderId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun markWhatsAppDispatched(invoiceId: Long) {
        db.invoiceDao().updateWhatsAppSent(invoiceId, System.currentTimeMillis())
    }

    suspend fun editInvoiceWithRevision(
        orderId: Long,
        updatedItems: List<com.digitalchopda.app.domain.model.LineItem>,
        reason: String? = null
    ): Result<InvoiceEntity> {
        return try {
            val invoice = db.invoiceDao().getInvoiceById(orderId)
                ?: return Result.failure(IllegalArgumentException("Invoice not found"))

            val oldItemsJson = invoice.itemsJson
            val oldGrandTotal = invoice.grandTotal

            val newSubtotal = updatedItems.sumOf { it.subtotal }
            val newTax = updatedItems.sumOf { it.gstAmount }
            val newGrandTotal = updatedItems.sumOf { it.total }
            val deltaAmount = newGrandTotal - oldGrandTotal
            val newItemsJson = com.digitalchopda.app.core.document.DocumentItemParser.toJson(updatedItems)
            val newSummary = updatedItems.joinToString(", ") { "${it.title} (x${it.quantity})" }

            val revCount = db.orderRevisionDao().getRevisionCount(orderId)
            val revision = OrderRevisionEntity(
                companyId = invoice.companyId,
                orderId = orderId,
                revisionNumber = revCount + 1,
                revisionDate = System.currentTimeMillis(),
                previousTotal = oldGrandTotal,
                newTotal = newGrandTotal,
                deltaAmount = deltaAmount,
                reason = reason ?: "Order Modified",
                itemsSnapshotJson = oldItemsJson.ifBlank { "[]" },
                newItemsSnapshotJson = newItemsJson
            )
            db.orderRevisionDao().insertRevision(revision)

            // Recalculate status preserving payments
            val paidAmount = invoice.paidAmount
            val newStatus = when {
                paidAmount >= newGrandTotal && newGrandTotal > 0 -> "PAID"
                paidAmount > 0 -> "PARTIAL"
                else -> "UNPAID"
            }

            val updatedInvoice = invoice.copy(
                itemsSummary = newSummary,
                itemsJson = newItemsJson,
                subtotal = newSubtotal,
                cgst = newTax / 2.0,
                sgst = newTax / 2.0,
                grandTotal = newGrandTotal,
                status = newStatus,
                syncStatus = "PENDING_SYNC"
            )
            db.invoiceDao().updateInvoice(updatedInvoice)

            val customer = db.customerDao().getCustomerByPhone(invoice.companyId, invoice.customerPhone)
            if (customer != null) {
                db.customerDao().updateBalance(customer.id, deltaAmount)
            }

            Result.success(updatedInvoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchOrdersSummary(): com.digitalchopda.app.core.network.ApiOrdersSummaryResponse? {
        val api = apiService ?: return null
        return try {
            val res = api.getOrdersSummary()
            if (res.isSuccessful) res.body() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchDashboardSummary(): com.digitalchopda.app.core.network.ApiDashboardSummaryResponse? {
        val api = apiService ?: return null
        return try {
            val res = api.getDashboardSummary()
            if (res.isSuccessful) res.body() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveAndDispatchQuotation(company: Company, invoice: Invoice): Result<Long> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client configured"))
        return try {
            val req = ApiQuotationRequest(
                phone = invoice.customerPhone.ifBlank { "9999999999" },
                name = invoice.customerName.ifBlank { "Customer / Prospect" },
                items = invoice.items.map {
                    ApiQuotationItemRequest(
                        productName = it.title,
                        quantity = it.quantity.toDouble(),
                        rate = it.unitPrice,
                        gstRate = it.gstRatePercent,
                        unit = "PCS"
                    )
                },
                termsAndConditions = "Standard commercial terms apply. Valid for 15 days from issue date."
            )
            val resp = api.createQuotation(req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!.id)
            } else {
                Result.failure(Exception("Quotation creation failed: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackendStatementPdf(context: Context, customerId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getCustomerStatementPdf(customerId)
            if (response.isSuccessful && response.body() != null) {
                val dir = File(context.filesDir, "statements").apply { mkdirs() }
                val targetFile = File(dir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadBackendPaymentReceiptPdf(context: Context, paymentId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getPaymentReceiptPdf(paymentId)
            if (response.isSuccessful && response.body() != null) {
                val dir = File(context.filesDir, "receipts").apply { mkdirs() }
                val targetFile = File(dir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun syncPendingInvoices(): Int {
        val api = apiService ?: return 0
        val pending = db.invoiceDao().getPendingSyncInvoices()
        var syncedCount = 0
        for (inv in pending) {
            try {
                val parsedItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                    itemsJson = inv.itemsJson,
                    itemsSummary = inv.itemsSummary,
                    defaultSubtotal = inv.subtotal
                )
                val orderItems = if (parsedItems.isNotEmpty()) {
                    parsedItems.map { item ->
                        ApiOrderItemRequest(
                            productName = item.title.ifBlank { "Item" },
                            quantity = item.quantity.toDouble(),
                            unitPrice = item.unitPrice,
                            gstRate = item.gstRatePercent
                        )
                    }
                } else {
                    listOf(
                        ApiOrderItemRequest(
                            productName = inv.itemsSummary.ifBlank { "General Items" },
                            quantity = 1.0,
                            unitPrice = inv.subtotal,
                            gstRate = 18.0
                        )
                    )
                }

                val isPaid = inv.status.equals("PAID", ignoreCase = true)
                val isPartial = inv.status.equals("PARTIAL", ignoreCase = true) || inv.status.equals("PARTIALLY_PAID", ignoreCase = true)
                val mappedPaymentStatus = when {
                    isPaid -> "paid"
                    isPartial -> "partially_paid"
                    else -> "pending"
                }
                val paidAmt = when {
                    isPartial -> inv.paidAmount
                    isPaid -> inv.grandTotal
                    else -> null
                }

                val orderRequest = ApiCreateOrderRequest(
                    phone = inv.customerPhone.ifBlank { "9999999999" },
                    name = inv.customerName.ifBlank { "Walk-in Customer" },
                    items = orderItems,
                    paymentStatus = mappedPaymentStatus,
                    paymentMethod = inv.paymentMode.lowercase(),
                    paidAmount = paidAmt,
                    createInvoice = true,
                    invoiceNumber = inv.invoiceNumber,
                    roundOff = inv.roundOff,
                    notes = "Synced from Mobile Outbox",
                    clientReferenceId = inv.clientReferenceId,
                    clientUuid = inv.idempotencyKey,
                    idempotencyKey = inv.idempotencyKey
                )
                val res = api.createOrder(orderRequest)
                if (res.isSuccessful) {
                    val serverId = res.body()?.id ?: 0L
                    db.invoiceDao().updateServerOrderIdAndSyncStatus(inv.id, serverId, SyncStatus.SYNCED.name)
                    syncedCount++
                } else {
                    db.invoiceDao().incrementSyncAttempts(inv.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("BillingRepo", "Failed to sync invoice ${inv.invoiceNumber}", e)
                try {
                    db.invoiceDao().incrementSyncAttempts(inv.id)
                } catch (_: Exception) {}
            }
        }
        return syncedCount
    }

    suspend fun syncInvoiceById(localId: Long): Result<InvoiceEntity> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client configured"))
        val inv = db.invoiceDao().getInvoiceById(localId)
            ?: return Result.failure(IllegalArgumentException("Invoice with ID $localId not found"))
        if (inv.serverOrderId != null && inv.serverOrderId > 0L) {
            return Result.success(inv)
        }
        return try {
            val parsedItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                itemsJson = inv.itemsJson,
                itemsSummary = inv.itemsSummary,
                defaultSubtotal = inv.subtotal
            )
            val orderItems = if (parsedItems.isNotEmpty()) {
                parsedItems.map { item ->
                    ApiOrderItemRequest(
                        productName = item.title.ifBlank { "Item" },
                        quantity = item.quantity.toDouble(),
                        unitPrice = item.unitPrice,
                        gstRate = item.gstRatePercent
                    )
                }
            } else {
                listOf(
                    ApiOrderItemRequest(
                        productName = inv.itemsSummary.ifBlank { "General Items" },
                        quantity = 1.0,
                        unitPrice = inv.subtotal,
                        gstRate = 18.0
                    )
                )
            }

            val orderRequest = ApiCreateOrderRequest(
                phone = inv.customerPhone.ifBlank { "9999999999" },
                name = inv.customerName.ifBlank { "Walk-in Customer" },
                items = orderItems,
                paymentStatus = when (inv.status.uppercase()) {
                    "PAID" -> "paid"
                    "PARTIAL", "PARTIALLY_PAID" -> "partially_paid"
                    else -> "pending"
                },
                paymentMethod = inv.paymentMode.lowercase(),
                paidAmount = if (inv.status.equals("PARTIAL", ignoreCase = true) || inv.status.equals("PARTIALLY_PAID", ignoreCase = true)) inv.paidAmount else if (inv.status.equals("PAID", ignoreCase = true)) inv.grandTotal else null,
                createInvoice = true,
                invoiceNumber = inv.invoiceNumber,
                roundOff = inv.roundOff,
                notes = "Synced on demand",
                clientReferenceId = inv.clientReferenceId,
                clientUuid = inv.idempotencyKey,
                idempotencyKey = inv.idempotencyKey
            )
            val res = api.createOrder(orderRequest)
            if (res.isSuccessful && res.body() != null) {
                val serverId = res.body()!!.id
                db.invoiceDao().updateServerOrderIdAndSyncStatus(inv.id, serverId, SyncStatus.SYNCED.name)
                Result.success(inv.copy(serverOrderId = serverId, syncStatus = SyncStatus.SYNCED.name))
            } else {
                Result.failure(Exception("Server returned ${res.code()}: ${res.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingPayments(): Int {
        val api = apiService ?: return 0
        val pending = db.paymentDao().getPendingSyncPayments()
        var syncedCount = 0
        for (pmt in pending) {
            try {
                val resolvedOrderId = pmt.orderId ?: if (pmt.invoiceId != null) {
                    db.invoiceDao().getInvoiceById(pmt.invoiceId)?.serverOrderId
                } else null

                val resolvedContact = if (pmt.customerPhone.isNotBlank()) {
                    db.customerDao().getCustomerByPhone(pmt.companyId, pmt.customerPhone)
                } else if (pmt.customerId != null) {
                    db.customerDao().getCustomerById(pmt.customerId)
                } else null

                val resolvedPhone = pmt.customerPhone.ifBlank { resolvedContact?.phone ?: "" }
                val resolvedContactId = pmt.serverContactId ?: resolvedContact?.serverContactId ?: pmt.customerId

                val isDebit = !pmt.isJama // true = You Got (Credit), false = You Gave (Debit)
                if (isDebit) {
                    if (resolvedContactId == null) {
                        continue
                    }
                    val khataReq = com.digitalchopda.app.core.network.ApiCreateKhataTransactionRequest(
                        contactId = resolvedContactId,
                        entryType = "debit",
                        amount = pmt.amount,
                        transactionDate = null,
                        particulars = pmt.notes.ifBlank { "Debit" },
                        paymentMode = pmt.paymentMode.lowercase(),
                        referenceNo = pmt.receiptNumber.ifBlank { null },
                        notes = pmt.notes.ifBlank { null },
                        clientReferenceId = pmt.clientReferenceId
                    )
                    val res = api.createKhataTransaction(khataReq)
                    if (res.isSuccessful) {
                        val body = res.body()
                        val serverId = body?.id ?: 0L
                        db.paymentDao().markSynced(
                            id = pmt.id,
                            serverId = serverId,
                            receiptNumber = pmt.receiptNumber,
                            syncStatus = "SYNCED"
                        )
                        syncedCount++
                    }
                } else {
                    val req = ApiPaymentInRequest(
                        contactId = resolvedContactId,
                        phone = resolvedPhone.ifBlank { null },
                        name = pmt.customerName.ifBlank { resolvedContact?.name },
                        amount = pmt.amount,
                        paymentMode = pmt.paymentMode.lowercase(),
                        orderId = resolvedOrderId,
                        notes = pmt.notes.ifBlank { "Mobile Payment In" },
                        paidAt = null,
                        clientReferenceId = pmt.clientReferenceId
                    )
                    val res = api.createPaymentIn(req)
                    if (res.isSuccessful) {
                        val body = res.body()
                        val serverId = body?.id ?: 0L
                        val receiptNo = body?.receiptNumber ?: pmt.receiptNumber
                        db.paymentDao().markSynced(
                            id = pmt.id,
                            serverId = serverId,
                            receiptNumber = receiptNo,
                            syncStatus = "SYNCED"
                        )
                        syncedCount++
                    } else if (res.code() == 404 && resolvedPhone.isBlank() && pmt.customerName.isBlank()) {
                        // Orphaned payment voucher from older test seed with no customer details - prune to prevent deadlock
                        db.paymentDao().markSynced(
                            id = pmt.id,
                            serverId = 0L,
                            receiptNumber = pmt.receiptNumber,
                            syncStatus = "SYNCED"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BillingRepo", "Failed to sync payment ${pmt.id}", e)
            }
        }
        return syncedCount
    }

    suspend fun getRecentInvoices(companyId: Long, limit: Int = 10): List<InvoiceEntity> {
        return db.invoiceDao().getRecentInvoices(companyId, limit)
    }

    fun getPaymentsForCompany(companyId: Long): Flow<List<PaymentEntity>> {
        return db.paymentDao().getPaymentsForCompany(companyId)
    }

    suspend fun voidPayment(paymentId: Long, reason: String = "Voided by user"): Result<Unit> {
        return try {
            val p = db.paymentDao().getPaymentById(paymentId)
                ?: return Result.failure(IllegalArgumentException("Payment not found"))

            // Reverse customer balance
            if (p.customerId != null) {
                val reverseDelta = if (p.isJama) p.amount else -p.amount
                db.customerDao().updateBalance(p.customerId, reverseDelta)
            }

            // If attached to an invoice, revert paid amount
            if (p.invoiceId != null) {
                val inv = db.invoiceDao().getInvoiceById(p.invoiceId)
                if (inv != null) {
                    val newPaid = (inv.paidAmount - p.amount).coerceAtLeast(0.0)
                    val newStatus = if (newPaid <= 0.001) "PENDING" else "PARTIAL"
                    db.invoiceDao().updatePaidAmountAndStatus(p.invoiceId, newPaid, newStatus)
                }
            }

            // Push delete to server if online
            if (apiService != null && p.serverPaymentId != null) {
                try {
                    apiService.deletePayment(p.serverPaymentId)
                } catch (e: Exception) {
                    android.util.Log.w("BillingRepo", "Server payment void deferred or failed: ${e.message}")
                }
            }

            // Remove locally
            db.paymentDao().deletePayment(paymentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class InventoryRepository(
    private val db: ChopdaDatabase,
    private val apiService: DigitalChopdaApiService? = null
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
            barcode = item.barcode,
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

    suspend fun syncProductsWithServer(companyId: Long): Result<Int> = withContext(Dispatchers.IO) {
        val api = apiService ?: return@withContext Result.failure(IllegalStateException("No network client"))
        try {
            val response = api.getProducts()
            if (response.isSuccessful && response.body() != null) {
                val remoteItems = response.body()!!
                val entities = remoteItems.map { p ->
                    ItemEntity(
                        companyId = companyId,
                        sku = p.sku ?: "PROD-${p.id}",
                        barcode = p.barcode ?: "",
                        itemName = p.name,
                        packSize = "1 Unit",
                        quantity = p.stockQuantity.toInt(),
                        sellingPrice = p.defaultPrice,
                        costPrice = p.defaultPrice * 0.7,
                        gstRatePercent = p.gstRate ?: 18.0,
                        minStockThreshold = 5,
                        version = p.version
                    )
                }
                // Atomically chunk upserts in background transaction to prevent excessive Room flow recompositions
                db.itemDao().upsertItemsBatch(entities, chunkSize = 100)
                Result.success(remoteItems.size)
            } else {
                Result.failure(Exception("Failed to fetch products: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncStockDeltaWithServer(itemId: Long, delta: Int, baseVersion: Int): Result<com.digitalchopda.app.core.network.StockRebaseResponse> {
        if (apiService != null) {
            try {
                val resp = apiService.rebaseStockDelta(
                    com.digitalchopda.app.core.network.ApiStockSyncDeltaRequest(
                        itemId = itemId,
                        deltaQuantity = delta,
                        baseVersion = baseVersion
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    val newQty = body.updatedQuantity ?: 0
                    val newVer = body.updatedVersion ?: (baseVersion + 1)
                    db.itemDao().updateStockQuantityAndVersion(itemId, newQty, newVer, System.currentTimeMillis())
                    return Result.success(
                        com.digitalchopda.app.core.network.StockRebaseResponse(
                            success = true,
                            itemId = itemId,
                            updatedQuantity = newQty,
                            updatedVersion = newVer,
                            rebasedCleanly = body.rebasedCleanly ?: true
                        )
                    )
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(IllegalStateException("No network client configured for stock sync"))
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
    fun getCustomers(companyId: Long): Flow<List<CustomerEntity>> = getCustomersForCompany(companyId)

    fun getCustomersForCompany(companyId: Long): Flow<List<CustomerEntity>> {
        return db.customerDao().getCustomersForCompany(companyId)
    }

    suspend fun getCustomerById(id: Long): CustomerEntity? {
        return db.customerDao().getCustomerById(id)
    }

    suspend fun getCustomerByPhone(companyId: Long, phone: String): CustomerEntity? {
        return db.customerDao().getCustomerByPhone(companyId, phone)
    }

    suspend fun addCustomer(customer: CustomerEntity): Long {
        val res = createParty(customer)
        return res.getOrDefault(0L)
    }

    suspend fun updateBalance(customerId: Long, delta: Double) {
        db.customerDao().updateBalance(customerId, delta)
    }

    suspend fun updateLeadInfo(id: Long, priority: String, expectedDealValue: Double, notes: String, followUpDate: Long?) {
        db.customerDao().updateLeadInfo(id, priority, expectedDealValue, notes, followUpDate)
    }

    suspend fun updateCreditLimit(id: Long, limit: Double) {
        db.customerDao().updateCreditLimit(id, limit)
    }

    fun searchCustomers(companyId: Long, query: String): Flow<List<CustomerEntity>> {
        return db.customerDao().searchCustomers(companyId, query)
    }

    suspend fun deleteCustomer(id: Long) {
        db.customerDao().deleteCustomer(id)
    }

    fun getUnpaidInvoicesForParty(companyId: Long, phone: String, name: String): Flow<List<InvoiceEntity>> {
        return db.invoiceDao().getUnpaidInvoicesForCustomer(companyId, phone, name)
    }

    suspend fun getUnpaidInvoicesForPartySync(companyId: Long, phone: String, name: String): List<InvoiceEntity> {
        return db.invoiceDao().getUnpaidInvoicesForCustomerSync(companyId, phone, name)
    }

    /**
     * 🌐 Complete 360° Party Ledger & Transaction Aggregator
     * Computes running balance across Invoices (debit), Payments (credit/debit), and Opening Balance.
     */
    fun getParty360Stream(companyId: Long, customerId: Long): Flow<Party360Detail?> = combine(
        db.customerDao().getCustomersForCompany(companyId),
        db.invoiceDao().getInvoicesForCompany(companyId),
        db.quotationDao().getQuotationsForCompany(companyId),
        db.paymentDao().getPaymentsForCompany(companyId)
    ) { allCustomers, allInvoices, allQuotations, allPayments ->
        val customer = allCustomers.find { it.id == customerId } ?: return@combine null

        val customerPhone = customer.phone
        val customerName = customer.name

        val partyInvoices = allInvoices.filter {
            it.customerPhone == customerPhone || it.customerName.equals(customerName, ignoreCase = true)
        }

        val partyQuotations = allQuotations.filter {
            it.customerPhone == customerPhone || it.customerName.equals(customerName, ignoreCase = true)
        }

        val partyPayments = allPayments.filter {
            it.customerId == customerId || it.customerPhone == customerPhone || it.customerName.equals(customerName, ignoreCase = true)
        }

        // Assemble raw ledger entries
        val rawEntries = mutableListOf<PartyLedgerEntry>()

        // 1. Opening Balance (Explicit or Synced from Party Balance)
        val txNet = partyInvoices.sumOf { it.grandTotal } -
                partyPayments.filter { it.isJama }.sumOf { it.amount } +
                partyPayments.filter { !it.isJama }.sumOf { it.amount }

        val hasExplicitOpen = customer.openingBalance != 0.0
        val effectiveOpeningBalance = when {
            hasExplicitOpen -> customer.openingBalance
            kotlin.math.abs(customer.balance - txNet) > 0.01 -> kotlin.math.abs(customer.balance - txNet)
            partyInvoices.isEmpty() && partyPayments.isEmpty() && customer.balance != 0.0 -> kotlin.math.abs(customer.balance)
            else -> 0.0
        }
        val effectiveOpeningType = when {
            hasExplicitOpen -> customer.openingBalanceType
            (customer.balance - txNet) >= 0 -> "to_receive"
            else -> "to_pay"
        }

        if (effectiveOpeningBalance > 0.0) {
            val isToReceive = effectiveOpeningType.equals("to_receive", ignoreCase = true)
            rawEntries.add(
                PartyLedgerEntry(
                    id = "open-${customer.id}",
                    date = customer.openingBalanceDate ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000),
                    voucherType = "Opening Balance",
                    refNumber = "OPEN-BAL",
                    particulars = "Opening Balance (${if (isToReceive) "To Receive" else "To Pay"})",
                    debit = if (isToReceive) effectiveOpeningBalance else 0.0,
                    credit = if (!isToReceive) effectiveOpeningBalance else 0.0,
                    runningBalance = 0.0,
                    paymentMode = "—"
                )
            )
        }

        // 2. Sales Invoices (Debits to customer / Udhar)
        for (inv in partyInvoices) {
            rawEntries.add(
                PartyLedgerEntry(
                    id = "inv-${inv.id}",
                    date = inv.createdAt,
                    voucherType = "Sales Invoices",
                    refNumber = inv.invoiceNumber,
                    particulars = inv.itemsSummary.ifBlank { "Tax Invoice" },
                    debit = inv.grandTotal,
                    credit = 0.0,
                    runningBalance = 0.0,
                    paymentMode = inv.paymentMode
                )
            )
        }

        // 3. Payments (Jama / Udhar)
        for (pay in partyPayments) {
            val isCredit = pay.isJama
            val particularText = when {
                pay.invoiceNumber.isNotBlank() -> "Against ${pay.invoiceNumber}" + (if (pay.notes.isNotBlank()) " - ${pay.notes}" else "")
                pay.notes.isNotBlank() -> pay.notes
                isCredit -> "Payment Received (Cash/UPI)"
                else -> "Amount Paid Out"
            }
            rawEntries.add(
                PartyLedgerEntry(
                    id = "pay-${pay.id}",
                    date = pay.createdAt,
                    voucherType = if (isCredit) "Payment In" else "Payment Out",
                    refNumber = pay.receiptNumber.ifBlank { "REC-${pay.id}" },
                    particulars = particularText,
                    debit = if (!isCredit) pay.amount else 0.0,
                    credit = if (isCredit) pay.amount else 0.0,
                    runningBalance = 0.0,
                    paymentMode = pay.paymentMode.uppercase()
                )
            )
        }

        // Sort chronologically ascending for accurate running balance accumulation
        rawEntries.sortBy { it.date }

        var running = 0.0
        var totalBilled = 0.0
        var totalCollected = 0.0

        val computedEntries = rawEntries.map { entry ->
            running = running + entry.debit - entry.credit
            if (entry.voucherType == "Sales Invoices" || (entry.voucherType == "Opening Balance" && entry.debit > 0)) {
                totalBilled += entry.debit
            }
            if (entry.credit > 0) {
                totalCollected += entry.credit
            }
            entry.copy(runningBalance = Math.round(running * 100.0) / 100.0)
        }

        Party360Detail(
            customer = customer,
            invoices = partyInvoices,
            quotations = partyQuotations,
            payments = partyPayments,
            ledgerEntries = computedEntries,
            totalBilled = totalBilled,
            totalCollected = totalCollected,
            netOutstanding = if (computedEntries.isNotEmpty()) running else customer.balance
        )
    }

    suspend fun recordPayment(
        companyId: Long,
        customerId: Long,
        amount: Double,
        isJama: Boolean,
        paymentMode: String,
        notes: String,
        invoiceId: Long? = null,
        orderId: Long? = null,
        invoiceNumber: String? = null,
        allocations: List<com.digitalchopda.app.core.network.ApiPaymentAllocationItemDto>? = null
    ): Long {
        val paymentId = db.withTransaction {
            val customer = db.customerDao().getCustomerById(customerId) ?: return@withTransaction -1L
            val receiptNo = "REC-${System.currentTimeMillis() % 100000}"

            var targetInvoiceId = invoiceId
            var targetOrderId = orderId
            var targetInvoiceNumber = invoiceNumber ?: ""

            if (!allocations.isNullOrEmpty()) {
                val matchedNumbers = mutableListOf<String>()
                for (item in allocations) {
                    val invoice = db.invoiceDao().getInvoiceByServerOrderId(item.orderId)
                        ?: db.invoiceDao().getInvoiceById(item.orderId)
                    if (invoice != null) {
                        matchedNumbers.add(invoice.invoiceNumber)
                        val newPaidAmount = invoice.paidAmount + item.amount
                        val newStatus = if (newPaidAmount >= (invoice.grandTotal - 0.001)) "PAID" else "PARTIAL"
                        db.invoiceDao().updatePaidAmountAndStatus(invoice.id, newPaidAmount, newStatus)
                        if (targetInvoiceId == null) targetInvoiceId = invoice.id
                        if (targetOrderId == null) targetOrderId = invoice.serverOrderId
                    }
                }
                if (matchedNumbers.isNotEmpty()) {
                    targetInvoiceNumber = matchedNumbers.joinToString(", ")
                }
            } else if (targetInvoiceId != null) {
                val invoice = db.invoiceDao().getInvoiceById(targetInvoiceId)
                if (invoice != null) {
                    if (targetInvoiceNumber.isBlank()) {
                        targetInvoiceNumber = invoice.invoiceNumber
                    }
                    if (targetOrderId == null) {
                        targetOrderId = invoice.serverOrderId
                    }
                    val newPaidAmount = invoice.paidAmount + amount
                    val newStatus = if (newPaidAmount >= (invoice.grandTotal - 0.001)) "PAID" else "PARTIAL"
                    db.invoiceDao().updatePaidAmountAndStatus(targetInvoiceId, newPaidAmount, newStatus)
                }
            } else if (targetOrderId != null) {
                val invoice = db.invoiceDao().getInvoiceByServerOrderId(targetOrderId)
                if (invoice != null) {
                    targetInvoiceId = invoice.id
                    if (targetInvoiceNumber.isBlank()) {
                        targetInvoiceNumber = invoice.invoiceNumber
                    }
                    val newPaidAmount = invoice.paidAmount + amount
                    val newStatus = if (newPaidAmount >= (invoice.grandTotal - 0.001)) "PAID" else "PARTIAL"
                    db.invoiceDao().updatePaidAmountAndStatus(invoice.id, newPaidAmount, newStatus)
                }
            }

            val payment = PaymentEntity(
                companyId = companyId,
                customerId = customerId,
                customerName = customer.name,
                customerPhone = customer.phone,
                amount = amount,
                isJama = isJama,
                paymentMode = paymentMode.lowercase(),
                receiptNumber = receiptNo,
                notes = notes,
                syncStatus = "PENDING_SYNC",
                invoiceId = targetInvoiceId,
                orderId = targetOrderId,
                invoiceNumber = targetInvoiceNumber,
                isSettlement = !allocations.isNullOrEmpty() || targetInvoiceId != null
            )
            val insertedId = db.paymentDao().insertPayment(payment)
            val delta = if (isJama) -amount else amount
            db.customerDao().updateBalance(customerId, delta)
            insertedId
        }

        if (paymentId > 0 && apiService != null) {
            withContext(Dispatchers.IO) {
                try {
                    val pmt = db.paymentDao().getPaymentById(paymentId) ?: return@withContext
                    val customer = db.customerDao().getCustomerById(customerId)
                    val resolvedContactId = customer?.serverContactId ?: customerId
                    if (isJama) {
                        val req = ApiPaymentInRequest(
                            contactId = resolvedContactId,
                            phone = customer?.phone?.ifBlank { null },
                            name = customer?.name?.ifBlank { null },
                            amount = amount,
                            paymentMode = paymentMode.lowercase(),
                            orderId = pmt.orderId,
                            notes = notes.ifBlank { "Mobile Payment In" },
                            paidAt = null,
                            allocations = allocations
                        )
                        val res = apiService.createPaymentIn(req)
                        if (res.isSuccessful && res.body() != null) {
                            val body = res.body()!!
                            db.paymentDao().markSynced(
                                id = paymentId,
                                serverId = body.id ?: 0L,
                                receiptNumber = body.receiptNumber ?: pmt.receiptNumber,
                                syncStatus = "SYNCED"
                            )
                        }
                    } else {
                        val khataReq = com.digitalchopda.app.core.network.ApiCreateKhataTransactionRequest(
                            contactId = resolvedContactId,
                            entryType = "debit",
                            amount = amount,
                            transactionDate = null,
                            particulars = notes.ifBlank { "Payment Out" },
                            paymentMode = paymentMode.lowercase(),
                            referenceNo = pmt.receiptNumber.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                        val res = apiService.createKhataTransaction(khataReq)
                        if (res.isSuccessful && res.body() != null) {
                            db.paymentDao().markSynced(
                                id = paymentId,
                                serverId = res.body()!!.id,
                                receiptNumber = pmt.receiptNumber,
                                syncStatus = "SYNCED"
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerRepo", "Failed immediate payment sync: ${e.message}")
                }
            }
        }
        return paymentId
    }

    suspend fun createParty(party: CustomerEntity): Result<Long> {
        return try {
            val safeClientRefId = party.clientReferenceId.ifBlank { java.util.UUID.randomUUID().toString() }
            val entityToSave = if (party.clientReferenceId != safeClientRefId) party.copy(clientReferenceId = safeClientRefId) else party
            val localId = db.customerDao().insertCustomer(entityToSave)
            if (apiService != null) {
                try {
                    val req = com.digitalchopda.app.core.network.ApiCreateContactRequest(
                        name = entityToSave.name,
                        phone = entityToSave.phone,
                        partyType = entityToSave.partyType,
                        partyTag = entityToSave.partyTag,
                        openingBalance = entityToSave.openingBalance,
                        openingBalanceType = entityToSave.openingBalanceType,
                        creditLimit = entityToSave.creditLimit,
                        gstin = entityToSave.gstin.ifBlank { null },
                        pan = entityToSave.pan.ifBlank { null },
                        billingAddress = entityToSave.address.ifBlank { null },
                        city = entityToSave.city.ifBlank { null },
                        state = entityToSave.state.ifBlank { null },
                        clientReferenceId = safeClientRefId
                    )
                    val createRes = apiService.createContact(req)
                    if (createRes.isSuccessful && createRes.body() != null) {
                        val serverContact = createRes.body()!!
                        db.customerDao().updateServerContactId(
                            id = localId,
                            serverContactId = serverContact.id,
                            outstanding = serverContact.outstanding ?: serverContact.balance ?: party.balance
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerRepo", "Failed to post party to server: ${e.message}")
                }
            }
            Result.success(localId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncContactsWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            // First push any unsynced offline customers created locally
            val unsynced = db.customerDao().getUnsyncedCustomers(companyId)
            for (u in unsynced) {
                try {
                    val req = com.digitalchopda.app.core.network.ApiCreateContactRequest(
                        name = u.name,
                        phone = com.digitalchopda.app.util.PhoneSanitizer.normalize(u.phone),
                        partyType = u.partyType,
                        partyTag = u.partyTag,
                        openingBalance = u.openingBalance,
                        openingBalanceType = u.openingBalanceType,
                        creditLimit = u.creditLimit,
                        gstin = u.gstin.ifBlank { null },
                        pan = u.pan.ifBlank { null },
                        billingAddress = u.address.ifBlank { null },
                        city = u.city.ifBlank { null },
                        state = u.state.ifBlank { null },
                        clientReferenceId = u.clientReferenceId
                    )
                    val createRes = api.createContact(req)
                    if (createRes.isSuccessful && createRes.body() != null) {
                        val serverContact = createRes.body()!!
                        db.customerDao().updateServerContactId(
                            id = u.id,
                            serverContactId = serverContact.id,
                            outstanding = serverContact.outstanding ?: serverContact.balance ?: u.balance
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomerRepo", "Error syncing local contact ${u.phone}", e)
                }
            }

            val res = api.getContacts()
            if (res.isSuccessful && res.body() != null) {
                val contacts = res.body()!!
                val localCustomers = db.customerDao().getCustomersSync(companyId)
                val byServerId = localCustomers.filter { it.serverContactId != null }.associateBy { it.serverContactId }
                val byPhone = localCustomers.associateBy { it.phone }
                val byNormalizedPhone = localCustomers.filter { it.phone.isNotBlank() }.associateBy { com.digitalchopda.app.util.PhoneSanitizer.normalize(it.phone) }

                for (c in contacts) {
                    val normPhone = com.digitalchopda.app.util.PhoneSanitizer.normalize(c.phone)
                    val existing = byServerId[c.id]
                        ?: byPhone[c.phone]
                        ?: byNormalizedPhone[normPhone]
                    val entity = CustomerEntity(
                        id = existing?.id ?: 0,
                        companyId = companyId,
                        name = c.name ?: "Contact ${c.phone}",
                        phone = c.phone,
                        gstin = c.gstin ?: "",
                        pan = c.pan ?: "",
                        balance = c.outstanding ?: c.balance ?: 0.0,
                        address = c.billingAddress ?: c.city ?: "",
                        city = c.city ?: "",
                        state = c.state ?: "",
                        creditLimit = c.creditLimit ?: 0.0,
                        openingBalance = c.openingBalance ?: 0.0,
                        openingBalanceType = c.openingBalanceType ?: if ((c.outstanding ?: c.balance ?: 0.0) >= 0) "to_receive" else "to_pay",
                        partyType = c.partyType ?: "customer",
                        partyTag = c.partyTag ?: "Active Party",
                        serverContactId = c.id,
                        clientReferenceId = existing?.clientReferenceId ?: java.util.UUID.randomUUID().toString(),
                        serverOutstanding = c.outstanding ?: c.balance ?: 0.0
                    )
                    db.customerDao().insertCustomer(entity)
                }

                // 🧹 Authoritative Reconciliation: Prune stale or reseeded parties that no longer exist on Central Depot
                val remoteServerIds = contacts.map { it.id }.toSet()
                val currentLocal = db.customerDao().getCustomersSync(companyId)
                val stale = currentLocal.filter { it.serverContactId != null && it.serverContactId !in remoteServerIds }
                for (s in stale) {
                    db.customerDao().deleteCustomer(s.id)
                }

                // Deduplicate local records sharing the same serverContactId
                val remaining = db.customerDao().getCustomersSync(companyId)
                val groupedByServerId = remaining.filter { it.serverContactId != null }.groupBy { it.serverContactId!! }
                for ((_, duplicates) in groupedByServerId) {
                    if (duplicates.size > 1) {
                        val sorted = duplicates.sortedByDescending { it.id }
                        for (dup in sorted.drop(1)) {
                            db.customerDao().deleteCustomer(dup.id)
                        }
                    }
                }

                Result.success(contacts.size)
            } else {
                Result.failure(Exception("Failed to fetch contacts: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPaymentOut(
        companyId: Long,
        partyId: Long,
        amount: Double,
        paymentMode: String,
        referenceNo: String,
        notes: String
    ): Result<Long> {
        return try {
            val id = db.withTransaction {
                val customer = db.customerDao().getCustomerById(partyId)
                val supplier = if (customer == null) db.supplierDao().getSupplierById(partyId) else null

                if (customer == null && supplier == null) {
                    throw IllegalArgumentException("Party not found")
                }

                val phone = customer?.phone ?: supplier?.phone ?: ""
                val name = customer?.name ?: supplier?.name ?: "Supplier"

                // Reduce balance / debt
                if (customer != null) {
                    db.customerDao().updateBalance(partyId, -amount)
                } else if (supplier != null) {
                    db.supplierDao().updateBalance(partyId, -amount)
                }

                val payment = PaymentEntity(
                    companyId = companyId,
                    customerId = partyId,
                    customerPhone = phone,
                    customerName = name,
                    amount = amount,
                    isJama = false, // Debit: You Gave money
                    isSettlement = true,
                    paymentMode = paymentMode.lowercase(),
                    receiptNumber = referenceNo.ifBlank { "PAY-${System.currentTimeMillis() % 100000}" },
                    notes = notes.ifBlank { "Supplier Payment Out" },
                    syncStatus = "PENDING_SYNC"
                )
                db.paymentDao().insertPayment(payment)
            }
            if (id > 0 && apiService != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val customer = db.customerDao().getCustomerById(partyId)
                        val resolvedContactId = customer?.serverContactId ?: partyId
                        val khataReq = com.digitalchopda.app.core.network.ApiCreateKhataTransactionRequest(
                            contactId = resolvedContactId,
                            entryType = "debit",
                            amount = amount,
                            transactionDate = null,
                            particulars = notes.ifBlank { "Supplier Payment Out" },
                            paymentMode = paymentMode.lowercase(),
                            referenceNo = referenceNo.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                        val res = apiService.createKhataTransaction(khataReq)
                        if (res.isSuccessful && res.body() != null) {
                            db.paymentDao().markSynced(
                                id = id,
                                serverId = res.body()!!.id,
                                receiptNumber = referenceNo.ifBlank { "PAY-$id" },
                                syncStatus = "SYNCED"
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CustomerRepo", "Failed immediate payment out sync: ${e.message}")
                    }
                }
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class QuotationRepository(
    private val db: ChopdaDatabase,
    val apiService: DigitalChopdaApiService? = null
) {
    fun getQuotationsForCompany(companyId: Long): Flow<List<QuotationEntity>> {
        return db.quotationDao().getQuotationsForCompany(companyId)
    }

    fun getQuotationsForCustomer(companyId: Long, phone: String, name: String): Flow<List<QuotationEntity>> {
        return db.quotationDao().getQuotationsForCustomer(companyId, phone, name)
    }

    suspend fun getQuotationById(id: Long): QuotationEntity? {
        return db.quotationDao().getQuotationById(id)
    }

    suspend fun getQuotationByNumber(companyId: Long, quotationNumber: String): QuotationEntity? {
        return db.quotationDao().getQuotationByNumber(companyId, quotationNumber)
    }

    suspend fun createQuotation(quotation: QuotationEntity): Long {
        return db.quotationDao().insertQuotation(quotation)
    }

    suspend fun updateStatus(id: Long, status: String) {
        db.quotationDao().updateStatus(id, status)
    }

    suspend fun updateSyncStatus(id: Long, syncStatus: String) {
        db.quotationDao().updateSyncStatus(id, syncStatus)
    }

    suspend fun updateServerQuotationIdAndSyncStatus(id: Long, serverQuotationId: Long, syncStatus: String) {
        db.quotationDao().updateServerQuotationIdAndSyncStatus(id, serverQuotationId, syncStatus)
    }

    suspend fun syncPendingQuotations(): Int {
        val api = apiService ?: return 0
        val pending = db.quotationDao().getPendingSyncQuotations()
        var syncedCount = 0
        for (q in pending) {
            try {
                val parsedItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                    itemsJson = q.itemsJson,
                    itemsSummary = q.itemsSummary,
                    defaultSubtotal = q.subtotal
                )
                val itemsReq = if (parsedItems.isNotEmpty()) {
                    parsedItems.map {
                        ApiQuotationItemRequest(
                            productName = it.title.ifBlank { "Quotation Item" },
                            quantity = it.quantity.toDouble(),
                            rate = it.unitPrice,
                            gstRate = it.gstRatePercent,
                            unit = "PCS"
                        )
                    }
                } else {
                    listOf(
                        ApiQuotationItemRequest(
                            productName = q.itemsSummary.ifBlank { "Quotation Item" },
                            quantity = 1.0,
                            rate = q.subtotal,
                            gstRate = 18.0,
                            unit = "PCS"
                        )
                    )
                }
                val req = ApiQuotationRequest(
                    phone = q.customerPhone.ifBlank { "9999999999" },
                    name = q.customerName.ifBlank { "Customer / Prospect" },
                    items = itemsReq,
                    termsAndConditions = "Standard commercial terms apply. Valid for 15 days from issue date."
                )
                val res = api.createQuotation(req)
                if (res.isSuccessful && res.body() != null) {
                    val srvId = res.body()!!.id
                    db.quotationDao().updateServerQuotationIdAndSyncStatus(q.id, srvId, "SYNCED")
                    syncedCount++
                }
            } catch (e: Exception) {
                android.util.Log.e("QuotationRepo", "Failed to sync quotation ${q.quotationNumber}", e)
            }
        }
        return syncedCount
    }

    suspend fun syncQuotationById(localId: Long): Result<QuotationEntity> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client configured"))
        val q = db.quotationDao().getQuotationById(localId)
            ?: return Result.failure(IllegalArgumentException("Quotation with ID $localId not found"))
        if (q.serverQuotationId != null && q.serverQuotationId > 0L) {
            return Result.success(q)
        }
        return try {
            val parsedItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                itemsJson = q.itemsJson,
                itemsSummary = q.itemsSummary,
                defaultSubtotal = q.subtotal
            )
            val itemsReq = if (parsedItems.isNotEmpty()) {
                parsedItems.map {
                    ApiQuotationItemRequest(
                        productName = it.title.ifBlank { "Quotation Item" },
                        quantity = it.quantity.toDouble(),
                        rate = it.unitPrice,
                        gstRate = it.gstRatePercent,
                        unit = "PCS"
                    )
                }
            } else {
                listOf(
                    ApiQuotationItemRequest(
                        productName = q.itemsSummary.ifBlank { "Quotation Item" },
                        quantity = 1.0,
                        rate = q.subtotal,
                        gstRate = 18.0,
                        unit = "PCS"
                    )
                )
            }
            val req = ApiQuotationRequest(
                phone = q.customerPhone.ifBlank { "9999999999" },
                name = q.customerName.ifBlank { "Customer / Prospect" },
                items = itemsReq,
                termsAndConditions = "Standard commercial terms apply. Valid for 15 days from issue date."
            )
            val res = api.createQuotation(req)
            if (res.isSuccessful && res.body() != null) {
                val srvId = res.body()!!.id
                db.quotationDao().updateServerQuotationIdAndSyncStatus(q.id, srvId, "SYNCED")
                Result.success(q.copy(serverQuotationId = srvId, syncStatus = "SYNCED"))
            } else {
                Result.failure(Exception("Server returned ${res.code()}: ${res.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncQuotationsWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.success(0)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Flush pending local outbox first
                try {
                    syncPendingQuotations()
                } catch (e: Exception) {
                    android.util.Log.w("QuotationRepo", "Outbox flush before quotation fetch had errors", e)
                }

                val res = api.getQuotations()
                if (!res.isSuccessful || res.body() == null) {
                    return@withContext Result.failure(Exception("Failed to fetch quotations: ${res.code()}"))
                }
                val remoteQuotations = res.body()!!
                var syncedCount = 0
                for (q in remoteQuotations) {
                    val qBizId = q.businessId ?: 1L
                    if (qBizId != companyId && companyId != 1L) {
                        continue
                    }
                    val quoteNumber = q.quotationNumber ?: "QT-${q.id.toString().padStart(4, '0')}"
                    val custName = q.billToName ?: "Customer"
                    val custMobile = q.billToMobile ?: q.billToPhone ?: ""
                    val subTot = q.subtotal ?: 0.0
                    val taxTot = q.taxAmount ?: 0.0
                    val grandTot = q.totalAmount ?: (subTot + taxTot)
                    val statusStr = q.status?.uppercase() ?: "PENDING"
                    val createdAtMillis = com.digitalchopda.app.util.DateTimeUtils.parseIsoDateToMillis(q.createdAt)

                    val existing = db.quotationDao().getQuotationByServerId(q.id)
                        ?: db.quotationDao().getQuotationByNumber(companyId, quoteNumber)
                    if (existing != null) {
                        db.quotationDao().updateQuotation(
                            existing.copy(
                                customerName = custName,
                                customerPhone = custMobile,
                                subtotal = subTot,
                                taxAmount = taxTot,
                                grandTotal = grandTot,
                                status = statusStr,
                                serverQuotationId = q.id,
                                syncStatus = "SYNCED"
                            )
                        )
                    } else {
                        db.quotationDao().insertQuotation(
                            QuotationEntity(
                                companyId = companyId,
                                quotationNumber = quoteNumber,
                                customerName = custName,
                                customerPhone = custMobile,
                                itemsSummary = "Quotation line items",
                                subtotal = subTot,
                                taxAmount = taxTot,
                                grandTotal = grandTot,
                                status = statusStr,
                                serverQuotationId = q.id,
                                syncStatus = "SYNCED",
                                createdAt = createdAtMillis
                            )
                        )
                    }
                    syncedCount++
                }

                // Authoritative Scoped Pruning: delete stale local quotations not on server for companyId
                val remoteServerIds = remoteQuotations.map { it.id }.toSet()
                val localQuotations = db.quotationDao().getAllQuotationsSync(companyId)
                val staleQuotations = localQuotations.filter {
                    it.companyId == companyId &&
                    it.serverQuotationId != null &&
                    it.serverQuotationId !in remoteServerIds
                }
                for (sq in staleQuotations) {
                    db.quotationDao().deleteQuotation(sq.id)
                }

                // Authoritative Scoped Deduplication
                val remainingQuotations = db.quotationDao().getAllQuotationsSync(companyId)
                val grouped = remainingQuotations.filter { it.serverQuotationId != null }.groupBy { it.serverQuotationId!! }
                for ((_, duplicates) in grouped) {
                    if (duplicates.size > 1) {
                        val sorted = duplicates.sortedByDescending { it.id }
                        for (dup in sorted.drop(1)) {
                            db.quotationDao().deleteQuotation(dup.id)
                        }
                    }
                }

                Result.success(syncedCount)
            } catch (e: Exception) {
                android.util.Log.e("QuotationRepository", "Error syncing quotations from server", e)
                Result.failure(e)
            }
        }
    }

    suspend fun convertQuotationToInvoice(quotationId: Long, companyId: Long): Result<InvoiceEntity> {
        val q = db.quotationDao().getQuotationById(quotationId)
            ?: return Result.failure(IllegalArgumentException("Quotation not found"))

        if (q.status.equals("CONVERTED", ignoreCase = true)) {
            return Result.failure(IllegalStateException("Quotation is already converted"))
        }

        // Try online conversion first if connected
        if (apiService != null && q.serverQuotationId != null && q.serverQuotationId > 0) {
            try {
                val res = apiService.convertQuotationToInvoice(q.serverQuotationId)
                if (res.isSuccessful && res.body() != null) {
                    val orderDto = res.body()!!
                    db.quotationDao().updateStatus(quotationId, "CONVERTED")
                    val invNumber = orderDto.invoiceNumber ?: "INV-${orderDto.id}"
                    val itemsSummary = if (!orderDto.items.isNullOrEmpty()) {
                        orderDto.items.joinToString(", ") { "${it.productName} ×${it.quantity.toInt()}" }
                    } else {
                        q.itemsSummary
                    }
                    val invoiceEntity = InvoiceEntity(
                        companyId = companyId,
                        invoiceNumber = invNumber,
                        customerName = orderDto.name ?: q.customerName,
                        customerPhone = orderDto.phone ?: q.customerPhone,
                        itemsSummary = itemsSummary,
                        itemsJson = q.itemsJson,
                        subtotal = orderDto.totalAmount ?: q.subtotal,
                        cgst = 0.0,
                        sgst = 0.0,
                        igst = orderDto.taxAmount ?: q.taxAmount,
                        roundOff = 0.0,
                        grandTotal = orderDto.grandTotal ?: q.grandTotal,
                        paidAmount = orderDto.receivedAmount ?: 0.0,
                        status = (orderDto.paymentStatus ?: "PENDING").uppercase(),
                        paymentMode = "CASH",
                        syncStatus = "SYNCED",
                        idempotencyKey = java.util.UUID.randomUUID().toString(),
                        fulfillmentStatus = orderDto.fulfillmentStatus ?: "not_ready",
                        serverOrderId = orderDto.id,
                        createdAt = System.currentTimeMillis()
                    )
                    val invId = db.withTransaction {
                        val id = db.invoiceDao().insertInvoice(invoiceEntity)
                        val customer = db.customerDao().getCustomerByPhone(companyId, q.customerPhone)
                        if (customer != null && invoiceEntity.balanceDue > 0) {
                            db.customerDao().updateBalance(customer.id, invoiceEntity.balanceDue)
                        }
                        id
                    }
                    return Result.success(invoiceEntity.copy(id = invId))
                }
            } catch (e: Exception) {
                android.util.Log.w("QuotationRepo", "Online conversion failed, falling back to local: ${e.message}")
            }
        }

        // Local-first conversion
        return try {
            val leaseEntity = db.leaseDao().getActiveLease(companyId)
            val invNumber = if (leaseEntity != null) {
                val leaseRes = AllocateLeaseUseCase().allocateNextInvoiceNumber(leaseEntity.toDomain())
                db.leaseDao().updateLease(leaseEntity.copy(currentCursor = leaseRes.updatedLease.currentCursor))
                leaseRes.allocatedNumber
            } else {
                "INV-${System.currentTimeMillis() % 100000}"
            }

            val invoiceEntity = InvoiceEntity(
                companyId = companyId,
                invoiceNumber = invNumber,
                customerName = q.customerName,
                customerPhone = q.customerPhone,
                itemsSummary = q.itemsSummary,
                itemsJson = q.itemsJson,
                subtotal = q.subtotal,
                cgst = q.taxAmount / 2.0,
                sgst = q.taxAmount / 2.0,
                igst = 0.0,
                roundOff = 0.0,
                grandTotal = q.grandTotal,
                paidAmount = 0.0,
                status = "UNPAID",
                paymentMode = "CASH",
                syncStatus = "PENDING_SYNC",
                idempotencyKey = java.util.UUID.randomUUID().toString(),
                clientReferenceId = java.util.UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis()
            )
            val invId = db.invoiceDao().insertInvoice(invoiceEntity)
            db.quotationDao().updateStatus(quotationId, "CONVERTED")

            // Adjust customer balance
            val customer = db.customerDao().getCustomerByPhone(companyId, q.customerPhone)
            if (customer != null) {
                db.customerDao().updateBalance(customer.id, q.grandTotal)
            }

            Result.success(invoiceEntity.copy(id = invId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuotation(id: Long): Result<Boolean> {
        return try {
            db.quotationDao().deleteQuotation(id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CreditNoteRepository(
    private val db: ChopdaDatabase,
    val apiService: DigitalChopdaApiService? = null
) {
    fun getCreditNotesForCompany(companyId: Long): Flow<List<CreditNoteEntity>> {
        return db.creditNoteDao().getCreditNotesForCompany(companyId)
    }

    fun getCreditNotesForCustomer(companyId: Long, phone: String, name: String): Flow<List<CreditNoteEntity>> {
        return db.creditNoteDao().getCreditNotesForCustomer(companyId, phone, name)
    }

    suspend fun getCreditNoteById(id: Long): CreditNoteEntity? {
        return db.creditNoteDao().getCreditNoteById(id)
    }

    suspend fun getNextCreditNoteNumber(companyId: Long): String {
        val count = db.creditNoteDao().getCreditNoteCount(companyId)
        return "CN-${(count + 1).toString().padStart(4, '0')}"
    }

    suspend fun createCreditNote(creditNote: CreditNoteEntity): Long {
        return db.withTransaction {
            val id = db.creditNoteDao().insertCreditNote(creditNote)
            val customer = db.customerDao().getCustomerByPhone(creditNote.companyId, creditNote.customerPhone)
            if (customer != null) {
                db.customerDao().updateBalance(customer.id, -creditNote.totalAmount)
            }
            if (creditNote.itemsJson.isNotBlank()) {
                val parsedItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                    itemsJson = creditNote.itemsJson,
                    itemsSummary = creditNote.itemsSummary
                )
                for (item in parsedItems) {
                    if (item.quantity > 0) {
                        val product = db.productDao().getProductByName(creditNote.companyId, item.title)
                        if (product != null) {
                            db.productDao().updateStock(product.id, product.stockQuantity + item.quantity.toDouble())
                        }
                    }
                }
            }
            id
        }
    }

    suspend fun voidCreditNote(id: Long) {
        val cn = db.creditNoteDao().getCreditNoteById(id)
        if (cn != null && cn.status != "VOIDED") {
            if (cn.serverCreditNoteId != null && apiService != null) {
                try {
                    apiService.voidCreditNote(cn.serverCreditNoteId)
                } catch (e: Exception) {
                    android.util.Log.w("CreditNoteRepo", "Remote void failed, proceeding locally", e)
                }
            }
            db.withTransaction {
                db.creditNoteDao().voidCreditNote(id)
                val customer = db.customerDao().getCustomerByPhone(cn.companyId, cn.customerPhone)
                if (customer != null) {
                    db.customerDao().updateBalance(customer.id, cn.totalAmount)
                }
                if (cn.itemsJson.isNotBlank()) {
                    val parsedItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                        itemsJson = cn.itemsJson,
                        itemsSummary = cn.itemsSummary
                    )
                    for (item in parsedItems) {
                        if (item.quantity > 0) {
                            val product = db.productDao().getProductByName(cn.companyId, item.title)
                            if (product != null) {
                                db.productDao().updateStock(product.id, maxOf(0.0, product.stockQuantity - item.quantity.toDouble()))
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun downloadBackendCreditNotePdf(context: Context, creditNoteId: Long, filename: String): File? {
        val api = apiService ?: return null
        return try {
            val response = api.getCreditNotePdf(creditNoteId)
            if (response.isSuccessful && response.body() != null) {
                val dir = File(context.filesDir, "credit_notes").apply { mkdirs() }
                val targetFile = File(dir, filename)
                response.body()!!.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun syncPendingCreditNotes(): Int {
        val api = apiService ?: return 0
        val pending = db.creditNoteDao().getPendingSyncCreditNotes()
        var synced = 0
        for (cn in pending) {
            try {
                val contact = db.customerDao().getCustomerByPhone(cn.companyId, cn.customerPhone)
                val contactId = contact?.serverContactId ?: contact?.id ?: 1L
                val lineItems = com.digitalchopda.app.core.document.DocumentItemParser.parseLineItems(
                    itemsJson = cn.itemsJson,
                    itemsSummary = cn.itemsSummary,
                    defaultSubtotal = cn.subtotal
                )
                val itemsReq = if (lineItems.isNotEmpty()) {
                    lineItems.map {
                        com.digitalchopda.app.core.network.ApiCreditNoteItemDto(
                            productName = it.title.ifBlank { "Item" },
                            quantity = it.quantity.toDouble(),
                            unitPrice = it.unitPrice,
                            gstRate = it.gstRatePercent
                        )
                    }
                } else {
                    listOf(
                        com.digitalchopda.app.core.network.ApiCreditNoteItemDto(
                            productName = cn.itemsSummary.ifBlank { "Returned Goods" },
                            quantity = 1.0,
                            unitPrice = cn.subtotal,
                            gstRate = 0.0
                        )
                    )
                }

                val resolvedOrderId = cn.orderId?.let { localOrdId ->
                    db.invoiceDao().getInvoiceById(localOrdId)?.serverOrderId
                }

                val req = com.digitalchopda.app.core.network.ApiCreateCreditNoteRequest(
                    contactId = contactId,
                    orderId = resolvedOrderId,
                    creditNoteNumber = cn.creditNoteNumber,
                    reason = cn.reason,
                    notes = cn.notes.ifBlank { null },
                    items = itemsReq,
                    clientReferenceId = cn.clientReferenceId
                )
                val res = api.createCreditNote(req)
                if (res.isSuccessful && res.body() != null) {
                    val serverId = res.body()!!.id
                    db.creditNoteDao().markSynced(cn.id, serverId)
                    synced++
                }
            } catch (e: Exception) {
                android.util.Log.e("CreditNoteRepo", "Failed to sync credit note ${cn.creditNoteNumber}", e)
            }
        }
        return synced
    }

    suspend fun syncCreditNotesWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.success(0)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                try {
                    syncPendingCreditNotes()
                } catch (e: Exception) {
                    android.util.Log.w("CreditNoteRepo", "Outbox flush before credit notes fetch had errors", e)
                }

                val res = api.getCreditNotes()
                if (!res.isSuccessful || res.body() == null) {
                    return@withContext Result.failure(Exception("Failed to fetch credit notes: ${res.code()}"))
                }
                val remoteList = res.body()!!
                var syncedCount = 0
                for (cn in remoteList) {
                    val cnBizId = cn.businessId ?: 1L
                    if (cnBizId != companyId && companyId != 1L) {
                        continue
                    }
                    val existing = db.creditNoteDao().getCreditNoteByServerId(cn.id)
                        ?: db.creditNoteDao().getCreditNoteByNumber(companyId, cn.creditNoteNumber)

                    val contact = cn.contactId?.let { db.customerDao().getCustomerByServerContactId(it) }
                    val custName = contact?.name ?: cn.contactName ?: "Customer"
                    val custPhone = contact?.phone ?: cn.contactPhone ?: ""

                    val totalAmount = cn.totalAmount ?: 0.0
                    val subtotal = cn.subtotal ?: (totalAmount / 1.18)
                    val taxAmount = cn.taxAmount ?: (totalAmount - subtotal)
                    val status = "ACTIVE"
                    val createdAtMillis = com.digitalchopda.app.util.DateTimeUtils.parseIsoDateToMillis(cn.createdAt)

                    val entity = (existing ?: CreditNoteEntity(
                        companyId = companyId,
                        creditNoteNumber = cn.creditNoteNumber,
                        customerName = custName,
                        customerPhone = custPhone,
                        reason = cn.reason ?: "Return"
                    )).copy(
                        companyId = companyId,
                        creditNoteNumber = cn.creditNoteNumber,
                        customerName = custName,
                        customerPhone = custPhone,
                        reason = cn.reason ?: "Return",
                        notes = cn.notes ?: "",
                        subtotal = subtotal,
                        taxAmount = taxAmount,
                        totalAmount = totalAmount,
                        status = status,
                        syncStatus = "SYNCED",
                        serverCreditNoteId = cn.id,
                        createdAt = createdAtMillis
                    )

                    if (existing != null) {
                        db.creditNoteDao().updateCreditNote(entity)
                    } else {
                        db.creditNoteDao().insertCreditNote(entity)
                    }
                    syncedCount++
                }

                // Authoritative Scoped Pruning: delete stale local credit notes not on server for companyId
                val remoteServerIds = remoteList.map { it.id }.toSet()
                val localCreditNotes = db.creditNoteDao().getAllCreditNotesSync(companyId)
                val staleCreditNotes = localCreditNotes.filter {
                    it.companyId == companyId &&
                    it.serverCreditNoteId != null &&
                    it.serverCreditNoteId !in remoteServerIds
                }
                for (scn in staleCreditNotes) {
                    db.creditNoteDao().deleteCreditNote(scn.id)
                }

                // Authoritative Scoped Deduplication
                val remainingCreditNotes = db.creditNoteDao().getAllCreditNotesSync(companyId)
                val grouped = remainingCreditNotes.filter { it.serverCreditNoteId != null }.groupBy { it.serverCreditNoteId!! }
                for ((_, duplicates) in grouped) {
                    if (duplicates.size > 1) {
                        val sorted = duplicates.sortedByDescending { it.id }
                        for (dup in sorted.drop(1)) {
                            db.creditNoteDao().deleteCreditNote(dup.id)
                        }
                    }
                }

                Result.success(syncedCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

class ProductRepository(
    private val db: ChopdaDatabase,
    private val apiService: DigitalChopdaApiService? = null
) {
    fun getProducts(companyId: Long): Flow<List<Product>> {
        return db.productDao().getProductsForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun searchProducts(companyId: Long, query: String): Flow<List<Product>> {
        return db.productDao().searchProducts(companyId, query).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getProductCount(companyId: Long): Flow<Int> {
        return db.productDao().getProductCount(companyId)
    }

    suspend fun getProductById(id: Long): Product? {
        return db.productDao().getProductById(id)?.toDomain()
    }

    suspend fun syncProductsWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client"))
        return try {
            val res = api.getProducts()
            if (res.isSuccessful && res.body() != null) {
                val remoteList = res.body()!!
                for (p in remoteList) {
                    val existing = db.productDao().getProductByName(companyId, p.name)
                    val entity = ProductEntity(
                        id = existing?.id ?: 0,
                        companyId = companyId,
                        name = p.name,
                        defaultPrice = p.defaultPrice,
                        hsnCode = p.hsnCode ?: existing?.hsnCode,
                        gstRate = p.gstRate ?: existing?.gstRate ?: 18.0,
                        unit = existing?.unit ?: "PCS",
                        stockQuantity = p.stockQuantity ?: existing?.stockQuantity ?: 0.0,
                        minStockAlert = p.minStockAlert ?: existing?.minStockAlert ?: 5.0,
                        purchasePrice = p.purchasePrice ?: existing?.purchasePrice ?: 0.0,
                        sku = p.sku ?: existing?.sku,
                        barcode = p.barcode ?: existing?.barcode,
                        version = existing?.version ?: 1,
                        syncStatus = "SYNCED",
                        serverProductId = p.id,
                        primaryImageUrl = p.primaryImageUrl ?: existing?.primaryImageUrl,
                        wholesalePrice = p.wholesalePrice ?: existing?.wholesalePrice,
                        wholesaleQuantity = p.wholesaleQuantity ?: existing?.wholesaleQuantity ?: 1.0,
                        batchNumber = p.batchNumber ?: existing?.batchNumber,
                        expiryDate = p.expiryDate ?: existing?.expiryDate,
                        showInOnlineStore = p.showInOnlineStore ?: existing?.showInOnlineStore ?: true,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    db.productDao().upsertProduct(entity)
                }

                // 🧹 Authoritative Reconciliation: Prune stale or reseeded products
                val currentProducts = db.productDao().getAllProductsSync(companyId)
                val remoteProductIds = remoteList.map { it.id }.toSet()
                val staleProducts = currentProducts.filter {
                    it.companyId == companyId && (
                        (it.serverProductId != null && it.serverProductId !in remoteProductIds && it.syncStatus == "SYNCED") ||
                        (it.serverProductId == null && it.syncStatus == "SYNCED")
                    )
                }
                for (sp in staleProducts) {
                    db.productDao().deleteProduct(sp.id)
                }

                val remainingProd = db.productDao().getAllProductsSync(companyId)
                val groupedProducts = remainingProd.filter { it.serverProductId != null }.groupBy { it.serverProductId!! }
                for ((_, duplicates) in groupedProducts) {
                    if (duplicates.size > 1) {
                        val sorted = duplicates.sortedByDescending { it.id }
                        for (dup in sorted.drop(1)) {
                            db.productDao().deleteProduct(dup.id)
                        }
                    }
                }

                Result.success(remoteList.size)
            } else {
                Result.failure(Exception("Failed to fetch products: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addProduct(product: Product): Result<Long> {
        val trimmedName = product.name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name is required"))
        }
        val existing = db.productDao().getProductByName(product.companyId, trimmedName)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("That product already exists in your catalog"))
        }
        val entity = ProductEntity(
            companyId = product.companyId,
            name = trimmedName,
            defaultPrice = product.defaultPrice,
            hsnCode = product.hsnCode?.trim()?.ifBlank { null },
            gstRate = product.gstRate,
            unit = product.unit.trim().ifBlank { "PCS" },
            stockQuantity = product.stockQuantity,
            minStockAlert = product.minStockAlert,
            purchasePrice = product.purchasePrice,
            sku = product.sku?.trim()?.ifBlank { null },
            barcode = product.barcode?.trim()?.ifBlank { null },
            primaryImageUrl = product.primaryImageUrl,
            wholesalePrice = product.wholesalePrice,
            wholesaleQuantity = product.wholesaleQuantity,
            batchNumber = product.batchNumber,
            expiryDate = product.expiryDate,
            showInOnlineStore = product.showInOnlineStore
        )
        return try {
            val id = db.productDao().insertProduct(entity)
            if (apiService != null) {
                try {
                    val req = com.digitalchopda.app.core.network.ApiCreateProductRequest(
                        name = trimmedName,
                        defaultPrice = product.defaultPrice,
                        hsnCode = product.hsnCode?.trim()?.ifBlank { null },
                        gstRate = product.gstRate,
                        barcode = entity.barcode,
                        sku = entity.sku,
                        stockQuantity = entity.stockQuantity,
                        minStockAlert = entity.minStockAlert,
                        purchasePrice = entity.purchasePrice,
                        primaryImageUrl = product.primaryImageUrl,
                        wholesalePrice = product.wholesalePrice,
                        wholesaleQuantity = product.wholesaleQuantity,
                        batchNumber = product.batchNumber,
                        expiryDate = product.expiryDate,
                        showInOnlineStore = product.showInOnlineStore
                    )
                    val remoteRes = apiService.createProduct(req)
                    if (remoteRes.isSuccessful && remoteRes.body() != null) {
                        val remote = remoteRes.body()!!
                        db.productDao().updateProduct(entity.copy(id = id, serverProductId = remote.id, syncStatus = "SYNCED"))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepo", "Failed to sync created product to server: ${e.message}")
                }
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        val trimmedName = product.name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name is required"))
        }
        val existing = db.productDao().getProductByName(product.companyId, trimmedName)
        if (existing != null && existing.id != product.id) {
            return Result.failure(IllegalArgumentException("Another product with that name already exists"))
        }
        val existingEntity = db.productDao().getProductById(product.id)
        val entity = ProductEntity(
            id = product.id,
            companyId = product.companyId,
            name = trimmedName,
            defaultPrice = product.defaultPrice,
            hsnCode = product.hsnCode?.trim()?.ifBlank { null },
            gstRate = product.gstRate,
            unit = product.unit.trim().ifBlank { "PCS" },
            stockQuantity = product.stockQuantity,
            minStockAlert = product.minStockAlert,
            purchasePrice = product.purchasePrice,
            sku = product.sku?.trim()?.ifBlank { null },
            barcode = product.barcode?.trim()?.ifBlank { null },
            primaryImageUrl = product.primaryImageUrl ?: existingEntity?.primaryImageUrl,
            wholesalePrice = product.wholesalePrice ?: existingEntity?.wholesalePrice,
            wholesaleQuantity = product.wholesaleQuantity,
            batchNumber = product.batchNumber ?: existingEntity?.batchNumber,
            expiryDate = product.expiryDate ?: existingEntity?.expiryDate,
            showInOnlineStore = product.showInOnlineStore,
            version = (existingEntity?.version ?: 1) + 1,
            serverProductId = existingEntity?.serverProductId,
            createdAt = product.createdAt
        )
        return try {
            db.productDao().updateProduct(entity)
            if (apiService != null && existingEntity?.serverProductId != null) {
                try {
                    val req = com.digitalchopda.app.core.network.ApiCreateProductRequest(
                        name = trimmedName,
                        defaultPrice = product.defaultPrice,
                        hsnCode = product.hsnCode?.trim()?.ifBlank { null },
                        gstRate = product.gstRate,
                        barcode = entity.barcode,
                        sku = entity.sku,
                        stockQuantity = entity.stockQuantity,
                        minStockAlert = entity.minStockAlert,
                        purchasePrice = entity.purchasePrice,
                        primaryImageUrl = entity.primaryImageUrl,
                        wholesalePrice = entity.wholesalePrice,
                        wholesaleQuantity = entity.wholesaleQuantity,
                        batchNumber = entity.batchNumber,
                        expiryDate = entity.expiryDate,
                        showInOnlineStore = entity.showInOnlineStore
                    )
                    apiService.updateProduct(existingEntity.serverProductId, req)
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepo", "Failed to update product on server: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLowStockProducts(companyId: Long): Flow<List<Product>> {
        return db.productDao().getLowStockProducts(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getStockHistory(companyId: Long, productId: Long): Flow<List<StockAdjustment>> {
        return db.stockAdjustmentDao().getAdjustmentsForProduct(companyId, productId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun adjustStock(
        companyId: Long,
        productId: Long,
        adjustmentType: String,
        quantity: Double,
        reason: String = "Manual Adjustment",
        notes: String? = null
    ): Result<StockAdjustment> {
        if (!listOf("add", "reduce", "set").contains(adjustmentType.lowercase())) {
            return Result.failure(IllegalArgumentException("adjustmentType must be 'add', 'reduce', or 'set'"))
        }
        if (quantity < 0) {
            return Result.failure(IllegalArgumentException("quantity must be non-negative"))
        }

        return try {
            val product = db.productDao().getProductById(productId)
                ?: return Result.failure(IllegalArgumentException("Product not found"))

            val prevStock = product.stockQuantity
            val newStock = when (adjustmentType.lowercase()) {
                "add" -> prevStock + quantity
                "reduce" -> (prevStock - quantity).coerceAtLeast(0.0)
                "set" -> quantity
                else -> prevStock
            }

            db.productDao().updateProduct(
                product.copy(
                    stockQuantity = newStock,
                    version = product.version + 1
                )
            )

            val adjustmentEntity = StockAdjustmentEntity(
                companyId = companyId,
                productId = productId,
                adjustmentType = adjustmentType.lowercase(),
                quantity = quantity,
                previousStock = prevStock,
                newStock = newStock,
                reason = reason,
                notes = notes
            )
            val adjId = db.stockAdjustmentDao().insertAdjustment(adjustmentEntity)

            if (apiService != null && product.serverProductId != null) {
                try {
                    val req = com.digitalchopda.app.core.network.ApiStockAdjustmentRequest(
                        productId = product.serverProductId,
                        adjustmentType = adjustmentType.lowercase(),
                        quantity = quantity,
                        reason = reason,
                        notes = notes
                    )
                    val res = apiService.adjustStock(req)
                    if (res.isSuccessful) {
                        db.stockAdjustmentDao().markAdjustmentSynced(adjId, "SYNCED")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProductRepo", "Stock adjustment live sync deferred: ${e.message}")
                }
            }

            Result.success(adjustmentEntity.copy(id = adjId).toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingStockAdjustments(): Int {
        val api = apiService ?: return 0
        return try {
            val pending = db.stockAdjustmentDao().getPendingSyncAdjustments()
            var count = 0
            for (adj in pending) {
                try {
                    val prod = db.productDao().getProductById(adj.productId)
                    if (prod == null || prod.serverProductId == null) {
                        // Orphaned local adjustment without server product - prune to clear outbox
                        db.stockAdjustmentDao().markAdjustmentSynced(adj.id, "SYNCED")
                        continue
                    }
                    val serverProdId = prod.serverProductId
                    val req = com.digitalchopda.app.core.network.ApiStockAdjustmentRequest(
                        productId = serverProdId,
                        adjustmentType = adj.adjustmentType,
                        quantity = adj.quantity,
                        reason = adj.reason,
                        notes = adj.notes
                    )
                    val res = api.adjustStock(req)
                    if (res.isSuccessful || res.code() == 404) {
                        db.stockAdjustmentDao().markAdjustmentSynced(adj.id, "SYNCED")
                        if (res.isSuccessful) count++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepo", "Failed to sync adjustment ${adj.id}: ${e.message}")
                }
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    suspend fun deleteProduct(id: Long): Result<Unit> {
        return try {
            val product = db.productDao().getProductById(id)
            if (product?.serverProductId != null && apiService != null) {
                try {
                    apiService.deleteProduct(product.serverProductId)
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepo", "Failed to delete product from server: ${e.message}")
                }
            }
            db.productDao().deleteProduct(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AuthRepository(
    private val tokenStorage: TokenStorage,
    private val apiService: DigitalChopdaApiService? = null,
    private val db: ChopdaDatabase? = null,
    private val billingRepository: BillingRepository? = null,
    private val quotationRepository: QuotationRepository? = null,
    private val customerRepository: CustomerRepository? = null,
    private val productRepository: ProductRepository? = null
) {
    suspend fun loginWithPassword(usernameOrMobile: String, pass: String?): Result<AuthResponse> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client configured for login"))
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

                // Immediately pull downstream documents from server into Room
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        billingRepository?.syncInvoicesWithServer(activeBizId)
                        quotationRepository?.syncQuotationsWithServer(activeBizId)
                        customerRepository?.syncContactsWithServer(activeBizId)
                        productRepository?.syncProductsWithServer(activeBizId)
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepository", "Post-login sync failed", e)
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
            return Result.failure(e)
        }
    }

    suspend fun verifyOtp(mobile: String, otp: String): Result<AuthResponse> {
        val api = apiService ?: return Result.failure(IllegalStateException("No network client configured"))
        return try {
            val res = api.verifyOtp(com.digitalchopda.app.core.network.ApiVerifyOtpRequest(phone = mobile.trim(), otp = otp.trim()))
            if (res.isSuccessful && res.body()?.token != null) {
                val body = res.body()!!
                val token = body.token!!
                val userId = body.user?.id?.toString() ?: "1"
                val activeBizId = body.activeBusiness?.id ?: body.businesses?.firstOrNull()?.id ?: 1L

                tokenStorage.saveTokens(token, "")
                tokenStorage.saveUserSession(userId, mobile, activeBizId)

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

                Result.success(
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
                    res.body()?.error ?: "Invalid OTP (Code ${res.code()})"
                }
                Result.failure(IllegalArgumentException(parsedError))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerMerchant(
        firmName: String,
        ownerName: String,
        mobileNumber: String,
        pass: String
    ): Result<AuthResponse> {
        val cleanFirm = firmName.trim()
        val cleanOwner = ownerName.trim().ifBlank { cleanFirm }
        val cleanMobile = mobileNumber.trim()
        val cleanPass = pass.trim()

        val generatedPrefix = cleanFirm
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.first().uppercaseChar() }
            .take(3)
            .joinToString("")
            .ifBlank { "INV" } + "-"

        // 1. Try remote backend if online
        var remoteBizId: Long? = null
        val api = apiService
        if (api != null) {
            try {
                val req = com.digitalchopda.app.core.network.ApiCreateBusinessRequest(
                    name = cleanFirm,
                    tradeName = cleanOwner,
                    phone = cleanMobile,
                    invoicePrefix = generatedPrefix
                )
                val res = api.createBusiness(req)
                if (res.isSuccessful && res.body() != null) {
                    remoteBizId = res.body()!!.id
                }
            } catch (_: Exception) {
                // Offline fallback
            }
        }

        // 2. Sovereign Native Offline Registration in Room
        val companyId = if (db != null) {
            db.companyDao().deactivateAll()
            val entity = CompanyEntity(
                id = remoteBizId ?: 0L,
                firmName = cleanFirm,
                ownerName = cleanOwner,
                phone = cleanMobile,
                invoicePrefix = generatedPrefix,
                isActive = true
            )
            db.companyDao().insertCompany(entity)
        } else {
            remoteBizId ?: 1L
        }

        val auth = AuthResponse(
            success = true,
            accessToken = "m_acc_off_${System.currentTimeMillis()}",
            refreshToken = "m_ref_off_${System.currentTimeMillis()}",
            userId = cleanMobile.ifBlank { "1" },
            ownerName = cleanOwner,
            companies = listOf(
                Company(
                    id = companyId,
                    firmName = cleanFirm,
                    ownerName = cleanOwner,
                    phone = cleanMobile,
                    invoicePrefix = generatedPrefix,
                    isActive = true
                )
            )
        )

        tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
        tokenStorage.saveUserSession(auth.userId, cleanMobile, companyId)
        return Result.success(auth)
    }

    suspend fun logout() {
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
    upiVpa = upiVpa,
    bankName = bankName,
    bankAccountNo = bankAccountNo,
    bankIfsc = bankIfsc,
    bankBranch = bankBranch
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
    barcode = barcode,
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

fun ProductEntity.toDomain() = Product(
    id = id,
    companyId = companyId,
    name = name,
    defaultPrice = defaultPrice,
    hsnCode = hsnCode,
    gstRate = gstRate,
    unit = unit,
    stockQuantity = stockQuantity,
    minStockAlert = minStockAlert,
    purchasePrice = purchasePrice,
    sku = sku,
    barcode = barcode,
    primaryImageUrl = primaryImageUrl,
    wholesalePrice = wholesalePrice,
    wholesaleQuantity = wholesaleQuantity,
    batchNumber = batchNumber,
    expiryDate = expiryDate,
    showInOnlineStore = showInOnlineStore,
    createdAt = createdAt
)

fun PurchaseItemEntity.toDomain() = PurchaseItem(
    id = id,
    purchaseId = purchaseId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    gstRate = gstRate,
    hsnCode = hsnCode
)

fun PurchaseEntity.toDomain(items: List<PurchaseItem> = emptyList()) = Purchase(
    id = id,
    companyId = companyId,
    supplierId = supplierId,
    purchaseNumber = purchaseNumber,
    vendorBillNumber = vendorBillNumber,
    supplierInvoiceNo = supplierInvoiceNo,
    subtotal = subtotal,
    taxAmount = taxAmount,
    grandTotal = grandTotal,
    paymentStatus = paymentStatus,
    paymentMethod = paymentMethod,
    notes = notes,
    purchaseDate = purchaseDate,
    items = items,
    createdAt = createdAt
)

fun StockAdjustmentEntity.toDomain() = StockAdjustment(
    id = id,
    companyId = companyId,
    productId = productId,
    adjustmentType = adjustmentType,
    quantity = quantity,
    previousStock = previousStock,
    newStock = newStock,
    reason = reason,
    notes = notes,
    createdAt = createdAt
)

fun OrderRevisionEntity.toDomain() = OrderRevision(
    id = id,
    companyId = companyId,
    orderId = orderId,
    revisionNumber = revisionNumber,
    revisionDate = revisionDate,
    previousTotal = previousTotal,
    newTotal = newTotal,
    deltaAmount = deltaAmount,
    reason = reason,
    itemsSnapshotJson = itemsSnapshotJson,
    newItemsSnapshotJson = newItemsSnapshotJson,
    createdAt = createdAt
)

fun InvoiceEntity.toDomain(items: List<com.digitalchopda.app.domain.model.LineItem> = emptyList()) = Invoice(
    id = id,
    companyId = companyId,
    invoiceNumber = invoiceNumber,
    customerName = customerName,
    customerPhone = customerPhone,
    items = items,
    subtotal = subtotal,
    cgst = cgst,
    sgst = sgst,
    igst = igst,
    roundOff = roundOff,
    grandTotal = grandTotal,
    paidAmount = paidAmount,
    balanceDue = balanceDue,
    status = when (status.uppercase()) {
        "PARTIAL", "PARTIALLY_PAID" -> InvoiceStatus.PARTIAL
        "UNPAID", "PENDING", "CREDIT" -> InvoiceStatus.UNPAID
        else -> InvoiceStatus.PAID
    },
    paymentMode = try { PaymentMode.valueOf(paymentMode) } catch (_: Exception) { PaymentMode.UPI },
    syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (_: Exception) { SyncStatus.PENDING_SYNC },
    serverOrderId = serverOrderId,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt
)

