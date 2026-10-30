package com.example

import com.example.data.local.ExerciseEntity
import com.example.data.local.FitnessDatabase
import com.example.data.local.GymAttendanceEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkoutSessionEntity
import com.example.data.local.WorkoutSetEntity
import com.example.data.repository.AnalyticsAndInsightsEngine
import com.example.data.repository.OverloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun trainingVolume_calculatesSumOfWeightTimesRepsForCompletedSetsOnly() {
        val completedSetVolume = FitnessDatabase.calculateSetVolumeKg(
            weightKg = 100f,
            reps = 5,
            status = "COMPLETED"
        )
        val warmupSetVolume = FitnessDatabase.calculateSetVolumeKg(
            weightKg = 60f,
            reps = 10,
            status = "WARMUP"
        )
        val skippedSetVolume = FitnessDatabase.calculateSetVolumeKg(
            weightKg = 100f,
            reps = 5,
            status = "SKIPPED"
        )

        assertEquals(500f, completedSetVolume, 0.01f)
        assertEquals(0f, warmupSetVolume, 0.01f)
        assertEquals(0f, skippedSetVolume, 0.01f)
    }

    @Test
    fun attendanceOverview_calculatesStreaksAndWeeklyGoalAccurately() {
        val now = System.currentTimeMillis()
        val today = AnalyticsAndInsightsEngine.dateKeyForDaysAgo(0, now)
        val yesterday = AnalyticsAndInsightsEngine.dateKeyForDaysAgo(1, now)
        val twoDaysAgo = AnalyticsAndInsightsEngine.dateKeyForDaysAgo(2, now)

        val records = listOf(
            GymAttendanceEntity(id = 1, dateKey = today, checkInTimestampMillis = now),
            GymAttendanceEntity(id = 2, dateKey = yesterday, checkInTimestampMillis = now - 86_400_000L),
            GymAttendanceEntity(id = 3, dateKey = twoDaysAgo, checkInTimestampMillis = now - 172_800_000L)
        )
        val profile = UserProfileEntity(weeklyAttendanceGoal = 4)

        val overview = AnalyticsAndInsightsEngine.buildAttendanceOverview(
            records = records,
            profile = profile,
            nowMillis = now
        )

        assertTrue(overview.attendedToday)
        assertEquals(3, overview.weeklyVisitDays)
        assertEquals(3, overview.currentStreakDays)
        assertEquals(3, overview.longestStreakDays)
        assertEquals(0.75f, overview.weeklyGoalProgress, 0.01f)
    }

    @Test
    fun progressiveOverloadEngine_detectsWeightAndRepImprovementsWithoutFakeData() {
        val exercises = listOf(
            ExerciseEntity(id = 1, name = "Barbell Bench Press", muscleGroup = "Chest", equipment = "Barbell")
        )

        // When 0 sessions exist, status should be INSUFFICIENT_DATA with 0 values
        val emptyReports = AnalyticsAndInsightsEngine.buildExerciseProgressionReports(
            exercises = exercises,
            completedSessions = emptyList(),
            allSets = emptyList(),
            userBodyweightKg = 75f
        )
        assertEquals(1, emptyReports.size)
        assertEquals(OverloadStatus.INSUFFICIENT_DATA, emptyReports.first().overloadStatus)
        assertEquals(0, emptyReports.first().totalSessionsLogged)
        assertFalse(emptyReports.first().personalRecordWeightKg > 0f)

        // When 2 sessions exist with increased top weight, status should detect overload/new PR
        val sessions = listOf(
            WorkoutSessionEntity(
                id = 10L,
                title = "Push Session 1",
                category = "Strength",
                dateKey = "2026-10-20",
                timestampMillis = 1_700_000_000_000L,
                durationMinutes = 45
            ),
            WorkoutSessionEntity(
                id = 11L,
                title = "Push Session 2",
                category = "Strength",
                dateKey = "2026-10-25",
                timestampMillis = 1_700_432_000_000L,
                durationMinutes = 50
            )
        )
        val sets = listOf(
            WorkoutSetEntity(
                id = 1L,
                sessionId = 10L,
                dateKey = "2026-10-20",
                exerciseName = "Barbell Bench Press",
                muscleGroup = "Chest",
                setNumber = 1,
                weightKg = 80f,
                reps = 8,
                status = "COMPLETED"
            ),
            WorkoutSetEntity(
                id = 2L,
                sessionId = 11L,
                dateKey = "2026-10-25",
                exerciseName = "Barbell Bench Press",
                muscleGroup = "Chest",
                setNumber = 1,
                weightKg = 85f,
                reps = 8,
                status = "COMPLETED"
            )
        )

        val reports = AnalyticsAndInsightsEngine.buildExerciseProgressionReports(
            exercises = exercises,
            completedSessions = sessions,
            allSets = sets,
            userBodyweightKg = 75f
        )
        val benchReport = reports.first()
        assertEquals(2, benchReport.totalSessionsLogged)
        assertEquals(85f, benchReport.personalRecordWeightKg, 0.01f)
        assertEquals(5f, benchReport.weightDeltaKg, 0.01f)
        assertEquals(OverloadStatus.NEW_PR, benchReport.overloadStatus)
    }
}
