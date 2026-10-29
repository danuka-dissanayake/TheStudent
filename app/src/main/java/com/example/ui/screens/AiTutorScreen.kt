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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.model.TutorCallEntry
import com.example.data.model.TutorLesson
import com.example.data.remote.HuggingFaceSpaceClient
import com.example.data.remote.TutorLessonCatalog
import com.example.ui.components.AssistantVoiceState
import com.example.ui.theme.MaisonNeueFontFamily

// ─────────────────────────────────────────────────────────────────────────────
//  Palette — the pastel / glass language of the approved mockup
//  (designs/35-ai-tutor-glass.png)
// ─────────────────────────────────────────────────────────────────────────────

private val Ink = Color(0xFF1F2430)
private val Muted = Color(0xFF6B7280)
private val Faint = Color(0xFF9AA3AF)
private val Violet = Color(0xFF7C6CF6)
private val VioletDeep = Color(0xFF5B4BD6)
private val Coral = Color(0xFFF2836B)
private val Mint = Color(0xFF3FBF9A)

private val CanvasTop = Color(0xFFDCE6FB)
private val CanvasMid = Color(0xFFEFE7F5)
private val CanvasBottom = Color(0xFFFBEFE6)

private val GlassFill = Color(0xB3FFFFFF)      // 70% white
private val GlassFillSoft = Color(0x8CFFFFFF)   // 55%
private val GlassEdge = Color(0xE6FFFFFF)       // 90% white hairline
private val BubbleTutor = Color(0xB8FFFFFF)
private val BubbleStudent = Color(0xE67C6CF6)
private val CorrectionFill = Color(0xD1FFEEE4)  // warm, still translucent

/**
 * The English tutor screen — glass edition.
 *
 * The screen has one job: get a shy student *talking*. Everything on it follows
 * from that, which is why there is exactly **one control**: the microphone.
 *
 *  - Behind you speak / in the call — the same violet mic, in the same place.
 *  - No keyboard, no reply chips, no dock row. A student who is mid-sentence
 *    should not be choosing between five buttons.
 *  - Leaving, changing topic and model settings live in the ⋯ menu, out of the
 *    way until they are wanted.
 *
 * The visual language is pastel glass: a soft gradient canvas, frosted panels,
 * and an iridescent sphere that is the tutor's "face" — big before the call,
 * small once the conversation starts, so the words get the room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorScreen(
    voiceState: AssistantVoiceState,
    callPhase: CallPhase,
    callSeconds: Int,
    isMuted: Boolean,
    transcript: List<TutorCallEntry>,
    currentFix: GrammarFix?,
    callSummary: CallSummary?,
    currentScenario: String,
    studentName: String,
    spokenMinutes: Int,
    spokenCalls: Int,
    activeLessonId: String?,
    lessonStepIndex: Int,
    isLessonComplete: Boolean,
    hfStatusMessage: String,
    isHfSettingsOpen: Boolean,
    customHfToken: String,
    customHfBaseUrl: String,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    /** Leave the tutor tab entirely (the idle screen's back button). */
    onExit: () -> Unit,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onFinishTurn: () -> Unit,
    onReplay: () -> Unit,
    onHearFix: () -> Unit,
    onSelectScenario: (String) -> Unit,
    onSelectLesson: (TutorLesson?) -> Unit,
    onOpenHfSettings: (Boolean) -> Unit,
    onUpdateHfCredentials: (String, String) -> Unit,
    onTestHfConnection: () -> Unit,
    onDismissSummary: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val lesson = remember(activeLessonId) { TutorLessonCatalog.find(activeLessonId) }
    val onCall = callPhase == CallPhase.LIVE || callPhase == CallPhase.CONNECTING

    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartCall()
        } else {
            onShowSnackbar("Microphone permission is needed to speak with your tutor.")
        }
    }

    fun startWithPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onStartCall() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Box(modifier = modifier.fillMaxSize().testTag("ai_voice_tutor_screen")) {
        TutorBackdrop()

        when {
            callPhase == CallPhase.ENDED && callSummary != null -> SummaryContent(
                summary = callSummary,
                onCallAgain = { onDismissSummary(); startWithPermission() },
                onChangeTopic = { onDismissSummary(); menuOpen = true },
                onClose = onDismissSummary
            )

            onCall -> LiveContent(
                voiceState = voiceState,
                callPhase = callPhase,
                callSeconds = callSeconds,
                isMuted = isMuted,
                transcript = transcript,
                currentFix = currentFix,
                lesson = lesson,
                lessonStepIndex = lessonStepIndex,
                isLessonComplete = isLessonComplete,
                onMic = onToggleMute,
                onInterrupt = onInterrupt,
                onFinishTurn = onFinishTurn,
                onReplay = onReplay,
                onHearFix = onHearFix,
                onMenu = { menuOpen = true },
                onBack = onEndCall
            )

            else -> IdleContent(
                studentName = studentName,
                spokenMinutes = spokenMinutes,
                spokenCalls = spokenCalls,
                scenario = currentScenario,
                lesson = lesson,
                status = hfStatusMessage,
                onMic = { startWithPermission() },
                onMenu = { menuOpen = true },
                onExit = onExit,
                onPickTopic = { menuOpen = true }
            )
        }
    }

    if (menuOpen) {
        TutorMenuSheet(
            currentScenario = currentScenario,
            activeLessonId = activeLessonId,
            onCall = onCall,
            onSelectScenario = { onSelectScenario(it); menuOpen = false },
            onSelectLesson = { onSelectLesson(it); menuOpen = false },
            onOpenSettings = { menuOpen = false; onOpenHfSettings(true) },
            onEndCall = { menuOpen = false; onEndCall() },
            onDismiss = { menuOpen = false }
        )
    }

    if (isHfSettingsOpen) {
        ModelSettingsDialog(
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
//  Canvas
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The gradient room everything sits in.
 *
 * Two luminous corners (cool top-left, warm bottom-right) read as light falling
 * on a wall; without them the glass panels have nothing to be glass *against*
 * and the screen goes flat grey.
 */
@Composable
private fun TutorBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CanvasTop, CanvasMid, CanvasBottom)))
    ) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = (-140).dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x99C6CEFF), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 110.dp, y = 90.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x8CFFD6C4), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-120).dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x66FFCEE8), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * The iridescent sphere — the tutor's face.
 *
 * Six soft colour fields blended inside a circle, with a specular highlight and
 * a slow drift, drawn on a Canvas so it costs one composable rather than a
 * dozen stacked gradients. The drift is deliberately slow: this sits on screen
 * for minutes at a time, and anything faster becomes something to stare at
 * instead of something to talk to.
 */
@Composable
private fun IridescentOrb(
    size: androidx.compose.ui.unit.Dp,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(26_000), repeatMode = RepeatMode.Restart),
        label = "orb_drift"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(4200), repeatMode = RepeatMode.Reverse),
        label = "orb_breathe"
    )

    Canvas(
        // The blobs are painted wider than the sphere and clipped back to it: a
        // hard circular mask is what makes it read as a ball of liquid glass
        // instead of six overlapping smudges.
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = breathe
                scaleY = breathe
            }
            .clip(CircleShape)
    ) {
        // Held explicitly: a local function cannot lean on the lambda's implicit
        // receiver, and these helpers are clearer with the scope named.
        val scope = this
        val r = scope.size.minDimension / 2f
        val c = Offset(scope.size.width / 2f, scope.size.height / 2f)

        fun blob(angleDeg: Float, distance: Float, radius: Float, color: Color) {
            val a = Math.toRadians((angleDeg + drift).toDouble())
            val x = (c.x + (distance * r * Math.cos(a))).toFloat()
            val y = (c.y + (distance * r * Math.sin(a))).toFloat()
            scope.orbBlob(Offset(x, y), radius * r, color)
        }

        // sphere body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF4F7FF), Color(0xFFE7EDFD)),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
                radius = r * 1.5f
            ),
            radius = r,
            center = c
        )

        blob(300f, 0.35f, 0.75f, Color(0xCCBFD3FF))
        blob(35f, 0.42f, 0.68f, Color(0xCCFFD8C2))
        blob(95f, 0.48f, 0.60f, Color(0xCCFFC9E4))
        blob(160f, 0.40f, 0.62f, Color(0xBFC9C4FF))
        blob(235f, 0.44f, 0.55f, Color(0xBFB8EDE9))
        blob(0f, 0f, 0.55f, Color(0xB3FFFFFF))

        // specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xF2FFFFFF), Color(0x00FFFFFF)),
                center = Offset(c.x - r * 0.33f, c.y - r * 0.42f),
                radius = r * 0.55f
            ),
            radius = r * 0.55f,
            center = Offset(c.x - r * 0.33f, c.y - r * 0.42f)
        )

        if (isLive) {
            drawCircle(
                color = Color(0x33FFFFFF),
                radius = r,
                center = c,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.02f)
            )
        }
    }
}

/**
 * One drifting colour field of the orb.
 *
 * A `DrawScope` extension rather than a local function so the alpha falloff and
 * the centre are computed once, in one place, for every layer.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.orbBlob(
    center: Offset,
    radius: Float,
    color: Color
) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chrome
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
            GlassCircleButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Ink,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = Muted
                )
            }
            GlassCircleButton(
                onClick = onMenu,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Ink)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(GlassFill)
            .border(1.dp, GlassEdge, CircleShape)
            .clickable(onClick = onClick)
            .testTag("tutor_glass_circle"),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    fill: Color = GlassFillSoft,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = modifier
        .clip(shape)
        .background(fill)
        .border(1.dp, GlassEdge, shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Idle: before the call
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    studentName: String,
    spokenMinutes: Int,
    spokenCalls: Int,
    scenario: String,
    lesson: TutorLesson?,
    status: String,
    onMic: () -> Unit,
    onMenu: () -> Unit,
    onExit: () -> Unit,
    onPickTopic: () -> Unit
) {
    val firstName = remember(studentName) {
        studentName.trim().split(" ").firstOrNull().orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            title = "Miss Emma",
            subtitle = "English speaking tutor",
            onBack = onExit,
            onMenu = onMenu
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            IridescentOrb(size = 218.dp, isLive = false)
            Spacer(Modifier.height(26.dp))
            Text(
                text = if (firstName.isBlank()) "Hello" else "Hello, $firstName",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.5.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ready to speak English?",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(
                    icon = Icons.Default.Mic,
                    tint = Violet,
                    title = "Free talk",
                    subtitle = "Talk freely · I fix as you go",
                    selected = lesson == null,
                    onClick = onPickTopic,
                    modifier = Modifier.weight(1f)
                )
                ModeCard(
                    icon = Icons.Default.PlayArrow,
                    tint = Mint,
                    title = lesson?.title ?: "Guided lesson",
                    subtitle = if (lesson != null) {
                        "Lesson · ${lesson.stepCount} steps · ${lesson.level}"
                    } else {
                        "4 steps · A2–B1"
                    },
                    selected = lesson != null,
                    onClick = onPickTopic,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            LevelStrip(spokenMinutes = spokenMinutes, spokenCalls = spokenCalls)
            Spacer(Modifier.height(10.dp))
            Text(
                text = status,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                color = Faint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        MicDock(
            state = AssistantVoiceState.IDLE,
            phase = CallPhase.IDLE,
            isMuted = false,
            caption = "Tap to speak",
            onMic = onMic,
            onLongPress = null
        )
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.heightIn(min = 100.dp),
        fill = if (selected) GlassFill else GlassFillSoft,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 9.5.sp,
                lineHeight = 13.sp,
                color = Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A quiet progress line for speaking time.
 *
 * Deliberately not a streak or a score: minutes actually spoken is the one number
 * that improves speaking, and it is the only one that cannot be gamed by tapping.
 */
@Composable
private fun LevelStrip(spokenMinutes: Int, spokenCalls: Int) {
    // Bands are minutes spoken, not grammar scores: the only thing that moves a
    // speaking level is time spent speaking. Thresholds are deliberately close
    // together at the start, where the first ten minutes feel like a lot.
    val (label, floor, ceiling) = when {
        spokenMinutes >= 240 -> Triple("B2", 240, 480)
        spokenMinutes >= 90 -> Triple("B1+", 90, 240)
        spokenMinutes >= 30 -> Triple("B1", 30, 90)
        spokenMinutes >= 10 -> Triple("A2+", 10, 30)
        else -> Triple("A2", 0, 10)
    }
    val progress = if (ceiling <= floor) {
        1f
    } else {
        ((spokenMinutes - floor).toFloat() / (ceiling - floor)).coerceIn(0.04f, 1f)
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(
                text = "Your speaking level",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                color = Muted
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$label  ·  $spokenMinutes min spoken",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (spokenCalls == 0) {
                        "First call"
                    } else {
                        "$spokenCalls ${if (spokenCalls == 1) "call" else "calls"}"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Mint
                )
            }
            Spacer(Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Violet.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Violet)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Live: the call
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LiveContent(
    voiceState: AssistantVoiceState,
    callPhase: CallPhase,
    callSeconds: Int,
    isMuted: Boolean,
    transcript: List<TutorCallEntry>,
    currentFix: GrammarFix?,
    lesson: TutorLesson?,
    lessonStepIndex: Int,
    isLessonComplete: Boolean,
    onMic: () -> Unit,
    onInterrupt: () -> Unit,
    onFinishTurn: () -> Unit,
    onReplay: () -> Unit,
    onHearFix: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit
) {
    val scroll = rememberScrollState()
    val haptics = LocalHapticFeedback.current

    // A quiet tap when the floor comes back to the student: with a synthetic
    // voice there is no "your turn" silence to recognise, and talking over the
    // tutor is the most common way a learner loses confidence mid-call.
    LaunchedEffect(voiceState) {
        if (voiceState == AssistantVoiceState.LISTENING) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(transcript.size, voiceState) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            title = "Miss Emma",
            subtitle = if (callPhase == CallPhase.CONNECTING) {
                "connecting…"
            } else {
                "${formatCallTime(callSeconds)}  ·  live"
            },
            onBack = onBack,
            onMenu = onMenu
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            IridescentOrb(size = 84.dp, isLive = true)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(statusColor(voiceState, isMuted))
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = statusLabel(voiceState, callPhase, isMuted),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMuted) Muted else VioletDeep
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "·  tap the mic to reply",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                color = Muted
            )
        }

        if (lesson != null) {
            Spacer(Modifier.height(10.dp))
            LessonChip(lesson = lesson, stepIndex = lessonStepIndex, isComplete = isLessonComplete)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (transcript.isEmpty()) {
                Text(
                    text = "Connecting you to Miss Emma…",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            transcript.forEach { entry ->
                when (entry.role) {
                    TutorCallEntry.Role.STUDENT -> StudentBubble(entry.text)
                    TutorCallEntry.Role.TUTOR -> TutorBubble(entry.text)
                    TutorCallEntry.Role.FIX -> entry.fix?.let {
                        CorrectionCard(fix = it, onHear = onHearFix)
                    }
                }
            }

            // The live correction is shown even before the model's reply lands, so
            // the student sees the fix while the words are still fresh.
            if (currentFix != null && transcript.none { it.role == TutorCallEntry.Role.FIX }) {
                CorrectionCard(fix = currentFix, onHear = onHearFix)
            }

            AnimatedVisibility(
                visible = voiceState == AssistantVoiceState.THINKING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ThinkingBubble()
            }
            Spacer(Modifier.height(4.dp))
        }

        // One button, whose meaning depends on whose turn it is. That is the whole
        // interaction: the student never has to find the right control mid-sentence.
        val micAction: () -> Unit = when {
            isMuted -> onMic
            voiceState == AssistantVoiceState.SPEAKING -> onInterrupt
            voiceState == AssistantVoiceState.LISTENING -> onFinishTurn
            voiceState == AssistantVoiceState.THINKING -> onReplay
            else -> onMic
        }
        MicDock(
            state = voiceState,
            phase = callPhase,
            isMuted = isMuted,
            caption = when {
                isMuted -> "Muted — tap to unmute · hold to speak again"
                voiceState == AssistantVoiceState.SPEAKING -> "Tap to interrupt"
                voiceState == AssistantVoiceState.THINKING -> "Thinking… tap to hear the last line again"
                voiceState == AssistantVoiceState.LISTENING -> "Listening — tap when you have finished"
                else -> "Tap to speak"
            },
            onMic = micAction,
            onLongPress = onMic
        )
    }
}

private fun statusColor(state: AssistantVoiceState, isMuted: Boolean): Color = when {
    isMuted -> Faint
    state == AssistantVoiceState.LISTENING -> Coral
    state == AssistantVoiceState.THINKING -> Color(0xFFE0A83C)
    else -> Violet
}

private fun statusLabel(state: AssistantVoiceState, phase: CallPhase, isMuted: Boolean): String = when {
    isMuted -> "Muted"
    phase == CallPhase.CONNECTING -> "Connecting"
    state == AssistantVoiceState.LISTENING -> "Your turn"
    state == AssistantVoiceState.THINKING -> "Thinking"
    state == AssistantVoiceState.SPEAKING -> "Emma is speaking"
    else -> "Ready"
}

@Composable
private fun LessonChip(lesson: TutorLesson, stepIndex: Int, isComplete: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x66FFFFFF))
                .border(1.dp, GlassEdge, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isComplete) {
                    "Lesson complete · ${lesson.title}"
                } else {
                    "Step ${stepIndex + 1}/${lesson.stepCount} · " +
                        "${lesson.step(stepIndex)?.name ?: lesson.title}"
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VioletDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TutorBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp))
                .background(BubbleTutor)
                .border(
                    1.dp, GlassEdge,
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = text,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Ink
            )
        }
    }
}

@Composable
private fun StudentBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp))
                .background(BubbleStudent)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = text,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Color.White
            )
        }
    }
}

/**
 * The correction, inline where it happened.
 *
 * Warm, not red: a student corrected every third sentence and shown an alarm
 * stops speaking. The corrected sentence replaces the mistake, the rule is named
 * in three words, and "Hear it" reads it aloud — a spoken correction is what a
 * real teacher gives.
 */
@Composable
private fun CorrectionCard(fix: GrammarFix, onHear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CorrectionFill)
            .border(1.dp, Color(0xCCFFFFFF), RoundedCornerShape(18.dp))
            .clickable(onClick = onHear)
            .padding(14.dp)
            .testTag("tutor_grammar_fix")
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Coral))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "BETTER",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = Coral
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Hear it  ▸",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Coral
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = "“${fix.corrected}”",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            if (fix.rule.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = fix.rule,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
                    color = Color(0xFF9A6B55)
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp))
                .background(BubbleTutor)
                .border(
                    1.dp, GlassEdge,
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val transition = rememberInfiniteTransition(label = "dot$index")
                    val alpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha$index"
                    )
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Violet.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  The one control
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The microphone, and nothing else.
 *
 * The rings are the tutor's state made visible — a slow bloom while it speaks,
 * a still stack while it listens, a dull ring when muted — so the student reads
 * whose turn it is without looking for a label.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MicDock(
    state: AssistantVoiceState,
    phase: CallPhase,
    isMuted: Boolean,
    caption: String,
    onMic: () -> Unit,
    onLongPress: (() -> Unit)?
) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = if (state == AssistantVoiceState.SPEAKING) 1.06f else 1f,
        targetValue = if (state == AssistantVoiceState.SPEAKING) 1.16f else 1.04f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "mic_pulse"
    )
    val isLive = phase == CallPhase.LIVE || phase == CallPhase.CONNECTING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(
                        if (isMuted) Faint.copy(alpha = 0.10f) else Violet.copy(alpha = 0.14f)
                    )
            )
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMuted) Faint.copy(alpha = 0.16f) else Violet.copy(alpha = 0.22f)
                    )
            )
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) Faint else Violet)
                    .combinedClickable(
                        onClick = onMic,
                        onLongClick = onLongPress
                    )
                    .testTag("tutor_mic")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = caption,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  After the call
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryContent(
    summary: CallSummary,
    onCallAgain: () -> Unit,
    onChangeTopic: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassHeader(
            title = "Call summary",
            subtitle = "with Miss Emma",
            onBack = onClose,
            onMenu = onClose
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = summary.durationLabel,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Ink
            )
            Text(
                text = summary.lessonTitle ?: summary.scenario,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = Muted
            )

            Spacer(Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStat(summary.turns.toString(), "turns")
                    SummaryStat(summary.corrections.size.toString(), "corrections")
                    SummaryStat(summary.words.size.toString(), "new words")
                }
            }

            if (summary.corrections.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "GRAMMAR FIXES",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = Muted
                )
                Spacer(Modifier.height(8.dp))
                summary.corrections.forEach { fix ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "“${fix.original}”",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                color = Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "“${fix.corrected}”",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            if (fix.rule.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = fix.rule,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 10.sp,
                                    color = Coral
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
                    color = Muted
                )
                Spacer(Modifier.height(8.dp))
                summary.words.forEach { word ->
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(CircleShape)
                            .background(Color(0x66FFFFFF))
                            .border(1.dp, GlassEdge, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = word,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VioletDeep
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (summary.cleanTurnPercent >= 80) {
                            "Strong call"
                        } else if (summary.turns == 0) {
                            "A quiet call"
                        } else {
                            "Next time"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            summary.turns == 0 ->
                                "No turns yet. Try two minutes — one sentence is enough to start."
                            summary.cleanTurnPercent >= 80 ->
                                "${summary.cleanTurnPercent}% of your sentences were already correct. " +
                                    "Keep the same pace and add one new word per call."
                            else ->
                                "${summary.cleanTurnPercent}% of your sentences were clean. " +
                                    "Say the corrections below out loud once before your next call."
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Muted
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 14.dp)
        ) {
            Surface(
                onClick = onCallAgain,
                shape = RoundedCornerShape(18.dp),
                color = Violet,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("tutor_call_again")
            ) {
                Box(contentAlignment = Alignment.Center) {
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
                shape = RoundedCornerShape(18.dp),
                color = GlassFillSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassEdge),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Change topic or lesson",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
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
            color = Ink
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            color = Muted
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Menu: topic, lesson, hang up, model — all off the main surface
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorMenuSheet(
    currentScenario: String,
    activeLessonId: String?,
    onCall: Boolean,
    onSelectScenario: (String) -> Unit,
    onSelectLesson: (TutorLesson?) -> Unit,
    onOpenSettings: () -> Unit,
    onEndCall: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLessons by remember { mutableStateOf(activeLessonId != null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF6F7FB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "What shall we practise?",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A topic is a conversation. A lesson has steps, so the tutor " +
                    "teaches one grammar point properly.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = Muted
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuTab("Conversation", !showLessons) { showLessons = false }
                MenuTab("Lessons", showLessons) { showLessons = true }
            }
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showLessons) {
                    MenuRow(
                        title = "Free conversation",
                        subtitle = "No plan — just talk, with corrections",
                        badge = null,
                        selected = activeLessonId == null,
                        onClick = { onSelectLesson(null) }
                    )
                    TutorLessonCatalog.lessons.forEach { lesson ->
                        MenuRow(
                            title = lesson.title,
                            subtitle = lesson.goal,
                            badge = "${lesson.level} · ${lesson.stepCount}",
                            selected = activeLessonId == lesson.id,
                            onClick = { onSelectLesson(lesson) }
                        )
                    }
                } else {
                    com.example.data.remote.EnglishVoiceTutorEngine.scenarios.forEach { scenario ->
                        MenuRow(
                            title = scenario,
                            subtitle = null,
                            badge = null,
                            selected = activeLessonId == null && scenario == currentScenario,
                            onClick = { onSelectScenario(scenario) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenSettings) {
                    Text("Model settings", color = VioletDeep, fontSize = 12.5.sp)
                }
                Spacer(Modifier.weight(1f))
                if (onCall) {
                    TextButton(onClick = onEndCall) {
                        Text("End call", color = Coral, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Violet else Color(0x66FFFFFF))
            .border(1.dp, if (selected) Color.Transparent else GlassEdge, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Muted
        )
    }
}

@Composable
private fun MenuRow(
    title: String,
    subtitle: String?,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Violet.copy(alpha = 0.12f) else Color(0x99FFFFFF))
            .border(1.dp, if (selected) Violet.copy(alpha = 0.35f) else GlassEdge, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = Ink
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Muted,
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
                    color = VioletDeep
                )
            }
        }
    }
}

@Composable
private fun ModelSettingsDialog(
    token: String,
    baseUrl: String,
    status: String,
    onSave: (String, String) -> Unit,
    onTest: () -> Unit,
    onDismiss: () -> Unit
) {
    var tokenInput by remember { mutableStateOf(token) }
    var urlInput by remember { mutableStateOf(baseUrl) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F8FC),
        title = {
            Text(
                text = "Tutor model",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = status,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = VioletDeep
                )
                Text(
                    text = "The call works without a model: Miss Emma falls back to a local " +
                        "script and the grammar rules on this phone. A token is only needed " +
                        "if the Space is private.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Muted
                )
                androidx.compose.material3.OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Access token") },
                    placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxxx") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Base URL") },
                    placeholder = { Text(HuggingFaceSpaceClient.DEFAULT_SPACE_DIRECT_URL) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onTest) {
                    Text("Test connection", color = VioletDeep, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(tokenInput.trim(), urlInput.trim()) }) {
                Text("Save", color = VioletDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Muted) }
        }
    )
}

/** mm:ss, the way a call app counts. */
private fun formatCallTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
