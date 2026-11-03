package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * The assistant's native header, built from the same design tokens as
 * [TheStudentTopAppBar]: a hairline-bordered surface, status-bar aware padding,
 * a 54 dp row and 40 dp rounded icon buttons with their own border.
 *
 * The hamburger sits in the RIGHT corner (as requested) and opens the chat
 * history drawer — in embed mode the website hides its own sidebar, so the
 * native drawer is the only way back to older conversations.
 */
@Composable
fun AssistantTopBar(
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDarkTheme) Color(0xFF141F2D) else Color.White
    val hairline = if (isDarkTheme) Color(0xFF334155) else Color(0xFFEDF1F6)
    val buttonBg = if (isDarkTheme) Color(0xFF1E2D3F) else Color(0xFFF5F8FC)
    val ink = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val muted = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("assistant_top_bar"),
        color = barBg,
        border = BorderStroke(1.dp, hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── back ────────────────────────────────────────────────────────
            TopBarIconButton(
                onClick = onBack,
                buttonBg = buttonBg,
                hairline = hairline,
                testTag = "assistant_top_bar_back"
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // ── title ───────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TheStudent AI",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink,
                    maxLines = 1
                )
                Text(
                    "Your study assistant",
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 11.sp,
                    color = muted,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            // ── emerald badge ───────────────────────────────────────────────
            Text(
                "AI",
                fontFamily = MaisonNeueFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF059669))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("assistant_top_bar_badge")
            )

            Spacer(Modifier.width(8.dp))

            // ── new chat ────────────────────────────────────────────────────
            TopBarIconButton(
                onClick = onNewChat,
                buttonBg = buttonBg,
                hairline = hairline,
                testTag = "assistant_top_bar_new_chat"
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // ── hamburger: chat history (right corner) ──────────────────────
            TopBarIconButton(
                onClick = onHistoryClick,
                buttonBg = buttonBg,
                hairline = hairline,
                testTag = "assistant_top_bar_history"
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** 40 dp rounded, hairline-bordered icon button — the app's header signature. */
@Composable
private fun TopBarIconButton(
    onClick: () -> Unit,
    buttonBg: Color,
    hairline: Color,
    testTag: String,
    icon: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = buttonBg,
        border = BorderStroke(1.dp, hairline),
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.size(40.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
        }
    }
}
