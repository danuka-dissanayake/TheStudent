package com.example.ui.webview

import android.net.Uri

/**
 * Production URL of the TheStudent AI Assistant web app.
 *
 * Kept in this package (rather than in the screen) so the pre-warmer, the session
 * holder and the tests can all point at the same origin-lock rules.
 */
const val AI_ASSISTANT_WEB_URL = "https://chatbot-liart-rho-81.vercel.app/"

/**
 * Every constant the AI Assistant `WebView` needs, in one place.
 *
 * The origin locks here are security boundaries, not convenience values: the
 * session token is only ever handed to pages whose main frame matches
 * [TRUSTED_HOST] over https.
 */
object AiWebConfig {

    /** Host the session token may be handed to. Exact match — no subdomains. */
    const val TRUSTED_HOST = "chatbot-liart-rho-81.vercel.app"

    /** Origin used as the `targetOrigin` of the postMessage auth handshake. */
    const val TRUSTED_ORIGIN = "https://chatbot-liart-rho-81.vercel.app"

    /** Name of the synchronous JavaScript bridge object on `window`. */
    const val JS_BRIDGE = "TheStudentAndroid"

    /** `id` of the injected `<style>` element — makes re-injection a no-op. */
    const val STYLE_ID = "ts-native-feel"

    /** Assets injected into every trusted page load. */
    const val CSS_ASSET = "webview/ai_native_feel.css"
    const val JS_ASSET = "webview/ai_native_feel.js"

    /**
     * The Web Speech API polyfill. Android's System WebView has neither
     * `SpeechRecognition` nor `speechSynthesis`, so the page's "talk" mode is
     * rebuilt on top of the bridge in this file.
     */
    const val JS_SPEECH_ASSET = "webview/ai_speech.js"

    /**
     * How long a detached session is kept warm. Long enough that leaving the
     * screen to open a past paper and coming back is instant, short enough that
     * a `WebView` renderer never sits in RAM for a whole study session.
     */
    const val KEEP_ALIVE_MS = 15 * 60 * 1000L

    /** Suffix appended to the stock UA so the web app can feature-detect the shell. */
    const val USER_AGENT_TAG = "TheStudentAndroid/1.1"

    /** Session tokens are 32–256 hex characters. Anything else is rejected. */
    val SESSION_TOKEN_REGEX = Regex("^[A-Fa-f0-9]{32,256}$")
}

/** Which failure the offline/error overlay should describe. */
enum class AiWebError {
    /** No usable radio / DNS failure — "check your connection". */
    OFFLINE,

    /** The request went out but nothing came back in time. */
    TIMEOUT,

    /** The server answered with a 5xx. Not the student's fault. */
    SERVER,

    /** Renderer crash, bad URL, or anything else. */
    GENERIC
}

/** True when [uri] is https on the trusted AI Assistant host. */
fun isTrustedWebUri(uri: Uri?): Boolean {
    if (uri == null) return false
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    val host = uri.host ?: return false
    return host.equals(AiWebConfig.TRUSTED_HOST, ignoreCase = true)
}
