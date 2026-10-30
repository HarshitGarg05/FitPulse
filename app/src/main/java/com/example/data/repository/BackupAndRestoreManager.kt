package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.ExerciseEntity
import com.example.data.local.FitnessDao
import com.example.data.local.FitnessDatabase
import com.example.data.local.GymAttendanceEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupSnapshotInfo(
    val fileName: String,
    val filePath: String,
    val createdAtLabel: String,
    val sizeBytes: Long,
    val attendanceCount: Int,
    val sessionsCount: Int,
    val setsCount: Int,
    val exercisesCount: Int,
    val routinesCount: Int,
    val isAutoBackup: Boolean
)

data class BackupRestoreSummary(
    val attendanceRestored: Int,
    val sessionsRestored: Int,
    val setsRestored: Int,
    val exercisesRestored: Int,
    val routinesRestored: Int
)

object BackupAndRestoreManager {

    private const val BACKUP_DIR_NAME = "fitpulse_backups"
    private const val AUTO_BACKUP_FILE_NAME = "FitPulse_AutoBackup_Latest.json"
    private const val BACKUP_SCHEMA_VERSION = FitnessDatabase.CURRENT_SCHEMA_VERSION
    private const val PDF_EMBEDDED_START_MARKER = "%FITPULSE_DB_BACKUP_B64_START"
    private const val PDF_EMBEDDED_END_MARKER = "%FITPULSE_DB_BACKUP_B64_END"

    fun defaultExportFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date(nowMillis))
        return "FitPulse_Backup_$stamp.json"
    }

    fun defaultExportPdfBackupFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date(nowMillis))
        return "FitPulse_Database_Backup_$stamp.pdf"
    }

    private fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Serializes 100% of the app's stored data (Profile, Theme, Attendance, Exercises,
     * Saved Workout Plans/Templates, Completed & Draft Workout Sessions, and all Workout Sets)
     * into a formatted JSON string.
     */
    suspend fun createFullBackupJsonString(
        dao: FitnessDao,
        themeMode: ThemeMode,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val profile = dao.getUserProfileSnapshot() ?: UserProfileEntity()
        val attendance = dao.getAllAttendanceSnapshot()
        val exercises = dao.getAllExercisesSnapshot()
        val templates = dao.getAllTemplatesSnapshot()
        val sessions = dao.getAllSessionsSnapshot()
        val sets = dao.getAllWorkoutSetsSnapshot()

        val readableDate = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date(nowMillis))

        val root = JSONObject().apply {
            put("app", "FitPulse")
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("exportedAtMillis", nowMillis)
            put("exportedAtReadable", readableDate)
            put("themeMode", themeMode.name)

            put(
                "summary",
                JSONObject().apply {
                    put("attendanceCount", attendance.size)
                    put("sessionsCount", sessions.size)
                    put("setsCount", sets.size)
                    put("exercisesCount", exercises.size)
                    put("routinesCount", templates.size)
                }
            )

            put(
                "profile",
                JSONObject().apply {
                    put("id", profile.id)
                    put("displayName", profile.displayName)
                    put("preferredWeightUnit", profile.preferredWeightUnit)
                    put("weightKg", profile.weightKg.toDouble())
                    put("heightCm", profile.heightCm.toDouble())
                    put("fitnessLevel", profile.fitnessLevel)
                    put("primaryObjective", profile.primaryObjective)
                    put("weeklyAttendanceGoal", profile.weeklyAttendanceGoal)
                    put("trainingDaysCsv", profile.trainingDaysCsv)
                    put("allowMultipleVisitsPerDay", profile.allowMultipleVisitsPerDay)
                    put("defaultRestSeconds", profile.defaultRestSeconds)
                    put("overloadIncrementKg", profile.overloadIncrementKg.toDouble())
                    put("barbellWeightKg", profile.barbellWeightKg.toDouble())
                    put("autoStartRestTimer", profile.autoStartRestTimer)
                    put("autoCheckInOnWorkoutComplete", profile.autoCheckInOnWorkoutComplete)
                    put("preferredThemeMode", themeMode.name)
                    put("lastUpdatedMillis", profile.lastUpdatedMillis)
                }
            )

            put(
                "attendanceRecords",
                JSONArray().apply {
                    attendance.forEach { rec ->
                        put(
                            JSONObject().apply {
                                put("id", rec.id)
                                put("dateKey", rec.dateKey)
                                put("checkInTimestampMillis", rec.checkInTimestampMillis)
                                put("note", rec.note)
                                put("durationMinutes", rec.durationMinutes)
                            }
                        )
                    }
                }
            )

            put(
                "exercises",
                JSONArray().apply {
                    exercises.forEach { ex ->
                        put(
                            JSONObject().apply {
                                put("id", ex.id)
                                put("name", ex.name)
                                put("muscleGroup", ex.muscleGroup)
                                put("secondaryMuscles", ex.secondaryMuscles)
                                put("equipment", ex.equipment)
                                put("category", ex.category)
                                put("instructions", ex.instructions)
                                put("isCustom", ex.isCustom)
                                put("isArchived", ex.isArchived)
                                put("isBodyweight", ex.isBodyweight)
                                put("isAssisted", ex.isAssisted)
                            }
                        )
                    }
                }
            )

            put(
                "workoutTemplates",
                JSONArray().apply {
                    templates.forEach { tpl ->
                        put(
                            JSONObject().apply {
                                put("id", tpl.id)
                                put("name", tpl.name)
                                put("category", tpl.category)
                                put("targetDurationMin", tpl.targetDurationMin)
                                put("exercisesCsv", tpl.exercisesCsv)
                                put("isCustom", tpl.isCustom)
                                put("createdAtMillis", tpl.createdAtMillis)
                            }
                        )
                    }
                }
            )

            put(
                "workoutSessions",
                JSONArray().apply {
                    sessions.forEach { sess ->
                        put(
                            JSONObject().apply {
                                put("id", sess.id)
                                put("title", sess.title)
                                put("category", sess.category)
                                put("dateKey", sess.dateKey)
                                put("timestampMillis", sess.timestampMillis)
                                put("durationMinutes", sess.durationMinutes)
                                put("totalVolumeKg", sess.totalVolumeKg.toDouble())
                                put("totalSets", sess.totalSets)
                                put("exerciseCount", sess.exerciseCount)
                                put("caloriesBurned", sess.caloriesBurned)
                                put("notes", sess.notes)
                                put("prCount", sess.prCount)
                                put("isDraft", sess.isDraft)
                            }
                        )
                    }
                }
            )

            put(
                "workoutSets",
                JSONArray().apply {
                    sets.forEach { s ->
                        put(
                            JSONObject().apply {
                                put("id", s.id)
                                put("sessionId", s.sessionId)
                                put("dateKey", s.dateKey)
                                put("exerciseName", s.exerciseName)
                                put("muscleGroup", s.muscleGroup)
                                put("exerciseOrder", s.exerciseOrder)
                                put("setOrder", s.setOrder)
                                put("setNumber", s.setNumber)
                                put("weightKg", s.weightKg.toDouble())
                                put("reps", s.reps)
                                put("durationSeconds", s.durationSeconds)
                                put("distanceKm", s.distanceKm.toDouble())
                                put("restSeconds", s.restSeconds)
                                put("rpe", s.rpe.toDouble())
                                put("setType", s.setType)
                                put("status", s.status)
                                put("isPersonalRecord", s.isPersonalRecord)
                                put("isBodyweight", s.isBodyweight)
                                put("isAssisted", s.isAssisted)
                                put("notes", s.notes)
                            }
                        )
                    }
                }
            )
        }

        return root.toString(2)
    }

    /**
     * Saves a local snapshot file inside `filesDir/fitpulse_backups/`.
     * If `isAutoBackup = true`, updates `FitPulse_AutoBackup_Latest.json`.
     * If `isAutoBackup = false`, writes a timestamped snapshot and prunes older manual snapshots beyond 10.
     */
    suspend fun saveLocalSnapshot(
        context: Context,
        dao: FitnessDao,
        themeMode: ThemeMode,
        isAutoBackup: Boolean
    ): LocalBackupSnapshotInfo {
        val now = System.currentTimeMillis()
        val json = createFullBackupJsonString(dao, themeMode, now)
        val dir = getBackupDir(context)
        val fileName = if (isAutoBackup) {
            AUTO_BACKUP_FILE_NAME
        } else {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
            "FitPulse_Snapshot_$stamp.json"
        }
        val targetFile = File(dir, fileName)
        targetFile.writeText(json, Charsets.UTF_8)

        if (!isAutoBackup) {
            val manualFiles = dir.listFiles { f ->
                f.isFile && f.name.endsWith(".json") && f.name != AUTO_BACKUP_FILE_NAME
            }?.sortedByDescending { it.lastModified() }.orEmpty()
            if (manualFiles.size > 8) {
                manualFiles.drop(8).forEach { it.delete() }
            }
        }

        return parseSnapshotInfo(targetFile)
    }

    fun listLocalSnapshots(context: Context): List<LocalBackupSnapshotInfo> {
        val dir = getBackupDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }.orEmpty()
        return files
            .map { parseSnapshotInfo(it) }
            .sortedWith(
                compareByDescending<LocalBackupSnapshotInfo> { it.isAutoBackup }
                    .thenByDescending { File(it.filePath).lastModified() }
            )
    }

    private fun parseSnapshotInfo(file: File): LocalBackupSnapshotInfo {
        return runCatching {
            val text = file.readText(Charsets.UTF_8)
            val root = JSONObject(text)
            val exportedReadable = root.optString(
                "exportedAtReadable",
                SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date(file.lastModified()))
            )
            val summary = root.optJSONObject("summary")
            val attendanceCount = summary?.optInt("attendanceCount")
                ?: root.optJSONArray("attendanceRecords")?.length() ?: 0
            val sessionsCount = summary?.optInt("sessionsCount")
                ?: root.optJSONArray("workoutSessions")?.length() ?: 0
            val setsCount = summary?.optInt("setsCount")
                ?: root.optJSONArray("workoutSets")?.length() ?: 0
            val exercisesCount = summary?.optInt("exercisesCount")
                ?: root.optJSONArray("exercises")?.length() ?: 0
            val routinesCount = summary?.optInt("routinesCount")
                ?: root.optJSONArray("workoutTemplates")?.length() ?: 0

            LocalBackupSnapshotInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                createdAtLabel = exportedReadable,
                sizeBytes = file.length(),
                attendanceCount = attendanceCount,
                sessionsCount = sessionsCount,
                setsCount = setsCount,
                exercisesCount = exercisesCount,
                routinesCount = routinesCount,
                isAutoBackup = file.name == AUTO_BACKUP_FILE_NAME
            )
        }.getOrElse {
            LocalBackupSnapshotInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                createdAtLabel = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date(file.lastModified())),
                sizeBytes = file.length(),
                attendanceCount = 0,
                sessionsCount = 0,
                setsCount = 0,
                exercisesCount = 0,
                routinesCount = 0,
                isAutoBackup = file.name == AUTO_BACKUP_FILE_NAME
            )
        }
    }

    /**
     * Writes the full backup JSON directly to a user-selected document Uri.
     */
    suspend fun exportBackupToUri(
        context: Context,
        dao: FitnessDao,
        themeMode: ThemeMode,
        destinationUri: Uri
    ): Boolean {
        return runCatching {
            val json = createFullBackupJsonString(dao, themeMode)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    /**
     * Exports the full Room database state to a user-defined PDF file AND embeds the
     * Base64-encoded JSON database snapshot inside a PDF trailer comment block so the
     * generated PDF file is both human-readable AND 100% restorable via Import Backup.
     */
    suspend fun exportBackupPdfToUri(
        context: Context,
        dao: FitnessDao,
        themeMode: ThemeMode,
        destinationUri: Uri
    ): Boolean {
        return runCatching {
            val profile = dao.getUserProfileSnapshot() ?: UserProfileEntity()
            val attendance = dao.getAllAttendanceSnapshot()
            val exercises = dao.getAllExercisesSnapshot()
            val completedSessions = dao.getAllSessionsSnapshot().filter { !it.isDraft }
            val allSets = dao.getAllWorkoutSetsSnapshot()

            val attendanceOverview = AnalyticsAndInsightsEngine.buildAttendanceOverview(
                records = attendance,
                profile = profile
            )
            val dashboardAnalytics = AnalyticsAndInsightsEngine.buildDashboardAnalytics(
                dateRange = AnalyticsDateRange.ALL_TIME,
                attendanceRecords = attendance,
                exercises = exercises,
                completedSessions = completedSessions,
                allSets = allSets,
                profile = profile
            )

            val generatedPdf = PdfReportGenerator.generateFormattedPdfReport(
                context = context,
                profile = profile,
                attendanceOverview = attendanceOverview,
                attendanceRecords = attendance,
                dashboardAnalytics = dashboardAnalytics,
                completedSessions = completedSessions,
                allSets = allSets
            )

            val jsonSnapshot = createFullBackupJsonString(dao, themeMode)
            val base64Payload = java.util.Base64.getEncoder()
                .encodeToString(jsonSnapshot.toByteArray(Charsets.UTF_8))
            val trailerBlock = "\n$PDF_EMBEDDED_START_MARKER\n%$base64Payload\n$PDF_EMBEDDED_END_MARKER\n"

            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                generatedPdf.file.inputStream().use { input ->
                    input.copyTo(out)
                }
                out.write(trailerBlock.toByteArray(Charsets.US_ASCII))
                out.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    /**
     * Extracts the JSON backup string from either a raw `.json` file or a FitPulse `.pdf`
     * backup file containing an embedded `%FITPULSE_DB_BACKUP_B64_START` payload.
     */
    fun extractJsonFromRawBackupContent(rawContent: String): String {
        val trimmed = rawContent.trim()
        if (trimmed.contains(PDF_EMBEDDED_START_MARKER) && trimmed.contains(PDF_EMBEDDED_END_MARKER)) {
            val startIdx = trimmed.indexOf(PDF_EMBEDDED_START_MARKER) + PDF_EMBEDDED_START_MARKER.length
            val endIdx = trimmed.indexOf(PDF_EMBEDDED_END_MARKER)
            if (startIdx in 1 until endIdx) {
                val base64Chunk = trimmed.substring(startIdx, endIdx)
                    .lines()
                    .joinToString("") { line -> line.trim().removePrefix("%").trim() }
                val decodedBytes = java.util.Base64.getDecoder().decode(base64Chunk)
                return String(decodedBytes, Charsets.UTF_8)
            }
        }
        return trimmed
    }

    /**
     * Reads a backup JSON or FitPulse PDF backup from a user-selected document Uri and restores it.
     */
    suspend fun importBackupFromUri(
        context: Context,
        dao: FitnessDao,
        sourceUri: Uri,
        replaceExisting: Boolean,
        onThemeRestored: suspend (ThemeMode) -> Unit
    ): Result<BackupRestoreSummary> {
        return runCatching {
            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes()
            } ?: throw IllegalArgumentException("Unable to read the selected backup file.")
            val rawText = String(rawBytes, Charsets.UTF_8)
            val jsonText = extractJsonFromRawBackupContent(rawText)
            restoreFromBackupJsonString(
                jsonText = jsonText,
                dao = dao,
                replaceExisting = replaceExisting,
                onThemeRestored = onThemeRestored
            ).getOrThrow()
        }
    }

    /**
     * Restores all app data from a JSON string (or PDF backup payload).
     * When `replaceExisting = true`, replaces all tables with the backup's contents.
     * When `replaceExisting = false`, intelligently merges records without losing existing data.
     */
    suspend fun restoreFromBackupJsonString(
        jsonText: String,
        dao: FitnessDao,
        replaceExisting: Boolean,
        onThemeRestored: suspend (ThemeMode) -> Unit
    ): Result<BackupRestoreSummary> {
        return runCatching {
            val normalizedJson = extractJsonFromRawBackupContent(jsonText)
            val root = JSONObject(normalizedJson)

            // 1. Parse Theme Mode if present
            val profileObj = root.optJSONObject("profile")
            val themeRaw = root.optString("themeMode", "").ifBlank {
                profileObj?.optString("preferredThemeMode", "") ?: ""
            }
            val resolvedThemeMode = runCatching { ThemeMode.valueOf(themeRaw) }.getOrNull()
            if (resolvedThemeMode != null) {
                onThemeRestored(resolvedThemeMode)
            }

            // 2. Parse Profile
            val parsedProfile = if (profileObj != null) {
                UserProfileEntity(
                    id = 1,
                    displayName = profileObj.optString("displayName", "Athlete"),
                    preferredWeightUnit = profileObj.optString("preferredWeightUnit", "kg"),
                    weightKg = profileObj.optDouble("weightKg", 75.0).toFloat(),
                    heightCm = profileObj.optDouble("heightCm", 175.0).toFloat(),
                    fitnessLevel = profileObj.optString("fitnessLevel", "Intermediate"),
                    primaryObjective = profileObj.optString("primaryObjective", "Strength & Progressive Overload"),
                    weeklyAttendanceGoal = profileObj.optInt("weeklyAttendanceGoal", 4),
                    trainingDaysCsv = profileObj.optString("trainingDaysCsv", ""),
                    allowMultipleVisitsPerDay = profileObj.optBoolean("allowMultipleVisitsPerDay", false),
                    defaultRestSeconds = profileObj.optInt("defaultRestSeconds", 90),
                    overloadIncrementKg = profileObj.optDouble("overloadIncrementKg", 2.5).toFloat(),
                    barbellWeightKg = profileObj.optDouble("barbellWeightKg", 20.0).toFloat(),
                    autoStartRestTimer = profileObj.optBoolean("autoStartRestTimer", true),
                    autoCheckInOnWorkoutComplete = profileObj.optBoolean("autoCheckInOnWorkoutComplete", true),
                    preferredThemeMode = resolvedThemeMode?.name
                        ?: profileObj.optString("preferredThemeMode", "LIGHT"),
                    lastUpdatedMillis = profileObj.optLong("lastUpdatedMillis", System.currentTimeMillis())
                )
            } else null

            // 3. Parse Attendance
            val attendanceArr = root.optJSONArray("attendanceRecords") ?: JSONArray()
            val parsedAttendance = buildList {
                for (i in 0 until attendanceArr.length()) {
                    val item = attendanceArr.optJSONObject(i) ?: continue
                    val dateKey = item.optString("dateKey", "").trim()
                    if (dateKey.isNotEmpty()) {
                        add(
                            GymAttendanceEntity(
                                id = item.optLong("id", 0L),
                                dateKey = dateKey,
                                checkInTimestampMillis = item.optLong("checkInTimestampMillis", System.currentTimeMillis()),
                                note = item.optString("note", "Gym Session"),
                                durationMinutes = item.optInt("durationMinutes", 60)
                            )
                        )
                    }
                }
            }

            // 4. Parse Exercises
            val exercisesArr = root.optJSONArray("exercises") ?: JSONArray()
            val parsedExercises = buildList {
                for (i in 0 until exercisesArr.length()) {
                    val item = exercisesArr.optJSONObject(i) ?: continue
                    val name = item.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        add(
                            ExerciseEntity(
                                id = item.optLong("id", 0L),
                                name = name,
                                muscleGroup = item.optString("muscleGroup", "Full Body"),
                                secondaryMuscles = item.optString("secondaryMuscles", ""),
                                equipment = item.optString("equipment", "Barbell"),
                                category = item.optString("category", "Strength"),
                                instructions = item.optString("instructions", ""),
                                isCustom = item.optBoolean("isCustom", false),
                                isArchived = item.optBoolean("isArchived", false),
                                isBodyweight = item.optBoolean("isBodyweight", false),
                                isAssisted = item.optBoolean("isAssisted", false)
                            )
                        )
                    }
                }
            }

            // 5. Parse Workout Templates / Saved Plans
            val templatesArr = root.optJSONArray("workoutTemplates") ?: JSONArray()
            val parsedTemplates = buildList {
                for (i in 0 until templatesArr.length()) {
                    val item = templatesArr.optJSONObject(i) ?: continue
                    val name = item.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        add(
                            WorkoutTemplateEntity(
                                id = item.optLong("id", 0L),
                                name = name,
                                category = item.optString("category", "Strength"),
                                targetDurationMin = item.optInt("targetDurationMin", 60),
                                exercisesCsv = item.optString("exercisesCsv", ""),
                                isCustom = item.optBoolean("isCustom", true)
                            )
                        )
                    }
                }
            }

            // 6. Parse Workout Sessions
            val sessionsArr = root.optJSONArray("workoutSessions") ?: JSONArray()
            val parsedSessions = buildList {
                for (i in 0 until sessionsArr.length()) {
                    val item = sessionsArr.optJSONObject(i) ?: continue
                    add(
                        WorkoutSessionEntity(
                            id = item.optLong("id", 0L),
                            title = item.optString("title", "Workout Session"),
                            category = item.optString("category", "Strength"),
                            dateKey = item.optString("dateKey", ""),
                            timestampMillis = item.optLong("timestampMillis", System.currentTimeMillis()),
                            durationMinutes = item.optInt("durationMinutes", 45),
                            totalVolumeKg = item.optDouble("totalVolumeKg", 0.0).toFloat(),
                            totalSets = item.optInt("totalSets", 0),
                            exerciseCount = item.optInt("exerciseCount", 0),
                            caloriesBurned = item.optInt("caloriesBurned", 0),
                            notes = item.optString("notes", ""),
                            prCount = item.optInt("prCount", 0),
                            isDraft = item.optBoolean("isDraft", false)
                        )
                    )
                }
            }

            // 7. Parse Workout Sets
            val setsArr = root.optJSONArray("workoutSets") ?: JSONArray()
            val parsedSets = buildList {
                for (i in 0 until setsArr.length()) {
                    val item = setsArr.optJSONObject(i) ?: continue
                    add(
                        WorkoutSetEntity(
                            id = item.optLong("id", 0L),
                            sessionId = item.optLong("sessionId", 0L),
                            dateKey = item.optString("dateKey", ""),
                            exerciseName = item.optString("exerciseName", ""),
                            muscleGroup = item.optString("muscleGroup", "Full Body"),
                            exerciseOrder = item.optInt("exerciseOrder", 0),
                            setOrder = item.optInt("setOrder", i),
                            setNumber = item.optInt("setNumber", 1),
                            weightKg = item.optDouble("weightKg", 0.0).toFloat(),
                            reps = item.optInt("reps", 0),
                            durationSeconds = item.optInt("durationSeconds", 0),
                            distanceKm = item.optDouble("distanceKm", 0.0).toFloat(),
                            restSeconds = item.optInt("restSeconds", 90),
                            rpe = item.optDouble("rpe", 8.0).toFloat(),
                            setType = item.optString("setType", "NORMAL"),
                            status = item.optString("status", "COMPLETED"),
                            isPersonalRecord = item.optBoolean("isPersonalRecord", false),
                            isBodyweight = item.optBoolean("isBodyweight", false),
                            isAssisted = item.optBoolean("isAssisted", false),
                            notes = item.optString("notes", "")
                        )
                    )
                }
            }

            if (replaceExisting) {
                dao.clearAllSets()
                dao.clearAllSessions()
                dao.clearAllAttendance()
                dao.clearAllTemplates()
                dao.clearAllExercises()
                if (parsedProfile != null) {
                    dao.upsertUserProfile(parsedProfile)
                }
                if (parsedExercises.isNotEmpty()) {
                    dao.insertExercises(parsedExercises)
                } else {
                    FitnessDatabase.ensureInitialized(dao)
                }
                if (parsedTemplates.isNotEmpty()) {
                    dao.insertWorkoutTemplates(parsedTemplates)
                }
                if (parsedAttendance.isNotEmpty()) {
                    dao.insertAttendances(parsedAttendance)
                }
                if (parsedSessions.isNotEmpty()) {
                    dao.insertWorkoutSessions(parsedSessions)
                }
                if (parsedSets.isNotEmpty()) {
                    dao.insertWorkoutSets(parsedSets)
                }
            } else {
                // Smart Merge Mode: preserves current records and merges any missing records from backup
                if (parsedProfile != null) {
                    dao.upsertUserProfile(parsedProfile)
                }

                val existingExercisesByName = dao.getAllExercisesSnapshot().associateBy { it.name.lowercase(Locale.US) }
                parsedExercises.forEach { ex ->
                    val existing = existingExercisesByName[ex.name.lowercase(Locale.US)]
                    if (existing == null) {
                        dao.insertExercise(ex.copy(id = 0L))
                    }
                }

                val existingTemplatesByName = dao.getAllTemplatesSnapshot().associateBy { it.name.lowercase(Locale.US) }
                parsedTemplates.forEach { tpl ->
                    val existing = existingTemplatesByName[tpl.name.lowercase(Locale.US)]
                    if (existing == null) {
                        dao.insertWorkoutTemplate(tpl.copy(id = 0L))
                    } else {
                        dao.updateWorkoutTemplate(tpl.copy(id = existing.id))
                    }
                }

                val existingAttendanceKeys = dao.getAllAttendanceSnapshot()
                    .map { "${it.dateKey}_${it.note}" }
                    .toSet()
                parsedAttendance.forEach { rec ->
                    val key = "${rec.dateKey}_${rec.note}"
                    if (!existingAttendanceKeys.contains(key)) {
                        dao.insertAttendance(rec.copy(id = 0L))
                    }
                }

                val existingSessions = dao.getAllSessionsSnapshot()
                val existingSessionFingerprints = existingSessions
                    .map { "${it.dateKey}_${it.title}_${it.timestampMillis}" }
                    .toSet()
                val setsByOldSessionId = parsedSets.groupBy { it.sessionId }

                parsedSessions.forEach { sess ->
                    val fingerprint = "${sess.dateKey}_${sess.title}_${sess.timestampMillis}"
                    if (!existingSessionFingerprints.contains(fingerprint)) {
                        val newSessionId = dao.insertWorkoutSession(sess.copy(id = 0L))
                        val sessionSets = setsByOldSessionId[sess.id].orEmpty().map {
                            it.copy(id = 0L, sessionId = newSessionId)
                        }
                        if (sessionSets.isNotEmpty()) {
                            dao.insertWorkoutSets(sessionSets)
                        }
                    }
                }
            }

            BackupRestoreSummary(
                attendanceRestored = parsedAttendance.size,
                sessionsRestored = parsedSessions.size,
                setsRestored = parsedSets.size,
                exercisesRestored = parsedExercises.size,
                routinesRestored = parsedTemplates.size
            )
        }
    }
}
