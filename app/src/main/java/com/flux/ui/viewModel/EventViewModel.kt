package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.occursOn
import com.flux.data.model.toScheduleRequest
import com.flux.data.repository.EventRepository
import com.flux.other.cancelReminder
import com.flux.other.computeMonthlyEventDates
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// destructuring support
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component1() = first
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component2() = second
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component3() = third
operator fun <A, B, C, D> Quadruple<A, B, C, D>.component4() = fourth

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
            combine(
                repository.loadEventData(),
                repository.loadEventInstanceData(),
                state.map { it.selectedDate }.distinctUntilChanged(),
                state.map { it.selectedYearMonth }.distinctUntilChanged()
            ) { allEvents, allInstances, selectedDate, selectedYearMonth ->
                val datedEvents = filterByDate(allEvents, selectedDate)
                val monthlyDates = computeMonthlyEventDates(allEvents, selectedYearMonth)

                Quadruple(allEvents, datedEvents, allInstances, monthlyDates)
            }.collect { (allEvents, datedEvents, allInstances, monthlyDates) ->

                updateState {
                    it.copy(
                        isAllEventsLoading = false,
                        isDatedEventLoading = false,
                        allEvent = allEvents,
                        datedEvents = datedEvents,
                        allEventInstances = allInstances,
                        monthlyEventDates = monthlyDates
                    )
                }
            }
        }
    }

    private fun reduce(event: TaskEvents) {
        when (event) {
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
            cancelReminder(context, data.toScheduleRequest())
            repository.deleteEvent(data)
        }
    }

    private fun upsertEvent(context: Context, data: EventModel) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(context, data.toScheduleRequest())
            repository.upsertEvent(data)
            scheduleNextReminder(context = context, data.toScheduleRequest())
        }
    }

    private fun deleteWorkspaceEvents(workspaceId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            state.value.allEvent.forEach { event -> cancelReminder(context, event.toScheduleRequest()) }
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