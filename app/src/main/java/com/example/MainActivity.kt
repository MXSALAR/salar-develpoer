package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CommunityAndNotesScreen
import com.example.ui.screens.CourseSubscriptionDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginAccountDialog
import com.example.ui.screens.PracticeTestsScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.StudyMaterialsScreen
import com.example.ui.screens.TermsAndConditionsModal
import com.example.ui.theme.MDCATTheme
import com.example.ui.theme.StreakFlame
import com.example.ui.viewmodel.MDCATViewModel
import com.example.ui.viewmodel.MainNavTab

class MainActivity : ComponentActivity() {

  private val viewModel: MDCATViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_MyApplication)
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.themeMode.collectAsState()

      MDCATTheme(themeMode = themeMode) {
        MDCATMainScreen(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MDCATMainScreen(viewModel: MDCATViewModel) {
  val currentTab by viewModel.currentTab.collectAsState()
  val gamification by viewModel.gamification.collectAsState()
  val showSettings by viewModel.showSettingsDialog.collectAsState()
  val showAdminDashboard by viewModel.showAdminDashboard.collectAsState()
  val isQuizActive by viewModel.isQuizActive.collectAsState()
  val userAccount by viewModel.userAccount.collectAsState()
  val showLoginDialog by viewModel.showLoginDialog.collectAsState()
  val showLoginScreen by viewModel.showLoginScreen.collectAsState()
  val showSubscriptionSheet by viewModel.showSubscriptionSheet.collectAsState()
  val showPrivacyPolicy by viewModel.showPrivacyPolicyScreen.collectAsState()
  val showTermsModal by viewModel.showTermsModal.collectAsState()

  if (showPrivacyPolicy) {
    PrivacyPolicyScreen(
      viewModel = viewModel,
      onBack = { viewModel.togglePrivacyPolicyScreen(false) }
    )
    return
  }

  if (showLoginScreen) {
    com.example.ui.screens.LoginScreen(
      viewModel = viewModel,
      onLoginSuccess = { viewModel.toggleLoginScreen(false) },
      onDismiss = { viewModel.toggleLoginScreen(false) }
    )
    return
  }

  if (showAdminDashboard) {
    AdminDashboardScreen(
      viewModel = viewModel,
      onClose = { viewModel.toggleAdminDashboard(false) }
    )
    return
  }

  Scaffold(
    topBar = {
      if (!isQuizActive) {
        CenterAlignedTopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { viewModel.toggleAdminDashboard(true) }
            ) {
              Text(
                text = "MDCAT Master",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Dr salar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          },
          navigationIcon = {
            // Streak counter badge
            Row(
              modifier = Modifier
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = StreakFlame,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${gamification.streakDays}d",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          actions = {
            IconButton(
              onClick = { viewModel.toggleLoginDialog(true) },
              modifier = Modifier.testTag("btn_top_account")
            ) {
              if (userAccount.isLoggedIn) {
                Surface(
                  color = if (userAccount.isInstructor) MaterialTheme.colorScheme.primary else Color(0xFF4285F4),
                  shape = CircleShape,
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = userAccount.displayName.take(1).uppercase(),
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                    )
                  }
                }
              } else {
                Icon(
                  Icons.Default.AccountCircle,
                  contentDescription = "Sign in with Gmail",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
            if (userAccount.isInstructor) {
              IconButton(
                onClick = { viewModel.toggleAdminDashboard(true) },
                modifier = Modifier.testTag("btn_open_admin_hub")
              ) {
                Icon(
                  Icons.Default.MedicalServices,
                  contentDescription = "Instructor Hub (Dr. Salar)",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
            IconButton(
              onClick = { viewModel.toggleSettingsDialog(true) },
              modifier = Modifier.testTag("btn_open_settings")
            ) {
              Icon(
                Icons.Default.Settings,
                contentDescription = "Settings & Customization",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          )
        )
      }
    },
    bottomBar = {
      if (!isQuizActive) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.testTag("main_navigation_bar")
        ) {
          val navItems = listOf(
            Triple(MainNavTab.DASHBOARD, Icons.Default.Home, "nav_dashboard"),
            Triple(MainNavTab.STUDY, Icons.Default.Book, "nav_study"),
            Triple(MainNavTab.TESTS, Icons.Default.Quiz, "nav_tests"),
            Triple(MainNavTab.ANALYTICS, Icons.Default.BarChart, "nav_analytics"),
            Triple(MainNavTab.COMMUNITY_NOTES, Icons.Default.Forum, "nav_community")
          )

          navItems.forEach { (tab, icon, tag) ->
            NavigationBarItem(
              selected = currentTab == tab,
              onClick = { viewModel.setTab(tab) },
              icon = { Icon(icon, contentDescription = tab.title) },
              label = { Text(tab.title, fontSize = 11.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
              modifier = Modifier.testTag(tag)
            )
          }
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentTab) {
        MainNavTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
        MainNavTab.STUDY -> StudyMaterialsScreen(viewModel = viewModel)
        MainNavTab.TESTS -> PracticeTestsScreen(viewModel = viewModel)
        MainNavTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
        MainNavTab.COMMUNITY_NOTES -> CommunityAndNotesScreen(viewModel = viewModel)
      }
    }
  }

  // Settings & Dr salar Credit Dialog
  if (showSettings) {
    SettingsDialog(
      viewModel = viewModel,
      onDismiss = { viewModel.toggleSettingsDialog(false) }
    )
  }

  // Gmail Login & User Profile Dialog
  if (showLoginDialog) {
    LoginAccountDialog(
      viewModel = viewModel,
      onDismiss = { viewModel.toggleLoginDialog(false) }
    )
  }

  // Course Subscription Management Dialog
  if (showSubscriptionSheet) {
    CourseSubscriptionDialog(
      viewModel = viewModel,
      onDismiss = { viewModel.toggleSubscriptionSheet(false) }
    )
  }

  // Terms and Conditions Onboarding Modal
  if (showTermsModal) {
    TermsAndConditionsModal(
      viewModel = viewModel,
      onDismiss = { viewModel.toggleTermsModal(false) },
      onViewPrivacyPolicy = {
        viewModel.toggleTermsModal(false)
        viewModel.togglePrivacyPolicyScreen(true)
      }
    )
  }
}
