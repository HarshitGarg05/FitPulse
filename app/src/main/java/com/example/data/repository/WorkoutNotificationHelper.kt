package com.example.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object WorkoutNotificationHelper {
    private const val CHANNEL_ID_TIMERS = "fitpulse_timers_channel"
    private const val CHANNEL_ID_REMINDERS = "fitpulse_reminders_channel"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMERS,
                "Workout & Rest Timers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your rest timer between sets finishes."
            }
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Training Reminders & Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scheduled workout reminders and streak achievements."
            }
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(reminderChannel)
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

    fun sendNotificationIfPermitted(
        context: Context,
        title: String,
        body: String,
        isTimerAlert: Boolean = false
    ): Boolean {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return false
        return try {
            val channel = if (isTimerAlert) CHANNEL_ID_TIMERS else CHANNEL_ID_REMINDERS
            val notification = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(
                    if (isTimerAlert) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify((1000..9999).random(), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
