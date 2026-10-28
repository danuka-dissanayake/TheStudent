package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LearningActivityEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SavedPaperEntity
import com.example.data.model.PastPaper
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    savedPapers: List<SavedPaperEntity>,
    savedCourses: List<SavedCourseEntity>,
    recentActivities: List<LearningActivityEntity>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenAuthDialog: () -> Unit,
    onPaperClick: (String) -> Unit,
    /** Opens the academic step (stream, 3 subjects, exam year, medium). */
    onEditAcademic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Saved Papers, 1: Enrolled Courses, 2: History

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    TealDark.copy(alpha = 0.08f),
                                    EmeraldPrimary.copy(alpha = 0.14f),
                                    LimeAccent.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .size(60.dp)
                                    .testTag("profile_avatar")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val avatar = userProfile.avatarUrl
                                    if (avatar.isNotBlank()) {
                                        // Uploaded profile photo (or the Google
                                        // avatar when no upload happened yet).
                                        coil.compose.AsyncImage(
                                            model = avatar,
                                            contentDescription = "Profile photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = userProfile.name.split(" ").map { it.take(1) }.joinToString(""),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldContainer,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = userProfile.institution.ifBlank { "Add your school" },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = OnEmeraldContainer
                                    )
                                }
                            }

                            IconButton(onClick = onOpenAuthDialog) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = "Account Settings",
                                    tint = EmeraldPrimary
                                )
                            }
                        }

                        // Academic credentials + the structured subjects the
                        // matching algorithm reads. Empty states are prompts,
                        // never invented values.
                        val academic = userProfile.academic
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (userProfile.targetExam.isBlank()) {
                                            "Curriculum not set"
                                        } else {
                                            "Target Exam: ${userProfile.targetExam}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = when {
                                            academic.summary().isNotBlank() -> academic.summary()
                                            userProfile.streamOrDegree.isNotBlank() ->
                                                userProfile.streamOrDegree
                                            else -> "Stream not set"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (academic.subjects.isEmpty()) {
                                            "Add your 3 subjects so we can match you with the right help."
                                        } else {
                                            academic.subjectLabels().joinToString(" · ")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (academic.subjects.isEmpty()) {
                                            EmeraldPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = onEditAcademic,
                                    modifier = Modifier.testTag("profile_edit_academic")
                                ) {
                                    Text(
                                        text = if (academic.isComplete) "Edit" else "Add",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        // Stats counters row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileStatItem(title = "Study Time", value = "${userProfile.studyHoursThisWeek}h")
                            ProfileStatItem(title = "Papers Solved", value = "${userProfile.totalPapersSolved}")
                            ProfileStatItem(title = "Daily Streak", value = "${userProfile.streakDays}d 🔥")
                            ProfileStatItem(title = "Total XP", value = "${userProfile.points}")
                        }
                    }
                }
            }
        }

        // Settings Strip (Theme toggle & Account)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleTheme)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAuthDialog)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Text(
                                text = "Account: ${userProfile.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Switch / Login",
                            color = EmeraldPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sub-tabs (Saved Papers, Enrolled Courses, Learning History)
        item {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Saved Papers (${savedPapers.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Courses (${savedCourses.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History", fontSize = 12.sp) }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                if (savedPapers.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.BookmarkBorder,
                            message = "No saved papers yet. Bookmark past papers from the Papers tab!"
                        )
                    }
                } else {
                    items(savedPapers) { paper ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPaperClick(paper.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = OnEmeraldContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = paper.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${paper.examination} • ${paper.medium} • ${paper.fileSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (paper.isDownloaded) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = TealContainer
                                    ) {
                                        Text(
                                            text = "Offline",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnTealContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (savedCourses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.School,
                            message = "No enrolled courses yet. Browse courses in the Learn tab!"
                        )
                    }
                } else {
                    items(savedCourses) { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${course.instructor} • ${course.institution}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { course.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = EmeraldContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${course.completedLessons}/${course.totalLessons} Lessons",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${course.progressPercent}% Completed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (recentActivities.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.History,
                            message = "No learning activities recorded yet."
                        )
                    }
                } else {
                    items(recentActivities) { activity ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (activity.category) {
                                        "Past Paper" -> EmeraldContainer
                                        "Course" -> TealContainer
                                        "AI Tutor" -> LimeContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (activity.category) {
                                                "Past Paper" -> Icons.Default.Description
                                                "Course" -> Icons.Default.School
                                                "AI Tutor" -> Icons.Default.AutoAwesome
                                                else -> Icons.Default.Translate
                                            },
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = activity.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = activity.scoreOrDuration,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
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
fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
fun EmptyStateCard(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
