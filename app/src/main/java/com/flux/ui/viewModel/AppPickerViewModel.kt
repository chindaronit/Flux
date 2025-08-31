package com.flux.ui.viewModel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flux.ui.screens.settings.attention_manager.App
import com.flux.ui.state.AppPickerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppPickerState())
    val uiState: StateFlow<AppPickerState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = AppPickerState(isLoading = true)

            val packageManager: PackageManager = getApplication<Application>().packageManager
            val packages: List<ApplicationInfo> =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            val apps = packages.map { app ->
                val label = app.loadLabel(packageManager).toString()
                val packageName = app.packageName
                val icon = app.loadIcon(packageManager).toBitmap()
                App(label, packageName, icon)
            }

            _uiState.value = AppPickerState(isLoading = false, apps = apps)
        }
    }
}
