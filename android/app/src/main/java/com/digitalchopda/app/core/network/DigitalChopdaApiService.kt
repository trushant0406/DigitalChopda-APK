package com.digitalchopda.app.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit REST Interface for DigitalChopda Central Depot (Backend API).
 */
interface DigitalChopdaApiService {

    // --- Authentication ---
    @POST("auth/login")
    suspend fun login(@Body request: ApiLoginRequest): Response<ApiLoginResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiLoginResponse>

    // --- Catalog / Products ---
    @GET("products")
    suspend fun getProducts(): Response<List<ApiProductDto>>

    @POST("products")
    suspend fun createProduct(@Body request: ApiCreateProductRequest): Response<ApiProductDto>

    // --- Parties / Contacts ---
    @GET("contacts")
    suspend fun getContacts(@Query("q") query: String? = null): Response<List<ApiContactDto>>

    // --- Orders & Invoicing ---
    @GET("orders")
    suspend fun getOrders(@Query("status") status: String? = null): Response<List<ApiOrderDto>>

    @POST("orders")
    suspend fun createOrder(@Body request: ApiCreateOrderRequest): Response<ApiOrderDto>

    @PUT("orders/{id}")
    suspend fun updateOrder(@Path("id") id: Long, @Body request: ApiCreateOrderRequest): Response<ApiOrderDto>

    @DELETE("orders/{id}")
    suspend fun deleteOrder(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("orders/next-number")
    suspend fun getNextInvoiceNumber(): Response<ApiNextNumberResponse>

    // --- WhatsApp Dispatch (Company WhatsApp Server) ---
    @POST("orders/{id}/send-whatsapp")
    suspend fun sendOrderViaCompanyWhatsApp(@Path("id") id: Long): Response<ApiSendWhatsAppResponse>

    @POST("credit/customers/{id}/statement/whatsapp")
    suspend fun sendStatementViaCompanyWhatsApp(@Path("id") id: Long): Response<ApiSendWhatsAppResponse>

    // --- Credit / Khata & Payment In ---
    @GET("credit/summary")
    suspend fun getCreditSummary(): Response<ApiCreditSummaryResponse>

    @POST("credit/payment-in")
    suspend fun createPaymentIn(@Body request: ApiPaymentInRequest): Response<ApiPaymentInResponse>

    @GET("credit/customers/{id}/ledger")
    suspend fun getCustomerLedger(@Path("id") id: Long): Response<ApiCustomerLedgerResponse>

    // --- Multi-Tenancy Businesses ---
    @GET("businesses")
    suspend fun getBusinesses(): Response<List<ApiBusinessDto>>
}
