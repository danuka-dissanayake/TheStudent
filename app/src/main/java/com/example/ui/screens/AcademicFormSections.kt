package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicCatalog
import com.example.data.model.AcademicProfile
import com.example.data.model.AcademicSubject
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

private val FormInk = Color(0xFF0B1220)
private val FormMuted = Color(0xFF64748B)
private val FormFaint = Color(0xFF94A3B8)
private val FormHairline = Color(0xFFE2E8F0)
private val FormSoftMint = Color(0xFFE9F8F0)

/**
 * The academic form, shared by the two places that collect it:
 *
 *  • `ProfileSetupScreen` — the mandatory step after signing up, so a student
 *    arrives at the dashboard with their stage and subjects already known.
 *  • `AcademicSetupScreen` — the same form, later, when they want to change it.
 *
 * One implementation means the two can never disagree about what a stage needs,
 * which subject list applies, or what counts as complete.
 *
 * Controlled component: it renders [profile] and reports every edit through
 * [onChange]; the caller owns the draft.
 *
 * @param flat true inside an existing card (the setup screen has its own white
 *   section cards), false when each section should draw its own floating card.
 */
@Composable
fun AcademicStageForm(
    profile: AcademicProfile,
    onChange: (AcademicProfile) -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    showStageSelector: Boolean = true
) {
    val level = profile.examLevel.ifBlank { AcademicCatalog.LEVEL_AL }
    val isAl = level == AcademicCatalog.LEVEL_AL
    val isOl = level == AcademicCatalog.LEVEL_OL
    val isJunior = level == AcademicCatalog.LEVEL_JUNIOR
    val isUni = level == AcademicCatalog.LEVEL_UNI

    // O/L keeps one pick per bucket; that is the only stage with buckets.
    val olPicks: Map<Int, String> = if (isOl) {
        profile.subjects.mapNotNull { subject -> subject.bucket?.let { it to subject.label } }.toMap()
    } else {
        emptyMap()
    }

    fun setSubjects(labels: List<String>) {
        onChange(profile.copy(subjects = labels.map { AcademicCatalog.subject(it) }))
    }

    fun toggleSubject(label: String) {
        val current = profile.subjects.map { it.label }
        onChange(
            profile.copy(
                subjects = when {
                    current.contains(label) -> profile.subjects.filterNot { it.label == label }
                    profile.subjects.size >= 3 -> profile.subjects
                    else -> profile.subjects + AcademicCatalog.subject(label)
                }
            )
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (flat) 18.dp else 0.dp)) {
        // ── stage ────────────────────────────────────────────────────────────
        if (showStageSelector) {
            AcademicSectionCard(
                title = "Where are you now?",
                subtitle = "This decides which of the rest applies to you.",
                flat = flat
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
                                            // Subjects, streams and years belong to a
                                            // stage: never carry a pick across.
                                            onChange(AcademicProfile(examLevel = info.code))
                                        }
                                    }
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        when {
            isJunior -> JuniorSection(
                studyYear = profile.studyYear,
                medium = profile.medium,
                subjects = profile.subjects,
                flat = flat,
                onYear = { onChange(profile.copy(studyYear = it)) },
                onMedium = { onChange(profile.copy(medium = it)) },
                onSubject = { label -> toggleSubject(label) }
            )

            isOl -> OlSection(
                studyYear = profile.studyYear,
                examYear = profile.examYear,
                medium = profile.medium,
                picks = olPicks,
                flat = flat,
                onYear = { onChange(profile.copy(studyYear = it)) },
                onExamYear = { onChange(profile.copy(examYear = it)) },
                onMedium = { onChange(profile.copy(medium = it)) },
                onPick = { bucket, subject ->
                    // Replace that bucket's pick (or clear it).
                    val kept = profile.subjects.filterNot { it.bucket == bucket }
                    onChange(
                        profile.copy(
                            subjects = if (olPicks[bucket] == subject) {
                                kept
                            } else {
                                kept + AcademicCatalog.subject(subject, bucket)
                            }.sortedBy { it.bucket }
                        )
                    )
                }
            )

            isAl -> AlSection(
                stream = profile.streamCode,
                subjectLabels = profile.subjects.map { it.label },
                studyYear = profile.studyYear,
                examYear = profile.examYear,
                medium = profile.medium,
                flat = flat,
                onStream = { name ->
                    if (name != profile.streamCode) {
                        onChange(profile.copy(streamCode = name, subjects = emptyList()))
                    }
                },
                onSubject = { label -> toggleSubject(label) },
                onYear = { onChange(profile.copy(studyYear = it)) },
                onExamYear = { onChange(profile.copy(examYear = it)) },
                onMedium = { onChange(profile.copy(medium = it)) }
            )

            isUni -> UniSection(
                university = profile.university,
                programme = profile.programme,
                studyYear = profile.studyYear,
                flat = flat,
                onUniversity = { onChange(profile.copy(university = it)) },
                onProgramme = { onChange(profile.copy(programme = it)) },
                onYear = { onChange(profile.copy(studyYear = it)) }
            )
        }
    }
}

/** The single most useful sentence about what is still missing, or "". */
fun academicTodo(profile: AcademicProfile): String {
    if (profile.isComplete) return ""
    return when (profile.examLevel) {
        AcademicCatalog.LEVEL_JUNIOR -> when {
            profile.studyYear == null -> "Choose your grade."
            profile.subjects.isEmpty() -> "Choose at least one subject you want help with."
            else -> "Choose your medium."
        }
        AcademicCatalog.LEVEL_OL -> when {
            profile.examYear == null -> "Choose the year you sit O/L."
            profile.subjects.size < 3 -> "Choose one subject from each bucket."
            else -> "Choose your medium."
        }
        AcademicCatalog.LEVEL_AL -> when {
            profile.streamCode.isBlank() -> "Choose your stream."
            profile.subjects.size < 3 -> "Choose 3 subjects."
            profile.examYear == null -> "Choose the year you sit A/L."
            else -> "Choose your medium."
        }
        AcademicCatalog.LEVEL_UNI -> when {
            profile.university.isBlank() -> "Choose your university."
            profile.programme.isBlank() -> "Choose your field of study."
            else -> "Choose your year of study."
        }
        else -> "Choose your stage to continue."
    }
}

/** Value the legacy `target_exam` column gets for a stage. */
fun targetExamFor(profile: AcademicProfile): String = when (profile.examLevel) {
    AcademicCatalog.LEVEL_JUNIOR -> "Grades 6–9"
    AcademicCatalog.LEVEL_OL -> "G.C.E. O/L"
    AcademicCatalog.LEVEL_AL -> "G.C.E. A/L"
    AcademicCatalog.LEVEL_UNI -> "University"
    else -> "G.C.E. A/L"
}

/**
 * Value the legacy `stream_or_degree` column gets.
 *
 * The structured profile is the real record; this column stays because older
 * builds and the admin panel read it, and it must never be empty (the setup
 * endpoint rejects a blank one).
 */
fun streamOrDegreeFor(profile: AcademicProfile): String = when (profile.examLevel) {
    AcademicCatalog.LEVEL_JUNIOR -> profile.studyYear?.let { "Grade $it" } ?: "Grades 6–9"
    AcademicCatalog.LEVEL_OL -> profile.studyYear?.let { "Grade $it" } ?: "O/L candidate"
    AcademicCatalog.LEVEL_AL -> profile.streamCode.ifBlank { "A/L" }
    AcademicCatalog.LEVEL_UNI -> listOf(profile.university, profile.programme)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "University" }
    else -> ""
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stage sections
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JuniorSection(
    studyYear: Int?,
    medium: String,
    subjects: List<AcademicSubject>,
    flat: Boolean,
    onYear: (Int) -> Unit,
    onMedium: (String) -> Unit,
    onSubject: (String) -> Unit
) {
    AcademicSectionCard(
        title = "Your grade",
        subtitle = "Grades 6–9 are junior secondary: a common curriculum, no national exam.",
        flat = flat
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
        subtitle = "Choose up to three — ${subjects.size} of 3 picked.",
        flat = flat
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

    MediumCard(medium = medium, onMedium = onMedium, flat = flat)
}

@Composable
private fun OlSection(
    studyYear: Int?,
    examYear: Int?,
    medium: String,
    picks: Map<Int, String>,
    flat: Boolean,
    onYear: (Int) -> Unit,
    onExamYear: (Int) -> Unit,
    onMedium: (String) -> Unit,
    onPick: (Int, String) -> Unit
) {
    AcademicSectionCard(
        title = "Your grade",
        subtitle = "O/L is sat at the end of Grade 11.",
        flat = flat
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
        subtitle = "Six for every candidate — already assumed, no need to pick them.",
        flat = flat
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
                        color = FormInk
                    )
                }
            }
        }
    }

    AcademicCatalog.olBuckets.forEach { bucket ->
        AcademicSectionCard(
            title = bucket.title,
            subtitle = "${bucket.hint} · choose one",
            flat = flat
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

    ExamYearCard(examYear = examYear, onExamYear = onExamYear, flat = flat)
    MediumCard(medium = medium, onMedium = onMedium, flat = flat)
}

@Composable
private fun AlSection(
    stream: String,
    subjectLabels: List<String>,
    studyYear: Int?,
    examYear: Int?,
    medium: String,
    flat: Boolean,
    onStream: (String) -> Unit,
    onSubject: (String) -> Unit,
    onYear: (Int) -> Unit,
    onExamYear: (Int) -> Unit,
    onMedium: (String) -> Unit
) {
    AcademicSectionCard(
        title = "Stream",
        subtitle = "Pick the stream on your A/L admission card.",
        flat = flat
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
            subjectLabels.isEmpty() -> "Most ${stream.lowercase()} students take " +
                AcademicCatalog.alTypicalCore(stream).joinToString(" and ") + "."
            else -> "${subjectLabels.size} of 3 selected"
        },
        flat = flat
    ) {
        if (stream.isBlank()) {
            Text(
                text = "Choose a stream first.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                color = FormFaint
            )
        } else {
            AcademicCatalog.alSubjects(stream).forEach { subject ->
                val chosen = subjectLabels.contains(subject)
                AcademicOptionRow(
                    label = subject,
                    selected = chosen,
                    // A fourth pick is simply not allowed — the exam is three.
                    enabled = chosen || subjectLabels.size < 3,
                    rowTag = "academic_subject_" + AcademicCatalog.codeFor(subject),
                    onClick = { onSubject(subject) }
                )
            }
        }
    }

    AcademicSectionCard(
        title = "Sat by everyone",
        subtitle = "These two appear on every A/L admission card but are not part of your three.",
        flat = flat
    ) {
        Text(
            text = AcademicCatalog.alCommonComponents.joinToString(" · "),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = FormMuted
        )
    }

    AcademicSectionCard(
        title = "Your grade",
        subtitle = "A/L is a two-year programme — say which year you are in.",
        flat = flat
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

    ExamYearCard(examYear = examYear, onExamYear = onExamYear, label = "A/L exam year", flat = flat)
    MediumCard(medium = medium, onMedium = onMedium, flat = flat)
}

@Composable
private fun UniSection(
    university: String,
    programme: String,
    studyYear: Int?,
    flat: Boolean,
    onUniversity: (String) -> Unit,
    onProgramme: (String) -> Unit,
    onYear: (Int) -> Unit
) {
    AcademicSectionCard(
        title = "University",
        subtitle = "The seventeen state universities and one honest \"other\".",
        flat = flat
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
        subtitle = "The faculty or programme you are reading for.",
        flat = flat
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
        subtitle = "Most degrees run three or four years; medicine and some others longer.",
        flat = flat
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
    label: String = "Exam year",
    flat: Boolean
) {
    AcademicSectionCard(title = label, subtitle = "The year you sit the exam.", flat = flat) {
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
private fun MediumCard(medium: String, onMedium: (String) -> Unit, flat: Boolean) {
    AcademicSectionCard(
        title = "Medium",
        subtitle = "Help only works in a language you both share.",
        flat = flat
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
        border = if (selected) null else BorderStroke(1.dp, FormHairline),
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
                color = if (selected) Color.White.copy(alpha = 0.75f) else FormFaint
            )
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else FormInk
            )
            Text(
                text = blurb,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                color = if (selected) Color.White.copy(alpha = 0.85f) else FormMuted
            )
        }
    }
}

/**
 * Section shell. `flat` draws just the heading and the content, for a parent
 * that already supplies the white card (the setup screen does).
 */
@Composable
private fun AcademicSectionCard(
    title: String,
    subtitle: String? = null,
    flat: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Text(
            text = title.uppercase(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = FormFaint,
            letterSpacing = 1.1.sp
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = FormMuted
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }

    if (flat) {
        Column(modifier = Modifier.fillMaxWidth(), content = body)
    } else {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                content = body
            )
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
            .background(if (selected) FormSoftMint else Color.Transparent)
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
                selected -> FormInk
                enabled -> FormInk
                else -> FormFaint
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
                border = BorderStroke(1.5.dp, FormHairline),
                modifier = Modifier.size(20.dp)
            ) {}
        }
    }
}

/** Pill used for stage, grade, exam year and medium. */
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
        border = if (selected) null else BorderStroke(1.dp, FormHairline),
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else FormMuted,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}
