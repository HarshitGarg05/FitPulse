package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WorkoutSessionEntity
import com.example.data.repository.AnalyticsAndInsightsEngine
import com.example.data.repository.AnalyticsDateRange
import com.example.data.repository.ExerciseProgressionReport
import com.example.data.repository.OverloadStatus
import com.example.data.repository.PersonalRecordItem
import com.example.ui.components.AppleActivityRingsCardContent
import com.example.ui.components.AppleDropdownField
import com.example.ui.components.AppleHealthBarChart
import com.example.ui.components.AppleLargePageHeader
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.ExerciseProgressionChart
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.components.MuscleVolumeDistributionGraph
import com.example.ui.components.ProgressionChartMetric
import com.example.ui.components.StrengthStandardGaugeBar
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleGoldYellow
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.theme.AppleStandCyan
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.FitPulseUiState
import com.example.ui.viewmodel.FitnessViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    state: FitPulseUiState,
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val analytics = state.dashboardAnalytics
    val attendance = state.attendanceOverview
    val todayReadable = remember(attendance.todayDateKey) {
        AnalyticsAndInsightsEngine.formatDateKeyReadable(attendance.todayDateKey).uppercase(Locale.US)
    }
    var selectedProgressionMetric by remember { mutableStateOf(ProgressionChartMetric.TOP_WEIGHT) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
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
            // 1. Unified Apple iOS 26 Summary Large Page Header
            item {
                AppleLargePageHeader(
                    overlineText = todayReadable,
                    title = "Summary",
                    accentColor = AppleMoveRed,
                    trailingBadge = {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (attendance.attendedToday) {
                                AppleExerciseGreen.copy(alpha = 0.18f)
                            } else {
                                AppleStandBlue.copy(alpha = 0.16f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (attendance.attendedToday) AppleExerciseGreen.copy(alpha = 0.45f)
                                else AppleStandBlue.copy(alpha = 0.40f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (attendance.attendedToday) AppleExerciseGreen else AppleStandBlue
                                        )
                                )
                                Text(
                                    text = if (attendance.attendedToday) "Checked In Today" else "Ready to Train",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendance.attendedToday) AppleExerciseGreen else AppleStandBlue,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                )
            }

            // 2. Today's Overview & Real-Time 4-Ring Activity Card
            item {
                LiquidGlassCard(
                    accentTint = AppleMoveRed,
                    modifier = Modifier.testTag("todays_overview_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppleActivityRingsCardContent(
                            attendanceDaysThisWeek = attendance.weeklyVisitDays,
                            attendanceWeeklyGoal = attendance.weeklyGoalDays,
                            completedSetsThisWeek = analytics.periodCompletedSets,
                            weeklySetsTarget = (attendance.weeklyGoalDays * 12).coerceAtLeast(12),
                            currentStreakDays = attendance.currentStreakDays,
                            weeklyVolumeKg = analytics.periodTotalVolumeKg,
                            weeklyVolumeTargetKg = (attendance.weeklyGoalDays * 2500f).coerceAtLeast(3000f),
                            todayVolumeLabel = viewModel.formatVolume(analytics.volumeTodayKg)
                        )

                        // Live Workout Status Banner (when a workout is active)
                        AnimatedVisibility(
                            visible = state.activeWorkout.isWorkoutActive || state.activeWorkout.sets.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AppleStandBlue.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, AppleStandBlue.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = AppleStandBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (state.activeWorkout.isWorkoutActive) {
                                                    "Live Workout: ${state.activeWorkout.title.ifBlank { "Active Session" }}"
                                                } else {
                                                    "Draft Plan: ${state.activeWorkout.sets.map { it.exerciseName }.distinct().size} exercises planned"
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleStandBlue,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val activeEx = state.activeWorkout.activeExerciseName
                                            Text(
                                                text = if (activeEx != null) {
                                                    "Performing: $activeEx • ${state.activeWorkout.elapsedSeconds / 60}m ${state.activeWorkout.elapsedSeconds % 60}s"
                                                } else {
                                                    "Tap Resume Workout to start exercise timer"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Equal-Height Single-Line Action Buttons (NEVER wraps onto 2 lines or clips!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!attendance.attendedToday) {
                                LiquidGlassPrimaryButton(
                                    text = "Check In",
                                    onClick = { viewModel.recordGymAttendance() },
                                    icon = Icons.Default.CheckCircle,
                                    accentColor = AppleMoveRed,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    testTag = "check_in_today_button"
                                )
                            } else {
                                LiquidGlassSecondaryButton(
                                    text = "Attendance",
                                    onClick = { viewModel.selectTab(AppDestination.ATTENDANCE) },
                                    icon = Icons.Default.CalendarMonth,
                                    accentColor = AppleMoveRed,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    testTag = "open_attendance_button"
                                )
                            }

                            LiquidGlassPrimaryButton(
                                text = if (state.activeWorkout.isWorkoutActive) "Resume Workout" else "Start Workout",
                                onClick = { viewModel.openWorkoutPlanBuilder() },
                                icon = if (state.activeWorkout.isWorkoutActive) Icons.Default.PlayArrow else Icons.Default.AddCircle,
                                accentColor = AppleStandBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                testTag = "start_workout_dashboard_button"
                            )
                        }
                    }
                }
            }

            // 3. Fitness Statistics Bento Grid (Equal Height 2x2 Multi-Color Liquid Glass Tiles)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppleSectionHeader(
                        title = "Activity & Strength Highlights",
                        subtitle = "Calculated from your saved local records"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppleStatTile(
                            label = "GYM ATTENDANCE",
                            primaryValue = "${attendance.totalAllTimeVisits} visits",
                            secondaryValue = "${attendance.weeklyVisitDays} this wk • ${attendance.monthlyVisitDays} this mo",
                            icon = Icons.Default.CalendarMonth,
                            accentColor = AppleMoveRed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        AppleStatTile(
                            label = "ACTIVE STREAK",
                            primaryValue = "${attendance.currentStreakDays} ${if (attendance.currentStreakDays == 1) "day" else "days"}",
                            secondaryValue = "Longest streak: ${attendance.longestStreakDays}d",
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = AppleOverloadOrange,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppleStatTile(
                            label = "WORKOUT SESSIONS",
                            primaryValue = "${state.completedSessions.size} sessions",
                            secondaryValue = "${analytics.periodCompletedSets} sets in period",
                            icon = Icons.Default.FitnessCenter,
                            accentColor = AppleStandBlue,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        AppleStatTile(
                            label = "TRAINING VOLUME",
                            primaryValue = viewModel.formatVolume(analytics.periodTotalVolumeKg),
                            secondaryValue = "Today: ${viewModel.formatVolume(analytics.volumeTodayKg)}",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = AppleExerciseGreen,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // 4. Progress Visualization & Measurable Charts Card
            item {
                LiquidGlassCard(accentTint = AppleExerciseGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "TRAINING ANALYTICS & VOLUME GRAPHS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppleExerciseGreen
                            )
                            Text(
                                text = "Volume, Sets & Muscle Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AppleSegmentedControl(
                            items = AnalyticsDateRange.entries,
                            selectedItem = state.analyticsDateRange,
                            onItemSelected = { viewModel.setAnalyticsDateRange(it) },
                            labelProvider = { it.shortLabel },
                            accentColor = AppleExerciseGreen
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Training Volume (${state.unitLabel})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = viewModel.formatVolume(analytics.periodTotalVolumeKg),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleExerciseGreen
                                )
                            }
                            AppleHealthBarChart(
                                points = analytics.dailyVolumeSeries,
                                barColor = AppleExerciseGreen,
                                unitSuffix = state.unitLabel
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Completed Sets",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${analytics.periodCompletedSets} sets",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleMoveRed
                                )
                            }
                            AppleHealthBarChart(
                                points = analytics.dailySetsSeries,
                                barColor = AppleMoveRed,
                                unitSuffix = "sets"
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Muscle Group Volume & Set Breakdown",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MuscleVolumeDistributionGraph(
                                muscleBreakdown = analytics.muscleGroupBreakdown,
                                formatVolume = { viewModel.formatVolume(it) }
                            )
                        }
                    }
                }
            }

            // 5. Exercise-Specific Strength Progression Spotlight Card
            item {
                val reports = analytics.overloadReports
                val activeReport = state.selectedExerciseReport ?: reports.firstOrNull()

                if (activeReport != null) {
                    LiquidGlassCard(accentTint = AppleStandBlue) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "PROGRESSIVE OVERLOAD ANALYTICS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleStandBlue
                                    )
                                    Text(
                                        text = activeReport.exerciseName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OverloadBadge(
                                    status = activeReport.overloadStatus,
                                    dynamicLabel = activeReport.dynamicStatusBadge
                                )
                            }

                            AppleDropdownField(
                                label = "Select Exercise to Inspect",
                                selectedValue = activeReport.exerciseName,
                                options = reports.map { it.exerciseName },
                                onOptionSelected = { viewModel.selectExerciseForDetail(it) },
                                subtitleProvider = { name ->
                                    val r = reports.firstOrNull { it.exerciseName == name }
                                    if (r != null) "${r.muscleGroup} • ${r.equipment} • ${r.dynamicStatusBadge}" else null
                                },
                                testTag = "dashboard_exercise_dropdown"
                            )

                            val displayWeight = activeReport.personalRecordWeightKg
                            val displayReps = activeReport.personalRecordReps
                            val displayVolume = activeReport.personalRecordVolumeKg
                            val display1Rm = activeReport.personalRecordOneRepMaxKg

                            // Equal-height 4-box metric row so Est. 1RM is ALWAYS the exact same height!
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EqualHeightMetricMiniBox(
                                    label = "Best Load",
                                    value = viewModel.formatWeight(displayWeight),
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
                                    value = viewModel.formatVolume(displayVolume),
                                    accent = AppleExerciseGreen,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                EqualHeightMetricMiniBox(
                                    label = "Est. 1RM",
                                    value = viewModel.formatWeight(display1Rm),
                                    accent = AppleMindPurple,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }

                            AppleSegmentedControl(
                                items = ProgressionChartMetric.entries,
                                selectedItem = selectedProgressionMetric,
                                onItemSelected = { selectedProgressionMetric = it },
                                labelProvider = { it.shortLabel },
                                accentColor = AppleStandBlue
                            )

                            ExerciseProgressionChart(
                                sessions = activeReport.sessionHistory,
                                metric = selectedProgressionMetric,
                                useLbs = state.useLbs,
                                unitLabel = state.unitLabel,
                                recommendedWeightKg = activeReport.recommendedStartWeightKg,
                                recommendedReps = activeReport.recommendedTargetReps,
                                recommendedVolumeKg = activeReport.recommendedTargetVolumeKg,
                                recommended1RmKg = activeReport.recommendedTargetOneRepMaxKg,
                                accentColor = AppleStandBlue
                            )

                            StrengthStandardGaugeBar(
                                exerciseName = activeReport.exerciseName,
                                currentOrTargetWeightKg = displayWeight,
                                userBodyweightKg = state.profile.weightKg,
                                unitLabel = state.unitLabel,
                                useLbs = state.useLbs
                            )
                        }
                    }
                }
            }

            // 6. Recent Personal Records & Strength Improvements
            item {
                PersonalRecordsCard(
                    records = analytics.personalRecords,
                    reports = analytics.overloadReports,
                    formatWeight = { viewModel.formatWeight(it) },
                    onNavigateToExercises = { viewModel.selectTab(AppDestination.EXERCISES) }
                )
            }

            // 7. Recent Workout History
            item {
                RecentWorkoutHistorySection(
                    workouts = state.completedSessions.take(5),
                    formatVolume = { viewModel.formatVolume(it) },
                    onReuseWorkout = { session ->
                        viewModel.loadSessionIntoEditor(session, duplicateAsNewToday = true)
                    },
                    onStartNewWorkout = { viewModel.openWorkoutPlanBuilder() }
                )
            }
        }
    }
}

@Composable
private fun AppleStatTile(
    label: String,
    primaryValue: String,
    secondaryValue: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        accentTint = accentColor
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = secondaryValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EqualHeightMetricMiniBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.3.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PersonalRecordsCard(
    records: List<PersonalRecordItem>,
    reports: List<ExerciseProgressionReport>,
    formatWeight: (Float) -> String,
    onNavigateToExercises: () -> Unit
) {
    LiquidGlassCard(accentTint = AppleOverloadOrange) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppleSectionHeader(
                title = "Personal Records & Milestones",
                subtitle = if (records.isEmpty()) {
                    "0 personal records logged yet — log sets to track PRs"
                } else {
                    "${records.size} exercises with verified personal records"
                },
                actionLabel = "Inspect",
                actionIcon = Icons.Default.EmojiEvents,
                actionColor = AppleOverloadOrange,
                onActionClick = onNavigateToExercises
            )

            if (records.isNotEmpty()) {
                records.take(4).forEachIndexed { index, pr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pr.exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pr.muscleGroup} • Achieved ${pr.achievedDateLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppleOverloadOrange.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, AppleOverloadOrange.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${formatWeight(pr.bestWeightKg)} × ${pr.bestReps}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleOverloadOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (index < records.take(4).lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            } else {
                reports.take(3).forEachIndexed { index, report ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${report.muscleGroup} • 0 sessions logged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppleStandBlue.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AppleStandBlue.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${formatWeight(0f)} × 0",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleStandBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (index < reports.take(3).lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentWorkoutHistorySection(
    workouts: List<WorkoutSessionEntity>,
    formatVolume: (Float) -> String,
    onReuseWorkout: (WorkoutSessionEntity) -> Unit,
    onStartNewWorkout: () -> Unit
) {
    LiquidGlassCard(accentTint = AppleMindPurple) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppleSectionHeader(
                title = "Recent Workout Sessions",
                subtitle = if (workouts.isEmpty()) "No sessions recorded yet" else "Tap Repeat to load a past plan",
                actionLabel = "Build Plan",
                actionIcon = Icons.Default.FitnessCenter,
                actionColor = AppleMindPurple,
                onActionClick = onStartNewWorkout
            )

            if (workouts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Build your first workout plan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create a custom plan in the Workouts tab, save it for next time, and tap each exercise as you perform it to start the live timer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                workouts.forEachIndexed { idx, workout ->
                    val dateStr = AnalyticsAndInsightsEngine.formatDateKeyReadable(workout.dateKey)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workout.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$dateStr • ${workout.durationMinutes} min • ${workout.totalSets} sets • ${formatVolume(workout.totalVolumeKg)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidGlassSecondaryButton(
                            text = "Repeat",
                            onClick = { onReuseWorkout(workout) },
                            accentColor = AppleMindPurple
                        )
                    }
                    if (idx < workouts.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
        }
    }
}

@Composable
fun OverloadBadge(
    status: OverloadStatus,
    dynamicLabel: String? = null
) {
    val color = when (status) {
        OverloadStatus.NEW_PR -> AppleGoldYellow
        OverloadStatus.INCREASED_WEIGHT -> AppleExerciseGreen
        OverloadStatus.MORE_REPS -> AppleStandBlue
        OverloadStatus.INCREASED_VOLUME -> AppleExerciseGreen
        OverloadStatus.MAINTAINED_OR_LOWER -> AppleOverloadOrange
        OverloadStatus.BASELINE_ESTABLISHED -> AppleMindPurple
        OverloadStatus.INSUFFICIENT_DATA -> AppleStandCyan
    }
    val label = dynamicLabel ?: status.badgeLabel

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (status == OverloadStatus.NEW_PR) AppleOverloadOrange else color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppleHealthCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        accentTint = AppleStandBlue,
        onClick = onClick,
        content = content
    )
}
