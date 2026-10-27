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
}
