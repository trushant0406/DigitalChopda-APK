package com.digitalchopda.app.presentation.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.QuotationRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlueDeep
import com.digitalchopda.app.core.designsystem.ChopdaPdfLoadingOverlay
import com.digitalchopda.app.core.designsystem.DarkNavy
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
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.network.DigitalChopdaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * WhatsApp Dispatch Button State
 */
private enum class WhatsAppSendState {
    IDLE,
    SENDING,
    SENT,
    ERROR
}

/**
 * ⚡ Native In-App PDF Viewer & Dual-Channel Sharing Dock
 *
 * Implements:
 * 1. Native zero-dependency multi-page rendering using Android's `PdfRenderer`
 * 2. Pinch-to-zoom & double-tap zoom with strict memory reclamation on disposal
 * 3. Page count indicator chip (`Page X of Y`)
 * 4. Dual-Action Sharing Dock:
 *    - Option 1 (Primary Blue): Direct dispatch via Company Headless WhatsApp Web API
 *    - Option 2 (Secondary Outlined): Native multi-app system chooser via FileProvider
 * 5. Smart Fallback Sheet when Headless WhatsApp session is offline/disconnected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    invoiceId: Long,
    customerName: String,
    customerPhone: String,
    invoiceNumber: String,
    docType: String = "invoice",
    serverOrderId: Long? = null,
    apiService: DigitalChopdaApiService?,
    billingRepository: BillingRepository? = null,
    quotationRepository: QuotationRepository? = null,
    fileProviderUriBuilder: (File) -> Uri,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var currentServerOrderId by remember(serverOrderId) {
        mutableStateOf(if (serverOrderId != null && serverOrderId > 0L) serverOrderId else null)
    }
    val resolvedDocType = remember(docType, invoiceNumber) {
        if (docType.isNotBlank() && !docType.equals("invoice", ignoreCase = true)) docType
        else if (invoiceNumber.startsWith("CN", ignoreCase = true)) "credit_note"
        else if (invoiceNumber.startsWith("QT", ignoreCase = true) || invoiceNumber.startsWith("EST", ignoreCase = true)) "quotation"
        else docType.ifBlank { "invoice" }
    }

    LaunchedEffect(invoiceId, invoiceNumber) {
        if (currentServerOrderId == null || currentServerOrderId!! <= 0L) {
            withContext(Dispatchers.IO) {
                try {
                    if (resolvedDocType.equals("quotation", ignoreCase = true)) {
                        val q = (if (invoiceId > 0) quotationRepository?.getQuotationById(invoiceId) else null)
                            ?: quotationRepository?.getQuotationByNumber(1L, invoiceNumber)
                        if (q != null) {
                            if (q.serverQuotationId != null && q.serverQuotationId > 0) {
                                currentServerOrderId = q.serverQuotationId
                            } else if (apiService != null) {
                                val syncRes = quotationRepository?.syncQuotationById(q.id)
                                syncRes?.getOrNull()?.serverQuotationId?.let { sId ->
                                    currentServerOrderId = sId
                                }
                            }
                        }
                    } else {
                        val inv = (if (invoiceId > 0) billingRepository?.getInvoiceById(invoiceId) else null)
                            ?: billingRepository?.getInvoiceByNumber(1L, invoiceNumber)
                        if (inv != null) {
                            if (inv.serverOrderId != null && inv.serverOrderId > 0) {
                                currentServerOrderId = inv.serverOrderId
                            } else if (apiService != null) {
                                val syncRes = billingRepository?.syncInvoiceById(inv.id)
                                syncRes?.getOrNull()?.serverOrderId?.let { sId ->
                                    currentServerOrderId = sId
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val file = remember(filePath) { File(filePath) }
    var totalPages by remember { mutableIntStateOf(0) }
    val renderedPages = remember { mutableStateListOf<Bitmap>() }
    var isLoading by remember { mutableStateOf(true) }
    var renderError by remember { mutableStateOf<String?>(null) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // WhatsApp Dispatch States
    var whatsAppState by remember { mutableStateOf(WhatsAppSendState.IDLE) }
    var whatsAppErrorMessage by remember { mutableStateOf<String?>(null) }
    var showFallbackSheet by remember { mutableStateOf(false) }

    // Page indicator tracking
    val currentVisiblePage by remember {
        derivedStateOf {
            if (totalPages == 0) 1 else (listState.firstVisibleItemIndex + 1).coerceAtMost(totalPages)
        }
    }

    // ─── PDF RENDERER LIFECYCLE & CLEANUP ───
    DisposableEffect(filePath) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        scope.launch(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        renderError = "Document file not found or empty."
                        isLoading = false
                    }
                    return@launch
                }

                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(pfd!!)
                renderer = pdfRenderer
                val pagesCount = pdfRenderer.pageCount

                withContext(Dispatchers.Main) {
                    totalPages = pagesCount
                }

                val bitmaps = ArrayList<Bitmap>(pagesCount)
                // Render pages at 2.0x DPI scale for razor-sharp rendering on modern screens
                for (i in 0 until pagesCount) {
                    val page = pdfRenderer.openPage(i)
                    val width = (page.width * 2.0f).toInt()
                    val height = (page.height * 2.0f).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }

                withContext(Dispatchers.Main) {
                    renderedPages.clear()
                    renderedPages.addAll(bitmaps)
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    renderError = "Failed to render PDF: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }

        onDispose {
            try {
                renderer?.close()
                pfd?.close()
                renderedPages.forEach { if (!it.isRecycled) it.recycle() }
                renderedPages.clear()
            } catch (_: Exception) {}
        }
    }

    val whatsAppGreen = Color(0xFF25D366)
    var showWhatsAppDispatchDialog by remember { mutableStateOf(false) }

    // ─── OPTION 2: SYSTEM MULTI-APP SHARE CHOOSER ───
    fun shareViaSystemChooser() {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                return
            }
            val contentUri = fileProviderUriBuilder(file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hello $customerName,\nPlease find your document $invoiceNumber attached.\nThank you for your business!"
                )
                putExtra(Intent.EXTRA_SUBJECT, "Document $invoiceNumber")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Document $invoiceNumber")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── OPTION 1: DISPATCH TO PARTY DIRECTLY VIA LOGGED-IN WHATSAPP ───
    fun sendToWhatsAppParty() {
        val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
        val caption = "Hello $customerName,\nPlease find your document $invoiceNumber attached.\nThank you for your business!"

        try {
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
                return
            }
            val contentUri = fileProviderUriBuilder(file)

            // Direct WhatsApp Send Intent
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (formattedPhone.isNotBlank()) {
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                }
                setPackage("com.whatsapp")
            }
            context.startActivity(sendIntent)
        } catch (_: Exception) {
            // Fallback to WhatsApp Business
            try {
                val contentUri = fileProviderUriBuilder(file)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (formattedPhone.isNotBlank()) {
                        putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    }
                    setPackage("com.whatsapp.w4b")
                }
                context.startActivity(sendIntent)
            } catch (_: Exception) {
                if (formattedPhone.isNotBlank()) {
                    try {
                        val waUrl = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(caption)}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                    } catch (_: Exception) {
                        shareViaSystemChooser()
                    }
                } else {
                    shareViaSystemChooser()
                }
            }
        }
    }

    // ─── OPTION 1B: OPTIONAL DISPATCH VIA COMPANY HEADLESS WHATSAPP ───
    fun sendViaCompanyWhatsApp() {
        if (whatsAppState == WhatsAppSendState.SENDING) return

        scope.launch {
            whatsAppState = WhatsAppSendState.SENDING
            try {
                var targetId = currentServerOrderId
                if (targetId == null || targetId <= 0L) {
                    val syncedTarget = withContext(Dispatchers.IO) {
                        try {
                            if (resolvedDocType.equals("quotation", ignoreCase = true)) {
                                val q = (if (invoiceId > 0) quotationRepository?.getQuotationById(invoiceId) else null)
                                    ?: quotationRepository?.getQuotationByNumber(1L, invoiceNumber)
                                if (q != null) {
                                    if (q.serverQuotationId != null && q.serverQuotationId > 0) {
                                        q.serverQuotationId
                                    } else {
                                        quotationRepository?.syncQuotationById(q.id)?.getOrNull()?.serverQuotationId
                                    }
                                } else null
                            } else {
                                val inv = (if (invoiceId > 0) billingRepository?.getInvoiceById(invoiceId) else null)
                                    ?: billingRepository?.getInvoiceByNumber(1L, invoiceNumber)
                                if (inv != null) {
                                    if (inv.serverOrderId != null && inv.serverOrderId > 0) {
                                        inv.serverOrderId
                                    } else {
                                        billingRepository?.syncInvoiceById(inv.id)?.getOrNull()?.serverOrderId
                                    }
                                } else null
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (syncedTarget != null && syncedTarget > 0L) {
                        currentServerOrderId = syncedTarget
                        targetId = syncedTarget
                    }
                }

                if (targetId == null || targetId <= 0L) {
                    whatsAppState = WhatsAppSendState.ERROR
                    whatsAppErrorMessage = "This document is only saved locally. Please connect to internet or use Phone WhatsApp."
                    showFallbackSheet = true
                    return@launch
                }

                val service = apiService
                if (service == null) {
                    whatsAppState = WhatsAppSendState.ERROR
                    whatsAppErrorMessage = "Network service unavailable."
                    showFallbackSheet = true
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    when (resolvedDocType) {
                        "quotation" -> service.sendQuotationViaCompanyWhatsApp(targetId)
                        "credit_note" -> service.sendCreditNoteViaCompanyWhatsApp(targetId)
                        else -> service.sendOrderViaCompanyWhatsApp(targetId)
                    }
                }

                if (response.isSuccessful && response.body()?.ok == true) {
                    whatsAppState = WhatsAppSendState.SENT
                    Toast.makeText(context, "Dispatched via Company WhatsApp ✓", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = response.body()?.error
                        ?: response.body()?.message
                        ?: "Company WhatsApp Web is currently disconnected on server."
                    whatsAppState = WhatsAppSendState.ERROR
                    whatsAppErrorMessage = errorMsg
                    showFallbackSheet = true
                }
            } catch (e: Exception) {
                whatsAppState = WhatsAppSendState.ERROR
                whatsAppErrorMessage = e.localizedMessage ?: "Failed to reach Company WhatsApp Server."
                showFallbackSheet = true
            }
        }
    }

    val cleanCustomerName = remember(customerName) {
        try {
            java.net.URLDecoder.decode(customerName, "UTF-8").replace("+", " ")
        } catch (e: Exception) {
            customerName.replace("+", " ")
        }
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (invoiceNumber.isNotBlank()) "Invoice $invoiceNumber" else "Document Viewer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                        )
                        if (cleanCustomerName.isNotBlank()) {
                            Text(
                                text = "Party: $cleanCustomerName ${if (customerPhone.isNotBlank()) "($customerPhone)" else ""}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle)
                            )
                        }
                    }
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
                    // Quick top share shortcut
                    IconButton(onClick = { shareViaSystemChooser() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = DarkNavy
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
            // ─── DUAL-ACTION SHARING DOCK (WHITE & BLUE) ───
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp),
                color = MidnightCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Button 1: Share on WhatsApp (WhatsApp Green #25D366)
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(whatsAppGreen)
                                .bouncyClick { showWhatsAppDispatchDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Share on WhatsApp",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // Button 2: Share (System Chooser for external apps)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, BrandPrimaryBlue, RoundedCornerShape(12.dp))
                                .background(MidnightCardSurface)
                                .bouncyClick { shareViaSystemChooser() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandPrimaryBlue
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepObsidianCanvas)
        ) {
            when {
                renderError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RubyUdhar,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = renderError ?: "Error",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RubyUdhar,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                else -> {
                    // Multi-page Vertical Scrolling with Pinch & Pan Zoom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isLoading) Modifier.blur(14.dp) else Modifier)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 3.5f)
                                    if (scale == 1f) {
                                        offset = Offset.Zero
                                    } else {
                                        offset = Offset(
                                            x = offset.x + pan.x,
                                            y = offset.y + pan.y
                                        )
                                    }
                                }
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = renderedPages,
                                key = { index, _ -> "pdf_page_$index" },
                                contentType = { _, _ -> "pdf_page" }
                            ) { pageIndex, pageBitmap ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, RoundedCornerShape(8.dp)),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                                ) {
                                    Image(
                                        bitmap = pageBitmap.asImageBitmap(),
                                        contentDescription = "Page ${pageIndex + 1}",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Floating Page Indicator Badge (Top-Right)
                        if (totalPages > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MidnightCardSurface.copy(alpha = 0.92f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "Page $currentVisiblePage / $totalPages",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Double Tap to Reset Zoom Reset Chip
                        if (scale > 1.05f) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .bouncyClick {
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = BrandPrimaryBlue,
                                shadowElevation = 6.dp
                            ) {
                                Text(
                                    text = "Reset Zoom (1.0x)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            ChopdaPdfLoadingOverlay(
                visible = isLoading,
                title = "Opening Ledger PDF...",
                subtitle = "Rendering crisp high-resolution pages..."
            )
        }
    }

    // ─── SMART FALLBACK BOTTOM SHEET (WHITE & BLUE) ───
    if (showFallbackSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFallbackSheet = false },
            sheetState = sheetState,
            containerColor = MidnightCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RubyUdharBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RubyUdhar,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Company WhatsApp Offline",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "Central dispatch session is unreachable",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = whatsAppErrorMessage
                        ?: "The headless Company WhatsApp Web session is currently disconnected or waiting for QR re-authentication.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action: Fallback to System Chooser Immediately
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPrimaryBlue)
                        .bouncyClick {
                            showFallbackSheet = false
                            shareViaSystemChooser()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share via Personal WhatsApp / Other Apps",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action: Dismiss
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                        .background(ElevatedSlate)
                        .bouncyClick { showFallbackSheet = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showWhatsAppDispatchDialog) {
        com.digitalchopda.app.presentation.billing.WhatsAppDispatchDialog(
            docType = resolvedDocType,
            docId = invoiceId,
            serverOrderId = currentServerOrderId,
            docNumber = invoiceNumber,
            partyName = cleanCustomerName,
            partyPhone = customerPhone,
            amount = 0.0,
            firmName = "Our Company",
            apiService = apiService,
            billingRepository = billingRepository,
            quotationRepository = quotationRepository,
            pdfFilePath = filePath,
            onDismiss = { showWhatsAppDispatchDialog = false }
        )
    }
}
