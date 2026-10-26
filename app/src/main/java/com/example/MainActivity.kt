package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreenFlow
import com.example.viewmodel.DzdViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DzdViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val savedPapers by viewModel.savedPapers.collectAsStateWithLifecycle()
            val savedCourses by viewModel.savedCourses.collectAsStateWithLifecycle()
            val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()

            // Handle back navigation: close drawer if open, else return to Home if not on Home
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            BackHandler(enabled = !drawerState.isOpen && !uiState.isLanguageLabOpen && uiState.screenFlow == AppScreenFlow.MAIN && (uiState.currentTab != DzdNavTab.HOME || uiState.activeChatConversation != null)) {
                if (uiState.activeChatConversation != null) {
                    viewModel.closeChat()
                } else {
                    viewModel.setTab(DzdNavTab.HOME)
                }
            }

            LaunchedEffect(uiState.snackbarMessage) {
                uiState.snackbarMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.dismissSnackbar()
                }
            }

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                AnimatedContent(
                    targetState = uiState.screenFlow,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)).togetherWith(
                            fadeOut(animationSpec = tween(180))
                        )
                    },
                    label = "root_flow_transition"
                ) { flow ->
                    when (flow) {
                        AppScreenFlow.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = { viewModel.finishSplash() }
                            )
                        }
                        AppScreenFlow.ONBOARDING -> {
                            OnboardingScreen(
                                onFinishOnboarding = { openAuth, isRegister ->
                                    viewModel.finishOnboarding(openAuth = openAuth, isRegister = isRegister)
                                },
                                onContinueAsGuest = {
                                    viewModel.continueAsGuest()
                                },
                                onSocialLoginSuccess = { profile ->
                                    viewModel.handleAuthSuccess(profile)
                                }
                            )
                        }
                        AppScreenFlow.AUTH -> {
                            if (uiState.isAdminPanelOpen) {
                                val dynamicPapers by viewModel.dynamicPapers.collectAsStateWithLifecycle()
                                val isLiveDb by viewModel.isLiveDatabaseConnected.collectAsStateWithLifecycle()
                                val serverUrl by viewModel.serverUrlState.collectAsStateWithLifecycle()
                                val isTestingConn by viewModel.isTestingConnection.collectAsStateWithLifecycle()
                                val connResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()

                                AdminPanelScreen(
                                    papersList = dynamicPapers,
                                    serverUrl = serverUrl,
                                    isLiveConnected = isLiveDb,
                                    onUploadPaper = { viewModel.uploadNewPaper(it) },
                                    onDeletePaper = { viewModel.deletePaper(it) },
                                    onUpdateServerUrl = { viewModel.updateBackendUrl(it) },
                                    isTestingConnection = isTestingConn,
                                    connectionTestResult = connResult,
                                    onTestAndSaveServerUrl = { viewModel.testAndSaveServerUrl(it) },
                                    onBack = { viewModel.closeAdminPanel() }
                                )
                            } else {
                                AuthScreen(
                                    initialIsRegister = uiState.isRegisterAuthMode,
                                    onAuthSuccess = { profile ->
                                        viewModel.handleAuthSuccess(profile)
                                    },
                                    onContinueAsGuest = {
                                        viewModel.continueAsGuest()
                                    },
                                    onOpenAdminPanel = {
                                        viewModel.openAdminPanel()
                                    }
                                )
                            }
                        }
                        AppScreenFlow.MAIN -> {
                            if (uiState.isAdminPanelOpen) {
                                val dynamicPapers by viewModel.dynamicPapers.collectAsStateWithLifecycle()
                                val isLiveDb by viewModel.isLiveDatabaseConnected.collectAsStateWithLifecycle()
                                val serverUrl by viewModel.serverUrlState.collectAsStateWithLifecycle()
                                val isTestingConn by viewModel.isTestingConnection.collectAsStateWithLifecycle()
                                val connResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()

                                AdminPanelScreen(
                                    papersList = dynamicPapers,
                                    serverUrl = serverUrl,
                                    isLiveConnected = isLiveDb,
                                    onUploadPaper = { viewModel.uploadNewPaper(it) },
                                    onDeletePaper = { viewModel.deletePaper(it) },
                                    onUpdateServerUrl = { viewModel.updateBackendUrl(it) },
                                    isTestingConnection = isTestingConn,
                                    connectionTestResult = connResult,
                                    onTestAndSaveServerUrl = { viewModel.testAndSaveServerUrl(it) },
                                    onBack = { viewModel.closeAdminPanel() }
                                )
                            } else if (uiState.isAiAssistantOpen) {
                                AiAssistantScreen(
                                    userProfile = userProfile,
                                    messages = uiState.aiAssistantMessages,
                                    isGenerating = uiState.isAiAssistantGenerating,
                                    isReasoningActive = uiState.isAiReasoningActive,
                                    streamingReasoningText = uiState.aiStreamingReasoningText,
                                    inputText = uiState.aiAssistantInputText,
                                    pendingAttachment = uiState.pendingAiAttachment,
                                    selectedSubject = uiState.selectedAiSubject,
                                    onInputChange = { viewModel.setAiAssistantInput(it) },
                                    onSendMessage = { text, attachment -> viewModel.sendAiAssistantMessage(text, attachment) },
                                    onAttachMedia = { viewModel.setPendingAiAttachment(it) },
                                    onClearChat = { viewModel.clearAiAssistantChat() },
                                    onSelectSubject = { viewModel.selectAiAssistantSubject(it) },
                                    onRateMessage = { id, helpful -> viewModel.rateAiAssistantMessage(id, helpful) },
                                    onSpeakText = { viewModel.speakAiAssistantText(it) },
                                    onRetryMessage = { viewModel.retryAiAssistantMessage(it) },
                                    onStopGenerating = { viewModel.stopAiAssistantGeneration() },
                                    onBack = { viewModel.closeAiAssistant() },
                                    isOcrExtracting = uiState.isOcrExtracting,
                                    ocrExtractedText = uiState.ocrExtractedText,
                                    ocrDetectedLanguage = uiState.ocrDetectedLanguage,
                                    isOcrFastMode = uiState.isOcrFastMode,
                                    onRetryOcr = { uri, lang -> viewModel.runOcrExtraction(uri, lang) },
                                    onUpdateOcrText = { viewModel.updateOcrExtractedText(it) },
                                    onToggleOcrFastMode = { viewModel.toggleOcrFastMode(it) },
                                    selectedModel = uiState.selectedAiModel,
                                    onSelectModel = { viewModel.selectAiModel(it) },
                                    customZaiApiKey = uiState.customZaiApiKey,
                                    onUpdateZaiApiKey = { viewModel.updateZaiApiKey(it) }
                                )
                            } else if (uiState.isLanguageLabOpen) {
                                AiLanguageScreen(
                                    activeSubTab = uiState.activeLanguageSubTab,
                                    onSubTabSelected = { viewModel.setLanguageSubTab(it) },
                                    vocabularyList = viewModel.vocabularyList,
                                    grammarList = viewModel.grammarList,
                                    scenarios = viewModel.conversationScenarios,
                                    pronunciationSentences = viewModel.pronunciationSentences,
                                    activeGrammarIndex = uiState.activeGrammarQuestionIndex,
                                    selectedGrammarAnswer = uiState.selectedGrammarAnswer,
                                    grammarAnswerSubmitted = uiState.grammarAnswerSubmitted,
                                    onSubmitGrammarAnswer = { viewModel.submitGrammarAnswer(it) },
                                    onNextGrammar = { viewModel.nextGrammarQuestion() },
                                    onSpeakText = { viewModel.speakText(it) },
                                    onClose = { viewModel.closeLanguageLab() }
                                )
                            } else {
                                ModalNavigationDrawer(
                                    drawerState = drawerState,
                                    gesturesEnabled = uiState.currentTab == DzdNavTab.HOME,
                                    drawerContent = {
                                        ModernSidebarDrawer(
                                            userProfile = userProfile,
                                            currentTab = uiState.currentTab,
                                            onSelectTab = { tab ->
                                                viewModel.setTab(tab)
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            onOpenAiAssistant = {
                                                viewModel.openAiAssistant()
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            onOpenLanguageLab = {
                                                viewModel.openLanguageLab(0)
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            onCloseDrawer = {
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                ) {
                                    Scaffold(
                                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("main_scaffold"),
                                         topBar = {
                                             if (uiState.currentTab != DzdNavTab.CHAT) {
                                                 DzdTopAppBar(
                                                     currentTab = uiState.currentTab,
                                                     streakDays = userProfile.streakDays,
                                                     onNavigationClick = {
                                                         if (uiState.currentTab == DzdNavTab.HOME) {
                                                             coroutineScope.launch { drawerState.open() }
                                                         } else {
                                                             viewModel.setTab(DzdNavTab.HOME)
                                                         }
                                                     },
                                                     onLanguageLabClick = { viewModel.openLanguageLab(0) },
                                                     onSearchClick = { viewModel.setTab(DzdNavTab.PAPERS) }
                                                 )
                                             }
                                         },
                                            bottomBar = {
                                                if (uiState.currentTab != DzdNavTab.CHAT || uiState.activeChatConversation == null) {
                                                    DzdBottomNavigationBar(
                                                        currentTab = uiState.currentTab,
                                                        onTabSelected = { viewModel.setTab(it) }
                                                    )
                                                }
                                            },
                                            snackbarHost = {
                                                SnackbarHost(hostState = snackbarHostState)
                                            }
                                        ) { innerPadding ->
                                        val bottomNavTopLinePadding = (innerPadding.calculateBottomPadding() - 18.dp).coerceAtLeast(0.dp)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    top = innerPadding.calculateTopPadding(),
                                                    bottom = bottomNavTopLinePadding
                                                )
                                        ) {
                                            Crossfade(
                                                targetState = uiState.currentTab,
                                                label = "tab_transition"
                                            ) { tab ->
                                                when (tab) {
                                                    DzdNavTab.HOME -> {
                                                        HomeScreen(
                                                            userProfile = userProfile,
                                                            popularCourses = viewModel.allCourses,
                                                            recentNotes = viewModel.allNotes,
                                                            recentPapers = viewModel.allPapers,
                                                            savedCourses = savedCourses,
                                                            isLoading = uiState.isLoadingContent,
                                                            onCourseClick = { viewModel.openCourseDetail(it) },
                                                            onPaperClick = { viewModel.openPaperDetail(it) },
                                                            onOpenAiAssistant = { viewModel.openAiAssistant() },
                                                            onOpenLanguageLab = { viewModel.openLanguageLab(0) },
                                                            onNavigateTab = { viewModel.setTab(it) }
                                                        )
                                                    }
                                                    DzdNavTab.CHAT -> {
                                                         ChatScreen(
                                                             viewModel = viewModel
                                                         )
                                                     }
                                                     DzdNavTab.PAPERS -> {
                                                        val dynamicPapers by viewModel.dynamicPapers.collectAsStateWithLifecycle()
                                                        val isLiveDb by viewModel.isLiveDatabaseConnected.collectAsStateWithLifecycle()
                                                        val serverUrl by viewModel.serverUrlState.collectAsStateWithLifecycle()
    
                                                        PapersScreen(
                                                            papers = dynamicPapers,
                                                            savedPapers = savedPapers,
                                                            selectedLevel = uiState.selectedEducationLevel,
                                                            isLoading = uiState.isLoadingContent,
                                                            isLiveDatabase = isLiveDb,
                                                            serverUrl = serverUrl,
                                                            onLevelSelected = {
                                                                viewModel.setEducationLevel(it)
                                                                viewModel.simulateSkeletonRefresh()
                                                            },
                                                            onPaperClick = { viewModel.openPaperDetail(it) },
                                                            onToggleSavePaper = { viewModel.toggleSavePaper(it) },
                                                            onDownloadPaper = { viewModel.downloadPaperOffline(it) },
                                                            onRefreshPapers = { viewModel.loadDynamicPapers(forceRefresh = true) },
                                                            onUpdateServerUrl = { viewModel.updateBackendUrl(it) }
                                                        )
                                                    }
                                                    DzdNavTab.AI_TUTOR -> {
                                                        AiTutorScreen(
                                                            voiceState = uiState.voiceAssistantState,
                                                            micRmsDb = uiState.micRmsDb,
                                                            userTranscript = uiState.currentUserTranscript,
                                                            aiResponseText = uiState.currentAiResponseText,
                                                            grammarTip = uiState.currentGrammarTip,
                                                            vocabularyHighlight = uiState.currentVocabularyHighlight,
                                                            currentScenario = uiState.currentScenario,
                                                            suggestedPills = uiState.suggestedVoicePills,
                                                            isHandsFree = uiState.isHandsFreeActive,
                                                            isKeyboardOpen = uiState.isKeyboardInputOpen,
                                                            isHfSettingsOpen = uiState.isHfSettingsOpen,
                                                            customHfToken = uiState.customHfToken,
                                                            customHfBaseUrl = uiState.customHfBaseUrl,
                                                            hfStatusMessage = uiState.hfStatusMessage,
                                                            onStartListening = { viewModel.startVoiceListening() },
                                                            onStopListening = { viewModel.stopVoiceListening() },
                                                            onToggleVoice = { viewModel.toggleVoiceListening() },
                                                            onSendQuery = { viewModel.processUserVoiceInput(it) },
                                                            onReplaySpeech = { viewModel.replayCurrentAiResponse() },
                                                            onStopSpeech = { viewModel.stopAiSpeech() },
                                                            onSelectScenario = { viewModel.selectVoiceScenario(it) },
                                                            onToggleHandsFree = { viewModel.toggleHandsFreeMode() },
                                                            onToggleKeyboard = { viewModel.toggleKeyboardInput(it) },
                                                            onToggleHfSettings = { viewModel.toggleHfSettings(it) },
                                                            onUpdateHfCredentials = { token, url -> viewModel.updateHfCredentials(token, url) },
                                                            onTestHfConnection = { viewModel.testHfSpaceConnection() },
                                                            onShowSnackbar = { viewModel.showSnackbar(it) }
                                                        )
                                                    }
                                                    DzdNavTab.PROFILE -> {
                                                        ProfileScreen(
                                                            userProfile = userProfile,
                                                            savedPapers = savedPapers,
                                                            savedCourses = savedCourses,
                                                            recentActivities = recentActivities,
                                                            isDarkTheme = uiState.isDarkTheme,
                                                            onToggleTheme = { viewModel.toggleTheme() },
                                                            onOpenAuthDialog = { viewModel.openAuthScreen(false) },
                                                            onPaperClick = { paperId ->
                                                                viewModel.allPapers.find { it.id == paperId }?.let {
                                                                    viewModel.openPaperDetail(it)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
    
                                // Active Paper Detail Dialog
                                uiState.activePaperDetail?.let { paper ->
                                    val isSaved = savedPapers.any { it.id == paper.id }
                                    val isDownloaded = savedPapers.find { it.id == paper.id }?.isDownloaded == true
    
                                    PaperDetailDialog(
                                        paper = paper,
                                        isSaved = isSaved,
                                        isDownloaded = isDownloaded,
                                        onToggleSave = { viewModel.toggleSavePaper(paper) },
                                        onDownload = { viewModel.downloadPaperOffline(paper) },
                                        onAskAi = {
                                            viewModel.closePaperDetail()
                                            viewModel.openAiAssistant()
                                            viewModel.setAiAssistantInput("Can you guide me through ${paper.title} and explain key questions?")
                                        },
                                        onDismiss = { viewModel.closePaperDetail() }
                                    )
                                }
    
                                // Active Course Detail Dialog
                                uiState.activeCourseDetail?.let { course ->
                                    val isEnrolled = savedCourses.any { it.id == course.id }
                                    CourseDetailDialog(
                                        course = course,
                                        isEnrolled = isEnrolled,
                                        onToggleEnroll = { viewModel.toggleSaveCourse(course) },
                                        onDismiss = { viewModel.closeCourseDetail() }
                                    )
                                }
    
                                // Auth Dialog (Sign up / Login / Google Login modal fallback)
                                AuthDialog(
                                    isOpen = uiState.isAuthDialogOpen,
                                    onDismiss = { viewModel.toggleAuthDialog(false) },
                                    onSuccess = { message ->
                                        viewModel.showSnackbar(message)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(text = "TheStudent - $name", modifier = modifier)
    }
