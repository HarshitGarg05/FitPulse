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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.GymAttendanceEntity
import com.example.data.repository.AnalyticsAndInsightsEngine
import com.example.data.repository.AnalyticsDateRange
import com.example.ui.components.AppleDropdownField
import com.example.ui.components.AppleHealthBarChart
import com.example.ui.components.AppleLargePageHeader
import com.example.ui.components.AppleLiquidGlassTextField
import com.example.ui.components.AppleSectionHeader
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AttendanceCalendarGrid
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.viewmodel.FitPulseUiState
import com.example.ui.viewmodel.FitnessViewModel

@Composable
fun AttendanceScreen(
    state: FitPulseUiState,
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val overview = state.attendanceOverview
    var selectedDateKey by remember { mutableStateOf<String?>(null) }
    var showLogModalForDateKey by remember { mutableStateOf<String?>(null) }
    var editingRecord by remember { mutableStateOf<GymAttendanceEntity?>(null) }
    var editingNoteText by remember { mutableStateOf("") }
    var editingDurationText by remember { mutableStateOf("60") }
    var showGoalEditor by remember { mutableStateOf(false) }

    val recentDateOptions = remember {
        (0..14).map { daysAgo ->
            val key = AnalyticsAndInsightsEngine.dateKeyForDaysAgo(daysAgo)
            val label = AnalyticsAndInsightsEngine.formatDateKeyReadable(key)
            key to "$label ($key)"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("attendance_screen"),
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
                    overlineText = "GYM CHECK-INS, STREAKS & CONSISTENCY",
                    title = "Attendance",
                    accentColor = if (overview.attendedToday) AppleExerciseGreen else AppleMoveRed,
                    actionLabel = if (overview.attendedToday) "Log Visit" else "Check In",
                    actionIcon = Icons.Default.CheckCircle,
                    actionTestTag = "attendance_primary_checkin_button",
                    onActionClick = {
                        editingRecord = overview.todayRecords.firstOrNull()
                        editingNoteText = editingRecord?.note ?: "Gym Training Session"
                        editingDurationText = (editingRecord?.durationMinutes ?: 60).toString()
                        showLogModalForDateKey = overview.todayDateKey
                    }
                )
            }

            // 1. Weekly Goal Progress & Equal-Height Streak Metrics
            item {
                LiquidGlassCard(accentTint = AppleMoveRed) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "WEEKLY ATTENDANCE GOAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleMoveRed
                                )
                                Text(
                                    text = "${overview.weeklyVisitDays} of ${overview.weeklyGoalDays} gym days this week",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LiquidGlassSecondaryButton(
                                text = "Edit Goal",
                                onClick = { showGoalEditor = !showGoalEditor },
                                accentColor = AppleStandBlue,
                                testTag = "edit_weekly_goal_button"
                            )
                        }

                        LinearProgressIndicator(
                            progress = { overview.weeklyGoalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = AppleMoveRed,
                            trackColor = AppleMoveRed.copy(alpha = 0.15f)
                        )

                        if (showGoalEditor) {
                            var sliderVal by remember(overview.weeklyGoalDays) {
                                mutableFloatStateOf(overview.weeklyGoalDays.toFloat())
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Target Days Per Week: ${sliderVal.toInt()} days",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Slider(
                                        value = sliderVal,
                                        onValueChange = { sliderVal = it },
                                        onValueChangeFinished = {
                                            val p = state.profile
                                            viewModel.updateProfileSettings(
                                                displayName = p.displayName,
                                                weightDisplay = viewModel.toDisplayWeightValue(p.weightKg),
                                                heightCm = p.heightCm,
                                                fitnessLevel = p.fitnessLevel,
                                                primaryObjective = p.primaryObjective,
                                                weeklyAttendanceGoal = sliderVal.toInt(),
                                                trainingDaysCsv = p.trainingDaysCsv,
                                                allowMultipleVisitsPerDay = p.allowMultipleVisitsPerDay
                                            )
                                        },
                                        valueRange = 1f..7f,
                                        steps = 5,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AppleMoveRed,
                                            activeTrackColor = AppleMoveRed
                                        )
                                    )
                                }
                            }
                        }

                        // Equal-Height 4-Metric Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EqualHeightMetricMiniBox(
                                label = "Active Streak",
                                value = "${overview.currentStreakDays}d",
                                accent = AppleOverloadOrange,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            EqualHeightMetricMiniBox(
                                label = "Best Streak",
                                value = "${overview.longestStreakDays}d",
                                accent = AppleStandBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            EqualHeightMetricMiniBox(
                                label = "This Month",
                                value = "${overview.monthlyVisitDays}d",
                                accent = AppleExerciseGreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            EqualHeightMetricMiniBox(
                                label = "Total Visits",
                                value = "${overview.totalAllTimeVisits}",
                                accent = AppleMoveRed,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // 2. 7-Day Attendance Consistency Bar Graph
            item {
                LiquidGlassCard(accentTint = AppleExerciseGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "7-DAY CHECK-IN CONSISTENCY GRAPH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleExerciseGreen
                        )
                        Text(
                            text = "Daily Gym Visits This Week",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AppleHealthBarChart(
                            points = overview.weeklyAttendanceSeries,
                            barColor = AppleMoveRed,
                            unitSuffix = "visits"
                        )
                    }
                }
            }

            // 3. Interactive Monthly Attendance Calendar
            item {
                LiquidGlassCard(accentTint = AppleStandBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.navigateCalendarMonth(-1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Month"
                                )
                            }
                            Text(
                                text = overview.calendarMonthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.navigateCalendarMonth(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Month"
                                )
                            }
                        }

                        AttendanceCalendarGrid(
                            cells = overview.calendarDays,
                            selectedDateKey = selectedDateKey,
                            onSelectDate = { cell ->
                                selectedDateKey = cell.dateKey
                                if (cell.dateKey <= overview.todayDateKey) {
                                    editingRecord = cell.records.firstOrNull()
                                    editingNoteText = editingRecord?.note ?: "Gym Training Session"
                                    editingDurationText = (editingRecord?.durationMinutes ?: 60).toString()
                                    showLogModalForDateKey = cell.dateKey
                                }
                            }
                        )
                    }
                }
            }

            // 4. Filterable Chronological Attendance History
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppleSectionHeader(
                        title = "Attendance History",
                        subtitle = "${state.filteredAttendanceRecords.size} check-ins in selected period",
                        actionLabel = "Log Past Date",
                        actionIcon = Icons.Default.Add,
                        actionColor = AppleStandBlue,
                        onActionClick = {
                            val yesterday = AnalyticsAndInsightsEngine.dateKeyForDaysAgo(1)
                            editingRecord = state.allAttendanceRecords.firstOrNull { it.dateKey == yesterday }
                            editingNoteText = editingRecord?.note ?: ""
                            editingDurationText = (editingRecord?.durationMinutes ?: 60).toString()
                            showLogModalForDateKey = yesterday
                        }
                    )

                    AppleSegmentedControl(
                        items = AnalyticsDateRange.entries,
                        selectedItem = state.attendanceFilterRange,
                        onItemSelected = { viewModel.setAttendanceFilterRange(it) },
                        labelProvider = { it.shortLabel },
                        accentColor = AppleMoveRed
                    )
                }
            }

            if (state.filteredAttendanceRecords.isEmpty()) {
                item {
                    LiquidGlassCard(accentTint = AppleStandBlue) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No gym visits recorded for this filter",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap any date on the calendar above or tap 'Check In' to record your gym attendance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(state.filteredAttendanceRecords, key = { it.id }) { record ->
                    AttendanceRecordRow(
                        record = record,
                        onEdit = {
                            editingRecord = record
                            editingNoteText = record.note
                            editingDurationText = record.durationMinutes.toString()
                            showLogModalForDateKey = record.dateKey
                        },
                        onDelete = { viewModel.deleteGymAttendance(record.id) }
                    )
                }
            }
        }
    }

    val modalDateKey = showLogModalForDateKey
    if (modalDateKey != null) {
        val currentOptionLabel = recentDateOptions.firstOrNull { it.first == modalDateKey }?.second
            ?: "${AnalyticsAndInsightsEngine.formatDateKeyReadable(modalDateKey)} ($modalDateKey)"

        AlertDialog(
            onDismissRequest = {
                showLogModalForDateKey = null
                editingRecord = null
            },
            title = {
                Text(
                    text = if (editingRecord != null) "Edit Gym Check-In" else "Log Gym Attendance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppleDropdownField(
                        label = "Select Check-In Date",
                        selectedValue = currentOptionLabel,
                        options = recentDateOptions.map { it.second },
                        onOptionSelected = { selectedLabel ->
                            val matchedKey = recentDateOptions.firstOrNull { it.second == selectedLabel }?.first
                            if (matchedKey != null) {
                                showLogModalForDateKey = matchedKey
                                val existing = state.allAttendanceRecords.firstOrNull { it.dateKey == matchedKey }
                                editingRecord = existing
                                editingNoteText = existing?.note ?: ""
                                editingDurationText = (existing?.durationMinutes ?: 60).toString()
                            }
                        }
                    )

                    AppleLiquidGlassTextField(
                        value = editingNoteText,
                        onValueChange = { editingNoteText = it },
                        label = "Session focus or note",
                        placeholder = "e.g., Upper Body Strength, Leg Day",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        fieldModifier = Modifier.testTag("attendance_note_input")
                    )

                    AppleLiquidGlassTextField(
                        value = editingDurationText,
                        onValueChange = { editingDurationText = it },
                        label = "Session Duration (minutes)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LiquidGlassPrimaryButton(
                    text = if (editingRecord != null) "Save Changes" else "Confirm Check-In",
                    onClick = {
                        val dur = editingDurationText.toIntOrNull() ?: 60
                        val existing = editingRecord
                        if (existing != null) {
                            viewModel.updateGymAttendance(existing, modalDateKey, editingNoteText, dur)
                        } else {
                            viewModel.recordGymAttendance(modalDateKey, editingNoteText, dur)
                        }
                        showLogModalForDateKey = null
                        editingRecord = null
                    },
                    accentColor = AppleMoveRed,
                    testTag = "confirm_attendance_modal_button"
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val existing = editingRecord
                    if (existing != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteGymAttendance(existing.id)
                                showLogModalForDateKey = null
                                editingRecord = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(
                        onClick = {
                            showLogModalForDateKey = null
                            editingRecord = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun AttendanceRecordRow(
    record: GymAttendanceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatted = remember(record.dateKey) {
        AnalyticsAndInsightsEngine.formatDateKeyReadable(record.dateKey)
    }

    LiquidGlassCard(accentTint = AppleExerciseGreen) {
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
                        .background(AppleExerciseGreen.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppleExerciseGreen
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${record.note.ifBlank { "Gym attendance verified" }} • ${record.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Check-In",
                        tint = AppleStandBlue
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Check-In",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
