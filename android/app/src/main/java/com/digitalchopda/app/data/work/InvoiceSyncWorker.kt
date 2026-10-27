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

            // 1. Sync contacts/parties (resolves serverContactId for offline parties and pulls remote updates)
            try {
                app.customerRepository.syncContactsWithServer(activeCompany.id)
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Contact sync step encountered an issue: ${e.message}")
            }

            // 2. Sync products catalog (pulls latest catalog without wiping stock)
            try {
                app.productRepository.syncProductsWithServer(activeCompany.id)
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Product sync step encountered an issue: ${e.message}")
            }

            // 3. Flush pending stock adjustments outbox
            try {
                app.productRepository.syncPendingStockAdjustments()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Stock adjustment outbox sync encountered an issue: ${e.message}")
            }

            // 4. Flush pending invoices outbox & pull remote invoices
            try {
                app.billingRepository.syncPendingInvoices()
                app.billingRepository.syncInvoicesWithServer(activeCompany.id)
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Invoice sync step encountered an issue: ${e.message}")
            }

            // 5. Flush pending purchases outbox & pull remote purchases
            try {
                app.purchaseRepository.syncPendingPurchases()
                app.purchaseRepository.syncPurchasesWithServer(activeCompany.id)
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Purchase sync step encountered an issue: ${e.message}")
            }

            // 6. Sync pending payments & khata debits
            try {
                app.billingRepository.syncPendingPayments()
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Payment sync step encountered an issue: ${e.message}")
            }

            // 7. Sync pending quotations outbox & pull remote quotations
            try {
                app.quotationRepository.syncPendingQuotations()
                app.quotationRepository.syncQuotationsWithServer(activeCompany.id)
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Quotation sync step encountered an issue: ${e.message}")
            }

            // 8. Sync pending credit notes outbox & pull remote credit notes
            try {
                app.creditNoteRepository.syncPendingCreditNotes()
                app.creditNoteRepository.syncCreditNotesWithServer(activeCompany.id)
            } catch (e: Exception) {
                hasTransientErrors = true
                android.util.Log.w("InvoiceSyncWorker", "Credit note sync step encountered an issue: ${e.message}")
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
                ExistingWorkPolicy.KEEP,
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
