package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.*

enum class AssistantVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

/**
 * Modern Audio Visualizer inspired by LiveKit Agents UI (AgentAudioVisualizerBar / BarVisualizer).
 *
 * Implements:
 * - Dynamic multi-band equalizer bars with capsule ends.
 * - Reactive audio amplitude scaling based on live mic RMS decibels.
 * - LiveKit's signature traveling wave during THINKING state.
 * - Harmonic multi-frequency equalizer pulse during SPEAKING state.
 * - Gentle ambient breathing during IDLE state.
 * - Ambient radial aura glow adhering to the Green / Emerald palette.
 */
@Composable
fun CentralizedAudioVisualizer(
    state: AssistantVoiceState,
    rmsDb: Float, // Decibels from mic (typically -2 to 14+)
    modifier: Modifier = Modifier,
    barCount: Int = 15, // LiveKit standard 11-17 bars
    height: Dp = 130.dp,
    barWidth: Dp = 7.dp,
    barGap: Dp = 6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "livekit_visualizer_anim")

    // Continuous time animation for smooth continuous motion
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat() * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_time"
    )

    // Idle breathing oscillation
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_breathing"
    )

    // Thinking ripple speed phase
    val thinkingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_phase"
    )

    // Speaking energetic equalizer phase
    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speaking_phase"
    )

    // Normalized RMS decibel volume (0f to 1f) with smooth spring transition
    val rawNormalizedRms = remember(rmsDb) {
        val clamped = (rmsDb + 2f).coerceIn(0f, 14f)
        (clamped / 14f).coerceIn(0.04f, 1f)
    }

    val smoothRms by animateFloatAsState(
        targetValue = if (state == AssistantVoiceState.LISTENING) rawNormalizedRms else 0.08f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "smooth_rms"
    )

    // Green color palette tokens
    val barPrimary = EmeraldPrimary
    val barLight = EmeraldLight
    val barAccent = LimeAccent
    val barTeal = TealLight
    val glowColor = EmeraldLight

    val totalWidth = (barWidth + barGap) * barCount - barGap

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 40.dp)
            .testTag("central_audio_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft aura glow behind the bars
        Canvas(
            modifier = Modifier
                .size(totalWidth + 80.dp, height + 30.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val auraAlpha = when (state) {
                AssistantVoiceState.IDLE -> 0.08f + sin(idleBreathing).absoluteValue * 0.04f
                AssistantVoiceState.LISTENING -> 0.16f + smoothRms * 0.22f
                AssistantVoiceState.THINKING -> 0.18f + sin(thinkingPhase).absoluteValue * 0.12f
                AssistantVoiceState.SPEAKING -> 0.25f + sin(speakingPhase).absoluteValue * 0.15f
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = auraAlpha),
                        barPrimary.copy(alpha = auraAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (size.width / 2f).coerceAtLeast(80.dp.toPx())
                ),
                radius = (size.width / 2f).coerceAtLeast(80.dp.toPx()),
                center = center
            )
        }

        // LiveKit Vertical Equalizer Bars Canvas
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(height)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            val pxBarWidth = barWidth.toPx()
            val pxBarGap = barGap.toPx()
            val minBarHeight = 10.dp.toPx()
            val maxBarHeight = canvasHeight * 0.88f

            val midIndex = (barCount - 1) / 2f

            for (i in 0 until barCount) {
                val x = i * (pxBarWidth + pxBarGap)
                // Distance from center (0 at center, 1 at edges)
                val distFromCenter = (abs(i - midIndex) / midIndex).coerceIn(0f, 1f)
                val centerWeight = 1f - (distFromCenter * 0.35f) // Center bars slightly taller

                val barH: Float = when (state) {
                    AssistantVoiceState.IDLE -> {
                        // LiveKit Idle: Soft gentle breathing wave across the bars
                        val wave = sin(idleBreathing + i * 0.45f) * 0.5f + 0.5f
                        val base = minBarHeight + (12.dp.toPx() * wave * centerWeight)
                        base.coerceIn(minBarHeight, canvasHeight * 0.35f)
                    }

                    AssistantVoiceState.LISTENING -> {
                        // LiveKit Listening: Bars dance to user's incoming microphone audio
                        // Multi-band frequency simulation: each bar has unique harmonic response
                        val harmonic1 = sin(animTime * 4f + i * 0.7f).absoluteValue
                        val harmonic2 = cos(animTime * 2.5f - i * 0.5f).absoluteValue
                        val jitter = (harmonic1 * 0.6f + harmonic2 * 0.4f)
                        val dynamicFactor = (smoothRms * 1.2f * jitter * centerWeight).coerceIn(0.05f, 1f)
                        val targetHeight = minBarHeight + (maxBarHeight - minBarHeight) * dynamicFactor
                        targetHeight.coerceIn(minBarHeight, maxBarHeight)
                    }

                    AssistantVoiceState.THINKING -> {
                        // LiveKit Thinking: Smooth signature traveling sine wave across the bars
                        // Creates that iconic rippling wave moving left to right
                        val wave = sin(thinkingPhase * 2.2f - (i * 0.55f))
                        val normalizedWave = (wave + 1f) / 2f // 0f to 1f
                        val targetHeight = minBarHeight + (maxBarHeight * 0.65f * normalizedWave)
                        targetHeight.coerceIn(minBarHeight, maxBarHeight * 0.75f)
                    }

                    AssistantVoiceState.SPEAKING -> {
                        // LiveKit Speaking: Full multi-band audio equalizer frequency motion
                        // High energy, responsive multi-frequency superposition
                        val f1 = sin(speakingPhase * 3.2f + i * 0.8f).absoluteValue
                        val f2 = cos(speakingPhase * 5.1f - i * 1.1f).absoluteValue
                        val f3 = sin(speakingPhase * 1.8f + i * 0.4f).absoluteValue
                        val composite = (f1 * 0.45f + f2 * 0.35f + f3 * 0.20f) * centerWeight
                        val speechEnergy = 0.25f + composite * 0.75f
                        val targetHeight = minBarHeight + (maxBarHeight - minBarHeight) * speechEnergy
                        targetHeight.coerceIn(minBarHeight, maxBarHeight)
                    }
                }

                val top = centerY - (barH / 2f)

                // LiveKit Gradient styling:
                // Emerald light to emerald primary with lime accent highlights when high amplitude
                val isPeak = barH > (maxBarHeight * 0.65f)
                val topColor = when {
                    state == AssistantVoiceState.THINKING -> {
                        val waveHighlight = sin(thinkingPhase * 2.2f - (i * 0.55f))
                        if (waveHighlight > 0.5f) barAccent else barLight
                    }
                    isPeak -> barAccent
                    state == AssistantVoiceState.SPEAKING -> barLight
                    else -> barLight
                }
                val bottomColor = when (state) {
                    AssistantVoiceState.THINKING -> barTeal
                    else -> barPrimary
                }

                val barBrush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = top,
                    endY = top + barH
                )

                // Draw rounded capsule bar
                val cornerRadius = CornerRadius(pxBarWidth / 2f, pxBarWidth / 2f)
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x, top),
                    size = Size(pxBarWidth, barH),
                    cornerRadius = cornerRadius
                )

                // Optional subtle top highlight cap dot for polished LiveKit look
                if (barH > minBarHeight + 6.dp.toPx()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = pxBarWidth * 0.28f,
                        center = Offset(x + pxBarWidth / 2f, top + pxBarWidth / 2f)
                    )
                }
            }
        }
    }
}

/**
 * Minimal status indicator below visualizer matching modern clean AI voice interfaces.
 */
@Composable
fun MinimalVisualizerStatus(
    state: AssistantVoiceState,
    modifier: Modifier = Modifier
) {
    val statusText = when (state) {
        AssistantVoiceState.IDLE -> "Ready to practice English"
        AssistantVoiceState.LISTENING -> "Listening..."
        AssistantVoiceState.THINKING -> "Thinking with TheStudent AI..."
        AssistantVoiceState.SPEAKING -> "AI Speaking..."
    }

    val indicatorColor = when (state) {
        AssistantVoiceState.IDLE -> EmeraldLight.copy(alpha = 0.7f)
        AssistantVoiceState.LISTENING -> RedError
        AssistantVoiceState.THINKING -> AmberWarning
        AssistantVoiceState.SPEAKING -> EmeraldLight
    }

    Row(
        modifier = modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            modifier = Modifier.size(7.dp),
            shape = CircleShape,
            color = indicatorColor
        ) {}

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp,
            letterSpacing = 0.3.sp
        )
    }
}
