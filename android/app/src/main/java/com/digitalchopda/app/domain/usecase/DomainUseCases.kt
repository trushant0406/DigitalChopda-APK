package com.digitalchopda.app.domain.usecase

import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.SequenceLease
import com.digitalchopda.app.domain.model.SyncStatus
import java.util.UUID
import kotlin.math.round

/**
 * GST Rule 46 Sequence Lease Engine
 * Strictly formats as: PREFIX/FY/00001 (e.g. DC/26-27/01001)
 */
class AllocateLeaseUseCase {

    data class AllocationResult(
        val allocatedNumber: String,
        val updatedLease: SequenceLease,
        val shouldRequestReplenishment: Boolean
    )

    fun allocateNextInvoiceNumber(currentLease: SequenceLease): AllocationResult {
        val nextSeq = currentLease.currentCursor
        val formattedNumber = currentLease.formatInvoiceNumber(nextSeq)

        val updatedLease = currentLease.copy(
            currentCursor = currentLease.currentCursor + 1
        )

        return AllocationResult(
            allocatedNumber = formattedNumber,
            updatedLease = updatedLease,
            shouldRequestReplenishment = updatedLease.needsRenewal
        )
    }
}

/**
 * Creates invoice with GST tax calculation, round-off and idempotency key
 */
class CreateInvoiceUseCase {

    data class CalculationResult(
        val subtotal: Double,
        val cgst: Double,
        val sgst: Double,
        val igst: Double,
        val totalTax: Double,
        val roundOff: Double,
        val grandTotal: Double
    )

    fun calculateTaxes(
        items: List<LineItem>,
        isInterState: Boolean = false
    ): CalculationResult {
        var subtotal = 0.0
        var totalTax = 0.0

        items.forEach { item ->
            subtotal += item.subtotal
            totalTax += item.gstAmount
        }

        val cgst: Double
        val sgst: Double
        val igst: Double

        if (isInterState) {
            cgst = 0.0
            sgst = 0.0
            igst = totalTax
        } else {
            cgst = totalTax / 2.0
            sgst = totalTax / 2.0
            igst = 0.0
        }

        val rawTotal = subtotal + totalTax
        val roundedGrandTotal = round(rawTotal)
        val roundOff = roundedGrandTotal - rawTotal

        return CalculationResult(
            subtotal = subtotal,
            cgst = cgst,
            sgst = sgst,
            igst = igst,
            totalTax = totalTax,
            roundOff = roundOff,
            grandTotal = roundedGrandTotal
        )
    }

    fun buildInvoice(
        company: Company,
        invoiceNumber: String,
        customerName: String,
        customerPhone: String,
        items: List<LineItem>,
        isInterState: Boolean = false,
        status: InvoiceStatus = InvoiceStatus.PAID,
        paymentMode: PaymentMode = PaymentMode.UPI,
        paidAmount: Double = 0.0
    ): Invoice {
        val calc = calculateTaxes(items, isInterState)
        val finalPaidAmount = when (status) {
            InvoiceStatus.PAID -> calc.grandTotal
            InvoiceStatus.PARTIAL -> paidAmount.coerceIn(0.0, calc.grandTotal)
            InvoiceStatus.UNPAID -> 0.0
        }
        val finalBalanceDue = (calc.grandTotal - finalPaidAmount).coerceAtLeast(0.0)
        return Invoice(
            companyId = company.id,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            items = items,
            subtotal = calc.subtotal,
            cgst = calc.cgst,
            sgst = calc.sgst,
            igst = calc.igst,
            roundOff = calc.roundOff,
            grandTotal = calc.grandTotal,
            paidAmount = finalPaidAmount,
            balanceDue = finalBalanceDue,
            status = status,
            paymentMode = paymentMode,
            syncStatus = SyncStatus.PENDING_SYNC,
            idempotencyKey = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
    }
}

/**
 * Optimistic Multi-Device Stock Sync with Delta Rebasing
 */
class RebaseStockUseCase {

    data class DeltaEvent(
        val itemId: Long,
        val deltaQuantity: Int, // e.g. -2 for sale
        val baseVersion: Int
    )

    data class RebaseResult(
        val newQuantity: Int,
        val newVersion: Int,
        val committedCleanly: Boolean
    )

    fun applyDelta(
        currentServerQuantity: Int,
        currentServerVersion: Int,
        delta: DeltaEvent
    ): RebaseResult {
        // If versions match, commit cleanly
        val committedCleanly = (currentServerVersion == delta.baseVersion)

        // Optimistic delta rebasing: apply delta to current server state
        val rebasedQuantity = (currentServerQuantity + delta.deltaQuantity).coerceAtLeast(0)
        val newVersion = currentServerVersion + 1

        return RebaseResult(
            newQuantity = rebasedQuantity,
            newVersion = newVersion,
            committedCleanly = committedCleanly
        )
    }
}
