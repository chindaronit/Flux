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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.occursOn
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.shapeManager
import com.flux.ui.events.TaskEvents
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.eventHomeItems(
    navController: NavController,
    radius: Int,
    isLoading: Boolean,
    allEvents: List<EventModel>,
    allEventInstances: List<EventInstanceModel>,
    workspaceId: String,
    onTaskEvents: (TaskEvents) -> Unit
) {
    val today = LocalDate.now()
    val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

    // Today’s events
    val eventsToday = allEvents.filter { it.occursOn(today) }

    // Upcoming (at least one occurrence before end of month, but not today)
    val upcomingEvents = allEvents.filter { event ->
        !event.occursOn(today) &&
                (1..ChronoUnit.DAYS.between(today, endOfMonth)).any { offset ->
                    val date = today.plusDays(offset)
                    event.occursOn(date)
                }
    }.distinctBy { it.id }

    if (isLoading) {
        item { Loader() }
    } else {
        if (eventsToday.isEmpty() && upcomingEvents.isEmpty()) {
            item { EmptyEvents() }
        } else {
            // Section for today
            if (eventsToday.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.Today),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(eventsToday) { event ->
                val instance = allEventInstances.find {
                    it.eventId == event.id && it.instanceDate == today.toEpochDay()
                }

                EventCard(
                    radius = radius,
                    isPending = instance == null,
                    title = event.title,
                    repeat = event.recurrence,
                    startDateTime = event.startDateTime,
                    onChangeStatus = {
                        onTaskEvents(
                            TaskEvents.ToggleStatus(
                                instance == null,
                                event.id,
                                workspaceId,
                                today.toEpochDay()
                            )
                        )
                    },
                    onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(workspaceId, event.id, today.toEpochDay())) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Section for upcoming
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
                // Find the *next occurrence date* after today
                val nextDate = (1..ChronoUnit.DAYS.between(today, endOfMonth))
                    .map { today.plusDays(it) }
                    .firstOrNull { event.occursOn(it) }

                if (nextDate != null) {
                    val epochDay = nextDate.toEpochDay()

                    // Find if an instance already exists for that date
                    val instance = allEventInstances.find {
                        it.eventId == event.id && it.instanceDate == epochDay
                    }

                    EventCard(
                        radius = radius,
                        isPending = instance == null,
                        title = event.title,
                        repeat = event.recurrence,
                        startDateTime = event.startDateTime,
                        onChangeStatus = {
                            onTaskEvents(
                                TaskEvents.ToggleStatus(
                                    instance == null,
                                    event.id,
                                    workspaceId,
                                    epochDay
                                )
                            )
                        },
                        onClick = {
                            navController.navigate(
                                NavRoutes.EventDetails.withArgs(
                                    workspaceId,
                                    event.id,
                                    epochDay
                                )
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
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
    isPending: Boolean,
    title: String,
    repeat: RecurrenceRule,
    startDateTime: Long,
    onChangeStatus: () -> Unit,
    onClick: () -> Unit
) {
    val containerColor =
        if (isPending) MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        else MaterialTheme.colorScheme.primaryContainer
    val contentColor =
        if (isPending) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onPrimaryContainer

    val localDate = Instant.ofEpochMilli(startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    val time = DateTimeFormatter.ofPattern("hh:mm a")
        .format(Instant.ofEpochMilli(startDateTime).atZone(ZoneId.systemDefault()))

    // Recurrence text
    val context = LocalContext.current

// Recurrence text
    val recurrenceText = when (repeat) {
        is RecurrenceRule.Once -> {
            context.getString(
                R.string.recurrence_once,
                localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            )
        }

        is RecurrenceRule.Custom -> {
            if (repeat.everyXDays == 1) {
                context.getString(R.string.recurrence_daily)
            } else {
                context.getString(R.string.recurrence_every_x_days, repeat.everyXDays)
            }
        }

        is RecurrenceRule.Weekly -> {
            val days = listOf("M", "T", "W", "T", "F", "S", "S") // (you can also localize these)
            if (repeat.daysOfWeek.size == 7) {
                context.getString(R.string.recurrence_daily)
            } else {
                val daysText = repeat.daysOfWeek.sorted().joinToString(", ") { days[it] }
                context.getString(R.string.recurrence_weekly_on, daysText)
            }
        }

        is RecurrenceRule.Monthly -> {
            context.getString(R.string.recurrence_monthly_on, localDate.dayOfMonth)
        }

        is RecurrenceRule.Yearly -> {
            context.getString(
                R.string.recurrence_yearly_on,
                localDate.format(DateTimeFormatter.ofPattern("MMM dd"))
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = shapeManager(radius = radius * 2),
        onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
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
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$recurrenceText at $time",
                        style = MaterialTheme.typography.bodySmall
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