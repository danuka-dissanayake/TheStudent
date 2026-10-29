package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedPaperEntity
import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.example.data.model.SubjectCodes
import com.example.ui.components.PaperCover
import com.example.ui.components.PaperTileSkeleton
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/**
 * Words a student types that are not the words in the database.
 *
 * Each entry maps what they type to the spellings worth trying. Nothing here
 * invents a match — it only widens a token to the terms the library actually
 * stores, so "maths 2020" still finds "Combined Mathematics 2020".
 */
private val SEARCH_SYNONYMS: Map<String, List<String>> = mapOf(
    "maths" to listOf("mathematics"),
    "math" to listOf("mathematics"),
    "mathematics" to listOf("mathematics", "combined mathematics"),
    "chem" to listOf("chemistry"),
    "bio" to listOf("biology"),
    "phy" to listOf("physics"),
    "phys" to listOf("physics"),
    "eng" to listOf("english"),
    "sin" to listOf("sinhala"),
    "tam" to listOf("tamil"),
    "ict" to listOf("information communication technology", "ict"),
    "agri" to listOf("agricultural", "agriculture"),
    "pastpaper" to listOf("paper"),
    "pastpapers" to listOf("paper"),
    "pp" to listOf("paper"),
    "answers" to listOf("marking scheme", "answers"),
    "answer" to listOf("marking scheme", "answers"),
    "scheme" to listOf("marking scheme", "scheme"),
    "markingscheme" to listOf("marking scheme", "scheme")
)

/** Letters and digits only, lowercased: "A/L 2020" and "al2020" meet here. */
private fun String.squeezedSearch(): String =
    filter { it.isLetterOrDigit() }.lowercase()

private fun String.searchWords(): List<String> =
    split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }.map { it.lowercase() }

/**
 * Everything a row can be found by, as flat strings.
 *
 * Two composite entries are added on purpose: students type "2020 sft" and
 * "sft2020" and "2020sft" interchangeably, and the year and the subject code
 * live in different columns, so `"$code $year"` and `"$code$year"` are what make
 * the unspaced forms match.
 */
private fun PaperEntry.searchFields(): List<String> {
    val paper = this.paper
    val code = SubjectCodes.of(paper.subject).takeIf { it != "PAPER" }.orEmpty()
    val fields = mutableListOf(
        title,
        paper.subject,
        code,
        paper.grade,
        paper.medium,
        paper.year.toString(),
        paper.examType,
        paper.examination,
        paper.category,
        paper.stream.orEmpty(),
        if (isScheme) "marking scheme answers" else ""
    )
    if (code.isNotEmpty()) {
        fields += "$code ${paper.year}"
        fields += "${paper.year} $code"
        fields += "$code${paper.year}"
        fields += "${paper.year}$code"
    }
    if (paper.subject.isNotBlank()) {
        fields += "${paper.subject} ${paper.year}"
        fields += "${paper.year} ${paper.subject}"
    }
    return fields.filter { it.isNotBlank() }
}

/** Does one word of the query appear anywhere in the row? */
private fun matchesToken(token: String, fields: List<String>): Boolean =
    (SEARCH_SYNONYMS[token] ?: listOf(token)).any { alternative ->
        val variant = alternative.squeezedSearch()
        if (variant.isEmpty()) return@any false
        fields.any { field ->
            field.contains(alternative, ignoreCase = true) ||
                field.squeezedSearch().contains(variant) ||
                // A one-letter token would otherwise match almost every row, so
                // short tokens have to start a word instead.
                (variant.length < 2 && field.searchWords().any { it.startsWith(variant) })
        }
    }

/**
 * Does this row answer the search text?
 *
 * The query is split into words and **every** word must be found somewhere in
 * the row — not necessarily in the same field. That is what makes "2020 sft"
 * work: "2020" matches the year column and "sft" matches the subject code, and
 * the row is kept because both matched. Requiring the whole phrase in one field,
 * as the first version did, only ever found rows whose title happened to contain
 * the phrase verbatim.
 */
private fun matchesQuery(entry: PaperEntry, query: String): Boolean {
    if (query.isBlank()) return true
    val tokens = query.searchWords().map { it.squeezedSearch() }.filter { it.isNotEmpty() }.distinct()
    if (tokens.isEmpty()) return true
    val fields = entry.searchFields()
    return tokens.all { token -> matchesToken(token, fields) }
}

/** How the results are laid out. Both are lists; one is compact, one is roomy. */
private enum class PaperLayout(val label: String) {
    LIST("Compact list"),
    CARDS("Card list")
}

/**
 * One row in the library. A paper and its marking scheme are two files, so they
 * are two entries: a student looking for "physics marking scheme" should find it
 * the same way they find the question paper, without opening anything first.
 */
private data class PaperEntry(val paper: PastPaper, val isScheme: Boolean = false) {
    val id: String get() = if (isScheme) "${paper.id}#scheme" else paper.id
    val title: String
        get() = if (isScheme) "${paper.title} — Marking scheme" else paper.title
}

/** Expand papers into paper rows plus one scheme row per available scheme. */
private fun buildPaperEntries(papers: List<PastPaper>): List<PaperEntry> {
    val entries = ArrayList<PaperEntry>(papers.size + papers.size / 2)
    papers.forEach { paper ->
        entries += PaperEntry(paper)
        if (paper.hasMarkingScheme) entries += PaperEntry(paper, isScheme = true)
    }
    return entries
}

/**
 * The screen's type scale, named the same way the dashboard names its own
 * (`HT`): a handful of roles instead of a dozen one-off sizes. Every size on
 * this screen comes from here, so nothing drifts by half a point.
 */
private object PT {
    val screen: TextUnit = 21.sp        // "Past papers" in the top bar
    val section: TextUnit = 18.sp       // sheet headings, empty-state headlines
    val cardTitle: TextUnit = 15.sp     // paper title in the roomy layout
    val body: TextUnit = 14.sp          // paper title in the compact row, buttons
    val meta: TextUnit = 13.sp          // metadata lines, sheet options, body copy
    val micro: TextUnit = 12.sp         // subtitles, badges, page pills
    val overline: TextUnit = 11.sp      // subject code, page label, tiny counters

    val bodyHeight: TextUnit = 19.sp
    val metaHeight: TextUnit = 17.sp
}

/** Fixed page size: the library is read best in small, finishable pages. */
private const val PAPERS_PER_PAGE = 15

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
    onDownloadScheme: (PastPaper) -> Unit = {},
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
    // What the field shows, and what the list actually filters on. Filtering on
    // every keystroke made a 1,200-row library re-sort while the student was
    // still typing; the applied query now waits for a pause.
    var queryInput by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var quickChip by remember { mutableStateOf("All") }
    var savedOnly by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(PaperSort.NEWEST) }
    var filters by remember { mutableStateOf(PaperFilters()) }
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf(PaperLayout.LIST) }
    var page by remember { mutableStateOf(1) }
    // A short, deliberate shimmer whenever the list is about to change: the
    // screen never jumps from one set of rows straight to another.
    var isSettling by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Where the search row sits, so a tap anywhere else can be told apart from a
    // tap on the field itself. Both boxes are filled in by layout passes below.
    var searchFieldBounds by remember { mutableStateOf(Rect.Zero) }
    var screenBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val settleScope = rememberCoroutineScope()

    // Search runs when the student says so — Enter on the keyboard, or the
    // search button beside the field. Typing no longer re-filters the library
    // under their fingers. The shimmer is raised here as well as by the query
    // effect, so submitting the *same* words again still answers with a refresh.
    fun submitSearch() {
        query = queryInput.trim()
        isSettling = true
        settleScope.launch {
            delay(320)
            isSettling = false
        }
        focusManager.clearFocus()
        keyboard?.hide()
    }

    fun clearSearch() {
        queryInput = ""
        query = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }

    LaunchedEffect(query, quickChip, savedOnly, filters, sort) {
        isSettling = true
        delay(320)
        isSettling = false
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }

    // The refresh spinner is a promise with a ceiling: if the request never
    // reports back, the old list returns instead of shimmering indefinitely.
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(2000)
            isRefreshing = false
        }
    }

    // Any change to what is being shown returns to the first page.
    LaunchedEffect(query, quickChip, savedOnly, filters, sort) {
        page = 1
    }

    val savedIds = remember(savedPapers) { savedPapers.map { it.id }.toSet() }
    val downloadedIds = remember(savedPapers) {
        savedPapers.filter { it.isDownloaded }.map { it.id }.toSet()
    }

    BackHandler(enabled = queryInput.isNotBlank() || filters.count > 0 || savedOnly) {
        clearSearch()
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

    val entries = remember(papers) { buildPaperEntries(papers) }
    val paperTotal = remember(papers) { papers.size }
    val schemeTotal = remember(papers) { papers.count { it.hasMarkingScheme } }

    val visible = remember(entries, query, quickChip, savedOnly, sort, filters, savedIds) {
        entries
            .asSequence()
            .filter { entry ->
                matchesChip(
                    entry.paper,
                    QUICK_CHIPS.firstOrNull { it.first == quickChip }?.second.orEmpty()
                )
            }
            .filter { entry -> !savedOnly || savedIds.contains(entry.id) }
            .filter { entry ->
                filters.medium == "Any medium" ||
                    entry.paper.medium.equals(filters.medium, true)
            }
            .filter { entry ->
                filters.year == "Any year" || entry.paper.year.toString() == filters.year
            }
            .filter { entry ->
                filters.subject == "Any subject" ||
                    entry.paper.subject.equals(filters.subject, true)
            }
            .filter { entry -> !filters.markingSchemeOnly || entry.paper.hasMarkingScheme }
            .filter { entry -> matchesQuery(entry, query) }
            .sortedWith(
                when (sort) {
                    // Within a year the question paper leads its own scheme.
                    PaperSort.NEWEST -> compareByDescending<PaperEntry> { it.paper.year }
                        .thenBy { it.paper.subject }
                        .thenBy { it.isScheme }
                    PaperSort.OLDEST -> compareBy<PaperEntry> { it.paper.year }
                        .thenBy { it.paper.subject }
                        .thenBy { it.isScheme }
                    PaperSort.MOST_DOWNLOADED ->
                        compareByDescending<PaperEntry> { it.paper.downloadCount }
                            .thenBy { it.isScheme }
                    PaperSort.A_TO_Z -> compareBy<PaperEntry> { it.paper.subject }
                        .thenByDescending { it.paper.year }
                        .thenBy { it.isScheme }
                }
            )
            .toList()
    }

    val pageCount = ((visible.size + PAPERS_PER_PAGE - 1) / PAPERS_PER_PAGE)
        .coerceAtLeast(1)
    val safePage = page.coerceIn(1, pageCount)
    val pageStart = (safePage - 1) * PAPERS_PER_PAGE
    val pageItems = visible.drop(pageStart).take(PAPERS_PER_PAGE)

    val listState = rememberLazyListState()
    // The controls (tabs, search, chips, sort) are the list's first item, so the
    // first paper or scheme sits one item further down — plus the offline notice
    // when the library is showing cached rows.
    val resultsStartIndex = 1 + if (isLiveDatabase) 0 else 1

    // Turning a page is a change of scene: the list jumps to the top of the
    // *results* under a short shimmer, not to the top of the whole screen.
    var shownPage by remember { mutableStateOf(safePage) }
    LaunchedEffect(safePage) {
        if (safePage != shownPage) {
            shownPage = safePage
            listState.scrollToItem(resultsStartIndex)
        }
    }

    val isFiltering = query.isNotBlank() || quickChip != "All" || savedOnly || filters.count > 0
    // The list shows skeletons while the library loads, while a refresh runs,
    // and for the moment the filter or query changes.
    val showSkeletons = isLoading || isSettling || isRefreshing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBg)
            .onGloballyPositioned { screenBounds = it }
            // Tapping anywhere that is not the search field ends the focus, the
            // way every other search box on the phone behaves. This observes the
            // Initial pass and consumes nothing, so scrolling, row taps, chips
            // and the keyboard itself keep working exactly as before.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.firstOrNull {
                            it.changedToDownIgnoreConsumed()
                        } ?: continue
                        // "boundsInRoot" is measured against the window, the tap
                        // against this Column; the origin turns one into the other.
                        val origin = screenBounds?.boundsInRoot()?.topLeft ?: Offset.Zero
                        val field = searchFieldBounds
                        val tappedTheField = field != Rect.Zero &&
                            field.contains(down.position + origin)
                        if (!tappedTheField) focusManager.clearFocus()
                    }
                }
            }
    ) {
        // ── top bar: the only thing that stays put ──────────────────────────
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
                        fontSize = PT.screen,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaperInk,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = if (segment == 0) {
                            if (isLoading && papers.isEmpty()) {
                                "Loading the library…"
                            } else {
                                "$paperTotal papers · $schemeTotal marking schemes"
                            }
                        } else {
                            if (isQuizLoading && quizzes.isEmpty()) {
                                "Loading quizzes…"
                            } else {
                                "${quizzes.size} quizzes · pass one to earn EXP"
                            }
                        },
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = PT.micro,
                        color = PaperMuted
                    )
                }
                if (segment == 0) {
                    CircleIconButton(
                        icon = Icons.Default.Refresh,
                        description = "Refresh library",
                        onClick = {
                            isRefreshing = true
                            onRefreshPapers()
                        },
                        tag = "papers_refresh_button"
                    )
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(
                        icon = Icons.Default.FilterList,
                        description = "Filters",
                        onClick = { showFilters = true },
                        tag = "papers_filter_button",
                        badge = filters.count
                    )
                }
            }
        }

        if (segment == 1) {
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
            // Tabs, search, chips and the sort row scroll with the results; the
            // page bar sits at the end of the list, never over it.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("papers_screen_content"),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "controls") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PaperBg)
                    ) {
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
                        // search: type, then Enter or the button to run it
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
                                .onGloballyPositioned { searchFieldBounds = it.boundsInRoot() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = queryInput,
                                onValueChange = { queryInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("papers_search_bar"),
                                placeholder = {
                                    Text(
                                        text = "Physics, PHY, 2020 SFT…",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = PT.meta,
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
                                    if (queryInput.isNotEmpty() || query.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = PaperMuted,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { clearSearch() }
                                                .testTag("papers_search_clear")
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(26.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = PT.body,
                                    color = PaperInk
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PaperGreen,
                                    unfocusedBorderColor = PaperHairline,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            // The same row, one tap: nothing here depends on the
                            // student knowing that a keyboard has a search key.
                            Surface(
                                onClick = { submitSearch() },
                                shape = RoundedCornerShape(16.dp),
                                color = PaperGreen,
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("papers_search_submit")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search papers",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                        }

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
                                isLoading && papers.isEmpty() -> "Loading…"
                                isFiltering -> "${visible.size} of ${papers.size} papers"
                                visible.size > PAPERS_PER_PAGE ->
                                    "${pageStart + 1}–${pageStart + pageItems.size} of ${visible.size}"
                                else -> "${visible.size} papers"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = PT.micro,
                            fontWeight = FontWeight.SemiBold,
                            color = PaperMuted,
                            modifier = Modifier.weight(1f)
                        )
                        if (isFiltering) {
                            Text(
                                text = "Clear",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = PT.micro,
                                fontWeight = FontWeight.Bold,
                                color = PaperGreen,
                                modifier = Modifier
                                    .clickable {
                                        queryInput = ""
                                        query = ""
                                        quickChip = "All"
                                        savedOnly = false
                                        filters = PaperFilters()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("papers_clear_filters")
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    layout = if (layout == PaperLayout.LIST) {
                                        PaperLayout.CARDS
                                    } else {
                                        PaperLayout.LIST
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("papers_layout_toggle")
                        ) {
                            Icon(
                                imageVector = if (layout == PaperLayout.LIST) {
                                    Icons.Default.GridView
                                } else {
                                    Icons.Default.ViewList
                                },
                                contentDescription = "Switch to " +
                                    if (layout == PaperLayout.LIST) {
                                        PaperLayout.CARDS.label
                                    } else {
                                        PaperLayout.LIST.label
                                    },
                                tint = PaperInk,
                                modifier = Modifier.size(17.dp)
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
                                fontSize = PT.micro,
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

                when {
                    showSkeletons -> items(7) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PaperTileSkeleton()
                        }
                    }

                    papers.isEmpty() -> item(key = "offline") {
                        Box(modifier = Modifier.fillParentMaxHeight(0.75f)) {
                            LibraryOfflineState(
                                serverUrl = serverUrl,
                                isLiveDatabase = isLiveDatabase,
                                onRefresh = onRefreshPapers,
                                onUpdateServerUrl = onUpdateServerUrl
                            )
                        }
                    }

                    visible.isEmpty() -> item(key = "empty") {
                        Box(modifier = Modifier.fillParentMaxHeight(0.75f)) {
                            EmptyResultsState(
                                onClear = {
                                    clearSearch()
                                    quickChip = "All"
                                    savedOnly = false
                                    filters = PaperFilters()
                                }
                            )
                        }
                    }

                    else -> {
                        if (!isLiveDatabase) {
                            item(key = "notice") {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    OfflineNotice(onRefresh = onRefreshPapers)
                                }
                            }
                        }
                        items(pageItems, key = { it.id }) { entry ->
                            val paper = entry.paper
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                if (layout == PaperLayout.LIST) {
                                    PaperRow(
                                        paper = paper,
                                        title = entry.title,
                                        isSaved = savedIds.contains(paper.id),
                                        isDownloaded = downloadedIds.contains(paper.id),
                                        onClick = { onPaperClick(paper) },
                                        onToggleSave = { onToggleSavePaper(paper) }
                                    )
                                } else {
                                    PaperCardRow(
                                        paper = paper,
                                        title = entry.title,
                                        isSaved = savedIds.contains(paper.id),
                                        isDownloaded = downloadedIds.contains(paper.id),
                                        onClick = { onPaperClick(paper) },
                                        onToggleSave = { onToggleSavePaper(paper) }
                                    )
                                }
                            }
                        }
                        if (pageCount > 1) {
                            item(key = "pagination") {
                                PaginationBar(
                                    page = safePage,
                                    pageCount = pageCount,
                                    total = visible.size,
                                    // Every page tap is a change of scene: the
                                    // rows give way to skeletons, the list jumps
                                    // to the first paper, then the new page lands.
                                    onPage = { target ->
                                        page = target
                                        isSettling = true
                                        settleScope.launch {
                                            delay(300)
                                            isSettling = false
                                        }
                                    }
                                )
                            }
                        }
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
                    fontSize = PT.cardTitle,
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
                            fontSize = PT.body,
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
                        fontSize = PT.section,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaperInk,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { filters = PaperFilters() }) {
                        Text(
                            text = "Reset",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = PT.meta,
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
                        fontSize = PT.meta,
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
                            fontSize = PT.body,
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
                fontSize = PT.meta,
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
    title: String,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
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
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = PT.body,
                    lineHeight = PT.bodyHeight,
                    fontWeight = FontWeight.Bold,
                    color = PaperInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = paper.subtitleLine(),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = PT.micro,
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
            Spacer(Modifier.width(8.dp))
            // Downloading lives one tap away, in the detail sheet: two download
            // pills on every row turned the list into a button grid.
            IconAction(
                icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                description = if (isSaved) "Remove from saved" else "Save paper",
                tint = if (isSaved) PaperGreen else PaperMuted,
                onClick = onToggleSave,
                tag = "papers_save_${paper.id}"
            )
        }
    }
}

/** The roomy layout: a large cover and the title given room to breathe. */
@Composable
private fun PaperCardRow(
    paper: PastPaper,
    title: String,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("papers_card_${paper.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaperCover(
                paper = paper,
                modifier = Modifier.size(width = 74.dp, height = 94.dp),
                corner = 14.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SubjectCodes.of(paper.subject),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = PT.overline,
                    fontWeight = FontWeight.ExtraBold,
                    color = PaperGreen,
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = PT.cardTitle,
                    lineHeight = PT.bodyHeight,
                    fontWeight = FontWeight.Bold,
                    color = PaperInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = paper.subtitleLine(),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = PT.micro,
                    color = PaperMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (paper.hasMarkingScheme) {
                        MiniBadge("Marking scheme", PaperGreen, PaperGreenSoft)
                    }
                    if (isDownloaded) {
                        MiniBadge("Offline", Color(0xFF2563EB), Color(0xFFEFF6FF))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconAction(
                icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                description = if (isSaved) "Remove from saved" else "Save paper",
                tint = if (isSaved) PaperGreen else PaperMuted,
                onClick = onToggleSave,
                tag = "papers_card_save_${paper.id}"
            )
        }
    }
}

/** "PHY · Physics · 2024 · Sinhala", with empty pieces dropped. */
private fun PastPaper.subtitleLine(): String {
    val code = SubjectCodes.of(subject).takeIf { it != "PAPER" }
    return listOfNotNull(code, subject.takeIf { it.isNotBlank() }, year.toString(), medium.takeIf { it.isNotBlank() })
        .joinToString(" · ")
}

/**
 * Page pills: first, last, and a window around the current page. 741 papers at
 * 15 a page is 50 pages, so an unclipped row of numbers would be useless.
 */
@Composable
private fun PaginationBar(
    page: Int,
    pageCount: Int,
    total: Int,
    onPage: (Int) -> Unit
) {
    val window = remember(page, pageCount) {
        val pages = mutableListOf<Int>()
        pages += 1
        for (candidate in (page - 1)..(page + 1)) {
            if (candidate in 2 until pageCount) pages += candidate
        }
        if (pageCount >= 2) pages += pageCount
        pages.distinct().sorted()
    }

    // No background and no elevation: the bar is the list's last item, so it
    // scrolls up and comes to rest under the final paper like a printed footer.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(PaperHairline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PageArrow(
                forward = false,
                enabled = page > 1,
                tag = "papers_page_prev",
                onClick = { onPage(page - 1) }
            )
            Spacer(Modifier.width(6.dp))
            window.forEachIndexed { index, number ->
                if (index > 0 && number - window[index - 1] > 1) {
                    Text(
                        text = "…",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = PT.micro,
                        color = PaperFaint,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
                PagePill(
                    number = number,
                    selected = number == page,
                    onClick = { onPage(number) }
                )
                Spacer(Modifier.width(6.dp))
            }
            PageArrow(
                forward = true,
                enabled = page < pageCount,
                tag = "papers_page_next",
                onClick = { onPage(page + 1) }
            )
        }
        Text(
            text = "Page $page of $pageCount · $PAPERS_PER_PAGE per page · $total papers",
            fontFamily = MaisonNeueFontFamily,
            fontSize = PT.overline,
            color = PaperFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("papers_page_label")
        )
    }
}

@Composable
private fun PagePill(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (selected) PaperGreen else Color.White)
            .clickable(onClick = onClick)
            .testTag("papers_page_$number"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = PT.micro,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) Color.White else PaperInk
        )
    }
}

@Composable
private fun PageArrow(
    forward: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (forward) {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            },
            contentDescription = if (forward) "Next page" else "Previous page",
            tint = if (enabled) PaperInk else PaperFaint,
            modifier = Modifier.size(18.dp)
        )
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
            fontSize = PT.micro,
            color = Color(0xFF92400E),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Refresh",
            fontFamily = MaisonNeueFontFamily,
            fontSize = PT.micro,
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
            fontSize = PT.section,
            fontWeight = FontWeight.ExtraBold,
            color = PaperInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The library returned nothing. If you are on a new server, check the connection; otherwise pull again.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = PT.meta,
            lineHeight = PT.metaHeight,
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
                    fontSize = PT.meta,
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
            fontSize = PT.micro,
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
                    Text("Server URL", fontSize = PT.micro, fontFamily = MaisonNeueFontFamily)
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
                            fontSize = PT.meta,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
                Text(
                    text = if (isLiveDatabase) "Connected" else "Not connected",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = PT.micro,
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
            fontSize = PT.section,
            fontWeight = FontWeight.ExtraBold,
            color = PaperInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Try fewer words — e.g. \"Physics 2020\" or a code like SFT.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = PT.meta,
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
                    fontSize = PT.meta,
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
                    fontSize = PT.overline,
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
                fontSize = PT.meta,
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
            fontSize = PT.overline,
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
            fontSize = PT.overline,
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
