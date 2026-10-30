package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.VivoHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExerciseEntity
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleIndigo
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import com.example.ui.theme.AppleStandCyan
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared VSYNC-synced animation phase State so all Liquid Glass cards, buttons, and bars
 * read the phase strictly inside their GPU DrawScope (`drawWithContent` / `Canvas`) without
 * triggering per-frame Compose recompositions. Runs smoothly at 60Hz, 90Hz, 120Hz, and 144Hz.
 */
val LocalLiquidGlassPhase = compositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

/**
 * Real-time actively behaving ambient Apple iOS 26 Liquid Glass background mesh.
 * Continuously animates 6 vibrant multi-color aurora caustics behind translucent glass surfaces
 * in both Light Mode and Dark Mode.
 */
@Composable
fun LiquidGlassAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "liquidGlassMasterClock")
    val phaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "masterGlassPhase"
    )

    val baseBgColor = if (isDark) {
        Color(0xFF000000)
    } else {
        Color(0xFFE4EDFA) // Cool Iridescent Ice-Pearl Backdrop so White Glass & Caustics Pop in Light Mode
    }

    CompositionLocalProvider(
        LocalLiquidGlassPhase provides phaseState,
        LocalContentColor provides MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(baseBgColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val phase = phaseState.value
                val w = size.width
                val h = size.height
                val alphaScale = if (isDark) 0.28f else 0.44f

                if (!isDark) {
                    // Subtle iridescent pearl-sky base wash in Light Mode
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFDCE8FC),
                                Color(0xFFF3E9F8),
                                Color(0xFFE0F4F2),
                                Color(0xFFE6EEFA)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }

                // 1. Apple Move Crimson-Pink Liquid Orb
                val orb1Center = Offset(
                    x = w * (0.18f + 0.25f * cos(phase)),
                    y = h * (0.14f + 0.11f * sin(phase))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppleMoveRed.copy(alpha = alphaScale), Color.Transparent),
                        center = orb1Center,
                        radius = w * 0.72f
                    ),
                    center = orb1Center,
                    radius = w * 0.72f
                )

                // 2. Apple Stand Electric Blue Orb
                val orb2Center = Offset(
                    x = w * (0.84f + 0.18f * sin(phase * 0.8f + 1.1f)),
                    y = h * (0.30f + 0.15f * cos(phase * 0.8f + 0.5f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppleStandBlue.copy(alpha = alphaScale), Color.Transparent),
                        center = orb2Center,
                        radius = w * 0.74f
                    ),
                    center = orb2Center,
                    radius = w * 0.74f
                )

                // 3. Apple Exercise Neon Green Orb
                val orb3Center = Offset(
                    x = w * (0.24f + 0.22f * sin(phase + 2.0f)),
                    y = h * (0.60f + 0.14f * cos(phase + 1.2f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppleExerciseGreen.copy(alpha = alphaScale * 0.92f), Color.Transparent),
                        center = orb3Center,
                        radius = w * 0.68f
                    ),
                    center = orb3Center,
                    radius = w * 0.68f
                )

                // 4. Apple Mindful Violet / Indigo Orb
                val orb4Center = Offset(
                    x = w * (0.76f + 0.20f * cos(phase + 3.1f)),
                    y = h * (0.82f + 0.10f * sin(phase + 2.4f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppleMindPurple.copy(alpha = alphaScale * 0.95f), Color.Transparent),
                        center = orb4Center,
                        radius = w * 0.70f
                    ),
                    center = orb4Center,
                    radius = w * 0.70f
                )

                // 5. Apple Overload Flame Orange Caustic Highlight
                val orb5Center = Offset(
                    x = w * (0.52f + 0.26f * sin(phase * 1.1f)),
                    y = h * (0.46f + 0.18f * cos(phase * 1.1f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppleOverloadOrange.copy(alpha = alphaScale * 0.82f), Color.Transparent),
                        center = orb5Center,
                        radius = w * 0.58f
                    ),
                    center = orb5Center,
                    radius = w * 0.58f
                )

                // 6. Iridescent Cyan Glass Refraction Ribbon
                val orb6Center = Offset(
                    x = w * (0.45f + 0.30f * cos(phase + 4.2f)),
                    y = h * (0.22f + 0.20f * sin(phase + 3.8f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppleStandCyan.copy(alpha = alphaScale * 0.88f), Color.Transparent),
                        center = orb6Center,
                        radius = w * 0.62f
                    ),
                    center = orb6Center,
                    radius = w * 0.62f
                )
            }
            content()
        }
    }
}

/**
 * True Optical-Lens Apple iOS 26 Liquid Glass Card.
 * - Uses Draw-Phase (`drawWithContent`) rendering synced to `LocalLiquidGlassPhase` for 0-recomposition
 *   performance across 60Hz, 90Hz, 120Hz, and 144Hz displays.
 * - In Light Mode, renders a genuinely translucent frosted acrylic lens (`0.46f` white + tinted caustic wave +
 *   dual prismatic & white specular bevels) so the live ambient aurora visibly refracts through every card.
 * - Guarantees `LocalContentColor` is `MaterialTheme.colorScheme.onSurface` so text is ALWAYS crisp white
 *   in Dark Mode and deep ink in Light Mode.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    accentTint: Color = AppleStandBlue,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val phaseState = LocalLiquidGlassPhase.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassCardScale"
    )

    val shape = RoundedCornerShape(24.dp)

    CompositionLocalProvider(LocalContentColor provides onSurfaceColor) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .drawWithContent {
                    val phase = phaseState.value
                    val w = size.width
                    val h = size.height
                    val cornerPx = 24.dp.toPx()
                    val cornerRadius = CornerRadius(cornerPx, cornerPx)

                    // 1. Translucent Frosted Optical Glass Base (allows background aurora to refract through!)
                    val glassBaseBrush = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E1E24).copy(alpha = 0.72f),
                                Color(0xFF141418).copy(alpha = 0.64f),
                                accentTint.copy(alpha = 0.14f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.56f),
                                accentTint.copy(alpha = 0.13f),
                                AppleStandCyan.copy(alpha = 0.11f),
                                Color.White.copy(alpha = 0.44f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    }
                    drawRoundRect(
                        brush = glassBaseBrush,
                        cornerRadius = cornerRadius
                    )

                    // 2. Real-Time Moving Internal Liquid Caustic Orb inside the Glass Card
                    val causticCenter = Offset(
                        x = w * (0.50f + 0.36f * cos(phase + accentTint.red * 3f)),
                        y = h * (0.45f + 0.32f * sin(phase + accentTint.blue * 3f))
                    )
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentTint.copy(alpha = if (isDark) 0.20f else 0.24f),
                                AppleStandCyan.copy(alpha = if (isDark) 0.10f else 0.15f),
                                Color.Transparent
                            ),
                            center = causticCenter,
                            radius = (w.coerceAtLeast(h) * 0.75f).coerceAtLeast(100f)
                        ),
                        cornerRadius = cornerRadius
                    )

                    // 3. Curved Top-Half Optical Glass Lens Specular Sheen
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.78f),
                                    Color.White.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            },
                            startY = 0f,
                            endY = (h * 0.48f).coerceAtLeast(40f)
                        ),
                        cornerRadius = cornerRadius
                    )

                    // 4. Draw card UI content
                    drawContent()

                    // 5. Inner 3D Optical Specular Bevel Ring (inset by 1.5dp)
                    val insetPx = 1.5.dp.toPx()
                    val innerCorner = CornerRadius((cornerPx - insetPx).coerceAtLeast(0f))
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    Color.Transparent,
                                    accentTint.copy(alpha = 0.25f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.96f),
                                    Color.White.copy(alpha = 0.35f),
                                    accentTint.copy(alpha = 0.32f)
                                )
                            },
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        topLeft = Offset(insetPx, insetPx),
                        size = Size((w - insetPx * 2f).coerceAtLeast(0f), (h - insetPx * 2f).coerceAtLeast(0f)),
                        cornerRadius = innerCorner,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // 6. Outer Orbiting Prismatic Liquid Glass Rim Border (2.2dp)
                    val shimmerX = w * (0.5f + 0.5f * cos(phase))
                    val shimmerY = h * (0.5f + 0.5f * sin(phase))
                    val borderBrush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.62f),
                                accentTint.copy(alpha = 0.78f),
                                AppleStandCyan.copy(alpha = 0.58f),
                                AppleMindPurple.copy(alpha = 0.58f),
                                Color.White.copy(alpha = 0.35f)
                            )
                        } else {
                            listOf(
                                accentTint.copy(alpha = 0.72f),
                                Color.White.copy(alpha = 1.0f),
                                AppleStandCyan.copy(alpha = 0.68f),
                                AppleMindPurple.copy(alpha = 0.55f),
                                accentTint.copy(alpha = 0.65f)
                            )
                        },
                        start = Offset(shimmerX, 0f),
                        end = Offset(w - shimmerX, h + shimmerY * 0.3f)
                    )
                    val strokePx = 2.dp.toPx()
                    drawRoundRect(
                        brush = borderBrush,
                        topLeft = Offset(strokePx / 2f, strokePx / 2f),
                        size = Size((w - strokePx).coerceAtLeast(0f), (h - strokePx).coerceAtLeast(0f)),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = strokePx)
                    )
                }
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(16.dp)
        ) {
            content()
        }
    }
}

/**
 * Unified Apple iOS 26 Large Page Header used at the top of ALL 5 tabs
 * (`Summary`, `Attendance`, `Workouts`, `Exercises`, `Settings`) so every screen has
 * the exact same vibrant uppercase overline + large 36sp bold display title + optional right action.
 */
@Composable
fun AppleLargePageHeader(
    overlineText: String,
    title: String,
    accentColor: Color = AppleStandBlue,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null,
    trailingBadge: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = overlineText.uppercase(Locale.US),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingBadge != null) {
            trailingBadge()
        } else if (actionLabel != null && onActionClick != null) {
            LiquidGlassPrimaryButton(
                text = actionLabel,
                onClick = onActionClick,
                icon = actionIcon,
                accentColor = accentColor,
                testTag = actionTestTag
            )
        }
    }
}

/**
 * Floating Apple Dynamic Island / Liquid Glass Bottom Dock.
 * Uses drawWithContent synced to `LocalLiquidGlassPhase` for 0-recomposition liquid refraction
 * in both Light Mode and Dark Mode.
 */
data class DynamicIslandNavItem<T>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

@Composable
fun <T> LiquidGlassDynamicIslandBar(
    items: List<DynamicIslandNavItem<T>>,
    selectedRoute: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val phaseState = LocalLiquidGlassPhase.current
    val islandShape = RoundedCornerShape(36.dp)
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "islandSlidingPillIndex"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = selectedItem.accentColor,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "islandSlidingAccent"
    )

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = islandShape,
                        spotColor = animatedAccentColor.copy(alpha = if (isDark) 0.60f else 0.40f),
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.70f else 0.35f)
                    )
                    .clip(islandShape)
                    .drawWithContent {
                        val phase = phaseState.value
                        val w = size.width
                        val h = size.height
                        val cornerRadius = CornerRadius(36.dp.toPx(), 36.dp.toPx())

                        // 0. 100% Opaque Foundation - completely prevents background scrolling text/cards from showing through
                        drawRoundRect(
                            color = if (isDark) Color(0xFF15161E) else Color(0xFFFFFFFF),
                            cornerRadius = cornerRadius
                        )

                        // 1. Frosted Liquid Glass Pill Surface Sheen
                        drawRoundRect(
                            brush = if (isDark) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E1F29).copy(alpha = 0.95f),
                                        Color(0xFF15161E).copy(alpha = 0.98f),
                                        animatedAccentColor.copy(alpha = 0.18f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(0xFFF6F8FE),
                                        animatedAccentColor.copy(alpha = 0.12f),
                                        Color.White
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(w, h)
                                )
                            },
                            cornerRadius = cornerRadius
                        )

                        // 2. Top lens highlight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.22f else 0.85f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h * 0.55f
                            ),
                            cornerRadius = cornerRadius
                        )

                        // 3. Smoothly Sliding Active Tab Liquid Glass Capsule
                        val padH = 6.dp.toPx()
                        val padV = 6.dp.toPx()
                        val innerW = (w - padH * 2f).coerceAtLeast(1f)
                        val innerH = (h - padV * 2f).coerceAtLeast(1f)
                        val count = items.size.coerceAtLeast(1)
                        val tabWidth = innerW / count
                        val pillLeft = padH + (animatedIndex * tabWidth)
                        val pillCorner = CornerRadius(28.dp.toPx(), 28.dp.toPx())

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(
                                        animatedAccentColor.copy(alpha = 0.34f),
                                        animatedAccentColor.copy(alpha = 0.18f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        animatedAccentColor.copy(alpha = 0.24f)
                                    )
                                },
                                start = Offset(pillLeft, padV),
                                end = Offset(pillLeft + tabWidth, padV + innerH)
                            ),
                            topLeft = Offset(pillLeft, padV),
                            size = Size(tabWidth, innerH),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.65f else 0.98f),
                                    animatedAccentColor.copy(alpha = 0.72f)
                                ),
                                start = Offset(pillLeft, padV),
                                end = Offset(pillLeft + tabWidth, padV + innerH)
                            ),
                            topLeft = Offset(pillLeft, padV),
                            size = Size(tabWidth, innerH),
                            cornerRadius = pillCorner,
                            style = Stroke(width = 1.3.dp.toPx())
                        )

                        // 4. Tab Icons & Labels
                        drawContent()

                        // 5. Animated Prismatic Outer Rim
                        val shiftX = w * (0.5f + 0.5f * cos(phase))
                        val strokePx = 2.dp.toPx()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    animatedAccentColor.copy(alpha = 0.82f),
                                    Color.White.copy(alpha = if (isDark) 0.65f else 1.0f),
                                    AppleStandCyan.copy(alpha = 0.70f),
                                    AppleMindPurple.copy(alpha = 0.65f),
                                    animatedAccentColor.copy(alpha = 0.75f)
                                ),
                                start = Offset(shiftX, 0f),
                                end = Offset(w - shiftX, h)
                            ),
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size((w - strokePx).coerceAtLeast(0f), (h - strokePx).coerceAtLeast(0f)),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = strokePx)
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = item.route == selectedRoute
                    val itemInteraction = remember { MutableInteractionSource() }
                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "navItemScale"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            item.accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "navContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                            .clickable(
                                interactionSource = itemInteraction,
                                indication = null
                            ) { onSelect(item.route) }
                            .padding(vertical = 8.dp, horizontal = 2.dp)
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single-line tactile Apple Liquid Glass Primary Button that NEVER wraps or clips text onto 2 lines.
 */
@Composable
fun LiquidGlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = AppleStandBlue,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        border = BorderStroke(
            width = 1.4.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.80f),
                    accentColor.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.35f)
                )
            )
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = Color.White,
            disabledContainerColor = accentColor.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LiquidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = AppleStandBlue,
    testTag: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "secBtnScale"
    )

    val labelAndIconColor = if (isDark) {
        accentColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.6.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isDark) 0.75f else 0.90f),
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.95f),
                    accentColor.copy(alpha = if (isDark) 0.55f else 0.78f)
                )
            )
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = accentColor.copy(alpha = if (isDark) 0.18f else 0.24f),
            contentColor = labelAndIconColor
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = labelAndIconColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Polished Apple iOS 26 Liquid Glass Segmented Control:
 * - Smoothly slides the selected Liquid Glass pill horizontally from its current option position
 *   to the newly clicked option position using spring physics (`animatedSelectedIndex`).
 * - Uses a high-contrast vibrant `accentColor` Liquid Glass pill with pure white selected text
 *   and deep ink/white unselected text so it is 100% legible in both Light Mode and Dark Mode.
 * - Eliminates any black/gray color flash by never interpolating with `Color.Transparent` (`#00000000`)
 *   and disabling dark ripple flashes (`indication = null`).
 */
@Composable
fun <T> AppleSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    accentColor: Color = AppleStandBlue,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "segmentedSlidingPillIndex"
    )

    val trackShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .drawWithContent {
                val w = size.width
                val h = size.height
                val trackCorner = CornerRadius(16.dp.toPx(), 16.dp.toPx())

                // 1. Defined Translucent Liquid Glass Track Trough
                drawRoundRect(
                    brush = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A1A22).copy(alpha = 0.85f),
                                Color(0xFF121218).copy(alpha = 0.80f),
                                accentColor.copy(alpha = 0.18f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFD5E1F5).copy(alpha = 0.86f),
                                accentColor.copy(alpha = 0.16f),
                                Color(0xFFDEEAFA).copy(alpha = 0.82f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    },
                    cornerRadius = trackCorner
                )

                // 2. Continuous Sliding Vibrant Liquid Glass Active Pill (translates smoothly from old X to new X)
                val pad = 4.dp.toPx()
                val innerW = (w - pad * 2f).coerceAtLeast(1f)
                val innerH = (h - pad * 2f).coerceAtLeast(1f)
                val count = items.size.coerceAtLeast(1)
                val segmentW = innerW / count
                val pillLeft = pad + (animatedIndex * segmentW)
                val pillCorner = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                // Vibrant accent-tinted Liquid Glass pill fill
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.90f else 0.95f),
                            accentColor.copy(alpha = if (isDark) 0.72f else 0.84f)
                        ),
                        startY = pad,
                        endY = pad + innerH
                    ),
                    topLeft = Offset(pillLeft, pad),
                    size = Size(segmentW, innerH),
                    cornerRadius = pillCorner
                )

                // Top glossy lens sheen on the sliding pill
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.Transparent
                        ),
                        startY = pad,
                        endY = pad + innerH * 0.55f
                    ),
                    topLeft = Offset(pillLeft, pad),
                    size = Size(segmentW, innerH),
                    cornerRadius = pillCorner
                )

                // Pill prismatic specular rim
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f),
                            accentColor.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.60f)
                        ),
                        start = Offset(pillLeft, pad),
                        end = Offset(pillLeft + segmentW, pad + innerH)
                    ),
                    topLeft = Offset(pillLeft, pad),
                    size = Size(segmentW, innerH),
                    cornerRadius = pillCorner,
                    style = Stroke(width = 1.4.dp.toPx())
                )

                // 3. Draw segment text labels
                drawContent()

                // 4. Outer Track Specular Border
                val borderStroke = 1.5.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.48f else 0.58f),
                            Color.White.copy(alpha = if (isDark) 0.35f else 0.90f),
                            AppleStandCyan.copy(alpha = 0.45f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    ),
                    topLeft = Offset(borderStroke / 2f, borderStroke / 2f),
                    size = Size((w - borderStroke).coerceAtLeast(0f), (h - borderStroke).coerceAtLeast(0f)),
                    cornerRadius = trackCorner,
                    style = Stroke(width = borderStroke)
                )
            }
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val itemInteraction = remember { MutableInteractionSource() }
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "segTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = itemInteraction,
                            indication = null
                        ) { onItemSelected(item) }
                        .padding(vertical = 9.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Vivo / iQOO OriginOS 6 "Origin Island" (Atomic Island / 原子岛) Live Floating Top Capsule.
 * Automatically triggers and expands at the top of the app whenever:
 * 1. A Live Workout is started (`isWorkoutActive == true`)
 * 2. An Exercise is registered in the plan & its live timer is running (`activeExerciseName != null`)
 * 3. The Rest Countdown Timer is running (`isRestTimerRunning == true`)
 * Also syncs with the background OriginOS 6 Origin Island notification when the app is minimized.
 */
@Composable
fun OriginIslandLiveTopCapsule(
    isVisible: Boolean,
    isResting: Boolean,
    isTimerRunning: Boolean,
    headlineText: String,
    subtitleText: String,
    timerBadgeText: String,
    accentColor: Color,
    onToggleTimer: () -> Unit,
    onAddRest15s: () -> Unit,
    onSkipRest: () -> Unit,
    onOpenWorkoutLogger: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val phaseState = LocalLiquidGlassPhase.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(220)) + expandVertically(tween(260)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(220)),
        modifier = modifier
    ) {
        val capsuleCorner = if (isExpanded) 28.dp else 999.dp
        val capsuleShape = RoundedCornerShape(capsuleCorner)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(if (isExpanded) 1f else 0.92f)
                    .clip(capsuleShape)
                    .drawWithContent {
                        val phase = phaseState.value
                        val w = size.width
                        val h = size.height
                        val cr = CornerRadius(capsuleCorner.toPx(), capsuleCorner.toPx())

                        // OriginOS 6 Deep Charcoal / Liquid Glass Capsule Body
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0E0E13).copy(alpha = 0.94f),
                                    Color(0xFF1A1A24).copy(alpha = 0.92f),
                                    accentColor.copy(alpha = 0.28f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(w, h)
                            ),
                            cornerRadius = cr
                        )

                        // Moving caustic glow inside Origin Island
                        val glowX = w * (0.5f + 0.4f * cos(phase))
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.32f),
                                    Color.Transparent
                                ),
                                center = Offset(glowX, h * 0.5f),
                                radius = w * 0.65f
                            ),
                            cornerRadius = cr
                        )

                        drawContent()

                        // Glowing Origin Island Prismatic Rim
                        val strokeW = 1.8.dp.toPx()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor,
                                    Color.White.copy(alpha = 0.85f),
                                    AppleStandCyan,
                                    accentColor
                                ),
                                start = Offset(glowX, 0f),
                                end = Offset(w - glowX, h)
                            ),
                            topLeft = Offset(strokeW / 2f, strokeW / 2f),
                            size = Size((w - strokeW).coerceAtLeast(0f), (h - strokeW).coerceAtLeast(0f)),
                            cornerRadius = cr,
                            style = Stroke(width = strokeW)
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("origin_island_live_capsule"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing Origin Island Status Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = headlineText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD0D0DC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Live Timer Chip inside Origin Island
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accentColor.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.70f))
                    ) {
                        Text(
                            text = timerBadgeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (isExpanded) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isResting) {
                            LiquidGlassPrimaryButton(
                                text = "+15s Rest",
                                onClick = onAddRest15s,
                                accentColor = AppleOverloadOrange,
                                modifier = Modifier.weight(1f)
                            )
                            LiquidGlassPrimaryButton(
                                text = "Skip Rest",
                                onClick = onSkipRest,
                                accentColor = AppleMoveRed,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LiquidGlassPrimaryButton(
                                text = if (isTimerRunning) "Pause Timer" else "Resume Timer",
                                onClick = onToggleTimer,
                                accentColor = if (isTimerRunning) AppleOverloadOrange else AppleExerciseGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LiquidGlassPrimaryButton(
                            text = "Open Logger",
                            onClick = {
                                isExpanded = false
                                onOpenWorkoutLogger()
                            },
                            accentColor = AppleStandBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple iOS 26 Liquid Glass Dropdown Field:
 * Places the field heading cleanly ABOVE the unbroken rounded Liquid Glass dropdown trigger box
 * so the heading never overlaps or cuts a notch into the dropdown border on any page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitleProvider: ((String) -> String?)? = null,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember(selectedValue) { mutableStateOf(selectedValue) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSearchable = options.size > 3

    val filteredOptions = remember(searchQuery, options, selectedValue) {
        val q = searchQuery.trim()
        if (q.isBlank() || q.equals(selectedValue.trim(), ignoreCase = true)) {
            options
        } else {
            options.filter { opt ->
                opt.contains(q, ignoreCase = true) ||
                (subtitleProvider?.invoke(opt)?.contains(q, ignoreCase = true) == true)
            }.sortedWith(
                compareByDescending<String> {
                    if (it.startsWith(q, ignoreCase = true)) 3
                    else if (it.contains(q, ignoreCase = true)) 2
                    else 1
                }.thenBy { it }
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSearchable) {
                // Primary Editable Anchor: Standard Compose architecture for searchable dropdowns.
                // Because this TextField is in the main window hierarchy (not inside a popup),
                // the Android soft keyboard (IME) is guaranteed to open immediately on all devices!
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        expanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                    placeholder = {
                        Text(
                            text = selectedValue.ifBlank { "Type to search or select..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = if (isDark) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                        } else {
                            Color.White.copy(alpha = 0.88f)
                        },
                        unfocusedContainerColor = if (isDark) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                        } else {
                            Color.White.copy(alpha = 0.74f)
                        },
                        focusedBorderColor = AppleStandBlue,
                        unfocusedBorderColor = AppleStandBlue.copy(alpha = 0.42f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AppleStandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty() && searchQuery != selectedValue) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        expanded = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    }
                )
            } else {
                Surface(
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) {
                        MaterialTheme.colorScheme.surface.copy(alpha = if (expanded) 0.84f else 0.70f)
                    } else {
                        Color.White.copy(alpha = if (expanded) 0.88f else 0.74f)
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        width = if (expanded) 1.5.dp else 1.1.dp,
                        color = if (expanded) AppleStandBlue else AppleStandBlue.copy(alpha = 0.42f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedValue,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = label.ifBlank { "Dropdown" },
                            tint = AppleStandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    searchQuery = selectedValue
                },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                if (filteredOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "No matches found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {}
                    )
                } else {
                    filteredOptions.forEach { option ->
                        val isSelected = option.equals(selectedValue, ignoreCase = true)
                        val sub = subtitleProvider?.invoke(option)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AppleStandBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!sub.isNullOrBlank()) {
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppleStandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                searchQuery = option
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple iOS 26 Liquid Glass Text Field:
 * Renders the field heading cleanly ABOVE the unbroken rounded input box (`label = null` on
 * `OutlinedTextField`) so the heading never overlaps or cuts a notch into the border.
 */
@Composable
fun AppleLiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    accentColor: Color = AppleStandBlue,
    fieldModifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = null,
            placeholder = if (placeholder.isNotBlank()) {
                {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = if (isDark) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                } else {
                    Color.White.copy(alpha = 0.78f)
                },
                unfocusedContainerColor = if (isDark) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)
                } else {
                    Color.White.copy(alpha = 0.68f)
                },
                focusedBorderColor = accentColor,
                unfocusedBorderColor = accentColor.copy(alpha = 0.42f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(fieldModifier)
        )
    }
}

/**
 * Multi-select Exercise Dropdown for Routine Creation/Editing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppleMultiSelectExerciseDropdown(
    availableExercises: List<ExerciseEntity>,
    selectedExerciseNames: List<String>,
    onToggleExercise: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember(expanded) { mutableStateOf("") }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val filteredExercises = remember(searchQuery, availableExercises) {
        if (searchQuery.isBlank()) {
            availableExercises
        } else {
            val q = searchQuery.trim()
            availableExercises.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.muscleGroup.contains(q, ignoreCase = true) ||
                it.equipment.contains(q, ignoreCase = true)
            }.sortedWith(
                compareByDescending<ExerciseEntity> {
                    if (it.name.startsWith(q, ignoreCase = true)) 3
                    else if (it.name.contains(q, ignoreCase = true)) 2
                    else 1
                }.thenBy { it.name }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .testTag("routine_exercises_dropdown_trigger"),
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            } else {
                Color.White.copy(alpha = 0.58f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.2.dp, AppleStandBlue.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Exercises (Searchable List)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (selectedExerciseNames.isEmpty()) {
                            "Choose exercises from library (0 selected)"
                        } else {
                            "${selectedExerciseNames.size} exercise${if (selectedExerciseNames.size == 1) "" else "s"} selected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Exercise Dropdown",
                    tint = AppleStandBlue
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    Color.White.copy(alpha = 0.88f)
                },
                border = BorderStroke(1.dp, AppleStandBlue.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search exercise (e.g. Leg, Press)...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = AppleStandBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    if (filteredExercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No exercises found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val displayExercises = if (searchQuery.isBlank()) filteredExercises.take(15) else filteredExercises
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            displayExercises.forEachIndexed { index, exercise ->
                                val isSelected = selectedExerciseNames.any { it.equals(exercise.name, ignoreCase = true) }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleExercise(exercise.name) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AppleStandBlue.copy(alpha = 0.12f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exercise.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) AppleStandBlue else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${exercise.muscleGroup} • ${exercise.equipment}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = AppleStandBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (index < displayExercises.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { expanded = false }) {
                            Text("Done Selecting", style = MaterialTheme.typography.labelMedium, color = AppleStandBlue)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedExerciseNames.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedExerciseNames.forEachIndexed { idx, name ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppleStandBlue.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.dp, AppleStandBlue.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { onToggleExercise(name) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(AppleStandBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Properly aligned Section Header with single-line action button so buttons like
 * "+ Create Plan" never wrap onto two lines or collide with the section title.
 */
@Composable
fun AppleSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    actionColor: Color = AppleStandBlue,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            LiquidGlassPrimaryButton(
                text = actionLabel,
                onClick = onActionClick,
                icon = actionIcon,
                accentColor = actionColor,
                testTag = actionTestTag
            )
        }
    }
}

/**
 * Smoothly animated numeric value for real-time KPI & overload cards.
 */
@Composable
fun AnimatedMetricValueText(
    targetValue: Float,
    suffix: String = "",
    decimals: Int = 0,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedVal by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "metricCounter"
    )
    val formatted = if (decimals <= 0 || animatedVal % 1f == 0f) {
        animatedVal.toInt().toString()
    } else {
        String.format(Locale.US, "%.${decimals}f", animatedVal)
    }
    Text(
        text = if (suffix.isBlank()) formatted else "$formatted $suffix",
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}
