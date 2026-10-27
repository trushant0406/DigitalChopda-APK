package com.digitalchopda.app.data.repository

import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.data.local.database.ChopdaDatabase
import androidx.room.withTransaction
import com.digitalchopda.app.data.local.entity.ProductEntity
import com.digitalchopda.app.data.local.entity.PurchaseEntity
import com.digitalchopda.app.data.local.entity.PurchaseItemEntity
import com.digitalchopda.app.data.local.entity.StockAdjustmentEntity
import com.digitalchopda.app.data.local.entity.SupplierEntity
import com.digitalchopda.app.domain.model.Purchase
import com.digitalchopda.app.domain.model.PurchaseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PurchaseRepository(
    private val db: ChopdaDatabase,
    val apiService: DigitalChopdaApiService? = null
) {
    fun getPurchasesForCompany(companyId: Long): Flow<List<Purchase>> {
        return db.purchaseDao().getPurchasesForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getPurchasesForSupplier(companyId: Long, supplierId: Long): Flow<List<Purchase>> {
        return db.purchaseDao().getPurchasesForSupplier(companyId, supplierId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getPurchaseById(id: Long): Purchase? {
        val entity = db.purchaseDao().getPurchaseById(id) ?: return null
        val items = db.purchaseDao().getPurchaseItems(id).map { it.toDomain() }
        return entity.toDomain(items)
    }

    suspend fun getNextPurchaseNumber(companyId: Long): String {
        val count = db.purchaseDao().getPurchaseCountSync(companyId)
        return "PUR-${(count + 1).toString().padStart(4, '0')}"
    }

    suspend fun recordPurchase(
        companyId: Long,
        supplierId: Long,
        supplierInvoiceNo: String,
        items: List<PurchaseItem>,
        paymentStatus: String = "unpaid",
        paymentMethod: String = "cash",
        notes: String = ""
    ): Result<Long> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Purchase must contain at least one item"))
        }

        return try {
            val purchaseNumber = getNextPurchaseNumber(companyId)
            val subtotal = items.sumOf { it.subtotal }
            val taxAmount = items.sumOf { it.taxAmount }
            val grandTotal = subtotal + taxAmount

            val purchaseEntity = PurchaseEntity(
                companyId = companyId,
                supplierId = supplierId,
                purchaseNumber = purchaseNumber,
                vendorBillNumber = supplierInvoiceNo,
                supplierInvoiceNo = supplierInvoiceNo,
                subtotal = subtotal,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                paymentStatus = paymentStatus.lowercase(),
                paymentMethod = paymentMethod.lowercase(),
                notes = notes,
                syncStatus = "PENDING_SYNC",
                clientReferenceId = UUID.randomUUID().toString(),
                purchaseDate = System.currentTimeMillis()
            )

            val purchaseId = db.withTransaction {
                val pId = db.purchaseDao().insertPurchase(purchaseEntity)

                val itemEntities = items.map { item ->
                    PurchaseItemEntity(
                        purchaseId = pId,
                        productName = item.productName.trim(),
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        gstRate = item.gstRate,
                        hsnCode = item.hsnCode?.trim()?.ifBlank { null }
                    )
                }
                db.purchaseDao().insertPurchaseItems(itemEntities)

                // Auto-increment stock in catalog or create new product
                for (item in items) {
                    val productName = item.productName.trim()
                    val existingProduct = db.productDao().getProductByName(companyId, productName)
                    if (existingProduct != null) {
                        val prevStock = existingProduct.stockQuantity
                        val newStock = prevStock + item.quantity
                        db.productDao().updateProduct(
                            existingProduct.copy(
                                stockQuantity = newStock,
                                purchasePrice = item.unitPrice,
                                version = existingProduct.version + 1
                            )
                        )
                        // Log stock adjustment audit
                        db.stockAdjustmentDao().insertAdjustment(
                            StockAdjustmentEntity(
                                companyId = companyId,
                                productId = existingProduct.id,
                                adjustmentType = "add",
                                quantity = item.quantity,
                                previousStock = prevStock,
                                newStock = newStock,
                                reason = "Supplier Purchase ($purchaseNumber)",
                                notes = "Vendor Bill: $supplierInvoiceNo"
                            )
                        )
                    } else {
                        // Create product in catalog
                        val newProdId = db.productDao().insertProduct(
                            ProductEntity(
                                companyId = companyId,
                                name = productName,
                                defaultPrice = item.unitPrice * 1.2, // standard default price
                                hsnCode = item.hsnCode?.trim()?.ifBlank { null },
                                gstRate = item.gstRate,
                                unit = "PCS",
                                stockQuantity = item.quantity,
                                minStockAlert = 5.0,
                                purchasePrice = item.unitPrice,
                                version = 1
                            )
                        )
                        db.stockAdjustmentDao().insertAdjustment(
                            StockAdjustmentEntity(
                                companyId = companyId,
                                productId = newProdId,
                                adjustmentType = "add",
                                quantity = item.quantity,
                                previousStock = 0.0,
                                newStock = item.quantity,
                                reason = "Supplier Purchase ($purchaseNumber)",
                                notes = "Initial stock from bill: $supplierInvoiceNo"
                            )
                        )
                    }
                }

                // If unpaid or pending, update supplier balanceToPay
                if (paymentStatus.equals("unpaid", ignoreCase = true) || paymentStatus.equals("pending", ignoreCase = true)) {
                    db.supplierDao().updateBalance(supplierId, grandTotal)
                }
                pId
            }

            // Attempt live push to server
            if (apiService != null) {
                try {
                    val serverContactId = resolveServerContactId(companyId, supplierId)
                    val req = com.digitalchopda.app.core.network.ApiCreatePurchaseRequest(
                        contactId = serverContactId ?: 1L,
                        purchaseNumber = purchaseNumber,
                        supplierInvoiceNo = supplierInvoiceNo,
                        paymentStatus = paymentStatus.lowercase(),
                        paymentMethod = paymentMethod.lowercase(),
                        items = items.map {
                            com.digitalchopda.app.core.network.ApiPurchaseItemRequest(
                                productName = it.productName.trim(),
                                quantity = it.quantity,
                                unitPrice = it.unitPrice,
                                gstRate = it.gstRate,
                                hsnCode = it.hsnCode?.trim()?.ifBlank { null }
                            )
                        },
                        notes = notes,
                        clientReferenceId = purchaseEntity.clientReferenceId
                    )
                    val res = apiService.createPurchase(req)
                    if (res.isSuccessful && res.body() != null) {
                        val serverPurchase = res.body()!!
                        db.purchaseDao().markPurchaseSynced(purchaseId, serverPurchase.id, "SYNCED")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PurchaseRepo", "Live purchase dispatch deferred to background sync: ${e.message}")
                }
            }

            Result.success(purchaseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveServerContactId(companyId: Long, supplierId: Long): Long? {
        val supp = db.supplierDao().getSupplierById(supplierId)
        if (supp != null) {
            val matchingCust = db.customerDao().getCustomerByPhone(companyId, supp.phone)
            if (matchingCust?.serverContactId != null) return matchingCust.serverContactId

            if (apiService != null) {
                try {
                    val contactRes = apiService.createContact(
                        com.digitalchopda.app.core.network.ApiCreateContactRequest(
                            name = supp.name,
                            phone = supp.phone,
                            partyType = "supplier",
                            partyTag = "Supplier",
                            gstin = supp.gstin.ifBlank { null }
                        )
                    )
                    if (contactRes.isSuccessful && contactRes.body() != null) {
                        val serverContact = contactRes.body()!!
                        return serverContact.id
                    }
                } catch (_: Exception) {}
            }
        }

        val cust = db.customerDao().getCustomerById(supplierId)
        if (cust?.serverContactId != null) return cust.serverContactId
        if (cust != null && apiService != null) {
            try {
                val contactRes = apiService.createContact(
                    com.digitalchopda.app.core.network.ApiCreateContactRequest(
                        name = cust.name,
                        phone = cust.phone,
                        partyType = "supplier",
                        partyTag = "Supplier",
                        gstin = cust.gstin.ifBlank { null }
                    )
                )
                if (contactRes.isSuccessful && contactRes.body() != null) {
                    val serverContact = contactRes.body()!!
                    db.customerDao().updateServerContactId(
                        id = cust.id,
                        serverContactId = serverContact.id,
                        outstanding = serverContact.outstanding ?: serverContact.balance ?: cust.balance
                    )
                    return serverContact.id
                }
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun syncPendingPurchases(): Int {
        val api = apiService ?: return 0
        return try {
            val pending = db.purchaseDao().getPendingSyncPurchases()
            var count = 0
            for (p in pending) {
                try {
                    val items = db.purchaseDao().getPurchaseItems(p.id)
                    val serverContactId = resolveServerContactId(p.companyId, p.supplierId)
                    if (serverContactId == null || serverContactId <= 0L) {
                        android.util.Log.w("PurchaseRepo", "Deferring sync for purchase ${p.purchaseNumber}: unresolved supplier contact ID")
                        continue
                    }
                    val finalItems = if (items.isEmpty()) {
                        listOf(
                            com.digitalchopda.app.core.network.ApiPurchaseItemRequest(
                                productName = "General Inward Item",
                                quantity = 1.0,
                                unitPrice = p.grandTotal,
                                gstRate = 0.0,
                                hsnCode = null
                            )
                        )
                    } else {
                        items.map {
                            com.digitalchopda.app.core.network.ApiPurchaseItemRequest(
                                productName = it.productName,
                                quantity = it.quantity,
                                unitPrice = it.unitPrice,
                                gstRate = it.gstRate,
                                hsnCode = it.hsnCode
                            )
                        }
                    }
                    val req = com.digitalchopda.app.core.network.ApiCreatePurchaseRequest(
                        contactId = serverContactId,
                        purchaseNumber = p.purchaseNumber,
                        supplierInvoiceNo = p.supplierInvoiceNo.ifBlank { p.vendorBillNumber },
                        paymentStatus = p.paymentStatus,
                        paymentMethod = p.paymentMethod,
                        items = finalItems,
                        notes = p.notes,
                        clientReferenceId = p.clientReferenceId
                    )
                    val res = api.createPurchase(req)
                    if (res.isSuccessful && res.body() != null) {
                        val serverPur = res.body()!!
                        db.purchaseDao().markPurchaseSynced(p.id, serverPur.id, "SYNCED")
                        count++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PurchaseRepo", "Failed to sync pending purchase ${p.purchaseNumber}: ${e.message}")
                }
            }
            count
        } catch (e: Exception) {
            android.util.Log.e("PurchaseRepo", "Error in syncPendingPurchases", e)
            0
        }
    }

    suspend fun syncPurchasesWithServer(companyId: Long): Result<Int> {
        val api = apiService ?: return Result.success(0)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Flush local outbox first
                try {
                    syncPendingPurchases()
                } catch (e: Exception) {
                    android.util.Log.w("PurchaseRepo", "Outbox flush before purchases fetch had errors", e)
                }

                val res = api.getPurchases()
                if (!res.isSuccessful || res.body() == null) {
                    return@withContext Result.failure(Exception("Failed to fetch purchases: ${res.code()}"))
                }
                val remotePurchases = res.body()!!
                var syncedCount = 0
                for (p in remotePurchases) {
                    val pBizId = p.businessId
                    if (pBizId != companyId && companyId != 1L) {
                        continue
                    }

                    val existing = db.purchaseDao().getPurchaseByServerId(p.id)
                        ?: db.purchaseDao().getPurchaseByNumber(p.purchaseNumber)

                    val localSupplier = db.customerDao().getCustomerByServerContactId(p.contactId)
                    val supplierId = localSupplier?.id ?: 1L

                    val purchaseDateMillis = com.digitalchopda.app.util.DateTimeUtils.parseIsoDateToMillis(p.purchaseDate)

                    val purchaseEntity = (existing ?: PurchaseEntity(
                        companyId = companyId,
                        supplierId = supplierId,
                        purchaseNumber = p.purchaseNumber
                    )).copy(
                        companyId = companyId,
                        supplierId = supplierId,
                        purchaseNumber = p.purchaseNumber,
                        supplierInvoiceNo = p.supplierInvoiceNo ?: "",
                        vendorBillNumber = p.supplierInvoiceNo ?: "",
                        subtotal = p.subtotal,
                        taxAmount = p.taxAmount,
                        grandTotal = p.totalAmount,
                        paymentStatus = p.paymentStatus,
                        paymentMethod = p.paymentMethod ?: "cash",
                        notes = p.notes ?: "",
                        syncStatus = "SYNCED",
                        serverPurchaseId = p.id,
                        purchaseDate = purchaseDateMillis
                    )

                    val localPurchaseId = if (existing != null) {
                        db.purchaseDao().updatePurchase(purchaseEntity)
                        existing.id
                    } else {
                        val newId = db.purchaseDao().insertPurchase(purchaseEntity)
                        if (p.items.isNotEmpty()) {
                            val itemEntities = p.items.map { item ->
                                PurchaseItemEntity(
                                    purchaseId = newId,
                                    productName = item.productName,
                                    quantity = item.quantity,
                                    unitPrice = item.unitPrice,
                                    gstRate = item.gstRate,
                                    hsnCode = item.hsnCode
                                )
                            }
                            db.purchaseDao().insertPurchaseItems(itemEntities)
                        }
                        newId
                    }
                    syncedCount++
                }

                // Authoritative Scoped Pruning: delete stale local purchases not on server for companyId
                val remoteServerIds = remotePurchases.map { it.id }.toSet()
                val localPurchases = db.purchaseDao().getAllPurchasesSync(companyId)
                val stalePurchases = localPurchases.filter {
                    it.companyId == companyId &&
                    it.serverPurchaseId != null &&
                    it.serverPurchaseId !in remoteServerIds
                }
                for (sp in stalePurchases) {
                    db.purchaseDao().deletePurchase(sp.id)
                }

                // Authoritative Scoped Deduplication
                val remainingPurchases = db.purchaseDao().getAllPurchasesSync(companyId)
                val grouped = remainingPurchases.filter { it.serverPurchaseId != null }.groupBy { it.serverPurchaseId!! }
                for ((_, duplicates) in grouped) {
                    if (duplicates.size > 1) {
                        val sorted = duplicates.sortedByDescending { it.id }
                        for (dup in sorted.drop(1)) {
                            db.purchaseDao().deletePurchase(dup.id)
                        }
                    }
                }

                Result.success(syncedCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getSuppliers(companyId: Long): Flow<List<SupplierEntity>> {
        return db.supplierDao().getSuppliersForCompany(companyId)
    }

    suspend fun addSupplier(supplier: SupplierEntity): Long {
        return db.supplierDao().insertSupplier(supplier)
    }

    suspend fun getSupplierById(id: Long): SupplierEntity? {
        return db.supplierDao().getSupplierById(id)
    }
}
