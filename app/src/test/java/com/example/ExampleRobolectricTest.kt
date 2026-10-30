package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExerciseEntity
import com.example.data.local.GymAttendanceEntity
import com.example.data.local.UserProfileEntity
import com.example.data.repository.AnalyticsAndInsightsEngine
import com.example.data.repository.AnalyticsDateRange
import com.example.data.repository.BackupAndRestoreManager
import com.example.data.repository.CalendarAttendanceStatus
import com.example.data.repository.PdfReportGenerator
import com.example.ui.viewmodel.ActiveWorkoutEditorState
import com.example.util.OriginIslandLiveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun mainActivity_launchesSuccessfullyAndGeneratesFormattedPdfReport() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        assertNotNull(activity)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val profile = UserProfileEntity(displayName = "Test Athlete", preferredWeightUnit = "kg")
        val attendanceOverview = AnalyticsAndInsightsEngine.buildAttendanceOverview(
            records = listOf(GymAttendanceEntity(dateKey = "2026-10-29")),
            profile = profile
        )
        // Verify calendar only uses ATTENDED or REST_OR_UNLOGGED (no PLANNED days)
        assertTrue(
            attendanceOverview.calendarDays.all {
                it.status == CalendarAttendanceStatus.ATTENDED ||
                    it.status == CalendarAttendanceStatus.REST_OR_UNLOGGED
            }
        )

        val testExercise = ExerciseEntity(
            id = 1L,
            name = "Barbell Bench Press",
            muscleGroup = "Chest",
            equipment = "Barbell"
        )
        val analytics = AnalyticsAndInsightsEngine.buildDashboardAnalytics(
            dateRange = AnalyticsDateRange.YEAR_1,
            attendanceRecords = emptyList(),
            exercises = listOf(testExercise),
            completedSessions = emptyList(),
            allSets = emptyList(),
            profile = profile
        )

        // Verify unlogged exercise has 0 prefilled target weight/reps/1RM
        val report = analytics.overloadReports.first()
        assertEquals(0, report.totalSessionsLogged)
        assertEquals(0f, report.recommendedStartWeightKg, 0.001f)
        assertEquals(0, report.recommendedTargetReps)
        assertEquals(0f, report.recommendedTargetOneRepMaxKg, 0.001f)

        // Verify BackupAndRestoreManager naming and OriginOS 6 Origin Island Live Manager sync
        val backupFileName = BackupAndRestoreManager.defaultExportFileName(1761720000000L)
        assertTrue(backupFileName.startsWith("FitPulse_Backup_") && backupFileName.endsWith(".json"))

        OriginIslandLiveManager.syncOriginIslandState(
            context = context,
            activeWorkout = ActiveWorkoutEditorState(
                isWorkoutActive = true,
                title = "Push Hypertrophy",
                activeExerciseName = "Barbell Bench Press",
                elapsedSeconds = 125,
                isTimerRunning = true
            ),
            unitLabel = "kg",
            useLbs = false
        )

        val generatedPdf = PdfReportGenerator.generateFormattedPdfReport(
            context = context,
            profile = profile,
            attendanceOverview = attendanceOverview,
            attendanceRecords = emptyList(),
            dashboardAnalytics = analytics,
            completedSessions = emptyList(),
            allSets = emptyList()
        )

        assertTrue(generatedPdf.file.exists())
        assertTrue(generatedPdf.file.length() > 0L)
        assertTrue(generatedPdf.pageCount >= 1)
    }
}

