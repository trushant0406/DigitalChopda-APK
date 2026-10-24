package com.digitalchopda.app.presentation.party

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.QuotationRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Party360Detail
import com.digitalchopda.app.domain.model.PartyLedgerEntry
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(
    partyId: Long,
    initialTab: Int = 0,
    companyRepository: CompanyRepository,
    customerRepository: CustomerRepository,
    billingRepository: BillingRepository,
    quotationRepository: QuotationRepository,
    purchaseRepository: com.digitalchopda.app.data.repository.PurchaseRepository? = null,
    languageManager: LanguageManager? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCreateInvoice: (customerName: String, customerPhone: String, partyId: Long) -> Unit = { _, _, _ -> },
    onNavigateToDocumentDetail: (docType: String, docId: Long) -> Unit = { _, _ -> },
    onNavigateToPdfViewer: (filePath: String, invoiceId: Long, customerName: String, customerPhone: String, invoiceNumber: String) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

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

    LaunchedEffect(Unit) {
        activeCompany = companyRepository.getActiveCompany()
    }

    val partyDetail by customerRepository.getParty360Stream(activeCompany.id, partyId)
        .collectAsState(initial = null)

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var ledgerFilter by remember { mutableStateOf("ALL") } // ALL, INVOICES, PAYMENTS

    var showPaymentInDialog by remember { mutableStateOf(false) }
    var showPaymentOutDialog by remember { mutableStateOf(false) }
    var showRecordPurchaseDialog by remember { mutableStateOf(false) }
    var invoiceForPayment by remember { mutableStateOf<com.digitalchopda.app.data.local.entity.InvoiceEntity?>(null) }
    var showPaymentReminderDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = partyDetail?.customer?.companyName?.ifBlank { partyDetail?.customer?.name } ?: "Party Profile",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                fontSize = 16.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = partyDetail?.customer?.phone ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSubtle,
                                fontSize = 11.5.sp
                            )
                        )
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
                    partyDetail?.customer?.let { customer ->
                        // Call Action
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${customer.phone}")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = EmeraldJama,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // WhatsApp Action
                        IconButton(
                            onClick = {
                                showPaymentReminderDialog = true
                            }
                        ) {
                            Text("💬", fontSize = 18.sp)
                        }

                        // Inward Purchase Action (Stock & Cargo Intake)
                        if (purchaseRepository != null) {
                            IconButton(
                                onClick = { showRecordPurchaseDialog = true }
                            ) {
                                Text("📦", fontSize = 18.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightCardSurface)
            )
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
        },
        bottomBar = {
            // Fixed Bottom Action Dock
            Surface(
                color = MidnightCardSurface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + Receive Payment (Credit)
                    Button(
                        onClick = { showPaymentInDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldJama)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Receive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    // - Give Credit (Debit)
                    Button(
                        onClick = { showPaymentOutDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RubyUdhar)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "- Give",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    // + Create Bill
                    Button(
                        onClick = {
                            partyDetail?.customer?.let { c ->
                                onNavigateToCreateInvoice(c.name, c.phone, c.id)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ New Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (partyDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading Party 360° Data...", color = TextMuted)
            }
        } else {
            val detail = partyDetail!!
            val customer = detail.customer

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cockpit Card: Balance & Party Identifiers
                item {
                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (avatarBg, avatarText) = remember(customer.name) { getAvatarColors(customer.name) }
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(avatarBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = avatarText,
                                                fontSize = 18.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = DarkNavy,
                                                fontSize = 16.sp
                                            )
                                        )
                                        if (customer.companyName.isNotBlank() && customer.companyName != customer.name) {
                                            Text(
                                                text = customer.companyName,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSubtle,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                        if (customer.gstin.isNotBlank()) {
                                            Text(
                                                text = "GSTIN: ${customer.gstin}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = BrandPrimaryBlue,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                // Lead Priority Pill
                                StatusPill(
                                    text = customer.leadPriority,
                                    textColor = when (customer.leadPriority) {
                                        "HOT" -> RubyUdhar
                                        "WARM" -> BrandPrimaryBlue
                                        "CONVERTED" -> EmeraldJama
                                        else -> TextSubtle
                                    },
                                    backgroundColor = when (customer.leadPriority) {
                                        "HOT" -> RubyUdhar.copy(alpha = 0.12f)
                                        "WARM" -> BrandPrimaryBlue.copy(alpha = 0.12f)
                                        "CONVERTED" -> EmeraldJama.copy(alpha = 0.12f)
                                        else -> ElevatedSlate
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Running Balance Bar
                            val netBal = if (detail.netOutstanding != 0.0) detail.netOutstanding else customer.balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (netBal > 0) "BALANCED ${CurrencyFormatter.formatCurrency(Math.abs(netBal))} Due" else if (netBal < 0) "YOU'LL GIVE" else "SETTLED ₹ 0",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (netBal > 0) EmeraldJama else if (netBal < 0) RubyUdhar else TextSubtle,
                                            letterSpacing = 0.8.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(Math.abs(netBal)),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (netBal > 0) EmeraldJama else if (netBal < 0) RubyUdhar else DarkNavy,
                                            fontSize = 24.sp
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "TOTAL BILLED: ${CurrencyFormatter.formatCurrency(detail.totalBilled)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "COLLECTED: ${CurrencyFormatter.formatCurrency(detail.totalCollected)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldJama,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    if (customer.creditLimit > 0) {
                                        Text(
                                            text = "LIMIT: ${CurrencyFormatter.formatCurrency(customer.creditLimit)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSubtle,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4-Tab Capsule Swiper [Ledger | Invoices | Quotations | Lead & Profile]
                item {
                    val tabs = listOf(
                        "Passbook (${detail.ledgerEntries.size})",
                        "Invoices (${detail.invoices.size})",
                        "Quotations (${detail.quotations.size})",
                        "Profile & Details"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSlate)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.forEachIndexed { index, tabTitle ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BrandPrimaryBlue else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else DarkNavy,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // TAB 0: KHATA LEDGER
                if (selectedTab == 0) {
                    item {
                        // Filter Pills: All, Bills Only, Payments Only
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSACTION LEDGER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                )
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ALL" to "All", "INVOICES" to "Invoices", "PAYMENTS" to "Payments").forEach { (key, label) ->
                                    val isSel = ledgerFilter == key
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) BrandAccentSoft else ElevatedSlate)
                                            .clickable { ledgerFilter = key }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) BrandPrimaryBlue else TextSubtle,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val filteredEntries = detail.ledgerEntries.filter { entry ->
                        when (ledgerFilter) {
                            "INVOICES" -> entry.voucherType == "Sales Invoices"
                            "PAYMENTS" -> entry.voucherType.startsWith("Payment")
                            else -> true
                        }
                    }

                    if (filteredEntries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transactions recorded yet", color = TextMuted)
                            }
                        }
                    } else {
                        items(
                            items = filteredEntries,
                            key = { it.id }
                        ) { entry ->
                            LedgerRowCard(entry = entry, dateFormat = shortDateFormat)
                        }
                    }
                }

                // TAB 1: INVOICES & BILLS
                if (selectedTab == 1) {
                    if (detail.invoices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No bills generated yet", fontWeight = FontWeight.Bold, color = DarkNavy)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("No bills generated for this party yet", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(
                            items = detail.invoices,
                            key = { it.id }
                        ) { inv ->
                            PartyInvoiceCard(
                                invoice = inv,
                                onPreviewClick = {
                                    onNavigateToDocumentDetail("invoice", inv.id)
                                },
                                onPrintClick = {
                                    Toast.makeText(context, "Printing Invoice ${inv.invoiceNumber} via Bluetooth", Toast.LENGTH_SHORT).show()
                                },
                                onReceivePaymentClick = {
                                    invoiceForPayment = inv
                                    showPaymentInDialog = true
                                }
                            )
                        }
                    }
                }

                // TAB 2: QUOTATIONS / ESTIMATES
                if (selectedTab == 2) {
                    if (detail.quotations.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No quotations issued yet", fontWeight = FontWeight.Bold, color = DarkNavy)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("No quotations issued for this party yet", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(
                            items = detail.quotations,
                            key = { it.id }
                        ) { quote ->
                            PartyQuotationCard(
                                quotation = quote,
                                onConvertClick = {
                                    onNavigateToCreateInvoice(quote.customerName, quote.customerPhone, partyId)
                                },
                                onClick = {
                                    onNavigateToDocumentDetail("quotation", quote.id)
                                }
                            )
                        }
                    }
                }

                // TAB 3: CONTACT & LEAD PROFILE
                if (selectedTab == 3) {
                    item {
                        ChopdaCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            borderColor = BorderStrokeDark,
                            backgroundColor = MidnightCardSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LEAD PIPELINE STAGE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextSubtle,
                                            letterSpacing = 0.8.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                // 4 Stage Buttons: Hot, Warm, Cold, Converted
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("HOT" to "🔥 Hot", "WARM" to "⚡ Warm", "COLD" to "❄️ Cold", "CONVERTED" to "✅ Won").forEach { (stage, label) ->
                                        val isSel = customer.leadPriority == stage
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) BrandPrimaryBlue else ElevatedSlate)
                                                .clickable {
                                                    scope.launch {
                                                        customerRepository.updateLeadInfo(
                                                            customer.id,
                                                            stage,
                                                            customer.expectedDealValue,
                                                            customer.leadNotes,
                                                            customer.nextFollowUpDate
                                                        )
                                                        Toast.makeText(context, "Stage updated to $stage", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSel) Color.White else DarkNavy,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)

                                // Lead Fields
                                PartyDetailField(label = "Expected Deal Value", value = "₹ ${customer.expectedDealValue.toInt()}")
                                if (customer.leadNotes.isNotBlank()) {
                                    PartyDetailField(label = "Requirements & Notes", value = customer.leadNotes)
                                }
                                PartyDetailField(label = "Firm / Company Name", value = customer.companyName.ifBlank { customer.name })
                                PartyDetailField(label = "GSTIN", value = customer.gstin.ifBlank { "Not Registered" })
                                PartyDetailField(label = "PAN", value = customer.pan.ifBlank { "—" })
                                PartyDetailField(label = "City / State", value = "${customer.city.ifBlank { "Surat" }}, ${customer.state.ifBlank { "Gujarat" }}")
                                PartyDetailField(label = "Address", value = customer.address.ifBlank { "—" })
                                PartyDetailField(label = "Credit Limit", value = if (customer.creditLimit > 0) "₹ ${customer.creditLimit.toInt()}" else "No Limit")
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Record Payment In (જમા)
    if (showPaymentInDialog) {
        val unpaidInvoices = partyDetail?.invoices?.filter { !it.status.equals("PAID", ignoreCase = true) } ?: emptyList()
        RecordPaymentDialog(
            isJama = true,
            partyName = partyDetail?.customer?.name ?: "Customer",
            unpaidInvoices = unpaidInvoices,
            initialInvoiceId = invoiceForPayment?.id,
            onDismiss = {
                showPaymentInDialog = false
                invoiceForPayment = null
            },
            onConfirm = { amount, mode, notes, selectedInvoice ->
                scope.launch {
                    customerRepository.recordPayment(
                        companyId = activeCompany.id,
                        customerId = partyId,
                        amount = amount,
                        isJama = true,
                        paymentMode = mode,
                        notes = notes,
                        invoiceId = selectedInvoice?.id,
                        orderId = selectedInvoice?.serverOrderId,
                        invoiceNumber = selectedInvoice?.invoiceNumber
                    )
                    showPaymentInDialog = false
                    invoiceForPayment = null
                    val againstText = if (selectedInvoice != null) " against ${selectedInvoice.invoiceNumber}" else ""
                    Toast.makeText(context, "✅ Recorded + ${CurrencyFormatter.formatCurrency(amount)}$againstText Payment In", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Modal Dialog: Record Payment Out (Supplier Payment / Debt Settlement)
    if (showPaymentOutDialog) {
        RecordPaymentOutDialog(
            companyId = activeCompany.id,
            partyId = partyId,
            partyName = partyDetail?.customer?.companyName?.ifBlank { partyDetail?.customer?.name } ?: partyDetail?.customer?.name ?: "Supplier",
            currentBalanceDue = Math.abs(partyDetail?.netOutstanding ?: 0.0),
            customerRepository = customerRepository,
            onDismiss = { showPaymentOutDialog = false },
            onPaymentRecorded = {
                showPaymentOutDialog = false
            }
        )
    }

    // Modal Dialog: Record Inward Purchase (Cargo / Stock Intake)
    if (showRecordPurchaseDialog && purchaseRepository != null) {
        RecordPurchaseDialog(
            companyId = activeCompany.id,
            supplierId = partyId,
            supplierName = partyDetail?.customer?.companyName?.ifBlank { partyDetail?.customer?.name ?: "" } ?: partyDetail?.customer?.name ?: "Supplier",
            purchaseRepository = purchaseRepository,
            onDismiss = { showRecordPurchaseDialog = false },
            onPurchaseRecorded = {
                showRecordPurchaseDialog = false
            }
        )
    }

    // Modal Dialog: WhatsApp Payment Reminder with Direct NPCI UPI Link
    if (showPaymentReminderDialog) {
        val cust = partyDetail?.customer
        val netBal = partyDetail?.netOutstanding ?: 0.0
        PaymentReminderDialog(
            partyName = cust?.companyName?.ifBlank { cust.name } ?: cust?.name ?: "Customer",
            partyPhone = cust?.phone ?: "",
            outstandingAmount = netBal,
            activeCompany = activeCompany,
            onDismiss = { showPaymentReminderDialog = false }
        )
    }
}

@Composable
private fun LedgerRowCard(
    entry: PartyLedgerEntry,
    dateFormat: SimpleDateFormat
) {
    val isCredit = entry.credit > 0
    ChopdaCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.voucherType,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCredit) EmeraldJama else if (entry.debit > 0) RubyUdhar else DarkNavy,
                            fontSize = 13.5.sp
                        )
                    )
                    if (entry.refNumber.isNotBlank()) {
                        Text(
                            text = "• ${entry.refNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSubtle,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.particulars,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DarkNavy,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(entry.date)),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (entry.debit > 0) {
                    Text(
                        text = "- ${CurrencyFormatter.formatCurrency(entry.debit)}",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = RubyUdhar,
                            fontSize = 14.sp
                        )
                    )
                }
                if (entry.credit > 0) {
                    Text(
                        text = "+ ${CurrencyFormatter.formatCurrency(entry.credit)}",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldJama,
                            fontSize = 14.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bal: ${CurrencyFormatter.formatCurrency(entry.runningBalance)}",
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSubtle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun PartyInvoiceCard(
    invoice: InvoiceEntity,
    onPreviewClick: () -> Unit,
    onPrintClick: () -> Unit,
    onReceivePaymentClick: (() -> Unit)? = null
) {
    val isFullyPaid = invoice.status.equals("PAID", ignoreCase = true)
    ChopdaCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = invoice.itemsSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                StatusPill(
                    text = invoice.status,
                    textColor = when (invoice.status) {
                        "PAID" -> EmeraldJama
                        "PARTIAL" -> BrandPrimaryBlue
                        else -> RubyUdhar
                    },
                    backgroundColor = when (invoice.status) {
                        "PAID" -> EmeraldJama.copy(alpha = 0.12f)
                        "PARTIAL" -> BrandPrimaryBlue.copy(alpha = 0.12f)
                        else -> RubyUdhar.copy(alpha = 0.12f)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹ ${invoice.grandTotal.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = DarkNavy,
                            fontSize = 16.sp
                        )
                    )
                    if (!isFullyPaid) {
                        Text(
                            text = "Due: ₹ ${String.format(java.util.Locale.US, "%.2f", invoice.balanceDue)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = RubyUdhar,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isFullyPaid && onReceivePaymentClick != null) {
                        Button(
                            onClick = onReceivePaymentClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldJama),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Receive", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onPreviewClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View PDF", fontSize = 11.5.sp, color = BrandPrimaryBlue)
                    }

                    OutlinedButton(
                        onClick = onPrintClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = DarkNavy,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.5.sp, color = DarkNavy)
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyQuotationCard(
    quotation: QuotationEntity,
    onConvertClick: () -> Unit,
    onClick: () -> Unit = {}
) {
    ChopdaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 14.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = quotation.quotationNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = quotation.itemsSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                StatusPill(
                    text = quotation.status,
                    textColor = BrandPrimaryBlue,
                    backgroundColor = BrandPrimaryBlue.copy(alpha = 0.12f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹ ${quotation.grandTotal.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = DarkNavy,
                        fontSize = 16.sp
                    )
                )

                Button(
                    onClick = onConvertClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("⚡ Convert to Bill", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PartyDetailField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSubtle,
                fontSize = 12.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = DarkNavy,
                fontSize = 13.sp
            )
        )
    }
}
