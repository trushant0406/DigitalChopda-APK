package com.digitalchopda.app.presentation.billing

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.data.repository.toDomain
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.EmeraldJamaBg
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.RubyUdharBg
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WhatsAppGreen
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import com.digitalchopda.app.core.payment.UpiPaymentUtils
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.QuotationRepository

/**
 * Innovative Dual-Route WhatsApp Dispatch Dialog for Android APK
 *
 * Provides merchants with:
 * 1. ⚡ Logged-in Company WhatsApp (Background PDF attachment from server's linked session)
 * 2. 📱 Phone WhatsApp (Direct local Intent with pre-filled message & UPI link)
 */
@Composable
fun WhatsAppDispatchDialog(
    docType: String = "invoice", // "invoice", "quotation", "credit_note"
    docId: Long,
    serverOrderId: Long? = null,
    docNumber: String,
    partyName: String,
    partyPhone: String,
    amount: Double,
    upiVpa: String = "",
    firmName: String,
    apiService: DigitalChopdaApiService? = null,
    billingRepository: BillingRepository? = null,
    quotationRepository: QuotationRepository? = null,
    pdfFilePath: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phoneInput by remember { mutableStateOf(partyPhone.replace(Regex("[^0-9]"), "").takeLast(10)) }
    var selectedTemplate by remember { mutableStateOf("standard") } // standard, reminder, dispatch, brief
    var isSendingViaCompany by remember { mutableStateOf(false) }
    var companySendStatus by remember { mutableStateOf<String?>(null) }
    var companySendError by remember { mutableStateOf<String?>(null) }

    fun buildLocalMessage(): String {
        return when (selectedTemplate) {
            "reminder" -> UpiPaymentUtils.buildWhatsAppReminderMessage(
                customerName = partyName.ifBlank { "Customer" },
                firmName = firmName.ifBlank { "Our Company" },
                amount = amount,
                upiVpa = upiVpa,
                invoiceNumber = docNumber
            )
            "dispatch" -> {
                val formattedAmount = CurrencyFormatter.formatINR(amount)
                """
                |🚚 *Order Dispatched - $docNumber*
                |
                |Dear *${partyName.ifBlank { "Customer" }}*,
                |
                |Your order under $docNumber has been packed and dispatched for delivery!
                |💰 *Total:* $formattedAmount
                |
                |Thank you for doing business with *$firmName*.
                """.trimMargin()
            }
            "brief" -> {
                val formattedAmount = CurrencyFormatter.formatINR(amount)
                """
                |Hi *${partyName.ifBlank { "Customer" }}*,
                |
                |Your $docNumber for $formattedAmount is attached.
                |
                |Thank you,
                |*$firmName*
                """.trimMargin()
            }
            else -> {
                val formattedAmount = CurrencyFormatter.formatINR(amount)
                val upiBlock = if (upiVpa.isNotBlank()) "\n📲 *Pay via UPI:* upi://pay?pa=$upiVpa&pn=${UpiPaymentUtils.urlEncode(firmName)}&am=$amount\n" else ""
                """
                |📄 *$docNumber - Official Document*
                |
                |Dear *${partyName.ifBlank { "Customer" }}*,
                |
                |Thank you for choosing *$firmName*!
                |💰 *Amount:* $formattedAmount
                |$upiBlock
                |Thank you,
                |*$firmName*
                """.trimMargin()
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSendingViaCompany) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BorderStrokeDark, RoundedCornerShape(20.dp)),
            color = MidnightCardSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Send via WhatsApp",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = "$docNumber • ${partyName.ifBlank { "Customer" }}",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSendingViaCompany,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Mobile Number Field
                Text(
                    text = "RECIPIENT MOBILE NUMBER (+91)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it.take(10) },
                    placeholder = { Text("10-digit mobile number", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Text(
                            text = "🇮🇳 +91",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            ),
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DeepObsidianCanvas,
                        unfocusedContainerColor = DeepObsidianCanvas
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Template Chips
                Text(
                    text = "SELECT MESSAGE STYLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "standard" to "📄 Doc",
                        "reminder" to "🔔 Reminder",
                        "dispatch" to "🚚 Dispatch",
                        "brief" to "💬 Brief"
                    ).forEach { (key, label) ->
                        val isSelected = selectedTemplate == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) WhatsAppGreen.copy(alpha = 0.15f) else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) WhatsAppGreen else BorderStrokeDark,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTemplate = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) WhatsAppGreen else TextMuted
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feedback / Error banner
                if (companySendStatus != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldJamaBg)
                            .border(1.dp, EmeraldJama.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldJama, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = companySendStatus!!, color = EmeraldJama, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (companySendError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RubyUdharBg)
                            .border(1.dp, RubyUdhar.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RubyUdhar, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = companySendError!!, color = RubyUdhar, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Tip: Tap 'Phone WhatsApp' below to send directly from this phone.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ─── ACTION OPTION 1: LOGGED-IN COMPANY WHATSAPP WEB ───
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, WhatsAppGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .bouncyClick {
                            if (isSendingViaCompany) return@bouncyClick
                            scope.launch {
                                isSendingViaCompany = true
                                companySendStatus = "Preparing document..."
                                companySendError = null

                                var targetId = serverOrderId ?: (if (docId > 0) docId else null)

                                // If targetId is missing or local-only, attempt on-demand eager sync first
                                if (targetId == null || targetId <= 0) {
                                    companySendStatus = "Syncing with Central Depot..."
                                    val syncedId = withContext(Dispatchers.IO) {
                                        try {
                                            if (docType.equals("quotation", ignoreCase = true)) {
                                                val q = (if (docId > 0) quotationRepository?.getQuotationById(docId) else null)
                                                    ?: quotationRepository?.getQuotationByNumber(1L, docNumber)
                                                if (q != null) {
                                                    if (q.serverQuotationId != null && q.serverQuotationId > 0) {
                                                        q.serverQuotationId
                                                    } else {
                                                        quotationRepository?.syncQuotationById(q.id)?.getOrNull()?.serverQuotationId
                                                    }
                                                } else null
                                            } else {
                                                val inv = (if (docId > 0) billingRepository?.getInvoiceById(docId) else null)
                                                    ?: billingRepository?.getInvoiceByNumber(1L, docNumber)
                                                if (inv != null) {
                                                    if (inv.serverOrderId != null && inv.serverOrderId > 0) {
                                                        inv.serverOrderId
                                                    } else {
                                                        billingRepository?.syncInvoiceById(inv.id)?.getOrNull()?.serverOrderId
                                                    }
                                                } else null
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (syncedId != null && syncedId > 0) {
                                        targetId = syncedId
                                    }
                                }

                                if (targetId == null || targetId <= 0) {
                                    companySendStatus = null
                                    companySendError = "This document is only saved locally. Please connect to internet or send via Phone WhatsApp."
                                    isSendingViaCompany = false
                                    return@launch
                                }

                                if (apiService == null) {
                                    companySendStatus = null
                                    companySendError = "Server network service is currently unavailable."
                                    isSendingViaCompany = false
                                    return@launch
                                }

                                companySendStatus = "Dispatching via Company WhatsApp..."
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        when (docType) {
                                            "quotation" -> apiService.sendQuotationViaCompanyWhatsApp(targetId)
                                            "credit_note" -> apiService.sendCreditNoteViaCompanyWhatsApp(targetId)
                                            else -> apiService.sendOrderViaCompanyWhatsApp(targetId)
                                        }
                                    }
                                    if (response.isSuccessful && response.body()?.ok == true) {
                                        companySendStatus = "Dispatched via Logged-in Company WhatsApp ✓"
                                        Toast.makeText(context, "Dispatched via Company WhatsApp ✓", Toast.LENGTH_SHORT).show()
                                        kotlinx.coroutines.delay(1200)
                                        onDismiss()
                                    } else {
                                        val err = response.body()?.error
                                            ?: response.body()?.message
                                            ?: "Company WhatsApp Web is currently disconnected on server."
                                        companySendError = err
                                    }
                                } catch (e: Exception) {
                                    companySendError = e.localizedMessage ?: "Failed to reach Company WhatsApp server."
                                } finally {
                                    isSendingViaCompany = false
                                }
                            }
                        },
                    color = DeepObsidianCanvas,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSendingViaCompany) {
                                CircularProgressIndicator(color = WhatsAppGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Logged-in Company WhatsApp",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = "Sends official PDF attachment silently from server's linked session.",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ─── ACTION OPTION 2: LOCAL DEVICE WHATSAPP (WITH REAL PDF ATTACHMENT) ───
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
                        .bouncyClick {
                            scope.launch {
                                try {
                                    val msg = buildLocalMessage()
                                    val rawPhone = phoneInput.ifBlank { partyPhone }.replace(Regex("[^0-9]"), "")
                                    val cleanPhone = if (rawPhone.length == 10) "91$rawPhone" else rawPhone

                                    // 1. Locate or generate real PDF file
                                    var targetFile: File? = if (!pdfFilePath.isNullOrBlank()) File(pdfFilePath) else null
                                    if (targetFile == null || !targetFile.exists() || targetFile.length() == 0L) {
                                        val cleanDocNumber = docNumber.replace("/", "_")
                                        val candidateFile = when (docType.lowercase()) {
                                            "quotation" -> File(File(context.filesDir, "quotations"), "Quotation_$cleanDocNumber.pdf")
                                            "credit_note" -> File(File(context.filesDir, "credit_notes"), "CreditNote_$cleanDocNumber.pdf")
                                            else -> File(File(context.filesDir, "invoices"), "Invoice_$cleanDocNumber.pdf")
                                        }
                                        if (candidateFile.exists() && candidateFile.length() > 0L) {
                                            targetFile = candidateFile
                                        } else {
                                            // Generate PDF on the fly using native InvoicePdfGenerator
                                            withContext(Dispatchers.IO) {
                                                val pdfGen = InvoicePdfGenerator(context)
                                                val company = billingRepository?.getActiveCompany() ?: Company(
                                                    firmName = firmName.ifBlank { "Our Company" },
                                                    ownerName = "Proprietor",
                                                    upiVpa = upiVpa
                                                )
                                                when (docType.lowercase()) {
                                                    "quotation" -> {
                                                        val q = quotationRepository?.getQuotationById(docId)
                                                        if (q != null) targetFile = pdfGen.generateQuotationPdf(company, q)
                                                    }
                                                    "credit_note" -> {
                                                        val cn = billingRepository?.getCreditNoteById(docId)
                                                        if (cn != null) targetFile = pdfGen.generateCreditNotePdf(company, cn)
                                                    }
                                                    else -> {
                                                        val inv = billingRepository?.getInvoiceById(docId)
                                                        if (inv != null) targetFile = pdfGen.generateInvoicePdf(company, inv.toDomain())
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. Build Intent with real attached PDF via FileProvider
                                    val finalFile = targetFile
                                    if (finalFile != null && finalFile.exists() && finalFile.length() > 0L) {
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            finalFile
                                        )

                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            if (cleanPhone.isNotBlank()) {
                                                putExtra("jid", "$cleanPhone@s.whatsapp.net")
                                            }
                                        }

                                        sendIntent.setPackage("com.whatsapp")
                                        try {
                                            context.startActivity(sendIntent)
                                        } catch (e: Exception) {
                                            try {
                                                sendIntent.setPackage("com.whatsapp.w4b")
                                                context.startActivity(sendIntent)
                                            } catch (e2: Exception) {
                                                sendIntent.setPackage(null)
                                                context.startActivity(Intent.createChooser(sendIntent, "Share Document via WhatsApp"))
                                            }
                                        }
                                    } else {
                                        // Fallback to text link if PDF could not be generated
                                        UpiPaymentUtils.launchWhatsApp(
                                            context = context,
                                            phoneNumber = cleanPhone,
                                            message = msg
                                        )
                                    }

                                    // 3. Mark dispatched audit counter in SQLite
                                    if (docType.lowercase() == "invoice" && docId > 0 && billingRepository != null) {
                                        withContext(Dispatchers.IO) {
                                            billingRepository.markWhatsAppDispatched(docId)
                                        }
                                    }

                                    Toast.makeText(context, "Opening WhatsApp with PDF...", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open WhatsApp: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                    color = ElevatedSlate,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BrandPrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📱 Phone WhatsApp App",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = "Attaches official PDF document & pre-fills customer chat directly.",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                    }
                }
            }
        }
    }
}
