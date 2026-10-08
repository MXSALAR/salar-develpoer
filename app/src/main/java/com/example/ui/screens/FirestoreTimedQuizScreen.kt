package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MDCATSubject
import com.example.data.model.QuizQuestion
import com.example.data.repository.QuizSessionResult
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.theme.LogicPink
import com.example.ui.theme.PhysicsPurple
import com.example.ui.viewmodel.MDCATViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class QuizPacingMode(val title: String, val secondsPerMcq: Int, val description: String) {
  PMC_STANDARD("PMC Standard", 54, "54s / question (Official 180 MCQs / 210m speed)"),
  RAPID_SPRINT("Rapid Sprint", 30, "30s / question (High-intensity reflexes)"),
  MASTERY_PACE("Mastery Pace", 75, "75s / question (In-depth problem solving)")
}

enum class QuizFlowState {
  SETUP,
  FETCHING_FIRESTORE,
  ACTIVE_TEST,
  SESSION_SUMMARY
}

/**
 * Timed quiz interface that:
 * 1. Fetches authentic MDCAT-style questions from Firebase Firestore
 * 2. Manages real-time countdown timer with pause/resume and warning thresholds
 * 3. Tracks student completion speed per question and per session (seconds/MCQ)
 * 4. Tracks accuracy, question pacing, and stores completed session telemetry into Firestore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirestoreTimedQuizScreen(
  viewModel: MDCATViewModel,
  onExit: () -> Unit,
  onNavigateToAnalytics: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val cloudSyncStatus by viewModel.firestoreCloudSyncStatus.collectAsState()
  val recentSessions by viewModel.firestoreQuizSessions.collectAsState()

  var flowState by remember { mutableStateOf(QuizFlowState.SETUP) }
  var selectedSubject by remember { mutableStateOf<MDCATSubject?>(null) }
  var selectedQuestionCount by remember { mutableIntStateOf(10) }
  var selectedPacingMode by remember { mutableStateOf(QuizPacingMode.PMC_STANDARD) }

  // Fetched questions
  var fetchedQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
  var currentQuestionIndex by remember { mutableIntStateOf(0) }

  // Timing states
  val totalAllottedSeconds = remember(selectedQuestionCount, selectedPacingMode) {
    selectedQuestionCount * selectedPacingMode.secondsPerMcq
  }
  var secondsRemaining by remember { mutableIntStateOf(totalAllottedSeconds) }
  var isTimerPaused by remember { mutableStateOf(false) }
  var showPauseDialog by remember { mutableStateOf(false) }
  var showExitConfirmDialog by remember { mutableStateOf(false) }
  var showSubmitConfirmDialog by remember { mutableStateOf(false) }

  // Telemetry per session
  val userAnswers = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> selectedOptionIndex
  val questionPacingSeconds = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> seconds spent
  val flaggedQuestions = remember { mutableStateOf(setOf<Int>()) }

  // Real-time tracking of current question elapsed seconds
  var currentQuestionStartSec by remember { mutableIntStateOf(0) }
  var completedSessionResult by remember { mutableStateOf<QuizSessionResult?>(null) }

  val coroutineScope = rememberCoroutineScope()

  // Helper function to finalize session and compute speed/accuracy telemetry
  val finishSession = {
    // Record time on the last active question
    val spentOnCurrent = (totalAllottedSeconds - secondsRemaining - currentQuestionStartSec).coerceAtLeast(1)
    val existing = questionPacingSeconds[currentQuestionIndex] ?: 0
    questionPacingSeconds[currentQuestionIndex] = existing + spentOnCurrent

    val questions = fetchedQuestions
    val totalQ = questions.size
    val answeredCount = userAnswers.size
    var correctCount = 0
    var wrongCount = 0
    val perQuestionCorrect = mutableMapOf<Int, Boolean>()

    questions.forEachIndexed { idx, q ->
      val userAns = userAnswers[idx]
      if (userAns != null && userAns == q.correctOptionIndex) {
        correctCount++
        perQuestionCorrect[idx] = true
      } else {
        wrongCount++
        perQuestionCorrect[idx] = false
      }
    }

    val accuracy = if (totalQ > 0) (correctCount.toFloat() / totalQ.toFloat()) * 100f else 0f
    val timeTakenSec = (totalAllottedSeconds - secondsRemaining).coerceAtLeast(1)
    val avgSpeed = if (totalQ > 0) timeTakenSec.toFloat() / totalQ.toFloat() else 0f

    val pacingList = questionPacingSeconds.values.toList()
    val fastest = pacingList.minOrNull() ?: 0
    val slowest = pacingList.maxOrNull() ?: 0

    val speedGrade = when {
      avgSpeed <= 35f -> "EXPRESS SPEED (≤35s/MCQ)"
      avgSpeed <= 54f -> "OPTIMAL PMC PACE (≤54s/MCQ)"
      else -> "NEEDS PACING ACCELERATION (>54s/MCQ)"
    }

    val accGrade = when {
      accuracy >= 90f -> "Distinction Tier (90%+)"
      accuracy >= 80f -> "High Merit Tier (80-89%)"
      accuracy >= 65f -> "Passing MDCAT Tier (65-79%)"
      else -> "Foundation Review Required (<65%)"
    }

    val subjectLabel = selectedSubject?.displayName ?: "Full Mock"
    val session = QuizSessionResult(
      sessionId = "fs_sess_${System.currentTimeMillis()}",
      testTitle = "Firestore MDCAT Quiz: $subjectLabel",
      subject = selectedSubject,
      totalQuestions = totalQ,
      answeredCount = answeredCount,
      correctCount = correctCount,
      wrongCount = wrongCount,
      accuracyPercent = accuracy,
      totalAllottedSeconds = totalAllottedSeconds,
      timeTakenSeconds = timeTakenSec,
      averageSpeedSecondsPerQuestion = avgSpeed,
      fastestQuestionSeconds = fastest,
      slowestQuestionSeconds = slowest,
      speedPacingGrade = speedGrade,
      accuracyGrade = accGrade,
      perQuestionPacing = questionPacingSeconds.toMap(),
      perQuestionCorrect = perQuestionCorrect,
      userAnswers = userAnswers.toMap(),
      timestampMillis = System.currentTimeMillis()
    )

    completedSessionResult = session
    flowState = QuizFlowState.SESSION_SUMMARY

    // Persist to Firestore & Local Room via ViewModel
    viewModel.recordFirestoreQuizSession(session)
  }

  // Active quiz countdown timer loop
  LaunchedEffect(flowState, isTimerPaused) {
    if (flowState == QuizFlowState.ACTIVE_TEST && !isTimerPaused) {
      while (secondsRemaining > 0 && flowState == QuizFlowState.ACTIVE_TEST) {
        delay(1000L)
        if (!isTimerPaused) {
          secondsRemaining -= 1
        }
      }
      if (secondsRemaining <= 0 && flowState == QuizFlowState.ACTIVE_TEST) {
        // Auto-submit on time expiry
        finishSession()
      }
    }
  }

  // Track question change to calculate per-question pacing
  fun navigateToQuestion(newIndex: Int) {
    if (newIndex in fetchedQuestions.indices && newIndex != currentQuestionIndex) {
      val timeSpentOnPrevious = (totalAllottedSeconds - secondsRemaining - currentQuestionStartSec).coerceAtLeast(1)
      val existing = questionPacingSeconds[currentQuestionIndex] ?: 0
      questionPacingSeconds[currentQuestionIndex] = existing + timeSpentOnPrevious

      currentQuestionIndex = newIndex
      currentQuestionStartSec = totalAllottedSeconds - secondsRemaining
    }
  }

  // Main UI routing based on QuizFlowState
  when (flowState) {
    QuizFlowState.SETUP -> {
      FirestoreQuizSetupScreen(
        selectedSubject = selectedSubject,
        onSelectSubject = { selectedSubject = it },
        selectedCount = selectedQuestionCount,
        onSelectCount = { selectedQuestionCount = it },
        selectedPacingMode = selectedPacingMode,
        onSelectPacingMode = { selectedPacingMode = it },
        cloudSyncStatus = cloudSyncStatus,
        recentSessions = recentSessions,
        onStartQuiz = {
          flowState = QuizFlowState.FETCHING_FIRESTORE
          coroutineScope.launch {
            val questions = viewModel.fetchFirestoreQuestions(selectedSubject, selectedQuestionCount)
            fetchedQuestions = questions.ifEmpty {
              viewModel.allQuizQuestions.value.take(selectedQuestionCount)
            }
            // Reset timers & state
            secondsRemaining = selectedQuestionCount * selectedPacingMode.secondsPerMcq
            currentQuestionIndex = 0
            currentQuestionStartSec = 0
            userAnswers.clear()
            questionPacingSeconds.clear()
            flaggedQuestions.value = emptySet()
            isTimerPaused = false
            flowState = QuizFlowState.ACTIVE_TEST
          }
        },
        onSeedQuestions = { viewModel.seedFirestoreQuestions() },
        onExit = onExit,
        modifier = modifier
      )
    }

    QuizFlowState.FETCHING_FIRESTORE -> {
      FirestoreFetchingLoadingScreen(
        cloudSyncStatus = cloudSyncStatus,
        modifier = modifier
      )
    }

    QuizFlowState.ACTIVE_TEST -> {
      ActiveFirestoreQuizInterface(
        questions = fetchedQuestions,
        currentIndex = currentQuestionIndex,
        totalAllottedSeconds = totalAllottedSeconds,
        secondsRemaining = secondsRemaining,
        isTimerPaused = isTimerPaused,
        userAnswers = userAnswers,
        flaggedQuestions = flaggedQuestions.value,
        questionPacingSeconds = questionPacingSeconds,
        currentQuestionStartSec = currentQuestionStartSec,
        pacingMode = selectedPacingMode,
        onNavigateQuestion = { navigateToQuestion(it) },
        onSelectOption = { optIdx -> userAnswers[currentQuestionIndex] = optIdx },
        onClearOption = { userAnswers.remove(currentQuestionIndex) },
        onToggleFlag = {
          val current = flaggedQuestions.value
          flaggedQuestions.value = if (current.contains(currentQuestionIndex)) {
            current - currentQuestionIndex
          } else {
            current + currentQuestionIndex
          }
        },
        onPauseToggle = {
          isTimerPaused = !isTimerPaused
          showPauseDialog = isTimerPaused
        },
        onRequestSubmit = { showSubmitConfirmDialog = true },
        onRequestExit = { showExitConfirmDialog = true },
        modifier = modifier
      )
    }

    QuizFlowState.SESSION_SUMMARY -> {
      val session = completedSessionResult
      if (session != null) {
        FirestoreSessionSummaryScreen(
          session = session,
          questions = fetchedQuestions,
          onRetake = {
            flowState = QuizFlowState.SETUP
          },
          onNavigateToAnalytics = onNavigateToAnalytics,
          onExit = onExit,
          modifier = modifier
        )
      }
    }
  }

  // Submit confirmation dialog
  if (showSubmitConfirmDialog) {
    val unansweredCount = fetchedQuestions.size - userAnswers.size
    AlertDialog(
      onDismissRequest = { showSubmitConfirmDialog = false },
      title = { Text("Submit Timed Quiz?", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("You have answered ${userAnswers.size} of ${fetchedQuestions.size} questions.")
          if (unansweredCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "$unansweredCount question(s) remain unanswered.",
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.Medium
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text("Your completion speed and accuracy metrics will be recorded in Firebase Firestore.")
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showSubmitConfirmDialog = false
            finishSession()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("Confirm Submit")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSubmitConfirmDialog = false }) {
          Text("Continue Quiz")
        }
      }
    )
  }

  // Pause Dialog
  if (showPauseDialog) {
    AlertDialog(
      onDismissRequest = {
        isTimerPaused = false
        showPauseDialog = false
      },
      icon = {
        Icon(Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
      },
      title = { Text("Quiz Paused", fontWeight = FontWeight.Bold) },
      text = {
        Text("Timer and telemetry tracking are currently paused. Press Resume when you are ready to continue answering.")
      },
      confirmButton = {
        Button(
          onClick = {
            isTimerPaused = false
            showPauseDialog = false
          }
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Resume Quiz")
        }
      }
    )
  }

  // Exit Confirmation Dialog
  if (showExitConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showExitConfirmDialog = false },
      title = { Text("Exit Quiz Session?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to exit? Your progress and speed metrics for this in-progress session will be lost.")
      },
      confirmButton = {
        Button(
          onClick = {
            showExitConfirmDialog = false
            onExit()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Exit Session")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExitConfirmDialog = false }) {
          Text("Keep Going")
        }
      }
    )
  }
}

/**
 * Lobby & Setup screen for configuring the Firestore Timed Quiz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirestoreQuizSetupScreen(
  selectedSubject: MDCATSubject?,
  onSelectSubject: (MDCATSubject?) -> Unit,
  selectedCount: Int,
  onSelectCount: (Int) -> Unit,
  selectedPacingMode: QuizPacingMode,
  onSelectPacingMode: (QuizPacingMode) -> Unit,
  cloudSyncStatus: String,
  recentSessions: List<QuizSessionResult>,
  onStartQuiz: () -> Unit,
  onSeedQuestions: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Timed Quiz Simulator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Firebase Firestore Engine", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
          }
        },
        navigationIcon = {
          IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onSeedQuestions) {
            Icon(Icons.Default.CloudSync, contentDescription = "Sync Cloud Questions", tint = MaterialTheme.colorScheme.primary)
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Cloud Connection & Pacing Hero Banner
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          ),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "FIRESTORE CLOUD SYNC",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                  )
                }
              }

              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "PMC Target: 54s/MCQ",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "MDCAT Timed Speed & Accuracy Assessment",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Fetches authenticated MDCAT questions directly from Firestore. Real-time telemetry records your exact solving speed per question and saves performance metrics to the cloud.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Status: $cloudSyncStatus",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }

      // Step 1: Select Subject
      item {
        Text(
          text = "1. Select Subject",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        val subjects = listOf<MDCATSubject?>(null) + MDCATSubject.values().toList()
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 4.dp)
        ) {
          items(subjects) { subj ->
            val isSelected = selectedSubject == subj
            val label = subj?.displayName ?: "All Subjects (PMC Mix)"
            val badgeColor = subj?.color ?: MaterialTheme.colorScheme.primary

            FilterChip(
              selected = isSelected,
              onClick = { onSelectSubject(subj) },
              label = {
                Text(
                  text = label,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = badgeColor.copy(alpha = 0.2f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
              )
            )
          }
        }
      }

      // Step 2: Select Question Count
      item {
        Text(
          text = "2. Number of Questions",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          listOf(5, 10, 15, 20).forEach { count ->
            val isSelected = selectedCount == count
            OutlinedButton(
              onClick = { onSelectCount(count) },
              modifier = Modifier
                .weight(1f)
                .testTag("btn_count_$count"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
              ),
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              )
            ) {
              Text(
                text = "$count MCQs",
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Step 3: Select Pacing Mode
      item {
        Text(
          text = "3. Pacing Speed Mode",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          QuizPacingMode.values().forEach { mode ->
            val isSelected = selectedPacingMode == mode
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectPacingMode(mode) }
                .testTag("card_pacing_${mode.name}"),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
              ),
              border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = mode.title,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = if (mode == QuizPacingMode.PMC_STANDARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "${mode.secondsPerMcq}s/MCQ",
                        color = if (mode == QuizPacingMode.PMC_STANDARD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = mode.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                val totalTimeMin = (selectedCount * mode.secondsPerMcq) / 60
                val totalTimeSec = (selectedCount * mode.secondsPerMcq) % 60
                val timeLabel = if (totalTimeSec > 0) "${totalTimeMin}m ${totalTimeSec}s" else "${totalTimeMin}m"

                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }

      // Launch Hero Button
      item {
        val totalSec = selectedCount * selectedPacingMode.secondsPerMcq
        val mins = totalSec / 60
        val secs = totalSec % 60
        val totalDurationStr = if (secs > 0) "${mins}m ${secs}s" else "${mins} minutes"

        Button(
          onClick = onStartQuiz,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("btn_launch_firestore_quiz"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Icon(Icons.Default.ElectricBolt, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Start Timed Quiz ($selectedCount MCQs • $totalDurationStr)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }

      // Recent Firestore Sessions Speed & Accuracy History
      if (recentSessions.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Recent Firestore Sessions (Speed & Accuracy)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        items(recentSessions.take(4)) { session ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = session.testTitle,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = "Speed: ${String.format("%.1f", session.averageSpeedSecondsPerQuestion)}s/MCQ",
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${session.correctCount}/${session.totalQuestions} Correct",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${String.format("%.1f", session.accuracyPercent)}%",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 16.sp,
                  color = if (session.accuracyPercent >= 80f) BioGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                  text = if (session.isSyncedToFirestore) "☁️ Synced" else "Saved Local",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }
}

/**
 * Animated Loading State while fetching questions from Firestore.
 */
@Composable
fun FirestoreFetchingLoadingScreen(
  cloudSyncStatus: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(32.dp)
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(54.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 4.dp
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = "Connecting to Firebase Firestore...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Fetching authentic MDCAT questions and configuring completion speed telemetry.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = cloudSyncStatus,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
      }
    }
  }
}

/**
 * Active Timed Quiz Runner Interface with live speed tracker, HUD, and question palette.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFirestoreQuizInterface(
  questions: List<QuizQuestion>,
  currentIndex: Int,
  totalAllottedSeconds: Int,
  secondsRemaining: Int,
  isTimerPaused: Boolean,
  userAnswers: Map<Int, Int>,
  flaggedQuestions: Set<Int>,
  questionPacingSeconds: Map<Int, Int>,
  currentQuestionStartSec: Int,
  pacingMode: QuizPacingMode,
  onNavigateQuestion: (Int) -> Unit,
  onSelectOption: (Int) -> Unit,
  onClearOption: () -> Unit,
  onToggleFlag: () -> Unit,
  onPauseToggle: () -> Unit,
  onRequestSubmit: () -> Unit,
  onRequestExit: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (questions.isEmpty()) return
  val currentQuestion = questions.getOrNull(currentIndex) ?: questions[0]
  val isFlagged = flaggedQuestions.contains(currentIndex)
  val selectedOption = userAnswers[currentIndex]

  // Time formatting
  val minutes = secondsRemaining / 60
  val seconds = secondsRemaining % 60
  val timerString = String.format("%02d:%02d", minutes, seconds)
  val timeRatio = (secondsRemaining.toFloat() / totalAllottedSeconds.toFloat()).coerceIn(0f, 1f)

  // Current question live timer (seconds elapsed on this MCQ)
  val currentQuestionSec = (totalAllottedSeconds - secondsRemaining - currentQuestionStartSec).coerceAtLeast(0) +
      (questionPacingSeconds[currentIndex] ?: 0)

  // Live average speed calculated across answered questions
  val answeredCount = userAnswers.size
  val timeElapsedSoFar = totalAllottedSeconds - secondsRemaining
  val liveAvgSpeed = if (answeredCount > 0) timeElapsedSoFar.toFloat() / answeredCount.toFloat() else 0f

  val isOvertime = currentQuestionSec > pacingMode.secondsPerMcq

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          // Timer Badge with Color Warning
          Surface(
            color = when {
              secondsRemaining < 60 -> MaterialTheme.colorScheme.errorContainer
              secondsRemaining < 180 -> ChemOrange.copy(alpha = 0.2f)
              else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                tint = if (secondsRemaining < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = timerString,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (secondsRemaining < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onRequestExit) {
            Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
          }
        },
        actions = {
          IconButton(onClick = onPauseToggle) {
            Icon(
              if (isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = "Pause",
              tint = MaterialTheme.colorScheme.primary
            )
          }
          TextButton(
            onClick = onRequestSubmit,
            modifier = Modifier.testTag("btn_submit_active_quiz")
          ) {
            Text("Finish", fontWeight = FontWeight.Bold)
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      // Progress & Speed Tracking HUD
      LinearProgressIndicator(
        progress = { 1f - timeRatio },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(CircleShape),
        color = if (secondsRemaining < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Speed & Pacing Telemetry Strip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Current Question Solving Time
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Timer,
            contentDescription = null,
            tint = if (isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Q#${currentIndex + 1} Time: ${currentQuestionSec}s",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
          )
          if (isOvertime) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "OVERTIME (> ${pacingMode.secondsPerMcq}s)",
                color = MaterialTheme.colorScheme.error,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Session Average Speed Meter
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (answeredCount > 0) "Avg: ${String.format("%.1f", liveAvgSpeed)}s/MCQ" else "Target: ${pacingMode.secondsPerMcq}s",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Question Navigator Palette
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        questions.forEachIndexed { idx, _ ->
          val answered = userAnswers.containsKey(idx)
          val flagged = flaggedQuestions.contains(idx)
          val isCurrent = idx == currentIndex

          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(
                when {
                  isCurrent -> MaterialTheme.colorScheme.primary
                  flagged -> ChemOrange
                  answered -> MaterialTheme.colorScheme.primaryContainer
                  else -> MaterialTheme.colorScheme.surfaceVariant
                }
              )
              .clickable { onNavigateQuestion(idx) }
              .testTag("palette_q_$idx"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${idx + 1}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = when {
                isCurrent -> MaterialTheme.colorScheme.onPrimary
                flagged -> Color.White
                answered -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Question Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          item {
            // Subject & Topic Badge
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = currentQuestion.subject.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "${currentQuestion.subject.displayName} • ${currentQuestion.topic}",
                  color = currentQuestion.subject.color,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }

              IconButton(onClick = onToggleFlag) {
                Icon(
                  if (isFlagged) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                  contentDescription = "Flag Question",
                  tint = if (isFlagged) ChemOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Question ${currentIndex + 1} of ${questions.size}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = currentQuestion.questionText,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
          }

          // Options A, B, C, D
          itemsIndexed(currentQuestion.options) { optIdx, optionText ->
            val isSelected = selectedOption == optIdx
            val optLetter = ('A' + optIdx).toString()

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clickable { onSelectOption(optIdx) }
                .testTag("option_${currentIndex}_$optIdx"),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
              ),
              border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = optLetter,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                  text = optionText,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Control Bar (Previous, Clear, Next)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { onNavigateQuestion(currentIndex - 1) },
          enabled = currentIndex > 0,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Previous")
        }

        if (selectedOption != null) {
          TextButton(onClick = onClearOption) {
            Text("Clear Selection", fontSize = 12.sp)
          }
        }

        if (currentIndex < questions.size - 1) {
          Button(
            onClick = { onNavigateQuestion(currentIndex + 1) },
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Next MCQ")
          }
        } else {
          Button(
            onClick = onRequestSubmit,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text("Submit Test", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Post-Session Breakdown Screen:
 * Displays accuracy metrics, completion speed breakdown (average seconds/MCQ),
 * fastest/slowest solved MCQs, comparison to PMC target, and full question explanations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirestoreSessionSummaryScreen(
  session: QuizSessionResult,
  questions: List<QuizQuestion>,
  onRetake: () -> Unit,
  onNavigateToAnalytics: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showExplanations by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Session Telemetry & Analytics", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        navigationIcon = {
          IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // Overall Score & Accuracy Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          ),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "SAVED TO FIRESTORE",
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "${String.format("%.1f", session.accuracyPercent)}%",
              fontSize = 42.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )

            Text(
              text = "${session.correctCount} of ${session.totalQuestions} Questions Correct",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
              color = if (session.accuracyPercent >= 80f) BioGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = session.accuracyGrade,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (session.accuracyPercent >= 80f) BioGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // Completion Speed & Pacing Telemetry Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Completion Speed Breakdown",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }

              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = session.speedPacingGrade,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Grid Telemetry Stats
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Average Speed Stat
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Average Speed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "${String.format("%.1f", session.averageSpeedSecondsPerQuestion)}s",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text("per question", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }

              // Total Time Taken Stat
              val timeTakenMin = session.timeTakenSeconds / 60
              val timeTakenSec = session.timeTakenSeconds % 60
              val totalAllottedMin = session.totalAllottedSeconds / 60
              val totalAllottedSec = session.totalAllottedSeconds % 60

              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Time Elapsed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    String.format("%02d:%02d", timeTakenMin, timeTakenSec),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text("allotted ${String.format("%02d:%02d", totalAllottedMin, totalAllottedSec)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Fastest MCQ
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Fastest MCQ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "${session.fastestQuestionSeconds}s",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioGreen
                  )
                  Text("quickest solution", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }

              // Slowest MCQ
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Slowest MCQ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "${session.slowestQuestionSeconds}s",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.slowestQuestionSeconds > 54) ChemOrange else MaterialTheme.colorScheme.onSurface
                  )
                  Text(if (session.slowestQuestionSeconds > 54) ">54s overtime" else "within target", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Comparison to Official PMC Benchmark
            val pmcTarget = 54f
            val speedDiffPercent = ((pmcTarget - session.averageSpeedSecondsPerQuestion) / pmcTarget) * 100f

            Surface(
              color = if (speedDiffPercent >= 0) BioGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  if (speedDiffPercent >= 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                  contentDescription = null,
                  tint = if (speedDiffPercent >= 0) BioGreen else MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (speedDiffPercent >= 0) {
                    "${String.format("%.1f", speedDiffPercent)}% faster than the official PMC 54s/MCQ limit!"
                  } else {
                    "${String.format("%.1f", -speedDiffPercent)}% slower than PMC 54s target. Practice pacing drills."
                  },
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (speedDiffPercent >= 0) BioGreen else MaterialTheme.colorScheme.error
                )
              }
            }
          }
        }
      }

      // Per-Question Speed & Accuracy Timeline Breakdown
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Per-Question Pacing Log",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Time spent per question vs 54-second PMC target:",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            questions.forEachIndexed { idx, q ->
              val timeSec = session.perQuestionPacing[idx] ?: 0
              val isCorrect = session.perQuestionCorrect[idx] ?: false
              val maxRefSec = 80f
              val ratio = (timeSec.toFloat() / maxRefSec).coerceIn(0.05f, 1f)

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Correct/Wrong Icon
                Icon(
                  if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                  contentDescription = null,
                  tint = if (isCorrect) BioGreen else MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))

                Text(
                  text = "Q${idx + 1}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.width(28.dp)
                )

                // Visual bar
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth(ratio)
                      .height(16.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(
                        when {
                          timeSec <= 35 -> BioGreen
                          timeSec <= 54 -> MaterialTheme.colorScheme.primary
                          else -> ChemOrange
                        }
                      )
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                  text = "${timeSec}s",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (timeSec > 54) ChemOrange else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.width(36.dp),
                  textAlign = TextAlign.End
                )
              }
            }
          }
        }
      }

      // Toggle Detailed Question Explanations
      item {
        OutlinedButton(
          onClick = { showExplanations = !showExplanations },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (showExplanations) "Hide Question Explanations" else "Review Question Explanations & Tips")
        }
      }

      if (showExplanations) {
        itemsIndexed(questions) { idx, q ->
          val userAns = session.userAnswers[idx]
          val isCorrect = userAns == q.correctOptionIndex

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isCorrect) BioGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(
              width = 1.dp,
              color = if (isCorrect) BioGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Question ${idx + 1} • ${q.subject.displayName}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (isCorrect) BioGreen else MaterialTheme.colorScheme.error
                )

                Text(
                  text = if (isCorrect) "✓ Correct" else "✗ Incorrect",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (isCorrect) BioGreen else MaterialTheme.colorScheme.error
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(text = q.questionText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Your Answer: ${userAns?.let { ('A' + it) + ". " + q.options[it] } ?: "Not Answered"}",
                fontSize = 12.sp,
                color = if (isCorrect) BioGreen else MaterialTheme.colorScheme.error
              )
              if (!isCorrect) {
                Text(
                  text = "Correct Answer: ${('A' + q.correctOptionIndex)}. ${q.options[q.correctOptionIndex]}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = BioGreen
                )
              }

              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Explanation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                  Text(text = q.explanation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  if (q.highYieldTip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💡 High-Yield Tip: ${q.highYieldTip}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ChemOrange)
                  }
                }
              }
            }
          }
        }
      }

      // Actions: Retake, Analytics, Exit
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Take Another Firestore Quiz", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onNavigateToAnalytics,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Assessment, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("View in Performance Dashboard")
          }

          TextButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Return to Practice Tests Catalog")
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
