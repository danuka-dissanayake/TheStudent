package com.example.ui.screens

import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.CatalogCertificate
import com.example.data.model.CatalogCourse
import com.example.data.model.CatalogProvider
import com.example.data.model.CatalogVideo
import com.example.data.model.CourseCatalog
import com.example.ui.components.paperCoverTint
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CcInk = Color(0xFF0F172A)
private val CcMuted = Color(0xFF64748B)
private val CcFaint = Color(0xFF94A3B8)
private val CcHairline = Color(0xFFE8EDF3)
private val CcBg = Color(0xFFF7F9FC)
private val CcGreen = Color(0xFF059669)
private val CcGreenSoft = Color(0xFFECFDF5)
private val CcAmber = Color(0xFFB45309)
private val CcAmberSoft = Color(0xFFFFFBEB)
private val CcBlue = Color(0xFF1D4ED8)
private val CcBlueSoft = Color(0xFFEEF2FF)

/**
 * One type scale for the whole screen, so nothing is left at 9sp because it
 * "fit". The floor is [micro] (11sp) — anything smaller is unreadable on a
 * 5-inch phone in daylight.
 */
private object CT {
    val screen: TextUnit = 21.sp
    val hero: TextUnit = 22.sp
    val section: TextUnit = 16.sp
    val cardTitle: TextUnit = 14.5.sp
    val body: TextUnit = 13.sp
    val meta: TextUnit = 12.sp
    val micro: TextUnit = 11.sp
    val chip: TextUnit = 12.sp

    val bodyHeight: TextUnit = 19.sp
    val metaHeight: TextUnit = 17.sp
}

private const val PAGE_SIZE = 15

private val STAGES = listOf("A/L", "Undergraduate", "Postgraduate")
private val LEVELS = listOf("Beginner", "Intermediate", "Advanced")

/**
 * The courses collection — an index of **other people's** courses.
 *
 * Two rules run through the whole screen:
 *
 *   1. **Every card names its owner.** The provider is a line of text on the
 *      card ("Provided by edX"), the provider rail shows their own logo, and the
 *      detail page repeats the credit above the button that leaves the app.
 *   2. **Nothing is re-hosted.** Tapping opens the provider's own page, so the
 *      student enrols with them, and their terms, certificates and prices stay
 *      their business. Videos are embedded from YouTube and credited to the
 *      channel that made them.
 */
@Composable
fun CourseCollectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: String? = null
) {
    var selectedCourse by remember { mutableStateOf<CatalogCourse?>(null) }
    var playingVideo by remember { mutableStateOf<CatalogVideo?>(null) }

    val uriHandler = LocalUriHandler.current
    val openUrl: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    BackHandler(enabled = selectedCourse != null) { selectedCourse = null }

    Surface(color = CcBg, modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val course = selectedCourse
            if (course != null) {
                CourseDetail(
                    course = course,
                    onBack = { selectedCourse = null },
                    onOpenProvider = { openUrl(course.url) },
                    onPlayVideo = { id -> playingVideo = CourseCatalog.video(id) }
                )
            } else {
                CourseCatalogBrowse(
                    onBack = onBack,
                    initialFilter = initialFilter,
                    onOpenCourse = { selectedCourse = it },
                    onPlayVideo = { playingVideo = it }
                )
            }

            // The player is an overlay in this composition, deliberately not a
            // Dialog: a dialog gets its own window, and a video surface in a
            // second window is exactly where frames go missing (audio and
            // controls work, the picture stays blank).
            playingVideo?.let { video ->
                YouTubePlayerOverlay(
                    video = video,
                    onDismiss = { playingVideo = null },
                    onOpenYouTube = {
                        openUrl(video.watchUrl)
                        playingVideo = null
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Browse
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseCatalogBrowse(
    onBack: () -> Unit,
    initialFilter: String?,
    onOpenCourse: (CatalogCourse) -> Unit,
    onPlayVideo: (CatalogVideo) -> Unit
) {
    // Opening from a dashboard course card seeds the browse state with that
    // card's category. A category only becomes a filter when the catalogue can
    // actually honour it — otherwise the student would land on an empty list,
    // which is worse than no filter at all.
    val seed = remember(initialFilter) { resolveSeed(initialFilter) }
    var query by remember { mutableStateOf(if (seed is CatalogSeed.Text) seed.value else "") }
    var certificate by remember { mutableStateOf<CatalogCertificate?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableStateOf<String?>(null) }
    var subject by remember {
        mutableStateOf((seed as? CatalogSeed.Subject)?.value)
    }
    var provider by remember { mutableStateOf<String?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val featured = remember { CourseCatalog.courses.filter { it.featured } }
    // Only the facets become chips. The search term is already visible in the
    // field right above, so echoing it beside the Filters button was noise.
    val activeFilters = listOfNotNull(
        certificate?.short,
        stage,
        level,
        subject,
        provider
    )
    val hasQuery = query.isNotBlank()
    val filtering = activeFilters.isNotEmpty() || hasQuery

    val visible = remember(query, certificate, stage, level, subject, provider) {
        CourseCatalog.courses.filter { course ->
            (certificate == null || course.certificate == certificate) &&
                (stage == null || course.stages.contains(stage)) &&
                (level == null || course.level == level) &&
                (subject == null || course.subject == subject) &&
                (provider == null || course.provider == provider) &&
                (
                    query.isBlank() ||
                        course.title.contains(query, true) ||
                        course.subject.contains(query, true) ||
                        course.provider.contains(query, true) ||
                        course.skills.any { it.contains(query, true) }
                    )
        }
    }

    // Shimmer whenever the visible set changes — on arrival and after any filter.
    // Nothing here is fetched, so this is a deliberate ~half-second of feedback
    // rather than a spinner for a render that already happened.
    val filterKey = "$query|$certificate|$stage|$level|$subject|$provider"
    val facetKey = "$certificate|$stage|$level|$subject|$provider"
    var isLoading by remember { mutableStateOf(true) }
    var booted by remember { mutableStateOf(false) }

    // First arrival: let the layout settle behind a shimmer instead of popping in.
    LaunchedEffect(Unit) {
        delay(700)
        booted = true
        isLoading = false
    }

    // Facet changes re-shimmer immediately.
    LaunchedEffect(facetKey) {
        if (booted) {
            isLoading = true
            delay(420)
            isLoading = false
        }
    }

    // Typing shimmers too, but only once the student pauses: the effect is keyed
    // on the query, so every keystroke cancels the previous delay. A 260ms pause
    // counts as "finished typing", then 320ms of skeleton — smooth, and it never
    // flickers mid-word.
    LaunchedEffect(query) {
        if (booted && query.isNotEmpty()) {
            delay(260)
            isLoading = true
            delay(320)
            isLoading = false
        }
    }

    // ── pagination ──────────────────────────────────────────────────────────
    // 15 at a time. A single list of 40 cards made the screen very tall for no
    // benefit; the first page now ends just past the second screenful.
    var page by remember { mutableIntStateOf(0) }
    val pageCount = ((visible.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    val safePage = page.coerceIn(0, pageCount - 1)
    val pageStart = safePage * PAGE_SIZE
    val pageItems = remember(visible, safePage) {
        visible.drop(pageStart).take(PAGE_SIZE)
    }

    // A filter changes what is above the list, so the old scroll offset can land
    // the student at the bottom of a much shorter page: go back to the top, and
    // back to the first page of results.
    LaunchedEffect(filterKey) {
        page = 0
        if (listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Paging is a page change like any other, so it shimmers too: the student
    // taps "2", the pager scrolls them up to the results heading, and the new
    // rows arrive behind a brief skeleton rather than snapping into place.
    var isPageLoading by remember { mutableStateOf(false) }
    LaunchedEffect(safePage) {
        if (booted) {
            isPageLoading = true
            delay(380)
            isPageLoading = false
        }
    }

    // Mirrors the item order below: search, filter row, then (when nothing is
    // filtered) the three rails, then the results heading.
    val resultsHeadingIndex = 2 +
        (if (!filtering && featured.isNotEmpty()) 1 else 0) +
        (if (!filtering) 2 else 0)

    val clearAll: () -> Unit = {
        query = ""
        certificate = null
        stage = null
        level = null
        subject = null
        provider = null
    }

    val shimmer = rememberShimmerBrush()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── the only fixed thing: the title bar ──────────────────────────────
        CatalogHeader(
            courseCount = CourseCatalog.courses.size,
            providerCount = CourseCatalog.usedProviders.size,
            onBack = onBack
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("courses_collection_content"),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Search scrolls with the page, not pinned above it.
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp)
                        .height(50.dp)
                        .testTag("courses_search"),
                    placeholder = {
                        Text(
                            text = "Search courses, subjects, providers",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.body,
                            color = CcFaint
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = CcMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = CcMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { query = "" }
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    textStyle = TextStyle(
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.body,
                        color = CcInk
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        // No coloured focus ring: the cursor and the filtered
                        // results are the feedback, and a green outline on a
                        // search field reads as an error state on this palette.
                        focusedBorderColor = CcHairline,
                        unfocusedBorderColor = CcHairline,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = CcInk
                    )
                )
            }

            item(key = "filter_row") {
                FilterSummaryRow(
                    filterCount = activeFilters.size,
                    activeFilters = activeFilters,
                    onOpenFilters = { sheetOpen = true },
                    onClear = clearAll
                )
            }

            if (isLoading) {
                item(key = "skeleton") {
                    CatalogSkeleton(brush = shimmer, showRails = !filtering)
                }
            } else {
                if (!filtering && featured.isNotEmpty()) {
                    item(key = "featured") {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            SectionTitle("Editor's picks", "Hand-checked · auto-advancing")
                            FeaturedSlider(featured = featured, onOpenCourse = onOpenCourse)
                        }
                    }
                }

                if (!filtering) {
                    item(key = "videos") {
                        Column(modifier = Modifier.padding(top = 22.dp)) {
                            SectionTitle(
                                "Watch first",
                                "Plays in the app · credited to each channel"
                            )
                            VideoRail(videos = CourseCatalog.videos, onPlayVideo = onPlayVideo)
                        }
                    }

                    item(key = "providers") {
                        Column(modifier = Modifier.padding(top = 22.dp)) {
                            SectionTitle("Where these come from", "Tap a provider to filter")
                            ProviderRail(
                                selected = provider,
                                onSelect = { name ->
                                    provider = if (provider == name) null else name
                                }
                            )
                        }
                    }
                }

                item(key = "list_title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filtering) "Results" else "All courses",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.section,
                            fontWeight = FontWeight.ExtraBold,
                            color = CcInk,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${visible.size} " +
                                if (visible.size == 1) "course" else "courses",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.meta,
                            color = CcMuted
                        )
                    }
                }

                if (visible.isEmpty()) {
                    item(key = "empty") {
                        NoCoursesState(onClear = clearAll)
                    }
                } else if (isPageLoading) {
                    item(key = "page_skeleton") {
                        CourseRowsSkeleton(brush = shimmer, count = pageItems.size.coerceAtMost(6))
                    }
                } else {
                    items(
                        items = pageItems,
                        key = { it.id },
                        // Every item here is the same shape, so one content type
                        // lets Compose reuse slots instead of rebuilding them.
                        contentType = { "course_row" }
                    ) { course ->
                        CourseRow(course = course, onClick = { onOpenCourse(course) })
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (visible.isNotEmpty()) {
                    item(key = "pager") {
                        CoursePager(
                            page = safePage,
                            pageCount = pageCount,
                            shown = pageItems.size,
                            total = visible.size,
                            firstIndex = pageStart + 1,
                            onPage = { target ->
                                page = target
                            },
                            onJumpToResults = {
                                scope.launch {
                                    listState.animateScrollToItem(resultsHeadingIndex)
                                }
                            }
                        )
                    }
                }

                item(key = "credit") {
                    CreditBlock()
                }
            }
        }
    }

    if (sheetOpen) {
        FiltersSheet(
            total = CourseCatalog.courses.size,
            resultCount = visible.size,
            certificate = certificate,
            stage = stage,
            level = level,
            subject = subject,
            provider = provider,
            onCertificate = { certificate = if (certificate == it) null else it },
            onStage = { stage = if (stage == it) null else it },
            onLevel = { level = if (level == it) null else it },
            onSubject = { subject = if (subject == it) null else it },
            onProvider = { provider = if (provider == it) null else it },
            onClear = clearAll,
            onDismiss = { sheetOpen = false }
        )
    }
}

@Composable
private fun CatalogHeader(courseCount: Int, providerCount: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.background(CcBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, CcHairline),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("courses_back")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CcInk,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Courses collection",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.screen,
                    fontWeight = FontWeight.ExtraBold,
                    color = CcInk,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$courseCount courses · $providerCount providers · credited to their owners",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.micro,
                    color = CcMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CcHairline)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Filter bar + sheet
// ─────────────────────────────────────────────────────────────────────────────

/** One row: the Filters button, then whatever is currently applied. */
@Composable
private fun FilterSummaryRow(
    filterCount: Int,
    activeFilters: List<String>,
    onOpenFilters: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onOpenFilters,
            shape = RoundedCornerShape(50),
            color = if (filterCount > 0) CcInk else Color.White,
            border = BorderStroke(1.dp, if (filterCount > 0) CcInk else CcHairline),
            modifier = Modifier
                .height(36.dp)
                .testTag("courses_filters_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (filterCount > 0) Color.White else CcInk,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (filterCount > 0) "Filters ($filterCount)" else "Filters",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.chip,
                    fontWeight = FontWeight.Bold,
                    color = if (filterCount > 0) Color.White else CcInk,
                    maxLines = 1
                )
            }
        }

        activeFilters.forEach { label ->
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CcGreenSoft)
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.micro,
                    fontWeight = FontWeight.SemiBold,
                    color = CcGreen,
                    maxLines = 1
                )
            }
        }

        if (filterCount > 0) {
            Text(
                text = "Clear",
                fontFamily = MaisonNeueFontFamily,
                fontSize = CT.chip,
                fontWeight = FontWeight.Bold,
                color = CcMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("courses_clear_filters")
            )
        }
    }
}

/**
 * Every filter in one place, pulled up from the bottom.
 *
 * Applying live means the count on the button always describes the list behind
 * the sheet, so nobody has to guess what "Apply" would do.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    total: Int,
    resultCount: Int,
    certificate: CatalogCertificate?,
    stage: String?,
    level: String?,
    subject: String?,
    provider: String?,
    onCertificate: (CatalogCertificate) -> Unit,
    onStage: (String) -> Unit,
    onLevel: (String) -> Unit,
    onSubject: (String) -> Unit,
    onProvider: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeCount = listOfNotNull(certificate, stage, level, subject, provider).size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { SheetDragHandle() },
        modifier = Modifier.testTag("courses_filter_sheet")
    ) {
        // Header and the apply button are fixed; only the chips scroll, and the
        // scroll area is capped so the sheet never grows into a full-screen page.
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 0.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filters",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CcInk
                    )
                    Text(
                        text = "$resultCount of $total courses shown",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        color = CcMuted
                    )
                }
                if (activeCount > 0) {
                    Text(
                        text = "Clear all",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.meta,
                        fontWeight = FontWeight.Bold,
                        color = CcGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 356.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 4.dp)
            ) {
                FilterSection("Certificate") {
                    CatalogCertificate.entries.forEach { option ->
                        SheetChip(
                            label = option.short,
                            selected = certificate == option,
                            tag = "courses_chip_" + option.name.lowercase(),
                            onClick = { onCertificate(option) }
                        )
                    }
                }

                FilterSection("Who it's for") {
                    STAGES.forEach { option ->
                        SheetChip(
                            label = option,
                            selected = stage == option,
                            tag = "courses_stage_" + option.lowercase().replace("/", "_"),
                            onClick = { onStage(option) }
                        )
                    }
                }

                FilterSection("Level") {
                    LEVELS.forEach { option ->
                        SheetChip(
                            label = option,
                            selected = level == option,
                            tag = "courses_level_" + option.lowercase(),
                            onClick = { onLevel(option) }
                        )
                    }
                }

                FilterSection("Subject") {
                    CourseCatalog.subjects.forEach { option ->
                        SheetChip(
                            label = option,
                            selected = subject == option,
                            tag = "courses_subject_" + option.lowercase().replace(' ', '_'),
                            onClick = { onSubject(option) }
                        )
                    }
                }

                FilterSection("Provider", last = true) {
                    CourseCatalog.usedProviders.forEach { option ->
                        SheetChip(
                            label = option.name,
                            selected = provider == option.name,
                            tag = "courses_chip_provider_" +
                                option.name.lowercase().replace(' ', '_'),
                            onClick = { onProvider(option.name) }
                        )
                    }
                }
            }

            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(13.dp),
                color = CcGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                    .height(46.dp)
                    .testTag("courses_filters_apply")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show $resultCount " +
                            if (resultCount == 1) "course" else "courses",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.cardTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** A slim handle — the default one plus 12dp of dead space is what made the
 *  sheet feel tall before anything had been drawn. */
@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(Color(0xFFD8DFE8))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    last: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = if (last) 8.dp else 14.dp)) {
        Text(
            text = title.uppercase(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CcFaint,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(7.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SheetChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CcGreenSoft else Color.White,
        border = BorderStroke(
            1.dp,
            if (selected) CcGreen.copy(alpha = 0.45f) else CcHairline
        ),
        modifier = Modifier
            .heightIn(min = 32.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = CcGreen,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = CT.micro,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) CcGreen else CcMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, bottom = 10.dp)) {
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.section,
            fontWeight = FontWeight.ExtraBold,
            color = CcInk
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = MaisonNeueFontFamily,
                fontSize = CT.micro,
                color = CcMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shimmer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberShimmerBrush(onDark: Boolean = false): Brush {
    val transition = rememberInfiniteTransition(label = "courses_shimmer")
    val shift by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing)),
        label = "courses_shimmer_shift"
    )
    val colors = if (onDark) {
        // Over the coloured hero: white sweeps, never grey on gradient.
        listOf(
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.26f),
            Color.White.copy(alpha = 0.10f)
        )
    } else {
        listOf(Color(0xFFEDF1F6), Color(0xFFF8FAFC), Color(0xFFE9EEF5))
    }
    return Brush.linearGradient(
        colors = colors,
        start = Offset(shift, 0f),
        end = Offset(shift + 600f, 600f)
    )
}

@Composable
private fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier,
    corner: Int = 16
) {
    Box(modifier = modifier.clip(RoundedCornerShape(corner.dp)).background(brush))
}

/**
 * The loading state. It mirrors the real layout (slider, two video cards, the
 * provider row, course rows) so the screen does not jump when the content lands.
 */
@Composable
private fun CatalogSkeleton(brush: Brush, showRails: Boolean) {
    Column(modifier = Modifier.testTag("courses_skeleton")) {
        if (showRails) {
            Spacer(Modifier.height(14.dp))
            ShimmerBlock(brush, Modifier.padding(start = 18.dp).width(120.dp).height(16.dp), 6)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    ShimmerBlock(brush, Modifier.width(276.dp).height(186.dp), 20)
                }
            }
            Spacer(Modifier.height(24.dp))
            ShimmerBlock(brush, Modifier.padding(start = 18.dp).width(100.dp).height(16.dp), 6)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    Column(modifier = Modifier.width(232.dp)) {
                        ShimmerBlock(brush, Modifier.fillMaxWidth().aspectRatio(16f / 9f), 16)
                        Spacer(Modifier.height(8.dp))
                        ShimmerBlock(brush, Modifier.width(180.dp).height(12.dp), 5)
                        Spacer(Modifier.height(5.dp))
                        ShimmerBlock(brush, Modifier.width(110.dp).height(10.dp), 5)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            ShimmerBlock(brush, Modifier.padding(start = 18.dp).width(160.dp).height(16.dp), 6)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    ShimmerBlock(brush, Modifier.width(168.dp).height(154.dp), 18)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        ShimmerBlock(brush, Modifier.padding(start = 18.dp).width(130.dp).height(16.dp), 6)
        Spacer(Modifier.height(12.dp))
        repeat(5) {
            ShimmerBlock(
                brush,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(104.dp),
                18
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Featured slider (auto-advancing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeaturedSlider(featured: List<CatalogCourse>, onOpenCourse: (CatalogCourse) -> Unit) {
    val loop = featured.size > 1
    // The same cards three times over. The rail starts in the middle copy, so
    // there is always another card to the right; when it drifts into the third
    // copy we slide back one copy — identical pixels, so the reset is invisible.
    // That is what makes the rail loop forward instead of snapping back to the
    // first card after a long reverse scroll.
    val items = remember(featured) {
        if (loop) featured + featured + featured else featured
    }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = if (loop) featured.size else 0
    )
    val stepPx = with(LocalDensity.current) { (276 + 10).dp.toPx() }

    LaunchedEffect(featured) {
        if (!loop) return@LaunchedEffect
        while (true) {
            delay(4200)
            // Never fight a finger, and let the previous slide finish.
            if (!state.isScrollInProgress) {
                state.animateScrollBy(stepPx, tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    LaunchedEffect(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) {
        if (!loop) return@LaunchedEffect
        val index = state.firstVisibleItemIndex
        val offset = state.firstVisibleItemScrollOffset
        when {
            index >= featured.size * 2 ->
                state.scrollToItem(index - featured.size, offset)
            index < featured.size / 2 ->
                state.scrollToItem(index + featured.size, offset)
        }
    }

    Column {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(count = items.size) { index ->
                val course = items[index]
                FeaturedCard(course = course, onClick = { onOpenCourse(course) })
            }
        }
        if (loop) {
            Spacer(Modifier.height(10.dp))
            // derivedStateOf: the raw scroll index changes on every pixel, and
            // reading it straight in composition recomposes this column (and its
            // dots) continuously while the rail moves. This only changes when the
            // *card* changes.
            val active by remember(featured) {
                derivedStateOf {
                    ((state.firstVisibleItemIndex % featured.size) + featured.size) %
                        featured.size
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                featured.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .height(6.dp)
                            .width(if (index == active) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index == active) CcInk else Color(0xFFDDE3EC))
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(course: CatalogCourse, onClick: () -> Unit) {
    val tint = paperCoverTint(course.subject)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = tint,
        modifier = Modifier
            .width(276.dp)
            .height(186.dp)
            .testTag("courses_featured_${course.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(tint, tint.copy(alpha = 0.72f), Color(0xFF0F172A))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderLogo(name = course.provider, onDark = true, size = 28)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = course.subject.uppercase(),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.92f),
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Provided by ${course.provider}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.micro,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DarkPill(course.certificate.short)
                    DarkPill(course.durationLabel)
                }
            }
        }
    }
}

@Composable
private fun DarkPill(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.micro,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * The provider's own icon, with initials as a fallback while it loads or if the
 * device is offline. Fetched at runtime — no logo is bundled or redrawn.
 */
@Composable
private fun ProviderLogo(
    name: String,
    size: Int = 46,
    onDark: Boolean = false
) {
    val provider: CatalogProvider? = CourseCatalog.provider(name)
    val shape = RoundedCornerShape((size / 3.4f).dp)

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .background(if (onDark) Color.White.copy(alpha = 0.16f) else Color.White)
            .then(
                if (onDark) Modifier
                else Modifier.border(0.5.dp, CcHairline, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Initials are drawn first and the logo paints over them, so "loading"
        // and "no network" look identical without subcomposing each row.
        ProviderInitials(name = name, size = size, onDark = onDark)
        if (provider != null) {
            AsyncImage(
                model = provider.logoUrl,
                contentDescription = provider.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size((size * 0.64f).dp)
            )
        }
    }
}

@Composable
private fun ProviderInitials(name: String, size: Int, onDark: Boolean) {
    val initials = CourseCatalog.provider(name)?.initials ?: name.take(2).uppercase()
    Text(
        text = initials,
        fontFamily = MaisonNeueFontFamily,
        fontSize = (size / 2.9f).sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (onDark) Color.White else paperCoverTint(name)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Video rail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoRail(videos: List<CatalogVideo>, onPlayVideo: (CatalogVideo) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(videos, key = { it.videoId }) { video ->
            Column(
                modifier = Modifier
                    .width(244.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPlayVideo(video) }
                    .testTag("courses_video_${video.videoId}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC0F172A))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.94f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = CcInk,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "${video.minutes} min",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xCC0F172A))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = video.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.body,
                    lineHeight = CT.metaHeight,
                    fontWeight = FontWeight.Bold,
                    color = CcInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channel,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.micro,
                    color = CcMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Provider rail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderRail(selected: String?, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(CourseCatalog.usedProviders, key = { it.name }) { provider ->
            val count = CourseCatalog.courses.count { it.provider == provider.name }
            val isSelected = selected == provider.name
            Surface(
                onClick = { onSelect(provider.name) },
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) CcGreenSoft else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) CcGreen.copy(alpha = 0.45f) else CcHairline
                ),
                modifier = Modifier
                    .width(168.dp)
                    .heightIn(min = 154.dp)
                    .testTag("courses_provider_" + provider.name.lowercase().replace(' ', '_'))
            ) {
                Column(modifier = Modifier.padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProviderLogo(name = provider.name, size = 46)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isSelected) CcGreen else CcFaint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    Text(
                        text = provider.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CcInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = provider.kind,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        lineHeight = 16.5.sp,
                        color = CcMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$count " + if (count == 1) "course" else "courses",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) CcGreen else CcInk
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Course row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CourseRow(course: CatalogCourse, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CcHairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("courses_card_${course.id}")
    ) {
        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.subject,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        fontWeight = FontWeight.ExtraBold,
                        color = paperCoverTint(course.subject),
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (course.local) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CcGreenSoft)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Sri Lanka",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = CT.micro,
                                fontWeight = FontWeight.Bold,
                                color = CcGreen
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.cardTitle,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = CcInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderLogo(name = course.provider, size = 17)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Provided by ${course.provider}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        color = CcMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CertificateBadge(course.certificate)
                    Text(
                        text = "${course.level} · ${course.durationLabel}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.micro,
                        color = CcMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(CcBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CcMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CertificateBadge(certificate: CatalogCertificate) {
    val (tint, background) = when (certificate) {
        CatalogCertificate.FREE -> CcGreen to CcGreenSoft
        CatalogCertificate.PAID -> CcAmber to CcAmberSoft
        CatalogCertificate.PARTICIPATION -> CcBlue to CcBlueSoft
        CatalogCertificate.NONE -> CcMuted to Color(0xFFF1F5F9)
    }
    Row(
        modifier = Modifier
            .heightIn(min = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = certificate.short,
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.micro,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1
        )
    }
}

/**
 * The rows' loading state, used when the pager changes page. Same height as a
 * real card, so the pager below it does not jump when the rows arrive.
 */
@Composable
private fun CourseRowsSkeleton(brush: Brush, count: Int) {
    Column(modifier = Modifier.testTag("courses_page_skeleton")) {
        repeat(count) {
            ShimmerBlock(
                brush,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(104.dp),
                18
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Page controls under the results.
 *
 * Numbered rather than infinite scroll on purpose: the count is small, a student
 * wants to know how much is left, and "15 at a time" keeps the page short. The
 * row also says exactly which slice is on screen ("16–30 of 41").
 */
@Composable
private fun CoursePager(
    page: Int,
    pageCount: Int,
    shown: Int,
    total: Int,
    firstIndex: Int,
    onPage: (Int) -> Unit,
    onJumpToResults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("courses_pager")
    ) {
        Text(
            text = "Showing $firstIndex–${firstIndex + shown - 1} of $total",
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.micro,
            color = CcMuted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PagerArrow(
                label = "Prev",
                enabled = page > 0,
                tag = "courses_page_prev",
                onClick = {
                    onPage(page - 1)
                    onJumpToResults()
                }
            )
            Spacer(Modifier.width(8.dp))
            (0 until pageCount).forEach { index ->
                val selected = index == page
                Surface(
                    onClick = {
                        if (!selected) {
                            onPage(index)
                            onJumpToResults()
                        }
                    },
                    shape = CircleShape,
                    color = if (selected) CcInk else Color.White,
                    border = BorderStroke(1.dp, if (selected) CcInk else CcHairline),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(34.dp)
                        .testTag("courses_page_${index + 1}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.meta,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else CcMuted
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            PagerArrow(
                label = "Next",
                enabled = page < pageCount - 1,
                tag = "courses_page_next",
                onClick = {
                    onPage(page + 1)
                    onJumpToResults()
                }
            )
        }
    }
}

@Composable
private fun PagerArrow(
    label: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(50),
        color = Color.White,
        border = BorderStroke(1.dp, CcHairline),
        modifier = Modifier
            .height(34.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = CT.micro,
                fontWeight = FontWeight.Bold,
                color = if (enabled) CcInk else CcFaint,
                modifier = Modifier.padding(horizontal = 13.dp)
            )
        }
    }
}

@Composable
private fun NoCoursesState(onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 30.dp)
            .testTag("courses_empty"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No courses match that",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CcInk
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Try a different subject or remove a filter.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.body,
            color = CcMuted
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            onClick = onClear,
            shape = RoundedCornerShape(50),
            color = CcInk,
            modifier = Modifier
                .height(42.dp)
                .testTag("courses_empty_clear")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Clear filters",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.body,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun CreditBlock() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CcHairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp)
            .testTag("courses_credit_block")
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = "Every course here belongs to someone else",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CcInk
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "TheStudent does not create, host, sell or re-upload these courses. " +
                    "Each card links to the provider's own page, where you enrol under their " +
                    "terms. Certificate availability and prices are theirs and can change — " +
                    "check the course page before you start. Our thanks to every university " +
                    "and platform below for keeping their work open.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = CT.meta,
                lineHeight = CT.metaHeight,
                color = CcMuted
            )
            Spacer(Modifier.height(12.dp))
            CourseCatalog.usedProviders.forEach { provider ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProviderLogo(name = provider.name, size = 24)
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = provider.name,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.meta,
                            fontWeight = FontWeight.SemiBold,
                            color = CcInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = provider.kind,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.micro,
                            color = CcMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Detail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CourseDetail(
    course: CatalogCourse,
    onBack: () -> Unit,
    onOpenProvider: () -> Unit,
    onPlayVideo: (String) -> Unit
) {
    // Opening a card is a page change, so it gets the same ~0.4s of shimmer the
    // list does — the detail never snaps in fully formed.
    var loading by remember(course.id) { mutableStateOf(true) }
    LaunchedEffect(course.id) {
        delay(420)
        loading = false
    }
    if (loading) {
        CourseDetailSkeleton(course = course, onBack = onBack)
        return
    }

    val tint = paperCoverTint(course.subject)
    val video = course.videoId?.let { CourseCatalog.video(it) }

    Column(modifier = Modifier.fillMaxSize().testTag("course_detail_${course.id}")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f), CcInk)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("course_detail_back")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = course.subject.uppercase(),
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.micro,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.hero,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderLogo(name = course.provider, onDark = true, size = 24)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Provided by ${course.provider}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.body,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "facts") { FactsCard(course) }

            item(key = "certificate") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CcHairline)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        CertificateBadge(course.certificate)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = course.priceNote,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.meta,
                            lineHeight = CT.metaHeight,
                            color = CcMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "about") {
                Column {
                    SectionHeading("About this course")
                    Text(
                        text = course.summary,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.body,
                        lineHeight = CT.bodyHeight,
                        color = CcMuted
                    )
                }
            }

            if (course.skills.isNotEmpty()) {
                item(key = "skills") {
                    Column {
                        SectionHeading("What you'll learn")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            course.skills.forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                        .padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = skill,
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = CT.meta,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CcInk,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (video != null) {
                item(key = "video") {
                    Column {
                        SectionHeading("Watch first")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CcInk)
                                .clickable { onPlayVideo(video.videoId) }
                                .testTag("course_detail_video")
                        ) {
                            AsyncImage(
                                model = video.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0xCC0F172A))
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.94f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = CcInk,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(11.dp)
                            ) {
                                Text(
                                    text = video.title,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = CT.meta,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${video.channel} · ${video.minutes} min",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = CT.micro,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item(key = "credit") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CcBg,
                    border = BorderStroke(1.dp, CcHairline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Ownership",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.meta,
                            fontWeight = FontWeight.ExtraBold,
                            color = CcInk
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "This course is created, owned and delivered by " +
                                "${course.provider}. TheStudent lists it as a courtesy and " +
                                "takes no fee; enrolment, content and certificates are " +
                                "handled entirely by them.",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = CT.meta,
                            lineHeight = CT.metaHeight,
                            color = CcMuted
                        )
                    }
                }
            }
        }

        // Sticky action: leaves the app for the provider's own page.
        Surface(
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                onClick = onOpenProvider,
                shape = RoundedCornerShape(14.dp),
                color = CcGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
                    .testTag("course_detail_open")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open on ${course.provider}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = CT.cardTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        fontFamily = MaisonNeueFontFamily,
        fontSize = CT.section,
        fontWeight = FontWeight.ExtraBold,
        color = CcInk,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/**
 * Level, time and audience. Two columns *plus* a full-width row for "For", so a
 * long audience list wraps instead of being clipped on a narrow phone.
 */
@Composable
private fun FactsCard(course: CatalogCourse) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CcHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Fact(label = "Level", value = course.level, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(36.dp)
                        .background(CcHairline)
                )
                Fact(label = "Time", value = course.durationLabel, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CcHairline)
            )
            Spacer(Modifier.height(12.dp))
            Fact(
                label = "For",
                value = course.stages.joinToString(" · "),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.micro,
            fontWeight = FontWeight.Bold,
            color = CcFaint,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = CT.body,
            lineHeight = CT.metaHeight,
            fontWeight = FontWeight.Bold,
            color = CcInk,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Detail loading state
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The detail page's loading state. It mirrors the real page — coloured hero,
 * facts card, certificate row, about text, video, sticky button — so nothing
 * jumps when the content arrives. The back button is real from the first frame:
 * a loading state must never trap you on a screen.
 *
 * It lives here, above the player section, on purpose: two rewrites of that last
 * section took the end of this file with them and left the call site orphaned.
 */
@Composable
private fun CourseDetailSkeleton(course: CatalogCourse, onBack: () -> Unit) {
    val tint = paperCoverTint(course.subject)
    val light = rememberShimmerBrush()
    val dark = rememberShimmerBrush(onDark = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("course_detail_skeleton")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.9f), tint.copy(alpha = 0.6f))
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 18.dp)
        ) {
            Column {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("course_detail_back")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(dark)
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(dark)
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(dark)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(dark)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBlock(light, Modifier.fillMaxWidth().height(108.dp), 16)
            ShimmerBlock(light, Modifier.fillMaxWidth().height(64.dp), 16)
            ShimmerBlock(light, Modifier.width(150.dp).height(16.dp), 6)
            ShimmerBlock(light, Modifier.fillMaxWidth().height(38.dp), 10)
            ShimmerBlock(light, Modifier.width(150.dp).height(16.dp), 6)
            ShimmerBlock(light, Modifier.fillMaxWidth().aspectRatio(16f / 9f), 16)
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp)
        ) {
            ShimmerBlock(light, Modifier.fillMaxSize(), 14)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  YouTube
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A real player, inside the app — rendered as an in-composition overlay.
 *
 * Two things are load-bearing here, and both were learned the hard way:
 *
 *   1. **It is not a `Dialog`.** The YouTube player paints its video through a
 *      surface of its own; a dialog window is a second window, and on many
 *      devices that second window composites everything except the video frames
 *      — the controls respond, the audio plays, the picture is blank. Staying in
 *      the activity's own window (which is hardware accelerated) fixes it.
 *   2. **Nothing clips the `WebView`.** A `Modifier.clip()` above it forces the
 *      view into an offscreen layer, with the same result: a white or black
 *      rectangle. The video block is therefore square-cornered, and only the
 *      card's *background* is rounded (`background(shape)` draws a shape without
 *      clipping children).
 *
 * The `Referer` header is still required — YouTube refuses to configure a
 * player (error 152/153) for an embed that cannot say which app hosts it — and
 * it must match the `origin=` parameter, so both come from one value.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlayerOverlay(
    video: CatalogVideo,
    onDismiss: () -> Unit,
    onOpenYouTube: () -> Unit
) {
    val context = LocalContext.current
    val origin = remember(context) { "https://" + context.packageName }

    var loading by remember(video.videoId) { mutableStateOf(true) }
    var failed by remember(video.videoId) { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20B1220))
            // Swallow everything that is not meant for the card. Compose hit-tests
            // siblings from the top down and stops at the first one that takes the
            // gesture, so with this the catalogue behind cannot scroll, cannot
            // fling, and cannot open another course while a video is playing.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .testTag("course_video_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(Color.White, RoundedCornerShape(18.dp))
        ) {
            // No clip, no shadow elevation over the WebView: plain black box.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .testTag("course_video_player")
            ) {
                if (failed) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "This video will not play here",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = CT.body,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                onClick = onOpenYouTube,
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFDC2626),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("course_video_fallback")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Open in YouTube",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = CT.meta,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            // A FrameLayout, not a bare WebView: the web core hands
                            // the video surface back through
                            // WebChromeClient.onShowCustomView, and if nobody
                            // attaches that view the player runs — sound, controls,
                            // scrubbing — over a white rectangle. This host is where
                            // it gets attached.
                            val host = FrameLayout(ctx)
                            val webView = WebView(ctx).apply {
                                setBackgroundColor(AndroidColor.BLACK)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    mixedContentMode =
                                        WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                }
                                webChromeClient = VideoChromeClient(host)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String?) {
                                        loading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        error: WebResourceError
                                    ) {
                                        if (request.isForMainFrame) {
                                            loading = false
                                            failed = true
                                        }
                                    }
                                }
                                loadUrl(
                                    "https://www.youtube.com/embed/" +
                                        video.videoId +
                                        "?autoplay=1&playsinline=1&rel=0" +
                                        "&modestbranding=1&origin=" + origin,
                                    mapOf("Referer" to origin)
                                )
                            }
                            host.addView(
                                webView,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                            host
                        },
                        onRelease = { host ->
                            host.getChildAt(0)?.let { child ->
                                if (child is WebView) {
                                    child.stopLoading()
                                    child.loadUrl("about:blank")
                                    child.destroy()
                                }
                            }
                            host.removeAllViews()
                        }
                    )

                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = video.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.cardTitle,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = CcInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${video.channel} on YouTube",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = CT.meta,
                    color = CcMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = CcBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("course_video_close")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Close",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = CT.body,
                                fontWeight = FontWeight.Bold,
                                color = CcInk
                            )
                        }
                    }
                    Surface(
                        onClick = onOpenYouTube,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("course_video_open_youtube")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Watch on YouTube",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = CT.body,
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
}

/**
 * Attaches the view the web core hands back for video output.
 *
 * Chromium renders `<video>` into a view of its own and delivers it through
 * `onShowCustomView`. Every embed sample that "works but shows a white box" is
 * missing this: the page, the audio and the controls are all rendered by the
 * WebView, while the picture is a view nobody added to the hierarchy. Adding it
 * to the same `FrameLayout` as the WebView — and removing it again on
 * `onHideCustomView` — is the whole fix, and it also gives fullscreen for free.
 */
private class VideoChromeClient(
    private val host: FrameLayout
) : WebChromeClient() {

    private var customView: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (customView != null) {
            // A second request replaces the first rather than stacking.
            onHideCustomView()
        }
        this.customView = view
        this.callback = callback
        view.setBackgroundColor(AndroidColor.BLACK)
        host.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onHideCustomView() {
        customView?.let { host.removeView(it) }
        customView = null
        callback?.onCustomViewHidden()
        callback = null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Opening the collection from a course card
// ─────────────────────────────────────────────────────────────────────────────

/** What a card's category turns into once the catalogue has had its say. */
private sealed interface CatalogSeed {
    /** No filter: the catalogue simply opens. */
    data object None : CatalogSeed

    /** A real subject facet, applied as a chip. */
    data class Subject(val value: String) : CatalogSeed

    /** Not a facet, but it matches courses: applied as a search term. */
    data class Text(val value: String) : CatalogSeed
}

/**
 * The dashboard's course categories ("Physical Science", "Business & Finance")
 * are not the catalogue's subjects ("Mathematics", "Business"), so a raw match
 * would find nothing. These aliases bridge the two; anything still unmatched
 * opens the collection unfiltered rather than showing an empty page.
 */
private val CATEGORY_ALIASES: Map<String, String> = mapOf(
    "business & finance" to "Business",
    "business and finance" to "Business",
    "academic & research" to "Academic Writing",
    "academic and research" to "Academic Writing",
    "general english" to "Communication",
    "speaking & presentation" to "Communication",
    "computer science" to "Computer Science",
    "information technology" to "Computer Science"
)

private fun resolveSeed(raw: String?): CatalogSeed {
    val text = raw?.trim().orEmpty()
    if (text.isBlank()) return CatalogSeed.None

    val subject = CourseCatalog.courses
        .firstOrNull { it.subject.equals(text, ignoreCase = true) }
        ?.subject
    if (subject != null) return CatalogSeed.Subject(subject)

    val alias = CATEGORY_ALIASES[text.lowercase()]
    if (alias != null) return CatalogSeed.Subject(alias)

    val matches = CourseCatalog.courses.any { course ->
        course.title.contains(text, true) ||
            course.subject.contains(text, true) ||
            course.provider.contains(text, true) ||
            course.skills.any { it.contains(text, true) }
    }
    return if (matches) CatalogSeed.Text(text) else CatalogSeed.None
}
