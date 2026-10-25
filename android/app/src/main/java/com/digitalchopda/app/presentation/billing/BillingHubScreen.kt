package com.digitalchopda.app.presentation.billing

import android.widget.Toast
import com.digitalchopda.app.core.network.InstantNetworkSyncMonitor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import com.digitalchopda.app.core.designsystem.EmptyStateCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
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
import com.digitalchopda.app.core.designsystem.SyncStatusPill
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.data.work.InvoiceSyncWorker
import com.digitalchopda.app.data.local.entity.CreditNoteEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.local.entity.QuotationEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CreditNoteRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.PurchaseRepository
import com.digitalchopda.app.data.repository.QuotationRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Purchase
import com.digitalchopda.app.presentation.party.RecordPurchaseDialog
import com.digitalchopda.app.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🧾 Dedicated Billing & Documents Hub (Tab 2)
 *
 * Provides dedicated separated sections for:
 * 1. Invoices (Tax Invoices) -> with "+ New Invoice" button
 * 2. Quotations (Estimates) -> with "+ New Quotation" button
 * 3. Sales Returns (Credit Notes) -> with "+ New Sales Return" button
 * 4. Purchases (Inward Cargo / Bills) -> with "+ Record Purchase Bill" button
 *
 * Tapping any document row opens the dedicated DocumentDetailScreen with line items, tax breakdown,
 * Edit action, and See as PDF action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingHubScreen(
    companyRepository: CompanyRepository,
    billingRepository: BillingRepository,
    quotationRepository: QuotationRepository,
    creditNoteRepository: CreditNoteRepository,
    customerRepository: CustomerRepository,
    purchaseRepository: PurchaseRepository? = null,
    pdfGenerator: InvoicePdfGenerator,
    onNavigateToCreateInvoice: () -> Unit,
    onNavigateToCreateQuotation: () -> Unit,
    onNavigateToDocumentDetail: (docType: String, docId: Long) -> Unit,
    onNavigateToPdfViewer: (filePath: String, invoiceId: Long, customerName: String, customerPhone: String, invoiceNumber: String) -> Unit
) {
    val context = LocalContext.current
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

    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activeCompany = companyRepository.getActiveCompany()
    }

    LaunchedEffect(activeCompany.id) {
        isSyncing = true
        try {
            billingRepository.syncInvoicesWithServer(activeCompany.id)
            quotationRepository.syncQuotationsWithServer(activeCompany.id)
            purchaseRepository?.syncPurchasesWithServer(activeCompany.id)
        } catch (e: Exception) {
            android.util.Log.e("BillingHubScreen", "Auto-sync error", e)
        } finally {
            isSyncing = false
        }
    }

    var selectedSection by rememberSaveable { mutableIntStateOf(0) } // 0: Invoices, 1: Quotations, 2: Sales Returns, 3: Purchases
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var invoiceFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, PAID, UNPAID

    var showCreateCreditNoteDialog by remember { mutableStateOf(false) }
    var showRecordPurchaseDialog by remember { mutableStateOf(false) }

    val invoices by billingRepository.getInvoicesForCompany(activeCompany.id).collectAsState(initial = emptyList())
    val quotations by quotationRepository.getQuotationsForCompany(activeCompany.id).collectAsState(initial = emptyList())
    val creditNotes by creditNoteRepository.getCreditNotesForCompany(activeCompany.id).collectAsState(initial = emptyList())
    val purchases by (purchaseRepository?.getPurchasesForCompany(activeCompany.id) ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
    val customers by customerRepository.getCustomers(activeCompany.id).collectAsState(initial = emptyList())
    val pendingSyncCount by billingRepository.observePendingSyncCount().collectAsState(initial = 0)
    val isOnline by InstantNetworkSyncMonitor.isOnline.collectAsState()
    val isSyncingOutbox by InstantNetworkSyncMonitor.isSyncing.collectAsState()

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Aggregate statistics
    val totalInvoiced = remember(invoices) { invoices.sumOf { it.grandTotal } }
    val totalPaid = remember(invoices) { invoices.filter { it.status.equals("PAID", ignoreCase = true) }.sumOf { it.grandTotal } }
    val totalPending = remember(invoices) { invoices.filter { !it.status.equals("PAID", ignoreCase = true) }.sumOf { it.grandTotal } }
    val totalReturns = remember(creditNotes) { creditNotes.filter { it.status == "ACTIVE" }.sumOf { it.totalAmount } }
    val totalPurchases = remember(purchases) { purchases.sumOf { it.grandTotal } }

    val filteredInvoices = remember(invoices, searchQuery, invoiceFilter) {
        invoices.filter { inv ->
            val matchesQuery = searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.customerPhone.contains(searchQuery) ||
                inv.itemsSummary.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (invoiceFilter) {
                "PAID" -> inv.status.equals("PAID", ignoreCase = true)
                "UNPAID" -> !inv.status.equals("PAID", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val filteredQuotations = remember(quotations, searchQuery) {
        quotations.filter { quote ->
            searchQuery.isBlank() ||
                quote.quotationNumber.contains(searchQuery, ignoreCase = true) ||
                quote.customerName.contains(searchQuery, ignoreCase = true) ||
                quote.customerPhone.contains(searchQuery) ||
                quote.itemsSummary.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredCreditNotes = remember(creditNotes, searchQuery) {
        creditNotes.filter { cn ->
            searchQuery.isBlank() ||
                cn.creditNoteNumber.contains(searchQuery, ignoreCase = true) ||
                cn.customerName.contains(searchQuery, ignoreCase = true) ||
                cn.customerPhone.contains(searchQuery) ||
                cn.originalInvoiceNumber.contains(searchQuery, ignoreCase = true) ||
                cn.reason.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPurchases = remember(purchases, searchQuery) {
        purchases.filter { p ->
            searchQuery.isBlank() ||
                p.purchaseNumber.contains(searchQuery, ignoreCase = true) ||
                p.vendorBillNumber.contains(searchQuery, ignoreCase = true) ||
                p.supplierInvoiceNo.contains(searchQuery, ignoreCase = true) ||
                p.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Billing & Documents",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = "${activeCompany.firmName} • GST Invoicing Hub",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightCardSurface),
                actions = {
                    SyncStatusPill(
                        pendingCount = pendingSyncCount,
                        isOnline = isOnline,
                        isSyncing = isSyncingOutbox || isSyncing,
                        onSyncClick = {
                            InstantNetworkSyncMonitor.triggerManualSync(context)
                            val message = when {
                                !isOnline -> "Device is offline. $pendingSyncCount records queued in local outbox."
                                pendingSyncCount == 0 -> "Cloud vault is completely up to date with Central Depot"
                                else -> "Syncing $pendingSyncCount pending items with Central Depot..."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                isSyncing = true
                                try {
                                    billingRepository.syncInvoicesWithServer(activeCompany.id)
                                    quotationRepository.syncQuotationsWithServer(activeCompany.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("BillingHubScreen", "Manual sync error", e)
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                            color = BrandPrimaryBlue
                        )
                    } else {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isSyncing = true
                                    try {
                                        billingRepository.syncInvoicesWithServer(activeCompany.id)
                                        quotationRepository.syncQuotationsWithServer(activeCompany.id)
                                        purchaseRepository?.syncPurchasesWithServer(activeCompany.id)
                                    } catch (e: Exception) {
                                        android.util.Log.e("BillingHubScreen", "Manual sync error", e)
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Documents",
                                tint = BrandPrimaryBlue
                            )
                        }
                    }
                }
            )
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
        },
        floatingActionButton = {
            when (selectedSection) {
                0 -> {
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToCreateInvoice() },
                        containerColor = BrandPrimaryBlue,
                        contentColor = Color.White,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Tax Invoice",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "New Tax Invoice",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .semantics(mergeDescendants = true) { contentDescription = "New Tax Invoice" }
                            .padding(bottom = 8.dp)
                    )
                }
                1 -> {
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToCreateQuotation() },
                        containerColor = BrandPrimaryBlue,
                        contentColor = Color.White,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Quotation / Estimate",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "New Quotation / Estimate",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .semantics(mergeDescendants = true) { contentDescription = "New Quotation / Estimate" }
                            .padding(bottom = 8.dp)
                    )
                }
                2 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateCreditNoteDialog = true },
                        containerColor = RubyUdhar,
                        contentColor = Color.White,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Sales Return / Credit Note",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "New Sales Return / Credit Note",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.5.sp
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .semantics(mergeDescendants = true) { contentDescription = "New Sales Return / Credit Note" }
                            .padding(bottom = 8.dp)
                    )
                }
                3 -> {
                    if (purchaseRepository != null) {
                        ExtendedFloatingActionButton(
                            onClick = { showRecordPurchaseDialog = true },
                            containerColor = EmeraldJama,
                            contentColor = Color.White,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Record Inward Purchase Bill",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Record Purchase Bill",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.5.sp
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .semantics(mergeDescendants = true) { contentDescription = "Record Inward Purchase Bill" }
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepObsidianCanvas)
        ) {
            // Summary Metrics Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MidnightCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Billed", style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle))
                        Text(
                            text = CurrencyFormatter.formatINR(totalInvoiced),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Udhar / Due", style = MaterialTheme.typography.labelSmall.copy(color = RubyUdhar))
                        Text(
                            text = CurrencyFormatter.formatINR(totalPending),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = RubyUdhar
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sales Returns", style = MaterialTheme.typography.labelSmall.copy(color = WarningAmber))
                        Text(
                            text = CurrencyFormatter.formatINR(totalReturns),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = WarningAmber
                            )
                        )
                    }
                }
            }

            // Quick Search Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by bill #, party, phone, or item...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSubtle)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightCardSurface,
                        unfocusedContainerColor = MidnightCardSurface,
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            }

            // 4 Segmented Sub-Tabs: [Invoices] | [Quotations] | [Sales Returns] | [Purchases]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = ElevatedSlate,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tab 0: Invoices
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 0) BrandPrimaryBlue else Color.Transparent)
                            .clickable { selectedSection = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🧾 Bills",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSection == 0) Color.White else TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            if (invoices.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "(${invoices.size})",
                                    fontSize = 10.sp,
                                    color = if (selectedSection == 0) Color.White.copy(alpha = 0.8f) else TextSubtle
                                )
                            }
                        }
                    }

                    // Tab 1: Quotations
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 1) BrandPrimaryBlue else Color.Transparent)
                            .clickable { selectedSection = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📋 Quotes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSection == 1) Color.White else TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            if (quotations.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "(${quotations.size})",
                                    fontSize = 10.sp,
                                    color = if (selectedSection == 1) Color.White.copy(alpha = 0.8f) else TextSubtle
                                )
                            }
                        }
                    }

                    // Tab 2: Sales Returns
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 2) BrandPrimaryBlue else Color.Transparent)
                            .clickable { selectedSection = 2 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "↩️ Returns",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedSection == 2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSection == 2) Color.White else TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            if (creditNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "(${creditNotes.size})",
                                    fontSize = 10.sp,
                                    color = if (selectedSection == 2) Color.White.copy(alpha = 0.8f) else TextSubtle
                                )
                            }
                        }
                    }

                    // Tab 3: Purchases
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 3) BrandPrimaryBlue else Color.Transparent)
                            .clickable { selectedSection = 3 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📥 Intake",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedSection == 3) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedSection == 3) Color.White else TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            if (purchases.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "(${purchases.size})",
                                    fontSize = 10.sp,
                                    color = if (selectedSection == 3) Color.White.copy(alpha = 0.8f) else TextSubtle
                                )
                            }
                        }
                    }
                }
            }

            // Main Document List & Bottom Creation Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedSection) {
                    0 -> {
                        // ─── TAB 0: INVOICES LIST ───
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Filter row for invoices (ALL, PAID, UNPAID)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL" to "All Bills", "PAID" to "Paid ✓", "UNPAID" to "Unpaid / Due").forEach { (key, label) ->
                                    val isSelected = invoiceFilter == key
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) BrandAccentSoft else ElevatedSlate)
                                            .border(1.dp, if (isSelected) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(16.dp))
                                            .clickable { invoiceFilter = key }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) BrandPrimaryBlue else TextMuted
                                            )
                                        )
                                    }
                                }
                            }

                            if (filteredInvoices.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateCard(
                                        title = if (searchQuery.isNotBlank()) "No invoices matching '$searchQuery'" else "No invoices created yet",
                                        subtitle = if (searchQuery.isNotBlank()) "Try searching by party name or invoice number" else "Generate GST-compliant tax invoices with automatic numbering and QR codes",
                                        icon = Icons.Default.Receipt,
                                        actionText = if (searchQuery.isBlank()) "+ Create First Invoice" else null,
                                        onActionClick = if (searchQuery.isBlank()) { { onNavigateToCreateInvoice() } } else null
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredInvoices, key = { it.id }) { inv ->
                                        InvoiceItemCard(
                                            invoice = inv,
                                            dateFormat = dateFormat,
                                            onClick = { onNavigateToDocumentDetail("invoice", inv.id) },
                                            onAdvanceFulfillment = { targetInv ->
                                                coroutineScope.launch {
                                                    val nextStatus = when (targetInv.fulfillmentStatus.lowercase()) {
                                                        "not_ready" -> "ready"
                                                        "ready" -> "dispatched"
                                                        "dispatched" -> "delivered"
                                                        else -> "delivered"
                                                    }
                                                    billingRepository.updateFulfillmentStatus(targetInv.id, targetInv.serverOrderId, nextStatus)
                                                    Toast.makeText(context, "Order marked as ${nextStatus.replace('_', ' ').uppercase()}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ─── TAB 1: QUOTATIONS LIST ───
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (filteredQuotations.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateCard(
                                        title = if (searchQuery.isNotBlank()) "No quotations matching '$searchQuery'" else "No quotations issued yet",
                                        subtitle = if (searchQuery.isNotBlank()) "Try searching by party or quotation number" else "Send formal price estimates & convert them to tax invoices with 1 tap",
                                        icon = Icons.Default.Description,
                                        actionText = if (searchQuery.isBlank()) "+ Create Quotation" else null,
                                        onActionClick = if (searchQuery.isBlank()) { { onNavigateToCreateInvoice() } } else null
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredQuotations, key = { it.id }) { quote ->
                                        QuotationItemCard(
                                            quotation = quote,
                                            dateFormat = dateFormat,
                                            onClick = { onNavigateToDocumentDetail("quotation", quote.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ─── TAB 2: SALES RETURNS (CREDIT NOTES) ───
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (filteredCreditNotes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateCard(
                                        title = if (searchQuery.isNotBlank()) "No sales returns matching '$searchQuery'" else "No sales returns recorded yet",
                                        subtitle = if (searchQuery.isNotBlank()) "Try checking the party name or reason" else "Record returned goods and issue GST credit notes with automatic ledger adjustments",
                                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                                        actionText = if (searchQuery.isBlank()) "+ New Sales Return" else null,
                                        onActionClick = if (searchQuery.isBlank()) { { showCreateCreditNoteDialog = true } } else null
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredCreditNotes, key = { it.id }) { cn ->
                                        CreditNoteItemCard(
                                            creditNote = cn,
                                            dateFormat = dateFormat,
                                            onClick = { onNavigateToDocumentDetail("credit_note", cn.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // ─── TAB 3: INWARD PURCHASES (BILLS) ───
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (filteredPurchases.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateCard(
                                        title = if (searchQuery.isNotBlank()) "No purchases matching '$searchQuery'" else "No purchase bills recorded yet",
                                        subtitle = if (searchQuery.isNotBlank()) "Try checking the bill number or notes" else "Record inward cargo and vendor bills with automatic inventory stock addition",
                                        icon = Icons.Default.ShoppingBag,
                                        actionText = if (searchQuery.isBlank() && purchaseRepository != null) "+ Record Purchase Bill" else null,
                                        onActionClick = if (searchQuery.isBlank() && purchaseRepository != null) { { showRecordPurchaseDialog = true } } else null
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredPurchases, key = { it.id }) { purchase ->
                                        val supplier = customers.firstOrNull { it.id == purchase.supplierId }
                                        val supplierDisplayName = supplier?.name ?: if (purchase.supplierId > 0) "Supplier #${purchase.supplierId}" else "Cash / Direct Supplier"
                                        PurchaseItemCard(
                                            purchase = purchase,
                                            dateFormat = dateFormat,
                                            supplierName = supplierDisplayName,
                                            onClick = {
                                                Toast.makeText(context, "${purchase.purchaseNumber}: Total ${CurrencyFormatter.formatINR(purchase.grandTotal)} (${purchase.paymentStatus.uppercase()})", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Create Credit Note Dialog
    if (showCreateCreditNoteDialog) {
        CreateCreditNoteDialog(
            companyRepository = companyRepository,
            customerRepository = customerRepository,
            billingRepository = billingRepository,
            creditNoteRepository = creditNoteRepository,
            pdfGenerator = pdfGenerator,
            onDismiss = { showCreateCreditNoteDialog = false },
            onCreditNoteCreated = { savedEntity, pdfFile ->
                showCreateCreditNoteDialog = false
                onNavigateToPdfViewer(
                    pdfFile.absolutePath,
                    savedEntity.id,
                    savedEntity.customerName,
                    savedEntity.customerPhone,
                    savedEntity.creditNoteNumber
                )
            }
        )
    }

    // Modal Sheet: Record Inward Purchase Dialog
    if (showRecordPurchaseDialog && purchaseRepository != null) {
        RecordPurchaseDialog(
            companyId = activeCompany.id,
            supplierId = 0L,
            supplierName = "",
            purchaseRepository = purchaseRepository,
            customerRepository = customerRepository,
            onDismiss = { showRecordPurchaseDialog = false },
            onPurchaseRecorded = {
                showRecordPurchaseDialog = false
                coroutineScope.launch {
                    purchaseRepository.syncPurchasesWithServer(activeCompany.id)
                }
            }
        )
    }
}

/**
 * Invoice Item Card
 */
@Composable
fun InvoiceItemCard(
    invoice: InvoiceEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onAdvanceFulfillment: ((InvoiceEntity) -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MidnightCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
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
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = BrandPrimaryBlue
                        )
                    )
                    Text(
                        text = dateFormat.format(Date(invoice.createdAt)),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fulfillment = invoice.fulfillmentStatus.lowercase()
                    val (fText, fBg, fColor) = when (fulfillment) {
                        "ready" -> Triple("READY", Color(0xFF1E3A8A), Color(0xFF93C5FD))
                        "dispatched" -> Triple("DISPATCHED", Color(0xFF3B0764), Color(0xFFD8B4FE))
                        "delivered" -> Triple("DELIVERED", EmeraldJamaBg, EmeraldJama)
                        else -> Triple("NOT READY", Color(0xFF451A03), Color(0xFFFCD34D))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(fBg)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = fText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = fColor,
                                fontSize = 10.sp
                            )
                        )
                    }

                    val (badgeText, badgeBg, badgeColor) = when (invoice.status.uppercase()) {
                        "PAID" -> Triple("PAID", EmeraldJamaBg, EmeraldJama)
                        "PARTIAL" -> Triple("PARTIAL", Color(0xFFFEF3C7), WarningAmber)
                        else -> Triple("UNPAID", RubyUdharBg, RubyUdhar)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ElevatedSlate)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.customerName.ifBlank { "Walk-in Customer" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    if (invoice.itemsSummary.isNotBlank()) {
                        Text(
                            text = invoice.itemsSummary,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            maxLines = 1
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatINR(invoice.grandTotal),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                    )
                    if (!invoice.status.equals("PAID", ignoreCase = true)) {
                        Text(
                            text = "Due: ${CurrencyFormatter.formatINR(invoice.balanceDue)}",
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RubyUdhar,
                                fontSize = 11.sp
                            )
                        )
                    } else {
                        Text(
                            text = "Mode: ${invoice.paymentMode}",
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp)
                        )
                    }
                }
            }

            if (onAdvanceFulfillment != null && invoice.fulfillmentStatus.lowercase() != "delivered") {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = ElevatedSlate.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fulfillment Action:",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp)
                    )
                    val (actionLabel, actionBg, actionTextColor) = when (invoice.fulfillmentStatus.lowercase()) {
                        "not_ready" -> Triple("Mark Ready 📦", BrandPrimaryBlue, Color.White)
                        "ready" -> Triple("Mark Dispatched 🚚", Color(0xFF3B0764), Color(0xFFD8B4FE))
                        "dispatched" -> Triple("Mark Delivered ✓", EmeraldJama, Color.White)
                        else -> Triple("", ElevatedSlate, TextMuted)
                    }
                    if (actionLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(actionBg)
                                .bouncyClick { onAdvanceFulfillment(invoice) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = actionTextColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quotation Item Card
 */
@Composable
fun QuotationItemCard(
    quotation: QuotationEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MidnightCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
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
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = ImperialGoldLight
                        )
                    )
                    Text(
                        text = dateFormat.format(Date(quotation.createdAt)),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                    )
                }

                val (badgeText, badgeBg, badgeColor) = when (quotation.status.uppercase()) {
                    "CONVERTED" -> Triple("CONVERTED ✓", EmeraldJamaBg, EmeraldJama)
                    "EXPIRED" -> Triple("EXPIRED", ElevatedSlate, TextMuted)
                    else -> Triple("PENDING", Color(0xFFFEF3C7), WarningAmber)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ElevatedSlate)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quotation.customerName.ifBlank { "Prospect" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    if (quotation.itemsSummary.isNotBlank()) {
                        Text(
                            text = quotation.itemsSummary,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            maxLines = 1
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatINR(quotation.grandTotal),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = ImperialGoldLight
                        )
                    )
                    Text(
                        text = "Estimate Total",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

/**
 * Credit Note Item Card (Sales Return)
 */
@Composable
fun CreditNoteItemCard(
    creditNote: CreditNoteEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MidnightCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = creditNote.creditNoteNumber,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = RubyUdhar
                        )
                    )
                    Text(
                        text = dateFormat.format(Date(creditNote.createdAt)),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RubyUdharBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = creditNote.reason.take(20),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = RubyUdhar,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ElevatedSlate)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = creditNote.customerName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    if (creditNote.originalInvoiceNumber.isNotBlank()) {
                        Text(
                            text = "Bill: ${creditNote.originalInvoiceNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrandPrimaryBlue),
                            maxLines = 1
                        )
                    } else if (creditNote.itemsSummary.isNotBlank()) {
                        Text(
                            text = creditNote.itemsSummary,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            maxLines = 1
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatINR(creditNote.totalAmount),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = RubyUdhar
                        )
                    )
                    Text(
                        text = "Credit Refund",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

/**
 * Inward Purchase Bill Item Card
 */
@Composable
fun PurchaseItemCard(
    purchase: Purchase,
    dateFormat: SimpleDateFormat,
    supplierName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MidnightCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = purchase.purchaseNumber,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = EmeraldJama
                            )
                        )
                        val billRef = purchase.supplierInvoiceNo.ifBlank { purchase.vendorBillNumber }
                        if (billRef.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Ref: $billRef",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                        }
                    }
                    Text(
                        text = dateFormat.format(Date(purchase.purchaseDate)),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                    )
                }

                val (badgeText, badgeBg, badgeColor) = when (purchase.paymentStatus.lowercase()) {
                    "paid" -> Triple("PAID", EmeraldJamaBg, EmeraldJama)
                    "partially_paid" -> Triple("PARTIAL", Color(0xFFFEF3C7), WarningAmber)
                    else -> Triple("UNPAID", RubyUdharBg, RubyUdhar)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ElevatedSlate)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplierName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    if (purchase.notes.isNotBlank()) {
                        Text(
                            text = purchase.notes,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            maxLines = 1
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatINR(purchase.grandTotal),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                    )
                    if (purchase.taxAmount > 0.0) {
                        Text(
                            text = "Tax: ${CurrencyFormatter.formatINR(purchase.taxAmount)}",
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}
