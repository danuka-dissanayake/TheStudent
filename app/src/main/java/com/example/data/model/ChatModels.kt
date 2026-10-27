package com.example.data.model

import org.json.JSONObject
import java.util.Locale

/**
 * A registered student as seen from the chat screen.
 *
 * Privacy: this is deliberately the *whole* of what the chat API exposes about
 * another student — id, full name, profile picture and coarse presence. E-mail,
 * phone number, school and every other profile column never leave the server.
 */
data class ChatPeer(
    val id: Int,
    /** Full name only (registration name). Never split, never an e-mail. */
    val name: String,
    /** Absolute URL of the uploaded profile picture, or null for the initials avatar. */
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    /** Seconds since the student was last active; null when never seen. */
    val lastSeenSeconds: Long? = null,
    /** True while this student is typing to me right now. */
    val isTyping: Boolean = false,
    /**
     * Public half of their end-to-end encryption key (base64 SPKI).
     * Null when the student has never opened chat on an updated build, so no
     * encrypted message can be delivered to them yet.
     */
    val publicKey: String? = null
) {
    /** True when this student can receive end-to-end encrypted messages. */
    val canReceiveEncrypted: Boolean get() = !publicKey.isNullOrBlank()

    val initials: String
        get() {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts.first().take(1).uppercase()
                else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
            }
        }

    /** "Online" / "typing…" / "last seen 5 min ago" for the chat header. */
    val presenceLabel: String
        get() = when {
            isTyping -> "typing…"
            isOnline -> "Online"
            lastSeenSeconds == null -> "Offline"
            lastSeenSeconds < 60 -> "Last seen just now"
            lastSeenSeconds < 3600 -> "Last seen ${lastSeenSeconds / 60} min ago"
            lastSeenSeconds < 86_400 -> "Last seen ${lastSeenSeconds / 3600} h ago"
            lastSeenSeconds < 172_800 -> "Last seen yesterday"
            else -> "Last seen ${lastSeenSeconds / 86_400} d ago"
        }
}

/**
 * One message in a 1:1 conversation.
 *
 * [localId] is generated on the device, so a message has an identity before the
 * server ever sees it (offline-first). [serverId] stays 0 until the relay
 * accepted it, and is what read receipts refer to.
 */
data class ChatMessage(
    val localId: String,
    val serverId: Long = 0L,
    val senderId: Int,
    val body: String,
    val isMine: Boolean,
    val sentAtEpoch: Long,
    val isRead: Boolean = false,
    val isPending: Boolean = false,
    /** Image, document or voice note sent with this message, if any. */
    val attachment: ChatAttachment? = null,
    /** The message this one quotes, if it was sent as a reply. */
    val replyTo: ChatReply? = null,
    /** True once the decrypted file for [attachment] is on this phone. */
    val attachmentAvailable: Boolean = false
)

/** A directory row: a student plus the state of my conversation with them. */
data class ChatThread(
    val peer: ChatPeer,
    val lastMessage: String? = null,
    val lastMessageIsMine: Boolean = false,
    val lastMessageEpoch: Long = 0L,
    val unreadCount: Int = 0
) {
    val hasConversation: Boolean get() = lastMessageEpoch > 0L
}

/** Everything the chat UI needs for the directory + presence counters. */
data class ChatDirectory(
    val threads: List<ChatThread> = emptyList(),
    /** Other students currently online (never includes me — see the API). */
    val onlineCount: Int = 0,
    val totalStudents: Int = 0,
    val safetyAcknowledged: Boolean = false,
    val serverEpoch: Long = 0L,
    /** The server's copy of my own published key (null until I publish one). */
    val myPublicKey: String? = null
)


// ─────────────────────────────────────────────────────────────────────────────
//  Attachments
// ─────────────────────────────────────────────────────────────────────────────

/** One image or document travelling with a message. */
data class ChatAttachment(
    /** Relay id: random hex issued by `api/chat.php?action=upload_attachment`. */
    val id: String,
    val name: String,
    val mime: String,
    val size: Long,
    /** "image", "file" or "audio" — decides which bubble the chat renders. */
    val kind: String,
    /** Length of a voice note in milliseconds (0 for anything else). */
    val durationMs: Long = 0L
) {
    val isImage: Boolean get() = kind == IMAGE
    val isAudio: Boolean get() = kind == AUDIO

    /** "0:07" for a voice note. */
    val readableDuration: String
        get() {
            val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
            return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
        }

    /** "1.4 MB" for the bubble subtitle. */
    val readableSize: String
        get() = when {
            size <= 0L -> ""
            size < 1024L -> "$size B"
            size < 1024L * 1024L -> String.format(Locale.US, "%.0f KB", size / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
        }

    /** Short extension label: "PDF", "JPG", … */
    val extensionLabel: String
        get() = when {
            name.contains('.') -> name.substringAfterLast('.').uppercase(Locale.US).take(4)
            mime.contains('/') -> mime.substringAfterLast('/').uppercase(Locale.US).take(4)
            else -> "FILE"
        }

    companion object {
        const val IMAGE = "image"
        const val FILE = "file"
        const val AUDIO = "audio"
    }
}

/**
 * The message a reply quotes.
 *
 * Only a short preview travels with the reply — the full original stays in the
 * device database, so quoting never re-uploads anything.
 */
data class ChatReply(
    /** Relay id of the quoted message (0 when it was still pending/never sent). */
    val serverId: Long,
    /** True when the quoted message is mine. */
    val isMine: Boolean,
    /** First ~120 characters of the quoted message, or "Photo" / "Voice note". */
    val preview: String
)

/** An attachment chosen in the composer but not sent yet. */
data class ChatAttachmentDraft(
    /** Temp copy in `cacheDir`, so a revoked content Uri cannot break the send. */
    val path: String,
    val name: String,
    val mime: String,
    val size: Long,
    val kind: String,
    /** Voice note length, 0 for photos and documents. */
    val durationMs: Long = 0L
) {
    val isImage: Boolean get() = kind == ChatAttachment.IMAGE
    val isAudio: Boolean get() = kind == ChatAttachment.AUDIO
}

/**
 * Wire format of one decrypted message body.
 *
 * Plain text messages stay plain text (and older builds keep working), while a
 * message carrying a file is a small JSON envelope — encrypted exactly like a
 * text body, so the relay still only sees `e2ee:v1:…`.
 *
 *     {"v":1,"t":"look at this","a":{"i":"<id>","n":"notes.pdf",
 *      "m":"application/pdf","s":48213,"k":"file"}}
 */
object ChatEnvelope {

    private val MARKER = Regex("""^\{"v":\d+,""")

    fun looksLikeEnvelope(plainText: String): Boolean = MARKER.containsMatchIn(plainText)

    /**
     * Build the plaintext that will be encrypted.
     *
     * A plain text message stays a plain string (so older builds keep working);
     * anything carrying a file, a voice note or a quote becomes a small JSON
     * envelope, which is itself encrypted before upload.
     */
    fun encode(
        text: String,
        attachment: ChatAttachment?,
        reply: ChatReply? = null
    ): String {
        if (attachment == null && reply == null) return text

        val json = JSONObject().put("v", 2).put("t", text)

        attachment?.let { a ->
            json.put(
                "a",
                JSONObject()
                    .put("i", a.id)
                    .put("n", a.name)
                    .put("m", a.mime)
                    .put("s", a.size)
                    .put("k", a.kind)
                    .put("d", a.durationMs)
            )
        }

        reply?.let { r ->
            json.put(
                "r",
                JSONObject()
                    .put("i", r.serverId)
                    .put("m", r.isMine)
                    .put("t", r.preview)
            )
        }

        return json.toString()
    }

    /** Split a decrypted body into caption, attachment and quoted message. */
    fun decode(plainText: String): DecodedBody {
        if (!looksLikeEnvelope(plainText)) return DecodedBody(plainText, null, null)
        return try {
            val json = JSONObject(plainText)
            val attachment = json.optJSONObject("a")?.let { a ->
                val id = a.optString("i")
                if (id.isBlank()) null else ChatAttachment(
                    id = id,
                    name = a.optString("n").ifBlank { "attachment" },
                    mime = a.optString("m").ifBlank { "application/octet-stream" },
                    size = a.optLong("s", 0L),
                    kind = a.optString("k").ifBlank { ChatAttachment.FILE },
                    durationMs = a.optLong("d", 0L)
                )
            }
            val reply = json.optJSONObject("r")?.let { r ->
                val preview = r.optString("t").take(160)
                if (preview.isBlank()) null
                else ChatReply(
                    serverId = r.optLong("i", 0L),
                    isMine = r.optBoolean("m", false),
                    preview = preview
                )
            }
            DecodedBody(json.optString("t"), attachment, reply)
        } catch (e: Exception) {
            DecodedBody(plainText, null, null)
        }
    }
}

/** One decrypted message body: caption, optional file, optional quote. */
data class DecodedBody(
    val text: String,
    val attachment: ChatAttachment?,
    val reply: ChatReply?
)
