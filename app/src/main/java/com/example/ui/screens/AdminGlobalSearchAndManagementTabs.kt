package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicClass
import com.example.data.model.CourseSubscription
import com.example.data.model.LiveSession
import com.example.data.model.LiveSessionStatus
import com.example.data.model.MDCATSubject
import com.example.data.model.PdfResource
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyMaterial
import com.example.data.model.StudyNoteDraft
import com.example.ui.viewmodel.MDCATViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global Search Bar in Admin Dashboard
 */
@Composable
fun AdminGlobalSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = modifier
      .fillMaxWidth()
      .testTag("input_admin_global_search"),
    placeholder = {
      Text(
        text = "Search study materials, MCQs, PDFs, courses, drafts...",
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    leadingIcon = {
      Icon(
        Icons.Default.Search,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.primary
      )
    },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = onClear) {
          Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp)
  )
}

enum class SearchCategory(val label: String) {
  ALL("All"),
  NOTES("Study Notes"),
  MCQS("MCQs"),
  PDFS("PDF Resources"),
  DRAFTS("Drafts"),
  LIVE("Live Sessions"),
  COURSES("Courses")
}

data class GlobalSearchResultItem(
  val id: String,
  val category: SearchCategory,
  val title: String,
  val subtitle: String,
  val snippet: String,
  val subject: MDCATSubject?,
  val academicClass: AcademicClass?,
  val originalObj: Any
)

/**
 * Global Search Results View
 */
@Composable
fun AdminGlobalSearchResultsView(
  query: String,
  materials: List<StudyMaterial>,
  questions: List<QuizQuestion>,
  pdfs: List<PdfResource>,
  drafts: List<StudyNoteDraft>,
  liveSessions: List<LiveSession>,
  courses: List<CourseSubscription>,
  isEditable: Boolean,
  onDeleteMaterial: (String) -> Unit,
  onDeleteQuestion: (String) -> Unit,
  onDeletePdf: (String) -> Unit,
  onDeleteDraft: (String) -> Unit,
  onResumeDraft: (StudyNoteDraft) -> Unit,
  onClearSearch: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }
  var selectedSubjectFilter by remember { mutableStateOf<MDCATSubject?>(null) }
  var previewItem by remember { mutableStateOf<GlobalSearchResultItem?>(null) }

  val cleanQuery = query.trim().lowercase()

  // Collect all searchable items
  val allResults = remember(query, materials, questions, pdfs, drafts, liveSessions, courses) {
    if (cleanQuery.isBlank()) emptyList()
    else {
      val list = mutableListOf<GlobalSearchResultItem>()

      // 1. Study Materials
      materials.forEach { mat ->
        val matchesTitle = mat.title.lowercase().contains(cleanQuery)
        val matchesChapter = mat.chapter.lowercase().contains(cleanQuery)
        val matchesContent = mat.contentMarkdown.lowercase().contains(cleanQuery)
        val matchesConcepts = mat.keyConcepts.any { it.lowercase().contains(cleanQuery) }
        val matchesSubject = mat.subject.displayName.lowercase().contains(cleanQuery)

        if (matchesTitle || matchesChapter || matchesContent || matchesConcepts || matchesSubject) {
          list.add(
            GlobalSearchResultItem(
              id = mat.id,
              category = SearchCategory.NOTES,
              title = mat.title,
              subtitle = "${mat.subject.displayName} • ${mat.chapter} • ${mat.readTimeMin}m read",
              snippet = mat.keyConcepts.firstOrNull() ?: mat.contentMarkdown.take(120),
              subject = mat.subject,
              academicClass = mat.academicClass,
              originalObj = mat
            )
          )
        }
      }

      // 2. MCQs
      questions.forEach { q ->
        val matchesText = q.questionText.lowercase().contains(cleanQuery)
        val matchesTopic = q.topic.lowercase().contains(cleanQuery)
        val matchesOptions = q.options.any { it.lowercase().contains(cleanQuery) }
        val matchesExplanation = q.explanation.lowercase().contains(cleanQuery)
        val matchesSubject = q.subject.displayName.lowercase().contains(cleanQuery)

        if (matchesText || matchesTopic || matchesOptions || matchesExplanation || matchesSubject) {
          list.add(
            GlobalSearchResultItem(
              id = q.id,
              category = SearchCategory.MCQS,
              title = q.questionText,
              subtitle = "${q.subject.displayName} • ${q.topic}",
              snippet = "Answer: ${q.options.getOrNull(q.correctOptionIndex) ?: ""} • ${q.explanation}",
              subject = q.subject,
              academicClass = q.academicClass,
              originalObj = q
            )
          )
        }
      }

      // 3. PDF Resources
      pdfs.forEach { pdf ->
        val matchesTitle = pdf.title.lowercase().contains(cleanQuery)
        val matchesChapter = pdf.chapter.lowercase().contains(cleanQuery)
        val matchesDesc = pdf.description.lowercase().contains(cleanQuery)
        val matchesSubject = pdf.subject.displayName.lowercase().contains(cleanQuery)

        if (matchesTitle || matchesChapter || matchesDesc || matchesSubject) {
          list.add(
            GlobalSearchResultItem(
              id = pdf.id,
              category = SearchCategory.PDFS,
              title = pdf.title,
              subtitle = "${pdf.subject.displayName} • ${pdf.chapter} • ${pdf.fileSizeMb} MB",
              snippet = pdf.description.ifBlank { "Handbook: ${pdf.pdfUrlOrPath}" },
              subject = pdf.subject,
              academicClass = pdf.academicClass,
              originalObj = pdf
            )
          )
        }
      }

      // 4. Drafts
      drafts.forEach { d ->
        val matchesTitle = d.title.lowercase().contains(cleanQuery)
        val matchesChapter = d.chapter.lowercase().contains(cleanQuery)
        val matchesContent = d.contentMarkdown.lowercase().contains(cleanQuery)

        if (matchesTitle || matchesChapter || matchesContent) {
          list.add(
            GlobalSearchResultItem(
              id = d.id,
              category = SearchCategory.DRAFTS,
              title = "[Draft] ${d.title}",
              subtitle = "${d.subject.displayName} • ${d.chapter} (In Progress)",
              snippet = d.keyConceptsRaw.take(120),
              subject = d.subject,
              academicClass = d.academicClass,
              originalObj = d
            )
          )
        }
      }

      // 5. Live Sessions
      liveSessions.forEach { live ->
        val matchesTitle = live.title.lowercase().contains(cleanQuery)
        val matchesAgenda = live.agenda.lowercase().contains(cleanQuery)

        if (matchesTitle || matchesAgenda) {
          list.add(
            GlobalSearchResultItem(
              id = live.id,
              category = SearchCategory.LIVE,
              title = live.title,
              subtitle = "${live.subject.displayName} • ${live.status.name} • ${live.attendeesCount} attendees",
              snippet = live.agenda.take(120),
              subject = live.subject,
              academicClass = live.academicClass,
              originalObj = live
            )
          )
        }
      }

      // 6. Courses
      courses.forEach { c ->
        val matchesTitle = c.title.lowercase().contains(cleanQuery)
        val matchesFocus = c.subjectFocus.lowercase().contains(cleanQuery)
        val matchesDesc = c.description.lowercase().contains(cleanQuery)

        if (matchesTitle || matchesFocus || matchesDesc) {
          list.add(
            GlobalSearchResultItem(
              id = c.courseId,
              category = SearchCategory.COURSES,
              title = c.title,
              subtitle = "Course • PKR ${c.pricePkr} • ${c.durationWeeks} Weeks",
              snippet = c.description.take(120),
              subject = null,
              academicClass = c.targetClass,
              originalObj = c
            )
          )
        }
      }

      list
    }
  }

  // Filter by category & subject
  val filteredResults = remember(allResults, selectedCategory, selectedSubjectFilter) {
    allResults.filter { item ->
      (selectedCategory == SearchCategory.ALL || item.category == selectedCategory) &&
        (selectedSubjectFilter == null || item.subject == selectedSubjectFilter)
    }
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    // Header summary & Clear button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Search Results for \"$query\" (${filteredResults.size} matches)",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      TextButton(onClick = onClearSearch) {
        Text("Exit Search", fontSize = 12.sp)
      }
    }

    // Category Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      SearchCategory.values().forEach { cat ->
        val count = if (cat == SearchCategory.ALL) allResults.size else allResults.count { it.category == cat }
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          label = { Text("${cat.label} ($count)", fontSize = 11.sp) }
        )
      }
    }

    // Subject Filter Chips
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilterChip(
        selected = selectedSubjectFilter == null,
        onClick = { selectedSubjectFilter = null },
        label = { Text("All Subjects", fontSize = 10.sp) }
      )
      MDCATSubject.values().forEach { sub ->
        FilterChip(
          selected = selectedSubjectFilter == sub,
          onClick = { selectedSubjectFilter = sub },
          label = { Text(sub.displayName, fontSize = 10.sp) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (filteredResults.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text("No matching materials found for \"$query\"", fontWeight = FontWeight.Bold)
          Text("Try searching for topics like 'Enzymes', 'Glycolysis', 'Stoichiometry' or '11th'", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredResults, key = { "${it.category.name}_${it.id}" }) { item ->
          GlobalSearchResultCard(
            item = item,
            isEditable = isEditable,
            onPreview = { previewItem = item },
            onResumeDraft = {
              if (item.originalObj is StudyNoteDraft) {
                onResumeDraft(item.originalObj)
              }
            },
            onDelete = {
              when (item.category) {
                SearchCategory.NOTES -> onDeleteMaterial(item.id)
                SearchCategory.MCQS -> onDeleteQuestion(item.id)
                SearchCategory.PDFS -> onDeletePdf(item.id)
                SearchCategory.DRAFTS -> onDeleteDraft(item.id)
                else -> {}
              }
            }
          )
        }
      }
    }
  }

  // Preview Dialog
  if (previewItem != null) {
    SearchResultPreviewDialog(
      item = previewItem!!,
      onDismiss = { previewItem = null }
    )
  }
}

@Composable
fun GlobalSearchResultCard(
  item: GlobalSearchResultItem,
  isEditable: Boolean,
  onPreview: () -> Unit,
  onResumeDraft: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder(),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onPreview() }
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Category Badge
          val catColor = when (item.category) {
            SearchCategory.NOTES -> MaterialTheme.colorScheme.primary
            SearchCategory.MCQS -> Color(0xFF8E24AA)
            SearchCategory.PDFS -> Color(0xFFD93025)
            SearchCategory.DRAFTS -> Color(0xFFF9AB00)
            SearchCategory.LIVE -> Color(0xFFE53935)
            SearchCategory.COURSES -> Color(0xFF1E88E5)
            else -> MaterialTheme.colorScheme.secondary
          }

          Surface(
            color = catColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = item.category.label,
              color = catColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (item.subject != null) {
            Surface(
              color = item.subject.color.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = item.subject.displayName,
                color = item.subject.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          if (item.academicClass != null) {
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = item.academicClass.shortName,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onPreview, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          }

          if (item.category == SearchCategory.DRAFTS) {
            IconButton(onClick = onResumeDraft, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Resume Draft", tint = Color(0xFFF9AB00), modifier = Modifier.size(16.dp))
            }
          }

          if (isEditable && (item.category == SearchCategory.NOTES || item.category == SearchCategory.MCQS || item.category == SearchCategory.PDFS || item.category == SearchCategory.DRAFTS)) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = item.title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = item.subtitle,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
      )

      if (item.snippet.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = item.snippet,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(6.dp)
          )
        }
      }
    }
  }
}

@Composable
fun SearchResultPreviewDialog(
  item: GlobalSearchResultItem,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = item.category.label,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          if (item.academicClass != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(item.academicClass.shortName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(item.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        HorizontalDivider()

        when (val obj = item.originalObj) {
          is StudyMaterial -> {
            Text("Key Concepts:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            obj.keyConcepts.forEach { concept ->
              Text("• $concept", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Content Preview:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(obj.contentMarkdown, fontSize = 11.sp, lineHeight = 16.sp)
          }
          is QuizQuestion -> {
            Text("Question Options:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            obj.options.forEachIndexed { idx, opt ->
              val isCorrect = idx == obj.correctOptionIndex
              Text(
                text = "${('A' + idx)}. $opt ${if (isCorrect) "✓ (Correct)" else ""}",
                fontSize = 11.sp,
                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                color = if (isCorrect) Color(0xFF34A853) else MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Explanation: ${obj.explanation}", fontSize = 11.sp)
          }
          is PdfResource -> {
            Text("File Size: ${obj.fileSizeMb} MB", fontSize = 12.sp)
            Text("PDF URL / Path: ${obj.pdfUrlOrPath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Text("Description: ${obj.description}", fontSize = 11.sp)
          }
          is LiveSession -> {
            Text("Instructor: ${obj.instructorName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Google Meet: ${obj.meetingUrl}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Text("Agenda:\n${obj.agenda}", fontSize = 11.sp)
          }
          is CourseSubscription -> {
            Text("Price: PKR ${obj.pricePkr} (${obj.durationWeeks} Weeks)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(obj.description, fontSize = 11.sp)
            Text("Included Features:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            obj.features.forEach { f -> Text("• $f", fontSize = 11.sp) }
          }
          else -> {
            Text(item.snippet, fontSize = 11.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Close")
      }
    }
  )
}

/**
 * Live Sessions Management Tab for Admin Dashboard
 */
@Composable
fun ManageLiveSessionsTab(
  liveSessions: List<LiveSession>,
  defaultClass: AcademicClass,
  isEditable: Boolean,
  onAddLiveSession: (title: String, subject: MDCATSubject, academicClass: AcademicClass, durationMin: Int, meetingUrl: String, agenda: String, status: LiveSessionStatus) -> Unit,
  onUpdateStatus: (sessionId: String, status: LiveSessionStatus) -> Unit,
  onDeleteSession: (sessionId: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf(MDCATSubject.BIOLOGY) }
  var targetClass by remember { mutableStateOf(defaultClass) }
  var durationMin by remember { mutableIntStateOf(60) }
  var meetingUrl by remember { mutableStateOf("https://meet.google.com/mdcat-salar-live") }
  var agenda by remember { mutableStateOf("") }
  var status by remember { mutableStateOf(LiveSessionStatus.UPCOMING) }
  var formError by remember { mutableStateOf<String?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Schedule / Launch Live Session", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("Dr. Salar Live", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Session Title") },
            placeholder = { Text("e.g. Bioenergetics & Respiration High-Yield Shortcuts") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_live_title"),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Subject Chips
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

          // Academic Class selection
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = targetClass == AcademicClass.CLASS_11,
              onClick = { targetClass = AcademicClass.CLASS_11 },
              label = { Text("11th Class", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = targetClass == AcademicClass.CLASS_12,
              onClick = { targetClass = AcademicClass.CLASS_12 },
              label = { Text("12th Class", fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = meetingUrl,
            onValueChange = { meetingUrl = it },
            label = { Text("Google Meet / Stream URL") },
            placeholder = { Text("https://meet.google.com/...") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_live_meet_url"),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = agenda,
            onValueChange = { agenda = it },
            label = { Text("High-Yield Agenda Topics (1 per line)") },
            placeholder = { Text("1. Regulatory enzymes\n2. Krebs cycle traps\n3. 25 past questions") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_live_agenda"),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Status toggle: Upcoming vs LIVE NOW
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Initial Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FilterChip(
              selected = status == LiveSessionStatus.UPCOMING,
              onClick = { status = LiveSessionStatus.UPCOMING },
              label = { Text("Upcoming", fontSize = 10.sp) }
            )
            FilterChip(
              selected = status == LiveSessionStatus.LIVE_NOW,
              onClick = { status = LiveSessionStatus.LIVE_NOW },
              label = { Text("● GO LIVE NOW", fontSize = 10.sp, color = Color(0xFFD93025)) }
            )
          }

          if (formError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(formError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              if (title.isBlank()) {
                formError = "Please enter a session title."
                return@Button
              }
              onAddLiveSession(
                title,
                subject,
                targetClass,
                durationMin,
                meetingUrl,
                agenda.ifBlank { "High-yield concept review and student Q&A with Dr. Salar." },
                status
              )
              title = ""
              agenda = ""
              formError = null
            },
            enabled = isEditable,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_publish_live_session"),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publish Live Session to Students")
          }
        }
      }
    }

    item {
      Text(
        text = "Active & Scheduled Sessions (${liveSessions.size})",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
    }

    items(liveSessions, key = { it.id }) { session ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (session.status == LiveSessionStatus.LIVE_NOW) {
                Surface(color = Color(0xFFD93025), shape = RoundedCornerShape(6.dp)) {
                  Text("● LIVE NOW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                  Text(session.status.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              }
              Surface(color = session.subject.color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                Text(session.subject.displayName, color = session.subject.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                Text(session.academicClass.shortName, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }

            if (isEditable) {
              IconButton(onClick = { onDeleteSession(session.id) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(session.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("Link: ${session.meetingUrl}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
          Text("${session.attendeesCount} enrolled students attending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (session.status != LiveSessionStatus.LIVE_NOW) {
              Button(
                onClick = { onUpdateStatus(session.id, LiveSessionStatus.LIVE_NOW) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93025)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Live Now", fontSize = 11.sp)
              }
            } else {
              OutlinedButton(
                onClick = { onUpdateStatus(session.id, LiveSessionStatus.RECORDED) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("End Session", fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Course Subscriptions Management Tab for Admin Dashboard
 */
@Composable
fun ManageCourseSubscriptionsTab(
  courses: List<CourseSubscription>,
  isEditable: Boolean,
  onAddCourse: (title: String, subjectFocus: String, targetClass: AcademicClass?, pricePkr: Int, durationWeeks: Int, description: String, features: List<String>) -> Unit
) {
  var showAddDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Course Subscriptions & Bundles", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Total Active Courses: ${courses.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Button(
        onClick = { showAddDialog = true },
        enabled = isEditable,
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("New Course", fontSize = 12.sp)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(courses, key = { it.courseId }) { course ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = Color(0xFFF9AB00).copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = course.badge,
                  color = Color(0xFFE37400),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }

              Text(
                text = "PKR ${course.pricePkr}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(course.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(course.subjectFocus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Text("${course.totalSubscribers} Students Currently Enrolled", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(course.description, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }

  if (showAddDialog) {
    var title by remember { mutableStateOf("") }
    var subjectFocus by remember { mutableStateOf("") }
    var targetClass by remember { mutableStateOf<AcademicClass?>(AcademicClass.CLASS_11) }
    var pricePkr by remember { mutableStateOf("4500") }
    var durationWeeks by remember { mutableStateOf("12") }
    var description by remember { mutableStateOf("") }
    var featuresRaw by remember { mutableStateOf("Full syllabus lectures\nWeekly live webinars\nChapter-wise MCQs") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Add Course Offering", fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Course Title") },
            placeholder = { Text("e.g. Physics High-Yield Booster") },
            shape = RoundedCornerShape(8.dp)
          )
          OutlinedTextField(
            value = subjectFocus,
            onValueChange = { subjectFocus = it },
            label = { Text("Subject Focus") },
            placeholder = { Text("e.g. Physics & Mechanics") },
            shape = RoundedCornerShape(8.dp)
          )
          OutlinedTextField(
            value = pricePkr,
            onValueChange = { pricePkr = it },
            label = { Text("Price (PKR)") },
            shape = RoundedCornerShape(8.dp)
          )
          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            shape = RoundedCornerShape(8.dp)
          )
          OutlinedTextField(
            value = featuresRaw,
            onValueChange = { featuresRaw = it },
            label = { Text("Features (1 per line)") },
            shape = RoundedCornerShape(8.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank()) {
              onAddCourse(
                title,
                subjectFocus.ifBlank { "MDCAT Core" },
                targetClass,
                pricePkr.toIntOrNull() ?: 3500,
                durationWeeks.toIntOrNull() ?: 8,
                description.ifBlank { "Comprehensive course bundle by Dr. Salar." },
                featuresRaw.split("\n").filter { it.isNotBlank() }
              )
              showAddDialog = false
            }
          },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Add Course")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
      }
    )
  }
}
