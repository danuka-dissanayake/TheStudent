package com.example.data.voice

import android.content.Context
import android.media.MediaPlayer
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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
 *  3. **Playback.** Each sentence is synthesised to a temporary wav and played by
 *     a `MediaPlayer`, one after another, on the same sentence boundaries the
 *     call loop already uses. Callbacks always arrive on the main thread.
 *
 * Everything here degrades: if the download fails, the model is corrupt or the
 * native library is missing, [isReady] simply stays false and the caller keeps
 * using Android's own voice.
 */
class PiperNarrator(private val context: Context) {

    private val rootDir = File(context.filesDir, "piper")
    private val voiceDir = File(rootDir, VOICE_FOLDER)
    private val cacheDir = File(context.cacheDir, "piper").apply { mkdirs() }
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    /** Bumped by [stop]; a worker whose token no longer matches walks away. */
    private val token = AtomicInteger(0)

    private var engine: OfflineTts? = null
    private var player: MediaPlayer? = null

    /** The engine is loaded and can speak right now. */
    fun isReady(): Boolean = engine != null

    /** The voice pack is already on disk, so no download is needed. */
    fun isDownloaded(): Boolean =
        File(voiceDir, "tokens.txt").exists() && findModel() != null && findEspeakData() != null

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
     * Speak a list of sentences, one after another.
     *
     * [onStart] fires when the first sound is about to play, [onSentenceDone]
     * after each sentence finishes (so the call loop can count them down), and
     * [onError] when synthesis or playback failed for good. Missing a sentence
     * is never silently swallowed: [onError] is the caller's cue to fall back.
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
        worker.execute {
            var started = false
            for ((index, sentence) in sentences.withIndex()) {
                if (token.get() != myToken) return@execute
                val file = File(cacheDir, "line-$myToken-$index.wav")
                val rendered = try {
                    val audio = tts.generate(sentence, 0, SPEED)
                    audio.save(file.absolutePath) && file.length() > 1_000L
                } catch (t: Throwable) {
                    Log.w(TAG, "Could not synthesise a line", t)
                    false
                }
                if (!rendered) {
                    if (token.get() == myToken) main.post { onError() }
                    return@execute
                }
                if (token.get() != myToken) {
                    file.delete()
                    return@execute
                }

                if (!started) {
                    started = true
                    main.post { onStart() }
                }

                val played = CountDownLatch(1)
                main.post {
                    play(file) {
                        played.countDown()
                    }
                }
                played.await(60, TimeUnit.SECONDS)
                file.delete()

                if (token.get() != myToken) return@execute
                main.post { onSentenceDone() }
            }
        }
    }

    /** Barge-in: silence the tutor immediately and abandon the queue. */
    fun stop() {
        token.incrementAndGet()
        main.post { stopPlayer() }
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        try {
            engine?.release()
        } catch (_: Exception) {
        }
        engine = null
        worker.shutdownNow()
    }

    // ── playback ─────────────────────────────────────────────────────────────

    private fun play(file: File, onFinished: () -> Unit) {
        stopPlayer()
        val mp = MediaPlayer()
        player = mp
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            if (player === mp) player = null
            try {
                mp.release()
            } catch (_: Exception) {
            }
            onFinished()
        }
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, _, _ ->
                finish()
                true
            }
            mp.prepare()
            mp.start()
        } catch (t: Throwable) {
            Log.w(TAG, "Playback failed", t)
            finish()
        }
    }

    private fun stopPlayer() {
        val mp = player ?: return
        player = null
        try {
            mp.stop()
        } catch (_: Exception) {
        }
        try {
            mp.release()
        } catch (_: Exception) {
        }
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

    private fun findModel(): File? = voiceDir.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".onnx", ignoreCase = true) }
        .maxByOrNull { it.length() }

    private fun findEspeakData(): File? = voiceDir.walkTopDown()
        .firstOrNull { it.isDirectory && it.name == "espeak-ng-data" }

    private fun buildEngine(): OfflineTts? {
        val model = findModel() ?: return null
        val tokens = voiceDir.walkTopDown().firstOrNull { it.isFile && it.name == "tokens.txt" } ?: return null
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

        /** Slightly under 1.0: a tutor speaking to a learner, not a newsreader. */
        private const val SPEED = 0.98f
    }
}
