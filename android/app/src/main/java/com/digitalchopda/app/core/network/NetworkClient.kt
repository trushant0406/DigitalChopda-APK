package com.digitalchopda.app.core.network

import com.digitalchopda.app.BuildConfig
import com.digitalchopda.app.core.security.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    @Volatile
    private var apiService: DigitalChopdaApiService? = null

    @Volatile
    private var lastBaseUrl: String? = null

    fun getApiService(tokenStorage: TokenStorage): DigitalChopdaApiService {
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

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(AuthHeaderInterceptor(tokenStorage))
                .addInterceptor(loggingInterceptor)
                .build()

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
