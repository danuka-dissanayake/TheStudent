package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MaisonNeueFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The "you've been rated" moment.
 *
 * Ported from the design file `TheStudent – Rating celebration.html`: white
 * background, no card and no scrim — only type, five bursting stars and
 * confetti. Timeline (ms):
 *
 * ```
 * 0      progress line starts shrinking (2600, linear)
 * 80     headline rises and settles (640, cubic-bezier(.22,1,.36,1))
 * 140    score rises and settles (640, same)
 * 120 / 210 / 300 / 390 / 480   stars 1→5 burst from the score centre (700, back-out)
 * 150–1750   confetti spawns, falls, sways and flips
 * 640    "Nimal rated you" fades in (420, ease-out)
 * 760    the word pill fades in (420, ease-out)
 * 2400   exit: fade + lift (500) → gone by 2900
 * tap    exit in 300 ms
 * ```
 *
 * It never blocks the app: it finishes on its own, and a tap closes it early.
 */
@Composable
fun RatingCelebrationOverlay(
    raterName: String,
    stars: Int,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val score = stars.coerceIn(1, 5)
    val firstName = raterName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "A student" }
    val initials = remember(raterName) {
        val parts = raterName.trim().split(" ").filter { it.isNotBlank() }
        val first = parts.firstOrNull()?.firstOrNull()?.toString().orEmpty()
        val second = parts.getOrNull(1)?.firstOrNull()?.toString().orEmpty()
        (first + second).uppercase().ifBlank { "?" }
    }
    val word = when (score) {
        1 -> "Not great"
        2 -> "Okay"
        3 -> "Good"
        4 -> "Very good"
        else -> "Excellent"
    }
    val scoreText = "$score.0"

    // One animatable per moving part, all driven from a single coroutine so the
    // choreography in the timeline above is easy to read and easy to change.
    val headlineAnim = remember { Animatable(0f) }
    val scoreAnim = remember { Animatable(0f) }
    val starAnims = remember { List(5) { Animatable(0f) } }
    val whoAnim = remember { Animatable(0f) }
    val pillAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(1f) }
    val exitAnim = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    fun finish() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    fun close(quick: Boolean) {
        if (closing) return
        closing = true
        scope.launch {
            exitAnim.animateTo(1f, tween(if (quick) 300 else 500))
            finish()
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                delay(80)
                headlineAnim.animateTo(1f, tween(640, easing = SETTLE))
            }
            launch {
                delay(140)
                scoreAnim.animateTo(1f, tween(640, easing = SETTLE))
            }
            starAnims.forEachIndexed { index, anim ->
                launch {
                    delay(120L + index * 90L)
                    anim.animateTo(1f, tween(700, easing = BURST))
                }
            }
            launch {
                delay(640)
                whoAnim.animateTo(1f, tween(420, easing = EASE_OUT))
            }
            launch {
                delay(760)
                pillAnim.animateTo(1f, tween(420, easing = EASE_OUT))
            }
            launch {
                progressAnim.animateTo(0f, tween(2600, easing = LinearEasing))
            }
        }
        // The bar has run out: leave the way the design asks, then hand control back.
        close(quick = false)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("rating_celebration")
            .pointerInput(Unit) {
                detectTapGestures { close(quick = true) }
            },
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth
        val height = maxHeight
        val density = LocalDensity.current

        // A breath of emerald light behind everything — no panel, no card.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.10f),
                            Color(0xFF10B981).copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = with(density) {
                            Offset(width.toPx(), height.toPx() * 0.42f)
                        },
                        radius = with(density) { width.toPx() * 1.15f }
                    )
                )
        )

        ConfettiLayer(alpha = 1f - exitAnim.value)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - exitAnim.value
                    translationY = -width.toPx() * 0.03f * exitAnim.value
                },
            verticalArrangement = Arrangement.Center
        ) {
            // ── headline ───────────────────────────────────────────────────
            Text(
                text = "You've been rated!",
                fontFamily = MaisonNeueFontFamily,
                fontSize = with(density) { (width * 0.076f).toSp() },
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02).em,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    val p = headlineAnim.value
                    alpha = p.coerceIn(0f, 1f)
                    translationY = width.toPx() * 0.06f * (1f - p)
                    val s = 1f + 0.10f * (1f - p)
                    scaleX = s
                    scaleY = s
                }
            )

            // ── the five stars ─────────────────────────────────────────────
            val starsHeight = width * 0.25f
            Box(
                modifier = Modifier
                    .width(width)
                    .height(starsHeight)
            ) {
                BURST_STARS.forEachIndexed { index, star ->
                    val starSize = width * star.size
                    // The canvas is larger than the star so the halo can fade out
                    // instead of being cut off by the layout bounds.
                    val haloSize = starSize * 1.8f
                    Canvas(
                        modifier = Modifier
                            .offset(
                                x = width * 0.5f + width * star.dx - haloSize / 2f,
                                y = width * star.top - (haloSize - starSize) / 2f
                            )
                            .size(haloSize)
                            .graphicsLayer {
                                val p = starAnims[index].value
                                scaleX = 0.05f + 0.95f * p
                                scaleY = scaleX
                                rotationZ = -40f + (40f + star.rot) * p
                                translationX = -width.toPx() * star.dx * (1f - p)
                                translationY = width.toPx() * star.fromY * (1f - p)
                                alpha = (p * 4f).coerceIn(0f, 1f)
                            }
                    ) {
                        val r = starSize.toPx() / 2f
                        drawStarGlow(center = center, radius = r)
                        drawStar(center = center, outer = r * 0.98f)
                    }
                }
            }

            // ── the score ──────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val p = scoreAnim.value
                    alpha = p.coerceIn(0f, 1f)
                    translationY = width.toPx() * 0.06f * (1f - p)
                    val s = 1f + 0.10f * (1f - p)
                    scaleX = s
                    scaleY = s
                }
            ) {
                Text(
                    text = scoreText,
                    style = TextStyle(
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = with(density) { (width * 0.30f).toSp() },
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.04).em,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                        )
                    )
                )
                Text(
                    text = "out of 5",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = with(density) { (width * 0.038f).toSp() },
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(Modifier.height(width * 0.055f))

            // ── who rated you ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    val p = whoAnim.value
                    alpha = p
                    translationY = width.toPx() * 0.016f * (1f - p)
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(width * 0.084f)
                        .clip(CircleShape)
                        .background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = with(density) { (width * 0.035f).toSp() },
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
                Spacer(Modifier.width(width * 0.026f))
                Text(
                    text = firstName,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = with(density) { (width * 0.046f).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(width * 0.014f))
                Text(
                    text = "rated you",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = with(density) { (width * 0.046f).toSp() },
                    color = Color(0xFF64748B)
                )
            }

            Spacer(Modifier.height(width * 0.040f))

            // ── the word ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val p = pillAnim.value
                        alpha = p
                        translationY = width.toPx() * 0.016f * (1f - p)
                    }
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF059669))
                    .padding(
                        horizontal = width * 0.046f,
                        vertical = width * 0.017f
                    )
            ) {
                Text(
                    text = word,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = with(density) { (width * 0.037f).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // ── the "it will close itself" line ────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = width * 0.05f)
                .width(width * 0.80f)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF059669))
                .graphicsLayer {
                    val p = progressAnim.value
                    alpha = (1f - exitAnim.value) * if (p <= 0.01f) 0f else 1f
                    scaleX = p
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stars
// ─────────────────────────────────────────────────────────────────────────────

/** dx / top / size / tilt / delay — all in fractions of the screen width. */
private data class BurstStar(
    val dx: Float,
    val top: Float,
    val size: Float,
    val rot: Float,
    val fromY: Float
)

private val BURST_STARS = listOf(
    BurstStar(dx = -0.33f, top = 0.11f, size = 0.11f, rot = -14f, fromY = 0.23f),
    BurstStar(dx = -0.17f, top = 0.05f, size = 0.14f, rot = -8f, fromY = 0.255f),
    BurstStar(dx = 0f, top = 0f, size = 0.19f, rot = 7f, fromY = 0.28f),
    BurstStar(dx = 0.17f, top = 0.05f, size = 0.14f, rot = 10f, fromY = 0.255f),
    BurstStar(dx = 0.33f, top = 0.11f, size = 0.11f, rot = 16f, fromY = 0.23f)
)

/** Warm gold, soft halo — drawn rather than an icon so it can glow and gradient. */
private fun DrawScope.drawStarGlow(
    center: Offset,
    radius: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFBBF24).copy(alpha = 0.38f),
                Color(0xFFF59E0B).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.7f
        ),
        radius = radius * 1.7f,
        center = center
    )
}

private fun DrawScope.drawStar(
    center: Offset,
    outer: Float
) {
    val inner = outer * 0.46f
    val path = Path()
    for (i in 0 until 10) {
        val angle = (-PI / 2 + i * PI / 5).toFloat()
        val r = if (i % 2 == 0) outer else inner
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFDE68A), Color(0xFFFBBF24), Color(0xFFF59E0B))
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Confetti
// ─────────────────────────────────────────────────────────────────────────────

private data class ConfettiPiece(
    val x: Float,
    val y0: Float,
    val vy: Float,
    val amp: Float,
    val freq: Float,
    val phase: Float,
    val rot: Float,
    val vrot: Float,
    val flip: Float,
    val type: Int,
    val size: Float,
    val color: Color,
    val start: Float
)

private val CONFETTI_COLORS = listOf(
    0xFFFBBF24, 0xFFFCD34D, 0xFF059669, 0xFF10B981,
    0xFF6EE7B7, 0xFFA7F3D0, 0xFFFFFFFF
).map { Color(it) }

/** How long confetti keeps ticking: the design's last piece lands well before this. */
private const val CONFETTI_SECONDS = 4.6f

private val CONFETTI = buildList {
    val random = kotlin.random.Random(20260301)
    repeat(84) {
        val side = if (random.nextBoolean()) -1f else 1f
        val spread = 0.1f + 0.4f * kotlin.math.sqrt(random.nextFloat().toDouble()).toFloat()
        val type = when (val r = random.nextFloat()) {
            in 0f..0.5f -> 0
            in 0.5f..0.8f -> 1
            else -> 2
        }
        add(
            ConfettiPiece(
                x = 0.5f + side * spread,
                y0 = -0.05f + random.nextFloat() * 0.30f,
                vy = 0.18f + random.nextFloat() * 0.36f,
                amp = 0.015f + random.nextFloat() * 0.041f,
                freq = 0.8f + random.nextFloat() * 1.6f,
                phase = random.nextFloat() * 6.28f,
                rot = random.nextFloat() * 6.28f,
                vrot = (random.nextFloat() - 0.5f) * 5f,
                flip = 2f + random.nextFloat() * 4f,
                type = type,
                size = 0.008f + random.nextFloat() * 0.010f,
                color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.size)],
                start = 0.15f + random.nextFloat() * 1.5f
            )
        )
    }
}

@Composable
private fun ConfettiLayer(alpha: Float) {
    var clock by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        // The piece from the design lives about four seconds; stop ticking then
        // rather than animating a screen nobody is looking at any more.
        while (clock < CONFETTI_SECONDS) {
            withFrameNanos { now ->
                clock = (now - startNanos) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (alpha <= 0.01f) return@Canvas
        val w = size.width
        val h = size.height

        CONFETTI.forEach { piece ->
            val t = clock - piece.start
            if (t <= 0f) return@forEach

            val x = piece.x * w + sin(piece.phase + t * piece.freq) * piece.amp * w
            val y = piece.y0 * h + t * piece.vy * h
            if (y > h * 1.15f) return@forEach

            val fade = (t / 0.3f).coerceIn(0f, 1f) * alpha
            val spin = piece.rot + t * piece.vrot * 180f / PI.toFloat()
            val side = piece.size * w

            rotate(degrees = spin, pivot = Offset(x, y)) {
                when (piece.type) {
                    0 -> {
                        // A paper strip; the height breathes so it looks flipped.
                        val hh = abs(cos(piece.phase + t * piece.flip)) * side * 0.76f + 0.6f
                        drawRect(
                            color = piece.color,
                            topLeft = Offset(x - side * 1.2f, y - hh / 2f),
                            size = Size(side * 2.4f, hh),
                            alpha = fade
                        )
                    }

                    1 -> drawCircle(
                        color = piece.color,
                        radius = side * 0.55f,
                        center = Offset(x, y),
                        alpha = fade
                    )

                    else -> {
                        // A four-point sparkle, the same shape the design draws.
                        val q = side * 1.5f
                        val path = Path().apply {
                            moveTo(x, y - q)
                            quadraticBezierTo(x, y, x + q, y)
                            quadraticBezierTo(x, y, x, y + q)
                            quadraticBezierTo(x, y, x - q, y)
                            quadraticBezierTo(x, y, x, y - q)
                            close()
                        }
                        drawPath(path = path, color = piece.color, alpha = fade)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Easings, named after the design file
// ─────────────────────────────────────────────────────────────────────────────

/** cubic-bezier(.22,1,.36,1) */
private val SETTLE = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** cubic-bezier(.34,1.56,.64,1) — the back-out overshoot of a star. */
private val BURST = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** ease-out */
private val EASE_OUT = CubicBezierEasing(0f, 0f, 0.58f, 1f)
