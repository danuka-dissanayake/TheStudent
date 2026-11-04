package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.graphics.PathParser
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.TheStudentPreferences
import com.example.data.model.AiAssistantMessage
import com.example.data.model.AiModelChoice
import com.example.data.model.AttachedMedia
import com.example.data.model.UserProfile
import com.example.ui.components.AiChatSkeleton
import com.example.ui.components.TheStudentSystemBars
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.webview.AiWebError
import com.example.ui.webview.AiWebSessionManager
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID


private val AiWebBackground = Color(0xFF121016)


/**
 * TheStudent AI Assistant: the full-featured mobile web experience hosted in a WebView that
 * is made to behave like a native screen.
 *
 * - **Warm page.** The `WebView` belongs to [AiWebSessionManager], not to this composable, so
 *   leaving the screen and coming back re-parents an already-painted page instead of doing a
 *   cold Chromium start. See `AiWebSession` for the lifecycle, and `AiWebSessionManager.prewarm`
 *   for the warm start.
 * - **Native loading state.** The first paint shows [AiChatSkeleton], the chat screen's own
 *   silhouette, and cross-fades into the page — never a spinner over a black rectangle.
 * - **Native feel.** A JS/CSS layer injected from `assets/webview/` kills tap highlights and
 *   scrollbars, syncs the app's accent colour, drives platform haptics, keeps the composer
 *   above the keyboard, and lets the page close its own drawer before back leaves the screen.
 * - Status bar is fully transparent (`Color.TRANSPARENT`) with light/white status bar icons
 *   (`isAppearanceLightStatusBars = false`) so the clock, Wi-Fi, and battery texts are crisp
 *   and clearly visible against the dark background.
 * - Uses the web app's built-in top header bar and slide-out chat history sidebar directly
 *   (no redundant native top bar or native chat history).
 * - Includes a compact floating back button at the top-right plus system back gesture support.
 */
@SuppressLint("SetJavaScriptEnabled")
@Suppress("UNUSED_PARAMETER", "DEPRECATION")
@Composable
fun AiAssistantScreen(
    userProfile: UserProfile,
    messages: List<AiAssistantMessage> = emptyList(),
    isGenerating: Boolean = false,
    isReasoningActive: Boolean = false,
    streamingReasoningText: String = "",
    inputText: String = "",
    pendingAttachment: AttachedMedia? = null,
    selectedSubject: String = "",
    onInputChange: (String) -> Unit = {},
    onSendMessage: (String, AttachedMedia?) -> Unit = { _, _ -> },
    onAttachMedia: (AttachedMedia?) -> Unit = {},
    onClearChat: () -> Unit = {},
    onSelectSubject: (String) -> Unit = {},
    onRateMessage: (String, Boolean) -> Unit = { _, _ -> },
    onSpeakText: (String) -> Unit = {},
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
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Keep the status bar transparent and flip status bar texts/icons to light (white)
    //    so they are clearly visible against the dark AI Assistant background.
    TheStudentSystemBars(darkIcons = false)

    // Flush cookies on ON_PAUSE so the login cookie survives the app being killed (Section 4.3)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching { CookieManager.getInstance().flush() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { CookieManager.getInstance().flush() }
        }
    }

    if (!view.isInEditMode) {
        DisposableEffect(view) {
            val window = view.context.findHostingActivity()?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
            }
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false

            onDispose {
                CookieManager.getInstance().flush()
                if (window != null) {
                    window.statusBarColor = AndroidColor.TRANSPARENT
                    window.navigationBarColor = AndroidColor.TRANSPARENT
                }
                controller?.isAppearanceLightStatusBars = true
                controller?.isAppearanceLightNavigationBars = true
            }
        }
    }

    val appContext = remember(context) { context.applicationContext }
    val preferences = remember(appContext) { TheStudentPreferences(appContext) }
    val hostingActivity = remember(context) { context.findHostingActivity() }

    // One long-lived WebView for the whole process (see AiWebSession). The old
    // code built a WebView per visit and destroyed it on the way out, so every
    // re-entry paid for a cold Chromium start, a TLS handshake and a full JS boot
    // of the chat app. This reuses the warm page instead.
    val session = remember(hostingActivity, preferences) {
        hostingActivity?.let { AiWebSessionManager.obtain(it) }
    }

    val isLoading = session?.isLoading ?: true
    val loadError = session?.error
    val hasLoadError = loadError != null

    var queuedPromptSent by remember { mutableStateOf(false) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    // App session and web session stay in step. Only an actual change costs a
    // reload; the normal path re-runs the origin-locked handshake on the page
    // that is already on screen.
    LaunchedEffect(userProfile.isLoggedIn, userProfile.email, session) {
        session?.syncToken(preferences.sessionToken)
    }

    // Attachments: `<input type="file">` in the web app opens the system picker,
    // with a "Take a photo" entry — photographing a question is how most students
    // actually use this screen.
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val captured = pendingCaptureUri
        pendingCaptureUri = null

        val uris: Array<Uri>? = when {
            result.resultCode != Activity.RESULT_OK -> null
            data?.clipData != null -> {
                val clip = data.clipData!!
                Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
            }
            data?.data != null -> arrayOf(data.data!!)
            captured != null -> arrayOf(captured)
            else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        }
        session?.deliverFileSelection(uris)
    }

    // Microphone permission for the voice input inside the web app.
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> session?.deliverAudioPermission(granted) }

    val accentHex = remember {
        String.format(Locale.US, "#%06X", 0xFFFFFF and EmeraldLight.toArgb())
    }
    val fontScale = LocalDensity.current.fontScale

    if (session != null) {
        // Refreshed every recomposition so the page always sees current state.
        SideEffect {
            session.audioPermissionRequester = {
                runCatching { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            }
            session.fileChooserHandler = { params ->
                val picker = runCatching { params?.createIntent() }.getOrNull()
                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(
                            Intent.EXTRA_MIME_TYPES,
                            arrayOf("image/*", "application/pdf", "text/*")
                        )
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                val capture = cameraCaptureIntent(context)
                pendingCaptureUri = capture?.second
                val chooser = Intent.createChooser(picker, "Attach a file").apply {
                    if (capture != null) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(capture.first))
                    }
                }
                runCatching { fileChooserLauncher.launch(chooser) }
                    .onFailure { session.deliverFileSelection(null) }
            }
            // Real platform haptics instead of a web page's silence. Posted to the
            // view because the bridge calls this from its own thread.
            session.hapticFeedback = {
                view.post { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
            }
            session.themeProvider = {
                JSONObject()
                    .put("accent", accentHex)
                    .put("mode", "dark")
                    .put("fontScale", fontScale.toDouble())
            }
        }
    }

    // System back: let the page close its own drawer or dialog first, then walk
    // its history, and only then leave the screen.
    BackHandler {
        if (session == null || !session.handleBack()) onBack()
    }

    // A prompt queued from another screen ("Ask AI" on a past paper) is typed
    // into the web composer once the page is actually interactive.
    LaunchedEffect(inputText, isLoading) {
        if (inputText.isBlank()) {
            queuedPromptSent = false
            return@LaunchedEffect
        }
        if (isLoading || queuedPromptSent) return@LaunchedEffect
        queuedPromptSent = true
        session?.setComposerText(inputText)
        onInputChange("")
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AiWebBackground)
            .testTag("ai_assistant_screen")
    ) {
        // WebView container padded below the transparent status bar and above the nav bar / keyboard
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.ime)
                )
                .background(AiWebBackground)
        ) {
            if (session != null) {
                // The WebView belongs to the session and is only re-parented here,
                // so leaving and returning to this screen costs nothing: the page,
                // its scroll position and any streaming answer are still there.
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ai_assistant_webview"),
                    factory = { session.attach() },
                    onRelease = { session.detach() }
                )
            }

            // The chat screen's own silhouette, not a spinner: the page fades in
            // underneath it, so a load reads as "the app is starting" rather than
            // "a web page is downloading".
            AnimatedVisibility(
                visible = isLoading && !hasLoadError,
                enter = fadeIn(animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(220))
            ) {
                AiChatSkeleton(modifier = Modifier.fillMaxSize())
            }

            // Offline / connection error fallback state. The copy is specific
            // about what failed: "you're offline" and "our server is down" are
            // different problems and students react differently to them.
            if (hasLoadError) {
                val errorTitle = when (loadError) {
                    AiWebError.OFFLINE -> "You're offline"
                    AiWebError.TIMEOUT -> "The connection timed out"
                    AiWebError.SERVER -> "TheStudent AI is taking a break"
                    else -> "Unable to load TheStudent AI"
                }
                val errorBody = when (loadError) {
                    AiWebError.OFFLINE ->
                        "Check your mobile data or Wi-Fi and try again. Your chat is safe."
                    AiWebError.TIMEOUT ->
                        "The network is slow right now. Give it another go in a moment."
                    AiWebError.SERVER ->
                        "Our servers didn't answer. Nothing was lost — try again."
                    else ->
                        "Something went wrong while opening the chat. Try again."
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AiWebBackground)
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1B24),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.WifiOff,
                                    contentDescription = "Offline",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Text(
                            text = errorTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = errorBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { session?.reload() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Try again",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Small floating back button positioned in the top-right header space
            // so it never overlaps the web app's top-left sidebar toggle, logo, or Private selector.
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xCC1C1923),
                border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .testTag("ai_assistant_back_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to app",
                        tint = Color(0xFFE4E4E7),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Exit",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE4E4E7)
                    )
                }
            }
        }
    }
}

/**
 * "Take a photo" entry for the attachment chooser. The shot is written straight
 * into the app cache and handed to the page as a `content://` Uri, so no storage
 * permission is needed on any API level.
 */
private fun cameraCaptureIntent(context: Context): Pair<Intent, Uri>? {
    val dir = File(context.cacheDir, "ai_captures").apply { mkdirs() }
    val file = File(dir, "question_" + System.currentTimeMillis() + ".jpg")
    val uri = runCatching {
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }.getOrNull() ?: return null

    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (intent.resolveActivity(context.packageManager) == null) return null
    return intent to uri
}

private fun Context.findHostingActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

data class ChatSourceItem(
    val title: String,
    val url: String,
    val domain: String,
    val faviconUrl: String = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
)

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

fun parseChainOfThoughtSteps(rawText: String, isThinking: Boolean): List<ChainOfThoughtStepData> {
    if (rawText.isBlank()) return emptyList()

    val steps = mutableListOf<ChainOfThoughtStepData>()
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

    return steps
}

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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLastStep) 2.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = step.label,
                fontSize = 14.sp,
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
                    fontSize = 13.sp,
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
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

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
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFD4D4D8)
            )
        }
    }
}

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
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = durationLabel,
                    color = primaryTextColor,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ReasoningChevronIcon(
                rotationAngle = rotationAngle,
                tint = primaryTextColor
            )
        }

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
                        fontSize = 13.5.sp,
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
                                            fontSize = 11.5.sp,
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
                                    fontSize = 12.5.sp,
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
                                            fontSize = 12.sp,
                                            color = Color(0xFF22C55E),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

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
                                    fontSize = 13.sp,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }

                        if (isEditingKey) {
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                placeholder = { Text("Paste Z.AI API Key", fontSize = 13.sp) },
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
                            fontSize = 12.sp,
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
