package com.example.data.local

import androidx.room.Entity
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
    val hasPassword: Boolean = true
)
