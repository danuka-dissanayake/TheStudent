package com.example.data.local

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Records a chat voice note into the app cache.
 *
 * AAC in an MPEG-4 container at a low bitrate: a one-minute note is roughly
 * 240 KB, so it travels and stores cheaply. The file is encrypted like any other
 * attachment before it leaves the phone — the relay never sees audio.
 */
class ChatVoiceRecorder(context: Context) {

    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Milliseconds since recording started (0 when idle). */
    fun elapsedMs(): Long = if (startedAt == 0L) 0L else System.currentTimeMillis() - startedAt

    fun start(): Boolean {
        stopInternal(discard = true)

        val file = File(appContext.cacheDir, "chat_voice_" + System.currentTimeMillis() + ".m4a")
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            created.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(96_000)
                setOutputFile(file.absolutePath)
            }
            created.prepare()
            created.start()
            recorder = created
            outputFile = file
            startedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Voice note recording failed: ${e.message}")
            runCatching { created.release() }
            file.delete()
            recorder = null
            outputFile = null
            startedAt = 0L
            false
        }
    }

    /**
     * Finish and return the recording, or null when it was too short to be
     * useful (under a second, or nothing was captured).
     */
    fun stop(): ChatAttachmentDraft? {
        val file = outputFile
        val duration = elapsedMs()
        val stopped = stopInternal(discard = false)
        if (!stopped || file == null || !file.exists()) {
            file?.delete()
            return null
        }
        if (duration < MIN_DURATION_MS || file.length() < 1024L) {
            file.delete()
            return null
        }
        return ChatAttachmentDraft(
            path = file.absolutePath,
            name = "Voice note",
            mime = "audio/mp4",
            size = file.length(),
            kind = com.example.data.model.ChatAttachment.AUDIO,
            durationMs = duration
        )
    }

    /** Abandon the recording in progress and delete whatever was captured. */
    fun cancel() {
        val file = outputFile
        stopInternal(discard = true)
        file?.delete()
    }

    private fun stopInternal(discard: Boolean): Boolean {
        val active = recorder ?: return false
        recorder = null
        startedAt = 0L
        return try {
            if (!discard) active.stop()
            active.release()
            !discard
        } catch (e: Exception) {
            // stop() throws when nothing was captured; treat it as a failure.
            Log.w(TAG, "Voice note stop failed: ${e.message}")
            runCatching { active.release() }
            false
        }
    }

    private companion object {
        const val TAG = "ChatVoiceRecorder"
        const val MIN_DURATION_MS = 1_000L
    }
}
