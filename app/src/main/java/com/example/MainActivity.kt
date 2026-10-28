package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreenFlow
import com.example.notifications.ChatNotifications
import com.example.viewmodel.TheStudentViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TheStudentViewModel by viewModels()

    /**
     * Peer id carried by a tapped chat notification. Held in a flow rather than
     * opening the chat directly so both cases work: a cold start (the intent
     * arrives before the directory is loaded) and a warm app (onNewIntent).
     */
    private val pendingChatPeer = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ChatNotifications.ensureChannel(this)
        askForNotificationPermission()
        readChatIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val savedPapers by viewModel.savedPapers.collectAsStateWithLifecycle()
            val savedCourses by viewModel.savedCourses.collectAsStateWithLifecycle()
            val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

            val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
            val snackbarHostState = remember { SnackbarHostState() }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()

            // Handle back navigation: close drawer if open, else return to Home if not on Home
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            // The profile step cannot be skipped with the system back gesture;
            // "Sign out" on the screen is the only way out.
            BackHandler(enabled = uiState.screenFlow == AppScreenFlow.PROFILE_SETUP) {
                // consumed on purpose
            }

            BackHandler(enabled = !drawerState.isOpen && !uiState.isLanguageLabOpen && uiState.screenFlow == AppScreenFlow.MAIN && (uiState.currentTab != TheStudentNavTab.HOME || uiState.isChatConversationOpen)) {
                if (uiState.isChatConversationOpen) {
                    viewModel.closeChat()
                } else {
                    viewModel.setTab(TheStudentNavTab.HOME)
                }
            }

            val chatPeerFromNotification by pendingChatPeer.collectAsStateWithLifecycle()
            LaunchedEffect(chatPeerFromNotification) {
                chatPeerFromNotification?.let { peerId ->
                    viewModel.openChatFromNotification(peerId)
                    pendingChatPeer.value = null
                }
            }

            LaunchedEffect(uiState.snackbarMessage) {
                uiState.snackbarMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.dismissSnackbar()
                }
            }

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                // ── Cross-flow navigation shimmer ───────────────────────────────────────
                // Shows NavigationTransitionShimmer for ~1050ms when navigating from any
                // auth/onboarding flow into the main dashboard.
                var showNavShimmer by remember { mutableStateOf(false) }
                val prevScreenFlow = remember { mutableStateOf(uiState.screenFlow) }

                LaunchedEffect(uiState.screenFlow) {
                    val from = prevScreenFlow.value
                    val to = uiState.screenFlow
                    if ((from == AppScreenFlow.AUTH ||
                            from == AppScreenFlow.ONBOARDING ||
                            from == AppScreenFlow.PROFILE_SETUP) &&
                        to == AppScreenFlow.MAIN
                    ) {
                        showNavShimmer = true
                        kotlinx.coroutines.delay(1050)
                        showNavShimmer = false
                    }
                    prevScreenFlow.value = to
                }

                // Outer animated switch: shimmer overlay vs. real screen tree
                AnimatedContent(
                    targetState = showNavShimmer,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)).togetherWith(
                            fadeOut(animationSpec = tween(200))
                        )
                    },
                    label = "nav_shimmer_overlay"
                ) { shimmerActive ->
                    if (shimmerActive) {
                        NavigationTransitionShimmer(modifier = Modifier.fillMaxSize())
                    } else {
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
                                onStartGoogleVerification = { profile, otp ->
                                    viewModel.startGoogleVerification(profile, otp)
                                },
                                onAuthSuccess = { profile ->
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
                                val serverUrl by viewModel.serverUrlState.collectAsStateWithLifecycle()
                                AuthScreen(
                                    initialIsRegister = uiState.isRegisterAuthMode,
                                    initialIsOtp = uiState.isOtpAuthMode,
                                    initialPendingProfile = uiState.pendingAuthProfile,
                                    initialOtpCode = uiState.pendingAuthOtp,
                                    backendBaseUrl = serverUrl,
                                    onAuthSuccess = { profile ->
                                        viewModel.handleAuthSuccess(profile)
                                    },
                                    onBack = {
                                        viewModel.handleAuthBack()
                                    }
                                )
                            }
                        }
                        AppScreenFlow.PROFILE_SETUP -> {
                            // Mandatory: phone, school, stream, photo (+ password
                            // for Google-only accounts) before the dashboard.
                            ProfileSetupScreen(
                                initialProfile = userProfile,
                                isSaving = uiState.isSavingProfile,
                                errorMessage = uiState.profileSetupError,
                                onSubmit = { submission ->
                                    viewModel.submitProfileSetup(submission)
                                },
                                onDismissError = { viewModel.clearProfileSetupError() },
                                onSignOut = { viewModel.logout() }
                            )
                        }
                        AppScreenFlow.MAIN -> {
                            if (!uiState.isAuthenticated && !viewModel.preferences.isLoggedIn) {
                                LaunchedEffect(Unit) {
                                    viewModel.openAuthScreen()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                )
                            } else if (uiState.isSecurityPanelOpen) {
                                // Account & security: password change, sign out of all
                                // devices, account deletion (Play policy requirement).
                                SecurityScreen(
                                    backendBaseUrl = viewModel.serverUrlState.collectAsStateWithLifecycle().value,
                                    onBack = { viewModel.closeSecurityPanel() },
                                    onSignOutEverywhere = { viewModel.signOutEverywhere() },
                                    onPasswordChanged = { newToken, expiresAt ->
                                        val prefs = viewModel.preferences
                                        val email = prefs.userEmail.orEmpty()
                                        val name = prefs.userName.orEmpty()
                                        if (!newToken.isNullOrBlank()) {
                                            // The server rotated the session: store the new token.
                                            com.example.data.local.TheStudentPreferences.setSession(
                                                context = appContext,
                                                token = newToken,
                                                userEmail = email,
                                                userName = name,
                                                rememberMe = true,
                                                role = prefs.userRole,
                                                expiresAt = expiresAt
                                            )
                                        }
                                    },
                                    onAccountDeleted = {
                                        viewModel.endSession(
                                            "Your account has been deleted.",
                                            revokeOnServer = false
                                        )
                                    }
                                )
                            } else if (uiState.isAdminPanelOpen) {
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
                                    gesturesEnabled = uiState.currentTab == TheStudentNavTab.HOME,
                                    drawerContent = {
                                        val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
                                        ModernSidebarDrawer(
                                            userProfile = userProfile,
                                            isAdmin = isAdmin,
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
                                            },
                                            onOpenAdminPanel = {
                                                viewModel.openAdminPanel()
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            onOpenSecurity = {
                                                viewModel.openSecurityPanel()
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            onSignOut = {
                                                coroutineScope.launch { drawerState.close() }
                                                viewModel.logout()
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
                                             if (uiState.currentTab != TheStudentNavTab.CHAT) {
                                                 TheStudentTopAppBar(
                                                     currentTab = uiState.currentTab,
                                                     streakDays = userProfile.streakDays,
                                                     onNavigationClick = {
                                                         if (uiState.currentTab == TheStudentNavTab.HOME) {
                                                             coroutineScope.launch { drawerState.open() }
                                                         } else {
                                                             viewModel.setTab(TheStudentNavTab.HOME)
                                                         }
                                                     },
                                                     onLanguageLabClick = { viewModel.openLanguageLab(0) },
                                                     onSearchClick = { viewModel.setTab(TheStudentNavTab.PAPERS) }
                                                 )
                                             }
                                         },
                                            bottomBar = {
                                                if (uiState.currentTab != TheStudentNavTab.CHAT || !uiState.isChatConversationOpen) {
                                                    TheStudentBottomNavigationBar(
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
                                                    TheStudentNavTab.HOME -> {
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
                                                    TheStudentNavTab.CHAT -> {
                                                         ChatScreen(
                                                             viewModel = viewModel
                                                         )
                                                     }
                                                     TheStudentNavTab.PAPERS -> {
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
                                                    TheStudentNavTab.AI_TUTOR -> {
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
                                                    TheStudentNavTab.PROFILE -> {
                                                        // The academic step is a full-screen page rather
                                                        // than a dialog: it can be long on O/L, and it is
                                                        // the data the matching algorithm depends on.
                                                        if (uiState.isAcademicSetupOpen) {
                                                            AcademicSetupScreen(
                                                                initial = userProfile.academic,
                                                                isSaving = uiState.isSavingAcademic,
                                                                errorMessage = uiState.academicSetupError,
                                                                onDismiss = { viewModel.closeAcademicSetup() },
                                                                onSave = { profile ->
                                                                    viewModel.saveAcademicProfile(profile)
                                                                }
                                                            )
                                                        } else {
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
                                                                },
                                                                onEditAcademic = { viewModel.openAcademicSetup() }
                                                            )
                                                        }
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
                    } // end else (not shimmerActive)
                } // end outer AnimatedContent (nav shimmer)
            }
        }
    // ── notifications & deep links ────────────────────────────────────────────

    override fun onStart() {
        super.onStart()
        // While the app is on screen the chat list itself shows the unread
        // badges, so the view model clears whatever the shade was holding.
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        // Going to the background: anything still unread becomes a notification.
        viewModel.onAppBackgrounded()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readChatIntent(intent)
    }

    /** A notification was tapped: remember which conversation to open. */
    private fun readChatIntent(intent: Intent?) {
        val peerId = intent?.getIntExtra(ChatNotifications.EXTRA_PEER_ID, 0) ?: 0
        if (peerId > 0) pendingChatPeer.value = peerId
    }

    /**
     * Android 13+ requires the runtime grant before anything can be posted.
     * Declining is harmless — chat still works, just silently.
     */
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        runCatching {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    private companion object {
        const val REQUEST_POST_NOTIFICATIONS = 4_201
    }

    }
    
    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(text = "TheStudent - $name", modifier = modifier)
    }
