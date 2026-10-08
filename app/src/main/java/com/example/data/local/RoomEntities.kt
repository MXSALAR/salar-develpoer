package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "encrypted_notes")
data class EncryptedNoteEntity(
  @PrimaryKey val id: String,
  val title: String,
  val subjectName: String,
  val rawContent: String,
  val isEncrypted: Boolean,
  val encryptionAlgorithm: String,
  val lastModifiedMillis: Long,
  val isPinned: Boolean
)

@Entity(tableName = "test_results")
data class TestResultEntity(
  @PrimaryKey val id: String,
  val testTitle: String,
  val subjectName: String?,
  val score: Int,
  val totalQuestions: Int,
  val correctCount: Int,
  val wrongCount: Int,
  val timeTakenSeconds: Int,
  val dateMillis: Long,
  val weakTopicsString: String // comma separated
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
  @PrimaryKey val id: String,
  val title: String,
  val dateMillis: Long,
  val eventTypeName: String,
  val notes: String,
  val isCompleted: Boolean
)

@Entity(tableName = "offline_downloads")
data class OfflineDownloadEntity(
  @PrimaryKey val itemId: String,
  val itemType: String, // "STUDY_MATERIAL" or "VIDEO_LECTURE"
  val downloadedAtMillis: Long
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
  @PrimaryKey val id: String,
  val subjectName: String,
  val topic: String,
  val questionText: String,
  val optionsRaw: String, // delimiter: |||
  val correctOptionIndex: Int,
  val explanation: String,
  val highYieldTip: String = "",
  val academicClassName: String = "CLASS_11"
)

// Extension converters for Room Entity <-> Domain Model
fun QuizQuestionEntity.toQuizQuestion(): com.example.data.model.QuizQuestion {
  val subjectEnum = runCatching { com.example.data.model.MDCATSubject.valueOf(subjectName) }
    .getOrDefault(com.example.data.model.MDCATSubject.BIOLOGY)
  val academicClassEnum = runCatching { com.example.data.model.AcademicClass.valueOf(academicClassName) }
    .getOrDefault(com.example.data.model.AcademicClass.CLASS_11)
  val optionsList = if (optionsRaw.contains("|||")) {
    optionsRaw.split("|||")
  } else {
    optionsRaw.lines().filter { it.isNotBlank() }
  }
  return com.example.data.model.QuizQuestion(
    id = id,
    subject = subjectEnum,
    topic = topic,
    questionText = questionText,
    options = optionsList,
    correctOptionIndex = correctOptionIndex,
    explanation = explanation,
    highYieldTip = highYieldTip,
    academicClass = academicClassEnum
  )
}

fun com.example.data.model.QuizQuestion.toEntity(): QuizQuestionEntity {
  return QuizQuestionEntity(
    id = id,
    subjectName = subject.name,
    topic = topic,
    questionText = questionText,
    optionsRaw = options.joinToString("|||"),
    correctOptionIndex = correctOptionIndex,
    explanation = explanation,
    highYieldTip = highYieldTip,
    academicClassName = academicClass.name
  )
}

// --- DAOs ---

@Dao
interface NoteDao {
  @Query("SELECT * FROM encrypted_notes ORDER BY isPinned DESC, lastModifiedMillis DESC")
  fun getAllNotes(): Flow<List<EncryptedNoteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: EncryptedNoteEntity)

  @Query("DELETE FROM encrypted_notes WHERE id = :id")
  suspend fun deleteNote(id: String)
}

@Dao
interface TestResultDao {
  @Query("SELECT * FROM test_results ORDER BY dateMillis DESC")
  fun getAllTestResults(): Flow<List<TestResultEntity>>

  @Query("SELECT COUNT(*) FROM test_results")
  suspend fun getTestResultCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTestResult(result: TestResultEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTestResults(results: List<TestResultEntity>)
}

@Dao
interface CalendarEventDao {
  @Query("SELECT * FROM calendar_events ORDER BY dateMillis ASC")
  fun getAllEvents(): Flow<List<CalendarEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: CalendarEventEntity)

  @Query("UPDATE calendar_events SET isCompleted = :isCompleted WHERE id = :id")
  suspend fun updateEventStatus(id: String, isCompleted: Boolean)

  @Query("DELETE FROM calendar_events WHERE id = :id")
  suspend fun deleteEvent(id: String)
}

@Dao
interface OfflineDao {
  @Query("SELECT itemId FROM offline_downloads WHERE itemType = :itemType")
  fun getDownloadedItemIds(itemType: String): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDownload(download: OfflineDownloadEntity)

  @Query("DELETE FROM offline_downloads WHERE itemId = :itemId")
  suspend fun removeDownload(itemId: String)

  @Query("SELECT EXISTS(SELECT 1 FROM offline_downloads WHERE itemId = :itemId)")
  suspend fun isDownloaded(itemId: String): Boolean
}

@Dao
interface QuizQuestionDao {
  @Query("SELECT * FROM quiz_questions")
  fun getAllQuestions(): Flow<List<QuizQuestionEntity>>

  @Query("SELECT * FROM quiz_questions WHERE subjectName = :subjectName")
  fun getQuestionsBySubject(subjectName: String): Flow<List<QuizQuestionEntity>>

  @Query("SELECT * FROM quiz_questions WHERE academicClassName = :academicClassName")
  fun getQuestionsByClass(academicClassName: String): Flow<List<QuizQuestionEntity>>

  @Query("SELECT * FROM quiz_questions WHERE id = :id")
  suspend fun getQuestionById(id: String): QuizQuestionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestion(question: QuizQuestionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

  @Query("DELETE FROM quiz_questions WHERE id = :id")
  suspend fun deleteQuestion(id: String)

  @Query("SELECT COUNT(*) FROM quiz_questions")
  suspend fun getQuestionCount(): Int
}

// --- Database ---

@Database(
  entities = [
    EncryptedNoteEntity::class,
    TestResultEntity::class,
    CalendarEventEntity::class,
    OfflineDownloadEntity::class,
    QuizQuestionEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun noteDao(): NoteDao
  abstract fun testResultDao(): TestResultDao
  abstract fun calendarEventDao(): CalendarEventDao
  abstract fun offlineDao(): OfflineDao
  abstract fun quizQuestionDao(): QuizQuestionDao
}
