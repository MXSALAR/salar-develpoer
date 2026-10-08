package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicClass
import com.example.data.model.MDCATSubject
import com.example.data.model.PdfResource
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyMaterial
import com.example.data.model.StudyNoteDraft
import com.example.data.model.VideoLecture
import com.example.ui.viewmodel.MDCATViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  viewModel: MDCATViewModel,
  onClose: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var showAuthDialog by remember { mutableStateOf(false) }

  // Separate Blocks for Academic Classes (null = All Syllabus)
  var selectedClassBlock by remember { mutableStateOf<AcademicClass?>(AcademicClass.CLASS_11) }

  // Active Draft being edited in Notes Form
  var draftToEdit by remember { mutableStateOf<StudyNoteDraft?>(null) }

  val adminAuthState by viewModel.adminAuthState.collectAsState()
  val userAccount by viewModel.userAccount.collectAsState()
  val questions by viewModel.allQuizQuestions.collectAsState()
  val studyMaterials by viewModel.studyMaterials.collectAsState()
  val pdfResources by viewModel.pdfResources.collectAsState()
  val videoLectures by viewModel.videoLectures.collectAsState()
  val drafts by viewModel.draftNotes.collectAsState()
  val liveSessions by viewModel.liveSessions.collectAsState()
  val courseSubscriptions by viewModel.courseSubscriptions.collectAsState()
  val adminSearchQuery by viewModel.adminSearchQuery.collectAsState()
  val remainingDays by viewModel.remainingDays.collectAsState()

  // Role-Based Access Control (RBAC): Only 'Owner' or 'Admin' can view or modify study content
  val isOwner = adminAuthState.isOwner || userAccount.role == com.example.data.model.UserRole.OWNER || userAccount.isOwner ||
      userAccount.email.equals("shaukatsalar231@gmail.com", ignoreCase = true)
  val isAdmin = adminAuthState.isAdmin || userAccount.role == com.example.data.model.UserRole.ADMIN || userAccount.isAdmin
  val canViewOrModifyStudyContent = isOwner || isAdmin
  val isAuthorizedToEdit = canViewOrModifyStudyContent && adminAuthState.isEditModeUnlocked

  // Filtered lists based on the selected Class Block
  val filteredQuestions = remember(questions, selectedClassBlock) {
    if (selectedClassBlock == null) questions else questions.filter { it.academicClass == selectedClassBlock }
  }
  val filteredMaterials = remember(studyMaterials, selectedClassBlock) {
    if (selectedClassBlock == null) studyMaterials else studyMaterials.filter { it.academicClass == selectedClassBlock }
  }
  val filteredPdfs = remember(pdfResources, selectedClassBlock) {
    if (selectedClassBlock == null) pdfResources else pdfResources.filter { it.academicClass == selectedClassBlock }
  }
  val filteredDrafts = remember(drafts, selectedClassBlock) {
    if (selectedClassBlock == null) drafts else drafts.filter { it.academicClass == selectedClassBlock }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Instructor Hub",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Dr. Salar",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                  Icons.Default.Verified,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("btn_admin_close")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Study Portal"
            )
          }
        },
        actions = {
          // Quick Security/Auth Toggle Button
          IconButton(
            onClick = {
              if (adminAuthState.isOwner) {
                viewModel.toggleOwnerEditMode(!adminAuthState.isEditModeUnlocked)
                feedbackMessage = if (!adminAuthState.isEditModeUnlocked) {
                  "Owner Edit Mode enabled: Full administrative rights granted."
                } else {
                  "Dashboard locked in Read-Only Protection Mode."
                }
              } else {
                showAuthDialog = true
              }
            },
            modifier = Modifier.testTag("btn_admin_auth_status")
          ) {
            Icon(
              imageVector = if (isAuthorizedToEdit) Icons.Default.LockOpen else Icons.Default.Lock,
              contentDescription = "Owner Authorization",
              tint = if (isAuthorizedToEdit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { padding ->
    if (!canViewOrModifyStudyContent) {
      AdminAccessDeniedView(
        currentUserEmail = userAccount.email.ifBlank { adminAuthState.currentUserEmail },
        userRole = userAccount.role,
        onLoginAsAdminClick = {
          viewModel.toggleLoginScreen(true)
        },
        onClose = onClose,
        modifier = Modifier.padding(padding)
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp)
      ) {
        // 1. Owner & Domain Security Status Card
        OwnerDomainStatusBar(
          authState = adminAuthState,
          userRole = userAccount.role,
          onVerifyClick = { showAuthDialog = true },
          onToggleLock = {
            viewModel.toggleOwnerEditMode(!adminAuthState.isEditModeUnlocked)
            feedbackMessage = if (!adminAuthState.isEditModeUnlocked) {
              "Owner Edit Mode unlocked for ${adminAuthState.currentUserEmail}."
            } else {
              "Dashboard UI locked in Read-Only protection mode."
            }
          }
        )

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Separate Blocks for 11th Class & 12th Class
      AcademicClassBlockSelector(
        selectedBlock = selectedClassBlock,
        onSelectBlock = { selectedClassBlock = it },
        class11Count = questions.count { it.academicClass == AcademicClass.CLASS_11 } + studyMaterials.count { it.academicClass == AcademicClass.CLASS_11 },
        class12Count = questions.count { it.academicClass == AcademicClass.CLASS_12 } + studyMaterials.count { it.academicClass == AcademicClass.CLASS_12 },
        draftsCount = drafts.size
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Class-specific Context Card
      ClassBlockHeaderCard(selectedClassBlock = selectedClassBlock)

      Spacer(modifier = Modifier.height(10.dp))

      // Global Search Bar for Dr. Salar Admin Hub
      AdminGlobalSearchBar(
        query = adminSearchQuery,
        onQueryChange = { viewModel.setAdminSearchQuery(it) },
        onClear = { viewModel.setAdminSearchQuery("") }
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (adminSearchQuery.isNotBlank()) {
        // Render Global Search Results View across all categories and classes
        AdminGlobalSearchResultsView(
          query = adminSearchQuery,
          materials = studyMaterials,
          questions = questions,
          pdfs = pdfResources,
          drafts = drafts,
          liveSessions = liveSessions,
          courses = courseSubscriptions,
          isEditable = isAuthorizedToEdit,
          onDeleteMaterial = {
            if (!isAuthorizedToEdit) {
              feedbackMessage = "Access Denied: Only verified owner can delete materials."
            } else {
              viewModel.deleteStudyMaterial(it)
              feedbackMessage = "Study material deleted."
            }
          },
          onDeleteQuestion = {
            if (!isAuthorizedToEdit) {
              feedbackMessage = "Access Denied: Only owner can delete MCQs."
            } else {
              viewModel.deleteQuizQuestion(it)
              feedbackMessage = "MCQ deleted."
            }
          },
          onDeletePdf = {
            if (!isAuthorizedToEdit) {
              feedbackMessage = "Access Denied: Only owner can delete PDFs."
            } else {
              viewModel.deletePdfResource(it)
              feedbackMessage = "PDF deleted."
            }
          },
          onDeleteDraft = {
            if (!isAuthorizedToEdit) {
              feedbackMessage = "Access Denied: Only owner can delete drafts."
            } else {
              viewModel.deleteDraftNote(it)
              feedbackMessage = "Draft deleted."
            }
          },
          onResumeDraft = { draft ->
            draftToEdit = draft
            selectedTab = 2 // Switch to study notes tab to edit draft
            viewModel.setAdminSearchQuery("")
            feedbackMessage = "Resumed draft: '${draft.title}'"
          },
          onClearSearch = { viewModel.setAdminSearchQuery("") }
        )
      } else {
        // 3. Navigation Tabs (Upload MCQs, Link PDFs, Notes, Drafts, Live Sessions, Courses, Videos)
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.primary,
          edgePadding = 0.dp
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Upload MCQs", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Link PDFs", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("Study Notes", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Drafts", fontWeight = FontWeight.Bold)
                if (drafts.isNotEmpty()) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                  ) {
                    Text(
                      text = "${drafts.size}",
                      color = MaterialTheme.colorScheme.onPrimary,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                }
              }
            }
          )
          Tab(
            selected = selectedTab == 4,
            onClick = { selectedTab = 4 },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live Sessions", fontWeight = FontWeight.Bold)
                val hasLive = liveSessions.any { it.status == com.example.data.model.LiveSessionStatus.LIVE_NOW }
                if (hasLive) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Surface(
                    color = Color(0xFFD93025),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = "LIVE",
                      color = Color.White,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.ExtraBold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
              }
            }
          )
          Tab(
            selected = selectedTab == 5,
            onClick = { selectedTab = 5 },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Courses", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  color = Color(0xFFF9AB00).copy(alpha = 0.2f),
                  shape = CircleShape
                ) {
                  Text(
                    text = "${courseSubscriptions.size}",
                    color = Color(0xFFE37400),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                  )
                }
              }
            }
          )
          Tab(
            selected = selectedTab == 6,
            onClick = { selectedTab = 6 },
            text = { Text("Videos", fontWeight = FontWeight.Bold) }
          )
        }

        // Success / Notice Feedback Banner
        if (feedbackMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = feedbackMessage!!,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { feedbackMessage = null }, modifier = Modifier.size(24.dp)) {
                Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
          when (selectedTab) {
            0 -> ManageMcqsTab(
              questions = filteredQuestions,
              defaultClass = selectedClassBlock ?: AcademicClass.CLASS_11,
              isEditable = isAuthorizedToEdit,
              remainingDays = remainingDays,
              onUpdateRemainingDays = { newDays ->
                viewModel.updateRemainingDays(newDays)
                feedbackMessage = "MDCAT countdown updated to $newDays days remaining across student app!"
              },
              onAddMcq = { subject, topic, qText, opts, correctIdx, exp, tip, academicClass ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner (shaukatsalar231@gmail.com) can publish MCQs."
                } else {
                  viewModel.addQuizQuestion(subject, topic, qText, opts, correctIdx, exp, tip, academicClass)
                  feedbackMessage = "MCQ published successfully to ${academicClass.shortName} syllabus!"
                }
              },
              onDeleteMcq = {
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only owner can delete questions."
                } else {
                  viewModel.deleteQuizQuestion(it)
                }
              }
            )
            1 -> ManagePdfsTab(
              pdfs = filteredPdfs,
              defaultClass = selectedClassBlock ?: AcademicClass.CLASS_11,
              isEditable = isAuthorizedToEdit,
              onAddPdf = { title, subject, chapter, url, desc, sizeMb, academicClass ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner can link PDF resources."
                } else {
                  viewModel.addPdfResource(title, subject, chapter, url, desc, sizeMb, academicClass)
                  feedbackMessage = "PDF linked to ${academicClass.shortName} handbook collection!"
                }
              },
              onDeletePdf = {
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only owner can delete PDF resources."
                } else {
                  viewModel.deletePdfResource(it)
                }
              }
            )
            2 -> ManageStudyMaterialsTab(
              materials = filteredMaterials,
              drafts = filteredDrafts,
              defaultClass = selectedClassBlock ?: AcademicClass.CLASS_11,
              activeDraft = draftToEdit,
              isEditable = isAuthorizedToEdit,
              onAddMaterial = { title, subject, chapter, readTime, concepts, mnemonics, content, pdfUrl, academicClass, activeDraftId ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner can publish notes."
                } else {
                  viewModel.addStudyMaterial(title, subject, chapter, readTime, concepts, mnemonics, content, pdfUrl, academicClass)
                  if (activeDraftId != null) {
                    viewModel.deleteDraftNote(activeDraftId)
                  }
                  draftToEdit = null
                  feedbackMessage = "Notes published to live ${academicClass.shortName} syllabus!"
                }
              },
              onSaveDraft = { draftId, title, subject, academicClass, chapter, readTime, conceptsRaw, mnemonicsRaw, content, pdfUrl ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner can save drafts."
                } else {
                  val savedId = viewModel.saveDraftNote(
                    id = draftId,
                    title = title,
                    subject = subject,
                    academicClass = academicClass,
                    chapter = chapter,
                    readTimeMin = readTime,
                    keyConceptsRaw = conceptsRaw,
                    mnemonicsRaw = mnemonicsRaw,
                    contentMarkdown = content,
                    linkedPdfUrl = pdfUrl
                  )
                  draftToEdit = drafts.find { it.id == savedId }
                  feedbackMessage = "Draft saved successfully! You can resume editing over multiple sessions."
                }
              },
              onDeleteMaterial = {
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only owner can delete materials."
                } else {
                  viewModel.deleteStudyMaterial(it)
                }
              },
              onResumeDraft = { draft ->
                draftToEdit = draft
                feedbackMessage = "Resumed draft: '${draft.title}' (${draft.academicClass.shortName})"
              },
              onDiscardDraft = { draftId ->
                if (draftToEdit?.id == draftId) draftToEdit = null
                viewModel.deleteDraftNote(draftId)
                feedbackMessage = "Draft discarded."
              },
              onClearActiveDraft = { draftToEdit = null }
            )
            3 -> ManageDraftsTab(
              drafts = filteredDrafts,
              isEditable = isAuthorizedToEdit,
              onResumeDraft = { draft ->
                draftToEdit = draft
                selectedTab = 2 // Switch to study notes tab to edit
                feedbackMessage = "Resumed draft: '${draft.title}'"
              },
              onDeleteDraft = { draftId ->
                if (draftToEdit?.id == draftId) draftToEdit = null
                viewModel.deleteDraftNote(draftId)
                feedbackMessage = "Draft deleted."
              }
            )
            4 -> ManageLiveSessionsTab(
              liveSessions = liveSessions,
              defaultClass = selectedClassBlock ?: AcademicClass.CLASS_11,
              isEditable = isAuthorizedToEdit,
              onAddLiveSession = { title, subject, academicClass, durationMin, meetingUrl, agenda, status ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner can schedule live sessions."
                } else {
                  viewModel.addLiveSession(
                    title = title,
                    subject = subject,
                    academicClass = academicClass,
                    scheduledTimeMillis = System.currentTimeMillis() + (2 * 3600 * 1000),
                    durationMinutes = durationMin,
                    meetingUrl = meetingUrl,
                    agenda = agenda,
                    status = status
                  )
                  feedbackMessage = "Live session published to students!"
                }
              },
              onUpdateStatus = { sessionId, status ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only owner can change session status."
                } else {
                  viewModel.updateLiveSessionStatus(sessionId, status)
                  feedbackMessage = "Session status updated to ${status.name}."
                }
              },
              onDeleteSession = { sessionId ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only owner can delete sessions."
                } else {
                  viewModel.deleteLiveSession(sessionId)
                  feedbackMessage = "Live session deleted."
                }
              }
            )
            5 -> ManageCourseSubscriptionsTab(
              courses = courseSubscriptions,
              isEditable = isAuthorizedToEdit,
              onAddCourse = { title, focus, targetClass, price, duration, desc, features ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner can add courses."
                } else {
                  viewModel.addCourseSubscription(title, focus, targetClass, price, duration, desc, features)
                  feedbackMessage = "New course bundle '$title' created!"
                }
              }
            )
            6 -> ManageVideosTab(
              videos = videoLectures,
              isEditable = isAuthorizedToEdit,
              onAddVideo = { title, subject, topic, duration, summary ->
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only verified owner can publish videos."
                } else {
                  viewModel.addVideoLecture(title, subject, topic, duration, summary, selectedClassBlock ?: AcademicClass.CLASS_11)
                  feedbackMessage = "Video Masterclass added to Dr. Salar's catalog!"
                }
              },
              onDeleteVideo = {
                if (!isAuthorizedToEdit) {
                  feedbackMessage = "Access Denied: Only owner can delete videos."
                } else {
                  viewModel.deleteVideoLecture(it)
                }
              }
            )
          }
        }
      }

    }
    }
  }

  // Owner Auth / Domain Verification Dialog
  if (showAuthDialog) {
    OwnerAuthDialog(
      currentEmail = adminAuthState.currentUserEmail,
      authorizedEmail = adminAuthState.authorizedOwnerEmail,
      onDismiss = { showAuthDialog = false },
      onConfirmEmail = { email ->
        val ok = viewModel.verifyOwnerEmail(email)
        showAuthDialog = false
        feedbackMessage = if (ok) {
          "Authenticated as Owner: ${email}. Edit permissions unlocked."
        } else {
          "Unauthorized email (${email}). Restricted to view-only mode."
        }
      }
    )
  }
}

/**
 * 1. Owner & Domain Security Status Bar
 */
@Composable
fun OwnerDomainStatusBar(
  authState: com.example.data.model.AdminAuthState,
  userRole: com.example.data.model.UserRole = com.example.data.model.UserRole.OWNER,
  onVerifyClick: () -> Unit,
  onToggleLock: () -> Unit
) {
  val isEditable = authState.isEditModeUnlocked

  Surface(
    color = if (isEditable) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(if (isEditable) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isEditable) Icons.Default.Security else Icons.Default.Warning,
          contentDescription = null,
          tint = if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isEditable) "RBAC Verified (${userRole.label})" else "Protected Mode (Read-Only)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          )
          Spacer(modifier = Modifier.width(4.dp))
          if (isEditable) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
          }
        }
        Text(
          text = "Role: ${userRole.name} • Email: ${authState.currentUserEmail.ifBlank { authState.authorizedOwnerEmail }}",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Lock / Unlock button
      OutlinedButton(
        onClick = onToggleLock,
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Icon(
          imageVector = if (isEditable) Icons.Default.LockOpen else Icons.Default.Lock,
          contentDescription = null,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isEditable) "Lock" else "Unlock",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * 2. Academic Class Block Selector (11th Class vs 12th Class vs All)
 */
@Composable
fun AcademicClassBlockSelector(
  selectedBlock: AcademicClass?,
  onSelectBlock: (AcademicClass?) -> Unit,
  class11Count: Int,
  class12Count: Int,
  draftsCount: Int
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // 11th Class Block Chip
    FilterChip(
      selected = selectedBlock == AcademicClass.CLASS_11,
      onClick = { onSelectBlock(AcademicClass.CLASS_11) },
      label = {
        Text("11th Class Block", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      },
      leadingIcon = if (selectedBlock == AcademicClass.CLASS_11) {
        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
      } else null,
      modifier = Modifier.weight(1f)
    )

    // 12th Class Block Chip
    FilterChip(
      selected = selectedBlock == AcademicClass.CLASS_12,
      onClick = { onSelectBlock(AcademicClass.CLASS_12) },
      label = {
        Text("12th Class Block", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      },
      leadingIcon = if (selectedBlock == AcademicClass.CLASS_12) {
        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
      } else null,
      modifier = Modifier.weight(1f)
    )

    // All Syllabus View
    FilterChip(
      selected = selectedBlock == null,
      onClick = { onSelectBlock(null) },
      label = {
        Text("All", fontSize = 11.sp)
      }
    )
  }
}

/**
 * Block Header Card describing syllabus scope for 11th or 12th class
 */
@Composable
fun ClassBlockHeaderCard(selectedClassBlock: AcademicClass?) {
  val title = when (selectedClassBlock) {
    AcademicClass.CLASS_11 -> "11th Class (FSc Part 1) Syllabus Management"
    AcademicClass.CLASS_12 -> "12th Class (FSc Part 2) Syllabus Management"
    null -> "Complete MDCAT Combined Syllabus Block"
  }

  val description = when (selectedClassBlock) {
    AcademicClass.CLASS_11 -> "Focus: Cell Biology, Bioenergetics, Enzymes, Stoichiometry, Mechanics, Gas Laws & Fluid Dynamics."
    AcademicClass.CLASS_12 -> "Focus: Nervous Coordination, Reproduction, Genetics & DNA, Evolution, Organic Chemistry, Electromagnetism."
    null -> "Managing curriculum assets across all First Year and Second Year Pre-Medical subjects."
  }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Book,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      }
    }
  }
}

/**
 * Manage MCQs Tab with Academic Class selector and Owner gating
 */
@Composable
fun ManageMcqsTab(
  questions: List<QuizQuestion>,
  defaultClass: AcademicClass,
  isEditable: Boolean,
  remainingDays: Int = 72,
  onUpdateRemainingDays: (Int) -> Unit = {},
  onAddMcq: (MDCATSubject, String, String, List<String>, Int, String, String, AcademicClass) -> Unit,
  onDeleteMcq: (String) -> Unit
) {
  var subject by remember { mutableStateOf(MDCATSubject.BIOLOGY) }
  var academicClass by remember(defaultClass) { mutableStateOf(defaultClass) }
  var topic by remember { mutableStateOf("") }
  var questionText by remember { mutableStateOf("") }
  var optA by remember { mutableStateOf("") }
  var optB by remember { mutableStateOf("") }
  var optC by remember { mutableStateOf("") }
  var optD by remember { mutableStateOf("") }
  var correctOptionIndex by remember { mutableIntStateOf(0) }
  var explanation by remember { mutableStateOf("") }
  var highYieldTip by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      // NEW SECTION: MDCAT Exam Countdown & Remaining Days Controller
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_admin_remaining_days_controller"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Exam Countdown Controller",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                  text = "Change the remaining days until MDCAT 2026",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "$remainingDays DAYS LEFT",
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              val nowMillis = remember { System.currentTimeMillis() }
              val nowStr = remember(nowMillis) {
                SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(nowMillis))
              }
              Text(
                text = nowStr,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          var daysInput by remember(remainingDays) { mutableStateOf(remainingDays.toString()) }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = {
                val updated = (remainingDays - 5).coerceAtLeast(0)
                daysInput = updated.toString()
                onUpdateRemainingDays(updated)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("-5d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = {
                val updated = (remainingDays - 1).coerceAtLeast(0)
                daysInput = updated.toString()
                onUpdateRemainingDays(updated)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("-1d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
              value = daysInput,
              onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 3) {
                  daysInput = input
                }
              },
              label = { Text("Days", fontSize = 10.sp) },
              singleLine = true,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1.3f)
            )

            OutlinedButton(
              onClick = {
                val updated = remainingDays + 1
                daysInput = updated.toString()
                onUpdateRemainingDays(updated)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("+1d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = {
                val updated = remainingDays + 5
                daysInput = updated.toString()
                onUpdateRemainingDays(updated)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("+5d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(30, 45, 60, 72, 90).forEach { preset ->
                FilterChip(
                  selected = remainingDays == preset,
                  onClick = {
                    daysInput = preset.toString()
                    onUpdateRemainingDays(preset)
                  },
                  label = { Text("${preset}d", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                  shape = RoundedCornerShape(6.dp)
                )
              }
            }

            Button(
              onClick = {
                val parsed = daysInput.toIntOrNull() ?: remainingDays
                onUpdateRemainingDays(parsed)
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      // 180 MCQs Official Syllabus Distribution Banner
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "180 MCQs Standard: Bio 81 • Chem 45 • Phys 36 • English 9 • Logic 9",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Upload New Practice MCQ",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = "Add questions curated by Dr. Salar with options, answer keys, and high-yield rationales.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Academic Class Selector (11th vs 12th)
          Text("Target Syllabus Class:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AcademicClass.values().forEach { ac ->
              FilterChip(
                selected = academicClass == ac,
                onClick = { academicClass = ac },
                label = { Text(ac.shortName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Subject selector chips
          Text("Subject:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            MDCATSubject.values().forEach { sub ->
              FilterChip(
                selected = subject == sub,
                onClick = { subject = sub },
                label = { Text(sub.displayName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic / Chapter (e.g., Enzyme Kinetics)") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            label = { Text("MCQ Question Stem") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text("Answer Options (Select correct option radio button):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

          listOf(
            Triple("A", optA) { s: String -> optA = s },
            Triple("B", optB) { s: String -> optB = s },
            Triple("C", optC) { s: String -> optC = s },
            Triple("D", optD) { s: String -> optD = s }
          ).forEachIndexed { idx, (label, value, setter) ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              RadioButton(
                selected = correctOptionIndex == idx,
                onClick = { correctOptionIndex = idx },
                enabled = isEditable
              )
              OutlinedTextField(
                value = value,
                onValueChange = setter,
                label = { Text("Option $label") },
                enabled = isEditable,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("High-Yield Medical Explanation") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = highYieldTip,
            onValueChange = { highYieldTip = it },
            label = { Text("Dr. Salar's Distractor Warning (Optional)") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              if (questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
                onAddMcq(
                  subject,
                  topic,
                  questionText,
                  listOf(optA, optB, optC.ifBlank { "None of these" }, optD.ifBlank { "All of these" }),
                  correctOptionIndex,
                  explanation.ifBlank { "High-yield concept formulated by Dr. Salar." },
                  highYieldTip,
                  academicClass
                )
                questionText = ""
                optA = ""
                optB = ""
                optC = ""
                optD = ""
                explanation = ""
                highYieldTip = ""
              }
            },
            enabled = isEditable,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_publish_mcq")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publish MCQ to ${academicClass.shortName}", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item {
      Text(
        text = "Current Practice MCQs (${questions.size})",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
      )
    }

    items(questions, key = { it.id }) { q ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Surface(
                color = q.subject.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "${q.subject.displayName} • ${q.topic}",
                  color = q.subject.color,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }

              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = q.academicClass.shortName,
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            if (isEditable) {
              IconButton(
                onClick = { onDeleteMcq(q.id) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(q.questionText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "Correct Option: ${'A' + q.correctOptionIndex} (${q.options.getOrElse(q.correctOptionIndex) { "" }})",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

/**
 * Manage PDFs Tab with Class support
 */
@Composable
fun ManagePdfsTab(
  pdfs: List<PdfResource>,
  defaultClass: AcademicClass,
  isEditable: Boolean,
  onAddPdf: (String, MDCATSubject, String, String, String, Float, AcademicClass) -> Unit,
  onDeletePdf: (String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf(MDCATSubject.BIOLOGY) }
  var academicClass by remember(defaultClass) { mutableStateOf(defaultClass) }
  var chapter by remember { mutableStateOf("") }
  var pdfUrl by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var fileSizeMb by remember { mutableStateOf("7.5") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Link New PDF Resource",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = "Link syllabus handbooks, solved past papers, formula sheets, or Dr. Salar's notes.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text("Academic Class Block:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AcademicClass.values().forEach { ac ->
              FilterChip(
                selected = academicClass == ac,
                onClick = { academicClass = ac },
                label = { Text(ac.shortName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Subject selector chips
          Text("Subject:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            MDCATSubject.values().forEach { sub ->
              FilterChip(
                selected = subject == sub,
                onClick = { subject = sub },
                label = { Text(sub.displayName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("PDF Document Title (e.g., Solved Past Papers)") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = chapter,
            onValueChange = { chapter = it },
            label = { Text("Syllabus Chapter / Focus Unit") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = pdfUrl,
            onValueChange = { pdfUrl = it },
            label = { Text("PDF Direct Download URL or Drive Link") },
            placeholder = { Text("https://example.com/handbook.pdf") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = fileSizeMb,
            onValueChange = { fileSizeMb = it },
            label = { Text("File Size in MB") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Resource Description & Dr. Salar's Study Guidance") },
            enabled = isEditable,
            modifier = Modifier
              .fillMaxWidth()
              .height(90.dp),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              if (title.isNotBlank()) {
                val size = fileSizeMb.toFloatOrNull() ?: 5.0f
                val finalUrl = pdfUrl.ifBlank { "https://mdcatmaster.edu.pk/resources/${title.lowercase().replace(" ", "_")}.pdf" }
                onAddPdf(title, subject, chapter, finalUrl, description, size, academicClass)
                title = ""
                chapter = ""
                pdfUrl = ""
                description = ""
              }
            },
            enabled = isEditable,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_link_pdf")
          ) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Link PDF to ${academicClass.shortName}", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item {
      Text(
        text = "Linked PDF Resources (${pdfs.size})",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
      )
    }

    items(pdfs, key = { it.id }) { pdf ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(pdf.subject.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.PictureAsPdf,
              contentDescription = null,
              tint = pdf.subject.color,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(pdf.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
              "${pdf.subject.displayName} • ${pdf.academicClass.shortName} • ${pdf.chapter} • ${pdf.fileSizeMb} MB",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pdf.description.isNotBlank()) {
              Text(
                pdf.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 2
              )
            }
          }

          if (isEditable) {
            IconButton(
              onClick = { onDeletePdf(pdf.id) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

/**
 * 3. Manage Study Materials Tab with 'Save as Draft' feature and Active Drafts Tray
 */
@Composable
fun ManageStudyMaterialsTab(
  materials: List<StudyMaterial>,
  drafts: List<StudyNoteDraft>,
  defaultClass: AcademicClass,
  activeDraft: StudyNoteDraft?,
  isEditable: Boolean,
  onAddMaterial: (String, MDCATSubject, String, Int, List<String>, List<String>, String, String?, AcademicClass, String?) -> Unit,
  onSaveDraft: (String?, String, MDCATSubject, AcademicClass, String, Int, String, String, String, String) -> Unit,
  onDeleteMaterial: (String) -> Unit,
  onResumeDraft: (StudyNoteDraft) -> Unit,
  onDiscardDraft: (String) -> Unit,
  onClearActiveDraft: () -> Unit
) {
  var editingDraftId by remember { mutableStateOf<String?>(null) }
  var title by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf(MDCATSubject.BIOLOGY) }
  var academicClass by remember(defaultClass) { mutableStateOf(defaultClass) }
  var chapter by remember { mutableStateOf("") }
  var readTimeMin by remember { mutableStateOf("10") }
  var keyConceptsText by remember { mutableStateOf("") }
  var mnemonicsText by remember { mutableStateOf("") }
  var contentMarkdown by remember { mutableStateOf("") }
  var linkedPdfUrl by remember { mutableStateOf("") }

  // Synchronize inputs when a draft is resumed
  androidx.compose.runtime.LaunchedEffect(activeDraft) {
    if (activeDraft != null) {
      editingDraftId = activeDraft.id
      title = activeDraft.title
      subject = activeDraft.subject
      academicClass = activeDraft.academicClass
      chapter = activeDraft.chapter
      readTimeMin = activeDraft.readTimeMin.toString()
      keyConceptsText = activeDraft.keyConceptsRaw
      mnemonicsText = activeDraft.mnemonicsRaw
      contentMarkdown = activeDraft.contentMarkdown
      linkedPdfUrl = activeDraft.linkedPdfUrl
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Draft in Progress Banner
    if (editingDraftId != null) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Currently Editing Draft",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Text(
                text = "Changes can be updated as draft or published directly to syllabus.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
              )
            }
            TextButton(
              onClick = {
                editingDraftId = null
                title = ""
                chapter = ""
                keyConceptsText = ""
                mnemonicsText = ""
                contentMarkdown = ""
                linkedPdfUrl = ""
                onClearActiveDraft()
              }
            ) {
              Text("New Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Content Creation Form
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (editingDraftId != null) "Edit Draft Study Notes" else "Create High-Yield Study Notes",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium
            )

            if (editingDraftId != null) {
              Surface(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "DRAFT ACTIVE",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Academic Class Block Selector
          Text("Target Syllabus Block:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AcademicClass.values().forEach { ac ->
              FilterChip(
                selected = academicClass == ac,
                onClick = { academicClass = ac },
                label = { Text(ac.shortName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Subject selector chips
          Text("Subject:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            MDCATSubject.values().forEach { sub ->
              FilterChip(
                selected = subject == sub,
                onClick = { subject = sub },
                label = { Text(sub.displayName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Chapter / Material Title") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = chapter,
            onValueChange = { chapter = it },
            label = { Text("Syllabus Unit (e.g. Molecular Genetics)") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = readTimeMin,
            onValueChange = { readTimeMin = it },
            label = { Text("Estimated Read Time (Minutes)") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = keyConceptsText,
            onValueChange = { keyConceptsText = it },
            label = { Text("Key Takeaways (one per line)") },
            enabled = isEditable,
            modifier = Modifier
              .fillMaxWidth()
              .height(90.dp),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = mnemonicsText,
            onValueChange = { mnemonicsText = it },
            label = { Text("Dr. Salar's Mnemonics (one per line)") },
            enabled = isEditable,
            modifier = Modifier
              .fillMaxWidth()
              .height(90.dp),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = contentMarkdown,
            onValueChange = { contentMarkdown = it },
            label = { Text("Full Study Notes (Markdown format)") },
            enabled = isEditable,
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = linkedPdfUrl,
            onValueChange = { linkedPdfUrl = it },
            label = { Text("Optional Companion PDF Download URL") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // TWO MAIN ACTIONS: "Save as Draft" vs "Publish Notes"
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Save as Draft Button
            OutlinedButton(
              onClick = {
                val time = readTimeMin.toIntOrNull() ?: 10
                onSaveDraft(
                  editingDraftId,
                  title,
                  subject,
                  academicClass,
                  chapter,
                  time,
                  keyConceptsText,
                  mnemonicsText,
                  contentMarkdown,
                  linkedPdfUrl
                )
              },
              enabled = isEditable && title.isNotBlank(),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_save_draft")
            ) {
              Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Draft", fontWeight = FontWeight.Bold)
            }

            // Publish Live Button
            Button(
              onClick = {
                if (title.isNotBlank()) {
                  val concepts = keyConceptsText.split("\n").filter { it.isNotBlank() }
                  val mnemonics = mnemonicsText.split("\n").filter { it.isNotBlank() }
                  val time = readTimeMin.toIntOrNull() ?: 10
                  onAddMaterial(
                    title,
                    subject,
                    chapter,
                    time,
                    concepts,
                    mnemonics,
                    contentMarkdown.ifBlank { "Comprehensive notes compiled by Dr. Salar." },
                    linkedPdfUrl.ifBlank { null },
                    academicClass,
                    editingDraftId
                  )
                  title = ""
                  chapter = ""
                  keyConceptsText = ""
                  mnemonicsText = ""
                  contentMarkdown = ""
                  linkedPdfUrl = ""
                  editingDraftId = null
                }
              },
              enabled = isEditable && title.isNotBlank(),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1.2f)
                .testTag("btn_publish_material")
            ) {
              Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Publish to ${academicClass.shortName}", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Active Drafts Quick Tray
    if (drafts.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Active Saved Drafts (${drafts.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
          )
          Text(
            text = "Dr. Salar's Work-in-Progress",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      items(drafts, key = { it.id }) { draft ->
        DraftItemCard(
          draft = draft,
          isEditable = isEditable,
          isCurrentlyActive = editingDraftId == draft.id,
          onResume = { onResumeDraft(draft) },
          onDelete = { onDiscardDraft(draft.id) }
        )
      }
    }

    // Existing Live Study Materials
    item {
      Text(
        text = "Published Syllabus Chapters (${materials.size})",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
      )
    }

    items(materials, key = { it.id }) { material ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(material.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = material.academicClass.shortName,
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              "${material.subject.displayName} • ${material.chapter} • ${material.readTimeMin}m read",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (isEditable) {
            IconButton(
              onClick = { onDeleteMaterial(material.id) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

/**
 * 4. Dedicated Drafts Management Tab
 */
@Composable
fun ManageDraftsTab(
  drafts: List<StudyNoteDraft>,
  isEditable: Boolean,
  onResumeDraft: (StudyNoteDraft) -> Unit,
  onDeleteDraft: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dr. Salar's Notes Workspace", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Complex notes saved in draft mode are preserved here across sessions until ready to be published to the live student syllabus.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (drafts.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No Active Drafts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Use 'Save Draft' in the Study Notes tab to save work in progress.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    } else {
      items(drafts, key = { it.id }) { draft ->
        DraftItemCard(
          draft = draft,
          isEditable = isEditable,
          isCurrentlyActive = false,
          onResume = { onResumeDraft(draft) },
          onDelete = { onDeleteDraft(draft.id) }
        )
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

/**
 * Reusable Draft Card Component
 */
@Composable
fun DraftItemCard(
  draft: StudyNoteDraft,
  isEditable: Boolean,
  isCurrentlyActive: Boolean,
  onResume: () -> Unit,
  onDelete: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
  val formattedDate = remember(draft.lastSavedMillis) { dateFormat.format(Date(draft.lastSavedMillis)) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentlyActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Surface(
            color = draft.subject.color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = draft.subject.displayName,
              color = draft.subject.color,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = draft.academicClass.shortName,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Text(
          text = "Saved $formattedDate",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = draft.title.ifBlank { "Untitled Note Draft" },
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
      if (draft.chapter.isNotBlank()) {
        Text(
          text = "Unit: ${draft.chapter}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isEditable) {
          TextButton(
            onClick = onDelete,
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Discard", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
          }

          Spacer(modifier = Modifier.width(8.dp))
        }

        Button(
          onClick = onResume,
          shape = RoundedCornerShape(6.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Resume Editing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Manage Videos Tab
 */
@Composable
fun ManageVideosTab(
  videos: List<VideoLecture>,
  isEditable: Boolean,
  onAddVideo: (String, MDCATSubject, String, Int, String) -> Unit,
  onDeleteVideo: (String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf(MDCATSubject.BIOLOGY) }
  var topic by remember { mutableStateOf("") }
  var durationMin by remember { mutableStateOf("40") }
  var summaryNotes by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Add Video Masterclass",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Subject selector chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            MDCATSubject.values().forEach { sub ->
              FilterChip(
                selected = subject == sub,
                onClick = { subject = sub },
                label = { Text(sub.displayName, fontSize = 11.sp) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Video Lecture Title") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Core Topic Focus") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = durationMin,
            onValueChange = { durationMin = it },
            label = { Text("Duration in Minutes") },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = summaryNotes,
            onValueChange = { summaryNotes = it },
            label = { Text("Summary Notes & Timestamps Breakdown") },
            enabled = isEditable,
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              if (title.isNotBlank()) {
                val duration = durationMin.toIntOrNull() ?: 30
                onAddVideo(title, subject, topic, duration, summaryNotes)
                title = ""
                topic = ""
                summaryNotes = ""
              }
            },
            enabled = isEditable,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_publish_video")
          ) {
            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Video Masterclass", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item {
      Text(
        text = "Current Video Masterclasses (${videos.size})",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
      )
    }

    items(videos, key = { it.id }) { video ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(video.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
              "${video.subject.displayName} • ${video.topic} • ${video.durationMinutes}m • ${video.educatorName}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (isEditable) {
            IconButton(
              onClick = { onDeleteVideo(video.id) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

/**
 * Owner Authentication & Domain Access Dialog
 */
@Composable
fun OwnerAuthDialog(
  currentEmail: String,
  authorizedEmail: String,
  onDismiss: () -> Unit,
  onConfirmEmail: (String) -> Unit
) {
  var inputEmail by remember { mutableStateOf(currentEmail) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Owner & Domain Access Control", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column {
        Text(
          text = "Modifications to the dashboard UI and live syllabus are restricted strictly to the verified owner and authorized domain:",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Owner Email: $authorizedEmail", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Authorized Domain: @gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Text("Faculty: Dr. Salar (Primary Syllabus Admin)", fontSize = 11.sp)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = inputEmail,
          onValueChange = { inputEmail = it },
          label = { Text("Enter Verified Owner Email") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmEmail(inputEmail) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Authenticate & Unlock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

/**
 * Access Denied View enforcing Role-Based Access Control (RBAC).
 * Only users identified as 'Owner' or 'Admin' can view or modify study content.
 */
@Composable
fun AdminAccessDeniedView(
  currentUserEmail: String,
  userRole: com.example.data.model.UserRole,
  onLoginAsAdminClick: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("admin_access_denied_view"),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.errorContainer,
      modifier = Modifier.size(72.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Access Denied",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(38.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = "Role-Based Access Control (RBAC)",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Instructor Access Denied",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Only users identified as 'Owner' (Dr. Salar) or 'Admin' have permission to view or modify study syllabus content, practice MCQs, PDF handbooks, and live sessions.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Current Account Identity Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      )
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Active Session:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
          ) {
            Text(
              text = userRole.label.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = currentUserEmail.ifBlank { "Unauthenticated Guest" },
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Required: Owner ('shaukatsalar231@gmail.com') or Admin account with syllabus management permissions.",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.error
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onLoginAsAdminClick,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_switch_to_admin_login"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
      )
    ) {
      Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign In with Gmail / Google (Owner or Admin)", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
      onClick = onClose,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_return_to_student_portal"),
      shape = RoundedCornerShape(12.dp)
    ) {
      Text("Return to Student Study Portal")
    }
  }
}
