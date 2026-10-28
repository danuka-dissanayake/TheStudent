package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationScenario
import com.example.data.model.GrammarPractice
import com.example.data.model.VocabularyWord
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLanguageScreen(
    activeSubTab: Int,
    onSubTabSelected: (Int) -> Unit,
    vocabularyList: List<VocabularyWord>,
    grammarList: List<GrammarPractice>,
    scenarios: List<ConversationScenario>,
    pronunciationSentences: List<String>,
    activeGrammarIndex: Int,
    selectedGrammarAnswer: Int?,
    grammarAnswerSubmitted: Boolean,
    onSubmitGrammarAnswer: (Int) -> Unit,
    onNextGrammar: () -> Unit,
    onSpeakText: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf("AI Voice", "Vocabulary", "Grammar", "Speaking")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ai_language_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "AI Language Lab",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LimeContainer,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = "CEFR B2/C1",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnLimeContainer
                    )
                }
            }
        )

        // Sub Tabs
        PrimaryTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeSubTab == index,
                    onClick = { onSubTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeSubTab == index) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (activeSubTab) {
            0 -> VoiceConversationTab(
                scenarios = scenarios,
                onSpeakText = onSpeakText
            )
            1 -> VocabularyTab(
                vocabularyList = vocabularyList,
                onSpeakText = onSpeakText
            )
            2 -> GrammarTab(
                grammar = grammarList.getOrNull(activeGrammarIndex) ?: grammarList.first(),
                currentIndex = activeGrammarIndex,
                totalCount = grammarList.size,
                selectedAnswer = selectedGrammarAnswer,
                isSubmitted = grammarAnswerSubmitted,
                onSelectAnswer = onSubmitGrammarAnswer,
                onNext = onNextGrammar
            )
            3 -> SpeakingPronunciationTab(
                sentences = pronunciationSentences,
                onSpeakText = onSpeakText
            )
        }
    }
}

@Composable
fun VoiceConversationTab(
    scenarios: List<ConversationScenario>,
    onSpeakText: (String) -> Unit
) {
    var selectedScenarioIndex by remember { mutableIntStateOf(0) }
    var isSimulatedVoiceActive by remember { mutableStateOf(false) }
    var currentDialogueIndex by remember { mutableIntStateOf(0) }

    val activeScenario = scenarios[selectedScenarioIndex]

    // Pulsing animation for microphone wave
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSimulatedVoiceActive) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Scenario Selector
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose Conversation Context",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scenarios.forEachIndexed { index, sc ->
                            val isSelected = selectedScenarioIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedScenarioIndex = index
                                    currentDialogueIndex = 0
                                    isSimulatedVoiceActive = false
                                },
                                label = {
                                    Text(
                                        text = when (index) {
                                            0 -> "Viva Interview"
                                            1 -> "Tech Job"
                                            else -> "Library"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = activeScenario.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeScenario.contextDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Voice Orb & Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = TealDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(waveScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(LimeAccent, EmeraldPrimary)
                                )
                            )
                            .clickable {
                                isSimulatedVoiceActive = !isSimulatedVoiceActive
                                if (isSimulatedVoiceActive) {
                                    val dialogue = activeScenario.sampleDialogue[currentDialogueIndex]
                                    onSpeakText("${dialogue.first} says: ${dialogue.second}")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSimulatedVoiceActive) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = "Voice Mic",
                            tint = Color(0xFF134E4A),
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Text(
                        text = if (isSimulatedVoiceActive) "AI Voice Active • Speaking..." else "Tap Microphone to Speak",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Role: You are '${activeScenario.userRole}' speaking with '${activeScenario.speakerRole}'",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Dialogue Script Cards
        item {
            Text(
                text = "Interactive Script & Responses",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(activeScenario.sampleDialogue) { (speaker, text) ->
            val isAiSpeaker = speaker != "Candidate" && speaker != "Applicant" && speaker != "Student"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAiSpeaker) MaterialTheme.colorScheme.surface else EmeraldContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isAiSpeaker) TealSecondary else EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = speaker.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = speaker,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAiSpeaker) TealSecondary else EmeraldPrimary
                            )
                            IconButton(
                                onClick = { onSpeakText(text) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak line",
                                    modifier = Modifier.size(16.dp),
                                    tint = EmeraldPrimary
                                )
                            }
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VocabularyTab(
    vocabularyList: List<VocabularyWord>,
    onSpeakText: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Essential Academic & Professional Words",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Includes Sinhala and Tamil definitions for Sri Lankan scholars",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(vocabularyList) { word ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = word.phonetic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { onSpeakText(word.word) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Pronounce",
                                tint = EmeraldPrimary
                            )
                        }
                    }

                    // Translations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = "සිංහල: ${word.translationSinhala}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnEmeraldContainer
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealContainer
                        ) {
                            Text(
                                text = "தமிழ்: ${word.translationTamil}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnTealContainer
                            )
                        }
                    }

                    Text(
                        text = word.definition,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "“${word.exampleSentence}”",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarTab(
    grammar: GrammarPractice,
    currentIndex: Int,
    totalCount: Int,
    selectedAnswer: Int?,
    isSubmitted: Boolean,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question ${currentIndex + 1} of $totalCount",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LimeContainer
            ) {
                Text(
                    text = "Grammar Mastery",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnLimeContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Rule card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = grammar.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = grammar.ruleExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Question Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = grammar.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grammar.options.forEachIndexed { index, option ->
                        val isSelected = selectedAnswer == index
                        val isCorrect = index == grammar.correctIndex

                        val containerColor = when {
                            !isSubmitted && isSelected -> EmeraldContainer
                            isSubmitted && isCorrect -> EmeraldPrimary
                            isSubmitted && isSelected && !isCorrect -> RedError.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val textColor = when {
                            isSubmitted && (isCorrect || (isSelected && !isCorrect)) -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSubmitted) {
                                    onSelectAnswer(index)
                                },
                            color = containerColor
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected || (isSubmitted && isCorrect)) Color.White else EmeraldPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ('A' + index).toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected || (isSubmitted && isCorrect)) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                if (isSubmitted) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Text(
                        text = "Explanation: ${grammar.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Next Question", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpeakingPronunciationTab(
    sentences: List<String>,
    onSpeakText: (String) -> Unit
) {
    var selectedSentenceIndex by remember { mutableIntStateOf(0) }
    var hasPracticedSpeaking by remember { mutableStateOf(false) }

    val currentSentence = sentences[selectedSentenceIndex]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pronunciation & Oral Fluency Training",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Listen to the standard academic cadence, then practice repeating it aloud.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sentence ${selectedSentenceIndex + 1} of ${sentences.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "“$currentSentence”",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onSpeakText(currentSentence) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Listen (TTS)")
                        }

                        Button(
                            onClick = {
                                hasPracticedSpeaking = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I Practiced")
                        }
                    }

                    if (hasPracticedSpeaking) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fluency Score: 94% • Excellent Intonation!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnEmeraldContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        if (selectedSentenceIndex > 0) {
                            selectedSentenceIndex--
                            hasPracticedSpeaking = false
                        }
                    },
                    enabled = selectedSentenceIndex > 0
                ) {
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (selectedSentenceIndex < sentences.size - 1) {
                            selectedSentenceIndex++
                            hasPracticedSpeaking = false
                        }
                    },
                    enabled = selectedSentenceIndex < sentences.size - 1,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Next Sentence")
                }
            }
        }
    }
}
