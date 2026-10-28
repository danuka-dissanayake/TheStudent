package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * The top bar.
 *
 * Left: the hamburger that opens the sidebar, then the real TheStudent wordmark
 * (the same artwork as the store listing), so the bar is the brand rather than a
 * page title. Right: the streak capsule and the avatar, which opens the profile.
 * Search and the language lab live in the Papers header and the sidebar — the
 * bar keeps only what a student uses on every screen.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun TheStudentTopAppBar(
    currentTab: TheStudentNavTab,
    streakDays: Int,
    onNavigationClick: () -> Unit,
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
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .height(48.dp),
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

            Spacer(Modifier.width(12.dp))

            // ── wordmark ────────────────────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.thestudent_wordmark),
                contentDescription = "TheStudent",
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .testTag("top_bar_wordmark")
            )

            Spacer(Modifier.width(8.dp))

            // ── streak ──────────────────────────────────────────────────────
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
                        modifier = Modifier.padding(horizontal = 10.dp),
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
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) ink else Color(0xFFB45309),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            // ── avatar ──────────────────────────────────────────────────────
            Surface(
                shape = CircleShape,
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(38.dp)
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
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
    }
}
