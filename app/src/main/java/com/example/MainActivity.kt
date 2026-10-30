package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DynamicIslandNavItem
import com.example.ui.components.LiquidGlassAmbientBackground
import com.example.ui.components.LiquidGlassDynamicIslandBar
import com.example.ui.components.OriginIslandLiveTopCapsule
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsAndRecoveryScreen
import com.example.ui.screens.ProfileAndPrivacyScreen
import com.example.ui.screens.WorkoutLoggerScreen
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.FitnessViewModel
import com.example.util.OriginIslandLiveManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureHighRefreshRateDisplay()
        OriginIslandLiveManager.ensureChannelCreated(this)
        setContent {
            val fitnessViewModel: FitnessViewModel = viewModel()
            val state by fitnessViewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = state.themeMode) {
                FitPulseApp(
                    viewModel = fitnessViewModel
                )
            }
        }
    }

    /**
     * Optimizes frame pacing across 60Hz, 90Hz, 120Hz, and 144Hz ProMotion/Adaptive displays
     * by selecting the highest refresh rate display mode matching the active physical resolution.
     */
    private fun configureHighRefreshRateDisplay() {
        runCatching {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val activeMode = currentDisplay.mode
            val bestHighRefreshMode = currentDisplay.supportedModes
                .filter {
                    it.physicalWidth == activeMode.physicalWidth &&
                        it.physicalHeight == activeMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }

            if (bestHighRefreshMode != null) {
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = bestHighRefreshMode.modeId
                layoutParams.preferredRefreshRate = bestHighRefreshMode.refreshRate
                window.attributes = layoutParams
            }
        }
    }
}

@Composable
fun FitPulseApp(
    viewModel: FitnessViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        OriginIslandLiveManager.syncOriginIslandState(
            context = context,
            activeWorkout = state.activeWorkout,
            unitLabel = state.unitLabel,
            useLbs = state.useLbs
        )
    }

    DisposableEffect(viewModel) {
        OriginIslandLiveManager.onToggleTimerCallback = { viewModel.toggleWorkoutTimer() }
        OriginIslandLiveManager.onSkipRestCallback = { viewModel.stopRestTimer() }
        OriginIslandLiveManager.onAddRest15sCallback = {
            val next = (state.activeWorkout.restTimerRemainingSeconds + 15).coerceAtLeast(15)
            viewModel.startRestTimer(next)
        }
        onDispose { }
    }

    // Request notification permission when a live workout starts so Vivo/iQOO OriginOS 6 Origin Island works when minimized
    LaunchedEffect(state.activeWorkout.isWorkoutActive) {
        if (state.activeWorkout.isWorkoutActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Continuously sync OriginOS 6 Origin Island / Android Promoted Live Activity state (even when minimized)
    LaunchedEffect(
        state.activeWorkout.isWorkoutActive,
        state.activeWorkout.title,
        state.activeWorkout.activeExerciseName,
        state.activeWorkout.activeExerciseElapsedSeconds,
        state.activeWorkout.elapsedSeconds,
        state.activeWorkout.isTimerRunning,
        state.activeWorkout.isRestTimerRunning,
        state.activeWorkout.restTimerRemainingSeconds,
        state.activeWorkout.restTimerTotalSeconds,
        state.activeWorkout.sets,
        state.unitLabel,
        state.useLbs
    ) {
        OriginIslandLiveManager.syncOriginIslandState(
            context = context,
            activeWorkout = state.activeWorkout,
            unitLabel = state.unitLabel,
            useLbs = state.useLbs
        )
    }

    BackHandler(enabled = state.currentTab != AppDestination.SUMMARY) {
        viewModel.selectTab(AppDestination.SUMMARY)
    }

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    val navItems = remember {
        listOf(
            DynamicIslandNavItem(
                route = AppDestination.SUMMARY,
                label = "Summary",
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                accentColor = AppleMoveRed,
                testTag = "nav_summary"
            ),
            DynamicIslandNavItem(
                route = AppDestination.ATTENDANCE,
                label = "Attendance",
                selectedIcon = Icons.Filled.CalendarMonth,
                unselectedIcon = Icons.Outlined.CalendarMonth,
                accentColor = AppleExerciseGreen,
                testTag = "nav_attendance"
            ),
            DynamicIslandNavItem(
                route = AppDestination.WORKOUTS,
                label = "Workouts",
                selectedIcon = Icons.Filled.FitnessCenter,
                unselectedIcon = Icons.Outlined.FitnessCenter,
                accentColor = AppleStandBlue,
                testTag = "nav_workouts"
            ),
            DynamicIslandNavItem(
                route = AppDestination.EXERCISES,
                label = "Exercises",
                selectedIcon = Icons.AutoMirrored.Filled.TrendingUp,
                unselectedIcon = Icons.AutoMirrored.Outlined.TrendingUp,
                accentColor = AppleOverloadOrange,
                testTag = "nav_exercises"
            ),
            DynamicIslandNavItem(
                route = AppDestination.SETTINGS,
                label = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                accentColor = AppleMindPurple,
                testTag = "nav_settings"
            )
        )
    }

    LiquidGlassAmbientBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            ),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Content scrolls freely all the way to the bottom underneath the floating navbar
                AnimatedContent(
                    targetState = state.currentTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "mainTabTransition"
                ) { tab ->
                    when (tab) {
                        AppDestination.SUMMARY -> DashboardScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppDestination.ATTENDANCE -> AttendanceScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppDestination.WORKOUTS -> WorkoutLoggerScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppDestination.EXERCISES -> GoalsAndRecoveryScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppDestination.SETTINGS -> ProfileAndPrivacyScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }

                // Soft ambient bottom scrim to preserve 100% contrast of floating navbar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(124.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Truly floating dynamic island navbar hovering over screen content
                LiquidGlassDynamicIslandBar(
                    items = navItems,
                    selectedRoute = state.currentTab,
                    onSelect = { viewModel.selectTab(it) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Live Dynamic Island Punch-Hole Top Capsule (Vivo Origin Island & Moto Live Updates in-app counterpart)
                val isWorkoutOrRestActive = state.activeWorkout.isWorkoutActive ||
                    (state.activeWorkout.isRestTimerRunning && state.activeWorkout.restTimerRemainingSeconds > 0)
                val isRestingNow = state.activeWorkout.isRestTimerRunning && state.activeWorkout.restTimerRemainingSeconds > 0
                val activeEx = state.activeWorkout.activeExerciseName

                val headline: String
                val subtitle: String
                val timerBadge: String
                val capsuleAccent: Color

                if (isRestingNow) {
                    val restTime = formatClock(state.activeWorkout.restTimerRemainingSeconds)
                    headline = "Rest Timer • $restTime"
                    subtitle = if (!activeEx.isNullOrBlank()) "Next set: $activeEx" else "Rest and recover"
                    timerBadge = "REST $restTime"
                    capsuleAccent = AppleOverloadOrange
                } else if (!activeEx.isNullOrBlank()) {
                    val exTime = formatClock(state.activeWorkout.activeExerciseElapsedSeconds)
                    val completedSets = state.activeWorkout.sets.count { it.status.equals("COMPLETED", ignoreCase = true) }
                    val totalSets = state.activeWorkout.sets.size.coerceAtLeast(1)
                    headline = "$activeEx • $exTime"
                    subtitle = "$completedSets/$totalSets sets done"
                    timerBadge = if (state.activeWorkout.isTimerRunning) "LIVE $exTime" else "PAUSED"
                    capsuleAccent = AppleExerciseGreen
                } else {
                    val totalTime = formatClock(state.activeWorkout.elapsedSeconds)
                    val wTitle = state.activeWorkout.title.ifBlank { "Live Gym Session" }
                    headline = "$wTitle • $totalTime"
                    subtitle = "Tap an exercise to begin tracking"
                    timerBadge = "GYM $totalTime"
                    capsuleAccent = AppleStandBlue
                }

                OriginIslandLiveTopCapsule(
                    isVisible = isWorkoutOrRestActive && state.currentTab != AppDestination.WORKOUTS,
                    isResting = isRestingNow,
                    isTimerRunning = state.activeWorkout.isTimerRunning,
                    headlineText = headline,
                    subtitleText = subtitle,
                    timerBadgeText = timerBadge,
                    accentColor = capsuleAccent,
                    onToggleTimer = { viewModel.toggleWorkoutTimer() },
                    onAddRest15s = {
                        val next = (state.activeWorkout.restTimerRemainingSeconds + 15).coerceAtLeast(15)
                        viewModel.startRestTimer(next)
                    },
                    onSkipRest = { viewModel.stopRestTimer() },
                    onOpenWorkoutLogger = { viewModel.selectTab(AppDestination.WORKOUTS) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hrs = safe / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

