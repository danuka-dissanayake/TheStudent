package com.example.data.model

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
    val isTyping: Boolean = false
) {
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

/** One message in a 1:1 conversation. */
data class ChatMessage(
    val id: Long,
    val senderId: Int,
    val body: String,
    val isMine: Boolean,
    val sentAtEpoch: Long,
    val isRead: Boolean
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
    val onlineCount: Int = 0,
    val totalStudents: Int = 0,
    val safetyAcknowledged: Boolean = false,
    val serverEpoch: Long = 0L
)

/** Snapshot of one open conversation. */
data class ChatConversationState(
    val peer: ChatPeer,
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val safetyAcknowledged: Boolean = false,
    val serverEpoch: Long = 0L
)
