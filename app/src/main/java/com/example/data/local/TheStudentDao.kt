package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TheStudentDao {
    // Saved Papers
    @Query("SELECT * FROM saved_papers ORDER BY savedAt DESC")
    fun getAllSavedPapers(): Flow<List<SavedPaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPaper(paper: SavedPaperEntity)

    @Query("DELETE FROM saved_papers WHERE id = :paperId")
    suspend fun deleteSavedPaper(paperId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_papers WHERE id = :paperId)")
    fun isPaperSaved(paperId: String): Flow<Boolean>

    // Saved Courses
    @Query("SELECT * FROM saved_courses ORDER BY savedAt DESC")
    fun getAllSavedCourses(): Flow<List<SavedCourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedCourse(course: SavedCourseEntity)

    @Query("DELETE FROM saved_courses WHERE id = :courseId")
    suspend fun deleteSavedCourse(courseId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_courses WHERE id = :courseId)")
    fun isCourseSaved(courseId: String): Flow<Boolean>

    @Query("UPDATE saved_courses SET completedLessons = :completed, progressPercent = :progress WHERE id = :courseId")
    suspend fun updateCourseProgress(courseId: String, completed: Int, progress: Int)

    // Learning Activities / History
    @Query("SELECT * FROM learning_activities ORDER BY timestamp DESC LIMIT 20")
    fun getRecentActivities(): Flow<List<LearningActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: LearningActivityEntity)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Query("UPDATE user_profile SET isLoggedIn = :isLoggedIn WHERE id = 1")
    suspend fun updateLoginStatus(isLoggedIn: Boolean)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    // ── Chat (device-only history) ──────────────────────────────────────────

    @Query("SELECT * FROM chat_messages WHERE peerId = :peerId ORDER BY sentAtEpoch ASC, serverId ASC")
    fun observeConversation(peerId: Int): Flow<List<ChatMessageEntity>>

    @Query(
        """SELECT peerId,
               (SELECT body FROM chat_messages b WHERE b.peerId = a.peerId
                 ORDER BY b.sentAtEpoch DESC, b.serverId DESC LIMIT 1) AS lastBody,
               (SELECT isMine FROM chat_messages c WHERE c.peerId = a.peerId
                 ORDER BY c.sentAtEpoch DESC, c.serverId DESC LIMIT 1) AS lastIsMine,
               (SELECT sentAtEpoch FROM chat_messages d WHERE d.peerId = a.peerId
                 ORDER BY d.sentAtEpoch DESC, d.serverId DESC LIMIT 1) AS lastEpoch,
               SUM(CASE WHEN a.isMine = 0 AND a.isRead = 0 THEN 1 ELSE 0 END) AS unreadCount,
               (SELECT attachmentKind FROM chat_messages e WHERE e.peerId = a.peerId
                 ORDER BY e.sentAtEpoch DESC, e.serverId DESC LIMIT 1) AS lastAttachmentKind,
               (SELECT attachmentName FROM chat_messages f WHERE f.peerId = a.peerId
                 ORDER BY f.sentAtEpoch DESC, f.serverId DESC LIMIT 1) AS lastAttachmentName
        FROM chat_messages a
        GROUP BY a.peerId"""
    )
    fun observeSummaries(): Flow<List<ChatSummaryRow>>

    @Query("SELECT * FROM chat_messages WHERE serverId > 0 ORDER BY serverId DESC LIMIT 1")
    suspend fun newestReceivedMessage(): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChatMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET serverId = :serverId, isPending = 0, sentAtEpoch = :sentAt WHERE localId = :localId")
    suspend fun markChatMessageSent(localId: String, serverId: Long, sentAt: Long)

    @Query("UPDATE chat_messages SET isPending = 0 WHERE localId = :localId")
    suspend fun markChatMessageFailed(localId: String)

    /**
     * Read receipt from [peerId]: they have read MY messages up to
     * [upToServerId]. The ids in a receipt are ids of messages **I** sent, so
     * only `isMine = 1` rows may be touched — filtering on the incoming rows
     * here would clear unread badges for messages that were never opened.
     */
    @Query(
        "UPDATE chat_messages SET isRead = 1 " +
            "WHERE peerId = :peerId AND isMine = 1 AND serverId > 0 AND serverId <= :upToServerId"
    )
    suspend fun markMyMessagesRead(peerId: Int, upToServerId: Long)

    @Query("SELECT MAX(serverId) FROM chat_messages WHERE peerId = :peerId AND isMine = 0")
    suspend fun newestServerIdFromPeer(peerId: Int): Long?

    @Query("SELECT COUNT(*) FROM chat_messages WHERE isMine = 0 AND isRead = 0")
    fun observeTotalUnread(): Flow<Int>

    /**
     * Everything still unread, oldest first, for building device notifications.
     * Reading the rows (rather than keeping a list in memory) means a
     * notification can be rebuilt correctly after the process was recreated.
     */
    @Query(
        "SELECT * FROM chat_messages WHERE isMine = 0 AND isRead = 0 " +
            "ORDER BY sentAtEpoch ASC, serverId ASC"
    )
    suspend fun unreadMessages(): List<ChatMessageEntity>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE peerId = :peerId AND isMine = 0")
    suspend fun markConversationRead(peerId: Int)

    @Query("DELETE FROM chat_messages WHERE peerId = :peerId")
    suspend fun clearConversation(peerId: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun countChatMessages(): Int

    // ── Attachments ─────────────────────────────────────────────────────────

    /** Rows whose file has not been downloaded and decrypted yet. */
    @Query(
        "SELECT * FROM chat_messages " +
            "WHERE attachmentId IS NOT NULL AND attachmentLocal = 0 " +
            "ORDER BY sentAtEpoch DESC LIMIT 12"
    )
    suspend fun pendingAttachments(): List<ChatMessageEntity>

    @Query("UPDATE chat_messages SET attachmentLocal = :available WHERE localId = :localId")
    suspend fun setAttachmentAvailable(localId: String, available: Boolean)

    // ── Deletions ───────────────────────────────────────────────────────────

    /** Remove one message (used by "delete for everyone" on both sides). */
    @Query("DELETE FROM chat_messages WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    /** Remove a message the peer just deleted: delivered rows are `srv-<id>`. */
    @Query("DELETE FROM chat_messages WHERE localId = :localId")
    suspend fun deleteReceivedMessage(localId: String)
}
