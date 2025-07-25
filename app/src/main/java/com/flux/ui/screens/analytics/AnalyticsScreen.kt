package com.flux.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.EventStatus
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.JournalModel
import com.flux.data.model.Repetition
import com.flux.data.model.WorkspaceModel
import com.flux.ui.components.ActionType
import com.flux.ui.components.SettingOption
import com.flux.ui.components.shapeManager
import com.flux.ui.theme.completed
import com.flux.ui.theme.failed
import com.flux.ui.theme.pending
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun Analytics(
    workspace: WorkspaceModel,
    radius: Int,
    allHabitInstances: List<HabitInstanceModel>,
    totalHabits: Int,
    totalNotes: Int,
    journalEntries: List<JournalModel>,
    allEvents: List<EventModel>,
    allEventInstances: List<EventInstanceModel>
) {
    if(!workspace.isNotesAdded &&  !workspace.isJournalAdded && !workspace.isEventsAdded && !workspace.isHabitsAdded){ EmptyAnalytics() }
    LazyColumn(
        Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (workspace.isNotesAdded){
            item {
                SettingOption(
                    title = stringResource(R.string.Notes),
                    description = totalNotes.toString(),
                    icon = Icons.AutoMirrored.Default.Notes,
                    radius = shapeManager(radius = radius, isBoth = true),
                    actionType = ActionType.None
                )
            }
        }
        if(workspace.isJournalAdded){ item { JournalAnalytics(radius, journalEntries) } }
        if (workspace.isEventsAdded) { item { ChartCirclePie(radius=radius, weeklyEventStats = calculateWeeklyEventStats(allEvents, allEventInstances)) } }
        if(workspace.isHabitsAdded) item { HabitHeatMap(radius, allHabitInstances, totalHabits) }
    }
}

data class WeeklyEventStats(
    val upcoming: Int,
    val completed: Int,
    val failed: Int
)

fun calculateWeeklyEventStats(
    events: List<EventModel>,
    instances: List<EventInstanceModel>
): WeeklyEventStats {
    val zoneId = ZoneId.systemDefault()
    val now = LocalDateTime.now(zoneId)
    val today = now.toLocalDate()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val weekEnd = today.with(DayOfWeek.SUNDAY)

    val instanceMap = instances.associateBy { it.eventId to it.instanceDate }

    var upcoming = 0
    var completed = 0
    var failed = 0

    fun dateRange(start: LocalDate, endInclusive: LocalDate): Sequence<LocalDate> =
        generateSequence(start) { it.plusDays(1) }.takeWhile { it <= endInclusive }


    events.forEach { event ->
        val baseDateTime = Instant.ofEpochMilli(event.startDateTime)
            .atZone(zoneId)
            .toLocalDateTime()

        for (date in dateRange(weekStart, weekEnd))  {
            val matches = when (event.repetition) {
                Repetition.DAILY -> true
                Repetition.WEEKLY -> baseDateTime.dayOfWeek == date.dayOfWeek
                Repetition.MONTHLY -> baseDateTime.dayOfMonth == date.dayOfMonth
                Repetition.YEARLY -> baseDateTime.dayOfYear == date.dayOfYear
                Repetition.NONE -> baseDateTime.toLocalDate() == date
            }

            if (!matches) continue

            val instance = instanceMap[event.eventId to date]
            val instanceDateTime = baseDateTime.with(date)

            val status = instance?.status ?: EventStatus.PENDING

            if (status == EventStatus.COMPLETED) {
                completed++
            } else {
                val eventHasPassed = instanceDateTime.isBefore(now)
                if (eventHasPassed) {
                    failed++
                } else {
                    upcoming++
                }
            }
        }
    }

    return WeeklyEventStats(upcoming, completed, failed)
}


@Composable
fun HabitHeatMap(radius: Int, allHabitInstances: List<HabitInstanceModel>, totalHabits: Int){
    val today = LocalDate.now()
    val yearStart = LocalDate.of(today.year, 1, 1)

    // Calculate the offset from Monday for January 1st
    val jan1DayOfWeek = yearStart.dayOfWeek.value // Monday = 1, Sunday = 7
    val offsetFromMonday = jan1DayOfWeek - 1 // 0 for Monday, 6 for Sunday

    val totalDays = ChronoUnit.DAYS.between(yearStart, today).toInt() + 1
    val allDates = (0 until totalDays).map { yearStart.plusDays(it.toLong()) }

    val habitMap = remember(allHabitInstances) {
        allHabitInstances.groupBy { it.instanceDate }
    }

    // Create week columns with proper day alignment
    val weekColumns = mutableListOf<List<LocalDate?>>()
    var currentWeek = MutableList<LocalDate?>(7) { null }

    // Fill the first week with nulls for days before January 1st
    for (i in 0 until offsetFromMonday) {
        currentWeek[i] = null
    }

    // Add all dates starting from the correct day of week
    allDates.forEachIndexed { index, date ->
        val dayIndex = (offsetFromMonday + index) % 7
        currentWeek[dayIndex] = date

        // When we complete a week (reach Sunday) or it's the last date
        if (dayIndex == 6 || index == allDates.size - 1) {
            weekColumns.add(currentWeek.toList())
            currentWeek = MutableList(7) { null }
        }
    }
    val boxSize = 24.dp
    val lazyListState = rememberLazyListState()

    // Calculate the index of the current month's first week
    val currentMonthStartIndex = remember(weekColumns) {
        val currentMonth = today.month
        weekColumns.indexOfFirst { week ->
            week.any { date -> date?.month == currentMonth }
        }.takeIf { it != -1 } ?: 0
    }

    val todayHabit = allHabitInstances.count { it.instanceDate == today }

    // Auto-scroll to current month on first composition
    LaunchedEffect(currentMonthStartIndex) {
        if (currentMonthStartIndex > 0) {
            lazyListState.scrollToItem(
                index = maxOf(0, currentMonthStartIndex - 2)
            )
        }
    }

    Card(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        shape = shapeManager(radius = radius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.HeatMap), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold
            ), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("${stringResource(R.string.Completed_Today)}: $todayHabit/$totalHabits", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .width(boxSize)
                        .padding(top = 26.dp, end = 2.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        Box(
                            modifier = Modifier
                                .width(boxSize + 12.dp)
                                .height(boxSize),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = day.name.take(3),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Combined month + heatmap
                LazyRow(
                    state = lazyListState,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(weekColumns) { index, columnDates ->
                        val firstDate = columnDates.firstOrNull()
                        val month = firstDate?.month

                        // Show month label if this is the first week of the month
                        // or if it's the very first column
                        val showMonth = month != null && (index == 0 || weekColumns.getOrNull(index - 1)?.firstOrNull()?.month != month)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Month label on top (only once per month)
                            Box(
                                modifier = Modifier.height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showMonth) {
                                    Text(
                                        text = firstDate.month.name.take(3),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Heatmap boxes
                            columnDates.forEachIndexed { dayIndex, date ->
                                if (date != null) {
                                    val count = habitMap[date]?.size ?: 0
                                    val intensity = (count / if(totalHabits>0) totalHabits.toFloat() else 2f).coerceIn(0f, 1f)
                                    val color = lerp(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        MaterialTheme.colorScheme.primary,
                                        intensity
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(boxSize)
                                            .background(color, RoundedCornerShape(3.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 9.sp,
                                            color = if (intensity > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.size(boxSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ChartModel(
    val value: Float,
    val color: Color,
)

@Composable
private fun ChartCirclePie(
    radius: Int,
    modifier: Modifier = Modifier,
    weeklyEventStats: WeeklyEventStats,
    size: Dp = 120.dp,
    strokeWidth: Dp = 16.dp
) {
    val upcomingEvents = weeklyEventStats.upcoming
    val completedEvents = weeklyEventStats.completed
    val failedEvents = weeklyEventStats.failed
    val total = upcomingEvents + completedEvents + failedEvents

    val charts = if (total == 0) {
        listOf(
            ChartModel(0.33f, pending),
            ChartModel(0.33f, completed),
            ChartModel(0.34f, failed)
        )
    } else {
        listOf(
            ChartModel(upcomingEvents.toFloat() / total, pending),
            ChartModel(completedEvents.toFloat() / total, completed),
            ChartModel(failedEvents.toFloat() / total, failed)
        )
    }

    Card(
        shape = shapeManager(radius = radius),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = modifier.size(size),
                onDraw = {
                    var startAngle = -90f
                    charts.forEach { chart ->
                        val sweepAngle = chart.value * 360f
                        drawArc(
                            color = chart.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(
                                width = strokeWidth.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                        startAngle += sweepAngle
                    }
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    stringResource(R.string.This_Week),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 30.dp, height = 10.dp).background(pending))
                    Spacer(Modifier.width(8.dp))
                    Text("${stringResource(R.string.Upcoming)}: $upcomingEvents", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 30.dp, height = 10.dp).background(completed))
                    Spacer(Modifier.width(8.dp))
                    Text("${stringResource(R.string.Completed)}: $completedEvents", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 30.dp, height = 10.dp).background(failed))
                    Spacer(Modifier.width(8.dp))
                    Text("${stringResource(R.string.Failed)}: $failedEvents", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun JournalAnalytics(radius: Int, entries: List<JournalModel>){
    val (thisWeek, thisMonth)=countJournalsThisWeekAndMonth(entries)
    val daysInMonth = LocalDate.now().lengthOfMonth()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        JournalAnalyticsCard(
            radius=radius,
            modifier = Modifier.weight(0.5f),
            progress = thisWeek/7f,
            title = stringResource(R.string.This_Week),
            bestStreak = calculateWeeklyStreak(entries),
            journalsWritten = thisWeek
        )

        JournalAnalyticsCard(
            radius=radius,
            modifier = Modifier.weight(0.5f),
            progress = thisMonth.toFloat()/daysInMonth.toFloat(),
            title = stringResource(R.string.This_Month),
            bestStreak = calculateMonthlyStreak(entries),
            journalsWritten = thisMonth
        )
    }
}

@Composable
fun JournalAnalyticsCard(
    radius: Int,
    modifier: Modifier= Modifier,
    title: String=stringResource(R.string.This_Week),
    progress: Float,
    journalsWritten: Int,
    bestStreak: Int
){
    Card(
        shape = shapeManager(radius=radius),
        modifier = modifier,
        onClick = {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        Column(Modifier
            .fillMaxSize()
            .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.primary.copy(0.35f),
                strokeCap = StrokeCap.Round,
            )

            Row(Modifier.align(Alignment.Start).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.journals_written, journalsWritten), style = MaterialTheme.typography.labelMedium)
            }
            Row(
                Modifier.align(Alignment.Start).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.LocalFireDepartment, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.best_streak, bestStreak), style = MaterialTheme.typography.labelMedium)
            }

            Row(Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.completion_percentage, (progress * 100).toInt()), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}


fun calculateWeeklyStreak(entries: List<JournalModel>): Int {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val endOfWeek = today.with(DayOfWeek.SUNDAY)

    // Extract distinct dates within this week
    val entryDatesThisWeek = entries
        .map {
            Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate()
        }
        .filter { it in startOfWeek..endOfWeek }
        .toSet()

    // Count consecutive streaks starting from Monday to Sunday
    var currentStreak = 0
    var maxStreak = 0

    for (i in 0..6) {
        val date = startOfWeek.plusDays(i.toLong())
        if (entryDatesThisWeek.contains(date)) {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else {
            currentStreak = 0
        }
    }

    return maxStreak
}

fun calculateMonthlyStreak(entries: List<JournalModel>): Int {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val startOfMonth = today.withDayOfMonth(1)
    val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

    // Extract distinct journal dates in current month
    val entryDatesInMonth = entries
        .map { Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate() }
        .filter { it in startOfMonth..endOfMonth }
        .distinct()
        .sorted()

    if (entryDatesInMonth.isEmpty()) return 0

    var maxStreak = 1
    var currentStreak = 1

    for (i in 1 until entryDatesInMonth.size) {
        val prev = entryDatesInMonth[i - 1]
        val curr = entryDatesInMonth[i]
        if (prev.plusDays(1) == curr) {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else {
            currentStreak = 1
        }
    }

    return maxStreak
}

fun countJournalsThisWeekAndMonth(entries: List<JournalModel>): Pair<Int, Int> {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)

    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val startOfMonth = today.withDayOfMonth(1)

    val journalsThisWeek = entries.count {
        val date = Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate()
        date in startOfWeek..today
    }

    val journalsThisMonth = entries.count {
        val date = Instant.ofEpochMilli(it.dateTime).atZone(zoneId).toLocalDate()
        date in startOfMonth..today
    }

    return journalsThisWeek to journalsThisMonth
}

@Composable
fun EmptyAnalytics(){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Analytics,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(stringResource(R.string.Empty_Analytics))
    }
}
