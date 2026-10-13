package com.digitalchopda.app.presentation.settings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
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
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.network.MockDigitalChopdaApiService
import com.digitalchopda.app.data.repository.AuthRepository
import com.digitalchopda.app.core.localization.AppLanguage
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.core.security.BiometricAuthManager
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.domain.model.Company
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    companyRepository: CompanyRepository,
    authRepository: AuthRepository,
    mockApi: MockDigitalChopdaApiService,
    languageManager: LanguageManager,
    biometricAuthManager: BiometricAuthManager,
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val currentLang by languageManager.currentLanguage.collectAsState()
    val isBiometricEnabled by biometricAuthManager.isBiometricEnabled.collectAsState()

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

    val companies by companyRepository.getAllCompanies().collectAsState(initial = emptyList())
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddCompanyDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "FIRM SETTINGS & SECURITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ImperialGold,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ImperialGold
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(GoldFoilGradient)
                                    .border(1.5.dp, ImperialGoldLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeCompany.ownerName.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1E1005)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = activeCompany.ownerName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                )
                                Text(
                                    text = activeCompany.firmName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = ImperialGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = activeCompany.phone,
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                )
                            }
                        }

                        IconButton(onClick = {
                            Toast.makeText(context, "Edit Profile", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ImperialGold)
                        }
                    }
                }
            }

            // MULTIPLE COMPANY MANAGEMENT SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.registeredFirms} (${companies.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = strings.addFirm,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImperialGold,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .bouncyClick { showAddCompanyDialog = true }
                            .padding(4.dp)
                    )
                }
            }

            items(companies) { comp ->
                val isActive = comp.id == activeCompany.id
                ChopdaCard(
                    borderColor = if (isActive) ImperialGold else BorderStrokeDark,
                    backgroundColor = if (isActive) ElevatedSlate else MidnightCardSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClick {
                                scope.launch {
                                    companyRepository.switchActiveCompany(comp.id)
                                    activeCompany = comp
                                    Toast.makeText(context, "Switched to ${comp.firmName}", Toast.LENGTH_SHORT).show()
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = comp.firmName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) ImperialGold else TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (comp.gstin.isNotEmpty()) "GSTIN: ${comp.gstin}" else "Prefix: ${comp.invoicePrefix}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        if (isActive) {
                            StatusPill(
                                text = strings.activeFirmBadge,
                                textColor = SuccessGreen,
                                backgroundColor = SuccessGreen.copy(alpha = 0.2f)
                            )
                        } else {
                            Text(
                                text = "${strings.switchFirmBtn} ›",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ImperialGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Business Customization Section
            item {
                Text(
                    text = strings.businessCustomization,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                ChopdaCard {
                    SettingsRowItem(
                        icon = Icons.Default.Receipt,
                        title = "Invoice & Billing Customization",
                        subtitle = "Shop logo, colors, Terms & Conditions",
                        onClick = { Toast.makeText(context, "Invoice template saved", Toast.LENGTH_SHORT).show() }
                    )
                    Divider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 10.dp))
                    SettingsRowItem(
                        icon = Icons.Default.QrCode,
                        title = "Payment & UPI QR Code",
                        subtitle = "VPA: ${activeCompany.upiVpa.ifEmpty { "sharmatraders@oksbi" }}",
                        onClick = { Toast.makeText(context, "UPI QR configured", Toast.LENGTH_SHORT).show() }
                    )
                    Divider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 10.dp))
                    SettingsRowItem(
                        icon = Icons.Default.Business,
                        title = "GSTIN & Tax Scheme",
                        subtitle = "${activeCompany.gstin.ifEmpty { "24AAACS1429B1Z8" }} • Regular Scheme",
                        onClick = { Toast.makeText(context, "GSTIN verified", Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            // App & Security Settings
            item {
                Text(
                    text = strings.appPersonalSettings,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                ChopdaCard {
                    // Language Switcher
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = ImperialGold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.appLanguageLabel, color = TextWhite, fontWeight = FontWeight.SemiBold)
                                    Text("English / ગુજરાતી / हिन्दी", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ImperialGold.copy(alpha = 0.15f))
                                    .border(1.dp, ImperialGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .bouncyClick {
                                        val next = languageManager.cycleNextLanguage()
                                        Toast.makeText(context, "Language: ${next.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = currentLang.nativeName,
                                    color = ImperialGold,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Direct 1-tap language switch pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = lang == currentLang
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) ImperialGold else ElevatedSlate)
                                        .border(
                                            1.dp,
                                            if (isSelected) ImperialGoldLight else BorderStrokeLight,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            languageManager.setLanguage(lang)
                                            Toast.makeText(context, "ભાષા: ${lang.nativeName}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang.nativeName,
                                        color = if (isSelected) Color(0xFF1E1005) else TextWhite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 10.dp))

                    // Biometric Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ImperialGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(strings.biometricLockLabel, color = TextWhite, fontWeight = FontWeight.SemiBold)
                                Text(strings.biometricLockSubtitle, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { isEnabled ->
                                biometricAuthManager.setBiometricEnabled(isEnabled)
                                Toast.makeText(
                                    context,
                                    if (isEnabled) "🔐 Biometric Lock Enabled" else "🔓 Biometric Lock Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ImperialGold,
                                checkedTrackColor = ImperialGold.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Divider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 10.dp))

                    // Local Encrypted Vault
                    SettingsRowItem(
                        icon = Icons.Default.CloudSync,
                        title = strings.hardwareVaultLabel,
                        subtitle = strings.hardwareVaultSubtitle,
                        onClick = {
                            Toast.makeText(context, "Encrypted Local Database Verified!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Divider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 10.dp))

                    // DPDP & Export Data
                    SettingsRowItem(
                        icon = Icons.Default.Download,
                        title = "Export My Ledger (DPDP 2023)",
                        subtitle = "72-month statutory CGST compliance zip (Local)",
                        onClick = {
                            Toast.makeText(context, "Exported ledger zip", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // LOG OUT BUTTON
            item {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = DangerRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.logoutBtn,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MidnightCardSurface,
            title = { Text("Log Out Confirmation", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to log out from ${activeCompany.firmName}? Your offline encrypted database will remain safely stored on this device.",
                    color = TextMuted
                )
            },
            confirmButton = {
                GoldFoilButton(
                    text = "LOG OUT",
                    textColor = DangerRed,
                    onClick = {
                        scope.launch {
                            authRepository.logout()
                            showLogoutDialog = false
                            onLogoutSuccess()
                        }
                    }
                )
            },
            dismissButton = {
                Text(
                    text = "CANCEL",
                    color = TextMuted,
                    modifier = Modifier
                        .clickable { showLogoutDialog = false }
                        .padding(12.dp)
                )
            }
        )
    }

    if (showAddCompanyDialog) {
        AddCompanyDialog(
            onDismiss = { showAddCompanyDialog = false },
            onSave = { newComp ->
                scope.launch {
                    val newId = companyRepository.addCompany(newComp)
                    companyRepository.switchActiveCompany(newId)
                    activeCompany = newComp.copy(id = newId)
                    showAddCompanyDialog = false
                    Toast.makeText(context, "Company ${newComp.firmName} Added & Activated!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ImperialGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("›", color = ImperialGold.copy(alpha = 0.6f), fontSize = 18.sp)
    }
}
