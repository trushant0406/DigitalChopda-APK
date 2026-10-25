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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Enterprise Zero-Latency Multi-Trigger Network Reconnect Sync Monitor (Pillar 1)
 *
 * Automatically monitors device internet connectivity via ConnectivityManager.NetworkCallback:
 * 1. The instant internet connection is restored (WiFi, Cellular, Ethernet), this monitor
 *    debounces (1.2s) and triggers [InvoiceSyncWorker.enqueueImmediateSync] ASAP.
 * 2. Hooks into Application.ActivityLifecycleCallbacks to flush outbox immediately upon app foregrounding.
 * 3. Maintains a lightweight 45-second coroutine heartbeat pulse while the app is active in foreground,
 *    pulling remote depot updates (invoices, payments, contacts) silently in the background.
 * 4. Exposes [isOnline], [isSyncing], and [lastSyncTimestamp] StateFlows for real-time UI observability.
 */
object InstantNetworkSyncMonitor {

    private const val TAG = "NetworkSyncMonitor"
    private const val DEBOUNCE_MILLIS = 1200L
    private const val FOREGROUND_PULSE_INTERVAL_MS = 45_000L

    private val lastTriggerTime = AtomicLong(0L)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRegistered = false

    private val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pulseJob: Job? = null

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    fun start(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext

        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                // Check initial network state
                val activeNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNetwork)
                val currentlyOnline = caps != null &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                _isOnline.value = currentlyOnline

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()

                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "🟢 Internet connection restored. Scheduling immediate outbox sync.")
                        _isOnline.value = true
                        scheduleDebouncedSync(appContext)
                    }

                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        if (isValidated) {
                            Log.d(TAG, "📶 Network validated with internet capability.")
                            _isOnline.value = true
                            scheduleDebouncedSync(appContext)
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.w(TAG, "🔴 Internet connection lost. Transactions will queue offline safely.")
                        _isOnline.value = false
                    }
                })

                isRegistered = true
                Log.i(TAG, "InstantNetworkSyncMonitor registered successfully with ConnectivityManager.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }

        // Hook into Application.ActivityLifecycleCallbacks to flush outbox and start/stop 45s pulse
        if (appContext is android.app.Application) {
            appContext.registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                private var resumedActivities = 0

                override fun onActivityResumed(activity: android.app.Activity) {
                    resumedActivities++
                    if (resumedActivities == 1) {
                        Log.d(TAG, "📱 App entered foreground. Checking and flushing outbox sync.")
                        scheduleDebouncedSync(appContext)
                        startForegroundPulse(appContext)
                    }
                }

                override fun onActivityPaused(activity: android.app.Activity) {
                    resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
                    if (resumedActivities == 0) {
                        Log.d(TAG, "📱 App entered background. Pausing foreground sync pulse.")
                        stopForegroundPulse()
                    }
                }

                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                override fun onActivityStarted(activity: android.app.Activity) {}
                override fun onActivityStopped(activity: android.app.Activity) {}
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                override fun onActivityDestroyed(activity: android.app.Activity) {}
            })
        }
    }

    private fun startForegroundPulse(context: Context) {
        pulseJob?.cancel()
        pulseJob = monitorScope.launch {
            while (isActive) {
                delay(FOREGROUND_PULSE_INTERVAL_MS)
                if (_isOnline.value) {
                    Log.d(TAG, "💓 Foreground 45s smart pulse triggering outbox & delta sync.")
                    mainHandler.post {
                        InvoiceSyncWorker.enqueueImmediateSync(context)
                    }
                }
            }
        }
    }

    private fun stopForegroundPulse() {
        pulseJob?.cancel()
        pulseJob = null
    }

    fun triggerManualSync(context: Context) {
        Log.i(TAG, "⚡ Manual outbox flush & sync requested.")
        mainHandler.post {
            InvoiceSyncWorker.enqueueImmediateSync(context)
        }
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
        if (!syncing) {
            _lastSyncTimestamp.value = System.currentTimeMillis()
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
