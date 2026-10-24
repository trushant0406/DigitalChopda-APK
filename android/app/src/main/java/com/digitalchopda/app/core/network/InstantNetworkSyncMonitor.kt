package com.digitalchopda.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.digitalchopda.app.data.work.InvoiceSyncWorker
import java.util.concurrent.atomic.AtomicLong

/**
 * Enterprise Zero-Latency Network Reconnect Sync Monitor
 *
 * Automatically monitors device internet connectivity via ConnectivityManager.NetworkCallback.
 * The instant internet connection is restored (WiFi, Cellular, Ethernet), this monitor
 * debounces and triggers [InvoiceSyncWorker.enqueueImmediateSync] ASAP without waiting
 * for periodic WorkManager intervals.
 *
 * Also monitors app foreground transitions via ProcessLifecycleOwner to flush pending
 * outbox entries immediately upon app launch or resume.
 */
object InstantNetworkSyncMonitor {

    private const val TAG = "NetworkSyncMonitor"
    private const val DEBOUNCE_MILLIS = 1200L

    private val lastTriggerTime = AtomicLong(0L)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRegistered = false

    fun start(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext

        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()

                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "🟢 Internet connection restored. Scheduling immediate outbox sync.")
                        scheduleDebouncedSync(appContext)
                    }

                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                            Log.d(TAG, "📶 Network validated with internet capability.")
                            scheduleDebouncedSync(appContext)
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.w(TAG, "🔴 Internet connection lost. Transactions will queue offline safely.")
                    }
                })

                isRegistered = true
                Log.i(TAG, "InstantNetworkSyncMonitor registered successfully with ConnectivityManager.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }

        // Also hook into Application.ActivityLifecycleCallbacks to flush outbox on app foreground/start
        if (appContext is android.app.Application) {
            appContext.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                private var resumedActivities = 0

                override fun onActivityResumed(activity: android.app.Activity) {
                    resumedActivities++
                    if (resumedActivities == 1) {
                        Log.d(TAG, "📱 App entered foreground. Checking and flushing outbox sync.")
                        scheduleDebouncedSync(appContext)
                    }
                }

                override fun onActivityPaused(activity: android.app.Activity) {
                    resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
                }

                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                override fun onActivityStarted(activity: android.app.Activity) {}
                override fun onActivityStopped(activity: android.app.Activity) {}
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                override fun onActivityDestroyed(activity: android.app.Activity) {}
            })
        }
    }

    private fun scheduleDebouncedSync(context: Context) {
        val now = System.currentTimeMillis()
        val prev = lastTriggerTime.get()

        if (now - prev >= DEBOUNCE_MILLIS) {
            if (lastTriggerTime.compareAndSet(prev, now)) {
                mainHandler.post {
                    InvoiceSyncWorker.enqueueImmediateSync(context)
                }
            }
        }
    }
}
