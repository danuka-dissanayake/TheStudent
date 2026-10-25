package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SavedPaperEntity
import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.example.data.model.SriLankanCurriculumConfig
import com.example.ui.components.PaperTileSkeleton
import com.example.ui.theme.*

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
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Navigation state
    // Categories: All, Junior Secondary, Ordinary Level (O/L), Advanced Level (A/L)
    var selectedCategory by remember { mutableStateOf("All Categories") }
    // Grade: All Grades, Grade 6 .. Grade 13
    var selectedGrade by remember { mutableStateOf("All Grades") }
    // Stream (for A/L: Grade 12 & 13): All Streams, Physical Science, Biological Science, Commerce, Arts, Technology
    var selectedStream by remember { mutableStateOf("All Streams") }
    // Subject filter
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    // Year filter
    var selectedYear by remember { mutableStateOf("All Years") }

    // Dropdown menu control states
    var isGradeDropdownExpanded by remember { mutableStateOf(false) }
    var isStreamDropdownExpanded by remember { mutableStateOf(false) }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }

    // Determine category based on selected grade
    val isAlActive = selectedGrade == "Grade 12" || selectedGrade == "Grade 13" || selectedCategory.contains("A/L")
    val isOlActive = selectedGrade == "Grade 10" || selectedGrade == "Grade 11" || selectedCategory.contains("O/L")
    val isJuniorActive = selectedGrade in listOf("Grade 6", "Grade 7", "Grade 8", "Grade 9") || selectedCategory.contains("Junior")

    // Filter grades list based on selected category
    val gradeList = remember(selectedCategory) {
        when {
            selectedCategory.contains("Junior") -> listOf("All Grades", "Grade 6", "Grade 7", "Grade 8", "Grade 9")
            selectedCategory.contains("O/L") -> listOf("All Grades", "Grade 10", "Grade 11")
            selectedCategory.contains("A/L") -> listOf("All Grades", "Grade 12", "Grade 13")
            else -> SriLankanCurriculumConfig.allGrades
        }
    }

    // Available subjects dynamically resolved from curriculum config + merged with live database papers
    val availableSubjects = remember(selectedGrade, selectedStream, selectedCategory, papers) {
        val dbSubjects = papers
            .filter { paper ->
                val matchesGrade = selectedGrade == "All Grades" || paper.grade.equals(selectedGrade, ignoreCase = true)
                val matchesStream = selectedStream == "All Streams" || paper.stream.equals(selectedStream, ignoreCase = true)
                val matchesCat = selectedCategory == "All Categories" ||
                        paper.category.contains(selectedCategory, ignoreCase = true) ||
                        (selectedCategory.contains("A/L") && (paper.grade == "Grade 12" || paper.grade == "Grade 13")) ||
                        (selectedCategory.contains("O/L") && (paper.grade == "Grade 10" || paper.grade == "Grade 11")) ||
                        (selectedCategory.contains("Junior") && paper.grade in listOf("Grade 6", "Grade 7", "Grade 8", "Grade 9"))
                matchesGrade && matchesStream && matchesCat
            }
            .map { it.subject }
            .distinct()

        SriLankanCurriculumConfig.getSubjectsForSelection(
            grade = selectedGrade,
            stream = selectedStream,
            databaseSubjects = dbSubjects
        )
    }

    // Ensure selected subject remains valid when grade/stream changes
    LaunchedEffect(selectedGrade, selectedStream, selectedCategory) {
        if (selectedSubject != "All Subjects" && !availableSubjects.contains(selectedSubject)) {
            selectedSubject = "All Subjects"
        }
    }

    // When student picks a grade, auto-sync category
    fun onSelectGrade(grade: String) {
        selectedGrade = grade
        if (grade == "All Grades") {
            // keep current category
        } else if (grade == "Grade 12" || grade == "Grade 13") {
            selectedCategory = SriLankanCurriculumConfig.CAT_AL
        } else if (grade == "Grade 10" || grade == "Grade 11") {
            selectedCategory = SriLankanCurriculumConfig.CAT_OL
            selectedStream = "All Streams"
        } else if (grade in listOf("Grade 6", "Grade 7", "Grade 8", "Grade 9")) {
            selectedCategory = SriLankanCurriculumConfig.CAT_JUNIOR
            selectedStream = "All Streams"
        }
    }

    // Available years from papers
    val availableYears = remember(papers) {
        val years = papers.map { it.year.toString() }.distinct().sortedDescending()
        listOf("All Years") + if (years.isNotEmpty()) years else listOf("2024", "2023", "2022")
    }

    // Filter logic
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
            // Category check
            val matchesCategory = when {
                selectedCategory == "All Categories" -> true
                selectedCategory.contains("Junior") -> {
                    paper.category.contains("Junior", ignoreCase = true) ||
                            paper.grade in listOf("Grade 6", "Grade 7", "Grade 8", "Grade 9")
                }
                selectedCategory.contains("O/L") -> {
                    paper.category.contains("O/L", ignoreCase = true) ||
                            paper.grade in listOf("Grade 10", "Grade 11")
                }
                selectedCategory.contains("A/L") -> {
                    paper.category.contains("A/L", ignoreCase = true) ||
                            paper.grade in listOf("Grade 12", "Grade 13")
                }
                else -> true
            }

            // Grade check
            val matchesGrade = (selectedGrade == "All Grades") ||
                    paper.grade.equals(selectedGrade, ignoreCase = true) ||
                    paper.grade.contains(selectedGrade, ignoreCase = true)

            // Stream check (only applies if A/L)
            val matchesStream = if (!isAlActive || selectedStream == "All Streams") true else {
                paper.stream?.equals(selectedStream, ignoreCase = true) == true ||
                        paper.stream.isNullOrBlank() // Allow papers without stream if user searches broadly
            }

            // Subject check
            val matchesSubject = (selectedSubject == "All Subjects") ||
                    paper.subject.equals(selectedSubject, ignoreCase = true) ||
                    paper.subject.contains(selectedSubject, ignoreCase = true)

            // Year check
            val matchesYear = (selectedYear == "All Years") ||
                    paper.year.toString() == selectedYear

            // Search query matches subject, year, title, grade, stream, or description
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                paper.title.contains(q, ignoreCase = true) ||
                        paper.subject.contains(q, ignoreCase = true) ||
                        paper.grade.contains(q, ignoreCase = true) ||
                        (paper.stream?.contains(q, ignoreCase = true) == true) ||
                        paper.category.contains(q, ignoreCase = true) ||
                        paper.year.toString().contains(q) ||
                        paper.medium.contains(q, ignoreCase = true) ||
                        paper.description.contains(q, ignoreCase = true)
            }

            matchesCategory && matchesGrade && matchesStream && matchesSubject && matchesYear && matchesSearch
        }
    }

    // Group filtered papers by Year (Subject -> Year -> Paper)
    val papersByYear = remember(filteredPapers) {
        filteredPapers.groupBy { it.year }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("papers_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Header with Title & Backend Indicator
        // ==========================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Past Papers",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Find examination papers, marking schemes and revision materials.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Backend Status Pill & Server Settings Button
                    Surface(
                        onClick = { showServerConfigDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isLiveDatabase) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("backend_status_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isLiveDatabase) EmeraldPrimary else Color(0xFFEAB308),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isLiveDatabase) "MySQL Connected" else "InfinityFree API",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiveDatabase) OnEmeraldContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Server Settings",
                                modifier = Modifier.size(12.dp),
                                tint = if (isLiveDatabase) OnEmeraldContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. Search Bar
        // ==========================================
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("papers_search_bar"),
                placeholder = { Text("Search by subject, year (e.g. 2024), grade, stream...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )
        }

        // ==========================================
        // 3. Category Selector (Junior, O/L, A/L)
        // ==========================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Education Level & Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SriLankanCurriculumConfig.educationCategories.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = category
                                selectedGrade = "All Grades"
                                selectedStream = "All Streams"
                                selectedSubject = "All Subjects"
                            },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("cat_chip_${category.take(8)}")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 4. Grade Selection (Grade 6 to Grade 13)
        // ==========================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grade Selection (Grade 6 – 13)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dropdown menu button for fast selection
                    Box {
                        TextButton(
                            onClick = { isGradeDropdownExpanded = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedGrade == "All Grades") "Select Grade ▾" else "$selectedGrade ▾",
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = isGradeDropdownExpanded,
                            onDismissRequest = { isGradeDropdownExpanded = false }
                        ) {
                            gradeList.forEach { gradeOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = gradeOption,
                                            fontWeight = if (selectedGrade == gradeOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedGrade == gradeOption) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSelectGrade(gradeOption)
                                        isGradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Interactive horizontal grade chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gradeList.forEach { grade ->
                        val isSelected = selectedGrade == grade
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectGrade(grade) },
                            label = {
                                Text(
                                    text = grade,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealSecondary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("grade_chip_${grade.replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. A/L Stream Selection (Grade 12 or 13)
        // ==========================================
        if (isAlActive) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TealContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = OnTealContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "A/L Stream Selection",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnTealContainer
                                )
                            }

                            // Dropdown option for stream
                            Box {
                                TextButton(
                                    onClick = { isStreamDropdownExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (selectedStream == "All Streams") "Stream Menu ▾" else "$selectedStream ▾",
                                        fontSize = 11.sp,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = isStreamDropdownExpanded,
                                    onDismissRequest = { isStreamDropdownExpanded = false }
                                ) {
                                    SriLankanCurriculumConfig.alStreams.forEach { streamOption ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = streamOption,
                                                    fontWeight = if (selectedStream == streamOption) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedStream == streamOption) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                selectedStream = streamOption
                                                isStreamDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Stream chips row: Physical Science, Biological Science, Commerce, Arts, Technology
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SriLankanCurriculumConfig.alStreams.forEach { stream ->
                                val isSelected = selectedStream == stream
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStream = stream },
                                    label = {
                                        Text(
                                            text = stream,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("stream_chip_${stream.replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. Subject Selection (Configurable per grade & stream)
        // ==========================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subjectHeading = when {
                        isAlActive && selectedStream != "All Streams" -> "Subjects ($selectedStream)"
                        isAlActive -> "A/L Subjects"
                        isOlActive -> "O/L Subjects"
                        isJuniorActive -> "Junior Secondary Subjects ($selectedGrade)"
                        else -> "Available Subjects"
                    }

                    Text(
                        text = subjectHeading,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Dropdown menu button for subjects
                        Box {
                            TextButton(
                                onClick = { isSubjectDropdownExpanded = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedSubject == "All Subjects") "All Subjects ▾" else "$selectedSubject ▾",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = isSubjectDropdownExpanded,
                                onDismissRequest = { isSubjectDropdownExpanded = false }
                            ) {
                                availableSubjects.forEach { subjOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = subjOption,
                                                fontWeight = if (selectedSubject == subjOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedSubject == subjOption) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            selectedSubject = subjOption
                                            isSubjectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedSubject != "All Subjects") {
                            TextButton(
                                onClick = { selectedSubject = "All Subjects" },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Horizontal subject chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSubjects.forEach { subject ->
                        val isSelected = selectedSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubject = subject },
                            label = {
                                Text(
                                    text = subject,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealSecondary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("subject_chip_${subject.take(10).replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 7. Active Navigation Breadcrumb Card
        // ==========================================
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = buildString {
                                append("Papers")
                                if (selectedGrade != "All Grades") append(" › $selectedGrade")
                                if (isAlActive && selectedStream != "All Streams") append(" › $selectedStream")
                                if (selectedSubject != "All Subjects") append(" › $selectedSubject")
                                if (selectedYear != "All Years") append(" › $selectedYear")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedGrade != "All Grades" || selectedStream != "All Streams" || selectedSubject != "All Subjects" || selectedYear != "All Years" || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    searchQuery = ""
                                    selectedCategory = "All Categories"
                                    selectedGrade = "All Grades"
                                    selectedStream = "All Streams"
                                    selectedSubject = "All Subjects"
                                    selectedYear = "All Years"
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 8. Examination Year Selector
        // ==========================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Examination Year",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${filteredPapers.size} papers found",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableYears.forEach { yr ->
                        val isSelected = selectedYear == yr
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYear = yr },
                            label = { Text(yr, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 9. Results Header & Refresh from MySQL
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filteredPapers.isNotEmpty()) {
                        "${filteredPapers.size} Examination Papers Available"
                    } else {
                        "Examination Papers"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onRefreshPapers,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh from MySQL",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ==========================================
        // 10. Paper List grouped by Year (Subject -> Year -> Paper)
        // ==========================================
        if (isLoading) {
            items(4) {
                PaperTileSkeleton()
            }
        } else if (filteredPapers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No papers found matching criteria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try switching category, stream, clearing search, or syncing with MySQL database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All Categories"
                                selectedGrade = "All Grades"
                                selectedStream = "All Streams"
                                selectedSubject = "All Subjects"
                                selectedYear = "All Years"
                                onRefreshPapers()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Show All Papers", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Grouped by Year: 2024 -> Papers, 2023 -> Papers, 2022 -> Papers
            papersByYear.forEach { (year, papersInYear) ->
                item(key = "year_header_$year") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldPrimary
                                ) {
                                    Text(
                                        text = "$year",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$year Examination Papers",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${papersInYear.size} papers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(papersInYear, key = { it.id }) { paper ->
                    val savedEntity = savedPapers.find { it.id == paper.id }
                    val isSaved = savedEntity != null
                    val isDownloaded = savedEntity?.isDownloaded == true

                    PastPaperCategorizedCard(
                        paper = paper,
                        isSaved = isSaved,
                        isDownloaded = isDownloaded,
                        onClick = { onPaperClick(paper) },
                        onToggleSave = { onToggleSavePaper(paper) },
                        onDownload = { onDownloadPaper(paper) }
                    )
                }
            }
        }
    }

    // Server Configuration Dialog for InfinityFree MySQL Backend
    if (showServerConfigDialog) {
        ServerConfigModal(
            currentUrl = serverUrl,
            onDismiss = { showServerConfigDialog = false },
            onSave = { newUrl ->
                onUpdateServerUrl(newUrl)
                showServerConfigDialog = false
            },
            onSyncNow = {
                onRefreshPapers()
                showServerConfigDialog = false
            }
        )
    }
}

/**
 * Modern Paper Card containing:
 * - Year
 * - Examination type
 * - Subject
 * - Grade & Stream
 * - Medium
 * - PDF icon
 * - View button
 * - Download button
 */
@Composable
fun PastPaperCategorizedCard(
    paper: PastPaper,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("paper_card_${paper.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Year, Medium, Grade Badge, Stream Badge & Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Year chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldContainer
                    ) {
                        Text(
                            text = "${paper.year}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnEmeraldContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Medium chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TealContainer
                    ) {
                        Text(
                            text = paper.medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnTealContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Grade / Stream badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        val badgeText = if (!paper.stream.isNullOrBlank()) {
                            "${paper.grade} • ${paper.stream}"
                        } else {
                            "${paper.category} • ${paper.grade}"
                        }
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isSaved) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Paper Title & PDF Icon indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // PDF Icon badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2), // Light Red for PDF
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Document",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PDF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(paper.examType)
                            append(" • ")
                            append(paper.subject)
                            if (!paper.stream.isNullOrBlank()) {
                                append(" (${paper.stream})")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Description / Syllabus Summary
            if (paper.description.isNotBlank() || paper.syllabusSummary.isNotBlank()) {
                Text(
                    text = paper.description.ifBlank { paper.syllabusSummary },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Marking Scheme & Pages Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${paper.pages} Pages (${paper.fileSize})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (paper.hasMarkingScheme) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Marking Scheme Included",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Action Buttons: View Button & Download Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Paper",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onDownload,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) EmeraldDark else EmeraldLight,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDownloaded) "Downloaded" else "Download PDF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Server Configuration Modal
 * Allows user to view/modify the InfinityFree MySQL API Base URL and sync papers live.
 */
@Composable
fun ServerConfigModal(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onSyncNow: () -> Unit
) {
    var editedUrl by remember { mutableStateOf(currentUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = EmeraldPrimary)
                        Text("MySQL Backend Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Configure your InfinityFree PHP API endpoint. The app connects to your live MySQL database to retrieve papers dynamically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = editedUrl,
                    onValueChange = { editedUrl = it },
                    label = { Text("InfinityFree API Base URL") },
                    placeholder = { Text("https://yourdomain.infinityfreeapp.com/") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("API Endpoint:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("${editedUrl.trimEnd('/')}/api/get_papers.php", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncNow,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, EmeraldPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EmeraldPrimary
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }

                    Button(
                        onClick = { onSave(editedUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save & Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
