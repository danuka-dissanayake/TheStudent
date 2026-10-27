package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GoogleAuthResult {
    data class Success(val profile: UserProfile, val idToken: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
}

/**
 * Handles Google Sign-In on Android using Credential Manager and Google ID,
 * and securely verifies the resulting ID Token with the backend API.
 */
object TheStudentGoogleAuthClient {

    private const val TAG = "TheStudentGoogleAuth"

    const val GOOGLE_WEB_CLIENT_ID = "849765952531-7bnroph62m0homvnpfa3nngkmcorb4mf.apps.googleusercontent.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Triggers the modern Google Sign-In bottom sheet via Credential Manager.
     */
    suspend fun signIn(context: Context): GoogleAuthResult = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName ?: "Student"
                    val pictureUri = googleIdTokenCredential.profilePictureUri?.toString()

                    val profile = UserProfile(
                        name = displayName,
                        email = email,
                        academicLevel = "G.C.E. Advanced Level",
                        streamOrDegree = "Physical Science",
                        institution = "Sri Lanka",
                        targetExam = "G.C.E. A/L",
                        studyHoursThisWeek = 12.0f,
                        totalPapersSolved = 28,
                        streakDays = 7,
                        points = 1500,
                        isLoggedIn = true
                    )

                    return@withContext GoogleAuthResult.Success(profile, idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Failed to parse Google ID Token: ${e.message}", e)
                    return@withContext GoogleAuthResult.Error("Failed to parse Google ID Token: ${e.localizedMessage}")
                }
            } else {
                Log.w(TAG, "Unexpected credential type received: ${credential.type}")
                return@withContext GoogleAuthResult.Error("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User dismissed Google Sign-In account picker.")
            return@withContext GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            return@withContext GoogleAuthResult.Error("Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}")
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In unexpected error: ${e.message}", e)
            return@withContext GoogleAuthResult.Error("Google Sign-In error: ${e.localizedMessage ?: "Network or configuration issue"}")
        }
    }

    /**
     * Syncs the verified ID token with the self-hosted PHP / MySQL backend.
     */
    suspend fun syncWithBackend(backendBaseUrl: String, idToken: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val normalizedUrl = if (backendBaseUrl.endsWith("/")) backendBaseUrl else "$backendBaseUrl/"
        val endpoint = "${normalizedUrl}api/auth.php?action=google_login"

        val jsonPayload = JSONObject().apply {
            put("id_token", idToken)
            put("target_exam", "G.C.E. A/L")
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(bodyString)
                val userObj = json.optJSONObject("user")
                if (userObj != null) {
                    val profile = UserProfile(
                        name = userObj.optString("full_name", userObj.optString("name", "Student")),
                        email = userObj.optString("email", ""),
                        academicLevel = "G.C.E. Advanced Level",
                        streamOrDegree = "Physical Science",
                        institution = "Sri Lanka",
                        targetExam = userObj.optString("target_exam", "G.C.E. A/L"),
                        isLoggedIn = true
                    )
                    return@withContext Result.success(profile)
                }
            }
            Result.failure(Exception("Backend responded with HTTP ${response.code}: $bodyString"))
        } catch (e: Exception) {
            Log.w(TAG, "Backend sync error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
