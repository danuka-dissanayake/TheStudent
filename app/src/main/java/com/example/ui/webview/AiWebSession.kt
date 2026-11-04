package com.example.ui.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.data.local.TheStudentPreferences
import org.json.JSONObject

/**
 * One long-lived `WebView` for the AI Assistant screen.
 *
 * The screen used to build a `WebView` in `AndroidView(factory = …)` and destroy it
 * in `onRelease`, which meant every visit paid for a cold Chromium start, a fresh
 * TLS handshake, and a full JavaScript boot of the chat app. This class keeps that
 * work alive across navigation instead:
 *
 * ```
 *   attach()   → the container is handed to AndroidView (page already painted)
 *   detach()   → Compose takes the view off screen, the page keeps its state
 *   destroy()  → logout, renderer crash, 15 min idle, or low memory
 * ```
 *
 * Everything in here runs on the main thread except the JavaScript bridge
 * ([AiWebBridge]), which runs on the WebView's private bridge thread and only
 * touches thread-safe fields and Compose snapshot state.
 */
@SuppressLint("SetJavaScriptEnabled")
class AiWebSession internal constructor(val ownerActivity: Activity) {

    private val app: Context = ownerActivity.applicationContext
    private val preferences = TheStudentPreferences(app)
    private val mainHandler = Handler(Looper.getMainLooper())

    // ── State the Compose screen reads ────────────────────────────────────────

    /** A main-frame load is in flight and nothing is painted yet. */
    var isLoading by mutableStateOf(true)
        private set

    /** The page has committed its first frame — safe to hide the skeleton. */
    var isContentVisible by mutableStateOf(false)
        private set

    /** Non-null while the offline / error overlay should be showing. */
    var error by mutableStateOf<AiWebError?>(null)
        private set

    /** True when the page itself wants to consume the next back press. */
    var canWebHandleBack by mutableStateOf(false)
        private set

    // ── Hooks the screen fills in ─────────────────────────────────────────────

    /** Accent / colour scheme / font scale, read by the page through the bridge. */
    var themeProvider: () -> JSONObject = { JSONObject().put("mode", "dark") }

    /** Called when the page opens `<input type="file">`. The screen owns the intent. */
    var fileChooserHandler: ((WebChromeClient.FileChooserParams?) -> Unit)? = null

    /** Called when the page wants the microphone (voice input inside the chat). */
    var audioPermissionRequester: (() -> Unit)? = null

    /** Light tap feedback. The screen posts it to a real `View`. */
    var hapticFeedback: (() -> Unit)? = null

    // ── Bridge ────────────────────────────────────────────────────────────────

    internal val bridge: AiWebBridge = AiWebBridge(
        app = app,
        tokenProvider = { preferences.sessionToken },
        themeProvider = { themeProvider() },
        hapticHandler = { hapticFeedback?.invoke() },
        reportRoute = { open -> canWebHandleBack = open },
        reportReady = {
            isContentVisible = true
            isLoading = false
        },
        launchIntent = { intent -> launchExternal(intent) }
    )

    // ── View plumbing ─────────────────────────────────────────────────────────

    /** Stable host for the WebView so it can be re-parented into any Compose tree. */
    val container: FrameLayout = FrameLayout(ownerActivity)

    var webView: WebView? = null
        private set

    private var attached = false
    private var lastDetachedAt = SystemClock.uptimeMillis()
    private var cacheRetryUsed = false
    private var initialLoadStarted = false
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermission: PermissionRequest? = null
    private var lastSyncedToken: String = preferences.sessionToken.orEmpty().trim()
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    val isAttached: Boolean get() = attached

    /** True once the session has been off screen long enough to be worth dropping. */
    fun isExpired(): Boolean =
        !attached && SystemClock.uptimeMillis() - lastDetachedAt > AiWebConfig.KEEP_ALIVE_MS

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Build the WebView and start loading before the screen is ever shown.
     * Called by [AiWebSessionManager.prewarm] shortly after the app starts.
     */
    fun warmUp() {
        ensureLoaded(ensureWebView())
        lastDetachedAt = SystemClock.uptimeMillis()
    }

    /** Hand the container to `AndroidView`. Resumes the page if it was paused. */
    fun attach(): FrameLayout {
        (container.parent as? ViewGroup)?.removeView(container)
        attached = true
        lastDetachedAt = SystemClock.uptimeMillis()
        val wv = ensureWebView()
        // A session created on demand (no prewarm) still has to load the page.
        ensureLoaded(wv)
        runCatching { wv.onResume() }
        // A warm page is already painted: no skeleton on the way back in.
        if (isContentVisible) isLoading = false
        installNetworkWatch()
        return container
    }

    /** Compose is dropping the view. Keep the page, stop the work. */
    fun detach() {
        attached = false
        lastDetachedAt = SystemClock.uptimeMillis()
        runCatching { webView?.onPause() }
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
    }

    /** The whole app is leaving the foreground. */
    fun onAppBackground() {
        runCatching { webView?.onPause() }
        WebView.pauseTimers()
        runCatching { CookieManager.getInstance().flush() }
    }

    /** Back to the foreground: timers resume, page resumes only if visible. */
    fun onAppForeground() {
        WebView.resumeTimers()
        if (attached) runCatching { webView?.onResume() }
    }

    /** Throw the page away. The next visit starts cold, by design. */
    fun destroy() {
        uninstallNetworkWatch()
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = null
        pendingPermission?.deny()
        pendingPermission = null
        canWebHandleBack = false
        initialLoadStarted = false
        val wv = webView
        webView = null
        if (wv != null) {
            runCatching { container.removeView(wv) }
            runCatching { wv.stopLoading() }
            runCatching { wv.loadUrl("about:blank") }
            runCatching { wv.onPause() }
            runCatching { wv.removeJavascriptInterface(AiWebConfig.JS_BRIDGE) }
            runCatching { wv.webChromeClient = null }
            runCatching { wv.webViewClient = null }
            runCatching { wv.destroy() }
        }
        runCatching { container.removeAllViews() }
    }

    // ── Account / token sync ──────────────────────────────────────────────────

    /**
     * Keep the app session and the web session in step. Only a *change* costs a
     * reload; the common path (same token) just re-runs the origin-locked
     * handshake for the page that is already on screen.
     */
    fun syncToken(token: String?) {
        val next = token.orEmpty().trim()
        bridge.cachedToken = next
        if (next == lastSyncedToken) {
            if (isContentVisible) postAuthHandshake()
            return
        }
        lastSyncedToken = next
        val cookies = CookieManager.getInstance()
        cookies.removeAllCookies {
            cookies.flush()
            mainHandler.post { reload() }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** Manual retry: clears the error, drops any cached-page fallback, reloads. */
    fun reload() {
        error = null
        cacheRetryUsed = false
        isContentVisible = false
        isLoading = true
        val wv = ensureWebView()
        runCatching {
            wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
            if (wv.url.isNullOrBlank()) wv.loadUrl(AI_ASSISTANT_WEB_URL) else wv.reload()
        }
    }

    /**
     * System back. Returns true when the screen should stay (the page or its
     * history consumed the press).
     */
    fun handleBack(): Boolean {
        if (canWebHandleBack) {
            evaluate("window.__tsNative && window.__tsNative.requestBack && window.__tsNative.requestBack();")
            return true
        }
        val wv = webView ?: return false
        return if (wv.canGoBack()) {
            wv.goBack()
            true
        } else {
            false
        }
    }

    /** Type a queued prompt ("Ask AI" from a past paper) into the web composer. */
    fun setComposerText(raw: String) {
        if (raw.isBlank()) return
        val quoted = JSONObject.quote(raw)
        evaluate(
            """
            (function(){
              var el = document.querySelector('textarea, input[type="text"]');
              if (!el) return;
              var proto = el.tagName === 'TEXTAREA'
                ? window.HTMLTextAreaElement.prototype
                : window.HTMLInputElement.prototype;
              var setter = Object.getOwnPropertyDescriptor(proto, 'value');
              if (setter && setter.set) { setter.set.call(el, $quoted); } else { el.value = $quoted; }
              el.dispatchEvent(new Event('input', { bubbles: true }));
              el.focus();
            })();
            """.trimIndent()
        )
    }

    // ── Handlers the screen completes ─────────────────────────────────────────

    fun deliverFileSelection(uris: Array<Uri>?) {
        val callback = pendingFileCallback
        pendingFileCallback = null
        callback?.onReceiveValue(uris)
    }

    fun deliverAudioPermission(granted: Boolean) {
        val request = pendingPermission
        pendingPermission = null
        if (request == null) return
        runCatching {
            if (granted) request.grant(request.resources) else request.deny()
        }
    }

    // ── WebView construction ──────────────────────────────────────────────────

    /**
     * Loads the chat app exactly once per WebView instance. A reload or an account
     * switch keeps whatever navigation state the page already has.
     */
    private fun ensureLoaded(wv: WebView) {
        if (initialLoadStarted) return
        initialLoadStarted = true
        runCatching { wv.loadUrl(AI_ASSISTANT_WEB_URL) }
    }

    private fun ensureWebView(): WebView {
        webView?.let { return it }
        val wv = WebView(ownerActivity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.parseColor("#121016"))
            overScrollMode = View.OVER_SCROLL_NEVER

            val cookies = CookieManager.getInstance()
            cookies.setAcceptCookie(true)
            cookies.setAcceptThirdPartyCookies(this, false)

            addJavascriptInterface(bridge, AiWebConfig.JS_BRIDGE)

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
                javaScriptCanOpenWindowsAutomatically = false
                setGeolocationEnabled(false)
                // A chat screen must not steal focus (and pop the keyboard) on load.
                setNeedInitialFocus(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                val defaultUa = userAgentString.orEmpty()
                if (!defaultUa.contains(AiWebConfig.USER_AGENT_TAG)) {
                    userAgentString = "$defaultUa ${AiWebConfig.USER_AGENT_TAG}"
                }
            }

            webChromeClient = buildChromeClient()
            webViewClient = buildWebViewClient()
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                handleDownload(url, userAgent, contentDisposition, mimeType)
            }
        }

        container.removeAllViews()
        container.addView(wv)
        webView = wv
        return wv
    }

    private fun buildChromeClient(): WebChromeClient = object : WebChromeClient() {

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
                container.addView(
                    it,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        }

        override fun onHideCustomView() {
            customView?.let { container.removeView(it) }
            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            val origin = request.origin
            if (origin == null || !origin.scheme.equals("https", true) ||
                !origin.host.equals(AiWebConfig.TRUSTED_HOST, true)
            ) {
                runCatching { request.deny() }
                return
            }
            val needsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            if (!needsAudio) {
                runCatching { request.deny() }
                return
            }
            val granted = ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                runCatching { request.grant(request.resources) }
                return
            }
            val requester = audioPermissionRequester
            if (requester == null) {
                runCatching { request.deny() }
                return
            }
            pendingPermission?.deny()
            pendingPermission = request
            requester()
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = filePathCallback
            val handler = fileChooserHandler
            if (handler == null) {
                pendingFileCallback = null
                filePathCallback?.onReceiveValue(null)
                return false
            }
            handler(fileChooserParams)
            return true
        }
    }

    private fun buildWebViewClient(): WebViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            val uri = url?.let { runCatching { Uri.parse(it) }.getOrNull() }
            if (!isTrustedWebUri(uri)) {
                bridge.isActive = false
                return
            }
            bridge.isActive = true
            bridge.pageHost = uri?.host.orEmpty()
            bridge.cachedToken = preferences.sessionToken.orEmpty().trim()
            error = null
            isContentVisible = false
            isLoading = true
            canWebHandleBack = false
            view?.let { injectNativeFeel(it) }
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            // The first frame is on screen — drop the skeleton immediately rather
            // than waiting for every last font and image to finish.
            isContentVisible = true
            isLoading = false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val uri = url?.let { runCatching { Uri.parse(it) }.getOrNull() }
            if (!isTrustedWebUri(uri)) return
            isContentVisible = true
            isLoading = false
            cacheRetryUsed = false
            view?.let {
                injectNativeFeel(it)
                postAuthHandshake(it)
            }
            runCatching { CookieManager.getInstance().flush() }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            webError: WebResourceError?
        ) {
            super.onReceivedError(view, request, webError)
            if (request?.isForMainFrame != true) return
            val scheme = request.url?.scheme.orEmpty()
            if (!scheme.equals("http", true) && !scheme.equals("https", true)) return
            failWith(mapError(webError?.errorCode ?: 0))
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            if (request?.isForMainFrame != true) return
            if ((errorResponse?.statusCode ?: 0) >= 500) failWith(AiWebError.SERVER)
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            // The renderer died (heavy page + low RAM). The instance is dead too;
            // rebuild it and show the error card instead of a black rectangle.
            if (webView === view) {
                webView = null
                isLoading = false
                isContentVisible = false
                canWebHandleBack = false
                error = AiWebError.GENERIC
                val dead = view
                mainHandler.post {
                    if (dead == null) return@post
                    runCatching { container.removeView(dead) }
                    runCatching { dead.destroy() }
                }
            }
            return true
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val target = request?.url ?: return false
            if (isTrustedWebUri(target)) return false
            launchExternal(Intent(Intent.ACTION_VIEW, target))
            return true
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun mapError(code: Int): AiWebError = when (code) {
        WebViewClient.ERROR_HOST_LOOKUP,
        WebViewClient.ERROR_UNKNOWN -> AiWebError.OFFLINE
        WebViewClient.ERROR_TIMEOUT,
        WebViewClient.ERROR_CONNECT,
        WebViewClient.ERROR_IO -> AiWebError.TIMEOUT
        else -> AiWebError.GENERIC
    }

    /**
     * Records a main-frame failure. The first failure gets one cheap fallback:
     * re-run the load from the HTTP cache, which is enough for a page the student
     * opened before. If that misses too, the error overlay takes over.
     */
    private fun failWith(reason: AiWebError) {
        isContentVisible = false
        isLoading = false
        val wv = webView
        if (!cacheRetryUsed && wv != null) {
            cacheRetryUsed = true
            runCatching {
                wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                if (wv.url.isNullOrBlank()) wv.loadUrl(AI_ASSISTANT_WEB_URL) else wv.reload()
            }
            return
        }
        error = reason
    }

    private fun injectNativeFeel(wv: WebView) {
        val css = cssAsset
        val js = jsAsset
        if (css.isEmpty() && js.isEmpty()) return
        val styleId = JSONObject.quote(AiWebConfig.STYLE_ID)
        val script = buildString {
            append("(function(){try{")
            append("var d=document;var head=d.head||d.documentElement;")
            append("if(head&&!d.getElementById(").append(styleId).append(")){")
            append("var s=d.createElement('style');s.id=").append(styleId)
            append(";s.textContent=").append(JSONObject.quote(css)).append(";head.appendChild(s);}")
            append(js)
            append("}catch(e){}})();")
        }
        runCatching { wv.evaluateJavascript(script, null) }
    }

    private fun postAuthHandshake(wv: WebView? = webView) {
        if (wv == null) return
        val uri = wv.url?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (!isTrustedWebUri(uri)) return
        val token = preferences.sessionToken.orEmpty().trim()
        if (!AiWebConfig.SESSION_TOKEN_REGEX.matches(token)) return
        val script = "window.postMessage({type:'THESTUDENT_AUTH_HANDSHAKE',token:" +
            JSONObject.quote(token) + "}," + JSONObject.quote(AiWebConfig.TRUSTED_ORIGIN) + ");"
        runCatching { wv.evaluateJavascript(script, null) }
    }

    private fun evaluate(script: String) {
        val wv = webView ?: return
        runCatching { wv.evaluateJavascript(script, null) }
    }

    private fun handleDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                launchExternal(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return
            }
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                setTitle(fileName)
                setDescription("Downloading from TheStudent AI")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(app, "Downloading $fileName…", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            runCatching { launchExternal(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    /**
     * Starts an activity from whichever context is alive. The session outlives any
     * one composition, so the activity may already be gone; in that case the app
     * context needs `FLAG_ACTIVITY_NEW_TASK` or the call throws.
     */
    private fun launchExternal(intent: Intent) {
        val activity = ownerActivity
        val started = if (activity.isFinishing || activity.isDestroyed) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { app.startActivity(intent) }.isSuccess
        } else {
            runCatching { activity.startActivity(intent) }.isSuccess
        }
        if (!started) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { app.startActivity(intent) }
        }
    }

    /**
     * Auto-recovery: when the phone gets its network back, reload instead of
     * making the student hunt for the Retry button.
     */
    private fun installNetworkWatch() {
        if (networkCallback != null) return
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post {
                    if (error != null) reload()
                }
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        connectivityManager = manager
        networkCallback = callback
    }

    private fun uninstallNetworkWatch() {
        val manager = connectivityManager
        val callback = networkCallback
        connectivityManager = null
        networkCallback = null
        if (manager != null && callback != null) {
            runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }

    private val cssAsset: String by lazy { readAsset(AiWebConfig.CSS_ASSET) }
    private val jsAsset: String by lazy { readAsset(AiWebConfig.JS_ASSET) }

    private fun readAsset(path: String): String =
        runCatching { app.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("")
}
