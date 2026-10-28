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
 * The academic step: A/L or O/L, then stream, three subjects, exam year and
 * medium.
 *
 * This is the first piece of the help-matching work. Nothing else in that
 * feature can be honest until the app knows *what* each student studies and in
 * which medium — free-text "Technology" can't tell whether two students share
 * Science for Technology, and a Sinhala-medium student can't help a Tamil-medium
 * one. The server stores one row per subject so the matcher can JOIN on overlap
 * later.
 *
 * O/L is deliberately different: the exam is six compulsory subjects plus one
 * from each of three buckets, so the UI enforces exactly that shape instead of
 * pretending O/L is "pick any three".
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
    var alSubjects by remember(initial) { mutableStateOf(initial.subjects) }
    var olPicks by remember(initial) {
        mutableStateOf(
            initial.subjects
                .mapNotNull { subject -> subject.bucket?.let { it to subject.label } }
                .toMap()
        )
    }

    BackHandler(enabled = !isSaving) { onDismiss() }

    val isAl = level == AcademicCatalog.LEVEL_AL
    val effectiveSubjects: List<AcademicSubject> = if (isAl) {
        alSubjects.take(3)
    } else {
        olPicks.entries.sortedBy { it.key }
            .map { AcademicCatalog.subject(it.value, it.key) }
    }

    val canSave = examYear != null &&
        medium.isNotBlank() &&
        effectiveSubjects.size == 3 &&
        (!isAl || stream.isNotBlank())

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

                    // ── level ────────────────────────────────────────────────
                    AcademicSectionCard(title = "I'm studying for") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AcademicCatalog.levels.forEach { (code, label) ->
                                AcademicChoiceChip(
                                    label = label,
                                    selected = level == code,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(
                                            if (code == AcademicCatalog.LEVEL_AL) {
                                                "academic_level_al"
                                            } else {
                                                "academic_level_ol"
                                            }
                                        ),
                                    onClick = {
                                        if (level != code) {
                                            level = code
                                            // Subjects belong to a level: never
                                            // carry A/L subjects into O/L.
                                            alSubjects = emptyList()
                                            olPicks = emptyMap()
                                            stream = ""
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (isAl) {
                        // ── stream ───────────────────────────────────────────
                        AcademicSectionCard(
                            title = "Stream",
                            subtitle = "Pick the stream on your A/L admission card."
                        ) {
                            AcademicCatalog.alStreams.forEach { name ->
                                AcademicOptionRow(
                                    label = name,
                                    selected = stream == name,
                                    rowTag = "academic_stream_" + AcademicCatalog.codeFor(name),
                                    onClick = {
                                        if (stream != name) {
                                            stream = name
                                            // Different streams share almost no
                                            // subjects, so a stale pick would be
                                            // rejected by the server.
                                            alSubjects = emptyList()
                                        }
                                    }
                                )
                            }
                        }

                        // ── subjects ─────────────────────────────────────────
                        val options = if (stream.isBlank()) emptyList() else {
                            AcademicCatalog.alSubjects(stream)
                        }
                        AcademicSectionCard(
                            title = "Your 3 subjects",
                            subtitle = when {
                                stream.isBlank() -> "Choose a stream first."
                                else -> "${alSubjects.size} of 3 selected"
                            }
                        ) {
                            if (options.isEmpty()) {
                                Text(
                                    text = "Choose a stream first.",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.5.sp,
                                    color = AcademicFaint
                                )
                            } else {
                                options.forEach { subject ->
                                    val chosen = alSubjects.any { it.label == subject }
                                    AcademicOptionRow(
                                        label = subject,
                                        selected = chosen,
                                        // A fourth pick is simply not allowed —
                                        // the server requires exactly three.
                                        enabled = chosen || alSubjects.size < 3,
                                        rowTag = "academic_subject_" + AcademicCatalog.codeFor(subject),
                                        onClick = {
                                            alSubjects = if (chosen) {
                                                alSubjects.filterNot { it.label == subject }
                                            } else if (alSubjects.size < 3) {
                                                alSubjects + AcademicCatalog.subject(subject)
                                            } else {
                                                alSubjects
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // ── O/L buckets ──────────────────────────────────────
                        AcademicSectionCard(
                            title = "Compulsory subjects",
                            subtitle = "These six are already assumed."
                        ) {
                            Text(
                                text = AcademicCatalog.olCompulsorySubjects.joinToString(" · "),
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = AcademicMuted
                            )
                        }

                        AcademicCatalog.olBuckets.forEach { bucket ->
                            AcademicSectionCard(
                                title = bucket.title,
                                subtitle = "Choose one"
                            ) {
                                bucket.subjects.forEach { subject ->
                                    val picked = olPicks[bucket.number] == subject
                                    AcademicOptionRow(
                                        label = subject,
                                        selected = picked,
                                        rowTag = "academic_ol_" + bucket.number + "_" +
                                            AcademicCatalog.codeFor(subject),
                                        onClick = {
                                            olPicks = if (picked) {
                                                olPicks - bucket.number
                                            } else {
                                                olPicks + (bucket.number to subject)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ── exam year ────────────────────────────────────────────
                    AcademicSectionCard(title = "Exam year") {
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
                                    onClick = { examYear = year }
                                )
                            }
                        }
                    }

                    // ── medium ───────────────────────────────────────────────
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
                                    onClick = { medium = name }
                                )
                            }
                        }
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
                                text = when {
                                    isAl && stream.isBlank() -> "Choose your stream to continue."
                                    isAl && alSubjects.size < 3 -> "Choose 3 subjects to continue."
                                    !isAl && olPicks.size < 3 -> "Choose one subject from each bucket."
                                    examYear == null -> "Choose your exam year."
                                    medium.isBlank() -> "Choose your medium."
                                    else -> "Finish the details above to save."
                                },
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.5.sp,
                                color = AcademicMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                if (canSave && !isSaving) {
                                    onSave(
                                        AcademicProfile(
                                            examLevel = level,
                                            streamCode = if (isAl) stream else "",
                                            examYear = examYear,
                                            medium = medium,
                                            subjects = effectiveSubjects
                                        )
                                    )
                                }
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

/** Pill used for level, exam year and medium. */
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
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}
