package com.digitalchopda.app.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.network.ApiSubscriptionPlanDto
import com.digitalchopda.app.core.payment.UpiPaymentUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

/**
 * Modern Subscription & Plan status card displayed below Company Name in Settings.
 */
@Composable
fun SubscriptionStatusCard(
    planTier: String,
    status: String,
    daysLeft: Int,
    isExpired: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierUpper = planTier.uppercase().ifBlank { "PRO" }
    val displayTierName = when {
        tierUpper.contains("ENTERPRISE") -> "Multi-Firm Enterprise"
        tierUpper.contains("PRO") -> "Growth Pro"
        else -> "Starter Pro"
    }

    val (tierBg, tierBorder, tierText) = when {
        tierUpper.contains("ENTERPRISE") -> Triple(Color(0xFFEDE9FE), Color(0xFFDDD6FE), Color(0xFF6D28D9))
        tierUpper.contains("PRO") -> Triple(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFB45309))
        else -> Triple(Color(0xFFEFF6FF), Color(0xFFBFDBFE), BrandPrimaryBlue)
    }

    val statusPillBg = when {
        isExpired -> Color(0xFFFEE2E2)
        status.equals("ACTIVE", ignoreCase = true) || status.equals("LIFETIME", ignoreCase = true) -> EmeraldJamaBg
        daysLeft <= 5 -> Color(0xFFFEF3C7)
        else -> BrandAccentSoft
    }

    val statusPillColor = when {
        isExpired -> DangerRed
        status.equals("ACTIVE", ignoreCase = true) || status.equals("LIFETIME", ignoreCase = true) -> EmeraldJama
        daysLeft <= 5 -> Color(0xFFB45309)
        else -> BrandPrimaryBlue
    }

    val statusLabel = when {
        isExpired -> "🚨 Trial Expired"
        status.equals("LIFETIME", ignoreCase = true) -> "💎 Lifetime License"
        status.equals("ACTIVE", ignoreCase = true) -> "💎 Paid Subscription"
        else -> "✨ Free Trial"
    }

    ChopdaCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColor = if (isExpired) DangerRed.copy(alpha = 0.4f) else BorderStrokeDark,
        backgroundColor = MidnightCardSurface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Gem Icon, Plan Name, Status Pill & Tier Badge
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
                            .clip(CircleShape)
                            .background(tierBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = tierText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayTierName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    fontSize = 14.5.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isExpired) {
                                "Access locked. Upgrade to restore billing."
                            } else if (status.equals("ACTIVE", ignoreCase = true) || status.equals("LIFETIME", ignoreCase = true)) {
                                "Full access to POS, Logistics & Multi-Firm"
                            } else {
                                "$daysLeft days left in free activation trial"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isExpired) DangerRed else TextSubtle,
                                fontSize = 11.5.sp,
                                fontWeight = if (isExpired) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusPillBg)
                        .border(0.8.dp, statusPillColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusPillColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar (if trial or standard validity)
            if (!status.equals("LIFETIME", ignoreCase = true)) {
                val clampedDays = daysLeft.coerceIn(0, 30)
                val progressFraction = (clampedDays / 30f).coerceIn(0f, 1f)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpired) "0 Days Remaining" else "$daysLeft of 30 Days Remaining",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isExpired) DangerRed else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (isExpired) "Expired" else "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isExpired) DangerRed else BrandPrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    when {
                                        isExpired -> DangerRed
                                        status.equals("ACTIVE", ignoreCase = true) -> EmeraldJama
                                        daysLeft <= 5 -> Color(0xFFF59E0B)
                                        else -> BrandPrimaryBlue
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick Perks Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("POS Thermal", "GST Invoices", "WhatsApp Khata").forEach { perk ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElevatedSlate)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "✓ $perk",
                            fontSize = 10.sp,
                            color = TextSubtle,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button: Upgrade Plan / Renew Plan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (isExpired) {
                                listOf(Color(0xFFE11D48), Color(0xFFBE123C))
                            } else {
                                listOf(BrandPrimaryBlue, Color(0xFF405BC6))
                            }
                        )
                    )
                    .clickable { onUpgradeClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpired) Icons.Default.Star else Icons.Default.Diamond,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExpired) "RENEW PLAN NOW ›" else "UPGRADE / VIEW PLANS ›",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Interactive 3-tier Plan Showroom dialog matching Web SubscriptionPage.jsx.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansDialog(
    isOpen: Boolean,
    currentPlanTier: String,
    firmName: String,
    onDismissRequest: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var billingCycle by remember { mutableStateOf("annual") } // "monthly" | "annual"
    var selectedPlanForUpgrade by remember { mutableStateOf<ApiSubscriptionPlanDto?>(null) }

    val defaultPlans = remember {
        listOf(
            ApiSubscriptionPlanDto(
                key = "STARTER",
                name = "Starter Pro",
                tagline = "Ideal for single retail shops, kiranas & single counters",
                monthlyPrice = 199,
                annualPrice = 1999,
                popular = false,
                features = listOf(
                    "1 Registered Company Workspace",
                    "Unlimited Customers, Parties & Khata Ledger",
                    "Unlimited GST Invoices & Estimates (A4 & Thermal)",
                    "Basic Product & Service Inventory Catalog",
                    "WhatsApp & Email Bill & Statement Delivery",
                    "Cloud Backup & Outbox Sync"
                )
            ),
            ApiSubscriptionPlanDto(
                key = "PRO",
                name = "Growth Pro",
                tagline = "Built for expanding traders, wholesalers & multi-firm merchants",
                monthlyPrice = 499,
                annualPrice = 4999,
                popular = true,
                features = listOf(
                    "Up to 5 Registered Company Workspaces",
                    "Unlimited Customers, Products & Contacts",
                    "Express POS Counter & Thermal Bills (58mm/80mm)",
                    "Automated Low Stock Alerts & Reorder Radar",
                    "Near Expiry / FEFO Batch Tracking",
                    "Delivery Challans & Dispatch Logistics",
                    "Multi-Godown Stock Transfers & Batches",
                    "CA-Ready GSTR-1, GSTR-3B & P&L Excel Exports",
                    "Automated WhatsApp Bill & Reminder Delivery"
                )
            ),
            ApiSubscriptionPlanDto(
                key = "ENTERPRISE",
                name = "Multi-Firm Enterprise",
                tagline = "For distribution agencies, corporate groups & unlimited firms",
                monthlyPrice = 999,
                annualPrice = 9999,
                popular = false,
                features = listOf(
                    "Unlimited Company Creation (Unlimited Firms)",
                    "Unlimited Customers & Multi-Firm Khata",
                    "Unlimited POS Cashier Terminals & Shifts",
                    "Custom Print Layouts & Watermarked Letterheads",
                    "Multi-Warehouse Inventory Ledger & Serialization",
                    "Dedicated Priority WhatsApp Relationship Manager",
                    "Direct API Access & Webhooks for Custom Integrations"
                )
            )
        )
    }

    ChopdaModalDialog(
        onDismissRequest = onDismissRequest,
        title = "DigitalChopda Vyapar Plans",
        subtitle = "Simple, transparent pricing for Bharat's vyaparis • 100% GST claimable",
        icon = Icons.Default.Diamond,
        iconTint = BrandPrimaryBlue,
        widthFraction = 1f,
        maxHeightFraction = 0.92f,
        isBottomSheet = true,
        scrollable = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Billing Cycle Switcher: Monthly vs Annual (Save 20%)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monthly Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (billingCycle == "monthly") Color.White else Color.Transparent)
                        .clickable { billingCycle = "monthly" }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Monthly",
                        fontSize = 12.sp,
                        fontWeight = if (billingCycle == "monthly") FontWeight.Bold else FontWeight.Medium,
                        color = if (billingCycle == "monthly") DarkNavy else TextMuted
                    )
                }

                // Annual Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (billingCycle == "annual") Color.White else Color.Transparent)
                        .clickable { billingCycle = "annual" }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Annual Billing",
                            fontSize = 12.sp,
                            fontWeight = if (billingCycle == "annual") FontWeight.Bold else FontWeight.Medium,
                            color = if (billingCycle == "annual") DarkNavy else TextMuted
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SAVE 20%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Tier Plan Cards
            defaultPlans.forEach { plan ->
                val isAnnual = billingCycle == "annual"
                val price = if (isAnnual) plan.annualPrice else plan.monthlyPrice
                val isCurrentPlan = currentPlanTier.contains(plan.key, ignoreCase = true)

                ChopdaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    cornerRadius = 14.dp,
                    borderColor = if (plan.popular) Color(0xFFF59E0B) else BorderStrokeDark,
                    backgroundColor = if (plan.popular) Color(0xFFFFFBEB) else MidnightCardSurface,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = plan.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy,
                                        fontSize = 15.sp
                                    )
                                )
                                plan.tagline?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSubtle,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            if (plan.popular) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "MOST POPULAR",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Price Row
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "₹",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%,d", price),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkNavy
                            )
                            Text(
                                text = if (isAnnual) " /year" else " /month",
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (isAnnual) "Billed annually" else "+18% GST",
                                fontSize = 10.5.sp,
                                color = TextSubtle,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderStrokeDark, thickness = 0.6.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Feature Checklist
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            plan.features.forEach { feat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldJama,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = feat,
                                        fontSize = 11.5.sp,
                                        color = DarkNavy
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Choose / Upgrade CTA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (plan.popular) {
                                        Brush.horizontalGradient(listOf(BrandPrimaryBlue, Color(0xFF405BC6)))
                                    } else {
                                        Brush.horizontalGradient(listOf(ElevatedSlate, ElevatedSlate))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (plan.popular) Color.Transparent else BorderStrokeDark,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedPlanForUpgrade = plan
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCurrentPlan) "Renew ${plan.name} ›" else "Choose ${plan.name} ›",
                                color = if (plan.popular) Color.White else DarkNavy,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Upgrade Bottom Sheet with UPI QR and WhatsApp
    selectedPlanForUpgrade?.let { plan ->
        SubscriptionUpgradeSheet(
            plan = plan,
            billingCycle = billingCycle,
            firmName = firmName,
            onDismiss = { selectedPlanForUpgrade = null }
        )
    }
}

/**
 * Dynamic UPI QR & 1-Tap WhatsApp Activation Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionUpgradeSheet(
    plan: ApiSubscriptionPlanDto,
    billingCycle: String,
    firmName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAnnual = billingCycle == "annual"
    val basePrice = if (isAnnual) plan.annualPrice else plan.monthlyPrice
    val gstAmount = (basePrice * 0.18).toInt()
    val totalAmount = basePrice + gstAmount

    val upiVpa = "chopda@icici"
    val upiUri = remember(totalAmount, plan.key) {
        "upi://pay?pa=$upiVpa&pn=DigitalChopda&am=$totalAmount.00&cu=INR&tn=Plan-${plan.key}-$billingCycle"
    }

    val qrBitmap = remember(upiUri) {
        generateQrBitmap(upiUri, size = 480)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightCardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activate ${plan.name}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "${if (isAnnual) "Annual Subscription" else "Monthly Subscription"} • $firmName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 12.sp
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pricing summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Plan Base Price", fontSize = 12.sp, color = TextMuted)
                        Text(text = "₹${String.format(Locale.US, "%,d", basePrice)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "GST (18%)", fontSize = 12.sp, color = TextMuted)
                        Text(text = "₹${String.format(Locale.US, "%,d", gstAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextMuted)
                    }
                    HorizontalDivider(color = BorderStrokeDark, thickness = 0.6.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Payable Amount", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                        Text(text = "₹${String.format(Locale.US, "%,d", totalAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandPrimaryBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Box
            Text(
                text = "SCAN & PAY VIA ANY UPI APP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSubtle,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.5.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "UPI QR Code",
                        modifier = Modifier.size(180.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = BrandPrimaryBlue,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Copy UPI ID button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(0.8.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                    .clickable {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("UPI ID", upiVpa))
                        Toast.makeText(context, "UPI ID copied: $upiVpa", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "UPI: $upiVpa", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandPrimaryBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandPrimaryBlue, modifier = Modifier.size(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1-Tap WhatsApp Activation Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF25D366))
                    .clickable {
                        val message = """
                        |🙏 *Namaste DigitalChopda Support!*
                        |
                        |I want to activate a subscription plan:
                        |🏢 *Business:* $firmName
                        |💎 *Plan:* ${plan.name} (${billingCycle.uppercase()})
                        |💰 *Amount:* ₹${String.format(Locale.US, "%,d", totalAmount)} (incl. GST)
                        |
                        |I have completed the payment via UPI to `$upiVpa`. Please confirm and activate my plan.
                        |
                        |Thank you!
                        """.trimMargin()

                        UpiPaymentUtils.launchWhatsApp(
                            context = context,
                            phoneNumber = "+919427812345",
                            message = message
                        )
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💬 ACTIVATE VIA WHATSAPP SUPPORT ›",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Instant activation within 5 minutes of confirmation.",
                fontSize = 11.sp,
                color = TextSubtle,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
