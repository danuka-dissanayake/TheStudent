package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizResult
import com.example.data.model.QuizReviewItem
import com.example.data.model.QuizSummary
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay

private val RunInk = Color(0xFF0F172A)
private val RunMuted = Color(0xFF64748B)
private val RunHairline = Color(0xFFE8EDF3)
private val RunBg = Color(0xFFF7F9FC)
private val RunGreen = Color(0xFF059669)
private val RunGreenSoft = Color(0xFFECFDF5)
private val RunRed = Color(0xFFB91C1C)
private val RunRedSoft = Color(0xFFFEF2F2)
private val RunAmber = Color(0xFFB45309)

/**
 * The quiz runner and its result page.
 *
 * One question at a time: the selection is remembered per question, the timer (if
 * the quiz has one) auto-submits at zero, and grading is never done here — the
 * result arrives from the server.
 */
@Composable
fun QuizRunnerScreen(
    quiz: QuizSummary,
    attempt: QuizAttempt?,
    answers: Map<Int, Int>,
    result: QuizResult?,
    isStarting: Boolean,
    isSubmitting: Boolean,
    onAnswer: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onRetake: () -> Unit,
    onExit: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var index by remember(attempt?.attemptId) { mutableIntStateOf(0) }
    var confirmLeave by remember { mutableStateOf(false) }
    var remaining by remember(attempt?.attemptId, quiz.id) {
        mutableIntStateOf(quiz.timeLimitSeconds)
    }

    // Countdown. A timed quiz submits itself at zero, which is the same call the
    // Submit button makes, so the server sees one code path.
    LaunchedEffect(attempt?.attemptId, result, quiz.timeLimitSeconds) {
        if (quiz.timeLimitSeconds <= 0 || attempt == null) return@LaunchedEffect
        remaining = quiz.timeLimitSeconds
        while (remaining > 0 && result == null) {
            delay(1000)
            remaining -= 1
        }
        if (remaining <= 0 && result == null) onSubmit()
    }

    BackHandler(enabled = true) {
        if (result != null) onBackToList() else confirmLeave = true
    }

    Surface(color = RunBg, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleGhost(
                    icon = Icons.Default.Close,
                    description = if (result != null) "Back to quizzes" else "Leave quiz",
                    onClick = { if (result != null) onBackToList() else confirmLeave = true },
                    tag = "quiz_close"
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RunInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            val base = listOf(quiz.subject, quiz.grade)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                            if (base.isNotBlank()) append(base)
                            if (attempt != null && result == null) {
                                val number = (attempt.quiz.attempts + 1)
                                    .coerceAtMost(quiz.maxAttempts.coerceAtLeast(1))
                                if (isNotEmpty()) append(" · ")
                                append("Attempt $number of ${quiz.maxAttempts}")
                            }
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = RunMuted,
                        maxLines = 1
                    )
                }
                if (quiz.isTimed && result == null && attempt != null) {
                    TimerPill(remaining = remaining)
                }
            }

            when {
                result != null -> QuizResultContent(
                    result = result,
                    isSubmitting = isSubmitting,
                    onRetake = onRetake,
                    onBackToList = onBackToList
                )

                isStarting || attempt == null -> RunnerSkeleton()

                // The server refuses to start a quiz with no questions; this is
                // only a guard so a malformed payload can never index past the end.
                attempt.questions.isEmpty() -> RunnerSkeleton()

                else -> {
                    val questions = attempt.questions
                    val safeIndex = index.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
                    val question = questions[safeIndex]
                    val isLast = safeIndex == questions.lastIndex
                    val answered = questions.count { answers.containsKey(it.id) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // progress
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Question ${safeIndex + 1} of ${questions.size}",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RunMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (question.marks == 1) "1 mark" else "${question.marks} marks",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                color = RunMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((safeIndex + 1).toFloat() / questions.size.toFloat())
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(RunGreen)
                            )
                        }
                        Spacer(Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // question card
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0x140F172A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "QUESTION ${safeIndex + 1}",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RunGreen,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = question.prompt,
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RunInk
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            question.options.forEachIndexed { optionIndex, option ->
                                OptionRow(
                                    letter = ('A' + optionIndex).toString(),
                                    label = option.label,
                                    selected = answers[question.id] == option.id,
                                    onClick = { onAnswer(question.id, option.id) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // footer
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (safeIndex > 0) {
                                    Surface(
                                        onClick = { index = safeIndex - 1 },
                                        shape = RoundedCornerShape(50),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, RunHairline),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Previous",
                                                fontFamily = MaisonNeueFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RunInk
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    onClick = {
                                        if (isLast) onSubmit() else index = safeIndex + 1
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = RunGreen,
                                    modifier = Modifier
                                        .weight(if (safeIndex > 0) 1.4f else 1f)
                                        .height(52.dp)
                                        .testTag(if (isLast) "quiz_submit" else "quiz_next")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isLast && isSubmitting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (isLast) "Submit quiz" else "Next question",
                                                fontFamily = MaisonNeueFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                            if (isLast) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (answered == questions.size) {
                                        "All ${questions.size} answered"
                                    } else {
                                        "${questions.size - answered} unanswered — they are marked wrong"
                                    },
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.sp,
                                    color = if (answered == questions.size) RunGreen else RunMuted,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmLeave) {
        LeaveQuizDialog(
            answered = answers.size,
            total = attempt?.questions?.size ?: quiz.questionCount,
            onStay = { confirmLeave = false },
            onLeave = {
                confirmLeave = false
                onExit()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptionRow(
    letter: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) RunInk else Color.White,
        border = if (selected) null else BorderStroke(1.dp, RunHairline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = RunGreen,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Text(
                        text = letter,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RunMuted
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = if (selected) Color.White else RunInk,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimerPill(remaining: Int) {
    val isUrgent = remaining <= 60
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isUrgent) Color(0xFFFFFBEB) else Color.White)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = if (isUrgent) RunAmber else RunInk,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "%d:%02d".format((remaining / 60).coerceAtLeast(0), (remaining % 60).coerceAtLeast(0)),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUrgent) RunAmber else RunInk
        )
    }
}

@Composable
private fun CircleGhost(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, RunHairline),
        modifier = Modifier
            .size(40.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = RunInk, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RunnerSkeleton() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        )
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun LeaveQuizDialog(
    answered: Int,
    total: Int,
    onStay: () -> Unit,
    onLeave: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onStay) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Leave the quiz?",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RunInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (answered == 0) {
                        "Nothing is submitted until you press Submit. You can come back and start again."
                    } else {
                        "$answered of $total answers are on this screen. They stay until you leave, then the attempt starts over."
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = RunMuted
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onStay,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Keep going",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RunInk
                            )
                        }
                    }
                    Surface(
                        onClick = onLeave,
                        shape = RoundedCornerShape(50),
                        color = RunInk,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Leave",
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

// ─────────────────────────────────────────────────────────────────────────────
//  Result
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizResultContent(
    result: QuizResult,
    isSubmitting: Boolean,
    onRetake: () -> Unit,
    onBackToList: () -> Unit
) {
    var wrongOnly by remember { mutableStateOf(false) }
    val review = remember(result, wrongOnly) {
        if (wrongOnly) result.review.filter { !it.correct } else result.review
    }
    val correctCount = remember(result) { result.review.count { it.correct } }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .testTag("quiz_result")
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(124.dp)) {
                        CircularProgressIndicator(
                            progress = { result.percent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (result.passed) RunGreen else RunRed,
                            trackColor = Color(0xFFEDF1F6),
                            strokeWidth = 10.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${result.percent}%",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RunInk
                            )
                            Text(
                                text = "${result.score} / ${result.maxScore}",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                color = RunMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = result.quizTitle,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RunInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (result.passed) RunGreenSoft else RunRedSoft)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (result.passed) {
                                "Passed · pass mark ${result.passPercent}%"
                            } else {
                                "Not passed · pass mark ${result.passPercent}%"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.passed) RunGreen else RunRed
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (result.canRetake) {
                            "${result.attemptsLeft} of ${result.maxAttempts} attempts left"
                        } else {
                            "All ${result.maxAttempts} attempts used · added to your enrolled quizzes"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = RunMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResultStat("Correct", "$correctCount", RunGreen, RunGreenSoft, Modifier.weight(1f))
                        ResultStat("Answered", "${result.answered}", RunInk, Color(0xFFF8FAFC), Modifier.weight(1f))
                        ResultStat(
                            "Time",
                            formatDuration(result.durationSeconds),
                            RunInk,
                            Color(0xFFF8FAFC),
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            ExpCard(result = result)

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Review",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RunInk,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = { wrongOnly = !wrongOnly },
                    shape = RoundedCornerShape(50),
                    color = if (wrongOnly) RunInk else Color.White,
                    border = if (wrongOnly) null else BorderStroke(1.dp, RunHairline),
                    modifier = Modifier.height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Wrong only",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wrongOnly) Color.White else RunMuted,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (review.isEmpty()) {
                Text(
                    text = "No wrong answers to show.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    color = RunMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                review.forEach { item ->
                    ReviewCard(item)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onRetake,
                shape = RoundedCornerShape(50),
                color = if (result.canRetake) Color.White else Color(0xFFF1F5F9),
                border = if (result.canRetake) BorderStroke(1.dp, RunHairline) else null,
                enabled = !isSubmitting && result.canRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("quiz_retake")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (result.canRetake) "Try again" else "No attempts left",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = if (result.canRetake) 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.canRetake) RunInk else RunMuted
                    )
                }
            }
            Surface(
                onClick = onBackToList,
                shape = RoundedCornerShape(50),
                color = RunGreen,
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp)
                    .testTag("quiz_back_to_list")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Back to quizzes",
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

@Composable
private fun ExpCard(result: QuizResult) {
    val earned = result.expAwarded
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (earned > 0) RunGreenSoft else Color.White,
        border = BorderStroke(1.dp, if (earned > 0) Color(0xFFBBF7D0) else RunHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (earned > 0) RunGreen else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (earned > 0) "+$earned" else "0",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (earned > 0) Color.White else RunMuted
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (earned > 0) "$earned EXP earned" else "No EXP this time",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = RunInk
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (earned > 0) {
                        "Added to your ledger — open Your EXP to see it."
                    } else {
                        "This quiz's EXP was already earned, or today's allowance is used up."
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = RunMuted
                )
            }
        }
    }
}

@Composable
private fun ResultStat(
    label: String,
    value: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.sp,
            color = RunMuted
        )
    }
}

@Composable
private fun ReviewCard(item: QuizReviewItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (item.correct) Color(0xFFBBF7D0) else RunHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (item.correct) RunGreenSoft else RunRedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.correct) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (item.correct) RunGreen else RunRed,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        item.correct -> "Correct"
                        !item.answered -> "Not answered"
                        else -> "Incorrect"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.correct) RunGreen else RunRed
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.prompt,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = RunInk
            )
            if (!item.correct) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your answer: ${item.chosenLabel ?: "not answered"}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = RunRed
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Correct answer: ${item.correctLabel}",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                color = RunGreen
            )
            item.explanation?.let { explanation ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(10.dp)
                ) {
                    Text(
                        text = explanation,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = RunMuted
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return if (safe < 60) "${safe}s" else "${safe / 60}m ${safe % 60}s"
}
