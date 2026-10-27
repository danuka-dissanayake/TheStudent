package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.ChatCrypto
import com.example.data.model.ChatConversationState
import com.example.data.model.ChatDirectory
import com.example.data.model.ChatHistoryBackup
import com.example.data.model.ChatKeyBackup
import com.example.data.model.ChatMessage
import com.example.data.model.ChatPeer
import com.example.data.model.ChatThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

/**
 * Result of a chat call. Network failures are kept separate from real server
 * errors so the UI can keep showing the last known data instead of blanking.
 */
sealed class ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>()
    data class Failure(
        val message: String,
        val statusCode: Int = 0,
        val isNetworkError: Boolean = statusCode == 0
    ) : ChatResult<Nothing>()

    /** Human readable error, or null on success. */
    val failureMessage: String? get() = (this as? Failure)?.message

    /**
     * Payload for a successful call.
     *
     * The cast is unchecked but always correct: [Success] is only ever built
     * with a value of the declared type argument.
     */
    @Suppress("UNCHECKED_CAST")
    fun dataOrNull(): T? {
        val success = this as? Success<*> ?: return null
        return success.data as T
    }
}

/**
 * Client for `/api/chat.php`.
 *
 * Contract
 * --------
 * • Bearer token only (never a query string), same session as `auth.php`.
 * • Message bodies are **encrypted on the device** before upload (see
 *   [ChatCrypto]); the server and the database only ever see ciphertext.
 * • Only full names + avatars come back for other students — no e-mail, phone
 *   or school.
 * • Every method is safe to call from `Dispatchers.IO` and never throws.
 */
object TheStudentChatClient {

    private const val TAG = "TheStudentChatClient"
    private const val DEFAULT_BASE_URL = "https://app.quizontal.site/"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun endpoint(baseUrl: String, action: String): String {
        val base = baseUrl.trim().ifEmpty { DEFAULT_BASE_URL }.let {
            if (it.endsWith("/")) it else "$it/"
        }
        return "${base}api/chat.php?action=$action"
    }

    // -----------------------------------------------------------------------
    // Encryption identity
    // -----------------------------------------------------------------------

    /**
     * Make sure this device has a chat identity and that the server knows its
     * public half. The identity is created (and sealed in the Keystore) on the
     * first call.
     *
     * @param passwordForBackup when supplied, an extra copy of the private key
     *   wrapped with this password is uploaded, so the history can be restored
     *   on a new phone.
     */
    suspend fun publishIdentity(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        passwordForBackup: String? = null,
        /** Replace a key the server already published (only after a history wipe). */
        allowReplace: Boolean = false
    ): ChatResult<String> = withContext(Dispatchers.IO) {
        val identity = ChatCrypto.ensureIdentity(context, accountKey)
        if (identity.publicKeyBase64.isBlank()) {
            return@withContext ChatResult.Failure("Secure chat could not start on this device.")
        }

        val payload = JSONObject().put("public_key", identity.publicKeyBase64)
        if (allowReplace) payload.put("allow_replace", 1)
        if (!passwordForBackup.isNullOrBlank()) {
            val backup = ChatCrypto.exportBackup(context, accountKey, passwordForBackup)
            if (backup != null) {
                payload.put("key_backup", backup.wrappedPrivateKeyBase64)
                payload.put("key_salt", backup.saltBase64)
            }
        }

        post(backendBaseUrl, token, "set_key", payload) { identity.publicKeyBase64 }
    }

    /** Fetch the password-wrapped private key so a new device can decrypt history. */
    suspend fun unlockKeyBackup(
        backendBaseUrl: String,
        token: String,
        password: String
    ): ChatResult<ChatKeyBackup> = withContext(Dispatchers.IO) {
        post(backendBaseUrl, token, "unlock_key_backup", JSONObject().put("password", password)) { json ->
            ChatKeyBackup(
                wrappedPrivateKeyBase64 = json.optString("key_backup"),
                saltBase64 = json.optString("key_salt"),
                publicKey = json.optString("public_key").takeIf { it.isNotBlank() }
            )
        }
    }

    // -----------------------------------------------------------------------
    // Directory + presence
    // -----------------------------------------------------------------------

    suspend fun loadDirectory(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String
    ): ChatResult<ChatDirectory> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "threads") { json ->
            val arr = json.optJSONArray("threads") ?: JSONArray()
            val threads = ArrayList<ChatThread>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                threads += parseThread(context, accountKey, obj)
            }
            val backup = json.optJSONObject("history_backup")
            ChatDirectory(
                threads = threads,
                onlineCount = json.optInt("online_count", 0),
                totalStudents = json.optInt("total_students", threads.size),
                safetyAcknowledged = json.optBoolean("safety_acknowledged", false),
                serverEpoch = json.optLong("server_epoch", 0L),
                historyBackup = ChatHistoryBackup(
                    available = backup?.optBoolean("available", false) ?: false,
                    messageCount = backup?.optInt("message_count", 0) ?: 0,
                    threadCount = backup?.optInt("thread_count", 0) ?: 0,
                    newestEpoch = backup?.optLong("newest_epoch", 0L) ?: 0L
                ),
                myPublicKey = json.optString("my_public_key").takeIf { it.isNotBlank() },
                keyBackupAvailable = json.optBoolean("key_backup_available", false)
            )
        }
    }

    suspend fun heartbeat(backendBaseUrl: String, token: String): ChatResult<Long> =
        withContext(Dispatchers.IO) {
            post(backendBaseUrl, token, "heartbeat", JSONObject()) { json ->
                json.optLong("online_count", 0L)
            }
        }

    // -----------------------------------------------------------------------
    // Conversation
    // -----------------------------------------------------------------------

    /**
     * @param afterId only return messages newer than this id (incremental poll);
     *   pass 0 for the initial history load.
     */
    suspend fun loadMessages(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        afterId: Long = 0L
    ): ChatResult<ChatConversationState> = withContext(Dispatchers.IO) {
        try {
            val url = endpoint(backendBaseUrl, "messages") +
                "&peer_id=$peerId" + if (afterId > 0) "&after_id=$afterId" else ""
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()
            execute(request) { json ->
                val peerObj = json.optJSONObject("peer") ?: JSONObject()
                val peer = parsePeer(peerObj).copy(
                    isTyping = json.optBoolean("peer_is_typing", false)
                )
                val key = conversationKey(context, accountKey, peer)
                val arr = json.optJSONArray("messages") ?: JSONArray()
                val messages = ArrayList<ChatMessage>(arr.length())
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    messages += parseMessage(obj, key)
                }
                ChatConversationState(
                    peer = peer,
                    messages = messages,
                    safetyAcknowledged = json.optBoolean("safety_acknowledged", false),
                    serverEpoch = json.optLong("server_epoch", 0L)
                )
            }
        } catch (e: Exception) {
            ChatResult.Failure(networkMessage(e))
        }
    }

    /**
     * Encrypt [body] for [peer] and send it. Fails when the peer has not
     * published an encryption key yet — we never fall back to plain text.
     */
    suspend fun sendMessage(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        peer: ChatPeer,
        body: String
    ): ChatResult<ChatMessage> = withContext(Dispatchers.IO) {
        val key = conversationKey(context, accountKey, peer)
        if (key == null) {
            return@withContext ChatResult.Failure(
                if (peer.canReceiveEncrypted) {
                    "Secure chat is still starting on this device. Please try again."
                } else {
                    "${peer.name} hasn't opened secure chat yet, so encrypted messages " +
                        "can't be delivered to them."
                }
            )
        }
        val cipher = ChatCrypto.encrypt(key, body)
            ?: return@withContext ChatResult.Failure("This message could not be encrypted.")

        post(
            backendBaseUrl,
            token,
            "send",
            JSONObject().put("peer_id", peer.id).put("body", cipher)
        ) { json ->
            // The server echoes the ciphertext back; decrypt it so the bubble is
            // built from exactly what the recipient will read.
            val obj = json.optJSONObject("message") ?: JSONObject()
            parseMessage(obj, key).copy(body = body)
        }
    }

    /** Fire-and-forget typing marker; failures are silent by design. */
    suspend fun setTyping(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        isTyping: Boolean
    ) {
        withContext(Dispatchers.IO) {
            post(
                backendBaseUrl,
                token,
                "typing",
                JSONObject().put("peer_id", peerId).put("is_typing", isTyping)
            ) { }
        }
    }

    suspend fun markRead(backendBaseUrl: String, token: String, peerId: Int) {
        withContext(Dispatchers.IO) {
            post(backendBaseUrl, token, "read", JSONObject().put("peer_id", peerId)) { }
        }
    }

    suspend fun clearConversation(
        backendBaseUrl: String,
        token: String,
        peerId: Int
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(backendBaseUrl, token, "clear", JSONObject().put("peer_id", peerId)) { }
    }

    /** "Skip" on the restore card: delete my history from the database. */
    suspend fun discardHistory(backendBaseUrl: String, token: String): ChatResult<Int> =
        withContext(Dispatchers.IO) {
            post(backendBaseUrl, token, "discard_history", JSONObject()) { json ->
                json.optInt("deleted", 0)
            }
        }

    suspend fun acknowledgeSafetyNotice(
        backendBaseUrl: String,
        token: String
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(backendBaseUrl, token, "ack_safety", JSONObject()) { }
    }

    // -----------------------------------------------------------------------
    // Plumbing
    // -----------------------------------------------------------------------

    private fun conversationKey(context: Context, accountKey: String, peer: ChatPeer): SecretKey? {
        val peerKey = peer.publicKey
        if (peerKey.isNullOrBlank()) return null
        return ChatCrypto.conversationKey(context, accountKey, peer.id, peerKey)
    }

    private fun <T> get(
        backendBaseUrl: String,
        token: String,
        action: String,
        parse: (JSONObject) -> T
    ): ChatResult<T> {
        return try {
            val request = Request.Builder()
                .url(endpoint(backendBaseUrl, action))
                .get()
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()
            execute(request, parse)
        } catch (e: Exception) {
            ChatResult.Failure(networkMessage(e))
        }
    }

    private fun <T> post(
        backendBaseUrl: String,
        token: String,
        action: String,
        payload: JSONObject,
        parse: (JSONObject) -> T
    ): ChatResult<T> {
        return try {
            val request = Request.Builder()
                .url(endpoint(backendBaseUrl, action))
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()
            execute(request, parse)
        } catch (e: Exception) {
            ChatResult.Failure(networkMessage(e))
        }
    }

    private fun <T> execute(request: Request, parse: (JSONObject) -> T): ChatResult<T> {
        return try {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (json == null) {
                    Log.e(TAG, "Non-JSON chat response: HTTP ${response.code} $raw")
                    return ChatResult.Failure(
                        if (response.isSuccessful) {
                            "Unexpected response from the server."
                        } else {
                            "The server could not process that request (HTTP ${response.code})."
                        },
                        response.code
                    )
                }
                val ok = response.isSuccessful && json.optString("status") == "success"
                if (!ok) {
                    val message = json.optString("message").ifBlank { "Request failed (${response.code})" }
                    return ChatResult.Failure(message, response.code)
                }
                ChatResult.Success(parse(json))
            }
        } catch (e: Exception) {
            ChatResult.Failure(networkMessage(e))
        }
    }

    private fun networkMessage(e: Exception): String = when (e) {
        is IOException -> "You're offline. Check your connection and try again."
        else -> "Could not reach the chat server. Please try again."
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    private fun parsePeer(obj: JSONObject): ChatPeer {
        val lastSeen = if (obj.isNull("last_seen_seconds")) null else obj.optLong("last_seen_seconds", 0L)
        return ChatPeer(
            id = obj.optInt("id", 0),
            name = obj.optString("name").ifBlank { "Student" },
            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() && !obj.isNull("avatar_url") },
            isOnline = obj.optBoolean("is_online", false),
            lastSeenSeconds = lastSeen,
            publicKey = obj.optString("public_key").takeIf { it.isNotBlank() && !obj.isNull("public_key") }
        )
    }

    private fun parseMessage(obj: JSONObject, key: SecretKey?): ChatMessage {
        val raw = obj.optString("body")
        return ChatMessage(
            id = obj.optLong("id", 0L),
            senderId = obj.optInt("sender_id", 0),
            body = ChatCrypto.decrypt(key, raw),
            isMine = obj.optBoolean("is_mine", false),
            sentAtEpoch = obj.optLong("sent_epoch", 0L),
            isRead = obj.optBoolean("is_read", false)
        )
    }

    private fun parseThread(context: Context, accountKey: String, obj: JSONObject): ChatThread {
        val peer = parsePeer(obj.optJSONObject("peer") ?: JSONObject()).copy(
            isTyping = obj.optBoolean("is_typing", false)
        )
        val last = obj.optJSONObject("last_message")
        val preview = last?.optString("body")?.takeIf { it.isNotBlank() }?.let { raw ->
            ChatCrypto.decrypt(conversationKey(context, accountKey, peer), raw)
        }
        return ChatThread(
            peer = peer,
            lastMessage = preview,
            lastMessageIsMine = last?.optBoolean("is_mine", false) ?: false,
            lastMessageEpoch = last?.optLong("sent_epoch", 0L) ?: 0L,
            unreadCount = obj.optInt("unread_count", 0)
        )
    }
}
