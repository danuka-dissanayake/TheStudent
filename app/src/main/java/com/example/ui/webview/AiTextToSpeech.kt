package com.example.ui.webview

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Native text-to-speech behind the `speechSynthesis` polyfill.
 *
 * Android's System WebView ships no speech synthesis engine, so a page that calls
 * `speechSynthesis.speak(...)` in "talk" mode either throws or silently does
 * nothing. This backs the small `speechSynthesis` shim in
 * `assets/webview/ai_speech.js` with the platform [TextToSpeech] engine.
 *
 * Threading: every public method posts to the main looper. The engine's progress
 * callbacks arrive on a binder thread, so [emit] always hops back to main first —
 * `evaluateJavascript` must run there.
 */
class AiTextToSpeech(
    private val app: Context,
    private val emit: (String, String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var engine: TextToSpeech? = null
    private var initialising = false
    private var ready = false

    private var pendingText: String? = null
    private var pendingLanguage: String? = null

    /** Speaks [text] (max [MAX_CHARS], the engine's practical limit). */
    fun speak(text: String?, languageTag: String?) {
        val body = text?.trim().orEmpty()
        if (body.isEmpty()) return
        val clipped = if (body.length > MAX_CHARS) body.substring(0, MAX_CHARS) else body
        mainHandler.post { speakInternal(clipped, languageTag) }
    }

    fun stop() {
        mainHandler.post {
            pendingText = null
            runCatching { engine?.stop() }
            emit(EVENT_END, "{}")
        }
    }

    fun release() {
        mainHandler.post {
            pendingText = null
            val active = engine
            engine = null
            ready = false
            initialising = false
            runCatching { active?.stop() }
            runCatching { active?.shutdown() }
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun speakInternal(text: String, languageTag: String?) {
        val current = ensureEngine()
        if (current == null) {
            // Still initialising: keep the last request and speak it when ready.
            pendingText = text
            pendingLanguage = languageTag
            return
        }
        if (!ready) {
            pendingText = text
            pendingLanguage = languageTag
            return
        }

        val locale = languageTag?.takeIf { it.isNotBlank() }?.let { Locale.forLanguageTag(it) }
        if (locale != null) runCatching { current.setLanguage(locale) }

        val queued = runCatching {
            current.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), UTTERANCE_ID)
        }.getOrDefault(TextToSpeech.ERROR)

        if (queued == TextToSpeech.ERROR) {
            emit(EVENT_ERROR, "{}")
        } else {
            emit(EVENT_START, "{}")
        }
    }

    private fun ensureEngine(): TextToSpeech? {
        engine?.let { return it }
        if (initialising) return null
        initialising = true

        val created = TextToSpeech(app) { status ->
            ready = status == TextToSpeech.SUCCESS
            initialising = false
            emit(if (ready) EVENT_READY else EVENT_ERROR, "{}")
            val queued = pendingText
            val language = pendingLanguage
            pendingText = null
            pendingLanguage = null
            if (ready && queued != null) speakInternal(queued, language)
        }

        created.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                emit(EVENT_START, "{}")
            }

            override fun onDone(utteranceId: String?) {
                emit(EVENT_END, "{}")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                emit(EVENT_ERROR, "{}")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                emit(EVENT_END, "{}")
            }
        })

        engine = created
        return created
    }

    companion object {
        const val EVENT_READY = "ttsready"
        const val EVENT_START = "ttsstart"
        const val EVENT_END = "ttsend"
        const val EVENT_ERROR = "ttserror"

        /** The engine truncates far beyond this; clip before it complains. */
        private const val MAX_CHARS = 3500
        private const val UTTERANCE_ID = "thestudent-ai"
    }
}
