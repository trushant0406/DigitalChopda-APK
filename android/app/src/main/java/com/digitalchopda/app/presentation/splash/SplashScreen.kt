package com.digitalchopda.app.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.ChopdaRed
import com.digitalchopda.app.core.designsystem.ChopdaRedDeep
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.GoldFoilGradient
import com.digitalchopda.app.core.designsystem.GridMeshBackground
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.ParchmentCream
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    languageManager: LanguageManager,
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLang by languageManager.currentLanguage.collectAsState()

    val scaleAnim = remember { Animatable(0.70f) }
    val alphaAnim = remember { Animatable(0f) }
    val tiltAnim = remember { Animatable(-8f) }
    val progressAnim = remember { Animatable(0f) }

    // Shimmering float and pulse transitions (120Hz smooth)
    val infiniteTransition = rememberInfiniteTransition(label = "splashLoop")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerRotation"
    )

    // Exact 4.0-second cinematic transaction
    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(800)
            )
        }
        launch {
            tiltAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(1400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(3800, easing = LinearEasing)
            )
        }

        // Wait total 4.0 seconds (4000ms)
        delay(4000L)
        onNavigateNext(isLoggedIn)
    }

    GridMeshBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Language Selector Pill on Top Right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, ImperialGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .bouncyClick { languageManager.cycleNextLanguage() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Language",
                        tint = ImperialGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentLang.nativeName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImperialGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .scale(scaleAnim.value)
            ) {
                // Sacred 3D-embossed Chopda with Radiating Mandala
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .offset(y = floatY.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Radiating Golden Mandala Ring
                    Box(
                        modifier = Modifier
                            .size(185.dp)
                            .scale(haloPulse)
                            .rotate(shimmerRotation)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        ImperialGold.copy(alpha = 0.28f),
                                        Color(0x10EAB308),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Hardware Enclave Slab Housing
                    Box(
                        modifier = Modifier
                            .size(136.dp, 178.dp)
                            .rotate(tiltAnim.value)
                            .clip(RoundedCornerShape(26.dp))
                            .background(MidnightCardSurface)
                            .border(1.5.dp, ImperialGold.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                            .shadow(28.dp, spotColor = ImperialGold),
                        contentAlignment = Alignment.Center
                    ) {
                        // Sacred Red Velvet Bahi-Khata Ledger
                        Box(
                            modifier = Modifier
                                .size(114.dp, 154.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(ChopdaClothGradient)
                                .border(1.5.dp, ImperialGoldLight.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                                .padding(10.dp)
                        ) {
                            // Traditional Yellow Sacred Book Bookmark Ribbon
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .width(7.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(GoldFoilGradient)
                            )

                            // Inner Inscription Cockpit
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Holy Swastika Symbol
                                Text(
                                    text = "卐",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ImperialGoldLight,
                                    style = MaterialTheme.typography.displayMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Sacred Inscription
                                Text(
                                    text = "શુભ લાભ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ParchmentCream,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "ચોપડો",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ImperialGoldLight.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Floating Gilded Currency Coin Seal
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-8).dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldFoilGradient)
                            .border(1.dp, ImperialGoldLight, CircleShape)
                            .shadow(8.dp, spotColor = ImperialGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E1005)
                            )
                        )
                    }

                    // Floating Mini Bill Voucher Seal
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-8).dp, y = 8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElevatedSlate)
                            .border(1.dp, ImperialGold.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ImperialGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title with Gold Leaf Contrast
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DIGITAL ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = "CHOPDA",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = ImperialGold,
                            letterSpacing = 2.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cultural Tagline
                Text(
                    text = strings.appSubtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ImperialGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = strings.offlineEncryptedTag,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // 4-Second Dynamic Progress Ribbon & Status
                Column(
                    modifier = Modifier.width(220.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Smooth 120Hz Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ElevatedSlate)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progressAnim.value)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(GoldFoilGradient)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3-Stage Cinematic Status Text
                    val statusText = when {
                        progressAnim.value < 0.40f -> strings.splashStepInit
                        progressAnim.value < 0.75f -> strings.splashStepVault
                        else -> strings.splashStepReady
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImperialGold.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
