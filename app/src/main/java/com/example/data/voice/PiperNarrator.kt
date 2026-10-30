package com.example.data.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Miss Emma's real voice: an on-device Piper (VITS) voice, run by sherpa-onnx.
 *
 * The phone's own text-to-speech engine is fine as a fallback but it is the
 * single biggest reason a "call with a tutor" sounds like a phone menu. A Piper
 * voice is a small neural network — 20 MB for the int8 build of `en_US-amy` —
 * that runs locally on the CPU, so the voice is natural, unlimited, private and
 * works with no network at all.
 *
 * This class owns three things and nothing else:
 *
 *  1. **The voice pack.** Downloaded once from sherpa-onnx's model release,
 *     unpacked into app storage and never touched again. Nothing is bundled in
 *     the APK: 20 MB of model does not belong in Git.
 *  2. **The engine.** sherpa-onnx loads the ONNX graph plus the espeak-ng data
 *     that turns English into phonemes.
 *  3. **Playback.** Sentences are streamed as float PCM straight into an
 *     `AudioTrack` at the voice's own 22,050 Hz, with the *next* sentence
 *     synthesised on a second thread while the current one is still speaking, so
 *     there is no hole between them. Callbacks always arrive on the main thread.
 *
 * Everything here degrades: if the download fails, the model is corrupt or the
 * native library is missing, [isReady] simply stays false and the caller keeps
 * using Android's own voice.
 */
class PiperNarrator(private val context: Context) {

    private val rootDir = File(context.filesDir, "piper")
    private val cacheDir = File(context.cacheDir, "piper").apply { mkdirs() }
    private val main = Handler(Looper.getMainLooper())
    /** One thread per job: synthesis must be able to run ahead of playback. */
    private val synth = Executors.newSingleThreadExecutor()
    private val play = Executors.newSingleThreadExecutor()

    /** Bumped by [stop]; a worker whose token no longer matches walks away. */
    private val token = AtomicInteger(0)

    private var engine: OfflineTts? = null

    /** The track currently pushing samples, so [stop] can silence it at once. */
    @Volatile
    private var activeTrack: AudioTrack? = null

    /** The engine is loaded and can speak right now. */
    fun isReady(): Boolean = engine != null

    /** The voice pack is already on disk, so no download is needed. */
    fun isDownloaded(): Boolean =
        findTokens() != null && findModel() != null && findEspeakData() != null

    /**
     * Download (once), unpack and load the voice.
     *
     * Blocking — call it from a background coroutine. Returns true when the
     * engine is usable. [onProgress] reports 0..1 during the download and -1
     * while unpacking, so the caller can show something honest.
     */
    fun prepare(onProgress: ((Float) -> Unit)? = null): Boolean {
        if (engine != null) return true
        return try {
            if (!isDownloaded()) {
                val archive = File(cacheDir, "$VOICE_FOLDER.tar.bz2")
                download(URL(PACK_URL), archive, onProgress)
                onProgress?.invoke(-1f)
                unpack(archive, rootDir)
                archive.delete()
            }
            if (!isDownloaded()) {
                Log.w(TAG, "Voice pack unpacked but incomplete")
                return false
            }
            engine = buildEngine()
            engine != null
        } catch (t: Throwable) {
            Log.w(TAG, "Piper voice unavailable", t)
            false
        }
    }

    /**
     * Speak a list of sentences, streamed.
     *
     * The old implementation wrote each sentence to a wav and waited for a
     * `MediaPlayer` to report completion before synthesising the next one, which
     * is why there was a hole between every sentence: the phone was decoding,
     * writing ~200 KB to flash, opening a player and *then* starting to think
     * about the next line.
     *
     * Now there are two threads and a two-sentence buffer. The synthesiser
     * produces sentence N+1 while the player is still speaking sentence N, so the
     * gap is closed. Audio goes straight from `GeneratedAudio.samples` (float
     * mono, already 22,050 Hz — the voice's own rate, no resampling anywhere) into
     * an `AudioTrack`, so nothing touches the filesystem and playback starts on
     * the first sample instead of after a container is parsed.
     *
     * [onStart] fires when the first sound plays, [onSentenceDone] after each
     * sentence's last sample has been written, [onError] when a line could not be
     * synthesised at all.
     */
    fun speak(
        sentences: List<String>,
        onStart: () -> Unit,
        onSentenceDone: () -> Unit,
        onError: () -> Unit
    ) {
        val tts = engine
        if (tts == null || sentences.isEmpty()) {
            main.post { onError() }
            return
        }
        val myToken = token.incrementAndGet()
        // Two slots: one playing, one ready. More would add latency, not smoothness.
        val queue = ArrayBlockingQueue<FloatArray>(2)
        val sampleRate = runCatching { tts.sampleRate() }.getOrDefault(DEFAULT_SAMPLE_RATE)
            .takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE

        synthesise(tts, sentences, queue, myToken, onError)
        playStream(queue, sampleRate, myToken, onStart, onSentenceDone)
    }

    /** Thread one: text → float samples, as fast as the CPU allows. */
    private fun synthesise(
        tts: OfflineTts,
        sentences: List<String>,
        queue: ArrayBlockingQueue<FloatArray>,
        myToken: Int,
        onError: () -> Unit
    ) {
        synth.execute {
            try {
                for (sentence in sentences) {
                    if (token.get() != myToken) return@execute
                    val audio = try {
                        tts.generate(sentence, 0, SPEED)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Could not synthesise a line", t)
                        null
                    }
                    if (audio == null) {
                        if (token.get() == myToken) main.post { onError() }
                        return@execute
                    }
                    // The voice pads every line with silence; trimming it is what
                    // makes one sentence run into the next like speech instead of
                    // like a slideshow.
                    val samples = trimSilence(audio.samples)
                    if (samples.isEmpty()) continue
                    // Timed offer, not put(): if the call is interrupted while the
                    // buffer is full, this must be able to see the new token and
                    // walk away instead of hanging on a lock for ever.
                    while (!queue.offer(samples, 100, TimeUnit.MILLISECONDS)) {
                        if (token.get() != myToken) return@execute
                    }
                }
            } catch (_: InterruptedException) {
            } finally {
                offerEnd(queue, myToken)
            }
        }
    }

    /** Thread two: float samples → the speaker, sentence after sentence. */
    private fun playStream(
        queue: ArrayBlockingQueue<FloatArray>,
        sampleRate: Int,
        myToken: Int,
        onStart: () -> Unit,
        onSentenceDone: () -> Unit
    ) {
        play.execute {
            var track: AudioTrack? = null
            var announced = false
            // Frames handed to the track since it was created, so "this sentence
            // has finished" can mean *played*, not merely *buffered*.
            var framesWritten = 0L
            try {
                while (true) {
                    if (token.get() != myToken) return@execute
                    val chunk = queue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    if (chunk.isEmpty()) return@execute // end sentinel

                    val out = track ?: buildTrack(sampleRate).also {
                        track = it
                        activeTrack = it
                        it.play()
                    }
                    if (!announced) {
                        announced = true
                        main.post { onStart() }
                    }

                    // Write in ~125 ms slices so an interrupt is heard immediately
                    // instead of after a whole sentence has been pushed through.
                    val slice = (sampleRate / 8).coerceAtLeast(1024)
                    var offset = 0
                    while (offset < chunk.size) {
                        if (token.get() != myToken) return@execute
                        val count = minOf(slice, chunk.size - offset)
                        val written = try {
                            out.write(chunk, offset, count, AudioTrack.WRITE_BLOCKING)
                        } catch (t: Throwable) {
                            Log.w(TAG, "Playback failed", t)
                            -1
                        }
                        if (written <= 0) return@execute
                        offset += written
                    }
                    framesWritten += chunk.size
                    // Do not hand the turn back while the last words are still in
                    // the output buffer: wait until the play head has passed the
                    // final frame of this sentence.
                    val deadline = System.nanoTime() + 60_000_000_000L
                    while (out.playbackHeadPosition.toLong() < framesWritten) {
                        if (token.get() != myToken) return@execute
                        if (System.nanoTime() > deadline) break
                        Thread.sleep(10)
                    }
                    if (token.get() != myToken) return@execute
                    main.post { onSentenceDone() }
                }
            } catch (_: InterruptedException) {
            } finally {
                if (activeTrack === track) activeTrack = null
                try {
                    track?.stop()
                } catch (_: Exception) {
                }
                try {
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * A line of speech with the dead air removed from both ends.
     *
     * Amy pads roughly 0.1–0.3 s of silence onto each line. Across a five-line
     * answer that is a full second of nothing, which reads as the tutor being
     * slow rather than as punctuation.
     */
    private fun trimSilence(samples: FloatArray): FloatArray {
        val threshold = 0.006f
        var first = 0
        while (first < samples.size && abs(samples[first]) < threshold) first++
        var last = samples.size - 1
        while (last > first && abs(samples[last]) < threshold) last--
        if (first >= last) return FloatArray(0)
        // Keep a short breath at each end: cutting on the exact waveform edge
        // clips the first and last consonant.
        val pad = (KEEP_MS * DEFAULT_SAMPLE_RATE / 1000)
        val from = (first - pad).coerceAtLeast(0)
        val to = (last + pad).coerceAtMost(samples.size - 1)
        return samples.copyOfRange(from, to + 1)
    }

    private fun offerEnd(queue: ArrayBlockingQueue<FloatArray>, myToken: Int) {
        val end = FloatArray(0)
        // The player may be a sentence behind; wait for a free slot rather than
        // dropping the marker (which would leave the player waiting for ever).
        while (queue.remainingCapacity() == 0) {
            if (token.get() != myToken) return
            try {
                Thread.sleep(20)
            } catch (_: InterruptedException) {
                return
            }
        }
        queue.offer(end)
    }

    private fun buildTrack(sampleRate: Int): AudioTrack {
        val bufferSize = (AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )).coerceAtLeast(sampleRate / 2 * 4)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    /** Barge-in: silence the tutor immediately and abandon the queue. */
    fun stop() {
        token.incrementAndGet()
        val track = activeTrack
        activeTrack = null
        if (track != null) {
            try {
                track.pause()
                track.flush()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        stop()
        try {
            engine?.release()
        } catch (_: Exception) {
        }
        engine = null
        synth.shutdownNow()
        play.shutdownNow()
    }

    // ── voice pack ───────────────────────────────────────────────────────────

    private fun download(url: URL, target: File, onProgress: ((Float) -> Unit)?) {
        val partial = File(target.parentFile, target.name + ".part")
        partial.delete()
        var connection: HttpURLConnection? = null
        try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "TheStudent-Android")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress?.invoke(written.toFloat() / total)
                    }
                    output.flush()
                }
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Unpack a `.tar.bz2` voice pack.
     *
     * Android has no bzip2 in the platform, hence commons-compress. The archive
     * unpacks into a folder of its own inside [destination].
     */
    private fun unpack(archive: File, destination: File) {
        destination.mkdirs()
        val root = destination.canonicalPath
        BZip2CompressorInputStream(BufferedInputStream(archive.inputStream())).use { bzip ->
            TarArchiveInputStream(bzip).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    val out = File(destination, entry.name)
                    // Never let an archive path escape the voice folder.
                    if (!out.canonicalPath.startsWith(root)) continue
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { sink -> tar.copyTo(sink) }
                    }
                }
            }
        }
    }

    private fun findTokens(): File? = rootDir.walkTopDown()
        .firstOrNull { it.isFile && it.name == "tokens.txt" }

    private fun findModel(): File? = rootDir.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".onnx", ignoreCase = true) }
        .maxByOrNull { it.length() }

    private fun findEspeakData(): File? = rootDir.walkTopDown()
        .firstOrNull { it.isDirectory && it.name == "espeak-ng-data" }

    private fun buildEngine(): OfflineTts? {
        val model = findModel() ?: return null
        val tokens = findTokens() ?: return null
        val data = findEspeakData() ?: return null
        // The Android AAR ships the *Kotlin* API: the configs are data classes
        // (named arguments, everything else defaulted) and the engine takes a
        // named `config` — the first parameter is an optional AssetManager.
        // `dataDir` is the espeak-ng data that turns English into phonemes; it
        // is still honoured on this 1.13.x runtime.
        val vits = OfflineTtsVitsModelConfig(
            model = model.absolutePath,
            tokens = tokens.absolutePath,
            dataDir = data.absolutePath
        )
        val modelConfig = OfflineTtsModelConfig(
            vits = vits,
            numThreads = 2
        )
        val config = OfflineTtsConfig(model = modelConfig)
        return OfflineTts(config = config)
    }

    companion object {
        private const val TAG = "PiperNarrator"

        /** `en_US-amy-medium`, int8: the same voice at 20 MB instead of 64. */
        private const val VOICE_FOLDER = "vits-piper-en_US-amy-medium-int8"
        private const val PACK_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
                "vits-piper-en_US-amy-medium-int8.tar.bz2"

        /**
         * A touch above 1.0. Amy's medium voice is deliberately unhurried; at
         * 0.98 the tutor sounded hesitant, at ~1.08 she sounds like a teacher
         * who is glad to see you.
         */
        private const val SPEED = 1.08f

        /** The voice's own rate — `en_US-amy-medium` is trained at 22,050 Hz. */
        private const val DEFAULT_SAMPLE_RATE = 22_050

        /** Breath kept at each end of a line, in milliseconds. */
        private const val KEEP_MS = 25
    }
}
