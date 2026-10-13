package com.digitalchopda.app.core.document

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline Native Android PdfDocument Generator for GST Tax Invoices
 */
class InvoicePdfGenerator(private val context: Context) {

    fun generateInvoicePdf(company: Company, invoice: Invoice): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Accent Bar
        paint.color = Color.parseColor("#FF5E3A") // Coral
        canvas.drawRect(0f, 0f, 595f, 12f, paint)

        // Company Firm Name & Tagline
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(company.firmName, 36f, 48f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#64748B")
        canvas.drawText(company.address, 36f, 64f, paint)
        canvas.drawText("GSTIN: ${company.gstin.ifEmpty { "URP" }} | State Code: ${company.stateCode} | Phone: ${company.phone}", 36f, 78f, paint)

        // Tax Invoice Header (Right aligned)
        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TAX INVOICE", 440f, 48f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", 420f, 64f, paint)
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(invoice.createdAt))
        canvas.drawText("Date: $dateStr", 420f, 78f, paint)

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 94f, 559f, 94f, paint)

        // Bill To (Customer Details)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO / BUYER:", 36f, 114f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("Name: ${invoice.customerName}", 36f, 130f, paint)
        canvas.drawText("Phone: ${invoice.customerPhone}", 36f, 144f, paint)
        canvas.drawText("Payment Status: ${invoice.status.name} | Mode: ${invoice.paymentMode.name}", 36f, 158f, paint)

        // Line Items Table Header
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(36f, 175f, 559f, 195f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 44f, 189f, paint)
        canvas.drawText("Item / Description", 72f, 189f, paint)
        canvas.drawText("Qty", 290f, 189f, paint)
        canvas.drawText("Rate (₹)", 340f, 189f, paint)
        canvas.drawText("GST %", 410f, 189f, paint)
        canvas.drawText("Total (₹)", 480f, 189f, paint)

        // Items Rows
        var y = 215f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#0F172A")

        invoice.items.forEachIndexed { index, item ->
            canvas.drawText((index + 1).toString(), 44f, y, paint)
            canvas.drawText(item.title.take(30), 72f, y, paint)
            canvas.drawText(item.quantity.toString(), 290f, y, paint)
            canvas.drawText("%.2f".format(item.unitPrice), 340f, y, paint)
            canvas.drawText("${item.gstRatePercent.toInt()}%", 410f, y, paint)
            canvas.drawText("%.2f".format(item.total), 480f, y, paint)
            y += 20f
        }

        // Divider
        y += 10f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(36f, y, 559f, y, paint)
        y += 20f

        // Tax & Totals Breakdown (Right side)
        paint.color = Color.parseColor("#334155")
        canvas.drawText("Taxable Subtotal:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(invoice.subtotal), 480f, y, paint)
        y += 18f

        canvas.drawText("CGST:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(invoice.cgst), 480f, y, paint)
        y += 18f

        canvas.drawText("SGST:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(invoice.sgst), 480f, y, paint)
        y += 18f

        if (invoice.roundOff != 0.0) {
            canvas.drawText("Round Off:", 350f, y, paint)
            canvas.drawText("₹ %.2f".format(invoice.roundOff), 480f, y, paint)
            y += 18f
        }

        paint.color = Color.parseColor("#FF5E3A") // Coral highlight
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("GRAND TOTAL:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(invoice.grandTotal), 480f, y, paint)

        // Footer
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("This is a computer-generated tax invoice created offline via DIGITALCHOPDA.IN", 36f, 800f, paint)

        document.finishPage(page)

        // Write to file
        val invoicesDir = File(context.filesDir, "invoices").apply { mkdirs() }
        val cleanNumber = invoice.invoiceNumber.replace("/", "_")
        val pdfFile = File(invoicesDir, "Invoice_$cleanNumber.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    fun getFileProviderUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}
