package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Controller to manage fast, high-performance radial light and dark wave transitions
 * bursting from the theme switch button.
 */
class ThemeTransitionState {
    var buttonOrigin by mutableStateOf(Offset.Unspecified)
    var isTransitioning by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var isTargetDark by mutableStateOf(false)

    fun recordOrigin(offset: Offset) {
        buttonOrigin = offset
    }

    fun startTransition(toDark: Boolean, origin: Offset? = null, scope: CoroutineScope) {
        if (origin != null && origin.isSpecified) {
            buttonOrigin = origin
        }
        isTargetDark = toDark
        isTransitioning = true
        scope.launch {
            val anim = Animatable(0f)
            // Ultra-responsive, fluid 360ms fast-out cubic bezier for zero-lag 120fps feel
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 360,
                    easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
                )
            ) {
                progress = value
            }
            isTransitioning = false
            progress = 0f
        }
    }
}

val LocalThemeTransition = compositionLocalOf { ThemeTransitionState() }

@Composable
fun ThemeLightWaveOverlay(
    state: ThemeTransitionState,
    modifier: Modifier = Modifier
) {
    if (!state.isTransitioning || state.progress <= 0f) return

    val progress = state.progress
    val isToDark = state.isTargetDark
    val origin = if (state.buttonOrigin.isSpecified) state.buttonOrigin else Offset(800f, 150f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val ox = origin.x.coerceIn(0f, w)
        val oy = origin.y.coerceIn(0f, h)
        val center = Offset(ox, oy)

        // Calculate maximum corner distance
        val d1 = hypot(ox, oy)
        val d2 = hypot(w - ox, oy)
        val d3 = hypot(ox, h - oy)
        val d4 = hypot(w - ox, h - oy)
        val maxRadius = maxOf(d1, d2, d3, d4) * 1.15f

        val currentRadius = progress * maxRadius
        // Wave alpha fades out smoothly towards the end of expansion
        val waveAlpha = if (progress < 0.6f) 1f else ((1f - progress) / 0.4f).coerceIn(0f, 1f)

        if (!isToDark) {
            // === LIGHT MODE: LUMINOUS SUNLIGHT BURST ===
            // Expanding radiant daylight wave with bright golden-white photonic glow
            val lightBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f * waveAlpha),
                    Color(0xFFFFFBEB).copy(alpha = 0.85f * waveAlpha),
                    Color(0xFFFDE68A).copy(alpha = 0.50f * waveAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius.coerceAtLeast(10f)
            )

            drawCircle(
                brush = lightBrush,
                radius = currentRadius.coerceAtLeast(10f),
                center = center,
                style = Fill
            )

            // Bright wavefront shockwave ring
            if (currentRadius > 15f) {
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = 0.75f * waveAlpha),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 8f * (1f - progress * 0.5f))
                )
            }
        } else {
            // === DARK MODE: VELVET MIDNIGHT WAVE ===
            // Expanding deep obsidian nebula wave with celestial indigo rim
            val darkBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF030712).copy(alpha = 0.96f * waveAlpha),
                    Color(0xFF0F172A).copy(alpha = 0.85f * waveAlpha),
                    Color(0xFF1E1B4B).copy(alpha = 0.45f * waveAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius.coerceAtLeast(10f)
            )

            drawCircle(
                brush = darkBrush,
                radius = currentRadius.coerceAtLeast(10f),
                center = center,
                style = Fill
            )

            // Electric indigo/cyan wavefront ring
            if (currentRadius > 15f) {
                drawCircle(
                    color = Color(0xFF6366F1).copy(alpha = 0.70f * waveAlpha),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 8f * (1f - progress * 0.5f))
                )
            }
        }
    }
}
