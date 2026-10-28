package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CatalogCertificate
import com.example.data.model.CatalogCourse
import com.example.data.model.CatalogVideo
import com.example.data.model.CourseCatalog
import com.example.ui.components.paperCoverTint
import com.example.ui.theme.MaisonNeueFontFamily

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
 * The courses collection — an index of **other people's** courses.
 *
 * Two rules run through the whole screen:
 *
 *   1. **Every card names its owner.** The provider is not a tag in the corner,
 *      it is a line on the card ("Provided by edX"), and the detail page repeats
 *      it before the button that leaves the app.
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
            CourseCatalogList(
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
//  Catalog list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CourseCatalogList(
    onBack: () -> Unit,
    onOpenCourse: (CatalogCourse) -> Unit,
    onPlayVideo: (CatalogVideo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var certificate by remember { mutableStateOf<CatalogCertificate?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var provider by remember { mutableStateOf<String?>(null) }

    val featured = remember { CourseCatalog.courses.filter { it.featured } }

    val visible = remember(query, certificate, stage, provider) {
        CourseCatalog.courses.filter { course ->
            (certificate == null || course.certificate == certificate) &&
                (stage == null || course.stages.contains(stage)) &&
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

    val filtering =
        query.isNotBlank() || certificate != null || stage != null || provider != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("courses_collection_content"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "header") {
            CatalogHeader(
                courseCount = CourseCatalog.courses.size,
                providerCount = CourseCatalog.usedProviders.size,
                onBack = onBack
            )
        }

        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp)
                    .height(50.dp)
                    .testTag("courses_search"),
                placeholder = {
                    Text(
                        text = "Search courses, subjects, providers",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        color = CcFaint
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = CcMuted,
                        modifier = Modifier.size(19.dp)
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
                    fontSize = 13.5.sp,
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

        // ── certificate + stage filters ──────────────────────────────────────
        item(key = "filters") {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "All",
                        selected = certificate == null,
                        tag = "courses_chip_all",
                        onClick = { certificate = null }
                    )
                    CatalogCertificate.entries.forEach { option ->
                        FilterPill(
                            label = option.label,
                            selected = certificate == option,
                            tag = "courses_chip_" + option.name.lowercase(),
                            onClick = {
                                certificate = if (certificate == option) null else option
                            }
                        )
                    }
                    FilterPill(
                        label = "Sri Lanka",
                        selected = provider == "University of Moratuwa",
                        tag = "courses_chip_local",
                        onClick = {
                            provider = if (provider == "University of Moratuwa") null
                            else "University of Moratuwa"
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("A/L", "Undergraduate", "Postgraduate").forEach { option ->
                        StagePill(
                            label = option,
                            selected = stage == option,
                            tag = "courses_stage_" + option.lowercase().replace("/", "_"),
                            onClick = { stage = if (stage == option) null else option }
                        )
                    }
                }
            }
        }

        // ── editor's picks slider ────────────────────────────────────────────
        if (!filtering && featured.isNotEmpty()) {
            item(key = "featured") {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SectionTitle("Editor's picks", "Hand-checked, free or free-to-audit")
                    FeaturedSlider(featured = featured, onOpenCourse = onOpenCourse)
                }
            }
        }

        // ── videos ───────────────────────────────────────────────────────────
        if (!filtering) {
            item(key = "videos") {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    SectionTitle("Watch first", "Plays in the app — credit to each channel")
                    VideoRail(videos = CourseCatalog.videos, onPlayVideo = onPlayVideo)
                }
            }
        }

        // ── providers ────────────────────────────────────────────────────────
        if (!filtering) {
            item(key = "providers") {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    SectionTitle("Where these come from", "Tap a provider to filter")
                    ProviderRail(
                        selected = provider,
                        onSelect = { name -> provider = if (provider == name) null else name }
                    )
                }
            }
        }

        // ── the list ─────────────────────────────────────────────────────────
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CcInk,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${visible.size} " + if (visible.size == 1) "course" else "courses",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.5.sp,
                    color = CcMuted
                )
                if (filtering) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Clear",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CcGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                query = ""
                                certificate = null
                                stage = null
                                provider = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("courses_clear_filters")
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                NoCoursesState(
                    onClear = {
                        query = ""
                        certificate = null
                        stage = null
                        provider = null
                    }
                )
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
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CcInk,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "$courseCount courses · $providerCount providers · credited to their owners",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
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

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, bottom = 10.dp)) {
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CcInk
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                color = CcMuted
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CcInk else Color.White,
        border = BorderStroke(1.dp, if (selected) CcInk else CcHairline),
        modifier = Modifier
            .height(32.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else CcMuted,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun StagePill(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CcGreenSoft else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) CcGreen.copy(alpha = 0.35f) else CcHairline
        ),
        modifier = Modifier
            .height(28.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) CcGreen else CcMuted,
                modifier = Modifier.padding(horizontal = 11.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Featured slider
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeaturedSlider(featured: List<CatalogCourse>, onOpenCourse: (CatalogCourse) -> Unit) {
    val state = rememberLazyListState()
    var dot by remember { mutableIntStateOf(0) }

    Column {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(featured, key = { it.id }) { course ->
                FeaturedCard(course = course, onClick = { onOpenCourse(course) })
            }
        }
        // Dots track the *first* visible card: enough to say "there is more",
        // without pretending this is a paged carousel.
        if (featured.size > 1) {
            Spacer(Modifier.height(8.dp))
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
                            .height(5.dp)
                            .width(if (index == dot) 16.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (index == dot) CcInk else Color(0xFFDDE3EC))
                    )
                }
            }
            androidx.compose.runtime.LaunchedEffect(state.firstVisibleItemIndex) {
                dot = state.firstVisibleItemIndex.coerceIn(0, featured.lastIndex)
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
            .width(268.dp)
            .height(178.dp)
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
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderAvatar(name = course.provider, onDark = true)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = course.subject.uppercase(),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Provided by ${course.provider}",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
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
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ProviderAvatar(name: String, onDark: Boolean = false, size: Int = 26) {
    val provider = CourseCatalog.provider(name)
    val initials = provider?.initials ?: name.take(2).uppercase()
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(
                if (onDark) Color.White.copy(alpha = 0.2f) else paperCoverTint(name).copy(alpha = 0.12f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontFamily = MaisonNeueFontFamily,
            fontSize = (size / 2.9f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (onDark) Color.White else paperCoverTint(name)
        )
    }
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
                    .width(232.dp)
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = CcInk,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${video.minutes} min",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
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
                Spacer(Modifier.height(7.dp))
                Text(
                    text = video.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CcInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channel,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 10.sp,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CourseCatalog.usedProviders, key = { it.name }) { provider ->
            val count = CourseCatalog.courses.count { it.provider == provider.name }
            val isSelected = selected == provider.name
            Surface(
                onClick = { onSelect(provider.name) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) CcGreenSoft else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) CcGreen.copy(alpha = 0.4f) else CcHairline
                ),
                modifier = Modifier
                    .width(150.dp)
                    .testTag("courses_provider_" + provider.name.lowercase().replace(' ', '_'))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    ProviderAvatar(name = provider.name, size = 28)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = provider.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CcInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$count " + if (count == 1) "course" else "courses",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
                        color = CcMuted
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
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = paperCoverTint(course.subject),
                        letterSpacing = 0.4.sp,
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
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CcGreen
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CcInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderAvatar(name = course.provider, size = 15)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Provided by ${course.provider}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.sp,
                        color = CcMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CertificateBadge(course.certificate)
                    Text(
                        text = "${course.level} · ${course.durationLabel}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
                        color = CcFaint,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CcBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CcMuted,
                    modifier = Modifier.size(17.dp)
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
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = certificate.short,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.sp,
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
            .padding(horizontal = 28.dp, vertical = 26.dp)
            .testTag("courses_empty"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No courses match that",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CcInk
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Try a different subject or remove a filter.",
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            color = CcMuted
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = onClear,
            shape = RoundedCornerShape(50),
            color = CcInk,
            modifier = Modifier.height(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Clear filters",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp)
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
            .padding(top = 20.dp)
            .testTag("courses_credit_block")
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = "Every course here belongs to someone else",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CcInk
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "TheStudent does not create, host, sell or re-upload these courses. " +
                    "Each card links to the provider's own page, where you enrol under their " +
                    "terms. Certificate availability and prices are theirs and can change — " +
                    "check the course page before you start. Our thanks to every university " +
                    "and platform below for keeping their work open.",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = CcMuted
            )
            Spacer(Modifier.height(10.dp))
            CourseCatalog.usedProviders.forEach { provider ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "·",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = CcFaint
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = provider.name,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CcInk
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = provider.kind,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.sp,
                        color = CcFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    val tint = paperCoverTint(course.subject)
    val video = course.videoId?.let { CourseCatalog.video(it) }

    Column(modifier = Modifier.fillMaxSize().testTag("course_detail_${course.id}")) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.7f), CcInk)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
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
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = course.subject.uppercase(),
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = course.title,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderAvatar(name = course.provider, onDark = true, size = 20)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Provided by ${course.provider}",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "facts") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CcHairline)
                ) {
                    Row(modifier = Modifier.padding(vertical = 12.dp)) {
                        Fact(label = "Level", value = course.level, modifier = Modifier.weight(1f))
                        Fact(
                            label = "Time",
                            value = course.durationLabel,
                            modifier = Modifier.weight(1f)
                        )
                        Fact(
                            label = "For",
                            value = course.stages.joinToString(", "),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }

            item(key = "certificate") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CcHairline)
                ) {
                    Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                        CertificateBadge(course.certificate)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = course.priceNote,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = CcMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "about") {
                Column {
                    Text(
                        text = "About this course",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CcInk,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Text(
                        text = course.summary,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = CcMuted
                    )
                }
            }

            if (course.skills.isNotEmpty()) {
                item(key = "skills") {
                    Column {
                        Text(
                            text = "What you'll learn",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CcInk,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            course.skills.forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = skill,
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CcInk
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
                        Text(
                            text = "Watch first",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CcInk,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
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
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.92f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = CcInk,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = video.title,
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${video.channel} · ${video.minutes} min",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 9.5.sp,
                                    color = Color.White.copy(alpha = 0.8f)
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
                    Column(modifier = Modifier.padding(13.dp)) {
                        Text(
                            text = "Ownership",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CcInk
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "This course is created, owned and delivered by " +
                                "${course.provider}. TheStudent lists it as a courtesy and " +
                                "takes no fee; enrolment, content and certificates are " +
                                "handled entirely by them.",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
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
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = CcInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 9.sp,
            color = CcFaint
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  YouTube
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A real embed, not a thumbnail that leaves the app: `youtube-nocookie.com`
 * keeps tracking off until the student presses play, and the channel name sits
 * under the player so the credit travels with the video.
 */
@Composable
private fun YouTubeEmbedDialog(
    video: CatalogVideo,
    onDismiss: () -> Unit,
    onOpenYouTube: () -> Unit
) {
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
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                setBackgroundColor(AndroidColor.BLACK)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webViewClient = WebViewClient()
                                loadUrl(
                                    "https://www.youtube-nocookie.com/embed/" +
                                        "${video.videoId}?rel=0&modestbranding=1&playsinline=1"
                                )
                            }
                        },
                        onRelease = { webView ->
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.destroy()
                        }
                    )
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = video.title,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CcInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${video.channel} on YouTube",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 10.5.sp,
                        color = CcMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            color = CcBg,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("course_video_close")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Close",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.sp,
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
                                .height(40.dp)
                                .testTag("course_video_open_youtube")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Watch on YouTube",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 12.sp,
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
