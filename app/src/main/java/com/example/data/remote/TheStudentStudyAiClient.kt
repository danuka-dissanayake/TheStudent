package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.TesseractOcrEngine
import com.example.data.model.AiModelChoice
import com.example.data.model.AttachedMedia
import com.example.data.model.AttachedMediaType
import kotlinx.coroutines.Dispatchers
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

sealed class TheStudentAiStudyResponse {
    data class Success(
        val text: String,
        val suggestedPrompts: List<String> = emptyList(),
        val latencyMs: Long = 0,
        val modelUsed: String = "AI Assistant",
        val reasoningText: String? = null
    ) : TheStudentAiStudyResponse()

    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
        val isSleepingOrOffline: Boolean = false
    ) : TheStudentAiStudyResponse()
}

private data class ToolCallAccumulator(
    var id: String = "",
    var name: String = "",
    val arguments: StringBuilder = StringBuilder()
)

/**
 * Intelligent multimodal study assistant client for Sri Lankan students.
 *
 * Engine Strategy:
 * 1. Z.AI Free Tier: GLM-4.7-Flash with real-time SSE streaming & native tool calling.
 * 2. Hugging Face Space: danuka21/dzd-learn-ai (GPT-OSS 20B with deep reasoning).
 * 3. Google Gemini 3.8 Flash: Multimodal Vision & OCR reasoning.
 */
class TheStudentStudyAiClient(private val context: Context) {

    companion object {
        private const val TAG = "TheStudentStudyAiClient"

        // Google Gemini
        private const val GEMINI_MODEL = "gemini-3.8-flash"
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

        // Hugging Face Space Subdomain and Web URLs (Preserved exactly as configured)
        const val SPACE_DIRECT_URL = "https://danuka21-dzd-learn-ai.hf.space"
        const val SPACE_WEB_URL = "https://huggingface.co/spaces/danuka21/dzd-learn-ai"

        // Z.AI / BigModel Free Model & Endpoints
        const val ZAI_BASE_URL = "https://api.z.ai/api/paas/v4"
        const val ZAI_FALLBACK_URL = "https://open.bigmodel.cn/api/paas/v4"
        const val GLM_4_7_FLASH = "glm-4.7-flash"
        const val GLM_4_FLASH = "glm-4-flash"

        const val SYSTEM_PROMPT = """You are TheStudent AI, an expert, encouraging, and intelligent study assistant for Sri Lankan students (A/L, O/L, Grade 6-13, and University levels).

Guidelines:
1. Language Fluency: You understand and generate Sinhala (සිංහල), English, and Singlish fluently. If the student speaks/asks in Sinhala (සිංහල) or Singlish, answer clearly and accurately in Sinhala/Singlish. If asked in English, answer in clear, articulate English. If asked in Tamil (தமிழ்), answer in Tamil.
2. Step-by-Step Learning: Break down complex mathematics, physics, chemistry, biology, ICT, and commerce problems step-by-step with clear formulas, derivations, and final verified answers.
3. Formatting: Format answers with clean Markdown headings (###), bullet points (•), and place mathematical equations and calculations inside code blocks (```) for high readability. If the student requests a specific format (such as a Markdown table (| Col 1 | Col 2 |), numbered list, or comparative breakdown), strictly format the answer according to the student's requested structure. Never force an unwanted template.
4. Numbered Lists & Headings: When using numbered lists, always start numbering sequentially from 1 (1., 2., 3., 4.). Never skip number 1 and never start a list from 2. Never prefix numbered items with heading hashes (e.g. do not write '### 1.').
5. Multimodal & Documents: When analyzing photos of exam questions, diagrams, or PDF study notes, thoroughly inspect the exact question numbers, variables, and diagrams, explain the underlying theory, and provide the complete marking-scheme solution.
6. Direct & Accurate Guidance: Never generate fake challenge questions, puzzles, or trivia (such as 'High-Yield Practice Challenge' or logic riddles) at the end of answers. Provide only accurate, verified study explanations and marking scheme criteria.
7. Web & Document Links: When providing official portal links, past papers, gazettes, or PDF study notes, format them cleanly as [Descriptive Title](URL) on their own line or list item so the interface renders them as clickable buttons.
8. Natural Labels & Direct Answers: Never mention internal technical tool names, MCP servers, DuckDuckGo, protocols, or APIs in your thoughts or answers. Refer naturally to actions like 'searching the web for...' or 'calculating'. Directly and accurately answer what the student asked.
9. Document Retrieval & Multi-Source Search Hierarchy: When the student asks for a past paper, model paper, term test paper, timetable, marking scheme, syllabus, or exam document for any subject/year (e.g. '2023 Combined Maths Sinhala medium', '2019 SFT paper', '2024 Physics marking scheme', '2026 timetable'):
- PRIORITY 1: ALWAYS search and filter `pastpapers.wiki` website first using `Tools_Web_Search` (e.g. 'site:pastpapers.wiki 2019 SFT past paper download' or 'pastpapers.wiki 2023 Combined Maths marking scheme').
- PRIORITY 2: If the requested paper or marking scheme is NOT available on `pastpapers.wiki`, search and check `govdoc.lk` (e.g. 'site:govdoc.lk 2019 SFT' or 'govdoc.lk Combined Maths 2023').
- PRIORITY 3: If still not available on `govdoc.lk`, search and filter other verified educational websites (e.g. `doenets.lk`, `e-thaksalawa.moe.gov.lk`, `e-kalvi.com`, `nie.lk`, `apepanthiya.lk`, etc.).
- Call `download_and_share_document` with the exact title, medium, and discovered URL to download the real verified document into the chat for the student.
- In your final response text, provide only a brief 1-sentence friendly confirmation introducing the paper (e.g. 'Here is the official 2019 A/L Science for Technology past paper and marking scheme ready for download below.'), followed immediately on its own line by the download card [DOWNLOAD_DOCUMENT:...].
- Do NOT output any duplicate bullet points (such as Document, Medium, Year, Type, Pages, or Content) in the outer text, because all document details are already built into the download card.
- Filter out and provide ONLY the exact requested document (or paper + marking scheme). Do NOT dump lists of random links and do NOT generate challenge questions."""
    }

    init {
        TheStudentStudyTools.appContext = context.applicationContext
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val tesseractOcrEngine = TesseractOcrEngine(context)

    /**
     * Resolves the Z.AI API key from:
     * 1. User runtime setting
     * 2. BuildConfig.ZAI_API_KEY / BuildConfig.Z_AI_API_KEY (from Secrets panel)
     * 3. System environment variable
     */
    fun getZaiApiKey(customKey: String? = null): String {
        val custom = customKey?.trim().orEmpty()
        if (custom.isNotBlank()) return custom

        val bcZai = try {
            val key = BuildConfig.ZAI_API_KEY.trim()
            if (key.isNotBlank() && !key.contains("YOUR_ZAI_API_KEY_HERE")) key else ""
        } catch (_: Throwable) { "" }
        if (bcZai.isNotBlank()) return bcZai

        val bcZ_Ai = try {
            val key = BuildConfig.Z_AI_API_KEY.trim()
            if (key.isNotBlank() && !key.contains("YOUR_Z_AI_API_KEY_HERE")) key else ""
        } catch (_: Throwable) { "" }
        if (bcZ_Ai.isNotBlank()) return bcZ_Ai

        val envKey = System.getenv("ZAI_API_KEY") ?: System.getenv("Z_AI_API_KEY")
        return envKey?.trim().orEmpty()
    }

    /**
     * Check whether a valid Z.AI API Key is present in the environment or settings.
     */
    fun isZaiApiKeyAvailable(customKey: String? = null): Boolean {
        return getZaiApiKey(customKey).isNotBlank()
    }

    /**
     * Send query to the AI study backend with real-time SSE token streaming and live reasoning support.
     * Supports:
     * - GLM-4.7-Flash (Z.AI Free Tier) with multi-turn tool calling
     * - Hugging Face Space (GPT-OSS 20B with deep reasoning)
     * - Google Gemini 3.8 Flash (Multimodal vision)
     */
    suspend fun streamStudyResponse(
        prompt: String,
        attachment: AttachedMedia? = null,
        subject: String = "General Studies",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customToken: String? = null,
        customBaseUrl: String? = null,
        selectedModel: AiModelChoice = AiModelChoice.GLM_4_7_FLASH,
        customZaiApiKey: String? = null,
        onReasoningChunk: suspend (String) -> Unit = {},
        onReasoningComplete: suspend () -> Unit = {},
        onContentChunk: suspend (String) -> Unit = {}
    ): TheStudentAiStudyResponse = withContext(Dispatchers.IO) {
        val geminiKey = BuildConfig.GEMINI_API_KEY.trim()
        val zaiKey = getZaiApiKey(customZaiApiKey)
        val hasCustomHfUrl = !customBaseUrl.isNullOrBlank()

        when (selectedModel) {
            AiModelChoice.GLM_4_7_FLASH -> {
                if (zaiKey.isNotBlank()) {
                    val zaiResponse = streamZaiGlmResponse(
                        prompt = prompt,
                        attachment = attachment,
                        subject = subject,
                        conversationHistory = conversationHistory,
                        apiKey = zaiKey,
                        onReasoningChunk = onReasoningChunk,
                        onReasoningComplete = onReasoningComplete,
                        onContentChunk = onContentChunk
                    )
                    if (zaiResponse is TheStudentAiStudyResponse.Success) {
                        return@withContext zaiResponse
                    }
                    Log.w(TAG, "Z.AI GLM-4.7-Flash failed: ${(zaiResponse as? TheStudentAiStudyResponse.Error)?.message}. Seamlessly falling back to Hugging Face Space...")
                } else {
                    Log.i(TAG, "ZAI_API_KEY not configured. Falling back to Hugging Face Space TheStudent AI...")
                }
            }
            AiModelChoice.GEMINI_FLASH -> {
                if (!hasCustomHfUrl && geminiKey.isNotBlank() && !geminiKey.contains("MY_GEMINI_API_KEY")) {
                    val geminiResponse = generateGeminiStudyResponse(
                        prompt = prompt,
                        attachment = attachment,
                        subject = subject,
                        conversationHistory = conversationHistory,
                        apiKey = geminiKey
                    )
                    if (geminiResponse is TheStudentAiStudyResponse.Success) {
                        onReasoningComplete()
                        onContentChunk(geminiResponse.text)
                        return@withContext geminiResponse
                    }
                }
            }
            AiModelChoice.HUGGING_FACE -> {
                // User explicitly selected Hugging Face Space
            }
        }

        // Secondary / Fallback Engine: Hugging Face Space (danuka21/dzd-learn-ai with real-time SSE streaming)
        // Keeps user's Hugging Face Space configuration completely intact!
        streamHuggingFaceSpaceResponse(
            prompt = prompt,
            attachment = attachment,
            subject = subject,
            conversationHistory = conversationHistory,
            customToken = customToken,
            customBaseUrl = customBaseUrl,
            onReasoningChunk = onReasoningChunk,
            onReasoningComplete = onReasoningComplete,
            onContentChunk = onContentChunk
        )
    }

    /**
     * Send query to the AI study backend (non-streaming wrapper).
     */
    suspend fun generateStudyResponse(
        prompt: String,
        attachment: AttachedMedia? = null,
        subject: String = "General Studies",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customToken: String? = null,
        customBaseUrl: String? = null,
        selectedModel: AiModelChoice = AiModelChoice.GLM_4_7_FLASH,
        customZaiApiKey: String? = null
    ): TheStudentAiStudyResponse = streamStudyResponse(
        prompt = prompt,
        attachment = attachment,
        subject = subject,
        conversationHistory = conversationHistory,
        customToken = customToken,
        customBaseUrl = customBaseUrl,
        selectedModel = selectedModel,
        customZaiApiKey = customZaiApiKey
    )

    /**
     * Send multimodal / text query to Google Gemini 3.8 Flash.
     */
    private fun generateGeminiStudyResponse(
        prompt: String,
        attachment: AttachedMedia?,
        subject: String,
        conversationHistory: List<Pair<String, String>>,
        apiKey: String
    ): TheStudentAiStudyResponse {
        val startTime = System.currentTimeMillis()
        val endpointUrl = "$GEMINI_API_URL?key=$apiKey"

        try {
            val contentsArray = JSONArray()

            // 1. Conversation History (last 6 turns)
            conversationHistory.takeLast(6).forEach { (role, text) ->
                val geminiRole = if (role.equals("user", ignoreCase = true)) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    })
                })
            }

            // 2. Current User Turn (with optional multimodal image / document context)
            val userParts = JSONArray()

            val isSinhala = prompt.any { it.code in 0x0D80..0x0DFF } ||
                            attachment?.extractedOcrText?.any { it.code in 0x0D80..0x0DFF } == true ||
                            attachment?.ocrLanguage?.contains("Sinhala", ignoreCase = true) == true

            val isTamil = prompt.any { it.code in 0x0B80..0x0BFF } ||
                          attachment?.extractedOcrText?.any { it.code in 0x0B80..0x0BFF } == true ||
                          attachment?.ocrLanguage?.contains("Tamil", ignoreCase = true) == true

            val languageRequirement = when {
                isSinhala -> "\n\n[MANDATORY LANGUAGE: The student's question or attached document is in Sinhala (සිංහල). You MUST provide your complete explanation, step-by-step reasoning, and final answer in natural, student-friendly Sinhala (සිංහල). Keep mathematical formulas, symbols, and scientific terms clear and well-formatted.]"
                isTamil -> "\n\n[MANDATORY LANGUAGE: The student's question or attached document is in Tamil (தமிழ்). You MUST provide your complete explanation, step-by-step reasoning, and final answer in natural, student-friendly Tamil (தமிழ்). Keep mathematical formulas, symbols, and scientific terms clear and well-formatted.]"
                else -> ""
            }

            val ocrContext = if (!attachment?.extractedOcrText.isNullOrBlank()) {
                "\n\n[Auto-Extracted Text from Image (Tesseract-OCR)]:\n${attachment.extractedOcrText}"
            } else ""

            val promptText = when {
                prompt.isNotBlank() -> "$prompt$ocrContext$languageRequirement"
                attachment?.type == AttachedMediaType.IMAGE -> {
                    val defaultText = if (isSinhala) {
                        "කරුණාකර මෙම පින්තූරයේ ඇති විභාග ප්‍රශ්නය පියවරෙන් පියවර පැහැදිලි කර නිවැරදි පිළිතුර සහ සමීකරණ සිංහලෙන් ලබා දෙන්න."
                    } else if (isTamil) {
                        "தயவுசெய்து இந்தப் படத்தில் உள்ள தேர்வு வினாவை படி படியாக விளக்கி தெளிவான தீர்வு வழங்கவும்."
                    } else {
                        "Please solve and explain the exam question shown in this image step-by-step with clear formulas and verified answers."
                    }
                    "$defaultText$ocrContext$languageRequirement"
                }
                attachment?.type == AttachedMediaType.PDF -> "Please summarize the key exam concepts and high-yield notes from this document.$languageRequirement"
                else -> "Help me study for $subject.$languageRequirement"
            }

            userParts.put(JSONObject().apply {
                put("text", promptText)
            })

            // Attach image if present
            if (attachment?.type == AttachedMediaType.IMAGE && !attachment.uri.isNullOrBlank()) {
                val imageBase64 = encodeImageUriToBase64(attachment.uri)
                if (imageBase64 != null) {
                    userParts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                }
            } else if (attachment?.type == AttachedMediaType.PDF) {
                userParts.put(JSONObject().apply {
                    put("text", "\n[Context: Document attached: ${attachment.displayName} (${attachment.sizeFormatted})]")
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", userParts)
            })

            // System instruction
            val systemInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "$SYSTEM_PROMPT\nCurrent Subject Domain: $subject.")
                    })
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstructionObj)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
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

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "").trim()
                        val latency = System.currentTimeMillis() - startTime
                        if (text.isNotEmpty()) {
                            val cleaned = cleanModelOutput(text)
                            val dynamicSuggestions = generateFollowUpSuggestions(prompt, subject, cleaned)
                            return TheStudentAiStudyResponse.Success(cleaned, dynamicSuggestions, latency)
                        }
                    }
                }
            }

            Log.w(TAG, "Gemini Study API error: HTTP $responseCode: $responseBody")
            return TheStudentAiStudyResponse.Error("Gemini Study API error (HTTP $responseCode)")
        } catch (e: Exception) {
            Log.w(TAG, "Gemini Study API exception: ${e.message}", e)
            return TheStudentAiStudyResponse.Error("Network error: ${e.localizedMessage ?: "timeout"}")
        }
    }

    /**
     * Stream multimodal / text query to Z.AI (GLM-4.7-Flash free model).
     * Connects via OpenAI-compatible Server-Sent Events (SSE) `/chat/completions` with native multi-turn tool calling.
     */
    private suspend fun streamZaiGlmResponse(
        prompt: String,
        attachment: AttachedMedia?,
        subject: String,
        conversationHistory: List<Pair<String, String>>,
        apiKey: String,
        onReasoningChunk: suspend (String) -> Unit,
        onReasoningComplete: suspend () -> Unit,
        onContentChunk: suspend (String) -> Unit
    ): TheStudentAiStudyResponse {
        val startTime = System.currentTimeMillis()
        val endpointUrl = "$ZAI_BASE_URL/chat/completions"

        return try {
            val messagesArray = JSONArray()

            // 1. System Prompt
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", "$SYSTEM_PROMPT\nCurrent Subject Domain: $subject.")
            })

            // 2. Multi-turn History (last 6 turns)
            conversationHistory.takeLast(6).forEach { (role, text) ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (role.equals("user", ignoreCase = true)) "user" else "assistant")
                    put("content", text)
                })
            }

            // 3. User Message with OCR or document context
            val userMessageObj = JSONObject().apply {
                put("role", "user")
                val isSinhala = prompt.any { it.code in 0x0D80..0x0DFF } ||
                                attachment?.extractedOcrText?.any { it.code in 0x0D80..0x0DFF } == true
                val isTamil = prompt.any { it.code in 0x0B80..0x0BFF } ||
                              attachment?.extractedOcrText?.any { it.code in 0x0B80..0x0BFF } == true

                val promptBuilder = StringBuilder()
                if (!attachment?.extractedOcrText.isNullOrBlank()) {
                    promptBuilder.append("[Extracted Text from Image/Notes]:\n${attachment!!.extractedOcrText}\n\n")
                }
                if (prompt.isNotBlank()) {
                    promptBuilder.append(prompt).append("\n\n")
                } else {
                    promptBuilder.append("Help me solve this study problem step-by-step with clear explanations and formulas.\n\n")
                }
                if (isSinhala) {
                    promptBuilder.append("[MANDATORY LANGUAGE]: කරුණාකර සම්පූර්ණ පිළිතුර සහ පැහැදිලි කිරීම පැහැදිලි සිංහලෙන් (Sinhala) ලබා දෙන්න.")
                } else if (isTamil) {
                    promptBuilder.append("[MANDATORY LANGUAGE]: தயவுசெய்து உங்கள் முழு விளக்கத்தையும் தமிழில் (Tamil) வழங்கவும்.")
                }
                put("content", promptBuilder.toString().trim())
            }
            messagesArray.put(userMessageObj)

            executeZaiStreamLoop(
                endpointUrl = endpointUrl,
                apiKey = apiKey,
                messagesArray = messagesArray,
                startTime = startTime,
                subject = subject,
                prompt = prompt,
                onReasoningChunk = onReasoningChunk,
                onReasoningComplete = onReasoningComplete,
                onContentChunk = onContentChunk
            )
        } catch (e: Exception) {
            Log.w(TAG, "Z.AI GLM call exception: ${e.message}", e)
            TheStudentAiStudyResponse.Error("Could not reach Z.AI GLM service: ${e.localizedMessage ?: "Network error"}")
        }
    }

    private fun extractTextToolCalls(content: String): List<ToolCallAccumulator> {
        val results = mutableListOf<ToolCallAccumulator>()
        if (!content.contains("<tool_call", ignoreCase = true)) return results

        val toolCallRegex = Regex("<tool_call>([\\s\\S]*?)(?:</tool_call>|$)", RegexOption.IGNORE_CASE)
        val matches = toolCallRegex.findAll(content).toList()
        matches.forEachIndexed { i, match ->
            val inner = match.groupValues[1].trim()
            if (inner.isNotBlank()) {
                val rawName = inner.substringBefore("<").trim().ifBlank {
                    if (inner.contains("url", ignoreCase = true)) "WebFetch" else "Tools_Web_Search"
                }
                val cleanName = rawName.replace(">", "").trim()
                val argsObj = JSONObject()
                val argPairRegex = Regex("<arg_key>([\\s\\S]*?)</arg_key>\\s*<arg_value>([\\s\\S]*?)</arg_value>", RegexOption.IGNORE_CASE)
                val pairs = argPairRegex.findAll(inner).toList()
                if (pairs.isNotEmpty()) {
                    for (p in pairs) {
                        val k = p.groupValues[1].trim()
                        val v = p.groupValues[2].trim()
                        argsObj.put(k, v)
                    }
                    results.add(
                        ToolCallAccumulator(
                            id = "call_text_${System.currentTimeMillis()}_$i",
                            name = cleanName,
                            arguments = StringBuilder(argsObj.toString())
                        )
                    )
                } else if (inner.startsWith("{") && inner.endsWith("}")) {
                    try {
                        val json = JSONObject(inner)
                        val n = json.optString("name", cleanName)
                        val a = json.optJSONObject("arguments")?.toString() ?: json.optString("arguments", "{}")
                        results.add(
                            ToolCallAccumulator(
                                id = "call_text_${System.currentTimeMillis()}_$i",
                                name = n,
                                arguments = StringBuilder(a)
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
        }
        return results
    }

    private suspend fun executeZaiStreamLoop(
        endpointUrl: String,
        apiKey: String,
        messagesArray: JSONArray,
        startTime: Long,
        subject: String,
        prompt: String,
        onReasoningChunk: suspend (String) -> Unit,
        onReasoningComplete: suspend () -> Unit,
        onContentChunk: suspend (String) -> Unit
    ): TheStudentAiStudyResponse {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val fullContent = StringBuilder()
        val fullReasoning = StringBuilder()
        var hasReasoningCompleted = false
        var turnCount = 0
        val maxTurns = 3
        val allExecutedToolFindings = mutableListOf<String>()
        val allDownloadTokens = mutableListOf<String>()

        while (turnCount < maxTurns) {
            turnCount++
            val accumulatedToolCalls = mutableMapOf<Int, ToolCallAccumulator>()
            val turnTextContent = StringBuilder()

            val requestJson = JSONObject().apply {
                put("model", GLM_4_7_FLASH)
                put("messages", messagesArray)
                put("temperature", 0.6)
                put("max_tokens", 8192)
                put("stream", true)
                // Tools are only provided in Turn 1!
                // Once tools are executed, in Turn 2 we don't supply tools schema so the model
                // focuses purely on generating the final answer for the student matching their requested format.
                if (turnCount == 1) {
                    put("tools", TheStudentStudyTools.getToolsSchema())
                    put("tool_choice", "auto")
                }
            }

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer $apiKey")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string().orEmpty()
                if (code == 401 || code == 403) {
                    return TheStudentAiStudyResponse.Error(
                        message = "Z.AI API Key is invalid or expired (HTTP $code). Please configure your API key in Settings.",
                        isAuthError = true
                    )
                }
                if (turnCount == 1) {
                    return TheStudentAiStudyResponse.Error("Z.AI returned HTTP $code: ${errBody.take(150)}")
                } else {
                    Log.w(TAG, "Turn $turnCount request failed with HTTP $code: $errBody. Falling back to direct synthesis.")
                    val fallbackContent = synthesizeWithPromptFallback(endpointUrl, apiKey, prompt, subject, allExecutedToolFindings, onContentChunk)
                    if (fallbackContent.isNotBlank()) {
                        fullContent.append(fallbackContent)
                    }
                    break
                }
            }

            val responseBody = response.body ?: break
            val source = responseBody.source()

            while (!source.exhausted()) {
                val rawLine = source.readUtf8Line() ?: break
                if (rawLine.isBlank()) continue

                val parts = if (rawLine.contains("data:")) {
                    rawLine.split("data:").filter { it.isNotBlank() }
                } else listOf(rawLine)

                for (part in parts) {
                    val trimmed = part.trim()
                    if (trimmed == "[DONE]") break
                    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                        try {
                            val chunkJson = JSONObject(trimmed)
                            val choices = chunkJson.optJSONArray("choices") ?: continue
                            if (choices.length() == 0) continue
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta") ?: continue

                            // 1. Live reasoning tokens (if present)
                            if (delta.has("reasoning_content") && !delta.isNull("reasoning_content")) {
                                val r = delta.optString("reasoning_content", "")
                                if (r.isNotEmpty() && r != "null") {
                                    fullReasoning.append(r)
                                    onReasoningChunk(r)
                                }
                            }

                            // 2. Standard JSON tool calls
                            val toolCallsArr = delta.optJSONArray("tool_calls")
                            if (toolCallsArr != null && toolCallsArr.length() > 0) {
                                for (i in 0 until toolCallsArr.length()) {
                                    val tcObj = toolCallsArr.getJSONObject(i)
                                    val idx = tcObj.optInt("index", i)
                                    val tcId = tcObj.optString("id", "")
                                    val funcObj = tcObj.optJSONObject("function")
                                    val fName = funcObj?.optString("name", "") ?: ""
                                    val fArgs = funcObj?.optString("arguments", "") ?: ""

                                    val acc = accumulatedToolCalls.getOrPut(idx) { ToolCallAccumulator() }
                                    if (tcId.isNotBlank()) acc.id = tcId
                                    if (fName.isNotBlank()) acc.name = fName
                                    if (fArgs.isNotBlank()) acc.arguments.append(fArgs)
                                }
                            }

                            // 3. Content tokens (pure real-time streaming to the UI)
                            if (delta.has("content") && !delta.isNull("content")) {
                                var c = delta.optString("content", "")
                                if (c != "null" && c != "<null>") {
                                    if (c.startsWith("null")) c = c.removePrefix("null").trimStart()
                                    if (c.isNotEmpty()) {
                                        turnTextContent.append(c)
                                        if (turnCount > 1) {
                                            // In Turn 2, tools are not active, so every token is the final answer!
                                            // Stream immediately to onContentChunk with zero delays!
                                            if (!hasReasoningCompleted) {
                                                hasReasoningCompleted = true
                                                onReasoningComplete()
                                            }
                                            fullContent.append(c)
                                            onContentChunk(c)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // Check if turnTextContent contains embedded <tool_call> tags
            val rawTurnText = turnTextContent.toString()
            val textCalls = extractTextToolCalls(rawTurnText)
            if (textCalls.isNotEmpty()) {
                val baseIdx = (accumulatedToolCalls.keys.maxOrNull() ?: -1) + 1
                textCalls.forEachIndexed { i, tc ->
                    accumulatedToolCalls[baseIdx + i] = tc
                }
            }

            // If tool calls were made in this turn:
            if (accumulatedToolCalls.isNotEmpty()) {
                val hasSyntheticCalls = accumulatedToolCalls.values.any { it.id.startsWith("call_text_") }
                val toolCallsJsonArray = JSONArray()
                val toolResultMessages = mutableListOf<JSONObject>()

                val downloadTokens = mutableListOf<String>()

                accumulatedToolCalls.values.forEach { tc ->
                    val callId = tc.id.ifBlank { "call_${System.currentTimeMillis()}" }
                    val toolName = tc.name
                    val toolArgs = tc.arguments.toString()

                    val q = try {
                        val a = JSONObject(toolArgs)
                        a.optString("query").ifBlank {
                            a.optString("title_or_query").ifBlank {
                                a.optString("url").ifBlank {
                                    a.optString("expression")
                                }
                            }
                        }
                    } catch (_: Exception) { "" }

                    val actionLabel = when {
                        toolName.contains("read_document", ignoreCase = true) || toolName.contains("read_pdf", ignoreCase = true) ->
                            if (q.isNotBlank()) "Reading document & extracting text from \"$q\"" else "Reading document & extracting text"
                        toolName.contains("download", ignoreCase = true) || toolName.contains("share", ignoreCase = true) ->
                            if (q.isNotBlank()) "Retrieving & preparing document: \"$q\"" else "Preparing official document download"
                        toolName.contains("Search", ignoreCase = true) ->
                            if (q.isNotBlank()) "Searching the web for \"$q\"" else "Searching the web"
                        toolName.contains("Fetch", ignoreCase = true) ->
                            if (q.isNotBlank()) "Reading webpage: $q" else "Reading webpage"
                        toolName.contains("calculator", ignoreCase = true) ->
                            if (q.isNotBlank()) "Calculating: $q" else "Calculating step-by-step"
                        else -> "Looking up educational resources"
                    }
                    val stepLog = "\n• $actionLabel...\n"
                    onReasoningChunk(stepLog)
                    fullReasoning.append(stepLog)

                    val result = TheStudentStudyTools.executeTool(toolName, toolArgs, context = context)
                    val cleanResult = cleanJargon(TheStudentStudyTools.cleanMcpSearchText(result))
                    allExecutedToolFindings.add(cleanResult)

                    if (result.contains("[DOWNLOAD_DOCUMENT:")) {
                        val token = result.substringAfter("[DOWNLOAD_DOCUMENT:").substringBefore("]")
                        allDownloadTokens.add("[DOWNLOAD_DOCUMENT:$token]")
                    }

                    val preview = if (cleanResult.length > 200) cleanResult.take(200) + "..." else cleanResult
                    val resultLog = "✔ Information retrieved: $preview\n\n"
                    onReasoningChunk(resultLog)
                    fullReasoning.append(resultLog)
                }

                // Add findings to messagesArray in standard assistant + user turns
                // This universal format is accepted seamlessly by all OpenAI / Z.AI / GLM gateways without HTTP 400 schema rejections.
                val cleanFindings = allExecutedToolFindings.joinToString("\n\n").take(3500)
                messagesArray.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", "I am retrieving the latest educational information for your inquiry.")
                })
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", "[Verified Study Findings]:\n$cleanFindings\n\nPlease now provide a comprehensive, clear educational response for the student based on these findings: \"$prompt\"")
                })

                // Continue loop to turn 2 so GLM synthesizes the findings in the user's requested format!
                continue
            }

            // No tool calls: this turn produced the final answer!
            val cleanedTurnContent = cleanModelOutput(rawTurnText)
            if (cleanedTurnContent.isNotBlank()) {
                if (!hasReasoningCompleted) {
                    hasReasoningCompleted = true
                    onReasoningComplete()
                }
                if (turnCount == 1 || fullContent.isEmpty()) {
                    fullContent.append(cleanedTurnContent)
                    onContentChunk(cleanedTurnContent)
                }
            }
            break
        }

        if (!hasReasoningCompleted) {
            hasReasoningCompleted = true
            onReasoningComplete()
        }

        var finalOutput = fullContent.toString().trim()
        val latency = System.currentTimeMillis() - startTime

        // Resilient fallback: if the model completed without emitting text, synthesize using prompt fallback or format actual findings
        if (finalOutput.isBlank() && allExecutedToolFindings.isNotEmpty()) {
            val promptFallbackAnswer = synthesizeWithPromptFallback(endpointUrl, apiKey, prompt, subject, allExecutedToolFindings, onContentChunk)
            if (promptFallbackAnswer.isNotBlank()) {
                finalOutput = promptFallbackAnswer
            } else {
                val isTableRequested = prompt.contains("table", ignoreCase = true) || prompt.contains("tabular", ignoreCase = true)
                val isPastPaperQuery = prompt.contains("past paper", ignoreCase = true) || prompt.contains("marking scheme", ignoreCase = true) || prompt.contains("paper", ignoreCase = true)
                val sb = StringBuilder()

                val rawSnippets = allExecutedToolFindings.flatMap { it.lines() }
                    .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
                    .filter {
                        it.length > 8 &&
                        !it.contains("http", ignoreCase = true) &&
                        !it.contains("searching", ignoreCase = true) &&
                        !it.contains("Current Date", ignoreCase = true) &&
                        !it.contains("Current Time", ignoreCase = true)
                    }
                    .distinct()
                    .take(12)

                if (isPastPaperQuery) {
                    val isSinhala = prompt.any { it.code in 0x0D80..0x0DFF }
                    val isTamil = prompt.any { it.code in 0x0B80..0x0BFF }
                    if (isSinhala) {
                        sb.append("ඔබ ඉල්ලූ පසුගිය විභාග ප්‍රශ්න පත්‍රය සහ ලකුණු දීමේ පටිපාටිය (Marking Scheme) සාර්ථකව සොයා ගන්නා ලදී. පහත ඇති බාගත කිරීමේ පත (Download Card) මඟින් සෘජුවම ඔබගේ දුරකථනයට PDF ගොනුව සුරැකිය හැක.\n\n")
                        sb.append("### විභාග මාර්ගෝපදේශ සහ ලකුණු බෙදීයාම:\n")
                        sb.append("• **I වන කොටස**: බහුවරණ (MCQ) ප්‍රශ්න සඳහා නියමිත කාලය තුළ පිළිතුරු සැපයීම පුහුණු වන්න.\n")
                        sb.append("• **II වන කොටස**: ව්‍යූහගත රචනා සහ රචනා ප්‍රශ්න සඳහා පියවර ලකුණු (Step Marks) නිවැරදිව ලබා ගැනීමට සූත්‍ර සහ ඒකක පැහැදිලිව සටහන් කරන්න.\n\n")
                    } else if (isTamil) {
                        sb.append("நீங்கள் கோரிய கடந்தகால வினாத்தாள் மற்றும் புள்ளி வழங்கும் திட்டம் (Marking Scheme) தயாராக உள்ளது. கீழே உள்ள அட்டை மூலம் நேரடியாக பதிவிறக்கம் செய்து கொள்ளலாம்.\n\n")
                    } else {
                        sb.append("Here is the requested past examination paper and official marking scheme prepared for download below.\n\n")
                        sb.append("### Examination Guidelines & Revision Advice:\n")
                        sb.append("• **Part I (MCQs)**: Practice under strict timed examination conditions to build accuracy and pacing.\n")
                        sb.append("• **Part II (Structured Essay & Essay)**: Pay attention to step marks, standard formulas, and exact units as required by Department of Examinations marking criteria.\n\n")
                    }
                } else if (isTableRequested) {
                    sb.append("Here is the requested information in table format:\n\n")
                    sb.append("| Category / Subject | Information & Details |\n")
                    sb.append("| :--- | :--- |\n")
                    if (rawSnippets.isNotEmpty()) {
                        rawSnippets.forEachIndexed { idx, item ->
                            val parts = item.split(":", limit = 2)
                            if (parts.size == 2 && parts[0].length < 40) {
                                sb.append("| **${parts[0].trim()}** | ${parts[1].trim()} |\n")
                            } else {
                                sb.append("| **Item ${idx + 1}** | $item |\n")
                            }
                        }
                    } else {
                        sb.append("| **Inquiry** | $prompt |\n")
                        sb.append("| **Details** | Verified information retrieved. |\n")
                    }
                } else {
                    if (rawSnippets.isNotEmpty()) {
                        rawSnippets.forEach { item ->
                            sb.append("• $item\n")
                        }
                    } else {
                        sb.append(allExecutedToolFindings.joinToString("\n\n").take(600))
                    }
                }
                finalOutput = sb.toString().trim()
                onContentChunk(finalOutput)
            }
        }

        if (allDownloadTokens.isNotEmpty() && !finalOutput.contains("[DOWNLOAD_DOCUMENT:")) {
            val singleToken = allDownloadTokens.distinct().first()
            val docBlock = "\n\n" + singleToken
            finalOutput += docBlock
            onContentChunk(docBlock)
        }

        return if (finalOutput.isNotBlank()) {
            val cleaned = cleanModelOutput(finalOutput)
            val suggestions = generateFollowUpSuggestions(prompt, subject, cleaned)
            TheStudentAiStudyResponse.Success(cleaned, suggestions, latency, modelUsed = "GLM-4.7-Flash", reasoningText = fullReasoning.toString().takeIf { it.isNotBlank() })
        } else {
            TheStudentAiStudyResponse.Error("GLM-4.7-Flash completed without generating text.")
        }
    }

    /**
     * Resilient standard multi-turn synthesis fallback when tool_calls schemas are rejected by the backend.
     * Passes the verified search findings directly as context to synthesize a complete educational response.
     */
    private suspend fun synthesizeWithPromptFallback(
        endpointUrl: String,
        apiKey: String,
        prompt: String,
        subject: String,
        findings: List<String>,
        onContentChunk: suspend (String) -> Unit
    ): String {
        return try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val findingsSummary = findings.joinToString("\n\n").take(3500)
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "$SYSTEM_PROMPT\nSubject: $subject.\nYou have access to the verified online findings below. Provide a clear, thorough, and accurate answer to the student's question based on these findings. Strictly follow any formatting, table structure, or presentation requested by the student (e.g. Markdown table, bullet points, step-by-step, etc.). Always start numbered lists sequentially from 1 (1., 2., 3.). Never skip 1. Never prefix numbered items with heading hashes (###). Never output raw code or tool tags.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "$prompt\n\n[Verified Findings]:\n$findingsSummary\n\nPlease answer my question accurately using the findings above, strictly adhering to any formatting requirements (such as a table or list) requested in my question.")
                })
            }

            val requestJson = JSONObject().apply {
                put("model", GLM_4_7_FLASH)
                put("messages", messagesArray)
                put("temperature", 0.6)
                put("max_tokens", 8192)
                put("stream", true)
            }

            val req = Request.Builder()
                .url(endpointUrl)
                .post(requestJson.toString().toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer $apiKey")
                .build()

            val resp = client.newCall(req).execute()
            val resultSb = StringBuilder()
            if (resp.isSuccessful) {
                val src = resp.body?.source()
                if (src != null) {
                    while (!src.exhausted()) {
                        val line = src.readUtf8Line() ?: break
                        if (line.isBlank()) continue
                        val parts = if (line.contains("data:")) line.split("data:").filter { it.isNotBlank() } else listOf(line)
                        for (p in parts) {
                            val tr = p.trim()
                            if (tr == "[DONE]") break
                            if (tr.startsWith("{") && tr.endsWith("}")) {
                                try {
                                    val j = JSONObject(tr)
                                    val delta = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                                    val c = delta?.optString("content", "") ?: ""
                                    if (c.isNotEmpty() && c != "null") {
                                        val cl = c.replace(Regex("<tool_call>[\\s\\S]*", RegexOption.IGNORE_CASE), "")
                                        if (cl.isNotEmpty()) {
                                            resultSb.append(cl)
                                            onContentChunk(cl)
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
            cleanModelOutput(resultSb.toString().trim())
        } catch (e: Exception) {
            Log.w(TAG, "synthesizeWithPromptFallback failed: ${e.message}", e)
            ""
        }
    }

    /**
     * Stream multimodal / text query to the dzd-learn-ai Hugging Face Space backend (GPT-OSS 20B on llama-server).
     * Connects via OpenAI-compatible Server-Sent Events (SSE) `/v1/chat/completions`.
     * Live streams reasoning tokens to [onReasoningChunk], detects completion, and streams final answer to [onContentChunk].
     */
    private suspend fun streamHuggingFaceSpaceResponse(
        prompt: String,
        attachment: AttachedMedia?,
        subject: String,
        conversationHistory: List<Pair<String, String>>,
        customToken: String?,
        customBaseUrl: String?,
        onReasoningChunk: suspend (String) -> Unit,
        onReasoningComplete: suspend () -> Unit,
        onContentChunk: suspend (String) -> Unit
    ): TheStudentAiStudyResponse {
        val startTime = System.currentTimeMillis()

        val token = when {
            !customToken.isNullOrBlank() -> customToken.trim()
            BuildConfig.HF_API_TOKEN.isNotBlank() && !BuildConfig.HF_API_TOKEN.contains("YOUR_HF_ACCESS_TOKEN_HERE") -> BuildConfig.HF_API_TOKEN.trim()
            !System.getenv("HF_API_TOKEN").isNullOrBlank() -> System.getenv("HF_API_TOKEN")!!.trim()
            else -> "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"
        }

        val targetBaseUrl = (customBaseUrl?.takeIf { it.isNotBlank() } ?: SPACE_DIRECT_URL).trimEnd('/')
        val endpointUrl = "$targetBaseUrl/v1/chat/completions"

        return try {
            val messagesArray = JSONArray()

            // 1. System Prompt
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", "$SYSTEM_PROMPT\nCurrent Subject Domain: $subject.")
            })

            // 2. Multi-turn Conversation History (take last 6 turns)
            conversationHistory.takeLast(6).forEach { (role, text) ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (role.equals("user", ignoreCase = true)) "user" else "assistant")
                    put("content", text)
                })
            }

            // 3. User Message (Pure Text for GPT-OSS 20B on llama-server)
            val userMessageObj = JSONObject()
            userMessageObj.put("role", "user")

            // If an image is attached, extract text using on-device Tesseract-OCR (100% offline, zero mmproj requirement)
            var extractedOcrText = attachment?.extractedOcrText
            if (attachment?.type == AttachedMediaType.IMAGE && extractedOcrText.isNullOrBlank() && !attachment.uri.isNullOrBlank()) {
                try {
                    val tessResult = tesseractOcrEngine.recognizeText(Uri.parse(attachment.uri), null)
                    if (tessResult.isSuccess && tessResult.text.isNotBlank()) {
                        extractedOcrText = tessResult.text
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "On-device Tesseract fallback extraction error: ${e.message}")
                }
            }

            val isSinhala = prompt.any { it.code in 0x0D80..0x0DFF } ||
                            extractedOcrText?.any { it.code in 0x0D80..0x0DFF } == true ||
                            attachment?.ocrLanguage?.contains("Sinhala", ignoreCase = true) == true

            val isTamil = prompt.any { it.code in 0x0B80..0x0BFF } ||
                          extractedOcrText?.any { it.code in 0x0B80..0x0BFF } == true ||
                          attachment?.ocrLanguage?.contains("Tamil", ignoreCase = true) == true

            val promptBuilder = StringBuilder()

            if (!extractedOcrText.isNullOrBlank()) {
                if (isSinhala) {
                    promptBuilder.append("ප්‍රශ්නය (පින්තූරයෙන් Tesseract-OCR මඟින් කියවන ලදී):\n")
                } else if (isTamil) {
                    promptBuilder.append("வினா (புகைப்படத்திலிருந்து பெறப்பட்ட உரை):\n")
                } else {
                    promptBuilder.append("Exam Question (Extracted from photo via Tesseract-OCR):\n")
                }
                promptBuilder.append(extractedOcrText).append("\n\n")
            } else if (attachment?.type == AttachedMediaType.PDF) {
                promptBuilder.append("[Attached Document: ${attachment.displayName} (${attachment.sizeFormatted})]\n\n")
            }

            if (prompt.isNotBlank()) {
                promptBuilder.append("ශිෂ්‍යයාගේ විමසුම / Student Request: ").append(prompt).append("\n\n")
            } else {
                if (isSinhala) {
                    promptBuilder.append("ඉල්ලීම: කරුණාකර මෙම විභාග ප්‍රශ්නය පියවරෙන් පියවර පැහැදිලි කර නිවැරදි පිළිතුර සහ සූත්‍ර සිංහලෙන් ලබා දෙන්න.\n\n")
                } else if (isTamil) {
                    promptBuilder.append("கோரிக்கை: தயவுசெய்து இந்த தேர்வு வினாவை படி படியாக விளக்கி தெளிவான தீர்வு வழங்கவும்.\n\n")
                } else {
                    promptBuilder.append("Request: Please solve and explain this exam question step-by-step with clear formulas and derivations.\n\n")
                }
            }

            if (isSinhala) {
                promptBuilder.append("[භාෂා උපදෙස් / MANDATORY LANGUAGE INSTRUCTION]:\nමෙම ප්‍රශ්නය සිංහල භාෂාවෙන් ඇත. කරුණාකර ඔබගේ සම්පූර්ණ පිළිතුර, පැහැදිලි කිරීම සහ පියවරෙන් පියවර විවරණය ශිෂ්‍යයාට තේරුම් ගත හැකි පරිදි පැහැදිලි සිංහලෙන් (Sinhala) ලබා දෙන්න. ගණිතමය සූත්‍ර සහ සංකේත නිවැරදිව Markdown ආකාරයෙන් දක්වන්න.")
            } else if (isTamil) {
                promptBuilder.append("[MANDATORY LANGUAGE INSTRUCTION]:\nThe above question is in Tamil. Provide your full explanation, derivations, and final answer in fluent Tamil. Keep formulas and units formatted clearly in Markdown.")
            }

            // GPT-OSS 20B on llama-server expects clean text content (NO image_url objects)
            userMessageObj.put("content", promptBuilder.toString().trim())
            messagesArray.put(userMessageObj)

            val requestJson = JSONObject().apply {
                put("messages", messagesArray)
                put("temperature", 0.7)
                put("max_tokens", 6144)
                put("stream", true)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val rawJsonString = requestJson.toString().replace("\\/", "/")
            val requestBody = rawJsonString.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")

            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            val responseCode = response.code

            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                return when (responseCode) {
                    401, 403 -> TheStudentAiStudyResponse.Error(
                        message = "Private Hugging Face Space requires a valid Access Token. Please configure your token in settings.",
                        isAuthError = true
                    )
                    502 -> TheStudentAiStudyResponse.Error(
                        message = "Hugging Face Space process restarted (HTTP 502).",
                        isSleepingOrOffline = true
                    )
                    503, 504 -> TheStudentAiStudyResponse.Error(
                        message = "Hugging Face Space is currently starting up or sleeping. Please allow a moment to warm up.",
                        isSleepingOrOffline = true
                    )
                    else -> {
                        val errorMessage = try {
                            val errJson = JSONObject(errorBody)
                            errJson.optJSONObject("error")?.optString("message")
                                ?: errJson.optString("error")
                                ?: errJson.optString("detail")
                        } catch (_: Exception) {
                            null
                        }
                        val cleanError = when {
                            !errorMessage.isNullOrBlank() -> errorMessage
                            errorBody.contains("<!DOCTYPE") || errorBody.contains("<html") -> "Server returned HTTP $responseCode (${response.message})"
                            else -> errorBody.take(200)
                        }
                        TheStudentAiStudyResponse.Error(cleanError)
                    }
                }
            }

            val responseBody = response.body ?: return TheStudentAiStudyResponse.Error("Empty stream response from model.")
            val source = responseBody.source()
            val fullContentBuilder = StringBuilder()
            val fullReasoningBuilder = StringBuilder()
            var hasReasoningCompleted = false
            var insideThinkTag = false

            while (!source.exhausted()) {
                val rawLine = source.readUtf8Line() ?: break
                if (rawLine.isBlank()) continue

                val parts = if (rawLine.contains("data:")) {
                    rawLine.split("data:").filter { it.isNotBlank() }
                } else {
                    listOf(rawLine)
                }

                for (part in parts) {
                    val trimmed = part.trim()
                    if (trimmed == "[DONE]") {
                        break
                    }
                    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                        try {
                            val chunkJson = JSONObject(trimmed)
                            val choices = chunkJson.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val delta = choice.optJSONObject("delta")
                                if (delta != null) {
                                    val reasoningChunk = if (delta.has("reasoning_content") && !delta.isNull("reasoning_content")) {
                                        val r = delta.optString("reasoning_content", "")
                                        if (r == "null" || r == "<null>") "" else r
                                    } else ""

                                    val contentChunk = if (delta.has("content") && !delta.isNull("content")) {
                                        var c = delta.optString("content", "")
                                        if (c == "null" || c == "<null>") "" else {
                                            if (c.startsWith("null")) c = c.removePrefix("null").trimStart()
                                            c
                                        }
                                    } else ""

                                    if (reasoningChunk.isNotEmpty()) {
                                        fullReasoningBuilder.append(reasoningChunk)
                                        onReasoningChunk(reasoningChunk)
                                    }

                                    if (contentChunk.isNotEmpty()) {
                                        // Support <think>...</think> tags if model embeds thoughts in content
                                        if (contentChunk.contains("<think>")) {
                                            insideThinkTag = true
                                            val afterThink = contentChunk.substringAfter("<think>")
                                            if (afterThink.contains("</think>")) {
                                                val thinkContent = afterThink.substringBefore("</think>")
                                                val remaining = afterThink.substringAfter("</think>")
                                                if (thinkContent.isNotEmpty()) {
                                                    fullReasoningBuilder.append(thinkContent)
                                                    onReasoningChunk(thinkContent)
                                                }
                                                insideThinkTag = false
                                                if (!hasReasoningCompleted) {
                                                    hasReasoningCompleted = true
                                                    onReasoningComplete()
                                                }
                                                if (remaining.isNotEmpty()) {
                                                    fullContentBuilder.append(remaining)
                                                    onContentChunk(remaining)
                                                }
                                            } else {
                                                if (afterThink.isNotEmpty()) {
                                                    fullReasoningBuilder.append(afterThink)
                                                    onReasoningChunk(afterThink)
                                                }
                                            }
                                        } else if (insideThinkTag) {
                                            if (contentChunk.contains("</think>")) {
                                                val thinkPart = contentChunk.substringBefore("</think>")
                                                val contentPart = contentChunk.substringAfter("</think>")
                                                if (thinkPart.isNotEmpty()) {
                                                    fullReasoningBuilder.append(thinkPart)
                                                    onReasoningChunk(thinkPart)
                                                }
                                                insideThinkTag = false
                                                if (!hasReasoningCompleted) {
                                                    hasReasoningCompleted = true
                                                    onReasoningComplete()
                                                }
                                                if (contentPart.isNotEmpty()) {
                                                    fullContentBuilder.append(contentPart)
                                                    onContentChunk(contentPart)
                                                }
                                            } else {
                                                fullReasoningBuilder.append(contentChunk)
                                                onReasoningChunk(contentChunk)
                                            }
                                        } else {
                                            if (!hasReasoningCompleted) {
                                                hasReasoningCompleted = true
                                                onReasoningComplete()
                                            }
                                            fullContentBuilder.append(contentChunk)
                                            onContentChunk(contentChunk)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse SSE JSON chunk: $trimmed", e)
                        }
                    }
                }
            }

            if (!hasReasoningCompleted) {
                hasReasoningCompleted = true
                onReasoningComplete()
            }

            val finalOutput = fullContentBuilder.toString().trim().ifBlank {
                fullReasoningBuilder.toString().trim()
            }

            val latency = System.currentTimeMillis() - startTime
            if (finalOutput.isNotBlank()) {
                val cleaned = cleanModelOutput(finalOutput)
                val dynamicSuggestions = generateFollowUpSuggestions(prompt, subject, cleaned)
                TheStudentAiStudyResponse.Success(cleaned, dynamicSuggestions, latency)
            } else {
                TheStudentAiStudyResponse.Error("Model returned empty response.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception calling HF space: ${e.message}", e)
            TheStudentAiStudyResponse.Error(
                message = "Could not connect to study space (${e.localizedMessage ?: "Network timeout"}).",
                isSleepingOrOffline = true
            )
        }
    }

    /**
     * Converts an image Uri to Base64 JPEG with downscaling for network efficiency.
     */
    private fun encodeImageUriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val maxDim = 1200
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scale = if (width > maxDim || height > maxDim) {
                    val max = maxOf(width, height)
                    maxDim.toFloat() / max
                } else 1.0f

                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (width * scale).toInt(),
                        (height * scale).toInt(),
                        true
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val byteArray = outputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to encode image uri: $uriString", e)
            null
        }
    }

    /**
     * Clean deepseek/qwen thinking tags, remove jargon, and retain clean educational text.
     */
    private fun cleanModelOutput(text: String): String {
        var withoutThinking = text.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        if (withoutThinking.isBlank()) {
            withoutThinking = text.replace("<think>", "").replace("</think>", "").trim()
        }
        var cleaned = withoutThinking
        while (cleaned.startsWith("null")) {
            cleaned = cleaned.removePrefix("null").trimStart()
        }
        cleaned = cleaned
            .replace(Regex("<tool_call>[\\s\\S]*?</tool_call>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<tool_call>[\\s\\S]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?tool_call>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?arg_key>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?arg_value>", RegexOption.IGNORE_CASE), "")
            .trim()

        // If the model output a raw Markdown link with a local device path (e.g. [Download Document](/storage/emulated/...)), convert it to clean document token
        val localPathRegex = Regex("""\[([^\]]+)\]\((/(?:storage|data)[^\)]+\.pdf)\)""", RegexOption.IGNORE_CASE)
        val match = localPathRegex.find(cleaned)
        if (match != null) {
            val label = match.groupValues[1]
            val filePath = match.groupValues[2]
            val fileName = java.io.File(filePath).nameWithoutExtension.replace('_', ' ')
            val docTitle = if (label.contains("Document", ignoreCase = true) || label.contains("Download", ignoreCase = true)) fileName else label
            val docJson = org.json.JSONObject().apply {
                put("title", docTitle)
                put("uri", filePath)
                put("download_url", filePath)
                put("file_size", "Download / TheStudent")
                put("medium", if (docTitle.contains("Tamil", ignoreCase = true)) "Tamil" else if (docTitle.contains("English", ignoreCase = true)) "English" else "Sinhala")
                put("pages", 14)
                put("type", "Official Document")
            }
            cleaned = cleaned.replace(match.value, "\n\n[DOWNLOAD_DOCUMENT:$docJson]\n\n")
        }

        // Clean any residual raw file path strings
        cleaned = cleaned
            .replace(Regex("""/storage/emulated/0/Android/data/[^/\s\)]+/files/documents/studypapers/"""), "Download/TheStudent/")
            .replace(Regex("""/storage/emulated/0/[^\s\)]+"""), "")
            .replace(Regex("""com\.(?:aistudio|thestudent)\.[a-zA-Z0-9_\.]*"""), "")

        // If message contains [DOWNLOAD_DOCUMENT:], deduplicate multiple cards and strip duplicate bulleted metadata from the text above the card
        if (cleaned.contains("[DOWNLOAD_DOCUMENT:")) {
            // Deduplicate: Keep only the first/richest download token if multiple are emitted
            val allTokens = Regex("""\[DOWNLOAD_DOCUMENT:([^\]]+)\]""").findAll(cleaned).toList()
            if (allTokens.size > 1) {
                var isFirst = true
                cleaned = Regex("""\[DOWNLOAD_DOCUMENT:([^\]]+)\]""").replace(cleaned) { match ->
                    if (isFirst) {
                        isFirst = false
                        match.value
                    } else {
                        ""
                    }
                }
            }

            cleaned = cleaned
                .replace(Regex("""(?m)^[•\-*+]\s*(\[DOWNLOAD_DOCUMENT:)"""), "$1")
                .replace(Regex("""(?i)(?:Here are the details for the document:?|Details of the document:?|Document Information:?)"""), "")
                .replace(Regex("""(?m)^[•\-*+]\s*\*\*(?:Document|Medium|Year|Type|Pages|Content|Key Topics)\*\*:[^\n]*\n?"""), "")
                .replace(Regex("""(?m)^[•\-*+]\s*(?:Document|Medium|Year|Type|Pages|Content|Key Topics):[^\n]*\n?"""), "")
                .replace(Regex("""(?m)^\s*Content:\s*This document contains[^\n]*\n?"""), "")
                .replace(Regex("""\n{3,}"""), "\n\n")
                .trim()
        }

        // Strip unwanted generic challenge questions
        cleaned = cleaned
            .replace(Regex("""(?i)(?:---|___|\*\*\*)\s*(?:###\s*)?(?:🧠\s*)?High-Yield Practice Challenge[\s\S]*$"""), "")
            .replace(Regex("""(?i)(?:###\s*)?(?:🧠\s*)?High-Yield Practice Challenge[\s\S]*$"""), "")
            .replace(Regex("""(?i)(?:---|___|\*\*\*)\s*(?:###\s*)?(?:🧠\s*)?Practice Challenge[\s\S]*$"""), "")
            .replace(Regex("""(?i)(?:###\s*)?(?:🧠\s*)?Practice Challenge[\s\S]*$"""), "")
            .trim()

        return cleanJargon(cleaned)
    }

    private fun cleanJargon(text: String): String {
        return text
            .replace(Regex("(?i)^[•\\-*+]?\\s*Current Date:\\s*[^\\n]*\\n?"), "")
            .replace(Regex("(?i)^[•\\-*+]?\\s*Current Time:\\s*[^\\n]*\\n?"), "")
            .replace(Regex("(?i)Current Date:\\s*[^\\n]*"), "")
            .replace(Regex("(?i)Current Time:\\s*[^\\n]*"), "")
            .replace(Regex("(?i)via\\s+DuckDuckGo\\s+MCP"), "")
            .replace(Regex("(?i)DuckDuckGo\\s+MCP"), "")
            .replace(Regex("(?i)via\\s+MCP"), "")
            .replace(Regex("(?i)DuckDuckGo"), "")
            .replace(Regex("(?i)MCP\\s+Server"), "search server")
            .replace(Regex("(?i)MCP\\s+tools?"), "tools")
            .replace(Regex("(?i)MCP"), "")
            .replace("Tools_Web_Search", "web search")
            .replace("Tools_Web_Fetch", "web reader")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    /**
     * Generate context-aware interactive follow-up prompts for student pills.
     */
    private fun generateFollowUpSuggestions(prompt: String, subject: String, answer: String): List<String> {
        val lowerPrompt = prompt.lowercase()
        val isSinhala = prompt.any { it.code in 0x0D80..0x0DFF }

        return when {
            isSinhala -> listOf(
                "තව උදාහරණයක් දෙන්න",
                "මෙය සරලව පැහැදිලි කරන්න",
                "පසුගිය විභාග ප්‍රශ්න පෙන්වන්න"
            )
            lowerPrompt.contains("photo") || lowerPrompt.contains("image") || lowerPrompt.contains("diagram") -> listOf(
                "Explain step 2 in deeper detail",
                "Give me a similar practice problem",
                "Show unit dimensional analysis"
            )
            lowerPrompt.contains("quiz") || lowerPrompt.contains("mcq") -> listOf(
                "Generate 5 harder questions",
                "Explain the wrong options",
                "Test me on another chapter"
            )
            lowerPrompt.contains("pdf") || lowerPrompt.contains("summary") -> listOf(
                "Generate 5 MCQs from this",
                "List essential formulas only",
                "Create flashcard points"
            )
            else -> listOf(
                "Explain step-by-step with formulas",
                "Can you give a real-life analogy?",
                "Give me a practice problem"
            )
        }
    }
}
