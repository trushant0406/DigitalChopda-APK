package com.digitalchopda.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.GoldFoilButton
import com.digitalchopda.app.core.designsystem.GoldFoilGradient
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.domain.model.Company
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySwitchSheet(
    companyRepository: CompanyRepository,
    currentActiveCompanyId: Long,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit,
    onCompanySelected: (Company) -> Unit
) {
    val companies by companyRepository.getAllCompanies().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightCardSurface,
        scrimColor = MidnightCardSurface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MANAGE BUSINESSES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImperialGold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Switch Active Company",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Companies List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(companies) { comp ->
                    val isActive = comp.id == currentActiveCompanyId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) ElevatedSlate else MidnightCardSurface)
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) ImperialGold else BorderStrokeDark,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                scope.launch {
                                    companyRepository.switchActiveCompany(comp.id)
                                    onCompanySelected(comp)
                                    onDismiss()
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Avatar / Logo initial
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) ImperialGold.copy(alpha = 0.2f) else ElevatedSlate)
                                        .border(
                                            1.dp,
                                            if (isActive) ImperialGold else BorderStrokeLight,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = comp.firmName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) ImperialGold else TextWhite
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = comp.firmName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (comp.gstin.isNotEmpty()) "GSTIN: ${comp.gstin}" else "Prefix: ${comp.invoicePrefix} • ${comp.financialYear}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                            }

                            if (isActive) {
                                StatusPill(
                                    text = "ACTIVE",
                                    textColor = SuccessGreen,
                                    backgroundColor = SuccessGreen.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add Company Button
            GoldFoilButton(
                text = "+ ADD NEW COMPANY / FIRM",
                icon = Icons.Default.Add,
                onClick = { showAddDialog = true }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showAddDialog) {
        AddCompanyDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newComp ->
                scope.launch {
                    val newId = companyRepository.addCompany(newComp)
                    companyRepository.switchActiveCompany(newId)
                    onCompanySelected(newComp.copy(id = newId))
                    showAddDialog = false
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun AddCompanyDialog(
    onDismiss: () -> Unit,
    onSave: (Company) -> Unit
) {
    var firmName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("Amit Sharma") }
    var gstin by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91 ") }
    var address by remember { mutableStateOf("") }
    var invoicePrefix by remember { mutableStateOf("DC") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = {
            Text(
                text = "Add New Company / Vyapar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = firmName,
                    onValueChange = { firmName = it },
                    label = { Text("Firm / Trade Name", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it.uppercase() },
                    label = { Text("GSTIN (Optional)", color = TextMuted) },
                    placeholder = { Text("24AAAAA0000A1Z5", color = TextSubtle) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = invoicePrefix,
                    onValueChange = { invoicePrefix = it.uppercase() },
                    label = { Text("Invoice Prefix", color = TextMuted) },
                    placeholder = { Text("e.g. DC", color = TextSubtle) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop / Office Address", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            GoldFoilButton(
                text = "SAVE FIRM",
                enabled = firmName.isNotBlank(),
                onClick = {
                    onSave(
                        Company(
                            firmName = firmName.trim(),
                            ownerName = ownerName.trim(),
                            gstin = gstin.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            invoicePrefix = invoicePrefix.trim().ifEmpty { "DC" },
                            isActive = true
                        )
                    )
                }
            )
        },
        dismissButton = {
            Text(
                text = "CANCEL",
                color = TextMuted,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(12.dp)
            )
        }
    )
}
