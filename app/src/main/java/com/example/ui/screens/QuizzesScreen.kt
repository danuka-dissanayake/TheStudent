package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizSummary
import com.example.ui.components.paperCoverTint
import com.example.ui.theme.MaisonNeueFontFamily

private val QuizInk = Color(0xFF0F172A)
private val QuizMuted = Color(0xFF64748B)
private val QuizFaint = Color(0xFF94A3B8)
private val QuizHairline = Color(0xFFE8EDF3)
private val QuizGreen = Color(0xFF059669)
private val QuizGreenSoft = Color(0xFFECFDF5)
private val QuizAmber = Color(0xFFB45309)
private val QuizAmberSoft = Color(0xFFFFFBEB)

private val QUIZ_CHIPS = listOf("All", "A/L", "O/L", "Grades 6–9", "University")

/**
 * The `Quizzes` segment of the Papers tab.
 *
 * Everything here is derived from what the server returned: the chips, the
 * counts, the badges. A student with nothing published sees an empty state that
 * says so, not a demo quiz.
 */
@Composable
fun QuizzesSegment(
    quizzes: List<QuizSummary>,
    isLoading: Boolean,
    isOffline: Boolean,
    error: String?,
    onOpenQuiz: (QuizSummary) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf("All") }
    var onlyPassed by remember { mutableStateOf(false) }
    var onlyNew by remember { mutableStateOf(false) }

    val visible = remember(quizzes, query, chip, onlyPassed, onlyNew) {
        quizzes
            .filter { quiz -> matchesChip(quiz, chip) }
            .filter { quiz -> !onlyPassed || quiz.passed }
            .filter { quiz -> !onlyNew || quiz.attempts == 0 }
            .filter { quiz ->
                query.isBlank() ||
                    quiz.title.contains(query, true) ||
                    quiz.subject.contains(query, true) ||
                    quiz.grade.contains(query, true) ||
                    quiz.medium.contains(query, true)
            }
            .sortedWith(
                compareBy<QuizSummary> { if (it.attempts == 0) 1 else 0 }
                    .thenByDescending { it.bestPercent ?: 0 }
            )
    }

    val filtering = query.isNotBlank() || chip != "All" || onlyPassed || onlyNew

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(50.dp)
                .testTag("quizzes_search_bar"),
            placeholder = {
                Text(
                    text = "Search quizzes, subjects, grades",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    color = QuizFaint
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = QuizMuted, modifier = Modifier.size(19.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = QuizMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { query = "" }
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            textStyle = TextStyle(fontFamily = MaisonNeueFontFamily, fontSize = 13.5.sp, color = QuizInk),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = QuizGreen,
                unfocusedBorderColor = QuizHairline,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUIZ_CHIPS.forEach { option ->
                QuizPill(
                    label = option,
                    selected = !onlyPassed && !onlyNew && chip == option,
                    tag = "quizzes_chip_" + option.lowercase().replace(Regex("[^a-z0-9]"), "_"),
                    onClick = {
                        onlyPassed = false
                        onlyNew = false
                        chip = option
                    }
                )
            }
            QuizPill(
                label = "Passed",
                selected = onlyPassed,
                tag = "quizzes_chip_passed",
                onClick = {
                    onlyNew = false
                    onlyPassed = !onlyPassed
                }
            )
            QuizPill(
                label = "New",
                selected = onlyNew,
                tag = "quizzes_chip_new",
                onClick = {
                    onlyPassed = false
                    onlyNew = !onlyNew
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isLoading -> "Loading…"
                    filtering -> "${visible.size} of ${quizzes.size} quizzes"
                    else -> "${quizzes.size} quizzes"
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuizMuted,
                modifier = Modifier.weight(1f)
            )
            if (filtering) {
                Text(
                    text = "Clear",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuizGreen,
                    modifier = Modifier
                        .clickable {
                            query = ""
                            chip = "All"
                            onlyPassed = false
                            onlyNew = false
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("quizzes_refresh"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh quizzes",
                    tint = QuizInk,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Refresh",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuizInk
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(QuizHairline)
        )

        when {
            isLoading && quizzes.isEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(4) { QuizCardSkeleton() }
            }

            quizzes.isEmpty() -> QuizEmptyState(error = error, isOffline = isOffline, onRefresh = onRefresh)

            visible.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .testTag("quizzes_no_match"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Nothing matches",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = QuizInk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Try another subject or clear the filters.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = QuizMuted
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = {
                        query = ""
                        chip = "All"
                        onlyPassed = false
                        onlyNew = false
                    },
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.dp, QuizHairline),
                    modifier = Modifier.height(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Clear filters",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuizInk,
                            modifier = Modifier.padding(horizontal = 22.dp)
                        )
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("quizzes_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isOffline) {
                    item(key = "offline") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(QuizAmberSoft)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = QuizAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Showing the last quizzes this phone loaded.",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                color = QuizAmber
                            )
                        }
                    }
                }
                items(visible, key = { it.id }) { quiz ->
                    QuizCard(quiz = quiz, onClick = { onOpenQuiz(quiz) })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizCard(quiz: QuizSummary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_card_${quiz.id}")
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            QuizCover(quiz = quiz, modifier = Modifier.size(width = 40.dp, height = 50.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuizInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOf(quiz.subject, quiz.grade, quiz.medium)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = QuizMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuizBadge("${quiz.questionCount} questions", QuizMuted, Color(0xFFF1F5F9))
                    if (quiz.isTimed) {
                        QuizBadge("${quiz.minutes} min", QuizMuted, Color(0xFFF1F5F9))
                    }
                    if (quiz.expReward > 0) {
                        QuizBadge("+${quiz.expReward} EXP", QuizGreen, QuizGreenSoft)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            ScoreChip(quiz)
        }
    }
}

@Composable
private fun ScoreChip(quiz: QuizSummary) {
    val best = quiz.bestPercent
    when {
        quiz.passed -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(QuizGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = QuizGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${best ?: 0}%",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuizGreen
                )
            }
        }
        best != null -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$best",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB91C1C)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "best",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.sp,
                    color = QuizMuted
                )
            }
        }
        else -> QuizBadge("New", QuizGreen, QuizGreenSoft)
    }
}

/** A miniature quiz sheet: coloured header band, ruled lines, subject initial. */
@Composable
private fun QuizCover(quiz: QuizSummary, modifier: Modifier = Modifier) {
    val tint = paperCoverTint(quiz.subject)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x140F172A)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quiz.subject.take(1).uppercase().ifBlank { "Q" },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(tint.copy(alpha = 0.35f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(1.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFCBD5E1))
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizBadge(text: String, color: Color, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun QuizPill(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) QuizInk else Color.White,
        border = if (selected) null else BorderStroke(1.dp, QuizHairline),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else QuizMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuizCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEDF1F6))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEDF1F6))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F5F9))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F5F9))
            )
        }
    }
}

@Composable
private fun QuizEmptyState(error: String?, isOffline: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("quizzes_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Quiz,
                contentDescription = null,
                tint = QuizFaint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (isOffline) "Can't reach the quizzes" else "No quizzes yet",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = QuizInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = error ?: "Quizzes published by your teachers will appear here.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = QuizMuted,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            color = QuizGreen,
            modifier = Modifier
                .height(44.dp)
                .testTag("quizzes_retry")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Try again",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 26.dp)
                )
            }
        }
    }
}

/** Category match for the chips — uses the quiz's own tags, never a guess. */
private fun matchesChip(quiz: QuizSummary, chip: String): Boolean = when (chip) {
    "A/L" -> quiz.category.equals("A/L", true) || quiz.grade == "Grade 12" || quiz.grade == "Grade 13"
    "O/L" -> quiz.category.equals("O/L", true) || quiz.grade == "Grade 10" || quiz.grade == "Grade 11"
    "Grades 6–9" -> quiz.category.contains("Junior", true) ||
        (quiz.grade.startsWith("Grade ") && (quiz.grade.lastOrNull()?.digitToIntOrNull() ?: 0) in 6..9)
    "University" -> quiz.category.contains("University", true) ||
        quiz.grade.contains("Undergraduate", true) || quiz.grade.contains("Year", true)
    else -> true
}
