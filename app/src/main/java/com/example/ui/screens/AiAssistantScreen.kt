package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.core.view.WindowCompat
import com.example.data.local.TheStudentPreferences
import com.example.data.model.AiAssistantMessage
import com.example.data.model.AiModelChoice
import com.example.data.model.AttachedMedia
import com.example.data.model.UserProfile
import com.example.ui.components.TheStudentSystemBars
import com.example.ui.theme.EmeraldPrimary
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

const val AI_ASSISTANT_WEB_URL = "https://chatbot-liart-rho-81.vercel.app/"
private const val AI_ASSISTANT_TRUSTED_ORIGIN = "https://chatbot-liart-rho-81.vercel.app"
private const val AI_ASSISTANT_TRUSTED_HOST = "chatbot-liart-rho-81.vercel.app"

private val AiWebBackground = Color(0xFF121016)
private val AiWebBackgroundInt = AndroidColor.parseColor("#121016")

/**
 * Origin-verified bridge for the "Login with TheStudent Android App" button on the Web Login Screen.
 * Returns the 64-hex session token ONLY when the WebView is currently on `https://chatbot-liart-rho-81.vercel.app`.
 */
private class TheStudentAppLoginBridge(
    private val webViewProvider: () -> WebView?,
    private val tokenProvider: () -> String?
) {
    private fun isOnTrustedOrigin(): Boolean {
        val wv = webViewProvider() ?: return false
        val currentUrl = try {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                wv.url
            } else {
                var urlResult: String? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                wv.post {
                    urlResult = wv.url
                    latch.countDown()
                }
                latch.await(1, java.util.concurrent.TimeUnit.SECONDS)
                urlResult
            }
        } catch (_: Exception) {
            null
        } ?: return false

        val uri = runCatching { Uri.parse(currentUrl) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(AI_ASSISTANT_TRUSTED_HOST, ignoreCase = true)
    }

    @JavascriptInterface
    fun isAndroidApp(): Boolean = isOnTrustedOrigin()

    @JavascriptInterface
    fun getSessionToken(): String {
        if (!isOnTrustedOrigin()) return ""
        val token = tokenProvider()?.trim().orEmpty()
        return if (token.matches(Regex("^[A-Fa-f0-9]{32,256}$"))) token else ""
    }
}

/**
 * Single, origin-locked authentication handshake:
 * - Verifies that the WebView is currently on `https://chatbot-liart-rho-81.vercel.app`.
 * - Sends ONLY the opaque 64-hex session token via `window.postMessage` with a strict
 *   `targetOrigin` of `https://chatbot-liart-rho-81.vercel.app`.
 * - No `@JavascriptInterface`, no `localStorage`, and no unverified client profile JSON.
 * - The web app verifies the token server-to-server against `GET /api/auth.php?action=me`.
 */
private fun postOriginLockedAuthHandshake(
    webView: WebView?,
    sessionToken: String?
) {
    if (webView == null || sessionToken.isNullOrBlank()) return
    val currentUri = runCatching { Uri.parse(webView.url.orEmpty()) }.getOrNull() ?: return
    val isTrustedOrigin = currentUri.scheme.equals("https", ignoreCase = true) &&
        currentUri.host.equals(AI_ASSISTANT_TRUSTED_HOST, ignoreCase = true)
    if (!isTrustedOrigin) return

    // Only allow hex session tokens to be interpolated
    if (!sessionToken.matches(Regex("^[A-Fa-f0-9]{32,256}$"))) return

    val quotedToken = JSONObject.quote(sessionToken)
    val quotedOrigin = JSONObject.quote(AI_ASSISTANT_TRUSTED_ORIGIN)
    val script = """
        (function() {
            if (window.location.origin !== $quotedOrigin) return;
            var sendHandshake = function() {
                if (window.location.origin === $quotedOrigin) {
                    window.postMessage(
                        { type: 'THESTUDENT_AUTH_HANDSHAKE', token: $quotedToken },
                        $quotedOrigin
                    );
                }
            };
            sendHandshake();
            setTimeout(sendHandshake, 350);
            setTimeout(sendHandshake, 1000);
            setTimeout(sendHandshake, 2500);
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}

/**
 * TheStudent AI Assistant Screen powered directly by the full-featured mobile web experience
 * at [AI_ASSISTANT_WEB_URL].
 *
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

    // 1. Keep the status bar transparent and flip status bar texts/icons to light (white)
    //    so they are clearly visible against the dark AI Assistant background.
    TheStudentSystemBars(darkIcons = false)

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

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(true) }
    var hasLoadError by remember { mutableStateOf(false) }

    val preferences = remember(context) { TheStudentPreferences(context) }

    LaunchedEffect(userProfile.isLoggedIn, isPageLoading) {
        val wv = webViewRef
        if (wv != null && !isPageLoading) {
            postOriginLockedAuthHandshake(wv, preferences.sessionToken)
        }
    }

    // File chooser state for `<input type="file">` (photo, PDF, document attachments in the web app)
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        filePathCallback = null
        if (callback == null) return@rememberLauncherForActivityResult

        val data = result.data
        val uris: Array<Uri>? = when {
            result.resultCode != Activity.RESULT_OK -> null
            data?.clipData != null -> {
                val clip = data.clipData!!
                Array(clip.itemCount) { idx -> clip.getItemAt(idx).uri }
            }
            data?.data != null -> arrayOf(data.data!!)
            else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        }
        callback.onReceiveValue(uris)
    }

    // Microphone permission state for WebRTC / voice input inside the web app
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (request != null) {
            if (granted) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
        }
    }

    // System back gesture/button: navigate back within WebView history first, else exit screen
    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    // If a pre-filled prompt was queued from another screen (e.g. "Ask AI" on a past paper),
    // populate the web app's composer once loaded.
    LaunchedEffect(inputText, isPageLoading) {
        val wv = webViewRef
        if (!isPageLoading && wv != null && inputText.isNotBlank()) {
            val escapedPrompt = JSONObject.quote(inputText)
            val script = """
                (function() {
                    var el = document.querySelector('textarea, input[type="text"]');
                    if (el) {
                        var proto = el.tagName === 'TEXTAREA'
                            ? window.HTMLTextAreaElement.prototype
                            : window.HTMLInputElement.prototype;
                        var setter = Object.getOwnPropertyDescriptor(proto, 'value');
                        if (setter && setter.set) {
                            setter.set.call(el, $escapedPrompt);
                        } else {
                            el.value = $escapedPrompt;
                        }
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.focus();
                    }
                })();
            """.trimIndent()
            wv.evaluateJavascript(script, null)
            onInputChange("")
        }
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
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ai_assistant_webview"),
                factory = { ctx ->
                    val host = FrameLayout(ctx).apply {
                        setBackgroundColor(AiWebBackgroundInt)
                    }

                    val webView = WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AiWebBackgroundInt)

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, false)

                        addJavascriptInterface(
                            TheStudentAppLoginBridge(
                                webViewProvider = { this },
                                tokenProvider = { preferences.sessionToken }
                            ),
                            "TheStudentAndroid"
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        webChromeClient = object : WebChromeClient() {
                            private var customView: View? = null
                            private var customViewCallback: CustomViewCallback? = null

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (customView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                customView = view
                                customViewCallback = callback
                                view?.let {
                                    host.addView(
                                        it,
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                            }

                            override fun onHideCustomView() {
                                customView?.let { host.removeView(it) }
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (request == null) return
                                val originUri = request.origin
                                val isTrustedOrigin = originUri != null &&
                                    originUri.scheme.equals("https", ignoreCase = true) &&
                                    originUri.host.equals(AI_ASSISTANT_TRUSTED_HOST, ignoreCase = true)
                                if (!isTrustedOrigin) {
                                    request.deny()
                                    return
                                }
                                val needsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                                if (needsAudio) {
                                    val hasAudioPerm = ContextCompat.checkSelfPermission(
                                        ctx,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasAudioPerm) {
                                        request.grant(request.resources)
                                    } else {
                                        pendingPermissionRequest?.deny()
                                        pendingPermissionRequest = request
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    request.deny()
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                callback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = callback

                                val chooserIntent = try {
                                    fileChooserParams?.createIntent()
                                } catch (_: Exception) {
                                    null
                                } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                    putExtra(
                                        Intent.EXTRA_MIME_TYPES,
                                        arrayOf("image/*", "application/pdf", "text/*")
                                    )
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }

                                return try {
                                    fileChooserLauncher.launch(chooserIntent)
                                    true
                                } catch (_: ActivityNotFoundException) {
                                    filePathCallback?.onReceiveValue(null)
                                    filePathCallback = null
                                    false
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                hasLoadError = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoading = false
                                postOriginLockedAuthHandshake(view, preferences.sessionToken)
                                CookieManager.getInstance().flush()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isPageLoading = false
                                    hasLoadError = true
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUri = request?.url ?: return false
                                val scheme = targetUri.scheme?.lowercase(Locale.ROOT) ?: return true
                                val host = targetUri.host?.lowercase(Locale.ROOT).orEmpty()

                                // Only allow HTTPS pages on the trusted AI Assistant origin to load inside the WebView
                                if (scheme == "https" && host == AI_ASSISTANT_TRUSTED_HOST) {
                                    return false
                                }

                                return try {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            try {
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                    val dmRequest = DownloadManager.Request(Uri.parse(url)).apply {
                                        setMimeType(mimeType)
                                        addRequestHeader("User-Agent", userAgent)
                                        CookieManager.getInstance().getCookie(url)?.let { cookie ->
                                            addRequestHeader("Cookie", cookie)
                                        }
                                        setTitle(fileName)
                                        setDescription("Downloading from TheStudent AI")
                                        setNotificationVisibility(
                                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                        )
                                        setDestinationInExternalPublicDir(
                                            Environment.DIRECTORY_DOWNLOADS,
                                            fileName
                                        )
                                    }
                                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    dm.enqueue(dmRequest)
                                    Toast.makeText(ctx, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
                                } else {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            } catch (_: Exception) {
                                runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        }

                        val initialUrl = if (inputText.isNotBlank()) {
                            val built = Uri.parse(AI_ASSISTANT_WEB_URL)
                                .buildUpon()
                                .appendQueryParameter("q", inputText.take(2000))
                                .build()
                                .toString()
                            onInputChange("")
                            built
                        } else {
                            AI_ASSISTANT_WEB_URL
                        }
                        loadUrl(initialUrl)
                    }

                    webViewRef = webView
                    host.addView(webView)
                    host
                },
                onRelease = { host ->
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    pendingPermissionRequest?.deny()
                    pendingPermissionRequest = null
                    for (i in 0 until host.childCount) {
                        val child = host.getChildAt(i)
                        if (child is WebView) {
                            child.stopLoading()
                            child.onPause()
                            child.destroy()
                        }
                    }
                    host.removeAllViews()
                    webViewRef = null
                }
            )

            // Loading overlay matching the web app's dark background so there is never a white flash
            AnimatedVisibility(
                visible = isPageLoading && !hasLoadError,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(220))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AiWebBackground),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Offline / connection error fallback state
            if (hasLoadError) {
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
                            text = "Unable to load TheStudent AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Please check your internet connection and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                hasLoadError = false
                                isPageLoading = true
                                webViewRef?.reload() ?: webViewRef?.loadUrl(AI_ASSISTANT_WEB_URL)
                            },
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
                                text = "Retry",
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
