package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.remember

private val DarkColorScheme = darkColorScheme(
    primary = AccentDark,
    secondary = Accent2Color,
    tertiary = ChipOnTxDark,
    background = BgDark,
    surface = CardDark,
    onPrimary = TxDark,
    onSecondary = CardDark,
    onBackground = TxDark,
    onSurface = TxDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentLight,
    secondary = Accent2Color,
    tertiary = ChipOnTxLight,
    background = BgLight,
    surface = CardLight,
    onPrimary = CardLight,
    onSecondary = TxLight,
    onBackground = TxLight,
    onSurface = TxLight
)

@Composable
fun GlassNotesTheme(
    themeSetting: String = "auto", // "auto", "light", "dark"
    reduceTransparency: Boolean = false,
    accentPaletteKey: String = "gold",
    fontFamilyStyle: String = "sans",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val palette = AccentPalettes[accentPaletteKey] ?: AccentPalettes["gold"]!!

    val baseColors = if (darkTheme) {
        DarkGlassColors.copy(
            accent = palette.darkAccent,
            accentSecondary = palette.darkSecondary,
            chipOnBg = palette.chipOnBgDark,
            chipOnTx = palette.chipOnTxDark
        )
    } else {
        LightGlassColors.copy(
            accent = palette.lightAccent,
            accentSecondary = palette.lightSecondary,
            chipOnBg = palette.chipOnBgLight,
            chipOnTx = palette.chipOnTxLight
        )
    }

    val targetColors = baseColors.copy(
        isReduced = reduceTransparency,
        glass = if (reduceTransparency) (if (darkTheme) CardDark else CardLight) else (if (darkTheme) GlassDark else GlassLight)
    )

    val colorScheme = (if (darkTheme) DarkColorScheme else LightColorScheme).copy(
        primary = targetColors.accent,
        secondary = targetColors.accentSecondary,
        tertiary = targetColors.chipOnTx
    )

    val typography = remember(fontFamilyStyle) {
        getAppTypography(fontFamilyStyle)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalGlassColors provides targetColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

object GlassTheme {
    val colors: GlassCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current
}

