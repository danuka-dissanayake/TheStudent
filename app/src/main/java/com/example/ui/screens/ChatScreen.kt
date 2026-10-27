package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.PureLightGreenButton
import com.example.viewmodel.TheStudentViewModel
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessageItem

data class OnlineStudent(
    val id: String,
    val name: String,
    val initials: String,
    val backgroundColor: Color,
    val isOnline: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: TheStudentViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Groups, Friends, Unread
    var showNewChatDialog by remember { mutableStateOf(false) }

    val onlineStudents = remember {
        listOf(
            OnlineStudent("1", "Dilshan", "DP", Color(0xFF0F172A)),
            OnlineStudent("2", "Nimali", "NR", Color(0xFF15803D)),
            OnlineStudent("3", "Sahan", "SF", Color(0xFF16A34A)),
            OnlineStudent("4", "Tharushi", "TS", Color(0xFF0F172A)),
            OnlineStudent("5", "Kasun", "KJ", Color(0xFF86EFAC)),
            OnlineStudent("6", "Ayesha", "AY", Color(0xFF0284C7))
        )
    }

    val initialConversations = remember {
        listOf(
            ChatConversation(
                id = "nimali_chat",
                name = "Nimali Rathnayake",
                lastMessage = "This is really helpful, thank you!",
                time = "9:50",
                unreadCount = 0,
                initials = "NR",
                avatarColor = Color(0xFF15803D),
                isTyping = true,
                messages = listOf(
                    ChatMessageItem(
                        id = "m1",
                        senderName = "Nimali Rathnayake",
                        text = "Hi! I saw your question in the Maths group. Need any help?",
                        timestamp = "9:44",
                        isUser = false
                    ),
                    ChatMessageItem(
                        id = "m2",
                        senderName = "You",
                        text = "Yes please! Can you explain Q5 from this paper?",
                        timestamp = "9:45",
                        isUser = true,
                        paperSnippetTitle = "A/L Combined Maths I – 2024",
                        paperSnippetSubtitle = "Question 5 • Paper I"
                    ),
                    ChatMessageItem(
                        id = "m3",
                        senderName = "Nimali Rathnayake",
                        text = "Sure. Start by writing the equation in terms of u. I made short notes on this, check them out.",
                        timestamp = "9:48",
                        isUser = false,
                        attachmentName = "Integration short notes.pdf",
                        attachmentSize = "1.2 MB",
                        attachmentPages = "4 pages"
                    ),
                    ChatMessageItem(
                        id = "m4",
                        senderName = "You",
                        text = "This is really helpful, thank you!",
                        timestamp = "9:50",
                        isUser = true
                    )
                )
            ),
            ChatConversation(
                id = "c1",
                name = "TheStudent AI",
                lastMessage = "Here is a step-by-step solution for Q7...",
                time = "9:41",
                unreadCount = 2,
                isAi = true,
                initials = "AI",
                avatarColor = Color(0xFF15803D),
                messages = listOf(
                    ChatMessageItem("m101", "TheStudent AI", "Hello! I am your AI study assistant. How can I help you with your past papers today?", "9:40", false),
                    ChatMessageItem("m102", "TheStudent AI", "Here is a step-by-step solution for Q7...", "9:41", false)
                )
            ),
            ChatConversation(
                id = "c2",
                name = "A/L Maths 2024",
                lastMessage = "Kasun: Anyone solved Q7 part (b)?",
                time = "9:32",
                unreadCount = 5,
                isGroup = true,
                initials = "AL",
                avatarColor = Color(0xFF0F172A),
                messages = listOf(
                    ChatMessageItem("m201", "Kasun", "Hey everyone, working on the 2024 paper.", "9:30", false),
                    ChatMessageItem("m202", "Kasun", "Anyone solved Q7 part (b)?", "9:32", false)
                )
            ),
            ChatConversation(
                id = "c3",
                name = "O/L Science Help",
                lastMessage = "Nimali: Sharing the 2023 marking scheme...",
                time = "8:50",
                unreadCount = 2,
                isGroup = true,
                initials = "OS",
                avatarColor = Color(0xFFBBF7D0),
                messages = listOf(
                    ChatMessageItem("m301", "Nimali", "Sharing the 2023 marking scheme for science.", "8:50", false)
                )
            ),
            ChatConversation(
                id = "c4",
                name = "Dilshan Perera",
                lastMessage = "Thanks, that makes sense now",
                time = "Yesterday",
                unreadCount = 0,
                initials = "DP",
                avatarColor = Color(0xFF16A34A),
                messages = listOf(
                    ChatMessageItem("m401", "Dilshan Perera", "Can you explain the physics vector question?", "Yesterday", false),
                    ChatMessageItem("m402", "You", "Sure, you need to resolve components along x and y axes.", "Yesterday", true),
                    ChatMessageItem("m403", "Dilshan Perera", "Thanks, that makes sense now", "Yesterday", false)
                )
            ),
            ChatConversation(
                id = "c5",
                name = "Physics Revision Club",
                lastMessage = "You: Let's meet at 7 pm for the quiz",
                time = "Yesterday",
                unreadCount = 0,
                isGroup = true,
                initials = "PR",
                avatarColor = Color(0xFF0F172A),
                messages = listOf(
                    ChatMessageItem("m501", "You", "Let's meet at 7 pm for the quiz", "Yesterday", true)
                )
            ),
            ChatConversation(
                id = "c8",
                name = "Tharushi Silva",
                lastMessage = "typing...",
                time = "9:15",
                unreadCount = 0,
                isTyping = false,
                initials = "TS",
                avatarColor = Color(0xFF0F172A),
                messages = listOf(
                    ChatMessageItem("m801", "Tharushi Silva", "Did you check the chemistry model paper?", "9:14", false)
                )
            )
        )
    }

    var conversations by remember { mutableStateOf(initialConversations) }

    if (uiState.activeChatConversation != null) {
        val activeConv = uiState.activeChatConversation!!
        ConversationDetailScreen(
            conversation = activeConv,
            onBack = { viewModel.closeChat() },
            onSendMessage = { text ->
                val updatedMsg = ChatMessageItem(
                    id = "msg-${System.currentTimeMillis()}",
                    senderName = "You",
                    text = text,
                    timestamp = "Just now",
                    isUser = true
                )
                val updatedConv = activeConv.copy(
                    lastMessage = "You: $text",
                    time = "Just now",
                    isTyping = false,
                    messages = activeConv.messages + updatedMsg
                )
                viewModel.openChat(updatedConv)
                conversations = conversations.map { if (it.id == updatedConv.id) updatedConv else it }
            },
            onClearChat = {
                val updatedConv = activeConv.copy(
                    lastMessage = "",
                    messages = emptyList()
                )
                viewModel.openChat(updatedConv)
                conversations = conversations.map { if (it.id == updatedConv.id) updatedConv else it }
            }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val nimali = conversations.find { it.name.contains("Nimali") }
                    if (nimali != null) {
                        viewModel.openChat(nimali)
                    } else {
                        showNewChatDialog = true
                    }
                },
                containerColor = PureLightGreenButton,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Start Chat", modifier = Modifier.size(24.dp))
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: "TheStudent" & "Chats" with proper status bar padding
            Surface(
                color = Color.White,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TheStudent",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Chats",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(
                                onClick = { /* Search trigger */ },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF0F172A))
                            }
                            IconButton(
                                onClick = { showNewChatDialog = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(PureLightGreenButton.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "New Group/Chat", tint = PureLightGreenButton)
                            }
                        }
                    }
                }
            }

            // Online Now Header & Horizontal Avatar Scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PureLightGreenButton, CircleShape)
                        )
                        Text(
                            text = "Online now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "24 students",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    Text(
                        text = "See all",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PureLightGreenButton,
                        modifier = Modifier.clickable { /* See all online */ }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(onlineStudents) { student ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                val existing = conversations.find { it.name.contains(student.name, ignoreCase = true) }
                                if (existing != null) {
                                    viewModel.openChat(existing)
                                } else {
                                    val newConv = ChatConversation(
                                        id = "chat-${System.currentTimeMillis()}",
                                        name = student.name,
                                        lastMessage = "Say hello to ${student.name}!",
                                        time = "Just now",
                                        initials = student.initials,
                                        avatarColor = student.backgroundColor,
                                        messages = listOf(ChatMessageItem("m", student.name, "Hi there! Let's discuss past paper questions.", "Just now", false))
                                    )
                                    conversations = listOf(newConv) + conversations
                                    viewModel.openChat(newConv)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .border(BorderStroke(2.5.dp, PureLightGreenButton), CircleShape)
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(student.backgroundColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = student.initials,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color.White, CircleShape)
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PureLightGreenButton, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = student.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("All", "Groups", "Friends", "Unread").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation List
            val filteredConversations = conversations.filter { conv ->
                val matchesFilter = when (selectedFilter) {
                    "Groups" -> conv.isGroup
                    "Friends" -> !conv.isGroup && !conv.isAi
                    "Unread" -> conv.unreadCount > 0
                    else -> true
                }
                val matchesSearch = searchQuery.isBlank() ||
                        conv.name.contains(searchQuery, ignoreCase = true) ||
                        conv.lastMessage.contains(searchQuery, ignoreCase = true)
                matchesFilter && matchesSearch
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredConversations, key = { it.id }) { chat ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openChat(chat) },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(chat.avatarColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (chat.isAi) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    Text(
                                        text = chat.initials,
                                        color = if (chat.avatarColor == Color(0xFFBBF7D0)) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chat.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = chat.time,
                                        fontSize = 12.sp,
                                        color = if (chat.unreadCount > 0) PureLightGreenButton else Color.Gray,
                                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chat.lastMessage,
                                        fontSize = 13.sp,
                                        color = if (chat.isTyping) PureLightGreenButton else Color(0xFF64748B),
                                        fontWeight = if (chat.isTyping) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (chat.unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(PureLightGreenButton, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = chat.unreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
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
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Chat or Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connect with fellow Sri Lankan students to discuss past papers, syllabi, and study groups.")
                    Button(
                        onClick = {
                            val newGroup = ChatConversation(
                                id = "group-${System.currentTimeMillis()}",
                                name = "Study Group #${(10..99).random()}",
                                lastMessage = "New study group created. Say hi!",
                                time = "Just now",
                                isGroup = true,
                                initials = "SG",
                                avatarColor = Color(0xFF0F172A),
                                messages = listOf(ChatMessageItem("mg1", "System", "Group created successfully. Invite friends to collaborate!", "Just now", false))
                            )
                            conversations = listOf(newGroup) + conversations
                            showNewChatDialog = false
                            viewModel.openChat(newGroup)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PureLightGreenButton)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Study Group")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversation: ChatConversation,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit = {}
) {
    var inputMessage by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    var showFeedbackToast by remember { mutableStateOf(false) }

    // Intercept hardware/gesture back press to return to chat list
    BackHandler(onBack = onBack)

    // Main layout container: WindowInsets handles navigation bar & software keyboard with ZERO gap
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F5))
            .windowInsetsPadding(
                WindowInsets.navigationBars
                    .only(WindowInsetsSides.Bottom)
                    .union(WindowInsets.ime)
            )
    ) {
        // 1. Top Bar Header
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF15803D), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(conversation.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF16A34A), CircleShape)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = conversation.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (conversation.isTyping) "typing..." else "Online",
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 3-Dots Menu Dropdown
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rate Student") },
                            leadingIcon = {
                                Icon(Icons.Default.StarRate, contentDescription = null, tint = Color(0xFFF59E0B))
                            },
                            onClick = {
                                showMenu = false
                                showRateDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Chat", color = Color(0xFFDC2626)) },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626))
                            },
                            onClick = {
                                showMenu = false
                                showClearConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }

        // 2. Chat Messages Area (takes all remaining vertical space)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Today pill header
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE2E8F0).copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(conversation.messages) { msg ->
                val isMe = msg.isUser
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        color = if (isMe) PureLightGreenButton else Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isMe && conversation.isGroup) {
                                Text(
                                    text = msg.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }

                            // Paper snippet card if attached to message
                            if (msg.paperSnippetTitle != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "M",
                                                color = PureLightGreenButton,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = msg.paperSnippetTitle,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (msg.paperSnippetSubtitle != null) {
                                                Text(
                                                    text = msg.paperSnippetSubtitle,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // PDF Attachment card (clickable for automatic download, no separate download button icon)
                            if (msg.attachmentName != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Automatic file download / open action
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = "PDF",
                                                tint = PureLightGreenButton,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = msg.attachmentName,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${msg.attachmentSize} • ${msg.attachmentPages}",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (msg.text.isNotBlank()) {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.sp,
                                    color = if (isMe) Color.White else Color(0xFF0F172A)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = msg.timestamp,
                                    fontSize = 10.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
                                )
                                if (isMe) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Typing indicator bubble (appears ONLY when conversation.isTyping is true)
            if (conversation.isTyping) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF94A3B8), CircleShape))
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF94A3B8), CircleShape))
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF94A3B8), CircleShape))
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Message Input Bar (docked flush at the bottom of the Column with 0px gap above keyboard)
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { /* Attach paper/notes */ }) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach",
                        tint = Color(0xFF64748B)
                    )
                }
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = {
                        val recipientName = conversation.name.split(" ").firstOrNull() ?: conversation.name
                        Text(
                            text = "Message $recipientName...",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = PureLightGreenButton,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedContainerColor = Color.White
                    )
                )
                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            onSendMessage(inputMessage.trim())
                            inputMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(PureLightGreenButton, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Rate Student Dialog
    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = {
                Text(
                    text = "Rate ${conversation.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "How helpful was ${conversation.name}'s explanation with questions & past papers?",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    // Star rating selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { ratingStars = i },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (i <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $i",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        placeholder = { Text("Add optional feedback...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRateDialog = false
                        ratingComment = ""
                        showFeedbackToast = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureLightGreenButton)
                ) {
                    Text("Submit Rating")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to clear all messages with ${conversation.name}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearChat()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear All Messages")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
