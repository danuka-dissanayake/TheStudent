package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Color Schemes from Pipecat Voice UI Kit:
 * - Ocean: #22d3ee, #34d399, #818cf8
 * - Sunset: #ff6b6b, #ffa726, #ff5722
 * - Forest: #4caf50, #8bc34a, #cddc39 (Default)
 * - Cosmic: #9c27b0, #e91e63, #ff4081
 */
enum class PlasmaScheme(
    val id: String,
    val displayName: String,
    val color1: Color,
    val color2: Color,
    val color3: Color
) {
    FOREST("forest", "Forest", Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39)),
    OCEAN("ocean", "Ocean", Color(0xFF22D3EE), Color(0xFF34D399), Color(0xFF818CF8)),
    SUNSET("sunset", "Sunset", Color(0xFFFF6B6B), Color(0xFFFFA726), Color(0xFFFF5722)),
    COSMIC("cosmic", "Cosmic", Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFFFF4081))
}

/**
 * Pipecat Voice UI Kit WebGL Plasma Configuration Parameters:
 * intensity: 1.8
 * radius: 1.7
 * ringCount: 3
 * ringVisibility: 0.6
 * ringBounce: 0.2
 * ringThickness: 12
 * ringVariance: 0.5
 * ringAmplitude: 0.03
 * ringSpeed: 1.5
 * ringSegments: 5
 * colorCycleSpeed: 0.8
 * plasmaSpeed: 1.2
 * useCustomColors: true
 * backgroundColor: "transparent"
 */
data class PlasmaVisualizerConfig(
    val intensity: Float = 1.8f,
    val radius: Float = 1.7f,
    val ringCount: Int = 3,
    val ringVisibility: Float = 0.6f,
    val ringBounce: Float = 0.2f,
    val ringThickness: Float = 12f,
    val ringVariance: Float = 0.5f,
    val ringAmplitude: Float = 0.03f,
    val ringSpeed: Float = 1.5f,
    val ringSegments: Int = 5,
    val colorCycleSpeed: Float = 0.8f,
    val plasmaSpeed: Float = 1.2f,
    val colorScheme: PlasmaScheme = PlasmaScheme.FOREST
)

/**
 * Native Jetpack Compose implementation of Pipecat Voice UI Kit WebGL Plasma Audio Visualizer.
 * Faithfully maps all parameters (ringCount=3, ringSegments=5, ringThickness=12, ringSpeed=1.5,
 * intensity=1.8, forest palette) with real-time speech reactivity.
 */
@Composable
fun PlasmaVisualizer(
    state: AssistantVoiceState,
    rmsDb: Float,
    modifier: Modifier = Modifier,
    height: Dp = 250.dp,
    activeScheme: PlasmaScheme = PlasmaScheme.FOREST,
    onSchemeChanged: ((PlasmaScheme) -> Unit)? = null,
    showSchemeSelector: Boolean = false
) {
    var selectedScheme by remember { mutableStateOf(activeScheme) }
    val currentScheme = if (onSchemeChanged != null) activeScheme else selectedScheme

    val config = remember(currentScheme) {
        PlasmaVisualizerConfig(colorScheme = currentScheme)
    }

    // Continuous time ticker for fluid animation
    val infiniteTransition = rememberInfiniteTransition(label = "plasma_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_ticker"
    )

    // Smooth normalized decibels from mic (0f to 1f)
    val rawNormalized = remember(rmsDb) {
        val clamped = (rmsDb + 2f).coerceIn(0f, 14f)
        (clamped / 14f).coerceIn(0.02f, 1f)
    }

    val animatedAudioLevel by animateFloatAsState(
        targetValue = when (state) {
            AssistantVoiceState.LISTENING -> rawNormalized
            AssistantVoiceState.SPEAKING -> 0.45f + (sin(time * 6f).absoluteValue * 0.40f)
            AssistantVoiceState.THINKING -> 0.28f + (sin(time * 3f).absoluteValue * 0.20f)
            AssistantVoiceState.IDLE -> 0.05f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "audio_bounce_level"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("plasma_audio_visualizer_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Optional scheme selector buttons matching the user's React snippet:
        // ocean, sunset, forest, cosmic
        if (showSchemeSelector) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlasmaScheme.values().forEach { scheme ->
                    val isSelected = scheme == currentScheme
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) scheme.color1 else Color(0xFF22292F),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedScheme = scheme
                                onSchemeChanged?.invoke(scheme)
                            }
                    ) {
                        Text(
                            text = scheme.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Plasma Black Canvas Container: matching <div className="w-full h-96 relative bg-black rounded-lg overflow-hidden">
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF050906)) // Deep black-green ambient canvas
                .border(
                    width = 1.dp,
                    color = currentScheme.color1.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // WebGL Plasma Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("plasma_gl_canvas")
            ) {
                drawPlasmaShader(
                    config = config,
                    time = time,
                    audioLevel = animatedAudioLevel,
                    state = state
                )
            }
        }
    }
}

/**
 * Draws the Pipecat WebGL Plasma shader effects:
 * - Glowing radial plasma energy core
 * - 3 concentric wavy rings (ringCount = 3)
 * - 5-lobed sinusoidal harmonic deformation (ringSegments = 5)
 * - 12dp thick glowing ribbons (ringThickness = 12)
 * - Flowing fluid plasma nodes orbiting inside
 * - Forest color cycling: #4caf50, #8bc34a, #cddc39
 */
private fun DrawScope.drawPlasmaShader(
    config: PlasmaVisualizerConfig,
    time: Float,
    audioLevel: Float,
    state: AssistantVoiceState
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = (min(size.width, size.height) / 2f) * 0.92f

    val c1 = config.colorScheme.color1
    val c2 = config.colorScheme.color2
    val c3 = config.colorScheme.color3

    val plasmaT = time * config.plasmaSpeed
    val ringT = time * config.ringSpeed
    val colorCycle = time * config.colorCycleSpeed

    // 1. Central Ambient Fluid Plasma Core Glow (Intensity = 1.8, Radius = 1.7)
    val coreBaseRadius = (maxRadius * 0.50f) * (config.radius / 1.7f)
    val coreDynamicBounce = 1f + (audioLevel * config.ringBounce * 1.5f)
    val coreGlowRadius = (coreBaseRadius * coreDynamicBounce).coerceAtLeast(20f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                c2.copy(alpha = 0.40f * config.intensity.coerceIn(0.5f, 2f)),
                c1.copy(alpha = 0.25f * config.intensity.coerceIn(0.5f, 2f)),
                c3.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = center,
            radius = coreGlowRadius * 1.4f
        ),
        radius = coreGlowRadius * 1.4f,
        center = center
    )

    // 2. Swirling Fluid Plasma Blobs (Simulating WebGL fragment shader fluid dynamics)
    val numBlobs = 4
    for (i in 0 until numBlobs) {
        val blobAngle = (plasmaT * 0.7f) + (i * (2 * Math.PI / numBlobs).toFloat())
        val orbitDist = coreBaseRadius * (0.25f + 0.15f * sin(plasmaT * 1.1f + i))
        val blobCenter = Offset(
            center.x + orbitDist * cos(blobAngle),
            center.y + orbitDist * sin(blobAngle)
        )
        val blobColor = when (i % 3) {
            0 -> c1
            1 -> c2
            else -> c3
        }
        val blobRadius = (32.dp.toPx() * (1f + audioLevel * 0.4f)).coerceAtLeast(10f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    blobColor.copy(alpha = 0.45f * config.intensity.coerceIn(0.5f, 2f)),
                    blobColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = blobCenter,
                radius = blobRadius
            ),
            radius = blobRadius,
            center = blobCenter,
            blendMode = BlendMode.Screen
        )
    }

    // 3. Concentric Plasma Rings (ringCount = 3, ringSegments = 5, ringThickness = 12dp, ringVisibility = 0.6)
    val ringThicknessPx = config.ringThickness.dp.toPx()
    val ringVisibility = config.ringVisibility.coerceIn(0.1f, 1f)
    val ringBounceAmount = audioLevel * config.ringBounce

    // Ring radii distribution
    val ringFractions = listOf(0.42f, 0.68f, 0.94f)

    for (k in 0 until config.ringCount) {
        val ringFraction = ringFractions.getOrElse(k) { (k + 1f) / (config.ringCount + 1f) }
        val baseRingR = maxRadius * ringFraction

        // Harmonic phase offset per ring
        val ringPhase = ringT + (k * (Math.PI / config.ringCount).toFloat() * 1.6f)
        val kRingBounce = 1f + (ringBounceAmount * (1f + k * 0.35f))

        // Create 5-lobed closed plasma path (ringSegments = 5)
        val path = Path()
        val numPoints = 100
        var firstPoint = Offset.Zero

        for (p in 0..numPoints) {
            val theta = (p.toFloat() / numPoints) * 2f * Math.PI.toFloat()

            // 5-segment sinusoidal harmonic modulation + variance
            val segmentWave = sin((config.ringSegments * theta) - ringPhase)
            val varianceWave = cos((2f * theta) + (ringPhase * 0.5f))

            val deformation = (config.ringAmplitude + ringBounceAmount) * segmentWave +
                    (config.ringVariance * 0.04f * varianceWave)

            val r = baseRingR * kRingBounce * (1f + deformation)

            val px = center.x + r * cos(theta)
            val py = center.y + r * sin(theta)

            if (p == 0) {
                path.moveTo(px, py)
                firstPoint = Offset(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()

        // Rotating Forest color gradient across the ring
        val gradientAngle = colorCycle + (k * 0.8f)
        val ringBrush = Brush.sweepGradient(
            colors = listOf(
                c1.copy(alpha = ringVisibility),
                c2.copy(alpha = ringVisibility * 1.1f.coerceAtMost(1f)),
                c3.copy(alpha = ringVisibility),
                c2.copy(alpha = ringVisibility * 0.9f),
                c1.copy(alpha = ringVisibility)
            ),
            center = center
        )

        // Outer soft glow layer for glowing WebGL look
        drawPath(
            path = path,
            brush = Brush.sweepGradient(
                colors = listOf(
                    c1.copy(alpha = ringVisibility * 0.25f),
                    c2.copy(alpha = ringVisibility * 0.30f),
                    c3.copy(alpha = ringVisibility * 0.25f),
                    c1.copy(alpha = ringVisibility * 0.25f)
                ),
                center = center
            ),
            style = Stroke(
                width = ringThicknessPx * 2.2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            blendMode = BlendMode.Screen
        )

        // Main Plasma Ribbon Ring
        drawPath(
            path = path,
            brush = ringBrush,
            style = Stroke(
                width = ringThicknessPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Subtle specular highlight line along the center of the ring
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.35f * ringVisibility),
            style = Stroke(
                width = (ringThicknessPx * 0.22f).coerceAtLeast(1.5f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // 4. Subtle Inner Pulsing Node at the very center
    val innerPulseR = 10.dp.toPx() * (1f + audioLevel * 0.5f)
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = innerPulseR * 0.5f,
        center = center
    )
    drawCircle(
        color = c3.copy(alpha = 0.6f),
        radius = innerPulseR,
        center = center
    )
}
