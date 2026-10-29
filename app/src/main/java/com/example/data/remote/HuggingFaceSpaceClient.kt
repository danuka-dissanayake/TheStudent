package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class HfApiResponse {
    data class Success(val text: String, val latencyMs: Long) : HfApiResponse()
    data class Error(val message: String, val isAuthError: Boolean = false, val isSleepingOrOffline: Boolean = false) : HfApiResponse()
}

class HuggingFaceSpaceClient {

    companion object {
        private const val TAG = "HuggingFaceSpaceClient"
        // Hugging Face direct space subdomain for danuka21/Qwen3.8-27B
        const val DEFAULT_SPACE_DIRECT_URL = "https://danuka21-qwen3-8-27b.hf.space"
        const val DEFAULT_SPACE_WEB_URL = "https://huggingface.co/spaces/danuka21/Qwen3.8-27B"
        
        const val SYSTEM_PROMPT = """You are TheStudent English Voice Tutor, an encouraging and friendly personal English language learning assistant for Sri Lankan students.
Your guidelines:
1. Speak in warm, natural, and conversational English.
2. Keep your answers concise (2 to 3 sentences max) so that spoken audio is engaging and easy to listen to.
3. Gently correct grammar or pronunciation tips if the student makes a mistake.
4. Always end with an engaging open-ended question to keep the English speaking conversation flowing naturally.
5. Do not include markdown asterisks or complex symbols that sound weird in spoken text-to-speech."""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Send a conversation message to the llama.cpp OpenAI-compatible endpoint.
     */
    suspend fun getChatCompletion(
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customToken: String? = null,
        customBaseUrl: String? = null,
        // A tutor turn needs more than the default persona: the lesson step, the
        // answer format, and a budget short enough to keep the call flowing.
        systemPrompt: String = SYSTEM_PROMPT,
        maxTokens: Int = 250,
        temperature: Double = 0.7,
        cleanForSpeech: Boolean = true
    ): HfApiResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // Priority for token: passed token > BuildConfig.HF_API_TOKEN > empty
        val token = when {
            !customToken.isNullOrBlank() -> customToken.trim()
            BuildConfig.HF_API_TOKEN.isNotBlank() && !BuildConfig.HF_API_TOKEN.contains("YOUR_HF_ACCESS_TOKEN_HERE") -> BuildConfig.HF_API_TOKEN.trim()
            else -> ""
        }

        // Try direct HF space URL first, and if failed try the web space url
        val targetBaseUrl = (customBaseUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_SPACE_DIRECT_URL).trimEnd('/')
        val endpointUrl = "$targetBaseUrl/v1/chat/completions"

        try {
            val messagesArray = JSONArray().apply {
                // System message
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // Prior turns
                conversationHistory.takeLast(6).forEach { (role, content) ->
                    put(JSONObject().apply {
                        put("role", if (role.equals("user", ignoreCase = true)) "user" else "assistant")
                        put("content", content)
                    })
                }

                // Current user message
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            val requestBodyJson = JSONObject().apply {
                put("messages", messagesArray)
                put("temperature", temperature)
                put("max_tokens", maxTokens)
                put("stream", false)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            Log.d(TAG, "Calling HuggingFace Space endpoint: $endpointUrl (token present: ${token.isNotBlank()})")

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string().orEmpty()

            Log.d(TAG, "Response code: $responseCode, length: ${responseBody.length}")

            when (responseCode) {
                in 200..299 -> {
                    val json = JSONObject(responseBody)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val messageObj = firstChoice.optJSONObject("message")
                        val content = messageObj?.optString("content").orEmpty().trim()
                        val latency = System.currentTimeMillis() - startTime
                        if (content.isNotEmpty()) {
                            // Tutor turns keep their SAY/FIX/RULE/WORD labels for the
                            // parser; everything else is cleaned for speech.
                            val cleanedText = if (cleanForSpeech) cleanTextForSpeech(content) else content
                            HfApiResponse.Success(cleanedText, latency)
                        } else {
                            HfApiResponse.Error("Model returned empty text response.")
                        }
                    } else {
                        HfApiResponse.Error("No completion choices returned by model.")
                    }
                }
                401, 403 -> {
                    HfApiResponse.Error(
                        message = "Private Hugging Face Space requires a valid Access Token. Please add your token via the settings button.",
                        isAuthError = true
                    )
                }
                503, 504 -> {
                    HfApiResponse.Error(
                        message = "Hugging Face Space is currently sleeping or restarting. Waking up the container...",
                        isSleepingOrOffline = true
                    )
                }
                else -> {
                    HfApiResponse.Error("Hugging Face API returned HTTP $responseCode: $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception calling HF space: ${e.message}", e)
            HfApiResponse.Error(
                message = "Could not reach Hugging Face Space (${e.localizedMessage ?: "Network error"}).",
                isSleepingOrOffline = true
            )
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("<think>[\\s\\S]*?</think>"), "") // Remove DeepSeek/Qwen thinking tags
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold formatting
            .replace(Regex("\\*(.*?)\\*"), "$1") // Remove italic formatting
            .replace(Regex("`([^`]+)`"), "$1") // Remove inline code
            .replace("#", "")
            .trim()
    }
}
