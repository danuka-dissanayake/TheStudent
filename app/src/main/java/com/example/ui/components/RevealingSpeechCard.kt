package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Minimalist transcript view for voice AI assistant.
 * Strips away unwanted card borders, heavy badges, and repetitive headers,
 * presenting clean typography with smooth character reveal and understated controls.
 */
@Composable
fun RevealingSpeechCard(
    userSpokenText: String?,
    aiResponseText: String,
    grammarTip: String?,
    vocabularyHighlight: String?,
    isSpeaking: Boolean,
    onReplayAudio: () -> Unit = {},
    onStopAudio: () -> Unit = {},
    onCopyText: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Typewriter effect state
    var displayedLength by remember(aiResponseText) { mutableIntStateOf(0) }
    var isRevealingComplete by remember(aiResponseText) { mutableStateOf(false) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var wasSpeaking by remember { mutableStateOf(false) }

    LaunchedEffect(isSpeaking) {
        // If replay is pressed while text was already finished, re-trigger typing synced with audio
        if (isSpeaking && !wasSpeaking && isRevealingComplete) {
            replayTrigger++
        }
        // When audio finishes or stops, immediately complete any remaining text
        if (!isSpeaking && displayedLength > 0 && displayedLength < aiResponseText.length) {
            displayedLength = aiResponseText.length
            isRevealingComplete = true
        }
        wasSpeaking = isSpeaking
    }

    LaunchedEffect(aiResponseText, replayTrigger) {
        if (aiResponseText.isBlank()) {
            displayedLength = 0
            isRevealingComplete = false
            return@LaunchedEffect
        }

        displayedLength = 0
        isRevealingComplete = false

        // 1. Wait until Android TTS audio actually starts playing through speaker
        // This ensures the first word is typed at the exact instant the first word is spoken
        var waitedMs = 0L
        while (!isSpeaking && waitedMs < 350L) {
            delay(25L)
            waitedMs += 25L
        }

        // 2. Liquid-smooth, continuous typewriter stream (no jerky stop-and-go stutter)
        for (i in 1..aiResponseText.length) {
            displayedLength = i
            val char = aiResponseText[i - 1]
            val charDelay = when (char) {
                '.', '!', '?' -> 70L // Brief natural breath (smooth, no jerky freeze)
                ',', ';', ':' -> 42L // Subtle micro-clause pacing
                ' ' -> 26L           // Fluid word boundary
                else -> 22L          // Buttery 45-50 FPS continuous ink flow
            }
            delay(charDelay)
        }
        isRevealingComplete = true
    }

    val visibleText = remember(aiResponseText, displayedLength) {
        if (aiResponseText.isEmpty()) ""
        else aiResponseText.take(displayedLength)
    }

    val transcriptScrollState = rememberScrollState()

    // Auto-scroll to bottom like modern AI chat (ChatGPT/Claude) whenever new sentences are generated
    LaunchedEffect(displayedLength, transcriptScrollState.maxValue) {
        if (transcriptScrollState.maxValue > 0) {
            transcriptScrollState.scrollTo(transcriptScrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("revealing_speech_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User speech transcript: understated minimal subtitle
        if (!userSpokenText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "“$userSpokenText”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // AI Spoken Response: Clean, modern, open layout with high legibility
        if (aiResponseText.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Speech text container strictly fixed to 4 sentence lines (120dp)
                // New lines auto-scroll up into the top blur fade, keeping the speaker/copy buttons below 100% stationary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                if (transcriptScrollState.value > 0) {
                                    // True alpha mask fade: top sentences smoothly dissolve
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            0.28f to Color.Black,
                                            1.0f to Color.Black
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                            .verticalScroll(transcriptScrollState)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = visibleText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 28.sp,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Soft ambient background blur/gradient at top when sentences scroll up
                    if (transcriptScrollState.value > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.background,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
