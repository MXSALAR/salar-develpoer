package com.example.data.local

import android.content.Context
import androidx.room.Room
import com.example.data.model.CalendarEvent
import com.example.data.model.CalendarEventType
import com.example.data.model.EncryptedNote
import com.example.data.model.ForumPost
import com.example.data.model.MDCATSubject
import com.example.data.model.StudyGroup
import com.example.data.model.StudyMaterial
import com.example.data.model.TestResult
import com.example.data.model.VideoLecture
import com.example.data.sample.MDCATSyllabusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DataRepository private constructor(context: Context) {

  private val appContext = context.applicationContext

  private val database: AppDatabase = Room.databaseBuilder(
    appContext,
    AppDatabase::class.java,
    "mdcat_master.db"
  ).fallbackToDestructiveMigration(dropAllTables = true).build()

  private val noteDao = database.noteDao()
  private val testResultDao = database.testResultDao()
  private val calendarEventDao = database.calendarEventDao()
  private val offlineDao = database.offlineDao()
  private val quizQuestionDao = database.quizQuestionDao()

  // User Profile Repository managing Firestore storage and Admin Dashboard roles
  val userProfileRepository = com.example.data.repository.UserProfileRepository.getInstance(context)
  val userProfile: Flow<com.example.data.model.UserProfile> = userProfileRepository.activeUserProfile
  val adminProfiles: Flow<List<com.example.data.model.UserProfile>> = userProfileRepository.adminProfiles

  // Firestore Quiz Repository managing cloud questions and student speed/accuracy tracking
  val firestoreQuizRepository = com.example.data.repository.FirestoreQuizRepository.getInstance(context)
  val firestoreQuizSessions: Flow<List<com.example.data.repository.QuizSessionResult>> = firestoreQuizRepository.recentSessions
  val firestoreCloudSyncStatus: Flow<String> = firestoreQuizRepository.cloudSyncStatus

  // Terms and Conditions Acceptance (Google Play Compliance & Legal Onboarding)
  private val prefs = appContext.getSharedPreferences("mdcat_user_prefs", Context.MODE_PRIVATE)
  private val _hasAcceptedTerms = MutableStateFlow(prefs.getBoolean("has_accepted_terms", false))
  val hasAcceptedTerms: Flow<Boolean> = _hasAcceptedTerms.asStateFlow()

  fun acceptTermsAndConditions() {
    prefs.edit().putBoolean("has_accepted_terms", true).apply()
    _hasAcceptedTerms.value = true
  }

  fun resetTermsAcceptance() {
    prefs.edit().putBoolean("has_accepted_terms", false).apply()
    _hasAcceptedTerms.value = false
  }

  // In-memory reactive state for dynamic forums, study groups, materials
  private val _forumPosts = MutableStateFlow(MDCATSyllabusData.forumPosts)
  val forumPosts: Flow<List<ForumPost>> = _forumPosts.asStateFlow()

  private val _studyGroups = MutableStateFlow(MDCATSyllabusData.studyGroups)
  val studyGroups: Flow<List<StudyGroup>> = _studyGroups.asStateFlow()

  private val _studyMaterials = MutableStateFlow(MDCATSyllabusData.studyMaterials)
  val studyMaterials: Flow<List<StudyMaterial>> = _studyMaterials.asStateFlow()

  private val _videoLectures = MutableStateFlow(MDCATSyllabusData.videoLectures)
  val videoLectures: Flow<List<VideoLecture>> = _videoLectures.asStateFlow()

  // Quiz questions fetched directly from local Room Database
  val quizQuestions: Flow<List<com.example.data.model.QuizQuestion>> =
    quizQuestionDao.getAllQuestions().map { entities ->
      if (entities.isEmpty()) {
        MDCATSyllabusData.quizQuestions
      } else {
        entities.map { it.toQuizQuestion() }
      }
    }

  private val _pdfResources = MutableStateFlow(MDCATSyllabusData.initialPdfResources)
  val pdfResources: Flow<List<com.example.data.model.PdfResource>> = _pdfResources.asStateFlow()

  private val _draftNotes = MutableStateFlow(MDCATSyllabusData.initialDraftNotes)
  val draftNotes: Flow<List<com.example.data.model.StudyNoteDraft>> = _draftNotes.asStateFlow()

  // User Account & Authentication
  private val _userAccount = MutableStateFlow(com.example.data.model.UserAccount())
  val userAccount: Flow<com.example.data.model.UserAccount> = _userAccount.asStateFlow()

  // Course Subscriptions & Enrollments
  private val _courseSubscriptions = MutableStateFlow(MDCATSyllabusData.initialCourseSubscriptions)
  val courseSubscriptions: Flow<List<com.example.data.model.CourseSubscription>> = _courseSubscriptions.asStateFlow()

  // Live Interactive Sessions
  private val _liveSessions = MutableStateFlow(MDCATSyllabusData.initialLiveSessions)
  val liveSessions: Flow<List<com.example.data.model.LiveSession>> = _liveSessions.asStateFlow()

  // Cloud & Cross-platform Sync State
  private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis() - 15 * 60 * 1000)
  val lastSyncTimestamp: Flow<Long> = _lastSyncTimestamp.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()

  // MDCAT Exam Remaining Days (Editable from Upload MCQs section by Dr. Salar)
  private val _remainingDays = MutableStateFlow(72)
  val remainingDays: Flow<Int> = _remainingDays.asStateFlow()

  fun updateRemainingDays(days: Int) {
    _remainingDays.value = days.coerceAtLeast(0)
  }

  // AI Study Schedule Alert Prompt Setting
  private val _studyScheduleAlertEnabled = MutableStateFlow(true)
  val studyScheduleAlertEnabled: Flow<Boolean> = _studyScheduleAlertEnabled.asStateFlow()

  fun setStudyScheduleAlertEnabled(enabled: Boolean) {
    _studyScheduleAlertEnabled.value = enabled
  }

  init {
    // Populate initial notes and calendar events if empty
    CoroutineScope(Dispatchers.IO).launch {
      MDCATSyllabusData.initialEncryptedNotes.forEach { note ->
        noteDao.insertNote(
          EncryptedNoteEntity(
            id = note.id,
            title = note.title,
            subjectName = note.subject.name,
            rawContent = note.rawContent,
            isEncrypted = note.isEncrypted,
            encryptionAlgorithm = note.encryptionAlgorithm,
            lastModifiedMillis = note.lastModifiedMillis,
            isPinned = note.isPinned
          )
        )
      }

      MDCATSyllabusData.initialCalendarEvents.forEach { event ->
        calendarEventDao.insertEvent(
          CalendarEventEntity(
            id = event.id,
            title = event.title,
            dateMillis = event.dateMillis,
            eventTypeName = event.eventType.name,
            notes = event.notes,
            isCompleted = event.isCompleted
          )
        )
      }

      // Initial sample test results for analytics over time
      if (testResultDao.getTestResultCount() < 5) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 3600 * 1000
        val initialSampleResults = listOf(
          TestResultEntity(
            id = "tr_1",
            testTitle = "Biology Unit 1 Diagnostic Test",
            subjectName = MDCATSubject.BIOLOGY.name,
            score = 65,
            totalQuestions = 100,
            correctCount = 65,
            wrongCount = 35,
            timeTakenSeconds = 2800,
            dateMillis = now - 28L * dayMillis,
            weakTopicsString = "Cellular Respiration, Chemiosmosis"
          ),
          TestResultEntity(
            id = "tr_2",
            testTitle = "Chemistry Fund. Diagnostic Quiz",
            subjectName = MDCATSubject.CHEMISTRY.name,
            score = 68,
            totalQuestions = 100,
            correctCount = 68,
            wrongCount = 32,
            timeTakenSeconds = 3000,
            dateMillis = now - 25L * dayMillis,
            weakTopicsString = "Gas Laws, Stoichiometry Calculations"
          ),
          TestResultEntity(
            id = "tr_3",
            testTitle = "Physics Mechanics Practice Drill",
            subjectName = MDCATSubject.PHYSICS.name,
            score = 62,
            totalQuestions = 100,
            correctCount = 62,
            wrongCount = 38,
            timeTakenSeconds = 3200,
            dateMillis = now - 23L * dayMillis,
            weakTopicsString = "Vectors & Equilibrium, Centripetal Force"
          ),
          TestResultEntity(
            id = "tr_4",
            testTitle = "English Grammar Concord & Rules",
            subjectName = MDCATSubject.ENGLISH.name,
            score = 78,
            totalQuestions = 100,
            correctCount = 78,
            wrongCount = 22,
            timeTakenSeconds = 1800,
            dateMillis = now - 21L * dayMillis,
            weakTopicsString = "Subject-Verb Concord, Dangling Modifiers"
          ),
          TestResultEntity(
            id = "tr_5",
            testTitle = "Biology Bioenergetics & Enzymes",
            subjectName = MDCATSubject.BIOLOGY.name,
            score = 74,
            totalQuestions = 100,
            correctCount = 74,
            wrongCount = 26,
            timeTakenSeconds = 2700,
            dateMillis = now - 18L * dayMillis,
            weakTopicsString = "Enzyme Inhibitors, Light Reactions"
          ),
          TestResultEntity(
            id = "tr_6",
            testTitle = "Full MDCAT Mock Simulation 1",
            subjectName = null,
            score = 135,
            totalQuestions = 180,
            correctCount = 135,
            wrongCount = 45,
            timeTakenSeconds = 8900,
            dateMillis = now - 16L * dayMillis,
            weakTopicsString = "Electromagnetism, Organic Carbonyls"
          ),
          TestResultEntity(
            id = "tr_7",
            testTitle = "Chemistry Organic Functional Groups",
            subjectName = MDCATSubject.CHEMISTRY.name,
            score = 77,
            totalQuestions = 100,
            correctCount = 77,
            wrongCount = 23,
            timeTakenSeconds = 2500,
            dateMillis = now - 14L * dayMillis,
            weakTopicsString = "Aldehyde Tests, Equilibrium Shifts"
          ),
          TestResultEntity(
            id = "tr_8",
            testTitle = "Physics Electromagnetism & Waves",
            subjectName = MDCATSubject.PHYSICS.name,
            score = 73,
            totalQuestions = 100,
            correctCount = 73,
            wrongCount = 27,
            timeTakenSeconds = 2600,
            dateMillis = now - 11L * dayMillis,
            weakTopicsString = "Magnetic Flux, Doppler Shift"
          ),
          TestResultEntity(
            id = "tr_9",
            testTitle = "Logical Reasoning Deductions Drill",
            subjectName = MDCATSubject.LOGICAL_REASONING.name,
            score = 89,
            totalQuestions = 100,
            correctCount = 89,
            wrongCount = 11,
            timeTakenSeconds = 1400,
            dateMillis = now - 9L * dayMillis,
            weakTopicsString = "Syllogistic Fallacies"
          ),
          TestResultEntity(
            id = "tr_10",
            testTitle = "Biology Human Physiology & Genetics",
            subjectName = MDCATSubject.BIOLOGY.name,
            score = 84,
            totalQuestions = 100,
            correctCount = 84,
            wrongCount = 16,
            timeTakenSeconds = 2400,
            dateMillis = now - 7L * dayMillis,
            weakTopicsString = "Mendelian Genetics, Synaptic Transmission"
          ),
          TestResultEntity(
            id = "tr_11",
            testTitle = "Full MDCAT Mock Simulation 2",
            subjectName = null,
            score = 152,
            totalQuestions = 180,
            correctCount = 152,
            wrongCount = 28,
            timeTakenSeconds = 8700,
            dateMillis = now - 5L * dayMillis,
            weakTopicsString = "Projectile Motion, Buffer Solutions"
          ),
          TestResultEntity(
            id = "tr_12",
            testTitle = "Chemistry Reaction Kinetics Drill",
            subjectName = MDCATSubject.CHEMISTRY.name,
            score = 86,
            totalQuestions = 100,
            correctCount = 86,
            wrongCount = 14,
            timeTakenSeconds = 2300,
            dateMillis = now - 3L * dayMillis,
            weakTopicsString = "Rate Law Orders, Catalyst Mechanisms"
          ),
          TestResultEntity(
            id = "tr_13",
            testTitle = "Biology High-Yield Grand Drill",
            subjectName = MDCATSubject.BIOLOGY.name,
            score = 91,
            totalQuestions = 100,
            correctCount = 91,
            wrongCount = 9,
            timeTakenSeconds = 2100,
            dateMillis = now - 2L * dayMillis,
            weakTopicsString = "Proton Gradient Complex II"
          ),
          TestResultEntity(
            id = "tr_14",
            testTitle = "Full MDCAT Grand Simulator 3",
            subjectName = null,
            score = 164,
            totalQuestions = 180,
            correctCount = 164,
            wrongCount = 16,
            timeTakenSeconds = 8500,
            dateMillis = now - 1L * dayMillis,
            weakTopicsString = "AC Phase Angle"
          )
        )
        testResultDao.insertTestResults(initialSampleResults)
      }

      // Seed Room Database with comprehensive MDCAT quiz questions if table is empty
      if (quizQuestionDao.getQuestionCount() == 0) {
        val questionEntities = MDCATSyllabusData.quizQuestions.map { it.toEntity() }
        quizQuestionDao.insertQuestions(questionEntities)
      }
    }
  }

  // --- Encrypted Notes ---

  val allNotes: Flow<List<EncryptedNote>> = noteDao.getAllNotes().map { entities ->
    entities.map { entity ->
      EncryptedNote(
        id = entity.id,
        title = entity.title,
        subject = runCatching { MDCATSubject.valueOf(entity.subjectName) }.getOrDefault(MDCATSubject.BIOLOGY),
        rawContent = entity.rawContent,
        isEncrypted = entity.isEncrypted,
        encryptionAlgorithm = entity.encryptionAlgorithm,
        lastModifiedMillis = entity.lastModifiedMillis,
        isPinned = entity.isPinned
      )
    }
  }

  suspend fun saveNote(note: EncryptedNote) {
    noteDao.insertNote(
      EncryptedNoteEntity(
        id = note.id,
        title = note.title,
        subjectName = note.subject.name,
        rawContent = note.rawContent,
        isEncrypted = note.isEncrypted,
        encryptionAlgorithm = note.encryptionAlgorithm,
        lastModifiedMillis = System.currentTimeMillis(),
        isPinned = note.isPinned
      )
    )
  }

  suspend fun deleteNote(id: String) {
    noteDao.deleteNote(id)
  }

  // --- Test Results ---

  val allTestResults: Flow<List<TestResult>> = testResultDao.getAllTestResults().map { entities ->
    entities.map { entity ->
      TestResult(
        id = entity.id,
        testTitle = entity.testTitle,
        subject = entity.subjectName?.let { runCatching { MDCATSubject.valueOf(it) }.getOrNull() },
        score = entity.score,
        totalQuestions = entity.totalQuestions,
        correctCount = entity.correctCount,
        wrongCount = entity.wrongCount,
        timeTakenSeconds = entity.timeTakenSeconds,
        dateMillis = entity.dateMillis,
        weakTopics = if (entity.weakTopicsString.isBlank()) emptyList() else entity.weakTopicsString.split(", ")
      )
    }
  }

  suspend fun recordTestResult(result: TestResult) {
    testResultDao.insertTestResult(
      TestResultEntity(
        id = result.id,
        testTitle = result.testTitle,
        subjectName = result.subject?.name,
        score = result.score,
        totalQuestions = result.totalQuestions,
        correctCount = result.correctCount,
        wrongCount = result.wrongCount,
        timeTakenSeconds = result.timeTakenSeconds,
        dateMillis = result.dateMillis,
        weakTopicsString = result.weakTopics.joinToString(", ")
      )
    )
  }

  // --- Calendar Events ---

  val allCalendarEvents: Flow<List<CalendarEvent>> = calendarEventDao.getAllEvents().map { entities ->
    entities.map { entity ->
      CalendarEvent(
        id = entity.id,
        title = entity.title,
        dateMillis = entity.dateMillis,
        eventType = runCatching { CalendarEventType.valueOf(entity.eventTypeName) }.getOrDefault(CalendarEventType.MOCK_TEST),
        notes = entity.notes,
        isCompleted = entity.isCompleted
      )
    }
  }

  suspend fun addCalendarEvent(event: CalendarEvent) {
    calendarEventDao.insertEvent(
      CalendarEventEntity(
        id = event.id,
        title = event.title,
        dateMillis = event.dateMillis,
        eventTypeName = event.eventType.name,
        notes = event.notes,
        isCompleted = event.isCompleted
      )
    )
  }

  suspend fun toggleEventCompletion(id: String, completed: Boolean) {
    calendarEventDao.updateEventStatus(id, completed)
  }

  // --- Offline Access ---

  suspend fun toggleOfflineStudyMaterial(materialId: String) {
    val isDownloaded = offlineDao.isDownloaded(materialId)
    if (isDownloaded) {
      offlineDao.removeDownload(materialId)
    } else {
      offlineDao.insertDownload(
        OfflineDownloadEntity(
          itemId = materialId,
          itemType = "STUDY_MATERIAL",
          downloadedAtMillis = System.currentTimeMillis()
        )
      )
    }
    _studyMaterials.value = _studyMaterials.value.map {
      if (it.id == materialId) it.copy(isDownloadedOffline = !isDownloaded) else it
    }
  }

  suspend fun toggleOfflineVideoLecture(lectureId: String) {
    val isDownloaded = offlineDao.isDownloaded(lectureId)
    if (isDownloaded) {
      offlineDao.removeDownload(lectureId)
    } else {
      offlineDao.insertDownload(
        OfflineDownloadEntity(
          itemId = lectureId,
          itemType = "VIDEO_LECTURE",
          downloadedAtMillis = System.currentTimeMillis()
        )
      )
    }
    _videoLectures.value = _videoLectures.value.map {
      if (it.id == lectureId) it.copy(isDownloadedOffline = !isDownloaded) else it
    }
  }

  // --- Forum Actions ---

  fun addForumPost(title: String, content: String, subject: MDCATSubject, authorName: String) {
    val newPost = ForumPost(
      id = "post_${System.currentTimeMillis()}",
      authorName = authorName,
      authorTag = "MDCAT Aspirant",
      title = title,
      content = content,
      subject = subject,
      upvotes = 1,
      isUpvoted = true,
      isResolved = false,
      timestampMillis = System.currentTimeMillis(),
      answers = emptyList()
    )
    _forumPosts.value = listOf(newPost) + _forumPosts.value
  }

  fun togglePostUpvote(postId: String) {
    _forumPosts.value = _forumPosts.value.map { post ->
      if (post.id == postId) {
        val newUpvoted = !post.isUpvoted
        post.copy(
          isUpvoted = newUpvoted,
          upvotes = if (newUpvoted) post.upvotes + 1 else post.upvotes - 1
        )
      } else post
    }
  }

  fun addForumAnswer(postId: String, answerContent: String, authorName: String) {
    val newAnswer = com.example.data.model.ForumAnswer(
      id = "ans_${System.currentTimeMillis()}",
      authorName = authorName,
      authorTag = "MDCAT Peer",
      content = answerContent,
      timestampMillis = System.currentTimeMillis(),
      isVerifiedEducator = false,
      upvotes = 0
    )
    _forumPosts.value = _forumPosts.value.map { post ->
      if (post.id == postId) {
        post.copy(answers = post.answers + newAnswer)
      } else post
    }
  }

  fun createStudyGroup(name: String, subject: MDCATSubject, description: String) {
    val newGroup = StudyGroup(
      id = "grp_${System.currentTimeMillis()}",
      name = name,
      targetBatch = "MDCAT Aspirants Batch",
      memberCount = 1,
      subjectFocus = subject,
      description = description,
      recentDiscussion = "Study group founded by you. Welcome new members!",
      sharedResourcesCount = 1
    )
    _studyGroups.value = listOf(newGroup) + _studyGroups.value
  }

  // --- Admin / Instructor Management (Dr. Salar) ---

  fun addQuizQuestion(question: com.example.data.model.QuizQuestion) {
    CoroutineScope(Dispatchers.IO).launch {
      quizQuestionDao.insertQuestion(question.toEntity())
    }
  }

  fun deleteQuizQuestion(id: String) {
    CoroutineScope(Dispatchers.IO).launch {
      quizQuestionDao.deleteQuestion(id)
    }
  }

  fun getQuestionsBySubjectFromRoom(subject: MDCATSubject): Flow<List<com.example.data.model.QuizQuestion>> {
    return quizQuestionDao.getQuestionsBySubject(subject.name).map { entities ->
      entities.map { it.toQuizQuestion() }
    }
  }

  fun getQuestionsByClassFromRoom(academicClass: com.example.data.model.AcademicClass): Flow<List<com.example.data.model.QuizQuestion>> {
    return quizQuestionDao.getQuestionsByClass(academicClass.name).map { entities ->
      entities.map { it.toQuizQuestion() }
    }
  }

  fun addStudyMaterial(material: StudyMaterial) {
    _studyMaterials.value = listOf(material) + _studyMaterials.value
  }

  fun deleteStudyMaterial(id: String) {
    _studyMaterials.value = _studyMaterials.value.filterNot { it.id == id }
  }

  fun addPdfResource(pdf: com.example.data.model.PdfResource) {
    _pdfResources.value = listOf(pdf) + _pdfResources.value
  }

  fun deletePdfResource(id: String) {
    _pdfResources.value = _pdfResources.value.filterNot { it.id == id }
  }

  fun addVideoLecture(video: VideoLecture) {
    _videoLectures.value = listOf(video) + _videoLectures.value
  }

  fun deleteVideoLecture(id: String) {
    _videoLectures.value = _videoLectures.value.filterNot { it.id == id }
  }

  fun saveDraftNote(draft: com.example.data.model.StudyNoteDraft) {
    val existingIndex = _draftNotes.value.indexOfFirst { it.id == draft.id }
    if (existingIndex >= 0) {
      _draftNotes.value = _draftNotes.value.toMutableList().also {
        it[existingIndex] = draft
      }
    } else {
      _draftNotes.value = listOf(draft) + _draftNotes.value
    }
  }

  fun deleteDraftNote(id: String) {
    _draftNotes.value = _draftNotes.value.filterNot { it.id == id }
  }

  // --- User Authentication & Account Management with Firebase & RBAC ---

  val firebaseAuthService = com.example.data.auth.FirebaseAuthService(context)

  fun loginWithGmail(
    email: String,
    displayName: String? = null,
    uid: String? = null,
    roleOverride: com.example.data.model.UserRole? = null
  ): com.example.data.model.UserAccount {
    val cleanEmail = email.trim().lowercase()
    val assignedRole = roleOverride ?: firebaseAuthService.resolveRole(cleanEmail)
    val isOwner = assignedRole == com.example.data.model.UserRole.OWNER
    val isAdmin = assignedRole == com.example.data.model.UserRole.ADMIN
    val derivedName = displayName ?: when (assignedRole) {
      com.example.data.model.UserRole.OWNER -> "Dr. Salar (Owner)"
      com.example.data.model.UserRole.ADMIN -> "Academic Admin"
      else -> cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
    }

    val account = com.example.data.model.UserAccount(
      uid = uid ?: "user_${cleanEmail.hashCode()}",
      email = cleanEmail,
      displayName = derivedName,
      photoUrl = null,
      role = assignedRole,
      isLoggedIn = true,
      isGmailUser = cleanEmail.endsWith("@gmail.com"),
      isInstructor = assignedRole.canManageContent,
      enrolledCourseIds = if (assignedRole.canManageContent) {
        setOf("course_mdcat_full", "course_bio_11", "course_physics_12", "course_crash_test")
      } else {
        setOf("course_mdcat_full", "course_bio_11")
      }
    )
    _userAccount.value = account
    CoroutineScope(Dispatchers.IO).launch {
      userProfileRepository.syncWithAccount(account)
    }
    return account
  }

  fun logoutUser() {
    firebaseAuthService.signOut()
    _userAccount.value = com.example.data.model.UserAccount(
      uid = "guest",
      email = "",
      displayName = "Guest Student",
      role = com.example.data.model.UserRole.GUEST,
      isLoggedIn = false,
      isGmailUser = false,
      isInstructor = false,
      enrolledCourseIds = emptySet()
    )
  }

  // --- Course Subscriptions Management ---

  fun toggleCourseSubscription(courseId: String, isSubscribed: Boolean) {
    _courseSubscriptions.value = _courseSubscriptions.value.map { course ->
      if (course.courseId == courseId) {
        course.copy(
          isSubscribed = isSubscribed,
          totalSubscribers = if (isSubscribed) course.totalSubscribers + 1 else (course.totalSubscribers - 1).coerceAtLeast(0)
        )
      } else {
        course
      }
    }

    // Also reflect in UserAccount enrolledCourseIds
    val currentAccount = _userAccount.value
    val updatedIds = if (isSubscribed) {
      currentAccount.enrolledCourseIds + courseId
    } else {
      currentAccount.enrolledCourseIds - courseId
    }
    _userAccount.value = currentAccount.copy(enrolledCourseIds = updatedIds)
  }

  fun addCourseSubscription(course: com.example.data.model.CourseSubscription) {
    _courseSubscriptions.value = listOf(course) + _courseSubscriptions.value
  }

  // --- Live Interactive Sessions Management ---

  fun addLiveSession(session: com.example.data.model.LiveSession) {
    _liveSessions.value = listOf(session) + _liveSessions.value
  }

  fun updateLiveSessionStatus(sessionId: String, status: com.example.data.model.LiveSessionStatus) {
    _liveSessions.value = _liveSessions.value.map {
      if (it.id == sessionId) it.copy(status = status) else it
    }
  }

  fun deleteLiveSession(id: String) {
    _liveSessions.value = _liveSessions.value.filterNot { it.id == id }
  }

  suspend fun triggerCloudSync() {
    _isSyncing.value = true
    kotlinx.coroutines.delay(1200) // Realistic cloud synchronization simulation
    _lastSyncTimestamp.value = System.currentTimeMillis()
    _isSyncing.value = false
  }

  companion object {
    @Volatile
    private var INSTANCE: DataRepository? = null

    fun getInstance(context: Context): DataRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: DataRepository(context).also { INSTANCE = it }
      }
    }
  }
}
