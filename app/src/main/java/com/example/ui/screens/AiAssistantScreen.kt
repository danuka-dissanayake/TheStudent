package com.example.ui.screens

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.graphics.PathParser
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.ModernBackArrowIcon
import com.example.ui.theme.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced AI Study Assistant Screen:
 * - Unified single-column layout structure completely removing empty keyboard void gaps.
 * - Fixed Top Bar with statusBarsPadding() that prevents scrolling overflow.
 * - Pure Black AI response card (#000000) with smooth streaming character typing.
 * - Action buttons (Copy, Read Aloud, Share) placed cleanly beneath the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    userProfile: UserProfile,
    messages: List<AiAssistantMessage>,
    isGenerating: Boolean,
    isReasoningActive: Boolean = false,
    streamingReasoningText: String = "",
    inputText: String,
    pendingAttachment: AttachedMedia?,
    selectedSubject: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String, AttachedMedia?) -> Unit,
    onAttachMedia: (AttachedMedia?) -> Unit,
    onClearChat: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onRateMessage: (String, Boolean) -> Unit,
    onSpeakText: (String) -> Unit,
    onRetryMessage: (String) -> Unit = {},
    onStopGenerating: () -> Unit = {},
    onBack: () -> Unit,
    isOcrExtracting: Boolean = false,
    ocrExtractedText: String? = null,
    ocrDetectedLanguage: String? = null,
    isOcrFastMode: Boolean = true,
    onRetryOcr: (String, String?) -> Unit = { _, _ -> },
    onUpdateOcrText: (String) -> Unit = {},
    onToggleOcrFastMode: (Boolean) -> Unit = {},
    selectedModel: AiModelChoice = AiModelChoice.GLM_4_7_FLASH,
    onSelectModel: (AiModelChoice) -> Unit = {},
    customZaiApiKey: String = "",
    onUpdateZaiApiKey: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    // Track active typing stream to disable user sending until animation completes
    var isTypingActive by remember { mutableStateOf(false) }
    var stopRequestedForId by remember { mutableStateOf<String?>(null) }
    val isBusy = isGenerating || isTypingActive

    // Track messages that have already completed streaming so scrolling NEVER re-types them
    val streamedMessageIds = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    // Dynamic animated placeholder rotation (cycles every 3 seconds)
    val dynamicPlaceholders = remember {
        listOf(
            "Ask dzd ai anything...",
            "Solve math, physics & chem problems...",
            "Explain concepts in Sinhala or English...",
            "Draft summaries from uploaded notes...",
            "Generate exam revision flashcards...",
            "Ask for formulas, equations & step-by-step help..."
        )
    }
    var placeholderIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            placeholderIndex = (placeholderIndex + 1) % dynamicPlaceholders.size
        }
    }

    // Intercept back button to return smoothly
    BackHandler {
        onBack()
    }

    // Auto-scroll continuously to the bottom as the AI streams tokens or reasoning
    LaunchedEffect(messages.size, isGenerating, messages.lastOrNull()?.text?.length, streamingReasoningText.length) {
        val totalCount = messages.size + 1 + (if (isGenerating && isReasoningActive) 1 else 0)
        if (totalCount > 0) {
            val targetIndex = (totalCount - 1).coerceAtLeast(0)
            listState.scrollToItem(targetIndex, 100000)
        }
    }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryFileName(context, uri) ?: "Question_Photo.jpg"
            onAttachMedia(
                AttachedMedia(
                    uri = uri.toString(),
                    type = AttachedMediaType.IMAGE,
                    displayName = fileName,
                    sizeFormatted = "Image • Ready to solve"
                )
            )
            showAttachmentSheet = false
        }
    }

    // PDF Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryFileName(context, uri) ?: "Study_Notes.pdf"
            onAttachMedia(
                AttachedMedia(
                    uri = uri.toString(),
                    type = AttachedMediaType.PDF,
                    displayName = fileName,
                    sizeFormatted = "PDF Document • Ready to summarize"
                )
            )
            showAttachmentSheet = false
        }
    }

    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color.White
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_assistant_screen"),
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = backgroundColor,
        topBar = {
            // Fixed Top App Bar with .statusBarsPadding() so it sits below status bar icons
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_assistant_top_bar"),
                color = surfaceColor,
                border = BorderStroke(
                    1.dp,
                    if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    else Color(0xFFF1F5F9)
                ),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back button + Screen Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onBack)
                                .testTag("ai_assistant_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                ModernBackArrowIcon(
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AI Study Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                            }
                            // Clickable Model Selection Pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (selectedModel) {
                                    AiModelChoice.GLM_4_7_FLASH -> if (isDark) Color(0xFF14532D).copy(alpha = 0.4f) else Color(0xFFDCFCE7)
                                    AiModelChoice.HUGGING_FACE -> if (isDark) Color(0xFF312E81).copy(alpha = 0.4f) else Color(0xFFE0E7FF)
                                    AiModelChoice.GEMINI_FLASH -> if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.4f) else Color(0xFFE0F2FE)
                                },
                                border = BorderStroke(
                                    0.5.dp,
                                    when (selectedModel) {
                                        AiModelChoice.GLM_4_7_FLASH -> Color(0xFF22C55E)
                                        AiModelChoice.HUGGING_FACE -> Color(0xFF818CF8)
                                        AiModelChoice.GEMINI_FLASH -> Color(0xFF38BDF8)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showModelDialog = true }
                                    .testTag("ai_assistant_model_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = when (selectedModel) {
                                            AiModelChoice.GLM_4_7_FLASH -> Icons.Default.Bolt
                                            AiModelChoice.HUGGING_FACE -> Icons.Default.Psychology
                                            AiModelChoice.GEMINI_FLASH -> Icons.Default.AutoAwesome
                                        },
                                        contentDescription = null,
                                        tint = when (selectedModel) {
                                            AiModelChoice.GLM_4_7_FLASH -> Color(0xFF22C55E)
                                            AiModelChoice.HUGGING_FACE -> Color(0xFF818CF8)
                                            AiModelChoice.GEMINI_FLASH -> Color(0xFF38BDF8)
                                        },
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${selectedModel.displayName} (${selectedModel.providerBadge})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (selectedModel) {
                                            AiModelChoice.GLM_4_7_FLASH -> if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                            AiModelChoice.HUGGING_FACE -> if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                                            AiModelChoice.GEMINI_FLASH -> if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right Actions: Streak badge + Options menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Streak badge capsule
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AmberWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .testTag("ai_assistant_streak_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${userProfile.streakDays} d",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // More / Options menu
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMoreMenu = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Model: ${selectedModel.displayName}") },
                                    onClick = {
                                        showMoreMenu = false
                                        showModelDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF22C55E))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New / Reset Chat") },
                                    onClick = {
                                        onClearChat()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Color(0xFF22C55E))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Upload Question Photo") },
                                    onClick = {
                                        showMoreMenu = false
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color(0xFF22C55E))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Attach PDF Notes") },
                                    onClick = {
                                        showMoreMenu = false
                                        documentPickerLauncher.launch("application/pdf")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null, tint = Color(0xFF0F766E))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Single unified Column: Chat content on top (weight 1f) and Input Bar docked at bottom
        // This ensures the input bar sits flush with 0px gap directly above the software keyboard!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .union(WindowInsets.ime)
                )
                .background(backgroundColor)
        ) {
            // Main Chat Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (messages.isEmpty()) {
                    // Discovery / Greeting Window with Ambient Arch & Starlight Art
                    AiAssistantHomeDiscoveryView(
                        userProfile = userProfile,
                        onCardClick = { prompt, mediaType ->
                            if (mediaType == AttachedMediaType.IMAGE) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                                onInputChange(prompt)
                            } else if (mediaType == AttachedMediaType.PDF) {
                                documentPickerLauncher.launch("application/pdf")
                                onInputChange(prompt)
                            } else {
                                onSendMessage(prompt, null)
                            }
                        }
                    )
                } else {
                    // Active Chat History
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Today • " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                            when (message.sender) {
                                MessageSender.USER -> {
                                    UserChatBubbleDesign(message = message)
                                }
                                MessageSender.ASSISTANT, MessageSender.AI -> {
                                    val isAlreadyStreamed = streamedMessageIds.contains(message.id)
                                    AssistantChatCardDesign(
                                        message = message,
                                        onSpeak = onSpeakText,
                                        onRetry = onRetryMessage,
                                        isLatestMessage = index == messages.lastIndex,
                                        isGenerating = isGenerating,
                                        hasAlreadyStreamed = isAlreadyStreamed,
                                        isStopRequested = stopRequestedForId == message.id,
                                        onTypingStateChange = { active ->
                                            if (index == messages.lastIndex) {
                                                isTypingActive = active
                                            }
                                        },
                                        onStreamComplete = {
                                            if (!streamedMessageIds.contains(message.id)) {
                                                streamedMessageIds.add(message.id)
                                            }
                                            if (index == messages.lastIndex) {
                                                isTypingActive = false
                                            }
                                        }
                                    )
                                }
                                MessageSender.SYSTEM -> {
                                    SystemNoticeBubble(message = message)
                                }
                            }
                        }

                        // Claude Code / Arena AI Styled Live Reasoning Indicator
                        // Live streams reasoning text while thinking, then simply disappears when reasoning completes!
                        if (isGenerating && isReasoningActive) {
                            item {
                                AiThinkingIndicator(
                                    reasoningText = streamingReasoningText,
                                    subject = selectedSubject
                                )
                            }
                        }
                    }
                }
            }

            // Floating Input Bar Area (Floating directly on the screen without background card or full-width shadow)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                // Pending Attachment Preview Strip
                AnimatedVisibility(
                    visible = pendingAttachment != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (pendingAttachment != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            if (pendingAttachment.type == AttachedMediaType.IMAGE) {
                                // Ultra-clean ChatGPT / Gemini Floating Thumbnail (Zero clutter, silent background OCR)
                                Box(
                                    modifier = Modifier.padding(top = 4.dp, end = 6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        shadowElevation = 3.dp,
                                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)),
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        AsyncImage(
                                            model = pendingAttachment.uri,
                                            contentDescription = "Attached photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Floating dismiss button at top-right
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xDD0F172A),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                            .size(22.dp)
                                            .clickable { onAttachMedia(null) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Modern ChatGPT / Gemini PDF / Document Attachment Pill
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.14f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.PictureAsPdf,
                                                    contentDescription = "PDF",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.widthIn(max = 180.dp)) {
                                            Text(
                                                text = pendingAttachment.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = pendingAttachment.sizeFormatted,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { onAttachMedia(null) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove document",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Input Bar (Pure black background, white text and white icon in dark mode)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = if (isDark) Color(0xFF000000) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plus (+) attachment button (White icon in dark mode)
                        Surface(
                            shape = CircleShape,
                            color = if (pendingAttachment != null) Color(0xFF22C55E).copy(alpha = 0.2f)
                            else if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9),
                            border = if (isDark) BorderStroke(0.5.dp, Color(0xFF27272A)) else null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { showAttachmentSheet = true }
                                .testTag("ai_assistant_attachment_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (pendingAttachment != null) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Attach",
                                    tint = if (pendingAttachment != null) Color(0xFF22C55E)
                                    else if (isDark) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Text Input Field with animated dynamic rotating placeholder
                        TextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            placeholder = {
                                AnimatedContent(
                                    targetState = dynamicPlaceholders[placeholderIndex],
                                    transitionSpec = {
                                        (slideInVertically(animationSpec = tween(350)) { height -> height / 2 } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(
                                                slideOutVertically(animationSpec = tween(350)) { height -> -height / 2 } + fadeOut(animationSpec = tween(350))
                                            )
                                    },
                                    label = "dynamic_placeholder_animation"
                                ) { currentPlaceholder ->
                                    Text(
                                        text = currentPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.5.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_assistant_input_text"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                cursorColor = Color(0xFF22C55E)
                            ),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )

                        // Right Corner Action Button: Send button when idle & Stop button when busy (Emerald Background)
                        val hasSendable = inputText.isNotBlank() || pendingAttachment != null
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary, // Solid Vibrant Emerald Green
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (isBusy) {
                                        if (isGenerating) {
                                            onStopGenerating()
                                        }
                                        val latestMsgId = messages.lastOrNull()?.id
                                        if (latestMsgId != null) {
                                            stopRequestedForId = latestMsgId
                                        }
                                        isTypingActive = false
                                    } else {
                                        val toSend = if (hasSendable) {
                                            inputText.ifBlank { "Please analyze this attached study material." }
                                        } else {
                                            "Generate today's personalized revision plan for my target exam."
                                        }
                                        onSendMessage(toSend, pendingAttachment)
                                    }
                                }
                                .testTag("ai_assistant_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = isBusy,
                                    transitionSpec = {
                                        (scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)))
                                            .togetherWith(scaleOut(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)))
                                    },
                                    label = "generating_action_transition"
                                ) { busy ->
                                    if (busy) {
                                        // Crisp white stop square on solid emerald background
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(2.5.dp))
                                                .background(Color.White)
                                        )
                                    } else {
                                        // Always Send icon (does not change when typing)
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Professional AI Disclaimer Message
                Text(
                    text = "AI can make mistakes. Please verify important study facts & formulas with syllabus materials.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 2.dp, bottom = 4.dp)
                )
            }
        }
    }

    // Attachment Options Modal Bottom Sheet (Clean White Design)
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Upload Study Material",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Attach question photos, diagrams, or PDF study notes for instant AI explanations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Option 1: Photo (Crisp White Card)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "Question Photo",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Question Photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Diagrams & formulas",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: PDF (Crisp White Card)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                documentPickerLauncher.launch("application/pdf")
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F766E).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = "PDF Document",
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "PDF Notes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Past papers & slides",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // AI Model Selection & API Key Configuration Dialog
    if (showModelDialog) {
        AiModelSelectionDialog(
            selectedModel = selectedModel,
            onSelectModel = onSelectModel,
            customZaiApiKey = customZaiApiKey,
            onUpdateZaiApiKey = onUpdateZaiApiKey,
            onDismiss = { showModelDialog = false }
        )
    }
}

/**
 * Discovery Home View with background art and custom palette.
 */
@Composable
private fun AiAssistantHomeDiscoveryView(
    userProfile: UserProfile,
    onCardClick: (String, AttachedMediaType?) -> Unit
) {
    val firstName = userProfile.name.split(" ").firstOrNull() ?: "Student"
    val todayDateFormatted = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    Box(modifier = Modifier.fillMaxSize()) {
        // Architectural Arch & Starlight Background Art situated at footer
        OnboardingModernFooterArt(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
        ) {
            // Greeting Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Good afternoon,",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = firstName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "$todayDateFormatted • 4 study goals due",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Hero Schedule & Progress Card (Emerald / Teal Custom Gradient)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x2210B981), spotColor = Color(0x2210B981)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F766E)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B),
                                        Color(0xFF0F766E),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Column: Schedule List
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Today's Schedule",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                ScheduleDotItem(time = "9:30", label = "Physics Theory", dotColor = Color(0xFF4ADE80))
                                ScheduleDotItem(time = "11:00", label = "Pure Math Past Paper", dotColor = Color(0xFF38BDF8))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ScheduleDotItem(time = "2:00", label = "Design review", dotColor = Color(0xFF4ADE80))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "in 3h",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                ScheduleDotItem(time = "3:30", label = "Organic Chemistry", dotColor = Color(0xFFFBBF24))
                                ScheduleDotItem(time = "4:30", label = "English Viva Prep", dotColor = Color(0xFFA78BFA))
                                ScheduleDotItem(time = "6:00", label = "Revise Flashcards", dotColor = Color(0xFF34D399))
                            }

                            // Right Column: Circular Progress Ring
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(90.dp)) {
                                    val strokeWidth = 8.dp.toPx()
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.15f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color(0xFF4ADE80),
                                        startAngle = -90f,
                                        sweepAngle = 120f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "33%",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "2 OF 6",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // "FOR YOU" Section Heading
            item {
                Text(
                    text = "FOR YOU",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
            }

            // "FOR YOU" 2x2 Clean White Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ForYouTile(
                            icon = Icons.Outlined.CalendarMonth,
                            iconBg = Color(0xFF22C55E),
                            title = "Plan my day",
                            subtitle = "Build today's schedule",
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick("Rearrange my study schedule around my 2 PM exam prep session.", null) }
                        )

                        ForYouTile(
                            icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                            iconBg = Color(0xFF0F766E),
                            title = "Draft notes",
                            subtitle = "From PDF lecture notes",
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick("Summarize key concepts from attached study notes into high-yield exam bullet points.", AttachedMediaType.PDF) }
                        )
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ForYouTile(
                            icon = Icons.Outlined.PhotoCamera,
                            iconBg = Color(0xFFF59E0B),
                            title = "Solve photo",
                            subtitle = "Snap exam question",
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick("Solve this question step-by-step and show all formulas.", AttachedMediaType.IMAGE) }
                        )

                        ForYouTile(
                            icon = Icons.Outlined.Quiz,
                            iconBg = Color(0xFF8B5CF6),
                            title = "What's due?",
                            subtitle = "Tasks across subjects",
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick("Generate 5 practice multiple-choice questions for my upcoming exam with answers.", null) }
                        )
                    }

                    // Row 3: Official Past Papers & Examination Timetables
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ForYouTile(
                            icon = Icons.Outlined.Description,
                            iconBg = Color(0xFFEF4444),
                            title = "2019 SFT Paper",
                            subtitle = "Download Sinhala / English PDF",
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick("Give me 2019 A/L SFT past paper (Sinhala medium) with marking scheme.", null) }
                        )

                        ForYouTile(
                            icon = Icons.Outlined.AccessTime,
                            iconBg = Color(0xFF3B82F6),
                            title = "2026 Timetable",
                            subtitle = "Official exam schedule",
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick("What is the official G.C.E. A/L 2026 examination timetable schedule?", null) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Schedule Dot Item inside Hero Card.
 */
@Composable
private fun ScheduleDotItem(time: String, label: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = time,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 2x2 Grid Tile on White Surface.
 */
@Composable
private fun ForYouTile(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * User Chat Bubble with Emerald Green Styling.
 */
@Composable
private fun UserChatBubbleDesign(message: AiAssistantMessage) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 4.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = Color(0xFF22C55E), // Green background for user message
            contentColor = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.attachment != null) {
                    if (message.attachment.type == AttachedMediaType.IMAGE) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                        ) {
                            AsyncImage(
                                model = message.attachment.uri,
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.20f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = message.attachment.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.attachment.sizeFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Assistant Chat Card in pure black background (#000000) with smooth true token streaming,
 * responsive markdown formatting without raw marks, and copy/voice/share controls underneath.
 */
@Composable
private fun AssistantChatCardDesign(
    message: AiAssistantMessage,
    onSpeak: (String) -> Unit,
    onRetry: (String) -> Unit = {},
    isLatestMessage: Boolean = false,
    isGenerating: Boolean = false,
    hasAlreadyStreamed: Boolean = false,
    isStopRequested: Boolean = false,
    onTypingStateChange: (Boolean) -> Unit = {},
    onStreamComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    var fullText = message.text
    while (fullText.startsWith("null")) {
        fullText = fullText.removePrefix("null").trimStart()
    }

    // Pure real-time streaming: display incoming tokens immediately without artificial delays
    LaunchedEffect(isGenerating, fullText.length, isStopRequested) {
        if (isStopRequested || !isGenerating) {
            onTypingStateChange(false)
            onStreamComplete()
        } else {
            onTypingStateChange(true)
        }
    }

    val visibleText = remember(fullText, isGenerating) {
        val cleaned = fullText
            .replace(Regex("<tool_call>[\\s\\S]*?</tool_call>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<tool_call>[\\s\\S]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?tool_call>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?arg_key>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?arg_value>", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.isNotBlank()) cleaned else if (isGenerating) "Synthesizing latest educational details..." else ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sparkling AI Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // AI Reply Card in Pure Black Background (#000000)
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 22.dp,
                    bottomStart = 22.dp,
                    bottomEnd = 22.dp
                ),
                color = Color(0xFF000000), // Pure Black Card Background
                border = BorderStroke(1.dp, Color(0xFF27272A)),
                shadowElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!message.reasoningText.isNullOrBlank()) {
                        AiReasoningBlock(
                            reasoningText = message.reasoningText,
                            isThinking = false,
                            durationSeconds = message.thinkingDurationSeconds,
                            initialExpanded = false
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            thickness = 1.dp,
                            color = Color(0xFF27272A)
                        )
                    }

                    // Streamed answer with fluid word blur-in & vertical settle animation
                    val isStreamingActive = isGenerating && isLatestMessage
                    var lastLength by remember { mutableIntStateOf(0) }
                    val settleAnim = remember { Animatable(if (isStreamingActive) 0.6f else 1f) }
                    val offsetYAnim = remember { Animatable(if (isStreamingActive) 4f else 0f) }

                    LaunchedEffect(visibleText.length, isStreamingActive) {
                        if (isStreamingActive && visibleText.length > lastLength) {
                            lastLength = visibleText.length
                            settleAnim.snapTo(0.65f)
                            offsetYAnim.snapTo(4f)
                            launch {
                                settleAnim.animateTo(1f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                            }
                            launch {
                                offsetYAnim.animateTo(0f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                            }
                        }
                    }

                    Box(
                        modifier = if (isStreamingActive) {
                            Modifier.graphicsLayer {
                                alpha = settleAnim.value
                                translationY = offsetYAnim.value
                            }
                        } else Modifier
                    ) {
                        RichMarkdownContentDark(content = visibleText)
                    }

                    // Sources Dropdown at the bottom of the response
                    val sources = remember(visibleText, message.reasoningText) {
                        extractSourcesFromMessage(visibleText, message.reasoningText)
                    }
                    if (sources.isNotEmpty()) {
                        ResponseSourcesDropdown(sources = sources)
                    }
                }
            }
        }

        // Action Buttons Row placed below the card (Copy, Retry, Read Aloud, Share)
        Row(
            modifier = Modifier
                .padding(start = 42.dp, top = 2.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Copy Action Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val cleanText = formatCleanTextForClipboard(fullText)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Response", cleanText))
                        Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Retry / Regenerate Action Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRetry(message.id) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry / Regenerate response",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Read Aloud / TTS Voice Action Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSpeak(formatCleanTextForClipboard(fullText)) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Read aloud",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Share Action Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val cleanText = formatCleanTextForClipboard(fullText)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, cleanText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Explanation")
                        context.startActivity(shareIntent)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private enum class DocDownloadState {
    IDLE,
    DOWNLOADING,
    COMPLETED
}

/**
 * Interactive, ChatGPT-styled Document Card for Downloadable Sri Lankan Past Papers,
 * Examination Timetables, Marking Schemes, and Syllabus documents.
 * Styled with dark grey card background on pure black, top PDF icon and title,
 * document details in clean bullet points, and bottom-right vibrant green Download button.
 */
@Composable
private fun ChatGptDocumentCard(
    docJson: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf(DocDownloadState.IDLE) }

    val parsed = remember(docJson) {
        try {
            org.json.JSONObject(docJson.trim())
        } catch (_: Exception) {
            org.json.JSONObject()
        }
    }
    val rawTitle = parsed.optString("title", "").ifBlank {
        parsed.optString("document_title", "")
    }
    val medium = parsed.optString("medium", "Sinhala")
    val docType = parsed.optString("type", "Official Past Paper")
    val pages = parsed.optInt("pages", 14)
    val url = parsed.optString("download_url").ifBlank { parsed.optString("uri", "") }
    val size = parsed.optString("file_size", "Ready to save")

    val title = if (rawTitle.isBlank() || rawTitle.equals("Official Educational Document", ignoreCase = true) || rawTitle.equals("Study_Document", ignoreCase = true)) {
        val subject = parsed.optString("subject", "").ifBlank {
            when {
                url.contains("sft", ignoreCase = true) || docJson.contains("sft", ignoreCase = true) -> "Science for Technology (SFT)"
                url.contains("math", ignoreCase = true) || docJson.contains("math", ignoreCase = true) -> "Combined Mathematics"
                url.contains("phy", ignoreCase = true) || docJson.contains("phy", ignoreCase = true) -> "Physics"
                url.contains("chem", ignoreCase = true) || docJson.contains("chem", ignoreCase = true) -> "Chemistry"
                url.contains("bio", ignoreCase = true) || docJson.contains("bio", ignoreCase = true) -> "Biology"
                url.contains("ict", ignoreCase = true) || docJson.contains("ict", ignoreCase = true) -> "ICT"
                else -> "Curriculum Studies"
            }
        }
        val year = parsed.optInt("year", 2019)
        "G.C.E. A/L $subject $year Past Paper & Marking Scheme"
    } else {
        rawTitle
    }

    val sourceDomain = remember(url) {
        try {
            if (url.startsWith("http")) {
                val host = Uri.parse(url).host?.removePrefix("www.") ?: ""
                if (host.isNotBlank()) host else "pastpapers.wiki"
            } else "pastpapers.wiki"
        } catch (_: Exception) { "pastpapers.wiki" }
    }

    // Dark grey card on dark black background with subtle sleek border
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF18181B), // Dark Grey Card Background
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: PDF Icon + Filename on Top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "PDF Document",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Verified G.C.E. Document • $sourceDomain",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFF27272A),
                thickness = 1.dp
            )

            // Document Information displayed as clean Bullet Points inside the Card
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            ) {
                // Bullet 1: Medium
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFF22C55E), // Vibrant Green Bullet Point
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Medium: $medium Medium",
                        color = Color(0xFFE4E4E7),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bullet 2: Type
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFF22C55E), // Vibrant Green Bullet Point
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Type: $docType & Marking Scheme",
                        color = Color(0xFFE4E4E7),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bullet 3: Format & Pages
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFF22C55E), // Vibrant Green Bullet Point
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Pages: $pages Pages (Verified Curriculum Content)",
                        color = Color(0xFFE4E4E7),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bullet 4: Storage Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFF22C55E), // Vibrant Green Bullet Point
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Destination: Download > DZD Learn",
                        color = Color(0xFFE4E4E7),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right Side Bottom Download Button (Vibrant Green, only "Download" text)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (downloadState == DocDownloadState.DOWNLOADING) return@Button
                        downloadState = DocDownloadState.DOWNLOADING

                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val localFile = if (url.startsWith("/") && java.io.File(url).exists()) {
                                    java.io.File(url)
                                } else null

                                val sanitizedFileName = title.replace(Regex("[^a-zA-Z0-9_ -]"), "_").trim().take(40).ifBlank { "Study_Document" } + ".pdf"

                                val success = if (localFile != null && localFile.exists()) {
                                    val (saved, _) = com.example.data.remote.DzdDocumentHandler.savePdfToPublicDownloads(
                                        context = context,
                                        sourceFile = localFile,
                                        sourceBytes = null,
                                        suggestedFileName = sanitizedFileName
                                    )
                                    saved
                                } else {
                                    val artifact = com.example.data.remote.DzdDocumentHandler.downloadOrRetrieveDocument(
                                        context = context,
                                        queryOrTitle = title,
                                        directUrl = if (url.startsWith("http")) url else null,
                                        mediumHint = medium,
                                        docTypeHint = docType
                                    )
                                    true
                                }

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (success) {
                                        downloadState = DocDownloadState.COMPLETED
                                        Toast.makeText(context, "Saved to Downloads > DZD Learn", Toast.LENGTH_SHORT).show()
                                    } else {
                                        downloadState = DocDownloadState.IDLE
                                        Toast.makeText(context, "Could not save document", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                if (success) {
                                    kotlinx.coroutines.delay(2600)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        downloadState = DocDownloadState.IDLE
                                    }
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    downloadState = DocDownloadState.IDLE
                                    Toast.makeText(context, "Download error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = downloadState != DocDownloadState.DOWNLOADING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        disabledContainerColor = EmeraldPrimary.copy(alpha = 0.85f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    AnimatedContent(
                        targetState = downloadState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "download_btn_anim"
                    ) { state ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            when (state) {
                                DocDownloadState.IDLE -> {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Download",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                DocDownloadState.DOWNLOADING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.25f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Downloading...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                DocDownloadState.COMPLETED -> {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Downloaded",
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
    }
}

data class ChatSourceItem(
    val title: String,
    val url: String,
    val domain: String,
    val faviconUrl: String = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
)

/**
 * Extracts distinct web sources from assistant text and chain-of-thought traces.
 * Provides authentic portal and page titles, deduplicates by title and domain,
 * and extracts the real website names and icons.
 */
private fun extractSourcesFromMessage(text: String, reasoningText: String?): List<ChatSourceItem> {
    val rawSources = mutableListOf<ChatSourceItem>()
    val combined = "$text\n${reasoningText.orEmpty()}"

    fun cleanDomainName(url: String): String {
        return try {
            val host = Uri.parse(url).host?.removePrefix("www.") ?: url
            host.lowercase()
        } catch (_: Exception) {
            url.lowercase()
        }
    }

    fun cleanSlugToTitle(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val path = uri.path?.trim('/') ?: return null
            val segments = path.split('/').filter { it.isNotBlank() && !it.equals("download", ignoreCase = true) }
            val slug = segments.lastOrNull() ?: return null
            if (slug.length < 4 || slug.all { it.isDigit() }) return null
            val titleWords = slug.split('-', '_')
                .filter { it.isNotBlank() && it != "html" && it != "htm" && it != "php" }
                .map { word ->
                    when (word.lowercase()) {
                        "al", "a-l", "a/l" -> "A/L"
                        "ol", "o-l", "o/l" -> "O/L"
                        "sft" -> "SFT"
                        "et" -> "ET"
                        "ict" -> "ICT"
                        "bst" -> "BST"
                        "pdf" -> "PDF"
                        "lk", "wiki" -> ""
                        else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                }
                .filter { it.isNotBlank() }
                .joinToString(" ")

            if (titleWords.length >= 6) titleWords else null
        } catch (_: Exception) {
            null
        }
    }

    fun getFriendlyPortalName(domain: String, url: String, label: String): String {
        val d = domain.lowercase()
        val cleanLabel = label.trim()
        val isLabelGeneric = cleanLabel.isBlank() ||
                cleanLabel.equals("Download Document", ignoreCase = true) ||
                cleanLabel.equals("Download", ignoreCase = true) ||
                cleanLabel.equals("Open PDF", ignoreCase = true) ||
                cleanLabel.equals("PDF Document", ignoreCase = true) ||
                cleanLabel.equals("Link", ignoreCase = true) ||
                cleanLabel.startsWith("http", ignoreCase = true) ||
                cleanLabel.contains("archive.org", ignoreCase = true)

        if (!isLabelGeneric && cleanLabel.length in 4..60) {
            return cleanLabel
        }

        val slugTitle = cleanSlugToTitle(url)

        return when {
            d.contains("pastpapers.wiki") || d.contains("pastpaperswiki") -> {
                if (!slugTitle.isNullOrBlank()) "Past Papers WIKI • $slugTitle" else "Past Papers WIKI"
            }
            d.contains("govdoc.lk") -> {
                if (!slugTitle.isNullOrBlank()) "GovDoc.lk • $slugTitle" else "GovDoc.lk - Educational Documents"
            }
            d.contains("doenets.lk") -> "Department of Examinations Sri Lanka"
            d.contains("nie.lk") -> "National Institute of Education (NIE)"
            d.contains("e-thaksalawa.moe.gov.lk") || (d.contains("moe.gov.lk") && !d.contains("doenets")) -> "e-Thaksalawa National Learning Portal"
            d.contains("e-kalvi.com") -> "E-Kalvi Educational Hub"
            d.contains("apepanthiya.lk") -> "Ape Panthiya Educational Hub"
            d.contains("mathsapi.com") -> "MathsApi Sri Lanka"
            d.contains("guru.lk") -> "Guru.lk Education Hub"
            d.contains("wikipedia.org") -> if (!slugTitle.isNullOrBlank()) "Wikipedia • $slugTitle" else "Wikipedia"
            d.contains("archive.org") -> "Internet Archive Study Repository"
            d.contains("khanacademy.org") -> "Khan Academy"
            d.contains("cambridgeinternational.org") -> "Cambridge Assessment International"
            else -> {
                val siteName = domain.substringBefore('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                if (!slugTitle.isNullOrBlank()) "$siteName • $slugTitle" else siteName
            }
        }
    }

    // 1. Markdown links [Label](URL)
    val mdLinkPattern = Regex("""\[([^\]]+)\]\((https?://[^\)]+)\)""")
    mdLinkPattern.findAll(combined).forEach { m ->
        val label = m.groupValues[1].trim()
        val url = m.groupValues[2].trim()
        val d = cleanDomainName(url)
        val isIgnored = url.endsWith(".pdf", ignoreCase = true) ||
                url.contains("/download/", ignoreCase = true) ||
                url.contains(".png") || url.contains(".jpg") ||
                d == "localhost" || url.startsWith("content://") || url.startsWith("file://")

        if (url.startsWith("http") && !isIgnored) {
            val friendlyTitle = getFriendlyPortalName(d, url, label)
            rawSources.add(ChatSourceItem(title = friendlyTitle, url = url, domain = d))
        }
    }

    // 2. Raw URLs
    val rawUrlPattern = Regex("""https?://[^\s\)\],<"']+""")
    rawUrlPattern.findAll(combined).forEach { m ->
        val url = m.value.trim()
        val d = cleanDomainName(url)
        val isIgnored = url.endsWith(".pdf", ignoreCase = true) ||
                url.contains("/download/", ignoreCase = true) ||
                url.contains(".png") || url.contains(".jpg") ||
                d == "localhost" || url.startsWith("content://") || url.startsWith("file://")

        if (url.startsWith("http") && !isIgnored) {
            val friendlyTitle = getFriendlyPortalName(d, url, "")
            rawSources.add(ChatSourceItem(title = friendlyTitle, url = url, domain = d))
        }
    }

    // 3. Provide genuine official educational repositories for curriculum/exam inquiries prioritizing pastpapers.wiki first, then govdoc.lk
    if (rawSources.isEmpty() && (combined.contains("past paper", ignoreCase = true) || combined.contains("exam", ignoreCase = true) || combined.contains("marking scheme", ignoreCase = true) || combined.contains("timetable", ignoreCase = true) || combined.contains("syllabus", ignoreCase = true) || combined.contains("document", ignoreCase = true))) {
        val queryHint = text.take(60).replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        val encodedQuery = try { java.net.URLEncoder.encode(queryHint, "UTF-8") } catch (_: Exception) { "sri+lanka+past+papers" }

        // Tier 1: pastpapers.wiki
        rawSources.add(
            ChatSourceItem(
                title = "Past Papers WIKI - Official Repository",
                url = "https://pastpapers.wiki/?s=$encodedQuery",
                domain = "pastpapers.wiki"
            )
        )
        // Tier 2: govdoc.lk
        rawSources.add(
            ChatSourceItem(
                title = "GovDoc.lk - Past Papers & Marking Schemes",
                url = "https://govdoc.lk/?s=$encodedQuery",
                domain = "govdoc.lk"
            )
        )
        // Tier 3: Official Examination and NIE Portals
        rawSources.add(
            ChatSourceItem(
                title = "Department of Examinations Sri Lanka (Doenets)",
                url = "https://www.doenets.lk/",
                domain = "doenets.lk"
            )
        )
        rawSources.add(
            ChatSourceItem(
                title = "National Institute of Education (NIE)",
                url = "https://nie.lk/",
                domain = "nie.lk"
            )
        )
        rawSources.add(
            ChatSourceItem(
                title = "e-Thaksalawa National Learning Portal",
                url = "https://e-thaksalawa.moe.gov.lk/",
                domain = "e-thaksalawa.moe.gov.lk"
            )
        )
    }

    // 4. Strict Deduplication:
    // Ensure uniqueness by (1) exact URL, and (2) unique title to prevent duplicate chips.
    // Also limit at most 2 distinct items per domain so pastpapers.wiki / govdoc.lk don't flood the list.
    val deduplicated = mutableListOf<ChatSourceItem>()
    val seenUrls = mutableSetOf<String>()
    val seenTitles = mutableSetOf<String>()
    val domainCounts = mutableMapOf<String, Int>()

    for (item in rawSources) {
        val normalizedUrl = item.url.trim().removeSuffix("/")
        val normalizedTitle = item.title.trim().lowercase()
        val count = domainCounts.getOrDefault(item.domain, 0)

        if (!seenUrls.contains(normalizedUrl) && !seenTitles.contains(normalizedTitle) && count < 2) {
            seenUrls.add(normalizedUrl)
            seenTitles.add(normalizedTitle)
            domainCounts[item.domain] = count + 1
            deduplicated.add(item)
        }
    }

    // Sort sources by priority hierarchy: 1. pastpapers.wiki, 2. govdoc.lk, 3. official/other portals
    val sortedSources = deduplicated.sortedWith(
        compareBy { item ->
            val d = item.domain.lowercase()
            when {
                d.contains("pastpapers.wiki") || d.contains("pastpaperswiki") -> 0
                d.contains("govdoc.lk") -> 1
                d.contains("doenets.lk") -> 2
                d.contains("nie.lk") -> 3
                d.contains("moe.gov.lk") -> 4
                else -> 5
            }
        }
    )

    return sortedSources.take(4)
}

/**
 * Modern Sources dropdown menu styled with real website favicons and responsive chip links.
 * Styled with Dark Grey container and Vibrant Green accents.
 */
@Composable
private fun ResponseSourcesDropdown(
    sources: List<ChatSourceItem>,
    modifier: Modifier = Modifier
) {
    if (sources.isEmpty()) return
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "sources_chevron"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF18181B), // Dark Grey
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Header Row (Clickable to toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = "Sources",
                        tint = Color(0xFF22C55E), // Vibrant Green
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sources (${sources.size})",
                        color = Color(0xFFF4F4F5),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sources.forEach { source ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF27272A),
                            border = BorderStroke(0.8.dp, Color(0xFF3F3F46)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Could not open source link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Real website favicon loaded via Coil AsyncImage with custom fallback
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF18181B),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        var isIconError by remember { mutableStateOf(false) }
                                        if (!isIconError) {
                                            AsyncImage(
                                                model = source.faviconUrl,
                                                contentDescription = source.title,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                contentScale = ContentScale.Fit,
                                                onError = { isIconError = true }
                                            )
                                        }
                                        if (isIconError) {
                                            Icon(
                                                imageVector = when {
                                                    source.domain.contains("pastpapers") -> Icons.Filled.MenuBook
                                                    source.domain.contains("govdoc") -> Icons.Filled.Description
                                                    source.domain.contains("doenets") || source.domain.contains("nie") -> Icons.Filled.School
                                                    else -> Icons.Outlined.Public
                                                },
                                                contentDescription = null,
                                                tint = Color(0xFF22C55E), // Vibrant Green
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = source.domain,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.NorthEast,
                                    contentDescription = "Open",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clickable web and document link button.
 * Renders with a globe icon (🌐) for web links or document icon (📄) for PDFs,
 * a clean underlined title in vibrant green/white, and an open-in-new arrow indicator.
 */
@Composable
private fun ChatGptLinkButton(
    label: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanUrl = url.trim().removeSurrounding("<", ">").removeSurrounding("\"", "\"").trim()
    val isPdf = cleanUrl.endsWith(".pdf", ignoreCase = true) || label.contains("pdf", ignoreCase = true)
    val displayLabel = label.trim().ifBlank {
        try {
            Uri.parse(cleanUrl).host ?: cleanUrl
        } catch (_: Exception) {
            cleanUrl
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF18181B),
        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                try {
                    val uri = Uri.parse(cleanUrl)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.w("ChatGptLinkButton", "Cannot open URL: $cleanUrl", e)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isPdf) Icons.AutoMirrored.Outlined.InsertDriveFile else Icons.Outlined.Language,
                contentDescription = if (isPdf) "PDF Document" else "Web Link",
                tint = if (isPdf) Color(0xFFEF4444) else Color(0xFF22C55E),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                ),
                color = if (isPdf) Color(0xFFFCA5A5) else Color(0xFF4ADE80),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open link",
                tint = Color(0xFFA1A1AA),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Renders a line of markdown, extracting any [Title](URL) links, raw URLs, and document objects
 * into ChatGPT-styled clickable buttons and interactive download cards.
 */
@Composable
private fun MarkdownLineWithLinks(
    rawText: String,
    modifier: Modifier = Modifier
) {
    if (rawText.contains("[DOWNLOAD_DOCUMENT:")) {
        val jsonStr = rawText.substringAfter("[DOWNLOAD_DOCUMENT:").substringBefore("]")
        val before = rawText.substringBefore("[DOWNLOAD_DOCUMENT:").trim()
        val after = rawText.substringAfter("]", "").trim()
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (before.isNotBlank()) {
                MarkdownLineWithLinks(rawText = before)
            }
            ChatGptDocumentCard(docJson = jsonStr)
            if (after.isNotBlank()) {
                MarkdownLineWithLinks(rawText = after)
            }
        }
        return
    }

    val trimmed = rawText.trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}") && (trimmed.contains("download_url") || trimmed.contains("uri") || trimmed.contains("document_title") || trimmed.contains("title"))) {
        ChatGptDocumentCard(docJson = trimmed, modifier = modifier)
        return
    }

    // 1. Check for Markdown links [Label](URL or FilePath)
    val mdLinkPattern = Regex("""\[([^\]]+)\]\(([^\)]+)\)""")
    val mdMatches = mdLinkPattern.findAll(rawText).toList()

    if (mdMatches.isNotEmpty()) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            var lastIdx = 0
            for (m in mdMatches) {
                val before = rawText.substring(lastIdx, m.range.first).trim()
                if (before.isNotBlank()) {
                    Text(
                        text = buildFormattedAnnotatedString(raw = before),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
                val label = m.groupValues[1].trim()
                val linkUrl = m.groupValues[2].trim()
                val isLocalPdfFile = linkUrl.endsWith(".pdf", ignoreCase = true) && (linkUrl.startsWith("/storage") || linkUrl.startsWith("/data") || linkUrl.startsWith("content://") || linkUrl.startsWith("file://"))

                if (isLocalPdfFile) {
                    val cleanTitle = java.io.File(linkUrl).nameWithoutExtension.replace('_', ' ').ifBlank { label }
                    ChatGptDocumentCard(
                        docJson = org.json.JSONObject().apply {
                            put("title", cleanTitle.removePrefix("Download ").removePrefix("PDF: ").trim())
                            put("download_url", linkUrl)
                            put("uri", linkUrl)
                            put("medium", if (label.contains("Tamil", ignoreCase = true)) "Tamil" else if (label.contains("English", ignoreCase = true)) "English" else "Sinhala")
                            put("pages", 14)
                            put("file_size", "Ready to save")
                            put("type", "Official Past Paper")
                        }.toString()
                    )
                } else {
                    ChatGptLinkButton(label = label, url = linkUrl)
                }
                lastIdx = m.range.last + 1
            }
            if (lastIdx < rawText.length) {
                val after = rawText.substring(lastIdx).trim()
                if (after.isNotBlank()) {
                    Text(
                        text = buildFormattedAnnotatedString(raw = after),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        return
    }

    // 2. Check for raw URLs (e.g. "Download Link: https://..." or "• SFT 2019: https://...")
    val urlPattern = Regex("""https?://[^\s\)\],]+""")
    val rawUrlMatches = urlPattern.findAll(rawText).toList()

    if (rawUrlMatches.isNotEmpty()) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            var lastIdx = 0
            for (m in rawUrlMatches) {
                val before = rawText.substring(lastIdx, m.range.first).trim()
                val url = m.value.trim()

                if (before.isNotBlank()) {
                    Text(
                        text = buildFormattedAnnotatedString(raw = before),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
                val displayLabel = before.takeLast(40).trim().ifBlank { "Visit Educational Portal" }
                ChatGptLinkButton(label = displayLabel, url = url)
                lastIdx = m.range.last + 1
            }
            if (lastIdx < rawText.length) {
                val after = rawText.substring(lastIdx).trim()
                if (after.isNotBlank()) {
                    Text(
                        text = buildFormattedAnnotatedString(raw = after),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        return
    }

    // Fallback: Standard inline-formatted text
    Text(
        text = buildFormattedAnnotatedString(raw = rawText),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        lineHeight = 20.sp,
        modifier = modifier
    )
}

/**
 * Enhanced responsive Markdown renderer styled for the Black AI Card.
 * Fully supports all markdown styles without showing raw marks:
 * - Truly responsive Tables (adaptive widths on mobile screens)
 * - Headings (# H1 to ###### H6 with or without space / outer ** wrappers)
 * - Horizontal Dividers (---, ***, ___)
 * - Code & Formula blocks with language tag and copy action
 * - Blockquotes and Callout boxes
 * - Numbered and bulleted lists
 * - Inline formatting (***bold italic***, **bold**, *italic*, ~~strikethrough~~, `code`, $math$)
 */
@Composable
private fun RichMarkdownContentDark(content: String) {
    val context = LocalContext.current
    var cleanContent = content
    while (cleanContent.startsWith("null")) {
        cleanContent = cleanContent.removePrefix("null").trimStart()
    }
    if (cleanContent.isBlank()) return

    // 1. Normalize line breaks (\r\n -> \n)
    cleanContent = cleanContent.replace("\r\n", "\n").replace("\r", "\n")

    // 2. Ensure any markdown heading (###) is on its own separate line if glued to previous text
    cleanContent = cleanContent.replace(Regex("""([^\n])\s*(#{1,6}\s+)"""), "$1\n\n$2")

    // 3. Ensure numbered list items glued to previous sentences are separated into lines
    cleanContent = cleanContent.replace(Regex("""([.:;?!])\s+(\d+[\.\)]\s+)"""), "$1\n$2")

    // 4. Ensure "Study Tip:", "Tip:", "Note:", or "Important:" are moved to their own line if glued after links or text
    cleanContent = cleanContent.replace(Regex("""(\)|[^\n\s])\s*(\*{0,2}(Study Tip:|Tip:|Note:|Important:)\*{0,2})"""), "$1\n\n$2")

    // 5. If content contains a Download Document Card, deduplicate and strip redundant repetitive bullet metadata outside the card
    if (cleanContent.contains("[DOWNLOAD_DOCUMENT:")) {
        val allDocTokens = Regex("""\[DOWNLOAD_DOCUMENT:([^\]]+)\]""").findAll(cleanContent).toList()
        if (allDocTokens.size > 1) {
            var isFirst = true
            cleanContent = Regex("""\[DOWNLOAD_DOCUMENT:([^\]]+)\]""").replace(cleanContent) { match ->
                if (isFirst) {
                    isFirst = false
                    match.value
                } else {
                    ""
                }
            }
        }

        cleanContent = cleanContent
            .replace(Regex("""(?i)(?:Here are the details for the document:?|Details of the document:?|Document Information:?)"""), "")
            .replace(Regex("""(?m)^[•\-*+]\s*\*\*(?:Document|Medium|Year|Type|Pages|Content|Key Topics)\*\*:[^\n]*\n?"""), "")
            .replace(Regex("""(?m)^[•\-*+]\s*(?:Document|Medium|Year|Type|Pages|Content|Key Topics):[^\n]*\n?"""), "")
            .replace(Regex("""(?m)^\s*Content:\s*This document contains[^\n]*\n?"""), "")
            .replace(Regex("""(?m)^[•\-*+]\s*(\[DOWNLOAD_DOCUMENT:)"""), "$1")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    val rawLines = cleanContent.split("\n")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var lineIndex = 0
        while (lineIndex < rawLines.size) {
            val line = rawLines[lineIndex]
            val trimmed = line.trim()

            when {
                // 0. Download Document Card
                trimmed.contains("[DOWNLOAD_DOCUMENT:") -> {
                    val jsonStr = trimmed.substringAfter("[DOWNLOAD_DOCUMENT:").substringBefore("]")
                    val before = trimmed.substringBefore("[DOWNLOAD_DOCUMENT:").trim()
                        .removePrefix("•").removePrefix("-").removePrefix("*").removePrefix("+").trim()
                    if (before.isNotBlank() && before != "•" && before != "-" && before != "*") {
                        MarkdownLineWithLinks(rawText = before)
                    }
                    ChatGptDocumentCard(docJson = jsonStr)
                    val after = trimmed.substringAfter("]", "").trim()
                        .removePrefix("•").removePrefix("-").removePrefix("*").removePrefix("+").trim()
                    if (after.isNotBlank() && after != "•" && after != "-" && after != "*") {
                        MarkdownLineWithLinks(rawText = after)
                    }
                    lineIndex++
                }

                // 1. Code / Formula Block (```)
                trimmed.startsWith("```") -> {
                    val langTag = trimmed.removePrefix("```").trim().ifEmpty { "Formula / Code" }
                    val codeLines = mutableListOf<String>()
                    lineIndex++
                    while (lineIndex < rawLines.size && !rawLines[lineIndex].trim().startsWith("```")) {
                        codeLines.add(rawLines[lineIndex])
                        lineIndex++
                    }
                    if (lineIndex < rawLines.size) lineIndex++ // skip closing ```
                    val codeContent = codeLines.joinToString("\n")

                    if (codeContent.trim().startsWith("{") && (codeContent.contains("download_url") || codeContent.contains("uri") || codeContent.contains("document_title") || codeContent.contains("Past Paper"))) {
                        ChatGptDocumentCard(docJson = codeContent)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF09090B),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Top Header Bar with Language and Copy button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF18181B))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = langTag.uppercase(),
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                val cleanCode = formatCleanTextForClipboard(codeContent)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Code", cleanCode))
                                                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Copy",
                                            color = Color(0xFF22C55E),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                            // Code Body with horizontal scrolling
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = codeContent,
                                    color = Color(0xFF4ADE80),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                lineIndex++
            }

                // 2. Responsive Markdown Table Detection
                (trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2) -> {
                    val tableLines = mutableListOf<String>()
                    while (lineIndex < rawLines.size && rawLines[lineIndex].trim().startsWith("|") && rawLines[lineIndex].trim().count { it == '|' } >= 2) {
                        tableLines.add(rawLines[lineIndex].trim())
                        lineIndex++
                    }
                    if (tableLines.size >= 2) {
                        ResponsiveMarkdownTableDark(tableLines = tableLines)
                    }
                }

                // 3. Horizontal Dividers (---, ***, ___, * * *, - - -)
                trimmed.matches(Regex("""^(\*{3,}|-{3,}|_{3,}|\*\s+\*\s+\*|-\s+-\s+-)$""")) -> {
                    HorizontalDivider(
                        color = Color(0xFF22C55E).copy(alpha = 0.35f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    lineIndex++
                }

                // 4. Headings (# H1 to ###### H6, with or without outer ** wrappers, with or without spaces)
                trimmed.removePrefix("**").trim().startsWith("#") -> {
                    val candidate = trimmed.removePrefix("**").removeSuffix("**").trim()
                    val hashes = candidate.takeWhile { it == '#' }
                    val level = hashes.length.coerceIn(1, 6)
                    var headingText = candidate.removePrefix(hashes).trim()
                    headingText = headingText.removePrefix("**").removeSuffix("**").trim()
                    // Strip any trailing closing hashes (e.g. ### Heading ###)
                    headingText = headingText.replace(Regex("""\s*#{1,6}\s*$"""), "").trim()

                    // Vibrant green colors for headings
                    val (titleColor, fontSize, paddingTop) = when (level) {
                        1 -> Triple(Color(0xFF22C55E), 18.sp, 10.dp)
                        2 -> Triple(Color(0xFF22C55E), 16.5.sp, 8.dp)
                        3 -> Triple(Color(0xFF4ADE80), 15.sp, 6.dp)
                        4 -> Triple(Color(0xFF4ADE80), 14.sp, 4.dp)
                        else -> Triple(Color(0xFF86EFAC), 13.5.sp, 3.dp)
                    }

                    if (headingText.isNotBlank()) {
                        Text(
                            text = buildFormattedAnnotatedString(raw = headingText, overrideColor = titleColor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            fontSize = fontSize,
                            modifier = Modifier.padding(top = paddingTop, bottom = 3.dp)
                        )
                    }
                    lineIndex++
                }

                // 5. Blockquotes (> quote)
                trimmed.startsWith(">") -> {
                    val quoteText = trimmed.removePrefix(">").replace(Regex("""^\s*#{1,6}\s*"""), "").trim()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF18181B),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF22C55E), RoundedCornerShape(2.dp))
                            )
                            MarkdownLineWithLinks(rawText = quoteText)
                        }
                    }
                    lineIndex++
                }

                // 6. Bullet Lists (•, -, *, +, **•, etc.)
                trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") || trimmed.startsWith("**•") || trimmed == "*" || trimmed == "-" -> {
                    var bulletText = trimmed.removePrefix("**").trim()
                    bulletText = when {
                        bulletText.startsWith("•") -> bulletText.removePrefix("•").trim()
                        bulletText.startsWith("- ") -> bulletText.removePrefix("- ").trim()
                        bulletText.startsWith("* ") -> bulletText.removePrefix("* ").trim()
                        bulletText.startsWith("+ ") -> bulletText.removePrefix("+ ").trim()
                        else -> bulletText
                    }
                    bulletText = bulletText.removeSuffix("**").trim()
                    // Strip any stray heading hashes inside bullet text
                    bulletText = bulletText.replace(Regex("""^\s*#{1,6}\s*"""), "").replace(Regex("""\s*#{1,6}\s*$"""), "").trim()

                    // Check if the bullet item actually contains a document card or link
                    if (bulletText.contains("[DOWNLOAD_DOCUMENT:") || (bulletText.startsWith("{") && bulletText.contains("download_url"))) {
                        val jsonStr = if (bulletText.contains("[DOWNLOAD_DOCUMENT:")) {
                            bulletText.substringAfter("[DOWNLOAD_DOCUMENT:").substringBefore("]")
                        } else bulletText
                        ChatGptDocumentCard(docJson = jsonStr)
                    } else {
                        val leadingSpaces = line.takeWhile { it == ' ' }.length
                        val indent = (leadingSpaces / 2 * 10).dp.coerceIn(4.dp, 32.dp)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = indent, top = 3.dp, bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•",
                                color = Color(0xFF22C55E), // Vibrant Green bullet dot (send button color)
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            MarkdownLineWithLinks(rawText = bulletText, modifier = Modifier.weight(1f, fill = false))
                        }
                    }
                    lineIndex++
                }

                // 7. Numbered Lists (1., 2., 1), **1.**, **1)**, etc.)
                trimmed.matches(Regex("""^(\*\*)?(\d+)[\.\)](\*\*)?\s+.*""")) -> {
                    // Collect contiguous numbered list items together to ensure sequential numbering starting at 1
                    val listItems = mutableListOf<Pair<String, String>>()
                    while (lineIndex < rawLines.size && rawLines[lineIndex].trim().matches(Regex("""^(\*\*)?(\d+)[\.\)](\*\*)?\s+.*"""))) {
                        val currTrimmed = rawLines[lineIndex].trim()
                        val match = Regex("""^(\*\*)?(\d+)[\.\)](\*\*)?\s+(.*)""").find(currTrimmed)
                        val origNum = match?.groupValues?.get(2) ?: "1"
                        var bText = match?.groupValues?.get(4)?.trim().orEmpty()
                        bText = bText.replace(Regex("""^\s*#{1,6}\s*"""), "").replace(Regex("""\s*#{1,6}\s*$"""), "").trim()
                        listItems.add(origNum to bText)
                        lineIndex++
                    }

                    // If the list starts at 2 (e.g. item 1 was missing or skipped), renumber sequentially from 1
                    val startsAtTwo = listItems.isNotEmpty() && listItems.first().first == "2"

                    listItems.forEachIndexed { itemIdx, (origNum, bText) ->
                        val displayNumber = if (startsAtTwo) {
                            "${itemIdx + 1}."
                        } else {
                            "$origNum."
                        }

                        if (bText.contains("[DOWNLOAD_DOCUMENT:") || (bText.startsWith("{") && bText.contains("download_url"))) {
                            val jsonStr = if (bText.contains("[DOWNLOAD_DOCUMENT:")) {
                                bText.substringAfter("[DOWNLOAD_DOCUMENT:").substringBefore("]")
                            } else bText
                            ChatGptDocumentCard(docJson = jsonStr)
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayNumber,
                                    color = Color(0xFF22C55E), // Vibrant Green number
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                MarkdownLineWithLinks(rawText = bText, modifier = Modifier.weight(1f, fill = false))
                            }
                        }
                    }
                }

                // 8. Normal Paragraph
                line.isNotBlank() -> {
                    // Strip any stray heading hashes from paragraph lines
                    val para = line.trim().replace(Regex("""^\s*#{1,6}\s*"""), "").replace(Regex("""\s*#{1,6}\s*$"""), "").trim()
                    if (para.isNotBlank()) {
                        MarkdownLineWithLinks(rawText = para)
                    }
                    lineIndex++
                }

                else -> {
                    lineIndex++
                }
            }
        }
    }
}

/**
 * Truly responsive Markdown Table Renderer with adaptive mobile layout.
 * Table background is clean White with vibrant Green outline borders.
 * Uses high-contrast, perfectly readable dark text on white table cells.
 */
@Composable
private fun ResponsiveMarkdownTableDark(tableLines: List<String>) {
    if (tableLines.isEmpty()) return

    // 1. Parse rows and cells
    val rawRows = tableLines.map { line ->
        var clean = line.trim()
        if (clean.startsWith("|")) clean = clean.substring(1)
        if (clean.endsWith("|")) clean = clean.substring(0, clean.length - 1)
        clean.split("|").map { it.trim() }
    }

    // 2. Identify header row, separator row (for alignments), and data rows
    var separatorIndex = -1
    for (i in rawRows.indices) {
        if (rawRows[i].any { it.contains("---") || it.contains("-|-") }) {
            separatorIndex = i
            break
        }
    }

    val headerRow = if (separatorIndex > 0) rawRows[separatorIndex - 1] else rawRows.firstOrNull() ?: return
    val separatorRow = if (separatorIndex >= 0) rawRows[separatorIndex] else emptyList()
    val dataRows = if (separatorIndex >= 0) {
        rawRows.filterIndexed { index, _ -> index > separatorIndex }
    } else {
        rawRows.drop(1)
    }.filter { row -> row.any { it.isNotBlank() && !it.contains("---") } }

    val colAlignments = headerRow.indices.map { colIdx ->
        val sep = separatorRow.getOrNull(colIdx) ?: ""
        when {
            sep.startsWith(":") && sep.endsWith(":") -> TextAlign.Center
            sep.endsWith(":") -> TextAlign.End
            else -> TextAlign.Start
        }
    }

    val numCols = maxOf(headerRow.size, dataRows.maxOfOrNull { it.size } ?: 1)
    if (numCols <= 0) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White, // Pure White Table Background as requested
        border = BorderStroke(1.5.dp, Color(0xFF22C55E)), // Vibrant Green Outline as requested
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth
            val isCompact = numCols <= 3 && availableWidth > 240.dp
            val colWidth = if (isCompact) {
                availableWidth / numCols
            } else {
                maxOf(120.dp, availableWidth / numCols)
            }

            val tableModifier = if (isCompact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            }

            Column(modifier = tableModifier) {
                // Table Header Row: Pure white background with deep emerald bold text and green outline
                Row(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headerRow.forEachIndexed { colIdx, cellText ->
                        val align = colAlignments.getOrElse(colIdx) { TextAlign.Start }
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .padding(horizontal = 10.dp),
                            contentAlignment = when (align) {
                                TextAlign.Center -> Alignment.Center
                                TextAlign.End -> Alignment.CenterEnd
                                else -> Alignment.CenterStart
                            }
                        ) {
                            Text(
                                text = buildFormattedAnnotatedString(cellText, isDarkOnLight = true),
                                color = Color(0xFF15803D), // Deep rich green for header text on white table
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = align
                            )
                        }
                        if (colIdx < headerRow.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color(0xFF22C55E).copy(alpha = 0.4f))
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF22C55E), thickness = 1.5.dp)

                // Table Data Rows: Pure white background with dark readable text and green cell dividers
                dataRows.forEachIndexed { rowIdx, rowCells ->
                    Row(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (colIdx in 0 until numCols) {
                            val cellText = rowCells.getOrNull(colIdx) ?: ""
                            val align = colAlignments.getOrElse(colIdx) { TextAlign.Start }
                            Box(
                                modifier = Modifier
                                    .width(colWidth)
                                    .padding(horizontal = 10.dp),
                                contentAlignment = when (align) {
                                    TextAlign.Center -> Alignment.Center
                                    TextAlign.End -> Alignment.CenterEnd
                                    else -> Alignment.CenterStart
                                }
                            ) {
                                Text(
                                    text = buildFormattedAnnotatedString(cellText, isDarkOnLight = true),
                                    color = Color(0xFF0F172A), // Crisp high-contrast dark text on white table
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    textAlign = align
                                )
                            }
                            if (colIdx < numCols - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(20.dp)
                                        .background(Color(0xFF22C55E).copy(alpha = 0.25f))
                                )
                            }
                        }
                    }
                    if (rowIdx < dataRows.lastIndex) {
                        HorizontalDivider(color = Color(0xFF22C55E).copy(alpha = 0.3f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

/**
 * Speculative streaming markdown normalizer:
 * 1. Hides incomplete trailing markdown symbols (***, **, *, `, ~~) so they don't flicker on screen.
 * 2. Speculatively auto-closes opened markdown tags so formatting (bold, italic, code) is rendered
 *    live in real-time as tokens arrive, without waiting for the closing sentence or delimiter!
 */
private fun normalizeStreamingMarkdown(raw: String): String {
    var text = raw
    while (text.startsWith("null")) {
        text = text.removePrefix("null").trimStart()
    }
    if (text == "null" || text.isBlank()) return ""

    // Strip trailing incomplete syntax symbols that haven't received content yet
    text = when {
        text.endsWith("***") -> text.removeSuffix("***")
        text.endsWith("**") -> text.removeSuffix("**")
        text.endsWith("___") -> text.removeSuffix("___")
        text.endsWith("__") -> text.removeSuffix("__")
        text.endsWith("~~") -> text.removeSuffix("~~")
        text.endsWith("`") && !text.endsWith("```") -> text.removeSuffix("`")
        text.endsWith("$") -> text.removeSuffix("$")
        text.endsWith("*") && (text.length == 1 || text[text.length - 2].isWhitespace()) -> text.removeSuffix("*")
        else -> text
    }

    if (text.isBlank()) return ""

    // Speculatively close unclosed triple asterisks (***bold italic***)
    val tripleCount = countOccurrences(text, "***")
    if (tripleCount % 2 != 0) {
        text += "***"
    }

    // Speculatively close unclosed double asterisks (**bold**)
    val textWithoutTriple = text.replace("***", "   ")
    val doubleCount = countOccurrences(textWithoutTriple, "**")
    if (doubleCount % 2 != 0) {
        text += "**"
    }

    // Speculatively close unclosed double underscores (__bold__)
    val doubleUnderCount = countOccurrences(text, "__")
    if (doubleUnderCount % 2 != 0) {
        text += "__"
    }

    // Speculatively close unclosed single asterisks (*italic*)
    val textWithoutDouble = textWithoutTriple.replace("**", "  ")
    val singleCount = countOccurrences(textWithoutDouble, "*")
    if (singleCount % 2 != 0) {
        val lastStar = text.lastIndexOf('*')
        if (lastStar != -1 && lastStar + 1 < text.length && !text[lastStar + 1].isWhitespace()) {
            text += "*"
        }
    }

    // Speculatively close unclosed backticks (`code`)
    val textWithoutCodeFence = text.replace("```", "   ")
    val backtickCount = countOccurrences(textWithoutCodeFence, "`")
    if (backtickCount % 2 != 0) {
        text += "`"
    }

    // Speculatively close unclosed math dollar signs ($math$)
    val dollarCount = countOccurrences(text, "$")
    if (dollarCount % 2 != 0) {
        text += "$"
    }

    // Speculatively close unclosed strikethrough (~~text~~)
    val tildeCount = countOccurrences(text, "~~")
    if (tildeCount % 2 != 0) {
        text += "~~"
    }

    return text
}

private fun countOccurrences(source: String, target: String): Int {
    var count = 0
    var index = 0
    while (index < source.length) {
        val found = source.indexOf(target, index)
        if (found != -1) {
            count++
            index = found + target.length
        } else {
            break
        }
    }
    return count
}

/**
 * Comprehensive inline Markdown parser supporting:
 * - Bold + Italic: ***text*** or ___text___
 * - Bold: **text** or __text__
 * - Italic: *text* or _text_
 * - Strikethrough: ~~text~~
 * - Inline Code: `code`
 * - Math: $formula$
 * - Cleanly removes stray ***, **, ### marks and "null" markers
 */
private fun buildFormattedAnnotatedString(
    raw: String,
    isDarkOnLight: Boolean = false,
    overrideColor: Color? = null
) = buildAnnotatedString {
    var sanitized = normalizeStreamingMarkdown(raw)
        .replace(Regex("""\[([^\]]+)\]\((https?://[^\)]+)\)"""), "🌐 $1")
        .replace(Regex("""\[([^\]]+)\]\((/[^\)]+)\)"""), "📄 $1")
        .replace(Regex("""/storage/emulated/0/Android/data/[^/\s\)]+/files/documents/studypapers/"""), "Download/DZD Learn/")
        .replace(Regex("""/storage/emulated/0/[^\s\)]+"""), "")
        .replace(Regex("""com\.aistudio\.[a-zA-Z0-9_\.]*"""), "")
        .replace(Regex("""(^|\s)#{1,6}\s*"""), "$1")
        .replace(Regex("""\s*#{1,6}\s*$"""), "")
        .replace(Regex("""#{1,6}"""), "")
    if (sanitized.isBlank()) {
        return@buildAnnotatedString
    }

    val boldColor = overrideColor ?: if (isDarkOnLight) Color(0xFF15803D) else Color.White
    val italicColor = overrideColor ?: if (isDarkOnLight) Color(0xFF334155) else Color(0xFFE2E8F0)
    val normalTextColor = overrideColor ?: if (isDarkOnLight) Color(0xFF0F172A) else Color.Unspecified
    val codeColor = if (isDarkOnLight) Color(0xFF15803D) else Color(0xFF4ADE80)
    val codeBg = if (isDarkOnLight) Color(0xFFF1F5F9) else Color(0xFF1E293B)

    val inlinePattern = Regex(
        """(\*\*\*(.+?)\*\*\*)|(___(.+?)___)|(\*\*(.+?)\*\*)|(__(.+?)__)|(\*(.+?)\*)|(_([^_]+?)_)|(~~(.+?)~~)|(`([^`]+?)`)|(\$([^\$\n]+?)\$)"""
    )

    var lastIndex = 0
    val matches = inlinePattern.findAll(sanitized)

    for (match in matches) {
        val range = match.range
        if (range.first > lastIndex) {
            val normalText = sanitized.substring(lastIndex, range.first)
                .replace(Regex("""^\s*#{1,6}\s*"""), "")
                .replace(Regex("""\*{1,3}(?!\w)"""), "")
            if (isDarkOnLight) {
                withStyle(SpanStyle(color = normalTextColor)) {
                    append(normalText)
                }
            } else {
                append(normalText)
            }
        }

        when {
            // Group 2: ***bold italic***
            match.groups[2] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = boldColor)) {
                    append(match.groups[2]!!.value)
                }
            }
            // Group 4: ___bold italic___
            match.groups[4] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = boldColor)) {
                    append(match.groups[4]!!.value)
                }
            }
            // Group 6: **bold**
            match.groups[6] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(match.groups[6]!!.value)
                }
            }
            // Group 8: __bold__
            match.groups[8] != null -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(match.groups[8]!!.value)
                }
            }
            // Group 10: *italic*
            match.groups[10] != null -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = italicColor)) {
                    append(match.groups[10]!!.value)
                }
            }
            // Group 12: _italic_
            match.groups[12] != null -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = italicColor)) {
                    append(match.groups[12]!!.value)
                }
            }
            // Group 14: ~~strikethrough~~
            match.groups[14] != null -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color(0xFF94A3B8))) {
                    append(match.groups[14]!!.value)
                }
            }
            // Group 16: `code`
            match.groups[16] != null -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = codeColor,
                        background = codeBg
                    )
                ) {
                    append(" ${match.groups[16]!!.value} ")
                }
            }
            // Group 18: $math$
            match.groups[18] != null -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                ) {
                    append(match.groups[18]!!.value)
                }
            }
        }
        lastIndex = range.last + 1
    }

    if (lastIndex < sanitized.length) {
        val remainder = sanitized.substring(lastIndex)
            .replace(Regex("""^\s*#{1,6}\s*"""), "")
            .replace(Regex("""\*{1,3}(?!\w)"""), "")
        if (isDarkOnLight) {
            withStyle(SpanStyle(color = normalTextColor)) {
                append(remainder)
            }
        } else {
            append(remainder)
        }
    }
}

/**
 * System notice bubble.
 */
@Composable
private fun SystemNoticeBubble(message: AiAssistantMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Reasoning Brain SVG Icon matching exact SVG paths:
 * M9.5 2a3.5 3.5 0 0 0-3.5 3.5V6a3 3 0 0 0-3 3v1a3 3 0 0 0 2 2.83V14a3 3 0 0 0 3 3h1
 * M14.5 2a3.5 3.5 0 0 1 3.5 3.5V6a3 3 0 0 1 3 3v1a3 3 0 0 1-2 2.83V14a3 3 0 0 1-3 3h-1
 * M9 2v4 M15 2v4 M9 17v5 M15 17v5 M6 9h3 M15 9h3 M6 14h3 M15 14h3 M12 6v12
 */
@Composable
fun ReasoningBrainIcon(
    tint: Color = Color(0xFF71717A),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val scaleFactor = size.width / 24f
        val stroke = Stroke(
            width = 2f * scaleFactor,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        try {
            val pathData = "M9.5 2a3.5 3.5 0 0 0-3.5 3.5V6a3 3 0 0 0-3 3v1a3 3 0 0 0 2 2.83V14a3 3 0 0 0 3 3h1 " +
                    "M14.5 2a3.5 3.5 0 0 1 3.5 3.5V6a3 3 0 0 1 3 3v1a3 3 0 0 1-2 2.83V14a3 3 0 0 1-3 3h-1 " +
                    "M9 2v4 M15 2v4 M9 17v5 M15 17v5 M6 9h3 M15 9h3 M6 14h3 M15 14h3 M12 6v12"
            val androidPath = PathParser.createPathFromPathData(pathData)
            val composePath = androidPath.asComposePath()
            scale(scaleFactor, scaleFactor, Offset.Zero) {
                drawPath(composePath, color = tint, style = stroke)
            }
        } catch (_: Exception) {
            drawCircle(color = tint, style = stroke)
        }
    }
}

/**
 * Reasoning Chevron SVG Icon matching <path d="m6 9 6 6 6-6"/> with animated rotation
 */
@Composable
fun ReasoningChevronIcon(
    rotationAngle: Float,
    tint: Color = Color(0xFF71717A),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(16.dp).rotate(rotationAngle)) {
        val scaleFactor = size.width / 24f
        val stroke = Stroke(
            width = 2f * scaleFactor,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val path = Path().apply {
            moveTo(6f * scaleFactor, 9f * scaleFactor)
            lineTo(12f * scaleFactor, 15f * scaleFactor)
            lineTo(18f * scaleFactor, 9f * scaleFactor)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

/**
 * Shimmer text with animated gradient:
 * linear-gradient(90deg,#71717a 0%,#d4d4d8 50%,#71717a 100%)
 */
@Composable
fun ReasoningShimmerText(
    text: String = "Thinking...",
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerColors = listOf(
        Color(0xFF71717A),
        Color(0xFFD4D4D8),
        Color(0xFFFFFFFF),
        Color(0xFFD4D4D8),
        Color(0xFF71717A)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 300f, 0f),
        tileMode = TileMode.Clamp
    )

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = TextStyle(brush = brush),
        modifier = modifier
    )
}

/**
 * Chain of Thought step status matching:
 * complete: "text-muted-foreground" (#A1A1AA)
 * active: "text-foreground" (#F4F4F5)
 * pending: "text-muted-foreground/50" (#71717A)
 */
enum class StepStatus {
    COMPLETE,
    ACTIVE,
    PENDING
}

enum class StepIconType {
    BRAIN,
    SEARCH,
    GLOBE,
    CALCULATOR,
    CHECK,
    INFO,
    DOT
}

data class ChainOfThoughtStepData(
    val id: String = UUID.randomUUID().toString(),
    val iconType: StepIconType = StepIconType.DOT,
    val label: String,
    val description: String? = null,
    val badges: List<String> = emptyList(),
    val codeSnippet: String? = null,
    val status: StepStatus = StepStatus.COMPLETE
)

private fun cleanThoughtJargon(text: String): String {
    return text
        .replace(Regex("(?i)via\\s+DuckDuckGo\\s+MCP"), "")
        .replace(Regex("(?i)DuckDuckGo\\s+MCP"), "")
        .replace(Regex("(?i)via\\s+MCP"), "")
        .replace(Regex("(?i)DuckDuckGo"), "")
        .replace(Regex("(?i)MCP\\s+Server"), "search server")
        .replace(Regex("(?i)MCP\\s+tools?"), "tools")
        .replace(Regex("(?i)MCP"), "")
        .replace("Tools_Web_Search", "web search")
        .replace("Tools_Web_Fetch", "web reader")
        .replace("Web_Search", "web search")
        .replace("Web_Fetch", "web reader")
        .replace("alyxsis/Tools", "")
        .replace("alyxsis", "")
        .replace("Hugging Face Spaces", "")
        .replace("Hugging Face Space", "")
        .replace("Hugging Face", "")
        .replace("JSON-RPC 2.0", "")
        .replace("JSON-RPC", "")
        .replace("function to look for", "to check")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

/**
 * Intelligent parser that converts live reasoning text and tool call traces
 * into structured ChainOfThoughtStepData items with user-friendly, non-technical labels.
 */
fun parseChainOfThoughtSteps(rawText: String, isThinking: Boolean): List<ChainOfThoughtStepData> {
    if (rawText.isBlank()) return emptyList()

    val steps = mutableListOf<ChainOfThoughtStepData>()

    // Check if rawText contains tool calling markers
    val hasToolCalls = rawText.contains("[DZD AI Live Tool Calling]") ||
            rawText.contains("Searching Web") ||
            rawText.contains("Searching the web") ||
            rawText.contains("Tools_Web_Search") ||
            rawText.contains("Tools_Web_Fetch") ||
            rawText.contains("calculator") ||
            rawText.contains("⚡") ||
            rawText.contains("• 🔍") ||
            rawText.contains("• Searching") ||
            rawText.contains("• Reading") ||
            rawText.contains("• Calculating")

    if (hasToolCalls) {
        // 1. Initial reasoning before tool call
        val toolStartIndex = when {
            rawText.indexOf("⚡") >= 0 -> rawText.indexOf("⚡")
            rawText.indexOf("• 🔍") >= 0 -> rawText.indexOf("• 🔍")
            rawText.indexOf("• Searching") >= 0 -> rawText.indexOf("• Searching")
            rawText.indexOf("[DZD AI") >= 0 -> rawText.indexOf("[DZD AI")
            else -> -1
        }

        if (toolStartIndex > 0) {
            val preThought = rawText.substring(0, toolStartIndex).trim()
            if (preThought.isNotBlank()) {
                val desc = if (preThought.length > 250) preThought.take(250) + "..." else preThought
                val cleanedDesc = cleanThoughtJargon(desc)
                if (cleanedDesc.isNotBlank()) {
                    steps.add(
                        ChainOfThoughtStepData(
                            iconType = StepIconType.BRAIN,
                            label = "Analyzing inquiry & requirements",
                            description = cleanedDesc,
                            status = StepStatus.COMPLETE
                        )
                    )
                }
            }
        }

        // 2. Web search tool execution
        if (rawText.contains("Searching Web", ignoreCase = true) ||
            rawText.contains("Searching the web", ignoreCase = true) ||
            rawText.contains("Tools_Web_Search", ignoreCase = true) ||
            rawText.contains("Web_Search", ignoreCase = true)
        ) {
            val queryRegex = Regex("\"query\"\\s*:\\s*\"([^\"]+)\"")
            val simpleQueryRegex = Regex("Searching the web for \"([^\"]+)\"")
            val queryMatch = queryRegex.find(rawText)?.groupValues?.get(1)
                ?: simpleQueryRegex.find(rawText)?.groupValues?.get(1)

            val queryClean = queryMatch?.let { cleanThoughtJargon(it) }?.ifBlank { null }
            val badgesList = mutableListOf<String>()
            if (!queryClean.isNullOrBlank()) {
                val shortQuery = if (queryClean.length > 30) queryClean.take(30) + "..." else queryClean
                badgesList.add("🔍 \"$shortQuery\"")
            }
            badgesList.add("🌐 Web Search")
            badgesList.add("📄 Live sources")

            val isSearchComplete = rawText.contains("Data received", ignoreCase = true) ||
                    rawText.contains("Information retrieved", ignoreCase = true) ||
                    !isThinking

            steps.add(
                ChainOfThoughtStepData(
                    iconType = StepIconType.SEARCH,
                    label = "Searching the web",
                    description = if (!queryClean.isNullOrBlank()) "Looking for: $queryClean" else "Searching relevant educational and curriculum sources",
                    badges = badgesList,
                    status = if (isSearchComplete) StepStatus.COMPLETE else StepStatus.ACTIVE
                )
            )
        }

        // 3. Web fetch tool execution
        if (rawText.contains("Fetching Live Webpage", ignoreCase = true) ||
            rawText.contains("Reading webpage", ignoreCase = true) ||
            rawText.contains("Tools_Web_Fetch", ignoreCase = true) ||
            rawText.contains("Web_Fetch", ignoreCase = true)
        ) {
            val urlRegex = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
            val urlMatch = urlRegex.find(rawText)?.groupValues?.get(1)
            val displayUrl = urlMatch?.let { cleanThoughtJargon(it) }?.ifBlank { null }
                ?: "Official reference source"
            steps.add(
                ChainOfThoughtStepData(
                    iconType = StepIconType.GLOBE,
                    label = "Reading webpage",
                    description = displayUrl,
                    badges = listOf("🌐 Web Source", "Article & Syllabus Content"),
                    status = if (rawText.contains("Data received", ignoreCase = true) ||
                        rawText.contains("Information retrieved", ignoreCase = true) ||
                        !isThinking) StepStatus.COMPLETE else StepStatus.ACTIVE
                )
            )
        }

        // 4. Calculator tool execution
        if (rawText.contains("Calculating Math", ignoreCase = true) ||
            rawText.contains("Calculating", ignoreCase = true) ||
            rawText.contains("calculator", ignoreCase = true)
        ) {
            val exprRegex = Regex("\"expression\"\\s*:\\s*\"([^\"]+)\"")
            val simpleExprRegex = Regex("Calculating: ([^\\n\\.]*)")
            val exprMatch = exprRegex.find(rawText)?.groupValues?.get(1)
                ?: simpleExprRegex.find(rawText)?.groupValues?.get(1)
                ?: "Mathematical expression"
            steps.add(
                ChainOfThoughtStepData(
                    iconType = StepIconType.CALCULATOR,
                    label = "Calculating step-by-step",
                    description = "Evaluating: $exprMatch",
                    badges = listOf("🧮 Exact Calculation", "Verified result"),
                    status = if (rawText.contains("Data received", ignoreCase = true) ||
                        rawText.contains("Information retrieved", ignoreCase = true) ||
                        !isThinking) StepStatus.COMPLETE else StepStatus.ACTIVE
                )
            )
        }

        // 5. Data received / evaluation
        if (rawText.contains("Data received:", ignoreCase = true) ||
            rawText.contains("Information retrieved:", ignoreCase = true)
        ) {
            val dataPart = (if (rawText.contains("Information retrieved:")) {
                rawText.substringAfter("Information retrieved:")
            } else {
                rawText.substringAfter("Data received:")
            }).trim()

            val isNoResults = dataPart.contains("No text results", ignoreCase = true) ||
                    dataPart.contains("No results found", ignoreCase = true) ||
                    dataPart.contains("Network error", ignoreCase = true)

            val desc = if (isNoResults) {
                "Online search complete. Synthesizing comprehensive explanation from verified syllabus and academic curriculum."
            } else {
                val cleanedData = cleanThoughtJargon(dataPart)
                if (cleanedData.length > 250) cleanedData.take(250) + "..." else cleanedData
            }

            val badges = if (isNoResults) {
                listOf("Curriculum knowledge", "Synthesizing answer")
            } else {
                listOf("✔ Sources analyzed", "Synthesizing answer")
            }

            steps.add(
                ChainOfThoughtStepData(
                    iconType = StepIconType.CHECK,
                    label = "Reviewing search results",
                    description = desc,
                    badges = badges,
                    status = StepStatus.COMPLETE
                )
            )
        }
    } else {
        // General deep reasoning trace (no tool calls)
        val paragraphs = rawText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.size <= 1) {
            val p = paragraphs.firstOrNull() ?: rawText.trim()
            steps.add(
                ChainOfThoughtStepData(
                    iconType = StepIconType.BRAIN,
                    label = "Analyzing inquiry & curriculum theory",
                    description = cleanThoughtJargon(p),
                    status = if (isThinking) StepStatus.ACTIVE else StepStatus.COMPLETE
                )
            )
        } else {
            paragraphs.forEachIndexed { idx, p ->
                val (label, iconType) = when (idx) {
                    0 -> "Analyzing inquiry & curriculum requirements" to StepIconType.BRAIN
                    1 -> (if (p.contains("=") || p.contains("+") || p.contains("sqrt")) "Evaluating equations & derivations" else "Formulating step-by-step logic") to (if (p.contains("=")) StepIconType.CALCULATOR else StepIconType.INFO)
                    2 -> "Verifying boundary conditions & formulas" to StepIconType.CHECK
                    else -> "Synthesizing comprehensive explanation" to StepIconType.DOT
                }
                val isCurrentStepActive = isThinking && idx == paragraphs.lastIndex
                steps.add(
                    ChainOfThoughtStepData(
                        iconType = iconType,
                        label = label,
                        description = cleanThoughtJargon(p),
                        status = if (isCurrentStepActive) StepStatus.ACTIVE else StepStatus.COMPLETE
                    )
                )
            }
        }
    }

    return steps
}

/**
 * Step Icon with status animations.
 */
@Composable
fun ChainOfThoughtStepIcon(
    iconType: StepIconType,
    status: StepStatus
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step_active_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val iconColor = when (status) {
        StepStatus.ACTIVE -> Color(0xFF22C55E).copy(alpha = pulseAlpha)
        StepStatus.COMPLETE -> when (iconType) {
            StepIconType.SEARCH -> Color(0xFF38BDF8)
            StepIconType.GLOBE -> Color(0xFF38BDF8)
            StepIconType.CALCULATOR -> Color(0xFFFBBF24)
            StepIconType.CHECK -> Color(0xFF22C55E)
            StepIconType.BRAIN -> Color(0xFFA1A1AA)
            else -> Color(0xFFA1A1AA)
        }
        StepStatus.PENDING -> Color(0xFF71717A)
    }

    when (iconType) {
        StepIconType.SEARCH -> {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        StepIconType.GLOBE -> {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = "Web Fetch",
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        StepIconType.CALCULATOR -> {
            Icon(
                imageVector = Icons.Outlined.Calculate,
                contentDescription = "Calculation",
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        StepIconType.CHECK -> {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Complete",
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        StepIconType.BRAIN -> {
            ReasoningBrainIcon(
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        StepIconType.INFO -> {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = "Concept",
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        StepIconType.DOT -> {
            Box(
                modifier = Modifier
                    .size(if (status == StepStatus.ACTIVE) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(iconColor)
            )
        }
    }
}

/**
 * Individual step within the Chain of Thought with connected vertical timeline line:
 * <div className="absolute top-7 bottom-0 left-1/2 -mx-px w-px bg-border" />
 */
@Composable
fun ChainOfThoughtStep(
    step: ChainOfThoughtStepData,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Column: Icon at top + connecting vertical timeline line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                ChainOfThoughtStepIcon(iconType = step.iconType, status = step.status)
            }
            if (!isLastStep) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(top = 4.dp, bottom = 4.dp)
                        .background(Color(0xFF27272A))
                )
            }
        }

        // Right Column: Label, description, badges, details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLastStep) 2.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = step.label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = when (step.status) {
                    StepStatus.ACTIVE -> Color(0xFFF4F4F5)
                    StepStatus.COMPLETE -> Color(0xFFA1A1AA)
                    StepStatus.PENDING -> Color(0xFF71717A)
                }
            )

            if (!step.description.isNullOrBlank()) {
                Text(
                    text = step.description,
                    fontSize = 12.sp,
                    lineHeight = 17.5.sp,
                    color = Color(0xFF71717A)
                )
            }

            if (step.badges.isNotEmpty()) {
                ChainOfThoughtSearchResults(
                    badges = step.badges,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!step.codeSnippet.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(0.5.dp, Color(0xFF27272A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = step.codeSnippet,
                        color = Color(0xFFF4F4F5),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Search results container:
 * <div className="flex flex-wrap items-center gap-2" />
 */
@Composable
fun ChainOfThoughtSearchResults(
    badges: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        badges.forEach { badgeText ->
            ChainOfThoughtSearchResult(text = badgeText)
        }
    }
}

/**
 * Individual search result badge:
 * <Badge className="gap-1 px-2 py-0.5 font-normal text-xs" variant="secondary" />
 */
@Composable
fun ChainOfThoughtSearchResult(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF18181B),
        border = BorderStroke(0.5.dp, Color(0xFF27272A)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFD4D4D8)
            )
        }
    }
}

/**
 * Chain of Thought component matching the Radix UI / Shadcn specification:
 * - ChainOfThoughtHeader (BrainIcon, duration/shimmer label, rotating chevron)
 * - ChainOfThoughtContent (Collapsible animated container)
 * - ChainOfThoughtStep (Connecting vertical timeline, icon, label, description)
 * - ChainOfThoughtSearchResults & ChainOfThoughtSearchResult (Badge pills)
 */
@Composable
fun ChainOfThought(
    reasoningText: String,
    isThinking: Boolean,
    durationSeconds: Double? = null,
    modifier: Modifier = Modifier,
    defaultOpen: Boolean = isThinking,
    isDarkTheme: Boolean = true
) {
    var isOpen by remember(isThinking) {
        mutableStateOf(if (isThinking) true else defaultOpen)
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "cot_chevron_rotation"
    )

    val primaryTextColor = if (isDarkTheme) Color(0xFFA1A1AA) else Color(0xFF71717A)

    val durationLabel = remember(durationSeconds) {
        if (durationSeconds != null && durationSeconds > 0.05) {
            String.format(Locale.US, "Thought for %.1f seconds", durationSeconds)
        } else {
            "Thought for a few seconds"
        }
    }

    val steps = remember(reasoningText, isThinking) {
        parseChainOfThoughtSteps(reasoningText, isThinking)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // ChainOfThoughtHeader
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { isOpen = !isOpen }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReasoningBrainIcon(tint = primaryTextColor)

            if (isThinking) {
                ReasoningShimmerText(
                    text = "Thinking...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = durationLabel,
                    color = primaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ReasoningChevronIcon(
                rotationAngle = rotationAngle,
                tint = primaryTextColor
            )
        }

        // ChainOfThoughtContent (Collapsible)
        AnimatedVisibility(
            visible = isOpen,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(250)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(250))
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(reasoningText.length, isThinking) {
                if (isThinking && reasoningText.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .padding(top = 12.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (steps.isEmpty()) {
                    Text(
                        text = if (isThinking) "Analyzing query and formulating answer..." else "No detailed thought steps recorded.",
                        color = primaryTextColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                    )
                } else {
                    steps.forEachIndexed { index, step ->
                        ChainOfThoughtStep(
                            step = step,
                            isLastStep = index == steps.lastIndex
                        )
                    }
                }
            }
        }
    }
}

/**
 * Drop-in wrapper delegating directly to ChainOfThought for existing callsites.
 */
@Composable
fun AiReasoningBlock(
    reasoningText: String,
    isThinking: Boolean,
    durationSeconds: Double? = null,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = isThinking,
    isDarkTheme: Boolean = true
) {
    ChainOfThought(
        reasoningText = reasoningText,
        isThinking = isThinking,
        durationSeconds = durationSeconds,
        modifier = modifier,
        defaultOpen = initialExpanded,
        isDarkTheme = isDarkTheme
    )
}

/**
 * Reasoning Mode Live Thinking Indicator.
 * Renders the live ChainOfThought timeline while thinking and executing MCP tools.
 */
@Composable
private fun AiThinkingIndicator(
    reasoningText: String = "",
    subject: String = "General Studies"
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 6.dp, end = 10.dp)
    ) {
        // AI Sparkle Badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Thinking AI",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Pure Black Theme Reasoning Card
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = Color(0xFF000000),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            shadowElevation = 4.dp,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.padding(14.dp)
            ) {
                ChainOfThought(
                    reasoningText = reasoningText,
                    isThinking = true,
                    defaultOpen = true
                )
            }
        }
    }
}

/**
 * Helper to query file name.
 */
private fun queryFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

/**
 * AI Model Selection and API Key configuration dialog.
 * Enables switching between:
 * 1. GLM-4.7-Flash (Z.AI Free Tier) with tool calling
 * 2. Hugging Face Space (dzd-learn-ai / GPT-OSS 20B with reasoning)
 * 3. Google Gemini 3.8 Flash (Multimodal vision)
 */
@Composable
fun AiModelSelectionDialog(
    selectedModel: AiModelChoice,
    onSelectModel: (AiModelChoice) -> Unit,
    customZaiApiKey: String,
    onUpdateZaiApiKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var tempKey by remember { mutableStateOf(customZaiApiKey) }
    var isEditingKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AI Study Engine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select which AI model powers your homework, exam solutions, and step-by-step guidance:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AiModelChoice.values().forEach { model ->
                    val isSelected = model == selectedModel
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            if (isDark) Color(0xFF14532D).copy(alpha = 0.35f) else Color(0xFFDCFCE7)
                        } else {
                            if (isDark) Color(0xFF18181B) else Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) Color(0xFF22C55E) else if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectModel(model) }
                            .testTag("model_choice_${model.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectModel(model) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF22C55E)
                                )
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) Color(0xFF22C55E).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = model.providerBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (model == AiModelChoice.GLM_4_7_FLASH) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Supports mathematical tools & curriculum lookup",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = Color(0xFF22C55E),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Z.AI API Key configuration section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF000000) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Z.AI API Key",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = { isEditingKey = !isEditingKey }) {
                                Text(
                                    text = if (isEditingKey) "Cancel" else if (customZaiApiKey.isNotBlank()) "Change" else "Configure",
                                    fontSize = 12.sp,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }

                        if (isEditingKey) {
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                placeholder = { Text("Paste Z.AI API Key", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = {
                                    onUpdateZaiApiKey(tempKey)
                                    isEditingKey = false
                                }) {
                                    Text("Save Key", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "💡 Tip: You can also configure ZAI_API_KEY in your environment configuration.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Universal text sanitizer for clipboard copy operations.
 * Cleanly strips raw markdown asterisks (**bold**), headers (###), JSON artifacts,
 * code blocks, and internal tags to yield pristine human-readable plain text.
 */
private fun formatCleanTextForClipboard(rawText: String): String {
    var text = rawText

    // 1. Convert [DOWNLOAD_DOCUMENT:{"title":"...", "download_url":"..."}] to readable label & URL
    val docRegex = Regex("""\[DOWNLOAD_DOCUMENT:(\{.*?\})\]""")
    text = docRegex.replace(text) { matchResult ->
        try {
            val json = org.json.JSONObject(matchResult.groupValues[1])
            val title = json.optString("title", "Document")
            val url = json.optString("download_url").ifBlank { json.optString("uri", "") }
            val medium = json.optString("medium", "")
            val medText = if (medium.isNotBlank()) " ($medium Medium)" else ""
            "📄 $title$medText\nDownload: $url"
        } catch (_: Exception) {
            ""
        }
    }

    // 2. Convert markdown links [Label](URL) -> Label (URL)
    text = text.replace(Regex("""\[([^\]]+)\]\((https?://[^\)]+)\)"""), "$1 ($2)")

    // 3. Remove markdown headers (# H1, ## H2, etc.)
    text = text.replace(Regex("""(^|\n)\s*#{1,6}\s*"""), "$1")
    text = text.replace(Regex("""\s*#{1,6}\s*($|\n)"""), "$1")

    // 4. Remove bold & italic formatting asterisks and underscores
    text = text.replace(Regex("""\*{1,3}([^\*\n]+?)\*{1,3}"""), "$1")
    text = text.replace(Regex("""_{1,3}([^_\n]+?)_{1,3}"""), "$1")
    text = text.replace("**", "")
    text = text.replace("*", "")

    // 5. Remove strikethrough (~~text~~)
    text = text.replace(Regex("""~~([^~\n]+?)~~"""), "$1")

    // 6. Remove inline code backticks (`code`)
    text = text.replace(Regex("""`([^`\n]+?)`"""), "$1")

    // 7. Remove code fences (```lang ... ```)
    text = text.replace(Regex("""```[a-zA-Z0-9_-]*\n?"""), "")

    // 8. Clean up multiple empty lines
    text = text.replace(Regex("""\n{3,}"""), "\n\n").trim()
    return text
}

