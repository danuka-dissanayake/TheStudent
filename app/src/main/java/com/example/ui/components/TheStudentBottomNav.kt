package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Navigation tabs matching modern redesign with AI Tutor centered as the hero action.
 */
enum class TheStudentNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    CHAT("Chat", Icons.Filled.Chat, Icons.Outlined.Chat, "nav_tab_chat"),
    AI_TUTOR("AI Tutor", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_ai_tutor"),
    PAPERS("Papers", Icons.Filled.Description, Icons.Outlined.Description, "nav_tab_papers"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
}

/**
 * Modern floating bottom navigation bar with elevated center circular hero button.
 * Inspired by reference design with glowing vibrant gradient, crisp borders, and balanced layout.
 */
@Composable
fun TheStudentBottomNavigationBar(
    currentTab: TheStudentNavTab,
    onTabSelected: (TheStudentNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val navBarBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("thestudent_bottom_nav"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Navigation Bar Base Surface (Smooth rounded top corners with soft shadow)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp) // Leave headroom for the elevated center button
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    ambientColor = if (isDark) Color.Black else Color(0x24000000),
                    spotColor = if (isDark) Color.Black else Color(0x24000000)
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = navBarBg,
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                else Color(0xFFF1F5F9)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomInset)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(58.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Home
                NavTabItem(
                    tab = TheStudentNavTab.HOME,
                    isSelected = currentTab == TheStudentNavTab.HOME,
                    onClick = { onTabSelected(TheStudentNavTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Chat
                NavTabItem(
                    tab = TheStudentNavTab.CHAT,
                    isSelected = currentTab == TheStudentNavTab.CHAT,
                    onClick = { onTabSelected(TheStudentNavTab.CHAT) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 3 Center Gap: Space reserved for elevated AI Tutor button
                Spacer(modifier = Modifier.weight(1.2f))

                // Tab 4: Papers
                NavTabItem(
                    tab = TheStudentNavTab.PAPERS,
                    isSelected = currentTab == TheStudentNavTab.PAPERS,
                    onClick = { onTabSelected(TheStudentNavTab.PAPERS) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 5: Profile
                NavTabItem(
                    tab = TheStudentNavTab.PROFILE,
                    isSelected = currentTab == TheStudentNavTab.PROFILE,
                    onClick = { onTabSelected(TheStudentNavTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Elevated Floating Center AI Tutor Hero Button
        val isAiTutorSelected = currentTab == TheStudentNavTab.AI_TUTOR
        val greenHighlight = Color(0xFF22C55E) // Matching AI Tutor screen mic button
        val buttonScale by animateFloatAsState(
            targetValue = if (isAiTutorSelected) 1.08f else 1.0f,
            animationSpec = spring(dampingRatio = 0.7f),
            label = "ai_center_button_scale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(bottom = bottomInset + 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft atmospheric glowing halo behind the floating circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(buttonScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isAiTutorSelected) greenHighlight.copy(alpha = 0.45f)
                                else Color.Black.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Primary Floating Button with bold black background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(buttonScale)
                    .shadow(
                        elevation = if (isAiTutorSelected) 14.dp else 8.dp,
                        shape = CircleShape,
                        ambientColor = if (isAiTutorSelected) greenHighlight else Color.Black,
                        spotColor = if (isAiTutorSelected) greenHighlight else Color.Black
                    )
                    .border(
                        width = 3.5.dp,
                        color = navBarBg,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF27272A), // Modern obsidian black gradient
                                Color(0xFF18181B),
                                Color(0xFF09090B)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 32.dp),
                        onClick = { onTabSelected(TheStudentNavTab.AI_TUTOR) }
                    )
                    .testTag(TheStudentNavTab.AI_TUTOR.testTag),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI Tutor",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Individual standard tab item with animated icon colors and smooth micro-spring scaling.
 */
@Composable
private fun NavTabItem(
    tab: TheStudentNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF22C55E) // Vivid emerald green matching mic button
    val inactiveColor = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        label = "tab_icon_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        label = "tab_scale"
    )

    Column(
        modifier = modifier
            .testTag(tab.testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.title,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = iconColor,
            maxLines = 1
        )
    }
}
