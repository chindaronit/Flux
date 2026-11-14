package com.omnipad.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnipad.R
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventOccurrence
import com.omnipad.data.model.configuredDurationMillis
import com.omnipad.data.model.displayedEventColor
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 24 * HOUR_MILLIS
private const val DEFAULT_DURATION = 30 * 60_000L
private const val MIN_DISPLAY_DURATION = 20 * 60_000L

internal data class TimelineEntry(
    val occurrence: EventOccurrence,
    val start: Long,
    val end: Long,
    val column: Int,
    val columns: Int
)

internal fun dayTimelineEntries(
    date: LocalDate,
    events: List<EventModel>,
    exceptions: List<EventExceptionModel>,
    instances: List<EventInstanceModel>,
    showCompletedEvents: Boolean,
    zone: ZoneId = ZoneId.systemDefault()
): Pair<List<EventOccurrence>, List<TimelineEntry>> {
    val maxDuration = events.filterNot { it.isAllDay }
        .maxOfOrNull { it.configuredDurationMillis() ?: DEFAULT_DURATION } ?: DEFAULT_DURATION
    val lookback = (maxDuration / DAY_MILLIS + 1).coerceAtMost(ChronoUnit.DAYS.between(LocalDate.MIN, date))
    val occurrences = filterCompletedEventOccurrences(
        findEventOccurrences(events, exceptions, date.minusDays(lookback), date),
        instances,
        showCompletedEvents
    )
    val allDay = occurrences.filter { it.event.isAllDay && it.date == date }
    val intervals = occurrences.filterNot { it.event.isAllDay }.mapNotNull { occurrence ->
        val startTime = Instant.ofEpochMilli(occurrence.event.startDateTime)
            .atZone(zone).toLocalTime().toNanoOfDay() / 1_000_000L
        val dayOffset = ChronoUnit.DAYS.between(date, occurrence.date)
        // One-off events outside the requested range are also returned by findEventOccurrences.
        if (dayOffset > 0 || dayOffset < -lookback) return@mapNotNull null
        val start = dayOffset * DAY_MILLIS + startTime
        val duration = occurrence.event.configuredDurationMillis() ?: DEFAULT_DURATION
        val end = if (duration > Long.MAX_VALUE - start.coerceAtLeast(0)) Long.MAX_VALUE else start + duration
        if (start >= DAY_MILLIS || end <= 0) null
        else Triple(occurrence, start.coerceAtLeast(0), end.coerceAtMost(DAY_MILLIS))
    }.sortedWith(compareBy({ it.second }, { it.third }, { it.first.event.id }))

    val result = mutableListOf<TimelineEntry>()
    var group = mutableListOf<TimelineEntry>()
    val columnEnds = mutableListOf<Long>()
    var groupEnd = 0L
    fun finishGroup() {
        val columns = columnEnds.size
        result.addAll(group.map { it.copy(columns = columns) })
        group = mutableListOf()
        columnEnds.clear()
    }
    intervals.forEach { (occurrence, start, end) ->
        if (group.isNotEmpty() && start >= groupEnd) finishGroup()
        val column = columnEnds.indexOfFirst { it <= start }.let { if (it < 0) columnEnds.size else it }
        val displayEnd = maxOf(end, start + MIN_DISPLAY_DURATION).coerceAtMost(DAY_MILLIS)
        if (column == columnEnds.size) columnEnds.add(displayEnd) else columnEnds[column] = displayEnd
        groupEnd = maxOf(if (group.isEmpty()) 0 else groupEnd, displayEnd)
        group.add(TimelineEntry(occurrence, start, end, column, 0))
    }
    if (group.isNotEmpty()) finishGroup()
    return allDay to result
}

@Composable
internal fun EventDayTimeline(
    date: LocalDate,
    events: List<EventModel>,
    exceptions: List<EventExceptionModel>,
    instances: List<EventInstanceModel>,
    showCompletedEvents: Boolean,
    completed: Set<Pair<String, Long>>,
    colors: Map<Pair<String, Long>, Int>,
    completedColorArgb: Int?,
    is24HourFormat: Boolean,
    radius: Int,
    onEventClick: (EventOccurrence) -> Unit
) {
    val (allDay, entries) = remember(date, events, exceptions, instances, showCompletedEvents) {
        dayTimelineEntries(date, events, exceptions, instances, showCompletedEvents)
    }
    val hourHeight = 72.dp
    val initialHour = entries.minOfOrNull { it.start }?.div(HOUR_MILLIS)?.toInt() ?: 8
    val initialScroll = with(LocalDensity.current) { (hourHeight * (initialHour - 1).coerceAtLeast(0)).roundToPx() }
    val scrollState = rememberScrollState(initial = initialScroll)
    Column(Modifier.fillMaxSize()) {
        if (allDay.isNotEmpty()) {
            Text(
                stringResource(R.string.event_all_day),
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
            Column(
                modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                allDay.forEach { occurrence ->
                    val event = occurrence.event
                    val key = event.id to occurrence.instanceDate
                    EventCard(
                        radius = radius,
                        is24HourFormat = is24HourFormat,
                        isPending = key !in completed,
                        title = event.title,
                        repeat = event.recurrence,
                        startDateTime = event.startDateTime,
                        isAllDay = true,
                        colorArgb = colors[key] ?: event.colorArgb,
                        completedColorArgb = completedColorArgb,
                        onClick = { onEventClick(occurrence) },
                        onLongPressed = { onEventClick(occurrence) }
                    )
                }
            }
        }
        if (allDay.isEmpty() && entries.isEmpty()) {
            Text(stringResource(R.string.Empty_Events), Modifier.padding(vertical = 8.dp))
        }
        Row(Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(top = 8.dp)) {
            val formatter = remember(is24HourFormat) {
                DateTimeFormatter.ofPattern(if (is24HourFormat) "HH:mm" else "h a", Locale.getDefault())
            }
            Column(Modifier.width(52.dp)) {
                repeat(24) { hour ->
                    Text(
                        LocalTime.of(hour, 0).format(formatter),
                        Modifier.height(hourHeight),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BoxWithConstraints(Modifier.weight(1f).height(hourHeight * 24)) {
                repeat(24) { hour ->
                    Box(
                        Modifier.fillMaxWidth()
                            .offset(y = hourHeight * hour)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                entries.forEach { entry ->
                    val event = entry.occurrence.event
                    val key = event.id to entry.occurrence.instanceDate
                    val displayColor = displayedEventColor(colors[key] ?: event.colorArgb, completedColorArgb, key !in completed)
                    val background = displayColor?.let(::Color)
                        ?: if (key in completed) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                    val content = displayColor?.let(::eventTextColor)
                        ?: if (key in completed) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    val columnWidth = maxWidth / entry.columns
                    val top = hourHeight * (entry.start.toFloat() / HOUR_MILLIS)
                    val height = hourHeight * ((entry.end - entry.start).toFloat() / HOUR_MILLIS)
                    val remaining = hourHeight * 24 - top
                    Box(
                        Modifier.offset(x = columnWidth * entry.column, y = top)
                            .width(columnWidth)
                            .height(maxOf(height, 24.dp).coerceAtMost(remaining))
                            .padding(end = 2.dp, bottom = 1.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(background)
                            .clickable { onEventClick(entry.occurrence) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            event.title,
                            color = content,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
