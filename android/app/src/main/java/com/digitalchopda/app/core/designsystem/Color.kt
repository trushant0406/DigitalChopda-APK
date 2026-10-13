package com.digitalchopda.app.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 🌐 Modern SaaS Light Canvas & Card Surfaces (Matching Web Frontend index.css / App.css)
val DeepObsidianCanvas = Color(0xFFF5F6F8) // Web: --bg: #f5f6f8
val MidnightCardSurface = Color(0xFFFFFFFF) // Web: --card-bg: #ffffff
val ElevatedSlate = Color(0xFFF0F2F5) // Web: --hover-bg: #f0f1f4 / input-bg
val BorderStrokeDark = Color(0xFFDDE1E6) // Web: --border: #dde1e6
val BorderStrokeLight = Color(0xFFE2E8F0)
val GlassHighlight = Color(0x0A000000)

// ⚡ Modern Brand Accent: Digital Royal Indigo Blue (Matching Web --accent: #3f5efb)
val BrandPrimaryBlue = Color(0xFF3F5EFB) // Web: --accent: #3f5efb
val BrandPrimaryBlueLight = Color(0xFF5B7CFA)
val BrandPrimaryBlueDeep = Color(0xFF2546D8)
val BrandAccentSoft = Color(0xFFE8ECFF) // Web: --accent-soft: #e8ecff

// Aliases for seamless drop-in backwards compatibility across all screens
val CoralPrimary = BrandPrimaryBlue
val TangerineSecondary = BrandPrimaryBlueDeep
val ImperialGold = BrandPrimaryBlue
val ImperialGoldLight = BrandPrimaryBlueLight
val ImperialGoldDeep = BrandPrimaryBlueDeep
val ChopdaGold = BrandPrimaryBlue
val ElectricBrandCyan = BrandPrimaryBlue
val ElectricBrandCyanSoft = BrandAccentSoft

val ChopdaRed = Color(0xFFEF4444)
val ChopdaRedBright = Color(0xFFDC2626)
val ChopdaRedDeep = Color(0xFF991B1B)

// 💰 Semantic Financial Ledger Colors (Udhar-Jama)
val EmeraldJama = Color(0xFF10B981) // Credit / Received / Jama
val EmeraldJamaBg = Color(0x1F10B981)
val RubyUdhar = Color(0xFFEF4444) // Debit / Due / Udhar
val RubyUdharBg = Color(0x1FEF4444)

val SuccessGreen = EmeraldJama
val SuccessGreenBg = EmeraldJamaBg
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0x1FF59E0B)
val DangerRed = RubyUdhar
val DangerRedBg = RubyUdharBg
val WhatsAppGreen = Color(0xFF25D366)

// 📝 Typography & Contrast (Dark Charcoal for crisp readability on Light Canvas)
val TextWhite = Color(0xFF1A1B1E) // Web: color: #1a1b1e (Primary Headline / Body)
val TextMuted = Color(0xFF5B6472) // Web: --muted: #5b6472 (Subtitles)
val TextSubtle = Color(0xFF8A94A6)
val ParchmentCream = Color(0xFFFFFFFF)

// 🌟 Clean Modern Royal Gradients
val GoldFoilGradient = Brush.horizontalGradient(
    colors = listOf(BrandPrimaryBlue, BrandPrimaryBlueLight, BrandPrimaryBlue)
)

val ChopdaClothGradient = Brush.linearGradient(
    colors = listOf(BrandPrimaryBlue, BrandPrimaryBlueDeep)
)

val CoralTangerineGradient = Brush.horizontalGradient(
    colors = listOf(BrandPrimaryBlue, BrandPrimaryBlueLight)
)

val CyanCoralGlowGradient = Brush.horizontalGradient(
    colors = listOf(BrandPrimaryBlue, BrandPrimaryBlueLight)
)

val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF8FAFC))
)

val JamaGlowGradient = Brush.horizontalGradient(
    colors = listOf(EmeraldJama, Color(0xFF34D399))
)

val UdharGlowGradient = Brush.horizontalGradient(
    colors = listOf(RubyUdhar, Color(0xFFFB7185))
)

val MeshGridColor = Color(0x06000000)
