package com.digitalchopda.app.core.network

import com.digitalchopda.app.BuildConfig
import com.digitalchopda.app.core.security.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
import java.io.File

object NetworkClient {

    @Volatile
    private var apiService: DigitalChopdaApiService? = null

    @Volatile
    private var lastBaseUrl: String? = null

    fun getApiService(tokenStorage: TokenStorage, context: Context? = null): DigitalChopdaApiService {
        val currentBaseUrl = tokenStorage.getBaseUrl()

        val existing = apiService
        if (existing != null && lastBaseUrl == currentBaseUrl) {
            return existing
        }

        return synchronized(this) {
            val doubleCheck = apiService
            if (doubleCheck != null && lastBaseUrl == currentBaseUrl) {
                return@synchronized doubleCheck
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(8, 3, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .addInterceptor(AuthHeaderInterceptor(tokenStorage))
                .addInterceptor(loggingInterceptor)

            // 🌐 25MB HTTP Disk Cache for sub-millisecond offline catalog & settings caching
            context?.cacheDir?.let { cacheDir ->
                try {
                    val httpCacheDir = File(cacheDir, "http_cache")
                    clientBuilder.cache(Cache(httpCacheDir, 25L * 1024 * 1024))
                } catch (e: Exception) {
                    android.util.Log.w("NetworkClient", "Could not initialize OkHttp disk cache", e)
                }
            }

            val okHttpClient = clientBuilder.build()

            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val created = retrofit.create(DigitalChopdaApiService::class.java)
            apiService = created
            lastBaseUrl = currentBaseUrl
            created
        }
    }

    /**
     * Resets the cached Retrofit instance when base URL or network configuration changes.
     */
    fun reset() {
        synchronized(this) {
            apiService = null
            lastBaseUrl = null
        }
    }
}
