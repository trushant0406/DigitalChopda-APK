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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.LeadRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Lead
import com.digitalchopda.app.domain.model.LeadPriority
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    leadRepository: LeadRepository,
    companyRepository: CompanyRepository,
    customerRepository: com.digitalchopda.app.data.repository.CustomerRepository? = null,
    onNavigateBack: () -> Unit,
    onNavigateToPartyDetail: (partyId: Long, initialTab: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var activeCompany by remember {
        mutableStateOf(
            Company(
                id = 1L,
                firmName = "GUPTA TRADERS",
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
                            color = DarkNavy,
                            fontSize = 18.sp
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics Banner: Total Leads & Active Deals
            item {
                ChopdaCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    borderColor = BorderStrokeDark,
                    backgroundColor = MidnightCardSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL LEADS PIPELINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSubtle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${leads.size.coerceAtLeast(84)} Leads",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPrimaryBlue
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ACTIVE DEALS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSubtle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StatusPill(
                                text = "18 ACTIVE",
                                textColor = EmeraldJama,
                                backgroundColor = EmeraldJama.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }

            // Top CTA: + Add Lead
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandPrimaryBlue)
                        .bouncyClick { showAddLeadDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ ADD NEW PROSPECT / LEAD",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search prospect by name or business...", color = TextSubtle, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightCardSurface,
                        unfocusedContainerColor = MidnightCardSurface,
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
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
                    items(stages, key = { it }) { stage ->
                        val isSelected = selectedStage == stage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BrandPrimaryBlue else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedStage = stage }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stage,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.White else DarkNavy,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Leads List
            items(
                items = filteredLeads,
                key = { it.id },
                contentType = { "lead_card" }
            ) { lead ->
                ChopdaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (customerRepository != null) {
                                scope.launch {
                                    val existing = customerRepository.getCustomerByPhone(activeCompany.id, lead.phone)
                                    val partyId = existing?.id ?: customerRepository.addCustomer(
                                        com.digitalchopda.app.data.local.entity.CustomerEntity(
                                            companyId = activeCompany.id,
                                            name = lead.customerName,
                                            phone = lead.phone,
                                            companyName = lead.businessName
                                        )
                                    )
                                    onNavigateToPartyDetail(partyId, 3)
                                }
                            } else {
                                onNavigateToPartyDetail(lead.id, 3)
                            }
                        },
                    cornerRadius = 14.dp,
                    borderColor = BorderStrokeDark,
                    backgroundColor = MidnightCardSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                        color = DarkNavy,
                                        fontSize = 15.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${lead.businessName} • ${lead.phone}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSubtle,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            // Priority Badge
                            val (badgeColor, bgColor) = when (lead.priority) {
                                LeadPriority.HOT -> Pair(RubyUdhar, RubyUdhar.copy(alpha = 0.12f))
                                LeadPriority.WARM -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.12f))
                                LeadPriority.COLD -> Pair(BrandPrimaryBlue, BrandPrimaryBlue.copy(alpha = 0.12f))
                                LeadPriority.CONVERTED -> Pair(EmeraldJama, EmeraldJama.copy(alpha = 0.12f))
                            }
                            StatusPill(
                                text = "${lead.priority.name} LEAD",
                                textColor = badgeColor,
                                backgroundColor = bgColor
                            )
                        }

                        if (lead.requirementDetails.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lead.requirementDetails,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = DarkNavy,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Expected Deal",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "₹ ${lead.expectedDealValue.toInt()}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 15.sp
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
                                        tint = EmeraldJama,
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

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Add New Lead / Prospect",
        subtitle = "Track inquiries & deal stages",
        icon = Icons.Default.PersonAdd,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.94f,
        maxHeightFraction = 0.88f,
        scrollable = true,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }

                Box(modifier = Modifier.weight(1.5f)) {
                    CoralGradientButton(
                        text = "SAVE LEAD",
                        enabled = name.isNotBlank(),
                        onClick = {
                            onAdd(
                                Lead(
                                    companyId = companyId,
                                    customerName = name.trim(),
                                    businessName = firm.trim().ifEmpty { "Individual Buyer" },
                                    phone = phone.trim(),
                                    priority = selectedPriority,
                                    requirementDetails = requirement.trim(),
                                    expectedDealValue = dealValueStr.toDoubleOrNull() ?: 10000.0
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Priority Selector Chips
            Text(
                text = "Lead Priority Level",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(LeadPriority.HOT, LeadPriority.WARM, LeadPriority.COLD).forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val (chipColor, label) = when (priority) {
                        LeadPriority.HOT -> Pair(RubyUdhar, "🔥 HOT")
                        LeadPriority.WARM -> Pair(WarningAmber, "⚡ WARM")
                        LeadPriority.COLD -> Pair(BrandPrimaryBlue, "❄️ COLD")
                        LeadPriority.CONVERTED -> Pair(EmeraldJama, "✅ CONVERTED")
                        else -> Pair(BrandPrimaryBlue, "LEAD")
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) chipColor.copy(alpha = 0.15f) else ElevatedSlate)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipColor else BorderStrokeDark,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPriority = priority }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) chipColor else TextSubtle
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact Name *", color = TextSubtle) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                ),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = firm,
                onValueChange = { firm = it },
                label = { Text("Business / Firm Name", color = TextSubtle) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                ),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number", color = TextSubtle) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                ),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = requirement,
                onValueChange = { requirement = it },
                label = { Text("Requirement Details", color = TextSubtle) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                ),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = dealValueStr,
                onValueChange = { dealValueStr = it },
                label = { Text("Expected Deal Value (₹)", color = TextSubtle) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedSlate,
                    unfocusedContainerColor = ElevatedSlate,
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
