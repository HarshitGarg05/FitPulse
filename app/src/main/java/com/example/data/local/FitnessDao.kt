package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    // --- User Profile ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileSnapshot(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // --- Gym Attendance ---
    @Query("SELECT * FROM gym_attendance ORDER BY dateKey DESC, checkInTimestampMillis DESC")
    fun observeAllAttendance(): Flow<List<GymAttendanceEntity>>

    @Query("SELECT COUNT(*) FROM gym_attendance WHERE dateKey = :dateKey")
    suspend fun getAttendanceCountForDate(dateKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: GymAttendanceEntity): Long

    @Update
    suspend fun updateAttendance(record: GymAttendanceEntity)

    @Query("DELETE FROM gym_attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: Long)

    @Query("DELETE FROM gym_attendance")
    suspend fun clearAllAttendance()

    // --- Exercise Library ---
    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY isArchived ASC, name ASC")
    fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET isArchived = :archived WHERE id = :id")
    suspend fun setExerciseArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Query("DELETE FROM exercises")
    suspend fun clearAllExercises()

    // --- Reusable Workout Templates / Routines ---
    @Query("SELECT * FROM workout_templates ORDER BY id DESC")
    fun observeWorkoutTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT COUNT(*) FROM workout_templates")
    suspend fun getTemplateCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutTemplates(templates: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutTemplate(template: WorkoutTemplateEntity): Long

    @Update
    suspend fun updateWorkoutTemplate(template: WorkoutTemplateEntity)

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun deleteWorkoutTemplate(id: Long)

    @Query("DELETE FROM workout_templates")
    suspend fun clearAllTemplates()

    // --- Workout Sessions (Completed & Drafts) ---
    @Query("SELECT * FROM workout_sessions WHERE isDraft = 0 ORDER BY dateKey DESC, timestampMillis DESC")
    fun observeCompletedWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isDraft = 1 ORDER BY timestampMillis DESC")
    fun observeDraftWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getWorkoutSessionById(sessionId: Long): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteWorkoutSession(sessionId: Long)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllSessions()

    // --- Workout Sets ---
    @Query("SELECT * FROM workout_sets ORDER BY sessionId ASC, exerciseOrder ASC, setNumber ASC")
    fun observeAllWorkoutSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets ORDER BY sessionId ASC, exerciseOrder ASC, setNumber ASC")
    suspend fun getAllWorkoutSetsSnapshot(): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    suspend fun getSetsForSession(sessionId: Long): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long)

    @Query("DELETE FROM workout_sets")
    suspend fun clearAllSets()

    // --- Full Backup & Restore Snapshot Queries ---
    @Query("SELECT * FROM gym_attendance ORDER BY dateKey ASC, checkInTimestampMillis ASC")
    suspend fun getAllAttendanceSnapshot(): List<GymAttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(records: List<GymAttendanceEntity>)

    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getAllExercisesSnapshot(): List<ExerciseEntity>

    @Query("SELECT * FROM workout_templates ORDER BY id ASC")
    suspend fun getAllTemplatesSnapshot(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY id ASC")
    suspend fun getAllSessionsSnapshot(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSessions(sessions: List<WorkoutSessionEntity>)
}
