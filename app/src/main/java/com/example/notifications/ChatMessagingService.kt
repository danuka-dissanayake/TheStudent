package com.example.notifications

import android.util.Log
import com.example.data.local.TheStudentDatabase
import com.example.data.local.TheStudentPreferences
import com.example.data.remote.PastPapersRemoteRepository
import com.example.data.remote.TheStudentChatClient
import com.example.data.sync.ChatInboxSync
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives chat pushes from the relay.
 *
 * The push carries **no message text** — only "a message from this student is
 * waiting". The service then fetches from the relay exactly like the app does,
 * decrypts on this device and only then builds the notification. That keeps the
 * end-to-end encryption promise: Google's servers only ever see routing metadata.
 *
 * Everything runs off the main thread and is wrapped, because a push must never
 * be able to crash the app.
 */
class ChatMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TheStudentPreferences(applicationContext).fcmToken = token
        // Upload straight away if a session exists; otherwise the next sign-in
        // (or any app start) sends it.
        scope.launch { uploadToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data["type"] != TYPE_CHAT_MESSAGE) return

        scope.launch {
            runCatching {
                ChatNotifications.ensureChannel(applicationContext)

                // Pull the actual message (decrypts locally) and store it, so the
                // notification shows real text and the conversation is already up
                // to date when it is opened.
                val received = ChatInboxSync.fetchAndStore(applicationContext)
                val names = if (received.isEmpty()) {
                    emptyMap()
                } else {
                    ChatInboxSync.peerNames(applicationContext)
                }

                val dao = TheStudentDatabase.getDatabase(applicationContext).theStudentDao()
                ChatNotifier.reconcile(
                    context = applicationContext,
                    dao = dao,
                    names = { peerId -> names[peerId] },
                    // This process has no conversation on screen; if the app is
                    // open the view model reconciles again on its own poll.
                    activePeerId = null,
                    quiet = ChatNotifications.isAppVisible
                )
            }.onFailure { error ->
                Log.w(TAG, "Could not handle chat push", error)
            }
        }
    }

    private suspend fun uploadToken(fcmToken: String) {
        runCatching {
            val preferences = TheStudentPreferences(applicationContext)
            val sessionToken = preferences.sessionToken?.takeIf { it.isNotBlank() } ?: return
            val baseUrl = PastPapersRemoteRepository(applicationContext).backendBaseUrl
            TheStudentChatClient.registerDevice(
                backendBaseUrl = baseUrl,
                token = sessionToken,
                fcmToken = fcmToken
            )
        }.onFailure { error ->
            Log.w(TAG, "Could not register push token", error)
        }
    }

    private companion object {
        const val TAG = "ChatMessagingService"

        /** Data-message key the relay sets so other pushes are ignored. */
        const val TYPE_CHAT_MESSAGE = "chat_message"
    }
}
