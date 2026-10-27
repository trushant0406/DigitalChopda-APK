package com.digitalchopda.app.core.network

import android.content.Context
import android.util.Log
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.data.work.InvoiceSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RealtimeEventSubscriber {
    private const val TAG = "RealtimeSync"
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(context: Context) {
        if (job?.isActive == true) return

        job = scope.launch {
            val app = context.applicationContext as? DigitalChopdaApplication ?: return@launch
            val tokenStorage = app.tokenStorage

            val sseClient = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val candidateHosts = listOf("100.99.250.0", "127.0.0.1", "10.74.4.212", "10.0.2.2")

            while (isActive) {
                var connected = false
                val activeCompany = try { app.companyRepository.getActiveCompany() } catch (_: Exception) { null }
                val companyId = activeCompany?.id ?: 1L
                val token = tokenStorage.getAccessToken()

                for (host in candidateHosts) {
                    if (!isActive) break
                    val syncUrl = "http://$host:3000/api/sync/events?business_id=$companyId"
                    try {
                        Log.d(TAG, "Attempting SSE connection to $syncUrl ...")
                        val reqBuilder = Request.Builder()
                            .url(syncUrl)
                            .header("Accept", "text/event-stream")
                            .header("Cache-Control", "no-cache")
                            .header("Connection", "keep-alive")
                            .header("X-Business-Id", companyId.toString())

                        if (token.isNotBlank()) {
                            reqBuilder.header("Authorization", "Bearer $token")
                        }

                        val call = sseClient.newCall(reqBuilder.build())
                        val response = call.execute()

                        if (response.isSuccessful && response.body != null) {
                            Log.i(TAG, "🟢 SSE Connected successfully to $syncUrl")
                            connected = true
                            AuthHeaderInterceptor.activeHostOverride = host
                            val reader = response.body!!.byteStream().bufferedReader()
                            var currentEvent: String? = null
                            var line: String? = null

                            while (isActive && reader.readLine().also { line = it } != null) {
                            val l = line?.trim() ?: continue
                            if (l.startsWith("event:")) {
                                currentEvent = l.substringAfter("event:").trim()
                            } else if (l.startsWith("data:")) {
                                val dataStr = l.substringAfter("data:").trim()
                                if (currentEvent == "sync") {
                                    Log.d(TAG, "Real-time sync event received: $dataStr")
                                    try {
                                        val json = JSONObject(dataStr)
                                        val entity = json.optString("entity")

                                        // 1. Immediately trigger Outbox topological sync via WorkManager
                                        InvoiceSyncWorker.enqueueImmediateSync(app)

                                        // 2. Proactively delta-fetch specific entity in background
                                        when (entity) {
                                            "products", "inventory" -> {
                                                app.productRepository.syncProductsWithServer(companyId)
                                            }
                                            "orders", "invoices" -> {
                                                app.billingRepository.syncInvoicesWithServer(companyId)
                                                app.customerRepository.syncContactsWithServer(companyId)
                                            }
                                            "contacts", "parties" -> {
                                                app.customerRepository.syncContactsWithServer(companyId)
                                            }
                                            "payments", "credit" -> {
                                                app.billingRepository.syncPendingPayments()
                                                app.billingRepository.syncInvoicesWithServer(companyId)
                                                app.customerRepository.syncContactsWithServer(companyId)
                                            }
                                            "purchases", "expenses" -> {
                                                app.purchaseRepository.syncPurchasesWithServer(companyId)
                                                app.customerRepository.syncContactsWithServer(companyId)
                                                app.productRepository.syncProductsWithServer(companyId)
                                            }
                                            "quotations" -> {
                                                app.quotationRepository.syncQuotationsWithServer(companyId)
                                            }
                                            "credit_notes" -> {
                                                app.creditNoteRepository.syncCreditNotesWithServer(companyId)
                                                app.customerRepository.syncContactsWithServer(companyId)
                                                app.billingRepository.syncInvoicesWithServer(companyId)
                                                app.productRepository.syncProductsWithServer(companyId)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Error handling sync event: ${e.message}")
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "SSE connection to $host failed with code: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "SSE host $host dropped/unreachable: ${e.message}")
                }
                if (connected) break
            }
            if (!connected) {
                delay(5000)
            }
        }
    }
}

    fun stop() {
        job?.cancel()
        job = null
    }

    fun restartForCompany(context: Context, companyId: Long) {
        Log.i(TAG, "Re-tuning SSE radio subscriber for company #$companyId")
        stop()
        start(context)
    }
}
