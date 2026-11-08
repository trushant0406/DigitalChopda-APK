package com.digitalchopda.app

import android.app.Application
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.core.network.NetworkClient
import com.digitalchopda.app.core.printer.BluetoothPrinterManager
import com.digitalchopda.app.core.security.TokenStorage
import com.digitalchopda.app.data.local.database.ChopdaDatabase
import com.digitalchopda.app.data.repository.AuthRepository
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.InventoryRepository
import com.digitalchopda.app.data.repository.LeadRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DigitalChopdaApplication : Application() {

    lateinit var database: ChopdaDatabase
        private set

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var apiService: DigitalChopdaApiService
        private set

    lateinit var pdfGenerator: InvoicePdfGenerator
        private set

    lateinit var printerManager: BluetoothPrinterManager
        private set

    lateinit var companyRepository: CompanyRepository
        private set

    lateinit var billingRepository: BillingRepository
        private set

    lateinit var inventoryRepository: InventoryRepository
        private set

    lateinit var leadRepository: LeadRepository
        private set

    lateinit var customerRepository: CustomerRepository
        private set

    lateinit var quotationRepository: com.digitalchopda.app.data.repository.QuotationRepository
        private set

    lateinit var creditNoteRepository: com.digitalchopda.app.data.repository.CreditNoteRepository
        private set

    lateinit var productRepository: com.digitalchopda.app.data.repository.ProductRepository
        private set

    lateinit var purchaseRepository: com.digitalchopda.app.data.repository.PurchaseRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var languageManager: com.digitalchopda.app.core.localization.LanguageManager
        private set

    lateinit var biometricAuthManager: com.digitalchopda.app.core.security.BiometricAuthManager
        private set

    companion object {
        lateinit var instance: DigitalChopdaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("DigitalChopda", "CRITICAL UNCAUGHT EXCEPTION on ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        languageManager = com.digitalchopda.app.core.localization.LanguageManager(this)
        biometricAuthManager = com.digitalchopda.app.core.security.BiometricAuthManager(this)
        database = ChopdaDatabase.getInstance(this)
        tokenStorage = TokenStorage(this)
        apiService = NetworkClient.getApiService(tokenStorage, this)
        pdfGenerator = InvoicePdfGenerator(this)
        printerManager = BluetoothPrinterManager(this)

        companyRepository = CompanyRepository(database, tokenStorage, apiService)
        billingRepository = BillingRepository(database, apiService)
        inventoryRepository = InventoryRepository(database, apiService)
        leadRepository = LeadRepository(database)
        customerRepository = CustomerRepository(database, apiService)
        quotationRepository = com.digitalchopda.app.data.repository.QuotationRepository(database, apiService)
        creditNoteRepository = com.digitalchopda.app.data.repository.CreditNoteRepository(database, apiService)
        productRepository = com.digitalchopda.app.data.repository.ProductRepository(database, apiService)
        purchaseRepository = com.digitalchopda.app.data.repository.PurchaseRepository(database, apiService)
        authRepository = AuthRepository(
            tokenStorage = tokenStorage,
            apiService = apiService,
            db = database,
            billingRepository = billingRepository,
            quotationRepository = quotationRepository,
            customerRepository = customerRepository,
            productRepository = productRepository
        )

        // 🏎️ Cold-Start Acceleration: Offload secondary background engines & soundbox to background thread
        CoroutineScope(Dispatchers.Default).launch {
            try {
                com.digitalchopda.app.core.sound.SoundEffectsManager.init(this@DigitalChopdaApplication)
                com.digitalchopda.app.data.work.InvoiceSyncWorker.schedulePeriodicSync(this@DigitalChopdaApplication)
                com.digitalchopda.app.core.network.InstantNetworkSyncMonitor.start(this@DigitalChopdaApplication)
                com.digitalchopda.app.core.network.RealtimeEventSubscriber.start(this@DigitalChopdaApplication)
            } catch (e: Exception) {
                android.util.Log.w("DigitalChopda", "Background service initialization warning", e)
            }
        }
    }
}
