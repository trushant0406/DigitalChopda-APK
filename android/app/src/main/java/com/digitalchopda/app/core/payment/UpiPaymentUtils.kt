package com.digitalchopda.app.core.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.digitalchopda.app.core.localization.AppLanguage
import com.digitalchopda.app.core.localization.LanguageManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standardized NPCI UPI payment URI generator, QR raster utility, and WhatsApp reminder builder.
 */
object UpiPaymentUtils {

    fun urlEncode(s: String): String =
        try {
            java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            s
        }

    /**
     * Constructs a standard NPCI UPI payload:
     * upi://pay?pa={vpa}&pn={payee}&am={amount}&cu=INR&tn={note}
     */
    fun buildUpiUri(
        vpa: String,
        firmName: String,
        amount: Double,
        note: String
    ): String {
        val cleanVpa = vpa.trim().ifBlank { "merchant@upi" }
        val encodedFirm = urlEncode(firmName.trim())
        val formattedAmount = String.format(Locale.US, "%.2f", kotlin.math.abs(amount))
        val encodedNote = urlEncode(note.trim())
        return "upi://pay?pa=$cleanVpa&pn=$encodedFirm&am=$formattedAmount&cu=INR&tn=$encodedNote"
    }

    /**
     * Generates a 2D ZXing BitMatrix for thermal receipt printing.
     * Uses minimal margin (1) and Error Correction Level M for clear optical scanning on thermal paper.
     */
    fun generateQrBitMatrix(content: String, size: Int): BitMatrix? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds a structured, localized WhatsApp payment reminder message containing
     * customer details, outstanding balance, direct clickable upi://pay link, and UPI VPA.
     */
    fun buildWhatsAppReminderMessage(
        customerName: String,
        firmName: String,
        amount: Double,
        upiVpa: String,
        invoiceNumber: String? = null,
        language: AppLanguage = AppLanguage.GUJARATI
    ): String {
        val formattedAmount = String.format(Locale.US, "%,.2f", kotlin.math.abs(amount))
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val note = if (!invoiceNumber.isNullOrBlank()) "Bill $invoiceNumber" else "Payment to $firmName"
        val upiUri = buildUpiUri(upiVpa, firmName, amount, note)
        val cleanVpa = upiVpa.trim().ifBlank { "Not configured" }

        return when (language) {
            AppLanguage.GUJARATI -> {
                val billLine = if (!invoiceNumber.isNullOrBlank()) "🧾 બિલ નં: *$invoiceNumber*\n" else ""
                """
                |🙏 *નમસ્તે $customerName*,
                |
                |*$firmName* તરફથી ચુકવણીનું નમ્ર રીમાઇન્ડર:
                |${billLine}📌 બાકી રકમ: *₹$formattedAmount*
                |📅 તારીખ: $dateStr
                |
                |📲 *સીધું UPI થી ચૂકવવા નીચેની લિંક પર ક્લિક કરો:*
                |$upiUri
                |
                |💳 *UPI ID:* `$cleanVpa`
                |🏦 Google Pay, PhonePe, Paytm, BHIM દ્વારા સ્વીકાર્ય.
                |
                |આભાર,
                |*$firmName*
                |_DigitalChopda Sovereign Ledger_
                """.trimMargin()
            }
            AppLanguage.HINDI -> {
                val billLine = if (!invoiceNumber.isNullOrBlank()) "🧾 बिल संख्या: *$invoiceNumber*\n" else ""
                """
                |🙏 *नमस्ते $customerName*,
                |
                |*$firmName* की ओर से भुगतान रिमाइंडर:
                |${billLine}📌 बकाया राशि: *₹$formattedAmount*
                |📅 दिनांक: $dateStr
                |
                |📲 *सीधे UPI द्वारा भुगतान करने के लिए इस लिंक पर क्लिक करें:*
                |$upiUri
                |
                |💳 *UPI ID:* `$cleanVpa`
                |🏦 Google Pay, PhonePe, Paytm, BHIM सभी UPI ऐप स्वीकार्य हैं।
                |
                |धन्यवाद,
                |*$firmName*
                |_DigitalChopda Sovereign Ledger_
                """.trimMargin()
            }
            AppLanguage.ENGLISH -> {
                val billLine = if (!invoiceNumber.isNullOrBlank()) "🧾 Invoice: *$invoiceNumber*\n" else ""
                """
                |🙏 *Hello $customerName*,
                |
                |Payment reminder from *$firmName*:
                |${billLine}📌 Outstanding Balance: *₹$formattedAmount*
                |📅 Date: $dateStr
                |
                |📲 *Tap to Pay directly via UPI:*
                |$upiUri
                |
                |💳 *UPI ID:* `$cleanVpa`
                |🏦 Accepted on Google Pay, PhonePe, Paytm, BHIM & all UPI apps.
                |
                |Thank you,
                |*$firmName*
                |_Powered by DigitalChopda_
                """.trimMargin()
            }
        }
    }

    /**
     * Safely dispatches the reminder message to WhatsApp.
     * Falls back to copying the message to the clipboard if WhatsApp is not installed.
     */
    fun launchWhatsApp(
        context: Context,
        phoneNumber: String,
        message: String
    ) {
        val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
        val encodedMessage = urlEncode(message)
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("DigitalChopda Payment Reminder", message))
            Toast.makeText(context, "WhatsApp not installed. Reminder copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }
}
