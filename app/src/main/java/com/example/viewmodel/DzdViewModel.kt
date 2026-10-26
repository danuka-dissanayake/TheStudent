package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DzdDatabase
import com.example.data.local.LearningActivityEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SavedPaperEntity
import com.example.data.model.*
import com.example.data.repository.DzdRepository
import com.example.data.repository.SampleEducationalData
import com.example.ui.components.DzdNavTab
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

import com.example.data.remote.DzdAiStudyResponse
import com.example.data.remote.DzdOcrEngine
import com.example.data.remote.DzdStudyAiClient
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
import kotlinx.coroutines.withContext

enum class AppScreenFlow {
    SPLASH,
    ONBOARDING,
    AUTH,
    MAIN
}

data class DzdUiState(
    val screenFlow: AppScreenFlow = AppScreenFlow.SPLASH,
    val isLoadingContent: Boolean = false,
    val isRegisterAuthMode: Boolean = false,
    val currentTab: DzdNavTab = DzdNavTab.HOME,
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
    val activeChatConversation: ChatConversation? = null
)

class DzdViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = DzdDatabase.getDatabase(application)
    private val repository: DzdRepository = DzdRepository(database.dzdDao())
    val pastPapersRepository = PastPapersRemoteRepository(application)
    private val huggingFaceClient = HuggingFaceSpaceClient()
    private val dzdStudyAiClient = DzdStudyAiClient(application)
    private val dzdOcrEngine = DzdOcrEngine(application)
    private var aiAssistantJob: Job? = null
    private var ocrJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val conversationTurns = mutableListOf<Pair<String, String>>()

    private val _uiState = MutableStateFlow(DzdUiState())
    val uiState: StateFlow<DzdUiState> = _uiState.asStateFlow()

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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeDefaultDataIfEmpty()
        }
        loadDynamicPapers()
    }

    private fun initTts() {
        if (tts == null) {
            try {
                tts = TextToSpeech(getApplication(), this)
            } catch (e: Exception) {
                Log.w("DzdViewModel", "TTS initialization deferred/unavailable", e)
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
                            kotlinx.coroutines.delay(800)
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
                    isLoggedIn = entity.isLoggedIn
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
                Log.e("DzdViewModel", "Safe error handling in loadDynamicPapers: ${e.message}", e)
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
        _uiState.update { it.copy(isAdminPanelOpen = true) }
    }

    fun closeAdminPanel() {
        _uiState.update { it.copy(isAdminPanelOpen = false) }
    }

    fun openChat(conversation: ChatConversation) {
        _uiState.update { it.copy(activeChatConversation = conversation) }
    }

    fun closeChat() {
        _uiState.update { it.copy(activeChatConversation = null) }
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

    // App Flow Transitions
    fun finishSplash() {
        _uiState.update { it.copy(screenFlow = AppScreenFlow.ONBOARDING) }
    }

    fun finishOnboarding(openAuth: Boolean, isRegister: Boolean = false) {
        if (openAuth) {
            _uiState.update { it.copy(screenFlow = AppScreenFlow.AUTH, isRegisterAuthMode = isRegister) }
        } else {
            _uiState.update { it.copy(screenFlow = AppScreenFlow.MAIN) }
            simulateSkeletonRefresh()
        }
    }

    fun openAuthScreen(isRegister: Boolean = false) {
        _uiState.update { it.copy(screenFlow = AppScreenFlow.AUTH, isRegisterAuthMode = isRegister) }
    }

    fun handleAuthSuccess(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _uiState.update { it.copy(screenFlow = AppScreenFlow.MAIN) }
            showSnackbar("Welcome to TheStudent, ${profile.name.split(" ").first()}!")
            simulateSkeletonRefresh()
        }
    }

    fun continueAsGuest() {
        _uiState.update { it.copy(screenFlow = AppScreenFlow.MAIN) }
        simulateSkeletonRefresh()
    }

    fun simulateSkeletonRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContent = true) }
            kotlinx.coroutines.delay(450)
            _uiState.update { it.copy(isLoadingContent = false) }
        }
    }

    // Navigation and tabs
    fun setTab(tab: DzdNavTab) {
        val wasDifferent = _uiState.value.currentTab != tab
        _uiState.update { it.copy(currentTab = tab, isLanguageLabOpen = false) }
        if (wasDifferent && (tab == DzdNavTab.HOME || tab == DzdNavTab.CHAT || tab == DzdNavTab.PAPERS)) {
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
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dzd_tts_id")
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
                val result = dzdOcrEngine.extractTextFromImage(uri, preferredLang)
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

            val response = dzdStudyAiClient.streamStudyResponse(
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
                is DzdAiStudyResponse.Success -> {
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
                is DzdAiStudyResponse.Error -> {
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

            val response = dzdStudyAiClient.streamStudyResponse(
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
                is DzdAiStudyResponse.Success -> {
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
                is DzdAiStudyResponse.Error -> {
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

