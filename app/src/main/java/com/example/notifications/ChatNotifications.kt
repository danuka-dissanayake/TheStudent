package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Device notifications for incoming chat messages.
 *
 * Design goals (WhatsApp-like, without any push service):
 *
 *  * **One notification per conversation.** New messages from the same student
 *    land in the same notification instead of stacking up; the message history
 *    inside it is rebuilt from the unread rows in the device database, so it
 *    survives the process being recreated.
 *  * **All of them grouped.** The individual conversations are children of a
 *    single summary notification ("2 chats · 5 new messages"), which is what the
 *    shade collapses to.
 *  * **Tap goes straight to the conversation.** The [PendingIntent] carries the
 *    peer id; [MainActivity] hands it to the view model, which switches to the
 *    chat tab and opens that thread.
 *  * **Never noisy about what you are reading.** The view model only asks for a
 *    notification when the message is not already on screen.
 *
 * Everything here is best-effort: a notification must never be able to break
 * sending, receiving or the app itself.
 */
object ChatNotifications {

    /** Channel shown in system settings. Users can mute just chat here. */
    const val CHANNEL_MESSAGES = "chat_messages"

    /** Notification group: one summary, many per-conversation children. */
    private const val GROUP_CHAT = "com.thestudent.sl.CHAT"

    private const val SUMMARY_ID = 9_001
    private const val PER_PEER_ID_BASE = 9_100

    /** Brand emerald, used for the notification accent. */
    private const val ACCENT = 0xFF10B981.toInt()

    /** Extra key consumed by MainActivity to open one conversation. */
    const val EXTRA_PEER_ID = "com.thestudent.sl.extra.PEER_ID"

    /**
     * True while any activity of this app is on screen. Set from
     * MainActivity.onStart / onStop. Used to decide whether a message needs a
     * notification at all: if the student is looking at the chat list, the
     * unread badge is enough.
     */
    @Volatile
    var isAppVisible: Boolean = false

    /** One line of a conversation, already decrypted and human readable. */
    data class Message(
        val text: String,
        /** Unix seconds, as stored locally. */
        val sentAtEpoch: Long,
        val fromMe: Boolean
    )

    private fun idForPeer(peerId: Int): Int = PER_PEER_ID_BASE + peerId

    /** Create the channel once; calling it repeatedly is safe. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_MESSAGES) != null) return

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            // HIGH so a new message can pop as a heads-up banner.
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New messages from other students."
            enableLights(true)
            lightColor = ACCENT
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 80, 120)
            setShowBadge(true)
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        runCatching { manager.createNotificationChannel(channel) }
    }

    /**
     * Android 13+ needs an explicit runtime grant; below that the user can still
     * have switched the app off in settings.
     */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Post (or refresh) the notification for one conversation.
     *
     * [messages] is the tail of the conversation, oldest first; [unreadCount] is
     * how many of them are still unread, shown as the badge number.
     */
    fun showForPeer(
        context: Context,
        peerId: Int,
        peerName: String,
        messages: List<Message>,
        unreadCount: Int
    ) {
        if (messages.isEmpty()) {
            clearForPeer(context, peerId)
            return
        }
        if (!canPost(context)) return
        ensureChannel(context)

        // A conversation whose newest message is mine has nothing to announce.
        if (messages.lastOrNull()?.fromMe != false) {
            clearForPeer(context, peerId)
            return
        }

        // androidx Person, not android.app.Person: the framework class only
        // exists from API 28 and this app supports 24.
        val me = androidx.core.app.Person.Builder().setName("You").build()
        val them = androidx.core.app.Person.Builder().setName(peerName).build()

        val style = NotificationCompat.MessagingStyle(me)
            .setConversationTitle(peerName)

        messages.takeLast(MAX_MESSAGES_IN_NOTIFICATION).forEach { message ->
            style.addMessage(
                message.text,
                message.sentAtEpoch * 1000L,
                if (message.fromMe) me else them
            )
        }

        val last = messages.last()
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_chat_notification)
            .setColor(ACCENT)
            .setContentTitle(peerName)
            .setContentText(last.text)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(last.sentAtEpoch * 1000L)
            .setShowWhen(true)
            .setNumber(unreadCount)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setGroup(GROUP_CHAT)
            // The conversation itself is the thing that rings; the summary is
            // silent so a message never buzzes the phone twice.
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(conversationIntent(context, peerId))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        post(context, idForPeer(peerId), notification)
    }

    /**
     * The single grouped notification the shade collapses to. [lines] are
     * "Name: message" previews, newest conversation first.
     */
    fun showSummary(
        context: Context,
        lines: List<String>,
        conversationCount: Int,
        messageCount: Int
    ) {
        if (lines.isEmpty() || conversationCount <= 0) {
            clearSummary(context)
            return
        }
        if (!canPost(context)) return
        ensureChannel(context)

        val style = NotificationCompat.InboxStyle()
        lines.take(MAX_SUMMARY_LINES).forEach { style.addLine(it) }
        if (messageCount > MAX_SUMMARY_LINES) {
            style.setSummaryText("+${messageCount - MAX_SUMMARY_LINES} more")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_chat_notification)
            .setColor(ACCENT)
            .setContentTitle(
                if (conversationCount == 1) "1 new message" else "$messageCount new messages"
            )
            .setContentText(
                if (conversationCount == 1) lines.first() else "From $conversationCount students"
            )
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(messageCount)
            .setAutoCancel(true)
            .setGroup(GROUP_CHAT)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(inboxIntent(context))
            .build()

        post(context, SUMMARY_ID, notification)
    }

    fun clearForPeer(context: Context, peerId: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(idForPeer(peerId)) }
    }

    fun clearSummary(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(SUMMARY_ID) }
    }

    /** Remove every chat notification (sign-out, or "clear all"). */
    fun clearAll(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
    }

    // ── intents ───────────────────────────────────────────────────────────────

    /** Opens the app on the chat tab with [peerId] ready to open. */
    private fun conversationIntent(context: Context, peerId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_PEER_ID, peerId)
            // Distinct action per conversation so the extras are not reused.
            action = "com.thestudent.sl.OPEN_CHAT_$peerId"
        }
        return PendingIntent.getActivity(
            context,
            peerId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Opens the app on the chat tab, no particular conversation. */
    private fun inboxIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "com.thestudent.sl.OPEN_INBOX"
        }
        return PendingIntent.getActivity(
            context,
            SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val SUMMARY_REQUEST_CODE = 1_001

    private const val MAX_MESSAGES_IN_NOTIFICATION = 8
    private const val MAX_SUMMARY_LINES = 5

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        // SecurityException can be thrown if the grant was revoked between the
        // check and the post; a notification is never worth a crash.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
