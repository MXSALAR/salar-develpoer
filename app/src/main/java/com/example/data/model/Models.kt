package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.theme.LogicPink
import com.example.ui.theme.PhysicsPurple

enum class MDCATSubject(
  val displayName: String,
  val weightagePercent: Int,
  val color: Color,
  val totalMDCATMcqs: Int
) {
  BIOLOGY("Biology", 34, BioGreen, 68),
  CHEMISTRY("Chemistry", 27, ChemOrange, 54),
  PHYSICS("Physics", 27, PhysicsPurple, 54),
  ENGLISH("English", 9, EnglishBlue, 18),
  LOGICAL_REASONING("Logical Reasoning", 3, LogicPink, 6)
}

enum class AcademicClass(val displayName: String, val shortName: String) {
  CLASS_11("11th Class (1st Year / FSc-I)", "11th Class"),
  CLASS_12("12th Class (2nd Year / FSc-II)", "12th Class")
}

data class StudyNoteDraft(
  val id: String,
  val title: String,
  val subject: MDCATSubject,
  val academicClass: AcademicClass = AcademicClass.CLASS_11,
  val chapter: String = "",
  val readTimeMin: Int = 10,
  val keyConceptsRaw: String = "",
  val mnemonicsRaw: String = "",
  val contentMarkdown: String = "",
  val linkedPdfUrl: String = "",
  val lastSavedMillis: Long = System.currentTimeMillis()
)

enum class UserRole(val label: String, val level: Int) {
  OWNER("Owner", 3),
  ADMIN("Admin", 2),
  STUDENT("Student", 1),
  GUEST("Guest", 0);

  val canManageContent: Boolean get() = this == OWNER || this == ADMIN
  val canModifySystemSettings: Boolean get() = this == OWNER
}

data class AdminAuthState(
  val currentUserEmail: String = "shaukatsalar231@gmail.com",
  val role: UserRole = UserRole.OWNER,
  val isOwner: Boolean = true,
  val isAdmin: Boolean = false,
  val authorizedOwnerEmail: String = "shaukatsalar231@gmail.com",
  val authorizedAdminEmails: Set<String> = setOf("shaukatsalar231@gmail.com", "admin.mdcat@gmail.com", "drsalar.assistant@gmail.com"),
  val allowedDomain: String = "gmail.com",
  val isEditModeUnlocked: Boolean = true,
  val roleTitle: String = "Verified Syllabus Owner & Lead Educator"
) {
  val canViewOrModifyStudyContent: Boolean
    get() = (role == UserRole.OWNER || role == UserRole.ADMIN || isOwner || isAdmin) && isEditModeUnlocked
}

data class StudyMaterial(
  val id: String,
  val subject: MDCATSubject,
  val title: String,
  val chapter: String,
  val readTimeMin: Int,
  val keyConcepts: List<String>,
  val mnemonics: List<String>,
  val formulas: List<String> = emptyList(),
  val contentMarkdown: String,
  val isDownloadedOffline: Boolean = false,
  val isBookmarked: Boolean = false,
  val highYieldPriority: String = "HIGH",
  val linkedPdfUrl: String? = null,
  val academicClass: AcademicClass = AcademicClass.CLASS_11
)

data class PdfResource(
  val id: String,
  val title: String,
  val subject: MDCATSubject,
  val chapter: String,
  val fileSizeMb: Float,
  val pdfUrlOrPath: String,
  val uploadedBy: String = "Dr. Salar",
  val uploadedAtMillis: Long = System.currentTimeMillis(),
  val description: String = "",
  val academicClass: AcademicClass = AcademicClass.CLASS_11
)

data class QuizQuestion(
  val id: String,
  val subject: MDCATSubject,
  val topic: String,
  val questionText: String,
  val options: List<String>,
  val correctOptionIndex: Int,
  val explanation: String,
  val highYieldTip: String = "",
  val academicClass: AcademicClass = AcademicClass.CLASS_11
)

data class TestResult(
  val id: String,
  val testTitle: String,
  val subject: MDCATSubject?, // null if full mock
  val score: Int,
  val totalQuestions: Int,
  val correctCount: Int,
  val wrongCount: Int,
  val timeTakenSeconds: Int,
  val dateMillis: Long,
  val weakTopics: List<String>
)

data class VideoLecture(
  val id: String,
  val title: String,
  val subject: MDCATSubject,
  val educatorName: String,
  val durationMinutes: Int,
  val topic: String,
  val summaryNotes: String,
  val keyTimestamps: List<Pair<String, String>>,
  val isDownloadedOffline: Boolean = false,
  val academicClass: AcademicClass = AcademicClass.CLASS_11
)

data class ForumAnswer(
  val id: String,
  val authorName: String,
  val authorTag: String,
  val content: String,
  val timestampMillis: Long,
  val isVerifiedEducator: Boolean = false,
  val upvotes: Int = 0
)

data class ForumPost(
  val id: String,
  val authorName: String,
  val authorTag: String,
  val title: String,
  val content: String,
  val subject: MDCATSubject,
  val upvotes: Int = 0,
  val isUpvoted: Boolean = false,
  val isResolved: Boolean = false,
  val timestampMillis: Long,
  val answers: List<ForumAnswer> = emptyList()
)

data class StudyGroup(
  val id: String,
  val name: String,
  val targetBatch: String,
  val memberCount: Int,
  val subjectFocus: MDCATSubject,
  val description: String,
  val recentDiscussion: String,
  val sharedResourcesCount: Int = 12
)

data class EncryptedNote(
  val id: String,
  val title: String,
  val subject: MDCATSubject,
  val rawContent: String,
  val isEncrypted: Boolean = true,
  val encryptionAlgorithm: String = "AES-256-GCM (End-to-End)",
  val lastModifiedMillis: Long = System.currentTimeMillis(),
  val isPinned: Boolean = false
)

enum class CalendarEventType {
  EXAM,
  MOCK_TEST,
  SYLLABUS_DEADLINE,
  REVISION
}

data class CalendarEvent(
  val id: String,
  val title: String,
  val dateMillis: Long,
  val eventType: CalendarEventType,
  val notes: String = "",
  val isCompleted: Boolean = false
)

data class StudyDaySchedule(
  val dayNumber: Int,
  val dayLabel: String,
  val focusSubject: MDCATSubject,
  val topicTitle: String,
  val targetMcqsCount: Int,
  val plannedHours: Float,
  var isDone: Boolean = false
)

data class AIStudyPlan(
  val id: String,
  val studentTargetScore: Int,
  val weakAreas: List<String>,
  val dailyTargetHours: Float,
  val generatedTimestamp: Long,
  val scheduleDays: List<StudyDaySchedule>,
  val aiRecommendationNotes: String
)

data class BadgeItem(
  val id: String,
  val title: String,
  val description: String,
  val iconName: String,
  val isUnlocked: Boolean,
  val progress: Float = 1f
)

data class GamificationProfile(
  val streakDays: Int = 7,
  val xpPoints: Int = 1450,
  val level: Int = 4,
  val completedTestsCount: Int = 12,
  val solvedMcqsCount: Int = 480,
  val targetMDCATScore: Int = 165,
  val targetMDCATDateMillis: Long = System.currentTimeMillis() + (72L * 24 * 3600 * 1000) // ~72 days away
)

data class UserAccount(
  val uid: String = "user_default_salar",
  val email: String = "shaukatsalar231@gmail.com",
  val displayName: String = "Dr. Salar",
  val photoUrl: String? = null,
  val role: UserRole = UserRole.OWNER,
  val isLoggedIn: Boolean = true,
  val isGmailUser: Boolean = true,
  val isInstructor: Boolean = true,
  val enrolledCourseIds: Set<String> = setOf("course_mdcat_full", "course_bio_11"),
  val memberSinceMillis: Long = System.currentTimeMillis()
) {
  val isOwner: Boolean get() = role == UserRole.OWNER || email.equals("shaukatsalar231@gmail.com", ignoreCase = true)
  val isAdmin: Boolean get() = role == UserRole.ADMIN
  val hasAdminAccess: Boolean get() = isOwner || isAdmin
}

data class UserProfile(
  val uid: String = "user_default_salar",
  val email: String = "shaukatsalar231@gmail.com",
  val displayName: String = "Dr. Salar",
  val photoUrl: String? = null,
  val role: UserRole = UserRole.OWNER,
  val assignedRoles: List<String> = listOf("OWNER", "LEAD_INSTRUCTOR"),
  val permissions: List<String> = listOf(
    "MANAGE_SYLLABUS",
    "CREATE_MCQS",
    "EDIT_EXAMS",
    "MANAGE_USERS",
    "VIEW_ANALYTICS",
    "BROADCAST_LIVE"
  ),
  val isAdminDashboardAuthorized: Boolean = true,
  val department: String = "MDCAT Academic Directorate",
  val enrolledCourses: List<String> = listOf("course_mdcat_full", "course_bio_11"),
  val createdAtMillis: Long = System.currentTimeMillis(),
  val lastLoginMillis: Long = System.currentTimeMillis()
) {
  val isOwner: Boolean get() = role == UserRole.OWNER || email.equals("shaukatsalar231@gmail.com", ignoreCase = true)
  val isAdmin: Boolean get() = role == UserRole.ADMIN || assignedRoles.contains("ADMIN")
  val canAccessAdminDashboard: Boolean get() = isOwner || isAdmin || isAdminDashboardAuthorized
}

data class CourseSubscription(
  val courseId: String,
  val title: String,
  val subjectFocus: String,
  val targetClass: AcademicClass?,
  val instructor: String = "Dr. Salar",
  val pricePkr: Int,
  val originalPricePkr: Int,
  val durationWeeks: Int,
  val description: String,
  val features: List<String>,
  val isSubscribed: Boolean = false,
  val badge: String = "POPULAR",
  val totalSubscribers: Int = 1240,
  val validUntilMillis: Long = System.currentTimeMillis() + (90L * 24 * 3600 * 1000)
)

enum class LiveSessionStatus(val label: String) {
  LIVE_NOW("LIVE NOW"),
  UPCOMING("UPCOMING"),
  RECORDED("RECORDED")
}

data class LiveSession(
  val id: String,
  val title: String,
  val instructorName: String = "Dr. Salar",
  val subject: MDCATSubject,
  val academicClass: AcademicClass,
  val scheduledTimeMillis: Long,
  val durationMinutes: Int = 60,
  val meetingUrl: String,
  val status: LiveSessionStatus = LiveSessionStatus.UPCOMING,
  val agenda: String,
  val attendeesCount: Int = 184,
  val recordingUrl: String? = null
)
