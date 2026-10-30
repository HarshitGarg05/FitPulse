package com.example.data.repository

import com.example.data.local.ExerciseEntity
import com.example.data.local.FitnessDatabase
import com.example.data.local.GymAttendanceEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class AnalyticsDateRange(val label: String, val shortLabel: String, val days: Int?) {
    DAYS_7("1 Week", "Week", 7),
    DAYS_30("1 Month", "Month", 30),
    DAYS_90("6 Months", "6M", 180),
    YEAR_1("1 Year", "Year", 365),
    ALL_TIME("All Time", "All", null)
}

enum class CalendarAttendanceStatus {
    ATTENDED,
    REST_OR_UNLOGGED
}

data class CalendarDayCell(
    val dateKey: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val status: CalendarAttendanceStatus,
    val visitCount: Int,
    val records: List<GymAttendanceEntity>
)

data class DailyChartPoint(
    val dateKey: String,
    val shortDayLabel: String,
    val fullDateLabel: String,
    val value: Float,
    val secondaryValue: Float = 0f
)

data class AttendanceOverview(
    val todayDateKey: String,
    val attendedToday: Boolean,
    val todayRecords: List<GymAttendanceEntity>,
    val weeklyVisitDays: Int,
    val weeklyTotalCheckIns: Int,
    val monthlyVisitDays: Int,
    val totalAllTimeVisits: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val weeklyGoalDays: Int,
    val weeklyGoalProgress: Float,
    val calendarYear: Int,
    val calendarMonthZeroBased: Int,
    val calendarMonthTitle: String,
    val calendarDays: List<CalendarDayCell>,
    val weeklyAttendanceSeries: List<DailyChartPoint>
)

enum class OverloadStatus(val badgeLabel: String) {
    NEW_PR("New Personal Record"),
    INCREASED_WEIGHT("+Weight Overload"),
    MORE_REPS("+Reps Overload"),
    INCREASED_VOLUME("+Volume Overload"),
    MAINTAINED_OR_LOWER("Maintained Load"),
    BASELINE_ESTABLISHED("Baseline Logged"),
    INSUFFICIENT_DATA("0 Logs Yet")
}

data class ExerciseSessionSummary(
    val sessionId: Long,
    val sessionTitle: String,
    val dateKey: String,
    val dateLabel: String,
    val timestampMillis: Long,
    val topWeightKg: Float,
    val maxRepsAtTopWeight: Int,
    val bestSetReps: Int,
    val totalReps: Int,
    val completedSetCount: Int,
    val sessionVolumeKg: Float,
    val bestEstimatedOneRepMaxKg: Float,
    val isLiveActiveSession: Boolean = false,
    val sets: List<WorkoutSetEntity>
)

data class ExerciseProgressionReport(
    val exerciseId: Long?,
    val exerciseName: String,
    val muscleGroup: String,
    val secondaryMuscles: String,
    val equipment: String,
    val instructions: String,
    val isBodyweight: Boolean,
    val isAssisted: Boolean,
    val totalSessionsLogged: Int,
    val totalCompletedSets: Int,
    val lifetimeVolumeKg: Float,
    val personalRecordWeightKg: Float,
    val personalRecordReps: Int,
    val personalRecordVolumeKg: Float,
    val personalRecordOneRepMaxKg: Float,
    // Dynamic Exercise-Specific Target / Prescription metrics (used when 0 sessions or as next-session overload targets)
    val recommendedStartWeightKg: Float,
    val recommendedTargetReps: Int,
    val recommendedTargetSets: Int,
    val recommendedTargetVolumeKg: Float,
    val recommendedTargetOneRepMaxKg: Float,
    val nextOverloadTargetWeightKg: Float,
    val nextOverloadTargetReps: Int,
    val dynamicStatusBadge: String,
    val latestSession: ExerciseSessionSummary?,
    val previousSession: ExerciseSessionSummary?,
    val weightDeltaKg: Float,
    val repsDelta: Int,
    val volumeDeltaKg: Float,
    val volumeDeltaPercent: Float,
    val overloadStatus: OverloadStatus,
    val overloadHeadline: String,
    val overloadComparisonDetail: String,
    val sessionHistory: List<ExerciseSessionSummary>
)

data class PersonalRecordItem(
    val exerciseName: String,
    val muscleGroup: String,
    val bestWeightKg: Float,
    val bestReps: Int,
    val bestVolumeKg: Float,
    val estimatedOneRepMaxKg: Float,
    val achievedDateKey: String,
    val achievedDateLabel: String
)

data class MuscleGroupStat(
    val muscleGroup: String,
    val completedSets: Int,
    val totalVolumeKg: Float,
    val percentageOfSets: Float
)

data class FrequentExerciseStat(
    val exerciseName: String,
    val muscleGroup: String,
    val sessionCount: Int,
    val totalSets: Int,
    val totalVolumeKg: Float,
    val bestWeightKg: Float
)

data class DashboardAnalyticsSummary(
    val dateRange: AnalyticsDateRange,
    val attendedToday: Boolean,
    val workoutsCompletedToday: Int,
    val setsCompletedToday: Int,
    val volumeTodayKg: Float,
    val periodAttendanceDays: Int,
    val periodWorkoutsCount: Int,
    val periodCompletedSets: Int,
    val periodTotalReps: Int,
    val periodTotalVolumeKg: Float,
    val dailyVolumeSeries: List<DailyChartPoint>,
    val dailySetsSeries: List<DailyChartPoint>,
    val muscleGroupBreakdown: List<MuscleGroupStat>,
    val frequentExercises: List<FrequentExerciseStat>,
    val personalRecords: List<PersonalRecordItem>,
    val overloadReports: List<ExerciseProgressionReport>
)

object AnalyticsAndInsightsEngine {

    private const val DAY_MS = 86_400_000L

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun epochMillisToDateKey(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun dateKeyToEpochMillis(dateKey: String): Long {
        return try {
            val parts = dateKey.split("-")
            if (parts.size == 3) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatDateKeyReadable(dateKey: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dateKey) ?: return dateKey
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun formatDateKeyShort(dateKey: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dateKey) ?: return dateKey
            SimpleDateFormat("MMM d", Locale.US).format(parsed)
        } catch (_: Exception) {
            dateKey
        }
    }

    fun dateKeyForDaysAgo(daysAgo: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun buildAttendanceOverview(
        records: List<GymAttendanceEntity>,
        profile: UserProfileEntity,
        selectedCalendarYear: Int? = null,
        selectedCalendarMonthZeroBased: Int? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): AttendanceOverview {
        val todayKey = todayDateKey(nowMillis)
        val recordsByDate = records.groupBy { it.dateKey }
        val distinctAttendedDatesSorted = recordsByDate.keys.sorted()

        val attendedToday = recordsByDate.containsKey(todayKey)
        val todayRecords = recordsByDate[todayKey].orEmpty()

        val last7DateKeys = (6 downTo 0).map { dateKeyForDaysAgo(it, nowMillis) }
        val weeklyVisitDays = last7DateKeys.count { recordsByDate.containsKey(it) }
        val weeklyTotalCheckIns = last7DateKeys.sumOf { recordsByDate[it]?.size ?: 0 }

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYearMonthPrefix = String.format(
            Locale.US,
            "%04d-%02d",
            nowCal.get(Calendar.YEAR),
            nowCal.get(Calendar.MONTH) + 1
        )
        val monthlyVisitDays = distinctAttendedDatesSorted.count { it.startsWith(currentYearMonthPrefix) }

        val (currentStreak, longestStreak) = calculateStreaks(
            distinctDates = distinctAttendedDatesSorted.toSet(),
            todayKey = todayKey,
            nowMillis = nowMillis
        )

        val goalDays = profile.weeklyAttendanceGoal.coerceIn(1, 7)
        val goalProgress = (weeklyVisitDays.toFloat() / goalDays.toFloat()).coerceIn(0f, 1f)

        val targetYear = selectedCalendarYear ?: nowCal.get(Calendar.YEAR)
        val targetMonth = selectedCalendarMonthZeroBased ?: nowCal.get(Calendar.MONTH)
        val calendarCells = buildMonthCalendarCells(
            year = targetYear,
            monthZeroBased = targetMonth,
            todayKey = todayKey,
            recordsByDate = recordsByDate,
            plannedTrainingDaysCsv = profile.trainingDaysCsv
        )

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.US).format(monthCal.time)

        val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
        val weeklySeries = last7DateKeys.map { dateKey ->
            val millis = dateKeyToEpochMillis(dateKey)
            val count = recordsByDate[dateKey]?.size ?: 0
            DailyChartPoint(
                dateKey = dateKey,
                shortDayLabel = dayLabelFormat.format(Date(millis)),
                fullDateLabel = formatDateKeyShort(dateKey),
                value = if (count > 0) 1f else 0f,
                secondaryValue = count.toFloat()
            )
        }

        return AttendanceOverview(
            todayDateKey = todayKey,
            attendedToday = attendedToday,
            todayRecords = todayRecords,
            weeklyVisitDays = weeklyVisitDays,
            weeklyTotalCheckIns = weeklyTotalCheckIns,
            monthlyVisitDays = monthlyVisitDays,
            totalAllTimeVisits = records.size,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            weeklyGoalDays = goalDays,
            weeklyGoalProgress = goalProgress,
            calendarYear = targetYear,
            calendarMonthZeroBased = targetMonth,
            calendarMonthTitle = monthTitle,
            calendarDays = calendarCells,
            weeklyAttendanceSeries = weeklySeries
        )
    }

    private fun calculateStreaks(
        distinctDates: Set<String>,
        todayKey: String,
        nowMillis: Long
    ): Pair<Int, Int> {
        if (distinctDates.isEmpty()) return 0 to 0

        var currentStreak = 0
        var offset = if (distinctDates.contains(todayKey)) 0 else 1
        while (true) {
            val checkKey = dateKeyForDaysAgo(offset, nowMillis)
            if (distinctDates.contains(checkKey)) {
                currentStreak++
                offset++
            } else {
                break
            }
        }

        val sortedMillis = distinctDates.map { dateKeyToEpochMillis(it) }.sorted()
        var longest = 1
        var running = 1
        for (i in 1 until sortedMillis.size) {
            val diffDays = ((sortedMillis[i] - sortedMillis[i - 1]).toDouble() / DAY_MS.toDouble()).roundToInt()
            if (diffDays == 1) {
                running++
                if (running > longest) longest = running
            } else if (diffDays > 1) {
                running = 1
            }
        }

        return currentStreak to maxOf(longest, currentStreak)
    }

    private fun buildMonthCalendarCells(
        year: Int,
        monthZeroBased: Int,
        todayKey: String,
        recordsByDate: Map<String, List<GymAttendanceEntity>>,
        @Suppress("UNUSED_PARAMETER") plannedTrainingDaysCsv: String
    ): List<CalendarDayCell> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

        val cells = mutableListOf<CalendarDayCell>()
        for (i in 0 until firstDayOfWeekOffset) {
            cells.add(
                CalendarDayCell(
                    dateKey = "",
                    dayOfMonth = 0,
                    isCurrentMonth = false,
                    isToday = false,
                    status = CalendarAttendanceStatus.REST_OR_UNLOGGED,
                    visitCount = 0,
                    records = emptyList()
                )
            )
        }

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day)
            val dayRecords = recordsByDate[dateKey].orEmpty()
            val isToday = dateKey == todayKey

            val status = if (dayRecords.isNotEmpty()) {
                CalendarAttendanceStatus.ATTENDED
            } else {
                CalendarAttendanceStatus.REST_OR_UNLOGGED
            }

            cells.add(
                CalendarDayCell(
                    dateKey = dateKey,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = isToday,
                    status = status,
                    visitCount = dayRecords.size,
                    records = dayRecords
                )
            )
        }
        return cells
    }

    /**
     * Calculates exercise-specific biomechanical starting targets based on athlete bodyweight,
     * fitness level, and exercise mechanics so every exercise displays distinct, intelligent
     * target prescriptions before its first session is logged.
     */
    private fun calculateExercisePrescription(
        exerciseName: String,
        muscleGroup: String,
        equipment: String,
        isBodyweight: Boolean,
        isAssisted: Boolean,
        userBodyweightKg: Float,
        fitnessLevel: String
    ): Triple<Float, Int, Int> {
        val bw = userBodyweightKg.coerceIn(40f, 180f)
        val levelMultiplier = when {
            fitnessLevel.contains("Beginner", ignoreCase = true) -> 0.72f
            fitnessLevel.contains("Advanced", ignoreCase = true) -> 1.25f
            else -> 1.0f // Intermediate
        }
        val lowerName = exerciseName.lowercase(Locale.US)

        val (rawRatio, targetReps, targetSets) = when {
            isAssisted -> Triple(0.25f, 10, 3)
            isBodyweight && lowerName.contains("pull") -> Triple(1.0f, 8, 4)
            isBodyweight && lowerName.contains("push") -> Triple(0.65f, 15, 3)
            isBodyweight -> Triple(0.55f, 12, 3)
            lowerName.contains("deadlift") && !lowerName.contains("romanian") -> Triple(1.35f, 5, 3)
            lowerName.contains("romanian") || lowerName.contains("rdl") -> Triple(0.95f, 8, 3)
            lowerName.contains("squat") -> Triple(1.10f, 6, 4)
            lowerName.contains("leg press") -> Triple(1.85f, 10, 4)
            lowerName.contains("bench press") -> Triple(0.82f, 8, 4)
            lowerName.contains("incline") && equipment.contains("Dumbbell", ignoreCase = true) -> Triple(0.32f, 10, 3)
            lowerName.contains("overhead") || lowerName.contains("shoulder press") -> Triple(0.52f, 8, 4)
            lowerName.contains("row") -> Triple(0.75f, 8, 4)
            lowerName.contains("pulldown") -> Triple(0.70f, 10, 3)
            lowerName.contains("lateral raise") -> Triple(0.14f, 12, 4)
            lowerName.contains("curl") -> Triple(0.36f, 10, 3)
            lowerName.contains("tricep") || lowerName.contains("pushdown") -> Triple(0.34f, 12, 3)
            equipment.contains("Barbell", ignoreCase = true) && muscleGroup.equals("Legs", ignoreCase = true) -> Triple(1.0f, 8, 4)
            equipment.contains("Barbell", ignoreCase = true) -> Triple(0.70f, 8, 3)
            equipment.contains("Dumbbell", ignoreCase = true) -> Triple(0.26f, 10, 3)
            equipment.contains("Machine", ignoreCase = true) -> Triple(0.85f, 10, 3)
            equipment.contains("Cable", ignoreCase = true) -> Triple(0.42f, 12, 3)
            else -> Triple(0.50f, 10, 3)
        }

        val rawWeight = if (isBodyweight && !isAssisted) {
            bw
        } else {
            // Round to nearest 2.5 kg increment
            val unrounded = bw * rawRatio * levelMultiplier
            ((unrounded / 2.5f).roundToInt() * 2.5f).coerceAtLeast(5f)
        }
        return Triple(rawWeight, targetReps, targetSets)
    }

    /**
     * Resolves an exercise name from a logged set to a canonical catalog exercise if a user
     * typed a short alias (e.g. "Bench" -> "Barbell Bench Press") or exact name.
     */
    private fun resolveCanonicalExerciseName(
        rawName: String,
        catalogExercises: List<ExerciseEntity>
    ): String {
        val trimmed = rawName.trim()
        val exact = catalogExercises.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (exact != null) return exact.name
        // If user typed a short alias like "Bench" and there isn't a custom exercise literally called "Bench"
        val containsMatch = catalogExercises.firstOrNull {
            !it.isCustom && it.name.contains(trimmed, ignoreCase = true) && trimmed.length >= 4
        }
        return containsMatch?.name ?: trimmed
    }

    /**
     * Computes exercise-specific progression and real-time progressive overload insights.
     * Includes both completed workout sessions AND any live active workout sets so the UI
     * updates immediately in real time.
     */
    fun buildExerciseProgressionReports(
        exercises: List<ExerciseEntity>,
        completedSessions: List<WorkoutSessionEntity>,
        allSets: List<WorkoutSetEntity>,
        userBodyweightKg: Float,
        fitnessLevel: String = "Intermediate",
        activeWorkoutTitle: String? = null,
        activeWorkoutDateKey: String? = null,
        activeDraftSets: List<DraftSetEntry> = emptyList()
    ): List<ExerciseProgressionReport> {
        val allSessionsList = completedSessions.filter { !it.isDraft }.toMutableList()
        val combinedSetsList = allSets.toMutableList()

        // Include live active workout sets in real time if present
        val validLiveDraftSets = activeDraftSets.filter { it.status == "COMPLETED" && it.reps > 0 && it.weightKg >= 0f }
        if (validLiveDraftSets.isNotEmpty()) {
            val liveSessionId = -999L
            val liveDateKey = activeWorkoutDateKey?.ifBlank { todayDateKey() } ?: todayDateKey()
            allSessionsList.add(
                WorkoutSessionEntity(
                    id = liveSessionId,
                    title = "${activeWorkoutTitle ?: "Active Workout"} (Live)",
                    category = "Strength",
                    dateKey = liveDateKey,
                    timestampMillis = System.currentTimeMillis() + 1000L,
                    durationMinutes = 30,
                    isDraft = false
                )
            )
            validLiveDraftSets.forEachIndexed { idx, draft ->
                combinedSetsList.add(
                    WorkoutSetEntity(
                        id = -1000L - idx,
                        sessionId = liveSessionId,
                        dateKey = liveDateKey,
                        exerciseName = draft.exerciseName,
                        muscleGroup = draft.muscleGroup,
                        exerciseOrder = draft.exerciseOrder,
                        setOrder = idx,
                        setNumber = draft.setNumber,
                        weightKg = draft.weightKg,
                        reps = draft.reps,
                        restSeconds = draft.restSeconds,
                        status = draft.status,
                        isBodyweight = draft.isBodyweight,
                        isAssisted = draft.isAssisted,
                        notes = draft.notes
                    )
                )
            }
        }

        val sessionsById = allSessionsList.associateBy { it.id }
        val exercisesByName = exercises.associateBy { it.name.lowercase(Locale.US) }

        val validCompletedSets = combinedSetsList.filter { set ->
            sessionsById.containsKey(set.sessionId) &&
                set.status == "COMPLETED" &&
                set.reps > 0 &&
                set.weightKg >= 0f
        }

        // Group sets by canonical exercise name so both exact and alias sets map cleanly
        val setsByExerciseName = mutableMapOf<String, MutableList<WorkoutSetEntity>>()
        validCompletedSets.forEach { set ->
            val rawName = set.exerciseName.trim()
            setsByExerciseName.getOrPut(rawName) { mutableListOf() }.add(set)
            val canonical = resolveCanonicalExerciseName(rawName, exercises)
            if (!canonical.equals(rawName, ignoreCase = true)) {
                setsByExerciseName.getOrPut(canonical) { mutableListOf() }.add(set)
            }
        }

        val allExerciseNames = (exercises.filter { !it.isArchived }.map { it.name } + setsByExerciseName.keys)
            .distinctBy { it.lowercase(Locale.US) }

        return allExerciseNames.map { exerciseName ->
            val meta = exercisesByName[exerciseName.lowercase(Locale.US)]
            val exerciseSets = setsByExerciseName.entries
                .firstOrNull { it.key.equals(exerciseName, ignoreCase = true) }
                ?.value
                .orEmpty()

            val isBodyweight = meta?.isBodyweight ?: exerciseSets.firstOrNull()?.isBodyweight ?: false
            val isAssisted = meta?.isAssisted ?: exerciseSets.firstOrNull()?.isAssisted ?: false
            val muscleGroup = meta?.muscleGroup ?: exerciseSets.firstOrNull()?.muscleGroup ?: "Full Body"
            val secondaryMuscles = meta?.secondaryMuscles ?: ""
            val equipment = meta?.equipment ?: "Barbell"
            val instructions = meta?.instructions ?: "Perform with controlled tempo through full range of motion."

            val (recWeightKg, recReps, recSets) = calculateExercisePrescription(
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                equipment = equipment,
                isBodyweight = isBodyweight,
                isAssisted = isAssisted,
                userBodyweightKg = userBodyweightKg,
                fitnessLevel = fitnessLevel
            )
            val recVolumeKg = recWeightKg * recReps * recSets
            val rec1RmKg = FitnessDatabase.calculateEpleyOneRepMax(recWeightKg, recReps)

            val sessionSummaries = exerciseSets
                .groupBy { it.sessionId }
                .mapNotNull { (sessionId, sessionSets) ->
                    val session = sessionsById[sessionId] ?: return@mapNotNull null
                    val orderedSets = sessionSets.sortedBy { it.setNumber }
                    val topWeightKg = orderedSets.maxOfOrNull {
                        if ((it.isBodyweight || isBodyweight) && it.weightKg <= 0f) userBodyweightKg else it.weightKg
                    } ?: 0f
                    val maxRepsAtTopWeight = orderedSets
                        .filter {
                            val eff = if ((it.isBodyweight || isBodyweight) && it.weightKg <= 0f) userBodyweightKg else it.weightKg
                            eff == topWeightKg
                        }
                        .maxOfOrNull { it.reps } ?: 0
                    val bestSetReps = orderedSets.maxOfOrNull { it.reps } ?: 0
                    val totalReps = orderedSets.sumOf { it.reps }
                    val sessionVolumeKg = orderedSets.fold(0f) { acc, s ->
                        acc + FitnessDatabase.calculateSetVolumeKg(
                            weightKg = s.weightKg,
                            reps = s.reps,
                            status = s.status,
                            isBodyweight = s.isBodyweight || isBodyweight,
                            isAssisted = s.isAssisted || isAssisted,
                            userBodyweightKg = userBodyweightKg
                        )
                    }
                    val best1Rm = orderedSets.maxOfOrNull { s ->
                        val effectiveWeight = if ((s.isBodyweight || isBodyweight) && s.weightKg <= 0f) {
                            userBodyweightKg
                        } else {
                            s.weightKg
                        }
                        FitnessDatabase.calculateEpleyOneRepMax(effectiveWeight, s.reps)
                    } ?: 0f

                    val dateKey = session.dateKey.ifBlank { epochMillisToDateKey(session.timestampMillis) }
                    ExerciseSessionSummary(
                        sessionId = session.id,
                        sessionTitle = session.title,
                        dateKey = dateKey,
                        dateLabel = formatDateKeyShort(dateKey),
                        timestampMillis = session.timestampMillis,
                        topWeightKg = topWeightKg,
                        maxRepsAtTopWeight = maxRepsAtTopWeight,
                        bestSetReps = bestSetReps,
                        totalReps = totalReps,
                        completedSetCount = orderedSets.size,
                        sessionVolumeKg = sessionVolumeKg,
                        bestEstimatedOneRepMaxKg = best1Rm,
                        isLiveActiveSession = session.id == -999L,
                        sets = orderedSets
                    )
                }
                .sortedWith(compareBy<ExerciseSessionSummary> { it.dateKey }.thenBy { it.timestampMillis })

            val prWeight = sessionSummaries.maxOfOrNull { it.topWeightKg } ?: 0f
            val prReps = sessionSummaries.maxOfOrNull { it.bestSetReps } ?: 0
            val prVolume = sessionSummaries.maxOfOrNull { it.sessionVolumeKg } ?: 0f
            val pr1Rm = sessionSummaries.maxOfOrNull { it.bestEstimatedOneRepMaxKg } ?: 0f
            val lifetimeVolume = sessionSummaries.fold(0f) { acc, s -> acc + s.sessionVolumeKg }
            val totalSetsCount = sessionSummaries.sumOf { it.completedSetCount }

            val latest = sessionSummaries.lastOrNull()
            val previous = if (sessionSummaries.size >= 2) sessionSummaries[sessionSummaries.size - 2] else null

            val weightDelta = if (latest != null && previous != null) latest.topWeightKg - previous.topWeightKg else 0f
            val repsDelta = if (latest != null && previous != null) latest.bestSetReps - previous.bestSetReps else 0
            val volumeDelta = if (latest != null && previous != null) latest.sessionVolumeKg - previous.sessionVolumeKg else 0f
            val volumeDeltaPct = if (latest != null && previous != null && previous.sessionVolumeKg > 0f) {
                ((latest.sessionVolumeKg - previous.sessionVolumeKg) / previous.sessionVolumeKg) * 100f
            } else {
                0f
            }

            val priorBestWeight = if (sessionSummaries.size >= 2) {
                sessionSummaries.dropLast(1).maxOfOrNull { it.topWeightKg } ?: 0f
            } else 0f
            val priorBest1Rm = if (sessionSummaries.size >= 2) {
                sessionSummaries.dropLast(1).maxOfOrNull { it.bestEstimatedOneRepMaxKg } ?: 0f
            } else 0f

            val loadStep = if (equipment.contains("Barbell", ignoreCase = true) || equipment.contains("Machine", ignoreCase = true)) 2.5f else 2.0f
            val nextTargetWeight = if (latest != null) latest.topWeightKg + loadStep else 0f
            val nextTargetReps = if (latest != null) (latest.maxRepsAtTopWeight + 1).coerceAtMost(15) else 0

            val (status, dynamicBadge, headline, detail) = evaluateOverloadInsight(
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                equipment = equipment,
                isBodyweight = isBodyweight,
                recWeightKg = 0f,
                recReps = 0,
                recSets = 0,
                nextTargetWeight = nextTargetWeight,
                nextTargetReps = nextTargetReps,
                latest = latest,
                previous = previous,
                priorBestWeight = priorBestWeight,
                priorBest1Rm = priorBest1Rm,
                weightDelta = weightDelta,
                repsDelta = repsDelta,
                volumeDelta = volumeDelta,
                volumeDeltaPct = volumeDeltaPct
            )

            ExerciseProgressionReport(
                exerciseId = meta?.id,
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                secondaryMuscles = secondaryMuscles,
                equipment = equipment,
                instructions = instructions,
                isBodyweight = isBodyweight,
                isAssisted = isAssisted,
                totalSessionsLogged = sessionSummaries.size,
                totalCompletedSets = totalSetsCount,
                lifetimeVolumeKg = lifetimeVolume,
                personalRecordWeightKg = prWeight,
                personalRecordReps = prReps,
                personalRecordVolumeKg = prVolume,
                personalRecordOneRepMaxKg = pr1Rm,
                recommendedStartWeightKg = 0f,
                recommendedTargetReps = 0,
                recommendedTargetSets = 0,
                recommendedTargetVolumeKg = 0f,
                recommendedTargetOneRepMaxKg = 0f,
                nextOverloadTargetWeightKg = nextTargetWeight,
                nextOverloadTargetReps = nextTargetReps,
                dynamicStatusBadge = dynamicBadge,
                latestSession = latest,
                previousSession = previous,
                weightDeltaKg = weightDelta,
                repsDelta = repsDelta,
                volumeDeltaKg = volumeDelta,
                volumeDeltaPercent = volumeDeltaPct,
                overloadStatus = status,
                overloadHeadline = headline,
                overloadComparisonDetail = detail,
                sessionHistory = sessionSummaries
            )
        }.sortedWith(
            compareByDescending<ExerciseProgressionReport> { it.totalSessionsLogged }
                .thenBy { it.exerciseName }
        )
    }

    private data class OverloadEvalResult(
        val status: OverloadStatus,
        val badgeText: String,
        val headline: String,
        val detail: String
    )

    @Suppress("UNUSED_PARAMETER")
    private fun evaluateOverloadInsight(
        exerciseName: String,
        muscleGroup: String,
        equipment: String,
        isBodyweight: Boolean,
        recWeightKg: Float,
        recReps: Int,
        recSets: Int,
        nextTargetWeight: Float,
        nextTargetReps: Int,
        latest: ExerciseSessionSummary?,
        previous: ExerciseSessionSummary?,
        priorBestWeight: Float,
        priorBest1Rm: Float,
        weightDelta: Float,
        repsDelta: Int,
        volumeDelta: Float,
        volumeDeltaPct: Float
    ): OverloadEvalResult {
        if (latest == null) {
            return OverloadEvalResult(
                status = OverloadStatus.INSUFFICIENT_DATA,
                badgeText = "0 Logs Yet",
                headline = "0 sessions logged for $exerciseName",
                detail = "$muscleGroup ($equipment) • Log sets in Workouts or Quick Log below to track your weekly, monthly, and yearly progression."
            )
        }
        if (previous == null) {
            return OverloadEvalResult(
                status = OverloadStatus.BASELINE_ESTABLISHED,
                badgeText = "Baseline: ${formatDecimal(latest.topWeightKg)} kg × ${latest.maxRepsAtTopWeight}",
                headline = "Baseline logged on ${latest.dateLabel}: ${formatDecimal(latest.topWeightKg)} kg × ${latest.maxRepsAtTopWeight} reps (${latest.sessionVolumeKg.roundToInt()} kg vol)",
                detail = "Next session progressive overload goal: Aim for ${formatDecimal(nextTargetWeight)} kg × ${latest.maxRepsAtTopWeight} reps OR ${formatDecimal(latest.topWeightKg)} kg × $nextTargetReps reps."
            )
        }

        val isNewAllTimePr = (latest.topWeightKg > priorBestWeight && latest.topWeightKg > 0f) ||
            (latest.bestEstimatedOneRepMaxKg > priorBest1Rm + 0.25f)

        return when {
            isNewAllTimePr && weightDelta > 0f -> OverloadEvalResult(
                status = OverloadStatus.NEW_PR,
                badgeText = "New PR: +${formatDecimal(weightDelta)} kg",
                headline = "New PR: +${formatDecimal(weightDelta)} kg top load vs ${previous.dateLabel}",
                detail = "Top set increased from ${formatDecimal(previous.topWeightKg)} kg to ${formatDecimal(latest.topWeightKg)} kg (${latest.dateLabel}). Next target: ${formatDecimal(nextTargetWeight)} kg."
            )
            weightDelta > 0f -> OverloadEvalResult(
                status = OverloadStatus.INCREASED_WEIGHT,
                badgeText = "+${formatDecimal(weightDelta)} kg Load",
                headline = "Increased weight by +${formatDecimal(weightDelta)} kg",
                detail = "Compared to ${previous.dateLabel} (${formatDecimal(previous.topWeightKg)} kg → ${formatDecimal(latest.topWeightKg)} kg)."
            )
            weightDelta == 0f && latest.maxRepsAtTopWeight > previous.maxRepsAtTopWeight -> {
                val repGain = latest.maxRepsAtTopWeight - previous.maxRepsAtTopWeight
                OverloadEvalResult(
                    status = OverloadStatus.MORE_REPS,
                    badgeText = "+$repGain Reps @ ${formatDecimal(latest.topWeightKg)} kg",
                    headline = "+$repGain reps at ${formatDecimal(latest.topWeightKg)} kg",
                    detail = "Completed ${latest.maxRepsAtTopWeight} reps vs ${previous.maxRepsAtTopWeight} reps at the same top load on ${previous.dateLabel}."
                )
            }
            volumeDelta > 0f -> OverloadEvalResult(
                status = OverloadStatus.INCREASED_VOLUME,
                badgeText = "+${volumeDeltaPct.roundToInt()}% Volume",
                headline = "Session volume +${formatDecimal(volumeDelta)} kg (+${volumeDeltaPct.roundToInt()}%)",
                detail = "Total volume rose from ${previous.sessionVolumeKg.roundToInt()} kg (${previous.dateLabel}) to ${latest.sessionVolumeKg.roundToInt()} kg (${latest.dateLabel})."
            )
            repsDelta > 0 -> OverloadEvalResult(
                status = OverloadStatus.MORE_REPS,
                badgeText = "+$repsDelta Peak Reps",
                headline = "Best set increased by +$repsDelta reps",
                detail = "Peak repetitions improved from ${previous.bestSetReps} reps (${previous.dateLabel}) to ${latest.bestSetReps} reps (${latest.dateLabel})."
            )
            else -> OverloadEvalResult(
                status = OverloadStatus.MAINTAINED_OR_LOWER,
                badgeText = "Maintained (${latest.sessionVolumeKg.roundToInt()} kg)",
                headline = "Maintained / deload session on ${latest.dateLabel}",
                detail = "Latest session (${latest.sessionVolumeKg.roundToInt()} kg volume) vs ${previous.dateLabel} (${previous.sessionVolumeKg.roundToInt()} kg). Next goal: ${formatDecimal(nextTargetWeight)} kg × ${latest.maxRepsAtTopWeight}."
            )
        }
    }

    fun buildDashboardAnalytics(
        dateRange: AnalyticsDateRange,
        attendanceRecords: List<GymAttendanceEntity>,
        exercises: List<ExerciseEntity>,
        completedSessions: List<WorkoutSessionEntity>,
        allSets: List<WorkoutSetEntity>,
        profile: UserProfileEntity,
        activeWorkoutTitle: String? = null,
        activeWorkoutDateKey: String? = null,
        activeDraftSets: List<DraftSetEntry> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardAnalyticsSummary {
        val todayKey = todayDateKey(nowMillis)
        val nonDraftSessions = completedSessions.filter { !it.isDraft }
        val nonDraftSessionIds = nonDraftSessions.map { it.id }.toSet()

        val cutoffDateKey = dateRange.days?.let { days ->
            dateKeyForDaysAgo(days - 1, nowMillis)
        }

        val periodAttendance = if (cutoffDateKey != null) {
            attendanceRecords.filter { it.dateKey >= cutoffDateKey && it.dateKey <= todayKey }
        } else {
            attendanceRecords
        }
        val periodSessions = if (cutoffDateKey != null) {
            nonDraftSessions.filter {
                val key = it.dateKey.ifBlank { epochMillisToDateKey(it.timestampMillis) }
                key >= cutoffDateKey && key <= todayKey
            }
        } else {
            nonDraftSessions
        }
        val periodSessionIds = periodSessions.map { it.id }.toSet()

        val validCompletedSetsAll = allSets.filter {
            nonDraftSessionIds.contains(it.sessionId) && it.status == "COMPLETED" && it.reps > 0
        }.toMutableList()

        // Include live active workout sets so Summary & Exercise analytics react immediately
        val validLiveSets = activeDraftSets.filter { it.status == "COMPLETED" && it.reps > 0 && it.weightKg >= 0f }
        val liveDateKey = activeWorkoutDateKey?.ifBlank { todayKey } ?: todayKey
        val liveSetEntities = validLiveSets.mapIndexed { idx, draft ->
            WorkoutSetEntity(
                id = -1000L - idx,
                sessionId = -999L,
                dateKey = liveDateKey,
                exerciseName = draft.exerciseName,
                muscleGroup = draft.muscleGroup,
                exerciseOrder = draft.exerciseOrder,
                setOrder = idx,
                setNumber = draft.setNumber,
                weightKg = draft.weightKg,
                reps = draft.reps,
                restSeconds = draft.restSeconds,
                status = draft.status,
                isBodyweight = draft.isBodyweight,
                isAssisted = draft.isAssisted,
                notes = draft.notes
            )
        }

        val periodSets = validCompletedSetsAll.filter { periodSessionIds.contains(it.sessionId) } +
            if (cutoffDateKey == null || liveDateKey >= cutoffDateKey) liveSetEntities else emptyList()

        val todaySessionIds = nonDraftSessions
            .filter { (it.dateKey.ifBlank { epochMillisToDateKey(it.timestampMillis) }) == todayKey }
            .map { it.id }
            .toSet()
        val todaySets = validCompletedSetsAll.filter { todaySessionIds.contains(it.sessionId) } +
            if (liveDateKey == todayKey) liveSetEntities else emptyList()

        val volumeTodayKg = todaySets.fold(0f) { acc, set ->
            acc + FitnessDatabase.calculateSetVolumeKg(
                weightKg = set.weightKg,
                reps = set.reps,
                status = set.status,
                isBodyweight = set.isBodyweight,
                isAssisted = set.isAssisted,
                userBodyweightKg = profile.weightKg
            )
        }

        val periodTotalVolumeKg = periodSets.fold(0f) { acc, set ->
            acc + FitnessDatabase.calculateSetVolumeKg(
                weightKg = set.weightKg,
                reps = set.reps,
                status = set.status,
                isBodyweight = set.isBodyweight,
                isAssisted = set.isAssisted,
                userBodyweightKg = profile.weightKg
            )
        }
        val periodTotalReps = periodSets.sumOf { it.reps }

        val sessionDateMap = nonDraftSessions.associate {
            it.id to it.dateKey.ifBlank { epochMillisToDateKey(it.timestampMillis) }
        }
        val allSetsWithLive = validCompletedSetsAll + liveSetEntities
        val setsByDateKey = allSetsWithLive.groupBy {
            if (it.sessionId == -999L) liveDateKey else (sessionDateMap[it.sessionId] ?: it.dateKey)
        }

        val (dailyVolumeSeries, dailySetsSeries) = buildHorizonChartSeries(
            dateRange = dateRange,
            setsByDateKey = setsByDateKey,
            userBodyweightKg = profile.weightKg,
            nowMillis = nowMillis
        )

        val totalPeriodSetCount = periodSets.size.coerceAtLeast(1)
        val muscleBreakdown = periodSets
            .groupBy { it.muscleGroup.ifBlank { "Full Body" } }
            .map { (group, groupSets) ->
                val vol = groupSets.fold(0f) { acc, s ->
                    acc + FitnessDatabase.calculateSetVolumeKg(
                        weightKg = s.weightKg,
                        reps = s.reps,
                        status = s.status,
                        isBodyweight = s.isBodyweight,
                        isAssisted = s.isAssisted,
                        userBodyweightKg = profile.weightKg
                    )
                }
                MuscleGroupStat(
                    muscleGroup = group,
                    completedSets = groupSets.size,
                    totalVolumeKg = vol,
                    percentageOfSets = (groupSets.size.toFloat() / totalPeriodSetCount.toFloat()) * 100f
                )
            }
            .sortedByDescending { it.completedSets }

        val frequentExercises = periodSets
            .groupBy { it.exerciseName }
            .map { (name, exSets) ->
                val vol = exSets.fold(0f) { acc, s ->
                    acc + FitnessDatabase.calculateSetVolumeKg(
                        weightKg = s.weightKg,
                        reps = s.reps,
                        status = s.status,
                        isBodyweight = s.isBodyweight,
                        isAssisted = s.isAssisted,
                        userBodyweightKg = profile.weightKg
                    )
                }
                FrequentExerciseStat(
                    exerciseName = name,
                    muscleGroup = exSets.firstOrNull()?.muscleGroup ?: "Full Body",
                    sessionCount = exSets.map { it.sessionId }.distinct().size,
                    totalSets = exSets.size,
                    totalVolumeKg = vol,
                    bestWeightKg = exSets.maxOfOrNull { it.weightKg } ?: 0f
                )
            }
            .sortedWith(compareByDescending<FrequentExerciseStat> { it.sessionCount }.thenByDescending { it.totalSets })

        val overloadReports = buildExerciseProgressionReports(
            exercises = exercises,
            completedSessions = nonDraftSessions,
            allSets = allSets,
            userBodyweightKg = profile.weightKg,
            fitnessLevel = profile.fitnessLevel,
            activeWorkoutTitle = activeWorkoutTitle,
            activeWorkoutDateKey = activeWorkoutDateKey,
            activeDraftSets = activeDraftSets
        )

        val personalRecords = overloadReports
            .filter { it.totalSessionsLogged > 0 }
            .mapNotNull { report ->
                val bestSession = report.sessionHistory.maxByOrNull { it.bestEstimatedOneRepMaxKg }
                    ?: report.latestSession
                    ?: return@mapNotNull null
                PersonalRecordItem(
                    exerciseName = report.exerciseName,
                    muscleGroup = report.muscleGroup,
                    bestWeightKg = report.personalRecordWeightKg,
                    bestReps = report.personalRecordReps,
                    bestVolumeKg = report.personalRecordVolumeKg,
                    estimatedOneRepMaxKg = report.personalRecordOneRepMaxKg,
                    achievedDateKey = bestSession.dateKey,
                    achievedDateLabel = bestSession.dateLabel
                )
            }
            .sortedByDescending { it.estimatedOneRepMaxKg }

        return DashboardAnalyticsSummary(
            dateRange = dateRange,
            attendedToday = attendanceRecords.any { it.dateKey == todayKey },
            workoutsCompletedToday = todaySessionIds.size,
            setsCompletedToday = todaySets.size,
            volumeTodayKg = volumeTodayKg,
            periodAttendanceDays = periodAttendance.map { it.dateKey }.distinct().size,
            periodWorkoutsCount = periodSessions.size,
            periodCompletedSets = periodSets.size,
            periodTotalReps = periodTotalReps,
            periodTotalVolumeKg = periodTotalVolumeKg,
            dailyVolumeSeries = dailyVolumeSeries,
            dailySetsSeries = dailySetsSeries,
            muscleGroupBreakdown = muscleBreakdown,
            frequentExercises = frequentExercises,
            personalRecords = personalRecords,
            overloadReports = overloadReports
        )
    }

    /**
     * Builds Volume and Sets bar series across 1 Week (7 daily bars), 1 Month (6 five-day buckets),
     * 6 Months (6 monthly buckets), 1 Year (12 monthly buckets), or All Time (12 monthly buckets).
     */
    private fun buildHorizonChartSeries(
        dateRange: AnalyticsDateRange,
        setsByDateKey: Map<String, List<WorkoutSetEntity>>,
        userBodyweightKg: Float,
        nowMillis: Long
    ): Pair<List<DailyChartPoint>, List<DailyChartPoint>> {
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val monthShortFormat = SimpleDateFormat("MMM", Locale.US)
        val monthFullFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

        return when (dateRange) {
            AnalyticsDateRange.DAYS_7 -> {
                val keys = (6 downTo 0).map { dateKeyForDaysAgo(it, nowMillis) }
                val volPoints = keys.map { key ->
                    val daySets = setsByDateKey[key].orEmpty()
                    val vol = daySets.fold(0f) { acc, set ->
                        acc + FitnessDatabase.calculateSetVolumeKg(
                            weightKg = set.weightKg,
                            reps = set.reps,
                            status = set.status,
                            isBodyweight = set.isBodyweight,
                            isAssisted = set.isAssisted,
                            userBodyweightKg = userBodyweightKg
                        )
                    }
                    DailyChartPoint(
                        dateKey = key,
                        shortDayLabel = dayFormat.format(Date(dateKeyToEpochMillis(key))),
                        fullDateLabel = formatDateKeyShort(key),
                        value = vol,
                        secondaryValue = daySets.size.toFloat()
                    )
                }
                val setPoints = keys.map { key ->
                    val daySets = setsByDateKey[key].orEmpty()
                    DailyChartPoint(
                        dateKey = key,
                        shortDayLabel = dayFormat.format(Date(dateKeyToEpochMillis(key))),
                        fullDateLabel = formatDateKeyShort(key),
                        value = daySets.size.toFloat()
                    )
                }
                volPoints to setPoints
            }

            AnalyticsDateRange.DAYS_30 -> {
                // 6 buckets of 5 days each covering the last 30 days
                val volPoints = mutableListOf<DailyChartPoint>()
                val setPoints = mutableListOf<DailyChartPoint>()
                for (bucket in 5 downTo 0) {
                    val startDaysAgo = bucket * 5 + 4
                    val endDaysAgo = bucket * 5
                    val bucketKeys = (startDaysAgo downTo endDaysAgo).map { dateKeyForDaysAgo(it, nowMillis) }
                    val startKey = bucketKeys.first()
                    val endKey = bucketKeys.last()
                    val bucketSets = bucketKeys.flatMap { setsByDateKey[it].orEmpty() }
                    val vol = bucketSets.fold(0f) { acc, set ->
                        acc + FitnessDatabase.calculateSetVolumeKg(
                            weightKg = set.weightKg,
                            reps = set.reps,
                            status = set.status,
                            isBodyweight = set.isBodyweight,
                            isAssisted = set.isAssisted,
                            userBodyweightKg = userBodyweightKg
                        )
                    }
                    val shortLbl = formatDateKeyShort(endKey)
                    val fullLbl = "${formatDateKeyShort(startKey)} – ${formatDateKeyShort(endKey)}"
                    volPoints.add(
                        DailyChartPoint(
                            dateKey = endKey,
                            shortDayLabel = shortLbl,
                            fullDateLabel = fullLbl,
                            value = vol,
                            secondaryValue = bucketSets.size.toFloat()
                        )
                    )
                    setPoints.add(
                        DailyChartPoint(
                            dateKey = endKey,
                            shortDayLabel = shortLbl,
                            fullDateLabel = fullLbl,
                            value = bucketSets.size.toFloat()
                        )
                    )
                }
                volPoints to setPoints
            }

            AnalyticsDateRange.DAYS_90,
            AnalyticsDateRange.YEAR_1,
            AnalyticsDateRange.ALL_TIME -> {
                val monthsCount = if (dateRange == AnalyticsDateRange.DAYS_90) 6 else 12
                val volPoints = mutableListOf<DailyChartPoint>()
                val setPoints = mutableListOf<DailyChartPoint>()

                for (mAgo in (monthsCount - 1) downTo 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, -mAgo)
                    }
                    val ymPrefix = String.format(
                        Locale.US,
                        "%04d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1
                    )
                    val monthSets = setsByDateKey
                        .filterKeys { it.startsWith(ymPrefix) }
                        .values
                        .flatten()
                    val vol = monthSets.fold(0f) { acc, set ->
                        acc + FitnessDatabase.calculateSetVolumeKg(
                            weightKg = set.weightKg,
                            reps = set.reps,
                            status = set.status,
                            isBodyweight = set.isBodyweight,
                            isAssisted = set.isAssisted,
                            userBodyweightKg = userBodyweightKg
                        )
                    }
                    val shortLbl = monthShortFormat.format(cal.time)
                    val fullLbl = monthFullFormat.format(cal.time)
                    volPoints.add(
                        DailyChartPoint(
                            dateKey = ymPrefix,
                            shortDayLabel = shortLbl,
                            fullDateLabel = fullLbl,
                            value = vol,
                            secondaryValue = monthSets.size.toFloat()
                        )
                    )
                    setPoints.add(
                        DailyChartPoint(
                            dateKey = ymPrefix,
                            shortDayLabel = shortLbl,
                            fullDateLabel = fullLbl,
                            value = monthSets.size.toFloat()
                        )
                    )
                }
                volPoints to setPoints
            }
        }
    }

    private fun formatDecimal(value: Float): String {
        return if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }
}
