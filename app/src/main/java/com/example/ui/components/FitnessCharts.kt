package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.CalendarAttendanceStatus
import com.example.data.repository.CalendarDayCell
import com.example.data.repository.DailyChartPoint
import com.example.data.repository.DraftSetEntry
import com.example.data.repository.ExerciseSessionSummary
import com.example.data.repository.MuscleGroupStat
import com.example.ui.theme.AppleExerciseGreen
import com.example.ui.theme.AppleGoldYellow
import com.example.ui.theme.AppleMindPurple
import com.example.ui.theme.AppleMoveRed
import com.example.ui.theme.AppleOverloadOrange
import com.example.ui.theme.AppleStandBlue
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 4-Ring Real-Time Multi-Color Apple Fitness & Liquid Glass Activity Rings:
 * 1. Outer Ring (Ruby-Magenta Gradient): Weekly Gym Attendance Goal
 * 2. Second Ring (Emerald-Lime Gradient): Completed + Live Training Sets
 * 3. Third Ring (Electric Cyan-Blue Gradient): Active Attendance Streak
 * 4. Inner Ring (Amber-Purple Overload Gradient): Training Volume Progress
 */
@Composable
fun AppleActivityRingsCardContent(
    attendanceDaysThisWeek: Int,
    attendanceWeeklyGoal: Int,
    completedSetsThisWeek: Int,
    weeklySetsTarget: Int = 15,
    currentStreakDays: Int,
    weeklyVolumeKg: Float = 0f,
    weeklyVolumeTargetKg: Float = 4000f,
    todayVolumeLabel: String,
    modifier: Modifier = Modifier
) {
    var selectedRingIndex by remember { mutableIntStateOf(0) }

    val moveProgressTarget = (attendanceDaysThisWeek.toFloat() / attendanceWeeklyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val exerciseProgressTarget = (completedSetsThisWeek.toFloat() / weeklySetsTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
    val standProgressTarget = (currentStreakDays.toFloat() / attendanceWeeklyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val volumeProgressTarget = (weeklyVolumeKg / weeklyVolumeTargetKg.coerceAtLeast(500f)).coerceIn(0f, 1f)

    val moveAnim by animateFloatAsState(
        targetValue = moveProgressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "moveRing"
    )
    val exerciseAnim by animateFloatAsState(
        targetValue = exerciseProgressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "exerciseRing"
    )
    val standAnim by animateFloatAsState(
        targetValue = standProgressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "standRing"
    )
    val volumeAnim by animateFloatAsState(
        targetValue = volumeProgressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "volumeRing"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable { selectedRingIndex = (selectedRingIndex + 1) % 4 },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 11.dp.toPx()
                val gap = 3.5.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                val r1 = (size.minDimension / 2f) - (strokeWidth / 2f) - 2.dp.toPx()
                val r2 = r1 - strokeWidth - gap
                val r3 = r2 - strokeWidth - gap
                val r4 = r3 - strokeWidth - gap

                fun drawMultiColorRing(
                    ringIdx: Int,
                    radius: Float,
                    progress: Float,
                    primaryColor: Color,
                    secondaryColor: Color,
                    tertiaryColor: Color
                ) {
                    val isHighlighted = selectedRingIndex == ringIdx
                    val effectiveStroke = if (isHighlighted) strokeWidth * 1.12f else strokeWidth
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val ringSize = Size(radius * 2f, radius * 2f)

                    drawArc(
                        color = primaryColor.copy(alpha = if (isHighlighted) 0.24f else 0.14f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = ringSize,
                        style = Stroke(width = effectiveStroke, cap = StrokeCap.Round)
                    )

                    val sweep = (progress * 360f).coerceAtLeast(if (progress > 0f) 12f else 0f)
                    if (sweep > 0f) {
                        val gradientBrush = Brush.sweepGradient(
                            colors = listOf(
                                primaryColor,
                                secondaryColor,
                                tertiaryColor,
                                primaryColor
                            ),
                            center = center
                        )
                        drawArc(
                            brush = gradientBrush,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = ringSize,
                            style = Stroke(width = effectiveStroke, cap = StrokeCap.Round)
                        )

                        val endAngleRad = Math.toRadians((-90.0 + sweep.toDouble()))
                        val tipX = center.x + radius * cos(endAngleRad).toFloat()
                        val tipY = center.y + radius * sin(endAngleRad).toFloat()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.88f),
                            radius = (effectiveStroke * 0.28f) * glowPulse,
                            center = Offset(tipX, tipY)
                        )
                    }
                }

                drawMultiColorRing(
                    ringIdx = 0,
                    radius = r1,
                    progress = moveAnim,
                    primaryColor = AppleMoveRed,
                    secondaryColor = Color(0xFFFF375F),
                    tertiaryColor = Color(0xFFFF6482)
                )
                drawMultiColorRing(
                    ringIdx = 1,
                    radius = r2,
                    progress = exerciseAnim,
                    primaryColor = AppleExerciseGreen,
                    secondaryColor = Color(0xFF34C759),
                    tertiaryColor = Color(0xFF00E676)
                )
                drawMultiColorRing(
                    ringIdx = 2,
                    radius = r3,
                    progress = standAnim,
                    primaryColor = AppleStandBlue,
                    secondaryColor = Color(0xFF5AC8FA),
                    tertiaryColor = Color(0xFF64D2FF)
                )
                drawMultiColorRing(
                    ringIdx = 3,
                    radius = r4,
                    progress = volumeAnim,
                    primaryColor = AppleOverloadOrange,
                    secondaryColor = AppleGoldYellow,
                    tertiaryColor = AppleMindPurple
                )
            }

            val centerPercent = when (selectedRingIndex) {
                0 -> (moveAnim * 100).roundToInt()
                1 -> (exerciseAnim * 100).roundToInt()
                2 -> (standAnim * 100).roundToInt()
                else -> (volumeAnim * 100).roundToInt()
            }
            val centerLabel = when (selectedRingIndex) {
                0 -> "Attend"
                1 -> "Sets"
                2 -> "Streak"
                else -> "Volume"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$centerPercent%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RingMetricLegendRow(
                color = AppleMoveRed,
                secondaryColor = Color(0xFFFF6482),
                label = "ATTENDANCE",
                value = "$attendanceDaysThisWeek / $attendanceWeeklyGoal days",
                subLabel = "Weekly Gym Goal",
                isSelected = selectedRingIndex == 0,
                onClick = { selectedRingIndex = 0 }
            )
            RingMetricLegendRow(
                color = AppleExerciseGreen,
                secondaryColor = Color(0xFF00E676),
                label = "TRAINING SETS",
                value = "$completedSetsThisWeek / $weeklySetsTarget sets",
                subLabel = "Live + Completed",
                isSelected = selectedRingIndex == 1,
                onClick = { selectedRingIndex = 1 }
            )
            RingMetricLegendRow(
                color = AppleStandBlue,
                secondaryColor = Color(0xFF64D2FF),
                label = "ACTIVE STREAK",
                value = "$currentStreakDays day${if (currentStreakDays == 1) "" else "s"}",
                subLabel = "Consecutive Visits",
                isSelected = selectedRingIndex == 2,
                onClick = { selectedRingIndex = 2 }
            )
            RingMetricLegendRow(
                color = AppleOverloadOrange,
                secondaryColor = AppleMindPurple,
                label = "VOLUME LOAD",
                value = todayVolumeLabel,
                subLabel = "Real-Time Overload",
                isSelected = selectedRingIndex == 3,
                onClick = { selectedRingIndex = 3 }
            )
        }
    }
}

@Composable
private fun RingMetricLegendRow(
    color: Color,
    secondaryColor: Color,
    label: String,
    value: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.14f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "legendBgAlpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = bgAlpha))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(color, secondaryColor)))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Interactive Apple Health style bar chart for 7-day volume, sets, or attendance
 * with animated bar growth and multi-stop gradient bars.
 */
@Composable
fun AppleHealthBarChart(
    points: List<DailyChartPoint>,
    barColor: Color,
    unitSuffix: String,
    emptyMessage: String = "No activity recorded for this range yet.",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points) {
        val lastNonZero = points.indexOfLast { it.value > 0f }
        mutableIntStateOf(if (lastNonZero >= 0) lastNonZero else points.lastIndex)
    }
    val safeIdx = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIdx]
    val maxVal = (points.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)
    val allZero = points.all { it.value <= 0f }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedPoint.shortDayLabel} • ${selectedPoint.fullDateLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val formattedVal = if (selectedPoint.value % 1f == 0f) {
                    selectedPoint.value.roundToInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", selectedPoint.value)
                }
                Text(
                    text = "$formattedVal $unitSuffix".trim(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == safeIdx
                val targetFraction = if (pt.value <= 0f) 0.10f else (pt.value / maxVal).coerceIn(0.14f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "barAnim_$idx"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIndex = idx }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxSize(animatedFraction)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(
                                    brush = when {
                                        pt.value <= 0f -> Brush.verticalGradient(
                                            listOf(
                                                barColor.copy(alpha = if (isSelected) 0.35f else 0.18f),
                                                barColor.copy(alpha = 0.08f)
                                            )
                                        )
                                        isSelected -> Brush.verticalGradient(
                                            listOf(barColor, barColor.copy(alpha = 0.72f))
                                        )
                                        else -> Brush.verticalGradient(
                                            listOf(barColor.copy(alpha = 0.65f), barColor.copy(alpha = 0.35f))
                                        )
                                    }
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pt.shortDayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        if (allZero) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

enum class ProgressionChartMetric(val label: String, val shortLabel: String) {
    TOP_WEIGHT("Top Weight", "Weight"),
    BEST_REPS("Best Reps", "Reps"),
    SESSION_VOLUME("Session Volume", "Volume"),
    EST_1RM("Est. 1RM", "Est. 1RM")
}

/**
 * Interactive Exercise Progression Line + Area chart for Progressive Overload tracking.
 */
@Composable
fun ExerciseProgressionChart(
    sessions: List<ExerciseSessionSummary>,
    metric: ProgressionChartMetric,
    useLbs: Boolean,
    unitLabel: String,
    recommendedWeightKg: Float = 40f,
    recommendedReps: Int = 8,
    recommendedVolumeKg: Float = 960f,
    recommended1RmKg: Float = 50f,
    accentColor: Color = AppleOverloadOrange,
    modifier: Modifier = Modifier
) {
    val factor = if (useLbs) 2.20462f else 1f
    val suffix = when (metric) {
        ProgressionChartMetric.BEST_REPS -> "reps"
        else -> unitLabel
    }

    val displaySessions = remember(sessions) {
        if (sessions.isNotEmpty()) {
            sessions
        } else {
            listOf(
                ExerciseSessionSummary(
                    sessionId = -1L,
                    sessionTitle = "No Sessions Logged",
                    dateKey = "Week",
                    dateLabel = "Week",
                    timestampMillis = 1L,
                    topWeightKg = 0f,
                    maxRepsAtTopWeight = 0,
                    bestSetReps = 0,
                    totalReps = 0,
                    completedSetCount = 0,
                    sessionVolumeKg = 0f,
                    bestEstimatedOneRepMaxKg = 0f,
                    sets = emptyList()
                ),
                ExerciseSessionSummary(
                    sessionId = -2L,
                    sessionTitle = "No Sessions Logged",
                    dateKey = "Month",
                    dateLabel = "Month",
                    timestampMillis = 2L,
                    topWeightKg = 0f,
                    maxRepsAtTopWeight = 0,
                    bestSetReps = 0,
                    totalReps = 0,
                    completedSetCount = 0,
                    sessionVolumeKg = 0f,
                    bestEstimatedOneRepMaxKg = 0f,
                    sets = emptyList()
                ),
                ExerciseSessionSummary(
                    sessionId = -3L,
                    sessionTitle = "No Sessions Logged",
                    dateKey = "Year",
                    dateLabel = "Year",
                    timestampMillis = 3L,
                    topWeightKg = 0f,
                    maxRepsAtTopWeight = 0,
                    bestSetReps = 0,
                    totalReps = 0,
                    completedSetCount = 0,
                    sessionVolumeKg = 0f,
                    bestEstimatedOneRepMaxKg = 0f,
                    sets = emptyList()
                )
            )
        }
    }

    val isEmptyHistory = sessions.isEmpty()

    val values = displaySessions.map { s ->
        when (metric) {
            ProgressionChartMetric.TOP_WEIGHT -> s.topWeightKg * factor
            ProgressionChartMetric.BEST_REPS -> s.bestSetReps.toFloat()
            ProgressionChartMetric.SESSION_VOLUME -> s.sessionVolumeKg * factor
            ProgressionChartMetric.EST_1RM -> s.bestEstimatedOneRepMaxKg * factor
        }
    }

    var selectedIdx by remember(displaySessions, metric) { mutableIntStateOf(displaySessions.lastIndex) }
    val safeIdx = selectedIdx.coerceIn(0, displaySessions.lastIndex)
    val selectedSession = displaySessions[safeIdx]
    val selectedValue = values[safeIdx]

    val minVal = (values.minOrNull() ?: 0f)
    val maxVal = (values.maxOrNull() ?: 1f)
    val valueSpan = (maxVal - minVal).coerceAtLeast(1f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEmptyHistory) {
                            "0 Sessions Logged"
                        } else {
                            "${selectedSession.dateLabel} • ${selectedSession.sessionTitle}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isEmptyHistory) {
                            "Log workouts to track progression across weeks, months & years"
                        } else {
                            "${selectedSession.completedSetCount} sets • ${selectedSession.totalReps} total reps"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${selectedValue.roundToInt()} $suffix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height

            for (i in 0..2) {
                val y = h * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val stepX = if (values.size > 1) w / (values.size - 1).toFloat() else 0f
            val pointsOffsets = values.mapIndexed { idx, v ->
                val x = if (values.size > 1) idx * stepX else w / 2f
                val normalized = if (values.size == 1 || maxVal == minVal) {
                    0.5f
                } else {
                    (v - minVal) / valueSpan
                }
                val y = h - (normalized * (h * 0.76f) + h * 0.12f)
                Offset(x, y)
            }

            if (pointsOffsets.size > 1) {
                val areaPath = Path().apply {
                    moveTo(pointsOffsets.first().x, h)
                    pointsOffsets.forEach { pt -> lineTo(pt.x, pt.y) }
                    lineTo(pointsOffsets.last().x, h)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.26f), Color.Transparent)
                    )
                )

                val linePath = Path().apply {
                    moveTo(pointsOffsets.first().x, pointsOffsets.first().y)
                    for (i in 1 until pointsOffsets.size) {
                        lineTo(pointsOffsets[i].x, pointsOffsets[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            pointsOffsets.forEachIndexed { idx, offset ->
                val isSelected = idx == safeIdx
                drawCircle(
                    color = if (isSelected) accentColor else accentColor.copy(alpha = 0.78f),
                    radius = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx(),
                    center = offset
                )
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.8.dp.toPx(),
                        center = offset
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displaySessions.takeLast(5).forEach { sess ->
                val actualIdx = displaySessions.indexOf(sess)
                val isSelected = actualIdx == safeIdx
                Text(
                    text = sess.dateLabel,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent
                        )
                        .clickable { selectedIdx = actualIdx }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Measurable Strength-to-Bodyweight & Overload Standard Gauge Bar.
 * Shows where an exercise's current or target lift sits relative to bodyweight standards.
 */
@Composable
fun StrengthStandardGaugeBar(
    exerciseName: String,
    currentOrTargetWeightKg: Float,
    userBodyweightKg: Float,
    unitLabel: String,
    useLbs: Boolean,
    modifier: Modifier = Modifier
) {
    val safeBw = userBodyweightKg.coerceAtLeast(45f)
    val hasWeight = currentOrTargetWeightKg > 0f
    val ratio = if (hasWeight) (currentOrTargetWeightKg / safeBw).coerceIn(0f, 2.5f) else 0f
    val progressTarget = if (hasWeight) (ratio / 2.0f).coerceIn(0.05f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "strengthGauge"
    )

    val tierLabel = when {
        !hasWeight -> "0 $unitLabel Logged"
        ratio >= 1.5f -> "Advanced / Elite"
        ratio >= 1.0f -> "Intermediate+"
        ratio >= 0.65f -> "Developing"
        else -> "Foundation"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength-to-Bodyweight Index (${String.format(Locale.US, "%.2f", ratio)}× BW)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tierLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppleOverloadOrange
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(AppleStandBlue, AppleExerciseGreen, AppleOverloadOrange, AppleMoveRed)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.5× Novice", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("1.0× Interm.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("1.5× Advanced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("2.0× Elite", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Live Set-by-Set Volume & Load Mini Graph shown inside the Active Workout Logger.
 */
@Composable
fun LiveWorkoutSetVolumeGraph(
    sets: List<DraftSetEntry>,
    useLbs: Boolean,
    unitLabel: String,
    modifier: Modifier = Modifier
) {
    if (sets.isEmpty()) return
    val factor = if (useLbs) 2.20462f else 1f
    val volumes = sets.map { s ->
        if (s.status == "COMPLETED") (s.weightKg * factor * s.reps).coerceAtLeast(10f) else 5f
    }
    val maxVol = (volumes.maxOrNull() ?: 100f).coerceAtLeast(10f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Live Set-by-Set Volume Graph (${sets.size} sets)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Peak Set: ${volumes.maxOrNull()?.roundToInt() ?: 0} $unitLabel",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppleExerciseGreen
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            sets.take(14).forEachIndexed { idx, set ->
                val v = volumes[idx]
                val frac by animateFloatAsState(
                    targetValue = (v / maxVol).coerceIn(0.15f, 1f),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "liveSetBar_$idx"
                )
                val barColor = when (set.status) {
                    "COMPLETED" -> AppleExerciseGreen
                    "WARMUP" -> AppleOverloadOrange
                    else -> MaterialTheme.colorScheme.outline
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .fillMaxSize(frac)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(barColor)
                        )
                    }
                    Text(
                        text = "S${idx + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Multi-Color Muscle Group Volume & Set Distribution Graph.
 */
@Composable
fun MuscleVolumeDistributionGraph(
    muscleBreakdown: List<MuscleGroupStat>,
    formatVolume: (Float) -> String,
    modifier: Modifier = Modifier
) {
    val defaultMuscles = if (muscleBreakdown.isNotEmpty()) {
        muscleBreakdown
    } else {
        listOf(
            MuscleGroupStat("Chest", 0, 0f, 0f),
            MuscleGroupStat("Back", 0, 0f, 0f),
            MuscleGroupStat("Legs", 0, 0f, 0f),
            MuscleGroupStat("Shoulders", 0, 0f, 0f),
            MuscleGroupStat("Arms", 0, 0f, 0f)
        )
    }

    val palette = listOf(AppleMoveRed, AppleStandBlue, AppleExerciseGreen, AppleOverloadOrange, AppleMindPurple)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        defaultMuscles.take(6).forEachIndexed { idx, mg ->
            val color = palette[idx % palette.size]
            val targetProgress = if (mg.completedSets > 0) {
                (mg.percentageOfSets / 100f).coerceIn(0.08f, 1f)
            } else {
                0.04f
            }
            val animProgress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                label = "muscleDist_$idx"
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = mg.muscleGroup,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${mg.completedSets} sets (${mg.percentageOfSets.roundToInt()}%) • ${formatVolume(mg.totalVolumeKg)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.65f))))
                    )
                }
            }
        }
    }
}

/**
 * Interactive Monthly Gym Attendance Calendar Grid.
 */
@Composable
fun AttendanceCalendarGrid(
    cells: List<CalendarDayCell>,
    selectedDateKey: String?,
    onSelectDate: (CalendarDayCell) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val rows = cells.chunked(7)
        rows.forEach { weekRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val cell = weekRow.getOrNull(col)
                    if (cell == null || !cell.isCurrentMonth) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val isSelected = cell.dateKey == selectedDateKey
                        val isAttended = cell.status == CalendarAttendanceStatus.ATTENDED
                        val bgColor = if (isAttended) {
                            AppleMoveRed
                        } else {
                            AppleStandBlue.copy(alpha = 0.16f)
                        }
                        val textColor = if (isAttended) {
                            Color.White
                        } else {
                            AppleStandBlue
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else if (cell.isToday && !isAttended) {
                                        Modifier.border(2.dp, AppleMoveRed, CircleShape)
                                    } else if (!isAttended) {
                                        Modifier.border(1.2.dp, AppleStandBlue.copy(alpha = 0.58f), CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectDate(cell) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = cell.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                if (cell.visitCount > 1) {
                                    Text(
                                        text = "×${cell.visitCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.92f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarLegendItem(color = AppleMoveRed, label = "Attended")
            CalendarLegendItem(color = AppleStandBlue, label = "Rest Day", outlined = true)
        }
    }
}

@Composable
private fun CalendarLegendItem(
    color: Color,
    label: String,
    outlined: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (outlined) color.copy(alpha = 0.15f) else color)
                .then(
                    if (outlined) Modifier.border(1.dp, color, CircleShape) else Modifier
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
