package com.flux.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.LabelModel
import com.flux.data.repository.LabelRepository
import com.flux.ui.events.LabelEvents
import com.flux.ui.state.LabelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LabelViewModel @Inject constructor(
    private val repository: LabelRepository
) : ViewModel() {
    private val _state: MutableStateFlow<LabelState> = MutableStateFlow(LabelState())
    val state: StateFlow<LabelState> = _state.asStateFlow()

    fun onEvent(event: LabelEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private fun updateState(reducer: (LabelState) -> LabelState) {
        _state.value = reducer(_state.value)
    }

    init {
        viewModelScope.launch {
            repository.loadAllLabels().collect { labels -> updateState { it.copy(isLoading = false, allLabels = labels) } }
        }
    }

    private fun reduce(event: LabelEvents) {
        when (event) {
            is LabelEvents.DeleteLabel -> deleteLabel(event.data)
            is LabelEvents.UpsertLabel -> upsertLabel(event.data)
            is LabelEvents.DeleteAllWorkspaceLabels -> deleteAllWorkspaceLabels(event.workspaceId)
        }
    }

    private fun deleteLabel(data: LabelModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteLabel(data) }
    }

    private fun upsertLabel(data: LabelModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.upsertLabel(data) }
    }

    private fun deleteAllWorkspaceLabels(workspaceId: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteAllWorkspaceLabels(workspaceId) }
    }
}