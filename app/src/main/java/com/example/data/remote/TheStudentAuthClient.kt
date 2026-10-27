package com.example.data.remote

import android.util.Log
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AuthApiResult {
    data class Success(
        val profile: UserProfile,
        val token: String,
        val message: String
    ) : AuthApiResult()

    data class RequiresOtp(
        val email: String,
        val maskedEmail: String,
        val reason: String, // 'registration', 'unverified_account', 'ip_change'
        val message: String
    ) : AuthApiResult()

    data class Error(
        val message: String,
        val statusCode: Int = 400
    ) : AuthApiResult()
}

/**
 * Production-grade client for TheStudent DirectAdmin MySQL Auth API.
 * Handles rate limits, server-side validation, anti-brute-force lockout, and dynamic OTP verification.
 */
object TheStudentAuthClient {
    private const val TAG = "TheStudentAuthClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun normalizeUrl(backendBaseUrl: String): String {
        val trimmed = backendBaseUrl.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    /**
     * Authenticates an email and password against the remote MySQL database.
     */
    suspend fun login(
        backendBaseUrl: String,
        email: String,
        password: String
    ): AuthApiResult = withContext(Dispatchers.IO) {
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=login"
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("password", password)
        }.toString()

        executeAuthRequest(endpoint, payload)
    }

    /**
     * Registers a new student account and triggers a 6-digit email OTP.
     */
    suspend fun register(
        backendBaseUrl: String,
        fullName: String,
        email: String,
        password: String,
        targetExam: String = "G.C.E. A/L"
    ): AuthApiResult = withContext(Dispatchers.IO) {
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=register"
        val payload = JSONObject().apply {
            put("full_name", fullName.trim())
            put("email", email.trim().lowercase())
            put("password", password)
            put("target_exam", targetExam)
        }.toString()

        executeAuthRequest(endpoint, payload)
    }

    /**
     * Verifies the 6-digit OTP code against the database.
     */
    suspend fun verifyOtp(
        backendBaseUrl: String,
        email: String,
        otpCode: String
    ): AuthApiResult = withContext(Dispatchers.IO) {
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=verify_otp"
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("otp_code", otpCode.trim())
        }.toString()

        executeAuthRequest(endpoint, payload)
    }

    /**
     * Requests a new 6-digit OTP code with anti-spam rate limiting.
     */
    suspend fun resendOtp(
        backendBaseUrl: String,
        email: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=resend_otp"
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            val json = try { JSONObject(bodyString) } catch (e: Exception) { JSONObject() }
            val message = json.optString("message", "A new code has been sent.")

            if (response.isSuccessful && json.optString("status") == "success") {
                Result.success(message)
            } else {
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resending OTP: ${e.message}", e)
            Result.failure(Exception("Could not connect to authentication server. Please check your internet connection."))
        }
    }

    /**
     * Syncs Google OAuth ID Token with the server and enforces IP-change verification if needed.
     */
    suspend fun googleLogin(
        backendBaseUrl: String,
        idToken: String,
        targetExam: String = "G.C.E. A/L"
    ): AuthApiResult = withContext(Dispatchers.IO) {
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=google_login"
        val payload = JSONObject().apply {
            put("id_token", idToken)
            put("target_exam", targetExam)
        }.toString()

        executeAuthRequest(endpoint, payload)
    }

    /**
     * Verifies the active session token against the remote MySQL database.
     * If the user record was deleted or token expired, returns failure with 401 error.
     */
    suspend fun validateSession(
        backendBaseUrl: String,
        token: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Session token is empty"))
        }

        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=me&token=$encodedToken"
        val request = Request.Builder()
            .url(endpoint)
            .get()
            .header("Authorization", "Bearer $token")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val statusCode = response.code
            val bodyString = response.body?.string().orEmpty()
            val json = try { JSONObject(bodyString) } catch (e: Exception) { JSONObject() }

            if (response.isSuccessful && json.optString("status") == "success") {
                val userObj = json.optJSONObject("user")
                val isVerified = userObj?.optBoolean("is_verified", false) ?: false
                if (!isVerified) {
                    return@withContext Result.failure(Exception("Email not verified"))
                }
                val profile = parseUserProfile(userObj)
                Result.success(profile)
            } else {
                val message = json.optString("message", "Session invalid or expired (HTTP $statusCode)")
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "validateSession network error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Invalidate session token on server.
     */
    suspend fun logout(backendBaseUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext true
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val endpoint = "${normalizeUrl(backendBaseUrl)}api/auth.php?action=logout&token=$encodedToken"
        val request = Request.Builder()
            .url(endpoint)
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $token")
            .build()
        try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun executeAuthRequest(endpoint: String, jsonPayload: String): AuthApiResult {
        val request = Request.Builder()
            .url(endpoint)
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val statusCode = response.code
            val bodyString = response.body?.string().orEmpty()

            val json = try {
                JSONObject(bodyString)
            } catch (e: Exception) {
                JSONObject()
            }

            val status = json.optString("status")
            val message = json.optString("message", if (response.isSuccessful) "Success" else "Request failed ($statusCode)")

            if (status == "requires_verification") {
                return AuthApiResult.RequiresOtp(
                    email = json.optString("email"),
                    maskedEmail = json.optString("masked_email", json.optString("email")),
                    reason = json.optString("verification_reason", "unverified_account"),
                    message = message
                )
            }

            if (response.isSuccessful && status == "success") {
                val token = json.optString("token", "")
                val userObj = json.optJSONObject("user")
                val profile = parseUserProfile(userObj)
                return AuthApiResult.Success(profile, token, message)
            }

            // HTTP 429 Too Many Requests (Brute-force lockout)
            if (statusCode == 429) {
                return AuthApiResult.Error(
                    message = message.ifBlank { "Too many attempts. For your security, this action has been temporarily blocked for 15 minutes." },
                    statusCode = 429
                )
            }

            // Other errors (400, 401, 409, 500)
            return AuthApiResult.Error(message, statusCode)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "DNS resolution failed for $endpoint: ${e.message}")
            return AuthApiResult.Error("Cannot reach authentication server. Please check your internet connection or server address.")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Connection timeout to $endpoint: ${e.message}")
            return AuthApiResult.Error("Connection timed out. Server is taking too long to respond.")
        } catch (e: Exception) {
            Log.e(TAG, "Auth request error: ${e.message}", e)
            return AuthApiResult.Error(e.localizedMessage ?: "Network error during authentication.")
        }
    }

    private fun parseUserProfile(userObj: JSONObject?): UserProfile {
        if (userObj == null) {
            return UserProfile(isLoggedIn = true)
        }

        val fullName = userObj.optString("name", userObj.optString("full_name", "Student"))
        val email = userObj.optString("email", "")
        val institution = userObj.optString("institution", "Sri Lanka Education System")
        val stream = userObj.optString("stream_or_degree", "Physical Science")
        val targetExam = userObj.optString("target_exam", "G.C.E. A/L")

        return UserProfile(
            name = fullName,
            email = email,
            institution = institution,
            targetExam = targetExam,
            streamOrDegree = stream,
            academicLevel = "G.C.E. Advanced Level",
            streakDays = 1,
            studyHoursThisWeek = 2.0f,
            totalPapersSolved = 5,
            points = 250,
            isLoggedIn = true
        )
    }
}
