package com.digitalchopda.app.core.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit REST Interface for DigitalChopda Central Depot (Backend API).
 */
interface DigitalChopdaApiService {

    // --- Authentication ---
    @POST("auth/login")
    suspend fun login(@Body request: ApiLoginRequest): Response<ApiLoginResponse>

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: ApiSendOtpRequest): Response<ApiSendOtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: ApiVerifyOtpRequest): Response<ApiLoginResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiLoginResponse>

    // --- Catalog / Products ---
    @GET("products")
    suspend fun getProducts(): Response<List<ApiProductDto>>

    @POST("products")
    suspend fun createProduct(@Body request: ApiCreateProductRequest): Response<ApiProductDto>

    @PATCH("products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body request: ApiCreateProductRequest): Response<ApiProductDto>

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): Response<Map<String, Any>>

    // --- Parties / Contacts ---
    @GET("contacts")
    suspend fun getContacts(@Query("q") query: String? = null): Response<List<ApiContactDto>>

    @POST("contacts")
    suspend fun createContact(@Body request: ApiCreateContactRequest): Response<ApiContactDto>

    @PATCH("contacts/{id}")
    suspend fun updateContact(@Path("id") id: Long, @Body request: ApiCreateContactRequest): Response<ApiContactDto>

    // --- Orders & Invoicing ---
    @GET("orders")
    suspend fun getOrders(@Query("status") status: String? = null): Response<List<ApiOrderDto>>

    @POST("orders")
    suspend fun createOrder(@Body request: ApiCreateOrderRequest): Response<ApiOrderDto>

    @PUT("orders/{id}")
    suspend fun updateOrder(@Path("id") id: Long, @Body request: ApiCreateOrderRequest): Response<ApiOrderDto>

    @PATCH("orders/{id}")
    suspend fun updateOrderStatus(
        @Path("id") id: Long,
        @Body request: Map<String, String>
    ): Response<ApiOrderDto>

    @DELETE("orders/{id}")
    suspend fun deleteOrder(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("orders/next-number")
    suspend fun getNextInvoiceNumber(): Response<ApiNextNumberResponse>

    @GET("orders/summary")
    suspend fun getOrdersSummary(): Response<ApiOrdersSummaryResponse>

    @GET("dashboard/summary")
    suspend fun getDashboardSummary(): Response<ApiDashboardSummaryResponse>

    @GET("settings/status")
    suspend fun getSettingsStatus(): Response<ApiSettingsStatusResponse>

    @POST("settings/whatsapp-web/reconnect")
    suspend fun reconnectWhatsAppWeb(): Response<ApiSettingsStatusResponse>

    // --- Official PDF Generation Engine (Backend) ---
    @GET("orders/{id}/pdf")
    @Streaming
    suspend fun getOrderPdf(@Path("id") id: Long): Response<ResponseBody>

    @GET("orders/{id}/shipping-label/pdf")
    @Streaming
    suspend fun getOrderShippingLabelPdf(@Path("id") id: Long): Response<ResponseBody>

    @GET("quotations")
    suspend fun getQuotations(@Query("q") query: String? = null): Response<List<ApiQuotationDto>>

    @POST("quotations")
    suspend fun createQuotation(@Body request: ApiQuotationRequest): Response<ApiQuotationDto>

    @GET("quotations/{id}/pdf")
    @Streaming
    suspend fun getQuotationPdf(@Path("id") id: Long): Response<ResponseBody>

    @GET("quotations/{id}/shipping-label/pdf")
    @Streaming
    suspend fun getQuotationShippingLabelPdf(@Path("id") id: Long): Response<ResponseBody>

    @POST("quotations/{id}/convert-to-invoice")
    suspend fun convertQuotationToInvoice(
        @Path("id") id: Long,
        @Body request: Map<String, String> = emptyMap()
    ): Response<ApiOrderDto>

    @GET("credit/customers/{id}/statement/pdf")
    @Streaming
    suspend fun getCustomerStatementPdf(@Path("id") id: Long): Response<ResponseBody>

    @GET("credit/payments/{id}/receipt/pdf")
    @Streaming
    suspend fun getPaymentReceiptPdf(@Path("id") id: Long): Response<ResponseBody>

    // --- WhatsApp Dispatch (Company WhatsApp Server) ---
    @POST("orders/{id}/send-whatsapp")
    suspend fun sendOrderViaCompanyWhatsApp(@Path("id") id: Long): Response<ApiSendWhatsAppResponse>

    @POST("quotations/{id}/send-whatsapp")
    suspend fun sendQuotationViaCompanyWhatsApp(@Path("id") id: Long): Response<ApiSendWhatsAppResponse>

    @POST("credit-notes/{id}/whatsapp")
    suspend fun sendCreditNoteViaCompanyWhatsApp(@Path("id") id: Long): Response<ApiSendWhatsAppResponse>

    @POST("credit/customers/{id}/statement/whatsapp")
    suspend fun sendStatementViaCompanyWhatsApp(@Path("id") id: Long): Response<ApiSendWhatsAppResponse>

    // --- Credit / Khata & Payment In ---
    @GET("credit/summary")
    suspend fun getCreditSummary(): Response<ApiCreditSummaryResponse>

    @POST("credit/payment-in")
    suspend fun createPaymentIn(@Body request: ApiPaymentInRequest): Response<ApiPaymentInResponse>

    @POST("credit/transactions")
    suspend fun createKhataTransaction(@Body request: ApiCreateKhataTransactionRequest): Response<ApiKhataTransactionDto>

    @GET("credit/customers/{id}/ledger")
    suspend fun getCustomerLedger(@Path("id") id: Long): Response<ApiCustomerLedgerResponse>

    // --- Multi-Tenancy Businesses ---
    @GET("businesses")
    suspend fun getBusinesses(): Response<List<ApiBusinessDto>>

    @POST("businesses")
    suspend fun createBusiness(@Body request: ApiCreateBusinessRequest): Response<ApiBusinessDto>

    // --- Sequence Leases (Offline Billing Engine) ---
    @POST("leases/acquire")
    suspend fun acquireLease(@Body request: ApiLeaseRequest): Response<ApiLeaseResponse>

    @GET("leases/current")
    suspend fun getCurrentLease(@Query("device_id") deviceId: String): Response<ApiLeaseResponse>

    // --- Inventory Rebase & Delta Sync ---
    @POST("inventory/rebase-delta")
    suspend fun rebaseStockDelta(@Body request: ApiStockSyncDeltaRequest): Response<ApiStockRebaseResponse>

    @GET("products/barcode/{barcode}")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): Response<ApiProductDto>

    // --- Credit Notes / Sales Returns ---
    @GET("credit-notes")
    suspend fun getCreditNotes(@Query("q") query: String? = null): Response<List<ApiCreditNoteDto>>

    @GET("credit-notes/{id}")
    suspend fun getCreditNoteById(@Path("id") id: Long): Response<ApiCreditNoteDto>

    @GET("credit-notes/{id}/pdf")
    @Streaming
    suspend fun getCreditNotePdf(@Path("id") id: Long): Response<ResponseBody>

    @POST("credit-notes")
    suspend fun createCreditNote(@Body request: ApiCreateCreditNoteRequest): Response<ApiCreditNoteDto>

    @DELETE("credit-notes/{id}")
    suspend fun voidCreditNote(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("credit-notes/orders/{orderId}/returnable-items")
    suspend fun getReturnableOrderItems(@Path("orderId") orderId: Long): Response<ApiReturnableItemsResponse>

    // --- Purchases (Inward Supplier Bills) ---
    @GET("purchases")
    suspend fun getPurchases(): Response<List<ApiPurchaseDto>>

    @POST("purchases")
    suspend fun createPurchase(@Body request: ApiCreatePurchaseRequest): Response<ApiPurchaseDto>

    @DELETE("purchases/{id}")
    suspend fun deletePurchase(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("purchases/next-number")
    suspend fun getNextPurchaseNumber(): Response<ApiNextNumberResponse>

    // --- Stock Adjustments & Inventory Audit ---
    @POST("inventory/adjust")
    suspend fun adjustStock(@Body request: ApiStockAdjustmentRequest): Response<ApiStockAdjustmentResponse>

    @GET("inventory/history/{productId}")
    suspend fun getStockHistory(@Path("productId") productId: Long): Response<List<ApiStockAdjustmentDto>>

    // --- Payment Void & Ledger Safeguards ---
    @DELETE("credit/payments/{id}")
    suspend fun deletePayment(
        @Path("id") id: Long,
        @Body request: Map<String, String> = emptyMap()
    ): Response<Map<String, Any>>

    // --- Business, GST & Audit Reports ---
    @GET("reports/gst/summary")
    suspend fun getGstSummary(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("period") period: String? = null
    ): Response<ApiGstSummaryResponse>

    @GET("reports/gst/gstr1-excel")
    @Streaming
    suspend fun downloadGstr1Excel(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/gst/gstr1-json")
    @Streaming
    suspend fun downloadGstr1Json(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/gst/gstr2-excel")
    @Streaming
    suspend fun downloadGstr2Excel(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/gst/gstr3b-excel")
    @Streaming
    suspend fun downloadGstr3bExcel(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/gst/hsn-excel")
    @Streaming
    suspend fun downloadHsnExcel(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/ca-bundle.zip")
    @Streaming
    suspend fun downloadCaBundleZip(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/sales/excel")
    @Streaming
    suspend fun downloadSalesRegister(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/purchases/excel")
    @Streaming
    suspend fun downloadPurchaseRegister(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/stock/excel")
    @Streaming
    suspend fun downloadStockReport(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/parties/excel")
    @Streaming
    suspend fun downloadPartyBalances(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/daybook/excel")
    @Streaming
    suspend fun downloadDayBook(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/profit-loss/excel")
    @Streaming
    suspend fun downloadProfitLoss(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ResponseBody>

    @GET("reports/anomalies")
    suspend fun getPreFilingAnomalies(): Response<List<ApiPreFilingAnomalyDto>>
}

