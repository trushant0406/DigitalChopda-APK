package com.digitalchopda.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.localization.LocalAppStrings

/**
 * Standard Midnight Card Surface with 16dp rounded corners & subtle 1dp border stroke
 */
@Composable
fun ChopdaCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = BorderStrokeDark,
    backgroundColor: Color = MidnightCardSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * High-impact Glowing Card for Today's Overview & Key Metrics
 */
@Composable
fun GlowingSummaryCard(
    title: String,
    primaryValue: String,
    badgeText: String? = null,
    badgeColor: Color = SuccessGreen,
    badgeBgColor: Color = SuccessGreenBg,
    subMetrics: List<Pair<String, String>> = emptyList(),
    modifier: Modifier = Modifier,
    glowColor: Color = CoralPrimary
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x10000000),
                ambientColor = Color(0x08000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )
                )
                if (badgeText != null) {
                    StatusPill(
                        text = badgeText,
                        textColor = badgeColor,
                        backgroundColor = badgeBgColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite
                )
            )

            if (subMetrics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    subMetrics.forEach { (label, value) ->
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
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
 * Full-width Vibrant Coral / Tangerine Gradient CTA Button
 */
@Composable
fun CoralGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = ElevatedSlate.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = CoralPrimary.copy(alpha = 0.4f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) CoralTangerineGradient
                    else Brush.linearGradient(listOf(ElevatedSlate, ElevatedSlate))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Imperial Gold Foil Gradient CTA Button with bouncy tactile physics
 */
@Composable
fun GoldFoilButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = ImperialGold.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) GoldFoilGradient
                else Brush.linearGradient(listOf(ElevatedSlate, ElevatedSlate))
            )
            .border(1.dp, if (enabled) ImperialGoldLight.copy(alpha = 0.7f) else BorderStrokeDark, RoundedCornerShape(12.dp))
            .then(if (enabled && !isLoading) Modifier.bouncyClick(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = textColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        letterSpacing = 0.8.sp
                    )
                )
            }
        }
    }
}

/**
 * Status Pill (IN STOCK, LOW STOCK, PAID, UNPAID, HOT LEAD, etc.)
 */
@Composable
fun StatusPill(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(0.8.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Stepper (- 2 +) for inline quantity changes
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 9999
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElevatedSlate)
            .border(1.dp, BorderStrokeLight, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (quantity > min) onQuantityChange(quantity - 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = if (quantity > min) TextWhite else TextSubtle,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextWhite
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            onClick = { if (quantity < max) onQuantityChange(quantity + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                tint = if (quantity < max) TextWhite else TextSubtle,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Pulsing Alert Strip
 */
@Composable
fun PulsingAlertStrip(
    message: String,
    modifier: Modifier = Modifier,
    accentColor: Color = WarningAmber
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = alpha), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Company Switcher Header Component
 */
@Composable
fun CompanySwitcherHeader(
    firmName: String,
    ownerName: String,
    unreadNotifications: Int = 3,
    languageName: String? = null,
    onLanguageClick: (() -> Unit)? = null,
    onCompanyClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCompanyClick)
                .padding(4.dp)
        ) {
            // User Avatar matching Web .brand-mark
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ownerName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = firmName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Switch Company",
                        tint = ElectricBrandCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = ownerName,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Switcher Pill
            if (onLanguageClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, ImperialGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onLanguageClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 12.sp)
                        if (languageName != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = languageName,
                                color = ImperialGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Notification Bell with badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ElevatedSlate)
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center
            ) {
                Text("🔔", fontSize = 16.sp)
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(DangerRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadNotifications.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smooth Bezier Sales Trend Graph with cyan-to-orange glowing gradient fill
 */
@Composable
fun BezierSalesChart(
    points: List<Float> = listOf(12f, 18f, 15f, 24f, 22f, 29f, 34f),
    labels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    modifier: Modifier = Modifier,
    height: Dp = 150.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WEEKLY SALES TREND",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "+24.8% WOW",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size < 2) return@Canvas

                val maxPoint = points.maxOrNull() ?: 1f
                val minPoint = (points.minOrNull() ?: 0f).coerceAtLeast(0f)
                val range = (maxPoint - minPoint).coerceAtLeast(1f)

                val width = size.width
                val canvasHeight = size.height
                val spacing = width / (points.size - 1)

                val coordinates = points.mapIndexed { index, value ->
                    val x = index * spacing
                    val y = canvasHeight - ((value - minPoint) / range * (canvasHeight * 0.75f) + canvasHeight * 0.1f)
                    Offset(x, y)
                }

                // Build smooth Bezier path
                val strokePath = Path().apply {
                    moveTo(coordinates.first().x, coordinates.first().y)
                    for (i in 0 until coordinates.size - 1) {
                        val current = coordinates[i]
                        val next = coordinates[i + 1]
                        val controlPoint1 = Offset(current.x + (next.x - current.x) / 2f, current.y)
                        val controlPoint2 = Offset(current.x + (next.x - current.x) / 2f, next.y)
                        cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            next.x, next.y
                        )
                    }
                }

                // Fill gradient path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(width, canvasHeight)
                    lineTo(0f, canvasHeight)
                    close()
                }

                // Draw gradient fill (Imperial Gold to Emerald Jama glow)
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ImperialGold.copy(alpha = 0.35f),
                            EmeraldJama.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )

                // Draw glowing line stroke
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(ImperialGold, EmeraldJama)
                    ),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw coordinate nodes
                coordinates.forEach { coord ->
                    drawCircle(
                        color = MidnightCardSurface,
                        radius = 4.5.dp.toPx(),
                        center = coord
                    )
                    drawCircle(
                        color = ImperialGold,
                        radius = 3.dp.toPx(),
                        center = coord
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                )
            }
        }
    }
}

/**
 * Dark Grid-Mesh Canvas Background with GPU-accelerated drawWithCache & Sacred Gold Ambient Light
 */
@Composable
fun GridMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidianCanvas)
            .drawWithCache {
                val step = 48.dp.toPx()
                val w = size.width
                val h = size.height
                val gridPath = Path().apply {
                    var x = 0f
                    while (x <= w) {
                        moveTo(x, 0f)
                        lineTo(x, h)
                        x += step
                    }
                    var y = 0f
                    while (y <= h) {
                        moveTo(0f, y)
                        lineTo(w, y)
                        y += step
                    }
                }
                val ambientRadialGlow = Brush.radialGradient(
                    colors = listOf(BrandPrimaryBlue.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(w * 0.5f, 0f),
                    radius = w * 0.9f
                )
                onDrawBehind {
                    drawRect(brush = ambientRadialGlow)
                    drawPath(path = gridPath, color = MeshGridColor, style = Stroke(width = 0.8f))
                }
            }
    ) {
        content()
    }
}

/**
 * ⚡ Tactile 120Hz Spring-Bouncy Modifier with Haptic Impulse
 */
fun Modifier.bouncyClick(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bouncyScale"
    )
    val haptic = LocalHapticFeedback.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (_: Exception) {}
                onClick()
            }
        )
}

/**
 * 🔱 Sacred Indian Chopda Ledger Cockpit (Dual-Chamber Vault)
 */
@Composable
fun SacredLedgerCockpitCard(
    youWillGetTotal: String,
    youWillGiveTotal: String,
    activePartiesCount: Int,
    onSendAllReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = ImperialGold.copy(alpha = 0.25f),
                ambientColor = ChopdaRed.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MidnightCardSurface,
                        ElevatedSlate
                    )
                )
            )
            .border(1.2.dp, BorderStrokeDark, RoundedCornerShape(20.dp))
    ) {
        // Red Cloth Subtle Header Backing
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChopdaClothGradient)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "|| 卐 શુભ લાભ 卐 ||",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImperialGoldLight,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Offline Vault",
                            tint = ImperialGoldLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = strings.offlineEncryptedTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ImperialGoldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // Dual Golden Vault Columns (Udhar vs Jama)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Vault: You'll Get / લેવાના (Credit)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(EmeraldJamaBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = EmeraldJama,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.youWillGet,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = youWillGetTotal,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = EmeraldJama,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = "$activePartiesCount Parties Due",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            fontSize = 11.sp
                        )
                    )
                }

                // Vertical Divider Line with Gold Jewel
                Box(
                    modifier = Modifier
                        .height(68.dp)
                        .width(1.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ImperialGold.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Right Vault: You'll Give / આપવાના (Debit)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(RubyUdharBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = RubyUdhar,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.youWillGive,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = youWillGiveTotal,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = RubyUdhar,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = "Suppliers / Vendors",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Bottom Shimmering Action Bar (1-Tap WhatsApp Payment Nudges)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                    .bouncyClick(onClick = onSendAllReminders)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.sendPaymentReminders,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Text(
                                text = "Auto-formatted WhatsApp message with exact balance",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSubtle,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = ImperialGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 👥 Interactive Tactile Chopda Party Item Row
 */
@Composable
fun ChopdaPartyRow(
    name: String,
    phone: String,
    balance: Double,
    lastActive: String,
    onPartyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = balance >= 0
    val amountColor = if (balance == 0.0) TextMuted else if (isPositive) EmeraldJama else RubyUdhar
    val tagText = if (balance == 0.0) "SETTLED" else if (isPositive) "લેવાના (GET)" else "આપવાના (GIVE)"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
            .bouncyClick(onClick = onPartyClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Customer Monogram & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElevatedSlate, MidnightCardSurface)
                            )
                        )
                        .border(1.dp, if (isPositive) EmeraldJama.copy(alpha = 0.5f) else RubyUdhar.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString("")
                            .uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = ImperialGoldLight
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$phone • $lastActive",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle
                        )
                    )
                }
            }

            // Balance & WhatsApp Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${String.format("%,.0f", kotlin.math.abs(balance))}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                    )
                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor.copy(alpha = 0.85f)
                        )
                    )
                }

                // WhatsApp 1-Tap Trigger Pill
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen.copy(alpha = 0.16f))
                        .border(1.dp, WhatsAppGreen.copy(alpha = 0.4f), CircleShape)
                        .bouncyClick(onClick = onWhatsAppClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💬", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 🎯 3-Way Spring Perspective Slider (Parties vs Rokad vs Bills)
 */
@Composable
fun ChopdaPerspectiveSlider(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val tabs = listOf(strings.tabParties, strings.tabCashbook, strings.tabInvoices)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElevatedSlate)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) GoldFoilGradient
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .bouncyClick { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color(0xFF1E1005) else TextMuted
                        )
                    )
                }
            }
        }
    }
}


