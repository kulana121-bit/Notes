package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val targetColors = (if (darkTheme) DarkGlassColors else LightGlassColors).let { base ->
        base.copy(
            isReduced = reduceTransparency,
            glass = if (reduceTransparency) (if (darkTheme) CardDark else CardLight) else (if (darkTheme) GlassDark else GlassLight)
        )
    }

    // Fast, ultra-smooth and realistic 260ms color transition on theme change
    val animSpec = tween<Color>(durationMillis = 260, easing = FastOutSlowInEasing)

    val bgAnimated by animateColorAsState(targetColors.bg, animSpec, label = "bg")
    val cardAnimated by animateColorAsState(targetColors.card, animSpec, label = "card")
    val textAnimated by animateColorAsState(targetColors.text, animSpec, label = "text")
    val textSecAnimated by animateColorAsState(targetColors.textSecondary, animSpec, label = "textSec")
    val textTertAnimated by animateColorAsState(targetColors.textTertiary, animSpec, label = "textTert")
    val hairlineAnimated by animateColorAsState(targetColors.hairline, animSpec, label = "hairline")
    val accentAnimated by animateColorAsState(targetColors.accent, animSpec, label = "accent")
    val accentSecAnimated by animateColorAsState(targetColors.accentSecondary, animSpec, label = "accentSec")
    val chipOnBgAnimated by animateColorAsState(targetColors.chipOnBg, animSpec, label = "chipOnBg")
    val chipOnTxAnimated by animateColorAsState(targetColors.chipOnTx, animSpec, label = "chipOnTx")
    val glassAnimated by animateColorAsState(targetColors.glass, animSpec, label = "glass")
    val glassBorderAnimated by animateColorAsState(targetColors.glassBorder, animSpec, label = "glassBorder")
    val glassHighlightAnimated by animateColorAsState(targetColors.glassHighlight, animSpec, label = "glassHighlight")
    val fieldAnimated by animateColorAsState(targetColors.field, animSpec, label = "field")
    val markAnimated by animateColorAsState(targetColors.mark, animSpec, label = "mark")
    val dangerAnimated by animateColorAsState(targetColors.danger, animSpec, label = "danger")
    val shadowAnimated by animateColorAsState(targetColors.shadow, animSpec, label = "shadow")

    val animatedGlassColors = GlassCustomColors(
        bg = bgAnimated,
        card = cardAnimated,
        text = textAnimated,
        textSecondary = textSecAnimated,
        textTertiary = textTertAnimated,
        hairline = hairlineAnimated,
        accent = accentAnimated,
        accentSecondary = accentSecAnimated,
        chipOnBg = chipOnBgAnimated,
        chipOnTx = chipOnTxAnimated,
        glass = glassAnimated,
        glassBorder = glassBorderAnimated,
        glassHighlight = glassHighlightAnimated,
        field = fieldAnimated,
        mark = markAnimated,
        danger = dangerAnimated,
        shadow = shadowAnimated,
        isDark = darkTheme,
        isReduced = reduceTransparency
    )

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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
        LocalGlassColors provides animatedGlassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
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

