package com.digitalchopda.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY id ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCompany(): CompanyEntity?

    @Query("SELECT * FROM companies WHERE isActive = 1 LIMIT 1")
    fun getActiveCompanyFlow(): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getCompanyById(id: Long): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Update
    suspend fun updateCompany(company: CompanyEntity)

    @Query("UPDATE companies SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE companies SET isActive = 1 WHERE id = :id")
    suspend fun setActiveCompany(id: Long)

    @Query("SELECT COUNT(*) FROM companies")
    suspend fun getCompanyCount(): Int
}

@Dao
interface LeaseDao {
    @Query("SELECT * FROM sequence_leases WHERE companyId = :companyId ORDER BY id DESC LIMIT 1")
    suspend fun getActiveLease(companyId: Long): LeaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLease(lease: LeaseEntity): Long

    @Update
    suspend fun updateLease(lease: LeaseEntity)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE companyId = :companyId ORDER BY id DESC")
    fun getInvoicesForCompany(companyId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) ORDER BY id DESC")
    fun getInvoicesForCustomer(companyId: Long, phone: String, name: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE companyId = :companyId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentInvoices(companyId: Long, limit: Int = 10): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE companyId = :companyId AND invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(companyId: Long, invoiceNumber: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE serverOrderId = :serverOrderId LIMIT 1")
    suspend fun getInvoiceByServerOrderId(serverOrderId: Long): InvoiceEntity?

    @Query("SELECT COUNT(*) FROM invoices WHERE companyId = :companyId")
    suspend fun getInvoiceCount(companyId: Long): Int

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE companyId = :companyId")
    suspend fun getTotalSales(companyId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)

    @Query("UPDATE invoices SET serverOrderId = :serverOrderId, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateServerOrderIdAndSyncStatus(id: Long, serverOrderId: Long, syncStatus: String)

    @Query("SELECT * FROM invoices WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) AND UPPER(status) != 'PAID' ORDER BY id ASC")
    fun getUnpaidInvoicesForCustomer(companyId: Long, phone: String, name: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) AND UPPER(status) != 'PAID' ORDER BY id ASC")
    suspend fun getUnpaidInvoicesForCustomerSync(companyId: Long, phone: String, name: String): List<InvoiceEntity>

    @Query("UPDATE invoices SET paidAmount = :paidAmount, status = :status WHERE id = :id")
    suspend fun updatePaidAmountAndStatus(id: Long, paidAmount: Double, status: String)

    @Query("UPDATE invoices SET fulfillmentStatus = :fulfillmentStatus WHERE id = :id")
    suspend fun updateFulfillmentStatus(id: Long, fulfillmentStatus: String)

    @Query("SELECT * FROM invoices WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncInvoices(): List<InvoiceEntity>

    @Query("SELECT COUNT(*) FROM invoices WHERE syncStatus = 'PENDING_SYNC'")
    fun observePendingSyncInvoiceCount(): Flow<Int>

    @Query("UPDATE invoices SET whatsappSentCount = whatsappSentCount + 1, lastWhatsappSentAt = :timestamp WHERE id = :id")
    suspend fun updateWhatsAppSent(id: Long, timestamp: Long)

    @Query("UPDATE invoices SET syncAttempts = syncAttempts + 1 WHERE id = :id")
    suspend fun incrementSyncAttempts(id: Long)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE companyId = :companyId ORDER BY id DESC")
    fun getPaymentsForCompany(companyId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) ORDER BY id DESC")
    fun getPaymentsForParty(companyId: Long, phone: String, name: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncPayments(): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payments WHERE syncStatus = 'PENDING_SYNC'")
    fun observePendingSyncPaymentCount(): Flow<Int>

    @Query("UPDATE payments SET syncStatus = :syncStatus, serverPaymentId = :serverId, receiptNumber = :receiptNumber WHERE id = :id")
    suspend fun markSynced(id: Long, serverId: Long, receiptNumber: String, syncStatus: String = "SYNCED")

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): PaymentEntity?

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePayment(id: Long)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM inventory_items WHERE companyId = :companyId ORDER BY itemName ASC")
    fun getItemsForCompany(companyId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE companyId = :companyId AND quantity <= minStockThreshold")
    fun getLowStockItems(companyId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>): List<Long>

    @Transaction
    suspend fun upsertItemsBatch(items: List<ItemEntity>, chunkSize: Int = 100) {
        items.chunked(chunkSize).forEach { chunk ->
            insertItems(chunk)
        }
    }

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE inventory_items SET quantity = :quantity, version = :version, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun updateStockQuantityAndVersion(id: Long, quantity: Int, version: Int, lastUpdated: Long)
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads WHERE companyId = :companyId ORDER BY followUpTimestamp ASC")
    fun getLeadsForCompany(companyId: Long): Flow<List<LeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity): Long

    @Update
    suspend fun updateLead(lead: LeadEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE companyId = :companyId ORDER BY name ASC")
    fun getCustomersForCompany(companyId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE companyId = :companyId AND phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(companyId: Long, phone: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET balance = balance + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Query("UPDATE customers SET leadPriority = :priority, expectedDealValue = :expectedDealValue, leadNotes = :notes, nextFollowUpDate = :followUpDate WHERE id = :id")
    suspend fun updateLeadInfo(id: Long, priority: String, expectedDealValue: Double, notes: String, followUpDate: Long?)

    @Query("UPDATE customers SET creditLimit = :creditLimit WHERE id = :id")
    suspend fun updateCreditLimit(id: Long, creditLimit: Double)

    @Query("SELECT * FROM customers WHERE companyId = :companyId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(companyId: Long, query: String): Flow<List<CustomerEntity>>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)

    @Query("SELECT COUNT(*) FROM customers WHERE companyId = :companyId")
    suspend fun getCustomerCount(companyId: Long): Int

    @Query("SELECT * FROM customers WHERE companyId = :companyId AND serverContactId IS NULL")
    suspend fun getUnsyncedCustomers(companyId: Long): List<CustomerEntity>

    @Query("UPDATE customers SET serverContactId = :serverContactId, serverOutstanding = :outstanding WHERE id = :id")
    suspend fun updateServerContactId(id: Long, serverContactId: Long, outstanding: Double)

    @Query("SELECT * FROM customers WHERE serverContactId = :serverContactId LIMIT 1")
    suspend fun getCustomerByServerContactId(serverContactId: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE companyId = :companyId")
    suspend fun getCustomersSync(companyId: Long): List<CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers WHERE serverContactId IS NULL")
    fun observePendingSyncCustomerCount(): Flow<Int>
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations WHERE companyId = :companyId ORDER BY id DESC")
    fun getQuotationsForCompany(companyId: Long): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) ORDER BY id DESC")
    fun getQuotationsForCustomer(companyId: Long, phone: String, name: String): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    suspend fun getQuotationById(id: Long): QuotationEntity?

    @Query("SELECT * FROM quotations WHERE companyId = :companyId AND quotationNumber = :quotationNumber LIMIT 1")
    suspend fun getQuotationByNumber(companyId: Long, quotationNumber: String): QuotationEntity?

    @Query("SELECT COUNT(*) FROM quotations WHERE companyId = :companyId")
    suspend fun getQuotationCount(companyId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: QuotationEntity): Long

    @Update
    suspend fun updateQuotation(quotation: QuotationEntity)

    @Query("SELECT * FROM quotations WHERE serverQuotationId = :serverQuotationId LIMIT 1")
    suspend fun getQuotationByServerId(serverQuotationId: Long): QuotationEntity?

    @Query("UPDATE quotations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE quotations SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)

    @Query("UPDATE quotations SET serverQuotationId = :serverQuotationId, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateServerQuotationIdAndSyncStatus(id: Long, serverQuotationId: Long, syncStatus: String)

    @Query("SELECT * FROM quotations WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncQuotations(): List<QuotationEntity>

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteQuotation(id: Long)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE companyId = :companyId ORDER BY name ASC")
    fun getSuppliersForCompany(companyId: Long): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET balanceToPay = balanceToPay + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Query("SELECT * FROM suppliers WHERE companyId = :companyId AND phone = :phone LIMIT 1")
    suspend fun getSupplierByPhone(companyId: Long, phone: String): SupplierEntity?

    @Query("SELECT COUNT(*) FROM suppliers WHERE companyId = :companyId")
    suspend fun getSupplierCount(companyId: Long): Int

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplier(id: Long)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE companyId = :companyId ORDER BY purchaseDate DESC")
    fun getPurchasesForCompany(companyId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE companyId = :companyId AND supplierId = :supplierId ORDER BY purchaseDate DESC")
    fun getPurchasesForSupplier(companyId: Long, supplierId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: Long): PurchaseEntity?

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    fun getPurchaseItemsFlow(purchaseId: Long): Flow<List<PurchaseItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deletePurchase(id: Long)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deletePurchaseItems(purchaseId: Long)

    @Query("SELECT * FROM purchases WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncPurchases(): List<PurchaseEntity>

    @Query("UPDATE purchases SET syncStatus = :syncStatus, serverPurchaseId = :serverId WHERE id = :id")
    suspend fun markPurchaseSynced(id: Long, serverId: Long, syncStatus: String = "SYNCED")

    @Query("SELECT COUNT(*) FROM purchases WHERE companyId = :companyId")
    fun getPurchaseCount(companyId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM purchases WHERE companyId = :companyId")
    suspend fun getPurchaseCountSync(companyId: Long): Int

    @Query("SELECT SUM(grandTotal) FROM purchases WHERE companyId = :companyId")
    suspend fun getTotalPurchases(companyId: Long): Double?

    @Query("SELECT * FROM purchases WHERE serverPurchaseId = :serverId LIMIT 1")
    suspend fun getPurchaseByServerId(serverId: Long): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE purchaseNumber = :purchaseNumber LIMIT 1")
    suspend fun getPurchaseByNumber(purchaseNumber: String): PurchaseEntity?
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE companyId = :companyId ORDER BY name ASC")
    fun getProductsForCompany(companyId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE companyId = :companyId AND (name LIKE '%' || :query || '%' OR hsnCode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(companyId: Long, query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE companyId = :companyId AND ((minStockAlert > 0 AND stockQuantity <= minStockAlert) OR stockQuantity <= 0) ORDER BY stockQuantity ASC, name ASC")
    fun getLowStockProducts(companyId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE companyId = :companyId AND stockQuantity <= 0 ORDER BY name ASC")
    fun getOutOfStockProducts(companyId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE companyId = :companyId AND name = :name LIMIT 1")
    suspend fun getProductByName(companyId: Long, name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :newStock, version = version + 1 WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Double)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)

    @Query("SELECT COUNT(*) FROM products WHERE companyId = :companyId")
    fun getProductCount(companyId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE companyId = :companyId")
    suspend fun getProductCountSync(companyId: Long): Int
}

@Dao
interface CreditNoteDao {
    @Query("SELECT * FROM credit_notes WHERE companyId = :companyId ORDER BY id DESC")
    fun getCreditNotesForCompany(companyId: Long): Flow<List<CreditNoteEntity>>

    @Query("SELECT * FROM credit_notes WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) ORDER BY id DESC")
    fun getCreditNotesForCustomer(companyId: Long, phone: String, name: String): Flow<List<CreditNoteEntity>>

    @Query("SELECT * FROM credit_notes WHERE id = :id LIMIT 1")
    suspend fun getCreditNoteById(id: Long): CreditNoteEntity?

    @Query("SELECT * FROM credit_notes WHERE companyId = :companyId AND creditNoteNumber = :creditNoteNumber LIMIT 1")
    suspend fun getCreditNoteByNumber(companyId: Long, creditNoteNumber: String): CreditNoteEntity?

    @Query("SELECT COUNT(*) FROM credit_notes WHERE companyId = :companyId")
    suspend fun getCreditNoteCount(companyId: Long): Int

    @Query("SELECT SUM(totalAmount) FROM credit_notes WHERE companyId = :companyId AND status = 'ACTIVE'")
    suspend fun getTotalRefunds(companyId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditNote(creditNote: CreditNoteEntity): Long

    @Update
    suspend fun updateCreditNote(creditNote: CreditNoteEntity)

    @Query("UPDATE credit_notes SET status = 'VOIDED' WHERE id = :id")
    suspend fun voidCreditNote(id: Long)

    @Query("DELETE FROM credit_notes WHERE id = :id")
    suspend fun deleteCreditNote(id: Long)

    @Query("SELECT * FROM credit_notes WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncCreditNotes(): List<CreditNoteEntity>

    @Query("UPDATE credit_notes SET syncStatus = :syncStatus, serverCreditNoteId = :serverId WHERE id = :id")
    suspend fun markSynced(id: Long, serverId: Long, syncStatus: String = "SYNCED")

    @Query("SELECT * FROM credit_notes WHERE serverCreditNoteId = :serverId LIMIT 1")
    suspend fun getCreditNoteByServerId(serverId: Long): CreditNoteEntity?
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments WHERE companyId = :companyId AND productId = :productId ORDER BY createdAt DESC")
    fun getAdjustmentsForProduct(companyId: Long, productId: Long): Flow<List<StockAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustmentEntity): Long

    @Query("SELECT * FROM stock_adjustments WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncAdjustments(): List<StockAdjustmentEntity>

    @Query("UPDATE stock_adjustments SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun markAdjustmentSynced(id: Long, syncStatus: String = "SYNCED")

    @Query("SELECT COUNT(*) FROM stock_adjustments WHERE syncStatus = 'PENDING_SYNC'")
    fun observePendingSyncAdjustmentCount(): Flow<Int>
}

@Dao
interface OrderRevisionDao {
    @Query("SELECT * FROM order_revisions WHERE orderId = :orderId ORDER BY revisionNumber ASC")
    fun getRevisionsForOrder(orderId: Long): Flow<List<OrderRevisionEntity>>

    @Query("SELECT COUNT(*) FROM order_revisions WHERE orderId = :orderId")
    suspend fun getRevisionCount(orderId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: OrderRevisionEntity): Long
}

