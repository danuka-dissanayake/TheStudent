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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.remote.TutorLessonCatalog
import com.example.ui.components.AssistantVoiceState
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.flow.StateFlow

// ─────────────────────────────────────────────────────────────────────────────
//  Palette — the app's own colours, worn as pastel glass
//
//  Emerald and teal, not lilac: the tutor is a page of TheStudent, and a student
//  who taps into it should not feel they left the app. The values below are the
//  brand tokens (ui/theme/Color.kt) softened into translucent glass.
// ─────────────────────────────────────────────────────────────────────────────

private val Ink = Color(0xFF0F172A)          // TextPrimaryLight
private val Muted = Color(0xFF64748B)        // TextSecondaryLight
private val Faint = Color(0xFF94A3B8)

private val Brand = Color(0xFF059669)        // EmeraldPrimary
private val BrandDeep = Color(0xFF047857)    // EmeraldDark
private val TealDeep = Color(0xFF0F766E)     // TealSecondary
private val Fresh = Color(0xFF10B981)        // EmeraldLight
private val Lime = Color(0xFF84CC16)         // LimeAccent
private val Warm = Color(0xFFF59E0B)         // AmberWarning — corrections

private val CanvasTop = Color(0xFFE2F2EB)    // mint
private val CanvasMid = Color(0xFFEFF9F5)    // foam
private val CanvasBottom = Color(0xFFE3F0F7) // cool aqua

private val GlassFill = Color(0xB8FFFFFF)      // 72% white
private val GlassFillSoft = Color(0x96FFFFFF)  // 59%
private val GlassEdge = Color(0xE6FFFFFF)      // 90% hairline
private val BubbleTutor = Color(0xC2FFFFFF)
private val BubbleStudent = Color(0xF2059696)
private val CorrectionFill = Color(0xD9FFF7E6) // warm amber, still translucent

/** What the sphere is doing. It is the only thing on the screen that moves. */
private enum class OrbMode { IDLE, LISTENING, SPEAKING, THINKING }

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
    /** Live microphone energy (0..1). Kept out of UiState so it only redraws the orb. */
    micLevel: StateFlow<Float>,
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
    onDismissSummary: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    // Which tab the sheet opens on. The lesson tile asks for the lesson list; the
    // ⋯ button opens wherever the student left off.
    var menuLessons by remember { mutableStateOf(false) }
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
                onChangeTopic = {
                    onDismissSummary()
                    menuLessons = true
                    menuOpen = true
                },
                onClose = onDismissSummary
            )

            onCall -> LiveContent(
                voiceState = voiceState,
                callPhase = callPhase,
                callSeconds = callSeconds,
                isMuted = isMuted,
                micLevel = micLevel,
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
                onMenu = {
                    menuLessons = activeLessonId != null
                    menuOpen = true
                },
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
                onMenu = {
                    menuLessons = activeLessonId != null
                    menuOpen = true
                },
                onExit = onExit,
                onPickLesson = {
                    menuLessons = true
                    menuOpen = true
                }
            )
        }
    }

    if (menuOpen) {
        TutorMenuSheet(
            currentScenario = currentScenario,
            activeLessonId = activeLessonId,
            startOnLessons = menuLessons,
            onCall = onCall,
            // Picking a topic is not a settings change — it is the start of the
            // call. Off a call we open it immediately; mid-call the topic swaps
            // and the tutor carries on in the same call.
            onSelectScenario = { picked ->
                menuOpen = false
                onSelectScenario(picked)
                if (!onCall) startWithPermission()
            },
            onSelectLesson = { picked ->
                menuOpen = false
                onSelectLesson(picked)
                if (!onCall) startWithPermission()
            },
            onEndCall = { menuOpen = false; onEndCall() },
            onDismiss = { menuOpen = false }
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
                        colors = listOf(Color(0x99A7EFD3), Color.Transparent)
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
                        colors = listOf(Color(0x8CBFE7F5), Color.Transparent)
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
                        colors = listOf(Color(0x66D9F2C4), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * The iridescent sphere — the tutor's face, and the only thing here that behaves
 * like a living thing.
 *
 * It has to answer three questions a student asks silently before they speak:
 * *is it awake?*, *can it hear me?*, *is it my turn?* So the sphere does not idle
 * in a loop — it breathes while it waits, swells with the actual loudness of the
 * microphone while it listens, pulses in a speech-like rhythm while it talks,
 * and settles into a slow shimmer while it thinks. Touching it presses in and
 * starts the call, because on every voice assistant people try the orb first.
 *
 * Everything is drawn in one `Canvas`, masked to a circle: six drifting colour
 * fields, a halo that widens with the voice, ripple rings while listening, a
 * specular highlight and a rim.
 */
@Composable
private fun IridescentOrb(
    size: androidx.compose.ui.unit.Dp,
    mode: OrbMode,
    micLevel: Float = 0f,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(26_000), repeatMode = RepeatMode.Restart),
        label = "orb_drift"
    )
    // Waiting: a slow breath. Long enough that it never pulls the eye.
    val breathe = transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(4_200), repeatMode = RepeatMode.Reverse),
        label = "orb_breathe"
    )
    // Talking: a ~600 ms swell, which is roughly the rhythm of a spoken phrase
    // and reads as speech rather than as a loading spinner.
    val speechPulse = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(620), repeatMode = RepeatMode.Reverse),
        label = "orb_speech"
    )
    // Thinking: a tighter, faster shimmer.
    val thinkPulse = transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "orb_think"
    )
    // A ring leaves the sphere every two seconds while the call is live.
    val ripple = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_ripple"
    )

    var pressed by remember { mutableStateOf(false) }
    // Springs rather than a tween: the sphere should lean into a loud word and
    // settle back, the way a speaker's shoulders do.
    val micEnergy = androidx.compose.animation.core.animateFloatAsState(
        targetValue = micLevel.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.55f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "orb_mic"
    )

    // Every animated value above is a State that is *read inside the draw and
    // layer blocks*, never in composition: the sphere then re-draws without
    // re-composing, which is what keeps a 60fps orb cheap next to a scrolling
    // thread. The helpers below are the single source of the "how alive is it"
    // number, used by both blocks.
    fun energyOf(): Float = when (mode) {
        OrbMode.IDLE -> 0f
        OrbMode.LISTENING -> micEnergy.value
        OrbMode.SPEAKING -> speechPulse.value
        OrbMode.THINKING -> thinkPulse.value
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val energy = energyOf()
                val swell = when (mode) {
                    OrbMode.LISTENING -> 0.16f * energy
                    OrbMode.SPEAKING -> 0.07f * energy
                    OrbMode.THINKING -> 0.02f * energy
                    OrbMode.IDLE -> 0f
                }
                val scale = breathe.value * (1f + swell) * (if (pressed) 0.93f else 1f)
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(onTap) {
                if (onTap == null) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { onTap() }
                )
            }
    ) {
        val scope = this
        // The box is bigger than the sphere on purpose: the spare ring of space is
        // where the glow and the drop shadow live.
        val r = scope.size.minDimension / 2f
        val sr = r * 0.84f
        val c = Offset(scope.size.width / 2f, scope.size.height / 2f)
        // Read here, at draw time, so the sphere animates without re-composing.
        val energy = energyOf()
        val driftDeg = drift.value
        val rippleT = ripple.value
        val pressLift = if (pressed) 1f else 0f

        // ── Shadow ───────────────────────────────────────────────────────────
        // A soft ground shadow tucked under the sphere: this is what stops it
        // looking pasted onto the page. It deepens a touch while it is speaking
        // or being pressed, as if the sphere sat closer to the glass.
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2F6F5C).copy(alpha = 0.20f + 0.08f * energy + 0.06f * pressLift),
                    Color(0xFF2F6F5C).copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(c.x, c.y + sr * 0.62f),
                radius = sr * 0.98f
            ),
            radius = sr * 0.98f,
            center = Offset(c.x, c.y + sr * 0.62f)
        )

        // ── Bloom ────────────────────────────────────────────────────────────
        // Brand light spilling out of the sphere, brightest at the bottom-right
        // where the light would come from, and stronger when the voice is loud.
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Brand.copy(alpha = 0.16f + 0.22f * energy),
                    Brand.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(c.x + sr * 0.10f, c.y + sr * 0.10f),
                radius = r
            ),
            radius = r,
            center = Offset(c.x + sr * 0.10f, c.y + sr * 0.10f)
        )

        // Ripples while the call is live: the "it can hear me" cue.
        if (mode != OrbMode.IDLE) {
            repeat(2) { index ->
                val t = (rippleT + index * 0.5f) % 1f
                val alpha = ((1f - t) * (0.16f + 0.40f * energy)).coerceIn(0f, 0.5f)
                scope.drawCircle(
                    color = Brand.copy(alpha = alpha),
                    radius = sr * (0.92f + 0.42f * t),
                    center = c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = sr * 0.035f)
                )
            }
        }

        // ── The sphere itself, masked to its own circle ──────────────────────
        val oval = Path().apply {
            addOval(Rect(center = c, radius = sr))
        }
        scope.clipPath(oval) {
            // body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF7FFFC), Color(0xFFDBEFE6)),
                    center = Offset(c.x - sr * 0.35f, c.y - sr * 0.4f),
                    radius = sr * 1.5f
                ),
                radius = sr,
                center = c
            )

            // inner light: white at the top-left, so the glass has a lit side
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(c.x - sr * 0.30f, c.y - sr * 0.40f),
                    radius = sr * 0.9f
                ),
                radius = sr * 0.9f,
                center = Offset(c.x - sr * 0.30f, c.y - sr * 0.40f)
            )

            fun blob(angleDeg: Float, distance: Float, radius: Float, color: Color) {
                val a = Math.toRadians((angleDeg + driftDeg + energy * 40f).toDouble())
                val push = distance * sr * (1f + 0.34f * energy)
                val x = (c.x + push * Math.cos(a)).toFloat()
                val y = (c.y + push * Math.sin(a)).toFloat()
                // Drawn through the outer scope: the clip lives on the canvas,
                // so this stays inside the sphere.
                scope.orbBlob(Offset(x, y), radius * sr * (1f + 0.24f * energy), color)
            }

            blob(300f, 0.35f, 0.75f, Color(0xCCBCEFD9))
            blob(35f, 0.42f, 0.68f, Color(0xCCE8F7C0))
            blob(95f, 0.48f, 0.60f, Color(0xCCB4EEE2))
            blob(160f, 0.40f, 0.62f, Color(0xBFBBE4F2))
            blob(235f, 0.44f, 0.55f, Color(0xBFCBEBB8))
            blob(0f, 0f, 0.55f, Color(0xB3FFFFFF))

            // specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xF2FFFFFF), Color(0x00FFFFFF)),
                    center = Offset(c.x - sr * 0.33f, c.y - sr * 0.42f),
                    radius = sr * 0.55f
                ),
                radius = sr * 0.55f,
                center = Offset(c.x - sr * 0.33f, c.y - sr * 0.42f)
            )

            // rim, so the sphere keeps its edge against a light background
            drawCircle(
                color = Color.White.copy(alpha = 0.30f + 0.20f * energy),
                radius = sr - sr * 0.015f,
                center = c,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = sr * 0.03f)
            )
        }
    }
}

/** The sphere follows the turn: it is the only "who is speaking" indicator on the screen. */
private fun orbModeFor(voiceState: AssistantVoiceState, isMuted: Boolean): OrbMode = when {
    isMuted -> OrbMode.IDLE
    voiceState == AssistantVoiceState.LISTENING -> OrbMode.LISTENING
    voiceState == AssistantVoiceState.SPEAKING -> OrbMode.SPEAKING
    voiceState == AssistantVoiceState.THINKING -> OrbMode.THINKING
    else -> OrbMode.IDLE
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
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
    onPickLesson: () -> Unit
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
            Spacer(Modifier.height(10.dp))
            IridescentOrb(
                size = 236.dp,
                mode = OrbMode.IDLE,
                onTap = onMic
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (firstName.isBlank()) "Hello" else "Hello, $firstName",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 16.5.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ready to speak English?",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            // Two full-width tiles instead of the old pair of small squares: the
            // card is now a row, so the title gets a whole line and the tap target
            // is the full width of the page.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeTile(
                    icon = Icons.Default.Mic,
                    title = "Free conversation",
                    subtitle = "Talk about anything — I correct as you go",
                    meta = "A2 – B1",
                    selected = lesson == null,
                    onClick = onMic
                )
                ModeTile(
                    icon = Icons.Default.PlayArrow,
                    title = lesson?.title ?: "Guided lesson",
                    subtitle = if (lesson != null) {
                        "Following the plan · ${lesson.level}"
                    } else {
                        "Pick a lesson and I will teach it step by step"
                    },
                    meta = if (lesson != null) "${lesson.stepCount} steps" else "4 steps",
                    selected = lesson != null,
                    onClick = onPickLesson
                )
            }

            Spacer(Modifier.height(14.dp))
            LevelStrip(spokenMinutes = spokenMinutes, spokenCalls = spokenCalls)
            Spacer(Modifier.height(10.dp))
            Text(
                text = status,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
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

/**
 * One way into the call, as a full-width glass row.
 *
 * The previous pair of square cards forced two-line titles into half the page;
 * a row puts the icon, the promise and the badge on one baseline, which survives
 * long lesson titles and a 320 dp screen without ellipsing the part that matters.
 */
@Composable
private fun ModeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    meta: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (selected) Brand else Brand.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GlassFill else GlassFillSoft)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = if (selected) Brand.copy(alpha = 0.45f) else GlassEdge,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag("tutor_mode_tile")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = if (selected) 0.95f else 0.22f),
                                Fresh.copy(alpha = if (selected) 0.75f else 0.16f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else BrandDeep,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = meta,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) BrandDeep else Faint,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (selected) Brand else Faint,
                    modifier = Modifier.size(17.dp)
                )
            }
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
                fontSize = 13.sp,
                color = Muted
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$label  ·  $spokenMinutes min spoken",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.5.sp,
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
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fresh
                )
            }
            Spacer(Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Brand.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Brand)
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
    micLevel: StateFlow<Float>,
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
            LiveOrb(
                voiceState = voiceState,
                isMuted = isMuted,
                micLevel = micLevel,
                size = 112.dp
            )
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
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMuted) Muted else BrandDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    fontSize = 14.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            transcript.forEachIndexed { index, entry ->
                // Keyed by position and role: a new line slides up into place, an
                // old one never replays its entrance when the list is re-composed.
                key(index, entry.role) {
                    AppearIn {
                        when (entry.role) {
                            TutorCallEntry.Role.STUDENT -> StudentBubble(entry.text)
                            TutorCallEntry.Role.TUTOR -> TutorBubble(entry.text)
                            TutorCallEntry.Role.FIX -> entry.fix?.let {
                                CorrectionCard(fix = it, onHear = onHearFix)
                            }
                        }
                    }
                }
            }

            // The live correction is shown even before the model's reply lands, so
            // the student sees the fix while the words are still fresh.
            if (currentFix != null && transcript.none { it.role == TutorCallEntry.Role.FIX }) {
                AppearIn { CorrectionCard(fix = currentFix, onHear = onHearFix) }
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

/**
 * The live sphere, wrapped so the microphone energy is read here rather than in
 * the screen: at ~12 updates a second it must repaint one 96 dp circle and
 * nothing else.
 */
@Composable
private fun LiveOrb(
    voiceState: AssistantVoiceState,
    isMuted: Boolean,
    micLevel: StateFlow<Float>,
    size: androidx.compose.ui.unit.Dp
) {
    val level by micLevel.collectAsState()
    IridescentOrb(
        size = size,
        mode = orbModeFor(voiceState, isMuted),
        micLevel = level
    )
}

private fun statusColor(state: AssistantVoiceState, isMuted: Boolean): Color = when {
    isMuted -> Faint
    state == AssistantVoiceState.LISTENING -> Warm
    state == AssistantVoiceState.THINKING -> Color(0xFFE0A83C)
    else -> Brand
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Fade-and-rise for anything that arrives in the thread.
 *
 * A bubble that pops into place reads as a list updating; one that rises 8 dp
 * while it fades reads as someone speaking. The height is reserved from the
 * first frame (only alpha and offset animate), so the scroll does not jump as
 * each line lands.
 */
@Composable
private fun AppearIn(content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(animationSpec = tween(240)) +
            androidx.compose.animation.slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
    ) {
        content()
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
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
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
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
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
                Box(Modifier.size(6.dp).clip(CircleShape).background(Warm))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "BETTER",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = Warm
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Hear it  ▸",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Warm
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = "“${fix.corrected}”",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            if (fix.rule.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = fix.rule,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF8A6A2F)
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
                            .background(Brand.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  The one control
// ─────────────────────────────────────────────────────────────────────────────

/** The five faces of the one button, so the icon itself says what a tap will do. */
private enum class MicFace { IDLE, LISTENING, SPEAKING, THINKING, MUTED }

/**
 * The microphone, and nothing else.
 *
 * The rings are the tutor's state made visible — a slow bloom while it speaks,
 * a ring goes out while it listens, a dull circle when muted — so the student
 * reads whose turn it is without looking for a label. The glyph inside changes
 * with the face too: mic, level bars while the tutor talks, ellipsis while it
 * thinks, mic-off when muted.
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
        initialValue = if (state == AssistantVoiceState.SPEAKING) 1.05f else 1f,
        targetValue = if (state == AssistantVoiceState.SPEAKING) 1.15f else 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "mic_pulse"
    )
    val ringT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mic_ring"
    )
    val isLive = phase == CallPhase.LIVE || phase == CallPhase.CONNECTING

    val face = when {
        isMuted -> MicFace.MUTED
        !isLive -> MicFace.IDLE
        state == AssistantVoiceState.LISTENING -> MicFace.LISTENING
        state == AssistantVoiceState.SPEAKING -> MicFace.SPEAKING
        state == AssistantVoiceState.THINKING -> MicFace.THINKING
        else -> MicFace.IDLE
    }
    val solid = when (face) {
        MicFace.MUTED -> Faint
        MicFace.SPEAKING -> BrandDeep
        MicFace.THINKING -> Brand.copy(alpha = 0.55f)
        else -> Brand
    }
    val iconLabel = when (face) {
        MicFace.MUTED -> "Unmute"
        MicFace.SPEAKING -> "Interrupt the tutor"
        MicFace.THINKING -> "Tutor is thinking"
        MicFace.LISTENING -> "Listening"
        MicFace.IDLE -> "Speak"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Rings: they breathe while the tutor talks and send a ring out while
            // it listens, so the state is legible before the icon is read.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .clip(CircleShape)
                    .background(solid.copy(alpha = 0.14f))
            )
            if (face == MicFace.LISTENING) {
                val alpha = ((1f - ringT) * 0.45f).coerceIn(0f, 0.45f)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            val grow = 1f + 0.32f * ringT
                            scaleX = grow
                            scaleY = grow
                            this.alpha = alpha
                        }
                        .border(2.dp, Brand.copy(alpha = 0.55f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(solid.copy(alpha = 0.22f))
            )
            // The button centres its own content: the icon used to be wrapped in a
            // second, unsized Box, which parked it in the top-left of the circle.
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(solid)
                    .combinedClickable(
                        onClick = onMic,
                        onLongClick = onLongPress
                    )
                    .testTag("tutor_mic"),
                contentAlignment = Alignment.Center
            ) {
                // Crossfade, not a swap: the glyph the student is aiming at changes
                // under their thumb, and a hard cut makes that feel like a glitch.
                androidx.compose.animation.Crossfade(
                    targetState = face,
                    animationSpec = tween(220),
                    label = "mic_face"
                ) { current ->
                    Icon(
                        imageVector = when (current) {
                            MicFace.MUTED -> Icons.Default.MicOff
                            MicFace.SPEAKING -> Icons.Default.GraphicEq
                            MicFace.THINKING -> Icons.Default.MoreHoriz
                            else -> Icons.Default.Mic
                        },
                        contentDescription = iconLabel,
                        tint = Color.White,
                        modifier = Modifier.size(if (current == MicFace.THINKING) 30.dp else 28.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = caption,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            maxLines = 2
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
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = Ink
            )
            Text(
                text = summary.lessonTitle ?: summary.scenario,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.sp,
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
                    fontSize = 11.5.sp,
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
                                fontSize = 13.5.sp,
                                color = Muted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "“${fix.corrected}”",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            if (fix.rule.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = fix.rule,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.sp,
                                    color = Warm
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
                    fontSize = 11.5.sp,
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandDeep
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
                        fontSize = 14.5.sp,
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
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
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
                color = Brand,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("tutor_call_again")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Practise again",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 16.sp,
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
                        fontSize = 15.sp,
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
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
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
    startOnLessons: Boolean,
    onCall: Boolean,
    onSelectScenario: (String) -> Unit,
    onSelectLesson: (TutorLesson?) -> Unit,
    onEndCall: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLessons by remember(startOnLessons) { mutableStateOf(startOnLessons) }

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
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A topic is a conversation. A lesson has steps, so the tutor " +
                    "teaches one grammar point properly.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
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

            if (onCall) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEndCall) {
                        Text("End call", color = Warm, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
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
            .background(if (selected) Brand else Color(0x66FFFFFF))
            .border(1.dp, if (selected) Color.Transparent else GlassEdge, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
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
            .background(if (selected) Brand.copy(alpha = 0.12f) else Color(0x99FFFFFF))
            .border(1.dp, if (selected) Brand.copy(alpha = 0.35f) else GlassEdge, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = Ink
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandDeep
                )
            }
        }
    }
}

/** mm:ss, the way a call app counts. */
private fun formatCallTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
