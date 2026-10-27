package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class EducationLevel(val displayName: String) {
    ALL("All Levels"),
    SCHOOL("School Students"),
    UNDERGRADUATE("Undergraduates"),
    GRADUATE("Graduates & Postgrads")
}

enum class ExamType(val displayName: String) {
    ALL("All Exams"),
    GCE_AL("G.C.E. A/L"),
    GCE_OL("G.C.E. O/L"),
    UNIVERSITY("University"),
    BIT_BCS("BIT / BCS / Professional")
}

data class PastPaper(
    val id: String,
    val title: String,
    val examination: String = "G.C.E. Examination",
    val subject: String,
    val year: Int,
    val medium: String = "English", // "English", "Sinhala", "Tamil"
    val level: EducationLevel = EducationLevel.SCHOOL,
    val grade: String = if (examination.contains("Ordinary", ignoreCase = true)) "Grade 11" else "Grade 13",
    val category: String = if (examination.contains("Ordinary", ignoreCase = true)) "O/L" else "A/L",
    val stream: String? = null, // e.g. "Physical Science", "Biological Science", "Commerce", "Arts", "Technology"
    val examType: String = examination,
    val pdfUrl: String = "",
    val markingSchemeUrl: String? = null,
    val thumbnailUrl: String = "",
    val description: String = "",
    val createdAt: String? = null,
    val status: String = "published",
    val downloadCount: Int = 1200,
    val pages: Int = 16,
    val fileSize: String = "3.5 MB",
    val hasMarkingScheme: Boolean = !markingSchemeUrl.isNullOrBlank(),
    val syllabusSummary: String = description.ifBlank { "Comprehensive examination syllabus and structure." },
    val sampleQuestions: List<String> = emptyList(),
    val markingSchemeNotes: List<String> = emptyList(),
    val isDownloaded: Boolean = false,
    val isSaved: Boolean = false
)

data class Course(
    val id: String,
    val title: String,
    val instructor: String,
    val institution: String, // e.g. "University of Moratuwa", "Colombo University", "NIE Sri Lanka"
    val level: EducationLevel,
    val category: String, // "Science & Tech", "Business & Finance", "Engineering", "Medicine", "Arts"
    val durationHours: Int,
    val totalLessons: Int,
    val completedLessons: Int = 0,
    val rating: Float,
    val studentsCount: Int,
    val description: String,
    val lessons: List<Lesson>,
    val isEnrolled: Boolean = false,
    val isSaved: Boolean = false
)

data class Lesson(
    val id: String,
    val number: Int,
    val title: String,
    val durationMin: Int,
    val isCompleted: Boolean = false,
    val summary: String,
    val keyConcepts: List<String> = emptyList()
)

data class StudyNote(
    val id: String,
    val title: String,
    val subject: String,
    val level: EducationLevel,
    val author: String,
    val institutionOrSchool: String,
    val downloads: Int,
    val likes: Int,
    val readTimeMinutes: Int,
    val tags: List<String>,
    val contentSummary: String,
    val isSaved: Boolean = false
)

enum class MessageSender {
    USER, AI, ASSISTANT, SYSTEM
}

data class AiChatMessage(
    val id: String,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedFollowUps: List<String> = emptyList()
)

data class VocabularyWord(
    val word: String,
    val phonetic: String,
    val translationSinhala: String,
    val translationTamil: String,
    val definition: String,
    val exampleSentence: String,
    val category: String
)

data class GrammarPractice(
    val id: String,
    val title: String,
    val ruleExplanation: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ConversationScenario(
    val id: String,
    val title: String,
    val contextDescription: String,
    val difficulty: String,
    val speakerRole: String,
    val userRole: String,
    val sampleDialogue: List<Pair<String, String>>
)

// ... (existing content)
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val academicLevel: String = "G.C.E. Advanced Level",
    val streamOrDegree: String = "Physical Science",
    val institution: String = "Sri Lanka Education System",
    val targetExam: String = "G.C.E. A/L",
    val streakDays: Int = 0,
    val studyHoursThisWeek: Float = 0f,
    val totalPapersSolved: Int = 0,
    val points: Int = 0,
    val isLoggedIn: Boolean = false,
    /** E.164 phone number collected on the profile-setup screen. */
    val phone: String = "",
    /** Absolute URL of the profile photo (uploaded file or the Google avatar). */
    val avatarUrl: String = "",
    /**
     * False until the mandatory profile-setup step has been submitted.
     * Defaults to true so cached/guest profiles are never blocked, and only an
     * explicit `profile_completed: false` from the server opens the gate.
     */
    val profileCompleted: Boolean = true,
    /** False for Google-only accounts: the setup screen must ask for a password. */
    val hasPassword: Boolean = true,
    val authProvider: String = "email"
)

/**
 * What the profile-setup screen sends to `?action=complete_profile`.
 *
 * [avatarJpeg] is already decoded, downscaled and re-encoded by
 * `AvatarPreparer`, so it is always a small, valid JPEG.
 */
@Suppress("ArrayInDataClass")
data class ProfileSetupSubmission(
    val fullName: String,
    val phone: String,
    val targetExam: String,
    val institution: String,
    val streamOrDegree: String,
    val password: String? = null,
    val avatarJpeg: ByteArray? = null
)

data class OnlineStudent(
    val id: String,
    val name: String,
    val initials: String,
    val backgroundColor: Color,
    val isOnline: Boolean = true
)

data class ChatMessageItem(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isUser: Boolean,
    val attachmentName: String? = null,
    val attachmentSize: String? = null,
    val attachmentPages: String? = null,
    val paperSnippetTitle: String? = null,
    val paperSnippetSubtitle: String? = null
)

data class ChatConversation(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val isAi: Boolean = false,
    val initials: String,
    val avatarColor: Color,
    val isTyping: Boolean = false,
    val messages: List<ChatMessageItem> = emptyList()
)
