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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicClass
import com.example.data.model.CourseSubscription
import com.example.ui.viewmodel.MDCATViewModel

@Composable
fun CourseSubscriptionDialog(
  viewModel: MDCATViewModel,
  onDismiss: () -> Unit
) {
  val courses by viewModel.courseSubscriptions.collectAsState()
  val userAccount by viewModel.userAccount.collectAsState()

  var selectedClassFilter by remember { mutableStateOf<AcademicClass?>(null) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }

  val filteredCourses = remember(courses, selectedClassFilter) {
    if (selectedClassFilter == null) courses else courses.filter { it.targetClass == selectedClassFilter }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = Color(0xFFF9AB00).copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFF9AB00),
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Course Subscriptions",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Official MDCAT Curriculum by Dr. Salar",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close dialog")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(480.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Class Block Filters
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = selectedClassFilter == null,
            onClick = { selectedClassFilter = null },
            label = { Text("All Courses (${courses.size})", fontSize = 11.sp) }
          )
          FilterChip(
            selected = selectedClassFilter == AcademicClass.CLASS_11,
            onClick = { selectedClassFilter = AcademicClass.CLASS_11 },
            label = { Text("11th Class", fontSize = 11.sp) }
          )
          FilterChip(
            selected = selectedClassFilter == AcademicClass.CLASS_12,
            onClick = { selectedClassFilter = AcademicClass.CLASS_12 },
            label = { Text("12th Class", fontSize = 11.sp) }
          )
        }

        if (feedbackMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = feedbackMessage!!,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { feedbackMessage = null }, modifier = Modifier.size(20.dp)) {
                Text("✕", fontSize = 10.sp)
              }
            }
          }
        }

        // List of Course Cards
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredCourses, key = { it.courseId }) { course ->
            CourseSubscriptionItemCard(
              course = course,
              isLoggedIn = userAccount.isLoggedIn,
              onToggleSubscribe = { subscribe ->
                if (!userAccount.isLoggedIn) {
                  viewModel.toggleLoginDialog(true)
                } else {
                  viewModel.toggleCourseSubscription(course.courseId, subscribe)
                  feedbackMessage = if (subscribe) {
                    "Subscribed to ${course.title}! All materials & live classes unlocked."
                  } else {
                    "Subscription cancelled for ${course.title}."
                  }
                }
              }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Done")
      }
    }
  )
}

@Composable
fun CourseSubscriptionItemCard(
  course: CourseSubscription,
  isLoggedIn: Boolean,
  onToggleSubscribe: (Boolean) -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (course.isSubscribed) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
      } else {
        MaterialTheme.colorScheme.surface
      }
    ),
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
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = Color(0xFFF9AB00).copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = course.badge,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFFE37400),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (course.targetClass != null) {
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = course.targetClass.shortName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        if (course.isSubscribed) {
          Surface(
            color = Color(0xFF137333),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("SUBSCRIBED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = course.title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Lead Faculty: ${course.instructor}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "• ${course.durationWeeks} Weeks",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Text(
        text = course.description,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 15.sp,
        modifier = Modifier.padding(vertical = 4.dp)
      )

      Spacer(modifier = Modifier.height(6.dp))
      // Key Features
      course.features.take(3).forEach { feature ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(vertical = 1.dp)
        ) {
          Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF34A853),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = feature, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "PKR ${course.pricePkr}",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "PKR ${course.originalPricePkr}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textDecoration = TextDecoration.LineThrough
            )
          }
          Text(
            text = "${course.totalSubscribers} Students Enrolled",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (course.isSubscribed) {
          OutlinedButton(
            onClick = { onToggleSubscribe(false) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_unsubscribe_${course.courseId}")
          ) {
            Text("Cancel Sub", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
          }
        } else {
          Button(
            onClick = { onToggleSubscribe(true) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_subscribe_${course.courseId}")
          ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Subscribe", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  }
}
