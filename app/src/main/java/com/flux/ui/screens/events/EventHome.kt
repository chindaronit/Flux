package com.flux.ui.screens.events

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.EventStatus
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.shapeManager
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.Settings
import com.flux.ui.theme.completed
import com.flux.ui.theme.pending
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                val eventDate = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(zoneId).toLocalDate()
                eventDate == today
            }
            .sortedBy { event ->
                val instance = allEventInstances.find {
                    it.eventId == event.eventId && it.instanceDate == today.toEpochDay()
                }
                instance?.status == EventStatus.COMPLETED
            }

        // 🔹 Upcoming events (after today)
        val upcomingEvents = allEvents
            .filter { event ->
                val eventDate = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(zoneId).toLocalDate()
                eventDate.isAfter(today)
            }
            .sortedBy { event ->
                val eventDate = Instant.ofEpochMilli(event.startDateTime)
                    .atZone(zoneId).toLocalDate()
                val instance = allEventInstances.find {
                    it.eventId == event.eventId && it.instanceDate == eventDate.toEpochDay()
                }
                instance?.status == EventStatus.COMPLETED
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
                } ?: EventInstanceModel(
                    eventId = event.eventId,
                    instanceDate = today.toEpochDay(),
                    workspaceId = workspaceId
                )

                EventCard(
                    radius = radius,
                    isAllDay = event.isAllDay,
                    eventInstance = instance,
                    title = event.title,
                    timeline = event.startDateTime,
                    description = event.description,
                    repeat = event.repetition,
                    settings = settings,
                    onChangeStatus = { onTaskEvents(TaskEvents.ToggleStatus(it)) },
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
                } ?: EventInstanceModel(
                    eventId = event.eventId,
                    instanceDate = eventDate.toEpochDay(),
                    workspaceId = workspaceId
                )

                EventCard(
                    radius = radius,
                    isAllDay = event.isAllDay,
                    eventInstance = instance,
                    title = event.title,
                    timeline = event.startDateTime,
                    description = event.description,
                    repeat = event.repetition,
                    settings = settings,
                    onChangeStatus = { onTaskEvents(TaskEvents.ToggleStatus(it)) },
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
    checkedTint: Color = MaterialTheme.colorScheme.onSurface,
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
    eventInstance: EventInstanceModel,
    title: String,
    timeline: Long,
    description: String,
    repeat: String,
    settings: Settings,
    onChangeStatus: (EventInstanceModel) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val eventStatus = eventInstance.status
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconRadioButton(
            selected = eventStatus == EventStatus.COMPLETED,
            onClick = {
                val newStatus =
                    if (eventStatus == EventStatus.COMPLETED) EventStatus.PENDING else EventStatus.COMPLETED
                onChangeStatus(eventInstance.copy(status = newStatus))
            }
        )
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (description.isEmpty()) 55.dp else 76.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = shapeManager(radius = radius * 2),
            onClick = onClick
        ) {
            Row(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(if (description.isEmpty()) 55.dp else 76.dp)
                        .background(
                            if (eventStatus == EventStatus.PENDING) pending else completed,
                            RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                        )
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
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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