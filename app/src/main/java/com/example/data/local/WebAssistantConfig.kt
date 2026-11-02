package com.example.data.local

import java.net.URI
import java.net.URLEncoder

/**
 * Everything the in-app WebView needs to know about the AI assistant website.
 *
 * This object is deliberately pure Kotlin (no Android classes) so the URL rules
 * can be checked on the JVM. The screen never free-types a URL: every navigation
 * goes through [isAllowedUrl], and every page we open goes through [pageUrl].
 */
object WebAssistantConfig {

    /**
     * Point this at a STABLE production domain before shipping.
     *
     * The value below is a Vercel *deployment* URL (`project-{hash}-{scope}`).
     * Vercel deployment URLs are immutable snapshots: they keep working, but they
     * never show a newer build. A production domain (`project.vercel.app`) or a
     * custom domain follows the latest deploy. It can also be changed at runtime
     * without an app update with `TheStudentViewModel.setWebAssistantBaseUrl()`.
     *
     * SSO: this host MUST equal `ASSISTANT_WEB_ORIGIN` in
     * `backend/config.secrets.php`. The app refuses to POST a live one-time code
     * to any other host (see `isAllowedExchangeUrl` and docs/ASSISTANT_SSO.md).
     */
    const val DEFAULT_BASE_URL = "https://chatbot-liart-rho-81.vercel.app/"

    const val APP_MODE = "android"

    /** Only HTTPS URLs are accepted; anything else falls back to the default. */
    fun normaliseBaseUrl(raw: String?): String {
        val candidate = raw?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
        if (!candidate.startsWith("https://", ignoreCase = true)) return DEFAULT_BASE_URL
        val parsed = runCatching { URI(candidate) }.getOrNull() ?: return DEFAULT_BASE_URL
        val host = parsed.host ?: return DEFAULT_BASE_URL
        val port = if (parsed.port > 0) ":${parsed.port}" else ""
        val path = (parsed.path ?: "").trimEnd('/')
        return if (path.isBlank()) "https://$host$port/" else "https://$host$port$path/"
    }

    fun hostOf(baseUrl: String): String? =
        runCatching { URI(normaliseBaseUrl(baseUrl)).host }.getOrNull()

    /**
     * Exact origin (scheme + host + port, no path, no trailing slash) used for
     * the `addWebMessageListener` allowlist. Never use `"*"` here: the listener
     * would then be injected into every site the WebView ever visits.
     */
    fun originOf(baseUrl: String): String {
        val normalised = normaliseBaseUrl(baseUrl)
        val parsed = runCatching { URI(normalised) }.getOrNull() ?: return "https://${hostOf(baseUrl).orEmpty()}"
        val host = parsed.host ?: return "https://${hostOf(baseUrl).orEmpty()}"
        val port = if (parsed.port > 0) ":${parsed.port}" else ""
        return "https://$host$port"
    }

    /**
     * The backend returns the website's exchange URL, but the app must not POST
     * a live one-time code to an arbitrary host. Require HTTPS and the SAME host
     * as the page we deliberately chose to embed.
     */
    fun isAllowedExchangeUrl(exchangeUrl: String?, baseUrl: String): Boolean =
        isAllowedUrl(exchangeUrl, baseUrl)

    /**
     * Same host + HTTPS only. A link to any other site is consumed by the app
     * instead of opening Chrome — that is what keeps the experience "in app".
     */
    fun isAllowedUrl(target: String?, baseUrl: String): Boolean {
        val value = target?.trim()?.takeIf { it.isNotBlank() } ?: return false
        val allowed = hostOf(baseUrl) ?: return false
        val parsed = runCatching { URI(value) }.getOrNull() ?: return false
        return parsed.scheme.equals("https", ignoreCase = true) &&
            parsed.host.equals(allowed, ignoreCase = true)
    }

    /**
     * Build the page the app opens. `app=android` asks the website for its
     * embedded layout (see docs/WEBVIEW_ASSISTANT.md); `theme` keeps it in step
     * with the app; `q` carries an optional first question from "Ask AI".
     */
    fun pageUrl(baseUrl: String, darkTheme: Boolean, initialPrompt: String? = null): String {
        val base = normaliseBaseUrl(baseUrl)
        val params = mutableListOf(
            "app=$APP_MODE",
            "theme=" + if (darkTheme) "dark" else "light"
        )
        if (!initialPrompt.isNullOrBlank()) {
            params += "q=" + encode(initialPrompt.trim())
        }
        return base + "?" + params.joinToString("&")
    }

    /**
     * GUEST-MODE FALLBACK ONLY. Signed-in students are authenticated by the
     * website's HttpOnly cookie (see docs/ASSISTANT_SSO.md); injecting a JS
     * identity there could trigger the website's "account changed" sign-out.
     *
     * Two delivery channels are used on purpose: a global the site can read at
     * any point during hydration, and a `postMessage` for a listener. No secret
     * is ever put in the URL; only a display name and the opaque `users.id`.
     * This value is a display hint — the website must never use it to grant
     * access.
     */
    fun identityScript(name: String?, userId: String?, darkTheme: Boolean): String {
        val literal = buildString {
            append("{type:'THESTUDENT_USER',source:'android',")
            append("name:").append(jsonString(name.orEmpty())).append(',')
            append("userId:").append(jsonString(userId.orEmpty())).append(',')
            append("theme:").append(jsonString(if (darkTheme) "dark" else "light"))
            append('}')
        }
        return "(function(){try{" +
            "var u=$literal;" +
            "window.__THESTUDENT_USER__=u;" +
            "window.postMessage(u,window.location.origin);" +
            "}catch(e){}})();"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** JSON string literal with the characters that could break out of a script. */
    private fun jsonString(value: String): String {
        val out = StringBuilder("\"")
        for (ch in value) {
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '<' -> out.append("\\u003c")
                '>' -> out.append("\\u003e")
                '&' -> out.append("\\u0026")
                else -> if (ch.code < 0x20) {
                    out.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(ch)
                }
            }
        }
        return out.append('"').toString()
    }
}
