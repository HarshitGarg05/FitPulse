package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FitnessDatabase
import com.example.data.repository.BackupAndRestoreManager
import com.example.ui.components.AppleDropdownField
import com.example.ui.components.AppleLargePageHeader
import com.example.ui.components.AppleLiquidGlassTextField
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPrimaryButton
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.FitPulseUiState
import com.example.ui.viewmodel.FitnessViewModel
import java.util.Locale

@Composable
fun ProfileAndPrivacyScreen(
    state: FitPulseUiState,
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    val profile = state.profile

    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var weightInput by remember(profile.weightKg, state.useLbs) {
        mutableStateOf(viewModel.toDisplayWeightValue(profile.weightKg).toString())
    }
    var heightInput by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var weeklyGoalInput by remember(profile.weeklyAttendanceGoal) {
        mutableStateOf(profile.weeklyAttendanceGoal.toString())
    }
    var fitnessLevelInput by remember(profile.fitnessLevel) { mutableStateOf(profile.fitnessLevel) }
    var primaryGoalInput by remember(profile.primaryObjective) { mutableStateOf(profile.primaryObjective) }
    var allowMultipleVisits by remember(profile.allowMultipleVisitsPerDay) {
        mutableStateOf(profile.allowMultipleVisitsPerDay)
    }

    var showClearConfirmation by remember { mutableStateOf(false) }
    var showPdfPreviewPages by remember { mutableStateOf(false) }
    var targetPlateWeightInput by remember { mutableStateOf("100") }
    var replaceOnRestore by remember { mutableStateOf(false) }
    var showJsonBackupModal by remember { mutableStateOf(false) }
    var jsonBackupText by remember { mutableStateOf("") }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(
        state.allAttendanceRecords.size,
        state.completedSessions.size,
        state.allExercises.size,
        state.routines.size
    ) {
        viewModel.refreshLocalBackupSnapshots()
        if (state.generatedPdfReport == null && !state.isGeneratingPdf) {
            viewModel.generateFormattedPdfReport()
        }
    }

    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.saveGeneratedPdfToUri(uri)
        }
    }

    val exportBackupJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupJsonToUri(uri)
        }
    }

    val exportBackupPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupPdfToUri(uri)
        }
    }

    val importBackupJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupJsonFromUri(
                sourceUri = uri,
                replaceExisting = replaceOnRestore
            )
        }
    }

    val bmiValue = remember(profile.weightKg, profile.heightCm) {
        val hMeters = (profile.heightCm / 100f).coerceAtLeast(1.2f)
        profile.weightKg / (hMeters * hMeters)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
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
                    overlineText = "TRAINING PREFERENCES, BACKUP & PDF EXPORT",
                    title = "Settings",
                    accentColor = AppleStandBlue,
                    actionLabel = "Backup Now",
                    actionIcon = Icons.Default.Backup,
                    actionTestTag = "header_quick_backup_button",
                    onActionClick = { viewModel.createManualBackupSnapshot() }
                )
            }

            // 1. Gym & Progressive Overload Engine Settings
            item {
                LiquidGlassCard(accentTint = AppleExerciseGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AppleExerciseGreen.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AppleExerciseGreen
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Workout & Overload Automation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Configure rest intervals, progressive overload steps, and barbell specs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Preferred Weight Unit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AppleSegmentedControl(
                                items = listOf("kg", "lbs"),
                                selectedItem = state.unitLabel,
                                onItemSelected = { viewModel.setPreferredWeightUnit(it) },
                                labelProvider = { if (it == "kg") "Kilograms (kg)" else "Pounds (lbs)" },
                                accentColor = AppleExerciseGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleDropdownField(
                                label = "Default Rest Timer",
                                selectedValue = "${profile.defaultRestSeconds}s",
                                options = listOf("60s", "90s", "120s", "180s"),
                                onOptionSelected = { sel ->
                                    val secs = sel.removeSuffix("s").toIntOrNull() ?: 90
                                    viewModel.updateProfileSettings(
                                        displayName = profile.displayName,
                                        weightDisplay = viewModel.toDisplayWeightValue(profile.weightKg),
                                        heightCm = profile.heightCm,
                                        fitnessLevel = profile.fitnessLevel,
                                        primaryObjective = profile.primaryObjective,
                                        weeklyAttendanceGoal = profile.weeklyAttendanceGoal,
                                        trainingDaysCsv = profile.trainingDaysCsv,
                                        allowMultipleVisitsPerDay = profile.allowMultipleVisitsPerDay,
                                        defaultRestSeconds = secs
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            val stepStr = if (profile.overloadIncrementKg % 1f == 0f) {
                                profile.overloadIncrementKg.toInt().toString()
                            } else {
                                profile.overloadIncrementKg.toString()
                            }
                            AppleDropdownField(
                                label = "Overload Step (${state.unitLabel})",
                                selectedValue = "$stepStr ${state.unitLabel}",
                                options = listOf(
                                    "1.25 ${state.unitLabel}",
                                    "2.5 ${state.unitLabel}",
                                    "5.0 ${state.unitLabel}"
                                ),
                                onOptionSelected = { sel ->
                                    val inc = sel.substringBefore(" ").toFloatOrNull() ?: 2.5f
                                    viewModel.updateProfileSettings(
                                        displayName = profile.displayName,
                                        weightDisplay = viewModel.toDisplayWeightValue(profile.weightKg),
                                        heightCm = profile.heightCm,
                                        fitnessLevel = profile.fitnessLevel,
                                        primaryObjective = profile.primaryObjective,
                                        weeklyAttendanceGoal = profile.weeklyAttendanceGoal,
                                        trainingDaysCsv = profile.trainingDaysCsv,
                                        allowMultipleVisitsPerDay = profile.allowMultipleVisitsPerDay,
                                        overloadIncrementKg = inc
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AppleDropdownField(
                            label = "Standard Olympic Barbell Weight",
                            selectedValue = "${profile.barbellWeightKg.toInt()} ${state.unitLabel} Standard Bar",
                            options = listOf(
                                "20 ${state.unitLabel} Standard Bar",
                                "15 ${state.unitLabel} Technique Bar",
                                "10 ${state.unitLabel} EZ-Curl Bar"
                            ),
                            onOptionSelected = { sel ->
                                val bar = sel.substringBefore(" ").toFloatOrNull() ?: 20f
                                viewModel.updateProfileSettings(
                                    displayName = profile.displayName,
                                    weightDisplay = viewModel.toDisplayWeightValue(profile.weightKg),
                                    heightCm = profile.heightCm,
                                    fitnessLevel = profile.fitnessLevel,
                                    primaryObjective = profile.primaryObjective,
                                    weeklyAttendanceGoal = profile.weeklyAttendanceGoal,
                                    trainingDaysCsv = profile.trainingDaysCsv,
                                    allowMultipleVisitsPerDay = profile.allowMultipleVisitsPerDay,
                                    barbellWeightKg = bar
                                )
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Auto-Start Rest Timer on Set Register",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Starts your ${profile.defaultRestSeconds}s countdown whenever you register a set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = profile.autoStartRestTimer,
                                onCheckedChange = { enabled ->
                                    viewModel.updateProfileSettings(
                                        displayName = profile.displayName,
                                        weightDisplay = viewModel.toDisplayWeightValue(profile.weightKg),
                                        heightCm = profile.heightCm,
                                        fitnessLevel = profile.fitnessLevel,
                                        primaryObjective = profile.primaryObjective,
                                        weeklyAttendanceGoal = profile.weeklyAttendanceGoal,
                                        trainingDaysCsv = profile.trainingDaysCsv,
                                        allowMultipleVisitsPerDay = profile.allowMultipleVisitsPerDay,
                                        autoStartRestTimer = enabled
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = AppleExerciseGreen)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Auto-Log Gym Attendance on Workout Save",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Automatically marks the session date as attended when finishing a workout",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = profile.autoCheckInOnWorkoutComplete,
                                onCheckedChange = { enabled ->
                                    viewModel.updateProfileSettings(
                                        displayName = profile.displayName,
                                        weightDisplay = viewModel.toDisplayWeightValue(profile.weightKg),
                                        heightCm = profile.heightCm,
                                        fitnessLevel = profile.fitnessLevel,
                                        primaryObjective = profile.primaryObjective,
                                        weeklyAttendanceGoal = profile.weeklyAttendanceGoal,
                                        trainingDaysCsv = profile.trainingDaysCsv,
                                        allowMultipleVisitsPerDay = profile.allowMultipleVisitsPerDay,
                                        autoCheckInOnWorkoutComplete = enabled
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = AppleExerciseGreen)
                            )
                        }
                    }
                }
            }

            // 2. Interactive Barbell Plate Loader Calculator Card
            item {
                BarbellPlateCalculatorCard(
                    barbellWeightKg = profile.barbellWeightKg,
                    unitLabel = state.unitLabel,
                    targetWeightText = targetPlateWeightInput,
                    onTargetWeightChange = { targetPlateWeightInput = it }
                )
            }

            // 3. Appearance & Apple Liquid Glass Theme Mode
            item {
                LiquidGlassCard(accentTint = AppleStandBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "APPEARANCE & LIQUID GLASS THEME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleStandBlue
                        )
                        Text(
                            text = "Display Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AppleSegmentedControl(
                            items = ThemeMode.entries,
                            selectedItem = state.themeMode,
                            onItemSelected = { viewModel.setThemeMode(it) },
                            labelProvider = { mode ->
                                when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "Auto"
                                }
                            },
                            accentColor = AppleStandBlue
                        )
                    }
                }
            }

            // 4. Formatted & Designed PDF Fitness Report Card (Fixes vertical green bar & button clipping!)
            item {
                LiquidGlassCard(
                    accentTint = AppleMoveRed,
                    modifier = Modifier.testTag("pdf_export_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppleMoveRed.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = AppleMoveRed
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Formatted PDF Fitness Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Export an Apple Health-styled PDF with your attendance, streaks, workouts, and progressive overload records.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Equal-height single-line buttons that never wrap or clip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassPrimaryButton(
                                text = "Refresh PDF",
                                onClick = { viewModel.generateFormattedPdfReport() },
                                icon = Icons.Default.Refresh,
                                accentColor = AppleMoveRed,
                                enabled = !state.isGeneratingPdf,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                testTag = "generate_pdf_report_button"
                            )

                            val latestReport = state.generatedPdfReport
                            if (latestReport != null) {
                                LiquidGlassSecondaryButton(
                                    text = "Save PDF File",
                                    onClick = { createPdfDocumentLauncher.launch(latestReport.fileName) },
                                    icon = Icons.Default.Download,
                                    accentColor = AppleMoveRed,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    testTag = "save_pdf_to_device_button"
                                )
                            }
                        }

                        if (state.isGeneratingPdf) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleMoveRed
                                )
                                Text(
                                    text = "Rendering formatted PDF pages...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        val report = state.generatedPdfReport
                        if (report != null) {
                            // Fixed layout: Filename column uses Modifier.weight(1f) and single-line ellipsis
                            // so the green "Ready" badge is NEVER squashed into a vertical bar!
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = report.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Generated ${report.generatedAtLabel} • ${report.pageCount} ${if (report.pageCount == 1) "page" else "pages"} • ${(report.file.length() / 1024L).coerceAtLeast(1L)} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = AppleExerciseGreen.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, AppleExerciseGreen.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = AppleExerciseGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Ready",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleExerciseGreen,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            LiquidGlassSecondaryButton(
                                text = if (showPdfPreviewPages) "Hide PDF Page Preview" else "Preview Formatted PDF (${report.pageCount} Page${if (report.pageCount == 1) "" else "s"})",
                                onClick = { showPdfPreviewPages = !showPdfPreviewPages },
                                icon = if (showPdfPreviewPages) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                accentColor = AppleStandBlue,
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(visible = showPdfPreviewPages && report.previewBitmaps.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    report.previewBitmaps.forEachIndexed { idx, bmp ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            shadowElevation = 3.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Formatted PDF Report Page ${idx + 1}",
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Athlete Profile, Body Composition & Weekly Targets Card
            item {
                LiquidGlassCard(accentTint = AppleMindPurple) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AppleMindPurple
                                )
                                Text(
                                    text = "Athlete Profile & Biometrics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = AppleMindPurple.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "BMI %.1f", bmiValue),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleMindPurple,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        AppleLiquidGlassTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = "Athlete Name",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleLiquidGlassTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = "Bodyweight (${state.unitLabel})",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AppleLiquidGlassTextField(
                                value = heightInput,
                                onValueChange = { heightInput = it },
                                label = "Height (cm)",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleDropdownField(
                                label = "Experience Level",
                                selectedValue = fitnessLevelInput,
                                options = listOf("Beginner", "Intermediate", "Advanced"),
                                onOptionSelected = { fitnessLevelInput = it },
                                modifier = Modifier.weight(1f)
                            )
                            AppleLiquidGlassTextField(
                                value = weeklyGoalInput,
                                onValueChange = { weeklyGoalInput = it },
                                label = "Weekly Gym Goal (1-7)",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AppleDropdownField(
                            label = "Primary Objective",
                            selectedValue = primaryGoalInput,
                            options = listOf(
                                "Strength & Progressive Overload",
                                "Hypertrophy & Muscle Gain",
                                "Powerlifting & 1RM Strength",
                                "General Fitness & Consistency"
                            ),
                            onOptionSelected = { primaryGoalInput = it }
                        )

                        LiquidGlassPrimaryButton(
                            text = "Save Profile & Targets",
                            onClick = {
                                viewModel.updateProfileSettings(
                                    displayName = nameInput.trim().ifEmpty { "Athlete" },
                                    weightDisplay = weightInput.toFloatOrNull() ?: 75f,
                                    heightCm = heightInput.toFloatOrNull() ?: 175f,
                                    fitnessLevel = fitnessLevelInput,
                                    primaryObjective = primaryGoalInput,
                                    weeklyAttendanceGoal = (weeklyGoalInput.toIntOrNull() ?: 4).coerceIn(1, 7),
                                    trainingDaysCsv = profile.trainingDaysCsv,
                                    allowMultipleVisitsPerDay = allowMultipleVisits
                                )
                            },
                            accentColor = AppleMindPurple,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "save_profile_button"
                        )
                    }
                }
            }

            // 6. Full App Data Backup & Restore (Export .json, Import .json, Auto-Snapshots & Clipboard)
            item {
                LiquidGlassCard(
                    accentTint = AppleStandBlue,
                    modifier = Modifier.testTag("data_backup_restore_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppleStandBlue.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = AppleStandBlue
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full Data Backup & Restore",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Export or import all attendance check-ins, workouts, sets, saved plans, custom exercises, and profile settings (.json).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Import / Restore Mode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AppleSegmentedControl(
                                items = listOf(false, true),
                                selectedItem = replaceOnRestore,
                                onItemSelected = { replaceOnRestore = it },
                                labelProvider = { isReplace ->
                                    if (isReplace) "Full Replace" else "Smart Merge (Safe)"
                                },
                                accentColor = AppleStandBlue
                            )
                        }

                        // Export JSON & Export PDF Backup Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassPrimaryButton(
                                text = "Export JSON",
                                onClick = {
                                    exportBackupJsonLauncher.launch(BackupAndRestoreManager.defaultExportFileName())
                                },
                                icon = Icons.Default.Download,
                                accentColor = AppleStandBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                testTag = "export_backup_json_button"
                            )

                            LiquidGlassPrimaryButton(
                                text = "Export PDF Backup",
                                onClick = {
                                    exportBackupPdfLauncher.launch(BackupAndRestoreManager.defaultExportPdfBackupFileName())
                                },
                                icon = Icons.Default.PictureAsPdf,
                                accentColor = AppleMoveRed,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                testTag = "export_backup_pdf_button"
                            )
                        }

                        // Import (.json or .pdf), Save Snapshot & Copy/Paste JSON Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassSecondaryButton(
                                text = "Import (.json / .pdf)",
                                onClick = {
                                    importBackupJsonLauncher.launch(
                                        arrayOf("application/json", "application/pdf", "text/plain", "*/*")
                                    )
                                },
                                icon = Icons.Default.FileOpen,
                                accentColor = AppleExerciseGreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                testTag = "import_backup_json_button"
                            )

                            LiquidGlassSecondaryButton(
                                text = "Save Snapshot",
                                onClick = { viewModel.createManualBackupSnapshot() },
                                icon = Icons.Default.Backup,
                                accentColor = AppleMindPurple,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                testTag = "create_manual_snapshot_button"
                            )
                        }

                        LiquidGlassSecondaryButton(
                            text = "Copy / Paste Raw Backup JSON",
                            onClick = {
                                viewModel.generateBackupJsonString { generated ->
                                    jsonBackupText = generated
                                    showJsonBackupModal = true
                                }
                            },
                            icon = Icons.Default.ContentCopy,
                            accentColor = AppleOverloadOrange,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "open_json_clipboard_modal_button"
                        )

                        if (state.localBackupSnapshots.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Text(
                                text = "SAVED ON-DEVICE BACKUP SNAPSHOTS (${state.localBackupSnapshots.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppleStandBlue
                            )
                            state.localBackupSnapshots.take(4).forEach { snap ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (snap.isAutoBackup) "Auto-Backup Snapshot (Latest)" else snap.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${snap.createdAtLabel} • ${snap.attendanceCount} visits • ${snap.sessionsCount} workouts • ${snap.setsCount} sets",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        LiquidGlassSecondaryButton(
                                            text = "Restore",
                                            onClick = {
                                                viewModel.restoreFromLocalSnapshot(
                                                    snapshot = snap,
                                                    replaceExisting = replaceOnRestore
                                                )
                                            },
                                            icon = Icons.Default.Restore,
                                            accentColor = AppleStandBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Local Storage Status & Reset Management
            item {
                LiquidGlassCard(accentTint = AppleOverloadOrange) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = AppleExerciseGreen
                            )
                            Column {
                                Text(
                                    text = "Room SQLite v${FitnessDatabase.CURRENT_SCHEMA_VERSION} • Write-Ahead Logging (WAL)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Non-destructive migrations v1→v${FitnessDatabase.CURRENT_SCHEMA_VERSION} active • ${state.allAttendanceRecords.size} check-ins • ${state.completedSessions.size} workouts • ${state.allExercises.size} exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        LiquidGlassSecondaryButton(
                            text = "Reset All Recorded Workouts & Attendance",
                            onClick = { showClearConfirmation = true },
                            icon = Icons.Default.DeleteForever,
                            accentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showJsonBackupModal) {
        AlertDialog(
            onDismissRequest = { showJsonBackupModal = false },
            title = {
                Text(
                    text = "Backup & Restore JSON Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Copy this JSON to back up your data anywhere, or paste a saved FitPulse backup JSON below and tap Restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LiquidGlassSecondaryButton(
                        text = "Copy Backup JSON to Clipboard",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonBackupText))
                        },
                        icon = Icons.Default.ContentCopy,
                        accentColor = AppleStandBlue,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppleLiquidGlassTextField(
                        value = jsonBackupText,
                        onValueChange = { jsonBackupText = it },
                        label = "FitPulse Backup JSON",
                        singleLine = false,
                        maxLines = 8,
                        accentColor = AppleStandBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .testTag("backup_json_text_input")
                    )
                }
            },
            confirmButton = {
                LiquidGlassPrimaryButton(
                    text = "Restore JSON",
                    onClick = {
                        viewModel.restoreFromBackupJsonText(
                            jsonText = jsonBackupText,
                            replaceExisting = replaceOnRestore
                        )
                        showJsonBackupModal = false
                    },
                    icon = Icons.Default.Restore,
                    accentColor = AppleExerciseGreen,
                    testTag = "confirm_restore_json_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showJsonBackupModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All Recorded Gym Data?") },
            text = {
                Text("This clears recorded gym attendance check-ins, saved plans, and workout sessions. An automatic backup snapshot will be saved first so you can restore anytime.")
            },
            confirmButton = {
                LiquidGlassPrimaryButton(
                    text = "Delete All History",
                    onClick = {
                        viewModel.clearAllLocalData()
                        showClearConfirmation = false
                    },
                    accentColor = MaterialTheme.colorScheme.error
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarbellPlateCalculatorCard(
    barbellWeightKg: Float,
    unitLabel: String,
    targetWeightText: String,
    onTargetWeightChange: (String) -> Unit
) {
    val targetTotal = targetWeightText.toFloatOrNull() ?: barbellWeightKg
    val perSideWeight = ((targetTotal - barbellWeightKg) / 2f).coerceAtLeast(0f)

    val platesPerSide = remember(perSideWeight) {
        val availablePlates = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)
        var remaining = perSideWeight
        val result = mutableListOf<Float>()
        for (plate in availablePlates) {
            while (remaining >= plate - 0.01f && result.size < 12) {
                result.add(plate)
                remaining -= plate
            }
        }
        result
    }

    LiquidGlassCard(accentTint = AppleOverloadOrange) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AppleOverloadOrange.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = AppleOverloadOrange
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Barbell Plate Loader Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Exact plates per side using your ${barbellWeightKg.toInt()} $unitLabel barbell",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                AppleLiquidGlassTextField(
                    value = targetWeightText,
                    onValueChange = onTargetWeightChange,
                    label = "Target Total Lift ($unitLabel)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppleOverloadOrange.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AppleOverloadOrange.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EACH SIDE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppleOverloadOrange
                        )
                        val sideFormatted = if (perSideWeight % 1f == 0f) {
                            perSideWeight.toInt().toString()
                        } else {
                            String.format(Locale.US, "%.2f", perSideWeight)
                        }
                        Text(
                            text = "$sideFormatted $unitLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (platesPerSide.isEmpty()) {
                Text(
                    text = "Empty bar (${barbellWeightKg.toInt()} $unitLabel) — enter a weight above ${barbellWeightKg.toInt()} $unitLabel to calculate plates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    platesPerSide.forEach { plate ->
                        val plateColor = when (plate) {
                            25f -> AppleMoveRed
                            20f -> AppleStandBlue
                            15f -> AppleOverloadOrange
                            10f -> AppleExerciseGreen
                            else -> AppleMindPurple
                        }
                        val pLabel = if (plate % 1f == 0f) plate.toInt().toString() else plate.toString()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = plateColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, plateColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "$pLabel $unitLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = plateColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
