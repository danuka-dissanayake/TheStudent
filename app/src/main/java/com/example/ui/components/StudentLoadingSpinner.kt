package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Premium Student Loading Spinner matching custom SVG animation:
 * - Dual spinning track arcs in emerald green and radiant gold.
 * - Illustrated open book with emerald cover and warm ivory pages.
 * - Bobbing graduation cap with swinging golden tassel.
 * - Gentle writing pencil motion & pulsating sparkling stars.
 * - Hardware accelerated, crisp at any DP scale.
 */
@Composable
fun StudentLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    isOnDarkBackground: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "StudentSpinnerTransition")

    // 1. Arc 1: Gold outer spinning arc (1.6s clockwise)
    val yellowSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yellowSpin"
    )

    // 2. Arc 2: Green inner spinning arc (2.4s counter-clockwise)
    val greenSpin by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "greenSpin"
    )

    // 3. Graduation cap bobbing & tilting (1.6s ease in-out)
    val bobProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bobProgress"
    )

    // 4. Sparkles pulse (1.2s ease in-out)
    val sparklePulse by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparklePulse"
    )

    // 5. Pencil writing progress (3.0s looping)
    val writeProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "writeProgress"
    )

    // Color palette tuned for light vs dark button backgrounds
    val yellowColor = if (isOnDarkBackground) Color(0xFFFDE047) else Color(0xFFF59E0B)
    val greenColor = if (isOnDarkBackground) Color(0xFF4ADE80) else Color(0xFF16A34A)
    val trackColor = if (isOnDarkBackground) Color(0x33FFFFFF) else Color(0x1F16A34A)
    val inkColor = if (isOnDarkBackground) Color(0xFF0F172A) else Color(0xFF1E293B)
    val pageColor = if (isOnDarkBackground) Color(0xFFF8FAFC) else Color(0xFFFFFDF7)
    val capColor = if (isOnDarkBackground) Color(0xFF090D16) else Color(0xFF1E293B)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Scale all 200x200 SVG coordinates to the Canvas size
            val scaleFactor = this.size.width / 200f

            withTransform({
                scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            }) {
                // --- 1. CIRCULAR TRACK ---
                drawCircle(
                    color = trackColor,
                    radius = 90f,
                    center = Offset(100f, 100f),
                    style = Stroke(width = 5.5f)
                )

                // --- 2. INNER GREEN ARC (r=82, counter-clockwise) ---
                drawArc(
                    color = greenColor,
                    startAngle = greenSpin,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(18f, 18f),
                    size = Size(164f, 164f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )

                // --- 3. OUTER GOLD ARC (r=90, clockwise) ---
                drawArc(
                    color = yellowColor,
                    startAngle = yellowSpin,
                    sweepAngle = 105f,
                    useCenter = false,
                    topLeft = Offset(10f, 10f),
                    size = Size(180f, 180f),
                    style = Stroke(width = 6.5f, cap = StrokeCap.Round)
                )

                // --- 4. OPEN BOOK ---
                // Cover binding
                val coverPath = Path().apply {
                    moveTo(42f, 106f)
                    lineTo(42f, 156f)
                    quadraticTo(72f, 146f, 100f, 158f)
                    quadraticTo(128f, 146f, 158f, 156f)
                    lineTo(158f, 106f)
                    close()
                }
                drawPath(path = coverPath, color = greenColor, style = Fill)
                drawPath(path = coverPath, color = inkColor, style = Stroke(width = 2.2f, join = StrokeJoin.Round))

                // Left Page
                val leftPagePath = Path().apply {
                    moveTo(50f, 100f)
                    quadraticTo(75f, 92f, 100f, 102f)
                    lineTo(100f, 150f)
                    quadraticTo(75f, 140f, 50f, 148f)
                    close()
                }
                drawPath(path = leftPagePath, color = pageColor, style = Fill)
                drawPath(path = leftPagePath, color = inkColor, style = Stroke(width = 2f, join = StrokeJoin.Round))

                // Right Page
                val rightPagePath = Path().apply {
                    moveTo(100f, 102f)
                    quadraticTo(125f, 92f, 150f, 100f)
                    lineTo(150f, 148f)
                    quadraticTo(125f, 140f, 100f, 150f)
                    close()
                }
                drawPath(path = rightPagePath, color = pageColor, style = Fill)
                drawPath(path = rightPagePath, color = inkColor, style = Stroke(width = 2f, join = StrokeJoin.Round))

                // Ruled lines on pages (animated text feel)
                drawLine(
                    color = inkColor.copy(alpha = 0.7f),
                    start = Offset(58f, 114f),
                    end = Offset(92f, 115f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = inkColor.copy(alpha = 0.7f),
                    start = Offset(58f, 124f),
                    end = Offset(92f, 125f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = inkColor.copy(alpha = 0.7f),
                    start = Offset(58f, 134f),
                    end = Offset(88f, 135f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )

                // Right page lines
                drawLine(
                    color = inkColor.copy(alpha = 0.7f),
                    start = Offset(108f, 115f),
                    end = Offset(142f, 114f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = inkColor.copy(alpha = 0.7f),
                    start = Offset(108f, 125f),
                    end = Offset(142f, 124f),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round
                )

                // Flourish Underline in Gold
                val flourishPath = Path().apply {
                    moveTo(56f, 140f)
                    quadraticTo(100f, 147f, 146f, 136f)
                }
                drawPath(
                    path = flourishPath,
                    color = yellowColor,
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                )

                // --- 5. SPARKLES (4-point stars) ---
                drawSparkleStar(Offset(34f, 98f), sparklePulse * 5f, yellowColor)
                drawSparkleStar(Offset(166f, 92f), (1.4f - sparklePulse * 0.5f) * 5f, yellowColor)
                drawSparkleStar(Offset(158f, 168f), sparklePulse * 4f, yellowColor)
                drawSparkleStar(Offset(40f, 166f), (1.4f - sparklePulse * 0.5f) * 4f, yellowColor)

                // --- 6. WRITING PENCIL ---
                val pencilX = 108f + (writeProgress * 36f)
                val pencilY = 114f + (sin(writeProgress * 4 * PI).toFloat() * 6f)

                withTransform({
                    translate(pencilX, pencilY)
                    rotate(degrees = 35f)
                }) {
                    // Lead point
                    val leadPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(-3f, -8f)
                        lineTo(3f, -8f)
                        close()
                    }
                    drawPath(path = leadPath, color = inkColor, style = Fill)

                    // Pencil shaft (Yellow)
                    drawRect(
                        color = yellowColor,
                        topLeft = Offset(-3f, -24f),
                        size = Size(6f, 16f)
                    )
                    drawRect(
                        color = inkColor,
                        topLeft = Offset(-3f, -24f),
                        size = Size(6f, 16f),
                        style = Stroke(width = 1.2f)
                    )

                    // Eraser (Warm page/pink)
                    drawRect(
                        color = pageColor,
                        topLeft = Offset(-3f, -30f),
                        size = Size(6f, 6f)
                    )
                    drawRect(
                        color = inkColor,
                        topLeft = Offset(-3f, -30f),
                        size = Size(6f, 6f),
                        style = Stroke(width = 1.2f)
                    )
                }

                // --- 7. GRADUATION CAP (BOBBING & SWINGING) ---
                val capYOffset = sin(bobProgress) * -4f
                val capAngle = sin(bobProgress) * 3f
                val tasselAngle = sin(bobProgress + 0.5f) * 12f

                withTransform({
                    translate(left = 0f, top = capYOffset)
                    rotate(degrees = capAngle, pivot = Offset(100f, 66f))
                }) {
                    // Cap skull base
                    val capBasePath = Path().apply {
                        moveTo(80f, 74f)
                        lineTo(80f, 86f)
                        quadraticTo(100f, 96f, 120f, 86f)
                        lineTo(120f, 74f)
                        close()
                    }
                    drawPath(path = capBasePath, color = capColor, style = Fill)
                    drawPath(path = capBasePath, color = inkColor, style = Stroke(width = 1.5f, join = StrokeJoin.Round))

                    // Mortarboard Diamond
                    val mortarboardPath = Path().apply {
                        moveTo(100f, 50f)
                        lineTo(142f, 66f)
                        lineTo(100f, 82f)
                        lineTo(58f, 66f)
                        close()
                    }
                    drawPath(path = mortarboardPath, color = capColor, style = Fill)
                    drawPath(path = mortarboardPath, color = inkColor, style = Stroke(width = 1.8f, join = StrokeJoin.Round))

                    // Mortarboard top button
                    drawCircle(color = yellowColor, radius = 2.5f, center = Offset(100f, 66f))

                    // Swinging Golden Tassel
                    withTransform({
                        rotate(degrees = tasselAngle, pivot = Offset(140f, 66f))
                    }) {
                        drawLine(
                            color = yellowColor,
                            start = Offset(140f, 66f),
                            end = Offset(140f, 86f),
                            strokeWidth = 2.2f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = yellowColor, radius = 3.5f, center = Offset(140f, 89f))
                    }
                }
            }
        }
    }
}

/**
 * Draws a 4-point sparkle star centered at [center] with radius [r].
 */
private fun DrawScope.drawSparkleStar(center: Offset, r: Float, color: Color) {
    if (r <= 0.5f) return
    val starPath = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.25f, center.y - r * 0.25f)
        lineTo(center.x + r, center.y)
        lineTo(center.x + r * 0.25f, center.y + r * 0.25f)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r * 0.25f, center.y + r * 0.25f)
        lineTo(center.x - r, center.y)
        lineTo(center.x - r * 0.25f, center.y - r * 0.25f)
        close()
    }
    drawPath(path = starPath, color = color, style = Fill)
}
