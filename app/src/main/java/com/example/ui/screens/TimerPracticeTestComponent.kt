package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import com.example.data.model.TestResult
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.theme.LogicPink
import com.example.ui.theme.PhysicsPurple
import com.example.ui.viewmodel.MDCATViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full timer-based practice test component that:
 * 1. Fetches quiz questions from the local Room database
 * 2. Manages real-time countdown timer with pause/resume and time warning states
 * 3. Tracks comprehensive state for user progress (answers, flagged questions, completion rate)
 * 4. Calculates final scores, accuracy, weak topics, and persists results into Room database
 * 5. Provides comprehensive answer review mode and AI remediation triggers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerPracticeTestComponent(
  viewModel: MDCATViewModel,
  testTitle: String = "MDCAT Rapid Assessment Quiz",
  subjectFilter: MDCATSubject? = null,
  durationMinutes: Int = 10,
  onExitTest: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Fetch questions from Room database via ViewModel / DataRepository Flow
  val allQuestionsFromRoom by viewModel.allQuizQuestions.collectAsState()
  val filteredQuestions = remember(allQuestionsFromRoom, subjectFilter) {
    if (subjectFilter != null) {
      allQuestionsFromRoom.filter { it.subject == subjectFilter }
    } else {
      allQuestionsFromRoom
    }.ifEmpty { allQuestionsFromRoom }
  }

  // Timer & Test Execution States
  val totalTestSeconds = remember(durationMinutes) { durationMinutes * 60 }
  var secondsRemaining by remember(totalTestSeconds) { mutableIntStateOf(totalTestSeconds) }
  var isTimerPaused by remember { mutableStateOf(false) }
  var isTestCompleted by remember { mutableStateOf(false) }
  var showExitConfirmDialog by remember { mutableStateOf(false) }
  var showSubmitConfirmDialog by remember { mutableStateOf(false) }

  // User Progress States
  var currentQuestionIndex by remember { mutableIntStateOf(0) }
  val userSelectedAnswers = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> optionIndex
  val flaggedQuestions = remember { mutableStateOf(setOf<Int>()) } // question indices flagged for review
  val visitedQuestions = remember { mutableStateOf(setOf(0)) }

  // Final Calculated Score State
  var finalCalculatedResult by remember { mutableStateOf<CalculatedQuizScore?>(null) }
  var isReviewModeActive by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  // Countdown timer coroutine loop
  LaunchedEffect(isTimerPaused, isTestCompleted) {
    if (!isTimerPaused && !isTestCompleted) {
      while (secondsRemaining > 0 && !isTestCompleted) {
        delay(1000L)
        if (!isTimerPaused) {
          secondsRemaining -= 1
        }
      }
      if (secondsRemaining <= 0 && !isTestCompleted) {
        // Auto-submit test upon timer expiry
        finalCalculatedResult = calculateFinalScore(
          testTitle = "$testTitle (Timed Out)",
          questions = filteredQuestions,
          userAnswers = userSelectedAnswers,
          timeTakenSeconds = totalTestSeconds - secondsRemaining,
          subjectFilter = subjectFilter
        )
        isTestCompleted = true
        // Persist to Room database
        viewModel.recordCustomTestResult(finalCalculatedResult!!.toTestResult())
      }
    }
  }

  // Update visited questions set
  LaunchedEffect(currentQuestionIndex) {
    visitedQuestions.value = visitedQuestions.value + currentQuestionIndex
  }

  // Helper submit action
  val submitCurrentTest = {
    val result = calculateFinalScore(
      testTitle = testTitle,
      questions = filteredQuestions,
      userAnswers = userSelectedAnswers,
      timeTakenSeconds = (totalTestSeconds - secondsRemaining).coerceAtLeast(1),
      subjectFilter = subjectFilter
    )
    finalCalculatedResult = result
    isTestCompleted = true
    viewModel.recordCustomTestResult(result.toTestResult())
  }

  if (isTestCompleted && finalCalculatedResult != null) {
    // Show Final Score Calculation & Review Screen
    TestScoreAndReviewScreen(
      result = finalCalculatedResult!!,
      questions = filteredQuestions,
      userAnswers = userSelectedAnswers,
      isReviewMode = isReviewModeActive,
      onToggleReviewMode = { isReviewModeActive = !isReviewModeActive },
      onRetakeTest = {
        secondsRemaining = totalTestSeconds
        currentQuestionIndex = 0
        userSelectedAnswers.clear()
        flaggedQuestions.value = emptySet()
        visitedQuestions.value = setOf(0)
        isTestCompleted = false
        isReviewModeActive = false
        finalCalculatedResult = null
      },
      onGenerateAIPlan = {
        viewModel.generateAIStudyPlan(finalCalculatedResult!!.weakTopics)
        onExitTest()
        viewModel.setTab(com.example.ui.viewmodel.MainNavTab.ANALYTICS)
      },
      onExit = onExitTest,
      modifier = modifier
    )
    return
  }

  // Ongoing Timer-Based Test Screen
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = testTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
            Text(
              text = "Fetched from Local Room DB • ${filteredQuestions.size} MCQs",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = { showExitConfirmDialog = true },
            modifier = Modifier.testTag("btn_exit_timer_test")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Exit Test")
          }
        },
        actions = {
          // Pause / Resume Toggle
          IconButton(
            onClick = { isTimerPaused = !isTimerPaused },
            modifier = Modifier.testTag("btn_pause_resume_timer")
          ) {
            Icon(
              if (isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = if (isTimerPaused) "Resume Timer" else "Pause Timer",
              tint = if (isTimerPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      // Bottom Navigation Controls
      Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              if (currentQuestionIndex > 0) {
                currentQuestionIndex -= 1
              }
            },
            enabled = currentQuestionIndex > 0,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("btn_prev_question")
          ) {
            Text("Previous")
          }

          // Clear Selected Option Button
          if (userSelectedAnswers.containsKey(currentQuestionIndex)) {
            TextButton(
              onClick = { userSelectedAnswers.remove(currentQuestionIndex) },
              modifier = Modifier.testTag("btn_clear_selection")
            ) {
              Text("Clear Choice", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
          }

          if (currentQuestionIndex < filteredQuestions.size - 1) {
            Button(
              onClick = { currentQuestionIndex += 1 },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("btn_next_question")
            ) {
              Text("Next")
            }
          } else {
            Button(
              onClick = { showSubmitConfirmDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.testTag("btn_finish_submit_test")
            ) {
              Text("Finish & Submit", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  ) { paddingValues ->
    if (filteredQuestions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Text("No questions found in local Room database.", style = MaterialTheme.typography.bodyLarge)
      }
      return@Scaffold
    }

    val currentQuestion = filteredQuestions.getOrNull(currentQuestionIndex) ?: filteredQuestions[0]
    val selectedOptionIndex = userSelectedAnswers[currentQuestionIndex]
    val isFlagged = flaggedQuestions.value.contains(currentQuestionIndex)
    val isLowTime = secondsRemaining < 120 && secondsRemaining > 0

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)

    val timerBadgeColor by animateColorAsState(
      targetValue = if (isLowTime) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
      label = "TimerColor"
    )
    val timerTextColor by animateColorAsState(
      targetValue = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
      label = "TimerTextColor"
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Header Banner: Timer Display + Answered Progress Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Real-time Countdown Timer Badge
        Surface(
          color = timerBadgeColor,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("timer_badge")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Alarm,
              contentDescription = null,
              tint = timerTextColor,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isTimerPaused) "PAUSED ($timerString)" else timerString,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = timerTextColor
            )
          }
        }

        // Answered Progress Count
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "${userSelectedAnswers.size} of ${filteredQuestions.size} Solved",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }

        // Quick Submit Action
        OutlinedButton(
          onClick = { showSubmitConfirmDialog = true },
          shape = RoundedCornerShape(10.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.testTag("btn_quick_submit")
        ) {
          Text("Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Linear Progress Indicator
      val progress = if (filteredQuestions.isNotEmpty()) {
        userSelectedAnswers.size.toFloat() / filteredQuestions.size.toFloat()
      } else 0f

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Interactive Question Palette (Chips)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        filteredQuestions.forEachIndexed { index, _ ->
          val isCurrent = index == currentQuestionIndex
          val isAnswered = userSelectedAnswers.containsKey(index)
          val isBookmarked = flaggedQuestions.value.contains(index)

          val chipColor = when {
            isCurrent -> MaterialTheme.colorScheme.primary
            isBookmarked -> MaterialTheme.colorScheme.tertiary
            isAnswered -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
          }

          val chipTextColor = when {
            isCurrent -> MaterialTheme.colorScheme.onPrimary
            isBookmarked -> MaterialTheme.colorScheme.onTertiary
            isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }

          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(chipColor)
              .border(
                width = if (isCurrent) 2.dp else 0.5.dp,
                color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
              )
              .clickable { currentQuestionIndex = index }
              .testTag("question_palette_chip_$index"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${index + 1}",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = chipTextColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Question Content Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
        ) {
          // Question Header: Subject Badge & Bookmark Toggle
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

            // Flag for review button
            IconButton(
              onClick = {
                val current = flaggedQuestions.value
                flaggedQuestions.value = if (current.contains(currentQuestionIndex)) {
                  current - currentQuestionIndex
                } else {
                  current + currentQuestionIndex
                }
              },
              modifier = Modifier.testTag("btn_flag_question")
            ) {
              Icon(
                if (isFlagged) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Flag for Review",
                tint = if (isFlagged) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Question ${currentQuestionIndex + 1} of ${filteredQuestions.size}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Question Prompt
          Text(
            text = currentQuestion.questionText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Multiple Choice Options List
          currentQuestion.options.forEachIndexed { optIndex, optionText ->
            val isSelected = selectedOptionIndex == optIndex

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clickable {
                  userSelectedAnswers[currentQuestionIndex] = optIndex
                }
                .testTag("quiz_option_${currentQuestionIndex}_$optIndex"),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
              ),
              border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary)
              } else null
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = ('A' + optIndex).toString(),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                  text = optionText,
                  fontSize = 14.sp,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                  lineHeight = 20.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }

  // Exit Confirmation Dialog
  if (showExitConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showExitConfirmDialog = false },
      title = { Text("Exit Practice Test?") },
      text = {
        Text("Your timer and progress will not be recorded unless submitted. Are you sure you want to exit?")
      },
      confirmButton = {
        Button(
          onClick = {
            showExitConfirmDialog = false
            onExitTest()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Discard & Exit")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showExitConfirmDialog = false }) {
          Text("Resume Test")
        }
      }
    )
  }

  // Submit Confirmation Dialog
  if (showSubmitConfirmDialog) {
    val unattemptedCount = filteredQuestions.size - userSelectedAnswers.size
    AlertDialog(
      onDismissRequest = { showSubmitConfirmDialog = false },
      title = { Text("Submit Practice Test?") },
      text = {
        Column {
          Text("Are you ready to calculate your final score?")
          Spacer(modifier = Modifier.height(8.dp))
          Text("• Total Questions: ${filteredQuestions.size}")
          Text("• Solved: ${userSelectedAnswers.size}")
          if (unattemptedCount > 0) {
            Text("• Unattempted: $unattemptedCount", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
          }
          if (flaggedQuestions.value.isNotEmpty()) {
            Text("• Flagged for Review: ${flaggedQuestions.value.size}", color = MaterialTheme.colorScheme.tertiary)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showSubmitConfirmDialog = false
            submitCurrentTest()
          },
          modifier = Modifier.testTag("btn_confirm_final_submit")
        ) {
          Text("Submit Now")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showSubmitConfirmDialog = false }) {
          Text("Keep Solving")
        }
      }
    )
  }
}

/**
 * Calculated Quiz Score Data Model for detailed performance breakdown
 */
data class CalculatedQuizScore(
  val testTitle: String,
  val scorePercentage: Int,
  val totalQuestions: Int,
  val correctCount: Int,
  val wrongCount: Int,
  val unattemptedCount: Int,
  val timeTakenSeconds: Int,
  val accuracyRate: Int,
  val weakTopics: List<String>,
  val performanceTier: String,
  val subjectFilter: MDCATSubject?
) {
  fun toTestResult(): TestResult {
    return TestResult(
      id = "test_${System.currentTimeMillis()}",
      testTitle = testTitle,
      subject = subjectFilter,
      score = scorePercentage,
      totalQuestions = totalQuestions,
      correctCount = correctCount,
      wrongCount = wrongCount,
      timeTakenSeconds = timeTakenSeconds,
      dateMillis = System.currentTimeMillis(),
      weakTopics = weakTopics
    )
  }
}

/**
 * Calculates score, accuracy, and detects weak topics from answers
 */
private fun calculateFinalScore(
  testTitle: String,
  questions: List<QuizQuestion>,
  userAnswers: Map<Int, Int>,
  timeTakenSeconds: Int,
  subjectFilter: MDCATSubject?
): CalculatedQuizScore {
  var correct = 0
  var wrong = 0
  var unattempted = 0
  val weakTopicsSet = mutableSetOf<String>()

  questions.forEachIndexed { index, q ->
    val userOption = userAnswers[index]
    if (userOption == null) {
      unattempted++
      weakTopicsSet.add("${q.subject.displayName}: ${q.topic}")
    } else if (userOption == q.correctOptionIndex) {
      correct++
    } else {
      wrong++
      weakTopicsSet.add("${q.subject.displayName}: ${q.topic}")
    }
  }

  val total = questions.size
  val scorePercent = if (total > 0) (correct * 100) / total else 0
  val attempted = correct + wrong
  val accuracy = if (attempted > 0) (correct * 100) / attempted else 0

  val performanceTier = when {
    scorePercent >= 90 -> "Elite Merit Tier (Govt Medical College Qualified)"
    scorePercent >= 75 -> "High Competitive Tier (Public Sector Zone)"
    scorePercent >= 60 -> "Qualified Tier (Private / Foundation Range)"
    else -> "Needs Revision (Targeted Remediation Required)"
  }

  return CalculatedQuizScore(
    testTitle = testTitle,
    scorePercentage = scorePercent,
    totalQuestions = total,
    correctCount = correct,
    wrongCount = wrong,
    unattemptedCount = unattempted,
    timeTakenSeconds = timeTakenSeconds,
    accuracyRate = accuracy,
    weakTopics = weakTopicsSet.toList(),
    performanceTier = performanceTier,
    subjectFilter = subjectFilter
  )
}

/**
 * Final Score & Review Screen showing score metrics, weak areas, and question-by-question explanations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScoreAndReviewScreen(
  result: CalculatedQuizScore,
  questions: List<QuizQuestion>,
  userAnswers: Map<Int, Int>,
  isReviewMode: Boolean,
  onToggleReviewMode: () -> Unit,
  onRetakeTest: () -> Unit,
  onGenerateAIPlan: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier
) {
  val minutesTaken = result.timeTakenSeconds / 60
  val secondsTaken = result.timeTakenSeconds % 60
  val timeString = String.format("%02d:%02d", minutesTaken, secondsTaken)

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = if (isReviewMode) "Review Solutions" else "Score Report",
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit to Tests")
          }
        },
        actions = {
          TextButton(onClick = onToggleReviewMode) {
            Text(
              text = if (isReviewMode) "Summary" else "Review",
              fontWeight = FontWeight.Bold
            )
          }
        }
      )
    }
  ) { padding ->
    if (isReviewMode) {
      // Question-by-Question Solution Breakdown
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(4.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Detailed Solutions & Explanations",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                  text = "Green: Correct Option • Red: Your Selection",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
              OutlinedButton(
                onClick = onToggleReviewMode,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Back to Score")
              }
            }
          }
        }

        itemsIndexed(questions) { qIndex, question ->
          val userSelected = userAnswers[qIndex]
          val isCorrect = userSelected == question.correctOptionIndex
          val isUnattempted = userSelected == null

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              when {
                isCorrect -> BioGreen
                isUnattempted -> MaterialTheme.colorScheme.outlineVariant
                else -> MaterialTheme.colorScheme.error
              }
            )
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  color = question.subject.color.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "${question.subject.displayName} • Q${qIndex + 1}",
                    color = question.subject.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }

                Surface(
                  color = when {
                    isCorrect -> BioGreen.copy(alpha = 0.15f)
                    isUnattempted -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.errorContainer
                  },
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = when {
                      isCorrect -> "CORRECT (+1)"
                      isUnattempted -> "UNATTEMPTED"
                      else -> "INCORRECT"
                    },
                    color = when {
                      isCorrect -> BioGreen
                      isUnattempted -> MaterialTheme.colorScheme.onSurfaceVariant
                      else -> MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = question.questionText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Options
              question.options.forEachIndexed { optIndex, optText ->
                val isOptCorrect = optIndex == question.correctOptionIndex
                val isUserOpt = userSelected == optIndex

                val containerColor = when {
                  isOptCorrect -> BioGreen.copy(alpha = 0.15f)
                  isUserOpt -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                  else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }

                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  color = containerColor,
                  shape = RoundedCornerShape(8.dp),
                  border = if (isOptCorrect || isUserOpt) {
                    androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isOptCorrect) BioGreen else MaterialTheme.colorScheme.error
                    )
                  } else null
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "${('A' + optIndex)}. ",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = when {
                        isOptCorrect -> BioGreen
                        isUserOpt -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      }
                    )
                    Text(
                      text = optText,
                      fontSize = 13.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Explanation Box
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "High-Yield Explanation:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = question.explanation,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  if (question.highYieldTip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "💡 Doctor's Tip: ${question.highYieldTip}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = ChemOrange
                    )
                  }
                }
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    } else {
      // Primary Score Report & Diagnostic Metrics
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Hero Score Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "${result.scorePercentage}%",
              fontSize = 44.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
              text = result.performanceTier,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Matrix Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              ScoreMetricItem(label = "Correct", value = "${result.correctCount}", color = BioGreen)
              ScoreMetricItem(label = "Incorrect", value = "${result.wrongCount}", color = LogicPink)
              ScoreMetricItem(label = "Skipped", value = "${result.unattemptedCount}", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
              ScoreMetricItem(label = "Time", value = timeString, color = MaterialTheme.colorScheme.onPrimaryContainer)
              ScoreMetricItem(label = "Accuracy", value = "${result.accuracyRate}%", color = ChemOrange)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weak Topics Detected Box
        if (result.weakTopics.isNotEmpty()) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ChemOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Weak Areas Detected (Saved to Room DB)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              result.weakTopics.take(5).forEach { topic ->
                Row(
                  modifier = Modifier.padding(vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(ChemOrange)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = topic, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Button(
                onClick = onGenerateAIPlan,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_build_ai_remedial_plan")
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate AI Remedial Plan")
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row: Review Answers & Retake Test
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onToggleReviewMode,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_review_solutions")
          ) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Review Answers")
          }

          Button(
            onClick = onRetakeTest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("btn_retake_test")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retake Test")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
          onClick = onExit,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Return to Practice Tests Catalog")
        }
      }
    }
  }
}

@Composable
private fun ScoreMetricItem(
  label: String,
  value: String,
  color: Color
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 18.sp,
      color = color
    )
    Text(
      text = label,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    )
  }
}
