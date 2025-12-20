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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.RecurrenceRule
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.DailyViewCalendar
import com.flux.ui.components.MonthlyViewCalendar
import com.flux.ui.components.shapeManager
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.Settings
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.eventHomeItems(
    navController: NavController,
    radius: Int,
    is24HourFormat: Boolean,
    isLoading: Boolean,
    workspaceId: String,
    selectedMonth: YearMonth,
    selectedDate: Long,
    settings: Settings,
    datedEvents: List<EventModel>,
    allEventInstances: List<EventInstanceModel>,
    onTaskEvents: (TaskEvents) -> Unit
) {
    val isMonthlyView = settings.data.isCalendarMonthlyView

    if (isMonthlyView) {
        item {
            MonthlyViewCalendar(
                selectedMonth, selectedDate,
                onMonthChange = {
                    onTaskEvents(TaskEvents.ChangeMonth(it))
                },
                onDateChange = {
                    onTaskEvents(TaskEvents.LoadDateTask(workspaceId, it))
                    onTaskEvents(TaskEvents.ChangeDate(it))
                })
        }
    } else {
        item {
            DailyViewCalendar(
                selectedMonth,
                selectedDate,
                onDateChange = {
                    onTaskEvents(TaskEvents.LoadDateTask(workspaceId, it))
                    onTaskEvents(TaskEvents.ChangeDate(it))
                })
        }
    }

    if (isLoading) {
        item { Loader() }
    } else if (datedEvents.isEmpty()) {
        item { EmptyEvents() }
    } else {
        item { Spacer(Modifier.height(24.dp)) }

        val pendingTasks = datedEvents.filter { task ->
            val instance = allEventInstances.find { it.eventId == task.id && it.instanceDate == selectedDate }
            instance == null
        }

        val completedTasks = datedEvents.filter { task ->
            val instance = allEventInstances.find { it.eventId == task.id && it.instanceDate == selectedDate }
            instance != null
        }

        if (pendingTasks.isNotEmpty()) {
            items(pendingTasks) { task ->
                EventCard(
                    radius = radius,
                    is24HourFormat = is24HourFormat,
                    isPending = true,
                    title = task.title,
                    repeat = task.recurrence,
                    startDateTime = task.startDateTime,
                    onChangeStatus = { onTaskEvents(TaskEvents.ToggleStatus(true, task.id, workspaceId, selectedDate)) },
                    onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(workspaceId, task.id, selectedDate)) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        if (completedTasks.isNotEmpty()) {
            items(completedTasks) { task ->
                EventCard(
                    radius = radius,
                    is24HourFormat = is24HourFormat,
                    isPending = false,
                    title = task.title,
                    repeat = task.recurrence,
                    startDateTime = task.startDateTime,
                    onChangeStatus = { onTaskEvents(TaskEvents.ToggleStatus(false, task.id, workspaceId, selectedDate)) },
                    onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(workspaceId, task.id, selectedDate)) }
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
    is24HourFormat: Boolean,
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

    val context = LocalContext.current
    val time = startDateTime.toFormattedTime(is24HourFormat)

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
                        "${getRecurrenceText(context, repeat, startDateTime)} at $time",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

fun getRecurrenceText(context: Context, repeat: RecurrenceRule, startDateTime: Long): String {
    val localDate = Instant.ofEpochMilli(startDateTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return when (repeat) {
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
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
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