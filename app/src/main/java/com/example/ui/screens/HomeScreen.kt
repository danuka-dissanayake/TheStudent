package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SavedCourseEntity
import com.example.data.model.Course
import com.example.data.model.CourseCatalog
import com.example.data.model.PastPaper
import com.example.data.model.QuizProgress
import com.example.data.model.StudyNote
import com.example.data.model.UserProfile
import com.example.ui.components.HomeSkeletonLoading
import com.example.ui.components.PaperCover
import com.example.ui.components.TheStudentNavTab
import com.example.ui.components.paperCoverTint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Palette — one place, so the dashboard has a single visual voice
// ─────────────────────────────────────────────────────────────────────────────

private val HInk = Color(0xFF0F172A)
private val HMuted = Color(0xFF64748B)
private val HFaint = Color(0xFF94A3B8)
private val HHairline = Color(0xFFE8EDF3)
private val HBg = Color(0xFFF7F9FC)
private val HCard = Color.White
private val HGreen = EmeraldPrimary
private val HGreenSoft = Color(0xFFECFDF5)
private val HViolet = Color(0xFF7C3AED)
private val HVioletSoft = Color(0xFFF5F3FF)
private val HBlue = Color(0xFF2563EB)
private val HBlueSoft = Color(0xFFEEF2FF)
private val HAmber = Color(0xFFB45309)
private val HAmberSoft = Color(0xFFFFFBEB)
private val HDeep = Color(0xFF06281F)

private object HT {
    val screen: androidx.compose.ui.unit.TextUnit = 22.sp
    val section: androidx.compose.ui.unit.TextUnit = 16.sp
    val cardTitle: androidx.compose.ui.unit.TextUnit = 14.sp
    val body: androidx.compose.ui.unit.TextUnit = 12.5.sp
    val meta: androidx.compose.ui.unit.TextUnit = 11.5.sp
    val micro: androidx.compose.ui.unit.TextUnit = 11.sp
}

/**
 * The dashboard.
 *
 * Everything numeric here comes from a real source: the hero ring, the pass
 * count, study time and EXP are the same figures the progress page computes;
 * the weekly bars are the student's own attempts bucketed by weekday; the
 * subject rows are their weakest subjects. Nothing is a decorative placeholder —
 * where there is no data yet, the card says so and offers the action that would
 * create it.
 */
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    popularCourses: List<Course>,
    recentNotes: List<StudyNote>,
    recentPapers: List<PastPaper>,
    savedCourses: List<SavedCourseEntity>,
    isLoading: Boolean = false,
    onCourseClick: (Course) -> Unit,
    onPaperClick: (PastPaper) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenLanguageLab: () -> Unit,
    onOpenExp: () -> Unit = {},
    quizProgress: QuizProgress = QuizProgress(),
    onOpenProgress: () -> Unit = {},
    onContinueLearning: () -> Unit = {},
    onOpenCourseCollection: () -> Unit = {},
    onNavigateTab: (TheStudentNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        HomeSkeletonLoading(modifier = modifier)
        return
    }

    val summary = quizProgress.summary
    val practice = quizProgress.practice
    val firstName = userProfile.name.trim().split(" ").firstOrNull().orEmpty()
    val streak = maxOf(summary.streakDays, userProfile.streakDays)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── greeting ────────────────────────────────────────────────────────
        item(key = "greeting") {
            GreetingRow(
                firstName = firstName,
                avatarUrl = userProfile.avatarUrl,
                streakDays = streak,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 14.dp)
                    .testTag("welcome_card")
            )
        }

        // ── hero report card ────────────────────────────────────────────────
        item(key = "hero") {
            HeroReportCard(
                averagePercent = summary.averagePercent,
                quizzesPassed = summary.quizzesPassed,
                quizzesPublished = summary.quizzesPublished,
                studySeconds = summary.durationSeconds,
                exp = summary.expFromQuizzes,
                hasData = summary.attempts > 0,
                onOpenProgress = onOpenProgress,
                onOpenExp = onOpenExp,
                onPractise = { if (practice != null) onContinueLearning() else onOpenProgress() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── quick actions ───────────────────────────────────────────────────
        item(key = "actions") {
            QuickActions(
                onAskAi = onOpenAiAssistant,
                onCourses = onOpenCourseCollection,
                onPapers = { onNavigateTab(TheStudentNavTab.PAPERS) },
                onReport = onOpenProgress,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)
            )
        }

        // ── weekly report ───────────────────────────────────────────────────
        item(key = "report") {
            WeeklyReportCard(
                progress = quizProgress,
                onOpenProgress = onOpenProgress,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)
            )
        }

        // ── what to practise next ───────────────────────────────────────────
        item(key = "practise") {
            PractiseNextCard(
                title = practice?.title,
                subject = practice?.subject,
                accuracy = practice?.accuracy ?: -1,
                attemptsLeft = if (practice != null) {
                    (practice.maxAttempts - practice.attemptsUsed).coerceAtLeast(0)
                } else {
                    0
                },
                onStart = {
                    if (practice != null) onContinueLearning() else onNavigateTab(TheStudentNavTab.PAPERS)
                },
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)
            )
        }

        // ── two promos side by side ─────────────────────────────────────────
        item(key = "promos") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp)
                    // Intrinsic height, so the shorter card stretches to match
                    // its neighbour instead of leaving a ragged bottom edge.
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CoursesPromoCard(
                    onOpen = onOpenCourseCollection,
                    modifier = Modifier.weight(1.15f).fillMaxHeight()
                )
                LanguageLabPromoCard(
                    onOpen = onOpenLanguageLab,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // ── popular courses ─────────────────────────────────────────────────
        if (popularCourses.isNotEmpty()) {
            item(key = "courses_header") {
                SectionHeader(
                    title = "Popular courses",
                    action = "See all",
                    onAction = onOpenCourseCollection,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)
                )
            }
            item(key = "courses_rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(popularCourses, key = { it.id }) { course ->
                        CourseRailCard(course = course, onClick = { onCourseClick(course) })
                    }
                }
            }
        }

        // ── past papers ─────────────────────────────────────────────────────
        if (recentPapers.isNotEmpty()) {
            item(key = "papers_header") {
                SectionHeader(
                    title = "Past papers",
                    action = "See all",
                    onAction = { onNavigateTab(TheStudentNavTab.PAPERS) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)
                )
            }
            item(key = "papers_rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentPapers.take(10), key = { it.id }) { paper ->
                        PaperRailCard(paper = paper, onClick = { onPaperClick(paper) })
                    }
                }
            }
        }

        // ── notes ───────────────────────────────────────────────────────────
        if (recentNotes.isNotEmpty()) {
            item(key = "notes_header") {
                SectionHeader(
                    title = "High-yield notes",
                    action = "${recentNotes.size}",
                    onAction = null,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)
                )
            }
            items(recentNotes.take(3), key = { "note-${it.id}" }) { note ->
                CompactNoteCard(
                    note = note,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                )
            }
        }

        // ── saved courses, when there are any ───────────────────────────────
        if (savedCourses.isNotEmpty()) {
            item(key = "saved_header") {
                SectionHeader(
                    title = "Saved courses",
                    action = "${savedCourses.size}",
                    onAction = null,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)
                )
            }
            item(key = "saved_rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(savedCourses, key = { it.id }) { saved ->
                        SavedCourseChip(
                            title = saved.title.ifBlank { "Saved course" },
                            onClick = {
                                popularCourses.firstOrNull { it.id == saved.id }
                                    ?.let(onCourseClick)
                            }
                        )
                    }
                }
            }
        }

        // ── footer ──────────────────────────────────────────────────────────
        val lastAttempt = quizProgress.recent.firstOrNull()?.submittedAt?.let(::shortDate)
        if (lastAttempt != null) {
            item(key = "footer") {
                Text(
                    text = "Last attempt $lastAttempt",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    color = HFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Greeting
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GreetingRow(
    firstName: String,
    avatarUrl: String,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingForNow(),
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.body,
                color = HMuted
            )
            Text(
                text = firstName.ifBlank { "there" },
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.screen,
                fontWeight = FontWeight.ExtraBold,
                color = HInk,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (streakDays > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(HAmberSoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("home_streak_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = HAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$streakDays day streak",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = HT.micro,
                        fontWeight = FontWeight.Bold,
                        color = HAmber,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
        }
        Avatar(url = avatarUrl, initials = firstName.take(1).uppercase())
    }
}

@Composable
private fun Avatar(url: String, initials: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(HGreenSoft),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = initials.ifBlank { "S" },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HGreen
            )
        }
    }
}

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroReportCard(
    averagePercent: Int,
    quizzesPassed: Int,
    quizzesPublished: Int,
    studySeconds: Int,
    exp: Int,
    hasData: Boolean,
    onOpenProgress: () -> Unit,
    onOpenExp: () -> Unit,
    onPractise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HDeep,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color(0xFF0E7A5F), Color(0xFF0B4A3B), HDeep))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccuracyRing(
                        percent = averagePercent,
                        modifier = Modifier.size(98.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        HeroStat(
                            value = if (quizzesPublished > 0) "$quizzesPassed of $quizzesPublished" else "—",
                            label = "quizzes passed"
                        )
                        HeroDivider()
                        HeroStat(
                            value = if (studySeconds > 0) formatStudyTime(studySeconds) else "—",
                            label = "studied"
                        )
                        HeroDivider()
                        // The EXP figure is the door to the ledger behind it, as
                        // it was when the pill lived in the header.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenExp)
                                .testTag("home_exp_pill")
                        ) {
                            HeroStat(
                                value = if (exp > 0) "$exp XP" else "—",
                                label = "earned from quizzes"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onPractise)
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                        .testTag("hero_cta"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (hasData) "Keep practising" else "Take your first quiz",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Full report",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenProgress)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("home_open_progress")
                )
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = HT.micro,
            color = Color.White.copy(alpha = 0.66f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.13f))
    )
}

/** A white ring on a translucent track. Drawn, so it is crisp at any size. */
@Composable
private fun AccuracyRing(percent: Int, modifier: Modifier = Modifier) {
    val safe = percent.coerceIn(0, 100)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.085f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * (safe / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$safe%",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "avg accuracy",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 9.5.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Quick actions
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActions(
    onAskAi: () -> Unit,
    onCourses: () -> Unit,
    onPapers: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAction(
            icon = Icons.Default.AutoAwesome,
            label = "Ask AI",
            tint = HViolet,
            background = HVioletSoft,
            tag = "home_ai_assistant_card",
            onClick = onAskAi,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.Default.School,
            label = "Courses",
            tint = HBlue,
            background = HBlueSoft,
            tag = "home_course_collection_card",
            onClick = onCourses,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.Default.Description,
            label = "Papers",
            tint = HAmber,
            background = HAmberSoft,
            tag = "home_quick_papers",
            onClick = onPapers,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.Default.Insights,
            label = "Report",
            tint = HGreen,
            background = HGreenSoft,
            tag = "home_quick_report",
            onClick = onReport,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = modifier.heightIn(min = 86.dp).testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.meta,
                fontWeight = FontWeight.Bold,
                color = HInk,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Weekly report
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyReportCard(
    progress: QuizProgress,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects = progress.subjects.take(4)
    val weekly = remember(progress.recent) { attemptsByWeekday(progress.recent.map { it.submittedAt }) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = modifier.fillMaxWidth().testTag("home_weekly_report")
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "This week",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.section,
                    fontWeight = FontWeight.ExtraBold,
                    color = HInk,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenProgress)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = HT.micro,
                        fontWeight = FontWeight.Bold,
                        color = HGreen
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = HGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (subjects.isEmpty()) {
                Text(
                    text = "Take a quiz and your subject accuracy, streak and weekly " +
                        "activity appear here.",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.meta,
                    lineHeight = 17.sp,
                    color = HMuted
                )
            } else {
                Row {
                    Column(
                        modifier = Modifier.weight(1.35f),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        subjects.forEach { subject ->
                            SubjectAccuracyRow(
                                subject = subject.subject,
                                accuracy = subject.accuracy,
                                tint = accuracyTint(subject.accuracy)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(86.dp)
                            .background(HHairline)
                    )
                    Spacer(Modifier.width(12.dp))
                    WeekBars(
                        counts = weekly,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (progress.summary.attempts > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatChip(
                        icon = Icons.Default.EmojiEvents,
                        text = "Best ${progress.summary.bestPercent}%",
                        tint = HGreen,
                        background = HGreenSoft
                    )
                    Spacer(Modifier.width(6.dp))
                    StatChip(
                        icon = Icons.Default.Stars,
                        text = "${progress.summary.attempts} attempts",
                        tint = HViolet,
                        background = HVioletSoft
                    )
                    Spacer(Modifier.width(6.dp))
                    StatChip(
                        icon = Icons.Default.AccessTime,
                        text = formatStudyTime(progress.summary.durationSeconds),
                        tint = HBlue,
                        background = HBlueSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectAccuracyRow(subject: String, accuracy: Int, tint: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subject,
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.meta,
                fontWeight = FontWeight.SemiBold,
                color = HInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$accuracy%",
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.micro,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDF1F6))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(accuracy.coerceIn(0, 100) / 100f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

private fun accuracyTint(accuracy: Int): Color = when {
    accuracy >= 75 -> HGreen
    accuracy >= 50 -> HBlue
    else -> Color(0xFFF59E0B)
}

/** Seven rounded bars, one per weekday, from the student's own attempts. */
@Composable
private fun WeekBars(counts: IntArray, modifier: Modifier = Modifier) {
    val peak = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val labels = listOf("M", "Tu", "We", "Th", "Fr", "Sa", "Su")
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            counts.forEachIndexed { index, count ->
                val fraction = if (count == 0) 0.12f else (count.toFloat() / peak) * 0.86f + 0.14f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(66.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (count == 0) Color(0xFFE7EDF4) else HGreen)
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 8.5.sp,
                    color = HFaint,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    text: String,
    tint: Color,
    background: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1
        )
    }
}

/**
 * Attempts per weekday, Monday first, from the ISO dates the server returns.
 * `java.time` is off the table here (minSdk 24, no desugaring), so this uses
 * Calendar — the one API that is guaranteed on every supported device.
 */
private fun attemptsByWeekday(dates: List<String>): IntArray {
    val counts = IntArray(7)
    if (dates.isEmpty()) return counts
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    dates.forEach { raw ->
        val datePart = raw.substringBefore(' ').trim()
        val parsed = runCatching { parser.parse(datePart) }.getOrNull() ?: return@forEach
        calendar.time = parsed
        // Calendar: Sunday = 1 … Saturday = 7 → Monday-first index.
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val index = (dayOfWeek + 5) % 7
        counts[index] = counts[index] + 1
    }
    return counts
}

// ─────────────────────────────────────────────────────────────────────────────
//  Practise next
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PractiseNextCard(
    title: String?,
    subject: String?,
    accuracy: Int,
    attemptsLeft: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (subject != null) paperCoverTint(subject) else HGreen
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = modifier.fillMaxWidth().testTag("continue_learning_card")
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (subject ?: "Quizzes").uppercase(),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tint,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (attemptsLeft > 0) "$attemptsLeft left" else "Start here",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    color = HMuted
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = title ?: "Pick a quiz to begin",
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.cardTitle,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDF1F6))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((if (accuracy >= 0) accuracy else 0).coerceIn(0, 100) / 100f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(tint)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (accuracy >= 0) "$accuracy%" else "new",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (accuracy >= 0) "Your weakest topic" else "Not attempted yet",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    color = HMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    onClick = onStart,
                    shape = RoundedCornerShape(50),
                    color = HGreen,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("resume_lesson_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Practise",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = HT.meta,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Promo cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CoursesPromoCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = HInk,
        modifier = modifier.heightIn(min = 132.dp)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF134E4A), Color(0xFF065F46))
                )
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("University of Moratuwa", "edX", "freeCodeCamp").forEach { provider ->
                        ProviderTile(name = provider)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${CourseCatalog.courses.size} free courses",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "Moratuwa, MIT and edX, all free",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Browse",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = HT.micro,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderTile(name: String) {
    val provider = CourseCatalog.provider(name)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (provider != null) {
            AsyncImage(
                model = provider.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(17.dp)
            )
        } else {
            Text(
                text = name.take(1),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HInk
            )
        }
    }
}

@Composable
private fun LanguageLabPromoCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = modifier.heightIn(min = 132.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HVioletSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = null,
                    tint = HViolet,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Language lab",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HInk,
                maxLines = 1
            )
            Text(
                text = "Speak, listen, correct",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = HMuted,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Open",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    fontWeight = FontWeight.Bold,
                    color = HViolet
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = HViolet,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rails
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    action: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = HT.section,
            fontWeight = FontWeight.ExtraBold,
            color = HInk,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                text = action,
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.meta,
                fontWeight = FontWeight.Bold,
                color = HGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CourseRailCard(course: Course, onClick: () -> Unit) {
    val tint = paperCoverTint(course.category)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = Modifier
            .width(178.dp)
            .testTag("home_course_${course.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(
                        Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.62f)))
                    )
            ) {
                Text(
                    text = course.title.take(1).uppercase(),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = course.level.displayName,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            Column(modifier = Modifier.padding(11.dp)) {
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = course.instructor,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = HMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = HFaint,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${course.totalLessons} lessons",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.sp,
                        color = HFaint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperRailCard(paper: PastPaper, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("home_paper_${paper.id}")
    ) {
        PaperCover(
            paper = paper,
            corner = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = paper.title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = paper.subject,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.5.sp,
            color = HMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedCourseChip(title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = Modifier.heightIn(min = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = HGreen,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                fontFamily = MaisonNeueFontFamily,
                fontSize = HT.meta,
                fontWeight = FontWeight.SemiBold,
                color = HInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Notes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactNoteCard(note: StudyNote, modifier: Modifier = Modifier) {
    val tint = paperCoverTint(note.subject)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = note.subject.take(1).uppercase(),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.cardTitle,
                    fontWeight = FontWeight.Bold,
                    color = HInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${note.subject} · ${note.author}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = HT.micro,
                    color = HMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(HBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${note.readTimeMinutes} min",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = HMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The full note card, kept public because the Learn tab renders it too. The
 * dashboard uses the compact variant above.
 */
@Composable
fun NoteCard(note: StudyNote) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HCard,
        border = BorderStroke(1.dp, HHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.subject,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = HGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${note.readTimeMinutes} min read",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = HMuted
                )
            }

            Text(
                text = note.title,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = HInk
            )

            Text(
                text = note.contentSummary,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = HMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${note.author}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = HMuted
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = HGreen
                    )
                    Text(
                        text = "${note.downloads}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = HInk
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** "2026-10-27 14:02:11" → "27 Oct". */
private fun shortDate(raw: String): String {
    val parts = raw.substringBefore(' ').trim().split('-')
    if (parts.size != 3) return raw.substringBefore(' ').trim()
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val month = parts[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: return ""
    val day = parts[2].toIntOrNull() ?: return ""
    return "$day $month"
}

private fun formatStudyTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}
