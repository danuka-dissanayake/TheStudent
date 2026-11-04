package com.example.ui.webview

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONObject
import java.io.File

/**
 * Native voice-message recorder for the AI Assistant screen.
 *
 * The page's own recorder is `getUserMedia` + `MediaRecorder`, which is the right
 * thing to use when the WebView supports it. This is the guaranteed path: a real
 * `MediaRecorder` on the app side, exposed through the bridge as
 * `startAudioRecording()` / `stopAudioRecording()` and finished off by
 * `window.TheStudentVoiceRecorder` in `assets/webview/ai_speech.js`, which hands
 * the page a normal `Blob` it can upload.
 *
 * Output is AAC in an MP4 container (`.m4a`) — universally playable, half the size
 * of 16-bit WAV for the same quality, and it needs no transcoding on the way out.
 *
 * Threading: everything runs on the main looper; [emit] is therefore main-threaded,
 * which is where `evaluateJavascript` has to be.
 */
class AiAudioRecorder(
    private val app: Context,
    private val emit: (String, String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    /** Every API this app supports has MediaRecorder; kept for symmetry with speech. */
    fun isAvailable(): Boolean = true

    fun start() = mainHandler.post { startInternal() }

    fun stop() = mainHandler.post { stopInternal() }

    fun cancel() = mainHandler.post { cancelInternal() }

    fun release() = mainHandler.post { cancelInternal() }

    /** The student declined the microphone: surface it as a recording error. */
    fun reportDenied() {
        emit(
            EVENT_ERROR,
            JSONObject()
                .put("error", "not-allowed")
                .put("message", "Microphone permission was denied")
                .toString()
        )
        emit(EVENT_END, JSONObject().put("aborted", true).toString())
    }

    // ── Internals (main thread) ───────────────────────────────────────────────

    private fun startInternal() {
        if (recorder != null) return

        val directory = File(app.cacheDir, "ai_voice").apply { mkdirs() }
        val file = File(directory, "voice_" + System.currentTimeMillis() + ".m4a")

        @Suppress("DEPRECATION")
        val engine = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(app)
        } else {
            MediaRecorder()
        }

        val started = runCatching {
            engine.setAudioSource(MediaRecorder.AudioSource.MIC)
            engine.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            engine.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            engine.setAudioEncodingBitRate(BIT_RATE)
            engine.setAudioSamplingRate(SAMPLE_RATE)
            engine.setOutputFile(file.absolutePath)
            engine.prepare()
            engine.start()
        }.isSuccess

        if (!started) {
            runCatching { engine.release() }
            file.delete()
            emit(
                EVENT_ERROR,
                JSONObject()
                    .put("error", "audio-capture")
                    .put("message", "The recorder could not start")
                    .toString()
            )
            emit(EVENT_END, JSONObject().put("aborted", true).toString())
            return
        }

        recorder = engine
        outputFile = file
        startedAt = System.currentTimeMillis()
        emit(EVENT_START, "{}")
    }

    private fun stopInternal() {
        val engine = recorder ?: return
        recorder = null

        val file = outputFile
        outputFile = null
        val durationMs = System.currentTimeMillis() - startedAt

        val stopped = runCatching { engine.stop() }.isSuccess
        runCatching { engine.release() }

        if (!stopped || file == null || !file.exists()) {
            file?.delete()
            emit(
                EVENT_ERROR,
                JSONObject()
                    .put("error", "audio-capture")
                    .put("message", "The recording was too short to save")
                    .toString()
            )
            emit(EVENT_END, JSONObject().put("aborted", true).toString())
            return
        }

        val bytes = runCatching { file.readBytes() }.getOrDefault(ByteArray(0))
        file.delete()

        if (bytes.isEmpty()) {
            emit(
                EVENT_ERROR,
                JSONObject().put("error", "audio-capture").put("message", "Empty recording").toString()
            )
            emit(EVENT_END, JSONObject().put("aborted", true).toString())
            return
        }

        // Base64 in chunks: one multi-megabyte evaluateJavascript call is a stall,
        // and every chunk boundary is 4-aligned so each one decodes on its own.
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        var index = 0
        var offset = 0
        while (offset < encoded.length) {
            val end = minOf(offset + CHUNK_CHARS, encoded.length)
            emit(
                EVENT_CHUNK,
                JSONObject()
                    .put("index", index)
                    .put("data", encoded.substring(offset, end))
                    .toString()
            )
            offset = end
            index++
        }

        emit(
            EVENT_END,
            JSONObject()
                .put("chunks", index)
                .put("mime", MIME_TYPE)
                .put("durationMs", durationMs)
                .put("size", bytes.size)
                .toString()
        )
    }

    private fun cancelInternal() {
        val engine = recorder
        recorder = null
        if (engine != null) {
            runCatching { engine.stop() }
            runCatching { engine.release() }
        }
        outputFile?.delete()
        outputFile = null
        startedAt = 0L
    }

    companion object {
        const val EVENT_START = "recordstart"
        const val EVENT_CHUNK = "recordchunk"
        const val EVENT_END = "recordend"
        const val EVENT_ERROR = "recorderror"

        const val MIME_TYPE = "audio/mp4"

        private const val BIT_RATE = 64_000
        private const val SAMPLE_RATE = 44_100

        /** 240_000 base64 characters ≈ 180 KB of audio per bridge call. */
        private const val CHUNK_CHARS = 240_000
    }
}
