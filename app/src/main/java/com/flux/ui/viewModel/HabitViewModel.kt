package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.repository.HabitRepository
import com.flux.other.cancelReminder
import com.flux.other.scheduleNextReminder
import com.flux.ui.events.HabitEvents
import com.flux.ui.state.HabitState
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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
            is HabitEvents.EnterWorkspace -> { updateState { if (it.workspaceId == event.workspaceId) { it } else { it.copy(workspaceId = event.workspaceId, isLoading = true) }} }
            is HabitEvents.UpsertHabit -> upsertHabit(event.context, event.habit)
            is HabitEvents.MarkDone -> upsertInstance(event.habitInstance)
            is HabitEvents.MarkUndone -> deleteInstance(event.habitInstance)
            is HabitEvents.DeleteAllWorkspaceHabits -> deleteWorkspaceHabits(event.workspaceId, event.context)
        }
    }

    init {
        viewModelScope.launch {
            state
                .map { it.workspaceId }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { workspaceId ->
                    combine(
                        repository.loadAllHabitsOfWorkspace(workspaceId),
                        repository.loadAllHabitInstance(workspaceId)
                    ) { habits, instances ->
                        habits.sortedBy { it.startDateTime } to instances
                    }
                }
                .collect { (habits, instances) ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            allHabits = habits,
                            allInstances = instances
                        )
                    }
                }
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
                    habit.startDateTime,
                    habit.notificationOffset,
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
            cancelReminder(context, data.id, data.type.toString(), data.title, data.description, data.workspaceId, data.endDateTime, data.startDateTime, data.notificationOffset, data.recurrence)
            repository.deleteHabit(data)
        }
    }

    private fun upsertHabit(context: Context, data: HabitModel) {
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
                data.recurrence
            )

            repository.upsertHabit(data)

            scheduleNextReminder(context = context, item = data)
        }
    }
}