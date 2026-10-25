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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary

/**
 * Modern Sidebar Drawer displayed when the user taps the hamburger menu icon on Home.
 */
@Composable
fun ModernSidebarDrawer(
    userProfile: UserProfile,
    currentTab: DzdNavTab,
    onSelectTab: (DzdNavTab) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenLanguageLab: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val drawerBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 330.dp)
            .fillMaxHeight()
            .testTag("modern_sidebar_drawer"),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = drawerBg,
        drawerTonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header: User Profile Info & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Modern Circular Avatar
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF0F766E))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userProfile.targetExam,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Close Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCloseDrawer)
                        .testTag("close_drawer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Quick Banner (XP + Streak)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "XP",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "${userProfile.points} XP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total Score",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "${userProfile.streakDays} Days",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Daily Streak",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Items Section
            Text(
                text = "MAIN NAVIGATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            DrawerNavTile(
                title = "Dashboard Home",
                icon = Icons.Filled.Home,
                isSelected = currentTab == DzdNavTab.HOME,
                onClick = { onSelectTab(DzdNavTab.HOME) }
            )

            DrawerNavTile(
                title = "Learn & Courses",
                icon = Icons.Filled.School,
                isSelected = currentTab == DzdNavTab.LEARN,
                onClick = { onSelectTab(DzdNavTab.LEARN) }
            )

            DrawerNavTile(
                title = "AI Voice Tutor",
                icon = Icons.Filled.AutoAwesome,
                isSelected = currentTab == DzdNavTab.AI_TUTOR,
                badge = "Interactive",
                onClick = { onSelectTab(DzdNavTab.AI_TUTOR) }
            )

            DrawerNavTile(
                title = "Past Papers & Exams",
                icon = Icons.Filled.Description,
                isSelected = currentTab == DzdNavTab.PAPERS,
                onClick = { onSelectTab(DzdNavTab.PAPERS) }
            )

            DrawerNavTile(
                title = "My Profile & Settings",
                icon = Icons.Filled.Person,
                isSelected = currentTab == DzdNavTab.PROFILE,
                onClick = { onSelectTab(DzdNavTab.PROFILE) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Special Features Section
            Text(
                text = "AI STUDY TOOLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            DrawerNavTile(
                title = "AI Study Assistant",
                icon = Icons.Filled.Psychology,
                isSelected = false,
                badge = "Multimodal",
                onClick = onOpenAiAssistant
            )

            DrawerNavTile(
                title = "AI Language Lab",
                icon = Icons.Filled.Translate,
                isSelected = false,
                badge = "CEFR B2",
                onClick = onOpenLanguageLab
            )
        }
    }
}

@Composable
private fun DrawerNavTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val activeBg = Color(0xFF22C55E).copy(alpha = 0.12f)
    val activeColor = Color(0xFF22C55E)
    val inactiveColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) activeBg else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, activeColor.copy(alpha = 0.35f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) activeColor else inactiveColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.5.sp,
                    color = if (isSelected) activeColor else inactiveColor
                )
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) activeColor else (if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)),
                    contentColor = if (isSelected) Color.White else EmeraldPrimary
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
