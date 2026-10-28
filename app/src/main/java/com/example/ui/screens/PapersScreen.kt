package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedPaperEntity
import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.example.ui.components.PaperCover
import com.example.ui.components.PaperTileSkeleton
import com.example.ui.theme.MaisonNeueFontFamily

// File-private palette: a calm paper-library surface with one emerald accent.
private val PaperInk = Color(0xFF0F172A)
private val PaperMuted = Color(0xFF64748B)
private val PaperFaint = Color(0xFF94A3B8)
private val PaperHairline = Color(0xFFE8EDF3)
private val PaperBg = Color(0xFFF7F9FC)
private val PaperGreen = Color(0xFF059669)
private val PaperGreenSoft = Color(0xFFECFDF5)

/** How the list is ordered. */
private enum class PaperSort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    MOST_DOWNLOADED("Most downloaded"),
    A_TO_Z("A–Z by subject")
}

/** Everything the filter sheet can narrow, in one place. */
private data class PaperFilters(
    val medium: String = "Any medium",
    val year: String = "Any year",
    val subject: String = "Any subject",
    val markingSchemeOnly: Boolean = false
) {
    val count: Int
        get() = listOf(
            medium != "Any medium",
            year != "Any year",
            subject != "Any subject",
            markingSchemeOnly
        ).count { it }
}

private val QUICK_CHIPS = listOf(
    "All" to "",
    "A/L" to "A/L",
    "O/L" to "O/L",
    "Grades 6–9" to "Junior",
    "Provincial" to "Provincial",
    "Model papers" to "Model"
)

/**
 * Past papers library.
 *
 * Everything on this screen is derived from the papers the server actually
 * returned: the chips, the years, the counts, the sort options and the empty
 * states. There is no bundled sample catalog any more — if the library is empty,
 * the screen says so and offers a retry instead of dressing up invented papers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapersScreen(
    papers: List<PastPaper>,
    savedPapers: List<SavedPaperEntity>,
    selectedLevel: EducationLevel,
    isLoading: Boolean = false,
    isLiveDatabase: Boolean = false,
    serverUrl: String = "",
    onLevelSelected: (EducationLevel) -> Unit = {},
    onPaperClick: (PastPaper) -> Unit,
    onToggleSavePaper: (PastPaper) -> Unit,
    onDownloadPaper: (PastPaper) -> Unit,
    onRefreshPapers: () -> Unit = {},
    onUpdateServerUrl: (String) -> Unit = {},
    onNavigateHome: () -> Unit = {},
    // Quizzes live in the same tab, one segment over.
    quizzes: List<com.example.data.model.QuizSummary> = emptyList(),
    isQuizLoading: Boolean = false,
    isQuizOffline: Boolean = false,
    quizError: String? = null,
    segment: Int = 0,
    onSegmentChange: (Int) -> Unit = {},
    onOpenQuiz: (com.example.data.model.QuizSummary) -> Unit = {},
    onOpenQuizProgress: () -> Unit = {},
    onRefreshQuizzes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var quickChip by remember { mutableStateOf("All") }
    var savedOnly by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(PaperSort.NEWEST) }
    var filters by remember { mutableStateOf(PaperFilters()) }
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    val savedIds = remember(savedPapers) { savedPapers.map { it.id }.toSet() }
    val downloadedIds = remember(savedPapers) {
        savedPapers.filter { it.isDownloaded }.map { it.id }.toSet()
    }

    BackHandler(enabled = query.isNotBlank() || filters.count > 0 || savedOnly) {
        query = ""
        filters = PaperFilters()
        savedOnly = false
        quickChip = "All"
    }

    // Every option below is measured from the data, never hard-coded.
    val years = remember(papers) { papers.map { it.year }.distinct().sortedDescending() }
    val subjects = remember(papers) { papers.map { it.subject }.distinct().sorted() }
    val mediums = remember(papers) { papers.map { it.medium }.filter { it.isNotBlank() }.distinct().sorted() }

    fun matchesChip(paper: PastPaper, chip: String): Boolean = when (chip) {
        "" -> true
        "A/L" -> paper.category.equals("A/L", true) ||
            paper.grade == "Grade 12" || paper.grade == "Grade 13"
        "O/L" -> paper.category.equals("O/L", true) ||
            paper.grade == "Grade 10" || paper.grade == "Grade 11"
        "Junior" -> paper.category.contains("Junior", true) ||
            paper.grade in listOf("Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9")
        "Provincial" -> paper.category.contains("Provincial", true) ||
            paper.examination.contains("Provincial", true) ||
            paper.examType.contains("Term", true)
        "Model" -> paper.category.contains("Prototype", true) ||
            paper.examination.contains("Model", true) ||
            paper.examination.contains("Prototype", true)
        else -> true
    }

    val visible = remember(papers, query, quickChip, savedOnly, sort, filters, savedIds) {
        papers
            .asSequence()
            .filter { paper ->
                matchesChip(paper, QUICK_CHIPS.firstOrNull { it.first == quickChip }?.second.orEmpty())
            }
            .filter { paper -> !savedOnly || savedIds.contains(paper.id) }
            .filter { paper -> filters.medium == "Any medium" || paper.medium.equals(filters.medium, true) }
            .filter { paper -> filters.year == "Any year" || paper.year.toString() == filters.year }
            .filter { paper -> filters.subject == "Any subject" || paper.subject.equals(filters.subject, true) }
            .filter { paper -> !filters.markingSchemeOnly || paper.hasMarkingScheme }
            .filter { paper ->
                query.isBlank() ||
                    paper.title.contains(query, true) ||
                    paper.subject.contains(query, true) ||
                    paper.grade.contains(query, true) ||
                    paper.medium.contains(query, true) ||
                    paper.year.toString().contains(query) ||
                    (paper.stream?.contains(query, true) == true)
            }
            .sortedWith(
                when (sort) {
                    PaperSort.NEWEST -> compareByDescending<PastPaper> { it.year }
                        .thenBy { it.subject }
                    PaperSort.OLDEST -> compareBy<PastPaper> { it.year }
                        .thenBy { it.subject }
                    PaperSort.MOST_DOWNLOADED -> compareByDescending { it.downloadCount }
                    PaperSort.A_TO_Z -> compareBy { it.subject }
                }
            )
            .toList()
    }

    val isFiltering = query.isNotBlank() || quickChip != "All" || savedOnly || filters.count > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBg)
    ) {
        // ── pinned header ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperBg)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Back",
                    onClick = onNavigateHome,
                    tag = "papers_back"
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (segment == 0) "Past papers" else "Quizzes",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaperInk,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = if (segment == 0) {
                            if (isLoading && papers.isEmpty()) {
                                "Loading the library…"
                            } else {
                                "${papers.size} papers · every year, every medium"
                            }
                        } else {
                            if (isQuizLoading && quizzes.isEmpty()) {
                                "Loading quizzes…"
                            } else {
                                "${quizzes.size} quizzes · pass one to earn EXP"
                            }
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        color = PaperMuted
                    )
                }
                if (segment == 0) {
                    CircleIconButton(
                        icon = Icons.Default.FilterList,
                        description = "Filters",
                        onClick = { showFilters = true },
                        tag = "papers_filter_button",
                        badge = filters.count
                    )
                }
            }

            // ── segment pills: papers | quizzes ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentPill(
                    label = "Papers",
                    selected = segment == 0,
                    tag = "segment_papers",
                    onClick = { onSegmentChange(0) }
                )
                SegmentPill(
                    label = "Quizzes",
                    selected = segment == 1,
                    tag = "segment_quizzes",
                    onClick = { onSegmentChange(1) }
                )
            }

            if (segment == 0) {
            // search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(50.dp)
                    .testTag("papers_search_bar"),
                placeholder = {
                    Text(
                        text = "Search papers, subjects, years",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        color = PaperFaint
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = PaperMuted,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = PaperMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { query = "" }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 14.sp,
                    color = PaperInk
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaperGreen,
                    unfocusedBorderColor = PaperHairline,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // quick chips + saved
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_CHIPS.forEach { (label, value) ->
                    PillChip(
                        label = label,
                        selected = !savedOnly && quickChip == label,
                        tag = "papers_chip_" + label.lowercase().replace(Regex("[^a-z0-9]"), "_"),
                        onClick = {
                            savedOnly = false
                            quickChip = label
                        }
                    )
                }
                PillChip(
                    label = "Saved",
                    selected = savedOnly,
                    tag = "papers_chip_saved",
                    onClick = { savedOnly = !savedOnly }
                )
            }

            // result line + sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isLoading -> "Loading…"
                        isFiltering -> "${visible.size} of ${papers.size} papers"
                        else -> "${visible.size} papers"
                    },
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaperMuted,
                    modifier = Modifier.weight(1f)
                )
                if (isFiltering) {
                    Text(
                        text = "Clear",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaperGreen,
                        modifier = Modifier
                            .clickable {
                                query = ""
                                quickChip = "All"
                                savedOnly = false
                                filters = PaperFilters()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("papers_clear_filters")
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showSort = !showSort }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("papers_sort_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = PaperInk,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = sort.label,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaperInk
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PaperHairline)
            )
            }
        }

        // ── body ────────────────────────────────────────────────────────────
        if (segment == 1) {
            QuizzesSegment(
                quizzes = quizzes,
                isLoading = isQuizLoading,
                isOffline = isQuizOffline,
                error = quizError,
                onOpenQuiz = onOpenQuiz,
                onRefresh = onRefreshQuizzes,
                onOpenProgress = onOpenQuizProgress,
                modifier = Modifier.fillMaxSize()
            )
        } else {
        when {
            isLoading && papers.isEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(6) { PaperTileSkeleton() }
            }

            papers.isEmpty() -> LibraryOfflineState(
                serverUrl = serverUrl,
                isLiveDatabase = isLiveDatabase,
                onRefresh = onRefreshPapers,
                onUpdateServerUrl = onUpdateServerUrl
            )

            visible.isEmpty() -> EmptyResultsState(
                onClear = {
                    query = ""
                    quickChip = "All"
                    savedOnly = false
                    filters = PaperFilters()
                }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("papers_screen_content"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isLiveDatabase) {
                    item(key = "offline") {
                        OfflineNotice(onRefresh = onRefreshPapers)
                    }
                }
                items(visible, key = { it.id }) { paper ->
                    PaperRow(
                        paper = paper,
                        isSaved = savedIds.contains(paper.id),
                        isDownloaded = downloadedIds.contains(paper.id),
                        onClick = { onPaperClick(paper) },
                        onToggleSave = { onToggleSavePaper(paper) },
                        onDownload = { onDownloadPaper(paper) }
                    )
                }
            }
        }
        }
    }

    // ── sort sheet ──────────────────────────────────────────────────────────
    if (showSort) {
        ModalBottomSheet(
            onDismissRequest = { showSort = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    text = "Sort papers",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PaperInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PaperSort.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                sort = option
                                showSort = false
                            }
                            .padding(horizontal = 10.dp, vertical = 13.dp)
                            .testTag("papers_sort_" + option.name.lowercase()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (sort == option) FontWeight.Bold else FontWeight.Normal,
                            color = PaperInk,
                            modifier = Modifier.weight(1f)
                        )
                        if (sort == option) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = PaperGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    // ── filter sheet ────────────────────────────────────────────────────────
    if (showFilters) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .testTag("papers_filters_sheet")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Filters",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaperInk,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { filters = PaperFilters() }) {
                        Text(
                            text = "Reset",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaperMuted
                        )
                    }
                }

                FilterGroup(title = "Medium") {
                    (listOf("Any medium") + mediums).forEach { option ->
                        PillChip(
                            label = option,
                            selected = filters.medium == option,
                            tag = "papers_filter_medium_" + option.lowercase().replace(Regex("[^a-z0-9]"), "_"),
                            onClick = { filters = filters.copy(medium = option) }
                        )
                    }
                }

                FilterGroup(title = "Year") {
                    (listOf("Any year") + years.map { it.toString() }).forEach { option ->
                        PillChip(
                            label = option,
                            selected = filters.year == option,
                            tag = "papers_filter_year_$option",
                            onClick = { filters = filters.copy(year = option) }
                        )
                    }
                }

                FilterGroup(title = "Subject") {
                    (listOf("Any subject") + subjects).forEach { option ->
                        PillChip(
                            label = option,
                            selected = filters.subject == option,
                            tag = "papers_filter_subject_" + option.lowercase().replace(Regex("[^a-z0-9]"), "_"),
                            onClick = { filters = filters.copy(subject = option) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { filters = filters.copy(markingSchemeOnly = !filters.markingSchemeOnly) }
                        .padding(vertical = 10.dp)
                        .testTag("papers_filter_marking_scheme"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Only papers with a marking scheme",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.5.sp,
                        color = PaperInk,
                        modifier = Modifier.weight(1f)
                    )
                    SwitchDot(on = filters.markingSchemeOnly)
                }

                Spacer(Modifier.height(18.dp))
                Surface(
                    onClick = { showFilters = false },
                    shape = RoundedCornerShape(50),
                    color = PaperGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("papers_filters_apply")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (visible.isEmpty()) "Show results" else "Show ${visible.size} papers",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** One of the two segments under the header: Papers | Quizzes. */
@Composable
private fun SegmentPill(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) PaperInk else Color.White,
        border = if (selected) null else BorderStroke(1.dp, PaperHairline),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else PaperMuted,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rows and states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaperRow(
    paper: PastPaper,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("papers_row_${paper.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A cover drawn from the paper itself — the thumbnail a user
            // expects to see next to a paper, not a generic icon.
            PaperCover(
                paper = paper,
                modifier = Modifier.size(width = 40.dp, height = 50.dp),
                corner = 10.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaperInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOf(paper.subject, paper.year.toString(), paper.medium)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = PaperMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (paper.hasMarkingScheme) {
                        MiniBadge("Marking scheme", PaperGreen, PaperGreenSoft)
                    }
                    if (isDownloaded) {
                        MiniBadge("Offline", Color(0xFF2563EB), Color(0xFFEFF6FF))
                    }
                    if (paper.grade.isNotBlank()) {
                        MiniBadge(paper.grade, PaperMuted, Color(0xFFF1F5F9))
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconAction(
                    icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    description = if (isSaved) "Remove from saved" else "Save paper",
                    tint = if (isSaved) PaperGreen else PaperMuted,
                    onClick = onToggleSave,
                    tag = "papers_save_${paper.id}"
                )
                Spacer(Modifier.height(6.dp))
                IconAction(
                    icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                    description = "Download paper",
                    tint = if (isDownloaded) PaperGreen else PaperMuted,
                    onClick = onDownload,
                    tag = "papers_download_${paper.id}"
                )
            }
        }
    }
}

@Composable
private fun OfflineNotice(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFFBEB))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = Color(0xFFB45309),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Showing the last papers this phone loaded.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            color = Color(0xFF92400E),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Refresh",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF92400E),
            modifier = Modifier
                .clickable(onClick = onRefresh)
                .padding(start = 8.dp)
        )
    }
}

/**
 * The catalog itself is empty — either the server has nothing yet, the filters
 * on the server side are wrong, or this phone cannot reach it. Say which.
 */
@Composable
private fun LibraryOfflineState(
    serverUrl: String,
    isLiveDatabase: Boolean,
    onRefresh: () -> Unit,
    onUpdateServerUrl: (String) -> Unit
) {
    var showServer by remember { mutableStateOf(false) }
    var urlDraft by remember(serverUrl) { mutableStateOf(serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("papers_empty"),
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
                Icons.Default.Description,
                contentDescription = null,
                tint = PaperFaint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No papers yet",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PaperInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The library returned nothing. If you are on a new server, check the connection; otherwise pull again.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = PaperMuted,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            color = PaperGreen,
            modifier = Modifier
                .height(44.dp)
                .testTag("papers_retry")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Try again",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 26.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (showServer) "Hide server settings" else "Server settings",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = PaperMuted,
            modifier = Modifier
                .clickable { showServer = !showServer }
                .padding(8.dp)
        )
        if (showServer) {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = urlDraft,
                onValueChange = { urlDraft = it },
                singleLine = true,
                label = {
                    Text("Server URL", fontSize = 12.sp, fontFamily = MaisonNeueFontFamily)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = {
                        if (urlDraft.isNotBlank()) {
                            onUpdateServerUrl(urlDraft.trim())
                            onRefresh()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = PaperGreen,
                    modifier = Modifier.height(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Save & retry",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
                Text(
                    text = if (isLiveDatabase) "Connected" else "Not connected",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = PaperMuted,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
private fun EmptyResultsState(onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nothing matches",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PaperInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Try a different subject, year or medium.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.sp,
            color = PaperMuted
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onClear,
            shape = RoundedCornerShape(50),
            color = Color.White,
            border = BorderStroke(1.dp, PaperHairline),
            modifier = Modifier.height(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Clear filters",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaperInk,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Small pieces
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tag: String,
    badge: Int = 0
) {
    Box {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, PaperHairline),
            modifier = Modifier
                .size(40.dp)
                .testTag(tag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = description, tint = PaperInk, modifier = Modifier.size(19.dp))
            }
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(PaperGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PillChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) PaperInk else Color.White,
        border = if (selected) null else BorderStroke(1.dp, PaperHairline),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else PaperMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MiniBadge(text: String, color: Color, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFFF8FAFC),
        modifier = Modifier
            .size(32.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SwitchDot(on: Boolean) {
    Row(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(CircleShape)
            .background(if (on) PaperGreen else Color(0xFFE2E8F0))
            .padding(horizontal = 3.dp),
        horizontalArrangement = if (on) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Text(
            text = title.uppercase(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PaperInk,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chips are individual children; wrapping is handled by FlowRow.
            content()
        }
    }
}
