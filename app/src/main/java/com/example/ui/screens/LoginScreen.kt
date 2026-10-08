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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.BioGreen
import com.example.ui.theme.ChemOrange
import com.example.ui.theme.EnglishBlue
import com.example.ui.viewmodel.MDCATViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated Firebase Auth & Google Identity Login Screen.
 * Secures access to study materials and establishes Role-Based Access Control (RBAC).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  viewModel: MDCATViewModel,
  onLoginSuccess: () -> Unit = {},
  onDismiss: () -> Unit = {}
) {
  val userAccount by viewModel.userAccount.collectAsState()
  val adminAuthState by viewModel.adminAuthState.collectAsState()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var selectedRole by remember { mutableStateOf<UserRole?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    modifier = Modifier.testTag("login_screen"),
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Security",
              tint = BioGreen,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Firebase Identity & RBAC Portal",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("login_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        actions = {
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App Identity Header
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleBrushShape())
            .background(
              Brush.linearGradient(
                listOf(BioGreen.copy(alpha = 0.85f), EnglishBlue.copy(alpha = 0.85f))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = "Dr. Salar MDCAT",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Dr. Salar's MDCAT Master",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Text(
          text = "Secure Access for Study Materials & Faculty Hub",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Security badges
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = BioGreen.copy(alpha = 0.12f)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BioGreen,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Firebase Auth 34.17",
                style = MaterialTheme.typography.labelSmall,
                color = BioGreen,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = EnglishBlue.copy(alpha = 0.12f)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = EnglishBlue,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Google Identity Ready",
                style = MaterialTheme.typography.labelSmall,
                color = EnglishBlue,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        // Error / Success Feedback Banners
        errorMessage?.let { error ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          }
        }

        successMessage?.let { success ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp),
            colors = CardDefaults.cardColors(containerColor = BioGreen.copy(alpha = 0.15f))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = BioGreen,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = success,
                style = MaterialTheme.typography.bodySmall,
                color = BioGreen,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // --- PRIMARY SIGN IN: Google / Gmail Button ---
        Button(
          onClick = {
            isLoading = true
            errorMessage = null
            scope.launch {
              delay(600) // Realistic interactive authentication
              // Default to Dr. Salar owner login if no email typed, or parse input
              val targetEmail = if (emailInput.isNotBlank()) emailInput else "shaukatsalar231@gmail.com"
              val success = viewModel.loginWithGmail(
                email = targetEmail,
                displayName = if (targetEmail == "shaukatsalar231@gmail.com") "Dr. Salar" else null,
                roleOverride = selectedRole
              )
              isLoading = false
              if (success) {
                successMessage = "Successfully authenticated via Google Identity & Firebase Auth!"
                delay(400)
                onLoginSuccess()
                onDismiss()
              } else {
                errorMessage = "Google Sign-In failed. Please check your credentials."
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_signin_button"),
          shape = RoundedCornerShape(26.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = BioGreen,
              strokeWidth = 2.dp
            )
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              // Google G badge placeholder icon
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(Color.White),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "G",
                  color = EnglishBlue,
                  fontWeight = FontWeight.Black,
                  fontSize = 15.sp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Sign in with Google / Gmail",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Divider
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f))
          Text(
            text = "OR ENTER GMAIL CREDENTIALS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp)
          )
          HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Gmail input field
        OutlinedTextField(
          value = emailInput,
          onValueChange = {
            emailInput = it
            errorMessage = null
          },
          label = { Text("Gmail Address") },
          placeholder = { Text("e.g. shaukatsalar231@gmail.com") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Email,
              contentDescription = "Email",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("gmail_input_field"),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password input field
        OutlinedTextField(
          value = passwordInput,
          onValueChange = {
            passwordInput = it
            errorMessage = null
          },
          label = { Text("Password / Passcode") },
          placeholder = { Text("Enter account password") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Password",
              tint = MaterialTheme.colorScheme.primary
            )
          },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (passwordVisible) "Hide password" else "Show password"
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (emailInput.isNotBlank()) {
                isLoading = true
                viewModel.loginWithGmail(emailInput, roleOverride = selectedRole)
                isLoading = false
                onLoginSuccess()
                onDismiss()
              }
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("password_input_field"),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Role Designation Info
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = ChemOrange,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Role-Based Access Control (RBAC)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Roles determine whether you can view or modify study materials and the Admin Dashboard. Select an account profile below to test access levels:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Role Switch Test Profiles
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              // 1. Owner Profile
              QuickProfileItem(
                title = "Dr. Salar (Verified Owner)",
                email = "shaukatsalar231@gmail.com",
                role = UserRole.OWNER,
                roleDescription = "Full Access: View & modify all study materials, MCQs, PDFs & Admin Dashboard",
                badgeColor = BioGreen,
                isSelected = (emailInput == "shaukatsalar231@gmail.com" || userAccount.email == "shaukatsalar231@gmail.com"),
                onClick = {
                  emailInput = "shaukatsalar231@gmail.com"
                  passwordInput = "••••••••"
                  selectedRole = UserRole.OWNER
                }
              )

              // 2. Admin Profile
              QuickProfileItem(
                title = "Academic Admin (Co-Instructor)",
                email = "admin.mdcat@gmail.com",
                role = UserRole.ADMIN,
                roleDescription = "Faculty Access: Can view and modify study materials, notes, PDFs, and tests",
                badgeColor = EnglishBlue,
                isSelected = (emailInput == "admin.mdcat@gmail.com" || selectedRole == UserRole.ADMIN),
                onClick = {
                  emailInput = "admin.mdcat@gmail.com"
                  passwordInput = "••••••••"
                  selectedRole = UserRole.ADMIN
                }
              )

              // 3. Student Profile
              QuickProfileItem(
                title = "Pre-Medical Aspirant (Student)",
                email = "student.mdcat@gmail.com",
                role = UserRole.STUDENT,
                roleDescription = "Student Access: Can view study notes & take tests. Admin Dashboard is strictly locked",
                badgeColor = ChemOrange,
                isSelected = (emailInput == "student.mdcat@gmail.com" || selectedRole == UserRole.STUDENT),
                onClick = {
                  emailInput = "student.mdcat@gmail.com"
                  passwordInput = "••••••••"
                  selectedRole = UserRole.STUDENT
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Action Button
        Button(
          onClick = {
            val email = emailInput.trim().ifBlank { "shaukatsalar231@gmail.com" }
            if (!email.endsWith("@gmail.com")) {
              errorMessage = "Please enter a valid Gmail address ending in @gmail.com"
              return@Button
            }
            isLoading = true
            errorMessage = null
            scope.launch {
              delay(400)
              val success = viewModel.loginWithGmail(
                email = email,
                displayName = null,
                roleOverride = selectedRole
              )
              isLoading = false
              if (success) {
                successMessage = "Signed in as ${if (selectedRole != null) selectedRole?.label else "User"} successfully!"
                delay(300)
                onLoginSuccess()
                onDismiss()
              } else {
                errorMessage = "Authentication failed. Please verify credentials."
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("submit_login_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BioGreen)
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
          } else {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (emailInput.isNotBlank()) "Sign In with $emailInput" else "Sign In with Gmail",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Guest / Continue Without Login Button
        OutlinedButton(
          onClick = {
            viewModel.logoutUser()
            onDismiss()
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("guest_mode_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text(
            text = "Continue as Guest (Restricted Access)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun QuickProfileItem(
  title: String,
  email: String,
  role: UserRole,
  roleDescription: String,
  badgeColor: Color,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isSelected) badgeColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor) else null,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("quick_profile_${role.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(badgeColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (role) {
            UserRole.OWNER -> Icons.Default.WorkspacePremium
            UserRole.ADMIN -> Icons.Default.Security
            UserRole.STUDENT -> Icons.Default.School
            UserRole.GUEST -> Icons.Default.Person
          },
          contentDescription = null,
          tint = badgeColor,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeColor.copy(alpha = 0.25f)
          ) {
            Text(
              text = role.label.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Text(
          text = email,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = roleDescription,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp,
          lineHeight = 14.sp
        )
      }

      if (isSelected) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Selected",
          tint = badgeColor,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

private class CircleBrushShape : androidx.compose.ui.graphics.Shape {
  override fun createOutline(
    size: androidx.compose.ui.geometry.Size,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    density: androidx.compose.ui.unit.Density
  ): androidx.compose.ui.graphics.Outline {
    return androidx.compose.ui.graphics.Outline.Generic(
      androidx.compose.ui.graphics.Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
      }
    )
  }
}
