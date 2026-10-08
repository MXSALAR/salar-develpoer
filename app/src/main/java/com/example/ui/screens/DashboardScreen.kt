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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Event
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MDCATSubject
import com.example.ui.theme.StreakFlame
import com.example.ui.theme.XpGold
import com.example.ui.viewmodel.MDCATViewModel
import com.example.ui.viewmodel.MainNavTab

@Composable
fun DashboardScreen(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  val gamification by viewModel.gamification.collectAsState()
  val aiPlan by viewModel.aiStudyPlan.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val lastSync by viewModel.lastSyncTimestamp.collectAsState()
  val calendarEvents by viewModel.calendarEvents.collectAsState()
  val userAccount by viewModel.userAccount.collectAsState()
  val courseSubscriptions by viewModel.courseSubscriptions.collectAsState()
  val testResults by viewModel.testResults.collectAsState()
  val remainingDays by viewModel.remainingDays.collectAsState()
  val studyScheduleAlertEnabled by viewModel.studyScheduleAlertEnabled.collectAsState()
  val context = LocalContext.current

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      // Role Mode Preview Switcher (Student View vs Instructor View)
      Surface(
        color = if (userAccount.isInstructor) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else Color(0xFFE8F0FE),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (userAccount.isInstructor) MaterialTheme.colorScheme.outlineVariant else Color(0xFF4285F4).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (userAccount.isInstructor) Icons.Default.MedicalServices else Icons.Default.School,
              contentDescription = null,
              tint = if (userAccount.isInstructor) MaterialTheme.colorScheme.primary else Color(0xFF1967D2),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (userAccount.isInstructor) "Active View: Instructor (Dr. Salar)" else "Active View: Student Mode (Ahmad Khan)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (userAccount.isInstructor) MaterialTheme.colorScheme.onSurface else Color(0xFF1967D2)
              )
              Text(
                text = if (userAccount.isInstructor) "Full admin & syllabus editing privileges" else "Student dashboard • Study notes • Quizzes • Mock tests",
                fontSize = 9.sp,
                color = if (userAccount.isInstructor) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1967D2).copy(alpha = 0.8f)
              )
            }
          }

          TextButton(
            onClick = {
              if (userAccount.isInstructor) {
                viewModel.loginWithGmail("student.mdcat2026@gmail.com", "Ahmad Khan (Student)")
              } else {
                viewModel.loginWithGmail("shaukatsalar231@gmail.com", "Dr. Salar")
              }
            },
            modifier = Modifier.height(30.dp)
          ) {
            Text(
              text = if (userAccount.isInstructor) "Preview as Student" else "Switch to Dr. Salar",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (userAccount.isInstructor) MaterialTheme.colorScheme.primary else Color(0xFF1967D2)
            )
          }
        }
      }
    }
    item {
      Spacer(modifier = Modifier.height(8.dp))
      // MDCAT Countdown Banner with Dynamic Remaining Days & 180 MCQ Pattern
      CountdownHeroBanner(
        remainingDays = remainingDays,
        targetScore = gamification.targetMDCATScore,
        onStartQuickQuiz = {
          viewModel.setTab(MainNavTab.TESTS)
          viewModel.startQuiz(MDCATSubject.BIOLOGY)
        }
      )
    }

    item {
      // Notification / Toast Alert System for sticking to personalized AI Study Schedule
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_study_schedule_alert"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "AI Study Schedule Reminder",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Prompt to stick to personalized goals",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Switch(
              checked = studyScheduleAlertEnabled,
              onCheckedChange = { isEnabled ->
                viewModel.setStudyScheduleAlertEnabled(isEnabled)
                if (isEnabled) {
                  viewModel.triggerStudyScheduleAlert(context, "✅ Daily AI study alerts activated! Stick to your target.")
                }
              },
              modifier = Modifier.testTag("switch_study_schedule_alert")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          val todayFocus = aiPlan?.scheduleDays?.firstOrNull { !it.isDone }
          val topicText = todayFocus?.topicTitle ?: "Biology: Cell Structure & Biological Molecules"
          val targetMcqs = todayFocus?.targetMcqsCount ?: 45

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "TODAY'S AI SCHEDULE FOCUS",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = topicText,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Goal: $targetMcqs MCQs practice today",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Button(
                onClick = {
                  viewModel.triggerStudyScheduleAlert(
                    context,
                    "⏰ Study Alert: Stick to today's schedule! Today's focus: $topicText (Target: $targetMcqs MCQs)."
                  )
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_trigger_study_schedule_toast")
              ) {
                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Alert Me", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    item {
      // Super Simple 3-Step Guide so anyone can use and understand the app in seconds
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("card_simple_3step_guide"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = Color(0xFFFEF7E0),
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFF9AB00),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "How to Prepare (3 Easy Steps)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Easy Guide",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          SimpleGuideStepItem(
            stepNumber = "1",
            title = "Read Handbooks & Notes",
            description = "Class 11 & 12 Biology, Chemistry, Physics",
            actionLabel = "Open Notes",
            badgeColor = Color(0xFF1B873F),
            onClick = { viewModel.setTab(MainNavTab.STUDY) }
          )

          Spacer(modifier = Modifier.height(8.dp))

          SimpleGuideStepItem(
            stepNumber = "2",
            title = "Solve 180 MCQs Tests",
            description = "Official PMC syllabus mock tests & practice",
            actionLabel = "Start Test",
            badgeColor = Color(0xFF1967D2),
            onClick = { viewModel.setTab(MainNavTab.TESTS) }
          )

          Spacer(modifier = Modifier.height(8.dp))

          SimpleGuideStepItem(
            stepNumber = "3",
            title = "Check Scores & Progress",
            description = "See weak topics, test history & marks",
            actionLabel = "View Scores",
            badgeColor = Color(0xFF7B1FA2),
            onClick = { viewModel.setTab(MainNavTab.ANALYTICS) }
          )
        }
      }
    }

    if (!userAccount.isLoggedIn) {
      item {
        // Gmail Login Prompt Banner
        Card(
          onClick = { viewModel.toggleLoginDialog(true) },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_gmail_login")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = CircleShape,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("G", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Sign in with Gmail for Full Access",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Unlock Dr. Salar's live interactive classes, full MCQs, and syllabus handbooks.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = { viewModel.toggleLoginDialog(true) },
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      // Live Session Interactive Block
      LiveSessionBlock(viewModel = viewModel)
    }

    item {
      // Course Subscription Showcase Block
      val activeSubscriptionsCount = courseSubscriptions.count { it.isSubscribed }
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF9AB00), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Course Subscriptions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Surface(
              color = if (activeSubscriptionsCount > 0) Color(0xFFE6F4EA) else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = if (activeSubscriptionsCount > 0) "$activeSubscriptionsCount Enrolled" else "Special Bundles",
                color = if (activeSubscriptionsCount > 0) Color(0xFF137333) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Dr. Salar's comprehensive 11th & 12th Class Pre-Medical and High-Yield MDCAT Crash Courses.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )

          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = { viewModel.toggleSubscriptionSheet(true) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_open_course_subscriptions")
          ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Browse & Subscribe to Courses", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    item {
      // Gamification Stat Strip: Streak, XP, Level, Sync
      GamificationStrip(
        streakDays = gamification.streakDays,
        xp = gamification.xpPoints,
        level = gamification.level,
        isSyncing = isSyncing,
        onTriggerSync = { viewModel.triggerSync() }
      )
    }

    item {
      // Practice Test Performance Trends Visualization
      PerformanceTrendChartCard(
        testResults = testResults,
        onOpenAnalytics = { viewModel.setTab(MainNavTab.ANALYTICS) }
      )
    }

    item {
      // Daily Goal Progress
      DailyGoalCard(
        solvedMcqs = 34,
        targetMcqs = 50,
        studiedHours = 2.8f,
        targetHours = 4.0f
      )
    }

    item {
      // Today's AI-Driven Focus
      TodayAIFocusCard(
        aiPlan = aiPlan,
        onOpenAnalytics = { viewModel.setTab(MainNavTab.ANALYTICS) }
      )
    }

    item {
      // Quick Navigation Actions - 4 Core Pillars for Easy Navigation
      Text(
        text = "Quick Study Access",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionCard(
          title = "1. Notes & Books",
          subtitle = "Class 11 & 12",
          icon = Icons.Default.Book,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.weight(1f),
          testTag = "btn_quick_notes",
          onClick = { viewModel.setTab(MainNavTab.STUDY) }
        )
        QuickActionCard(
          title = "2. 180 MCQs Tests",
          subtitle = "Official Simulator",
          icon = Icons.Default.Quiz,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.weight(1f),
          testTag = "btn_quick_quiz",
          onClick = { viewModel.setTab(MainNavTab.TESTS) }
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionCard(
          title = "3. My Scores",
          subtitle = "Check Marks & Rank",
          icon = Icons.Default.BarChart,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.weight(1f),
          testTag = "btn_quick_analytics",
          onClick = { viewModel.setTab(MainNavTab.ANALYTICS) }
        )
        QuickActionCard(
          title = "4. Ask Doubts",
          subtitle = "Community & Vault",
          icon = Icons.Default.Forum,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
          testTag = "btn_quick_community",
          onClick = { viewModel.setTab(MainNavTab.COMMUNITY_NOTES) }
        )
      }
    }

    item {
      // Upcoming Milestones & Deadlines
      MilestonesSection(calendarEvents = calendarEvents)
    }

    item {
      // Remote Offline Status banner
      OfflineSyncStatusCard(
        isSyncing = isSyncing,
        lastSyncTimestamp = lastSync,
        onSyncClick = { viewModel.triggerSync() }
      )
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun CountdownHeroBanner(
  remainingDays: Int = 72,
  targetScore: Int,
  onStartQuickQuiz: () -> Unit
) {
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      currentTimeMillis = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
  val dayFormatter = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
  val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }
  val targetExamFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  val currentTimeStr = remember(currentTimeMillis) { timeFormatter.format(Date(currentTimeMillis)) }
  val currentDayStr = remember(currentTimeMillis) { dayFormatter.format(Date(currentTimeMillis)) }
  val currentDateStr = remember(currentTimeMillis) { dateFormatter.format(Date(currentTimeMillis)) }
  val targetExamDateStr = remember(remainingDays, currentTimeMillis) {
    val targetMillis = currentTimeMillis + (remainingDays.toLong() * 24L * 3600L * 1000L)
    targetExamFormatter.format(Date(targetMillis))
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_countdown_banner"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
  ) {
    Box(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary,
              MaterialTheme.colorScheme.secondary
            )
          )
        )
        .padding(18.dp)
    ) {
      Column {
        // Top row: Header tag & Target Score
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = "MDCAT 2026 COUNTDOWN",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          }

          Text(
            text = "Target: $targetScore/180",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Side-by-Side: Days Left Box and Date, Time, Year Box
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // 1. DAYS LEFT BOX
          Surface(
            color = Color.White.copy(alpha = 0.22f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
              .weight(1.05f)
              .testTag("box_days_left")
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "$remainingDays",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 40.sp
              )
              Text(
                text = "DAYS LEFT",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 0.5.sp
              )
              Text(
                text = "Exam: ~$targetExamDateStr",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
          }

          // 2. LIVE DATE, TIME & YEAR BOX (Directly beside Days Left box)
          Surface(
            color = Color.White.copy(alpha = 0.22f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier
              .weight(1.4f)
              .testTag("box_date_time_year")
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              // Current Live Time
              Row(
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.AccessTime,
                  contentDescription = "Current Time",
                  tint = Color.White,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = currentTimeStr,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White,
                  letterSpacing = 0.5.sp
                )
              }

              Spacer(modifier = Modifier.height(3.dp))

              // Current Day of Week & Date
              Row(
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarToday,
                  contentDescription = "Current Date",
                  tint = Color.White.copy(alpha = 0.9f),
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                  Text(
                    text = currentDayStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = currentDateStr,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))

              // Academic Year 2026 Badge
              Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "📅 Academic Year 2026",
                  color = Color.White,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          color = Color.White.copy(alpha = 0.18f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "⚡ 180 MCQs: Bio 81 • Chem 45 • Phys 36 • English 9 • Logic 9",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        Text(
          text = "Every day counts. Master your weak areas with Dr. Salar's high-yield drills.",
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        Button(
          onClick = onStartQuickQuiz,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_daily_drill")
        ) {
          Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Start Today's Rapid Drill (Biology)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}

@Composable
fun GamificationStrip(
  streakDays: Int,
  xp: Int,
  level: Int,
  isSyncing: Boolean,
  onTriggerSync: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Streak Card
    Card(
      modifier = Modifier
        .weight(1f)
        .testTag("streak_indicator"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(StreakFlame.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.LocalFireDepartment,
            contentDescription = "Streak",
            tint = StreakFlame,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "$streakDays Days",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Active Streak",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }
    }

    // XP Card
    Card(
      modifier = Modifier
        .weight(1f)
        .testTag("xp_indicator"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(XpGold.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.WorkspacePremium,
            contentDescription = "XP",
            tint = XpGold,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "$xp XP",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Level $level Scholar",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }
    }
  }
}

@Composable
fun DailyGoalCard(
  solvedMcqs: Int,
  targetMcqs: Int,
  studiedHours: Float,
  targetHours: Float
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_daily_goals"),
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
          text = "Daily Study Goals",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${((solvedMcqs.toFloat() / targetMcqs) * 100).toInt()}% Done",
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // MCQs Progress
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("MCQs Practice", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$solvedMcqs / $targetMcqs MCQs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { (solvedMcqs.toFloat() / targetMcqs).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Study Hours Progress
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Study Duration", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$studiedHours / $targetHours hrs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { (studiedHours / targetHours).coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )
    }
  }
}

@Composable
fun TodayAIFocusCard(
  aiPlan: com.example.data.model.AIStudyPlan?,
  onOpenAnalytics: () -> Unit
) {
  val firstDay = aiPlan?.scheduleDays?.firstOrNull()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_ai_study_plan"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "AI-Driven Study Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "Targeted Weak Areas",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = firstDay?.topicTitle ?: "Bioenergetics & Oxidative Phosphorylation",
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text(
        text = "Recommended by Dr salar: Focus on Complex II and PFK-1 kinetics to fix diagnostic gaps.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = onOpenAnalytics,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("View Full 7-Day Plan")
        }
      }
    }
  }
}

@Composable
fun QuickActionCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
      Spacer(modifier = Modifier.height(12.dp))
      Text(text = title, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 15.sp)
      Text(text = subtitle, fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
    }
  }
}

@Composable
fun MilestonesSection(calendarEvents: List<com.example.data.model.CalendarEvent>) {
  Column {
    Text(
      text = "Upcoming Exam Milestones",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))

    calendarEvents.take(3).forEach { event ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Event,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = event.title,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )
            Text(
              text = event.notes,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun OfflineSyncStatusCard(
  isSyncing: Boolean,
  lastSyncTimestamp: Long,
  onSyncClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          Icons.Default.CloudDone,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Offline Access & Cloud Sync",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
          Text(
            text = if (isSyncing) "Synchronizing data..." else "Materials cached locally for remote sessions",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      OutlinedButton(
        onClick = onSyncClick,
        enabled = !isSyncing,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("btn_sync_now")
      ) {
        if (isSyncing) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Sync", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun SimpleGuideStepItem(
  stepNumber: String,
  title: String,
  description: String,
  actionLabel: String,
  badgeColor: Color,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = badgeColor,
          shape = CircleShape,
          modifier = Modifier.size(24.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = stepNumber,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = description,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
