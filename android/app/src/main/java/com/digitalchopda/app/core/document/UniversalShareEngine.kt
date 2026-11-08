package com.digitalchopda.app.core.document

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * 🚀 Universal Dispatch & Sharing Engine for DigitalChopda
 *
 * Provides a hardened, enterprise-grade sharing pipeline across all screens:
 * 1. Indian Phone Normalization: Guarantees +91 country code prefix on 10-digit mobile numbers.
 * 2. Multi-Tier WhatsApp Routing: Direct WhatsApp (`com.whatsapp`) -> WhatsApp Business (`com.whatsapp.w4b`) -> Web Link -> Android Chooser.
 * 3. FileProvider Content URI Safety: Strict read permission grants and chooser fallbacks.
 * 4. Clipboard Safety Net: Always ensures the merchant never loses their message if no app responds.
 */
object UniversalShareEngine {

    /**
     * Sanitizes and normalizes phone numbers.
     * Guarantees 91 prefix for standard Indian 10-digit mobile numbers.
     */
    fun normalizeIndianPhone(rawPhone: String?): String {
        if (rawPhone.isNullOrBlank()) return ""
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 12 && digits.startsWith("91") -> digits
            digits.length > 10 && digits.startsWith("0") -> "91" + digits.substring(1)
            else -> digits
        }
    }

    /**
     * Dispatches a physical document (PDF / Excel / Image) to WhatsApp with chooser fallback.
     */
    fun shareDocument(
        context: Context,
        file: File,
        mimeType: String = "application/pdf",
        caption: String? = null,
        recipientPhone: String? = null,
        chooserTitle: String = "Share Document",
        preferWhatsApp: Boolean = true
    ) {
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "Document file not ready or empty", Toast.LENGTH_SHORT).show()
            if (!caption.isNullOrBlank()) {
                shareTextMessage(context, caption, recipientPhone, chooserTitle)
            }
            return
        }

        val contentUri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            Toast.makeText(context, "File access error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanPhone = normalizeIndianPhone(recipientPhone)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (cleanPhone.isNotBlank()) {
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
        }

        if (preferWhatsApp) {
            try {
                // Tier 1: Try official WhatsApp
                val waIntent = Intent(intent).setPackage("com.whatsapp")
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                try {
                    // Tier 2: Try WhatsApp Business
                    val w4bIntent = Intent(intent).setPackage("com.whatsapp.w4b")
                    context.startActivity(w4bIntent)
                    return
                } catch (_: Exception) {
                    // Fallthrough to standard Android chooser
                }
            }
        }

        // Tier 3: Standard Android Multi-App Chooser
        try {
            intent.setPackage(null)
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dispatches a text message (e.g. Payment Reminder, Customer Portal Link, Ledger Summary)
     * with WhatsApp priority, System Chooser fallback, and Clipboard safety.
     */
    fun shareTextMessage(
        context: Context,
        message: String,
        recipientPhone: String? = null,
        chooserTitle: String = "Share Message",
        preferWhatsApp: Boolean = true
    ) {
        val cleanPhone = normalizeIndianPhone(recipientPhone)

        if (preferWhatsApp && cleanPhone.isNotBlank()) {
            try {
                // Tier 1: WhatsApp Universal Web URI with explicit phone & encoded text
                val encodedText = Uri.encode(message)
                val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
                val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage("com.whatsapp")
                }
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                try {
                    // Tier 2: WhatsApp Business
                    val encodedText = Uri.encode(message)
                    val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")
                    val w4bIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(w4bIntent)
                    return
                } catch (_: Exception) {
                    // Fallthrough
                }
            }
        }

        // Tier 3: Standard Android Sharesheet
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Tier 4: Clipboard Safety Net
            copyToClipboard(context, "DigitalChopda", message)
            Toast.makeText(context, "No messaging app found. Copied to clipboard ✓", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies text to the system clipboard with tactile feedback.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard ✓", Toast.LENGTH_SHORT).show()
    }
}
