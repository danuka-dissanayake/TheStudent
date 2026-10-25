package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Modern Studio Capsule Microphone Icon:
 * - Sleek pill capsule body with smooth rounded geometry.
 * - Elegant curved cradle arc underneath with rounded stroke terminals.
 * - Minimalist center stem and grounded base.
 */
@Composable
fun ModernMicIcon(
    modifier: Modifier = Modifier.size(32.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.082f

        // 1. Center Studio Capsule Pill (Fill with smooth rounded corners)
        val capsuleWidth = w * 0.36f
        val capsuleHeight = h * 0.48f
        val capsuleLeft = (w - capsuleWidth) / 2f
        val capsuleTop = h * 0.12f
        val capsuleRadius = capsuleWidth / 2f

        val capsulePath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = capsuleLeft,
                        top = capsuleTop,
                        right = capsuleLeft + capsuleWidth,
                        bottom = capsuleTop + capsuleHeight
                    ),
                    cornerRadius = CornerRadius(capsuleRadius, capsuleRadius)
                )
            )
        }
        drawPath(path = capsulePath, color = tint, style = Fill)

        // 2. Elegant curved cradle arc below capsule
        val cradleLeft = w * 0.20f
        val cradleRight = w * 0.80f
        val cradleWidth = cradleRight - cradleLeft
        val cradleTop = h * 0.28f
        val cradleHeight = h * 0.40f

        val cradlePath = Path().apply {
            // Arc sweeping from left around the bottom to right
            arcTo(
                rect = Rect(
                    left = cradleLeft,
                    top = cradleTop,
                    right = cradleRight,
                    bottom = cradleTop + cradleHeight
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
        }
        drawPath(
            path = cradlePath,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // 3. Upright vertical stem
        val stemTop = cradleTop + cradleHeight
        val stemBottom = h * 0.84f
        drawLine(
            color = tint,
            start = Offset(w / 2f, stemTop),
            end = Offset(w / 2f, stemBottom),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // 4. Grounded horizontal base bar
        val baseWidth = w * 0.36f
        drawLine(
            color = tint,
            start = Offset((w - baseWidth) / 2f, stemBottom),
            end = Offset((w + baseWidth) / 2f, stemBottom),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Modern Dual-Pill Pause Icon:
 * - Two sleek vertical rounded capsules with modern proportions.
 * - Perfectly spaced with rounded caps, inspired by Apple and modern voice UIs.
 */
@Composable
fun ModernPauseIcon(
    modifier: Modifier = Modifier.size(28.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val barWidth = w * 0.16f
        val barHeight = h * 0.60f
        val barTop = (h - barHeight) / 2f
        val spacing = w * 0.18f

        // Left Pill Bar
        val leftBarX = (w / 2f) - (spacing / 2f) - barWidth
        drawRoundRect(
            color = tint,
            topLeft = Offset(leftBarX, barTop),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )

        // Right Pill Bar
        val rightBarX = (w / 2f) + (spacing / 2f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(rightBarX, barTop),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

/**
 * Modern Squircle Stop Icon:
 * - Soft rounded-corner squircle, modern alternative to harsh retro square stop icons.
 */
@Composable
fun ModernStopSquircleIcon(
    modifier: Modifier = Modifier.size(28.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val boxSize = w * 0.52f
        val boxLeft = (w - boxSize) / 2f
        val boxTop = (h - boxSize) / 2f
        val cornerRadius = boxSize * 0.28f

        drawRoundRect(
            color = tint,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    }
}

/**
 * Modern Animated Voice Waveform Icon:
 * - 4 undulating rounded vertical pill bars that rhythmically pulse when speaking.
 */
@Composable
fun ModernAudioWaveIcon(
    modifier: Modifier = Modifier.size(30.dp),
    tint: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave_bars")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_3"
    )

    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_4"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.12f
        val barRadius = barWidth / 2f
        val maxBarHeight = h * 0.70f

        val heights = listOf(h1, h2, h3, h4)
        val totalBars = heights.size
        val totalBarWidth = totalBars * barWidth
        val spacing = (w * 0.75f - totalBarWidth) / (totalBars - 1)
        val startX = (w - (totalBarWidth + spacing * (totalBars - 1))) / 2f

        heights.forEachIndexed { index, fraction ->
            val barH = maxBarHeight * fraction
            val x = startX + index * (barWidth + spacing)
            val y = (h - barH) / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barRadius, barRadius)
            )
        }
    }
}

/**
 * Modern Designer Hamburger Sidebar Menu Icon:
 * - 3 sleek parallel pill lines with rounded caps.
 * - Dynamic modern staggered width (top: full, middle: 68% accent, bottom: full)
 *   providing an artistic, premium look.
 */
@Composable
fun ModernMenuIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.Unspecified
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = h * 0.115f

        val topY = h * 0.25f
        val midY = h * 0.50f
        val botY = h * 0.75f

        val startX = w * 0.12f
        val endX = w * 0.88f
        val midEndX = w * 0.68f // Modern staggered offset

        // Top line
        drawLine(
            color = tint,
            start = Offset(startX, topY),
            end = Offset(endX, topY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Middle line (staggered modern width)
        drawLine(
            color = tint,
            start = Offset(startX, midY),
            end = Offset(midEndX, midY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Bottom line
        drawLine(
            color = tint,
            start = Offset(startX, botY),
            end = Offset(endX, botY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Modern Back Arrow Chevron Icon:
 * - Clean modern directional arrow with smooth rounded vertex and rounded caps.
 */
@Composable
fun ModernBackArrowIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.Unspecified
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.11f

        // Rounded chevron pointing left
        val tipX = w * 0.32f
        val rightX = w * 0.68f
        val tipY = h * 0.50f
        val topY = h * 0.25f
        val botY = h * 0.75f

        val path = Path().apply {
            moveTo(rightX, topY)
            lineTo(tipX, tipY)
            lineTo(rightX, botY)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

