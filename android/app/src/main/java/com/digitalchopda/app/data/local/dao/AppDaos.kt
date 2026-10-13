package com.digitalchopda.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.digitalchopda.app.data.local.entity.CompanyEntity
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.ItemEntity
import com.digitalchopda.app.data.local.entity.LeadEntity
import com.digitalchopda.app.data.local.entity.LeaseEntity
import com.digitalchopda.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY id ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCompany(): CompanyEntity?

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

    @Query("SELECT * FROM invoices WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncInvoices(): List<InvoiceEntity>

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE companyId = :companyId ORDER BY id DESC")
    fun getPaymentsForCompany(companyId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE companyId = :companyId AND (customerPhone = :phone OR customerName = :name) ORDER BY id DESC")
    fun getPaymentsForParty(companyId: Long, phone: String, name: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncPayments(): List<PaymentEntity>

    @Query("UPDATE payments SET syncStatus = :syncStatus, serverPaymentId = :serverId, receiptNumber = :receiptNumber WHERE id = :id")
    suspend fun markSynced(id: Long, serverId: Long, receiptNumber: String, syncStatus: String = "SYNCED")
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET balance = balance + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Query("SELECT * FROM customers WHERE companyId = :companyId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchCustomers(companyId: Long, query: String): Flow<List<CustomerEntity>>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)
}
