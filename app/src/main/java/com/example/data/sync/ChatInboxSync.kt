package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.TheStudentDatabase
import com.example.data.local.TheStudentPreferences
import com.example.data.model.ChatMessage
import com.example.data.remote.PastPapersRemoteRepository
import com.example.data.remote.TheStudentChatClient

/**
 * Fetch the relay and store what it hands over — **without a view model**.
 *
 * The chat tab does this itself while it is open, but a push can arrive when no
 * screen exists (phone in a pocket, app killed). This is the same fetch, reduced
 * to what a notification needs: decrypt, persist, and keep the read state honest.
 * Attachment blobs are deliberately *not* downloaded here — the conversation
 * downloads them when it is opened, and a notification should never wait on 25 MB.
 */
object ChatInboxSync {

    private const val TAG = "ChatInboxSync"

    /** Session bits needed to talk to the relay. */
    private data class Session(val baseUrl: String, val token: String, val accountKey: String)

    private fun session(context: Context): Session? {
        val preferences = TheStudentPreferences(context)
        val token = preferences.sessionToken?.takeIf { it.isNotBlank() } ?: return null
        val baseUrl = PastPapersRemoteRepository(context).backendBaseUrl
        // Same account scoping the app uses for the encryption identity.
        val accountKey = preferences.userEmail?.takeIf { it.isNotBlank() } ?: "local-account"
        return Session(baseUrl, token, accountKey)
    }

    /**
     * Pull everything waiting on the relay and store it locally.
     *
     * Returns the messages that were newly received (already decrypted), or an
     * empty list when there was nothing to do — the caller only needs to know
     * whether anything changed.
     */
    suspend fun fetchAndStore(context: Context): List<ChatMessage> {
        val session = session(context) ?: return emptyList()
        return try {
            val inbox = TheStudentChatClient.fetchInbox(
                context = context,
                accountKey = session.accountKey,
                backendBaseUrl = session.baseUrl,
                token = session.token
            ).dataOrNull() ?: return emptyList()

            val dao = TheStudentDatabase.getDatabase(context).theStudentDao()

            inbox.messages.forEach { message ->
                val attachment = message.attachment
                dao.upsertChatMessage(
                    ChatMessageEntity(
                        localId = message.localId,
                        serverId = message.serverId,
                        peerId = message.senderId,
                        senderId = message.senderId,
                        isMine = false,
                        body = message.body,
                        sentAtEpoch = message.sentAtEpoch,
                        // Stored unread: the conversation marks it read when it is
                        // opened, which is also what clears the notification.
                        isRead = false,
                        attachmentId = attachment?.id,
                        attachmentName = attachment?.name,
                        attachmentMime = attachment?.mime,
                        attachmentSize = attachment?.size ?: 0L,
                        attachmentKind = attachment?.kind,
                        attachmentDurationMs = attachment?.durationMs ?: 0L,
                        attachmentLocal = false,
                        replyToServerId = message.replyTo?.serverId ?: 0L,
                        replyToIsMine = message.replyTo?.isMine ?: false,
                        replyToPreview = message.replyTo?.preview
                    )
                )
            }

            inbox.receipts.forEach { (peerId, upTo) ->
                dao.markMyMessagesRead(peerId, upTo)
            }
            inbox.deletions.forEach { deletion ->
                dao.deleteReceivedMessage("srv-${deletion.serverId}")
            }

            inbox.messages
        } catch (error: Throwable) {
            Log.w(TAG, "Background inbox fetch failed", error)
            emptyList()
        }
    }

    /**
     * Peer id -> display name, for notification titles.
     *
     * Read from the relay's directory rather than stored locally: names change
     * rarely, and a push that arrives after a restart has no directory cached.
     */
    suspend fun peerNames(context: Context): Map<Int, String> {
        val session = session(context) ?: return emptyMap()
        return try {
            val directory = TheStudentChatClient.loadDirectory(
                backendBaseUrl = session.baseUrl,
                token = session.token
            ).dataOrNull() ?: return emptyMap()
            directory.threads.associate { it.peer.id to it.peer.name }
        } catch (error: Throwable) {
            Log.w(TAG, "Could not load peer names for notifications", error)
            emptyMap()
        }
    }
}
