package com.omnipad.ui.screens.events

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnipad.R
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.EventOccurrence
import com.omnipad.data.model.EventOccurrenceColorModel
import com.omnipad.data.model.displayedEventColor
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.text.NumberFormat
import kotlinx.coroutines.delay

data class CalendarFocusRequest(val eventId: String, val date: LocalDate, val sequence: Int) {
    val month: YearMonth get() = YearMonth.from(date)

    fun matches(occurrence: EventOccurrence): Boolean =
        eventId == occurrence.event.id && date == occurrence.date
}

internal fun calendarDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val last = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return (0..java.time.temporal.ChronoUnit.DAYS.between(first, last).toInt())
        .map { first.plusDays(it.toLong()) }
}

internal fun calendarMonthOffset(year: Int, month: Month, currentMonth: YearMonth): Int? {
    val offset = ChronoUnit.MONTHS.between(currentMonth, YearMonth.of(year, month))
    return offset.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
}

internal fun calendarYearsForMonth(currentMonth: YearMonth, month: Month): IntRange {
    val first = currentMonth.plusMonths(Int.MIN_VALUE.toLong())
    val last = currentMonth.plusMonths(Int.MAX_VALUE.toLong())
    val firstYear = first.year + if (month.value < first.monthValue) 1 else 0
    val lastYear = last.year - if (month.value > last.monthValue) 1 else 0
    return firstYear..lastYear
}

@Composable
private fun CalendarMonthPicker(
    current: YearMonth,
    currentMonth: YearMonth,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_choose_month)) },
        text = {
            Column {
                Month.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            val offset = calendarMonthOffset(current.year, option, currentMonth)
                            val isSelected = option == current.month
                            val description = option.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                            TextButton(
                                enabled = offset != null,
                                onClick = { offset?.let(onSelect) },
                                modifier = Modifier.weight(1f).semantics {
                                    contentDescription = description
                                    selected = isSelected
                                },
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                            ) {
                                Text(option.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()), maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.Cancel)) }
        }
    )
}

@Composable
private fun CalendarYearPicker(
    current: YearMonth,
    currentMonth: YearMonth,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = remember(currentMonth, current.month) { calendarYearsForMonth(currentMonth, current.month) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (current.year - years.first - 8).coerceAtLeast(0))
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()).apply { isGroupingUsed = false } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_choose_year)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.fillMaxWidth().height(288.dp)
            ) {
                items(count = years.last - years.first + 1) { index ->
                    val year = years.first + index
                    val isSelected = year == current.year
                    val offset = calendarMonthOffset(year, current.month, currentMonth)
                    TextButton(
                        onClick = { offset?.let(onSelect) },
                        enabled = offset != null,
                        modifier = Modifier.height(48.dp).semantics { selected = isSelected },
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                    ) {
                        Text(formatter.format(year), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.Cancel)) } }
    )
}

@Composable
fun EventCalendar(
    events: List<EventModel>,
    exceptions: List<EventExceptionModel>,
    instances: List<EventInstanceModel>,
    occurrenceColors: List<EventOccurrenceColorModel>,
    showCompletedEvents: Boolean,
    completedColorArgb: Int?,
    monthOffset: Int,
    onMonthOffsetChange: (Int) -> Unit,
    onEventClick: (EventOccurrence) -> Unit,
    modifier: Modifier = Modifier,
    focusRequest: CalendarFocusRequest? = null
) {
    val currentMonth = YearMonth.now()
    val month = currentMonth.plusMonths(monthOffset.toLong())
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var highlighted by remember { mutableStateOf<CalendarFocusRequest?>(null) }
    val dayFlash = remember { Animatable(0f) }
    var flashedRequest by remember { mutableStateOf<CalendarFocusRequest?>(null) }
    var flashActive by remember { mutableStateOf(false) }
    LaunchedEffect(focusRequest) {
        if (focusRequest != null) {
            onMonthOffsetChange(ChronoUnit.MONTHS.between(currentMonth, focusRequest.month).toInt())
            highlighted = focusRequest
            delay(4000)
            highlighted = null
        } else {
            highlighted = null
        }
    }
    val dates = remember(month) { calendarDates(month) }
    val occurrencesByDate = remember(events, exceptions, instances, showCompletedEvents, month) {
        filterCompletedEventOccurrences(
            findEventOccurrences(events, exceptions, dates.first(), dates.last()),
            instances,
            showCompletedEvents
        ).groupBy { it.date }
    }
    val completed = remember(instances) { instances.map { it.eventId to it.instanceDate }.toSet() }
    val colors = remember(occurrenceColors) {
        occurrenceColors.associate { (it.eventId to it.occurrenceDate) to it.colorArgb }
    }
    val today = LocalDate.now()

    if (showMonthPicker) {
        CalendarMonthPicker(
            current = month,
            currentMonth = currentMonth,
            onSelect = {
                onMonthOffsetChange(it)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
    if (showYearPicker) {
        CalendarYearPicker(
            current = month,
            currentMonth = currentMonth,
            onSelect = {
                onMonthOffsetChange(it)
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthLabel = month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                val yearLabel = remember(month.year) {
                    NumberFormat.getIntegerInstance(Locale.getDefault()).apply { isGroupingUsed = false }.format(month.year)
                }
                val monthDescription = stringResource(R.string.event_choose_month_description, monthLabel)
                val yearDescription = stringResource(R.string.event_choose_year_description, yearLabel)
                TextButton(
                    onClick = { showMonthPicker = true },
                    modifier = Modifier.weight(1f, fill = false).semantics { contentDescription = monthDescription },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(monthLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                TextButton(
                    onClick = { showYearPicker = true },
                    modifier = Modifier.semantics { contentDescription = yearDescription },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(yearLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = { onMonthOffsetChange(monthOffset - 1) }, enabled = monthOffset > Int.MIN_VALUE) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, stringResource(R.string.event_previous_month))
            }
            IconButton(onClick = { onMonthOffsetChange(monthOffset + 1) }, enabled = monthOffset < Int.MAX_VALUE) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, stringResource(R.string.event_next_month))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        day.getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            dates.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    week.forEach { date ->
                        val flashColor = MaterialTheme.colorScheme.primary
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 110.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .drawWithContent {
                                    drawContent()
                                    if (flashActive && highlighted?.date == date && YearMonth.from(date) == month) {
                                        drawRect(flashColor.copy(alpha = dayFlash.value))
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (date == today) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (YearMonth.from(date) == month) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    }
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                occurrencesByDate[date].orEmpty().forEach { occurrence ->
                                    val key = occurrence.event.id to occurrence.instanceDate
                                    val isHighlighted = highlighted?.matches(occurrence) == true
                                    val bringIntoViewRequester = remember(key) { BringIntoViewRequester() }
                                    LaunchedEffect(isHighlighted, month) {
                                        if (isHighlighted && YearMonth.from(date) == month) {
                                            bringIntoViewRequester.bringIntoView()
                                            val request = highlighted
                                            if (request != null && flashedRequest != request) {
                                                flashedRequest = request
                                                dayFlash.snapTo(0f)
                                                flashActive = true
                                                try {
                                                    repeat(2) {
                                                        dayFlash.animateTo(0.45f, tween(300))
                                                        dayFlash.animateTo(0f, tween(300))
                                                    }
                                                } finally {
                                                    flashActive = false
                                                }
                                            }
                                        }
                                    }
                                    val isCompleted = key in completed
                                    val eventColor = colors[key] ?: occurrence.event.colorArgb
                                    val displayColor = displayedEventColor(eventColor, completedColorArgb, !isCompleted)
                                    val background = displayColor?.let(::Color)
                                        ?: if (isCompleted) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                                    val content = displayColor?.let(::eventTextColor)
                                        ?: if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    Text(
                                        text = occurrence.event.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(background)
                                            .then(
                                                if (isHighlighted) Modifier.border(
                                                    2.dp, content, RoundedCornerShape(4.dp)
                                                ) else Modifier
                                            )
                                            .clickable { onEventClick(occurrence) }
                                            .padding(horizontal = 3.dp, vertical = 3.dp),
                                        color = content,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
