package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getAppTypography(fontFamilyStyle: String = "sans"): Typography {
    val family = when (fontFamilyStyle.lowercase()) {
        "serif" -> FontFamily.Serif
        "mono", "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.6).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.4).sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.3).sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.2).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = getAppTypography("sans")
