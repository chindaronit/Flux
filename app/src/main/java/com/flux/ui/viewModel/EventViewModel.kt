package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.occursOn
import com.flux.data.repository.EventRepository
import com.flux.other.cancelReminder
import com.flux.other.getNextOccurrence
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
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    val repository: EventRepository
) : ViewModel() {
    private val mutex = Mutex()
    private val _state: MutableStateFlow<EventState> = MutableStateFlow(EventState())
    val state: StateFlow<EventState> = _state.asStateFlow()

    fun onEvent(event: TaskEvents) { viewModelScope.launch { reduce(event = event) } }
    private suspend fun safeUpdateState(reducer: (EventState) -> EventState) {
        mutex.withLock { _state.value = reducer(_state.value) }
    }

    private suspend fun reduce(event: TaskEvents) {
        when (event) {
            is TaskEvents.DeleteTask -> deleteEvent(event.taskEvent, event.context)
            is TaskEvents.UpsertTask -> upsertEvent(event.context, event.taskEvent)
            is TaskEvents.LoadDateTask -> loadDateEvents(event.workspaceId, event.selectedDate)
            is TaskEvents.LoadAllTask -> loadAllEvents(event.workspaceId)
            is TaskEvents.LoadAllInstances -> loadAllEventsInstances(event.workspaceId)
            is TaskEvents.DeleteAllWorkspaceEvents -> deleteWorkspaceEvents(event.workspaceId, event.context)
            is TaskEvents.ChangeMonth -> safeUpdateState { it.copy(selectedYearMonth = event.newYearMonth) }
            is TaskEvents.ChangeDate -> safeUpdateState {
                val newDate = event.newLocalDate
                it.copy(
                    selectedDate = newDate,
                    selectedYearMonth = YearMonth.from(LocalDate.ofEpochDay(newDate))
                )
            }
            is TaskEvents.ToggleStatus -> { toggleStatus(event.markDone, event.eventId, event.workspaceId, event.date) }
        }
    }

    private fun toggleStatus(markDone: Boolean, eventId: String, workspaceId: String, date: Long) {
        val data = _state.value.allEventInstances.find{ it.eventId==eventId && it.instanceDate==date}?: EventInstanceModel(eventId, workspaceId, date)
        viewModelScope.launch(Dispatchers.IO) {
            if(markDone){ repository.upsertEventInstance(data) }
            else{ repository.deleteEventInstance(data) }
        }
    }

    private fun deleteEvent(data: EventModel, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(
                context,
                data.id,
                data.type.toString(),
                data.title,
                data.description,
                data.recurrence
            )
            repository.deleteEvent(data)
        }
    }

    private fun upsertEvent(context: Context, data: EventModel) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(context, data.id, data.type.toString(), data.title, data.description, data.recurrence)
            repository.upsertEvent(data)
            val nextOccurrence = getNextOccurrence(data.recurrence, data.startDateTime)

            // Only schedule reminder if there's a future occurrence
            if (nextOccurrence != null && nextOccurrence > System.currentTimeMillis()) {
                scheduleReminder(
                    context = context,
                    id = data.id,
                    data.type.toString(),
                    recurrence = data.recurrence,
                    timeInMillis = nextOccurrence-data.notificationOffset,
                    title = data.title,
                    description = data.description
                )
            }
        }
    }

    private fun deleteWorkspaceEvents(workspaceId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value.allEvent.forEach { event ->
                cancelReminder(
                    context,
                    event.id,
                    event.type.toString(),
                    event.title,
                    event.description,
                    event.recurrence
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
        return events.filter { it.occursOn(date) }
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
            safeUpdateState { it.copy(isAllEventsLoading = false, allEvent = events) }
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
