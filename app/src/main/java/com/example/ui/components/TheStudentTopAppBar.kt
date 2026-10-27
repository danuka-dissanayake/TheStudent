package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary

/**
 * Modern redesigned Top App Bar matching the sleek aesthetic of the bottom navigation bar:
 * - App logo and app name completely removed as requested.
 * - Home screen features a modern designer hamburger sidebar menu button on the top-left.
 * - Other screens feature a modern directional back arrow button on the top-left to return to Home.
 * - Clean centered/aligned page title with modern typography.
 * - Right actions: Modern streak counter capsule badge, quick AI Language Lab, and search buttons.
 */
@Composable
fun TheStudentTopAppBar(
    currentTab: TheStudentNavTab,
    streakDays: Int,
    onNavigationClick: () -> Unit,
    onLanguageLabClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val navBarBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val borderStrokeColor = if (isDark) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    } else {
        Color(0xFFF1F5F9)
    }
    val buttonBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        Color(0xFFF8FAFC)
    }
    val buttonBorderColor = if (isDark) {
        Color(0x33FFFFFF)
    } else {
        Color(0xFFE2E8F0)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("thestudent_top_app_bar"),
        color = navBarBg,
        border = BorderStroke(1.dp, borderStrokeColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top-Left: Modern Navigation Button (Hamburger on Home, Back Arrow on other screens)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = buttonBg,
                    border = BorderStroke(1.dp, buttonBorderColor),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigationClick)
                        .testTag(if (currentTab == TheStudentNavTab.HOME) "top_bar_hamburger_button" else "top_bar_back_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (currentTab == TheStudentNavTab.HOME) {
                            // Modern designer hamburger icon with staggered bars
                            ModernMenuIcon(
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            // Modern directional back arrow icon
                            ModernBackArrowIcon(
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Modern Screen Title (Crossfaded between destinations)
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "top_bar_title_anim"
                ) { tab ->
                    val titleText = when (tab) {
                        TheStudentNavTab.HOME -> "Dashboard"
                        TheStudentNavTab.CHAT -> "Student Chats"
                        TheStudentNavTab.AI_TUTOR -> "AI Voice Tutor"
                        TheStudentNavTab.PAPERS -> "Papers & Exams"
                        TheStudentNavTab.PROFILE -> "My Profile"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            // 2. Top-Right: Modern Actions (Streak Badge + Language Lab + Search)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak counter modern capsule pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmberWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("streak_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = AmberWarning,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "$streakDays d",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Quick AI Language Lab button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = buttonBg,
                    border = BorderStroke(1.dp, buttonBorderColor),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onLanguageLabClick)
                        .testTag("top_bar_language_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "AI Language Lab",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Modern Search button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = buttonBg,
                    border = BorderStroke(1.dp, buttonBorderColor),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSearchClick)
                        .testTag("top_bar_search_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
