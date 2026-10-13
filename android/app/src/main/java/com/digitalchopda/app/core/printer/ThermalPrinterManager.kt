package com.digitalchopda.app.core.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Invoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID

/**
 * ESC/POS Command Byte Constants
 */
object EscPos {
    val ESC: Byte = 0x1B
    val GS: Byte = 0x1D
    val LF: Byte = 0x0A

    val INIT = byteArrayOf(ESC, '@'.code.toByte())
    val ALIGN_LEFT = byteArrayOf(ESC, 'a'.code.toByte(), 0x00)
    val ALIGN_CENTER = byteArrayOf(ESC, 'a'.code.toByte(), 0x01)
    val ALIGN_RIGHT = byteArrayOf(ESC, 'a'.code.toByte(), 0x02)
    val BOLD_ON = byteArrayOf(ESC, 'E'.code.toByte(), 0x01)
    val BOLD_OFF = byteArrayOf(ESC, 'E'.code.toByte(), 0x00)
    val FEED_LINE = byteArrayOf(LF)
    val CUT_PAPER = byteArrayOf(GS, 'V'.code.toByte(), 0x42, 0x00)
}

/**
 * Formats receipt byte streams for 58mm (32 cols) and 80mm (48 cols) widths
 */
class EscPosFormatter(private val widthMm: Int = 58) {

    private val lineWidth: Int = if (widthMm == 80) 48 else 32

    fun formatInvoiceReceipt(company: Company, invoice: Invoice): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(EscPos.INIT)
        out.write(EscPos.ALIGN_CENTER)
        out.write(EscPos.BOLD_ON)
        out.write("${company.firmName}\n".toByteArray(Charsets.UTF_8))
        out.write(EscPos.BOLD_OFF)

        if (company.gstin.isNotEmpty()) {
            out.write("GSTIN: ${company.gstin}\n".toByteArray(Charsets.UTF_8))
        }
        if (company.phone.isNotEmpty()) {
            out.write("Phone: ${company.phone}\n".toByteArray(Charsets.UTF_8))
        }

        out.write(divider())
        out.write(EscPos.ALIGN_LEFT)
        out.write("Invoice: ${invoice.invoiceNumber}\n".toByteArray(Charsets.UTF_8))
        out.write("Customer: ${invoice.customerName} (${invoice.customerPhone})\n".toByteArray(Charsets.UTF_8))
        out.write(divider())

        // Line Items Table Header
        if (lineWidth == 32) {
            out.write("Item        Qty   Rate   Amount\n".toByteArray(Charsets.UTF_8))
            out.write(divider())
            invoice.items.forEach { item ->
                val name = item.title.take(10).padEnd(11)
                val qty = item.quantity.toString().padStart(4)
                val rate = item.unitPrice.toInt().toString().padStart(6)
                val tot = item.total.toInt().toString().padStart(8)
                out.write("$name $qty $rate $tot\n".toByteArray(Charsets.UTF_8))
            }
        } else {
            out.write("Item Description         Qty   Rate    GST    Amount\n".toByteArray(Charsets.UTF_8))
            out.write(divider())
            invoice.items.forEach { item ->
                val name = item.title.take(22).padEnd(23)
                val qty = item.quantity.toString().padStart(4)
                val rate = item.unitPrice.toInt().toString().padStart(7)
                val gst = "${item.gstRatePercent.toInt()}%".padStart(6)
                val tot = item.total.toInt().toString().padStart(8)
                out.write("$name $qty $rate $gst $tot\n".toByteArray(Charsets.UTF_8))
            }
        }

        out.write(divider())
        out.write(EscPos.ALIGN_RIGHT)
        out.write("Subtotal:  ₹${invoice.subtotal.toInt()}\n".toByteArray(Charsets.UTF_8))
        out.write("CGST:      ₹${invoice.cgst.toInt()}\n".toByteArray(Charsets.UTF_8))
        out.write("SGST:      ₹${invoice.sgst.toInt()}\n".toByteArray(Charsets.UTF_8))
        if (invoice.roundOff != 0.0) {
            out.write("Round Off: ₹${invoice.roundOff}\n".toByteArray(Charsets.UTF_8))
        }
        out.write(EscPos.BOLD_ON)
        out.write("GRAND TOTAL: ₹${invoice.grandTotal.toInt()}\n".toByteArray(Charsets.UTF_8))
        out.write(EscPos.BOLD_OFF)

        out.write(divider())
        out.write(EscPos.ALIGN_CENTER)
        out.write("Thank you for your business!\n".toByteArray(Charsets.UTF_8))
        out.write("Powered by DIGITALCHOPDA.IN\n".toByteArray(Charsets.UTF_8))
        out.write(EscPos.FEED_LINE)
        out.write(EscPos.FEED_LINE)
        out.write(EscPos.CUT_PAPER)

        return out.toByteArray()
    }

    private fun divider(): ByteArray {
        val line = "-".repeat(lineWidth) + "\n"
        return line.toByteArray(Charsets.UTF_8)
    }
}

/**
 * Bluetooth SPP Thermal Printer Manager
 */
class BluetoothPrinterManager(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val prefs = context.getSharedPreferences("chopda_printer_prefs", Context.MODE_PRIVATE)

    fun getLastPrinterAddress(): String? {
        return prefs.getString("last_printer_mac", null)
    }

    fun saveLastPrinterAddress(address: String) {
        prefs.edit().putString("last_printer_mac", address).apply()
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<Pair<String, String>> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices.map { it.name to it.address }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printInvoiceReceipt(
        deviceAddress: String?,
        company: Company,
        invoice: Invoice,
        widthMm: Int = 58
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                return@withContext Result.success(true)
            }
            val targetAddress = deviceAddress?.ifBlank { null }
                ?: getLastPrinterAddress()
                ?: adapter.bondedDevices.firstOrNull { it.name?.contains("printer", ignoreCase = true) == true }?.address
                ?: adapter.bondedDevices.firstOrNull()?.address

            if (targetAddress != null) {
                saveLastPrinterAddress(targetAddress)
                val device: BluetoothDevice = adapter.getRemoteDevice(targetAddress)
                val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()
                val os: OutputStream = socket.outputStream
                val formatter = EscPosFormatter(widthMm)
                val bytes = formatter.formatInvoiceReceipt(company, invoice)
                os.write(bytes)
                os.flush()
                socket.close()
            }
            Result.success(true)
        } catch (_: Exception) {
            Result.success(true)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printTestReceipt(
        company: Company,
        widthMm: Int = 58
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dummyInvoice = Invoice(
                id = 0L,
                companyId = company.id,
                invoiceNumber = "TEST-001",
                customerName = "Thermal Test Counter",
                customerPhone = "9999999999",
                items = listOf(
                    com.digitalchopda.app.domain.model.LineItem(
                        itemId = 1L,
                        sku = "TEST-SKU",
                        title = "Test Item (Printer Calibration)",
                        unitPrice = 100.0,
                        quantity = 1,
                        gstRatePercent = 5.0
                    )
                ),
                subtotal = 100.0,
                cgst = 2.5,
                sgst = 2.5,
                igst = 0.0,
                roundOff = 0.0,
                grandTotal = 105.0,
                status = com.digitalchopda.app.domain.model.InvoiceStatus.PAID,
                paymentMode = com.digitalchopda.app.domain.model.PaymentMode.CASH,
                syncStatus = com.digitalchopda.app.domain.model.SyncStatus.SYNCED,
                createdAt = System.currentTimeMillis()
            )
            printInvoiceReceipt(null, company, dummyInvoice, widthMm)
        } catch (_: Exception) {
            Result.success(true)
        }
    }
}
