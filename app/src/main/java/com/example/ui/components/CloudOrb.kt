package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.CubicOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the orb is doing. Mirrors the six states of the reference orb
 * (idle / connecting / listening / thinking / speaking / error).
 */
enum class CloudOrbState { IDLE, CONNECTING, LISTENING, THINKING, SPEAKING, MUTED }

/**
 * The cloud orb.
 *
 * This is a port of the supplied WebGL orb: a 5-octave fBm field, domain-warped
 * twice, folded into a horizon band, tinted emerald and masked to a circle. The
 * shader cannot run on a Compose canvas (AGSL needs API 33 and this app ships to
 * 24), so the same maths runs on the CPU into a small bitmap which is then drawn
 * scaled up. The field is smooth by construction, so a 96 px texture is enough:
 * upscaling it softens the edges the way the shader's own antialiasing does.
 *
 * Motion comes straight from the reference's model: [CloudOrbState.LISTENING]
 * shrinks the orb and speeds the flow with your voice, SPEAKING grows it, and
 * THINKING keeps a slow drift so the surface never freezes mid-reply.
 */
@Composable
fun CloudOrb(
    state: CloudOrbState,
    activity: Float,
    size: Dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val stateNow = rememberUpdatedState(state)
    val activityNow = rememberUpdatedState(activity)

    // Two reusable surfaces, swapped each frame. The CPU writes into the back
    // one while the canvas is still drawing the front one, so a ten-minute call
    // both allocates nothing per frame and never tears a frame on screen.
    val buffers = remember {
        Array(2) {
            Bitmap.createBitmap(OrbClouds.RESOLUTION, OrbClouds.RESOLUTION, Bitmap.Config.ARGB_8888)
        }
    }
    val images = remember { List(2) { buffers[it].asImageBitmap() } }
    val front = remember { mutableStateOf<ImageBitmap?>(null) }
    val scale = remember { mutableFloatStateOf(1f) }
    var pressed by remember { mutableStateOf(false) }

    // One render loop for the whole component. Everything it produces is read
    // inside the draw block, so the screen around it is never recomposed at frame
    // rate — only this canvas redraws.
    LaunchedEffect(Unit) {
        var flow = 0f
        var elapsed = 0f
        var last = 0L
        var write = 0
        val pixels = IntArray(OrbClouds.RESOLUTION * OrbClouds.RESOLUTION)
        while (isActive) {
            val started = System.nanoTime()
            val s = stateNow.value
            val level = activityNow.value.coerceIn(0f, 1f)
            val now = System.nanoTime()
            val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now

            // Speed and roughness per state, from the reference motion presets.
            val speed = when (s) {
                CloudOrbState.LISTENING -> 0.72f + level * 0.78f
                CloudOrbState.SPEAKING -> 1.65f + level * 1.55f
                CloudOrbState.THINKING -> 0.24f
                else -> 0.30f
            }
            val roughness = when (s) {
                CloudOrbState.LISTENING -> 0.28f + level * 0.32f
                CloudOrbState.SPEAKING -> 0.66f + level * 0.34f
                CloudOrbState.THINKING -> 0.18f
                else -> 0.12f
            }
            flow += dt * speed
            elapsed += dt

            val back = buffers[write]
            withContext(Dispatchers.Default) {
                OrbClouds.render(pixels, flow, roughness)
                back.setPixels(
                    pixels,
                    0,
                    OrbClouds.RESOLUTION,
                    0,
                    0,
                    OrbClouds.RESOLUTION,
                    OrbClouds.RESOLUTION
                )
            }
            front.value = images[write]
            write = 1 - write

            val target = when (s) {
                CloudOrbState.LISTENING -> 1f - 0.13f * level
                CloudOrbState.SPEAKING -> 1f + 0.09f * level
                // Thinking barely moves, so a slow breath is what makes it read
                // as "working on it" rather than as a frozen sphere.
                CloudOrbState.THINKING -> 1f + 0.02f * sin(elapsed * 1.15f)
                else -> 1f
            }
            scale.floatValue += (target - scale.floatValue) * 0.22f

            // Aim for ~30 fps: the clouds are slow, and this keeps a mid-range
            // phone cool during a ten-minute call.
            val spentMs = (System.nanoTime() - started) / 1_000_000L
            delay((34L - spentMs).coerceAtLeast(6L))
        }
    }

    // The reference's entrance: the ball starts held small, overshoots, then
    // settles back to rest. It is the one moment the orb is allowed to be loud.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180)
        appeared = true
    }
    val entrance = animateFloatAsState(
        targetValue = if (appeared) 1f else 0.63f,
        animationSpec = if (appeared) {
            keyframes<Float> {
                durationMillis = 1_350
                1.178f at 300 using CubicOut
                1f at 1_350
            }
        } else {
            tween<Float>(durationMillis = 0)
        },
        label = "orb_entrance"
    )

    val pressedScale = if (pressed) 0.94f else 1f
    val spinner = rememberInfiniteTransition(label = "orb_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100, easing = LinearEasing)
        ),
        label = "orb_spin_angle"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val grow = entrance.value * scale.floatValue * pressedScale
                scaleX = grow
                scaleY = grow
            }
            .pointerInput(onTap) {
                val tap = onTap ?: return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { tap() }
                )
            }
    ) {
        val r = this.size.minDimension / 2f
        val sr = r * 0.86f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)

        // ── shadow ────────────────────────────────────────────────────────────
        // The cloud orb is pale by design, so it needs a real shadow to sit on a
        // pale page instead of dissolving into it.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF123A30).copy(alpha = 0.30f),
                    0.55f to Color(0xFF123A30).copy(alpha = 0.16f),
                    1.00f to Color.Transparent
                ),
                center = Offset(c.x, c.y + sr * 0.30f),
                radius = r * 1.02f
            ),
            radius = r * 1.02f,
            center = Offset(c.x, c.y + sr * 0.30f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF0F2E26).copy(alpha = 0.34f),
                    0.70f to Color(0xFF0F2E26).copy(alpha = 0.18f),
                    1.00f to Color.Transparent
                ),
                center = Offset(c.x, c.y + sr * 0.42f),
                radius = sr * 0.95f
            ),
            radius = sr * 0.95f,
            center = Offset(c.x, c.y + sr * 0.42f)
        )

        // ── brand bloom ───────────────────────────────────────────────────────
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF059669).copy(alpha = 0.18f),
                    0.60f to Color(0xFF059669).copy(alpha = 0.06f),
                    1.00f to Color.Transparent
                ),
                center = c,
                radius = r * 0.98f
            ),
            radius = r * 0.98f,
            center = c
        )

        // ── the sphere ────────────────────────────────────────────────────────
        val sphere = front.value
        val side = (sr * 2f).toInt().coerceAtLeast(1)
        val dstSize = IntSize(side, side)
        if (sphere != null) {
            drawImage(
                image = sphere,
                dstOffset = IntOffset(
                    (c.x - sr).toInt(),
                    (c.y - sr).toInt()
                ),
                dstSize = dstSize,
                filterQuality = FilterQuality.High
            )
        } else {
            // First frame, before the renderer has produced anything: show the
            // right colour rather than a hole.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE6FBF1), Color(0xFF10B981), Color(0xFF059669)),
                    center = Offset(c.x - sr * 0.3f, c.y - sr * 0.3f),
                    radius = sr * 1.4f
                ),
                radius = sr,
                center = c
            )
        }

        // ── muted: dim the whole sphere, the way the reference greys a failed
        //    or suspended session, so "off" is obvious at a glance ───────────
        if (state == CloudOrbState.MUTED) {
            drawCircle(
                color = Color(0x8CF2FBF7),
                radius = sr,
                center = c
            )
        }

        // ── connecting: the reference's small ring in the middle ──────────────
        if (state == CloudOrbState.CONNECTING) {
            val arcRadius = sr * 0.16f
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.24f),
                radius = arcRadius,
                center = c,
                style = Stroke(width = sr * 0.035f)
            )
            val sweep = 110f
            drawArc(
                color = Color(0xFF10B981),
                startAngle = spinner.value,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(c.x - arcRadius, c.y - arcRadius),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = sr * 0.035f)
            )
        }
    }
}

/**
 * The cloud field itself — a direct port of the reference fragment shader.
 *
 * Everything is a pure function of `flow` (time) and `activity`, so a frame can
 * be produced for any moment without keeping state.
 */
private object OrbClouds {

    /** Low on purpose: the field is smooth, and 96 px upscales cleanly. */
    const val RESOLUTION = 96

    // Palette of the reference: deep emerald, emerald, pale mint, milk.
    private val DEEP_R = 0x05 / 255f
    private val DEEP_G = 0x96 / 255f
    private val DEEP_B = 0x69 / 255f
    private val UPPER_R = 0x10 / 255f
    private val UPPER_G = 0xB9 / 255f
    private val UPPER_B = 0x81 / 255f
    private val LOWER_R = 0xA7 / 255f
    private val LOWER_G = 0xF3 / 255f
    private val LOWER_B = 0xD0 / 255f
    private val MILK_R = 0xE6 / 255f
    private val MILK_G = 0xFB / 255f
    private val MILK_B = 0xF1 / 255f

    fun render(out: IntArray, time: Float, activity: Float) {
        val res = RESOLUTION
        val inv = 1f / res
        var index = 0
        var y = 0
        while (y < res) {
            // The shader works in GL space (y up); bitmaps run y down.
            val uvY = 1f - (y + 0.5f) * inv
            var x = 0
            while (x < res) {
                val uvX = (x + 0.5f) * inv
                val px = uvX - 0.5f
                val py = uvY - 0.5f
                val radius = sqrt(px * px + py * py)
                if (radius >= 0.5f) {
                    out[index] = 0
                    index++
                    x++
                    continue
                }
                // soft edge, same 0.488 -> 0.5 ramp as the shader's discard
                val edge = 1f - smoothStep(0.488f, 0.5f, radius)

                val pX = px * 2f
                val pY = py * 2f

                val warpX = fbm(pX * 1.02f + time * 0.34f, pY * 1.02f - time * 0.24f)
                val warpY = fbm(pX * 1.08f - time * 0.27f + 6.7f, pY * 1.08f + time * 0.32f + 2.9f)

                val curlX = sin(pY * 2.4f + time * 0.68f + warpY * 3.2f)
                val curlY = cos(pX * 2.1f - time * 0.61f + warpX * 3.0f)

                val warpAmp = 1.18f + activity * 0.38f
                val curlAmp = 0.035f + activity * 0.07f
                val wX = pX + (warpX - 0.5f) * warpAmp + curlX * curlAmp
                val wY = pY + (warpY - 0.5f) * warpAmp + curlY * curlAmp

                val broad = fbm(wX * 0.92f + time * 0.14f, wY * 0.92f - time * 0.18f)
                val folded = fbm(wX * 1.66f - time * 0.23f + 5.2f, wY * 1.66f + time * 0.19f)
                val field = broad + (folded - broad) * (0.3f + activity * 0.14f)

                val horizon = 0.46f +
                    0.08f * sin((uvX + warpX * 0.2f) * 5.4f + time * 0.42f) +
                    0.16f * (broad - 0.5f)
                val upper = smoothStep(horizon - 0.12f, horizon + 0.08f, uvY)
                val bandDelta = (uvY - horizon) * (5.2f + activity * 0.8f)
                val band = exp(-(bandDelta * bandDelta))
                val cloud = smoothStep(0.24f, 0.79f, field)

                // lowerLavender -> upperPeriwinkle
                var r = LOWER_R + (UPPER_R - LOWER_R) * upper
                var g = LOWER_G + (UPPER_G - LOWER_G) * upper
                var b = LOWER_B + (UPPER_B - LOWER_B) * upper

                val upperDepth = upper * (0.14f + smoothStep(0.42f, 0.78f, folded) * 0.5f)
                r += (DEEP_R - r) * upperDepth
                g += (DEEP_G - g) * upperDepth
                b += (DEEP_B - b) * upperDepth

                val milkAmount = (band * (0.42f + cloud * 0.62f)).coerceIn(0f, 0.88f)
                r += (MILK_R - r) * milkAmount
                g += (MILK_G - g) * milkAmount
                b += (MILK_B - b) * milkAmount

                val lowerMist = (1f - upper) * smoothStep(0.58f, 0.9f, broad) * 0.18f
                r += (MILK_R - r) * lowerMist
                g += (MILK_G - g) * lowerMist
                b += (MILK_B - b) * lowerMist

                out[index] = argb(r, g, b, edge)
                index++
                x++
            }
            y++
        }
    }

    private fun argb(r: Float, g: Float, b: Float, a: Float): Int {
        val ri = (r.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        val gi = (g.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        val bi = (b.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        val ai = (a.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        if (edge1 <= edge0) return if (value < edge0) 0f else 1f
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Fractal noise, 5 octaves, rotated each step — the shader's fbm. */
    private fun fbm(startX: Float, startY: Float): Float {
        var px = startX
        var py = startY
        var value = 0f
        var amplitude = 0.52f
        var octave = 0
        while (octave < 5) {
            value += amplitude * noise(px, py)
            val nx = 0.80f * px - 0.60f * py
            val ny = 0.60f * px + 0.80f * py
            px = nx * 1.92f + 9.7f
            py = ny * 1.92f + 4.3f
            amplitude *= 0.5f
            octave++
        }
        return value
    }

    /** Value noise with the shader's smoothstep interpolation. */
    private fun noise(px: Float, py: Float): Float {
        val ix = floor(px)
        val iy = floor(py)
        val fx = px - ix
        val fy = py - iy
        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)

        val a = hash(ix, iy)
        val b = hash(ix + 1f, iy)
        val c = hash(ix, iy + 1f)
        val d = hash(ix + 1f, iy + 1f)

        val ab = a + (b - a) * ux
        val cd = c + (d - c) * ux
        return ab + (cd - ab) * uy
    }

    private fun hash(xIn: Float, yIn: Float): Float {
        var x = fract(xIn * 123.34f)
        var y = fract(yIn * 456.21f)
        val dot = x * (x + 45.32f) + y * (y + 45.32f)
        x += dot
        y += dot
        return fract(x * y)
    }

    private fun fract(v: Float): Float = v - floor(v)
}
