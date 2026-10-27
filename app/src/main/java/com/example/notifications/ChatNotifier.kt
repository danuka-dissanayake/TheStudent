package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.TheStudentDao
import com.example.data.model.ChatAttachment

/** Who a notification is about: the name for the title, the photo for the icon. */
data class NotificationPeer(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null
)

/**
 * Keeps the notification shade in step with the messages stored on this device.
 *
 * It is the single place that decides what a chat notification says, shared by
 * two very different callers:
 *
 *  * the view model, after every relay poll and on foreground/background
 *    transitions — including while the app is open on any other screen;
 *  * the FCM service, when a push wakes the app with the screen off.
 *
 * Being stateless about *why* it was called is what makes that possible: the
 * unread rows in the database are the source of truth, so a push, a poll and a
 * process restart all produce the same, correct notification.
 */
object ChatNotifier {

    private const val TAG = "ChatNotifier"

    /** Conversations that currently have a notification showing. */
    private var notifiedPeers: Set<Int> = emptySet()

    /**
     * What each posted notification currently says. Without this the 2 s poll
     * would re-post identical content and the phone would buzz over and over.
     */
    private val signatures = HashMap<Int, String>()
    private var summarySignature: String? = null

    /** Clear the bookkeeping (sign-out, or notifications switched off). */
    fun reset() {
        notifiedPeers = emptySet()
        signatures.clear()
        summarySignature = null
    }

    /**
     * Reconcile the shade with the unread rows.
     *
     * @param peers resolves a peer id to its name and photo; null falls back to
     *   "Student".
     */
    suspend fun reconcile(
        context: Context,
        dao: TheStudentDao,
        peers: (Int) -> NotificationPeer?
    ) {
        try {
            if (!ChatNotifications.canPost(context)) {
                ChatNotifications.clearAll(context)
                reset()
                return
            }

            val unread = dao.unreadMessages()
            val byPeer = unread.groupBy { it.peerId }

            // Only the conversation that is actually on screen is suppressed.
            // Everywhere else — home, papers, the chat list, the background —
            // the notification still arrives while the app is open.
            val activePeerId = ChatNotifications.uiActivePeerId

            var totalUnread = 0
            val summaryLines = mutableListOf<String>()
            val showing = mutableSetOf<Int>()

            // Newest conversation first in the summary, oldest message first
            // inside each conversation.
            val conversations = byPeer.entries
                .sortedByDescending { (_, rows) -> rows.lastOrNull()?.sentAtEpoch ?: 0L }
                .map { (peerId, rows) ->
                    totalUnread += rows.size
                    val peer = peers(peerId)
                    val peerName = peer?.name ?: "Student"
                    val messages = rows.map { row ->
                        ChatNotifications.Message(
                            text = row.notificationText(),
                            sentAtEpoch = row.sentAtEpoch,
                            fromMe = row.isMine
                        )
                    }
                    summaryLines += "$peerName: ${messages.last().text}"
                    Conversation(peerId, peerName, messages, rows.size, peer?.avatarUrl)
                }

            // Grouping is decided up front: a lone notification must not be a
            // group child, or the shade will not let it expand.
            val visible = conversations.filter { it.peerId != activePeerId }
            val grouped = visible.size > 1

            conversations.forEach { conversation ->
                val peerId = conversation.peerId
                if (activePeerId == peerId) {
                    // On screen: no notification for this conversation.
                    ChatNotifications.clearForPeer(context, peerId)
                    signatures.remove(peerId)
                } else {
                    val last = conversation.messages.last()
                    // The grouping flag is part of the signature: going from two
                    // unread conversations to one must repost the survivor
                    // *without* group chrome, which is what makes it expandable
                    // again.
                    val signature =
                        "${conversation.unreadCount}|${last.text}|${last.sentAtEpoch}|$grouped"
                    if (signatures[peerId] != signature) {
                        ChatNotifications.showForPeer(
                            context = context,
                            peerId = peerId,
                            peerName = conversation.peerName,
                            messages = conversation.messages,
                            unreadCount = conversation.unreadCount,
                            avatar = ChatNotificationAvatars.photo(conversation.avatarUrl),
                            grouped = grouped
                        )
                        signatures[peerId] = signature
                    }
                    showing += peerId
                }
            }

            // Conversations that are no longer unread lose their notification.
            (notifiedPeers - showing).forEach { peerId ->
                ChatNotifications.clearForPeer(context, peerId)
                signatures.remove(peerId)
            }
            notifiedPeers = showing

            // One conversation gets no summary: a single chat should look like a
            // single, ordinary notification, not "1 new message" above itself.
            if (showing.size <= 1) {
                ChatNotifications.clearSummary(context)
                summarySignature = null
            } else {
                val signature = summaryLines.joinToString("\u0000") +
                    "|$totalUnread|${showing.size}"
                if (summarySignature != signature) {
                    ChatNotifications.showSummary(
                        context = context,
                        lines = summaryLines,
                        conversationCount = showing.size,
                        messageCount = totalUnread
                    )
                    summarySignature = signature
                }
            }
        } catch (error: Throwable) {
            // Notifications are a feature of chat; they must never be able to
            // break sending, receiving or the app itself.
            Log.w(TAG, "Could not reconcile chat notifications", error)
        }
    }

    /** One conversation, assembled once so grouping can be decided up front. */
    private data class Conversation(
        val peerId: Int,
        val peerName: String,
        val messages: List<ChatNotifications.Message>,
        val unreadCount: Int,
        val avatarUrl: String?
    )

    /** One line of a message as it should appear in a notification. */
    private fun ChatMessageEntity.notificationText(): String {
        val text = body.trim()
        if (text.isNotBlank()) return text
        return when (attachmentKind) {
            ChatAttachment.IMAGE -> "📷 Photo"
            ChatAttachment.AUDIO -> "🎤 Voice note"
            null -> "Message"
            else -> "📎 " + (attachmentName ?: "Attachment")
        }
    }
}
