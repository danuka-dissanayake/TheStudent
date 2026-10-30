package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the orb is doing. Mirrors the six states of the reference orb
 * (idle / connecting / listening / thinking / speaking / error).
 */
enum class CloudOrbState { IDLE, CONNECTING, LISTENING, THINKING, SPEAKING, MUTED }

/** The prototype's `cubic-bezier(.215, .61, .355, 1)` — ease-out for the entrance. */
private val OrbEaseOut = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/**
 * The cloud orb.
 *
 * A port of the supplied WebGL orb: a 5-octave fBm field, domain-warped twice,
 * folded into a horizon band, tinted emerald and masked to a circle.
 *
 * It runs as a **real fragment shader** wherever the device can do it: from
 * Android 13 (API 33) `RuntimeShader` gives us per-pixel evaluation on the GPU,
 * which is both sharper than any bitmap and cheaper than the CPU loop — the orb
 * redraws at the display's own refresh rate instead of a 30 fps budget. On older
 * devices the identical maths runs in Kotlin into a small bitmap, which is then
 * drawn upscaled.
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

    // GPU first. A device that cannot compile the shader silently keeps the CPU
    // path, so this is never a crash, only a quality difference.
    val gpu = remember { createOrbGpu() }

    // ── CPU fallback surfaces ────────────────────────────────────────────────
    // Two reusable bitmaps, swapped each frame: the CPU writes into the back one
    // while the canvas is still drawing the front one, so nothing allocates and
    // no frame is ever torn.
    val buffers = remember {
        Array(2) {
            Bitmap.createBitmap(OrbClouds.RESOLUTION, OrbClouds.RESOLUTION, Bitmap.Config.ARGB_8888)
        }
    }
    val images = remember { List(2) { buffers[it].asImageBitmap() } }
    val front = remember { mutableStateOf<ImageBitmap?>(null) }
    val gpuFrame = remember { mutableIntStateOf(0) }

    val scale = remember { mutableFloatStateOf(1f) }
    var pressed by remember { mutableStateOf(false) }

    // ── The frame loop ───────────────────────────────────────────────────────
    // One loop, two drivers. On the GPU path we are called once per display
    // frame, so the flow is integrated against the real frame delta and the
    // shader redraws in step with the screen. On the CPU path we budget ~30 fps
    // and never allocate.
    LaunchedEffect(gpu) {
        var flow = 0f
        var elapsed = 0f
        var last = 0L
        var write = 0
        val pixels = IntArray(OrbClouds.RESOLUTION * OrbClouds.RESOLUTION)

        if (gpu != null) {
            while (isActive) {
                withFrameNanos { nanos ->
                    val dt = if (last == 0L) 0f else
                        ((nanos - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    last = nanos

                    val s = stateNow.value
                    val level = activityNow.value.coerceIn(0f, 1f)
                    flow += dt * speedFor(s, level)
                    elapsed += dt
                    gpu.setTime(flow, roughnessFor(s, level))
                    scale.floatValue += (scaleFor(s, level, elapsed) - scale.floatValue) * 0.22f
                    gpuFrame.intValue++
                }
            }
        } else {
            while (isActive) {
                val started = System.nanoTime()
                val s = stateNow.value
                val level = activityNow.value.coerceIn(0f, 1f)
                val now = System.nanoTime()
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now

                flow += dt * speedFor(s, level)
                elapsed += dt

                val back = buffers[write]
                val roughness = roughnessFor(s, level)
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

                scale.floatValue += (scaleFor(s, level, elapsed) - scale.floatValue) * 0.22f

                val spentMs = (System.nanoTime() - started) / 1_000_000L
                delay((34L - spentMs).coerceAtLeast(6L))
            }
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
                1.178f at 300 using OrbEaseOut
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
        val side = (sr * 2f).coerceAtLeast(1f)
        if (gpu != null) {
            // Read the frame counter so every shader update repaints this canvas,
            // then hand the shader the exact square it is about to cover.
            gpuFrame.intValue
            gpu.setFrame(c.x - sr, c.y - sr, side)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(
                    c.x - sr,
                    c.y - sr,
                    c.x + sr,
                    c.y + sr,
                    gpu.paint
                )
            }
        } else {
            val sphere = front.value
            val px = side.toInt().coerceAtLeast(1)
            if (sphere != null) {
                drawImage(
                    image = sphere,
                    dstOffset = IntOffset(
                        (c.x - sr).toInt(),
                        (c.y - sr).toInt()
                    ),
                    dstSize = IntSize(px, px),
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

/** Flow speed per state, from the reference motion presets. */
private fun speedFor(state: CloudOrbState, level: Float): Float = when (state) {
    CloudOrbState.LISTENING -> 0.72f + level * 0.78f
    CloudOrbState.SPEAKING -> 1.65f + level * 1.55f
    CloudOrbState.THINKING -> 0.24f
    else -> 0.30f
}

/** "Roughness" is the shader's `activity`: it widens the warp and the milk band. */
private fun roughnessFor(state: CloudOrbState, level: Float): Float = when (state) {
    CloudOrbState.LISTENING -> 0.28f + level * 0.32f
    CloudOrbState.SPEAKING -> 0.66f + level * 0.34f
    CloudOrbState.THINKING -> 0.18f
    else -> 0.12f
}

/** The ball leans in while listening and swells while speaking. */
private fun scaleFor(state: CloudOrbState, level: Float, elapsed: Float): Float = when (state) {
    CloudOrbState.LISTENING -> 1f - 0.13f * level
    CloudOrbState.SPEAKING -> 1f + 0.09f * level
    // Thinking barely moves, so a slow breath is what makes it read as
    // "working on it" rather than as a frozen sphere.
    CloudOrbState.THINKING -> 1f + 0.02f * sin(elapsed * 1.15f)
    else -> 1f
}

/**
 * Builds the GPU orb, or `null` where AGSL is unavailable.
 *
 * Kept outside [OrbGpu] on purpose: on an API 24 device the class itself must
 * never be resolved, and a `catch` around this call also swallows the
 * `NoClassDefFoundError` a strict verifier can raise for the missing type.
 */
private fun createOrbGpu(): OrbGpu? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return try {
        OrbGpu.create()
    } catch (t: Throwable) {
        null
    }
}

/**
 * The GPU orb: the reference shader, verbatim, compiled by AGSL.
 *
 * `RuntimeShader` exists from API 33. Every entry point returns `null` when the
 * device is older or when the driver refuses the program, and the caller then
 * uses [OrbClouds].
 */
private class OrbGpu private constructor(val shader: RuntimeShader, val paint: Paint) {

    fun setTime(time: Float, activity: Float) {
        shader.setFloatUniform("uTime", time)
        shader.setFloatUniform("uActivity", activity)
    }

    /** The square the canvas is about to fill, in canvas pixels. */
    fun setFrame(left: Float, top: Float, side: Float) {
        shader.setFloatUniform("uOrigin", left, top)
        shader.setFloatUniform("uSize", side, side)
    }

    companion object {
        fun create(): OrbGpu? {
            return try {
                val shader = RuntimeShader(SOURCE)
                val paint = Paint().apply {
                    isAntiAlias = true
                    this.shader = shader
                }
                OrbGpu(shader, paint)
            } catch (t: Throwable) {
                // A driver that cannot compile AGSL, or a device that reports 33
                // without a working runtime shader: fall back to the CPU orb.
                null
            }
        }

        /**
         * The fragment shader. Colours are the prototype's palette in 0..1:
         * #059669 deep, #10B981 upper, #A7F3D0 lower, #E6FBF1 milk. Output is
         * premultiplied, because that is what the canvas blends with.
         */
        private val SOURCE = """
            uniform float2 uOrigin;
            uniform float2 uSize;
            uniform float uTime;
            uniform float uActivity;

            float hash21(float2 p) {
                p = fract(p * float2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            float vnoise(float2 p) {
                float2 i = floor(p);
                float2 f = fract(p);
                float2 u = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + float2(1.0, 0.0));
                float c = hash21(i + float2(0.0, 1.0));
                float d = hash21(i + float2(1.0, 1.0));
                return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
            }

            float fbm(float2 p) {
                float v = 0.0;
                float amp = 0.52;
                for (int i = 0; i < 5; i++) {
                    v += amp * vnoise(p);
                    p = float2x2(0.80, 0.60, -0.60, 0.80) * p * 1.92 + float2(9.7, 4.3);
                    amp *= 0.5;
                }
                return v;
            }

            half4 main(float2 fragCoord) {
                float2 local = fragCoord - uOrigin;
                float2 uv = float2(local.x / uSize.x, 1.0 - local.y / uSize.y);
                float2 q = uv - 0.5;
                float radius = length(q);
                float edge = 1.0 - smoothstep(0.488, 0.5, radius);
                if (edge <= 0.0) {
                    return half4(0.0);
                }

                float2 p = q * 2.0;
                float t = uTime;
                float a = uActivity;

                float wx = fbm(p * 1.02 + float2(t * 0.34, -t * 0.24));
                float wy = fbm(p * 1.08 + float2(-t * 0.27 + 6.7, t * 0.32 + 2.9));
                float cx = sin(p.y * 2.4 + t * 0.68 + wy * 3.2);
                float cy = cos(p.x * 2.1 - t * 0.61 + wx * 3.0);

                float2 warped = p
                    + (float2(wx, wy) - 0.5) * (1.18 + a * 0.38)
                    + float2(cx, cy) * (0.035 + a * 0.07);

                float broad = fbm(warped * 0.92 + float2(t * 0.14, -t * 0.18));
                float folded = fbm(warped * 1.66 + float2(-t * 0.23 + 5.2, t * 0.19));
                float field = mix(broad, folded, 0.3 + a * 0.14);

                float horizon = 0.46
                    + 0.08 * sin((uv.x + wx * 0.2) * 5.4 + t * 0.42)
                    + 0.16 * (broad - 0.5);
                float upper = smoothstep(horizon - 0.12, horizon + 0.08, uv.y);
                float bd = (uv.y - horizon) * (5.2 + a * 0.8);
                float band = exp(-(bd * bd));
                float cloud = smoothstep(0.24, 0.79, field);

                float3 deep = float3(0.019608, 0.588235, 0.411765);
                float3 up = float3(0.062745, 0.725490, 0.505882);
                float3 low = float3(0.654902, 0.952941, 0.815686);
                float3 milk = float3(0.901961, 0.984314, 0.945098);

                float3 col = mix(low, up, upper);
                float upperDepth = upper * (0.14 + smoothstep(0.42, 0.78, folded) * 0.5);
                col = mix(col, deep, upperDepth);
                float milkAmount = clamp(band * (0.42 + cloud * 0.62), 0.0, 0.88);
                col = mix(col, milk, milkAmount);
                float lowerMist = (1.0 - upper) * smoothstep(0.58, 0.9, broad) * 0.18;
                col = mix(col, milk, lowerMist);

                float4 rgba = float4(col * edge, edge);
                return half4(rgba);
            }
        """.trimIndent()
    }
}

/**
 * The CPU fallback: the same cloud field for devices without `RuntimeShader`.
 *
 * Everything is a pure function of `flow` (time) and `activity`, so a frame can
 * be produced for any moment without keeping state.
 */
private object OrbClouds {

    /** Low on purpose: the field is smooth, and it upscales cleanly. */
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
                val band = kotlin.math.exp(-(bandDelta * bandDelta))
                val cloud = smoothStep(0.24f, 0.79f, field)

                // lower -> upper
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
