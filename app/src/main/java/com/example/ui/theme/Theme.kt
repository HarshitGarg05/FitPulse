package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    LIGHT("Apple Health Light"),
    DARK("Apple Fitness Dark"),
    SYSTEM("System Automatic")
}

private val AppleHealthLightColorScheme = lightColorScheme(
    primary = AppleMoveRed,
    onPrimary = Color.White,
    primaryContainer = AppleMoveRedContainerLight,
    onPrimaryContainer = Color(0xFF5C0018),
    secondary = AppleStandBlue,
    onSecondary = Color.White,
    secondaryContainer = AppleStandBlueContainerLight,
    onSecondaryContainer = Color(0xFF002D62),
    tertiary = AppleExerciseGreenDarkText,
    onTertiary = Color.White,
    tertiaryContainer = AppleExerciseGreenContainerLight,
    onTertiaryContainer = Color(0xFF053814),
    background = AppleHealthLightBg,
    onBackground = AppleHealthLightOnSurface,
    surface = AppleHealthLightSurface,
    onSurface = AppleHealthLightOnSurface,
    surfaceVariant = AppleHealthLightContainer,
    onSurfaceVariant = AppleHealthLightOnVariant,
    outline = AppleHealthLightOutline,
    error = Color(0xFFFF3B30),
    onError = Color.White
)

private val AppleHealthDarkColorScheme = darkColorScheme(
    primary = AppleMoveRed,
    onPrimary = Color.White,
    primaryContainer = AppleMoveRedContainerDark,
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = AppleStandBlueDark,
    onSecondary = Color(0xFF001E42),
    secondaryContainer = AppleStandBlueContainerDark,
    onSecondaryContainer = Color(0xFFD4E7FF),
    tertiary = AppleExerciseGreen,
    onTertiary = Color(0xFF00290C),
    tertiaryContainer = AppleExerciseGreenContainerDark,
    onTertiaryContainer = Color(0xFFB6F6C6),
    background = AppleHealthDarkBg,
    onBackground = AppleHealthDarkOnSurface,
    surface = AppleHealthDarkSurface,
    onSurface = AppleHealthDarkOnSurface,
    surfaceVariant = AppleHealthDarkContainer,
    onSurfaceVariant = AppleHealthDarkOnVariant,
    outline = AppleHealthDarkOutline,
    error = Color(0xFFFF453A),
    onError = Color(0xFF4A0002)
)

val FitPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (useDark) AppleHealthDarkColorScheme else AppleHealthLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FitPulseShapes
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
            content()
        }
    }
}
