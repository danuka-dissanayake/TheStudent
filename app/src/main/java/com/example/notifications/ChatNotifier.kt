package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.TheStudentDao
import com.example.data.model.ChatAttachment

/**
 * Keeps the notification shade in step with the messages stored on this device.
 *
 * It is the single place that decides what a chat notification says, shared by
 * two very different callers:
 *
 *  * the view model, after every relay poll and on foreground/background
 *    transitions;
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
     * @param names peer id -> display name, for the notification title.
     * @param activePeerId the conversation currently on screen, if any.
     * @param quiet true when the student is looking at the chat list, where the
     *   badges already show everything: a banner on top of that is just noise.
     */
    suspend fun reconcile(
        context: Context,
        dao: TheStudentDao,
        names: (Int) -> String?,
        activePeerId: Int?,
        quiet: Boolean
    ) {
        try {
            if (!ChatNotifications.canPost(context)) {
                ChatNotifications.clearAll(context)
                reset()
                return
            }

            val unread = dao.unreadMessages()
            val byPeer = unread.groupBy { it.peerId }

            if (quiet) {
                (notifiedPeers + byPeer.keys).forEach { peerId ->
                    ChatNotifications.clearForPeer(context, peerId)
                }
                ChatNotifications.clearSummary(context)
                reset()
                return
            }

            var totalUnread = 0
            val summaryLines = mutableListOf<String>()
            val showing = mutableSetOf<Int>()

            // Newest conversation first in the summary, oldest message first
            // inside each conversation.
            byPeer.entries
                .sortedByDescending { (_, rows) -> rows.lastOrNull()?.sentAtEpoch ?: 0L }
                .forEach { (peerId, rows) ->
                    totalUnread += rows.size
                    val peerName = names(peerId) ?: "Student"
                    val messages = rows.map { row ->
                        ChatNotifications.Message(
                            text = row.notificationText(),
                            sentAtEpoch = row.sentAtEpoch,
                            fromMe = row.isMine
                        )
                    }
                    summaryLines += "$peerName: ${messages.last().text}"

                    if (activePeerId == peerId) {
                        // On screen: no notification for this conversation.
                        ChatNotifications.clearForPeer(context, peerId)
                        signatures.remove(peerId)
                    } else {
                        val last = messages.last()
                        val signature = "${rows.size}|${last.text}|${last.sentAtEpoch}"
                        if (signatures[peerId] != signature) {
                            ChatNotifications.showForPeer(
                                context = context,
                                peerId = peerId,
                                peerName = peerName,
                                messages = messages,
                                unreadCount = rows.size
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

            if (showing.isEmpty()) {
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
