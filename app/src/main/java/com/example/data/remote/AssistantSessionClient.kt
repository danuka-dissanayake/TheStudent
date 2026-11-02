package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to the PHP backend's assistant SSO endpoint.
 *
 * The app's session token is used ONLY as a Bearer header to this first-party
 * API — exactly like every other authenticated call in the app. It is never
 * passed to the WebView, never put in a URL and never written to disk here.
 */
class AssistantSessionClient {

    sealed class MintResult {
        data class Success(
            val code: String,
            val exchangeUrl: String,
            val subject: String,
            val expiresIn: Int
        ) : MintResult()

        data class Failure(
            val message: String,
            val isAuthError: Boolean = false
        ) : MintResult()
    }

    companion object {
        private const val TAG = "AssistantSessionClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val DEFAULT_BASE_URL = "https://app.quizontal.site/"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun endpoint(baseUrl: String, action: String): String {
        val trimmed = baseUrl.trim().ifBlank { DEFAULT_BASE_URL }
        val normalised = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        return "${normalised}api/assistant_session.php?action=$action"
    }

    /**
     * Ask our own backend for a short-lived, single-use code. The backend
     * validates the app session and remembers the PKCE challenge + state hash.
     */
    suspend fun mint(
        baseUrl: String,
        appToken: String,
        codeChallenge: String,
        state: String
    ): MintResult = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("code_challenge", codeChallenge)
                .put("code_challenge_method", "S256")
                .put("state", state)
            val request = Request.Builder()
                .url(endpoint(baseUrl, "mint"))
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $appToken")
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (json == null) {
                    Log.e(TAG, "Non-JSON mint response: HTTP ${response.code} ${raw.take(300)}")
                    return@use MintResult.Failure("The sign-in service returned an unexpected response.")
                }
                if (!response.isSuccessful || json.optString("status") != "success") {
                    return@use MintResult.Failure(
                        json.optString("message").ifBlank { "Couldn't start a secure assistant session." },
                        response.code == 401 || response.code == 403
                    )
                }
                val code = json.optString("code")
                val exchangeUrl = json.optString("exchange_url")
                val subject = json.optString("sub")
                if (code.isBlank() || exchangeUrl.isBlank() || subject.isBlank()) {
                    return@use MintResult.Failure("The sign-in service returned an incomplete response.")
                }
                return@use MintResult.Success(
                    code = code,
                    exchangeUrl = exchangeUrl,
                    subject = subject,
                    expiresIn = json.optInt("expires_in", 90)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Assistant session mint failed", e)
            MintResult.Failure("Couldn't reach the sign-in service. Check your connection.")
        }
    }

    /**
     * Best-effort server-side revocation on sign-out. Local WebView cookies are
     * cleared regardless, so a failed call never leaves this device signed in;
     * it only means a session on another device may live until it expires.
     */
    suspend fun revoke(baseUrl: String, appToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpoint(baseUrl, "revoke"))
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer $appToken")
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "Assistant revoke failed; local cookies are still cleared", e)
            false
        }
    }
}
