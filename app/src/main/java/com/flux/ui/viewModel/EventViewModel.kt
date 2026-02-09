package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.occursOn
import com.flux.data.repository.EventRepository
import com.flux.other.cancelReminder
import com.flux.other.scheduleNextReminder
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.EventState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventState())
    val state: StateFlow<EventState> = _state.asStateFlow()

    fun onEvent(event: TaskEvents) {
        viewModelScope.launch { reduce(event) }
    }

    private fun updateState(reducer: (EventState) -> EventState) {
        _state.update(reducer)
    }

    init {
        viewModelScope.launch {
            state
                .map { it.workspaceId }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { workspaceId: String ->
                    combine(
                        repository.loadAllWorkspaceEvents(workspaceId),
                        repository.loadAllEventInstances(workspaceId)
                    ) { events: List<EventModel>, instances: List<EventInstanceModel> ->
                        Pair(events, instances)
                    }
                }
                .combine(
                    state.map { it.selectedDate }.distinctUntilChanged()
                ) { eventsAndInstances: Pair<List<EventModel>, List<EventInstanceModel>>, selectedDate: Long ->
                    val (allEvents, allInstances) = eventsAndInstances
                    val datedEvents = filterByDate(allEvents, selectedDate)

                    Triple(allEvents, datedEvents, allInstances)
                }
                .collect { (allEvents, datedEvents, allInstances) ->
                    updateState {
                        it.copy(
                            isAllEventsLoading = false,
                            isDatedEventLoading = false,
                            allEvent = allEvents,
                            datedEvents = datedEvents,
                            allEventInstances = allInstances
                        )
                    }
                }
        }
    }

    private fun reduce(event: TaskEvents) {
        when (event) {
            is TaskEvents.EnterWorkspace -> {
                updateState {
                    if (it.workspaceId == event.workspaceId) { it } else {
                        it.copy(
                            workspaceId = event.workspaceId,
                            isAllEventsLoading = true,
                            isDatedEventLoading = true
                        )
                    }
                }
            }
            is TaskEvents.ChangeMonth -> updateState { it.copy(selectedYearMonth = event.newYearMonth) }
            is TaskEvents.ChangeDate -> {
                val newDate = event.newLocalDate
                updateState {
                    it.copy(
                        selectedDate = newDate,
                        selectedYearMonth = YearMonth.from(
                            LocalDate.ofEpochDay(newDate)
                        )
                    )
                }
            }
            is TaskEvents.UpsertTask -> upsertEvent(event.context, event.taskEvent)
            is TaskEvents.DeleteTask -> deleteEvent(event.taskEvent, event.context)
            is TaskEvents.ToggleStatus ->
                toggleStatus(
                    event.markDone,
                    event.eventId,
                    event.workspaceId,
                    event.date
                )
            is TaskEvents.DeleteAllWorkspaceEvents -> deleteWorkspaceEvents(event.workspaceId, event.context)
        }
    }

    private fun toggleStatus(
        markDone: Boolean,
        eventId: String,
        workspaceId: String,
        date: Long
    ) {
        val instance =
            state.value.allEventInstances.find {
                it.eventId == eventId && it.instanceDate == date
            } ?: EventInstanceModel(eventId, workspaceId, date)

        viewModelScope.launch(Dispatchers.IO) {
            if (markDone) repository.upsertEventInstance(instance)
            else repository.deleteEventInstance(instance)
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
                data.workspaceId,
                data.endDateTime,
                data.startDateTime,
                data.notificationOffset,
                data.recurrence,
            )
            repository.deleteEvent(data)
        }
    }

    private fun upsertEvent(context: Context, data: EventModel) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(
                context,
                data.id,
                data.type.toString(),
                data.title,
                data.description,
                data.workspaceId,
                data.endDateTime,
                data.startDateTime,
                data.notificationOffset,
                data.recurrence,
            )

            repository.upsertEvent(data)

            scheduleNextReminder(
                context = context,
                item = data
            )
        }
    }

    private fun deleteWorkspaceEvents(workspaceId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            state.value.allEvent.forEach { event ->
                cancelReminder(
                    context,
                    event.id,
                    event.type.toString(),
                    event.title,
                    event.description,
                    event.workspaceId,
                    event.endDateTime,
                    event.startDateTime,
                    event.notificationOffset,
                    event.recurrence
                )
            }
            repository.deleteAllWorkspaceEvent(workspaceId)
        }
    }

    private fun filterByDate(
        entries: List<EventModel>,
        epochDay: Long
    ): List<EventModel> {
        val date = LocalDate.ofEpochDay(epochDay)
        return entries.filter { it.occursOn(date) }
    }
}