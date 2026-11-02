package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.local.AssistantHandshake
import com.example.data.local.WebAssistantConfig
import com.example.data.local.WebAssistantSession
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * TheStudent AI, served by the website but framed as an ordinary app screen.
 *
 * Everything around the web content is native: the header, the back button, the
 * loading bar and the error card. Nothing shows a URL, and no link is allowed to
 * hand the student to Chrome — [WebAssistantConfig.isAllowedUrl] keeps the whole
 * journey on one host.
 *
 * Sign-in
 * -------
 * When [authCode] is present the screen does NOT load the site's home page. It
 * POSTs a short-lived, single-use code + PKCE verifier to the website's exchange
 * route with [WebView.postUrl]. The code lives in the request BODY, never in a
 * URL. The website exchanges it server-to-server, rotates its session and sets
 * an `HttpOnly; Secure; SameSite=Lax` cookie; JavaScript in this WebView never
 * sees a token. When there is no code the site opens in guest mode.
 *
 * Bridge
 * ------
 * Web -> app messages use `WebViewCompat.addWebMessageListener` with an EXACT
 * origin allowlist, a main-frame + origin re-check, a size cap and an event
 * allowlist. `addJavascriptInterface` is never used. If the WebView build does
 * not support the feature we fail closed: no bridge is injected at all.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAssistantScreen(
    baseUrl: String,
    studentName: String?,
    /** Opaque `sub` (users.id) from the SSO mint. NEVER an email or a token. */
    studentId: String?,
    isDarkTheme: Boolean,
    initialPrompt: String?,
    authPending: Boolean,
    authCode: String?,
    exchangeUrl: String?,
    authVerifier: String?,
    authState: String?,
    authFailed: Boolean,
    onClose: () -> Unit,
    onMessage: (String) -> Unit = {},
    onRetrySignIn: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    onBridgeEvent: (String, JSONObject) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val pageUrl = remember(baseUrl, isDarkTheme, initialPrompt) {
        WebAssistantConfig.pageUrl(baseUrl, isDarkTheme, initialPrompt)
    }

    // Resolve the SSO material once. A code whose exchange URL points at any
    // other host is discarded, so a misconfigured backend can never receive it.
    val code = authCode?.takeIf { it.isNotBlank() }
    val target = exchangeUrl?.takeIf {
        it.isNotBlank() && WebAssistantConfig.isAllowedExchangeUrl(it, baseUrl)
    }
    val verifier = authVerifier?.takeIf { it.isNotBlank() }
    val state = authState?.takeIf { it.isNotBlank() }
    val ssoReady = code != null && target != null && verifier != null && state != null
    val postData = if (ssoReady) {
        AssistantHandshake.formBody(code!!, verifier!!, state!!, isDarkTheme)
    } else {
        null
    }
    // A new code means a new single-use attempt, so it gets a fresh WebView.
    val navKey = if (ssoReady) "sso:$code" else "guest:$pageUrl"

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val fileCallback = remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // The site offers "Summarize document (PDF, Word)". Without a file chooser
    // WebChromeClient that button silently does nothing inside a WebView.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val chosen: Array<Uri> = if (uri == null) emptyArray() else arrayOf(uri)
        fileCallback.value?.onReceiveValue(chosen)
        fileCallback.value = null
    }

    fun pushIdentity(view: WebView?) {
        // With SSO the HttpOnly cookie IS the identity. Re-posting a JS identity
        // could make the website sign its own session out on an "account change",
        // so we deliberately do not inject it in the signed-in flow.
        if (ssoReady) return
        val script = WebAssistantConfig.identityScript(studentName, studentId, isDarkTheme)
        runCatching { view?.evaluateJavascript(script, null) }
    }

    fun createWebView(ctx: Context): WebView = WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        setBackgroundColor(android.graphics.Color.TRANSPARENT)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = false
            // No target="_blank" / window.open: new windows load in this view.
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString + " TheStudentAndroid/1.0"
            setGeolocationEnabled(false)
        }
        WebAssistantSession.prepare(this)

        // -------------------------------------------------------------------
        // Origin-pinned web -> app channel. Installed BEFORE any page loads.
        // -------------------------------------------------------------------
        val allowedOrigin = WebAssistantConfig.originOf(baseUrl)
        val allowedHost = WebAssistantConfig.hostOf(baseUrl)
        if (allowedHost != null &&
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        ) {
            runCatching {
                WebViewCompat.addWebMessageListener(
                    this,
                    AssistantHandshake.BRIDGE_OBJECT,
                    setOf(allowedOrigin),
                    object : WebViewCompat.WebMessageListener {
                        override fun onPostMessage(
                            view: WebView,
                            message: WebMessageCompat,
                            sourceOrigin: Uri,
                            isMainFrame: Boolean,
                            replyProxy: JavaScriptReplyProxy
                        ) {
                            // Defence in depth: the allowlist is enforced by
                            // WebView, but we re-check origin, scheme and frame
                            // before a single byte is acted on.
                            if (!isMainFrame) return
                            if (!"https".equals(sourceOrigin.scheme, ignoreCase = true)) return
                            val host = sourceOrigin.host
                            if (host == null || !host.equals(allowedHost, ignoreCase = true)) return

                            val raw = message.data ?: return
                            if (raw.length > AssistantHandshake.MAX_BRIDGE_MESSAGE) return
                            val event = runCatching { JSONObject(raw) }.getOrNull() ?: return
                            val type = event.optString("type")
                            // Explicit allowlist: no credential events exist.
                            if (!AssistantHandshake.isAllowedEvent(type)) return
                            runCatching { replyProxy.postMessage("{\"ok\":true}") }

                            when (type) {
                                "SESSION_EXPIRED" -> onSessionExpired()
                                "CLOSE" -> onClose()
                                "NEW_CHAT" -> {
                                    isLoading = true
                                    loadError = null
                                    webViewRef.value?.loadUrl(
                                        WebAssistantConfig.pageUrl(baseUrl, isDarkTheme, null)
                                    )
                                }
                                else -> onBridgeEvent(type, event)
                            }
                        }
                    }
                )
            }.onFailure {
                // Fail closed: an old/broken WebView simply has no bridge.
                Log.w("WebAssistant", "WebMessageListener unavailable; bridge disabled", it)
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val target = request?.url?.toString().orEmpty()
                // Same host and HTTPS stay here. Anything else is consumed, so
                // the student never lands in a browser with a visible URL.
                if (WebAssistantConfig.isAllowedUrl(target, baseUrl)) return false
                onMessage("That link can't be opened here.")
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isLoading = true
                loadError = null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
                if (!ssoReady) {
                    pushIdentity(view)
                    // A hydrated SPA may register its message listener a beat
                    // after onPageFinished. Retry only if the page no longer
                    // holds the value (for example after a guest-sign-in
                    // reload). Sending an identical identity twice is NOT
                    // harmless: the website signs its session out when userId
                    // changes, so a duplicate post could interrupt a redirect.
                    scope.launch {
                        delay(700)
                        if (webViewRef.value !== view) return@launch
                        runCatching {
                            view?.evaluateJavascript(
                                "(function(){try{" +
                                    "return !!(window.__THESTUDENT_USER__ && " +
                                    "window.__THESTUDENT_USER__.type==='THESTUDENT_USER')" +
                                    "}catch(e){return false}})()"
                            ) { present ->
                                if (present != "true") pushIdentity(view)
                            }
                        }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    isLoading = false
                    loadError = "Couldn't reach TheStudent AI. Check your connection."
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                isLoading = false
                loadError = "This connection isn't secure."
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                fileCallback.value?.onReceiveValue(emptyArray<Uri>())
                fileCallback.value = filePathCallback
                val mime = fileChooserParams?.acceptTypes
                    ?.firstOrNull { it.isNotBlank() && it != "*/*" }
                    ?: "*/*"
                runCatching { filePicker.launch(mime) }.onFailure {
                    fileCallback.value?.onReceiveValue(emptyArray<Uri>())
                    fileCallback.value = null
                }
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // Web content may ask for the microphone. Grant only what the
                // app itself is already allowed to use; deny everything else.
                val wanted = request?.resources?.toList().orEmpty()
                val audio = wanted.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                val video = wanted.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                val granted = wanted.filter { resource ->
                    (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE && audio) ||
                        (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE && video)
                }
                if (granted.isNotEmpty() && granted.size == wanted.size) {
                    request?.grant(granted.toTypedArray())
                } else {
                    request?.deny()
                }
            }
        }

        // Downloads would otherwise be handed to the system browser.
        setDownloadListener { _, _, _, _, _ -> onMessage("Downloads aren't available here.") }
    }

    // The system back button walks the site's history first, then closes the
    // screen. MainActivity's "return to Home" handler is disabled while open.
    BackHandler(enabled = true) {
        val view = webViewRef.value
        if (view != null && view.canGoBack()) view.goBack() else onClose()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F7F4))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("web_assistant_screen")
    ) {
        Surface(
            color = if (isDarkTheme) Color(0xFF152238) else Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TheStudent AI",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        "Your study assistant",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = {
                    // Reload the plain embed URL: no ?q=, no stale chat. The
                    // HttpOnly cookie keeps the student signed in.
                    isLoading = true
                    loadError = null
                    webViewRef.value?.loadUrl(
                        WebAssistantConfig.pageUrl(baseUrl, isDarkTheme, null)
                    )
                }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "New chat",
                        tint = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                }
                Text(
                    "AI",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF059669))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
        }

        // A failed automatic sign-in never blocks the assistant: the student
        // can still use it as a guest and retry with one tap.
        if (authFailed) {
            Surface(color = if (isDarkTheme) Color(0xFF3A2A12) else Color(0xFFFFF4D6)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Couldn't sign you in automatically. You're using TheStudent AI as a guest.",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetrySignIn) { Text("Retry") }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (authPending) {
                // Native "secure sign-in" state. No WebView exists yet, so no
                // page can observe a half-finished handshake.
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = if (isDarkTheme) Color(0xFF152238) else Color.White,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(
                            "Signing you in securely…",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            "You won't have to sign in again.",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                // A different page URL / code / theme gets a fresh WebView. The
                // old one is removed from its parent and destroyed, otherwise
                // WebView keeps its memory after removal. `owned` belongs to
                // this key, so a stale dispose can never destroy the WebView
                // that replaced it.
                key(navKey) {
                    val owned = remember { mutableStateOf<WebView?>(null) }
                    AndroidView(
                        factory = { ctx ->
                            val view = createWebView(ctx)
                            (view.parent as? ViewGroup)?.removeView(view)
                            owned.value = view
                            webViewRef.value = view
                            if (ssoReady && postData != null && target != null) {
                                // The code is in the POST BODY, never in a URL.
                                view.postUrl(target, postData)
                            } else {
                                view.loadUrl(pageUrl)
                            }
                            view
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("web_assistant_webview")
                    )
                    DisposableEffect(Unit) {
                        onDispose {
                            val view = owned.value
                            if (view != null) {
                                (view.parent as? ViewGroup)?.removeView(view)
                                view.stopLoading()
                                view.destroy()
                                if (webViewRef.value === view) webViewRef.value = null
                                owned.value = null
                            }
                            WebAssistantSession.flush()
                        }
                    }
                }

                if (isLoading && loadError == null && progress < 100) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }

            if (loadError != null && !authPending) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "TheStudent AI is unavailable",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            loadError.orEmpty(),
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )
                        Button(onClick = {
                            loadError = null
                            isLoading = true
                            webViewRef.value?.reload()
                        }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Try again")
                        }
                    }
                }
            }
        }
    }
}
