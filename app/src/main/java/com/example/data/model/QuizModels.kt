package com.example.data.model

/**
 * A quiz as the app sees it.
 *
 * The list payload is already the student's view of it: [bestPercent] and
 * [passed] come from the server for the signed-in user, so the card never has to
 * guess, and no option ever carries `isCorrect` outside a graded result.
 */
data class QuizSummary(
    val id: Int,
    val code: String = "",
    val title: String,
    val description: String = "",
    val subject: String,
    val grade: String,
    val category: String,
    val stream: String? = null,
    val medium: String = "English",
    val minutes: Int = 0,
    val timeLimitSeconds: Int = 0,
    val passPercent: Int = 60,
    val expReward: Int = 20,
    val status: String = "published",
    val questionCount: Int = 0,
    val bestPercent: Int? = null,
    val passed: Boolean = false,
    val attempts: Int = 0,
    /** Attempts the quiz allows, and how many are left for this student. */
    val maxAttempts: Int = 3,
    val attemptsLeft: Int = 3,
    val attemptCount: Int = 0,
    val averagePercent: Int = 0
) {
    /**
     * True once every attempt is used. The server decides this (it is the one
     * that will refuse the next `start`), the app only reflects it: an enrolled
     * quiz is listed under "Enrolled" and cannot be opened again.
     */
    val isEnrolled: Boolean get() = attemptsLeft <= 0

    val attemptsLabel: String
        get() = when {
            attempts == 0 -> "$maxAttempts attempts"
            maxAttempts == 1 -> "1 attempt only"
            else -> "${attempts.coerceAtMost(maxAttempts)} of $maxAttempts attempts used"
        }

    val isTimed: Boolean get() = timeLimitSeconds > 0
    val isPublished: Boolean get() = status == "published"
    val isDraft: Boolean get() = status == "draft"
    val statusLabel: String
        get() = when (status) {
            "published" -> "Published"
            "archived" -> "Archived"
            else -> "Draft"
        }
}

data class QuizOption(
    val id: Int,
    val label: String,
    val isCorrect: Boolean = false
)

data class QuizQuestion(
    val id: Int,
    val position: Int = 0,
    val prompt: String,
    val mediaUrl: String? = null,
    val marks: Int = 1,
    val options: List<QuizOption> = emptyList(),
    val explanation: String? = null
)

/** A started attempt: the questions this student was actually given. */
data class QuizAttempt(
    val attemptId: Int,
    val quiz: QuizSummary,
    val questions: List<QuizQuestion>
)

data class QuizReviewItem(
    val questionId: Int,
    val prompt: String,
    val chosenOptionId: Int? = null,
    val chosenLabel: String? = null,
    val correctOptionId: Int = 0,
    val correctLabel: String = "",
    val explanation: String? = null,
    val marks: Int = 1,
    val correct: Boolean = false,
    val answered: Boolean = false
)

data class QuizResult(
    val attemptId: Int,
    val quizId: Int,
    val quizTitle: String,
    val score: Int,
    val maxScore: Int,
    val percent: Int,
    val passed: Boolean,
    val passPercent: Int,
    val durationSeconds: Int,
    val expAwarded: Int,
    val answered: Int,
    /** How many attempts the student has left *after* this one. 0 = enrolled. */
    val attemptsLeft: Int = 0,
    val maxAttempts: Int = 3,
    val review: List<QuizReviewItem> = emptyList()
) {
    val canRetake: Boolean get() = attemptsLeft > 0
}

data class QuizHistoryItem(
    val attemptId: Int,
    val quizId: Int,
    val quizTitle: String,
    val subject: String,
    val grade: String,
    val score: Int,
    val maxScore: Int,
    val percent: Int,
    val passed: Boolean,
    val durationSeconds: Int,
    val expAwarded: Int,
    val submittedAt: String = ""
)

/** Small summary used by the hub header and the EXP screen. */
data class QuizHubStats(
    val published: Int = 0,
    val attempts: Int = 0,
    val best: Int = 0,
    val average: Int = 0,
    val passed: Boolean = false,
    val expFromQuizzes: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
//  Authoring (in-app admin panel)
// ─────────────────────────────────────────────────────────────────────────────

data class QuizDraftOption(
    val id: Long,
    val label: String = "",
    val isCorrect: Boolean = false
)

data class QuizDraftQuestion(
    val id: Long,
    val prompt: String = "",
    val explanation: String = "",
    val marks: Int = 1,
    val mediaUrl: String = "",
    val options: List<QuizDraftOption> = emptyList()
) {
    val isComplete: Boolean
        get() = prompt.isNotBlank() &&
            options.count { it.isCorrect } == 1 &&
            options.all { it.label.isNotBlank() }
}

data class QuizDraft(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val grade: String = "",
    val category: String = "",
    val stream: String = "",
    val medium: String = "English",
    val timeLimitSeconds: Int = 0,
    val passPercent: Int = 60,
    val expReward: Int = 20,
    val maxAttempts: Int = 3,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val questions: List<QuizDraftQuestion> = emptyList()
) {
    /** The first problem that stops this from being saved, or null. */
    val problem: String?
        get() = when {
            title.trim().length < 3 -> "Give the quiz a title (at least 3 characters)."
            subject.isBlank() -> "Choose a subject."
            grade.isBlank() -> "Choose a grade."
            category.isBlank() -> "Choose a category."
            questions.isEmpty() -> "Add at least one question."
            else -> questions.firstOrNull { !it.isComplete }?.let { question ->
                val number = questions.indexOf(question) + 1
                when {
                    question.prompt.isBlank() -> "Question $number needs a question text."
                    question.options.any { it.label.isBlank() } ->
                        "Question $number has an empty option."
                    question.options.count { it.isCorrect } != 1 ->
                        "Question $number must have exactly one correct answer."
                    else -> null
                }
            }
        }

    val isReady: Boolean get() = problem == null
}

// ─────────────────────────────────────────────────────────────────────────────
//  Progress (derived from real attempts, never stored as a stat)
// ─────────────────────────────────────────────────────────────────────────────

data class QuizProgressSummary(
    val attempts: Int = 0,
    val quizzesPublished: Int = 0,
    val quizzesPassed: Int = 0,
    val averagePercent: Int = 0,
    val bestPercent: Int = 0,
    val durationSeconds: Int = 0,
    val expFromQuizzes: Int = 0,
    val streakDays: Int = 0
) {
    /** 0f…1f, for the hero bar. */
    val passedFraction: Float
        get() = if (quizzesPublished <= 0) 0f
        else (quizzesPassed.toFloat() / quizzesPublished.toFloat()).coerceIn(0f, 1f)
}

data class SubjectProgress(
    val subject: String,
    val attempts: Int = 0,
    val accuracy: Int = 0,
    val best: Int = 0,
    val passed: Boolean = false
) {
    val strength: Strength
        get() = when {
            accuracy >= 75 -> Strength.STRONG
            accuracy >= 50 -> Strength.STEADY
            else -> Strength.WEAK
        }

    enum class Strength { STRONG, STEADY, WEAK }
}

/** "Practise next" — always a quiz the student can still open. */
data class PracticeSuggestion(
    val quizId: Int,
    val title: String,
    val subject: String,
    /** -1 when the quiz has never been attempted. */
    val accuracy: Int = -1,
    val attemptsUsed: Int = 0,
    val maxAttempts: Int = 3
)

data class QuizProgress(
    val summary: QuizProgressSummary = QuizProgressSummary(),
    val subjects: List<SubjectProgress> = emptyList(),
    val practice: PracticeSuggestion? = null,
    val recent: List<QuizHistoryItem> = emptyList()
) {
    val isEmpty: Boolean get() = summary.attempts == 0 && recent.isEmpty()
}
