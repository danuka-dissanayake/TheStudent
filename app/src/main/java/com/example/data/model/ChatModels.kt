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
    /** Image or document sent with this message, if any. */
    val attachment: ChatAttachment? = null,
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
    /** "image" or "file" — decides which bubble the chat renders. */
    val kind: String
) {
    val isImage: Boolean get() = kind == IMAGE

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
    }
}

/** An attachment chosen in the composer but not sent yet. */
data class ChatAttachmentDraft(
    /** Temp copy in `cacheDir`, so a revoked content Uri cannot break the send. */
    val path: String,
    val name: String,
    val mime: String,
    val size: Long,
    val kind: String
) {
    val isImage: Boolean get() = kind == ChatAttachment.IMAGE
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

    private const val MARKER = "{\"v\":1,"

    fun looksLikeEnvelope(plainText: String): Boolean = plainText.startsWith(MARKER)

    /** Build the plaintext that will be encrypted. */
    fun encode(text: String, attachment: ChatAttachment?): String {
        if (attachment == null) return text
        val json = JSONObject()
            .put("v", 1)
            .put("t", text)
            .put(
                "a",
                JSONObject()
                    .put("i", attachment.id)
                    .put("n", attachment.name)
                    .put("m", attachment.mime)
                    .put("s", attachment.size)
                    .put("k", attachment.kind)
            )
        return json.toString()
    }

    /** Split a decrypted body into the caption and its attachment. */
    fun decode(plainText: String): Pair<String, ChatAttachment?> {
        if (!looksLikeEnvelope(plainText)) return plainText to null
        return try {
            val json = JSONObject(plainText)
            val attachment = json.optJSONObject("a")?.let { a ->
                val id = a.optString("i")
                if (id.isBlank()) null else ChatAttachment(
                    id = id,
                    name = a.optString("n").ifBlank { "attachment" },
                    mime = a.optString("m").ifBlank { "application/octet-stream" },
                    size = a.optLong("s", 0L),
                    kind = a.optString("k").ifBlank { ChatAttachment.FILE }
                )
            }
            json.optString("t") to attachment
        } catch (e: Exception) {
            plainText to null
        }
    }
}
