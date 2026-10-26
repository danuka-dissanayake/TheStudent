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
    val name: String = "Kavindu Perera",
    val email: String = "kavindu.p@dzdlearn.lk",
    val academicLevel: String = "G.C.E. Advanced Level - 2025",
    val streamOrDegree: String = "Physical Science (Combined Maths, Physics, Chemistry)",
    val institution: String = "Royal College, Colombo",
    val targetExam: String = "G.C.E. A/L 2025",
    val streakDays: Int = 14,
    val studyHoursThisWeek: Float = 18.5f,
    val totalPapersSolved: Int = 42,
    val points: Int = 1280,
    val isLoggedIn: Boolean = true
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
