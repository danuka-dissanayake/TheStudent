package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.QuizDraft
import com.example.data.model.QuizDraftOption
import com.example.data.model.QuizDraftQuestion
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.model.QuizSummary
import com.example.ui.theme.MaisonNeueFontFamily
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val AdmInk = Color(0xFF0F172A)
private val AdmMuted = Color(0xFF64748B)
private val AdmFaint = Color(0xFF94A3B8)
private val AdmHairline = Color(0xFFE8EDF3)
private val AdmBg = Color(0xFFF7F9FC)
private val AdmGreen = Color(0xFF059669)
private val AdmGreenSoft = Color(0xFFECFDF5)
private val AdmRed = Color(0xFFB91C1C)
private val AdmRedSoft = Color(0xFFFEF2F2)
private val AdmAmber = Color(0xFFB45309)
private val AdmAmberSoft = Color(0xFFFFFBEB)

/**
 * Remembers an unfinished quiz on this phone.
 *
 * Authoring a quiz takes minutes of typing; Android may kill the app before the
 * admin presses Save. The draft never leaves the device until Save/Publish, so a
 * half-written quiz can never reach a student.
 */
private object QuizDraftStore {
    private const val PREFS = "quiz_admin_draft_prefs"
    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    private fun adapter() = moshi.adapter(QuizDraft::class.java)

    fun save(context: Context, key: String, draft: QuizDraft) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(key, adapter().toJson(draft))
                .apply()
        }
    }

    fun load(context: Context, key: String): QuizDraft? = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
        if (json.isNullOrBlank()) null else adapter().fromJson(json)
    }.getOrNull()

    fun clear(context: Context, key: String) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
        }
    }
}

private fun draftKey(quizId: Int) = if (quizId <= 0) "new_quiz" else "quiz_$quizId"

/** Convert the wire model into something editable. */
fun quizDraftOf(quiz: QuizSummary?, questions: List<QuizQuestion>): QuizDraft {
    var nextOptionId = 1L
    var nextQuestionId = 1L
    return QuizDraft(
        id = quiz?.id ?: 0,
        title = quiz?.title.orEmpty(),
        heroImageUrl = quiz?.heroImageUrl.orEmpty(),
        description = quiz?.description.orEmpty(),
        subject = quiz?.subject.orEmpty(),
        grade = quiz?.grade.orEmpty(),
        category = quiz?.category.orEmpty(),
        stream = quiz?.stream.orEmpty(),
        medium = quiz?.medium ?: "English",
        timeLimitSeconds = quiz?.timeLimitSeconds ?: 0,
        passPercent = quiz?.passPercent ?: 60,
        expReward = quiz?.expReward ?: 20,
        maxAttempts = quiz?.maxAttempts ?: 3,
        shuffleQuestions = true,
        shuffleOptions = true,
        questions = questions.map { question ->
            QuizDraftQuestion(
                id = nextQuestionId++,
                prompt = question.prompt,
                explanation = question.explanation.orEmpty(),
                marks = question.marks,
                mediaUrl = question.mediaUrl.orEmpty(),
                options = question.options.map { option ->
                    QuizDraftOption(
                        id = nextOptionId++,
                        label = option.label,
                        isCorrect = option.isCorrect
                    )
                }
            )
        }
    )
}

private fun blankQuestion(id: Long): QuizDraftQuestion = QuizDraftQuestion(
    id = id,
    options = listOf(
        QuizDraftOption(id = id * 10 + 1),
        QuizDraftOption(id = id * 10 + 2),
        QuizDraftOption(id = id * 10 + 3),
        QuizDraftOption(id = id * 10 + 4)
    )
)

// ─────────────────────────────────────────────────────────────────────────────
//  Admin list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuizAdminList(
    quizzes: List<QuizSummary>,
    isLoading: Boolean,
    onNew: () -> Unit,
    onEdit: (QuizSummary) -> Unit,
    onStatus: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onResetAttempts: (Int) -> Unit = {},
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf<QuizSummary?>(null) }
    var confirmReset by remember { mutableStateOf<QuizSummary?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Quizzes",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdmInk
                )
                Text(
                    text = "${quizzes.count { it.isPublished }} published · " +
                        "${quizzes.count { it.isDraft }} drafts",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = AdmMuted
                )
            }
            Surface(
                onClick = onRefresh,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, AdmHairline),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Quiz,
                        contentDescription = "Refresh quizzes",
                        tint = AdmInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onNew,
                shape = RoundedCornerShape(50),
                color = AdmGreen,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("quiz_admin_new")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "New quiz",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        when {
            isLoading && quizzes.isEmpty() -> Column(modifier = Modifier.padding(16.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    )
                }
            }
            quizzes.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No quizzes yet",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdmInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Create the first one — students see it the moment you publish.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    color = AdmMuted,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quizzes.forEach { quiz ->
                    QuizAdminRow(
                        quiz = quiz,
                        onEdit = { onEdit(quiz) },
                        onStatus = { status -> onStatus(quiz.id, status) },
                        onDelete = { confirmDelete = quiz },
                        onResetAttempts = if (quiz.attemptCount > 0) {
                            { confirmReset = quiz }
                        } else null
                    )
                }
            }
        }
    }

    confirmReset?.let { quiz ->
        ConfirmResetAttempts(
            quiz = quiz,
            onCancel = { confirmReset = null },
            onConfirm = {
                confirmReset = null
                onResetAttempts(quiz.id)
            }
        )
    }

    confirmDelete?.let { quiz ->
        ConfirmDeleteQuiz(
            quiz = quiz,
            onCancel = { confirmDelete = null },
            onDelete = {
                confirmDelete = null
                onDelete(quiz.id)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizAdminRow(
    quiz: QuizSummary,
    onEdit: () -> Unit,
    onStatus: (String) -> Unit,
    onDelete: () -> Unit,
    onResetAttempts: (() -> Unit)? = null
) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AdmHairline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_admin_row_${quiz.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = quiz.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdmInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(quiz)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(
                    quiz.subject,
                    quiz.grade,
                    "${quiz.questionCount} questions",
                    "${quiz.maxAttempts} attempts each",
                    if (quiz.attemptCount > 0) "${quiz.attemptCount} submitted · avg ${quiz.averagePercent}%" else "no attempts yet"
                ).filter { it.isNotBlank() }.joinToString(" · "),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = AdmMuted
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdminAction(
                    label = if (quiz.isPublished) "Unpublish" else "Publish",
                    tint = AdmGreen,
                    onClick = { onStatus(if (quiz.isPublished) "draft" else "published") }
                )
                AdminAction(label = "Edit", tint = AdmInk, onClick = onEdit)
                if (onResetAttempts != null) {
                    AdminAction(label = "Reset attempts", tint = AdmAmber, onClick = onResetAttempts)
                }
                if (quiz.status != "archived") {
                    AdminAction(label = "Archive", tint = AdmMuted, onClick = { onStatus("archived") })
                }
                AdminAction(label = "Delete", tint = AdmRed, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun AdminAction(label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, AdmHairline),
        modifier = Modifier.height(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(quiz: QuizSummary) {
    val (bg, fg) = when {
        quiz.isPublished -> AdmGreenSoft to AdmGreen
        quiz.status == "archived" -> Color(0xFFF1F5F9) to AdmMuted
        else -> AdmAmberSoft to AdmAmber
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = quiz.statusLabel,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun ConfirmResetAttempts(quiz: QuizSummary, onCancel: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Give every attempt back?",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdmInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "All ${quiz.attemptCount} submitted attempts on \"${quiz.title}\" will be " +
                        "deleted, so every student can take it again from scratch. Best scores and " +
                        "quiz badges go with them.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = AdmMuted
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onCancel,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdmInk
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        color = AdmAmber,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Reset",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteQuiz(quiz: QuizSummary, onCancel: () -> Unit, onDelete: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Delete this quiz?",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdmInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\"${quiz.title}\" and its questions will be removed. A quiz students have " +
                        "already attempted cannot be deleted — archive it instead.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = AdmMuted
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onCancel,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdmInk
                            )
                        }
                    }
                    Surface(
                        onClick = onDelete,
                        shape = RoundedCornerShape(50),
                        color = AdmRed,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Delete",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Editor
// ─────────────────────────────────────────────────────────────────────────────

private val GRADE_OPTIONS = listOf(
    "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11",
    "Grade 12", "Grade 13", "Undergraduate"
)
private val CATEGORY_OPTIONS = listOf("A/L", "O/L", "Junior", "Provincial", "University")
private val MEDIUM_OPTIONS = listOf("English", "Sinhala", "Tamil")
private val TIMER_OPTIONS = listOf(0, 10, 15, 20, 30, 45, 60, 90)
private val PASS_OPTIONS = listOf(40, 50, 60, 70, 80)

@Composable
fun QuizEditorScreen(
    quizId: Int,
    isSaving: Boolean,
    saveError: String?,
    onSave: (QuizDraft, Boolean) -> Unit,
    onClose: () -> Unit,
    /** Upload bytes and return the URL to preview (the path is saved later). */
    onUploadHero: suspend (bytes: ByteArray, fileName: String, mimeType: String) -> Result<String>,
    loadExisting: suspend (Int) -> QuizDraft?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val key = draftKey(quizId)
    var draft by remember(quizId) { mutableStateOf<QuizDraft?>(null) }
    var nextQuestionId by remember(quizId) { mutableStateOf(1000L) }

    LaunchedEffect(quizId) {
        val restored = QuizDraftStore.load(context, key)
        draft = when {
            quizId <= 0 -> restored ?: QuizDraft()
            restored?.id == quizId -> restored
            else -> loadExisting(quizId) ?: QuizDraft(id = quizId)
        }
        nextQuestionId = ((draft?.questions?.maxOfOrNull { it.id } ?: 0L) + 1).coerceAtLeast(1L)
    }

    // Every change is written to this phone; nothing is sent until Save/Publish.
    LaunchedEffect(draft) {
        draft?.let { QuizDraftStore.save(context, key, it) }
    }

    val current = draft
    Surface(color = AdmBg, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, AdmHairline),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close editor",
                            tint = AdmInk,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (quizId <= 0) "New quiz" else "Edit quiz",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdmInk
                    )
                    Text(
                        text = "Draft kept on this phone until you publish",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        color = AdmMuted
                    )
                }
            }

            if (current == null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                        )
                    }
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                saveError?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AdmRedSoft)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp,
                            color = AdmRed
                        )
                    }
                }

                // ── details ────────────────────────────────────────────────
                EditorCard(title = "Details") {
                    EditorField("Title", current.title) { draft = current.copy(title = it) }
                    EditorField("Description (optional)", current.description, singleLine = false) {
                        draft = current.copy(description = it)
                    }
                    EditorField("Subject", current.subject) { draft = current.copy(subject = it) }
                    EditorChips("Grade", GRADE_OPTIONS, current.grade) { draft = current.copy(grade = it) }
                    EditorChips("Category", CATEGORY_OPTIONS, current.category) { draft = current.copy(category = it) }
                    EditorChips("Medium", MEDIUM_OPTIONS, current.medium) { draft = current.copy(medium = it) }
                }

                // ── hero image ─────────────────────────────────────────────
                HeroImageField(
                    value = current.heroImageUrl,
                    onUploaded = { url -> draft = current.copy(heroImageUrl = url) },
                    onClear = { draft = current.copy(heroImageUrl = "") },
                    onUploadHero = onUploadHero
                )

                // ── rules ──────────────────────────────────────────────────
                EditorCard(title = "Rules") {
                    Text(
                        text = "TIME LIMIT",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdmInk,
                        letterSpacing = 0.8.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TIMER_OPTIONS.forEach { option ->
                            val selected = current.timeLimitSeconds == option * 60
                            EditorPill(
                                label = if (option == 0) "No timer" else "$option min",
                                selected = selected
                            ) { draft = current.copy(timeLimitSeconds = option * 60) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "PASS MARK · ${current.passPercent}%",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdmInk,
                        letterSpacing = 0.8.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PASS_OPTIONS.forEach { option ->
                            EditorPill(label = "$option%", selected = current.passPercent == option) {
                                draft = current.copy(passPercent = option)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    EditorChips(
                        "EXP for a first pass",
                        listOf(0, 10, 20, 30, 40).map { it.toString() },
                        current.expReward.toString()
                    ) { draft = current.copy(expReward = it.toIntOrNull() ?: 20) }
                    EditorChips(
                        "Attempts per student",
                        listOf(1, 2, 3, 4, 5).map { it.toString() },
                        current.maxAttempts.toString()
                    ) { draft = current.copy(maxAttempts = it.toIntOrNull() ?: 3) }
                    Text(
                        text = "When every attempt is used the quiz stays open: students keep " +
                            "opening it to read their own results.",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        color = AdmMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    ToggleRow("Shuffle questions", current.shuffleQuestions) {
                        draft = current.copy(shuffleQuestions = it)
                    }
                    ToggleRow("Shuffle options", current.shuffleOptions) {
                        draft = current.copy(shuffleOptions = it)
                    }
                }

                // ── questions ──────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Questions",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdmInk,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${current.questions.size} · " +
                            "${current.questions.sumOf { it.marks }} marks",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = AdmMuted
                    )
                }

                current.questions.forEachIndexed { index, question ->
                    QuestionEditor(
                        index = index,
                        question = question,
                        canMoveUp = index > 0,
                        canMoveDown = index < current.questions.lastIndex,
                        onChange = { updated ->
                            draft = current.copy(
                                questions = current.questions.map {
                                    if (it.id == question.id) updated else it
                                }
                            )
                        },
                        onDelete = {
                            draft = current.copy(questions = current.questions.filter { it.id != question.id })
                        },
                        onMove = { delta ->
                            val list = current.questions.toMutableList()
                            val target = index + delta
                            if (target in list.indices) {
                                list[index] = list[target].also { list[target] = list[index] }
                                draft = current.copy(questions = list)
                            }
                        }
                    )
                }

                Surface(
                    onClick = {
                        draft = current.copy(
                            questions = current.questions + blankQuestion(nextQuestionId)
                        )
                        nextQuestionId += 1
                    },
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.dp, AdmHairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("quiz_editor_add_question")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AdmGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Add question",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdmGreen
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // ── save bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onSave(current, false) },
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.dp, AdmHairline),
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Save draft",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdmInk
                        )
                    }
                }
                Surface(
                    onClick = { onSave(current, true) },
                    shape = RoundedCornerShape(50),
                    color = AdmGreen,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("quiz_editor_publish")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Publish",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionEditor(
    index: Int,
    question: QuizDraftQuestion,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (QuizDraftQuestion) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (question.isComplete) AdmHairline else Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "QUESTION ${index + 1}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdmGreen,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.weight(1f)
                )
                SmallIcon(Icons.Default.ArrowUpward, "Move up", canMoveUp) { onMove(-1) }
                Spacer(Modifier.width(4.dp))
                SmallIcon(Icons.Default.ArrowDownward, "Move down", canMoveDown) { onMove(1) }
                Spacer(Modifier.width(4.dp))
                SmallIcon(Icons.Default.Delete, "Delete question", true, tint = AdmRed, onClick = onDelete)
            }
            Spacer(Modifier.height(8.dp))
            EditorField("Question", question.prompt, singleLine = false) {
                onChange(question.copy(prompt = it))
            }
            question.options.forEachIndexed { optionIndex, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (option.isCorrect) AdmGreen else Color(0xFFF1F5F9))
                            .clickable {
                                onChange(
                                    question.copy(
                                        options = question.options.map {
                                            if (it.id == option.id) it.copy(isCorrect = true)
                                            else it.copy(isCorrect = false)
                                        }
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (option.isCorrect) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Correct answer",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = ('A' + optionIndex).toString(),
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdmMuted
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        EditorField(
                            label = "Option ${('A' + optionIndex)}",
                            value = option.label,
                            compact = true
                        ) { newLabel ->
                            onChange(
                                question.copy(
                                    options = question.options.map {
                                        if (it.id == option.id) it.copy(label = newLabel) else it
                                    }
                                )
                            )
                        }
                    }
                    if (question.options.size > 2) {
                        Spacer(Modifier.width(4.dp))
                        SmallIcon(
                            icon = Icons.Default.Close,
                            description = "Remove option",
                            enabled = true,
                            tint = AdmFaint,
                            onClick = {
                                onChange(question.copy(options = question.options.filter { it.id != option.id }))
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = "+ Add option",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AdmGreen,
                modifier = Modifier
                    .clickable {
                        if (question.options.size < 6) {
                            onChange(
                                question.copy(
                                    options = question.options + QuizDraftOption(
                                        id = question.id * 100 + System.currentTimeMillis() % 1000
                                    )
                                )
                            )
                        }
                    }
                    .padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(6.dp))
            EditorField("Explanation (shown after grading)", question.explanation, singleLine = false) {
                onChange(question.copy(explanation = it))
            }
            EditorChips(
                "Marks",
                listOf("1", "2", "3"),
                question.marks.toString()
            ) { onChange(question.copy(marks = it.toIntOrNull() ?: 1)) }
        }
    }
}

/**
 * The quiz's cover picture, uploaded from the phone.
 *
 * There is deliberately no text field for a link: a hero is a file the admin
 * picks, which the server stores under `uploads/quizzes/`. With nothing picked,
 * the app falls back to the subject photo, so this never blocks saving a quiz.
 */
@Composable
private fun HeroImageField(
    value: String,
    onUploaded: (String) -> Unit,
    onClear: () -> Unit,
    onUploadHero: suspend (bytes: ByteArray, fileName: String, mimeType: String) -> Result<String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Convert the picked content URI to bytes before uploading; the server never
    // sees a URI, only a file it stores itself.
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            error = null
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes == null || bytes.isEmpty()) {
                error = "That image could not be read. Try another one."
                isUploading = false
                return@launch
            }
            if (bytes.size > 5 * 1024 * 1024) {
                error = "That image is larger than 5 MB. Pick a smaller one."
                isUploading = false
                return@launch
            }
            val extension = when (mime) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val result = onUploadHero(bytes, "quiz-hero.$extension", mime)
            result
                .onSuccess { onUploaded(it) }
                .onFailure { error = it.message ?: "The image could not be uploaded." }
            isUploading = false
        }
    }

    EditorCard(title = "Cover image") {
        if (value.isNotBlank()) {
            AsyncImage(
                model = value,
                contentDescription = "Quiz cover image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("quiz_hero_preview")
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AdmBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No image yet — the subject photo is used instead",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = AdmFaint,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isUploading,
                shape = RoundedCornerShape(12.dp),
                color = AdmGreenSoft,
                modifier = Modifier.testTag("quiz_hero_upload")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = AdmGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = AdmGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isUploading -> "Uploading…"
                            value.isBlank() -> "Upload image"
                            else -> "Replace image"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdmGreen
                    )
                }
            }
            if (value.isNotBlank() && !isUploading) {
                Surface(
                    onClick = onClear,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, AdmHairline),
                    modifier = Modifier.testTag("quiz_hero_clear")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = AdmMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Remove",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdmMuted
                        )
                    }
                }
            }
        }

        error?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = AdmRed
            )
        }
    }
}

@Composable
private fun EditorCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AdmHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title.uppercase(),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AdmInk,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    compact: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        label = {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = AdmMuted
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
            color = AdmInk
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 0.dp else 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorChips(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(
        text = label.uppercase(),
        fontFamily = MaisonNeueFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = AdmInk,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            EditorPill(label = option, selected = option == selected) { onSelect(option) }
        }
    }
}

@Composable
private fun EditorPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) AdmInk else Color.White,
        border = if (selected) null else BorderStroke(1.dp, AdmHairline),
        modifier = Modifier.heightIn(min = 32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else AdmMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = AdmInk,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .size(width = 36.dp, height = 21.dp)
                .clip(CircleShape)
                .background(if (checked) AdmGreen else Color(0xFFE2E8F0))
                .padding(horizontal = 2.dp),
            horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun SmallIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    tint: Color = AdmMuted,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else AdmFaint,
            modifier = Modifier.size(14.dp)
        )
    }
}
