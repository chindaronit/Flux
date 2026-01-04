package com.flux.ui.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.SettingsModel
import com.flux.data.repository.SettingsRepository
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val repository: SettingsRepository
) : ViewModel() {

    private val _state: MutableStateFlow<Settings> = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private fun updateState(reducer: (Settings) -> Settings) {
        _state.value = reducer(_state.value)
    }

    private fun reduce(event: SettingEvents) {
        when (event) {
            is SettingEvents.UpdateSettings -> {
                updateSettings(event.data)
            }
        }
    }

    private fun loadSettings() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.loadSettings().collect { data ->
                if (data != null){
                    println(data)
                    updateState { it.copy(isLoading = false, data = data) }
                }
                else updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateSettings(data: SettingsModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.upsertSettings(data) }
    }

    suspend fun isStorageReady(): Boolean {
        return repository.hasValidStorageRoot()
    }

    fun saveRootUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveStorageRoot(uri)
        }
    }
}
