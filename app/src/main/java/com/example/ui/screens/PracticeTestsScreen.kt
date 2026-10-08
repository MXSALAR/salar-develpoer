package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.MDCATSubject
import com.example.data.model.TestResult
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.theme.LogicPink
import com.example.ui.theme.PhysicsPurple
import com.example.ui.viewmodel.MDCATViewModel
import com.example.ui.viewmodel.MainNavTab

data class TimerTestLaunchConfig(
  val title: String,
  val subject: MDCATSubject?,
  val durationMinutes: Int
)

@Composable
fun PracticeTestsScreen(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  val isQuizActive by viewModel.isQuizActive.collectAsState()
  val showResultDialog by viewModel.showQuizResultDialog.collectAsState()
  val latestResult by viewModel.latestTestResult.collectAsState()

  // State for Timer-Based Practice Test Component (Room DB powered)
  var customTimerTestConfig by remember { mutableStateOf<TimerTestLaunchConfig?>(null) }
  val activeConfig = customTimerTestConfig

  // State for Firestore Timed Quiz Simulator (Speed & Accuracy tracking)
  var showFirestoreTimedQuiz by remember { mutableStateOf(false) }

  if (showFirestoreTimedQuiz) {
    FirestoreTimedQuizScreen(
      viewModel = viewModel,
      onExit = { showFirestoreTimedQuiz = false },
      onNavigateToAnalytics = {
        showFirestoreTimedQuiz = false
        viewModel.setTab(MainNavTab.ANALYTICS)
      },
      modifier = modifier
    )
  } else if (activeConfig != null) {
    // Show the newly implemented TimerPracticeTestComponent
    TimerPracticeTestComponent(
      viewModel = viewModel,
      testTitle = activeConfig.title,
      subjectFilter = activeConfig.subject,
      durationMinutes = activeConfig.durationMinutes,
      onExitTest = { customTimerTestConfig = null },
      modifier = modifier
    )
  } else if (isQuizActive) {
    // Show active timed quiz interface
    ActiveQuizRunner(viewModel = viewModel, modifier = modifier)
  } else {
    // Show tests & quizzes catalog
    TestsCatalogView(
      viewModel = viewModel,
      onLaunchTimerTest = { config -> customTimerTestConfig = config },
      onLaunchFirestoreTimedQuiz = { showFirestoreTimedQuiz = true },
      modifier = modifier
    )
  }

  // Diagnostic Results Modal
  if (showResultDialog && latestResult != null) {
    QuizResultDialog(
      result = latestResult!!,
      onDismiss = { viewModel.dismissQuizResult() },
      onGenerateAIPlan = {
        viewModel.generateAIStudyPlan(latestResult!!.weakTopics)
        viewModel.dismissQuizResult()
        viewModel.setTab(MainNavTab.ANALYTICS)
      }
    )
  }
}

@Composable
fun TestsCatalogView(
  viewModel: MDCATViewModel,
  onLaunchTimerTest: (TimerTestLaunchConfig) -> Unit = {},
  onLaunchFirestoreTimedQuiz: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      // Featured Firestore Timed Quiz Simulator (Speed & Accuracy Tracking)
      FirestoreTimedQuizBanner(
        onLaunch = onLaunchFirestoreTimedQuiz
      )
    }

    item {
      // Featured Room DB Timer-Based Practice Test Card
      RoomDatabaseTimerTestBanner(
        onLaunchTest = { minutes, subject, title ->
          onLaunchTimerTest(TimerTestLaunchConfig(title, subject, minutes))
        }
      )
    }

    item {
      // Full Length Grand Mock Exam Card
      GrandMockCard(
        onStart = { viewModel.startQuiz(subject = null, isFullMock = true) }
      )
    }

    item {
      Text(
        text = "Subject-Wise Interactive Quizzes",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    item {
      SubjectQuizCard(
        subject = MDCATSubject.BIOLOGY,
        totalMcqs = 81,
        durationMinutes = 60,
        testTag = "btn_start_bio_quiz",
        onStart = { viewModel.startQuiz(MDCATSubject.BIOLOGY) }
      )
    }

    item {
      SubjectQuizCard(
        subject = MDCATSubject.CHEMISTRY,
        totalMcqs = 45,
        durationMinutes = 40,
        testTag = "btn_start_chem_quiz",
        onStart = { viewModel.startQuiz(MDCATSubject.CHEMISTRY) }
      )
    }

    item {
      SubjectQuizCard(
        subject = MDCATSubject.PHYSICS,
        totalMcqs = 36,
        durationMinutes = 35,
        testTag = "btn_start_phys_quiz",
        onStart = { viewModel.startQuiz(MDCATSubject.PHYSICS) }
      )
    }

    item {
      SubjectQuizCard(
        subject = MDCATSubject.ENGLISH,
        totalMcqs = 9,
        durationMinutes = 10,
        testTag = "btn_start_eng_quiz",
        onStart = { viewModel.startQuiz(MDCATSubject.ENGLISH) }
      )
    }

    item {
      SubjectQuizCard(
        subject = MDCATSubject.LOGICAL_REASONING,
        totalMcqs = 9,
        durationMinutes = 10,
        testTag = "btn_start_logic_quiz",
        onStart = { viewModel.startQuiz(MDCATSubject.LOGICAL_REASONING) }
      )
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun FirestoreTimedQuizBanner(
  onLaunch: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_firestore_quiz_banner"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.5.dp,
      MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    )
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
              Icons.Default.CloudSync,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "FIRESTORE TIMED ENGINE",
              color = MaterialTheme.colorScheme.onPrimary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "Speed & Accuracy Telemetry",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "MDCAT Cloud Timed Quiz Simulator",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "Fetches MDCAT questions directly from Firestore and tracks your completion speed (seconds per MCQ) vs the official PMC 54s benchmark, with per-question accuracy analytics.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "⚡ Pacing: 54s / MCQ",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "☁️ Cloud Synced",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BioGreen,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Button(
          onClick = onLaunch,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("btn_launch_firestore_timed_quiz")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Launch Simulator", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun RoomDatabaseTimerTestBanner(
  onLaunchTest: (durationMinutes: Int, subject: MDCATSubject?, title: String) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_room_timer_test_banner"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
              Icons.Default.Alarm,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "ROOM DB TIMED ENGINE",
              color = MaterialTheme.colorScheme.onPrimary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          }
        }

        Text(
          text = "Local Persistence",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Timed MCQ Practice & Score Engine",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "Fetches questions directly from Room SQLite database. Features countdown timer with pause/resume, interactive question palette, and detailed score breakdown with weak topic diagnosis.",
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )

      // Quick Test Selector Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = false,
          onClick = {
            onLaunchTest(10, null, "MDCAT 10-Min Speed Drill")
          },
          label = { Text("⚡ 10-Min Drill", fontSize = 12.sp) }
        )

        FilterChip(
          selected = false,
          onClick = {
            onLaunchTest(15, MDCATSubject.BIOLOGY, "Biology Diagnostic Sprint")
          },
          label = { Text("🧬 15-Min Biology", fontSize = 12.sp) }
        )

        FilterChip(
          selected = false,
          onClick = {
            onLaunchTest(15, MDCATSubject.CHEMISTRY, "Chemistry Core Sprint")
          },
          label = { Text("🧪 15-Min Chemistry", fontSize = 12.sp) }
        )

        FilterChip(
          selected = false,
          onClick = {
            onLaunchTest(20, MDCATSubject.PHYSICS, "Physics Numerical Benchmark")
          },
          label = { Text("⚛️ 20-Min Physics", fontSize = 12.sp) }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = {
          onLaunchTest(10, null, "MDCAT Grand Diagnostic Drill")
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_launch_timed_practice_test")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Start Timed Practice Test", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun GrandMockCard(onStart: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_grand_mock"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "OFFICIAL MDCAT SIMULATOR",
          color = MaterialTheme.colorScheme.onPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Full-Length Grand Mock Test",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )

      Text(
        text = "180 MCQs • 3.5 Hours • Exact PMC Pattern: Bio (81), Chem (45), Phy (36), English (9), Logical Reasoning (9).",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
      )

      Button(
        onClick = onStart,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("btn_start_grand_mock")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Begin Full Mock Exam", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun SubjectQuizCard(
  subject: MDCATSubject,
  totalMcqs: Int,
  durationMinutes: Int,
  testTag: String,
  onStart: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onStart)
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(subject.color.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = subject.displayName.take(2).uppercase(),
            color = subject.color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = subject.displayName,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          Text(
            text = "$totalMcqs MCQs • $durationMinutes Minutes • Instant Explanations",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Icon(
        Icons.Default.PlayArrow,
        contentDescription = "Start",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
fun ActiveQuizRunner(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  val questions by viewModel.quizQuestions.collectAsState()
  val currentIndex by viewModel.currentQuestionIndex.collectAsState()
  val userAnswers by viewModel.userQuizAnswers.collectAsState()
  val markedForReview by viewModel.markedForReview.collectAsState()
  val secondsRemaining by viewModel.quizSecondsRemaining.collectAsState()

  if (questions.isEmpty()) return

  val currentQuestion = questions.getOrNull(currentIndex) ?: questions[0]
  val selectedAnswer = userAnswers[currentIndex]
  val isMarked = markedForReview.contains(currentIndex)

  val minutes = secondsRemaining / 60
  val seconds = secondsRemaining % 60
  val timerString = String.format("%02d:%02d", minutes, seconds)

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    // Top Header: Timer + Submit button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        color = if (secondsRemaining < 120) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.Alarm,
            contentDescription = null,
            tint = if (secondsRemaining < 120) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = timerString,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (secondsRemaining < 120) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Button(
        onClick = { viewModel.submitQuiz() },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("btn_submit_quiz")
      ) {
        Text("Submit Test", fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Horizontal Question Palette
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      questions.forEachIndexed { idx, _ ->
        val answered = userAnswers.containsKey(idx)
        val marked = markedForReview.contains(idx)
        val isCurrent = idx == currentIndex

        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
              when {
                isCurrent -> MaterialTheme.colorScheme.primary
                marked -> MaterialTheme.colorScheme.tertiary
                answered -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
            )
            .clickable { viewModel.navigateQuizQuestion(idx) },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${idx + 1}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = when {
              isCurrent -> MaterialTheme.colorScheme.onPrimary
              marked -> MaterialTheme.colorScheme.onTertiary
              answered -> MaterialTheme.colorScheme.onPrimaryContainer
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Question Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = currentQuestion.subject.color.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "${currentQuestion.subject.displayName} • ${currentQuestion.topic}",
                color = currentQuestion.subject.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }

            IconButton(onClick = { viewModel.toggleMarkForReview(currentIndex) }) {
              Icon(
                if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Mark for Review",
                tint = if (isMarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

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

        // Options
        items(currentQuestion.options.indices.toList()) { optIdx ->
          val optionText = currentQuestion.options[optIdx]
          val isSelected = selectedAnswer == optIdx

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .clickable { viewModel.selectQuizAnswer(currentIndex, optIdx) }
              .testTag("option_${currentIndex}_$optIdx"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = ('A' + optIdx).toString(),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Text(
                text = optionText,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Bottom Navigation Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      OutlinedButton(
        onClick = { viewModel.navigateQuizQuestion(currentIndex - 1) },
        enabled = currentIndex > 0,
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Previous")
      }

      Button(
        onClick = {
          if (currentIndex < questions.size - 1) {
            viewModel.navigateQuizQuestion(currentIndex + 1)
          } else {
            viewModel.submitQuiz()
          }
        },
        shape = RoundedCornerShape(10.dp)
      ) {
        Text(if (currentIndex < questions.size - 1) "Next Question" else "Finish & Submit")
      }
    }
  }
}

@Composable
fun QuizResultDialog(
  result: TestResult,
  onDismiss: () -> Unit,
  onGenerateAIPlan: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Diagnostic Test Report", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = result.testTitle,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Score Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${result.score}%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
              Text("Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${result.correctCount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BioGreen)
              Text("Correct", fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${result.wrongCount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = LogicPink)
              Text("Review", fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Weak Areas Identified
        if (result.weakTopics.isNotEmpty()) {
          Text(
            text = "Weak Areas Detected by AI Engine:",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          result.weakTopics.take(3).forEach { topic ->
            Row(
              modifier = Modifier.padding(vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = ChemOrange, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = topic, fontSize = 12.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onGenerateAIPlan,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("btn_ai_plan_from_result")
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Build AI Study Plan", fontSize = 12.sp)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Done")
      }
    }
  )
}
