package com.digitalchopda.app.presentation.party

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.digitalchopda.app.core.designsystem.EmptyStateCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.designsystem.SyncStatusPill
import com.digitalchopda.app.core.network.InstantNetworkSyncMonitor
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch
import kotlin.math.abs


private fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex())
    return when {
        parts.isEmpty() || parts[0].isEmpty() -> "P"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyScreen(
    customerRepository: CustomerRepository,
    companyRepository: CompanyRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToPartyDetail: (partyId: Long, initialTab: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeCompany by remember {
        mutableStateOf(
            Company(
                id = 1L,
                firmName = "ABC E-COMMERCE",
                ownerName = "Admin User",
                isActive = true
            )
        )
    }

    LaunchedEffect(Unit) {
        val company = companyRepository.getActiveCompany()
        activeCompany = company
    }

    val allParties by customerRepository.getCustomersForCompany(activeCompany.id).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CUSTOMERS, SUPPLIERS, TO_RECEIVE, TO_PAY
    var isSyncing by remember { mutableStateOf(false) }
    var showAddPartyDialog by remember { mutableStateOf(false) }

    val isOnline by InstantNetworkSyncMonitor.isOnline.collectAsState()
    val isSyncingOutbox by InstantNetworkSyncMonitor.isSyncing.collectAsState()
    val pendingSyncCount by remember { DigitalChopdaApplication.instance.billingRepository.observePendingSyncCount() }.collectAsState(initial = 0)

    // Auto-sync on screen launch
    LaunchedEffect(activeCompany.id) {
        if (activeCompany.id > 0) {
            isSyncing = true
            try {
                customerRepository.syncContactsWithServer(activeCompany.id)
            } catch (_: Exception) {
            } finally {
                isSyncing = false
            }
        }
    }

    fun triggerSync() {
        InstantNetworkSyncMonitor.triggerManualSync(context)
        scope.launch {
            isSyncing = true
            val result = customerRepository.syncContactsWithServer(activeCompany.id)
            isSyncing = false
            if (result.isSuccess) {
                Toast.makeText(context, "✓ Parties synced from Central Depot (${result.getOrNull()} contacts)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Sync note: Working in offline mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filter parties
    val filteredParties = remember(allParties, searchQuery, selectedFilter) {
        allParties.filter { party ->
            // Search query filter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    party.name.lowercase().contains(q) ||
                    party.phone.contains(q) ||
                    party.city.lowercase().contains(q)

            if (!matchesQuery) return@filter false

            // Type filter
            when (selectedFilter) {
                "CUSTOMERS" -> party.partyType.equals("customer", ignoreCase = true)
                "SUPPLIERS" -> party.partyType.equals("supplier", ignoreCase = true)
                "TO_RECEIVE" -> party.balance > 0.01
                "TO_PAY" -> party.balance < -0.01
                else -> true
            }
        }
    }

    // Totals calculations
    val totalReceivable = remember(allParties) {
        allParties.filter { it.balance > 0 }.sumOf { it.balance }
    }
    val totalPayable = remember(allParties) {
        allParties.filter { it.balance < 0 }.sumOf { abs(it.balance) }
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Parties (Khata)",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = DarkNavy,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandAccentSoft)
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${allParties.size}",
                                    color = BrandPrimaryBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${activeCompany.firmName.ifBlank { "DigitalChopda" }} • Ledger Management",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                },
                actions = {
                    SyncStatusPill(
                        pendingCount = pendingSyncCount,
                        isOnline = isOnline,
                        isSyncing = isSyncingOutbox || isSyncing,
                        onSyncClick = { triggerSync() },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    IconButton(
                        onClick = { triggerSync() },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElevatedSlate)
                            .border(1.dp, BorderStrokeDark, CircleShape)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = BrandPrimaryBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Sync Contacts",
                                tint = BrandPrimaryBlue,
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPartyDialog = true },
                containerColor = BrandPrimaryBlue,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Party",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add Party",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.5.sp
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .semantics(mergeDescendants = true) { contentDescription = "Add Party" }
                    .padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search party by name, mobile, city...", fontSize = 13.sp, color = TextSubtle) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandPrimaryBlue, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy,
                    focusedContainerColor = MidnightCardSurface,
                    unfocusedContainerColor = MidnightCardSurface,
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (Strictly 3 chips)
            val filters = listOf(
                "ALL" to "All",
                "CUSTOMERS" to "Customers",
                "SUPPLIERS" to "Suppliers"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filters.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandPrimaryBlue else ElevatedSlate)
                            .border(1.dp, if (isSelected) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = key }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else DarkNavy,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Party List
            if (filteredParties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = if (searchQuery.isNotBlank()) "No party matching '$searchQuery'" else "No parties in catalog yet",
                        subtitle = if (searchQuery.isNotBlank()) "Try checking the spelling or phone number" else "Create your first customer to start tracking khata balances & payments",
                        icon = Icons.Default.People,
                        actionText = if (searchQuery.isBlank()) "+ Add First Party" else null,
                        onActionClick = if (searchQuery.isBlank()) { { showAddPartyDialog = true } } else null
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyCardItem(
                            party = party,
                            onClick = {
                                onNavigateToPartyDetail(party.id, 0)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Party Dialog
    if (showAddPartyDialog) {
        AddEditPartyDialog(
            companyId = activeCompany.id,
            onDismiss = { showAddPartyDialog = false },
            onSave = { newParty ->
                scope.launch {
                    val res = customerRepository.createParty(newParty)
                    showAddPartyDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "✓ Party '${newParty.name}' added successfully!", Toast.LENGTH_SHORT).show()
                        customerRepository.syncContactsWithServer(activeCompany.id)
                    } else {
                        Toast.makeText(context, "Failed to save: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun PartyCardItem(
    party: CustomerEntity,
    onClick: () -> Unit
) {
    val (avatarBg, avatarText) = remember(party.name) { getAvatarColors(party.name) }
    val initials = remember(party.name) { getInitials(party.name) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
            .bouncyClick { onClick() },
        color = MidnightCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = avatarText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = party.name,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = party.phone,
                        color = TextSubtle,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    val cleanCity = party.city.trim().trimEnd(',', ' ')
                    if (cleanCity.isNotBlank()) {
                        Text(
                            text = "• $cleanCity",
                            color = TextSubtle,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val isSupplier = party.partyType.equals("supplier", ignoreCase = true)
                    val tagText = party.partyTag.ifBlank { if (isSupplier) "Supplier" else "Wholesale" }
                    Text(
                        text = "•",
                        color = TextSubtle,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSupplier) Color(0xFFFEF3C7) else BrandAccentSoft)
                            .border(0.8.dp, if (isSupplier) Color(0xFFFDE68A) else Color(0xFFBFDBFE), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = tagText,
                            color = if (isSupplier) Color(0xFF92400E) else BrandPrimaryBlue,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Outstanding Balance
            Column(horizontalAlignment = Alignment.End) {
                val bal = party.balance
                val (balColor, balLabel) = when {
                    bal > 0.01 -> EmeraldJama to "You'll Get"
                    bal < -0.01 -> RubyUdhar to "You'll Give"
                    else -> TextSubtle to "Settled"
                }

                Text(
                    text = CurrencyFormatter.formatCurrency(abs(bal)),
                    color = balColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End
                )
                Text(
                    text = balLabel,
                    color = balColor.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View Party",
                tint = TextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
