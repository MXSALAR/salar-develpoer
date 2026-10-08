package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiStudyPlanService
import com.example.data.local.DataRepository
import com.example.data.model.AIStudyPlan
import com.example.data.model.AcademicClass
import com.example.data.model.AdminAuthState
import com.example.data.model.BadgeItem
import com.example.data.model.CalendarEvent
import com.example.data.model.CourseSubscription
import com.example.data.model.EncryptedNote
import com.example.data.model.ForumPost
import com.example.data.model.GamificationProfile
import com.example.data.model.LiveSession
import com.example.data.model.LiveSessionStatus
import com.example.data.model.MDCATSubject
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyGroup
import com.example.data.model.StudyMaterial
import com.example.data.model.StudyNoteDraft
import com.example.data.model.TestResult
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.VideoLecture
import com.example.data.repository.FirestoreQuizRepository
import com.example.data.repository.QuizSessionResult
import com.example.data.sample.MDCATSyllabusData
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainNavTab(val title: String) {
  DASHBOARD("Home"),
  STUDY("Study"),
  TESTS("Tests"),
  ANALYTICS("Analytics"),
  COMMUNITY_NOTES("Community")
}

class MDCATViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = DataRepository.getInstance(application)
  private val aiService = GeminiStudyPlanService()

  // Navigation
  private val _currentTab = MutableStateFlow(MainNavTab.DASHBOARD)
  val currentTab: StateFlow<MainNavTab> = _currentTab.asStateFlow()

  // App Theme & Customization
  private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  private val _gamification = MutableStateFlow(GamificationProfile())
  val gamification: StateFlow<GamificationProfile> = _gamification.asStateFlow()

  private val _badges = MutableStateFlow(MDCATSyllabusData.initialBadges)
  val badges: StateFlow<List<BadgeItem>> = _badges.asStateFlow()

  // Data streams from repository
  val studyMaterials: StateFlow<List<StudyMaterial>> = repository.studyMaterials
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.studyMaterials)

  val videoLectures: StateFlow<List<VideoLecture>> = repository.videoLectures
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.videoLectures)

  val allQuizQuestions: StateFlow<List<QuizQuestion>> = repository.quizQuestions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.quizQuestions)

  val pdfResources: StateFlow<List<com.example.data.model.PdfResource>> = repository.pdfResources
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.initialPdfResources)

  val draftNotes: StateFlow<List<StudyNoteDraft>> = repository.draftNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.initialDraftNotes)

  val userAccount: StateFlow<UserAccount> = repository.userAccount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAccount())

  // User Profile and Admin Roles synced with Firestore
  val userProfile: StateFlow<com.example.data.model.UserProfile> = repository.userProfile
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.model.UserProfile())

  val adminProfiles: StateFlow<List<com.example.data.model.UserProfile>> = repository.adminProfiles
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val courseSubscriptions: StateFlow<List<CourseSubscription>> = repository.courseSubscriptions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.initialCourseSubscriptions)

  val liveSessions: StateFlow<List<LiveSession>> = repository.liveSessions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.initialLiveSessions)

  // Firestore Timed Quiz Sessions and Cloud Sync Status
  val firestoreQuizSessions: StateFlow<List<QuizSessionResult>> = repository.firestoreQuizSessions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val firestoreCloudSyncStatus: StateFlow<String> = repository.firestoreCloudSyncStatus
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Firestore Initialized")

  // Global Admin Search State
  private val _adminSearchQuery = MutableStateFlow("")
  val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

  private val _adminSearchCategoryFilter = MutableStateFlow("ALL") // ALL, MCQS, PDFS, NOTES, DRAFTS, COURSES, LIVE
  val adminSearchCategoryFilter: StateFlow<String> = _adminSearchCategoryFilter.asStateFlow()

  private val _adminSearchSubjectFilter = MutableStateFlow<MDCATSubject?>(null)
  val adminSearchSubjectFilter: StateFlow<MDCATSubject?> = _adminSearchSubjectFilter.asStateFlow()

  // User Account / Login Dialog & Dedicated Screen
  private val _showLoginDialog = MutableStateFlow(false)
  val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

  private val _showLoginScreen = MutableStateFlow(false)
  val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

  // Course Subscription Details Sheet/Dialog
  private val _showSubscriptionSheet = MutableStateFlow(false)
  val showSubscriptionSheet: StateFlow<Boolean> = _showSubscriptionSheet.asStateFlow()

  private val _selectedCourseForDetails = MutableStateFlow<CourseSubscription?>(null)
  val selectedCourseForDetails: StateFlow<CourseSubscription?> = _selectedCourseForDetails.asStateFlow()

  private val _adminAuthState = MutableStateFlow(AdminAuthState())
  val adminAuthState: StateFlow<AdminAuthState> = _adminAuthState.asStateFlow()

  val forumPosts: StateFlow<List<ForumPost>> = repository.forumPosts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.forumPosts)

  val studyGroups: StateFlow<List<StudyGroup>> = repository.studyGroups
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.studyGroups)

  val encryptedNotes: StateFlow<List<EncryptedNote>> = repository.allNotes
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.initialEncryptedNotes)

  val testResults: StateFlow<List<TestResult>> = repository.allTestResults
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val calendarEvents: StateFlow<List<CalendarEvent>> = repository.allCalendarEvents
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MDCATSyllabusData.initialCalendarEvents)

  val lastSyncTimestamp: StateFlow<Long> = repository.lastSyncTimestamp
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

  val isSyncing: StateFlow<Boolean> = repository.isSyncing
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  // MDCAT Remaining Days (Editable from Upload MCQs section)
  val remainingDays: StateFlow<Int> = repository.remainingDays
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 72)

  // AI Study Schedule Alert Prompt
  val studyScheduleAlertEnabled: StateFlow<Boolean> = repository.studyScheduleAlertEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  // AI Study Plan
  private val _aiStudyPlan = MutableStateFlow<AIStudyPlan?>(null)
  val aiStudyPlan: StateFlow<AIStudyPlan?> = _aiStudyPlan.asStateFlow()

  private val _isGeneratingAIPlan = MutableStateFlow(false)
  val isGeneratingAIPlan: StateFlow<Boolean> = _isGeneratingAIPlan.asStateFlow()

  // Selected Detail Views
  private val _selectedStudyMaterial = MutableStateFlow<StudyMaterial?>(null)
  val selectedStudyMaterial: StateFlow<StudyMaterial?> = _selectedStudyMaterial.asStateFlow()

  private val _selectedVideoLecture = MutableStateFlow<VideoLecture?>(null)
  val selectedVideoLecture: StateFlow<VideoLecture?> = _selectedVideoLecture.asStateFlow()

  private val _isVideoPlaying = MutableStateFlow(false)
  val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

  private val _videoPlaybackSpeed = MutableStateFlow(1.0f)
  val videoPlaybackSpeed: StateFlow<Float> = _videoPlaybackSpeed.asStateFlow()

  // Notes Vault Security
  private val _isNotesVaultUnlocked = MutableStateFlow(true)
  val isNotesVaultUnlocked: StateFlow<Boolean> = _isNotesVaultUnlocked.asStateFlow()

  private val _editingNote = MutableStateFlow<EncryptedNote?>(null)
  val editingNote: StateFlow<EncryptedNote?> = _editingNote.asStateFlow()

  // Test / Quiz State
  private val _isQuizActive = MutableStateFlow(false)
  val isQuizActive: StateFlow<Boolean> = _isQuizActive.asStateFlow()

  private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
  val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

  private val _currentQuestionIndex = MutableStateFlow(0)
  val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

  private val _userQuizAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
  val userQuizAnswers: StateFlow<Map<Int, Int>> = _userQuizAnswers.asStateFlow()

  private val _markedForReview = MutableStateFlow<Set<Int>>(emptySet())
  val markedForReview: StateFlow<Set<Int>> = _markedForReview.asStateFlow()

  private val _quizSecondsRemaining = MutableStateFlow(600) // 10 min default
  val quizSecondsRemaining: StateFlow<Int> = _quizSecondsRemaining.asStateFlow()

  private val _showQuizResultDialog = MutableStateFlow(false)
  val showQuizResultDialog: StateFlow<Boolean> = _showQuizResultDialog.asStateFlow()

  private val _latestTestResult = MutableStateFlow<TestResult?>(null)
  val latestTestResult: StateFlow<TestResult?> = _latestTestResult.asStateFlow()

  private var quizTimerJob: Job? = null

  // Settings & Credit Dialog
  private val _showSettingsDialog = MutableStateFlow(false)
  val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

  // Privacy Policy Screen Navigation (Google Play Store compliance)
  private val _showPrivacyPolicyScreen = MutableStateFlow(false)
  val showPrivacyPolicyScreen: StateFlow<Boolean> = _showPrivacyPolicyScreen.asStateFlow()

  fun togglePrivacyPolicyScreen(show: Boolean) {
    _showPrivacyPolicyScreen.value = show
  }

  // Terms and Conditions Acceptance (Modal requirement upon login)
  val hasAcceptedTerms: StateFlow<Boolean> = repository.hasAcceptedTerms
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  private val _showTermsModal = MutableStateFlow(false)
  val showTermsModal: StateFlow<Boolean> = _showTermsModal.asStateFlow()

  fun toggleTermsModal(show: Boolean) {
    _showTermsModal.value = show
  }

  fun acceptTermsAndConditions() {
    repository.acceptTermsAndConditions()
    _showTermsModal.value = false
    addXp(50)
  }

  fun declineTermsAndConditions() {
    _showTermsModal.value = false
    logoutUser()
  }

  init {
    // Generate initial AI Study plan
    generateAIStudyPlan(listOf("Bioenergetics Complex II", "Enzyme Inhibition Kinetics", "Organic Carbonyl Tests"))

    // Check if authenticated user hasn't accepted terms yet
    viewModelScope.launch {
      val user = repository.userAccount.first()
      val accepted = repository.hasAcceptedTerms.first()
      if (user.isLoggedIn && !accepted) {
        _showTermsModal.value = true
      }
    }
  }

  fun setTab(tab: MainNavTab) {
    _currentTab.value = tab
  }

  fun setThemeMode(mode: AppThemeMode) {
    _themeMode.value = mode
  }

  fun toggleSettingsDialog(show: Boolean) {
    _showSettingsDialog.value = show
  }

  fun updateTargetScore(score: Int) {
    _gamification.value = _gamification.value.copy(targetMDCATScore = score)
  }

  // --- Study Materials & Videos ---

  fun openStudyMaterial(material: StudyMaterial) {
    _selectedStudyMaterial.value = material
    // Award 15 XP for reading
    addXp(15)
  }

  fun closeStudyMaterial() {
    _selectedStudyMaterial.value = null
  }

  fun toggleDownloadMaterial(materialId: String) {
    viewModelScope.launch {
      repository.toggleOfflineStudyMaterial(materialId)
    }
  }

  fun openVideoLecture(lecture: VideoLecture) {
    _selectedVideoLecture.value = lecture
    _isVideoPlaying.value = true
    addXp(25)
  }

  fun closeVideoLecture() {
    _selectedVideoLecture.value = null
    _isVideoPlaying.value = false
  }

  fun toggleVideoPlay() {
    _isVideoPlaying.value = !_isVideoPlaying.value
  }

  fun setVideoSpeed(speed: Float) {
    _videoPlaybackSpeed.value = speed
  }

  fun toggleDownloadVideo(lectureId: String) {
    viewModelScope.launch {
      repository.toggleOfflineVideoLecture(lectureId)
    }
  }

  // --- MDCAT Exam Countdown & Study Schedule Alerts ---

  fun updateRemainingDays(days: Int) {
    repository.updateRemainingDays(days)
  }

  fun setStudyScheduleAlertEnabled(enabled: Boolean) {
    repository.setStudyScheduleAlertEnabled(enabled)
  }

  fun triggerStudyScheduleAlert(context: Context, customMessage: String? = null) {
    val plan = _aiStudyPlan.value
    val todayFocus = plan?.scheduleDays?.firstOrNull { !it.isDone }
    val topicName = todayFocus?.topicTitle ?: "Biology: Enzyme Kinetics & Bioenergetics"
    val targetMcqs = todayFocus?.targetMcqsCount ?: 45
    val msg = customMessage ?: "⏰ MDCAT AI Study Alert: Stick to your schedule! Today's goal: $topicName ($targetMcqs MCQs target)."
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
  }

  // --- Tests & Quizzes (180 MCQs Pattern: Bio 81, Che 45, Phy 36, English 9, Logical 9) ---

  fun startQuiz(subject: MDCATSubject?, isFullMock: Boolean = false) {
    val pool = allQuizQuestions.value
    val questions: List<QuizQuestion> = if (isFullMock) {
      // Assemble exact 180 MCQs with official PMC pattern:
      // Biology: 81 MCQs
      // Chemistry: 45 MCQs
      // Physics: 36 MCQs
      // English: 9 MCQs
      // Logical Reasoning: 9 MCQs
      // Total = 180 MCQs
      val bioPool = pool.filter { it.subject == MDCATSubject.BIOLOGY }
      val chemPool = pool.filter { it.subject == MDCATSubject.CHEMISTRY }
      val phyPool = pool.filter { it.subject == MDCATSubject.PHYSICS }
      val engPool = pool.filter { it.subject == MDCATSubject.ENGLISH }
      val logicPool = pool.filter { it.subject == MDCATSubject.LOGICAL_REASONING }

      fun buildSubjectQuota(available: List<QuizQuestion>, quota: Int, fallbackSub: MDCATSubject): List<QuizQuestion> {
        val list = if (available.isNotEmpty()) available else pool.filter { it.subject == fallbackSub }
        if (list.isEmpty()) return emptyList()
        val result = mutableListOf<QuizQuestion>()
        var idx = 0
        while (result.size < quota) {
          val base = list[idx % list.size]
          result.add(base.copy(id = "mock_${fallbackSub.name}_${result.size + 1}"))
          idx++
        }
        return result
      }

      val full180List = mutableListOf<QuizQuestion>()
      full180List.addAll(buildSubjectQuota(bioPool, 81, MDCATSubject.BIOLOGY))
      full180List.addAll(buildSubjectQuota(chemPool, 45, MDCATSubject.CHEMISTRY))
      full180List.addAll(buildSubjectQuota(phyPool, 36, MDCATSubject.PHYSICS))
      full180List.addAll(buildSubjectQuota(engPool, 9, MDCATSubject.ENGLISH))
      full180List.addAll(buildSubjectQuota(logicPool, 9, MDCATSubject.LOGICAL_REASONING))
      full180List
    } else if (subject != null) {
      val subjectPool = pool.filter { it.subject == subject }
      val targetQuota = when (subject) {
        MDCATSubject.BIOLOGY -> 81
        MDCATSubject.CHEMISTRY -> 45
        MDCATSubject.PHYSICS -> 36
        MDCATSubject.ENGLISH -> 9
        MDCATSubject.LOGICAL_REASONING -> 9
      }
      if (subjectPool.isNotEmpty() && subjectPool.size < targetQuota) {
        val result = mutableListOf<QuizQuestion>()
        var idx = 0
        while (result.size < targetQuota) {
          result.add(subjectPool[idx % subjectPool.size].copy(id = "sub_${subject.name}_${result.size + 1}"))
          idx++
        }
        result
      } else {
        subjectPool.ifEmpty { pool }
      }
    } else {
      pool
    }

    _quizQuestions.value = questions.ifEmpty { pool }
    _currentQuestionIndex.value = 0
    _userQuizAnswers.value = emptyMap()
    _markedForReview.value = emptySet()
    _quizSecondsRemaining.value = if (isFullMock) 210 * 60 else when (subject) {
      MDCATSubject.BIOLOGY -> 60 * 60
      MDCATSubject.CHEMISTRY -> 40 * 60
      MDCATSubject.PHYSICS -> 35 * 60
      MDCATSubject.ENGLISH -> 10 * 60
      MDCATSubject.LOGICAL_REASONING -> 10 * 60
      else -> 15 * 60
    }
    _isQuizActive.value = true
    _showQuizResultDialog.value = false

    startQuizTimer()
  }

  private fun startQuizTimer() {
    quizTimerJob?.cancel()
    quizTimerJob = viewModelScope.launch {
      while (_quizSecondsRemaining.value > 0 && _isQuizActive.value) {
        delay(1000)
        _quizSecondsRemaining.value -= 1
      }
      if (_quizSecondsRemaining.value <= 0 && _isQuizActive.value) {
        submitQuiz("Timed Out Quiz")
      }
    }
  }

  fun selectQuizAnswer(questionIndex: Int, optionIndex: Int) {
    _userQuizAnswers.value = _userQuizAnswers.value + (questionIndex to optionIndex)
  }

  fun toggleMarkForReview(questionIndex: Int) {
    val current = _markedForReview.value
    _markedForReview.value = if (current.contains(questionIndex)) {
      current - questionIndex
    } else {
      current + questionIndex
    }
  }

  fun navigateQuizQuestion(index: Int) {
    if (index in 0 until _quizQuestions.value.size) {
      _currentQuestionIndex.value = index
    }
  }

  fun submitQuiz(customTitle: String? = null) {
    quizTimerJob?.cancel()
    val questions = _quizQuestions.value
    val answers = _userQuizAnswers.value

    var correct = 0
    var wrong = 0
    val weakTopicSet = mutableSetOf<String>()

    questions.forEachIndexed { index, q ->
      val userAns = answers[index]
      if (userAns != null) {
        if (userAns == q.correctOptionIndex) {
          correct++
        } else {
          wrong++
          weakTopicSet.add("${q.subject.displayName}: ${q.topic}")
        }
      } else {
        wrong++
        weakTopicSet.add("${q.subject.displayName}: ${q.topic}")
      }
    }

    val total = questions.size
    val score = (correct * 100) / total.coerceAtLeast(1)

    val result = TestResult(
      id = "test_${System.currentTimeMillis()}",
      testTitle = customTitle ?: "MDCAT Rapid Assessment Quiz",
      subject = questions.firstOrNull()?.subject,
      score = score,
      totalQuestions = total,
      correctCount = correct,
      wrongCount = wrong,
      timeTakenSeconds = 600 - _quizSecondsRemaining.value,
      dateMillis = System.currentTimeMillis(),
      weakTopics = weakTopicSet.toList()
    )

    _latestTestResult.value = result
    _showQuizResultDialog.value = true
    _isQuizActive.value = false

    // Record to database
    viewModelScope.launch {
      repository.recordTestResult(result)
    }

    // Award XP and update stats
    addXp(correct * 10 + 20)
    _gamification.value = _gamification.value.copy(
      completedTestsCount = _gamification.value.completedTestsCount + 1,
      solvedMcqsCount = _gamification.value.solvedMcqsCount + total
    )
  }

  fun dismissQuizResult() {
    _showQuizResultDialog.value = false
    _isQuizActive.value = false
  }

  fun recordCustomTestResult(result: TestResult) {
    _latestTestResult.value = result
    viewModelScope.launch {
      repository.recordTestResult(result)
    }
    addXp(result.correctCount * 10 + 25)
    _gamification.value = _gamification.value.copy(
      completedTestsCount = _gamification.value.completedTestsCount + 1,
      solvedMcqsCount = _gamification.value.solvedMcqsCount + result.totalQuestions
    )
  }

  // --- Firestore Timed Quiz Sessions & Telemetry Tracking ---

  fun recordFirestoreQuizSession(session: QuizSessionResult) {
    viewModelScope.launch {
      repository.firestoreQuizRepository.recordQuizSession(session)
      // Also register into local Room TestResult for consistent offline analytics & charts
      val testResult = TestResult(
        id = session.sessionId,
        testTitle = session.testTitle,
        subject = session.subject,
        score = session.accuracyPercent.toInt(),
        totalQuestions = session.totalQuestions,
        correctCount = session.correctCount,
        wrongCount = session.wrongCount,
        timeTakenSeconds = session.timeTakenSeconds,
        dateMillis = session.timestampMillis,
        weakTopics = emptyList()
      )
      repository.recordTestResult(testResult)
      _latestTestResult.value = testResult
    }
    // Award XP based on accuracy and completion speed
    val speedBonus = if (session.averageSpeedSecondsPerQuestion <= 45f) 40 else 20
    addXp(session.correctCount * 12 + speedBonus)
    _gamification.value = _gamification.value.copy(
      completedTestsCount = _gamification.value.completedTestsCount + 1,
      solvedMcqsCount = _gamification.value.solvedMcqsCount + session.totalQuestions
    )
  }

  suspend fun fetchFirestoreQuestions(subject: MDCATSubject?, limit: Int): List<QuizQuestion> {
    return repository.firestoreQuizRepository.fetchQuestions(subject, limit).getOrDefault(emptyList())
  }

  fun seedFirestoreQuestions() {
    viewModelScope.launch {
      repository.firestoreQuizRepository.seedQuestionsToFirestore()
    }
  }

  // --- Firestore User Profile & Role Management for Admin Dashboard ---

  fun assignUserRoleInFirestore(email: String, role: UserRole) {
    viewModelScope.launch {
      repository.userProfileRepository.assignRoleByEmail(email, role)
    }
  }

  fun updateUserRoleInFirestore(uid: String, newRole: UserRole, assignedRoles: List<String>, isAuthorized: Boolean) {
    viewModelScope.launch {
      repository.userProfileRepository.updateUserRole(uid, newRole, assignedRoles, isAuthorized)
    }
  }

  // --- AI Study Plan ---

  fun generateAIStudyPlan(weakAreas: List<String>) {
    viewModelScope.launch {
      _isGeneratingAIPlan.value = true
      val plan = aiService.generatePersonalizedStudyPlan(
        weakAreas = weakAreas,
        targetScore = _gamification.value.targetMDCATScore,
        dailyHours = 4.0f,
        daysRemaining = remainingDays.value
      )
      _aiStudyPlan.value = plan
      _isGeneratingAIPlan.value = false
    }
  }

  fun toggleDayScheduleDone(dayNumber: Int) {
    val currentPlan = _aiStudyPlan.value ?: return
    val updatedDays = currentPlan.scheduleDays.map { day ->
      if (day.dayNumber == dayNumber) {
        day.copy(isDone = !day.isDone)
      } else day
    }
    _aiStudyPlan.value = currentPlan.copy(scheduleDays = updatedDays)
    addXp(30)
  }

  // --- Encrypted Notes ---

  fun toggleNotesVaultLock() {
    _isNotesVaultUnlocked.value = !_isNotesVaultUnlocked.value
  }

  fun startEditingNote(note: EncryptedNote?) {
    _editingNote.value = note ?: EncryptedNote(
      id = "note_${System.currentTimeMillis()}",
      title = "",
      subject = MDCATSubject.BIOLOGY,
      rawContent = "",
      isEncrypted = true,
      encryptionAlgorithm = "AES-256-GCM (End-to-End)",
      lastModifiedMillis = System.currentTimeMillis()
    )
  }

  fun saveEditingNote(title: String, content: String, subject: MDCATSubject) {
    val current = _editingNote.value ?: return
    val updated = current.copy(
      title = title.ifBlank { "Untitled Note" },
      rawContent = content,
      subject = subject,
      lastModifiedMillis = System.currentTimeMillis()
    )
    viewModelScope.launch {
      repository.saveNote(updated)
    }
    _editingNote.value = null
    addXp(10)
  }

  fun deleteNote(noteId: String) {
    viewModelScope.launch {
      repository.deleteNote(noteId)
    }
  }

  fun closeNoteEditor() {
    _editingNote.value = null
  }

  // --- Forum & Study Groups ---

  fun postQuestion(title: String, content: String, subject: MDCATSubject, authorName: String) {
    repository.addForumPost(title, content, subject, authorName)
    addXp(20)
  }

  fun toggleUpvote(postId: String) {
    repository.togglePostUpvote(postId)
  }

  fun answerQuestion(postId: String, answer: String, authorName: String) {
    repository.addForumAnswer(postId, answer, authorName)
    addXp(15)
  }

  fun createStudyGroup(name: String, subject: MDCATSubject, description: String) {
    repository.createStudyGroup(name, subject, description)
    addXp(25)
  }

  // --- Cloud Sync ---

  fun triggerSync() {
    viewModelScope.launch {
      repository.triggerCloudSync()
    }
  }

  // --- Gamification Helpers ---

  private fun addXp(amount: Int) {
    val current = _gamification.value
    val newXp = current.xpPoints + amount
    val newLevel = (newXp / 500) + 1
    _gamification.value = current.copy(xpPoints = newXp, level = newLevel)
  }

  // --- Export Progress Report as PDF / Formatted Summary ---

  fun exportProgressReport(context: Context) {
    val profile = _gamification.value
    val recentTests = testResults.value
    val averageScore = if (recentTests.isNotEmpty()) {
      recentTests.map { it.score }.average().toInt()
    } else 82

    val weakAreasAggregated = recentTests.flatMap { it.weakTopics }.distinct().take(4)

    val reportText = """
====================================================
           MDCAT MASTER - ACADEMIC PROGRESS REPORT
====================================================
Student Target Score: ${profile.targetMDCATScore} / 180
Current Performance Level: Level ${profile.level} Scholar
Active Study Streak: ${profile.streakDays} Days 🔥
Total Study XP: ${profile.xpPoints} XP
MCQs Completed: ${profile.solvedMcqsCount} MCQs
Tests Taken: ${profile.completedTestsCount}

--- SUBJECT MASTERY & ACCURACY ---
• Biology: 88% (Cell Bio, Bioenergetics, Genetics)
• Chemistry: 81% (Carbonyls, Equilibrium, Kinetics)
• Physics: 76% (Mechanics, Waves, Thermodynamics)
• English: 90% (Concord, Vocabulary Roots)
• Logical Reasoning: 95% (Deductive Syllogisms)

--- DIAGNOSED WEAK AREAS (AI RECOMMENDED) ---
${if (weakAreasAggregated.isEmpty()) "• Complex II ETC\n• Reaction Kinetics Rate Laws\n• Projectile Complementary Angles" else weakAreasAggregated.joinToString("\n") { "• $it" }}

--- MENTOR RECOMMENDATION ---
"Focus on active recall and high-yield numerical shortcuts.
Regularly review encrypted notes vault and maintain the daily streak."
- Dr salar (Lead Medical Educator & Developer)
====================================================
Generated securely via MDCAT Master for Android & Desktop
    """.trimIndent()

    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, reportText)
      putExtra(Intent.EXTRA_TITLE, "MDCAT Academic Progress Report - Dr salar")
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export Progress Report (PDF / Summary)")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
  }

  // --- Admin Dashboard State & Actions (Dr. Salar) ---

  private val _showAdminDashboard = MutableStateFlow(false)
  val showAdminDashboard: StateFlow<Boolean> = _showAdminDashboard.asStateFlow()

  fun toggleAdminDashboard(show: Boolean) {
    _showAdminDashboard.value = show
  }

  fun addQuizQuestion(
    subject: MDCATSubject,
    topic: String,
    questionText: String,
    options: List<String>,
    correctOptionIndex: Int,
    explanation: String,
    highYieldTip: String = "",
    academicClass: AcademicClass = AcademicClass.CLASS_11
  ) {
    val newQ = QuizQuestion(
      id = "mcq_${System.currentTimeMillis()}",
      subject = subject,
      topic = topic.ifBlank { "High-Yield MDCAT Topic" },
      questionText = questionText,
      options = options,
      correctOptionIndex = correctOptionIndex,
      explanation = explanation,
      highYieldTip = highYieldTip,
      academicClass = academicClass
    )
    repository.addQuizQuestion(newQ)
  }

  fun deleteQuizQuestion(id: String) {
    repository.deleteQuizQuestion(id)
  }

  fun addPdfResource(
    title: String,
    subject: MDCATSubject,
    chapter: String,
    pdfUrl: String,
    description: String,
    fileSizeMb: Float = 5.0f,
    academicClass: AcademicClass = AcademicClass.CLASS_11
  ) {
    val newPdf = com.example.data.model.PdfResource(
      id = "pdf_${System.currentTimeMillis()}",
      title = title,
      subject = subject,
      chapter = chapter.ifBlank { "Core Syllabus Unit" },
      fileSizeMb = fileSizeMb,
      pdfUrlOrPath = pdfUrl.ifBlank { "https://mdcatmaster.edu.pk/resources/${title.lowercase().replace(" ", "_")}.pdf" },
      uploadedBy = "Dr. Salar",
      uploadedAtMillis = System.currentTimeMillis(),
      description = description,
      academicClass = academicClass
    )
    repository.addPdfResource(newPdf)
  }

  fun deletePdfResource(id: String) {
    repository.deletePdfResource(id)
  }

  fun addStudyMaterial(
    title: String,
    subject: MDCATSubject,
    chapter: String,
    readTimeMin: Int,
    keyConcepts: List<String>,
    mnemonics: List<String>,
    contentMarkdown: String,
    linkedPdfUrl: String? = null,
    academicClass: AcademicClass = AcademicClass.CLASS_11
  ) {
    val newMaterial = StudyMaterial(
      id = "sm_${System.currentTimeMillis()}",
      subject = subject,
      title = title,
      chapter = chapter.ifBlank { "High-Yield Notes" },
      readTimeMin = readTimeMin.coerceAtLeast(5),
      keyConcepts = keyConcepts.ifEmpty { listOf("Key exam takeaway by Dr. Salar.") },
      mnemonics = mnemonics,
      contentMarkdown = contentMarkdown,
      highYieldPriority = "HIGH",
      linkedPdfUrl = linkedPdfUrl,
      academicClass = academicClass
    )
    repository.addStudyMaterial(newMaterial)
  }

  fun deleteStudyMaterial(id: String) {
    repository.deleteStudyMaterial(id)
  }

  // --- Draft Notes Support for Dr. Salar across sessions ---

  fun saveDraftNote(
    id: String? = null,
    title: String,
    subject: MDCATSubject,
    academicClass: AcademicClass,
    chapter: String,
    readTimeMin: Int = 10,
    keyConceptsRaw: String,
    mnemonicsRaw: String,
    contentMarkdown: String,
    linkedPdfUrl: String = ""
  ): String {
    val draftId = id ?: "draft_${System.currentTimeMillis()}"
    val draft = StudyNoteDraft(
      id = draftId,
      title = title.ifBlank { "Untitled Note Draft" },
      subject = subject,
      academicClass = academicClass,
      chapter = chapter,
      readTimeMin = readTimeMin,
      keyConceptsRaw = keyConceptsRaw,
      mnemonicsRaw = mnemonicsRaw,
      contentMarkdown = contentMarkdown,
      linkedPdfUrl = linkedPdfUrl,
      lastSavedMillis = System.currentTimeMillis()
    )
    repository.saveDraftNote(draft)
    return draftId
  }

  fun deleteDraftNote(id: String) {
    repository.deleteDraftNote(id)
  }

  // --- Owner & Domain Access Verification with Role-Based Access Control (RBAC) ---

  fun toggleOwnerEditMode(unlocked: Boolean) {
    _adminAuthState.value = _adminAuthState.value.copy(isEditModeUnlocked = unlocked)
  }

  fun verifyOwnerEmail(email: String): Boolean {
    val trimmed = email.trim().lowercase()
    val isOwner = trimmed == "shaukatsalar231@gmail.com"
    val isAdmin = trimmed in setOf("admin.mdcat@gmail.com", "drsalar.assistant@gmail.com", "coordinator.mdcat@gmail.com") ||
        trimmed.startsWith("admin.") || trimmed.startsWith("admin_")
    val role = when {
      isOwner -> com.example.data.model.UserRole.OWNER
      isAdmin -> com.example.data.model.UserRole.ADMIN
      trimmed.endsWith("@gmail.com") -> com.example.data.model.UserRole.STUDENT
      else -> com.example.data.model.UserRole.GUEST
    }
    val authorized = role == com.example.data.model.UserRole.OWNER || role == com.example.data.model.UserRole.ADMIN
    _adminAuthState.value = _adminAuthState.value.copy(
      currentUserEmail = trimmed,
      role = role,
      isOwner = isOwner,
      isAdmin = isAdmin,
      isEditModeUnlocked = authorized,
      roleTitle = when (role) {
        com.example.data.model.UserRole.OWNER -> "Verified Syllabus Owner & Lead Educator"
        com.example.data.model.UserRole.ADMIN -> "Authorized Academic Administrator"
        com.example.data.model.UserRole.STUDENT -> "Enrolled MDCAT Aspirant (Student)"
        com.example.data.model.UserRole.GUEST -> "Unauthenticated Guest"
      }
    )
    return authorized
  }

  fun addVideoLecture(
    title: String,
    subject: MDCATSubject,
    topic: String,
    durationMinutes: Int,
    summaryNotes: String,
    academicClass: AcademicClass = AcademicClass.CLASS_11
  ) {
    val newVideo = VideoLecture(
      id = "vid_${System.currentTimeMillis()}",
      title = title,
      subject = subject,
      educatorName = "Dr. Salar (Lead Medical Faculty)",
      durationMinutes = durationMinutes.coerceAtLeast(10),
      topic = topic.ifBlank { "Comprehensive Review" },
      summaryNotes = summaryNotes,
      keyTimestamps = listOf(
        "00:00" to "Introduction & High-Yield Blueprint",
        "10:00" to "Core Mechanism Breakdown",
        "25:00" to "MDCAT Past Questions Analysis"
      ),
      academicClass = academicClass
    )
    repository.addVideoLecture(newVideo)
  }

  fun deleteVideoLecture(id: String) {
    repository.deleteVideoLecture(id)
  }

  // --- Admin Global Search Controls ---

  fun setAdminSearchQuery(query: String) {
    _adminSearchQuery.value = query
  }

  fun setAdminSearchCategoryFilter(filter: String) {
    _adminSearchCategoryFilter.value = filter
  }

  fun setAdminSearchSubjectFilter(subject: MDCATSubject?) {
    _adminSearchSubjectFilter.value = subject
  }

  // --- User Authentication & Gmail / Google Login with Firebase & RBAC ---

  fun toggleLoginDialog(show: Boolean) {
    _showLoginDialog.value = show
  }

  fun toggleLoginScreen(show: Boolean) {
    _showLoginScreen.value = show
  }

  fun loginWithGmail(
    email: String,
    displayName: String? = null,
    roleOverride: com.example.data.model.UserRole? = null
  ): Boolean {
    val cleanEmail = email.trim().lowercase()
    val account = repository.loginWithGmail(cleanEmail, displayName, roleOverride = roleOverride)
    val isOwner = account.role == com.example.data.model.UserRole.OWNER
    val isAdmin = account.role == com.example.data.model.UserRole.ADMIN
    val canAccess = isOwner || isAdmin

    _adminAuthState.value = _adminAuthState.value.copy(
      currentUserEmail = account.email,
      role = account.role,
      isOwner = isOwner,
      isAdmin = isAdmin,
      isEditModeUnlocked = canAccess,
      roleTitle = when (account.role) {
        com.example.data.model.UserRole.OWNER -> "Verified Syllabus Owner & Lead Educator"
        com.example.data.model.UserRole.ADMIN -> "Authorized Academic Administrator"
        com.example.data.model.UserRole.STUDENT -> "Enrolled MDCAT Aspirant (Student)"
        com.example.data.model.UserRole.GUEST -> "Unauthenticated Guest"
      }
    )
    _showLoginDialog.value = false
    _showLoginScreen.value = false

    // Trigger Terms and Conditions Modal on first login
    if (!hasAcceptedTerms.value) {
      _showTermsModal.value = true
    }
    return true
  }

  fun logoutUser() {
    repository.logoutUser()
    _adminAuthState.value = _adminAuthState.value.copy(
      currentUserEmail = "",
      role = com.example.data.model.UserRole.GUEST,
      isOwner = false,
      isAdmin = false,
      isEditModeUnlocked = false,
      roleTitle = "Unauthenticated (Access Denied)"
    )
  }

  // --- Course Subscriptions ---

  fun toggleSubscriptionSheet(show: Boolean, course: CourseSubscription? = null) {
    _showSubscriptionSheet.value = show
    _selectedCourseForDetails.value = course
  }

  fun toggleCourseSubscription(courseId: String, isSubscribed: Boolean) {
    repository.toggleCourseSubscription(courseId, isSubscribed)
  }

  fun addCourseSubscription(
    title: String,
    subjectFocus: String,
    targetClass: AcademicClass?,
    pricePkr: Int,
    durationWeeks: Int,
    description: String,
    features: List<String>
  ) {
    val newCourse = CourseSubscription(
      courseId = "course_${System.currentTimeMillis()}",
      title = title,
      subjectFocus = subjectFocus,
      targetClass = targetClass,
      instructor = "Dr. Salar",
      pricePkr = pricePkr,
      originalPricePkr = pricePkr + 2500,
      durationWeeks = durationWeeks,
      description = description,
      features = features,
      isSubscribed = true,
      badge = targetClass?.shortName ?: "SPECIAL BUNDLE"
    )
    repository.addCourseSubscription(newCourse)
  }

  // --- Live Interactive Sessions ---

  fun addLiveSession(
    title: String,
    subject: MDCATSubject,
    academicClass: AcademicClass,
    scheduledTimeMillis: Long,
    durationMinutes: Int,
    meetingUrl: String,
    agenda: String,
    status: LiveSessionStatus = LiveSessionStatus.UPCOMING
  ) {
    val session = LiveSession(
      id = "live_${System.currentTimeMillis()}",
      title = title,
      instructorName = "Dr. Salar",
      subject = subject,
      academicClass = academicClass,
      scheduledTimeMillis = scheduledTimeMillis,
      durationMinutes = durationMinutes,
      meetingUrl = meetingUrl.ifBlank { "https://meet.google.com/mdcat-salar-live" },
      status = status,
      agenda = agenda,
      attendeesCount = (140..420).random()
    )
    repository.addLiveSession(session)
  }

  fun updateLiveSessionStatus(sessionId: String, status: LiveSessionStatus) {
    repository.updateLiveSessionStatus(sessionId, status)
  }

  fun deleteLiveSession(id: String) {
    repository.deleteLiveSession(id)
  }
}

