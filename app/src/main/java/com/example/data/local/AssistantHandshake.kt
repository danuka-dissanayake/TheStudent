package com.example.data.local

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pure helpers for the assistant single-sign-on handshake.
 *
 * This is an OAuth-style one-time code exchange: the app never hands its
 * session token to the WebView. It generates a PKCE verifier, gets a short-lived
 * single-use code from the PHP backend, and POSTs that code (with the verifier)
 * to the website. The website exchanges it server-to-server and sets its own
 * HttpOnly session cookie.
 *
 * No Android classes here: the crypto and encoding are unit tested on the JVM.
 */
object AssistantHandshake {

    /** Name of the origin-pinned JS object injected by addWebMessageListener. */
    const val BRIDGE_OBJECT = "TheStudentBridge"

    /**
     * The ONLY messages web content may send to the app. Nothing here may read,
     * write or request credentials. Unknown types are ignored.
     */
    /** Website -> app: the site sends its chat list for the native history drawer. */
    const val EVENT_CHAT_HISTORY = "CHAT_HISTORY"

    /** App -> website: ask for the chat list / open one conversation. */
    const val EVENT_REQUEST_HISTORY = "REQUEST_HISTORY"
    const val EVENT_OPEN_CHAT = "OPEN_CHAT"

    val ALLOWED_EVENTS = setOf(
        "SESSION_READY",
        "SESSION_EXPIRED",
        "NEW_CHAT",
        "CLOSE",
        "OPEN_PAPERS",
        "SHARE_TEXT",
        EVENT_CHAT_HISTORY
    )

    const val MAX_BRIDGE_MESSAGE = 4_096

    private const val VERIFIER_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private const val BASE64_URL =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val random = SecureRandom()

    /** 48 characters, inside PKCE's 43–128 character unreserved range. */
    fun newVerifier(): String = randomString(48)

    /** High-entropy CSRF value bound to this one exchange. */
    fun newState(): String = randomString(32)

    /** S256 challenge: base64url(SHA-256(ASCII(verifier))) without padding. */
    fun challengeFor(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    /**
     * Form-encoded POST body. The code travels in a body, never in a URL where
     * it could leak through history, Referer, logs or screenshots.
     */
    fun formBody(
        code: String,
        verifier: String,
        state: String,
        darkTheme: Boolean
    ): ByteArray {
        val params = listOf(
            "code" to code,
            "code_verifier" to verifier,
            "state" to state,
            "app" to WebAssistantConfig.APP_MODE,
            "theme" to if (darkTheme) "dark" else "light"
        )
        return params.joinToString("&") { (key, value) ->
            encode(key) + "=" + encode(value)
        }.toByteArray(Charsets.UTF_8)
    }

    /** True only for a main-frame, allow-listed event. Payload is parsed by the caller. */
    fun isAllowedEvent(type: String?): Boolean = type != null && type in ALLOWED_EVENTS

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun randomString(length: Int): String {
        val out = CharArray(length)
        for (i in 0 until length) {
            out[i] = VERIFIER_ALPHABET[random.nextInt(VERIFIER_ALPHABET.length)]
        }
        return String(out)
    }

    /** Manual base64url so this compiles and behaves the same on API 24. */
    private fun base64Url(bytes: ByteArray): String {
        val out = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xff
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xff else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xff else -1
            val n = (b0 shl 16) or (maxOf(b1, 0) shl 8) or maxOf(b2, 0)
            out.append(BASE64_URL[(n shr 18) and 63])
            out.append(BASE64_URL[(n shr 12) and 63])
            if (b1 >= 0) out.append(BASE64_URL[(n shr 6) and 63])
            if (b2 >= 0) out.append(BASE64_URL[n and 63])
            i += 3
        }
        return out.toString()
    }
}
