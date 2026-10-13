package com.digitalchopda.app.core.network

import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.InventoryItem
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.Lead
import com.digitalchopda.app.domain.model.SequenceLease
import kotlinx.coroutines.delay
import java.util.UUID

// DTOs & Contracts
data class LoginRequest(val mobile: String, val password: String?)
data class OtpVerifyRequest(val mobile: String, val otp: String)
data class AuthResponse(
    val success: Boolean,
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val ownerName: String,
    val companies: List<Company>,
    val message: String? = null
)

data class LeaseRequest(
    val deviceId: String,
    val companyId: Long,
    val requestedBlockSize: Int = 200
)

data class LeaseResponse(
    val success: Boolean,
    val prefix: String,
    val financialYear: String,
    val rangeStart: Long,
    val rangeEnd: Long
)

data class StockSyncDelta(
    val itemId: Long,
    val deltaQuantity: Int,
    val baseVersion: Int
)

data class StockRebaseResponse(
    val success: Boolean,
    val itemId: Long,
    val updatedQuantity: Int,
    val updatedVersion: Int,
    val rebasedCleanly: Boolean
)

data class GstReportData(
    val period: String,
    val totalInwardTaxLiability: Double,
    val totalOutwardTaxLiability: Double,
    val inputTaxCredit: Double,
    val netGstPayable: Double,
    val gstr1Status: String,
    val gstr3bStatus: String,
    val gstr2bStatus: String
)

/**
 * High-Fidelity Mock API of Backend for DigitalChopda
 * Simulates network latency, token generation, lease replenishment, and cloud sync.
 */
class MockDigitalChopdaApiService {

    private val activeTokens = mutableSetOf<String>()
    private var sequenceCounter = 1000L

    suspend fun login(mobile: String, password: String?): Result<AuthResponse> {
        delay(300) // realistic network simulation
        val input = mobile.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter username or mobile number"))
        }
        if (password.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Please enter password"))
        }

        val accessToken = "m_acc_" + UUID.randomUUID().toString().take(16)
        val refreshToken = "m_ref_" + UUID.randomUUID().toString()
        activeTokens.add(accessToken)

        val defaultCompanies = listOf(
            Company(
                id = 1L,
                firmName = "Sharma Traders",
                ownerName = "Amit Sharma",
                gstin = "24AAACS1429B1Z8",
                phone = "+91 98765 43210",
                address = "Shop 12, Ring Road Cloth Market, Surat, Gujarat",
                stateCode = "24",
                financialYear = "26-27",
                invoicePrefix = "DC",
                isActive = true,
                upiVpa = "sharmatraders@oksbi"
            ),
            Company(
                id = 2L,
                firmName = "Sharma Electronics & Mobile",
                ownerName = "Amit Sharma",
                gstin = "24BBCDE5678C1Z4",
                phone = "+91 98765 43211",
                address = "2nd Floor, Electronic Complex, Ahmedabad, Gujarat",
                stateCode = "24",
                financialYear = "26-27",
                invoicePrefix = "SEM",
                isActive = false,
                upiVpa = "sharmaelec@icici"
            ),
            Company(
                id = 3L,
                firmName = "Gujarat Super Mart",
                ownerName = "Amit Sharma",
                gstin = "24XYZAB9876D1Z2",
                phone = "+91 98765 43212",
                address = "Near Central Bus Depot, Vadodara, Gujarat",
                stateCode = "24",
                financialYear = "26-27",
                invoicePrefix = "GSM",
                isActive = false,
                upiVpa = "gujmart@axisbank"
            )
        )

        val resolvedOwner = if (input.equals("admin", ignoreCase = true)) "Savan Admin" else if (input.all { it.isDigit() }) "Merchant" else input.replaceFirstChar { it.uppercase() }
        val resolvedUserId = if (input.equals("admin", ignoreCase = true)) "1" else "usr_merchant_${UUID.randomUUID().toString().take(6)}"

        return Result.success(
            AuthResponse(
                success = true,
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = resolvedUserId,
                ownerName = resolvedOwner,
                companies = defaultCompanies
            )
        )
    }

    suspend fun verifyOtp(mobile: String, otp: String): Result<AuthResponse> {
        delay(350)
        if (otp.length != 4 && otp.length != 6) {
            return Result.failure(IllegalArgumentException("Invalid OTP code"))
        }
        return login(mobile, "otp_login")
    }

    suspend fun requestInvoiceLease(request: LeaseRequest): Result<LeaseResponse> {
        delay(300)
        val start = sequenceCounter + 1
        val end = start + request.requestedBlockSize - 1
        sequenceCounter = end

        return Result.success(
            LeaseResponse(
                success = true,
                prefix = if (request.companyId == 2L) "SEM" else if (request.companyId == 3L) "GSM" else "DC",
                financialYear = "26-27",
                rangeStart = start,
                rangeEnd = end
            )
        )
    }

    suspend fun syncInvoiceOutbox(invoice: Invoice, idempotencyKey: String): Result<Boolean> {
        delay(450)
        // Verify idempotency
        return Result.success(true)
    }

    suspend fun syncStockDelta(delta: StockSyncDelta): Result<StockRebaseResponse> {
        delay(300)
        val updatedQty = (120 + delta.deltaQuantity).coerceAtLeast(0)
        val newVer = delta.baseVersion + 1
        return Result.success(
            StockRebaseResponse(
                success = true,
                itemId = delta.itemId,
                updatedQuantity = updatedQty,
                updatedVersion = newVer,
                rebasedCleanly = true
            )
        )
    }

    suspend fun fetchGstReport(period: String = "October 2026"): Result<GstReportData> {
        delay(250)
        return Result.success(
            GstReportData(
                period = period,
                totalInwardTaxLiability = 38200.0,
                totalOutwardTaxLiability = 38200.0,
                inputTaxCredit = 25750.0,
                netGstPayable = 12450.0,
                gstr1Status = "READY TO FILE",
                gstr3bStatus = "PENDING (Due in 9 Days)",
                gstr2bStatus = "AVAILABLE (100% Matched)"
            )
        )
    }

    suspend fun logout(token: String): Result<Boolean> {
        delay(200)
        activeTokens.remove(token)
        return Result.success(true)
    }
}
