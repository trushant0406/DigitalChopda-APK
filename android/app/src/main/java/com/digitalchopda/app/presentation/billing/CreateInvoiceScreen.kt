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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
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
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.domain.usecase.CreateInvoiceUseCase
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.collectAsState
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
    onNavigateBack: () -> Unit,
    viewModel: InvoiceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = InvoiceViewModel.Factory(
            billingRepository,
            companyRepository,
            inventoryRepository,
            customerRepository
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    val activeCompany by viewModel.activeCompany.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val customerGstin by viewModel.customerGstin.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedPaymentMode by viewModel.selectedPaymentMode.collectAsState()
    val lineItems by viewModel.lineItems.collectAsState()
    val taxCalc by viewModel.taxCalculation.collectAsState()
    val stockItems by viewModel.stockItems.collectAsState()
    val existingCustomers by viewModel.matchingCustomers.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showCustomerModal by remember { mutableStateOf(false) }
    var showThermalPreview by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }
    val thermalSheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "|| ${strings.billingTitle} ||",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = ImperialGoldLight,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "${activeCompany.firmName} • GSTIN: ${activeCompany.gstin}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ImperialGoldLight
                        )
                    }
                },
                actions = {
                    // Dynamic Counter UPI QR Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3F5EFB).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF3F5EFB).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .bouncyClick { showUpiQrDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "UPI QR",
                                tint = Color(0xFF3F5EFB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UPI QR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3F5EFB)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Thermal Slip Receipt Preview Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ImperialGold.copy(alpha = 0.15f))
                            .border(1.dp, ImperialGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .bouncyClick { showThermalPreview = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Thermal Receipt",
                                tint = ImperialGoldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.thermalSlipBtn,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImperialGoldLight
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
        },
        bottomBar = {
            // Sticky Bottom Footer
            Surface(
                color = MidnightCardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderStrokeDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSlate)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .bouncyClick {
                                Toast.makeText(context, "ઇન્વોઇસ ડ્રાફ્ટ સાચવ્યો", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.saveDraftBtn, color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldFoilGradient)
                            .bouncyClick {
                                viewModel.saveInvoice(
                                    pdfGenerator = pdfGenerator,
                                    onSuccess = { invoice, pdfFile, uri ->
                                        Toast.makeText(context, "ઇન્વોઇસ ${invoice.invoiceNumber} તૈયાર!", Toast.LENGTH_SHORT).show()

                                        // WhatsApp Dual-Path Delivery (Zero Cost Native Intent)
                                        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "નમસ્તે ${invoice.customerName},\n${activeCompany.firmName} તરફથી આપનું કર ઇન્વોઇસ ${invoice.invoiceNumber} (₹${invoice.grandTotal.toInt()}) મોકલેલ છે.\nઆભાર!\n${activeCompany.ownerName}"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            setPackage("com.whatsapp")
                                        }

                                        try {
                                            context.startActivity(whatsappIntent)
                                        } catch (_: Exception) {
                                            val chooser = Intent.createChooser(whatsappIntent, "Share Invoice via WhatsApp")
                                            context.startActivity(chooser)
                                        }

                                        onNavigateBack()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "ભૂલ: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💬", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.saveAndSendBtn,
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Invoice Header: Rule 46 Auto-incremented Invoice Number
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.invoiceNumberRule46,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = invoiceNumber,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = ImperialGoldLight,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DATE",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Customer Selector Card
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.billToCustomer,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = strings.changeCustomer,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ImperialGoldLight,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { showCustomerModal = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElevatedSlate)
                                .border(1.dp, ImperialGold.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ImperialGoldLight
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = customerPhone,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }
                }
            }

            // Quick POS Presets Bar (1-Tap counter entry)
            item {
                Column {
                    Text(
                        text = "⚡ ${strings.quickPresetsLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImperialGoldLight,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = listOf(
                            Triple(strings.preset1, 1850.0, 5.0),
                            Triple(strings.preset2, 4200.0, 5.0),
                            Triple(strings.preset3, 650.0, 5.0),
                            Triple(strings.preset4, 1200.0, 5.0)
                        )
                        items(presets) { (title, price, gst) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ElevatedSlate)
                                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(20.dp))
                                    .bouncyClick {
                                        viewModel.addLineItem(
                                            LineItem(
                                                itemId = System.currentTimeMillis(),
                                                sku = "QUICK-${System.currentTimeMillis() % 1000}",
                                                title = title,
                                                unitPrice = price,
                                                quantity = 1,
                                                gstRatePercent = gst
                                            )
                                        )
                                        Toast.makeText(context, "+ $title ઉમેરાઈ", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "+ $title (₹${price.toInt()})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ImperialGoldLight
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Line Items List
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
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            itemsIndexed(lineItems) { index, item ->
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹ ${item.unitPrice.toInt()} / unit",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPill(
                                    text = "GST ${item.gstRatePercent.toInt()}%",
                                    textColor = CoralPrimary,
                                    backgroundColor = CoralPrimary.copy(alpha = 0.15f)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.removeLineItem(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = DangerRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuantityStepper(
                            quantity = item.quantity,
                            onQuantityChange = { newQ ->
                                viewModel.updateItemQuantity(index, newQ)
                            }
                        )

                        Text(
                            text = "₹ ${item.total.toInt()}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                        )
                    }
                }
            }

            // + Add Items / Services Dashed Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCardSurface)
                        .border(1.dp, ImperialGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .bouncyClick { showAddItemDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ImperialGoldLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.addItemsServices,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImperialGoldLight
                            )
                        )
                    }
                }
            }

            // Billing Summary Card (GST breakdown, Round-off, Grand Total)
            item {
                ChopdaCard {
                    Text(
                        text = "BILLING SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = TextMuted)
                        Text("₹ %.2f".format(taxCalc.subtotal), color = TextWhite)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CGST (2.5%)", color = TextMuted)
                        Text("₹ %.2f".format(taxCalc.cgst), color = TextWhite)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SGST (2.5%)", color = TextMuted)
                        Text("₹ %.2f".format(taxCalc.sgst), color = TextWhite)
                    }

                    if (taxCalc.roundOff != 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Auto Round-Off", color = TextSubtle)
                            Text("₹ %.2f".format(taxCalc.roundOff), color = TextSubtle)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = BorderStrokeDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAND TOTAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "₹ %.2f".format(taxCalc.grandTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldJama
                            )
                        )
                    }
                }
            }

            // Payment Status & Mode Selectors
            item {
                ChopdaCard {
                    Text(
                        text = "PAYMENT & STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InvoiceStatus.entries.forEach { status ->
                            val isSelected = selectedStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ImperialGold else ElevatedSlate)
                                    .bouncyClick { viewModel.setStatus(status) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color(0xFF1E1005) else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMode.entries.forEach { mode ->
                            val isSelected = selectedPaymentMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ImperialGold else ElevatedSlate)
                                    .bouncyClick {
                                        viewModel.setPaymentMode(mode)
                                        if (mode == PaymentMode.UPI) {
                                            showUpiQrDialog = true
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color(0xFF1E1005) else TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddLineItemDialog(
            stockItems = stockItems,
            onDismiss = { showAddItemDialog = false },
            onAdd = { newItem ->
                viewModel.addLineItem(newItem)
                showAddItemDialog = false
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
                Toast.makeText(context, "ચુકવણી સ્વીકારાઈ (Paid via UPI)", Toast.LENGTH_SHORT).show()
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
                                    text = "₹${item.total.toInt()}",
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
                                Toast.makeText(context, "🖨️ ESC/POS થર્મલ પ્રિન્ટ સફળ!", Toast.LENGTH_SHORT).show()
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
}

@Composable
fun AddLineItemDialog(
    stockItems: List<InventoryItem> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (LineItem) -> Unit
) {
    var searchCatalogQuery by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("1000") }
    var qtyStr by remember { mutableStateOf("1") }
    var gstRate by remember { mutableStateOf(5.0) }

    val matchingStock = remember(searchCatalogQuery, stockItems) {
        if (searchCatalogQuery.isBlank()) stockItems.take(4)
        else stockItems.filter {
            it.itemName.contains(searchCatalogQuery, ignoreCase = true) ||
            it.sku.contains(searchCatalogQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = { Text("Add Line Item", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick Catalog Autocomplete
                if (stockItems.isNotEmpty()) {
                    Text(
                        text = "📦 Quick Catalog Autocomplete:",
                        style = MaterialTheme.typography.labelSmall.copy(color = ImperialGoldLight, fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = searchCatalogQuery,
                        onValueChange = { searchCatalogQuery = it },
                        placeholder = { Text("Search stock items...", color = TextMuted) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = ImperialGoldLight, modifier = Modifier.size(18.dp))
                        },
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingStock) { item ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSlate)
                                    .border(1.dp, ImperialGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        title = item.itemName
                                        priceStr = item.sellingPrice.toInt().toString()
                                        gstRate = item.gstRatePercent
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${item.itemName.take(18)} (₹${item.sellingPrice.toInt()})",
                                    fontSize = 11.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Divider(color = BorderStrokeDark, thickness = 0.8.dp)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item / Description", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)", color = TextMuted) },
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
            }
        },
        confirmButton = {
            CoralGradientButton(
                text = "ADD ITEM",
                enabled = title.isNotBlank(),
                onClick = {
                    onAdd(
                        LineItem(
                            title = title,
                            unitPrice = priceStr.toDoubleOrNull() ?: 100.0,
                            quantity = qtyStr.toIntOrNull() ?: 1,
                            gstRatePercent = gstRate
                        )
                    )
                }
            )
        },
        dismissButton = {
            Text("CANCEL", color = TextMuted, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        }
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = { Text("Customer Details", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existingCustomers.isNotEmpty()) {
                    Text(
                        text = "👥 Pick Saved Customer:",
                        style = MaterialTheme.typography.labelSmall.copy(color = ImperialGoldLight, fontWeight = FontWeight.Bold)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingCustomers) { c ->
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
                    Divider(color = BorderStrokeDark, thickness = 0.8.dp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name", color = TextMuted) },
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
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("GSTIN (Optional, e.g. 24...)", color = TextMuted) },
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
        },
        confirmButton = {
            CoralGradientButton(
                text = "SET CUSTOMER",
                enabled = name.isNotBlank(),
                onClick = { onSelect(name, phone, gstin) }
            )
        },
        dismissButton = {
            Text("CANCEL", color = TextMuted, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        }
    )
}
