package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AnimatedVoiceButton(
    state: AssistantVoiceState,
    isHandsFree: Boolean,
    onMicClick: () -> Unit,
    onKeyboardToggle: () -> Unit,
    onHandsFreeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_waves")

    // Ripple 1 - State deferred to graphicsLayer to prevent recompositions
    val rippleScale1 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_1"
    )
    val rippleAlpha1 = infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_1"
    )

    // Ripple 2
    val rippleScale2 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_2"
    )
    val rippleAlpha2 = infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_2"
    )

    // Breathing pulse for idle
    val idleBreathe = infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breathe"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_voice_button_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primary Centralized Animated Voice Button
            Box(
                modifier = Modifier
                    .size(105.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding ripples when listening or speaking
                val continueButtonColor = Color(0xFF22C55E)

                if (state == AssistantVoiceState.LISTENING || state == AssistantVoiceState.SPEAKING) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                scaleX = rippleScale2.value
                                scaleY = rippleScale2.value
                                alpha = rippleAlpha2.value
                            }
                            .clip(CircleShape)
                            .background(
                                if (state == AssistantVoiceState.LISTENING)
                                    RedError
                                else
                                    continueButtonColor
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                scaleX = rippleScale1.value
                                scaleY = rippleScale1.value
                                alpha = rippleAlpha1.value
                            }
                            .clip(CircleShape)
                            .background(
                                if (state == AssistantVoiceState.LISTENING)
                                    RedError
                                else
                                    continueButtonColor
                            )
                    )
                }

                // Core Button Surface
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            val s = if (state == AssistantVoiceState.IDLE) idleBreathe.value else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMicClick
                        )
                        .testTag("primary_voice_mic_button"),
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (state == AssistantVoiceState.LISTENING)
                                    RedError
                                else
                                    continueButtonColor
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            AssistantVoiceState.LISTENING -> {
                                ModernPauseIcon(
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            AssistantVoiceState.THINKING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                            AssistantVoiceState.SPEAKING -> {
                                ModernAudioWaveIcon(
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            AssistantVoiceState.IDLE -> {
                                ModernMicIcon(
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtitle status
        Text(
            text = when (state) {
                AssistantVoiceState.IDLE -> if (isHandsFree) "Hands-Free Mode Active • Tap to Speak" else "Tap microphone to speak English"
                AssistantVoiceState.LISTENING -> "Listening to your voice..."
                AssistantVoiceState.THINKING -> "Thinking with TheStudent AI..."
                AssistantVoiceState.SPEAKING -> "Speaking • Tap to interrupt"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}
