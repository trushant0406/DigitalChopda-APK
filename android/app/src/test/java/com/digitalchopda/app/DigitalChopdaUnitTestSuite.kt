package com.digitalchopda.app

import com.digitalchopda.app.core.network.ApiCreateOrderRequest
import com.digitalchopda.app.core.network.ApiLoginRequest
import com.digitalchopda.app.core.network.ApiOrderItemRequest
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.core.network.LeaseRequest
import com.digitalchopda.app.core.network.MockDigitalChopdaApiService
import com.digitalchopda.app.core.network.StockSyncDelta
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.SequenceLease
import com.digitalchopda.app.domain.usecase.AllocateLeaseUseCase
import com.digitalchopda.app.domain.usecase.CreateInvoiceUseCase
import com.digitalchopda.app.domain.usecase.RebaseStockUseCase
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.runBlocking
import com.digitalchopda.app.core.localization.AppLanguage
import com.digitalchopda.app.core.localization.EnglishStrings
import com.digitalchopda.app.core.localization.GujaratiStrings
import com.digitalchopda.app.core.localization.HindiStrings
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.getStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitalChopdaUnitTestSuite {

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.format(24580.0)
        assertTrue(formatted.contains("24,580"))

        val withDecimals = CurrencyFormatter.format(12450.50, includeDecimals = true)
        assertTrue(withDecimals.contains("12,450.50"))
    }

    @Test
    fun testGstRule46SequenceLeaseAllocation() {
        val lease = SequenceLease(
            companyId = 1L,
            prefix = "DC",
            financialYear = "26-27",
            rangeStart = 1001L,
            rangeEnd = 1200L,
            currentCursor = 1001L
        )

        assertEquals("DC/26-27/01001", lease.currentFormattedNumber())
        assertEquals(200L, lease.totalCapacity)
        assertEquals(200L, lease.remainingCapacity)
        assertFalse(lease.needsRenewal)

        // Allocate next
        val useCase = AllocateLeaseUseCase()
        val result = useCase.allocateNextInvoiceNumber(lease)

        assertEquals("DC/26-27/01001", result.allocatedNumber)
        assertEquals(1002L, result.updatedLease.currentCursor)
        assertEquals("DC/26-27/01002", result.updatedLease.currentFormattedNumber())

        // Test lease near exhaustion triggers renewal
        val nearExhaustedLease = lease.copy(currentCursor = 1190L) // only 11 left out of 200 (5.5%)
        val renewalResult = useCase.allocateNextInvoiceNumber(nearExhaustedLease)
        assertTrue(renewalResult.shouldRequestReplenishment)
    }

    @Test
    fun testInvoiceTaxCalculationAndRoundOff() {
        val useCase = CreateInvoiceUseCase()
        val items = listOf(
            LineItem(
                title = "Item A",
                unitPrice = 1850.0,
                quantity = 2,
                gstRatePercent = 5.0
            ),
            LineItem(
                title = "Item B",
                unitPrice = 4200.0,
                quantity = 1,
                gstRatePercent = 5.0
            )
        )

        // Subtotal = (1850 * 2) + 4200 = 7900
        // GST (5%) = 395.0
        // CGST = 197.5, SGST = 197.5
        // Grand Total = 7900 + 395 = 8295.0
        val result = useCase.calculateTaxes(items, isInterState = false)

        assertEquals(7900.0, result.subtotal, 0.01)
        assertEquals(197.5, result.cgst, 0.01)
        assertEquals(197.5, result.sgst, 0.01)
        assertEquals(0.0, result.igst, 0.01)
        assertEquals(8295.0, result.grandTotal, 0.01)
        assertEquals(0.0, result.roundOff, 0.01)
    }

    @Test
    fun testInterStateIGSTCalculation() {
        val useCase = CreateInvoiceUseCase()
        val items = listOf(
            LineItem(
                title = "Electronics Item",
                unitPrice = 10000.0,
                quantity = 1,
                gstRatePercent = 18.0
            )
        )

        val result = useCase.calculateTaxes(items, isInterState = true)

        assertEquals(10000.0, result.subtotal, 0.01)
        assertEquals(0.0, result.cgst, 0.01)
        assertEquals(0.0, result.sgst, 0.01)
        assertEquals(1800.0, result.igst, 0.01)
        assertEquals(11800.0, result.grandTotal, 0.01)
    }

    @Test
    fun testStockDeltaRebasing() {
        val rebaseUseCase = RebaseStockUseCase()

        // Clean commit case
        val cleanResult = rebaseUseCase.applyDelta(
            currentServerQuantity = 50,
            currentServerVersion = 3,
            delta = RebaseStockUseCase.DeltaEvent(
                itemId = 101L,
                deltaQuantity = -2,
                baseVersion = 3
            )
        )
        assertTrue(cleanResult.committedCleanly)
        assertEquals(48, cleanResult.newQuantity)
        assertEquals(4, cleanResult.newVersion)

        // Drifted commit case (concurrent modifications handled optimistically)
        val driftedResult = rebaseUseCase.applyDelta(
            currentServerQuantity = 45, // drifted on server from 50 to 45
            currentServerVersion = 5,   // version advanced to 5
            delta = RebaseStockUseCase.DeltaEvent(
                itemId = 101L,
                deltaQuantity = -3,
                baseVersion = 3         // client based on older version 3
            )
        )
        assertFalse(driftedResult.committedCleanly)
        assertEquals(42, driftedResult.newQuantity) // 45 - 3 = 42
        assertEquals(6, driftedResult.newVersion)
    }

    @Test
    fun testMockBackendApiEndToEnd() = runBlocking {
        val mockApi = MockDigitalChopdaApiService()

        // 1. Test Login with 10-digit mobile and username 'admin'
        val loginRes = mockApi.login("9876543210", "123456")
        assertTrue(loginRes.isSuccess)
        val auth = loginRes.getOrThrow()
        assertTrue(auth.accessToken.startsWith("m_acc_"))
        assertEquals(3, auth.companies.size)
        assertEquals("Sharma Traders", auth.companies[0].firmName)
        assertEquals("Sharma Electronics & Mobile", auth.companies[1].firmName)
        assertEquals("Gujarat Super Mart", auth.companies[2].firmName)

        val adminLoginRes = mockApi.login("admin", "admin123")
        assertTrue(adminLoginRes.isSuccess)
        val adminAuth = adminLoginRes.getOrThrow()
        assertEquals("Savan Admin", adminAuth.ownerName)
        assertEquals("1", adminAuth.userId)

        // 2. Test Rule 46 Sequence Lease replenishment
        val leaseRes = mockApi.requestInvoiceLease(
            LeaseRequest(deviceId = "DEV-01", companyId = 1L, requestedBlockSize = 200)
        )
        assertTrue(leaseRes.isSuccess)
        val lease = leaseRes.getOrThrow()
        assertEquals("DC", lease.prefix)
        assertEquals("26-27", lease.financialYear)
        assertTrue(lease.rangeEnd > lease.rangeStart)

        // 3. Test Delta Stock Sync
        val stockRes = mockApi.syncStockDelta(
            StockSyncDelta(itemId = 1L, deltaQuantity = -5, baseVersion = 1)
        )
        assertTrue(stockRes.isSuccess)
        assertEquals(115, stockRes.getOrThrow().updatedQuantity)

        // 4. Test GST Statutory Report
        val reportRes = mockApi.fetchGstReport("October 2026")
        assertTrue(reportRes.isSuccess)
        val rep = reportRes.getOrThrow()
        assertEquals(12450.0, rep.netGstPayable, 0.01)
        assertEquals("READY TO FILE", rep.gstr1Status)

        // 5. Test Logout
        val logoutRes = mockApi.logout(auth.accessToken)
        assertTrue(logoutRes.isSuccess)
    }

    @Test
    fun testLiveTailscaleBackendE2E() = runBlocking {
        val tailscaleBaseUrl = "http://100.99.250.0:4000/api/"

        var authToken = ""
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val b = chain.request().newBuilder()
                if (authToken.isNotEmpty()) {
                    b.header("Authorization", "Bearer $authToken")
                    b.header("X-Business-Id", "1")
                }
                chain.proceed(b.build())
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(tailscaleBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(DigitalChopdaApiService::class.java)

        // 1. E2E Login test via Tailscale IP
        val loginRes = api.login(ApiLoginRequest("admin", "admin123"))
        assertTrue("Tailscale Login should succeed: ${loginRes.code()}", loginRes.isSuccessful)
        val loginBody = loginRes.body()
        assertNotNull(loginBody)
        authToken = loginBody!!.token ?: ""
        assertTrue("Auth token must be non-empty", authToken.isNotEmpty())
        assertEquals("admin", loginBody.user?.username)

        // 2. E2E Fetch Next Invoice Number via Tailscale IP
        val nextRes = api.getNextInvoiceNumber()
        assertTrue("Next invoice number query should succeed: ${nextRes.code()}", nextRes.isSuccessful)
        val nextBody = nextRes.body()
        assertNotNull(nextBody)
        assertTrue("Suggested number must not be blank", nextBody!!.suggestedNumber.isNotBlank())

        // 3. E2E Create Live Order / Invoice via Tailscale IP
        val testInvoiceNum = "TAILSCALE-E2E-${System.currentTimeMillis() % 100000}"
        val orderReq = ApiCreateOrderRequest(
            phone = "9825000001",
            name = "Tailscale Test Merchant",
            city = "Surat",
            items = listOf(
                ApiOrderItemRequest(
                    productName = "Tailscale Live Test Cloth",
                    quantity = 2.0,
                    unitPrice = 1200.0,
                    gstRate = 5.0
                )
            ),
            paymentStatus = "paid",
            paymentMethod = "upi",
            createInvoice = true,
            invoiceNumber = testInvoiceNum,
            notes = "E2E Verification over Tailscale IP 100.99.250.0"
        )

        val createRes = api.createOrder(orderReq)
        assertTrue("Creating order via Tailscale should succeed: ${createRes.code()}", createRes.isSuccessful)
        val orderBody = createRes.body()
        assertNotNull(orderBody)
        assertEquals(testInvoiceNum, orderBody!!.invoiceNumber)

        // 4. E2E Fetch Orders via Tailscale IP to confirm persistence
        val ordersRes = api.getOrders()
        assertTrue("Orders list query should succeed: ${ordersRes.code()}", ordersRes.isSuccessful)
        val orders = ordersRes.body()
        assertNotNull(orders)
        assertTrue(orders!!.any { it.invoiceNumber == testInvoiceNum })
    }

    @Test
    fun testLanguageEnumMetadata() {
        assertEquals("gu", AppLanguage.GUJARATI.code)
        assertEquals("Gujarati", AppLanguage.GUJARATI.displayName)
        assertEquals("ગુજરાતી", AppLanguage.GUJARATI.nativeName)

        assertEquals("hi", AppLanguage.HINDI.code)
        assertEquals("Hindi", AppLanguage.HINDI.displayName)
        assertEquals("हिन्दी", AppLanguage.HINDI.nativeName)

        assertEquals("en", AppLanguage.ENGLISH.code)
        assertEquals("English", AppLanguage.ENGLISH.displayName)
        assertEquals("English", AppLanguage.ENGLISH.nativeName)
    }

    @Test
    fun testLanguageManagerSwitchingAndFlowReactivity() {
        val manager = LanguageManager()

        // 1. Initial State should be Gujarati default
        assertEquals(AppLanguage.GUJARATI, manager.currentLanguage.value)
        assertEquals(GujaratiStrings, manager.strings.value)
        assertEquals("ડિજિટલ ચોપડો", manager.strings.value.appName)
        assertEquals("ચોપડો", manager.strings.value.navChopda)
        assertEquals("બિલિંગ", manager.strings.value.navBilling)

        // 2. Cycle to Hindi
        val cycle1 = manager.cycleNextLanguage()
        assertEquals(AppLanguage.HINDI, cycle1)
        assertEquals(AppLanguage.HINDI, manager.currentLanguage.value)
        assertEquals(HindiStrings, manager.strings.value)
        assertEquals("डिजिटल चोपड़ा", manager.strings.value.appName)
        assertEquals("चोपड़ा", manager.strings.value.navChopda)
        assertEquals("बिलिंग", manager.strings.value.navBilling)
        assertEquals("थर्मल बिल", manager.strings.value.thermalSlipBtn)

        // 3. Cycle to English
        val cycle2 = manager.cycleNextLanguage()
        assertEquals(AppLanguage.ENGLISH, cycle2)
        assertEquals(AppLanguage.ENGLISH, manager.currentLanguage.value)
        assertEquals(EnglishStrings, manager.strings.value)
        assertEquals("DigitalChopda", manager.strings.value.appName)
        assertEquals("Chopda", manager.strings.value.navChopda)
        assertEquals("Billing", manager.strings.value.navBilling)
        assertEquals("Thermal Bill", manager.strings.value.thermalSlipBtn)

        // 4. Cycle back to Gujarati (Full Loop)
        val cycle3 = manager.cycleNextLanguage()
        assertEquals(AppLanguage.GUJARATI, cycle3)
        assertEquals(AppLanguage.GUJARATI, manager.currentLanguage.value)
        assertEquals(GujaratiStrings, manager.strings.value)
        assertEquals("ડિજિટલ ચોપડો", manager.strings.value.appName)

        // 5. Direct setter
        manager.setLanguage(AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, manager.currentLanguage.value)
        assertEquals(HindiStrings, manager.strings.value)

        manager.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, manager.currentLanguage.value)
        assertEquals(EnglishStrings, manager.strings.value)
    }

    @Test
    fun testLanguageBundlesCompletenessAndNonEmptyFields() {
        val allBundles = listOf(
            "Gujarati" to GujaratiStrings,
            "Hindi" to HindiStrings,
            "English" to EnglishStrings
        )

        for ((name, bundle) in allBundles) {
            assertTrue("$name appName must not be blank", bundle.appName.isNotBlank())
            assertTrue("$name sacredInvocation must not be blank", bundle.sacredInvocation.isNotBlank())
            assertTrue("$name appSubtitle must not be blank", bundle.appSubtitle.isNotBlank())
            assertTrue("$name navChopda must not be blank", bundle.navChopda.isNotBlank())
            assertTrue("$name navBilling must not be blank", bundle.navBilling.isNotBlank())
            assertTrue("$name navStock must not be blank", bundle.navStock.isNotBlank())
            assertTrue("$name navGst must not be blank", bundle.navGst.isNotBlank())
            assertTrue("$name navSettings must not be blank", bundle.navSettings.isNotBlank())
            assertTrue("$name tabParties must not be blank", bundle.tabParties.isNotBlank())
            assertTrue("$name tabCashbook must not be blank", bundle.tabCashbook.isNotBlank())
            assertTrue("$name tabInvoices must not be blank", bundle.tabInvoices.isNotBlank())
            assertTrue("$name youWillGet must not be blank", bundle.youWillGet.isNotBlank())
            assertTrue("$name youWillGive must not be blank", bundle.youWillGive.isNotBlank())
            assertTrue("$name billingTitle must not be blank", bundle.billingTitle.isNotBlank())
            assertTrue("$name printReceiptBtn must not be blank", bundle.printReceiptBtn.isNotBlank())
            assertTrue("$name thermalSlipBtn must not be blank", bundle.thermalSlipBtn.isNotBlank())
            assertTrue("$name saveDraftBtn must not be blank", bundle.saveDraftBtn.isNotBlank())
            assertTrue("$name saveAndSendBtn must not be blank", bundle.saveAndSendBtn.isNotBlank())
            assertTrue("$name invoiceNumberRule46 must not be blank", bundle.invoiceNumberRule46.isNotBlank())
            assertTrue("$name billToCustomer must not be blank", bundle.billToCustomer.isNotBlank())
            assertTrue("$name changeCustomer must not be blank", bundle.changeCustomer.isNotBlank())
            assertTrue("$name addItemsServices must not be blank", bundle.addItemsServices.isNotBlank())
            assertTrue("$name totalStockValue must not be blank", bundle.totalStockValue.isNotBlank())
            assertTrue("$name lowStockAlert must not be blank", bundle.lowStockAlert.isNotBlank())
            assertTrue("$name biometricUnlockBtn must not be blank", bundle.biometricUnlockBtn.isNotBlank())
        }
    }

    @Test
    fun testLanguageScriptAuthenticity() {
        // Gujarati script regex (\u0A80-\u0AFF)
        val gujaratiRegex = Regex("[\\u0A80-\\u0AFF]")
        assertTrue("Gujarati appName must contain Gujarati characters", gujaratiRegex.containsMatchIn(GujaratiStrings.appName))
        assertTrue("Gujarati navChopda must contain Gujarati characters", gujaratiRegex.containsMatchIn(GujaratiStrings.navChopda))
        assertTrue("Gujarati sacredInvocation must contain Gujarati characters", gujaratiRegex.containsMatchIn(GujaratiStrings.sacredInvocation))

        // Devanagari script regex (\u0900-\u097F)
        val devanagariRegex = Regex("[\\u0900-\\u097F]")
        assertTrue("Hindi appName must contain Devanagari characters", devanagariRegex.containsMatchIn(HindiStrings.appName))
        assertTrue("Hindi navChopda must contain Devanagari characters", devanagariRegex.containsMatchIn(HindiStrings.navChopda))
        assertTrue("Hindi sacredInvocation must contain Devanagari characters", devanagariRegex.containsMatchIn(HindiStrings.sacredInvocation))

        // English appName
        assertEquals("DigitalChopda", EnglishStrings.appName)
        assertEquals("Chopda", EnglishStrings.navChopda)
    }
}
