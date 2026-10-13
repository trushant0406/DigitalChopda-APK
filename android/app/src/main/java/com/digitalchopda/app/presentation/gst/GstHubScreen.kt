package com.digitalchopda.app.presentation.gst

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElectricBrandCyan
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.GoldFoilGradient
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.network.GstReportData
import com.digitalchopda.app.core.network.MockDigitalChopdaApiService
import com.digitalchopda.app.core.localization.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstHubScreen(
    mockApi: MockDigitalChopdaApiService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    var selectedPeriod by remember { mutableStateOf("October 2026") }
    var reportData by remember {
        mutableStateOf(
            GstReportData(
                period = "October 2026",
                totalInwardTaxLiability = 38200.0,
                totalOutwardTaxLiability = 38200.0,
                inputTaxCredit = 25750.0,
                netGstPayable = 12450.0,
                gstr1Status = "READY TO FILE",
                gstr3bStatus = "PENDING (Due in 9 Days)",
                gstr2bStatus = "AVAILABLE (100% Matched)"
            )
        )
    }

    var showRateCalcDialog by remember { mutableStateOf(false) }
    var showHsnDirectoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPeriod) {
        val res = mockApi.fetchGstReport(selectedPeriod)
        if (res.isSuccess) {
            reportData = res.getOrThrow()
        }
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "|| ${strings.gstTitle} ||",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = ImperialGoldLight,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "GSTIN: 24AAACS1429B1Z8 • ${strings.itcCreditLabel}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ImperialGoldLight
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.filingPeriodLabel}: $selectedPeriod",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ImperialGoldLight,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Text(
                        text = "Q3 FY 26-27",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }

            // Tax Summary Card
            item {
                ChopdaCard {
                    Text(
                        text = strings.estimatedNetGstPayable,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹ %.2f".format(reportData.netGstPayable),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RubyUdhar
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = BorderStrokeDark)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.outwardSalesTax, style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("₹ %.2f".format(reportData.totalOutwardTaxLiability), color = RubyUdhar, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.inputTaxCredit, style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("₹ %.2f".format(reportData.inputTaxCredit), color = EmeraldJama, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldFoilGradient)
                            .bouncyClick {
                                Toast.makeText(context, "Exported GST Report for $selectedPeriod", Toast.LENGTH_LONG).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF1E1005))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.exportCaExcel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1E1005)
                                )
                            )
                        }
                    }
                }
            }

            // Statutory Return Cards
            item {
                Text(
                    text = "STATUTORY RETURNS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            // GSTR-1 Card
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GSTR-1 (Outward Sales)", style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
                            Text("18 Invoices Ready • Due 11th", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        }
                        StatusPill(text = "READY TO FILE", textColor = EmeraldJama, backgroundColor = EmeraldJama.copy(alpha = 0.15f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSlate)
                            .border(1.dp, ImperialGold, RoundedCornerShape(10.dp))
                            .bouncyClick {
                                Toast.makeText(context, "Preparing GSTR-1 Json Package...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Prepare & File GSTR-1", color = ImperialGoldLight, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // GSTR-3B Card
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GSTR-3B (Monthly Summary)", style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
                            Text("Due in 9 Days (20th Oct)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        }
                        StatusPill(text = "PENDING", textColor = WarningAmber, backgroundColor = WarningAmber.copy(alpha = 0.15f))
                    }
                }
            }

            // GSTR-2B Card
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GSTR-2B (ITC Auto-drafted)", style = MaterialTheme.typography.titleMedium.copy(color = TextWhite, fontWeight = FontWeight.Bold))
                            Text("Purchase Reconciliation: 100% Matched", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        }
                        StatusPill(text = "AVAILABLE", textColor = EmeraldJama, backgroundColor = EmeraldJama.copy(alpha = 0.15f))
                    }
                }
            }

            // Quick Tools
            item {
                Text(
                    text = "QUICK TAX TOOLS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .bouncyClick { showRateCalcDialog = true }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = ImperialGoldLight)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rate Calculator", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .bouncyClick { showHsnDirectoryDialog = true }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FindInPage, contentDescription = null, tint = ImperialGoldLight)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HSN/SAC Finder", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showRateCalcDialog) {
        GstCalculatorDialog(onDismiss = { showRateCalcDialog = false })
    }

    if (showHsnDirectoryDialog) {
        HsnFinderDialog(onDismiss = { showHsnDirectoryDialog = false })
    }
}

@Composable
fun GstCalculatorDialog(onDismiss: () -> Unit) {
    var amountStr by remember { mutableStateOf("10000") }
    var selectedRate by remember { mutableStateOf(18.0) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val gstAmount = amount * (selectedRate / 100.0)
    val totalAmount = amount + gstAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = { Text("GST Rate Calculator", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Base Amount (₹)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5.0, 12.0, 18.0, 28.0).forEach { rate ->
                        val isSelected = selectedRate == rate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CoralPrimary else ElevatedSlate)
                                .clickable { selectedRate = rate }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${rate.toInt()}%", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GST Tax Amount:", color = TextMuted)
                    Text("₹ %.2f".format(gstAmount), color = CoralPrimary, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total with GST:", color = TextMuted)
                    Text("₹ %.2f".format(totalAmount), color = ElectricBrandCyan, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            CoralGradientButton(text = "DONE", onClick = onDismiss)
        }
    )
}

@Composable
fun HsnFinderDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = { Text("HSN / SAC Code Directory", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• 5208: Woven fabrics of cotton (GST: 5%)", color = TextWhite)
                Text("• 5007: Woven fabrics of silk (GST: 5%)", color = TextWhite)
                Text("• 6204: Women kurtis, suits & ensembles (GST: 12%)", color = TextWhite)
                Text("• 8517: Mobile smartphones & telecom devices (GST: 18%)", color = TextWhite)
                Text("• 9983: Information technology consulting (GST: 18%)", color = TextWhite)
            }
        },
        confirmButton = {
            CoralGradientButton(text = "CLOSE", onClick = onDismiss)
        }
    )
}
