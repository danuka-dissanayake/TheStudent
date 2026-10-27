package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption for student-to-student chat.
 *
 * How it works
 * ------------
 * 1. Every student owns a long-term **EC P-256 identity key pair**, generated on
 *    the device. The public half is published to `api/chat.php`; the private
 *    half never leaves the phone and is sealed with an AES-256-GCM key held in
 *    the Android Keystore (hardware backed where available).
 * 2. Two students derive the **same conversation secret** with ECDH:
 *    `ECDH(myPrivate, theirPublic) == ECDH(theirPrivate, myPublic)`. The raw
 *    secret is run through SHA-256 to get an AES-256 key.
 * 3. Every message body is encrypted with AES-256-GCM (random 12-byte IV) before
 *    it is uploaded, so the MySQL database — and anyone who dumps it — only ever
 *    sees ciphertext. The server cannot read a single message.
 * 4. The identity key can be uploaded **wrapped with a key derived from the
 *    student's account password** (PBKDF2), so a re-installed app can restore
 *    the history after the password is typed once. The server stores only the
 *    wrapped blob: it cannot unwrap it without the password.
 *
 * Wire format of an encrypted body:  `e2ee:v1:` + Base64( IV || ciphertext+tag )
 * Bodies without that prefix are legacy plain text and are shown unchanged.
 */
object ChatCrypto {

    private const val TAG = "ChatCrypto"

    private const val PREFIX = "e2ee:v1:"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val SEAL_ALIAS_BASE = "thestudent_chat_identity_vault_"
    private const val PREFS_BASE = "thestudent_chat_identity_"

    private const val KEY_PUBLIC = "public_key_b64"
    private const val KEY_PRIVATE_SEALED = "private_key_sealed_b64"

    /** PBKDF2 settings for the password-protected key backup. */
    private const val PBKDF2_ITERATIONS = 120_000
    private const val BACKUP_ALG_SHA256 = "PBKDF2WithHmacSHA256"
    private const val BACKUP_ALG_SHA1 = "PBKDF2WithHmacSHA1"
    private const val BACKUP_SALT_BYTES = 16

    /** Placeholder shown when a body cannot be decrypted on this device. */
    const val UNDECRYPTABLE = "🔒 This message can't be decrypted on this device."

    private val random = SecureRandom()
    private val conversationKeys = ConcurrentHashMap<String, SecretKey>()

    data class ChatIdentity(
        val publicKeyBase64: String,
        /** True when the key pair was created by this call. */
        val isNew: Boolean
    )

    /** Wrapped key material for the server-side (password protected) backup. */
    data class KeyBackup(
        val wrappedPrivateKeyBase64: String,
        val saltBase64: String
    )

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    private fun prefs(context: Context, accountKey: String): SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFS_BASE + accountSuffix(accountKey),
            Context.MODE_PRIVATE
        )

    private fun accountSuffix(accountKey: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(accountKey.lowercase().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)

    fun publicKeyBase64(context: Context, accountKey: String): String? =
        prefs(context, accountKey).getString(KEY_PUBLIC, null)?.takeIf { it.isNotBlank() }

    fun hasPrivateKey(context: Context, accountKey: String): Boolean =
        !prefs(context, accountKey).getString(KEY_PRIVATE_SEALED, null).isNullOrBlank()

    /**
     * Return the identity public key, generating the key pair on first use.
     * Safe to call from any thread.
     */
    @Synchronized
    fun ensureIdentity(context: Context, accountKey: String): ChatIdentity {
        val store = prefs(context, accountKey)
        val existingPublic = store.getString(KEY_PUBLIC, null)
        val existingPrivate = store.getString(KEY_PRIVATE_SEALED, null)
        if (!existingPublic.isNullOrBlank() && !existingPrivate.isNullOrBlank()) {
            return ChatIdentity(existingPublic, isNew = false)
        }

        return try {
            val generator = KeyPairGenerator.getInstance("EC")
            generator.initialize(ECGenParameterSpec("secp256r1"), random)
            val keyPair = generator.generateKeyPair()

            val publicB64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
            val sealed = seal(accountSuffix(accountKey), keyPair.private.encoded)
                ?: return ChatIdentity("", true)

            store.edit()
                .putString(KEY_PUBLIC, publicB64)
                .putString(KEY_PRIVATE_SEALED, sealed)
                .apply()

            ChatIdentity(publicB64, isNew = true)
        } catch (e: Exception) {
            Log.e(TAG, "Identity generation failed: ${e.message}", e)
            ChatIdentity("", true)
        }
    }

    private fun privateKey(context: Context, accountKey: String): PrivateKey? {
        val store = prefs(context, accountKey)
        val sealed = store.getString(KEY_PRIVATE_SEALED, null) ?: return null
        val pkcs8 = unseal(accountSuffix(accountKey), sealed) ?: return null
        return try {
            KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
        } catch (e: Exception) {
            Log.e(TAG, "Private key parse failed: ${e.message}")
            null
        }
    }

    // -----------------------------------------------------------------------
    // Password-protected key backup (used to restore history on a new device)
    // -----------------------------------------------------------------------

    /** Wrap the identity private key with a password-derived key. */
    fun exportBackup(context: Context, accountKey: String, password: String): KeyBackup? {
        if (password.length < 8) return null
        val privateKey = privateKey(context, accountKey) ?: return null
        return try {
            val salt = ByteArray(BACKUP_SALT_BYTES).also { random.nextBytes(it) }
            val key = deriveKey(password, salt, BACKUP_ALG_SHA256) ?: deriveKey(password, salt, BACKUP_ALG_SHA1)
                ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(privateKey.encoded)
            val packed = iv + cipherText
            KeyBackup(
                wrappedPrivateKeyBase64 = Base64.encodeToString(packed, Base64.NO_WRAP),
                saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Backup export failed: ${e.message}", e)
            null
        }
    }

    /**
     * Unwrap a server-side backup into a usable identity.
     *
     * @param publishedPublicKey the public key the server already stores for
     *   this account (it is the pair of the wrapped private key).
     * @return true when the password unlocked the backup.
     */
    @Synchronized
    fun restoreIdentity(
        context: Context,
        accountKey: String,
        wrappedPrivateKeyBase64: String,
        saltBase64: String,
        password: String,
        publishedPublicKey: String?
    ): Boolean {
        val pkcs8 = unwrapPrivateKey(wrappedPrivateKeyBase64, saltBase64, password) ?: return false
        if (publishedPublicKey.isNullOrBlank()) return false
        return try {
            storePrivateKey(context, accountKey, pkcs8)
            prefs(context, accountKey).edit()
                .putString(KEY_PUBLIC, publishedPublicKey)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restored identity could not be stored: ${e.message}")
            false
        }
    }

    private fun unwrapPrivateKey(
        wrappedPrivateKeyBase64: String,
        saltBase64: String,
        password: String
    ): ByteArray? = try {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val packed = Base64.decode(wrappedPrivateKeyBase64, Base64.NO_WRAP)
        if (packed.size <= IV_LENGTH) {
            null
        } else {
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val cipherText = packed.copyOfRange(IV_LENGTH, packed.size)
            var result: ByteArray? = null
            for (algorithm in listOf(BACKUP_ALG_SHA256, BACKUP_ALG_SHA1)) {
                val key = deriveKey(password, salt, algorithm) ?: continue
                result = try {
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                    cipher.doFinal(cipherText)
                } catch (e: Exception) {
                    null
                }
                if (result != null) break
            }
            result
        }
    } catch (e: Exception) {
        Log.w(TAG, "Backup unwrap failed: ${e.message}")
        null
    }

    private fun storePrivateKey(context: Context, accountKey: String, pkcs8: ByteArray) {
        val store = prefs(context, accountKey)
        val sealed = seal(accountSuffix(accountKey), pkcs8)
            ?: throw IllegalStateException("Keystore unavailable")
        store.edit().putString(KEY_PRIVATE_SEALED, sealed).apply()
        conversationKeys.keys.removeAll { it.startsWith("$accountKey:") }
    }

    private fun deriveKey(password: String, salt: ByteArray, algorithm: String): SecretKey? = try {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance(algorithm)
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        SecretKeySpec(encoded, "AES")
    } catch (e: Exception) {
        null
    }

    // -----------------------------------------------------------------------
    // Message encryption
    // -----------------------------------------------------------------------

    /** Derive (and cache) the shared conversation key for one peer. */
    fun conversationKey(
        context: Context,
        accountKey: String,
        peerId: Int,
        peerPublicKeyBase64: String
    ): SecretKey? {
        if (peerPublicKeyBase64.isBlank()) return null
        val cacheKey = "$accountKey:$peerId:$peerPublicKeyBase64"
        conversationKeys[cacheKey]?.let { return it }

        val mine = privateKey(context, accountKey) ?: return null
        return try {
            val peerPublic = KeyFactory.getInstance("EC").generatePublic(
                X509EncodedKeySpec(Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP))
            )
            val agreement = KeyAgreement.getInstance("ECDH")
            agreement.init(mine)
            agreement.doPhase(peerPublic, true)
            val shared = agreement.generateSecret()
            val aesKey = SecretKeySpec(
                MessageDigest.getInstance("SHA-256").digest(shared),
                "AES"
            )
            conversationKeys[cacheKey] = aesKey
            aesKey
        } catch (e: Exception) {
            Log.e(TAG, "ECDH with peer $peerId failed: ${e.message}")
            null
        }
    }

    /** Encrypt a message body. Returns null when the peer has no usable key. */
    fun encrypt(key: SecretKey?, plaintext: String): String? {
        if (key == null) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Message encryption failed: ${e.message}")
            null
        }
    }

    /**
     * Decrypt a message body. Legacy plain-text bodies (no prefix) are returned
     * unchanged so old conversations stay readable after the upgrade.
     */
    fun decrypt(key: SecretKey?, payload: String): String {
        if (!payload.startsWith(PREFIX)) return payload
        if (key == null) return UNDECRYPTABLE
        return try {
            val packed = Base64.decode(payload.removePrefix(PREFIX), Base64.NO_WRAP)
            if (packed.size <= IV_LENGTH) {
                UNDECRYPTABLE
            } else {
                val iv = packed.copyOfRange(0, IV_LENGTH)
                val cipherText = packed.copyOfRange(IV_LENGTH, packed.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                String(cipher.doFinal(cipherText), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Message decryption failed: ${e.message}")
            UNDECRYPTABLE
        }
    }

    fun isEncrypted(payload: String): Boolean = payload.startsWith(PREFIX)

    /**
     * Drop every derived conversation key from memory. Called on sign-out so a
     * different account on the same phone cannot reuse them.
     */
    fun clearSessionCache() {
        conversationKeys.clear()
    }

    // -----------------------------------------------------------------------
    // Keystore sealing
    // -----------------------------------------------------------------------

    private fun seal(aliasSuffix: String, plain: ByteArray): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, sealKey(aliasSuffix))
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e(TAG, "Identity sealing failed: ${e.message}", e)
        null
    }

    private fun unseal(aliasSuffix: String, sealedBase64: String): ByteArray? = try {
        val packed = Base64.decode(sealedBase64, Base64.NO_WRAP)
        if (packed.size <= IV_LENGTH) {
            null
        } else {
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val cipherText = packed.copyOfRange(IV_LENGTH, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, sealKey(aliasSuffix), GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(cipherText)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Identity unsealing failed: ${e.message}")
        null
    }

    /** One Keystore AES key per account, so accounts never read each other. */
    private fun sealKey(aliasSuffix: String): SecretKey {
        val alias = SEAL_ALIAS_BASE + aliasSuffix
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
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

}
