package com.example.data.remote

import com.example.data.model.QuizAttempt
import com.example.data.model.QuizHistoryItem
import com.example.data.model.QuizHubStats
import com.example.data.model.PracticeSuggestion
import com.example.data.model.QuizOption
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizResult
import com.example.data.model.QuizProgress
import com.example.data.model.QuizProgressSummary
import com.example.data.model.QuizReviewItem
import com.example.data.model.QuizSummary
import com.example.data.model.SubjectProgress
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────────────────────
//  Wire models — one envelope, like every other TheStudent endpoint
// ─────────────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class QuizEnvelope(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: QuizPayload? = null
)

@JsonClass(generateAdapter = true)
data class QuizPayload(
    @Json(name = "quizzes") val quizzes: List<QuizDto>? = null,
    @Json(name = "quiz") val quiz: QuizDto? = null,
    @Json(name = "questions") val questions: List<QuizQuestionDto>? = null,
    @Json(name = "attempt_id") val attemptId: Int? = null,
    @Json(name = "result") val result: QuizResultDto? = null,
    @Json(name = "attempts") val attempts: List<QuizHistoryDto>? = null,
    @Json(name = "published") val published: Int? = null,
    @Json(name = "attempt_count") val attemptCount: Int? = null,
    @Json(name = "best") val best: Int? = null,
    @Json(name = "average") val average: Int? = null,
    @Json(name = "passed") val passed: Boolean? = null,
    @Json(name = "exp_from_quizzes") val expFromQuizzes: Int? = null,
    @Json(name = "quiz_id") val quizId: Int? = null,
    // progress
    @Json(name = "summary") val summary: QuizProgressSummaryDto? = null,
    @Json(name = "subjects") val subjects: List<SubjectProgressDto>? = null,
    @Json(name = "practice") val practice: PracticeSuggestionDto? = null,
    @Json(name = "recent") val recent: List<QuizHistoryDto>? = null
)

@JsonClass(generateAdapter = true)
data class QuizProgressSummaryDto(
    @Json(name = "attempts") val attempts: Int = 0,
    @Json(name = "quizzes_published") val quizzesPublished: Int = 0,
    @Json(name = "quizzes_passed") val quizzesPassed: Int = 0,
    @Json(name = "average_percent") val averagePercent: Int = 0,
    @Json(name = "best_percent") val bestPercent: Int = 0,
    @Json(name = "duration_seconds") val durationSeconds: Int = 0,
    @Json(name = "exp_from_quizzes") val expFromQuizzes: Int = 0,
    @Json(name = "streak_days") val streakDays: Int = 0
)

@JsonClass(generateAdapter = true)
data class SubjectProgressDto(
    @Json(name = "subject") val subject: String = "",
    @Json(name = "attempts") val attempts: Int = 0,
    @Json(name = "accuracy") val accuracy: Int = 0,
    @Json(name = "best") val best: Int = 0,
    @Json(name = "passed") val passed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PracticeSuggestionDto(
    @Json(name = "quiz_id") val quizId: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "subject") val subject: String = "",
    @Json(name = "accuracy") val accuracy: Int = -1,
    @Json(name = "attempts_used") val attemptsUsed: Int = 0,
    @Json(name = "max_attempts") val maxAttempts: Int = 3
)

@JsonClass(generateAdapter = true)
data class QuizDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "code") val code: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "subject") val subject: String = "",
    @Json(name = "grade") val grade: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "stream") val stream: String? = null,
    @Json(name = "medium") val medium: String = "English",
    @Json(name = "minutes") val minutes: Int = 0,
    @Json(name = "time_limit_seconds") val timeLimitSeconds: Int = 0,
    @Json(name = "pass_percent") val passPercent: Int = 60,
    @Json(name = "exp_reward") val expReward: Int = 20,
    @Json(name = "status") val status: String = "published",
    @Json(name = "question_count") val questionCount: Int = 0,
    @Json(name = "best_percent") val bestPercent: Int? = null,
    @Json(name = "passed") val passed: Boolean = false,
    @Json(name = "attempts") val attempts: Int = 0,
    @Json(name = "max_attempts") val maxAttempts: Int = 3,
    @Json(name = "attempts_left") val attemptsLeft: Int? = null,
    @Json(name = "attempt_count") val attemptCount: Int = 0,
    @Json(name = "average_percent") val averagePercent: Int = 0
) {
    fun toDomain(): QuizSummary = QuizSummary(
        id = id,
        code = code,
        title = title.ifBlank { "Untitled quiz" },
        description = description.orEmpty(),
        subject = subject,
        grade = grade,
        category = category,
        stream = stream,
        medium = medium,
        minutes = minutes,
        timeLimitSeconds = timeLimitSeconds,
        passPercent = passPercent,
        expReward = expReward,
        status = status,
        questionCount = questionCount,
        bestPercent = bestPercent,
        passed = passed,
        attempts = attempts,
        maxAttempts = maxAttempts,
        // Servers older than 1.1 omit the field; derive it rather than guess.
        attemptsLeft = attemptsLeft ?: (maxAttempts - attempts).coerceAtLeast(0),
        attemptCount = attemptCount,
        averagePercent = averagePercent
    )
}

@JsonClass(generateAdapter = true)
data class QuizOptionDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "label") val label: String = "",
    @Json(name = "is_correct") val isCorrect: Boolean = false
) {
    fun toDomain(): QuizOption = QuizOption(id = id, label = label, isCorrect = isCorrect)
}

@JsonClass(generateAdapter = true)
data class QuizQuestionDto(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "prompt") val prompt: String = "",
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "marks") val marks: Int = 1,
    @Json(name = "explanation") val explanation: String? = null,
    @Json(name = "options") val options: List<QuizOptionDto>? = null
) {
    fun toDomain(): QuizQuestion = QuizQuestion(
        id = id,
        position = position,
        prompt = prompt,
        mediaUrl = mediaUrl?.takeIf { it.isNotBlank() },
        marks = if (marks < 1) 1 else marks,
        options = options.orEmpty().map { it.toDomain() },
        explanation = explanation?.takeIf { it.isNotBlank() }
    )
}

@JsonClass(generateAdapter = true)
data class QuizReviewDto(
    @Json(name = "question_id") val questionId: Int = 0,
    @Json(name = "prompt") val prompt: String = "",
    @Json(name = "chosen_option_id") val chosenOptionId: Int? = null,
    @Json(name = "chosen_label") val chosenLabel: String? = null,
    @Json(name = "correct_option_id") val correctOptionId: Int = 0,
    @Json(name = "correct_label") val correctLabel: String = "",
    @Json(name = "explanation") val explanation: String? = null,
    @Json(name = "marks") val marks: Int = 1,
    @Json(name = "correct") val correct: Boolean = false,
    @Json(name = "answered") val answered: Boolean = false
) {
    fun toDomain(): QuizReviewItem = QuizReviewItem(
        questionId = questionId,
        prompt = prompt,
        chosenOptionId = chosenOptionId,
        chosenLabel = chosenLabel?.takeIf { it.isNotBlank() },
        correctOptionId = correctOptionId,
        correctLabel = correctLabel,
        explanation = explanation?.takeIf { it.isNotBlank() },
        marks = marks,
        correct = correct,
        answered = answered
    )
}

@JsonClass(generateAdapter = true)
data class QuizResultDto(
    @Json(name = "attempt_id") val attemptId: Int = 0,
    @Json(name = "quiz_id") val quizId: Int = 0,
    @Json(name = "quiz_title") val quizTitle: String = "",
    @Json(name = "score") val score: Int = 0,
    @Json(name = "max_score") val maxScore: Int = 0,
    @Json(name = "percent") val percent: Int = 0,
    @Json(name = "passed") val passed: Boolean = false,
    @Json(name = "pass_percent") val passPercent: Int = 60,
    @Json(name = "duration_seconds") val durationSeconds: Int = 0,
    @Json(name = "exp_awarded") val expAwarded: Int = 0,
    @Json(name = "answered") val answered: Int = 0,
    @Json(name = "max_attempts") val maxAttempts: Int = 3,
    @Json(name = "attempts_left") val attemptsLeft: Int = 0,
    @Json(name = "review") val review: List<QuizReviewDto>? = null
)

@JsonClass(generateAdapter = true)
data class QuizHistoryDto(
    @Json(name = "attempt_id") val attemptId: Int = 0,
    @Json(name = "quiz_id") val quizId: Int = 0,
    @Json(name = "quiz_title") val quizTitle: String = "",
    @Json(name = "subject") val subject: String = "",
    @Json(name = "grade") val grade: String = "",
    @Json(name = "score") val score: Int = 0,
    @Json(name = "max_score") val maxScore: Int = 0,
    @Json(name = "percent") val percent: Int = 0,
    @Json(name = "passed") val passed: Boolean = false,
    @Json(name = "duration_seconds") val durationSeconds: Int = 0,
    @Json(name = "exp_awarded") val expAwarded: Int = 0,
    @Json(name = "submitted_at") val submittedAt: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
//  Requests
// ─────────────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class QuizStartRequest(
    @Json(name = "quiz_id") val quizId: Int
)

@JsonClass(generateAdapter = true)
data class QuizAnswerRequest(
    @Json(name = "question_id") val questionId: Int,
    @Json(name = "option_id") val optionId: Int
)

@JsonClass(generateAdapter = true)
data class QuizSubmitRequest(
    @Json(name = "attempt_id") val attemptId: Int,
    @Json(name = "answers") val answers: List<QuizAnswerRequest>
)

@JsonClass(generateAdapter = true)
data class QuizSaveOptionRequest(
    @Json(name = "label") val label: String,
    @Json(name = "is_correct") val isCorrect: Boolean
)

@JsonClass(generateAdapter = true)
data class QuizSaveQuestionRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "explanation") val explanation: String? = null,
    @Json(name = "marks") val marks: Int = 1,
    @Json(name = "options") val options: List<QuizSaveOptionRequest>
)

@JsonClass(generateAdapter = true)
data class QuizSaveRequest(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "subject") val subject: String,
    @Json(name = "grade") val grade: String,
    @Json(name = "category") val category: String,
    @Json(name = "stream") val stream: String? = null,
    @Json(name = "medium") val medium: String = "English",
    @Json(name = "time_limit_seconds") val timeLimitSeconds: Int = 0,
    @Json(name = "pass_percent") val passPercent: Int = 60,
    @Json(name = "shuffle_questions") val shuffleQuestions: Boolean = true,
    @Json(name = "shuffle_options") val shuffleOptions: Boolean = true,
    @Json(name = "exp_reward") val expReward: Int = 20,
    @Json(name = "max_attempts") val maxAttempts: Int = 3,
    @Json(name = "status") val status: String = "draft",
    @Json(name = "questions") val questions: List<QuizSaveQuestionRequest>
)

@JsonClass(generateAdapter = true)
data class QuizStatusRequest(
    @Json(name = "id") val id: Int,
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class QuizDeleteRequest(
    @Json(name = "id") val id: Int
)

interface QuizzesApiService {

    @GET("api/quizzes.php")
    suspend fun listQuizzes(
        @Header("Authorization") authorization: String,
        @Query("action") action: String = "list"
    ): QuizEnvelope

    @GET("api/quizzes.php")
    suspend fun quizState(
        @Header("Authorization") authorization: String,
        @Query("action") action: String = "state"
    ): QuizEnvelope

    @POST("api/quizzes.php?action=start")
    suspend fun startQuiz(
        @Header("Authorization") authorization: String,
        @Body request: QuizStartRequest
    ): QuizEnvelope

    @POST("api/quizzes.php?action=submit")
    suspend fun submitQuiz(
        @Header("Authorization") authorization: String,
        @Body request: QuizSubmitRequest
    ): QuizEnvelope

    @GET("api/quizzes.php")
    suspend fun progress(
        @Header("Authorization") authorization: String,
        @Query("action") action: String = "progress"
    ): QuizEnvelope

    @GET("api/quizzes.php")
    suspend fun history(
        @Header("Authorization") authorization: String,
        @Query("action") action: String = "history"
    ): QuizEnvelope

    // ── admin ────────────────────────────────────────────────────────────────

    @GET("api/quizzes.php")
    suspend fun adminList(
        @Header("Authorization") authorization: String,
        @Query("action") action: String = "admin_list"
    ): QuizEnvelope

    @GET("api/quizzes.php")
    suspend fun adminGet(
        @Header("Authorization") authorization: String,
        @Query("action") action: String = "admin_get",
        @Query("id") id: Int
    ): QuizEnvelope

    @POST("api/quizzes.php?action=admin_save")
    suspend fun adminSave(
        @Header("Authorization") authorization: String,
        @Body request: QuizSaveRequest
    ): QuizEnvelope

    @POST("api/quizzes.php?action=admin_status")
    suspend fun adminStatus(
        @Header("Authorization") authorization: String,
        @Body request: QuizStatusRequest
    ): QuizEnvelope

    @POST("api/quizzes.php?action=admin_reset_attempts")
    suspend fun adminResetAttempts(
        @Header("Authorization") authorization: String,
        @Body request: QuizDeleteRequest
    ): QuizEnvelope

    @POST("api/quizzes.php?action=admin_delete")
    suspend fun adminDelete(
        @Header("Authorization") authorization: String,
        @Body request: QuizDeleteRequest
    ): QuizEnvelope
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers shared by the repository
// ─────────────────────────────────────────────────────────────────────────────

internal fun QuizEnvelope.quizzes(): List<QuizSummary> =
    data?.quizzes.orEmpty().map { it.toDomain() }

internal fun QuizEnvelope.attempt(quiz: QuizSummary): QuizAttempt? {
    val payload = data ?: return null
    val attemptId = payload.attemptId ?: return null
    return QuizAttempt(
        attemptId = attemptId,
        quiz = (payload.quiz?.toDomain() ?: quiz).copy(
            questionCount = payload.questions?.size ?: quiz.questionCount
        ),
        questions = payload.questions.orEmpty().map { it.toDomain() }
    )
}

internal fun QuizEnvelope.result(): QuizResult? {
    val dto = data?.result ?: return null
    return QuizResult(
        attemptId = dto.attemptId,
        quizId = dto.quizId,
        quizTitle = dto.quizTitle,
        score = dto.score,
        maxScore = dto.maxScore,
        percent = dto.percent,
        passed = dto.passed,
        passPercent = dto.passPercent,
        durationSeconds = dto.durationSeconds,
        expAwarded = dto.expAwarded,
        answered = dto.answered,
        attemptsLeft = dto.attemptsLeft,
        maxAttempts = dto.maxAttempts,
        review = dto.review.orEmpty().map { it.toDomain() }
    )
}

internal fun QuizHistoryDto.toHistoryItem(): QuizHistoryItem = QuizHistoryItem(
    attemptId = attemptId,
    quizId = quizId,
    quizTitle = quizTitle,
    subject = subject,
    grade = grade,
    score = score,
    maxScore = maxScore,
    percent = percent,
    passed = passed,
    durationSeconds = durationSeconds,
    expAwarded = expAwarded,
    submittedAt = submittedAt
)

internal fun QuizEnvelope.history(): List<QuizHistoryItem> =
    data?.attempts.orEmpty().map {
        QuizHistoryItem(
            attemptId = it.attemptId,
            quizId = it.quizId,
            quizTitle = it.quizTitle,
            subject = it.subject,
            grade = it.grade,
            score = it.score,
            maxScore = it.maxScore,
            percent = it.percent,
            passed = it.passed,
            durationSeconds = it.durationSeconds,
            expAwarded = it.expAwarded,
            submittedAt = it.submittedAt
        )
    }

internal fun QuizEnvelope.hubStats(): QuizHubStats {
    val payload = data
    return QuizHubStats(
        published = payload?.published ?: 0,
        attempts = payload?.attemptCount ?: 0,
        best = payload?.best ?: 0,
        average = payload?.average ?: 0,
        passed = payload?.passed ?: false,
        expFromQuizzes = payload?.expFromQuizzes ?: 0
    )
}

internal fun QuizEnvelope.progress(): QuizProgress {
    val payload = data
    val summaryDto = payload?.summary
    return QuizProgress(
        summary = QuizProgressSummary(
            attempts = summaryDto?.attempts ?: 0,
            quizzesPublished = summaryDto?.quizzesPublished ?: 0,
            quizzesPassed = summaryDto?.quizzesPassed ?: 0,
            averagePercent = summaryDto?.averagePercent ?: 0,
            bestPercent = summaryDto?.bestPercent ?: 0,
            durationSeconds = summaryDto?.durationSeconds ?: 0,
            expFromQuizzes = summaryDto?.expFromQuizzes ?: 0,
            streakDays = summaryDto?.streakDays ?: 0
        ),
        subjects = payload?.subjects.orEmpty().map {
            SubjectProgress(
                subject = it.subject,
                attempts = it.attempts,
                accuracy = it.accuracy,
                best = it.best,
                passed = it.passed
            )
        },
        practice = payload?.practice?.takeIf { it.quizId > 0 }?.let {
            PracticeSuggestion(
                quizId = it.quizId,
                title = it.title,
                subject = it.subject,
                accuracy = it.accuracy,
                attemptsUsed = it.attemptsUsed,
                maxAttempts = it.maxAttempts
            )
        },
        recent = payload?.recent.orEmpty().map { it.toHistoryItem() }
    )
}

internal fun QuizEnvelope.questionsForAdmin(): Pair<QuizSummary?, List<QuizQuestion>> {
    val payload = data
    return Pair(
        payload?.quiz?.toDomain(),
        payload?.questions.orEmpty().map { it.toDomain() }
    )
}
