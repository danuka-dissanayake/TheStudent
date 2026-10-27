package com.example.data.remote

import android.util.Log
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Result of an authentication call that may end in a session.
 */
sealed class AuthApiResult {
    data class Success(
        val profile: UserProfile,
        val token: String,
        val message: String,
        val role: String = "student",
        val expiresAt: String? = null
    ) : AuthApiResult()

    /** Email verification required (registration, unverified account or new IP). */
    data class RequiresOtp(
        val email: String,
        val maskedEmail: String,
        val reason: String,
        val message: String,
        val resendCooldownSeconds: Int = 60
    ) : AuthApiResult()

    /** OTP verified for a password reset; a new password must be chosen. */
    data class ResetRequired(
        val resetToken: String,
        val message: String,
        val expiresInSeconds: Int = 900
    ) : AuthApiResult()

    data class Error(
        val message: String,
        val statusCode: Int = 400,
        val isNetworkError: Boolean = false,
        val retryAfterSeconds: Int? = null
    ) : AuthApiResult() {
        /** True when the stored session must be discarded (401/403). */
        val invalidatesSession: Boolean
            get() = !isNetworkError && (statusCode == 401 || statusCode == 403)
    }
}

/**
 * Outcome for calls that do not create a session (resend code, password reset…).
 */
sealed class ApiOutcome {
    data class Success(val message: String, val cooldownSeconds: Int = 60) : ApiOutcome()
    data class Failure(
        val message: String,
        val statusCode: Int = 400,
        val isNetworkError: Boolean = false,
        val retryAfterSeconds: Int? = null
    ) : ApiOutcome()
}

/**
 * Result of validating a stored session on app start.
 * Network problems are deliberately separated from real invalidation so a
 * flaky connection can never sign the user out.
 */
sealed class SessionValidation {
    data class Valid(
        val profile: UserProfile,
        val role: String,
        val expiresAt: String?
    ) : SessionValidation()

    data class Invalid(val message: String, val statusCode: Int) : SessionValidation()

    data class Unreachable(val message: String) : SessionValidation()
}

/**
 * Client for the TheStudent authentication API (`/api/auth.php`).
 *
 * Contract notes
 * --------------
 * • All requests/responses are JSON; the bearer token travels ONLY in the
 *   `Authorization` header (never in a query string).
 * • Tokens are opaque 64-hex strings; the server stores only their digest.
 * • Every method is safe to call from `Dispatchers.IO` and never throws.
 */
object TheStudentAuthClient {

    private const val TAG = "TheStudentAuthClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun normalizeUrl(backendBaseUrl: String): String {
        val trimmed = backendBaseUrl.trim().ifEmpty { DEFAULT_BASE_URL }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private const val DEFAULT_BASE_URL = "https://app.quizontal.site/"

    private fun endpoint(baseUrl: String, action: String): String =
        "${normalizeUrl(baseUrl)}api/auth.php?action=$action"

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    suspend fun register(
        backendBaseUrl: String,
        fullName: String,
        email: String,
        password: String,
        targetExam: String = "G.C.E. A/L"
    ): AuthApiResult = withContext(Dispatchers.IO) {
        postAuth(
            endpoint(backendBaseUrl, "register"),
            JSONObject().apply {
                put("full_name", fullName.trim())
                put("email", email.trim().lowercase())
                put("password", password)
                put("target_exam", targetExam.trim())
            }
        )
    }

    suspend fun login(
        backendBaseUrl: String,
        email: String,
        password: String
    ): AuthApiResult = withContext(Dispatchers.IO) {
        postAuth(
            endpoint(backendBaseUrl, "login"),
            JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
            }
        )
    }

    suspend fun verifyOtp(
        backendBaseUrl: String,
        email: String,
        otpCode: String
    ): AuthApiResult = withContext(Dispatchers.IO) {
        postAuth(
            endpoint(backendBaseUrl, "verify_otp"),
            JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp_code", otpCode.filter { it.isDigit() })
            }
        )
    }

    suspend fun resendOtp(
        backendBaseUrl: String,
        email: String
    ): ApiOutcome = withContext(Dispatchers.IO) {
        postOutcome(
            endpoint(backendBaseUrl, "resend_otp"),
            JSONObject().apply { put("email", email.trim().lowercase()) }
        )
    }

    /** Always returns a generic success on the server (no account enumeration). */
    suspend fun requestPasswordReset(
        backendBaseUrl: String,
        email: String
    ): ApiOutcome = withContext(Dispatchers.IO) {
        postOutcome(
            endpoint(backendBaseUrl, "request_password_reset"),
            JSONObject().apply { put("email", email.trim().lowercase()) }
        )
    }

    suspend fun resetPassword(
        backendBaseUrl: String,
        resetToken: String,
        newPassword: String
    ): ApiOutcome = withContext(Dispatchers.IO) {
        postOutcome(
            endpoint(backendBaseUrl, "reset_password"),
            JSONObject().apply {
                put("reset_token", resetToken)
                put("new_password", newPassword)
            }
        )
    }

    suspend fun googleLogin(
        backendBaseUrl: String,
        idToken: String,
        targetExam: String = "G.C.E. A/L"
    ): AuthApiResult = withContext(Dispatchers.IO) {
        postAuth(
            endpoint(backendBaseUrl, "google_login"),
            JSONObject().apply {
                put("id_token", idToken)
                put("target_exam", targetExam)
            }
        )
    }

    /**
     * Authenticated password change. The server revokes every other session and
     * returns a brand new token for this device.
     */
    suspend fun changePassword(
        backendBaseUrl: String,
        token: String,
        currentPassword: String,
        newPassword: String
    ): AuthApiResult = withContext(Dispatchers.IO) {
        postAuth(
            endpoint(backendBaseUrl, "change_password"),
            JSONObject().apply {
                put("current_password", currentPassword)
                put("new_password", newPassword)
            },
            bearerToken = token
        )
    }

    /**
     * Permanently delete the signed-in account and all of its data.
     *
     * The server re-checks the password for e-mail accounts (Google-only
     * accounts have none, so the valid session is the proof of ownership).
     */
    suspend fun deleteAccount(
        backendBaseUrl: String,
        token: String,
        password: String = ""
    ): ApiOutcome = withContext(Dispatchers.IO) {
        postOutcome(
            endpoint(backendBaseUrl, "delete_account"),
            JSONObject().apply { put("password", password) },
            bearerToken = token
        )
    }

    /**
     * Validate a stored session. Distinguishes "invalid" from "offline".
     */
    suspend fun validateSession(
        backendBaseUrl: String,
        token: String
    ): SessionValidation = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext SessionValidation.Invalid("Missing session token", 401)
        }

        val request = Request.Builder()
            .url(endpoint(backendBaseUrl, "me"))
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrElse { JSONObject() }

                if (response.isSuccessful && json.optString("status") == "success") {
                    val userObj = json.optJSONObject("user")
                    if (userObj == null) {
                        return@withContext SessionValidation.Invalid("Malformed profile response", 500)
                    }
                    val role = userObj.optString("role", "student")
                    SessionValidation.Valid(
                        profile = parseUserProfile(userObj),
                        role = role,
                        expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() }
                    )
                } else {
                    SessionValidation.Invalid(
                        json.optString("message", "Session expired"),
                        response.code
                    )
                }
            }
        } catch (e: UnknownHostException) {
            Log.w(TAG, "validateSession: host unreachable")
            SessionValidation.Unreachable("Cannot reach the server. Please check your connection.")
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "validateSession: timeout")
            SessionValidation.Unreachable("The server took too long to respond.")
        } catch (e: IOException) {
            Log.w(TAG, "validateSession: IO error - ${e.message}")
            SessionValidation.Unreachable("Network error. Please check your connection.")
        } catch (e: Exception) {
            Log.e(TAG, "validateSession: unexpected error - ${e.message}", e)
            SessionValidation.Unreachable("Unexpected error while restoring your session.")
        }
    }

    /** Revoke this device's session. Best effort - never blocks sign-out. */
    suspend fun logout(backendBaseUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext true
        val request = Request.Builder()
            .url(endpoint(backendBaseUrl, "logout"))
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $token")
            .build()
        try {
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "logout failed: ${e.message}")
            false
        }
    }

    /** Revoke every session for the account (all devices). */
    suspend fun logoutAll(backendBaseUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        val request = Request.Builder()
            .url(endpoint(backendBaseUrl, "logout_all"))
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $token")
            .build()
        try {
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "logoutAll failed: ${e.message}")
            false
        }
    }

    /**
     * Submit the mandatory profile-setup form.
     *
     * Sends `multipart/form-data` so the photo and the text fields are stored in
     * one atomic request. The server validates everything again (the checks in
     * the UI are only for instant feedback) and returns the refreshed profile
     * with `profileCompleted = true`.
     *
     * @param avatarJpeg Re-encoded, downscaled JPEG bytes from [AvatarPreparer].
     * @param password Only for Google-only accounts (the server refuses to set a
     *   password on an account that already has one).
     */
    suspend fun completeProfile(
        backendBaseUrl: String,
        token: String,
        fullName: String,
        phone: String,
        targetExam: String,
        institution: String,
        streamOrDegree: String,
        password: String? = null,
        avatarJpeg: ByteArray? = null
    ): AuthApiResult = withContext(Dispatchers.IO) {
        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("full_name", fullName.trim())
            .addFormDataPart("phone", phone.trim())
            .addFormDataPart("target_exam", targetExam.trim())
            .addFormDataPart("institution", institution.trim())
            .addFormDataPart("stream_or_degree", streamOrDegree.trim())

        if (!password.isNullOrEmpty()) {
            bodyBuilder.addFormDataPart("password", password)
        }
        if (avatarJpeg != null && avatarJpeg.isNotEmpty()) {
            bodyBuilder.addFormDataPart(
                "avatar",
                "avatar.jpg",
                avatarJpeg.toRequestBody("image/jpeg".toMediaType())
            )
        }

        executeAuth(
            Request.Builder()
                .url(endpoint(backendBaseUrl, "complete_profile"))
                .post(bodyBuilder.build())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
        )
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private fun postAuth(
        endpointUrl: String,
        payload: JSONObject,
        bearerToken: String? = null
    ): AuthApiResult = executeAuth(
        Request.Builder()
            .url(endpointUrl)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .apply { if (!bearerToken.isNullOrBlank()) header("Authorization", "Bearer $bearerToken") }
    )

    /**
     * Single place where an auth response is turned into [AuthApiResult].
     * Shared by the JSON endpoints and the multipart profile upload so both
     * surface server errors identically.
     */
    private fun executeAuth(builder: Request.Builder): AuthApiResult {
        return try {
            httpClient.newCall(builder.build()).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                val parsedJson = runCatching { JSONObject(bodyString) }.getOrNull()
                val json = parsedJson ?: JSONObject()
                val status = json.optString("status")

                // A non-JSON body on a failure almost always means PHP died before
                // it could answer (missing table, syntax error, memory limit).
                // Log what the server actually sent so `adb logcat` shows it, and
                // surface a readable message instead of a bare status code.
                if (parsedJson == null && !response.isSuccessful) {
                    Log.e(
                        TAG,
                        "Non-JSON error response: HTTP ${response.code} from ${response.request.url} " +
                            "body=${bodyString.take(600).ifBlank { "<empty>" }}"
                    )
                }

                val message = json.optString("message").ifBlank {
                    if (response.isSuccessful) {
                        "Success"
                    } else if (parsedJson == null) {
                        // Nothing readable came back from the server.
                        "The server could not process this request (HTTP ${response.code}). " +
                            "Please try again in a moment."
                    } else {
                        "Request failed (${response.code})"
                    }
                }
                val retryAfter = extractRetryAfter(json, response.headers["Retry-After"])

                when {
                    status == "requires_verification" -> AuthApiResult.RequiresOtp(
                        email = json.optString("email"),
                        maskedEmail = json.optString("masked_email", json.optString("email")),
                        reason = json.optString("verification_reason", "unverified_account"),
                        message = message,
                        resendCooldownSeconds = json.optInt("resend_cooldown", 60)
                    )

                    status == "reset_required" -> AuthApiResult.ResetRequired(
                        resetToken = json.optString("reset_token"),
                        message = message,
                        expiresInSeconds = json.optInt("reset_expires_in", 900)
                    )

                    response.isSuccessful && status == "success" -> {
                        val token = json.optString("token")
                        AuthApiResult.Success(
                            profile = parseUserProfile(json.optJSONObject("user")),
                            token = token,
                            message = message,
                            role = json.optJSONObject("user")?.optString("role", "student") ?: "student",
                            expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() }
                        )
                    }

                    else -> AuthApiResult.Error(
                        message = message,
                        statusCode = response.code,
                        retryAfterSeconds = retryAfter
                    )
                }
            }
        } catch (e: UnknownHostException) {
            AuthApiResult.Error(
                "Cannot reach the authentication server. Please check your connection.",
                isNetworkError = true
            )
        } catch (e: SocketTimeoutException) {
            AuthApiResult.Error(
                "The connection timed out. Please try again.",
                isNetworkError = true
            )
        } catch (e: IOException) {
            AuthApiResult.Error(
                "Network error. Please check your internet connection and try again.",
                isNetworkError = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Auth request failed: ${e.message}", e)
            AuthApiResult.Error(
                "Something went wrong while contacting the server. Please try again.",
                isNetworkError = true
            )
        }
    }

    private fun postOutcome(
        endpointUrl: String,
        payload: JSONObject,
        bearerToken: String? = null
    ): ApiOutcome {
        val builder = Request.Builder()
            .url(endpointUrl)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
        if (!bearerToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }

        return try {
            httpClient.newCall(builder.build()).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(bodyString) }.getOrElse { JSONObject() }
                val message = json.optString("message").ifBlank {
                    if (response.isSuccessful) "Done" else "Request failed (${response.code})"
                }
                val retryAfter = extractRetryAfter(json, response.headers["Retry-After"])

                if (response.isSuccessful && json.optString("status") == "success") {
                    ApiOutcome.Success(message, json.optInt("resend_cooldown", 60))
                } else {
                    ApiOutcome.Failure(
                        message = message,
                        statusCode = response.code,
                        retryAfterSeconds = retryAfter
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Request failed: ${e.message}")
            ApiOutcome.Failure(
                message = "Network error. Please check your internet connection and try again.",
                isNetworkError = true
            )
        }
    }

    private fun extractRetryAfter(json: JSONObject, header: String?): Int? {
        json.optInt("retry_after", 0).takeIf { it > 0 }?.let { return it }
        return header?.toIntOrNull()?.takeIf { it > 0 }
    }

    /**
     * Read a string, mapping "missing", JSON null and the literal "null" to "".
     */
    private fun JSONObject.textOrEmpty(key: String): String {
        if (!has(key) || isNull(key)) return ""
        val value = optString(key, "").trim()
        return if (value.isEmpty() || value.equals("null", ignoreCase = true)) "" else value
    }

    private fun parseUserProfile(userObj: JSONObject?): UserProfile {
        if (userObj == null) {
            return UserProfile(isLoggedIn = true)
        }
        // IMPORTANT: org.json's optString() renders a JSON null as the literal
        // string "null", which is how the phone field once displayed "null".
        // `textOrEmpty()` treats absent / null / "null" as an empty string so the
        // UI can show a placeholder instead.
        val displayName = userObj.textOrEmpty("name")
            .ifBlank { userObj.textOrEmpty("full_name") }
            .ifBlank { "Student" }

        return UserProfile(
            name = displayName,
            email = userObj.textOrEmpty("email"),
            academicLevel = "G.C.E. Advanced Level",
            streamOrDegree = userObj.textOrEmpty("stream_or_degree"),
            institution = userObj.textOrEmpty("institution"),
            targetExam = userObj.textOrEmpty("target_exam"),
            isLoggedIn = true,
            phone = userObj.textOrEmpty("phone"),
            avatarUrl = userObj.textOrEmpty("avatar_url")
                .ifBlank { userObj.textOrEmpty("profile_picture") },
            // Absent on older server builds -> treat as complete so a rolling
            // deploy can never trap a signed-in student on the setup screen.
            profileCompleted = if (userObj.has("profile_completed")) {
                userObj.optBoolean("profile_completed", true)
            } else {
                true
            },
            hasPassword = if (userObj.has("has_password")) {
                userObj.optBoolean("has_password", true)
            } else {
                true
            },
            authProvider = userObj.optString("auth_provider", "email")
        )
    }
}
