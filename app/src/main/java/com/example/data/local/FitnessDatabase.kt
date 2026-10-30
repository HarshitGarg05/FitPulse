package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        GymAttendanceEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class
    ],
    version = FitnessDatabase.CURRENT_SCHEMA_VERSION,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao

    companion object {
        const val CURRENT_SCHEMA_VERSION = 7

        @Volatile
        private var INSTANCE: FitnessDatabase? = null

        /**
         * Inspects `PRAGMA table_info(tableName)` before executing `ALTER TABLE ADD COLUMN`
         * so every Room migration step is 100% idempotent and never fails on existing columns.
         */
        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinitionSql: String
        ) {
            var columnExists = false
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                        columnExists = true
                        break
                    }
                }
            }
            if (!columnExists) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinitionSql")
            }
        }

        /**
         * Ensures all 6 core tables and their indices exist with their complete schema definitions.
         */
        private fun ensureAllTablesAndIndicesExist(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `user_profile` (
                    `id` INTEGER NOT NULL,
                    `displayName` TEXT NOT NULL DEFAULT 'Athlete',
                    `preferredWeightUnit` TEXT NOT NULL DEFAULT 'kg',
                    `weightKg` REAL NOT NULL DEFAULT 75.0,
                    `heightCm` REAL NOT NULL DEFAULT 175.0,
                    `fitnessLevel` TEXT NOT NULL DEFAULT 'Intermediate',
                    `primaryObjective` TEXT NOT NULL DEFAULT 'Strength & Progressive Overload',
                    `weeklyAttendanceGoal` INTEGER NOT NULL DEFAULT 4,
                    `trainingDaysCsv` TEXT NOT NULL DEFAULT '',
                    `allowMultipleVisitsPerDay` INTEGER NOT NULL DEFAULT 0,
                    `defaultRestSeconds` INTEGER NOT NULL DEFAULT 90,
                    `overloadIncrementKg` REAL NOT NULL DEFAULT 2.5,
                    `barbellWeightKg` REAL NOT NULL DEFAULT 20.0,
                    `autoStartRestTimer` INTEGER NOT NULL DEFAULT 1,
                    `autoCheckInOnWorkoutComplete` INTEGER NOT NULL DEFAULT 1,
                    `preferredThemeMode` TEXT NOT NULL DEFAULT 'LIGHT',
                    `lastUpdatedMillis` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gym_attendance` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateKey` TEXT NOT NULL,
                    `checkInTimestampMillis` INTEGER NOT NULL DEFAULT 0,
                    `note` TEXT NOT NULL DEFAULT 'Gym Session',
                    `durationMinutes` INTEGER NOT NULL DEFAULT 60
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercises` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `muscleGroup` TEXT NOT NULL,
                    `secondaryMuscles` TEXT NOT NULL DEFAULT '',
                    `equipment` TEXT NOT NULL DEFAULT 'Barbell',
                    `category` TEXT NOT NULL DEFAULT 'Strength',
                    `instructions` TEXT NOT NULL DEFAULT '',
                    `isCustom` INTEGER NOT NULL DEFAULT 0,
                    `isArchived` INTEGER NOT NULL DEFAULT 0,
                    `isBodyweight` INTEGER NOT NULL DEFAULT 0,
                    `isAssisted` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_templates` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL DEFAULT 'Strength',
                    `targetDurationMin` INTEGER NOT NULL DEFAULT 60,
                    `exercisesCsv` TEXT NOT NULL DEFAULT '',
                    `isCustom` INTEGER NOT NULL DEFAULT 1,
                    `createdAtMillis` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL DEFAULT 'Strength',
                    `dateKey` TEXT NOT NULL DEFAULT '',
                    `timestampMillis` INTEGER NOT NULL DEFAULT 0,
                    `durationMinutes` INTEGER NOT NULL DEFAULT 45,
                    `totalVolumeKg` REAL NOT NULL DEFAULT 0.0,
                    `totalSets` INTEGER NOT NULL DEFAULT 0,
                    `exerciseCount` INTEGER NOT NULL DEFAULT 0,
                    `caloriesBurned` INTEGER NOT NULL DEFAULT 0,
                    `notes` TEXT NOT NULL DEFAULT '',
                    `prCount` INTEGER NOT NULL DEFAULT 0,
                    `isDraft` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `dateKey` TEXT NOT NULL DEFAULT '',
                    `exerciseName` TEXT NOT NULL,
                    `muscleGroup` TEXT NOT NULL,
                    `exerciseOrder` INTEGER NOT NULL DEFAULT 0,
                    `setOrder` INTEGER NOT NULL DEFAULT 0,
                    `setNumber` INTEGER NOT NULL,
                    `weightKg` REAL NOT NULL,
                    `reps` INTEGER NOT NULL,
                    `durationSeconds` INTEGER NOT NULL DEFAULT 0,
                    `distanceKm` REAL NOT NULL DEFAULT 0.0,
                    `restSeconds` INTEGER NOT NULL DEFAULT 90,
                    `rpe` REAL NOT NULL DEFAULT 8.0,
                    `setType` TEXT NOT NULL DEFAULT 'NORMAL',
                    `status` TEXT NOT NULL DEFAULT 'COMPLETED',
                    `isPersonalRecord` INTEGER NOT NULL DEFAULT 0,
                    `isBodyweight` INTEGER NOT NULL DEFAULT 0,
                    `isAssisted` INTEGER NOT NULL DEFAULT 0,
                    `notes` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )

            // Ensure all Room indices exist matching @Entity declarations
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_attendance_dateKey` ON `gym_attendance` (`dateKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_muscleGroup` ON `exercises` (`muscleGroup`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_isArchived` ON `exercises` (`isArchived`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_dateKey` ON `workout_sessions` (`dateKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_isDraft` ON `workout_sessions` (`isDraft`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionId` ON `workout_sets` (`sessionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseName` ON `workout_sets` (`exerciseName`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_dateKey` ON `workout_sets` (`dateKey`)")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndIndicesExist(db)
                addColumnIfMissing(db, "exercises", "secondaryMuscles", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "exercises", "equipment", "TEXT NOT NULL DEFAULT 'Barbell'")
                addColumnIfMissing(db, "exercises", "instructions", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "exercises", "isArchived", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndIndicesExist(db)
                addColumnIfMissing(db, "exercises", "isBodyweight", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "exercises", "isAssisted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "isBodyweight", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "isAssisted", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndIndicesExist(db)
                addColumnIfMissing(db, "user_profile", "defaultRestSeconds", "INTEGER NOT NULL DEFAULT 90")
                addColumnIfMissing(db, "user_profile", "overloadIncrementKg", "REAL NOT NULL DEFAULT 2.5")
                addColumnIfMissing(db, "user_profile", "barbellWeightKg", "REAL NOT NULL DEFAULT 20.0")
                addColumnIfMissing(db, "user_profile", "autoStartRestTimer", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "user_profile", "autoCheckInOnWorkoutComplete", "INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndIndicesExist(db)
                addColumnIfMissing(db, "workout_sessions", "dateKey", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "workout_sessions", "exerciseCount", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sessions", "prCount", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sessions", "isDraft", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "dateKey", "TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "workout_sets", "exerciseOrder", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "setOrder", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "durationSeconds", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "distanceKm", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(db, "workout_sets", "restSeconds", "INTEGER NOT NULL DEFAULT 90")
                addColumnIfMissing(db, "workout_sets", "rpe", "REAL NOT NULL DEFAULT 8.0")
                addColumnIfMissing(db, "workout_sets", "setType", "TEXT NOT NULL DEFAULT 'NORMAL'")
                addColumnIfMissing(db, "workout_sets", "status", "TEXT NOT NULL DEFAULT 'COMPLETED'")
                addColumnIfMissing(db, "workout_sets", "isPersonalRecord", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_sets", "notes", "TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndIndicesExist(db)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureAllTablesAndIndicesExist(db)
                addColumnIfMissing(db, "user_profile", "preferredThemeMode", "TEXT NOT NULL DEFAULT 'LIGHT'")
                addColumnIfMissing(db, "user_profile", "lastUpdatedMillis", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "workout_templates", "createdAtMillis", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7
        )

        fun getInstance(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitpulse_apple_health.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Epley 1-Rep Max estimation formula: Weight * (1 + Reps / 30).
         */
        fun calculateEpleyOneRepMax(weightKg: Float, reps: Int): Float {
            if (weightKg <= 0f || reps <= 0) return 0f
            if (reps == 1) return weightKg
            val cappedReps = reps.coerceAtMost(20)
            return weightKg * (1f + cappedReps / 30f)
        }

        /**
         * Training Volume = Weight * Repetitions across completed valid sets.
         * Handles bodyweight and assisted exercises cleanly.
         */
        fun calculateSetVolumeKg(
            weightKg: Float,
            reps: Int,
            status: String = "COMPLETED",
            isBodyweight: Boolean = false,
            isAssisted: Boolean = false,
            userBodyweightKg: Float = 75f
        ): Float {
            if (status != "COMPLETED" || reps <= 0) return 0f
            val effectiveWeight = when {
                isAssisted -> (userBodyweightKg - weightKg.coerceAtLeast(0f)).coerceAtLeast(5f)
                isBodyweight && weightKg <= 0f -> userBodyweightKg.coerceAtLeast(0f)
                else -> weightKg.coerceAtLeast(0f)
            }
            return effectiveWeight * reps
        }

        /**
         * Seeds ONLY the default user profile and comprehensive reference exercise catalog.
         * Zero demo routines, zero prefilled planned days, zero fake workouts, and zero fake attendance.
         */
        suspend fun ensureInitialized(dao: FitnessDao) {
            if (dao.getUserProfileSnapshot() == null) {
                dao.upsertUserProfile(
                    UserProfileEntity(
                        id = 1,
                        displayName = "Athlete",
                        preferredWeightUnit = "kg",
                        weightKg = 75f,
                        heightCm = 175f,
                        fitnessLevel = "Intermediate",
                        primaryObjective = "Strength & Progressive Overload",
                        weeklyAttendanceGoal = 4,
                        trainingDaysCsv = "",
                        allowMultipleVisitsPerDay = false
                    )
                )
            }

            if (dao.getExerciseCount() == 0) {
                val catalog = listOf(
                    // ==================== CHEST ====================
                    ExerciseEntity(
                        name = "Barbell Bench Press",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Triceps, Front Delts",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Retract shoulder blades, lower bar to mid-chest under control, and press to full lockout."
                    ),
                    ExerciseEntity(
                        name = "Incline Barbell Bench Press",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Upper Chest, Front Delts, Triceps",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Set bench to 30 degrees, lower bar to clavicular chest, and press vertically."
                    ),
                    ExerciseEntity(
                        name = "Flat Dumbbell Bench Press",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Triceps, Front Delts",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Lower dumbbells with a deep stretch across the pecs and press up without clanking weights."
                    ),
                    ExerciseEntity(
                        name = "Incline Dumbbell Press",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Upper Chest, Triceps",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Set bench to 30 degrees. Lower dumbbells with controlled tempo and press up."
                    ),
                    ExerciseEntity(
                        name = "Decline Barbell Bench Press",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Lower Chest, Triceps",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Secure feet, lower bar to lower sternum, and press up with elbows tucked at 45 degrees."
                    ),
                    ExerciseEntity(
                        name = "Cable Chest Fly (High-to-Low)",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Lower & Inner Chest, Front Delts",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Step forward for tension, maintain slight elbow bend, and bring handles together in a wide arc."
                    ),
                    ExerciseEntity(
                        name = "Low-to-High Cable Chest Fly",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Upper Chest, Front Delts",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Sweep cables upward and inward to chest height, squeezing the upper pectorals."
                    ),
                    ExerciseEntity(
                        name = "Pec Deck / Machine Chest Fly",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Front Delts",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Keep shoulder blades pinned to pad and bring arms together with a 1-second peak contraction."
                    ),
                    ExerciseEntity(
                        name = "Seated Machine Chest Press",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Triceps, Front Delts",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Adjust seat so handles align with mid-chest and press smoothly to full extension."
                    ),
                    ExerciseEntity(
                        name = "Weighted Chest Dips",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Triceps, Front Delts",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Lean torso slightly forward on parallel bars, lower until shoulders are below elbows, and press up.",
                        isBodyweight = true
                    ),
                    ExerciseEntity(
                        name = "Push-Ups",
                        muscleGroup = "Chest",
                        secondaryMuscles = "Triceps, Core, Front Delts",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Maintain rigid plank position, lower chest to floor, and press up.",
                        isBodyweight = true
                    ),

                    // ==================== BACK ====================
                    ExerciseEntity(
                        name = "Conventional Deadlift",
                        muscleGroup = "Back",
                        secondaryMuscles = "Hamstrings, Glutes, Traps, Forearms",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Engage lats, brace core hard, maintain neutral spine, and drive the floor away."
                    ),
                    ExerciseEntity(
                        name = "Sumo Deadlift",
                        muscleGroup = "Back",
                        secondaryMuscles = "Glutes, Adductors, Quads, Traps",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Take a wide stance with toes pointed out, grip inside knees, keep chest tall, and lock hips out."
                    ),
                    ExerciseEntity(
                        name = "Bent-Over Barbell Row",
                        muscleGroup = "Back",
                        secondaryMuscles = "Rhomboids, Biceps, Rear Delts",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Hinge torso to 45 degrees and row bar toward lower ribs."
                    ),
                    ExerciseEntity(
                        name = "Pendlay Barbell Row",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lats, Upper Back, Biceps",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Keep torso parallel to floor, pull bar explosively from dead stop on floor to lower chest."
                    ),
                    ExerciseEntity(
                        name = "One-Arm Dumbbell Row",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lats, Rhomboids, Biceps",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Brace non-working hand on bench, pull dumbbell back toward hip in a sweeping arc."
                    ),
                    ExerciseEntity(
                        name = "Chest-Supported T-Bar Row",
                        muscleGroup = "Back",
                        secondaryMuscles = "Mid-Back, Lats, Rear Delts",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Keep chest glued to pad, protract at bottom for full stretch, and retract scapulae at top."
                    ),
                    ExerciseEntity(
                        name = "Wide-Grip Cable Lat Pulldown",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lats, Tera Major, Biceps",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Pull bar down toward upper chest while driving elbows down and squeezing lats."
                    ),
                    ExerciseEntity(
                        name = "Close-Grip Neutral Lat Pulldown",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lower Lats, Biceps",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Use V-bar handle, lean back slightly, and pull handle to sternum."
                    ),
                    ExerciseEntity(
                        name = "Seated Cable Row",
                        muscleGroup = "Back",
                        secondaryMuscles = "Mid-Back, Lats, Biceps",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Sit tall with knees slightly bent, row handle into navel, and squeeze shoulder blades."
                    ),
                    ExerciseEntity(
                        name = "Straight-Arm Cable Lat Pulldown",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lats, Teres Major, Triceps Long Head",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Keep arms nearly straight and sweep bar down in an arc to thighs using pure lat contraction."
                    ),
                    ExerciseEntity(
                        name = "Strict Pull-Ups",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lats, Biceps, Core",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Pull chin above bar from dead hang. Enter extra belt weight if weighted.",
                        isBodyweight = true
                    ),
                    ExerciseEntity(
                        name = "Chin-Ups (Supinated Grip)",
                        muscleGroup = "Back",
                        secondaryMuscles = "Biceps, Lats",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Grip bar palms-facing-you shoulder-width apart and pull chest to bar.",
                        isBodyweight = true
                    ),
                    ExerciseEntity(
                        name = "Assisted Pull-Up Machine",
                        muscleGroup = "Back",
                        secondaryMuscles = "Lats, Biceps",
                        equipment = "Machine",
                        category = "Assisted",
                        instructions = "Use knee pad assistance; lower assistance weight indicates strength progression.",
                        isAssisted = true
                    ),
                    ExerciseEntity(
                        name = "Dumbbell Shrugs",
                        muscleGroup = "Back",
                        secondaryMuscles = "Upper Traps, Forearms",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Elevate shoulders straight toward ears, pause 1 second at top, and lower slowly."
                    ),

                    // ==================== LEGS (QUADS, HAMSTRINGS, GLUTES, CALVES) ====================
                    ExerciseEntity(
                        name = "Barbell Back Squat",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Glutes, Quads, Core",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Brace core, sit hips back and down until thighs reach parallel or below, drive up."
                    ),
                    ExerciseEntity(
                        name = "Barbell Front Squat",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Quads, Upper Back, Core",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Rack bar across front deltoids with high elbows, keep torso upright, and squat deep."
                    ),
                    ExerciseEntity(
                        name = "Romanian Deadlift (RDL)",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Hamstrings, Glutes, Lower Back",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Keep slight knee bend, hinge hips backward while keeping bar close to shins until hamstring stretch."
                    ),
                    ExerciseEntity(
                        name = "Dumbbell Romanian Deadlift",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Hamstrings, Glutes",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Track dumbbells along thighs and shins while pushing hips straight back."
                    ),
                    ExerciseEntity(
                        name = "Barbell Hip Thrust",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Glutes, Hamstrings",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Rest upper back on bench, drive through heels to full hip extension, and squeeze glutes at top."
                    ),
                    ExerciseEntity(
                        name = "Machine Leg Press",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Quads, Glutes",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Lower sled under control to 90-degree knee bend while keeping lower back flat."
                    ),
                    ExerciseEntity(
                        name = "Hack Squat Machine",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Quads, Glutes",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Keep back flat against pad, descend to full knee flexion for quad bias, and drive up."
                    ),
                    ExerciseEntity(
                        name = "Dumbbell Bulgarian Split Squat",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Quads, Glutes, Stabilizers",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Elevate rear foot on bench, lower back knee toward floor under control, and drive through front foot."
                    ),
                    ExerciseEntity(
                        name = "Dumbbell Walking Lunges",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Quads, Glutes, Hamstrings",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Take controlled strides forward, lowering rear knee close to floor before stepping through."
                    ),
                    ExerciseEntity(
                        name = "Seated Leg Extension",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Quadriceps (Rectus Femoris & Vastus)",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Extend knees fully against pad, pause 1 second at top contraction, and lower slowly."
                    ),
                    ExerciseEntity(
                        name = "Lying Leg Curl",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Hamstrings, Calves",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Keep hips pressed into pad and curl roller toward glutes without lifting pelvis."
                    ),
                    ExerciseEntity(
                        name = "Seated Hamstring Leg Curl",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Hamstrings",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Lock thigh pad snug over knees and curl lower legs back through full range."
                    ),
                    ExerciseEntity(
                        name = "Standing Calf Raise",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Gastrocnemius, Soleus",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Lower heels for a deep 2-second stretch at bottom, then drive up onto balls of feet."
                    ),
                    ExerciseEntity(
                        name = "Seated Calf Raise",
                        muscleGroup = "Legs",
                        secondaryMuscles = "Soleus",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Perform full range ankle plantarflexion with a controlled pause at both top and bottom."
                    ),

                    // ==================== SHOULDERS ====================
                    ExerciseEntity(
                        name = "Overhead Barbell Press (OHP)",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Triceps, Upper Chest, Core",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Brace core and glutes tightly, press bar vertically from collarbone to overhead lockout."
                    ),
                    ExerciseEntity(
                        name = "Seated Dumbbell Shoulder Press",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Front & Medial Delts, Triceps",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Sit against upright bench, lower dumbbells to ear level, and press overhead."
                    ),
                    ExerciseEntity(
                        name = "Arnold Dumbbell Press",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Front & Medial Delts, Triceps",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Start palms facing you at chin level and rotate wrists outward while pressing overhead."
                    ),
                    ExerciseEntity(
                        name = "Dumbbell Lateral Raise",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Medial Delts",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Raise dumbbells out to sides in scapular plane to shoulder height with slight elbow bend."
                    ),
                    ExerciseEntity(
                        name = "Cable Single-Arm Lateral Raise",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Medial Delts",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Set pulley to wrist height and sweep arm out to side for constant tension across the deltoid."
                    ),
                    ExerciseEntity(
                        name = "Cable Rope Face Pull",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Rear Delts, Rotator Cuff, Mid-Traps",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Pull rope toward forehead while externally rotating shoulders so knuckles point backward."
                    ),
                    ExerciseEntity(
                        name = "Reverse Pec Deck (Rear Delt Fly)",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Rear Delts, Rhomboids",
                        equipment = "Machine",
                        category = "Strength",
                        instructions = "Sweep arms straight back in a wide arc focusing on posterior deltoids without shrugging."
                    ),
                    ExerciseEntity(
                        name = "Bent-Over Dumbbell Rear Delt Raise",
                        muscleGroup = "Shoulders",
                        secondaryMuscles = "Rear Delts, Upper Back",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Hinge torso nearly parallel to floor and raise dumbbells out to sides with neutral or pronated grip."
                    ),

                    // ==================== ARMS (BICEPS, TRICEPS, FOREARMS) ====================
                    ExerciseEntity(
                        name = "Barbell / EZ-Bar Bicep Curl",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Biceps Brachii, Forearms",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Keep elbows tucked at sides and curl bar through full range without torso swing."
                    ),
                    ExerciseEntity(
                        name = "Incline Dumbbell Bicep Curl",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Biceps Long Head",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Lay back on 45-60 degree incline bench, let arms hang behind torso for deep stretch, and curl."
                    ),
                    ExerciseEntity(
                        name = "Dumbbell Hammer Curl",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Brachialis, Brachioradialis, Biceps",
                        equipment = "Dumbbell",
                        category = "Strength",
                        instructions = "Maintain neutral palms-facing-in grip throughout the entire curl."
                    ),
                    ExerciseEntity(
                        name = "Preacher Curl (EZ-Bar or Machine)",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Biceps Short Head",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Keep triceps firmly supported on preacher pad, lower under strict control, and curl up."
                    ),
                    ExerciseEntity(
                        name = "Bayesian Cable Bicep Curl",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Biceps Long Head",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Face away from low pulley with hand behind hip and curl forward while keeping elbow stationary."
                    ),
                    ExerciseEntity(
                        name = "Close-Grip Barbell Bench Press",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Triceps, Inner Chest, Front Delts",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Grip bar shoulder-width apart, keep elbows tucked close to ribs, and press to lockout."
                    ),
                    ExerciseEntity(
                        name = "EZ-Bar Skullcrushers (Lying Tricep Extension)",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Triceps Long & Medial Heads",
                        equipment = "Barbell",
                        category = "Strength",
                        instructions = "Angle upper arms slightly back behind head, lower bar toward forehead/crown, and extend elbows."
                    ),
                    ExerciseEntity(
                        name = "Cable Rope Tricep Pushdown",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Triceps Lateral & Medial Heads",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Extend forearms fully downward and spread rope ends apart at bottom while keeping elbows pinned."
                    ),
                    ExerciseEntity(
                        name = "Overhead Cable Rope Tricep Extension",
                        muscleGroup = "Arms",
                        secondaryMuscles = "Triceps Long Head",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Face away from pulley, allow elbows to bend deeply behind head, and extend overhead."
                    ),

                    // ==================== CORE & ABS ====================
                    ExerciseEntity(
                        name = "Hanging Leg Raise",
                        muscleGroup = "Core",
                        secondaryMuscles = "Lower Abs, Hip Flexors, Grip",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Hang from bar, posteriorly tilt pelvis, and raise legs under control without swinging.",
                        isBodyweight = true
                    ),
                    ExerciseEntity(
                        name = "Kneeling Cable Rope Crunch",
                        muscleGroup = "Core",
                        secondaryMuscles = "Rectus Abdominis",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Hold rope beside head, flex spine by curling ribs toward pelvis rather than hinging at hips."
                    ),
                    ExerciseEntity(
                        name = "Ab Wheel Rollout",
                        muscleGroup = "Core",
                        secondaryMuscles = "Abs, Lats, Serratus",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Keep glutes squeezed and ribs tucked, roll out as far as lumbar control allows, and pull back.",
                        isBodyweight = true
                    ),
                    ExerciseEntity(
                        name = "Cable Woodchoppers",
                        muscleGroup = "Core",
                        secondaryMuscles = "Obliques, Transverse Abdominis",
                        equipment = "Cable",
                        category = "Strength",
                        instructions = "Rotate torso diagonally across body with arms extended while bracing core."
                    ),
                    ExerciseEntity(
                        name = "Weighted Plank",
                        muscleGroup = "Core",
                        secondaryMuscles = "Transverse Abdominis, Glutes, Shoulders",
                        equipment = "Bodyweight",
                        category = "Bodyweight",
                        instructions = "Maintain straight line from head to heels while bracing abs and glutes maximally.",
                        isBodyweight = true
                    )
                )
                dao.insertExercises(catalog)
            }
        }
    }
}
