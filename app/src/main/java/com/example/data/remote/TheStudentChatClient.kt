package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.ChatCrypto
import com.example.data.model.ChatDirectory
import com.example.data.model.ChatEnvelope
import com.example.data.model.ChatHelpRequest
import com.example.data.model.ChatHelpState
import com.example.data.model.ChatMessage
import com.example.data.model.ChatPeer
import com.example.data.model.ChatPeerProfile
import com.example.data.model.ChatRateState
import com.example.data.model.UserExp
import com.example.data.model.ChatReceivedRating
import com.example.data.model.ChatRating
import com.example.data.model.ChatRatingResult
import com.example.data.model.ChatRatingSubject
import com.example.data.model.ChatRatingSummary
import com.example.data.model.ChatRatingTag
import com.example.data.model.ChatThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import okio.BufferedSink
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
     * True when the call never reached the server (no connection, DNS, timeout).
     *
     * Lives on the sealed type because callers only ever hold a [ChatResult], and
     * "offline" has to be told apart from "the server refused this": the first is
     * queued silently, the second is shown to the student.
     */
    val isNetworkFailure: Boolean get() = (this as? Failure)?.isNetworkError == true

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

/** A message the sender removed for both sides. */
data class ChatDeletion(
    val peerId: Int,
    /** Relay id of the message I received from them, stored locally as `srv-<id>`. */
    val serverId: Long
)

/** Everything the relay handed over in one inbox fetch (already decrypted). */
data class ChatInbox(
    val messages: List<ChatMessage> = emptyList(),
    /** peerId -> highest server id the peer has read of mine. */
    val receipts: Map<Int, Long> = emptyMap(),
    val deletions: List<ChatDeletion> = emptyList(),
    val serverEpoch: Long = 0L
)

/**
 * Client for `/api/chat.php`.
 *
 * The server is a **relay + presence board**, never an archive: messages are
 * fetched once (then deleted server-side) and stored in the device database by
 * the view model. Bodies are encrypted on the device before upload
 * ([ChatCrypto]) and decrypted when they come back.
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

    private fun endpoint(baseUrl: String, action: String, query: String? = null): String {
        val base = baseUrl.trim().ifEmpty { DEFAULT_BASE_URL }.let {
            if (it.endsWith("/")) it else "$it/"
        }
        val suffix = if (query.isNullOrBlank()) "" else "&$query"
        return "${base}api/chat.php?action=$action$suffix"
    }

    // -----------------------------------------------------------------------
    // Encryption identity
    // -----------------------------------------------------------------------

    /**
     * Make sure this device has a chat identity and that the server knows its
     * public half. The identity is created (and sealed in the Keystore) on the
     * first call.
     */
    suspend fun publishIdentity(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        /** Replace a key the server already published (only after a history wipe). */
        allowReplace: Boolean = false
    ): ChatResult<String> = withContext(Dispatchers.IO) {
        val identity = ChatCrypto.ensureIdentity(context, accountKey)
        if (identity.publicKeyBase64.isBlank()) {
            return@withContext ChatResult.Failure("Secure chat could not start on this device.")
        }

        val payload = JSONObject().put("public_key", identity.publicKeyBase64)
        if (allowReplace) payload.put("allow_replace", 1)

        post(backendBaseUrl, token, "set_key", payload) { identity.publicKeyBase64 }
    }

    /**
     * The chat API version this host is running.
     *
     * `?action=version` is deliberately public (no token needed), so the same
     * URL can be opened in a browser to confirm which file was actually
     * uploaded. The app uses it to warn once when the host is behind the build
     * instead of silently missing features.
     */
    suspend fun fetchApiVersion(
        backendBaseUrl: String,
        token: String
    ): ChatResult<String> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "version") { json -> json.optString("api_version") }
    }

    // -----------------------------------------------------------------------
    // Help requests
    // -----------------------------------------------------------------------

    /** Open a help request with [peerId]. The server de-duplicates an open one. */
    suspend fun createHelpRequest(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        subjectCode: String,
        subjectLabel: String,
        purposes: List<String>
    ): ChatResult<ChatHelpRequest?> = withContext(Dispatchers.IO) {
        val purposeArray = JSONArray()
        purposes.forEach { purposeArray.put(it) }

        val payload = JSONObject()
            .put("peer_id", peerId)
            .put("subject_code", subjectCode)
            .put("subject_label", subjectLabel)
            .put("purposes", purposeArray)

        post(backendBaseUrl, token, "help_request", payload) { json ->
            parseHelpRequest(json.optJSONObject("request"))
        }
    }

    /** Latest request between me and [peerId] plus the surrounding counts. */
    suspend fun fetchHelpState(
        backendBaseUrl: String,
        token: String,
        peerId: Int
    ): ChatResult<ChatHelpState> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "help_state", query = "peer_id=$peerId") { json ->
            ChatHelpState(
                request = parseHelpRequest(json.optJSONObject("request")),
                completedWithPeer = json.optInt("completed_with_peer", 0),
                myOpenRequests = json.optInt("my_open_requests", 0),
                purpose = json.optString("purpose"),
                purposeSubject = json.optString("purpose_subject")
            )
        }
    }

    /**
     * Declare what a conversation is about. Fire-and-forget from the UI's point
     * of view: a failure is logged, never shown, because the label is a nicety
     * and the chat itself must not be interrupted.
     */
    suspend fun setPurpose(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        purpose: String,
        subjectLabel: String
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(
            backendBaseUrl,
            token,
            "set_purpose",
            JSONObject()
                .put("peer_id", peerId)
                .put("purpose", purpose)
                .put("subject_label", subjectLabel)
        ) { }
    }

    /** accept / close / cancel — the server decides who is allowed to. */
    suspend fun updateHelpRequest(
        backendBaseUrl: String,
        token: String,
        action: String,
        requestId: Long
    ): ChatResult<ChatHelpRequest?> = withContext(Dispatchers.IO) {
        post(
            backendBaseUrl,
            token,
            action,
            JSONObject().put("request_id", requestId)
        ) { json ->
            parseHelpRequest(json.optJSONObject("request"))
        }
    }

    // -----------------------------------------------------------------------
    // Ratings
    // -----------------------------------------------------------------------

    /**
     * What can be said about [peerId] right now: which subject a closed request
     * covers, whether a rating already exists, and how long it stays editable.
     */
    suspend fun fetchRateState(
        backendBaseUrl: String,
        token: String,
        peerId: Int
    ): ChatResult<ChatRateState> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "rate_state", query = "peer_id=$peerId") { json ->
            ChatRateState(
                peerId = json.optInt("peer_id", peerId),
                kind = json.optString("kind", "talk"),
                subjectCode = json.optString("subject_code"),
                subjectLabel = json.optString("subject_label"),
                canRateNow = json.optBoolean("can_rate_now", false),
                talkMinMessages = json.optInt("talk_min_messages", 20),
                maxTags = json.optInt("max_tags", 3),
                maxNote = json.optInt("max_note", 40),
                existing = parseRating(json.optJSONObject("existing"))
            )
        }
    }

    /**
     * Send (or correct) a rating. [talkMessagesEachWay] is only used when no
     * help request was ever closed: the server cannot see messages, so the
     * phone attests how much has actually been said in both directions.
     */
    suspend fun submitRating(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        stars: Int,
        tags: List<String>,
        note: String,
        talkMessagesEachWay: Int
    ): ChatResult<ChatRatingResult> = withContext(Dispatchers.IO) {
        val tagArray = JSONArray()
        tags.forEach { tagArray.put(it) }

        val payload = JSONObject()
            .put("peer_id", peerId)
            .put("stars", stars)
            .put("tags", tagArray)
            .put("note", note)
            .put("talk_messages_each_way", talkMessagesEachWay)

        post(backendBaseUrl, token, "rate", payload) { json ->
            ChatRatingResult(
                rating = parseRating(json.optJSONObject("rating")),
                summary = parseRatingSummary(json.optJSONObject("summary"))
            )
        }
    }

    /**
     * The student's own EXP: total, today's allowance, tier and the newest awards.
     * The server owns the arithmetic; this only carries it.
     */
    suspend fun fetchExpState(
        backendBaseUrl: String,
        token: String
    ): ChatResult<UserExp> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "exp_state") { json ->
            UserExp.from(json.optJSONObject("exp"))
        }
    }

    /**
     * The newest rating I have received, or null when nobody has rated me yet.
     * Used only to decide whether the celebration should play once.
     */
    suspend fun fetchLatestReceivedRating(
        backendBaseUrl: String,
        token: String
    ): ChatResult<ChatReceivedRating?> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "my_ratings") { json ->
            val latest = json.optJSONObject("latest") ?: return@get null
            val id = latest.optLong("id", 0L)
            if (id <= 0L) return@get null
            ChatReceivedRating(
                id = id,
                stars = latest.optInt("stars", 0),
                raterName = latest.optString("rater_name").ifBlank { "A student" },
                createdEpoch = latest.optLong("created_epoch", 0L)
            )
        }
    }

    /** A student's public reputation (no notes, no rater names). */
    suspend fun fetchRatingSummary(
        backendBaseUrl: String,
        token: String,
        peerId: Int
    ): ChatResult<ChatRatingSummary> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "rating_summary", query = "peer_id=$peerId") { json ->
            parseRatingSummary(json.optJSONObject("summary"))
        }
    }

    private fun parseRating(obj: JSONObject?): ChatRating? {
        if (obj == null) return null
        val id = obj.optLong("id", 0L)
        if (id <= 0L) return null

        val tags = ArrayList<String>()
        obj.optJSONArray("tags")?.let { array ->
            for (index in 0 until array.length()) {
                val tag = array.optString(index).trim()
                if (tag.isNotEmpty()) tags.add(tag)
            }
        }

        // The row carries both ids; the useful one is always "the other side".
        val isMine = obj.optBoolean("is_mine", false)
        val other = if (isMine) obj.optInt("ratee_id", 0) else obj.optInt("rater_id", 0)

        return ChatRating(
            id = id,
            peerId = other,
            subjectCode = obj.optString("subject_code"),
            subjectLabel = obj.optString("subject_label"),
            kind = obj.optString("kind", "help"),
            stars = obj.optInt("stars", 0),
            tags = tags,
            note = obj.optString("note"),
            isMine = isMine,
            createdEpoch = obj.optLong("created_epoch", 0L),
            editable = obj.optBoolean("editable", false),
            locksEpoch = obj.optLong("locks_epoch", 0L).takeIf { it > 0L }
        )
    }

    private fun parseRatingSummary(obj: JSONObject?): ChatRatingSummary {
        if (obj == null) return ChatRatingSummary()

        val subjects = ArrayList<ChatRatingSubject>()
        obj.optJSONArray("subjects")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                subjects.add(
                    ChatRatingSubject(
                        code = item.optString("code"),
                        label = item.optString("label"),
                        count = item.optInt("count", 0),
                        average = if (item.isNull("average")) null else item.optDouble("average", 0.0)
                    )
                )
            }
        }

        val tags = ArrayList<ChatRatingTag>()
        obj.optJSONArray("tags")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val tag = item.optString("tag").trim()
                if (tag.isNotEmpty()) {
                    tags.add(ChatRatingTag(tag = tag, count = item.optInt("count", 0)))
                }
            }
        }

        return ChatRatingSummary(
            count = obj.optInt("count", 0),
            average = if (obj.isNull("average")) null else obj.optDouble("average", 0.0),
            subjects = subjects,
            tags = tags
        )
    }

    /**
     * Null-safe parser: a request payload the app cannot understand must not
     * crash a poll — it simply means "no request on screen".
     */
    private fun parseHelpRequest(obj: JSONObject?): ChatHelpRequest? {
        if (obj == null) return null
        val id = obj.optLong("id", 0L)
        if (id <= 0L) return null

        val purposes = ArrayList<String>()
        val purposeArray = obj.optJSONArray("purposes")
        if (purposeArray != null) {
            for (index in 0 until purposeArray.length()) {
                val value = purposeArray.optString(index).trim()
                if (value.isNotEmpty()) purposes.add(value)
            }
        }

        return ChatHelpRequest(
            id = id,
            peerId = obj.optInt("peer_id", 0),
            subjectCode = obj.optString("subject_code"),
            subjectLabel = obj.optString("subject_label"),
            purposes = purposes,
            status = obj.optString("status", "pending"),
            isSeeker = obj.optBoolean("is_seeker", true),
            canAccept = obj.optBoolean("can_accept", false),
            canClose = obj.optBoolean("can_close", false),
            canCancel = obj.optBoolean("can_cancel", false),
            acceptedEpoch = obj.optLong("accepted_epoch", 0L).takeIf { it > 0L },
            completedEpoch = obj.optLong("completed_epoch", 0L).takeIf { it > 0L }
        )
    }

    // -----------------------------------------------------------------------
    // Directory + presence
    // -----------------------------------------------------------------------

    suspend fun loadDirectory(
        backendBaseUrl: String,
        token: String
    ): ChatResult<ChatDirectory> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "threads") { json ->
            val arr = json.optJSONArray("threads") ?: JSONArray()
            val threads = ArrayList<ChatThread>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                threads += ChatThread(
                    peer = parsePeer(obj.optJSONObject("peer") ?: JSONObject()).copy(
                        isTyping = obj.optBoolean("is_typing", false)
                    ),
                    purpose = obj.optString("purpose"),
                    purposeSubject = obj.optString("purpose_subject")
                )
            }
            ChatDirectory(
                threads = threads,
                onlineCount = json.optInt("online_count", 0),
                totalStudents = json.optInt("total_students", threads.size),
                safetyAcknowledged = json.optBoolean("safety_acknowledged", false),
                serverEpoch = json.optLong("server_epoch", 0L),
                myPublicKey = json.optString("my_public_key").takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun heartbeat(backendBaseUrl: String, token: String): ChatResult<Long> =
        withContext(Dispatchers.IO) {
            post(backendBaseUrl, token, "heartbeat", JSONObject()) { json ->
                json.optLong("online_count", 0L)
            }
        }

    /**
     * Publish one undelivered message. [peer] supplies the encryption key; the
     * ciphertext is produced here, so the plain text never leaves the device.
     *
     * @return the relay id assigned to the message.
     */
    suspend fun sendMessage(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        peer: ChatPeer,
        plaintext: String
    ): ChatResult<Long> = withContext(Dispatchers.IO) {
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
        val cipher = ChatCrypto.encrypt(key, plaintext)
            ?: return@withContext ChatResult.Failure("This message could not be encrypted.")

        post(
            backendBaseUrl,
            token,
            "send",
            JSONObject().put("peer_id", peer.id).put("body", cipher)
        ) { json -> json.optLong("server_id", 0L) }
    }

    /**
     * Fetch everything waiting for me. The server deletes each row as it hands
     * it over, so this is the only copy the device will ever get.
     */
    suspend fun fetchInbox(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String
    ): ChatResult<ChatInbox> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "inbox") { json ->
            val messages = ArrayList<ChatMessage>()
            val receipts = HashMap<Int, Long>()

            val msgArr = json.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until msgArr.length()) {
                val obj = msgArr.optJSONObject(i) ?: continue
                val sender = parsePeer(obj.optJSONObject("sender") ?: JSONObject())
                val serverId = obj.optLong("server_id", 0L)
                val key = conversationKey(context, accountKey, sender)
                val plain = ChatCrypto.decrypt(key, obj.optString("body"))
                val decoded = ChatEnvelope.decode(plain)
                messages += ChatMessage(
                    localId = "srv-$serverId",
                    serverId = serverId,
                    senderId = obj.optInt("sender_id", sender.id),
                    body = decoded.text,
                    isMine = false,
                    sentAtEpoch = obj.optLong("sent_epoch", 0L),
                    attachment = decoded.attachment,
                    replyTo = decoded.reply
                )
            }

            val receiptArr = json.optJSONArray("receipts") ?: JSONArray()
            for (i in 0 until receiptArr.length()) {
                val obj = receiptArr.optJSONObject(i) ?: continue
                val peerId = obj.optInt("peer_id", 0)
                val upTo = obj.optLong("up_to_server_id", 0L)
                if (peerId > 0 && upTo > 0) {
                    receipts[peerId] = maxOf(receipts[peerId] ?: 0L, upTo)
                }
            }

            val deletions = ArrayList<ChatDeletion>()
            val delArr = json.optJSONArray("deletions") ?: JSONArray()
            for (i in 0 until delArr.length()) {
                val obj = delArr.optJSONObject(i) ?: continue
                val peerId = obj.optInt("peer_id", 0)
                val serverId = obj.optLong("server_id", 0L)
                if (peerId > 0 && serverId > 0) {
                    deletions += ChatDeletion(peerId = peerId, serverId = serverId)
                }
            }

            ChatInbox(
                messages = messages,
                receipts = receipts,
                deletions = deletions,
                serverEpoch = json.optLong("server_epoch", 0L)
            )
        }
    }

    /** Tell the sender how far I have read their messages. */
    suspend fun sendReadReceipt(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        upToServerId: Long
    ) {
        withContext(Dispatchers.IO) {
            post(
                backendBaseUrl,
                token,
                "read",
                JSONObject().put("peer_id", peerId).put("up_to_server_id", upToServerId)
            ) { }
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

    suspend fun acknowledgeSafetyNotice(
        backendBaseUrl: String,
        token: String
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(backendBaseUrl, token, "ack_safety", JSONObject()) { }
    }

    // -----------------------------------------------------------------------
    // Plumbing
    // -----------------------------------------------------------------------

    /**
     * Delete one of my messages for both sides. The relay can only hold the
     * instruction, so the peer's app removes the copy it already has.
     */
    suspend fun deleteMessage(
        backendBaseUrl: String,
        token: String,
        peerId: Int,
        serverId: Long
    ): ChatResult<Boolean> = withContext(Dispatchers.IO) {
        post(
            backendBaseUrl,
            token,
            "delete_message",
            JSONObject().put("peer_id", peerId).put("server_id", serverId)
        ) { true }
    }

    /**
     * Public profile of one student.
     *
     * The response is an explicit allow-list (education + presence), so nothing
     * account-identifying can leak into the chat profile screen even if the
     * server grows new columns later.
     */
    suspend fun fetchProfile(
        backendBaseUrl: String,
        token: String,
        peerId: Int
    ): ChatResult<ChatPeerProfile> = withContext(Dispatchers.IO) {
        get(backendBaseUrl, token, "profile", query = "peer_id=$peerId") { json ->
            val peerJson = json.optJSONObject("peer") ?: JSONObject()
            val education = json.optJSONObject("education") ?: JSONObject()
            val subjects = ArrayList<String>(3)
            val subjectsJson = education.optJSONArray("subjects")
            if (subjectsJson != null) {
                for (index in 0 until subjectsJson.length()) {
                    val item = subjectsJson.optJSONObject(index) ?: continue
                    val label = item.optString("label").trim()
                    if (label.isNotEmpty()) subjects.add(label)
                }
            }
            ChatPeerProfile(
                peer = parsePeer(peerJson),
                targetExam = education.optString("target_exam"),
                streamOrDegree = education.optString("stream_or_degree"),
                institution = education.optString("institution"),
                streamCode = education.optString("stream_code"),
                examLevel = education.optString("exam_level"),
                examYear = education.optInt("exam_year", 0).takeIf { it > 0 },
                medium = education.optString("medium"),
                subjects = subjects,
                joinedEpoch = if (json.isNull("joined_epoch")) {
                    null
                } else {
                    json.optLong("joined_epoch", 0L).takeIf { it > 0L }
                }
            )
        }
    }

    /**
     * Hand this device's push token to the relay.
     *
     * The token is what lets the server wake this phone with a data message the
     * moment a message is queued. It is stored per account and replaced on every
     * refresh, because FCM can rotate it at any time.
     */
    suspend fun registerDevice(
        backendBaseUrl: String,
        token: String,
        fcmToken: String,
        platform: String = "android"
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(
            backendBaseUrl,
            token,
            "register_device",
            JSONObject().put("fcm_token", fcmToken).put("platform", platform)
        ) { }
    }

    /** Stop pushes for this account (called on sign-out). */
    suspend fun unregisterDevice(
        backendBaseUrl: String,
        token: String
    ): ChatResult<Unit> = withContext(Dispatchers.IO) {
        post(backendBaseUrl, token, "unregister_device", JSONObject()) { }
    }

    // -----------------------------------------------------------------------
    // Attachments
    // -----------------------------------------------------------------------

    /**
     * Encrypt a file with the conversation key and drop it on the relay.
     *
     * The server only ever stores the AES-GCM blob: it cannot read the file,
     * cannot tell a photo from a PDF, and deletes it as soon as the recipient
     * downloads it (or after 7 days).
     */
    suspend fun uploadAttachment(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        peer: ChatPeer,
        bytes: ByteArray,
        /** 0f..1f, called from the upload thread as bytes leave the phone. */
        onProgress: (Float) -> Unit = {}
    ): ChatResult<String> = withContext(Dispatchers.IO) {
        val key = conversationKey(context, accountKey, peer)
            ?: return@withContext ChatResult.Failure(
                "${peer.name} hasn't opened secure chat yet, so files cannot be sent to them."
            )
        val blob = ChatCrypto.encryptBytes(key, bytes)
            ?: return@withContext ChatResult.Failure("That file could not be encrypted.")

        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("peer_id", peer.id.toString())
                .addFormDataPart(
                    "file",
                    "blob.bin",
                    ProgressRequestBody(
                        bytes = blob,
                        contentType = "application/octet-stream".toMediaType(),
                        onProgress = onProgress
                    )
                )
                .build()

            val url = endpoint(backendBaseUrl, "upload_attachment") + "&peer_id=" + peer.id
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                    ?: return@use ChatResult.Failure(
                        "The server rejected the upload (HTTP ${response.code}). " +
                            "Check that api/chat.php is the latest version.",
                        response.code
                    )
                if (!response.isSuccessful || json.optString("status") != "success") {
                    return@use ChatResult.Failure(
                        json.optString("message").ifBlank { "The file could not be uploaded." },
                        response.code
                    )
                }
                val id = json.optString("attachment_id")
                if (id.isBlank()) {
                    ChatResult.Failure("The server did not return an attachment id.", response.code)
                } else {
                    ChatResult.Success(id)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "attachment upload failed: ${e.message}")
            ChatResult.Failure("No connection. The file was not sent.")
        } catch (e: Exception) {
            Log.e(TAG, "attachment upload crashed: ${e.message}", e)
            ChatResult.Failure("The file could not be uploaded.")
        }
    }

    /** Fetch and decrypt one attachment blob from the relay. */
    suspend fun downloadAttachment(
        context: Context,
        accountKey: String,
        backendBaseUrl: String,
        token: String,
        peer: ChatPeer,
        attachmentId: String,
        /** 0f..1f as the encrypted blob arrives. */
        onProgress: (Float) -> Unit = {}
    ): ChatResult<ByteArray> = withContext(Dispatchers.IO) {
        val key = conversationKey(context, accountKey, peer) ?: return@withContext ChatResult.Failure(
            "Secure chat is still starting on this device."
        )

        try {
            val url = endpoint(backendBaseUrl, "attachment") + "&id=" + attachmentId
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type").orEmpty()
                if (!response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    val message = runCatching { JSONObject(raw).optString("message") }.getOrNull()
                    return@use ChatResult.Failure(
                        message?.takeIf { it.isNotBlank() }
                            ?: "That file is no longer available on the server.",
                        response.code
                    )
                }
                if (!contentType.contains("octet-stream", ignoreCase = true)) {
                    return@use ChatResult.Failure("The server returned an unexpected file.")
                }
                val body = response.body
                    ?: return@use ChatResult.Failure("That file could not be downloaded.")
                val expected = body.contentLength()
                val blob = body.byteStream().use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        out.write(buffer, 0, read)
                        if (expected > 0L) {
                            onProgress((total.toFloat() / expected).coerceIn(0f, 1f))
                        }
                    }
                    onProgress(1f)
                    out.toByteArray()
                }
                val plain = ChatCrypto.decryptBytes(key, blob)
                    ?: return@use ChatResult.Failure("That file could not be decrypted on this device.")
                ChatResult.Success(plain)
            }
        } catch (e: IOException) {
            ChatResult.Failure("No connection. The file will download when you are back online.")
        } catch (e: Exception) {
            Log.e(TAG, "attachment download crashed: ${e.message}", e)
            ChatResult.Failure("That file could not be downloaded.")
        }
    }

    private fun conversationKey(context: Context, accountKey: String, peer: ChatPeer): SecretKey? {
        val peerKey = peer.publicKey
        if (peerKey.isNullOrBlank()) return null
        return ChatCrypto.conversationKey(context, accountKey, peer.id, peerKey)
    }

    private fun <T> get(
        backendBaseUrl: String,
        token: String,
        action: String,
        query: String? = null,
        parse: (JSONObject) -> T
    ): ChatResult<T> {
        return try {
            val request = Request.Builder()
                .url(endpoint(backendBaseUrl, action, query))
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
}

/**
 * Multipart body part that reports how much has gone out, so a 25 MB document
 * can show a real progress bar instead of an indefinite spinner.
 */
private class ProgressRequestBody(
    private val bytes: ByteArray,
    private val contentType: MediaType?,
    private val onProgress: (Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = bytes.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val chunkSize = 64 * 1024
        var written = 0
        while (written < bytes.size) {
            val count = minOf(chunkSize, bytes.size - written)
            sink.write(bytes, written, count)
            written += count
            onProgress((written.toFloat() / bytes.size).coerceIn(0f, 1f))
        }
    }
}
