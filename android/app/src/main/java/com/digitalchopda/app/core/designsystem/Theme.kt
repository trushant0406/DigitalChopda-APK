package com.digitalchopda.app.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightSaaSColorScheme = lightColorScheme(
    primary = BrandPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = BrandAccentSoft,
    onPrimaryContainer = BrandPrimaryBlue,
    secondary = BrandPrimaryBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandAccentSoft,
    onSecondaryContainer = BrandPrimaryBlue,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = DeepObsidianCanvas, // #F5F6F8
    onBackground = TextWhite, // #1A1B1E
    surface = MidnightCardSurface, // #FFFFFF
    onSurface = TextWhite, // #1A1B1E
    surfaceVariant = ElevatedSlate, // #F0F2F5
    onSurfaceVariant = TextMuted, // #5B6472
    outline = BorderStrokeDark, // #DDE1E6
    error = DangerRed,
    onError = Color.White
)

@Composable
fun DigitalChopdaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightSaaSColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeepObsidianCanvas.toArgb()
                window.navigationBarColor = Color.White.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DigitalChopdaTypography,
        content = content
    )
}
