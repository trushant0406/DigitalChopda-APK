package com.digitalchopda.app.presentation.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.digitalchopda.app.core.network.InstantNetworkSyncMonitor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.digitalchopda.app.core.designsystem.EmptyStateCard
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BezierSalesChart
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.ChopdaPartyRow
import com.digitalchopda.app.core.designsystem.ChopdaPerspectiveSlider
import com.digitalchopda.app.core.designsystem.CompanySwitcherHeader
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElectricBrandCyan
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.GoldFoilGradient
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.PulsingAlertStrip
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.SacredLedgerCockpitCard
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WhatsAppGreen
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.work.InvoiceSyncWorker
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.presentation.settings.CompanySwitchSheet
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch

data class PartyViewItem(
    val id: Long,
    val name: String,
    val phone: String,
    val balance: Double, // Positive = Jama (You'll Get), Negative = Udhar (You'll Give)
    val lastActive: String
)

data class PartyTransaction(
    val id: Long,
    val date: String,
    val note: String,
    val isJama: Boolean, // true = You Got (જમા), false = You Gave (ઉધાર)
    val amount: Double,
    val balanceAfter: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    companyRepository: CompanyRepository,
    billingRepository: BillingRepository,
    customerRepository: CustomerRepository,
    languageManager: com.digitalchopda.app.core.localization.LanguageManager? = null,
    onNavigateToParty: () -> Unit = {},
    onNavigateToBilling: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToLeads: () -> Unit,
    onNavigateToGst: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPartyDetail: (partyId: Long, initialTab: Int) -> Unit = { _, _ -> },
    onNavigateToDocumentDetail: (docType: String, docId: Long) -> Unit = { _, _ -> },
    showBottomDock: Boolean = true,
    viewModel: DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = DashboardViewModel.Factory(companyRepository, billingRepository, customerRepository)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    val activeCompany by viewModel.activeCompany.collectAsState()
    val filteredCustomerEntities by viewModel.filteredCustomers.collectAsState()
    val totalJama by viewModel.totalJama.collectAsState()
    val totalUdhar by viewModel.totalUdhar.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pendingSyncCount by billingRepository.observePendingSyncCount().collectAsState(initial = 0)
    val isOnline by InstantNetworkSyncMonitor.isOnline.collectAsState()
    val isSyncingOutbox by InstantNetworkSyncMonitor.isSyncing.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val ordersSummary by viewModel.ordersSummary.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val totalCashInflow by viewModel.totalCashInflow.collectAsState()
    val totalCashOutflow by viewModel.totalCashOutflow.collectAsState()
    val recentInvoices by viewModel.recentInvoices.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val invoiceCount by viewModel.invoiceCount.collectAsState()

    var showCompanySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Selected Party for Ledger Detail Sheet (ખાતાવહી)
    var selectedPartyForDetail by remember { mutableStateOf<PartyViewItem?>(null) }
    val detailSheetState = rememberModalBottomSheetState()

    // 0 = ચોપડો (Parties Ledger), 1 = રોકડ (Daily Cashbook), 2 = ઇન્વોઇસ (Invoices & Stats)
    var selectedPerspective by remember { mutableIntStateOf(0) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val partyFilter by viewModel.partyFilter.collectAsState()

    // Quick Voucher Dialog
    var showQuickVoucherSheet by remember { mutableStateOf(false) }
    val voucherSheetState = rememberModalBottomSheetState()
    var voucherPartyName by remember { mutableStateOf("") }
    var voucherAmount by remember { mutableStateOf("") }
    var voucherNote by remember { mutableStateOf("") }
    var voucherIsJama by remember { mutableStateOf(true) } // true = You Got (જમા), false = You Gave (ઉધાર)

    // Map Room entities reactively to UI party view items
    val filteredParties: List<PartyViewItem> = remember(filteredCustomerEntities) {
        filteredCustomerEntities
            .distinctBy { it.id }
            .map { item ->
                PartyViewItem(
                    id = item.id,
                    name = item.name,
                    phone = item.phone.ifBlank { "+91 98000 00000" },
                    balance = item.balance,
                    lastActive = if (item.address.isNotBlank()) item.address else "ચોપડા ખાતું"
                )
            }
    }

    fun sendWhatsAppReminder(party: PartyViewItem) {
        val message = com.digitalchopda.app.core.payment.UpiPaymentUtils.buildWhatsAppReminderMessage(
            customerName = party.name,
            firmName = activeCompany.firmName,
            amount = kotlin.math.abs(party.balance),
            upiVpa = activeCompany.upiVpa
        )
        com.digitalchopda.app.core.payment.UpiPaymentUtils.launchWhatsApp(
            context = context,
            phoneNumber = party.phone,
            message = message
        )
    }

    fun sendAllReminders() {
        val debtors = filteredParties.filter { it.balance > 0 }.sortedByDescending { it.balance }
        if (debtors.isEmpty()) {
            Toast.makeText(context, "✅ All party balances are clear!", Toast.LENGTH_SHORT).show()
            return
        }
        val topDebtor = debtors.first()
        Toast.makeText(
            context,
            "📢 Opening WhatsApp UPI reminder for ${topDebtor.name} (${debtors.size} parties pending)",
            Toast.LENGTH_SHORT
        ).show()
        sendWhatsAppReminder(topDebtor)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidianCanvas)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = if (showBottomDock) 76.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Royal Firm Header & Live Switcher
            item {
                val currentLang = languageManager?.currentLanguage?.collectAsState()?.value
                CompanySwitcherHeader(
                    firmName = activeCompany.firmName.ifBlank { "ABC E-COMMERCE" },
                    ownerName = if (activeCompany.ownerName.isBlank()) "Admin User" else activeCompany.ownerName,
                    unreadNotifications = if (pendingSyncCount > 0) pendingSyncCount else 0,
                    languageName = currentLang?.nativeName,
                    statusPillText = when {
                        !isOnline -> if (pendingSyncCount > 0) "🔴 $pendingSyncCount Queued" else "🔴 Offline Enclave"
                        isSyncingOutbox -> "🔄 Syncing Depot..."
                        pendingSyncCount > 0 -> "⏳ $pendingSyncCount Pending"
                        else -> "● Cloud Synced"
                    },
                    onLanguageClick = if (languageManager != null) {
                        {
                            val next = languageManager.cycleNextLanguage()
                            Toast.makeText(context, "ભાષા: ${next.nativeName}", Toast.LENGTH_SHORT).show()
                        }
                    } else null,
                    onCompanyClick = { showCompanySheet = true },
                    onNotificationClick = {
                        InstantNetworkSyncMonitor.triggerManualSync(context)
                        val message = when {
                            !isOnline -> "Device is offline. $pendingSyncCount records queued in local outbox."
                            pendingSyncCount == 0 -> "Cloud vault is completely up to date with Central Depot"
                            else -> "Syncing $pendingSyncCount pending items with Central Depot..."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onFilterClick = {}
                )
            }

            // 2. Sacred Indian Chopda Ledger Cockpit (Dual Vault Card)
            item {
                SacredLedgerCockpitCard(
                    youWillGetTotal = CurrencyFormatter.formatCurrency(totalJama),
                    youWillGiveTotal = CurrencyFormatter.formatCurrency(totalUdhar),
                    activePartiesCount = filteredParties.count { it.balance > 0 },
                    onSendAllReminders = { sendAllReminders() }
                )
            }

            // 3. 3-Way Perspective Selector (ચોપડો vs રોકડ vs ઇન્વોઇસ)
            item {
                ChopdaPerspectiveSlider(
                    selectedIndex = selectedPerspective,
                    onTabSelected = { selectedPerspective = it }
                )
            }

            // 4. Perspective Content Switching
            when (selectedPerspective) {
                0 -> {
                    // ─── PERSPECTIVE 0: પક્ષકારો / CUSTOMER LEDGER (ચોપડો) ───
                    item {
                        // Quick Search Bar with Blue Highlight
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    "Search Party, City or Phone...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSubtle)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue
                                )
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

                    // Filter Chips: All Parties / Receivable / Payable
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChipPill(
                                label = "All Parties (${filteredParties.size})",
                                isSelected = partyFilter == "ALL",
                                onClick = { viewModel.setPartyFilter("ALL") }
                            )
                            FilterChipPill(
                                label = "Receivable",
                                isSelected = partyFilter == "JAMA",
                                activeColor = EmeraldJama,
                                onClick = { viewModel.setPartyFilter("JAMA") }
                            )
                            FilterChipPill(
                                label = "Payable",
                                isSelected = partyFilter == "UDHAR",
                                activeColor = RubyUdhar,
                                onClick = { viewModel.setPartyFilter("UDHAR") }
                            )
                        }
                    }

                    // Parties List
                    if (filteredParties.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "કોઈ પક્ષકાર મળ્યા નથી",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                )
                            }
                        }
                    } else {
                        items(
                            items = filteredParties,
                            key = { party -> party.id },
                            contentType = { "party_row" }
                        ) { party ->
                            ChopdaPartyRow(
                                name = party.name,
                                phone = party.phone,
                                balance = party.balance,
                                lastActive = party.lastActive,
                                onPartyClick = {
                                    onNavigateToPartyDetail(party.id, 0)
                                },
                                onWhatsAppClick = {
                                    sendWhatsAppReminder(party)
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // ─── PERSPECTIVE 1: દૈનિક રોકડ રજિસ્ટર (ROKAD CASHBOOK) ───
                    item {
                        val netCash = totalCashInflow - totalCashOutflow
                        RokadCashDrawerCard(
                            closingCash = netCash,
                            openingBalance = 0.0,
                            totalInflow = totalCashInflow,
                            totalOutflow = totalCashOutflow
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "આજની રોકડ લેવડ-દેવડ (CASH FLOW)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "${payments.size} વ્યવહારો",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                        }
                    }

                    if (payments.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "કોઈ રોકડ વ્યવહાર નથી",
                                subtitle = "રોકડ આવક (જમા) અથવા ખર્ચ (ઉધાર) ઉમેરવા માટે '+ Quick Entry' પર ક્લિક કરો",
                                icon = Icons.Default.PointOfSale,
                                actionText = "+ Quick Entry",
                                onActionClick = {
                                    voucherPartyName = ""
                                    voucherAmount = ""
                                    voucherNote = ""
                                    voucherIsJama = true
                                    showQuickVoucherSheet = true
                                }
                            )
                        }
                    } else {
                        items(
                            items = payments.take(25),
                            key = { it.id }
                        ) { pmt ->
                            val partyName = if (pmt.customerName.isNotBlank()) pmt.customerName else (if (pmt.isJama) "રોકડ આવક (Cash In)" else "રોકડ ખર્ચ (Cash Out)")
                            val noteText = if (pmt.notes.isNotBlank()) pmt.notes else (if (pmt.isJama) "You Got (જમા)" else "You Gave (ઉધાર)")
                            val timeStr = remember(pmt.createdAt) {
                                java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(pmt.createdAt))
                            }
                            ChopdaPartyRow(
                                name = partyName,
                                phone = "$noteText • ${pmt.paymentMode.uppercase()}",
                                balance = if (pmt.isJama) pmt.amount else -pmt.amount,
                                lastActive = timeStr,
                                onPartyClick = {
                                    voucherPartyName = pmt.customerName
                                    voucherAmount = pmt.amount.toString()
                                    voucherIsJama = pmt.isJama
                                    voucherNote = pmt.notes
                                    showQuickVoucherSheet = true
                                },
                                onWhatsAppClick = {
                                    if (pmt.customerPhone.isNotBlank()) {
                                        val msg = "Payment of ₹${pmt.amount} recorded for ${pmt.customerName} via ${pmt.paymentMode.uppercase()}."
                                        com.digitalchopda.app.core.payment.UpiPaymentUtils.launchWhatsApp(context, pmt.customerPhone, msg)
                                    } else {
                                        Toast.makeText(context, "Payment entry: ₹${pmt.amount} (${if (pmt.isJama) "જમા" else "ઉધાર"})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                2 -> {
                    // ─── PERSPECTIVE 2: બિલિંગ અને સેલ્સ એનાલિટિક્સ (INVOICING) ───
                    item {
                        Text(
                            text = "ORDERS & SALES OVERVIEW (ઓર્ડર અને વેચાણ સારાંશ)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // 6 Live Stat Cards matching Web DashboardPage.jsx
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val unpaidCount = ordersSummary?.paymentPending ?: recentInvoices.count { !it.status.equals("PAID", ignoreCase = true) }
                                val unpaidAmt = ordersSummary?.pendingAmount ?: recentInvoices.filter { !it.status.equals("PAID", ignoreCase = true) }.sumOf { it.grandTotal - it.paidAmount }
                                DashboardStatCard(
                                    label = "Payment pending",
                                    value = "$unpaidCount",
                                    subValue = "₹${com.digitalchopda.app.util.formatINR(unpaidAmt)} pending",
                                    accentColor = DangerRed,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToBilling
                                )
                                DashboardStatCard(
                                    label = "Ready to dispatch",
                                    value = "${ordersSummary?.readyToDispatch ?: 0}",
                                    subValue = "Ready for packing",
                                    accentColor = BrandPrimaryBlue,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToBilling
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DashboardStatCard(
                                    label = "Sent to customer",
                                    value = "${ordersSummary?.dispatched ?: 0}",
                                    subValue = "In transit / Dispatched",
                                    accentColor = ElectricBrandCyan,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToBilling
                                )
                                DashboardStatCard(
                                    label = "Delivered",
                                    value = "${ordersSummary?.delivered ?: 0}",
                                    subValue = "Fulfilled orders",
                                    accentColor = EmeraldJama,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToBilling
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DashboardStatCard(
                                    label = "Total Orders",
                                    value = "${ordersSummary?.totalOrders ?: invoiceCount}",
                                    subValue = "All recorded orders",
                                    accentColor = TextWhite,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToBilling
                                )
                                DashboardStatCard(
                                    label = "Total Sales",
                                    value = "₹${com.digitalchopda.app.util.formatINR(totalSales)}",
                                    subValue = "$invoiceCount Invoices generated",
                                    accentColor = ImperialGold,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToBilling
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "QUICK COMMERCIAL ACTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                title = "નવું બિલ બનાવો",
                                subtitle = "GST Invoice & Print",
                                icon = Icons.Default.PointOfSale,
                                accentColor = CoralPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToBilling
                            )
                            QuickActionCard(
                                title = "Products Catalog",
                                subtitle = "Items & Price List",
                                icon = Icons.Default.ShoppingBag,
                                accentColor = ElectricBrandCyan,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToStock
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT SALES INVOICES (તાજેતરના બિલ)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "View All →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { onNavigateToBilling() }
                            )
                        }
                    }

                    if (recentInvoices.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "કોઈ તાજેતરના બિલ નથી",
                                subtitle = "ગ્રાહકો માટે GST બિલ બનાવવા માટે 'નવું બિલ બનાવો' પર ક્લિક કરો",
                                icon = Icons.Default.Description,
                                actionText = "Create First Bill",
                                onActionClick = onNavigateToBilling
                            )
                        }
                    } else {
                        items(
                            items = recentInvoices.take(8),
                            key = { it.id }
                        ) { inv ->
                            RecentInvoiceCard(
                                invoice = inv,
                                onClick = { onNavigateToDocumentDetail("invoice", inv.id) }
                            )
                        }
                    }
                }
            }
        }

        // Quick Entry FAB overlay
        if (!showBottomDock) {
            ExtendedFloatingActionButton(
                onClick = { showQuickVoucherSheet = true },
                containerColor = BrandPrimaryBlue,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Entry",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = {
                    Text(
                        text = "Quick Entry",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.5.sp
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "Quick Entry" }
            )
        }

        if (showBottomDock) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingCapsuleDock(
                    currentTab = 0,
                    onTabSelect = { tabIndex ->
                        when (tabIndex) {
                            0 -> {} // Home
                            1 -> onNavigateToParty()
                            2 -> onNavigateToBilling()
                            3 -> onNavigateToStock()
                            4 -> onNavigateToSettings()
                        }
                    }
                )
            }
        }
    }

    // Modal Sheet 1: Company Switcher
    if (showCompanySheet) {
        CompanySwitchSheet(
            companyRepository = companyRepository,
            currentActiveCompanyId = activeCompany.id,
            sheetState = sheetState,
            onDismiss = { showCompanySheet = false },
            onCompanySelected = { _ ->
                viewModel.refresh()
            }
        )
    }

    // Modal Sheet 2: Instant Quick-Entry Voucher Sheet (જમા / ઉધાર નોંધ)
    if (showQuickVoucherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickVoucherSheet = false },
            sheetState = voucherSheetState,
            containerColor = MidnightCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = strings.newTransactionTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = ImperialGoldLight,
                        letterSpacing = 0.5.sp
                    )
                )

                // Toggle: જમા (You Got) vs ઉધાર (You Gave)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSlate)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (voucherIsJama) EmeraldJama else Color.Transparent)
                            .clickable { voucherIsJama = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ ${strings.filterJama}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (voucherIsJama) TextWhite else TextMuted
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!voucherIsJama) RubyUdhar else Color.Transparent)
                            .clickable { voucherIsJama = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "- ${strings.filterUdhar}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!voucherIsJama) TextWhite else TextMuted
                            )
                        )
                    }
                }

                // Party Name
                OutlinedTextField(
                    value = voucherPartyName,
                    onValueChange = { voucherPartyName = it },
                    label = { Text(strings.partyNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount
                OutlinedTextField(
                    value = voucherAmount,
                    onValueChange = { voucherAmount = it },
                    label = { Text(strings.amountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Note
                OutlinedTextField(
                    value = voucherNote,
                    onValueChange = { voucherNote = it },
                    label = { Text(strings.noteLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Confirm Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (voucherIsJama) EmeraldJama else RubyUdhar)
                        .bouncyClick {
                            val amt = voucherAmount.toDoubleOrNull() ?: 0.0
                            if (voucherPartyName.isNotBlank() && amt > 0.0) {
                                viewModel.addQuickVoucher(
                                    partyName = voucherPartyName,
                                    amount = amt,
                                    isJama = voucherIsJama,
                                    note = voucherNote
                                )
                                Toast.makeText(context, "✅ Entry saved successfully!", Toast.LENGTH_SHORT).show()
                                voucherPartyName = ""
                                voucherAmount = ""
                                voucherNote = ""
                                scope.launch {
                                    voucherSheetState.hide()
                                    showQuickVoucherSheet = false
                                }
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.saveToChopda,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }

    // Modal Sheet 3: Dedicated Party Ledger Statement & Bahi-Khata (ખાતાવહી વિગત)
    selectedPartyForDetail?.let { party ->
        val isPositive = party.balance >= 0
        val balanceColor = if (isPositive) EmeraldJama else RubyUdhar
        val balanceTag = if (isPositive) strings.youWillGet else strings.youWillGive

        ModalBottomSheet(
            onDismissRequest = { selectedPartyForDetail = null },
            sheetState = detailSheetState,
            containerColor = MidnightCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sacred Header Ribbon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ChopdaClothGradient)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.sacredInvocation,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = ImperialGoldLight,
                            letterSpacing = 1.2.sp
                        )
                    )
                }

                // Party Profile & Fast Contact Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ElevatedSlate)
                                .border(1.5.dp, ImperialGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = party.name.split(" ")
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2)
                                    .joinToString("")
                                    .uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = ImperialGoldLight
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = party.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = party.phone,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }

                    // Contact Actions: Call & WhatsApp
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Phone Call Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, CircleShape)
                                .bouncyClick {
                                    try {
                                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${party.phone}")
                                        }
                                        context.startActivity(callIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = ImperialGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close Sheet
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, CircleShape)
                                .bouncyClick {
                                    scope.launch {
                                        detailSheetState.hide()
                                        selectedPartyForDetail = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSubtle,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Balance Vault Cockpit Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, balanceColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = balanceTag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = balanceColor,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            StatusPill(
                                text = strings.activeAccountBadge,
                                textColor = ImperialGold,
                                backgroundColor = ImperialGold.copy(alpha = 0.15f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹ ${String.format("%,.0f", kotlin.math.abs(party.balance))}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = balanceColor
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${strings.lastTransactionLabel}: ${party.lastActive}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle)
                        )
                    }
                }

                // In-Ledger Quick Action Buttons (+ જમા / - ઉધાર)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // + જમા મળ્યા (+ Received)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldJama)
                            .bouncyClick {
                                viewModel.addQuickVoucher(
                                    partyName = party.name,
                                    amount = 1000.0,
                                    isJama = true,
                                    note = "Quick +1000 Received"
                                )
                                selectedPartyForDetail = party.copy(
                                    balance = party.balance + 1000.0,
                                    lastActive = "Just now • ₹1,000 Received"
                                )
                                Toast.makeText(context, "₹1,000 Payment Received recorded!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.quickAddJama,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                        }
                    }

                    // - Give Credit (- Gave)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RubyUdhar)
                            .bouncyClick {
                                viewModel.addQuickVoucher(
                                    partyName = party.name,
                                    amount = 1000.0,
                                    isJama = false,
                                    note = "Quick +1000 Given"
                                )
                                selectedPartyForDetail = party.copy(
                                    balance = party.balance - 1000.0,
                                    lastActive = "Just now • ₹1,000 Given"
                                )
                                Toast.makeText(context, "₹1,000 Credit/Given recorded!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.quickAddUdhar,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                        }
                    }
                }

                // Recent Transactions Timeline Section
                Text(
                    text = strings.transactionHistory,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )

                // Timeline Items
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sampleTxns = listOf(
                        Triple("Today • 2:15 PM", "Bill #1021 - Textile Purchase", if (isPositive) -18500.0 else 6400.0),
                        Triple("Yesterday • 5:00 PM", "Cash Deposit (GPay UPI)", 5000.0),
                        Triple("02-Oct • 11:30 AM", "Initial Ledger Balance", if (isPositive) -5000.0 else -2000.0)
                    )

                    sampleTxns.forEach { (date, note, amt) ->
                        val isTxnJama = amt > 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                             fontWeight = FontWeight.SemiBold,
                                             color = TextWhite
                                        )
                                    )
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(
                                        text = if (isTxnJama) "RECEIVE (IN)" else "GIVE (OUT)",
                                        textColor = if (isTxnJama) EmeraldJama else RubyUdhar,
                                        backgroundColor = (if (isTxnJama) EmeraldJama else RubyUdhar).copy(alpha = 0.15f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "₹ ${String.format("%,.0f", kotlin.math.abs(amt))}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isTxnJama) EmeraldJama else RubyUdhar
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Dual Share Action Bar (WhatsApp Statement & Download PDF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Send WhatsApp Summary
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WhatsAppGreen)
                            .bouncyClick {
                                sendWhatsAppReminder(party)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.shareStatementWhatsApp,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    // Download Statement PDF
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .bouncyClick {
                                scope.launch {
                                    Toast.makeText(context, "Generating Statement PDF...", Toast.LENGTH_SHORT).show()
                                    val safeName = party.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                                    val pdfFile = billingRepository.downloadBackendStatementPdf(
                                        context = context,
                                        customerId = party.id,
                                        filename = "Statement_${safeName}.pdf"
                                    )
                                    if (pdfFile != null && pdfFile.exists()) {
                                        Toast.makeText(context, "Statement PDF Ready!", Toast.LENGTH_SHORT).show()
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            pdfFile
                                        )
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(viewIntent)
                                        } catch (_: Exception) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Statement PDF"))
                                        }
                                    } else {
                                        Toast.makeText(context, "${party.name} Statement PDF Generated!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = ImperialGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.downloadPdfStatement,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImperialGoldLight
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
 * Filter Chip Pill
 */
@Composable
fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color = BrandPrimaryBlue,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) activeColor else ElevatedSlate
            )
            .border(
                1.dp,
                if (isSelected) activeColor else BorderStrokeDark,
                RoundedCornerShape(20.dp)
            )
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextMuted
            )
        )
    }
}

/**
 * 📦 Rokad Drawer Daily Balance Card
 */
@Composable
fun RokadCashDrawerCard(
    closingCash: Double = 0.0,
    openingBalance: Double = 0.0,
    totalInflow: Double = 0.0,
    totalOutflow: Double = 0.0
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S CASH BOX (DAILY DRAWER)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextMuted,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldJama,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "₹ ${com.digitalchopda.app.util.formatINR(closingCash)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = ImperialGoldLight
                )
            )

            Text(
                text = "Closing Cash In Hand",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Opening Balance", style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle))
                    Text("₹ ${com.digitalchopda.app.util.formatINR(openingBalance)}", style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Total Inflow (+)", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldJama))
                    Text("+₹ ${com.digitalchopda.app.util.formatINR(totalInflow)}", style = MaterialTheme.typography.titleMedium.copy(color = EmeraldJama, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Total Outflow (-)", style = MaterialTheme.typography.labelSmall.copy(color = RubyUdhar))
                    Text("-₹ ${com.digitalchopda.app.util.formatINR(totalOutflow)}", style = MaterialTheme.typography.titleMedium.copy(color = RubyUdhar, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * 📊 Dashboard Metric Stat Card (Matching Web Dashboard)
 */
@Composable
fun DashboardStatCard(
    label: String,
    value: String,
    subValue: String? = null,
    accentColor: Color = BrandPrimaryBlue,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    fontSize = 20.sp
                )
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSubtle,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 🧾 Recent Sales Invoice Row Card
 */
@Composable
fun RecentInvoiceCard(
    invoice: InvoiceEntity,
    onClick: () -> Unit
) {
    val isPaid = invoice.status.equals("PAID", ignoreCase = true)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = invoice.customerName.ifBlank { "Walk-in Customer" },
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${com.digitalchopda.app.util.formatINR(invoice.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) EmeraldJama else RubyUdhar,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(
                    text = if (isPaid) "PAID" else "PENDING",
                    textColor = if (isPaid) EmeraldJama else RubyUdhar,
                    backgroundColor = if (isPaid) EmeraldJama.copy(alpha = 0.15f) else RubyUdhar.copy(alpha = 0.15f)
                )
            }
        }
    }
}

/**
 * Quick Action Card
 */
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(16.dp))
            .bouncyClick(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }
    }
}

/**
 * 🛸 Floating Capsule Glass Navigation Dock
 * Perfectly centered items (weight 1f) with solid BrandPrimaryBlue active state (matching Billing design)
 */
@Composable
fun FloatingCapsuleDock(
    currentTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val items = remember(strings) {
        listOf(
            Triple(strings.navChopda, Icons.Default.Description, "Chopda"),
            Triple(strings.navParty, Icons.Default.People, "Party"),
            Triple(strings.navBilling, Icons.Default.PointOfSale, "Billing"),
            Triple(strings.navProducts, Icons.Default.ShoppingBag, "Product"),
            Triple(strings.navSettings, Icons.Default.Tune, "Settings")
        )
    }

    val density = LocalDensity.current
    var dockWidthDp by remember { mutableStateOf(0.dp) }
    var dockHeightDp by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x33000000), ambientColor = Color(0x22000000))
                .clip(RoundedCornerShape(24.dp))
                .background(MidnightCardSurface)
                .border(1.dp, BorderStrokeDark, RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            if (dockWidthDp > 0.dp && dockHeightDp > 0.dp) {
                val tabCount = items.size
                val tabWidth = dockWidthDp / tabCount
                val animatedTabOffset by animateDpAsState(
                    targetValue = tabWidth * currentTab,
                    animationSpec = spring(
                        dampingRatio = 0.76f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "dockPillOffset"
                )

                // 1. Sliding Magnetic Gradient Pill
                Box(
                    modifier = Modifier
                        .offset(x = animatedTabOffset)
                        .width(tabWidth)
                        .height(dockHeightDp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BrandPrimaryBlue,
                                    Color(0xFF1E40AF)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x8893C5FD),
                                    Color(0x111E40AF)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    // Innovative Upward Beam / Projection Accent pointing towards content area
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(18.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(ElectricBrandCyan)
                    )
                }
            }

            // 2. Interactive Row of Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        with(density) {
                            dockWidthDp = size.width.toDp()
                            dockHeightDp = size.height.toDp()
                        }
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (label, icon, _) ->
                    val isSelected = currentTab == index
                    val animatedItemColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else TextSubtle,
                        animationSpec = tween(durationMillis = 180),
                        label = "dockItemColor"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "dockIconScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .bouncyClick { onTabSelect(index) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = animatedItemColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = animatedItemColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
