package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExerciseEntity
import com.example.data.local.FitnessDatabase
import com.example.data.local.GymAttendanceEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.repository.AnalyticsAndInsightsEngine
import com.example.data.repository.AnalyticsDateRange
import com.example.data.repository.AttendanceCheckInResult
import com.example.data.repository.AttendanceOverview
import com.example.data.repository.DashboardAnalyticsSummary
import com.example.data.repository.DraftSetEntry
import com.example.data.repository.ExerciseProgressionReport
import com.example.data.repository.FitnessRepository
import com.example.data.repository.GeneratedPdfReport
import com.example.data.repository.LocalBackupSnapshotInfo
import com.example.data.repository.PdfReportGenerator
import com.example.ui.theme.ThemeMode
import com.example.util.OriginIslandLiveManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

enum class AppDestination(val title: String) {
    SUMMARY("Summary"),
    ATTENDANCE("Attendance"),
    WORKOUTS("Workouts"),
    EXERCISES("Exercises"),
    SETTINGS("Settings")
}

/**
 * Two-Stage Workout Plan & Live Logger State:
 * Stage 1 (Plan Builder): `isWorkoutActive == false`
 *   - Starts completely blank (`title = ""`, `sets = emptyList()`).
 *   - User creates a workout plan by adding exercises & target sets, and can "Save Plan for Next Time"
 *     or "Start Workout".
 * Stage 2 (Live Execution): `isWorkoutActive == true`
 *   - User clicks on a specific exercise in their plan (`activeExerciseName`) to register it.
 *   - Clicking that exercise automatically starts the live timer (`isTimerRunning = true`,
 *     `activeExerciseElapsedSeconds` & `elapsedSeconds`), lets them register sets, and start rest timers.
 */
data class ActiveWorkoutEditorState(
    val editingSessionId: Long? = null,
    val isWorkoutActive: Boolean = false,
    val title: String = "",
    val category: String = "Strength",
    val dateKey: String = AnalyticsAndInsightsEngine.todayDateKey(),
    val notes: String = "",
    val activeExerciseName: String? = null,
    val activeExerciseElapsedSeconds: Int = 0,
    val exerciseElapsedSecondsMap: Map<String, Int> = emptyMap(),
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val restTimerRemainingSeconds: Int = 0,
    val restTimerTotalSeconds: Int = 90,
    val isRestTimerRunning: Boolean = false,
    val sets: List<DraftSetEntry> = emptyList()
)

private data class CoreDataFlows(
    val profile: UserProfileEntity,
    val attendance: List<GymAttendanceEntity>,
    val exercises: List<ExerciseEntity>,
    val routines: List<WorkoutTemplateEntity>,
    val completedSessions: List<WorkoutSessionEntity>,
    val draftSessions: List<WorkoutSessionEntity>,
    val allSets: List<WorkoutSetEntity>
)

private data class UiControlState(
    val currentTab: AppDestination = AppDestination.SUMMARY,
    val analyticsDateRange: AnalyticsDateRange = AnalyticsDateRange.DAYS_30,
    val attendanceFilterRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
    val selectedCalendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedCalendarMonthZeroBased: Int = Calendar.getInstance().get(Calendar.MONTH),
    val exerciseSearchQuery: String = "",
    val selectedMuscleFilter: String = "All",
    val selectedEquipmentFilter: String = "All",
    val showArchivedExercises: Boolean = false,
    val selectedExerciseForDetail: String? = null,
    val selectedHistoricalSessionId: Long? = null,
    val activeWorkout: ActiveWorkoutEditorState = ActiveWorkoutEditorState(),
    val generatedPdfReport: GeneratedPdfReport? = null,
    val isGeneratingPdf: Boolean = false,
    val localBackupSnapshots: List<LocalBackupSnapshotInfo> = emptyList(),
    val statusMessage: String? = null
)

data class FitPulseUiState(
    val isReady: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val currentTab: AppDestination = AppDestination.SUMMARY,
    val profile: UserProfileEntity = UserProfileEntity(),
    val useLbs: Boolean = false,
    val unitLabel: String = "kg",
    val attendanceOverview: AttendanceOverview = AnalyticsAndInsightsEngine.buildAttendanceOverview(
        emptyList(),
        UserProfileEntity()
    ),
    val allAttendanceRecords: List<GymAttendanceEntity> = emptyList(),
    val filteredAttendanceRecords: List<GymAttendanceEntity> = emptyList(),
    val attendanceFilterRange: AnalyticsDateRange = AnalyticsDateRange.ALL_TIME,
    val dashboardAnalytics: DashboardAnalyticsSummary = AnalyticsAndInsightsEngine.buildDashboardAnalytics(
        AnalyticsDateRange.DAYS_30,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        UserProfileEntity()
    ),
    val analyticsDateRange: AnalyticsDateRange = AnalyticsDateRange.DAYS_30,
    val allExercises: List<ExerciseEntity> = emptyList(),
    val filteredExercises: List<ExerciseEntity> = emptyList(),
    val exerciseSearchQuery: String = "",
    val selectedMuscleFilter: String = "All",
    val selectedEquipmentFilter: String = "All",
    val showArchivedExercises: Boolean = false,
    val selectedExerciseReport: ExerciseProgressionReport? = null,
    val routines: List<WorkoutTemplateEntity> = emptyList(),
    val completedSessions: List<WorkoutSessionEntity> = emptyList(),
    val draftSessions: List<WorkoutSessionEntity> = emptyList(),
    val allSets: List<WorkoutSetEntity> = emptyList(),
    val selectedHistoricalSession: WorkoutSessionEntity? = null,
    val selectedHistoricalSessionSets: List<WorkoutSetEntity> = emptyList(),
    val activeWorkout: ActiveWorkoutEditorState = ActiveWorkoutEditorState(),
    val generatedPdfReport: GeneratedPdfReport? = null,
    val isGeneratingPdf: Boolean = false,
    val localBackupSnapshots: List<LocalBackupSnapshotInfo> = emptyList(),
    val statusMessage: String? = null
)

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FitnessDatabase.getInstance(application)
    private val repository = FitnessRepository(application, database.fitnessDao())

    private val controlState = MutableStateFlow(UiControlState())
    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null
    private var nextDraftSetId = 1000L

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            refreshLocalBackupSnapshots()
        }
    }

    private val coreDataFlow = combine(
        repository.userProfileFlow,
        repository.attendanceRecordsFlow,
        repository.allExercisesFlow,
        repository.workoutTemplatesFlow,
        combine(
            repository.completedSessionsFlow,
            repository.draftSessionsFlow,
            repository.allWorkoutSetsFlow
        ) { completed, drafts, sets -> Triple(completed, drafts, sets) }
    ) { profile, attendance, exercises, routines, sessionTriple ->
        CoreDataFlows(
            profile = profile ?: UserProfileEntity(),
            attendance = attendance,
            exercises = exercises,
            routines = routines,
            completedSessions = sessionTriple.first,
            draftSessions = sessionTriple.second,
            allSets = sessionTriple.third
        )
    }

    val uiState: StateFlow<FitPulseUiState> = combine(
        coreDataFlow,
        repository.themeModeFlow,
        controlState
    ) { core, themeMode, ctrl ->
        val profile = core.profile
        val useLbs = profile.preferredWeightUnit.equals("lbs", ignoreCase = true)
        val unitLabel = if (useLbs) "lbs" else "kg"

        val attendanceOverview = AnalyticsAndInsightsEngine.buildAttendanceOverview(
            records = core.attendance,
            profile = profile,
            selectedCalendarYear = ctrl.selectedCalendarYear,
            selectedCalendarMonthZeroBased = ctrl.selectedCalendarMonthZeroBased
        )

        val todayKey = AnalyticsAndInsightsEngine.todayDateKey()
        val attendanceCutoff = ctrl.attendanceFilterRange.days?.let { days ->
            AnalyticsAndInsightsEngine.dateKeyForDaysAgo(days - 1)
        }
        val filteredAttendance = if (attendanceCutoff != null) {
            core.attendance.filter { it.dateKey >= attendanceCutoff && it.dateKey <= todayKey }
        } else {
            core.attendance
        }

        // Only completed sets in an active workout contribute to real-time volume & rings
        val liveCompletedSets = if (ctrl.activeWorkout.isWorkoutActive) {
            ctrl.activeWorkout.sets.filter { it.status == "COMPLETED" }
        } else {
            emptyList()
        }

        val dashboardAnalytics = AnalyticsAndInsightsEngine.buildDashboardAnalytics(
            dateRange = ctrl.analyticsDateRange,
            attendanceRecords = core.attendance,
            exercises = core.exercises,
            completedSessions = core.completedSessions,
            allSets = core.allSets,
            profile = profile,
            activeWorkoutTitle = ctrl.activeWorkout.title.ifBlank { "Live Workout" },
            activeWorkoutDateKey = ctrl.activeWorkout.dateKey,
            activeDraftSets = liveCompletedSets
        )

        val filteredExercises = core.exercises.filter { ex ->
            val archiveMatch = if (ctrl.showArchivedExercises) true else !ex.isArchived
            val queryMatch = ctrl.exerciseSearchQuery.isBlank() ||
                ex.name.contains(ctrl.exerciseSearchQuery, ignoreCase = true) ||
                ex.muscleGroup.contains(ctrl.exerciseSearchQuery, ignoreCase = true) ||
                ex.equipment.contains(ctrl.exerciseSearchQuery, ignoreCase = true)
            val muscleMatch = ctrl.selectedMuscleFilter == "All" ||
                ex.muscleGroup.equals(ctrl.selectedMuscleFilter, ignoreCase = true)
            val equipMatch = ctrl.selectedEquipmentFilter == "All" ||
                ex.equipment.equals(ctrl.selectedEquipmentFilter, ignoreCase = true)
            archiveMatch && queryMatch && muscleMatch && equipMatch
        }

        val selectedExerciseName = ctrl.selectedExerciseForDetail
            ?: dashboardAnalytics.overloadReports.firstOrNull { it.totalSessionsLogged > 0 }?.exerciseName
            ?: dashboardAnalytics.overloadReports.firstOrNull()?.exerciseName

        val selectedExerciseReport = dashboardAnalytics.overloadReports.firstOrNull {
            it.exerciseName.equals(selectedExerciseName, ignoreCase = true)
        }

        val selectedSession = (core.completedSessions + core.draftSessions).firstOrNull {
            it.id == ctrl.selectedHistoricalSessionId
        }
        val selectedSessionSets = if (selectedSession != null) {
            core.allSets
                .filter { it.sessionId == selectedSession.id }
                .sortedWith(compareBy<WorkoutSetEntity> { it.exerciseOrder }.thenBy { it.setNumber })
        } else {
            emptyList()
        }

        FitPulseUiState(
            isReady = true,
            themeMode = themeMode,
            currentTab = ctrl.currentTab,
            profile = profile,
            useLbs = useLbs,
            unitLabel = unitLabel,
            attendanceOverview = attendanceOverview,
            allAttendanceRecords = core.attendance,
            filteredAttendanceRecords = filteredAttendance,
            attendanceFilterRange = ctrl.attendanceFilterRange,
            dashboardAnalytics = dashboardAnalytics,
            analyticsDateRange = ctrl.analyticsDateRange,
            allExercises = core.exercises,
            filteredExercises = filteredExercises,
            exerciseSearchQuery = ctrl.exerciseSearchQuery,
            selectedMuscleFilter = ctrl.selectedMuscleFilter,
            selectedEquipmentFilter = ctrl.selectedEquipmentFilter,
            showArchivedExercises = ctrl.showArchivedExercises,
            selectedExerciseReport = selectedExerciseReport,
            routines = core.routines,
            completedSessions = core.completedSessions,
            draftSessions = core.draftSessions,
            allSets = core.allSets,
            selectedHistoricalSession = selectedSession,
            selectedHistoricalSessionSets = selectedSessionSets,
            activeWorkout = ctrl.activeWorkout,
            generatedPdfReport = ctrl.generatedPdfReport,
            isGeneratingPdf = ctrl.isGeneratingPdf,
            localBackupSnapshots = ctrl.localBackupSnapshots,
            statusMessage = ctrl.statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FitPulseUiState()
    )

    // --- NAVIGATION & FILTERS ---

    fun selectTab(tab: AppDestination) {
        controlState.update { it.copy(currentTab = tab) }
    }

    fun clearStatusMessage() {
        controlState.update { it.copy(statusMessage = null) }
    }

    private fun postMessage(msg: String) {
        controlState.update { it.copy(statusMessage = msg) }
    }

    fun setAnalyticsDateRange(range: AnalyticsDateRange) {
        controlState.update { it.copy(analyticsDateRange = range) }
    }

    fun setAttendanceFilterRange(range: AnalyticsDateRange) {
        controlState.update { it.copy(attendanceFilterRange = range) }
    }

    fun navigateCalendarMonth(deltaMonths: Int) {
        controlState.update { state ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.selectedCalendarYear)
                set(Calendar.MONTH, state.selectedCalendarMonthZeroBased)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, deltaMonths)
            }
            state.copy(
                selectedCalendarYear = cal.get(Calendar.YEAR),
                selectedCalendarMonthZeroBased = cal.get(Calendar.MONTH)
            )
        }
    }

    // --- GYM ATTENDANCE ACTIONS ---

    fun recordGymAttendance(
        dateKey: String = AnalyticsAndInsightsEngine.todayDateKey(),
        note: String = "Gym Session Check-In",
        durationMinutes: Int = 60
    ) {
        viewModelScope.launch {
            val allowMulti = uiState.value.profile.allowMultipleVisitsPerDay
            when (
                val result = repository.recordGymAttendance(
                    dateKey = dateKey,
                    note = note,
                    durationMinutes = durationMinutes,
                    allowMultipleForSameDate = allowMulti
                )
            ) {
                is AttendanceCheckInResult.Success -> {
                    val readable = AnalyticsAndInsightsEngine.formatDateKeyReadable(result.dateKey)
                    postMessage("Gym attendance recorded for $readable.")
                }
                is AttendanceCheckInResult.DuplicateDateBlocked -> {
                    val readable = AnalyticsAndInsightsEngine.formatDateKeyReadable(result.dateKey)
                    postMessage("Attendance already logged for $readable. Enable multiple daily visits in Settings if needed.")
                }
                is AttendanceCheckInResult.InvalidInput -> {
                    postMessage(result.message)
                }
            }
        }
    }

    fun updateGymAttendance(record: GymAttendanceEntity, newDateKey: String, newNote: String, newDurationMinutes: Int) {
        viewModelScope.launch {
            val cleanDate = newDateKey.trim()
            if (!cleanDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                postMessage("Please use YYYY-MM-DD date format.")
                return@launch
            }
            val todayKey = AnalyticsAndInsightsEngine.todayDateKey()
            if (cleanDate > todayKey) {
                postMessage("Cannot set completed attendance to a future date.")
                return@launch
            }
            repository.updateGymAttendance(
                record.copy(
                    dateKey = cleanDate,
                    note = newNote.trim(),
                    durationMinutes = newDurationMinutes.coerceIn(5, 360)
                )
            )
            postMessage("Attendance record updated.")
        }
    }

    fun deleteGymAttendance(id: Long) {
        viewModelScope.launch {
            repository.deleteGymAttendance(id)
            postMessage("Attendance record deleted.")
        }
    }

    // --- TWO-STAGE WORKOUT PLAN BUILDER & LIVE EXERCISE LOGGER ---

    /**
     * Opens the Workouts tab in a completely blank Plan Builder state (or keeps existing work if already started).
     */
    fun openWorkoutPlanBuilder() {
        controlState.update { state ->
            state.copy(currentTab = AppDestination.WORKOUTS)
        }
    }

    /**
     * Resets the Active Logger to a 100% blank plan builder.
     */
    fun startBlankWorkout(
        title: String = "",
        category: String = "Strength",
        dateKey: String = AnalyticsAndInsightsEngine.todayDateKey()
    ) {
        workoutTimerJob?.cancel()
        stopRestTimer()
        val defaultRest = uiState.value.profile.defaultRestSeconds
        controlState.update {
            it.copy(
                currentTab = AppDestination.WORKOUTS,
                activeWorkout = ActiveWorkoutEditorState(
                    editingSessionId = null,
                    isWorkoutActive = false,
                    title = title,
                    category = category,
                    dateKey = dateKey,
                    notes = "",
                    activeExerciseName = null,
                    activeExerciseElapsedSeconds = 0,
                    exerciseElapsedSecondsMap = emptyMap(),
                    elapsedSeconds = 0,
                    isTimerRunning = false,
                    restTimerTotalSeconds = defaultRest,
                    sets = emptyList()
                )
            )
        }
    }

    /**
     * Transitions from Stage 1 (Blank/Custom Plan Builder) into Stage 2 (Live Workout Execution).
     * Does NOT auto-start the timer until the user clicks on a particular exercise in their plan!
     */
    fun startPlannedWorkout() {
        val current = controlState.value.activeWorkout
        if (current.sets.isEmpty()) {
            postMessage("Add at least one exercise to your plan before starting the workout.")
            return
        }
        controlState.update { state ->
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    isWorkoutActive = true,
                    title = state.activeWorkout.title.trim().ifBlank { "Workout Session" },
                    activeExerciseName = null,
                    isTimerRunning = false
                )
            )
        }
        postMessage("Workout started! Tap any exercise in your plan below to register it and start the live timer.")
    }

    /**
     * Saves the exercises currently in the Plan Builder as a reusable Routine ("Save Plan for Next Time").
     */
    fun saveCurrentPlanForNextTime() {
        val current = controlState.value.activeWorkout
        val distinctExercises = current.sets.map { it.exerciseName.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinctExercises.isEmpty()) {
            postMessage("Add at least one exercise to your plan before saving it for next time.")
            return
        }
        val planName = current.title.trim().ifBlank {
            "Custom Plan (${distinctExercises.take(2).joinToString(" & ")})"
        }
        viewModelScope.launch {
            repository.saveWorkoutRoutine(
                id = null,
                name = planName,
                category = current.category.ifBlank { "Strength" },
                targetDurationMin = (distinctExercises.size * 15).coerceIn(20, 180),
                exercisesList = distinctExercises
            )
            controlState.update { state ->
                state.copy(activeWorkout = state.activeWorkout.copy(title = planName))
            }
            postMessage("Saved '$planName' for next time! You can start it now or anytime from your Plans.")
        }
    }

    /**
     * When the workout is active and the user clicks on a particular exercise from their saved plan:
     * 1. Registers that exercise as the active exercise in focus (`activeExerciseName`).
     * 2. Automatically starts the live timer (`isTimerRunning = true`) for that exercise & workout!
     */
    fun selectAndStartExerciseInWorkout(exerciseName: String) {
        controlState.update { state ->
            val current = state.activeWorkout
            val previousSecondsForExercise = current.exerciseElapsedSecondsMap[exerciseName] ?: 0
            state.copy(
                activeWorkout = current.copy(
                    isWorkoutActive = true,
                    activeExerciseName = exerciseName,
                    activeExerciseElapsedSeconds = previousSecondsForExercise,
                    isTimerRunning = true
                )
            )
        }
        startWorkoutTimerLoop()
        postMessage("Active exercise: $exerciseName — Live timer started!")
    }

    /**
     * Registers/completes a specific set when clicked by the user during the live workout,
     * and automatically triggers the Rest Timer if enabled or requested.
     */
    fun registerSetCompleted(localId: Long, triggerRestTimer: Boolean = true) {
        val defaultRest = uiState.value.profile.defaultRestSeconds
        var targetRestSec = defaultRest
        var newlyCompleted = false

        controlState.update { state ->
            val current = state.activeWorkout
            var clickedExerciseName: String? = current.activeExerciseName
            val updatedSets = current.sets.map { entry ->
                if (entry.localId == localId) {
                    clickedExerciseName = entry.exerciseName
                    val nextStatus = if (entry.status == "COMPLETED") "PLANNED" else "COMPLETED"
                    newlyCompleted = (nextStatus == "COMPLETED")
                    targetRestSec = entry.restSeconds.takeIf { it > 0 } ?: defaultRest
                    entry.copy(status = nextStatus)
                } else {
                    entry
                }
            }
            state.copy(
                activeWorkout = current.copy(
                    isWorkoutActive = true,
                    activeExerciseName = clickedExerciseName,
                    isTimerRunning = true,
                    sets = updatedSets
                )
            )
        }
        startWorkoutTimerLoop()
        if (newlyCompleted && (triggerRestTimer || uiState.value.profile.autoStartRestTimer)) {
            startRestTimer(targetRestSec)
        }
    }

    /**
     * Loads a saved Routine/Plan into the Workout Plan stage with all sets in "PLANNED" status
     * so nothing is pre-completed until the user clicks the exercise and registers sets.
     */
    fun startWorkoutFromRoutine(routine: WorkoutTemplateEntity, startImmediately: Boolean = true) {
        workoutTimerJob?.cancel()
        stopRestTimer()
        val defaultRest = uiState.value.profile.defaultRestSeconds
        val exerciseCatalog = uiState.value.allExercises.associateBy { it.name.lowercase() }
        val exerciseNames = routine.exercisesCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val draftSets = mutableListOf<DraftSetEntry>()
        exerciseNames.forEachIndexed { orderIdx, exName ->
            val meta = exerciseCatalog[exName.lowercase()]
            val muscle = meta?.muscleGroup ?: "Full Body"
            val isBw = meta?.isBodyweight ?: false
            val isAssist = meta?.isAssisted ?: false
            for (setNum in 1..3) {
                draftSets.add(
                    DraftSetEntry(
                        localId = nextDraftSetId++,
                        exerciseName = exName,
                        muscleGroup = muscle,
                        exerciseOrder = orderIdx,
                        setNumber = setNum,
                        weightKg = if (isBw) 0f else 40f,
                        reps = 8,
                        restSeconds = defaultRest,
                        status = "PLANNED",
                        isBodyweight = isBw,
                        isAssisted = isAssist
                    )
                )
            }
        }

        controlState.update {
            it.copy(
                currentTab = AppDestination.WORKOUTS,
                activeWorkout = ActiveWorkoutEditorState(
                    editingSessionId = null,
                    isWorkoutActive = startImmediately,
                    title = routine.name,
                    category = routine.category,
                    dateKey = AnalyticsAndInsightsEngine.todayDateKey(),
                    notes = "",
                    activeExerciseName = null,
                    activeExerciseElapsedSeconds = 0,
                    exerciseElapsedSecondsMap = emptyMap(),
                    elapsedSeconds = 0,
                    isTimerRunning = false,
                    restTimerTotalSeconds = defaultRest,
                    sets = draftSets
                )
            )
        }
        postMessage(
            if (startImmediately) {
                "Loaded plan '${routine.name}'. Tap any exercise below to register it and start the timer!"
            } else {
                "Loaded '${routine.name}' into Plan Builder."
            }
        )
    }

    /**
     * Allows 1-tap instant set logging directly from the Exercises tab so the user
     * can record real sets and immediately see real-time progressive overload charts & rings update.
     */
    fun quickLogExerciseSession(
        exerciseName: String,
        muscleGroup: String,
        weightDisplay: Float,
        reps: Int,
        setsCount: Int = 3
    ) {
        val cleanName = exerciseName.trim()
        if (cleanName.isBlank() || reps <= 0) {
            postMessage("Enter valid weight and repetitions to log sets.")
            return
        }
        viewModelScope.launch {
            val profile = uiState.value.profile
            val useLbs = uiState.value.useLbs
            val weightKg = if (useLbs) weightDisplay / 2.20462f else weightDisplay
            val meta = uiState.value.allExercises.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
            val isBw = meta?.isBodyweight ?: false
            val isAssist = meta?.isAssisted ?: false

            val entries = (1..setsCount.coerceIn(1, 8)).map { setNum ->
                DraftSetEntry(
                    localId = nextDraftSetId++,
                    exerciseName = cleanName,
                    muscleGroup = muscleGroup.ifBlank { meta?.muscleGroup ?: "Full Body" },
                    exerciseOrder = 0,
                    setNumber = setNum,
                    weightKg = weightKg.coerceAtLeast(0f),
                    reps = reps.coerceAtLeast(1),
                    restSeconds = profile.defaultRestSeconds,
                    status = "COMPLETED",
                    isBodyweight = isBw,
                    isAssisted = isAssist
                )
            }

            repository.saveWorkoutSession(
                existingSessionId = null,
                title = "$cleanName Session",
                category = meta?.category ?: "Strength",
                dateKey = AnalyticsAndInsightsEngine.todayDateKey(),
                durationMinutes = (setsCount * 4).coerceAtLeast(15),
                notes = "Logged via Progressive Overload Tracker",
                isDraft = false,
                sets = entries,
                userBodyweightKg = profile.weightKg,
                autoRecordAttendanceOnComplete = profile.autoCheckInOnWorkoutComplete
            )
            controlState.update { it.copy(selectedExerciseForDetail = cleanName) }
            postMessage("Logged $setsCount sets of $cleanName ($weightDisplay ${uiState.value.unitLabel} × $reps reps)!")
        }
    }

    fun loadSessionIntoEditor(session: WorkoutSessionEntity, duplicateAsNewToday: Boolean = false) {
        viewModelScope.launch {
            val sessionSets = repository.getSetsForSession(session.id)
            val draftEntries = sessionSets.map { s ->
                DraftSetEntry(
                    localId = nextDraftSetId++,
                    exerciseName = s.exerciseName,
                    muscleGroup = s.muscleGroup,
                    exerciseOrder = s.exerciseOrder,
                    setNumber = s.setNumber,
                    weightKg = s.weightKg,
                    reps = s.reps,
                    durationSeconds = s.durationSeconds,
                    distanceKm = s.distanceKm,
                    restSeconds = s.restSeconds,
                    rpe = s.rpe,
                    setType = s.setType,
                    status = if (duplicateAsNewToday) "PLANNED" else s.status,
                    isBodyweight = s.isBodyweight,
                    isAssisted = s.isAssisted,
                    notes = s.notes
                )
            }

            val targetDateKey = if (duplicateAsNewToday) {
                AnalyticsAndInsightsEngine.todayDateKey()
            } else {
                session.dateKey.ifBlank { AnalyticsAndInsightsEngine.epochMillisToDateKey(session.timestampMillis) }
            }

            workoutTimerJob?.cancel()
            controlState.update {
                it.copy(
                    currentTab = AppDestination.WORKOUTS,
                    activeWorkout = ActiveWorkoutEditorState(
                        editingSessionId = if (duplicateAsNewToday) null else session.id,
                        isWorkoutActive = true,
                        title = if (duplicateAsNewToday) session.title else session.title,
                        category = session.category,
                        dateKey = targetDateKey,
                        notes = session.notes,
                        activeExerciseName = null,
                        elapsedSeconds = if (duplicateAsNewToday) 0 else session.durationMinutes * 60,
                        isTimerRunning = false,
                        sets = draftEntries
                    )
                )
            }
            postMessage(
                if (duplicateAsNewToday) "Loaded '${session.title}' plan. Tap any exercise to start the timer!"
                else "Loaded '${session.title}' for editing."
            )
        }
    }

    fun updateActiveWorkoutMeta(
        title: String? = null,
        category: String? = null,
        dateKey: String? = null,
        notes: String? = null
    ) {
        controlState.update { state ->
            val current = state.activeWorkout
            state.copy(
                activeWorkout = current.copy(
                    title = title ?: current.title,
                    category = category ?: current.category,
                    dateKey = dateKey ?: current.dateKey,
                    notes = notes ?: current.notes
                )
            )
        }
    }

    fun toggleWorkoutTimer() {
        val currentlyRunning = controlState.value.activeWorkout.isTimerRunning
        controlState.update { state ->
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    isWorkoutActive = true,
                    isTimerRunning = !currentlyRunning
                )
            )
        }
        if (!currentlyRunning) {
            startWorkoutTimerLoop()
        } else {
            workoutTimerJob?.cancel()
        }
        syncOriginIslandNow()
    }

    fun syncOriginIslandNow() {
        val app = getApplication<Application>()
        val currentActive = controlState.value.activeWorkout
        val useLbs = uiState.value.useLbs
        val unitLabel = uiState.value.unitLabel
        OriginIslandLiveManager.syncOriginIslandState(
            context = app,
            activeWorkout = currentActive,
            unitLabel = unitLabel,
            useLbs = useLbs
        )
    }

    fun setDefaultRestSeconds(seconds: Int) {
        val safe = seconds.coerceIn(15, 600)
        viewModelScope.launch {
            val cur = uiState.value.profile
            repository.updateProfile(cur.copy(defaultRestSeconds = safe))
            controlState.update { state ->
                state.copy(
                    activeWorkout = state.activeWorkout.copy(
                        restTimerTotalSeconds = safe
                    )
                )
            }
            postMessage("Rest timer set to ${safe}s")
        }
    }

    private fun startWorkoutTimerLoop() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (controlState.value.activeWorkout.isWorkoutActive && controlState.value.activeWorkout.isTimerRunning) {
                delay(1000L)
                controlState.update { state ->
                    val cur = state.activeWorkout
                    val activeEx = cur.activeExerciseName
                    val updatedExSeconds = if (activeEx != null) {
                        (cur.exerciseElapsedSecondsMap[activeEx] ?: cur.activeExerciseElapsedSeconds) + 1
                    } else {
                        cur.activeExerciseElapsedSeconds
                    }
                    val updatedMap = if (activeEx != null) {
                        cur.exerciseElapsedSecondsMap + (activeEx to updatedExSeconds)
                    } else {
                        cur.exerciseElapsedSecondsMap
                    }
                    state.copy(
                        activeWorkout = cur.copy(
                            elapsedSeconds = cur.elapsedSeconds + 1,
                            activeExerciseElapsedSeconds = updatedExSeconds,
                            exerciseElapsedSecondsMap = updatedMap
                        )
                    )
                }
                syncOriginIslandNow()
            }
        }
    }

    fun startRestTimer(seconds: Int = uiState.value.profile.defaultRestSeconds) {
        restTimerJob?.cancel()
        val safeSec = seconds.coerceIn(10, 600)
        controlState.update { state ->
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    restTimerRemainingSeconds = safeSec,
                    restTimerTotalSeconds = safeSec,
                    isRestTimerRunning = true
                )
            )
        }
        syncOriginIslandNow()
        restTimerJob = viewModelScope.launch {
            while (controlState.value.activeWorkout.restTimerRemainingSeconds > 0 &&
                controlState.value.activeWorkout.isRestTimerRunning
            ) {
                delay(1000L)
                controlState.update { state ->
                    val rem = (state.activeWorkout.restTimerRemainingSeconds - 1).coerceAtLeast(0)
                    state.copy(
                        activeWorkout = state.activeWorkout.copy(
                            restTimerRemainingSeconds = rem,
                            isRestTimerRunning = rem > 0
                        )
                    )
                }
                syncOriginIslandNow()
            }
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        controlState.update { state ->
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    restTimerRemainingSeconds = 0,
                    isRestTimerRunning = false
                )
            )
        }
        syncOriginIslandNow()
    }

    /**
     * Adds an exercise to the current Workout Plan (or Active Workout).
     * Sets start as "PLANNED" so they only register when the user clicks the exercise and logs them!
     */
    fun addExerciseToActiveWorkout(
        exerciseName: String,
        muscleGroup: String,
        initialWeightDisplay: Float = 40f,
        initialReps: Int = 8,
        initialSetsCount: Int = 3
    ) {
        val cleanName = exerciseName.trim()
        if (cleanName.isBlank()) {
            postMessage("Please select or enter an exercise name.")
            return
        }
        val useLbs = uiState.value.useLbs
        val weightKg = if (useLbs) initialWeightDisplay / 2.20462f else initialWeightDisplay
        val meta = uiState.value.allExercises.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        val isBw = meta?.isBodyweight ?: false
        val isAssist = meta?.isAssisted ?: false
        val defaultRest = uiState.value.profile.defaultRestSeconds

        controlState.update { state ->
            val currentSets = state.activeWorkout.sets.toMutableList()
            val existingForExercise = currentSets.filter { it.exerciseName.equals(cleanName, ignoreCase = true) }
            val exerciseOrder = existingForExercise.firstOrNull()?.exerciseOrder
                ?: ((currentSets.maxOfOrNull { it.exerciseOrder } ?: -1) + 1)
            val startingSetNumber = (existingForExercise.maxOfOrNull { it.setNumber } ?: 0) + 1

            for (i in 0 until initialSetsCount.coerceIn(1, 10)) {
                currentSets.add(
                    DraftSetEntry(
                        localId = nextDraftSetId++,
                        exerciseName = cleanName,
                        muscleGroup = muscleGroup.ifBlank { meta?.muscleGroup ?: "Full Body" },
                        exerciseOrder = exerciseOrder,
                        setNumber = startingSetNumber + i,
                        weightKg = if (isBw && initialWeightDisplay == 0f) 0f else weightKg.coerceAtLeast(0f),
                        reps = initialReps.coerceAtLeast(1),
                        restSeconds = defaultRest,
                        status = "PLANNED",
                        isBodyweight = isBw,
                        isAssisted = isAssist
                    )
                )
            }

            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    sets = normalizeSetNumbersAndOrder(currentSets)
                )
            )
        }
        postMessage("Added $cleanName ($initialSetsCount sets) to your workout plan.")
    }

    fun addSingleSetToExerciseInActiveWorkout(exerciseName: String) {
        val defaultRest = uiState.value.profile.defaultRestSeconds
        controlState.update { state ->
            val currentSets = state.activeWorkout.sets.toMutableList()
            val existing = currentSets.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
            val lastSet = existing.lastOrNull()
            val newSet = DraftSetEntry(
                localId = nextDraftSetId++,
                exerciseName = exerciseName,
                muscleGroup = lastSet?.muscleGroup ?: "Full Body",
                exerciseOrder = lastSet?.exerciseOrder ?: ((currentSets.maxOfOrNull { it.exerciseOrder } ?: -1) + 1),
                setNumber = (existing.maxOfOrNull { it.setNumber } ?: 0) + 1,
                weightKg = lastSet?.weightKg ?: 40f,
                reps = lastSet?.reps ?: 8,
                restSeconds = lastSet?.restSeconds ?: defaultRest,
                status = "PLANNED",
                isBodyweight = lastSet?.isBodyweight ?: false,
                isAssisted = lastSet?.isAssisted ?: false
            )
            currentSets.add(newSet)
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    sets = normalizeSetNumbersAndOrder(currentSets)
                )
            )
        }
    }

    fun updateDraftSet(
        localId: Long,
        weightDisplay: Float? = null,
        reps: Int? = null,
        restSeconds: Int? = null,
        status: String? = null,
        notes: String? = null
    ) {
        val useLbs = uiState.value.useLbs
        controlState.update { state ->
            val updated = state.activeWorkout.sets.map { entry ->
                if (entry.localId == localId) {
                    val newWeightKg = if (weightDisplay != null) {
                        val raw = weightDisplay.coerceAtLeast(0f)
                        if (useLbs) raw / 2.20462f else raw
                    } else {
                        entry.weightKg
                    }
                    entry.copy(
                        weightKg = newWeightKg,
                        reps = reps?.coerceAtLeast(0) ?: entry.reps,
                        restSeconds = restSeconds?.coerceIn(0, 600) ?: entry.restSeconds,
                        status = status ?: entry.status,
                        notes = notes ?: entry.notes
                    )
                } else {
                    entry
                }
            }
            state.copy(activeWorkout = state.activeWorkout.copy(sets = updated))
        }
    }

    fun removeDraftSet(localId: Long) {
        controlState.update { state ->
            val remaining = state.activeWorkout.sets.filterNot { it.localId == localId }
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    sets = normalizeSetNumbersAndOrder(remaining)
                )
            )
        }
    }

    fun removeExerciseFromActiveWorkout(exerciseName: String) {
        controlState.update { state ->
            val remaining = state.activeWorkout.sets.filterNot {
                it.exerciseName.equals(exerciseName, ignoreCase = true)
            }
            val nextActive = if (state.activeWorkout.activeExerciseName.equals(exerciseName, ignoreCase = true)) {
                null
            } else {
                state.activeWorkout.activeExerciseName
            }
            state.copy(
                activeWorkout = state.activeWorkout.copy(
                    activeExerciseName = nextActive,
                    sets = normalizeSetNumbersAndOrder(remaining)
                )
            )
        }
    }

    fun moveExerciseOrderInActiveWorkout(exerciseName: String, moveUp: Boolean) {
        controlState.update { state ->
            val grouped = state.activeWorkout.sets
                .groupBy { it.exerciseName }
                .entries
                .sortedBy { it.value.firstOrNull()?.exerciseOrder ?: 0 }
                .map { it.key to it.value }
                .toMutableList()

            val currentIdx = grouped.indexOfFirst { it.first.equals(exerciseName, ignoreCase = true) }
            if (currentIdx == -1) return@update state
            val targetIdx = if (moveUp) currentIdx - 1 else currentIdx + 1
            if (targetIdx !in grouped.indices) return@update state

            val temp = grouped[currentIdx]
            grouped[currentIdx] = grouped[targetIdx]
            grouped[targetIdx] = temp

            val reorderedSets = mutableListOf<DraftSetEntry>()
            grouped.forEachIndexed { newOrder, (_, list) ->
                list.sortedBy { it.setNumber }.forEachIndexed { setIdx, item ->
                    reorderedSets.add(
                        item.copy(
                            exerciseOrder = newOrder,
                            setNumber = setIdx + 1
                        )
                    )
                }
            }
            state.copy(activeWorkout = state.activeWorkout.copy(sets = reorderedSets))
        }
    }

    private fun normalizeSetNumbersAndOrder(sets: List<DraftSetEntry>): List<DraftSetEntry> {
        val grouped = sets
            .groupBy { it.exerciseName }
            .entries
            .sortedBy { it.value.firstOrNull()?.exerciseOrder ?: 0 }

        val normalized = mutableListOf<DraftSetEntry>()
        grouped.forEachIndexed { orderIdx, (_, exSets) ->
            exSets.forEachIndexed { setIdx, item ->
                normalized.add(
                    item.copy(
                        exerciseOrder = orderIdx,
                        setNumber = setIdx + 1
                    )
                )
            }
        }
        return normalized
    }

    fun saveActiveWorkout(asDraft: Boolean) {
        val current = controlState.value.activeWorkout
        if (current.sets.isEmpty()) {
            postMessage("Add at least one exercise set before saving your workout.")
            return
        }
        val cleanDate = current.dateKey.trim()
        if (!cleanDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            postMessage("Workout date must be in YYYY-MM-DD format.")
            return
        }
        if (!asDraft && cleanDate > AnalyticsAndInsightsEngine.todayDateKey()) {
            postMessage("Cannot complete a workout for a future date ($cleanDate). Save as Draft or choose today's date.")
            return
        }

        // If completing workout and user hasn't toggled individual sets to COMPLETED yet, mark planned sets as COMPLETED
        val setsToPersist = if (!asDraft && current.sets.none { it.status == "COMPLETED" }) {
            current.sets.map { if (it.status == "PLANNED") it.copy(status = "COMPLETED") else it }
        } else {
            current.sets
        }

        viewModelScope.launch {
            workoutTimerJob?.cancel()
            stopRestTimer()
            OriginIslandLiveManager.cancelOriginIsland(getApplication())
            val durationMinutes = ((current.elapsedSeconds / 60).coerceAtLeast(15))
            val profile = uiState.value.profile
            val cleanTitle = current.title.trim().ifBlank { "Strength Session" }
            val savedId = repository.saveWorkoutSession(
                existingSessionId = current.editingSessionId,
                title = cleanTitle,
                category = current.category,
                dateKey = cleanDate,
                durationMinutes = durationMinutes,
                notes = current.notes,
                isDraft = asDraft,
                sets = setsToPersist,
                userBodyweightKg = profile.weightKg,
                autoRecordAttendanceOnComplete = profile.autoCheckInOnWorkoutComplete
            )
            controlState.update { state ->
                state.copy(
                    selectedHistoricalSessionId = savedId,
                    activeWorkout = ActiveWorkoutEditorState()
                )
            }
            postMessage(
                if (asDraft) "Workout plan saved as draft."
                else "Workout '$cleanTitle' completed and saved!"
            )
        }
    }

    fun discardActiveWorkout() {
        workoutTimerJob?.cancel()
        stopRestTimer()
        controlState.update { state ->
            state.copy(activeWorkout = ActiveWorkoutEditorState())
        }
        postMessage("Cleared workout plan builder.")
    }

    fun selectHistoricalSession(sessionId: Long?) {
        controlState.update { it.copy(selectedHistoricalSessionId = sessionId) }
    }

    fun deleteWorkoutSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutSession(sessionId)
            controlState.update { state ->
                state.copy(
                    selectedHistoricalSessionId = if (state.selectedHistoricalSessionId == sessionId) null else state.selectedHistoricalSessionId
                )
            }
            postMessage("Workout session deleted.")
        }
    }

    // --- ROUTINES CRUD ---

    fun saveWorkoutRoutine(
        id: Long? = null,
        name: String,
        category: String,
        targetDurationMin: Int,
        exercisesList: List<String>
    ) {
        if (name.trim().isBlank()) {
            postMessage("Please enter a routine name.")
            return
        }
        if (exercisesList.isEmpty()) {
            postMessage("Select at least one exercise from the dropdown for the routine.")
            return
        }
        viewModelScope.launch {
            repository.saveWorkoutRoutine(
                id = id,
                name = name,
                category = category,
                targetDurationMin = targetDurationMin,
                exercisesList = exercisesList
            )
            postMessage(if (id == null) "Routine '$name' created." else "Routine '$name' updated.")
        }
    }

    fun deleteWorkoutRoutine(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutRoutine(id)
            postMessage("Workout routine deleted.")
        }
    }

    // --- EXERCISE LIBRARY & PROGRESSIVE OVERLOAD ---

    fun setExerciseSearchQuery(query: String) {
        controlState.update { it.copy(exerciseSearchQuery = query) }
    }

    fun setMuscleFilter(muscle: String) {
        controlState.update { it.copy(selectedMuscleFilter = muscle) }
    }

    fun setEquipmentFilter(equipment: String) {
        controlState.update { it.copy(selectedEquipmentFilter = equipment) }
    }

    fun setShowArchivedExercises(show: Boolean) {
        controlState.update { it.copy(showArchivedExercises = show) }
    }

    fun selectExerciseForDetail(exerciseName: String?) {
        controlState.update { it.copy(selectedExerciseForDetail = exerciseName) }
    }

    fun saveCustomExercise(
        id: Long? = null,
        name: String,
        muscleGroup: String,
        secondaryMuscles: String,
        equipment: String,
        category: String = "Strength",
        instructions: String,
        isBodyweight: Boolean = false,
        isAssisted: Boolean = false
    ) {
        if (name.trim().isBlank()) {
            postMessage("Exercise name cannot be empty.")
            return
        }
        viewModelScope.launch {
            repository.saveExercise(
                id = id,
                name = name,
                muscleGroup = muscleGroup,
                secondaryMuscles = secondaryMuscles,
                equipment = equipment,
                category = category,
                instructions = instructions,
                isBodyweight = isBodyweight,
                isAssisted = isAssisted
            )
            controlState.update { it.copy(selectedExerciseForDetail = name.trim()) }
            postMessage(if (id == null) "Exercise '$name' added to library." else "Exercise '$name' updated.")
        }
    }

    fun setExerciseArchived(id: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.setExerciseArchived(id, archived)
            postMessage(if (archived) "Exercise archived (historical sets preserved)." else "Exercise restored to active library.")
        }
    }

    fun deleteExercisePreservingHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteExercisePreservingHistory(id)
            postMessage("Exercise removed from library. Historical workout records remain intact.")
        }
    }

    // --- SETTINGS, UNITS, THEME & FORMATTED PDF EXPORT ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setPreferredWeightUnit(unit: String) {
        val clean = if (unit.equals("lbs", ignoreCase = true)) "lbs" else "kg"
        viewModelScope.launch {
            val current = uiState.value.profile
            repository.updateProfile(current.copy(preferredWeightUnit = clean))
            postMessage("Display unit updated to ${clean.uppercase()}.")
        }
    }

    fun updateProfileSettings(
        displayName: String,
        weightDisplay: Float,
        heightCm: Float,
        fitnessLevel: String,
        primaryObjective: String,
        weeklyAttendanceGoal: Int,
        trainingDaysCsv: String,
        allowMultipleVisitsPerDay: Boolean,
        defaultRestSeconds: Int = uiState.value.profile.defaultRestSeconds,
        overloadIncrementKg: Float = uiState.value.profile.overloadIncrementKg,
        barbellWeightKg: Float = uiState.value.profile.barbellWeightKg,
        autoStartRestTimer: Boolean = uiState.value.profile.autoStartRestTimer,
        autoCheckInOnWorkoutComplete: Boolean = uiState.value.profile.autoCheckInOnWorkoutComplete
    ) {
        viewModelScope.launch {
            val current = uiState.value.profile
            val weightKg = if (uiState.value.useLbs) weightDisplay / 2.20462f else weightDisplay
            repository.updateProfile(
                current.copy(
                    displayName = displayName.trim().ifBlank { "Athlete" },
                    weightKg = weightKg.coerceIn(25f, 300f),
                    heightCm = heightCm.coerceIn(100f, 250f),
                    fitnessLevel = fitnessLevel,
                    primaryObjective = primaryObjective,
                    weeklyAttendanceGoal = weeklyAttendanceGoal.coerceIn(1, 7),
                    trainingDaysCsv = trainingDaysCsv,
                    allowMultipleVisitsPerDay = allowMultipleVisitsPerDay,
                    defaultRestSeconds = defaultRestSeconds.coerceIn(15, 600),
                    overloadIncrementKg = overloadIncrementKg.coerceIn(0.5f, 20f),
                    barbellWeightKg = barbellWeightKg.coerceIn(5f, 45f),
                    autoStartRestTimer = autoStartRestTimer,
                    autoCheckInOnWorkoutComplete = autoCheckInOnWorkoutComplete
                )
            )
            postMessage("Athlete profile, overload increments & timer preferences saved.")
        }
    }

    fun generateFormattedPdfReport() {
        val snapshot = uiState.value
        controlState.update { it.copy(isGeneratingPdf = true) }
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                PdfReportGenerator.generateFormattedPdfReport(
                    context = getApplication(),
                    profile = snapshot.profile,
                    attendanceOverview = snapshot.attendanceOverview,
                    attendanceRecords = snapshot.allAttendanceRecords,
                    dashboardAnalytics = snapshot.dashboardAnalytics,
                    completedSessions = snapshot.completedSessions,
                    allSets = snapshot.allSets
                )
            }
            controlState.update {
                it.copy(
                    generatedPdfReport = report,
                    isGeneratingPdf = false,
                    statusMessage = "Formatted PDF Report (${report.pageCount} page${if (report.pageCount > 1) "s" else ""}) generated."
                )
            }
        }
    }

    fun saveGeneratedPdfToUri(destinationUri: Uri) {
        val report = controlState.value.generatedPdfReport ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = PdfReportGenerator.copyPdfToUri(
                context = getApplication(),
                sourceFile = report.file,
                destinationUri = destinationUri
            )
            withContext(Dispatchers.Main) {
                if (ok) {
                    postMessage("PDF report saved to device: ${report.fileName}")
                } else {
                    postMessage("Unable to write PDF file to the selected location.")
                }
            }
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            workoutTimerJob?.cancel()
            stopRestTimer()
            // Save an automatic safety snapshot right before clearing so user can always restore if needed
            withContext(Dispatchers.IO) {
                runCatching {
                    repository.saveLocalBackupSnapshot(
                        themeMode = uiState.value.themeMode,
                        isAutoBackup = true
                    )
                }
            }
            repository.clearAllUserData()
            val snapshots = withContext(Dispatchers.IO) {
                repository.listLocalBackupSnapshots()
            }
            controlState.update {
                UiControlState(
                    currentTab = AppDestination.SUMMARY,
                    localBackupSnapshots = snapshots,
                    statusMessage = "All local records cleared (auto-backup snapshot preserved in Settings)."
                )
            }
        }
    }

    // --- FULL APP BACKUP & RESTORE (EXPORT / IMPORT / SNAPSHOTS) ---

    fun refreshLocalBackupSnapshots() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.saveLocalBackupSnapshot(
                    themeMode = uiState.value.themeMode,
                    isAutoBackup = true
                )
            }
            val list = repository.listLocalBackupSnapshots()
            controlState.update { it.copy(localBackupSnapshots = list) }
        }
    }

    fun createManualBackupSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = repository.saveLocalBackupSnapshot(
                themeMode = uiState.value.themeMode,
                isAutoBackup = false
            )
            val list = repository.listLocalBackupSnapshots()
            controlState.update {
                it.copy(
                    localBackupSnapshots = list,
                    statusMessage = "Backup snapshot saved (${info.attendanceCount} check-ins, ${info.sessionsCount} workouts)."
                )
            }
        }
    }

    fun exportBackupJsonToUri(destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.exportBackupToUri(
                themeMode = uiState.value.themeMode,
                destinationUri = destinationUri
            )
            val list = repository.listLocalBackupSnapshots()
            controlState.update {
                it.copy(
                    localBackupSnapshots = list,
                    statusMessage = if (ok) {
                        "Full database state (.json) exported to device storage."
                    } else {
                        "Unable to write JSON backup file to the selected destination."
                    }
                )
            }
        }
    }

    fun exportBackupPdfToUri(destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.exportBackupPdfToUri(
                themeMode = uiState.value.themeMode,
                destinationUri = destinationUri
            )
            val list = repository.listLocalBackupSnapshots()
            controlState.update {
                it.copy(
                    localBackupSnapshots = list,
                    statusMessage = if (ok) {
                        "Full database state (.pdf with embedded restorable backup) exported to device storage."
                    } else {
                        "Unable to write PDF backup file to the selected destination."
                    }
                )
            }
        }
    }

    fun importBackupJsonFromUri(sourceUri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importBackupFromUri(
                sourceUri = sourceUri,
                replaceExisting = replaceExisting
            )
            val list = repository.listLocalBackupSnapshots()
            result.fold(
                onSuccess = { summary ->
                    controlState.update {
                        it.copy(
                            localBackupSnapshots = list,
                            statusMessage = "Backup restored: ${summary.attendanceRestored} visits, ${summary.sessionsRestored} workouts, ${summary.setsRestored} sets, ${summary.routinesRestored} plans."
                        )
                    }
                },
                onFailure = { err ->
                    controlState.update {
                        it.copy(
                            localBackupSnapshots = list,
                            statusMessage = "Backup import failed: ${err.message ?: "Invalid backup JSON format."}"
                        )
                    }
                }
            )
        }
    }

    fun restoreFromLocalSnapshot(snapshot: LocalBackupSnapshotInfo, replaceExisting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonText = runCatching { File(snapshot.filePath).readText(Charsets.UTF_8) }.getOrNull()
            if (jsonText.isNullOrBlank()) {
                postMessage("Snapshot file could not be read.")
                return@launch
            }
            val result = repository.restoreFromBackupJsonString(
                jsonText = jsonText,
                replaceExisting = replaceExisting
            )
            val list = repository.listLocalBackupSnapshots()
            result.fold(
                onSuccess = { summary ->
                    controlState.update {
                        it.copy(
                            localBackupSnapshots = list,
                            statusMessage = "Snapshot restored: ${summary.attendanceRestored} visits, ${summary.sessionsRestored} workouts, ${summary.setsRestored} sets."
                        )
                    }
                },
                onFailure = { err ->
                    controlState.update {
                        it.copy(
                            localBackupSnapshots = list,
                            statusMessage = "Snapshot restore failed: ${err.message ?: "Invalid format"}"
                        )
                    }
                }
            )
        }
    }

    fun restoreFromBackupJsonText(jsonText: String, replaceExisting: Boolean) {
        if (jsonText.isBlank()) {
            postMessage("Please paste valid FitPulse backup JSON first.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.restoreFromBackupJsonString(
                jsonText = jsonText,
                replaceExisting = replaceExisting
            )
            val list = repository.listLocalBackupSnapshots()
            result.fold(
                onSuccess = { summary ->
                    controlState.update {
                        it.copy(
                            localBackupSnapshots = list,
                            statusMessage = "Data restored from JSON: ${summary.attendanceRestored} visits, ${summary.sessionsRestored} workouts, ${summary.setsRestored} sets."
                        )
                    }
                },
                onFailure = { err ->
                    controlState.update {
                        it.copy(
                            localBackupSnapshots = list,
                            statusMessage = "JSON restore failed: ${err.message ?: "Malformed JSON"}"
                        )
                    }
                }
            )
        }
    }

    fun generateBackupJsonString(onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.createFullBackupJsonString(uiState.value.themeMode)
            withContext(Dispatchers.Main) {
                onReady(json)
            }
        }
    }

    // --- DISPLAY CONVERSION HELPERS ---

    fun formatWeight(weightKg: Float): String {
        val useLbs = uiState.value.useLbs
        val converted = if (useLbs) weightKg * 2.20462f else weightKg
        val rounded = (converted * 10f).roundToInt() / 10f
        return if (rounded % 1f == 0f) {
            "${rounded.toInt()} ${uiState.value.unitLabel}"
        } else {
            "$rounded ${uiState.value.unitLabel}"
        }
    }

    fun formatVolume(volumeKg: Float): String {
        val useLbs = uiState.value.useLbs
        val converted = (if (useLbs) volumeKg * 2.20462f else volumeKg).roundToInt()
        return "$converted ${uiState.value.unitLabel}"
    }

    fun toDisplayWeightValue(weightKg: Float): Float {
        val converted = if (uiState.value.useLbs) weightKg * 2.20462f else weightKg
        return (converted * 10f).roundToInt() / 10f
    }
}
