package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AssistantChatItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * Native chat-history drawer for the embedded assistant.
 *
 * `?app=android` hides the website's sidebar, so without this the student has
 * no way back to an older conversation. The list is supplied by the website
 * through the origin-pinned `CHAT_HISTORY` bridge event; nothing here reads a
 * token or calls an API directly.
 */
@Composable
fun AssistantHistoryDrawer(
    visible: Boolean,
    chats: List<AssistantChatItem>,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onRefresh: () -> Unit,
    onChatSelected: (AssistantChatItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val panelBg = if (isDarkTheme) Color(0xFF141F2D) else Color.White
    val rowBg = if (isDarkTheme) Color(0xFF1E2D3F) else Color(0xFFF5F8FC)
    val hairline = if (isDarkTheme) Color(0xFF334155) else Color(0xFFEDF1F6)
    val ink = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val muted = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss)
                    .testTag("assistant_history_scrim")
            )
        }

        // Right-hand panel (the hamburger lives in the right corner).
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .testTag("assistant_history_drawer"),
                color = panelBg,
                shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Chat history",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ink
                            )
                            Text(
                                "Your TheStudent AI conversations",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 11.sp,
                                color = muted
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh history", tint = ink)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close history", tint = ink)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(hairline)
                    )

                    // Always-available new chat action.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.14f))
                            .clickable(onClick = onNewChat)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("assistant_history_new_chat"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "New chat",
                            fontFamily = MaisonNeueFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            isLoading && chats.isEmpty() -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UiverseLoaderVisualizer(size = 84.dp)
                                    Text(
                                        "Loading your chats…",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 12.sp,
                                        color = muted
                                    )
                                }
                            }

                            chats.isEmpty() -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "No conversations yet",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ink
                                    )
                                    Text(
                                        "Start a new chat, or pull to refresh your history.",
                                        fontFamily = MaisonNeueFontFamily,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = muted
                                    )
                                }
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(chats, key = { it.id }) { chat ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(rowBg)
                                                .clickable { onChatSelected(chat) }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldPrimary)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    chat.title,
                                                    fontFamily = MaisonNeueFontFamily,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ink,
                                                    maxLines = 1
                                                )
                                                if (chat.updatedAt != null) {
                                                    Text(
                                                        chat.updatedAt.orEmpty(),
                                                        fontFamily = MaisonNeueFontFamily,
                                                        fontSize = 11.sp,
                                                        color = muted,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Text("›", fontSize = 20.sp, color = muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
