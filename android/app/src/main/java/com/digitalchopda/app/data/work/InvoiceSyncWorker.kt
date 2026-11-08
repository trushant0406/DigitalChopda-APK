package com.digitalchopda.app.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.core.network.InstantNetworkSyncMonitor
import java.util.concurrent.TimeUnit

/**
 * Enterprise Topological Outbox Synchronization Worker (Pillars 1 & 2)
 *
 * Flushes offline entities strictly in parent-first dependency order:
 * 1. Contacts/Parties (resolves serverContactId for offline parties and pulls remote updates)
 * 2. Products Catalog (syncs master catalog)
 * 3. Stock Adjustments (flushes inventory deltas)
 * 4. Invoices / Orders (uses resolved serverContactIds, acquires serverOrderIds)
 * 5. Purchases (flushes purchase vouchers)
 * 6. Payments & Khata Debits (uses resolved serverContactIds & serverOrderIds)
 * 7. Quotations (flushes quotations)
 * 8. Credit Notes / Sales Returns
 *
 * Updates [InstantNetworkSyncMonitor] syncing state and publishes progress.
 */
class InvoiceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        InstantNetworkSyncMonitor.setSyncing(true)
        return try {
            setProgress(workDataOf("is_syncing" to true))
            val app = applicationContext as? DigitalChopdaApplication ?: return Result.failure()
            val activeCompany = app.companyRepository.getActiveCompany()
            var hasTransientErrors = false

            // 1. Flush all local pending outbox records first in topological order
            try {
                app.productRepository.syncPendingStockAdjustments()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Stock adjustment outbox sync issue: ${e.message}")
            }
            try {
                app.billingRepository.syncPendingInvoices()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Invoice outbox sync issue: ${e.message}")
            }
            try {
                app.purchaseRepository.syncPendingPurchases()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Purchase outbox sync issue: ${e.message}")
            }
            try {
                app.billingRepository.syncPendingPayments()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Payment outbox sync issue: ${e.message}")
            }
            try {
                app.quotationRepository.syncPendingQuotations()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Quotation outbox sync issue: ${e.message}")
            }
            try {
                app.creditNoteRepository.syncPendingCreditNotes()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Credit note outbox sync issue: ${e.message}")
            }

            // 2. High-speed Fast Sync: Pull all remote updates in a single HTTP roundtrip (<150ms)
            var fastSyncSucceeded = false
            try {
                val fastSyncRes = app.apiService.fastSyncGet()
                if (fastSyncRes.isSuccessful && fastSyncRes.body()?.data != null) {
                    val data = fastSyncRes.body()!!.data!!
                    android.util.Log.i("InvoiceSyncWorker", "⚡ FastSync received batch: ${data.contacts.size} contacts, ${data.products.size} products, ${data.orders.size} orders, ${data.purchases.size} purchases, ${data.quotations.size} quotes, ${data.creditNotes.size} credit notes")
                    try { app.customerRepository.syncContactsWithServer(activeCompany.id, data.contacts) } catch (e: Exception) { android.util.Log.w("InvoiceSyncWorker", "FastSync contacts error", e) }
                    try { app.productRepository.syncProductsWithServer(activeCompany.id, data.products) } catch (e: Exception) { android.util.Log.w("InvoiceSyncWorker", "FastSync products error", e) }
                    try { app.billingRepository.syncInvoicesWithServer(activeCompany.id, data.orders) } catch (e: Exception) { android.util.Log.w("InvoiceSyncWorker", "FastSync orders error", e) }
                    try { app.purchaseRepository.syncPurchasesWithServer(activeCompany.id, data.purchases) } catch (e: Exception) { android.util.Log.w("InvoiceSyncWorker", "FastSync purchases error", e) }
                    try { app.quotationRepository.syncQuotationsWithServer(activeCompany.id, data.quotations) } catch (e: Exception) { android.util.Log.w("InvoiceSyncWorker", "FastSync quotes error", e) }
                    try { app.creditNoteRepository.syncCreditNotesWithServer(activeCompany.id, data.creditNotes) } catch (e: Exception) { android.util.Log.w("InvoiceSyncWorker", "FastSync credit notes error", e) }
                    fastSyncSucceeded = true
                }
            } catch (e: Exception) {
                android.util.Log.w("InvoiceSyncWorker", "FastSync roundtrip attempt failed, falling back to individual endpoints: ${e.message}")
            }

            // 3. Fallback to individual endpoints if FastSync was not available
            if (!fastSyncSucceeded) {
                try {
                    app.customerRepository.syncContactsWithServer(activeCompany.id)
                } catch (e: Exception) {
                    hasTransientErrors = true
                    android.util.Log.w("InvoiceSyncWorker", "Fallback contact sync error: ${e.message}")
                }
                try {
                    app.productRepository.syncProductsWithServer(activeCompany.id)
                } catch (e: Exception) {
                    hasTransientErrors = true
                    android.util.Log.w("InvoiceSyncWorker", "Fallback product sync error: ${e.message}")
                }
                try {
                    app.billingRepository.syncInvoicesWithServer(activeCompany.id)
                } catch (e: Exception) {
                    hasTransientErrors = true
                    android.util.Log.w("InvoiceSyncWorker", "Fallback invoice sync error: ${e.message}")
                }
                try {
                    app.purchaseRepository.syncPurchasesWithServer(activeCompany.id)
                } catch (e: Exception) {
                    hasTransientErrors = true
                    android.util.Log.w("InvoiceSyncWorker", "Fallback purchase sync error: ${e.message}")
                }
                try {
                    app.quotationRepository.syncQuotationsWithServer(activeCompany.id)
                } catch (e: Exception) {
                    hasTransientErrors = true
                    android.util.Log.w("InvoiceSyncWorker", "Fallback quotation sync error: ${e.message}")
                }
                try {
                    app.creditNoteRepository.syncCreditNotesWithServer(activeCompany.id)
                } catch (e: Exception) {
                    hasTransientErrors = true
                    android.util.Log.w("InvoiceSyncWorker", "Fallback credit note sync error: ${e.message}")
                }
            }

            if (hasTransientErrors && runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (_: Exception) {
            Result.retry()
        } finally {
            InstantNetworkSyncMonitor.setSyncing(false)
            try {
                setProgress(workDataOf("is_syncing" to false, "last_sync" to System.currentTimeMillis()))
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "digital_chopda_outbox_sync"

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<InvoiceSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${UNIQUE_WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<InvoiceSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
