package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.VivoHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExerciseEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.local.WorkoutTemplateEntity
import com.example.data.repository.AnalyticsAndInsightsEngine
import com.example.data.repository.DraftSetEntry
import com.example.ui.components.AppleDropdownField
import com.example.ui.components.AppleLiquidGlassTextField
import com.example.ui.components.AppleMultiSelectExerciseDropdown
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.components.LiveWorkoutSetVolumeGraph
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.theme.AppleStandCyan
import com.example.ui.viewmodel.ActiveWorkoutEditorState
import com.example.ui.viewmodel.FitPulseUiState
import com.example.ui.viewmodel.FitnessViewModel
import java.util.Locale
import kotlin.math.roundToInt

enum class WorkoutTabSection(val label: String) {
    ACTIVE_LOGGER("Active Logger"),
    ROUTINES("Saved Plans"),
    HISTORY("History")
}

@Composable
fun WorkoutLoggerScreen(
    state: FitPulseUiState,
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(WorkoutTabSection.ACTIVE_LOGGER) }
    var showAddExerciseModal by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    var isCreatingNewRoutine by remember { mutableStateOf(false) }
    var collapsedPlannedExercises by rememberSaveable { mutableStateOf(listOf<String>()) }
    var collapsedLiveExercises by rememberSaveable { mutableStateOf(listOf<String>()) }
    val context = LocalContext.current

    val activeWorkout = state.activeWorkout
    val groupedExercises = remember(activeWorkout.sets) {
        activeWorkout.sets
            .groupBy { it.exerciseName }
            .entries
            .sortedBy { it.value.firstOrNull()?.exerciseOrder ?: 0 }
            .map { it.key to it.value.sortedBy { s -> s.setNumber } }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("workout_logger_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "WORKOUT PLANNER, LIVE TIMER & SET LOGGER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleStandBlue
                        )
                        Text(
                            text = "Workouts",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    AppleSegmentedControl(
                        items = WorkoutTabSection.entries,
                        selectedItem = selectedSubTab,
                        onItemSelected = { selectedSubTab = it },
                        labelProvider = { tab ->
                            when (tab) {
                                WorkoutTabSection.ACTIVE_LOGGER -> if (activeWorkout.isWorkoutActive) "Live Logger" else "Plan & Logger"
                                WorkoutTabSection.ROUTINES -> "Saved Plans (${state.routines.size})"
                                WorkoutTabSection.HISTORY -> "History (${state.completedSessions.size})"
                            }
                        },
                        accentColor = AppleStandBlue
                    )
                }
            }

            // Live Rest Countdown Banner (Visible whenever Rest Timer is running)
            if (activeWorkout.isRestTimerRunning) {
                item {
                    RestTimerBanner(
                        secondsRemaining = activeWorkout.restTimerRemainingSeconds,
                        totalSeconds = activeWorkout.restTimerTotalSeconds,
                        currentDefaultRestSeconds = state.profile.defaultRestSeconds,
                        onAddSeconds = { addSec ->
                            val next = (activeWorkout.restTimerRemainingSeconds + addSec).coerceAtLeast(10)
                            viewModel.startRestTimer(next)
                        },
                        onChangeRestDuration = { newDuration ->
                            viewModel.setDefaultRestSeconds(newDuration)
                            viewModel.startRestTimer(newDuration)
                        },
                        onStopTimer = { viewModel.stopRestTimer() }
                    )
                }
            }

            when (selectedSubTab) {
                WorkoutTabSection.ACTIVE_LOGGER -> {
                    if (!activeWorkout.isWorkoutActive) {
                        // STAGE 1: BLANK WORKOUT PLAN BUILDER
                        // User starts with a blank canvas to build their workout plan, save it for next time, or start the live workout!
                        item {
                            BlankWorkoutPlanBuilderCard(
                                editor = activeWorkout,
                                savedPlans = state.routines,
                                plannedExerciseCount = groupedExercises.size,
                                totalPlannedSets = activeWorkout.sets.size,
                                onUpdateTitle = { viewModel.updateActiveWorkoutMeta(title = it) },
                                onUpdateCategory = { viewModel.updateActiveWorkoutMeta(category = it) },
                                onUpdateDate = { viewModel.updateActiveWorkoutMeta(dateKey = it) },
                                onUpdateNotes = { viewModel.updateActiveWorkoutMeta(notes = it) },
                                onLoadSavedPlan = { routine ->
                                    viewModel.startWorkoutFromRoutine(routine, startImmediately = false)
                                },
                                onClearPlan = { viewModel.startBlankWorkout() },
                                onAddExerciseClick = { showAddExerciseModal = true }
                            )
                        }

                        if (groupedExercises.isNotEmpty()) {
                            item {
                                AppleSectionHeader(
                                    title = "Exercises in Your Plan (${groupedExercises.size})",
                                    subtitle = "Save this plan for next time or tap Start Workout below when ready",
                                    actionLabel = "Add Exercise",
                                    actionIcon = Icons.Default.Add,
                                    actionColor = AppleStandBlue,
                                    onActionClick = { showAddExerciseModal = true }
                                )
                            }

                            items(groupedExercises, key = { "planned_${it.first}" }) { (exerciseName, exerciseSets) ->
                                val idx = groupedExercises.indexOfFirst { it.first == exerciseName }
                                val isExpanded = !collapsedPlannedExercises.contains(exerciseName)
                                PlannedExerciseBuilderCard(
                                    exerciseName = exerciseName,
                                    sets = exerciseSets,
                                    unitLabel = state.unitLabel,
                                    useLbs = state.useLbs,
                                    canMoveUp = idx > 0,
                                    canMoveDown = idx < groupedExercises.lastIndex,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        val willExpand = !isExpanded
                                        VivoHapticFeedback.performAccordionToggle(context, willExpand)
                                        collapsedPlannedExercises = if (isExpanded) {
                                            collapsedPlannedExercises + exerciseName
                                        } else {
                                            collapsedPlannedExercises - exerciseName
                                        }
                                    },
                                    onMoveUp = { viewModel.moveExerciseOrderInActiveWorkout(exerciseName, moveUp = true) },
                                    onMoveDown = { viewModel.moveExerciseOrderInActiveWorkout(exerciseName, moveUp = false) },
                                    onRemoveExercise = { viewModel.removeExerciseFromActiveWorkout(exerciseName) },
                                    onAddSet = { viewModel.addSingleSetToExerciseInActiveWorkout(exerciseName) },
                                    onUpdateSet = { setId, w, r ->
                                        viewModel.updateDraftSet(localId = setId, weightDisplay = w, reps = r)
                                    },
                                    onRemoveSet = { setId -> viewModel.removeDraftSet(setId) }
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LiquidGlassSecondaryButton(
                                        text = "Save for Next Time",
                                        onClick = { viewModel.saveCurrentPlanForNextTime() },
                                        icon = Icons.Default.BookmarkAdd,
                                        accentColor = AppleMindPurple,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        testTag = "save_plan_for_next_time_button"
                                    )
                                    LiquidGlassPrimaryButton(
                                        text = "Start Workout",
                                        onClick = { viewModel.startPlannedWorkout() },
                                        icon = Icons.Default.PlayArrow,
                                        accentColor = AppleExerciseGreen,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        testTag = "start_planned_workout_button"
                                    )
                                }
                            }
                        }
                    } else {
                        // STAGE 2: LIVE WORKOUT EXECUTION
                        // Clicking any exercise in the plan registers it as the active exercise and starts the live timer!
                        item {
                            LiveWorkoutTelemetryHeaderCard(
                                editor = activeWorkout,
                                unitLabel = state.unitLabel,
                                useLbs = state.useLbs,
                                onToggleTimer = { viewModel.toggleWorkoutTimer() },
                                onStartRestTimer = { secs -> viewModel.startRestTimer(secs) },
                                onAddExerciseClick = { showAddExerciseModal = true },
                                onEditPlanMode = {
                                    // Allows adding/reordering exercises or saving as routine
                                    viewModel.saveCurrentPlanForNextTime()
                                },
                                onCompleteWorkout = { viewModel.saveActiveWorkout(asDraft = false) },
                                onDiscardWorkout = { viewModel.discardActiveWorkout() }
                            )
                        }

                        // Real-Time Set Volume Graph inside Active Workout
                        if (activeWorkout.sets.any { it.status == "COMPLETED" }) {
                            item {
                                LiquidGlassCard(accentTint = AppleExerciseGreen) {
                                    LiveWorkoutSetVolumeGraph(
                                        sets = activeWorkout.sets,
                                        useLbs = state.useLbs,
                                        unitLabel = state.unitLabel
                                    )
                                }
                            }
                        }

                        item {
                            AppleSectionHeader(
                                title = "Tap an Exercise to Register & Start Timer",
                                subtitle = if (activeWorkout.activeExerciseName != null) {
                                    "Currently performing: ${activeWorkout.activeExerciseName}"
                                } else {
                                    "Select any exercise from your plan below to start its timer"
                                },
                                actionLabel = "Add Exercise",
                                actionIcon = Icons.Default.Add,
                                actionColor = AppleStandBlue,
                                onActionClick = { showAddExerciseModal = true }
                            )
                        }

                        items(groupedExercises, key = { "live_${it.first}" }) { (exerciseName, exerciseSets) ->
                            val isExerciseActive = activeWorkout.activeExerciseName.equals(exerciseName, ignoreCase = true)
                            val exerciseSeconds = if (isExerciseActive) {
                                activeWorkout.activeExerciseElapsedSeconds
                            } else {
                                activeWorkout.exerciseElapsedSecondsMap[exerciseName] ?: 0
                            }
                            val idx = groupedExercises.indexOfFirst { it.first == exerciseName }
                            val isExpanded = !collapsedLiveExercises.contains(exerciseName)

                            LiveInteractiveExerciseCard(
                                exerciseName = exerciseName,
                                sets = exerciseSets,
                                isExerciseActive = isExerciseActive,
                                isTimerRunning = activeWorkout.isTimerRunning && isExerciseActive,
                                exerciseElapsedSeconds = exerciseSeconds,
                                unitLabel = state.unitLabel,
                                useLbs = state.useLbs,
                                defaultRestSeconds = state.profile.defaultRestSeconds,
                                canMoveUp = idx > 0,
                                canMoveDown = idx < groupedExercises.lastIndex,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    val willExpand = !isExpanded
                                    VivoHapticFeedback.performAccordionToggle(context, willExpand)
                                    collapsedLiveExercises = if (isExpanded) {
                                        collapsedLiveExercises + exerciseName
                                    } else {
                                        collapsedLiveExercises - exerciseName
                                    }
                                },
                                onClickExerciseToStart = {
                                    if (!isExpanded) {
                                        collapsedLiveExercises = collapsedLiveExercises - exerciseName
                                    }
                                    viewModel.selectAndStartExerciseInWorkout(exerciseName)
                                },
                                onToggleExerciseTimer = {
                                    if (!isExpanded) {
                                        collapsedLiveExercises = collapsedLiveExercises - exerciseName
                                    }
                                    if (!isExerciseActive) {
                                        viewModel.selectAndStartExerciseInWorkout(exerciseName)
                                    } else {
                                        viewModel.toggleWorkoutTimer()
                                    }
                                },
                                onStartRestTimer = { secs -> viewModel.startRestTimer(secs) },
                                onMoveUp = { viewModel.moveExerciseOrderInActiveWorkout(exerciseName, moveUp = true) },
                                onMoveDown = { viewModel.moveExerciseOrderInActiveWorkout(exerciseName, moveUp = false) },
                                onRemoveExercise = { viewModel.removeExerciseFromActiveWorkout(exerciseName) },
                                onAddSet = { viewModel.addSingleSetToExerciseInActiveWorkout(exerciseName) },
                                onUpdateSet = { setId, w, r, notes ->
                                    viewModel.updateDraftSet(
                                        localId = setId,
                                        weightDisplay = w,
                                        reps = r,
                                        notes = notes
                                    )
                                },
                                onRegisterSet = { setId ->
                                    viewModel.registerSetCompleted(localId = setId, triggerRestTimer = true)
                                },
                                onRemoveSet = { setId -> viewModel.removeDraftSet(setId) }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LiquidGlassSecondaryButton(
                                    text = "Save Plan for Next Time",
                                    onClick = { viewModel.saveCurrentPlanForNextTime() },
                                    icon = Icons.Default.BookmarkAdd,
                                    accentColor = AppleMindPurple,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                LiquidGlassPrimaryButton(
                                    text = "Finish & Save Workout",
                                    onClick = { viewModel.saveActiveWorkout(asDraft = false) },
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = AppleExerciseGreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    testTag = "finish_and_save_workout_button"
                                )
                            }
                        }
                    }
                }

                WorkoutTabSection.ROUTINES -> {
                    // Fixes Image 2: Single-line properly aligned "Create Routine" button with weighted title
                    item {
                        AppleSectionHeader(
                            title = "Saved Workout Plans",
                            subtitle = "Reusable plans you saved for future sessions",
                            actionLabel = "Create Plan",
                            actionIcon = Icons.Default.Add,
                            actionColor = AppleStandBlue,
                            actionTestTag = "create_routine_button",
                            onActionClick = {
                                isCreatingNewRoutine = true
                                editingRoutine = null
                            }
                        )
                    }

                    if (state.routines.isEmpty()) {
                        item {
                            LiquidGlassCard(accentTint = AppleStandBlue) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "No saved workout plans yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Build a plan in the 'Plan & Logger' tab and tap 'Save for Next Time', or tap 'Create Plan' above to create a reusable template.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.routines, key = { it.id }) { routine ->
                            RoutineItemCard(
                                routine = routine,
                                onLoadIntoBuilder = {
                                    viewModel.startWorkoutFromRoutine(routine, startImmediately = false)
                                    selectedSubTab = WorkoutTabSection.ACTIVE_LOGGER
                                },
                                onStartLive = {
                                    viewModel.startWorkoutFromRoutine(routine, startImmediately = true)
                                    selectedSubTab = WorkoutTabSection.ACTIVE_LOGGER
                                },
                                onEdit = {
                                    isCreatingNewRoutine = false
                                    editingRoutine = routine
                                },
                                onDelete = { viewModel.deleteWorkoutRoutine(routine.id) }
                            )
                        }
                    }
                }

                WorkoutTabSection.HISTORY -> {
                    item {
                        AppleSectionHeader(
                            title = "Completed Workout History",
                            subtitle = "${state.completedSessions.size} sessions stored on device"
                        )
                    }

                    if (state.completedSessions.isEmpty()) {
                        item {
                            LiquidGlassCard(accentTint = AppleMindPurple) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "No completed workouts yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Create a plan in the Active Logger, tap an exercise to start your timer, register your sets, and finish your workout.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.completedSessions, key = { it.id }) { workout ->
                            val sessionSets = remember(state.allSets, workout.id) {
                                state.allSets.filter { it.sessionId == workout.id }
                            }
                            HistoricalWorkoutCard(
                                workout = workout,
                                sets = sessionSets,
                                formatWeight = { viewModel.formatWeight(it) },
                                formatVolume = { viewModel.formatVolume(it) },
                                onSelectSession = { viewModel.selectHistoricalSession(workout.id) },
                                onDuplicateSession = {
                                    viewModel.loadSessionIntoEditor(workout, duplicateAsNewToday = true)
                                    selectedSubTab = WorkoutTabSection.ACTIVE_LOGGER
                                },
                                onDeleteSession = { viewModel.deleteWorkoutSession(workout.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.selectedHistoricalSession != null) {
        val selectedSession = state.selectedHistoricalSession!!
        HistoricalWorkoutDetailDialog(
            session = selectedSession,
            sets = state.selectedHistoricalSessionSets,
            useLbs = state.useLbs,
            unitLabel = state.unitLabel,
            formatWeight = { viewModel.formatWeight(it) },
            formatVolume = { viewModel.formatVolume(it) },
            onDismiss = { viewModel.selectHistoricalSession(null) },
            onRepeatAsNewToday = {
                viewModel.loadSessionIntoEditor(selectedSession, duplicateAsNewToday = true)
                viewModel.selectHistoricalSession(null)
                selectedSubTab = WorkoutTabSection.ACTIVE_LOGGER
            }
        )
    }

    if (showAddExerciseModal) {
        AddExerciseToWorkoutDialog(
            exercises = state.allExercises.filter { !it.isArchived },
            unitLabel = state.unitLabel,
            onDismiss = { showAddExerciseModal = false },
            onSelectExercise = { name, muscle, targetWeight, targetReps, targetSets ->
                viewModel.addExerciseToActiveWorkout(
                    exerciseName = name,
                    muscleGroup = muscle,
                    initialWeightDisplay = targetWeight,
                    initialReps = targetReps,
                    initialSetsCount = targetSets
                )
                showAddExerciseModal = false
            }
        )
    }

    if (isCreatingNewRoutine || editingRoutine != null) {
        RoutineEditorDialog(
            existingRoutine = editingRoutine,
            availableExercises = state.allExercises.filter { !it.isArchived },
            onDismiss = {
                isCreatingNewRoutine = false
                editingRoutine = null
            },
            onSave = { name, category, targetDurationMin, exerciseList ->
                viewModel.saveWorkoutRoutine(
                    id = editingRoutine?.id,
                    name = name,
                    category = category,
                    targetDurationMin = targetDurationMin,
                    exercisesList = exerciseList
                )
                isCreatingNewRoutine = false
                editingRoutine = null
            }
        )
    }
}

@Composable
private fun BlankWorkoutPlanBuilderCard(
    editor: ActiveWorkoutEditorState,
    savedPlans: List<WorkoutTemplateEntity>,
    plannedExerciseCount: Int,
    totalPlannedSets: Int,
    onUpdateTitle: (String) -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateDate: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onLoadSavedPlan: (WorkoutTemplateEntity) -> Unit,
    onClearPlan: () -> Unit,
    onAddExerciseClick: () -> Unit
) {
    val dateOptions = remember {
        (0..7).map { daysAgo ->
            val key = AnalyticsAndInsightsEngine.dateKeyForDaysAgo(daysAgo)
            val readable = AnalyticsAndInsightsEngine.formatDateKeyReadable(key)
            key to "$readable ($key)"
        }
    }
    val currentSelectedDateLabel = dateOptions.firstOrNull { it.first == editor.dateKey }?.second
        ?: "${AnalyticsAndInsightsEngine.formatDateKeyReadable(editor.dateKey)} (${editor.dateKey})"

    LiquidGlassCard(accentTint = AppleStandBlue) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STEP 1 • CREATE OR LOAD WORKOUT PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppleStandBlue
                    )
                    Text(
                        text = if (plannedExerciseCount == 0) {
                            "Blank Workout Plan Builder"
                        } else {
                            "Plan Ready ($plannedExerciseCount Exercises • $totalPlannedSets Sets)"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (plannedExerciseCount > 0 || editor.title.isNotBlank()) {
                    TextButton(onClick = onClearPlan) {
                        Text("Reset Blank", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "1. Build your plan below (or load a saved plan).\n2. Save it for next time or tap 'Start Workout'.\n3. During the workout, tap any exercise in your plan to register it and start the live timer & rest timer!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (savedPlans.isNotEmpty()) {
                AppleDropdownField(
                    label = "Load a Saved Plan (Optional)",
                    selectedValue = editor.title.ifBlank { "Select a saved plan to load..." },
                    options = savedPlans.map { it.name },
                    onOptionSelected = { selectedName ->
                        val matched = savedPlans.firstOrNull { it.name == selectedName }
                        if (matched != null) onLoadSavedPlan(matched)
                    },
                    subtitleProvider = { name ->
                        val r = savedPlans.firstOrNull { it.name == name }
                        r?.exercisesCsv?.replace(",", " • ")
                    }
                )
            }

            AppleLiquidGlassTextField(
                value = editor.title,
                onValueChange = onUpdateTitle,
                label = "Workout Plan Name",
                placeholder = "e.g., Monday Chest & Triceps Plan",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                fieldModifier = Modifier.testTag("active_workout_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppleDropdownField(
                    label = "Training Split",
                    selectedValue = editor.category,
                    options = listOf("Strength", "Hypertrophy", "Push", "Pull", "Legs", "Upper Body", "Full Body"),
                    onOptionSelected = onUpdateCategory,
                    modifier = Modifier.weight(1f)
                )

                AppleDropdownField(
                    label = "Workout Date",
                    selectedValue = currentSelectedDateLabel,
                    options = dateOptions.map { it.second },
                    onOptionSelected = { label ->
                        val key = dateOptions.firstOrNull { it.second == label }?.first
                        if (key != null) onUpdateDate(key)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            AppleLiquidGlassTextField(
                value = editor.notes,
                onValueChange = onUpdateNotes,
                label = "Plan Notes / Goals (optional)",
                placeholder = "e.g., Focus on slow eccentric, 90s rest between sets",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LiquidGlassPrimaryButton(
                text = "Add Exercise to Plan",
                onClick = onAddExerciseClick,
                icon = Icons.Default.Add,
                accentColor = AppleStandBlue,
                modifier = Modifier.fillMaxWidth(),
                testTag = "add_exercise_to_workout_button"
            )
        }
    }
}

@Composable
private fun PlannedExerciseBuilderCard(
    exerciseName: String,
    sets: List<DraftSetEntry>,
    unitLabel: String,
    useLbs: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemoveExercise: () -> Unit,
    onAddSet: () -> Unit,
    onUpdateSet: (Long, Float?, Int?) -> Unit,
    onRemoveSet: (Long) -> Unit
) {
    val factor = if (useLbs) 2.20462f else 1f
    val muscleGroup = sets.firstOrNull()?.muscleGroup ?: "Full Body"

    LiquidGlassCard(
        accentTint = AppleStandCyan,
        onClick = onToggleExpand
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleStandBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$muscleGroup • ${sets.size} planned sets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemoveExercise) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove Exercise",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Bouncy Expand / Shrink (^) (v) Chevron Button
                    Surface(
                        onClick = onToggleExpand,
                        shape = CircleShape,
                        color = if (isExpanded) {
                            AppleStandBlue.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isExpanded) AppleStandBlue.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("plan_exercise_expand_toggle_${exerciseName}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Shrink Exercise Card" else "Expand Exercise Card",
                                tint = AppleStandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    sets.forEach { setEntry ->
                        val displayW = ((setEntry.weightKg * factor * 10f).roundToInt() / 10f)
                        var weightText by remember(setEntry.localId, displayW) {
                            mutableStateOf(if (displayW % 1f == 0f) displayW.toInt().toString() else displayW.toString())
                        }
                        var repsText by remember(setEntry.localId, setEntry.reps) {
                            mutableStateOf(setEntry.reps.toString())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AppleStandBlue.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Set #${setEntry.setNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleStandBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
                                )
                            }

                            AppleLiquidGlassTextField(
                                value = weightText,
                                onValueChange = {
                                    weightText = it
                                    onUpdateSet(setEntry.localId, it.toFloatOrNull(), null)
                                },
                                label = "Target $unitLabel",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            AppleLiquidGlassTextField(
                                value = repsText,
                                onValueChange = {
                                    repsText = it
                                    onUpdateSet(setEntry.localId, null, it.toIntOrNull())
                                },
                                label = "Target Reps",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = { onRemoveSet(setEntry.localId) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Set",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    LiquidGlassSecondaryButton(
                        text = "Add Planned Set",
                        onClick = onAddSet,
                        icon = Icons.Default.Add,
                        accentColor = AppleStandBlue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveWorkoutTelemetryHeaderCard(
    editor: ActiveWorkoutEditorState,
    unitLabel: String,
    useLbs: Boolean,
    onToggleTimer: () -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onAddExerciseClick: () -> Unit,
    onEditPlanMode: () -> Unit,
    onCompleteWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit
) {
    val factor = if (useLbs) 2.20462f else 1f
    val completedSets = editor.sets.count { it.status == "COMPLETED" }
    val totalSets = editor.sets.size
    val liveVolumeDisplay = editor.sets
        .filter { it.status == "COMPLETED" }
        .sumOf { (it.weightKg * factor * it.reps).roundToInt() }

    val sessionMins = editor.elapsedSeconds / 60
    val sessionSecs = editor.elapsedSeconds % 60
    val sessionTimerFormatted = String.format(Locale.US, "%02d:%02d", sessionMins, sessionSecs)

    val exMins = editor.activeExerciseElapsedSeconds / 60
    val exSecs = editor.activeExerciseElapsedSeconds % 60
    val exTimerFormatted = String.format(Locale.US, "%02d:%02d", exMins, exSecs)

    LiquidGlassCard(accentTint = AppleExerciseGreen) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (editor.isTimerRunning) AppleExerciseGreen else AppleOverloadOrange)
                        )
                        Text(
                            text = if (editor.isTimerRunning) {
                                "LIVE WORKOUT TIMER RUNNING"
                            } else {
                                "WORKOUT READY • TAP AN EXERCISE TO START TIMER"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (editor.isTimerRunning) AppleExerciseGreen else AppleOverloadOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = editor.title.ifBlank { "Live Strength Session" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onDiscardWorkout) {
                    Text("End / Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            // Equal-Height 3-Tile Live Telemetry Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualHeightMetricMiniBox(
                    label = if (editor.activeExerciseName != null) "Exercise Timer" else "Session Timer",
                    value = if (editor.activeExerciseName != null) exTimerFormatted else sessionTimerFormatted,
                    accent = AppleExerciseGreen,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EqualHeightMetricMiniBox(
                    label = "Sets Registered",
                    value = "$completedSets / $totalSets",
                    accent = AppleStandBlue,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EqualHeightMetricMiniBox(
                    label = "Live Volume",
                    value = "$liveVolumeDisplay $unitLabel",
                    accent = AppleMoveRed,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Active Exercise & Timer Controls Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, AppleStandBlue.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = if (editor.activeExerciseName != null) {
                                "Active Exercise: ${editor.activeExerciseName}"
                            } else {
                                "No Exercise Selected Yet"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (editor.activeExerciseName != null) AppleExerciseGreen else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Total Session Time: $sessionTimerFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LiquidGlassPrimaryButton(
                        text = if (editor.isTimerRunning) "Pause Timer" else "Start Timer",
                        onClick = onToggleTimer,
                        icon = if (editor.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        accentColor = if (editor.isTimerRunning) AppleOverloadOrange else AppleExerciseGreen
                    )
                }
            }

            // Quick Rest Timer Trigger Buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "START REST TIMER BETWEEN SETS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60, 90, 120, 180).forEach { secs ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onStartRestTimer(secs) },
                            shape = RoundedCornerShape(12.dp),
                            color = AppleStandBlue.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AppleStandBlue.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Rest ${secs}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleStandBlue,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveInteractiveExerciseCard(
    exerciseName: String,
    sets: List<DraftSetEntry>,
    isExerciseActive: Boolean,
    isTimerRunning: Boolean,
    exerciseElapsedSeconds: Int,
    unitLabel: String,
    useLbs: Boolean,
    defaultRestSeconds: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClickExerciseToStart: () -> Unit,
    onToggleExerciseTimer: () -> Unit,
    onStartRestTimer: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemoveExercise: () -> Unit,
    onAddSet: () -> Unit,
    onUpdateSet: (Long, Float?, Int?, String?) -> Unit,
    onRegisterSet: (Long) -> Unit,
    onRemoveSet: (Long) -> Unit
) {
    val factor = if (useLbs) 2.20462f else 1f
    val completedCount = sets.count { it.status == "COMPLETED" }
    val muscleGroup = sets.firstOrNull()?.muscleGroup ?: "Full Body"
    val mins = exerciseElapsedSeconds / 60
    val secs = exerciseElapsedSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val context = LocalContext.current

    LiquidGlassCard(
        accentTint = if (isExerciseActive) AppleExerciseGreen else AppleStandBlue,
        onClick = {
            if (!isExpanded) {
                onToggleExpand()
            }
            onClickExerciseToStart()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isExerciseActive) {
                                AppleExerciseGreen.copy(alpha = 0.20f)
                            } else {
                                AppleStandBlue.copy(alpha = 0.14f)
                            }
                        ) {
                            Text(
                                text = if (isExerciseActive) {
                                    "ACTIVE • $timerText"
                                } else if (completedCount == sets.size && sets.isNotEmpty()) {
                                    "COMPLETED ✓"
                                } else {
                                    "TAP TO START EXERCISE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExerciseActive) AppleExerciseGreen else AppleStandBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isExerciseActive) AppleExerciseGreen else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$muscleGroup • $completedCount of ${sets.size} sets registered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPrimaryButton(
                        text = when {
                            isExerciseActive && isTimerRunning -> "Timer $timerText"
                            isExerciseActive -> "Resume $timerText"
                            else -> "Start Exercise"
                        },
                        onClick = {
                            if (!isExpanded) onToggleExpand()
                            VivoHapticFeedback.performLaunch(context)
                            onToggleExerciseTimer()
                        },
                        icon = if (isExerciseActive && isTimerRunning) Icons.Default.Timer else Icons.Default.PlayArrow,
                        accentColor = if (isExerciseActive) AppleExerciseGreen else AppleStandBlue
                    )

                    // Bouncy Expand / Shrink (^) (v) Chevron Button
                    Surface(
                        onClick = onToggleExpand,
                        shape = CircleShape,
                        color = if (isExerciseActive) {
                            AppleExerciseGreen.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isExerciseActive) AppleExerciseGreen.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("exercise_expand_toggle_${exerciseName}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Shrink Exercise Card" else "Expand Exercise Card",
                                tint = if (isExerciseActive) AppleExerciseGreen else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            sets.forEach { setEntry ->
                val isCompleted = setEntry.status == "COMPLETED"
                val displayW = ((setEntry.weightKg * factor * 10f).roundToInt() / 10f)
                var weightText by remember(setEntry.localId, displayW) {
                    mutableStateOf(if (displayW % 1f == 0f) displayW.toInt().toString() else displayW.toString())
                }
                var repsText by remember(setEntry.localId, setEntry.reps) {
                    mutableStateOf(setEntry.reps.toString())
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCompleted) {
                        AppleExerciseGreen.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) AppleExerciseGreen.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) AppleExerciseGreen else AppleStandBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${setEntry.setNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            AppleLiquidGlassTextField(
                                value = weightText,
                                onValueChange = {
                                    weightText = it
                                    onUpdateSet(setEntry.localId, it.toFloatOrNull(), null, null)
                                },
                                label = unitLabel,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            AppleLiquidGlassTextField(
                                value = repsText,
                                onValueChange = {
                                    repsText = it
                                    onUpdateSet(setEntry.localId, null, it.toIntOrNull(), null)
                                },
                                label = "Reps",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f)
                            )

                            LiquidGlassPrimaryButton(
                                text = if (isCompleted) "Done" else "Register",
                                onClick = { onRegisterSet(setEntry.localId) },
                                icon = if (isCompleted) Icons.Default.Check else null,
                                accentColor = if (isCompleted) AppleExerciseGreen else AppleStandBlue,
                                modifier = Modifier.weight(1.1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCompleted) {
                                    "Set registered • Rest timer started (${defaultRestSeconds}s)"
                                } else {
                                    "Tap 'Register' when you complete this set to log volume & start rest"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted) AppleExerciseGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(onClick = { onStartRestTimer(defaultRestSeconds) }) {
                                    Text(
                                        text = "Rest ${defaultRestSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleStandBlue
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveSet(setEntry.localId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Set",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassSecondaryButton(
                    text = "Add Set",
                    onClick = onAddSet,
                    icon = Icons.Default.Add,
                    accentColor = AppleStandBlue,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                LiquidGlassSecondaryButton(
                    text = "Start Rest (${defaultRestSeconds}s)",
                    onClick = { onStartRestTimer(defaultRestSeconds) },
                    icon = Icons.Default.Timer,
                    accentColor = AppleOverloadOrange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}
}
}

@Composable
private fun RestTimerBanner(
    secondsRemaining: Int,
    totalSeconds: Int,
    currentDefaultRestSeconds: Int,
    onAddSeconds: (Int) -> Unit,
    onChangeRestDuration: (Int) -> Unit,
    onStopTimer: () -> Unit
) {
    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val formatted = String.format(Locale.US, "%d:%02d", mins, secs)
    val progress = (secondsRemaining.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)

    LiquidGlassCard(accentTint = AppleOverloadOrange) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppleOverloadOrange.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AppleOverloadOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "REST TIMER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleOverloadOrange
                        )
                        Text(
                            text = "$formatted remaining",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassSecondaryButton(
                        text = "+15s",
                        onClick = { onAddSeconds(15) },
                        accentColor = AppleOverloadOrange
                    )
                    LiquidGlassPrimaryButton(
                        text = "Skip",
                        onClick = onStopTimer,
                        accentColor = AppleMoveRed
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = AppleOverloadOrange,
                trackColor = AppleOverloadOrange.copy(alpha = 0.16f)
            )

            // Preset Rest Duration Selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rest Duration:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                val presetOptions = listOf(30, 60, 90, 120, 180)
                val activeSelectedSeconds = when {
                    presetOptions.contains(totalSeconds) -> totalSeconds
                    presetOptions.contains(currentDefaultRestSeconds) -> currentDefaultRestSeconds
                    else -> 90
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetOptions.forEach { sec ->
                        val isSelected = (sec == activeSelectedSeconds)
                        Surface(
                            onClick = { onChangeRestDuration(sec) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AppleOverloadOrange.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) AppleOverloadOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        ) {
                            Text(
                                text = "${sec}s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AppleOverloadOrange else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineItemCard(
    routine: WorkoutTemplateEntity,
    onLoadIntoBuilder: () -> Unit,
    onStartLive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val exercises = remember(routine.exercisesCsv) {
        routine.exercisesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    LiquidGlassCard(accentTint = AppleStandBlue) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${routine.category.uppercase(Locale.US)} • ~${routine.targetDurationMin} MIN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppleStandBlue
                    )
                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Plan",
                            tint = AppleStandBlue
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Plan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = if (exercises.isEmpty()) {
                    "No exercises assigned"
                } else {
                    exercises.joinToString(" • ")
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassSecondaryButton(
                    text = "Edit in Builder",
                    onClick = onLoadIntoBuilder,
                    icon = Icons.Default.Edit,
                    accentColor = AppleStandBlue,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                LiquidGlassPrimaryButton(
                    text = "Start Workout",
                    onClick = onStartLive,
                    icon = Icons.Default.PlayArrow,
                    accentColor = AppleExerciseGreen,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun HistoricalWorkoutCard(
    workout: WorkoutSessionEntity,
    sets: List<WorkoutSetEntity>,
    formatWeight: (Float) -> String,
    formatVolume: (Float) -> String,
    onSelectSession: () -> Unit,
    onDuplicateSession: () -> Unit,
    onDeleteSession: () -> Unit
) {
    val dateFormatted = remember(workout.dateKey) {
        AnalyticsAndInsightsEngine.formatDateKeyReadable(workout.dateKey)
    }
    val groupedByExercise = remember(sets) {
        sets.groupBy { it.exerciseName }
    }

    LiquidGlassCard(
        accentTint = AppleMindPurple,
        onClick = onSelectSession
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatted.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppleMindPurple
                    )
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${workout.durationMinutes} min • ${workout.totalSets} sets • ${formatVolume(workout.totalVolumeKg)} total volume",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onDuplicateSession) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Repeat Plan as Today's Workout",
                            tint = AppleExerciseGreen
                        )
                    }
                    IconButton(onClick = onDeleteSession) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Workout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (groupedByExercise.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                groupedByExercise.entries.forEach { (exerciseName, exSets) ->
                    val bestSet = exSets.maxByOrNull { it.weightKg }
                    val setsDescription = exSets.joinToString(", ") {
                        "${formatWeight(it.weightKg)} × ${it.reps}"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (bestSet != null) {
                                Text(
                                    text = "Top: ${formatWeight(bestSet.weightKg)} × ${bestSet.reps}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleExerciseGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = setsDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "Tap to view workout stats, graphs & full set log",
                style = MaterialTheme.typography.labelSmall,
                color = AppleMindPurple.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HistoricalWorkoutDetailDialog(
    session: WorkoutSessionEntity,
    sets: List<WorkoutSetEntity>,
    useLbs: Boolean,
    unitLabel: String,
    formatWeight: (Float) -> String,
    formatVolume: (Float) -> String,
    onDismiss: () -> Unit,
    onRepeatAsNewToday: () -> Unit
) {
    val dateReadable = remember(session.dateKey) {
        AnalyticsAndInsightsEngine.formatDateKeyReadable(session.dateKey)
    }
    val groupedSets = remember(sets) {
        sets.groupBy { it.exerciseName }
    }
    val totalReps = remember(sets) {
        sets.sumOf { it.reps }
    }
    val draftSetsForGraph = remember(sets) {
        sets.map {
            DraftSetEntry(
                localId = it.id,
                exerciseName = it.exerciseName,
                muscleGroup = it.muscleGroup,
                weightKg = it.weightKg,
                reps = it.reps,
                status = it.status,
                setNumber = it.setNumber
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${session.category.uppercase(Locale.US)} • $dateReadable",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppleMindPurple
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key Metrics
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqualHeightMetricMiniBox(
                            label = "Duration",
                            value = "${session.durationMinutes} min",
                            accent = AppleStandBlue,
                            modifier = Modifier.weight(1f)
                        )
                        EqualHeightMetricMiniBox(
                            label = "Total Volume",
                            value = formatVolume(session.totalVolumeKg),
                            accent = AppleExerciseGreen,
                            modifier = Modifier.weight(1f)
                        )
                        EqualHeightMetricMiniBox(
                            label = "Sets / Reps",
                            value = "${session.totalSets} / $totalReps",
                            accent = AppleOverloadOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Volume Graph
                if (draftSetsForGraph.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                LiveWorkoutSetVolumeGraph(
                                    sets = draftSetsForGraph,
                                    useLbs = useLbs,
                                    unitLabel = unitLabel
                                )
                            }
                        }
                    }
                }

                // Exercise-by-Exercise breakdown
                item {
                    Text(
                        text = "EXERCISE BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(groupedSets.entries.toList(), key = { it.key }) { (exerciseName, exSets) ->
                    val topSet = exSets.maxByOrNull { it.weightKg }
                    val exerciseVolume = exSets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exerciseName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatVolume(exerciseVolume),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleExerciseGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            exSets.sortedBy { it.setNumber }.forEach { s ->
                                val isTop = topSet != null && s.id == topSet.id
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Set #${s.setNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isTop) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AppleExerciseGreen.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "Top Set",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AppleExerciseGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${formatWeight(s.weightKg)} × ${s.reps} reps",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (session.notes.isNotBlank()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "NOTES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = session.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            LiquidGlassPrimaryButton(
                text = "Repeat as Today's Plan",
                onClick = onRepeatAsNewToday,
                icon = Icons.Default.PlayArrow,
                accentColor = AppleExerciseGreen
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AddExerciseToWorkoutDialog(
    exercises: List<ExerciseEntity>,
    unitLabel: String,
    onDismiss: () -> Unit,
    onSelectExercise: (String, String, Float, Int, Int) -> Unit
) {
    var selectedLibraryExerciseName by remember(exercises) {
        mutableStateOf(exercises.firstOrNull()?.name ?: "")
    }
    var useCustomExercise by remember { mutableStateOf(false) }
    var customExerciseName by remember { mutableStateOf("") }
    var customMuscleGroup by remember { mutableStateOf("Chest") }
    var targetWeightText by remember { mutableStateOf("40") }
    var targetRepsText by remember { mutableStateOf("8") }
    var targetSetsText by remember { mutableStateOf("3") }

    val muscleGroups = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Exercise to Workout Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppleSegmentedControl(
                    items = listOf(false, true),
                    selectedItem = useCustomExercise,
                    onItemSelected = { useCustomExercise = it },
                    labelProvider = { if (it) "Custom Exercise" else "From Library" },
                    accentColor = AppleStandBlue
                )

                if (!useCustomExercise && exercises.isNotEmpty()) {
                    AppleDropdownField(
                        label = "Select Exercise from Library",
                        selectedValue = selectedLibraryExerciseName,
                        options = exercises.map { it.name },
                        onOptionSelected = { selectedLibraryExerciseName = it },
                        subtitleProvider = { name ->
                            val ex = exercises.firstOrNull { it.name == name }
                            if (ex != null) "${ex.muscleGroup} • ${ex.equipment}" else null
                        },
                        testTag = "workout_library_exercise_dropdown"
                    )
                } else {
                    AppleLiquidGlassTextField(
                        value = customExerciseName,
                        onValueChange = { customExerciseName = it },
                        label = "Custom Exercise Name",
                        placeholder = "e.g., Incline Cable Fly",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AppleDropdownField(
                        label = "Primary Muscle Group",
                        selectedValue = customMuscleGroup,
                        options = muscleGroups,
                        onOptionSelected = { customMuscleGroup = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppleLiquidGlassTextField(
                        value = targetSetsText,
                        onValueChange = { targetSetsText = it },
                        label = "Sets",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    AppleLiquidGlassTextField(
                        value = targetRepsText,
                        onValueChange = { targetRepsText = it },
                        label = "Reps",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    AppleLiquidGlassTextField(
                        value = targetWeightText,
                        onValueChange = { targetWeightText = it },
                        label = unitLabel,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            LiquidGlassPrimaryButton(
                text = "Add to Plan",
                onClick = {
                    val weight = targetWeightText.toFloatOrNull() ?: 40f
                    val reps = targetRepsText.toIntOrNull() ?: 8
                    val sets = targetSetsText.toIntOrNull() ?: 3
                    if (!useCustomExercise && selectedLibraryExerciseName.isNotBlank()) {
                        val matched = exercises.firstOrNull { it.name == selectedLibraryExerciseName }
                        onSelectExercise(
                            selectedLibraryExerciseName,
                            matched?.muscleGroup ?: "Full Body",
                            weight,
                            reps,
                            sets
                        )
                    } else if (customExerciseName.trim().isNotEmpty()) {
                        onSelectExercise(
                            customExerciseName.trim(),
                            customMuscleGroup,
                            weight,
                            reps,
                            sets
                        )
                    }
                },
                accentColor = AppleStandBlue
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RoutineEditorDialog(
    existingRoutine: WorkoutTemplateEntity?,
    availableExercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(existingRoutine?.name ?: "") }
    var category by remember { mutableStateOf(existingRoutine?.category ?: "Strength") }
    var durationText by remember { mutableStateOf((existingRoutine?.targetDurationMin ?: 60).toString()) }
    val selectedExercises = remember {
        mutableStateListOf<String>().apply {
            existingRoutine?.exercisesCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.let { addAll(it) }
        }
    }

    val categoryOptions = listOf("Strength", "Hypertrophy", "Push / Pull / Legs", "Upper / Lower", "Full Body", "Conditioning")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingRoutine != null) "Edit Workout Plan" else "Create Saved Workout Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppleLiquidGlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Plan / Routine Name",
                    placeholder = "e.g., Upper Body Hypertrophy",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AppleDropdownField(
                    label = "Training Split / Category",
                    selectedValue = category,
                    options = categoryOptions,
                    onOptionSelected = { category = it },
                    testTag = "routine_category_dropdown"
                )

                AppleLiquidGlassTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = "Target Duration (minutes)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Fixes Image 1: Uses a proper multi-select Dropdown List instead of a clipped horizontal chip row
                AppleMultiSelectExerciseDropdown(
                    availableExercises = availableExercises,
                    selectedExerciseNames = selectedExercises,
                    onToggleExercise = { toggledName ->
                        if (selectedExercises.contains(toggledName)) {
                            selectedExercises.remove(toggledName)
                        } else {
                            selectedExercises.add(toggledName)
                        }
                    }
                )
            }
        },
        confirmButton = {
            LiquidGlassPrimaryButton(
                text = "Save Plan",
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onSave(
                            name.trim(),
                            category.trim(),
                            durationText.toIntOrNull() ?: 60,
                            selectedExercises.toList()
                        )
                    }
                },
                accentColor = AppleStandBlue,
                testTag = "save_routine_dialog_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
