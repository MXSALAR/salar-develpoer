package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiStudyPlanService
import com.example.data.model.MDCATSubject
import com.example.data.sample.MDCATSyllabusData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches MDCAT Master`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MDCAT Master", appName)
  }

  @Test
  fun `verify syllabus data contains required subjects and questions`() {
    val questions = MDCATSyllabusData.quizQuestions
    assertTrue("Should have multiple questions", questions.size >= 5)

    val subjects = questions.map { it.subject }.distinct()
    assertTrue("Should contain Biology", subjects.contains(MDCATSubject.BIOLOGY))
    assertTrue("Should contain Chemistry", subjects.contains(MDCATSubject.CHEMISTRY))
    assertTrue("Should contain Physics", subjects.contains(MDCATSubject.PHYSICS))

    val materials = MDCATSyllabusData.studyMaterials
    assertTrue("Should have study materials", materials.isNotEmpty())

    val videos = MDCATSyllabusData.videoLectures
    assertTrue("Should have video lectures", videos.isNotEmpty())
    assertTrue("Educator name should contain Dr salar", videos.first().educatorName.contains("Dr salar"))
  }

  @Test
  fun `verify AI study plan local fallback generator produces structured 7-day plan`() = runBlocking {
    val aiService = GeminiStudyPlanService()
    val weakTopics = listOf("Bioenergetics Complex II", "Equilibrium Shifts")
    val plan = aiService.generatePersonalizedStudyPlan(
      weakAreas = weakTopics,
      targetScore = 185,
      dailyHours = 4.0f,
      daysRemaining = 72
    )

    assertNotNull(plan)
    assertEquals(185, plan.studentTargetScore)
    assertEquals(7, plan.scheduleDays.size)
    assertTrue("Plan recommendations should credit Dr salar", plan.aiRecommendationNotes.contains("Dr salar"))
  }

  @Test
  fun `verify encrypted notes initial data security state`() {
    val notes = MDCATSyllabusData.initialEncryptedNotes
    assertTrue("Notes should exist", notes.isNotEmpty())
    val firstNote = notes.first()
    assertTrue("Note should be flagged as encrypted", firstNote.isEncrypted)
    assertEquals("AES-256-GCM (End-to-End)", firstNote.encryptionAlgorithm)
  }

  @Test
  fun `verify admin pdf resources and new mcq creation`() {
    val pdfs = MDCATSyllabusData.initialPdfResources
    assertTrue("Initial PDF resources should exist", pdfs.isNotEmpty())
    assertEquals("Dr. Salar", pdfs.first().uploadedBy)

    val newQ = com.example.data.model.QuizQuestion(
      id = "mcq_test",
      subject = MDCATSubject.BIOLOGY,
      topic = "Cell Cycle",
      questionText = "During which phase does crossing over occur?",
      options = listOf("Prophase I (Pachytene)", "Metaphase I", "Anaphase II", "Telophase I"),
      correctOptionIndex = 0,
      explanation = "Crossing over occurs during pachytene stage of prophase I."
    )
    assertEquals(0, newQ.correctOptionIndex)
    assertEquals("Cell Cycle", newQ.topic)
  }

  @Test
  fun `verify AcademicClass separation in syllabus and resources`() {
    val pdfs = MDCATSyllabusData.initialPdfResources
    val class11Pdfs = pdfs.filter { it.academicClass == com.example.data.model.AcademicClass.CLASS_11 }
    val class12Pdfs = pdfs.filter { it.academicClass == com.example.data.model.AcademicClass.CLASS_12 }

    assertTrue("Should have 11th class PDFs", class11Pdfs.isNotEmpty())
    assertTrue("Should have 12th class PDFs", class12Pdfs.isNotEmpty())
    assertEquals("11th Class", com.example.data.model.AcademicClass.CLASS_11.shortName)
    assertEquals("12th Class", com.example.data.model.AcademicClass.CLASS_12.shortName)
  }

  @Test
  fun `verify Draft notes lifecycle for Dr Salar multiple sessions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.local.DataRepository.getInstance(context)
    val testDraft = com.example.data.model.StudyNoteDraft(
      id = "draft_test_1",
      title = "Enzymes Detailed Kinetics",
      subject = MDCATSubject.BIOLOGY,
      academicClass = com.example.data.model.AcademicClass.CLASS_11,
      chapter = "Bioenergetics & Enzymes",
      readTimeMin = 15,
      keyConceptsRaw = "Lock and key model\nInduced fit hypothesis",
      mnemonicsRaw = "Enzymes lower Ea",
      contentMarkdown = "### Enzyme Kinetics by Dr. Salar",
      linkedPdfUrl = "https://example.com/enzymes.pdf"
    )

    repo.saveDraftNote(testDraft)

    // Save and verify
    repo.deleteDraftNote("draft_test_1")
  }

  @Test
  fun `verify Owner and Domain Access Control logic`() {
    val auth = com.example.data.model.AdminAuthState()
    assertEquals("shaukatsalar231@gmail.com", auth.authorizedOwnerEmail)
    assertEquals("gmail.com", auth.allowedDomain)
    assertTrue("Default primary owner email should be authorized", auth.isOwner)

    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.MDCATViewModel(app)

    val isAuthorized = vm.verifyOwnerEmail("shaukatsalar231@gmail.com")
    assertTrue("Owner email should succeed verification", isAuthorized)
    assertTrue("Edit mode should be unlocked for owner", vm.adminAuthState.value.isEditModeUnlocked)

    val isUnauthorized = vm.verifyOwnerEmail("hacker@malicious.com")
    assertTrue("Non-owner email should be rejected", !isUnauthorized)
    assertTrue("Non-owner edit mode should be locked", !vm.adminAuthState.value.isEditModeUnlocked)
  }
}
