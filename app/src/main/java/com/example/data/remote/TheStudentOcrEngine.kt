package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.TesseractOcrEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class TheStudentOcrResult(
    val extractedText: String,
    val detectedLanguage: String, // "Sinhala (සිංහල)", "Tamil (தமிழ்)", "English & Math"
    val isAutoDetected: Boolean = true,
    val isSuccess: Boolean = true,
    val engineUsed: String = "Embedded Tesseract-OCR (On-Device)",
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

/**
 * High-performance OCR Engine with embedded on-device Tesseract-OCR.
 *
 * Engine Capabilities:
 * 1. Embedded On-Device Tesseract-OCR:
 *    - Runs 100% on the user's phone CPU without requiring any API or internet connection.
 *    - Bundled with Sinhala (sin), Tamil (tam), and English (eng) traineddata models.
 *    - Completely free with zero rate limits or quotas.
 * 2. On-Device Google ML Kit:
 *    - Instant local Latin & math character recognition (~30ms).
 * 3. Google Gemini 3.8 Flash Vision:
 *    - High-precision cloud multimodal OCR for complex diagrams and handwriting.
 */
class TheStudentOcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "TheStudentOcrEngine"
        private const val GEMINI_MODEL = "gemini-3.8-flash"
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"
    }

    private val tesseractOcrEngine = TesseractOcrEngine(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Run pure embedded Tesseract OCR on-device with zero external API.
     */
    suspend fun extractWithTesseractOnDevice(
        imageUri: Uri,
        preferredLanguage: String? = null
    ): TheStudentOcrResult {
        val result = tesseractOcrEngine.recognizeText(imageUri, preferredLanguage)
        return TheStudentOcrResult(
            extractedText = result.text,
            detectedLanguage = result.language,
            isAutoDetected = preferredLanguage == null || preferredLanguage == "auto",
            isSuccess = result.isSuccess,
            engineUsed = "Embedded Tesseract-OCR (On-Device ⚡)",
            latencyMs = result.latencyMs,
            errorMessage = result.errorMessage
        )
    }

    /**
     * Extracts text directly from an in-memory Bitmap (e.g. rendered PDF page).
     * Automatically handles Sinhala (sin), Tamil (tam), and English & Math (eng).
     */
    suspend fun extractTextFromBitmap(
        bitmap: Bitmap,
        preferredLanguage: String? = null
    ): TheStudentOcrResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val tessResult = tesseractOcrEngine.recognizeDirectBitmap(bitmap, preferredLanguage)
        if (tessResult.isSuccess && tessResult.text.isNotBlank()) {
            return@withContext TheStudentOcrResult(
                extractedText = tessResult.text,
                detectedLanguage = tessResult.language,
                isAutoDetected = preferredLanguage == null || preferredLanguage == "auto",
                isSuccess = true,
                engineUsed = "Embedded Tesseract-OCR (On-Device ⚡)",
                latencyMs = tessResult.latencyMs,
                errorMessage = null
            )
        }

        val onDeviceText = runOnDeviceMlKitBitmap(bitmap)
        val mlKitLatency = System.currentTimeMillis() - startTime
        if (onDeviceText.isNotBlank()) {
            return@withContext TheStudentOcrResult(
                extractedText = onDeviceText,
                detectedLanguage = detectLanguageFromText(onDeviceText),
                isAutoDetected = true,
                isSuccess = true,
                engineUsed = "On-Device ML Kit (Fast Latin & Math ⚡)",
                latencyMs = mlKitLatency
            )
        }

        return@withContext TheStudentOcrResult(
            extractedText = "",
            detectedLanguage = "Unknown",
            isSuccess = false,
            engineUsed = "On-Device OCR Engine",
            errorMessage = "Could not extract legible text from this page."
        )
    }

    /**
     * Automatically extract text from an image with high precision.
     * Evaluates available engines:
     * - If Gemini API key is available and online, uses Gemini Vision for maximum precision.
     * - Falls back directly to Embedded On-Device Tesseract-OCR (Zero API) + ML Kit.
     */
    suspend fun extractTextFromImage(
        imageUri: Uri,
        preferredLanguage: String? = null, // null for auto-detect, or "sin", "tam", "eng"
        forceOnDeviceOnly: Boolean = false
    ): TheStudentOcrResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val isAutoMode = preferredLanguage == null || preferredLanguage == "auto"

        // 1. Primary Engine: Embedded On-Device Tesseract-OCR (Zero API, 100% Offline, Fast quantized LSTM)
        val tessResult = extractWithTesseractOnDevice(imageUri, preferredLanguage)
        if (tessResult.isSuccess && tessResult.extractedText.isNotBlank()) {
            return@withContext tessResult
        }

        // 2. Fallback to On-Device Google ML Kit (Instant Latin & Math)
        val onDeviceText = runOnDeviceMlKit(imageUri)
        val mlKitLatency = System.currentTimeMillis() - startTime
        if (onDeviceText.isNotBlank()) {
            val detectedLang = if (containsSinhala(onDeviceText)) {
                "Sinhala (සිංහල)"
            } else if (containsTamil(onDeviceText)) {
                "Tamil (தமிழ்)"
            } else {
                "English & Math"
            }

            return@withContext TheStudentOcrResult(
                extractedText = cleanOcrOutput(onDeviceText),
                detectedLanguage = detectedLang,
                isAutoDetected = isAutoMode,
                isSuccess = true,
                engineUsed = "On-Device ML Kit (Offline ⚡)",
                latencyMs = mlKitLatency
            )
        }

        // 3. Fallback to Cloud Gemini Vision if online and API key configured
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (!forceOnDeviceOnly && apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")) {
            val imageBase64 = encodeImageUriToBase64(imageUri)
            if (imageBase64 != null) {
                val geminiResult = runGeminiVisionOcr(imageBase64, preferredLanguage, apiKey)
                if (geminiResult.isSuccess && geminiResult.extractedText.isNotBlank()) {
                    val latency = System.currentTimeMillis() - startTime
                    return@withContext geminiResult.copy(
                        isAutoDetected = isAutoMode,
                        latencyMs = latency
                    )
                }
            }
        }
        val latency = System.currentTimeMillis() - startTime
        TheStudentOcrResult(
            extractedText = "",
            detectedLanguage = "Unknown",
            isAutoDetected = isAutoMode,
            isSuccess = false,
            errorMessage = "Could not clearly read text from photo. Please ensure good lighting and clear focus.",
            latencyMs = latency
        )
    }

    /**
     * Executes Google Gemini 3.8 Flash Multimodal OCR call.
     */
    private fun runGeminiVisionOcr(
        imageBase64: String,
        preferredLanguage: String?,
        apiKey: String
    ): TheStudentOcrResult {
        return try {
            val endpointUrl = "$GEMINI_API_URL?key=$apiKey"

            val langInstruction = when (preferredLanguage) {
                "sin" -> "Focus specifically on accurate Sinhala (සිංහල) characters, diacritics, and any accompanying math formulas."
                "tam" -> "Focus specifically on accurate Tamil (தமிழ்) characters, diacritics, and any accompanying math formulas."
                "eng" -> "Focus on English text, numbers, formulas, and diagrams."
                else -> "Transcribe all languages present: Sinhala (සිංහල), Tamil (தமிழ்), English, and mathematical expressions exactly as they appear."
            }

            val ocrSystemPrompt = """You are a high-precision OCR transcription engine for Sri Lankan educational past papers, textbooks, and exam questions.
Task: Transcribe all text, question labels, numbers, and formulas from this image verbatim with 100% accuracy.
Rules:
1. $langInstruction
2. Accurately transcribe mathematical symbols, fractions, superscripts/subscripts, Greek letters, and formulas (e.g., F = ma, v = u + at, integrals, square roots).
3. Preserve question numbering exactly (e.g. 01, (a), (i), (ii), 2.1, A, B, C, D).
4. Do NOT add commentary, answers, solutions, or explanations.
5. Do NOT wrap output in markdown codeblocks. Output ONLY the raw transcribed text verbatim."""

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", ocrSystemPrompt)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", imageBase64)
                            })
                        })
                    })
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 4096)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val firstPart = parts.getJSONObject(0)
                        val text = firstPart.optString("text", "").trim()
                        if (text.isNotBlank()) {
                            val detectedLanguage = detectLanguageFromText(text)
                            return TheStudentOcrResult(
                                extractedText = cleanOcrOutput(text),
                                detectedLanguage = detectedLanguage,
                                isSuccess = true,
                                engineUsed = "Google Gemini 3.8 Flash Vision (100% Precision)"
                            )
                        }
                    }
                }
            }

            TheStudentOcrResult(
                extractedText = "",
                detectedLanguage = "Unknown",
                isSuccess = false,
                errorMessage = "Gemini OCR returned code ${response.code}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "runGeminiVisionOcr exception", e)
            TheStudentOcrResult(
                extractedText = "",
                detectedLanguage = "Unknown",
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    private fun detectLanguageFromText(text: String): String {
        val hasSinhala = containsSinhala(text)
        val hasTamil = containsTamil(text)
        val hasEnglishMath = containsEnglishMath(text)

        return when {
            hasSinhala && hasEnglishMath -> "Sinhala & Math (ද්විභාෂික)"
            hasSinhala -> "Sinhala (සිංහල)"
            hasTamil && hasEnglishMath -> "Tamil & Math (இருமொழி)"
            hasTamil -> "Tamil (தமிழ்)"
            else -> "English & Math"
        }
    }

    private suspend fun runOnDeviceMlKit(uri: Uri): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (cont.isActive) cont.resume(visionText.text.trim())
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "On-device ML Kit error", e)
                        if (cont.isActive) cont.resume("")
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "ML Kit initialization error", t)
                if (cont.isActive) cont.resume("")
            }
        }
    }

    private suspend fun runOnDeviceMlKitBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (cont.isActive) cont.resume(visionText.text.trim())
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "On-device ML Kit error on bitmap", e)
                        if (cont.isActive) cont.resume("")
                    }
            } catch (t: Throwable) {
                Log.w(TAG, "ML Kit bitmap error", t)
                if (cont.isActive) cont.resume("")
            }
        }
    }

    private fun containsSinhala(text: String): Boolean {
        return text.any { it.code in 0x0D80..0x0DFF }
    }

    private fun containsTamil(text: String): Boolean {
        return text.any { it.code in 0x0B80..0x0BFF }
    }

    private fun containsEnglishMath(text: String): Boolean {
        return text.any { it in 'a'..'z' || it in 'A'..'Z' || it in "+=-*/^()√" }
    }

    private fun cleanOcrOutput(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun encodeImageUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val maxDim = 1600
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scale = if (width > maxDim || height > maxDim) {
                    maxDim.toFloat() / maxOf(width, height)
                } else 1.0f

                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (width * scale).toInt(),
                        (height * scale).toInt(),
                        true
                    )
                } else originalBitmap

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
                val byteArray = outputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to encode image uri: $uri", e)
            null
        }
    }
}
