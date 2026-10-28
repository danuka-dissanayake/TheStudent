package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "saved_papers")
data class SavedPaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val examination: String,
    val subject: String,
    val year: Int,
    val medium: String,
    val fileSize: String,
    val isDownloaded: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_courses")
data class SavedCourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val instructor: String,
    val institution: String,
    val level: String,
    val progressPercent: Int = 0,
    val totalLessons: Int,
    val completedLessons: Int = 0,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_activities")
data class LearningActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Paper", "Course", "AI Tutor", "Language"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val scoreOrDuration: String
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val email: String,
    val academicLevel: String,
    val streamOrDegree: String,
    val institution: String,
    val targetExam: String,
    val streakDays: Int,
    val studyHoursThisWeek: Float,
    val totalPapersSolved: Int,
    val points: Int,
    val isLoggedIn: Boolean,
    // Added in schema v2 for the mandatory profile-setup step. Defaults keep the
    // migration trivial and mean "nothing known yet", not "complete".
    val phone: String = "",
    val avatarUrl: String = "",
    val profileCompleted: Boolean = true,
    val hasPassword: Boolean = true,
    // Added in schema v6 with the structured academic profile. These are a
    // cache of the server's copy: they let the profile screen and the chat
    // intent sheet show the student's own subjects without a network round
    // trip, and they are overwritten on every save / sign-in.
    val examLevel: String = "",
    val streamCode: String = "",
    /** 0 means "not chosen" (Room has no nullable Int column here by design). */
    val examYear: Int = 0,
    val medium: String = "",
    /** Subjects encoded by `AcademicProfile.encodeSubjects`. */
    val academicSubjects: String = ""
)

/**
 * One chat message, stored **only on the device**.
 *
 * Chat history never touches the server database: the server is a relay for
 * undelivered messages and keeps nothing once they are fetched. This table is
 * the single source of truth for a conversation, so the history survives app
 * restarts but disappears when the student deletes the app (or the chat).
 *
 * [body] is already decrypted for display; Room's database file lives in the
 * app's private storage, which is covered by Android's file-based encryption.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index("peerId"), Index("sentAtEpoch")]
)
data class ChatMessageEntity(
    /** Client-generated id (UUID) — works offline, before the server sees it. */
    @PrimaryKey val localId: String,
    /** Relay id assigned by the server; 0 while the message is still pending. */
    val serverId: Long = 0L,
    /** `users.id` of the other student. */
    val peerId: Int,
    val senderId: Int,
    val isMine: Boolean,
    val body: String,
    val sentAtEpoch: Long,
    val isRead: Boolean = false,
    /** True until the server confirmed it is queued for delivery. */
    val isPending: Boolean = false,

    // ── Attachment (optional) ───────────────────────────────────────────────
    // The file itself never lives in the database: [body] keeps the caption and
    // the decrypted bytes sit in filesDir (see ChatAttachmentStore).
    /** Relay id of the encrypted blob, or null for a plain text message. */
    val attachmentId: String? = null,
    val attachmentName: String? = null,
    val attachmentMime: String? = null,
    val attachmentSize: Long = 0L,
    /** "image" or "file" — picks the bubble layout. */
    val attachmentKind: String? = null,
    /** True once the decrypted file is available on this device. */
    val attachmentLocal: Boolean = false,
    /** Voice note length in milliseconds, 0 otherwise. */
    val attachmentDurationMs: Long = 0L,

    // ── Reply (optional) ────────────────────────────────────────────────────
    /** Relay id of the quoted message, 0 when this is not a reply. */
    val replyToServerId: Long = 0L,
    /** True when the quoted message is mine. */
    val replyToIsMine: Boolean = false,
    /** Short preview of the quoted message. */
    val replyToPreview: String? = null
)

/** Per-conversation projection used to build the chat list. */
data class ChatSummaryRow(
    val peerId: Int,
    val lastBody: String?,
    val lastIsMine: Boolean,
    val lastEpoch: Long,
    val unreadCount: Int,
    /** "image" / "file" when the newest message carries an attachment. */
    val lastAttachmentKind: String? = null,
    val lastAttachmentName: String? = null,
)
