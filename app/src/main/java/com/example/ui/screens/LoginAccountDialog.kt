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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MDCATViewModel

@Composable
fun LoginAccountDialog(
  viewModel: MDCATViewModel,
  onDismiss: () -> Unit
) {
  val userAccount by viewModel.userAccount.collectAsState()
  val courseSubscriptions by viewModel.courseSubscriptions.collectAsState()

  var inputEmail by remember { mutableStateOf(userAccount.email.ifBlank { "student.mdcat2026@gmail.com" }) }
  var inputDisplayName by remember { mutableStateOf(userAccount.displayName.ifBlank { "MDCAT Aspirant" }) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

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
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (userAccount.isLoggedIn) "Google Account Profile" else "Sign In with Gmail",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "MDCAT Master by Dr. Salar",
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
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        if (userAccount.isLoggedIn && userAccount.email.isNotBlank()) {
          // Logged In User Profile Card
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = if (userAccount.isInstructor) MaterialTheme.colorScheme.primary else Color(0xFF4285F4),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = userAccount.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = userAccount.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                      )
                      if (userAccount.isInstructor) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                          Icons.Default.Verified,
                          contentDescription = "Verified Educator",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(15.dp)
                        )
                      }
                    }
                    Text(
                      text = userAccount.email,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Surface(
                  color = if (userAccount.isInstructor) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE8F0FE),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = if (userAccount.isInstructor) "Lead Educator" else "Active Student",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (userAccount.isInstructor) MaterialTheme.colorScheme.primary else Color(0xFF1967D2),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
              HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
              Spacer(modifier = Modifier.height(10.dp))

              // Access & Enrollment Details
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Full Syllabus & Material Access: Granted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              val subscribedCount = courseSubscriptions.count { it.isSubscribed }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  Icons.Default.WorkspacePremium,
                  contentDescription = null,
                  tint = Color(0xFFF9AB00),
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Enrolled in $subscribedCount Course Bundle(s)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Logout or Switch Account Options
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                viewModel.logoutUser()
                successMessage = "Signed out. Please sign in with Gmail to use materials."
              },
              modifier = Modifier
                .weight(1f)
                .testTag("btn_logout_user"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sign Out", fontSize = 12.sp)
            }

            Button(
              onClick = {
                // Quick switch to Dr. Salar Owner account
                if (userAccount.email != "shaukatsalar231@gmail.com") {
                  viewModel.loginWithGmail("shaukatsalar231@gmail.com", "Dr. Salar")
                  successMessage = "Switched to Dr. Salar (Instructor) account."
                } else {
                  viewModel.loginWithGmail("student.mdcat2026@gmail.com", "Ahmad (Student)")
                  successMessage = "Switched to Student test account."
                }
              },
              modifier = Modifier
                .weight(1.2f)
                .testTag("btn_switch_account"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(
                text = if (userAccount.isInstructor) "Switch to Student" else "Login as Dr. Salar",
                fontSize = 12.sp
              )
            }
          }
        } else {
          // Logged Out State - Must Login to Use Materials
          Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Sign in with a valid Gmail account to unlock Dr. Salar's study materials, live sessions, and full MCQ question banks.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 16.sp
              )
            }
          }

          // One-tap Google Sign-In button
          Card(
            onClick = {
              viewModel.loginWithGmail("student.mdcat2026@gmail.com", "MDCAT Student")
              successMessage = "Successfully signed in with Google!"
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_google_signin_instant")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              // Google color badge
              Surface(
                color = Color(0xFF4285F4),
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text("G", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Instant Sign in with Google",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
              text = "  OR ENTER GMAIL  ",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
          }

          // Manual Email Input
          OutlinedTextField(
            value = inputEmail,
            onValueChange = {
              inputEmail = it
              errorMessage = null
            },
            label = { Text("Gmail Address") },
            placeholder = { Text("yourname@gmail.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_login_gmail"),
            shape = RoundedCornerShape(10.dp)
          )

          OutlinedTextField(
            value = inputDisplayName,
            onValueChange = { inputDisplayName = it },
            label = { Text("Student / Full Name") },
            placeholder = { Text("e.g. Ayesha Khan") },
            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_login_name"),
            shape = RoundedCornerShape(10.dp)
          )

          // Preset Buttons
          Text(
            text = "Quick Select Accounts:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                inputEmail = "shaukatsalar231@gmail.com"
                inputDisplayName = "Dr. Salar"
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Dr. Salar (Admin)", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = {
                inputEmail = "student.mdcat2026@gmail.com"
                inputDisplayName = "Hamza Ali"
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Student Account", fontSize = 11.sp)
            }
          }

          if (errorMessage != null) {
            Text(
              text = errorMessage!!,
              color = MaterialTheme.colorScheme.error,
              fontSize = 11.sp
            )
          }

          Button(
            onClick = {
              val email = inputEmail.trim().lowercase()
              if (!email.contains("@") || !email.endsWith("@gmail.com")) {
                errorMessage = "Please enter a valid @gmail.com address."
                return@Button
              }
              viewModel.loginWithGmail(email, inputDisplayName.ifBlank { "MDCAT Student" })
              successMessage = "Logged in successfully! Material access unlocked."
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_submit_gmail_login"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Verify & Continue with Gmail", fontWeight = FontWeight.Bold)
          }
        }

        if (successMessage != null) {
          Surface(
            color = Color(0xFFE6F4EA),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF137333),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = successMessage!!,
                color = Color(0xFF137333),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
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
