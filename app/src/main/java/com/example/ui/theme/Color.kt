package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Apple Health, Apple Fitness+ & iOS Liquid Glass Vibrant Palette
val AppleMoveRed = Color(0xFFFA114F)            // Move / Gym Attendance Ring (Vibrant Crimson-Pink)
val AppleMoveRedLight = Color(0xFFFF375F)
val AppleMoveRedContainerLight = Color(0xFFFFE5EC)
val AppleMoveRedContainerDark = Color(0xFF4C081B)

val AppleExerciseGreen = Color(0xFF30D158)      // Exercise / Workout Sets Ring (Apple Neon Green)
val AppleExerciseGreenDarkText = Color(0xFF1E8E3E)
val AppleExerciseGreenContainerLight = Color(0xFFE6F9EB)
val AppleExerciseGreenContainerDark = Color(0xFF0B3B17)

val AppleStandBlue = Color(0xFF0A84FF)          // Stand / Consistency Ring (iOS Electric Blue)
val AppleStandBlueDark = Color(0xFF409CFF)
val AppleStandCyan = Color(0xFF5AC8FA)          // Apple Fitness Cyan Ring Accent
val AppleStandBlueContainerLight = Color(0xFFE5F1FF)
val AppleStandBlueContainerDark = Color(0xFF002B59)

val AppleOverloadOrange = Color(0xFFFF9F0A)     // Progressive Overload / Volume Flame Orange
val AppleMindPurple = Color(0xFFBF5AF2)         // Mindful / Hypertrophy & PR Violet
val AppleIndigo = Color(0xFF5E5CE6)             // Apple Fitness+ Indigo Accent
val AppleTeal = Color(0xFF64D2FF)               // Apple Health Respiratory / Recovery Cyan-Teal
val ApplePink = Color(0xFFFF2D55)               // Apple Health Heart Pink
val AppleMint = Color(0xFF00C7BE)               // Apple Health Mobility Mint
val AppleGoldYellow = Color(0xFFFFD60A)         // Trophy / Personal Record Gold

// Apple Health Light Mode Surfaces (Translucent iOS Grouped Liquid Glass Appearance)
val AppleHealthLightBg = Color(0xFFEAF0FA)      // iOS Liquid Pearl Backdrop for High Specular Glass Contrast
val AppleHealthLightSurface = Color(0xFFFFFFFF) // Pure White Modular Card Surface
val AppleHealthLightContainer = Color(0xFFDCE4F2) // iOS Translucent Cool Glass Container
val AppleHealthLightOutline = Color(0xFFC5CFE0) // Crisp Specular Hairline Border
val AppleHealthLightOnSurface = Color(0xFF0B0C10) // Crisp Deep Ink Primary Text
val AppleHealthLightOnVariant = Color(0xFF4A4D57) // High-Contrast iOS Secondary Label Gray

// Apple Fitness+ Dark Mode Surfaces (OLED Pitch Black & Translucent Charcoal Glass)
val AppleHealthDarkBg = Color(0xFF000000)       // True Pitch Black OLED Background
val AppleHealthDarkSurface = Color(0xFF1C1C1E)  // Elevated iOS Dark Card Surface
val AppleHealthDarkContainer = Color(0xFF2C2C2E) // Secondary Dark Container
val AppleHealthDarkOutline = Color(0xFF3A3A3C)  // Subtle Dark Specular Separator
val AppleHealthDarkOnSurface = Color(0xFFFFFFFF) // Pure White Primary Text
val AppleHealthDarkOnVariant = Color(0xFFB8B8C2) // Bright Legible iOS Dark Secondary Gray

// Semantic compatibility aliases across components
val GoogleFitHeartTeal = AppleExerciseGreen
val GoogleFitHeartTealDarkText = AppleExerciseGreenDarkText
val GoogleFitStepsBlue = AppleStandBlue
val GoogleFitStepsBlueDark = AppleStandBlueDark
val GoogleFitCaloriesOrange = AppleOverloadOrange
val GoogleFitSleepPurple = AppleMindPurple
val GoogleFitVitalsRed = AppleMoveRed
val GoogleFitNutritionYellow = AppleGoldYellow

val FitHeartTealDark = AppleExerciseGreen
val FitHeartTealLight = AppleExerciseGreenDarkText
val FitStepsBlueDark = AppleStandBlueDark
val FitStepsBlueLight = AppleStandBlue
val FitCaloriesOrange = AppleOverloadOrange
val FitSleepPurple = AppleMindPurple

val ReadinessOptimal = AppleExerciseGreen
val ReadinessModerate = AppleOverloadOrange
val ReadinessLow = AppleMoveRed
val GoldPrBadge = AppleOverloadOrange
