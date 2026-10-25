package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class TesseractResult(
    val text: String,
    val language: String,
    val isSuccess: Boolean,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

/**
 * Embedded On-Device Tesseract-OCR Engine for Sri Lankan educational documents.
 *
 * Optimized for high speed and accuracy:
 * 1. Pre-bundled integer-quantized LSTM neural models (tessdata_fast):
 *    - sin (Sinhala), tam (Tamil), eng (English & Math).
 * 2. Pre-allocated / cached TessBaseAPI instance with thread-safe Mutex:
 *    - Avoids re-initializing LSTM weights from disk on every scan (~2s saved).
 * 3. Intelligent inSampleSize Subsampling & Contrast Enhancement Preprocessing:
 *    - Automatically handles 48MP phone photos without OOM.
 *    - Enhances faint pen/pencil ink against grayish or unevenly lit paper.
 * 4. Page Segmentation Mode (PSM_AUTO) + Whitespace Preservation for equations.
 */
class TesseractOcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "TesseractOcrEngine"
        private const val TESSDATA_DIR = "tessdata"
        private val SUPPORTED_LANGUAGES = listOf("eng", "sin", "tam")
        private const val TARGET_MAX_DIMENSION = 1600
    }

    private val tessDataPath: String by lazy {
        context.filesDir.absolutePath
    }

    // Thread-safe mutex and cached instance to avoid repetitive expensive C++ init()
    private val mutex = Mutex()
    private var cachedTessApi: TessBaseAPI? = null
    private var cachedLangCode: String? = null

    /**
     * Initializes Tesseract traineddata files from APK assets into internal storage.
     * Native C++ Tesseract engine requires uncompressed file paths on disk.
     */
    suspend fun ensureTessDataInstalled() = withContext(Dispatchers.IO) {
        try {
            val tessDir = File(context.filesDir, TESSDATA_DIR)
            if (!tessDir.exists()) {
                tessDir.mkdirs()
            }

            for (lang in SUPPORTED_LANGUAGES) {
                val fileName = "$lang.traineddata"
                val destFile = File(tessDir, fileName)

                // If file does not exist or has zero size, copy from APK assets
                if (!destFile.exists() || destFile.length() < 1000L) {
                    try {
                        context.assets.open("$TESSDATA_DIR/$fileName").use { rawInput ->
                            BufferedInputStream(rawInput).use { input ->
                                BufferedOutputStream(FileOutputStream(destFile)).use { output ->
                                    input.copyTo(output, bufferSize = 32 * 1024)
                                }
                            }
                        }
                        Log.d(TAG, "Extracted $fileName to ${destFile.absolutePath} (${destFile.length()} bytes)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Asset copy failed for $fileName: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring tessdata installed", e)
        }
    }

    /**
     * Recognize text directly from an in-memory Bitmap (e.g. rendered from a PDF page via PdfRenderer)
     * without needing file disk URI roundtrips.
     */
    suspend fun recognizeDirectBitmap(
        sourceBitmap: Bitmap,
        preferredLanguage: String? = null
    ): TesseractResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        ensureTessDataInstalled()

        val enhanced = enhanceBitmapForOcr(sourceBitmap)

        val langCode = when (preferredLanguage?.lowercase()) {
            "sin" -> "sin+eng"
            "tam" -> "tam+eng"
            "eng" -> "eng"
            else -> {
                val hasSin = File(context.filesDir, "$TESSDATA_DIR/sin.traineddata").exists()
                val hasTam = File(context.filesDir, "$TESSDATA_DIR/tam.traineddata").exists()
                when {
                    hasSin && hasTam -> "sin+tam+eng"
                    hasSin -> "sin+eng"
                    hasTam -> "tam+eng"
                    else -> "eng"
                }
            }
        }

        mutex.withLock {
            try {
                var tessApi = cachedTessApi
                if (tessApi == null || cachedLangCode != langCode) {
                    tessApi?.recycle()
                    tessApi = TessBaseAPI()
                    val initSuccess = tessApi.init(tessDataPath, langCode)
                    if (!initSuccess) {
                        tessApi.init(tessDataPath, "eng")
                        cachedLangCode = "eng"
                    } else {
                        cachedLangCode = langCode
                    }
                    tessApi.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                    tessApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_FALSE)
                    tessApi.setVariable("preserve_interword_spaces", "1")
                    cachedTessApi = tessApi
                }

                tessApi.setImage(enhanced)
                val rawText = tessApi.utF8Text.orEmpty().trim()
                val latency = System.currentTimeMillis() - startTime
                val cleaned = cleanText(rawText)
                val detectedLang = detectLanguage(cleaned)

                TesseractResult(
                    text = cleaned,
                    language = detectedLang,
                    isSuccess = cleaned.isNotBlank(),
                    latencyMs = latency,
                    errorMessage = if (cleaned.isBlank()) "No text found on page." else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "recognizeDirectBitmap error", e)
                TesseractResult(
                    text = "",
                    language = "Unknown",
                    isSuccess = false,
                    errorMessage = e.message
                )
            } finally {
                if (enhanced != sourceBitmap && !enhanced.isRecycled) {
                    enhanced.recycle()
                }
            }
        }
    }

    /**
     * Recognize text from an image Uri using embedded on-device Tesseract.
     *
     * @param imageUri Uri of the image to process.
     * @param preferredLanguage "sin", "tam", "eng", or null for multilingual "eng+sin+tam".
     */
    suspend fun recognizeText(
        imageUri: Uri,
        preferredLanguage: String? = null
    ): TesseractResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. Ensure language files are prepared in filesDir/tessdata
        ensureTessDataInstalled()

        // 2. Decode and preprocess the image (subsampling + contrast stretching)
        val bitmap = preprocessBitmap(imageUri)
        if (bitmap == null) {
            return@withContext TesseractResult(
                text = "",
                language = "Unknown",
                isSuccess = false,
                errorMessage = "Could not decode image from URI."
            )
        }

        // 3. Determine Tesseract language string
        val langCode = when (preferredLanguage?.lowercase()) {
            "sin" -> "sin+eng"
            "tam" -> "tam+eng"
            "eng" -> "eng"
            else -> {
                val hasSin = File(context.filesDir, "$TESSDATA_DIR/sin.traineddata").exists()
                val hasTam = File(context.filesDir, "$TESSDATA_DIR/tam.traineddata").exists()
                when {
                    hasSin && hasTam -> "sin+tam+eng"
                    hasSin -> "sin+eng"
                    hasTam -> "tam+eng"
                    else -> "eng"
                }
            }
        }

        mutex.withLock {
            try {
                var tessApi = cachedTessApi

                // Re-initialize only if language changed or instance is null
                if (tessApi == null || cachedLangCode != langCode) {
                    tessApi?.recycle()
                    tessApi = TessBaseAPI()

                    val initSuccess = tessApi.init(tessDataPath, langCode)
                    if (!initSuccess) {
                        Log.w(TAG, "Failed to init $langCode, falling back to eng")
                        val fallbackSuccess = tessApi.init(tessDataPath, "eng")
                        if (!fallbackSuccess) {
                            cachedTessApi = null
                            cachedLangCode = null
                            return@withContext TesseractResult(
                                text = "",
                                language = "Unknown",
                                isSuccess = false,
                                errorMessage = "Tesseract initialization failed for $langCode."
                            )
                        }
                        cachedLangCode = "eng"
                    } else {
                        cachedLangCode = langCode
                    }

                    // Configure for speed and accuracy
                    tessApi.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                    tessApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_FALSE)
                    tessApi.setVariable("preserve_interword_spaces", "1")

                    cachedTessApi = tessApi
                }

                // Feed bitmap and execute C++ LSTM recognition
                tessApi.setImage(bitmap)
                val rawText = tessApi.utF8Text.orEmpty().trim()
                val latency = System.currentTimeMillis() - startTime

                val cleaned = cleanText(rawText)
                val detectedLang = detectLanguage(cleaned)

                TesseractResult(
                    text = cleaned,
                    language = detectedLang,
                    isSuccess = cleaned.isNotBlank(),
                    latencyMs = latency,
                    errorMessage = if (cleaned.isBlank()) "No text found in photo. Please ensure clear focus and lighting." else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Tesseract recognition error", e)
                TesseractResult(
                    text = "",
                    language = "Unknown",
                    isSuccess = false,
                    errorMessage = e.message
                )
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    /**
     * Preprocesses the bitmap for optimal Tesseract OCR performance:
     * - Uses inSampleSize during decoding to prevent OOM on 48MP photos.
     * - Resizes to optimal resolution (~1600px max dimension).
     * - Converts to high-contrast grayscale with luminance stretching for ink sharpness.
     */
    private fun preprocessBitmap(uri: Uri): Bitmap? {
        return try {
            // Step 1: Decode bounds only to calculate inSampleSize
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            val maxOriginal = maxOf(origWidth, origHeight)
            while ((maxOriginal / sampleSize) > TARGET_MAX_DIMENSION * 1.5) {
                sampleSize *= 2
            }

            // Step 2: Decode subsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val subsampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Step 3: Exact scaling to TARGET_MAX_DIMENSION if needed
            val curWidth = subsampled.width
            val curHeight = subsampled.height
            val scale = if (curWidth > TARGET_MAX_DIMENSION || curHeight > TARGET_MAX_DIMENSION) {
                TARGET_MAX_DIMENSION.toFloat() / maxOf(curWidth, curHeight)
            } else 1.0f

            val scaled = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    subsampled,
                    (curWidth * scale).toInt(),
                    (curHeight * scale).toInt(),
                    true
                ).also {
                    if (it != subsampled) subsampled.recycle()
                }
            } else {
                subsampled
            }

            enhanceBitmapForOcr(scaled)
        } catch (e: Exception) {
            Log.e(TAG, "Error preprocessing bitmap for Tesseract", e)
            null
        }
    }

    /**
     * Grayscale binarization and contrast boost for sharp text detection.
     */
    fun enhanceBitmapForOcr(source: Bitmap): Bitmap {
        return try {
            val curWidth = source.width
            val curHeight = source.height
            val scale = if (curWidth > TARGET_MAX_DIMENSION || curHeight > TARGET_MAX_DIMENSION) {
                TARGET_MAX_DIMENSION.toFloat() / maxOf(curWidth, curHeight)
            } else 1.0f

            val scaled = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    source,
                    (curWidth * scale).toInt(),
                    (curHeight * scale).toInt(),
                    true
                )
            } else {
                source
            }

            val enhanced = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(enhanced)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            val contrast = 1.35f
            val brightness = 12f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
            val grayscaleMatrix = ColorMatrix().apply { setSaturation(0f) }
            contrastMatrix.preConcat(grayscaleMatrix)

            paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
            canvas.drawBitmap(scaled, 0f, 0f, paint)

            if (scaled != source && scaled != enhanced) {
                scaled.recycle()
            }
            enhanced
        } catch (e: Exception) {
            Log.w(TAG, "enhanceBitmapForOcr fallback to original: ${e.message}")
            source
        }
    }

    private fun detectLanguage(text: String): String {
        val hasSinhala = text.any { it.code in 0x0D80..0x0DFF }
        val hasTamil = text.any { it.code in 0x0B80..0x0BFF }
        val hasEnglishMath = text.any { it in 'a'..'z' || it in 'A'..'Z' || it in "+=-*/^()√" }

        return when {
            hasSinhala && hasEnglishMath -> "Sinhala & Math (ද්විභාෂික)"
            hasSinhala -> "Sinhala (සිංහල)"
            hasTamil && hasEnglishMath -> "Tamil & Math (இருமொழி)"
            hasTamil -> "Tamil (தமிழ்)"
            else -> "English & Math"
        }
    }

    private fun cleanText(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            // Remove common Tesseract speckle noise (solitary stray characters like ~ or |)
            .lines()
            .map { line ->
                val trimmed = line.trim()
                if (trimmed.length == 1 && trimmed[0] in "~|`_\"'") "" else trimmed
            }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
    }
}
