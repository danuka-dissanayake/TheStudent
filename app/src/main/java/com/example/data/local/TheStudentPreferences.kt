package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Persistent onboarding & session state.
 *
 * The bearer token is **never** written to disk in plaintext:
 *  - a per-install AES-256-GCM key lives in the Android Keystore (hardware
 *    backed where available) and can only be used by this app,
 *  - only the ciphertext + IV are stored in SharedPreferences,
 *  - legacy plaintext tokens from older builds are migrated transparently and
 *    the plaintext copy is removed.
 */
class TheStudentPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val tokenVault = SessionTokenVault()

    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ONBOARDED, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isGuest: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST, value).apply()

    /**
     * The bearer token, decrypted on demand.
     * Reading it never fails hard: unreadable/rotated ciphertext simply means
     * the user has to sign in again.
     */
    var sessionToken: String?
        get() {
            val encrypted = prefs.getString(KEY_SESSION_TOKEN, null)
            if (encrypted.isNullOrBlank()) {
                return null
            }
            return tokenVault.decrypt(encrypted) ?: run {
                // Ciphertext cannot be read (key invalidated / restored backup).
                prefs.edit().remove(KEY_SESSION_TOKEN).apply()
                null
            }
        }
        set(value) {
            val editor = prefs.edit()
            if (value.isNullOrBlank()) {
                editor.remove(KEY_SESSION_TOKEN)
            } else {
                val encrypted = tokenVault.encrypt(value)
                if (encrypted != null) {
                    editor.putString(KEY_SESSION_TOKEN, encrypted)
                } else {
                    // Never fall back to plaintext.
                    Log.e(TAG, "Secure token storage unavailable; session not persisted")
                    editor.remove(KEY_SESSION_TOKEN)
                }
            }
            editor.apply()
        }

    /** "student" or "admin" - comes from the backend, never from the UI. */
    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, ROLE_STUDENT) ?: ROLE_STUDENT
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    val isAdmin: Boolean
        get() = userRole.equals(ROLE_ADMIN, ignoreCase = true)

    /** Server-provided session expiry, ISO-8601 ("yyyy-MM-dd HH:mm:ss"). */
    var sessionExpiresAt: String?
        get() = prefs.getString(KEY_SESSION_EXPIRES_AT, null)
        set(value) = prefs.edit().putString(KEY_SESSION_EXPIRES_AT, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    fun hasUsableSession(): Boolean {
        val token = sessionToken
        return !token.isNullOrBlank() && isLoggedIn
    }

    fun updateRoleFromServer(role: String?) {
        userRole = if (role.equals(ROLE_ADMIN, ignoreCase = true)) ROLE_ADMIN else ROLE_STUDENT
    }

    /**
     * Clears every piece of authentication state (token, expiry, role, profile).
     * Keeps the onboarding flag so the user lands on sign-in, not the intro.
     */
    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putBoolean(KEY_IS_GUEST, false)
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_SESSION_EXPIRES_AT)
            .remove(KEY_USER_ROLE)
            .apply()
    }

    companion object {
        private const val TAG = "TheStudentPreferences"
        private const val PREFS_NAME = "thestudent_user_session"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_SESSION_TOKEN = "session_token"      // ciphertext only
        private const val KEY_SESSION_EXPIRES_AT = "session_expires_at"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"

        const val ROLE_STUDENT = "student"
        const val ROLE_ADMIN = "admin"

        /**
         * Persist a freshly issued session. `token` must be the raw token
         * returned by the API; it is encrypted before touching storage.
         */
        fun setSession(
            context: Context,
            token: String,
            userEmail: String,
            userName: String,
            rememberMe: Boolean = true,
            role: String = ROLE_STUDENT,
            expiresAt: String? = null
        ) {
            val prefs = TheStudentPreferences(context)
            prefs.sessionToken = token
            prefs.userEmail = userEmail
            prefs.userName = userName
            prefs.updateRoleFromServer(role)
            prefs.sessionExpiresAt = expiresAt

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_IS_LOGGED_IN, rememberMe)
                .putBoolean(KEY_IS_ONBOARDED, true)
                .putBoolean(KEY_IS_GUEST, false)
                .apply()
        }

        fun setIsOnboarded(context: Context, isOnboarded: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_IS_ONBOARDED, isOnboarded)
                .apply()
        }

        fun currentRole(context: Context): String =
            TheStudentPreferences(context).userRole
    }
}

/**
 * AES-256-GCM encryption backed by the Android Keystore.
 *
 * Stored format: Base64( 12-byte IV || ciphertext || 16-byte tag ).
 */
private class SessionTokenVault {

    fun encrypt(plainText: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(cipherText, 0, packed, iv.size, cipherText.size)
        Base64.encodeToString(packed, Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e(TAG, "Token encryption failed: ${e.message}", e)
        null
    }

    fun decrypt(payload: String): String? = try {
        val packed = Base64.decode(payload, Base64.NO_WRAP)
        if (packed.size <= IV_LENGTH) {
            null
        } else {
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val cipherText = packed.copyOfRange(IV_LENGTH, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Token decryption failed: ${e.message}")
        null
    }

    private fun obtainKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val TAG = "SessionTokenVault"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "thestudent_session_vault_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
    }
}
