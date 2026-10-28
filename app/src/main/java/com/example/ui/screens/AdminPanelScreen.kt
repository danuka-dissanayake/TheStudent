package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EducationLevel
import com.example.data.model.PastPaper
import com.example.ui.theme.*

enum class AdminTab {
    UPLOAD,
    CATALOG,
    QUIZZES,
    DATABASE_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    papersList: List<PastPaper>,
    serverUrl: String,
    isLiveConnected: Boolean,
    onUploadPaper: (PastPaper) -> Unit,
    onDeletePaper: (String) -> Unit,
    onUpdateServerUrl: (String) -> Unit,
    isTestingConnection: Boolean = false,
    connectionTestResult: Pair<Boolean, String>? = null,
    onTestAndSaveServerUrl: (String) -> Unit = onUpdateServerUrl,
    // Quizzes: authored here, on the phone, and published straight to the server.
    adminQuizzes: List<com.example.data.model.QuizSummary> = emptyList(),
    isAdminQuizzesLoading: Boolean = false,
    onLoadAdminQuizzes: () -> Unit = {},
    onNewQuiz: () -> Unit = {},
    onEditQuiz: (com.example.data.model.QuizSummary) -> Unit = {},
    onQuizStatus: (Int, String) -> Unit = { _, _ -> },
    onDeleteQuiz: (Int) -> Unit = {},
    onResetQuizAttempts: (Int) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(AdminTab.UPLOAD) }

    // Upload Form State (each field on new line, long fields support multiline)
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("2024") }
    var durationText by remember { mutableStateOf("3 Hours") }
    var pagesText by remember { mutableStateOf("16") }
    var medium by remember { mutableStateOf("Sinhala") } // Sinhala, Tamil, English
    var includeMarkingScheme by remember { mutableStateOf(true) }
    
    // File upload states
    var selectedThumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMarkingSchemeUri by remember { mutableStateOf<Uri?>(null) }

    var category by remember { mutableStateOf("A/L") }
    var grade by remember { mutableStateOf("Grade 13") }
    var stream by remember { mutableStateOf("Physical Science") }
    var description by remember { mutableStateOf("") }

    var formError by remember { mutableStateOf<String?>(null) }
    var showSuccessBanner by remember { mutableStateOf(false) }

    // Server Config state
    var tempServerUrl by remember { mutableStateOf(serverUrl) }

    // Activity Result Launchers for file and image selection
    val context = LocalContext.current
    var detectedFileSize by remember { mutableStateOf("3.5 MB") }
    var detectedFileName by remember { mutableStateOf<String?>(null) }
    var detectedSchemeSize by remember { mutableStateOf<String?>(null) }
    var detectedSchemeName by remember { mutableStateOf<String?>(null) }

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedThumbnailUri = uri
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            detectedFileName = cursor.getString(nameIndex)
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            detectedFileSize = if (sizeBytes >= 1048576) {
                                String.format(java.util.Locale.US, "%.2f MB", sizeBytes / 1048576.0)
                            } else {
                                String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1024.0)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                detectedFileSize = "3.5 MB"
            }
        }
    }

    val markingSchemePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedMarkingSchemeUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            detectedSchemeName = cursor.getString(nameIndex)
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            detectedSchemeSize = if (sizeBytes >= 1048576) {
                                String.format(java.util.Locale.US, "%.2f MB", sizeBytes / 1048576.0)
                            } else {
                                String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1024.0)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_panel_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Admin Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isLiveConnected) "DirectAdmin MySQL Connected" else "App Catalog Mode",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldDark
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector Row
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTab == AdminTab.UPLOAD,
                        onClick = { activeTab = AdminTab.UPLOAD },
                        label = { Text("Upload", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureLightGreenButton,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    FilterChip(
                        selected = activeTab == AdminTab.CATALOG,
                        onClick = { activeTab = AdminTab.CATALOG },
                        label = { Text("Papers (${papersList.size})", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureLightGreenButton,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    FilterChip(
                        selected = activeTab == AdminTab.QUIZZES,
                        onClick = { activeTab = AdminTab.QUIZZES },
                        label = { Text("Quizzes", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureLightGreenButton,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    FilterChip(
                        selected = activeTab == AdminTab.DATABASE_SETTINGS,
                        onClick = { activeTab = AdminTab.DATABASE_SETTINGS },
                        label = { Text("Server", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureLightGreenButton,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            when (activeTab) {
                AdminTab.UPLOAD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(PureLightGreenButton.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PostAdd, contentDescription = null, tint = PureLightGreenButton, modifier = Modifier.size(24.dp))
                                    }
                                    Column {
                                        Text("Upload New Past Paper", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Fill in paper details and upload files directly", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                if (showSuccessBanner) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PureLightGreenButton.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PureLightGreenButton)
                                            Text(
                                                "Paper uploaded successfully! It is now live in the app.",
                                                color = EmeraldDark,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                if (formError != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = RedError.copy(alpha = 0.1f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Error, contentDescription = null, tint = RedError, modifier = Modifier.size(18.dp))
                                            Text(formError!!, color = RedError, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                // 1. Sri Lankan Category Selector Chips
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Examination Category *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("A/L", "O/L", "Scholarship", "Junior").forEach { cat ->
                                            val isSelected = category.equals(cat, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    category = cat
                                                    grade = when (cat) {
                                                        "A/L" -> "Grade 13"
                                                        "O/L" -> "Grade 11"
                                                        "Scholarship" -> "Grade 5"
                                                        else -> "Grade 9"
                                                    }
                                                },
                                                label = { Text(if (cat == "A/L") "A/L (උසස් පෙළ)" else if (cat == "O/L") "O/L (සා/පෙළ)" else if (cat == "Scholarship") "Grade 5" else "Grades 6-9", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PureLightGreenButton,
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // 2. A/L Stream Selector (Visible when A/L is active)
                                if (category.contains("A/L", ignoreCase = true)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("A/L Stream (විෂය ධාරාව)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val streams = listOf("Physical Science", "Biological Science", "Commerce", "Technology", "Arts", "Common")
                                            items(streams) { s ->
                                                FilterChip(
                                                    selected = stream == s,
                                                    onClick = { stream = s },
                                                    label = { Text(s, fontSize = 12.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PureLightGreenButton,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Quick Sri Lankan Subject Presets
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Quick Select Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val quickSubjects = when {
                                        category.contains("A/L") && stream.contains("Phys") -> listOf("Combined Mathematics", "Physics", "Chemistry", "ICT")
                                        category.contains("A/L") && stream.contains("Bio") -> listOf("Biology", "Chemistry", "Physics", "Agricultural Science")
                                        category.contains("A/L") && stream.contains("Comm") -> listOf("Accounting", "Business Studies", "Economics", "ICT")
                                        category.contains("A/L") && stream.contains("Tech") -> listOf("Science for Technology", "Engineering Tech", "Bio Systems Tech", "ICT")
                                        category.contains("A/L") && stream.contains("Art") -> listOf("Sinhala", "English", "Logic", "Political Science", "History")
                                        category.contains("O/L") -> listOf("Mathematics", "Science", "English", "Sinhala", "History", "Buddhism", "Commerce", "ICT")
                                        category.contains("Scholar") -> listOf("Scholarship Paper I", "Scholarship Paper II")
                                        else -> listOf("Mathematics", "Science", "English", "Sinhala", "History")
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(quickSubjects) { subj ->
                                            SuggestionChip(
                                                onClick = {
                                                    subject = subj
                                                    title = "$yearText G.C.E. $category $subj ($medium Medium)"
                                                    formError = null
                                                },
                                                label = { Text(subj, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (subject == subj) PureLightGreenButton.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                                    labelColor = if (subject == subj) EmeraldDark else Color(0xFF334155)
                                                )
                                            )
                                        }
                                    }
                                }

                                // 4. Subject Name
                                ModernFormField(
                                    label = "Subject Name *",
                                    value = subject,
                                    placeholder = "e.g., Combined Mathematics / Physics / Chemistry / ICT",
                                    singleLine = false,
                                    minLines = 1,
                                    onValueChange = { subject = it; formError = null }
                                )

                                // 5. Paper Title with Auto-Generate Helper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Paper Title *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(
                                        onClick = {
                                            if (subject.isNotBlank()) {
                                                title = "$yearText G.C.E. $category $subject ($medium Medium)"
                                            } else {
                                                title = "$yearText G.C.E. $category Examination Paper ($medium Medium)"
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldDark)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Auto Title", fontSize = 11.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                    }
                                }

                                ModernFormField(
                                    label = "",
                                    value = title,
                                    placeholder = "e.g., 2024 G.C.E. A/L Combined Mathematics Paper I & II (Sinhala Medium)",
                                    singleLine = false,
                                    minLines = 2,
                                    onValueChange = { title = it; formError = null }
                                )

                                // 6. Year, Duration, Pages Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernFormField(
                                            label = "Year *",
                                            value = yearText,
                                            placeholder = "2024",
                                            keyboardType = KeyboardType.Number,
                                            onValueChange = { yearText = it }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernFormField(
                                            label = "Duration",
                                            value = durationText,
                                            placeholder = "3 Hours",
                                            onValueChange = { durationText = it }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernFormField(
                                            label = "Pages",
                                            value = pagesText,
                                            placeholder = "16",
                                            keyboardType = KeyboardType.Number,
                                            onValueChange = { pagesText = it }
                                        )
                                    }
                                }

                                // 7. Medium Selector
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Medium Language *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Sinhala", "Tamil", "English").forEach { med ->
                                            val isSelected = medium.equals(med, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { medium = med },
                                                label = { Text(med, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PureLightGreenButton,
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // 10. Thumbnail Image Upload Card
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Thumbnail Image", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                thumbnailPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (selectedThumbnailUri != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.LightGray)
                                                ) {
                                                    AsyncImage(
                                                        model = selectedThumbnailUri,
                                                        contentDescription = "Thumbnail preview",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Thumbnail Selected", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldDark)
                                                    Text(selectedThumbnailUri?.lastPathSegment ?: "Image file", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                                }
                                                IconButton(onClick = { selectedThumbnailUri = null }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(PureLightGreenButton.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PureLightGreenButton)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Upload Thumbnail Image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Tap to browse gallery (JPG, PNG)", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    }
                                }

                                // 11. Paper PDF File Upload Card
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Paper PDF Document *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (selectedPdfUri != null) PureLightGreenButton.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (selectedPdfUri != null) Icons.Default.PictureAsPdf else Icons.Default.UploadFile,
                                                    contentDescription = null,
                                                    tint = if (selectedPdfUri != null) PureLightGreenButton else RedError
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (selectedPdfUri != null) "PDF File Attached" else "Upload Paper PDF *",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (selectedPdfUri != null) EmeraldDark else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (selectedPdfUri != null) (detectedFileName ?: (selectedPdfUri?.lastPathSegment ?: "PDF Selected")) else "Tap to select past paper PDF file",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1
                                                )
                                                if (selectedPdfUri != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = PureLightGreenButton.copy(alpha = 0.12f),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Real Size: $detectedFileSize (Auto detected)",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = EmeraldDark,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (selectedPdfUri != null) {
                                                IconButton(onClick = { selectedPdfUri = null }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                                                }
                                            } else {
                                                Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    }
                                }

                                // 12. Marking Scheme Checkbox & File Upload
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { includeMarkingScheme = !includeMarkingScheme }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = includeMarkingScheme,
                                        onCheckedChange = { includeMarkingScheme = it },
                                        colors = CheckboxDefaults.colors(checkedColor = PureLightGreenButton)
                                    )
                                    Text(
                                        "Include Official Marking Scheme",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                AnimatedVisibility(visible = includeMarkingScheme) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Marking Scheme PDF Document", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    markingSchemePickerLauncher.launch(arrayOf("application/pdf"))
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(PureLightGreenButton.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = PureLightGreenButton)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (selectedMarkingSchemeUri != null) "Marking Scheme PDF Attached" else "Upload Marking Scheme PDF",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = if (selectedMarkingSchemeUri != null) (detectedSchemeName ?: (selectedMarkingSchemeUri?.lastPathSegment ?: "Scheme Selected")) else "Tap to select answer guide PDF",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray,
                                                        maxLines = 1
                                                    )
                                                    if (selectedMarkingSchemeUri != null && detectedSchemeSize != null) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = PureLightGreenButton.copy(alpha = 0.12f),
                                                            modifier = Modifier.padding(top = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Real Size: $detectedSchemeSize (Auto detected)",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = EmeraldDark,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (selectedMarkingSchemeUri != null) {
                                                    IconButton(onClick = { selectedMarkingSchemeUri = null }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                                                    }
                                                } else {
                                                    Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }

                                // 13. Description & Syllabus Notes (Multiline)
                                ModernFormField(
                                    label = "Description & Syllabus Summary",
                                    value = description,
                                    placeholder = "Provide syllabus structure, topic weightage, or instructions...",
                                    singleLine = false,
                                    minLines = 3,
                                    onValueChange = { description = it }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Submit Upload Button
                                Button(
                                    onClick = {
                                        if (title.isBlank()) {
                                            formError = "Paper Title is required."
                                            return@Button
                                        }
                                        if (subject.isBlank()) {
                                            formError = "Subject Name is required."
                                            return@Button
                                        }
                                        if (selectedPdfUri == null) {
                                            formError = "Please attach the Paper PDF document."
                                            return@Button
                                        }

                                        val parsedYear = yearText.toIntOrNull() ?: 2024
                                        val parsedPages = pagesText.toIntOrNull() ?: 16
                                        val newId = "paper-${System.currentTimeMillis()}"

                                        val newPaper = PastPaper(
                                            id = newId,
                                            title = title.trim(),
                                            examination = if (category.contains("O/L")) "G.C.E. O/L Examination" else "G.C.E. A/L Examination",
                                            subject = subject.trim(),
                                            year = parsedYear,
                                            medium = medium,
                                            level = EducationLevel.SCHOOL,
                                            grade = grade.ifBlank { "Grade 13" },
                                            category = category.ifBlank { "A/L" },
                                            stream = stream.ifBlank { "Physical Science" },
                                            pdfUrl = selectedPdfUri.toString(),
                                            markingSchemeUrl = if (includeMarkingScheme) selectedMarkingSchemeUri?.toString() else null,
                                            thumbnailUrl = selectedThumbnailUri?.toString() ?: "",
                                            description = description.trim(),
                                            pages = parsedPages,
                                            fileSize = detectedFileSize,
                                            hasMarkingScheme = includeMarkingScheme
                                        )

                                        onUploadPaper(newPaper)
                                        showSuccessBanner = true
                                        title = ""
                                        subject = ""
                                        selectedPdfUri = null
                                        selectedThumbnailUri = null
                                        selectedMarkingSchemeUri = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .shadow(4.dp, RoundedCornerShape(27.dp), spotColor = PureLightGreenButton.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(27.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PureLightGreenButton)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Paper to Database", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                AdminTab.QUIZZES -> {
                    LaunchedEffect(activeTab) { onLoadAdminQuizzes() }
                    QuizAdminList(
                        quizzes = adminQuizzes,
                        isLoading = isAdminQuizzesLoading,
                        onNew = onNewQuiz,
                        onEdit = onEditQuiz,
                        onStatus = onQuizStatus,
                        onDelete = onDeleteQuiz,
                        onResetAttempts = onResetQuizAttempts,
                        onRefresh = onLoadAdminQuizzes,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AdminTab.CATALOG -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Live Paper Database (${papersList.size} Papers)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(papersList, key = { it.id }) { paper ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = PureLightGreenButton.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = paper.category,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldDark,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${paper.year} • ${paper.medium}",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = paper.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${paper.subject} | ${paper.grade} ${paper.stream?.let { "($it)" } ?: ""}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { onDeletePaper(paper.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RedError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminTab.DATABASE_SETTINGS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("DirectAdmin REST API Endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Connect this app to your DirectAdmin web server and MySQL database.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                                ModernFormField(
                                    label = "Server Base URL",
                                    value = tempServerUrl,
                                    placeholder = "https://thestudent.lk/api/",
                                    onValueChange = { tempServerUrl = it }
                                )

                                Text(
                                    "Note: Enter the base URL ending with /api/ (e.g. https://thestudent.lk/api/). Do not include specific PHP filenames like get_papers.php.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )

                                Button(
                                    onClick = { onTestAndSaveServerUrl(tempServerUrl) },
                                    enabled = !isTestingConnection,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                                ) {
                                    if (isTestingConnection) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Testing API Connection...")
                                    } else {
                                        Icon(Icons.Default.CloudSync, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save & Connect to DirectAdmin")
                                    }
                                }

                                if (isTestingConnection) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Text(
                                                "Contacting DirectAdmin API at $tempServerUrl...",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                connectionTestResult?.let { (isSuccess, message) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSuccess) PureLightGreenButton.copy(alpha = 0.12f) else RedError.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, if (isSuccess) PureLightGreenButton else RedError),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = null,
                                                tint = if (isSuccess) EmeraldDark else RedError
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isSuccess) "API Connection Verified" else "API Connection Failed",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSuccess) EmeraldDark else RedError
                                                )
                                                Text(
                                                    text = message,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Detailed DirectAdmin Setup Instructions
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Storage, contentDescription = null, tint = PureLightGreenButton)
                                    Text("DirectAdmin Host Setup Steps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "1. Log into your DirectAdmin control panel.\n" +
                                            "2. Go to 'MySQL Management' and create a database (e.g., `thestudent_papers_db`).\n" +
                                            "3. Open phpMyAdmin, import the SQL schema provided by the assistant.\n" +
                                            "4. Upload `upload_paper.php`, `get_papers.php`, and `db_config.php` to your `public_html/api/` folder.\n" +
                                            "5. Enter your website URL above (e.g. `https://yourdomain.com/`).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernFormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.LightGray, fontSize = 13.sp) },
            singleLine = singleLine,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = PureLightGreenButton,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
    }
}
