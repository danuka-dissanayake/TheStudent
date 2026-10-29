package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizSummary
import com.example.data.model.SubjectCodes
import com.example.ui.components.paperCoverTint
import com.example.ui.components.shimmerEffect
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QuizInk = Color(0xFF0F172A)
private val QuizMuted = Color(0xFF64748B)
private val QuizFaint = Color(0xFF94A3B8)
private val QuizHairline = Color(0xFFE8EDF3)
private val QuizGreen = Color(0xFF059669)
private val QuizGreenSoft = Color(0xFFECFDF5)
private val QuizAmber = Color(0xFFB45309)
private val QuizAmberSoft = Color(0xFFFFFBEB)

private val QUIZ_CHIPS = listOf("All", "A/L", "O/L", "Grades 6–9", "University")

/** Fixed page size, matching the papers library. */
private const val QUIZZES_PER_PAGE = 15

/**
 * The hints that cycle through the quiz search field while it is empty. One
 * placeholder can only teach one way of searching, so the field takes turns.
 */
private val QUIZ_SEARCH_HINTS = listOf(
    "Search Physics, SFT or Grade 13…",
    "Try a subject: Combined Mathematics",
    "Any medium: Sinhala, Tamil, English",
    "Find a quiz to earn EXP"
)

private const val QUIZ_HINT_INTERVAL_MS = 3200L

/**
 * The segment's type scale, the same roles and values as the papers screen's
 * `PT`, so the two halves of this tab read as one screen.
 */
private object QT {
    val section: TextUnit = 18.sp
    val cardTitle: TextUnit = 15.sp
    val body: TextUnit = 14.sp
    val meta: TextUnit = 13.sp
    val micro: TextUnit = 12.sp
    val overline: TextUnit = 11.sp
}

/**
 * Does this quiz answer the search text?
 *
 * The query is split into words and every word must match somewhere in the quiz
 * — title, subject, grade, medium or the short subject code — so "physics 13"
 * works even though those live in different fields. Words are also matched with
 * spaces and punctuation removed, and a handful of student spellings ("maths",
 * "chem", "phy") widen to the names the database stores.
 */
private fun matchesQuizQuery(quiz: QuizSummary, query: String): Boolean {
    if (query.isBlank()) return true
    val tokens = query.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .distinct()
    if (tokens.isEmpty()) return true
    val code = SubjectCodes.of(quiz.subject).takeIf { it != "PAPER" }.orEmpty()
    val fields = listOf(
        quiz.title,
        quiz.subject,
        code,
        quiz.grade,
        quiz.medium,
        if (code.isNotEmpty()) "$code ${quiz.grade}" else ""
    ).filter { it.isNotBlank() }
    return tokens.all { token ->
        (QUIZ_SEARCH_SYNONYMS[token] ?: listOf(token)).any { alternative ->
            val squeezed = alternative.filter { it.isLetterOrDigit() }
            fields.any { field ->
                field.contains(alternative, ignoreCase = true) ||
                    field.filter { it.isLetterOrDigit() }.lowercase().contains(squeezed)
            }
        }
    }
}

private val QUIZ_SEARCH_SYNONYMS: Map<String, List<String>> = mapOf(
    "maths" to listOf("mathematics"),
    "math" to listOf("mathematics"),
    "chem" to listOf("chemistry"),
    "bio" to listOf("biology"),
    "phy" to listOf("physics"),
    "phys" to listOf("physics"),
    "eng" to listOf("english"),
    "sin" to listOf("sinhala"),
    "tam" to listOf("tamil")
)

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
    onOpenProgress: () -> Unit = {},
    // The Papers / Quizzes pills live in the caller, but they belong to the
    // scrolling content here: only the top bar stays put on this screen.
    topContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // What the field shows, and what the list filters on. The list waits for a
    // submit so the library is never re-filtered while a student is typing.
    var searchInput by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf("All") }
    var onlyPassed by remember { mutableStateOf(false) }
    var onlyNew by remember { mutableStateOf(false) }
    var onlyEnrolled by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var hintIndex by remember { mutableStateOf(0) }
    var isSettling by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Geometry for "tap outside ends the focus", measured in root coordinates.
    var fieldOrigin by remember { mutableStateOf(Offset.Zero) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }

    fun shimmerBriefly(durationMs: Long = 320L) {
        isSettling = true
        scope.launch {
            delay(durationMs)
            isSettling = false
        }
    }

    fun submitSearch() {
        query = searchInput.trim()
        shimmerBriefly()
        focusManager.clearFocus()
        keyboard?.hide()
    }

    fun clearAll() {
        searchInput = ""
        query = ""
        chip = "All"
        onlyPassed = false
        onlyNew = false
        onlyEnrolled = false
        focusManager.clearFocus()
        keyboard?.hide()
    }

    // Placeholder hints cycle only while the field is empty.
    LaunchedEffect(searchInput.isEmpty()) {
        if (searchInput.isEmpty()) {
            while (true) {
                delay(QUIZ_HINT_INTERVAL_MS)
                hintIndex = (hintIndex + 1) % QUIZ_SEARCH_HINTS.size
            }
        }
    }

    LaunchedEffect(query, chip, onlyPassed, onlyNew, onlyEnrolled) {
        page = 1
        shimmerBriefly()
    }

    val visible = remember(quizzes, query, chip, onlyPassed, onlyNew, onlyEnrolled) {
        quizzes
            .filter { quiz -> matchesChip(quiz, chip) }
            .filter { quiz -> !onlyPassed || quiz.passed }
            .filter { quiz -> !onlyNew || quiz.attempts == 0 }
            .filter { quiz -> !onlyEnrolled || quiz.isEnrolled }
            .filter { quiz -> matchesQuizQuery(quiz, query) }
            // Live quizzes first; quizzes whose attempts are used up sink to the
            // bottom, where the Enrolled chip collects them.
            .sortedWith(
                compareBy<QuizSummary> { if (it.isEnrolled) 2 else if (it.attempts == 0) 1 else 0 }
                    .thenByDescending { it.bestPercent ?: 0 }
            )
    }

    val pageCount = ((visible.size + QUIZZES_PER_PAGE - 1) / QUIZZES_PER_PAGE)
        .coerceAtLeast(1)
    val safePage = page.coerceIn(1, pageCount)
    val pageStart = (safePage - 1) * QUIZZES_PER_PAGE
    val pageItems = visible.drop(pageStart).take(QUIZZES_PER_PAGE)

    val enrolledCount = remember(quizzes) { quizzes.count { it.isEnrolled } }
    val filtering = query.isNotBlank() || chip != "All" || onlyPassed || onlyNew || onlyEnrolled
    // Shimmer on entry, on refresh, and for the moment any filter or the query
    // changes — not only on a cold start.
    val showSkeletons = isLoading || isSettling

    // Turning a page lands on the first quiz, not on the search field.
    var shownPage by remember { mutableStateOf(safePage) }
    LaunchedEffect(safePage) {
        if (safePage != shownPage) {
            shownPage = safePage
            listState.scrollToItem(1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.localToRoot(Offset.Zero) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val touch = event.changes.firstOrNull()?.position ?: continue
                        val x = touch.x + rootOrigin.x
                        val y = touch.y + rootOrigin.y
                        val insideField = fieldSize.width > 0 &&
                            x >= fieldOrigin.x &&
                            y >= fieldOrigin.y &&
                            x <= fieldOrigin.x + fieldSize.width &&
                            y <= fieldOrigin.y + fieldSize.height
                        if (!insideField) focusManager.clearFocus()
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("quizzes_list"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "controls") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    topContent()

                    // search: type, then Enter or the button to run it
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .onGloballyPositioned {
                                fieldOrigin = it.localToRoot(Offset.Zero)
                                fieldSize = it.size
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("quizzes_search_bar"),
                            placeholder = {
                                Crossfade(
                                    targetState = hintIndex,
                                    animationSpec = tween(durationMillis = 600),
                                    label = "quizzes_search_hint"
                                ) { index ->
                                    Text(
                                        text = QUIZ_SEARCH_HINTS[index % QUIZ_SEARCH_HINTS.size],
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = QT.meta,
                                        color = QuizFaint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = QuizMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchInput.isNotEmpty() || query.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = QuizMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { clearAll() }
                                            .testTag("quizzes_search_clear")
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(26.dp),
                            textStyle = TextStyle(
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = QT.body,
                                color = QuizInk
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QuizGreen,
                                unfocusedBorderColor = QuizHairline,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Same row, same surface as the field: white with a
                        // hairline, only the magnifier carries the accent.
                        Surface(
                            onClick = { submitSearch() },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, QuizHairline),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("quizzes_search_submit")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search quizzes",
                                    tint = QuizGreen,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }

                    // chips
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
                            label = "New",
                            selected = onlyNew,
                            tag = "quizzes_chip_new",
                            onClick = {
                                onlyPassed = false
                                onlyEnrolled = false
                                onlyNew = !onlyNew
                            }
                        )
                        QuizPill(
                            label = "Passed",
                            selected = onlyPassed,
                            tag = "quizzes_chip_passed",
                            onClick = {
                                onlyNew = false
                                onlyEnrolled = false
                                onlyPassed = !onlyPassed
                            }
                        )
                        if (enrolledCount > 0) {
                            QuizPill(
                                label = "Enrolled ($enrolledCount)",
                                selected = onlyEnrolled,
                                tag = "quizzes_chip_enrolled",
                                onClick = {
                                    onlyNew = false
                                    onlyPassed = false
                                    onlyEnrolled = !onlyEnrolled
                                }
                            )
                        }
                        // A filter, not a toggle: it leaves the list for the progress page.
                        Surface(
                            onClick = onOpenProgress,
                            shape = RoundedCornerShape(50),
                            color = QuizGreenSoft,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("quizzes_open_progress")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = QuizGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "My progress",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = QT.meta,
                                    fontWeight = FontWeight.Bold,
                                    color = QuizGreen
                                )
                            }
                        }
                    }

                    // result line + refresh
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isLoading && quizzes.isEmpty() -> "Loading…"
                                filtering -> "${visible.size} of ${quizzes.size} quizzes"
                                visible.size > QUIZZES_PER_PAGE ->
                                    "${pageStart + 1}–${pageStart + pageItems.size} of ${visible.size}"
                                else -> "${visible.size} quizzes"
                            },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = QT.micro,
                            fontWeight = FontWeight.SemiBold,
                            color = QuizMuted,
                            modifier = Modifier.weight(1f)
                        )
                        if (filtering) {
                            Text(
                                text = "Clear",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = QT.micro,
                                fontWeight = FontWeight.Bold,
                                color = QuizGreen,
                                modifier = Modifier
                                    .clickable { clearAll() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("quizzes_clear_filters")
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
                                fontSize = QT.micro,
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
                }
            }

            when {
                showSkeletons -> items(7) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        QuizCardSkeleton()
                    }
                }

                quizzes.isEmpty() -> item(key = "empty") {
                    Box(modifier = Modifier.fillParentMaxHeight(0.72f)) {
                        QuizEmptyState(error = error, isOffline = isOffline, onRefresh = onRefresh)
                    }
                }

                visible.isEmpty() -> item(key = "no_match") {
                    Box(modifier = Modifier.fillParentMaxHeight(0.72f)) {
                        QuizNoMatchState(onClear = { clearAll() })
                    }
                }

                else -> {
                    if (isOffline) {
                        item(key = "offline") {
                            QuizOfflineNotice(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    items(pageItems, key = { it.id }) { quiz ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            QuizCard(
                                quiz = quiz,
                                onClick = {
                                    // Enrolled cards are not tappable; this is the
                                    // guard in case a stale list is on screen.
                                    if (!quiz.isEnrolled) onOpenQuiz(quiz)
                                }
                            )
                        }
                    }
                    if (pageCount > 1) {
                        item(key = "pagination") {
                            QuizPaginationBar(
                                page = safePage,
                                pageCount = pageCount,
                                total = visible.size,
                                onPage = { target ->
                                    page = target
                                    // A page tap is a change of scene: skeletons,
                                    // then the list jumps to the first quiz.
                                    shimmerBriefly(300L)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Nothing matches" for the quizzes segment, with the one action that helps. */
@Composable
private fun QuizNoMatchState(onClear: () -> Unit) {
    Column(
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
            fontSize = QT.section,
            fontWeight = FontWeight.ExtraBold,
            color = QuizInk
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Try fewer words, or clear the filters below.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = QT.meta,
            color = QuizMuted
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onClear,
            shape = RoundedCornerShape(50),
            color = Color.White,
            border = BorderStroke(1.dp, QuizHairline),
            modifier = Modifier.height(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Clear filters",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = QT.body,
                    fontWeight = FontWeight.Bold,
                    color = QuizInk,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun QuizOfflineNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
            fontSize = QT.micro,
            color = QuizAmber
        )
    }
}

/**
 * Page pills for the quiz list: first, last and a window around the current
 * page, plus the plain-language summary underneath.
 */
@Composable
private fun QuizPaginationBar(
    page: Int,
    pageCount: Int,
    total: Int,
    onPage: (Int) -> Unit
) {
    val window = remember(page, pageCount) {
        val pages = mutableListOf(1)
        for (candidate in (page - 1)..(page + 1)) {
            if (candidate in 2 until pageCount) pages += candidate
        }
        if (pageCount >= 2) pages += pageCount
        pages.distinct().sorted()
    }

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
                .background(QuizHairline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            QuizPageArrow(
                forward = false,
                enabled = page > 1,
                tag = "quizzes_page_prev",
                onClick = { onPage(page - 1) }
            )
            Spacer(Modifier.width(6.dp))
            window.forEachIndexed { index, number ->
                if (index > 0 && number - window[index - 1] > 1) {
                    Text(
                        text = "…",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = QT.micro,
                        color = QuizFaint,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
                QuizPagePill(
                    number = number,
                    selected = number == page,
                    onClick = { onPage(number) }
                )
                Spacer(Modifier.width(6.dp))
            }
            QuizPageArrow(
                forward = true,
                enabled = page < pageCount,
                tag = "quizzes_page_next",
                onClick = { onPage(page + 1) }
            )
        }
        Text(
            text = "Page $page of $pageCount · $QUIZZES_PER_PAGE per page · $total quizzes",
            fontFamily = MaisonNeueFontFamily,
            fontSize = QT.overline,
            color = QuizFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("quizzes_page_label")
        )
    }
}

@Composable
private fun QuizPagePill(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (selected) QuizGreen else Color.White)
            .clickable(onClick = onClick)
            .testTag("quizzes_page_$number"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = QT.micro,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) Color.White else QuizInk
        )
    }
}

@Composable
private fun QuizPageArrow(
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
            tint = if (enabled) QuizInk else QuizFaint,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizCard(quiz: QuizSummary, onClick: () -> Unit) {
    val enrolled = quiz.isEnrolled

    Surface(
        onClick = onClick,
        enabled = !enrolled,
        shape = RoundedCornerShape(18.dp),
        color = if (enrolled) Color(0xFFF4F6FA) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(
                if (enrolled) "quiz_card_enrolled_${quiz.id}" else "quiz_card_${quiz.id}"
            )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            QuizCover(
                quiz = quiz,
                modifier = Modifier.size(width = 40.dp, height = 50.dp),
                dimmed = enrolled
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enrolled) QuizMuted else QuizInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOf(quiz.subject, quiz.grade, quiz.medium)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    color = QuizMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (enrolled) {
                        QuizBadge("Enrolled", QuizMuted, Color(0xFFE8EDF3))
                        QuizBadge(quiz.attemptsLabel, QuizMuted, Color(0xFFF1F5F9))
                    } else {
                        QuizBadge("${quiz.questionCount} questions", QuizMuted, Color(0xFFF1F5F9))
                        if (quiz.isTimed) {
                            QuizBadge("${quiz.minutes} min", QuizMuted, Color(0xFFF1F5F9))
                        }
                        if (quiz.expReward > 0) {
                            QuizBadge("+${quiz.expReward} EXP", QuizGreen, QuizGreenSoft)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (enrolled) {
                EnrolledChip()
            } else {
                ScoreChip(quiz)
            }
        }
    }
}

/** The locked state: the quiz is finished, its attempts are used up. */
@Composable
private fun EnrolledChip() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8EDF3)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Enrolled — no attempts left",
                tint = QuizMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Enrolled",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = QuizMuted
        )
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
                    fontSize = 11.5.sp,
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB91C1C)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "best",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.5.sp,
                    color = QuizMuted
                )
            }
        }
        else -> QuizBadge("New", QuizGreen, QuizGreenSoft)
    }
}

/** A miniature quiz sheet: coloured header band, ruled lines, subject initial. */
@Composable
private fun QuizCover(quiz: QuizSummary, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val tint = if (dimmed) QuizFaint else paperCoverTint(quiz.subject)
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
                    fontSize = 12.sp,
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
            fontSize = 10.5.sp,
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
                fontSize = 13.sp,
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
                .shimmerEffect(shapeRadius = 10.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(11.dp)
                    .shimmerEffect(shapeRadius = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(9.dp)
                    .shimmerEffect(shapeRadius = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(9.dp)
                    .shimmerEffect(shapeRadius = 4.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .shimmerEffect(shapeRadius = 17.dp)
        )
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
            fontSize = 13.sp,
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
                    fontSize = 13.5.sp,
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
    // Parse the whole grade number: "Grade 13" must not fall into 6–9 just
    // because it ends in 3.
    "Grades 6–9" -> quiz.category.contains("Junior", true) ||
        (quiz.grade.removePrefix("Grade ").trim().toIntOrNull() ?: 0) in 6..9
    "University" -> quiz.category.contains("University", true) ||
        quiz.grade.contains("Undergraduate", true) || quiz.grade.contains("Year", true)
    else -> true
}
