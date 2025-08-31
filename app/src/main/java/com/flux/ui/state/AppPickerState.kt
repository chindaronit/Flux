package com.flux.ui.state

import com.flux.ui.screens.settings.attention_manager.App

data class AppPickerState(
    val isLoading: Boolean = true,
    val apps: List<App> = emptyList()
)
