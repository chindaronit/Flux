package com.flux.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.TodoModel
import com.flux.data.repository.TodoRepository
import com.flux.ui.events.TodoEvents
import com.flux.ui.state.TodoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
            state
                .map { it.workspaceId }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { workspaceId: String ->
                    repository.loadAllLists(workspaceId)
                }
                .collect { lists ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            allLists = lists
                        )
                    }
                }
        }
    }

    private fun reduce(event: TodoEvents) {
        when (event) {
            is TodoEvents.DeleteList -> { deleteList(event.data) }
            is TodoEvents.UpsertList -> { upsertList(event.data) }
            is TodoEvents.DeleteAllWorkspaceLists -> deleteWorkspaceLists(event.workspaceId)
            is TodoEvents.EnterWorkspace -> { updateState { if (it.workspaceId == event.workspaceId) { it } else {it.copy(workspaceId = event.workspaceId, isLoading = true) }} }
        }
    }

    private fun deleteWorkspaceLists(workspaceId: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteAllWorkspaceLists(workspaceId) }
    }

    private fun deleteList(data: TodoModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteList(data) }
    }

    private fun upsertList(data: TodoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertList(data)
        }
    }
}