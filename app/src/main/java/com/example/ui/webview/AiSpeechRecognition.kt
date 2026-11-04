package com.example.ui.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import org.json.JSONObject
import java.util.Locale

/**
 * Native speech-to-text behind the Web Speech API polyfill.
 *
 * Android's System WebView does not implement `SpeechRecognition` at all —
 * `window.webkitSpeechRecognition` is `undefined` inside a WebView, so a page that
 * calls `new webkitSpeechRecognition()` throws and its "talk" mode does nothing.
 * This class backs `assets/webview/ai_speech.js`, which re-creates the browser
 * interface on top of the platform [SpeechRecognizer], so the web app's existing
 * code keeps working without a rewrite.
 *
 * Threading: [SpeechRecognizer] must be created and driven from a thread with a
 * Looper. Every public method here is safe to call from any thread and hops to
 * the main looper internally. [emit] is therefore always called on the main
 * thread, which is also where `evaluateJavascript` has to run.
 */
class AiSpeechRecognition(
    private val app: Context,
    private val emit: (String, String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null

    /** The page wants recognition running (it has not stopped/aborted). */
    private var active = false

    /** The recognizer is actually capturing right now. */
    private var listening = false

    private var continuous = false
    private var languageTag = "en-US"
    private var restartScheduled = false

    /** True when the device has a usable on-device or cloud recogniser. */
    fun isAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(app) }.getOrDefault(false)

    /**
     * Begin listening. Returns false immediately when the device cannot do
     * speech recognition at all, so the page can surface its own fallback.
     */
    fun start(languageTag: String?, continuousMode: Boolean): Boolean {
        val requested = languageTag?.trim().orEmpty()
        this.languageTag = if (requested.isEmpty()) defaultLanguageTag() else requested
        this.continuous = continuousMode
        active = true
        mainHandler.post { startInternal() }
        return isAvailable()
    }

    /** Graceful stop: the recogniser still delivers the final transcript. */
    fun stop() {
        mainHandler.post {
            active = false
            runCatching { recognizer?.stopListening() }
        }
    }

    /** Hard abort: drop whatever was heard and fire `end` straight away. */
    fun cancel() {
        mainHandler.post {
            if (!active && !listening && recognizer == null) return@post
            active = false
            listening = false
            runCatching { recognizer?.cancel() }
            emit(EVENT_END, "{}")
        }
    }

    /** Release the engine. Called when the WebView goes away. */
    fun release() {
        mainHandler.post {
            active = false
            listening = false
            restartScheduled = false
            val engine = recognizer
            recognizer = null
            runCatching { engine?.destroy() }
        }
    }

    // ── Internals (main thread only) ──────────────────────────────────────────

    private fun startInternal() {
        if (!active) return
        if (!isAvailable()) {
            emit(EVENT_ERROR, errorPayload("service-not-allowed", "Speech recognition is not available on this device"))
            emit(EVENT_END, "{}")
            active = false
            return
        }

        val engine = recognizer ?: createRecognizer().also { recognizer = it }
        if (listening) {
            listening = false
            runCatching { engine.cancel() }
        }

        listening = true
        emit(EVENT_START, "{}")
        runCatching { engine.startListening(buildIntent()) }.onFailure { failure ->
            listening = false
            active = false
            emit(EVENT_ERROR, errorPayload("audio-capture", failure.message))
            emit(EVENT_END, "{}")
        }
    }

    private fun createRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(app).apply {
            setRecognitionListener(listener)
        }

    private fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, app.packageName)
    }

    private val listener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            if (!active) return
            emit(EVENT_AUDIO_START, "{}")
        }

        override fun onBeginningOfSpeech() {
            if (!active) return
            emit(EVENT_SPEECH_START, "{}")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Deliberately ignored: forwarding ~10 events a second would mean ten
            // evaluateJavascript calls a second, which is exactly the kind of cost
            // this screen is trying to remove.
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            if (!active) return
            emit(EVENT_SPEECH_END, "{}")
        }

        override fun onError(error: Int) {
            if (!active) return
            listening = false
            val transient = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            if (continuous && transient) {
                scheduleRestart()
                return
            }
            emit(EVENT_ERROR, errorPayload(mapError(error), errorMessage(error)))
            emit(EVENT_END, "{}")
            active = false
        }

        override fun onResults(results: Bundle?) {
            if (!active) return
            deliver(results, isFinal = true)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active) return
            deliver(partialResults, isFinal = false)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun deliver(results: Bundle?, isFinal: Boolean) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val transcript = matches.firstOrNull().orEmpty()
        val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            ?.firstOrNull()
            ?: 0f

        if (transcript.isBlank()) {
            if (isFinal) {
                listening = false
                if (continuous) scheduleRestart() else {
                    emit(EVENT_NOMATCH, "{}")
                    emit(EVENT_END, "{}")
                    active = false
                }
            }
            return
        }

        emit(
            EVENT_RESULT,
            JSONObject()
                .put("transcript", transcript)
                .put("isFinal", isFinal)
                .put("confidence", confidence.toDouble())
                .toString()
        )

        if (!isFinal) return

        listening = false
        // `continuous` recognition keeps the session open until the page stops it.
        if (continuous && active) scheduleRestart() else {
            emit(EVENT_END, "{}")
            active = false
        }
    }

    private fun scheduleRestart() {
        if (restartScheduled) return
        restartScheduled = true
        mainHandler.postDelayed({
            restartScheduled = false
            if (active && !listening) startInternal()
        }, RESTART_DELAY_MS)
    }

    private fun defaultLanguageTag(): String =
        runCatching { Locale.getDefault().toLanguageTag() }.getOrDefault("en-US")

    private fun errorPayload(code: String, message: String?): String =
        JSONObject()
            .put("error", code)
            .put("message", message ?: code)
            .toString()

    /** Maps platform error codes onto the Web Speech API's `error` vocabulary. */
    private fun mapError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "audio-capture"
        SpeechRecognizer.ERROR_CLIENT -> "aborted"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "not-allowed"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> "network"
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "no-speech"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "aborted"
        else -> "service-not-allowed"
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognition was cancelled"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing"
        SpeechRecognizer.ERROR_NETWORK -> "Speech service network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service timed out"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech understood"
        SpeechRecognizer.ERROR_SERVER -> "Speech service error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The recogniser is busy"
        else -> "Speech recognition failed"
    }

    companion object {
        const val EVENT_START = "start"
        const val EVENT_AUDIO_START = "audiostart"
        const val EVENT_SPEECH_START = "speechstart"
        const val EVENT_SPEECH_END = "speechend"
        const val EVENT_RESULT = "result"
        const val EVENT_NOMATCH = "nomatch"
        const val EVENT_ERROR = "error"
        const val EVENT_END = "end"

        /** Gap between an auto-restart, long enough for the service to release the mic. */
        private const val RESTART_DELAY_MS = 220L
    }
}
