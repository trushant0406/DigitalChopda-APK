package com.digitalchopda.app.core.network

import com.digitalchopda.app.core.security.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects Central Depot Security Badge (Bearer JWT) and
 * Multi-Tenancy Stamp (X-Business-Id) into every outgoing request.
 */
class AuthHeaderInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

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

        return chain.proceed(builder.build())
    }
}
