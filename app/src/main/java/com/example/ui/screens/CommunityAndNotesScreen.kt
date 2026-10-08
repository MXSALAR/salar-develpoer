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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.model.EncryptedNote
import com.example.data.model.ForumPost
import com.example.data.model.MDCATSubject
import com.example.data.model.StudyGroup
import com.example.ui.theme.BioGreen
import com.example.ui.viewmodel.MDCATViewModel

@Composable
fun CommunityAndNotesScreen(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  var selectedMainTab by remember { mutableStateOf(0) } // 0: Forum & Groups, 1: Encrypted Notes Vault
  val editingNote by viewModel.editingNote.collectAsState()

  Scaffold(
    floatingActionButton = {
      if (selectedMainTab == 1) {
        FloatingActionButton(
          onClick = { viewModel.startEditingNote(null) },
          containerColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("fab_add_encrypted_note")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add Note")
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      TabRow(
        selectedTabIndex = selectedMainTab,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
      ) {
        Tab(
          selected = selectedMainTab == 0,
          onClick = { selectedMainTab = 0 },
          text = { Text("Community Forum & Groups", fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedMainTab == 1,
          onClick = { selectedMainTab = 1 },
          text = { Text("Encrypted Notes Vault", fontWeight = FontWeight.Bold) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (selectedMainTab == 0) {
        CommunityForumAndGroupsView(viewModel = viewModel)
      } else {
        EncryptedNotesVaultView(viewModel = viewModel)
      }
    }
  }

  // Note Editor Dialog
  if (editingNote != null) {
    NoteEditorDialog(
      note = editingNote!!,
      onDismiss = { viewModel.closeNoteEditor() },
      onSave = { title, content, subject ->
        viewModel.saveEditingNote(title, content, subject)
      },
      onDelete = {
        viewModel.deleteNote(editingNote!!.id)
        viewModel.closeNoteEditor()
      }
    )
  }
}

@Composable
fun CommunityForumAndGroupsView(viewModel: MDCATViewModel) {
  var subViewIndex by remember { mutableStateOf(0) } // 0: Doubts, 1: Study Groups
  val posts by viewModel.forumPosts.collectAsState()
  val groups by viewModel.studyGroups.collectAsState()

  var showAskDoubtDialog by remember { mutableStateOf(false) }
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var answeringPostId by remember { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { subViewIndex = 0 },
          shape = RoundedCornerShape(8.dp),
          colors = if (subViewIndex == 0) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
        ) {
          Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Peer Doubts", fontSize = 12.sp)
        }

        OutlinedButton(
          onClick = { subViewIndex = 1 },
          shape = RoundedCornerShape(8.dp),
          colors = if (subViewIndex == 1) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
        ) {
          Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Study Groups", fontSize = 12.sp)
        }
      }

      Button(
        onClick = {
          if (subViewIndex == 0) showAskDoubtDialog = true else showCreateGroupDialog = true
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("btn_post_community")
      ) {
        Text(if (subViewIndex == 0) "Ask Doubt" else "New Group", fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (subViewIndex == 0) {
      // Forum Posts List
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(posts, key = { it.id }) { post ->
          ForumPostCard(
            post = post,
            onUpvote = { viewModel.toggleUpvote(post.id) },
            onReply = { answeringPostId = post.id }
          )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
      }
    } else {
      // Study Groups List
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(groups, key = { it.id }) { group ->
          StudyGroupCard(group = group)
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
      }
    }
  }

  // Ask Doubt Dialog
  if (showAskDoubtDialog) {
    AskDoubtDialog(
      onDismiss = { showAskDoubtDialog = false },
      onSubmit = { title, content, subject ->
        viewModel.postQuestion(title, content, subject, "You (MDCAT Aspirant)")
        showAskDoubtDialog = false
      }
    )
  }

  // Create Group Dialog
  if (showCreateGroupDialog) {
    CreateGroupDialog(
      onDismiss = { showCreateGroupDialog = false },
      onSubmit = { name, subject, desc ->
        viewModel.createStudyGroup(name, subject, desc)
        showCreateGroupDialog = false
      }
    )
  }

  // Answer Doubt Dialog
  if (answeringPostId != null) {
    AnswerDoubtDialog(
      onDismiss = { answeringPostId = null },
      onSubmit = { answerText ->
        viewModel.answerQuestion(answeringPostId!!, answerText, "You (MDCAT Peer)")
        answeringPostId = null
      }
    )
  }
}

@Composable
fun ForumPostCard(
  post: ForumPost,
  onUpvote: () -> Unit,
  onReply: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_forum_post_${post.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = post.subject.color.copy(alpha = 0.15f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = post.subject.displayName,
            color = post.subject.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }

        Text(
          text = post.authorName,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = post.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = post.content,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        modifier = Modifier.padding(vertical = 4.dp)
      )

      // Verified Educator Answer if present
      val verifiedAns = post.answers.firstOrNull { it.isVerifiedEducator }
      if (verifiedAns != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${verifiedAns.authorName} (${verifiedAns.authorTag})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = verifiedAns.content,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .clickable(onClick = onUpvote)
            .padding(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.ThumbUp,
            contentDescription = "Upvote",
            tint = if (post.isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.upvotes}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onReply,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Text("Reply / Answer", fontSize = 11.sp)
        }
      }
    }
  }
}

@Composable
fun StudyGroupCard(group: StudyGroup) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = group.name,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          modifier = Modifier.weight(1f)
        )
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${group.memberCount} Members",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = group.description,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "💬 ${group.recentDiscussion}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
fun EncryptedNotesVaultView(viewModel: MDCATViewModel) {
  val notes by viewModel.encryptedNotes.collectAsState()
  val isUnlocked by viewModel.isNotesVaultUnlocked.collectAsState()

  Column(modifier = Modifier.fillMaxSize()) {
    // E2E Privacy Banner
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "End-to-End Encrypted Study Vault",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "Private student notes secured with AES-256 local encryption.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
          )
        }

        IconButton(onClick = { viewModel.toggleNotesVaultLock() }) {
          Icon(
            if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = "Lock Toggle",
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (!isUnlocked) {
      // Locked Screen State
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Vault Locked for Privacy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("Tap the unlock icon to decrypt your study notes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(12.dp))
          Button(onClick = { viewModel.toggleNotesVaultLock() }) {
            Text("Unlock Vault")
          }
        }
      }
    } else {
      // Notes List
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(notes, key = { it.id }) { note ->
          EncryptedNoteCard(
            note = note,
            onEdit = { viewModel.startEditingNote(note) },
            onDelete = { viewModel.deleteNote(note.id) }
          )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
      }
    }
  }
}

@Composable
fun EncryptedNoteCard(
  note: EncryptedNote,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onEdit)
      .testTag("card_note_${note.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (note.isPinned) {
            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(note.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Surface(
          color = note.subject.color.copy(alpha = 0.15f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = note.subject.displayName,
            color = note.subject.color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = note.rawContent,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        maxLines = 3,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "🔒 ${note.encryptionAlgorithm}",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
          }
        }
      }
    }
  }
}

@Composable
fun NoteEditorDialog(
  note: EncryptedNote,
  onDismiss: () -> Unit,
  onSave: (String, String, MDCATSubject) -> Unit,
  onDelete: () -> Unit
) {
  var title by remember { mutableStateOf(note.title) }
  var content by remember { mutableStateOf(note.rawContent) }
  var selectedSubject by remember { mutableStateOf(note.subject) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (note.title.isBlank()) "New Encrypted Note" else "Edit Note", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Note Title") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Encrypted Content (Formulas, Mnemonics)") },
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          shape = RoundedCornerShape(8.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, content, selectedSubject) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save Encrypted")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun AskDoubtDialog(
  onDismiss: () -> Unit,
  onSubmit: (String, String, MDCATSubject) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf(MDCATSubject.BIOLOGY) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Post Doubt to Dr salar & Peers", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Brief Question Title") },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Detailed Explanation / Problem statement") },
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (title.isNotBlank()) onSubmit(title, content, subject) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Post Doubt")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun CreateGroupDialog(
  onDismiss: () -> Unit,
  onSubmit: (String, MDCATSubject, String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create MDCAT Study Group", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Group Name (e.g. Physics Masters)") },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text("Description & Goals") },
          modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onSubmit(name, MDCATSubject.BIOLOGY, desc) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Create Group")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun AnswerDoubtDialog(
  onDismiss: () -> Unit,
  onSubmit: (String) -> Unit
) {
  var answerText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Post Peer Answer", fontWeight = FontWeight.Bold) },
    text = {
      OutlinedTextField(
        value = answerText,
        onValueChange = { answerText = it },
        label = { Text("Write your explanation...") },
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
      )
    },
    confirmButton = {
      Button(
        onClick = { if (answerText.isNotBlank()) onSubmit(answerText) },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Submit Answer")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel")
      }
    }
  )
}
