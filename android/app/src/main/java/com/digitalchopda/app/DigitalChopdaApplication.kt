package com.digitalchopda.app

import android.app.Application
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.core.network.MockDigitalChopdaApiService
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

class DigitalChopdaApplication : Application() {

    lateinit var database: ChopdaDatabase
        private set

    lateinit var tokenStorage: TokenStorage
        private set

    lateinit var apiService: DigitalChopdaApiService
        private set

    lateinit var mockApi: MockDigitalChopdaApiService
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
        mockApi = MockDigitalChopdaApiService()
        apiService = NetworkClient.getApiService(tokenStorage)
        pdfGenerator = InvoicePdfGenerator(this)
        printerManager = BluetoothPrinterManager(this)

        companyRepository = CompanyRepository(database, tokenStorage, apiService)
        billingRepository = BillingRepository(database, apiService, mockApi)
        inventoryRepository = InventoryRepository(database, apiService, mockApi)
        leadRepository = LeadRepository(database)
        customerRepository = CustomerRepository(database, apiService)
        authRepository = AuthRepository(tokenStorage, apiService, database, mockApi)

        // Optimization 6: Schedule Automated WorkManager Background Outbox Sync
        try {
            com.digitalchopda.app.data.work.InvoiceSyncWorker.schedulePeriodicSync(this)
        } catch (_: Exception) {}
    }
}
