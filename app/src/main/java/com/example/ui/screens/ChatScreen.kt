package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.local.ChatAttachmentStore
import com.example.data.model.ChatAttachment
import com.example.data.model.ChatAttachmentDraft
import com.example.data.model.ChatHelpRequest
import com.example.data.model.ChatMessage
import com.example.data.model.ChatPeer
import com.example.data.model.ChatPeerProfile
import com.example.ui.components.ExpBadgeLadder
import com.example.ui.components.ExpHeroCard
import com.example.ui.components.RatingCelebrationOverlay
import com.example.data.model.ChatRateState
import com.example.data.model.ChatRatingSummary
import com.example.data.model.chatRatingTagLabels
import com.example.data.model.ChatReply
import com.example.data.model.ChatThread
import com.example.ui.components.TheStudentAppBackground
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import com.example.viewmodel.ChatUiState
import com.example.viewmodel.TheStudentViewModel
import android.media.MediaPlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Palette / helpers
// ─────────────────────────────────────────────────────────────────────────────
private val ChatInk = Color(0xFF0B1220)
private val ChatMuted = Color(0xFF64748B)
private val ChatFaint = Color(0xFF94A3B8)
private val ChatHairline = Color(0xFFE8EDF3)
private val ChatOnline = Color(0xFF22C55E)
private val ChatError = Color(0xFFDC2626)

/** Outgoing bubble: pure black, the text and ticks carry the contrast. */
private val ChatBubbleOutgoing = Color(0xFF000000)

/** Markdown palette for a bubble: white-ish inside the black bubble, ink on white. */
private fun chatMarkdownColors(isMine: Boolean) = ChatMarkdownColors(
    text = if (isMine) Color.White else ChatInk,
    muted = if (isMine) Color.White.copy(alpha = 0.72f) else ChatMuted,
    codeBackground = if (isMine) Color.White.copy(alpha = 0.16f) else Color(0xFFEFF3F8),
    codeText = if (isMine) Color(0xFFDFF7E6) else Color(0xFF0F172A),
    link = if (isMine) Color(0xFF9BE7B8) else Color(0xFF047857),
    // Greener than the brand emerald so it reads as a search hit on both bubbles.
    highlightBackground = Color(0xFF34D399),
    highlightText = Color(0xFF06281A)
)

/** Soft elevation-less card used by the attachment sheet. */
private val ChatSheetCard = Color(0xFFF6F8FA)

/** Stable avatar tint per student id, so the initials avatar never flickers. */
private val ChatAvatarPalette = listOf(
    Color(0xFF047857), Color(0xFF0F766E), Color(0xFF065F46),
    Color(0xFF15803D), Color(0xFF0369A1), Color(0xFF4338CA),
    Color(0xFF7C3AED), Color(0xFFBE185D)
)

private fun avatarColorFor(id: Int): Color =
    ChatAvatarPalette[Math.floorMod(id, ChatAvatarPalette.size)]

private fun formatClock(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSeconds * 1000))
}

/** "9:45" today, "Yesterday", otherwise "12 Mar". */
private fun formatThreadStamp(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = epochSeconds * 1000 }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return formatClock(epochSeconds)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val wasYesterday = yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (wasYesterday) return "Yesterday"

    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochSeconds * 1000))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chat tab
// ─────────────────────────────────────────────────────────────────────────────
/**
 * The chat tab.
 *
 * A thin wrapper so the rating celebration can sit *over* everything the tab
 * draws — list, conversation or profile — without any of those screens having
 * to know about it.
 */
@Composable
fun ChatScreen(
    viewModel: TheStudentViewModel,
    modifier: Modifier = Modifier
) {
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        ChatScreenBody(viewModel = viewModel, modifier = Modifier.fillMaxSize())

        // "You've been rated": stars, the score and who gave it — then it leaves
        // on its own. It never covers a button for longer than it takes to read.
        val celebration = chatState.celebratingRating
        if (celebration != null) {
            RatingCelebrationOverlay(
                raterName = celebration.raterName,
                stars = celebration.stars,
                onFinished = { viewModel.dismissRatingCelebration() }
            )
        }
    }
}

@Composable
private fun ChatScreenBody(
    viewModel: TheStudentViewModel,
    modifier: Modifier = Modifier
) {
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()

    // The directory polls while the tab is visible; presence heartbeat runs too.
    LaunchedEffect(Unit) { viewModel.startChatSync() }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopChatSync() }
    }

    val viewedProfile = chatState.viewedProfile
    if (viewedProfile != null) {
        BackHandler { viewModel.closeChatProfile() }
        ChatPeerProfileScreen(
            profile = viewedProfile,
            isLoading = chatState.isLoadingProfile,
            onBack = { viewModel.closeChatProfile() },
            onMessage = {
                // Closing the profile first keeps the conversation's own back
                // gesture going to the chat list, not to this screen.
                viewModel.closeChatProfile()
                viewModel.openChat(viewedProfile.peer)
            },
            onAskForHelp = { viewModel.openChatForHelp(viewedProfile.peer) },
            modifier = modifier
        )
        return
    }

    val activePeer = chatState.activePeer
    if (activePeer != null) {
        BackHandler { viewModel.closeChat() }
        ChatConversationScreen(
            state = chatState,
            onBack = { viewModel.closeChat() },
            onSend = { viewModel.sendChatMessage(it) },
            onTyping = { viewModel.notifyChatTyping() },
            onAcknowledgeSafety = { viewModel.acknowledgeChatSafetyNotice() },
            onClearChat = { viewModel.clearActiveChat() },
            onDismissError = { viewModel.dismissChatError() },
            onPickAttachment = { uri -> viewModel.stageChatAttachment(uri) },
            onClearAttachment = { viewModel.clearStagedChatAttachment() },
            onDeleteMessage = { message -> viewModel.deleteChatMessage(message) },
            onReply = { message -> viewModel.setChatReply(message) },
            onClearReply = { viewModel.clearChatReply() },
            onStartVoiceNote = { viewModel.startVoiceNote() },
            onSendVoiceNote = { viewModel.sendVoiceNote() },
            onCancelVoiceNote = { viewModel.cancelVoiceNote() },
            onStopTyping = { viewModel.stopChatTyping() },
            onOpenProfile = { viewModel.openChatProfile(it) },
            onAcceptHelp = { viewModel.acceptHelpRequest() },
            onCompleteHelp = { viewModel.completeHelpRequest() },
            onCancelHelp = { viewModel.cancelHelpRequest() },
            onOpenIntent = { viewModel.openChatIntent() },
            onOpenRating = { viewModel.openRatingCard() },
            onTrackQuestion = { viewModel.trackQuestionAsHelp() },
            onDismissQuestionOffer = { viewModel.dismissQuestionOffer() },
            onAskAgain = { viewModel.askForHelp() },
            modifier = modifier
        )

        // "What's this chat about?" — shown once, for a conversation with no
        // history. It sits above the thread so the composer stays visible.
        val intentPeer = chatState.chatIntentPeer
        if (intentPeer != null) {
            BackHandler { viewModel.dismissChatIntent() }
            ChatIntentSheet(
                peer = intentPeer,
                suggestedSubject = chatState.mySubjects.firstOrNull().orEmpty(),
                currentPurpose = chatState.chatPurpose,
                isBusy = chatState.isHelpBusy || chatState.isAskingHelp,
                onChoose = { purpose, subject ->
                    viewModel.applyChatPurpose(purpose, subject)
                },
                onDismiss = { viewModel.dismissChatIntent() }
            )
        }

        // The rating card: opened by itself when a help request is closed, and
        // from the ⋯ menu afterwards. It floats over the thread, never gates the
        // composer, and "Not now" leaves without a fuss.
        val rate = chatState.rateState
        if (rate != null) {
            BackHandler { viewModel.dismissRatingCard() }
            RatingCardOverlay(
                rate = rate,
                peer = activePeer,
                isBusy = chatState.isRatingBusy,
                onDismiss = { viewModel.dismissRatingCard() },
                onSubmit = { stars, tags, note -> viewModel.submitRating(stars, tags, note) }
            )
        }
        return
    }

    ChatDirectoryScreen(
        state = chatState,
        onOpenChat = { viewModel.openChat(it) },
        onOpenProfile = { viewModel.openChatProfile(it) },
        onRefresh = { viewModel.refreshChatDirectoryNow() },
        onDismissError = { viewModel.dismissChatError() },
        onAcknowledgeSafety = { viewModel.acknowledgeChatSafetyNotice() },
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Help requests (intent sheet + the card in the thread)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The floating help bubble.
 *
 * A small circle that floats over the conversation, parked above the microphone
 * by default. It is dragged with a finger and remembers where it was left for
 * that conversation, so it never permanently covers a message.
 *
 * The circle stays 44 dp — small, out of the way — and carries its state in a
 * badge on the rim. The words come out on tap, in a compact card above it:
 *
 *  • amber `?`  — you just sent a question, offer to track it
 *  • amber dot  — request sent, waiting for them
 *  • green tick — they accepted, close it when solved
 *  • green `+`  — completed before, ask again
 *  • no badge   — nothing to do; the bubble fades back so it stops shouting
 */
@Composable
private fun FloatingHelpBubble(
    state: ChatUiState,
    peerName: String,
    onOpenPurpose: () -> Unit,
    onAskForHelp: () -> Unit,
    onAskAgain: () -> Unit,
    onAcceptHelp: () -> Unit,
    onCompleteHelp: () -> Unit,
    onCancelHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val request = state.helpRequest
    val firstName = peerName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "them" }

    // Badge + wording for the current situation. One place, so the badge and the
    // card can never disagree.
    val badge: String
    val headline: String
    val body: String
    when {
        state.questionOfferVisible -> {
            badge = "?"
            headline = "Track this question?"
            body = "$firstName gets credit, and you can rate the help after."
        }
        request?.status == "pending" -> {
            badge = "·"
            headline = if (request.isSeeker) "Waiting for $firstName" else "$firstName needs help"
            body = if (request.isSeeker) "Sent — they haven't answered yet." else "Tap to open the request."
        }
        request?.status == "accepted" -> {
            badge = "✓"
            headline = if (request.isSeeker) "Helped?" else "You're helping $firstName"
            body = if (request.isSeeker) "Close the request when your question is solved." else "They close it when it's solved."
        }
        state.helpCompletedWithPeer > 0 -> {
            badge = "+"
            headline = "Helped ${state.helpCompletedWithPeer} times"
            body = "Ask $firstName again, or change what this chat is about."
        }
        else -> {
            badge = ""
            headline = "Need help with something?"
            body = "Tap to tell $firstName, or just keep chatting."
        }
    }

    var expanded by remember(state.activePeer?.id) { mutableStateOf(false) }
    // Primitive state, read only in the layout phase: dragging repositions the
    // bubble without recomposing anything, which is what made it feel like it
    // was trailing the finger.
    var offsetX by remember(state.activePeer?.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(state.activePeer?.id) { mutableFloatStateOf(0f) }
    // Which side the bubble is parked on. Only changes on release, so the card
    // can sit on the inside of the screen without recomposing mid-drag.
    var parkedLeft by remember(state.activePeer?.id) { mutableStateOf(false) }
    var cardWidthPx by remember(state.activePeer?.id) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    // The snap-to-edge animation, so a new drag can cancel it at once instead of
    // fighting the finger for a frame.
    var snapJob by remember(state.activePeer?.id) { mutableStateOf<Job?>(null) }

    val bubblePx = with(LocalDensity.current) { 44.dp.toPx() }
    val edgePadPx = with(LocalDensity.current) { 13.dp.toPx() }
    val gapPx = with(LocalDensity.current) { 8.dp.toPx() }
    val bottomPadPx = with(LocalDensity.current) { 12.dp.toPx() }

    // While there is something to act on the bubble breathes; idle bubbles sit
    // still so the screen is not permanently animated.
    val isActive = badge.isNotEmpty()
    val pulse = if (isActive) {
        val transition = rememberInfiniteTransition(label = "help_bubble_pulse")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "help_bubble_scale"
        )
        scale
    } else {
        1f
    }

    // Where the circle actually sits in the window. The drag needs it because
    // the circle moves as it is dragged, so its own coordinates shift under the
    // finger; adding this back gives the true finger position every event.
    var bubbleOrigin by remember(state.activePeer?.id) { mutableStateOf(Offset.Zero) }

    // The tap-anywhere-else catcher only exists while the card is open. Even an
    // inert `pointerInput` is still a hit target, and a full-screen hit target
    // sitting over the thread takes every touch away from the message list —
    // that is what froze scrolling.
    val dismissLayer = if (expanded) {
        Modifier.pointerInput("dismiss") {
            detectTapGestures(onTap = { expanded = false })
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .then(dismissLayer)
    ) {
        // ── the card, anchored to the circle ───────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            // Scaled from the corner nearest the circle instead of animating
            // the height: a height animation measures the card a little short
            // for a frame or two, and the elevated Surface's shadow then gets
            // squared off at the bottom corners.
            enter = fadeIn(tween(140)) +
                scaleIn(
                    initialScale = 0.90f,
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(170)
                ),
            exit = fadeOut(tween(110)) +
                scaleOut(
                    targetScale = 0.90f,
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(120)
                ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 13.dp, bottom = 12.dp)
                .graphicsLayer {
                    // Sits just above the circle. When the circle is parked on
                    // the left, the card shifts so it opens inwards instead of
                    // hanging off the edge of the screen.
                    val cardWidth = if (cardWidthPx > 0f) cardWidthPx else bubblePx
                    val maxShift = (containerSize.width - edgePadPx * 2f - cardWidth)
                        .coerceAtLeast(0f)
                    translationX = if (parkedLeft) {
                        (offsetX - bubblePx + cardWidth).coerceIn(-maxShift, 0f)
                    } else {
                        0f
                    }
                    translationY = -(bubblePx + gapPx) + offsetY
                }
        ) {
            HelpBubbleCard(
                headline = headline,
                body = body,
                isSeeker = request?.isSeeker ?: true,
                status = request?.status,
                canAskAgain = state.helpCompletedWithPeer > 0,
                isBusy = state.isAskingHelp || state.isHelpBusy,
                onPrimary = {
                    expanded = false
                    when {
                        state.questionOfferVisible -> onAskForHelp()
                        // The other student's request: accept it right here, so
                        // the card is not a dead end pointing at another card.
                        request?.status == "pending" && !request.isSeeker -> onAcceptHelp()
                        request?.status == "accepted" && request.isSeeker -> onCompleteHelp()
                        state.helpCompletedWithPeer > 0 -> onAskAgain()
                        request == null -> onAskForHelp()
                        else -> onOpenPurpose()
                    }
                },
                onSecondary = {
                    expanded = false
                    when {
                        // Both a waiting and an accepted request can be cancelled
                        // by the student who asked — this is the button that used
                        // to do nothing.
                        request != null && request.isSeeker &&
                            (request.status == "pending" || request.status == "accepted") ->
                            onCancelHelp()
                        else -> onOpenPurpose()
                    }
                },
                modifier = Modifier.onSizeChanged { cardWidthPx = it.width.toFloat() }
            )
        }

        // ── the circle itself ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 13.dp, bottom = 12.dp)
                // Pure draw-phase: following the finger never re-measures the
                // thread behind it, so nothing else can flicker.
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    scaleX = pulse
                    scaleY = pulse
                }
                .size(44.dp)
                .testTag("chat_help_bubble")
                .onGloballyPositioned { coordinates ->
                    bubbleOrigin = coordinates.positionInRoot()
                }
                .pointerInput(state.activePeer?.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        snapJob?.cancel()
                        down.consume()

                        // Finger and circle position in *window* coordinates:
                        // stable even though the circle itself is moving, so the
                        // movement is measured once instead of twice.
                        val startFinger = down.position + bubbleOrigin
                        val startX = offsetX
                        val startY = offsetY
                        var dragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()

                            val finger = change.position + bubbleOrigin
                            val dx = finger.x - startFinger.x
                            val dy = finger.y - startFinger.y
                            if (!dragging &&
                                (dx * dx + dy * dy) >
                                viewConfiguration.touchSlop * viewConfiguration.touchSlop
                            ) {
                                dragging = true
                                expanded = false
                            }
                            if (dragging) {
                                val maxLeft = (containerSize.width - bubblePx - edgePadPx * 2f)
                                    .coerceAtLeast(0f)
                                val maxUp = (containerSize.height - bubblePx - bottomPadPx - edgePadPx)
                                    .coerceAtLeast(0f)
                                offsetX = (startX + dx).coerceIn(-maxLeft, 0f)
                                offsetY = (startY + dy).coerceIn(-maxUp, 0f)
                            }
                            if (!change.pressed) break
                        }
                        if (!dragging) {
                            // A press that never really moved opens the card.
                            expanded = !expanded
                            return@awaitEachGesture
                        }
                        // Chat-head behaviour: it never rests in the middle of the
                        // conversation. On release it slides to whichever side it
                        // was dropped nearer to, at the height it was dropped at.
                        val maxLeft = (containerSize.width - bubblePx - edgePadPx * 2f)
                            .coerceAtLeast(0f)
                        val from = offsetX
                        val targetX = if (from > -maxLeft / 2f) 0f else -maxLeft
                        parkedLeft = targetX < -maxLeft / 2f
                        if (targetX != from) {
                            snapJob = scope.launch {
                                val startNanos = withFrameNanos { it }
                                while (true) {
                                    val done = withFrameNanos { now ->
                                        // 200 ms eased out: long enough to read
                                        // as a movement, short enough that it
                                        // never feels sticky.
                                        val t = ((now - startNanos) / 200_000_000f)
                                            .coerceIn(0f, 1f)
                                        val eased = 1f - (1f - t) * (1f - t)
                                        offsetX = from + (targetX - from) * eased
                                        t >= 1f
                                    }
                                    if (done) break
                                }
                            }
                        }
                    }
                }
                // The gesture is on the circle, so the screen reader needs to be
                // told this circle is a button.
                .semantics {
                    contentDescription = "Chat help"
                    onClick(label = "Open help options") {
                        expanded = !expanded
                        true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(10.dp, CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF10B981), Color(0xFF059669))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Chat options",
                        tint = Color.White.copy(alpha = if (isActive) 1f else 0.82f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Badge on the rim: `?` offer, `·` waiting, `✓` accepted,
                // `+` ask again. Empty when there is nothing to do.
                if (badge.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(17.dp)
                            .shadow(3.dp, CircleShape)
                            .background(
                                when (badge) {
                                    "?" , "·" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

/** The compact card that opens above the bubble. Two actions at most. */
@Composable
private fun HelpBubbleCard(
    headline: String,
    body: String,
    isSeeker: Boolean,
    status: String?,
    canAskAgain: Boolean,
    isBusy: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryLabel = when {
        status == "pending" && !isSeeker -> "I can help"
        status == "pending" -> "Change"
        status == "accepted" && isSeeker -> "This helped — close"
        status == "accepted" -> "Open chat"
        canAskAgain -> "Ask again"
        else -> "Ask for help"
    }
    val secondaryLabel = when {
        // Cancel is reachable from the bubble too — that was the button that
        // looked dead when it lived only inside the thread card.
        status == "pending" && isSeeker -> "Cancel request"
        status == "accepted" && isSeeker -> "Cancel request"
        else -> "What's this chat?"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = modifier
            .widthIn(max = 250.dp)
            .testTag("chat_help_bubble_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = headline,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = ChatInk
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = ChatMuted
            )
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { if (!isBusy) onPrimary() },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("chat_help_bubble_primary")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 13.dp)
                    ) {
                        Text(
                            text = if (isBusy) "…" else primaryLabel,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    onClick = onSecondary,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.dp, ChatHairline),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 13.dp)
                    ) {
                        Text(
                            text = secondaryLabel,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChatMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** One row of the purpose sheet. */
private data class PurposeOption(
    val code: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * The rating card.
 *
 * Five stars, up to three tags from a fixed list, and an optional note of at
 * most forty characters that only the rated student ever reads. It floats over
 * the conversation rather than taking it over: "Not now" costs nothing, the
 * composer is never blocked, and the stars already picked survive a failure.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingCardOverlay(
    rate: ChatRateState,
    peer: ChatPeer,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, List<String>, String) -> Unit
) {
    val existing = rate.existing
    var stars by remember(rate.subjectCode) { mutableStateOf(existing?.stars ?: 0) }
    var tags by remember(rate.subjectCode) { mutableStateOf(existing?.tags?.toSet() ?: emptySet()) }
    var note by remember(rate.subjectCode) { mutableStateOf(existing?.note.orEmpty()) }

    val firstName = peer.name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "them" }
    val canSend = stars > 0 && !isBusy

    Box(modifier = Modifier.fillMaxSize()) {
        // Tapping the scrim is "Not now" — the same weight as the button, so a
        // student never feels trapped in the card.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220).copy(alpha = 0.34f))
                .clickable(onClick = onDismiss)
        )

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 20.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 22.dp)
                .fillMaxWidth()
                .testTag("chat_rating_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChatAvatar(
                        peer = peer,
                        size = 42.dp,
                        showPresence = false,
                        ringColor = Color.White
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existing != null) {
                                "Update your rating?"
                            } else {
                                "How was $firstName's help?"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChatInk
                        )
                        Text(
                            text = rate.subjectLine + " · " +
                                if (rate.isTalk) "after a long chat" else "help closed",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            color = ChatMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── stars ────────────────────────────────────────────────────
                // Warm yellow, a soft disc behind each chosen star and a small
                // pop as it fills — a rating should feel like a treat, not a
                // form field.
                val starYellow = Color(0xFFFBBF24)
                val starEmpty = Color(0xFFE4E9F0)
                val starWords = listOf(
                    "", "Not great", "Okay", "Good", "Very good", "Excellent"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_rating_stars"),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { index ->
                        val filled = index <= stars
                        val pop by animateFloatAsState(
                            targetValue = if (filled) 1f else 0.88f,
                            animationSpec = tween(140),
                            label = "rating_star_$index"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) starYellow.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = !isBusy) { stars = index }
                                .testTag("chat_rating_star_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = if (index == 1) "1 star" else "$index stars",
                                tint = if (filled) starYellow else starEmpty,
                                modifier = Modifier
                                    .size(37.dp)
                                    .graphicsLayer {
                                        scaleX = pop
                                        scaleY = pop
                                    }
                            )
                        }
                    }
                }

                Text(
                    text = starWords.getOrElse(stars) { "" }
                        .ifBlank { "Tap a star" },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stars == 0) ChatMuted else Color(0xFFB45309),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )

                Spacer(Modifier.height(14.dp))

                // ── tags (at most three) ─────────────────────────────────────
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chatRatingTagLabels.forEach { (slug, label) ->
                        val selected = slug in tags
                        Surface(
                            onClick = {
                                tags = when {
                                    selected -> tags - slug
                                    tags.size >= rate.maxTags -> tags
                                    else -> tags + slug
                                }
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(50),
                            color = if (selected) Color(0xFFE9F8F0) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (selected) EmeraldPrimary else ChatHairline
                            )
                        ) {
                            Text(
                                text = label,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) EmeraldDark else ChatMuted,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── the private note ────────────────────────────────────────
                OutlinedTextField(
                    value = note,
                    onValueChange = { typed ->
                        if (typed.length <= rate.maxNote) note = typed
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_rating_note"),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        color = ChatInk
                    ),
                    placeholder = {
                        Text(
                            text = "Private note (optional)",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            color = ChatMuted
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "${note.length}/${rate.maxNote}",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            color = ChatMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ChatHairline,
                        focusedContainerColor = Color(0xFFF6F8FA),
                        unfocusedContainerColor = Color(0xFFF6F8FA)
                    )
                )

                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { if (canSend) onSubmit(stars, tags.toList(), note.trim()) },
                        enabled = canSend,
                        shape = RoundedCornerShape(50),
                        color = if (canSend) EmeraldPrimary else Color(0xFFD1FAE5),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("chat_rating_send")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when {
                                    isBusy -> "Sending…"
                                    existing != null -> "Update rating"
                                    else -> "Send rating"
                                },
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canSend) Color.White else EmeraldDark
                            )
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("chat_rating_dismiss")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            Text(
                                text = "Not now",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChatMuted
                            )
                        }
                    }
                }

            }
        }
    }
}

/**
 * The purpose sheet — now three rows instead of four checkboxes.
 *
 * Opening it is always a deliberate act (the chip, or the ⋯ menu); it never
 * appears by itself, so the keyboard is never taken away from a student who is
 * mid-sentence. Choosing "help" also files the request, so there is no second
 * confirmation step.
 */
@Composable
private fun ChatIntentSheet(
    peer: ChatPeer,
    suggestedSubject: String,
    currentPurpose: String,
    isBusy: Boolean,
    onChoose: (purpose: String, subject: String) -> Unit,
    onDismiss: () -> Unit
) {
    var choice by remember(currentPurpose) {
        mutableStateOf(
            when (currentPurpose) {
                "help", "talk", "notes" -> currentPurpose
                else -> ""
            }
        )
    }
    var subject by remember(suggestedSubject) { mutableStateOf(suggestedSubject) }

    val options = listOf(
        PurposeOption(
            code = "help",
            title = "I need help with something",
            subtitle = "Tracked, so they can be rated for it",
            icon = Icons.Default.Forum
        ),
        PurposeOption(
            code = "talk",
            title = "Just talking",
            subtitle = "No tracking, no ratings",
            icon = Icons.Default.EmojiEmotions
        ),
        PurposeOption(
            code = "notes",
            title = "Sharing notes or papers",
            subtitle = "Files and past papers",
            icon = Icons.Default.AttachFile
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220).copy(alpha = 0.34f))
                .clickable(onClick = onDismiss)
        )

        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 18.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag("chat_intent_sheet")
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .background(ChatHairline, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "What's this chat about?",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ChatInk
                )
                Spacer(Modifier.height(12.dp))

                options.forEach { option ->
                    val isSelected = choice == option.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color(0xFFE9F8F0) else Color(0xFFF6F8FA)
                            )
                            .clickable { choice = option.code }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                            .testTag("chat_intent_${option.code}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isSelected) Color(0xFFD1FAE5) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) EmeraldDark else ChatMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = ChatInk
                            )
                            Text(
                                text = option.subtitle,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 10.5.sp,
                                color = ChatFaint
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (choice == "help") {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it.take(80) },
                        label = { Text("Subject") },
                        placeholder = { Text("e.g. Science for Technology") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_intent_subject")
                    )
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = {
                        if (choice.isNotBlank() && !isBusy) {
                            onChoose(choice, if (choice == "help") subject.trim() else "")
                        } else {
                            onDismiss()
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = if (choice.isNotBlank() && !isBusy) EmeraldPrimary else Color(0xFFCBD5E1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("chat_intent_start")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                isBusy -> "Saving…"
                                choice == "help" -> "Save & ask for help"
                                choice.isBlank() -> "Choose one"
                                else -> "Save"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Not now",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatMuted
                    )
                }
            }
        }
    }
}

/**
 * The offer that appears **after** a message that reads like a question.
 *
 * Deliberately small and skippable: the message has already been sent, so the
 * only thing asked is whether the student wants this tracked as a help request.
 * "Not now" is remembered for the pair, forever.
 */
@Composable
private fun QuestionOfferCard(
    peerName: String,
    isBusy: Boolean,
    onTrack: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstName = peerName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "they" }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp)
            .testTag("chat_question_offer")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "This looks like a question for $firstName",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatInk
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Track it so $firstName gets credit for the help — and you can rate them after.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = ChatMuted
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { if (!isBusy) onTrack() },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("chat_question_track")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (isBusy) "Sending…" else "Ask for help",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.dp, ChatHairline),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("chat_question_dismiss")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Not now",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChatMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * The request card at the top of a conversation.
 *
 * Everything it offers is decided by the server flags (`canAccept`,
 * `canClose`, `canCancel`) — the app never invites an action the API refuses.
 */
@Composable
private fun HelpRequestCard(
    request: ChatHelpRequest,
    peerName: String,
    completedCount: Int,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onAskAgain: () -> Unit
) {
    val firstName = peerName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "They" }
    val title: String
    val subtitle: String
    when {
        request.isCompleted -> {
            title = "Help completed"
            subtitle = if (completedCount > 1) {
                "You've been helped $completedCount times — last one: ${request.subjectLine}."
            } else {
                "You were helped with ${request.subjectLine}."
            }
        }
        request.status == "accepted" -> {
            title = if (request.isSeeker) "Help accepted" else "You're helping with ${request.subjectLine}"
            subtitle = if (request.isSeeker) {
                "Ask your question — close the request when it's answered."
            } else {
                "Answer when you can. $firstName will close it once it's solved."
            }
        }
        request.isSeeker -> {
            title = "Waiting for $firstName"
            subtitle = "Asked for help with ${request.subjectLine}."
        }
        else -> {
            title = "$firstName needs help"
            subtitle = "Asking about ${request.subjectLine}."
        }
    }

    val isPending = request.status == "pending"
    val isAccepted = request.status == "accepted"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (request.isCompleted) Color(0xFFE9F8F0) else Color(0xFFF0FBF5),
        border = BorderStroke(
            1.dp,
            if (request.isCompleted) Color(0xFFA7F3D0) else Color(0xFFBFE9D3)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("chat_help_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (request.isCompleted) EmeraldPrimary else Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (request.isCompleted) Icons.Default.DoneAll else Icons.Default.Forum,
                        contentDescription = null,
                        tint = if (request.isCompleted) Color.White else EmeraldDark,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatInk
                    )
                    Text(
                        text = subtitle,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = ChatMuted
                    )
                }
            }

            if (isPending && request.canAccept) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { if (!isBusy) onAccept() },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("chat_help_accept")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "I can help",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        onClick = { if (!isBusy) onCancel() },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("chat_help_decline")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Not now",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChatMuted
                            )
                        }
                    }
                }
            } else if (isPending && request.isSeeker && request.canCancel) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = { if (!isBusy) onCancel() },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.dp, ChatHairline),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("chat_help_cancel")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Cancel request",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChatMuted
                        )
                    }
                }
            } else if (isAccepted && request.canClose) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { if (!isBusy) onComplete() },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("chat_help_complete")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = "This helped — close",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else if (isAccepted && !request.isSeeker) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Waiting for them to close the request.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = ChatFaint
                )
            } else if (request.isCompleted) {
                // Closed once does not mean closed forever: students come back
                // to the same person with the next question, so the loop has to
                // be reopenable from the same card.
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { if (!isBusy) onAskAgain() },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("chat_help_again")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = if (isBusy) "Sending…" else "Ask again",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = "Next question, same student",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = ChatMuted
                    )
                }
            }
        }
    }
}

/**
 * A help request *arriving* — somebody in this chat needs a hand.
 *
 * It is a small card pinned under the header rather than a row in the thread:
 * the asker's question is what the next message is about, so the card has to stay
 * in front of the reader while they scroll. Accept or wave it away in one tap;
 * the full story of the request stays in the thread underneath.
 */
@Composable
private fun PinnedHelpRequestCard(
    request: ChatHelpRequest,
    peerName: String,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val firstName = peerName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "They" }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
        modifier = Modifier
            .widthIn(max = 300.dp)
            .testTag("chat_help_banner")
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$firstName needs help",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text = request.subjectLine,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = ChatMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(9.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { if (!isBusy) onAccept() },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("chat_help_banner_accept")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (isBusy) "Accepting…" else "I can help",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "Not now",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatMuted,
                    modifier = Modifier
                        .clickable(enabled = !isBusy, onClick = onDecline)
                        .padding(vertical = 4.dp)
                        .testTag("chat_help_banner_decline")
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Student profile
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Read-only profile of another student, opened from their avatar.
 *
 * Shows exactly what they chose to share — name, photo, target exam, stream,
 * institution, presence and when they joined — and nothing that identifies their
 * account. E-mail, phone and role never leave the server (`action=profile`), and
 * the privacy card says so, because students ask.
 */
@Composable
private fun ChatPeerProfileScreen(
    profile: ChatPeerProfile,
    isLoading: Boolean,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onAskForHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val peer = profile.peer
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_profile_screen"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        // The hero is deliberately *not* a card. Photo, name and the Message
        // button sit straight on a mint wash that fades into white, and only the
        // white cards float on top of it. Wrapping the hero in its own surface
        // was what made the page look like a form instead of a profile.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0xFFD7F0E1),
                        0.36f to Color(0xFFE9F7EF),
                        0.70f to Color(0xFFF5FBF8),
                        1.00f to Color(0xFFFDFEFD)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(bottom = 30.dp)
            ) {
                // ── top bar ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ChatInk,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Profile",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatInk
                    )
                }

                // ── hero: centred on the wash ────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ChatAvatar(
                        peer = peer,
                        size = 116.dp,
                        showPresence = true,
                        ringColor = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = peer.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChatInk,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = chatPresenceLine(peer),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (peer.isOnline) EmeraldDark else ChatMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    // Two small capsules that hug their own text instead of
                    // stretching edge to edge: a pair of buttons that fills the
                    // width reads as a form footer, and these are secondary to
                    // the person above them. Centred, 38 dp tall, Message in
                    // solid emerald and Ask help in white with a mint chip.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onMessage,
                            shape = RoundedCornerShape(50),
                            color = EmeraldPrimary,
                            shadowElevation = 9.dp,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("chat_profile_message")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF10B981), EmeraldPrimary)
                                        )
                                    )
                                    .padding(horizontal = 20.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "Message",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Surface(
                            onClick = onAskForHelp,
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            border = BorderStroke(1.2.dp, Color(0xFF6EE7B7)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("chat_profile_help")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1FAE5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = EmeraldDark,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "Ask help",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // While the details are in flight the hero above is already
                // real (name, photo, presence come from the directory), so the
                // whole card stack is replaced by the app's orbit spinner in the
                // same place — no half-filled card with empty rows.
                if (isLoading) {
                    ProfileLoadingState(
                        modifier = Modifier.padding(top = 26.dp, bottom = 22.dp)
                    )
                } else {
                    // ── level ────────────────────────────────────────────────────
                    // The same hero the EXP screen uses, so a level reads the same
                    // everywhere in the app. Hidden entirely for a student with no
                    // EXP yet rather than showing a zero.
                    profile.exp.total.takeIf { it > 0 }?.let { total ->
                        ExpHeroCard(
                            total = total,
                            tier = profile.exp.tier,
                            compact = true,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("chat_profile_exp")
                        )
                    }

                    // ── ratings ──────────────────────────────────────────────────
                    PeerRatingsCard(profile.ratings)

                    // ── badges ───────────────────────────────────────────────────
                    if (profile.exp.total > 0) {
                        ProfileCard(title = "Badges") {
                            ExpBadgeLadder(
                                total = profile.exp.total,
                                medallionSize = 42.dp
                            )
                        }
                    }

                    // ── education ────────────────────────────────────────────────
                    if (profile.hasEducation) {
                        ProfileCard(title = "Education") {
                            // The stage they are at, in the app's own vocabulary:
                            // Grades 6–9 / O/L / A/L / University.
                            if (profile.stageLabel.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Default.MenuBook,
                                    tint = EmeraldPrimary,
                                    tintSoft = Color(0xFFE3F8EC),
                                    label = "Stage",
                                    value = profile.stageLabel
                                )
                            } else if (profile.targetExam.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Default.MenuBook,
                                    tint = EmeraldPrimary,
                                    tintSoft = Color(0xFFE3F8EC),
                                    label = "Target exam",
                                    value = profile.targetExam
                                )
                            }
                            if (profile.gradeLine.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Default.CalendarMonth,
                                    tint = Color(0xFF8B5CF6),
                                    tintSoft = Color(0xFFF1EAFE),
                                    label = if (profile.stageLabel == "University") "Year of study" else "Grade",
                                    value = profile.gradeLine
                                )
                            }
                            if (profile.universityLine.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Default.AccountBalance,
                                    tint = Color(0xFFF0A22E),
                                    tintSoft = Color(0xFFFFF1DC),
                                    label = "University",
                                    value = profile.universityLine
                                )
                            }
                            if (profile.streamLine.isNotBlank() && profile.stageLabel != "University") {
                                ProfileInfoRow(
                                    icon = Icons.Default.School,
                                    tint = Color(0xFF0EA5E9),
                                    tintSoft = Color(0xFFE0F2FE),
                                    label = "Stream",
                                    value = profile.streamLine
                                )
                            }
                            if (profile.subjects.isNotEmpty()) {
                                // The three subjects are what decide whether this
                                // student can help with a specific question, so each
                                // one gets its own line. Joined with " · " they ran
                                // off the card and wrapped mid-subject.
                                SubjectListRow(subjects = profile.subjects)
                            }
                            if (profile.medium.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Default.Translate,
                                    tint = Color(0xFF14B8A6),
                                    tintSoft = Color(0xFFD9F7F1),
                                    label = "Medium",
                                    value = profile.medium
                                )
                            }
                            if (profile.institution.isNotBlank()) {
                                ProfileInfoRow(
                                    icon = Icons.Default.AccountBalance,
                                    tint = Color(0xFFF0A22E),
                                    tintSoft = Color(0xFFFFF1DC),
                                    label = "Institution",
                                    value = profile.institution
                                )
                            }
                        }
                    }

                    // ── on the student app ───────────────────────────────────────
                    ProfileCard(title = "On TheStudent") {
                        profile.joinedEpoch?.let { joined ->
                            ProfileFactRow(
                                icon = Icons.Default.CalendarToday,
                                tint = Color(0xFF8B5CF6),
                                tintSoft = Color(0xFFF1EAFE),
                                label = "Joined",
                                value = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                    .format(Date(joined * 1000))
                            )
                        }
                        profile.chattingSinceEpoch?.let { since ->
                            ProfileFactRow(
                                icon = Icons.Default.Forum,
                                tint = Color(0xFF14B8A6),
                                tintSoft = Color(0xFFD9F7F1),
                                label = "Chatting since",
                                // On this phone only: the relay deletes messages as
                                // soon as they are delivered.
                                value = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(since * 1000))
                            )
                        }
                        ProfileFactRow(
                            icon = Icons.Default.Circle,
                            tint = if (peer.isOnline) ChatOnline else ChatFaint,
                            tintSoft = if (peer.isOnline) Color(0xFFE3F8EC) else Color(0xFFF1F5F9),
                            label = "Currently",
                            value = chatPresenceLine(peer)
                        )
                    }

                    // ── privacy note ─────────────────────────────────────────────
                    // White shell, mint well — the same nested-card look the app
                    // uses on the settings screen.
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFE9F8F0),
                            border = BorderStroke(1.dp, Color(0xFFBEE9D4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(7.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Only what they chose to share",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "Phone numbers and e-mail addresses are never " +
                                            "shown to other students.",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = ChatMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A student's reputation, at a glance.
 *
 * Big average, five stars, the count, a five-bar distribution and the two tag
 * words other students used most. A student nobody has rated yet gets a plain
 * "New" state — never "0.0".
 */
@Composable
private fun PeerRatingsCard(ratings: ChatRatingSummary) {
    ProfileCard(title = "Ratings") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (ratings.hasRatings) ratings.averageLine else "New",
                fontFamily = MaisonNeueFontFamily,
                fontSize = if (ratings.hasRatings) 34.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (ratings.hasRatings) ChatInk else ChatMuted,
                letterSpacing = (-0.8).sp
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val fullStars = ratings.average?.let { kotlin.math.round(it).toInt() } ?: 0
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < fullStars) Color(0xFFFBBF24) else Color(0xFFE2E8F0),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (ratings.hasRatings) {
                        val plural = if (ratings.count == 1) "" else "s"
                        "${ratings.count} rating$plural from students"
                    } else {
                        "No ratings yet"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = ChatMuted
                )
            }
        }

        if (ratings.hasRatings && ratings.breakdown.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            // Five rows, 5★ down to 1★: each bar is that share of all ratings.
            for (star in 5 downTo 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "$star",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatFaint,
                        modifier = Modifier.width(10.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratings.shareOf(star))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFBBF24))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${ratings.breakdown[star] ?: 0}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = ChatFaint,
                        modifier = Modifier.width(16.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        if (ratings.topTags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ratings.topTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE9F8F0))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tag.label,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subjects, one per row.
 *
 * A joining " · " string wrapped mid-subject and lost the fact that these are
 * three separate things. Each subject is now its own tappable-weight line with a
 * small subject-coloured dot, so the list reads as a list.
 */
@Composable
private fun SubjectListRow(subjects: List<String>) {
    Row {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFFF1EAFE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Subjects",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = ChatFaint
            )
            Spacer(Modifier.height(6.dp))
            subjects.forEachIndexed { index, subject ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFF8B5CF6), CircleShape)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = subject,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatInk,
                        modifier = Modifier.testTag("chat_profile_subject_$index")
                    )
                }
            }
        }
    }
}

/**
 * The orbit spinner, in the space where a peer's cards will appear.
 *
 * Reuses `OrbitLoader` — the same loader the conversation shows while it is
 * being fetched — so a two-second wait anywhere in chat looks like one app rather
 * than three different spinners.
 */
@Composable
private fun ProfileLoadingState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_profile_loading")
    ) {
        OrbitLoader(size = 44.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Loading their profile…",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ChatMuted
        )
    }
}

/** White card with a small caps title, floating on the profile wash. */
@Composable
private fun ProfileCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ChatFaint,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Education row: tinted icon tile, tiny label, value underneath. */
@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tintSoft: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = ChatFaint
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChatInk
            )
        }
    }
}

/** Fact row: icon, label on the left, the value pushed to the right edge. */
@Composable
private fun ProfileFactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tintSoft: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = ChatMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChatInk,
            textAlign = TextAlign.End
        )
    }
}

/** "Online now" / "Last seen 12 min ago" — the same wording as the chat header. */
private fun chatPresenceLine(peer: ChatPeer): String {
    if (peer.isOnline) return "Online now"
    val seconds = peer.lastSeenSeconds ?: return "Offline"
    val minutes = seconds / 60
    return when {
        seconds < 60 -> "Last seen just now"
        minutes < 60 -> "Last seen $minutes min ago"
        minutes < 60 * 24 -> "Last seen ${minutes / 60} h ago"
        minutes < 60 * 24 * 7 -> "Last seen ${minutes / (60 * 24)} d ago"
        else -> "Offline"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Directory (list of registered students)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatDirectoryScreen(
    state: ChatUiState,
    onOpenChat: (ChatPeer) -> Unit,
    onOpenProfile: (ChatPeer) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onAcknowledgeSafety: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }

    val visibleThreads = remember(state.threads, searchQuery) {
        if (searchQuery.isBlank()) {
            state.threads
        } else {
            state.threads.filter { it.peer.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    val onlineStudents = remember(state.threads) { state.threads.filter { it.peer.isOnline } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_screen"),
        // Transparent on purpose: the app-wide wash is drawn edge-to-edge behind
        // the content so the system bars blend into it, exactly like onboarding.
        containerColor = Color.Transparent,
        // The dashboard Scaffold already reserves the bottom-bar area; only the
        // status bar has to be handled here.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        TheStudentAppBackground(
            baseColor = Color(0xFFF8FAFC),
            modifier = Modifier.fillMaxSize()
        ) {
            // Same architectural background art used on the onboarding slides.
            OnboardingModernFooterArt(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.BottomCenter)
                    .alpha(0.55f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // ── header ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Student chat",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChatInk,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                append("${state.totalStudents} registered student")
                                if (state.totalStudents != 1) append("s")
                                append(" · ${state.onlineCount} online")
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            color = ChatMuted
                        )
                    }

                    Surface(
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) searchQuery = ""
                        },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_search_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (searchOpen) "Close search" else "Search students",
                                tint = ChatInk,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onRefresh,
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = EmeraldDark,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = searchOpen) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search by full name",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 14.sp,
                                color = ChatFaint
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = ChatFaint)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ChatHairline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("chat_search_field")
                    )
                }

                // ── online now rail ──────────────────────────────────────────
                if (onlineStudents.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(ChatOnline, CircleShape)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "Online now",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChatInk
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${state.onlineCount}",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            color = ChatMuted
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(onlineStudents, key = { it.peer.id }) { thread ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clickable { onOpenChat(thread.peer) }
                            ) {
                                ChatAvatar(
                                    peer = thread.peer,
                                    size = 58.dp,
                                    showPresence = true,
                                    ringColor = Color.White
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    // First name only in the rail to keep the row tight;
                                    // the full name is on the directory row below.
                                    text = thread.peer.name.substringBefore(" "),
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ChatInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                } else {
                    Spacer(Modifier.height(10.dp))
                }

                // ── error banner ─────────────────────────────────────────────
                state.errorMessage?.let { message ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = ChatError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Dismiss",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChatError,
                                modifier = Modifier
                                    .clickable { onDismissError() }
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── student list ─────────────────────────────────────────────
                when {
                    state.isLoadingDirectory && state.threads.isEmpty() -> {
                        Column(modifier = Modifier.weight(1f)) {
                            ChatDirectorySkeleton()
                        }
                    }

                    visibleThreads.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = ChatFaint,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (state.threads.isEmpty()) {
                                        "No other students have signed up yet"
                                    } else {
                                        "No student matches \"$searchQuery\""
                                    },
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ChatInk,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Only registered TheStudent students appear here.",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.5.sp,
                                    color = ChatMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(
                                bottom = if (state.safetyAcknowledged) 24.dp else 190.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(visibleThreads, key = { it.peer.id }) { thread ->
                                ChatThreadRow(
                                    thread = thread,
                                    onClick = { onOpenChat(thread.peer) },
                                    onOpenProfile = { onOpenProfile(thread.peer) }
                                )
                            }
                        }
                    }
                }
            }

            // One-time safety notice: shown on first entry until acknowledged.
            // Stored server-side (`users.chat_safety_ack_at`) so it never comes
            // back — not even after a reinstall or on a second device.
            AnimatedVisibility(
                visible = !state.safetyAcknowledged,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    SafetyNoticeBanner(onAcknowledge = onAcknowledgeSafety)
                }
            }
        }
    }
}

@Composable
private fun ChatThreadRow(
    thread: ChatThread,
    onClick: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val peer = thread.peer
    val preview = when {
        peer.isTyping -> "typing…"
        thread.lastMessage != null -> (if (thread.lastMessageIsMine) "You: " else "") + thread.lastMessage
        else -> "Say hello 👋"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("chat_thread_${peer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The avatar opens the profile; the rest of the row opens the chat,
            // so one tap never has to guess what the student meant.
            ChatAvatar(
                peer = peer,
                size = 52.dp,
                showPresence = true,
                ringColor = Color.White,
                // No outer clip: it also cut the little green presence dot in
                // half, which is why it looked out of place. The avatar clips
                // its own picture; the ripple just gets a square bound.
                modifier = Modifier
                    .clickable(onClick = onOpenProfile)
                    .testTag("chat_profile_${peer.id}")
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Only the last message here. The "what's this chat about"
                    // label used to sit in front of it, and it turned fifteen
                    // rows of real conversation into fifteen labels.
                    Text(
                        text = preview,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        color = if (peer.isTyping) EmeraldPrimary else ChatMuted,
                        fontWeight = if (peer.isTyping) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatThreadStamp(thread.lastMessageEpoch),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = if (thread.unreadCount > 0) EmeraldPrimary else ChatFaint,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Messages from them I still have to read: the loud badge.
                    if (thread.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .background(EmeraldPrimary, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (thread.unreadCount > 99) "99+" else thread.unreadCount.toString(),
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    if (thread.unreadCount == 0 && peer.isOnline) {
                        Text(
                            text = "online",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            color = ChatOnline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Conversation
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatConversationScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onTyping: () -> Unit,
    onAcknowledgeSafety: () -> Unit,
    onClearChat: () -> Unit,
    onDismissError: () -> Unit,
    onPickAttachment: (Uri) -> Unit,
    onClearAttachment: () -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onReply: (ChatMessage) -> Unit,
    onClearReply: () -> Unit,
    onStartVoiceNote: () -> Boolean,
    onSendVoiceNote: () -> Unit,
    onCancelVoiceNote: () -> Unit,
    onStopTyping: () -> Unit,
    onOpenProfile: (ChatPeer) -> Unit,
    onAcceptHelp: () -> Unit = {},
    onCompleteHelp: () -> Unit = {},
    onCancelHelp: () -> Unit = {},
    onOpenIntent: () -> Unit = {},
    onOpenRating: () -> Unit = {},
    onTrackQuestion: () -> Unit = {},
    onDismissQuestionOffer: () -> Unit = {},
    onAskAgain: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val peer = state.activePeer ?: return
    var input by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var pendingDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var emojiRowOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val onLongPressMessage: (ChatMessage) -> Unit = { selectedMessage = it }

    // Tapping anywhere outside the composer closes the keyboard, drops the input
    // border (focus) and clears the "typing…" flag for the other student.
    val dismissComposer: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        onStopTyping()
    }

    // Voice notes need the microphone. Everything else in chat needs nothing.
    val micPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceNote()
        } else {
            Toast.makeText(context, "Microphone access is needed for voice notes.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery and document pickers. Neither needs a runtime permission: the
    // system picker hands back a one-off readable Uri.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPickAttachment) }
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPickAttachment) }

    // Search never hides messages: it jumps between hits, like WhatsApp, so the
    // conversation around each match stays visible.
    val rows = remember(state.messages) { buildChatRows(state.messages) }
    val searchActive = searchOpen && searchQuery.isNotBlank()
    val searchTerm = searchQuery.trim()

    // Row indices (including day separators) of every matching message.
    val matchRowIndices = remember(rows, searchTerm) {
        if (searchTerm.isEmpty()) {
            emptyList()
        } else {
            rows.mapIndexedNotNull { index, row ->
                if (row is ChatRow.Msg && row.message.body.contains(searchTerm, ignoreCase = true)) {
                    index
                } else {
                    null
                }
            }
        }
    }
    val activeMatch = remember(searchTerm) { mutableIntStateOf(0) }

    // Keep the cursor inside the result list as the query changes.
    LaunchedEffect(matchRowIndices.size, searchTerm) {
        if (matchRowIndices.isNotEmpty() && activeMatch.intValue > matchRowIndices.lastIndex) {
            activeMatch.intValue = matchRowIndices.lastIndex
        }
    }

    fun jumpToMatch(index: Int) {
        if (matchRowIndices.isEmpty()) return
        val wrapped = ((index % matchRowIndices.size) + matchRowIndices.size) % matchRowIndices.size
        activeMatch.intValue = wrapped
        scope.launch {
            listState.animateScrollToItem(matchRowIndices[wrapped])
        }
    }

    val currentMatchRow = matchRowIndices.getOrNull(activeMatch.intValue)

    // New query: start at the first hit so the student sees a result immediately.
    LaunchedEffect(searchTerm) {
        if (searchTerm.isNotEmpty()) jumpToMatch(0)
    }

    // The chat is only revealed once the list is already parked at the newest
    // message, so the student never watches it fly down the screen.
    var conversationSettled by remember(peer.id) { mutableStateOf(false) }

    /**
     * How many rows the list held the last time the reader was parked at the
     * bottom. A count (not an index) so it still means "the reader was here"
     * after rows are inserted — see the follow effect below.
     */
    var rowsWhenLastAtBottom by remember(peer.id) { mutableStateOf(-1) }

    LaunchedEffect(rows.size, state.isLoadingMessages, peer.id) {
        if (conversationSettled) return@LaunchedEffect

        if (rows.isEmpty()) {
            // Nothing stored yet: only settle once the database has answered,
            // otherwise the empty state would flash before the history arrives.
            if (!state.isLoadingMessages) {
                withFrameNanos { }
                conversationSettled = true
            }
            return@LaunchedEffect
        }

        // Jump (not animate) to the last message while the loader still covers
        // the list, give the frame a chance to lay out at that position, then
        // let the caller fade the loader away onto a conversation that is
        // already at the bottom.
        listState.scrollToItem(rows.lastIndex)
        withFrameNanos { }
        withFrameNanos { }
        rowsWhenLastAtBottom = rows.size
        conversationSettled = true
    }

    // Safety net: never leave the student behind the spinner.
    LaunchedEffect(peer.id) {
        delay(4_000)
        if (!conversationSettled) conversationSettled = true
    }

    // ── staying at the bottom ────────────────────────────────────────────────
    // The follow decision used to come from
    // `layoutInfo.visibleItemsInfo.lastOrNull()`, which is exactly the wrong
    // moment to look: when a message is inserted the effect can run before that
    // row has been laid out, so "am I at the bottom?" read stale indices and the
    // list simply never moved.
    //
    // `canScrollForward == false` means nothing is below the viewport, i.e. the
    // reader is at the newest message. rememberUpdatedState (not a write during
    // composition) keeps this single collector reading the current row count.
    val rowCount by rememberUpdatedState(rows.size)

    LaunchedEffect(listState, peer.id) {
        snapshotFlow { listState.canScrollForward }
            .collect { canScrollForward ->
                // canScrollForward == false means there is nothing below the
                // viewport: the reader is at the newest message.
                if (!canScrollForward) rowsWhenLastAtBottom = rowCount
            }
    }

    // Follow the conversation. My own message always scrolls (sending must never
    // leave your own bubble off screen); an incoming one scrolls while the
    // reader is at the bottom, so scrolling back through history or an active
    // search is never yanked away.
    LaunchedEffect(rows.size, searchActive, conversationSettled) {
        if (rows.isEmpty() || !conversationSettled) return@LaunchedEffect
        val newestIsMine = (rows.lastOrNull() as? ChatRow.Msg)?.message?.isMine == true
        val wasAtBottom = rowsWhenLastAtBottom >= 0 &&
            rowsWhenLastAtBottom >= rows.size - 1
        if (searchActive && !newestIsMine) return@LaunchedEffect
        if (newestIsMine || wasAtBottom) {
            // Let the new row be laid out first: scrolling in the same frame as
            // the insertion is what made the list appear not to move at all.
            withFrameNanos { }
            listState.animateScrollToItem(rows.lastIndex)
            rowsWhenLastAtBottom = rows.size
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_conversation_screen"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        TheStudentAppBackground(
            baseColor = Color(0xFFF8FAFC),
            modifier = Modifier.fillMaxSize()
        ) {
            OnboardingModernFooterArt(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .align(Alignment.BottomCenter)
                    .alpha(0.30f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .union(WindowInsets.ime)
                    )
                    // Taps on the header / message area / day chips land here.
                    // Gestures already claimed by a bubble never reach it.
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { dismissComposer() })
                    }
            ) {
                // ── top bar ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ChatInk,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    // Header photo also opens the profile — the natural place to
                    // tap when you want to know who you are talking to.
                    ChatAvatar(
                        peer = peer,
                        size = 42.dp,
                        showPresence = true,
                        ringColor = Color.White,
                        // Unclipped for the same reason as the list row: a clip
                        // here shaves the presence dot.
                        modifier = Modifier.clickable { onOpenProfile(peer) }
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peer.name,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChatInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Presence only. The purpose label used to sit beside it
                        // and pushed the row into two lines, hiding "last seen"
                        // behind the name block. It now lives in the ⋯ menu and
                        // on the floating bubble, where there is room for it.
                        Text(
                            text = peer.presenceLabel,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                peer.isTyping -> EmeraldPrimary
                                peer.isOnline -> ChatOnline
                                else -> ChatMuted
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) searchQuery = ""
                        },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_conversation_search")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (searchOpen) "Close search" else "Search this chat",
                                tint = ChatInk,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Surface(
                            onClick = { showMenu = true },
                            shape = CircleShape,
                            color = if (showMenu) Color(0xFFECFDF5) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (showMenu) Color(0xFF6EE7B7) else ChatHairline
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("chat_menu_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Conversation options",
                                    tint = if (showMenu) EmeraldDark else ChatInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // A popup rather than a Material DropdownMenu: the menu
                        // needs a header, grouped rows with their own tinted icon
                        // tiles, a divider and a destructive row at the bottom.
                        // Those are the things that make it read as part of this
                        // app instead of a stock list.
                        if (showMenu) {
                            val density = LocalDensity.current
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, with(density) { 46.dp.roundToPx() }),
                                onDismissRequest = { showMenu = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                ChatOptionsMenu(
                                    peerName = peer.name,
                                    presence = peer.presenceLabel,
                                    isOnline = peer.isOnline,
                                    onAskAgain = {
                                        showMenu = false
                                        // Straight to a request: no sheet, no
                                        // subject field. "Ask for help" and
                                        // "what's this chat about" are different
                                        // questions.
                                        onAskAgain()
                                    },
                                    onOpenIntent = {
                                        showMenu = false
                                        onOpenIntent()
                                    },
                                    onOpenRating = {
                                        showMenu = false
                                        onOpenRating()
                                    },
                                    onClearChat = {
                                        showMenu = false
                                        showClearDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // ── in-conversation search ───────────────────────────────────
                AnimatedVisibility(visible = searchOpen, enter = fadeIn(), exit = fadeOut()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search in this chat",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                color = ChatFaint
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = ChatMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchTerm.isNotEmpty()) {
                                    Text(
                                        text = if (matchRowIndices.isEmpty()) {
                                            "0/0"
                                        } else {
                                            "${activeMatch.intValue + 1}/${matchRowIndices.size}"
                                        },
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (matchRowIndices.isEmpty()) ChatFaint else EmeraldDark
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    SearchNavButton(
                                        icon = Icons.Default.KeyboardArrowUp,
                                        description = "Previous match",
                                        enabled = matchRowIndices.isNotEmpty(),
                                        onClick = { jumpToMatch(activeMatch.intValue - 1) }
                                    )
                                    SearchNavButton(
                                        icon = Icons.Default.KeyboardArrowDown,
                                        description = "Next match",
                                        enabled = matchRowIndices.isNotEmpty(),
                                        onClick = { jumpToMatch(activeMatch.intValue + 1) }
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = ChatMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ChatHairline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                if (searchActive) {
                    Text(
                        text = when (matchRowIndices.size) {
                            0 -> "No message in this chat contains \"$searchTerm\""
                            1 -> "1 matching message"
                            else -> "${matchRowIndices.size} matching messages · use ↑ ↓ to jump"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        color = ChatMuted,
                        modifier = Modifier.padding(start = 18.dp, top = 2.dp, bottom = 2.dp)
                    )
                }

                // ── an incoming request, pinned under the header ─────────────
                // A question that needs an answer right now stays in front of the
                // reader while they scroll; it is not a message in the thread.
                val incomingHelp = state.helpRequest
                    ?.takeIf { it.status == "pending" && it.canAccept }
                AnimatedVisibility(
                    visible = incomingHelp != null,
                    enter = fadeIn(tween(160)) +
                        scaleIn(
                            initialScale = 0.94f,
                            transformOrigin = TransformOrigin(1f, 0f),
                            animationSpec = tween(180)
                        ),
                    exit = fadeOut(tween(120)) +
                        scaleOut(
                            targetScale = 0.96f,
                            transformOrigin = TransformOrigin(1f, 0f),
                            animationSpec = tween(120)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 14.dp, top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        incomingHelp?.let { help ->
                            PinnedHelpRequestCard(
                                request = help,
                                peerName = peer.name,
                                isBusy = state.isHelpBusy,
                                onAccept = onAcceptHelp,
                                onDecline = onCancelHelp
                            )
                        }
                    }
                }

                // ── messages ─────────────────────────────────────────────────
                // The list is composed even while the loader is up: it gets parked
                // at the newest message underneath, then the overlay fades away.
                Box(modifier = Modifier.weight(1f)) {
                    // Hidden (not removed) while the loader is up: it still has to
                    // be laid out so it can be parked at the newest message, but
                    // its contents must not flash before the overlay fades.
                    // Cross-fades with the loader over the same 220 ms, so the
                    // reveal is a fade rather than a snap.
                    val listAlpha by animateFloatAsState(
                        targetValue = if (conversationSettled) 1f else 0f,
                        animationSpec = tween(220),
                        label = "chat_list_reveal"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(alpha = listAlpha)
                    ) {
                    when {
                        state.messages.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (searchActive) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = ChatFaint,
                                            modifier = Modifier.size(38.dp)
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "No messages match your search",
                                            fontFamily = MaisonNeueFontFamily,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ChatMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Try a different word or clear the search.",
                                            fontFamily = MaisonNeueFontFamily,
                                            fontSize = 12.sp,
                                            color = ChatFaint,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        return@Column
                                    }
                                    ChatAvatar(
                                        peer = peer,
                                        size = 64.dp,
                                        showPresence = false,
                                        ringColor = Color.Transparent
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "This is the start with ${peer.name}",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChatInk,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Ask about a past paper, share how you solved a question, or just say hello.",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 12.5.sp,
                                        color = ChatMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // The help request lives above the messages:
                                // it is context for the whole conversation, not
                                // a message in it.
                                state.helpRequest?.let { help ->
                                    // A request that still needs an answer lives in
                                    // the pinned card above, not twice in the thread.
                                    val pinned = help.status == "pending" && help.canAccept
                                    if ((help.isOpen || help.isCompleted) && !pinned) {
                                        item(key = "help-request") {
                                            HelpRequestCard(
                                                request = help,
                                                peerName = peer.name,
                                                completedCount = state.helpCompletedWithPeer,
                                                isBusy = state.isHelpBusy || state.isAskingHelp,
                                                onAccept = onAcceptHelp,
                                                onComplete = onCompleteHelp,
                                                onCancel = onCancelHelp,
                                                onAskAgain = onAskAgain
                                            )
                                        }
                                    }
                                }

                                itemsIndexed(
                                    items = rows,
                                    key = { _, row ->
                                        when (row) {
                                            is ChatRow.Day -> "day-${row.key}"
                                            is ChatRow.Msg -> row.message.localId
                                        }
                                    }
                                ) { currentIndex, row ->
                                    when (row) {
                                        is ChatRow.Day -> DayDivider(row.label)
                                        is ChatRow.Msg -> ChatBubble(
                                            message = row.message,
                                            peerName = peer.name,
                                            downloadProgress = row.message.attachment?.let { attachment ->
                                                state.attachmentDownloadProgress[attachment.id]
                                            },
                                            onLongPress = onLongPressMessage,
                                            onSwipeReply = { message -> onReply(message) },
                                            highlight = searchTerm.takeIf { searchActive },
                                            isCurrentMatch = currentMatchRow == currentIndex
                                        )
                                    }
                                }

                                if (peer.isTyping) {
                                    item { TypingIndicator() }
                                }

                                // Offered only after a question was sent, and
                                // only once per pair: the message already went.
                                if (state.questionOfferVisible) {
                                    item(key = "question-offer") {
                                        QuestionOfferCard(
                                            peerName = peer.name,
                                            isBusy = state.isAskingHelp,
                                            onTrack = onTrackQuestion,
                                            onDismiss = onDismissQuestionOffer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    } // end hidden list content

                    // Overlay inside the list container: the conversation is
                    // already parked at the bottom underneath when this fades out.
                    ConversationLoadingOverlay(visible = !conversationSettled)

                    // The help bubble floats over the conversation, parked above
                    // the microphone by default and draggable from there. While a
                    // request is waiting for an answer the pinned card says it all,
                    // so the bubble steps aside instead of repeating it.
                    if (conversationSettled && incomingHelp == null) {
                        FloatingHelpBubble(
                            state = state,
                            peerName = peer.name,
                            onOpenPurpose = onOpenIntent,
                            onAskForHelp = onTrackQuestion,
                            onAskAgain = onAskAgain,
                            onAcceptHelp = onAcceptHelp,
                            onCompleteHelp = onCompleteHelp,
                            onCancelHelp = onCancelHelp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                state.errorMessage?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            color = ChatError,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChatError,
                            modifier = Modifier
                                .clickable { onDismissError() }
                                .padding(start = 8.dp)
                        )
                    }
                }

                // ── secure-chat availability (peer has not published a key) ──
                if (!peer.canReceiveEncrypted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = ChatMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${peer.name} hasn't opened secure chat yet, so messages can't be delivered.",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            color = ChatMuted
                        )
                    }
                }

                // ── one-time safety notice ───────────────────────────────────
                AnimatedVisibility(
                    visible = !state.safetyAcknowledged,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SafetyNoticeBanner(onAcknowledge = onAcknowledgeSafety)
                }

                // ── composer ─────────────────────────────────────────────────

                // Quoted message the next send will reply to.
                state.pendingReply?.let { reply ->
                    ReplyComposerBar(
                        reply = reply,
                        peerName = peer.name,
                        onCancel = onClearReply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
                    )
                }

                // Picked file, previewed small above the input until it is sent.
                state.pendingAttachment?.let { draft ->
                    AttachmentDraftCard(
                        draft = draft,
                        onRemove = onClearAttachment,
                        uploadProgress = if (state.isUploadingAttachment) {
                            state.attachmentUploadProgress
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
                    )
                }

                // Emoji strip: quick access without switching keyboards.
                AnimatedVisibility(visible = emojiRowOpen, enter = fadeIn(), exit = fadeOut()) {
                    EmojiPickerRow(
                        onPick = { emoji -> input += emoji },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                if (state.isRecordingVoiceNote) {
                    // The wave runs *inside* the input bar, in the composer's own
                    // place: the screen does not lurch into a different layout the
                    // moment you tap the microphone, and the send button is where
                    // the send button always is.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RecordingComposer(
                            elapsedMs = state.voiceNoteElapsedMs,
                            level = state.voiceNoteLevel,
                            onCancel = onCancelVoiceNote,
                            modifier = Modifier
                                .weight(1f)
                                // A fixed height, matching the text field. A
                                // minimum alone left the maximum unbounded, and
                                // the row inside then filled the whole screen —
                                // which is what drew that tall box on the left.
                                .height(50.dp)
                                .testTag("chat_recording_bar")
                        )
                        Spacer(Modifier.width(8.dp))
                        // One tap: stop, stage and send. No second question.
                        Surface(
                            onClick = onSendVoiceNote,
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("chat_voice_send")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send voice note",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            onClick = { showAttachSheet = true },
                            enabled = peer.canReceiveEncrypted && !state.isSending,
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, ChatHairline),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("chat_attach_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = "Attach a photo or document",
                                    tint = if (peer.canReceiveEncrypted) EmeraldDark else ChatFaint,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = {
                                input = it
                                if (it.isNotBlank()) onTyping()
                            },
                            placeholder = {
                                Text(
                                    "Message ${peer.name.substringBefore(" ")}…",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 14.sp,
                                    color = ChatFaint
                                )
                            },
                            maxLines = 5,
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                Surface(
                                    onClick = { emojiRowOpen = !emojiRowOpen },
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (emojiRowOpen) {
                                                Icons.Default.Keyboard
                                            } else {
                                                Icons.Default.EmojiEmotions
                                            },
                                            contentDescription = if (emojiRowOpen) {
                                                "Hide emoji"
                                            } else {
                                                "Show emoji"
                                            },
                                            tint = if (emojiRowOpen) EmeraldPrimary else ChatMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ChatHairline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                // Keyboard dismissed with the back gesture, or focus
                                // moved elsewhere, must not leave "typing…" behind.
                                .onFocusChanged { state ->
                                    if (!state.isFocused) onStopTyping()
                                }
                                .testTag("chat_input_field")
                        )
                        Spacer(Modifier.width(8.dp))

                        val hasSomethingToSend = input.isNotBlank() || state.pendingAttachment != null
                        val canSend = hasSomethingToSend && !state.isSending && peer.canReceiveEncrypted
                        // Empty composer -> microphone; anything typed or attached
                        // -> send. Exactly one primary action, like every chat app.
                        if (!hasSomethingToSend) {
                            Surface(
                                onClick = {
                                    if (peer.canReceiveEncrypted) {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            onStartVoiceNote()
                                        } else {
                                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                enabled = peer.canReceiveEncrypted && !state.isSending,
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, ChatHairline),
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("chat_mic_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Record a voice note",
                                        tint = if (peer.canReceiveEncrypted) EmeraldDark else ChatFaint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                onClick = {
                                    val text = input.trim()
                                    if (canSend) {
                                        onSend(text)
                                        input = ""
                                    }
                                },
                                enabled = canSend,
                                shape = CircleShape,
                                color = if (canSend) EmeraldPrimary else Color(0xFFCBD5E1),
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("chat_send_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (state.isSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // End-to-end encryption note: grey, directly under the input bar.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = ChatFaint,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Messages are end-to-end encrypted",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = ChatFaint
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ChatConfirmDialog(
            icon = Icons.Default.DeleteOutline,
            title = "Clear this chat?",
            message = "Your copy of the conversation with ${peer.name} is removed. " +
                "The messages stay in their chat.",
            confirmLabel = "Clear chat",
            note = "Only on this phone",
            noteIcon = Icons.Default.Smartphone,
            onConfirm = onClearChat,
            onDismiss = { showClearDialog = false }
        )
    }

    // ── attachment picker ───────────────────────────────────────────────────
    if (showAttachSheet) {
        AttachmentPickerSheet(
            onDismiss = { showAttachSheet = false },
            onPhoto = {
                showAttachSheet = false
                imagePicker.launch("image/*")
            },
            onDocument = {
                showAttachSheet = false
                documentPicker.launch(CHAT_DOCUMENT_TYPES)
            }
        )
    }

    // ── long-press actions on a message ─────────────────────────────────────
    selectedMessage?.let { message ->
        MessageActionsSheet(
            message = message,
            onDismiss = { selectedMessage = null },
            onReply = {
                onReply(message)
                selectedMessage = null
            },
            onCopy = {
                copyToClipboard(context, message.body)
                selectedMessage = null
            },
            onDelete = {
                pendingDelete = message
                selectedMessage = null
            }
        )
    }

    pendingDelete?.let { message ->
        val forEveryone = message.isMine && message.serverId > 0L
        ChatConfirmDialog(
            icon = Icons.Default.DeleteOutline,
            title = if (forEveryone) "Delete for everyone?" else "Remove this message?",
            message = if (forEveryone) {
                "This message disappears from ${peer.name}'s phone too."
            } else {
                "It disappears from your phone. ${peer.name} keeps their copy."
            },
            confirmLabel = if (forEveryone) "Delete" else "Remove",
            preview = message.body.takeIf { it.isNotBlank() }
                ?: message.attachment?.let { attachment ->
                    if (attachment.isImage) "📷 Photo" else "📎 ${attachment.name}"
                },
            note = if (forEveryone) "Both of you lose it" else "Only on this phone",
            noteIcon = if (forEveryone) Icons.Default.Groups else Icons.Default.Smartphone,
            onConfirm = { onDeleteMessage(message) },
            onDismiss = { pendingDelete = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pieces
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bottom sheet with the two ways to send a file.
 *
 * Deliberately not a dropdown menu: each option gets a tinted icon tile, a
 * one-line explanation and the upload limit, so the choice is obvious before the
 * system picker takes over the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentPickerSheet(
    onDismiss: () -> Unit,
    onPhoto: () -> Unit,
    onDocument: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ChatHairline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Send a file",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ChatInk,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Encrypted on your phone before it leaves. Up to 25 MB.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = ChatMuted
            )
            Spacer(Modifier.height(16.dp))

            AttachmentOptionRow(
                title = "Photo",
                subtitle = "JPG, PNG, GIF or WebP from your gallery",
                iconTint = EmeraldDark,
                iconBackground = Color(0xFFE9F9EF),
                icon = Icons.Default.Image,
                onClick = onPhoto
            )
            Spacer(Modifier.height(10.dp))
            AttachmentOptionRow(
                title = "Document",
                subtitle = "PDF, Word, PowerPoint, Excel, text or ZIP",
                iconTint = Color(0xFF2563EB),
                iconBackground = Color(0xFFEAF1FE),
                icon = Icons.Default.Description,
                onClick = onDocument
            )
        }
    }
}

@Composable
private fun AttachmentOptionRow(
    title: String,
    subtitle: String,
    iconTint: Color,
    iconBackground: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ChatSheetCard,
        border = BorderStroke(1.dp, ChatHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatInk
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = ChatMuted
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ChatFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Tiny circular button for the search field's ↑ / ↓ navigation. */
@Composable
private fun SearchNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) Color(0xFFE9F9EF) else Color(0xFFF1F5F9),
        modifier = Modifier
            .size(26.dp)
            .padding(horizontal = 0.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (enabled) EmeraldDark else ChatFaint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Quoted-message bar shown above the composer while replying. */
@Composable
private fun ReplyComposerBar(
    reply: ChatReply,
    peerName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ChatHairline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EmeraldPrimary)
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (reply.isMine) {
                        "Replying to your message"
                    } else {
                        "Replying to ${peerName.substringBefore(" ")}"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = reply.preview,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = ChatMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onCancel,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = ChatMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/** Quoted block rendered at the top of a bubble. */
@Composable
private fun ReplyQuoteBlock(reply: ChatReply, isMine: Boolean, peerName: String = "") {
    // `reply.isMine` was recorded by whoever wrote THIS bubble, so it means
    // "the quoted message is mine" only on their device. On the other device the
    // sides are mirrored: a quote of the sender's own message belongs to the peer.
    // Fixing that is what stopped every quote reading "You" on both phones.
    val quotedIsMine = if (isMine) reply.isMine else !reply.isMine
    val quotedName = if (quotedIsMine) {
        "You"
    } else {
        peerName.substringBefore(' ').ifBlank { "Them" }
    }
    val accent = if (isMine) Color.White.copy(alpha = 0.55f) else EmeraldPrimary
    val background = if (isMine) Color.White.copy(alpha = 0.12f) else Color(0xFFF1F5F9)
    Row(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .padding(vertical = 5.dp, horizontal = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accent)
        )
        Spacer(Modifier.width(7.dp))
        Column {
            Text(
                text = quotedName,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = reply.preview,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = if (isMine) Color.White.copy(alpha = 0.88f) else ChatMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A scrollable strip of common emoji, tap to insert. */
@Composable
private fun EmojiPickerRow(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(CHAT_EMOJI) { emoji ->
            Surface(
                onClick = { onPick(emoji) },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, ChatHairline),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 19.sp)
                }
            }
        }
    }
}

/** Replaces the composer while a voice note is being recorded. */
/**
 * The composer while a voice note is being recorded.
 *
 * Built to sit exactly where the text field sits: a 24 dp pill with the live
 * waveform inside it, a pulsing record dot, the running clock and a small cross
 * to throw the take away. The waveform reacts to the microphone — [level] is the
 * peak of the last 90 ms — but keeps a gentle idle motion, so the bar still reads
 * as "recording" on a quiet phone or an emulator that reports no amplitude.
 */
@Composable
private fun RecordingComposer(
    elapsedMs: Long,
    level: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = elapsedMs / 1000L
    val clock = String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)

    // A smooth, ever-running phase: the bars never freeze, whatever the mic says.
    val wave = rememberInfiniteTransition(label = "voice_wave")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_wave_phase"
    )
    val dotAlpha by wave.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_dot"
    )
    // The mic is spiky; ease it so the bars swell instead of jittering.
    val smoothed by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "voice_level"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Color(0xFF6EE7B7)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .background(ChatError, CircleShape)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = clock,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ChatInk
            )
            Spacer(Modifier.width(12.dp))

            VoiceWave(
                phase = phase,
                level = smoothed,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
            )

            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onCancel,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel recording",
                        tint = ChatError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * The live waveform: a row of emerald bars whose heights travel like a wave and
 * grow with the microphone level.
 *
 * Drawn on a Canvas rather than as thirty composables — one draw pass per frame,
 * and each bar is centred in its own cell so the shape reads as a wave rather
 * than a bar chart.
 */
@Composable
private fun VoiceWave(
    phase: Float,
    level: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bars = 26
        val cell = size.width / bars
        val barWidth = cell * 0.42f
        // A quiet mic still moves: 0.22 minimum, 1.0 when the student is loud.
        val energy = 0.22f + 0.78f * level

        for (index in 0 until bars) {
            val t = (phase + index.toFloat() / bars) * 2f * PI.toFloat()
            val shape = (0.35f + 0.65f * abs(sin(t))) * energy
            // Taper the ends so the wave reads as a pill, not a block of bars.
            val taper = sin((index + 0.5f) / bars * PI.toFloat()).coerceIn(0f, 1f)
            val height = (size.height * shape * (0.45f + 0.55f * taper))
                .coerceAtLeast(barWidth)
            val left = index * cell + (cell - barWidth) / 2f
            drawRoundRect(
                color = EmeraldPrimary.copy(alpha = 0.30f + 0.55f * shape),
                topLeft = Offset(left, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

/** Thin progress line used while an attachment uploads or downloads. */
@Composable
private fun AttachmentProgressBar(progress: Float, onDark: Boolean, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (onDark) Color.White.copy(alpha = 0.28f) else Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(RoundedCornerShape(2.dp))
                .background(if (onDark) Color.White else EmeraldPrimary)
        )
    }
}

/**
 * The ⋯ menu for a conversation.
 *
 * Deliberately not a stock `DropdownMenu`: it opens with a small identity header
 * (who you are talking to, and whether they are around), groups the three things
 * you might *do* above a hairline, and keeps "Clear chat" alone at the bottom in
 * red where a destructive action belongs. 244 dp wide, one tap target per row.
 */
@Composable
private fun ChatOptionsMenu(
    peerName: String,
    presence: String,
    isOnline: Boolean,
    onAskAgain: () -> Unit,
    onOpenIntent: () -> Unit,
    onOpenRating: () -> Unit,
    onClearChat: () -> Unit
) {
    // Scale-in from the dot that opened it, so the menu grows out of the toolbar
    // instead of blinking into existence.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(110)) +
            scaleIn(
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(150)
            )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFFEEF2F7)),
            modifier = Modifier
                .width(244.dp)
                .testTag("chat_options_menu")
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                // ── who ──────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peerName.trim().take(1).uppercase(Locale.getDefault())
                                .ifBlank { "?" },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldDark
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peerName,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChatInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(
                                        if (isOnline) ChatOnline else ChatFaint,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = presence,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 10.5.sp,
                                color = if (isOnline) EmeraldDark else ChatMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF1F5F9))
                )

                // ── what you can do ──────────────────────────────────────────
                ChatMenuRow(
                    icon = Icons.Default.Forum,
                    tint = EmeraldDark,
                    tintSoft = Color(0xFFE3F8EC),
                    label = "Ask for help",
                    onClick = onAskAgain,
                    testTag = "chat_menu_ask_help"
                )
                ChatMenuRow(
                    icon = Icons.Default.Edit,
                    tint = Color(0xFF3B82F6),
                    tintSoft = Color(0xFFE7F0FE),
                    label = "What's this chat about?",
                    onClick = onOpenIntent,
                    testTag = "chat_menu_intent"
                )
                ChatMenuRow(
                    icon = Icons.Default.Star,
                    tint = Color(0xFFF59E0B),
                    tintSoft = Color(0xFFFFF4E0),
                    label = "Rate this student",
                    onClick = onOpenRating,
                    testTag = "chat_menu_rate"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(1.dp)
                        .background(Color(0xFFF1F5F9))
                )

                // ── the one that deletes things ──────────────────────────────
                ChatMenuRow(
                    icon = Icons.Default.DeleteOutline,
                    tint = ChatError,
                    tintSoft = Color(0xFFFEEBEB),
                    label = "Clear chat",
                    onClick = onClearChat,
                    testTag = "chat_menu_clear"
                )
            }
        }
    }
}

/** One row of the ⋯ menu: tinted icon tile, label, generous tap height. */
@Composable
private fun ChatMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tintSoft: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tintSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (tint == ChatError) ChatError else ChatInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Themed confirmation card, used for destructive chat actions.
 *
 * Built instead of [AlertDialog] because the Material dialog could not carry the
 * app's language: an icon badge, a hairline card, capsule buttons and the
 * MaisonNeue typeface. The message being acted on is shown as a quote chip, so
 * there is no doubt which one is about to disappear.
 */
@Composable
private fun ChatConfirmDialog(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
    /** Quoted snippet of the affected message (delete only). */
    preview: String? = null,
    /** One short reassurance line under the copy, e.g. "Only on this phone". */
    note: String? = null,
    noteIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val accent = if (destructive) ChatError else EmeraldPrimary
    val accentSoft = if (destructive) Color(0xFFFEF2F2) else Color(0xFFECFDF5)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            border = BorderStroke(1.dp, ChatHairline),
            shadowElevation = 20.dp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon badge: a soft tinted disc, not a loud warning triangle.
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(accentSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChatInk,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = ChatMuted,
                    textAlign = TextAlign.Center
                )

                // The message itself, quoted, so the target is unmistakable.
                if (preview != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChatSheetCard)
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(accent.copy(alpha = 0.55f))
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = preview,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = ChatMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Scope pill: "only on this phone" vs "also in their chat".
                if (note != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accentSoft)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        noteIcon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            text = note,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel is the quiet choice: grey capsule, ink text.
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(15.dp),
                        color = ChatSheetCard,
                        border = BorderStroke(1.dp, ChatHairline),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChatInk
                            )
                        }
                    }
                    // Confirm carries the colour, so it reads as the real action.
                    Surface(
                        onClick = {
                            onDismiss()
                            onConfirm()
                        },
                        shape = RoundedCornerShape(15.dp),
                        color = accent,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = confirmLabel,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Actions available by long-pressing a bubble. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val hasText = message.body.isNotBlank()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ChatHairline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            // A compact preview so it is obvious which message is about to change.
            Text(
                text = when {
                    hasText -> message.body.take(90)
                    message.attachment?.isImage == true -> "Photo"
                    message.attachment != null -> message.attachment.name
                    else -> "Message"
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = ChatMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))

            MessageActionRow(
                label = "Reply",
                icon = Icons.AutoMirrored.Filled.Reply,
                tint = ChatInk,
                onClick = onReply
            )
            Spacer(Modifier.height(8.dp))

            if (hasText) {
                MessageActionRow(
                    label = "Copy text",
                    icon = Icons.Default.ContentCopy,
                    tint = ChatInk,
                    onClick = onCopy
                )
                Spacer(Modifier.height(8.dp))
            }
            MessageActionRow(
                label = if (message.isMine && message.serverId > 0L) {
                    "Delete for everyone"
                } else {
                    "Remove from this chat"
                },
                icon = Icons.Default.DeleteOutline,
                tint = ChatError,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun MessageActionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ChatSheetCard,
        border = BorderStroke(1.dp, ChatHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

/** Copy a message body to the system clipboard. */
private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Chat message", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

/**
 * True when a message is nothing but emoji (1–6 of them), so the bubble can show
 * them large instead of at body-text size.
 */
private fun isEmojiOnlyMessage(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > 32) return false
    var emojiCount = 0
    var index = 0
    while (index < trimmed.length) {
        val codePoint = trimmed.codePointAt(index)
        index += Character.charCount(codePoint)
        when {
            // Variation selector, ZWJ (family emoji) and the keycap mark.
            codePoint == 0xFE0F || codePoint == 0x200D || codePoint == 0x20E3 -> Unit
            // Regional indicators (country flags).
            codePoint in 0x1F1E6..0x1F1FF -> emojiCount++
            // Main pictographic blocks.
            codePoint in 0x1F300..0x1FAFF -> emojiCount++
            codePoint in 0x2600..0x27BF -> emojiCount++
            codePoint in 0x2B00..0x2BFF -> emojiCount++
            codePoint in 0x2190..0x21FF -> emojiCount++
            codePoint == 0x0020 || codePoint == 0x000A -> Unit
            else -> return false
        }
    }
    return emojiCount in 1..6
}

/** Quick-access emoji for the composer strip. */
private val CHAT_EMOJI = listOf(
    "😀", "😁", "😂", "🤣", "😊", "😍", "🥰", "😘", "😎", "🤔",
    "😅", "😢", "😭", "😡", "🥺", "😴", "🤗", "🙃", "😇", "🤩",
    "👍", "👎", "👏", "🙏", "💪", "🤝", "✌️", "👌", "🫶", "🤙",
    "❤️", "🧡", "💚", "💙", "💜", "🔥", "✨", "🎉", "💯", "⭐",
    "📚", "📝", "✏️", "📖", "🎓", "🧪", "🧮", "🖊️", "📐", "🔬"
)

/** File types offered by the document picker. */
private val CHAT_DOCUMENT_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "application/zip"
)

/** One row of the conversation list: a message, or a day separator. */
private sealed interface ChatRow {
    data class Day(val label: String, val key: String) : ChatRow
    data class Msg(val message: ChatMessage) : ChatRow
}

private fun startOfDayMillis(epochSeconds: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = epochSeconds * 1000L
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

/** "Today" / "Yesterday" / "12 Oct" / "12 Oct 2024". */
private fun chatDayLabel(dayMillis: Long): String {
    val now = System.currentTimeMillis()
    val today = startOfDayMillis(now / 1000L)
    val yesterday = startOfDayMillis((now / 1000L) - 86_400L)
    return when (dayMillis) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> {
            val sameYear = Calendar.getInstance().apply { timeInMillis = today }
                .get(Calendar.YEAR) ==
                Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.YEAR)
            SimpleDateFormat(if (sameYear) "d MMMM" else "d MMMM yyyy", Locale.US)
                .format(Date(dayMillis))
        }
    }
}

/** Insert a separator whenever the calendar day changes. */
private fun buildChatRows(messages: List<ChatMessage>): List<ChatRow> {
    val rows = ArrayList<ChatRow>(messages.size + 6)
    var currentDay: Long? = null
    for (message in messages) {
        val day = startOfDayMillis(message.sentAtEpoch)
        if (day != currentDay) {
            rows.add(ChatRow.Day(chatDayLabel(day), day.toString()))
            currentDay = day
        }
        rows.add(ChatRow.Msg(message))
    }
    return rows
}

/** Centred "Today" chip between message groups. */
@Composable
private fun DayDivider(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, ChatHairline)
        ) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChatMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/** Small preview of the file picked in the composer, with a remove button. */
@Composable
private fun AttachmentDraftCard(
    draft: ChatAttachmentDraft,
    onRemove: () -> Unit,
    uploadProgress: Float? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ChatHairline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (draft.isImage) {
                    AsyncImage(
                        model = File(draft.path),
                        contentDescription = draft.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (draft.isAudio) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        uploadProgress != null ->
                            "Encrypting and sending… ${(uploadProgress * 100).toInt()}%"
                        draft.isAudio ->
                            formatDuration(draft.durationMs) + "  ·  voice note"
                        else -> readableBytes(draft.size) + "  ·  ready to send"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = ChatMuted
                )
                if (uploadProgress != null) {
                    Spacer(Modifier.height(5.dp))
                    AttachmentProgressBar(progress = uploadProgress, onDark = false)
                }
            }
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = ChatMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * The file inside a bubble: a rounded photo, or a document card with its name
 * and size. Tapping opens it with whatever app can handle that type.
 */
@Composable
private fun ChatAttachmentContent(
    message: ChatMessage,
    attachment: ChatAttachment,
    downloadProgress: Int? = null
) {
    val context = LocalContext.current
    val path = if (message.attachmentAvailable) {
        ChatAttachmentStore.pathFor(context, attachment.id)
    } else {
        null
    }
    val file = path?.let { File(it) }?.takeIf { it.exists() }

    if (attachment.isAudio) {
        AudioNoteContent(
            message = message,
            attachment = attachment,
            file = file,
            downloadProgress = downloadProgress
        )
        return
    }

    if (attachment.isImage) {
        Box(
            modifier = Modifier
                .size(width = 202.dp, height = 150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (message.isMine) Color.White.copy(alpha = 0.18f) else Color(0xFFEEF2F6))
                .then(
                    if (file != null) {
                        Modifier.clickable {
                            openChatAttachment(context, file, attachment.mime, attachment.name)
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = attachment.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = if (message.isMine) Color.White.copy(alpha = 0.85f) else ChatMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            message.isPending -> "Sending photo…"
                            downloadProgress != null -> "$downloadProgress%"
                            else -> "Fetching photo…"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = if (message.isMine) Color.White.copy(alpha = 0.85f) else ChatMuted
                    )
                    if (downloadProgress != null) {
                        Spacer(Modifier.height(6.dp))
                        AttachmentProgressBar(
                            progress = downloadProgress / 100f,
                            onDark = message.isMine,
                            modifier = Modifier.width(96.dp)
                        )
                    }
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (message.isMine) Color.White.copy(alpha = 0.16f) else Color(0xFFF1F5F9))
                .then(
                    if (file != null) {
                        Modifier.clickable {
                            openChatAttachment(context, file, attachment.mime, attachment.name)
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (message.isMine) Color.White.copy(alpha = 0.22f) else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (message.isMine) Color.White else EmeraldDark,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = attachment.name,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (message.isMine) Color.White else ChatInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        file != null -> attachment.extensionLabel +
                            (attachment.readableSize.takeIf { it.isNotBlank() }?.let { "  ·  $it" } ?: "")
                        message.isPending -> "Sending…"
                        downloadProgress != null -> "Downloading $downloadProgress%"
                        else -> "Fetching…"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = if (message.isMine) Color.White.copy(alpha = 0.85f) else ChatMuted
                )
            }
        }
    }
}

/**
 * Playable voice note: play/pause circle, a progress line and the length.
 * Playback is local — the file is already decrypted in filesDir.
 */
@Composable
private fun AudioNoteContent(
    message: ChatMessage,
    attachment: ChatAttachment,
    file: File?,
    downloadProgress: Int? = null
) {
    val isMine = message.isMine
    var playing by remember(file?.absolutePath) { mutableStateOf(false) }
    var positionMs by remember(file?.absolutePath) { mutableStateOf(0L) }
    // Prepared on the first tap, not when the bubble scrolls into view: a chat
    // full of voice notes should not decode every clip up front.
    val playerState = remember(file?.absolutePath) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file?.absolutePath) {
        onDispose {
            runCatching {
                playerState.value?.let { active ->
                    if (active.isPlaying) active.stop()
                    active.release()
                }
            }
            playerState.value = null
        }
    }

    // Keep the progress line in sync, and stop the moment the clip ends.
    //
    // Two independent signals, because trusting only the recorded duration was
    // what left the button stuck on "pause" at the end of a note:
    //  1. the player's own completion callback (fires exactly at the end), and
    //  2. a watchdog on isPlaying() for players that never deliver it.
    LaunchedEffect(playing) {
        while (playing) {
            val active = playerState.value ?: break
            val reported = runCatching { active.currentPosition.toLong() }.getOrDefault(0L)
            positionMs = reported

            val finished = runCatching { !active.isPlaying }.getOrDefault(true) || run {
                val duration = attachment.durationMs.takeIf { it > 0 }
                    ?: runCatching { active.duration.toLong() }.getOrDefault(0L)
                duration > 0 && reported >= duration
            }

            if (finished) {
                playing = false
                positionMs = 0L
                runCatching {
                    active.pause()
                    active.seekTo(0)
                }
                break
            }
            delay(120)
        }
    }

    val total = attachment.durationMs.takeIf { it > 0 } ?: 1L
    val fraction = (positionMs.toFloat() / total).coerceIn(0f, 1f)
    val accent = if (isMine) Color.White else EmeraldPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 224.dp)
    ) {
        Surface(
            onClick = {
                val source = file
                if (source != null) {
                    val active = playerState.value ?: runCatching {
                        MediaPlayer().apply {
                            setDataSource(source.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                // End of clip: rewind and flip the button back to play.
                                playing = false
                                positionMs = 0L
                                runCatching { seekTo(0) }
                            }
                        }
                    }.getOrNull()?.also { playerState.value = it }

                    if (active != null) {
                        if (playing) {
                            runCatching { active.pause() }
                            playing = false
                        } else {
                            runCatching { active.start() }
                            playing = true
                        }
                    }
                }
            },
            shape = CircleShape,
            color = if (isMine) Color.White.copy(alpha = 0.18f) else Color(0xFFE9F9EF),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause voice note" else "Play voice note",
                    tint = if (file == null) {
                        if (isMine) Color.White.copy(alpha = 0.45f) else ChatFaint
                    } else {
                        accent
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Static bar chart standing in for a waveform: cheap and readable.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                modifier = Modifier.height(20.dp)
            ) {
                repeat(20) { index ->
                    val reached = index / 19f <= fraction
                    val baseHeight = 6 + ((index * 7) % 12)
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(baseHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    reached -> accent
                                    isMine -> Color.White.copy(alpha = 0.35f)
                                    else -> Color(0xFFCBD5E1)
                                }
                            )
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = when {
                    file == null && message.isPending -> "Sending voice note…"
                    file == null && downloadProgress != null -> "Downloading $downloadProgress%"
                    file == null -> "Fetching voice note…"
                    playing -> formatDuration(positionMs)
                    else -> attachment.readableDuration
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = if (isMine) Color.White.copy(alpha = 0.85f) else ChatMuted
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/** Open a decrypted attachment with the system viewer. */
private fun openChatAttachment(context: Context, file: File, mime: String, displayName: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime.ifBlank { "application/octet-stream" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, displayName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "No app on this phone can open that file.", Toast.LENGTH_SHORT).show()
    }
}

/** "1.4 MB" for the composer preview. */
private fun readableBytes(size: Long): String = when {
    size <= 0L -> ""
    size < 1024L -> "$size B"
    size < 1024L * 1024L -> String.format(Locale.US, "%.0f KB", size / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
}


/** Circular avatar: the uploaded profile picture when there is one, initials otherwise. */
@Composable
private fun ChatAvatar(
    peer: ChatPeer,
    size: androidx.compose.ui.unit.Dp,
    showPresence: Boolean,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarColorFor(peer.id)),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = peer.avatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "${peer.name} profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = peer.initials,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = (size.value / 2.6f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (showPresence && peer.isOnline) {
            // The dot is centred exactly on the circle's rim, on the 45° line
            // through the bottom-right corner: the rim sits at 0.8536 · size
            // from the top-left, so the dot starts half its own width earlier.
            // Placing it by the corner instead left it drifting inside the photo
            // at big sizes and outside the circle at small ones.
            val dot = size * 0.30f
            val dotStart = size * 0.8536f - dot / 2f
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = dotStart, y = dotStart)
                    .size(dot)
                    .clip(CircleShape)
                    .background(ringColor)
                    .padding(size * 0.05f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ChatOnline, CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    /** Used only to name the person in a reply quote. */
    peerName: String = "",
    downloadProgress: Int? = null,
    onLongPress: (ChatMessage) -> Unit = {},
    onSwipeReply: (ChatMessage) -> Unit = {},
    highlight: String? = null,
    isCurrentMatch: Boolean = false
) {
    val isMine = message.isMine
    val haptics = LocalHapticFeedback.current

    // ── swipe to reply ──────────────────────────────────────────────────────
    // Always left → right, whichever side the bubble sits on: dragging right is
    // the gesture people expect for "reply". Only horizontal drags are claimed,
    // so vertical scrolling of the list still works.
    val density = LocalDensity.current
    val maxSwipe = with(density) { 76.dp.toPx() }
    val triggerAt = with(density) { 52.dp.toPx() }
    val direction = 1f
    val swipe = remember(message.localId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var replyTriggered by remember(message.localId) { mutableStateOf(false) }

    val progress = (swipe.value / triggerAt).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Reply hint revealed behind the bubble as it is pulled.
        if (progress > 0.02f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 18.dp)
                    .size(34.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.7f + 0.3f * progress
                        scaleY = 0.7f + 0.3f * progress
                    }
                    .clip(CircleShape)
                    .background(EmeraldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 5.dp,
                bottomEnd = if (isMine) 5.dp else 18.dp
            ),
            color = if (isMine) ChatBubbleOutgoing else Color.White,
            border = when {
                // The bubble the ↑ / ↓ arrows landed on gets a green ring.
                isCurrentMatch -> BorderStroke(2.dp, EmeraldPrimary)
                isMine -> null
                else -> BorderStroke(1.dp, ChatHairline)
            },
            modifier = Modifier
                .widthIn(max = 300.dp)
                .offset { IntOffset((direction * swipe.value).roundToInt(), 0) }
                .pointerInput(message.localId) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (replyTriggered) {
                                    onSwipeReply(message)
                                }
                                replyTriggered = false
                                swipe.animateTo(0f, tween(180))
                            }
                        },
                        onDragCancel = {
                            replyTriggered = false
                            scope.launch { swipe.animateTo(0f, tween(180)) }
                        }
                    ) { _, dragAmount ->
                        scope.launch {
                            // dragAmount is positive when the finger moves right,
                            // so it must be ADDED: subtracting made the bubble react
                            // only to a right-to-left drag.
                            val next = (swipe.value + dragAmount * direction).coerceIn(0f, maxSwipe)
                            swipe.snapTo(next)
                            if (next >= triggerAt) {
                                if (!replyTriggered) {
                                    replyTriggered = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else if (replyTriggered) {
                                replyTriggered = false
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(message)
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (message.attachment != null) 6.dp else 10.dp,
                    vertical = if (message.attachment != null) 6.dp else 7.dp
                )
            ) {
                message.replyTo?.let { reply ->
                    ReplyQuoteBlock(reply = reply, isMine = isMine, peerName = peerName)
                    Spacer(Modifier.height(6.dp))
                }
                message.attachment?.let { attachment ->
                    ChatAttachmentContent(
                        message = message,
                        attachment = attachment,
                        downloadProgress = downloadProgress
                    )
                    if (message.body.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
                if (message.body.isNotBlank()) {
                    // Emoji-only messages are shown big, like every other chat app;
                    // everything else goes through the small Markdown renderer.
                    val emojiOnly = message.attachment == null && isEmojiOnlyMessage(message.body)
                    val body = if (emojiOnly) {
                        AnnotatedString(message.body)
                    } else {
                        ChatMarkdown.render(
                            source = message.body,
                            colors = chatMarkdownColors(isMine),
                            highlight = highlight
                        )
                    }
                    Text(
                        text = body,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = if (emojiOnly) 34.sp else 14.sp,
                        lineHeight = if (emojiOnly) 40.sp else 20.sp,
                        color = if (isMine) Color.White else ChatInk,
                        modifier = if (message.attachment != null) {
                            Modifier.padding(start = 4.dp, end = 4.dp)
                        } else {
                            Modifier
                        }
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatClock(message.sentAtEpoch),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.sp,
                        color = if (isMine) Color.White.copy(alpha = 0.82f) else ChatFaint
                    )
                    if (isMine && message.isPending) {
                        // Waiting for the relay — which is where a message typed
                        // without a connection sits until it can be delivered.
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Waiting to send",
                            tint = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (isMine && message.serverId == 0L) {
                        // The server refused it outright (never a network drop).
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Not sent",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(13.dp)
                        )
                    } else if (isMine) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = if (message.isRead) Color(0xFFBBF7D0) else Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * WhatsApp-style "typing…" indicator.
 *
 * A rounded bubble with one squared corner (no card, no elevation, no label —
 * just the dots) holding three dots. Each dot runs an infinite 1.2 s loop that
 * lifts it 7 px and brightens it at the 30 % mark, staggered by 160 ms so the
 * bounce travels from left to right.
 */
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing_indicator")
    // One shared clock; each dot reads a delayed slice of it.
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "typing_clock"
    )

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 5.dp
                )
            )
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                // 160 ms stagger → 0.1333 of the 1.2 s cycle.
                val phase = (clock - index * 0.1333f + 1f) % 1f
                // Arc that peaks exactly at the 30 % mark and rests otherwise.
                val lift = if (phase < 0.3f) {
                    kotlin.math.sin((phase / 0.3f).toDouble() * Math.PI).toFloat()
                } else {
                    0f
                }
                Box(
                    modifier = Modifier
                        .offset(y = (-7f * lift).dp)
                        .size(7.dp)
                        .background(ChatMuted.copy(alpha = 0.35f + 0.65f * lift), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SafetyNoticeBanner(onAcknowledge: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFECFDF5),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("chat_safety_banner")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Keep your chats safe",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Never share phone numbers, home address, school, bank details or " +
                    "passwords here. TheStudent will never ask for your password, and you " +
                    "can report a message from the chat menu.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF065F46)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onAcknowledge,
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldDark,
                    modifier = Modifier.testTag("chat_safety_ack_button")
                ) {
                    Text(
                        text = "I understand",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Loading placeholders
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatDirectorySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(7) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9EEF5))
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE9EEF5))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF3F8))
                    )
                }
            }
        }
    }
}

/**
 * Loader scrim drawn over the conversation list during the first open.
 *
 * It is a separate composable on purpose: calling `AnimatedVisibility` inline
 * from inside the list's `Box` makes Kotlin resolve the `ColumnScope` extension
 * from the enclosing `Column` and reject the implicit receiver. At file scope
 * the plain, scope-free overload is used.
 */
@Composable
private fun ConversationLoadingOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(220))
    ) {
        // Deliberately NO background here. A flat fill (it used to be #F8FAFC)
        // covered the app's gradient background art, so opening a chat flashed a
        // plain white page before the real conversation appeared. The list itself
        // is hidden with alpha instead, which keeps the backdrop identical.
        Box(modifier = Modifier.fillMaxSize()) {
            ChatLoadingState()
        }
    }
}

/**
 * Loading state shown while a conversation is opening.
 *
 * Compose port of the CSS "orbit" loader: the whole element is rotated 45°, and
 * two dots circle inside it on two different planes — one squashed vertically
 * (CSS `rotateX(70deg)`, green #91FF00), one squashed horizontally
 * (CSS `rotateY(70deg)`, red #FF3D00) with a 0.4 s delay.
 */
@Composable
private fun ChatLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OrbitLoader()
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Loading your conversation…",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            color = ChatMuted
        )
    }
}

@Composable
private fun OrbitLoader(size: androidx.compose.ui.unit.Dp = 48.dp) {
    val transition = rememberInfiniteTransition(label = "orbit_loader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    Canvas(
        modifier = Modifier
            .size(size)
            // The CSS wrapper tilts the whole loader by 45 degrees.
            .graphicsLayer { rotationZ = 45f }
    ) {
        val radius = this.size.minDimension * 0.34f
        val dotRadius = this.size.minDimension * 0.062f
        // cos/sin of 70deg on the third axis, i.e. how flat each orbit looks.
        val squash = 0.342f

        fun drawDot(degrees: Float, color: Color, squashX: Boolean) {
            val radians = Math.toRadians(degrees.toDouble())
            val x = (kotlin.math.cos(radians) * radius).toFloat()
            val y = (kotlin.math.sin(radians) * radius).toFloat()
            drawCircle(
                color = color,
                radius = dotRadius,
                center = Offset(
                    x = center.x + if (squashX) x * squash else x,
                    y = center.y + if (squashX) y else y * squash
                )
            )
        }

        // ::before — vertical squash, starts immediately.
        drawDot(degrees = angle, color = Color(0xFF91FF00), squashX = false)
        // ::after — horizontal squash, 0.4 s behind.
        drawDot(degrees = angle + 144f, color = Color(0xFFFF3D00), squashX = true)
    }
}
