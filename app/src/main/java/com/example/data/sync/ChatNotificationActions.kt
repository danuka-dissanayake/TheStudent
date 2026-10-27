package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.TheStudentDatabase
import com.example.data.local.TheStudentPreferences
import com.example.data.model.ChatPeer
import com.example.data.remote.PastPapersRemoteRepository
import com.example.data.remote.TheStudentChatClient
import com.example.notifications.ChatNotifications
import com.example.notifications.ChatNotifier
import com.example.notifications.NotificationPeer

/**
 * Actions a student can take straight from a chat notification, without opening
 * the app: reply in place, or mark the conversation read.
 *
 * This deliberately reuses the same code the app uses — the same session, the
 * same ECDH conversation key, the same relay call — so a message sent from the
 * notification is indistinguishable from one sent in the chat screen.
 */
object ChatNotificationActions {

    private const val TAG = "ChatNotificationActions"

    private data class Session(val baseUrl: String, val token: String, val accountKey: String)

    private fun session(context: Context): Session? {
        val preferences = TheStudentPreferences(context)
        val token = preferences.sessionToken?.takeIf { it.isNotBlank() } ?: return null
        val accountKey = preferences.userEmail?.takeIf { it.isNotBlank() } ?: "local-account"
        return Session(PastPapersRemoteRepository(context).backendBaseUrl, token, accountKey)
    }

    /**
     * Send [text] to [peerId] from the notification's inline reply box.
     *
     * Returns true when the relay accepted it. The message is also written to the
     * local database, so opening the chat afterwards shows it in place instead of
     * a hole where the reply should be.
     */
    suspend fun reply(context: Context, peerId: Int, text: String): Boolean {
        val body = text.trim()
        if (body.isEmpty()) return false
        val session = session(context) ?: return false

        return try {
            val peer: ChatPeer = ChatInboxSync.peers(context)[peerId] ?: return false
            val result = TheStudentChatClient.sendMessage(
                context = context,
                accountKey = session.accountKey,
                backendBaseUrl = session.baseUrl,
                token = session.token,
                peer = peer,
                plaintext = body
            )
            val serverId = result.dataOrNull() ?: run {
                Log.w(TAG, "Reply from notification not delivered: ${result.failureMessage}")
                return false
            }

            val dao = TheStudentDatabase.getDatabase(context).theStudentDao()
            dao.upsertChatMessage(
                ChatMessageEntity(
                    localId = "srv-$serverId",
                    serverId = serverId,
                    peerId = peer.id,
                    senderId = 0,
                    isMine = true,
                    body = body,
                    sentAtEpoch = System.currentTimeMillis() / 1000L,
                    isRead = false,
                    isPending = false
                )
            )
            true
        } catch (error: Throwable) {
            Log.w(TAG, "Reply from notification failed", error)
            false
        }
    }

    /**
     * Mark [peerId]'s messages read: locally, on the relay (so the sender sees the
     * ticks), and in the shade (the notification disappears).
     */
    suspend fun markRead(context: Context, peerId: Int) {
        try {
            val dao = TheStudentDatabase.getDatabase(context).theStudentDao()
            dao.markConversationRead(peerId)
            ChatNotifications.clearForPeer(context, peerId)

            val session = session(context) ?: return
            val upTo = dao.newestServerIdFromPeer(peerId) ?: 0L
            if (upTo > 0) {
                TheStudentChatClient.sendReadReceipt(
                    backendBaseUrl = session.baseUrl,
                    token = session.token,
                    peerId = peerId,
                    upToServerId = upTo
                )
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Could not mark conversation read", error)
        }
    }

    /** Rebuild the shade from the database after an action changed something. */
    suspend fun reconcile(context: Context) {
        try {
            val dao = TheStudentDatabase.getDatabase(context).theStudentDao()
            val peers = ChatInboxSync.peers(context)
            ChatNotifier.reconcile(
                context = context,
                dao = dao,
                peers = { peerId ->
                    peers[peerId]?.let { peer ->
                        NotificationPeer(id = peer.id, name = peer.name, avatarUrl = peer.avatarUrl)
                    }
                }
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Could not refresh notifications", error)
        }
    }
}
