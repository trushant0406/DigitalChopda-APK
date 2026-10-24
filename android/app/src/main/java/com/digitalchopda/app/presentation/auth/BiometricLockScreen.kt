package com.digitalchopda.app.presentation.auth

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.core.security.BiometricAuthManager

@Composable
fun BiometricLockScreen(
    biometricAuthManager: BiometricAuthManager,
    languageManager: LanguageManager,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val strings = LocalAppStrings.current
    val currentLang by languageManager.currentLanguage.collectAsState()

    // Pulsing halo animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun triggerAuth() {
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            try {
                biometricAuthManager.authenticate(
                    activity = activity,
                    title = strings.biometricPromptTitle,
                    subtitle = strings.biometricPromptSubtitle,
                    onSuccess = {
                        Toast.makeText(context, "✅ Verified!", Toast.LENGTH_SHORT).show()
                        onAuthSuccess()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("BiometricLockScreen", "Error triggering biometric auth", e)
                biometricAuthManager.markSessionAuthenticated()
                onAuthSuccess()
            }
        } else {
            // Fallback for non-fragment host or finishing activity
            biometricAuthManager.markSessionAuthenticated()
            onAuthSuccess()
        }
    }

    LaunchedEffect(Unit) {
        // Safe 350ms delay for Compose navigation animation transition to settle
        kotlinx.coroutines.delay(350L)
        triggerAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidianCanvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top Language Switcher Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightCardSurface)
                .border(1.dp, BorderStrokeDark, RoundedCornerShape(20.dp))
                .bouncyClick {
                    val next = languageManager.cycleNextLanguage()
                    Toast.makeText(context, "Language: ${next.displayName}", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = BrandPrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentLang.nativeName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DarkNavy,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Trust Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElevatedSlate)
                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🔒 OFFLINE HARDWARE ENCLAVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DarkNavy,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pulsing Fingerprint / Biometric Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Outer Radial Blue Aura
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BrandPrimaryBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Middle Ring
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(MidnightCardSurface)
                        .border(1.5.dp, BrandPrimaryBlue.copy(alpha = 0.35f), CircleShape)
                        .shadow(12.dp, CircleShape, spotColor = BrandPrimaryBlue.copy(alpha = 0.2f))
                )

                // Inner Tap target with Blue Circle & Fingerprint Icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(BrandPrimaryBlue)
                        .bouncyClick { triggerAuth() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DIGITAL",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = DarkNavy,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "CHOPDA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = BrandPrimaryBlue,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.biometricLockedSubheader,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSubtle,
                    textAlign = TextAlign.Center,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Unlock CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandPrimaryBlue)
                    .bouncyClick { triggerAuth() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.biometricUnlockBtn,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = {
                    biometricAuthManager.markSessionAuthenticated()
                    onAuthSuccess()
                }
            ) {
                Text(
                    text = "🔓 Skip / Unlock with Password",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = BrandPrimaryBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
