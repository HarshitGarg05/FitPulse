package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Athlete",
    val preferredWeightUnit: String = "kg", // "kg" or "lbs"
    val weightKg: Float = 75f,
    val heightCm: Float = 175f,
    val fitnessLevel: String = "Intermediate",
    val primaryObjective: String = "Strength & Progressive Overload",
    val weeklyAttendanceGoal: Int = 4,
    val trainingDaysCsv: String = "",
    val allowMultipleVisitsPerDay: Boolean = false,
    val defaultRestSeconds: Int = 90,
    val overloadIncrementKg: Float = 2.5f,
    val barbellWeightKg: Float = 20f,
    val autoStartRestTimer: Boolean = true,
    val autoCheckInOnWorkoutComplete: Boolean = true,
    val preferredThemeMode: String = "LIGHT",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "gym_attendance",
    indices = [Index(value = ["dateKey"])]
)
data class GymAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateKey: String, // "YYYY-MM-DD"
    val checkInTimestampMillis: Long = System.currentTimeMillis(),
    val note: String = "Gym Session",
    val durationMinutes: Int = 60
)

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["muscleGroup"]), Index(value = ["isArchived"])]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val muscleGroup: String,
    val secondaryMuscles: String = "",
    val equipment: String = "Barbell",
    val category: String = "Strength",
    val instructions: String = "",
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val isBodyweight: Boolean = false,
    val isAssisted: Boolean = false
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val category: String = "Strength",
    val targetDurationMin: Int = 60,
    val exercisesCsv: String,
    val isCustom: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["dateKey"]), Index(value = ["isDraft"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val category: String = "Strength",
    val dateKey: String = "", // "YYYY-MM-DD"
    val timestampMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 45,
    val totalVolumeKg: Float = 0f,
    val totalSets: Int = 0,
    val exerciseCount: Int = 0,
    val caloriesBurned: Int = 0,
    val notes: String = "",
    val prCount: Int = 0,
    val isDraft: Boolean = false
)

@Entity(
    tableName = "workout_sets",
    indices = [Index(value = ["sessionId"]), Index(value = ["exerciseName"]), Index(value = ["dateKey"])]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val dateKey: String = "",
    val exerciseName: String,
    val muscleGroup: String,
    val exerciseOrder: Int = 0,
    val setOrder: Int = 0,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val durationSeconds: Int = 0,
    val distanceKm: Float = 0f,
    val restSeconds: Int = 90,
    val rpe: Float = 8.0f,
    val setType: String = "NORMAL",
    val status: String = "COMPLETED", // COMPLETED, WARMUP, SKIPPED
    val isPersonalRecord: Boolean = false,
    val isBodyweight: Boolean = false,
    val isAssisted: Boolean = false,
    val notes: String = ""
)
