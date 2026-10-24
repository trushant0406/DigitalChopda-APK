package com.digitalchopda.app.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════════════
// 🌐 DESIGN SYSTEM TOKENS: OPTIMIZED & INNOVATIVE WHITE-AND-BLUE ENTERPRISE POS
// ════════════════════════════════════════════════════════════════════════════

// Canvas & Elevated Card Surfaces (Crisp Surface White #F8FAFC / #FFFFFF, Subtle Slate Borders #E2E8F0)
val DeepObsidianCanvas = Color(0xFFF8FAFC)        // Crisp Surface White Canvas #F8FAFC
val MidnightCardSurface = Color(0xFFFFFFFF)       // Pure White Card Surface #FFFFFF
val ElevatedSlate = Color(0xFFF1F5F9)             // Subtle Slate Surface / Input background #F1F5F9
val SurfaceMuted = Color(0xFFE2E8F0)              // Slate Divider / Border #E2E8F0
val BorderStrokeDark = Color(0xFFE2E8F0)          // Subtle Slate Border #E2E8F0
val BorderStrokeLight = Color(0xFFF1F5F9)         // Hairline Divider #F1F5F9
val GlassHighlight = Color(0x080F172A)            // Hairline Inner Glow
val GlassSurface = Color(0xF8FFFFFF)              // Frosted Surface

// ⚡ Primary Brand Accent: Royal FinTech Blue (#1D4ED8 to #2563EB)
val BrandPrimaryBlue = Color(0xFF1D4ED8)          // Royal FinTech Blue #1D4ED8
val BrandPrimaryBlueLight = Color(0xFF2563EB)     // Vibrant Blue Hover/Gradient #2563EB
val BrandPrimaryBlueDeep = Color(0xFF1E40AF)      // Navy Anchor Tone #1E40AF
val BrandAccentSoft = Color(0xFFEFF6FF)           // Soft Blue Tint #EFF6FF

// 💎 Aliases for Drop-in Backwards Compatibility
val CoralPrimary = BrandPrimaryBlue
val TangerineSecondary = BrandPrimaryBlueDeep
val ImperialGold = BrandPrimaryBlue
val ImperialGoldLight = BrandPrimaryBlueLight
val ImperialGoldDeep = BrandPrimaryBlueDeep
val ChopdaGold = BrandPrimaryBlue
val ElectricBrandCyan = Color(0xFF0284C7)          // Electric Cyan/Blue #0284C7
val ElectricBrandCyanSoft = Color(0xFFE0F2FE)

val ChopdaRed = Color(0xFFDC2626)                 // Crimson Red #DC2626
val ChopdaRedBright = Color(0xFFEF4444)
val ChopdaRedDeep = Color(0xFFB91C1C)

// 💰 Semantic Financial Ledger Tokens:
val EmeraldJama = Color(0xFF16A34A)               // Emerald Green #16A34A (Receivable / Paid / In-Stock / Active)
val EmeraldJamaBg = Color(0xFFECFDF5)             // Soft Emerald Container #ECFDF5
val RubyUdhar = Color(0xFFDC2626)                 // Crimson Red #DC2626 (Payable / Low-Stock / Alert)
val RubyUdharBg = Color(0xFFFEF2F2)               // Soft Crimson Container #FEF2F2

val SuccessGreen = EmeraldJama
val SuccessGreenBg = EmeraldJamaBg
val WarningAmber = Color(0xFFD97706)              // Amber #D97706 (Pending)
val WarningAmberBg = Color(0xFFFFFBEB)            // Soft Amber Container #FFFBEB
val DangerRed = RubyUdhar
val DangerRedBg = RubyUdharBg
val WhatsAppGreen = Color(0xFF16A34A)             // WhatsApp Green #16A34A

// 📝 Typography & Contrast Scale (Crisp dark navy body & muted slate subtitles)
val TextWhite = Color(0xFF0F172A)                 // Dark Navy / Slate 900 for primary typography
val TextMuted = Color(0xFF64748B)                 // Slate 500 for subtitles
val TextSubtle = Color(0xFF94A3B8)                // Slate 400 for meta / placeholders
val ParchmentCream = Color(0xFFFFFFFF)
val DarkNavy = Color(0xFF0F172A)                  // Deep Dark Navy

// 🌟 Modern Tactile Gradients & Shimmers
val GoldFoilGradient = Brush.horizontalGradient(
    colors = listOf(BrandPrimaryBlue, BrandPrimaryBlueLight)
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
    colors = listOf(EmeraldJama, Color(0xFF22C55E))
)

val UdharGlowGradient = Brush.horizontalGradient(
    colors = listOf(RubyUdhar, Color(0xFFEF4444))
)

val ShimmerGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFF1F5F9),
        Color(0xFFE2E8F0),
        Color(0xFFF1F5F9)
    )
)

val MeshGridColor = Color(0x060F172A)

// 🎨 Harmonious Deterministic Pastel Avatar Palette
val AVATAR_PALETTE = listOf(
    Pair(Color(0xFFEFF6FF), Color(0xFF2563EB)), // Soft Blue
    Pair(Color(0xFFECFDF5), Color(0xFF059669)), // Soft Emerald
    Pair(Color(0xFFFEF3C7), Color(0xFFD97706)), // Soft Amber
    Pair(Color(0xFFF3E8FF), Color(0xFF7C3AED)), // Soft Purple
    Pair(Color(0xFFFEE2E2), Color(0xFFDC2626)), // Soft Rose
    Pair(Color(0xFFE0E7FF), Color(0xFF4F46E5)), // Soft Indigo
    Pair(Color(0xFFCCFBF1), Color(0xFF0D9488))  // Soft Teal
)

fun getAvatarColors(name: String): Pair<Color, Color> {
    if (name.isBlank()) return AVATAR_PALETTE[0]
    val hash = kotlin.math.abs(name.hashCode())
    return AVATAR_PALETTE[hash % AVATAR_PALETTE.size]
}
