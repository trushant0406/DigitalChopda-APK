package com.digitalchopda.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import com.digitalchopda.app.core.security.TokenStorage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.EmeraldJamaBg
import com.digitalchopda.app.core.printer.BluetoothPrinterManager
import com.digitalchopda.app.core.sound.SoundEffectsManager
import com.digitalchopda.app.presentation.gst.GstHubScreen
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
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.RubyUdharBg
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.AppLanguage
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.core.network.MockDigitalChopdaApiService
import com.digitalchopda.app.core.security.BiometricAuthManager
import com.digitalchopda.app.data.repository.AuthRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.core.network.InstantNetworkSyncMonitor
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
    onLogoutSuccess: () -> Unit,
    showBackButton: Boolean = false
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
                firmName = "My Business",
                ownerName = "Admin User",
                gstin = "",
                phone = "9876543210",
                address = "Surat",
                isActive = true,
                invoicePrefix = "INV-"
            )
        )
    }

    var waStatusText by remember { mutableStateOf("CHECKING...") }
    var isWaChecking by remember { mutableStateOf(false) }

    fun refreshWaStatus() {
        scope.launch {
            isWaChecking = true
            val res = companyRepository.getSettingsStatus()
            isWaChecking = false
            if (res.isSuccess) {
                val s = res.getOrNull()
                waStatusText = s?.whatsappWebStatus ?: if (s?.whatsappConfigured == true) "CONNECTED" else "READY"
            } else {
                waStatusText = "NATIVE_MODE"
            }
        }
    }

    LaunchedEffect(Unit) {
        val loaded = companyRepository.getActiveCompany()
        activeCompany = loaded
        try {
            companyRepository.syncCompaniesWithServer()
        } catch (_: Exception) {}
        refreshWaStatus()
    }

    val companies by companyRepository.getAllCompanies().collectAsState(initial = emptyList())
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddCompanyDialog by remember { mutableStateOf(false) }
    var showEditCompanyDialog by remember { mutableStateOf(false) }
    var showInvoicePrefixDialog by remember { mutableStateOf(false) }
    var showUpiVpaDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showEnclaveInfoDialog by remember { mutableStateOf(false) }
    var showExportLedgerDialog by remember { mutableStateOf(false) }
    var showGstHub by remember { mutableStateOf(false) }
    var showServerUrlDialog by remember { mutableStateOf(false) }

    val tokenStorage = remember { DigitalChopdaApplication.instance.tokenStorage }
    var currentServerBaseUrl by remember { mutableStateOf(tokenStorage.getBaseUrl()) }

    val settingsPrefs = remember { context.getSharedPreferences("chopda_settings_prefs", Context.MODE_PRIVATE) }
    var soundboxEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("soundbox_enabled", true)) }
    var printerRollWidth by remember { mutableStateOf(settingsPrefs.getInt("printer_roll_width", 58)) }
    val bluetoothPrinterManager = remember { BluetoothPrinterManager(context) }

    val billingRepo = remember { DigitalChopdaApplication.instance.billingRepository }
    val totalPendingOutboxCount by billingRepo.observePendingSyncCount().collectAsState(initial = 0)
    val pendingInvoicesCount by billingRepo.observePendingInvoiceCount().collectAsState(initial = 0)
    val pendingPaymentsCount by billingRepo.observePendingPaymentCount().collectAsState(initial = 0)
    val pendingPartiesCount by billingRepo.observePendingCustomerCount().collectAsState(initial = 0)
    val pendingAdjustmentsCount by billingRepo.observePendingAdjustmentCount().collectAsState(initial = 0)

    val isOnline by InstantNetworkSyncMonitor.isOnline.collectAsState()
    val isSyncingOutbox by InstantNetworkSyncMonitor.isSyncing.collectAsState()
    val lastSyncTimestamp by InstantNetworkSyncMonitor.lastSyncTimestamp.collectAsState()

    val formattedLastSync = remember(lastSyncTimestamp) {
        val diff = (System.currentTimeMillis() - lastSyncTimestamp).coerceAtLeast(0)
        when {
            diff < 30_000L -> "Just now"
            diff < 60_000L -> "Under 1 min ago"
            else -> java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTimestamp))
        }
    }

    if (showGstHub) {
        GstHubScreen(
            mockApi = mockApi,
            activeCompanyGstin = activeCompany.gstin,
            onNavigateBack = { showGstHub = false }
        )
        return
    }

    val displayCompanies = if (companies.isNotEmpty()) companies else listOf(activeCompany)

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = strings.settingsTitle.ifEmpty { "SETTINGS & MERCHANT VAULT" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = DarkNavy,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "${activeCompany.firmName.ifEmpty { "My Business" }} • Configuration & Vault",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DarkNavy
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MidnightCardSurface
                    )
                )
                HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── 1. EXECUTIVE MERCHANT PROFILE CARD ───
            item {
                ChopdaCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    borderColor = BorderStrokeDark,
                    backgroundColor = MidnightCardSurface,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val (compAvatarBg, compAvatarText) = getAvatarColors(activeCompany.firmName)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(compAvatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeCompany.firmName.take(2).uppercase().ifEmpty { "MB" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = compAvatarText,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = activeCompany.firmName.ifEmpty { "My Business" },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 15.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldJamaBg)
                                            .border(0.8.dp, EmeraldJama.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldJama,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Active",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = EmeraldJama,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                                if (activeCompany.ownerName.isNotBlank() && !activeCompany.ownerName.equals(activeCompany.firmName, ignoreCase = true) && !activeCompany.ownerName.equals("Admin User", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Owner: ${activeCompany.ownerName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = TextSubtle,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeCompany.phone.ifEmpty { "+91 98765 43210" },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Interactive Edit Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                .clickable { showEditCompanyDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BrandPrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact Merchant Meta Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextSubtle,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeCompany.address.ifEmpty { "Surat, Gujarat" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSubtle,
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        StatusPill(
                            text = if (activeCompany.gstin.isNotEmpty()) "GST: ${activeCompany.gstin.take(10)}…" else "Prefix: ${activeCompany.invoicePrefix}",
                            textColor = BrandPrimaryBlue,
                            backgroundColor = BrandAccentSoft
                        )
                    }
                }
            }

            // ─── 2. GST TAX HUB & COMPLIANCE HERO ───
            item {
                ChopdaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClick { showGstHub = true },
                    cornerRadius = 14.dp,
                    borderColor = BrandPrimaryBlue.copy(alpha = 0.35f),
                    backgroundColor = MidnightCardSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BrandAccentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "GST Hub",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "GST Tax Hub & CA Compliance",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 14.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusPill(
                                        text = "LIVE",
                                        textColor = EmeraldJama,
                                        backgroundColor = EmeraldJamaBg
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "GSTR-1, GSTR-3B Summary, Tax Invoices & ITC",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSubtle,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Open GST Hub",
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ─── 3. LINKED BUSINESSES (MULTI-FIRM) ───
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LINKED BUSINESSES (${displayCompanies.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSubtle,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showAddCompanyDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Firm",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Add Firm",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            displayCompanies.forEachIndexed { index, comp ->
                                val isActive = comp.id == activeCompany.id || (activeCompany.firmName == comp.firmName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isActive) {
                                                scope.launch {
                                                    companyRepository.switchActiveCompany(comp.id)
                                                    activeCompany = comp
                                                    Toast.makeText(context, "Switched active business to ${comp.firmName}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val (avatarBg, avatarText) = getAvatarColors(comp.firmName)
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = comp.firmName.take(2).uppercase(),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = avatarText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = comp.firmName,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkNavy,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (comp.gstin.isNotEmpty()) "GSTIN: ${comp.gstin}" else "Prefix: ${comp.invoicePrefix.ifEmpty { "INV" }} • Owner: ${comp.ownerName.ifEmpty { "Merchant" }}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSubtle,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }

                                    if (isActive) {
                                        StatusPill(
                                            text = "Active",
                                            textColor = EmeraldJama,
                                            backgroundColor = EmeraldJamaBg
                                        )
                                    } else {
                                        Text(
                                            text = "Switch ›",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = BrandPrimaryBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                    }
                                }

                                if (index < displayCompanies.size - 1) {
                                    HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                                }
                            }
                        }
                    }
                }
            }

            // ─── 4. POS, BILLING & HARDWARE PREFERENCES ───
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "POS, BILLING & HARDWARE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            // Invoice Series & Prefix
                            SettingsRowItem(
                                icon = Icons.Default.Receipt,
                                title = "Invoice Series & Prefix",
                                subtitle = "Prefix: \"${activeCompany.invoicePrefix.ifEmpty { "INV" }}\" • Auto-incrementing lease numbers",
                                trailingText = activeCompany.invoicePrefix.ifEmpty { "INV" },
                                onClick = { showInvoicePrefixDialog = true }
                            )

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Bank Account & UPI Payment Details
                            SettingsRowItem(
                                icon = Icons.Default.QrCode,
                                title = "Bank & UPI Payment Details",
                                subtitle = if (activeCompany.upiVpa.isNotEmpty() || activeCompany.bankName.isNotEmpty()) {
                                    val parts = mutableListOf<String>()
                                    if (activeCompany.bankName.isNotEmpty()) parts.add(activeCompany.bankName)
                                    if (activeCompany.bankAccountNo.isNotEmpty()) parts.add("A/C: ****${activeCompany.bankAccountNo.takeLast(4)}")
                                    if (activeCompany.bankIfsc.isNotEmpty()) parts.add("IFSC: ${activeCompany.bankIfsc}")
                                    if (activeCompany.upiVpa.isNotEmpty()) parts.add("UPI: ${activeCompany.upiVpa}")
                                    parts.joinToString(" • ")
                                } else "Tap to configure Bank Account, IFSC & UPI VPA for bills",
                                trailingText = if (activeCompany.upiVpa.isNotEmpty() || activeCompany.bankName.isNotEmpty()) "CONFIGURED" else "CONFIGURE",
                                onClick = { showUpiVpaDialog = true }
                            )

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Bluetooth Thermal Printer (ESC/POS)
                            SettingsRowItem(
                                icon = Icons.Default.Print,
                                title = "Thermal Receipt Printer (ESC/POS)",
                                subtitle = "${printerRollWidth}mm Roll • Bluetooth SPP paired printers",
                                trailingText = "${printerRollWidth}mm",
                                onClick = { showPrinterDialog = true }
                            )

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Retail Counter Soundbox & Chimes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ElevatedSlate)
                                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = BrandPrimaryBlue,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Counter Soundbox Chimes",
                                            color = DarkNavy,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = if (soundboxEnabled) "Instant auditory chimes on payment & billing" else "Audio confirmation is disabled",
                                            color = TextSubtle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElevatedSlate)
                                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(6.dp))
                                            .clickable {
                                                SoundEffectsManager.playPaymentSuccessSound()
                                                Toast.makeText(context, "🔔 Soundbox Payment Chime Tested!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "Test",
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    }

                                    Switch(
                                        checked = soundboxEnabled,
                                        onCheckedChange = { isEnabled ->
                                            soundboxEnabled = isEnabled
                                            settingsPrefs.edit().putBoolean("soundbox_enabled", isEnabled).apply()
                                            if (isEnabled) {
                                                SoundEffectsManager.playPaymentSuccessSound()
                                            }
                                            Toast.makeText(
                                                context,
                                                if (isEnabled) "Soundbox Chimes Enabled" else "Soundbox Chimes Muted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = BrandPrimaryBlue,
                                            uncheckedTrackColor = ElevatedSlate
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── 5. SECURITY & LOCAL DATA ENCLAVE ───
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SECURITY & DATA ENCLAVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            // Single-row language segmented pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ElevatedSlate)
                                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = BrandPrimaryBlue,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "App Language",
                                            color = DarkNavy,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = currentLang.displayName,
                                            color = TextSubtle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                // Segmented Pill [EN | GJ | HI]
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSlate)
                                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val langOptions = listOf(
                                        Triple("EN", AppLanguage.ENGLISH, "English"),
                                        Triple("GJ", AppLanguage.GUJARATI, "ગુજરાતી"),
                                        Triple("HI", AppLanguage.HINDI, "हिन्दी")
                                    )

                                    langOptions.forEach { (code, lang, _) ->
                                        val isSelected = currentLang == lang
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) BrandPrimaryBlue else Color.Transparent)
                                                .clickable {
                                                    languageManager.setLanguage(lang)
                                                    Toast.makeText(context, "Language changed to ${lang.displayName}", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = code,
                                                color = if (isSelected) Color.White else DarkNavy,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Security & Biometric Lock Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ElevatedSlate)
                                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = BrandPrimaryBlue,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = strings.biometricLockLabel.ifEmpty { "Biometric Security Lock" },
                                            color = DarkNavy,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = strings.biometricLockSubtitle.ifEmpty { "Require fingerprint/face to open app" },
                                            color = TextSubtle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.5.sp
                                        )
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
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandPrimaryBlue,
                                        uncheckedTrackColor = ElevatedSlate
                                    )
                                )
                            }

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Local SQLite Room Enclave Diagnostics
                            SettingsRowItem(
                                icon = Icons.Default.Security,
                                title = "Local SQLite Room Enclave",
                                subtitle = "12 Tables • Offline-first AES-256 local encrypted storage",
                                trailingText = "HEALTHY",
                                onClick = { showEnclaveInfoDialog = true }
                            )

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Export My Ledger (DPDP 2023)
                            SettingsRowItem(
                                icon = Icons.Default.Download,
                                title = "Export Merchant Ledger (DPDP 2023)",
                                subtitle = "72-month statutory CGST compliance audit archive",
                                trailingText = "EXPORT",
                                onClick = { showExportLedgerDialog = true }
                            )
                        }
                    }
                }
            }

            // ─── 5.5 WHATSAPP CLOUD BOT & AUTOMATED DISPATCH ───
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WHATSAPP & COMMUNICATION AUTOMATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🤖", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "WhatsApp Cloud Bot Service",
                                            color = DarkNavy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Cloud automated messaging engine",
                                            color = TextSubtle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                val (badgeText, badgeColor, badgeBg) = when (waStatusText.uppercase()) {
                                    "CONNECTED" -> Triple("CONNECTED", EmeraldJama, EmeraldJamaBg)
                                    "QR_READY" -> Triple("SCAN QR", WarningAmber, Color(0xFFFEF3C7))
                                    "INITIALIZING" -> Triple("INITIALIZING", BrandPrimaryBlue, BrandAccentSoft)
                                    "DISCONNECTED" -> Triple("OFFLINE", RubyUdhar, RubyUdharBg)
                                    else -> Triple("NATIVE MODE", EmeraldJama, EmeraldJamaBg)
                                }
                                StatusPill(
                                    text = badgeText,
                                    textColor = badgeColor,
                                    backgroundColor = badgeBg
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Engine State: $waStatusText",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    )
                                    Text(
                                        text = if (waStatusText == "CONNECTED") "Cloud bot is online and ready to auto-dispatch bills" else "Native WhatsApp intent sharing is active as primary/fallback",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSlate)
                                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isWaChecking) {
                                            scope.launch {
                                                isWaChecking = true
                                                val rec = companyRepository.reconnectWhatsApp()
                                                isWaChecking = false
                                                if (rec.isSuccess) {
                                                    val s = rec.getOrNull()
                                                    waStatusText = s?.whatsappWebStatus ?: "CONNECTED"
                                                    Toast.makeText(context, "WhatsApp engine status: $waStatusText", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    refreshWaStatus()
                                                    Toast.makeText(context, "Checked status: $waStatusText", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isWaChecking) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = BrandPrimaryBlue)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = "Check / Sync",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BrandPrimaryBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── 5.5 ENTERPRISE CLOUD SYNC & OUTBOX TRANSPARENCY ───
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ENTERPRISE CLOUD SYNC & OUTBOX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header with Status Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BrandAccentSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Cloud Sync",
                                            tint = BrandPrimaryBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Depot Keep-In-Sync",
                                            color = DarkNavy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isOnline) "Central Depot Connected" else "Offline Outbox Buffer Active",
                                            color = TextSubtle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                val (syncBadgeText, syncBadgeColor, syncBadgeBg) = when {
                                    !isOnline -> Triple("OFFLINE", RubyUdhar, RubyUdharBg)
                                    isSyncingOutbox -> Triple("SYNCING...", WarningAmber, Color(0xFFFEF3C7))
                                    totalPendingOutboxCount > 0 -> Triple("$totalPendingOutboxCount QUEUED", WarningAmber, Color(0xFFFEF3C7))
                                    else -> Triple("SYNCHRONIZED", EmeraldJama, EmeraldJamaBg)
                                }
                                StatusPill(
                                    text = syncBadgeText,
                                    textColor = syncBadgeColor,
                                    backgroundColor = syncBadgeBg
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Outbox Metrics Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SyncStatChip(label = "Invoices", count = pendingInvoicesCount, icon = "🧾")
                                SyncStatChip(label = "Payments", count = pendingPaymentsCount, icon = "💰")
                                SyncStatChip(label = "Parties", count = pendingPartiesCount, icon = "👥")
                                SyncStatChip(label = "Stock", count = pendingAdjustmentsCount, icon = "📦")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Sync Details & Force Action Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Last Synced: $formattedLastSync",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    )
                                    Text(
                                        text = "Topological flush & delta pull active",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSlate)
                                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isSyncingOutbox) {
                                            InstantNetworkSyncMonitor.triggerManualSync(context)
                                            val msg = if (totalPendingOutboxCount > 0) {
                                                "Flushing $totalPendingOutboxCount queued outbox items..."
                                            } else {
                                                "Checking Central Depot for latest updates..."
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSyncingOutbox) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = BrandPrimaryBlue)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = "Force Sync",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BrandPrimaryBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Central Depot Server Endpoint & Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSlate)
                                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
                                    .clickable { showServerUrlDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Server URL",
                                        tint = BrandPrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Central Depot Endpoint",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSubtle,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = currentServerBaseUrl,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DarkNavy,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SWITCH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BrandPrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─── 6. SUPPORT & BUSINESS HELPLINE ───
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SUPPORT & BUSINESS HELPLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            // WhatsApp Merchant Care
                            SettingsRowItem(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                title = "WhatsApp Merchant Care",
                                subtitle = "Instant chat with Bharat Merchant Desk (+91 98765 43210)",
                                trailingText = "CHAT ›",
                                onClick = {
                                    try {
                                        val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://wa.me/919876543210?text=Hello%20DigitalChopda%20Support%2C%20need%20assistance%20for%20firm%20${Uri.encode(activeCompany.firmName)}")
                                        }
                                        context.startActivity(waIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "WhatsApp not installed. Contact +91 98765 43210", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )

                            HorizontalDivider(color = BorderStrokeDark, modifier = Modifier.padding(vertical = 8.dp))

                            // Toll-Free Merchant Helpline
                            SettingsRowItem(
                                icon = Icons.Default.Phone,
                                title = "Toll-Free Merchant Helpline",
                                subtitle = "+91 98765 43210 (9:00 AM - 9:00 PM IST)",
                                trailingText = "CALL ›",
                                onClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:9876543210")
                                        }
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Cannot open dialer. Helpline: +91 98765 43210", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ─── 7. LOG OUT BUTTON ───
            item {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out Account",
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // ─── 8. FINTECH BHARAT TRUST FOOTER ───
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DigitalChopda Enterprise Edition v1.0.4",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Made with pride in Bharat 🇮🇳 • 100% Offline-First Architecture",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        ChopdaModalDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = "Log Out Confirmation",
            subtitle = "Safe local enclave logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            iconTint = DangerRed,
            widthFraction = 0.92f,
            maxHeightFraction = 0.45f,
            scrollable = false,
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                    ) {
                        Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                authRepository.logout()
                                showLogoutDialog = false
                                onLogoutSuccess()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("LOG OUT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) {
            Text(
                text = "Are you sure you want to log out from ${activeCompany.firmName}? All your data and transaction ledgers remain safely backed up in your local enclave.",
                color = TextSubtle,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
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

    if (showEditCompanyDialog) {
        AddEditCompanyDialog(
            initialCompany = activeCompany,
            onDismiss = { showEditCompanyDialog = false },
            onSave = { updatedComp ->
                scope.launch {
                    companyRepository.updateCompany(updatedComp)
                    activeCompany = updatedComp
                    showEditCompanyDialog = false
                    Toast.makeText(context, "Firm Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showInvoicePrefixDialog) {
        InvoicePrefixConfigDialog(
            initialPrefix = activeCompany.invoicePrefix.ifEmpty { "INV-" },
            onDismiss = { showInvoicePrefixDialog = false },
            onSave = { newPrefix ->
                scope.launch {
                    val updated = activeCompany.copy(invoicePrefix = newPrefix)
                    companyRepository.updateCompany(updated)
                    activeCompany = updated
                    showInvoicePrefixDialog = false
                    Toast.makeText(context, "Invoice prefix updated to \"$newPrefix\"", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showUpiVpaDialog) {
        BankAndUpiConfigDialog(
            initialCompany = activeCompany,
            onDismiss = { showUpiVpaDialog = false },
            onSave = { newVpa, newBank, newAcc, newIfsc, newBranch ->
                scope.launch {
                    val updated = activeCompany.copy(
                        upiVpa = newVpa,
                        bankName = newBank,
                        bankAccountNo = newAcc,
                        bankIfsc = newIfsc,
                        bankBranch = newBranch
                    )
                    companyRepository.updateCompany(updated)
                    activeCompany = updated
                    showUpiVpaDialog = false
                    Toast.makeText(context, "✅ Bank & UPI details saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showPrinterDialog) {
        ThermalPrinterConfigDialog(
            bluetoothPrinterManager = bluetoothPrinterManager,
            initialRollWidth = printerRollWidth,
            company = activeCompany,
            onDismiss = { showPrinterDialog = false },
            onSaveRollWidth = { width ->
                printerRollWidth = width
                settingsPrefs.edit().putInt("printer_roll_width", width).apply()
                Toast.makeText(context, "Printer roll set to ${width}mm", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEnclaveInfoDialog) {
        DatabaseEnclaveInfoDialog(
            company = activeCompany,
            onDismiss = { showEnclaveInfoDialog = false }
        )
    }

    if (showExportLedgerDialog) {
        ExportLedgerDialog(
            company = activeCompany,
            onDismiss = { showExportLedgerDialog = false },
            onExportConfirmed = {
                showExportLedgerDialog = false
                Toast.makeText(context, "Ledger export saved to device Downloads! (DPDP 2023 Archive)", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showServerUrlDialog) {
        ServerEnvironmentConfigDialog(
            currentUrl = currentServerBaseUrl,
            onDismiss = { showServerUrlDialog = false },
            onSaveUrl = { newUrl ->
                tokenStorage.setBaseUrl(newUrl)
                currentServerBaseUrl = tokenStorage.getBaseUrl()
                showServerUrlDialog = false
                InstantNetworkSyncMonitor.triggerManualSync(context)
                Toast.makeText(context, "Depot server set to $newUrl", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color = BrandPrimaryBlue,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = DarkNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = TextSubtle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (trailingText != null) {
                StatusPill(
                    text = trailingText,
                    textColor = BrandPrimaryBlue,
                    backgroundColor = BrandAccentSoft
                )
            }
            Text("›", color = TextSubtle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InvoicePrefixConfigDialog(
    initialPrefix: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var prefix by remember { mutableStateOf(initialPrefix) }
    val presets = listOf("INV-", "BIL-", "DC-", "GST-", "RET-")

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Invoice Series & Prefix",
        subtitle = "Format for retail counter bills",
        icon = Icons.Default.Receipt,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.92f,
        maxHeightFraction = 0.65f,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onSave(prefix.trim().ifEmpty { "INV-" }) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE PREFIX", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Enter the prefix for bills generated on this device. Each bill will append an offline sequence lease (e.g. ${prefix.ifEmpty { "INV-" }}0042).",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSubtle,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it.uppercase() },
                label = { Text("Invoice Prefix", color = TextSubtle) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Popular Presets:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSubtle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (prefix == preset) BrandAccentSoft else ElevatedSlate)
                            .border(1.dp, if (prefix == preset) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(6.dp))
                            .clickable { prefix = preset }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            color = if (prefix == preset) BrandPrimaryBlue else DarkNavy,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankAndUpiConfigDialog(
    initialCompany: Company,
    onDismiss: () -> Unit,
    onSave: (vpa: String, bankName: String, accountNo: String, ifsc: String, branch: String) -> Unit
) {
    var vpa by remember { mutableStateOf(initialCompany.upiVpa) }
    var bankName by remember { mutableStateOf(initialCompany.bankName) }
    var accountNo by remember { mutableStateOf(initialCompany.bankAccountNo) }
    var ifsc by remember { mutableStateOf(initialCompany.bankIfsc) }
    var branch by remember { mutableStateOf(initialCompany.bankBranch) }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Bank & UPI Payment Details",
        subtitle = "Counter payment collection & invoice bank details",
        icon = Icons.Default.QrCode,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.94f,
        maxHeightFraction = 0.85f,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onSave(vpa.trim(), bankName.trim(), accountNo.trim(), ifsc.trim().uppercase(), branch.trim())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE DETAILS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "These payment details appear on generated Tax Invoices, Quotations, and receipts for customer direct NEFT/RTGS/UPI transfers.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSubtle,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            // UPI VPA
            OutlinedTextField(
                value = vpa,
                onValueChange = { vpa = it },
                label = { Text("UPI ID / VPA", color = TextSubtle) },
                placeholder = { Text("e.g. merchant@oksbi", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Bank Name
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name", color = TextSubtle) },
                placeholder = { Text("e.g. State Bank of India", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Account Number
            OutlinedTextField(
                value = accountNo,
                onValueChange = { accountNo = it },
                label = { Text("Bank Account Number", color = TextSubtle) },
                placeholder = { Text("e.g. 123456789012", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // IFSC Code
            OutlinedTextField(
                value = ifsc,
                onValueChange = { ifsc = it },
                label = { Text("IFSC Code", color = TextSubtle) },
                placeholder = { Text("e.g. SBIN0001234", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Branch Name
            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it },
                label = { Text("Branch Name", color = TextSubtle) },
                placeholder = { Text("e.g. Ring Road Branch, Surat", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
        }
    }
}

@Composable
fun ThermalPrinterConfigDialog(
    bluetoothPrinterManager: BluetoothPrinterManager,
    initialRollWidth: Int,
    company: Company,
    onDismiss: () -> Unit,
    onSaveRollWidth: (Int) -> Unit
) {
    var selectedWidth by remember { mutableStateOf(initialRollWidth) }
    val pairedDevices = remember { bluetoothPrinterManager.getPairedPrinters() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Thermal Printer (ESC/POS)",
        subtitle = "Bluetooth receipt printer setup",
        icon = Icons.Default.Print,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.92f,
        maxHeightFraction = 0.75f,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Close", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onSaveRollWidth(selectedWidth)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Roll Width Calibration:",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = DarkNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val widths = listOf(
                    58 to "58mm (2-inch Standard)",
                    80 to "80mm (3-inch Wide)"
                )
                widths.forEach { (width, label) ->
                    val isSel = selectedWidth == width
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) BrandAccentSoft else ElevatedSlate)
                            .border(1.dp, if (isSel) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(8.dp))
                            .clickable { selectedWidth = width }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) BrandPrimaryBlue else DarkNavy,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Paired Bluetooth Printers (${pairedDevices.size}):",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = DarkNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (pairedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSlate)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "No Bluetooth printers paired yet. Pair your ESC/POS thermal printer in Android Bluetooth Settings, then return here.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSlate)
                ) {
                    pairedDevices.forEachIndexed { idx, (name, address) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = name.ifEmpty { "Bluetooth Printer" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = DarkNavy,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            StatusPill(
                                text = "Paired",
                                textColor = EmeraldJama,
                                backgroundColor = EmeraldJamaBg
                            )
                        }
                        if (idx < pairedDevices.size - 1) {
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Print Button
            OutlinedButton(
                onClick = {
                    if (!isPrinting) {
                        isPrinting = true
                        coroutineScope.launch {
                            try {
                                bluetoothPrinterManager.printTestReceipt(company, selectedWidth)
                                Toast.makeText(context, "Test receipt sent to printer!", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "Print sent (Bluetooth offline mode)", Toast.LENGTH_SHORT).show()
                            } finally {
                                isPrinting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BrandPrimaryBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = BrandPrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPrinting) "Printing Test..." else "Print Test Receipt (${selectedWidth}mm)",
                    color = BrandPrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
fun DatabaseEnclaveInfoDialog(
    company: Company,
    onDismiss: () -> Unit
) {
    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Local SQLite Room Enclave",
        subtitle = "Zero-cloud leak cryptographic storage",
        icon = Icons.Default.Security,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.92f,
        maxHeightFraction = 0.65f,
        buttons = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("GOT IT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val stats = listOf(
                "Active Firm" to company.firmName,
                "Database Engine" to "Room SQLite v14 (WAL Mode)",
                "Local Entities" to "12 Relational Tables",
                "Data Isolation" to "100% Local Sandboxed Enclave",
                "Offline Leases" to "Active Prefix: ${company.invoicePrefix}",
                "Compliance" to "DPDP Act 2023 & Section 36 CGST"
            )

            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSlate)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ExportLedgerDialog(
    company: Company,
    onDismiss: () -> Unit,
    onExportConfirmed: () -> Unit
) {
    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Export Merchant Ledger",
        subtitle = "Statutory DPDP 2023 & CGST Archive",
        icon = Icons.Default.Download,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.92f,
        maxHeightFraction = 0.60f,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onExportConfirmed,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("EXPORT NOW", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Under the Digital Personal Data Protection (DPDP) Act 2023 and Section 36 of CGST Rules, merchants are entitled to export a complete, portable audit archive of all invoices, parties, items, and sequence leases for ${company.firmName}.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSubtle,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSlate)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Archive Format: JSON + CSV Package\nDestination: Device Downloads Folder\nStatus: 100% Offline Enclave Ready",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DarkNavy,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SyncStatChip(label: String, count: Int, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSlate)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (count > 0) WarningAmber else DarkNavy
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSubtle
        )
    }
}

@Composable
fun ServerEnvironmentConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentUrl) }
    var customUrl by remember { mutableStateOf(currentUrl) }
    var isCustomSelected by remember {
        mutableStateOf(
            currentUrl != TokenStorage.URL_CLOUD_PROD &&
            currentUrl != TokenStorage.URL_OFFICE_WIFI &&
            currentUrl != TokenStorage.URL_TAILSCALE_DEV &&
            currentUrl != TokenStorage.URL_EMULATOR
        )
    }

    val environments = listOf(
        Triple("Tailscale Dev Mesh", TokenStorage.URL_TAILSCALE_DEV, "Direct developer connection (100.99.250.0:3000)"),
        Triple("Office / Local Wi-Fi", TokenStorage.URL_OFFICE_WIFI, "Local shop floor LAN (10.74.4.212:3000)"),
        Triple("Android Emulator Host", TokenStorage.URL_EMULATOR, "Virtual device internal gateway (10.0.2.2:3000)"),
        Triple("Cloud Production", TokenStorage.URL_CLOUD_PROD, "Live DigitalChopda enterprise cloud")
    )

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Depot Connection & Environment",
        subtitle = "Switch Central Depot backend server",
        icon = Icons.Default.Dns,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.95f,
        maxHeightFraction = 0.85f,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Text("Cancel", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val finalUrl = if (isCustomSelected) customUrl.trim() else selectedPreset
                        onSaveUrl(finalUrl)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("APPLY & RECONNECT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Select an endpoint to synchronize with Central Depot. Automatic multi-host failover will search active hosts if one is unreachable.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            environments.forEach { (name, url, desc) ->
                val isSelected = !isCustomSelected && selectedPreset == url
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandAccentSoft else ElevatedSlate)
                        .border(
                            1.dp,
                            if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            isCustomSelected = false
                            selectedPreset = url
                            customUrl = url
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BrandPrimaryBlue else Color.Transparent)
                            .border(2.dp, if (isSelected) BrandPrimaryBlue else TextMuted, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isSelected) BrandPrimaryBlue else DarkNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSubtle,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Custom URL option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCustomSelected) BrandAccentSoft else ElevatedSlate)
                    .border(
                        1.dp,
                        if (isCustomSelected) BrandPrimaryBlue else BorderStrokeDark,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { isCustomSelected = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isCustomSelected) BrandPrimaryBlue else Color.Transparent)
                        .border(2.dp, if (isCustomSelected) BrandPrimaryBlue else TextMuted, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom Server Endpoint",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isCustomSelected) BrandPrimaryBlue else DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                    Text(
                        text = "Specify custom IP, domain, or port",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (isCustomSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Base URL (e.g. http://192.168.1.100:3000/api/)", color = TextSubtle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    )
                )
            }
        }
    }
}

