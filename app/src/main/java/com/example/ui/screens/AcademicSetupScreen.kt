package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicProfile
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MaisonNeueFontFamily

private val AcademicInk = Color(0xFF0B1220)
private val AcademicMuted = Color(0xFF64748B)

/**
 * Change the academic details after setup.
 *
 * The form itself lives in `AcademicFormSections.kt` and is shared with the
 * mandatory setup step, so the two can never disagree about what a stage needs.
 * This screen adds the header, the wash, and the sticky save footer.
 */
@Composable
fun AcademicSetupScreen(
    initial: AcademicProfile,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (AcademicProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    // The draft is owned here, so the form stays a controlled component.
    var draft by remember(initial) { mutableStateOf(initial) }
    val canSave = draft.isComplete
    val todo = academicTodo(draft)

    BackHandler(enabled = !isSaving) { onDismiss() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("academic_setup_screen"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0xFFD7F0E1),
                        0.34f to Color(0xFFE9F7EF),
                        0.72f to Color(0xFFF5FBF8),
                        1.00f to Color(0xFFFDFEFD)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    // ── top bar ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = AcademicInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Academic details",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcademicInk
                            )
                            Text(
                                text = "Used to match you with students who can help",
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.sp,
                                color = AcademicMuted
                            )
                        }
                    }

                    AcademicStageForm(
                        profile = draft,
                        onChange = { draft = it }
                    )
                }

                // ── sticky footer ───────────────────────────────────────────
                Surface(
                    color = Color.White,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else if (!canSave) {
                            Text(
                                text = todo,
                                fontFamily = MaisonNeueFontFamily,
                                fontSize = 12.5.sp,
                                color = AcademicMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                if (canSave && !isSaving) onSave(draft)
                            },
                            enabled = canSave && !isSaving,
                            shape = RoundedCornerShape(50),
                            color = if (canSave && !isSaving) EmeraldPrimary else Color(0xFFCBD5E1),
                            shadowElevation = if (canSave && !isSaving) 8.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("academic_save")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isSaving) "Saving…" else "Save details",
                                    fontFamily = MaisonNeueFontFamily,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
