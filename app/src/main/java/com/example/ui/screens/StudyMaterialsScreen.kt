package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.AcademicClass
import com.example.data.model.MDCATSubject
import com.example.data.model.PdfResource
import com.example.data.model.StudyMaterial
import com.example.data.model.VideoLecture
import com.example.ui.viewmodel.MDCATViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialsScreen(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  val studyMaterials by viewModel.studyMaterials.collectAsState()
  val videoLectures by viewModel.videoLectures.collectAsState()
  val pdfResources by viewModel.pdfResources.collectAsState()
  val selectedMaterial by viewModel.selectedStudyMaterial.collectAsState()
  val selectedVideo by viewModel.selectedVideoLecture.collectAsState()
  val isVideoPlaying by viewModel.isVideoPlaying.collectAsState()
  val videoSpeed by viewModel.videoPlaybackSpeed.collectAsState()
  val userAccount by viewModel.userAccount.collectAsState()

  val isStudyAccessUnlocked = userAccount.isLoggedIn && userAccount.email.isNotBlank()

  var selectedSubTab by remember { mutableStateOf(0) } // 0: Notes, 1: Videos, 2: Linked PDFs
  var selectedSubjectFilter by remember { mutableStateOf<MDCATSubject?>(null) }
  var selectedClassFilter by remember { mutableStateOf<AcademicClass?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    // Study Materials Security Gate / Status Banner
    if (!isStudyAccessUnlocked) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp)
          .testTag("study_materials_security_gate"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Lock,
              contentDescription = "Secured",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Study Materials Protected by Firebase Auth",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Sign in with your Gmail/Google account to access Dr. Salar's comprehensive high-yield notes, video lectures, and PDF handbooks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = { viewModel.toggleLoginScreen(true) },
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
              .testTag("btn_login_for_study_materials"),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sign In with Gmail / Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Verified,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Signed in as ${userAccount.displayName.ifBlank { userAccount.email }} (${userAccount.role.label})",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = "Full Access Unlocked",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Sub-Tabs: Notes vs Videos vs Linked PDFs
    TabRow(
      selectedTabIndex = selectedSubTab,
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier.fillMaxWidth()
    ) {
      Tab(
        selected = selectedSubTab == 0,
        onClick = { selectedSubTab = 0 },
        text = { Text("High-Yield Notes", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
      )
      Tab(
        selected = selectedSubTab == 1,
        onClick = { selectedSubTab = 1 },
        text = { Text("Video Lectures", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
      )
      Tab(
        selected = selectedSubTab == 2,
        onClick = { selectedSubTab = 2 },
        text = { Text("PDF Handbooks", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Academic Class Filter Chips (11th vs 12th)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilterChip(
        selected = selectedClassFilter == null,
        onClick = { selectedClassFilter = null },
        label = { Text("All Classes", fontSize = 11.sp) },
        modifier = Modifier.weight(1f)
      )
      FilterChip(
        selected = selectedClassFilter == AcademicClass.CLASS_11,
        onClick = { selectedClassFilter = if (selectedClassFilter == AcademicClass.CLASS_11) null else AcademicClass.CLASS_11 },
        label = { Text("11th Class", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        modifier = Modifier.weight(1f)
      )
      FilterChip(
        selected = selectedClassFilter == AcademicClass.CLASS_12,
        onClick = { selectedClassFilter = if (selectedClassFilter == AcademicClass.CLASS_12) null else AcademicClass.CLASS_12 },
        label = { Text("12th Class", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Subject Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = selectedSubjectFilter == null,
        onClick = { selectedSubjectFilter = null },
        label = { Text("All Subjects") }
      )
      MDCATSubject.values().forEach { subject ->
        FilterChip(
          selected = selectedSubjectFilter == subject,
          onClick = { selectedSubjectFilter = if (selectedSubjectFilter == subject) null else subject },
          label = { Text(subject.displayName) }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (selectedSubTab) {
      0 -> {
        // Notes List
        val filteredNotes = studyMaterials.filter {
          (selectedSubjectFilter == null || it.subject == selectedSubjectFilter) &&
          (selectedClassFilter == null || it.academicClass == selectedClassFilter)
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredNotes, key = { it.id }) { material ->
            StudyMaterialCard(
              material = material,
              onOpen = {
                if (!isStudyAccessUnlocked) {
                  viewModel.toggleLoginScreen(true)
                } else {
                  viewModel.openStudyMaterial(material)
                }
              },
              onToggleOffline = { viewModel.toggleDownloadMaterial(material.id) }
            )
          }
          item { Spacer(modifier = Modifier.height(24.dp)) }
        }
      }
      1 -> {
        // Video Lectures List
        val filteredVideos = videoLectures.filter {
          selectedSubjectFilter == null || it.subject == selectedSubjectFilter
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredVideos, key = { it.id }) { video ->
            VideoLectureCard(
              video = video,
              onPlay = {
                if (!isStudyAccessUnlocked) {
                  viewModel.toggleLoginScreen(true)
                } else {
                  viewModel.openVideoLecture(video)
                }
              },
              onToggleOffline = { viewModel.toggleDownloadVideo(video.id) }
            )
          }
          item { Spacer(modifier = Modifier.height(24.dp)) }
        }
      }
      2 -> {
        // Linked PDF Resources List
        val filteredPdfs = pdfResources.filter {
          (selectedSubjectFilter == null || it.subject == selectedSubjectFilter) &&
          (selectedClassFilter == null || it.academicClass == selectedClassFilter)
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Official PDF Handbooks & Solved Past Papers by Dr. Salar",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          items(filteredPdfs, key = { it.id }) { pdf ->
            PdfResourceItemCard(
              pdf = pdf,
              onDownloadClick = {
                if (!isStudyAccessUnlocked) {
                  viewModel.toggleLoginScreen(true)
                }
              }
            )
          }
          item { Spacer(modifier = Modifier.height(24.dp)) }
        }
      }
    }
  }

  // Study Material Reader BottomSheet
  if (selectedMaterial != null) {
    ModalBottomSheet(
      onDismissRequest = { viewModel.closeStudyMaterial() },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
      StudyMaterialReaderSheet(
        material = selectedMaterial!!,
        onClose = { viewModel.closeStudyMaterial() },
        onToggleOffline = { viewModel.toggleDownloadMaterial(selectedMaterial!!.id) }
      )
    }
  }

  // Video Lecture Player BottomSheet
  if (selectedVideo != null) {
    ModalBottomSheet(
      onDismissRequest = { viewModel.closeVideoLecture() },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
      VideoPlayerModalSheet(
        video = selectedVideo!!,
        isPlaying = isVideoPlaying,
        speed = videoSpeed,
        onTogglePlay = { viewModel.toggleVideoPlay() },
        onSetSpeed = { viewModel.setVideoSpeed(it) },
        onToggleOffline = { viewModel.toggleDownloadVideo(selectedVideo!!.id) },
        onClose = { viewModel.closeVideoLecture() }
      )
    }
  }
}

@Composable
fun StudyMaterialCard(
  material: StudyMaterial,
  onOpen: () -> Unit,
  onToggleOffline: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen)
      .testTag("card_study_material_${material.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = material.subject.color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = material.subject.displayName,
              color = material.subject.color,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = material.academicClass.shortName,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (material.isDownloadedOffline) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.DownloadDone,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Offline Ready",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
          IconButton(onClick = onToggleOffline) {
            Icon(
              if (material.isDownloadedOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
              contentDescription = "Offline download",
              tint = if (material.isDownloadedOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = material.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "${material.chapter} • ${material.readTimeMin} min read",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
      )

      // Key concepts bullets preview
      material.keyConcepts.take(2).forEach { concept ->
        Text(
          text = "• $concept",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
          maxLines = 1
        )
      }
    }
  }
}

@Composable
fun VideoLectureCard(
  video: VideoLecture,
  onPlay: () -> Unit,
  onToggleOffline: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onPlay)
      .testTag("card_video_${video.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = video.subject.color.copy(alpha = 0.15f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = video.subject.displayName,
            color = video.subject.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        IconButton(onClick = onToggleOffline) {
          Icon(
            if (video.isDownloadedOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
            contentDescription = "Offline download",
            tint = if (video.isDownloadedOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = video.title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = "${video.educatorName} • ${video.durationMinutes} min",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun StudyMaterialReaderSheet(
  material: StudyMaterial,
  onClose: () -> Unit,
  onToggleOffline: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = material.subject.color.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = material.subject.displayName,
            color = material.subject.color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        Row {
          IconButton(onClick = onToggleOffline) {
            Icon(
              if (material.isDownloadedOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
              contentDescription = "Download",
              tint = if (material.isDownloadedOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = material.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = "${material.chapter} • ${material.readTimeMin} min read",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Mnemonics Card
      if (material.mnemonics.isNotEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Dr salar's High-Yield Mnemonics",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            material.mnemonics.forEach { m ->
              Text(
                text = "💡 $m",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Formulas Box if present
      if (material.formulas.isNotEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              "High-Frequency Formulae & Values",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            material.formulas.forEach { f ->
              Text(
                text = "⚡ $f",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Markdown Full Content
      Text(
        text = material.contentMarkdown,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
fun VideoPlayerModalSheet(
  video: VideoLecture,
  isPlaying: Boolean,
  speed: Float,
  onTogglePlay: () -> Unit,
  onSetSpeed: (Float) -> Unit,
  onToggleOffline: () -> Unit,
  onClose: () -> Unit
) {
  var progress by remember { mutableStateOf(0.35f) }

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = video.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Video Mock Screen Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.Black),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.25f))
          ) {
            Icon(
              if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Play/Pause",
              tint = Color.White,
              modifier = Modifier.size(36.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (isPlaying) "Playing: ${video.topic}" else "Paused • Tap to resume",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
          )
        }

        // Speed badge in player corner
        Surface(
          color = Color.Black.copy(alpha = 0.6f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
        ) {
          Text(
            text = "${speed}x",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scrubber
      Slider(
        value = progress,
        onValueChange = { progress = it },
        modifier = Modifier.fillMaxWidth()
      )

      // Speed selectors & Offline button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
            AssistChip(
              onClick = { onSetSpeed(s) },
              label = { Text("${s}x", fontSize = 11.sp) }
            )
          }
        }

        OutlinedButton(onClick = onToggleOffline) {
          Icon(
            if (video.isDownloadedOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (video.isDownloadedOffline) "Downloaded" else "Save Offline", fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Key Lecture Timestamps",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))

      video.keyTimestamps.forEach { (time, label) ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Jump to timestamp */ }
            .padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = time,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = label, fontSize = 13.sp)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Summary Notes",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = video.summaryNotes,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
fun PdfResourceItemCard(
  pdf: PdfResource,
  onDownloadClick: () -> Unit = {}
) {
  var isDownloaded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_pdf_${pdf.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = pdf.subject.color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = pdf.subject.displayName,
              color = pdf.subject.color,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = pdf.academicClass.shortName,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${pdf.fileSizeMb} MB PDF",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = pdf.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "Curated by ${pdf.uploadedBy} • ${pdf.chapter}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (pdf.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = pdf.description,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Verified,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Verified Syllabus Material",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Button(
          onClick = {
            onDownloadClick()
            isDownloaded = !isDownloaded
          },
          shape = RoundedCornerShape(8.dp),
          colors = if (isDownloaded) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
          } else {
            ButtonDefaults.buttonColors()
          }
        ) {
          Icon(
            if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isDownloaded) "Downloaded Offline" else "Download PDF",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
