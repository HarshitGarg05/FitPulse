package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExerciseEntity
import com.example.data.repository.ExerciseProgressionReport
import com.example.ui.components.AppleDropdownField
import com.example.ui.components.AppleLargePageHeader
import com.example.ui.components.AppleLiquidGlassTextField
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.ExerciseProgressionChart
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.ProgressionChartMetric
import com.example.ui.components.StrengthStandardGaugeBar
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.viewmodel.FitPulseUiState
import com.example.ui.viewmodel.FitnessViewModel
import java.util.Locale

@Composable
fun GoalsAndRecoveryScreen(
    state: FitPulseUiState,
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(ProgressionChartMetric.TOP_WEIGHT) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showCreateExerciseModal by remember { mutableStateOf(false) }

    val reports = state.dashboardAnalytics.overloadReports
    val selectedReport = state.selectedExerciseReport ?: reports.firstOrNull()

    val muscleGroups = remember(state.allExercises) {
        listOf("All") + state.allExercises.map { it.muscleGroup }.distinct().sorted()
    }
    val equipmentOptions = remember(state.allExercises) {
        listOf("All") + state.allExercises.map { it.equipment }.distinct().sorted()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("exercises_and_overload_screen"),
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
                AppleLargePageHeader(
                    overlineText = "EXERCISE LIBRARY & PROGRESSIVE OVERLOAD",
                    title = "Exercises",
                    accentColor = AppleOverloadOrange,
                    actionLabel = "New Exercise",
                    actionIcon = Icons.Default.Add,
                    actionTestTag = "new_custom_exercise_button",
                    onActionClick = {
                        editingExercise = null
                        showCreateExerciseModal = true
                    }
                )
            }

            // 1. Progressive Overload Inspector Card (Fixes Images 4 & 5 & Est. 1RM Equal Height)
            if (selectedReport != null) {
                item {
                    ProgressiveOverloadDetailCard(
                        allReports = reports,
                        report = selectedReport,
                        selectedMetric = selectedMetric,
                        unitLabel = state.unitLabel,
                        useLbs = state.useLbs,
                        userBodyweightKg = state.profile.weightKg,
                        formatWeight = { viewModel.formatWeight(it) },
                        formatVolume = { viewModel.formatVolume(it) },
                        toDisplayWeight = { viewModel.toDisplayWeightValue(it) },
                        onSelectExercise = { viewModel.selectExerciseForDetail(it) },
                        onSelectMetric = { selectedMetric = it }
                    )
                }
            }

            // 2. Search & Dropdown Filter Bar (Fixes Image 3: Uses Dropdowns instead of clipped chips)
            item {
                LiquidGlassCard(accentTint = AppleStandBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "EXERCISE LIBRARY (${state.filteredExercises.size} EXERCISES)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleStandBlue
                        )
                        AppleLiquidGlassTextField(
                            value = state.exerciseSearchQuery,
                            onValueChange = { viewModel.setExerciseSearchQuery(it) },
                            label = "Search Exercises",
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = "Search by exercise, muscle, or equipment",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            fieldModifier = Modifier.testTag("exercise_search_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleDropdownField(
                                label = "Muscle Group",
                                selectedValue = state.selectedMuscleFilter,
                                options = muscleGroups,
                                onOptionSelected = { viewModel.setMuscleFilter(it) },
                                modifier = Modifier.weight(1f),
                                testTag = "muscle_filter_dropdown"
                            )

                            AppleDropdownField(
                                label = "Equipment",
                                selectedValue = state.selectedEquipmentFilter,
                                options = equipmentOptions,
                                onOptionSelected = { viewModel.setEquipmentFilter(it) },
                                modifier = Modifier.weight(1f),
                                testTag = "equipment_filter_dropdown"
                            )
                        }
                    }
                }
            }

            // 3. Exercise Catalog List with Individual Performance Summary
            items(state.filteredExercises, key = { it.id }) { exercise ->
                val report = reports.firstOrNull {
                    it.exerciseName.equals(exercise.name, ignoreCase = true)
                }
                val isSelected = selectedReport?.exerciseName.equals(exercise.name, ignoreCase = true)

                ExerciseLibraryCard(
                    exercise = exercise,
                    report = report,
                    isSelectedForAnalytics = isSelected,
                    formatWeight = { viewModel.formatWeight(it) },
                    onSelectForAnalytics = { viewModel.selectExerciseForDetail(exercise.name) },
                    onQuickAddToWorkout = {
                        val recW = report?.recommendedStartWeightKg ?: 40f
                        val recReps = report?.recommendedTargetReps ?: 8
                        val recSets = report?.recommendedTargetSets ?: 3
                        viewModel.addExerciseToActiveWorkout(
                            exerciseName = exercise.name,
                            muscleGroup = exercise.muscleGroup,
                            initialWeightDisplay = viewModel.toDisplayWeightValue(recW),
                            initialReps = recReps,
                            initialSetsCount = recSets
                        )
                    },
                    onEdit = {
                        editingExercise = exercise
                        showCreateExerciseModal = true
                    },
                    onDelete = { viewModel.deleteExercisePreservingHistory(exercise.id) }
                )
            }
        }
    }

    if (showCreateExerciseModal) {
        ExerciseEditorDialog(
            existing = editingExercise,
            onDismiss = {
                showCreateExerciseModal = false
                editingExercise = null
            },
            onSave = { name, muscle, equip, notes ->
                viewModel.saveCustomExercise(
                    id = editingExercise?.id,
                    name = name,
                    muscleGroup = muscle,
                    secondaryMuscles = editingExercise?.secondaryMuscles ?: "",
                    equipment = equip,
                    instructions = notes
                )
                showCreateExerciseModal = false
                editingExercise = null
            }
        )
    }
}

@Composable
private fun ProgressiveOverloadDetailCard(
    allReports: List<ExerciseProgressionReport>,
    report: ExerciseProgressionReport,
    selectedMetric: ProgressionChartMetric,
    unitLabel: String,
    useLbs: Boolean,
    userBodyweightKg: Float,
    formatWeight: (Float) -> String,
    formatVolume: (Float) -> String,
    toDisplayWeight: (Float) -> Float,
    onSelectExercise: (String) -> Unit,
    onSelectMetric: (ProgressionChartMetric) -> Unit
) {
    val hasLoggedSessions = report.totalSessionsLogged > 0
    val displayWeightKg = report.personalRecordWeightKg
    val displayReps = report.personalRecordReps
    val displayVolumeKg = report.personalRecordVolumeKg
    val display1RmKg = report.personalRecordOneRepMaxKg

    LiquidGlassCard(accentTint = AppleOverloadOrange) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = AppleOverloadOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PROGRESSIVE OVERLOAD TRACKER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleOverloadOrange
                        )
                    }
                    Text(
                        text = report.exerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${report.muscleGroup} • ${report.equipment} • ${report.totalSessionsLogged} ${if (report.totalSessionsLogged == 1) "session" else "sessions"} logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OverloadBadge(
                    status = report.overloadStatus,
                    dynamicLabel = report.dynamicStatusBadge
                )
            }

            // Exercise selector dropdown right inside the tracker card
            AppleDropdownField(
                label = "Switch Exercise to Analyze",
                selectedValue = report.exerciseName,
                options = allReports.map { it.exerciseName },
                onOptionSelected = onSelectExercise,
                subtitleProvider = { name ->
                    val r = allReports.firstOrNull { it.exerciseName == name }
                    if (r != null) "${r.muscleGroup} • ${r.equipment} • ${r.dynamicStatusBadge}" else null
                }
            )

            // Comparison / Prescription Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = report.overloadHeadline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.overloadComparisonDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Equal-Height 4-Metric Row (Fixes Est. 1RM box being shorter than the others!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualHeightMetricMiniBox(
                    label = "Best Load",
                    value = formatWeight(displayWeightKg),
                    accent = AppleMoveRed,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EqualHeightMetricMiniBox(
                    label = "Best Reps",
                    value = "$displayReps reps",
                    accent = AppleStandBlue,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EqualHeightMetricMiniBox(
                    label = "Peak Vol",
                    value = formatVolume(displayVolumeKg),
                    accent = AppleExerciseGreen,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                EqualHeightMetricMiniBox(
                    label = "Est. 1RM",
                    value = formatWeight(display1RmKg),
                    accent = AppleMindPurple,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // 4-Tab Segmented Control that fits all 4 metrics cleanly without cutting off on the right
            AppleSegmentedControl(
                items = ProgressionChartMetric.entries,
                selectedItem = selectedMetric,
                onItemSelected = onSelectMetric,
                labelProvider = { it.shortLabel },
                accentColor = AppleOverloadOrange
            )

            ExerciseProgressionChart(
                sessions = report.sessionHistory,
                metric = selectedMetric,
                useLbs = useLbs,
                unitLabel = unitLabel,
                recommendedWeightKg = report.recommendedStartWeightKg,
                recommendedReps = report.recommendedTargetReps,
                recommendedVolumeKg = report.recommendedTargetVolumeKg,
                recommended1RmKg = report.recommendedTargetOneRepMaxKg,
                accentColor = AppleOverloadOrange
            )

            StrengthStandardGaugeBar(
                exerciseName = report.exerciseName,
                currentOrTargetWeightKg = displayWeightKg,
                userBodyweightKg = userBodyweightKg,
                unitLabel = unitLabel,
                useLbs = useLbs
            )
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    exercise: ExerciseEntity,
    report: ExerciseProgressionReport?,
    isSelectedForAnalytics: Boolean,
    formatWeight: (Float) -> String,
    onSelectForAnalytics: () -> Unit,
    onQuickAddToWorkout: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    LiquidGlassCard(
        accentTint = if (isSelectedForAnalytics) AppleStandBlue else AppleExerciseGreen,
        onClick = onSelectForAnalytics
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelectedForAnalytics) AppleStandBlue else AppleStandBlue.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (isSelectedForAnalytics) Color.White else AppleStandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${exercise.muscleGroup} • ${exercise.equipment}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    IconButton(onClick = onQuickAddToWorkout) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Workout Plan",
                            tint = AppleExerciseGreen
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exercise",
                            tint = AppleStandBlue
                        )
                    }
                    if (exercise.isCustom) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Custom Exercise",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (exercise.instructions.isNotBlank()) {
                Text(
                    text = exercise.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (report != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasHistory = report.totalSessionsLogged > 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (hasHistory) AppleOverloadOrange else AppleStandBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (hasHistory) {
                                "PR: ${formatWeight(report.personalRecordWeightKg)} × ${report.personalRecordReps} • ${report.totalSessionsLogged} sessions"
                            } else {
                                "PR: ${formatWeight(0f)} × 0 • 0 sessions logged"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasHistory) AppleOverloadOrange else AppleStandBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OverloadBadge(
                        status = report.overloadStatus,
                        dynamicLabel = report.dynamicStatusBadge
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditorDialog(
    existing: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var muscleGroup by remember { mutableStateOf(existing?.muscleGroup ?: "Chest") }
    var equipment by remember { mutableStateOf(existing?.equipment ?: "Barbell") }
    var notes by remember { mutableStateOf(existing?.instructions ?: "") }

    val muscleGroups = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")
    val equipments = listOf("Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight", "Kettlebell")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing != null) "Edit Exercise" else "Add Custom Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppleLiquidGlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Exercise Name",
                    placeholder = "e.g., Pendlay Row, Bulgarian Split Squat",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    fieldModifier = Modifier.testTag("custom_exercise_name_input")
                )

                // Fixes Image 3: Uses proper Dropdown Lists instead of clipped horizontal chip rows
                AppleDropdownField(
                    label = "Primary Muscle Group",
                    selectedValue = muscleGroup,
                    options = muscleGroups,
                    onOptionSelected = { muscleGroup = it },
                    testTag = "custom_exercise_muscle_dropdown"
                )

                AppleDropdownField(
                    label = "Equipment Type",
                    selectedValue = equipment,
                    options = equipments,
                    onOptionSelected = { equipment = it },
                    testTag = "custom_exercise_equipment_dropdown"
                )

                AppleLiquidGlassTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Form Cues & Instructions (optional)",
                    placeholder = "Technique notes or setup reminders",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            LiquidGlassPrimaryButton(
                text = "Save Exercise",
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onSave(name.trim(), muscleGroup, equipment, notes.trim())
                    }
                },
                accentColor = AppleStandBlue,
                testTag = "save_custom_exercise_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
