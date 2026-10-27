package com.example.data.remote

import android.util.Log
import com.example.data.model.ChatConversationState
import com.example.data.model.ChatDirectory
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
 * • Every method is safe to call from `Dispatchers.IO` and never throws.
 * • Only full names + avatars come back — no e-mail, phone or school.
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
    // Directory + presence
    // -----------------------------------------------------------------------

    suspend fun loadDirectory(backendBaseUrl: String, token: String): ChatResult<ChatDirectory> =
        withContext(Dispatchers.IO) {
            get(backendBaseUrl, token, "threads") { json ->
                val arr = json.optJSONArray("threads") ?: JSONArray()
                val threads = ArrayList<ChatThread>(arr.length())
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    threads += parseThread(obj)
                }
                ChatDirectory(
                    threads = threads,
                    onlineCount = json.optInt("online_count", 0),
                    totalStudents = json.optInt("total_students", threads.size),
                    safetyAcknowledged = json.optBoolean("safety_acknowledged", false),
                    serverEpoch = json.optLong("server_epoch", 0L)
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
                val arr = json.optJSONArray("messages") ?: JSONArray()
                val messages = ArrayList<ChatMessage>(arr.length())
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    messages += parseMessage(obj)
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

    suspend fun sendMessage(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        body: String
    ): ChatResult<ChatMessage> = withContext(Dispatchers.IO) {
        post(
            backendBaseUrl,
            token,
            "send",
            JSONObject().put("peer_id", peerId).put("body", body)
        ) { json ->
            parseMessage(json.optJSONObject("message") ?: JSONObject())
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

    suspend fun acknowledgeSafetyNotice(
        backendBaseUrl: String,
        token: String
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(backendBaseUrl, token, "ack_safety", JSONObject()) { }
    }

    // -----------------------------------------------------------------------
    // Plumbing
    // -----------------------------------------------------------------------

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
            lastSeenSeconds = lastSeen
        )
    }

    private fun parseMessage(obj: JSONObject): ChatMessage = ChatMessage(
        id = obj.optLong("id", 0L),
        senderId = obj.optInt("sender_id", 0),
        body = obj.optString("body"),
        isMine = obj.optBoolean("is_mine", false),
        sentAtEpoch = obj.optLong("sent_epoch", 0L),
        isRead = obj.optBoolean("is_read", false)
    )

    private fun parseThread(obj: JSONObject): ChatThread {
        val peer = parsePeer(obj.optJSONObject("peer") ?: JSONObject()).copy(
            isTyping = obj.optBoolean("is_typing", false)
        )
        val last = obj.optJSONObject("last_message")
        return ChatThread(
            peer = peer,
            lastMessage = last?.optString("body")?.takeIf { it.isNotBlank() },
            lastMessageIsMine = last?.optBoolean("is_mine", false) ?: false,
            lastMessageEpoch = last?.optLong("sent_epoch", 0L) ?: 0L,
            unreadCount = obj.optInt("unread_count", 0)
        )
    }
}
