package com.digitalchopda.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tabular numbers font feature setting for currency alignment
const val FONT_FEATURE_TABULAR_NUMBERS = "tnum"

val DigitalChopdaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextMuted,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextWhite,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextMuted,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = TextSubtle,
        fontFeatureSettings = FONT_FEATURE_TABULAR_NUMBERS
    )
)
