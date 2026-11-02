package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable

/**
 * One app-wide entry point to TheStudent AI.
 *
 * It is an ordinary Compose FAB placed in MainActivity's root content Box, so it
 * sits above the tab content and survives tab changes without being recreated.
 * Whether it is shown is decided by the caller (chats, full-screen pages, a live
 * tutor call and the assistant screen itself all hide it).
 */
@Composable
fun AssistantFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = Color(0xFF059669),
            contentColor = Color.White,
            modifier = Modifier.testTag("assistant_fab")
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Open TheStudent AI assistant"
            )
        }
    }
}
