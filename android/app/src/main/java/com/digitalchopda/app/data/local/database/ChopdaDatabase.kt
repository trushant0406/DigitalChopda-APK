package com.digitalchopda.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.digitalchopda.app.data.local.dao.CompanyDao
import com.digitalchopda.app.data.local.dao.CustomerDao
import com.digitalchopda.app.data.local.dao.InvoiceDao
import com.digitalchopda.app.data.local.dao.ItemDao
import com.digitalchopda.app.data.local.dao.LeadDao
import com.digitalchopda.app.data.local.dao.LeaseDao
import com.digitalchopda.app.data.local.entity.CompanyEntity
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.ItemEntity
import com.digitalchopda.app.data.local.entity.LeadEntity
import com.digitalchopda.app.data.local.entity.LeaseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CompanyEntity::class,
        LeaseEntity::class,
        InvoiceEntity::class,
        ItemEntity::class,
        LeadEntity::class,
        CustomerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChopdaDatabase : RoomDatabase() {

    abstract fun companyDao(): CompanyDao
    abstract fun leaseDao(): LeaseDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun itemDao(): ItemDao
    abstract fun leadDao(): LeadDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: ChopdaDatabase? = null

        fun getInstance(context: Context): ChopdaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChopdaDatabase::class.java,
                    "digital_chopda_v1.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate realistic seed data for immediate out-of-the-box experience
                            CoroutineScope(Dispatchers.IO).launch {
                                populateSeedData(getInstance(context))
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateSeedData(db: ChopdaDatabase) {
            // Seed Companies
            val c1 = db.companyDao().insertCompany(
                CompanyEntity(
                    firmName = "Sharma Traders",
                    ownerName = "Amit Sharma",
                    gstin = "24AAACS1429B1Z8",
                    phone = "+91 98765 43210",
                    address = "Shop 12, Ring Road Cloth Market, Surat, Gujarat",
                    stateCode = "24",
                    financialYear = "26-27",
                    invoicePrefix = "DC",
                    isActive = true,
                    upiVpa = "sharmatraders@oksbi"
                )
            )

            val c2 = db.companyDao().insertCompany(
                CompanyEntity(
                    firmName = "Sharma Electronics & Mobile",
                    ownerName = "Amit Sharma",
                    gstin = "24BBCDE5678C1Z4",
                    phone = "+91 98765 43211",
                    address = "2nd Floor, Electronic Complex, Ahmedabad, Gujarat",
                    stateCode = "24",
                    financialYear = "26-27",
                    invoicePrefix = "SEM",
                    isActive = false,
                    upiVpa = "sharmaelec@icici"
                )
            )

            val c3 = db.companyDao().insertCompany(
                CompanyEntity(
                    firmName = "Gujarat Super Mart",
                    ownerName = "Amit Sharma",
                    gstin = "24XYZAB9876D1Z2",
                    phone = "+91 98765 43212",
                    address = "Near Central Bus Depot, Vadodara, Gujarat",
                    stateCode = "24",
                    financialYear = "26-27",
                    invoicePrefix = "GSM",
                    isActive = false,
                    upiVpa = "gujmart@axisbank"
                )
            )

            // Seed Sequence Leases (Rule 46)
            db.leaseDao().insertOrUpdateLease(
                LeaseEntity(
                    companyId = c1,
                    prefix = "DC",
                    financialYear = "26-27",
                    rangeStart = 1001L,
                    rangeEnd = 1200L,
                    currentCursor = 1001L
                )
            )
            db.leaseDao().insertOrUpdateLease(
                LeaseEntity(
                    companyId = c2,
                    prefix = "SEM",
                    financialYear = "26-27",
                    rangeStart = 2001L,
                    rangeEnd = 2200L,
                    currentCursor = 2001L
                )
            )

            // Seed Inventory Items
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-COT-01",
                    itemName = "Pure Cotton Shahi Saree 6.3m",
                    packSize = "Box of 1",
                    quantity = 42,
                    sellingPrice = 1850.0,
                    costPrice = 1200.0,
                    gstRatePercent = 5.0,
                    minStockThreshold = 10,
                    version = 1
                )
            )
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-SILK-02",
                    itemName = "Kanjivaram Jacquard Silk",
                    packSize = "Box of 1",
                    quantity = 28,
                    sellingPrice = 4200.0,
                    costPrice = 2900.0,
                    gstRatePercent = 5.0,
                    minStockThreshold = 8,
                    version = 1
                )
            )
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-KUR-03",
                    itemName = "Embroidered Kurti Set M/L/XL",
                    packSize = "Pack of 3",
                    quantity = 3, // Low stock alert!
                    sellingPrice = 1450.0,
                    costPrice = 900.0,
                    gstRatePercent = 12.0,
                    minStockThreshold = 5,
                    version = 1
                )
            )
            db.itemDao().insertItem(
                ItemEntity(
                    companyId = c1,
                    sku = "TEX-DUP-04",
                    itemName = "Bandhani Georgette Dupatta",
                    packSize = "Single",
                    quantity = 2, // Low stock alert!
                    sellingPrice = 650.0,
                    costPrice = 380.0,
                    gstRatePercent = 5.0,
                    minStockThreshold = 6,
                    version = 1
                )
            )

            // Seed Customers (Khatavahi Accounts)
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Ramesh Patel (Textiles)",
                    phone = "+91 98251 22340",
                    gstin = "24AABCR1234F1Z8",
                    balance = 18500.0,
                    address = "Ring Road Market, Surat"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Shreenathji Silk Mills",
                    phone = "+91 97240 11980",
                    gstin = "24BBDCS5678G1Z2",
                    balance = -6400.0,
                    address = "Pandesara GIDC, Surat"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Ananya Fashion Hub",
                    phone = "+91 98980 55432",
                    gstin = "24CCDCA9012H1Z5",
                    balance = 12300.0,
                    address = "Ghod Dod Road, Surat"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Bhavin Enterprise",
                    phone = "+91 94260 77123",
                    gstin = "24DDDCE3456I1Z9",
                    balance = 4200.0,
                    address = "Katargam, Surat"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Maheshbhai Kirana Store",
                    phone = "+91 98244 88990",
                    balance = -1420.0,
                    address = "Rander Road, Surat"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Pooja Varma",
                    phone = "+91 98251 12345",
                    gstin = "24AABCV9988E1Z1",
                    balance = 4120.0,
                    address = "Adajan, Surat"
                )
            )
            db.customerDao().insertCustomer(
                CustomerEntity(
                    companyId = c1,
                    name = "Rajesh Patel",
                    phone = "+91 97240 54321",
                    balance = 0.0,
                    address = "Varachha, Surat"
                )
            )

            // Seed Leads
            db.leadDao().insertLead(
                LeadEntity(
                    companyId = c1,
                    customerName = "Sneha Boutique",
                    businessName = "Sneha Shah",
                    phone = "+91 98980 11223",
                    priority = "HOT",
                    requirementDetails = "Urgent requirement for 50 Designer Kurtis for Diwali wedding exhibition",
                    expectedDealValue = 45000.0,
                    followUpTimestamp = System.currentTimeMillis() + 86400000L
                )
            )
            db.leadDao().insertLead(
                LeadEntity(
                    companyId = c1,
                    customerName = "Manish Textiles Wholesale",
                    businessName = "Manish Agrawal",
                    phone = "+91 94260 77889",
                    priority = "WARM",
                    requirementDetails = "Bulk sample order for 200 Banarasi Duppatas",
                    expectedDealValue = 85000.0,
                    followUpTimestamp = System.currentTimeMillis() + (86400000L * 2)
                )
            )

            // Seed Recent Invoices
            db.invoiceDao().insertInvoice(
                InvoiceEntity(
                    companyId = c1,
                    invoiceNumber = "DC/26-27/01001",
                    customerName = "Pooja Varma",
                    customerPhone = "+91 98251 12345",
                    itemsSummary = "Pure Cotton Shahi Saree (x2), Kanjivaram Jacquard Silk (x1)",
                    subtotal = 7900.0,
                    cgst = 197.5,
                    sgst = 197.5,
                    igst = 0.0,
                    roundOff = 0.0,
                    grandTotal = 8295.0,
                    status = "PAID",
                    paymentMode = "UPI",
                    syncStatus = "SYNCED",
                    idempotencyKey = java.util.UUID.randomUUID().toString()
                )
            )
        }
    }
}
