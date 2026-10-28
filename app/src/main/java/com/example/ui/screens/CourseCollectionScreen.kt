package com.example.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.CatalogCertificate
import com.example.data.model.CatalogCourse
import com.example.data.model.CatalogProvider
import com.example.data.model.CatalogVideo
import com.example.data.model.CourseCatalog
import com.example.ui.components.paperCoverTint
import com.example.ui.theme.MaisonNeueFontFamily
import kotlinx.coroutines.delay

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
    modifier: Modifier = Modifier
) {
    var selectedCourse by remember { mutableStateOf<CatalogCourse?>(null) }
    var playingVideo by remember { mutableStateOf<CatalogVideo?>(null) }

    val uriHandler = LocalUriHandler.current
    val openUrl: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    BackHandler(enabled = selectedCourse != null) { selectedCourse = null }

    Surface(color = CcBg, modifier = modifier.fillMaxSize()) {
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
                onOpenCourse = { selectedCourse = it },
                onPlayVideo = { playingVideo = it }
            )
        }

        playingVideo?.let { video ->
            YouTubeEmbedDialog(
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

// ─────────────────────────────────────────────────────────────────────────────
//  Browse
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseCatalogBrowse(
    onBack: () -> Unit,
    onOpenCourse: (CatalogCourse) -> Unit,
    onPlayVideo: (CatalogVideo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var certificate by remember { mutableStateOf<CatalogCertificate?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf<String?>(null) }
    var provider by remember { mutableStateOf<String?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val featured = remember { CourseCatalog.courses.filter { it.featured } }
    val activeFilters = listOfNotNull(
        certificate?.short,
        stage,
        level,
        subject,
        provider,
        if (query.isNotBlank()) "“$query”" else null
    )
    val filtering = activeFilters.isNotEmpty()

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

    // Facet changes re-shimmer. Typing does not: flashing the list on every
    // keystroke would be worse than no animation at all.
    LaunchedEffect(facetKey) {
        if (booted) {
            isLoading = true
            delay(420)
            isLoading = false
        }
    }

    // A filter changes what is above the list, so the old scroll offset can land
    // the student at the bottom of a much shorter page: go back to the top.
    LaunchedEffect(filterKey) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
    }

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
                        focusedBorderColor = CcGreen,
                        unfocusedBorderColor = CcHairline,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                } else {
                    items(visible, key = { it.id }) { course ->
                        CourseRow(course = course, onClick = { onOpenCourse(course) })
                        Spacer(Modifier.height(8.dp))
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
            fontSize = 10.5.sp,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val active = ((state.firstVisibleItemIndex % featured.size) + featured.size) % featured.size
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
        if (provider == null) {
            ProviderInitials(name = name, size = size, onDark = onDark)
        } else {
            SubcomposeAsyncImage(
                model = provider.logoUrl,
                contentDescription = provider.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size((size * 0.64f).dp),
                loading = { ProviderInitials(name = name, size = size, onDark = onDark) },
                error = { ProviderInitials(name = name, size = size, onDark = onDark) }
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
                    SubcomposeAsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { Box(Modifier.fillMaxSize().background(CcInk)) },
                        error = { Box(Modifier.fillMaxSize().background(CcInk)) }
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
                        fontSize = 13.5.sp,
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
                        lineHeight = 15.sp,
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
                fontSize = 13.5.sp,
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
                            SubcomposeAsyncImage(
                                model = video.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = { Box(Modifier.fillMaxSize().background(CcInk)) },
                                error = { Box(Modifier.fillMaxSize().background(CcInk)) }
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
//  YouTube
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A real player, inside the app.
 *
 * YouTube now refuses to configure a player (error 152/153, "video player
 * configuration error") for an embed that arrives without a **Referer**: the
 * iframe has to be able to say which app is hosting it. A plain
 * `loadUrl("…/embed/…")` sends none, which is exactly how the first two attempts
 * failed — first as a black box, then through a library that cannot set the
 * header either.
 *
 * So the WebView loads the embed URL *with an explicit `Referer` header* whose
 * origin is the app's own package (`https://com.thestudent.sl`), and passes the
 * same value as `origin=` on the URL so the player's postMessage handshake
 * matches. That pair is what the player needs; nothing else about the embed is
 * unusual, ads and attribution stay intact, and the channel is credited
 * underneath with a "Watch on YouTube" exit.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeEmbedDialog(
    video: CatalogVideo,
    onDismiss: () -> Unit,
    onOpenYouTube: () -> Unit
) {
    val context = LocalContext.current
    // A syntactically valid https origin that belongs to this app. Used for both
    // the Referer header and the player's `origin` parameter, so they agree.
    val origin = remember(context) { "https://" + context.packageName }

    var loading by remember(video.videoId) { mutableStateOf(true) }
    var failed by remember(video.videoId) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color.Black)
                        .testTag("course_video_player")
                ) {
                    if (failed) {
                        // The player could not start: show the thumbnail and a
                        // way out instead of an empty rectangle.
                        SubcomposeAsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { Box(Modifier.fillMaxSize().background(CcInk)) },
                            error = { Box(Modifier.fillMaxSize().background(CcInk)) }
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
                                WebView(ctx).apply {
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
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(
                                            view: WebView,
                                            url: String?
                                        ) {
                                            loading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView,
                                            request: WebResourceRequest,
                                            error: WebResourceError
                                        ) {
                                            // Only the main document matters; a
                                            // failed sub-resource is not fatal.
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
                            },
                            onRelease = { webView ->
                                webView.stopLoading()
                                webView.loadUrl("about:blank")
                                webView.destroy()
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
}

/**
 * The detail page's loading state. It mirrors the real page — coloured hero,
 * facts card, certificate row, about text, video, sticky button — so nothing
 * jumps when the content arrives. The back button is real from the first frame:
 * a loading state must never trap you on a screen.
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
