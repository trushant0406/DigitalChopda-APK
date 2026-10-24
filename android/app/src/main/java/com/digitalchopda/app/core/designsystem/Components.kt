package com.digitalchopda.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
    contentPadding: PaddingValues = PaddingValues(14.dp),
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
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * 🛸 Unified Sovereign Chopda Modal Dialog
 *
 * Ensures:
 * 1. Consistent width (defaults to 94% screen width)
 * 2. Context-dependent dynamic height (wrapContentHeight with max height constraint)
 * 3. Soft-keyboard avoidance (imePadding + optional verticalScroll)
 * 4. Enterprise White-and-Blue design (Rounded 20.dp, 12.dp elevation shadow, BorderStrokeDark)
 * 5. Cohesive Header (optional icon, title, subtitle, close 'X' icon)
 * 6. Ergonomic Bottom-Sheet Presentation Mode (isBottomSheet = true)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChopdaModalDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = BrandPrimaryBlue,
    widthFraction: Float = 0.94f,
    maxHeightFraction: Float = 0.88f,
    showCloseButton: Boolean = true,
    scrollable: Boolean = true,
    isBottomSheet: Boolean = false,
    buttons: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MidnightCardSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Ergonomic Top Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(BorderStrokeDark)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Standardized Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(iconTint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy,
                                    fontSize = 17.sp
                                )
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    if (showCloseButton) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body Content (scrollable if enabled, otherwise tightly wrapped)
                if (scrollable) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        content = content
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content
                    )
                }

                // Pinned Action Buttons Row (Always visible, never scrolled away)
                if (buttons != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        buttons()
                    }
                }
            }
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .heightIn(max = 620.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0x30000000))
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(20.dp)),
                    color = MidnightCardSurface,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Standardized Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (icon != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(iconTint.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 17.sp
                                        )
                                    )
                                    if (!subtitle.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }

                            if (showCloseButton) {
                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Body Content
                        if (scrollable) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                content = content
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                content = content
                            )
                        }

                        // Pinned Action Buttons Row
                        if (buttons != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                buttons()
                            }
                        }
                    }
                }
            }
        }
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
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
            onClick()
        },
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
 * 🌟 Actionable Empty State Card with encouraging vernacular illustration & 1-tap CTA
 */
@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(18.dp)),
        color = MidnightCardSurface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandAccentSoft)
                    .border(1.dp, BrandPrimaryBlue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandPrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                GoldFoilButton(
                    text = actionText,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
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
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.5.sp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(0.8.dp, textColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
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
    statusPillText: String? = "● Offline Base Mode (Local)",
    onLanguageClick: (() -> Unit)? = null,
    onCompanyClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCompanyClick)
                .padding(vertical = 2.dp)
        ) {
            // Distinctive circular avatar badge ("AM")
            val (compAvatarBg, compAvatarText) = getAvatarColors(ownerName)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(compAvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ownerName.ifEmpty { "Admin User" }.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifEmpty { "AU" },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = compAvatarText,
                        fontSize = 13.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = firmName.ifEmpty { "ABC E-COMMERCE" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Switch Firm",
                        tint = BrandPrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (statusPillText != null) {
                    val isPending = statusPillText.contains("Pending", ignoreCase = true)
                    val pillBg = if (isPending) WarningAmberBg else EmeraldJamaBg
                    val pillColor = if (isPending) WarningAmber else EmeraldJama
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pillBg)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(pillColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusPillText.removePrefix("● ").removePrefix("• ").trim(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = pillColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Switcher Pill
            if (onLanguageClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
                        .bouncyClick(onClick = onLanguageClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = languageName ?: "EN",
                            color = BrandPrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        )
                    }
                }
            }

            // Notification Bell with unclipped badge
            Box(
                modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                        .border(1.dp, BorderStrokeDark, CircleShape)
                        .bouncyClick(onClick = onNotificationClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .border(1.5.dp, DeepObsidianCanvas, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
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

        val fillBrush = remember {
            Brush.verticalGradient(
                colors = listOf(
                    ImperialGold.copy(alpha = 0.35f),
                    EmeraldJama.copy(alpha = 0.10f),
                    Color.Transparent
                )
            )
        }
        val strokeBrush = remember {
            Brush.horizontalGradient(
                colors = listOf(ImperialGold, EmeraldJama)
            )
        }

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
                    brush = fillBrush
                )

                // Draw glowing line stroke
                drawPath(
                    path = strokePath,
                    brush = strokeBrush,
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
    val scale = animateFloatAsState(
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
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x0C0F172A),
                ambientColor = Color(0x050F172A)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Split Summary (Clean without thick banner)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: You'll Get (Receivable)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "You'll Get (Receivable)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldJamaBg)
                                .border(1.dp, EmeraldJama.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$activePartiesCount Due",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldJama,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = youWillGetTotal,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = EmeraldJama,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    )
                }

                // Subtle vertical divider
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                        .background(BorderStrokeDark)
                )

                // Right Column: You'll Give (Payable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "You'll Give (Payable)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RubyUdharBg)
                                .border(1.dp, RubyUdhar.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "1 Supplier",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = RubyUdhar,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = youWillGiveTotal,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = RubyUdhar,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Bar inside card: WhatsApp icon + reminder text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                    .bouncyClick(onClick = onSendAllReminders)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
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
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SEND ALL PAYMENT REMINDERS (Auto-formatted WhatsApp)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DarkNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = BrandPrimaryBlue,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }
    }
}

/**
 * 👥 High-Density Interactive Tactile Chopda Party Item Row
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
    val isPositive = balance > 0
    val isSettled = balance == 0.0

    val initials = remember(name) {
        val parts = name.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else {
            name.take(2).uppercase()
        }
    }
    val formattedBalance = remember(balance) {
        String.format("%,.0f", kotlin.math.abs(balance))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MidnightCardSurface)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
            .bouncyClick(onClick = onPartyClick)
            .padding(horizontal = 12.dp, vertical = 9.dp) // High-density: 30% reduced vertical padding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular avatar with initials + Party Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val (partyAvatarBg, partyAvatarText) = getAvatarColors(name)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(partyAvatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = partyAvatarText,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "$phone • $lastActive",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSubtle,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right-aligned status badge & direct WhatsApp send icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSettled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElevatedSlate)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Settle",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                } else if (isPositive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldJamaBg)
                            .border(1.dp, EmeraldJama.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "₹ $formattedBalance Due ›",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldJama,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RubyUdharBg)
                            .border(1.dp, RubyUdhar.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "₹ $formattedBalance Give ›",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = RubyUdhar,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                // WhatsApp 1-tap send icon ➤
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen.copy(alpha = 0.12f))
                        .bouncyClick(onClick = onWhatsAppClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "➤",
                        color = WhatsAppGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 🎯 3-Way Perspective Slider with Dark Navy Active Pill
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
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSlate)
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
            .padding(3.dp)
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
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) BrandPrimaryBlue // Active in brand blue
                            else Color.Transparent
                        )
                        .then(
                            if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(9.dp), spotColor = BrandPrimaryBlue.copy(alpha = 0.25f))
                            else Modifier
                        )
                        .bouncyClick { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * 🌟 Animated Digital Chopda Ledger Emblem for Loading Overlays
 */
@Composable
fun ChopdaAnimatedLogo(
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChopdaEmblemPulse")

    // Breathing scale animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // Halo pulse animation
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = breathingScale * scale
                scaleY = breathingScale * scale
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Golden & Royal Blue Aura Glow behind emblem
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.45f * haloAlpha),
                            BrandPrimaryBlue.copy(alpha = 0.30f * haloAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 2. The Digital Chopda Ledger Book
        Box(
            modifier = Modifier
                .size(width = 132.dp, height = 150.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 18.dp, bottomEnd = 18.dp),
                    ambientColor = Color(0x301E3A8A),
                    spotColor = Color(0x501D4ED8)
                )
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 18.dp, bottomEnd = 18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A),
                            BrandPrimaryBlue,
                            Color(0xFF2563EB)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(300f, 300f)
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFDE68A),
                            Color(0xFFD97706),
                            Color(0xFFF59E0B),
                            Color(0xFFFDE68A)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 18.dp, bottomEnd = 18.dp)
                )
        ) {
            // Left Book Spine with stitches
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(18.dp)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                Color(0xFF1E3A8A)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.9f))
                        )
                    }
                }
            }

            // Hanging Satin Golden Bookmark
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = (-2).dp)
                    .width(13.dp)
                    .height(50.dp)
                    .shadow(3.dp, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp), spotColor = Color(0xFFB45309))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFBBF24),
                                Color(0xFFD97706),
                                Color(0xFFB45309)
                            )
                        )
                    )
            )

            // Front Cover: Inner Filigree & Central Medallion
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Subtle Inner Hairline
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0xFFFDE68A).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                )

                // Central Medallion
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFEF3C7),
                                        Color(0xFFFDE68A),
                                        Color(0xFFD97706)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFFFFBEB), CircleShape)
                            .shadow(6.dp, CircleShape, spotColor = Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF78350F),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = "DIGITAL CHOPDA",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFDE68A),
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                        text = "બહી-ખાતા",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f),
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

/**
 * 🌟 Premium Blurred Full-Screen PDF Loading Overlay
 *
 * Displays a blurred frosted glass scrim with the illuminated animated Digital Chopda logo,
 * shimmering progress dots, and status indicator so merchants never feel stuck during PDF generation.
 */
@Composable
fun ChopdaPdfLoadingOverlay(
    visible: Boolean,
    title: String = "Generating Ledger PDF...",
    subtitle: String = "Crafting your GST compliant document...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pdfProgressDots")
        val dotPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dotPhase"
        )
        val dots = when (dotPhase.toInt() % 3) {
            0 -> "•"
            1 -> "••"
            else -> "•••"
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                // Prevent touch leakage to underlying views
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true
                ) { /* Consume taps */ }
                .background(Color(0xD90B1120)), // Frosted deep obsidian glass scrim
            contentAlignment = Alignment.Center
        ) {
            // Elegant frosted card dialog in center
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = BrandPrimaryBlue.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFFDE68A).copy(alpha = 0.65f),
                                BrandPrimaryBlue.copy(alpha = 0.45f),
                                Color(0xFFF59E0B).copy(alpha = 0.55f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Auspicious Invocation Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x22F59E0B))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "|| 卐 શુભ લાભ • શ્રી ગણેશાય નમઃ 卐 ||",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A),
                            letterSpacing = 0.6.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Logo Emblem
                    ChopdaAnimatedLogo(scale = 1.0f)

                    Spacer(modifier = Modifier.height(22.dp))

                    // Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 16.sp,
                            letterSpacing = 0.3.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // High-End Shimmer Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.80f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        val progressOffset by infiniteTransition.animateFloat(
                            initialValue = -1f,
                            targetValue = 2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "progressBarShimmer"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(4.dp)
                                .graphicsLayer {
                                    translationX = progressOffset * 180f
                                }
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF1D4ED8),
                                            Color(0xFF38BDF8),
                                            Color(0xFFF59E0B),
                                            Color(0xFF38BDF8)
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reassurance Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Processing instantly • Please wait $dots",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Outbox Sync Status Badge
 *
 * Displays a live reassurance indicator of cloud sync status.
 * - When pendingCount == 0: Green "☁️ Synced" pill.
 * - When pendingCount > 0: Amber "⏳ $pendingCount Pending" pill with 1-tap trigger to force sync.
 */
@Composable
fun SyncStatusPill(
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSynced = pendingCount == 0
    val backgroundColor = if (isSynced) EmeraldJamaBg else WarningAmberBg
    val contentColor = if (isSynced) EmeraldJama else WarningAmber
    val label = if (isSynced) "☁️ Synced" else "⏳ $pendingCount Pending"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onSyncClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}
