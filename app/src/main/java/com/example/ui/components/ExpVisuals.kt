package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpTier
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The visual language of EXP — one medallion, one hero card, one badge ladder.
 *
 * These live here rather than inside `ExpScreen` because the chat profile shows
 * the same three things: a student's level card and their badges must look the
 * same wherever they appear, or the two screens read as two different features.
 */

/** The dark end of the hero gradient. */
val ExpHeroDeep = Color(0xFF053B2B)

/** One colour per rung, so a badge is recognisable at a glance. */
fun expBadgeColor(code: String): Color = when (code.lowercase(Locale.US)) {
    "helper" -> Color(0xFF10B981)
    "guide" -> Color(0xFF0EA5E9)
    "mentor" -> Color(0xFF8B5CF6)
    "legend" -> Color(0xFFF59E0B)
    else -> Color(0xFF059669)
}

/** 1240 → "1,240": big numbers read better with the separator. */
fun formatExp(value: Int): String {
    if (value < 1000) return value.toString()
    return String.format(Locale.US, "%,d", value)
}

/**
 * A hexagon badge with a star, drawn rather than an icon so it can glow, carry a
 * tier colour and grey out when it is still locked.
 *
 * [shinePhase] paints a sheen that sweeps across an earned badge. It is read
 * inside the draw scope (`shinePhase()`), so animating it only invalidates the
 * pixels — nothing recomposes.
 */
@Composable
fun TierMedallion(
    code: String,
    size: Dp,
    glow: Boolean,
    earned: Boolean = true,
    shinePhase: (() -> Float)? = null
) {
    val base = if (earned) expBadgeColor(code) else Color(0xFFCBD5E1)
    val light = if (earned) lerp(base, Color.White, 0.42f) else Color(0xFFE7EDF3)

    Canvas(modifier = Modifier.size(size)) {
        val maxRadius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val hexRadius = maxRadius * 0.62f

        if (glow) {
            // Three soft rings, widest first: a halo, not a drop shadow.
            drawCircle(base.copy(alpha = 0.10f), maxRadius, center)
            drawCircle(base.copy(alpha = 0.14f), maxRadius * 0.84f, center)
            drawCircle(base.copy(alpha = 0.18f), maxRadius * 0.70f, center)
        }

        // Ribbon tails behind the hexagon.
        val tail = maxRadius * 0.42f
        val tailTop = hexRadius * 0.52f
        val tailBottom = maxRadius * 0.94f
        listOf(-1f, 1f).forEach { side ->
            val path = Path().apply {
                moveTo(center.x + side * tailTop, center.y + hexRadius * 0.40f)
                lineTo(center.x + side * tail, center.y + tailTop)
                lineTo(center.x + side * tail * 0.62f, center.y + tailBottom)
                lineTo(center.x + side * tail * 0.10f, center.y + hexRadius * 0.66f)
                close()
            }
            drawPath(path, color = lerp(base, Color.Black, 0.16f))
        }

        // Hexagon face.
        val hex = Path().apply {
            for (index in 0 until 6) {
                val angle = (-PI / 2 + index * PI / 3).toFloat()
                val x = center.x + hexRadius * cos(angle)
                val y = center.y + hexRadius * sin(angle)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = hex,
            brush = Brush.linearGradient(
                colors = listOf(light, base),
                start = Offset(center.x - hexRadius, center.y - hexRadius),
                end = Offset(center.x + hexRadius, center.y + hexRadius)
            )
        )
        drawPath(
            path = hex,
            color = Color.White.copy(alpha = if (glow) 0.55f else 0.35f),
            style = Stroke(width = maxRadius * 0.05f)
        )

        drawStarAt(
            center = center,
            outer = hexRadius * 0.46f,
            color = if (earned) Color.White.copy(alpha = 0.96f) else Color(0xFFF8FAFC)
        )

        // The sheen: two white bands half a cycle apart, travelling across the
        // face of an earned badge. The second band is entering as the first
        // leaves, so the metal is always catching light — a continuous shimmer
        // rather than a sweep, a pause, and a sweep.
        if (earned && shinePhase != null) {
            val phase = shinePhase().coerceIn(0f, 1f)
            val band = hexRadius * 1.15f
            val sweepStart = center.x - hexRadius - band * 0.5f
            val sweepEnd = center.x + hexRadius + band * 0.5f
            clipPath(hex) {
                listOf(0f, 0.5f).forEach { gap ->
                    val local = (phase + gap) % 1f
                    val bandCentre = sweepStart + (sweepEnd - sweepStart) * local
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.54f),
                                Color.Transparent
                            ),
                            start = Offset(bandCentre - band / 2f, center.y - hexRadius),
                            end = Offset(bandCentre + band / 2f, center.y + hexRadius)
                        ),
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = Size(maxRadius * 2f, maxRadius * 2f)
                    )
                }
            }
        }
    }
}

/** A five-point star, filled. */
private fun DrawScope.drawStarAt(center: Offset, outer: Float, color: Color) {
    val inner = outer * 0.46f
    val path = Path()
    for (index in 0 until 10) {
        val angle = (-PI / 2 + index * PI / 5).toFloat()
        val radius = if (index % 2 == 0) outer else inner
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

/**
 * The emerald hero: medallion on the left, "Total EXP", the number, an epithet.
 *
 * Used by the EXP screen and by a student's chat profile, so a level always looks
 * like a level.
 */
@Composable
fun ExpHeroCard(
    total: Int,
    tier: ExpTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ExpHeroDeep, Color(0xFF075E45), EmeraldPrimary),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .padding(
                horizontal = 18.dp,
                vertical = if (compact) 16.dp else 20.dp
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TierMedallion(
                code = tier.code,
                size = if (compact) 74.dp else 96.dp,
                glow = true
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total EXP",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = formatExp(total),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = if (compact) 32.sp else 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (compact) "Level ${tier.level} · ${tier.name}" else tier.epithet,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * The five rungs as a row of medallions: earned ones in colour, locked ones grey
 * with the EXP still needed.
 */
@Composable
fun ExpBadgeLadder(
    total: Int,
    modifier: Modifier = Modifier,
    medallionSize: Dp = 44.dp,
    showCaption: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        ExpTier.LADDER.forEach { step ->
            val earned = total >= step.min
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                TierMedallion(
                    code = step.code,
                    size = medallionSize,
                    glow = false,
                    earned = earned
                )
                if (showCaption) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = step.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = if (earned) FontWeight.Bold else FontWeight.Medium,
                        color = if (earned) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (earned) "Earned" else formatExp(step.min),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = if (earned) EmeraldPrimary else Color(0xFF94A3B8),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
