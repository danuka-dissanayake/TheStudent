package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicCatalog
import com.example.data.model.AcademicProfile
import com.example.data.model.AcademicSubject
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

// File-private palette so this screen can never drift from the approved look
// and can never collide with the chat palette in another file.
private val AcademicInk = Color(0xFF0B1220)
private val AcademicMuted = Color(0xFF64748B)
private val AcademicFaint = Color(0xFF94A3B8)
private val AcademicHairline = Color(0xFFE2E8F0)
private val AcademicSoftMint = Color(0xFFE9F8F0)

/**
 * The academic step, across every stage of the Sri Lankan system:
 *
 *  • **Grades 6–9** — junior secondary. No national exam, so the profile keeps
 *    the grade, the medium and the subjects the student wants help with.
 *  • **O/L** (Grades 10–11) — six compulsory subjects (shown, not stored, because
 *    everyone sits them) plus exactly one subject from each of the three
 *    optional buckets, and the year of the exam.
 *  • **A/L** (Grades 12–13) — a stream and three subjects. General English and
 *    the Common General Test are printed on the admission card of every
 *    candidate and are not part of the three, so they are stated once, not
 *    counted.
 *  • **University** — the university, the field of study and the year of study.
 *
 * This is the data the help matcher reads. Nothing else in that feature can be
 * honest until the app knows *what* each student studies, at what stage, and in
 * which medium — free-text "Technology" cannot tell whether two students share
 * Science for Technology, and a Sinhala-medium student cannot help a Tamil-medium
 * one.
 */
@Composable
fun AcademicSetupScreen(
    initial: AcademicProfile,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (AcademicProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var level by remember(initial) {
        mutableStateOf(initial.examLevel.ifBlank { AcademicCatalog.LEVEL_AL })
    }
    var stream by remember(initial) { mutableStateOf(initial.streamCode) }
    var examYear by remember(initial) { mutableStateOf(initial.examYear) }
    var medium by remember(initial) { mutableStateOf(initial.medium) }
    var studyYear by remember(initial) { mutableStateOf(initial.studyYear) }
    var university by remember(initial) { mutableStateOf(initial.university) }
    var programme by remember(initial) { mutableStateOf(initial.programme) }
    var subjects by remember(initial) { mutableStateOf(initial.subjects) }
    var olPicks by remember(initial) {
        mutableStateOf(
            initial.subjects
                .mapNotNull { subject -> subject.bucket?.let { it to subject.label } }
                .toMap()
        )
    }

    BackHandler(enabled = !isSaving) { onDismiss() }

    val isAl = level == AcademicCatalog.LEVEL_AL
    val isOl = level == AcademicCatalog.LEVEL_OL
    val isJunior = level == AcademicCatalog.LEVEL_JUNIOR
    val isUni = level == AcademicCatalog.LEVEL_UNI

    val pickedSubjects: List<AcademicSubject> = when {
        isOl -> olPicks.entries.sortedBy { it.key }
            .map { AcademicCatalog.subject(it.value, it.key) }
        isUni -> emptyList()
        else -> subjects.take(3)
    }

    val canSave = when {
        isJunior -> studyYear != null && medium.isNotBlank() && pickedSubjects.isNotEmpty()
        isOl -> examYear != null && medium.isNotBlank() && olPicks.size == 3
        isAl -> stream.isNotBlank() && examYear != null && medium.isNotBlank() && subjects.size == 3
        isUni -> university.isNotBlank() && programme.isNotBlank() && studyYear != null
        else -> false
    }

    /** The single most useful sentence about what is still missing. */
    val todo: String = when {
        canSave -> ""
        isJunior && studyYear == null -> "Choose your grade."
        isJunior && pickedSubjects.isEmpty() -> "Choose at least one subject you want help with."
        isJunior && medium.isBlank() -> "Choose your medium."
        isOl && examYear == null -> "Choose the year you sit O/L."
        isOl && olPicks.size < 3 -> "Choose one subject from each bucket."
        isOl && medium.isBlank() -> "Choose your medium."
        isAl && stream.isBlank() -> "Choose your stream."
        isAl && subjects.size < 3 -> "Choose 3 subjects."
        isAl && examYear == null -> "Choose the year you sit A/L."
        isAl && medium.isBlank() -> "Choose your medium."
        isUni && university.isBlank() -> "Choose your university."
        isUni && programme.isBlank() -> "Choose your field of study."
        isUni && studyYear == null -> "Choose your year of study."
        else -> "Finish the details above to save."
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("academic_setup_screen"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0xFFD7F0E1),
                        0.34f to Color(0xFFE9F7EF),
                        0.72f to Color(0xFFF5FBF8),
                        1.00f to Color(0xFFFDFEFD)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    // ── top bar ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AcademicInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Academic details",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcademicInk
                            )
                            Text(
                                text = "Used to match you with students who can help",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.sp,
                                color = AcademicMuted
                            )
                        }
                    }

                    // ── stage ────────────────────────────────────────────────
                    AcademicSectionCard(
                        title = "Where are you now?",
                        subtitle = "This decides which of the rest applies to you."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AcademicCatalog.levelInfos.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { info ->
                                        StageTile(
                                            label = info.label,
                                            range = info.range,
                                            blurb = info.blurb,
                                            selected = level == info.code,
                                            modifier = Modifier.weight(1f),
                                            tag = when (info.code) {
                                                AcademicCatalog.LEVEL_JUNIOR -> "academic_level_junior"
                                                AcademicCatalog.LEVEL_OL -> "academic_level_ol"
                                                AcademicCatalog.LEVEL_AL -> "academic_level_al"
                                                else -> "academic_level_uni"
                                            },
                                            onClick = {
                                                if (level != info.code) {
                                                    level = info.code
                                                    // Subjects, streams and years belong
                                                    // to a stage: never carry a pick
                                                    // across, or the server will reject it.
                                                    subjects = emptyList()
                                                    olPicks = emptyMap()
                                                    stream = ""
                                                }
                                            }
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    when {
                        isJunior -> JuniorSection(
                            studyYear = studyYear,
                            medium = medium,
                            subjects = subjects,
                            onYear = { studyYear = it },
                            onMedium = { medium = it },
                            onSubject = { label ->
                                val chosen = subjects.any { it.label == label }
                                subjects = if (chosen) {
                                    subjects.filterNot { it.label == label }
                                } else if (subjects.size < 3) {
                                    subjects + AcademicCatalog.subject(label)
                                } else {
                                    subjects
                                }
                            }
                        )

                        isOl -> OlSection(
                            studyYear = studyYear,
                            examYear = examYear,
                            medium = medium,
                            picks = olPicks,
                            onYear = { studyYear = it },
                            onExamYear = { examYear = it },
                            onMedium = { medium = it },
                            onPick = { bucket, subject ->
                                olPicks = if (olPicks[bucket] == subject) {
                                    olPicks - bucket
                                } else {
                                    olPicks + (bucket to subject)
                                }
                            }
                        )

                        isAl -> AlSection(
                            stream = stream,
                            subjects = subjects,
                            studyYear = studyYear,
                            examYear = examYear,
                            medium = medium,
                            onStream = { name ->
                                if (stream != name) {
                                    stream = name
                                    subjects = emptyList()
                                }
                            },
                            onSubject = { label ->
                                val chosen = subjects.any { it.label == label }
                                subjects = if (chosen) {
                                    subjects.filterNot { it.label == label }
                                } else if (subjects.size < 3) {
                                    subjects + AcademicCatalog.subject(label)
                                } else {
                                    subjects
                                }
                            },
                            onYear = { studyYear = it },
                            onExamYear = { examYear = it },
                            onMedium = { medium = it }
                        )

                        isUni -> UniSection(
                            university = university,
                            programme = programme,
                            studyYear = studyYear,
                            onUniversity = { university = it },
                            onProgramme = { programme = it },
                            onYear = { studyYear = it }
                        )
                    }
                }

                // ── sticky footer ───────────────────────────────────────────
                Surface(
                    color = Color.White,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else if (!canSave) {
                            Text(
                                text = todo,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                color = AcademicMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                if (!canSave || isSaving) return@Surface
                                onSave(
                                    AcademicProfile(
                                        examLevel = level,
                                        streamCode = if (isAl) stream else "",
                                        examYear = if (isUni) null else examYear,
                                        medium = if (isUni && medium.isBlank()) "English" else medium,
                                        subjects = pickedSubjects,
                                        studyYear = studyYear,
                                        university = if (isUni) university else "",
                                        programme = if (isUni) programme else ""
                                    )
                                )
                            },
                            enabled = canSave && !isSaving,
                            shape = RoundedCornerShape(50),
                            color = if (canSave && !isSaving) EmeraldPrimary else Color(0xFFCBD5E1),
                            shadowElevation = if (canSave && !isSaving) 8.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("academic_save")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isSaving) "Saving…" else "Save details",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 14.sp,
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
}

// ─────────────────────────────────────────────────────────────────────────────
//  Grades 6–9
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JuniorSection(
    studyYear: Int?,
    medium: String,
    subjects: List<AcademicSubject>,
    onYear: (Int) -> Unit,
    onMedium: (String) -> Unit,
    onSubject: (String) -> Unit
) {
    AcademicSectionCard(
        title = "Your grade",
        subtitle = "Grades 6–9 are junior secondary: a common curriculum, no national exam."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcademicCatalog.juniorGrades.forEach { grade ->
                AcademicChoiceChip(
                    label = "Grade $grade",
                    selected = studyYear == grade,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("academic_grade_$grade"),
                    onClick = { onYear(grade) }
                )
            }
        }
    }

    AcademicSectionCard(
        title = "Subjects you want help with",
        subtitle = "Choose up to three — ${subjects.size} of 3 picked."
    ) {
        AcademicCatalog.juniorSubjects.forEach { subject ->
            val chosen = subjects.any { it.label == subject }
            AcademicOptionRow(
                label = subject,
                selected = chosen,
                enabled = chosen || subjects.size < 3,
                rowTag = "academic_subject_" + AcademicCatalog.codeFor(subject),
                onClick = { onSubject(subject) }
            )
        }
    }

    MediumCard(medium = medium, onMedium = onMedium)
}

// ─────────────────────────────────────────────────────────────────────────────
//  O/L
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OlSection(
    studyYear: Int?,
    examYear: Int?,
    medium: String,
    picks: Map<Int, String>,
    onYear: (Int) -> Unit,
    onExamYear: (Int) -> Unit,
    onMedium: (String) -> Unit,
    onPick: (Int, String) -> Unit
) {
    AcademicSectionCard(
        title = "Your grade",
        subtitle = "O/L is sat at the end of Grade 11."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcademicCatalog.olGrades.forEach { grade ->
                AcademicChoiceChip(
                    label = "Grade $grade",
                    selected = studyYear == grade,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("academic_grade_$grade"),
                    onClick = { onYear(grade) }
                )
            }
        }
    }

    AcademicSectionCard(
        title = "Compulsory subjects",
        subtitle = "Six for every candidate — already assumed, no need to pick them."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AcademicCatalog.olCompulsorySubjects.forEach { subject ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(EmeraldPrimary, CircleShape)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = subject,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        color = AcademicInk
                    )
                }
            }
        }
    }

    AcademicCatalog.olBuckets.forEach { bucket ->
        AcademicSectionCard(
            title = bucket.title,
            subtitle = "${bucket.hint} · choose one"
        ) {
            bucket.subjects.forEach { subject ->
                AcademicOptionRow(
                    label = subject,
                    selected = picks[bucket.number] == subject,
                    rowTag = "academic_ol_" + bucket.number + "_" + AcademicCatalog.codeFor(subject),
                    onClick = { onPick(bucket.number, subject) }
                )
            }
        }
    }

    ExamYearCard(examYear = examYear, onExamYear = onExamYear)
    MediumCard(medium = medium, onMedium = onMedium)
}

// ─────────────────────────────────────────────────────────────────────────────
//  A/L
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AlSection(
    stream: String,
    subjects: List<AcademicSubject>,
    studyYear: Int?,
    examYear: Int?,
    medium: String,
    onStream: (String) -> Unit,
    onSubject: (String) -> Unit,
    onYear: (Int) -> Unit,
    onExamYear: (Int) -> Unit,
    onMedium: (String) -> Unit
) {
    AcademicSectionCard(
        title = "Stream",
        subtitle = "Pick the stream on your A/L admission card."
    ) {
        AcademicCatalog.alStreams.forEach { name ->
            AcademicOptionRow(
                label = name,
                selected = stream == name,
                rowTag = "academic_stream_" + AcademicCatalog.codeFor(name),
                onClick = { onStream(name) }
            )
        }
    }

    AcademicSectionCard(
        title = "Your 3 subjects",
        subtitle = when {
            stream.isBlank() -> "Choose a stream first."
            subjects.isEmpty() -> "Most ${stream.lowercase()} students take " +
                AcademicCatalog.alTypicalCore(stream).joinToString(" and ") + "."
            else -> "${subjects.size} of 3 selected"
        }
    ) {
        if (stream.isBlank()) {
            Text(
                text = "Choose a stream first.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                color = AcademicFaint
            )
        } else {
            AcademicCatalog.alSubjects(stream).forEach { subject ->
                val chosen = subjects.any { it.label == subject }
                AcademicOptionRow(
                    label = subject,
                    selected = chosen,
                    // A fourth pick is simply not allowed — the exam is three.
                    enabled = chosen || subjects.size < 3,
                    rowTag = "academic_subject_" + AcademicCatalog.codeFor(subject),
                    onClick = { onSubject(subject) }
                )
            }
        }
    }

    AcademicSectionCard(
        title = "Sat by everyone",
        subtitle = "These two appear on every A/L admission card but are not part of your three."
    ) {
        Text(
            text = AcademicCatalog.alCommonComponents.joinToString(" · "),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = AcademicMuted
        )
    }

    AcademicSectionCard(
        title = "Your grade",
        subtitle = "A/L is a two-year programme — say which year you are in."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcademicCatalog.alGrades.forEach { grade ->
                AcademicChoiceChip(
                    label = "Grade $grade",
                    selected = studyYear == grade,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("academic_grade_$grade"),
                    onClick = { onYear(grade) }
                )
            }
        }
    }

    ExamYearCard(examYear = examYear, onExamYear = onExamYear, label = "A/L exam year")
    MediumCard(medium = medium, onMedium = onMedium)
}

// ─────────────────────────────────────────────────────────────────────────────
//  University
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UniSection(
    university: String,
    programme: String,
    studyYear: Int?,
    onUniversity: (String) -> Unit,
    onProgramme: (String) -> Unit,
    onYear: (Int) -> Unit
) {
    AcademicSectionCard(
        title = "University",
        subtitle = "The seventeen state universities and one honest \"other\"."
    ) {
        AcademicCatalog.universities.forEach { name ->
            AcademicOptionRow(
                label = name,
                selected = university == name,
                rowTag = "academic_university_" + AcademicCatalog.codeFor(name),
                onClick = { onUniversity(name) }
            )
        }
    }

    AcademicSectionCard(
        title = "Field of study",
        subtitle = "The faculty or programme you are reading for."
    ) {
        AcademicCatalog.degreeFields.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { field ->
                    AcademicChoiceChip(
                        label = field,
                        selected = programme == field,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("academic_field_" + AcademicCatalog.codeFor(field)),
                        onClick = { onProgramme(field) }
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    AcademicSectionCard(
        title = "Year of study",
        subtitle = "Most degrees run three or four years; medicine and some others longer."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AcademicCatalog.studyYears.forEach { year ->
                AcademicChoiceChip(
                    label = "Year $year",
                    selected = studyYear == year,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("academic_year_of_study_$year"),
                    onClick = { onYear(year) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExamYearCard(
    examYear: Int?,
    onExamYear: (Int) -> Unit,
    label: String = "Exam year"
) {
    AcademicSectionCard(title = label, subtitle = "The year you sit the exam.") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AcademicCatalog.examYears().forEach { year ->
                AcademicChoiceChip(
                    label = year.toString(),
                    selected = examYear == year,
                    modifier = Modifier.testTag("academic_year_$year"),
                    onClick = { onExamYear(year) }
                )
            }
        }
    }
}

@Composable
private fun MediumCard(medium: String, onMedium: (String) -> Unit) {
    AcademicSectionCard(
        title = "Medium",
        subtitle = "Help only works in a language you both share."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AcademicCatalog.mediums.forEach { name ->
                AcademicChoiceChip(
                    label = name,
                    selected = medium == name,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("academic_medium_" + AcademicCatalog.codeFor(name)),
                    onClick = { onMedium(name) }
                )
            }
        }
    }
}

/** One of the four stage tiles. */
@Composable
private fun StageTile(
    label: String,
    range: String,
    blurb: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EmeraldPrimary else Color(0xFFF8FAFC),
        border = if (selected) null else BorderStroke(1.dp, AcademicHairline),
        modifier = modifier
            .height(92.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = range.uppercase(),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = if (selected) Color.White.copy(alpha = 0.75f) else AcademicFaint
            )
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else AcademicInk
            )
            Text(
                text = blurb,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                color = if (selected) Color.White.copy(alpha = 0.85f) else AcademicMuted
            )
        }
    }
}

/** White card floating on the wash — same shell as the profile screen. */
@Composable
private fun AcademicSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = title.uppercase(),
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AcademicFaint,
                letterSpacing = 1.1.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = AcademicMuted
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** One selectable row with a mint check when chosen. */
@Composable
private fun AcademicOptionRow(
    label: String,
    selected: Boolean,
    rowTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AcademicSoftMint else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag(rowTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                selected -> AcademicInk
                enabled -> AcademicInk
                else -> AcademicFaint
            },
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            // Hairline ring: no icon, so the row still reads as "tap to choose".
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, AcademicHairline),
                modifier = Modifier.size(20.dp)
            ) {}
        }
    }
}

/** Pill used for level, grade, exam year and medium. */
@Composable
private fun AcademicChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) EmeraldPrimary else Color.White,
        border = if (selected) null else BorderStroke(1.dp, AcademicHairline),
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else AcademicMuted,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}
