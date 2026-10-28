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
    /** Messages from this peer that I have not opened yet. */
    val unreadCount: Int = 0,
    /**
     * What I said this conversation is about: "help", "talk", "notes",
     * "partner", or "" when nothing has been declared yet. Set by me (never by
     * the peer) so the label in the list always reflects my own intent.
     */
    val purpose: String = "",
    val purposeSubject: String = ""
) {
    val hasConversation: Boolean get() = lastMessageEpoch > 0L

    /**
     * The label on the chat-list row: "Question · Physics", "Notes & papers",
     * "Just talking"…
     *
     * Never blank. A row with no declared purpose is honestly "Just talking" —
     * an empty slot made the list look unfinished, and the server now labels a
     * question chat from the help request itself.
     */
    val purposeLine: String
        get() = when (purpose) {
            "help" -> if (purposeSubject.isBlank()) "Question" else "Question · $purposeSubject"
            "notes" -> "Notes & papers"
            "partner" -> "Study partner"
            "talk" -> "Chatting"
            else -> "Just talking"
        }
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


/**
 * What the chat profile screen is allowed to show about another student.
 *
 * Deliberately narrow: name, photo, the three education fields they chose to
 * fill in, presence and the month they joined. E-mail, phone, role and every
 * login detail stay on the server — see `action=profile` in chat.php.
 */
/**
 * Everything the thread needs to render the help card, including the counts
 * that make a *second* request with the same student feel natural.
 *
 * [completedWithPeer] is how many requests this pair has already closed — it
 * drives "Ask again" and the "you have been helped N times" line.
 */
data class ChatHelpState(
    val request: ChatHelpRequest? = null,
    val completedWithPeer: Int = 0,
    val myOpenRequests: Int = 0,
    /** Purpose as stored on the server for (me, peer); "" when unset. */
    val purpose: String = "",
    val purposeSubject: String = ""
)

/**
 * A help request: the transaction behind a rating.
 *
 * The seeker opens it (optionally naming the subject), the helper accepts, and
 * the seeker closes it when the question is genuinely answered. Only then can a
 * rating be given — which is what stops two friends swapping five stars in ten
 * seconds. [canAccept] / [canClose] / [canCancel] are decided by the server, so
 * the UI can never offer an action the API will refuse.
 */
data class ChatHelpRequest(
    val id: Long,
    val peerId: Int,
    val subjectCode: String = "",
    val subjectLabel: String = "",
    val purposes: List<String> = emptyList(),
    /** pending | accepted | completed | cancelled | expired */
    val status: String = "pending",
    val isSeeker: Boolean = true,
    val canAccept: Boolean = false,
    val canClose: Boolean = false,
    val canCancel: Boolean = false,
    val acceptedEpoch: Long? = null,
    val completedEpoch: Long? = null
) {
    val isOpen: Boolean get() = status == "pending" || status == "accepted"
    val isCompleted: Boolean get() = status == "completed"

    /** Short line for the card header. */
    val subjectLine: String
        get() = subjectLabel.ifBlank { "a question" }
}

/**
 * Ratings — the other half of a closed help request.
 *
 * Rules the app and the server both enforce:
 *  * one rating per (rater, ratee, subject), so the same help cannot be
 *    starred twice from two devices;
 *  * 1–5 stars, at most [ChatRateState.maxTags] tags from a fixed list, and an
 *    optional note of at most [ChatRateState.maxNote] characters;
 *  * a rating is only possible after a *closed* help request with that student,
 *    or — for a pair that only ever talked — after 20 messages each way;
 *  * it can be corrected for 24 hours, then it locks.
 *
 * The note is private: only the student it is about ever reads it.
 */
data class ChatRating(
    val id: Long,
    /** The other side of the rating: who I rated, or who rated me. */
    val peerId: Int = 0,
    val subjectCode: String = "",
    val subjectLabel: String = "",
    /** help (earned by a closed request) | talk (a long, real conversation). */
    val kind: String = "help",
    val stars: Int = 0,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val isMine: Boolean = false,
    val createdEpoch: Long = 0L,
    /** Still inside the 24-hour window, so it can be changed. */
    val editable: Boolean = false,
    val locksEpoch: Long? = null
)

/** A student's public reputation. Never contains a note or a rater's name. */
data class ChatRatingSummary(
    val count: Int = 0,
    val average: Double? = null,
    val subjects: List<ChatRatingSubject> = emptyList(),
    val tags: List<ChatRatingTag> = emptyList()
) {
    val hasRatings: Boolean get() = count > 0

    /** "4.8" — one decimal, or "" when nobody has rated yet. */
    val averageLine: String
        get() {
            val value = average ?: return ""
            val rounded = kotlin.math.round(value * 10.0) / 10.0
            return if (rounded % 1.0 == 0.0) {
                "${rounded.toInt()}.0"
            } else {
                rounded.toString()
            }
        }

    /** "4.8 ★ · 12 ratings" or "New — no ratings yet". */
    val headline: String
        get() = if (hasRatings) {
            "$averageLine ★ · $count rating" + (if (count == 1) "" else "s")
        } else {
            "New — no ratings yet"
        }

    /** The two tag words worth showing on a profile row. */
    val topTags: List<ChatRatingTag> get() = tags.take(2)
}

data class ChatRatingSubject(
    val code: String,
    val label: String,
    val count: Int,
    val average: Double?
)

data class ChatRatingTag(val tag: String, val count: Int) {
    val label: String get() = chatRatingTagLabels[tag] ?: tag.replaceFirstChar { it.uppercase() }
}

/** Tag slugs the server accepts. The words live here, not in the database. */
val chatRatingTagLabels: Map<String, String> = linkedMapOf(
    "clear" to "Clear explanation",
    "patient" to "Patient",
    "fast" to "Fast reply",
    "notes" to "Good notes",
    "friendly" to "Friendly",
    "syllabus" to "Knows the syllabus"
)

/**
 * Everything the rating card needs to know before it opens.
 *
 * [kind] is "help" when a closed request already earns the star, "talk" when
 * only a long conversation can. [canRateNow] is decided by the server for help
 * and by the phone for talk (the server never sees messages, so the phone
 * attests the message count when it sends).
 */
data class ChatRateState(
    val peerId: Int = 0,
    val kind: String = "talk",
    val subjectCode: String = "",
    val subjectLabel: String = "",
    val canRateNow: Boolean = false,
    val talkMinMessages: Int = 20,
    val maxTags: Int = 3,
    val maxNote: Int = 40,
    val existing: ChatRating? = null
) {
    val isTalk: Boolean get() = kind == "talk"

    /** Card title line: "Physics" or "Just talking". */
    val subjectLine: String
        get() = subjectLabel.ifBlank { if (isTalk) "Just talking" else "a question" }
}

/** What `rate` returns: the saved rating and the updated public summary. */
data class ChatRatingResult(
    val rating: ChatRating? = null,
    val summary: ChatRatingSummary = ChatRatingSummary()
)

data class ChatPeerProfile(
    val peer: ChatPeer,
    val targetExam: String = "",
    val streamOrDegree: String = "",
    val institution: String = "",
    /** A/L stream label (or O/L) from the structured profile; "" when unset. */
    val streamCode: String = "",
    /** "AL" or "OL"; "" when the student has not done the academic step. */
    val examLevel: String = "",
    val examYear: Int? = null,
    /** Sinhala / Tamil / English; "" when unset. */
    val medium: String = "",
    /** The three subjects the student actually sits for. */
    val subjects: List<String> = emptyList(),
    /** Unix seconds, when the account was created; null if unknown. */
    val joinedEpoch: Long? = null,
    /** Unix seconds of the first message between us on this phone; null if none. */
    val chattingSinceEpoch: Long? = null
) {
    /** True when there is at least one education row worth rendering. */
    val hasEducation: Boolean
        get() = targetExam.isNotBlank() || streamOrDegree.isNotBlank() ||
            institution.isNotBlank() || subjects.isNotEmpty() || medium.isNotBlank()

    /** "Technology · 2026" — stream and exam year, when we have them. */
    val streamLine: String
        get() = buildList {
            val stream = streamCode.ifBlank { streamOrDegree }
            if (stream.isNotBlank()) add(stream)
            examYear?.let { add(it.toString()) }
        }.joinToString(" · ")
}

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
    /**
     * True when the quoted message was written by **the author of the message
     * carrying this quote** — not by the reader. On the other device the sides
     * are mirrored, so the renderer flips it before showing "You" or a name.
     */
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
