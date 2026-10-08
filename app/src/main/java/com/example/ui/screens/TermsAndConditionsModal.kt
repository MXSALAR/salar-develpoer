package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.MDCATViewModel

@Composable
fun TermsAndConditionsModal(
  viewModel: MDCATViewModel,
  onDismiss: () -> Unit = {},
  onViewPrivacyPolicy: () -> Unit = {}
) {
  var isChecked by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { /* Modal must be acted upon before dismissal */ },
    properties = DialogProperties(
      dismissOnBackPress = false,
      dismissOnClickOutside = false
    ),
    modifier = Modifier
      .fillMaxWidth(0.95f)
      .testTag("modal_terms_and_conditions"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Terms & Conditions",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Usage Agreement & Academic Policy",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(350.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Welcoming Notice
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.School,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Welcome to MDCAT Master by Dr. Salar! Please review and accept the academic usage terms before accessing study materials.",
              fontSize = 11.sp,
              lineHeight = 16.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        // Term 1
        TermItemBlock(
          number = "1",
          title = "Authorized Educational Preparation",
          body = "Access to Dr. Salar's 11th & 12th pre-medical syllabus summaries, video lectures, and PMC mock exams is granted exclusively for personal, non-commercial exam preparation."
        )

        // Term 2
        TermItemBlock(
          number = "2",
          title = "Intellectual Property & Anti-Piracy",
          body = "All questions, explanations, mnemonics, diagrams, and video lectures are the proprietary educational property of Dr. Salar. Unauthorized scraping, recording, bulk redistribution, or commercial sale is strictly prohibited."
        )

        // Term 3
        TermItemBlock(
          number = "3",
          title = "Academic Honesty & Integrity",
          body = "Timed practice tests and diagnostic simulators are designed to assess authentic student readiness. Students agree to solve quizzes independently to receive accurate AI remedial study recommendations."
        )

        // Term 4
        TermItemBlock(
          number = "4",
          title = "Community Discussion Guidelines",
          body = "Students must engage respectfully in study groups and discussion forums. Abusive remarks, spamming, or irrelevant advertisements will result in immediate suspension."
        )

        // Term 5
        TermItemBlock(
          number = "5",
          title = "Privacy & Data Storage Acknowledgement",
          body = "Your test results, quiz logs, and encrypted study notes are stored securely using local Room database storage (AES-256 GCM) and Google Firebase Firestore according to our Privacy Policy."
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Privacy Policy Shortcut
        OutlinedButton(
          onClick = onViewPrivacyPolicy,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_view_privacy_from_terms")
        ) {
          Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Read Full Privacy Policy", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Confirmation Checkbox
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { isChecked = !isChecked }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = isChecked,
            onCheckedChange = { isChecked = it },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("checkbox_accept_terms")
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "I have read and agree to the Terms & Conditions and Privacy Policy.",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 15.sp
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          viewModel.acceptTermsAndConditions()
          onDismiss()
        },
        enabled = isChecked,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("btn_confirm_agree_terms")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Agree & Continue", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = {
          viewModel.declineTermsAndConditions()
          onDismiss()
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("btn_decline_terms")
      ) {
        Text("Decline")
      }
    }
  )
}

@Composable
fun TermItemBlock(
  number: String,
  title: String,
  body: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = CircleShape,
      modifier = Modifier.size(20.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = number,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = body,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
