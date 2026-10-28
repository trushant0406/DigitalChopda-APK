package com.digitalchopda.app.presentation.splash

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.DigitalChopdaLogoMark
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlueDeep
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlueLight
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.data.repository.AuthRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pre-allocated sparkle particles for the auspicious prosperity aura.
 */
private class SparkleParticle(
    val angleRad: Float,
    val speedMultiplier: Float,
    val baseSizePx: Float,
    val color: Color,
    val isStar: Boolean = false
)

private val BURST_PARTICLES: List<SparkleParticle> by lazy {
    val list = ArrayList<SparkleParticle>(40)
    val colors = listOf(
        BrandPrimaryBlue,
        BrandPrimaryBlueLight,
        Color(0xFF38BDF8), // Electric Sky Blue
        Color(0xFFF59E0B), // Warm Auspicious Gold
        Color(0xFFFBBF24), // Bright Amber Gold
        Color(0xFF10B981)  // Emerald Prosperity Green
    )
    for (i in 0 until 40) {
        val angle = (i * (360f / 40f)) * (Math.PI / 180f).toFloat()
        val speed = 0.6f + ((i % 4) * 0.18f)
        val size = 2.0f + ((i % 3) * 1.5f)
        val color = colors[i % colors.size]
        val isStar = (i % 3 == 0)
        list.add(SparkleParticle(angle, speed, size, color, isStar))
    }
    list
}

/**
 * 🌟 Innovative Modern Digital Chopda Splash Screen
 *
 * Infuses the timeless trust and prosperity of the Indian Vyapar Chopda (બહી-ખાતા / ચોપડો)
 * with the lightning speed of modern FinTech.
 *
 * Sequence:
 * 1. Radiant Auspicious Header with subtle invocation "|| 卐 शुभ लाभ • श्री गणेशाय नमः 卐 ||".
 * 2. Bespoke Illuminated Digital Chopda Ledger Emblem with golden foil stitching, satin bookmark,
 *    and gleaming Rupee (₹) prosperity medallion.
 * 3. Soft golden aura waves & stardust particle glow.
 * 4. Crisp brand typography: "DIGITAL CHOPDA" & "स्मार्ट व्यापार • डिजिटल बही-खाता".
 * 5. Reassuring merchant startup progress bar (Zero technical developer jargon).
 * 6. Quick, responsive auto-navigation once startup verification completes.
 */
@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    languageManager: LanguageManager,
    companyRepository: CompanyRepository? = null,
    authRepository: AuthRepository? = null,
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    // ─── MASTER ANIMATION DRIVERS ───
    val entryAlpha = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(0.70f) }
    val particleProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleSlide = remember { Animatable(20f) }
    val progressBarProgress = remember { Animatable(0f) }
    val exitScale = remember { Animatable(1f) }
    val exitAlpha = remember { Animatable(1f) }

    var currentStatusIndex by remember { mutableIntStateOf(0) }

    // Ambient floating and breathing halo animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val goldShineAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "goldShineAngle"
    )

    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    val laserShimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserShimmerProgress"
    )

    // Single-shot navigation guard
    val hasNavigated = remember { mutableStateOf(false) }

    fun completeAndNavigate(finalLoggedInState: Boolean) {
        if (!hasNavigated.value) {
            hasNavigated.value = true
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } catch (_: Exception) {}

            scope.launch {
                launch { exitScale.animateTo(1.05f, tween(200, easing = FastOutSlowInEasing)) }
                launch { exitAlpha.animateTo(0f, tween(180)) }
                delay(180L)
                onNavigateNext(finalLoggedInState)
            }
        }
    }

    // ─── CHOREOGRAPHED STARTUP TIMELINE ───
    LaunchedEffect(Unit) {
        // Concurrently run actual startup initialization
        val startupInit = async(Dispatchers.IO) {
            try {
                companyRepository?.getActiveCompany()
            } catch (_: Exception) {}
            try {
                authRepository?.isUserLoggedIn() ?: isLoggedIn
            } catch (_: Exception) {
                isLoggedIn
            }
        }

        // Stage 1: Entrance Fade-In and Emblem Pop (0ms - 400ms)
        launch { entryAlpha.animateTo(1f, tween(300, easing = LinearOutSlowInEasing)) }
        launch {
            emblemScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }

        delay(200L)
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}

        // Stage 2: Stardust Prosperity Wave & Title Reveal (300ms - 800ms)
        launch {
            particleProgress.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }

        launch {
            titleAlpha.animateTo(1f, tween(400))
        }
        launch {
            titleSlide.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }

        // Stage 3: Dynamic Merchant Loading Steps
        launch {
            currentStatusIndex = 0
            progressBarProgress.animateTo(0.35f, tween(400, easing = FastOutSlowInEasing))
            delay(200L)
            currentStatusIndex = 1
            progressBarProgress.animateTo(0.78f, tween(400, easing = FastOutSlowInEasing))
            delay(200L)
            currentStatusIndex = 2
            progressBarProgress.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        }

        delay(1650L)
        val finalLoggedInState = startupInit.await()
        delay(250L)
        completeAndNavigate(finalLoggedInState)
    }

    val statusTexts = listOf(
        strings.splashStepInit,
        strings.splashStepVault,
        strings.splashStepReady
    )

    val statusSubtitles = listOf(
        strings.splashStepSubInit,
        strings.splashStepSubVault,
        strings.splashStepSubReady
    )

    // ─── MAIN FULL-SCREEN CANVAS ───
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidianCanvas)
            .graphicsLayer {
                scaleX = exitScale.value
                scaleY = exitScale.value
                alpha = exitAlpha.value
            }
    ) {
        // Specular Radial Background Glow (Royal Blue + Soft Warm Gold)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BrandPrimaryBlue.copy(alpha = 0.08f),
                        BrandAccentSoft.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.85f * haloPulse
                ),
                center = center,
                radius = size.width * 0.85f * haloPulse
            )
        }

        // ─── MAIN VERTICAL CONTENT COLUMN ───
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .graphicsLayer { alpha = entryAlpha.value }
        ) {
            // ─── TOP AUSPICIOUS TRUST HEADER ───
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCardSurface)
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
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

            Spacer(modifier = Modifier.weight(1f))

            // ─── CENTERPIECE: BESPOKE DIGITAL CHOPDA MASTER EMBLEM ───
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = emblemScale.value
                        scaleY = emblemScale.value
                        translationY = floatOffset * density
                    },
                contentAlignment = Alignment.Center
            ) {
                // 1. Concentric Radial Aura Circles
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(haloPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFEF3C7).copy(alpha = 0.50f),
                                    BrandAccentSoft.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 2. Decorative Golden Stardust Particles
                val progress = particleProgress.value
                if (progress > 0.05f) {
                    Canvas(modifier = Modifier.size(260.dp)) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        for (particle in BURST_PARTICLES) {
                            val dist = progress * 125f * particle.speedMultiplier
                            val px = centerX + (cos(particle.angleRad) * dist)
                            val py = centerY + (sin(particle.angleRad) * dist)
                            val alpha = (1f - (progress * 0.8f)).coerceIn(0f, 1f)

                            if (particle.isStar) {
                                val r = particle.baseSizePx * 1.6f
                                val p = Path().apply {
                                    moveTo(px, py - r)
                                    lineTo(px + (r * 0.35f), py)
                                    lineTo(px + r, py)
                                    lineTo(px + (r * 0.35f), py)
                                    lineTo(px, py + r)
                                    lineTo(px - (r * 0.35f), py)
                                    lineTo(px - r, py)
                                    lineTo(px - (r * 0.35f), py)
                                    close()
                                }
                                drawPath(p, color = particle.color.copy(alpha = alpha))
                            } else {
                                drawCircle(
                                    color = particle.color.copy(alpha = alpha),
                                    radius = particle.baseSizePx,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }
                }

                // 3. The Official DigitalChopda Vector Logo Mark
                DigitalChopdaLogoMark(
                    size = 150.dp,
                    animated = true,
                    showGlow = true
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── BRAND TYPOGRAPHY: CRISP & AUSPICIOUS ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = titleSlide.value * density
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DIGITAL",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = DarkNavy,
                            letterSpacing = 1.5.sp,
                            fontSize = 30.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "CHOPDA",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = BrandPrimaryBlue,
                            letterSpacing = 1.5.sp,
                            fontSize = 30.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ERP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Golden underline divider
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    BrandPrimaryBlue,
                                    Color(0xFFF59E0B),
                                    BrandPrimaryBlue
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "स्मार्ट व्यापार • डिजिटल बહી-खाता",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ─── HIGH-TECH BANK-GRADE PROGRESS CARD (INNOVATIVE & SECURE) ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(22.dp),
                            spotColor = Color(0x1F1D4ED8),
                            ambientColor = Color(0x0F000000)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.35f),
                                    Color(0xFF6366F1).copy(alpha = 0.20f),
                                    Color(0xFF10B981).copy(alpha = 0.35f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Top Telemetry Row: Cryptographic Security Badge & Live Radar Handshake
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Bank-Grade Vault Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "256-BIT AES ENCRYPTED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    letterSpacing = 0.6.sp
                                )
                            }

                            // Right: Pulsing Radar Beacon + Live Percent
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live Radar Beacon Canvas
                                Box(
                                    modifier = Modifier.size(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        // Animated expanding ping wave
                                        val pingRadius = 4f + (radarPulse * 16f)
                                        val pingAlpha = (1f - radarPulse).coerceIn(0f, 0.7f)
                                        drawCircle(
                                            color = Color(0xFF10B981).copy(alpha = pingAlpha),
                                            radius = pingRadius,
                                            center = center
                                        )
                                        // Solid inner core
                                        drawCircle(
                                            color = Color(0xFF10B981),
                                            radius = 4.5f,
                                            center = center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Percentage Badge Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ElevatedSlate)
                                        .border(0.75.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${(progressBarProgress.value * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DarkNavy
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Centerpiece Dynamic Loading Step: Modern Icon Squircle + Title + Subtitle
                        AnimatedContent(
                            targetState = currentStatusIndex,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                            label = "statusCrossfade"
                        ) { idx ->
                            val stepIcon = when (idx) {
                                0 -> Icons.Default.Lock
                                1 -> Icons.Default.Security
                                else -> Icons.Default.CheckCircle
                            }
                            val iconTint = when (idx) {
                                0 -> Color(0xFF2563EB)
                                1 -> Color(0xFF4F46E5)
                                else -> Color(0xFF10B981)
                            }
                            val iconBg = when (idx) {
                                0 -> Color(0xFFEFF6FF)
                                1 -> Color(0xFFEEF2FF)
                                else -> Color(0xFFECFDF5)
                            }
                            val iconBorder = when (idx) {
                                0 -> Color(0xFFBFDBFE)
                                1 -> Color(0xFFC7D2FE)
                                else -> Color(0xFFA7F3D0)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Squircle Icon Container
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iconBg)
                                        .border(1.dp, iconBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = stepIcon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Text Stack: Status Title + Cryptographic Subtitle
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = statusTexts.getOrElse(idx) { strings.splashStepReady },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = statusSubtitles.getOrElse(idx) { strings.splashStepSubReady },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Fluid Neon Progress Bar with Animated Laser Shimmer Beam
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(3.5.dp))
                        ) {
                            val curProgress = progressBarProgress.value.coerceIn(0f, 1f)
                            if (curProgress > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(curProgress)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(3.5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF2563EB),
                                                    Color(0xFF4F46E5),
                                                    Color(0xFF06B6D4),
                                                    Color(0xFF10B981)
                                                )
                                            )
                                        )
                                ) {
                                    // Animated specular laser shimmer traveling across the active bar
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val barWidth = size.width
                                        val beamCenter = laserShimmerProgress * barWidth
                                        val beamWidth = 60f
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.65f),
                                                    Color.Transparent
                                                ),
                                                startX = beamCenter - beamWidth,
                                                endX = beamCenter + beamWidth
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Integrated Cryptographic Trust Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🛡️", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "DPDP 2023 Safe",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔒", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "SQLiteCipher",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚡", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Offline First",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Trust Pill Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSlate)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🇮🇳 Bank-Grade Merchant Protection • Made in Bharat",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
