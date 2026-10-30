package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.viewmodel.ActiveWorkoutEditorState
import org.json.JSONObject
import java.util.Locale

/**
 * Live Dynamic Island & Promoted Live Updates Manager.
 *
 * Fully supports:
 * 1. Motorola Hello UI / My UX "Live Updates" (Dynamic Island / punch-hole widget on Moto Edge 50 series,
 *    Razr 50 / 50 Ultra, Moto G85, ThinkPhone, etc.)
 * 2. Vivo / iQOO OriginOS 5 & OriginOS 6 "Origin Island" (Atomic Island / 原子岛)
 * 3. Standard Android 15/16 Promoted Ongoing Notification live pill system
 *
 * Trigger Points (Active both In-App and when Minimized / Backgrounded):
 * 1. Live Workout Session Started (`isWorkoutActive == true`)
 * 2. Active Exercise Registered & Exercise Chronometer Running (`activeExerciseName != null`)
 * 3. Rest Countdown Timer Running (`isRestTimerRunning == true` && `restTimerRemainingSeconds > 0`)
 *
 * Populates:
 * - Standard Android 15/16 Promoted Ongoing Live Update Notification (`android.requestPromotedOngoing`,
 *   `android.shortCriticalText`, live `Chronometer` & `ProgressBar`).
 * - Motorola Hello UI Live Updates extras (`com.motorola.notifications.*`, `motorola.live_view.*`, `moto.live_update.*`)
 * - Vivo / iQOO OriginOS Origin Island capsule extras (`notification.superx.*` and `AndroidVivoLiveMessage` JSON)
 * - Interactive background BroadcastReceiver actions (`Pause/Resume`, `+15s Rest`, `Skip Rest`) that work
 *   directly from the hardware dynamic island while the app is minimized.
 */
object OriginIslandLiveManager {

    const val CHANNEL_ID = "origin_island_live_workout_channel"
    private const val CHANNEL_NAME = "Live Dynamic Island (Vivo & Motorola Live Updates)"
    const val NOTIFICATION_ID = 2606

    const val ACTION_TOGGLE_TIMER = "com.example.action.ORIGIN_ISLAND_TOGGLE_TIMER"
    const val ACTION_SKIP_REST = "com.example.action.ORIGIN_ISLAND_SKIP_REST"
    const val ACTION_ADD_REST_15S = "com.example.action.ORIGIN_ISLAND_ADD_REST_15S"

    @Volatile
    var onToggleTimerCallback: (() -> Unit)? = null

    @Volatile
    var onSkipRestCallback: (() -> Unit)? = null

    @Volatile
    var onAddRest15sCallback: (() -> Unit)? = null

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time Motorola Live Updates & Vivo Origin Island dynamic punch-hole tracking for workouts and rest timers"
                    setShowBadge(true)
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Synchronizes the current workout & rest timer state with Vivo/iQOO OriginOS 6 Origin Island
     * and Android's Promoted Ongoing Notification system.
     */
    fun syncOriginIslandState(
        context: Context,
        activeWorkout: ActiveWorkoutEditorState,
        unitLabel: String,
        useLbs: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val shouldShowOnOriginIsland = activeWorkout.isWorkoutActive ||
            (activeWorkout.isRestTimerRunning && activeWorkout.restTimerRemainingSeconds > 0)

        if (!shouldShowOnOriginIsland) {
            cancelOriginIsland(context)
            return
        }

        if (!hasNotificationPermission(context)) {
            return
        }

        ensureChannelCreated(context)

        val isResting = activeWorkout.isRestTimerRunning && activeWorkout.restTimerRemainingSeconds > 0
        val activeExercise = activeWorkout.activeExerciseName
        val completedSetsCount = activeWorkout.sets.count { it.status.equals("COMPLETED", ignoreCase = true) }
        val totalSetsCount = activeWorkout.sets.size.coerceAtLeast(1)

        val totalVolumeKg = activeWorkout.sets
            .filter { it.status.equals("COMPLETED", ignoreCase = true) }
            .sumOf { (it.weightKg * it.reps).toDouble() }
            .toFloat()
        val displayVolume = if (useLbs) (totalVolumeKg * 2.20462f).toInt() else totalVolumeKg.toInt()

        val shortChipText: String
        val capsuleTitle: String
        val capsuleSubtitle: String
        val accentHex: String
        val accentColorArgb: Int

        if (isResting) {
            val restFormatted = formatSeconds(activeWorkout.restTimerRemainingSeconds)
            shortChipText = "REST $restFormatted"
            capsuleTitle = "Rest Timer • $restFormatted"
            capsuleSubtitle = if (!activeExercise.isNullOrBlank()) {
                "Next: $activeExercise ($completedSetsCount/$totalSetsCount sets)"
            } else {
                "Recovering • $displayVolume $unitLabel logged"
            }
            accentHex = "#FF9F0A"
            accentColorArgb = 0xFFFF9F0A.toInt()
        } else if (!activeExercise.isNullOrBlank()) {
            val exSeconds = activeWorkout.activeExerciseElapsedSeconds
            val exFormatted = formatSeconds(exSeconds)
            shortChipText = if (activeWorkout.isTimerRunning) "LIVE $exFormatted" else "PAUSED $exFormatted"
            capsuleTitle = "$activeExercise • $exFormatted"
            capsuleSubtitle = "$completedSetsCount/$totalSetsCount sets done • $displayVolume $unitLabel"
            accentHex = "#30D158"
            accentColorArgb = 0xFF30D158.toInt()
        } else {
            val sessionFormatted = formatSeconds(activeWorkout.elapsedSeconds)
            val title = activeWorkout.title.ifBlank { "Live Gym Session" }
            shortChipText = "GYM $sessionFormatted"
            capsuleTitle = "$title • $sessionFormatted"
            capsuleSubtitle = "Tap an exercise to start tracking ($completedSetsCount/$totalSetsCount sets)"
            accentHex = "#0A84FF"
            accentColorArgb = 0xFF0A84FF.toInt()
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleTimerPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            Intent(context, OriginIslandActionReceiver::class.java).apply {
                action = ACTION_TOGGLE_TIMER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipRestPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, OriginIslandActionReceiver::class.java).apply {
                action = ACTION_SKIP_REST
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add15sPendingIntent = PendingIntent.getBroadcast(
            context,
            103,
            Intent(context, OriginIslandActionReceiver::class.java).apply {
                action = ACTION_ADD_REST_15S
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = System.currentTimeMillis()
        val chronometerBaseMillis = if (isResting) {
            now + (activeWorkout.restTimerRemainingSeconds * 1000L)
        } else {
            val elapsed = if (!activeExercise.isNullOrBlank()) {
                activeWorkout.activeExerciseElapsedSeconds
            } else {
                activeWorkout.elapsedSeconds
            }
            now - (elapsed * 1000L)
        }

        // Construct Vivo / iQOO OriginOS 6 Origin Island / Atomic Island payload extras
        val vivoLiveMessageJson = runCatching {
            JSONObject().apply {
                put("operation", 0) // 0 = Create / Update live capsule
                put("scene", "HEALTH_REGISTER")
                put("templateType", 1)
                put("showNotify", true)
                put("changeRecord", now)
                put(
                    "capsuleData",
                    JSONObject().apply {
                        put("bgColor", accentHex)
                        put("title", shortChipText)
                        put("content", capsuleTitle)
                        put("subContent", capsuleSubtitle)
                        put("status", if (activeWorkout.isTimerRunning || isResting) "RUNNING" else "PAUSED")
                        put("isChronometer", activeWorkout.isTimerRunning || isResting)
                        put("baseTime", chronometerBaseMillis)
                        put("countDown", isResting)
                    }
                )
            }.toString()
        }.getOrDefault("{}")

        val totalRest = activeWorkout.restTimerTotalSeconds.coerceAtLeast(1)
        val completedRest = (totalRest - activeWorkout.restTimerRemainingSeconds).coerceIn(0, totalRest)

        val originExtras = Bundle().apply {
            // Android 15/16 Promoted Ongoing Live Updates (also used by OriginOS 6 Origin Island & Moto Hello UI)
            putBoolean("android.requestPromotedOngoing", true)
            putString("android.shortCriticalText", shortChipText)
            putLong("android.chronometerBase", chronometerBaseMillis)
            putBoolean("android.chronometerCountDown", isResting)

            // Motorola Hello UI / My UX Live Updates (Dynamic Island on Moto Edge 50 series, Razr 50 / 50 Ultra, Moto G85, ThinkPhone)
            putBoolean("com.motorola.notifications.live_update", true)
            putBoolean("com.motorola.notifications.live_view", true)
            putString("com.motorola.notifications.capsule_text", shortChipText)
            putString("com.motorola.notifications.pill_title", capsuleTitle)
            putString("com.motorola.notifications.pill_subtitle", capsuleSubtitle)
            putInt("com.motorola.notifications.accent_color", accentColorArgb)
            putString("com.motorola.notifications.state", if (activeWorkout.isTimerRunning || isResting) "ACTIVE" else "PAUSED")
            putString("com.motorola.notifications.category", if (isResting) "TIMER" else "WORKOUT")
            putLong("com.motorola.notifications.chronometer_base", chronometerBaseMillis)
            putBoolean("com.motorola.notifications.is_countdown", isResting)
            putBoolean("motorola.live_view.enable", true)
            putString("motorola.live_view.chip_text", shortChipText)
            putString("motorola.live_view.title", capsuleTitle)
            putString("motorola.live_view.type", if (isResting) "TIMER" else "WORKOUT")
            putString("motorola.live_view.state", if (activeWorkout.isTimerRunning || isResting) "RUNNING" else "PAUSED")
            putBoolean("moto.live_update.active", true)
            putString("moto.live_update.pill_text", shortChipText)
            putInt("moto.live_update.progress", if (isResting) completedRest else completedSetsCount)
            putInt("moto.live_update.progress_max", if (isResting) totalRest else totalSetsCount)
            putInt("moto.live_update.color", accentColorArgb)
            putInt("moto.live_update.icon", R.mipmap.ic_launcher)

            // Vivo / iQOO OriginOS Atomic Island / Origin Island local notification bridge extras
            putBoolean("notification.superx.enable", true)
            putInt("notification.superx.operation", 0)
            putString("notification.superx.scene", "HEALTH_REGISTER")
            putString("notification.superx.capsuleTitle", shortChipText)
            putString("notification.superx.capsuleContent", capsuleTitle)
            putString("notification.superx.capsuleBgColor", accentHex)
            putBoolean("notification.superx.isChronometer", activeWorkout.isTimerRunning || isResting)
            putLong("notification.superx.baseTime", chronometerBaseMillis)
            putBoolean("notification.superx.countDown", isResting)
            putString("AndroidVivoLiveMessage", vivoLiveMessageJson)
        }

        val notifCategory = if (isResting) NotificationCompat.CATEGORY_STOPWATCH else NotificationCompat.CATEGORY_WORKOUT

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(capsuleTitle)
            .setContentText(capsuleSubtitle)
            .setSubText(shortChipText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(notifCategory)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(accentColorArgb)
            .setColorized(false)
            .setWhen(chronometerBaseMillis)
            .setUsesChronometer(activeWorkout.isTimerRunning || isResting)
            .setChronometerCountDown(isResting)
            .addExtras(originExtras)

        if (isResting) {
            builder.setProgress(totalRest, completedRest, false)
            builder.addAction(
                0,
                "+15s Rest",
                add15sPendingIntent
            )
            builder.addAction(
                0,
                "Skip Rest",
                skipRestPendingIntent
            )
        } else {
            builder.setProgress(totalSetsCount, completedSetsCount, false)
            builder.addAction(
                0,
                if (activeWorkout.isTimerRunning) "Pause Timer" else "Resume Timer",
                toggleTimerPendingIntent
            )
        }

        runCatching {
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelOriginIsland(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            manager.cancel(NOTIFICATION_ID)
        }
    }

    fun formatSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

/**
 * BroadcastReceiver allowing the user to Pause/Resume the workout timer, add +15s rest, or Skip Rest
 * directly from the OriginOS 6 Origin Island capsule / Ongoing Notification while the app is minimized.
 */
class OriginIslandActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            OriginIslandLiveManager.ACTION_TOGGLE_TIMER -> {
                OriginIslandLiveManager.onToggleTimerCallback?.invoke()
            }
            OriginIslandLiveManager.ACTION_SKIP_REST -> {
                OriginIslandLiveManager.onSkipRestCallback?.invoke()
            }
            OriginIslandLiveManager.ACTION_ADD_REST_15S -> {
                OriginIslandLiveManager.onAddRest15sCallback?.invoke()
            }
        }
    }
}
