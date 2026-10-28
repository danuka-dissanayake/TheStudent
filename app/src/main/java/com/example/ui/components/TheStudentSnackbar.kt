package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MaisonNeueFontFamily

/**
 * One message style for the whole app.
 *
 * The stock Material snackbar is a full-width dark rectangle with square
 * corners — it reads like a system alert sitting on top of the design. This is
 * the same information as a slim floating pill: a rounded surface, a tinted
 * icon disc that says at a glance whether it worked or not, and the sentence
 * itself. Nothing else competes with it.
 */
@Composable
fun TheStudentSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        TheStudentToast(message = data.visuals.message)
    }
}

/** Emerald check when something worked, amber mark when something did not. */
private fun isProblemMessage(message: String): Boolean {
    val text = message.lowercase()
    return PROBLEM_WORDS.any { text.contains(it) }
}

private val PROBLEM_WORDS = listOf(
    "offline",
    "error",
    "failed",
    "couldn't",
    "could not",
    "cannot",
    "can't",
    "unable",
    "no internet",
    "not sent",
    "expired",
    "try again"
)

@Composable
private fun TheStudentToast(message: String) {
    val isDark = isSystemInDarkTheme()
    val problem = isProblemMessage(message)

    val pillColor = if (isDark) Color(0xFF16212E) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accent = if (problem) Color(0xFFF59E0B) else Color(0xFF10B981)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = pillColor,
            contentColor = textColor,
            shadowElevation = 12.dp,
            modifier = Modifier
                // Wide enough for a sentence, never a full-width bar.
                .widthIn(max = 460.dp)
                .testTag("app_snackbar")
        ) {
            Row(
                modifier = Modifier.padding(start = 7.dp, end = 17.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (problem) Icons.Default.PriorityHigh else Icons.Default.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = message,
                    fontFamily = MaisonNeueFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    color = textColor,
                    maxLines = 3,
                    modifier = Modifier.padding(start = 11.dp)
                )
            }
        }
    }
}
