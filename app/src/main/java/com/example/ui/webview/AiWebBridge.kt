package com.example.ui.webview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import com.example.BuildConfig
import org.json.JSONObject
import java.util.Locale

/**
 * The object exposed to the web app as `window.TheStudentAndroid`.
 *
 * Behaviour notes that matter for correctness:
 *
 * - Every method runs on the WebView's private `JavaBridge` thread, never on the
 *   UI thread, so nothing here may touch a `View` directly. The callbacks handed
 *   in by [AiWebSession] do the thread hopping.
 * - Methods are only allowed to do anything while the **main frame** is the
 *   trusted https origin ([pageHost]). [isActive] is cleared the moment the
 *   WebView leaves that origin.
 * - No method takes a defaulted parameter: the JavaScript bridge resolves
 *   methods reflectively by name and arity, so `fun haptic(level: String = "tap")`
 *   would be visible to the page as `haptic(String)` only — calls would throw.
 *   Every JS-facing method is therefore explicit and nullable.
 */
class AiWebBridge internal constructor(
    private val app: Context,
    private val tokenProvider: () -> String?,
    private val themeProvider: () -> JSONObject,
    private val hapticHandler: () -> Unit,
    private val reportRoute: (Boolean) -> Unit,
    private val reportReady: () -> Unit,
    private val launchIntent: (Intent) -> Unit,
    private val speechAvailable: () -> Boolean,
    private val startSpeech: (String?, Boolean) -> Boolean,
    private val stopSpeech: () -> Unit,
    private val cancelSpeech: () -> Unit,
    private val speakHandler: (String?, String?) -> Unit,
    private val stopSpeakingHandler: () -> Unit,
    private val audioPermissionCheck: () -> Boolean,
    private val audioPermissionRequest: () -> Unit
) {

    /** Host of the main frame, kept current by the session's `WebViewClient`. */
    @Volatile
    var pageHost: String = AiWebConfig.TRUSTED_HOST

    /**
     * Last known session token. Cached so `getSessionToken()` can answer
     * synchronously — the web app calls it during boot and cannot await.
     */
    @Volatile
    var cachedToken: String = ""

    /** False while the WebView is navigating to or sitting on a non-trusted origin. */
    @Volatile
    var isActive: Boolean = true

    private fun isTrustedPage(): Boolean =
        isActive && pageHost.equals(AiWebConfig.TRUSTED_HOST, ignoreCase = true)

    // ── Identification ────────────────────────────────────────────────────────

    @JavascriptInterface
    fun isAndroidApp(): Boolean = true

    @JavascriptInterface
    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    @JavascriptInterface
    fun isDebugBuild(): Boolean = BuildConfig.DEBUG

    /**
     * The signed-in student's session token, or "" when this page is not allowed
     * to see it. Synchronous by design: the web app reads it while the first
     * render is being built.
     */
    @JavascriptInterface
    fun getSessionToken(): String {
        if (!isTrustedPage()) return ""
        val fresh = tokenProvider().orEmpty().trim()
        if (fresh.isNotEmpty()) cachedToken = fresh
        val token = cachedToken.trim()
        return if (AiWebConfig.SESSION_TOKEN_REGEX.matches(token)) token else ""
    }

    // ── Native feel ───────────────────────────────────────────────────────────

    /** Light tap feedback for buttons, tabs and list rows. */
    @JavascriptInterface
    fun haptic() {
        if (!isTrustedPage()) return
        hapticHandler()
    }

    /**
     * Accent colour, colour scheme and font scale as JSON, so the page can adopt
     * the app's Material theme instead of looking like a website in a box.
     * Example: `{"accent":"#10B981","mode":"dark","fontScale":1.0}`
     */
    @JavascriptInterface
    fun getTheme(): String = runCatching { themeProvider().toString() }.getOrDefault("{}")

    /** The page painted its first real frame; the skeleton can go away now. */
    @JavascriptInterface
    fun notifyReady() {
        if (!isTrustedPage()) return
        reportReady()
    }

    /**
     * Tells the app whether the page can consume a system back press itself
     * (sidebar open, modal open). `state` is `"open"` or `"idle"`.
     */
    @JavascriptInterface
    fun notifyRoute(state: String?) {
        if (!isTrustedPage()) return
        reportRoute(state == "open")
    }

    // ── Hand-offs to the OS ───────────────────────────────────────────────────

    /** Opens http(s) links outside the chat (terms, sources, university pages). */
    @JavascriptInterface
    fun openExternal(url: String?) {
        if (!isTrustedPage()) return
        val uri = runCatching { Uri.parse(url.orEmpty()) }.getOrNull() ?: return
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return
        if (scheme != "http" && scheme != "https") return
        launchIntent(Intent(Intent.ACTION_VIEW, uri))
    }

    /** Shares an answer or a study note through the system share sheet. */
    @JavascriptInterface
    fun share(text: String?, subject: String?) {
        if (!isTrustedPage()) return
        val body = text.orEmpty()
        if (body.isBlank()) return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
            val title = subject.orEmpty()
            if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
        }
        launchIntent(Intent.createChooser(send, "Share"))
    }

    /** Copies text with a real `ClipData` label instead of the browser fallback. */
    @JavascriptInterface
    fun copyText(text: String?) {
        if (!isTrustedPage()) return
        val value = text.orEmpty()
        if (value.isEmpty()) return
        runCatching {
            val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("TheStudent AI", value))
        }
    }

    // ── Speech ────────────────────────────────────────────────────────────────
    //
    // Android's System WebView has no Web Speech API, so `assets/webview/ai_speech.js`
    // re-creates `SpeechRecognition` and `speechSynthesis` on top of these calls.

    /** True when the device has a usable on-device or cloud recogniser. */
    @JavascriptInterface
    fun isSpeechRecognitionAvailable(): Boolean =
        if (isTrustedPage()) speechAvailable() else false

    /**
     * Starts listening. Returns false when this device cannot do speech
     * recognition at all (no RecognitionService), so the page can fall back to
     * typing instead of waiting forever for an `onresult`.
     */
    @JavascriptInterface
    fun startSpeechRecognition(lang: String?, continuous: Boolean): Boolean =
        if (isTrustedPage()) startSpeech(lang, continuous) else false

    /** Ends the session gracefully — the final transcript still arrives. */
    @JavascriptInterface
    fun stopSpeechRecognition() {
        if (!isTrustedPage()) return
        stopSpeech()
    }

    /** Drops whatever was heard and ends immediately. */
    @JavascriptInterface
    fun cancelSpeechRecognition() {
        if (!isTrustedPage()) return
        cancelSpeech()
    }

    /** Text-to-speech for the assistant's spoken replies. */
    @JavascriptInterface
    fun speakText(text: String?, lang: String?) {
        if (!isTrustedPage()) return
        speakHandler(text, lang)
    }

    @JavascriptInterface
    fun stopSpeaking() {
        if (!isTrustedPage()) return
        stopSpeakingHandler()
    }

    // ── Microphone permission ─────────────────────────────────────────────────
    //
    // `getUserMedia` / MediaRecorder (the voice-message recorder) needs the runtime
    // RECORD_AUDIO grant. Exposing the check and the prompt lets the page ask at
    // the right moment — a tap on the mic — instead of failing silently.

    @JavascriptInterface
    fun hasAudioPermission(): Boolean = audioPermissionCheck()

    @JavascriptInterface
    fun requestAudioPermission() {
        if (!isTrustedPage()) return
        audioPermissionRequest()
    }

    /** Debug-only console relay, so page errors show up in Logcat. */
    @JavascriptInterface
    fun log(message: String?) {
        if (!BuildConfig.DEBUG) return
        Log.d("AiWeb", message.orEmpty())
    }
}
