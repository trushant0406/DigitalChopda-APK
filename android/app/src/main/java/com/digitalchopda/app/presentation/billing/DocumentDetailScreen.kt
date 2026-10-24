package com.digitalchopda.app.presentation.billing

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaPdfLoadingOverlay
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.EmeraldJamaBg
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.RubyUdharBg
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.WhatsAppGreen
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.designsystem.getAvatarColors
import androidx.compose.foundation.clickable
import com.digitalchopda.app.core.document.DocumentItemParser
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.data.local.entity.CreditNoteEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CreditNoteRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.QuotationRepository
import com.digitalchopda.app.presentation.party.RecordPaymentDialog
import com.digitalchopda.app.data.repository.toDomain
import androidx.compose.material.icons.filled.ArrowDownward
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.util.CurrencyFormatter
import com.digitalchopda.app.util.formatINR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DocumentModel {
    data class InvoiceDoc(val entity: InvoiceEntity) : DocumentModel()
    data class QuotationDoc(val entity: QuotationEntity) : DocumentModel()
    data class CreditNoteDoc(val entity: CreditNoteEntity) : DocumentModel()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    docType: String, // "invoice", "quotation", "credit_note"
    docId: Long,
    companyRepository: CompanyRepository,
    billingRepository: BillingRepository,
    quotationRepository: QuotationRepository,
    creditNoteRepository: CreditNoteRepository,
    pdfGenerator: InvoicePdfGenerator,
    customerRepository: CustomerRepository? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (docType: String, docId: Long) -> Unit,
    onNavigateToPdfViewer: (filePath: String, invoiceId: Long, customerName: String, customerPhone: String, invoiceNumber: String, docType: String, serverOrderId: Long) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeCompany by remember {
        mutableStateOf(
            Company(
                id = 1L,
                firmName = "Sharma Traders",
                ownerName = "Amit Sharma",
                gstin = "24AAACS1429B1Z8",
                phone = "+91 98765 43210",
                address = "Surat, Gujarat",
                isActive = true
            )
        )
    }

    var document by remember { mutableStateOf<DocumentModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showReceivePaymentDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var isUpdatingFulfillment by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showConvertConfirmDialog by remember { mutableStateOf(false) }
    var showRevisionHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(docType, docId) {
        activeCompany = companyRepository.getActiveCompany()
        withContext(Dispatchers.IO) {
            when (docType.lowercase()) {
                "quotation" -> {
                    val q = quotationRepository.getQuotationById(docId)
                    if (q != null) document = DocumentModel.QuotationDoc(q)
                }
                "credit_note", "sales_return" -> {
                    val cn = creditNoteRepository.getCreditNoteById(docId)
                    if (cn != null) document = DocumentModel.CreditNoteDoc(cn)
                }
                else -> {
                    val inv = billingRepository.getInvoiceById(docId)
                    if (inv != null) document = DocumentModel.InvoiceDoc(inv)
                }
            }
        }
        isLoading = false
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    fun openPdf() {
        val doc = document ?: return
        isGeneratingPdf = true
        scope.launch {
            try {
                val pdfFile: File = withContext(Dispatchers.IO) {
                    when (doc) {
                        is DocumentModel.InvoiceDoc -> {
                            val inv = doc.entity
                            val downloaded = if (inv.serverOrderId != null) {
                                billingRepository.downloadBackendInvoicePdf(
                                    context = context,
                                    backendOrderId = inv.serverOrderId,
                                    filename = "Invoice_${inv.invoiceNumber.replace('/', '_')}.pdf"
                                )
                            } else null
                            downloaded ?: pdfGenerator.generateInvoicePdf(
                                company = activeCompany,
                                invoice = com.digitalchopda.app.domain.model.Invoice(
                                    id = inv.id,
                                    companyId = inv.companyId,
                                    invoiceNumber = inv.invoiceNumber,
                                    customerName = inv.customerName,
                                    customerPhone = inv.customerPhone,
                                    subtotal = inv.subtotal,
                                    cgst = inv.cgst,
                                    sgst = inv.sgst,
                                    igst = inv.igst,
                                    roundOff = inv.roundOff,
                                    grandTotal = inv.grandTotal,
                                    createdAt = inv.createdAt
                                )
                            )
                        }
                        is DocumentModel.QuotationDoc -> {
                            val q = doc.entity
                            val downloaded = billingRepository.downloadBackendQuotationPdf(
                                context = context,
                                quotationId = q.id,
                                filename = "Quotation_${q.quotationNumber.replace('/', '_')}.pdf"
                            )
                            downloaded ?: pdfGenerator.generateQuotationPdf(activeCompany, q)
                        }
                        is DocumentModel.CreditNoteDoc -> {
                            val cn = doc.entity
                            val downloaded = creditNoteRepository.downloadBackendCreditNotePdf(
                                context = context,
                                creditNoteId = cn.id,
                                filename = "CreditNote_${cn.creditNoteNumber.replace('/', '_')}.pdf"
                            )
                            downloaded ?: pdfGenerator.generateCreditNotePdf(activeCompany, cn)
                        }
                    }
                }

                isGeneratingPdf = false
                val name = when (doc) {
                    is DocumentModel.InvoiceDoc -> doc.entity.customerName
                    is DocumentModel.QuotationDoc -> doc.entity.customerName
                    is DocumentModel.CreditNoteDoc -> doc.entity.customerName
                }
                val phone = when (doc) {
                    is DocumentModel.InvoiceDoc -> doc.entity.customerPhone
                    is DocumentModel.QuotationDoc -> doc.entity.customerPhone
                    is DocumentModel.CreditNoteDoc -> doc.entity.customerPhone
                }
                val number = when (doc) {
                    is DocumentModel.InvoiceDoc -> doc.entity.invoiceNumber
                    is DocumentModel.QuotationDoc -> doc.entity.quotationNumber
                    is DocumentModel.CreditNoteDoc -> doc.entity.creditNoteNumber
                }
                val id = when (doc) {
                    is DocumentModel.InvoiceDoc -> doc.entity.id
                    is DocumentModel.QuotationDoc -> doc.entity.id
                    is DocumentModel.CreditNoteDoc -> doc.entity.id
                }
                val serverId = when (doc) {
                    is DocumentModel.InvoiceDoc -> doc.entity.serverOrderId ?: 0L
                    is DocumentModel.QuotationDoc -> doc.entity.serverQuotationId ?: 0L
                    is DocumentModel.CreditNoteDoc -> doc.entity.serverCreditNoteId ?: 0L
                }
                val dt = when (doc) {
                    is DocumentModel.InvoiceDoc -> "invoice"
                    is DocumentModel.QuotationDoc -> "quotation"
                    is DocumentModel.CreditNoteDoc -> "credit_note"
                }

                onNavigateToPdfViewer(
                    pdfFile.absolutePath,
                    id,
                    name,
                    phone,
                    number,
                    dt,
                    serverId
                )
            } catch (e: Exception) {
                isGeneratingPdf = false
                Toast.makeText(context, "Failed to render PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openShippingLabelPdf() {
        val doc = document ?: return
        isGeneratingPdf = true
        scope.launch {
            try {
                val pdfFile: File? = withContext(Dispatchers.IO) {
                    when (doc) {
                        is DocumentModel.InvoiceDoc -> {
                            val inv = doc.entity
                            val orderId = inv.serverOrderId
                            val downloaded = if (orderId != null && orderId > 0L) {
                                billingRepository.downloadBackendShippingLabelPdf(
                                    context = context,
                                    backendOrderId = orderId,
                                    filename = "ShippingLabel_${inv.invoiceNumber.replace('/', '_')}.pdf"
                                )
                            } else null
                            downloaded ?: pdfGenerator.generateShippingLabelPdf(activeCompany, inv.toDomain())
                        }
                        is DocumentModel.QuotationDoc -> {
                            val q = doc.entity
                            billingRepository.downloadQuotationShippingLabelPdf(
                                context = context,
                                quotationId = q.id,
                                filename = "ShippingLabel_Quotation_${q.quotationNumber.replace('/', '_')}.pdf"
                            )
                        }
                        is DocumentModel.CreditNoteDoc -> null
                    }
                }

                isGeneratingPdf = false
                if (pdfFile != null && pdfFile.exists()) {
                    val name = when (doc) {
                        is DocumentModel.InvoiceDoc -> doc.entity.customerName
                        is DocumentModel.QuotationDoc -> doc.entity.customerName
                        is DocumentModel.CreditNoteDoc -> doc.entity.customerName
                    }
                    val phone = when (doc) {
                        is DocumentModel.InvoiceDoc -> doc.entity.customerPhone
                        is DocumentModel.QuotationDoc -> doc.entity.customerPhone
                        is DocumentModel.CreditNoteDoc -> doc.entity.customerPhone
                    }
                    val number = when (doc) {
                        is DocumentModel.InvoiceDoc -> doc.entity.invoiceNumber
                        is DocumentModel.QuotationDoc -> doc.entity.quotationNumber
                        is DocumentModel.CreditNoteDoc -> doc.entity.creditNoteNumber
                    }
                    val id = when (doc) {
                        is DocumentModel.InvoiceDoc -> doc.entity.id
                        is DocumentModel.QuotationDoc -> doc.entity.id
                        is DocumentModel.CreditNoteDoc -> doc.entity.id
                    }
                    val serverId = when (doc) {
                        is DocumentModel.InvoiceDoc -> doc.entity.serverOrderId ?: 0L
                        is DocumentModel.QuotationDoc -> doc.entity.serverQuotationId ?: 0L
                        is DocumentModel.CreditNoteDoc -> doc.entity.serverCreditNoteId ?: 0L
                    }

                    onNavigateToPdfViewer(
                        pdfFile.absolutePath,
                        id,
                        name,
                        phone,
                        "$number-LABEL",
                        "shipping_label",
                        serverId
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Shipping label is generated from cloud order. Please sync invoice first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                isGeneratingPdf = false
                Toast.makeText(context, "Failed to download shipping label: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteCurrentDocument() {
        val doc = document ?: return
        scope.launch {
            try {
                val success: Boolean = withContext(Dispatchers.IO) {
                    when (doc) {
                        is DocumentModel.InvoiceDoc -> {
                            billingRepository.deleteInvoice(doc.entity.id).isSuccess
                        }
                        is DocumentModel.QuotationDoc -> {
                            quotationRepository.deleteQuotation(doc.entity.id).isSuccess
                        }
                        is DocumentModel.CreditNoteDoc -> {
                            creditNoteRepository.voidCreditNote(doc.entity.id)
                            true
                        }
                    }
                }
                if (success) {
                    Toast.makeText(context, "Document deleted successfully", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Could not delete document", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun convertQuotationToInvoice() {
        val doc = document as? DocumentModel.QuotationDoc ?: return
        scope.launch {
            try {
                val res = quotationRepository.convertQuotationToInvoice(doc.entity.id, activeCompany.id)
                if (res.isSuccess) {
                    val inv = res.getOrNull()
                    Toast.makeText(context, "⚡ Converted to Invoice #${inv?.invoiceNumber ?: ""}!", Toast.LENGTH_LONG).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Failed to convert: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error converting: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.then(if (isGeneratingPdf) Modifier.blur(14.dp) else Modifier),
            containerColor = DeepObsidianCanvas,
            topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val doc = document) {
                        is DocumentModel.InvoiceDoc -> "Invoice ${doc.entity.invoiceNumber}"
                        is DocumentModel.QuotationDoc -> "Quotation ${doc.entity.quotationNumber}"
                        is DocumentModel.CreditNoteDoc -> "Credit Note ${doc.entity.creditNoteNumber}"
                        null -> "Document Details"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkNavy
                        )
                    }
                },
                actions = {
                    if (document is DocumentModel.InvoiceDoc) {
                        IconButton(onClick = { showRevisionHistoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Revision History",
                                tint = BrandPrimaryBlue
                            )
                        }
                    }
                    if (document is DocumentModel.InvoiceDoc || document is DocumentModel.QuotationDoc) {
                        IconButton(onClick = { openShippingLabelPdf() }) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Shipping Label",
                                tint = BrandPrimaryBlue
                            )
                        }
                    }
                    IconButton(onClick = { openPdf() }) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "View PDF",
                            tint = BrandPrimaryBlue
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Document",
                            tint = DangerRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
        },
        bottomBar = {
            // FIXED BOTTOM ACTION BAR: [EDIT] & [SEE AS PDF]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp),
                color = MidnightCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 0: Receive Payment Button for Unpaid/Partial Invoices
                    val isUnpaidInvoice = document is DocumentModel.InvoiceDoc &&
                            !(document as DocumentModel.InvoiceDoc).entity.status.equals("PAID", ignoreCase = true) &&
                            customerRepository != null

                    if (isUnpaidInvoice) {
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldJama)
                                .bouncyClick {
                                    showReceivePaymentDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Receive Pay",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Option 0.5: Convert to Invoice Button for Quotations
                    if (document is DocumentModel.QuotationDoc) {
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ImperialGold)
                                .bouncyClick {
                                    showConvertConfirmDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Transform,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Convert to Bill",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // Option 1: Edit Document Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .background(ElevatedSlate)
                            .bouncyClick {
                                onNavigateToEdit(docType, docId)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Document",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                        }
                    }

                    // Option 1.5: WhatsApp Quick Dispatch Button
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WhatsAppGreen)
                            .bouncyClick {
                                showWhatsAppDialog = true
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Option 2: See as PDF Button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPrimaryBlue)
                            .bouncyClick {
                                if (!isGeneratingPdf) openPdf()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "See as PDF",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimaryBlue)
            }
        } else if (document == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Document not found", color = TextMuted)
            }
        } else {
            val doc = document!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DeepObsidianCanvas)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status & Primary Amount Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Document Type & Number
                            Column {
                                val label = when (doc) {
                                    is DocumentModel.InvoiceDoc -> "TAX INVOICE"
                                    is DocumentModel.QuotationDoc -> "QUOTATION ESTIMATE"
                                    is DocumentModel.CreditNoteDoc -> "SALES RETURN (CN)"
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSubtle,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                val number = when (doc) {
                                    is DocumentModel.InvoiceDoc -> doc.entity.invoiceNumber
                                    is DocumentModel.QuotationDoc -> doc.entity.quotationNumber
                                    is DocumentModel.CreditNoteDoc -> doc.entity.creditNoteNumber
                                }
                                Text(
                                    text = number,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                )
                            }

                            // Status Badges (Fulfillment & Payment)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (doc is DocumentModel.InvoiceDoc) {
                                    val fStatus = doc.entity.fulfillmentStatus.lowercase()
                                    val (fText, fBg, fColor) = when (fStatus) {
                                        "ready" -> Triple("READY", Color(0xFF1E3A8A), Color(0xFF93C5FD))
                                        "dispatched" -> Triple("DISPATCHED", Color(0xFF3B0764), Color(0xFFD8B4FE))
                                        "delivered" -> Triple("DELIVERED", EmeraldJamaBg, EmeraldJama)
                                        else -> Triple("NOT READY", Color(0xFF451A03), Color(0xFFFCD34D))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(fBg)
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = fText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = fColor
                                            )
                                        )
                                    }
                                }

                                val (statusText, statusBg, statusColor) = when (doc) {
                                    is DocumentModel.InvoiceDoc -> when (doc.entity.status.uppercase()) {
                                        "PAID" -> Triple("PAID ✓", EmeraldJamaBg, EmeraldJama)
                                        "PARTIAL" -> Triple("PARTIAL", Color(0xFFFEF3C7), WarningAmber)
                                        else -> Triple("UNPAID", RubyUdharBg, RubyUdhar)
                                    }
                                    is DocumentModel.QuotationDoc -> when (doc.entity.status.uppercase()) {
                                        "CONVERTED" -> Triple("CONVERTED ✓", EmeraldJamaBg, EmeraldJama)
                                        "EXPIRED" -> Triple("EXPIRED", ElevatedSlate, TextMuted)
                                        else -> Triple("PENDING", Color(0xFFFEF3C7), WarningAmber)
                                    }
                                    is DocumentModel.CreditNoteDoc -> when (doc.entity.status.uppercase()) {
                                        "VOIDED" -> Triple("VOIDED", RubyUdharBg, RubyUdhar)
                                        else -> Triple("ACTIVE ✓", EmeraldJamaBg, EmeraldJama)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BorderStrokeDark)
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalAmount = when (doc) {
                            is DocumentModel.InvoiceDoc -> doc.entity.grandTotal
                            is DocumentModel.QuotationDoc -> doc.entity.grandTotal
                            is DocumentModel.CreditNoteDoc -> doc.entity.totalAmount
                        }

                        val date = when (doc) {
                            is DocumentModel.InvoiceDoc -> doc.entity.createdAt
                            is DocumentModel.QuotationDoc -> doc.entity.createdAt
                            is DocumentModel.CreditNoteDoc -> doc.entity.createdAt
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Total Amount", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text(
                                    text = CurrencyFormatter.formatINR(totalAmount),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = when (doc) {
                                            is DocumentModel.CreditNoteDoc -> RubyUdhar
                                            else -> EmeraldJama
                                        }
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Issue Date", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text(
                                    text = dateFormat.format(Date(date)),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        if (doc is DocumentModel.InvoiceDoc && !doc.entity.status.equals("PAID", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSlate)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Paid: ${CurrencyFormatter.formatINR(doc.entity.paidAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EmeraldJama,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Balance Due: ${CurrencyFormatter.formatINR(doc.entity.balanceDue)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = RubyUdhar,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }
                    }
                }

                // If Invoice: Order Fulfillment Progression Card
                if (doc is DocumentModel.InvoiceDoc) {
                    val inv = doc.entity
                    val fulfillment = inv.fulfillmentStatus.lowercase()
                    val (fText, fBg, fColor) = when (fulfillment) {
                        "ready" -> Triple("READY FOR DISPATCH 📦", Color(0xFF1E3A8A), Color(0xFF93C5FD))
                        "dispatched" -> Triple("DISPATCHED 🚚", Color(0xFF3B0764), Color(0xFFD8B4FE))
                        "delivered" -> Triple("DELIVERED ✓", EmeraldJamaBg, EmeraldJama)
                        else -> Triple("NOT READY ⏳", Color(0xFF451A03), Color(0xFFFCD34D))
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MidnightCardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ORDER FULFILLMENT STATUS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextSubtle,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(fBg)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = fText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = fColor
                                            )
                                        )
                                    }
                                }

                                if (fulfillment != "delivered") {
                                    val (nextActionText, nextTarget) = when (fulfillment) {
                                        "ready" -> Pair("Mark Dispatched 🚚", "dispatched")
                                        "dispatched" -> Pair("Mark Delivered ✓", "delivered")
                                        else -> Pair("Mark Ready 📦", "ready")
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (fulfillment == "dispatched") EmeraldJama else BrandPrimaryBlue)
                                            .clickable(enabled = !isUpdatingFulfillment) {
                                                scope.launch {
                                                    isUpdatingFulfillment = true
                                                    billingRepository.updateFulfillmentStatus(inv.id, inv.serverOrderId, nextTarget)
                                                    val reloaded = withContext(Dispatchers.IO) {
                                                        billingRepository.getInvoiceById(inv.id)
                                                    }
                                                    if (reloaded != null) {
                                                        document = DocumentModel.InvoiceDoc(reloaded)
                                                    }
                                                    isUpdatingFulfillment = false
                                                    Toast.makeText(context, "Order marked as ${nextTarget.uppercase()}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isUpdatingFulfillment) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = nextActionText,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Party / Customer Information Card
                val (partyName, partyPhone) = when (doc) {
                    is DocumentModel.InvoiceDoc -> Pair(doc.entity.customerName, doc.entity.customerPhone)
                    is DocumentModel.QuotationDoc -> Pair(doc.entity.customerName, doc.entity.customerPhone)
                    is DocumentModel.CreditNoteDoc -> Pair(doc.entity.customerName, doc.entity.customerPhone)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PARTY / CLIENT DETAILS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (partyPhone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            showWhatsAppDialog = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldJamaBg)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = EmeraldJama, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$partyPhone"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(BrandPrimaryBlue.copy(alpha = 0.15f))
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = BrandPrimaryBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (avatarBg, avatarText) = getAvatarColors(partyName)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = partyName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = avatarText
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = partyName.ifBlank { "Walk-in Customer" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                )
                                if (partyPhone.isNotBlank()) {
                                    Text(
                                        text = partyPhone,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                            }
                        }
                    }
                }

                // If Credit Note: Show Reason and Original Bill
                if (doc is DocumentModel.CreditNoteDoc) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MidnightCardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SALES RETURN METADATA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RubyUdhar,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Return Reason", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                Text(doc.entity.reason, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontWeight = FontWeight.Bold))
                            }
                            if (doc.entity.originalInvoiceNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Against Invoice", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(doc.entity.originalInvoiceNumber, style = MaterialTheme.typography.bodySmall.copy(color = BrandPrimaryBlue, fontWeight = FontWeight.Bold))
                                }
                            }
                            if (doc.entity.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Remarks", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(doc.entity.notes, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                }
                            }
                        }
                    }
                }

                // Items Breakdown Table
                val parsedItems = when (doc) {
                    is DocumentModel.InvoiceDoc -> DocumentItemParser.parseLineItems(
                        itemsJson = doc.entity.itemsJson,
                        itemsSummary = doc.entity.itemsSummary,
                        defaultSubtotal = doc.entity.subtotal
                    )
                    is DocumentModel.QuotationDoc -> DocumentItemParser.parseLineItems(
                        itemsJson = doc.entity.itemsJson,
                        itemsSummary = doc.entity.itemsSummary,
                        defaultSubtotal = doc.entity.subtotal
                    )
                    is DocumentModel.CreditNoteDoc -> DocumentItemParser.parseLineItems(
                        itemsJson = doc.entity.itemsJson,
                        itemsSummary = doc.entity.itemsSummary,
                        defaultSubtotal = doc.entity.subtotal
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ITEMIZED PRODUCTS & PRICING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElevatedSlate)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${parsedItems.size} ${if (parsedItems.size == 1) "item" else "items"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BrandPrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (parsedItems.isEmpty()) {
                            Text("Standard invoice goods", color = TextMuted, fontSize = 13.sp)
                        } else {
                            // Column Headers Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElevatedSlate.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = "Item Details",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Qty",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = "Rate",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.width(64.dp)
                                )
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.width(68.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            parsedItems.forEachIndexed { idx, itm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSubtle,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                        Text(
                                            text = itm.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextWhite,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (itm.sku.isNotBlank()) {
                                                Text(
                                                    text = itm.sku,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                Text("•", color = TextMuted, fontSize = 10.sp)
                                            }
                                            Text(
                                                text = "GST ${itm.gstRatePercent.toInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = EmeraldJama,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${itm.quantity}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatINR(itm.unitPrice),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatINR(itm.total),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.width(68.dp)
                                    )
                                }
                                if (idx < parsedItems.size - 1) {
                                    HorizontalDivider(color = ElevatedSlate, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Items Total Summary Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Qty: ${parsedItems.sumOf { it.quantity }} units",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "Taxable: ${CurrencyFormatter.formatINR(parsedItems.sumOf { it.subtotal })}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldJama,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Tax & Totals Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FINANCIAL SUMMARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSubtle,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        when (doc) {
                            is DocumentModel.InvoiceDoc -> {
                                val inv = doc.entity
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Taxable Subtotal", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(CurrencyFormatter.formatINR(inv.subtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (inv.cgst > 0 || inv.sgst > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("CGST", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                        Text(CurrencyFormatter.formatINR(inv.cgst), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("SGST", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                        Text(CurrencyFormatter.formatINR(inv.sgst), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                    }
                                }
                                if (inv.igst > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("IGST", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                        Text(CurrencyFormatter.formatINR(inv.igst), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                    }
                                }
                                if (inv.roundOff != 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Round Off", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                        Text(CurrencyFormatter.formatINR(inv.roundOff), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                    }
                                }
                            }
                            is DocumentModel.QuotationDoc -> {
                                val q = doc.entity
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(CurrencyFormatter.formatINR(q.subtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Tax", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(CurrencyFormatter.formatINR(q.taxAmount), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                }
                            }
                            is DocumentModel.CreditNoteDoc -> {
                                val cn = doc.entity
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Return Subtotal", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(CurrencyFormatter.formatINR(cn.subtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Reversed Tax", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                                    Text(CurrencyFormatter.formatINR(cn.taxAmount), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = BorderStrokeDark)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET PAYABLE / VALUE",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = CurrencyFormatter.formatINR(
                                    when (doc) {
                                        is DocumentModel.InvoiceDoc -> doc.entity.grandTotal
                                        is DocumentModel.QuotationDoc -> doc.entity.grandTotal
                                        is DocumentModel.CreditNoteDoc -> doc.entity.totalAmount
                                    }
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = when (doc) {
                                        is DocumentModel.CreditNoteDoc -> RubyUdhar
                                        else -> EmeraldJama
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    ChopdaPdfLoadingOverlay(
        visible = isGeneratingPdf,
        title = "Opening Ledger PDF...",
        subtitle = "Fetching high-resolution ledger copy..."
    )
}

    if (showReceivePaymentDialog && document is DocumentModel.InvoiceDoc && customerRepository != null) {
        val inv = (document as DocumentModel.InvoiceDoc).entity
        RecordPaymentDialog(
            isJama = true,
            partyName = inv.customerName.ifBlank { "Customer" },
            unpaidInvoices = listOf(inv),
            initialInvoiceId = inv.id,
            onDismiss = { showReceivePaymentDialog = false },
            onConfirm = { amount, mode, notes, _ ->
                scope.launch {
                    val cust = withContext(Dispatchers.IO) {
                        customerRepository.getCustomerByPhone(activeCompany.id, inv.customerPhone)
                    }
                    val custId = cust?.id ?: 1L
                    customerRepository.recordPayment(
                        companyId = activeCompany.id,
                        customerId = custId,
                        amount = amount,
                        isJama = true,
                        paymentMode = mode,
                        notes = notes,
                        invoiceId = inv.id,
                        orderId = inv.serverOrderId,
                        invoiceNumber = inv.invoiceNumber
                    )
                    withContext(Dispatchers.IO) {
                        val reloaded = billingRepository.getInvoiceById(docId)
                        if (reloaded != null) {
                            document = DocumentModel.InvoiceDoc(reloaded)
                        }
                    }
                    showReceivePaymentDialog = false
                    Toast.makeText(context, "✅ Payment of ${CurrencyFormatter.formatINR(amount)} received for ${inv.invoiceNumber}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showWhatsAppDialog && document != null) {
        val currentDoc = document!!
        val docTypeStr = when (currentDoc) {
            is DocumentModel.InvoiceDoc -> "invoice"
            is DocumentModel.QuotationDoc -> "quotation"
            is DocumentModel.CreditNoteDoc -> "credit_note"
        }
        val docNum = when (currentDoc) {
            is DocumentModel.InvoiceDoc -> currentDoc.entity.invoiceNumber
            is DocumentModel.QuotationDoc -> currentDoc.entity.quotationNumber
            is DocumentModel.CreditNoteDoc -> currentDoc.entity.creditNoteNumber
        }
        val cName = when (currentDoc) {
            is DocumentModel.InvoiceDoc -> currentDoc.entity.customerName
            is DocumentModel.QuotationDoc -> currentDoc.entity.customerName
            is DocumentModel.CreditNoteDoc -> currentDoc.entity.customerName
        }
        val cPhone = when (currentDoc) {
            is DocumentModel.InvoiceDoc -> currentDoc.entity.customerPhone
            is DocumentModel.QuotationDoc -> currentDoc.entity.customerPhone
            is DocumentModel.CreditNoteDoc -> currentDoc.entity.customerPhone
        }
        val amt = when (currentDoc) {
            is DocumentModel.InvoiceDoc -> currentDoc.entity.grandTotal
            is DocumentModel.QuotationDoc -> currentDoc.entity.grandTotal
            is DocumentModel.CreditNoteDoc -> currentDoc.entity.totalAmount
        }
        val srvOrderId = when (currentDoc) {
            is DocumentModel.InvoiceDoc -> currentDoc.entity.serverOrderId
            is DocumentModel.QuotationDoc -> currentDoc.entity.serverQuotationId
            is DocumentModel.CreditNoteDoc -> currentDoc.entity.serverCreditNoteId
        }

        WhatsAppDispatchDialog(
            docType = docTypeStr,
            docId = docId,
            serverOrderId = srvOrderId,
            docNumber = docNum,
            partyName = cName,
            partyPhone = cPhone,
            amount = amt,
            upiVpa = activeCompany.upiVpa,
            firmName = activeCompany.firmName,
            apiService = billingRepository.apiService,
            billingRepository = billingRepository,
            quotationRepository = quotationRepository,
            onDismiss = { showWhatsAppDialog = false }
        )
    }

    if (showDeleteConfirmDialog && document != null) {
        val docName = when (val doc = document) {
            is DocumentModel.InvoiceDoc -> "Invoice ${doc.entity.invoiceNumber}"
            is DocumentModel.QuotationDoc -> "Quotation ${doc.entity.quotationNumber}"
            is DocumentModel.CreditNoteDoc -> "Credit Note ${doc.entity.creditNoteNumber}"
            null -> "Document"
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete $docName?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this record from your system. This action cannot be undone.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    deleteCurrentDocument()
                }) {
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSubtle)
                }
            },
            containerColor = MidnightCardSurface
        )
    }

    if (showConvertConfirmDialog && document is DocumentModel.QuotationDoc) {
        val q = (document as DocumentModel.QuotationDoc).entity
        AlertDialog(
            onDismissRequest = { showConvertConfirmDialog = false },
            title = { Text("Convert to Tax Invoice?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to convert Quotation #${q.quotationNumber} into an active Sales GST Invoice for ${q.customerName}?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showConvertConfirmDialog = false
                    convertQuotationToInvoice()
                }) {
                    Text("Convert", color = ImperialGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertConfirmDialog = false }) {
                    Text("Cancel", color = TextSubtle)
                }
            },
            containerColor = MidnightCardSurface
        )
    }

    if (showRevisionHistoryDialog && document is DocumentModel.InvoiceDoc) {
        val inv = (document as DocumentModel.InvoiceDoc).entity
        InvoiceRevisionHistoryDialog(
            invoiceId = inv.id,
            invoiceNumber = inv.invoiceNumber,
            billingRepository = billingRepository,
            onDismiss = { showRevisionHistoryDialog = false }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
