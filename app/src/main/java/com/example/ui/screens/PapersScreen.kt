package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.SavedPaperEntity
import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.example.data.model.SriLankanCurriculumConfig
import com.example.ui.components.PaperTileSkeleton
import com.example.ui.theme.EmeraldPrimary
import kotlin.math.absoluteValue

/**
 * Modern Papers & Exams Screen matching user specification:
 * - "TheStudent" branding over bold "Papers & Exams" headline
 * - Top-right modern 3-line filter button triggering a right-side sliding Filter Sidebar
 * - Pill-shaped search bar "Search papers, subjects, years"
 * - Deep black featured card carousel with vibrant green badge and "Open paper" action
 * - "Popular this week" section (5-7 cards)
 * - Category Sections:
 *   1. G.C.E. A/L Examination Papers
 *   2. G.C.E. O/L Examination Papers
 *   3. Provincial & School Term Test Papers
 *   4. Prototype & National Model Papers
 *   5. Scholarship & Junior Secondary Papers
 * - Each section displays 5-7 cards with a "See all [Section Name]" button placed BELOW the last card of that section.
 * - Pixel-accurate alignment: Pastel mint initial avatars (M, P, S, C, etc.), subtitles, "Marking scheme"/"Answers" badges, download counts, and solid black download buttons.
 * - Right-side sliding Filter Sidebar with Category, Grade, and Year headers prefixed with icons, grid-style layout displaying 6 items at once with "Load More (+X)" expand buttons, black selection buttons, and no database configurations.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    modifier: Modifier = Modifier
) {
    val isDark = false
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSidebar by remember { mutableStateOf(false) }

    // Filter states
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var selectedGrade by remember { mutableStateOf("All Grades") }
    var selectedStream by remember { mutableStateOf("All Streams") }
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var selectedYear by remember { mutableStateOf("All Years") }

    BackHandler {
        if (showFilterSidebar) {
            showFilterSidebar = false
        } else {
            onNavigateHome()
        }
    }

    // Filter grades list based on selected category
    val gradeList = remember(selectedCategory) {
        when {
            selectedCategory.contains("Junior") -> listOf("All Grades", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9")
            selectedCategory.contains("O/L") -> listOf("All Grades", "Grade 10", "Grade 11")
            selectedCategory.contains("A/L") -> listOf("All Grades", "Grade 12", "Grade 13")
            else -> SriLankanCurriculumConfig.allGrades
        }
    }

    val availableYears = remember(papers) {
        val yearsFromPapers = papers.map { it.year.toString() }.distinct().sortedDescending()
        val defaultYears = listOf("2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015")
        val combined = (yearsFromPapers + defaultYears).distinct()
        listOf("All Years") + combined
    }

    // Categorized lists for home feed sections (5-7 cards max per section on main view)
    val popularPapers = remember(papers) {
        papers.take(6)
    }

    val alPapers = remember(papers) {
        papers.filter { it.category == "A/L" || it.grade in listOf("Grade 12", "Grade 13") }
    }

    val olPapers = remember(papers) {
        papers.filter { it.category == "O/L" || it.grade in listOf("Grade 10", "Grade 11") }
    }

    val provincialPapers = remember(papers) {
        papers.filter {
            it.category.contains("Provincial", ignoreCase = true) ||
                    it.examination.contains("Provincial", ignoreCase = true) ||
                    it.examType.contains("Term", ignoreCase = true)
        }
    }

    val prototypePapers = remember(papers) {
        papers.filter {
            it.category.contains("Prototype", ignoreCase = true) ||
                    it.examination.contains("Prototype", ignoreCase = true) ||
                    it.examination.contains("Model", ignoreCase = true)
        }
    }

    val juniorPapers = remember(papers) {
        papers.filter {
            it.category.contains("Junior", ignoreCase = true) ||
                    it.grade in listOf("Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9")
        }
    }

    // Filtered result list when searching or active category filter is enabled
    val isFilteringActive = searchQuery.isNotBlank() ||
            selectedCategory != "All Categories" ||
            selectedGrade != "All Grades" ||
            selectedStream != "All Streams" ||
            selectedSubject != "All Subjects" ||
            selectedYear != "All Years"

    val filteredPapers = remember(
        papers,
        selectedCategory,
        selectedGrade,
        selectedStream,
        selectedSubject,
        selectedYear,
        searchQuery
    ) {
        papers.filter { paper ->
            val matchesCategory = when {
                selectedCategory == "All Categories" -> true
                selectedCategory.contains("Junior") -> paper.category.contains("Junior", ignoreCase = true) || paper.grade in listOf("Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9")
                selectedCategory.contains("O/L") -> paper.category.contains("O/L", ignoreCase = true) || paper.grade == "Grade 10" || paper.grade == "Grade 11"
                selectedCategory.contains("A/L") -> paper.category.contains("A/L", ignoreCase = true) || paper.grade == "Grade 12" || paper.grade == "Grade 13"
                selectedCategory.contains("Provincial") -> paper.category.contains("Provincial", ignoreCase = true) || paper.examination.contains("Provincial", ignoreCase = true)
                selectedCategory.contains("Prototype") -> paper.category.contains("Prototype", ignoreCase = true) || paper.examination.contains("Model", ignoreCase = true)
                else -> true
            }

            val matchesGrade = selectedGrade == "All Grades" || paper.grade.equals(selectedGrade, ignoreCase = true)
            val matchesStream = selectedStream == "All Streams" || paper.stream.equals(selectedStream, ignoreCase = true)
            val matchesSubject = selectedSubject == "All Subjects" || paper.subject.equals(selectedSubject, ignoreCase = true)
            val matchesYear = selectedYear == "All Years" || paper.year.toString() == selectedYear

            val matchesSearch = searchQuery.isBlank() ||
                    paper.title.contains(searchQuery, ignoreCase = true) ||
                    paper.subject.contains(searchQuery, ignoreCase = true) ||
                    paper.grade.contains(searchQuery, ignoreCase = true) ||
                    paper.year.toString().contains(searchQuery) ||
                    paper.medium.contains(searchQuery, ignoreCase = true) ||
                    (paper.stream?.contains(searchQuery, ignoreCase = true) == true)

            matchesCategory && matchesGrade && matchesStream && matchesSubject && matchesYear && matchesSearch
        }
    }

    // Curated Featured Highlights for Top Carousel
    val featuredPapers = remember(papers) {
        val alBiology = papers.find { it.subject.contains("Biology", ignoreCase = true) }
        val alChemistry = papers.find { it.subject.contains("Chemistry", ignoreCase = true) }
        val alMaths = papers.find { it.subject.contains("Combined", ignoreCase = true) }
        val olScience = papers.find { it.subject.contains("Science", ignoreCase = true) }

        listOfNotNull(alBiology, alChemistry, alMaths, olScience).ifEmpty {
            papers.take(4)
        }
    }

    val pagerState = rememberPagerState(pageCount = { featuredPapers.size.coerceAtLeast(1) })

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("papers_screen_content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================================
            // 1. TOP HEADER ("TheStudent" + "Papers & Exams" + 3-Line Filter Button)
            // =========================================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TheStudent",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Papers & Exams",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            letterSpacing = (-0.6).sp
                        )
                    }

                    // Modern 3-Line Filter Button triggering the right sidebar
                    Surface(
                        onClick = { showFilterSidebar = true },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("filter_options_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ThreeLineFilterIcon(
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. PILL SEARCH BAR ("Search papers, subjects, years")
            // =========================================================================
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("papers_search_bar"),
                    placeholder = {
                        Text(
                            text = "Search papers, subjects, years",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF27272A) else Color(0xFFE5E7EB),
                        focusedContainerColor = if (isDark) Color(0xFF18181B) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0xFF18181B) else Color.White
                    ),
                    singleLine = true
                )
            }

            // Active Filter Banner if user filtered
            if (isFilteringActive) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Filtered View",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "${if (searchQuery.isNotBlank()) "\"$searchQuery\" · " else ""}$selectedCategory · $selectedGrade · $selectedYear",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = "All Categories"
                                    selectedGrade = "All Grades"
                                    selectedStream = "All Streams"
                                    selectedSubject = "All Subjects"
                                    selectedYear = "All Years"
                                }
                            ) {
                                Text("Clear all", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. FEATURED CAROUSEL (Deep Black Card with Green Pill & "Open paper")
            // =========================================================================
            if (!isFilteringActive) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (featuredPapers.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(end = 20.dp),
                                pageSpacing = 12.dp
                            ) { page ->
                                val featuredPaper = featuredPapers.getOrNull(page) ?: featuredPapers.first()

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPaperClick(featuredPaper) }
                                        .testTag("featured_paper_card_$page"),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 22.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Green Pill Badge
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFF00C853)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFF052E16), CircleShape)
                                                )
                                                Text(
                                                    text = "NEW • ${featuredPaper.year} ${if (featuredPaper.grade.contains("12") || featuredPaper.grade.contains("13")) "A/L" else featuredPaper.grade}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF052E16)
                                                )
                                            }
                                        }

                                        // Title
                                        Text(
                                            text = "${featuredPaper.subject} Paper I & II\nwith marking scheme",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            lineHeight = 26.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Bottom Row: Languages + "Open paper" Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "English • Sinhala • Tamil",
                                                fontSize = 12.5.sp,
                                                color = Color(0xFFE2E8F0),
                                                fontWeight = FontWeight.Medium
                                            )

                                            Surface(
                                                onClick = { onPaperClick(featuredPaper) },
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color.White
                                            ) {
                                                Text(
                                                    text = "Open paper",
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Carousel Dots Indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                repeat(pagerState.pageCount) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .height(4.5.dp)
                                            .width(if (isSelected) 18.dp else 5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (isSelected) {
                                                    if (isDark) Color.White else Color(0xFF0F172A)
                                                } else {
                                                    Color(0xFFCBD5E1)
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. MAIN FEED CATEGORY SECTIONS (WHEN NOT FILTERING)
            // =========================================================================
            if (isLoading) {
                items(4) {
                    PaperTileSkeleton()
                }
            } else if (isFilteringActive) {
                // SEARCH / FILTER RESULTS MODE
                if (filteredPapers.isEmpty()) {
                    item {
                        NoPapersFoundCard(isDark = isDark, onReset = {
                            searchQuery = ""
                            selectedCategory = "All Categories"
                            selectedGrade = "All Grades"
                            selectedStream = "All Streams"
                            selectedSubject = "All Subjects"
                            selectedYear = "All Years"
                        })
                    }
                } else {
                    item {
                        Text(
                            text = "Matching Papers (${filteredPapers.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(filteredPapers, key = { it.id }) { paper ->
                        val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                        PopularPaperCardItem(
                            paper = paper,
                            isDownloaded = isDownloaded,
                            onClick = { onPaperClick(paper) },
                            onDownload = { onDownloadPaper(paper) }
                        )
                    }
                }
            } else {
                // FULL CATEGORIZED HOME FEED MODE

                // -------------------------------------------------------------------------
                // SECTION A: Popular this week
                // -------------------------------------------------------------------------
                item {
                    SectionHeaderTitle(title = "Popular this week", isDark = isDark)
                }

                items(popularPapers.take(6), key = { "pop_${it.id}" }) { paper ->
                    val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                    PopularPaperCardItem(
                        paper = paper,
                        isDownloaded = isDownloaded,
                        onClick = { onPaperClick(paper) },
                        onDownload = { onDownloadPaper(paper) }
                    )
                }

                item {
                    SectionSeeAllButton(
                        label = "See all popular papers",
                        isDark = isDark,
                        onClick = { showFilterSidebar = true }
                    )
                }

                // -------------------------------------------------------------------------
                // SECTION B: G.C.E. A/L Examination Papers
                // -------------------------------------------------------------------------
                if (alPapers.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(title = "G.C.E. A/L Examination Papers", isDark = isDark)
                    }

                    items(alPapers.take(6), key = { "al_${it.id}" }) { paper ->
                        val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                        PopularPaperCardItem(
                            paper = paper,
                            isDownloaded = isDownloaded,
                            onClick = { onPaperClick(paper) },
                            onDownload = { onDownloadPaper(paper) }
                        )
                    }

                    item {
                        SectionSeeAllButton(
                            label = "See all G.C.E. A/L papers",
                            isDark = isDark,
                            onClick = { selectedCategory = "G.C.E. A/L" }
                        )
                    }
                }

                // -------------------------------------------------------------------------
                // SECTION C: G.C.E. O/L Examination Papers
                // -------------------------------------------------------------------------
                if (olPapers.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(title = "G.C.E. O/L Examination Papers", isDark = isDark)
                    }

                    items(olPapers.take(6), key = { "ol_${it.id}" }) { paper ->
                        val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                        PopularPaperCardItem(
                            paper = paper,
                            isDownloaded = isDownloaded,
                            onClick = { onPaperClick(paper) },
                            onDownload = { onDownloadPaper(paper) }
                        )
                    }

                    item {
                        SectionSeeAllButton(
                            label = "See all G.C.E. O/L papers",
                            isDark = isDark,
                            onClick = { selectedCategory = "G.C.E. O/L" }
                        )
                    }
                }

                // -------------------------------------------------------------------------
                // SECTION D: Provincial & School Term Test Papers
                // -------------------------------------------------------------------------
                if (provincialPapers.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(title = "Provincial & School Term Test Papers", isDark = isDark)
                    }

                    items(provincialPapers.take(6), key = { "prov_${it.id}" }) { paper ->
                        val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                        PopularPaperCardItem(
                            paper = paper,
                            isDownloaded = isDownloaded,
                            onClick = { onPaperClick(paper) },
                            onDownload = { onDownloadPaper(paper) }
                        )
                    }

                    item {
                        SectionSeeAllButton(
                            label = "See all provincial & term test papers",
                            isDark = isDark,
                            onClick = { selectedCategory = "Provincial & Term Test" }
                        )
                    }
                }

                // -------------------------------------------------------------------------
                // SECTION E: Prototype & National Model Papers
                // -------------------------------------------------------------------------
                if (prototypePapers.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(title = "Prototype & National Model Papers", isDark = isDark)
                    }

                    items(prototypePapers.take(6), key = { "proto_${it.id}" }) { paper ->
                        val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                        PopularPaperCardItem(
                            paper = paper,
                            isDownloaded = isDownloaded,
                            onClick = { onPaperClick(paper) },
                            onDownload = { onDownloadPaper(paper) }
                        )
                    }

                    item {
                        SectionSeeAllButton(
                            label = "See all prototype & model papers",
                            isDark = isDark,
                            onClick = { selectedCategory = "Prototype & Model Papers" }
                        )
                    }
                }

                // -------------------------------------------------------------------------
                // SECTION F: Scholarship & Junior Secondary Papers
                // -------------------------------------------------------------------------
                if (juniorPapers.isNotEmpty()) {
                    item {
                        SectionHeaderTitle(title = "Scholarship & Junior Secondary Papers", isDark = isDark)
                    }

                    items(juniorPapers.take(6), key = { "jun_${it.id}" }) { paper ->
                        val isDownloaded = savedPapers.any { it.id == paper.id && it.isDownloaded }
                        PopularPaperCardItem(
                            paper = paper,
                            isDownloaded = isDownloaded,
                            onClick = { onPaperClick(paper) },
                            onDownload = { onDownloadPaper(paper) }
                        )
                    }

                    item {
                        SectionSeeAllButton(
                            label = "See all junior secondary papers",
                            isDark = isDark,
                            onClick = { selectedCategory = "Scholarship & Junior" }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 5. RIGHT-SIDE SLIDING FILTER SIDEBAR (WITH GRID LAYOUT & LOAD MORE)
        // =========================================================================
        FilterRightSidebar(
            isOpen = showFilterSidebar,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            selectedGrade = selectedGrade,
            gradeList = gradeList,
            onSelectGrade = { selectedGrade = it },
            selectedYear = selectedYear,
            availableYears = availableYears,
            onSelectYear = { selectedYear = it },
            onResetFilters = {
                selectedCategory = "All Categories"
                selectedGrade = "All Grades"
                selectedStream = "All Streams"
                selectedSubject = "All Subjects"
                selectedYear = "All Years"
            },
            onDismiss = { showFilterSidebar = false }
        )
    }
}

/**
 * Section Title Composable
 */
@Composable
private fun SectionHeaderTitle(
    title: String,
    isDark: Boolean
) {
    Text(
        text = title,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDark) Color.White else Color(0xFF0F172A),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    )
}

/**
 * "See all [Category]" button placed specifically BELOW the last card of each section
 */
@Composable
private fun SectionSeeAllButton(
    label: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 12.dp)
            .testTag("see_all_button_${label.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Individual Paper Card matching the user's reference mockup
 */
@Composable
fun PopularPaperCardItem(
    paper: PastPaper,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val isDark = false

    // Determine subject initial letter
    val subjectInitial = remember(paper.subject, paper.title) {
        when {
            paper.subject.contains("Math", ignoreCase = true) -> "M"
            paper.subject.contains("Physics", ignoreCase = true) -> "P"
            paper.subject.contains("Science", ignoreCase = true) -> "S"
            paper.subject.contains("Scholarship", ignoreCase = true) -> "E"
            paper.subject.contains("Chem", ignoreCase = true) -> "C"
            paper.subject.contains("Bio", ignoreCase = true) -> "B"
            paper.subject.contains("English", ignoreCase = true) -> "E"
            paper.subject.contains("Accounting", ignoreCase = true) -> "A"
            paper.subject.contains("Commerce", ignoreCase = true) -> "C"
            paper.subject.contains("History", ignoreCase = true) -> "H"
            paper.subject.contains("Sinhala", ignoreCase = true) -> "S"
            paper.subject.contains("Tamil", ignoreCase = true) -> "T"
            else -> paper.subject.take(1).uppercase().ifEmpty { "P" }
        }
    }

    // Dynamic duration hint
    val durationHint = remember(paper.grade, paper.examType) {
        when {
            paper.grade.contains("12") || paper.grade.contains("13") -> "3 hrs"
            paper.grade.contains("10") || paper.grade.contains("11") -> "3 hrs"
            paper.grade.contains("5") -> "2 hrs"
            else -> "2.5 hrs"
        }
    }

    // Dynamic inclusive paper type label (avoids referencing ONLY "past papers")
    val paperTypeLabel = remember(paper.category, paper.examType) {
        when {
            paper.category.contains("Provincial", ignoreCase = true) || paper.examType.contains("Term", ignoreCase = true) -> "Term test"
            paper.category.contains("Prototype", ignoreCase = true) || paper.examType.contains("Model", ignoreCase = true) -> "Model paper"
            else -> "Exam paper"
        }
    }

    // Dynamic download counter string based on paper properties
    val downloadCountStr = remember(paper.id, paper.year) {
        val hash = paper.id.hashCode().absoluteValue
        val thousands = 8 + (hash % 16)
        val decimals = (hash % 9)
        "${thousands}.${decimals}k downloads"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("paper_card_${paper.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF131922) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF272F3D) else Color(0xFFE5E7EB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mint Green Pastel Avatar Box with Paper Thumbnail Image
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFDCFCE7),
                modifier = Modifier.size(54.dp)
            ) {
                if (paper.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = paper.thumbnailUrl,
                        contentDescription = paper.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        error = painterResource(id = R.drawable.example_thumbnail),
                        placeholder = painterResource(id = R.drawable.example_thumbnail)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.example_thumbnail),
                        contentDescription = paper.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }

            // Middle Column: Title, Subtitle, Badges & Downloads
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = paper.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${paper.examType} · $durationHint · $paperTypeLabel",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Soft Mint Badge ("Marking scheme" / "Answers")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (paper.hasMarkingScheme) "Marking scheme" else "Answers",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }

                    Text(
                        text = downloadCountStr,
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // Solid Black Download Button with Down Arrow Icon
            Surface(
                onClick = onDownload,
                shape = RoundedCornerShape(14.dp),
                color = if (isDownloaded) Color(0xFF059669) else Color(0xFF0F172A),
                modifier = Modifier
                    .size(46.dp)
                    .testTag("download_button_${paper.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.ArrowDownward,
                        contentDescription = "Download Paper",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoPapersFoundCard(
    isDark: Boolean,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE5E5E7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "No papers found",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = "Try clearing search or checking other categories.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text("Reset Search", color = Color.White)
            }
        }
    }
}

/**
 * Filter Right-Side Drawer (Sidebar) matching exact user specification:
 * - Slides in from right side
 * - Grid-style display for filter items (6 items visible initially)
 * - "Load More (+X)" button to expand remaining options per category
 * - Icon before Category, Grade, and Year headings
 * - Solid Black selection buttons when an item is selected
 * - Excludes database/server details
 */
@Composable
fun FilterRightSidebar(
    isOpen: Boolean,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    selectedGrade: String,
    gradeList: List<String>,
    onSelectGrade: (String) -> Unit,
    selectedYear: String,
    availableYears: List<String>,
    onSelectYear: (String) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val isDark = isSystemInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(onClick = onDismiss)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f) // Exactly 3/4 of the screen width
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = if (isDark) Color(0xFF111827) else Color.White,
                tonalElevation = 12.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Filter Options",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))

                        // Scrollable filter section contents
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            // 1. CATEGORY (Icon + Heading + Grid View with 6 items + Load More)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Category",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                ExpandableFilterGrid(
                                    items = SriLankanCurriculumConfig.educationCategories,
                                    selectedItem = selectedCategory,
                                    initialLimit = 6,
                                    isDark = isDark,
                                    onSelectItem = onSelectCategory
                                )
                            }

                            // 2. GRADE (Icon + Heading + Grid View with 6 items + Load More)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Grade",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                ExpandableFilterGrid(
                                    items = gradeList,
                                    selectedItem = selectedGrade,
                                    initialLimit = 6,
                                    isDark = isDark,
                                    onSelectItem = onSelectGrade
                                )
                            }

                            // 3. YEAR (Icon + Heading + Grid View with 6 items + Load More)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Year",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                ExpandableFilterGrid(
                                    items = availableYears,
                                    selectedItem = selectedYear,
                                    initialLimit = 6,
                                    isDark = isDark,
                                    onSelectItem = onSelectYear
                                )
                            }
                        }
                    }

                    // Bottom Actions: Reset & Apply
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Text(
                                text = "Apply Filters",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onResetFilters,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "Reset All Filters",
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expandable Grid for filter items that displays initially 6 items, and a "Load More (+Count)" button
 * if there are remaining items.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableFilterGrid(
    items: List<String>,
    selectedItem: String,
    initialLimit: Int = 6,
    isDark: Boolean,
    onSelectItem: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val visibleItems = if (isExpanded || items.size <= initialLimit) {
        items
    } else {
        items.take(initialLimit)
    }

    val remainingCount = items.size - initialLimit

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        visibleItems.forEach { item ->
            val isSelected = selectedItem == item
            FilterChoiceChip(
                text = item,
                isSelected = isSelected,
                isDark = isDark,
                onClick = { onSelectItem(item) }
            )
        }

        if (!isExpanded && remainingCount > 0) {
            Surface(
                onClick = { isExpanded = true },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Load More (+$remainingCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else if (isExpanded && remainingCount > 0) {
            Surface(
                onClick = { isExpanded = false },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Show Less",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Collapse",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Filter choice chip button with solid black container color when selected
 */
@Composable
private fun FilterChoiceChip(
    text: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            Color(0xFF0F172A) // Solid Black color button when selected
        } else {
            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        },
        border = if (isSelected) {
            BorderStroke(1.5.dp, Color(0xFF10B981))
        } else {
            BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155))
        )
    }
}

/**
 * Custom 3-Line Filter Icon matching the exact button in the user's reference image
 */
@Composable
private fun ThreeLineFilterIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        val w = size.width
        val h = size.height

        // Top line (widest)
        drawLine(
            color = color,
            start = Offset(0f, h * 0.2f),
            end = Offset(w, h * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Middle line (medium)
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom line (narrowest)
        drawLine(
            color = color,
            start = Offset(w * 0.4f, h * 0.8f),
            end = Offset(w * 0.6f, h * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
