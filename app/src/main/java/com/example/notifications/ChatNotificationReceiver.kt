package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.data.sync.ChatNotificationActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles the two expanded-notification actions: **Reply** and **Mark as read**.
 *
 * A broadcast receiver has roughly ten seconds before the system stops it, so the
 * work is handed to a coroutine and the receiver is kept alive with [goAsync]
 * until the send has finished.
 */
class ChatNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val peerId = intent.getIntExtra(ChatNotifications.EXTRA_PEER_ID, 0)
        val action = intent.action
        if (peerId <= 0 || action == null) return

        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ChatNotifications.ACTION_REPLY -> {
                        val text = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(ChatNotifications.REPLY_KEY)
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                        if (text.isNotEmpty()) {
                            ChatNotificationActions.reply(appContext, peerId, text)
                        }
                        // Replying means reading: clear the thread and tell the
                        // sender their message was seen.
                        ChatNotificationActions.markRead(appContext, peerId)
                        ChatNotificationActions.reconcile(appContext)
                    }

                    ChatNotifications.ACTION_MARK_READ -> {
                        ChatNotificationActions.markRead(appContext, peerId)
                        ChatNotificationActions.reconcile(appContext)
                    }
                }
            } catch (error: Throwable) {
                Log.w(TAG, "Notification action failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ChatNotificationReceiver"
    }
}
