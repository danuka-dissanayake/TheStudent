package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.remote.EnglishVoiceTutorEngine
import com.example.data.remote.HuggingFaceSpaceClient
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorScreen(
    voiceState: AssistantVoiceState,
    micRmsDb: Float,
    userTranscript: String?,
    aiResponseText: String,
    grammarTip: String?,
    vocabularyHighlight: String?,
    currentScenario: String,
    suggestedPills: List<String>,
    isHandsFree: Boolean,
    isKeyboardOpen: Boolean,
    isHfSettingsOpen: Boolean,
    customHfToken: String,
    customHfBaseUrl: String,
    hfStatusMessage: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleVoice: () -> Unit,
    onSendQuery: (String) -> Unit,
    onReplaySpeech: () -> Unit,
    onStopSpeech: () -> Unit,
    onSelectScenario: (String) -> Unit,
    onToggleHandsFree: () -> Unit,
    onToggleKeyboard: (Boolean) -> Unit,
    onToggleHfSettings: (Boolean) -> Unit,
    onUpdateHfCredentials: (String, String) -> Unit,
    onTestHfConnection: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var keyboardInputText by remember { mutableStateOf("") }
    var showScenarioDialog by remember { mutableStateOf(false) }

    // Permission launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartListening()
        } else {
            onShowSnackbar("Microphone permission is required to speak with the AI assistant.")
        }
    }

    fun handleMicClick() {
        if (voiceState == AssistantVoiceState.LISTENING) {
            onStopListening()
        } else if (voiceState == AssistantVoiceState.SPEAKING) {
            onStopSpeech()
        } else {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasMicPermission) {
                onStartListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_voice_tutor_screen"),
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background geometric arch and starlight art situated directly flush on the bottom nav bar line
                OnboardingModernFooterArt(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Fixed Speak (Replay/Stop) and Copy action buttons directly above the mic icon without disappearing
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .testTag("fixed_tutor_action_controls"),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speaker / Stop / Replay button
                        IconButton(
                            onClick = {
                                if (voiceState == AssistantVoiceState.SPEAKING) {
                                    onStopSpeech()
                                } else {
                                    onReplaySpeech()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (voiceState == AssistantVoiceState.SPEAKING) {
                                ModernPauseIcon(
                                    tint = RedError,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Replay response",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Copy speech text button
                        IconButton(
                            onClick = {
                                val textToCopy = aiResponseText.ifBlank { "AI Tutor Voice Assistant" }
                                val cleanText = cleanMarkdownForClipboard(textToCopy)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Tutor Speech", cleanText))
                                onShowSnackbar("Copied tutor response to clipboard")
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy speech text",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Centralized Animated Voice Input Button Section
                    AnimatedVoiceButton(
                        state = voiceState,
                        isHandsFree = isHandsFree,
                        onMicClick = { handleMicClick() },
                        onKeyboardToggle = { onToggleKeyboard(true) },
                        onHandsFreeToggle = onToggleHandsFree
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Uiverse.io Organic Liquid Loader Visualizer by andrew-manzyk (Enlarged, Zero Background)
            UiverseLoaderVisualizer(
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Minimalist Status Indicator
            MinimalVisualizerStatus(state = voiceState)

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated scrollable container for the AI response text section only
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                if (aiResponseText.isNotBlank() || !userTranscript.isNullOrBlank()) {
                    RevealingSpeechCard(
                        userSpokenText = userTranscript,
                        aiResponseText = aiResponseText,
                        grammarTip = grammarTip,
                        vocabularyHighlight = vocabularyHighlight,
                        isSpeaking = voiceState == AssistantVoiceState.SPEAKING,
                        onReplayAudio = onReplaySpeech,
                        onStopAudio = onStopSpeech,
                        onCopyText = {
                            val cleanText = cleanMarkdownForClipboard(aiResponseText)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Tutor Speech", cleanText))
                            onShowSnackbar("Copied tutor response to clipboard")
                        }
                    )
                } else {
                    // Peaceful minimal idle placeholder lifted up for ideal visual balance
                    Text(
                        text = "Tap the microphone to speak English with your AI Tutor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Scenario Picker Dialog (opens on demand, keeping main screen clutter-free)
    if (showScenarioDialog) {
        AlertDialog(
            onDismissRequest = { showScenarioDialog = false },
            title = {
                Text(
                    text = "Practice Topic",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnglishVoiceTutorEngine.scenarios.forEach { scenario ->
                        val isSelected = scenario == currentScenario
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectScenario(scenario)
                                    showScenarioDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (scenario) {
                                        "Coffee Shop & Cafe" -> Icons.Default.LocalCafe
                                        "Job Interview" -> Icons.Default.Work
                                        "Travel & Airport" -> Icons.Default.Flight
                                        "Grammar Doctor" -> Icons.Default.HealthAndSafety
                                        "A/L Exam Speaking" -> Icons.Default.School
                                        else -> Icons.Default.RecordVoiceOver
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = scenario,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) OnEmeraldContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        showScenarioDialog = false
                        onToggleHfSettings(true)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Model Settings", color = EmeraldPrimary)
                    }
                    TextButton(onClick = { showScenarioDialog = false }) {
                        Text("Close", color = EmeraldPrimary)
                    }
                }
            }
        )
    }

    // Keyboard Input BottomSheet / Dialog
    if (isKeyboardOpen) {
        AlertDialog(
            onDismissRequest = { onToggleKeyboard(false) },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Text("Type to Practice English", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Type any sentence, grammar question, or scenario dialogue. The AI will speak its response aloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keyboardInputText,
                        onValueChange = { keyboardInputText = it },
                        placeholder = { Text("e.g. Can you explain the difference between 'since' and 'for'?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_keyboard_input_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyboardInputText.isNotBlank()) {
                            onSendQuery(keyboardInputText.trim())
                            keyboardInputText = ""
                            onToggleKeyboard(false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send & Speak")
                }
            },
            dismissButton = {
                TextButton(onClick = { onToggleKeyboard(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Hugging Face Space Connection & Settings Dialog
    if (isHfSettingsOpen) {
        var tokenInput by remember { mutableStateOf(customHfToken) }
        var baseUrlInput by remember { mutableStateOf(customHfBaseUrl) }

        AlertDialog(
            onDismissRequest = { onToggleHfSettings(false) },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Text("AI Model & HF Space", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldContainer.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Deployed HuggingFace Space:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = OnEmeraldContainer
                            )
                            Text(
                                text = "DZD Learn AI Model Engine",
                                fontSize = 12.sp,
                                color = EmeraldDark,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Status: $hfStatusMessage",
                                fontSize = 11.sp,
                                color = OnEmeraldContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Access Token (for Private Space):",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary
                        )
                    )
                    Text(
                        text = "Tip: You can configure your Hugging Face API Token in App Settings.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Base API URL:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        placeholder = { Text(HuggingFaceSpaceClient.DEFAULT_SPACE_DIRECT_URL) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary
                        )
                    )

                    OutlinedButton(
                        onClick = onTestHfConnection,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection to Space", color = EmeraldPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateHfCredentials(tokenInput.trim(), baseUrlInput.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { onToggleHfSettings(false) }) {
                    Text("Close")
                }
            }
        )
    }
}



@Composable
fun ScenarioSelectorRow(
    scenarios: List<String>,
    selectedScenario: String,
    onScenarioSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scenarios.forEach { scenario ->
            val isSelected = scenario == selectedScenario
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onScenarioSelect(scenario) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (scenario) {
                            "Coffee Shop & Cafe" -> Icons.Default.LocalCafe
                            "Job Interview" -> Icons.Default.Work
                            "Travel & Airport" -> Icons.Default.Flight
                            "Grammar Doctor" -> Icons.Default.HealthAndSafety
                            "A/L Exam Speaking" -> Icons.Default.School
                            else -> Icons.Default.RecordVoiceOver
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color.White else EmeraldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = scenario,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun cleanMarkdownForClipboard(rawText: String): String {
    return rawText
        .replace(Regex("""\[([^\]]+)\]\((https?://[^\)]+)\)"""), "$1 ($2)")
        .replace(Regex("""(^|\n)\s*#{1,6}\s*"""), "$1")
        .replace(Regex("""\*{1,3}([^\*\n]+?)\*{1,3}"""), "$1")
        .replace(Regex("""_{1,3}([^_\n]+?)_{1,3}"""), "$1")
        .replace("**", "")
        .replace("*", "")
        .replace("`", "")
        .trim()
}

