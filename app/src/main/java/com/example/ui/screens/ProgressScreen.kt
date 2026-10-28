package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizHistoryItem
import com.example.data.model.QuizProgress
import com.example.data.model.SubjectProgress
import com.example.ui.components.paperCoverTint
import com.example.ui.theme.MaisonNeueFontFamily

private val ProgInk = Color(0xFF0F172A)
private val ProgMuted = Color(0xFF64748B)
private val ProgFaint = Color(0xFF94A3B8)
private val ProgHairline = Color(0xFFE8EDF3)
private val ProgBg = Color(0xFFF7F9FC)
private val ProgGreen = Color(0xFF059669)
private val ProgGreenSoft = Color(0xFFECFDF5)
private val ProgAmber = Color(0xFFB45309)
private val ProgAmberSoft = Color(0xFFFFFBEB)
private val ProgRed = Color(0xFFB91C1C)
private val ProgRedSoft = Color(0xFFFEF2F2)

/**
 * "Your progress" — the feedback loop for quizzes.
 *
 * Every number here is computed by the server from submitted attempts: the
 * average is total marks over total possible, a subject's accuracy is the same
 * arithmetic for that subject only, and the streak counts consecutive days with
 * at least one graded attempt. Nothing on this page is stored as a stat, so it
 * cannot drift from the attempts it describes.
 */
@Composable
fun ProgressScreen(
    progress: QuizProgress,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = ProgBg, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, ProgHairline),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("progress_back")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ProgInk,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your progress",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProgInk,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = when {
                            isLoading && progress.isEmpty -> "Loading your results…"
                            progress.summary.attempts == 0 -> "No quizzes taken yet"
                            else -> "${progress.summary.attempts} attempts · " +
                                "${progress.summary.quizzesPassed} of " +
                                "${progress.summary.quizzesPublished} quizzes passed"
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = ProgMuted
                    )
                }
                Surface(
                    onClick = onRefresh,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, ProgHairline),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("progress_refresh")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh progress",
                            tint = ProgInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ProgHairline)
            )

            when {
                isLoading && progress.isEmpty -> ProgressSkeleton()

                progress.isEmpty -> ProgressEmptyState(error = error, onRefresh = onRefresh)

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 28.dp)
                        .testTag("progress_content")
                ) {
                    Spacer(Modifier.height(14.dp))
                    HeroCard(progress)

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${progress.summary.streakDays}",
                            label = "day streak",
                            tint = ProgAmber,
                            background = ProgAmberSoft,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Default.EmojiEvents,
                            value = "${progress.summary.bestPercent}%",
                            label = "best score",
                            tint = ProgGreen,
                            background = ProgGreenSoft,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            icon = Icons.Default.Star,
                            value = "${progress.summary.expFromQuizzes}",
                            label = "EXP",
                            tint = Color(0xFF7C3AED),
                            background = Color(0xFFF5F3FF),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (progress.subjects.isNotEmpty()) {
                        SectionTitle("Subjects")
                        progress.subjects.forEach { subject ->
                            SubjectRow(subject)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    progress.practice?.let { practice ->
                        SectionTitle("Practise next")
                        PracticeCard(
                            title = practice.title,
                            subject = practice.subject,
                            accuracy = practice.accuracy,
                            attemptsUsed = practice.attemptsUsed,
                            maxAttempts = practice.maxAttempts,
                            onPractice = onPractice
                        )
                    }

                    if (progress.recent.isNotEmpty()) {
                        SectionTitle("Recent attempts")
                        progress.recent.take(5).forEach { item ->
                            RecentRow(item)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(progress: QuizProgress) {
    val summary = progress.summary
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${summary.averagePercent}%",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProgInk,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "average across your quizzes",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = ProgMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(ProgHairline)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${summary.quizzesPassed} passed",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProgGreen
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${(summary.quizzesPublished - summary.quizzesPassed).coerceAtLeast(0)} to go",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProgMuted
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDF1F6))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(summary.passedFraction)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(ProgGreen)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${summary.attempts} attempts · ${formatStudyTime(summary.durationSeconds)} spent answering",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                color = ProgMuted
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = background,
        modifier = modifier.heightIn(min = 74.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ProgInk,
                maxLines = 1
            )
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                color = ProgMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontFamily = MaisonNeueFontFamily,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = ProgInk,
        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
    )
}

@Composable
private fun SubjectRow(subject: SubjectProgress) {
    val tint = when (subject.strength) {
        SubjectProgress.Strength.STRONG -> ProgGreen
        SubjectProgress.Strength.STEADY -> Color(0xFF2563EB)
        SubjectProgress.Strength.WEAK -> ProgAmber
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            if (subject.strength == SubjectProgress.Strength.WEAK) Color(0xFFFDE68A) else ProgHairline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_subject_${subject.subject.lowercase().replace(' ', '_')}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject.subject,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProgInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${subject.accuracy}%",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDF1F6))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((subject.accuracy.coerceIn(0, 100)) / 100f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(tint)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append("${subject.attempts} ")
                    append(if (subject.attempts == 1) "attempt" else "attempts")
                    append(" · best ${subject.best}%")
                    if (subject.passed) append(" · passed")
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = ProgMuted
            )
        }
    }
}

@Composable
private fun PracticeCard(
    title: String,
    subject: String,
    accuracy: Int,
    attemptsUsed: Int,
    maxAttempts: Int,
    onPractice: () -> Unit
) {
    val tint = paperCoverTint(subject)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ProgHairline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_practice")
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subject.take(1).uppercase().ifBlank { "Q" },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProgInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (accuracy >= 0) {
                        "Your weakest topic · $accuracy%"
                    } else {
                        "Not attempted yet · ${maxAttempts - attemptsUsed} attempts left"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = ProgMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onPractice,
                shape = RoundedCornerShape(50),
                color = ProgGreen,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("progress_practice_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Practise",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRow(item: QuizHistoryItem) {
    val (tint, background) = if (item.passed) {
        ProgGreen to ProgGreenSoft
    } else {
        ProgRed to ProgRedSoft
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.quizTitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProgInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOf(
                        item.subject,
                        item.grade,
                        shortDate(item.submittedAt),
                        "${item.score}/${item.maxScore}"
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = ProgMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${item.percent}%",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }
        }
    }
}

@Composable
private fun ProgressEmptyState(error: String?, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("progress_empty"),
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
                tint = ProgFaint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Nothing to show yet",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ProgInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = error ?: "Take a quiz and your subjects, strengths and streak appear here.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = ProgMuted,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            color = ProgGreen,
            modifier = Modifier.height(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Refresh",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 26.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressSkeleton() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                )
            }
        }
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            )
        }
    }
}

/** "1h 42m" / "12m" — study time, never raw seconds. */
private fun formatStudyTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${safe}s"
    }
}

/** "2026-10-28 14:03:11" -> "28 Oct". Falls back to the raw date if unknown. */
private fun shortDate(raw: String): String {
    val datePart = raw.substringBefore(' ').trim()
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val month = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: return datePart
    val day = parts[2].toIntOrNull() ?: return datePart
    return "$day $month"
}
