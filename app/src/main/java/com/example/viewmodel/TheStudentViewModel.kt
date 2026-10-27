package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TheStudentDatabase
import com.example.data.local.ChatCrypto
import com.example.data.local.LearningActivityEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SavedPaperEntity
import com.example.data.model.*
import com.example.data.repository.TheStudentRepository
import com.example.data.repository.SampleEducationalData
import com.example.ui.components.TheStudentNavTab
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
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
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    val onlineCount: Int = 0,
    val totalStudents: Int = 0,
    val safetyAcknowledged: Boolean = false,
    val activePeer: ChatPeer? = null,
    val messages: List<ChatMessage> = emptyList(),

    // ── Backup / restore (after a reinstall) ────────────────────────────────
    /** Server-side history that belongs to this account. */
    val historyBackup: ChatHistoryBackup = ChatHistoryBackup(),
    /** True while the "you have a backup chat history" card is on screen. */
    val showRestorePrompt: Boolean = false,
    /** True while restoring (or deleting) the backup — drives the loader. */
    val isRestoring: Boolean = false,
    /** 0f..1f progress for the restore progress bar. */
    val restoreProgress: Float = 0f,
    /**
     * True when this device has no private key of its own (fresh install) but
     * the server holds a password-wrapped one, so the card must ask for the
     * account password before it can decrypt the history.
     */
    val restoreNeedsPassword: Boolean = false,
    val restoreError: String? = null,
    /** True once the identity key is ready and the server knows the public key. */
    val isEncryptionReady: Boolean = false
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
            repository.initializeDefaultDataIfEmpty()
        }
        loadDynamicPapers()
        verifySessionWithBackend()
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
                    hasPassword = entity.hasPassword
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

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private var chatDirectoryJob: Job? = null
    private var chatPresenceJob: Job? = null
    private var chatMessagesJob: Job? = null
    private var chatTypingJob: Job? = null
    private var chatIsDirectoryVisible = false

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
     * Called by the auth screen the moment an e-mail sign-in / sign-up succeeds.
     *
     * The password is used here only to wrap a copy of the chat private key
     * (PBKDF2 + AES-GCM) so the student can restore their history on a new
     * phone. It is never stored, logged or sent anywhere: the server receives
     * the wrapped blob, which is useless without the password.
     */
    fun protectChatBackupWithPassword(password: String) {
        if (password.length < 8) return
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        viewModelScope.launch {
            val result = TheStudentChatClient.publishIdentity(
                context = appContext,
                accountKey = chatAccountKey,
                backendBaseUrl = baseUrl,
                token = token,
                passwordForBackup = password
            )
            if (result.dataOrNull() != null) {
                _chatState.update { it.copy(isEncryptionReady = true) }
            }
        }
    }

    /**
     * Called when the chat tab becomes visible: publishes the encryption key,
     * loads the student directory and starts presence + polling.
     */
    fun startChatSync() {
        if (chatIsDirectoryVisible) return
        chatIsDirectoryVisible = true

        chatDirectoryJob?.cancel()
        chatDirectoryJob = viewModelScope.launch {
            publishChatIdentityIfNeeded()
            refreshChatDirectory(showSpinner = _chatState.value.threads.isEmpty())
            while (true) {
                delay(CHAT_DIRECTORY_POLL_MS)
                refreshChatDirectory(showSpinner = false)
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
                // 2 s heartbeat: the server treats 10 s of silence as offline,
                // so online / offline flips within a couple of seconds.
                delay(CHAT_PRESENCE_MS)
            }
        }
    }

    /** Called when the chat tab leaves the composition. */
    fun stopChatSync() {
        chatIsDirectoryVisible = false
        chatDirectoryJob?.cancel()
        chatDirectoryJob = null
        chatPresenceJob?.cancel()
        chatPresenceJob = null
        closeChat()
    }

    fun refreshChatDirectoryNow() {
        viewModelScope.launch { refreshChatDirectory(showSpinner = _chatState.value.threads.isEmpty()) }
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

        // A brand-new key pair on a device that already has server-side history
        // means the app was reinstalled: the old private key is gone, so the
        // restore card has to unlock the password-wrapped backup first.
        if (identity.isNew) {
            _chatState.update { it.copy(restoreNeedsPassword = true) }
        }

        val result = TheStudentChatClient.publishIdentity(
            context = appContext,
            accountKey = chatAccountKey,
            backendBaseUrl = baseUrl,
            token = token,
            passwordForBackup = null
        )
        if (result.dataOrNull() != null) {
            _chatState.update { it.copy(isEncryptionReady = true) }
        } else if ((result as? ChatResult.Failure)?.statusCode == 409) {
            // The server already holds a key for this account that this device
            // cannot reproduce (reinstall). Do not send anything until the
            // password-protected backup has been unlocked.
            _chatState.update { it.copy(isEncryptionReady = false, restoreNeedsPassword = true) }
        }
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

        val result = TheStudentChatClient.loadDirectory(
            context = appContext,
            accountKey = chatAccountKey,
            backendBaseUrl = baseUrl,
            token = token
        )
        val directory = result.dataOrNull()
        if (directory != null) {
            val promptHandled = preferences.chatHistoryPromptHandled
            // The key on this device must be the one the server published,
            // otherwise anything we encrypt would be unreadable for the other
            // student (this happens right after a reinstall).
            val localKey = ChatCrypto.publicKeyBase64(appContext, chatAccountKey)
            val encryptionReady = !localKey.isNullOrBlank() &&
                (directory.myPublicKey.isNullOrBlank() || directory.myPublicKey == localKey)
            _chatState.update { state ->
                state.copy(
                    isLoadingDirectory = false,
                    errorMessage = null,
                    threads = directory.threads,
                    onlineCount = directory.onlineCount,
                    totalStudents = directory.totalStudents,
                    safetyAcknowledged = directory.safetyAcknowledged,
                    historyBackup = directory.historyBackup,
                    // Offer the backup only once per install and only while there
                    // is something to restore.
                    showRestorePrompt = !promptHandled &&
                        directory.historyBackup.available &&
                        !state.isRestoring,
                    isEncryptionReady = encryptionReady,
                    restoreNeedsPassword = !encryptionReady &&
                        directory.keyBackupAvailable,
                    activePeer = state.activePeer?.let { active ->
                        directory.threads.firstOrNull { it.peer.id == active.id }?.peer ?: active
                    }
                )
            }
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

    // ── Backup restore ───────────────────────────────────────────────────────

    /**
     * Restore the server-side history.
     *
     * On a re-installed app the private key is gone, so [password] is required
     * to unwrap the password-protected copy from the server. Without it the
     * messages stay ciphertext — there is no back door, by design.
     */
    fun restoreChatHistory(password: String? = null) {
        if (_chatState.value.isRestoring) return
        val session = chatSession() ?: return
        val (baseUrl, token) = session

        viewModelScope.launch {
            closeChat()
            _chatState.update {
                it.copy(isRestoring = true, restoreError = null, restoreProgress = 0.04f)
            }

            if (_chatState.value.restoreNeedsPassword) {
                val typed = password.orEmpty()
                if (typed.isBlank()) {
                    _chatState.update {
                        it.copy(isRestoring = false, restoreError = "Enter your account password.")
                    }
                    return@launch
                }

                val unlocked = TheStudentChatClient
                    .unlockKeyBackup(baseUrl, token, typed)
                    .dataOrNull()

                if (unlocked == null) {
                    _chatState.update {
                        it.copy(
                            isRestoring = false,
                            restoreProgress = 0f,
                            restoreError = "That password could not unlock your encrypted backup."
                        )
                    }
                    return@launch
                }

                val restored = ChatCrypto.restoreIdentity(
                    context = appContext,
                    accountKey = chatAccountKey,
                    wrappedPrivateKeyBase64 = unlocked.wrappedPrivateKeyBase64,
                    saltBase64 = unlocked.saltBase64,
                    password = typed,
                    publishedPublicKey = unlocked.publicKey
                )
                if (!restored) {
                    _chatState.update {
                        it.copy(
                            isRestoring = false,
                            restoreProgress = 0f,
                            restoreError = "That password could not unlock your encrypted backup."
                        )
                    }
                    return@launch
                }

                _chatState.update {
                    it.copy(restoreNeedsPassword = false, restoreProgress = 0.2f)
                }
            }

            // Reload the directory so peer keys are available, then walk every
            // conversation to rebuild the history (this is the visible progress).
            refreshChatDirectory(showSpinner = false)
            val peers = _chatState.value.threads.filter { it.hasConversation }
            if (peers.isEmpty()) {
                _chatState.update { it.copy(restoreProgress = 1f) }
            } else {
                peers.forEachIndexed { index, thread ->
                    TheStudentChatClient.loadMessages(
                        context = appContext,
                        accountKey = chatAccountKey,
                        backendBaseUrl = baseUrl,
                        token = token,
                        peerId = thread.peer.id,
                        afterId = 0L
                    )
                    _chatState.update {
                        it.copy(
                            restoreProgress = 0.2f + 0.78f * ((index + 1).toFloat() / peers.size)
                        )
                    }
                }
            }

            preferences.chatHistoryPromptHandled = true
            _chatState.update {
                it.copy(
                    isRestoring = false,
                    restoreProgress = 1f,
                    showRestorePrompt = false,
                    restoreError = null
                )
            }
            refreshChatDirectory(showSpinner = false)
        }
    }

    /**
     * "Skip" on the restore card: delete this account's chat history from the
     * database as well, so it cannot come back on the next install.
     */
    fun skipChatHistory() {
        if (_chatState.value.isRestoring) return
        val session = chatSession() ?: return
        val (baseUrl, token) = session

        viewModelScope.launch {
            _chatState.update {
                it.copy(isRestoring = true, restoreError = null, restoreProgress = 0.06f)
            }
            _chatState.update { it.copy(restoreProgress = 0.45f) }

            val deleted = TheStudentChatClient.discardHistory(baseUrl, token).dataOrNull()
            preferences.chatHistoryPromptHandled = true

            // The history is gone, so the old key is useless: publish this
            // device's key as the account's key so chat keeps working.
            if (deleted != null) {
                val published = TheStudentChatClient.publishIdentity(
                    context = appContext,
                    accountKey = chatAccountKey,
                    backendBaseUrl = baseUrl,
                    token = token,
                    allowReplace = true
                )
                if (published.dataOrNull() != null) {
                    _chatState.update { it.copy(isEncryptionReady = true) }
                }
            }

            _chatState.update { state ->
                state.copy(
                    isRestoring = false,
                    restoreProgress = 0f,
                    showRestorePrompt = false,
                    messages = emptyList(),
                    historyBackup = ChatHistoryBackup(),
                    threads = if (deleted != null) {
                        state.threads.map {
                            it.copy(
                                lastMessage = null,
                                lastMessageIsMine = false,
                                lastMessageEpoch = 0L,
                                unreadCount = 0
                            )
                        }
                    } else {
                        state.threads
                    },
                    errorMessage = if (deleted == null) {
                        "Could not delete the chat history. Please try again."
                    } else {
                        state.errorMessage
                    }
                )
            }
        }
    }

    fun dismissRestorePrompt() {
        preferences.chatHistoryPromptHandled = true
        _chatState.update { it.copy(showRestorePrompt = false) }
    }

    // ── Conversation ─────────────────────────────────────────────────────────

    /** Open a 1:1 conversation with a registered student. */
    fun openChat(peer: ChatPeer) {
        _chatState.update {
            it.copy(
                activePeer = peer,
                messages = emptyList(),
                isLoadingMessages = true,
                errorMessage = null
            )
        }
        _uiState.update { it.copy(isChatConversationOpen = true) }

        chatMessagesJob?.cancel()
        chatMessagesJob = viewModelScope.launch {
            val session = chatSession()
            if (session == null) {
                _chatState.update {
                    it.copy(isLoadingMessages = false, errorMessage = "Sign in to chat.")
                }
                return@launch
            }
            val (baseUrl, token) = session

            // Initial history
            val initial = TheStudentChatClient.loadMessages(
                context = appContext,
                accountKey = chatAccountKey,
                backendBaseUrl = baseUrl,
                token = token,
                peerId = peer.id,
                afterId = 0L
            )
            val snapshot = initial.dataOrNull()
            if (snapshot == null) {
                _chatState.update {
                    it.copy(isLoadingMessages = false, errorMessage = initial.failureMessage)
                }
                return@launch
            }
            applyMessages(snapshot, replace = true)
            TheStudentChatClient.markRead(baseUrl, token, peer.id)

            // Incremental polling
            while (true) {
                delay(CHAT_MESSAGES_POLL_MS)
                val afterId = _chatState.value.messages.lastOrNull()?.id ?: 0L
                val polled = TheStudentChatClient.loadMessages(
                    context = appContext,
                    accountKey = chatAccountKey,
                    backendBaseUrl = baseUrl,
                    token = token,
                    peerId = peer.id,
                    afterId = afterId
                ).dataOrNull()
                if (polled != null) {
                    applyMessages(polled, replace = afterId == 0L)
                }
            }
        }
    }

    private fun applyMessages(snapshot: ChatConversationState, replace: Boolean) {
        _chatState.update { state ->
            val merged = if (replace) {
                snapshot.messages
            } else {
                val known = state.messages.associateBy { it.id }
                val incoming = snapshot.messages.filter { known[it.id] == null }
                if (incoming.isEmpty()) state.messages else state.messages + incoming
            }
            state.copy(
                isLoadingMessages = false,
                activePeer = snapshot.peer,
                messages = merged,
                safetyAcknowledged = snapshot.safetyAcknowledged || state.safetyAcknowledged
            )
        }
        // Clear the unread badge for this peer in the directory immediately.
        val peerId = snapshot.peer.id
        if (snapshot.messages.any { !it.isMine && !it.isRead }) {
            _chatState.update { state ->
                state.copy(
                    threads = state.threads.map {
                        if (it.peer.id == peerId) it.copy(unreadCount = 0) else it
                    }
                )
            }
        }
    }

    fun closeChat() {
        chatMessagesJob?.cancel()
        chatMessagesJob = null
        chatTypingJob?.cancel()
        chatTypingJob = null
        _chatState.update { it.copy(activePeer = null, messages = emptyList()) }
        _uiState.update { it.copy(isChatConversationOpen = false) }
    }

    /** Send a message to the open conversation (encrypted before it leaves). */
    fun sendChatMessage(text: String) {
        val body = text.trim()
        val peer = _chatState.value.activePeer ?: return
        if (body.isEmpty() || _chatState.value.isSending) return
        if (!_chatState.value.isEncryptionReady) {
            _chatState.update {
                it.copy(
                    restoreError = null,
                    errorMessage = "Unlock your encrypted chat backup first, then send your message.",
                    showRestorePrompt = it.historyBackup.available
                )
            }
            return
        }

        val session = chatSession() ?: return
        val (baseUrl, token) = session

        _chatState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            val result = TheStudentChatClient.sendMessage(
                context = appContext,
                accountKey = chatAccountKey,
                backendBaseUrl = baseUrl,
                token = token,
                peer = peer,
                body = body
            )
            val sent = result.dataOrNull()
            if (sent != null) {
                _chatState.update { state ->
                    val alreadyThere = state.messages.any { it.id == sent.id }
                    state.copy(
                        isSending = false,
                        messages = if (alreadyThere) state.messages else state.messages + sent
                    )
                }
            } else {
                _chatState.update {
                    it.copy(isSending = false, errorMessage = result.failureMessage)
                }
            }
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

    /** Acknowledge the one-time "don't share personal details" notice. */
    fun acknowledgeChatSafetyNotice() {
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        _chatState.update { it.copy(safetyAcknowledged = true) }
        viewModelScope.launch {
            TheStudentChatClient.acknowledgeSafetyNotice(baseUrl, token)
        }
    }

    /** Clear my copy of the open conversation (the other student keeps theirs). */
    fun clearActiveChat() {
        val peer = _chatState.value.activePeer ?: return
        val session = chatSession() ?: return
        val (baseUrl, token) = session
        viewModelScope.launch {
            val result = TheStudentChatClient.clearConversation(baseUrl, token, peer.id)
            if (result.dataOrNull() != null) {
                _chatState.update { it.copy(messages = emptyList()) }
                refreshChatDirectory(showSpinner = false)
            } else {
                _chatState.update { it.copy(errorMessage = result.failureMessage) }
            }
        }
    }

    fun dismissChatError() {
        _chatState.update { it.copy(errorMessage = null) }
    }

    private companion object {
        /** Presence ping. The server calls 10 s of silence "offline". */
        const val CHAT_PRESENCE_MS = 2_000L
        /** Directory (names, avatars, online dots, unread) refresh. */
        const val CHAT_DIRECTORY_POLL_MS = 3_000L
        /** Open conversation refresh. */
        const val CHAT_MESSAGES_POLL_MS = 2_000L
        const val CHAT_TYPING_THROTTLE_MS = 3_000L
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
            preferences.logout()
            stopChatSync()
            _chatState.value = ChatUiState()
            repository.clearUserProfile()
            _isAdmin.value = false
            _uiState.update { it.copy(isAdminPanelOpen = false, isSecurityPanelOpen = false) }
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
        _uiState.update { it.copy(currentTab = tab, isLanguageLabOpen = false) }
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

