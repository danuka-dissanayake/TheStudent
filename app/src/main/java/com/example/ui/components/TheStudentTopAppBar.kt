package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * The top bar.
 *
 * Left: the hamburger that opens the sidebar, then the brand lockup — a small
 * emerald mark, the app name, and the current destination underneath it — so the
 * bar says where you are without spending a whole row on a page title.
 *
 * Right: the streak capsule, the language-lab shortcut, search, and the avatar.
 * Every control is a 36dp rounded square so the row never looks crowded, and the
 * brand column is the only thing that flexes when the screen is narrow.
 */
@Composable
fun TheStudentTopAppBar(
    currentTab: TheStudentNavTab,
    streakDays: Int,
    onNavigationClick: () -> Unit,
    onLanguageLabClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    userName: String = "",
    avatarUrl: String = "",
    onProfileClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val barBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val hairline = if (isDark) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    } else {
        Color(0xFFEDF1F6)
    }
    val buttonBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
        Color(0xFFF5F8FC)
    }
    val ink = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("thestudent_top_app_bar"),
        color = barBg,
        border = BorderStroke(1.dp, hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── hamburger ───────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = buttonBg,
                border = BorderStroke(1.dp, hairline),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(onClick = onNavigationClick)
                    .testTag("top_bar_hamburger_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ModernMenuIcon(modifier = Modifier.size(19.dp), tint = ink)
                }
            }

            Spacer(Modifier.width(10.dp))

            // ── brand lockup ────────────────────────────────────────────────
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF10B981), Color(0xFF047857))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TheStudent",
                        fontFamily = MaisonNeueFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "top_bar_subtitle"
                    ) { tab ->
                        Text(
                            text = tab.title,
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) {
                                EmeraldPrimary.copy(alpha = 0.9f)
                            } else {
                                EmeraldPrimary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            // ── actions ─────────────────────────────────────────────────────
            if (streakDays > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AmberWarning.copy(alpha = 0.13f),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("top_bar_streak_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = AmberWarning,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$streakDays",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) ink else Color(0xFFB45309),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            BarIconButton(
                icon = Icons.Default.Translate,
                description = "AI Language Lab",
                tint = EmeraldPrimary,
                background = buttonBg,
                border = hairline,
                tag = "top_bar_language_button",
                onClick = onLanguageLabClick
            )
            Spacer(Modifier.width(6.dp))
            BarIconButton(
                icon = Icons.Default.Search,
                description = "Search",
                tint = ink,
                background = buttonBg,
                border = hairline,
                tag = "top_bar_search_button",
                onClick = onSearchClick
            )
            Spacer(Modifier.width(6.dp))

            // ── avatar ──────────────────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick)
                    .testTag("top_bar_avatar")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initials = userName
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.take(1) }
                        .uppercase()
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = initials.ifBlank { "S" },
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    background: Color,
    border: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
