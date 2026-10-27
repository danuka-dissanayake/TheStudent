package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import com.example.data.local.ChatAttachmentStore
import com.example.data.model.ChatAttachment
import com.example.data.model.ChatAttachmentDraft
import com.example.data.model.ChatMessage
import com.example.data.model.ChatPeer
import com.example.data.model.ChatThread
import com.example.ui.components.TheStudentAppBackground
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import com.example.viewmodel.ChatUiState
import com.example.viewmodel.TheStudentViewModel
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
            onPickAttachment = { uri -> viewModel.stageChatAttachment(uri) },
            onClearAttachment = { viewModel.clearStagedChatAttachment() },
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
    onPickAttachment: (Uri) -> Unit,
    onClearAttachment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val peer = state.activePeer ?: return
    var input by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Gallery and document pickers. Neither needs a runtime permission: the
    // system picker hands back a one-off readable Uri.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPickAttachment) }
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPickAttachment) }

    // Day separators are computed once per message list, not per recomposition.
    val rows = remember(state.messages) { buildChatRows(state.messages) }

    // Follow the conversation as new messages arrive.
    LaunchedEffect(rows.size) {
        if (rows.isNotEmpty()) {
            listState.animateScrollToItem(rows.lastIndex)
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
                                items(
                                    items = rows,
                                    key = { row ->
                                        when (row) {
                                            is ChatRow.Day -> "day-${row.key}"
                                            is ChatRow.Msg -> row.message.localId
                                        }
                                    }
                                ) { row ->
                                    when (row) {
                                        is ChatRow.Day -> DayDivider(row.label)
                                        is ChatRow.Msg -> ChatBubble(message = row.message)
                                    }
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

                // Picked file, previewed small above the input until it is sent.
                state.pendingAttachment?.let { draft ->
                    AttachmentDraftCard(
                        draft = draft,
                        onRemove = onClearAttachment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box {
                        Surface(
                            onClick = { showAttachMenu = true },
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
                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Photo", fontFamily = MaisonNeueFontFamily, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldDark)
                                },
                                onClick = {
                                    showAttachMenu = false
                                    imagePicker.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Document", fontFamily = MaisonNeueFontFamily, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldDark)
                                },
                                onClick = {
                                    showAttachMenu = false
                                    documentPicker.launch(CHAT_DOCUMENT_TYPES)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
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
                    val canSend = (input.isNotBlank() || state.pendingAttachment != null) &&
                        !state.isSending && peer.canReceiveEncrypted
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
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ChatHairline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(11.dp))
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
                    text = readableBytes(draft.size) + "  ·  ready to send",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = ChatMuted
                )
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
private fun ChatAttachmentContent(message: ChatMessage, attachment: ChatAttachment) {
    val context = LocalContext.current
    val path = if (message.attachmentAvailable) {
        ChatAttachmentStore.pathFor(context, attachment.id)
    } else {
        null
    }
    val file = path?.let { File(it) }?.takeIf { it.exists() }

    if (attachment.isImage) {
        Box(
            modifier = Modifier
                .size(width = 216.dp, height = 162.dp)
                .clip(RoundedCornerShape(12.dp))
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
                        text = if (message.isPending) "Sending photo…" else "Fetching photo…",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = if (message.isMine) Color.White.copy(alpha = 0.85f) else ChatMuted
                    )
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 232.dp)
                .clip(RoundedCornerShape(12.dp))
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
                .padding(horizontal = 10.dp, vertical = 9.dp)
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
                message.attachment?.let { attachment ->
                    ChatAttachmentContent(message = message, attachment = attachment)
                    if (message.body.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (isMine) Color.White else ChatInk
                    )
                }
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
                    if (isMine && message.isPending) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Sending",
                            tint = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.size(12.dp)
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
