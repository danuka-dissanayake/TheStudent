package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.QuizProgress
import com.example.data.model.StudyMetrics
import com.example.data.model.UserProfile
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

private val SInk = Color(0xFF0F172A)
private val SMuted = Color(0xFF64748B)
private val SFaint = Color(0xFF94A3B8)
private val SHairline = Color(0xFFE8EDF3)
private val SDeep = Color(0xFF06281F)
private val SGreenSoft = Color(0xFFECFDF5)
private val SDanger = Color(0xFFDC2626)
private val SDangerSoft = Color(0xFFFEF2F2)

/**
 * The sidebar.
 *
 * Rebuilt around the student rather than around a list of links: a deep-emerald
 * identity header (avatar, name, stage, streak / XP / accuracy), a floating
 * "This week" strip that overlaps its bottom edge, then grouped navigation where
 * every row carries its own pastel icon chip and the active destination is
 * marked by a bar on the leading edge. Account and sign-out sit in a footer that
 * is visually separate from navigation.
 */
@Composable
fun ModernSidebarDrawer(
    userProfile: UserProfile,
    currentTab: TheStudentNavTab,
    onSelectTab: (TheStudentNavTab) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenLanguageLab: () -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenAdminPanel: (() -> Unit)? = null,
    onOpenSecurity: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier,
    quizProgress: QuizProgress = QuizProgress(),
    onOpenCourseCollection: (() -> Unit)? = null,
    onOpenQuizzes: (() -> Unit)? = null,
    onOpenProgress: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val sheetBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val weekly = remember(quizProgress.recent) {
        StudyMetrics.attemptsByWeekday(quizProgress.recent.map { it.submittedAt })
    }

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 324.dp)
            .fillMaxHeight()
            .testTag("modern_sidebar_drawer"),
        drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
        drawerContainerColor = sheetBg,
        drawerTonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SidebarHeader(
                userProfile = userProfile,
                accuracy = quizProgress.summary.averagePercent,
                onCloseDrawer = onCloseDrawer
            )

            // The strip is offset upwards so it straddles the header edge. The
            // visual shift does not change the layout slot, so the section below
            // compensates with a smaller top padding.
            WeeklyStrip(
                weekly = weekly,
                attempts = quizProgress.summary.attempts,
                onOpenProgress = onOpenProgress,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-26).dp)
            )

            DrawerSectionLabel("MENU", topPadding = 0.dp)

            DrawerNavTile(
                title = "Dashboard",
                icon = Icons.Filled.Home,
                accent = EmeraldPrimary,
                isSelected = currentTab == TheStudentNavTab.HOME,
                onClick = { onSelectTab(TheStudentNavTab.HOME) }
            )
            DrawerNavTile(
                title = "Student chats",
                icon = Icons.Filled.Chat,
                accent = Color(0xFF2563EB),
                isSelected = currentTab == TheStudentNavTab.CHAT,
                onClick = { onSelectTab(TheStudentNavTab.CHAT) }
            )
            DrawerNavTile(
                title = "AI Voice Tutor",
                icon = Icons.Filled.AutoAwesome,
                accent = Color(0xFF7C3AED),
                isSelected = currentTab == TheStudentNavTab.AI_TUTOR,
                badge = "AI",
                onClick = { onSelectTab(TheStudentNavTab.AI_TUTOR) }
            )
            DrawerNavTile(
                title = "Past papers",
                icon = Icons.Filled.Description,
                accent = Color(0xFFB45309),
                isSelected = currentTab == TheStudentNavTab.PAPERS,
                onClick = { onSelectTab(TheStudentNavTab.PAPERS) }
            )
            DrawerNavTile(
                title = "My profile",
                icon = Icons.Filled.Person,
                accent = Color(0xFF475569),
                isSelected = currentTab == TheStudentNavTab.PROFILE,
                onClick = { onSelectTab(TheStudentNavTab.PROFILE) }
            )

            DrawerSectionLabel("STUDY")

            if (onOpenQuizzes != null) {
                DrawerNavTile(
                    title = "Quizzes",
                    icon = Icons.Filled.Quiz,
                    accent = Color(0xFF0D9488),
                    isSelected = false,
                    onClick = onOpenQuizzes
                )
            }
            if (onOpenProgress != null) {
                DrawerNavTile(
                    title = "My progress",
                    icon = Icons.Filled.Insights,
                    accent = EmeraldPrimary,
                    isSelected = false,
                    onClick = onOpenProgress
                )
            }
            if (onOpenCourseCollection != null) {
                DrawerNavTile(
                    title = "Free courses",
                    icon = Icons.Filled.School,
                    accent = Color(0xFF2563EB),
                    isSelected = false,
                    onClick = onOpenCourseCollection
                )
            }
            DrawerNavTile(
                title = "AI Study Assistant",
                icon = Icons.Filled.Psychology,
                accent = Color(0xFF7C3AED),
                isSelected = false,
                badge = "Multimodal",
                onClick = onOpenAiAssistant
            )
            DrawerNavTile(
                title = "Language lab",
                icon = Icons.Filled.Translate,
                accent = Color(0xFF0D9488),
                isSelected = false,
                badge = "CEFR B2",
                onClick = onOpenLanguageLab
            )

            if (isAdmin && onOpenAdminPanel != null) {
                DrawerSectionLabel("ADMINISTRATOR", accent = MaterialTheme.colorScheme.error)
                DrawerNavTile(
                    title = "Admin panel",
                    icon = Icons.Filled.AdminPanelSettings,
                    accent = Color(0xFFDC2626),
                    isSelected = false,
                    badge = "Admin",
                    onClick = onOpenAdminPanel
                )
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(if (isDark) Color(0x22FFFFFF) else SHairline)
            )
            Spacer(Modifier.height(10.dp))

            if (onOpenSecurity != null) {
                DrawerFooterRow(
                    title = "Account & security",
                    icon = Icons.Filled.Security,
                    onClick = onOpenSecurity
                )
            }
            if (onSignOut != null) {
                DrawerSignOutRow(onClick = onSignOut)
            }

            Text(
                text = "TheStudent · v2.0",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                color = SFaint,
                modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SidebarHeader(
    userProfile: UserProfile,
    accuracy: Int,
    onCloseDrawer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomEnd = 30.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0E7A5F), Color(0xFF0B4A3B), SDeep)
                )
            )
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 44.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCloseDrawer)
                        .testTag("close_drawer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close menu",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Avatar with a white ring, as an identity anchor rather than decoration.
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = userProfile.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = initialsOf(userProfile.name),
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = userProfile.name.ifBlank { "Student" },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val stage = listOf(
                userProfile.academicLevel,
                userProfile.streamOrDegree
            ).filter { it.isNotBlank() }.joinToString(" · ")
            Text(
                text = stage.ifBlank { "Set up your academic details" },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (userProfile.streakDays > 0) {
                    HeaderPill("${userProfile.streakDays} day streak")
                }
                if (userProfile.points > 0) {
                    HeaderPill("${userProfile.points} XP")
                }
                if (accuracy > 0) {
                    HeaderPill("$accuracy% avg")
                }
            }
        }
    }
}

@Composable
private fun HeaderPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  This-week strip (straddles the header edge)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyStrip(
    weekly: IntArray,
    attempts: Int,
    onOpenProgress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val peak = (weekly.maxOrNull() ?: 0).coerceAtLeast(1)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SHairline),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp), clip = false)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "This week",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SInk,
                    modifier = Modifier.weight(1f)
                )
                if (onOpenProgress != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenProgress)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Report",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weekly.forEach { count ->
                    val fraction = if (count == 0) 0.16f else 0.3f + (count.toFloat() / peak) * 0.7f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (count == 0) Color(0xFFE2E8F0) else EmeraldPrimary)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (attempts > 0) {
                    "$attempts attempts this week"
                } else {
                    "No attempts yet — start a quiz"
                },
                fontFamily = MaisonNeueFontFamily,
                fontSize = 10.sp,
                color = SFaint,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rows
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerSectionLabel(
    text: String,
    topPadding: androidx.compose.ui.unit.Dp = 18.dp,
    accent: Color = SFaint
) {
    Text(
        text = text,
        fontFamily = MaisonNeueFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.1.sp,
        color = accent,
        modifier = Modifier.padding(start = 22.dp, top = topPadding, bottom = 8.dp)
    )
}

@Composable
private fun DrawerNavTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            if (isDark) accent.copy(alpha = 0.18f) else SGreenSoft
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading marker: the selected destination is anchored to the edge.
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accent else Color.Transparent)
            )
            Spacer(Modifier.width(9.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = if (isSelected) 0.18f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Text(
                text = title,
                fontFamily = MaisonNeueFontFamily,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isSelected) SInk else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SFaint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DrawerFooterRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SMuted,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(13.dp))
        Text(
            text = title,
            fontFamily = MaisonNeueFontFamily,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = SInk,
            maxLines = 1
        )
    }
}

@Composable
private fun DrawerSignOutRow(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SDangerSoft,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = SDanger,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(11.dp))
            Text(
                text = "Sign out",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = SDanger
            )
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotBlank() }.take(2)
    if (parts.isEmpty()) return "S"
    return parts.joinToString("") { it.take(1) }.uppercase()
}
