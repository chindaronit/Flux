package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.repository.EventRepository
import com.flux.other.cancelReminder
import com.flux.other.scheduleReminder
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.EventState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    val repository: EventRepository
) : ViewModel() {
    private val mutex = Mutex()
    private val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState())
    val state: StateFlow<EventState> = _state.asStateFlow()

    fun onEvent(event: TaskEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private suspend fun safeUpdateState(reducer: (EventState) -> EventState) {
        mutex.withLock { _state.value = reducer(_state.value) }
    }

    private suspend fun reduce(event: TaskEvents) {
        when (event) {
            is TaskEvents.DeleteTask -> deleteEvent(event.taskEvent)
            is TaskEvents.UpsertTask -> upsertEvent(
                event.context,
                event.taskEvent,
                event.adjustedTime
            )

            is TaskEvents.ToggleStatus -> toggleStatus(event.taskInstance)
            is TaskEvents.LoadDateTask -> loadDateEvents(event.workspaceId, event.selectedDate)
            is TaskEvents.LoadAllTask -> loadAllEvents(event.workspaceId)
            is TaskEvents.LoadAllInstances -> loadAllEventsInstances(event.workspaceId)
            is TaskEvents.DeleteAllWorkspaceEvents -> deleteWorkspaceEvents(
                event.workspaceId,
                event.context
            )

            is TaskEvents.ChangeMonth -> safeUpdateState {
                it.copy(selectedYearMonth = event.newYearMonth)
            }

            is TaskEvents.ChangeDate -> safeUpdateState {
                val newDate = event.newLocalDate
                it.copy(
                    selectedDate = newDate,
                    selectedYearMonth = YearMonth.from(LocalDate.ofEpochDay(newDate))
                )
            }
        }
    }

    private fun deleteEvent(data: EventModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteEvent(data) }
    }

    private fun upsertEvent(context: Context, data: EventModel, adjustedTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertEvent(data)
            if (adjustedTime != null) {
                scheduleReminder(
                    context = context,
                    id = data.eventId,
                    type = "EVENT",
                    repeat = data.repetition,
                    timeInMillis = adjustedTime,
                    title = data.title,
                    description = data.description
                )
            }
        }
    }

    private fun toggleStatus(data: EventInstanceModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.toggleStatus(data) }
    }

    private fun deleteWorkspaceEvents(workspaceId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value.allEvent.forEach { event ->
                cancelReminder(
                    context,
                    event.eventId,
                    "EVENT",
                    event.title,
                    event.description,
                    event.repetition
                )
            }
            repository.deleteAllWorkspaceEvent(workspaceId)
        }
    }

    private fun filterEventsByDate(
        events: List<EventModel>,
        epochDay: Long
    ): List<EventModel> {
        val date = LocalDate.ofEpochDay(epochDay)

        return events.filter { task ->
            val taskStartDate = Instant.ofEpochMilli(task.startDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            when (task.repetition) {
                "NONE" -> taskStartDate == date
                "DAILY" -> !date.isBefore(taskStartDate)
                "WEEKLY" -> !date.isBefore(taskStartDate) &&
                        date.dayOfWeek == taskStartDate.dayOfWeek
                "MONTHLY" -> !date.isBefore(taskStartDate) &&
                        date.dayOfMonth == taskStartDate.dayOfMonth
                "YEARLY" -> !date.isBefore(taskStartDate) &&
                        date.dayOfYear == taskStartDate.dayOfYear
                else -> false
            }
        }
    }

    private suspend fun collectWorkspaceEvents(
        workspaceId: String,
        onEvents: suspend (List<EventModel>) -> Unit
    ) {
        repository.loadAllWorkspaceEvents(workspaceId)
            .distinctUntilChanged()
            .collect { events -> onEvents(events) }
    }

    private suspend fun loadDateEvents(workspaceId: String, date: Long) {
        safeUpdateState { it.copy(isDatedEventLoading = true) }

        collectWorkspaceEvents(workspaceId) { events ->
            val filtered = filterEventsByDate(events, date)
            safeUpdateState { it.copy(isDatedEventLoading = false, datedEvents = filtered) }
        }
    }

    private suspend fun loadAllEvents(workspaceId: String) {
        safeUpdateState { it.copy(isAllEventsLoading = true) }

        collectWorkspaceEvents(workspaceId) { events ->
            safeUpdateState {
                it.copy(
                    isAllEventsLoading = false,
                    allEvent = events.sortedBy {event-> event.startDateTime })
            }
        }
    }

    private suspend fun loadAllEventsInstances(workspaceId: String) {
        safeUpdateState { it.copy(isDatedEventLoading = true) }
        repository.loadAllEventInstances(workspaceId)
            .distinctUntilChanged()
            .collect { data ->
                safeUpdateState {
                    it.copy(
                        isDatedEventLoading = false,
                        allEventInstances = data
                    )
                }
            }
    }
}
