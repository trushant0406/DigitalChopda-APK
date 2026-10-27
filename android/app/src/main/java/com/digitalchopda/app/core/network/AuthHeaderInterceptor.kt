package com.digitalchopda.app.core.network

import android.util.Log
import com.digitalchopda.app.core.security.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects Central Depot Security Badge (Bearer JWT) and
 * Multi-Tenancy Stamp (X-Business-Id) into every outgoing request.
 *
 * Provides Intelligent Multi-Host Auto-Failover:
 * If the active local/dev server host is unreachable, it automatically cycles
 * through candidate hosts (Tailscale, Office Wi-Fi, Emulator loopback, and local host)
 * without failing user operations.
 */
class AuthHeaderInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    companion object {
        private const val TAG = "AuthHeaderInterceptor"

        /**
         * Candidate local/development Depot hosts ordered by priority.
         */
        val DEV_CANDIDATE_HOSTS = listOf(
            "127.0.0.1",    // 1. Local loopback (with adb reverse - zero latency)
            "100.99.250.0", // 2. Tailscale Dev Mesh
            "10.74.4.212",  // 3. Office / Local Wi-Fi LAN
            "10.0.2.2",     // 4. Android Emulator host loopback
            "localhost"
        )

        @Volatile
        var activeHostOverride: String? = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // 1. Bearer Token Authorization
        val token = tokenStorage.getAccessToken()
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        // 2. Multi-Tenant Business Context Isolation
        val businessId = tokenStorage.getActiveCompanyId()
        if (businessId > 0) {
            builder.header("X-Business-Id", businessId.toString())
        }

        builder.header("Accept", "application/json")

        var requestToExecute = builder.build()
        val originalHost = requestToExecute.url.host

        // If the original host is part of the local dev candidates, apply any known working override
        val isDevHost = DEV_CANDIDATE_HOSTS.contains(originalHost)
        val currentOverride = activeHostOverride
        if (isDevHost && currentOverride != null && currentOverride != originalHost) {
            val rewrittenUrl = requestToExecute.url.newBuilder().host(currentOverride).build()
            requestToExecute = requestToExecute.newBuilder().url(rewrittenUrl).build()
        }

        return try {
            val response = chain.proceed(requestToExecute)
            if (isDevHost && response.isSuccessful) {
                activeHostOverride = requestToExecute.url.host
            }
            response
        } catch (e: Exception) {
            if (!isDevHost) {
                throw e
            }

            Log.w(TAG, "Request to ${requestToExecute.url.host} failed (${e.message}). Attempting multi-host failover...")

            val failedHost = requestToExecute.url.host
            val remainingCandidates = DEV_CANDIDATE_HOSTS.filter { it != failedHost && it != "localhost" }

            var lastException: Exception = e
            for (candidateHost in remainingCandidates) {
                try {
                    val failoverUrl = requestToExecute.url.newBuilder().host(candidateHost).build()
                    val failoverReq = requestToExecute.newBuilder().url(failoverUrl).build()
                    Log.i(TAG, "Trying failover host: $candidateHost ...")
                    val response = chain.proceed(failoverReq)
                    if (response.isSuccessful || response.code < 500) {
                        Log.i(TAG, "🟢 Failover successful! Connected to Depot on $candidateHost")
                        activeHostOverride = candidateHost
                        return response
                    }
                } catch (candidateEx: Exception) {
                    lastException = candidateEx
                    Log.d(TAG, "Candidate host $candidateHost failed: ${candidateEx.message}")
                }
            }

            throw lastException
        }
    }
}
