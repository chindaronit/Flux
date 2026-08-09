package com.flux.ui.events

import android.content.Context
import android.net.Uri
import com.flux.data.model.SettingsModel

sealed class SettingEvents {
    data class UpdateSettings(val data: SettingsModel): SettingEvents()
    data class ChangeStorageRoot(val context: Context, val newRootUri: Uri): SettingEvents()
    data object ResetRootChange: SettingEvents()
    data object ResetDatabase: SettingEvents()
}