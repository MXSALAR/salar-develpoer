package com.example.data.ai

import android.util.Log
import com.example.data.model.AIStudyPlan
import com.example.data.model.MDCATSubject
import com.example.data.model.StudyDaySchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiStudyPlanService {

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  suspend fun generatePersonalizedStudyPlan(
    weakAreas: List<String>,
    targetScore: Int,
    dailyHours: Float,
    daysRemaining: Int
  ): AIStudyPlan = withContext(Dispatchers.IO) {
    val cleanWeakAreas = if (weakAreas.isEmpty()) {
      listOf("Complex II in Bioenergetics", "Enzyme Inhibition Kinetics", "Organic Carbonyl Tests", "Projectile Motion Trajectory")
    } else {
      weakAreas.distinct()
    }

    // Try Gemini API if key is available
    val apiKey = try {
      com.example.BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
      try {
        val aiPlan = callGeminiForStudyPlan(apiKey, cleanWeakAreas, targetScore, dailyHours, daysRemaining)
        if (aiPlan != null) {
          return@withContext aiPlan
        }
      } catch (e: Exception) {
        Log.w("GeminiStudyPlan", "Falling back to local intelligent engine: ${e.message}")
      }
    }

    // High-yield clinical rule-based generation curated by Dr salar
    return@withContext generateAdaptiveLocalStudyPlan(cleanWeakAreas, targetScore, dailyHours)
  }

  private fun callGeminiForStudyPlan(
    apiKey: String,
    weakAreas: List<String>,
    targetScore: Int,
    dailyHours: Float,
    daysRemaining: Int
  ): AIStudyPlan? {
    val prompt = """
      You are Dr salar's AI Medical Education Planner for MDCAT students.
      The student has a target MDCAT score of $targetScore / 180 with $daysRemaining days left.
      Identified weak areas from recent diagnostic tests:
      ${weakAreas.joinToString(", ")}
      Daily study capacity: $dailyHours hours/day.

      Create a structured 7-day high-yield study plan addressing these weak areas.
      Respond ONLY in valid JSON with this structure:
      {
        "recommendationNotes": "Personalized advice from Dr salar addressing the student's weaknesses.",
        "days": [
          {
            "dayNumber": 1,
            "dayLabel": "Day 1: Bioenergetics Deep Dive",
            "subject": "BIOLOGY",
            "topicTitle": "Topic name",
            "targetMcqs": 40,
            "plannedHours": 3.5
          }
        ]
      }
    """.trimIndent()

    val requestJson = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", prompt)
            })
          })
        })
      })
    }

    val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
      .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
      .post(requestBody)
      .build()

    val response = okHttpClient.newCall(request).execute()
    if (!response.isSuccessful) return null

    val responseBody = response.body?.string() ?: return null
    val root = JSONObject(responseBody)
    val candidates = root.optJSONArray("candidates") ?: return null
    val firstCandidate = candidates.optJSONObject(0) ?: return null
    val text = firstCandidate.optJSONObject("content")
      ?.optJSONArray("parts")
      ?.optJSONObject(0)
      ?.optString("text") ?: return null

    // Extract JSON block if wrapped in markdown code fence
    val cleanJsonText = text.replace("```json", "").replace("```", "").trim()
    val planObj = JSONObject(cleanJsonText)
    val recommendationNotes = planObj.optString("recommendationNotes", "Dr salar: Focus on conceptual understanding and eliminating distractor options.")
    val daysArray = planObj.optJSONArray("days") ?: return null

    val scheduleList = mutableListOf<StudyDaySchedule>()
    for (i in 0 until daysArray.length()) {
      val dayItem = daysArray.getJSONObject(i)
      val subjectStr = dayItem.optString("subject", "BIOLOGY").uppercase()
      val subject = runCatching { MDCATSubject.valueOf(subjectStr) }.getOrDefault(MDCATSubject.BIOLOGY)

      scheduleList.add(
        StudyDaySchedule(
          dayNumber = dayItem.optInt("dayNumber", i + 1),
          dayLabel = dayItem.optString("dayLabel", "Day ${i + 1}"),
          focusSubject = subject,
          topicTitle = dayItem.optString("topicTitle", "High-Yield Topic"),
          targetMcqsCount = dayItem.optInt("targetMcqs", 35),
          plannedHours = dayItem.optDouble("plannedHours", dailyHours.toDouble()).toFloat(),
          isDone = false
        )
      )
    }

    return AIStudyPlan(
      id = "plan_${System.currentTimeMillis()}",
      studentTargetScore = targetScore,
      weakAreas = weakAreas,
      dailyTargetHours = dailyHours,
      generatedTimestamp = System.currentTimeMillis(),
      scheduleDays = scheduleList,
      aiRecommendationNotes = recommendationNotes
    )
  }

  private fun generateAdaptiveLocalStudyPlan(
    weakAreas: List<String>,
    targetScore: Int,
    dailyHours: Float
  ): AIStudyPlan {
    val schedules = mutableListOf<StudyDaySchedule>()

    val subjectsOrder = listOf(
      MDCATSubject.BIOLOGY,
      MDCATSubject.CHEMISTRY,
      MDCATSubject.PHYSICS,
      MDCATSubject.BIOLOGY,
      MDCATSubject.CHEMISTRY,
      MDCATSubject.ENGLISH,
      MDCATSubject.LOGICAL_REASONING
    )

    val topics = listOf(
      "Bioenergetics & Oxidative Phosphorylation" to "Target 45 MCQs on Complexes I-IV & PFK-1 Regulation",
      "Chemical Equilibrium & Le Chatelier Shifts" to "Solve 40 past paper numericals on Kp, Kc, and inert gases",
      "Mechanics, Projectiles & Gravitational PE" to "Master formula derivations and complementary angles (35 MCQs)",
      "Enzyme Kinetics & Lineweaver-Burk Analysis" to "Analyze competitive vs non-competitive inhibitor plots",
      "Organic Carbonyls & Distinction Reagents" to "Practice Iodoform, Tollens, and Fehling test identification drills",
      "Subject-Verb Concord & High-Frequency Roots" to "Solve 30 error-detection questions focusing on parenthetical phrases",
      "Grand Syllogism & Critical Logic Drills" to "Solve 25 Venn-diagram logic puzzles under strict 15-min countdown"
    )

    for (i in 0 until 7) {
      val (topic, _) = topics[i]
      schedules.add(
        StudyDaySchedule(
          dayNumber = i + 1,
          dayLabel = "Day ${i + 1}: ${topic.split("&")[0].trim()}",
          focusSubject = subjectsOrder[i],
          topicTitle = topic,
          targetMcqsCount = 30 + (i * 3),
          plannedHours = dailyHours,
          isDone = i == 0 // mark first as active
        )
      )
    }

    val notes = """
      Personalized Strategy by Dr salar:
      • Weak Areas Identified: ${weakAreas.joinToString(" • ")}.
      • To reach your target of $targetScore/180, spend 60% of your daily $dailyHours hours on active MCQ recall rather than passive rereading.
      • Re-test weak subtopics within 48 hours to secure long-term retention.
    """.trimIndent()

    return AIStudyPlan(
      id = "plan_${System.currentTimeMillis()}",
      studentTargetScore = targetScore,
      weakAreas = weakAreas,
      dailyTargetHours = dailyHours,
      generatedTimestamp = System.currentTimeMillis(),
      scheduleDays = schedules,
      aiRecommendationNotes = notes
    )
  }
}
