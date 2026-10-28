package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.TheStudentPreferences
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizDraft
import com.example.data.model.QuizHistoryItem
import com.example.data.model.QuizHubStats
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizResult
import com.example.data.model.QuizSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Result of a quiz call, with "offline" told apart from "the server said no". */
sealed class QuizOutcome<out T> {
    data class Ok<T>(val value: T) : QuizOutcome<T>()
    data class Err(val message: String, val offline: Boolean = false) : QuizOutcome<Nothing>

    fun valueOrNull(): T? = (this as? Ok<T>)?.value
    val errorOrNull: String? get() = (this as? Err)?.message
}

/**
 * Talks to `api/quizzes.php`.
 *
 * Every call needs the signed-in session: a quiz result that is not tied to a
 * student cannot pay EXP or appear in their history, so there is no anonymous
 * mode. Answers are graded on the server — this class never marks a paper.
 */
class QuizzesRepository(context: Context) {

    private val appContext = context.applicationContext
    private val sessionPrefs by lazy { TheStudentPreferences(appContext) }

    /** The papers repository already owns backend-URL storage and validation. */
    private val backend by lazy { PastPapersRemoteRepository(appContext) }

    private val backendUrl: String
        get() = backend.backendBaseUrl

    private var cachedService: QuizzesApiService? = null
    private var cachedBaseUrl: String = ""

    private fun service(): QuizzesApiService {
        val base = backendUrl
        val cached = cachedService
        if (cached != null && cachedBaseUrl == base) {
            return cached
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val service = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(QuizzesApiService::class.java)
        cachedService = service
        cachedBaseUrl = base
        return service
    }

    private fun authorization(): String? {
        val token = sessionPrefs.sessionToken
        return if (token.isNullOrBlank()) null else "Bearer $token"
    }

    private fun <T> failure(error: Throwable): QuizOutcome<T> {
        val (message, offline) = when (error) {
            is HttpException -> {
                val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
                val parsed = runCatching {
                    body?.let { JSONObject(it).optString("message") }
                }.getOrNull()?.takeIf { it.isNotBlank() }
                val fallback = when (error.code()) {
                    401 -> "Please sign in again to take quizzes."
                    403 -> "This action needs an administrator account."
                    404 -> "That quiz is no longer available."
                    503 -> "Quizzes are not set up on this server yet."
                    else -> "The quiz service returned an error (${error.code()})."
                }
                (parsed ?: fallback) to false
            }
            is IOException -> "You're offline. Check your connection and try again." to true
            else -> {
                Log.w(TAG, "Quiz call failed: ${error.message}")
                (error.localizedMessage ?: "Something went wrong.") to false
            }
        }
        return QuizOutcome.Err(message, offline)
    }

    private suspend fun <T> call(block: suspend (String) -> QuizOutcome<T>): QuizOutcome<T> =
        withContext(Dispatchers.IO) {
            val auth = authorization()
                ?: return@withContext QuizOutcome.Err("Sign in to take quizzes.", offline = false)
            try {
                block(auth)
            } catch (error: Exception) {
                failure<T>(error)
            }
        }

    // ── student ──────────────────────────────────────────────────────────────

    suspend fun loadQuizzes(): QuizOutcome<List<QuizSummary>> = call { auth ->
        val envelope = service().listQuizzes(auth)
        if (envelope.status == "success") {
            QuizOutcome.Ok(envelope.quizzes())
        } else {
            QuizOutcome.Err(envelope.message ?: "Quizzes could not be loaded.")
        }
    }

    suspend fun loadState(): QuizOutcome<QuizHubStats> = call { auth ->
        val envelope = service().quizState(auth)
        if (envelope.status == "success") {
            QuizOutcome.Ok(envelope.hubStats())
        } else {
            QuizOutcome.Err(envelope.message ?: "Quiz state could not be loaded.")
        }
    }

    suspend fun start(quiz: QuizSummary): QuizOutcome<QuizAttempt> = call { auth ->
        val envelope = service().startQuiz(auth, QuizStartRequest(quizId = quiz.id))
        val attempt = if (envelope.status == "success") envelope.attempt(quiz) else null
        if (attempt != null && attempt.questions.isNotEmpty()) {
            QuizOutcome.Ok(attempt)
        } else {
            QuizOutcome.Err(envelope.message ?: "This quiz could not be started.")
        }
    }

    suspend fun submit(attemptId: Int, answers: Map<Int, Int>): QuizOutcome<QuizResult> = call { auth ->
        val request = QuizSubmitRequest(
            attemptId = attemptId,
            answers = answers.map { (questionId, optionId) ->
                QuizAnswerRequest(questionId = questionId, optionId = optionId)
            }
        )
        val envelope = service().submitQuiz(auth, request)
        val result = if (envelope.status == "success") envelope.result() else null
        if (result != null) {
            QuizOutcome.Ok(result)
        } else {
            QuizOutcome.Err(envelope.message ?: "Your answers could not be submitted.")
        }
    }

    suspend fun history(): QuizOutcome<List<QuizHistoryItem>> = call { auth ->
        val envelope = service().history(auth)
        if (envelope.status == "success") {
            QuizOutcome.Ok(envelope.history())
        } else {
            QuizOutcome.Err(envelope.message ?: "Quiz history could not be loaded.")
        }
    }

    // ── admin (in-app admin panel) ───────────────────────────────────────────

    suspend fun loadAdminQuizzes(): QuizOutcome<List<QuizSummary>> = call { auth ->
        val envelope = service().adminList(auth)
        if (envelope.status == "success") {
            QuizOutcome.Ok(envelope.quizzes())
        } else {
            QuizOutcome.Err(envelope.message ?: "Quizzes could not be loaded.")
        }
    }

    suspend fun loadForEdit(id: Int): QuizOutcome<Pair<QuizSummary?, List<QuizQuestion>>> = call { auth ->
        val envelope = service().adminGet(auth, id)
        if (envelope.status == "success") {
            QuizOutcome.Ok(envelope.questionsForAdmin())
        } else {
            QuizOutcome.Err(envelope.message ?: "That quiz could not be opened.")
        }
    }

    suspend fun save(draft: QuizDraft, publish: Boolean): QuizOutcome<Int> = call { auth ->
        val envelope = service().adminSave(
            auth,
            draft.toSaveRequest(if (publish) "published" else "draft")
        )
        val id = envelope.data?.quizId ?: 0
        if (envelope.status == "success") {
            QuizOutcome.Ok(id)
        } else {
            QuizOutcome.Err(envelope.message ?: "The quiz could not be saved.")
        }
    }

    suspend fun setStatus(id: Int, status: String): QuizOutcome<Unit> = call { auth ->
        val envelope = service().adminStatus(auth, QuizStatusRequest(id = id, status = status))
        if (envelope.status == "success") {
            QuizOutcome.Ok(Unit)
        } else {
            QuizOutcome.Err(envelope.message ?: "The quiz could not be updated.")
        }
    }

    suspend fun delete(id: Int): QuizOutcome<Unit> = call { auth ->
        val envelope = service().adminDelete(auth, QuizDeleteRequest(id = id))
        if (envelope.status == "success") {
            QuizOutcome.Ok(Unit)
        } else {
            QuizOutcome.Err(envelope.message ?: "The quiz could not be deleted.")
        }
    }

    companion object {
        private const val TAG = "QuizzesRepository"
    }
}

/** The builder's draft, in the shape the API expects. */
internal fun QuizDraft.toSaveRequest(status: String): QuizSaveRequest = QuizSaveRequest(
    id = id,
    title = title.trim(),
    description = description.trim(),
    subject = subject,
    grade = grade,
    category = category,
    stream = stream.takeIf { it.isNotBlank() },
    medium = medium,
    timeLimitSeconds = timeLimitSeconds,
    passPercent = passPercent,
    shuffleQuestions = shuffleQuestions,
    shuffleOptions = shuffleOptions,
    expReward = expReward,
    status = status,
    questions = questions.map { question ->
        QuizSaveQuestionRequest(
            prompt = question.prompt.trim(),
            mediaUrl = question.mediaUrl.takeIf { it.isNotBlank() },
            explanation = question.explanation.trim().takeIf { it.isNotBlank() },
            marks = question.marks,
            options = question.options.map { option ->
                QuizSaveOptionRequest(label = option.label.trim(), isCorrect = option.isCorrect)
            }
        )
    }
)
