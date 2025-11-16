package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.repository.HabitRepository
import com.flux.other.cancelReminder
import com.flux.other.getNextOccurrence
import com.flux.other.scheduleReminder
import com.flux.ui.events.HabitEvents
import com.flux.ui.state.HabitState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(private val repository: HabitRepository) : ViewModel() {
    private val _state: MutableStateFlow<HabitState> = MutableStateFlow(HabitState())
    val state: StateFlow<HabitState> = _state.asStateFlow()
    private val mutex = Mutex()

    fun onEvent(event: HabitEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private suspend fun updateState(reducer: (HabitState) -> HabitState) {
        mutex.withLock { _state.value = reducer(_state.value) }
    }

    private suspend fun reduce(event: HabitEvents) {
        when (event) {
            is HabitEvents.DeleteHabit -> deleteHabit(event.habit, event.context)
            is HabitEvents.LoadAllHabits -> loadAllHabits(event.workspaceId)
            is HabitEvents.UpsertHabit -> upsertHabit(event.context, event.habit)
            is HabitEvents.LoadAllInstances -> loadAllInstances(event.workspaceId)
            is HabitEvents.MarkDone -> upsertInstance(event.habitInstance)
            is HabitEvents.MarkUndone -> deleteInstance(event.habitInstance)
            is HabitEvents.DeleteAllWorkspaceHabits -> deleteWorkspaceHabits(event.workspaceId, event.context)
        }
    }

    private fun deleteInstance(instance: HabitInstanceModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteInstance(instance) }
    }

    private fun deleteWorkspaceHabits(workspaceId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value.allHabits.forEach { habit ->
                cancelReminder(
                    context,
                    habit.id,
                    habit.type.toString(),
                    habit.title,
                    habit.description,
                    habit.workspaceId,
                    habit.endDateTime,
                    habit.recurrence
                )
            }
            repository.deleteAllWorkspaceHabit(workspaceId)
        }
    }

    private fun upsertInstance(instance: HabitInstanceModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.upsertHabitInstance(instance) }
    }

    private fun deleteHabit(data: HabitModel, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(context, data.id, data.type.toString(), data.title, data.description, data.workspaceId, data.endDateTime, data.recurrence)
            repository.deleteHabit(data)
        }
    }

    private fun upsertHabit(context: Context, data: HabitModel) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(context, data.id, data.type.toString(), data.title, data.description, data.workspaceId, data.endDateTime, data.recurrence)
            repository.upsertHabit(data)
            val nextOccurrence = getNextOccurrence(data.recurrence, data.startDateTime)

            // Only schedule reminder if there's a future occurrence
            if (nextOccurrence != null && nextOccurrence > System.currentTimeMillis()) {
                scheduleReminder(
                    context = context,
                    id = data.id,
                    data.type.toString(),
                    recurrence = data.recurrence,
                    timeInMillis = nextOccurrence,
                    title = data.title,
                    description = data.description,
                    workspaceId = data.workspaceId,
                    endTimeInMillis = data.endDateTime
                )
            }
        }
    }

    private suspend fun loadAllInstances(workspaceId: String) {
        updateState { it.copy(isLoading = true) }
        repository.loadAllHabitInstance(workspaceId).distinctUntilChanged()
            .collect { data -> updateState { it.copy(isLoading = false, allInstances = data) } }
    }

    private suspend fun loadAllHabits(workspaceId: String) {
        updateState { it.copy(isLoading = true) }
        repository.loadAllHabitsOfWorkspace(workspaceId)
            .distinctUntilChanged()
            .collect { data ->
                val sortedData = data.sortedBy { it.startDateTime }
                updateState { it.copy(isLoading = false, allHabits = sortedData) }
            }
    }
}