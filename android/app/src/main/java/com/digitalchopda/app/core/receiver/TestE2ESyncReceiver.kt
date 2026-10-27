package com.digitalchopda.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 🛰️ TestE2ESyncReceiver:
 * Broadcast receiver enabling empirical end-to-end testing of data creation
 * directly on the physical Pixel 8 Pro device, triggering real-time sync to the Central Depot
 * and broadcasting live updates to the Web Dashboard.
 */
class TestE2ESyncReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TestE2ESyncReceiver"
        const val ACTION_CREATE_PARTY = "com.digitalchopda.app.ACTION_CREATE_TEST_PARTY"
        const val ACTION_CREATE_PAYMENT = "com.digitalchopda.app.ACTION_CREATE_TEST_PAYMENT"
        const val ACTION_CREATE_ALLOCATED_PAYMENT = "com.digitalchopda.app.ACTION_CREATE_ALLOCATED_PAYMENT"
        const val ACTION_CREATE_INVOICE = "com.digitalchopda.app.ACTION_CREATE_TEST_INVOICE"
        const val ACTION_SYNC_NOW = "com.digitalchopda.app.ACTION_SYNC_NOW"
        const val ACTION_SWITCH_COMPANY = "com.digitalchopda.app.ACTION_SWITCH_COMPANY"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "🟢 [TestE2ESyncReceiver] Received broadcast action: $action")
        val app = DigitalChopdaApplication.instance

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val company = app.companyRepository.getActiveCompany()
                val companyId = company.id

                when (action) {
                    ACTION_CREATE_PARTY -> {
                        val name = intent.getStringExtra("name") ?: "Pixel Mobile VIP"
                        val phone = intent.getStringExtra("phone") ?: "9876543210"
                        val city = intent.getStringExtra("city") ?: "Ahmedabad"
                        val partyType = intent.getStringExtra("partyType") ?: "customer"

                        Log.i(TAG, "Creating Party on Pixel: name=$name, phone=$phone, type=$partyType")
                        val party = CustomerEntity(
                            companyId = companyId,
                            name = name,
                            phone = phone,
                            city = city,
                            balance = 0.0,
                            partyType = partyType
                        )
                        val res = app.customerRepository.createParty(party)
                        Log.i(TAG, "Party created with result: $res")
                    }

                    ACTION_CREATE_PAYMENT -> {
                        val name = intent.getStringExtra("name") ?: "Pixel Mobile VIP"
                        val phone = intent.getStringExtra("phone") ?: "9876543210"
                        val amount = intent.getDoubleExtra("amount", 2500.0)
                        val notes = intent.getStringExtra("notes") ?: "Mobile Field Payment"

                        var customer = app.database.customerDao().getCustomerByPhone(companyId, phone)
                        if (customer == null) {
                            val party = CustomerEntity(
                                companyId = companyId,
                                name = name,
                                phone = phone,
                                city = "Ahmedabad",
                                balance = 0.0,
                                partyType = "customer"
                            )
                            val res = app.customerRepository.createParty(party)
                            val localId = res.getOrDefault(0L)
                            customer = app.database.customerDao().getCustomerById(localId)
                        }

                        val customerId = customer?.id ?: 1L
                        Log.i(TAG, "Recording Payment on Pixel: customerId=$customerId, amount=$amount")
                        val pmtId = app.customerRepository.recordPayment(
                            companyId = companyId,
                            customerId = customerId,
                            amount = amount,
                            isJama = true,
                            paymentMode = "cash",
                            notes = notes
                        )
                        Log.i(TAG, "Payment recorded with local ID: $pmtId. Flushing pending outbox...")
                        app.billingRepository.syncPendingPayments()
                        Log.i(TAG, "Payment outbox flush complete.")
                    }

                    ACTION_CREATE_ALLOCATED_PAYMENT -> {
                        val name = intent.getStringExtra("name") ?: "Pixel Mobile VIP"
                        val phone = intent.getStringExtra("phone") ?: "9876543210"
                        val notes = intent.getStringExtra("notes") ?: "Mobile Field Multi-Bill Allocation"

                        val customer = app.database.customerDao().getCustomerByPhone(companyId, phone)
                            ?: app.database.customerDao().getCustomersSync(companyId).firstOrNull()

                        if (customer != null) {
                            val allInvoices = app.database.invoiceDao().getAllInvoicesSync(companyId)
                            val unpaidInvoices = allInvoices.filter {
                                (it.customerPhone == customer.phone || it.customerName == customer.name) &&
                                !it.status.equals("PAID", ignoreCase = true) &&
                                it.balanceDue > 0.0
                            }

                            if (unpaidInvoices.isNotEmpty()) {
                                val allocations = mutableListOf<com.digitalchopda.app.core.network.ApiPaymentAllocationItemDto>()
                                var totalAllocated = 0.0

                                for (inv in unpaidInvoices.take(2)) {
                                    val amt = Math.min(inv.balanceDue, 500.0)
                                    if (amt > 0.0) {
                                        allocations.add(
                                            com.digitalchopda.app.core.network.ApiPaymentAllocationItemDto(
                                                orderId = inv.serverOrderId ?: inv.id,
                                                amount = amt
                                            )
                                        )
                                        totalAllocated += amt
                                    }
                                }

                                if (allocations.isNotEmpty()) {
                                    Log.i(TAG, "Recording Allocated Payment on Pixel for customer=${customer.id}, total=$totalAllocated across ${allocations.size} bills")
                                    val pmtId = app.customerRepository.recordPayment(
                                        companyId = companyId,
                                        customerId = customer.id,
                                        amount = totalAllocated,
                                        isJama = true,
                                        paymentMode = "cash",
                                        notes = notes,
                                        allocations = allocations
                                    )
                                    Log.i(TAG, "Allocated payment recorded with local ID: $pmtId. Flushing pending outbox...")
                                    app.billingRepository.syncPendingPayments()
                                    Log.i(TAG, "Allocated payment outbox flush complete.")
                                } else {
                                    Log.w(TAG, "No unpaid invoices with positive balance for customer ${customer.id}")
                                }
                            } else {
                                Log.w(TAG, "No unpaid invoices found for customer ${customer.id}")
                            }
                        } else {
                            Log.w(TAG, "Customer not found for phone $phone")
                        }
                    }

                    ACTION_CREATE_INVOICE -> {
                        val name = intent.getStringExtra("name") ?: "Pixel Mobile VIP"
                        val phone = intent.getStringExtra("phone") ?: "9876543210"
                        val amount = intent.getDoubleExtra("amount", 1500.0)
                        val itemName = intent.getStringExtra("item") ?: "Diamond Polish Wheel"

                        val invNum = "MOB-${System.currentTimeMillis() % 100000}"
                        val lineItem = LineItem(
                            title = itemName,
                            unitPrice = amount,
                            quantity = 1,
                            gstRatePercent = 18.0
                        )
                        val invoice = Invoice(
                            companyId = companyId,
                            invoiceNumber = invNum,
                            customerName = name,
                            customerPhone = phone,
                            items = listOf(lineItem),
                            subtotal = amount,
                            cgst = amount * 0.09,
                            sgst = amount * 0.09,
                            igst = 0.0,
                            roundOff = 0.0,
                            grandTotal = amount * 1.18,
                            paidAmount = 0.0,
                            status = InvoiceStatus.UNPAID,
                            paymentMode = PaymentMode.CASH,
                            syncStatus = SyncStatus.PENDING_SYNC
                        )
                        Log.i(TAG, "Saving and dispatching Invoice on Pixel: $invNum for $name ($phone)")
                        val invRes = app.billingRepository.saveAndDispatchInvoice(company, invoice)
                        Log.i(TAG, "Invoice saveAndDispatch result: $invRes")
                        app.billingRepository.syncPendingInvoices()
                        Log.i(TAG, "Invoice sync complete.")
                    }

                    ACTION_SYNC_NOW -> {
                        Log.i(TAG, "Manual full sync requested.")
                        com.digitalchopda.app.data.work.InvoiceSyncWorker.enqueueImmediateSync(app)
                        app.customerRepository.syncContactsWithServer(companyId)
                        app.productRepository.syncProductsWithServer(companyId)
                        app.billingRepository.syncPendingInvoices()
                        app.billingRepository.syncInvoicesWithServer(companyId)
                        app.purchaseRepository.syncPurchasesWithServer(companyId)
                        app.billingRepository.syncPendingPayments()
                        app.quotationRepository.syncQuotationsWithServer(companyId)
                        app.creditNoteRepository.syncCreditNotesWithServer(companyId)
                        app.productRepository.syncPendingStockAdjustments()
                        Log.i(TAG, "Full sync complete.")
                    }

                    ACTION_SWITCH_COMPANY -> {
                        val targetId = intent.getLongExtra("companyId", intent.getIntExtra("companyId", 1).toLong())
                        Log.i(TAG, "Switching active company to $targetId")
                        app.companyRepository.switchActiveCompany(targetId)
                        app.customerRepository.syncContactsWithServer(targetId)
                        app.productRepository.syncProductsWithServer(targetId)
                        app.billingRepository.syncInvoicesWithServer(targetId)
                        app.purchaseRepository.syncPurchasesWithServer(targetId)
                        app.quotationRepository.syncQuotationsWithServer(targetId)
                        app.creditNoteRepository.syncCreditNotesWithServer(targetId)
                        Log.i(TAG, "Switched and synced company $targetId complete.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔴 Error processing test broadcast: ${e.message}", e)
            }
        }
    }
}
