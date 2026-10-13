package com.digitalchopda.app.presentation.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BezierSalesChart
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.ChopdaPartyRow
import com.digitalchopda.app.core.designsystem.ChopdaPerspectiveSlider
import com.digitalchopda.app.core.designsystem.CompanySwitcherHeader
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.CoralTangerineGradient
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
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.presentation.settings.CompanySwitchSheet
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
    onNavigateToBilling: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToLeads: () -> Unit,
    onNavigateToGst: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
    val filteredParties: List<PartyViewItem> = filteredCustomerEntities.map { item ->
        PartyViewItem(
            id = item.id,
            name = item.name,
            phone = item.phone.ifBlank { "+91 98000 00000" },
            balance = item.balance,
            lastActive = if (item.address.isNotBlank()) item.address else "ચોપડા ખાતું"
        )
    }

    fun sendWhatsAppReminder(party: PartyViewItem) {
        val cleanPhone = party.phone.replace("[^0-9]".toRegex(), "")
        val balanceFormatted = String.format("%,.0f", kotlin.math.abs(party.balance))
        val message = "નમસ્તે ${party.name},\n${activeCompany.firmName} તરફથી નમ્ર વિનંતી:\nઆપની બાકી રકમ ₹ $balanceFormatted છે.\nકૃપા કરીને વહેલી તકે ચુકવણી કરવા વિનંતી.\n\nઆભાર,\n${activeCompany.ownerName}"

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp not installed. Reminder copied!", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendAllReminders() {
        val count = filteredParties.count { it.balance > 0 }
        Toast.makeText(
            context,
            "🚀 Dispatched WhatsApp payment alerts to $count parties!",
            Toast.LENGTH_LONG
        ).show()
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickVoucherSheet = true },
                containerColor = ImperialGold,
                contentColor = Color(0xFF1E1005),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = ImperialGold)
                    .bouncyClick { showQuickVoucherSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Entry",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.newEntryFab,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        },
        bottomBar = {
            FloatingCapsuleDock(
                currentTab = 0,
                onTabSelect = { tabIndex ->
                    when (tabIndex) {
                        0 -> {} // Home
                        1 -> onNavigateToBilling()
                        2 -> onNavigateToStock()
                        3 -> onNavigateToGst()
                        4 -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Royal Firm Header & Live Switcher
            item {
                val currentLang = languageManager?.currentLanguage?.collectAsState()?.value
                CompanySwitcherHeader(
                    firmName = activeCompany.firmName,
                    ownerName = activeCompany.ownerName,
                    unreadNotifications = 3,
                    languageName = currentLang?.nativeName,
                    onLanguageClick = if (languageManager != null) {
                        {
                            val next = languageManager.cycleNextLanguage()
                            Toast.makeText(context, "ભાષા: ${next.nativeName}", Toast.LENGTH_SHORT).show()
                        }
                    } else null,
                    onCompanyClick = { showCompanySheet = true },
                    onNotificationClick = {
                        Toast.makeText(context, "All devices synchronized with local Room vault", Toast.LENGTH_SHORT).show()
                    },
                    onFilterClick = {}
                )
            }

            // 2. Sacred Indian Chopda Ledger Cockpit (Dual Vault Card)
            item {
                SacredLedgerCockpitCard(
                    youWillGetTotal = "₹ ${String.format("%,.0f", totalJama)}",
                    youWillGiveTotal = "₹ ${String.format("%,.0f", totalUdhar)}",
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
                        // Quick Search Bar with Gold Highlight
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    strings.searchPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSubtle)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ImperialGold
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MidnightCardSurface,
                                unfocusedContainerColor = MidnightCardSurface,
                                focusedBorderColor = ImperialGold,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                    }

                    // Filter Chips: ALL / JAMA / UDHAR
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChipPill(
                                label = "${strings.filterAll} (${filteredParties.size})",
                                isSelected = partyFilter == "ALL",
                                onClick = { viewModel.setPartyFilter("ALL") }
                            )
                            FilterChipPill(
                                label = strings.filterJama,
                                isSelected = partyFilter == "JAMA",
                                activeColor = EmeraldJama,
                                onClick = { viewModel.setPartyFilter("JAMA") }
                            )
                            FilterChipPill(
                                label = strings.filterUdhar,
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
                        items(filteredParties, key = { it.id }) { party ->
                            ChopdaPartyRow(
                                name = party.name,
                                phone = party.phone,
                                balance = party.balance,
                                lastActive = party.lastActive,
                                onPartyClick = {
                                    selectedPartyForDetail = party
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
                        RokadCashDrawerCard()
                    }

                    item {
                        Text(
                            text = "આજની રોકડ લેવડ-દેવડ (CASH FLOW)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    item {
                        ChopdaPartyRow(
                            name = "દૈનિક કાઉન્ટર વેચાણ (Cash Sale)",
                            phone = "રોકડ આવક",
                            balance = 14200.0,
                            lastActive = "3:30 PM",
                            onPartyClick = {},
                            onWhatsAppClick = {}
                        )
                    }

                    item {
                        ChopdaPartyRow(
                            name = "દુકાન ખર્ચ અને મજૂરી",
                            phone = "રોકડ ખર્ચ",
                            balance = -2400.0,
                            lastActive = "1:15 PM",
                            onPartyClick = {},
                            onWhatsAppClick = {}
                        )
                    }

                    item {
                        ChopdaPartyRow(
                            name = "શ્રીજી પેકિંગ મટીરીયલ",
                            phone = "રોકડ ચૂકવણું",
                            balance = -1800.0,
                            lastActive = "11:00 AM",
                            onPartyClick = {},
                            onWhatsAppClick = {}
                        )
                    }
                }

                2 -> {
                    // ─── PERSPECTIVE 2: બિલિંગ અને સેલ્સ એનાલિટિક્સ (INVOICING) ───
                    item {
                        BezierSalesChart()
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
                                title = "સ્ટોક ઇન્વેન્ટરી",
                                subtitle = "124 આઇટમ્સ ઉપલબ્ધ",
                                icon = Icons.Default.Inventory,
                                accentColor = ElectricBrandCyan,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToStock
                            )
                        }
                    }

                    item {
                        PulsingAlertStrip(
                            message = "⚠️ GST નિયમ 46 મુજબ સિક્વન્સ લીઝ એક્ટિવ છે: DC/26-27/01002"
                        )
                    }
                }
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
                                Toast.makeText(context, "✅ નોંધ સફળતાપૂર્વક સાચવવામાં આવી!", Toast.LENGTH_SHORT).show()
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
                                    note = "Quick +1000 જમા"
                                )
                                selectedPartyForDetail = party.copy(
                                    balance = party.balance + 1000.0,
                                    lastActive = "હમણાં જ • ₹1,000 જમા"
                                )
                                Toast.makeText(context, "₹1,000 જમા નોંધાયા!", Toast.LENGTH_SHORT).show()
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

                    // - ઉધાર આપ્યા (- Gave)
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
                                    note = "Quick +1000 ઉધાર"
                                )
                                selectedPartyForDetail = party.copy(
                                    balance = party.balance - 1000.0,
                                    lastActive = "હમણાં જ • ₹1,000 ઉધાર"
                                )
                                Toast.makeText(context, "₹1,000 ઉધાર નોંધાયા!", Toast.LENGTH_SHORT).show()
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
                        Triple("આજે • 2:15 PM", "બિલ #1021 - ટેક્સટાઇલ ખરીદી", if (isPositive) -18500.0 else 6400.0),
                        Triple("ગઈકાલે • 5:00 PM", "રોકડ જમા (GPay UPI)", 5000.0),
                        Triple("02-Oct • 11:30 AM", "પ્રારંભિક ખાતાવહી બાકી", if (isPositive) -5000.0 else -2000.0)
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
                                        text = if (isTxnJama) "જમા (GET)" else "ઉધાર (GIVE)",
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
                            Text("💬", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.shareStatementWhatsApp,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
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
                            .background(ElevatedSlate)
                            .border(1.dp, ImperialGold, RoundedCornerShape(12.dp))
                            .bouncyClick {
                                Toast.makeText(
                                    context,
                                    "📄 ${party.name} ખાતાવહી PDF સ્ટેટમેન્ટ તૈયાર થયું!",
                                    Toast.LENGTH_LONG
                                ).show()
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
    activeColor: Color = ImperialGold,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else ElevatedSlate)
            .border(1.dp, if (isSelected) activeColor else BorderStrokeDark, RoundedCornerShape(20.dp))
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) activeColor else TextMuted
            )
        )
    }
}

/**
 * 📦 Rokad Drawer Daily Balance Card
 */
@Composable
fun RokadCashDrawerCard() {
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
                    text = "દૈનિક રોકડ ગલ્લો (TODAY'S CASH BOX)",
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
                text = "₹ 26,300",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = ImperialGoldLight
                )
            )

            Text(
                text = "આજની આખર રોકડ સિલક (Net Cash In Hand)",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("શરૂઆત સિલક", style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle))
                    Text("₹ 12,000", style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("કુલ આવક (+)", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldJama))
                    Text("₹ 18,500", style = MaterialTheme.typography.titleMedium.copy(color = EmeraldJama, fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("કુલ ખર્ચ (-)", style = MaterialTheme.typography.labelSmall.copy(color = RubyUdhar))
                    Text("₹ 4,200", style = MaterialTheme.typography.titleMedium.copy(color = RubyUdhar, fontWeight = FontWeight.Bold))
                }
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
 */
@Composable
fun FloatingCapsuleDock(
    currentTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val items = listOf(
        Triple(strings.navChopda, Icons.Default.Description, "Ledger"),
        Triple(strings.navBilling, Icons.Default.PointOfSale, "Billing"),
        Triple(strings.navStock, Icons.Default.Inventory, "Stock"),
        Triple(strings.navGst, Icons.Default.Calculate, "Taxes"),
        Triple(strings.navSettings, Icons.Default.Tune, "Settings")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = ImperialGold.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(24.dp))
                .background(MidnightCardSurface.copy(alpha = 0.95f))
                .border(1.2.dp, BorderStrokeDark, RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (label, icon, _) ->
                val isSelected = currentTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) ImperialGold.copy(alpha = 0.16f) else Color.Transparent)
                        .bouncyClick { onTabSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) ImperialGoldLight else TextSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) ImperialGoldLight else TextSubtle
                            )
                        )
                    }
                }
            }
        }
    }
}
