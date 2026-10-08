package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveSession
import com.example.data.model.LiveSessionStatus
import com.example.ui.viewmodel.MDCATViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveSessionBlock(
  viewModel: MDCATViewModel,
  modifier: Modifier = Modifier
) {
  val liveSessions by viewModel.liveSessions.collectAsState()
  val userAccount by viewModel.userAccount.collectAsState()
  val context = LocalContext.current

  val activeLiveSession = liveSessions.firstOrNull { it.status == LiveSessionStatus.LIVE_NOW }
    ?: liveSessions.firstOrNull { it.status == LiveSessionStatus.UPCOMING }
    ?: liveSessions.firstOrNull()

  var isAgendaExpanded by remember { mutableStateOf(false) }
  var feedbackText by remember { mutableStateOf<String?>(null) }

  if (activeLiveSession == null) return

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (activeLiveSession.status == LiveSessionStatus.LIVE_NOW) {
        MaterialTheme.colorScheme.surface
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      }
    ),
    border = CardDefaults.outlinedCardBorder(),
    modifier = modifier
      .fillMaxWidth()
      .testTag("block_live_session")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top status row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (activeLiveSession.status == LiveSessionStatus.LIVE_NOW) {
            Surface(
              color = Color(0xFFD93025),
              shape = RoundedCornerShape(20.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.FiberManualRecord,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "LIVE NOW",
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 11.sp
                )
              }
            }
          } else {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(20.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.CalendarMonth,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = activeLiveSession.status.name,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = activeLiveSession.subject.color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = activeLiveSession.subject.displayName,
              color = activeLiveSession.subject.color,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = activeLiveSession.academicClass.shortName,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // Attendees badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            Icons.Default.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = "${activeLiveSession.attendeesCount} Live",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = activeLiveSession.title,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Instructor: ${activeLiveSession.instructorName}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(3.dp))
          Icon(
            Icons.Default.Verified,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${activeLiveSession.durationMinutes} mins",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Scheduled Time
      val timeFormat = SimpleDateFormat("EEEE, d MMM 'at' hh:mm a", Locale.getDefault())
      val formattedTime = remember(activeLiveSession.scheduledTimeMillis) {
        timeFormat.format(Date(activeLiveSession.scheduledTimeMillis))
      }

      Text(
        text = "Time: $formattedTime (PKT)",
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
      )

      // Agenda Accordion
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isAgendaExpanded = !isAgendaExpanded },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (isAgendaExpanded) "Hide Session Agenda" else "View High-Yield Agenda Topics",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
        Icon(
          if (isAgendaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }

      AnimatedVisibility(visible = isAgendaExpanded) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
        ) {
          Text(
            text = activeLiveSession.agenda,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
            modifier = Modifier.padding(10.dp)
          )
        }
      }

      if (feedbackText != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = feedbackText!!,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      Spacer(modifier = Modifier.height(10.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = {
            if (!userAccount.isLoggedIn) {
              viewModel.toggleLoginDialog(true)
            } else {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeLiveSession.meetingUrl))
                context.startActivity(intent)
              } catch (e: Exception) {
                feedbackText = "Opening Live Room: ${activeLiveSession.meetingUrl}"
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (activeLiveSession.status == LiveSessionStatus.LIVE_NOW) {
              Color(0xFFD93025)
            } else {
              MaterialTheme.colorScheme.primary
            }
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_join_live_session")
        ) {
          Icon(
            if (userAccount.isLoggedIn) Icons.Default.Videocam else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (!userAccount.isLoggedIn) {
              "Sign in to Join"
            } else if (activeLiveSession.status == LiveSessionStatus.LIVE_NOW) {
              "Join Google Meet Live"
            } else {
              "Enter Live Room"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }

        OutlinedButton(
          onClick = {
            feedbackText = "Reminder set for ${activeLiveSession.title}!"
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("btn_set_live_reminder")
        ) {
          Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reminder", fontSize = 11.sp)
        }
      }
    }
  }
}
