package com.digitalchopda.app.core.document

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.digitalchopda.app.data.local.entity.CreditNoteEntity
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Invoice
import com.digitalchopda.app.domain.model.LineItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-Performance Native Android PdfDocument Generator for GST Tax Invoices
 */
class InvoicePdfGenerator(val context: Context) {

    fun generateInvoicePdf(company: Company, invoice: Invoice): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Accent Bar
        paint.color = Color.parseColor("#3F5EFB") // Brand Primary Blue
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
        paint.color = Color.parseColor("#2546D8") // Brand Primary Blue Deep
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

        paint.color = Color.parseColor("#3F5EFB") // Brand Primary Blue highlight
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("GRAND TOTAL:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(invoice.grandTotal), 480f, y, paint)

        // Footer
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("This is an authorized computer-generated tax invoice verified via DIGITALCHOPDA.IN", 36f, 800f, paint)

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

    fun generateQuotationPdf(company: Company, quotation: QuotationEntity, items: List<LineItem> = emptyList()): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Accent Bar (Imperial Gold)
        paint.color = Color.parseColor("#EAB308")
        canvas.drawRect(0f, 0f, 595f, 12f, paint)

        // Company Firm Name & Address
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(company.firmName, 36f, 48f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#64748B")
        canvas.drawText(company.address, 36f, 64f, paint)
        canvas.drawText("GSTIN: ${company.gstin.ifEmpty { "URP" }} | Phone: ${company.phone}", 36f, 78f, paint)

        // Quotation Header (Right aligned)
        paint.color = Color.parseColor("#CA8A04")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("QUOTATION", 440f, 48f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Quote No: ${quotation.quotationNumber}", 420f, 64f, paint)
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(quotation.createdAt))
        canvas.drawText("Date: $dateStr", 420f, 78f, paint)
        val validDateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(quotation.validUntil))
        canvas.drawText("Valid Until: $validDateStr", 420f, 92f, paint)

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 104f, 559f, 104f, paint)

        // Customer Details
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ESTIMATE FOR:", 36f, 124f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("Customer Name: ${quotation.customerName}", 36f, 140f, paint)
        canvas.drawText("Mobile: ${quotation.customerPhone}", 36f, 154f, paint)
        canvas.drawText("Status: ${quotation.status}", 36f, 168f, paint)

        // Line Items Table Header
        paint.color = Color.parseColor("#FEF08A") // Soft Gold
        canvas.drawRect(36f, 185f, 559f, 205f, paint)

        paint.color = Color.parseColor("#713F12")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 44f, 199f, paint)
        canvas.drawText("Item / Particulars", 72f, 199f, paint)
        canvas.drawText("Qty", 290f, 199f, paint)
        canvas.drawText("Rate (₹)", 340f, 199f, paint)
        canvas.drawText("GST %", 410f, 199f, paint)
        canvas.drawText("Total (₹)", 480f, 199f, paint)

        var y = 225f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#0F172A")

        if (items.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                canvas.drawText((index + 1).toString(), 44f, y, paint)
                canvas.drawText(item.title.take(30), 72f, y, paint)
                canvas.drawText(item.quantity.toString(), 290f, y, paint)
                canvas.drawText("%.2f".format(item.unitPrice), 340f, y, paint)
                canvas.drawText("${item.gstRatePercent.toInt()}%", 410f, y, paint)
                canvas.drawText("%.2f".format(item.total), 480f, y, paint)
                y += 20f
            }
        } else {
            // Summary row fallback
            canvas.drawText("1", 44f, y, paint)
            canvas.drawText(quotation.itemsSummary.ifBlank { "Quotation Estimate Items" }.take(35), 72f, y, paint)
            canvas.drawText("1", 290f, y, paint)
            canvas.drawText("%.2f".format(quotation.subtotal), 340f, y, paint)
            canvas.drawText("5%", 410f, y, paint)
            canvas.drawText("%.2f".format(quotation.grandTotal), 480f, y, paint)
            y += 20f
        }

        y += 10f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(36f, y, 559f, y, paint)
        y += 20f

        paint.color = Color.parseColor("#334155")
        canvas.drawText("Taxable Subtotal:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(quotation.subtotal), 480f, y, paint)
        y += 18f

        canvas.drawText("Estimated GST Tax:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(quotation.taxAmount), 480f, y, paint)
        y += 18f

        paint.color = Color.parseColor("#CA8A04")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("ESTIMATED TOTAL:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(quotation.grandTotal), 480f, y, paint)

        // Footer
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("This is an official estimate. Prices are subject to confirmation at order booking.", 36f, 800f, paint)

        document.finishPage(page)

        val quotesDir = File(context.filesDir, "quotations").apply { mkdirs() }
        val cleanNumber = quotation.quotationNumber.replace("/", "_")
        val pdfFile = File(quotesDir, "Quotation_$cleanNumber.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return pdfFile
    }

    fun generateCreditNotePdf(company: Company, creditNote: CreditNoteEntity, items: List<LineItem> = emptyList()): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Accent Bar (Ruby Udhar)
        paint.color = Color.parseColor("#EF4444")
        canvas.drawRect(0f, 0f, 595f, 12f, paint)

        // Company Firm Name & Address
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(company.firmName, 36f, 48f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#64748B")
        canvas.drawText(company.address, 36f, 64f, paint)
        canvas.drawText("GSTIN: ${company.gstin.ifEmpty { "URP" }} | Phone: ${company.phone}", 36f, 78f, paint)

        // Credit Note Header (Right aligned)
        paint.color = Color.parseColor("#DC2626")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CREDIT NOTE", 440f, 48f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("CN No: ${creditNote.creditNoteNumber}", 420f, 64f, paint)
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(creditNote.createdAt))
        canvas.drawText("Date: $dateStr", 420f, 78f, paint)
        if (creditNote.originalInvoiceNumber.isNotBlank()) {
            canvas.drawText("Original Bill: ${creditNote.originalInvoiceNumber}", 420f, 92f, paint)
        }

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 104f, 559f, 104f, paint)

        // Customer & Return Reason Details
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CREDITED TO PARTY:", 36f, 124f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("Party Name: ${creditNote.customerName}", 36f, 140f, paint)
        canvas.drawText("Mobile: ${creditNote.customerPhone}", 36f, 154f, paint)
        canvas.drawText("Return Reason: ${creditNote.reason}", 36f, 168f, paint)
        if (creditNote.notes.isNotBlank()) {
            canvas.drawText("Remarks: ${creditNote.notes}", 36f, 182f, paint)
        }

        // Line Items Table Header
        val tableTop = if (creditNote.notes.isNotBlank()) 198f else 185f
        paint.color = Color.parseColor("#FEE2E2") // Soft Red
        canvas.drawRect(36f, tableTop, 559f, tableTop + 20f, paint)

        paint.color = Color.parseColor("#991B1B")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 44f, tableTop + 14f, paint)
        canvas.drawText("Returned Goods / Items", 72f, tableTop + 14f, paint)
        canvas.drawText("Qty", 290f, tableTop + 14f, paint)
        canvas.drawText("Rate (₹)", 340f, tableTop + 14f, paint)
        canvas.drawText("GST %", 410f, tableTop + 14f, paint)
        canvas.drawText("Credit (₹)", 480f, tableTop + 14f, paint)

        var y = tableTop + 38f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#0F172A")

        if (items.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                canvas.drawText((index + 1).toString(), 44f, y, paint)
                canvas.drawText(item.title.take(30), 72f, y, paint)
                canvas.drawText(item.quantity.toString(), 290f, y, paint)
                canvas.drawText("%.2f".format(item.unitPrice), 340f, y, paint)
                canvas.drawText("${item.gstRatePercent.toInt()}%", 410f, y, paint)
                canvas.drawText("%.2f".format(item.total), 480f, y, paint)
                y += 20f
            }
        } else {
            canvas.drawText("1", 44f, y, paint)
            canvas.drawText(creditNote.itemsSummary.ifBlank { "Returned Merchandise" }.take(35), 72f, y, paint)
            canvas.drawText("1", 290f, y, paint)
            canvas.drawText("%.2f".format(creditNote.subtotal), 340f, y, paint)
            canvas.drawText("5%", 410f, y, paint)
            canvas.drawText("%.2f".format(creditNote.totalAmount), 480f, y, paint)
            y += 20f
        }

        y += 10f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(36f, y, 559f, y, paint)
        y += 20f

        paint.color = Color.parseColor("#334155")
        canvas.drawText("Taxable Return Value:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(creditNote.subtotal), 480f, y, paint)
        y += 18f

        canvas.drawText("Reversed GST Tax:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(creditNote.taxAmount), 480f, y, paint)
        y += 18f

        paint.color = Color.parseColor("#DC2626")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("TOTAL REFUND CREDIT:", 350f, y, paint)
        canvas.drawText("₹ %.2f".format(creditNote.totalAmount), 480f, y, paint)

        // Footer
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8.5f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("This credit note amount is credited directly to the customer ledger account.", 36f, 800f, paint)

        document.finishPage(page)

        val creditNotesDir = File(context.filesDir, "credit_notes").apply { mkdirs() }
        val cleanNumber = creditNote.creditNoteNumber.replace("/", "_")
        val pdfFile = File(creditNotesDir, "CreditNote_$cleanNumber.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return pdfFile
    }

    fun generateShippingLabelPdf(company: Company, invoice: Invoice): File {
        val document = PdfDocument()
        // Standard 4" x 6" label (288 x 432 pt)
        val pageInfo = PdfDocument.PageInfo.Builder(288, 432, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Outer border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(8f, 8f, 280f, 424f, paint)
        paint.style = Paint.Style.FILL

        // 1. TOP HEADER: FROM (Merchant) & TO (Customer)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIP FROM: ${company.firmName.take(24)}", 14f, 26f, paint)

        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#475569")
        canvas.drawText(company.address.take(45), 14f, 38f, paint)
        canvas.drawText("Phone: ${company.phone} | GST: ${company.gstin.ifEmpty { "URP" }}", 14f, 49f, paint)

        // Horizontal separator
        paint.color = Color.parseColor("#0F172A")
        paint.strokeWidth = 1f
        canvas.drawLine(8f, 56f, 280f, 56f, paint)

        // 2. SHIP TO PARTY DETAILS (Prominent)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SHIP TO / BUYER DETAILS", 14f, 70f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.customerName.take(24), 14f, 88f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Phone: ${invoice.customerPhone}", 14f, 102f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#334155")
        val addr = "Standard Delivery / Shipping Address"
        canvas.drawText(addr, 14f, 115f, paint)

        // Destination State & City Badge (Right side of Ship-To)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(190f, 64f, 274f, 120f, paint)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DESTINATION", 198f, 76f, paint)
        paint.textSize = 12f
        canvas.drawText(company.stateCode.ifEmpty { "IN" }, 198f, 96f, paint)
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("STATE", 198f, 110f, paint)

        // Horizontal separator
        paint.color = Color.parseColor("#0F172A")
        paint.strokeWidth = 1f
        canvas.drawLine(8f, 126f, 280f, 126f, paint)

        // 3. PACKAGE / INVOICE METRICS
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PACKAGE CONTENTS & INVOICE REF", 14f, 140f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Invoice: ${invoice.invoiceNumber}", 14f, 154f, paint)
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(invoice.createdAt))
        canvas.drawText("Date: $dateStr", 160f, 154f, paint)

        // Items list (up to 3 items)
        var itemY = 172f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText("ITEM DESCRIPTION", 14f, itemY, paint)
        canvas.drawText("QTY", 230f, itemY, paint)
        itemY += 6f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(14f, itemY, 274f, itemY, paint)
        itemY += 12f

        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8f
        val displayItems = invoice.items.take(3)
        if (displayItems.isNotEmpty()) {
            displayItems.forEach { item ->
                canvas.drawText(item.title.take(30), 14f, itemY, paint)
                canvas.drawText("${item.quantity.toInt()} PCS", 230f, itemY, paint)
                itemY += 14f
            }
        } else {
            canvas.drawText("General Merchandise / Order Items", 14f, itemY, paint)
            canvas.drawText("1 PKG", 230f, itemY, paint)
            itemY += 14f
        }

        // Horizontal separator
        paint.color = Color.parseColor("#0F172A")
        paint.strokeWidth = 1f
        canvas.drawLine(8f, 240f, 280f, 240f, paint)

        // 4. TRANSPORT / LOGISTICS DETAILS (Dotted blanks for dispatch crew)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DISPATCH & CARRIER DETAILS", 14f, 255f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#334155")
        paint.textSize = 8f
        canvas.drawText("Transport / Courier: ____________________________", 14f, 272f, paint)
        canvas.drawText("Tracking / LR No: ____________________________", 14f, 290f, paint)
        canvas.drawText("No. of Boxes / Bags: ______   Weight: _______ kg", 14f, 308f, paint)

        // Horizontal separator
        paint.color = Color.parseColor("#0F172A")
        paint.strokeWidth = 1f
        canvas.drawLine(8f, 322f, 280f, 322f, paint)

        // 5. PAYMENT STATUS BADGE (Large & Clear for delivery agents)
        val isPaid = invoice.status.name.equals("PAID", ignoreCase = true)
        if (isPaid) {
            paint.color = Color.parseColor("#DCFCE7") // Light green
            canvas.drawRect(14f, 332f, 274f, 372f, paint)
            paint.color = Color.parseColor("#166534") // Dark green
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("✔ PREPAID - DO NOT COLLECT CASH", 22f, 356f, paint)
        } else {
            paint.color = Color.parseColor("#FEE2E2") // Light red
            canvas.drawRect(14f, 332f, 274f, 372f, paint)
            paint.color = Color.parseColor("#991B1B") // Dark red
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("⚠ C.O.D. PENDING: ₹ %.2f".format(invoice.grandTotal), 22f, 356f, paint)
        }

        // Tracking Barcode visual representation
        paint.color = Color.parseColor("#0F172A")
        for (i in 0 until 40) {
            val barX = 24f + (i * 6f)
            val barW = if (i % 3 == 0) 3.5f else 1.8f
            canvas.drawRect(barX, 384f, barX + barW, 404f, paint)
        }
        paint.textSize = 7f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("*${invoice.invoiceNumber.replace("-", "")}*", 100f, 416f, paint)

        document.finishPage(page)

        val labelsDir = File(context.filesDir, "shipping_labels").apply { mkdirs() }
        val cleanNumber = invoice.invoiceNumber.replace("/", "_")
        val pdfFile = File(labelsDir, "ShippingLabel_$cleanNumber.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return pdfFile
    }

    fun generatePaymentReceiptPdf(
        company: Company,
        receiptNumber: String,
        customerName: String,
        customerPhone: String,
        amount: Double,
        paymentMode: String,
        referenceNo: String = "",
        notes: String = "",
        createdAt: Long = System.currentTimeMillis()
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Accent Bar (Emerald Jama)
        paint.color = Color.parseColor("#10B981")
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
        canvas.drawText("GSTIN: ${company.gstin.ifEmpty { "URP" }} | Phone: ${company.phone}", 36f, 78f, paint)

        // Receipt Header
        paint.color = Color.parseColor("#059669")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PAYMENT RECEIPT", 400f, 48f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Receipt No: $receiptNumber", 400f, 64f, paint)
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(createdAt))
        canvas.drawText("Date: $dateStr", 400f, 78f, paint)

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 94f, 559f, 94f, paint)

        // Received From
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECEIVED WITH THANKS FROM:", 36f, 114f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("Party Name: $customerName", 36f, 130f, paint)
        canvas.drawText("Phone: $customerPhone", 36f, 144f, paint)

        // Amount Box
        paint.color = Color.parseColor("#ECFDF5")
        canvas.drawRect(36f, 170f, 559f, 230f, paint)
        paint.color = Color.parseColor("#065F46")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AMOUNT RECEIVED: ₹ %.2f".format(amount), 50f, 204f, paint)

        // Payment details
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Payment Mode: $paymentMode", 36f, 260f, paint)
        if (referenceNo.isNotBlank()) {
            canvas.drawText("Transaction Ref / Cheque No: $referenceNo", 36f, 280f, paint)
        }
        if (notes.isNotBlank()) {
            canvas.drawText("Notes: $notes", 36f, 300f, paint)
        }

        // Signature
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Authorized Signatory for ${company.firmName}", 380f, 360f, paint)

        document.finishPage(page)

        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val cleanNumber = receiptNumber.replace("/", "_")
        val pdfFile = File(receiptsDir, "Receipt_$cleanNumber.pdf")

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

