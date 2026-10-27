package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/**
 * Device notifications for incoming chat messages.
 *
 * The look mirrors what students expect from a messaging app:
 *
 *  * **One notification per conversation**, built with `MessagingStyle`: it
 *    arrives **collapsed** (app icon · student name · time, newest message under
 *    it) and expands, on the usual drag, into the recent messages of that
 *    conversation.
 *  * **The student's photo appears once per message row**, falling back to a
 *    coloured initial so a student without a picture still looks deliberate.
 *  * **All conversations grouped** under one summary, so five active chats do
 *    not fill the shade. With a single conversation no summary is posted at all,
 *    which is why one chat looks like a plain, clean notification.
 *  * **Tap goes straight to the conversation.** The [PendingIntent] carries the
 *    peer id; [MainActivity] hands it to the view model, which switches to the
 *    chat tab and opens that thread.
 *  * **Quiet about what is on screen.** Only the conversation currently open is
 *    suppressed — everywhere else in the app the notification still arrives.
 *
 * Everything here is best-effort: a notification must never be able to break
 * sending, receiving or the app itself.
 */
object ChatNotifications {

    private const val TAG = "ChatNotifications"

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
     * The conversation currently on screen, set by the view model when a chat is
     * opened or closed. Only this one conversation skips notifications: every
     * other screen (home, papers, even the chat list) still gets them.
     */
    @Volatile
    var uiActivePeerId: Int? = null

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
     * how many of them are still unread, shown as the badge number. [avatar] is
     * the peer's photo, already cropped for the large icon (null falls back to a
     * coloured initial).
     */
    fun showForPeer(
        context: Context,
        peerId: Int,
        peerName: String,
        messages: List<Message>,
        unreadCount: Int,
        avatar: android.graphics.Bitmap? = null
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

        // Used only for the message-row avatar (and the Person icon on those
        // rows) — never as the notification's large icon, which would duplicate
        // it in the header.
        val largeIcon = avatar ?: ChatNotificationAvatars.initials(peerName, peerId)

        // androidx Person, not android.app.Person: the framework class only
        // exists from API 28 and this app supports 24.
        val me = androidx.core.app.Person.Builder()
            .setName("You")
            .setKey("me")
            .build()
        val them = androidx.core.app.Person.Builder()
            .setName(peerName)
            .setKey("peer_$peerId")
            .setIcon(
                largeIcon?.let {
                    runCatching { IconCompat.createWithBitmap(it) }.getOrNull()
                }
            )
            .build()

        // A 1:1 chat is NOT a group conversation. Saying it was (true) makes
        // Android print the sender's name above every message *and* keep the
        // conversation title, which is exactly the doubled name.
        //
        // The title is set here, once: that is the name Android shows in the
        // collapsed row (app icon · name · time).
        val style = NotificationCompat.MessagingStyle(me)
            .setConversationTitle(peerName)
            .setGroupConversation(false)

        messages.takeLast(MAX_MESSAGES_IN_NOTIFICATION).forEach { message ->
            style.addMessage(
                message.text,
                message.sentAtEpoch * 1000L,
                if (message.fromMe) me else them
            )
        }

        val last = messages.last()
        // Titles and text are left to MessagingStyle on purpose. Setting
        // setContentTitle(peerName) as well made the name appear in the header
        // AND on the row; setLargeIcon(avatar) made the photo appear twice
        // (thumbnail on the left, sender avatar on the row).
        //
        // Collapsed  : app icon | student name | time, newest message underneath.
        // Expanded   : app icon | app name | time, then one row per message with
        //              the student's photo, name and text.
        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_chat_notification)
            .setColor(ACCENT)
            .setColorized(false)
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

        // Deliberately NO setShortcutId / dynamic shortcut here. From Android 11
        // a notification carrying a shortcut id is rendered as a "conversation",
        // and the system draws the shortcut's icon — the student's photo — in the
        // header beside the app icon, and their name beside the app name. That
        // was the second copy of the photo and of the name.
        post(context, idForPeer(peerId), builder.build())
    }

    /**
     * The single grouped notification the shade collapses to, used only when
     * more than one conversation is unread. [lines] are "Name: message" previews,
     * newest conversation first.
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
            .setContentTitle("$messageCount new messages")
            .setContentText("From $conversationCount students")
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(messageCount)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setGroup(GROUP_CHAT)
            .setGroupSummary(true)
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

    private fun post(context: Context, id: Int, notification: Notification) {
        // SecurityException can be thrown if the grant was revoked between the
        // check and the post; a notification is never worth a crash.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
            .onFailure { Log.w(TAG, "Could not post notification $id", it) }
    }
}
