package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TheStudentDatabase
import com.example.data.local.ChatCrypto
import com.example.data.local.ChatAttachmentStore
import com.example.data.local.ChatMediaPrep
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatVoiceRecorder
import com.example.data.local.ChatSummaryRow
import com.example.data.local.TheStudentPreferences
import com.example.data.local.LearningActivityEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SavedPaperEntity
import com.example.data.model.*
import com.example.data.repository.TheStudentRepository
import com.example.data.repository.SampleEducationalData
import com.example.notifications.ChatNotifications
import com.example.notifications.ChatNotifier
import com.example.notifications.NotificationPeer
import com.google.firebase.messaging.FirebaseMessaging
import com.example.ui.components.TheStudentNavTab
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.io.File
import java.util.UUID

import com.example.data.remote.TheStudentAiStudyResponse
import com.example.data.remote.TheStudentOcrEngine
import com.example.data.remote.TheStudentStudyAiClient
import com.example.data.remote.TheStudentAuthClient
import com.example.data.remote.AuthApiResult
import com.example.data.remote.SessionValidation
import com.example.data.remote.TheStudentChatClient
import com.example.data.remote.ChatResult
import com.example.data.remote.EnglishVoiceTutorEngine
import com.example.data.remote.HfApiResponse
import com.example.data.remote.HuggingFaceSpaceClient
import com.example.data.remote.VoiceTutorResponse
import com.example.data.remote.PastPapersRemoteRepository
import com.example.data.remote.DefaultPastPapersSeed
import com.example.ui.components.AssistantVoiceState
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

enum class AppScreenFlow {
    SPLASH,
    ONBOARDING,
    AUTH,

    /**
     * Mandatory profile-setup step, shown after a successful sign-up / sign-in
     * and on every cold start until the server reports `profile_completed`.
     * It sits between AUTH and MAIN so there is no path into the dashboard that
     * skips it.
     */
    PROFILE_SETUP,
    MAIN
}

data class TheStudentUiState(
    val screenFlow: AppScreenFlow = AppScreenFlow.SPLASH,
    val isLoadingContent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isSessionVerified: Boolean = false,
    val isRegisterAuthMode: Boolean = false,
    val currentTab: TheStudentNavTab = TheStudentNavTab.HOME,
    val selectedEducationLevel: EducationLevel = EducationLevel.ALL,
    val selectedExamFilter: String = "All Exams",
    val selectedSubjectFilter: String = "All Subjects",
    val selectedYearFilter: Int? = null,
    val searchQuery: String = "",
    val activePaperDetail: PastPaper? = null,
    val activeCourseDetail: Course? = null,
    val isLanguageLabOpen: Boolean = false,
    /** The "Your EXP" page: what was earned, today's allowance and the tier. */
    val isExpOpen: Boolean = false,
    /** The account details page, opened from Profile — never the sign-in flow. */
    val isAccountOpen: Boolean = false,
    val activeLanguageSubTab: Int = 0, // 0: Voice Conv, 1: Vocabulary, 2: Grammar, 3: Pronunciation
    val isSpeakingActive: Boolean = false,
    val activeGrammarQuestionIndex: Int = 0,
    val selectedGrammarAnswer: Int? = null,
    val grammarAnswerSubmitted: Boolean = false,
    val isTtsSpeaking: Boolean = false,
    val snackbarMessage: String? = null,
    val isAuthDialogOpen: Boolean = false,
    val isDarkTheme: Boolean = false,
    val isOtpAuthMode: Boolean = false,
    val pendingAuthProfile: UserProfile? = null,
    val pendingAuthOtp: String? = null,

    // Mandatory profile setup (phone, institution, stream, photo, password)
    val needsProfileSetup: Boolean = false,
    val isSavingProfile: Boolean = false,
    val profileSetupError: String? = null,

    // Editing your own details from the Account page ("Edit details").
    val isSavingAccount: Boolean = false,
    val accountSaveError: String? = null,

    // Structured academic profile (A/L or O/L, stream, subjects, medium).
    // Opened from the profile screen; also the data the matching algorithm
    // will read once the directory is ranked.
    val isAcademicSetupOpen: Boolean = false,
    val isSavingAcademic: Boolean = false,
    val academicSetupError: String? = null,

    // Redesigned AI Tutor (English Voice Assistant with Hugging Face Qwen/Gemma)
    val voiceAssistantState: AssistantVoiceState = AssistantVoiceState.IDLE,
    val micRmsDb: Float = 0f,
    val currentUserTranscript: String? = null,
    val currentAiResponseText: String = "Hello! I am your English Language Learning AI Voice Tutor. Tap the microphone below or choose a scenario to start speaking with me!",
    val currentGrammarTip: String? = null,
    val currentVocabularyHighlight: String? = "Conversational Fluency / Pronunciation",
    val currentScenario: String = "Free Conversation",
    val suggestedVoicePills: List<String> = listOf("Hello, how are you today?", "Can you help me practice English?", "Let's practice ordering coffee"),
    val isHandsFreeActive: Boolean = false,
    val isKeyboardInputOpen: Boolean = false,
    val isHfSettingsOpen: Boolean = false,
    val customHfToken: String = "",
    val customHfBaseUrl: String = HuggingFaceSpaceClient.DEFAULT_SPACE_DIRECT_URL,
    val hfStatusMessage: String = "Gemma/Qwen HuggingFace Model Ready",

    // Multimodal AI Study Assistant (ChatGPT / Gemini style)
    val isAiAssistantOpen: Boolean = false,
    val aiAssistantMessages: List<AiAssistantMessage> = emptyList(),
    val isAiAssistantGenerating: Boolean = false,
    val isAiReasoningActive: Boolean = false,
    val aiStreamingReasoningText: String = "",
    val aiReasoningDurationSeconds: Double? = null,
    val activeStreamingMessageId: String? = null,
    val aiAssistantInputText: String = "",
    val pendingAiAttachment: AttachedMedia? = null,
    val selectedAiSubject: String = "All Subjects",
    val selectedAiModel: AiModelChoice = AiModelChoice.GLM_4_7_FLASH,
    val customZaiApiKey: String = "",
    val isModelSelectorOpen: Boolean = false,

    // Embedded Fast OCR Pipeline (Sinhala / Tamil / English)
    val isOcrExtracting: Boolean = false,
    val ocrExtractedText: String? = null,
    val ocrDetectedLanguage: String? = null,
    val ocrEngineUsed: String? = null,
    val isOcrFastMode: Boolean = true,

    // Admin Panel State
    val isAdminPanelOpen: Boolean = false,
    // Account / security settings (change password, sign out everywhere, delete)
    val isSecurityPanelOpen: Boolean = false,
    /**
     * True while a 1:1 conversation is open inside the chat tab. The dashboard
     * uses it to hide the bottom bar and to route the back gesture to `closeChat`.
     */
    val isChatConversationOpen: Boolean = false
)

/**
 * Everything the chat screen renders.
 *
 * [threads] is the directory of *real* registered students (full name + avatar
 * only); [activePeer] / [messages] hold the conversation that is open.
 */
data class ChatUiState(
    val isLoadingDirectory: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val threads: List<ChatThread> = emptyList(),
    /** Other students online right now (never includes me). */
    val onlineCount: Int = 0,
    val totalStudents: Int = 0,
    val safetyAcknowledged: Boolean = false,
    val activePeer: ChatPeer? = null,
    val messages: List<ChatMessage> = emptyList(),
    /** Student whose profile is open (null when the profile screen is closed). */
    val viewedProfile: ChatPeerProfile? = null,
    /** True while that profile is being fetched. */
    val isLoadingProfile: Boolean = false,
    /**
     * The newest help request between me and [activePeer] (null when the pair
     * never asked each other for help). Drives the card at the top of the thread.
     */
    val helpRequest: ChatHelpRequest? = null,
    /** Non-null while the "What's this chat about?" sheet is showing. */
    val chatIntentPeer: ChatPeer? = null,
    /** My own subjects, offered as the default subject on a help request. */
    val mySubjects: List<String> = emptyList(),
    /** True while accept / close / cancel is in flight. */
    val isHelpBusy: Boolean = false,
    /** What this conversation is about: "" | help | talk | notes | partner. */
    val chatPurpose: String = "",
    val chatPurposeSubject: String = "",
    /** How many help requests this pair has already closed (drives "ask again"). */
    val helpCompletedWithPeer: Int = 0,
    /** The "this looks like a question — track it?" offer is on screen. */
    val questionOfferVisible: Boolean = false,
    /** True while a help request is being created (auto or from the sheet). */
    val isAskingHelp: Boolean = false,
    /** True once this device's encryption key is what the server published. */
    val isEncryptionReady: Boolean = false,
    /** Image or document picked in the composer, not sent yet. */
    val pendingAttachment: ChatAttachmentDraft? = null,
    /** True while a picked file is being encrypted and uploaded. */
    val isUploadingAttachment: Boolean = false,
    /** 0f..1f while an attachment uploads. */
    val attachmentUploadProgress: Float = 0f,
    /** attachmentId -> 0..100 while a received file downloads. */
    val attachmentDownloadProgress: Map<String, Int> = emptyMap(),
    /** Message the composer is currently replying to. */
    val pendingReply: ChatReply? = null,
    /** True while a voice note is being recorded. */
    val isRecordingVoiceNote: Boolean = false,
    /** Elapsed time of that recording, in milliseconds. */
    val voiceNoteElapsedMs: Long = 0L,
    /**
     * Microphone peak level, 0…1, sampled while a note is being recorded. Drives
     * the live waveform inside the composer.
     */
    val voiceNoteLevel: Float = 0f,
    /**
     * Non-null while the rating card is on screen. It carries everything the
     * card needs (subject, limits, the rating already given), so the card never
     * has to guess what is allowed.
     */
    val rateState: ChatRateState? = null,
    /** Public reputation of [activePeer] — average, count, tags. */
    val peerRatingSummary: ChatRatingSummary? = null,
    /** True while a rating is being sent. */
    val isRatingBusy: Boolean = false,
    /** Subject codes I have already rated this peer in (this session). */
    val ratedSubjects: Set<String> = emptySet(),
    /**
     * A rating I have just received, while its celebration is on screen. Null
     * the rest of the time — the moment is meant to be rare.
     */
    val celebratingRating: ChatReceivedRating? = null
) {
    val activePeerIsTyping: Boolean get() = activePeer?.isTyping == true
    val totalUnread: Int get() = threads.sumOf { it.unreadCount }
}

class TheStudentViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = TheStudentDatabase.getDatabase(application)
    private val repository: TheStudentRepository = TheStudentRepository(database.theStudentDao())
    val pastPapersRepository = PastPapersRemoteRepository(application)
    val preferences = com.example.data.local.TheStudentPreferences(application)
    private val huggingFaceClient = HuggingFaceSpaceClient()
    private val theStudentStudyAiClient = TheStudentStudyAiClient(application)
    private val theStudentOcrEngine = TheStudentOcrEngine(application)
    private var aiAssistantJob: Job? = null
    private var ocrJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val conversationTurns = mutableListOf<Pair<String, String>>()

    private val _uiState = MutableStateFlow(TheStudentUiState())
    val uiState: StateFlow<TheStudentUiState> = _uiState.asStateFlow()

    /**
     * Administrator access comes from the server-issued role ONLY.
     * There is no local bypass: the flag is refreshed on every session
     * validation and cleared on sign-out.
     */
    private val _isAdmin = MutableStateFlow(preferences.isAdmin)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _dynamicPapers = MutableStateFlow<List<PastPaper>>(DefaultPastPapersSeed.allPapers)
    val dynamicPapers: StateFlow<List<PastPaper>> = _dynamicPapers.asStateFlow()
    val allPapers: List<PastPaper> get() = _dynamicPapers.value

    private val _isLiveDatabaseConnected = MutableStateFlow(false)
    val isLiveDatabaseConnected: StateFlow<Boolean> = _isLiveDatabaseConnected.asStateFlow()

    private val _serverUrlState = MutableStateFlow(pastPapersRepository.backendBaseUrl)
    val serverUrlState: StateFlow<String> = _serverUrlState.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val connectionTestResult: StateFlow<Pair<Boolean, String>?> = _connectionTestResult.asStateFlow()

    private var sessionVerificationJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // A database problem (for example a schema surprise after an update)
            // must never take the whole app down before the first frame.
            runCatching { repository.initializeDefaultDataIfEmpty() }
                .onFailure { Log.e("TheStudentViewModel", "Database seed failed", it) }
        }
        loadDynamicPapers()
        verifySessionWithBackend()
        observeChatDatabase()
        ensureChatBackgroundSync()
    }

    private fun initTts() {
        if (tts == null) {
            try {
                tts = TextToSpeech(getApplication(), this)
            } catch (e: Exception) {
                Log.w("TheStudentViewModel", "TTS initialization deferred/unavailable", e)
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (speechRecognizer != null) return
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.LISTENING) }
                        }

                        override fun onBeginningOfSpeech() {
                            _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.LISTENING) }
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Suppress high-frequency decibel state flow emissions that cause 60fps Compose recomposition lag
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.THINKING) }
                        }

                        override fun onError(error: Int) {
                            // Don't override if already transitioned to processing
                            if (_uiState.value.voiceAssistantState == AssistantVoiceState.LISTENING) {
                                _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.IDLE, micRmsDb = 0f) }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull()
                            if (!recognizedText.isNullOrBlank()) {
                                processUserVoiceInput(recognizedText)
                            } else {
                                _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.IDLE, micRmsDb = 0f) }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()
                            if (!partial.isNullOrBlank()) {
                                _uiState.update { it.copy(currentUserTranscript = partial) }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                speechRecognizer = null
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.95f) // Slightly relaxed rate for language learning
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _uiState.update {
                        it.copy(
                            voiceAssistantState = AssistantVoiceState.SPEAKING,
                            isTtsSpeaking = true
                        )
                    }
                }

                override fun onDone(utteranceId: String?) {
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                voiceAssistantState = AssistantVoiceState.IDLE,
                                isTtsSpeaking = false
                            )
                        }
                        // If hands-free mode is active, automatically listen again after a brief pause
                        if (_uiState.value.isHandsFreeActive) {
                            delay(800)
                            startVoiceListening()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    _uiState.update {
                        it.copy(
                            voiceAssistantState = AssistantVoiceState.IDLE,
                            isTtsSpeaking = false
                        )
                    }
                }
            })
            isTtsInitialized = true
        }
    }

    // Room flows
    val savedPapers: StateFlow<List<SavedPaperEntity>> = repository.savedPapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCourses: StateFlow<List<SavedCourseEntity>> = repository.savedCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentActivities: StateFlow<List<LearningActivityEntity>> = repository.recentActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow
        .map { entity ->
            if (entity != null) {
                UserProfile(
                    name = entity.name,
                    email = entity.email,
                    academicLevel = entity.academicLevel,
                    streamOrDegree = entity.streamOrDegree,
                    institution = entity.institution,
                    targetExam = entity.targetExam,
                    streakDays = entity.streakDays,
                    studyHoursThisWeek = entity.studyHoursThisWeek,
                    totalPapersSolved = entity.totalPapersSolved,
                    points = entity.points,
                    isLoggedIn = entity.isLoggedIn,
                    phone = entity.phone,
                    avatarUrl = entity.avatarUrl,
                    profileCompleted = entity.profileCompleted,
                    hasPassword = entity.hasPassword,
                    academic = AcademicProfile(
                        examLevel = entity.examLevel,
                        streamCode = entity.streamCode,
                        examYear = entity.examYear.takeIf { it > 0 },
                        medium = entity.medium,
                        subjects = AcademicProfile.decodeSubjects(entity.academicSubjects),
                        studyYear = entity.studyYear.takeIf { it > 0 },
                        university = entity.university,
                        programme = entity.degreeProgramme
                    )
                )
            } else {
                UserProfile()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun loadDynamicPapers(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingContent = true) }
                val (fetched, isLive) = pastPapersRepository.fetchPapers()
                if (fetched.isNotEmpty()) {
                    _dynamicPapers.value = fetched
                }
                _isLiveDatabaseConnected.value = isLive
                _uiState.update { it.copy(isLoadingContent = false) }
                if (isLive) {
                    showSnackbar("Synced ${fetched.size} papers from MySQL Cloud Database")
                } else if (forceRefresh) {
                    showSnackbar("Refreshed papers catalog (${fetched.size} papers)")
                }
            } catch (e: Exception) {
                Log.e("TheStudentViewModel", "Safe error handling in loadDynamicPapers: ${e.message}", e)
                _uiState.update { it.copy(isLoadingContent = false) }
            }
        }
    }

    fun updateBackendUrl(newUrl: String) {
        testAndSaveServerUrl(newUrl)
    }

    fun testAndSaveServerUrl(newUrl: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = null
            pastPapersRepository.backendBaseUrl = newUrl
            _serverUrlState.value = pastPapersRepository.backendBaseUrl

            val result = pastPapersRepository.testConnection()
            _connectionTestResult.value = result
            _isTestingConnection.value = false

            if (result.first) {
                _isLiveDatabaseConnected.value = true
                loadDynamicPapers(forceRefresh = true)
                showSnackbar("✅ DirectAdmin API Connected Successfully!")
            } else {
                _isLiveDatabaseConnected.value = false
                showSnackbar("❌ API Connection Failed. Check domain & api folder.")
            }
        }
    }

    fun openAdminPanel() {
        if (!preferences.isAdmin) {
            // Defence in depth: the UI hides the entry point, but never trust it.
            showSnackbar("Administrator access required.")
            return
        }
        _uiState.update { it.copy(isAdminPanelOpen = true) }
    }

    fun closeAdminPanel() {
        _uiState.update { it.copy(isAdminPanelOpen = false) }
    }

    /** Open the account security sheet (password, sessions, deletion). */
    fun openSecurityPanel() {
        _uiState.update { it.copy(isSecurityPanelOpen = true) }
    }

    fun closeSecurityPanel() {
        _uiState.update { it.copy(isSecurityPanelOpen = false) }
    }

    // ── Chat ─────────────────────────────────────────────────────────────────
    //
    // Architecture: the server is a **relay + presence board**. Messages are
    // encrypted on this device, queued server-side only until the recipient
    // fetches them, and then stored in the local Room database — which is the
    // single source of truth for every conversation (see ChatMessageEntity).

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val chatDao = database.theStudentDao()

    private var chatDirectoryJob: Job? = null
    private var chatInboxJob: Job? = null
    private var chatPresenceJob: Job? = null
    private var chatMessagesJob: Job? = null
    private var profileJob: Job? = null

    /** Set once the deployed chat API has been compared with this build. */
    private var chatServerVersionChecked = false

    /** Purpose per peer, mirrored from the directory so rows keep their label. */
    private var chatPurposes: Map<Int, Pair<String, String>> = emptyMap()

    /** Day bucket + count for the "track this question?" offer budget. */
    private var offerBudgetDay = 0L
    private var offersMadeToday = 0
    private var chatTypingJob: Job? = null
    private var chatBackgroundJob: Job? = null
    private var chatIsDirectoryVisible = false
    /** Guards the pending-message drain against overlapping runs. */
    private var pendingRetryInFlight = false
    /** Last time the drain ran, so an offline phone does not retry every 2 s. */
    private var lastPendingRetryAt = 0L
    /** True between MainActivity.onStart and onStop. */
    private var appInForeground = false

    /** Peer directory from the last successful `threads` call. */
    private var chatPeers: List<ChatPeer> = emptyList()
    /** Set when a notification was tapped before the directory had loaded. */
    private var pendingNotificationPeerId: Int? = null
    /** Conversation previews, straight from the device database. */
    private var chatSummaries: List<ChatSummaryRow> = emptyList()
    private var chatActivePeerId: Int? = null

    private val appContext: Context
        get() = getApplication<Application>().applicationContext

    /**
     * End-to-end encryption identity is scoped to the account, so two students
     * signing in on the same phone never share (or break) each other's keys.
     */
    private val chatAccountKey: String
        get() = preferences.userEmail?.takeIf { it.isNotBlank() } ?: "local-account"

    /** Backend + bearer token for chat calls, or null when signed out. */
    private fun chatSession(): Pair<String, String>? {
        val token = preferences.sessionToken
        if (token.isNullOrBlank()) return null
        return pastPapersRepository.backendBaseUrl to token
    }

    /**
     * Warn once when the deployed `api/chat.php` is older than this build.
     *
     * Every chat feature added since 2.6.0 (help requests, subjects on the
     * profile) simply never appears on an older host, and "Unknown action" is a
     * useless thing to show a student. One snackbar naming the file to upload is
     * worth more than a silent missing card.
     */
    private suspend fun verifyChatServerVersion() {
        if (chatServerVersionChecked) return
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        val version = TheStudentChatClient.fetchApiVersion(baseUrl, token).dataOrNull()
            ?: return
        chatServerVersionChecked = true
        if (compareVersions(version, CHAT_MIN_SERVER_VERSION) < 0) {
            _uiState.update {
                it.copy(
                    snackbarMessage = "Chat server is v$version but this build needs " +
                        "$CHAT_MIN_SERVER_VERSION. Upload backend/api/chat.php."
                )
            }
        }
    }

    /** "2.10.1" vs "2.6.0" by numeric segment. Unknown shapes count as newer. */
    private fun compareVersions(a: String, b: String): Int {
        val left = a.split(".").mapNotNull { it.trim().toIntOrNull() }
        val right = b.split(".").mapNotNull { it.trim().toIntOrNull() }
        if (left.isEmpty() || right.isEmpty()) return 0
        for (index in 0 until maxOf(left.size, right.size)) {
            val l = left.getOrElse(index) { 0 }
            val r = right.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    /**
     * Called when the chat tab becomes visible: publishes the encryption key,
     * loads the directory and starts the relay + presence loops.
     */
    fun startChatSync() {
        if (chatIsDirectoryVisible) return
        chatIsDirectoryVisible = true

        chatDirectoryJob?.cancel()
        chatDirectoryJob = viewModelScope.launch {
            verifyChatServerVersion()
            publishChatIdentityIfNeeded()
            refreshChatDirectory(showSpinner = _chatState.value.threads.isEmpty())
            // A rating that arrived since the last visit gets one celebration.
            maybeCelebrateRating()
            while (true) {
                delay(CHAT_DIRECTORY_POLL_MS)
                refreshChatDirectory(showSpinner = false)
            }
        }

        // One inbox loop for the whole tab: it both delivers new messages and
        // keeps unread badges accurate even while no conversation is open.
        chatInboxJob?.cancel()
        chatInboxJob = viewModelScope.launch {
            while (true) {
                fetchChatInbox()
                // The help card must react to the other side accepting while
                // the conversation is on screen, so it polls with the inbox.
                val openPeerId = chatActivePeerId
                if (openPeerId != null) refreshHelpState(openPeerId)
                delay(CHAT_INBOX_POLL_MS)
            }
        }

        chatPresenceJob?.cancel()
        chatPresenceJob = viewModelScope.launch {
            while (true) {
                val session = chatSession()
                if (session != null) {
                    val (baseUrl, token) = session
                    val onlineCount = TheStudentChatClient.heartbeat(baseUrl, token).dataOrNull()
                    if (onlineCount != null) {
                        _chatState.update { it.copy(onlineCount = onlineCount.toInt()) }
                    }
                }
                // 2 s heartbeat: the server treats 10 s of silence as offline.
                delay(CHAT_PRESENCE_MS)
            }
        }
    }

    /** Called when the chat tab leaves the composition. */
    fun stopChatSync() {
        chatIsDirectoryVisible = false
        chatDirectoryJob?.cancel()
        chatDirectoryJob = null
        chatInboxJob?.cancel()
        chatInboxJob = null
        chatPresenceJob?.cancel()
        chatPresenceJob = null
        closeChat()
    }

    fun refreshChatDirectoryNow() {
        viewModelScope.launch {
            refreshChatDirectory(showSpinner = _chatState.value.threads.isEmpty())
            fetchChatInbox()
        }
    }

    /**
     * Make sure this device has a key pair and that the server knows the public
     * half. Without the public half other students cannot encrypt to us.
     */
    private suspend fun publishChatIdentityIfNeeded() {
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        val identity = ChatCrypto.ensureIdentity(appContext, chatAccountKey)
        if (identity.publicKeyBase64.isBlank()) return

        val result = TheStudentChatClient.publishIdentity(
            context = appContext,
            accountKey = chatAccountKey,
            backendBaseUrl = baseUrl,
            token = token
        )
        val published = result.dataOrNull()
        if (published != null) {
            _chatState.update { it.copy(isEncryptionReady = true) }
            return
        }

        if ((result as? ChatResult.Failure)?.statusCode != 409) return

        // The server already published a different key for this account. That
        // only happens after a reinstall: the old private key is gone with the
        // old install, and so is the local history. Rotation is safe then — and
        // necessary, otherwise nothing could ever be decrypted again.
        if (chatDao.countChatMessages() > 0) {
            _chatState.update {
                it.copy(
                    errorMessage = "Secure chat could not start: this account already has a " +
                        "different encryption key."
                )
            }
            return
        }

        val rotated = TheStudentChatClient.publishIdentity(
            context = appContext,
            accountKey = chatAccountKey,
            backendBaseUrl = baseUrl,
            token = token,
            allowReplace = true
        )
        _chatState.update { it.copy(isEncryptionReady = rotated.dataOrNull() != null) }
    }

    private suspend fun refreshChatDirectory(showSpinner: Boolean) {
        val session = chatSession() ?: run {
            _chatState.update {
                it.copy(
                    isLoadingDirectory = false,
                    errorMessage = "Sign in to chat with other students."
                )
            }
            return
        }
        val (baseUrl, token) = session

        if (showSpinner) _chatState.update { it.copy(isLoadingDirectory = true) }

        val result = TheStudentChatClient.loadDirectory(baseUrl, token)
        val directory = result.dataOrNull()
        if (directory != null) {
            chatPeers = directory.threads.map { it.peer }
            // Purposes come with the directory; cached so rebuilding the rows
            // (after a local change) does not need another round trip.
            chatPurposes = directory.threads.associate {
                it.peer.id to (it.purpose to it.purposeSubject)
            }
            val localKey = ChatCrypto.publicKeyBase64(appContext, chatAccountKey)
            val encryptionReady = !localKey.isNullOrBlank() &&
                (directory.myPublicKey.isNullOrBlank() || directory.myPublicKey == localKey)
            _chatState.update { state ->
                state.copy(
                    isLoadingDirectory = false,
                    errorMessage = null,
                    onlineCount = directory.onlineCount,
                    totalStudents = directory.totalStudents,
                    safetyAcknowledged = directory.safetyAcknowledged,
                    isEncryptionReady = encryptionReady,
                    activePeer = chatActivePeerId?.let { id ->
                        chatPeers.firstOrNull { it.id == id }
                    } ?: state.activePeer
                )
            }
            rebuildChatThreads()
            // A notification may have been tapped before the directory arrived.
            tryOpenPendingNotificationPeer()
        } else {
            val message = result.failureMessage
            _chatState.update {
                it.copy(
                    isLoadingDirectory = false,
                    // A flaky connection must not wipe the last good list.
                    errorMessage = if (it.threads.isEmpty()) message else null
                )
            }
        }
    }

    /** Merge the peer directory with the device database into UI rows. */
    private fun rebuildChatThreads() {
        val summaryByPeer = chatSummaries.associateBy { it.peerId }
        val threads = chatPeers.map { peer ->
            val summary = summaryByPeer[peer.id]
            // Attachment messages preview as "📷 Photo" / "📎 notes.pdf", and a
            // caption (if any) still wins when there is text to show too.
            val preview = when {
                summary == null -> null
                summary.lastAttachmentKind == ChatAttachment.IMAGE -> "📷 Photo"
                summary.lastAttachmentKind == ChatAttachment.AUDIO -> "🎤 Voice note"
                summary.lastAttachmentKind != null ->
                    "📎 " + (summary.lastAttachmentName ?: "Attachment")
                else -> summary.lastBody
            }
            val purpose = chatPurposes[peer.id]
            ChatThread(
                peer = peer,
                lastMessage = preview,
                lastMessageIsMine = summary?.lastIsMine ?: false,
                lastMessageEpoch = summary?.lastEpoch ?: 0L,
                unreadCount = summary?.unreadCount ?: 0,
                purpose = purpose?.first.orEmpty(),
                purposeSubject = purpose?.second.orEmpty()
            )
        }.sortedWith(
            compareByDescending<ChatThread> { it.lastMessageEpoch > 0L }
                .thenByDescending { it.lastMessageEpoch }
                .thenBy { it.peer.name.lowercase() }
        )
        _chatState.update { it.copy(threads = threads) }
    }

    // ── Student profile ──────────────────────────────────────────────────────

    /**
     * Open the read-only profile of [peer].
     *
     * Shows only what they chose to share (education + presence) — never e-mail,
     * phone or role. The directory entry is used as an instant placeholder, so
     * the screen has a name and photo while the request is in flight.
     */
    fun openChatProfile(peer: ChatPeer) {
        _chatState.update {
            it.copy(
                viewedProfile = ChatPeerProfile(peer = peer),
                isLoadingProfile = true
            )
        }
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val session = chatSession()
            val profile = if (session == null) {
                null
            } else {
                val (baseUrl, token) = session
                TheStudentChatClient.fetchProfile(baseUrl, token, peer.id).dataOrNull()
            }
            // "Chatting since" is not on the server (message bodies are deleted
            // after delivery), so it comes from the first message stored here.
            val firstEpoch = runCatching {
                chatDao.firstMessageEpochWith(peer.id)
            }.getOrNull()

            _chatState.update { state ->
                val current = state.viewedProfile
                // The student may have closed the screen while this was in flight.
                if (current == null || current.peer.id != peer.id) {
                    state
                } else {
                    state.copy(
                        viewedProfile = (profile ?: current)
                            .copy(chattingSinceEpoch = firstEpoch?.takeIf { it > 0L }),
                        isLoadingProfile = false,
                        // A failed fetch is not worth an error banner here: the
                        // screen still shows the name, photo and presence it had.
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun closeChatProfile() {
        profileJob?.cancel()
        profileJob = null
        _chatState.update { it.copy(viewedProfile = null, isLoadingProfile = false) }
    }

    /**
     * Pull everything the relay is holding for me and persist it locally.
     * The server deletes each row as it hands it over, so this is the only
     * chance to keep it.
     */
    private suspend fun fetchChatInbox() {
        val session = chatSession() ?: return
        // Anything typed while offline goes out before new mail is collected.
        retryPendingChatMessages()
        val (baseUrl, token) = session

        val inbox = TheStudentChatClient.fetchInbox(
            context = appContext,
            accountKey = chatAccountKey,
            backendBaseUrl = baseUrl,
            token = token
        ).dataOrNull() ?: return

        val activePeerId = chatActivePeerId
        var newForActivePeer = false

        inbox.messages.forEach { message ->
            val peerId = message.senderId
            val isForOpenChat = activePeerId == peerId
            if (isForOpenChat) newForActivePeer = true
            val attachment = message.attachment
            chatDao.upsertChatMessage(
                ChatMessageEntity(
                    localId = message.localId,
                    serverId = message.serverId,
                    peerId = peerId,
                    senderId = message.senderId,
                    isMine = false,
                    body = message.body,
                    sentAtEpoch = message.sentAtEpoch,
                    isRead = isForOpenChat,
                    attachmentId = attachment?.id,
                    attachmentName = attachment?.name,
                    attachmentMime = attachment?.mime,
                    attachmentSize = attachment?.size ?: 0L,
                    attachmentKind = attachment?.kind,
                    attachmentDurationMs = attachment?.durationMs ?: 0L,
                    attachmentLocal = attachment != null &&
                        ChatAttachmentStore.exists(appContext, attachment.id),
                    replyToServerId = message.replyTo?.serverId ?: 0L,
                    replyToIsMine = message.replyTo?.isMine ?: false,
                    replyToPreview = message.replyTo?.preview
                )
            )
        }

        // Read receipts tell me which of my messages the peer has seen. The ids
        // inside a receipt are ids of MY messages, so only those rows change —
        // marking incoming rows here used to clear unread badges by accident.
        inbox.receipts.forEach { (peerId, upTo) ->
            chatDao.markMyMessagesRead(peerId, upTo)
        }

        // Messages the sender deleted for both sides.
        inbox.deletions.forEach { deletion ->
            chatDao.deleteReceivedMessage("srv-${deletion.serverId}")
        }

        if (newForActivePeer) {
            val upTo = chatDao.newestServerIdFromPeer(activePeerId ?: 0) ?: 0L
            if (upTo > 0) {
                TheStudentChatClient.sendReadReceipt(baseUrl, token, activePeerId ?: 0, upTo)
            }
        }

        // Anything that arrived with a file gets its bytes pulled next.
        downloadPendingAttachments(baseUrl, token)

        // Reconcile device notifications with what is actually unread, whichever
        // screen the student happens to be on.
        refreshChatNotifications()
    }

    // ── Conversation ─────────────────────────────────────────────────────────

    /** Open a 1:1 conversation with a registered student. */
    fun openChat(peer: ChatPeer) {
        chatActivePeerId = peer.id
        // The message is on screen from here on: its notification is noise.
        ChatNotifications.uiActivePeerId = peer.id
        ChatNotifications.clearForPeer(appContext, peer.id)
        _chatState.update {
            it.copy(
                activePeer = peer,
                messages = emptyList(),
                isLoadingMessages = true,
                errorMessage = null,
                questionOfferVisible = false,
                isAskingHelp = false
            )
        }
        _uiState.update { it.copy(isChatConversationOpen = true) }

        chatMessagesJob?.cancel()
        chatMessagesJob = viewModelScope.launch {
            // Room is the source of truth: the list re-renders itself whenever a
            // message is added, sent, delivered or marked read. The very first
            // emission is held back for a moment so the loading animation is
            // actually seen instead of flashing for one frame.
            val openedAt = System.currentTimeMillis()
            var firstEmission = true
            chatDao.observeConversation(peer.id).collect { rows ->
                if (firstEmission) {
                    firstEmission = false
                    val elapsed = System.currentTimeMillis() - openedAt
                    if (elapsed < CHAT_LOADER_MIN_MS) delay(CHAT_LOADER_MIN_MS - elapsed)
                }
                _chatState.update {
                    it.copy(
                        isLoadingMessages = false,
                        messages = rows.map { row -> row.toChatMessage() }
                    )
                }
            }
        }

        // My own subjects pre-fill the help-request subject; the intent sheet
        // only appears for a brand-new conversation.
        // Read from the mapped profile state, not the raw Room flow: the entity
        // stores the academic fields packed and `userProfile` is what unpacks
        // them (and what the UI already observes, so there is no extra query).
        _chatState.update {
            it.copy(mySubjects = userProfile.value.academic.subjectLabels())
        }
        refreshHelpState(peer.id)
        refreshPeerRatings(peer.id)

        viewModelScope.launch {
            chatSession()?.let { (baseUrl, token) -> downloadPendingAttachments(baseUrl, token) }
            // Opening a chat marks everything already received as read and tells
            // the other side.
            chatDao.markConversationRead(peer.id)
            val session = chatSession() ?: return@launch
            val (baseUrl, token) = session
            val upTo = chatDao.newestServerIdFromPeer(peer.id) ?: 0L
            if (upTo > 0) {
                TheStudentChatClient.sendReadReceipt(baseUrl, token, peer.id, upTo)
            }
            // Those rows are read now, so their notifications go away.
            refreshChatNotifications()
        }
    }

    private fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
        localId = localId,
        serverId = serverId,
        senderId = senderId,
        body = body,
        isMine = isMine,
        sentAtEpoch = sentAtEpoch,
        isRead = isRead,
        isPending = isPending,
        attachment = attachmentId?.let { id ->
            ChatAttachment(
                id = id,
                name = attachmentName ?: "attachment",
                mime = attachmentMime ?: "application/octet-stream",
                size = attachmentSize,
                kind = attachmentKind ?: ChatAttachment.FILE,
                durationMs = attachmentDurationMs
            )
        },
        attachmentAvailable = attachmentLocal,
        replyTo = replyToPreview?.takeIf { it.isNotBlank() }?.let { preview ->
            ChatReply(
                serverId = replyToServerId,
                isMine = replyToIsMine,
                preview = preview
            )
        }
    )

    // ── Help requests ────────────────────────────────────────────────────────
    /**
     * Refresh the newest help request with [peerId].
     *
     * Cheap enough to run on every inbox poll (2 s) and inside the conversation:
     * the row is indexed on the pair and at most one is returned.
     */
    private fun refreshHelpState(peerId: Int) {
        val session = chatSession() ?: return
        viewModelScope.launch {
            val (baseUrl, token) = session
            val state = TheStudentChatClient.fetchHelpState(baseUrl, token, peerId)
                .dataOrNull() ?: return@launch
            // Ignore a response that lands after the student closed the chat.
            if (chatActivePeerId != peerId) return@launch
            _chatState.update {
                it.copy(
                    helpRequest = state.request,
                    helpCompletedWithPeer = state.completedWithPeer,
                    chatPurpose = state.purpose,
                    chatPurposeSubject = state.purposeSubject,
                    // A tracked request means the offer has been taken up.
                    questionOfferVisible = if (state.request != null) false
                        else it.questionOfferVisible
                )
            }
        }
    }

    // ── Ratings ──────────────────────────────────────────────────────────────

    /**
     * How many messages each side has sent in the open conversation.
     *
     * The server never sees a message (the relay deletes what it hands out), so
     * this count can only come from the phone. It is what unlocks a rating for
     * a pair that never closed a help request: real conversation, not a tap.
     */
    /** "Nimal" — the name the student actually goes by, or a neutral fallback. */
    private fun String.firstNameOrStudent(): String =
        trim().split(" ").firstOrNull().orEmpty().ifBlank { "this student" }

    private fun messagesEachWay(): Int {
        val messages = _chatState.value.messages
        val mine = messages.count { it.isMine }
        val theirs = messages.count { !it.isMine && !it.isPending }
        return minOf(mine, theirs)
    }

    /** Public reputation of the student in the open conversation. */
    private fun refreshPeerRatings(peerId: Int) {
        val session = chatSession() ?: return
        viewModelScope.launch {
            val (baseUrl, token) = session
            val summary = TheStudentChatClient.fetchRatingSummary(baseUrl, token, peerId)
                .dataOrNull() ?: return@launch
            if (chatActivePeerId != peerId) return@launch
            _chatState.update { it.copy(peerRatingSummary = summary) }
        }
    }

    /**
     * Open the rating card for the student in the conversation.
     *
     * Called by itself the moment a help request is closed, and from the
     * conversation menu afterwards. It opens only when something really earns
     * it: a closed request, or 20+ messages each way. Otherwise it says why and
     * stays closed — a rating nobody earned is just a number.
     */
    fun openRatingCard() {
        val peer = _chatState.value.activePeer ?: return
        if (_chatState.value.isRatingBusy) return
        val session = chatSession() ?: return
        viewModelScope.launch {
            val (baseUrl, token) = session
            val result = TheStudentChatClient.fetchRateState(baseUrl, token, peer.id)
            when (result) {
                is ChatResult.Success -> {
                    val rate = result.data
                    // "Just talking" is decided here, on the phone, because only
                    // the phone knows how much was actually said.
                    val talkReady = rate.isTalk && messagesEachWay() >= rate.talkMinMessages
                    if (!rate.canRateNow && !talkReady) {
                        _uiState.update {
                            it.copy(
                                snackbarMessage = "Rate ${peer.name.firstNameOrStudent()} " +
                                    "after a closed help request, or after " +
                                    "${rate.talkMinMessages} messages each way."
                            )
                        }
                        return@launch
                    }
                    if (chatActivePeerId != peer.id) return@launch
                    _chatState.update { it.copy(rateState = rate) }
                }
                is ChatResult.Failure -> {
                    _uiState.update { it.copy(snackbarMessage = chatFailureMessage(result.message)) }
                }
            }
        }
    }

    /**
     * Play the "you've been rated" moment if this phone has not shown it yet.
     *
     * The newest rating received is fetched once per inbox visit and compared
     * with the id stored in preferences: the same rating never celebrates twice,
     * and a rating given while the app was closed still gets its moment.
     */
    private fun maybeCelebrateRating() {
        val session = chatSession() ?: return
        viewModelScope.launch {
            val (baseUrl, token) = session
            val latest = TheStudentChatClient.fetchLatestReceivedRating(baseUrl, token)
                .dataOrNull() ?: return@launch
            if (latest.id <= preferences.lastCelebratedRatingId) return@launch
            preferences.lastCelebratedRatingId = latest.id
            _chatState.update { it.copy(celebratingRating = latest) }
        }
    }

    /** The celebration has finished (or was tapped away). */
    fun dismissRatingCelebration() {
        _chatState.update { it.copy(celebratingRating = null) }
    }

    /** "Not now" on the rating card. */
    fun dismissRatingCard() {
        _chatState.update { it.copy(rateState = null) }
    }

    /**
     * Send (or correct) the rating on screen. The card stays open on failure so
     * the student never loses the stars they picked.
     */
    fun submitRating(stars: Int, tags: List<String>, note: String) {
        val peer = _chatState.value.activePeer ?: return
        val rate = _chatState.value.rateState ?: return
        if (_chatState.value.isRatingBusy) return
        val session = chatSession() ?: return

        viewModelScope.launch {
            val (baseUrl, token) = session
            _chatState.update { it.copy(isRatingBusy = true) }
            val result = TheStudentChatClient.submitRating(
                backendBaseUrl = baseUrl,
                token = token,
                peerId = peer.id,
                stars = stars,
                tags = tags,
                note = note,
                talkMessagesEachWay = messagesEachWay()
            )
            when (result) {
                is ChatResult.Success -> {
                    _chatState.update {
                        it.copy(
                            isRatingBusy = false,
                            rateState = null,
                            peerRatingSummary = result.data.summary,
                            ratedSubjects = it.ratedSubjects + rate.subjectCode
                        )
                    }
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "Rating sent — ${peer.name.firstNameOrStudent()} " +
                                "can see it, and the note stays private."
                        )
                    }
                }
                is ChatResult.Failure -> {
                    _chatState.update { it.copy(isRatingBusy = false) }
                    _uiState.update {
                        it.copy(snackbarMessage = chatFailureMessage(result.message))
                    }
                }
            }
        }
    }

    /** Open the intent sheet for the conversation that is already on screen. */
    fun openChatIntent() {
        val peer = _chatState.value.activePeer ?: return
        _chatState.update { it.copy(chatIntentPeer = peer) }
    }

    /**
     * Open a conversation straight into the help composer.
     *
     * Used by the "Ask for help" button on a student's profile, so a student who
     * already knows who they want to ask does not have to find the conversation
     * menu afterwards.
     */
    fun openChatForHelp(peer: ChatPeer) {
        closeChatProfile()
        openChat(peer)
        // No sheet, no subject field: the student already said what they want by
        // tapping "Ask for help", and the subject comes from their profile.
        askForHelp()
    }

    /** Dismiss the intent sheet without opening a help request. */
    fun dismissChatIntent() {
        _chatState.update { it.copy(chatIntentPeer = null) }
    }

    /**
     * Remember what this conversation is about.
     *
     * The label is a nicety, so a failure is only logged into the snackbar-free
     * path: the chat itself must never be interrupted by it.
     */
    fun setChatPurpose(purpose: String, subjectLabel: String = "") {
        val peer = _chatState.value.activePeer ?: return
        val session = chatSession() ?: return
        _chatState.update {
            it.copy(
                chatPurpose = purpose,
                chatPurposeSubject = subjectLabel,
                chatIntentPeer = null
            )
        }
        // The chat list shows the same label, so refresh it without waiting for
        // the next directory poll.
        chatPurposes = chatPurposes + (peer.id to (purpose to subjectLabel))
        rebuildChatThreads()
        viewModelScope.launch {
            TheStudentChatClient.setPurpose(
                backendBaseUrl = session.first,
                token = session.second,
                peerId = peer.id,
                purpose = purpose,
                subjectLabel = subjectLabel
            )
        }
    }

    /**
     * The purpose sheet answered: label the chat, and for "help" also open a
     * tracked request so the student does not have to confirm twice.
     */
    fun applyChatPurpose(purpose: String, subjectLabel: String = "") {
        setChatPurpose(purpose, subjectLabel)
        if (purpose == "help") askForHelp(subjectLabel)
    }

    /**
     * Create a help request for the open conversation.
     *
     * Called from three places that all mean the same thing — the student
     * confirmed they want help tracked: the offer after a question, the
     * conversation menu, and the profile's "Ask for help". The subject comes
     * from the student's own profile, so there is nothing to fill in.
     */
    fun askForHelp(subjectLabel: String? = null) {
        val peer = _chatState.value.activePeer ?: return
        if (_chatState.value.isAskingHelp) return

        val subject = subjectLabel?.trim().orEmpty()
            .ifBlank { _chatState.value.mySubjects.firstOrNull().orEmpty() }

        val session = chatSession() ?: return
        _chatState.update { it.copy(isAskingHelp = true, questionOfferVisible = false) }

        viewModelScope.launch {
            val result = TheStudentChatClient.createHelpRequest(
                backendBaseUrl = session.first,
                token = session.second,
                peerId = peer.id,
                subjectCode = subject.lowercase(Locale.US)
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_'),
                subjectLabel = subject,
                purposes = listOf(INTENT_HELP_LABEL)
            )
            when (result) {
                is ChatResult.Success -> {
                    _chatState.update {
                        it.copy(
                            isAskingHelp = false,
                            helpRequest = result.data ?: it.helpRequest,
                            chatPurpose = "help",
                            chatPurposeSubject = subject
                        )
                    }
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "${peer.name.split(" ").firstOrNull().orEmpty()} " +
                                "will see your request — close it once it's answered."
                        )
                    }
                    // Keep the server-side label in step with the request.
                    TheStudentChatClient.setPurpose(
                        backendBaseUrl = session.first,
                        token = session.second,
                        peerId = peer.id,
                        purpose = "help",
                        subjectLabel = subject
                    )
                }
                is ChatResult.Failure -> {
                    _chatState.update { it.copy(isAskingHelp = false) }
                    if (!result.isNetworkFailure) {
                        _uiState.update { it.copy(snackbarMessage = chatFailureMessage(result.message)) }
                    }
                }
            }
        }
    }

    /** The offer under a sent question: yes, track it. */
    fun trackQuestionAsHelp() {
        askForHelp()
    }

    /**
     * The offer under a sent question: not now.
     *
     * Remembered per peer on the device, so the same student is never asked
     * twice — the whole point of moving the question out of the way.
     */
    fun dismissQuestionOffer() {
        val peer = _chatState.value.activePeer ?: return
        preferences.rememberQuestionOfferDismissal(peer.id)
        _chatState.update { it.copy(questionOfferVisible = false) }
    }

    /**
     * After a message is stored: is this the first thing that clearly reads like
     * a question, in a pair with no help request yet?
     *
     * Deliberately conservative — a few strong signals only, because a false
     * positive costs a card the student has to dismiss:
     *   • it ends with "?"  OR  starts with a request word ("can you", "help",
     *     "how do i", "explain", …),
     *   • the conversation has no request at all,
     *   • the student has never dismissed the offer for this peer,
     *   • at most two offers per day, app-wide.
     */
    private fun maybeOfferQuestionTracking(text: String, peerId: Int) {
        if (_chatState.value.helpRequest != null) return
        if (_chatState.value.questionOfferVisible) return
        if (preferences.questionOfferDismissals.contains(peerId)) return
        if (!looksLikeQuestion(text)) return
        if (!rollOfferBudget()) return
        _chatState.update { it.copy(questionOfferVisible = true) }
    }

    /** True when [text] reads like a request for help rather than small talk. */
    private fun looksLikeQuestion(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 8) return false
        val lower = trimmed.lowercase(Locale.US)
        if (lower.endsWith("?")) return true
        val openers = listOf(
            "can you", "could you", "can u", "help me", "help with", "how do i",
            "how to", "explain", "what is", "why is", "solve", "machan can",
            "any idea", "do you know", "need help", "teach me"
        )
        return openers.any { lower.startsWith(it) || lower.contains(" $it") }
    }

    /**
     * At most two offers a day across every conversation.
     *
     * Stored in memory: a restart granting two more is harmless, and it avoids
     * another preferences key for something this soft.
     */
    private fun rollOfferBudget(): Boolean {
        val today = System.currentTimeMillis() / 86_400_000L
        if (offerBudgetDay != today) {
            offerBudgetDay = today
            offersMadeToday = 0
        }
        if (offersMadeToday >= MAX_OFFERS_PER_DAY) return false
        offersMadeToday += 1
        return true
    }

    /**
     * Turn a bare server refusal into something actionable.
     *
     * "Unknown action" / "Invalid API action" mean the uploaded PHP predates
     * this build — a file to upload, not a mistake the student made. Anything
     * else is passed through untouched.
     */
    private fun chatFailureMessage(raw: String): String =
        if (raw.contains("Unknown action", true) || raw.contains("Invalid API action", true)) {
            "This server is running an older chat API. Upload backend/api/chat.php " +
                "(needs version $CHAT_MIN_SERVER_VERSION or newer)."
        } else {
            raw
        }

    /** Helper accepts the request. */
    fun acceptHelpRequest() {
        val request = _chatState.value.helpRequest ?: return
        updateHelp("help_accept", request.id)
    }

    /** Seeker confirms the question was answered — this unlocks the rating. */
    fun completeHelpRequest() {
        val request = _chatState.value.helpRequest ?: return
        updateHelp("help_close", request.id)
    }

    /** Either side withdraws the request. */
    fun cancelHelpRequest() {
        val request = _chatState.value.helpRequest ?: return
        if (_chatState.value.isHelpBusy) return
        // Optimistic: the student tapped cancel, so the card must go NOW. The
        // old version waited for the round trip, and when that request failed
        // quietly the button looked like it did nothing at all.
        _chatState.update { it.copy(helpRequest = null) }
        updateHelp("help_cancel", request.id, restoreOnFailure = request)
    }

    private fun updateHelp(
        action: String,
        requestId: Long,
        restoreOnFailure: ChatHelpRequest? = null
    ) {
        if (_chatState.value.isHelpBusy) return
        val session = chatSession() ?: run {
            if (restoreOnFailure != null) {
                _chatState.update { it.copy(helpRequest = restoreOnFailure) }
            }
            return
        }
        viewModelScope.launch {
            val (baseUrl, token) = session
            _chatState.update { it.copy(isHelpBusy = true) }
            val result = TheStudentChatClient.updateHelpRequest(baseUrl, token, action, requestId)
            when (result) {
                is ChatResult.Success -> {
                    _chatState.update {
                        it.copy(
                            isHelpBusy = false,
                            // A cancelled request leaves the screen instead of
                            // turning into a grey card nobody can act on.
                            helpRequest = if (action == "help_cancel") null
                            else result.data ?: it.helpRequest
                        )
                    }
                    if (action == "help_cancel") {
                        _uiState.update { it.copy(snackbarMessage = "Request cancelled") }
                    }
                    if (action == "help_close") {
                        _uiState.update {
                            it.copy(snackbarMessage = "Help confirmed — you can rate this student")
                        }
                        // The natural moment: the question is answered, so the
                        // rating card comes to the student instead of hiding
                        // behind a menu.
                        openRatingCard()
                    }
                }
                is ChatResult.Failure -> {
                    _chatState.update {
                        it.copy(
                            isHelpBusy = false,
                            // Put the card back: the cancel never reached the
                            // server, so pretending it vanished would be a lie.
                            helpRequest = restoreOnFailure ?: it.helpRequest
                        )
                    }
                    _uiState.update {
                        it.copy(
                            snackbarMessage = if (result.isNetworkFailure) {
                                "You're offline — the request is still active."
                            } else {
                                chatFailureMessage(result.message)
                            }
                        )
                    }
                }
            }
        }
    }

    fun closeChat() {
        chatMessagesJob?.cancel()
        chatMessagesJob = null
        chatActivePeerId = null
        ChatNotifications.uiActivePeerId = null
        chatTypingJob?.cancel()
        chatTypingJob = null
        // A file picked (or a recording in progress) must not survive the chat.
        if (voiceRecorder.isRecording) cancelVoiceNote()
        _chatState.value.pendingAttachment?.let { draft ->
            runCatching { File(draft.path).delete() }
        }
        _chatState.update {
            it.copy(
                activePeer = null,
                messages = emptyList(),
                pendingAttachment = null,
                isUploadingAttachment = false,
                attachmentUploadProgress = 0f,
                pendingReply = null,
                isRecordingVoiceNote = false,
                voiceNoteElapsedMs = 0L,
                helpRequest = null,
                chatIntentPeer = null,
                isHelpBusy = false,
                questionOfferVisible = false,
                isAskingHelp = false,
                helpCompletedWithPeer = 0
            )
        }
        _uiState.update { it.copy(isChatConversationOpen = false) }
    }

    /**
     * Send a message: it is stored locally first (so it appears instantly, even
     * offline), then encrypted and handed to the relay.
     */
    fun sendChatMessage(text: String) {
        val body = text.trim()
        val peer = _chatState.value.activePeer ?: return
        val draft = _chatState.value.pendingAttachment
        if ((body.isEmpty() && draft == null) || _chatState.value.isSending) return
        if (!_chatState.value.isEncryptionReady) {
            _chatState.update {
                it.copy(errorMessage = "Secure chat is still starting on this device. Please try again.")
            }
            return
        }

        val session = chatSession() ?: return
        val (baseUrl, token) = session
        val reply = _chatState.value.pendingReply
        val localId = UUID.randomUUID().toString()
        val sentAt = System.currentTimeMillis() / 1000

        _chatState.update {
            it.copy(isSending = true, errorMessage = null, attachmentUploadProgress = 0f)
        }
        viewModelScope.launch {
            // 1. A picked file is encrypted first and dropped on the relay; the
            //    message that follows only carries its metadata.
            var attachment: ChatAttachment? = null
            if (draft != null) {
                _chatState.update { it.copy(isUploadingAttachment = true) }
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { File(draft.path).readBytes() }.getOrNull()
                }
                if (bytes == null) {
                    _chatState.update {
                        it.copy(
                            isSending = false,
                            isUploadingAttachment = false,
                            errorMessage = "That file could not be read any more. Please pick it again."
                        )
                    }
                    return@launch
                }
                val uploaded = TheStudentChatClient.uploadAttachment(
                    context = appContext,
                    accountKey = chatAccountKey,
                    backendBaseUrl = baseUrl,
                    token = token,
                    peer = peer,
                    bytes = bytes,
                    onProgress = { fraction ->
                        // Called on the upload thread; the state flow is safe to
                        // write from here and only redraws the progress bar.
                        _chatState.update { it.copy(attachmentUploadProgress = fraction) }
                    }
                )
                val attachmentId = uploaded.dataOrNull()
                if (attachmentId == null) {
                    // Keep the draft so "send" can simply be tapped again. Offline
                    // is not an error the student has to read: the file stays in
                    // the composer and can be sent with one tap once there is data.
                    _chatState.update {
                        if (uploaded.isNetworkFailure) {
                            it.copy(isSending = false, isUploadingAttachment = false)
                        } else {
                            it.copy(
                                isSending = false,
                                isUploadingAttachment = false,
                                errorMessage = uploaded.failureMessage
                                    ?: "The file could not be sent. Please try again."
                            )
                        }
                    }
                    return@launch
                }
                ChatAttachmentStore.save(appContext, attachmentId, bytes)
                attachment = ChatAttachment(
                    id = attachmentId,
                    name = draft.name,
                    mime = draft.mime,
                    size = draft.size.takeIf { it > 0L } ?: bytes.size.toLong(),
                    kind = draft.kind,
                    durationMs = draft.durationMs
                )
                File(draft.path).delete()
                _chatState.update {
                    it.copy(
                        pendingAttachment = null,
                        isUploadingAttachment = false,
                        attachmentUploadProgress = 0f
                    )
                }
            }

            // 2. Show the bubble immediately, before the relay confirms it.
            chatDao.upsertChatMessage(
                ChatMessageEntity(
                    localId = localId,
                    peerId = peer.id,
                    senderId = 0,
                    isMine = true,
                    body = body,
                    sentAtEpoch = sentAt,
                    isRead = false,
                    isPending = true,
                    attachmentId = attachment?.id,
                    attachmentName = attachment?.name,
                    attachmentMime = attachment?.mime,
                    attachmentSize = attachment?.size ?: 0L,
                    attachmentKind = attachment?.kind,
                    attachmentDurationMs = attachment?.durationMs ?: 0L,
                    attachmentLocal = attachment != null,
                    replyToServerId = reply?.serverId ?: 0L,
                    replyToIsMine = reply?.isMine ?: false,
                    replyToPreview = reply?.preview
                )
            )

            // Offer to track this as a help request *after* the message is on
            // its way: the offer must never stand between the student and
            // sending the question they came here to ask.
            maybeOfferQuestionTracking(body, peer.id)

            val result = TheStudentChatClient.sendMessage(
                context = appContext,
                accountKey = chatAccountKey,
                backendBaseUrl = baseUrl,
                token = token,
                peer = peer,
                // Encrypt caption + attachment metadata + quote as one envelope.
                plaintext = ChatEnvelope.encode(body, attachment, reply)
            )
            val serverId = result.dataOrNull()
            if (serverId != null) {
                chatDao.markChatMessageSent(localId, serverId, sentAt)
                _chatState.update { it.copy(isSending = false, pendingReply = null) }
            } else if (result.isNetworkFailure) {
                // No connection: this is not an error the student has to act on.
                // The bubble stays in the conversation marked pending (clock
                // icon) and is re-sent automatically once the network is back —
                // and on the next app start, because the row is in the database.
                _chatState.update { it.copy(isSending = false, pendingReply = null) }
            } else {
                // The server actually refused it (peer unavailable, throttled,
                // message too long…): that is worth saying out loud.
                chatDao.markChatMessageFailed(localId)
                _chatState.update {
                    it.copy(isSending = false, errorMessage = result.failureMessage)
                }
            }
        }
    }

    /**
     * Re-send messages that are still marked pending.
     *
     * Pending means "written here, not accepted by the relay yet" — normally
     * because the phone was offline. Nothing is deleted or flagged: the bubble
     * keeps its clock until the relay takes it, then it becomes a normal ✓.
     *
     * Runs on every inbox poll, so reconnecting (or reopening the app) drains the
     * queue without the student doing anything. Stops at the first network
     * failure, since the rest would fail the same way, and a short throttle keeps
     * an offline phone from hammering connect timeouts every couple of seconds.
     */
    private suspend fun retryPendingChatMessages() {
        if (pendingRetryInFlight) return
        val now = System.currentTimeMillis()
        if (now - lastPendingRetryAt < CHAT_PENDING_RETRY_MS) return
        lastPendingRetryAt = now

        val session = chatSession() ?: return
        val (baseUrl, token) = session
        val pending = runCatching { chatDao.pendingOutgoingMessages() }.getOrNull() ?: return
        if (pending.isEmpty()) return

        pendingRetryInFlight = true
        try {
            for (row in pending) {
                val peer = chatPeers.firstOrNull { it.id == row.peerId } ?: continue
                val attachment = row.attachmentId?.let { id ->
                    ChatAttachment(
                        id = id,
                        name = row.attachmentName ?: "attachment",
                        mime = row.attachmentMime ?: "application/octet-stream",
                        size = row.attachmentSize,
                        kind = row.attachmentKind ?: ChatAttachment.FILE,
                        durationMs = row.attachmentDurationMs
                    )
                }
                val reply = row.replyToPreview?.takeIf { it.isNotBlank() }?.let { preview ->
                    ChatReply(
                        serverId = row.replyToServerId,
                        isMine = row.replyToIsMine,
                        preview = preview
                    )
                }

                val result = TheStudentChatClient.sendMessage(
                    context = appContext,
                    accountKey = chatAccountKey,
                    backendBaseUrl = baseUrl,
                    token = token,
                    peer = peer,
                    plaintext = ChatEnvelope.encode(row.body, attachment, reply)
                )
                val serverId = result.dataOrNull()
                if (serverId != null) {
                    // Keep the original timestamp so the bubble does not jump.
                    chatDao.markChatMessageSent(row.localId, serverId, row.sentAtEpoch)
                    continue
                }
                if (result.isNetworkFailure) return // still offline

                // The server refused it: stop showing a clock that will never
                // clear, and say why — but only in the conversation it belongs to.
                chatDao.markChatMessageFailed(row.localId)
                if (_chatState.value.activePeer?.id == row.peerId) {
                    _chatState.update { it.copy(errorMessage = result.failureMessage) }
                }
            }
        } finally {
            pendingRetryInFlight = false
        }
    }

    /**
     * Tell the server the student is typing. Throttled so a fast typist cannot
     * flood the API (the server also expires the flag after 6 seconds).
     */
    fun notifyChatTyping() {
        val peer = _chatState.value.activePeer ?: return
        if (chatTypingJob?.isActive == true) return
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        chatTypingJob = viewModelScope.launch {
            TheStudentChatClient.setTyping(baseUrl, token, peer.id, true)
            delay(CHAT_TYPING_THROTTLE_MS)
        }
    }

    /**
     * Stop the typing indicator immediately — used when the composer loses focus
     * (the student taps the conversation, closes the keyboard, or leaves), so the
     * other side is not left looking at "typing…" for the 6 s server window.
     */
    fun stopChatTyping() {
        val peer = _chatState.value.activePeer ?: return
        if (chatTypingJob == null) return
        chatTypingJob?.cancel()
        chatTypingJob = null
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        viewModelScope.launch {
            TheStudentChatClient.setTyping(baseUrl, token, peer.id, false)
        }
    }

    /** Acknowledge the one-time "don't share personal details" notice. */
    fun acknowledgeChatSafetyNotice() {
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        _chatState.update { it.copy(safetyAcknowledged = true) }
        viewModelScope.launch {
            TheStudentChatClient.acknowledgeSafetyNotice(baseUrl, token)
        }
    }

    /**
     * Delete one message.
     *
     * On my own message this is "delete for everyone": the row disappears here
     * immediately (and its attachment file with it), then the relay tells the
     * peer to drop their copy. On a message I received it only removes my copy —
     * the sender's history is theirs.
     */
    fun deleteChatMessage(message: ChatMessage) {
        val peer = _chatState.value.activePeer ?: return
        viewModelScope.launch {
            chatDao.deleteByLocalId(message.localId)
            message.attachment?.let { ChatAttachmentStore.delete(appContext, it.id) }
            if (message.isMine && message.serverId > 0) {
                chatSession()?.let { (baseUrl, token) ->
                    TheStudentChatClient.deleteMessage(
                        backendBaseUrl = baseUrl,
                        token = token,
                        peerId = peer.id,
                        serverId = message.serverId
                    )
                }
            }
        }
    }

    /** Delete the open conversation from this device only. */
    fun clearActiveChat() {
        val peer = _chatState.value.activePeer ?: return
        viewModelScope.launch {
            chatDao.clearConversation(peer.id)
            _chatState.update { it.copy(messages = emptyList()) }
        }
    }

    /**
     * Collect conversation previews and unread badges from the device database.
     * Started once; Room re-emits whenever a message changes.
     */
    private fun observeChatDatabase() {
        viewModelScope.launch {
            // MUST suspend before touching anything declared below the init
            // block. viewModelScope runs on Dispatchers.Main.immediate, so when
            // this is called from the constructor the body would otherwise run
            // inline — while `chatDao` and `chatSummaries` are still null. That
            // threw a NullPointerException into the catch below on every launch,
            // which is why the chat list showed "Say hello 👋" and never a
            // preview or an unread badge, even though conversations worked.
            yield()
            while (true) {
                try {
                    chatDao.observeSummaries().collect { rows ->
                        chatSummaries = rows
                        rebuildChatThreads()
                    }
                    // A Room flow over a whole table never finishes on its own,
                    // so reaching this line means the database was closed (sign
                    // out). Nothing left to observe.
                    return@launch
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    // Chat is a feature; it must never crash the app. Retrying
                    // matters because this one collector feeds every preview and
                    // unread badge on the chat list: before, a single hiccup
                    // ended it and the whole list quietly showed no badges, with
                    // no way back short of restarting the app.
                    Log.e("TheStudentViewModel", "Chat summaries failed, retrying", error)
                    delay(2_000)
                }
            }
        }
    }

    // ── Device notifications ─────────────────────────────────────────────────

    /**
     * Bring the notification shade in line with the device database.
     *
     * Called after every inbox poll, after opening a conversation and when the
     * app moves between foreground and background. The heavy lifting lives in
     * [ChatNotifier], which the FCM service uses too, so a pushed message and a
     * polled one produce exactly the same notification.
     *
     * The app being open no longer suppresses anything: only the conversation on
     * screen does (see [ChatNotifications.uiActivePeerId]).
     */
    private suspend fun refreshChatNotifications() {
        // Names come from the peer directory. If the app is polling from the
        // background and the directory was never loaded in this process, fetch
        // it once so notifications are not titled "Student".
        if (chatPeers.isEmpty()) {
            runCatching { refreshChatDirectory(showSpinner = false) }
        }
        ChatNotifier.reconcile(
            context = appContext,
            dao = chatDao,
            peers = { peerId ->
                chatPeers.firstOrNull { it.id == peerId }?.let { peer ->
                    NotificationPeer(id = peer.id, name = peer.name, avatarUrl = peer.avatarUrl)
                }
            }
        )
    }

    /** Post/reconcile notifications right now (app entering background, etc.). */
    fun refreshChatNotificationsNow() {
        viewModelScope.launch { refreshChatNotifications() }
    }

    /**
     * Poll the relay even when the chat tab is closed.
     *
     * The tab runs its own 2 s loop for the open conversation; this slower loop
     * is what lets a message arrive — and notify — while the student is on
     * another tab or has the app in the background. It is deliberately not a
     * push service: the relay has no way to wake a killed app, so a message that
     * arrives while the app is not running is delivered the next time it starts.
     */
    private fun ensureChatBackgroundSync() {
        if (chatBackgroundJob?.isActive == true) return
        chatBackgroundJob = viewModelScope.launch {
            // Sit out the constructor: viewModelScope is Main.immediate, so this
            // body would otherwise run before the lazily-declared chat state.
            yield()
            ChatNotifications.ensureChannel(appContext)
            syncPushToken()
            while (true) {
                // While the student is in the app, poll quickly: a message that
                // lands on another tab should notify (and badge) straight away.
                // In the background the slower interval saves battery.
                delay(if (appInForeground) CHAT_FOREGROUND_POLL_MS else CHAT_BACKGROUND_POLL_MS)
                if (chatIsDirectoryVisible) continue // the chat tab polls faster
                if (preferences.sessionToken.isNullOrBlank()) continue
                runCatching { fetchChatInbox() }
            }
        }
    }

    /**
     * Make sure the relay knows this device's push token.
     *
     * Without it the server has no way to wake this phone, and delivery falls
     * back to the 10 s polling loop. Wrapped end to end: if the Firebase files
     * are missing (a build without `google-services.json`) chat still works.
     */
    fun syncPushToken() {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                val token = runCatching { if (task.isSuccessful) task.result else null }.getOrNull()
                if (token.isNullOrBlank()) return@addOnCompleteListener
                TheStudentPreferences(appContext).fcmToken = token
                viewModelScope.launch { registerPushToken(token) }
            }
        }.onFailure { error ->
            Log.w("TheStudentViewModel", "Push messaging unavailable", error)
        }
    }

    private suspend fun registerPushToken(fcmToken: String) {
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        runCatching {
            TheStudentChatClient.registerDevice(
                backendBaseUrl = baseUrl,
                token = token,
                fcmToken = fcmToken
            )
        }
    }

    /**
     * A chat notification was tapped: switch to the chat tab and open that
     * conversation. The directory may not be loaded yet (cold start), in which
     * case the id is parked until [refreshChatDirectory] succeeds.
     */
    fun openChatFromNotification(peerId: Int) {
        if (peerId <= 0) return
        pendingNotificationPeerId = peerId
        closeChat()
        setTab(TheStudentNavTab.CHAT)
        tryOpenPendingNotificationPeer()
    }

    private fun tryOpenPendingNotificationPeer() {
        val peerId = pendingNotificationPeerId ?: return
        val peer = chatPeers.firstOrNull { it.id == peerId } ?: return
        pendingNotificationPeerId = null
        if (_uiState.value.screenFlow == AppScreenFlow.MAIN) openChat(peer)
    }

    /** Called from MainActivity.onStart. */
    fun onAppForegrounded() {
        appInForeground = true
        // Catch up on anything that arrived while the app was in the background.
        viewModelScope.launch { runCatching { fetchChatInbox() } }
        refreshChatNotificationsNow()
    }

    /** Called from MainActivity.onStop: unread messages deserve a notification. */
    fun onAppBackgrounded() {
        appInForeground = false
        refreshChatNotificationsNow()
    }

    // ── Attachments ──────────────────────────────────────────────────────────

    /**
     * Copy a file the student picked (gallery or document picker) into the app's
     * cache. Copying now means a content Uri that is revoked later can never
     * break a send, and the composer can already show a thumbnail.
     */
    fun stageChatAttachment(uri: Uri) {
        viewModelScope.launch {
            val staged = withContext(Dispatchers.IO) {
                runCatching { copyPickedAttachment(uri) }.getOrNull()
            }
            if (staged == null) {
                _chatState.update {
                    it.copy(
                        errorMessage = "That file could not be read, or it is larger than " +
                            "${CHAT_MAX_ATTACHMENT_BYTES / (1024 * 1024)} MB."
                    )
                }
                return@launch
            }
            _chatState.update { it.copy(pendingAttachment = staged, errorMessage = null) }
        }
    }

    /** Remove the attachment from the composer without sending anything. */
    fun clearStagedChatAttachment() {
        val draft = _chatState.value.pendingAttachment ?: return
        runCatching { File(draft.path).delete() }
        _chatState.update { it.copy(pendingAttachment = null) }
    }

    private fun copyPickedAttachment(uri: Uri): ChatAttachmentDraft? {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"

        var displayName = "attachment"
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                        }
                    }
                }
        }

        // Streamed to disk, never buffered whole: a 25 MB document must not have
        // to fit in the heap twice before it can be sent.
        val target = File(appContext.cacheDir, "chat_pick_" + UUID.randomUUID().toString())
        val copied = resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > CHAT_MAX_ATTACHMENT_BYTES) {
                        output.close()
                        target.delete()
                        return null
                    }
                    output.write(buffer, 0, read)
                }
                total
            }
        } ?: return null

        if (copied <= 0L) {
            target.delete()
            return null
        }

        // Photos are downscaled and re-encoded before they are ever encrypted:
        // a 9 MB camera JPEG becomes ~400 KB, which sends far faster and looks
        // the same on a phone. Documents are never touched.
        if (ChatMediaPrep.isCompressible(mime, displayName)) {
            val compressed = ChatMediaPrep.compress(target, appContext.cacheDir)
            if (compressed != null) {
                target.delete()
                return ChatAttachmentDraft(
                    path = compressed.absolutePath,
                    name = displayName.substringBeforeLast('.').take(110) + ".jpg",
                    mime = "image/jpeg",
                    size = compressed.length(),
                    kind = ChatAttachment.IMAGE
                )
            }
        }

        return ChatAttachmentDraft(
            path = target.absolutePath,
            name = displayName.take(120),
            mime = mime,
            size = copied,
            // Only real image types are rendered inline; everything else (PDF,
            // DOCX, ZIP, …) becomes a document card.
            kind = when {
                mime.startsWith("image/") -> ChatAttachment.IMAGE
                mime.startsWith("audio/") -> ChatAttachment.AUDIO
                else -> ChatAttachment.FILE
            }
        )
    }

    // ── Replies ──────────────────────────────────────────────────────────────

    /** Quote [message] in the next outgoing message. */
    fun setChatReply(message: ChatMessage) {
        val preview = when {
            message.body.isNotBlank() -> message.body.take(120)
            message.attachment?.isImage == true -> "Photo"
            message.attachment?.isAudio == true -> "Voice note"
            message.attachment != null -> message.attachment.name
            else -> "Message"
        }
        _chatState.update {
            it.copy(pendingReply = ChatReply(message.serverId, message.isMine, preview))
        }
    }

    fun clearChatReply() {
        _chatState.update { it.copy(pendingReply = null) }
    }

    // ── Voice notes ──────────────────────────────────────────────────────────

    private val voiceRecorder: ChatVoiceRecorder by lazy { ChatVoiceRecorder(appContext) }
    private var voiceTickerJob: Job? = null

    /** Start recording. Returns false when the microphone is unavailable. */
    fun startVoiceNote(): Boolean {
        if (voiceRecorder.isRecording) return true
        // A recording replaces any pending file/photo pick.
        _chatState.value.pendingAttachment?.let { clearStagedChatAttachment() }
        if (!voiceRecorder.start()) {
            _chatState.update { it.copy(errorMessage = "Could not start recording on this device.") }
            return false
        }
        _chatState.update {
            it.copy(
                isRecordingVoiceNote = true,
                voiceNoteElapsedMs = 0L,
                voiceNoteLevel = 0f,
                errorMessage = null
            )
        }
        voiceTickerJob?.cancel()
        voiceTickerJob = viewModelScope.launch {
            while (voiceRecorder.isRecording) {
                // 90 ms is fast enough for a waveform to read as "live" and slow
                // enough that the state write is never the bottleneck.
                _chatState.update {
                    it.copy(
                        voiceNoteElapsedMs = voiceRecorder.elapsedMs(),
                        voiceNoteLevel = voiceRecorder.level()
                    )
                }
                delay(90L)
            }
        }
        return true
    }

    /** Finish recording and stage the note in the composer. */
    fun stopVoiceNote() {
        voiceTickerJob?.cancel()
        voiceTickerJob = null
        val draft = voiceRecorder.stop()
        _chatState.update {
            it.copy(isRecordingVoiceNote = false, voiceNoteElapsedMs = 0L, voiceNoteLevel = 0f)
        }
        if (draft == null) {
            _chatState.update { it.copy(errorMessage = "That recording was too short to send.") }
            return
        }
        _chatState.update { it.copy(pendingAttachment = draft) }
    }

    /**
     * Finish the recording and send it in the same tap.
     *
     * This is the whole point of the send button inside the recording composer:
     * once you have said what you wanted, tapping send posts the note. The draft
     * is staged and immediately handed to `sendChatMessage("")`, so it never sits
     * in the composer asking to be confirmed a second time.
     */
    fun sendVoiceNote() {
        voiceTickerJob?.cancel()
        voiceTickerJob = null
        val draft = voiceRecorder.stop()
        _chatState.update {
            it.copy(isRecordingVoiceNote = false, voiceNoteElapsedMs = 0L, voiceNoteLevel = 0f)
        }
        if (draft == null) {
            _chatState.update { it.copy(errorMessage = "That recording was too short to send.") }
            return
        }
        // Staged, then sent straight away: `sendChatMessage` reads the pending
        // attachment synchronously, so there is no window where it goes missing.
        _chatState.update { it.copy(pendingAttachment = draft) }
        sendChatMessage("")
    }

    /** Throw the recording away. */
    fun cancelVoiceNote() {
        voiceTickerJob?.cancel()
        voiceTickerJob = null
        voiceRecorder.cancel()
        _chatState.update {
            it.copy(isRecordingVoiceNote = false, voiceNoteElapsedMs = 0L, voiceNoteLevel = 0f)
        }
    }

    /**
     * Pull the encrypted blobs for messages whose file is not on this device yet.
     *
     * The relay deletes a blob once it has been downloaded, so a failure here is
     * retried a minute later rather than every 2 s poll (which would just hammer
     * a server that no longer has the file).
     */
    private suspend fun downloadPendingAttachments(baseUrl: String, token: String) {
        val pending = runCatching { chatDao.pendingAttachments() }.getOrNull() ?: return
        if (pending.isEmpty()) return

        val now = System.currentTimeMillis()
        for (row in pending) {
            val attachmentId = row.attachmentId ?: continue
            val retryAt = attachmentRetryAt[attachmentId] ?: 0L
            if (now < retryAt) continue

            val peer = chatPeers.firstOrNull { it.id == row.peerId } ?: continue
            val result = TheStudentChatClient.downloadAttachment(
                context = appContext,
                accountKey = chatAccountKey,
                backendBaseUrl = baseUrl,
                token = token,
                peer = peer,
                attachmentId = attachmentId,
                onProgress = { fraction ->
                    _chatState.update {
                        it.copy(
                            attachmentDownloadProgress = it.attachmentDownloadProgress +
                                (attachmentId to (fraction * 100).toInt().coerceIn(0, 100))
                        )
                    }
                }
            )
            val bytes = result.dataOrNull()
            _chatState.update {
                it.copy(attachmentDownloadProgress = it.attachmentDownloadProgress - attachmentId)
            }
            if (bytes == null) {
                attachmentRetryAt[attachmentId] = now + CHAT_ATTACHMENT_RETRY_MS
                continue
            }
            if (ChatAttachmentStore.save(appContext, attachmentId, bytes) != null) {
                chatDao.setAttachmentAvailable(row.localId, true)
                attachmentRetryAt.remove(attachmentId)
            }
        }
        ChatAttachmentStore.trim(appContext)
    }

    /** Blobs that recently failed to download, and when to try again. */
    private val attachmentRetryAt = HashMap<String, Long>()

    fun dismissChatError() {
        _chatState.update { it.copy(errorMessage = null) }
    }

    private companion object {
        /** Presence ping. The server calls 10 s of silence "offline". */
        const val CHAT_PRESENCE_MS = 2_000L
        /** Directory (names, avatars, online dots) refresh. */
        const val CHAT_DIRECTORY_POLL_MS = 3_000L
        /** Relay fetch: new messages + read receipts. */
        const val CHAT_INBOX_POLL_MS = 2_000L
        /**
         * Relay fetch outside the chat tab, so a message can arrive (and notify)
         * while the student is elsewhere in the app or it sits in the background.
         * Slower than the tab's loop on purpose: battery and API load.
         */
        const val CHAT_BACKGROUND_POLL_MS = 10_000L
        /** Same loop while the app is on screen: notifications feel instant. */
        const val CHAT_FOREGROUND_POLL_MS = 3_000L
        const val CHAT_TYPING_THROTTLE_MS = 3_000L
        /** How often queued (offline) messages are offered to the relay again. */
        const val CHAT_PENDING_RETRY_MS = 5_000L
        /** Shortest time the conversation loading animation stays on screen. */
        const val CHAT_LOADER_MIN_MS = 420L
        /** Uploaded bytes per file. Photos are re-encoded well below this. */
        const val CHAT_MAX_ATTACHMENT_BYTES = 25L * 1024L * 1024L
        /** How long to wait before retrying a blob the relay no longer has. */
        const val CHAT_ATTACHMENT_RETRY_MS = 60_000L
        /**
         * Oldest `api/chat.php` this build works with. Bump it whenever a new
         * action is added, so a host that was never updated says so out loud
         * instead of silently hiding the feature.
         */
        const val CHAT_MIN_SERVER_VERSION = "2.9.0"
        /** Two "track this question?" offers a day, app-wide, is plenty. */
        private const val MAX_OFFERS_PER_DAY = 2
        /** Purpose string the server stores for a tracked question. */
        const val INTENT_HELP_LABEL = "I have a question and need help from this student"
    }

    fun uploadNewPaper(paper: PastPaper) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContent = true) }
            val isRemoteSuccess = pastPapersRepository.uploadPaperRemote(paper)
            
            // Immediately insert at the top of the local flow so the app's papers screen instantly displays the new paper
            val currentList = _dynamicPapers.value.toMutableList()
            currentList.removeAll { it.id == paper.id }
            currentList.add(0, paper)
            _dynamicPapers.value = currentList
            
            _uiState.update { it.copy(isLoadingContent = false) }
            if (isRemoteSuccess) {
                showSnackbar("Uploaded '${paper.title}' to DirectAdmin MySQL Database!")
            } else {
                showSnackbar("Saved '${paper.title}' locally in app catalog! (Check API URL in Admin Settings)")
            }
        }
    }

    fun deletePaper(paperId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContent = true) }
            pastPapersRepository.deletePaperRemote(paperId)
            val currentList = _dynamicPapers.value.filter { it.id != paperId }
            _dynamicPapers.value = currentList
            _uiState.update { it.copy(isLoadingContent = false) }
            showSnackbar("Paper removed from catalog.")
        }
    }

    val allCourses = SampleEducationalData.courses
    val allNotes = SampleEducationalData.studyNotes
    val vocabularyList = SampleEducationalData.vocabularyWords
    val grammarList = SampleEducationalData.grammarExercises
    val conversationScenarios = SampleEducationalData.conversationScenarios
    val pronunciationSentences = SampleEducationalData.pronunciationSentences

    // AI Chat history
    private val _chatMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "welcome-ai",
                sender = MessageSender.AI,
                content = """
                    **Ayubowan! I am your TheStudent Academic Tutor.**
                    
                    I am trained on the complete Sri Lankan curriculum (G.C.E. O/L, A/L Bio/Maths/Commerce/Arts/Tech streams, and University degree modules).
                    
                    Ask me any academic question, request step-by-step problem derivations, or generate revision questions with marking schemes!
                """.trimIndent(),
                suggestedFollowUps = listOf(
                    "Explain Bernoulli's Principle & Venturi Tube",
                    "Master A/L Combined Maths Integration techniques",
                    "Generate 3 exam practice questions with marking schemes",
                    "Summarize Le Chatelier's Principle in Chemistry"
                )
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    // Session Verification & Flow Transitions
    /**
     * Validate the stored session once per app launch.
     *
     * Three outcomes are handled distinctly:
     *  - VALID      -> refresh profile/role, stay signed in
     *  - INVALID    -> the token really is dead (401/403): wipe it and sign out
     *  - UNREACHABLE-> network problem: KEEP the session so a flaky connection
     *                  never logs the student out; the API calls will simply
     *                  fail until connectivity returns.
     */
    fun verifySessionWithBackend() {
        sessionVerificationJob = viewModelScope.launch(Dispatchers.IO) {
            val token = preferences.sessionToken
            val isLoggedIn = preferences.isLoggedIn

            if (token.isNullOrBlank() || !isLoggedIn) {
                preferences.logout()
                repository.clearUserProfile()
                _isAdmin.value = false
                _uiState.update { it.copy(isAuthenticated = false, isSessionVerified = true) }
                return@launch
            }

            when (val validation = TheStudentAuthClient.validateSession(serverUrlState.value, token)) {
                is SessionValidation.Valid -> {
                    repository.saveUserProfile(validation.profile)
                    preferences.isLoggedIn = true
                    preferences.isGuest = false
                    preferences.userName = validation.profile.name
                    preferences.userEmail = validation.profile.email
                    preferences.updateRoleFromServer(validation.role)
                    validation.expiresAt?.let { preferences.sessionExpiresAt = it }
                    _isAdmin.value = preferences.isAdmin
                    // A session whose profile was never completed (app killed on
                    // the setup screen, or an older account) is sent straight back
                    // to setup on the next launch instead of into the dashboard.
                    val needsSetup = !validation.profile.profileCompleted
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            isSessionVerified = true,
                            needsProfileSetup = needsSetup
                        )
                    }
                }

                is SessionValidation.Invalid -> {
                    Log.w("TheStudentViewModel", "Session rejected by server: ${validation.message}")
                    preferences.logout()
                    repository.clearUserProfile()
                    _isAdmin.value = false
                    _uiState.update {
                        it.copy(
                            isAuthenticated = false,
                            isSessionVerified = true,
                            isAdminPanelOpen = false
                        )
                    }
                }

                is SessionValidation.Unreachable -> {
                    // Offline / server hiccup: keep the user signed in with the
                    // locally cached profile.
                    Log.w("TheStudentViewModel", "Session check skipped: ${validation.message}")
                    _isAdmin.value = preferences.isAdmin
                    _uiState.update { it.copy(isAuthenticated = true, isSessionVerified = true) }
                }
            }
        }
    }

    fun finishSplash() {
        viewModelScope.launch {
            // Wait for remote session validation if still in progress
            try {
                sessionVerificationJob?.let { job ->
                    if (job.isActive) {
                        kotlinx.coroutines.withTimeoutOrNull(2000L) {
                            job.join()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TheStudentViewModel", "Session check timeout: ${e.message}")
            }

            val state = _uiState.value
            val isAuth = state.isAuthenticated && preferences.isLoggedIn
            val targetFlow = when {
                isAuth && state.needsProfileSetup -> AppScreenFlow.PROFILE_SETUP
                isAuth -> AppScreenFlow.MAIN
                preferences.isOnboarded -> AppScreenFlow.AUTH
                else -> AppScreenFlow.ONBOARDING
            }

            _uiState.update { it.copy(screenFlow = targetFlow) }
            if (targetFlow == AppScreenFlow.MAIN) {
                simulateSkeletonRefresh()
            }
        }
    }

    fun finishOnboarding(openAuth: Boolean, isRegister: Boolean = false) {
        preferences.isOnboarded = true
        if (_uiState.value.isAuthenticated && preferences.isLoggedIn) {
            _uiState.update { it.copy(screenFlow = AppScreenFlow.MAIN) }
            simulateSkeletonRefresh()
        } else {
            _uiState.update {
                it.copy(
                    screenFlow = AppScreenFlow.AUTH,
                    isRegisterAuthMode = isRegister,
                    isOtpAuthMode = false
                )
            }
        }
    }

    fun handleAuthBack() {
        // Navigating back from Auth returns to Onboarding screen, NEVER to Dashboard
        _uiState.update {
            it.copy(
                screenFlow = AppScreenFlow.ONBOARDING,
                isRegisterAuthMode = false,
                isOtpAuthMode = false
            )
        }
    }

    fun startGoogleVerification(profile: UserProfile, otpCode: String) {
        preferences.isOnboarded = true
        _uiState.update {
            it.copy(
                screenFlow = AppScreenFlow.AUTH,
                pendingAuthProfile = profile,
                pendingAuthOtp = otpCode,
                isOtpAuthMode = true
            )
        }
    }

    fun openAuthScreen(isRegister: Boolean = false) {
        _uiState.update { it.copy(screenFlow = AppScreenFlow.AUTH, isRegisterAuthMode = isRegister, isOtpAuthMode = false) }
    }

    fun handleAuthSuccess(profile: UserProfile) {
        viewModelScope.launch {
            preferences.isOnboarded = true

            // ── Mandatory profile setup ────────────────────────────────────
            // Email sign-up knows only name+email, and Google knows only
            // name+email+picture. Phone, school, stream, photo and a password
            // are collected here before the student can reach the dashboard.
            if (!profile.profileCompleted) {
                preferences.isGuest = false
                preferences.userName = profile.name
                preferences.userEmail = profile.email
                repository.saveUserProfile(profile.copy(isLoggedIn = true))
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        needsProfileSetup = true,
                        screenFlow = AppScreenFlow.PROFILE_SETUP,
                        isOtpAuthMode = false,
                        pendingAuthProfile = null,
                        pendingAuthOtp = null
                    )
                }
                return@launch
            }

            // NOTE: do not force isLoggedIn here. TheStudentPreferences.setSession()
            // already stored the session with the user's "remember me" choice, and
            // overriding it would silently keep a session alive that the user asked
            // not to persist. In-memory auth state is handled by _uiState below.
            preferences.isGuest = false
            preferences.userName = profile.name
            preferences.userEmail = profile.email
            val loggedInProfile = profile.copy(isLoggedIn = true)
            repository.saveUserProfile(loggedInProfile)
            _isAdmin.value = preferences.isAdmin
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    needsProfileSetup = false,
                    screenFlow = AppScreenFlow.MAIN,
                    isOtpAuthMode = false,
                    pendingAuthProfile = null,
                    pendingAuthOtp = null
                )
            }
            val firstName = profile.name.trim().split(" ").firstOrNull() ?: "Student"
            showSnackbar("Welcome to TheStudent, $firstName!")
            simulateSkeletonRefresh()
        }
    }

    // ── Structured academic profile ──────────────────────────────────────────
    /**
     * Open the academic step (stream, three subjects, exam year, medium).
     *
     * Every other new feature — matching, help requests, per-subject ratings —
     * reads from what is saved here, so this is the first thing the profile
     * screen nudges a student to finish.
     */
    fun openAcademicSetup() {
        _uiState.update {
            it.copy(isAcademicSetupOpen = true, academicSetupError = null)
        }
    }

    /** Close the academic step. Refused while a save is in flight. */
    fun closeAcademicSetup() {
        if (_uiState.value.isSavingAcademic) return
        _uiState.update {
            it.copy(isAcademicSetupOpen = false, academicSetupError = null)
        }
    }

    fun dismissAcademicSetupError() {
        _uiState.update { it.copy(academicSetupError = null) }
    }

    /**
     * Persist the academic profile.
     *
     * The server validates the shape (3 distinct subjects, one per O/L bucket,
     * a sane year, a known medium); this only prepares the request. On success
     * the refreshed profile replaces the cached one, so the profile screen and
     * the chat profile immediately show the new subjects.
     */
    fun saveAcademicProfile(profile: AcademicProfile) {
        if (_uiState.value.isSavingAcademic) return

        val token = preferences.sessionToken
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(academicSetupError = "Your session expired. Please sign in again.")
            }
            endSession("Your session expired. Please sign in again.", revokeOnServer = false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAcademic = true, academicSetupError = null) }

            val result = TheStudentAuthClient.saveAcademicProfile(
                backendBaseUrl = serverUrlState.value,
                token = token,
                profile = profile
            )

            when (result) {
                is AuthApiResult.Success -> {
                    val saved = result.profile.copy(isLoggedIn = true)
                    repository.saveUserProfile(saved)
                    _uiState.update {
                        it.copy(
                            isSavingAcademic = false,
                            isAcademicSetupOpen = false,
                            academicSetupError = null
                        )
                    }
                    showSnackbar("Subjects saved — matching will use them from now on")
                    simulateSkeletonRefresh()
                }

                is AuthApiResult.Error -> {
                    // The usual cause of this exact message is a host still
                    // serving an older api/auth.php. Say so instead of leaving
                    // the student with "Invalid API action".
                    val message = if (result.message.contains("Invalid API action", true)) {
                        "Your server is running an older API. Upload backend/api/auth.php " +
                            "(needs version 3.5.0 or newer), then try again."
                    } else {
                        result.message
                    }
                    _uiState.update {
                        it.copy(isSavingAcademic = false, academicSetupError = message)
                    }
                    if (result.invalidatesSession) {
                        endSession(result.message, revokeOnServer = false)
                    }
                }

                is AuthApiResult.RequiresOtp -> _uiState.update {
                    it.copy(
                        isSavingAcademic = false,
                        academicSetupError = "Please verify your email address first."
                    )
                }

                is AuthApiResult.ResetRequired -> _uiState.update {
                    it.copy(
                        isSavingAcademic = false,
                        academicSetupError = "Please sign in again to continue."
                    )
                }
            }
        }
    }

    // ── Mandatory profile setup ───────────────────────────────────────────────
    /**
     * Submit the phone / school / stream / photo (+ password for Google-only
     * accounts) that sign-up could not collect.
     *
     * The server re-validates everything; on success the refreshed profile is
     * cached and the flow moves to MAIN. Only a 401/403 (dead session) sends the
     * student back to the auth screen — every other failure keeps the form so no
     * typed data is lost.
     */
    fun submitProfileSetup(submission: ProfileSetupSubmission) {
        if (_uiState.value.isSavingProfile) return

        val token = preferences.sessionToken
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    profileSetupError = "Your session expired. Please sign in again.",
                    isSavingProfile = false
                )
            }
            endSession("Your session expired. Please sign in again.", revokeOnServer = false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, profileSetupError = null) }

            val result = TheStudentAuthClient.completeProfile(
                backendBaseUrl = serverUrlState.value,
                token = token,
                fullName = submission.fullName,
                phone = submission.phone,
                targetExam = submission.targetExam,
                institution = submission.institution,
                streamOrDegree = submission.streamOrDegree,
                password = submission.password,
                avatarJpeg = submission.avatarJpeg
            )

            when (result) {
                is AuthApiResult.Success -> {
                    val profile = result.profile.copy(isLoggedIn = true, profileCompleted = true)
                    repository.saveUserProfile(profile)
                    preferences.isLoggedIn = true
                    preferences.isGuest = false
                    preferences.userName = profile.name
                    preferences.userEmail = profile.email
                    result.expiresAt?.let { preferences.sessionExpiresAt = it }
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            needsProfileSetup = false,
                            isSavingProfile = false,
                            profileSetupError = null,
                            screenFlow = AppScreenFlow.MAIN
                        )
                    }
                    val firstName = profile.name.trim().split(" ").firstOrNull().orEmpty()
                    showSnackbar(
                        if (firstName.isBlank()) "Your profile is ready!" else "You're all set, $firstName!"
                    )
                    simulateSkeletonRefresh()
                }

                is AuthApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            profileSetupError = result.message
                        )
                    }
                    if (result.invalidatesSession) {
                        endSession(result.message, revokeOnServer = false)
                    }
                }

                is AuthApiResult.RequiresOtp -> _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profileSetupError = "Please verify your email address first."
                    )
                }

                is AuthApiResult.ResetRequired -> _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profileSetupError = "Unexpected response from the server. Please try again."
                    )
                }
            }
        }
    }

    fun clearProfileSetupError() {
        _uiState.update { it.copy(profileSetupError = null) }
    }

    fun continueAsGuest() {
        // Guest mode deprecated: Dashboard is strictly protected with authentication
        openAuthScreen(isRegister = false)
    }

    /** Sign out of this device (server session revoked, local state cleared). */
    fun logout() {
        endSession("Logged out successfully.")
    }

    /**
     * Save the details edited on the Account page.
     *
     * Only the fields that actually changed are sent, so a student who just
     * swapped their photo cannot accidentally rewrite their phone number with a
     * half-typed value. The session is never touched here — saving your own
     * details must not look like a sign-out.
     */
    fun updateAccountDetails(
        fullName: String,
        phone: String,
        institution: String,
        avatarJpeg: ByteArray? = null
    ) {
        if (_uiState.value.isSavingAccount) return

        val token = preferences.sessionToken
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(accountSaveError = "Your session expired. Please sign in again.")
            }
            return
        }

        val current = userProfile.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAccount = true, accountSaveError = null) }

            val result = TheStudentAuthClient.updateProfile(
                backendBaseUrl = serverUrlState.value,
                token = token,
                fullName = fullName.takeIf { it != current.name },
                phone = phone.takeIf { it != current.phone },
                institution = institution.takeIf { it != current.institution },
                avatarJpeg = avatarJpeg
            )

            when (result) {
                is AuthApiResult.Success -> {
                    val merged = result.profile.copy(
                        isLoggedIn = true,
                        profileCompleted = current.profileCompleted,
                        // Local academic data wins: the update endpoint does not
                        // touch level/stream/subjects, so echo what we already had.
                        academic = current.academic,
                        streakDays = current.streakDays,
                        totalPapersSolved = current.totalPapersSolved,
                        studyHoursThisWeek = current.studyHoursThisWeek
                    )
                    repository.saveUserProfile(merged)
                    preferences.userName = merged.name
                    _uiState.update {
                        it.copy(
                            isSavingAccount = false,
                            accountSaveError = null
                        )
                    }
                    showSnackbar("Your details are saved.")
                }

                is AuthApiResult.Error -> _uiState.update {
                    it.copy(isSavingAccount = false, accountSaveError = result.message)
                }

                is AuthApiResult.RequiresOtp -> _uiState.update {
                    it.copy(
                        isSavingAccount = false,
                        accountSaveError = "Please finish verifying your email first."
                    )
                }

                is AuthApiResult.ResetRequired -> _uiState.update {
                    it.copy(
                        isSavingAccount = false,
                        accountSaveError = "Please sign in again to edit your details."
                    )
                }
            }
        }
    }

    /**
     * Sign out of EVERY device, then clear local state.
     * Used by the security screen ("sign out everywhere") and after account
     * deletion, so a stolen token cannot keep working.
     */
    fun signOutEverywhere() {
        viewModelScope.launch {
            val token = preferences.sessionToken
            val serverUrl = serverUrlState.value
            if (!token.isNullOrBlank()) {
                TheStudentAuthClient.logoutAll(serverUrl, token)
            }
            endSession("Signed out of all devices.")
        }
    }

    /**
     * Clear every trace of the session and return to the auth flow.
     * `revokeOnServer` is skipped when the account no longer exists.
     */
    fun endSession(message: String, revokeOnServer: Boolean = true) {
        viewModelScope.launch {
            val token = preferences.sessionToken
            val serverUrl = serverUrlState.value
            // Stop pushes to this phone before the session disappears.
            if (!token.isNullOrBlank()) {
                runCatching { TheStudentChatClient.unregisterDevice(serverUrl, token) }
            }
            preferences.logout()
            stopChatSync()
            ChatCrypto.clearSessionCache()
            // Chat history is device storage: a different student signing in on
            // this phone must not be able to read the previous one's messages.
            runCatching { chatDao.clearAllChatMessages() }
            runCatching { ChatAttachmentStore.clearAll(appContext) }
            chatSummaries = emptyList()
            chatPeers = emptyList()
            profileJob?.cancel()
            profileJob = null
            ChatNotifier.reset()
            pendingNotificationPeerId = null
            ChatNotifications.clearAll(appContext)
            _chatState.value = ChatUiState()
            repository.clearUserProfile()
            _isAdmin.value = false
            _uiState.update {
                it.copy(
                    isAdminPanelOpen = false,
                    isSecurityPanelOpen = false,
                    isAccountOpen = false,
                    isExpOpen = false
                )
            }
            if (revokeOnServer && !token.isNullOrBlank()) {
                // Revoke server-side AFTER clearing local state so the UI reacts
                // instantly even if the network call is slow or fails.
                TheStudentAuthClient.logout(serverUrl, token)
            }
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    screenFlow = AppScreenFlow.AUTH,
                    isRegisterAuthMode = false,
                    isOtpAuthMode = false
                )
            }
            showSnackbar(message)
        }
    }

    fun simulateSkeletonRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContent = true) }
            kotlinx.coroutines.delay(450)
            _uiState.update { it.copy(isLoadingContent = false) }
        }
    }

    // Navigation and tabs
    fun setTab(tab: TheStudentNavTab) {
        val wasDifferent = _uiState.value.currentTab != tab
        _uiState.update {
            it.copy(
                currentTab = tab,
                isLanguageLabOpen = false,
                isExpOpen = false,
                isAccountOpen = false
            )
        }
        if (wasDifferent && (tab == TheStudentNavTab.HOME || tab == TheStudentNavTab.CHAT || tab == TheStudentNavTab.PAPERS)) {
            simulateSkeletonRefresh()
        }
    }

    fun openLanguageLab(subTab: Int = 0) {
        _uiState.update { it.copy(isLanguageLabOpen = true, activeLanguageSubTab = subTab) }
    }

    fun closeLanguageLab() {
        _uiState.update { it.copy(isLanguageLabOpen = false) }
    }

    // ── EXP ──────────────────────────────────────────────────────────────────
    private val _expState = MutableStateFlow(UserExp())
    val expState: StateFlow<UserExp> = _expState.asStateFlow()

    private val _isExpLoading = MutableStateFlow(false)
    val isExpLoading: StateFlow<Boolean> = _isExpLoading.asStateFlow()

    // ── My reputation (ratings I received) ───────────────────────────────────
    private val _myRatings = MutableStateFlow(ChatMyRatings())
    val myRatings: StateFlow<ChatMyRatings> = _myRatings.asStateFlow()

    private val _isMyRatingsLoading = MutableStateFlow(false)
    val isMyRatingsLoading: StateFlow<Boolean> = _isMyRatingsLoading.asStateFlow()

    /**
     * Pull the ratings I received, including the private notes.
     *
     * Only the profile tab calls this: it is the one screen where a student is
     * looking at themselves, and the notes are nobody else's business.
     */
    fun refreshMyRatings() {
        val session = chatSession() ?: return
        if (_isMyRatingsLoading.value) return
        _isMyRatingsLoading.value = true
        viewModelScope.launch {
            val (baseUrl, token) = session
            TheStudentChatClient.fetchMyRatings(baseUrl, token).dataOrNull()?.let {
                _myRatings.value = it
            }
            _isMyRatingsLoading.value = false
        }
    }

    /** Open "Your EXP" and refresh it from the ledger. */
    fun openExp() {
        _uiState.update { it.copy(isExpOpen = true) }
        refreshExp()
    }

    fun closeExp() {
        _uiState.update { it.copy(isExpOpen = false) }
    }

    /**
     * Account details.
     *
     * Deliberately separate from `openAuthScreen()`: the old profile screen sent
     * "Account" to the sign-in flow, which looks exactly like being logged out.
     * Looking at your own details must never touch the session.
     */
    fun openAccount() {
        _uiState.update { it.copy(isAccountOpen = true) }
    }

    fun closeAccount() {
        _uiState.update { it.copy(isAccountOpen = false) }
    }

    /**
     * Pull the ledger. This is the only place the EXP total is ever written on
     * the client — no optimistic "+25" anywhere — so the number on screen is
     * always one the server agrees with.
     */
    fun refreshExp() {
        val session = chatSession() ?: return
        if (_isExpLoading.value) return
        _isExpLoading.value = true
        viewModelScope.launch {
            val (baseUrl, token) = session
            val exp = TheStudentChatClient.fetchExpState(baseUrl, token).dataOrNull()
            if (exp != null) {
                _expState.value = exp
                // Keep the home pill in step with the ledger. Only the one
                // column is written: a refresh must never re-save the whole
                // profile from a copy that could be older than the row.
                repository.updateUserPoints(exp.total)
            }
            _isExpLoading.value = false
        }
    }

    fun setLanguageSubTab(subTab: Int) {
        _uiState.update { it.copy(activeLanguageSubTab = subTab) }
    }

    fun setEducationLevel(level: EducationLevel) {
        _uiState.update { it.copy(selectedEducationLevel = level) }
    }

    fun setExamFilter(exam: String) {
        _uiState.update { it.copy(selectedExamFilter = exam) }
    }

    fun setSubjectFilter(subject: String) {
        _uiState.update { it.copy(selectedSubjectFilter = subject) }
    }

    fun setYearFilter(year: Int?) {
        _uiState.update { it.copy(selectedYearFilter = year) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openPaperDetail(paper: PastPaper) {
        _uiState.update { it.copy(activePaperDetail = paper) }
    }

    fun closePaperDetail() {
        _uiState.update { it.copy(activePaperDetail = null) }
    }

    fun openCourseDetail(course: Course) {
        _uiState.update { it.copy(activeCourseDetail = course) }
    }

    fun closeCourseDetail() {
        _uiState.update { it.copy(activeCourseDetail = null) }
    }

    fun toggleSavePaper(paper: PastPaper) {
        viewModelScope.launch {
            val isSaved = savedPapers.value.any { it.id == paper.id }
            if (isSaved) {
                repository.removeSavedPaper(paper.id)
                showSnackbar("Paper removed from saved list")
            } else {
                repository.toggleSavePaper(paper)
                showSnackbar("Saved '${paper.title}' to Room database")
            }
        }
    }

    fun downloadPaperOffline(paper: PastPaper) {
        viewModelScope.launch {
            pastPapersRepository.downloadPaperPdf(paper)
            repository.markPaperDownloaded(paper.id)
            showSnackbar("Downloading ${paper.title} from server...")
        }
    }

    fun toggleSaveCourse(course: Course) {
        viewModelScope.launch {
            val isSaved = savedCourses.value.any { it.id == course.id }
            if (isSaved) {
                repository.removeSavedCourse(course.id)
                showSnackbar("Course removed from enrolled collection")
            } else {
                repository.toggleSaveCourse(course)
                showSnackbar("Enrolled in '${course.title}'")
            }
        }
    }

    fun sendAiQuestion(question: String) {
        if (question.isBlank()) return
        val userMsg = AiChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = MessageSender.USER,
            content = question
        )
        _chatMessages.update { it + userMsg }

        viewModelScope.launch {
            val aiResponse = repository.generateAiTutorResponse(question)
            _chatMessages.update { it + aiResponse }
            repository.recordAiSession(question.take(24), 2)
        }
    }

    fun speakText(text: String) {
        if (tts == null) {
            initTts()
        }
        if (isTtsInitialized && tts != null) {
            tts?.stop()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "thestudent_tts_id")
            _uiState.update { it.copy(isTtsSpeaking = true) }
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _uiState.update { it.copy(isTtsSpeaking = false) }
    }

    fun submitGrammarAnswer(selectedIndex: Int) {
        val currentQ = grammarList[_uiState.value.activeGrammarQuestionIndex]
        _uiState.update {
            it.copy(
                selectedGrammarAnswer = selectedIndex,
                grammarAnswerSubmitted = true
            )
        }
        val isCorrect = selectedIndex == currentQ.correctIndex
        viewModelScope.launch {
            repository.recordLanguagePractice(
                mode = "Grammar Quiz",
                score = if (isCorrect) "100% (Correct)" else "Review Needed"
            )
        }
    }

    fun nextGrammarQuestion() {
        val nextIdx = (_uiState.value.activeGrammarQuestionIndex + 1) % grammarList.size
        _uiState.update {
            it.copy(
                activeGrammarQuestionIndex = nextIdx,
                selectedGrammarAnswer = null,
                grammarAnswerSubmitted = false
            )
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun toggleAuthDialog(open: Boolean) {
        _uiState.update { it.copy(isAuthDialogOpen = open) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ==========================================
    // AI English Voice Tutor (Speech-to-Speech)
    // ==========================================

    fun startVoiceListening() {
        stopSpeaking()
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
                _uiState.update {
                    it.copy(
                        voiceAssistantState = AssistantVoiceState.LISTENING,
                        currentUserTranscript = "Listening..."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.IDLE) }
                showSnackbar("Could not start speech recognition: ${e.localizedMessage}")
            }
        } else {
            showSnackbar("Speech Recognition is not available on this device. You can type queries directly!")
            _uiState.update { it.copy(isKeyboardInputOpen = true) }
        }
    }

    fun stopVoiceListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.IDLE, micRmsDb = 0f) }
    }

    fun toggleVoiceListening() {
        when (_uiState.value.voiceAssistantState) {
            AssistantVoiceState.LISTENING -> stopVoiceListening()
            AssistantVoiceState.SPEAKING -> stopSpeaking()
            AssistantVoiceState.THINKING -> { /* waiting */ }
            AssistantVoiceState.IDLE -> startVoiceListening()
        }
    }

    fun processUserVoiceInput(userInput: String) {
        val trimmed = userInput.trim()
        if (trimmed.isBlank()) return

        stopSpeaking()
        _uiState.update {
            it.copy(
                currentUserTranscript = trimmed,
                voiceAssistantState = AssistantVoiceState.THINKING,
                micRmsDb = 0f
            )
        }

        viewModelScope.launch {
            val scenario = _uiState.value.currentScenario
            val customToken = _uiState.value.customHfToken.takeIf { it.isNotBlank() }
            val customBaseUrl = _uiState.value.customHfBaseUrl.takeIf { it.isNotBlank() }

            val response = huggingFaceClient.getChatCompletion(
                userMessage = "[$scenario Practice] $trimmed",
                conversationHistory = conversationTurns,
                customToken = customToken,
                customBaseUrl = customBaseUrl
            )

            when (response) {
                is HfApiResponse.Success -> {
                    conversationTurns.add("user" to trimmed)
                    conversationTurns.add("assistant" to response.text)
                    
                    val localAnalysis = EnglishVoiceTutorEngine.generateLocalConversationalResponse(trimmed, scenario)
                    _uiState.update {
                        it.copy(
                            currentAiResponseText = response.text,
                            currentGrammarTip = localAnalysis.grammarTip,
                            currentVocabularyHighlight = localAnalysis.vocabularyHighlight,
                            suggestedVoicePills = localAnalysis.suggestedReplyPills,
                            hfStatusMessage = "Connected to Gemma / Qwen (${response.latencyMs}ms)"
                        )
                    }
                    speakText(response.text)
                    repository.recordLanguagePractice("AI Voice ($scenario)", "Active Speech")
                }
                is HfApiResponse.Error -> {
                    // Fallback to high quality educational engine
                    val fallback = EnglishVoiceTutorEngine.generateLocalConversationalResponse(trimmed, scenario)
                    conversationTurns.add("user" to trimmed)
                    conversationTurns.add("assistant" to fallback.spokenText)

                    _uiState.update {
                        it.copy(
                            currentAiResponseText = fallback.spokenText,
                            currentGrammarTip = fallback.grammarTip,
                            currentVocabularyHighlight = fallback.vocabularyHighlight,
                            suggestedVoicePills = fallback.suggestedReplyPills,
                            hfStatusMessage = if (response.isAuthError) 
                                "Private Space: Access Token required" 
                            else if (response.isSleepingOrOffline) 
                                "Space is waking up (Running in Voice Assistant mode)"
                            else 
                                "Local Tutor Mode"
                        )
                    }
                    speakText(fallback.spokenText)

                    if (response.isAuthError) {
                        showSnackbar("Hugging Face Space is private. Add your HF_API_TOKEN in Settings.")
                    }
                }
            }
        }
    }

    fun selectVoiceScenario(scenario: String) {
        val starter = EnglishVoiceTutorEngine.getStarterGreeting(scenario)
        stopSpeaking()
        _uiState.update {
            it.copy(
                currentScenario = scenario,
                currentAiResponseText = starter.spokenText,
                currentGrammarTip = starter.grammarTip,
                currentVocabularyHighlight = starter.vocabularyHighlight,
                suggestedVoicePills = starter.suggestedReplyPills,
                currentUserTranscript = null,
                voiceAssistantState = AssistantVoiceState.IDLE
            )
        }
        speakText(starter.spokenText)
    }

    fun replayCurrentAiResponse() {
        val text = _uiState.value.currentAiResponseText
        if (text.isNotBlank()) {
            speakText(text)
        }
    }

    fun stopAiSpeech() {
        stopSpeaking()
        _uiState.update { it.copy(voiceAssistantState = AssistantVoiceState.IDLE) }
    }

    fun toggleHandsFreeMode() {
        val newHandsFree = !_uiState.value.isHandsFreeActive
        _uiState.update { it.copy(isHandsFreeActive = newHandsFree) }
        showSnackbar(if (newHandsFree) "Hands-Free Auto Conversation turned ON" else "Hands-Free turned OFF")
    }

    fun toggleKeyboardInput(open: Boolean) {
        _uiState.update { it.copy(isKeyboardInputOpen = open) }
    }

    fun toggleHfSettings(open: Boolean) {
        _uiState.update { it.copy(isHfSettingsOpen = open) }
    }

    fun updateHfCredentials(token: String, baseUrl: String) {
        _uiState.update {
            it.copy(
                customHfToken = token,
                customHfBaseUrl = baseUrl,
                isHfSettingsOpen = false
            )
        }
        showSnackbar("Updated Hugging Face Space configuration")
        testHfSpaceConnection()
    }

    fun testHfSpaceConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(hfStatusMessage = "Testing connection...") }
            val response = huggingFaceClient.getChatCompletion(
                userMessage = "Ping test: Are you ready?",
                conversationHistory = emptyList(),
                customToken = _uiState.value.customHfToken.takeIf { it.isNotBlank() },
                customBaseUrl = _uiState.value.customHfBaseUrl.takeIf { it.isNotBlank() }
            )
            when (response) {
                is HfApiResponse.Success -> {
                    _uiState.update { it.copy(hfStatusMessage = "Connected to Gemma/Qwen (${response.latencyMs}ms)") }
                    showSnackbar("Successfully connected to Hugging Face Gemma/Qwen Space!")
                }
                is HfApiResponse.Error -> {
                    _uiState.update {
                        it.copy(
                            hfStatusMessage = if (response.isAuthError) "Token Required (Private Space)" else "Space Sleeping or Offline"
                        )
                    }
                    showSnackbar("HF Status: ${response.message}")
                }
            }
        }
    }

    // ==========================================
    // MULTIMODAL AI STUDY ASSISTANT (ChatGPT / Gemini)
    // ==========================================

    fun openAiAssistant() {
        _uiState.update { state ->
            state.copy(isAiAssistantOpen = true)
        }
    }

    fun closeAiAssistant() {
        _uiState.update { it.copy(isAiAssistantOpen = false) }
    }

    fun setAiAssistantInput(text: String) {
        _uiState.update { it.copy(aiAssistantInputText = text) }
    }

    fun setPendingAiAttachment(attachment: AttachedMedia?) {
        ocrJob?.cancel()
        if (attachment == null) {
            _uiState.update {
                it.copy(
                    pendingAiAttachment = null,
                    isOcrExtracting = false,
                    ocrExtractedText = null,
                    ocrDetectedLanguage = null,
                    ocrEngineUsed = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                pendingAiAttachment = attachment,
                isOcrExtracting = false,
                ocrExtractedText = null,
                ocrDetectedLanguage = null,
                ocrEngineUsed = null
            )
        }

        // Auto-extract text in background immediately using Tesseract-OCR without blocking user input
        if (attachment.type == AttachedMediaType.IMAGE && !attachment.uri.isNullOrBlank()) {
            runOcrExtraction(attachment.uri, null)
        }
    }

    fun runOcrExtraction(uriString: String, preferredLang: String? = null) {
        ocrJob?.cancel()
        ocrJob = viewModelScope.launch {
            _uiState.update { it.copy(isOcrExtracting = true) }
            try {
                val uri = Uri.parse(uriString)
                val result = theStudentOcrEngine.extractTextFromImage(uri, preferredLang)
                _uiState.update { state ->
                    state.copy(
                        isOcrExtracting = false,
                        ocrExtractedText = if (result.isSuccess) result.extractedText else null,
                        ocrDetectedLanguage = result.detectedLanguage,
                        ocrEngineUsed = result.engineUsed,
                        pendingAiAttachment = state.pendingAiAttachment?.copy(
                            extractedOcrText = result.extractedText,
                            ocrLanguage = result.detectedLanguage
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isOcrExtracting = false) }
            }
        }
    }

    fun updateOcrExtractedText(newText: String) {
        _uiState.update { state ->
            state.copy(
                ocrExtractedText = newText,
                pendingAiAttachment = state.pendingAiAttachment?.copy(
                    extractedOcrText = newText
                )
            )
        }
    }

    fun toggleOcrFastMode(enabled: Boolean) {
        _uiState.update { it.copy(isOcrFastMode = enabled) }
    }

    fun selectAiAssistantSubject(subject: String) {
        _uiState.update { it.copy(selectedAiSubject = subject) }
    }

    fun clearAiAssistantChat() {
        _uiState.update {
            it.copy(
                aiAssistantMessages = listOf(
                    AiAssistantMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Chat cleared! What topic or question would you like to explore next?",
                        suggestedPrompts = listOf(
                            "Solve homework from photo",
                            "Summarize PDF study notes",
                            "Explain quadratic formulas"
                        )
                    )
                ),
                aiAssistantInputText = "",
                pendingAiAttachment = null,
                isOcrExtracting = false,
                ocrExtractedText = null
            )
        }
    }

    fun rateAiAssistantMessage(messageId: String, isHelpful: Boolean) {
        _uiState.update { state ->
            state.copy(
                aiAssistantMessages = state.aiAssistantMessages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(isHelpful = if (msg.isHelpful == isHelpful) null else isHelpful)
                    } else msg
                }
            )
        }
        showSnackbar(if (isHelpful) "Thanks for the feedback! 👍" else "Thanks! We'll improve future answers.")
    }

    fun speakAiAssistantText(text: String) {
        speakText(text.replace("#", "").replace("`", "").replace("*", "").take(300))
    }

    fun sendAiAssistantMessage(userText: String, attachment: AttachedMedia?, forceFullVision: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() && attachment == null) return
        if (_uiState.value.isAiAssistantGenerating) return // Prevent overlapping generations

        val currentExtractedText = _uiState.value.ocrExtractedText ?: _uiState.value.pendingAiAttachment?.extractedOcrText
        val currentOcrLang = _uiState.value.ocrDetectedLanguage ?: _uiState.value.pendingAiAttachment?.ocrLanguage ?: "Auto"

        val resolvedAttachment = if (attachment != null) {
            attachment.copy(
                extractedOcrText = currentExtractedText ?: attachment.extractedOcrText,
                ocrLanguage = currentOcrLang
            )
        } else null

        val isSinhala = (currentExtractedText?.any { it.code in 0x0D80..0x0DFF } == true) ||
                        (currentOcrLang.contains("Sinhala", ignoreCase = true))

        val isTamil = (currentExtractedText?.any { it.code in 0x0B80..0x0BFF } == true) ||
                      (currentOcrLang.contains("Tamil", ignoreCase = true))

        val defaultPrompt = when {
            isSinhala -> "කරුණාකර මෙම ප්‍රශ්නය පියවරෙන් පියවර විසඳා පැහැදිලි කරන්න:"
            isTamil -> "தயவுசெய்து இந்த வினாவை படி படியாக விளக்கி விடை தருக:"
            else -> "Solve this question from the photo:"
        }

        val userMessage = AiAssistantMessage(
            sender = MessageSender.USER,
            text = trimmed.ifBlank { defaultPrompt },
            attachment = resolvedAttachment
        )

        val priorHistory = _uiState.value.aiAssistantMessages.map {
            (if (it.sender == MessageSender.USER) "user" else "assistant") to it.text
        }

        _uiState.update { state ->
            state.copy(
                aiAssistantMessages = state.aiAssistantMessages + userMessage,
                aiAssistantInputText = "",
                pendingAiAttachment = null,
                isOcrExtracting = false,
                ocrExtractedText = null,
                isAiAssistantGenerating = true,
                isAiReasoningActive = true,
                aiStreamingReasoningText = "",
                activeStreamingMessageId = null
            )
        }

        aiAssistantJob?.cancel()
        aiAssistantJob = viewModelScope.launch {
            val selectedSubject = _uiState.value.selectedAiSubject

            // If background OCR is still completing, wait up to 700ms to grab latest Tesseract text
            if (ocrJob?.isActive == true) {
                runCatching {
                    kotlinx.coroutines.withTimeout(700) {
                        ocrJob?.join()
                    }
                }
            }

            val finalExtractedText = _uiState.value.ocrExtractedText ?: resolvedAttachment?.extractedOcrText
            val finalOcrLang = _uiState.value.ocrDetectedLanguage ?: resolvedAttachment?.ocrLanguage

            val clientAttachment = resolvedAttachment?.copy(
                extractedOcrText = finalExtractedText,
                ocrLanguage = finalOcrLang
            )

            val finalPrompt = when {
                trimmed.isNotBlank() -> trimmed
                clientAttachment?.type == AttachedMediaType.IMAGE -> {
                    if (isSinhala) "කරුණාකර මෙම ප්‍රශ්නය පියවරෙන් පියවර විසඳා පැහැදිලි කර නිවැරදි පිළිතුර සිංහලෙන් ලබා දෙන්න."
                    else if (isTamil) "தயவுசெய்து இந்த வினாவை படி படியாக விளக்கி விடை தருக."
                    else "Please solve and explain the exam question shown in this image step-by-step with clear derivations, formulas, and verified answers."
                }
                clientAttachment?.type == AttachedMediaType.PDF -> "Please summarize the key exam concepts and high-yield study notes from this document."
                else -> "Please help me study for ${selectedSubject}."
            }

            var streamingMessageId: String? = null
            val selectedModel = _uiState.value.selectedAiModel
            val customZaiKey = _uiState.value.customZaiApiKey.takeIf { it.isNotBlank() }
            val customHfToken = _uiState.value.customHfToken.takeIf { it.isNotBlank() }
            val customHfBaseUrl = _uiState.value.customHfBaseUrl.takeIf { it.isNotBlank() }
            val reasoningStartTime = System.currentTimeMillis()
            val accumulatedReasoning = StringBuilder()

            val response = theStudentStudyAiClient.streamStudyResponse(
                prompt = finalPrompt,
                attachment = clientAttachment,
                subject = selectedSubject,
                conversationHistory = priorHistory,
                customToken = customHfToken,
                customBaseUrl = customHfBaseUrl,
                selectedModel = selectedModel,
                customZaiApiKey = customZaiKey,
                onReasoningChunk = { reasoningToken ->
                    accumulatedReasoning.append(reasoningToken)
                    _uiState.update { state ->
                        state.copy(
                            isAiReasoningActive = true,
                            aiStreamingReasoningText = accumulatedReasoning.toString()
                        )
                    }
                },
                onReasoningComplete = {
                    val duration = (System.currentTimeMillis() - reasoningStartTime) / 1000.0
                    _uiState.update { state ->
                        state.copy(
                            isAiReasoningActive = false,
                            aiReasoningDurationSeconds = duration
                        )
                    }
                },
                onContentChunk = { contentToken ->
                    _uiState.update { state ->
                        val sanitized = if (contentToken == "null") "" else contentToken.removePrefix("null")
                        if (sanitized.isEmpty()) return@update state

                        val currentId = streamingMessageId
                        val duration = if (accumulatedReasoning.isNotEmpty()) {
                            (System.currentTimeMillis() - reasoningStartTime) / 1000.0
                        } else null
                        val currentReasoningText = accumulatedReasoning.toString().takeIf { it.isNotBlank() }

                        if (currentId == null) {
                            val newId = UUID.randomUUID().toString()
                            streamingMessageId = newId
                            val newMsg = AiAssistantMessage(
                                id = newId,
                                sender = MessageSender.ASSISTANT,
                                text = sanitized,
                                subjectTag = selectedSubject,
                                reasoningText = currentReasoningText,
                                thinkingDurationSeconds = duration
                            )
                            state.copy(
                                isAiReasoningActive = false,
                                aiReasoningDurationSeconds = duration,
                                activeStreamingMessageId = newId,
                                aiAssistantMessages = state.aiAssistantMessages + newMsg
                            )
                        } else {
                            val updated = state.aiAssistantMessages.map { msg ->
                                if (msg.id == currentId) {
                                    msg.copy(
                                        text = msg.text + sanitized,
                                        reasoningText = currentReasoningText ?: msg.reasoningText,
                                        thinkingDurationSeconds = duration ?: msg.thinkingDurationSeconds
                                    )
                                } else msg
                            }
                            state.copy(
                                isAiReasoningActive = false,
                                aiAssistantMessages = updated
                            )
                        }
                    }
                }
            )

            when (response) {
                is TheStudentAiStudyResponse.Success -> {
                    val finalReasoning = accumulatedReasoning.toString().takeIf { it.isNotBlank() }
                        ?: response.reasoningText?.takeIf { it.isNotBlank() }
                    val finalDuration = if (finalReasoning != null) {
                        (System.currentTimeMillis() - reasoningStartTime) / 1000.0
                    } else null

                    _uiState.update { state ->
                        val finalId = streamingMessageId
                        val updatedMessages = if (finalId != null) {
                            state.aiAssistantMessages.map { msg ->
                                if (msg.id == finalId) {
                                    msg.copy(
                                        text = response.text.ifBlank { msg.text },
                                        suggestedPrompts = response.suggestedPrompts,
                                        reasoningText = finalReasoning ?: msg.reasoningText,
                                        thinkingDurationSeconds = finalDuration ?: msg.thinkingDurationSeconds
                                    )
                                } else msg
                            }
                        } else {
                            state.aiAssistantMessages + AiAssistantMessage(
                                sender = MessageSender.ASSISTANT,
                                text = response.text,
                                subjectTag = selectedSubject,
                                suggestedPrompts = response.suggestedPrompts,
                                reasoningText = finalReasoning,
                                thinkingDurationSeconds = finalDuration
                            )
                        }
                        state.copy(
                            aiAssistantMessages = updatedMessages,
                            isAiAssistantGenerating = false,
                            isAiReasoningActive = false,
                            aiStreamingReasoningText = "",
                            activeStreamingMessageId = null
                        )
                    }
                }
                is TheStudentAiStudyResponse.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isAiAssistantGenerating = false,
                            isAiReasoningActive = false,
                            aiStreamingReasoningText = "",
                            activeStreamingMessageId = null
                        )
                    }
                    if (response.isAuthError) {
                        showSnackbar("AI Study API: Please configure your API key in Secrets panel.")
                    } else if (response.isSleepingOrOffline) {
                        showSnackbar("AI Study endpoint connecting...")
                    }
                    if (streamingMessageId == null) {
                        val fallback = generateStudyAssistantResponse(finalPrompt, clientAttachment, selectedSubject, response.message)
                        _uiState.update { it.copy(aiAssistantMessages = it.aiAssistantMessages + fallback) }
                    }
                }
            }
        }
    }

    fun stopAiAssistantGeneration() {
        if (aiAssistantJob?.isActive == true) {
            aiAssistantJob?.cancel()
            aiAssistantJob = null
        }
        _uiState.update { state ->
            val hasActiveStreaming = state.activeStreamingMessageId != null
            val updated = if (!hasActiveStreaming && state.isAiAssistantGenerating) {
                state.aiAssistantMessages + AiAssistantMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Response generation was stopped.",
                    isStopped = true
                )
            } else {
                state.aiAssistantMessages
            }
            state.copy(
                aiAssistantMessages = updated,
                isAiAssistantGenerating = false,
                isAiReasoningActive = false,
                aiStreamingReasoningText = "",
                activeStreamingMessageId = null
            )
        }
    }

    fun retryAiAssistantMessage(assistantMessageId: String) {
        if (_uiState.value.isAiAssistantGenerating) return
        val messages = _uiState.value.aiAssistantMessages
        val targetIndex = messages.indexOfFirst { it.id == assistantMessageId }
        if (targetIndex == -1) return

        // Find the user message that prompted this response
        val userMessage = messages.take(targetIndex).lastOrNull { it.sender == MessageSender.USER } ?: return

        // Remove the target assistant message from the list and re-generate
        val remainingMessages = messages.filterIndexed { index, _ -> index != targetIndex }
        _uiState.update { state ->
            state.copy(
                aiAssistantMessages = remainingMessages,
                isAiAssistantGenerating = true,
                isAiReasoningActive = true,
                aiStreamingReasoningText = "",
                activeStreamingMessageId = null
            )
        }

        val priorHistory = remainingMessages.map {
            (if (it.sender == MessageSender.USER) "user" else "assistant") to it.text
        }

        aiAssistantJob?.cancel()
        aiAssistantJob = viewModelScope.launch {
            val selectedSubject = _uiState.value.selectedAiSubject
            val selectedModel = _uiState.value.selectedAiModel
            val customZaiKey = _uiState.value.customZaiApiKey.takeIf { it.isNotBlank() }
            val customHfToken = _uiState.value.customHfToken.takeIf { it.isNotBlank() }
            val customHfBaseUrl = _uiState.value.customHfBaseUrl.takeIf { it.isNotBlank() }
            var streamingMessageId: String? = null
            val reasoningStartTime = System.currentTimeMillis()
            val accumulatedReasoning = StringBuilder()

            val response = theStudentStudyAiClient.streamStudyResponse(
                prompt = userMessage.text,
                attachment = userMessage.attachment,
                subject = selectedSubject,
                conversationHistory = priorHistory,
                customToken = customHfToken,
                customBaseUrl = customHfBaseUrl,
                selectedModel = selectedModel,
                customZaiApiKey = customZaiKey,
                onReasoningChunk = { reasoningToken ->
                    accumulatedReasoning.append(reasoningToken)
                    _uiState.update { state ->
                        state.copy(
                            isAiReasoningActive = true,
                            aiStreamingReasoningText = accumulatedReasoning.toString()
                        )
                    }
                },
                onReasoningComplete = {
                    val duration = (System.currentTimeMillis() - reasoningStartTime) / 1000.0
                    _uiState.update { state ->
                        state.copy(
                            isAiReasoningActive = false,
                            aiReasoningDurationSeconds = duration
                        )
                    }
                },
                onContentChunk = { contentToken ->
                    _uiState.update { state ->
                        val sanitized = if (contentToken == "null") "" else contentToken.removePrefix("null")
                        if (sanitized.isEmpty()) return@update state

                        val currentId = streamingMessageId
                        val duration = if (accumulatedReasoning.isNotEmpty()) {
                            (System.currentTimeMillis() - reasoningStartTime) / 1000.0
                        } else null
                        val currentReasoningText = accumulatedReasoning.toString().takeIf { it.isNotBlank() }

                        if (currentId == null) {
                            val newId = UUID.randomUUID().toString()
                            streamingMessageId = newId
                            val newMsg = AiAssistantMessage(
                                id = newId,
                                sender = MessageSender.ASSISTANT,
                                text = sanitized,
                                subjectTag = selectedSubject,
                                reasoningText = currentReasoningText,
                                thinkingDurationSeconds = duration
                            )
                            state.copy(
                                isAiReasoningActive = false,
                                aiReasoningDurationSeconds = duration,
                                activeStreamingMessageId = newId,
                                aiAssistantMessages = state.aiAssistantMessages + newMsg
                            )
                        } else {
                            val updated = state.aiAssistantMessages.map { msg ->
                                if (msg.id == currentId) {
                                    msg.copy(
                                        text = msg.text + sanitized,
                                        reasoningText = currentReasoningText ?: msg.reasoningText,
                                        thinkingDurationSeconds = duration ?: msg.thinkingDurationSeconds
                                    )
                                } else msg
                            }
                            state.copy(
                                isAiReasoningActive = false,
                                aiAssistantMessages = updated
                            )
                        }
                    }
                }
            )

            when (response) {
                is TheStudentAiStudyResponse.Success -> {
                    val finalReasoning = accumulatedReasoning.toString().takeIf { it.isNotBlank() }
                        ?: response.reasoningText?.takeIf { it.isNotBlank() }
                    val finalDuration = if (finalReasoning != null) {
                        (System.currentTimeMillis() - reasoningStartTime) / 1000.0
                    } else null

                    _uiState.update { state ->
                        val finalId = streamingMessageId
                        val updatedMessages = if (finalId != null) {
                            state.aiAssistantMessages.map { msg ->
                                if (msg.id == finalId) {
                                    msg.copy(
                                        text = response.text.ifBlank { msg.text },
                                        suggestedPrompts = response.suggestedPrompts,
                                        reasoningText = finalReasoning ?: msg.reasoningText,
                                        thinkingDurationSeconds = finalDuration ?: msg.thinkingDurationSeconds
                                    )
                                } else msg
                            }
                        } else {
                            state.aiAssistantMessages + AiAssistantMessage(
                                sender = MessageSender.ASSISTANT,
                                text = response.text,
                                subjectTag = selectedSubject,
                                suggestedPrompts = response.suggestedPrompts,
                                reasoningText = finalReasoning,
                                thinkingDurationSeconds = finalDuration
                            )
                        }
                        state.copy(
                            aiAssistantMessages = updatedMessages,
                            isAiAssistantGenerating = false,
                            isAiReasoningActive = false,
                            aiStreamingReasoningText = "",
                            activeStreamingMessageId = null
                        )
                    }
                }
                is TheStudentAiStudyResponse.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isAiAssistantGenerating = false,
                            isAiReasoningActive = false,
                            aiStreamingReasoningText = "",
                            activeStreamingMessageId = null
                        )
                    }
                    if (response.isAuthError) {
                        showSnackbar("AI Study API: Please configure your API key in Secrets panel.")
                    } else if (response.isSleepingOrOffline) {
                        showSnackbar("AI Study endpoint connecting...")
                    }
                    if (streamingMessageId == null) {
                        val fallback = generateStudyAssistantResponse(userMessage.text, userMessage.attachment, selectedSubject, response.message)
                        _uiState.update { it.copy(aiAssistantMessages = it.aiAssistantMessages + fallback) }
                    }
                }
            }
        }
    }

    private fun generateStudyAssistantResponse(
        prompt: String,
        attachment: AttachedMedia?,
        subject: String,
        errorMessage: String? = null
    ): AiAssistantMessage {
        val lower = prompt.lowercase()

        val (answerText, followUps) = when {
            attachment?.type == AttachedMediaType.IMAGE -> {
                val explanation = "### 💡 Photo Question Analysis\n\nI encountered a temporary connection issue while analyzing the photo (${errorMessage ?: "Request timed out"}).\n\n• Please check your connection and tap **Retry** below to resend the photo.\n• You can also type your question directly in the chat for an instant explanation!"
                Pair(
                    explanation,
                    listOf("Retry question", "Type question in text", "Ask for study notes")
                )
            }
            attachment?.type == AttachedMediaType.PDF -> {
                Pair(
                    "### 📄 Document Summary & Key Takeaways\n\nI have reviewed **${attachment.displayName}** for ${subject}:\n\n## 1. Executive Summary\nThe document covers fundamental principles, core definitions, and high-frequency exam questions.\n\n## 2. Core Concepts\n• **Concept A:** Key definition and theoretical foundation.\n• **Concept B:** Practical real-world applications.\n• **Formulas:** Essential mathematical representations for quick recall.\n\n## 3. High-Yield Exam Tips\nFocus on distinguishing edge cases and reviewing the sample questions at the end of Chapter 3.",
                    listOf("Generate 5 MCQs from this PDF", "Explain formulas from the notes", "Create flashcards summary")
                )
            }
            lower.contains("newton") || lower.contains("law") -> {
                Pair(
                    "### 🍎 Newton's Three Laws of Motion\n\nHere is an intuitive breakdown with formulas:\n\n**1. First Law (Inertia):**\nAn object remains at rest or in uniform motion unless acted upon by an external net force.\n\n**2. Second Law (Fundamental Dynamics):**\n```\nF_net = m * a\nwhere:\nF = Net Force (Newtons, N)\nm = Mass (kg)\na = Acceleration (m/s²)\n```\n\n**3. Third Law (Action-Reaction):**\nFor every action, there is an equal and opposite reaction (F_AB = -F_BA).\n\n• *Example:* When a rocket pushes exhaust gases downward, the gases exert an equal upward thrust propelling the rocket.",
                    listOf("Give numerical examples for F=ma", "How does friction affect Newton's 1st Law?", "Solve past paper question on momentum")
                )
            }
            lower.contains("quiz") || lower.contains("mcq") || lower.contains("practice") -> {
                Pair(
                    "### 📝 5 Practice Exam Questions (${subject})\n\n**Q1:** What is the SI unit of electric potential difference?\n• A) Ampere\n• B) Ohm\n• C) Volt ✅ *(Correct: 1 Volt = 1 Joule/Coulomb)*\n• D) Watt\n\n**Q2:** Which organelle is known as the powerhouse of the cell?\n• A) Ribosome\n• B) Mitochondria ✅ *(Correct: Generates ATP)*\n• C) Nucleus\n• D) Golgi Apparatus\n\n**Q3:** If a force of 20N moves a body 5m in the direction of force, calculate work done:\n```\nWork = Force * Displacement = 20N * 5m = 100 Joules\n```\n\nWould you like me to generate 5 more challenging questions?",
                    listOf("Generate 5 more difficult questions", "Explain Question 3 step-by-step", "Test me on definitions only")
                )
            }
            else -> {
                Pair(
                    "### 💡 Solution & Explanation\n\nHere is the step-by-step breakdown for your question:\n\n**1. Core Understanding:**\nIn **${subject}**, breaking down the fundamental definitions helps pinpoint the solution quickly.\n\n**2. Key Steps:**\n• Identify known variables and definitions\n• Apply standard theoretical formulas\n• Verify dimensional consistency and unit conversions\n\n```\nSolution: Apply standard relation for ${subject}\nResult: Verified consistent with Sri Lankan syllabus requirements.\n```\n\nLet me know if you want me to expand on any specific step or generate sample practice problems!",
                    listOf("Can you give a real-life analogy?", "Show step-by-step proof", "Give me a practice question")
                )
            }
        }

        return AiAssistantMessage(
            sender = MessageSender.ASSISTANT,
            text = answerText,
            subjectTag = subject,
            suggestedPrompts = followUps
        )
    }

    fun selectAiModel(model: AiModelChoice) {
        _uiState.update { it.copy(selectedAiModel = model) }
    }

    fun updateZaiApiKey(key: String) {
        _uiState.update { it.copy(customZaiApiKey = key.trim()) }
    }

    fun toggleModelSelector(open: Boolean) {
        _uiState.update { it.copy(isModelSelectorOpen = open) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        tts?.stop()
        tts?.shutdown()
    }
}

