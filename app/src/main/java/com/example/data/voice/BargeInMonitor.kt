package com.example.data.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

/**
 * Notices that the student has started talking while the tutor is still talking.
 *
 * A phone call you cannot interrupt is not a phone call — it is a recording. The
 * tap-to-interrupt control exists, but the reflex is to simply speak, and until
 * now speaking over the tutor did nothing at all.
 *
 * This is a deliberately small microphone tap, not a recogniser:
 *
 *  - it runs **only while the tutor is speaking**, so the microphone is never
 *    open during a student's turn (the recogniser owns it then);
 *  - it captures from `VOICE_COMMUNICATION`, which makes Android apply echo
 *    cancellation for the audio leaving the speaker — otherwise the tutor would
 *    interrupt herself on every sentence;
 *  - a barge-in needs **sustained** sound (~400 ms above threshold), so a cough,
 *    a door, or one loud syllable does not cut her off;
 *  - it ignores the first moments of playback, while the engine is still ramping
 *    up and the echo canceller is still adapting;
 *  - and if it fires twice in a call without the student actually saying
 *    anything, the call disables it (`disableForCall`) rather than keep guessing.
 */
class BargeInMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var thread: Thread? = null
    private var recorder: AudioRecord? = null
    private var echoCanceller: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    @Volatile
    private var running = false

    @Volatile
    private var enabled = true

    /** Called on the main thread when the student has spoken over the tutor. */
    var onBargeIn: (() -> Unit)? = null

    /** False once the monitor has proved unreliable in this call. */
    val isEnabled: Boolean get() = enabled

    fun start() {
        if (!enabled || running) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        running = true
        thread = Thread({ listen() }, "tutor-barge-in").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Stop capturing and let go of the microphone.
     *
     * The release happens on a background thread on purpose: `AudioRecord.read`
     * can take a couple of frames to return, and this is called from the middle
     * of a call — the screen must never wait for a microphone to close.
     */
    fun stop() {
        if (!running && thread == null && recorder == null) return
        running = false
        val worker = thread
        thread = null
        val record = recorder
        recorder = null
        val canceller = echoCanceller
        echoCanceller = null
        val suppressor = noiseSuppressor
        noiseSuppressor = null
        Thread {
            if (worker != null) {
                try {
                    worker.join(600)
                } catch (_: InterruptedException) {
                }
            }
            try {
                record?.stop()
            } catch (_: Exception) {
            }
            try {
                record?.release()
            } catch (_: Exception) {
            }
            try {
                canceller?.release()
            } catch (_: Exception) {
            }
            try {
                suppressor?.release()
            } catch (_: Exception) {
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    /** This device (or this room) is too echoey to trust: stop trying for this call. */
    fun disableForCall() {
        enabled = false
        stop()
    }

    /** A new call gets a fresh chance. */
    fun resetForCall() {
        enabled = true
    }

    @SuppressLint("MissingPermission")
    private fun listen() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) {
            running = false
            return
        }
        // One read = about 100 ms, which is the resolution the "sustained" test
        // needs; the device minimum buffer can be much smaller than that.
        val bufferBytes = maxOf(minBuffer, SAMPLE_RATE * 2 / 10)
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, CHANNEL, ENCODING, bufferBytes)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open the barge-in microphone: ${e.message}")
            null
        }
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            try {
                record?.release()
            } catch (_: Exception) {
            }
            running = false
            return
        }
        recorder = record
        try {
            echoCanceller = AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
        } catch (_: Exception) {
        }
        try {
            noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
        } catch (_: Exception) {
        }
        val started = try {
            record.startRecording()
            record.recordingState == AudioRecord.RECORDSTATE_RECORDING
        } catch (e: Exception) {
            false
        }
        if (!started) {
            running = false
            return
        }

        val samples = ShortArray(bufferBytes / 2)
        val startAt = System.currentTimeMillis()
        var loudFrames = 0
        while (running) {
            val read = try {
                record.read(samples, 0, samples.size)
            } catch (e: Exception) {
                -1
            }
            if (read <= 0) continue
            var energy = 0.0
            for (i in 0 until read) {
                val value = samples[i] / 32768.0
                energy += value * value
            }
            val rms = sqrt(energy / read)
            val inGuard = System.currentTimeMillis() - startAt < GUARD_MS
            if (inGuard || rms < THRESHOLD) {
                loudFrames = 0
            } else {
                loudFrames++
            }
            if (loudFrames >= SUSTAINED_FRAMES) {
                loudFrames = 0
                running = false
                mainHandler.post { onBargeIn?.invoke() }
                break
            }
        }
    }

    companion object {
        private const val TAG = "BargeInMonitor"
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Level (0..1 RMS) that counts as someone speaking into the phone. */
        private const val THRESHOLD = 0.055
        /** 100 ms frames above the threshold needed to interrupt. */
        private const val SUSTAINED_FRAMES = 4
        /** Ignore the start of playback: engine ramp-up and echo-canceller settling. */
        private const val GUARD_MS = 600L
    }
}
