package com.flux.ui.events

import com.flux.data.model.SettingsModel

sealed class SettingEvents {
    data class UpdateSettings(val data: SettingsModel): SettingEvents()
    data object ResetDatabase: SettingEvents()
}