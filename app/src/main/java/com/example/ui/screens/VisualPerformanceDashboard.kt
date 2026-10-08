package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MDCATSubject
import com.example.data.model.TestResult
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.theme.LogicPink
import com.example.ui.theme.PhysicsPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ChartTimeRange(val label: String, val days: Int) {
  DAYS_7("7 Days", 7),
  DAYS_30("30 Days", 30),
  ALL_TIME("All Time", 365)
}

enum class DashboardViewMode(val label: String) {
  TRAJECTORY("Trajectory (Area)"),
  SUBJECT_COMPARISON("Subject Compare (Bar)"),
  SUBJECT_MATRIX("Subject Mastery"),
  TEST_HISTORY("History Log")
}

data class SubjectMetric(
  val subject: MDCATSubject,
  val name: String,
  val weightPercent: Int,
  val mcqCount: Int,
  val color: Color,
  val initialScore: Int,
  val latestScore: Int,
  val averageScore: Int,
  val testCount: Int,
  val improvementDelta: Int,
  val status: String,
  val weakTopics: List<String>
)

/**
 * Visual Performance Dashboard inspired by Recharts data visualization.
 * Tracks student scores over time with interactive tooltips, subject filters,
 * comparison bars, and actionable improvement diagnostics.
 */
@Composable
fun VisualPerformanceDashboard(
  testResults: List<TestResult>,
  onPracticeSubject: (MDCATSubject) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedSubjectFilter by remember { mutableStateOf<MDCATSubject?>(null) }
  var selectedTimeRange by remember { mutableStateOf(ChartTimeRange.DAYS_30) }
  var selectedViewMode by remember { mutableStateOf(DashboardViewMode.TRAJECTORY) }
  var activeTooltipResult by remember { mutableStateOf<TestResult?>(null) }

  // Fallback to baseline progression if student is brand new
  val effectiveResults = remember(testResults) {
    if (testResults.isNotEmpty()) {
      testResults.sortedBy { it.dateMillis }
    } else {
      createFallbackResults()
    }
  }

  // Filter results by time range
  val cutoffMillis = remember(selectedTimeRange) {
    if (selectedTimeRange == ChartTimeRange.ALL_TIME) {
      0L
    } else {
      System.currentTimeMillis() - (selectedTimeRange.days.toLong() * 24L * 3600L * 1000L)
    }
  }

  val timeFilteredResults = remember(effectiveResults, cutoffMillis) {
    effectiveResults.filter { it.dateMillis >= cutoffMillis }
  }

  // Filter results by subject (null means All Subjects)
  val subjectAndFilteredResults = remember(timeFilteredResults, selectedSubjectFilter) {
    if (selectedSubjectFilter == null) {
      timeFilteredResults
    } else {
      timeFilteredResults.filter { it.subject == selectedSubjectFilter }
    }
  }

  // Calculate high-level performance metrics
  val firstScore = subjectAndFilteredResults.firstOrNull()?.score ?: 65
  val latestScore = subjectAndFilteredResults.lastOrNull()?.score ?: 90
  val scoresList = subjectAndFilteredResults.map { it.score }
  val averageScore = if (scoresList.isNotEmpty()) scoresList.average().roundToInt() else 80
  val peakScore = scoresList.maxOrNull() ?: 90
  val overallImprovement = latestScore - firstScore

  // Predicted 180 Marks calculation based on PMC weightage:
  // Bio (81), Chem (45), Phys (36), English (9), Logic (9)
  val predictedMDCATScore = remember(effectiveResults) {
    calculatePredictedMDCATScore(effectiveResults)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_visual_performance_dashboard"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // 1. HEADER ROW: Recharts Visual Dashboard title
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Visual Performance Dashboard",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Recharts-style multi-subject score progression",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Improvement Badge
        Surface(
          color = if (overallImprovement >= 0) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (overallImprovement >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingFlat,
              contentDescription = null,
              tint = if (overallImprovement >= 0) Color(0xFF137333) else Color(0xFFC5221F),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (overallImprovement >= 0) "+${overallImprovement}% Growth" else "${overallImprovement}% Change",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (overallImprovement >= 0) Color(0xFF137333) else Color(0xFFC5221F)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. METRICS STRIP: Growth, Average, Peak, Predicted 180 Marks
      PerformanceSummaryRibbon(
        growth = overallImprovement,
        average = averageScore,
        peak = peakScore,
        predicted180Score = predictedMDCATScore
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 3. SUBJECT FILTER CHIPS (Horizontal scroll)
      SubjectFilterChipRow(
        selectedSubject = selectedSubjectFilter,
        onSelectSubject = {
          selectedSubjectFilter = it
          activeTooltipResult = null
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 4. TIME RANGE & VIEW MODE ROW
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Time Range Pills (7d, 30d, All)
        Row(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(2.dp)
        ) {
          ChartTimeRange.values().forEach { range ->
            val isSelected = selectedTimeRange == range
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable {
                  selectedTimeRange = range
                  activeTooltipResult = null
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = range.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // View Mode Switcher
        Row(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(2.dp)
        ) {
          DashboardViewMode.values().forEach { mode ->
            val isSelected = selectedViewMode == mode
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { selectedViewMode = mode }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
              Text(
                text = when (mode) {
                  DashboardViewMode.TRAJECTORY -> "Trajectory"
                  DashboardViewMode.SUBJECT_COMPARISON -> "Compare"
                  DashboardViewMode.SUBJECT_MATRIX -> "Mastery"
                  DashboardViewMode.TEST_HISTORY -> "History"
                },
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 5. ACTIVE VIEW MODE CONTENT
      when (selectedViewMode) {
        DashboardViewMode.TRAJECTORY -> {
          // Recharts-inspired Interactive Canvas Chart
          RechartsInteractiveAreaChart(
            results = subjectAndFilteredResults,
            selectedSubject = selectedSubjectFilter,
            activeTooltip = activeTooltipResult,
            onSelectTooltip = { activeTooltipResult = it }
          )

          // Interactive Tooltip Card if a data point was tapped
          AnimatedVisibility(
            visible = activeTooltipResult != null,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            activeTooltipResult?.let { test ->
              InteractiveTooltipCard(
                result = test,
                onDismiss = { activeTooltipResult = null },
                onPractice = {
                  test.subject?.let { onPracticeSubject(it) } ?: onPracticeSubject(MDCATSubject.BIOLOGY)
                }
              )
            }
          }
        }

        DashboardViewMode.SUBJECT_COMPARISON -> {
          // Recharts-style Subject Bar Chart (Baseline vs Latest)
          RechartsSubjectBarChart(
            results = effectiveResults,
            onPracticeSubject = onPracticeSubject
          )
        }

        DashboardViewMode.SUBJECT_MATRIX -> {
          // Detailed Subject Mastery Progression Grid
          SubjectMasteryMatrix(
            results = effectiveResults,
            onPracticeSubject = onPracticeSubject
          )
        }

        DashboardViewMode.TEST_HISTORY -> {
          // Test History Timeline Log
          TestHistoryTimeline(
            results = subjectAndFilteredResults,
            onRetakeTest = { test ->
              test.subject?.let { onPracticeSubject(it) } ?: onPracticeSubject(MDCATSubject.BIOLOGY)
            }
          )
        }
      }
    }
  }
}

/**
 * Metric summary banner showing key stats.
 */
@Composable
fun PerformanceSummaryRibbon(
  growth: Int,
  average: Int,
  peak: Int,
  predicted180Score: Int
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
      .padding(vertical = 10.dp, horizontal = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Predicted MDCAT 180 Marks
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Predicted 2026", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Row(verticalAlignment = Alignment.Bottom) {
        Text("$predicted180Score", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        Text("/180", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

    // 2. Average Score
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Avg Accuracy", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("$average%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }

    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

    // 3. Peak Score
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Peak Score", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("$peak%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF137333))
    }

    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

    // 4. Overall Gain
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Net Growth", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("+${growth}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BioGreen)
    }
  }
}

/**
 * Filter chip list for selecting MDCAT subjects.
 */
@Composable
fun SubjectFilterChipRow(
  selectedSubject: MDCATSubject?,
  onSelectSubject: (MDCATSubject?) -> Unit
) {
  val subjects = listOf(
    null to "All Subjects",
    MDCATSubject.BIOLOGY to "Biology (81)",
    MDCATSubject.CHEMISTRY to "Chemistry (45)",
    MDCATSubject.PHYSICS to "Physics (36)",
    MDCATSubject.ENGLISH to "English (9)",
    MDCATSubject.LOGICAL_REASONING to "Logic (9)"
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    subjects.forEach { (subject, label) ->
      val isSelected = selectedSubject == subject
      val chipColor = when (subject) {
        MDCATSubject.BIOLOGY -> BioGreen
        MDCATSubject.CHEMISTRY -> ChemOrange
        MDCATSubject.PHYSICS -> PhysicsPurple
        MDCATSubject.ENGLISH -> EnglishBlue
        MDCATSubject.LOGICAL_REASONING -> LogicPink
        null -> MaterialTheme.colorScheme.primary
      }

      Surface(
        color = if (isSelected) chipColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isSelected) chipColor else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .clickable { onSelectSubject(subject) }
          .testTag("filter_chip_${subject?.name ?: "ALL"}")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (subject != null) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else chipColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
          }
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

/**
 * Recharts-inspired Interactive Area & Line Chart with smooth Bézier curves,
 * reference lines, target benchmarks, and touch tooltip scrubbing.
 */
@Composable
fun RechartsInteractiveAreaChart(
  results: List<TestResult>,
  selectedSubject: MDCATSubject?,
  activeTooltip: TestResult?,
  onSelectTooltip: (TestResult?) -> Unit
) {
  val primaryColor = when (selectedSubject) {
    MDCATSubject.BIOLOGY -> BioGreen
    MDCATSubject.CHEMISTRY -> ChemOrange
    MDCATSubject.PHYSICS -> PhysicsPurple
    MDCATSubject.ENGLISH -> EnglishBlue
    MDCATSubject.LOGICAL_REASONING -> LogicPink
    null -> MaterialTheme.colorScheme.primary
  }

  val outlineColor = MaterialTheme.colorScheme.outlineVariant
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val targetBenchmarkColor = Color(0xFF137333)
  val meritBenchmarkColor = Color(0xFFF9AB00)

  val displayPoints = remember(results) {
    if (results.isNotEmpty()) results else createFallbackResults()
  }

  val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

  Column {
    // Reference line legend
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(primaryColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (selectedSubject != null) "${selectedSubject.name} Trajectory" else "Overall Score Trajectory",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = primaryColor
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .width(10.dp)
              .height(2.dp)
              .background(targetBenchmarkColor)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Target (88%)", fontSize = 9.sp, color = targetBenchmarkColor, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .width(10.dp)
              .height(2.dp)
              .background(meritBenchmarkColor)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Merit Cutoff (77%)", fontSize = 9.sp, color = meritBenchmarkColor, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Chart Canvas with Touch Interaction
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .testTag("chart_recharts_canvas")
        .pointerInput(displayPoints) {
          detectTapGestures { offset ->
            val stepX = size.width / (displayPoints.size.coerceAtLeast(2) - 1)
            val tappedIndex = (offset.x / stepX).roundToInt().coerceIn(0, displayPoints.size - 1)
            onSelectTooltip(displayPoints[tappedIndex])
          }
        }
        .pointerInput(displayPoints) {
          detectDragGestures { change, _ ->
            change.consume()
            val stepX = size.width / (displayPoints.size.coerceAtLeast(2) - 1)
            val draggedIndex = (change.position.x / stepX).roundToInt().coerceIn(0, displayPoints.size - 1)
            onSelectTooltip(displayPoints[draggedIndex])
          }
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val topPadding = 18.dp.toPx()
        val bottomPadding = 24.dp.toPx()
        val usableHeight = canvasHeight - topPadding - bottomPadding

        val minScore = 50f
        val maxScore = 100f
        val range = maxScore - minScore

        fun getY(score: Int): Float {
          val normalized = (score.coerceIn(50, 100) - minScore) / range
          return canvasHeight - bottomPadding - (normalized * usableHeight)
        }

        val stepX = if (displayPoints.size > 1) canvasWidth / (displayPoints.size - 1) else canvasWidth

        // 1. Draw horizontal grid lines (at 60%, 75%, 90%)
        listOf(60, 75, 90).forEach { gridScore ->
          val y = getY(gridScore)
          drawLine(
            color = outlineColor.copy(alpha = 0.4f),
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
          )
        }

        // 2. Draw Target Benchmark line (88% ~ 160 marks)
        val targetY = getY(88)
        drawLine(
          color = targetBenchmarkColor.copy(alpha = 0.75f),
          start = Offset(0f, targetY),
          end = Offset(canvasWidth, targetY),
          strokeWidth = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )

        // 3. Draw Merit Cutoff line (77% ~ 140 marks)
        val meritY = getY(77)
        drawLine(
          color = meritBenchmarkColor.copy(alpha = 0.75f),
          start = Offset(0f, meritY),
          end = Offset(canvasWidth, meritY),
          strokeWidth = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )

        // 4. Build smooth Bézier curve line and area fill paths
        val linePath = Path()
        val fillPath = Path()

        displayPoints.forEachIndexed { index, test ->
          val x = index * stepX
          val y = getY(test.score)

          if (index == 0) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, canvasHeight - bottomPadding)
            fillPath.lineTo(x, y)
          } else {
            val prevX = (index - 1) * stepX
            val prevY = getY(displayPoints[index - 1].score)
            val controlX1 = (prevX + x) / 2
            val controlY1 = prevY
            val controlX2 = (prevX + x) / 2
            val controlY2 = y

            linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
          }

          if (index == displayPoints.size - 1) {
            fillPath.lineTo(x, canvasHeight - bottomPadding)
            fillPath.close()
          }
        }

        // Fill area beneath curve with gradient (Recharts-style)
        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              primaryColor.copy(alpha = 0.35f),
              primaryColor.copy(alpha = 0.02f)
            ),
            startY = topPadding,
            endY = canvasHeight - bottomPadding
          )
        )

        // Stroke line
        drawPath(
          path = linePath,
          color = primaryColor,
          style = Stroke(
            width = 3.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        )

        // 5. Draw data points and active tooltip indicator crosshair
        displayPoints.forEachIndexed { index, test ->
          val x = index * stepX
          val y = getY(test.score)
          val isSelected = activeTooltip?.id == test.id

          if (isSelected) {
            // Recharts-style vertical crosshair guideline
            drawLine(
              color = primaryColor.copy(alpha = 0.6f),
              start = Offset(x, topPadding),
              end = Offset(x, canvasHeight - bottomPadding),
              strokeWidth = 1.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            // Large highlighted pulsing ring
            drawCircle(
              color = primaryColor.copy(alpha = 0.25f),
              radius = 12.dp.toPx(),
              center = Offset(x, y)
            )
          }

          // Point outer halo
          drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
            center = Offset(x, y)
          )

          // Point solid core
          drawCircle(
            color = primaryColor,
            radius = if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx(),
            center = Offset(x, y)
          )

          // Crisp white center dot
          drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(x, y)
          )
        }
      }

      // X-Axis Date Timestamps overlay
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        val sampleStride = (displayPoints.size / 5).coerceAtLeast(1)
        displayPoints.forEachIndexed { index, test ->
          if (index % sampleStride == 0 || index == displayPoints.size - 1) {
            Text(
              text = dateFormatter.format(Date(test.dateMillis)),
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    Text(
      text = "💡 Tap or scrub any point on the chart to inspect score & weak topics.",
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

/**
 * Recharts-style Interactive Tooltip Card that pops up when a point is touched.
 */
@Composable
fun InteractiveTooltipCard(
  result: TestResult,
  onDismiss: () -> Unit,
  onPractice: () -> Unit
) {
  val dateFormatted = remember(result.dateMillis) {
    SimpleDateFormat("EEEE, dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(result.dateMillis))
  }

  val subjectColor = when (result.subject) {
    MDCATSubject.BIOLOGY -> BioGreen
    MDCATSubject.CHEMISTRY -> ChemOrange
    MDCATSubject.PHYSICS -> PhysicsPurple
    MDCATSubject.ENGLISH -> EnglishBlue
    MDCATSubject.LOGICAL_REASONING -> LogicPink
    null -> MaterialTheme.colorScheme.primary
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 10.dp)
      .testTag("tooltip_active_test_card"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
    border = BorderStroke(1.5.dp, subjectColor)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = subjectColor,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = result.subject?.name ?: "FULL MOCK (180 MCQs)",
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = dateFormatted,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = result.testTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${result.correctCount} Correct • ${result.wrongCount} Wrong • Time: ${result.timeTakenSeconds / 60} mins",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          color = subjectColor.copy(alpha = 0.15f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "${result.score}%",
              fontWeight = FontWeight.Black,
              fontSize = 16.sp,
              color = subjectColor
            )
            val scaled180Marks = ((result.score / 100.0) * 180).roundToInt()
            Text(
              text = "$scaled180Marks/180",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = subjectColor
            )
          }
        }
      }

      if (result.weakTopics.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = ChemOrange, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Weak Topics: ${result.weakTopics.joinToString(", ")}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(
          onClick = onPractice,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Practice This Subject", fontSize = 10.sp)
        }
      }
    }
  }
}

/**
 * Recharts-style Subject Bar Chart comparing Baseline Attempt vs Latest Score.
 */
@Composable
fun RechartsSubjectBarChart(
  results: List<TestResult>,
  onPracticeSubject: (MDCATSubject) -> Unit
) {
  val subjectMetrics = remember(results) { computeSubjectMetrics(results) }

  Column(modifier = Modifier.fillMaxWidth()) {
    // Chart Title & Legend
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Baseline vs Current Score by Subject",
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Initial", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Latest", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Grouped Bar Visualizer
    subjectMetrics.forEach { metric ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 5.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(metric.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${metric.name} (${metric.mcqCount} MCQs • ${metric.weightPercent}%)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${metric.initialScore}% → ${metric.latestScore}%",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = if (metric.improvementDelta >= 0) Color(0xFFE6F4EA) else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "+${metric.improvementDelta}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (metric.improvementDelta >= 0) Color(0xFF137333) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dual bar display
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
          // Baseline bar (lighter tint)
          Box(
            modifier = Modifier
              .fillMaxWidth(metric.initialScore / 100f)
              .height(18.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(metric.color.copy(alpha = 0.35f))
          )

          // Latest score bar (full saturated color, partial height or overlay)
          Box(
            modifier = Modifier
              .fillMaxWidth(metric.latestScore / 100f)
              .height(18.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(metric.color.copy(alpha = 0.85f))
          )
        }
      }
    }
  }
}

/**
 * Subject Mastery Matrix displaying detailed progress, tests taken, and quick drill launcher.
 */
@Composable
fun SubjectMasteryMatrix(
  results: List<TestResult>,
  onPracticeSubject: (MDCATSubject) -> Unit
) {
  val subjectMetrics = remember(results) { computeSubjectMetrics(results) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    subjectMetrics.forEach { metric ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = metric.color,
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = metric.name.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = metric.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Text(
                  text = "${metric.mcqCount} Questions in MDCAT • ${metric.testCount} Tests Completed",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              color = metric.color.copy(alpha = 0.15f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "${metric.latestScore}% (${metric.status})",
                color = metric.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Accuracy Progress bar
          LinearProgressIndicator(
            progress = { metric.latestScore / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = metric.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (metric.weakTopics.isNotEmpty()) "Focus: ${metric.weakTopics.first()}" else "All core concepts mastered",
              fontSize = 10.sp,
              color = if (metric.weakTopics.isNotEmpty()) MaterialTheme.colorScheme.error else BioGreen,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )

            OutlinedButton(
              onClick = { onPracticeSubject(metric.subject) },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Drill", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

/**
 * Filtered practice test history log.
 */
@Composable
fun TestHistoryTimeline(
  results: List<TestResult>,
  onRetakeTest: (TestResult) -> Unit
) {
  val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = "Practice Test Scores History (${results.size} Recorded)",
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp
    )

    results.asReversed().take(10).forEach { test ->
      val subjectColor = when (test.subject) {
        MDCATSubject.BIOLOGY -> BioGreen
        MDCATSubject.CHEMISTRY -> ChemOrange
        MDCATSubject.PHYSICS -> PhysicsPurple
        MDCATSubject.ENGLISH -> EnglishBlue
        MDCATSubject.LOGICAL_REASONING -> LogicPink
        null -> MaterialTheme.colorScheme.primary
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(subjectColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = test.testTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${dateFormatter.format(Date(test.dateMillis))} • ${test.subject?.name ?: "Full Mock"} • ${test.correctCount}/${test.totalQuestions} Correct",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = subjectColor.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "${test.score}%",
                color = subjectColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
              onClick = { onRetakeTest(test) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Retake Test",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}

// ---------------- Helper Functions ----------------

private fun computeSubjectMetrics(results: List<TestResult>): List<SubjectMetric> {
  val subjectsConfig = listOf(
    Triple(MDCATSubject.BIOLOGY, "Biology", 81 to 45),
    Triple(MDCATSubject.CHEMISTRY, "Chemistry", 45 to 25),
    Triple(MDCATSubject.PHYSICS, "Physics", 36 to 20),
    Triple(MDCATSubject.ENGLISH, "English", 9 to 5),
    Triple(MDCATSubject.LOGICAL_REASONING, "Logical Reasoning", 9 to 5)
  )

  return subjectsConfig.map { (subject, name, weights) ->
    val (mcqs, weightPct) = weights
    val subjectTests = results.filter { it.subject == subject }.sortedBy { it.dateMillis }

    val initial = subjectTests.firstOrNull()?.score ?: when (subject) {
      MDCATSubject.BIOLOGY -> 65
      MDCATSubject.CHEMISTRY -> 68
      MDCATSubject.PHYSICS -> 62
      MDCATSubject.ENGLISH -> 78
      MDCATSubject.LOGICAL_REASONING -> 89
    }

    val latest = subjectTests.lastOrNull()?.score ?: when (subject) {
      MDCATSubject.BIOLOGY -> 91
      MDCATSubject.CHEMISTRY -> 86
      MDCATSubject.PHYSICS -> 73
      MDCATSubject.ENGLISH -> 78
      MDCATSubject.LOGICAL_REASONING -> 89
    }

    val scores = subjectTests.map { it.score }
    val avg = if (scores.isNotEmpty()) scores.average().roundToInt() else (initial + latest) / 2
    val delta = latest - initial

    val color = when (subject) {
      MDCATSubject.BIOLOGY -> BioGreen
      MDCATSubject.CHEMISTRY -> ChemOrange
      MDCATSubject.PHYSICS -> PhysicsPurple
      MDCATSubject.ENGLISH -> EnglishBlue
      MDCATSubject.LOGICAL_REASONING -> LogicPink
    }

    val status = when {
      latest >= 90 -> "High Merit"
      latest >= 80 -> "Strong"
      latest >= 70 -> "Progressing"
      else -> "Needs Focus"
    }

    val weak = subjectTests.flatMap { it.weakTopics }.distinct().take(2)

    SubjectMetric(
      subject = subject,
      name = name,
      weightPercent = weightPct,
      mcqCount = mcqs,
      color = color,
      initialScore = initial,
      latestScore = latest,
      averageScore = avg,
      testCount = subjectTests.size.coerceAtLeast(1),
      improvementDelta = delta,
      status = status,
      weakTopics = weak
    )
  }
}

private fun calculatePredictedMDCATScore(results: List<TestResult>): Int {
  val metrics = computeSubjectMetrics(results)
  // PMC 180 Marks weighted sum:
  // Bio: 81 * (bioScore/100)
  // Chem: 45 * (chemScore/100)
  // Phys: 36 * (physScore/100)
  // Eng: 9 * (engScore/100)
  // Logic: 9 * (logicScore/100)
  var totalWeighted = 0.0
  metrics.forEach { m ->
    totalWeighted += (m.latestScore / 100.0) * m.mcqCount
  }
  return totalWeighted.roundToInt().coerceIn(120, 180)
}

private fun createFallbackResults(): List<TestResult> {
  val now = System.currentTimeMillis()
  val day = 24L * 3600 * 1000
  return listOf(
    TestResult("tr_1", "Biology Unit 1 Diagnostic", MDCATSubject.BIOLOGY, 65, 100, 65, 35, 2800, now - 28 * day, listOf("Cellular Respiration")),
    TestResult("tr_2", "Chemistry Fund. Diagnostic", MDCATSubject.CHEMISTRY, 68, 100, 68, 32, 3000, now - 25 * day, listOf("Gas Laws")),
    TestResult("tr_3", "Physics Mechanics Drill", MDCATSubject.PHYSICS, 62, 100, 62, 38, 3200, now - 23 * day, listOf("Vectors & Equilibrium")),
    TestResult("tr_4", "English Grammar Concord", MDCATSubject.ENGLISH, 78, 100, 78, 22, 1800, now - 21 * day, listOf("Subject-Verb Concord")),
    TestResult("tr_5", "Biology Bioenergetics", MDCATSubject.BIOLOGY, 74, 100, 74, 26, 2700, now - 18 * day, listOf("Enzyme Inhibitors")),
    TestResult("tr_6", "Full MDCAT Mock 1", null, 135, 180, 135, 45, 8900, now - 16 * day, listOf("Electromagnetism")),
    TestResult("tr_7", "Chemistry Organic Groups", MDCATSubject.CHEMISTRY, 77, 100, 77, 23, 2500, now - 14 * day, listOf("Aldehyde Tests")),
    TestResult("tr_8", "Physics Waves Drill", MDCATSubject.PHYSICS, 73, 100, 73, 27, 2600, now - 11 * day, listOf("Magnetic Flux")),
    TestResult("tr_9", "Logic Deductions Drill", MDCATSubject.LOGICAL_REASONING, 89, 100, 89, 11, 1400, now - 9 * day, listOf("Syllogisms")),
    TestResult("tr_10", "Biology Genetics Drill", MDCATSubject.BIOLOGY, 84, 100, 84, 16, 2400, now - 7 * day, listOf("Mendelian Genetics")),
    TestResult("tr_11", "Full MDCAT Mock 2", null, 152, 180, 152, 28, 8700, now - 5 * day, listOf("Projectile Motion")),
    TestResult("tr_12", "Chemistry Kinetics Drill", MDCATSubject.CHEMISTRY, 86, 100, 86, 14, 2300, now - 3 * day, listOf("Rate Laws")),
    TestResult("tr_13", "Biology High-Yield Drill", MDCATSubject.BIOLOGY, 91, 100, 91, 9, 2100, now - 2 * day, listOf("Proton Gradient")),
    TestResult("tr_14", "Full MDCAT Simulator 3", null, 164, 180, 164, 16, 8500, now - 1 * day, listOf("AC Phase Angle"))
  )
}
