package com.flux.ui.screens.calendar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.DailyViewCalendar
import com.flux.ui.components.MonthlyViewCalendar
import com.flux.ui.events.TaskEvents
import com.flux.ui.screens.events.EmptyEvents
import com.flux.ui.screens.events.EventCard
import com.flux.ui.state.Settings
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.calendarItems(
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