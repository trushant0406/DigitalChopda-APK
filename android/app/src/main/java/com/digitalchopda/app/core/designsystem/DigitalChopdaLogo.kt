package com.digitalchopda.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── BRAND LOGO GRADIENT PALETTE ───
val LogoGradientStart = Color(0xFF3B82F6)   // Electric Royal Blue
val LogoGradientMid   = Color(0xFF6366F1)   // FinTech Indigo
val LogoGradientEnd   = Color(0xFF10B981)   // Emerald Prosperity Green

val LogoSpineIndigo   = Color(0xFF4F46E5)   // Deep Indigo Spine & Rupee
val LogoCheckEmerald  = Color(0xFF10B981)   // Crisp Emerald Checkmark

val DigitalChopdaGradientBrush = Brush.linearGradient(
    colors = listOf(LogoGradientStart, LogoGradientMid, LogoGradientEnd),
    start = Offset(0f, 0f),
    end = Offset(400f, 400f)
)

enum class LogoVariant {
    Mark,   // Squircle icon badge only
    Full    // Squircle icon badge + Wordmark + Tagline
}

/**
 * 🌟 The Official DigitalChopda Vector Logo Mark
 *
 * Implements the identical vector geometry as the web application:
 * - Squircle container with 3-stop linear gradient (#3B82F6 -> #6366F1 -> #10B981).
 * - Inner specular border (#FFFFFF 40% opacity).
 * - Open white Chopda Ledger Book with rounded outer corners and straight inner spine.
 * - Center book spine (#4F46E5).
 * - Left Page: Stylized Indian Rupee symbol (₹) in FinTech Indigo.
 * - Right Page: Dynamic Emerald Checkmark (✓) in Prosperity Green.
 * - Optional continuous subtle breathing glow or shimmer pass.
 */
@Composable
fun DigitalChopdaLogoMark(
    size: Dp = 48.dp,
    animated: Boolean = true,
    showGlow: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cornerRadius = size * 0.26f

    val infiniteTransition = rememberInfiniteTransition(label = "logoGlowTransition")
    val glowScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowScale"
        )
    } else {
        remember { Animatable(1f) }.asState()
    }

    val glowAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { Animatable(0.3f) }.asState()
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Drop Glow
        if (showGlow) {
            Box(
                modifier = Modifier
                    .size(size * 1.08f)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                    }
                    .clip(RoundedCornerShape(cornerRadius * 1.1f))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LogoGradientStart.copy(alpha = glowAlpha),
                                LogoGradientMid.copy(alpha = glowAlpha * 0.8f),
                                LogoGradientEnd.copy(alpha = glowAlpha * 0.6f)
                            )
                        )
                    )
            )
        }

        // 2. Main Squircle Card Container
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = (size.value * 0.12f).dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = LogoGradientMid.copy(alpha = 0.35f),
                    spotColor = LogoGradientStart.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(LogoGradientStart, LogoGradientMid, LogoGradientEnd),
                        start = Offset.Zero,
                        end = Offset(size.value * 2.5f, size.value * 2.5f)
                    )
                )
                .border(
                    width = (size.value * 0.025f).coerceAtLeast(1f).dp,
                    color = Color.White.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            // 3. Canvas rendering the Chopda Book, Spine, Rupee & Checkmark
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height

                // Target coordinate mapping based on 100x100 web SVG:
                // Book spans roughly from x: 18% to 82%, y: 22% to 78%
                val bookTop = h * 0.24f
                val bookBottom = h * 0.76f
                val bookHeight = bookBottom - bookTop
                val bookCenter = w * 0.50f
                val spineGap = w * 0.015f
                val cornerR = bookHeight * 0.12f

                // Left Page Background
                val leftPagePath = Path().apply {
                    val left = w * 0.20f
                    val right = bookCenter - spineGap
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = bookTop,
                            right = right,
                            bottom = bookBottom,
                            topLeftCornerRadius = CornerRadius(cornerR, cornerR),
                            bottomLeftCornerRadius = CornerRadius(cornerR * 0.8f, cornerR * 0.8f),
                            topRightCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero
                        )
                    )
                }
                drawPath(leftPagePath, color = Color.White.copy(alpha = 0.96f), style = Fill)

                // Right Page Background
                val rightPagePath = Path().apply {
                    val left = bookCenter + spineGap
                    val right = w * 0.80f
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = bookTop,
                            right = right,
                            bottom = bookBottom,
                            topLeftCornerRadius = CornerRadius.Zero,
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            topRightCornerRadius = CornerRadius(cornerR, cornerR),
                            bottomRightCornerRadius = CornerRadius(cornerR * 0.8f, cornerR * 0.8f)
                        )
                    )
                }
                drawPath(rightPagePath, color = Color.White.copy(alpha = 0.92f), style = Fill)

                // Center Spine Divider Line
                val spineStrokeWidth = (w * 0.035f).coerceAtLeast(1.5f)
                drawLine(
                    color = LogoSpineIndigo,
                    start = Offset(bookCenter, bookTop - (h * 0.01f)),
                    end = Offset(bookCenter, bookBottom + (h * 0.01f)),
                    strokeWidth = spineStrokeWidth,
                    cap = StrokeCap.Round
                )

                // Left Page: Rupee Symbol (₹)
                val rupeeStrokeWidth = (w * 0.036f).coerceAtLeast(1.4f)
                val rLeft = w * 0.26f
                val rRight = w * 0.43f
                val rTop1 = h * 0.36f
                val rTop2 = h * 0.44f

                // Top horizontal bar
                drawLine(
                    color = LogoSpineIndigo,
                    start = Offset(rLeft, rTop1),
                    end = Offset(rRight, rTop1),
                    strokeWidth = rupeeStrokeWidth,
                    cap = StrokeCap.Round
                )

                // Second horizontal bar
                drawLine(
                    color = LogoSpineIndigo,
                    start = Offset(rLeft, rTop2),
                    end = Offset(rRight - (w * 0.02f), rTop2),
                    strokeWidth = rupeeStrokeWidth,
                    cap = StrokeCap.Round
                )

                // Vertical stem & upper loop & diagonal leg
                val rupeePath = Path().apply {
                    moveTo(rLeft + (w * 0.02f), rTop1)
                    lineTo(rLeft + (w * 0.02f), rTop2)
                    // upper loop
                    val loopRight = rRight
                    val loopBottom = h * 0.52f
                    cubicTo(
                        rRight + (w * 0.01f), rTop2,
                        rRight + (w * 0.01f), loopBottom,
                        rLeft + (w * 0.06f), loopBottom
                    )
                    // diagonal leg
                    lineTo(rLeft + (w * 0.01f), h * 0.65f)
                }
                drawPath(
                    rupeePath,
                    color = LogoSpineIndigo,
                    style = Stroke(
                        width = rupeeStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Right Page: Emerald Checkmark (✓)
                val checkStrokeWidth = (w * 0.052f).coerceAtLeast(2.0f)
                val checkPath = Path().apply {
                    moveTo(w * 0.56f, h * 0.52f)
                    lineTo(w * 0.63f, h * 0.62f)
                    lineTo(w * 0.75f, h * 0.38f)
                }
                drawPath(
                    checkPath,
                    color = LogoCheckEmerald,
                    style = Stroke(
                        width = checkStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * 🌟 Complete DigitalChopda Brand Header Component
 *
 * Combines the official vector logo mark, typographic wordmark ("DigitalChopda"),
 * modern FinTech "ERP" badge, and auspicious vyapar invocation pill.
 */
@Composable
fun DigitalChopdaLogo(
    variant: LogoVariant = LogoVariant.Full,
    markSize: Dp = 48.dp,
    showTagline: Boolean = true,
    showAuspiciousHeader: Boolean = false,
    inverseTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (showAuspiciousHeader) {
            // Sacred Auspicious Invocation Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "卐",
                    fontSize = 11.sp,
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "शुभ लाभ • श्री गणेशाय नमः",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "卐",
                    fontSize = 11.sp,
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (variant == LogoVariant.Mark) {
            DigitalChopdaLogoMark(size = markSize)
        } else {
            // Full Brand Composition: Mark + Wordmark Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                DigitalChopdaLogoMark(size = markSize)

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Digital",
                            fontSize = (markSize.value * 0.44f).coerceIn(18f, 32f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (inverseTheme) Color.White else DarkNavy,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Chopda",
                            fontSize = (markSize.value * 0.44f).coerceIn(18f, 32f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPrimaryBlue,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // ERP Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ERP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LogoCheckEmerald,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    if (showTagline) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VYAPAR KA DIGITAL CHOPDA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inverseTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(LogoCheckEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GST READY",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = LogoCheckEmerald,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
