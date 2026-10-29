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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
private val CardCream = Color(0xF7FFFDF8)       // the reference's off-white card

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
 * fields, a soft drop shadow underneath, a brand bloom that widens with the
 * voice, a specular highlight and a double rim.
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
        val pressLift = if (pressed) 1f else 0f

        // ── Shadow ───────────────────────────────────────────────────────────
        // The sphere is pastel on a pastel page, so it needs a real shadow to sit
        // ON the page rather than dissolve into it. This is a stacked, offset
        // shadow: a tight dark contact shadow right under the ball, then a wide
        // soft one, then a brand-tinted bloom. All of it is un-clipped, and none
        // of it animates as a ring — it only breathes with the voice.
        val lift = energy * 0.5f + pressLift * 0.6f
        // wide soft shadow
        scope.drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF123A30).copy(alpha = 0.26f + 0.06f * lift),
                    0.55f to Color(0xFF123A30).copy(alpha = 0.16f + 0.05f * lift),
                    1.00f to Color.Transparent
                ),
                center = Offset(c.x, c.y + sr * 0.30f),
                radius = r * 1.02f
            ),
            radius = r * 1.02f,
            center = Offset(c.x, c.y + sr * 0.30f)
        )
        // tight contact shadow, offset down: this is the one that reads as "solid"
        scope.drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF0F2E26).copy(alpha = 0.34f),
                    0.70f to Color(0xFF0F2E26).copy(alpha = 0.20f),
                    1.00f to Color.Transparent
                ),
                center = Offset(c.x, c.y + sr * 0.42f),
                radius = sr * 0.95f
            ),
            radius = sr * 0.95f,
            center = Offset(c.x, c.y + sr * 0.42f)
        )
        // brand bloom, so the light belongs to the app
        scope.drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Brand.copy(alpha = 0.20f + 0.24f * energy),
                    0.60f to Brand.copy(alpha = 0.07f),
                    1.00f to Color.Transparent
                ),
                center = Offset(c.x + sr * 0.06f, c.y + sr * 0.12f),
                radius = r * 0.98f
            ),
            radius = r * 0.98f,
            center = Offset(c.x + sr * 0.06f, c.y + sr * 0.12f)
        )

        // ── The sphere itself, masked to its own circle ──────────────────────
        // Modelled on the assistant-ui voice orb (assistant-ui.com/elements/orb):
        // a saturated liquid sphere, not a pastel bubble. The flow is built from
        // soft *elliptical* gradients — wide bands and thin streaks at different
        // angles — each one turning at its own speed, which is what reads as
        // liquid rather than as a handful of blobs. Compose has no shader here,
        // but with the mask, the vignette and the rim it holds up.
        val oval = Path().apply {
            addOval(Rect(center = c, radius = sr))
        }
        scope.clipPath(oval) {
            // base: lit from the top-left, deep sea-green at the far side
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF7FE9C6),
                        0.45f to Color(0xFF1FB28C),
                        0.80f to Color(0xFF0A7A63),
                        1.00f to Color(0xFF055146)
                    ),
                    center = Offset(c.x - sr * 0.40f, c.y - sr * 0.46f),
                    radius = sr * 1.6f
                ),
                radius = sr,
                center = c
            )

            // Wide bands
            orbBand(c, sr, -0.35f, -0.48f, 0.95f, 0.55f, -30f + driftDeg * 0.35f, Color(0xFFE6FFF3), 0.85f)
            orbBand(c, sr, 0.42f, -0.10f, 0.80f, 0.42f, 25f - driftDeg * 0.28f, Color(0xFFC9F96A), 0.55f)
            orbBand(c, sr, 0.10f, 0.42f, 0.90f, 0.50f, -15f + driftDeg * 0.42f, Color(0xFF3FE0D0), 0.60f)
            orbBand(c, sr, -0.55f, 0.30f, 0.70f, 0.40f, 40f - driftDeg * 0.30f, Color(0xFF5FD8F2), 0.50f)
            orbBand(c, sr, 0.30f, 0.55f, 0.65f, 0.34f, -40f + driftDeg * 0.50f, Color(0xFF8BFFD0), 0.55f)
            orbBand(c, sr, 0.60f, -0.45f, 0.55f, 0.30f, 15f - driftDeg * 0.45f, Color.White, 0.35f)

            // Thin streaks: the marble lines that make it look liquid
            orbBand(c, sr, -0.10f, -0.10f, 1.25f, 0.16f, -22f + driftDeg * 0.22f, Color.White, 0.42f)
            orbBand(c, sr, -0.15f, 0.20f, 1.10f, 0.14f, -28f - driftDeg * 0.26f, Color(0xFFD9FFF0), 0.40f)
            orbBand(c, sr, 0.05f, -0.30f, 1.05f, 0.10f, -20f + driftDeg * 0.34f, Color(0xFF0B4C41), 0.18f)

            // depth: dark at the rim, clear in the middle
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.00f to Color(0xFF043B32).copy(alpha = 0.30f)
                    ),
                    center = Offset(c.x - sr * 0.10f, c.y - sr * 0.14f),
                    radius = sr * 1.28f
                ),
                radius = sr * 1.28f,
                center = Offset(c.x - sr * 0.10f, c.y - sr * 0.14f)
            )

            // specular: the small hot spot that says "glass"
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.95f),
                        0.45f to Color.White.copy(alpha = 0.30f),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(c.x - sr * 0.34f, c.y - sr * 0.44f),
                    radius = sr * 0.40f
                ),
                radius = sr * 0.40f,
                center = Offset(c.x - sr * 0.34f, c.y - sr * 0.44f)
            )

            // rim light: bright toward the lower right, thin at the top left
            drawCircle(
                brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.55f),
                        0.25f to Color.White.copy(alpha = 0.12f),
                        0.55f to Color.White.copy(alpha = 0.70f),
                        0.75f to Color.White.copy(alpha = 0.35f),
                        1.00f to Color.White.copy(alpha = 0.55f)
                    ),
                    center = c
                ),
                radius = sr - sr * 0.010f,
                center = c,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = sr * 0.020f)
            )
        }

        // hairline edge so the ball keeps its outline on a pale page
        scope.drawCircle(
            color = Color(0xFF0B7A60).copy(alpha = 0.30f),
            radius = sr,
            center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sr * 0.010f)
        )
    }
}

/**
 * One flowing band inside the orb.
 *
 * A radial gradient drawn through a rotate + scale transform becomes an ellipse,
 * which is what turns "a few coloured balls" into flowing liquid. The rotation
 * carries the drift, so every band turns at its own speed.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.orbBand(
    center: Offset,
    sphereRadius: Float,
    offsetX: Float,
    offsetY: Float,
    radiusX: Float,
    radiusY: Float,
    rotation: Float,
    color: Color,
    alpha: Float
) {
    val bandCenter = Offset(
        center.x + offsetX * sphereRadius,
        center.y + offsetY * sphereRadius
    )
    androidx.compose.ui.graphics.drawscope.withTransform({
        rotate(rotation, bandCenter)
        scale(radiusX, radiusY, bandCenter)
    }) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to color.copy(alpha = alpha),
                    0.55f to color.copy(alpha = alpha * 0.45f),
                    1.00f to Color.Transparent
                ),
                center = bandCenter,
                radius = sphereRadius
            ),
            radius = sphereRadius,
            center = bandCenter
        )
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

        // No scroll on this page. The sphere is measured from the room that is
        // actually left and every gap is a weight, so the same layout sits whole
        // on a 640 dp phone and on a tablet instead of being cut off at the mic.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            val orbSize = (minOf(maxWidth * 0.62f, maxHeight * 0.42f))
                .coerceIn(120.dp, 250.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))
                IridescentOrb(
                    size = orbSize,
                    mode = OrbMode.IDLE,
                    onTap = onMic
                )
                Spacer(Modifier.weight(0.45f))
                Text(
                    text = if (firstName.isBlank()) "Hello" else "Hello, $firstName",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 16.sp,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ready to practice English with me?",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(0.55f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModeCard(
                        icon = Icons.Default.Chat,
                        tint = Brand,
                        title = "Free conversation",
                        subtitle = "Speak freely and let Miss Emma correct you as you talk",
                        onClick = onMic,
                        modifier = Modifier.weight(1f)
                    )
                    ModeCard(
                        icon = Icons.Default.PlayArrow,
                        tint = Warm,
                        title = lesson?.title ?: "Guided lesson",
                        subtitle = lesson?.goal
                            ?: "Pick a lesson and I will teach one point, step by step",
                        onClick = onPickLesson,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.weight(0.3f))
                Text(
                    text = status,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = Faint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
            }
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
 * One way into the call — two of these sit under the greeting.
 *
 * Taken from the reference the user supplied: a warm off-white card, a tinted
 * icon chip, a small arrow in the corner, then a bold promise and one line of
 * explanation. It is deliberately not glass: two translucent panels side by side
 * on a pastel page read as one washed-out block.
 */
@Composable
private fun ModeCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardCream)
            .border(1.dp, Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("tutor_mode_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.CallMade,
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = Muted,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
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
        initialValue = 1f,
        targetValue = if (state == AssistantVoiceState.SPEAKING) 1.06f else 1.025f,
        animationSpec = infiniteRepeatable(animation = tween(1_500), repeatMode = RepeatMode.Reverse),
        label = "mic_pulse"
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
    // A squircle, not a circle: a solid brand gradient block with a coloured drop
    // shadow reads as a button on this pastel page, where concentric rings read as
    // a 2018 voice recorder.
    val gradient = when (face) {
        MicFace.MUTED -> listOf(Color(0xFFC3CBD5), Color(0xFF9AA6B2))
        MicFace.SPEAKING -> listOf(Color(0xFF0FA97E), Color(0xFF0B6E5C))
        MicFace.THINKING -> listOf(Color(0xFF3FC79A), Color(0xFF0F8A6B))
        else -> listOf(Color(0xFF12B981), Color(0xFF047857))
    }
    val glow = when (face) {
        MicFace.MUTED -> Color(0xFF94A3B8)
        MicFace.SPEAKING -> Color(0xFF0B6E5C)
        MicFace.THINKING -> Color(0xFF0F8A6B)
        else -> Brand
    }
    val shape: Shape = CircleShape
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
            .padding(bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // The bed: a wide, faint squircle that only exists to give the button
            // a place to sit. It is not a ring and it never sweeps.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(glow.copy(alpha = 0.10f))
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = shape,
                        ambientColor = glow.copy(alpha = 0.75f),
                        spotColor = glow.copy(alpha = 0.85f)
                    )
                    .clip(shape)
                    .background(Brush.verticalGradient(gradient))
                    .combinedClickable(
                        onClick = onMic,
                        onLongClick = onLongPress
                    )
                    .testTag("tutor_mic"),
                contentAlignment = Alignment.Center
            ) {
                // gloss: a soft top-lit falloff over the whole circle, which is
                // what makes a flat gradient read as a physical, lit button
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = 0.34f),
                                    0.45f to Color.White.copy(alpha = 0.06f),
                                    1.00f to Color.Transparent
                                )
                            )
                        )
                )
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
                        modifier = Modifier.size(if (current == MicFace.THINKING) 30.dp else 27.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = caption,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
