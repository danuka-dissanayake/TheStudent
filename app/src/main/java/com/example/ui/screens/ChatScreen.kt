package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.ChatPeer
import com.example.data.model.ChatThread
import com.example.ui.components.TheStudentAppBackground
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import com.example.viewmodel.ChatUiState
import com.example.viewmodel.TheStudentViewModel
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
            modifier = modifier
        )
        return
    }

    ChatDirectoryScreen(
        state = chatState,
        onOpenChat = { viewModel.openChat(it) },
        onRefresh = { viewModel.refreshChatDirectoryNow() },
        onDismissError = { viewModel.dismissChatError() },
        onAcknowledgeSafety = { viewModel.acknowledgeChatSafetyNotice() },
        onRestoreHistory = { password -> viewModel.restoreChatHistory(password) },
        onSkipHistory = { viewModel.skipChatHistory() },
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Directory (list of registered students)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChatDirectoryScreen(
    state: ChatUiState,
    onOpenChat: (ChatPeer) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onAcknowledgeSafety: () -> Unit,
    onRestoreHistory: (String?) -> Unit,
    onSkipHistory: () -> Unit,
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

                // ── "you have a backup chat history" card ────────────────────
                AnimatedVisibility(
                    visible = state.showRestorePrompt || state.isRestoring,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BackupRestoreCard(
                        state = state,
                        onRestore = onRestoreHistory,
                        onSkip = onSkipHistory
                    )
                }

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
                                    onClick = { onOpenChat(thread.peer) }
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

/**
 * "You have a backup chat history" card.
 *
 * Shown once (per install) when the server still holds messages for this
 * account — which is exactly what happens after a reinstall. Restoring walks
 * every conversation and rebuilds it locally; skipping deletes the history from
 * the database as well.
 *
 * Premium look: emerald gradient, soft glow, floating icon, animated entry, and
 * the rotating cube loader + progress bar while the restore runs.
 */
@Composable
private fun BackupRestoreCard(
    state: ChatUiState,
    onRestore: (String?) -> Unit,
    onSkip: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    val backup = state.historyBackup

    val glow by rememberInfiniteTransition(label = "backup_glow").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backup_glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF059669))
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF34D399).copy(alpha = glow),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("chat_backup_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFFBBF7D0),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isRestoring) "Restoring your chats" else "You have a backup chat history",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            state.isRestoring -> "Please keep the app open for a moment."
                            backup.messageCount > 0 -> {
                                "${backup.messageCount} message" +
                                    (if (backup.messageCount == 1) "" else "s") +
                                    " in ${backup.threadCount} chat" +
                                    (if (backup.threadCount == 1) "" else "s") +
                                    " from your previous install."
                            }
                            else -> "Your encrypted messages are stored safely on our server."
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFFD1FAE5)
                    )
                }
            }

            if (state.isRestoring) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CubeDotsLoader(size = 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = { state.restoreProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF91FF00),
                            trackColor = Color.White.copy(alpha = 0.18f),
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${(state.restoreProgress.coerceIn(0f, 1f) * 100).toInt()}%",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBBF7D0)
                        )
                    }
                }
            } else {
                if (state.restoreNeedsPassword) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Account password to unlock the backup",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF91FF00),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = ChatInk,
                            unfocusedTextColor = ChatInk
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_backup_password_field")
                    )
                }

                state.restoreError?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        color = Color(0xFFFECACA)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = {
                            onRestore(if (state.restoreNeedsPassword) password else null)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF91FF00),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_backup_restore_button")
                    ) {
                        Text(
                            text = "Restore messages",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF052E16),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = onSkip,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("chat_backup_skip_button")
                    ) {
                        Text(
                            text = "Skip",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Skipping deletes this history from our servers. " +
                        "Your messages stay end-to-end encrypted either way.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFFA7F3D0)
                )
            }
        }
    }
}

/**
 * Compose port of the CSS "rotating cube" loader: two counter-orbiting dots on
 * flattened (rotateX / rotateY 70°) ellipses, the whole thing spun 45°.
 */
@Composable
private fun CubeDotsLoader(
    size: Dp = 48.dp,
    primary: Color = Color(0xFF91FF00),
    secondary: Color = Color(0xFFFF7A45)
) {
    val transition = rememberInfiniteTransition(label = "cube_loader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cube_loader_angle"
    )

    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = 45f }
    ) {
        val radius = this.size.minDimension / 2f - 3.dp.toPx()
        val flatten = 0.34 // cos(70°), kept as Double for the trig maths
        val dotRadius = 2.6.dp.toPx()
        val centre = Offset(this.size.width / 2f, this.size.height / 2f)

        fun drawDot(position: Offset, color: Color) {
            drawCircle(color = color.copy(alpha = 0.25f), radius = dotRadius * 2.1f, center = position)
            drawCircle(color = color, radius = dotRadius, center = position)
        }

        val r = radius.toDouble()

        // Orbit 1 — rotateX(70deg): flattened vertically.
        val a1 = Math.toRadians(angle.toDouble())
        drawDot(
            Offset(
                centre.x + (kotlin.math.cos(a1) * r).toFloat(),
                centre.y + (kotlin.math.sin(a1) * r * flatten).toFloat()
            ),
            primary
        )

        // Orbit 2 — rotateY(70deg): flattened horizontally, delayed 0.4 s.
        val a2 = Math.toRadians((angle + 144f).toDouble())
        drawDot(
            Offset(
                centre.x + (kotlin.math.cos(a2) * r * flatten).toFloat(),
                centre.y + (kotlin.math.sin(a2) * r).toFloat()
            ),
            secondary
        )
    }
}

@Composable
private fun ChatThreadRow(
    thread: ChatThread,
    onClick: () -> Unit
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
            ChatAvatar(peer = peer, size = 52.dp, showPresence = true, ringColor = Color.White)
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
                } else if (peer.isOnline) {
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
    modifier: Modifier = Modifier
) {
    val peer = state.activePeer ?: return
    var input by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Follow the conversation as new messages arrive.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
                    ChatAvatar(peer = peer, size = 42.dp, showPresence = true, ringColor = Color.White)
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
                            text = peer.presenceLabel + "  ·  🔒 end-to-end encrypted",
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

                // ── messages ─────────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.isLoadingMessages && state.messages.isEmpty() -> {
                            ChatMessageSkeleton()
                        }

                        state.messages.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                items(state.messages, key = { it.id }) { message ->
                                    ChatBubble(message = message)
                                }

                                if (peer.isTyping) {
                                    item { TypingIndicator() }
                                }
                            }
                        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ChatHairline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("chat_input_field")
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            val text = input.trim()
                            if (text.isNotEmpty()) {
                                onSend(text)
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !state.isSending && peer.canReceiveEncrypted,
                        shape = CircleShape,
                        color = if (input.isNotBlank() && peer.canReceiveEncrypted) {
                            EmeraldPrimary
                        } else {
                            Color(0xFFCBD5E1)
                        },
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
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear this chat?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your copy of the conversation with ${peer.name} will be removed. " +
                        "The messages stay in their chat."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearChat()
                    }
                ) {
                    Text("Clear chat", color = ChatError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pieces
// ─────────────────────────────────────────────────────────────────────────────

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

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isMine = message.isMine
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
            color = if (isMine) EmeraldPrimary else Color.White,
            border = if (isMine) null else BorderStroke(1.dp, ChatHairline),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.body,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isMine) Color.White else ChatInk
                )
                Spacer(Modifier.height(4.dp))
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
                    if (isMine) {
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

@Composable
private fun ChatMessageSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) { index ->
            val isMine = index % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isMine) 0.55f else 0.68f)
                        .height(if (index % 2 == 0) 46.dp else 64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isMine) Color(0xFFD9F2E6) else Color(0xFFE9EEF5))
                )
            }
        }
    }
}
