package com.omnipad.ui.state

import com.omnipad.data.model.SettingsModel

data class Settings(
    val isLoading: Boolean = true,
    val data: SettingsModel = SettingsModel()
)
