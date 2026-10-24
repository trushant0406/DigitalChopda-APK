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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import com.digitalchopda.app.core.designsystem.CoralGradientButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.bouncyClick
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

    LaunchedEffect(Unit) {
        try {
            companyRepository.syncCompaniesWithServer()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightCardSurface,
        scrimColor = DarkNavy.copy(alpha = 0.4f),
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
                            color = BrandPrimaryBlue,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Switch Active Company",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Companies List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(companies, key = { it.id }, contentType = { "company_item" }) { comp ->
                    val isActive = comp.id == currentActiveCompanyId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActive) ElevatedSlate else MidnightCardSurface)
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) BrandPrimaryBlue else BorderStrokeDark,
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
                                        .background(if (isActive) BrandPrimaryBlue else ElevatedSlate)
                                        .border(
                                            1.dp,
                                            if (isActive) BrandPrimaryBlue else BorderStrokeDark,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = comp.firmName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) Color.White else DarkNavy
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = comp.firmName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (comp.gstin.isNotEmpty()) "GSTIN: ${comp.gstin}" else "Prefix: ${comp.invoicePrefix} • ${comp.financialYear}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle)
                                    )
                                }
                            }

                            if (isActive) {
                                StatusPill(
                                    text = "ACTIVE",
                                    textColor = EmeraldJama,
                                    backgroundColor = EmeraldJama.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add Company Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPrimaryBlue)
                    .bouncyClick { showAddDialog = true },
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
                        text = "ADD NEW COMPANY / FIRM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

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
fun AddEditCompanyDialog(
    initialCompany: Company? = null,
    onDismiss: () -> Unit,
    onSave: (Company) -> Unit
) {
    val isEditing = initialCompany != null
    var firmName by remember { mutableStateOf(initialCompany?.firmName ?: "") }
    var ownerName by remember { mutableStateOf(initialCompany?.ownerName ?: "Amit Sharma") }
    var gstin by remember { mutableStateOf(initialCompany?.gstin ?: "") }
    var phone by remember { mutableStateOf(initialCompany?.phone ?: "+91 ") }
    var address by remember { mutableStateOf(initialCompany?.address ?: "") }
    var invoicePrefix by remember { mutableStateOf(initialCompany?.invoicePrefix ?: "DC") }
    var upiVpa by remember { mutableStateOf(initialCompany?.upiVpa ?: "") }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Edit Company / Vyapar" else "Add New Company / Vyapar",
        subtitle = if (isEditing) "Update profile & tax compliance details" else "Create separate book & invoice series",
        icon = Icons.Default.Business,
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
                        text = if (isEditing) "SAVE CHANGES" else "SAVE FIRM",
                        enabled = firmName.isNotBlank(),
                        onClick = {
                            onSave(
                                Company(
                                    id = initialCompany?.id ?: 0L,
                                    firmName = firmName.trim(),
                                    ownerName = ownerName.trim(),
                                    gstin = gstin.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    invoicePrefix = invoicePrefix.trim().ifEmpty { "DC" },
                                    isActive = initialCompany?.isActive ?: true,
                                    upiVpa = upiVpa.trim(),
                                    financialYear = initialCompany?.financialYear ?: "26-27",
                                    currencySymbol = initialCompany?.currencySymbol ?: "₹",
                                    createdAt = initialCompany?.createdAt ?: System.currentTimeMillis()
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
            OutlinedTextField(
                value = firmName,
                onValueChange = { firmName = it },
                label = { Text("Firm / Trade Name *", color = TextSubtle) },
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
                value = gstin,
                onValueChange = { gstin = it.uppercase() },
                label = { Text("GSTIN (Optional)", color = TextSubtle) },
                placeholder = { Text("24AAAAA0000A1Z5", color = TextMuted) },
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
                value = invoicePrefix,
                onValueChange = { invoicePrefix = it.uppercase() },
                label = { Text("Invoice Prefix", color = TextSubtle) },
                placeholder = { Text("e.g. DC", color = TextMuted) },
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
                value = address,
                onValueChange = { address = it },
                label = { Text("Shop / Office Address", color = TextSubtle) },
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
                value = upiVpa,
                onValueChange = { upiVpa = it.trim() },
                label = { Text("UPI Payment ID / VPA (Optional)", color = TextSubtle) },
                placeholder = { Text("e.g. sharmatraders@oksbi", color = TextMuted) },
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

@Composable
fun AddCompanyDialog(
    onDismiss: () -> Unit,
    onSave: (Company) -> Unit
) = AddEditCompanyDialog(initialCompany = null, onDismiss = onDismiss, onSave = onSave)

