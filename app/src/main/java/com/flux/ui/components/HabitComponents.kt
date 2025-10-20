package com.flux.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.RecurrenceRule
import com.flux.ui.events.HabitEvents
import com.flux.ui.screens.events.IconRadioButton
import com.flux.ui.screens.events.toFormattedDate
import com.flux.ui.screens.events.toFormattedTime
import com.flux.ui.screens.habits.isDateAllowedForHabit
import com.flux.ui.state.Settings
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EmptyHabits() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.EventAvailable,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(stringResource(R.string.Empty_Habits))
    }
}

@Composable
fun HabitDateCard(
    radius: Int,
    isTodayDone: Boolean,
    isDone: Boolean,
    day: String,
    date: Int,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isTodayDone && isDone -> MaterialTheme.colorScheme.primary
        isTodayDone && !isDone -> MaterialTheme.colorScheme.primaryContainer
        isDone -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    }

    val contentColor = when {
        isDone -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier,
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                day.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier.alpha(0.95f)
            )
            Text(
                date.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraLight),
                modifier = Modifier.alpha(0.95f)
            )
        }
    }
}

@Composable
fun HabitPreviewCard(
    radius: Int,
    habit: HabitModel,
    instances: List<HabitInstanceModel>,
    settings: Settings,
    onToggleDone: (Long) -> Unit,
    onAnalyticsClicked: () -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val isTodayDone = instances.any { it.instanceDate == todayEpoch }

    // Get Monday of this week
    val mondayEpoch = LocalDate.now().with(DayOfWeek.MONDAY).toEpochDay()
    val weekDates = (0L..6L).map { mondayEpoch + it }
    val (currentStreak, _) = calculateStreaks(habit.recurrence, habit.startDateTime, instances)

    Card(
        onClick = { onToggleDone(todayEpoch) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTodayDone) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        shape = shapeManager(radius = radius * 2)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconRadioButton(
                        selected = isTodayDone,
                    ) { onToggleDone(todayEpoch) }

                    Column(Modifier.padding(top = 8.dp)) {
                        Text(
                            habit.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            habit.startDateTime.toFormattedTime(settings.data.is24HourFormat),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraLight),
                            modifier = Modifier.alpha(0.9f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({}) { Icon(Icons.Default.LocalFireDepartment, null) }
                    Text("$currentStreak")
                    IconButton(onAnalyticsClicked) { Icon(Icons.Default.Analytics, null) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDates.forEach { epochDay ->
                    val localDate = LocalDate.ofEpochDay(epochDay)
                    val isCardDone = instances.any { it.instanceDate == epochDay }

                    HabitDateCard(
                        radius = radius,
                        isTodayDone=isTodayDone,
                        isDone = isCardDone,
                        day = localDate.dayOfWeek.name.take(3),
                        date = localDate.dayOfMonth,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun HabitCalendarCard(
    radius: Int,
    habitId: String,
    workspaceId: String,
    startDateTime: Long,
    recurrence: RecurrenceRule,
    habitInstances: List<HabitInstanceModel>,
    onHabitEvents: (HabitEvents) -> Unit
) {
    val habitStartMonth =
        Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()
            .let { YearMonth.of(it.year, it.month) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val currentYearMonth = YearMonth.now()
    val endOfYear = YearMonth.of(currentYearMonth.year, 12)

    val canGoBack = currentMonth > habitStartMonth
    val canGoForward = currentMonth < endOfYear

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
    val dates = (1..daysInMonth).map { currentMonth.atDay(it) }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val habitStartDate =
        Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        onClick = {},
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (canGoBack) currentMonth = currentMonth.minusMonths(1) },
                    enabled = canGoBack
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBackIos,
                        contentDescription = "Previous Month",
                        modifier = Modifier
                            .alpha(if (canGoBack) 0.8f else 0.3f)
                            .size(16.dp)
                    )
                }
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                            " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = { if (canGoForward) currentMonth = currentMonth.plusMonths(1) },
                    enabled = canGoForward
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowForwardIos,
                        contentDescription = "Next Month",
                        modifier = Modifier
                            .alpha(if (canGoForward) 0.8f else 0.3f)
                            .size(16.dp)
                    )
                }
            }

            // Days of week row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Empty cells before 1st day
                items(firstDayOfWeek) { Box(modifier = Modifier.size(32.dp)) }

                items(dates) { date ->
                    val epochDay = date.toEpochDay()
                    val instance = habitInstances.find { it.instanceDate == epochDay }
                    val isMarked = instance != null

                    val isBeforeStart = date.isBefore(habitStartDate)
                    val isAllowedByRecurrence = isDateAllowedForHabit(recurrence, epochDay)
                    val isClickable = !isBeforeStart && isAllowedByRecurrence

                    val backgroundColor = when {
                        isMarked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        isAllowedByRecurrence && !isBeforeStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        isMarked -> MaterialTheme.colorScheme.onPrimary
                        isAllowedByRecurrence && !isBeforeStart -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }

                    val dateAlpha = when {
                        isBeforeStart -> 0.2f
                        !isAllowedByRecurrence -> 0.4f
                        else -> 1f
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .alpha(dateAlpha)
                            .clickable(enabled = isClickable) {
                                if (isMarked) {
                                    onHabitEvents(HabitEvents.MarkUndone(instance))
                                } else {
                                    onHabitEvents(
                                        HabitEvents.MarkDone(
                                            HabitInstanceModel(
                                                habitId = habitId,
                                                workspaceId = workspaceId,
                                                instanceDate = epochDay
                                            )
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(date.dayOfMonth.toString(), color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun HabitStartCard(startDateTime: Long, radius: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                2.dp
            )
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.Started), modifier = Modifier.alpha(0.85f))
                Text(startDateTime.toFormattedDate(), fontWeight = FontWeight.SemiBold)
            }
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Default.Flag,
                    null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun MonthlyHabitAnalyticsCard(
    radius: Int,
    habitInstances: List<HabitInstanceModel>
) {
    val today = LocalDate.now()
    val currentYearMonth = YearMonth.of(today.year, today.month)
    val daysInMonth = currentYearMonth.lengthOfMonth()

    // Filter only habits from this month
    val thisMonthInstances = remember(habitInstances) {
        habitInstances.filter { instance ->
            val date = LocalDate.ofEpochDay(instance.instanceDate)
            val instanceYearMonth = YearMonth.of(date.year, date.month)
            instanceYearMonth == currentYearMonth
        }
    }

    // Break month into week ranges (1–7, 8–14, etc.)
    val weekRanges = remember(daysInMonth) {
        val ranges = mutableListOf<IntRange>()
        var start = 1
        while (start <= daysInMonth) {
            val end = minOf(start + 6, daysInMonth)
            ranges.add(start..end)
            start = end + 1
        }
        ranges
    }

    // Count completed habits per week
    val weekCounts = remember(thisMonthInstances) {
        val counts = MutableList(weekRanges.size) { 0 }

        thisMonthInstances
            .distinctBy { it.instanceDate }
            .forEach { instance ->
                val date = LocalDate.ofEpochDay(instance.instanceDate)
                val day = date.dayOfMonth
                weekRanges.forEachIndexed { index, range ->
                    if (day in range) {
                        counts[index]++
                        return@forEachIndexed
                    }
                }
            }

        counts
    }

    val completedHabits = weekCounts.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        onClick = {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.This_Month),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(R.string.completed_habits, completedHabits),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            HabitBarChart(
                weekCounts = weekCounts,
                weekLabels = weekRanges.map { "${it.first}-${it.last}" }
            )
        }
    }
}

@Composable
fun HabitBarChart(
    weekCounts: List<Int>,
    weekLabels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxDaysPerWeek = 7
    val yLabels = (maxDaysPerWeek downTo 1).toList()
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(bottom = 16.dp)
    ) {
        // Y-axis labels
        Box(
            modifier = Modifier
                .width(20.dp)
                .padding(end = 8.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepHeight = size.height / maxDaysPerWeek
                val textPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = 32f
                    color = primaryColor.toArgb()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                yLabels.forEach { label ->
                    val y = size.height - (stepHeight * label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label.toString(),
                        size.width - 10f,
                        y + 5f,
                        textPaint
                    )
                }
            }
        }

        // Bar chart canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val barWidth = size.width / (weekCounts.size * 2 + 1)
            val spacing = barWidth
            val stepHeight = size.height / maxDaysPerWeek

            weekCounts.forEachIndexed { index, count ->
                val barHeight = stepHeight * count
                val x = spacing + index * (barWidth + spacing)
                val y = size.height - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(x = 16.dp.toPx(), y = 16.dp.toPx())
                )

                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = primaryColor.toArgb()
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    canvas.nativeCanvas.drawText(
                        weekLabels[index],
                        x + barWidth / 2,
                        size.height + 40f,
                        paint
                    )
                }
            }

            for (i in 1..maxDaysPerWeek) {
                val y = size.height - (stepHeight * i)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.25.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun HabitStreakCard(
    habit: HabitModel,
    instances: List<HabitInstanceModel>,
    radius: Int
) {
    val streakData = remember(habit, instances) {
        calculateStreaks(habit.recurrence, habit.startDateTime, instances)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${stringResource(R.string.Current_Streak)} ${streakData.currentStreak}",
                    modifier = Modifier.alpha(0.85f)
                )
                Text(
                    "${stringResource(R.string.Best_Streak)} ${streakData.bestStreak}",
                    fontWeight = FontWeight.SemiBold
                )
            }
            CircleWrapper(MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

data class StreakData(
    val currentStreak: Int,
    val bestStreak: Int
)

fun calculateStreaks(
    recurrence: RecurrenceRule,
    startDateTime: Long,
    instances: List<HabitInstanceModel>
): StreakData {
    if (instances.isEmpty()) {
        return StreakData(currentStreak = 0, bestStreak = 0)
    }

    // Get all expected dates from habit start to today
    val expectedDates = getExpectedDates(recurrence, startDateTime)
    val completedDates = instances.map { it.instanceDate }.toSet()

    // Calculate current streak (working backwards from today)
    val currentStreak = calculateCurrentStreak(expectedDates, completedDates)

    // Calculate best streak (longest consecutive completed sequence)
    val bestStreak = calculateBestStreak(expectedDates, completedDates)

    return StreakData(currentStreak, bestStreak)
}

private fun getExpectedDates(recurrence: RecurrenceRule, startDateTime: Long): List<Long> {
    val today = LocalDate.now()
    val startDate = Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val expectedDates = mutableListOf<Long>()

    var current = startDate
    while (!current.isAfter(today)) {
        // Convert to Monday=0, Tuesday=1, ..., Sunday=6 format
        val dayOfWeek = (current.dayOfWeek.value + 5) % 7
        if (dayOfWeek in (recurrence as RecurrenceRule.Weekly).daysOfWeek) {
            expectedDates.add(current.toEpochDay())
        }
        current = current.plusDays(1)
    }

    return expectedDates.sorted()
}

private fun calculateCurrentStreak(expectedDates: List<Long>, completedDates: Set<Long>): Int {
    if (expectedDates.isEmpty()) return 0

    val now = System.currentTimeMillis()
    val lastPastIndex = expectedDates.indexOfLast { it <= now }
    if (lastPastIndex == -1) return 0

    var streak = 0
    var i = lastPastIndex

    // Case 1: Today expected but not completed → check yesterday instead
    if (expectedDates[i] !in completedDates) {
        i-- // move to yesterday
        // if yesterday also not complete → streak = 0
        if (i < 0 || expectedDates[i] !in completedDates) return 0
    }

    // Case 2: Count backwards while dates are completed
    while (i >= 0 && expectedDates[i] in completedDates) {
        streak++
        i--
    }

    return streak
}


private fun calculateBestStreak(expectedDates: List<Long>, completedDates: Set<Long>): Int {
    if (expectedDates.isEmpty()) return 0

    var bestStreak = 0
    var currentStreak = 0

    for (expectedDate in expectedDates) {
        if (expectedDate in completedDates) {
            currentStreak++
            bestStreak = maxOf(bestStreak, currentStreak)
        } else {
            currentStreak = 0
        }
    }

    return bestStreak
}
