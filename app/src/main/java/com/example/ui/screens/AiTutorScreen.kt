package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CallPhase
import com.example.data.model.CallSummary
import com.example.data.model.GrammarFix
import com.example.data.model.TutorLesson
import com.example.data.remote.EnglishVoiceTutorEngine
import com.example.data.remote.HuggingFaceSpaceClient
import com.example.data.remote.TutorLessonCatalog
import com.example.ui.components.AssistantVoiceState
import com.example.ui.components.UiverseLoaderVisualizer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.RedError
import kotlinx.coroutines.delay

/**
 * The English tutor, as a phone call.
 *
 * The purpose of this screen is one thing: a student who is shy about speaking
 * English gets to *talk*, out loud, with someone patient who answers back. That
 * is why it is shaped like a call and not like a chat box — a call has a clock, a
 * microphone that closes and opens on its own, an interruption, and an end. The
 * tutor corrects grammar inside the conversation instead of after it, and a
 * lesson gives the ten minutes a shape.
 *
 * Three ideas hold the screen together:
 *
 *  1. **The animation stays where it was.** The Uiverse orbit loader sits
 *     directly under the top bar, first thing on the page, in every phase — it is
 *     the tutor's face while it talks, thinks and listens.
 *  2. **The caption is the transcript.** The tutor's line is large and readable,
 *     the student's line is quoted underneath it, and a correction appears as a
 *     strip between them the moment it is known — not in a pile at the end.
 *  3. **The dock is a call dock.** Mute, type, replay/interrupt, hang up. There is
 *     no "send" button to hunt for while someone is talking.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiTutorScreen(
    voiceState: AssistantVoiceState,
    callPhase: CallPhase,
    callSeconds: Int,
    isMuted: Boolean,
    tutorLine: String,
    studentLine: String?,
    currentFix: GrammarFix?,
    callSummary: CallSummary?,
    quickReplies: List<String>,
    currentScenario: String,
    activeLessonId: String?,
    lessonStepIndex: Int,
    isLessonComplete: Boolean,
    hfStatusMessage: String,
    isTypingOpen: Boolean,
    isHfSettingsOpen: Boolean,
    customHfToken: String,
    customHfBaseUrl: String,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onReplay: () -> Unit,
    onHearFix: () -> Unit,
    onSendText: (String) -> Unit,
    onSelectScenario: (String) -> Unit,
    onSelectLesson: (TutorLesson?) -> Unit,
    onOpenTyping: (Boolean) -> Unit,
    onOpenHfSettings: (Boolean) -> Unit,
    onUpdateHfCredentials: (String, String) -> Unit,
    onTestHfConnection: () -> Unit,
    onDismissSummary: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTopicSheet by remember { mutableStateOf(false) }
    var typingDraft by remember { mutableStateOf("") }
    val lesson = remember(activeLessonId) { TutorLessonCatalog.find(activeLessonId) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartCall()
        } else {
            onShowSnackbar("Microphone permission is needed to speak with your tutor.")
        }
    }

    fun startCallWithPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onStartCall() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to EmeraldContainer.copy(alpha = 0.35f),
                    0.32f to MaterialTheme.colorScheme.background,
                    1f to MaterialTheme.colorScheme.background
                )
            )
            .testTag("ai_voice_tutor_screen")
    ) {
        if (callPhase == CallPhase.ENDED && callSummary != null) {
            CallSummaryView(
                summary = callSummary,
                onCallAgain = { onDismissSummary(); startCallWithPermission() },
                onChangeTopic = { onDismissSummary(); showTopicSheet = true },
                onClose = onDismissSummary
            )
        } else {
            TutorCallView(
                voiceState = voiceState,
                callPhase = callPhase,
                callSeconds = callSeconds,
                isMuted = isMuted,
                tutorLine = tutorLine,
                studentLine = studentLine,
                currentFix = currentFix,
                quickReplies = quickReplies,
                currentScenario = currentScenario,
                lesson = lesson,
                lessonStepIndex = lessonStepIndex,
                isLessonComplete = isLessonComplete,
                hfStatusMessage = hfStatusMessage,
                onStartCall = { startCallWithPermission() },
                onEndCall = onEndCall,
                onToggleMute = onToggleMute,
                onInterrupt = onInterrupt,
                onReplay = onReplay,
                onHearFix = onHearFix,
                onSendText = onSendText,
                onOpenTyping = { onOpenTyping(true) },
                onOpenTopics = { showTopicSheet = true },
                onOpenSettings = { onOpenHfSettings(true) }
            )
        }
    }

    if (showTopicSheet) {
        TopicPickerSheet(
            currentScenario = currentScenario,
            activeLessonId = activeLessonId,
            onSelectScenario = {
                onSelectScenario(it)
                showTopicSheet = false
            },
            onSelectLesson = {
                onSelectLesson(it)
                showTopicSheet = false
            },
            onOpenSettings = {
                showTopicSheet = false
                onOpenHfSettings(true)
            },
            onDismiss = { showTopicSheet = false }
        )
    }

    if (isTypingOpen) {
        AlertDialog(
            onDismissRequest = { onOpenTyping(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Type instead of speaking", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your sentence is corrected and answered just like a spoken one, " +
                            "so the call keeps going even in a noisy room.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = typingDraft,
                        onValueChange = { typingDraft = it },
                        placeholder = { Text("e.g. I go to the beach yesterday with my cousins.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_keyboard_input_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (typingDraft.isNotBlank()) {
                            onSendText(typingDraft.trim())
                            typingDraft = ""
                            onOpenTyping(false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenTyping(false) }) { Text("Cancel") }
            }
        )
    }

    if (isHfSettingsOpen) {
        HfSettingsDialog(
            token = customHfToken,
            baseUrl = customHfBaseUrl,
            status = hfStatusMessage,
            onSave = { token, url -> onUpdateHfCredentials(token, url) },
            onTest = onTestHfConnection,
            onDismiss = { onOpenHfSettings(false) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  The call itself
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorCallView(
    voiceState: AssistantVoiceState,
    callPhase: CallPhase,
    callSeconds: Int,
    isMuted: Boolean,
    tutorLine: String,
    studentLine: String?,
    currentFix: GrammarFix?,
    quickReplies: List<String>,
    currentScenario: String,
    lesson: TutorLesson?,
    lessonStepIndex: Int,
    isLessonComplete: Boolean,
    hfStatusMessage: String,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onReplay: () -> Unit,
    onHearFix: () -> Unit,
    onSendText: (String) -> Unit,
    onOpenTyping: () -> Unit,
    onOpenTopics: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val onCall = callPhase == CallPhase.LIVE || callPhase == CallPhase.CONNECTING

    // A quiet tap when the floor comes back to the student. On a call you can hear
    // when it is your turn; with a synthetic voice you cannot, and a student who
    // misses the pause starts talking over the tutor.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(voiceState, onCall) {
        if (onCall && voiceState == AssistantVoiceState.LISTENING) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 1. the animation: first thing under the top bar ──────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (onCall) 196.dp else 232.dp),
            contentAlignment = Alignment.Center
        ) {
            TutorOrb(
                state = voiceState,
                isLive = onCall,
                isMuted = isMuted
            )
        }

        // ── 2. who you are talking to, and for how long ──────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Miss Emma",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onCall) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatCallTime(callSeconds),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark,
                    modifier = Modifier.testTag("tutor_call_timer")
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (onCall) {
                "AI English tutor  ·  ${if (isMuted) "muted" else "live"}"
            } else {
                "AI English tutor  ·  ready when you are"
            },
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))
        CallStatusPill(
            state = voiceState,
            phase = callPhase,
            isMuted = isMuted
        )

        // ── 3. captions ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            if (tutorLine.isBlank() && !onCall) {
                StartCallCard(
                    scenario = currentScenario,
                    lesson = lesson,
                    status = hfStatusMessage,
                    onStartCall = onStartCall,
                    onOpenTopics = onOpenTopics,
                    onOpenSettings = onOpenSettings
                )
            } else {
                if (lesson != null) {
                    LessonStepStrip(
                        lesson = lesson,
                        stepIndex = lessonStepIndex,
                        isComplete = isLessonComplete
                    )
                    Spacer(Modifier.height(10.dp))
                }

                TutorCaptionCard(
                    text = tutorLine.ifBlank { "Connecting you to Miss Emma…" },
                    isSpeaking = voiceState == AssistantVoiceState.SPEAKING,
                    phase = callPhase
                )

                if (!studentLine.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "YOU SAID",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "“$studentLine”",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = currentFix != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    currentFix?.let { fix ->
                        Column {
                            Spacer(Modifier.height(10.dp))
                            GrammarFixStrip(fix = fix, onHear = onHearFix)
                        }
                    }
                }

                if (onCall && quickReplies.isNotEmpty() && voiceState != AssistantVoiceState.SPEAKING) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "TRY SAYING",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickReplies.take(3).forEach { reply ->
                            Surface(
                                onClick = { onSendText(reply) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    text = reply,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 4. the call dock ─────────────────────────────────────────────────
        CallDock(
            phase = callPhase,
            state = voiceState,
            isMuted = isMuted,
            onStartCall = onStartCall,
            onEndCall = onEndCall,
            onToggleMute = onToggleMute,
            onInterrupt = onInterrupt,
            onReplay = onReplay,
            onTyping = onOpenTyping,
            onOpenSettings = onOpenSettings
        )
    }
}

/**
 * The orbit animation, with a halo that answers the tutor's state.
 *
 * The loader itself is unchanged — it is the piece the student liked, and it is
 * memorable precisely because it does not change. What is added is the ring
 * behind it: green when the tutor speaks, red while you have the floor, amber
 * while it thinks, grey when the line is idle.
 */
@Composable
private fun TutorOrb(
    state: AssistantVoiceState,
    isLive: Boolean,
    isMuted: Boolean
) {
    val transition = rememberInfiniteTransition(label = "orb_halo")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == AssistantVoiceState.SPEAKING) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantVoiceState.SPEAKING) 900 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    val halo = when {
        isMuted -> Color(0xFFE2E8F0)
        state == AssistantVoiceState.SPEAKING -> EmeraldLight.copy(alpha = 0.22f)
        state == AssistantVoiceState.LISTENING -> RedError.copy(alpha = 0.14f)
        state == AssistantVoiceState.THINKING -> AmberWarning.copy(alpha = 0.16f)
        isLive -> EmeraldLight.copy(alpha = 0.12f)
        else -> EmeraldLight.copy(alpha = 0.10f)
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(if (isLive) 170.dp else 186.dp)
                .background(brush = Brush.radialGradient(listOf(halo, Color.Transparent)), shape = CircleShape)
        )
        // Unchanged, by request: the Uiverse orbit loader stays exactly where it
        // was — below the top bar, the first thing the student sees.
        UiverseLoaderVisualizer(size = if (isLive) 150.dp else 168.dp)
    }
}

@Composable
private fun CallStatusPill(
    state: AssistantVoiceState,
    phase: CallPhase,
    isMuted: Boolean
) {
    val (label, dot, background) = when {
        isMuted -> Triple(
            "Microphone off",
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
        phase == CallPhase.CONNECTING -> Triple("Connecting…", AmberWarning, AmberWarning.copy(alpha = 0.12f))
        phase == CallPhase.IDLE -> Triple(
            "Tap the green button to call",
            EmeraldLight,
            EmeraldContainer.copy(alpha = 0.5f)
        )
        state == AssistantVoiceState.SPEAKING -> Triple("Emma is speaking — tap the mic to interrupt", EmeraldPrimary, EmeraldContainer)
        state == AssistantVoiceState.LISTENING -> Triple("Listening — take your time", RedError, RedError.copy(alpha = 0.10f))
        state == AssistantVoiceState.THINKING -> Triple("Thinking…", AmberWarning, AmberWarning.copy(alpha = 0.12f))
        else -> Triple("Your turn", EmeraldLight, EmeraldContainer.copy(alpha = 0.5f))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StartCallCard(
    scenario: String,
    lesson: TutorLesson?,
    status: String,
    onStartCall: () -> Unit,
    onOpenTopics: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (lesson != null) lesson.title else scenario,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = lesson?.goal
                    ?: "A real conversation, out loud. Miss Emma corrects your grammar as you " +
                    "speak, keeps the call going, and never rushes you.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopicChip("Topic", Icons.Default.School, true, onOpenTopics)
                TopicChip("Lesson", Icons.Default.TrendingUp, lesson != null, { onOpenTopics() })
                TopicChip("Settings", Icons.Default.Settings, false, onOpenSettings)
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = onStartCall,
                shape = RoundedCornerShape(16.dp),
                color = EmeraldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tutor_start_call")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Start call",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = status,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopicChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (active) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) OnEmeraldContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) OnEmeraldContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonStepStrip(lesson: TutorLesson, stepIndex: Int, isComplete: Boolean) {
    val step = lesson.step(stepIndex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EmeraldContainer.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isComplete) "LESSON COMPLETE" else "STEP ${stepIndex + 1} OF ${lesson.stepCount}",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            color = EmeraldDark
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isComplete) lesson.title else "${step?.name} · ${lesson.title}",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnEmeraldContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The tutor's line, revealed as it is spoken.
 *
 * A caption that appears all at once while the voice is still on word three makes
 * the student read ahead and stop listening; revealing it at speaking pace keeps
 * eyes and ears together. When the audio stops, the rest appears immediately — the
 * text is never held hostage by a broken speaker.
 */
@Composable
private fun TutorCaptionCard(text: String, isSpeaking: Boolean, phase: CallPhase) {
    var revealed by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text, isSpeaking) {
        if (!isSpeaking) {
            revealed = text.length
            return@LaunchedEffect
        }
        revealed = 0
        while (revealed < text.length) {
            delay(22)
            revealed = (revealed + 2).coerceAtMost(text.length)
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TUTOR",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = EmeraldPrimary
                )
                if (phase == CallPhase.CONNECTING) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "connecting",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = text.take(revealed.coerceAtLeast(0)),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * The correction strip.
 *
 * It is deliberately not red-alarm styling: a student who is corrected every
 * third sentence and blushes stops talking. Amber, the corrected sentence in
 * place of the mistake, and the rule in three words — plus a button that says it
 * out loud, because a spoken correction is what a real teacher gives.
 */
@Composable
private fun GrammarFixStrip(fix: GrammarFix, onHear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AmberWarning.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tutor_grammar_fix")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "BETTER",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.9.sp,
                color = AmberWarning
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "“${fix.corrected}”",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (fix.rule.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = fix.rule,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                onClick = onHear,
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Hear it",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                }
            }
        }
    }
}

/**
 * The call dock.
 *
 * Four controls, the same four every phone has: mute, keyboard, replay/interrupt,
 * hang up. The hang-up button is the only red thing on the screen, so it is never
 * pressed by accident; everything else is a quiet circle.
 */
@Composable
private fun CallDock(
    phase: CallPhase,
    state: AssistantVoiceState,
    isMuted: Boolean,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onReplay: () -> Unit,
    onTyping: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val onCall = phase == CallPhase.LIVE || phase == CallPhase.CONNECTING

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!onCall) {
            DockButton(
                icon = Icons.Default.Mic,
                label = "Call",
                tint = Color.White,
                background = EmeraldPrimary,
                size = 62.dp,
                onClick = onStartCall,
                testTag = "tutor_dock_call"
            )
            DockButton(
                icon = Icons.Default.Keyboard,
                label = "Type",
                tint = EmeraldDark,
                background = MaterialTheme.colorScheme.surface,
                onClick = onTyping,
                testTag = "tutor_dock_type"
            )
            DockButton(
                icon = Icons.Default.Settings,
                label = "Model",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                background = MaterialTheme.colorScheme.surface,
                onClick = onOpenSettings,
                testTag = "tutor_dock_model"
            )
            return@Row
        }

        DockButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            tint = if (isMuted) Color.White else EmeraldDark,
            background = if (isMuted) RedError else MaterialTheme.colorScheme.surface,
            onClick = onToggleMute,
            testTag = "tutor_dock_mute"
        )
        DockButton(
            icon = Icons.Default.Keyboard,
            label = "Type",
            tint = EmeraldDark,
            background = MaterialTheme.colorScheme.surface,
            onClick = onTyping,
            testTag = "tutor_dock_type"
        )
        DockButton(
            icon = if (state == AssistantVoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.Replay,
            label = if (state == AssistantVoiceState.SPEAKING) "Stop" else "Replay",
            tint = EmeraldDark,
            background = MaterialTheme.colorScheme.surface,
            onClick = { if (state == AssistantVoiceState.SPEAKING) onInterrupt() else onReplay() },
            testTag = "tutor_dock_replay"
        )
        DockButton(
            icon = Icons.Default.CallEnd,
            label = "End",
            tint = Color.White,
            background = RedError,
            size = 62.dp,
            onClick = onEndCall,
            testTag = "tutor_dock_end"
        )
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    testTag: String,
    size: androidx.compose.ui.unit.Dp = 54.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(size)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size / 2.4f))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  After the call
// ─────────────────────────────────────────────────────────────────────────────

/**
 * What the call was worth.
 *
 * Not a score: minutes spoken, turns taken, every correction with the rule it
 * belongs to, and the words met along the way. The closing line tells the student
 * what to do next, because "well done" alone never made anyone fluent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallSummaryView(
    summary: CallSummary,
    onCallAgain: () -> Unit,
    onChangeTopic: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close summary",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Call summary",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = summary.durationLabel,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = EmeraldDark
            )
            Text(
                text = "spoken with Miss Emma  ·  " +
                    (summary.lessonTitle ?: summary.scenario.lowercase()),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat(summary.turns.toString(), "turns")
                SummaryStat(summary.corrections.size.toString(), "corrections")
                SummaryStat(summary.words.size.toString(), "new words")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    summary.turns == 0 -> "No turns taken yet — try a two-minute call."
                    summary.cleanTurnPercent >= 80 ->
                        "${summary.cleanTurnPercent}% of your sentences were already correct. Keep that pace."
                    summary.cleanTurnPercent >= 50 ->
                        "${summary.cleanTurnPercent}% of your sentences were clean. The rest are in the list below."
                    else -> "${summary.cleanTurnPercent}% were clean — that is normal on a first call. Read the fixes below once."
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (summary.corrections.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "GRAMMAR FIXES THIS CALL",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                summary.corrections.forEach { fix ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "“${fix.original}”",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "“${fix.corrected}”",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (fix.rule.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = fix.rule,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 10.5.sp,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (summary.words.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "NEW WORDS",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summary.words.forEach { word ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = word,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnEmeraldContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp)
        ) {
            Surface(
                onClick = onCallAgain,
                shape = RoundedCornerShape(16.dp),
                color = EmeraldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tutor_call_again")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Practise again",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onChangeTopic,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Change topic or lesson",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Topic and lesson picker
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TopicPickerSheet(
    currentScenario: String,
    activeLessonId: String?,
    onSelectScenario: (String) -> Unit,
    onSelectLesson: (TutorLesson?) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLessons by remember { mutableStateOf(activeLessonId != null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "What shall we practise?",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A topic is a conversation. A lesson has steps, so the tutor teaches " +
                    "one grammar point properly.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopicChip("Conversation topics", Icons.Default.School, !showLessons) { showLessons = false }
                TopicChip("Guided lessons", Icons.Default.TrendingUp, showLessons) { showLessons = true }
            }
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showLessons) {
                    PickerRow(
                        title = "Free conversation",
                        subtitle = "No plan — just talk, with corrections.",
                        badge = null,
                        selected = activeLessonId == null,
                        icon = Icons.Default.Mic
                    ) { onSelectLesson(null) }
                    TutorLessonCatalog.lessons.forEach { lesson ->
                        PickerRow(
                            title = lesson.title,
                            subtitle = lesson.goal,
                            badge = "${lesson.level} · ${lesson.stepCount} steps",
                            selected = activeLessonId == lesson.id,
                            icon = Icons.Default.TrendingUp
                        ) { onSelectLesson(lesson) }
                    }
                } else {
                    EnglishVoiceTutorEngine.scenarios.forEach { scenario ->
                        PickerRow(
                            title = scenario,
                            subtitle = null,
                            badge = null,
                            selected = activeLessonId == null && scenario == currentScenario,
                            icon = scenarioIcon(scenario)
                        ) { onSelectScenario(scenario) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Model and connection", color = EmeraldPrimary, fontSize = 12.5.sp)
            }
        }
    }
}

private fun scenarioIcon(scenario: String): ImageVector = when (scenario) {
    "Coffee Shop & Cafe" -> Icons.Default.LocalCafe
    "Job Interview" -> Icons.Default.Work
    "Travel & Airport" -> Icons.Default.Flight
    "Grammar Doctor" -> Icons.Default.HealthAndSafety
    "A/L Exam Speaking" -> Icons.Default.School
    else -> Icons.Default.Mic
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    badge: String?,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EmeraldContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) OnEmeraldContainer else MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = badge,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Model settings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HfSettingsDialog(
    token: String,
    baseUrl: String,
    status: String,
    onSave: (String, String) -> Unit,
    onTest: () -> Unit,
    onDismiss: () -> Unit
) {
    var tokenInput by remember { mutableStateOf(token) }
    var urlInput by remember { mutableStateOf(baseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tutor model", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Connection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = OnEmeraldContainer
                        )
                        Text(text = status, fontSize = 12.sp, color = EmeraldDark)
                    }
                }
                Text(
                    text = "The call works without a model: Miss Emma falls back to a local " +
                        "tutor script and the grammar rules on this phone. A token is only " +
                        "needed if the Space is private.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxxx") },
                    label = { Text("Access token") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text(HuggingFaceSpaceClient.DEFAULT_SPACE_DIRECT_URL) },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )
                OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
                    Text("Test connection", color = EmeraldPrimary, fontSize = 12.5.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tokenInput.trim(), urlInput.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/** mm:ss, the way a call app counts. */
private fun formatCallTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
