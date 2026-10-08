package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.MDCATViewModel

@Composable
fun SettingsDialog(
  viewModel: MDCATViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val themeMode by viewModel.themeMode.collectAsState()
  val gamification by viewModel.gamification.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val lastSync by viewModel.lastSyncTimestamp.collectAsState()
  val studyScheduleAlertEnabled by viewModel.studyScheduleAlertEnabled.collectAsState()

  var targetScoreSlider by remember { mutableFloatStateOf(gamification.targetMDCATScore.coerceIn(120, 180).toFloat()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Settings & Customization",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Theme Mode Selector (Light, Dark, Late-Night OLED)
        Column {
          Text(
            text = "Interface Theme & Eye Comfort",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "Switch to Late-Night OLED to minimize glare during late study hours.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = themeMode == AppThemeMode.LIGHT,
              onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
              label = { Text("Light", fontSize = 11.sp) },
              leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
            FilterChip(
              selected = themeMode == AppThemeMode.DARK,
              onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
              label = { Text("Dark", fontSize = 11.sp) },
              leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
            FilterChip(
              selected = themeMode == AppThemeMode.LATE_NIGHT_OLED,
              onClick = { viewModel.setThemeMode(AppThemeMode.LATE_NIGHT_OLED) },
              label = { Text("OLED Dark", fontSize = 11.sp) },
              leadingIcon = { Icon(Icons.Default.Brightness4, contentDescription = null, modifier = Modifier.size(14.dp)) }
            )
          }
        }

        // Target Score Customization
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("MDCAT Target Score", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${targetScoreSlider.toInt()} / 180", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = targetScoreSlider,
            onValueChange = {
              targetScoreSlider = it
              viewModel.updateTargetScore(it.toInt())
            },
            valueRange = 120f..180f,
            steps = 30,
            modifier = Modifier.fillMaxWidth()
          )
        }

        // Push Notifications & Toast Alerts for AI Study Schedule
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("AI Study Schedule Alert System", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Daily prompts to stick to your personalized AI study plan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = studyScheduleAlertEnabled,
              onCheckedChange = { isEnabled ->
                viewModel.setStudyScheduleAlertEnabled(isEnabled)
                if (isEnabled) {
                  viewModel.triggerStudyScheduleAlert(context, "✅ Daily AI study alert system activated!")
                }
              }
            )
          }
          if (studyScheduleAlertEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
              onClick = {
                viewModel.triggerStudyScheduleAlert(context)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("🔔 Trigger Today's Study Schedule Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Cloud & Cross-Platform Sync
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cross-Platform Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              OutlinedButton(
                onClick = { viewModel.triggerSync() },
                enabled = !isSyncing,
                shape = RoundedCornerShape(8.dp)
              ) {
                if (isSyncing) {
                  CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                  Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Sync", fontSize = 11.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Syncs test scores, encrypted notes & study progress across your Android & web sessions.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Legal & Google Play Compliance Section
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "LEGAL & PLAY STORE COMPLIANCE",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = {
                onDismiss()
                viewModel.togglePrivacyPolicyScreen(true)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_settings_privacy_policy")
            ) {
              Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Privacy Policy & Data Safety", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
              onClick = {
                onDismiss()
                viewModel.toggleTermsModal(true)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_settings_terms_service")
            ) {
              Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Terms & Conditions Agreement", fontSize = 12.sp)
            }
          }
        }

        // Developer Credit Section - Explicitly honoring Dr salar
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("card_developer_credit"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.MedicalServices,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Dr salar",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified Educator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                }
                Text(
                  text = "Lead Medical Educator & Developer",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Crafted with dedication by Dr salar to empower medical aspirants across Pakistan and globally to conquer the MDCAT exam with conceptual clarity, high-yield active recall, and AI-driven precision.",
              fontSize = 11.sp,
              lineHeight = 16.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                onDismiss()
                viewModel.toggleAdminDashboard(true)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("btn_launch_instructor_hub")
            ) {
              Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Manage Materials & Upload MCQs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Done")
      }
    }
  )
}
