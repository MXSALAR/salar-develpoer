package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIStudyPlan
import com.example.data.model.BadgeItem
import com.example.data.model.MDCATSubject
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.theme.LogicPink
import com.example.ui.theme.PhysicsPurple
import com.example.ui.theme.StreakFlame
import com.example.ui.theme.XpGold
import com.example.ui.viewmodel.MDCATViewModel
import com.example.ui.viewmodel.MainNavTab

@Composable
fun AnalyticsScreen(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val gamification by viewModel.gamification.collectAsState()
  val badges by viewModel.badges.collectAsState()
  val aiPlan by viewModel.aiStudyPlan.collectAsState()
  val isGeneratingPlan by viewModel.isGeneratingAIPlan.collectAsState()
  val testResults by viewModel.testResults.collectAsState()
  val firestoreSessions by viewModel.firestoreQuizSessions.collectAsState()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      // Overall Performance Header Card
      OverallPerformanceCard(
        targetScore = gamification.targetMDCATScore,
        currentEstimated = 168,
        accuracyPercent = 84,
        onExportPdf = { viewModel.exportProgressReport(context) }
      )
    }

    item {
      // Recharts-style Visual Performance Dashboard: Practice Test Scores Over Time
      VisualPerformanceDashboard(
        testResults = testResults,
        onPracticeSubject = { subject ->
          viewModel.setTab(MainNavTab.TESTS)
          viewModel.startQuiz(subject)
        }
      )
    }

    item {
      // Firestore Timed Quiz Sessions Speed & Accuracy Telemetry
      FirestoreQuizTelemetryCard(
        recentSessions = firestoreSessions,
        onStartTimedQuiz = {
          viewModel.setTab(MainNavTab.TESTS)
        }
      )
    }

    item {
      // Subject-wise Accuracy Bars
      SubjectAccuracyBreakdownCard()
    }

    item {
      // Long-Term Weekly Study Pattern Visualization
      StudyPatternVisualizerCard()
    }

    item {
      // Diagnosed Weak Areas & Action Drills
      DiagnosedWeakAreasCard(
        onPracticeTopic = {
          viewModel.setTab(MainNavTab.TESTS)
          viewModel.startQuiz(MDCATSubject.BIOLOGY)
        }
      )
    }

    item {
      // AI-Driven Study Plan Module
      AIStudyPlanModule(
        plan = aiPlan,
        isGenerating = isGeneratingPlan,
        onRegenerate = {
          viewModel.generateAIStudyPlan(listOf("Bioenergetics Complex II", "Enzyme Inhibition Kinetics", "Organic Carbonyl Tests"))
        },
        onToggleDay = { viewModel.toggleDayScheduleDone(it) }
      )
    }

    item {
      // Gamification Badges & Achievements
      BadgesShowcaseCard(badges = badges)
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun OverallPerformanceCard(
  targetScore: Int,
  currentEstimated: Int,
  accuracyPercent: Int,
  onExportPdf: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_overall_performance"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Academic Progress Dashboard",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Tracked across tests & practice drills",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedButton(
          onClick = onExportPdf,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("btn_export_pdf")
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export Report", fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("$currentEstimated", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
          Text("Est. Score / 180", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("$targetScore", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
          Text("Target Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("$accuracyPercent%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BioGreen)
          Text("Avg Accuracy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
fun SubjectAccuracyBreakdownCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Subject Mastery Breakdown",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      val subjectStats = listOf(
        Triple("Biology (34% Weight)", 0.88f, BioGreen),
        Triple("Chemistry (27% Weight)", 0.81f, ChemOrange),
        Triple("Physics (27% Weight)", 0.74f, PhysicsPurple),
        Triple("English (9% Weight)", 0.90f, EnglishBlue),
        Triple("Logical Reasoning (3% Weight)", 0.95f, LogicPink)
      )

      subjectStats.forEach { (label, progress, color) ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = color,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}

@Composable
fun StudyPatternVisualizerCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Weekly Study Patterns & Hours",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Average: 4.2 hours/day • Consistent late-night study habit",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      val days = listOf("Mon" to 3.8f, "Tue" to 4.5f, "Wed" to 4.0f, "Thu" to 5.2f, "Fri" to 3.5f, "Sat" to 6.0f, "Sun" to 4.8f)
      val maxHours = 6.0f

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        days.forEach { (day, hours) ->
          val heightFraction = (hours / maxHours).coerceIn(0.1f, 1f)

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
          ) {
            Text("${hours}h", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .width(16.dp)
                .height((70 * heightFraction).dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(
                  if (day == "Sat") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(day, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
fun DiagnosedWeakAreasCard(onPracticeTopic: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = ChemOrange, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Identified Weak Subtopics",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Subtopics where your accuracy dropped below 70% in recent tests:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      val weakTopics = listOf(
        "Complex II ETC Proton Pumping (Biology)" to "Only 55% accuracy",
        "Reaction Kinetics: Rate Laws & Units of k (Chemistry)" to "62% accuracy",
        "Projectile Motion: Maximum Range Derivations (Physics)" to "68% accuracy"
      )

      weakTopics.forEach { (title, stat) ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Text(stat, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
            Button(
              onClick = onPracticeTopic,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("btn_drill_weak_topic")
            ) {
              Text("Practice", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
fun AIStudyPlanModule(
  plan: AIStudyPlan?,
  isGenerating: Boolean,
  onRegenerate: () -> Unit,
  onToggleDay: (Int) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_ai_study_plan_module"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Personalized AI Study Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(onClick = onRegenerate, enabled = !isGenerating) {
          if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Plan")
          }
        }
      }

      if (plan != null) {
        Text(
          text = plan.aiRecommendationNotes,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Schedule days list
        plan.scheduleDays.forEach { day ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable { onToggleDay(day.dayNumber) },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (day.isDone) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
            )
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                if (day.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (day.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = day.dayLabel,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (day.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${day.topicTitle} • ${day.targetMcqsCount} MCQs • ${day.plannedHours} hrs",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun BadgesShowcaseCard(badges: List<BadgeItem>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = XpGold, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Gamified Badges & Milestones",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(12.dp))

      badges.forEach { badge ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (badge.isUnlocked) XpGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.WorkspacePremium,
              contentDescription = null,
              tint = if (badge.isUnlocked) XpGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(badge.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              if (badge.isUnlocked) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = BioGreen.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    "Unlocked",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioGreen,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Text(badge.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@Composable
fun FirestoreQuizTelemetryCard(
  recentSessions: List<com.example.data.repository.QuizSessionResult>,
  onStartTimedQuiz: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_firestore_quiz_telemetry"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
            text = "Firestore Timed Quiz Sessions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "Speed & Accuracy",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Track completion speed per session (seconds/MCQ) against the official PMC 54s benchmark:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (recentSessions.isEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "No Firestore quiz sessions completed yet.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = onStartTimedQuiz,
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Take First Firestore Timed Quiz")
            }
          }
        }
      } else {
        recentSessions.take(3).forEach { session ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = session.testTitle,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = "⚡ ${String.format("%.1f", session.averageSpeedSecondsPerQuestion)}s / MCQ",
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${session.correctCount}/${session.totalQuestions} Correct (${String.format("%.0f", session.accuracyPercent)}%)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              val isOptimal = session.averageSpeedSecondsPerQuestion <= 54f
              Surface(
                color = if (isOptimal) BioGreen.copy(alpha = 0.15f) else ChemOrange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (isOptimal) "Optimal Pace" else "Overtime",
                  color = if (isOptimal) BioGreen else ChemOrange,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onStartTimedQuiz,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Launch Firestore Timed Quiz Simulator")
        }
      }
    }
  }
}
