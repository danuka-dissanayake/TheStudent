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
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.local.ChatAttachmentStore
import com.example.data.model.ChatAttachment
import com.example.data.model.ChatAttachmentDraft
import com.example.data.model.ChatMessage
import com.example.data.model.ChatPeer
import com.example.data.model.ChatPeerProfile
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
@Composable
fun ChatScreen(
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
            onStopVoiceNote = { viewModel.stopVoiceNote() },
            onCancelVoiceNote = { viewModel.cancelVoiceNote() },
            onStopTyping = { viewModel.stopChatTyping() },
            onOpenProfile = { viewModel.openChatProfile(it) },
            modifier = modifier
        )
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
                    Spacer(Modifier.height(20.dp))
                    // Only the Message button: there is no block / mute on the
                    // relay yet, so the "..." of the mockup would be a dead tap.
                    Surface(
                        onClick = onMessage,
                        shape = RoundedCornerShape(50),
                        color = EmeraldPrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("chat_profile_message")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 26.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = "Message",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // ── education ────────────────────────────────────────────────
                if (profile.hasEducation) {
                    ProfileCard(title = "Education") {
                        if (profile.targetExam.isNotBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.MenuBook,
                                tint = EmeraldPrimary,
                                tintSoft = Color(0xFFE3F8EC),
                                label = "Target exam",
                                value = profile.targetExam
                            )
                        }
                        if (profile.streamOrDegree.isNotBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.School,
                                tint = Color(0xFF0EA5E9),
                                tintSoft = Color(0xFFE0F2FE),
                                label = "Stream / degree",
                                value = profile.streamOrDegree
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

                if (isLoading) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Loading details…",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.5.sp,
                            color = ChatFaint
                        )
                    }
                }
            }
        }
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
                modifier = Modifier
                    .clip(CircleShape)
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
                Text(
                    text = preview,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    color = if (peer.isTyping) EmeraldPrimary else ChatMuted,
                    fontWeight = if (peer.isTyping) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    onStopVoiceNote: () -> Unit,
    onCancelVoiceNote: () -> Unit,
    onStopTyping: () -> Unit,
    onOpenProfile: (ChatPeer) -> Unit,
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
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onOpenProfile(peer) }
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
                            maxLines = 1
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
                            color = Color.White,
                            border = BorderStroke(1.dp, ChatHairline),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Conversation options",
                                    tint = ChatInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear chat", fontFamily = MaisonNeueFontFamily) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = ChatError
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showClearDialog = true
                                }
                            )
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
                            }
                        }
                    }

                    } // end hidden list content

                    // Overlay inside the list container: the conversation is
                    // already parked at the bottom underneath when this fades out.
                    ConversationLoadingOverlay(visible = !conversationSettled)
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
                    VoiceRecordingBar(
                        elapsedMs = state.voiceNoteElapsedMs,
                        onCancel = onCancelVoiceNote,
                        onStop = onStopVoiceNote,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp)
                    )
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
@Composable
private fun VoiceRecordingBar(
    elapsedMs: Long,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = (elapsedMs / 1000L)
    val clock = String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)

    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ChatHairline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onCancel,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Cancel recording",
                        tint = ChatError,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))

            // Pulsing dot + timer, so it is obvious that recording is live.
            val transition = rememberInfiniteTransition(label = "recording_pulse")
            val dotAlpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "recording_dot"
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .alpha(dotAlpha)
                    .background(ChatError, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = clock,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChatInk
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Recording voice note…",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = ChatMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                onClick = onStop,
                shape = CircleShape,
                color = EmeraldPrimary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Stop and keep the recording",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
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
            Box(
                modifier = Modifier
                    .size(size * 0.30f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(ringColor)
                    .padding(size * 0.055f),
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
