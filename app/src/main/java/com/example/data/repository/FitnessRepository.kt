package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.ExerciseEntity
import com.example.data.local.FitnessDao
import com.example.data.local.FitnessDatabase
import com.example.data.local.GymAttendanceEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

private val Context.appDataStore by preferencesDataStore(name = "fitpulse_apple_health_prefs")

data class DraftSetEntry(
    val localId: Long = 0L,
    val exerciseName: String,
    val muscleGroup: String,
    val exerciseOrder: Int = 0,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val durationSeconds: Int = 0,
    val distanceKm: Float = 0f,
    val restSeconds: Int = 90,
    val rpe: Float = 8.0f,
    val setType: String = "NORMAL",
    val status: String = "COMPLETED", // COMPLETED, WARMUP, SKIPPED
    val isBodyweight: Boolean = false,
    val isAssisted: Boolean = false,
    val notes: String = ""
)

sealed class AttendanceCheckInResult {
    data class Success(val id: Long, val dateKey: String) : AttendanceCheckInResult()
    data class DuplicateDateBlocked(val dateKey: String) : AttendanceCheckInResult()
    data class InvalidInput(val message: String) : AttendanceCheckInResult()
}

class FitnessRepository(
    private val context: Context,
    private val dao: FitnessDao
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeModeFlow: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.LIGHT.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.LIGHT)
    }

    val userProfileFlow: Flow<UserProfileEntity?> = dao.observeUserProfile()
    val attendanceRecordsFlow: Flow<List<GymAttendanceEntity>> = dao.observeAllAttendance()
    val activeExercisesFlow: Flow<List<ExerciseEntity>> = dao.observeActiveExercises()
    val allExercisesFlow: Flow<List<ExerciseEntity>> = dao.observeAllExercises()
    val workoutTemplatesFlow: Flow<List<WorkoutTemplateEntity>> = dao.observeWorkoutTemplates()
    val completedSessionsFlow: Flow<List<WorkoutSessionEntity>> = dao.observeCompletedWorkoutSessions()
    val draftSessionsFlow: Flow<List<WorkoutSessionEntity>> = dao.observeDraftWorkoutSessions()
    val allWorkoutSetsFlow: Flow<List<WorkoutSetEntity>> = dao.observeAllWorkoutSets()

    suspend fun initializeIfNeeded() {
        FitnessDatabase.ensureInitialized(dao)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
        val currentProfile = dao.getUserProfileSnapshot() ?: UserProfileEntity()
        dao.upsertUserProfile(
            currentProfile.copy(
                id = 1,
                preferredThemeMode = mode.name,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        dao.upsertUserProfile(
            profile.copy(
                id = 1,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
    }

    // --- GYM ATTENDANCE CRUD ---

    suspend fun recordGymAttendance(
        dateKey: String,
        note: String,
        durationMinutes: Int = 60,
        allowMultipleForSameDate: Boolean = false
    ): AttendanceCheckInResult {
        val trimmedDate = dateKey.trim()
        if (!trimmedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            return AttendanceCheckInResult.InvalidInput("Please enter a valid date in YYYY-MM-DD format.")
        }
        val todayKey = AnalyticsAndInsightsEngine.todayDateKey()
        if (trimmedDate > todayKey) {
            return AttendanceCheckInResult.InvalidInput("Cannot record completed attendance for a future date ($trimmedDate).")
        }

        val existingCount = dao.getAttendanceCountForDate(trimmedDate)
        if (existingCount > 0 && !allowMultipleForSameDate) {
            return AttendanceCheckInResult.DuplicateDateBlocked(trimmedDate)
        }

        val now = System.currentTimeMillis()
        val id = dao.insertAttendance(
            GymAttendanceEntity(
                dateKey = trimmedDate,
                checkInTimestampMillis = now,
                note = note.trim(),
                durationMinutes = durationMinutes.coerceIn(5, 360)
            )
        )
        return AttendanceCheckInResult.Success(id = id, dateKey = trimmedDate)
    }

    suspend fun updateGymAttendance(record: GymAttendanceEntity) {
        dao.updateAttendance(record)
    }

    suspend fun deleteGymAttendance(id: Long) {
        dao.deleteAttendanceById(id)
    }

    // --- WORKOUT SESSION & SET LOGGING ---

    suspend fun getSetsForSession(sessionId: Long): List<WorkoutSetEntity> {
        return dao.getSetsForSession(sessionId)
    }

    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity? {
        return dao.getWorkoutSessionById(sessionId)
    }

    suspend fun saveWorkoutSession(
        existingSessionId: Long? = null,
        title: String,
        category: String,
        dateKey: String,
        durationMinutes: Int,
        notes: String,
        isDraft: Boolean,
        sets: List<DraftSetEntry>,
        userBodyweightKg: Float,
        autoRecordAttendanceOnComplete: Boolean = true
    ): Long {
        val cleanTitle = title.trim().ifBlank { "Strength Session" }
        val cleanDateKey = dateKey.trim().ifBlank { AnalyticsAndInsightsEngine.todayDateKey() }
        val timestampMillis = if (cleanDateKey == AnalyticsAndInsightsEngine.todayDateKey()) {
            System.currentTimeMillis()
        } else {
            AnalyticsAndInsightsEngine.dateKeyToEpochMillis(cleanDateKey)
        }

        val completedValidSets = sets.filter { it.status == "COMPLETED" && it.reps > 0 && it.weightKg >= 0f }
        val totalVolumeKg = completedValidSets.fold(0f) { acc, s ->
            acc + FitnessDatabase.calculateSetVolumeKg(
                weightKg = s.weightKg,
                reps = s.reps,
                status = s.status,
                isBodyweight = s.isBodyweight,
                isAssisted = s.isAssisted,
                userBodyweightKg = userBodyweightKg
            )
        }
        val distinctExercisesCount = sets.map { it.exerciseName.trim().lowercase() }.distinct().size
        val safeDuration = durationMinutes.coerceAtLeast(1)
        val estimatedCalories = ((safeDuration * 5.2f) + (completedValidSets.size * 7.5f)).roundToInt()

        // Evaluate whether any set beat historical PRs
        val existingAllSets = dao.getAllWorkoutSetsSnapshot()
            .filter { it.sessionId != (existingSessionId ?: -1L) && it.status == "COMPLETED" }
        val max1RmByExercise = existingAllSets
            .groupBy { it.exerciseName.lowercase() }
            .mapValues { (_, list) ->
                list.maxOfOrNull { FitnessDatabase.calculateEpleyOneRepMax(it.weightKg, it.reps) } ?: 0f
            }

        var prAchievedCount = 0
        val processedSetEntities = mutableListOf<WorkoutSetEntity>()
        val runningMaxByExercise = max1RmByExercise.toMutableMap()

        sets.forEachIndexed { idx, draft ->
            val current1Rm = if (draft.status == "COMPLETED" && draft.reps > 0) {
                val effectiveLoad = if (draft.isBodyweight && draft.weightKg <= 0f) userBodyweightKg else draft.weightKg
                FitnessDatabase.calculateEpleyOneRepMax(effectiveLoad, draft.reps)
            } else {
                0f
            }
            val exKey = draft.exerciseName.trim().lowercase()
            val prevBest = runningMaxByExercise[exKey] ?: 0f
            val isNewPr = !isDraft && draft.status == "COMPLETED" && current1Rm > prevBest && current1Rm > 0f
            if (isNewPr) {
                prAchievedCount++
                runningMaxByExercise[exKey] = current1Rm
            }

            processedSetEntities.add(
                WorkoutSetEntity(
                    sessionId = existingSessionId ?: 0L,
                    dateKey = cleanDateKey,
                    exerciseName = draft.exerciseName.trim(),
                    muscleGroup = draft.muscleGroup.trim().ifBlank { "Full Body" },
                    exerciseOrder = draft.exerciseOrder,
                    setOrder = idx,
                    setNumber = draft.setNumber,
                    weightKg = draft.weightKg.coerceAtLeast(0f),
                    reps = draft.reps.coerceAtLeast(0),
                    durationSeconds = draft.durationSeconds,
                    distanceKm = draft.distanceKm,
                    restSeconds = draft.restSeconds,
                    rpe = draft.rpe,
                    setType = draft.setType,
                    status = draft.status,
                    isPersonalRecord = isNewPr,
                    isBodyweight = draft.isBodyweight,
                    isAssisted = draft.isAssisted,
                    notes = draft.notes.trim()
                )
            )
        }

        val sessionEntity = WorkoutSessionEntity(
            id = existingSessionId ?: 0L,
            title = cleanTitle,
            category = category.ifBlank { "Strength" },
            dateKey = cleanDateKey,
            timestampMillis = timestampMillis,
            durationMinutes = safeDuration,
            totalVolumeKg = totalVolumeKg,
            totalSets = completedValidSets.size,
            exerciseCount = distinctExercisesCount,
            caloriesBurned = estimatedCalories,
            notes = notes.trim(),
            prCount = prAchievedCount,
            isDraft = isDraft
        )

        val savedSessionId = if (existingSessionId != null && existingSessionId > 0L) {
            dao.updateWorkoutSession(sessionEntity)
            dao.deleteSetsForSession(existingSessionId)
            existingSessionId
        } else {
            dao.insertWorkoutSession(sessionEntity)
        }

        dao.insertWorkoutSets(
            processedSetEntities.map { it.copy(sessionId = savedSessionId) }
        )

        // Automatically ensure gym attendance is recorded for completed workout dates
        if (!isDraft && autoRecordAttendanceOnComplete) {
            val countOnDate = dao.getAttendanceCountForDate(cleanDateKey)
            if (countOnDate == 0) {
                dao.insertAttendance(
                    GymAttendanceEntity(
                        dateKey = cleanDateKey,
                        checkInTimestampMillis = timestampMillis,
                        note = "Workout: $cleanTitle",
                        durationMinutes = safeDuration
                    )
                )
            }
        }

        return savedSessionId
    }

    suspend fun deleteWorkoutSession(sessionId: Long) {
        dao.deleteSetsForSession(sessionId)
        dao.deleteWorkoutSession(sessionId)
    }

    // --- WORKOUT ROUTINE / TEMPLATE CRUD ---

    suspend fun saveWorkoutRoutine(
        id: Long? = null,
        name: String,
        category: String,
        targetDurationMin: Int,
        exercisesList: List<String>
    ): Long {
        val cleanName = name.trim().ifBlank { "Custom Routine" }
        val csv = exercisesList.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
        val entity = WorkoutTemplateEntity(
            id = id ?: 0L,
            name = cleanName,
            category = category.ifBlank { "Strength" },
            targetDurationMin = targetDurationMin.coerceIn(10, 240),
            exercisesCsv = csv,
            isCustom = true
        )
        return if (id != null && id > 0L) {
            dao.updateWorkoutTemplate(entity)
            id
        } else {
            dao.insertWorkoutTemplate(entity)
        }
    }

    suspend fun deleteWorkoutRoutine(id: Long) {
        dao.deleteWorkoutTemplate(id)
    }

    // --- EXERCISE LIBRARY CRUD ---

    suspend fun saveExercise(
        id: Long? = null,
        name: String,
        muscleGroup: String,
        secondaryMuscles: String,
        equipment: String,
        category: String,
        instructions: String,
        isBodyweight: Boolean = false,
        isAssisted: Boolean = false
    ): Long {
        val cleanName = name.trim()
        val entity = ExerciseEntity(
            id = id ?: 0L,
            name = cleanName,
            muscleGroup = muscleGroup.trim().ifBlank { "Full Body" },
            secondaryMuscles = secondaryMuscles.trim(),
            equipment = equipment.trim().ifBlank { "Barbell" },
            category = category.trim().ifBlank { "Strength" },
            instructions = instructions.trim().ifBlank { "Perform with controlled tempo and full range of motion." },
            isCustom = true,
            isArchived = false,
            isBodyweight = isBodyweight,
            isAssisted = isAssisted
        )
        return if (id != null && id > 0L) {
            dao.updateExercise(entity)
            id
        } else {
            dao.insertExercise(entity)
        }
    }

    suspend fun setExerciseArchived(id: Long, archived: Boolean) {
        dao.setExerciseArchived(id, archived)
    }

    suspend fun deleteExercisePreservingHistory(id: Long) {
        // Deletes only the catalog entry in `exercises`; historical `workout_sets` store `exerciseName`
        // directly, so past workouts and charts remain intact.
        dao.deleteExerciseById(id)
    }

    // --- FULL APP BACKUP & RESTORE (EXPORT / IMPORT / AUTO-SNAPSHOT) ---

    suspend fun createFullBackupJsonString(themeMode: ThemeMode): String {
        return BackupAndRestoreManager.createFullBackupJsonString(
            dao = dao,
            themeMode = themeMode
        )
    }

    suspend fun saveLocalBackupSnapshot(
        themeMode: ThemeMode,
        isAutoBackup: Boolean
    ): LocalBackupSnapshotInfo {
        return BackupAndRestoreManager.saveLocalSnapshot(
            context = context,
            dao = dao,
            themeMode = themeMode,
            isAutoBackup = isAutoBackup
        )
    }

    fun listLocalBackupSnapshots(): List<LocalBackupSnapshotInfo> {
        return BackupAndRestoreManager.listLocalSnapshots(context)
    }

    suspend fun exportBackupToUri(
        themeMode: ThemeMode,
        destinationUri: Uri
    ): Boolean {
        return BackupAndRestoreManager.exportBackupToUri(
            context = context,
            dao = dao,
            themeMode = themeMode,
            destinationUri = destinationUri
        )
    }

    suspend fun exportBackupPdfToUri(
        themeMode: ThemeMode,
        destinationUri: Uri
    ): Boolean {
        return BackupAndRestoreManager.exportBackupPdfToUri(
            context = context,
            dao = dao,
            themeMode = themeMode,
            destinationUri = destinationUri
        )
    }

    suspend fun importBackupFromUri(
        sourceUri: Uri,
        replaceExisting: Boolean
    ): Result<BackupRestoreSummary> {
        return BackupAndRestoreManager.importBackupFromUri(
            context = context,
            dao = dao,
            sourceUri = sourceUri,
            replaceExisting = replaceExisting,
            onThemeRestored = { restoredTheme -> setThemeMode(restoredTheme) }
        )
    }

    suspend fun restoreFromBackupJsonString(
        jsonText: String,
        replaceExisting: Boolean
    ): Result<BackupRestoreSummary> {
        return BackupAndRestoreManager.restoreFromBackupJsonString(
            jsonText = jsonText,
            dao = dao,
            replaceExisting = replaceExisting,
            onThemeRestored = { restoredTheme -> setThemeMode(restoredTheme) }
        )
    }

    // --- CLEAR / RESET DATA ---

    suspend fun clearAllUserData() {
        dao.clearAllAttendance()
        dao.clearAllSets()
        dao.clearAllSessions()
        dao.clearAllTemplates()
        dao.clearAllExercises()
        dao.clearUserProfile()
        FitnessDatabase.ensureInitialized(dao)
    }
}
