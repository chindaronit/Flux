package com.flux.ui.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.database.FluxDatabase
import com.flux.data.model.SettingsModel
import com.flux.data.repository.SettingsRepository
import com.flux.ui.effects.ScreenEffect
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val repository: SettingsRepository,
    val database: FluxDatabase
) : ViewModel() {

    private val _state: MutableStateFlow<Settings> = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()
    private val _effect: Channel<ScreenEffect> = Channel()
    val effect = _effect.receiveAsFlow()

    init { loadSettings() }

    private fun setEffect(builder: () -> ScreenEffect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }

    fun onEvent(event: SettingEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private fun updateState(reducer: (Settings) -> Settings) {
        _state.value = reducer(_state.value)
    }

    private fun reduce(event: SettingEvents) {
        when (event) {
            is SettingEvents.UpdateSettings -> { updateSettings(event.data) }
            is SettingEvents.ResetDatabase -> { resetDatabase() }
        }
    }

    private fun loadSettings() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.loadSettings().collect { data ->
                if (data != null){ updateState { it.copy(isLoading = false, data = data) } }
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

    fun resetDatabase() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }
            }.onSuccess {
                setEffect {
                    ScreenEffect.ShowSnackBarMessage("Reset completed")
                }
            }.onFailure { throwable ->
                setEffect {
                    ScreenEffect.ShowSnackBarMessage(
                        "Failed to reset database: ${throwable.localizedMessage ?: "error"}"
                    )
                }
            }
        }
    }
}
