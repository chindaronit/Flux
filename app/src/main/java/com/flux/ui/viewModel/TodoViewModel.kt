package com.flux.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel
import com.flux.data.model.toScheduleRequest
import com.flux.data.repository.TodoRepository
import com.flux.other.cancelReminder
import com.flux.other.scheduleNextReminder
import com.flux.ui.events.TodoEvents
import com.flux.ui.state.TodoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {
    private val _state: MutableStateFlow<TodoState> = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    fun onEvent(event: TodoEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private fun updateState(reducer: (TodoState) -> TodoState) {
        _state.value = reducer(_state.value)
    }

    init {
        viewModelScope.launch {
            combine(
                repository.loadTodoData(),
                repository.loadAllTodoInstance()
            ) { lists, instances ->
                updateState {
                    it.copy(
                        isLoading = false,
                        allLists = lists,
                        allInstances = instances
                    )
                }
            }.collect()
        }
    }

    private fun reduce(event: TodoEvents) {
        when (event) {
            is TodoEvents.DeleteList -> { deleteList(event.context, event.data) }
            is TodoEvents.UpsertList -> { upsertList(event.context, event.isRemovingReminder, event.data) }
            is TodoEvents.DeleteAllWorkspaceLists -> deleteWorkspaceLists(event.context, event.workspaceId)
            is TodoEvents.CreateInstance -> createInstance(event.listId, event.workspaceId)
            is TodoEvents.UpsertInstance -> upsertInstance(event.instance)
        }
    }

    private fun createInstance(listId: String, workspaceId: String) {
        val todayEpoch = LocalDate.now().toEpochDay()
        val items = _state.value.allLists.find { it.id == listId }!!.items.map { it.copy(isChecked = false) }

        val instance = TodoInstance(
            todoId = listId,
            workspaceId = workspaceId,
            instanceDate = todayEpoch,
            items = items
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (!repository.existInstance(listId, todayEpoch)) {
                repository.upsertInstance(instance)
            }
        }
    }

    private fun upsertInstance(instance: TodoInstance) {
        viewModelScope.launch(Dispatchers.IO) { repository.upsertInstance(instance) }
    }

    private fun deleteWorkspaceLists(context: Context, workspaceId: String) {
        _state.value.allLists.forEach {data->
            if(data.recurrence is RecurrenceRule.Weekly) cancelReminder(context,data.toScheduleRequest())
        }
        viewModelScope.launch(Dispatchers.IO) { repository.deleteAllWorkspaceLists(workspaceId) }
    }

    private fun deleteList(context: Context, data: TodoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            if(data.recurrence is RecurrenceRule.Weekly) cancelReminder(context,data.toScheduleRequest())
            repository.deleteList(data)
        }
    }

    private fun upsertList(context: Context, isRemovingRecurrence: Boolean, data: TodoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            if(data.recurrence is RecurrenceRule.Weekly){
                cancelReminder(context,data.toScheduleRequest())
                scheduleNextReminder(context, data.toScheduleRequest())
            }
            if(isRemovingRecurrence){
                cancelReminder(context,data.toScheduleRequest())
            }

            repository.upsertList(data)
        }
    }
}