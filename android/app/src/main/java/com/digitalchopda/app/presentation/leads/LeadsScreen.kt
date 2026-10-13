package com.digitalchopda.app.presentation.leads

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElectricBrandCyan
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.LeadRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Lead
import com.digitalchopda.app.domain.model.LeadPriority
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    leadRepository: LeadRepository,
    companyRepository: CompanyRepository,
    onNavigateBack: () -> Unit
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

    LaunchedEffect(Unit) {
        activeCompany = companyRepository.getActiveCompany()
    }

    val leads by leadRepository.getLeadsForCompany(activeCompany.id).collectAsState(initial = emptyList())
    var selectedStage by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddLeadDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val stages = listOf("All", "Hot", "Warm", "Cold", "Converted")

    val filteredLeads = leads.filter { lead ->
        val matchesStage = when (selectedStage) {
            "All" -> true
            "Hot" -> lead.priority == LeadPriority.HOT
            "Warm" -> lead.priority == LeadPriority.WARM
            "Cold" -> lead.priority == LeadPriority.COLD
            "Converted" -> lead.priority == LeadPriority.CONVERTED
            else -> true
        }
        val matchesSearch = lead.customerName.contains(searchQuery, ignoreCase = true) ||
                lead.businessName.contains(searchQuery, ignoreCase = true)
        matchesStage && matchesSearch
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Leads & Deal Pipeline",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top CTA: + Add Lead
            item {
                CoralGradientButton(
                    text = "+ ADD NEW PROSPECT / LEAD",
                    icon = Icons.Default.Add,
                    onClick = { showAddLeadDialog = true }
                )
            }

            // Metrics Banner: Total Leads (84), Active Leads (18)
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL LEADS PIPELINE",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "84 Leads",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ElectricBrandCyan
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ACTIVE DEALS",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StatusPill(
                                text = "18 ACTIVE",
                                textColor = SuccessGreen,
                                backgroundColor = SuccessGreen.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search prospect by name or business...", color = TextSubtle) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ElectricBrandCyan
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightCardSurface,
                        unfocusedContainerColor = MidnightCardSurface,
                        focusedBorderColor = ElectricBrandCyan,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Stage Filter Bar
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(stages) { stage ->
                        val isSelected = selectedStage == stage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CoralPrimary else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) CoralPrimary else BorderStrokeLight,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedStage = stage }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stage,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) TextWhite else TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Leads List
            items(filteredLeads) { lead ->
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lead.customerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${lead.businessName} • ${lead.phone}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        // Priority Badge
                        val badgeColor = when (lead.priority) {
                            LeadPriority.HOT -> DangerRed
                            LeadPriority.WARM -> WarningAmber
                            LeadPriority.COLD -> ElectricBrandCyan
                            LeadPriority.CONVERTED -> SuccessGreen
                        }
                        StatusPill(
                            text = "${lead.priority.name} LEAD",
                            textColor = badgeColor,
                            backgroundColor = badgeColor.copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Requirement details
                    Text(
                        text = lead.requirementDetails,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Expected Deal",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Text(
                                text = "₹ ${lead.expectedDealValue.toInt()}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBrandCyan
                                )
                            )
                        }

                        // Quick Actions: Phone & WhatsApp
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Call Action
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSlate)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${lead.phone}")
                                        }
                                        context.startActivity(intent)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // WhatsApp Action
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSlate)
                                    .clickable {
                                        val cleanPhone = lead.phone.replace("[^0-9]".toRegex(), "")
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/$cleanPhone?text=Namaste%20${Uri.encode(lead.customerName)},%20regarding%20your%20requirement%20from%20DigitalChopda.")
                                        }
                                        context.startActivity(intent)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💬", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLeadDialog) {
        AddNewLeadDialog(
            companyId = activeCompany.id,
            onDismiss = { showAddLeadDialog = false },
            onAdd = { newLead ->
                scope.launch {
                    leadRepository.addLead(newLead)
                    showAddLeadDialog = false
                }
            }
        )
    }
}

@Composable
fun AddNewLeadDialog(
    companyId: Long,
    onDismiss: () -> Unit,
    onAdd: (Lead) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var firm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91 ") }
    var requirement by remember { mutableStateOf("") }
    var dealValueStr by remember { mutableStateOf("50000") }
    var selectedPriority by remember { mutableStateOf(LeadPriority.HOT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = { Text("Add New Lead / Prospect", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name", color = TextMuted) },
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
                    value = firm,
                    onValueChange = { firm = it },
                    label = { Text("Business / Firm Name", color = TextMuted) },
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
                    value = requirement,
                    onValueChange = { requirement = it },
                    label = { Text("Requirement Details", color = TextMuted) },
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
                    value = dealValueStr,
                    onValueChange = { dealValueStr = it },
                    label = { Text("Expected Deal Value (₹)", color = TextMuted) },
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
                text = "SAVE LEAD",
                enabled = name.isNotBlank(),
                onClick = {
                    onAdd(
                        Lead(
                            companyId = companyId,
                            customerName = name,
                            businessName = firm.ifEmpty { "Individual Buyer" },
                            phone = phone,
                            priority = selectedPriority,
                            requirementDetails = requirement,
                            expectedDealValue = dealValueStr.toDoubleOrNull() ?: 10000.0
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
