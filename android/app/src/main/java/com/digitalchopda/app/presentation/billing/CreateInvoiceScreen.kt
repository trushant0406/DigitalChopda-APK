package com.digitalchopda.app.presentation.billing

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.digitalchopda.app.core.designsystem.ChopdaPdfLoadingOverlay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlueDeep
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlueLight
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElectricBrandCyan
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.GoldFoilGradient
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.QuantityStepper
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.core.printer.BluetoothPrinterManager
import com.digitalchopda.app.core.sound.SoundEffectsManager
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.InventoryRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.InventoryItem
import com.digitalchopda.app.domain.model.InvoiceStatus
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.domain.model.PaymentMode
import com.digitalchopda.app.domain.model.Product
import com.digitalchopda.app.presentation.products.AddEditProductDialog
import androidx.compose.ui.text.style.TextOverflow
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.domain.usecase.CreateInvoiceUseCase
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.collectAsState
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    billingRepository: BillingRepository,
    companyRepository: CompanyRepository,
    inventoryRepository: InventoryRepository,
    customerRepository: CustomerRepository,
    pdfGenerator: InvoicePdfGenerator,
    printerManager: BluetoothPrinterManager,
    productRepository: com.digitalchopda.app.data.repository.ProductRepository? = null,
    quotationRepository: com.digitalchopda.app.data.repository.QuotationRepository? = null,
    initialMode: String = "invoice",
    editId: Long = 0L,
    initialPartyId: Long = 0L,
    initialPartyName: String = "",
    initialPartyPhone: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (filePath: String, invoiceId: Long, customerName: String, customerPhone: String, invoiceNumber: String, docType: String, serverOrderId: Long) -> Unit = { _, _, _, _, _, _, _ -> },
    viewModel: InvoiceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = InvoiceViewModel.Factory(
            billingRepository,
            companyRepository,
            inventoryRepository,
            customerRepository,
            productRepository,
            quotationRepository
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    LaunchedEffect(initialMode, editId, initialPartyId, initialPartyName, initialPartyPhone) {
        if (initialMode.equals("quotation", ignoreCase = true)) {
            viewModel.setDocumentMode(InvoiceViewModel.DocumentMode.QUOTATION)
        } else {
            viewModel.setDocumentMode(InvoiceViewModel.DocumentMode.INVOICE)
        }
        if (editId > 0L) {
            viewModel.loadDocumentForEdit(initialMode, editId)
        } else if (initialPartyId > 0L || initialPartyName.isNotBlank()) {
            viewModel.preselectParty(
                partyName = initialPartyName,
                partyPhone = initialPartyPhone,
                partyId = initialPartyId
            )
        }
    }

    val activeCompany by viewModel.activeCompany.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val customerGstin by viewModel.customerGstin.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val paidAmount by viewModel.paidAmount.collectAsState()
    val selectedPaymentMode by viewModel.selectedPaymentMode.collectAsState()
    val lineItems by viewModel.lineItems.collectAsState()
    val taxCalc by viewModel.taxCalculation.collectAsState()
    val stockItems by viewModel.stockItems.collectAsState()
    val catalogProducts by viewModel.catalogProducts.collectAsState()
    val existingCustomers by viewModel.matchingCustomers.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val documentMode by viewModel.documentMode.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showDirectAddProductPrompt by remember { mutableStateOf(false) }
    var productQuickFilter by remember { mutableStateOf("") }
    val filteredCatalogProducts = remember(catalogProducts, productQuickFilter) {
        if (productQuickFilter.isBlank()) catalogProducts
        else catalogProducts.filter {
            it.name.contains(productQuickFilter, ignoreCase = true) ||
                    it.hsnCode.orEmpty().contains(productQuickFilter, ignoreCase = true)
        }
    }
    var showCustomerModal by remember { mutableStateOf(false) }
    var partyDropdownExpanded by remember { mutableStateOf(false) }
    var partySearchQuery by remember { mutableStateOf("") }
    var showThermalPreview by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    val thermalSheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.then(if (isSaving) Modifier.blur(14.dp) else Modifier),
            containerColor = DeepObsidianCanvas,
            topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkNavy
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) "Quotation Estimate" else "Quick Billing",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = DarkNavy,
                                letterSpacing = 0.3.sp,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) Color(0xFFFEF3C7) else BrandAccentSoft)
                                    .border(1.dp, if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) ImperialGold else Color(0xFFBFDBFE), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) "QUOTE" else "POS",
                                    color = if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) ImperialGold else BrandPrimaryBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${activeCompany.firmName.ifBlank { "ABC E-COMMERCE" }} • Tax Invoice",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scan Icon Button
                        IconButton(
                            onClick = { showBarcodeScanner = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = DarkNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // UPI QR Icon Button
                        IconButton(
                            onClick = { showUpiQrDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFFBFDBFE), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "UPI QR",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Thermal Print Icon Button
                        IconButton(
                            onClick = { showThermalPreview = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Thermal Bill",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
        },
        bottomBar = {
            // Sleek Floating Action Bar cleanly positioned above Dock
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val isQuotationMode = documentMode == InvoiceViewModel.DocumentMode.QUOTATION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Draft Pill
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                            .bouncyClick {
                                Toast.makeText(context, "Draft Saved", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📄", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Draft",
                                color = DarkNavy,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 🖨️ Quick Thermal Print Trigger
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0F0F172A))
                            .bouncyClick { showThermalPreview = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🖨️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Print",
                                color = DarkNavy,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Full-width Primary CTA: SAVE & SEND
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimaryBlue, BrandPrimaryBlueLight)
                                )
                            )
                            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = BrandPrimaryBlue.copy(alpha = 0.35f))
                            .bouncyClick {
                                if (lineItems.isEmpty()) {
                                    Toast.makeText(context, "Please add at least 1 product to the bill", Toast.LENGTH_SHORT).show()
                                    showAddItemDialog = true
                                    return@bouncyClick
                                }
                                viewModel.saveInvoice(
                                    pdfGenerator = pdfGenerator,
                                    isQuotation = isQuotationMode,
                                    onSuccess = { invoice, pdfFile, _ ->
                                        SoundEffectsManager.playInvoiceCreatedSound()
                                        val docType = if (isQuotationMode) "quotation" else "invoice"
                                        val docTypeTitle = if (isQuotationMode) "Quotation" else "Tax Invoice"
                                        Toast.makeText(context, "$docTypeTitle ${invoice.invoiceNumber} Ready!", Toast.LENGTH_SHORT).show()
                                        onNavigateToPdfViewer(
                                            pdfFile.absolutePath,
                                            invoice.id,
                                            invoice.customerName,
                                            invoice.customerPhone,
                                            invoice.invoiceNumber,
                                            docType,
                                            invoice.serverOrderId ?: 0L
                                        )
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isQuotationMode) "➤ SEND QUOTE (${CurrencyFormatter.formatCurrency(taxCalc.grandTotal)})" else "➤ SAVE & SEND (${CurrencyFormatter.formatCurrency(taxCalc.grandTotal)})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 📑 Document Mode Selector: Tax Invoice vs Quotation / Estimate
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MidnightCardSurface)
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isInvoice = documentMode == InvoiceViewModel.DocumentMode.INVOICE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isInvoice) BrandPrimaryBlue else Color.Transparent)
                            .clickable { viewModel.setDocumentMode(InvoiceViewModel.DocumentMode.INVOICE) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tax Invoice (GST)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isInvoice) FontWeight.Bold else FontWeight.Medium,
                                color = if (isInvoice) Color.White else TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }

                    val isQuotation = documentMode == InvoiceViewModel.DocumentMode.QUOTATION
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isQuotation) BrandPrimaryBlue else Color.Transparent)
                            .clickable { viewModel.setDocumentMode(InvoiceViewModel.DocumentMode.QUOTATION) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Quotation / Estimate",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isQuotation) FontWeight.Bold else FontWeight.Medium,
                                color = if (isQuotation) Color.White else TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // ─── 2. MERGED CUSTOMER & INVOICE META CARD ───
            item {
                ChopdaCard {
                    // Top row: DC/26-27/01009 | 15-Oct-2026
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = invoiceNumber.ifBlank { "DC/26-27/01009" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DarkNavy,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        )

                        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderStrokeDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom section: Choose Party (Customer / Prospect) with interactive Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) "BILL TO PARTY (PROSPECT)" else "BILL TO PARTY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSubtle,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ Edit / Custom",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BrandPrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier.clickable { showCustomerModal = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (partyDropdownExpanded) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { partyDropdownExpanded = !partyDropdownExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (custAvatarBg, custAvatarText) = getAvatarColors(customerName)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(custAvatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customerName.isNotBlank()) {
                                    Text(
                                        text = customerName.take(2).uppercase(),
                                        color = custAvatarText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = custAvatarText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customerName.ifBlank { "Choose Party / Customer..." },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (customerName.isNotBlank()) DarkNavy else TextMuted,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val partySubtitle = buildString {
                                    if (customerPhone.isNotBlank()) append(customerPhone)
                                    if (customerGstin.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append("GST: $customerGstin")
                                    }
                                    if (isEmpty()) append("Tap to choose party from ledger")
                                }
                                Text(
                                    text = partySubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElevatedSlate)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (partyDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle party dropdown",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Dropdown Menu
                        DropdownMenu(
                            expanded = partyDropdownExpanded,
                            onDismissRequest = {
                                partyDropdownExpanded = false
                                partySearchQuery = ""
                            },
                            modifier = Modifier
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                                .widthIn(min = 290.dp, max = 350.dp)
                                .heightIn(max = 420.dp)
                        ) {
                            // Search field
                            OutlinedTextField(
                                value = partySearchQuery,
                                onValueChange = { partySearchQuery = it },
                                placeholder = { Text("Search party by name or phone...", color = TextMuted, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = BrandPrimaryBlue, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (partySearchQuery.isNotBlank()) {
                                        IconButton(onClick = { partySearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ElevatedSlate,
                                    unfocusedContainerColor = ElevatedSlate,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = BrandPrimaryBlue,
                                    unfocusedBorderColor = BorderStrokeDark
                                )
                            )

                            // Quick Item: Walk-in Customer (Cash Sale)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(ElevatedSlate),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonOutline,
                                                contentDescription = null,
                                                tint = EmeraldJama,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Walk-in Customer (Cash Sale)",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextWhite,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Text(
                                                text = "Direct billing without khata record",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setCustomer("Walk-in Customer", "", "")
                                    partyDropdownExpanded = false
                                    partySearchQuery = ""
                                }
                            )

                            // Quick Item: + Add New Party / Custom Details
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(BrandPrimaryBlue.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = BrandPrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "+ Add New Party / Custom Details…",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandPrimaryBlue,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Text(
                                                text = "Manual entry, GSTIN, or phonebook contacts",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    partyDropdownExpanded = false
                                    partySearchQuery = ""
                                    showCustomerModal = true
                                }
                            )

                            HorizontalDivider(color = BorderStrokeDark, thickness = 1.dp)

                            val filteredParties = if (partySearchQuery.isBlank()) {
                                existingCustomers
                            } else {
                                existingCustomers.filter {
                                    it.name.contains(partySearchQuery, ignoreCase = true) ||
                                    it.phone.contains(partySearchQuery) ||
                                    it.gstin.contains(partySearchQuery, ignoreCase = true)
                                }
                            }

                            if (filteredParties.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "No parties found matching \"$partySearchQuery\"",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
                                        )
                                    },
                                    onClick = {}
                                )
                            } else {
                                filteredParties.forEach { cust ->
                                    val isSelected = customerName.equals(cust.name, ignoreCase = true) &&
                                            (customerPhone.isBlank() || cust.phone.isBlank() || customerPhone == cust.phone)
                                    val (cAvatarBg, cAvatarText) = getAvatarColors(cust.name)

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(cAvatarBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = cust.name.take(2).uppercase().ifBlank { "P" },
                                                        color = cAvatarText,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = cust.name,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                                color = if (isSelected) ImperialGoldLight else TextWhite,
                                                                fontSize = 13.sp
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (cust.partyTag.isNotBlank() && cust.partyTag != "Active Party") {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = cust.partyTag,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.sp,
                                                                    color = TextMuted,
                                                                    fontWeight = FontWeight.Medium
                                                                ),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(ElevatedSlate)
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }

                                                    val line2 = buildString {
                                                        if (cust.phone.isNotBlank()) append(cust.phone)
                                                        if (cust.gstin.isNotBlank()) {
                                                            if (isNotEmpty()) append(" • ")
                                                            append("GST: ${cust.gstin}")
                                                        }
                                                    }
                                                    if (line2.isNotBlank()) {
                                                        Text(
                                                            text = line2,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = TextMuted,
                                                                fontSize = 10.sp
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = ImperialGoldLight,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setCustomer(cust.name, cust.phone, cust.gstin)
                                            partyDropdownExpanded = false
                                            partySearchQuery = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── 3. ACTION BAR & PRESETS ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BrandPrimaryBlue, RoundedCornerShape(10.dp))
                            .bouncyClick { showAddItemDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ ADD ITEM FROM CATALOG",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimaryBlue,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSlate)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                            .bouncyClick { showBarcodeScanner = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📷", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scan",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Quick Filter Bar for POS catalog products
            if (catalogProducts.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = productQuickFilter,
                        onValueChange = { productQuickFilter = it },
                        placeholder = {
                            Text(
                                "Quick filter products (name or HSN)...",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle, fontSize = 12.sp)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSubtle,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (productQuickFilter.isNotEmpty()) {
                                IconButton(
                                    onClick = { productQuickFilter = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = TextSubtle,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ElevatedSlate,
                            unfocusedContainerColor = ElevatedSlate,
                            focusedBorderColor = BrandPrimaryBlue,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedTextColor = DarkNavy,
                            unfocusedTextColor = DarkNavy
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
            }

            // Horizontal 1-tap POS Presets chips from real catalogProducts + "+ Add Product" shortcut
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shortcut 1: ✨ + Create New Item (Bypasses directly to Add Product prompt)
                    item(key = "add_new_shortcut") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BrandPrimaryBlue.copy(alpha = 0.12f))
                                .border(1.dp, BrandPrimaryBlue, RoundedCornerShape(16.dp))
                                .bouncyClick { showDirectAddProductPrompt = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Create New Item",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Catalog products as quick 1-tap chips
                    items(filteredCatalogProducts, key = { it.id }) { product ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(16.dp))
                                .bouncyClick {
                                    viewModel.addLineItem(
                                        LineItem(
                                            itemId = System.currentTimeMillis(),
                                            sku = product.hsnCode.orEmpty(),
                                            title = product.name,
                                            unitPrice = product.defaultPrice,
                                            quantity = 1,
                                            gstRatePercent = product.gstRate
                                        )
                                    )
                                    Toast.makeText(context, "+ ${product.name} added", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ ${product.name.take(18)} ${CurrencyFormatter.formatCurrency(product.defaultPrice)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // ─── 4. ITEM LIST OR INNOVATIVE EMPTY STATE ───
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ITEMS / SERVICES (${lineItems.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                    if (lineItems.isNotEmpty()) {
                        Text(
                            text = "+ Add More",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BrandPrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            modifier = Modifier
                                .clickable { showAddItemDialog = true }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (lineItems.isEmpty()) {
                item {
                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(BrandAccentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Your Bill Cart is Clean & Empty",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    fontSize = 14.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "Pick products from store catalog chips or scan barcode to add",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSubtle,
                                    fontSize = 11.5.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dual Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSlate)
                                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                        .clickable { showBarcodeScanner = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📷", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Scan Barcode",
                                            color = DarkNavy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandAccentSoft)
                                        .border(1.dp, BrandPrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { showAddItemDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = BrandPrimaryBlue,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Add Item",
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = lineItems,
                    key = { _, item -> item.itemId },
                    contentType = { _, _ -> "line_item" }
                ) { index, item ->
                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // ─── TIER 1: Title (flex) + GST Tag (fixed) + Line Total (right) ───
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 13.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusPill(
                                        text = "GST ${item.gstRatePercent.toInt()}%",
                                        textColor = BrandPrimaryBlue,
                                        backgroundColor = BrandAccentSoft
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = CurrencyFormatter.formatCurrencyExact(item.total),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DarkNavy,
                                        fontSize = 14.5.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = BorderStrokeDark.copy(alpha = 0.5f), thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // ─── TIER 2: Unit details (left) + Stepper & Remove (right) ───
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${item.quantity} × ${CurrencyFormatter.formatCurrencyExact(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    if (item.sku.isNotBlank()) {
                                        Text("•", color = TextSubtle, fontSize = 11.sp)
                                        Text(
                                            text = "HSN: ${item.sku}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSubtle,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    QuantityStepper(
                                        quantity = item.quantity.toInt().coerceAtLeast(1),
                                        onQuantityChange = { newQty ->
                                            viewModel.updateItemQuantity(index, newQty)
                                        },
                                        min = 1
                                    )

                                    IconButton(
                                        onClick = { viewModel.removeLineItem(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = DangerRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── 5. 2-COLUMN BILLING & PAYMENT SUMMARY CARD ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCardSurface)
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Billing Summary
                    Column(modifier = Modifier.weight(1.15f)) {
                        Text(
                            text = "BILLING SUMMARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = TextMuted, fontSize = 11.sp)
                            Text(CurrencyFormatter.formatCurrencyExact(taxCalc.subtotal), color = DarkNavy, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CGST+SGST", color = TextMuted, fontSize = 11.sp)
                            Text(CurrencyFormatter.formatCurrencyExact(taxCalc.cgst + taxCalc.sgst), color = DarkNavy, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }

                        if (taxCalc.roundOff != 0.0) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Round-Off", color = TextSubtle, fontSize = 11.sp)
                                Text(CurrencyFormatter.formatCurrencyExact(taxCalc.roundOff), color = TextSubtle, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = BorderStrokeDark)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grand Total",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkNavy,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrencyExact(taxCalc.grandTotal),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldJama,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Divider between columns
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(95.dp)
                            .background(BorderStrokeDark)
                    )

                    // Right Column: Payment & Method
                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PAYMENT & METHOD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 3-Way Segmented Control: [PAID] / [PARTIAL] / [UNPAID]
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Status:", fontSize = 11.sp, color = DarkNavy, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // PAID
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedStatus == InvoiceStatus.PAID) Color(0xFFDCFCE7) else Color.Transparent)
                                        .border(1.dp, if (selectedStatus == InvoiceStatus.PAID) Color(0xFF86EFAC) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setStatus(InvoiceStatus.PAID) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PAID",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedStatus == InvoiceStatus.PAID) EmeraldJama else Color(0xFF64748B)
                                    )
                                }
                                // PARTIAL
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedStatus == InvoiceStatus.PARTIAL) Color(0xFFE0F2FE) else Color.Transparent)
                                        .border(1.dp, if (selectedStatus == InvoiceStatus.PARTIAL) Color(0xFF7DD3FC) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.setStatus(InvoiceStatus.PARTIAL)
                                            if (paidAmount.isBlank() || (paidAmount.toDoubleOrNull() ?: 0.0) <= 0.0) {
                                                val half = (taxCalc.grandTotal / 2.0).toInt()
                                                if (half > 0) viewModel.setPaidAmount(half.toString())
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PARTIAL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedStatus == InvoiceStatus.PARTIAL) BrandPrimaryBlue else Color(0xFF64748B)
                                    )
                                }
                                // UNPAID
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedStatus == InvoiceStatus.UNPAID) Color(0xFFFEE2E2) else Color.Transparent)
                                        .border(1.dp, if (selectedStatus == InvoiceStatus.UNPAID) Color(0xFFFECACA) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setStatus(InvoiceStatus.UNPAID) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "UNPAID",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedStatus == InvoiceStatus.UNPAID) DangerRed else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // When PARTIAL: Show upfront advance input and live balance breakdown
                        if (selectedStatus == InvoiceStatus.PARTIAL) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val parsedPaid = paidAmount.toDoubleOrNull() ?: 0.0
                            val balDue = maxOf(0.0, taxCalc.grandTotal - parsedPaid)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF0F9FF))
                                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Paid Advance:", fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.25, 0.5, 0.75).forEach { ratio ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White)
                                                    .border(0.5.dp, Color(0xFF93C5FD), RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        val amt = kotlin.math.round(taxCalc.grandTotal * ratio).toInt()
                                                        viewModel.setPaidAmount(amt.toString())
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("${(ratio * 100).toInt()}%", fontSize = 8.sp, color = BrandPrimaryBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                BasicTextField(
                                    value = paidAmount,
                                    onValueChange = { viewModel.setPaidAmount(it) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy
                                    ),
                                    decorationBox = { innerTextField ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("₹ ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimaryBlue)
                                            if (paidAmount.isBlank()) {
                                                Text("Enter advance amount", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            } else {
                                                innerTextField()
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Received: ₹%.2f".format(parsedPaid), fontSize = 9.sp, color = Color(0xFF0369A1))
                                    Text("Bal Due: ₹%.2f".format(balDue), fontSize = 9.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Toggle for Method: [UPI] / [CASH] / [CREDIT]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Method:", fontSize = 11.sp, color = DarkNavy, fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                    .clickable {
                                        val nextMode = when (selectedPaymentMode) {
                                            PaymentMode.UPI -> PaymentMode.CASH
                                            PaymentMode.CASH -> PaymentMode.BANK
                                            PaymentMode.BANK -> PaymentMode.UPI
                                        }
                                        viewModel.setPaymentMode(nextMode)
                                        if (nextMode == PaymentMode.UPI) showUpiQrDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = selectedPaymentMode.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimaryBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    ChopdaPdfLoadingOverlay(
        visible = isSaving,
        title = if (documentMode == InvoiceViewModel.DocumentMode.QUOTATION) "Generating Quotation PDF..." else "Generating Tax Invoice PDF...",
        subtitle = "Saving ledger entries and crafting document..."
    )
}

    if (showAddItemDialog) {
        AddLineItemDialog(
            stockItems = stockItems,
            catalogProducts = catalogProducts,
            companyId = activeCompany.id,
            onSaveNewProductToCatalog = { newProd ->
                scope.launch { productRepository?.addProduct(newProd) }
            },
            onDismiss = { showAddItemDialog = false },
            onAdd = { newItem ->
                viewModel.addLineItem(newItem)
                showAddItemDialog = false
            }
        )
    }

    if (showDirectAddProductPrompt) {
        AddEditProductDialog(
            companyId = activeCompany.id,
            product = null,
            onDismiss = { showDirectAddProductPrompt = false },
            onSave = { newProd ->
                scope.launch {
                    productRepository?.addProduct(newProd)
                    viewModel.addLineItem(
                        LineItem(
                            itemId = System.currentTimeMillis(),
                            sku = newProd.hsnCode.orEmpty(),
                            title = newProd.name,
                            unitPrice = newProd.defaultPrice,
                            quantity = 1,
                            gstRatePercent = newProd.gstRate
                        )
                    )
                }
                showDirectAddProductPrompt = false
                Toast.makeText(context, "${newProd.name} added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCustomerModal) {
        ChangeCustomerDialog(
            currentName = customerName,
            currentPhone = customerPhone,
            currentGstin = customerGstin,
            existingCustomers = existingCustomers,
            onDismiss = { showCustomerModal = false },
            onSelect = { name, phone, gstin ->
                viewModel.setCustomer(name, phone, gstin)
                showCustomerModal = false
            }
        )
    }

    if (showUpiQrDialog) {
        UpiQrDialog(
            upiVpa = activeCompany.upiVpa,
            firmName = activeCompany.firmName,
            amount = taxCalc.grandTotal,
            invoiceNumber = invoiceNumber,
            onDismiss = { showUpiQrDialog = false },
            onPaymentConfirmed = {
                viewModel.setStatus(InvoiceStatus.PAID)
                viewModel.setPaymentMode(PaymentMode.UPI)
                showUpiQrDialog = false
                Toast.makeText(context, "Payment Received (Paid via UPI)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Thermal Slip Receipt Preview Modal
    if (showThermalPreview) {
        ModalBottomSheet(
            onDismissRequest = { showThermalPreview = false },
            sheetState = thermalSheetState,
            containerColor = MidnightCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sacred Header
                Text(
                    text = "|| 卐 ${strings.thermalReceiptPreviewTitle} 卐 ||",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ImperialGoldLight,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )

                // Parchment Paper Receipt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFAF8F0))
                        .border(1.dp, Color(0xFFE2DEC9), RoundedCornerShape(10.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "|| શ્રી ગણેશાય નમઃ ||",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeCompany.firmName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1A1A1A)
                            )
                        )
                        Text(
                            text = activeCompany.address,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF444444)
                            )
                        )
                        Text(
                            text = "GSTIN: ${activeCompany.gstin}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF222222)
                            )
                        )
                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("BILL: $invoiceNumber", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF222222))
                            Text("MODE: ${selectedPaymentMode.name}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF222222))
                        }
                        Text(
                            text = "CUSTOMER: $customerName",
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888)
                        )

                        // Line items in receipt
                        lineItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.title.take(16)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF111111)
                                )
                                Text(
                                    text = CurrencyFormatter.formatCurrency(item.total),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111)
                                )
                            }
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SUBTOTAL:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF444444))
                            Text("₹ %.2f".format(taxCalc.subtotal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF111111))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CGST+SGST (5%):", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF444444))
                            Text("₹ %.2f".format(taxCalc.cgst + taxCalc.sgst), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF111111))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL AMOUNT:", fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF000000))
                            Text("₹ %.2f".format(taxCalc.grandTotal), fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF000000))
                        }

                        Text(
                            text = "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF888888)
                        )
                        Text(
                            text = "|||||| | |||| ||||||| ||| ||||",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFF222222)
                        )
                        Text(
                            text = "|| શુભ લાભ • આભાર પધારો ||",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF444444)
                            )
                        )
                    }
                }

                // Print Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldFoilGradient)
                        .bouncyClick {
                            scope.launch {
                                printerManager.printInvoiceReceipt(
                                    deviceAddress = "00:11:22:33:44:55",
                                    company = activeCompany,
                                    invoice = viewModel.buildCurrentInvoice()
                                )
                                Toast.makeText(context, "🖨️ ESC/POS Thermal Print Successful!", Toast.LENGTH_SHORT).show()
                                showThermalPreview = false
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = Color(0xFF1E1005),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.printReceiptBtn,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E1005)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { code ->
                val matched = viewModel.addFromBarcode(code)
                android.widget.Toast.makeText(
                    context,
                    if (matched) "📦 Scanned item added: $code" else "Tag scanned: $code",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showBarcodeScanner = false
            }
        )
    }
}

@Composable
fun AddLineItemDialog(
    stockItems: List<InventoryItem> = emptyList(),
    catalogProducts: List<Product> = emptyList(),
    companyId: Long = 0L,
    onSaveNewProductToCatalog: (Product) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (LineItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }
    var gstRate by remember { mutableStateOf(18.0) }
    var hsnCode by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showDirectProductPrompt by remember { mutableStateOf(false) }

    // If user triggered "+ Add new product…", open AddEditProductDialog prompt
    if (showDirectProductPrompt) {
        AddEditProductDialog(
            companyId = companyId,
            product = null,
            onDismiss = { showDirectProductPrompt = false },
            onSave = { newProd ->
                onSaveNewProductToCatalog(newProd)
                selectedProduct = newProd
                title = newProd.name
                priceStr = if (newProd.defaultPrice % 1.0 == 0.0) newProd.defaultPrice.toInt().toString() else newProd.defaultPrice.toString()
                gstRate = newProd.gstRate
                hsnCode = newProd.hsnCode.orEmpty()
                showDirectProductPrompt = false
            }
        )
    }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Add Product / Line Item",
        subtitle = "Itemized bill charge",
        icon = Icons.Default.ShoppingBag,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.94f,
        maxHeightFraction = 0.88f,
        scrollable = true,
        isBottomSheet = true,
        buttons = {
            CoralGradientButton(
                text = "ADD ITEM",
                enabled = title.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) >= 0.0,
                onClick = {
                    onAdd(
                        LineItem(
                            itemId = System.currentTimeMillis(),
                            sku = hsnCode,
                            title = title.trim(),
                            unitPrice = priceStr.toDoubleOrNull() ?: 0.0,
                            quantity = qtyStr.toIntOrNull() ?: 1,
                            gstRatePercent = gstRate
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // Product Dropdown Selector (Matching Web frontend dropdown behavior)
                Text(
                    text = "Select Product",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandPrimaryBlueLight,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSlate)
                            .border(
                                width = 1.dp,
                                color = if (dropdownExpanded) BrandPrimaryBlue else BorderStrokeLight,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = if (selectedProduct != null) BrandPrimaryBlue else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedProduct != null) {
                                        "${selectedProduct!!.name} (${CurrencyFormatter.formatCurrency(selectedProduct!!.defaultPrice)})"
                                    } else if (title.isNotBlank()) {
                                        title
                                    } else {
                                        "Select product from catalog…"
                                    },
                                    color = if (selectedProduct != null || title.isNotBlank()) TextWhite else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedProduct != null) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle dropdown",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Dropdown menu showing all products + "+ Add new product…" option
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                            .widthIn(min = 280.dp, max = 340.dp)
                            .heightIn(max = 360.dp)
                    ) {
                        // Option 1: + Add new product… (Bypasses to AddEditProductDialog prompt)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = BrandPrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+ Add new product…",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                showDirectProductPrompt = true
                            }
                        )

                        HorizontalDivider(color = BorderStrokeDark, thickness = 1.dp)

                        // Option 2..N: List of all products from catalog
                        if (catalogProducts.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "No products yet. Tap above to add one.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    showDirectProductPrompt = true
                                }
                            )
                        } else {
                            catalogProducts.forEach { prod ->
                                val isSelected = selectedProduct?.id == prod.id || title == prod.name
                                DropdownMenuItem(
                                    text = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = prod.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = if (isSelected) BrandPrimaryBlue else TextWhite,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = CurrencyFormatter.formatCurrency(prod.defaultPrice),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = ImperialGoldLight,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "GST ${prod.gstRate.toInt()}% • ${prod.unit}${prod.hsnCode?.let { " • HSN $it" } ?: ""}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = prod
                                        title = prod.name
                                        priceStr = if (prod.defaultPrice % 1.0 == 0.0) prod.defaultPrice.toInt().toString() else prod.defaultPrice.toString()
                                        gstRate = prod.gstRate
                                        hsnCode = prod.hsnCode.orEmpty()
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Item description / Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Product / Description", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Rate & Qty row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Rate (₹)", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ElevatedSlate,
                            unfocusedContainerColor = ElevatedSlate,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Qty", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ElevatedSlate,
                            unfocusedContainerColor = ElevatedSlate,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // HSN Code
                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = { hsnCode = it },
                    label = { Text("HSN / SAC Code (Optional)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // GST Rate selector chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GST Rate (${gstRate.toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                            val isSelected = gstRate == rate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CoralPrimary else ElevatedSlate)
                                    .border(1.dp, if (isSelected) CoralPrimary else BorderStrokeLight, RoundedCornerShape(6.dp))
                                    .clickable { gstRate = rate }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${rate.toInt()}%",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else TextWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
fun ChangeCustomerDialog(
    currentName: String,
    currentPhone: String,
    currentGstin: String = "",
    existingCustomers: List<CustomerEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSelect: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var gstin by remember { mutableStateOf(currentGstin) }
    var searchCustomerQuery by remember { mutableStateOf("") }

    val matchingCustomers = remember(searchCustomerQuery, existingCustomers) {
        if (searchCustomerQuery.isBlank()) existingCustomers.take(4)
        else existingCustomers.filter {
            it.name.contains(searchCustomerQuery, ignoreCase = true) ||
            it.phone.contains(searchCustomerQuery)
        }
    }

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIdx != -1) {
                            val pickedName = it.getString(nameIdx)
                            if (!pickedName.isNullOrBlank()) name = pickedName
                        }
                        val hasPhoneIdx = it.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIdx != -1) it.getInt(hasPhoneIdx) else 1
                        val idIdx = it.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                        if (hasPhone > 0 && idIdx != -1) {
                            val contactId = it.getString(idIdx)
                            val pCursor = context.contentResolver.query(
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            pCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numIdx = pc.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIdx != -1) {
                                        val rawNumber = pc.getString(numIdx)
                                        if (!rawNumber.isNullOrBlank()) {
                                            phone = rawNumber.replace("[^0-9+]".toRegex(), "")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not load contact: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Customer Details",
        subtitle = "Assign buyer & contact info",
        icon = Icons.Default.PersonOutline,
        widthFraction = 0.94f,
        maxHeightFraction = 0.88f,
        scrollable = true,
        isBottomSheet = true,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.weight(1.5f)) {
                    CoralGradientButton(
                        text = "SET CUSTOMER",
                        enabled = name.isNotBlank(),
                        onClick = { onSelect(name, phone, gstin) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (existingCustomers.isNotEmpty()) {
                var customerDropdownOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 Pick Saved Customer:",
                        style = MaterialTheme.typography.labelSmall.copy(color = ImperialGoldLight, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (customerDropdownOpen) "Close list ▲" else "Choose from Dropdown ▼",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrandPrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        modifier = Modifier.clickable { customerDropdownOpen = !customerDropdownOpen }
                    )
                }

                if (customerDropdownOpen) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchCustomerQuery,
                                onValueChange = { searchCustomerQuery = it },
                                placeholder = { Text("Filter saved customers...", fontSize = 11.sp, color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ElevatedSlate,
                                    unfocusedContainerColor = ElevatedSlate,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val filtered = existingCustomers.filter {
                                searchCustomerQuery.isBlank() ||
                                it.name.contains(searchCustomerQuery, ignoreCase = true) ||
                                it.phone.contains(searchCustomerQuery)
                            }
                            filtered.take(6).forEach { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            name = c.name
                                            phone = c.phone
                                            gstin = c.gstin
                                            customerDropdownOpen = false
                                            searchCustomerQuery = ""
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(c.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        if (c.phone.isNotBlank()) {
                                            Text(c.phone, color = TextMuted, fontSize = 10.sp)
                                        }
                                    }
                                    if (name.equals(c.name, ignoreCase = true)) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldJama, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingCustomers, key = { it.id }, contentType = { "customer_pick" }) { c ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSlate)
                                    .border(1.dp, ImperialGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        name = c.name
                                        phone = c.phone
                                        gstin = c.gstin
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = c.name,
                                    fontSize = 11.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                trailingIcon = {
                    IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Pick Contact from Phonebook",
                            tint = ImperialGoldLight
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Quick Import from Phone Contacts Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSlate)
                    .clickable { contactPickerLauncher.launch(null) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    tint = ImperialGoldLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Import Party from Phone Contacts 📱",
                    color = ImperialGoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedTextField(
                value = gstin,
                onValueChange = { gstin = it.uppercase() },
                label = { Text("GSTIN (Optional, e.g. 24...)", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
