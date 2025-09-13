package com.flux.ui.screens.events

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.shapeManager
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.Settings
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.eventHomeItems(
    navController: NavController,
    radius: Int,
    isLoading: Boolean,
    allEvents: List<EventModel>,
    allEventInstances: List<EventInstanceModel>,
    settings: Settings,
    workspaceId: String,
    onTaskEvents: (TaskEvents) -> Unit
) {
    if (isLoading) {
        item { Loader() }
    } else if (allEvents.isEmpty()) {
        item { EmptyEvents() }
    } else {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()

        // 🔹 Events scheduled for today
        val todayEvents = allEvents
            .filter { event ->
                occursOnDate(event, today)
            }
            .sortedBy { event ->
                val eventTime = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()

                today.atTime(eventTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

        // 🔹 Upcoming events (after today)
        val upcomingEvents = allEvents
            .filter { event ->
                val eventStartDate = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                eventStartDate.isAfter(today) ||
                        (eventStartDate <= today && event.repetition != "NONE")
            }
            .filter { event ->
                !occursOnDate(event, today)
            }
            .sortedBy { event ->
                val nextOccurrenceDate = calculateNextOccurrence(event, today)
                val eventTime = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()

                if (nextOccurrenceDate != null) {
                    nextOccurrenceDate.atTime(eventTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    Long.MAX_VALUE
                }
            }

        if (todayEvents.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.Today),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(todayEvents) { event ->
                val instance = allEventInstances.find {
                    it.eventId == event.eventId && it.instanceDate == today.toEpochDay()
                }

                EventCard(
                    radius = radius,
                    isAllDay = event.isAllDay,
                    isPending = instance == null,
                    title = event.title,
                    timeline = event.startDateTime,
                    repeat = event.repetition,
                    settings = settings,
                    onChangeStatus = {
                        if(instance == null) {
                            onTaskEvents(
                                TaskEvents.UpsertInstance(
                                    EventInstanceModel(
                                        eventId = event.eventId,
                                        workspaceId = workspaceId,
                                        instanceDate = LocalDate.now().toEpochDay()
                                    )
                                )
                            )
                        }
                        else { onTaskEvents(TaskEvents.DeleteInstance(instance)) }
                    },
                    onClick = {
                        navController.navigate(
                            NavRoutes.EventDetails.withArgs(workspaceId, event.eventId)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (upcomingEvents.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.Upcoming),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(upcomingEvents) { event ->
                val eventDate = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(zoneId).toLocalDate()

                val instance = allEventInstances.find {
                    it.eventId == event.eventId && it.instanceDate == eventDate.toEpochDay()
                }

                EventCard(
                    radius = radius,
                    isAllDay = event.isAllDay,
                    isPending = instance == null,
                    title = event.title,
                    timeline = event.startDateTime,
                    repeat = event.repetition,
                    settings = settings,
                    onChangeStatus = {
                        if(instance == null) {
                            onTaskEvents(
                                TaskEvents.UpsertInstance(
                                    EventInstanceModel(
                                        eventId = event.eventId,
                                        workspaceId = workspaceId,
                                        instanceDate = LocalDate.now().toEpochDay()
                                    )
                                )
                            )
                        }
                        else { onTaskEvents(TaskEvents.DeleteInstance(instance)) }
                    },
                    onClick = {
                        navController.navigate(
                            NavRoutes.EventDetails.withArgs(workspaceId, event.eventId)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun EmptyEvents() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(stringResource(R.string.Empty_Events))
    }
}

@Composable
fun IconRadioButton(
    modifier: Modifier = Modifier,
    uncheckedTint: Color = MaterialTheme.colorScheme.onSurface,
    checkedTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(modifier = modifier, onClick = onClick) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = checkedTint
            )
        } else {
            Icon(
                imageVector = Icons.Default.RadioButtonUnchecked,
                contentDescription = "Unselected",
                tint = uncheckedTint
            )
        }
    }
}

@Composable
fun EventCard(
    radius: Int,
    isAllDay: Boolean,
    isPending: Boolean,
    title: String,
    timeline: Long,
    repeat: String,
    settings: Settings,
    onChangeStatus: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val containerColor = if (isPending) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp) else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isPending) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = shapeManager(radius = radius * 2),
        onClick = onClick
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            IconRadioButton(
                selected = !isPending,
                onClick = onChangeStatus
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (repeat != "NONE") {
                        Row(
                            modifier = Modifier.padding(end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(Icons.Default.Repeat, null, modifier = Modifier.size(15.dp))
                            Text(
                                repeat,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.alpha(0.9f)
                            )
                        }

                    }
                    Text(
                        when {
                            isAllDay -> "All Day"
                            repeat == "DAILY" -> timeline.toFormattedDailyTime(context,settings.data.is24HourFormat)
                            repeat == "WEEKLY" -> timeline.toFormattedWeeklyTime(context,settings.data.is24HourFormat)
                            repeat == "MONTHLY" -> timeline.toFormattedMonthlyTime(settings.data.is24HourFormat)
                            repeat == "YEARLY" -> timeline.toFormattedYearlyTime(settings.data.is24HourFormat)
                            else -> timeline.toFormattedDateTime(context,settings.data.is24HourFormat)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.6f)
                    )
                }
            }
        }
    }
}

fun Long.toFormattedTime(is24Hour: Boolean = false): String {
    val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(this))
}

fun Long.toFormattedDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    return format.format(date)
}

fun Long.toFormattedDateTime(context: Context, is24Hour: Boolean = false): String {
    val timePattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val datePattern = "dd MMM, yyyy"
    val atString = context.getString(R.string.at)

    val dateFormatter = SimpleDateFormat(datePattern, Locale.getDefault())
    val timeFormatter = SimpleDateFormat(timePattern, Locale.getDefault())

    val datePart = dateFormatter.format(Date(this))
    val timePart = timeFormatter.format(Date(this))

    // we don't put them together before in case atString contains characters SimpleDateFormat could detect
    return "$datePart $atString $timePart"
}

fun Long.toFormattedWeeklyTime(context: Context, is24Hour: Boolean = false): String {
    val timePattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val dayPattern = "EEEE"
    val atString = context.getString(R.string.at)

    val dayFormatter = SimpleDateFormat(dayPattern, Locale.getDefault())
    val timeFormatter = SimpleDateFormat(timePattern, Locale.getDefault())

    val dayPart = dayFormatter.format(Date(this))
    val timePart = timeFormatter.format(Date(this))

    return "$dayPart $atString $timePart"
}

fun Long.toFormattedMonthlyTime(is24Hour: Boolean = false): String {
    return this.toOrdinalFormatted("", is24Hour)
}

fun Long.toFormattedYearlyTime(is24Hour: Boolean = false): String {
    return this.toOrdinalFormatted("MMM", is24Hour)
}

private fun Long.toOrdinalFormatted(pattern: String, is24Hour: Boolean = false): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@toOrdinalFormatted }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val suffix = getDayOfMonthSuffix(day)

    val datePart = SimpleDateFormat("d'$suffix' $pattern", Locale.getDefault()).format(Date(this))
    val timePattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val timePart = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(this))

    return "on $datePart at $timePart"
}

fun Long.toFormattedDailyTime(context: Context, is24Hour: Boolean = false): String {
    val timePattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val atString = context.getString(R.string.at)
    val timeFormatter = SimpleDateFormat(timePattern, Locale.getDefault())
    val timePart = timeFormatter.format(Date(this))
    return "$atString $timePart"
}
private fun getDayOfMonthSuffix(day: Int, locale: Locale = Locale.getDefault()): String {
    return when (locale.language) {
        "en" -> {
            if (day in 11..13) "th"
            else when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
        "fr" -> if (day == 1) "er" else "" // 1er in French
        else -> "" // default: no suffix
    }
}

private fun occursOnDate(event: EventModel, date: LocalDate): Boolean {
    val eventStartDate = Instant.ofEpochMilli(event.startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return when (event.repetition) {
        "DAILY" -> {
            // Daily event: occurs if date is on or after start date
            !date.isBefore(eventStartDate)
        }
        "WEEKLY" -> {
            // Weekly: same day of week and on/after start date
            !date.isBefore(eventStartDate) &&
                    date.dayOfWeek == eventStartDate.dayOfWeek
        }
        "MONTHLY" -> {
            // Monthly: same day of month and on/after start date
            !date.isBefore(eventStartDate) &&
                    date.dayOfMonth == eventStartDate.dayOfMonth
        }
        "YEARLY" -> {
            // Yearly: same month/day and on/after start date
            !date.isBefore(eventStartDate) &&
                    date.month == eventStartDate.month &&
                    date.dayOfMonth == eventStartDate.dayOfMonth
        }
        "NONE" -> {
            // One-time event: exact match
            date == eventStartDate
        }
        else -> {
            false
        }
    }
}

private fun calculateNextOccurrence(event: EventModel, today: LocalDate): LocalDate? {
    val eventStartDate = Instant.ofEpochMilli(event.startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    if (today.isBefore(eventStartDate)) {
        return eventStartDate
    }

    return when (event.repetition) {
        "DAILY" -> today.plusDays(1)
        "WEEKLY" -> {
            val daysToAdd = (7 - (today.dayOfWeek.value - eventStartDate.dayOfWeek.value)) % 7
            today.plusDays(if (daysToAdd == 0) 7L else daysToAdd.toLong())
        }
        "MONTHLY" -> {
            today.plusMonths(1).withDayOfMonth(eventStartDate.dayOfMonth)
        }
        "YEARLY" -> {
            val nextYear = today.plusYears(1)
            // Handle February 29th edge case
            if (eventStartDate.month == Month.FEBRUARY && eventStartDate.dayOfMonth == 29) {
                nextYear.withDayOfMonth(min(29, nextYear.month.length(nextYear.isLeapYear)))
            } else {
                nextYear.withMonth(eventStartDate.monthValue).withDayOfMonth(eventStartDate.dayOfMonth)
            }
        }
        "NONE" -> if (eventStartDate.isAfter(today)) eventStartDate else null
        else -> null
    }
}