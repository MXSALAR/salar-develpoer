package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Data repository class to manage user profile storage in Firebase Firestore,
 * including assigned roles and permissions for the Admin Dashboard and Role-Based Access Control (RBAC).
 */
class UserProfileRepository private constructor(private val context: Context) {

  private val firestore: FirebaseFirestore? = runCatching {
    FirebaseFirestore.getInstance()
  }.onFailure {
    Log.w(TAG, "Firestore initialization fallback: ${it.message}")
  }.getOrNull()

  // In-memory reactive cache for the active user's profile
  private val _activeUserProfile = MutableStateFlow(createDefaultOwnerProfile())
  val activeUserProfile: Flow<UserProfile> = _activeUserProfile.asStateFlow()

  // Reactive list of all admin/instructor profiles for the Admin Dashboard user management
  private val _adminProfiles = MutableStateFlow<List<UserProfile>>(createDefaultAdminProfiles())
  val adminProfiles: Flow<List<UserProfile>> = _adminProfiles.asStateFlow()

  // Status message for sync operations
  private val _syncStatus = MutableStateFlow("Initialized")
  val syncStatus: Flow<String> = _syncStatus.asStateFlow()

  private var snapshotListener: ListenerRegistration? = null

  init {
    // Initial fetch of admin profiles from Firestore if available
    CoroutineScope(Dispatchers.IO).launch {
      fetchAdminProfilesFromFirestore()
    }
  }

  /**
   * Saves or updates a user profile in Firebase Firestore with their assigned roles and permissions.
   */
  suspend fun saveUserProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
    runCatching {
      // 1. Update local state immediately for responsive UI
      _activeUserProfile.value = profile
      updateAdminListWithProfile(profile)

      // 2. Persist to Firebase Firestore if connected
      val fs = firestore
      if (fs != null) {
        val docData = profileToMap(profile)
        fs.collection(USERS_COLLECTION)
          .document(profile.uid)
          .set(docData, SetOptions.merge())
          .await()
        Log.i(TAG, "User profile successfully saved to Firestore: ${profile.email} (${profile.role.name})")
        _syncStatus.value = "Profile synced with Firestore"
      } else {
        Log.w(TAG, "Firestore offline or unconfigured. Saved to local repository cache.")
        _syncStatus.value = "Saved locally (Offline Mode)"
      }
      profile
    }.onFailure { err ->
      Log.e(TAG, "Failed to save profile to Firestore", err)
      _syncStatus.value = "Sync error: ${err.message}"
    }
  }

  /**
   * Fetches a user profile from Firestore by UID. Falls back to active cache if Firestore unavailable.
   */
  suspend fun getUserProfile(uid: String): UserProfile? = withContext(Dispatchers.IO) {
    val fs = firestore
    if (fs == null) {
      return@withContext if (_activeUserProfile.value.uid == uid) _activeUserProfile.value else null
    }

    try {
      val doc = fs.collection(USERS_COLLECTION).document(uid).get().await()
      if (doc.exists()) {
        val profile = mapToProfile(doc)
        if (profile.uid == _activeUserProfile.value.uid) {
          _activeUserProfile.value = profile
        }
        profile
      } else {
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error fetching user profile from Firestore: ${e.message}")
      if (_activeUserProfile.value.uid == uid) _activeUserProfile.value else null
    }
  }

  /**
   * Starts a real-time snapshot listener on the active user profile in Firestore.
   */
  fun observeUserProfile(uid: String) {
    snapshotListener?.remove()
    val fs = firestore ?: return

    snapshotListener = fs.collection(USERS_COLLECTION).document(uid)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Snapshot listener error: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val profile = mapToProfile(snapshot)
          _activeUserProfile.value = profile
          updateAdminListWithProfile(profile)
        }
      }
  }

  /**
   * Updates assigned roles and permissions for a specific user. Used by Owner in Admin Dashboard.
   */
  suspend fun updateUserRole(
    uid: String,
    newRole: UserRole,
    assignedRoles: List<String>,
    isAdminAuthorized: Boolean
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val permissions = when (newRole) {
        UserRole.OWNER -> listOf(
          "MANAGE_SYLLABUS", "CREATE_MCQS", "EDIT_EXAMS", "MANAGE_USERS",
          "VIEW_ANALYTICS", "BROADCAST_LIVE", "SYSTEM_SETTINGS"
        )
        UserRole.ADMIN -> listOf(
          "MANAGE_SYLLABUS", "CREATE_MCQS", "EDIT_EXAMS", "VIEW_ANALYTICS"
        )
        UserRole.STUDENT -> listOf("VIEW_SYLLABUS", "TAKE_TESTS", "COMMUNITY_POST")
        UserRole.GUEST -> listOf("VIEW_PUBLIC")
      }

      // Update in-memory active user if same
      if (_activeUserProfile.value.uid == uid) {
        _activeUserProfile.value = _activeUserProfile.value.copy(
          role = newRole,
          assignedRoles = assignedRoles,
          permissions = permissions,
          isAdminDashboardAuthorized = isAdminAuthorized
        )
      }

      // Update admin list in-memory
      _adminProfiles.value = _adminProfiles.value.map { prof ->
        if (prof.uid == uid) {
          prof.copy(
            role = newRole,
            assignedRoles = assignedRoles,
            permissions = permissions,
            isAdminDashboardAuthorized = isAdminAuthorized
          )
        } else prof
      }

      // Persist update in Firestore
      val fs = firestore
      if (fs != null) {
        val updates = mapOf(
          "role" to newRole.name,
          "assignedRoles" to assignedRoles,
          "permissions" to permissions,
          "isAdminDashboardAuthorized" to isAdminAuthorized,
          "lastRoleUpdatedMillis" to System.currentTimeMillis()
        )
        fs.collection(USERS_COLLECTION).document(uid).update(updates).await()
        Log.i(TAG, "Updated role in Firestore for UID $uid to ${newRole.name}")
      }
    }
  }

  /**
   * Assigns an admin/owner role by email address, ensuring RBAC records in Firestore.
   */
  suspend fun assignRoleByEmail(
    email: String,
    role: UserRole,
    department: String = "Academic Administration"
  ): Result<UserProfile> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanEmail = email.trim().lowercase()
      val uid = "user_${cleanEmail.hashCode()}"
      val profile = UserProfile(
        uid = uid,
        email = cleanEmail,
        displayName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
        role = role,
        assignedRoles = listOf(role.name, "INSTRUCTOR"),
        permissions = if (role.canManageContent) {
          listOf("MANAGE_SYLLABUS", "CREATE_MCQS", "EDIT_EXAMS", "VIEW_ANALYTICS")
        } else {
          listOf("VIEW_SYLLABUS", "TAKE_TESTS")
        },
        isAdminDashboardAuthorized = role.canManageContent,
        department = department,
        createdAtMillis = System.currentTimeMillis(),
        lastLoginMillis = System.currentTimeMillis()
      )
      saveUserProfile(profile).getOrThrow()
    }
  }

  /**
   * Synchronizes an existing UserAccount from auth service into Firestore UserProfile.
   */
  suspend fun syncWithAccount(account: UserAccount): UserProfile {
    val assignedRoles = when (account.role) {
      UserRole.OWNER -> listOf("OWNER", "LEAD_INSTRUCTOR", "ADMIN")
      UserRole.ADMIN -> listOf("ADMIN", "INSTRUCTOR")
      UserRole.STUDENT -> listOf("STUDENT")
      UserRole.GUEST -> listOf("GUEST")
    }
    val profile = UserProfile(
      uid = account.uid,
      email = account.email,
      displayName = account.displayName,
      photoUrl = account.photoUrl,
      role = account.role,
      assignedRoles = assignedRoles,
      permissions = if (account.role.canManageContent) {
        listOf("MANAGE_SYLLABUS", "CREATE_MCQS", "EDIT_EXAMS", "VIEW_ANALYTICS")
      } else {
        listOf("VIEW_SYLLABUS", "TAKE_TESTS")
      },
      isAdminDashboardAuthorized = account.role.canManageContent,
      department = if (account.isOwner) "MDCAT Academic Directorate" else "MDCAT Student Community",
      enrolledCourses = account.enrolledCourseIds.toList(),
      lastLoginMillis = System.currentTimeMillis()
    )
    saveUserProfile(profile)
    return profile
  }

  private suspend fun fetchAdminProfilesFromFirestore() {
    val fs = firestore ?: return
    try {
      val snapshot = fs.collection(USERS_COLLECTION)
        .whereIn("role", listOf("OWNER", "ADMIN"))
        .get()
        .await()

      val remoteAdmins = snapshot.documents.map { mapToProfile(it) }
      if (remoteAdmins.isNotEmpty()) {
        _adminProfiles.value = remoteAdmins
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not fetch admin profiles from Firestore: ${e.message}")
    }
  }

  private fun updateAdminListWithProfile(profile: UserProfile) {
    if (profile.role.canManageContent) {
      val existing = _adminProfiles.value.toMutableList()
      val index = existing.indexOfFirst { it.uid == profile.uid }
      if (index >= 0) {
        existing[index] = profile
      } else {
        existing.add(profile)
      }
      _adminProfiles.value = existing
    }
  }

  private fun profileToMap(profile: UserProfile): Map<String, Any?> {
    return mapOf(
      "uid" to profile.uid,
      "email" to profile.email,
      "displayName" to profile.displayName,
      "photoUrl" to profile.photoUrl,
      "role" to profile.role.name,
      "assignedRoles" to profile.assignedRoles,
      "permissions" to profile.permissions,
      "isAdminDashboardAuthorized" to profile.isAdminDashboardAuthorized,
      "department" to profile.department,
      "enrolledCourses" to profile.enrolledCourses,
      "createdAtMillis" to profile.createdAtMillis,
      "lastLoginMillis" to profile.lastLoginMillis
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun mapToProfile(doc: DocumentSnapshot): UserProfile {
    val roleStr = doc.getString("role") ?: "STUDENT"
    val roleEnum = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.STUDENT)
    return UserProfile(
      uid = doc.getString("uid") ?: doc.id,
      email = doc.getString("email") ?: "",
      displayName = doc.getString("displayName") ?: "Student",
      photoUrl = doc.getString("photoUrl"),
      role = roleEnum,
      assignedRoles = (doc.get("assignedRoles") as? List<String>) ?: listOf(roleEnum.name),
      permissions = (doc.get("permissions") as? List<String>) ?: emptyList(),
      isAdminDashboardAuthorized = doc.getBoolean("isAdminDashboardAuthorized") ?: roleEnum.canManageContent,
      department = doc.getString("department") ?: "MDCAT Aspirant",
      enrolledCourses = (doc.get("enrolledCourses") as? List<String>) ?: emptyList(),
      createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis(),
      lastLoginMillis = doc.getLong("lastLoginMillis") ?: System.currentTimeMillis()
    )
  }

  private fun createDefaultOwnerProfile(): UserProfile {
    return UserProfile(
      uid = "user_salar_owner_root",
      email = "shaukatsalar231@gmail.com",
      displayName = "Dr. Salar",
      role = UserRole.OWNER,
      assignedRoles = listOf("OWNER", "LEAD_INSTRUCTOR", "ADMIN"),
      permissions = listOf(
        "MANAGE_SYLLABUS", "CREATE_MCQS", "EDIT_EXAMS", "MANAGE_USERS",
        "VIEW_ANALYTICS", "BROADCAST_LIVE", "SYSTEM_SETTINGS"
      ),
      isAdminDashboardAuthorized = true,
      department = "MDCAT Academic Directorate",
      enrolledCourses = listOf("course_mdcat_full", "course_bio_11", "course_physics_12")
    )
  }

  private fun createDefaultAdminProfiles(): List<UserProfile> {
    return listOf(
      createDefaultOwnerProfile(),
      UserProfile(
        uid = "user_academic_admin",
        email = "admin.mdcat@gmail.com",
        displayName = "Academic Admin (Syllabus)",
        role = UserRole.ADMIN,
        assignedRoles = listOf("ADMIN", "INSTRUCTOR"),
        permissions = listOf("MANAGE_SYLLABUS", "CREATE_MCQS", "EDIT_EXAMS", "VIEW_ANALYTICS"),
        isAdminDashboardAuthorized = true,
        department = "Content & Question Bank Team"
      ),
      UserProfile(
        uid = "user_salar_assistant",
        email = "drsalar.assistant@gmail.com",
        displayName = "Dr. Salar Teaching Assistant",
        role = UserRole.ADMIN,
        assignedRoles = listOf("ADMIN", "ASSISTANT"),
        permissions = listOf("MANAGE_SYLLABUS", "CREATE_MCQS", "VIEW_ANALYTICS"),
        isAdminDashboardAuthorized = true,
        department = "Biology & Chemistry Evaluation"
      )
    )
  }

  fun cleanup() {
    snapshotListener?.remove()
  }

  companion object {
    private const val TAG = "UserProfileRepository"
    private const val USERS_COLLECTION = "user_profiles"

    @Volatile
    private var INSTANCE: UserProfileRepository? = null

    fun getInstance(context: Context): UserProfileRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: UserProfileRepository(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
